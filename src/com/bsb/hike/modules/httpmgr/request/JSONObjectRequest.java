package com.bsb.hike.modules.httpmgr.request;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.log.LogFull;

/**
 * JSONObject request is used to return response in form of {@link JSONObject} to the request listener. InputStream to {@link JSONObject} is done in
 * {@link Request#parseResponse(InputStream)}
 * 
 * @author sidharth
 * 
 */
public class JSONObjectRequest extends Request<JSONObject>
{
	private JSONObjectRequest(Init<?> init)
	{
		super(init);
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		public RequestToken build()
		{
			JSONObjectRequest request = new JSONObjectRequest(this);
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
