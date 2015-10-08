/**
 * 
 */
package com.bsb.hike.modules.httpmgr.request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.engine.ProgressByteProcessor;
import com.bsb.hike.utils.Utils;

/**
 * File request is used to return response in form of File to the request listener. InputStream to File is done in {@link Request#parseResponse(InputStream)} This does not delete
 * the file in case something goes wrong. This allows one to resume the download. One can also set their own file path for the state file. If that is done, it"ll be the responsibility of the caller to delete the state file as well.
 * 
 * @author piyush
 * 
 */
public class FileRequestPersistent extends FileRequest
{
	private int downloadedLength = 0;

	private int totalContentLength = 0;

	private float oldProgress = 0.0f;
	
	public static String FILE_DELIMETER = "##";
	
	public static String STATE_FILE_EXT = "_state.bin";
	
	private String stateFilePath;

	private FileRequestPersistent(Init<?> init)
	{
		super(init);
		this.stateFilePath = init.stateFilePath;
	}

	protected static abstract class Init<S extends Init<S>> extends FileRequest.Init<S>
	{
		private String stateFilePath;
		
		public RequestToken build()
		{
			FileRequestPersistent request = new FileRequestPersistent(this);
			RequestToken token = new RequestToken(request);
			return token;
		}
		
		public S setStateFilePath(String path)
		{
			this.stateFilePath = path;
			return self();
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

	protected void readBytes(InputStream is, ProgressByteProcessor progressByteProcessor) throws IOException
	{
		final byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;
		while ((len = is.read(buffer)) != -1)
		{
			downloadedLength += len;
			progressByteProcessor.processBytes(buffer, 0, len);
			saveDownloadedState(downloadedLength);
		}

	}

	private void saveDownloadedState(int downloadedLen) throws IOException
	{
		float newProgress = (downloadedLen/totalContentLength);
		
		if ((newProgress - oldProgress) >= HikeConstants.ONE_PERCENT_PROGRESS) // If the delta is > 1% we will write to file
		{

			FileOutputStream stateOutputStream = null;
			try
			{
				File stateFile = new File(TextUtils.isEmpty(stateFilePath) ? (filePath + STATE_FILE_EXT) : (stateFilePath + STATE_FILE_EXT));
				stateFile.createNewFile();
				stateOutputStream = new FileOutputStream(stateFile, false); // Do not want to open it in append Mode

				String str = "Total Bytes" + FILE_DELIMETER + downloadedLen + "\n";
				String progress = "Progress" + FILE_DELIMETER + ((float) downloadedLen / totalContentLength) + "\n";
				String originalFilePath = "Original File" + FILE_DELIMETER + filePath + "\n";
				String url = "URL" + FILE_DELIMETER + getUrl().toString();

				str += progress + originalFilePath + url;
				stateOutputStream.write(str.getBytes());
				stateOutputStream.flush();
				stateOutputStream.getFD().sync();
			}

			finally
			{
				if (stateOutputStream != null)
					Utils.closeStreams(stateOutputStream);
			}
			
			oldProgress = newProgress; // Re init oldProgress
		}

	}

	@Override
	public File parseResponse(InputStream is, int contentLength) throws IOException
	{
		totalContentLength = contentLength;
		FileOutputStream fos = null;
		File file = null;
		try
		{
			file = new File(filePath);
			fos = new FileOutputStream(file, true); // Append Mode
			readBytes(is, new ProgressByteProcessor(this, fos, contentLength));
			fos.flush();
			fos.getFD().sync();

			return file;
		}
		catch (IOException ex)
		{
			// Got an IO Exception. Possibly something went wrong
			saveDownloadedState(downloadedLength);
			throw new IOException(ex);
		}
		finally
		{
			Utils.closeStreams(fos);
		}

	}

}
