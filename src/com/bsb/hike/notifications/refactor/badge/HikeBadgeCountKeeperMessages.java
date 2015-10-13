package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;

public class HikeBadgeCountKeeperMessages extends HikeBadgeCountKeeper
{

	

	@Override
	public void onEventReceived(String type, Object object)
	{

		setCount(HikeConversationsDatabase.getInstance().getTotalUnreadMessagesConversation());
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
	}

	@Override
	protected void init()
	{
		// TODO Auto-generated method stub
		mlistener = new String[] { HikePubSub.BULK_MESSAGE_NOTIFICATION, HikePubSub.MESSAGE_RECEIVED, HikePubSub.MSG_READ, HikePubSub.BADGE_COUNT_MESSAGE_CHANGED ,HikePubSub.CONVERSATION_DELETED,HikePubSub.NEW_CONVERSATION};

	}

	@Override
	public String toString()
	{
		return "HikeBadgeCountKeeperMessages";
	}
}
