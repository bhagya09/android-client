package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChatHeadServiceManager extends BroadcastReceiver
{
	static boolean flagScreen = true;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Logger.d("ashish", "hello5");

		// if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		// {
		// Logger.d("ashish", "screenoff");
		// }
		// else
		// {
		// Logger.d("ashish", "screenon");
		// }

		Context mContext = HikeMessengerApp.getInstance();
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.CHAT_HEAD_SERVICE, true)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.CHAT_HEAD_USR_CONTROL, true))
		{
			Logger.d("ashish", "hello");
			/*
			 * if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF) && Utils.isMyServiceRunning(ChatHeadService.class, mContext)) { flagScreen = false; mContext.stopService(new
			 * Intent(mContext, ChatHeadService.class)); Logger.d("ashish", "screenoff"); } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON) && !flagScreen) {
			 * mContext.startService(new Intent(mContext, ChatHeadService.class)); flagScreen = true; Logger.d("ashish", "screenon");
			 * 
			 * }
			 * 
			 * else
			 */if (intent.hasExtra(HikeConstants.ChatHeadService.INTENT_EXTRA))
			{
				if (intent.getIntExtra(HikeConstants.ChatHeadService.INTENT_EXTRA, HikeConstants.ChatHeadService.STARTING_SERVICE) == 1
						&& !Utils.isMyServiceRunning(ChatHeadService.class, mContext))
				{
					mContext.startService(new Intent(mContext, ChatHeadService.class));
				}
				else
				{
					if (Utils.isMyServiceRunning(ChatHeadService.class, mContext))
					{
						mContext.stopService(new Intent(mContext, ChatHeadService.class));
					}
					Intent mIntent = new Intent(mContext, ChatHeadServiceManager.class);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, PendingIntent.FLAG_ONE_SHOT);
					AlarmManager alarmManager = (AlarmManager) (mContext.getSystemService(mContext.ALARM_SERVICE));
					alarmManager.set(AlarmManager.RTC, intent.getIntExtra(HikeConstants.ChatHeadService.INTENT_EXTRA, 0), pendingIntent);
				}

			}

		}
		else
		{
			if (Utils.isMyServiceRunning(ChatHeadService.class, mContext))
			{
				mContext.stopService(new Intent(mContext, ChatHeadService.class));
			}
		}
	}
}