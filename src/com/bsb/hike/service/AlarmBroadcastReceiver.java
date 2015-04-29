package com.bsb.hike.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * 
 * @author himanshu
 * 
 *         Broadcast Receiver for receiving alarms.It starts a service to perform the task associated with the alarm.
 */

public class AlarmBroadcastReceiver extends WakefulBroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{

		// TODO Auto-generated method stub

		Intent service = new Intent(context, SimpleWakefulService.class);
		service.putExtras(intent);

		startWakefulService(context, service);

	}

}
