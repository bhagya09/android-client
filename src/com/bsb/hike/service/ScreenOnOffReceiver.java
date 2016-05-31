package com.bsb.hike.service;

import com.bsb.hike.chatHead.ChatHeadUtils;
import com.hike.cognito.UserLogInfo;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenOnOffReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Object logger;
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			if (Utils.isUserOnline(context))
			{
				HikeMqttManagerNew.getInstance().connectOnMqttThread();
			}
			ChatHeadUtils.startOrStopService(false);
		}
		else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			UserLogInfo.recordSessionInfo(ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS), UserLogInfo.STOP);
			ChatHeadUtils.stopService();
		}
	}

}
