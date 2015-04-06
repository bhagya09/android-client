package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.ConversationTip;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.GroupUtils;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends BaseAdapter
{

	private IconLoader iconLoader;

	private int mIconImageSize;

	private CountDownSetter countDownSetter;
	
	private SparseBooleanArray itemsToAnimat;
	
	private boolean stealthFtueTipAnimated = false;
	
	private boolean resetStealthTipAnimated = false;

	private List<Conversation> conversationList;

	private List<Conversation> phoneBookContacts;

	private List<Conversation> completeList;

	private Set<Conversation> stealthConversations;

	private Map<String, Integer> convSpanStartIndexes;

	private String refinedSearchText;

	private Context context;

	private ListView listView;
	
	private LayoutInflater inflater;

	private ContactFilter contactFilter;

	private Set<String> conversationsMsisdns;

	private boolean isSearchModeOn = false;

	private enum ViewType
	{
		CONVERSATION, STEALTH_FTUE_TIP_VIEW, RESET_STEALTH_TIP, WELCOME_HIKE_TIP, STEALTH_INFO_TIP, STEALTH_UNREAD_TIP, ATOMIC_PROFILE_PIC_TIP, ATOMIC_FAVOURITE_TIP, ATOMIC_INVITE_TIP, ATOMIC_STATUS_TIP, ATOMIC_INFO_TIP,ATOMIC_HTTP_TIP,ATOMIC_APP_GENERIC_TIP
	}

	private class ViewHolder
	{
		String msisdn;

		TextView headerText;

		View closeTip;

		TextView subText;

		ImageView imageStatus;

		TextView unreadIndicator;

		TextView timeStamp;

		ImageView avatar;
		
		View parent;
		
		ImageView muteIcon;
	}

	public ConversationsAdapter(Context context, List<Conversation> displayedConversations, Set<Conversation> stealthConversations, ListView listView)
	{
		this.context = context;
		this.completeList = displayedConversations;
		this.stealthConversations = stealthConversations;
		this.listView = listView;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setImageFadeIn(false);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		itemsToAnimat = new SparseBooleanArray();
		contactFilter = new ContactFilter();
		conversationList = new ArrayList<Conversation>();
		convSpanStartIndexes = new HashMap<String, Integer>();
	}

	@Override
	public int getCount()
	{
		return completeList.size();
	}

	@Override
	public Conversation getItem(int position)
	{
		return completeList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	public void clear()
	{
		completeList.clear();
	}

	@Override
	public int getItemViewType(int position)
	{
		Conversation conversation = getItem(position);
		if (conversation instanceof ConversationTip)
		{
			switch (((ConversationTip) conversation).getTipType())
			{
			case ConversationTip.STEALTH_FTUE_TIP:
				return ViewType.STEALTH_FTUE_TIP_VIEW.ordinal();
			case ConversationTip.RESET_STEALTH_TIP:
				return ViewType.RESET_STEALTH_TIP.ordinal();
			case ConversationTip.WELCOME_HIKE_TIP:
				return ViewType.WELCOME_HIKE_TIP.ordinal();
			case ConversationTip.STEALTH_INFO_TIP:
				return ViewType.STEALTH_INFO_TIP.ordinal();
			case ConversationTip.STEALTH_UNREAD_TIP:
				return ViewType.STEALTH_UNREAD_TIP.ordinal();
			case ConversationTip.ATOMIC_PROFILE_PIC_TIP:
				return ViewType.ATOMIC_PROFILE_PIC_TIP.ordinal();
			case ConversationTip.ATOMIC_FAVOURTITES_TIP:
				return ViewType.ATOMIC_FAVOURITE_TIP.ordinal();
			case ConversationTip.ATOMIC_INVITE_TIP:
				return ViewType.ATOMIC_INVITE_TIP.ordinal();
			case ConversationTip.ATOMIC_STATUS_TIP:
				return ViewType.ATOMIC_STATUS_TIP.ordinal();
			case ConversationTip.ATOMIC_INFO_TIP:
				return ViewType.ATOMIC_INFO_TIP.ordinal();
			case ConversationTip.ATOMIC_HTTP_TIP:
				return ViewType.ATOMIC_HTTP_TIP.ordinal();
			case ConversationTip.ATOMIC_APP_GENERIC_TIP:
				return ViewType.ATOMIC_APP_GENERIC_TIP.ordinal();
			}
		}
		return ViewType.CONVERSATION.ordinal();
	}

	public List<Conversation> getCompleteList()
	{
		return completeList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final Conversation conversation = getItem(position);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		View v = convertView;
		ViewHolder viewHolder = null;

		if (v == null)
		{
			viewHolder = new ViewHolder();
			switch (viewType)
			{
			case CONVERSATION:
				v = inflater.inflate(R.layout.conversation_item, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.contact);
				viewHolder.imageStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
				viewHolder.unreadIndicator = (TextView) v.findViewById(R.id.unread_indicator);
				viewHolder.subText = (TextView) v.findViewById(R.id.last_message);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.last_message_timestamp);
				viewHolder.avatar = (ImageView) v.findViewById(R.id.avatar);
				viewHolder.muteIcon = (ImageView) v.findViewById(R.id.mute_indicator);
				break;
			case STEALTH_FTUE_TIP_VIEW:
			case RESET_STEALTH_TIP:
				v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.tip);
				viewHolder.closeTip = v.findViewById(R.id.close);
				break;
			case WELCOME_HIKE_TIP:
				v = inflater.inflate(R.layout.welcome_hike_tip, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.tip_header);
				viewHolder.subText = (TextView) v.findViewById(R.id.tip_msg);
				viewHolder.closeTip = v.findViewById(R.id.close_tip);
				break;
			case STEALTH_INFO_TIP:
			case STEALTH_UNREAD_TIP:
				v = inflater.inflate(R.layout.stealth_unread_tip, parent, false);
				viewHolder.headerText = (TextView) v.findViewById(R.id.tip_header);
				viewHolder.subText = (TextView) v.findViewById(R.id.tip_msg);
				viewHolder.closeTip = v.findViewById(R.id.close_tip);
				viewHolder.parent = v.findViewById(R.id.all_content);
				break;
			case ATOMIC_PROFILE_PIC_TIP:
			case ATOMIC_FAVOURITE_TIP:
			case ATOMIC_INVITE_TIP:
			case ATOMIC_STATUS_TIP:
			case ATOMIC_INFO_TIP:
			case ATOMIC_HTTP_TIP:
			case ATOMIC_APP_GENERIC_TIP:
				v = inflater.inflate(R.layout.tip_left_arrow, parent, false);
				viewHolder.avatar = (ImageView) v.findViewById(R.id.arrow_pointer);
				viewHolder.headerText = (TextView) v.findViewById(R.id.tip_header);
				viewHolder.subText = (TextView) v.findViewById(R.id.tip_msg);
				viewHolder.closeTip = v.findViewById(R.id.close_tip);
				viewHolder.parent = v.findViewById(R.id.all_content);
				break;
			default:
				break;
			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		if (viewType == ViewType.STEALTH_FTUE_TIP_VIEW)
		{
			final int pos = position;
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					stealthFtueTipAnimated = false;
					HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP, pos);
				}
			});
			if(!stealthFtueTipAnimated)
			{
				stealthFtueTipAnimated = true;
				final TranslateAnimation animation = new TranslateAnimation(0, 0, -70*Utils.scaledDensityMultiplier, 0);
				animation.setDuration(300);
				parent.startAnimation(animation);
			}
			return v;
		}
		else if (viewType == ViewType.RESET_STEALTH_TIP)
		{
			long remainingTime = HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS
					- (System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l));

			if (remainingTime <= 0)
			{
				viewHolder.headerText.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
			}
			else
			{
				if (countDownSetter == null)
				{
					countDownSetter = new CountDownSetter(viewHolder.headerText, remainingTime, 1000);
					countDownSetter.start();

					setTimeRemainingText(viewHolder.headerText, remainingTime);
				}
				else
				{
					countDownSetter.setTextView(viewHolder.headerText);
				}
			}

			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					resetStealthTipAnimated = false;
					resetCountDownSetter();

					remove(conversation);
					notifyDataSetChanged();

					Utils.cancelScheduledStealthReset(context);
					
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
			});
			
			if(!resetStealthTipAnimated)
			{
				resetStealthTipAnimated = true;
				final TranslateAnimation animation = new TranslateAnimation(0, 0, -70*Utils.scaledDensityMultiplier, 0);
				animation.setDuration(300);
				parent.startAnimation(animation);
			}
			
			return v;
		}
		else if (viewType == ViewType.WELCOME_HIKE_TIP)
		{
			viewHolder.headerText.setText(R.string.new_ui_welcome_tip_header);
			viewHolder.subText.setText(R.string.new_ui_welcome_tip_msg);
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_WELCOME_HIKE_TIP, null);
				}
			});
			return v;
		}
		else if (viewType == ViewType.STEALTH_INFO_TIP)
		{
			viewHolder.headerText.setText(R.string.stealth_info_tip_header);
			viewHolder.subText.setText(R.string.stealth_info_tip_subtext);
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_INFO_TIP, null);
				}
			});
			return v;
		}
		else if (viewType == ViewType.STEALTH_UNREAD_TIP)
		{
			String headerTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_HEADER, "");
			String msgTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_MESSAGE, "");
			viewHolder.headerText.setText(headerTxt);
			viewHolder.subText.setText(msgTxt);
			viewHolder.parent.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_UNREAD_TIP_CLICKED, null);
				}
			});
			
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View view)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_UNREAD_TIP, null);
				}
			});
			return v;
		}
		else if (viewType == ViewType.ATOMIC_PROFILE_PIC_TIP || viewType == ViewType.ATOMIC_FAVOURITE_TIP || viewType == ViewType.ATOMIC_INVITE_TIP
				|| viewType == ViewType.ATOMIC_STATUS_TIP || viewType == ViewType.ATOMIC_INFO_TIP || viewType == ViewType.ATOMIC_HTTP_TIP || viewType == ViewType.ATOMIC_APP_GENERIC_TIP)
		{
			HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
			String headerTxt = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, "");
			String message = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_MAIN, "");
			viewHolder.headerText.setText(headerTxt);
			viewHolder.subText.setText(message);
			viewHolder.closeTip.setTag(position);
			boolean clickParentEnabled = true;
			if (viewType == ViewType.ATOMIC_PROFILE_PIC_TIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_profile);
			}
			else if (viewType == ViewType.ATOMIC_FAVOURITE_TIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_favorites);
			}
			else if (viewType == ViewType.ATOMIC_INVITE_TIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_rewards);
			}
			else if (viewType == ViewType.ATOMIC_STATUS_TIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_status_tip);
			}
			else if (viewType == ViewType.ATOMIC_INFO_TIP)
			{
				clickParentEnabled = false;
				viewHolder.avatar.setImageResource(R.drawable.ic_information);
			}else if(viewType == ViewType.ATOMIC_HTTP_TIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_information);
			}else if(viewType == ViewType.ATOMIC_APP_GENERIC_TIP){
				viewHolder.avatar.setImageDrawable(null);
			}
			viewHolder.closeTip.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Logger.i("tip", "on cross click ");
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
					// make sure it is on 0 position
					completeList.remove((int) ((Integer) v.getTag()));
					notifyDataSetChanged();
				}
			});
			if (clickParentEnabled)
			{
				viewHolder.parent.setTag(position);
				viewHolder.parent.setOnClickListener(new OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						Integer position = (Integer) v.getTag();
						resetAtomicPopUpKey(position);
					}
				});
			}
			return v;
		}

		viewHolder.msisdn = conversation.getMsisdn();

		updateViewsRelatedToName(v, conversation);

		if (itemToBeAnimated(conversation))
		{
			final Animation animation = AnimationUtils.loadAnimation(context,
		            R.anim.slide_in_from_left);
			v.startAnimation(animation);
			setItemAnimated(conversation);
		}

		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);
			updateViewsRelatedToLastMessage(v, message, conversation);
		}

		updateViewsRelatedToAvatar(v, conversation);

		updateViewsRelatedToMute(v, conversation);
		
		return v;
	}

	/**
	 * Activates search mode in the adapter.
	 * Setups contact msisdn lists. Launches task to fetch the contact list.
	 */
	public void setupSearch()
	{
		isSearchModeOn = true;
		// conversationList will contain all the conversations to be used in search mode 
		conversationList.clear();
		// conversationsMsisdns will contain the conversations so that they are not added again when getting contacts list 
		conversationsMsisdns = new HashSet<String>();
		for(Conversation conv : completeList)
		{
			if (!(conv instanceof ConversationTip))
			{
				conversationList.add(conv);
				conversationsMsisdns.add(conv.getMsisdn());
			}
		}
		FetchPhoneBookContactsTask fetchContactsTask = new FetchPhoneBookContactsTask();
		Utils.executeAsyncTask(fetchContactsTask);
	}

	/**
	 * Deactivates search mode in the adapter.
	 * Clears up the contact msisdn lists. Launches task to fetch the contact list.
	 */
	public void removeSearch()
	{
		isSearchModeOn = false;
		convSpanStartIndexes.clear();
		refinedSearchText = null;
		/*
		 * Purposely returning conversation list on the UI thread on collapse to avoid showing ftue empty state. 
		 */
		completeList.clear();
		completeList.addAll(conversationList);
		notifyDataSetChanged();
	}

	private class FetchPhoneBookContactsTask extends AsyncTask<Void, Void, Void>
	{
		List<Conversation> hikeContacts = new ArrayList<Conversation>();

		@Override
		protected Void doInBackground(Void... arg0)
		{
			List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
			for(ContactInfo contact : allContacts)
			{
				Conversation conv = new Conversation(contact.getMsisdn(), contact.getName(), contact.isOnhike());
				if(stealthConversations.contains(conv) || conversationsMsisdns.contains(contact.getMsisdn()))
				{
					continue;
				}
				String msg= null;
				if (contact.isOnhike())
				{
					msg = context.getString(R.string.start_new_chat);
				}
				else
				{
					msg = context.getString(R.string.on_sms);
				}
				List<ConvMessage> messagesList = new ArrayList<ConvMessage>();
				ConvMessage message = new ConvMessage(msg, contact.getMsisdn(), -1, State.RECEIVED_READ);
				messagesList.add(message);
				conv.setMessages(messagesList);
				if (contact.isOnhike())
				{
					hikeContacts.add(conv);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			phoneBookContacts = new ArrayList<Conversation>();
			phoneBookContacts.addAll(hikeContacts);
			super.onPostExecute(result);
		}
	}

	public void onQueryChanged(String s)
	{
		refinedSearchText = s.toLowerCase();
		contactFilter.filter(refinedSearchText);
	}

	private class ContactFilter extends Filter
	{
		private boolean noResultRecorded = false;

		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults results = new FilterResults();
			convSpanStartIndexes.clear();

			String textToBeFiltered = constraint.toString();
			if (!TextUtils.isEmpty(textToBeFiltered))
			{
				List<Conversation> filteredConversationsList = new ArrayList<Conversation>();
				List<Conversation> filteredphoneBookContacts = new ArrayList<Conversation>();

				if (conversationList != null && !conversationList.isEmpty())
				{
					filterList(conversationList, filteredConversationsList, textToBeFiltered);
				}
				if (phoneBookContacts != null && !phoneBookContacts.isEmpty())
				{
					filterList(phoneBookContacts, filteredphoneBookContacts, textToBeFiltered);
				}

				List<List<Conversation>> resultList = new ArrayList<List<Conversation>>();
				resultList.add(filteredConversationsList);
				resultList.add(filteredphoneBookContacts);

				results.values = resultList;
			}
			else
			{
				List<List<Conversation>> resultList = new ArrayList<List<Conversation>>();
				resultList.add(getOriginalList());
				results.values = resultList;
			}
			results.count = 1;
			return results;
		}

		private void filterList(List<Conversation> allList, List<Conversation> listToUpdate, String textToBeFiltered)
		{

			for (Conversation info : allList)
			{
				try
				{
					boolean found = false;
					if (textToBeFiltered.equals("broadcast") && Utils.isBroadcastConversation(info.getMsisdn()))
					{
						found = true;
					}
					else if (textToBeFiltered.equals("group") && Utils.isGroupConversation(info.getMsisdn()) && !Utils.isBroadcastConversation(info.getMsisdn()))
					{
						found = true;
					}
					else
					{
						String name = info.getLabel().toLowerCase();
						int startIndex = 0;
						if (name.startsWith(textToBeFiltered))
						{
							found = true;
							convSpanStartIndexes.put(info.getMsisdn(), startIndex);
						}
						else if (name.contains(" " + textToBeFiltered))
						{
							found = true;
							startIndex = name.indexOf(" " + textToBeFiltered) + 1;
							convSpanStartIndexes.put(info.getMsisdn(), startIndex);
						}
					}

					if(found)
					{
						listToUpdate.add(info);
					}
				}
				catch (Exception ex)
				{
					Logger.d(getClass().getSimpleName(), "Exception while filtering conversation contacts." + ex);
				}
			}
			
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			List<List<Conversation>> resultList = (List<List<Conversation>>) results.values;

			List<Conversation> filteredSearchList = new ArrayList<Conversation>();
			filteredSearchList.addAll(resultList.get(0));

			if(phoneBookContacts!=null && !phoneBookContacts.isEmpty() && resultList.size() > 1)
			{
				filteredSearchList.addAll(resultList.get(1));
			}

			completeList.clear();
			completeList.addAll(filteredSearchList);
			notifyDataSetChanged();
			if (completeList.isEmpty() && !noResultRecorded)
			{
				recordNoResultsSearch();
				noResultRecorded = true;
			}
			else if (!completeList.isEmpty())
			{
				noResultRecorded = false;
			}
		}
	}
	
	private void recordNoResultsSearch()
	{
		String SEARCH_NO_RESULT = "srchNoRslt";
		String SEARCH_TEXT = "srchTxt";
		
		JSONObject metadata = new JSONObject();
		try
		{
			metadata
			.put(HikeConstants.EVENT_KEY, SEARCH_NO_RESULT)
			.put(SEARCH_TEXT, refinedSearchText);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_HOME_SEARCH, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	protected List<Conversation> getOriginalList()
	{
		return conversationList;
	}

	private void resetAtomicPopUpKey(int position)
	{
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		Conversation con = conversationList.get(position);
		JSONObject metadata = new JSONObject();		
		
		if (con instanceof ConversationTip)
		{
			try
			{
				ConversationTip tip = (ConversationTip) con;
				switch (tip.getTipType())
				{
				case ConversationTip.ATOMIC_FAVOURTITES_TIP:
					context.startActivity(new Intent(context, PeopleActivity.class));
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_FAVOURITES_TIP_CLICKED);				
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					break;
				case ConversationTip.ATOMIC_INVITE_TIP:
					context.startActivity(new Intent(context, TellAFriend.class));
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_INVITE_TIP_CLICKED);				
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					break;
				case ConversationTip.ATOMIC_PROFILE_PIC_TIP:
					context.startActivity(new Intent(context, ProfileActivity.class));
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_PROFILE_PIC_TIP_CLICKED);				
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					break;
				case ConversationTip.ATOMIC_STATUS_TIP:
					context.startActivity(new Intent(context, StatusUpdate.class));
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_STATUS_TIP_CLICKED);				
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					break;
				case ConversationTip.ATOMIC_HTTP_TIP:
					String url = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, null);
					if(!TextUtils.isEmpty(url)){
					Utils.startWebViewActivity(context, url, pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, ""));
					pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, "");
					}
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_HTTP_TIP_CLICKED);				
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					break;
				case ConversationTip.ATOMIC_APP_GENERIC_TIP:
					onClickGenericAppTip(pref);
					break;
				}			
				conversationList.remove(position);
				notifyDataSetChanged();
				pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}

	}
	
	private void onClickGenericAppTip(HikeSharedPreferenceUtil pref){
		int what = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT, -1);
		
		try
		{
			JSONObject metadata = new JSONObject();
	
			switch(what){
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS:
				IntentManager.openSetting(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_NOTIF:
				IntentManager.openSettingNotification(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_NOTIF_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_PRIVACY:
				IntentManager.openSettingPrivacy(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_PRIVACY_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_SMS:
				IntentManager.openSettingSMS(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_SMS_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_MEDIA:
				IntentManager.openSettingMedia(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_MEDIA_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_INVITE_FREE_SMS:
				IntentManager.openInviteSMS(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_INVITE_FREE_SMS_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_INVITE_WATSAPP:
				if(Utils.isPackageInstalled(context, HikeConstants.PACKAGE_WATSAPP)){
					IntentManager.openInviteWatsApp(context);
				}
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_INVITE_WHATSAPP_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_TIMELINE:
				IntentManager.openTimeLine(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_TIMELINE_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_HIKE_EXTRA:
				IntentManager.openHikeExtras(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_HIKE_EXTRA_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_HIKE_REWARDS:
				IntentManager.openHikeRewards(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_HIKE_REWARDS_CLICKED);				
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			default:
				return;
			}
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	public void updateViewsRelatedToName(View parentView, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		TextView contactView = viewHolder.headerText;
		String name = conversation.getLabel();
		Integer startSpanIndex = convSpanStartIndexes.get(conversation.getMsisdn());
		if(isSearchModeOn && startSpanIndex!=null)
		{
			SpannableString spanName = new SpannableString(name);
			int start = startSpanIndex;
			int end = startSpanIndex + refinedSearchText.length();
			spanName.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blue_color_span)), start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			contactView.setText(spanName, TextView.BufferType.SPANNABLE);
		}
		else
		{
			contactView.setText(name);
		}

		if (conversation instanceof GroupConversation)
		{
			if (conversation instanceof BroadcastConversation)
			{
				contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			else
			{
				contactView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_group, 0, 0, 0);
			}
		}
		else
		{
			contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
	}

	public void updateViewsRelatedToAvatar(View parentView, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		ImageView avatarView = viewHolder.avatar;
		iconLoader.loadImage(conversation.getMsisdn(), avatarView, isListFlinging, false, true);
	}

	public void updateViewsRelatedToMute(View parentView, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		ImageView muteIcon = viewHolder.muteIcon;
		if (conversation instanceof GroupConversation && muteIcon != null)
		{
			if (((GroupConversation) conversation).isMuted())
			{
				muteIcon.setVisibility(View.VISIBLE);
			}
			else
			{
				muteIcon.setVisibility(View.GONE);
			}
		}
		else if (conversation.isBotConv() && muteIcon != null)
		{
			if (conversation.isMutedBotConv(false))
			{
				muteIcon.setVisibility(View.VISIBLE);
			}
			else
			{
				muteIcon.setVisibility(View.GONE);
			}
		}
		else if (muteIcon != null)
		{
			muteIcon.setVisibility(View.GONE);
		}
	}

	public void updateViewsRelatedToLastMessage(View parentView, ConvMessage message, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		TextView messageView = viewHolder.subText;
		messageView.setVisibility(View.VISIBLE);
		CharSequence markedUp = getConversationText(conversation, message);
		messageView.setText(markedUp);
		
		updateViewsRelatedToMessageState(parentView, message, conversation);
		
		TextView tsView = viewHolder.timeStamp;
		tsView.setText(message.getTimestampFormatted(true, context));
	}

	public void updateViewsRelatedToMessageState(View parentView, ConvMessage message, Conversation conversation)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		if(viewHolder == null)
		{
			// TODO: Find the root cause for viewholder being null
			Logger.w("nux","Viewholder is null");
			return;
		}
		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!conversation.getMsisdn().equals(viewHolder.msisdn))
		{
			Logger.i("UnreadBug", "msisdns different !!! conversation msisdn : " + conversation.getMsisdn() + " veiwHolderMsisdn : " + viewHolder.msisdn);
			return;
		}

		ImageView imgStatus = viewHolder.imageStatus;

		TextView messageView = viewHolder.subText;

		TextView unreadIndicator = viewHolder.unreadIndicator;
		boolean isNuxLocked = NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_IS_ACTIVE && NUXManager.getInstance().isContactLocked(message.getMsisdn());
		unreadIndicator.setVisibility(View.GONE);
		imgStatus.setVisibility(View.GONE);
		
		if (!isNuxLocked && (message.getParticipantInfoState() == ParticipantInfoState.VOIP_CALL_SUMMARY ||
				message.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_INCOMING ||
						message.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_OUTGOING))
		{
			String messageText = null;
			int imageId = R.drawable.ic_voip_conv_miss;
			if (message.getParticipantInfoState() == ParticipantInfoState.VOIP_CALL_SUMMARY)
			{
				boolean initiator = message.getMetadata().isVoipInitiator();
				int duration = message.getMetadata().getDuration();
				if (initiator)
				{
					messageText = context.getString(R.string.voip_call_summary_outgoing);
					imageId = R.drawable.ic_voip_conv_out; 
				}
				else
				{
					messageText = context.getString(R.string.voip_call_summary_incoming);
					imageId = R.drawable.ic_voip_conv_in;
				}
				messageText += String.format(" (%02d:%02d)", (duration / 60), (duration % 60));
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_OUTGOING)
			{
				messageText = context.getString(R.string.voip_missed_call_outgoing);
			}
			else if (message.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_INCOMING)
			{
				messageText = context.getString(R.string.voip_missed_call_incoming);
			}
			
			messageView.setText(messageText);
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null) && conversation.getUnreadCount() > 0 && !message.isSent())
			{
				unreadIndicator.setVisibility(View.VISIBLE);
				unreadIndicator.setBackgroundResource(conversation.isStealth() ? R.drawable.bg_unread_counter_stealth : R.drawable.bg_unread_counter);
				unreadIndicator.setText(Integer.toString(conversation.getUnreadCount()));
			}

			imgStatus.setImageResource(imageId);
			imgStatus.setVisibility(View.VISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) messageView.getLayoutParams();
			lp.setMargins((int) (5 * Utils.densityMultiplier), lp.topMargin, lp.rightMargin, lp.bottomMargin);
			messageView.setLayoutParams(lp);
		}
		/*
		 * If the message is a status message, we only show an indicator if the status of the message is unread.
		 */
		else if (message.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE || message.getState() == State.RECEIVED_UNREAD)
		{
			
			if (message.isSent())
			{
				imgStatus.setImageResource(message.getImageState());
				imgStatus.setVisibility(View.VISIBLE);
			}

			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null) && conversation.getUnreadCount() > 0 && !message.isSent())
			{
				unreadIndicator.setVisibility(View.VISIBLE);

				unreadIndicator.setBackgroundResource(conversation.isStealth() ? R.drawable.bg_unread_counter_stealth : R.drawable.bg_unread_counter);

				unreadIndicator.setText(Integer.toString(conversation.getUnreadCount()));
			}
			if(isNuxLocked)
			{ 
				imgStatus.setVisibility(View.VISIBLE);
				imgStatus.setImageBitmap(NUXManager.getInstance().getNuxChatRewardPojo().getPendingChatIcon());
				messageView.setText(NUXManager.getInstance().getNuxChatRewardPojo().getChatWaitingText());		
			}
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) messageView.getLayoutParams();
			lp.setMargins(0, lp.topMargin, lp.rightMargin, lp.bottomMargin);
			messageView.setLayoutParams(lp);
		}

		if (message.getState() == ConvMessage.State.RECEIVED_UNREAD || isNuxLocked)
		{
			/* set NUX waiting or unread messages to BLUE */
			messageView.setTextColor(context.getResources().getColor(R.color.unread_message));
		}
		else
		{
			messageView.setTextColor(context.getResources().getColor(R.color.list_item_header));
		}
	}

	private CharSequence getConversationText(Conversation conversation, ConvMessage message)
	{
		MessageMetadata metadata = message.getMetadata();
		CharSequence markedUp = null;

		if (message.isFileTransferMessage())
		{
			markedUp = HikeFileType.getFileTypeMessage(context, metadata.getHikeFiles().get(0).getHikeFileType(), message.isSent());
			if ((conversation instanceof GroupConversation) && !message.isSent())
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(message.getGroupParticipantMsisdn()), markedUp);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED)
		{
			JSONArray participantInfoArray = metadata.getGcjParticipantInfo();
			String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);
			markedUp = GroupUtils.getParticipantAddedMessage(message, context, highlight);
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.DND_USER)
		{
			JSONArray dndNumbers = metadata.getDndNumbers();
			if (dndNumbers != null && dndNumbers.length() > 0)
			{
				StringBuilder dndNames = new StringBuilder();
				for (int i = 0; i < dndNumbers.length(); i++)
				{
					String dndName;
					dndName = conversation instanceof GroupConversation ? ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(dndNumbers.optString(i)) : Utils
							.getFirstName(conversation.getLabel());
					if (i < dndNumbers.length() - 2)
					{
						dndNames.append(dndName + ", ");
					}
					else if (i < dndNumbers.length() - 1)
					{
						dndNames.append(dndName + " and ");
					}
					else
					{
						dndNames.append(dndName);
					}
				}
				markedUp = String.format(context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames.toString());
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE)
		{
			if (conversation.isOnhike())
			{
				boolean firstIntro = conversation.getMsisdn().hashCode() % 2 == 0;
				markedUp = String.format(context.getString(firstIntro ? R.string.start_thread1 : R.string.start_thread1), Utils.getFirstName(conversation.getLabel()));
			}
			else
			{
				markedUp = String.format(context.getString(R.string.intro_sms_thread), Utils.getFirstName(conversation.getLabel()));
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
		{
			String participantName;
			if (conversation instanceof GroupConversation)
			{
				String participantMsisdn = metadata.getMsisdn();
				participantName = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(participantMsisdn);
			}
			else
			{
				participantName = Utils.getFirstName(conversation.getLabel());
			}
			
			markedUp = String.format(message.getMessage(), participantName);

		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT || message.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
		{

			if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT)
			{
				// Showing the block internation sms message if the user was
				// booted because of that reason
				String participantMsisdn = metadata.getMsisdn();
				String participantName = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(participantMsisdn);
				markedUp = GroupUtils.getParticipantRemovedMessage(conversation, context, participantName);
			}
			else
			{
				markedUp = GroupUtils.getConversationEndedMessage(conversation, context);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME)
		{
			if (message.isBroadcastConversation())
			{
				markedUp = String.format(context.getString(R.string.change_broadcast_name), context.getString(R.string.you));
			}
			else
			{
				String msisdn = metadata.getMsisdn();

				String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

				String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(msisdn);
				
				markedUp = GroupUtils.getConversationNameChangedMessage(conversation, context, participantName);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
		{
			markedUp = context.getString(R.string.block_internation_sms);
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
		{
			String msisdn = metadata.getMsisdn();
			String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

			String nameString;
			if (conversation instanceof GroupConversation)
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(msisdn);
			}
			else
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : Utils.getFirstName(conversation.getLabel());
			}

			markedUp = context.getString(R.string.chat_bg_changed, nameString);
		}
		else
		{
			String msg = message.getMessage();
			/*
			 * Making sure this string is never null.
			 */
			if (msg == null)
			{
				msg = "";
			}
			markedUp = msg.substring(0, Math.min(msg.length(), HikeConstants.MAX_MESSAGE_PREVIEW_LENGTH));
			// For showing the name of the contact that sent the message in
			// a group chat
			if (conversation instanceof GroupConversation && !TextUtils.isEmpty(message.getGroupParticipantMsisdn())
					&& message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
			{
				markedUp = Utils.addContactName(((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(message.getGroupParticipantMsisdn()), markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, true);
		}

		return markedUp;
	}

	private class CountDownSetter extends CountDownTimer
	{
		TextView textView;

		public CountDownSetter(TextView textView, long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			this.textView = textView;
		}

		@Override
		public void onFinish()
		{
			if (textView == null)
			{
				return;
			}
			textView.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			if (textView == null)
			{
				return;
			}

			setTimeRemainingText(textView, millisUntilFinished);
		}

		public void setTextView(TextView tv)
		{
			this.textView = tv;
		}
	}

	private void setTimeRemainingText(TextView textView, long millisUntilFinished)
	{
		long secondsUntilFinished = millisUntilFinished / 1000;
		int minutes = (int) (secondsUntilFinished / 60);
		int seconds = (int) (secondsUntilFinished % 60);
		String text = String.format("%1$02d:%2$02d", minutes, seconds);
		textView.setText(Html.fromHtml(context.getString(R.string.reset_stealth_tip, text)));

	}

	public void resetCountDownSetter()
	{
		if(countDownSetter == null)
		{
			return;
		}

		this.countDownSetter.cancel();
		this.countDownSetter = null;
	}

	public void addItemsToAnimat(Set<Conversation> stealthConversations)
	{
		for (Conversation conversation : stealthConversations)
		{
			itemsToAnimat.put(conversation.hashCode(), true);
		}
	}
	
	public void setItemAnimated(Conversation conv)
	{
		itemsToAnimat.delete(conv.hashCode());
	}
	
	public boolean itemToBeAnimated(Conversation conv)
	{
		return itemsToAnimat.get(conv.hashCode()) && conv.isStealth();
	}

	@Override
	public void notifyDataSetChanged()
	{
		Logger.d("TestList", "NotifyDataSetChanged called");
		super.notifyDataSetChanged();
	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			/*
			 * We don't want to call notifyDataSetChanged here since that causes the UI to freeze for a bit. Instead we pick out the views and update the avatars there.
			 */
			int count = listView.getChildCount();
			for (int i = 0; i < count; i++)
			{
				View view = listView.getChildAt(i);
				int indexOfData = listView.getFirstVisiblePosition() + i;

				if(indexOfData >= getCount())
				{
					return;
				}
				ViewType viewType = ViewType.values()[getItemViewType(indexOfData)];
				/*
				 * Since tips cannot have custom avatars, we simply skip these cases.
				 */
				if (viewType != ViewType.CONVERSATION)
				{
					continue;
				}

				updateViewsRelatedToAvatar(view, getItem(indexOfData));
			}
		}
	}
	
	public IconLoader getIconLoader()
	{
		return iconLoader;
	}

	public void addToLists(Conversation conv)
	{
		if (!isSearchModeOn)
		{
			completeList.add(conv);
		}
		else
		{
			conversationList.add(conv);
		}
		if(conversationsMsisdns!=null)
		{
			conversationsMsisdns.add(conv.getMsisdn());
		}
		if(phoneBookContacts!=null)
		{
			phoneBookContacts.remove(conv);
		}
	}

	public void addToLists(Set<Conversation> list)
	{
		for (Conversation conv : list)
		{
			addToLists(conv);
		}
	}

	public void removeStealthConversationsFromLists()
	{
		for (Iterator<Conversation> iter = completeList.iterator(); iter.hasNext();)
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
				conversationList.remove(conv);
				if(conversationsMsisdns!=null)
				{
					conversationsMsisdns.remove(conv.getMsisdn());
				}
			}
		}
	}

	public void sortLists(Comparator<? super Conversation> mConversationsComparator)
	{
		Collections.sort(completeList, mConversationsComparator);
		Collections.sort(conversationList, mConversationsComparator);
	}

	public void remove(Conversation conv)
	{
		if (conv != null)
		{
			completeList.remove(conv);
			conversationList.remove(conv);
			if(conversationsMsisdns!=null)
			{
				conversationsMsisdns.remove(conv.getMsisdn());
			}
			if (phoneBookContacts != null)
			{
				phoneBookContacts.add(conv);
			}
		}
	}

}
