package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.view.MenuItemCompat;
import android.util.Pair;
import android.view.Menu;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.MessagingBotConfiguration;
import com.bsb.hike.bots.MessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.CustomFontButton;

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
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		MessagingBotMetadata botMetadata = new MessagingBotMetadata(botInfo.getMetadata());
		configuration = new MessagingBotConfiguration(botInfo.getConfiguration(), botMetadata.isReceiveEnabled());
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
		else
		{
			activity.findViewById(R.id.compose_container).setVisibility(View.VISIBLE);

		}
	}
	
	@Override
	protected boolean shouldShowKeyboard()
	{
		return configuration.isInputEnabled() && super.shouldShowKeyboard();
	}

	@Override
	protected Conversation fetchConversation()
	{
		super.fetchConversation();

		mConversation.setIsMute(HikeConversationsDatabase.getInstance().isBotMuted(msisdn));
		return mConversation;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onStart()
	{
		HAManager.getInstance().startChatSession(msisdn);
		super.onStart();
	}
	
	@Override
	protected void onStop()
	{
		HAManager.getInstance().endChatSession(msisdn);
		super.onStop();
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
		toggleConversationMuteViewVisibility(mConversation.isMuted());
		checkAndRecordNotificationAnalytics();
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
			muteBotToggled(true);
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in OneToOne ChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
		}
	}

	private void muteBotToggled(boolean isMuted)
	{
		mConversation.setIsMute(isMuted);
		HikeConversationsDatabase.getInstance().toggleMuteBot(msisdn, isMuted);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, new Pair<String, Boolean>(mConversation.getMsisdn(), isMuted));
	}
	
	@Override
	protected void showNetworkError(boolean isNetworkError) 
	{
		activity.findViewById(R.id.network_error_chat).setVisibility(isNetworkError ? View.VISIBLE : View.GONE);
		activity.findViewById(R.id.network_error_card).setVisibility(View.GONE);
	};
	
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

		List<OverFlowMenuItem> menuItemList = getOverFlowItems();

		mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, menuItemList, this, this);
		if (configuration.isOverflowMenuEnabled() && !menuItemList.isEmpty())
		{
			menu.findItem(R.id.overflow_menu).setVisible(true);
			MenuItemCompat.getActionView(menu.findItem(R.id.overflow_menu)).setOnClickListener(this);
			mActionBar.setOverflowViewListener(this);
		}
		else
		{
			menu.findItem(R.id.overflow_menu).setVisible(false);
		}

		menu.findItem(R.id.voip_call).setVisible(configuration.isCallEnabled());

		menu.findItem(R.id.attachment).setVisible(configuration.isAttachmentPickerEnabled());

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
		if (configuration.isSearchInOverflowMenuEnabled() && (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CHAT_SEARCH_ENABLED, true)))
		{
			list.add(new OverFlowMenuItem(getString(R.string.search), 0, 0, R.string.search));
		}

		/**
		 * Blocking all bots except Team Hike and muting only cricket Bot.
		 */

		if (configuration.isBlockInOverflowMenuEnabled() )
		{
			list.add(new OverFlowMenuItem(mConversation.isBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		}

		if (configuration.isMuteInOverflowMenuEnabled())
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
		
		if (configuration.isHikeKeyboardInOverflowMenuEnabled())
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CHANGE_KEYBOARD_CHAT_ENABLED, true) && HikeMessengerApp.isCustomKeyboardUsable())
			{
				list.add(new OverFlowMenuItem(getString(isSystemKeyboard()?R.string.hike_keyboard:R.string.system_keyboard), 0, 0, R.string.hike_keyboard));
			}
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
		HikeConversationsDatabase.getInstance().toggleMuteBot(msisdn, mConversation.isMuted());
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
	protected void setupDefaultActionBar(boolean firstInflation)
	{
		super.setupDefaultActionBar(firstInflation);
		if (!configuration.isViewProfileEnabled())
		{
			View contactInfoContainer = mActionBarView.findViewById(R.id.contactinfocontainer);
			contactInfoContainer.setClickable(false);
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
	protected void toggleConversationMuteViewVisibility(boolean isMuted)
	{

		View v = mConversationsView.getChildAt(0);
		if (v != null && v.getTag() != null && v.getTag().equals(R.string.mute))
		{
			CustomFontButton button = (CustomFontButton) v.findViewById(R.id.add_unknown_contact);
			button.setText(isMuted ? R.string.unmute : R.string.mute);
		}


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
		switch (v.getId())
		{
		case R.id.contact_info:
			BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.ACTION_BAR, AnalyticsConstants.CLICK_EVENT, null);
			break;

		case R.id.add_unknown_contact:
			muteBotToggled(!mConversation.isMuted());
			break;
		}

		super.onClick(v);
	}
	
	/**
	 * Used to record analytics for bot opens via push notifications
	 */
	private void checkAndRecordNotificationAnalytics()
	{
		if (activity.getIntent() != null && activity.getIntent().hasExtra(AnalyticsConstants.BOT_NOTIF_TRACKER))
		{
			PlatformUtils.recordBotOpenSource(msisdn, activity.getIntent().getStringExtra(AnalyticsConstants.BOT_NOTIF_TRACKER));
		}
	}
	
}
