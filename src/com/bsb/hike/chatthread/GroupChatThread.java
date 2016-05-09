
package com.bsb.hike.chatthread;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.MuteDuration;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.Mute;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.ui.utils.HashSpanWatcher;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.voip.VoIPUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.utils.HikeAnalyticsEvent;

/**
 * @author piyush
 * 
 */
public class GroupChatThread extends OneToNChatThread
{

	private static final String TAG = "groupchatthread";

	private static final int PIN_CREATE_ACTION_MODE = 302;
	
	private static final int LATEST_PIN_DELETED = 303;
	
	private static final int GROUP_END = 304;
	
	private static final String HASH_PIN = "#pin";

	private static final String PIN_MESSAGE_SEPARATOR = ": ";
	
	private View pinView;

	private boolean isNewChat;
	
	/**
	 * @param activity
	 * @param msisdn
	 * @param newChat TODO
	 */
	public GroupChatThread(ChatThreadActivity activity, String msisdn, boolean newChat)
	{
		super(activity, msisdn);
		isNewChat = newChat;
	}

	@Override
	public void onCreate(Bundle savedState) {
		// TODO Auto-generated method stub
		super.onCreate(savedState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (oneToNConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.group_chat_thread_menu, getOverFlowItems(), this, this);
			updateUnreadPinCount();
			
			if (shouldShowCallIcon()) 
				menu.findItem(R.id.voip_call).setVisible(true);

			return super.onCreateOptionsMenu(menu);
		}
		
		return false;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (isShowingPin())
		{
			ChatThreadUtils.decrementUnreadPInCount(mConversation, isActivityVisible);
		}

		updateUnreadPinCount();
		activity.recordActivityEndTime();
	}

	@Override
	protected String[] getPubSubListeners()
	{
		return new String[] { HikePubSub.ONETON_MESSAGE_DELIVERED_READ, HikePubSub.LATEST_PIN_DELETED, HikePubSub.CONV_META_DATA_UPDATED,
				HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.CONVERSATION_REVIVED, HikePubSub.PARTICIPANT_JOINED_ONETONCONV, HikePubSub.PARTICIPANT_LEFT_ONETONCONV,
				HikePubSub.PARTICIPANT_JOINED_SYSTEM_MESSAGE, HikePubSub.ONETONCONV_NAME_CHANGED, HikePubSub.GROUP_END, HikePubSub.UPDATE_MEMBER_COUNT };
	}

	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		int unreadPinCount = 0;
		if (oneToNConversation != null)
		{
			/**
			 * It could be possible that conversation, (which loads on a background thread) is created before the UI thread calls for overflow menu creation.
			 */
			unreadPinCount = oneToNConversation.getUnreadPinnedMessageCount();
		}
		
