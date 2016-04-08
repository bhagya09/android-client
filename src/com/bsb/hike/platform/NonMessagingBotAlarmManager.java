package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.video.HikeVideoCompressor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Class which handles alarms for Non-Messaging Bots
 * Created by pushkargupta on 28/03/16.
 */
public class NonMessagingBotAlarmManager {
    static String TAG = "NonMessagingBotAlarmmanager";

    /**
     * Method to set alarm for non messaging bots
     * @param context
     * @param json
     * @param msisdn
     * @param timeInMills
     * @param persistent
     */
    public static final void setAlarm(Context context, JSONObject json, String msisdn, long timeInMills,boolean persistent)
    {
        Intent intent = new Intent();
        String alarmData="";
        intent.putExtra(HikeConstants.MSISDN, msisdn);
        intent.putExtra(HikePlatformConstants.BOT_TYPE, HikeConstants.NON_MESSAGING_BOT);
        Iterator<String> i = json.keys();
        try
        {
            if (json.has(HikePlatformConstants.ALARM_DATA))
            {
                alarmData = json.getString(HikePlatformConstants.ALARM_DATA);
                intent.putExtra(HikePlatformConstants.ALARM_DATA, alarmData);
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
        HikeAlarmManager.setAlarmwithIntentPersistance(context,System.currentTimeMillis()+ timeInMills, (int)(msisdn.hashCode()+alarmData.hashCode()), true, intent, persistent);
    }

    /**
     * This method is called when the alarm is invoked
     * @param intent
     * @param context
     */
	public static final void processTasks(Intent intent, Context context)
	{
		Logger.i(TAG, "Process Tasks Invoked with intent :  " + intent.getExtras().toString());
		Bundle data = intent.getExtras();
		if (data != null && data.containsKey(HikePlatformConstants.ALARM_DATA))
		{
			String msisdn = data.getString(HikeConstants.MSISDN);
			if (TextUtils.isEmpty(msisdn))
			{
				return;
			}
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
			if (botInfo != null && botInfo.isNonMessagingBot())
			{
				if (ContactManager.getInstance().isBlocked(msisdn))
				{
					return;
				}

				String message = data.getString(HikePlatformConstants.NOTIFICATION);
                NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
                if(metadata!= null && metadata.isNativeMode())
                {
                    msisdn=metadata.getParentMsisdn();
                    if(TextUtils.isEmpty(msisdn) || !HikeConversationsDatabase.getInstance().isConversationExist(msisdn) || ContactManager.getInstance().isBlocked(msisdn))
                    {
                        return;
                    }
                }
				if (!TextUtils.isEmpty(message))
				{
					HikeConversationsDatabase.getInstance().updateLastMessageForNonMessagingBot(msisdn, message);
					// Saving lastConvMessage in memory as well to refresh the UI
					botInfo.setLastConversationMsg(Utils.makeConvMessage(msisdn, message, true, ConvMessage.State.RECEIVED_UNREAD));
				}
				showNotification(data, context);
				boolean increaseUnreadCount = Boolean.valueOf(data.getString(HikePlatformConstants.INCREASE_UNREAD));
				boolean rearrangeChat = Boolean.valueOf(data.getString(HikePlatformConstants.REARRANGE_CHAT));
				Utils.rearrangeChat(msisdn, rearrangeChat, increaseUnreadCount);

			}
		}
	}

    private static void showNotification(Bundle data, Context context)
    {
        Logger.d(TAG,"showing notif");
        if (data.containsKey(HikeConstants.MSISDN))
        {
            String message = data.getString(HikePlatformConstants.NOTIFICATION);
            if (!TextUtils.isEmpty(message))
            {
                String playS = data.getString(HikePlatformConstants.NOTIFICATION_SOUND);

                boolean playSound = playS!=null ? Boolean.valueOf(playS) : false;
                String notifData = data.getString(HikePlatformConstants.ALARM_DATA);
                String msisdn = data.getString(HikeConstants.MSISDN);
                if (!TextUtils.isEmpty(notifData))
                {
                    HikeConversationsDatabase.getInstance().updateNotifDataForMicroApps(msisdn, notifData);

                    HikeMessengerApp.getPubSub().publish(HikePubSub.NOTIF_DATA_RECEIVED, BotUtils.getBotInfoForBotMsisdn(msisdn));
                }
                HikeNotification.getInstance().sendNotificationToChatThread(msisdn, message, !playSound);
            }
        }
    }
}
