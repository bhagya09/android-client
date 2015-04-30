package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.MessagingBotConfiguration;
import com.bsb.hike.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;
import android.view.View;

import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;

/**
 * This class is a barebones skeleton for Bot chat thread. This is still Work in progress.
 * 
 * @author piyush
 */
public class BotChatThread extends OneToOneChatThread
{

	private static final String TAG = "BotChatThread";
	private MessagingBotConfiguration configuration;

	@Override
	protected void init()
	{
		super.init();
		BotInfo botInfo = Utils.getBotInfoForBotMsisdn(msisdn);

		configuration = new MessagingBotConfiguration(botInfo.getConfiguration(), botInfo.isReceiveEnabled());
		mConversation = new BotConversation.ConversationBuilder(botInfo).build();
	}

	/**
	 * @param activity
	 * @param msisdn
	 */
	public BotChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	@Override
	protected void initView()
	{
		super.initView();
		if (!configuration.isInputEnabled())
		{
			activity.findViewById(R.id.compose_container).setVisibility(View.GONE);
		}

		if (!configuration.isAudioRecordingEnabled())
		{
			activity.findViewById(R.id.send_message).setVisibility(View.GONE);
		}

		if (!configuration.isStickerPickerEnabled())
		{
			activity.findViewById(R.id.sticker_btn).setVisibility(View.GONE);
		}

		if(!configuration.isEmoticonPickerEnabled())
		{
			activity.findViewById(R.id.emoticon_btn).setVisibility(View.GONE);
		}

		if (!configuration.isTextInputEnabled())
		{
			activity.findViewById(R.id.msg_compose).setVisibility(View.GONE);
		}
	}

	@Override
	protected Conversation fetchConversation()
	{
		super.fetchConversation();

		mConversation.setIsMute(HikeConversationsDatabase.getInstance().isBotMuted(msisdn));
		return mConversation;
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
		toggleConversationMuteViewVisibility(mConversation.isMuted());
	}

