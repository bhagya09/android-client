/**
 * 
 */
package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Message;
import android.view.MotionEvent;
import android.widget.TextView;

import android.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author piyush
 * 
 */
public class BroadcastChatThread extends OneToNChatThread
{

	private static final String TAG = "broadcastChatThread";

	/**
	 * @param activity
	 * @param msisdn
	 */
	public BroadcastChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.chatthread.ChatThread#getPubSubListeners()
	 */
	@Override
    protected String[] getPubSubListeners()
    {
        return new String[] { HikePubSub.ONETON_MESSAGE_DELIVERED_READ, HikePubSub.CONVERSATION_REVIVED, HikePubSub.PARTICIPANT_JOINED_ONETONCONV, HikePubSub.PARTICIPANT_LEFT_ONETONCONV, 
        		HikePubSub.PARTICIPANT_JOINED_SYSTEM_MESSAGE, HikePubSub.ONETONCONV_NAME_CHANGED };
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (oneToNConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.broadcast_chat_thread_menu, getOverFlowItems(), this, this);
			return super.onCreateOptionsMenu(menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		return false;
	}
	
	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.broadcast_profile), 0, 0, R.string.broadcast_profile));
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CHAT_SEARCH_ENABLED, true))
			list.add(new OverFlowMenuItem(getString(R.string.search), 0, 0, R.string.search));
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		list.add(new OverFlowMenuItem(getString(R.string.add_shortcut), 0, 0, R.string.add_shortcut));
		
		return list;
	}
	
	@Override
	protected Conversation fetchConversation()
	{
		mConversation = oneToNConversation = (BroadcastConversation) mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, true);
		if (mConversation != null)
		{
			return super.fetchConversation();
		}

		return null;
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		showTips();
		oneToNConversation = (BroadcastConversation) conversation;
		super.fetchConversationFinished(conversation);
	}
	
	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		Logger.d(TAG, "Calling super Class' itemClicked");
		super.itemClicked(item);
		switch (item.id)
		{
		case R.string.broadcast_profile:
			openProfileScreen();
			break;
		case R.string.add_shortcut:
			Utils.createShortcut(activity, oneToNConversation.getConvInfo(), true);
			break;
		default:
		}
	}
	
	/**
	 * Used to launch Profile Activity from BroadcastChatThread
	 */
	@Override
	protected void openProfileScreen()
	{
		Intent intent = IntentFactory.getBroadcastProfileIntent(activity.getApplicationContext(), msisdn);

		activity.startActivity(intent);
	}
	
	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		mAdapter.addMessage(convMessage);
		if (convMessage.isSent())
		{
			oneToNConversation.setupReadByList(null, convMessage.getMsgID());
		}
		super.addMessage(convMessage);
	}
	
	@Override
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			setSentTo(convMessage);
			addMessage(convMessage);
			convMessage.setMessageOriginType(OriginType.BROADCAST);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
		}
	}
	
	@Override
	protected void showActiveConversationMemberCount()
	{
		int numActivePeople = oneToNConversation.getParticipantListSize();
		
		TextView memberCountTextView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		if (numActivePeople > 0)
		{
			memberCountTextView.setText(activity.getResources().getString(R.string.num_people, (numActivePeople)));
			memberCountTextView.setSelected(true);
		}
	}
	
	/*
	 * Called in UI Thread
	 * 
	 * @see com.bsb.hike.chatthread.ChatThread#fetchConversationFinished(com.bsb.hike.models.Conversation)
	 */

	private void showTips()
	{
		mTips = new ChatThreadTips(activity.getBaseContext(), activity.findViewById(R.id.chatThreadParentLayout), new int[] {}, sharedPreference);

		mTips.showTip();
	}
	
	/**
	 * This method adds the recipientsList in the ConvMessage object
	 */
	public void setSentTo(ConvMessage convMessage)
	{
		ArrayList<String> sentToList = new ArrayList<String>();
		sentToList.addAll(oneToNConversation.getConversationParticipantList().keySet());
		convMessage.setSentToMsisdnsList(sentToList);	
	}
	
	@Override
	protected boolean wasTipSetSeen(int whichTip)
	{
		return false;
	}
	
	/**
	 * No need to hide sticker tip as it won't be shown in BroadcastChatThread
	 */
	@Override
	protected void closeStickerTip()
	{
		return;
	}
	
	@Override
	protected void fetchConversationFailed()
	{
		/* the user must have deleted the chat. */
		Message message = Message.obtain();
		message.what = SHOW_TOAST;
		message.obj = R.string.invalid_broadcast_list;
		uiHandler.sendMessage(message);

		startHomeActivity();
		
		super.fetchConversationFailed();
	}
}
