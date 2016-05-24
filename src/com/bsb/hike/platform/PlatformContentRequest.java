package com.bsb.hike.platform;

import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.utils.Logger;

public class PlatformContentRequest
{

	private static String TAG = "PlatformContentRequest";

	public static byte STATE_READY = 1;

	public static byte STATE_WAIT = 2;

	public static byte STATE_CANCELLED = 3;

	public static byte STATE_PROBABLY_DEAD = 4;

	public static byte STATE_PROCESSING = 5;

	private PlatformContentModel mContentData;

	private PlatformContentListener<PlatformContentModel> mContentListener;

	private byte mState = STATE_READY;

	private PlatformContentRequest(PlatformContentModel contentData, PlatformContentListener<PlatformContentModel> contentListner)
	{
		mContentData = contentData;
		mContentListener = contentListner;
	}

	public static PlatformContentRequest make(PlatformContentModel contentData, PlatformContentListener<PlatformContentModel> contentListner)
	{
		if (contentData == null)
		{
			return null;
		}
		return new PlatformContentRequest(contentData, contentListner);
	}

	public PlatformContentModel getContentData()
	{
		return mContentData;
	}

	public void setmContentData(PlatformContentModel mContentData)
	{
		this.mContentData = mContentData;
	}

	public PlatformContentListener<PlatformContentModel> getListener()
	{
		return mContentListener;
	}

	public void setmContentListener(PlatformContentListener<PlatformContentModel> mContentListener)
	{
		this.mContentListener = mContentListener;
	}

	public byte getState()
	{
		return mState;
	}

	public void setState(byte mState)
	{
		Logger.d(TAG, "setting state" + (mState == STATE_READY ? "ready" : "wait") + "on " + mContentData.getContentJSON());
		this.mState = mState;
	}

	@Override
	public int hashCode()
	{
		if (mContentData == null)
		{
			return super.hashCode();
		}
		else
		{
			return mContentData.hashCode();
		}
	}

    public byte getBotType() {
        return this.mContentData.getBotType();
    }

	public void setBotType(byte botType)
	{
		this.mContentData.setBotType(botType);
	}
}
