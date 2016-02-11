package com.bsb.hike.modules.httpmgr.request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.bsb.hike.filetransfer.DownloadFileTask;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Utils;

/**
 * File request is used to return response in form of File to the request listener. InputStream to File is done in {@link Request#parseResponse(InputStream)}
 * 
 * @author sidharth
 * 
 */
public class FileDownloadRequest extends Request<File>
{
	private String filePath;

	private long start;

	private IGetChunkSize chunkSizePolicy;

	private FileDownloadRequest(Init<?> init)
	{
		super(init);
		this.filePath = init.filePath;
		this.chunkSizePolicy = init.chunkSizePolicy;
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		private String filePath;

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

		public RequestToken build()
		{
			FileDownloadRequest request = new FileDownloadRequest(this);
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
		start = new File(filePath).length();
		if (this.getState() == null)
		{
			FileSavedState fst = new FileSavedState(FTState.IN_PROGRESS, 0, start, 0);
			this.setState(fst);
		}
		this.replaceOrAddHeader(HttpHeaderConstants.RANGE, "bytes=" + start + "-");
		LogFull.d("download range start : " + start);
		return client.execute(this);
	}

	@Override
	public File parseResponse(InputStream is, int contentLength) throws IOException
	{
		FileOutputStream fos = null;
		File file;
		long transferredSize = 0;
		int byteRead = 0;
		FileSavedState state = getState();
		try
		{
			if (contentLength > Utils.getFreeSpace())
			{
				throw new IOException(DownloadFileTask.FILE_TOO_LARGE_ERROR_MESSAGE);
			}

			file = new File(filePath);
			fos = new FileOutputStream(file, true);

			byte[] buffer;
			int len = 0;
			transferredSize = start;
			long totalSize = start + contentLength;
			if (state == null)
			{
				state = new FileSavedState(FTState.IN_PROGRESS, totalSize, transferredSize, 0);
				this.setState(state);
			}
			else
			{
				state.setFTState(FTState.IN_PROGRESS);
				state.setTransferredSize(start);
				state.setTotalSize(totalSize);
			}

			long time = 0;
			while (state.getFTState() != FTState.PAUSED)
			{
				time = System.currentTimeMillis();
				chunkSize = chunkSizePolicy.getChunkSize();
				buffer = new byte[chunkSize];
				byteRead = 0;

				if (len == -1)
				{
					state.setFTState(FTState.COMPLETED);
					saveStateInDB(state);
					break;
				}

				publishProgress((float) transferredSize / totalSize);

				while (byteRead < chunkSize)
				{
					len = is.read(buffer, byteRead, chunkSize - byteRead);
					if (len == -1)
					{
						break;
					}
					byteRead += len;
				}

				fos.write(buffer, 0, byteRead);
				transferredSize += byteRead;
				state.setTransferredSize(transferredSize);
				FileSavedState fss = new FileSavedState(state);
				fss.setFTState(FTState.ERROR);
				saveStateInDB(fss);
				LogFull.d("downloaded size : " + byteRead + " time taken : " + (System.currentTimeMillis() - time));
			}
			fos.flush();
			fos.getFD().sync();
			return file;
		}
		catch (Throwable th)
		{
			if (state != null)
			{
				FileSavedState fss = new FileSavedState(state);
				fss.setFTState(FTState.ERROR);
				saveStateInDB(fss);
			}
			throw th;
		}
		finally
		{
			Utils.closeStreams(fos);
		}
	}
}
