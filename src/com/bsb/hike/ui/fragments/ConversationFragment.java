package com.bsb.hike.ui.fragments;

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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.adapters.EmptyConversationsAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.EmptyConversationContactItem;
import com.bsb.hike.models.EmptyConversationFtueCardItem;
import com.bsb.hike.models.EmptyConversationItem;
import com.bsb.hike.models.NUXChatReward;
import com.bsb.hike.models.NUXTaskDetails;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.Conversation.ConversationTip.ConversationTipClickedListener;
import com.bsb.hike.models.Conversation.OneToNConvInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.HikeFragmentable;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;

public class ConversationFragment extends SherlockListFragment implements OnItemLongClickListener, Listener, OnScrollListener, HikeFragmentable, OnClickListener, ConversationTipClickedListener
{

	private class DeleteConversationsAsyncTask extends AsyncTask<ConvInfo, Void, ConvInfo[]>
	{

		Context context;

		boolean publishStealthEvent;

		public DeleteConversationsAsyncTask(Context context)
		{
			this(context, true);
		}

		public DeleteConversationsAsyncTask(Context context, boolean publishStealthEvent)
		{
			/*
			 * Using application context since that will never be null while the task is running.
			 */
			this.context = context.getApplicationContext();
			this.publishStealthEvent = publishStealthEvent;
		}

		@Override
		protected ConvInfo[] doInBackground(ConvInfo... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<String> msisdns = new ArrayList<String>(convs.length);
			Editor editor = context.getSharedPreferences(HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).edit();
			for (ConvInfo conv : convs)
			{
				
				 if (conv instanceof OneToNConvInfo)
				{
					//TODO in case of leaving group from group info screen ==> 2 gcl event will trigger
					//we can avoid these by moving delete conversation task to db
					HikeMqttManagerNew.getInstance().sendMessage(conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), HikeMqttManagerNew.MQTT_QOS_ONE);
				}

				msisdns.add(conv.getMsisdn());
				editor.remove(conv.getMsisdn());
			}
			editor.commit();

			db = HikeConversationsDatabase.getInstance();
			db.deleteConversation(msisdns);

			ContactManager.getInstance().removeContacts(msisdns);
			return convs;
		}

