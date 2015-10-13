package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class HikeBadgeCountKeeperActivityUpdate extends HikeBadgeCountKeeper
{

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.ACTIVITY_FEED_COUNT_CHANGED.equals(type) || HikePubSub.BADGE_COUNT_ACTIVITY_UPDATE_CHANGED.equals(type))
		{
			if (object instanceof Integer)
			{
				Integer count = (Integer) object;
				setCount(count);
				HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
			}
		}
		else if (HikePubSub.TIMELINE_WIPE.equals(type))
		{
			setCount(0);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
		}

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.ACTIVITY_FEED_COUNT_CHANGED, HikePubSub.TIMELINE_WIPE, HikePubSub.BADGE_COUNT_ACTIVITY_UPDATE_CHANGED };

	}
	@Override
	public String toString()
	{
		return "HikeBadgeCountKeeperActivityUpdate";
	}

}
