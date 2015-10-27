package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperTimeline extends HikeBadgeCountKeeper
{
	public static final String BADGE_COUNT_TIMELINE_ONLY = "badgecounttimelineonly";

	@Override
	public void onEventReceived(String type, Object object)
	{
		
		setCount(Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false, false, true, true));
		super.onEventReceived(type, object);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, HikePubSub.TIMELINE_WIPE };

	}

	@Override
	public String getSharedPreferenceTag()
	{
		// TODO Auto-generated method stub
		return BADGE_COUNT_TIMELINE_ONLY;
	}

}
