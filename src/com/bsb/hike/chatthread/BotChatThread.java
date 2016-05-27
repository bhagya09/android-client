package com.bsb.hike.chatthread;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.MessagingBotConfiguration;
import com.bsb.hike.bots.MessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Mute;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
			activity.findViewById(R.id.bottom_panel).setVisibility(View.GONE);
		}
		else
		{
			activity.findViewById(R.id.bottom_panel).setVisibility(View.VISIBLE);

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

		return mConversation;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
        HAManager.getInstance().endChatSession(msisdn);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
        HAManager.getInstance().startChatSession(msisdn);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
	}

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        HAManager.getInstance().recordIndividualChatSession(msisdn);
    }

    @Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
		checkAndRecordNotificationAnalytics();
	}

	@Override
	protected boolean shouldShowKeyboardOffBoardingUI() {
		return configuration.isKptExitUIEnabled() && super.shouldShowKeyboardOffBoardingUI();
	}

	@Override
	protected void showNetworkError(boolean isNetworkError)
	{
		activity.findViewById(R.id.network_error_chat).setVisibility(isNetworkError ? View.VISIBLE : View.GONE);
		activity.findViewById(R.id.network_error_card).setVisibility(View.GONE);
	};

	@Override
	protected void sendNudge()
	{
		if (configuration.isNudgeEnabled())
		{
			super.sendNudge();
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
		
		if (configuration.isHelpInOverflowMenuEnabled() && BotUtils.isBot(HikePlatformConstants.CUSTOMER_SUPPORT_BOT_MSISDN))
		{
			list.add(new OverFlowMenuItem(getString(R.string.help), 0, 0, R.string.help));
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
			onMuteBotClicked(item.text);
			break;
		case R.string.view_profile:
			BotConversation.analyticsForBots(msisdn, HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
			break;
		case R.string.help:
			onHelpClicked();
			break;
		default:
			break;
		}
	}

	@Override
	public void positiveClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
			case HikeDialogFactory.MUTE_CHAT_DIALOG:
				toggleMuteBot();
				dialog.dismiss();
				break;
			default:
				super.positiveClicked(dialog);
				break;
		}
	}

	private void onMuteBotClicked(String text)
	{
		if ((getString(R.string.mute)).equals(text))
		{
			boolean muteApproach = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MUTE_ONE_TO_ONE_SERVER_SWITCH, true);
			if (muteApproach)
			{
				this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.MUTE_CHAT_DIALOG, this, mConversation.getMute());
			}
			else
			{
				Mute mute = new Mute.InitBuilder(mConversation.getMsisdn()).setIsMute(false).setMuteDuration(HikeConstants.MuteDuration.DURATION_FOREVER).setShowNotifInMute(false).build();
				mConversation.setMute(mute);
				Utils.toggleMuteChat(activity.getApplicationContext(), mConversation.getMute());
			}
		}
		else
		{
			toggleMuteBot();
		}
	}

	private void toggleMuteBot()
	{
		boolean wasMuted = mConversation.isMuted();
		Utils.toggleMuteChat(activity.getApplicationContext(), mConversation.getMute());
		HikeConversationsDatabase.getInstance().toggleMuteBot(msisdn, mConversation.isMuted());
		BotConversation.analyticsForBots(msisdn, wasMuted ? HikePlatformConstants.BOT_UNMUTE_CHAT : HikePlatformConstants.BOT_MUTE_CHAT, HikePlatformConstants.OVERFLOW_MENU,
				AnalyticsConstants.CLICK_EVENT, null);
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
			json.put(HikeConstants.BG_ID, currentThemeId);
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
			onMuteBotClicked(((CustomFontButton) v).getText().toString());
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

	/**
	 * Returning here since we do not want to show the h20 tip here.
	 */
	@Override
	public void scheduleH20Tip()
	{
		return;
	}

	/**
	 * Returning here, since we do not want any of friends shizzle in BotChats
	 */
	@Override
	protected void doSetupForAddFriend()
	{
		return;
	}

	/**
	 * Returning false here, since we do not want any of friends shizzle in BotChats
	 *
	 * @return
	 */
	@Override
	protected boolean isNotMyOneWayFriend()
	{
		return false;
	}

	@Override
	protected void addFavoriteTypeTypeFromContactInfo(JSONObject json)
	{
		return; //Do nothing
	}
}
