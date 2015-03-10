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
import android.app.Dialog;
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
import android.text.TextUtils;
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
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.ConversationTip;
import com.bsb.hike.models.EmptyConversationContactItem;
import com.bsb.hike.models.EmptyConversationFtueCardItem;
import com.bsb.hike.models.EmptyConversationItem;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.NUXChatReward;
import com.bsb.hike.models.NUXTaskDetails;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikeUser;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeFragmentable;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;

public class ConversationFragment extends SherlockListFragment implements OnItemLongClickListener, Listener, OnScrollListener, HikeFragmentable, OnClickListener
{

	private class DeleteConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
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
		protected Conversation[] doInBackground(Conversation... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<String> msisdns = new ArrayList<String>(convs.length);
			Editor editor = context.getSharedPreferences(HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).edit();
			for (Conversation conv : convs)
			{
				/*
				 * Added to check for the Conversation tip item we add for the group chat tip and other.
				 */
				if (conv instanceof ConversationTip)
				{
					continue;
				}
				else if (conv instanceof GroupConversation)
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
		protected void onPostExecute(Conversation[] deleted)
		{
			if (!isAdded())
			{
				return;
			}
			NotificationManager mgr = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			for (Conversation conversation : deleted)
			{
				/*
				 * Added to check for the Conversation tip item we add for the group chat tip and other.
				 */
				if (conversation instanceof ConversationTip)
				{
					continue;
				}
				mAdapter.remove(conversation);
				mConversationsByMSISDN.remove(conversation.getMsisdn());
				mConversationsAdded.remove(conversation.getMsisdn());

				HikeMessengerApp.removeStealthMsisdn(conversation.getMsisdn(), publishStealthEvent);
				stealthConversations.remove(conversation);
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
			HikePubSub.REMOVE_STEALTH_UNREAD_TIP, HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.GROUP_MESSAGE_DELIVERED_READ, HikePubSub.BULK_MESSAGE_DELIVERED_READ, HikePubSub.GROUP_END,
			HikePubSub.CONTACT_DELETED,HikePubSub.MULTI_MESSAGE_DB_INSERTED, HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.CONV_UNREAD_COUNT_MODIFIED};

	private ConversationsAdapter mAdapter;

	private HashMap<String, Conversation> mConversationsByMSISDN;

	private HashSet<String> mConversationsAdded;

	private Comparator<? super Conversation> mConversationsComparator;

	private View emptyView;

	private Set<Conversation> stealthConversations;

	private List<Conversation> displayedConversations;

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

		parent = inflater.inflate(R.layout.conversations, null);
		
		return parent;
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
				Intent in = IntentManager.openNuxCustomMessage(getActivity());
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
		if (wasViewSetup() && emptyView == null) /* Adding wasViewSetup() safety check for an NPE here */
		{
			ViewGroup emptyHolder = (ViewGroup) getView().findViewById(R.id.emptyViewHolder);
			emptyView = LayoutInflater.from(getActivity()).inflate(R.layout.conversation_empty_view2, emptyHolder);
			// emptyHolder.addView(emptyView);
			ListView friendsList = (ListView) getView().findViewById(android.R.id.list);
			friendsList.setEmptyView(emptyView);
		}
	}

	private void setupEmptyView()
	{

		if (emptyView == null || !isAdded())
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
		mConversationsComparator = new Conversation.ConversationComparator();
		fetchConversations();

		for (TypingNotification typingNotification : HikeMessengerApp.getTypingNotificationSet().values())
		{
			toggleTypingNotification(true, typingNotification);
		}
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		
		if (!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			// if stealth setup is not done and user has marked some chats as stealth unmark all of them
			for (Conversation conv : stealthConversations)
			{
				conv.setIsStealth(false);
				HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), false);
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
			Conversation convTip = displayedConversations.get(0);
			removeStealthConvTip(convTip);
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
		// TODO Auto-generated method stub
		super.onPause();

		if(mAdapter != null)
		{
			mAdapter.getIconLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Conversation conv = (Conversation) mAdapter.getItem(position);

		/*
		 * The item will be instance ConversationTip only for tips we show on ConversationFragment.
		 */
		if (conv instanceof ConversationTip)
		{
			switch (((ConversationTip) conv).getTipType())
			{
			case ConversationTip.STEALTH_FTUE_TIP:
				break;
			case ConversationTip.RESET_STEALTH_TIP:
				resetStealthTipClicked();
				break;
			}
			return;
		}

		Intent intent = Utils.createIntentForConversation(getSherlockActivity(), conv);
		startActivity(intent);

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (conv.getMsisdn().equals(HikeConstants.FTUE_HIKEBOT_MSISDN) && prefs.getInt(HikeConstants.HIKEBOT_CONV_STATE, 0) == hikeBotConvStat.NOTVIEWED.ordinal())
		{
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.VIEWED.ordinal());
			editor.commit();
		}
	}

	private void resetStealthTipClicked()
	{
		long remainingTime = System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l);

		if (remainingTime > HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS)
		{
			Object[] dialogStrings = new Object[4];
			dialogStrings[0] = getString(R.string.reset_complete_stealth_header);
			dialogStrings[1] = getString(R.string.reset_stealth_confirmation);
			dialogStrings[2] = getString(R.string.confirm);
			dialogStrings[3] = getString(R.string.cancel);

			HikeDialog.showDialog(getActivity(), HikeDialog.RESET_STEALTH_DIALOG, new HikeDialog.HikeDialogListener()
			{

				@Override
				public void positiveClicked(Dialog dialog)
				{
					HikeAnalyticsEvent.sendStealthReset();
					resetStealthMode();
					dialog.dismiss();
				}

				@Override
				public void neutralClicked(Dialog dialog)
				{

				}

				@Override
				public void negativeClicked(Dialog dialog)
				{
					removeTipIfExists(ConversationTip.RESET_STEALTH_TIP);

					Utils.cancelScheduledStealthReset(getActivity());

					dialog.dismiss();

					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}

				@Override
				public void onSucess(Dialog dialog)
				{
					// TODO Auto-generated method stub
					
				}
			}, dialogStrings);
		}
	}

	private void resetStealthMode()
	{
		removeTipIfExists(ConversationTip.RESET_STEALTH_TIP);

		int prevStealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		resetStealthPreferences();

		/*
		 * If previously the stealth mode was off, we should publish an event telling the friends fragment to refresh its list.
		 */
		if (prevStealthValue == HikeConstants.STEALTH_OFF)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_RESET_COMPLETE, null);
		}

