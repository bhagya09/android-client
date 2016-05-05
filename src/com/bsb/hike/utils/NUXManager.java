package com.bsb.hike.utils;

import static com.bsb.hike.NUXConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.NUXConstants.PushTypeEnum;
import com.bsb.hike.NUXConstants.RewardTypeEnum;
import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.NUXChatReward;
import com.bsb.hike.models.NUXTaskDetails;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.models.NuxInviteFriends;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.service.HikeMqttManagerNew;


/**
 * 
 * 
 * @author himanshu
 * 
 * A class that managers all states ,contact related to NUX.
 *
 */
public class NUXManager
{
	private static final NUXManager mmManager = new NUXManager();


	private HashSet<String> listNuxContacts;

	private HashSet<String> unlockedNUXContacts;

	private NuxInviteFriends inviteFriends;

	private NuxSelectFriends selectFriends;

	private NuxCustomMessage customMessage;

	private NUXChatReward chatReward;

	private HikeSharedPreferenceUtil mprefs;

	private NUXTaskDetails taskDetails;

	Context context;

	private NUXManager()
	{
		this.context = HikeMessengerApp.getInstance().getApplicationContext();
		listNuxContacts = new HashSet<String>();
		mprefs = HikeSharedPreferenceUtil.getInstance(NUX_SHARED_PREF);
		String msisdn = mprefs.getData(CURRENT_NUX_CONTACTS, null);
		if (!TextUtils.isEmpty(msisdn))
		{
			String[] arrmsisdn = msisdn.split(STRING_SPLIT_SEPERATOR);
			listNuxContacts.addAll(Arrays.asList(arrmsisdn));
		}

		unlockedNUXContacts = new HashSet<String>();
		msisdn = mprefs.getData(UNLOCKED_NUX_CONTACTS, null);
		if (!TextUtils.isEmpty(msisdn))
		{
			String[] arrmsisdn = msisdn.split(STRING_SPLIT_SEPERATOR);
			unlockedNUXContacts.addAll(Arrays.asList(arrmsisdn));
		}
	}

	public static NUXManager getInstance()
	{
		return mmManager;
	}

	public void startNUX(Activity activity)
	{
		activity.startActivity(IntentFactory.openInviteFriends(activity));
		activity.finish();
	}

	public void startNuxCustomMessage(String selectedFriends, Activity activity)
	{
		Intent in = IntentFactory.openNuxCustomMessage(activity);
		in.putExtra(SELECTED_FRIENDS, selectedFriends);
		activity.startActivity(in);
	}

	public void startNuxSelector(Activity activity)
	{
		if (getNuxSelectFriendsPojo().isModuleToggle())
		{

			setCurrentState(NUX_IS_ACTIVE);

			activity.startActivity(Utils.getHomeActivityIntent(activity));
			activity.finish();

		}
		else
		{
			activity.startActivity(IntentFactory.openNuxFriendSelector(activity));
		}
	}

	public void saveNUXContact(HashSet<String> msisdn)
	{

		listNuxContacts.addAll(msisdn);

		mprefs.saveData(CURRENT_NUX_CONTACTS, listNuxContacts.toString().replace("[", "").replace("]", ""));
	}

	public void removeNUXContact(HashSet<String> msisdn)
	{

		listNuxContacts.removeAll(msisdn);

		mprefs.saveData(CURRENT_NUX_CONTACTS, listNuxContacts.toString().replace("[", "").replace("]", ""));

	}

	public void addUnlockedNUXContact(HashSet<String> msisdns)
	{
		unlockedNUXContacts.addAll(msisdns);

		mprefs.saveData(UNLOCKED_NUX_CONTACTS, unlockedNUXContacts.toString().replace("[", "").replace("]", ""));

	}

	public int getCountLockedContacts()
	{
		return listNuxContacts.size();
	}

	public boolean isContactLocked(String msisdn)
	{
		return listNuxContacts.contains(msisdn);
	}

	public void shutDownNUX()
	{
		mprefs.deleteAllData();
		clearALLPojo();
		listNuxContacts.clear();
		unlockedNUXContacts.clear();
	}

	private void clearALLPojo()
	{
		selectFriends = null;
		taskDetails = null;
		inviteFriends = null;
		customMessage = null;
		chatReward=null;

	}

	public HashSet<String> getLockedContacts()
	{
		return listNuxContacts;
	}

