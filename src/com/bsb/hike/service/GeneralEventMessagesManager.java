package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.CocosProcessIntentService;
import com.bsb.hike.platform.HikePlatformConstants;
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
				String hm=data.optString(HikePlatformConstants.HIKE_MESSAGE,data.optString(HikePlatformConstants.NOTIFICATION));
				MessageEvent messageEvent = new MessageEvent(HikePlatformConstants.NORMAL_EVENT, from, namespace, eventMetadata, messageHash,
						HikePlatformConstants.EventStatus.EVENT_RECEIVED, clientTimestamp, mappedId, messageId, parent_msisdn,hm);
				long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);


				ConvMessage message=HikeConversationsDatabase.getInstance().updateMessageForGeneralEvent(messageHash, ConvMessage.State.RECEIVED_UNREAD, hm);
				HikeMessengerApp.getPubSub().publish(HikePubSub.GENERAL_EVENT, message);
				if (eventId < 0)
				{
					Logger.e("General Event", "Duplicate event");
					return;
				}
				messageEvent.setEventId(eventId);
				sendMessageEventToIntentService(messageEvent);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_EVENT_RECEIVED, messageEvent);
				boolean increaseUnreadCount = data.optBoolean(HikePlatformConstants.INCREASE_UNREAD);
				boolean rearrangeChat = data.optBoolean(HikePlatformConstants.REARRANGE_CHAT);
				Utils.rearrangeChat(from, rearrangeChat, increaseUnreadCount);
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

	public void sendMessageEventToIntentService(MessageEvent messageEvent)
	{
		Intent cocosProcessIntentService = new Intent(this.context, CocosProcessIntentService.class);
		cocosProcessIntentService.putExtra(CocosProcessIntentService.MESSAGE_EVENT_RECEIVED_DATA, messageEvent);
		context.startService(cocosProcessIntentService);
	}
	
}
