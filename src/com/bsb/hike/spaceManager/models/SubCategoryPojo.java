package com.bsb.hike.spaceManager.models;

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
}
