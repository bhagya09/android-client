package com.bsb.hike.notifications.refactor.badge;

import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperUserJoin extends HikeBadgeCountKeeper
{

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.USER_JOINED_NOTIFICATION.equalsIgnoreCase(type))
		{
			boolean showNujNotif = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true);
			if (showNujNotif && mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false))
			{
				setCount(1);
			}
			else
				setCount(0);

		}
		else
			setCount(0);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.USER_JOINED_NOTIFICATION, HikePubSub.BADGE_COUNT_USER_JOINED };

	}

	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return "HikeBadgeCountKeeperUserJoin";
	}

}
