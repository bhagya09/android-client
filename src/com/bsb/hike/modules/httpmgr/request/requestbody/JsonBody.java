package com.bsb.hike.modules.httpmgr.request.requestbody;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

public class JsonBody extends ByteArrayBody
{
	public JsonBody(JSONObject jsonBody)
	{
		super("application/json", convertToBytes(jsonBody));
	}

	private static byte[] convertToBytes(JSONObject jsonBody)
	{
		try
		{
			return jsonBody.toString().getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString()
	{
		try
		{
			return "TypedString[" + new String(getBytes(), "UTF-8") + "]";
		}
		catch (UnsupportedEncodingException e)
		{
			throw new AssertionError("Must be able to decode UTF-8");
		}
	}

}
