package com.bsb.hike.ui.fragments;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.support.v4.app.ListFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter.FilterListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.adapters.EmptyConversationsAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.MessagingBotConfiguration;
import com.bsb.hike.bots.MessagingBotMetadata;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTApkManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.Conversation.ConversationTip.ConversationTipClickedListener;
import com.bsb.hike.models.Conversation.OneToNConvInfo;
import com.bsb.hike.models.EmptyConversationContactItem;
import com.bsb.hike.models.EmptyConversationFtueCardItem;
import com.bsb.hike.models.EmptyConversationItem;
import com.bsb.hike.models.Mute;
import com.bsb.hike.models.NUXChatReward;
import com.bsb.hike.models.NUXTaskDetails;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.DisconnectFragmentType;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.productpopup.AtomicTipManager;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.HikeFragmentable;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.fragments.OfflineDisconnectFragment.OfflineConnectionRequestListener;
import com.bsb.hike.ui.utils.LockPattern;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConversationFragment extends ListFragment implements OnItemLongClickListener, Listener, OnScrollListener, HikeFragmentable, OnClickListener,
		ConversationTipClickedListener, FilterListener
{
	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MESSAGE_DELIVERED,
			HikePubSub.NEW_CONVERSATION, HikePubSub.MESSAGE_SENT, HikePubSub.MSG_READ, HikePubSub.ICON_CHANGED, HikePubSub.ONETONCONV_NAME_CHANGED, HikePubSub.CONTACT_ADDED,
			HikePubSub.LAST_MESSAGE_DELETED, HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.GROUP_LEFT, HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED,
			HikePubSub.CLEAR_CONVERSATION, HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, HikePubSub.REMOVE_TIP, HikePubSub.SHOW_TIP, HikePubSub.STEALTH_MODE_TOGGLED,
			HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.ONETON_MESSAGE_DELIVERED_READ, HikePubSub.BULK_MESSAGE_DELIVERED_READ, HikePubSub.GROUP_END, HikePubSub.CONTACT_DELETED,
			HikePubSub.MULTI_MESSAGE_DB_INSERTED, HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.CONV_UNREAD_COUNT_MODIFIED,
			HikePubSub.CONVERSATION_TS_UPDATED, HikePubSub.PARTICIPANT_JOINED_ONETONCONV, HikePubSub.PARTICIPANT_LEFT_ONETONCONV, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER,
			HikePubSub.CONVERSATION_DELETED, HikePubSub.DELETE_THIS_CONVERSATION, HikePubSub.ONETONCONV_NAME_CHANGED, HikePubSub.STEALTH_CONVERSATION_MARKED,
			HikePubSub.STEALTH_CONVERSATION_UNMARKED, HikePubSub.UPDATE_LAST_MSG_STATE, HikePubSub.OFFLINE_MESSAGE_SENT, HikePubSub.ON_OFFLINE_REQUEST,HikePubSub.GENERAL_EVENT,HikePubSub.GENERAL_EVENT_STATE_CHANGE,HikePubSub.LASTMSG_UPDATED};

	private ConversationsAdapter mAdapter;

	private HashMap<String, ConvInfo> mConversationsByMSISDN;

	private HashSet<String> mConversationsAdded;

	private Comparator<? super ConvInfo> mConversationsComparator;

	private View emptyView;

	private View searchEmptyView;

	private ViewGroup emptyHolder;

	private Set<ConvInfo> stealthConversations;

	private List<ConvInfo> displayedConversations;

	private boolean showingWelcomeHikeConvTip = false;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;

	private View inviteFooter;

	private LinearLayout llChatReward, llInviteOptions, llNuxFooter, llShadow;

	private ImageView footercontroller, lockImage;

	private HoloCircularProgress progressNux;

	private Button butInviteMore, butRemind;

	private TextView chatProgress, rewardCard;

	public boolean searchMode = false;

	private String searchText;

	private ConversationTip convTip;

	private View tipView;

	private AlertDialog alertDialog;

	private int tipType = ConversationTip.NO_TIP;
	
	protected static final int START_OFFLINE_CONNECTION = 1;

	protected static final int STEALTH_CONVERSATION_TOGGLE = 2;

	private enum hikeBotConvStat
	{
		NOTVIEWED, VIEWED, DELETED
	};

	public enum footerState
	{
		OPEN(0), HALFOPEN(1), CLOSED(2);

		private static footerState val = CLOSED;

		private footerState(int value)
		{
		}

		public static footerState getEnum()
		{
			return val;
		}

		public static void setEnumState(footerState value)
		{
			Logger.d("footer", "Footer state set = " + value + "");
			val = value;

		}
	};

	View parent;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);
		parent = inflater.inflate(R.layout.conversations, null);
		return parent;
	}

	private void setSearchEmptyState()
	{
		String emptyText = String.format(getActivity().getString(R.string.home_search_empty_text), searchText);
		TextView emptyTextView = (TextView) getView().findViewById(R.id.searchEmptyView).findViewById(R.id.empty_search_txt);
		if (!TextUtils.isEmpty(searchText))
		{
			SpannableString spanEmptyText = new SpannableString(emptyText);
			String darkText = "'" + searchText + "'";
			int start = spanEmptyText.toString().indexOf(darkText);
			int end = start + darkText.length();
			spanEmptyText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.standard_light_grey2)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			emptyTextView.setText(spanEmptyText, TextView.BufferType.SPANNABLE);
		}
		else
		{
			emptyTextView.setText(emptyText);
		}
	}

	private void bindNuxViews(final View root)
	{
		footercontroller = (ImageView) root.findViewById(R.id.imgv_nux);
		footercontroller.setOnClickListener(this);

		llShadow = (LinearLayout) root.findViewById(R.id.ll_shadow);
		lockImage = (ImageView) root.findViewById(R.id.nux_lock);
		llNuxFooter = (LinearLayout) root.findViewById(R.id.ll_footer);

		llChatReward = (LinearLayout) root.findViewById(R.id.ll_chatReward);
		llChatReward.setOnClickListener(this);
		llInviteOptions = (LinearLayout) root.findViewById(R.id.ll_buttons);
		chatProgress = (TextView) root.findViewById(R.id.tv_chatStatus);
		chatProgress.setOnClickListener(this);

		butInviteMore = (Button) root.findViewById(R.id.but_inviteMore);
		butRemind = (Button) root.findViewById(R.id.but_remind);

		butInviteMore.setOnClickListener(this);
		butRemind.setOnClickListener(this);
		progressNux = (HoloCircularProgress) root.findViewById(R.id.nux_progress);

		rewardCard = (TextView) root.findViewById(R.id.tv_chatReward);

		changeFooterState();
		fillNuxFooterElements();

	}

	/**
	 * Changing footer state on Activity Created
	 */
	private void changeFooterState()
	{

		footercontroller.post(new Runnable()
		{

			@Override
			public void run()
			{
				if (isAdded())
				{
					getListView().setPadding(0, 0, 0, footercontroller.getHeight());
				}
			}
		});
		Logger.d("footer", "changeFooterState");
		if (!NUXManager.getInstance().isReminderReceived())
		{
			llInviteOptions.post(new Runnable()
			{

				@Override
				public void run()
				{
					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llChatReward.getHeight() + llInviteOptions.getHeight()).setDuration(0).start();

				}
			});
		}
		else
		{

			footerState.setEnumState(footerState.OPEN);

			/**
			 * Check that reminder ==normal or not
			 */

			if (NUXManager.getInstance().wantToInfalte())
			{
				setFooterHalfOpen();
			}
			else
			{
				changeFooterControllerBackground(footerState.OPEN);
			}
			NUXManager.getInstance().reminderShown();
		}
	}

	/**
	 * Function to change the footer image drawable on changing state -->upArraw and DownArror
	 * 
	 * @param state
	 */
	public void changeFooterControllerBackground(footerState state)
	{
		switch (footerState.getEnum())
		{
		case OPEN:
			footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_downarrow));
			break;
		case HALFOPEN:
			NUXManager nm = NUXManager.getInstance();
			NuxSelectFriends mmSelectFriends = NUXManager.getInstance().getNuxSelectFriendsPojo();
			// if (!(mmSelectFriends.isModuleToggle() || nm.getCurrentState() == NUXConstants.COMPLETED))
			// {
			// footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_uparrow));
			// }
			// else
			// {
			footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_downarrow));
			// }
			break;
		case CLOSED:
			footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_uparrow));
			break;

		}
	}

	private void fillNuxFooterElements()
	{
		NUXManager mmNuxManager = NUXManager.getInstance();
		NUXTaskDetails mmDetails = mmNuxManager.getNuxTaskDetailsPojo();
		NUXChatReward mmReward = mmNuxManager.getNuxChatRewardPojo();
		NuxSelectFriends mmSelectFriends = mmNuxManager.getNuxSelectFriendsPojo();

		// Setting lock icon image 0 unlocked-->red ,>0<Min->orange, ==Min&&state=completed -->Green

		if (mmNuxManager.getCountUnlockedContacts() == 0)
		{

			chatProgress.setTextColor(getResources().getColor(R.color.red_color_span));
			lockImage.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_lock_red));

		}
		else
		{
			chatProgress.setTextColor(getResources().getColor(R.color.nux_chat_reward_status));
			lockImage.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_lock_orange));
		}

		// Module toggle true:no select friends and custom message screen shown hence hiding the buttons and the making the chat reward not tappable
		if (mmSelectFriends.isModuleToggle())
		{
			llChatReward.setOnClickListener(null);
			chatProgress.setText(NUXManager.getInstance().getNuxChatRewardPojo().getDetailsText());
			setFooterHalfOpen();
		}

		butInviteMore.setText(mmReward.getInviteMoreButtonText());

		butRemind.setText(mmReward.getRemindButtonText());

		rewardCard.setText(String.format(mmReward.getRewardCardText(), mmDetails.getMin()));

		// NUX Skipped state ...

		if (mmNuxManager.getCurrentState() == NUXConstants.NUX_SKIPPED)
		{
			butRemind.setVisibility(View.GONE);
			butInviteMore.setText(mmReward.getSelectFriendsText());
		}

		if (mmNuxManager.getCurrentState() == NUXConstants.NUX_IS_ACTIVE)
		{
			butRemind.setVisibility(View.VISIBLE);

			// condition when the total contacts selected = max,then remind button should show and invite more should be hidden

			if (mmNuxManager.getCountLockedContacts() + mmNuxManager.getCountUnlockedContacts() == mmDetails.getMax())
			{
				butInviteMore.setVisibility(View.GONE);

			}
		}

		if (!(mmNuxManager.getCurrentState() == NUXConstants.COMPLETED))
		{
			progressNux.setProgress(NUXManager.getInstance().getCountUnlockedContacts() / ((float) mmDetails.getMin()));

			if (footerState.getEnum() == footerState.HALFOPEN)
			{
				if (mmSelectFriends.isModuleToggle())
				{
					chatProgress.setText(mmReward.getDetailsText());
				}
				else
				{
					chatProgress.setText(String.format(mmReward.getStatusText(), mmNuxManager.getCountUnlockedContacts(), mmDetails.getMin()));
				}
			}
			else
			{
				chatProgress.setText(mmReward.getDetailsText());
			}
		}
		else
		{
			// Reward Unlocked.

			lockImage.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_nux_unlocked));

			chatProgress.setText(mmReward.getTapToClaimText());
			chatProgress.setTextColor(getResources().getColor(R.color.nuxunlocked));
			rewardCard.setText(String.format(mmReward.getRewardCardSuccessText(), mmDetails.getIncentiveAmount()));

			Animation pulse = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);

			// lockImage.startAnimation(pulse);

			// Changing the footer state to halfOpen when the reward is unlocked.
			setFooterHalfOpen();

			// llChatReward.setOnClickListener(null);

			progressNux.setVisibility(View.GONE);
		}
	}

	public void setFooterHalfOpen()
	{
		if (footerState.getEnum() == footerState.OPEN)
		{
			llInviteOptions.post(new Runnable()
			{

				@Override
				public void run()
				{
					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).setDuration(0).start();

				}
			});

			chatProgress.setText(NUXManager.getInstance().getNuxChatRewardPojo().getDetailsText());

			footerState.setEnumState(footerState.HALFOPEN);
			changeFooterControllerBackground(footerState.HALFOPEN);
		}
	}

	@Override
	public void onClick(View v)
	{
		NUXManager mmNuxManager = NUXManager.getInstance();
		NUXTaskDetails mmDetails = mmNuxManager.getNuxTaskDetailsPojo();
		NUXChatReward mmReward = mmNuxManager.getNuxChatRewardPojo();
		switch (v.getId())
		{
		case R.id.imgv_nux:

			switch (footerState.getEnum())
			{
			case OPEN:

				/**
				 * 
				 * When the footer is in open state
				 */
				Logger.d("Footer", "open");

				ObjectAnimator.ofFloat(llNuxFooter, "translationY", llChatReward.getHeight() + llInviteOptions.getHeight()).start();

				footerState.setEnumState(footerState.CLOSED);
				changeFooterControllerBackground(footerState.CLOSED);

				try
				{
					JSONObject metaData = new JSONObject();
					metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_EXPANDED_COM);
					NUXManager.getInstance().sendAnalytics(metaData);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				break;
			case HALFOPEN:

				ObjectAnimator.ofFloat(llNuxFooter, "translationY", llNuxFooter.getHeight() - footercontroller.getHeight() - llShadow.getHeight()).start();
				footerState.setEnumState(footerState.CLOSED);
				changeFooterControllerBackground(footerState.CLOSED);

				try
				{
					JSONObject metaData = new JSONObject();
					metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_FOOTER_NOR_COM);
					NUXManager.getInstance().sendAnalytics(metaData);
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case CLOSED:

				// When footer is in closed state
				Logger.d("Footer", "closed");

				// Will remove after testing

				if (mmNuxManager.getCurrentState() == NUXConstants.NUX_SKIPPED || mmNuxManager.getCurrentState() == NUXConstants.NUX_IS_ACTIVE)
				{
					if(NUXManager.getInstance().getNuxSelectFriendsPojo().isModuleToggle())
					{
						chatProgress.setText(mmReward.getDetailsText());
					}
					else
					{
						chatProgress.setText(String.format(mmReward.getStatusText(), mmNuxManager.getCountUnlockedContacts(), mmDetails.getMin()));
					}
					progressNux.setProgress(NUXManager.getInstance().getCountUnlockedContacts() / mmDetails.getMin());
				}
				ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).start();

				footerState.setEnumState(footerState.HALFOPEN);
				changeFooterControllerBackground(footerState.HALFOPEN);

				try
				{
					JSONObject metaData = new JSONObject();
					metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_FOOTER_COM_NOR);
					NUXManager.getInstance().sendAnalytics(metaData);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			break;
		case R.id.ll_chatReward:

			/**
			 * On click of Reward Bar to open the footer to expanded view.
			 */

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED || mmNuxManager.getCurrentState() == NUXConstants.NUX_NEW)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
			}
			else if (mmNuxManager.getCurrentState() == NUXConstants.COMPLETED)
			{
				onClick(chatProgress);
			}
			else
			{
				chatProgress.setText(NUXManager.getInstance().getNuxChatRewardPojo().getDetailsText());
				ObjectAnimator.ofFloat(llNuxFooter, "translationY", 0).start();

				footerState.setEnumState(footerState.OPEN);
				changeFooterControllerBackground(footerState.OPEN);

				try
				{
					JSONObject metaData = new JSONObject();
					metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_FOOTER_NOR_EXP);
					NUXManager.getInstance().sendAnalytics(metaData);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
			break;

		case R.id.tv_chatStatus:

			// On click of TAP To Claim and View Details

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED || mmNuxManager.getCurrentState() == NUXConstants.NUX_NEW)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
				break;
			}

			if (NUXManager.getInstance().getCurrentState() == NUXConstants.COMPLETED)
			{

				if ((!TextUtils.isEmpty(mmReward.getTapToClaimLink())))
				{

					try
					{
						JSONObject metaData = new JSONObject();
						metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_TAP_CLAIM);
						NUXManager.getInstance().sendAnalytics(metaData);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
					String tapToClaim = HikeConstants.ANDROID + "/" + mprefs.getData(HikeMessengerApp.REWARDS_TOKEN, "");
					String title = getString(R.string.hike);

					String link = String.format(mmReward.getTapToClaimLink(), tapToClaim);
					Utils.startWebViewActivity(getActivity(), link, title);

				}
			}

			else if (footerState.getEnum() == footerState.OPEN || (footerState.getEnum() == footerState.HALFOPEN && NUXManager.getInstance().getNuxSelectFriendsPojo().isModuleToggle()))
			{
				if ((!TextUtils.isEmpty(mmReward.getDetailsLink())))
				{

					try
					{
						JSONObject metaData = new JSONObject();
						metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_VIEW_MORE);
						NUXManager.getInstance().sendAnalytics(metaData);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
					String tapToClaim = HikeConstants.ANDROID + "/" + mprefs.getData(HikeMessengerApp.REWARDS_TOKEN, "");
					String title = getString(R.string.hike);
					String link = String.format(mmReward.getDetailsLink(), tapToClaim);
					Utils.startWebViewActivity(getActivity(), link, title);
				}
			}
			break;
		case R.id.but_remind:
			try
			{
				JSONObject metaData = new JSONObject();
				metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_REMIND);
				NUXManager.getInstance().sendAnalytics(metaData);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED || mmNuxManager.getCurrentState() == NUXConstants.NUX_NEW)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
				break;

			}

			if (mmNuxManager.getCurrentState() == NUXConstants.COMPLETED)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_completed), Toast.LENGTH_SHORT).show();
				break;
			}
			Intent in = IntentFactory.openNuxCustomMessage(getActivity());
			getActivity().startActivity(in);
			break;
		case R.id.but_inviteMore:

			try
			{
				JSONObject metaData = new JSONObject();
				metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_INVITE_MORE);
				metaData.put(NUXConstants.OTHER_STRING, butInviteMore.getText().toString());
				NUXManager.getInstance().sendAnalytics(metaData);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED || mmNuxManager.getCurrentState() == NUXConstants.NUX_NEW)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
				break;

			}
			if (mmNuxManager.getCurrentState() == NUXConstants.COMPLETED)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_completed), Toast.LENGTH_SHORT).show();
				break;
			}
			NUXManager.getInstance().startNuxSelector(getActivity());
			break;

		}
	}

	private void setEmptyState(boolean isConvScreenEmpty)
	{
		// Adding wasViewSetup() safety check for an NPE here.
		if (!wasViewSetup())
		{
			return;
		}

		searchEmptyView = getView().findViewById(R.id.searchEmptyView);
		emptyHolder = (ViewGroup) getView().findViewById(R.id.emptyViewHolder);

		if (!isConvScreenEmpty)
		{
			searchEmptyView.setVisibility(View.GONE);
			emptyHolder.setVisibility(View.GONE);
			return;
		}

		if (searchMode && !TextUtils.isEmpty(searchText))
		{
			emptyHolder.setVisibility(View.GONE);
			searchEmptyView.setVisibility(View.VISIBLE);
			setSearchEmptyState();
		}
		else
		{
			searchEmptyView.setVisibility(View.GONE);
			emptyHolder.setVisibility(View.VISIBLE);
		}

	}

	private void setupFTUEEmptyView()
	{

		if (emptyView == null || !isAdded() || searchMode)
		{
			return;
		}

		ListView ftueListView = (ListView) emptyView.findViewById(R.id.ftue_list);
		List<EmptyConversationItem> ftueListItems = new ArrayList<EmptyConversationItem>();

		if (!HomeActivity.ftueContactsData.getHikeContacts().isEmpty())
		{
			int hikeContactCount = HomeActivity.ftueContactsData.getTotalHikeContactsCount();
			EmptyConversationItem hikeContactsItem = new EmptyConversationContactItem(HomeActivity.ftueContactsData.getHikeContacts(), getResources().getString(
					R.string.ftue_hike_contact_card_header, hikeContactCount), EmptyConversationItem.HIKE_CONTACTS);
			ftueListItems.add(hikeContactsItem);
		}
		/*
		 * We only add this item if hike contacts are less than certain threashold
		 */
		if (HomeActivity.ftueContactsData.getHikeContacts().size() == 0 && !HomeActivity.ftueContactsData.getSmsContacts().isEmpty())
		{
			int smsContactCount = HomeActivity.ftueContactsData.getTotalSmsContactsCount();
			EmptyConversationItem hikeContactsItem = new EmptyConversationContactItem(HomeActivity.ftueContactsData.getSmsContacts(), getResources().getString(
					R.string.ftue_sms_contact_card_header, smsContactCount), EmptyConversationItem.SMS_CONTACTS);
			ftueListItems.add(hikeContactsItem);
		}
		if (ftueListView != null)
		{
			if (ftueListView.getFooterViewsCount() == 0)
			{
				addBottomPadding(ftueListView);
			}
			if (HomeActivity.ftueContactsData.getHikeContacts().isEmpty())
			{
				addFtueCards(ftueListItems);
			}
			ftueListView.setAdapter(new EmptyConversationsAdapter(getActivity(), -1, ftueListItems));
		}
	}

	private void addFtueCards(List<EmptyConversationItem> ftueListItems)
	{
		ftueListItems.add(new EmptyConversationItem(EmptyConversationItem.SEPERATOR));

		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.LAST_SEEN, R.drawable.ftue_card_last_seen_img_small, getResources().getColor(
				R.color.ftue_card_last_seen), R.string.ftue_card_header_last_seen, R.string.ftue_card_body_last_seen, R.string.ftue_card_click_text_last_seen, getResources()
				.getColor(R.color.ftue_card_last_seen_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.GROUP, R.drawable.ftue_card_group_img_small, getResources().getColor(R.color.ftue_card_group),
				R.string.group_chat, R.string.ftue_card_body_group, R.string.ftue_card_click_group, getResources().getColor(R.color.ftue_card_group_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.INVITE, R.drawable.ftue_card_invite_img_small, getResources().getColor(R.color.ftue_card_invite),
				R.string.invite_friends, R.string.ftue_card_body_invite, R.string.ftue_card_click_invite, getResources().getColor(R.color.ftue_card_invite_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.HIKE_OFFLINE, R.drawable.ftue_card_hike_offline_img_small, getResources().getColor(
				R.color.ftue_card_hike_offline), R.string.ftue_card_header_hike_offline, R.string.ftue_card_body_hike_offline, R.string.ftue_card_click_text_hike_offline,
				getResources().getColor(R.color.ftue_card_hike_offline_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.STICKERS, R.drawable.ftue_card_sticker_img_small, getResources().getColor(
				R.color.ftue_card_sticker), R.string.ftue_card_header_sticker, R.string.ftue_card_body_sticker, R.string.ftue_card_click_text_sticker, getResources().getColor(
				R.color.ftue_card_sticker_click_text)));
	}

	/*
	 * We are adding this footer in empty state list view to give proper padding at the bottom of the list.
	 */
	private void addBottomPadding(ListView ftueListView)
	{
		View paddingView = LayoutInflater.from(getActivity()).inflate(R.layout.ftue_list_padding_footer_view, null);
		ftueListView.addFooterView(paddingView);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		Logger.d("footer", "onActivityCreated");
		if (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_IS_ACTIVE || (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_SKIPPED)
				|| (NUXManager.getInstance().getCurrentState() == NUXConstants.COMPLETED))
		{
			ViewStub mmStub = (ViewStub) parent.findViewById(R.id.nux_footer);
			mmStub.setLayoutResource(R.layout.nux_footer);

			mmStub.setOnInflateListener(new OnInflateListener()
			{

				@Override
				public void onInflate(ViewStub stub, View inflated)
				{

					footerState.setEnumState(footerState.CLOSED);
					bindNuxViews(parent);
				}
			});

			mmStub.inflate();

		}
		else
		{
			if (OfflineUtils.shouldShowDisconnectFragment(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.DIRECT_REQUEST_DATA, "")))
			{
				if (savedInstanceState != null)
				{
					((HomeActivity) getActivity()).removeFragment(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
				}
				bindDisconnectionFragment(OfflineUtils.fetchMsisdnFromRequestPkt(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.DIRECT_REQUEST_DATA, "")));
			}

		}
		mConversationsComparator = new ConvInfo.ConvInfoComparator();
		fetchConversations();

		for (TypingNotification typingNotification : HikeMessengerApp.getTypingNotificationSet().values())
		{
			toggleTypingNotification(true, typingNotification);
		}
	}

	
	
	public void bindDisconnectionFragment(String msisdn)
	{
		if (!isAdded() || TextUtils.isEmpty(msisdn))
		{
			return;
		}
		final OfflineDisconnectFragment fragment = OfflineDisconnectFragment.newInstance(msisdn, null, DisconnectFragmentType.REQUESTING);

		ViewStub stub=(ViewStub) parent.findViewById(R.id.nux_footer);
		if (stub == null)
		{
			if (footerState.getEnum() == footerState.OPEN)
				setFooterHalfOpen();
		}
		
		// Old fragment is already attached.
		// remove old frgame and attach new fragment
		((HomeActivity) getActivity()).removeFragment(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		((HomeActivity) getActivity()).addFragment(R.id.hike_direct, fragment, OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		fragment.setConnectionListner(new OfflineConnectionRequestListener()
		{

			@Override
			public void removeDisconnectFragment(boolean isConnectionAccepted)
			{
				if (!isConnectionAccepted)
				{
					OfflineUtils.sendOfflineRequestCancelPacket(OfflineUtils.fetchMsisdnFromRequestPkt(HikeSharedPreferenceUtil.getInstance().getData(
							OfflineConstants.DIRECT_REQUEST_DATA, "")));
				}
				OfflineController.getInstance().removeConnectionRequest();

			}

			@Override
			public void onDisconnectionRequest()
			{

			}

			@Override
			public void onConnectionRequest(Boolean startAnimation)
			{
				String msisdn  = OfflineUtils.fetchMsisdnFromRequestPkt(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.DIRECT_REQUEST_DATA, ""));
				if(TextUtils.isEmpty(msisdn))
				{
					return;
				}
				
				if(StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
				{
					if(OfflineController.getInstance().isConnected())
					{
						OfflineUtils.stopFreeHikeConnection(getActivity().getApplicationContext(), OfflineUtils.getConnectedMsisdn());
						sendUIMessage(START_OFFLINE_CONNECTION, 1000, msisdn);
					}
					else
					{
						OfflineController.getInstance().connectAsPerMsisdn(msisdn);
					}
					
				}
				else
				{
					//Starting Chathread
					Intent in = IntentFactory.createChatThreadIntentFromMsisdn(getActivity(),
							OfflineUtils.fetchMsisdnFromRequestPkt(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.DIRECT_REQUEST_DATA, "")), false, false, ChatThreadActivity.ChatThreadOpenSources.OFFLINE);
					in.putExtra(OfflineConstants.START_CONNECT_FUNCTION, true);
					getActivity().startActivity(in);							
				}
				
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
	}

	private boolean isConversationsEmpty()
	{
		return (displayedConversations != null && displayedConversations.isEmpty()) ? true : false;
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		uiHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if (alertDialog != null)
		{
			alertDialog.dismiss();
		}
		if (mAdapter != null)
		{
			mAdapter.getIconLoader().setExitTasksEarly(true);
		}
		if (searchMode)
		{
			mAdapter.pauseSearch();
		}

		if (tipView != null)
		{
			StealthModeManager.getInstance().closingConversationScreen(tipType);
		}
		if (showingWelcomeHikeConvTip)
		{
			removeTipIfExists(ConversationTip.WELCOME_HIKE_TIP);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		/**
		 * Ignoring the clicks on header if any.
		 */
		if (position < getListView().getHeaderViewsCount())
		{
			return;
		}

		position -= getListView().getHeaderViewsCount();

		ConvInfo convInfo = (ConvInfo) mAdapter.getItem(position);

		Logger.d(HikeConstants.CHAT_OPENING_BENCHMARK, " msisdn=" + convInfo.getMsisdn() + " start=" + System.currentTimeMillis());
		if (convInfo instanceof BotInfo)
		{
			BotInfo botInfo = (BotInfo) convInfo;
			if (botInfo.isMessagingBot())
			{
				Intent intent = IntentFactory.createChatThreadIntentFromConversation(getActivity(), convInfo,
						ChatThreadActivity.ChatThreadOpenSources.CONV_FRAGMENT);
				startActivity(intent);
			}
			else
			{
				NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());
				if (nonMessagingBotMetadata.isNativeMode())
				{
					JSONObject data = new JSONObject();
					try
					{
						data.put(HikeConstants.SCREEN, nonMessagingBotMetadata.getTargetActivity());
						data.put(HikeConstants.MSISDN, botInfo.getMsisdn());
					}
					catch (JSONException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					PlatformUtils.openActivity(getActivity(),data.toString());

				}
				else
				{
					Intent web = IntentFactory.getNonMessagingBotIntent(convInfo.getMsisdn(), getActivity());
					if (web != null)
					{
						startActivity(web);
					}
				}

				resetNotificationCounter(convInfo);
				HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_MESSAGE_CHANGED, null);
			}
		}
		else
		{
			Intent intent = IntentFactory.createChatThreadIntentFromConversation(getActivity(), convInfo, ChatThreadActivity.ChatThreadOpenSources.CONV_FRAGMENT);
			startActivity(intent);
		}

		if (searchMode)
		{
			recordSearchItemClicked(convInfo, position, searchText);
		}

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (convInfo.getMsisdn().equals(HikeConstants.FTUE_HIKEBOT_MSISDN) && prefs.getInt(HikeConstants.HIKEBOT_CONV_STATE, 0) == hikeBotConvStat.NOTVIEWED.ordinal())
		{
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.VIEWED.ordinal());
			editor.commit();
		}
	}

	/**
	 * Utility method to update the last message state from unread to read
	 * 
	 * @param convInfo
	 */
	private void resetNotificationCounter(ConvInfo convInfo)
	{
		Utils.resetUnreadCounterForConversation(convInfo);
	}

	private void recordSearchItemClicked(ConvInfo convInfo, int position, String text)
	{
		String SEARCH_RESULT = "srchRslt";
		String INDEX = "idx";
		String SEARCH_TEXT = "srchTxt";
		String RESULT_TYPE = "rsltType";

		int resultType;
		// For existing conversation
		if (mConversationsByMSISDN.containsKey(convInfo.getMsisdn()))
		{
			resultType = 1;
		}
		// For new conversation with hike use
		else if (convInfo.isOnHike())
		{
			resultType = 2;
		}
		// For new conversation with SMS use
		else
		{
			resultType = 3;
		}
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.EVENT_KEY, SEARCH_RESULT).put(SEARCH_TEXT, text).put(INDEX, position).put(RESULT_TYPE, resultType);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_HOME_SEARCH, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	public void setupSearch()
	{
		checkAndRemoveExistingHeaders();
		if (mAdapter != null)
		{
			searchMode = true;
			mAdapter.setupSearch();
		}
	}

	public void onSearchQueryChanged(String s)
	{
		if (searchMode && mAdapter != null)
		{
			searchText = s.trim();
			mAdapter.onQueryChanged(searchText, this);
		}
	}

	public void removeSearch()
	{
		if (mAdapter != null)
		{
			searchText = null;
			mAdapter.removeSearch();
			ShowTipIfNeeded(displayedConversations.isEmpty(), false, ConversationTip.NO_TIP);
			searchMode = false;
		}
	}

	private void resetStealthTipClicked()
	{
		long remainingTime = System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l);

		if (remainingTime > HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS)
		{
			StealthModeManager.getInstance().setTipVisibility(false, ConversationTip.RESET_STEALTH_TIP);

			Object[] dialogStrings = new Object[4];
			dialogStrings[0] = getString(R.string.reset_complete_stealth_header);
			dialogStrings[1] = getString(R.string.reset_stealth_confirmation);
			dialogStrings[2] = getString(R.string.CONFIRM);
			dialogStrings[3] = getString(R.string.CANCEL);

			HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.RESET_STEALTH_DIALOG, new HikeDialogListener()
			{

				@Override
				public void positiveClicked(HikeDialog dialog)
				{
					LockPattern.recordResetPopupButtonClick("confirm", "home");
					HikeAnalyticsEvent.sendStealthReset();
					resetStealthMode();
					dialog.dismiss();
				}

				@Override
				public void neutralClicked(HikeDialog dialog)
				{

				}

				@Override
				public void negativeClicked(HikeDialog dialog)
				{
					LockPattern.recordResetPopupButtonClick("cancel", "home");

					StealthModeManager.getInstance().setTipVisibility(false, ConversationTip.RESET_STEALTH_TIP);

					Utils.cancelScheduledStealthReset();

					dialog.dismiss();

					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}

			}, dialogStrings);
		}
	}

	private void resetStealthMode()
	{
		StealthModeManager.getInstance().setTipVisibility(false, ConversationTip.RESET_STEALTH_TIP);
		resetStealthPreferences();
		StealthModeManager.getInstance().setUp(false);
		/*
		 * If previously the stealth mode was off, we should publish an event telling the friends fragment to refresh its list.
		 */
		if (!StealthModeManager.getInstance().isActive())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_RESET_COMPLETE, null);
		}

		/*
		 * Calling the delete conversation task in the end to ensure that we first publish the reset event. If the delete task was published at first, it was causing a threading
		 * issue where the contacts in the friends fragment were getting removed and not added again.
		 */
		ConvInfo[] stealthConvs = stealthConversations.toArray(new ConvInfo[0]);

		for (ConvInfo convInfo : stealthConvs)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_THIS_CONVERSATION, convInfo);
		}
		StealthModeManager.getInstance().clearStealthTimeline();
		StealthModeManager.getInstance().clearStealthMsisdn();
	}

	private void resetStealthPreferences()
	{
		StealthModeManager.getInstance().resetPreferences();
		AccountBackupRestore.getInstance(getActivity()).updatePrefs();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{

		if (position < getListView().getHeaderViewsCount())
		{
			return false;
		}

		position -= getListView().getHeaderViewsCount();

		if (position >= mAdapter.getCount())
		{
			return false;
		}

		ArrayList<String> optionsList = new ArrayList<String>();

		final ConvInfo conv = (ConvInfo) mAdapter.getItem(position);
		if (!mConversationsByMSISDN.containsKey(conv.getMsisdn()))
		{
			return false;
		}

		if (StealthModeManager.getInstance().isActive())
		{
			optionsList.add(getString(conv.isStealth() ? R.string.unmark_stealth : R.string.mark_stealth));
		}
		else
		{
			optionsList.add(getString(R.string.hide_chat));
		}

		/**
		 * Bot Menus
		 */
		if (BotUtils.isBot(conv.getMsisdn()))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(conv.getMsisdn());

			BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_LONG_PRESS, AnalyticsConstants.LONG_PRESS_EVENT);
			/**
			 * Non-Messaging bot
			 */
			if (botInfo.isNonMessagingBot())
			{
				NonMessagingBotConfiguration botConfig = null == botInfo.getConfigData() ? new NonMessagingBotConfiguration(botInfo.getConfiguration())
						: new NonMessagingBotConfiguration(botInfo.getConfiguration(), botInfo.getConfigData());

				if (botConfig.isLongTapEnabled())
				{
					if (botConfig.isAddShortCutEnabled())
						optionsList.add(getString(R.string.add_shortcut));

					if (botConfig.isDeleteAndBlockEnabled())
						optionsList.add(getString(R.string.delete_block));

					if (botConfig.isDeleteEnabled())
						optionsList.add(getString(R.string.delete));
				}

			}

			/**
			 * Messaging bot
			 */
			else
			{
				MessagingBotMetadata metadata;

				metadata = new MessagingBotMetadata(botInfo.getMetadata());

				MessagingBotConfiguration configuration = new MessagingBotConfiguration(botInfo.getConfiguration(), metadata.isReceiveEnabled());
				if (configuration.isLongTapEnabled())
				{
					BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_LONG_PRESS, AnalyticsConstants.LONG_PRESS_EVENT);

					if (configuration.isViewProfileInConversationScreenEnabled())
					{
						optionsList.add(getString(R.string.viewcontact));
					}
					if (configuration.isAddConvShortcutInConversationScreenEnabled())
					{
						optionsList.add(getString(R.string.shortcut));
					}
					if (configuration.isDeleteChatInConversationScreenEnabled())
					{
						optionsList.add(getString(R.string.delete_chat));
					}
					if (configuration.isClearConvInConversationScreenEnabled())
					{
						optionsList.add(getString(R.string.clear_whole_conversation));
					}
					if (configuration.isEmailConvInConversationScreenEnabled())
					{
						optionsList.add(getString(R.string.email_conversations));
					}
				}
			}

		}
		/**
		 * Other conversation menus
		 */
		else
		{
			if (!(conv instanceof OneToNConvInfo || BotUtils.isBot(conv.getMsisdn())) && ContactManager.getInstance().isUnknownContact(conv.getMsisdn()))
			{
				optionsList.add(getString(R.string.add_to_contacts));
				optionsList.add(getString(R.string.add_to_contacts_existing));
			}
			if (!(conv instanceof OneToNConvInfo))
			{
				if (conv.getConversationName() != null)
				{
					optionsList.add(getString(R.string.viewcontact));
				}
			}
			else
			{
				if (OneToNConversationUtils.isBroadcastConversation(conv.getMsisdn()))
				{
					optionsList.add(getString(R.string.broadcast_info));
				}
				else
				{
					if (ContactManager.getInstance().isGroupAlive(conv.getMsisdn()))
						optionsList.add(getString(R.string.group_info));

				}
			}
			if (conv.getLabel() != null)
			{
				optionsList.add(getString(R.string.shortcut));

			}

			if (!(conv instanceof OneToNConvInfo || BotUtils.isBot(conv.getMsisdn())) && ContactManager.getInstance().isUnknownContact(conv.getMsisdn()))
			{
				optionsList.add(ContactManager.getInstance().isBlocked(conv.getMsisdn()) ? getString(R.string.unblock_title) : getString(R.string.block_title));
			}
			if (OneToNConversationUtils.isGroupConversation(conv.getMsisdn()))
			{
				if(ContactManager.getInstance().isGroupAlive(conv.getMsisdn())){
					optionsList.add(getString(R.string.leave_group));
				}else{
					optionsList.add(getString(R.string.delete_chat));
				}
			}

			else if (OneToNConversationUtils.isBroadcastConversation(conv.getMsisdn()))
			{
				optionsList.add(getString(R.string.delete_broadcast));
			}
			else
			{
				optionsList.add(getString(R.string.delete_chat));
			}

			// Showing "Clear Whole Conv" option in Both Group and One-to-One Chat
			optionsList.add(getString(R.string.clear_whole_conversation));

			optionsList.add(getString(R.string.email_conversations));
		}

		if (optionsList.isEmpty())
		{
			return false;
		}

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new MenuArrayAdapter(getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String option = options[which];
				if (getString(R.string.shortcut).equals(option))
				{
					if (BotUtils.isBot(conv.getMsisdn()))
					{
						BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_ADD_SHORTCUT, AnalyticsConstants.CLICK_EVENT);
					}
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.ADD_SHORTCUT);
					Utils.createShortcut(getActivity(), conv, true);
				}
				else if (getString(R.string.delete_chat).equals(option))
				{
					int dialogId = HikeDialogFactory.DELETE_CHAT_DIALOG;
					if (OneToNConversationUtils.isGroupConversation(conv.getMsisdn()))
					{
						dialogId = HikeDialogFactory.DELETE_GROUP_CONVERSATION_DIALOG;
					}
					HikeDialogFactory.showDialog(getActivity(), dialogId, new HikeDialogListener()
					{

						@Override
						public void positiveClicked(HikeDialog hikeDialog)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							if (OneToNConversationUtils.isGroupConversation(conv.getMsisdn()))
							{
								deleteGCAnalyticEvent(true);
							}

							hikeDialog.dismiss();

							if (BotUtils.isBot(conv.getMsisdn()))
							{
								BotUtils.deleteBotConversation(conv.getMsisdn(), false);
								BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_DELETE_CHAT, AnalyticsConstants.CLICK_EVENT);
							}

							else
							{
								HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_THIS_CONVERSATION, conv);
							}
						}

						private void deleteGCAnalyticEvent(boolean confirm)
						{
							try
							{
								JSONObject metadata = new JSONObject();
								metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.DELETE_GC_CONVERSATION);
								metadata.put(HikeConstants.EVENT_CONFIRM, confirm);
								HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
							}
							catch (JSONException e)
							{
								Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
							}
						}
						
						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}

						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
							hikeDialog.dismiss();
							deleteGCAnalyticEvent(false);
						}
					}, conv.getLabel());

				}
				else if (getString(R.string.leave_group).equals(option))
				{
					HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_GROUP_DIALOG, new HikeDialogListener()
					{

						@Override
						public void positiveClicked(HikeDialog hikeDialog) {
							HikeMqttManagerNew.getInstance().sendMessage(
									conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE),
									MqttConstants.MQTT_QOS_ONE);

							if (((CustomAlertDialog) hikeDialog).isChecked()) {
								HikeMessengerApp.getPubSub().publish(HikePubSub.GROUP_LEFT, conv);

							} else {

								if (HikeConversationsDatabase.getInstance().toggleGroupDeadOrAlive(conv.getMsisdn(),
										false) > 0) {
									OneToNConversationUtils.saveStatusMesg(conv, getActivity().getApplicationContext());
								}
							}
							OneToNConversationUtils.leaveGCAnalyticEvent(hikeDialog, true,HikeConstants.LogEvent.LEAVE_GROUP_VIA_HOME);
							hikeDialog.dismiss();
						}
						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}

						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
							hikeDialog.dismiss();
							OneToNConversationUtils.leaveGCAnalyticEvent(hikeDialog, false,HikeConstants.LogEvent.LEAVE_GROUP_VIA_HOME);
						}
					}, conv.getLabel());

				}
				else if (getString(R.string.delete_broadcast).equals(option))
				{
					HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_BROADCAST_DIALOG, new HikeDialogListener()
					{

						@Override
						public void positiveClicked(HikeDialog hikeDialog)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							HikeMqttManagerNew.getInstance().sendMessage(conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), MqttConstants.MQTT_QOS_ONE);
							HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_THIS_CONVERSATION, conv);
							hikeDialog.dismiss();
						}

						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}

						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
							hikeDialog.dismiss();
						}
					}, conv.getLabel());
				}
			
				else if (getString(R.string.email_conversations).equals(option))
				{
					EmailConversationsAsyncTask task = new EmailConversationsAsyncTask(getActivity(), ConversationFragment.this);
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, conv);

					if (BotUtils.isBot(conv.getMsisdn()))
					{
						BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_EMAIL_CONVERSATION, AnalyticsConstants.CLICK_EVENT);
					}
				}
				// UNUSED CODE
				// else if (getString(R.string.deleteconversations).equals(option))
				// {
				// Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
				// DeleteAllConversations();
				// }
				else if (getString(R.string.viewcontact).equals(option))
				{
					if (conv.isBlocked())
					{
						Toast.makeText(getActivity(), getString(R.string.block_overlay_message, conv.getLabel()), Toast.LENGTH_SHORT).show();
						return;
					}
					viewContacts(conv);

					if (BotUtils.isBot(conv.getMsisdn()))
					{
						BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_VIEW_PROFILE, AnalyticsConstants.CLICK_EVENT);
					}
				}
				else if (getString(R.string.clear_whole_conversation).equals(option))
				{
					clearConversation(conv);

					if (BotUtils.isBot(conv.getMsisdn()))
					{
						BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_CLEAR_CONVERSATION, AnalyticsConstants.CLICK_EVENT);
					}
				}
				else if (getString(R.string.add_to_contacts).equals(option))
				{
					addToContacts(conv.getMsisdn());
				}
				else if (getString(R.string.add_to_contacts_existing).equals(option))
				{
					addToContactsExisting(conv.getMsisdn());
				}
				else if (getString(R.string.group_info).equals(option) || getString(R.string.broadcast_info).equals(option))
				{
					if (!ContactManager.getInstance().isGroupAlive(((OneToNConvInfo) conv).getMsisdn()))
					{
						return;
					}
					viewGroupInfo(conv);
				}
				else if (getString(R.string.mark_stealth).equals(option) || getString(R.string.unmark_stealth).equals(option) || getString(R.string.hide_chat).equals(option))
				{
					StealthModeManager.getInstance().toggleConversation(conv.getMsisdn(), !conv.isStealth(), getActivity());
				}
				else if (getString(R.string.block_title).equals(option))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, conv.getMsisdn());
					if (OfflineUtils.isConnectedToSameMsisdn(conv.getMsisdn()))
					{
						OfflineUtils.stopFreeHikeConnection(HikeMessengerApp.getInstance().getApplicationContext(), conv.getMsisdn());
					}

				}
				else if (getString(R.string.unblock_title).equals(option))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, conv.getMsisdn());
				}

				else if (getString(R.string.delete_block).equals(option))
				{
					onDeleteBotClicked(conv, true);
				}

				else if (getString(R.string.add_shortcut).equals(option))
				{
					onAddShortcutClicked(conv);
				}

				else if (getString(R.string.delete).equals(option))
				{
					onDeleteBotClicked(conv, false);
				}
			}
		});

		alertDialog = builder.show();
		alertDialog.getListView().setDivider(null);
		alertDialog.getListView().setPadding(0, getResources().getDimensionPixelSize(R.dimen.menu_list_padding_top), 0,
				getResources().getDimensionPixelSize(R.dimen.menu_list_padding_bottom));
		return true;
	}

	protected void onAddShortcutClicked(ConvInfo conv)
	{
		if (BotUtils.isBot(conv.getMsisdn()))
		{
			BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_ADD_SHORTCUT, AnalyticsConstants.CLICK_EVENT);
		}
		Utils.logEvent(getActivity(), HikeConstants.LogEvent.ADD_SHORTCUT);
		Utils.createShortcut(getActivity(), conv, true);
	}

	protected void onDeleteBotClicked(final ConvInfo conv, final boolean shouldBlock)
	{
		HikeDialogFactory.showDialog(getActivity(), shouldBlock ? HikeDialogFactory.DELETE_BLOCK : HikeDialogFactory.DELETE_NON_MESSAGING_BOT, new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
				if (shouldBlock)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, conv.getMsisdn());
					BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_DELETE_BLOCK_CHAT, AnalyticsConstants.CLICK_EVENT);
				}
				BotUtils.deleteBotConversation(conv.getMsisdn(), false);

				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}
		}, conv.getLabel());
	}

	protected void clearConversation(final ConvInfo conv)
	{
		HikeDialogFactory.showDialog(this.getActivity(), HikeDialogFactory.CLEAR_CONVERSATION_DIALOG, new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_CONVERSATION, conv.getMsisdn());
				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}
		});
	}

	private void fetchConversations()
	{
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		displayedConversations = new ArrayList<ConvInfo>();
		List<ConvInfo> conversationList = db.getConvInfoObjects();

		for (ConvInfo convInfo : conversationList)
		{
			convInfo.setBlocked(ContactManager.getInstance().isBlocked(convInfo.getMsisdn()));
			
			if (convInfo instanceof BotInfo)
			{
				((BotInfo) convInfo).setConvPresent(true);
			}

			if (convInfo.isMute())
			{
				updateViewForMuteToggle(convInfo);
			}

		}

		stealthConversations = new HashSet<ConvInfo>();

		displayedConversations.addAll(conversationList);

		mConversationsByMSISDN = new HashMap<String, ConvInfo>(displayedConversations.size());
		mConversationsAdded = new HashSet<String>();

		setupConversationLists();

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		if(getActivity().getIntent().getBooleanExtra(HikeConstants.Extras.HAS_TIP, false))
		{
			processTipFromNotif(getActivity().getIntent().getExtras());
		}
		else
		{
			ShowTipIfNeeded(displayedConversations.isEmpty(), false, ConversationTip.NO_TIP);
		}

		mAdapter = new ConversationsAdapter(getActivity(), displayedConversations, stealthConversations, getListView(), this);

		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(this);
		getListView().setOnScrollListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		setEmptyState(mAdapter.isEmpty());
		
		// Making the call to fetch thumbnails only once per app cycle to save data
		if (BotUtils.fetchBotThumbnails)
		{
			BotUtils.fetchBotIcons();
			BotUtils.fetchBotThumbnails = false;
		}

	}
	
	public void startActivityWithResult(Intent intent, int requestCode)
	{
		startActivityForResult(intent, requestCode);
	}

	private void processTipFromNotif(Bundle bundle)
	{
		int tipId = bundle.getInt(HikeConstants.TIP_ID, ConversationTip.NO_TIP);
		if(bundle.getBoolean(HikeConstants.IS_ATOMIC_TIP, false))
		{
			AtomicTipManager.getInstance().processAtomicTipFromNotif(tipId);
			tipId = ConversationTip.ATOMIC_TIP;
		}
		ShowTipIfNeeded(displayedConversations.isEmpty(), true, tipId);
	}

	private void ShowTipIfNeeded(boolean hasNoConversation, boolean isFromNewIntent, int tipFromNotifId)
	{

		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}

		StealthModeManager stealthManager = StealthModeManager.getInstance();
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String tip = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
		Logger.i("tip", "#" + tip + "#-currenttype");

		if(isFromNewIntent)
		{
			tipType = tipFromNotifId;
		}
		else if(AtomicTipManager.getInstance().doesUnseenTipExist() || AtomicTipManager.getInstance().doesHighPriorityTipExist())
		{
			tipType = ConversationTip.ATOMIC_TIP;
		}
		else if(shouldShowUpdateTip())
		{
			tipType = whichUpdateTip();
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Preparing to show tip:"+tipType);
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
		{
			tipType = ConversationTip.RESET_STEALTH_TIP;
		}
		else if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, false))
		{
			showingWelcomeHikeConvTip = true;
			tipType = ConversationTip.WELCOME_HIKE_TIP;
		}
		else if (stealthManager.isTipPersisted(ConversationTip.STEALTH_FTUE_TIP) && !stealthManager.isFtueDone() && stealthManager.isSetUp())
		{
			tipType = ConversationTip.STEALTH_FTUE_TIP;
		}
		else if (stealthManager.isTipPersisted(ConversationTip.STEALTH_INFO_TIP) && !stealthManager.isSetUp())
		{
			tipType = ConversationTip.STEALTH_INFO_TIP;
		}
		else if (StealthModeManager.getInstance().isTipPersisted(ConversationTip.STEALTH_UNREAD_TIP))
		{
			tipType = ConversationTip.STEALTH_UNREAD_TIP;
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_PROFILE_PIC))
		{
			tipType = ConversationTip.ATOMIC_PROFILE_PIC_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_FAVOURITES))
		{
			tipType = ConversationTip.ATOMIC_FAVOURTITES_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_INVITE))
		{
			tipType = ConversationTip.ATOMIC_INVITE_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_STATUS))
		{
			tipType = ConversationTip.ATOMIC_STATUS_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_INFORMATIONAL))
		{
			tipType = ConversationTip.ATOMIC_INFO_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_HTTP))
		{
			tipType = ConversationTip.ATOMIC_HTTP_TIP;
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC))
		{
			tipType = ConversationTip.ATOMIC_APP_GENERIC_TIP;
		}
		else if(AtomicTipManager.getInstance().doesAtomicTipExist())
		{
			tipType = ConversationTip.ATOMIC_TIP;
		}

		// to prevent more than one tip to display at a time , it can happen at time of onnewintent
		if (!hasNoConversation && tipView != null)
		{
			checkAndRemoveExistingHeaders();
		}

		tipView = convTip.getView(tipType);

		if (tipView != null)
		{
			checkAndAddListViewHeader(tipView);
		}
	}
	
	private boolean shouldShowUpdateTip()
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_INVITE_TIP, false))
		{
			return true;
		}
		else if(Utils.isUpdateRequired(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.LATEST_VERSION, ""), getContext()))
		{
			return (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false)
					|| HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false));
		}
		else
		{
			return false;
		}
		
	}
	
	private int whichUpdateTip()
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_INVITE_TIP, false))
		{
			return ConversationTip.INVITE_TIP;
		}
		else if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false))
		{
			return ConversationTip.UPDATE_NORMAL_TIP;
		}
		else if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false))
		{
			return ConversationTip.UPDATE_CRITICAL_TIP;
		}
		return -1;
	}

	private void setupConversationLists()
	{
		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<ConvInfo> iter = displayedConversations.iterator(); iter.hasNext();)
		{
			Object object = iter.next();
			ConvInfo convInfo = (ConvInfo) object;

			mConversationsByMSISDN.put(convInfo.getMsisdn(), convInfo);
			if (convInfo.isStealth())
			{
				stealthConversations.add(convInfo);
				StealthModeManager.getInstance().markStealthMsisdn(convInfo.getMsisdn(), true, false);
			}

			if (!StealthModeManager.getInstance().isActive() && convInfo.isStealth())

			{
				mConversationsAdded.add(convInfo.getMsisdn());
				iter.remove();
			}
			else
			{
				mConversationsAdded.add(convInfo.getMsisdn());
			}
		}

	}

	private void changeConversationsVisibility(int scrollToPosition)
	{
		// further making an isAdded check here, as onDestroy might have been called
		if(!isAdded())
		{
			return;
		}
		// we do not animate removal of multiple chats, coz hidden chats outside visible list
		// might duplicate once you move back to normal mode from hidden mode
		if (!StealthModeManager.getInstance().isActive())
		{
			if (scrollToPosition < 0)
			{
				// moving from hidden to normal mode without animation
				mAdapter.removeStealthConversationsFromLists();
			}
			else
			{
				// hiding individual chat with animation
				getListView().smoothScrollToPosition(scrollToPosition);
				mAdapter.addItemsToAnimat(stealthConversations);
			}

		}
		else
		{
			// moving from normal to hidden mode with animation
			mAdapter.addItemsToAnimat(stealthConversations);
			mAdapter.addToLists(stealthConversations);
		}

		mAdapter.sortLists(mConversationsComparator);
		notifyDataSetChanged();
	}

	private void toggleTypingNotification(boolean isTyping, TypingNotification typingNotification)
	{
		if (!wasViewSetup())
		{
			return;
		}

		if (mConversationsByMSISDN == null)
		{
			return;
		}
		String msisdn = typingNotification.getId();
		ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
		if (convInfo == null)
		{
			Logger.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}

		if (convInfo.getLastConversationMsg() == null)
		{
			Logger.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}

		if (isTyping)
		{
			convInfo.setTypingNotif(typingNotification);
			View parentView = getParenViewForConversation(convInfo);
			if (parentView == null)
			{
				notifyDataSetChanged();
				return;
			}

			mAdapter.updateViewsRelatedToTypingNotif(parentView, convInfo);

		}
		else
		{ // If we were already typing and we got isTyping as false, we remove the typing flag
			if (convInfo.getTypingNotif() != null)
			{
				convInfo.setTypingNotif(null);
				View parentView = getParenViewForConversation(convInfo);
				if (parentView == null)
				{
					notifyDataSetChanged();
					return;
				}

				mAdapter.updateViewsRelatedToLastMessage(parentView, convInfo.getLastConversationMsg(), convInfo);

			}
		}
	}

	public void notifyDataSetChanged()
	{
		if (mAdapter == null)
		{
			return;
		}
		setEmptyState(mAdapter.isEmpty());
		mAdapter.notifyDataSetChanged();
	}

	public void updateTimestampAndSortConversations(Object object)
	{
		Logger.d("ListeningTSPubsub", "listening");
		Pair<String, Long> p = (Pair<String, Long>) object;
		String msisdn = p.first;
		Long ts = p.second;

		final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
		if (convInfo != null)
		{
			if (ts >= 0)
			{
				convInfo.setSortingTimeStamp(ts);
			}

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Collections.sort(displayedConversations, mConversationsComparator); //AND-5145
					notifyDataSetChanged();
				}
			});
		}
	}
	
	private  Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			if (msg == null)
			{
				return;
			}
			handleUIMessage(msg);
		}

	};
	
	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(String type, Object object)
	{
		if (!isAdded())
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "Event received: " + type);

		if ((HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)) || HikePubSub.OFFLINE_MESSAGE_SENT.equals(type))
		{
			ConvMessage message = (ConvMessage) object;
			updateUIWithLastMessage(message);
		}
		else if (HikePubSub.LAST_MESSAGE_DELETED.equals(type))
		{
			Pair<ConvMessage, String> messageMsisdnPair = (Pair<ConvMessage, String>) object;

			final ConvMessage message = messageMsisdnPair.first;
			final String msisdn = messageMsisdnPair.second;

			final boolean conversationEmpty = message == null;

			final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (conversationEmpty)
					{
						clearConversation(msisdn);
						notifyDataSetChanged();
					}
					else
					{
						convInfo.setLastConversationMsg(message);
						sortAndUpdateTheView(convInfo, message, false);
					}
				}
			});
		}
		else if (HikePubSub.NEW_CONVERSATION.equals(type))
		{
			final ConvInfo convInfo = (ConvInfo) object;
			if (convInfo == null)
			{
				Logger.e(ConversationFragment.class.getSimpleName(), "convInfo is null");
				return;
			}

			if (mConversationsByMSISDN.containsKey(convInfo.getMsisdn()))
			{
				Logger.e(ConversationFragment.class.getSimpleName(), "conversation already exists");
				return;
			}

			if (HikeMessengerApp.hikeBotInfoMap.containsKey(convInfo.getMsisdn()))
			{
				convInfo.setmConversationName(HikeMessengerApp.hikeBotInfoMap.get(convInfo.getMsisdn()).getConversationName());
			}
			Logger.d(getClass().getSimpleName(), "New Conversation. Group Conversation? " + (OneToNConversationUtils.isOneToNConversation(convInfo.getMsisdn())));
			mConversationsByMSISDN.put(convInfo.getMsisdn(), convInfo);
			if (convInfo.getLastConversationMsg() == null && !(OneToNConversationUtils.isOneToNConversation(convInfo.getMsisdn())))
			{
				return;
			}

			mConversationsAdded.add(convInfo.getMsisdn());

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				public void run()
				{
					if (displayedConversations.isEmpty())
					{
						/*
						 * start new chat tip will come if user is on home empty state and a new conversation comes.
						 */
						movedFromEmptyToNonEmpty();
					}
					else if (displayedConversations.size() == 4)
					{
						StealthModeManager.getInstance().setTipVisibility(true, ConversationTip.STEALTH_INFO_TIP);
					}
					mAdapter.addToLists(convInfo);
					mAdapter.sortLists(mConversationsComparator);

					notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.MSG_READ.equals(type))
		{
			String msisdn = (String) object;
			final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
			if (convInfo == null)
			{
				/*
				 * We don't really need to do anything if the conversation does not exist.
				 */
				return;
			}
			convInfo.setUnreadCount(0);

			/*
			 * setting the last message as 'Read'
			 */
			final ConvMessage msg = convInfo.getLastConversationMsg();
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.RECEIVED_READ.ordinal()))
			{
				ConvMessage.State currentState = msg.getState();
				if (currentState == ConvMessage.State.RECEIVED_READ)
				{
					return;
				}
				msg.setState(ConvMessage.State.RECEIVED_READ);
			}

			/*
			 * We should only update the view if the last message's state was changed.
			 */
			if (!isAdded())
			{
				return;
			}

			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateViewForMessageStateChange(convInfo, msg);
				}
			});
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			setStateAndUpdateView(msgId);
		}
		else if (HikePubSub.SERVER_RECEIVED_MULTI_MSG.equals(type))
		{
			Pair<Long, Integer> p = (Pair<Long, Integer>) object;
			long baseId = p.first;
			int count = p.second;
			for (long msgId = baseId; msgId < (baseId + count); msgId++)
			{
				setStateAndUpdateView(msgId);
			}
		}
		/*
		 * Receives conversation group-id, the message id for the message read packet, and the participant msisdn.
		 */
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type) || HikePubSub.ONETON_MESSAGE_DELIVERED_READ.equals(type))
		{
			String sender = null;
			long[] ids;
			if (HikePubSub.ONETON_MESSAGE_DELIVERED_READ.equals(type))
			{
				Pair<String, Pair<Long, String>> pair = (Pair<String, Pair<Long, String>>) object;
				sender = pair.first;
				ids = new long[] { pair.second.first };
			}
			else
			{
				Pair<String, long[]> pair = (Pair<String, long[]>) object;
				sender = pair.first;
				ids = (long[]) pair.second;
			}

			final String msisdn = sender;

			ConvMessage lastConvMessage = null;

			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					// If the msisdn don't match we simply return
					if (!msg.getMsisdn().equals(msisdn))
					{
						return;
					}
					lastConvMessage = msg;

					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);

					/*
					 * Since we have updated the last message of the conversation, we don't need to iterate through the array anymore.
					 */
					break;
				}
			}

			if (!isAdded() || lastConvMessage == null)
			{
				return;
			}

			final ConvMessage message = lastConvMessage;
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
					/**
					 * If we are displaying isTyping on the UI, then do not update the UI.
					 */
					if (!convInfo.isLastMsgTyping())
					{
						updateViewForMessageStateChange(convInfo, message);
					}
				}
			});
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			Pair<String, Long> pair = (Pair<String, Long>) object;

			final String msisdn = pair.first;
			long msgId = pair.second;
			final ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
			{
				// If the msisdn don't match we simply return
				if (!msg.getMsisdn().equals(msisdn))
				{
					return;
				}
				msg.setState(ConvMessage.State.SENT_DELIVERED);

				if (!isAdded())
				{
					return;
				}
				getActivity().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);

						/**
						 * If we are displaying isTyping on the UI, then do not update the UI.
						 */
						if (!convInfo.isLastMsgTyping())
						{
							updateViewForMessageStateChange(convInfo, msg);
						}
					}
				});
			}
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}

			final String msisdn = (String) object;

			/* an icon changed, so update the view */
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);

					if (!wasViewSetup() || null == convInfo)
					{
						return;
					}

					View parentView = getParenViewForConversation(convInfo);

					if (parentView == null)
					{
						notifyDataSetChanged();
						return;
					}

					mAdapter.updateViewsRelatedToAvatar(parentView, convInfo);
				}
			});
		}
		else if (HikePubSub.ONETONCONV_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			final String groupName = ContactManager.getInstance().getName(groupId);

			final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
			if (convInfo == null)
			{
				return;
			}
			convInfo.setmConversationName(groupName);

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateViewForNameChange(convInfo);
				}
			});
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type) || HikePubSub.CONTACT_DELETED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null)
			{
				return;
			}

			final ConvInfo convInfo = this.mConversationsByMSISDN.get(contactInfo.getMsisdn());
			if (convInfo != null)
			{
				if (HikePubSub.CONTACT_DELETED.equals(type))
					convInfo.setmConversationName(contactInfo.getMsisdn());
				else
					convInfo.setmConversationName(contactInfo.getName());

				if (!isAdded())
				{
					return;
				}
				final String mType = type;
				getActivity().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						updateViewForNameChange(convInfo);
						if (HikePubSub.CONTACT_DELETED.equals(mType))
							updateViewForAvatarChange(convInfo);
					}
				});
			}
		}
		else if (HikePubSub.TYPING_CONVERSATION.equals(type) || HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			if (object == null)
			{
				return;
			}

			final boolean isTyping = HikePubSub.TYPING_CONVERSATION.equals(type);
			final TypingNotification typingNotification = (TypingNotification) object;

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					toggleTypingNotification(isTyping, typingNotification);
				}
			});
		}
		else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					setEmptyState(mAdapter != null && mAdapter.isEmpty());
					setupFTUEEmptyView();
				}
			});
		}
		else if (HikePubSub.CLEAR_CONVERSATION.equals(type))
		{
			String msisdn = (String) object;
			clearConversation(msisdn);
		}
		else if (HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE.equals(type))
		{
			String msisdn = (String) object;
			clearConversation(msisdn);
		}
		else if (HikePubSub.SHOW_TIP.equals(type))
		{
			if (!isAdded())
			{
				return;
			}

			final int whichTip = (int) object;
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					showStealthTip(whichTip);
				}
			});
		}
		else if (HikePubSub.STEALTH_MODE_TOGGLED.equals(type))
		{
			//this pubsub is fired on onStop and is not running on UI thread
			if (!isAdded())
			{
				return;
			}
			// since getActivity() can be made null by the UI thread,
			// hence we are posting on uiHandler, instead of using runOnUiThread on the activity
			sendUIMessage(STEALTH_CONVERSATION_TOGGLE, -1);
		}
		else if (HikePubSub.REMOVE_TIP.equals(type))
		{
			if (!isAdded())
			{
				return;
			}

			final int tipType = (int) object;
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeTipIfExists(tipType);
				}
			});
		}
		/*
		 * The list of messages is processed. The messages are added and the UI is updated at once.
		 */
		else if (HikePubSub.BULK_MESSAGE_RECEIVED.equals(type))
		{
			Logger.d(getClass().getSimpleName(), "New bulk msg event sent or received.");
			HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;

			if (messageListMap != null)
			{
				for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
				{
					if (entry != null)
					{
						String msisdn = entry.getKey();
						LinkedList<ConvMessage> messageList = entry.getValue();
						final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
						if (convInfo != null)
						{
							int unreadCount = 0;
							ConvMessage lastNonStatusMsg = null;
							for (ConvMessage convMessage : messageList)
							{
								if (Utils.shouldIncrementCounter(convMessage))
								{
									unreadCount++;
									lastNonStatusMsg = convMessage; //AND-3159
								}
							}
							if (unreadCount > 0)
							{
								convInfo.setUnreadCount(convInfo.getUnreadCount() + unreadCount);
							}
							ConvMessage message = messageList.get(messageList.size() - 1);
							if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
							{
								if (convInfo.getLastConversationMsg() != null)
								{
									ConvMessage prevMessage = convInfo.getLastConversationMsg();
									String metadata = message.getMetadata().serialize();

									/* Begin: AND-3159 */
									// The below logic is to correct the sorting of the conversation list, list should be sorted on last non-status message of bulk.
									long timestampToSortOn = (lastNonStatusMsg != null) ? lastNonStatusMsg.getTimestamp() : prevMessage.getTimestamp();
									message = new ConvMessage(message.getMessage(), message.getMsisdn(), timestampToSortOn, prevMessage.getState(),
											prevMessage.getMsgID(), prevMessage.getMappedMsgID(), message.getGroupParticipantMsisdn());
									/* End: AND-3159 */
									try
									{
										message.setMetadata(metadata);
									}
									catch (JSONException e)
									{
										e.printStackTrace();
									}
								}
							}

							final ConvMessage finalMessage = message;
							ConvMessage finalConvInfoMessage = convInfo.getLastConversationMsg();
							if (finalConvInfoMessage != null)
							{
								if (finalMessage.getMsgID() < finalConvInfoMessage.getMsgID())
								{
									return;
								}
							}

							convInfo.setLastConversationMsg(finalMessage);

							if (!isAdded())
							{
								return;
							}
							getActivity().runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									addMessage(convInfo, finalMessage, false);
								}
							});
						}
						else
						{
							// When a message gets sent from a user we don't have a
							// conversation for, the message gets
							// broadcasted first then the conversation gets created. It's
							// okay that we don't add it now, because
							// when the conversation is broadcasted it will contain the
							// messages
						}
					}
				}
				if (!isAdded()) {
					return;
				}
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						mAdapter.sortLists(mConversationsComparator);
						notifyDataSetChanged();
					}
				});
			}
		}
		/*
		 * The list of msisdns and their maximum ids for DR and MR packets is received. The messages are updated in the chat thread.
		 */
		else if (HikePubSub.BULK_MESSAGE_DELIVERED_READ.equals(type))
		{
			Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap = (Map<String, PairModified<PairModified<Long, Set<String>>, Long>>) object;

			if (messageStatusMap != null)
			{
				for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet())
				{
					if (entry != null)
					{
						final String msisdn = entry.getKey();
						PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
						if (pair != null)
						{
							long mrMsgId = (long) pair.getFirst().getFirst();
							long drMsgId = (long) pair.getSecond();

							if (mrMsgId > 0)
							{
								ConvMessage msg = findMessageById(mrMsgId);
								if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
								{
									// If the msisdn don't match we simply return
									if (!msg.getMsisdn().equals(msisdn))
									{
										return;
									}

									msg.setState(ConvMessage.State.SENT_DELIVERED_READ);

									if (!isAdded())
									{
										return;
									}

									final ConvMessage message = msg;
								}
							}
							if (drMsgId > 0)
							{
								final ConvMessage msg = findMessageById(drMsgId);
								if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
								{
									// If the msisdn don't match we simply return
									if (!msg.getMsisdn().equals(msisdn))
									{
										return;
									}

									msg.setState(ConvMessage.State.SENT_DELIVERED);

									if (!isAdded())
									{
										return;
									}
								}
							}
						}
					}
				}
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						notifyDataSetChanged();
					}
				});
			}
		}
		else if (HikePubSub.GROUP_END.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if (groupId != null)
			{
				final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
				if (convInfo == null)
				{
					return;
				}
				((OneToNConvInfo) convInfo).setConversationAlive(false);
			}
		}
		else if (HikePubSub.MULTI_MESSAGE_DB_INSERTED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			Logger.d(getClass().getSimpleName(), "New msg event sent or received.");
			List<Pair<ContactInfo, ConvMessage>> allPairs = (List<Pair<ContactInfo, ConvMessage>>) object;
			for (Pair<ContactInfo, ConvMessage> contactMessagePair : allPairs)
			{
				/* find the conversation corresponding to this message */
				ContactInfo contactInfo = contactMessagePair.first;
				String msisdn = contactInfo.getMsisdn();
				final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
				// possible few conversation does not exist ,as we can forward to any contact
				if (convInfo == null)
				{
					continue;
				}

				ConvMessage message = contactMessagePair.second;

				if (convInfo.getLastConversationMsg() != null)
				{
					if (message.getMsgID() < convInfo.getLastConversationMsg().getMsgID())
					{
						continue;
					}
				}

				// for multi messages , if conversation exists then only we need
				// to update messages . No new conversation will be created
				convInfo.setLastConversationMsg(message);

			}
			// messages added , update UI
			getActivity().runOnUiThread(new Runnable()
			{
				public void run()
				{
					mAdapter.sortLists(mConversationsComparator);
					notifyDataSetChanged();
				};
			});
		}
		else if (HikePubSub.CONV_UNREAD_COUNT_MODIFIED.equals(type))
		{
			unreadCountModified((Message) object);
		}
		else if (HikePubSub.STEALTH_CONVERSATION_MARKED.equals(type) || HikePubSub.STEALTH_CONVERSATION_UNMARKED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			final ConvInfo convInfo = mConversationsByMSISDN.get((String) object);
			if (convInfo == null)
			{
				return;
			}
			if (HikePubSub.STEALTH_CONVERSATION_UNMARKED.equals(type))
			{
				convInfo.setStealth(false);
				stealthConversations.remove(convInfo);
			}
			else if (HikePubSub.STEALTH_CONVERSATION_MARKED.equals(type))
			{
				convInfo.setStealth(true);
				stealthConversations.add(convInfo);
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// this is to show/remove the stealth badge
					notifyDataSetChanged();
					if (!StealthModeManager.getInstance().isActive() && convInfo.isStealth())
					{
						// the conversation is marked as stealth but is visible, even though stealth mode is inactive
						// so we play animation here to slide out the chat
						changeConversationsVisibility(displayedConversations.indexOf(convInfo));
					}
				}
			});

		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}

			Mute mute = (Mute) object;
			String msisdn = mute.getMsisdn();
			final Boolean isMuted = mute.isMute();

			final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);

			if (convInfo == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					convInfo.setIsMute(isMuted);

					updateViewForMuteToggle(convInfo);
				}
			});
		}
		else if (HikePubSub.CONVERSATION_TS_UPDATED.equals(type))
		{
			updateTimestampAndSortConversations(object);
		}
		else if (HikePubSub.PARTICIPANT_JOINED_ONETONCONV.equals(type) || HikePubSub.PARTICIPANT_LEFT_ONETONCONV.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if (TextUtils.isEmpty(groupId))
			{
				return;
			}

			// This Pubsub is currently used here only to update default name
			// of a broadcast conversation.
			if (!OneToNConversationUtils.isBroadcastConversation(groupId))
			{
				return;
			}
			final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
			if (convInfo == null || !(convInfo instanceof OneToNConvInfo))
			{
				return;
			}

			if (!isAdded())
			{
				return;
			}

			((OneToNConvInfo) convInfo).updateName();
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateViewForNameChange(convInfo);
				}
			});
		}
		else if (HikePubSub.CONVERSATION_DELETED.equals(type))
		{
			final ConvInfo delConv = (ConvInfo) object;
			final String msisdn = delConv.getMsisdn();

			if (!isAdded())
			{
				return;
			}

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					mAdapter.remove(delConv);
					mConversationsByMSISDN.remove(msisdn);
					mConversationsAdded.remove(msisdn);

					StealthModeManager.getInstance().markStealthMsisdn(msisdn, false, false);
					stealthConversations.remove(delConv);

					notifyDataSetChanged();

					if (mAdapter.getCount() == 0)
					{
						setEmptyState(mAdapter != null && mAdapter.isEmpty());
					}
				}
			});
		}
		else if (HikePubSub.BLOCK_USER.equals(type) || HikePubSub.UNBLOCK_USER.equals(type))
		{
			String mMsisdn = (String) object;
			ConvInfo convInfo = mConversationsByMSISDN.get(mMsisdn);
			if (convInfo != null)
			{
				convInfo.setBlocked(HikePubSub.BLOCK_USER.equals(type) ? true : false);
			}
		}

		else if (HikePubSub.UPDATE_LAST_MSG_STATE.equals(type))
		{
			Pair<Integer, String> stateMsisdnPair = (Pair<Integer, String>) object;

			final ConvInfo convInfo = mConversationsByMSISDN.get(stateMsisdnPair.second);

			if (convInfo != null)
			{
				final ConvMessage convMsg = convInfo.getLastConversationMsg();
				if (convMsg != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							/**
							 * Fix for PlayStore crash for illegal state exception on getListView()
							 */
							if (getView() == null || getView().findViewById(android.R.id.list) == null)
							{
								return;
							}
							/**
							 * Fix ends here.
							 */
							View parentView = getListView().getChildAt(
									displayedConversations.indexOf(convInfo) - getListView().getFirstVisiblePosition() + getOffsetForListHeader());

							if (parentView != null)
							{
								mAdapter.updateViewsRelatedToLastMessage(parentView, convMsg, convInfo);
							}
						}
					});
				}
			}
		}
		else if (HikePubSub.ON_OFFLINE_REQUEST.equals(type))
		{
			if(!isAdded())
			{
				return;	
			}
			
			if (object == null)
			{
				((HomeActivity)getActivity()).removeFragment(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
			}
			else
			{
				final String msisdn = (String) object;
				getActivity().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						bindDisconnectionFragment(msisdn);

					}
				});

			}
		}
		else if (HikePubSub.GENERAL_EVENT.equals(type))
		{
			final ConvMessage message=(ConvMessage)object;
			final ConvInfo convInfo = mConversationsByMSISDN.get(message.getMsisdn());
			if(convInfo!=null&&isAdded())
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{

						convInfo.setLastConversationMsg(message);
						sortAndUpdateTheView(convInfo, message, false);

					}
				});
			}
		}
		else if(HikePubSub.GENERAL_EVENT_STATE_CHANGE.equals(type))
		{
			if (isAdded())
			{
				final ConvMessage message = (ConvMessage) object;
				final ConvInfo convInfo = mConversationsByMSISDN.get(message.getMsisdn());
				if (convInfo != null)
				{
					convInfo.setLastConversationMsg(message);
					final ConvMessage convMsg = convInfo.getLastConversationMsg();
					if (convMsg != null)
					{
						getActivity().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								View parentView = getListView().getChildAt(
										displayedConversations.indexOf(convInfo) - getListView().getFirstVisiblePosition() + getOffsetForListHeader());

								if (parentView != null)
								{
									mAdapter.updateViewsRelatedToLastMessage(parentView, convMsg, convInfo);
								}
							}
						});
					}
				}


			}
		}
		else if (HikePubSub.LASTMSG_UPDATED.equals(type))
		{
			if (isAdded())
			{
				final ConvMessage message = (ConvMessage) object;
				final ConvInfo convInfo = mConversationsByMSISDN.get(message.getMsisdn());
				if (convInfo != null)
				{
					convInfo.setLastConversationMsg(message);
					final ConvMessage convMsg = convInfo.getLastConversationMsg();
					if (convMsg != null)
					{
						getActivity().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								/**
								 * Fix for PlayStore crash for illegal state exception on getListView()
								 */
								if (getView() == null || getView().findViewById(android.R.id.list) == null)
								{
									return;
								}
								/**
								 * Fix ends here.
								 */

								View parentView = getListView()
										.getChildAt(displayedConversations.indexOf(convInfo) - getListView().getFirstVisiblePosition() + getOffsetForListHeader());

								if (parentView != null)
								{
									mAdapter.updateViewsRelatedToLastMessage(parentView, convMsg, convInfo);
								}
							}
						});
					}
				}

			}
		}

	}

	protected void handleUIMessage(Message msg)
	{
		switch(msg.what)
		{
			case START_OFFLINE_CONNECTION:
				OfflineController.getInstance().connectAsPerMsisdn((String)msg.obj);
				break;
			case STEALTH_CONVERSATION_TOGGLE:
				changeConversationsVisibility((int)msg.obj);
				break;
		}
	}
	
	private void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}

	private void sendUIMessage(int what, long delayTime, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessageDelayed(message, delayTime);
	}

	private void unreadCountModified(Message message)
	{
		String msisdn = (String) message.obj;
		final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
		if (convInfo == null)
		{
			return;
		}
		convInfo.setUnreadCount(message.arg1);
		getActivity().runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				notifyDataSetChanged();
			}
		});
	}

	private ConvInfo getFirstConversation()
	{
		ConvInfo conv = null;
		if (!displayedConversations.isEmpty())
		{
			conv = displayedConversations.get(0);
		}
		return conv;
	}

	private void animateListView(boolean animateDown)
	{
		float fromYDelta = 70 * Utils.scaledDensityMultiplier;
		TranslateAnimation animation = new TranslateAnimation(0, 0, animateDown ? -1 * fromYDelta : fromYDelta, 0);
		animation.setDuration(300);
		parent.startAnimation(animation);
	}

	/**
	 * Workaround for bug where the header needs to be added after adapter has been set in the list view
	 * 
	 * @param headerView
	 */
	private void checkAndAddListViewHeader(View headerView)
	{
		if (!isAdded())
		{
			return;
		}
		ListAdapter adapter = getListAdapter();

		if (adapter != null)
		{
			setListAdapter(null);
			getListView().addHeaderView(headerView);
			setListAdapter(adapter);
		}

		else
		{
			getListView().addHeaderView(headerView);
		}
	}

	protected void showStealthTip(int whichType)
	{
		if (whichType == tipType || !isAdded())
		{
			return;
		}

		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}

		switch (whichType)
		{

		case ConversationTip.STEALTH_FTUE_TIP:
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, true);
			break;

		case ConversationTip.STEALTH_INFO_TIP:
			if (StealthModeManager.getInstance().isSetUp())
			{
				return;
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, true);
			break;

		default:
			break;
		}

		checkAndRemoveExistingHeaders();

		this.tipType = whichType;
		tipView = convTip.getView(whichType);

		if (tipView != null)
		{
			checkAndAddListViewHeader(tipView);
			animateListView(true);
		}

		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.MqttMessageTypes.TIP);
			metadata.put(AnalyticsConstants.StealthEvents.TIP_SHOW, whichType);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
		HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.VIEW_EVENT, EventPriority.HIGH, metadata);

	}

	public void checkAndRemoveExistingHeaders()
	{
		if (getListView().getHeaderViewsCount() > 0 && tipView != null)
		{
			Logger.d("ConversationFragment", "Found an existing header in listView. Removing it");
			getListView().removeHeaderView(tipView);
		}
	}

	private void clearConversation(String msisdn)
	{
		final ConvInfo conversation = mConversationsByMSISDN.get(msisdn);

		if (conversation == null)
		{
			return;
		}

		conversation.setUnreadCount(0);
		/*
		 * Adding a blank message
		 */
		final ConvMessage newMessage = new ConvMessage("", msisdn, conversation.getLastConversationMsg() != null ? conversation.getLastConversationMsg().getTimestamp() : 0,
				State.RECEIVED_READ);
		conversation.setLastConversationMsg(newMessage);

		if (!isAdded())
		{
			return;
		}

		getActivity().runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if (!wasViewSetup())
				{
					return;
				}

				View parentView = getParenViewForConversation(conversation);

				if (parentView == null)
				{
					notifyDataSetChanged();
					return;
				}

				mAdapter.updateViewsRelatedToLastMessage(parentView, newMessage, conversation);
			}
		});
	}

	private ConvMessage findMessageById(long msgId)
	{
		for (Entry<String, ConvInfo> conversationEntry : mConversationsByMSISDN.entrySet())
		{
			ConvInfo conversation = conversationEntry.getValue();
			if (conversation == null)
			{
				continue;
			}

			ConvMessage message = conversation.getLastConversationMsg();
			if (message != null && message.getMsgID() == msgId)
			{
				return message;
			}
		}

		return null;
	}

	private void setStateAndUpdateView(long msgId)
	{
		final ConvMessage msg = findMessageById(msgId);
		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
		{
			msg.setState(ConvMessage.State.SENT_CONFIRMED);

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					ConvInfo conversation = mConversationsByMSISDN.get(msg.getMsisdn());
					/**
					 * If we are displaying isTyping on the UI, then do not update the UI.
					 */
					if (!conversation.isLastMsgTyping())
					{
						updateViewForMessageStateChange(conversation, msg);
					}
				}
			});
		}
	}

	private View getParenViewForConversation(ConvInfo convInfo)
	{
		int index = displayedConversations.indexOf(convInfo);

		if (index == -1)
		{
			return null;
		}

		return getListView().getChildAt(index - getListView().getFirstVisiblePosition() + getOffsetForListHeader());
	}

	/**
	 * Provides an offset for the correct location as we might have header views for the list view
	 * 
	 * @return
	 */
	private int getOffsetForListHeader()
	{
		return getListView().getHeaderViewsCount();
	}

	private void updateViewForNameChange(ConvInfo convInfo)
	{
		if (!wasViewSetup())
		{
			return;
		}

		View parentView = getParenViewForConversation(convInfo);

		if (parentView == null)
		{
			notifyDataSetChanged();
			return;
		}

		mAdapter.updateViewsRelatedToName(parentView, convInfo);
	}

	private void updateViewForMuteToggle(ConvInfo convInfo)
	{
		if (!wasViewSetup())
		{
			return;
		}

		View parentView = getParenViewForConversation(convInfo);

		if (parentView == null)
		{
			notifyDataSetChanged();
			return;
		}

		mAdapter.updateViewsRelatedToMute(parentView, convInfo);
	}

	private void updateViewForAvatarChange(ConvInfo convInfo)
	{
		if (!wasViewSetup())
		{
			return;
		}

		View parentView = getParenViewForConversation(convInfo);

		if (parentView == null)
		{
			notifyDataSetChanged();
			return;
		}

		mAdapter.updateViewsRelatedToAvatar(parentView, convInfo);
	}

	private void updateViewForMessageStateChange(ConvInfo convInfo, ConvMessage convMessage)
	{
		if (!wasViewSetup() || null == convInfo)
		{
			Logger.d("UnreadBug", "Unread count event received but view wasn't setup");
			return;
		}

		View parentView = getParenViewForConversation(convInfo);

		if (parentView == null)
		{
			Logger.d("UnreadBug", "Unread count event received but parent view was null");
			notifyDataSetChanged();
			return;
		}

		mAdapter.updateViewsRelatedToMessageState(parentView, convMessage, convInfo);
	}

	private void addMessage(ConvInfo convInfo, ConvMessage convMessage, boolean sortAndUpdateView)
	{
		boolean newConversationAdded = false;

		if (!mConversationsAdded.contains(convInfo.getMsisdn()))
		{
			mConversationsAdded.add(convInfo.getMsisdn());
			mAdapter.addToLists(convInfo);

			newConversationAdded = true;

			if (displayedConversations.size() == 1)
			{
				movedFromEmptyToNonEmpty();
			}
			else if (displayedConversations.size() == 5)
			{
				StealthModeManager.getInstance().setTipVisibility(true, ConversationTip.STEALTH_INFO_TIP);
			}
		}

		Logger.d(getClass().getSimpleName(), "new message is " + convMessage);

		if (sortAndUpdateView)
		{
			sortAndUpdateTheView(convInfo, convMessage, newConversationAdded);
		}
	}

	public void movedFromEmptyToNonEmpty()
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_TO_HIKE_CARD, false))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_WELCOME_TO_HIKE_CARD, true);
		}
	}

	private void sortAndUpdateTheView(ConvInfo convInfo, ConvMessage convMessage, boolean newConversationAdded)
	{
		int prevIndex = displayedConversations.indexOf(convInfo);

		mAdapter.sortLists(mConversationsComparator);

		int newIndex = displayedConversations.indexOf(convInfo);

		/*
		 * Here we check if the index of the item remained the same after sorting. If it did, we just need to update that item's view. If not, we need to call notifyDataSetChanged.
		 * OR if a new conversation was added, in that case we simply call notify.
		 */
		if (newConversationAdded || newIndex != prevIndex)
		{
			notifyDataSetChanged();
		}
		else
		{
			// for cases when list view is null or index is -1 (stealth chats that are not displayed)
			if (!wasViewSetup() || newIndex < 0)
			{
				return;
			}

			View parentView = getParenViewForConversation(convInfo);

			if (parentView == null)
			{
				notifyDataSetChanged();
				return;
			}

			mAdapter.updateViewsRelatedToLastMessage(parentView, convMessage, convInfo);
		}
	}

	/**
	 * Returns whether the view is setup. We should call this before trying to get the ListView.
	 * 
	 * @return
	 */
	private boolean wasViewSetup()
	{
		return getView() != null;
	}

	// NOT IN USE
	/*
	 * public void DeleteAllConversations() { if (!mAdapter.isEmpty()) { Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
	 * HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_ALL_CONVERSATIONS, new HikeDialogListener() {
	 * 
	 * @Override public void positiveClicked(HikeDialog hikeDialog) { ConvInfo[] convs = new ConvInfo[mAdapter.getCount()]; for (int i = 0; i < convs.length; i++) { convs[i] =
	 * mAdapter.getItem(i); if (OneToNConversationUtils.isOneToNConversation(convs[i].getMsisdn())) {
	 * HikeMqttManagerNew.getInstance().sendMessage(convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), MqttConstants.MQTT_QOS_ONE); } }
	 * DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity()); task.execute(convs); hikeDialog.dismiss(); }
	 * 
	 * @Override public void neutralClicked(HikeDialog hikeDialog) { }
	 * 
	 * @Override public void negativeClicked(HikeDialog hikeDialog) { hikeDialog.dismiss(); } }); } }
	 */

	@Override
	public void onResume()
	{

		if (getActivity().getIntent().hasExtra(HikeConstants.STEALTH_MSISDN))
		{
			StealthModeManager.getInstance().showLockPattern(getActivity().getIntent().getStringExtra(HikeConstants.STEALTH_MSISDN), getActivity());
		}

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (getActivity() == null && prefs.getInt(HikeConstants.HIKEBOT_CONV_STATE, 0) == hikeBotConvStat.VIEWED.ordinal())
		{
			/*
			 * if there is a HikeBotConversation in Conversation list also it is Viewed by user then delete this.
			 */
			ConvInfo convInfo = null;
			convInfo = mConversationsByMSISDN.get(HikeConstants.FTUE_HIKEBOT_MSISDN);
			if (convInfo != null)
			{
				Editor editor = prefs.edit();
				editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.DELETED.ordinal());
				editor.commit();
				Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
				HikeMqttManagerNew.getInstance().sendMessage(convInfo.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), MqttConstants.MQTT_QOS_ONE);
				HikeMessengerApp.getPubSub().publish(HikePubSub.GROUP_LEFT, convInfo);
			}
		}
		if (searchMode)
		{
			mAdapter.onQueryChanged(searchText, this);
		}
		if (mAdapter != null)
		{
			mAdapter.getIconLoader().setExitTasksEarly(false);
			notifyDataSetChanged();
		}
		
		if (!OfflineUtils.shouldShowDisconnectFragment(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.DIRECT_REQUEST_DATA, "")))
		{
			((HomeActivity)getActivity()).removeFragment(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		}
		super.onResume();
	}

	public boolean hasNoConversation()
	{
		return displayedConversations.size() == 0;
	}

	private class MenuArrayAdapter extends ArrayAdapter<CharSequence>
	{
		private boolean stealthFtueDone = true;

		private int stealthType;

		public MenuArrayAdapter(Context context, int resource, int textViewResourceId, String[] options)
		{
			super(context, resource, textViewResourceId, options);
			stealthFtueDone = StealthModeManager.getInstance().isSetUp();
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = super.getView(position, convertView, parent);

			if (!stealthFtueDone && StealthModeManager.getInstance().isActive() && position == 0)
			{
				v.findViewById(R.id.intro_img).setVisibility(View.VISIBLE);
			}
			else
			{
				v.findViewById(R.id.intro_img).setVisibility(View.GONE);
			}
			// TODO Auto-generated method stub
			return v;
		}

	}

	protected void viewContacts(ConvInfo conv)
	{
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.CONTACT_INFO, conv.getMsisdn());
		intent.putExtra(HikeConstants.Extras.ON_HIKE, conv.isOnHike());
		startActivity(intent);
	}

	protected void viewGroupInfo(ConvInfo convInfo)
	{
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		if (OneToNConversationUtils.isBroadcastConversation(convInfo.getMsisdn()))
		{
			intent.putExtra(HikeConstants.Extras.BROADCAST_LIST, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST, convInfo.getMsisdn());
		}
		else
		{
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, convInfo.getMsisdn());
		}
		startActivity(intent);
	}

	private void addToContacts(String msisdn)
	{
		Intent i = new Intent(Intent.ACTION_INSERT);
		i.setType(ContactsContract.RawContacts.CONTENT_TYPE);
		i.putExtra(Insert.PHONE, msisdn);
		startActivity(i);
	}

	private void addToContactsExisting(String msisdn)
	{
		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(Insert.PHONE, msisdn);
		startActivity(i);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if (mAdapter == null)
		{
			return;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		mAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	private void removeTipIfExists(int whichTip)
	{

		if (tipType != whichTip || !isAdded())
		{
			return;
		}
		/*
		 * Remove tip always: for cases when we want to remove the tip before it is actually shown on the UI
		 */
		switch (tipType)
		{
		case ConversationTip.STEALTH_FTUE_TIP:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
			break;
		case ConversationTip.WELCOME_HIKE_TIP:
			showingWelcomeHikeConvTip = false;
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true);
			break;
		case ConversationTip.STEALTH_INFO_TIP:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP);
			break;
		case ConversationTip.STEALTH_UNREAD_TIP:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP);
			break;
		case ConversationTip.UPDATE_NORMAL_TIP:
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Removing normal update tip");
            HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP,
                    AnalyticsConstants.UPDATE_TIP_DISMISSED, AnalyticsConstants.CLICK_EVENT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false);
			break;
		case ConversationTip.UPDATE_CRITICAL_TIP:
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Removing critical update tip");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
			break;
		case ConversationTip.INVITE_TIP:
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Removing invite tip");
            HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP,
                    AnalyticsConstants.INVITE_TIP_DISMISSED, AnalyticsConstants.CLICK_EVENT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_INVITE_TIP, false);
			break;
		case ConversationTip.ATOMIC_TIP:
			AtomicTipManager.getInstance().onAtomicTipClosed();
			break;
		case ConversationTip.RESET_STEALTH_TIP:
			if (convTip != null)
			{
				convTip.resetCountDownSetter();
			}
		default:
			break;
		}

		getListView().removeHeaderView(tipView);
		animateListView(false);

		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.MqttMessageTypes.TIP);
			metadata.put(AnalyticsConstants.StealthEvents.TIP_HIDE, whichTip);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
		HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.VIEW_EVENT, EventPriority.HIGH, metadata);

		tipType = ConversationTip.NO_TIP;

	}

	@Override
	public void onNewintent(Intent intent)
	{
		Logger.d("footer", "onNewIntent");
		if (intent.getBooleanExtra(HikeConstants.Extras.HAS_TIP, false))
		{
			processTipFromNotif(intent.getExtras());
		}

		final NUXManager nm = NUXManager.getInstance();

		if (nm.getCurrentState() == NUXConstants.NUX_IS_ACTIVE || (nm.getCurrentState() == NUXConstants.NUX_SKIPPED) || (nm.getCurrentState() == NUXConstants.COMPLETED))
		{

			if (NUXManager.getInstance().isReminderReceived())
			{

				switch (footerState.getEnum())
				{
				case OPEN:
					if (NUXManager.getInstance().isReminderNormal())
						setFooterHalfOpen();

					break;
				case HALFOPEN:

					if (!nm.wantToInfalte())
					{
						onClick(llChatReward);
					}
					break;
				case CLOSED:

					onClick(footercontroller);
					if (!nm.wantToInfalte())
					{
						onClick(llChatReward);
					}
					break;
				}

				fillNuxFooterElements();
				NUXManager.getInstance().reminderShown();
			}
			else
			{
				fillNuxFooterElements();
			}

		}

		if (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_KILLED)
		{
			ViewStub mmStub = (ViewStub) parent.findViewById(R.id.nux_footer);
			if (mmStub == null && llNuxFooter != null && llInviteOptions != null && llChatReward != null)
			{
				llNuxFooter.setVisibility(View.GONE);
				llInviteOptions.setVisibility(View.GONE);
				llChatReward.setVisibility(View.GONE);
				getListView().setPadding(0, 0, 0, 0);
			}
		}
	}

	@Override
	public void closeTip(int whichTip)
	{
		if (tipView != null && tipType == whichTip)
		{
			if(whichTip == ConversationTip.ATOMIC_TIP)
			{
				AtomicTipManager.getInstance().tipUiEventAnalytics(AnalyticsConstants.AtomicTipsAnalyticsConstants.TIP_CROSSED);
			}
			removeTipIfExists(whichTip);

			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.MqttMessageTypes.TIP);
				metadata.put(AnalyticsConstants.StealthEvents.TIP_REMOVE, whichTip);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
	}

	@Override
	public void clickTip(int whichTip)
	{
		if (tipView != null)
		{
			switch (whichTip)
			{
			case ConversationTip.RESET_STEALTH_TIP:
				resetStealthTipClicked();
				break;
			case ConversationTip.UPDATE_CRITICAL_TIP:
			case ConversationTip.UPDATE_NORMAL_TIP:
				Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Processing update tip click.");
				HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP, AnalyticsConstants.UPDATE_TIP_CLICKED, AnalyticsConstants.CLICK_EVENT);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false);

				if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK, false))
				{
					FTApkManager.onUpdateTipClick(getContext());
				}
				else
				{
				Uri url = Uri.parse(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.URL, "market://details?id=com.bsb.hike"));
				Intent openUrl = new Intent(Intent.ACTION_VIEW, url);
				openUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(openUrl, ConversationTip.REQUEST_CODE_URL_OPEN);
				}
				break;
			case ConversationTip.INVITE_TIP:
				Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Processing invite tip click.");
                HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP,
                        AnalyticsConstants.INVITE_TIP_CLICKED, AnalyticsConstants.CLICK_EVENT);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_INVITE_TIP, false);
				Intent sendInvite = new Intent(getContext(), HikeListActivity.class);
				startActivityForResult(sendInvite, ConversationTip.REQUEST_CODE_SEND_INVITE);
				break;
			case ConversationTip.ATOMIC_TIP:
				AtomicTipManager.getInstance().tipUiEventAnalytics(AnalyticsConstants.AtomicTipsAnalyticsConstants.TIP_CLICKED);
				AtomicTipManager.getInstance().onAtomicTipClicked(getActivity());
				break;
			default:
				break;
			}
		}
		

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == ConversationTip.REQUEST_CODE_SEND_INVITE)
		{
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Returned after invite tip click.");
			removeTipIfExists(ConversationTip.INVITE_TIP);
		}
		else if(requestCode == ConversationTip.REQUEST_CODE_URL_OPEN)
		{
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Returned after update tip click.");
			removeTipIfExists(ConversationTip.UPDATE_NORMAL_TIP);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onFilterComplete(int count)
	{
		setEmptyState(mAdapter != null && mAdapter.isEmpty());
	}

	private void updateUIWithLastMessage(ConvMessage message)
	{
		Logger.d(getClass().getSimpleName(), "New msg event sent or received.");
		/* find the conversation corresponding to this message */
		String msisdn = message.getMsisdn();
		final ConvInfo conv = mConversationsByMSISDN.get(msisdn);

		if (conv == null)
		{
			// When a message gets sent from a user we don't have a
			// conversation for, the message gets
			// broadcasted first then the conversation gets created. It's
			// okay that we don't add it now, because
			// when the conversation is broadcasted it will contain the
			// messages
			return;
		}
		if (Utils.shouldIncrementCounter(message))
		{
			conv.setUnreadCount(conv.getUnreadCount() + 1);
		}

		if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
		{
			if (conv.getLastConversationMsg() != null)
			{
				ConvMessage prevMessage = conv.getLastConversationMsg();
				String metadata = message.getMetadata().serialize();
				message = new ConvMessage(message.getMessage(), message.getMsisdn(), prevMessage.getTimestamp(), prevMessage.getState(), prevMessage.getMsgID(),
						prevMessage.getMappedMsgID(), message.getGroupParticipantMsisdn(), prevMessage.getSortingId());
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		final ConvMessage finalMessage = message;

		if (conv.getLastConversationMsg() != null)
		{
			if (finalMessage.getMsgID() < conv.getLastConversationMsg().getMsgID())
			{
				return;
			}
		}

		conv.setLastConversationMsg(finalMessage); //Adding this here since the ConvInfo object can be read simultaneously on the pubSub and UI Thread.

		if (!isAdded())
		{
			return;
		}
		getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				addMessage(conv, finalMessage, true);
			}
		});

	}

}