	@Override
	protected String[] getPubSubListeners()
	{
		String[] oneToOnePubSub = super.getPubSubListeners();
		int superpubSubLength = oneToOnePubSub.length;
		String[] botPubSubListeners = new String[superpubSubLength + 1];
		int index = 0;
		for (index = 0; index < superpubSubLength; index++)
		{
			botPubSubListeners[index] = oneToOnePubSub[index];
		}

		botPubSubListeners[index] = HikePubSub.MUTE_BOT;

		return botPubSubListeners;
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.MUTE_BOT:
			muteBotPubSub();
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in OneToOne ChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
		}
	}

	private void muteBotPubSub()
	{
		mConversation.setIsMute(true);
		HikeConversationsDatabase.getInstance().updateBot(msisdn, null, null, mConversation.isMuted() ? 1 : 0);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, new Pair<String, Boolean>(mConversation.getMsisdn(), mConversation.isMuted()));
	}

	@Override
	protected void sendPoke()
	{
		if (configuration.isNudgeEnabled())
		{
			super.sendPoke();
		}
	}

	@Override
	protected void openProfileScreen()
	{
		if (configuration.isViewProfileEnabled())
		{
			super.openProfileScreen();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		Logger.i(TAG, "on create options menu " + menu.hashCode());

		if (mConversation == null)
		{
			return false;
		}

		mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
		if (configuration.isOverflowMenuEnabled())
		{
			menu.findItem(R.id.overflow_menu).setVisible(true);
			menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(this);
			mActionBar.setOverflowViewListener(this);
		}
		else
		{
			menu.findItem(R.id.overflow_menu).setVisible(false);
		}

		if (configuration.isCallEnabled())
		{
			menu.findItem(R.id.voip_call).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.voip_call).setVisible(false);
		}

		if (configuration.isAttachmentPickerEnabled())
		{
			menu.findItem(R.id.attachment).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.attachment).setVisible(false);
		}
		return true;
	}

	/**
	 * Returns a list of over flow menu items to be displayed
	 *
	 * @return
	 */
	private List<OverFlowMenuItem> getOverFlowItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();

		if (configuration.isViewProfileInOverflowMenuEnabled())
		{
			list.add(new OverFlowMenuItem(getString(R.string.view_profile), 0, 0, R.string.view_profile));
		}
		if (configuration.isChatThemeInOverflowMenuEnabled())
		{
			list.add(new OverFlowMenuItem(getString(R.string.chat_theme), 0, 0, R.string.chat_theme));
		}

		/**
		 * Blocking all bots except Team Hike and muting only cricket Bot.
		 */

		if (!msisdn.equals(HikeConstants.Bots.FTUE_TEAMHIKE_MSISDN) && configuration.isBlockInOverflowMenuEnabled() )
		{
			list.add(new OverFlowMenuItem(mConversation.isBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		}

		if (msisdn.equals(HikeConstants.Bots.CRICKET_BOT_MSISDN) && configuration.isMuteInOverflowMenuEnabled())
		{
			list.add(new OverFlowMenuItem(mConversation.isMuted() ? getString(R.string.unmute) : getString(R.string.mute), 0, 0, R.string.mute));
		}

		if (configuration.isClearChatInOverflowMenuEnabled())
		{
			list.add (new OverFlowMenuItem(getString(R.string.clear_chat), 0, 0, R.string.clear_chat));
		}

		if (configuration.isEmailChatInOverflowMenuEnabled())
		{
			list.add(new OverFlowMenuItem(getString(R.string.email_chat), 0, 0, R.string.email_chat));
		}


		return list;
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		Logger.d(TAG, "Calling super Class' itemClicked");
		super.itemClicked(item);

		switch (item.id)
		{
		case R.string.mute:
			onMuteBotClicked();
			break;
		case R.string.view_profile:
			BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
			break;
		default:
			break;
		}
	}

	private void onMuteBotClicked()
	{
		boolean wasMuted = mConversation.isMuted();
		mConversation.setIsMute(!mConversation.isMuted());
		HikeConversationsDatabase.getInstance().updateBot(msisdn, null, null, mConversation.isMuted() ? 1 : 0);
		BotConversation.analyticsForBots(msisdn, wasMuted ? HikePlatformConstants.BOT_UNMUTE_CHAT : HikePlatformConstants.BOT_MUTE_CHAT, HikePlatformConstants.OVERFLOW_MENU,
				AnalyticsConstants.CLICK_EVENT, null);

		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, new Pair<String, Boolean>(mConversation.getMsisdn(), mConversation.isMuted()));

	}

	@Override
	public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems)
	{
		if (overflowItems == null)
		{
			return;
		}

		super.onPrepareOverflowOptionsMenu(overflowItems);

		for (OverFlowMenuItem overFlowMenuItem : overflowItems)
		{

			switch (overFlowMenuItem.id)
			{
			case R.string.mute:
				overFlowMenuItem.text = mConversation.isMuted() ? getString(R.string.unmute) : getString(R.string.mute);
				break;
			}
		}
	}

	@Override
	protected void onBlockUserclicked()
	{
		BotConversation.analyticsForBots(msisdn, mConversation.isBlocked() ? HikePlatformConstants.BOT_UNBLOCK_CHAT : HikePlatformConstants.BOT_BLOCK_CHAT,
				HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
		super.onBlockUserclicked();
	}

	@Override
	protected void emailChat()
	{
		BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_EMAIL_CONVERSATION, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
		super.emailChat();
	}

	@Override
	protected void showClearConversationDialog()
	{
		BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_CLEAR_CONVERSATION, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
		super.showClearConversationDialog();
	}

	@Override
	protected void sendChatThemeMessage()
	{
		super.sendChatThemeMessage();
		
		JSONObject json = new JSONObject();
		try
		{
			json.put(HikeConstants.BG_ID, currentTheme.bgId());
			BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_CHAT_THEME_PICKER, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void setLastSeenStuff(boolean firstInflation)
	{
		hideLastSeenText();
	}
	
	@Override
	protected void updateNetworkState()
	{
		super.updateNetworkState();
		boolean networkError = ChatThreadUtils.checkNetworkError();
		toggleConversationMuteViewVisibility(networkError ? false : mConversation.isMuted());
	}
	
	@Override
	protected void addUnkownContactBlockHeader()
	{
		if (configuration.isAddBlockStripEnabled())
		{
			super.addUnkownContactBlockHeader();
		}
	}
	
	@Override
	protected void setLastSeenTextBasedOnHikeValue(boolean isConvOnHike)
	{
		hideLastSeenText();
	}
	
	@Override
	public void onClick(View v)
	{
		if(v.getId() == R.id.contact_info)
		{
			BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.ACTION_BAR, AnalyticsConstants.CLICK_EVENT, null);
		}
		super.onClick(v);
	}
	
}
