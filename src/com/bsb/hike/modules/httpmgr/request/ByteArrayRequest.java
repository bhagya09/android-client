package com.bsb.hike.modules.httpmgr.request;

import java.io.IOException;
import java.io.InputStream;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.HttpUtils;

/**
 * Byte array request is used to return response in form of byte[] to the request listener. InputStream to byte[] is done in {@link Request#parseResponse(InputStream)}
 * 
 * @author sidharth
 * 
 */
public class ByteArrayRequest extends Request<byte[]>
{
	private ByteArrayRequest(Init<?> init)
	{
		super(init);
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		ByteArrayRequest buildRequest()
		{
			return new ByteArrayRequest(this);
		}

		public RequestToken build()
		{
			ByteArrayRequest request = new ByteArrayRequest(this);
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
	public byte[] parseResponse(InputStream in, int contentLength) throws IOException
	{
		return HttpUtils.streamToBytes(in);
	}
}
