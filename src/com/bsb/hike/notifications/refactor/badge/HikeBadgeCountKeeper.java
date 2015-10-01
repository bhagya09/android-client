package com.bsb.hike.notifications.refactor.badge;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub.Listener;

public abstract class HikeBadgeCountKeeper implements Listener
{
	public HikeBadgeCountCategory mBadgeCountCategory;

	private int mBadgeCountPriority = 1;

	public String[] mlistener;

	private int count = 0;

	protected Context mContext;

	public HikeBadgeCountKeeper()
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		init();
		addListener();
	}

	protected abstract void init();

	public HikeBadgeCountKeeper(HikeBadgeCountCategory mBadgeCountCategory, String[] listener)
	{
		this.mBadgeCountCategory = mBadgeCountCategory;
		this.mlistener = listener;
		addListener();

	}

	private void addListener()
	{
		HikeMessengerApp.getInstance().getPubSub().addListeners(this, mlistener);
	}

	public int getBadgeCountPriority()
	{
		return mBadgeCountPriority;
	}

	public void setBadgeCountPriority(int mBadgeCountPriority)
	{
		this.mBadgeCountPriority = mBadgeCountPriority;
	}

	public int getCount()
	{
		return count;
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public void incrementCount(int i)
	{
		count += i;
	}

	public void resetCount()
	{
		count = 0;
	}
}
