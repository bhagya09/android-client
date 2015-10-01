package com.bsb.hike.modules.httpmgr.request;

import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.log.LogFull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by shobhit on 27/05/15.
 */
public class StringRequest extends Request<String>
{

	protected StringRequest(Init<?> builder)
	{
		super(builder);
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		public RequestToken build()
		{
			StringRequest request = new StringRequest(this);
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
	public String parseResponse(InputStream in, int contentLength) throws IOException
	{
		try
		{
			byte[] bytes = HttpUtils.streamToBytes(in);
			return new String(bytes);
		}
		catch (NullPointerException ex)
		{
			LogFull.e("Null pointer exception while parsing json object response : " + ex);
			return null;
		}
	}
}
