package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;

public class OfflineChatThread extends OneToOneChatThread
{

	public OfflineChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	@Override
	protected Conversation fetchConversation()
	{
		return super.fetchConversation();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		if (mConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
			return super.onCreateOptionsMenu(menu);
		}
		return false;
	}
	
	private List<OverFlowMenuItem> getOverFlowItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.view_profile), 0, 0, R.string.view_profile));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme), 0, 0, R.string.chat_theme));
		list.add(new OverFlowMenuItem(mConversation.isBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		return list;
	}
	
	@Override
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			convMessage.setIsOfflineMessage(true);
			addtoMessageMap(convMessage);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
			
			//TODO:Send it to the recepient
		}
	}

	@Override
	protected void sendMessage()
	{
		ConvMessage convMessage = createConvMessageFromCompose();
		sendMessage(convMessage);
	}
	
}