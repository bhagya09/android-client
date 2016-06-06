package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

/**
 * @author paramshah
 */
public abstract class SpaceManagerCustomItem extends SpaceManagerItem implements SpaceManagerItem.SpaceManagerClickListsener
{
	private String subText;

	public SpaceManagerCustomItem(String header, String subText)
	{
		this.subText = subText;
		setHeader(header);
		setType(CUSTOM);
	}

	@Override
	public String toString()
	{
		return new Gson().toJson(this);
	}

	public String getSubText()
	{
		return subText;
	}

	public void setSubText(String subText)
	{
		this.subText = subText;
	}
}
