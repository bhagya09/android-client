package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Pair;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.Logger;

public class MessagingBotAlarmManager implements HikePlatformConstants
{
	private static final String tag = "platformAlarmManager";

	public static final void setAlarm(Context context, JSONObject json, int messageId, long timeInMills)
	{
		Intent intent = new Intent();
		intent.putExtra(MESSAGE_ID, messageId); // for us uniqueness of a card is message id
		intent.putExtra(HikePlatformConstants.BOT_TYPE,HikePlatformConstants.NON_MESSAGING_BOT_TYPE);
		Iterator<String> i = json.keys();
		try
		{
			if (json.has(ALARM_DATA))
			{
				intent.putExtra(ALARM_DATA, json.getString(ALARM_DATA));
			}
			while (i.hasNext())
			{
				String key = i.next();
				intent.putExtra(key, json.getString(key));
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		// 100000 is added to avoid conflict between alarms at HikeAlarmManager end,Message ID Starts from 1
		HikeAlarmManager.setAlarmwithIntentPersistance(context, timeInMills, (int) (100000 + messageId), true, intent, true);
	}

	/*
	 * Assuming Format of metadata for platform is :
	 * 
	 * metadata:{'layout_id':'','file_id':'','card_data':{},'helper_data':{}}
	 * 
	 * On alarm of any card, we save it in ALARM TABLE
	 */
	public static final void processTasks(Intent intent, Context context)
	{
		Logger.i(tag, "Process Tasks Invoked with intent :  " + intent.getExtras().toString());
		Bundle data = intent.getExtras();
		if (data != null && data.containsKey(HikePlatformConstants.ALARM_DATA))
		{

			int messageId = data.getInt(MESSAGE_ID);
			if (messageId != 0) // validation
			{
				HikeConversationsDatabase.getInstance().insertMicroAppALarm(messageId, data.getString(HikePlatformConstants.ALARM_DATA));
				if (!deleteMessage(messageId, data, context))
				{
					increaseUnreadCount(data, context);
					showNotification(data, context);
					Message m = Message.obtain();
					m.arg1 = messageId;
					m.obj = data.get(ALARM_DATA);
					HikeMessengerApp.getPubSub().publish(HikePubSub.PLATFORM_CARD_ALARM, m);
				}
			}
		}
	}

	private static void increaseUnreadCount(Bundle data, Context context)
	{
		if (data.containsKey(CONV_MSISDN) && data.containsKey(INCREASE_UNREAD))
		{
			// increase unread count
			HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
			String msisdn = data.getString(CONV_MSISDN);
			db.incrementUnreadCounter(msisdn, 1);
			int newCount = db.getConvUnreadCount(msisdn);
			Message ms = Message.obtain();
			ms.arg1 = newCount;
			ms.obj = msisdn;
			HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_UNREAD_COUNT_MODIFIED, ms);
			Pair<String, Long> pair = new Pair<String, Long>(msisdn, System.currentTimeMillis() / 1000);
			HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_TS_UPDATED, pair);
		}
	}

	private static void showNotification(Bundle data, Context context)
	{
		if (data.containsKey(CONV_MSISDN))
		{
			String message = data.getString(NOTIFICATION);
			if (!TextUtils.isEmpty(message))
			{
				String playS = data.getString(NOTIFICATION_SOUND);
				
				boolean playSound = playS!=null ? Boolean.valueOf(playS) : false;
				
				HikeNotification.getInstance().sendNotificationToChatThread(data.getString(CONV_MSISDN), message, !playSound);
			}
		}
	}

	private static boolean deleteMessage(long messageId, Bundle data, Context context)
	{
		if (data.containsKey(DELETE_CARD) && data.containsKey(CONV_MSISDN))
		{
			String toDelete = data.getString(DELETE_CARD);
			if (Boolean.valueOf(toDelete))
			{
				String msisdn = data.getString(CONV_MSISDN);
				ArrayList<Long> list = new ArrayList<Long>();
				list.add(messageId);
				HikeConversationsDatabase.getInstance().deleteMessages(list, msisdn, null);
				return true;
			}
		}
		return false;
	}
}
