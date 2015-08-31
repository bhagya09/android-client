package com.bsb.hike.utils;

import org.json.JSONObject;

import com.mobileapptracker.MATResponse;

public class MatResponse implements MATResponse
{

	@Override
	public void didSucceedWithData(JSONObject data)
	{
		Logger.d("hike MAT callback", data.toString());
	}

	@Override
	public void didFailWithError(JSONObject data)
	{
		Logger.d("hike MAT fail", data.toString());
	}

	@Override
	public void enqueuedActionWithRefId(String data)
	{
		Logger.d("hike MAT enqueued", data.toString());
	}

}
