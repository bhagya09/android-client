package com.bsb.hike.models;

import android.content.Intent;

/**
 * Created by atul on 24/12/15.
 */

/**
 * Currently ComposeChatActivity Adapter is tightly bound to ContactInfo Objects only. Hence extending this until ComposeChatActivity is not refactored.
 */
public class HikeFeatureInfo extends ContactInfo
{
	private Intent mFireIntent;

	private String mName;

	private int mIconDrawable;

	private String mDescription;

	private boolean mShowCheckBox;

	public HikeFeatureInfo()
	{

	}

	public HikeFeatureInfo(String name, int iconDrawable, String description, boolean showCheckBox, Intent fireIntent)
	{
		mName = name;
		mIconDrawable = iconDrawable;
		mDescription = description;
		mShowCheckBox = showCheckBox;
		mFireIntent = fireIntent;
	}

	public int getIconDrawable()
	{
		return mIconDrawable;
	}

	public Intent getFireIntent()
	{
		return mFireIntent;
	}

	public String getName()
	{
		return mName;
	}

	public String getDescription()
	{
		return mDescription;
	}

	public boolean isShowCheckBox()
	{
		return mShowCheckBox;
	}
}