		list.add(new OverFlowMenuItem(getString(R.string.create_pin), 0, 0, R.string.create_pin));
		list.add(new OverFlowMenuItem(getString(R.string.group_profile), unreadPinCount, 0, R.string.group_profile));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme), 0, 0, R.string.chat_theme));
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CHAT_SEARCH_ENABLED, true))
			list.add(new OverFlowMenuItem(getString(R.string.search), 0, 0, R.string.search));
		list.add(new OverFlowMenuItem(isMuted() ? getString(R.string.unmute_group) : getString(R.string.mute_group), 0, 0, R.string.mute_group));
		
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		
		return list;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		if (!oneToNConversation.isConversationAlive())
		{
			return false;
		}
		else
		{
			return super.onDoubleTap(e);
		}
	}
	
	private boolean shouldShowCallIcon()
	{
		return VoIPUtils.isGroupCallEnabled(activity.getApplicationContext());
	}

	/**
	 * Done to typecast conversation as GroupConversation here
	 */
	@Override
	protected Conversation fetchConversation()
	{
		mConversation = oneToNConversation = (GroupConversation) HikeConversationsDatabase.getInstance().getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, true);
		// imp message from DB like pin
		if (mConversation != null)
		{
			fetchImpMessage();
			return super.fetchConversation();
		}
		
		return null;
	}
	
	private void fetchImpMessage()
	{
		if (oneToNConversation.getMetadata() != null && oneToNConversation.getMetadata().isShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN))
		{
			oneToNConversation.setPinnedConvMessage(mConversationDb.getLastPinForConversation(oneToNConversation));
		}
	}

	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		mHashSpanWatcher = new HashSpanWatcher(mComposeView, HASH_PIN, getResources().getColor(R.color.sticky_yellow));
		showTips();
		oneToNConversation = (GroupConversation) conversation;
		super.fetchConversationFinished(conversation);

		/**
		 * Is the group owner blocked ? If true then show the block overlay with appropriate strings
		 */
		if (oneToNConversation.isBlocked())

		{
			String label = oneToNConversation
					.getConversationParticipantName(oneToNConversation
							.getConversationOwner());
			showBlockOverlay(label);
		}
		toggleGroupLife(oneToNConversation.isConversationAlive());
		addUnreadCountMessage();
		if (oneToNConversation.getPinnedConvMessage() != null)
		{
			showStickyMessageAtTop(oneToNConversation.getPinnedConvMessage(), false);
		}

		updateUnreadPinCount();

	}

	@Override
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case LATEST_PIN_DELETED:
			hidePinFromUI((boolean) msg.obj);
			break;
		case SHOW_IMP_MESSAGE:
			showStickyMessageAtTop((ConvMessage) msg.obj, true);
			break;
		case MESSAGE_RECEIVED:
			addMessage((ConvMessage) msg.obj);
			break;
		case GROUP_END:
			toggleGroupLife(false);
			removeKeyboardShutdownIfShowing();
			break;
		default:
			super.handleUIMessage(msg);
			break;
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.LATEST_PIN_DELETED:
			onLatestPinDeleted(object);
			break;
		case HikePubSub.GROUP_END:
			if (msisdn.equals(((JSONObject) object).optString(HikeConstants.TO)))
				uiHandler.sendEmptyMessage(GROUP_END);
			break;
		case HikePubSub.UPDATE_MEMBER_COUNT:
			activity.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showActiveConversationMemberCount();
				}
			});
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in OneToNChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
		}
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		Logger.d(TAG, "Calling super Class' itemClicked");
		super.itemClicked(item);
		switch (item.id)
		{
		case R.string.mute_group:
			if ((item.text).equals(getString(R.string.mute_group)))
			{
				boolean muteGCApproach = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MUTE_GC_SERVER_SWITCH, true);
				if (muteGCApproach)
				{
					this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.MUTE_CHAT_DIALOG, this, mConversation.getMute());
				}
				else
				{
					Mute mute = new Mute.InitBuilder(mConversation.getMsisdn()).setIsMute(false).setMuteDuration(MuteDuration.DURATION_ONE_YEAR).setShowNotifInMute(false).build();
					mConversation.setMute(mute);
					Utils.toggleMuteChat(activity.getApplicationContext(), mConversation.getMute());
				}
			}
			else
			{
				Utils.toggleMuteChat(activity.getApplicationContext(), mConversation.getMute());
			}
			break;
		case R.string.group_profile:
			openProfileScreen();
			break;
		case R.string.chat_theme:
			showThemePicker(R.string.chat_theme_tip_group);
			break;
		case R.string.create_pin:
			showPinCreateView(null);
			break;
		default:
		}
	}
	
	/**
	 * Used to launch Profile Activity from GroupChatThread
	 */
	@Override
	protected void openProfileScreen()
	{
		/**
		 * Proceeding only if the group is alive
		 */
		if (oneToNConversation.isConversationAlive() && !oneToNConversation.isBlocked())
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);

			Intent intent = IntentFactory.getGroupProfileIntent(activity.getApplicationContext(), msisdn);

			activity.startActivity(intent);
		} else if (oneToNConversation.isBlocked()) {
			String label = oneToNConversation
					.getConversationParticipantName(oneToNConversation
							.getConversationOwner());
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.block_overlay_message, label),
					Toast.LENGTH_SHORT).show();
		}
		
		else
		{
			Toast.makeText(activity.getApplicationContext(), getString(R.string.group_chat_end), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onBackPressed()
	{
		if (!super.onBackPressed())
		{
			if (mActionMode.whichActionModeIsOn() == PIN_CREATE_ACTION_MODE)
			{
				destroyPinCreateView();
				return true;
			}
			else
			{
				return false;
			}
		}
		return true;
	}

	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		/*
		 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
		 */

		TypingNotification typingNotification = removeTypingNotification();

		/**
		 * Pin Message
		 */
		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			showStickyMessageAtTop(convMessage, true);
		}

		/**
		 * Adding message to the adapter
		 */
		mAdapter.addMessage(convMessage);

		if (convMessage.isSent())
		{
			oneToNConversation.setupReadByList(null, convMessage.getMsgID());
		}

		if (convMessage.getTypingNotification() == null && typingNotification != null)
		{
			if (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty())
			{
				Logger.d(TAG, "Typing notification in group chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
				mAdapter.addMessage(new ConvMessage(typingNotification));
			}
		}

		super.addMessage(convMessage);
	}

	/*
	 * Called in UI Thread
	 * 
	 * @see com.bsb.hike.chatthread.ChatThread#fetchConversationFinished(com.bsb.hike.models.Conversation)
	 */

	private void showTips()
	{
		mTips = new ChatThreadTips(activity.getBaseContext(), activity.findViewById(R.id.chatThreadParentLayout), new int[] { ChatThreadTips.ATOMIC_ATTACHMENT_TIP,
				ChatThreadTips.ATOMIC_STICKER_TIP, ChatThreadTips.STICKER_TIP, ChatThreadTips.STICKER_RECOMMEND_TIP, ChatThreadTips.STICKER_RECOMMEND_AUTO_OFF_TIP, ChatThreadTips.WT_RECOMMEND_TIP }, sharedPreference);

		mTips.showTip();
	}

	private void addUnreadCountMessage()
	{
		if (oneToNConversation.getUnreadCount() > 0 && oneToNConversation.getMessagesList().size() > 0)
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null))
			{
				int index = (messages.size() - mConversation.getUnreadCount()) ;
				index = index >= 0 ? index : 0;
				long timeStamp = messages.get(index).getTimestamp();
				long msgId = messages.get(index).getMsgID();
				long sortingId = messages.get(index).getSortingId();
				messages.add(index, new ConvMessage(mConversation.getUnreadCount(), timeStamp, msgId, sortingId));
			}
		}
	}
	
	@Override
	protected void clearConversation()
	{
		super.clearConversation();

		if (isShowingPin())
		{
			hidePinFromUI(true);
			Utils.resetPinUnreadCount(oneToNConversation);
			updateUnreadPinCount();
		}
	}
	
	@Override
	protected ConvMessage createConvMessageFromCompose()
	{
		ConvMessage convMessage = super.createConvMessageFromCompose();
		if (convMessage != null)
		{
			if (ChatThreadUtils.checkMessageTypeFromHash(activity.getApplicationContext(), convMessage, HASH_PIN))
			{
				Logger.d(TAG, "Found a pin message type");
				if (TextUtils.isEmpty(convMessage.getMessage()))
				{
					Toast.makeText(activity, R.string.text_empty_error, Toast.LENGTH_SHORT).show();
					return null;
				}

				ChatThreadUtils.modifyMessageToPin(activity.getApplicationContext(), convMessage);
			}
		}
		return convMessage;
	}
	
	@Override
	protected void handleActionModeOrientationChange(int whichActionMode)
	{
		switch (whichActionMode)
		{
		case PIN_CREATE_ACTION_MODE:
			mActionMode.reInflateActionMode();
			break;
		default:
			super.handleActionModeOrientationChange(whichActionMode);
		}
	}
	
	@Override
	public void actionModeDestroyed(int id)
	{
		switch (id)
		{
		case PIN_CREATE_ACTION_MODE:
			destroyPinCreateView();
			HikeAnalyticsEvent.recordAnalyticsForGCPins(ChatAnalyticConstants.GCEvents.GC_PIN_CANCEL, null, null, null);
			break;

		default:
			super.actionModeDestroyed(id);
		}
	}
	
	@Override
	public void onClick(View v)
	{
		Logger.i(TAG, "onclick of view " + v.getId());
		switch (v.getId())
		{
		case R.id.cross: // Pin message bar cross
			HikeAnalyticsEvent.recordAnalyticsForGCPins(ChatAnalyticConstants.GCEvents.GC_PIN_CROSS, null, null, null);
			hidePin();
			break;
		default:
			super.onClick(v);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		switch (v.getId())
		{
		case R.id.messageedittext:
			return mShareablePopupLayout.onEditTextTouch(v, event);
		default:
			return super.onTouch(v, event);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() != android.R.id.home)
			toastForGroupEnd();
		if (!checkForDeadOrBlocked())
		{
			if(item.getItemId() != android.R.id.home) {
				if(isWalkieTalkieShowing()) return true;
			}
			switch (item.getItemId())
			{
			case R.id.voip_call:

				// Make a group voip call after confirmation
				new AlertDialog.Builder(activity).
				setTitle(R.string.voip_conference_label).
				setMessage(R.string.voip_group_conference_confirmation).
				setPositiveButton(R.string.call, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Map<String, PairModified<GroupParticipant, String>> groupParticipants = oneToNConversation.getConversationParticipantList();
						ArrayList<String> msisdns = new ArrayList<String>();
						
						for (PairModified<GroupParticipant, String> groupParticipant : groupParticipants.values())
						{
							String msisdn = groupParticipant.getFirst().getContactInfo().getMsisdn();
							msisdns.add(msisdn);
						}
						
						// Launch VoIP service
						Intent intent = IntentFactory.getVoipCallIntent(activity.getApplicationContext(), 
								msisdns, msisdn, VoIPUtils.CallSource.CHAT_THREAD);
						if (intent != null)
							activity.getApplicationContext().startService(intent);
					}
				}).
				setNegativeButton(R.string.cancel, null).show();
				
				break;
			}
			return super.onOptionsItemSelected(item);
		}
		
		return false;
	}
	
	
	private boolean checkForDeadOrBlocked()
	{
		return checkForBlocked() ||checkForDead();
	}
	
	private boolean checkForDead()
	{
	    	return !oneToNConversation.isConversationAlive();
	}
	
	private boolean checkForBlocked()
	{
			return oneToNConversation.isBlocked();
	
	}

	@Override
	protected void setupActionBar(boolean firstInflation)
	{
		if (mCurrentActionMode == PIN_CREATE_ACTION_MODE)
		{
			showPinCreateView(mComposeView.getText().toString());
		}
		super.setupActionBar(firstInflation);
	}

	private void showPinCreateView(String pinText)
	{
		if (mActionMode.whichActionModeIsOn() == PIN_CREATE_ACTION_MODE)
		{
			return;
		}
		mActionMode.showActionMode(PIN_CREATE_ACTION_MODE, getString(R.string.create_pin), getString(R.string.pin), HikeActionMode.DEFAULT_LAYOUT_RESID);
		// TODO : dismissPopupWindow was here : gaurav

		removeKeyboardShutdownIfShowing();
		View content = activity.findViewById(R.id.impMessageCreateView);
		content.setVisibility(View.VISIBLE);
		mComposeView = (CustomFontEditText) content.findViewById(R.id.messageedittext);
		mComposeView.requestFocus();
		if (mEmoticonPicker != null)
		{
			mEmoticonPicker.updateET(mComposeView);
		}

		View mBottomView = activity.findViewById(R.id.bottom_panel);
		if (mShareablePopupLayout.isKeyboardOpen())
		{
			// ifkeyboard is not open, then keyboard will come which will make so much animation on screen
			mBottomView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.up_down_lower_part));
		}
		
		else //Show keyboard
		{
			Utils.toggleSoftKeyboard(activity.getApplicationContext());
		}

		mBottomView.setVisibility(View.GONE);

		playPinCreateViewAnim();

		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}

		mComposeView.setOnTouchListener(this);
		mComposeView.addTextChangedListener(new EmoticonTextWatcher());
		mComposeView.requestFocus();
		if (!TextUtils.isEmpty(pinText))
		{
			mComposeView.setText(pinText);
			mComposeView.setSelection(pinText.length());
		}else{
			mComposeView.setText("");
		}
		
