package com.bsb.hike.notifications.refactor.badge;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public abstract class HikeBadgeCountKeeper implements Listener
{

	private int mBadgeCountPriority = 1;

	public String[] mlistener;

	private int count = 0;
	
	protected int defaultCount;

	protected Context mContext;

	/**
	 * Listeners are added for listening to events 
	 * count is also fetched from shared Preference and updated for the initialization and on 
	 * initialization we check if the sharedPreference exists which is a valid case in case of a upgrade 
	 * otherwise we set the default Count which is kept by the keepers
	 */
	public HikeBadgeCountKeeper()
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		init();
		setCount(HikeSharedPreferenceUtil.getInstance().getData(getSharedPreferenceTag(), defaultCount));
		addListener();
	}

	protected abstract void init();

	private void addListener()
	{
		HikeMessengerApp.getPubSub().addListeners(this, mlistener);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.ACCOUNT_RESET_OR_DELETE, this);
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

	/**
	 * @param count
	 * @since saving in shared preference because if the user disables the badge counter through system settings even then on a later stage if he enables we should be able to get
	 *        all the counters.
	 */
	public void setCount(int count)
	{
		this.count = count;
		saveCount(getSharedPreferenceTag(), count);
	}

	public void resetCount()
	{
		setCount(0);
	}

	public void saveCount(String key, int count)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(key, count);
	}

	public abstract String getSharedPreferenceTag();

	@Override
	public void onEventReceived(String type, Object object)
	{
		//if there is a account reset/delete we would want to set all counters to zero 
		if (HikePubSub.ACCOUNT_RESET_OR_DELETE.equals(type))
			resetCount();

	}

}