		@Override
		protected void onPostExecute(ConvInfo[] deleted)
		{
			if (!isAdded())
			{
				return;
			}
			NotificationManager mgr = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			for (ConvInfo convInfo : deleted)
			{
				mAdapter.remove(convInfo);
				mConversationsByMSISDN.remove(convInfo.getMsisdn());
				mConversationsAdded.remove(convInfo.getMsisdn());
				resetSearchIcon();

				HikeMessengerApp.removeStealthMsisdn(convInfo.getMsisdn(), publishStealthEvent);
				stealthConversations.remove(convInfo);
			}

			notifyDataSetChanged();

			if (mAdapter.getCount() == 0)
			{
				setEmptyState();
			}
		}
	}

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MESSAGE_DELIVERED,
			HikePubSub.NEW_CONVERSATION, HikePubSub.MESSAGE_SENT, HikePubSub.MSG_READ, HikePubSub.ICON_CHANGED, HikePubSub.GROUP_NAME_CHANGED, HikePubSub.CONTACT_ADDED,
			HikePubSub.LAST_MESSAGE_DELETED, HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.GROUP_LEFT,
			HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.CLEAR_CONVERSATION, HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, 
			HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP, HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, HikePubSub.STEALTH_MODE_TOGGLED, HikePubSub.CLEAR_FTUE_STEALTH_CONV,
			HikePubSub.RESET_STEALTH_INITIATED, HikePubSub.RESET_STEALTH_CANCELLED, HikePubSub.REMOVE_WELCOME_HIKE_TIP, HikePubSub.REMOVE_STEALTH_INFO_TIP,
			HikePubSub.REMOVE_STEALTH_UNREAD_TIP, HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.ONETON_MESSAGE_DELIVERED_READ, HikePubSub.BULK_MESSAGE_DELIVERED_READ, HikePubSub.GROUP_END,
			HikePubSub.CONTACT_DELETED,HikePubSub.MULTI_MESSAGE_DB_INSERTED, HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.CONV_UNREAD_COUNT_MODIFIED,
			HikePubSub.CONVERSATION_TS_UPDATED, HikePubSub.PARTICIPANT_JOINED_ONETONCONV, HikePubSub.PARTICIPANT_LEFT_ONETONCONV};

	private ConversationsAdapter mAdapter;

	private HashMap<String, ConvInfo> mConversationsByMSISDN;

	private HashSet<String> mConversationsAdded;

	private Comparator<? super ConvInfo> mConversationsComparator;

	private View emptyView;

	private Set<ConvInfo> stealthConversations;

	private List<ConvInfo> displayedConversations;

	private boolean showingStealthFtueConvTip = false;

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
	
	private int tipType = ConversationTip.NO_TIP;

	private enum hikeBotConvStat
	{
		NOTVIEWED, VIEWED, DELETED
	};

	public  enum footerState
	{
		OPEN(0), HALFOPEN(1), CLOSED(2);
		
		private static footerState val=CLOSED;

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
		ListView friendsList = (ListView) getView().findViewById(android.R.id.list);
		View searchEmptyView = getView().findViewById(R.id.searchEmptyView);
		searchEmptyView.setVisibility(View.VISIBLE);
		String emptyText = String.format(getActivity().getString(R.string.home_search_empty_text), searchText);
		TextView emptyTextView = (TextView) searchEmptyView.findViewById(R.id.empty_search_txt);
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
		friendsList.setEmptyView(searchEmptyView);
	}

	private void bindNuxViews(final View root)
	{
		footercontroller = (ImageView) root.findViewById(R.id.imgv_nux);
		footercontroller.setOnClickListener(this);

		llShadow=(LinearLayout)root.findViewById(R.id.ll_shadow);
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
				getListView().setPadding(0, 0, 0, footercontroller.getHeight());
				
			}
		});
		Logger.d("footer","changeFooterState");
		if (!NUXManager.getInstance().isReminderReceived())
		{
			if (!Utils.isHoneycombOrHigher())
			{
				llChatReward.setVisibility(View.GONE);
				llInviteOptions.setVisibility(View.GONE);
			}
			else
			{
				/**
				 * Adding an Global Listener to close the footer on opening ...
				 */
//				llInviteOptions.addOnLayoutChangeListener(new OnLayoutChangeListener()
//				{
//
//					@Override
//					public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
//					{
//						ObjectAnimator.ofFloat(llNuxFooter, "translationY", llChatReward.getHeight() + llInviteOptions.getHeight()).setDuration(0).start();
//						llInviteOptions.removeOnLayoutChangeListener(this);
//					}
//				});
					
				

				llInviteOptions.post(new Runnable()
				{
					
					@Override
					public void run()
					{
						ObjectAnimator.ofFloat(llNuxFooter, "translationY", llChatReward.getHeight() + llInviteOptions.getHeight()).setDuration(0).start();
						
					}
				});
			}
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
	 * @param state
	 */
	public void changeFooterControllerBackground(footerState state)
	{
		switch(footerState.getEnum())
		{
		case OPEN:
			footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_downarrow));
			break;
		case HALFOPEN:
			NUXManager nm=NUXManager.getInstance();
			NuxSelectFriends mmSelectFriends=NUXManager.getInstance().getNuxSelectFriendsPojo();
//			if (!(mmSelectFriends.isModuleToggle() || nm.getCurrentState() == NUXConstants.COMPLETED))
//			{
//				footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_uparrow));
//			}
//			else
//			{
				footercontroller.setImageDrawable(getResources().getDrawable(R.drawable.btn_downarrow));
			//}
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
		NuxSelectFriends mmSelectFriends=mmNuxManager.getNuxSelectFriendsPojo();
		
		
		//Setting lock icon image 0 unlocked-->red ,>0<Min->orange,    ==Min&&state=completed  -->Green
		
		if(mmNuxManager.getCountUnlockedContacts()==0)
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
		
			setFooterHalfOpen();
		}

		butInviteMore.setText(mmReward.getInviteMoreButtonText());

		butRemind.setText(mmReward.getRemindButtonText());

		rewardCard.setText(String.format(mmReward.getRewardCardText(), mmDetails.getMin()));

		//NUX Skipped state ...
		
		if (mmNuxManager.getCurrentState() == NUXConstants.NUX_SKIPPED)
		{
			butRemind.setVisibility(View.GONE);
			butInviteMore.setText(mmReward.getSelectFriendsText());
		}
		
		if (mmNuxManager.getCurrentState() == NUXConstants.NUX_IS_ACTIVE) {
			butRemind.setVisibility(View.VISIBLE);

			// condition when the total contacts selected = max,then remind button should show and invite more should be hidden
			
			if (mmNuxManager.getCountLockedContacts()
					+ mmNuxManager.getCountUnlockedContacts() == mmDetails.getMax()) {
				butInviteMore.setVisibility(View.GONE);
			
			}
		}
		
		
		if (!(mmNuxManager.getCurrentState()==NUXConstants.COMPLETED))
		{
			progressNux.setProgress(NUXManager.getInstance().getCountUnlockedContacts() / ((float) mmDetails.getMin()));
		
			if(footerState.getEnum()==footerState.HALFOPEN)
			{
				
			
			chatProgress.setText(String.format(mmReward.getStatusText(), mmNuxManager.getCountUnlockedContacts(), mmDetails.getMin()));
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

			//lockImage.startAnimation(pulse);
			
			// Changing the footer state to halfOpen when the reward is unlocked.
			setFooterHalfOpen();
			
			
			//llChatReward.setOnClickListener(null);
			
			progressNux.setVisibility(View.GONE);
		}
	}

	
	public void setFooterHalfOpen()
	{
		if (footerState.getEnum() == footerState.OPEN)
		{
			if (Utils.isHoneycombOrHigher())
			
			{
				
				// This is done to handle the footer on home button pressed when the view is already inflated.
//				if (llInviteOptions.getHeight()>0)
//					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).setDuration(0).start();
//				else
//				{
//					llInviteOptions.addOnLayoutChangeListener(new OnLayoutChangeListener()
//					{
// 
//						@Override
//						public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
//						{
//							ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).setDuration(0).start();
//
//							llInviteOptions.removeOnLayoutChangeListener(this);
//						}
//					});
//				}
				
				
				llInviteOptions.post(new Runnable()
				{
					
					@Override
					public void run()
					{
						ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).setDuration(0).start();
						
					}
				});
			}

			else
			{
				llInviteOptions.setVisibility(View.GONE);
			}
			
			
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

				if (Utils.isHoneycombOrHigher())
				{
					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llChatReward.getHeight() + llInviteOptions.getHeight()).start();
				}
				else
				{
					llChatReward.setVisibility(View.GONE);
					llInviteOptions.setVisibility(View.GONE);
				}

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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			case HALFOPEN:
				
				
				if (Utils.isHoneycombOrHigher())
					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llNuxFooter.getHeight() - footercontroller.getHeight()-llShadow.getHeight()).start();
				else
				{
					llChatReward.setVisibility(View.GONE);
				}
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

				//Will remove after testing
				
				if(mmNuxManager.getCurrentState() == NUXConstants.NUX_SKIPPED || mmNuxManager.getCurrentState() == NUXConstants.NUX_IS_ACTIVE)
				{
					chatProgress.setText(String.format(mmReward.getStatusText(), mmNuxManager.getCountUnlockedContacts(), mmDetails.getMin()));
					progressNux.setProgress(NUXManager.getInstance().getCountUnlockedContacts() / mmDetails.getMin());
				}
				if (Utils.isHoneycombOrHigher())
					ObjectAnimator.ofFloat(llNuxFooter, "translationY", llInviteOptions.getHeight()).start();
				else
				{
					llChatReward.setVisibility(View.VISIBLE);
					llInviteOptions.setVisibility(View.GONE);
				}
				
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
			
			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED||mmNuxManager.getCurrentState()==NUXConstants.NUX_NEW)
			{
				Toast.makeText(getActivity(), getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
			}
			else if(mmNuxManager.getCurrentState()==NUXConstants.COMPLETED)
			{
				onClick(chatProgress);
			}
			else
			{
			chatProgress.setText(NUXManager.getInstance().getNuxChatRewardPojo().getDetailsText());

		
			if (Utils.isHoneycombOrHigher())
			{
				ObjectAnimator.ofFloat(llNuxFooter, "translationY", 0).start();
			}
			else
			{
				llInviteOptions.setVisibility(View.VISIBLE);
			}

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

		//On click of TAP To Claim and View Details	
			
			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED||mmNuxManager.getCurrentState()==NUXConstants.NUX_NEW)
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
					
					
					HikeSharedPreferenceUtil mprefs=HikeSharedPreferenceUtil.getInstance();
					String tapToClaim=HikeConstants.ANDROID+"/"+ mprefs.getData(HikeMessengerApp.REWARDS_TOKEN, "");
					String title= getString(R.string.hike);
				
					String link=String.format(mmReward.getTapToClaimLink(),tapToClaim);
					Utils.startWebViewActivity(getActivity(),  link, title);
					
					
					
				}
			}

			else if (footerState.getEnum() == footerState.OPEN)
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
					String tapToClaim = HikeConstants.ANDROID+"/"+ mprefs.getData(HikeMessengerApp.REWARDS_TOKEN, "");
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

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED||mmNuxManager.getCurrentState()==NUXConstants.NUX_NEW) 
			{
				Toast.makeText(getActivity(),  getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
				break;
				
			}
		
			if( mmNuxManager.getCurrentState() == NUXConstants.COMPLETED)
			{
				Toast.makeText(getActivity(),  getActivity().getString(R.string.nux_completed), Toast.LENGTH_SHORT).show();
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

			if (mmNuxManager.getCurrentState() == NUXConstants.NUX_KILLED||mmNuxManager.getCurrentState()==NUXConstants.NUX_NEW) 
			{
				Toast.makeText(getActivity(),  getActivity().getString(R.string.nux_expired), Toast.LENGTH_SHORT).show();
				break;
				
			} 
			if( mmNuxManager.getCurrentState() == NUXConstants.COMPLETED)
			{
				Toast.makeText(getActivity(),  getActivity().getString(R.string.nux_completed), Toast.LENGTH_SHORT).show();
				break;
			}
				NUXManager.getInstance().startNuxSelector(getActivity());
			break;

		}
	}

	private void setEmptyState()
	{
		// Adding wasViewSetup() safety check for an NPE here.
		if (wasViewSetup())
		{
			if (emptyView == null)
			{
				ViewGroup emptyHolder = (ViewGroup) getView().findViewById(R.id.emptyViewHolder);
				emptyView = LayoutInflater.from(getActivity()).inflate(R.layout.conversation_empty_view2, emptyHolder);
				//emptyHolder.addView(emptyView);
			}
			
			if (searchMode)
			{
				if (!TextUtils.isEmpty(searchText))
				{
					emptyView.setVisibility(View.GONE);
					getView().findViewById(R.id.searchEmptyView).setVisibility(View.VISIBLE);
					setSearchEmptyState();
				}
				else if (mConversationsByMSISDN.isEmpty())
				{
					getView().findViewById(R.id.searchEmptyView).setVisibility(View.GONE);
					emptyView.setVisibility(View.VISIBLE);
					ListView friendsList = (ListView) getView().findViewById(android.R.id.list);
					friendsList.setEmptyView(emptyView);
				}
			}
			else
			{
				getView().findViewById(R.id.searchEmptyView).setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				ListView friendsList = (ListView) getView().findViewById(android.R.id.list);
				friendsList.setEmptyView(emptyView);
			}
		}
	}

	private void setupFTUEEmptyView()
	{

		if (emptyView == null || !isAdded() || searchMode)
		{
			return;
		}

		ListView ftueListView = (ListView) emptyView.findViewById(R.id.ftue_list);
		List<EmptyConversationItem> ftueListItems= new ArrayList<EmptyConversationItem>();

		if(!HomeActivity.ftueContactsData.getHikeContacts().isEmpty())
		{
			int hikeContactCount = HomeActivity.ftueContactsData.getTotalHikeContactsCount();
			EmptyConversationItem hikeContactsItem = new EmptyConversationContactItem(HomeActivity.ftueContactsData.getHikeContacts(), getResources().getString(
					R.string.ftue_hike_contact_card_header, hikeContactCount), EmptyConversationItem.HIKE_CONTACTS);
			ftueListItems.add(hikeContactsItem);
		}
		/*
		 * We only add this item if hike contacts are less than 
		 * certain threashold
		 */
		if(HomeActivity.ftueContactsData.getHikeContacts().size() == 0
				&& !HomeActivity.ftueContactsData.getSmsContacts().isEmpty())
		{
			int smsContactCount = HomeActivity.ftueContactsData.getTotalSmsContactsCount();
			EmptyConversationItem hikeContactsItem = new EmptyConversationContactItem(HomeActivity.ftueContactsData.getSmsContacts(), getResources().getString(R.string.ftue_sms_contact_card_header, smsContactCount), EmptyConversationItem.SMS_CONTACTS);
			ftueListItems.add(hikeContactsItem);
		}
		if (ftueListView != null)
		{
		if (ftueListView.getFooterViewsCount() == 0)
		{
			addBottomPadding(ftueListView);
		}
		if(HomeActivity.ftueContactsData.getHikeContacts().isEmpty())
		{
			addFtueCards(ftueListItems);
		}
		ftueListView.setAdapter(new EmptyConversationsAdapter(getActivity(), -1, ftueListItems));
		}
	}

	private void addFtueCards(List<EmptyConversationItem> ftueListItems)
	{
		ftueListItems.add(new EmptyConversationItem(EmptyConversationItem.SEPERATOR));
		
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.LAST_SEEN, R.drawable.ftue_card_last_seen_img_small, getResources().getColor(R.color.ftue_card_last_seen),
				R.string.ftue_card_header_last_seen, R.string.ftue_card_body_last_seen, R.string.ftue_card_click_text_last_seen, getResources().getColor(R.color.ftue_card_last_seen_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.GROUP, R.drawable.ftue_card_group_img_small, getResources().getColor(R.color.ftue_card_group),
				R.string.group_chat, R.string.ftue_card_body_group, R.string.ftue_card_click_group, getResources().getColor(R.color.ftue_card_group_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.INVITE, R.drawable.ftue_card_invite_img_small, getResources().getColor(R.color.ftue_card_invite),
				R.string.invite_friends, R.string.ftue_card_body_invite, R.string.ftue_card_click_invite, getResources().getColor(R.color.ftue_card_invite_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.HIKE_OFFLINE, R.drawable.ftue_card_hike_offline_img_small, getResources().getColor(R.color.ftue_card_hike_offline),
				R.string.ftue_card_header_hike_offline, R.string.ftue_card_body_hike_offline, R.string.ftue_card_click_text_hike_offline, getResources().getColor(R.color.ftue_card_hike_offline_click_text)));
		ftueListItems.add(new EmptyConversationFtueCardItem(EmptyConversationItem.STICKERS, R.drawable.ftue_card_sticker_img_small, getResources().getColor(R.color.ftue_card_sticker),
				R.string.ftue_card_header_sticker, R.string.ftue_card_body_sticker, R.string.ftue_card_click_text_sticker, getResources().getColor(R.color.ftue_card_sticker_click_text)));
	}

	/*
	 * We are adding this footer in empty state list view
	 * to give proper padding at the bottom of the list.
	 */
	private void addBottomPadding(ListView ftueListView)
	{
		View paddingView = LayoutInflater.from(getActivity()).inflate(
				R.layout.ftue_list_padding_footer_view, null);
		ftueListView.addFooterView(paddingView);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		Logger.d("footer","onActivityCreated");
		if (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_IS_ACTIVE
				|| (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_SKIPPED)||(NUXManager.getInstance().getCurrentState() == NUXConstants.COMPLETED))
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
		mConversationsComparator = new ConvInfo.ConvInfoComparator();
		fetchConversations();

		for (TypingNotification typingNotification : HikeMessengerApp.getTypingNotificationSet().values())
		{
			toggleTypingNotification(true, typingNotification);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		resetSearchIcon();
	}

	private void resetSearchIcon()
	{
		if (!searchMode)
		{
			HomeActivity.setSearchOptionAccess(!isConversationsEmpty());
		}
	}

	private boolean isConversationsEmpty()
	{
		return (displayedConversations != null && displayedConversations.isEmpty()) ? true : false;
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		
		if (!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			// if stealth setup is not done and user has marked some chats as stealth unmark all of them
			for (ConvInfo convInfo : stealthConversations)
			{
				convInfo.setStealth(false);
				HikeConversationsDatabase.getInstance().toggleStealth(convInfo.getMsisdn(), false);
			}

			HikeMessengerApp.clearStealthMsisdn();
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		}

		super.onDestroy();
	}

	@Override
	public void onStop()
	{
		// TODO Auto-generated method stub
		super.onStop();
		if (showingStealthFtueConvTip)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			removeStealthConvTip();
		}
		
		if (showingWelcomeHikeConvTip)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			removeTipIfExists(ConversationTip.WELCOME_HIKE_TIP);
		}

	}
	
	@Override
	public void onPause()
	{
		super.onPause();

		if(mAdapter != null)
		{
			mAdapter.getIconLoader().setExitTasksEarly(true);
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
		
		Intent intent = IntentFactory.createChatThreadIntentFromConversation(getSherlockActivity(), convInfo);
		startActivity(intent);
		
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
			metadata
			.put(HikeConstants.EVENT_KEY, SEARCH_RESULT)
			.put(SEARCH_TEXT, text)
			.put(INDEX, position)
			.put(RESULT_TYPE, resultType);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_HOME_SEARCH, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	public void setupSearch()
	{
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
			setEmptyState();
			mAdapter.onQueryChanged(searchText);
		}
	}

	public void removeSearch()
	{
		if (mAdapter != null)
		{
			searchText = null;
			searchMode = false;
			setEmptyState();
			mAdapter.removeSearch();
		}
	}
// TODO
//	private void resetStealthTipClicked()
//	{
//		long remainingTime = System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l);
//
//		if (remainingTime > HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS)
//		{
//			Object[] dialogStrings = new Object[4];
//			dialogStrings[0] = getString(R.string.reset_complete_stealth_header);
//			dialogStrings[1] = getString(R.string.reset_stealth_confirmation);
//			dialogStrings[2] = getString(R.string.confirm);
//			dialogStrings[3] = getString(R.string.cancel);
//
//			HikeDialog.showDialog(getActivity(), HikeDialog.RESET_STEALTH_DIALOG, new HikeDialog.HikeDialogListener()
//			{
//
//				@Override
//				public void positiveClicked(Dialog dialog)
//				{
//					HikeAnalyticsEvent.sendStealthReset();
//					resetStealthMode();
//					dialog.dismiss();
//				}
//
//				@Override
//				public void neutralClicked(Dialog dialog)
//				{
//
//				}
//
//				@Override
//				public void negativeClicked(Dialog dialog)
//				{
//					removeTipIfExists(ConversationTip.RESET_STEALTH_TIP);
//
//					Utils.cancelScheduledStealthReset();
//
//					dialog.dismiss();
//
//					try
//					{
//						JSONObject metadata = new JSONObject();
//						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
//						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
//					}
//					catch(JSONException e)
//					{
//						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
//					}
//				}
//
//				@Override
//				public void onSucess(Dialog dialog)
//				{
//					// TODO Auto-generated method stub
//					
//				}
//			}, dialogStrings);
//		}
//	}

//	TODO
//	private void resetStealthMode()
//	{
//		removeTipIfExists(ConversationTip.RESET_STEALTH_TIP);
//
//		int prevStealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
//
//		resetStealthPreferences();
//
//		/*
//		 * If previously the stealth mode was off, we should publish an event telling the friends fragment to refresh its list.
//		 */
//		if (prevStealthValue == HikeConstants.STEALTH_OFF)
//		{
//			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_RESET_COMPLETE, null);
//		}
//
//		/*
//		 * Calling the delete conversation task in the end to ensure that we first publish the reset event. If the delete task was published at first, it was causing a threading
//		 * issue where the contacts in the friends fragment were getting removed and not added again.
//		 */
//		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity(), false);
//		task.execute(stealthConversations.toArray(new ConvInfo[0]));
//
//		HikeMessengerApp.clearStealthMsisdn();
//	}

	private void resetStealthPreferences()
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

		prefUtil.removeData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE);
		prefUtil.removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		prefUtil.removeData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME);
		prefUtil.removeData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST);
		DBBackupRestore.getInstance(getActivity()).updatePrefs();
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

        if (Utils.isBot(conv.getMsisdn()))
        {
            BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_LONG_PRESS, AnalyticsConstants.LONG_PRESS_EVENT);
        }

		final int stealthType = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthType == HikeConstants.STEALTH_ON || stealthType == HikeConstants.STEALTH_ON_FAKE)
		{
			optionsList.add(getString(conv.isStealth() ? R.string.unmark_stealth : R.string.mark_stealth));
		}
		if (!(conv instanceof OneToNConvInfo) && conv.getConversationName() == null)
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
				if (Utils.isBroadcastConversation(conv.getMsisdn()))
				{
					optionsList.add(getString(R.string.broadcast_info));
				}
				else
				{
					optionsList.add(getString(R.string.group_info));
				}
		}
		if (conv.getConversationName() != null)
		{
			optionsList.add(getString(R.string.shortcut));

		}

		if (!(conv instanceof OneToNConvInfo) && conv.getConversationName() == null)
		{
			optionsList.add(ContactManager.getInstance().isBlocked(conv.getMsisdn())?getString(R.string.unblock_title):getString(R.string.block_title));
		}
		if (Utils.isGroupConversation(conv.getMsisdn()))
		{
			optionsList.add(getString(R.string.delete_leave));
		}
		
		else if (Utils.isBroadcastConversation(conv.getMsisdn()))
		{
			optionsList.add(getString(R.string.delete_broadcast));
		}
		else
		{
			optionsList.add(getString(R.string.delete_chat));
		}

		//Showing "Clear Whole Conv" option in Both Group and One-to-One Chat
		optionsList.add(getString(R.string.clear_whole_conversation));
		
		optionsList.add(getString(R.string.email_conversations));

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
                    if (Utils.isBot(conv.getMsisdn()))
                    {
                        BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_ADD_SHORTCUT, AnalyticsConstants.CLICK_EVENT);
                    }
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.ADD_SHORTCUT);
					Utils.createShortcut(getSherlockActivity(), conv);
				}
				else if (getString(R.string.delete_chat).equals(option))
				{
					HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_CHAT_DIALOG, new HikeDialogListener()
					{
						
						@Override
						public void positiveClicked(HikeDialog hikeDialog)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
							Utils.executeConvInfoAsyncTask(task, conv);
							hikeDialog.dismiss();
                            if (Utils.isBot(conv.getMsisdn()))
                            {
                                BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_DELETE_CHAT, AnalyticsConstants.CLICK_EVENT);
                            }
						}
						
						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}
						
						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
						}
					}, conv.getLabel());
					
				}
				else if (getString(R.string.delete_leave).equals(option))
				{
					HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_GROUP_DIALOG, new HikeDialogListener()
					{
						
						@Override
						public void positiveClicked(HikeDialog hikeDialog)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							deleteConversation(conv);
							hikeDialog.dismiss();
						}
						
						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}
						
						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
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
							deleteConversation(conv);
							hikeDialog.dismiss();
						}
						
						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
						}
						
						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
						}
					}, conv.getLabel());
				}
				else if (getString(R.string.email_conversations).equals(option))
				{
					EmailConversationsAsyncTask task = new EmailConversationsAsyncTask(getSherlockActivity(), ConversationFragment.this);
					Utils.executeConvAsyncTask(task, conv);
                    if (Utils.isBot(conv.getMsisdn()))
                    {
                        BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_EMAIL_CONVERSATION, AnalyticsConstants.CLICK_EVENT);
                    }
				}
				else if (getString(R.string.deleteconversations).equals(option))
				{
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
					DeleteAllConversations();
				}
				else if (getString(R.string.viewcontact).equals(option))
				{
					viewContacts(conv);
                    if (Utils.isBot(conv.getMsisdn()))
                    {
                        BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_VIEW_PROFILE, AnalyticsConstants.CLICK_EVENT);
                    }
				}
				else if (getString(R.string.clear_whole_conversation).equals(option))
				{
					clearConversation(conv);
				    if (Utils.isBot(conv.getMsisdn()))
                    {
				    	BotConversation.analyticsForBots(conv, HikePlatformConstants.BOT_CLEAR_CONVERSATION,  AnalyticsConstants.CLICK_EVENT);
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
					if (!((OneToNConvInfo) conv).isConversationAlive())
					{
						return;
					}
					viewGroupInfo(conv);
				}
				else if (getString(R.string.mark_stealth).equals(option) || getString(R.string.unmark_stealth).equals(option))
				{
					boolean newStealthValue = !conv.isStealth();
					/*
					 * If stealth ftue conv tap tip is visible than remove it
					 */
					if (!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
					{
							if (tipView != null && tipType == ConversationTip.STEALTH_FTUE_TIP)
							{
								removeStealthConvTip();
							}
					}
					else
					{
						// We don't show this toast during stealth ftue setup.
						if(newStealthValue)
						{
							Toast.makeText(getActivity(), R.string.chat_marked_stealth, Toast.LENGTH_SHORT).show();
						}
						else if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, false))
						{
							Toast.makeText(getActivity(), R.string.chat_unmarked_stealth_first, Toast.LENGTH_LONG).show();
							HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, true);
						}
						else
						{
							Toast.makeText(getActivity(), R.string.chat_unmarked_stealth, Toast.LENGTH_SHORT).show();
						}
					}

					if (stealthType == HikeConstants.STEALTH_ON_FAKE)
					{
						/*
						 * We don't need to do anything here if the device is on fake stealth mode.
						 */
						return;
					}
					if (getString(R.string.mark_stealth).equals(option))
					{
						List<String> enabledConvs = new ArrayList<String>(1);
						enabledConvs.add(conv.getMsisdn());
						HikeAnalyticsEvent.sendStealthMsisdns(enabledConvs, null);

						stealthConversations.add(conv);
						HikeMessengerApp.addNewStealthMsisdn(conv.getMsisdn());
					}
					else
					{
						List<String> disabledConvs = new ArrayList<String>(1);
						disabledConvs.add(conv.getMsisdn());
						HikeAnalyticsEvent.sendStealthMsisdns(null, disabledConvs);

						stealthConversations.remove(conv);
						HikeMessengerApp.removeStealthMsisdn(conv.getMsisdn());
					}

					conv.setStealth(newStealthValue);

					HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), newStealthValue);

					notifyDataSetChanged();

					if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
						HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_SET_PASS_TIP, null);
					}
				}
				else if (getString(R.string.block_title).equals(option))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, conv.getMsisdn());
				}
				else if (getString(R.string.unblock_title).equals(option))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, conv.getMsisdn());
				}
			}
		});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
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
			}
		}, null);
	}

	private void fetchConversations()
	{
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		displayedConversations = new ArrayList<ConvInfo>();
		List<ConvInfo> conversationList = db.getConvInfoObjects();

		stealthConversations = new HashSet<ConvInfo>();

		displayedConversations.addAll(conversationList);

		mConversationsByMSISDN = new HashMap<String, ConvInfo>(displayedConversations.size());
		mConversationsAdded = new HashSet<String>();

		setupConversationLists();
		resetSearchIcon();

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		ShowTipIfNeeded(displayedConversations.isEmpty());
		
		mAdapter = new ConversationsAdapter(getActivity(), displayedConversations, stealthConversations, getListView());

		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(this);
		getListView().setOnScrollListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		if (displayedConversations.isEmpty())
		{
			setEmptyState();
		}
	}

	private void ShowTipIfNeeded(boolean hasNoConversation)
	{
		// to prevent more than one tip to display at a time , it can happen at time of onnewintent
		if(!hasNoConversation && tipView != null){
			checkAndRemoveExistingHeaders();
		}
		
		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}
		
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String tip = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
		Logger.i("tip", "#" + tip + "#-currenttype");
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
		{
			tipType = ConversationTip.RESET_STEALTH_TIP;
		}
		else if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, false))
		{
			showingWelcomeHikeConvTip = true;
			tipType = ConversationTip.WELCOME_HIKE_TIP;
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false) &&  displayedConversations.size() > 2)
		{
			tipType = ConversationTip.STEALTH_INFO_TIP;
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false))
		{
			tipType = ConversationTip.STEALTH_UNREAD_TIP;
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
		
		tipView = convTip.getView(tipType);
		
		if (tipView != null)
		{
			getListView().addHeaderView(tipView);
		}
	}

	private void setupConversationLists()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

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
				HikeMessengerApp.addStealthMsisdnToMap(convInfo.getMsisdn());
			}

			if ((stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE) && convInfo.isStealth())
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

	private void changeConversationsVisibility()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE)
		{
			mAdapter.removeStealthConversationsFromLists();

			if (mAdapter.getCount() == 0)
			{
				setEmptyState();
			}
		}
		else
		{
			mAdapter.addItemsToAnimat(stealthConversations);
			mAdapter.addToLists(stealthConversations);
		}

		resetSearchIcon();
		mAdapter.sortLists(mConversationsComparator);
		notifyDataSetChanged();
	}

	private void deleteConversation(ConvInfo conv)
	{
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
		Utils.executeConvInfoAsyncTask(task, conv);
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
		{	// If we were not already typing and we got isTyping as true, we set typing flag
			if (convInfo.getTypingNotif() == null)
			{
				convInfo.setTypingNotif(typingNotification);
				View parentView = getParenViewForConversation(convInfo);
				if (parentView == null)
				{
					notifyDataSetChanged();
				}
				
				mAdapter.updateViewsRelatedToTypingNotif(parentView, convInfo);
			}
		}
		else
		{	// If we were already typing and we got isTyping as false, we remove the typing flag
			if (convInfo.getTypingNotif() != null)
			{
				convInfo.setTypingNotif(null);
				View parentView = getParenViewForConversation(convInfo);
				if (parentView == null)
				{
					notifyDataSetChanged();
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
		mAdapter.notifyDataSetChanged();
	}

	public void updateTimestampAndSortConversations(Object object)
	{
		Logger.d("ListeningTSPubsub", "listening");
		Pair<String, Long> p = (Pair<String, Long>) object;
		String msisdn = p.first;
		Long ts = p.second;
		
		final ConvInfo convInfo = mConversationsByMSISDN.get(msisdn);
		if (convInfo!= null)
		{	
			if (ts>=0)
			{
				convInfo.setSortingTimeStamp(ts);
			}
			
			Collections.sort(displayedConversations, mConversationsComparator);
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
	
	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(String type, Object object)
	{
		if (!isAdded())
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "Event received: " + type);
		
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)) )
		{
			Logger.d(getClass().getSimpleName(), "New msg event sent or received.");
			ConvMessage message = (ConvMessage) object;
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
							prevMessage.getMappedMsgID(), message.getGroupParticipantMsisdn());
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
						mConversationsByMSISDN.remove(msisdn);
						mConversationsAdded.remove(msisdn);
						mAdapter.remove(convInfo);
						notifyDataSetChanged();
						resetSearchIcon();
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
			if (HikeMessengerApp.hikeBotNamesMap.containsKey(convInfo.getMsisdn()))
			{
				convInfo.setmConversationName(HikeMessengerApp.hikeBotNamesMap.get(convInfo.getMsisdn()));
			}
			Logger.d(getClass().getSimpleName(), "New Conversation. Group Conversation? " + (Utils.isGroupConversation(convInfo.getMsisdn())));
			mConversationsByMSISDN.put(convInfo.getMsisdn(), convInfo);
			if (convInfo.getLastConversationMsg() == null && !(Utils.isGroupConversation(convInfo.getMsisdn())))
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
					if(displayedConversations.isEmpty())
					{
						/*
						 * start new chat tip will come if user is on home empty state and
						 * a new conversation comes.
						 */
						movedFromEmptyToNonEmpty();
					}
					else if(displayedConversations.size() == 2)
					{
						if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
						{
							showStealthInfoTip();
						}
					}
					mAdapter.addToLists(convInfo);
					mAdapter.sortLists(mConversationsComparator);
					notifyDataSetChanged();
					resetSearchIcon();
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
			HikeConversationsDatabase.getInstance().setExtraConvUnreadCount(msisdn, 0);
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
			Pair<Long, Integer> p  = (Pair<Long, Integer>) object;
			long baseId = p.first;
			int count = p.second;
			for(long msgId=baseId; msgId<(baseId+count) ; msgId++)
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
				Pair<String, Pair<Long,String>> pair = (Pair<String, Pair<Long, String>>) object;
				sender = pair.first;
				ids = new long[] { pair.second.first} ;
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
					
					updateViewForMessageStateChange(convInfo, message);
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
						
						updateViewForMessageStateChange(convInfo, msg);
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
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
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
				if(HikePubSub.CONTACT_DELETED.equals(type))
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
						if(HikePubSub.CONTACT_DELETED.equals(mType))
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
		else if (HikePubSub.GROUP_LEFT.equals(type))
		{
			String groupId = (String) object;
			final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
			if (convInfo == null)
			{
				return;
			}

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					deleteConversation(convInfo);
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
					setEmptyState();
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
		else if (HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP.equals(type))
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
					removeStealthConvTip();
				}
			});
		}
		else if (HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP.equals(type))
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
					showStealthConvTip();
				}
			});

		}
		else if (HikePubSub.STEALTH_MODE_TOGGLED.equals(type))
		{
			boolean changeItemsVisibility = (Boolean) object;

			if (!changeItemsVisibility)
			{
				return;
			}

			if (!isAdded())
			{
				return;
			}

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					changeConversationsVisibility();
				}
			});
		}
		else if (HikePubSub.CLEAR_FTUE_STEALTH_CONV.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			
			/*
			 * If the stealth mode is already setup than we should return;
			 */
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
			{
				return ;
			}

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					// if stealth setup is not done and user has marked some chats as stealth unmark all of them
					for (ConvInfo convInfo : stealthConversations)
					{
						convInfo.setStealth(false);
						HikeConversationsDatabase.getInstance().toggleStealth(convInfo.getMsisdn(), false);
						HikeMessengerApp.removeStealthMsisdn(convInfo.getMsisdn());
					}
					mAdapter.addToLists(stealthConversations);
					stealthConversations.clear();
					mAdapter.sortLists(mConversationsComparator);
					notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.RESET_STEALTH_INITIATED.equals(type))
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
					getFirstConversation();
					//Adding stealth reset tip
					showStealthResetConvTip();
					notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.RESET_STEALTH_CANCELLED.equals(type))
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
					removeTipIfExists(ConversationTip.RESET_STEALTH_TIP);
				}
			});
		}
		else if (HikePubSub.REMOVE_WELCOME_HIKE_TIP.equals(type))
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
					removeTipIfExists(ConversationTip.WELCOME_HIKE_TIP);
				}
			});
		}
		else if (HikePubSub.REMOVE_STEALTH_INFO_TIP.equals(type))
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
					removeTipIfExists(ConversationTip.STEALTH_INFO_TIP);
				}
			});
		}
		else if (HikePubSub.REMOVE_STEALTH_UNREAD_TIP.equals(type))
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
					removeTipIfExists(ConversationTip.STEALTH_UNREAD_TIP);
				}
			});
		}
		/*
		 * The list of messages is processed.
		 * The messages are added and the UI is updated at once.
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
							for (ConvMessage convMessage : messageList)
							{
								if (Utils.shouldIncrementCounter(convMessage))
								{
									unreadCount++ ;
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
									message = new ConvMessage(message.getMessage(), message.getMsisdn(), prevMessage.getTimestamp(), prevMessage.getState(), prevMessage.getMsgID(),
											prevMessage.getMappedMsgID(), message.getGroupParticipantMsisdn());
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
							
							if (!isAdded())
							{
								return;
							}
							getActivity().runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									addMessage(convInfo,finalMessage,false);
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
		 * The list of msisdns and their maximum ids for DR and MR packets is received.
		 * The messages are updated in the chat thread.
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
		else if(HikePubSub.GROUP_END.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if(groupId != null)
			{
				final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
				if (convInfo == null)
				{
					return;
				}
				((OneToNConvInfo) convInfo).setConversationAlive(false);
			}
		}
		else if(HikePubSub.MULTI_MESSAGE_DB_INSERTED.equals(type)){
			if (!isAdded())
			{
				return;
			}
			Logger.d(getClass().getSimpleName(), "New msg event sent or received.");
			List<Pair<ContactInfo, ConvMessage>> allPairs = (List<Pair<ContactInfo, ConvMessage>>) object;
			for(Pair<ContactInfo, ConvMessage> contactMessagePair: allPairs){
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

				if (convInfo.getLastConversationMsg() != null) {
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
				public void run() {
					mAdapter.sortLists(mConversationsComparator);
					notifyDataSetChanged();
				};
			});
		}
		else if(HikePubSub.CONV_UNREAD_COUNT_MODIFIED.equals(type))
		{
			unreadCountModified((Message) object);
		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;
			String groupId = groupMute.first;
			final Boolean isMuted = groupMute.second;

			final ConvInfo convInfo = mConversationsByMSISDN.get(groupId);
			
			if (convInfo == null)
			{
				return ;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					View parentView = getParenViewForConversation(convInfo);
					if (parentView == null)
					{
						notifyDataSetChanged();
						return;
					}

					convInfo.setMute(true);

					notifyDataSetChanged();
				}
			});
		}
		else if(HikePubSub.CONVERSATION_TS_UPDATED.equals(type))
		{
			updateTimestampAndSortConversations(object);
		}
		else if (HikePubSub.PARTICIPANT_JOINED_ONETONCONV.equals(type) || HikePubSub.PARTICIPANT_LEFT_ONETONCONV.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if(TextUtils.isEmpty(groupId))
			{
				return;
			}
			
			// This Pubsub is currently used here only to update default name
			// of a broadcast conversation.
			if(!Utils.isBroadcastConversation(groupId))
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
	}
	
	private void unreadCountModified(Message message){
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

	/*
	 * Add item for stealth ftue conv tap tip.
	 */
	protected void showStealthConvTip()
	{
		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}

		tipType = ConversationTip.STEALTH_FTUE_TIP;
		checkAndRemoveExistingHeaders();
		tipView = convTip.getView(tipType);
		if (tipView != null)
		{
			getListView().addHeaderView(tipView);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, true);
			showingStealthFtueConvTip = true;
		}
	}
	
	protected void showStealthInfoTip()
	{
		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}

		tipType = ConversationTip.STEALTH_INFO_TIP;
		checkAndRemoveExistingHeaders();
		tipView = convTip.getView(tipType);
		if (tipView != null)
		{
			getListView().addHeaderView(tipView);
		}
	}
	
	
	protected void showStealthResetConvTip()
	{
		if (convTip == null)
		{
			convTip = new ConversationTip(getActivity(), this);
		}

		tipType = ConversationTip.RESET_STEALTH_TIP;
		checkAndRemoveExistingHeaders();
		tipView = convTip.getView(tipType);
		if (tipView != null)
		{
			getListView().addHeaderView(tipView);
		}
	}

	public void checkAndRemoveExistingHeaders()
	{
		if (getListView().getHeaderViewsCount() > 0 && tipView != null)
		{
			Logger.d("ConversationFragment", "Found an existing header in listView. Removing it");
			getListView().removeHeaderView(tipView);
		}
	}
	
	protected void removeStealthConvTip()
	{
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		showingStealthFtueConvTip = false;
		if (tipView != null)
		{
			getListView().removeHeaderView(tipView);
			tipView = null;
			tipType = ConversationTip.NO_TIP;
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
		final ConvMessage newMessage = new ConvMessage("", msisdn, conversation.getLastConversationMsg() != null ? conversation.getLastConversationMsg().getTimestamp() : 0, State.RECEIVED_READ);
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

					updateViewForMessageStateChange(conversation, msg);
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

		return getListView().getChildAt(index- getListView().getFirstVisiblePosition() + getOffsetForListHeader());
	}
	
	/**
	 * Provides an offset for the correct location as we might have header views for the list view
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
			else if (displayedConversations.size() == 3)
			{
				if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
				{
					showStealthInfoTip();
				}
			}
		}
		convInfo.setLastConversationMsg(convMessage);
		Logger.d(getClass().getSimpleName(), "new message is " + convMessage);

		if (sortAndUpdateView)
		{
			sortAndUpdateTheView(convInfo, convMessage, newConversationAdded);
		}
	}

	public void movedFromEmptyToNonEmpty()
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_TO_HIKE_CARD, false))
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
			if (!wasViewSetup())
			{
				return;
			}

			View parentView = getListView().getChildAt(newIndex - getListView().getFirstVisiblePosition() + getOffsetForListHeader());

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
	 * @return
	 */
	private boolean wasViewSetup()
	{
		return getView() != null;
	}

	public void DeleteAllConversations()
	{
		if (!mAdapter.isEmpty())
		{
			Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
			HikeDialogFactory.showDialog(getActivity(), HikeDialogFactory.DELETE_ALL_CONVERSATIONS, new HikeDialogListener()
			{

				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					ConvInfo[] convs = new ConvInfo[mAdapter.getCount()];
					for (int i = 0; i < convs.length; i++)
					{
						convs[i] = mAdapter.getItem(i);
						if (OneToNConversationUtils.isOneToNConversation(convs[i].getMsisdn()))
						{
							HikeMqttManagerNew.getInstance().sendMessage(convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), HikeMqttManagerNew.MQTT_QOS_ONE);
						}
					}
					DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
					task.execute(convs);
					hikeDialog.dismiss();
				}

				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
				}

				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
				}
			}, null);
		}
	}

	@Override
	public void onResume()
	{

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
				DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
				Utils.executeConvInfoAsyncTask(task, convInfo);
			}
		}
		if(mAdapter != null)
		{
			mAdapter.getIconLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
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
			stealthFtueDone = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false);
			stealthType = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = super.getView(position, convertView, parent);
			
			if(!stealthFtueDone && stealthType == HikeConstants.STEALTH_ON && position == 0)
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
	
	protected void viewGroupInfo(ConvInfo convInfo) {
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		if (Utils.isBroadcastConversation(convInfo.getMsisdn()))
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

		mAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
	}

	private void removeTipIfExists(int whichTip)
	{
		if (tipType != whichTip)
		{
			return;
		}
		/*
		 * Remove tip always: for cases when we want to remove the tip before it is actually shown on the UI
		 */
		switch (tipType)
		{
			case ConversationTip.WELCOME_HIKE_TIP:
				showingWelcomeHikeConvTip = false;
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true);
				break;
			case ConversationTip.STEALTH_INFO_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false);
				break;
			case ConversationTip.STEALTH_UNREAD_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false);
				break;
			case ConversationTip.RESET_STEALTH_TIP:
				if (convTip != null)
				{
					convTip.resetCountDownSetter();
				}
			default:
				break;
		}
	}

	@Override
	public void onNewintent(Intent intent)
	{
		Logger.d("footer","onNewIntent");
		if (intent.getBooleanExtra(HikeConstants.Extras.HAS_TIP, false))
		{
			ShowTipIfNeeded(displayedConversations.isEmpty());
		}
		final NUXManager nm=NUXManager.getInstance();

		if (nm.getCurrentState() == NUXConstants.NUX_IS_ACTIVE
				|| (nm.getCurrentState() == NUXConstants.NUX_SKIPPED)||(nm.getCurrentState() == NUXConstants.COMPLETED))
		{
			
			if (NUXManager.getInstance().isReminderReceived())
			{
				
				switch(footerState.getEnum())
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
			if (mmStub == null)
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
			getListView().removeHeaderView(tipView);
			tipView = null;
			removeTipIfExists(whichTip);
			tipType = ConversationTip.NO_TIP;
		}
	}

}
