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

	private final static  long ALARM_EXPIRY_THRESHOLD =10 * 60 * 1000;
	
	
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

		/**if the service is restarted then a null intent is retured
		
		http://developer.android.com/reference/android/app/IntentService.html#onStartCommand(android.content.Intent,%20int,%20int)
		*/
		if (intent == null)
		{
			return;
		}
		try
		{
			long time = intent.getLongExtra(HikeAlarmManager.ALARM_TIME, HikeAlarmManager.REQUESTCODE_DEFAULT);
			
			/*
			 * Here I am taking into account the optimization that the CPU will do to alarms ...So if you schedule an alarm at 3:00 pm and the alarm gets fired at 3:05 
			 *  we are assuming that it is due to system optimization.
			 */
			
			if(Math.abs(time - System.currentTimeMillis()) < (ALARM_EXPIRY_THRESHOLD))
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
