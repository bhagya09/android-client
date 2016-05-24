package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class BootService extends BroadcastReceiver
{

	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Logger.i("HikeBootService", "Received onBoot intent");
		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();

		// GCM_ID_SENT_PRELOAD=true,User Auth=false-->Best Scenario

		if (mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false) && (!Utils.isUserAuthenticated(ctx)))
		{
			PreloadNotificationSchedular.scheduleNextAlarm(ctx);
		}
		//TODO  remove this check
		if (TextUtils.isEmpty(mprefs.getData(HikeMessengerApp.TOKEN_SETTING, null)))
		{
			return;
		}		
		
		if(!Utils.shouldConnectToMQTT())
		{
			return;
		}
		
		Intent startServiceIntent = new Intent(ctx, HikeService.class);
		ctx.startService(startServiceIntent);		
	}
}
