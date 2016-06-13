package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.HikeFeatureInfo;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.FetchFriendsTask;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

public class ComposeChatAdapter extends FriendsAdapter implements PinnedSectionListAdapter
{

	private final boolean showTimeline;

	private Map<String, ContactInfo> selectedPeople;

	private Map<String, ContactInfo> existingParticipants;

	private boolean showCheckbox;

	private int mIconImageSize;

	private IconLoader iconloader;

	private boolean fetchGroups;

	private boolean fetchRecents;
	
	private String existingGroupId;

	private String sendingMsisdn;

	private int statusForEmptyContactInfo;

	private List<ContactInfo> newContactsList;

	private boolean isCreatingOrEditingGroup;

	private boolean nuxStateActive = false;
	
	private boolean isSearchModeOn = false;
	
	private boolean addFriendOption;

	private List<String> composeExcludeList;

	private boolean isGroupFirst;

	private boolean createGroupBroadcastOption;

    public ComposeChatAdapter(Context context, ListView listView, boolean fetchGroups, boolean fetchRecents, String existingGroupId, String sendingMsisdn, FriendsListFetchedCallback friendsListFetchedCallback, boolean showSMSContacts, boolean showHikeContacts, boolean showTimeline, boolean showBdaySection)
	{
		super(context, listView, friendsListFetchedCallback, ContactInfo.lastSeenTimeComparatorWithoutFav);
		selectedPeople = new LinkedHashMap<String, ContactInfo>();
		existingParticipants = new HashMap<String, ContactInfo>();
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconloader = new IconLoader(context, mIconImageSize);
		iconloader.setDefaultAvatarIfNoCustomIcon(true);
		iconloader.setImageFadeIn(false);

		this.existingGroupId = existingGroupId;
		this.sendingMsisdn = sendingMsisdn;
		this.fetchGroups = fetchGroups;
		this.fetchRecents = fetchRecents;

		groupsList = new ArrayList<ContactInfo>(0);
		groupsStealthList = new ArrayList<ContactInfo>(0);
		filteredGroupsList = new ArrayList<ContactInfo>(0);
		

		recentContactsList = new ArrayList<ContactInfo>(0);
		filteredRecentsList = new ArrayList<ContactInfo>(0);

		this.showHikeContacts = showHikeContacts;

		this.showSMSContacts = showSMSContacts;

		this.showTimeline = showTimeline;

		this.showBdaySection = showBdaySection;
	}

	public void setCreateGroupBroadcastOption(boolean setOption)
	{
		createGroupBroadcastOption = setOption;
	}

	public void setIsCreatingOrEditingGroup(boolean b)
	{
		isCreatingOrEditingGroup = b;
	}
	
	public void setNuxStateActive(boolean nuxStateActive) {
		this.nuxStateActive = nuxStateActive;
	}

	public void setComposeExcludeList(List<String> composeExcludeList) { this.composeExcludeList = composeExcludeList; }

	public void setGroupFirst(boolean isGroupFirst) { this.isGroupFirst = isGroupFirst; }

