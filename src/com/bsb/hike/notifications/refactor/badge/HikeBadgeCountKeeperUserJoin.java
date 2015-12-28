package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class HikeBadgeCountKeeperUserJoin extends HikeBadgeCountKeeper
{
	public static final String BADGE_COUNT_USER_JOIN = "badgecountuserjoin";

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.USER_JOINED_NOTIFICATION.equalsIgnoreCase(type) && shouldShowUserJoin()
				|| HikePubSub.SHOW_NEW_CHAT_RED_DOT.equalsIgnoreCase(type))
		{

			setCount(1);

		}
		else if (HikePubSub.BADGE_COUNT_USER_JOINED.equals(type) && object != null)
		{
			if (object instanceof Integer)
			{
				setCount((Integer) object);
			}
		}
		else
			resetCount();
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.USER_JOINED_NOTIFICATION, HikePubSub.BADGE_COUNT_USER_JOINED, HikePubSub.SHOW_NEW_CHAT_RED_DOT };
		if (shouldShowUserJoin())
		{
			defaultCount = 1;
		}
		else
			defaultCount = 0;
	}

	@Override
	public String getSharedPreferenceTag()
	{
		// TODO Auto-generated method stub
		return BADGE_COUNT_USER_JOIN;
	}
	private boolean shouldShowUserJoin(){
		boolean showNujNotif = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true);
		return (showNujNotif && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false));
	}
	
}
