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

}
