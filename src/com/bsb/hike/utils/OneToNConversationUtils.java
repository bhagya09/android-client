package com.bsb.hike.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;

public class OneToNConversationUtils {

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
			if (metadata.isNewGroup())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_group_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_group_message), highlight);
			}
		}
		return participantAddedMessage;
	}
	
	public static String getParticipantRemovedMessage(String msisdn, Context context, String participantName)
	{
		String participantRemovedMessage = String.format(context.getString(isBroadcastConversation(msisdn) ? R.string.removed_from_broadcast : R.string.left_conversation), participantName);
		return participantRemovedMessage;
	}
	
	public static String getConversationNameChangedMessage(String msisdn, Context context, String participantName)
	{
		String nameChangedMessage = String.format(context.getString(isBroadcastConversation(msisdn) ? R.string.change_broadcast_name : R.string.change_group_name), participantName);
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

	public static void createGroupOrBroadcast(Activity activity, ArrayList<ContactInfo> selectedContactList, String convName)
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
			oneToNConvId = activity.getIntent().getStringExtra(HikeConstants.Extras.GROUP_BROADCAST_ID);
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
			GroupParticipant convParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), new PairModified<GroupParticipant, String>(convParticipant, convParticipant.getContactInfo().getNameOrMsisdn()));
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE));

		OneToNConversation oneToNConversation;
		if (activity.getIntent().hasExtra(HikeConstants.IS_BROADCAST))
		{
			oneToNConversation = new BroadcastConversation.ConversationBuilder(oneToNConvId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).build();
		}
		else
		{
			oneToNConversation = new GroupConversation.ConversationBuilder(oneToNConvId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).build();
		}
		oneToNConversation.setConversationParticipantList(participantList);

		Logger.d(activity.getClass().getSimpleName(), "Creating group: " + oneToNConvId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addRemoveGroupParticipants(oneToNConvId, oneToNConversation.getConversationParticipantList(), false);
		if (newOneToNConv)
		{
			mConversationDb.addConversation(oneToNConversation.getMsisdn(), false, convName, oneToNConversation.getConversationOwner());
			ContactManager.getInstance().insertGroup(oneToNConversation.getMsisdn(),convName);
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
			ConvMessage msg = new ConvMessage(gcjPacket, oneToNConversation, activity, true);
			ContactManager.getInstance().updateGroupRecency(oneToNConvId, msg.getTimestamp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, msg);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		JSONObject gcjJson = oneToNConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
		/*
		 * Adding the group name to the packet
		 */
		if (newOneToNConv)
		{
			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.NAME, convName);

				String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
				String fileName = Utils.getTempProfileImageFileName(oneToNConvId);
				File groupImageFile = new File(directory, fileName);

				if (groupImageFile.exists())
				{
					metadata.put(HikeConstants.REQUEST_DP, true);
				}

				gcjJson.put(HikeConstants.METADATA, metadata);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		HikeMqttManagerNew.getInstance().sendMessage(gcjJson, HikeMqttManagerNew.MQTT_QOS_ONE);

		if (oneToNConversation instanceof BroadcastConversation && !newOneToNConv)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.PARTICIPANT_JOINED_ONETONCONV, gcjJson);
		}

		ContactInfo conversationContactInfo = new ContactInfo(oneToNConvId, oneToNConvId, oneToNConvId, oneToNConvId);
		Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(activity, conversationContactInfo, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
		activity.finish();
	}

	/**
	 * To ensure that group Conversation and Broadcast conversation are mutually exclusive, we add the !isBroadCast check
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
		return msisdn!=null && msisdn.startsWith("b:");
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
		List<Pair<ContactInfo, ConvMessage>> allPairs = new ArrayList<Pair<ContactInfo,ConvMessage>>(totalRecipient);
		long timestamp = System.currentTimeMillis()/1000;
		for(int i=0;i<totalRecipient;i++)
		{
			ConvMessage message = new ConvMessage(convMessage);
			if(convMessage.isBroadcastConversation())
			{
				message.setMessageOriginType(OriginType.BROADCAST);
			}
			else
			{
				//multi-forward case... in braodcast case we donot need to update timestamp
				message.setTimestamp(timestamp++);
			}
			message.setMsgID(firstMsgId+i);
			ContactInfo contactInfo = recipient.get(i);
			message.setMsisdn(contactInfo.getMsisdn());
			Pair<ContactInfo, ConvMessage> pair = new Pair<ContactInfo, ConvMessage>(contactInfo, message);
			allPairs.add(pair);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_DB_INSERTED, allPairs);
	}
}
