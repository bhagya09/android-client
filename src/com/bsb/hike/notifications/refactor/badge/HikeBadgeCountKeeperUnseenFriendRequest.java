package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperUnseenFriendRequest extends HikeBadgeCountKeeper
{


	@Override
	public void onEventReceived(String type, Object object)
	{

		setCount(Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false,false,false,true));
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.FAVORITE_COUNT_CHANGED};

	}
	

}
