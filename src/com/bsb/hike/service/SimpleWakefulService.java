package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;

import com.bsb.hike.models.HikeAlarmManager;

/**
 * 
 * @author himanshu
 * 
 *         This class is used to perform task that are received in AlarmBroadcastReceiver.The method onHandle Intent runs on backgroundthread.
 */
public class SimpleWakefulService extends IntentService
{

	public SimpleWakefulService()
	{
		super("SimpleWakeFulService");
	}

	public SimpleWakefulService(String name)
	{
		super("SimpleWakeFulService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{

		try
		{
			long time = intent.getLongExtra(HikeAlarmManager.ALARM_TIME, HikeAlarmManager.REQUESTCODE_DEFAULT);

			if (time > System.currentTimeMillis())
				HikeAlarmManager.processTasks(intent, this);
			else
				HikeAlarmManager.processExpiredTask(intent, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			AlarmBroadcastReceiver.completeWakefulIntent(intent);
		}

	}

}
