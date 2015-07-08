package com.bsb.hike.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeMqttManagerNew;

public class OneToNConversationUtils
{
	private static final String TAG = "OneToNConversationUtils";
	
	public static String getParticipantAddedMessage(ConvMessage convMessage, Context context, String highlight)
	{
		String participantAddedMessage;
		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isBroadcastConversation())
		{
			if (metadata.isNewBroadcast())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_broadcast_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_broadcast_message), highlight);
			}
		}
		else
		{
			String groupAdder = metadata.getGroupAdder();
			SharedPreferences preferences = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,context. MODE_PRIVATE);
			if (metadata.isNewGroup())
			{
				if (groupAdder == null) {
					 participantAddedMessage = String.format(context.getString(R.string.new_group_message), highlight);
				} else {
					String adder = "";
					if (groupAdder != null && groupAdder.trim().length() > 0) {
						ContactInfo contact = ContactManager.getInstance()
								.getContact(groupAdder, true, false);
						if (contact != null) {
							adder = contact.getFirstNameAndSurname();
						}
					}
					if(groupAdder.equalsIgnoreCase( preferences.getString(HikeMessengerApp.MSISDN_SETTING, ""))){
						participantAddedMessage = context.getString(
								R.string.created_group_text, highlight);
					}else{
					    participantAddedMessage = adder
								+ " "
								+ context.getString(R.string.group_member_added,
										highlight);
					}
				
				}
			}
			else
          {
				if (groupAdder == null) {
					 participantAddedMessage = String.format(context.getString(R.string.add_to_group_message), highlight);
				} else {
					String adder = "";
					if (groupAdder.equalsIgnoreCase(preferences.getString(
							HikeMessengerApp.MSISDN_SETTING, ""))) {
						adder = context.getString(R.string.you);
					} else {
						if (groupAdder != null
								&& groupAdder.trim().length() > 0) {
							ContactInfo contact = ContactManager.getInstance()
									.getContact(groupAdder, true, false);
							if (contact != null) {
								adder = contact.getFirstNameAndSurname();
							}
						}
					}
					participantAddedMessage = adder
							+ " "
							+ context.getString(R.string.group_member_added,
									highlight);
				}
			}
		}
		return participantAddedMessage;
	}
	
	public static String getAdminUpdatedMessage(ConvMessage convMessage,
			Context context) {
		String participantAddedMessage = null;
		if (!convMessage.isBroadcastMessage()) {
			String groupAdder = convMessage.getGroupParticipantMsisdn();
			String highlight = convMessage.getMetadata().getMsisdn();
			SharedPreferences preferences = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, context.MODE_PRIVATE);

			if (highlight != null) {
				if (highlight.equalsIgnoreCase(preferences.getString(
						HikeMessengerApp.MSISDN_SETTING, ""))) {
					highlight = context.getString(R.string.you).toLowerCase();
				}else{
				ContactInfo contact = ContactManager.getInstance().getContact(
						highlight, true, false);
				if (contact != null) {
					highlight = contact.getFirstNameAndSurname();
				}
				}
			}
		
			String adder = "";
			if (groupAdder.equalsIgnoreCase(preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, ""))) {
				adder = context.getString(R.string.you);
			} else {
				if (groupAdder != null && groupAdder.trim().length() > 0) {
					ContactInfo contact = ContactManager.getInstance()
							.getContact(groupAdder, true, false);
					if (contact != null) {
						adder = contact.getFirstNameAndSurname();
					}
				}
			}
			participantAddedMessage = adder
					+ " "
					+ context
							.getString(R.string.group_admin_updated, highlight);
		}

		return participantAddedMessage;
	}

	public static String getSettingUpdatedMessage(ConvMessage convMessage,
			Context context) {
		String participantAddedMessage = null;
		if (!convMessage.isBroadcastMessage()) {
			String groupAdder = convMessage.getGroupParticipantMsisdn();
			String highlight = convMessage.getMetadata().getMsisdn();
			SharedPreferences preferences = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, context.MODE_PRIVATE);

			if (highlight != null) {
				if (highlight.equalsIgnoreCase(preferences.getString(
						HikeMessengerApp.MSISDN_SETTING, ""))) {
					highlight = context.getString(R.string.you).toLowerCase();
				}else{
				ContactInfo contact = ContactManager.getInstance().getContact(
						highlight, true, false);
				if (contact != null) {
					highlight = contact.getFirstNameAndSurname();
				}
				}
			}
		
			String adder = "";
			if (groupAdder.equalsIgnoreCase(preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, ""))) {
				adder = context.getString(R.string.you);
				participantAddedMessage =  context
								.getString(R.string.you_group_settings_updated, highlight);
			} else {
				if (groupAdder != null && groupAdder.trim().length() > 0) {
					ContactInfo contact = ContactManager.getInstance()
							.getContact(groupAdder, true, false);
					if (contact != null) {
						adder = contact.getFirstNameAndSurname();
						participantAddedMessage = adder
								+ " "
								+ context
										.getString(R.string.group_settings_updated);
					}
				}
			}
			
		}

		return participantAddedMessage;
	}
	public static String getParticipantRemovedMessage(String msisdn, Context context, String participantName)
	{
		String participantRemovedMessage = String.format(context.getString(isBroadcastConversation(msisdn) ? R.string.removed_from_broadcast : R.string.left_conversation),
				participantName);
		return participantRemovedMessage;
	}

	public static String getConversationNameChangedMessage(String msisdn, Context context, String participantName)
	{
		String nameChangedMessage = String
				.format(context.getString(isBroadcastConversation(msisdn) ? R.string.change_broadcast_name : R.string.change_group_name), participantName);
		return nameChangedMessage;
	}

	public static String getConversationEndedMessage(String msisdn, Context context)
	{
		String message = context.getString(isBroadcastConversation(msisdn) ? R.string.broadcast_list_end : R.string.group_chat_end);
		return message;
	}

	public static boolean isOneToNConversation(String msisdn)
	{
		return isGroupConversation(msisdn) || isBroadcastConversation(msisdn);
	}

	public static void createGroupOrBroadcast(Activity activity, ArrayList<ContactInfo> selectedContactList, String convName, String convId, int setting)
	{
		String oneToNConvId;
		if (activity.getIntent().hasExtra(HikeConstants.Extras.BROADCAST_LIST))
		{
			oneToNConvId = activity.getIntent().getStringExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST);
		}
		else
		{
			oneToNConvId = activity.getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		}
		
		boolean newOneToNConv = false;

		if (TextUtils.isEmpty(oneToNConvId))
		{
			oneToNConvId = convId;
			if (TextUtils.isEmpty(oneToNConvId))
			{
				throw new IllegalArgumentException("No convId set.! Conversation cannot be created.");
			}
			newOneToNConv = true;
		}
		else
		{
			// Group alredy exists. Fetch existing participants.
			newOneToNConv = false;
		}
		Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();

		for (ContactInfo particpant : selectedContactList)
		{
			GroupParticipant convParticipant = new GroupParticipant(particpant, convId);
			participantList.put(particpant.getMsisdn(), new PairModified<GroupParticipant, String>(convParticipant, convParticipant.getContactInfo().getNameOrMsisdn()));
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE));

		OneToNConversation oneToNConversation;

		if (activity.getIntent().hasExtra(HikeConstants.Extras.CREATE_BROADCAST))
		{
			oneToNConversation = new BroadcastConversation.ConversationBuilder(oneToNConvId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).setCreationTime(System.currentTimeMillis()).build();
		}
		else
		{
			oneToNConversation = new GroupConversation.ConversationBuilder(oneToNConvId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).setCreationTime(System.currentTimeMillis()).setConversationCreator(userContactInfo.getMsisdn()).build();
		}

		oneToNConversation.setConversationParticipantList(participantList);

		Logger.d(activity.getClass().getSimpleName(), "Creating group: " + oneToNConvId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addRemoveGroupParticipants(oneToNConvId, oneToNConversation.getConversationParticipantList(), false);
		if (newOneToNConv)
		{
			mConversationDb.addConversation(oneToNConversation.getMsisdn(), false, convName, oneToNConversation.getConversationOwner(), null, oneToNConversation.getCreationDate(), oneToNConversation.getConversationCreator());
			ContactManager.getInstance().insertGroup(oneToNConversation.getMsisdn(), convName);
			if (oneToNConversation instanceof GroupConversation)
			{
		    	mConversationDb.changeGroupSettings(oneToNConvId, setting,1, new ContentValues());
			}
		}

		try
		{
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = oneToNConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			if (oneToNConversation instanceof BroadcastConversation)
			{
				gcjPacket.put(HikeConstants.NEW_BROADCAST, newOneToNConv);
			}
			else if (oneToNConversation instanceof GroupConversation)
			{
				gcjPacket.put(HikeConstants.NEW_GROUP, newOneToNConv);
			}
			
			/*
			 * Adding request dp to the packet
			 */
			if (newOneToNConv)
			{
				JSONObject metadata = new JSONObject();
				if (oneToNConversation instanceof GroupConversation){
				metadata.put(HikeConstants.FROM, oneToNConversation.getConversationOwner());
				metadata.put(HikeConstants.GROUP_TYPE, HikeConstants.GROUPS_TYPE.MULTI_ADMIN);
				metadata.put(HikeConstants.GROUP_SETTING,setting);
				}
				metadata.put(HikeConstants.NAME, convName);

				String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
				String fileName = Utils.getTempProfileImageFileName(oneToNConvId);
				File groupImageFile = new File(directory, fileName);

				if (groupImageFile.exists())
				{
					metadata.put(HikeConstants.REQUEST_DP, true);
				}
				
				gcjPacket.put(HikeConstants.METADATA, metadata);
			
			} else if (oneToNConversation instanceof GroupConversation) {
				JSONObject metadata = new JSONObject();

				metadata.put(HikeConstants.FROM,
						oneToNConversation.getConversationOwner());
				metadata.put(HikeConstants.GROUP_TYPE, HikeConstants.GROUPS_TYPE.MULTI_ADMIN);
				metadata.put(HikeConstants.GROUP_SETTING,setting);
				
				gcjPacket.put(HikeConstants.METADATA, metadata);
			}
	
			ConvMessage msg = new ConvMessage(gcjPacket, oneToNConversation, activity, true);
			ContactManager.getInstance().updateGroupRecency(oneToNConvId, msg.getTimestamp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, msg);
			
			HikeMqttManagerNew.getInstance().sendMessage(gcjPacket, MqttConstants.MQTT_QOS_ONE);

			/**
			 * This is for updating the UI in ChatThread if it is not a new conversation. Also used for updating the default broadcast name on homescreen
			 */
			if (!newOneToNConv)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.PARTICIPANT_JOINED_ONETONCONV, gcjPacket);
				HikeMessengerApp.getPubSub().publish(HikePubSub.PARTICIPANT_JOINED_SYSTEM_MESSAGE, msg);
			}

			ContactInfo conversationContactInfo = new ContactInfo(oneToNConvId, oneToNConvId, oneToNConvId, oneToNConvId);
			Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(activity, conversationContactInfo, true);
			activity.startActivity(intent);
			activity.finish();

		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Getting a JSON Exception while creating a newgroup/broadcast : " + e.toString());
		}

	}

	/**
	 * To ensure that group Conversation and Broadcast conversation are mutually exclusive, we add the !isBroadCast check
	 * 
	 * @param msisdn
	 * @return
	 */
	public static boolean isGroupConversation(String msisdn)
	{
		return msisdn != null && !msisdn.startsWith("+") && !isBroadcastConversation(msisdn);
	}

	/**
	 * @param msisdn
	 * @return
	 */
	public static boolean isBroadcastConversation(String msisdn)
	{
		return msisdn != null && msisdn.startsWith("b:");
	}

	public static void addBroadcastRecipientConversations(ConvMessage convMessage)
	{

		ArrayList<ContactInfo> contacts = HikeConversationsDatabase.getInstance().addBroadcastRecipientConversations(convMessage);

		sendPubSubForConvScreenBroadcastMessage(convMessage, contacts);
	}

	public static void sendPubSubForConvScreenBroadcastMessage(ConvMessage convMessage, ArrayList<ContactInfo> recipient)
	{
		long firstMsgId = convMessage.getMsgID() + 1;
		int totalRecipient = recipient.size();
		List<Pair<ContactInfo, ConvMessage>> allPairs = new ArrayList<Pair<ContactInfo, ConvMessage>>(totalRecipient);
		long timestamp = System.currentTimeMillis() / 1000;
		for (int i = 0; i < totalRecipient; i++)
		{
			ConvMessage message = new ConvMessage(convMessage);
			if (convMessage.isBroadcastConversation())
			{
				message.setMessageOriginType(OriginType.BROADCAST);
			}
			else
			{
				// multi-forward case... in braodcast case we donot need to update timestamp
				message.setTimestamp(timestamp++);
			}
			message.setMsgID(firstMsgId + i);
			ContactInfo contactInfo = recipient.get(i);
			message.setMsisdn(contactInfo.getMsisdn());
			Pair<ContactInfo, ConvMessage> pair = new Pair<ContactInfo, ConvMessage>(contactInfo, message);
			allPairs.add(pair);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_DB_INSERTED, allPairs);
	}

	public static String getGroupCreationTimeAsString(Context context,
			long creationTime) {
		String format;
		format = "dd MMM ''yy";

		SimpleDateFormat df = new SimpleDateFormat(format);
		return df.format(creationTime);

	}
}
