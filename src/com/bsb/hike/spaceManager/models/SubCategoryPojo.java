package com.bsb.hike.spaceManager.models;

import android.text.TextUtils;

import com.google.gson.Gson;

/**
 * @author paramshah
 */
public class SubCategoryPojo extends SpaceManagerPojo
{
	@Override
	public String toString()
	{
		return new Gson().toJson(this);
	}

	public boolean isValid()
	{
		if (TextUtils.isEmpty(getHeader()) || TextUtils.isEmpty(getClassName()))
		{
			return false;
		}
		return true;
	}
}
