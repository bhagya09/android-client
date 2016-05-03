package com.bsb.hike.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.HikeFeatureInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.FetchFriendsTask;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.LastSeenComparator;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;
import com.bsb.hike.view.PinnedSectionListView.PinnedSectionListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bsb.hike.utils.HikeAnalyticsEvent;

public class FriendsAdapter extends BaseAdapter implements OnClickListener, PinnedSectionListAdapter
{

	protected final ArrayList<ContactInfo> filteredHikeOtherFeaturesList;

	public static interface FriendsListFetchedCallback
	{
		public void listFetched();

		public void completeListFetched();
	}

	public static final int FRIEND_INDEX = 0;

	public static final int HIKE_INDEX = 1;

	public static final int SMS_INDEX = 2;

	public static final String EXTRA_ID = "-910";

	public static final String SECTION_ID = "-911";

	public static final String EMPTY_ID = "-912";

	public static final String REMOVE_SUGGESTIONS_ID = "-913";
	
	public static final String HIKE_APPS_ID = "-914";

	public static final String INVITE_MSISDN = "-123";

	public static final String GROUP_MSISDN = "-124";

	public static final String FRIEND_PHONE_NUM = "-125";

	public static final String CONTACT_PHONE_NUM = "--126";
	
	public static final String CONTACT_SMS_NUM = "--127";

	public static final String RECENT_PHONE_NUM = "-128";
	
	public static final String RECENTLY_JOINED = "-129";
	
	public static final String RECOMMENDED = "-130";
	
	public static final String APPS_ON_HIKE = "-131";
	
	// phoneNum for scrolling Microapps showcase
	public static final String HIKE_APPS_NUM = "-131";
	
	// msisdn for scrolling Microapps showcase
	public static final String HIKE_APPS_MSISDN = "-132";

	public static final String HIKE_FEATURES_ID = "-134";

	public static final String HIKE_FEATURES_TIMELINE_ID = "-135";

	public static final String CONTACT_FILTERED_NUM = "--133";

	/*stores the regex for matching number during search*/
	public static Pattern numberPattern;

	public enum ViewType
	{
		SECTION, FRIEND, NOT_FRIEND_HIKE, NOT_FRIEND_SMS, FRIEND_REQUEST, EXTRA, EMPTY, FTUE_CONTACT, REMOVE_SUGGESTIONS, NEW_CONTACT, RECOMMENDED, HIKE_APPS, HIKE_FEATURES
	}

	private LayoutInflater layoutInflater;

	protected List<ContactInfo> completeList;

	protected List<ContactInfo> friendsList;

	protected List<ContactInfo> hikeContactsList;

	protected List<ContactInfo> smsContactsList;

    protected List<ContactInfo> suggestedContactsList;
	
	protected List<ContactInfo> recentContactsList;
	
	protected List<ContactInfo> recentlyJoinedHikeContactsList;

	protected List<ContactInfo> friendsStealthList;

	protected List<ContactInfo> hikeStealthContactsList;

	protected List<ContactInfo> smsStealthContactsList;

	protected List<ContactInfo> recentStealthContactsList;
	
	protected List<ContactInfo> filteredFriendsList;

	protected List<ContactInfo> filteredHikeContactsList;

	protected List<ContactInfo> filteredSmsContactsList;

    protected List<ContactInfo> filteredSuggestedContactsList;

	protected List<ContactInfo> filteredRecentlyJoinedHikeContactsList;

	protected List<ContactInfo> groupsList;

	protected List<ContactInfo> groupsStealthList;

	protected List<ContactInfo> filteredGroupsList;

	protected List<ContactInfo> filteredRecentsList;

	protected List<ContactInfo> nuxRecommendedList;

	protected List<ContactInfo> nuxFilteredRecoList;

	protected List<BotInfo> microappShowcaseList;

	protected List<BotInfo> filteredmicroAppShowcaseList;

	protected List<ContactInfo> hikeOtherFeaturesList;

	protected int originalMicroAppCount = 0;

	protected Context context;

	private ContactInfo friendsSection;

	private ContactInfo hikeContactsSection;

	private ContactInfo smsContactsSection;

    private ContactInfo suggestedContactsSection;

    private ContactInfo inviteExtraItem;

	private ContactInfo groupExtraItem;

	private ContactFilter contactFilter;

	protected String queryText;

	private boolean lastSeenPref;

	protected boolean showSMSContacts;

	private IconLoader iconloader;

	private boolean listFetchedOnce;

	private int mIconImageSize;

	protected View emptyView, loadingView;

	protected ListView listView;

	private boolean isFiltered;
	
	private Map<String, StatusMessage> lastStatusMessagesMap;

	protected FriendsListFetchedCallback friendsListFetchedCallback;

	protected LastSeenComparator lastSeenComparator;

	protected Map<String, Integer> contactSpanStartIndexes;

    /*
     * Variables added for showing friends list based on a given csv
     */
    private boolean showFilteredContacts;

    private String msisdnList;

	public FriendsAdapter(Context context, ListView listView, FriendsListFetchedCallback friendsListFetchedCallback, LastSeenComparator lastSeenComparator)
	{
		this.listView = listView;
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.iconloader = new IconLoader(context, mIconImageSize);
		this.iconloader.setDefaultAvatarIfNoCustomIcon(true);
		this.iconloader.setImageFadeIn(false);
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
		this.contactFilter = new ContactFilter();
		this.lastSeenPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		/*
		 * Now we never show sms contacts section in people screen.
		 */
		//this.showSMSContacts = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.FREE_SMS_PREF, true) || Utils.getSendSmsPref(context);
		this.showSMSContacts = false;
		this.friendsListFetchedCallback = friendsListFetchedCallback;
		this.lastSeenComparator = lastSeenComparator;

		completeList = new ArrayList<ContactInfo>();

		friendsList = new ArrayList<ContactInfo>(0);
		hikeContactsList = new ArrayList<ContactInfo>(0);
		smsContactsList = new ArrayList<ContactInfo>(0);
        suggestedContactsList = new ArrayList<ContactInfo>(0);
		recentlyJoinedHikeContactsList = new ArrayList<ContactInfo>(0);
		nuxRecommendedList = new ArrayList<ContactInfo>(0);
		microappShowcaseList = new ArrayList<BotInfo>(0);
		filteredmicroAppShowcaseList = new ArrayList<BotInfo>(0);
		hikeOtherFeaturesList = new ArrayList<ContactInfo>(0);
		filteredHikeOtherFeaturesList = new ArrayList<ContactInfo>(0);

		friendsStealthList = new ArrayList<ContactInfo>(0);
		hikeStealthContactsList = new ArrayList<ContactInfo>(0);
		smsStealthContactsList = new ArrayList<ContactInfo>(0);

		filteredFriendsList = new ArrayList<ContactInfo>(0);
		filteredHikeContactsList = new ArrayList<ContactInfo>(0);
		filteredSmsContactsList = new ArrayList<ContactInfo>(0);
        filteredSuggestedContactsList = new ArrayList<ContactInfo>(0);
		filteredRecentlyJoinedHikeContactsList = new ArrayList<ContactInfo>(0);
		nuxFilteredRecoList = new ArrayList<ContactInfo>(0);
		lastStatusMessagesMap = new HashMap<String, StatusMessage>();

		listFetchedOnce = false;

		contactSpanStartIndexes = new HashMap<String, Integer>();
		
		/* Regex Explanation - number can start with '+', then any character between [0-9] one or more time and
	 	any character among them [-, ., space, slash ] only once
		if this pattern match then ignore all the hyphen, dot, space, slash 
		removed backtracking for numbers as it can lead to backtracking explosion for large string 
		reference : http://www.regular-expressions.info/catastrophic.html
		compiling the pattern once to avoid redundant recompiling */
		numberPattern = Pattern.compile("^\\+?((?>[0-9]+)[-.\\s/]?)*");
	}

