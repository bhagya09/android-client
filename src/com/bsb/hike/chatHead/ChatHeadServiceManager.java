package com.bsb.hike.chatHead;

import static com.bsb.hike.MqttConstants.MQTT_CONNECTION_CHECK_ACTION;

import java.util.Calendar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;

public class ChatHeadServiceManager extends BroadcastReceiver
{
	static boolean flagScreen = true, snooze = false;

	static Context mContext = HikeMessengerApp.getInstance();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Logger.d("ashish", "receive");

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, true)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, true))
		{
			if (intent.hasExtra(HikeConstants.ChatHead.INTENT_EXTRA))
			{
				snooze = false;
				startService();
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			{
				stopService();
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				startService();
			}
		}
		else
		{
			stopService();
		}
	}

	public static void startService()
	{
		if (!Utils.isMyServiceRunning(ChatHeadService.class, mContext) && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, true)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, true) && !snooze)
		{
			mContext.startService(new Intent(mContext, ChatHeadService.class));
		}
	}

	public static void stopService()
	{
		if (Utils.isMyServiceRunning(ChatHeadService.class, mContext))
		{
			mContext.stopService(new Intent(mContext, ChatHeadService.class));
		}
	}

	public static void serviceDecision()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, true)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, true))
		{
			startService();
		}
		else
		{
			stopService();
		}
	}
}