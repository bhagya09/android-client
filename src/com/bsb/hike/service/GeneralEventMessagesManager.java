package com.bsb.hike.service;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
//import com.bsb.hike.offline.OfflineUtils;

import android.content.Context;


public class GeneralEventMessagesManager
{
	private final Context context;
	
	private static volatile GeneralEventMessagesManager instance;
	
	private GeneralEventMessagesManager(Context context)
	{
		this.context = context;
	}

	public void handleGeneralMessage(JSONObject packet) throws JSONException
	{

		JSONObject data = packet.getJSONObject(HikeConstants.DATA);
		if (data != null)
		{
			String type = data.optString(HikeConstants.TYPE);

			if (HikeConstants.GeneralEventMessagesTypes.OFFLINE.equals(type))
			{
				//OfflineUtils.handleOfflineRequestPacket(context,packet);
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
				String eventMetadata = data.getString(HikePlatformConstants.EVENT_METADATA);
				String namespace = data.getString(HikePlatformConstants.NAMESPACE);
				MessageEvent messageEvent = new MessageEvent(HikePlatformConstants.NORMAL_EVENT, from, namespace, eventMetadata, messageHash,
						HikePlatformConstants.EventStatus.EVENT_RECEIVED, clientTimestamp, mappedId);
				long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);
				if (eventId < 0)
				{
					Logger.e("General Event", "Duplicate event");
					return;
				}
				messageEvent.setEventId(eventId);

			}

		}
	}

	public static GeneralEventMessagesManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (GeneralEventMessagesManager.class)
			{
				if (instance == null)
				{
					instance = new GeneralEventMessagesManager(context);
				}
			}
		}
		return instance;
	}
	
}
