package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperActivityUpdate extends HikeBadgeCountKeeper
{

	public static final String BADGE_COUNT_ACTIVITY_UPDATE = "badgecountactivityupdate";

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.ACTIVITY_FEED_COUNT_CHANGED.equals(type) || HikePubSub.BADGE_COUNT_ACTIVITY_UPDATE_CHANGED.equals(type))
		{
			if (object instanceof Integer)
			{
				Integer count = (Integer) object;
				setCount(count);

			}
		}
		else if (HikePubSub.TIMELINE_WIPE.equals(type))
		{
			resetCount();

		}
		else
			super.onEventReceived(type, object);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.ACTIVITY_FEED_COUNT_CHANGED, HikePubSub.TIMELINE_WIPE, HikePubSub.BADGE_COUNT_ACTIVITY_UPDATE_CHANGED };
		defaultCount=Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false, true, false, false);
	}
	@Override
	public String getSharedPreferenceTag()
	{

		return BADGE_COUNT_ACTIVITY_UPDATE;
	}

}
