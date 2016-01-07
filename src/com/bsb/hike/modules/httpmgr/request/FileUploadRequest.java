package com.bsb.hike.modules.httpmgr.request;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileUploadRequest extends Request<JSONObject>
{
	private static String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

	private String X_SESSION_ID;

	private String filePath;

	private String fileType;

	private IGetChunkSize chunkSizePolicy;

	private final int DEFAULT_CHUNK_SIZE = 4 * 1024;

	private Response response;

	private HttpException exception;

	private int bytesUploaded;

	private long time;

	private FileUploadRequest(Init<?> init)
	{
		super(init);
		this.filePath = init.filePath;
		this.fileType = init.fileType;
		this.chunkSizePolicy = init.chunkSizePolicy;
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		private String filePath;

		private String fileType;

		private IGetChunkSize chunkSizePolicy;

		public S setFile(String filePath)
		{
			this.filePath = filePath;
			return self();
		}

		public S setChunkSizePolicy(IGetChunkSize chunk)
		{
			chunkSizePolicy = chunk;
			return self();
		}

		public S setFileType(String fileType)
		{
			this.fileType = fileType;
			return self();
		}

		public RequestToken build()
		{
			FileUploadRequest request = new FileUploadRequest(this);
			RequestToken token = new RequestToken(request);
			return token;
		}
	}

	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}
	}

	@Override
	public Response executeRequest(IClient client) throws Throwable
	{
		File srcFile = new File(filePath);

		int length = (int) srcFile.length();
		if (length < 1)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing FileNotFoundException because File size less than 1 byte");
			throw new FileNotFoundException("File size less than 1 byte");
		}

		// check if state file exists
		// if not exists create new state file
		FileSavedState fileSavedState = getStateFromDB();
		int mStart = 0; // mStart represents the number of bytes already uploaded

		if (fileSavedState.getFTState().equals(FTState.INITIALIZED))
		{
			// check state in state file if not started
			// create new session id and mStart = 0;
			X_SESSION_ID = UUID.randomUUID().toString();
			mStart = 0;
		}
		else if (fileSavedState.getFTState().equals(FTState.PAUSED) || fileSavedState.getFTState().equals(FTState.ERROR))
		{
			// if paused or error
			// get session id from state file
			X_SESSION_ID = fileSavedState.getSessionId();
			if (TextUtils.isEmpty(X_SESSION_ID))
			{
				X_SESSION_ID = UUID.randomUUID().toString();
				mStart = 0;
			}
			else
			{
				// make an http call to get bytes uploaded from server using session id
				mStart = getBytesUploadedFromServer();
			}
		}

		if (mStart >= length)
		{
			// if mStart is greater than file length (SHOULD NOT HAPPEN) then start again
			X_SESSION_ID = UUID.randomUUID().toString();
			mStart = 0;
		}

		long bytesTransferred = mStart;
		// RandomAccessFile pointer on file to read data in bytes from files
		RandomAccessFile raf = null;
		chunkSize = 0;
		try
		{
			raf = new RandomAccessFile(srcFile, "r");
			raf.seek(mStart);

			// creating boundary msg
			String boundaryMesssage = getBoundaryMessage(srcFile);
			String boundary = "\r\n--" + BOUNDARY + "--\r\n";

			// Calculate chunk size using network type and other stuff
			chunkSize = chunkSizePolicy.getChunkSize();

			if (mStart == 0)
			{
				chunkSize = chunkSize / 5;
			}

			/*
			 * Safe check for the case where chunk size equals zero while calculating based on network and device memory. https://hike.fogbugz.com/default.asp?42482
			 */
			if (chunkSize <= 0)
			{
				FTAnalyticEvents.sendFTDevEvent(FTAnalyticEvents.UPLOAD_FILE_TASK, "Chunk size is less than or equal to 0, so setting it to default i.e. 100kb");
				chunkSize = DEFAULT_CHUNK_SIZE;
			}

			if (chunkSize > length)
				chunkSize = (int) length;

			// calculate start and end for range header using mStart and chunkSize
			int start = mStart;
			int end = length;
			if (end >= (start + chunkSize))
				end = start + chunkSize;
			else
				chunkSize = end - start;
			end--;

			// creating a byte array that will be sent finally to server (It doesn't have file data in it yet , only boundary is being set)
			// main data of file to be uploaded will be added in below while loop
			byte[] fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);

			getState().setSessionId(X_SESSION_ID);
			getState().setTotalSize(length);
			getState().setFTState(FTState.IN_PROGRESS);
			publishProgress((float) bytesTransferred / length);
			while (end < length)
			{
				FileSavedState st = getState();
				LogFull.d("ft state in while loop file upload : " + st.getFTState().name());
				if (st.getFTState() != FTState.IN_PROGRESS)
				{
					break;
				}
				int bytesRead = raf.read(fileBytes, boundaryMesssage.length(), chunkSize);
				if (bytesRead == -1)
				{
					raf.close();
					FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_READ, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing IOException in partial read. files ended");
					throw new IOException("Exception in partial read. files ended");
				}

				String contentRange = "bytes " + start + "-" + end + "/" + length;
				List<Header> headers = getFileUploadHeaders(srcFile, contentRange);
				headers.addAll(getHeaders());

				response = null;
				exception = null;
				time = System.currentTimeMillis();

				RequestToken token = HttpRequests.uploadChunk(this.getUrl(), fileBytes, BOUNDARY, headers, getCustomId() + contentRange, getUploadChunkRequestListener());
				token.execute();

				if (exception != null)
				{
					throw exception;
				}

				if (end == (length - 1) && response != null)
				{
					bytesTransferred += chunkSize;
					this.getState().setTransferredSize(bytesTransferred);
					// upload successful
					try
					{
						byte[] bytes = (byte[]) response.getBody().getContent();
						String responseString = new String(bytes);
						JSONObject responseJson = null;
						try
						{
							responseJson = new JSONObject(responseString);
						}
						catch(JSONException e)
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.JSON_PARSING_ISSUE, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "Parsing response json received from server",
									"Response = " + responseString, e);
							raf.close();
							throw e;
						}

						getState().setResponseJson(responseJson);
						publishProgress((float) bytesTransferred / length);
						try
						{
							Thread.sleep(200);
						}
						catch (InterruptedException ex)
						{

						}
						if (getState().getFTState() != FTState.PAUSED)
						{
							getState().setFTState(FTState.COMPLETED);
						}
						saveStateInDB(getState());
					}
					catch (Exception ex)
					{
						Logger.e(getClass().getSimpleName(), "exception in getting json from response ", ex);
						throw ex;
					}
					break;
				}
				// update start and end for range header
				start += chunkSize;

				// update state in state file
				bytesTransferred += chunkSize;
				this.getState().setTransferredSize(bytesTransferred);
				FileSavedState fss = new FileSavedState(getState());
				fss.setFTState(FTState.ERROR);
				saveStateInDB(fss);

				// calculate chunk size again
				chunkSize = chunkSizePolicy.getChunkSize();

				end = length;
				if (end >= (start + chunkSize))
				{
					end = start + chunkSize;
					end--;
				}
				else
				{
					end--;
					chunkSize = end - start + 1;
				}

				if (response != null)
				{
					int statusCode = response.getStatusCode();
					boolean isCompleted = statusCode == HttpURLConnection.HTTP_OK;
					int netType = Utils.getNetworkType(HikeMessengerApp.getInstance());
					if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_CREATED)
					{
						String fileExtension = Utils.getFileExtension(filePath);
						String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
						FTAnalyticEvents.logFTProcessingTime(FTAnalyticEvents.UPLOAD_FILE_TASK, X_SESSION_ID, isCompleted, fileBytes.length, (System.currentTimeMillis() - time), contentRange, netType, fileType);
						LogFull.d("content range  : " + contentRange + " time taken : " + time);
					}
				}

				fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
				publishProgress((float) bytesTransferred / length);
			}
			LogFull.d("while loop ended");
			return response;
		}
		catch (Throwable th)
		{
			FileSavedState fss = new FileSavedState(getState());
			fss.setFTState(FTState.ERROR);
			saveStateInDB(fss);
			throw th;
		}
		finally
		{
			if (raf != null)
			{
				raf.close();
			}
		}
	}

	private IRequestListener getUploadChunkRequestListener()
	{
		return new IRequestListener() {
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException) {
				exception = httpException;
				time = System.currentTimeMillis();
			}

			@Override
			public void onRequestSuccess(Response result) {
				response = result.clone();
			}

			@Override
			public void onRequestProgressUpdate(float progress) {

			}
		};
	}

	private int getBytesUploadedFromServer() throws Throwable
	{
		RequestToken requestToken = HttpRequests.getBytesFromServer(this.getUrl(), X_SESSION_ID, new IRequestListener()
		{
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				if (httpException.getErrorCode() / 100 > 0)
				{
					bytesUploaded = 0;
				}
				else
				{
					exception = httpException;
				}
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					byte[] byteArray = (byte[]) result.getBody().getContent();
					if (byteArray != null)
					{
						String resString = new String(byteArray);
						bytesUploaded = Integer.parseInt(resString) + 1;
						if (bytesUploaded <= 0)
						{
							X_SESSION_ID = UUID.randomUUID().toString();
							bytesUploaded = 0;
						}
					}
				}
				catch (NumberFormatException ex)
				{
					Logger.e(getClass().getSimpleName(), "NumberFormatException while getting bytes uploaded from server : ", ex);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}
		});
		requestToken.execute();

		if (exception != null)
		{
			throw exception;
		}

		return bytesUploaded;
	}

	private byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + chunkSize + boundary.length()];
		System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
		System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + chunkSize, boundary.length());
		return fileBytes;
	}

	private String getBoundaryMessage(File srcFile)
	{
		if (TextUtils.isEmpty(fileType))
		{
			fileType = "";
		}
		StringBuilder res = new StringBuilder("--").append(BOUNDARY).append("\r\n");
		String name = srcFile.getName();
		try
		{
			name = URLEncoder.encode(srcFile.getName(), "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.e(getClass().getSimpleName(), "UnsupportedEncodingException  : ", e);
		}
		Logger.d(getClass().getSimpleName(), "encode file name: " + name);

		res.append("Content-Disposition: form-data; name=\"").append("file").append("\"; filename=\"").append(name).append("\"\r\n").append("Content-Type: ").append(fileType)
				.append("\r\n\r\n");
		return res.toString();
	}

	private List<Header> getFileUploadHeaders(File srcFile, String contentRange)
	{
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Connection", "Keep-Alive"));
		headers.add(new Header("Content-Name", srcFile.getName()));
		headers.add(new Header("X-Thumbnail-Required", "0"));
		headers.add(new Header("X-SESSION-ID", X_SESSION_ID));
		headers.add(new Header("X-CONTENT-RANGE", contentRange));
		headers.add(new Header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY));
		return headers;
	}

	@Override
	public JSONObject parseResponse(InputStream in, int contentLength) throws IOException
	{
		try
		{
			byte[] bytes = HttpUtils.streamToBytes(in);
			JSONObject json = new JSONObject(new String(bytes));
			return json;
		}
		catch (JSONException ex)
		{
			LogFull.e("JSONException while parsing json object response : " + ex);
			return null;
		}
	}
}