    public FriendsAdapter(Context context, ListView listView, FriendsListFetchedCallback friendsListFetchedCallback, LastSeenComparator lastSeenComparator,boolean showFilteredContacts,String msisdnList)
    {
        this(context,listView,friendsListFetchedCallback,lastSeenComparator);
        /*
         * Initializing instance variables for retrieval of contacts based on given msisdns and phone nos list
         */
        this.showFilteredContacts = showFilteredContacts;
        this.msisdnList = msisdnList;
    }


	public void executeFetchTask()
	{
		setLoadingView();

        FetchFriendsTask fetchFriendsTask;

        msisdnList = showFilteredContacts ? (msisdnList) : "";

        fetchFriendsTask = new FetchFriendsTask(this, context, friendsList, hikeContactsList, smsContactsList, recentContactsList, recentlyJoinedHikeContactsList,friendsStealthList, hikeStealthContactsList,
                smsStealthContactsList, recentStealthContactsList, filteredFriendsList, filteredHikeContactsList, filteredSmsContactsList,suggestedContactsList,filteredSuggestedContactsList, false, true, false, false, false,true,true,showFilteredContacts,msisdnList);
        Utils.executeAsyncTask(fetchFriendsTask);
	}

	public void setListFetchedOnce(boolean b)
	{
		listFetchedOnce = b;
	}

	public void onQueryChanged(String s)
	{
		queryText = s;
		Matcher matcher = numberPattern.matcher(queryText);
		if(matcher.matches())
		{
			queryText = queryText.replaceAll("[-.\\s /]", "");
		}
		contactFilter.filter(queryText);
	}

	public void removeFilter()
	{
		onQueryChanged("");
	}

	private class ContactFilter extends Filter
	{
		List<BotInfo> filteredBotList = new ArrayList<BotInfo>(0);
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults results = new FilterResults();
			contactSpanStartIndexes.clear();
			if (!TextUtils.isEmpty(constraint))
			{
            	String textToBeFiltered = constraint.toString().toLowerCase().trim();

				List<ContactInfo> filteredFriendsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredHikeContactsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredSmsContactsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredGroupList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredRecentsList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredRecentlyJoinedList = new ArrayList<ContactInfo>();
				List<ContactInfo> filteredFeaturesList = new ArrayList<ContactInfo>();
                List<ContactInfo> filteredSuggestedContactsList = new ArrayList<ContactInfo>();

				List<ContactInfo> nuxFilteredRecoList = new ArrayList<ContactInfo>();

				filterList(friendsList, filteredFriendsList, textToBeFiltered);
				filterList(hikeContactsList, filteredHikeContactsList, textToBeFiltered);
				filterList(smsContactsList, filteredSmsContactsList, textToBeFiltered);
                filterList(suggestedContactsList, filteredSuggestedContactsList, textToBeFiltered);


                if (groupsList != null && !groupsList.isEmpty())
				{
					filterList(groupsList, filteredGroupList, textToBeFiltered);
				}
				
				if (recentContactsList != null && !recentContactsList.isEmpty())
				{
					filterList(recentContactsList, filteredRecentsList, textToBeFiltered);
				}

				if(recentlyJoinedHikeContactsList != null && !recentlyJoinedHikeContactsList.isEmpty())
				{
					filterList(recentlyJoinedHikeContactsList, filteredRecentlyJoinedList, textToBeFiltered);
				}

                if(suggestedContactsList != null && !suggestedContactsList.isEmpty())
                {
                    filteredSuggestedContactsList.clear();
                    filterList(suggestedContactsList, filteredSuggestedContactsList, textToBeFiltered);
                }
				
				if(nuxRecommendedList != null && !nuxRecommendedList.isEmpty())
				{

					filterList(nuxRecommendedList, nuxFilteredRecoList, textToBeFiltered);
				}

				if (hikeOtherFeaturesList != null && !hikeOtherFeaturesList.isEmpty())
				{
					filterList(hikeOtherFeaturesList, filteredFeaturesList, textToBeFiltered);
				}

				filterBots(textToBeFiltered);
				List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>();
				resultList.add(filteredFeaturesList);
				resultList.add(filteredFriendsList);
				resultList.add(filteredHikeContactsList);
				resultList.add(filteredSmsContactsList);
				resultList.add(filteredGroupList);
				resultList.add(filteredRecentsList);
				resultList.add(filteredRecentlyJoinedList);
				resultList.add(nuxFilteredRecoList);
                resultList.add(filteredSuggestedContactsList);

				results.values = resultList;
				isFiltered = true;
			}
			else
			{
				results.values = makeOriginalList();
				this.filteredBotList.clear();
				this.filteredBotList.addAll(microappShowcaseList);
				isFiltered = false;
			}
			results.count = 1;
			
			return results;
		}

