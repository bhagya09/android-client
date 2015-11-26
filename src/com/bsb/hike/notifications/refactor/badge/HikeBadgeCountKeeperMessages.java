package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;

public class HikeBadgeCountKeeperMessages extends HikeBadgeCountKeeper
{

	public static final String BADGE_COUNT_MESSAGES="badgecountmessages";

	@Override
	public void onEventReceived(String type, Object object)
	{
		setCount(HikeConversationsDatabase.getInstance().getTotalUnreadMessagesConversationBadgeCounter(false));
		super.onEventReceived(type, object);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
	}

	@Override
	protected void init()
	{
		// TODO Auto-generated method stub
		mlistener = new String[] { HikePubSub.BULK_MESSAGE_NOTIFICATION, HikePubSub.MESSAGE_RECEIVED, HikePubSub.MSG_READ, HikePubSub.BADGE_COUNT_MESSAGE_CHANGED ,HikePubSub.CONVERSATION_DELETED,HikePubSub.NEW_CONVERSATION,HikePubSub.STEALTH_CONVERSATION_MARKED,HikePubSub.STEALTH_CONVERSATION_UNMARKED};
		defaultCount=HikeConversationsDatabase.getInstance().getTotalUnreadMessagesConversationBadgeCounter(false);
	}

	@Override
	public String getSharedPreferenceTag()
	{
		// TODO Auto-generated method stub
		return BADGE_COUNT_MESSAGES;
	}

}
