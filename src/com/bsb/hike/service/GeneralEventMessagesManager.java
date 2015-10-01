package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;


public class GeneralEventMessagesManager
{
	private final Context context;
	
	private static volatile GeneralEventMessagesManager instance;
	
	private GeneralEventMessagesManager(Context context)
	{
		this.context = context;
	}

	private static void increaseUnreadCount(String msisdn)
	{
		// increase unread count
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		db.incrementUnreadCounter(msisdn, 1);
		int newCount = db.getConvUnreadCount(msisdn);
		Message ms = Message.obtain();
		ms.arg1 = newCount;
		ms.obj = msisdn;
		HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_UNREAD_COUNT_MODIFIED, ms);
		Pair<String, Long> pair = new Pair<String, Long>(msisdn, System.currentTimeMillis() / 1000);
		HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_TS_UPDATED, pair);
	}

	private static void showNotification(JSONObject data, String msisdn)
	{

		String message = data.optString(HikePlatformConstants.NOTIFICATION);
		if (!TextUtils.isEmpty(message))
		{
			boolean playSound = data.optBoolean(HikePlatformConstants.NOTIFICATION_SOUND);

			HikeNotification.getInstance().sendNotificationToChatThread(msisdn, message, !playSound);
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
				if(data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.OFFLINE_MESSAGE_REQUEST_CANCEL))
				{
					OfflineUtils.handleOfflineCancelRequestPacket(context,packet);
				}
			}
			
			else if (HikeConstants.GeneralEventMessagesTypes.MESSAGE_EVENT.equals(type))
			{
				String from = packet.getString(HikeConstants.FROM);
				String messageHash = data.getString(HikePlatformConstants.MESSAGE_HASH);
				long messageId = HikeConversationsDatabase.getInstance().getMessageIdFromMessageHash(messageHash, from);
				if (messageId < 0)
				{
					Logger.e("General Event", "Event is unauthenticated");
					return;
				}
				long mappedId = data.getLong(HikeConstants.EVENT_ID);

				long clientTimestamp = packet.getLong(HikeConstants.SEND_TIMESTAMP);
				String eventMetadata = data.getString(HikePlatformConstants.EVENT_CARDDATA);
				String namespace = data.getString(HikePlatformConstants.NAMESPACE);
				String parent_msisdn = data.optString(HikePlatformConstants.PARENT_MSISDN);
				MessageEvent messageEvent = new MessageEvent(HikePlatformConstants.NORMAL_EVENT, from, namespace, eventMetadata, messageHash,
						HikePlatformConstants.EventStatus.EVENT_RECEIVED, clientTimestamp, mappedId, messageId, parent_msisdn);
				long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);
				if (eventId < 0)
				{
					Logger.e("General Event", "Duplicate event");
					return;
				}
				messageEvent.setEventId(eventId);

				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_EVENT_RECEIVED, messageEvent);
				boolean increaseUnreadCount = data.optBoolean(HikePlatformConstants.INCREASE_UNREAD);
				if (increaseUnreadCount)
				{
					increaseUnreadCount(from);
				}
				showNotification(data, from);

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
	
}
