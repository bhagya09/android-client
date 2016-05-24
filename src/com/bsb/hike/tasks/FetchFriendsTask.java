package com.bsb.hike.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FetchFriendsTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private FriendsAdapter friendsAdapter;

	private Map<String, ContactInfo> selectedPeople;

	private List<ContactInfo> groupTaskList;

	private List<ContactInfo> friendTaskList;

	private List<ContactInfo> nuxRecommendedTaskList;

	private List<ContactInfo> nuxHideTaskList;

	private List<ContactInfo> hikeTaskList;

	private List<ContactInfo> smsTaskList;

	private List<ContactInfo> recentTaskList;

	private List<ContactInfo> recentlyJoinedTaskList;

	private List<ContactInfo> groupsList;

	private List<ContactInfo> friendsList;

	private List<ContactInfo> hikeContactsList;

	private List<ContactInfo> smsContactsList;

	private List<ContactInfo> recentContactsList;

	private List<ContactInfo> recommendedContactsList;

	private List<ContactInfo> suggestedContactsList;

	private List<ContactInfo> recentlyJoinedContactsList;

	private List<ContactInfo> groupsStealthList;

	private List<ContactInfo> friendsStealthList;

	private List<ContactInfo> hikeStealthContactsList;

	private List<ContactInfo> smsStealthContactsList;

	private List<ContactInfo> recentsStealthList;

	private List<ContactInfo> filteredGroupsList;

	private List<ContactInfo> filteredRecentsList;

	private List<ContactInfo> filteredRecentlyJoinedList;

	private List<ContactInfo> filteredFriendsList;

	private List<ContactInfo> filteredSmsContactsList;

	private List<ContactInfo> filteredHikeContactsList;

	private List<ContactInfo> filteredRecommendedContactsList;

	private List<ContactInfo> filteredSuggestedContactsList;

	private List<BotInfo> microappShowcaseList;

	private List<BotInfo> filteredMicroAppShowcaseList;

	private String existingGroupId;

	private String sendingMsisdn;

	private boolean fetchGroups = false;

	private boolean creatingOrEditingGroup = false;

	private Map<String, StatusMessage> lastStatusMessagesMap;

	private boolean fetchSmsContacts;

	private boolean fetchHikeContacts;

	private boolean fetchFavContacts;

	private boolean fetchRecents;

	private boolean fetchRecentlyJoined;

	private boolean fetchRecommendedContacts;

	private boolean filterHideList;

	boolean checkFavTypeInComparision;

	private boolean nativeSMSOn;

	private boolean showDefaultEmptyList;

	private boolean showMicroappShowcase;
	private List<ContactInfo> filteredOtherFeaturesList;
	private List<ContactInfo> otherFeaturesList;

	private boolean showFilteredContacts;

	private String msisdnList;

	private List<ContactInfo> contactsInfo;

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> recentContactsList, List<ContactInfo> recentlyJoinedHikeContactsList, List<ContactInfo> friendsStealthList,
			List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> recentsStealthList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> suggestedContactsList,
			List<ContactInfo> filteredSuggestedContactsList, boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents, boolean fetchRecentlyJoined,
			boolean showDefaultEmptyList, boolean fetchHikeContacts, boolean fetchFavContacts, boolean showFilteredContacts, String msisdnList)
	{
		this(friendsAdapter, context, friendsList, hikeContactsList, smsContactsList, recentContactsList, recentlyJoinedHikeContactsList, friendsStealthList,
				hikeStealthContactsList, smsStealthContactsList, recentsStealthList, filteredFriendsList, filteredHikeContactsList, filteredSmsContactsList, null, null, null,
				null, null, null, null, null, null, false, null, false, fetchSmsContacts, checkFavTypeInComparision, fetchRecents, fetchRecentlyJoined, showDefaultEmptyList,
				fetchHikeContacts, fetchFavContacts, false, false, null, null, false, showFilteredContacts, msisdnList, suggestedContactsList, filteredSuggestedContactsList);
	}

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> recentContactsList, List<ContactInfo> recentlyJoinedHikeContactsList, List<ContactInfo> friendsStealthList,
			List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> recentsStealthList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList, List<ContactInfo> groupsStealthList,
			List<ContactInfo> recommendedContactsList, List<ContactInfo> filteredRecommendedContactsList, List<ContactInfo> filteredGroupsList,
			List<ContactInfo> filteredRecentsList, List<ContactInfo> filteredRecentlyJoinedContactsList, Map<String, ContactInfo> selectedPeople, String sendingMsisdn,
			boolean fetchGroups, String existingGroupId, boolean creatingOrEditingGrou, boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents,
			boolean fetchRecentlyJoined, boolean showDefaultEmptyList, boolean fetchHikeContacts, boolean fetchFavContacts, boolean fetchRecommendedContacts,
			boolean filterHideList, List<BotInfo> microappShowcaseList, List<BotInfo> filteredMicroAppShowcaseList, boolean showMicroappShowcase, boolean showFilteredContacts,
			String msisdnList, List<ContactInfo> suggestedContactsList, List<ContactInfo> filteredSuggestedContactsList)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;
		this.recentContactsList = recentContactsList;
		this.recentlyJoinedContactsList = recentlyJoinedHikeContactsList;
		this.recommendedContactsList = recommendedContactsList;
		this.suggestedContactsList = suggestedContactsList;

		this.groupsStealthList = groupsStealthList;
		this.friendsStealthList = friendsStealthList;
		this.hikeStealthContactsList = hikeStealthContactsList;
		this.smsStealthContactsList = smsStealthContactsList;
		this.recentsStealthList = recentsStealthList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;
		this.filteredRecentsList = filteredRecentsList;
		this.filteredRecentlyJoinedList = filteredRecentlyJoinedContactsList;
		this.filteredRecommendedContactsList = filteredRecommendedContactsList;
		this.filteredSuggestedContactsList = filteredSuggestedContactsList;

		this.selectedPeople = selectedPeople;
		this.fetchRecommendedContacts = fetchRecommendedContacts;
		this.filterHideList = filterHideList;

		this.fetchGroups = fetchGroups;
		this.existingGroupId = existingGroupId;
		this.sendingMsisdn = sendingMsisdn;

		this.creatingOrEditingGroup = creatingOrEditingGroup;

		this.fetchSmsContacts = fetchSmsContacts;
		this.fetchHikeContacts = fetchHikeContacts;
		this.fetchFavContacts = fetchFavContacts;
		this.checkFavTypeInComparision = checkFavTypeInComparision;
		this.fetchRecents = fetchRecents;
		this.fetchRecentlyJoined = fetchRecentlyJoined;

		this.showDefaultEmptyList = showDefaultEmptyList;
		this.showMicroappShowcase = showMicroappShowcase;

		this.nativeSMSOn = Utils.getSendSmsPref(context);

		this.microappShowcaseList = microappShowcaseList;
		this.filteredMicroAppShowcaseList = filteredMicroAppShowcaseList;

		this.showFilteredContacts = showFilteredContacts;
		this.msisdnList = msisdnList;
	}

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> recentContactsList, List<ContactInfo> recentlyJoinedHikeContactsList, List<ContactInfo> friendsStealthList,
			List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> recentsStealthList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList, List<ContactInfo> groupsStealthList,
			List<ContactInfo> recommendedContactsList, List<ContactInfo> filteredRecommendedContactsList, List<ContactInfo> filteredGroupsList,
			List<ContactInfo> filteredRecentsList, List<ContactInfo> filteredRecentlyJoinedContactsList, Map<String, ContactInfo> selectedPeople, String sendingMsisdn,
			boolean fetchGroups, String existingGroupId, boolean creatingOrEditingGrou, boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents,
			boolean fetchRecentlyJoined, boolean showDefaultEmptyList, boolean fetchHikeContacts, boolean fetchFavContacts, boolean fetchRecommendedContacts,
			boolean filterHideList, List<BotInfo> microappShowcaseList, List<BotInfo> filteredMicroAppShowcaseList, boolean showMicroappShowcase)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;
		this.recentContactsList = recentContactsList;
		this.recentlyJoinedContactsList = recentlyJoinedHikeContactsList;
		this.recommendedContactsList = recommendedContactsList;

		this.groupsStealthList = groupsStealthList;
		this.friendsStealthList = friendsStealthList;
		this.hikeStealthContactsList = hikeStealthContactsList;
		this.smsStealthContactsList = smsStealthContactsList;
		this.recentsStealthList = recentsStealthList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;
		this.filteredRecentsList = filteredRecentsList;
		this.filteredRecentlyJoinedList = filteredRecentlyJoinedContactsList;
		this.filteredRecommendedContactsList = filteredRecommendedContactsList;

		this.selectedPeople = selectedPeople;
		this.fetchRecommendedContacts = fetchRecommendedContacts;
		this.filterHideList = filterHideList;

		this.fetchGroups = fetchGroups;
		this.existingGroupId = existingGroupId;
		this.sendingMsisdn = sendingMsisdn;

		this.creatingOrEditingGroup = creatingOrEditingGroup;

		this.fetchSmsContacts = fetchSmsContacts;
		this.fetchHikeContacts = fetchHikeContacts;
		this.fetchFavContacts = fetchFavContacts;
		this.checkFavTypeInComparision = checkFavTypeInComparision;
		this.fetchRecents = fetchRecents;
		this.fetchRecentlyJoined = fetchRecentlyJoined;

		this.showDefaultEmptyList = showDefaultEmptyList;
		this.showMicroappShowcase = showMicroappShowcase;

		this.nativeSMSOn = Utils.getSendSmsPref(context);

		this.microappShowcaseList = microappShowcaseList;
		this.filteredMicroAppShowcaseList = filteredMicroAppShowcaseList;

	}

	public void addOtherFeaturesList(List<ContactInfo> otherFeaturesList, List<ContactInfo> filteredOtherFeaturesList)
	{
		this.otherFeaturesList = otherFeaturesList;
		this.filteredOtherFeaturesList = filteredOtherFeaturesList;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		long startTime = System.currentTimeMillis();
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

		List<ContactInfo> bdayContactList = null;

		if(Utils.isBDayInNewChatEnabled())
		{
			bdayContactList = ChatHeadUtils.getSortedBdayContactListFromSharedPref();
		}
		else
		{
			bdayContactList = new ArrayList<ContactInfo>();
		}

		if (showFilteredContacts && !TextUtils.isEmpty(msisdnList))
		{
			contactsInfo = ContactManager.getInstance().getContactInfoListForMsisdnFilter(msisdnList);
			/**
			 * Removing Birthday users from contacts list
			 */
			contactsInfo.removeAll(bdayContactList);
			suggestedContactsList.addAll(contactsInfo);
			filteredSuggestedContactsList.addAll(contactsInfo);
		}

		boolean removeExistingParticipants = !TextUtils.isEmpty(existingGroupId);
		if (fetchGroups)
		{
			groupTaskList = ContactManager.getInstance().getConversationGroupsAsContacts(true);
			removeSendingMsisdnAndStealthContacts(groupTaskList, groupsStealthList, true);
		}

		long queryTime = System.currentTimeMillis();
		List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();

		NUXManager nm = NUXManager.getInstance();

		if (fetchRecents)
		{
			List<ContactInfo> convContacts = ContactManager.getInstance().getAllConversationContactsSorted(true, false);
			recentTaskList = new ArrayList<ContactInfo>();

			for (ContactInfo recentContact : convContacts)
			{
				if (recentTaskList.size() >= HikeConstants.MAX_RECENTS_TO_SHOW)
					break;
				String msisdn = recentContact.getMsisdn();
				boolean hideStealthMsisdn = StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive();
				boolean removeSendingMsisdn = (sendingMsisdn != null && sendingMsisdn.equals(msisdn));
				if (blockSet.contains(msisdn) || HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn) || myMsisdn.equals(msisdn) || hideStealthMsisdn || removeSendingMsisdn)
				{
					continue;
				}
				recentTaskList.add(recentContact);
			}
			/**
			 * Removing Birthday users from resent contacts list
			 */
			recentTaskList.removeAll(bdayContactList);
		}

		Logger.d("TestQuery", "query time: " + (System.currentTimeMillis() - queryTime));

		friendTaskList = new ArrayList<ContactInfo>();
		hikeTaskList = new ArrayList<ContactInfo>();
		smsTaskList = new ArrayList<ContactInfo>();
		recentlyJoinedTaskList = new ArrayList<ContactInfo>();
		nuxRecommendedTaskList = new ArrayList<ContactInfo>();
		nuxHideTaskList = new ArrayList<ContactInfo>();

		ContactManager cm = ContactManager.getInstance();

		for (String stealthMsisdn : StealthModeManager.getInstance().getStealthMsisdns())
		{
			nuxHideTaskList.add(cm.getContact(stealthMsisdn));
		}

		boolean separateOrHideNuxContacts = nm.getCurrentState() != NUXConstants.COMPLETED && nm.getCurrentState() != NUXConstants.NUX_KILLED
				&& (filterHideList || fetchRecommendedContacts);

		if (separateOrHideNuxContacts)
		{

			Set<String> mmSet = nm.getNuxSelectFriendsPojo().getRecoList();

			if (mmSet != null && fetchRecommendedContacts)
			{
				mmSet.removeAll(blockSet);
				for (String msisdn : mmSet)
				{
					ContactInfo nuxCI = cm.getContact(msisdn);
					if (!TextUtils.isEmpty(msisdn) && !(nuxCI == null) && nuxCI.getName() != null)
						nuxRecommendedTaskList.add(cm.getContact(msisdn));
				}
				allContacts.removeAll(nuxRecommendedTaskList);

			}

			ArrayList<String> mmList = nm.getNuxSelectFriendsPojo().getHideList();
			if (mmList != null && filterHideList)
			{
				for (String msisdn : mmList)
				{
					if (!TextUtils.isEmpty(msisdn) && !(cm.getContact(msisdn) == null))
						nuxHideTaskList.add(cm.getContact(msisdn));
				}
				nuxRecommendedTaskList.removeAll(nuxHideTaskList);
				allContacts.removeAll(nuxHideTaskList);
			}

		}

		long iterationTime = System.currentTimeMillis();
		/**
		 * Removing Birthday users from all contacts list
		 */
		allContacts.removeAll(bdayContactList);
		for (ContactInfo contactInfo : allContacts)
		{
			String msisdn = contactInfo.getMsisdn();
			if (msisdn.equals(myMsisdn) || HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn))
			{
				continue;
			}
			if (blockSet.contains(msisdn) || (sendingMsisdn != null && sendingMsisdn.equals(msisdn)))
			{
				continue;
			}
			if (fetchRecents && recentTaskList.contains(contactInfo))
			{
				continue;
			}
			FavoriteType favoriteType = contactInfo.getFavoriteType();

			addToRecentlyJoinedIfNeeded(contactInfo);
			if (shouldAddToFavorites(favoriteType) && fetchFavContacts)
			{
				friendTaskList.add(contactInfo);
			}
			else
			{
				if (null != contactInfo.getName())
				{
					if (contactInfo.isOnhike())
					{
						hikeTaskList.add(contactInfo);
					}
					else if (fetchSmsContacts && shouldShowSmsContact(msisdn))
					{
						smsTaskList.add(contactInfo);
					}
				}
			}
		}

		Logger.d("TestQuery", "Iteration time: " + (System.currentTimeMillis() - iterationTime));

		long sortTime = System.currentTimeMillis();
		Collections.sort(friendTaskList, checkFavTypeInComparision ? ContactInfo.lastSeenTimeComparator : ContactInfo.lastSeenTimeComparatorWithoutFav);
		Logger.d("TestQuery", "Sorting time: " + (System.currentTimeMillis() - sortTime));

		Collections.sort(recentlyJoinedTaskList, new Comparator<ContactInfo>()
		{
			@Override
			public int compare(ContactInfo lhs, ContactInfo rhs)
			{
				return (lhs.getHikeJoinTime() < rhs.getHikeJoinTime()) ? 1 : -1;
			}
		});

		/*
             *
             */
		if (recentlyJoinedTaskList.size() > HikeConstants.MAX_RECENTLY_JOINED_HIKE_TO_SHOW)
		{
			recentlyJoinedTaskList = recentlyJoinedTaskList.subList(0, HikeConstants.MAX_RECENTLY_JOINED_HIKE_TO_SHOW);
		}

		hikeTaskList.removeAll(recentlyJoinedTaskList);
		friendTaskList.removeAll(recentlyJoinedTaskList);

		if (removeExistingParticipants)
		{
			List<PairModified<GroupParticipant, String>> groupParticipantsList = ContactManager.getInstance().getGroupParticipants(existingGroupId, true, false);
			Map<String, PairModified<GroupParticipant, String>> groupParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
			for (PairModified<GroupParticipant, String> grpParticipant : groupParticipantsList)
			{
				String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
				groupParticipants.put(msisdn, grpParticipant);
			}

			removeContactsFromList(friendTaskList, groupParticipants);
			if (fetchHikeContacts)
			{
				removeContactsFromList(hikeTaskList, groupParticipants);
			}
			if (fetchSmsContacts)
			{
				removeContactsFromList(smsTaskList, groupParticipants);
			}
			if(fetchRecentlyJoined)
			{
				removeContactsFromList(recentlyJoinedTaskList, groupParticipants);
			}

			for (PairModified<GroupParticipant, String> groupParticipant : groupParticipants.values())
			{
				ContactInfo contactInfo = groupParticipant.getFirst().getContactInfo();

				selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
			}
		}
		addToStealthList(friendTaskList, friendsStealthList, false);
		if (fetchHikeContacts)
		{
			addToStealthList(hikeTaskList, hikeStealthContactsList, false);
		}
		if (fetchSmsContacts)
		{
			addToStealthList(smsTaskList, smsStealthContactsList, false);
		}

		lastStatusMessagesMap = HikeConversationsDatabase.getInstance().getLastStatusMessages(false, HikeConstants.STATUS_TYPE_LIST_TO_FETCH, friendTaskList);

		if (showMicroappShowcase && microappShowcaseList != null)
		{
			microappShowcaseList.clear();
			microappShowcaseList.addAll(HikeContentDatabase.getInstance().getDiscoveryBotInfoList());

			if (filteredMicroAppShowcaseList != null)
			{
				filteredMicroAppShowcaseList.clear();
				filteredMicroAppShowcaseList.addAll(microappShowcaseList);
			}
		}

		if(otherFeaturesList!=null)
		{
			filteredOtherFeaturesList.clear();
			filteredOtherFeaturesList.addAll(otherFeaturesList);
		}
		
		Logger.d("TestQuery", "total time: " + (System.currentTimeMillis() - startTime));
		return null;

	}

	private void addToRecentlyJoinedIfNeeded(ContactInfo contactInfo)
	{
		if (fetchRecentlyJoined && contactInfo.isOnhike() && !contactInfo.isUnknownContact())
		{
			if (!StealthModeManager.getInstance().isActive() && StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				return;
			}
			long hikeJoinTime = contactInfo.getHikeJoinTime();
			if (hikeJoinTime > 0)
			{
				recentlyJoinedTaskList.add(contactInfo);
			}
		}
	}

	private boolean shouldAddToFavorites(FavoriteType favoriteType)
	{
		return favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT
				|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED;
	}

	private boolean shouldShowSmsContact(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn))
		{
			return false;
		}

		if (!nativeSMSOn)
		{
			return msisdn.startsWith(HikeConstants.INDIA_COUNTRY_CODE);
		}

		return true;
	}

	private void addToStealthList(List<ContactInfo> contactList, List<ContactInfo> stealthList, boolean isGroupTask)
	{
		if (creatingOrEditingGroup)
		{
			return;
		}

		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			/*
			 * if case of group contactInfo.getId() will retrun groupId, which is treated as msisdn for groups.
			 */
			String msisdn = isGroupTask ? contactInfo.getId() : contactInfo.getMsisdn();
			if (StealthModeManager.getInstance().isStealthMsisdn(msisdn))
			{
				stealthList.add(contactInfo);

				/*
				 * If stealth mode is currently off, we should remove these contacts from the list.
				 */
				if (!StealthModeManager.getInstance().isActive())
				{
					iter.remove();
				}
			}
		}
	}

	private void removeContactsFromList(List<ContactInfo> contactList, Map<String, PairModified<GroupParticipant, String>> groupParticipants)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			if (groupParticipants.containsKey(contactInfo.getMsisdn()))
			{
				iter.remove();
			}
		}
	}

	private void removeSendingMsisdnAndStealthContacts(List<ContactInfo> contactList, List<ContactInfo> stealthList, boolean isGroupTask)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			/*
			 * if case of group contactInfo.getId() will retrun groupId, which is treated as msisdn for groups.
			 */
			String msisdn = isGroupTask ? contactInfo.getId() : contactInfo.getMsisdn();
			if (StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !creatingOrEditingGroup)
			{
				stealthList.add(contactInfo);

				/*
				 * If stealth mode is currently off, we should remove these contacts from the list.
				 */
				if (!StealthModeManager.getInstance().isActive())
				{
					iter.remove();
				}
			}
			if (sendingMsisdn != null && sendingMsisdn.equals(msisdn))
			{
				iter.remove();
			}
		}
	}

	@Override
	protected void onPostExecute(Void result)
	{
		/*
		 * Clearing all the lists initially to ensure we remove any existing contacts in the list that might be there because of the 'ai' packet.
		 */
		clearAllLists();

		if (fetchGroups)
		{
			groupsList.addAll(groupTaskList);
		}
		if (fetchRecents)
		{
			recentContactsList.addAll(recentTaskList);
		}
		friendsAdapter.initiateLastStatusMessagesMap(lastStatusMessagesMap);
		friendsList.addAll(friendTaskList);

		if (fetchHikeContacts)
		{
			hikeContactsList.addAll(hikeTaskList);
		}
		if (fetchRecents)
		{
			filteredRecentsList.addAll(recentTaskList);
		}

		if (fetchSmsContacts)
		{
			smsContactsList.addAll(smsTaskList);
		}

		if (fetchGroups)
		{
			filteredGroupsList.addAll(groupTaskList);
		}
		filteredFriendsList.addAll(friendTaskList);
		if (fetchHikeContacts)
		{
			filteredHikeContactsList.addAll(hikeTaskList);
		}
		if (fetchSmsContacts)
		{
			filteredSmsContactsList.addAll(smsTaskList);
		}

		if (fetchRecentlyJoined)
		{
			recentlyJoinedContactsList.addAll(recentlyJoinedTaskList);
			filteredRecentlyJoinedList.addAll(recentlyJoinedTaskList);
		}
		if (fetchRecommendedContacts)
		{
			if (recommendedContactsList != null)
			{

				recommendedContactsList.addAll(nuxRecommendedTaskList);
				filteredRecommendedContactsList.addAll(nuxRecommendedTaskList);
			}
		}
		friendsAdapter.setListFetchedOnce(true);

		if (microappShowcaseList != null)
		{
			friendsAdapter.setOriginalMicroAppsCount(microappShowcaseList.size());
		}

		// We dont need to show contacts in NUX Invite screen
		if (showDefaultEmptyList)
		{
			friendsAdapter.setEmptyView();
		}
		else
		{
			if (showFilteredContacts)
			{
				friendsAdapter.makeCompleteList(true, false);
			}
			else
			{
				friendsAdapter.makeCompleteList(true, true);
			}
		}
	}

	private void clearAllLists()
	{

		if (fetchRecommendedContacts)
		{
			if (recommendedContactsList != null)
				recommendedContactsList.clear();
		}
		if (fetchGroups)
		{
			groupsList.clear();
			filteredGroupsList.clear();
		}

		if (fetchRecents)
		{
			recentContactsList.clear();
			filteredRecentsList.clear();
		}

		if (fetchRecentlyJoined)
		{
			recentlyJoinedContactsList.clear();
			filteredRecentlyJoinedList.clear();
		}

		friendsList.clear();
		hikeContactsList.clear();
		smsContactsList.clear();
		filteredFriendsList.clear();
		filteredHikeContactsList.clear();
		filteredSmsContactsList.clear();
	}
}