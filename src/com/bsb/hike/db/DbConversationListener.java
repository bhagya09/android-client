package com.bsb.hike.db;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.*;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.*;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.HikeSDKMessageFilter;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.service.SmsMessageStatusReceiver;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DbConversationListener implements Listener
{
	private static final String SMS_SENT_ACTION = "com.bsb.hike.SMS_SENT";

	HikeConversationsDatabase mConversationDb;

	HikeMqttPersistence persistence;

	private HikePubSub mPubSub;

	private Context context;

	private int dayRecorded = 0;

	public DbConversationListener(Context context)
	{
		this.context = context;
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = HikeConversationsDatabase.getInstance();
		persistence = HikeMqttPersistence.getInstance();

		dayRecorded = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.DAY_RECORDED, 0);

		mPubSub.addListener(HikePubSub.MESSAGE_SENT, this);
		mPubSub.addListener(HikePubSub.DELETE_MESSAGE, this);
		mPubSub.addListener(HikePubSub.MESSAGE_FAILED, this);
		mPubSub.addListener(HikePubSub.BLOCK_USER, this);
		mPubSub.addListener(HikePubSub.UNBLOCK_USER, this);
		mPubSub.addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		mPubSub.addListener(HikePubSub.SERVER_RECEIVED_MULTI_MSG, this);
		mPubSub.addListener(HikePubSub.FAVORITE_TOGGLED, this);
		mPubSub.addListener(HikePubSub.MUTE_CONVERSATION_TOGGLED, this);
		mPubSub.addListener(HikePubSub.FRIEND_REQUEST_ACCEPTED, this);
		mPubSub.addListener(HikePubSub.REJECT_FRIEND_REQUEST, this);
		mPubSub.addListener(HikePubSub.DELETE_STATUS, this);
		mPubSub.addListener(HikePubSub.HIKE_JOIN_TIME_OBTAINED, this);
		mPubSub.addListener(HikePubSub.SEND_HIKE_SMS_FALLBACK, this);
		mPubSub.addListener(HikePubSub.SEND_NATIVE_SMS_FALLBACK, this);
		mPubSub.addListener(HikePubSub.REMOVE_PROTIP, this);
		mPubSub.addListener(HikePubSub.GAMING_PROTIP_DOWNLOADED, this);
		mPubSub.addListener(HikePubSub.CLEAR_CONVERSATION, this);
		mPubSub.addListener(HikePubSub.UPDATE_PIN_METADATA, this);
		mPubSub.addListener(HikePubSub.MULTI_MESSAGE_SENT, this);
		mPubSub.addListener(HikePubSub.MULTI_FILE_UPLOADED, this);
		mPubSub.addListener(HikePubSub.HIKE_SDK_MESSAGE, this);
		mPubSub.addListener(HikePubSub.CONVERSATION_TS_UPDATED, this);	
		mPubSub.addListener(HikePubSub.GROUP_LEFT, this);
		mPubSub.addListener(HikePubSub.DELETE_THIS_CONVERSATION, this);
		mPubSub.addListener(HikePubSub.UPDATE_LAST_MSG_STATE, this);
		mPubSub.addListener(HikePubSub.STEALTH_DATABASE_MARKED, this);
		mPubSub.addListener(HikePubSub.STEALTH_DATABASE_UNMARKED, this);
		mPubSub.addListener(HikePubSub.PLATFORM_CARD_EVENT_SENT, this);
		mPubSub.addListener(HikePubSub.UPDATE_MESSAGE_ORIGIN_TYPE, this);
		mPubSub.addListener(HikePubSub.BOT_DISCOVERY_DOWNLOAD_SUCCESS, this);
		mPubSub.addListener(HikePubSub.BOT_DISCOVERY_TABLE_FLUSH, this);
		mPubSub.addListener(HikePubSub.ADD_NM_BOT_CONVERSATION, this);
		mPubSub.addListener(HikePubSub.ADD_INLINE_FRIEND_MSG, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			boolean shouldSendMessage = convMessage.isFileTransferMessage() && !TextUtils.isEmpty(convMessage.getMetadata().getHikeFiles().get(0).getFileKey());
			boolean shouldSendNativeCardMessage = convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT && !TextUtils.isEmpty(convMessage.platformMessageMetadata.getHikeFiles().get(0).getFileKey());
			if (shouldSendMessage)
			{
				mConversationDb.updateMessageMetadata(convMessage.getMsgID(), convMessage.getMetadata());
				
				// Adding Logs for Message Reliability
				MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.DB_UPDATE_TRANSACTION_COMPLETED);
			}
			else if(shouldSendNativeCardMessage){
				mConversationDb.updateMessageMetadata(convMessage.getMsgID(), convMessage.platformMessageMetadata.getJSON());
				MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.DB_UPDATE_TRANSACTION_COMPLETED);
			}
			else
			{
				if (!convMessage.isFileTransferMessage() && convMessage.getMessageType() != HikeConstants.MESSAGE_TYPE.CONTENT)
				{
					mConversationDb.addConversationMessages(convMessage,true);
				
					// Adding Logs for Message Reliability
					MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.DB_ADD_TRANSACTION_COMPLETED);
					
					if (convMessage.isSent())
					{
						uploadFiksuPerDayMessageEvent();
					}
					if (convMessage.isBroadcastConversation())
					{
						OneToNConversationUtils.addBroadcastRecipientConversations(convMessage);
					}
				}
				// Recency was already updated when the ft message was added.
				ContactManager.getInstance().updateContactRecency(convMessage.getMsisdn(), convMessage.getTimestamp());

				mPubSub.publish(HikePubSub.RECENT_CONTACTS_UPDATED, convMessage.getMsisdn());
			}

			if ((convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
					&& (!convMessage.isFileTransferMessage() || shouldSendMessage) && (convMessage.getMessageType() !=HikeConstants.MESSAGE_TYPE.CONTENT || shouldSendNativeCardMessage ))
			{
				Logger.d("DBCONVERSATION LISTENER", "Sending Message : " + convMessage.getMessage() + "	;	to : " + convMessage.getMsisdn());
				if (!convMessage.isSMS() || !Utils.getSendSmsPref(context) || convMessage.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
				{
					HikeMqttManagerNew.getInstance().sendMessage(convMessage.serialize(), MqttConstants.MQTT_QOS_ONE);
				}
				else
				{
					Logger.d(getClass().getSimpleName(), "Messages Id: " + convMessage.getMsgID());
					sendNativeSMS(convMessage);
				}
				if (convMessage.isOneToNChat())
				{
					convMessage = mConversationDb.showParticipantStatusMessage(convMessage.getMsisdn());
					if(convMessage != null)
					{
						mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
					}
				}
			}
		}
		else if (HikePubSub.MULTI_MESSAGE_SENT.equals(type))
		{
			MultipleConvMessage multiConvMessages = (MultipleConvMessage) object;

            sendMultiConvMessage(multiConvMessages);
		}
		else if (HikePubSub.MULTI_FILE_UPLOADED.equals(type))
		{
			MultipleConvMessage multiConvMessages = (MultipleConvMessage) object;
			HikeMqttManagerNew.getInstance().sendMessage(multiConvMessages.serialize(), MqttConstants.MQTT_QOS_ONE);
		}
		else if (HikePubSub.DELETE_MESSAGE.equals(type))
		{
			Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
			ArrayList<Long> msgIds = deleteMessage.first;
			Bundle bundle = deleteMessage.second;
			Boolean containsLastMessage = null;
			if (bundle.containsKey(HikeConstants.Extras.IS_LAST_MESSAGE))
			{
				containsLastMessage = bundle.getBoolean(HikeConstants.Extras.IS_LAST_MESSAGE);
			}
			String msisdn = bundle.getString(HikeConstants.Extras.MSISDN);

			mConversationDb.deleteMessages(msgIds, msisdn, containsLastMessage);
			persistence.removeMessages(msgIds);
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type)) // server got msg
		// from client 1 and
		// sent back
		// received msg
		// receipt
		{
			updateDB(object, ConvMessage.State.SENT_FAILED.ordinal());
		}
		else if (HikePubSub.BLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			ContactManager.getInstance().block(msisdn);
			/*
			 * When a user blocks someone, we reset the contact's friend type.
			 */
			ContactManager.getInstance().toggleContactFavorite(msisdn, FavoriteType.NOT_FRIEND);

			JSONObject blockObj = blockUnblockSerialize("b", msisdn);

			if (!BotUtils.isBot(msisdn))
			{
				/*
				 * We remove the icon for a blocked user as well.
				 */
				HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			}
			
			// IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
			HikeMqttManagerNew.getInstance().sendMessage(blockObj, MqttConstants.MQTT_QOS_ONE);
		}
		else if (HikePubSub.UNBLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			ContactManager.getInstance().unblock(msisdn);
			JSONObject unblockObj = blockUnblockSerialize("ub", msisdn);
			HikeMqttManagerNew.getInstance().sendMessage(unblockObj, MqttConstants.MQTT_QOS_ONE);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) // server got
		// msg from
		// client 1 and
		// sent back
		// received msg
		// receipt
		{
			Logger.d("DBCONVERSATION LISTENER", "(Sender) Message sent confirmed for msgID -> " + (Long) object);
			updateDB(object, ConvMessage.State.SENT_CONFIRMED.ordinal());
		}
		else if(HikePubSub.SERVER_RECEIVED_MULTI_MSG.equals(type))
		{
			Pair<Long, Integer> p  = (Pair<Long, Integer>) object;
			long baseId = p.first;
			long endId = (p.first + p.second) - 1;
			Logger.d("DBCONVERSATION LISTENER", "(Sender) Message sent confirmed for msgID between " + baseId + "and "+ endId);
			mConversationDb.updateMsgStatusBetween(baseId, endId, ConvMessage.State.SENT_CONFIRMED.ordinal(), null);
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			ContactManager.getInstance().toggleContactFavorite(contactInfo.getMsisdn(), favoriteType);

			if (favoriteType != FavoriteType.REQUEST_RECEIVED && favoriteType != FavoriteType.REQUEST_SENT_REJECTED && !HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type))
			{
				String requestType;
				if (favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT)
				{

					/*
					 * Adding a status message for accepting the friend request
					 */
					if (favoriteType == FavoriteType.FRIEND)
					{
						StatusMessage statusMessage = new StatusMessage(0, null, contactInfo.getMsisdn(), contactInfo.getName(),
								context.getString(R.string.accepted_friend_request), StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST, System.currentTimeMillis() / 1000);
						mConversationDb.addStatusMessage(statusMessage, true);
						mConversationDb.updateHistoricalStatusMessages(statusMessage.getMsisdn());
						mPubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
						mPubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
					}

					requestType = HikeConstants.MqttMessageTypes.ADD_FAVORITE;
				}
				else if (HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
				{
					requestType = HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE;
				}
				else
				{
					requestType = HikeConstants.MqttMessageTypes.REMOVE_FAVORITE;
				}
				HikeMqttManagerNew.getInstance().sendMessage(serializeMsg(requestType, contactInfo.getMsisdn(), contactInfo instanceof FtueContactInfo), MqttConstants.MQTT_QOS_ONE);
			}
		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			Mute mute = (Mute) object;

			String id = mute.getMsisdn();
			boolean isMute = mute.isMute();

			mConversationDb.toggleChatMute(mute);

			if (BotUtils.isBot(id))
			{
				// TODO Do we have to do MQTT PUBLISH here?
				mConversationDb.toggleMuteBot(id, isMute);
			}
			else
			{
				mConversationDb.toggleGroupMute(id, isMute);
				if(isMute) {
					HikeMqttManagerNew.getInstance().sendMessage(serializeMsg(HikeConstants.MqttMessageTypes.MUTE, id, mute.shouldShowNotifInMute(), mute.getMuteDurationString()),
							MqttConstants.MQTT_QOS_ONE);
				} else {
					HikeMqttManagerNew.getInstance().sendMessage(serializeMsg(HikeConstants.MqttMessageTypes.UNMUTE, id),
							MqttConstants.MQTT_QOS_ONE);
				}
			}

		}
		else if (HikePubSub.DELETE_STATUS.equals(type))
		{
			String statusId = (String) object;
			mConversationDb.deleteStatus(statusId);
			mConversationDb.deleteActivityFeedForStatus(statusId);
			/*
			 * If the status also has an icon, we delete that as well.
			 */
			ContactManager.getInstance().removeIcon(statusId);
		}
		else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type))
		{
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			ContactManager.getInstance().setHikeJoinTime(msisdn, hikeJoinTime);
			
		}
		else if (HikePubSub.SEND_HIKE_SMS_FALLBACK.equals(type))
		{
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty())
			{
				return;
			}

			try
			{
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.FORCE_SMS);
				jsonObject.put(HikeConstants.TO, messages.get(0).getMsisdn());

				JSONObject data = new JSONObject();

				JSONArray messagesArray = new JSONArray();

				for (ConvMessage convMessage : messages)
				{

					mConversationDb.updateIsHikeMessageState(convMessage.getMsgID(), false);

					convMessage.setSMS(true);
				}
				
				/*
				 * We will send combined string of all the messages
				 * in json of last convMessage object
				 */
				ConvMessage lastMessage = messages.get(messages.size() -1);
				
				ConvMessage convMessage = new ConvMessage(Utils.combineInOneSmsString(context, true, messages, true), lastMessage.getMsisdn(), 
						lastMessage.getTimestamp(), lastMessage.getState(), lastMessage.getMsgID(), lastMessage.getMappedMsgID(), lastMessage.getSortingId());
				JSONObject messageJSON = convMessage.serialize().getJSONObject(HikeConstants.DATA);

				messagesArray.put(messageJSON);

				data.put(HikeConstants.BATCH_MESSAGE, messagesArray);
				data.put(HikeConstants.COUNT, 1);
				data.put(HikeConstants.MESSAGE_ID, messages.get(0).getMsgID());

				jsonObject.put(HikeConstants.DATA, data);

				mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);
				HikeMqttManagerNew.getInstance().sendMessage(jsonObject, MqttConstants.MQTT_QOS_ONE);
			}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), "Invalid json", e);
			}

		}
		else if (HikePubSub.SEND_NATIVE_SMS_FALLBACK.equals(type))
		{
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty())
			{
				return;
			}

			ContactInfo contactInfo = ContactManager.getInstance().getContact(messages.get(0).getMsisdn(), true, false);

			sendNativeSMSFallbackLogEvent(contactInfo.isOnhike(), Utils.isUserOnline(context), messages.size());

			for (ConvMessage convMessage : messages)
			{
				convMessage.setSMS(true);
				mConversationDb.updateIsHikeMessageState(convMessage.getMsgID(), false);
			}
			ConvMessage lastMessage = messages.get(messages.size() - 1);
			sendNativeSMS(new ConvMessage(Utils.combineInOneSmsString(context, true, messages, false), lastMessage.getMsisdn(), lastMessage.getTimestamp(), State.UNKNOWN, lastMessage.getMsgID(), -1, lastMessage.getSortingId()));

			mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);
		}
		else if (HikePubSub.REMOVE_PROTIP.equals(type))
		{
			String mappedId = (String) object;
			HikeMessengerApp.getLruCache().deleteIconForMSISDN(mappedId);
			// IconCacheManager.getInstance().deleteIconForMSISDN(mappedId);
			mConversationDb.deleteProtip(mappedId);

			sendDismissTipLogEvent(mappedId, null);
		}
		else if (HikePubSub.GAMING_PROTIP_DOWNLOADED.equals(type))
		{
			Protip protip = (Protip) object;

			String mappedId = protip.getMappedId();
			String url = protip.getGameDownlodURL();

			HikeMessengerApp.getLruCache().deleteIconForMSISDN(mappedId);
			// IconCacheManager.getInstance().deleteIconForMSISDN(mappedId);
			mConversationDb.deleteProtip(mappedId);
			sendDismissTipLogEvent(mappedId, url);
		}
		else if (HikePubSub.CLEAR_CONVERSATION.equals(type))
		{
			String msisdn = (String) object;
			mConversationDb.clearConversation(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_MESSAGE_CHANGED, null);
		}
		else if (HikePubSub.UPDATE_PIN_METADATA.equals(type))
		{
			
				Conversation conv = (Conversation)object;
				HikeConversationsDatabase.getInstance().updateConversationMetadata(conv.getMsisdn(), conv.getMetadata());
			
		}else if(HikePubSub.HIKE_SDK_MESSAGE.equals(type)){
			handleHikeSdkMessage(object);
		}
		else if (HikePubSub.CONVERSATION_TS_UPDATED.equals(type))
		{
			Pair<String, Long> p = (Pair<String, Long>) object;
			String msisdn = p.first;
			long timestamp = p.second;
			boolean isUpdated = mConversationDb.updateSortingTimestamp(msisdn, timestamp);
		}
		
		else if(HikePubSub.GROUP_LEFT.equals(type) || HikePubSub.DELETE_THIS_CONVERSATION.equals(type))
		{
			ConvInfo convInfo = (ConvInfo) object;
			String msisdn = convInfo.getMsisdn();
			Editor editor = context.getSharedPreferences(HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).edit();
			editor.remove(msisdn);
			editor.commit();

			HikeConversationsDatabase.getInstance().deleteConversation(msisdn);				
			ContactManager.getInstance().removeContacts(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_DELETED, convInfo);
		}
		
		else if (HikePubSub.UPDATE_LAST_MSG_STATE.equals(type))
		{
			Pair<Integer, String> stateMsisdnPair = (Pair<Integer, String>) object;
			mConversationDb.updateLastMessageStateAndCount(stateMsisdnPair.second, stateMsisdnPair.first);
		}
		
		else if(HikePubSub.STEALTH_DATABASE_MARKED.equals(type) || HikePubSub.STEALTH_DATABASE_UNMARKED.equals(type))
		{
			String msisdn = (String) object;
			boolean markStealth = HikePubSub.STEALTH_DATABASE_MARKED.equals(type);
			if( HikeConversationsDatabase.getInstance().toggleStealth(msisdn, markStealth) )
			{
				HikeMessengerApp.getPubSub().publish(markStealth ? HikePubSub.STEALTH_CONVERSATION_MARKED : HikePubSub.STEALTH_CONVERSATION_UNMARKED, msisdn);
			}
		}
		
		else if (HikePubSub.PLATFORM_CARD_EVENT_SENT.equals(type))
		{
			Pair<MessageEvent, JSONObject> pair = (Pair<MessageEvent, JSONObject>) object;

			MessageEvent messageEvent = pair.first;
			JSONObject data;
			data = pair.second;
			String hm=data.optString(HikePlatformConstants.HIKE_MESSAGE,data.optString(HikePlatformConstants.NOTIFICATION));
			if (messageEvent == null)
			{
				Logger.e(HikePlatformConstants.TAG, "Got Message Event null");
				return;
			}

			long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);

			boolean isGroup = false;
			String fromUserMsisdn = null;
			if(!TextUtils.isEmpty(messageEvent.getMsisdn()) && OneToNConversationUtils.isGroupConversation(messageEvent.getMsisdn()))
			{
				isGroup = true;
				fromUserMsisdn = messageEvent.getFromUserMsisdn();
			}

			if (eventId < 0 || (HikeConversationsDatabase.getInstance().updateMessageForGeneralEvent(messageEvent.getMessageHash(), State.SENT_UNCONFIRMED, hm,null, isGroup, fromUserMsisdn) == null))
			{
				Logger.e(HikePlatformConstants.TAG, "Error inserting message event");
			}
			else
			{
				messageEvent.setEventId(eventId);
				JSONObject jObj = new JSONObject();
				try
				{

					jObj.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ONE);
					jObj.put(HikeConstants.SEND_TIMESTAMP, messageEvent.getSentTimeStamp());
					jObj.put(HikeConstants.TIMESTAMP, messageEvent.getSentTimeStamp());
					jObj.put(HikeConstants.TO, messageEvent.getMsisdn());

					data.put(HikeConstants.TYPE, HikeConstants.GeneralEventMessagesTypes.MESSAGE_EVENT);
					data.put(HikePlatformConstants.MESSAGE_HASH, messageEvent.getMessageHash());
					data.put(HikePlatformConstants.NAMESPACE, messageEvent.getNameSpace());

					data.put(HikeConstants.EVENT_ID, eventId);
					long messageId = HikeConversationsDatabase.getInstance().getMessageIdFromMessageHash(messageEvent.getMessageHash(), messageEvent.getMsisdn());
					data.put(HikeConstants.MESSAGE_ID,messageId);
					jObj.put(HikeConstants.DATA, data);
					HikeMqttManagerNew.getInstance().sendMessage(jObj, MqttConstants.MQTT_QOS_ONE);
					boolean rearrangeChat = data.optBoolean(HikePlatformConstants.REARRANGE_CHAT);
					Utils.rearrangeChat(messageEvent.getMsisdn(), rearrangeChat, false);
				}

				catch (JSONException e)
				{
					Logger.e(HikePlatformConstants.TAG, "Got a JSON Exception while creating a message event : " + e);
				}
			}
		}
		
		else if(HikePubSub.UPDATE_MESSAGE_ORIGIN_TYPE.equals(type))
		{
			Pair<Long, Integer> pair = (Pair<Long, Integer>) object;

			long msgId = pair.first;

			HikeConversationsDatabase.getInstance().updateMessageOriginType(msgId, pair.second);
		}
		
		else if (HikePubSub.BOT_DISCOVERY_DOWNLOAD_SUCCESS.equals(type))
		{
			JSONObject result = (JSONObject) object;

			boolean flushOldData = result.optBoolean(HikeConstants.BOT_TABLE_REFRESH, false);
			
			JSONArray botArray = result.optJSONArray(HikePlatformConstants.BOTS);
			if (botArray == null || botArray.length() == 0)
			{
				Logger.e("BotDiscovery", "Got null botArray");
				return;
			}
			
			HikeContentDatabase.getInstance().populateBotDiscoveryTable(botArray, flushOldData);
		}
		
		else if (HikePubSub.BOT_DISCOVERY_TABLE_FLUSH.equals(type))
		{
			HikeContentDatabase.getInstance().flushBotDiscoveryTable();
		}
		
		else if (HikePubSub.ADD_NM_BOT_CONVERSATION.equals(type))
		{
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation((BotInfo) object);
		}

		else if (HikePubSub.ADD_INLINE_FRIEND_MSG.equals(type))
		{
			ConvMessage msg = (ConvMessage) object;

			if (msg != null)
			{
				HikeConversationsDatabase.getInstance().addConversationMessages(msg, false);
				// Update the CT back as well.
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_THREAD, msg);
			}
		}
	}

    private void sendMultiConvMessage(MultipleConvMessage multiConvMessages) {
		Logger.d("productpopup","start insert multimsg"+ System.currentTimeMillis());
        mConversationDb.addConversations(multiConvMessages.getMessageList(), multiConvMessages.getContactList(),multiConvMessages.getCreateChatThread());
        // after DB insertion, we need to update conversation UI , so sending event which contains all contacts and last message for each contact
        multiConvMessages.sendPubSubForConvScreenMultiMessage();
        // publishing mqtt packet
        HikeMqttManagerNew.getInstance().sendMessage(multiConvMessages.serialize(), MqttConstants.MQTT_QOS_ONE);
		Logger.d("productpopup", "start end multimsg" + System.currentTimeMillis());
    }

    private void handleHikeSdkMessage(Object object){
        /*
        * Handles the message sent by SDK. Will have a string of comma-separated msisdn, which is sent as a multi convMessage.
        */
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject((String) object);
            JSONObject parseJSON = new JSONObject(jsonObject.optString(HikePlatformConstants.MESSAGE));
            ArrayList<ConvMessage> listOfMessages= new ArrayList<ConvMessage>(1);
            ConvMessage convMessage= HikeSDKMessageFilter.filterMessage((JSONObject) parseJSON, context);
            listOfMessages.add(convMessage);

            String[] toArray = parseJSON.has(HikePlatformConstants.RECEPIENT) ? parseJSON.getString(HikePlatformConstants.RECEPIENT).split(",") : new String[]{};
			ArrayList<String> platformIds = new ArrayList();
			for (String id : toArray)
			{
				if (!TextUtils.isEmpty(id))
				{
					platformIds.add(id);
				}
			}
			if ( platformIds.size() == 0)
			{
				return;
			}
            ArrayList<String> msisdns = ContactManager.getInstance().getMsisdnFromId(platformIds);
            ArrayList<ContactInfo> listOfContacts = new ArrayList<ContactInfo>();
            for (String msisdn:msisdns){
                convMessage.platformMessageMetadata.addToThumbnailTable();
                listOfContacts.add(new ContactInfo(msisdn, msisdn, null, null,!convMessage.isSMS()));
            }
            convMessage.platformMessageMetadata.thumbnailMap.clear();
//            convMessage.platformMessageMetadata.addThumbnailsToMetadata();
            long timeStamp = System.currentTimeMillis()/1000;
			String source = jsonObject.optString(HikePlatformConstants.SOURCE);
            MultipleConvMessage multipleConvMessage = new MultipleConvMessage(listOfMessages, listOfContacts, timeStamp, true, source);
            sendMultiConvMessage(multipleConvMessage);

        } catch (JSONException e) {

            e.printStackTrace();
        }

		// publish MQTT and update UI here
		//	}
		}


	private void sendNativeSMSFallbackLogEvent(boolean onHike, boolean userOnline, int numMessages)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.IS_H2H, onHike);
			metadata.put(HikeConstants.OFFLINE, userOnline ? HikeConstants.RECIPIENT : HikeConstants.SENDER);
			metadata.put(HikeConstants.NUMBER_OF_SMS, numMessages);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void sendDismissTipLogEvent(String tipId, String URL)
	{
		JSONObject metadata = new JSONObject();
		
		try
		{
			metadata.put(HikeConstants.TIP_ID, tipId);
			
			if (!TextUtils.isEmpty(URL))
			{
				metadata.put(HikeConstants.TIP_URL, URL);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void sendNativeSMS(ConvMessage convMessage)
	{
		SmsManager smsManager = SmsManager.getDefault();

		ArrayList<String> messages = smsManager.divideMessage(Utils.getMessageDisplayText(convMessage, context));

		ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();

		for (int i = 0; i < messages.size(); i++)
		{
			Intent intent = new Intent(SMS_SENT_ACTION + convMessage.getMsgID());
			intent.setClass(context, SmsMessageStatusReceiver.class);
			intent.putExtra(HikeConstants.Extras.SMS_ID, convMessage.getMsgID());
			pendingIntents.add(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		}

		/*
		 * The try-catch block is needed for a bug in certain LG devices where it throws an NPE here.
		 */
		try
		{
			smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null, messages, pendingIntents, null);
		}
		catch (NullPointerException e)
		{
			Logger.d(getClass().getSimpleName(), "NPE while trying to send SMS", e);
		}

		writeToNativeSMSDb(convMessage);
	}

	/*
	 * Recording the event on fiksu if this was the first message of the day.
	 */
	private void uploadFiksuPerDayMessageEvent()
	{
		int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		if (today != dayRecorded)
		{
			dayRecorded = today;
			Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeMessengerApp.DAY_RECORDED, dayRecorded);
			editor.commit();
		}
	}

	private JSONObject serializeMsg(String type, String id, boolean showNotif, String duration) {
		JSONObject obj = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
			{
				obj.put(HikeConstants.TO, id);
			}
			obj.put(HikeConstants.TYPE, type);
			data.put(HikeConstants.ID, id);
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));

			String isStealth = "";
			if(StealthModeManager.getInstance().isStealthMsisdn(id)) {
				isStealth = ChatAnalyticConstants.STEALTH_CHAT_THREAD;
			}
			data.put(HikeConstants.VARIETY, isStealth);

			data.put(HikeConstants.VALUE_STR, duration);
			int notifOn = 0;
			if(showNotif) notifOn = 1;
			data.put(HikeConstants.VALUE_INT, notifOn);

			obj.put(HikeConstants.DATA, data);
			Logger.d(getClass().getSimpleName(), "Sending add friends packet, Object: "+obj.toString());
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid json", e);
		}
		return obj;
	}

	private JSONObject serializeMsg(String type, String id)
	{
		return serializeMsg(type, id, false);
	}
	
	private JSONObject serializeMsg(String type, String id, boolean isFromFtue)
	{
		JSONObject obj = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
			{
				obj.put(HikeConstants.TO, id);
			}
			obj.put(HikeConstants.TYPE, type);
			data.put(HikeConstants.ID, id);
			if(isFromFtue)
			{
				JSONObject ftueData = new JSONObject();
				ftueData.put(HikeConstants.SCREEN, HikeConstants.FTUE);
				data.put(HikeConstants.METADATA, ftueData);
			}
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
			obj.put(HikeConstants.DATA, data);
			Logger.d(getClass().getSimpleName(), "Sending add friends packet, Object: "+obj.toString());
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid json", e);
		}
		return obj;
	}

	private JSONObject blockUnblockSerialize(String type, String msisdn)
	{
		JSONObject obj = new JSONObject();
		try
		{
			obj.put(HikeConstants.TYPE, type);
			obj.put(HikeConstants.DATA, msisdn);
			obj.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations
		 */
		mConversationDb.updateMsgStatus(msgID, status, null);
	}

	private void writeToNativeSMSDb(ConvMessage convMessage)
	{

		ContentValues values = new ContentValues();
		values.put(HikeConstants.SMSNative.NUMBER, convMessage.getMsisdn());
		values.put(HikeConstants.SMSNative.DATE, convMessage.getTimestamp() * 1000);
		values.put(HikeConstants.SMSNative.MESSAGE, Utils.getMessageDisplayText(convMessage, context));

		context.getContentResolver().insert(HikeConstants.SMSNative.SENTBOX_CONTENT_URI, values);
	}
	
}
