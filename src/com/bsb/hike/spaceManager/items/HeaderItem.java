package com.bsb.hike.spaceManager.items;

import com.bsb.hike.spaceManager.models.SpaceManagerItem;

public class HeaderItem extends SpaceManagerItem
{
	private String subHeader;

	public HeaderItem(String header, String subHeader)
	{
		setHeader(header);
		this.subHeader = subHeader;
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

	public String getSubHeader()
	{
		return subHeader;
	}
}
