package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChatHeadServiceManager extends BroadcastReceiver
{
	static boolean flagScreen = true, snooze = false;

	static Context mContext = HikeMessengerApp.getInstance();

	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		
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
				Logger.d("ashish","screenoff");
				stopService();
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				Logger.d("ashish","screenon");
				startService();
			}
		}
		else
		{   
			snooze = false;
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