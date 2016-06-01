package com.bsb.hike.spaceManager.models;

/**
 * @author paramshah
 */
public abstract class SpaceManagerItem
{
	private long size;
	private String header;
	private int type;

	public abstract long computeSize();

	public abstract String toString();

	public long getSize()
	{
		return size;
	}

	public void setSize(long size)
	{
		this.size = size;
	}

	public String getHeader()
	{
		return header;
	}

	public void setHeader(String header)
	{
		this.header = header;
	}

	protected void setType(int type)
	{
		this.type = type;
	}

	public int getType()
	{
		return type;
	}
}
