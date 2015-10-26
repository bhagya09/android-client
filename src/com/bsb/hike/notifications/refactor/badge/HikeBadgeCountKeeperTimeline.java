package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperTimeline extends HikeBadgeCountKeeper
{


	@Override
	public void onEventReceived(String type, Object object)
	{

		setCount(Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false,false,true,true));
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, HikePubSub.TIMELINE_WIPE };

	}

}
