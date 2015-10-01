package com.bsb.hike.modules.httpmgr.request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.engine.ProgressByteProcessor;
import com.bsb.hike.utils.Utils;
/**
 * File request is used to return response in form of File to the request listener. InputStream to File is done in {@link Request#parseResponse(InputStream)}
 * 
 * @author sidharth
 * 
 */

public class FileRequest extends Request<File>
{
	private int BUFFER_SIZE = 4 * 1024; // 4Kb

	private String filePath;

	private FileRequest(Init<?> init)
	{
		super(init);
		this.filePath = init.filePath;
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		private String filePath;

		public S setFile(String filePath)
		{
			this.filePath = filePath;
			return self();
		}

		public RequestToken build()
		{
			FileRequest request = new FileRequest(this);
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
	public File parseResponse(InputStream is, int contentLength) throws IOException
	{
		FileOutputStream fos = null;
		File file = null;
		try
		{
			file = new File(filePath);
			fos = new FileOutputStream(file);
			readBytes(is, new ProgressByteProcessor(this, fos, contentLength));
			fos.flush();
			fos.getFD().sync();
			return file;
		}
		catch (IOException ex)
		{
			if (file != null)
			{
				file.delete();
			}
			throw new IOException(ex);
		}
		finally
		{
			Utils.closeStreams(fos);
		}
	}
}
