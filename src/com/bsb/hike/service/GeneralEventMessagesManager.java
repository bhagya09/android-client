package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.CustomKeyboardManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.CocosProcessIntentService;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class GeneralEventMessagesManager
{
	private final Context context;
	
	private static volatile GeneralEventMessagesManager instance;
	
	private GeneralEventMessagesManager(Context context)
	{
		this.context = context;
	}

	private static void showNotification(JSONObject data, String msisdn)
	{

		String message = data.optString(HikePlatformConstants.NOTIFICATION);
		if (!TextUtils.isEmpty(message))
		{
			boolean playSound = data.optBoolean(HikePlatformConstants.NOTIFICATION_SOUND);

			ToastListener.getInstance().showMessageEventNotification(msisdn, message, !playSound);
		}
	}

	
	public void handleGeneralMessage(JSONObject packet) throws JSONException
	{
		JSONObject data  = packet.getJSONObject(HikeConstants.DATA);
		if (data!=null)
		{
			String type = data.optString(HikeConstants.TYPE);
			
			if(HikeConstants.GeneralEventMessagesTypes.OFFLINE.equals(type))
			{
				if (data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.OFFLINE_MESSAGE_REQUEST))
				{
					OfflineUtils.handleOfflineRequestPacket(context, packet);
				}
				else if(data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.OFFLINE_MESSAGE_REQUEST_CANCEL))
				{
					OfflineUtils.handleOfflineCancelRequestPacket(context,packet);
				}
				else if(data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.HIKE_DIRECT_UNSUPPORTED_PEER))
				{
					OfflineUtils.handleUnsupportedPeer(context,packet);
				}
				else if(data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.HIKE_DIRECT_UPDGRADE_PEER))
				{
					OfflineUtils.handleUpgradablePeer(context,packet);
				}
			}
			
			else if (HikeConstants.GeneralEventMessagesTypes.MESSAGE_EVENT.equals(type))
			{
				String fromMsisdn = packet.getString(HikeConstants.FROM);
				if(!TextUtils.isEmpty(fromMsisdn) && ContactManager.getInstance().isBlocked(fromMsisdn))
					return; // returning in case of blocked
				String messageHash = data.getString(HikePlatformConstants.MESSAGE_HASH);
				long messageId = HikeConversationsDatabase.getInstance().getMessageIdFromMessageHash(messageHash, fromMsisdn);
				if (messageId < 0)
				{
					Logger.e("General Event", "Event is unauthenticated");
					return;
				}
				long mappedId = data.getLong(HikeConstants.EVENT_ID);
				long mappedMessageId=data.optLong(HikeConstants.MESSAGE_ID);

				long clientTimestamp = packet.getLong(HikeConstants.SEND_TIMESTAMP);
				String eventMetadata = data.getString(HikePlatformConstants.EVENT_CARDDATA);
				String namespace = data.getString(HikePlatformConstants.NAMESPACE);
				String parent_msisdn = data.optString(HikePlatformConstants.PARENT_MSISDN);
				String hm=data.optString(HikePlatformConstants.HIKE_MESSAGE,data.optString(HikePlatformConstants.NOTIFICATION));
				MessageEvent messageEvent = new MessageEvent(HikePlatformConstants.NORMAL_EVENT, fromMsisdn, namespace, eventMetadata, messageHash,
						HikePlatformConstants.EventStatus.EVENT_RECEIVED, clientTimestamp, mappedId, messageId, parent_msisdn,hm);
				long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);

				ConvMessage message = HikeConversationsDatabase.getInstance().updateMessageForGeneralEvent(messageHash, ConvMessage.State.RECEIVED_UNREAD, hm,mappedMessageId);

				if (message == null || eventId < 0)
				{
					return;
				}

				//Sending DR here
				PlatformUtils.sendGeneralEventDeliveryReport(mappedId, fromMsisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.GENERAL_EVENT, message);
				if (eventId < 0)
				{
					Logger.e("General Event", "Duplicate event");
					return;
				}
				messageEvent.setEventId(eventId);
				sendMessageEventToIntentService(messageEvent);
				boolean increaseUnreadCount = data.optBoolean(HikePlatformConstants.INCREASE_UNREAD);
				boolean rearrangeChat = data.optBoolean(HikePlatformConstants.REARRANGE_CHAT);
				Utils.rearrangeChat(fromMsisdn, rearrangeChat, increaseUnreadCount);
				showNotification(data, fromMsisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_EVENT_RECEIVED, messageEvent);

			}

			else if (HikeConstants.GeneralEventMessagesTypes.GENERAL_EVENT_DR.equals(type))
			{
				handleGeneralEventDRPacket(packet);
			}
            else if(HikeConstants.GeneralEventMessagesTypes.CUSTOM_KEYBOARD.equals(type))
            {
                Logger.i("tagcontroller", "has CUSTOM_KEYBOARD");

                CustomKeyboardManager.getInstance().saveToSharedPreferences(packet.getString(HikeConstants.FROM), data.getJSONObject(HikeConstants.DATA));

                HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_INPUT_BOX, packet.getString(HikeConstants.FROM));
            }
			
		}
	}

	public static GeneralEventMessagesManager getInstance()
	{
		if (instance == null)
		{
			synchronized (GeneralEventMessagesManager.class)
			{
				if (instance == null)
				{
					instance = new GeneralEventMessagesManager(HikeMessengerApp.getInstance().getApplicationContext());
				}
			}
		}
		return instance;
	}

	public void sendMessageEventToIntentService(MessageEvent messageEvent)
	{
		Intent cocosProcessIntentService = new Intent(this.context, CocosProcessIntentService.class);
		cocosProcessIntentService.putExtra(CocosProcessIntentService.MESSAGE_EVENT_RECEIVED_DATA, messageEvent);
		context.startService(cocosProcessIntentService);
	}

	private void handleGeneralEventDRPacket(JSONObject packet)
	{
		JSONObject data  = null;
		try
		{
			data = packet.getJSONObject(HikeConstants.DATA);
			long mappedEventId = data.optLong(HikeConstants.DATA, -1);

			String fromMsisdn = packet.getString(HikeConstants.FROM);

			if (mappedEventId < 0 || (TextUtils.isEmpty(fromMsisdn)))
			{
				Logger.e("GeneralEventMessagesManager", "Received mappedEventID as " + mappedEventId + " Hence returning");
				return;
			}

			long msgId = HikeConversationsDatabase.getInstance().getMessageIdFromEventId(
					mappedEventId, fromMsisdn);

			if (msgId < 0)
			{
				Logger.e("GeneralEventMessagesManager", "Got negative msgId form db " + msgId);
				return;
			}
			
			saveDeliveryReport(msgId, fromMsisdn);

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Saving the delivery report in ConvTable and Messages Table
	 * @param msgId
	 * @param fromMsisdn
	 */
	private void saveDeliveryReport(long msgId, String fromMsisdn)
	{

		int rowsUpdated = updateDB(msgId, ConvMessage.State.SENT_DELIVERED, fromMsisdn);

		if (rowsUpdated == 0)
		{
			Logger.d(getClass().getSimpleName(), "No rows updated");
			return;
		}

		Pair<String, Long> pair = new Pair<String, Long>(fromMsisdn, msgId);

		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
	}

	private int updateDB(Long msgId, ConvMessage.State status, String msisdn)
	{
		return HikeConversationsDatabase.getInstance().updateMsgStatus(msgId, status.ordinal(), msisdn);
	}

}
