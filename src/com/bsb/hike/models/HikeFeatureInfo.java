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

	private boolean isIconBGColorToBeSet;

	private boolean addSectionHeader;

	public HikeFeatureInfo()
	{

	}

	public HikeFeatureInfo(String name, int iconDrawable, String description, boolean showCheckBox, Intent fireIntent) {
		this(name, iconDrawable, description, showCheckBox, fireIntent, true, true);
	}
	public HikeFeatureInfo(String name, int iconDrawable, String description, boolean showCheckBox, Intent fireIntent, boolean isIconBGColorToBeSet, boolean addSectionHeader) {
		mName = name;
		mIconDrawable = iconDrawable;
		mDescription = description;
		mShowCheckBox = showCheckBox;
		mFireIntent = fireIntent;
		this.isIconBGColorToBeSet = isIconBGColorToBeSet;
		this.addSectionHeader = addSectionHeader;
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

	public boolean isIconBGColorToBeSet() {
		return isIconBGColorToBeSet;
	}

	public boolean needsHeader() {
		return addSectionHeader;
	}


}