		private void filterBots(String textToBeFiltered)
		{
			if (filteredBotList != null)
			{
				filteredBotList.clear();
			}
			
			else
			{
				filteredBotList = new ArrayList<BotInfo>(0);
			}
			
			for (BotInfo mBotInfo : microappShowcaseList)
			{
				String name = mBotInfo.getConversationName();
				
				boolean found = false;
				int startIndex = 0;
				if(name!=null){
				name = name.toLowerCase();
				if (name.startsWith(textToBeFiltered))
				{
					found = true;
				}
				else if (name.contains(" " + textToBeFiltered))
				{
					found = true;
					startIndex = name.indexOf(" " + textToBeFiltered) + 1;
				}
				}
				if(found)
				{
					contactSpanStartIndexes.put(mBotInfo.getMsisdn(), startIndex);
					filteredBotList.add(mBotInfo);
				}
			}
		}

		private void filterList(List<? extends ContactInfo> allList, List<ContactInfo> listToUpdate, String textToBeFiltered)
		{

			try
			{

				for (ContactInfo info : allList)
				{
					String name = info.getName();
					boolean found = false;
					int startIndex = 0;
					if(name!=null){
					name = name.toLowerCase();
					if (name.startsWith(textToBeFiltered))
					{
						found = true;
					}
					else if (name.contains(" " + textToBeFiltered))
					{
						found = true;
						startIndex = name.indexOf(" " + textToBeFiltered) + 1;
					}
					}
					if(found)
					{
						contactSpanStartIndexes.put(info.getMsisdn(), startIndex);
						listToUpdate.add(info);
					}
					else
					{
						String msisdn = info.getMsisdn();
						if (msisdn != null && !OneToNConversationUtils.isOneToNConversation(msisdn))
						{
							if(msisdn.contains(textToBeFiltered))
							{
								listToUpdate.add(info);
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			List<List<ContactInfo>> resultList = (List<List<ContactInfo>>) results.values;

			makeFilteredList(constraint, resultList, filteredBotList);

			if(recentlyJoinedHikeContactsList != null && !recentlyJoinedHikeContactsList.isEmpty())
			{
				filteredRecentlyJoinedHikeContactsList.clear();
				filteredRecentlyJoinedHikeContactsList.addAll(resultList.get(6));
			}
			if(nuxRecommendedList != null && !nuxRecommendedList.isEmpty())
			{
				nuxFilteredRecoList.clear();
				nuxFilteredRecoList.addAll(resultList.get(7));
			}
            if(suggestedContactsList != null && !suggestedContactsList.isEmpty())
            {
                filteredSuggestedContactsList.clear();
                filteredSuggestedContactsList.addAll(resultList.get(7));
            }
			
			makeCompleteList(true);
		}
	}

	protected List<List<ContactInfo>> makeOriginalList()
	{
		List<List<ContactInfo>> resultList = new ArrayList<List<ContactInfo>>(3);
		resultList.add(hikeOtherFeaturesList);
		resultList.add(friendsList);
		resultList.add(hikeContactsList);
		resultList.add(smsContactsList);
		resultList.add(groupsList);
		resultList.add(recentContactsList);
		resultList.add(recentlyJoinedHikeContactsList);
		resultList.add(nuxRecommendedList);
		resultList.add(suggestedContactsList);
		return resultList;
	}

	protected void makeFilteredList(CharSequence constraint, List<List<ContactInfo>> resultList)
	{
		int listsSize = resultList.size();


		if (filteredHikeOtherFeaturesList != null && listsSize > 0)
		{
			filteredHikeOtherFeaturesList.clear();
			filteredHikeOtherFeaturesList.addAll(resultList.get(0));
		}

		filteredFriendsList.clear();
		if(listsSize > 1)
		{
			filteredFriendsList.addAll(resultList.get(1));
		}

		filteredHikeContactsList.clear();
		if (listsSize > 2) {
			filteredHikeContactsList.addAll(resultList.get(2));
		}

		filteredSmsContactsList.clear();
		if(listsSize > 3)
		{
			filteredSmsContactsList.addAll(resultList.get(3));
		}

		if (groupsList != null && !groupsList.isEmpty())
		{
			filteredGroupsList.clear();
			if(listsSize > 4)
				filteredGroupsList.addAll(resultList.get(4));
		}

		if (recentContactsList != null && !recentContactsList.isEmpty())
		{
			filteredRecentsList.clear();
			if(listsSize > 5)
				filteredRecentsList.addAll(resultList.get(5));
		}
	}

	protected void makeFilteredList(CharSequence constraint, List<List<ContactInfo>> resultsList, List<BotInfo> filteredBots)
	{
		makeFilteredList(constraint, resultsList);

		if (filteredBots != null)
		{
			if (listFetchedOnce)
			{
				filteredmicroAppShowcaseList.clear();
			}

			if (!filteredBots.isEmpty())
				filteredmicroAppShowcaseList.addAll(filteredBots);
		}


	}

	public void makeCompleteList(boolean filtered)
	{
		makeCompleteList(filtered, false);
	}

	public void makeCompleteList(boolean filtered, boolean firstFetch)
	{
		if (firstFetch)
		{
			friendsListFetchedCallback.listFetched();
		}

		boolean shouldContinue = makeSetupForCompleteList(filtered,firstFetch);

		if (!shouldContinue)
		{
			return;
		}

		/*
		 * removed extra items from friends screen
		 */

        if(!showFilteredContacts)
        {
            friendsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredFriendsList.size()), context.getString(Utils.isFavToFriendsMigrationAllowed() ? R.string.friends_upper_case : R.string.favorites_upper_case), FRIEND_PHONE_NUM);
            updateFriendsList(friendsSection, true, true);
        }

        if (isHikeContactsPresent())
		{
			hikeContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredHikeContactsList.size()), context.getString(Utils.isFavToFriendsMigrationAllowed() ? R.string.add_frn_upper_case : R.string.add_favorites_upper_case), CONTACT_PHONE_NUM);
			updateHikeContactList(hikeContactsSection);
		}
		if (showSMSContacts)
		{
			smsContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredSmsContactsList.size()), context.getString(R.string.sms_contacts), CONTACT_SMS_NUM);
			updateSMSContacts(smsContactsSection);
		}
        if(showFilteredContacts)
        {
            suggestedContactsSection = new ContactInfo(SECTION_ID, Integer.toString(filteredSuggestedContactsList.size()), context.getString(R.string.contacts), CONTACT_FILTERED_NUM);
            updateSuggestedContacts(suggestedContactsSection);
        }

		notifyDataSetChanged();
		setEmptyView();
	}

	protected boolean makeSetupForCompleteList(boolean filtered, boolean firstFetch)
	{
		/*
		 * Only try to filter if we've fetched the list once.
		 */
		
                //Begin Fix AND-3408
		if (!TextUtils.isEmpty(queryText)) {
			if (firstFetch && listFetchedOnce) {
				contactFilter.filter(queryText);
				return false;
			}
		}
                //End Fix AND-3408

		/*
		 * If we do not fetch the list even once and all the lists are empty, we should show the spinner. Else we show the empty states
		 */
		if (!listFetchedOnce
				&& ((friendsList.isEmpty() && hikeContactsList.isEmpty() && smsContactsList.isEmpty()) || (filteredFriendsList.isEmpty() && filteredHikeContactsList.isEmpty() && filteredSmsContactsList
						.isEmpty())))
		{
			return false;
		}

		completeList.clear();

		return true;
	}

	protected void updateExtraList()
	{
		if (TextUtils.isEmpty(queryText))
		{

			inviteExtraItem = new ContactInfo(EXTRA_ID, INVITE_MSISDN, context.getString(R.string.invite_friends_hike), null);
			completeList.add(inviteExtraItem);

			groupExtraItem = new ContactInfo(EXTRA_ID, GROUP_MSISDN, context.getString(R.string.create_group), null);
			completeList.add(groupExtraItem);
		}
	}

	protected void updateFriendsList(ContactInfo section, boolean addFTUE, boolean showAddFriendView)
	{

		boolean hideSuggestions = true;

		if (section != null)
		{
			// either not filtered or if filtered then list should not be empty
			if (!filteredFriendsList.isEmpty() || !isFiltered)
				completeList.add(section);
		}
		if (addFTUE && !HomeActivity.ftueContactsData.isEmpty() && TextUtils.isEmpty(queryText) && friendsList.size() < HikeConstants.FTUE_LIMIT)
		{
			SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

			hideSuggestions = prefs.getBoolean(HikeMessengerApp.HIDE_FTUE_SUGGESTIONS, false);

			if (!hideSuggestions)
			{
				long firstTimeViewFtueSuggestionsTs = prefs.getLong(HikeMessengerApp.FIRST_VIEW_FTUE_LIST_TIMESTAMP, 0);

				long currentTime = System.currentTimeMillis() / 1000;

				if (currentTime - firstTimeViewFtueSuggestionsTs > 60 * 60)
				{
					completeList.add(new ContactInfo(REMOVE_SUGGESTIONS_ID, null, null, null));
				}

				int limit = HikeConstants.FTUE_LIMIT - friendsList.size();

				int counter = 0;
				for (ContactInfo contactInfo : HomeActivity.ftueContactsData.getCompleteList())
				{
					FavoriteType favoriteType = contactInfo.getFavoriteType();
					if (favoriteType == FavoriteType.NOT_FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED || favoriteType == null)
					{
						completeList.add(contactInfo);
						if (++counter == limit)
						{
							break;
						}
					}
				}
				if (!friendsList.isEmpty())
				{

					completeList.addAll(filteredFriendsList);
				}
			}
		}

		if (hideSuggestions)
		{
			if (!Utils.isFavToFriendsMigrationAllowed() && showAddFriendView && friendsList.isEmpty())
			{
				if (TextUtils.isEmpty(queryText))
				{
					completeList.add(new ContactInfo(EMPTY_ID, null, null, null));
				}
			}
			else
			{

				completeList.addAll(filteredFriendsList);
			}
		}
	}

	protected void updateHikeContactList(ContactInfo section)
	{

		if (!filteredHikeContactsList.isEmpty())
		{
			if (section != null)
			{
				completeList.add(section);
			}
			completeList.addAll(filteredHikeContactsList);
		}
	}

	protected void updateSMSContacts(ContactInfo section)
	{
		if (!filteredSmsContactsList.isEmpty())
		{
			if (section != null)
			{
				completeList.add(section);
			}
			completeList.addAll(filteredSmsContactsList);
		}
	}

    protected void updateSuggestedContacts(ContactInfo section)
    {
        if (!filteredSuggestedContactsList.isEmpty())
        {
            if (section != null)
            {
                completeList.add(section);
            }
            completeList.addAll(filteredSuggestedContactsList);
        }
    }


	protected boolean isHikeContactsPresent()
	{
		return !hikeContactsList.isEmpty();
	}

	public void toggleShowSMSContacts(boolean showSMSOn)
	{
		this.showSMSContacts = showSMSOn;
		notifyDataSetChanged();
	}

	private void removeContactByMatchingMsisdn(List<ContactInfo> contactList, ContactInfo contactInfo)
	{
		for (int i = 0; i < contactList.size(); i++)
		{
			ContactInfo listContactInfo = contactList.get(i);
			if (listContactInfo.getMsisdn().equals(contactInfo.getMsisdn()))
			{
				contactList.remove(i);
				break;
			}
		}
	}

	public void removeContact(ContactInfo contactInfo, boolean remakeCompleteList)
	{
		removeContactByMatchingMsisdn(friendsList, contactInfo);

		removeContactByMatchingMsisdn(hikeContactsList, contactInfo);

		removeContactByMatchingMsisdn(smsContactsList, contactInfo);

		if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
		{
			removeContactByMatchingMsisdn(friendsStealthList, contactInfo);

			removeContactByMatchingMsisdn(hikeStealthContactsList, contactInfo);

			removeContactByMatchingMsisdn(smsStealthContactsList, contactInfo);
		}

		if (remakeCompleteList)
		{
			makeCompleteList(false);
		}
	}

	public void removeStealthContacts()
	{
		removeStealthContactFromList(friendsList);
		removeStealthContactFromList(hikeContactsList);
		removeStealthContactFromList(smsContactsList);

		makeCompleteList(false);
	}

	private void removeStealthContactFromList(List<ContactInfo> contactList)
	{
		// TODO improve the searching here.
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				iter.remove();
			}
		}
	}

	public void addStealthContacts()
	{
		friendsList.addAll(friendsStealthList);
		Collections.sort(friendsList, lastSeenComparator);

		hikeContactsList.addAll(hikeStealthContactsList);
		Collections.sort(hikeContactsList);

		smsContactsList.addAll(smsStealthContactsList);
		Collections.sort(smsContactsList);

		makeCompleteList(false);
	}

	public void clearStealthLists()
	{
		friendsStealthList.clear();
		hikeStealthContactsList.clear();
		smsStealthContactsList.clear();
	}

	public void stealthContactAdded(String msisdn)
	{
		boolean contactAdded = addSingleStealthContactToStealthList(friendsList, friendsStealthList, msisdn);
		/*
		 * We only need to add the contact once. So if it was found removed in the first list, we don't need to check for it in the next list.
		 */
		if (contactAdded)
		{
			return;
		}
		contactAdded = addSingleStealthContactToStealthList(hikeContactsList, hikeStealthContactsList, msisdn);
		if (contactAdded)
		{
			return;
		}
		addSingleStealthContactToStealthList(smsContactsList, smsStealthContactsList, msisdn);
	}

	private boolean addSingleStealthContactToStealthList(List<ContactInfo> contactList, List<ContactInfo> stealthList, String msisdn)
	{
		for (ContactInfo contactInfo : contactList)
		{
			if (msisdn.equals(contactInfo.getMsisdn()))
			{
				stealthList.add(contactInfo);
				return true;
			}
		}
		return false;
	}

	public void stealthContactRemoved(String msisdn)
	{
		boolean contactRemoved = removeSingleStealthContactFromStealthList(friendsStealthList, msisdn);
		/*
		 * We only need to remove the contact once. So if it was already removed in the first list, we don't need to check for it in the next list.
		 */
		if (contactRemoved)
		{
			return;
		}
		contactRemoved = removeSingleStealthContactFromStealthList(hikeStealthContactsList, msisdn);
		if (contactRemoved)
		{
			return;
		}
		removeSingleStealthContactFromStealthList(smsStealthContactsList, msisdn);
	}

	private boolean removeSingleStealthContactFromStealthList(List<ContactInfo> contactList, String msisdn)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			if (msisdn.equals(contactInfo.getMsisdn()))
			{
				iter.remove();
				return true;
			}
		}
		return false;
	}

	public void addToGroup(ContactInfo contactInfo, int groupIndex)
	{
		removeContact(contactInfo, false);

		if (getCount() == 0)
		{
			return;
		}

		/*
		 * We check if the contact to be added is a stealth contact. If it is, we check if the current stealth mode allows us to display stealth contacts. If not we skip the rest
		 * of the process.
		 */
		if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
		{
			boolean addToDisplayList = addToStealthList(contactInfo, groupIndex);
			if (!addToDisplayList)
			{
				return;
			}
		}

		switch (groupIndex)
		{
		case FRIEND_INDEX:
			friendsList.add(contactInfo);
			Collections.sort(friendsList, lastSeenComparator);
			break;
		case HIKE_INDEX:
			hikeContactsList.add(contactInfo);
			Collections.sort(hikeContactsList);
			break;
		case SMS_INDEX:
			smsContactsList.add(contactInfo);
			Collections.sort(smsContactsList);
			break;
		}

		makeCompleteList(false);
	}

	private boolean addToStealthList(ContactInfo contactInfo, int groupIndex)
	{
		switch (groupIndex)
		{
		case FRIEND_INDEX:
			friendsStealthList.add(contactInfo);
			break;
		case HIKE_INDEX:
			hikeStealthContactsList.add(contactInfo);
			break;
		case SMS_INDEX:
			smsStealthContactsList.add(contactInfo);
			break;
		}

		return StealthModeManager.getInstance().isActive();
	}

	public void refreshGroupList(List<ContactInfo> newGroupList, int groupIndex)
	{
		List<ContactInfo> groupList = null;
		List<ContactInfo> stealthList = null;
		switch (groupIndex)
		{
		case FRIEND_INDEX:
			groupList = friendsList;
			stealthList = friendsStealthList;
			break;
		case HIKE_INDEX:
			groupList = hikeContactsList;
			stealthList = hikeStealthContactsList;
			break;
		case SMS_INDEX:
			groupList = smsContactsList;
			stealthList = smsStealthContactsList;
			break;
		}
		if (null != groupList)
		{
			groupList.clear();
		}
		if (null != stealthList)
		{
			stealthList.clear();
		}

		groupList.addAll(newGroupList);
		setupStealthListAndRemoveFromActualList(groupList, stealthList);

		makeCompleteList(false);
	}

	private void setupStealthListAndRemoveFromActualList(List<ContactInfo> contactList, List<ContactInfo> stealthList)
	{
		for(Iterator<ContactInfo> iterator = contactList.iterator(); iterator.hasNext();)
		{
			ContactInfo contactInfo = iterator.next();
			if(StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				stealthList.add(contactInfo);
				if(!StealthModeManager.getInstance().isActive())
				{
					iterator.remove();
				}
			}
		}
	}

	public void removeFromGroup(ContactInfo contactInfo, int groupIndex)
	{
		switch (groupIndex)
		{
		case FRIEND_INDEX:
			removeContactByMatchingMsisdn(friendsList, contactInfo);
			removeContactByMatchingMsisdn(friendsStealthList, contactInfo);
			break;
		case HIKE_INDEX:
			removeContactByMatchingMsisdn(hikeContactsList, contactInfo);
			removeContactByMatchingMsisdn(hikeStealthContactsList, contactInfo);
			break;
		case SMS_INDEX:
			removeContactByMatchingMsisdn(smsContactsList, contactInfo);
			removeContactByMatchingMsisdn(smsStealthContactsList, contactInfo);
			break;
		}
		makeCompleteList(false);
	}

	@Override
	public int getCount()
	{
		return completeList.size();
	}

	@Override
	public ContactInfo getItem(int position)
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

	@Override
	public int getItemViewType(int position)
	{

		ContactInfo contactInfo = getItem(position);
		if (EMPTY_ID.equals(contactInfo.getId()))
		{
			return ViewType.EMPTY.ordinal();
		}
		else if (SECTION_ID.equals(contactInfo.getId()))
		{
			return ViewType.SECTION.ordinal();
		}
		else if (EXTRA_ID.equals(contactInfo.getId()))
		{
			return ViewType.EXTRA.ordinal();
		}
		else if (REMOVE_SUGGESTIONS_ID.equals(contactInfo.getId()))
		{
			return ViewType.REMOVE_SUGGESTIONS.ordinal();
		}
		else if (HIKE_APPS_ID.equals(contactInfo.getId()))
		{
			return ViewType.HIKE_APPS.ordinal();
		}
		else if (HIKE_FEATURES_ID.equals(contactInfo.getId()))
		{
			return ViewType.HIKE_FEATURES.ordinal();
		}
		else
		{
			return getViewTypebasedOnFavType(contactInfo);
		}
	}

	public int getViewTypebasedOnFavType(ContactInfo contactInfo)
	{
		FavoriteType favoriteType = contactInfo.getFavoriteType();
		if (favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT || favoriteType == FavoriteType.REQUEST_SENT_REJECTED)
		{
			return ViewType.FRIEND.ordinal();
		}
		else if (favoriteType == FavoriteType.REQUEST_RECEIVED)
		{
			return ViewType.FRIEND_REQUEST.ordinal();
		}
		else if (HikeConstants.FTUE_MSISDN_TYPE.equals(contactInfo.getMsisdnType()))
		{
			return ViewType.FTUE_CONTACT.ordinal();
		}
		else if (contactInfo.isOnhike())
		{
			return ViewType.NOT_FRIEND_HIKE.ordinal();
		}
		return ViewType.NOT_FRIEND_SMS.ordinal();
	}

	private class ViewHolder
	{
		ImageView avatar;
		TextView name;
		ImageView onlineIndicator;
		TextView lastSeen;
		ImageView statusMood;
		TextView inviteBtn;
		TextView info;
		ImageView addFriend;
		ImageView inviteIcon;
		ViewGroup infoContainer;
		ImageView acceptBtn;
		ImageView rejectBtn;
		TextView addBtn;

		String msisdn;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		ContactInfo contactInfo = getItem(position);

		ViewHolder viewHolder = null;
		if (convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case FTUE_CONTACT:
			case FRIEND:
				convertView = layoutInflater.inflate(R.layout.friends_child_view, null);
				break;

			case NOT_FRIEND_HIKE:
				convertView = layoutInflater.inflate(R.layout.hike_contact_child_view, null);
				break;

			case NOT_FRIEND_SMS:
				convertView = layoutInflater.inflate(R.layout.sms_contact_child_view, null);
				break;

			case SECTION:
				convertView = layoutInflater.inflate(R.layout.friends_group_view, null);
				break;
			case FRIEND_REQUEST:
				convertView = layoutInflater.inflate(R.layout.friend_request_view, null);
				break;
			case EXTRA:
				convertView = layoutInflater.inflate(R.layout.friends_tab_extra_item, null);
				break;
			case EMPTY:
				convertView = layoutInflater.inflate(R.layout.friends_empty_view, parent, false);
				break;
			case REMOVE_SUGGESTIONS:
				convertView = layoutInflater.inflate(R.layout.remove_suggestions, parent, false);
			}

			switch (viewType)
			{
			case FRIEND:
			case NOT_FRIEND_HIKE:
			case FRIEND_REQUEST:
			case NOT_FRIEND_SMS:
			case FTUE_CONTACT:
				viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
				viewHolder.name = (TextView) convertView.findViewById(R.id.contact);
				viewHolder.onlineIndicator = (ImageView) convertView.findViewById(R.id.online_indicator);
				viewHolder.lastSeen = (TextView) convertView.findViewById(R.id.last_seen);
				viewHolder.statusMood = (ImageView) convertView.findViewById(R.id.status_mood);
				viewHolder.inviteBtn = (TextView) convertView.findViewById(R.id.invite_btn);
				viewHolder.acceptBtn = (ImageView) convertView.findViewById(R.id.accept);
				viewHolder.rejectBtn = (ImageView) convertView.findViewById(R.id.reject);
				viewHolder.addBtn = (TextView) convertView.findViewById(R.id.invite_btn);
				viewHolder.inviteBtn = (TextView) convertView.findViewById(R.id.invite_btn);
				viewHolder.inviteIcon = (ImageView) convertView.findViewById(R.id.invite_icon);
				viewHolder.addFriend = (ImageView) convertView.findViewById(R.id.add_friend);
				viewHolder.info = (TextView) convertView.findViewById(R.id.info);
				viewHolder.infoContainer = (ViewGroup) convertView.findViewById(R.id.info_container);
				break;

			case SECTION:
				viewHolder.name = (TextView) convertView.findViewById(R.id.name);
				viewHolder.info = (TextView) convertView.findViewById(R.id.count);
				break;

			case EXTRA:
				viewHolder.name = (TextView) convertView.findViewById(R.id.contact);
				viewHolder.onlineIndicator = (ImageView) convertView.findViewById(R.id.icon);
				break;
			case EMPTY:
				viewHolder.name = (TextView) convertView.findViewById(R.id.empty_text);
				String infoSubText = context.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
				viewHolder.name.setText(context.getString(R.string.tap_plus_add_favorites, infoSubText));
				break;
			}

			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType)
		{
		case FRIEND:
		case NOT_FRIEND_HIKE:
		case FRIEND_REQUEST:
		case NOT_FRIEND_SMS:
		case FTUE_CONTACT:

			viewHolder.msisdn = contactInfo.getMsisdn();

			TextView name = viewHolder.name;
			ImageView onlineIndicator = viewHolder.onlineIndicator;

			updateViewsRelatedToAvatar(convertView, contactInfo);

			String contactName = contactInfo.getName();
			String msisdn = contactInfo.getMsisdn();

			if(TextUtils.isEmpty(contactName))
			{
				name.setText(msisdn);
			}
			else
			{
				Integer startIndex = contactSpanStartIndexes.get(msisdn);
				if(startIndex!=null)
				{
					name.setText(getSpanText(contactName, startIndex), TextView.BufferType.SPANNABLE);
				}
				else
				{
					name.setText(contactName);
				}
			}

			if (viewType == ViewType.FRIEND || viewType == ViewType.FRIEND_REQUEST || viewType == ViewType.FTUE_CONTACT)
			{
				TextView lastSeen = viewHolder.lastSeen;
				ImageView statusMood = viewHolder.statusMood;

				lastSeen.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				lastSeen.setVisibility(View.GONE);

				TextView inviteBtn = viewHolder.inviteBtn;
				if (inviteBtn != null)
				{
					inviteBtn.setVisibility(View.GONE);
				}

				if (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
				{
					lastSeen.setVisibility(View.VISIBLE);
					StatusMessage lastStatusMessage = lastStatusMessagesMap.get(contactInfo.getMsisdn());
					if(lastStatusMessage != null)
					{
						lastSeen.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
						SmileyParser smileyParser = SmileyParser.getInstance();
						switch (lastStatusMessage.getStatusMessageType())
						{
						case TEXT:
							lastSeen.setText(smileyParser.addSmileySpans(lastStatusMessage.getText(), true));
							if (lastStatusMessage.hasMood())
							{
								statusMood.setVisibility(View.VISIBLE);
								statusMood.setImageResource(EmoticonConstants.moodMapping.get(lastStatusMessage.getMoodId()));
							}
							else
							{
								statusMood.setVisibility(View.GONE);
							}
							break;

						case PROFILE_PIC:
							lastSeen.setText(R.string.changed_profile);
							statusMood.setVisibility(View.GONE);
							break;

						case IMAGE:
						case TEXT_IMAGE:
							if(TextUtils.isEmpty(lastStatusMessage.getText()))
							{
								lastSeen.setText(lastStatusMessage.getMsisdn());
							}
							else
							{
								lastSeen.setText(smileyParser.addSmileySpans(lastStatusMessage.getText(), true));
							}
							if (lastStatusMessage.hasMood())
							{
								statusMood.setVisibility(View.VISIBLE);
								statusMood.setImageResource(EmoticonConstants.moodMapping.get(lastStatusMessage.getMoodId()));
							}
							else
							{
								statusMood.setVisibility(View.GONE);
							}
							break;
							
						default:
							break;
						}
					}
					else
					{
						lastSeen.setText(contactInfo.getMsisdn());
						statusMood.setVisibility(View.GONE);
					}
					if(onlineIndicator != null)
					{
						onlineIndicator.setVisibility(View.GONE);
					}
				}
				else if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT_REJECTED)
				{
					lastSeen.setVisibility(View.VISIBLE);
					lastSeen.setText(contactInfo.getMsisdn());
					statusMood.setVisibility(View.GONE);
					onlineIndicator.setVisibility(View.GONE);
				}
				else
				{
					if(onlineIndicator != null)
					{
						onlineIndicator.setVisibility(View.GONE);
					}
					if(statusMood != null)
					{
						statusMood.setVisibility(View.GONE);
					}
					if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT)
					{
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(contactInfo.getMsisdn());

						if (!contactInfo.isOnhike())
						{
							setInviteButton(contactInfo, inviteBtn, null);
						}
					}
					else if (viewType == ViewType.FRIEND_REQUEST)
					{
						lastSeen.setVisibility(View.VISIBLE);
						String infoSubText = context.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
						if (Utils.isFavToFriendsMigrationAllowed())
						{
							lastSeen.setText(context.getString(R.string.sent_you_friend_req));
						}
						else
						{
							lastSeen.setText(context.getString(R.string.sent_favorite_request_tab, infoSubText));
						}

						ImageView acceptBtn = viewHolder.acceptBtn;
						ImageView rejectBtn = viewHolder.rejectBtn;

						acceptBtn.setTag(contactInfo);
						rejectBtn.setTag(contactInfo);

						acceptBtn.setOnClickListener(acceptOnClickListener);
						rejectBtn.setOnClickListener(rejectOnClickListener);

						if (Utils.isFavToFriendsMigrationAllowed())
						{
							rejectBtn.setVisibility(View.GONE);
						}

					}
					else if (viewType == ViewType.FTUE_CONTACT)
					{
						lastSeen.setVisibility(View.VISIBLE);
						lastSeen.setText(Utils.isFavToFriendsMigrationAllowed() ? R.string.ftue_frn_subtext : R.string.ftue_favorite_subtext);

						TextView addBtn = viewHolder.addBtn;

						addBtn.setVisibility(View.VISIBLE);
						addBtn.setText(R.string.add);
						addBtn.setTag(contactInfo);
						addBtn.setOnClickListener(addOnClickListener);
					}
					
				}
			}
			else
			{
				TextView info = viewHolder.info;
				info.setText(contactInfo.isOnhike() ? R.string.tap_chat : R.string.tap_sms);
				if (viewType == ViewType.NOT_FRIEND_HIKE)
				{
					ImageView addFriend = viewHolder.addFriend;
					viewHolder.addFriend.setImageResource(Utils.isFavToFriendsMigrationAllowed() ? R.drawable.ic_add_friend : R.drawable.ic_add_favourite );
					addFriend.setTag(contactInfo);
					addFriend.setOnClickListener(this);
				}
				else
				{
					TextView inviteBtn = viewHolder.inviteBtn;
					ImageView inviteIcon = viewHolder.inviteIcon;
					ViewGroup infoContainer = viewHolder.infoContainer;

					setInviteButton(contactInfo, inviteBtn, inviteIcon);

                    LayoutParams layoutParams = (LayoutParams) infoContainer.getLayoutParams();
                    if (inviteIcon.getVisibility() == View.VISIBLE)
                    {
                        layoutParams.addRule(RelativeLayout.LEFT_OF, inviteIcon.getId());
                    }
                    else
                    {
                        layoutParams.addRule(RelativeLayout.LEFT_OF, inviteBtn.getId());
                    }
				}
			}
			break;

		case SECTION:
			TextView headerName = viewHolder.name;
			TextView headerCount = viewHolder.info;

			headerName.setText(contactInfo.getName());
			headerCount.setText(contactInfo.getMsisdn());
			if (!TextUtils.isEmpty(contactInfo.getPhoneNum()))
			{
				switch (contactInfo.getPhoneNum())
				{
				case FRIEND_PHONE_NUM:
					headerName.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(Utils.isFavToFriendsMigrationAllowed() ? R.drawable.ic_section_header_friends : R.drawable.ic_section_header_favorite), null, null, null);
					break;

				case CONTACT_PHONE_NUM:
					headerName.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_people_on_hike), null, null, null);
					break;

				case CONTACT_SMS_NUM:
                    headerName.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_sms_contact), null, null, null);
                    break;

                case CONTACT_FILTERED_NUM:
                    headerName.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_section_header_people_on_hike), null, null, null);
                    break;
				}

				headerName.setCompoundDrawablePadding((int) context.getResources().getDimension(R.dimen.favorites_star_icon_drawable_padding));

			}
			else
			{
				headerName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			break;

		case EXTRA:
			TextView headerName2 = viewHolder.name;
			ImageView headerIcon = viewHolder.onlineIndicator;

			if (contactInfo.getMsisdn().equals(INVITE_MSISDN))
			{
				headerIcon.setImageResource(R.drawable.ic_invite_to_hike);
				headerName2.setText(R.string.invite_friends_hike);
			}
			else
			{
				headerIcon.setImageResource(R.drawable.ic_create_group);
				headerName2.setText(R.string.create_group);
			}
			break;

		case EMPTY:
			TextView emptyText = viewHolder.name;
			String infoSubText = context.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
			emptyText.setText(context.getString(R.string.tap_plus_add_favorites, infoSubText));
			break;
		}

		return convertView;
	}

	protected SpannableString getSpanText(String text, Integer start)
	{
		SpannableString spanName = new SpannableString(text);
		int end = start + queryText.length();
		if (end > text.length())
		{
			end = text.length();
		}
		spanName.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blue_color_span)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spanName;
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

		iconloader.loadImage(contactInfo.getMsisdn(), holder.avatar, isListFlinging, false, true);
	}

	private void setInviteButton(ContactInfo contactInfo, TextView inviteBtn, ImageView inviteIcon)
	{

		inviteBtn.setEnabled(true);
		inviteBtn.setBackgroundResource(R.drawable.bg_green_btn_selector);
		inviteBtn.setTextColor(context.getResources().getColor(R.color.white));

		inviteBtn.setOnClickListener(inviteOnClickListener);
		inviteBtn.setTag(contactInfo);

		if (inviteIcon != null)
		{
			inviteIcon.setOnClickListener(inviteOnClickListener);
			inviteIcon.setTag(contactInfo);
		}

		if (contactInfo.getInviteTime() == 0)
		{
			if (inviteIcon != null)
			{
				inviteIcon.setVisibility(View.VISIBLE);
				inviteBtn.setVisibility(View.GONE);
			}
			else
			{
				inviteBtn.setVisibility(View.VISIBLE);
				inviteBtn.setText(R.string.invite_1);
			}
		}
		else
		{
			if (inviteIcon != null)
			{
				inviteIcon.setVisibility(View.GONE);
			}
			inviteBtn.setVisibility(View.VISIBLE);

			long inviteTime = contactInfo.getInviteTime();

			/*
			 * If the contact was invited more than an hour back, we give the option to remind this contact
			 */
			if ((System.currentTimeMillis() / 1000 - inviteTime) > 60 * 60)
			{
				inviteBtn.setText(R.string.remind);
			}
			else
			{
				inviteBtn.setEnabled(false);
				inviteBtn.setText(R.string.invited);
				inviteBtn.setBackgroundResource(0);
				inviteBtn.setTextColor(context.getResources().getColor(R.color.description_lightgrey));
			}
		}
	}

	@Override
	public void onClick(View v)
	{

		ContactInfo contactInfo = null;
		Object tag = v.getTag();

		if (tag instanceof ContactInfo)
		{
			contactInfo = (ContactInfo) tag;
		}
		else
		{
			return;
		}

		Utils.addFavorite(context, contactInfo, false, HikeConstants.AddFriendSources.FRIENDS_SCREEN);
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		ContactInfo contactInfo = getItem(position);
		if (SECTION_ID.equals(contactInfo.getId()))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private OnClickListener acceptOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, true);
		}
	};

	private OnClickListener rejectOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			respondToFriendRequest(contactInfo, false);
		}
	};

	private OnClickListener inviteOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();
			Utils.sendInviteUtil(contactInfo, context, HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED, context.getString(R.string.native_header),
					context.getString(R.string.native_info), WhichScreen.SMS_SECTION);
		}
	};

	private OnClickListener addOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			Utils.addFavorite(context, contactInfo, true, HikeConstants.AddFriendSources.FRIENDS_SCREEN);

			ContactInfo contactInfo2 = new ContactInfo(contactInfo);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.ADD_FRIENDS_CLICK);
				metadata.put(AnalyticsConstants.TO, contactInfo2.getMsisdn());
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			if (!contactInfo.isOnhike())
				Utils.sendInviteUtil(contactInfo2, context, HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED, context.getString(R.string.ftue_add_prompt_invite_title),
						context.getString(R.string.ftue_add_prompt_invite), WhichScreen.FRIENDS_TAB);

		}
	};

	private void respondToFriendRequest(ContactInfo contactInfo, boolean accept)
	{
		FavoriteType favoriteType = accept ? FavoriteType.FRIEND : FavoriteType.REQUEST_RECEIVED_REJECTED;
		Pair<ContactInfo, FavoriteType> favoriteAdded = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(favoriteType == FavoriteType.FRIEND ? HikePubSub.FAVORITE_TOGGLED : HikePubSub.REJECT_FRIEND_REQUEST, favoriteAdded);

		removeFromGroup(contactInfo, FRIEND_INDEX);
		if (accept)
		{
			HikeAnalyticsEvent.recordAnalyticsForAddFriend(contactInfo.getMsisdn(), HikeConstants.AddFriendSources.FRIENDS_SCREEN, false); //req_acc
		}
	}

	@Override
	public boolean isItemViewTypePinned(int viewType)
	{
		return viewType == ViewType.SECTION.ordinal();
	}

	public List<ContactInfo> getCompleteList()
	{
		return completeList;
	}

	public void setCompleteList(List<ContactInfo> completeList)
	{
		this.completeList = completeList;
	}

	public List<ContactInfo> getFriendsList()
	{
		return friendsList;
	}

	public List<ContactInfo> getStealthFriendsList()
	{
		return friendsStealthList;
	}

	public void setFriendsList(List<ContactInfo> friendsList)
	{
		this.friendsList = friendsList;
	}

	public List<ContactInfo> getFilteredFriendsList()
	{
		return filteredFriendsList;
	}

	public void setFilteredFriendsList(List<ContactInfo> filteredFriendsList)
	{
		this.filteredFriendsList = filteredFriendsList;
	}

	public void destroy()
	{
		friendsList.clear();
		hikeContactsList.clear();
		smsContactsList.clear();
		filteredFriendsList.clear();
		filteredHikeContactsList.clear();
		filteredSmsContactsList.clear();
	}

	public void setEmptyView(View view)
	{
		this.emptyView = view;
	}

	public void setLoadingView(View view)
	{
		this.loadingView = view;
	}

	protected void setLoadingView()
	{
		if (loadingView != null)
		{
			listView.setEmptyView(loadingView);
			Logger.e("errrr", "loading view is not null");
		}
		else
		{
			Logger.e("errrr", "loading view is null");
		}
	}

	public void setEmptyView()
	{
		if (emptyView != null)
		{
			if(loadingView != null)
				loadingView.setVisibility(View.GONE);
			listView.setEmptyView(emptyView);
		}
	}
	
	public void initiateLastStatusMessagesMap(Map<String, StatusMessage> lastStatusMessagesMap)
	{
		this.lastStatusMessagesMap.putAll(lastStatusMessagesMap);
	}
	
	public Map<String, StatusMessage> getLastStatusMessagesMap()
	{
		return lastStatusMessagesMap;
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
				if (viewType == ViewType.SECTION || viewType == ViewType.EXTRA || viewType == ViewType.EMPTY || !contactInfo.isOnhike())
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
	
	public void setOriginalMicroAppsCount(int count)
	{
		this.originalMicroAppCount = count;
	}
}
