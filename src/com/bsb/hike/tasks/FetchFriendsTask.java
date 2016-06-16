package com.bsb.hike.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.BirthdayUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
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

	private List<ContactInfo> groupsList;

	private List<ContactInfo> friendsList;

	private List<ContactInfo> hikeContactsList;

	private List<ContactInfo> smsContactsList;

	private List<ContactInfo> recentContactsList;

	private List<ContactInfo> recommendedContactsList;

	private List<ContactInfo> suggestedContactsList;

	private List<ContactInfo> groupsStealthList;

	private List<ContactInfo> friendsStealthList;

	private List<ContactInfo> hikeStealthContactsList;

	private List<ContactInfo> smsStealthContactsList;

	private List<ContactInfo> filteredGroupsList;

	private List<ContactInfo> filteredRecentsList;

	private List<ContactInfo> filteredFriendsList;

	private List<ContactInfo> filteredSmsContactsList;

	private List<ContactInfo> filteredHikeContactsList;

	private List<ContactInfo> filteredRecommendedContactsList;

	private List<ContactInfo> hikeBdayContactList;

	private List<ContactInfo> filteredHikeBdayContactList;

	private String existingGroupId;

	private String sendingMsisdn;

	private boolean fetchGroups = false;

	private boolean creatingOrEditingGroup = false;

	private Map<String, StatusMessage> lastStatusMessagesMap;

	private boolean fetchSmsContacts;

	private boolean fetchHikeContacts;

	private boolean fetchFavContacts;

	private boolean fetchRecents;

	private boolean fetchRecommendedContacts;

	private boolean filterHideList;

	boolean checkFavTypeInComparision;

	private boolean nativeSMSOn;

	private List<ContactInfo> filteredOtherFeaturesList;
	private List<ContactInfo> otherFeaturesList;

	private List<ContactInfo> contactsInfo;

	private List<String> composeExcludeList;

	private boolean showBdaySection;

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> recentContactsList, List<ContactInfo> friendsStealthList,
			List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList, List<ContactInfo> groupsStealthList,
			List<ContactInfo> recommendedContactsList, List<ContactInfo> filteredRecommendedContactsList, List<ContactInfo> filteredGroupsList,
			List<ContactInfo> filteredRecentsList, Map<String, ContactInfo> selectedPeople, String sendingMsisdn,
			boolean fetchGroups, String existingGroupId, boolean creatingOrEditingGroup, boolean fetchSmsContacts, boolean checkFavTypeInComparision, boolean fetchRecents,
			boolean fetchHikeContacts, boolean fetchFavContacts, boolean fetchRecommendedContacts,
			boolean filterHideList, List<String> composeExcludeList,
            boolean showBdaySection, List<ContactInfo> hikeBdayContactList, List<ContactInfo> filteredHikeBdayContactList)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;
		this.recentContactsList = recentContactsList;
		this.recommendedContactsList = recommendedContactsList;

		this.groupsStealthList = groupsStealthList;
		this.friendsStealthList = friendsStealthList;
		this.hikeStealthContactsList = hikeStealthContactsList;
		this.smsStealthContactsList = smsStealthContactsList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;
		this.filteredRecentsList = filteredRecentsList;
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

		this.nativeSMSOn = Utils.getSendSmsPref(context);

		this.composeExcludeList = composeExcludeList;

		this.showBdaySection = showBdaySection;

        this.hikeBdayContactList = hikeBdayContactList;
        this.filteredHikeBdayContactList = filteredHikeBdayContactList;
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
		List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
		removeExcludedParticipants(allContacts, composeExcludeList);
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

		if(showBdaySection)
		{
			hikeBdayContactList.clear();
            hikeBdayContactList.addAll(BirthdayUtils.getSortedBdayContactListFromSharedPref());
			filteredHikeBdayContactList.clear();
			BirthdayUtils.removeHiddenOrBlockedMsisdnFromContactInfoList(hikeBdayContactList);
			filteredHikeBdayContactList.addAll(hikeBdayContactList);
			/**
			 * Removing Birthday users from all contacts list
			 */
			allContacts.removeAll(filteredHikeBdayContactList);
		}
		else
		{
			hikeBdayContactList = new ArrayList<ContactInfo>();
			filteredHikeBdayContactList = new ArrayList<ContactInfo>();
		}

		if (fetchGroups)
		{
			groupTaskList = ContactManager.getInstance().getConversationGroupsAsContacts(true);
			removeSendingMsisdnAndStealthContacts(groupTaskList, groupsStealthList, true);
			removeExcludedParticipants(groupTaskList, composeExcludeList);
		}

		long queryTime = System.currentTimeMillis();

		NUXManager nm = NUXManager.getInstance();

		if (fetchRecents)
		{
			List<ContactInfo> convContacts = ContactManager.getInstance().getAllConversationContactsSorted(true, false);
			recentTaskList = new ArrayList<ContactInfo>();
			removeExcludedParticipants(convContacts, composeExcludeList);
			for (ContactInfo recentContact : convContacts)
			{
				String msisdn = recentContact.getMsisdn();
				boolean hideStealthMsisdn = StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive();
				boolean removeSendingMsisdn = (sendingMsisdn != null && sendingMsisdn.equals(msisdn));
				if (blockSet.contains(msisdn) || HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn) || myMsisdn.equals(msisdn) || hideStealthMsisdn || removeSendingMsisdn || !recentContact.isMyFriend())
				{
					continue;
				}
				recentTaskList.add(recentContact);
				if (recentTaskList.size() >= HikeConstants.MAX_RECENTS_TO_SHOW)
					break;
			}
			/**
			 * Removing Birthday users from resent contacts list
			 */
			recentTaskList.removeAll(filteredHikeBdayContactList);
		}

		Logger.d("TestQuery", "query time: " + (System.currentTimeMillis() - queryTime));

		friendTaskList = new ArrayList<ContactInfo>();
		hikeTaskList = new ArrayList<ContactInfo>();
		smsTaskList = new ArrayList<ContactInfo>();
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

		// Creating Friend list
		for (ContactInfo contactInfo : allContacts)
		{
			String msisdn = contactInfo.getMsisdn();
			// No need
			// If the contact is myself or is a bot.
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

			if (contactInfo.isMyFriend() && fetchFavContacts)
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

		// When adding members to an existing group, remove existing participants
		if (!TextUtils.isEmpty(existingGroupId))
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

		if(otherFeaturesList!=null)
		{
			filteredOtherFeaturesList.clear();
			filteredOtherFeaturesList.addAll(otherFeaturesList);
		}
		
		Logger.d("TestQuery", "total time: " + (System.currentTimeMillis() - startTime));
		return null;

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

	private void removeExcludedParticipants(List<ContactInfo> contactList, List<String> composeExcludeList)
	{
		if(contactList != null && contactList.size() > 0 && composeExcludeList != null && composeExcludeList.size() > 0)
		{
			Iterator<ContactInfo> i = contactList.iterator();
			while(i.hasNext())
			{
				ContactInfo contactInfo = i.next();
				if(composeExcludeList.contains(contactInfo.getId()))
				{
					i.remove();
				}
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

		friendsAdapter.initiateLastStatusMessagesMap(lastStatusMessagesMap);
		friendsList.addAll(friendTaskList);
		filteredFriendsList.addAll(friendTaskList);
		if (fetchGroups) {
			groupsList.addAll(groupTaskList);
			filteredGroupsList.addAll(groupTaskList);
		}
		if (fetchRecents) {
			recentContactsList.addAll(recentTaskList);
			filteredRecentsList.addAll(recentTaskList);
		}
		if (fetchHikeContacts) {
			hikeContactsList.addAll(hikeTaskList);
			filteredHikeContactsList.addAll(hikeTaskList);
		}
		if (fetchSmsContacts) {
			smsContactsList.addAll(smsTaskList);
			filteredSmsContactsList.addAll(smsTaskList);
		}
		if (fetchRecommendedContacts && recommendedContactsList != null) {
			recommendedContactsList.addAll(nuxRecommendedTaskList);
			filteredRecommendedContactsList.addAll(nuxRecommendedTaskList);
		}
		friendsAdapter.setListFetchedOnce(true);
		friendsAdapter.makeCompleteList(false, true);
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

		friendsList.clear();
		hikeContactsList.clear();
		smsContactsList.clear();
		filteredFriendsList.clear();
		filteredHikeContactsList.clear();
		filteredSmsContactsList.clear();
	}
}
