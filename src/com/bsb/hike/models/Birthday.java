package com.bsb.hike.models;

import com.bsb.hike.HikeConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class Birthday
{

	public int day;

	public int month;

	public int year;

	public static final int DEFAULT_DAY = 1;

	public static final int DEFAULT_MONTH = 0;

	public static final int DEFAULT_YEAR = 1991;

	public Birthday(int day, int month, int year)
	{
		this.day = day;
		this.month = month;
		this.year = year;
	}

	public Birthday(String jsonString)
	{
		JSONObject jsonObject;
		try
		{
			jsonObject = new JSONObject(jsonString);
			this.day = jsonObject.optInt(HikeConstants.DAY, DEFAULT_DAY);
			this.month = jsonObject.optInt(HikeConstants.MONTH, DEFAULT_MONTH);
			this.year = jsonObject.optInt(HikeConstants.YEAR, DEFAULT_YEAR);
		}
		catch (JSONException jse)
		{
			jse.printStackTrace();
		}
	}

	public String toJsonString()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.DAY, day);
			jsonObject.put(HikeConstants.MONTH, month);
			jsonObject.put(HikeConstants.YEAR, year);
		}
		catch (JSONException jse)
		{
			jse.printStackTrace();
		}

		return jsonObject.toString();
	}
}