//		activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		content.findViewById(R.id.emo_btn).setOnClickListener(this);
	}
	
	private void playPinCreateViewAnim()
	{
		final View view = activity.findViewById(R.id.impMessageCreateView);

		if (view == null)
		{
			return;
		}

		Animation am = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.up_down_fade_in);
		am.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
				if (isShowingPin())
				{
					pinView.setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
			}
		});
		view.startAnimation(am);
	}

	private void playPinCreateDestroyViewAnim()
	{
		final View view = activity.findViewById(R.id.impMessageCreateView);

		if (view == null)
		{
			return;
		}

		Animation an = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
			}
		});

		view.startAnimation(an);
	}
	
	/**
	 * Utility method used for sending Pin
	 */

	private void sendPin()
	{
		ConvMessage message = createConvMessageFromCompose();
		if (message != null)
		{
			ChatThreadUtils.modifyMessageToPin(activity.getApplicationContext(), message);
			sendMessage(message);
			mActionMode.finish();
		}
	}
	
	@Override
	public void doneClicked(int id)
	{
		if (id == PIN_CREATE_ACTION_MODE)
		{
			sendPin();
		}
	}
	
	@Override
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			addMessage(convMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);

			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				recordPinMessage(convMessage.getHashMessage() == HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE);
			}
		}
	}
	
	private void recordPinMessage(boolean viaPinIcon)
	{
		HAManager.getInstance().record(viaPinIcon ? HikeConstants.LogEvent.PIN_POSTED_VIA_ICON : HikeConstants.LogEvent.PIN_POSTED_VIA_HASH_PIN, AnalyticsConstants.UI_EVENT,
				AnalyticsConstants.CLICK_EVENT);
	}
	
	private void destroyPinCreateView()
	{
		// AFTER PIN MODE, we make sure mComposeView is reinitialized to message composer compose
		mComposeView = (CustomFontEditText) activity.findViewById(R.id.msg_compose);
		if (mEmoticonPicker != null)
		{
			mEmoticonPicker.updateET(mComposeView);
		}
		mComposeView.requestFocus();

		View mBottomView = activity.findViewById(R.id.bottom_panel);
		mBottomView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.down_up_lower_part));
		mBottomView.setVisibility(View.VISIBLE);
		
		/**
		 * If the pin had been hidden while pinCreate view was shown, now is the best time to make it visible again.
		 */
		if (wasPinHidden())
		{
			pinView.setVisibility(View.VISIBLE);
		}

		playPinCreateDestroyViewAnim();

		if (mShareablePopupLayout != null && mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
	}
	
	
	private boolean isShowingPin()
	{
		return pinView != null && pinView.getVisibility() == View.VISIBLE;
	}

	private void hidePinFromUI(boolean playAnim)
	{
		if (playAnim)
		{
			ChatThreadUtils.playUpDownAnimation(activity.getApplicationContext(), pinView);
		}
		else
		{
			pinView.setVisibility(View.GONE);
		}

		pinView = null;
	}
	
	private void showPinHistory(boolean viaMenu)
	{
		HikeAnalyticsEvent.recordAnalyticsForGCPins(ChatAnalyticConstants.GCEvents.GC_PIN_HISTORY, null, ChatAnalyticConstants.GCEvents.GC_PIN_HISTORY_SRC_CHATTHREAD, null);
		Intent intent = IntentFactory.getPinHistoryIntent(activity.getApplicationContext(), msisdn);
		activity.startActivity(intent);
		Utils.resetPinUnreadCount(oneToNConversation);

		HAManager.getInstance().record(viaMenu ? HikeConstants.LogEvent.PIN_HISTORY_VIA_MENU : HikeConstants.LogEvent.PIN_HISTORY_VIA_PIN_CLICK, AnalyticsConstants.UI_EVENT,
				AnalyticsConstants.CLICK_EVENT);
	}
	
	private boolean wasPinHidden()
	{
		return pinView != null && pinView.getVisibility() == View.GONE;
	}

	/**
	 * @param impMessage
	 *            -- ConvMessage to stick to top
	 */
	protected void showStickyMessageAtTop(ConvMessage impMessage, boolean playPinAnim)
	{
		// Hiding pin tip if open
		if (impMessage == null)
		{
			Logger.wtf(TAG, "Trying to showStickyPinMessage on a null ConvMessage object");
			return;
		}

		boolean wasPinViewInflated = false;
		if (impMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			if (pinView == null) // Inflate only if pinView is null
			{
				pinView = LayoutInflater.from(activity).inflate(R.layout.imp_message_text_pin, null);
				wasPinViewInflated = true;
			}
		}

		else
		{
			Logger.wtf(TAG, "got imp message but type is unnknown , type " + impMessage.getMessageType());
			return;
		}

		TextView text = (TextView) pinView.findViewById(R.id.text);
		if (impMessage.getMetadata() != null && impMessage.getMetadata().isGhostMessage())
		{
			pinView.findViewById(R.id.main_content).setBackgroundResource(R.drawable.pin_bg_black);
			text.setTextColor(getResources().getColor(R.color.gray));
		}
		String name = "";
		if (impMessage.isSent())
		{
			name = getString(R.string.pin_self) + PIN_MESSAGE_SEPARATOR;
		}
		else
		{
			name = oneToNConversation.getConvParticipantFirstNameAndSurname(impMessage.getGroupParticipantMsisdn()) + PIN_MESSAGE_SEPARATOR;
		}

		ForegroundColorSpan fSpan = new ForegroundColorSpan(getResources().getColor(R.color.pin_name_color));
		String str = name + impMessage.getMessage();
		SpannableString spanStr = new SpannableString(str);
		spanStr.setSpan(fSpan, 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spanStr.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.pin_text_color)), name.length(), str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		CharSequence markedUp = spanStr;
		SmileyParser smileyParser = SmileyParser.getInstance();
		markedUp = smileyParser.addSmileySpans(markedUp, false);
		text.setText(markedUp);
		Linkify.addLinks(text, Linkify.ALL);
		Linkify.addLinks(text, Utils.shortCodeRegex, "tel:");
		text.setMovementMethod(new LinkMovementMethod()
		{
			@Override
			public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
			{
				boolean result = super.onTouchEvent(widget, buffer, event);
				if (!result)
				{
					showPinHistory(false);
				}
				return result;
			}
		});
		// text.setText(spanStr);

		View cross = pinView.findViewById(R.id.cross);
		cross.setOnClickListener(this);

		pinView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showPinHistory(false);
			}
		});

		LinearLayout ll = ((LinearLayout) activity.findViewById(R.id.impMessageContainer));
		if (ll.getChildCount() > 0)
		{
			ll.removeAllViews();
		}
		try {
			ll.addView(pinView, 0);
		} catch (Exception e) {
			throw new IllegalStateException("Exception during adding of pin message, parent's child count  :"+ll.getChildCount(), e);
		}

		/**
		 * If we were composing a new pin and a pin arrives from somewhere else Then we hide the received pin. The pin view will be made visible when pinCreateView is Destroyed.
		 */

		if (activity.findViewById(R.id.impMessageCreateView).getVisibility() == View.VISIBLE)
		{
			pinView.setVisibility(View.GONE);
			wasPinViewInflated = false; // To avoid animation, since the message create view will be destroyed soon.
		}

		if (playPinAnim && wasPinViewInflated) // If we're inflating pin for the first time, then we animate it.
		{
			ChatThreadUtils.playPinUpAnimation(activity.getApplicationContext(), pinView, R.anim.up_down_fade_in);
		}

		// decrement the unread count if message pinned
		ChatThreadUtils.decrementUnreadPInCount(mConversation, isActivityVisible);

	}

	private void hidePin()
	{
		hidePinFromUI(true);
		OneToNConversationMetadata metadata = (OneToNConversationMetadata) mConversation.getMetadata();
		try
		{
			metadata.setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, false);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
	}

	/**
	 * Used to set unread pin count
	 */
	protected void updateUnreadPinCount()
	{
		if (oneToNConversation != null)
		{
			int unreadPinCount = oneToNConversation.getUnreadPinnedMessageCount();
			mActionBar.updateOverflowMenuIndicatorCount(unreadPinCount);
			mActionBar.updateOverflowMenuItemCount(R.string.group_profile, unreadPinCount);
		}
	}
	
	/**
	 * Called from the pubSub thread
	 * 
	 * @param object
	 */
	private void onLatestPinDeleted(Object object)
	{
		long msgId = (Long) object;

		try
		{
			long pinIdFromMetadata = oneToNConversation.getMetadata().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);

			if (msgId == pinIdFromMetadata)
			{
				sendUIMessage(LATEST_PIN_DELETED, true);
			}
		}

		catch (JSONException e)
		{
			Logger.wtf(TAG, "Got an exception during the pubSub : onLatestPinDeleted " + e.toString());
		}

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
			case R.string.create_pin:
			case R.string.group_profile:
			case R.string.chat_theme:
				overFlowMenuItem.enabled = !checkForDeadOrBlocked();
				break;
			case R.string.mute_group:
				overFlowMenuItem.enabled = !checkForDeadOrBlocked();
				overFlowMenuItem.text = oneToNConversation.isMuted() ? activity.getString(R.string.unmute_group) : activity.getString(R.string.mute_group);
				break;
			}
		}
	}

	private void toastForGroupEnd() {
		if(checkForDead()){
			Toast.makeText(activity.getApplicationContext(), getString(R.string.group_chat_end), Toast.LENGTH_SHORT).show();
		}else if(checkForBlocked()){
			String label = oneToNConversation.getConversationParticipantName(oneToNConversation.getConversationOwner());
			Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.block_overlay_message, label), Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void fetchConversationFailed()
	{
		/* the user must have deleted the chat. */
		Message message = Message.obtain();
		message.what = SHOW_TOAST;
		message.obj = R.string.invalid_group_chat;
		uiHandler.sendMessage(message);

		startHomeActivity();
		
		super.fetchConversationFailed();
	}

	@Override
	protected void setupSearchMode(String searchText)
	{
		if (isShowingPin())
		{
			pinView.setVisibility(View.GONE);
		}
		super.setupSearchMode(searchText);
	}

	@Override
	protected void destroySearchMode()
	{
		if (wasPinHidden())
		{
			pinView.setVisibility(View.VISIBLE);
		}
		if(!oneToNConversation.isConversationAlive()){
			Utils.hideSoftKeyboard(activity, mComposeView);
		}
		super.destroySearchMode();
	}
	
	@Override
	protected boolean shouldShowKeyboardInActionMode() {
	    return (super.shouldShowKeyboardInActionMode() || mActionMode.whichActionModeIsOn() == PIN_CREATE_ACTION_MODE);
	}

	@Override
	protected void initKeyboardOffBoarding() {
		if(!checkForDeadOrBlocked())
			super.initKeyboardOffBoarding();
	}
}