	public HashSet<String> getUnlockedContacts()
	{
		return unlockedNUXContacts;
	}

	
	/**
	 * 
	 * Sending a Multiforward message to all the contacts selected.
	 * 
	 * @param msisdn
	 * @param message
	 */
	public void sendMessage(ArrayList<String> msisdn, String message)
	{
		ConvMessage convMessage = null;

		ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>(msisdn.size());

		ArrayList<ConvMessage> messageList = new ArrayList<ConvMessage>();

		for (String number : msisdn)
		{
			contactList.add(ContactManager.getInstance().getContact(number, true, true, false));
			
		}

		convMessage = Utils.makeConvMessage(null, message, true);
//		convMessage.setMessage(message);
//		long mTimestamp=System.currentTimeMillis()/1000;
//		convMessage.setTimestamp(mTimestamp);
		//Logger.d("prettyTime","send Message "+mTimestamp+"");
		messageList.add(convMessage);
		MultipleConvMessage multiConvMessages = new MultipleConvMessage(messageList, contactList, System.currentTimeMillis() / 1000 , true, null);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_SENT, multiConvMessages);
	}

	public void parseNuxPacket(String json)
	{

		try
		{
			// Testing string will change to json.
			JSONObject root = new JSONObject(json);
			// "{  \"t\": \"nux\",  \"st\": \"new\", \"d\":{ \"task\": {    \"incentive_id\": \"8976rtfghfre456789iuhju\",    \"activity_id\": \"b7657yghh7tyhgtyghftt\",    \"incr_max\": -3,    \"incr_min\": 4,    \"max\": 6,    \"min\": 3  },  \"s\": {    \"s1\": {      \"ttl\": \"main_title\",      \"txt\": \"abcd\",      \"nxTgl\": false,      \"im\": \"images\",      \"btnTxt\": \"abcd\"    },    \"s2\": {      \"ttl1\": \"section_title\",      \"ttl2\": \"section_title\",      \"ttl3\": \"section_title\",      \"srchTgl\": true,      \"recTitle\": \"My Recoomendations\",      \"recList\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"cntctTyp\": \"5values\",      \"hdeLst\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"nxTxt\": \"next\"    },    \"s3\": {      \"scrnTgl\": true,      \"defMsg\": \"message\",      \"hnt\": \"tap to do something\",      \"stkrLst\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"nxTxt\": \"next\"    },    \"s4\": {      \"mdlTgl\": true,      \"rwdCrdTxt\": \"image\",      \"rwdCrdTxtSccss\": \"image\",      \"sttsTxt\": \"image\",      \"chtWtngTxt\": \"image\",      \"pndngChtIcn\": \"image\",      \"dtlsTxt\": \"image\",      \"dtlsLnk\": \"image\",      \"bttn1Txt\": \"image\",      \"bttn2Txt\": \"image\",      \"tpToClmLnk\": \"image\"    },    \"s5\": {      \"hkImg\": \"image\",      \"hkTxt\": \"image\",      \"nonHkImg\": \"image\",      \"nonHkTxt\": \"image\",      \"hkBttnTxt\": \"image\",      \"hkStkID\": \"image\"    }  }}}");

			if (root.optString(HikeConstants.SUB_TYPE).equals(SUBTYPE_NEW))
			{

				shutDownNUX();
				if (!TextUtils.isEmpty(root.optString(HikeConstants.DATA)))
				{

					JSONObject data = root.optJSONObject(HikeConstants.DATA);
					if (!TextUtils.isEmpty(data.optString(TASK_DETAILS)))
					{
						parseTaskDetails(data.optString(TASK_DETAILS));
					}
					JSONObject mmJsonObject = data.optJSONObject(SCREENS);
					if (mmJsonObject != null)
					{
						if (!TextUtils.isEmpty(mmJsonObject.optString(INVITE_FRIENDS)))
							parseInviteFriends(mmJsonObject.optString(INVITE_FRIENDS));

						if (!TextUtils.isEmpty(mmJsonObject.optString(SELECT_FRIENDS)))
							parseSelectFriends(mmJsonObject.optString(SELECT_FRIENDS));

						if (!TextUtils.isEmpty(mmJsonObject.optString(CUSTOM_MESSAGE)))
							parseCustomMessage(mmJsonObject.optString(CUSTOM_MESSAGE));

						if (!TextUtils.isEmpty(mmJsonObject.optString(CHAT_REWARDS_BAR)))
							parseChatRewards(mmJsonObject.optString(CHAT_REWARDS_BAR));

					}
					NUXTaskDetails mmDetails = getNuxTaskDetailsPojo();
					getNuxInviteFriendsPojo();
				
					// Check for min and max should not be zero.
					
					if(isNUXValid())
					{
						if (mmJsonObject.has(NOTIFICATION_PKT))
							showPush(mmJsonObject.getString(NOTIFICATION_PKT));
						//mprefs.saveData(REMINDER_RECEIVED, true);
						setCurrentState(NUX_NEW);
					}
				}
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(KILLNUX))
			{
				if (root.has(HikeConstants.DATA))
				{
					JSONObject data = root.optJSONObject(HikeConstants.DATA);
					if (data.has(SCREENS))
					{
						JSONObject mmJsonObject = data.optJSONObject(SCREENS);
						if (mmJsonObject.has(NOTIFICATION_PKT))
						{
							if (!TextUtils.isEmpty(mmJsonObject.optString(NOTIFICATION_PKT)))
								showPush(mmJsonObject.getString(NOTIFICATION_PKT));
						}
					}
				}
				setCurrentState(NUX_KILLED);
				shutDownNUX();
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(REFRESH))
			{

				if (!(getCurrentState() == NUX_KILLED || getCurrentState() == COMPLETED))
				{
					clearALLPojo();
					if (!TextUtils.isEmpty(root.optString(HikeConstants.DATA)))
					{
						JSONObject data = root.optJSONObject(HikeConstants.DATA);

						if (!TextUtils.isEmpty(data.optString(TASK_DETAILS)))
							parseTaskDetails(data.optString(TASK_DETAILS));

						JSONObject mmJsonObject = data.optJSONObject(SCREENS);

						if (mmJsonObject != null)
						{
							if (!TextUtils.isEmpty(mmJsonObject.optString(INVITE_FRIENDS)))
								parseInviteFriends(mmJsonObject.optString(INVITE_FRIENDS));

							if (!TextUtils.isEmpty(mmJsonObject.optString(SELECT_FRIENDS)))
								parseSelectFriends(mmJsonObject.optString(SELECT_FRIENDS));

							if (!TextUtils.isEmpty(mmJsonObject.optString(CUSTOM_MESSAGE)))
								parseCustomMessage(mmJsonObject.optString(CUSTOM_MESSAGE));

							if (!TextUtils.isEmpty(mmJsonObject.optString(CHAT_REWARDS_BAR)))
								parseChatRewards(mmJsonObject.optString(CHAT_REWARDS_BAR));

							if (!TextUtils.isEmpty(mmJsonObject.optString(NOTIFICATION_PKT)))
								showPush(mmJsonObject.getString(NOTIFICATION_PKT));
						}

					}
					if (!isNUXValid())
					{
						shutDownNUX();
					}
				}
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(UNLOCK))
			{
				if (!TextUtils.isEmpty(root.optString(HikeConstants.DATA)))
				{
					JSONObject mmdata = root.optJSONObject(HikeConstants.DATA);
					if (mmdata.has(UNLOCK_PERSONS))
					{
						JSONArray mmArray = mmdata.getJSONArray(UNLOCK_PERSONS);
						HashSet<String> mmUnlockedPersons = new HashSet<String>();
						for (int i = 0; i < mmArray.length(); i++)
						{
							mmUnlockedPersons.add(mmArray.optString(i));
						}
						addUnlockedNUXContact(mmUnlockedPersons);
						removeNUXContact(mmUnlockedPersons);
					}
					if (mmdata.has(HikeConstants.METADATA))
					{
						JSONObject mmMetaData = mmdata.getJSONObject(HikeConstants.METADATA);
						if (mmMetaData.optBoolean(SHOW_REWARDS))
						{
							setCurrentState(COMPLETED);
						}
					}
					JSONObject mmJsonObject = mmdata.optJSONObject(SCREENS);
					if (mmJsonObject != null)
					{
						if (!TextUtils.isEmpty(mmJsonObject.optString(NOTIFICATION_PKT)))
							showPush(mmJsonObject.getString(NOTIFICATION_PKT));
					}
				}
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(REMINDER))
			{
				if (!(getCurrentState() == NUX_KILLED))
				{

					if (root.has(HikeConstants.DATA))
					{
						JSONObject data = root.optJSONObject(HikeConstants.DATA);
						if (data.has(SCREENS))
						{
							JSONObject mmJsonObject = data.optJSONObject(SCREENS);
							if (mmJsonObject.has(NOTIFICATION_PKT))
							{
								if (!TextUtils.isEmpty(mmJsonObject.getString(NOTIFICATION_PKT)))
									showPush(mmJsonObject.getString(NOTIFICATION_PKT));
							}
						}
					}
				
				}
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private boolean isNUXValid()
	{
		NUXTaskDetails mmDetails = getNuxTaskDetailsPojo();
	
	Logger.d("ContactManager.getInstance()",ContactManager.getInstance().getAllContacts(true).size()+"");
		
		if (mmDetails.getMin() == 0
				|| mmDetails.getMax() == 0
				|| mmDetails.getMin() > mmDetails.getMax()
				||ContactManager.getInstance().getAllContacts(true).size()<mmDetails.getMin())
		{
			return false;
		}
		return true;
	}
	
	private void parseTaskDetails(String json)
	{
		JSONObject task_details = null;
		try
		{
			JSONObject newTaskDetails = new JSONObject(json);
			if (!TextUtils.isEmpty(mprefs.getData(TASK_DETAILS, "")))
				task_details = new JSONObject(mprefs.getData(TASK_DETAILS, ""));
			else
				task_details = new JSONObject();

			if (newTaskDetails.has(TD_INCENTIVE_ID))
			{
				task_details.put(TD_INCENTIVE_ID, newTaskDetails.getJSONObject(TD_INCENTIVE_ID));
			}

			if (newTaskDetails.has(TD_ACTIVITY_ID))
			{
				task_details.put(TD_ACTIVITY_ID, newTaskDetails.getJSONObject(TD_ACTIVITY_ID));
			}

			if (newTaskDetails.has(TD_MIN_CONTACTS))
			{
				task_details.put(TD_MIN_CONTACTS, newTaskDetails.getInt(TD_MIN_CONTACTS));
			}
			if (newTaskDetails.has(TD_MAX_CONTACTS))
			{
				task_details.put(TD_MAX_CONTACTS, newTaskDetails.getInt(TD_MAX_CONTACTS));
			}
			if(newTaskDetails.has(TD_PKT_CODE))
			{
				mprefs.saveData(TD_PKT_CODE, newTaskDetails.getLong(TD_PKT_CODE));
			}
		
			mprefs.saveData(TASK_DETAILS, task_details.toString());

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	private void parseInviteFriends(String json)
	{
		JSONObject inviteFriends = null;
		try
		{
			JSONObject newinviteFriends = new JSONObject(json);
			if (!TextUtils.isEmpty(mprefs.getData(INVITE_FRIENDS, "")))
				inviteFriends = new JSONObject(mprefs.getData(INVITE_FRIENDS, ""));
			else
				inviteFriends = new JSONObject();

			if (newinviteFriends.has(INVITEFRDS_BUT_TEXT))
			{
				inviteFriends.put(INVITEFRDS_BUT_TEXT, newinviteFriends.getString(INVITEFRDS_BUT_TEXT));
			}

			if (newinviteFriends.has(INVITEFRDS_IMAGE))
			{
				inviteFriends.put(INVITEFRDS_IMAGE, newinviteFriends.getString(INVITEFRDS_IMAGE));
			}

			if (newinviteFriends.has(INVITEFRDS_MAIN_TITLE))
			{
				inviteFriends.put(INVITEFRDS_MAIN_TITLE, newinviteFriends.getString(INVITEFRDS_MAIN_TITLE));
			}

			if (newinviteFriends.has(INVITEFRDS_SKIP_TOGGLE_BUTTON))
			{
				inviteFriends.put(INVITEFRDS_SKIP_TOGGLE_BUTTON, newinviteFriends.getBoolean(INVITEFRDS_SKIP_TOGGLE_BUTTON));
			}

			if (newinviteFriends.has(INVITEFRDS_TEXT))
			{
				inviteFriends.put(INVITEFRDS_TEXT, newinviteFriends.getString(INVITEFRDS_TEXT));
			}
			if (newinviteFriends.has(INVITE_NUX_IS_SKIPPABLE))
			{
				inviteFriends.put(INVITE_NUX_IS_SKIPPABLE, newinviteFriends.getBoolean(INVITE_NUX_IS_SKIPPABLE));
			}

			mprefs.saveData(INVITE_FRIENDS, inviteFriends.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	public void parseCustomMessage(String json)
	{
		JSONObject customMessage = null;
		try
		{
			JSONObject newCustomMessage = new JSONObject(json);

			if (!TextUtils.isEmpty(mprefs.getData(CUSTOM_MESSAGE, "")))
				customMessage = new JSONObject(mprefs.getData(CUSTOM_MESSAGE, ""));
			else
				customMessage = new JSONObject();

			if (newCustomMessage.has(CM_BUTTON_TEXT))
			{
				customMessage.put(CM_BUTTON_TEXT, newCustomMessage.getString(CM_BUTTON_TEXT));
			}

			if (newCustomMessage.has(CM_DEF_MESSAGE))
			{
				customMessage.put(CM_DEF_MESSAGE, newCustomMessage.getString(CM_DEF_MESSAGE));
			}

			if (newCustomMessage.has(CM_HINT))
			{
				customMessage.put(CM_HINT, newCustomMessage.getString(CM_HINT));
			}

			if (newCustomMessage.has(CM_SCREEN_TOGGLE))
			{
				customMessage.put(CM_SCREEN_TOGGLE, newCustomMessage.getBoolean(CM_SCREEN_TOGGLE));
			}

			mprefs.saveData(CUSTOM_MESSAGE, customMessage.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	public void parseSelectFriends(String json)
	{
		try
		{
			JSONObject selectFriend = null;
			JSONObject newselectFriend = new JSONObject(json);

			if (!TextUtils.isEmpty(mprefs.getData(SELECT_FRIENDS, "")))
				selectFriend = new JSONObject(mprefs.getData(SELECT_FRIENDS, ""));
			else
				selectFriend = new JSONObject();

			if (newselectFriend.has(SF_BUTTON_TEXT))
				selectFriend.put(SF_BUTTON_TEXT, newselectFriend.getString(SF_BUTTON_TEXT));

			if (newselectFriend.has(SF_CONTACT_SECTION_TYPE))
				selectFriend.put(SF_CONTACT_SECTION_TYPE, newselectFriend.getInt(SF_CONTACT_SECTION_TYPE));

			if (newselectFriend.has(SF_HIDE_LIST))
				selectFriend.put(SF_HIDE_LIST, newselectFriend.getJSONArray(SF_HIDE_LIST));

			if (newselectFriend.has(SF_RECO_TOGGLE))
				selectFriend.put(SF_RECO_TOGGLE, newselectFriend.getBoolean(SF_RECO_TOGGLE));

			if (newselectFriend.has(SF_RECO_SECTION_TITLE))
				selectFriend.put(SF_RECO_SECTION_TITLE, newselectFriend.getString(SF_RECO_SECTION_TITLE));

			if (newselectFriend.has(SF_MODULE_TOGGLE))
				selectFriend.put(SF_MODULE_TOGGLE, newselectFriend.getBoolean(SF_MODULE_TOGGLE));

			if (newselectFriend.has(SF_SECTION_TITLE))
				selectFriend.put(SF_SECTION_TITLE, newselectFriend.getString(SF_SECTION_TITLE));

			if (newselectFriend.has(SF_SECTION_TITLE2))
				selectFriend.put(SF_SECTION_TITLE2, newselectFriend.getString(SF_SECTION_TITLE2));

			if (newselectFriend.has(SF_SECTION_TITLE3))
				selectFriend.put(SF_SECTION_TITLE3, newselectFriend.getString(SF_SECTION_TITLE3));

			mprefs.saveData(SELECT_FRIENDS, selectFriend.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	public void parseChatRewards(String json)
	{
		try
		{
			JSONObject chatReward = null;
			JSONObject newChatReward = new JSONObject(json);

			if (!TextUtils.isEmpty(mprefs.getData(TASK_DETAILS, "")))
				chatReward = new JSONObject(mprefs.getData(TASK_DETAILS, ""));
			else
				chatReward = new JSONObject();

			if (newChatReward.has(CR_INVITE_MORE_TEXT))
				chatReward.put(CR_INVITE_MORE_TEXT, newChatReward.getString(CR_INVITE_MORE_TEXT));

			if (newChatReward.has(CR_REMIND_TEXT))
				chatReward.put(CR_REMIND_TEXT, newChatReward.getString(CR_REMIND_TEXT));

			if (newChatReward.has(CR_CHAT_WAITING_TEXT))
				chatReward.put(CR_CHAT_WAITING_TEXT, newChatReward.getString(CR_CHAT_WAITING_TEXT));

			if (newChatReward.has(CR_DETAILS_LINK))
				chatReward.put(CR_DETAILS_LINK, newChatReward.getString(CR_DETAILS_LINK));

			if (newChatReward.has(CR_DETAILS_TEXT))
				chatReward.put(CR_DETAILS_TEXT, newChatReward.getString(CR_DETAILS_TEXT));

			if (newChatReward.has(CR_PENDINGCHAT_ICON))
				chatReward.put(CR_PENDINGCHAT_ICON, newChatReward.getString(CR_PENDINGCHAT_ICON));

			if (newChatReward.has(CR_REWARD_CARD_SUCCESS_TEXT))
				chatReward.put(CR_REWARD_CARD_SUCCESS_TEXT, newChatReward.getString(CR_REWARD_CARD_SUCCESS_TEXT));

			if (newChatReward.has(CR_REWARD_CARD_TEXT))
				chatReward.put(CR_REWARD_CARD_TEXT, newChatReward.getString(CR_REWARD_CARD_TEXT));

			if (newChatReward.has(CR_STATUS_TEXT))
				chatReward.put(CR_STATUS_TEXT, newChatReward.getString(CR_STATUS_TEXT));

			if (newChatReward.has(CR_TAPTOCLAIM))
				chatReward.put(CR_TAPTOCLAIM, newChatReward.getString(CR_TAPTOCLAIM));

			if (newChatReward.has(CR_TAPTOCLAIMTEXT))
				chatReward.put(CR_TAPTOCLAIMTEXT, newChatReward.getString(CR_TAPTOCLAIMTEXT));

			if (newChatReward.has(CR_SELECTFRIENDS))
				chatReward.put(CR_SELECTFRIENDS, newChatReward.getString(CR_SELECTFRIENDS));
			
			mprefs.saveData(CHAT_REWARDS_BAR, chatReward.toString());
			

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public NuxCustomMessage getNuxCustomMessagePojo()
	{

		if (customMessage == null)
		{
			JSONObject custommessage = null;
			try
			{
				if (TextUtils.isEmpty(mprefs.getData(CUSTOM_MESSAGE, "")))
				{
					custommessage = new JSONObject();
				}
				else
				{
					custommessage = new JSONObject(mprefs.getData(CUSTOM_MESSAGE, ""));
				}
				if (!TextUtils.isEmpty(custommessage.toString()))
				{
					String customText = custommessage.optString(CM_DEF_MESSAGE, context.getString(R.string.custom_message));
					String indicatorText = custommessage.optString(CM_HINT, context.getString(R.string.custom_message_hint));
					String buttext = custommessage.optString(CM_BUTTON_TEXT, context.getString(R.string.custome_message_send_button));
					boolean togglecustommsg = custommessage.optBoolean(CM_SCREEN_TOGGLE, false);
					customMessage = new NuxCustomMessage(customText, indicatorText, buttext, togglecustommsg);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return customMessage;
	}

	/**
	 * 
	 * @return
	 * 
	 * POJO for task details
	 */
	public NUXTaskDetails getNuxTaskDetailsPojo()
	{
		if (taskDetails == null)
		{
			try
			{
				JSONObject task_details = null;
				if (!TextUtils.isEmpty(mprefs.getData(TASK_DETAILS, "")))
					task_details = new JSONObject(mprefs.getData(TASK_DETAILS, ""));
				else
					task_details = new JSONObject();

				String incentiveId = null;
				int incentiveAmount = 0;
				String activityId = null;
				JSONObject incentive = task_details.optJSONObject(TD_INCENTIVE_ID);
				if (incentive != null)
				{
					incentiveId = incentive.optString(ID);
					if (incentive.optJSONObject(TYPE) != null)
					{
						incentiveAmount = incentive.optInt(INCENTIVE_AMOUNT, context.getResources().getInteger(R.integer.incentive_amount));
					}

				}

				JSONObject activity = task_details.optJSONObject(TD_ACTIVITY_ID);
				if (activity != null)
				{
					activityId = activity.optString(ID);
				}

				int min = task_details.optInt(TD_MIN_CONTACTS, context.getResources().getInteger(R.integer.nux_min_contacts));
				int max = task_details.optInt(TD_MAX_CONTACTS, context.getResources().getInteger(R.integer.nux_max_contacts));
				taskDetails = new NUXTaskDetails(incentiveId, activityId, min, max, incentiveAmount);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return taskDetails;
	}

	
	/**
	 * 
	 * @return
	 * 
	 * POJO for Friend Selector screen
	 */
	public NuxSelectFriends getNuxSelectFriendsPojo()
	{
		if (selectFriends == null)
		{
			JSONObject select_friends = null;
			try
			{

				if (!TextUtils.isEmpty(mprefs.getData(SELECT_FRIENDS, "")))
					select_friends = new JSONObject(mprefs.getData(SELECT_FRIENDS, ""));
				else
					select_friends = new JSONObject();

				{
					JSONArray mmArray = null;
					Set<String> recoList = new HashSet<String>();
					ArrayList<String> hideList = new ArrayList<String>();
					String sectionTitle = select_friends.optString(SF_SECTION_TITLE, context.getString(R.string.nux_select_friends_1));
					String recoSectionTitle = select_friends.optString(SF_RECO_SECTION_TITLE, context.getString(R.string.nux_select_friends_reconame));

					if (select_friends.optBoolean(SF_RECO_TOGGLE))
					{
						HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();

						String mymsisdn = settings.getData(HikeMessengerApp.MSISDN_SETTING, "");
						recoList = Utils.getServerRecommendedContactsSelection(settings.getData(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), mymsisdn);
					}
					if (select_friends.has(SF_HIDE_LIST))
					{
						mmArray = select_friends.optJSONArray(SF_HIDE_LIST);

						for (int i = 0; i < mmArray.length(); i++)
						{
							hideList.add(mmArray.optString(i));
						}
					}
					String butText = select_friends.optString(SF_BUTTON_TEXT, context.getString(R.string.nux_select_friends_nextbut));
					String title2 = select_friends.optString(SF_SECTION_TITLE2, context.getString(R.string.nux_select_friends_2));
					String title3 = select_friends.optString(SF_SECTION_TITLE3, context.getString(R.string.nux_select_friends_3));
					boolean moduleToggle = select_friends.optBoolean(SF_MODULE_TOGGLE,false);
					int contactSectionType = select_friends.optInt(SF_CONTACT_SECTION_TYPE);
					selectFriends = new NuxSelectFriends(sectionTitle, title2, title3, recoSectionTitle, recoList, hideList, butText, moduleToggle, contactSectionType);
				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return selectFriends;
	}

	/**
	 * 
	 * @return
	 * 
	 * POJO for nux start screen ,i.e invite friends
	 */
	public NuxInviteFriends getNuxInviteFriendsPojo()
	{
		JSONObject incentive_reward = null;
		if (inviteFriends == null)
		{
			try
			{
				if (!TextUtils.isEmpty(mprefs.getData(INVITE_FRIENDS, "")))
					incentive_reward = new JSONObject(mprefs.getData(INVITE_FRIENDS, ""));
				else
					incentive_reward = new JSONObject();
				{

					String text = incentive_reward.optString(INVITEFRDS_TEXT, context.getString(R.string.nux_invitefrnds_objective));
					String image = incentive_reward.optString(INVITEFRDS_IMAGE);
					boolean showSkipButton = incentive_reward.optBoolean(INVITEFRDS_SKIP_TOGGLE_BUTTON, true);
					String title = incentive_reward.optString(INVITEFRDS_MAIN_TITLE, context.getString(R.string.nux_invitefrnds_reward));
					String buttext = incentive_reward.optString(INVITEFRDS_BUT_TEXT, context.getString(R.string.nux_invitefrnds_buttext));
					boolean isNuxSkippable = incentive_reward.optBoolean(INVITE_NUX_IS_SKIPPABLE, true);
					inviteFriends = new NuxInviteFriends(title, text, buttext, image, showSkipButton,isNuxSkippable);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return inviteFriends;
	}

	/**
	 * 
	 * @return
	 * 
	 * POJO for footer in the conversation list
	 */
	
	public NUXChatReward getNuxChatRewardPojo()
	{
		if (chatReward == null)
		{
			JSONObject chatrewardobj = null;
			try
			{
				if (!TextUtils.isEmpty(mprefs.getData(CHAT_REWARDS_BAR, "")))
					chatrewardobj = new JSONObject(mprefs.getData(CHAT_REWARDS_BAR, ""));
				else
					chatrewardobj = new JSONObject();
				{

					String rewardCardText = chatrewardobj.optString(CR_REWARD_CARD_TEXT, context.getString(R.string.reward_card_objective));
					String rewardCardSuccessText = chatrewardobj.optString(CR_REWARD_CARD_SUCCESS_TEXT, context.getString(R.string.reward_card_success));
					String statusText = chatrewardobj.optString(CR_STATUS_TEXT, context.getString(R.string.status_text));
					String chatWaitingText = chatrewardobj.optString(CR_CHAT_WAITING_TEXT, context.getString(R.string.chat_waiting_text));
					String pendingChatIcon = chatrewardobj.optString(CR_PENDINGCHAT_ICON);
					String detailsText = chatrewardobj.optString(CR_DETAILS_TEXT, context.getString(R.string.details_text));
					String detailsLink = chatrewardobj.optString(CR_DETAILS_LINK,VIEW_DETAILS_URL);
					String inviteMore = chatrewardobj.optString(CR_INVITE_MORE_TEXT, context.getString(R.string.nux_invite_more));
					String remind = chatrewardobj.optString(CR_REMIND_TEXT, context.getString(R.string.nux_remind));
					String tapToClaimLink = chatrewardobj.optString(CR_TAPTOCLAIM,TAP_CLAIM_URL);
					String tapToClaimText = chatrewardobj.optString(CR_TAPTOCLAIMTEXT, context.getString(R.string.tap_to_claim));
					String selectFriends = chatrewardobj.optString(CR_SELECTFRIENDS, context.getString(R.string.select_friends));
					chatReward = new NUXChatReward(rewardCardText, rewardCardSuccessText, statusText, chatWaitingText, pendingChatIcon, detailsText, detailsLink,
							inviteMore, remind, tapToClaimLink, tapToClaimText, selectFriends);

				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return chatReward;
	}

	public void setCurrentState(int mmCurrentState)
	{
		mprefs.saveData(CURRENT_NUX_ACTIVITY, mmCurrentState);
	}

	public int getCurrentState()
	{
		return mprefs.getData(CURRENT_NUX_ACTIVITY, NUX_KILLED);
	}

	public int getCountUnlockedContacts()
	{
		return unlockedNUXContacts.size();
	}

	public void sendMsisdnListToServer(HashSet<String> msisdn)
	{
		
		JSONObject root = new JSONObject();
		try
		{
			root.put(HikeConstants.TYPE, HikeConstants.NUX);
			root.put(HikeConstants.SUB_TYPE, INVITE);
			JSONArray mmArray = new JSONArray(msisdn);
			JSONObject object = new JSONObject();
			object.put(INVITE_ARRAY, mmArray);
			root.put(HikeConstants.DATA, object);

			HikeMqttManagerNew.getInstance().sendMessage(root, MqttConstants.MQTT_QOS_ONE);

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	public boolean isReminderReceived()
	{
		return mprefs.getData(REMINDER_RECEIVED, false);
	}

	public void reminderShown()
	{
		mprefs.saveData(REMINDER_RECEIVED, false);
		mprefs.saveData(REMINDER_NORMAL, false);
	}

	
	

	public boolean isReminderNormal()
	{
		return mprefs.getData(REMINDER_NORMAL, false);
	}
	
	public boolean wantToInfalte()
	{
		NuxSelectFriends mmSelectFriends = getNuxSelectFriendsPojo();
		if ((mmSelectFriends.isModuleToggle() || getCurrentState() == NUXConstants.COMPLETED||isReminderNormal()))
		{
			return true;
		}
		return false;
	}
	
	
	/**
	 * showing push notifications.
	 * @param json
	 */
	public void showPush(String json)
	{
		try
		{
			JSONObject mmNotification = new JSONObject(json);
			if (mmNotification.has(PUSH_TEXT) && mmNotification.has(PUSH_TYPE) && mmNotification.has(PUSH_TITLE))
			{
				String pushTitle = mmNotification.getString(PUSH_TEXT);
				String pushText = mmNotification.getString(PUSH_TITLE);
				int pushType = mmNotification.getInt(PUSH_TYPE);

				{
					switch (PushTypeEnum.getEnumValue(pushType))
					{
					case PUSH:
						HikeNotification.getInstance().notifyUserAndOpenHomeActivity(pushText, pushTitle, false);
						break;
					case SILENT:
						HikeNotification.getInstance().notifyUserAndOpenHomeActivity(pushText, pushTitle, true);
						break;
					case NONE:
					case UNKNOWN:
						break;
					}
				}

				if (mmNotification.has(PUSH_REWARD_CARD_TYPE))
				{
					int rewardType = mmNotification.getInt(PUSH_REWARD_CARD_TYPE);
					switch (RewardTypeEnum.getEnumValue(rewardType))
					{
					case COMPRESSED:
						reminderShown();
						break;
					case EXPANDED:
						mprefs.saveData(REMINDER_RECEIVED, true);
						mprefs.saveData(REMINDER_NORMAL, false);
						break;
					case NORMAL:
						mprefs.saveData(REMINDER_RECEIVED, true);
						mprefs.saveData(REMINDER_NORMAL, true);
					case UNKNOWN:
						break;
					}
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	

	/**
	 * 
	 * @return
	 * 
	 * decides to show the nux or not.
	 */
	public boolean showNuxScreen()
	{
		if (NUXManager.getInstance().getCurrentState() == NUXConstants.NUX_NEW)
		{
			if (NUXManager.getInstance().getNuxInviteFriendsPojo().isNuxSkippable())
			{
				NUXManager.getInstance().setCurrentState(NUXConstants.NUX_SKIPPED);
			}
			return true;
			
		}
		return false;
	}
	
	public void sendAnalytics(JSONObject metaData)
	{
		if (metaData != null)
		{
			try
			{
				metaData.put(TD_PKT_CODE, mprefs.getData(TD_PKT_CODE, 0l));
				Logger.d("Analytics", metaData.toString());
				HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, EventPriority.HIGH, metaData);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
	
	
	/**
	 * All these are testing functions will be removed afterwards.
	 */
	public void removeData()
	{
		HikeSharedPreferenceUtil.getInstance().removeData(CURRENT_NUX_CONTACTS);
	}

	public void putJsonData()
	{
		JSONObject root = new JSONObject();
		JSONObject incentive_reward = new JSONObject();

		JSONObject select_friends = new JSONObject();
		try
		{
			incentive_reward.put(INVITEFRDS_TEXT, "abcd");
			incentive_reward.put(TD_INCENTIVE_ID, 1);
			incentive_reward.put(TD_ACTIVITY_ID, 2);
			incentive_reward.put(TD_INCR_MAX, -3);
			incentive_reward.put(TD_INCR_MIN, 4);
			incentive_reward.put(INVITEFRDS_BUT_TEXT, "abcd");
			incentive_reward.put(INVITEFRDS_IMAGE, "");
			incentive_reward.put(INVITEFRDS_SKIP_TOGGLE_BUTTON, false);
			incentive_reward.put(INVITEFRDS_MAIN_TITLE, "main_title");
			root.put(INVITE_FRIENDS, incentive_reward);

			select_friends.put(SF_BUTTON_TEXT, "button_text");
			select_friends.put(SF_CONTACT_SECTION_TOGGLE, true);
			select_friends.put(SF_SECTION_TITLE2, "progress_text");
			select_friends.put(SF_RECO_SECTION_TITLE, "My Recoomendations");
			select_friends.put(SF_MODULE_TOGGLE, true);
			select_friends.put(SF_SECTION_TITLE, "section_title");

			JSONArray mmarray = new JSONArray();
			mmarray.put("+91-987656");
			mmarray.put("+91-273623");
			mmarray.put("+92827722");

			select_friends.put(SF_RECO_TOGGLE, mmarray);

			root.put(SELECT_FRIENDS, select_friends);

			JSONObject custommessage = new JSONObject();
			custommessage.put(CM_BUTTON_TEXT, "bu_text");
			custommessage.put(CM_DEF_MESSAGE, "screen tilit");
			custommessage.put(CM_HINT, true);

			root.put(CUSTOM_MESSAGE, custommessage);
			Logger.d(TAG, root.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}



}
