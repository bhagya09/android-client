package com.bsb.hike.spaceManager.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author paramshah
 */
public abstract class SpaceManagerItem
{
	private long size;
	private String header;
	@Type private int type;

	public static final int HEADER = 0;

	public static final int CATEGORY = 1;

	public static final int SUBCATEGORY = 2;

	@IntDef({CATEGORY, SUBCATEGORY, HEADER})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Type{}

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

	protected void setType(@Type int type)
	{
		this.type = type;
	}

	@Type
	public int getType()
	{
		return type;
	}

	public interface SpaceManagerDeleteListener
	{
		void onDelete();
	}

}