	public void executeFetchTask()
	{
		setLoadingView();
		FetchFriendsTask fetchFriendsTask;
		if(nuxStateActive){
			boolean fetchHikeContacts = true;
			boolean fetchSMSContacts = true;
			boolean fetchRecommendedContacts;
			boolean fetchHideListContacts;


			NuxSelectFriends nuxPojo = NUXManager.getInstance().getNuxSelectFriendsPojo();
			fetchHideListContacts = (nuxPojo.getHideList() != null && !nuxPojo.getHideList().isEmpty());
			fetchRecommendedContacts = (nuxPojo.getRecoList() != null && !nuxPojo.getRecoList().isEmpty());

			int contactsShown = nuxPojo.getContactSectionType();
			switch(NUXConstants.ContactSectionTypeEnum.getEnum(contactsShown)){
				case none :
					fetchHikeContacts = false;
					fetchSMSContacts = false;
					break;
				case nonhike:
					fetchHikeContacts = false;
					break;
				case hike :
					fetchSMSContacts = false;
					break;
				case both :
				case all :
				default:

			}

			fetchFriendsTask = new FetchFriendsTask(this, context, friendsList, hikeContactsList, smsContactsList, recentContactsList, friendsStealthList, hikeStealthContactsList,
					smsStealthContactsList, filteredFriendsList, filteredHikeContactsList, filteredSmsContactsList, groupsList, groupsStealthList, nuxRecommendedList, nuxFilteredRecoList, filteredGroupsList, filteredRecentsList,
					existingParticipants, sendingMsisdn, false, existingGroupId, isCreatingOrEditingGroup, fetchSMSContacts, false, false, fetchHikeContacts, false, fetchRecommendedContacts, fetchHideListContacts, null, showBdaySection,
                    hikeBdayContactList, filteredHikeBdayContactList);
		} else {
			fetchFriendsTask = new FetchFriendsTask(this, context, friendsList, hikeContactsList, smsContactsList, recentContactsList, friendsStealthList, hikeStealthContactsList,
					smsStealthContactsList, filteredFriendsList, filteredHikeContactsList, filteredSmsContactsList, groupsList, groupsStealthList, null, null, filteredGroupsList, filteredRecentsList,
					existingParticipants, sendingMsisdn, fetchGroups, existingGroupId, isCreatingOrEditingGroup, showSMSContacts, false, fetchRecents, showHikeContacts, true, false , false, composeExcludeList,
                    showBdaySection, hikeBdayContactList, filteredHikeBdayContactList);
		}

		if (showTimeline)
		{
			ContactInfo timelineListItem = new HikeFeatureInfo(context.getResources().getString(R.string.timeline), R.drawable.ic_timeline, context.getResources().getString(
					R.string.timeline_short_desc), true, new Intent());
			timelineListItem.setId(ComposeChatAdapter.HIKE_FEATURES_ID);
			timelineListItem.setPhoneNum(ComposeChatAdapter.HIKE_FEATURES_TIMELINE_ID);
			timelineListItem.setName(context.getString(R.string.timeline));
			timelineListItem.setMsisdn(context.getString(R.string.timeline));
			timelineListItem.setOnhike(true);

			hikeOtherFeaturesList.add(timelineListItem);

			fetchFriendsTask.addOtherFeaturesList(hikeOtherFeaturesList,filteredHikeOtherFeaturesList);
		}
		if (createGroupBroadcastOption) {
			ContactInfo newGroupListItem = new HikeFeatureInfo(context.getString(R.string.new_group), R.drawable.ic_new_group, null, false, new Intent(), false, false);
			newGroupListItem.setId(ComposeChatAdapter.HIKE_FEATURES_ID);
			newGroupListItem.setName(context.getString(R.string.new_group));
			newGroupListItem.setMsisdn(ComposeChatAdapter.HIKE_FEATURES_NEW_GROUP_ID);
			newGroupListItem.setOnhike(true);

			ContactInfo newBroadcastListItem = new HikeFeatureInfo(context.getString(R.string.new_broadcast), R.drawable.ic_new_broadcast, null, false, new Intent(), false, false);
			newBroadcastListItem.setId(ComposeChatAdapter.HIKE_FEATURES_ID);
			newBroadcastListItem.setName(context.getString(R.string.new_broadcast));
			newBroadcastListItem.setMsisdn(ComposeChatAdapter.HIKE_FEATURES_NEW_BROADCAST_ID);
			newBroadcastListItem.setOnhike(true);

			hikeOtherFeaturesList.add(newGroupListItem);
			hikeOtherFeaturesList.add(newBroadcastListItem);

			fetchFriendsTask.addOtherFeaturesList(hikeOtherFeaturesList,filteredHikeOtherFeaturesList);
		}
		fetchFriendsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ContactInfo contactInfo = null;
		ViewHolder holder = null;

		if (convertView == null)
		{
			convertView = inflateView(viewType, parent);
		}

		contactInfo = getItem(position);
		// either section or other we do have
		if (viewType == ViewType.SECTION)
		{
			TextView tv = (TextView) convertView.findViewById(R.id.name);
			tv.setText(contactInfo.getName());

			TextView count = (TextView) convertView.findViewById(R.id.count);
			count.setText(contactInfo.getMsisdn());
			// set section heading
			if (!TextUtils.isEmpty(contactInfo.getPhoneNum()))
			{
				switch (contactInfo.getPhoneNum())
				{
				case FRIEND_PHONE_NUM:
					tv.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(Utils.isFavToFriendsMigrationAllowed() ? R.drawable.ic_section_header_friends : R.drawable.ic_section_header_favorite), null, null, null);
					break;

				case CONTACT_PHONE_NUM:
					tv.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_people_on_hike), null, null, null);
					break;

				case CONTACT_SMS_NUM:
					tv.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_sms_contact), null, null, null);
					break;

				case APPS_ON_HIKE:
				case HIKE_FEATURES_ID:
					tv.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_hike_apps), null, null, null);
					break;
				case BDAY_CONTACT_ID:
					tv.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_hike_bdays), null, null, null);
					break;
				}

				tv.setCompoundDrawablePadding((int) context.getResources().getDimension(R.dimen.favorites_star_icon_drawable_padding));
			}
			else
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}
		else if (viewType == ViewType.HIKE_FEATURES)
		{
			holder = (ViewHolder) convertView.getTag();
			HikeFeatureInfo hikeFeatureInfo = (HikeFeatureInfo)contactInfo;
			Integer startIndex = contactSpanStartIndexes.get(hikeFeatureInfo.getMsisdn());
			if(startIndex!=null && viewType != ViewType.NEW_CONTACT)
			{
				holder.name.setText(getSpanText(hikeFeatureInfo.getName(), startIndex), TextView.BufferType.SPANNABLE);
			}
			else
			{
				holder.name.setText(hikeFeatureInfo.getName());
			}
			String descp = hikeFeatureInfo.getDescription();
			if (TextUtils.isEmpty(descp)) {
				holder.numLayout.setVisibility(View.GONE);
			} else {
				holder.numLayout.setVisibility(View.VISIBLE);
				holder.status.setText(descp);
			}

			Drawable timelineLogoDrawable = ContextCompat.getDrawable(context, hikeFeatureInfo.getIconDrawable());
			Drawable otherFeaturesDrawable = ContextCompat.getDrawable(context, R.drawable.other_features_bg);

			holder.userImage.setImageDrawable(timelineLogoDrawable);

			int paddingPx = HikePhotosUtils.dpToPx(10);

			holder.userImage.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

			if (hikeFeatureInfo.isIconBGColorToBeSet()) {
				if (Utils.isJellybeanOrHigher()) {
					holder.userImage.setBackground(otherFeaturesDrawable);
				} else {
					holder.userImage.setBackgroundDrawable(otherFeaturesDrawable);
				}
			} else {
				holder.userImage.setBackground(null);
			}

			if (hikeFeatureInfo.isShowCheckBox())
			{
				holder.checkbox.setVisibility(View.VISIBLE);
				if (selectedPeople.containsKey(contactInfo.getMsisdn()))
				{

					holder.checkbox.setChecked(true);
				}
				else
				{
					holder.checkbox.setChecked(false);
				}
			}
			else
			{
				holder.checkbox.setVisibility(View.GONE);
			}
		}
		else if (viewType == ViewType.BDAY_CONTACT)
		{
			holder = (ViewHolder) convertView.getTag();
			String msisdn = contactInfo.getMsisdn();
			holder.msisdn = msisdn;

			String name = contactInfo.getName();
			if (TextUtils.isEmpty(name))
			{
				holder.name.setText(msisdn);
			}
			else
			{
				Integer startIndex = contactSpanStartIndexes.get(msisdn);
				if (startIndex != null && viewType != ViewType.NEW_CONTACT)
				{
					holder.name.setText(getSpanText(name, startIndex), TextView.BufferType.SPANNABLE);
				}
				else
				{
					holder.name.setText(name);
				}
			}

			updateViewsRelatedToAvatar(convertView, contactInfo);

			if(showCheckbox)
			{
				holder.checkbox.setVisibility(View.VISIBLE);
				if (selectedPeople.containsKey(contactInfo.getMsisdn())){
					holder.checkbox.setChecked(true);
				} else {
					holder.checkbox.setChecked(false);
				}
			}
			else
			{
				holder.checkbox.setVisibility(View.GONE);
			}
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
			String msisdn = contactInfo.getMsisdn();
			holder.msisdn = msisdn;

			holder.status.setText(contactInfo.getMsisdn());

			String name = contactInfo.getName();
			if(TextUtils.isEmpty(name))
			{
				holder.name.setText(msisdn);
			}
			else
			{
				Integer startIndex = contactSpanStartIndexes.get(msisdn);
				if(startIndex!=null && viewType != ViewType.NEW_CONTACT)
				{
					holder.name.setText(getSpanText(name, startIndex), TextView.BufferType.SPANNABLE);
				}
				else
				{
					holder.name.setText(name);
				}
			}

			if (viewType == ViewType.NEW_CONTACT)
			{
				holder.status.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				holder.status.setText(statusForEmptyContactInfo);
				holder.statusMood.setVisibility(View.GONE);
			}
			else if (contactInfo.getFavoriteType() == FavoriteType.FRIEND || contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
			{
				StatusMessage lastStatusMessage = getLastStatusMessagesMap().get(contactInfo.getMsisdn());
				if (lastStatusMessage != null)
				{
					holder.status.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
					SmileyParser smileyParser = SmileyParser.getInstance();
					switch (lastStatusMessage.getStatusMessageType())
					{
					case TEXT:
						holder.status.setText(smileyParser.addSmileySpans(lastStatusMessage.getText(), true));
						if (lastStatusMessage.hasMood())
						{
							holder.statusMood.setVisibility(View.VISIBLE);
							holder.statusMood.setImageResource(EmoticonConstants.moodMapping.get(lastStatusMessage.getMoodId()));
						}
						else
						{
							holder.statusMood.setVisibility(View.GONE);
						}
						break;

					case PROFILE_PIC:
						holder.status.setText(R.string.changed_profile);
						holder.statusMood.setVisibility(View.GONE);
						break;

					case IMAGE:
					case TEXT_IMAGE:
						if(TextUtils.isEmpty(lastStatusMessage.getText()))
						{
							holder.status.setText(lastStatusMessage.getMsisdn());
						}
						else
						{
							holder.status.setText(smileyParser.addSmileySpans(lastStatusMessage.getText(), true));
						}
						if (lastStatusMessage.hasMood())
						{
							holder.statusMood.setVisibility(View.VISIBLE);
							holder.statusMood.setImageResource(EmoticonConstants.moodMapping.get(lastStatusMessage.getMoodId()));
						}
						else
						{
							holder.statusMood.setVisibility(View.GONE);
						}
						break;
						
					default:
						break;
					}
				}
				else
				{
					holder.status.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
					holder.status.setText(contactInfo.getMsisdn());
					holder.statusMood.setVisibility(View.GONE);
				}
				if(holder.onlineIndicator != null)
				{
					holder.onlineIndicator.setVisibility(View.GONE);
				}
			}
			else
			{
				holder.status.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				holder.status.setText(OneToNConversationUtils.isGroupConversation(contactInfo.getMsisdn()) ? contactInfo.getPhoneNum():contactInfo.getMsisdn());
				holder.statusMood.setVisibility(View.GONE);
				if (viewType != ViewType.FRIEND)
				{
					if (!contactInfo.isOnhike() && !showCheckbox)
					{
						long inviteTime = contactInfo.getInviteTime();
						if (inviteTime == 0)
						{
							holder.inviteIcon.setVisibility(View.VISIBLE);
							holder.inviteText.setVisibility(View.GONE);
							holder.inviteIcon.setTag(contactInfo);
							holder.inviteIcon.setOnClickListener(new OnClickListener()
							{

								public void onClick(View v)
								{
									ContactInfo contactInfo = (ContactInfo) v.getTag();
									Utils.sendInviteUtil(contactInfo, context, HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED, context.getString(R.string.native_header),
											context.getString(R.string.native_info), WhichScreen.SMS_SECTION);
									notifyDataSetChanged();
								}
							});
						}
						else
						{
							holder.inviteIcon.setVisibility(View.GONE);
							holder.inviteText.setVisibility(View.VISIBLE);
						}
					}
				}
			}

			/*
			 * We don't have an avatar for new contacts. So set a hard coded one
			 */
			if (viewType == ViewType.NEW_CONTACT)
			{
				holder.userImage.setImageDrawable(HikeBitmapFactory.getDefaultTextAvatar(null));
			}
			else
			{
				updateViewsRelatedToAvatar(convertView, contactInfo);
			}

			if (showCheckbox)
			{
				if (!contactInfo.isMyOneWayFriend()
						&& Utils.isFavToFriendsMigrationAllowed()
						&& !OneToNConversationUtils.isOneToNConversation(contactInfo.getMsisdn())
						&& addFriendOption)
				{
					holder.addFriendIcon.setVisibility(View.VISIBLE);
					holder.checkbox.setVisibility(View.GONE);
				}
				else
				{
					holder.addFriendIcon.setVisibility(View.GONE);
					holder.checkbox.setVisibility(View.VISIBLE);
					if (selectedPeople.containsKey(contactInfo.getMsisdn())){
						holder.checkbox.setChecked(true);
					} else {
						holder.checkbox.setChecked(false);
					}
				}
			}
			else
			{
				holder.addFriendIcon.setVisibility(View.GONE);
				holder.checkbox.setVisibility(View.GONE);
			}
		}
		return convertView;
	}

	private void updateViewsRelatedToAvatar(View parentView, ContactInfo contactInfo)
	{
		ViewHolder holder = (ViewHolder) parentView.getTag();

		/*
		 * If the viewholder's msisdn is different from the converstion's msisdn, it means that the viewholder is currently being used for a different conversation. We don't need
		 * to do anything here then.
		 */
		if (!contactInfo.getMsisdn().equals(holder.msisdn))
		{
			return;
		}

		holder.userImage.setScaleType(ScaleType.FIT_CENTER);
		String id = contactInfo.isGroupConversationContact() ? contactInfo.getId() : contactInfo.getMsisdn();
		iconloader.loadImage(id, holder.userImage, isListFlinging, false, true,contactInfo);
	}

	private View inflateView(ViewType viewType, ViewGroup parent)
	{
		View convertView = null;
		ViewHolder holder = null;
		switch (viewType)
		{
		case SECTION:
			convertView = LayoutInflater.from(context).inflate(R.layout.friends_group_view, null);
			break;
		case FRIEND:
			convertView = LayoutInflater.from(context).inflate(R.layout.friends_child_view, null);
			holder = new ViewHolder();
			holder.userImage = (ImageView) convertView.findViewById(R.id.avatar);
			holder.name = (TextView) convertView.findViewById(R.id.contact);
			holder.status = (TextView) convertView.findViewById(R.id.last_seen);
			holder.statusMood = (ImageView) convertView.findViewById(R.id.status_mood);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
			holder.addFriendIcon = (ImageView) convertView.findViewById(R.id.add_friend);
			holder.onlineIndicator = (ImageView) convertView.findViewById(R.id.online_indicator);
			convertView.setTag(holder);
			break;
		case BDAY_CONTACT:
			convertView = LayoutInflater.from(context).inflate(R.layout.hike_bday_list_item, parent, false);
			holder = new ViewHolder();
			holder.userImage = (ImageView) convertView.findViewById(R.id.contact_image);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
			convertView.setTag(holder);
			break;
		default:
			convertView = LayoutInflater.from(context).inflate(R.layout.hike_list_item, parent, false);
			holder = new ViewHolder();
			holder.userImage = (ImageView) convertView.findViewById(R.id.contact_image);
			holder.numLayout = (View) convertView.findViewById(R.id.num_layout);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.status = (TextView) convertView.findViewById(R.id.number);
			holder.statusMood = (ImageView) convertView.findViewById(R.id.status_mood);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
			holder.inviteText = (TextView) convertView.findViewById(R.id.invite_Text);
			holder.addFriendIcon = (ImageView) convertView.findViewById(R.id.add_friend);
			holder.inviteIcon = (ImageView) convertView.findViewById(R.id.invite_icon);
			convertView.setTag(holder);
			break;
		}
		return convertView;
	}

	private static class ViewHolder
	{
		ImageView userImage;

		TextView name;

		TextView status;

		CheckBox checkbox;

		ImageView statusMood;

		ImageView onlineIndicator;

		String msisdn;

		TextView inviteText;

		ImageView inviteIcon;
		
		RecyclerView recyclerView;

		ImageView addFriendIcon;

		View numLayout;
	}

	@Override
	protected List<List<ContactInfo>> makeOriginalList()
	{
		return super.makeOriginalList();
	}

	@Override
	public void makeCompleteList(boolean filtered)
	{
		makeCompleteList(filtered, false);
	}

	@Override
	public void makeCompleteList(boolean filtered, boolean firstFetch)
	{
		if (firstFetch)
		{
			friendsListFetchedCallback.listFetched();
		}

		//Fix AND-3408
		boolean shouldContinue = makeSetupForCompleteList(filtered, firstFetch);

		if (!shouldContinue)
		{
			return;
		}

		if (filteredHikeOtherFeaturesList != null)
		{
			ContactInfo otherFeaturesSection = new ContactInfo(SECTION_ID, "1", context.getResources().getString(R.string.you).toUpperCase(), HIKE_FEATURES_ID);
			if (!filteredHikeOtherFeaturesList.isEmpty())
			{
				for (ContactInfo info: filteredHikeOtherFeaturesList)
				{
					if (info instanceof HikeFeatureInfo)
					{
						if (((HikeFeatureInfo) info).needsHeader()) {
							completeList.add(otherFeaturesSection);
							break;
						}
					}
				}
				completeList.addAll(filteredHikeOtherFeaturesList);
			}
			else
			{
				completeList.remove(otherFeaturesSection);
				completeList.removeAll(filteredHikeOtherFeaturesList);
			}
		}

		if(nuxRecommendedList != null && !nuxRecommendedList.isEmpty())
		{
			String recoSectionHeader = NUXManager.getInstance().getNuxSelectFriendsPojo().getRecoSectionTitle();
			ContactInfo recommendedSection = new ContactInfo(SECTION_ID, Integer.toString(nuxFilteredRecoList.size()), recoSectionHeader, RECOMMENDED);
			if(nuxFilteredRecoList.size() > 0){
				completeList.add(recommendedSection);
				completeList.addAll(nuxFilteredRecoList);
			}
		}

		if (filteredHikeBdayContactList.size() != 0 && showBdaySection)
		{
			ContactInfo hikeBdayContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredHikeBdayContactList.size()), context.getString(R.string.hike_bday_contacts),
					BDAY_CONTACT_ID);
			updateHikeBdayContactList(hikeBdayContactsSection);
		}

		// hack for header, as we are using pinnedSectionListView
		if(fetchRecents && !recentContactsList.isEmpty())
		{
			ContactInfo recentsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredRecentsList.size()), context.getString(R.string.recent_chats), RECENT_PHONE_NUM);
			if (filteredRecentsList.size() > 0)
			{
				completeList.add(recentsSection);
				completeList.addAll(filteredRecentsList);
			}
		}

		boolean addFirstGroups = true;
	       
		if (groupsList.size() < filteredFriendsList.size()) {
			addFirstGroups = false;
		}

		if(!isGroupFirst)
		{
			if (addFirstGroups) {
				addGroupList();
				addFriendList();
			} else {
				addFriendList();
				addGroupList();
			}
		}
		else
		{
			Logger.d("ComposeChatAdapter","isGroupFirst");
			addGroupList();
			addFriendList();
		}
		if (showHikeContacts && isHikeContactsPresent())
		{
			ContactInfo hikeContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredHikeContactsList.size()), context.getString(R.string.hike_contacts),
					CONTACT_PHONE_NUM);
			updateHikeContactList(hikeContactsSection);
		}
		if (showSMSContacts)
		{
			ContactInfo smsContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredSmsContactsList.size()), context.getString(R.string.sms_contacts),
					CONTACT_SMS_NUM);
			updateSMSContacts(smsContactsSection);
		}
		if (newContactsList != null)
		{
			completeList.addAll(newContactsList);
		}
		notifyDataSetChanged();
		setEmptyView();
		friendsListFetchedCallback.completeListFetched();
		
		
		
	}
	private void addFriendList() {
		ContactInfo friendsSection = null;
		if (!filteredFriendsList.isEmpty())
		{
			friendsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredFriendsList.size()), context.getString(Utils.isFavToFriendsMigrationAllowed() ? R.string.friends_upper_case : R.string.favorites_upper_case), FRIEND_PHONE_NUM);
		}
		updateFriendsList(friendsSection, false, false);
	}

	private void addGroupList() {
		if (fetchGroups && !groupsList.isEmpty())
		{
			ContactInfo groupSection = new ContactInfo(SECTION_ID, Integer.toString(filteredGroupsList.size()), context.getString(R.string.group_chats_upper_case), GROUP_MSISDN);
			if (filteredGroupsList.size() > 0)
			{
				completeList.add(groupSection);
				completeList.addAll(filteredGroupsList);
			}
		}
	}
	public void addContact(ContactInfo contactInfo)
	{
		selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
		notifyDataSetChanged();
	}

	public void removeContact(ContactInfo contactInfo)
	{
		selectedPeople.remove(contactInfo.getMsisdn());
		notifyDataSetChanged();
	}

	public void clearAllSelection(boolean showCheckbox)
	{
		selectedPeople.clear();
		this.showCheckbox = showCheckbox;
		notifyDataSetChanged();
	}

	public void showCheckBoxAgainstItems(boolean showCheckbox)
	{
		this.showCheckbox = showCheckbox;

	}

	public void provideAddFriend(boolean addFriend)
	{
		this.addFriendOption = addFriend;
	}

	public ArrayList<ContactInfo> getAllSelectedContacts()
	{
		return new ArrayList<ContactInfo>(selectedPeople.values());
	}

	public List<String> getAllSelectedContactsMsisdns()
	{
		List<String> people = new ArrayList<String>(selectedPeople.keySet());
		return people;
	}
	
	/**
	 * It includes contact which are currently selected and existing to group (if applicable)
	 * 
	 * @return
	 */
	public int getSelectedContactCount()
	{
		return selectedPeople.size() + existingParticipants.size();
	}

	public int getCurrentSelection()
	{
		return selectedPeople.size();
	}

	public int getExistingSelection()
	{
		return existingParticipants.size();
	}
	
	public ArrayList<List<ContactInfo>> getOnHikeContactLists()
	{
		ArrayList<List<ContactInfo>> onHikeLists = new ArrayList<List<ContactInfo>>();
		onHikeLists.add(getOnHikeSublist(friendsList));
		onHikeLists.add(hikeContactsList);
		onHikeLists.add(getOnHikeSublist(recentContactsList));

		return onHikeLists;
	}

	public List<ContactInfo> getOnHikeSublist(List<ContactInfo> completeList)
	{
		List<ContactInfo> subList = new ArrayList<ContactInfo>();
		for (ContactInfo contactInfo : completeList) {
			if(contactInfo.isOnhike())
			{
				subList.add(contactInfo);
			}
		}
		return subList;
	}
	
	public int getOnHikeContactsCount()
	{
		int contactCount = 0;
		ArrayList<List<ContactInfo>> onHikeLists = getOnHikeContactLists();
		for (List<ContactInfo> list : onHikeLists) {
			contactCount += list.size();
		}
		return contactCount;
	}

	@Override
	protected void makeFilteredList(CharSequence constraint, List<List<ContactInfo>> resultList)
	{
		// TODO Auto-generated method stub

		super.makeFilteredList(constraint, resultList);
		// to add new section and number for user typed number
		String text = constraint.toString().trim();
	
		if (isIntegers(text))
		{
			newContactsList = new ArrayList<ContactInfo>();
			ContactInfo section = new ContactInfo(SECTION_ID, null, context.getString(R.string.compose_chat_other_contacts), null);
			String normalisedMsisdn = getNormalisedMsisdn(text);
			ContactInfo info = new ContactInfo(normalisedMsisdn, normalisedMsisdn, normalisedMsisdn, text);
			newContactsList.add(section);
			newContactsList.add(info);
		}
		else
		{
			newContactsList = null;
		}
	}

	private boolean isIntegers(String input)
	{
		return input.matches("\\+?\\d+");
	}

	private String getNormalisedMsisdn(String textEntered)
	{
		return PhoneUtils.normalizeNumber(textEntered,
				context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
	}

	public void setStatusForEmptyContactInfo(int statusStringId)
	{
		this.statusForEmptyContactInfo = statusStringId;
	}

	public boolean isContactAdded(ContactInfo info)
	{
		return selectedPeople.containsKey(info.getMsisdn());
	}

	public boolean isContactPresentInExistingParticipants(ContactInfo info)
	{
		return existingParticipants.containsKey(info.getMsisdn());
	}

	@Override
	public int getItemViewType(int position)
	{
		ContactInfo info = getItem(position);
		if(OneToNConversationUtils.isGroupConversation(info.getMsisdn()))
		{
			return super.getItemViewType(position);
		}
		else if (info.isUnknownContact() && info.getFavoriteType() == null)
		{
			return ViewType.NEW_CONTACT.ordinal();
		}
		return super.getItemViewType(position);
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

				ViewType viewType = ViewType.values()[getItemViewType(indexOfData)];
				ContactInfo contactInfo = getItem(indexOfData);

				/*
				 * Since sms contacts and dividers cannot have custom avatars, we simply skip these cases.
				 */
				if (viewType == ViewType.SECTION || !contactInfo.isOnhike())
				{
					continue;
				}

				if (ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()))
				{
					updateViewsRelatedToAvatar(view, getItem(indexOfData));
				}
			}
		}
	}
	
	public IconLoader getIconLoader()
	{
		return iconloader;
	}
	
	public void selectAllContacts(boolean select)
	{
		if(select)
		{
			ArrayList<List<ContactInfo>> listsToSelect = getOnHikeContactLists();
			listsToSelect.add(filteredHikeBdayContactList);
			listsToSelect.add(groupsList);
			listsToSelect.add(filteredHikeOtherFeaturesList);
			selectAllFromList(listsToSelect);
		}
		else
		{
			selectedPeople.clear();
		}
		notifyDataSetChanged();
	}

	public void preSelectContacts(HashSet<String> ... preSelectedMsisdnSets){
		int total = preSelectedMsisdnSets.length;
		for(int i=0;i<total;i++){
			HashSet<String> preSelectedSet = preSelectedMsisdnSets[i];
			if(preSelectedSet != null){
				for(String msisdn : preSelectedSet){
					if(msisdn != null && ContactManager.getInstance().getContact(msisdn) != null){
						selectedPeople.put(msisdn, ContactManager.getInstance().getContact(msisdn));
					}
				}
			}
		}
	}
	
	private void selectAllFromList(List<List<ContactInfo>> lists){
		for (List<ContactInfo> list : lists) {
			if(list!=null)
			{
				for(ContactInfo contactInfo: list)
				{
					if(contactInfo.isOnhike())
					{
						if (Utils.isFavToFriendsMigrationAllowed() && !OneToNConversationUtils.isOneToNConversation(contactInfo.getMsisdn()))
						{
							if (contactInfo.isMyOneWayFriend())
								selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
						}
						else
						{
							selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
						}
					}
				}
			}
		}
	}
	
	public void selectAllFromList(ArrayList<String> msisdns)
	{
		if (msisdns == null || msisdns.isEmpty())
		{
			return;
		}
		
		for (String msisdn : msisdns)
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, false);
			selectedPeople.put(msisdn, contactInfo);
		}
	}
	
	public void releaseResources()
	{
	}

	/**
	 * @param isSearchModeOn
	 *            the isSearchModeOn to set
	 */
	public void setSearchModeOn(boolean isSearchModeOn)
	{
		this.isSearchModeOn = isSearchModeOn;
	}

}