		/*
		 * Calling the delete conversation task in the end to ensure that we first publish the reset event. If the delete task was published at first, it was causing a threading
		 * issue where the contacts in the friends fragment were getting removed and not added again.
		 */
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity(), false);
		task.execute(stealthConversations.toArray(new Conversation[0]));

		HikeMessengerApp.clearStealthMsisdn();
	}

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
		if (position >= mAdapter.getCount())
		{
			return false;
		}
		ArrayList<String> optionsList = new ArrayList<String>();

		final Conversation conv = (Conversation) mAdapter.getItem(position);

		if (conv instanceof ConversationTip)
		{
			return false;
		}

		/*
		 * Switch to stealth mode if we are in ftue.
		 */
		if (!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			for (int i = 0; i < mAdapter.getCount(); i++)
			{
				Conversation convTip = mAdapter.getItem(i);
				if (convTip instanceof ConversationTip && ((ConversationTip) convTip).isStealthFtueTip())
				{
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_ON);
					break;
				}
			}
		}

        if (conv.isBotConv())
        {
            conv.analyticsForBots(HikePlatformConstants.BOT_LONG_PRESS, AnalyticsConstants.LONG_PRESS_EVENT);
        }

		final int stealthType = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthType == HikeConstants.STEALTH_ON || stealthType == HikeConstants.STEALTH_ON_FAKE)
		{
			optionsList.add(getString(conv.isStealth() ? R.string.unmark_stealth : R.string.mark_stealth));
		}
		if (!(conv instanceof GroupConversation) && conv.getContactName() == null)
		{
			optionsList.add(getString(R.string.add_to_contacts));
			optionsList.add(getString(R.string.add_to_contacts_existing));
		}
		if (!(conv instanceof GroupConversation))
		{
			if (conv.getContactName() != null)
			{
				optionsList.add(getString(R.string.viewcontact));
			}
		}
		else
		{
			optionsList.add(getString(R.string.group_info));
		}
		if (conv.getContactName() != null)
		{
			optionsList.add(getString(R.string.shortcut));

		}

		if (!(conv instanceof GroupConversation) && conv.getContactName() == null)
		{
			optionsList.add(ContactManager.getInstance().isBlocked(conv.getMsisdn())?getString(R.string.unblock_title):getString(R.string.block_title));
		}
		if (conv instanceof GroupConversation)
		{
			optionsList.add(getString(R.string.delete_leave));
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
                    if (conv.isBotConv())
                    {
                        conv.analyticsForBots(HikePlatformConstants.BOT_ADD_SHORTCUT, AnalyticsConstants.CLICK_EVENT);
                    }
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.ADD_SHORTCUT);
					Utils.createShortcut(getSherlockActivity(), conv);
				}
				else if (getString(R.string.delete_chat).equals(option))
				{
					final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(getActivity());
					deleteConfirmDialog.setHeader(R.string.delete);
					deleteConfirmDialog.setBody(getString(R.string.confirm_delete_chat_msg, conv.getLabel()));
					
					View.OnClickListener dialogOkClickListener = new View.OnClickListener()
					{

						@Override
						public void onClick(View v)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
							Utils.executeConvAsyncTask(task, conv);
							deleteConfirmDialog.dismiss();
                            if (conv.isBotConv())
                            {
                                conv.analyticsForBots(HikePlatformConstants.BOT_DELETE_CHAT, AnalyticsConstants.CLICK_EVENT);
                            }
						}
					};

					deleteConfirmDialog.setOkButton(R.string.yes, dialogOkClickListener);
					deleteConfirmDialog.setCancelButton(R.string.no);
					deleteConfirmDialog.show();
				}
				else if (getString(R.string.delete_leave).equals(option))
				{
					final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(getActivity());
					deleteConfirmDialog.setHeader(R.string.delete);
					deleteConfirmDialog.setBody(getString(R.string.confirm_delete_group_msg, conv.getLabel()));
					
					View.OnClickListener dialogOkClickListener = new View.OnClickListener()
					{

						@Override
						public void onClick(View v)
						{
							Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
							deleteConversation(conv);
							deleteConfirmDialog.dismiss();
						}
					};

					deleteConfirmDialog.setOkButton(android.R.string.ok, dialogOkClickListener);
					deleteConfirmDialog.setCancelButton(R.string.cancel);
					deleteConfirmDialog.show();
				}
				else if (getString(R.string.email_conversations).equals(option))
				{
					EmailConversationsAsyncTask task = new EmailConversationsAsyncTask(getSherlockActivity(), ConversationFragment.this);
					Utils.executeConvAsyncTask(task, conv);
                    if (conv.isBotConv())
                    {
                        conv.analyticsForBots(HikePlatformConstants.BOT_EMAIL_CONVERSATION, AnalyticsConstants.CLICK_EVENT);
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
                    if (conv.isBotConv())
                    {
                        conv.analyticsForBots(HikePlatformConstants.BOT_VIEW_PROFILE, AnalyticsConstants.CLICK_EVENT);
                    }
				}
				else if (getString(R.string.clear_whole_conversation).equals(option))
				{
					clearConversation(conv);
                    if (conv.isBotConv())
                    {
                        conv.analyticsForBots(HikePlatformConstants.BOT_CLEAR_CONVERSATION,  AnalyticsConstants.CLICK_EVENT);
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

				else if (getString(R.string.group_info).equals(option))
				{
					if (!((GroupConversation) conv).getIsGroupAlive())
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
						for (int i = 0; i < mAdapter.getCount(); i++)
						{
							Conversation convTip = mAdapter.getItem(i);
							if (convTip instanceof ConversationTip && ((ConversationTip) convTip).isStealthFtueTip())
							{
								removeStealthConvTip(convTip);
								break;
							}
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

					conv.setIsStealth(newStealthValue);

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


	protected void clearConversation(final Conversation conv) {
		final CustomAlertDialog clearConfirmDialog = new CustomAlertDialog(this.getActivity());
		clearConfirmDialog.setHeader(R.string.clear_conversation);
		clearConfirmDialog.setBody(R.string.confirm_clear_conversation);
		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_CONVERSATION, conv.getMsisdn());
				clearConfirmDialog.dismiss();
			}
		};

		clearConfirmDialog.setOkButton(R.string.ok, dialogOkClickListener);
		clearConfirmDialog.setCancelButton(R.string.cancel);
		clearConfirmDialog.show();
		
	}

	private void fetchConversations()
	{
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		displayedConversations = new ArrayList<Conversation>();
		List<Conversation> conversationList = db.getConversations();

		stealthConversations = new HashSet<Conversation>();

		displayedConversations.addAll(conversationList);

		mConversationsByMSISDN = new HashMap<String, Conversation>(displayedConversations.size());
		mConversationsAdded = new HashSet<String>();

		setupConversationLists();

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		ShowTipIfNeeded(displayedConversations.isEmpty());
		
		mAdapter = new ConversationsAdapter(getActivity(), displayedConversations, getListView());

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
		if(!hasNoConversation && displayedConversations.get(0) instanceof ConversationTip){
			displayedConversations.remove(0);
		}
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String tip = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
		Logger.i("tip", "#" + tip + "#-currenttype");
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.RESET_STEALTH_TIP));
		}
		else if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, false))
		{
			showingWelcomeHikeConvTip = true;
			displayedConversations.add(0, new ConversationTip(ConversationTip.WELCOME_HIKE_TIP));
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false) &&  displayedConversations.size() > 2)
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_INFO_TIP));
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_UNREAD_TIP));
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_PROFILE_PIC))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_PROFILE_PIC_TIP));
			// show atomic pop up profile pic
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_FAVOURITES))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_FAVOURTITES_TIP));
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_INVITE))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_INVITE_TIP));
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_STATUS))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_STATUS_TIP));
		}
		else if (tip.equals(HikeMessengerApp.ATOMIC_POP_UP_INFORMATIONAL))
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_INFO_TIP));
		}else if(tip.equals(HikeMessengerApp.ATOMIC_POP_UP_HTTP)){
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_HTTP_TIP));
		}else if(tip.equals(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC)){
			displayedConversations.add(0, new ConversationTip(ConversationTip.ATOMIC_APP_GENERIC_TIP));
		}
	}

	private void setupConversationLists()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<Conversation> iter = displayedConversations.iterator(); iter.hasNext();)
		{
			Object object = iter.next();
			Conversation conv = (Conversation) object;
			if (conv instanceof ConversationTip)
			{
				continue;
			}

			mConversationsByMSISDN.put(conv.getMsisdn(), conv);
			if (conv.isStealth())
			{
				stealthConversations.add(conv);
				HikeMessengerApp.addStealthMsisdnToMap(conv.getMsisdn());
			}

			if ((stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE) && conv.isStealth())
			{
				mConversationsAdded.add(conv.getMsisdn());
				iter.remove();
			}
			else
			{
				mConversationsAdded.add(conv.getMsisdn());
			}
		}

	}

	private void changeConversationsVisibility()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE)
		{
			for (Iterator<Conversation> iter = displayedConversations.iterator(); iter.hasNext();)
			{
				Object object = iter.next();
				if (object == null)
				{
					continue;
				}
				Conversation conv = (Conversation) object;
				if (conv.isStealth())
				{
					iter.remove();
				}
			}

			if (mAdapter.getCount() == 0)
			{
				setEmptyState();
			}
		}
		else
		{
			mAdapter.addItemsToAnimat(stealthConversations);
			displayedConversations.addAll(stealthConversations);
		}

		Collections.sort(displayedConversations, mConversationsComparator);
		notifyDataSetChanged();
	}

	private void deleteConversation(Conversation conv)
	{
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
		Utils.executeConvAsyncTask(task, conv);
	}

	private void toggleTypingNotification(boolean isTyping, TypingNotification typingNotification)
	{
		if (mConversationsByMSISDN == null)
		{
			return;
		}
		String msisdn = typingNotification.getId();
		Conversation conversation = mConversationsByMSISDN.get(msisdn);
		if (conversation == null)
		{
			Logger.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}
		List<ConvMessage> messageList = conversation.getMessages();
		if (messageList.isEmpty())
		{
			Logger.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}
		ConvMessage message;

		boolean shouldUpdateUI = false;

		if (isTyping)
		{
			message = messageList.get(messageList.size() - 1);
			if (message.getTypingNotification() == null)
			{
				ConvMessage convMessage = new ConvMessage(typingNotification);
				convMessage.setTimestamp(message.getTimestamp());
				convMessage.setMessage(HikeConstants.IS_TYPING);
				convMessage.setState(State.RECEIVED_UNREAD);
				messageList.add(convMessage);

				shouldUpdateUI = true;
			}
		}
		else
		{
			message = messageList.get(messageList.size() - 1);
			if (message.getTypingNotification() != null)
			{
				messageList.remove(message);
				shouldUpdateUI = true;
			}
		}

		if (shouldUpdateUI)
		{
			if (messageList.isEmpty())
			{
				return;
			}

			/*
			 * Getting the current last message in the conversation
			 */
			ConvMessage convMessage = messageList.get(messageList.size() - 1);

			if (!wasViewSetup())
			{
				return;
			}

			View parentView = getParenViewForConversation(conversation);

			if (parentView == null || convMessage == null)
			{
				if(parentView == null)
				{
					notifyDataSetChanged();
				}
				return;
			}
			
			mAdapter.updateViewsRelatedToLastMessage(parentView, convMessage, conversation);
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
			final Conversation conv = mConversationsByMSISDN.get(msisdn);

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
				if (!conv.getMessages().isEmpty())
				{
					ConvMessage prevMessage = conv.getMessages().get(conv.getMessages().size() - 1);
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
			// For updating the group name if some participant has joined or
			// left the group
			else if ((conv instanceof GroupConversation) && message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				ContactManager conMgr = ContactManager.getInstance();
				((GroupConversation) conv).setGroupParticipantList(conMgr.getGroupParticipants(conv.getMsisdn(), false, false));
			}

			final ConvMessage finalMessage = message;

			if (conv.getMessages().size() > 0)
			{
				if (finalMessage.getMsgID() < conv.getMessages().get(conv.getMessages().size() - 1).getMsgID())
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

			final Conversation conversation = mConversationsByMSISDN.get(msisdn);

			final List<ConvMessage> messageList = new ArrayList<ConvMessage>(1);

			if (!conversationEmpty)
			{
				if (conversation == null)
				{
					return;
				}
				messageList.add(message);
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
					if (conversationEmpty)
					{
						mConversationsByMSISDN.remove(msisdn);
						mConversationsAdded.remove(msisdn);
						mAdapter.remove(conversation);
						notifyDataSetChanged();
					}
					else
					{
						conversation.setMessages(messageList);
						sortAndUpdateTheView(conversation, messageList.get(messageList.size() - 1), false);
					}
				}
			});
		}
		else if (HikePubSub.NEW_CONVERSATION.equals(type))
		{
			final Conversation conversation = (Conversation) object;
			if (HikeMessengerApp.hikeBotNamesMap.containsKey(conversation.getMsisdn()))
			{
				conversation.setContactName(HikeMessengerApp.hikeBotNamesMap.get(conversation.getMsisdn()));
			}
			Logger.d(getClass().getSimpleName(), "New Conversation. Group Conversation? " + (conversation instanceof GroupConversation));
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty() && !(conversation instanceof GroupConversation))
			{
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());

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
							displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_INFO_TIP));
						}
					}
					displayedConversations.add(conversation);
					Collections.sort(displayedConversations, mConversationsComparator);
					notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.MSG_READ.equals(type))
		{
			String msisdn = (String) object;
			final Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null)
			{
				/*
				 * We don't really need to do anything if the conversation does not exist.
				 */
				return;
			}
			conv.setUnreadCount(0);
			HikeConversationsDatabase.getInstance().setExtraConvUnreadCount(msisdn, 0);
			/*
			 * look for the latest received messages and set them to read. Exit when we've found some read messages
			 */
			final List<ConvMessage> messages = conv.getMessages();
			for (int i = messages.size() - 1; i >= 0; --i)
			{
				ConvMessage msg = messages.get(i);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.RECEIVED_READ.ordinal()))
				{
					ConvMessage.State currentState = msg.getState();
					if (currentState == ConvMessage.State.RECEIVED_READ)
					{
						break;
					}

					msg.setState(ConvMessage.State.RECEIVED_READ);
				}
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
					if (messages.isEmpty())
					{
						return;
					}

					ConvMessage lastConvMessage = messages.get(messages.size() - 1);
					updateViewForMessageStateChange(conv, lastConvMessage);
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
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type) || HikePubSub.GROUP_MESSAGE_DELIVERED_READ.equals(type))
		{
			String sender = null;
			long[] ids;
			if (HikePubSub.GROUP_MESSAGE_DELIVERED_READ.equals(type))
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
					Conversation conversation = mConversationsByMSISDN.get(msisdn);
					
					updateViewForMessageStateChange(conversation, message);
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
						Conversation conversation = mConversationsByMSISDN.get(msisdn);
						
						updateViewForMessageStateChange(conversation, msg);
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
					Conversation conversation = mConversationsByMSISDN.get(msisdn);

					if (!wasViewSetup() || null == conversation)
					{
						return;
					}

					View parentView = getParenViewForConversation(conversation);

					if (parentView == null)
					{
						notifyDataSetChanged();
						return;
					}

					mAdapter.updateViewsRelatedToAvatar(parentView, conversation);
				}
			});
		}
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			final String groupName = ContactManager.getInstance().getName(groupId);

			final Conversation conv = mConversationsByMSISDN.get(groupId);
			if (conv == null)
			{
				return;
			}
			conv.setContactName(groupName);

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				
				@Override
				public void run()
				{
					updateViewForNameChange(conv);
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

			final Conversation conversation = this.mConversationsByMSISDN.get(contactInfo.getMsisdn());
			if (conversation != null)
			{
				if(HikePubSub.CONTACT_DELETED.equals(type))
					conversation.setContactName(contactInfo.getMsisdn());
				else
					conversation.setContactName(contactInfo.getName());

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
						updateViewForNameChange(conversation);
						if(HikePubSub.CONTACT_DELETED.equals(mType))
							updateViewForAvatarChange(conversation);
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
			final Conversation conversation = mConversationsByMSISDN.get(groupId);
			if (conversation == null)
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
					deleteConversation(conversation);
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
			if (mAdapter == null || mAdapter.isEmpty())
			{
				return;
			}
			int position = (Integer) object;
			final Conversation conversation = mAdapter.getItem(position);
			if (!(conversation instanceof ConversationTip && ((ConversationTip) conversation).isStealthFtueTip()))
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
					removeStealthConvTip(conversation);
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
					for (Conversation conv : stealthConversations)
					{
						conv.setIsStealth(false);
						HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), false);
						HikeMessengerApp.removeStealthMsisdn(conv.getMsisdn());
					}
					displayedConversations.addAll(stealthConversations);
					stealthConversations.clear();
					Collections.sort(displayedConversations, mConversationsComparator);
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

					displayedConversations.add(0, new ConversationTip(ConversationTip.RESET_STEALTH_TIP));

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
						final Conversation conv = mConversationsByMSISDN.get(msisdn);
						if (conv != null)
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
								conv.setUnreadCount(conv.getUnreadCount() + unreadCount);
							}
							ConvMessage message = messageList.get(messageList.size() - 1);
							if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
							{
								if (!conv.getMessages().isEmpty())
								{
									ConvMessage prevMessage = conv.getMessages().get(conv.getMessages().size() - 1);
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
							// For updating the group name if some participant has joined or
							// left the group
							else if ((conv instanceof GroupConversation) && message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
							{
								ContactManager conMgr = ContactManager.getInstance();
								((GroupConversation) conv).setGroupParticipantList(conMgr.getGroupParticipants(conv.getMsisdn(), false, false));
							}

							final ConvMessage finalMessage = message;
//							if (conv.getMessages().size() > 0)
//							{
//								if (finalMessage.getMsgID() < conv.getMessages().get(conv.getMessages().size() - 1).getMsgID())
//								{
//									return;
//								}
//							}
							if (!isAdded())
							{
								return;
							}
							getActivity().runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									addMessage(conv,finalMessage,false);
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
						Collections.sort(displayedConversations, mConversationsComparator);
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
				final Conversation conv = mConversationsByMSISDN.get(groupId);
				if (conv == null)
				{
					return;
				}
				((GroupConversation) conv).setGroupAlive(false);
			}
		}else if(HikePubSub.MULTI_MESSAGE_DB_INSERTED.equals(type)){
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
				final Conversation conv = mConversationsByMSISDN.get(msisdn);
				// possible few conversation does not exist ,as we can forward to any contact
				if (conv == null)
				{
					continue;
				}

				ConvMessage message = contactMessagePair.second;

				if (conv.getMessages().size() > 0) {
					if (message.getMsgID() < conv.getMessages()
							.get(conv.getMessages().size() - 1).getMsgID()) {
						continue;
					}
				}

				// for multi messages , if conversation exists then only we need
				// to update messages . No new conversation will be created
					conv.addMessage(message);
				
			}
			// messages added , update UI
			getActivity().runOnUiThread(new Runnable()
			{
				public void run() {
					Collections.sort(displayedConversations, mConversationsComparator);
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

			final Conversation conversation = mConversationsByMSISDN.get(groupId);
			
			if (conversation == null)
			{
				return ;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					View parentView = getParenViewForConversation(conversation);
					if (parentView == null)
					{
						notifyDataSetChanged();
						return;
					}

					// Updating data set for Conversation ListView
					if (conversation instanceof GroupConversation)
					{
						((GroupConversation) conversation).setIsMuted(isMuted);
					}
					else if (conversation.isBotConv())
					{
						conversation.isMutedBotConv(true);
					}

					notifyDataSetChanged();
				}
			});
		}
	}
	
	private void unreadCountModified(Message message){
		String msisdn = (String) message.obj;
		final Conversation conv = mConversationsByMSISDN.get(msisdn);
		if (conv == null)
		{
			return;
		}
		conv.setUnreadCount(message.arg1);
		getActivity().runOnUiThread(new Runnable()
		{
			
			@Override
			public void run()
			{
				notifyDataSetChanged();
			}
		});
	}

	private Conversation getFirstConversation()
	{
		Conversation conv = null;
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
		displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_FTUE_TIP));
		notifyDataSetChanged();
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, true);
		showingStealthFtueConvTip = true;
	}

	protected void removeStealthConvTip(Conversation conversation)
	{
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		showingStealthFtueConvTip = false;
		mAdapter.remove(conversation);
		notifyDataSetChanged();
	}

	private void clearConversation(String msisdn)
	{
		final Conversation conversation = mConversationsByMSISDN.get(msisdn);

		if (conversation == null)
		{
			return;
		}

		/*
		 * Clearing all current messages.
		 */
		List<ConvMessage> messages = conversation.getMessages();

		ConvMessage convMessage = null;
		if (!messages.isEmpty())
		{
			convMessage = messages.get(0);
		}

		messages.clear();
		conversation.setUnreadCount(0);
		/*
		 * Adding a blank message
		 */
		final ConvMessage newMessage = new ConvMessage("", msisdn, convMessage != null ? convMessage.getTimestamp() : 0, State.RECEIVED_READ);
		messages.add(newMessage);

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
		for (Entry<String, Conversation> conversationEntry : mConversationsByMSISDN.entrySet())
		{
			Conversation conversation = conversationEntry.getValue();
			if (conversation == null)
			{
				continue;
			}
			List<ConvMessage> messages = conversation.getMessages();
			if (messages.isEmpty())
			{
				continue;
			}

			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getMsgID() == msgId)
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
					Conversation conversation = mConversationsByMSISDN.get(msg.getMsisdn());

					updateViewForMessageStateChange(conversation, msg);
				}
			});
		}
	}

	private View getParenViewForConversation(Conversation conversation)
	{
		int index = displayedConversations.indexOf(conversation);

		if (index == -1)
		{
			return null;
		}

		return getListView().getChildAt(index- getListView().getFirstVisiblePosition());
	}

	private void updateViewForNameChange(Conversation conversation)
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

		mAdapter.updateViewsRelatedToName(parentView, conversation);
	}
	
	private void updateViewForAvatarChange(Conversation conversation)
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

		mAdapter.updateViewsRelatedToAvatar(parentView, conversation);
	}

	private void updateViewForMessageStateChange(Conversation conversation, ConvMessage convMessage)
	{
		if (!wasViewSetup() || null == conversation)
		{
			Logger.d("UnreadBug", "Unread count event received but view wasn't setup");
			return;
		}

		View parentView = getParenViewForConversation(conversation);

		if (parentView == null)
		{
			Logger.d("UnreadBug", "Unread count event received but parent view was null");
			notifyDataSetChanged();
			return;
		}

		mAdapter.updateViewsRelatedToMessageState(parentView, convMessage, conversation);
	}

	private void addMessage(Conversation conv, ConvMessage convMessage, boolean sortAndUpdateView)
	{
		boolean newConversationAdded = false;

		if (!mConversationsAdded.contains(conv.getMsisdn()))
		{
			mConversationsAdded.add(conv.getMsisdn());
			displayedConversations.add(conv);

			newConversationAdded = true;
			
			if (displayedConversations.size() == 1)
			{
				movedFromEmptyToNonEmpty();
			}
			else if (displayedConversations.size() == 3)
			{
				if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
				{
					displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_INFO_TIP));
				}
			}
		}
		conv.clearMessageListAndAddMessage(convMessage);
		Logger.d(getClass().getSimpleName(), "new message is " + convMessage);

		if (sortAndUpdateView)
		{
			sortAndUpdateTheView(conv, convMessage, newConversationAdded);
		}
	}

	public void movedFromEmptyToNonEmpty()
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_TO_HIKE_CARD, false))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_WELCOME_TO_HIKE_CARD, true);
		}
	}

	private void sortAndUpdateTheView(Conversation conversation, ConvMessage convMessage, boolean newConversationAdded)
	{
		int prevIndex = displayedConversations.indexOf(conversation);

		Collections.sort(displayedConversations, mConversationsComparator);

		int newIndex = displayedConversations.indexOf(conversation);

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

			View parentView = getListView().getChildAt(newIndex - getListView().getFirstVisiblePosition());

			if (parentView == null)
			{
				notifyDataSetChanged();
				return;
			}

			mAdapter.updateViewsRelatedToLastMessage(parentView, convMessage, conversation);
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
			final CustomAlertDialog deleteDialog = new CustomAlertDialog(getActivity());
			deleteDialog.setHeader(R.string.deleteconversations);
			deleteDialog.setBody(R.string.delete_all_question);
			OnClickListener deleteAllOkClickListener = new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Conversation[] convs = new Conversation[mAdapter.getCount()];
					for (int i = 0; i < convs.length; i++)
					{
						convs[i] = mAdapter.getItem(i);
						if ((convs[i] instanceof GroupConversation))
						{
							HikeMqttManagerNew.getInstance().sendMessage(convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), HikeMqttManagerNew.MQTT_QOS_ONE);
						}
					}
					DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
					task.execute(convs);
					deleteDialog.dismiss();
				}
			};

			deleteDialog.setOkButton(R.string.delete, deleteAllOkClickListener);
			deleteDialog.setCancelButton(R.string.cancel);

			deleteDialog.show();
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
			Conversation conv = null;
			conv = mConversationsByMSISDN.get(HikeConstants.FTUE_HIKEBOT_MSISDN);
			if (conv != null)
			{
				Editor editor = prefs.edit();
				editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.DELETED.ordinal());
				editor.commit();
				Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
				DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
				Utils.executeConvAsyncTask(task, conv);
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
		for(Conversation conv : displayedConversations)
		{
			if(conv instanceof ConversationTip)
			{
				/*
				 * we should ideally remove this tip here.
				 */
				continue;
			}
			else
			{
				return false;
			}
		}
		
		return true;
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
	
	protected void viewContacts(Conversation conv) 
	{
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.CONTACT_INFO, conv.getMsisdn());
		intent.putExtra(HikeConstants.Extras.ON_HIKE, conv.isOnhike());
		startActivity(intent);
	}
	protected void viewGroupInfo(Conversation conv) {
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, conv.getMsisdn());
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

	private void removeTipIfExists(int tipType)
	{
		if (mAdapter.isEmpty())
		{
			return;
		}

		Conversation conversation = mAdapter.getItem(0);

		/*
		 * Remove tip if already showing as the first element on the UI
		 */
		if(conversation instanceof ConversationTip && ((ConversationTip) conversation).getTipType() == tipType)
		{
			mAdapter.remove(conversation);
			switch (tipType)
			{
				case ConversationTip.RESET_STEALTH_TIP:
					mAdapter.resetCountDownSetter();
					break;
				case ConversationTip.WELCOME_HIKE_TIP:
					showingWelcomeHikeConvTip = false;
					break;
				default:
					break;
			}
			notifyDataSetChanged();
		}

		/*
		 * Remove tip always: for cases when we want to remove the tip before it is actually shown on the UI
		 */
		switch (tipType)
		{
			case ConversationTip.WELCOME_HIKE_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true);
				break;
			case ConversationTip.STEALTH_INFO_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false);
				break;
			case ConversationTip.STEALTH_UNREAD_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false);
				break;
			default:
				break;
		}

		if (mAdapter.getCount() == 0)
		{
			setEmptyState();
		}
	}

	@Override
	public void onNewintent(Intent intent)
	{
		Logger.d("footer","onNewIntent");
		if (intent.getBooleanExtra(HikeConstants.Extras.HAS_TIP, false))
		{
			ShowTipIfNeeded(displayedConversations.isEmpty());
			notifyDataSetChanged();
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

}
