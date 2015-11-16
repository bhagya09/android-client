package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperUnseenFriendRequest extends HikeBadgeCountKeeper
{

	public static final String BADGE_COUNT_UNSEEN_FRIEND_REQUEST="badgecountunseenfriendrequest";
	@Override
	public void onEventReceived(String type, Object object)
	{
		
		setCount(Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false,false,false,true));
		super.onEventReceived(type, object);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.FAVORITE_COUNT_CHANGED};

	}

	@Override
	public String getSharedPreferenceTag()
	{
		// TODO Auto-generated method stub
		return BADGE_COUNT_UNSEEN_FRIEND_REQUEST;
	}
	

}
