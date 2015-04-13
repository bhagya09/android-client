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
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.OneToNConvInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class ConversationsAdapter extends BaseAdapter
{

	private IconLoader iconLoader;

	private int mIconImageSize;

	private SparseBooleanArray itemsToAnimat;
	
	private List<ConvInfo> conversationList;

	private List<ConvInfo> phoneBookContacts;

	private List<ConvInfo> completeList;

	private Set<ConvInfo> stealthConversations;

	private Map<String, Integer> convSpanStartIndexes;

	private String refinedSearchText = "";

	private Context context;

	private ListView listView;
	
	private LayoutInflater inflater;

	private ContactFilter contactFilter;

	private Set<String> conversationsMsisdns;

	private boolean isSearchModeOn = false;

	private enum ViewType
	{
		CONVERSATION
	}

	private class ViewHolder
	{
		String msisdn;

		TextView headerText;

		TextView subText;

		ImageView imageStatus;

		TextView unreadIndicator;

		TextView timeStamp;

		ImageView avatar;
		
		ImageView muteIcon;
	}

	public ConversationsAdapter(Context context, List<ConvInfo> displayedConversations, Set<ConvInfo> stealthConversations, ListView listView)
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
		conversationList = new ArrayList<ConvInfo>();
		convSpanStartIndexes = new HashMap<String, Integer>();
	}

	@Override
	public int getCount()
	{
		return completeList.size();
	}

	@Override
	public ConvInfo getItem(int position)
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
		return ViewType.CONVERSATION.ordinal();
	}

	public List<ConvInfo> getCompleteList()
	{
		return completeList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final ConvInfo convInfo = getItem(position);

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
				v.setTag(viewHolder);
				break;
			}
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		viewHolder.msisdn = convInfo.getMsisdn();

		updateViewsRelatedToName(v, convInfo);

		if (itemToBeAnimated(convInfo))
		{
			Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_from_left);
			v.startAnimation(animation);
			setItemAnimated(convInfo);
		}
		
		ConvMessage lastConvMessage = convInfo.getLastConversationMsg();
		if (lastConvMessage != null)
		{
			updateViewsRelatedToLastMessage(v, lastConvMessage, convInfo);
		}
		
		if (lastConvMessage != null && convInfo.getTypingNotif() != null)
		{
			updateViewsRelatedToTypingNotif(v, convInfo);
		}

		updateViewsRelatedToAvatar(v, convInfo);

		updateViewsRelatedToMute(v, convInfo);
		
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
		for (ConvInfo conv : completeList)
		{
			conversationList.add(conv);
			conversationsMsisdns.add(conv.getMsisdn());
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
		refinedSearchText = "";
		/*
		 * Purposely returning conversation list on the UI thread on collapse to avoid showing ftue empty state. 
		 */
		completeList.clear();
		completeList.addAll(conversationList);
		notifyDataSetChanged();
	}

	private class FetchPhoneBookContactsTask extends AsyncTask<Void, Void, Void>
	{
		List<ConvInfo> hikeContacts = new ArrayList<ConvInfo>();

		@Override
		protected Void doInBackground(Void... arg0)
		{
			List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
			for (ContactInfo contact : allContacts)
			{
				ConvInfo convInfo = new ConvInfo.ConvInfoBuilder(contact.getMsisdn()).setConvName(contact.getName()).setOnHike(contact.isOnhike()).build();
				
				if(stealthConversations.contains(convInfo) || conversationsMsisdns.contains(contact.getMsisdn()) || !convInfo.isOnHike())
				{
					continue;
				}
				hikeContacts.add(getPhoneContactFakeConv(convInfo));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			phoneBookContacts = new ArrayList<ConvInfo>();
			phoneBookContacts.addAll(hikeContacts);
			super.onPostExecute(result);
		}
	}

	private ConvInfo getPhoneContactFakeConv(ConvInfo convInfo)
	{
		if (convInfo != null)
		{
			String msg= null;
			if (convInfo.isOnHike())
			{
				msg = context.getString(R.string.start_new_chat);
			}
			else
			{
				msg = context.getString(R.string.on_sms);
			}
			ConvMessage message = new ConvMessage(msg, convInfo.getMsisdn(), -1, State.RECEIVED_READ);
			convInfo.setLastConversationMsg(message);
		}
		return convInfo;
	}

	public void onQueryChanged(String s)
	{
		if (s == null)
		{
			s = "";
		}
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
				List<ConvInfo> filteredConversationsList = new ArrayList<ConvInfo>();
				List<ConvInfo> filteredphoneBookContacts = new ArrayList<ConvInfo>();

				if (conversationList != null && !conversationList.isEmpty())
				{
					filterList(conversationList, filteredConversationsList, textToBeFiltered);
				}
				if (phoneBookContacts != null && !phoneBookContacts.isEmpty())
				{
					filterList(phoneBookContacts, filteredphoneBookContacts, textToBeFiltered);
				}

				List<List<ConvInfo>> resultList = new ArrayList<List<ConvInfo>>();
				resultList.add(filteredConversationsList);
				resultList.add(filteredphoneBookContacts);

				results.values = resultList;
			}
			else
			{
				List<List<ConvInfo>> resultList = new ArrayList<List<ConvInfo>>();
				resultList.add(getOriginalList());
				results.values = resultList;
			}
			results.count = 1;
			return results;
		}

		private void filterList(List<ConvInfo> allList, List<ConvInfo> listToUpdate, String textToBeFiltered)
		{
			for (ConvInfo info : allList)
			{
				try
				{
					if(filterConvForSearch(info, textToBeFiltered))
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
		
		public boolean filterConvForSearch(ConvInfo convInfo, String textToBeFiltered)
		{
			boolean found = false;
			String msisdn = convInfo.getMsisdn();
			String name = convInfo.getConversationName();
			// For Groups/Broadcasts, the contact name can be empty, so the search is to be performed on the diaplayed name.
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				// getLabel() fetches the appropriate display name.
				name = convInfo.getLabel();
			}
			if (textToBeFiltered.equals(context.getString(R.string.broadcast).toLowerCase()) && OneToNConversationUtils.isBroadcastConversation(msisdn))
			{
				found = true;
			}
			else if (textToBeFiltered.equals(context.getString(R.string.group).toLowerCase()) && OneToNConversationUtils.isGroupConversation(msisdn))
			{
				found = true;
			}
			// The name field can be empty for unsaved 1 to 1 conversation
			// In this case search is to be performed on the contact number.
			else if (TextUtils.isEmpty(name))
			{
				if (msisdn.contains(textToBeFiltered))
				{
					found = true;
					int startIndex = msisdn.indexOf(textToBeFiltered);
					convSpanStartIndexes.put(msisdn, startIndex);
				}
			}
			else
			{
				name = name.toLowerCase();
				int startIndex = 0;
				if (name.startsWith(textToBeFiltered))
				{
					found = true;
					convSpanStartIndexes.put(msisdn, startIndex);
				}
				else if (name.contains(" " + textToBeFiltered))
				{
					found = true;
					startIndex = name.indexOf(" " + textToBeFiltered) + 1;
					convSpanStartIndexes.put(msisdn, startIndex);
				}
			}
			return found;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			List<List<ConvInfo>> resultList = (List<List<ConvInfo>>) results.values;

			List<ConvInfo> filteredSearchList = new ArrayList<ConvInfo>();
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

		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	protected List<ConvInfo> getOriginalList()
	{
		return conversationList;
	}

	public void updateViewsRelatedToName(View parentView, ConvInfo convInfo)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!convInfo.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		TextView contactView = viewHolder.headerText;
		String name = convInfo.getLabel();
		Integer startSpanIndex = convSpanStartIndexes.get(convInfo.getMsisdn());
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

		if (OneToNConversationUtils.isBroadcastConversation(convInfo.getMsisdn()))
		{
				contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
		else if (OneToNConversationUtils.isGroupConversation(convInfo.getMsisdn()))
		{
				contactView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_group, 0, 0, 0);
		}
		else
		{
			contactView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
	}

	public void updateViewsRelatedToAvatar(View parentView, ConvInfo convInfo)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!convInfo.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		ImageView avatarView = viewHolder.avatar;
		iconLoader.loadImage(convInfo.getMsisdn(), avatarView, isListFlinging, false, true);
	}

	public void updateViewsRelatedToMute(View parentView, ConvInfo convInfo)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		ImageView muteIcon = viewHolder.muteIcon;
		if (muteIcon != null)
		{
			if(convInfo.isMute())
			{
				muteIcon.setVisibility(View.VISIBLE);
			}
			else
			{
				muteIcon.setVisibility(View.GONE);
			}
		}
	}
	
	public void updateViewsRelatedToTypingNotif(View parentView, ConvInfo convInfo)
	{
		ConvMessage typingNotifMessage = generateTypingNotifMessage(convInfo.getLastConversationMsg(), convInfo.getTypingNotif());
		if (typingNotifMessage != null)
		{
			updateViewsRelatedToLastMessage(parentView, typingNotifMessage, convInfo);

		}
	}

	private ConvMessage generateTypingNotifMessage(ConvMessage lastConversationMsg, TypingNotification typingNotif)
	{
		ConvMessage convMessage = new ConvMessage(typingNotif);
		convMessage.setTimestamp(lastConversationMsg.getTimestamp());
		convMessage.setMessage(HikeConstants.IS_TYPING);
		convMessage.setState(State.RECEIVED_UNREAD);
		return convMessage;
	}

	public void updateViewsRelatedToLastMessage(View parentView, ConvMessage message, ConvInfo convInfo)
	{
		ViewHolder viewHolder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation.
		 * We don't need to do anything here then.
		 */
		if(!convInfo.getMsisdn().equals(viewHolder.msisdn))
		{
			return;
		}

		TextView messageView = viewHolder.subText;
		messageView.setVisibility(View.VISIBLE);
		CharSequence markedUp = getConversationText(convInfo, message);
		messageView.setText(markedUp);
		
		updateViewsRelatedToMessageState(parentView, message, convInfo);
		
		TextView tsView = viewHolder.timeStamp;
		tsView.setText(message.getTimestampFormatted(true, context));
	}

	public void updateViewsRelatedToMessageState(View parentView, ConvMessage message, ConvInfo convInfo)
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
		if(!convInfo.getMsisdn().equals(viewHolder.msisdn))
		{
			Logger.i("UnreadBug", "msisdns different !!! conversation msisdn : " + convInfo.getMsisdn() + " veiwHolderMsisdn : " + viewHolder.msisdn);
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
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null) && convInfo.getUnreadCount() > 0 && !message.isSent())
			{
				unreadIndicator.setVisibility(View.VISIBLE);
				unreadIndicator.setBackgroundResource(convInfo.isStealth() ? R.drawable.bg_unread_counter_stealth : R.drawable.bg_unread_counter);
				unreadIndicator.setText(Integer.toString(convInfo.getUnreadCount()));
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

			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null) && convInfo.getUnreadCount() > 0 && !message.isSent())
			{
				unreadIndicator.setVisibility(View.VISIBLE);

				unreadIndicator.setBackgroundResource(convInfo.isStealth() ? R.drawable.bg_unread_counter_stealth : R.drawable.bg_unread_counter);

				unreadIndicator.setText(Integer.toString(convInfo.getUnreadCount()));
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

	private CharSequence getConversationText(ConvInfo convInfo, ConvMessage message)
	{
		MessageMetadata metadata = message.getMetadata();
		CharSequence markedUp = null;

		if (message.isFileTransferMessage())
		{
			markedUp = HikeFileType.getFileTypeMessage(context, metadata.getHikeFiles().get(0).getHikeFileType(), message.isSent());
			// Group or broadcast
			if ((convInfo instanceof OneToNConvInfo) && !message.isSent())
			{
				markedUp = Utils.addContactName(((OneToNConvInfo)convInfo).getConvParticipantName(message.getGroupParticipantMsisdn()), markedUp);
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED)
		{
			JSONArray participantInfoArray = metadata.getGcjParticipantInfo();
			String highlight = Utils.getConversationJoinHighlightText(participantInfoArray, (OneToNConvInfo)convInfo);
			markedUp = OneToNConversationUtils.getParticipantAddedMessage(message, context, highlight);
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
					dndName = convInfo instanceof OneToNConvInfo ? ((OneToNConvInfo) convInfo).getConvParticipantName(dndNumbers.optString(i)) : Utils
							.getFirstName(convInfo.getLabel());
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
				markedUp = String.format(context.getString(convInfo instanceof OneToNConvInfo ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames.toString());
			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.INTRO_MESSAGE)
		{
//			if (convInfo.isOnhike())
			{
				boolean firstIntro = convInfo.getMsisdn().hashCode() % 2 == 0;
				markedUp = String.format(context.getString(firstIntro ? R.string.start_thread1 : R.string.start_thread2), Utils.getFirstName(convInfo.getLabel()));
			}
//			else
//			{
//				markedUp = String.format(context.getString(R.string.intro_sms_thread), Utils.getFirstName(convInfo.getConversationName()));
//			}
		}
		else if (message.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
		{
			String participantName;
			if (convInfo instanceof OneToNConvInfo)
			{
				String participantMsisdn = metadata.getMsisdn();
				participantName = ((OneToNConvInfo) convInfo).getConvParticipantName(participantMsisdn);
			}
			else
			{
				participantName = Utils.getFirstName(convInfo.getLabel());
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
				String participantName = ((OneToNConvInfo) convInfo).getConvParticipantName(participantMsisdn);
				markedUp = OneToNConversationUtils.getParticipantRemovedMessage(convInfo.getMsisdn(), context, participantName);
			}
			else
			{
				markedUp = OneToNConversationUtils.getConversationEndedMessage(convInfo.getMsisdn(), context);
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

				String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((OneToNConvInfo) convInfo).getConvParticipantName(msisdn);
				
				markedUp = OneToNConversationUtils.getConversationNameChangedMessage(convInfo.getMsisdn(), context, participantName);
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
			if (convInfo instanceof OneToNConvInfo)
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((OneToNConvInfo) convInfo).getConvParticipantName(msisdn);
			}
			else
			{
				nameString = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : Utils.getFirstName(convInfo.getLabel());
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
			if (convInfo instanceof OneToNConvInfo && !TextUtils.isEmpty(message.getGroupParticipantMsisdn())
					&& message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
			{
				markedUp = Utils.addContactName(((OneToNConvInfo) convInfo).getConvParticipantName(message.getGroupParticipantMsisdn()), markedUp);
			}
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, true);
		}

		return markedUp;
	}

	public void addItemsToAnimat(Set<ConvInfo> stealthConversations)
	{
		for (ConvInfo conversation : stealthConversations)
		{
			itemsToAnimat.put(conversation.hashCode(), true);
		}
	}
	
	public void setItemAnimated(ConvInfo conv)
	{
		itemsToAnimat.delete(conv.hashCode());
	}
	
	public boolean itemToBeAnimated(ConvInfo conv)
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

	public void removeStealthConversationsFromLists()
	{
		for (Iterator<ConvInfo> iter = completeList.iterator(); iter.hasNext();)
		{
			Object object = iter.next();
			if (object == null)
			{
				continue;
			}
			ConvInfo conv = (ConvInfo) object;
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

	public void sortLists(Comparator<? super ConvInfo> mConversationsComparator)
	{
		Collections.sort(completeList, mConversationsComparator);
		Collections.sort(conversationList, mConversationsComparator);
	}
	
	public void addToLists(ConvInfo convInfo)
	{
		conversationList.add(convInfo);
		if(conversationsMsisdns!=null)
		{
			conversationsMsisdns.add(convInfo.getMsisdn());
		}
		if(phoneBookContacts!=null)
		{
			phoneBookContacts.remove(convInfo);
		}
		if (!isSearchModeOn)
		{
			completeList.add(convInfo);
		}
		else
		{
			onQueryChanged(refinedSearchText);
		}
	}

	public void addToLists(Set<ConvInfo> list)
	{
		for (ConvInfo convInfo : list)
		{
			addToLists(convInfo);
		}
	}

	public void remove(ConvInfo conv)
	{
		if (conv != null)
		{
			completeList.remove(conv);
			conversationList.remove(conv);
			if(conversationsMsisdns!=null)
			{
				conversationsMsisdns.remove(conv.getMsisdn());
			}
			if (phoneBookContacts != null && conv.isOnHike())
			{
				phoneBookContacts.add(getPhoneContactFakeConv(conv));
			}
			if (isSearchModeOn)
			{
				onQueryChanged(refinedSearchText);
			}
		}
	}
}
