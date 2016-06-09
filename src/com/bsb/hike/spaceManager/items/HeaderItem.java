package com.bsb.hike.spaceManager.items;

import com.bsb.hike.spaceManager.models.SpaceManagerItem;

public class HeaderItem extends SpaceManagerItem
{
	public HeaderItem(String header)
	{
		setHeader(header);
	}

	@Override
	public long computeSize()
	{
		return 0;
	}

	@Override
	public String toString()
	{
		return null;
	}
}
