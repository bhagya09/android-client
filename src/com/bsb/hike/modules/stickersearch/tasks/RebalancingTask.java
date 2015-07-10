package com.bsb.hike.modules.stickersearch.tasks;

import android.content.Intent;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.Logger;

public class RebalancingTask implements Runnable
{
	private static final String TAG = "Rebalancing";
	
	private Intent intent;
	
	private boolean result;
	
	public RebalancingTask(Intent intent)
	{
		this.intent = intent;
	}
	
	@Override
	public void run()
	{
		Logger.d(TAG, "rebalancing started");
		
		//result = startRebalancing() TODO call rebalance function here
		if(result)
		{
			HikeAlarmManager.deleteAlarmFromDatabase(intent);
			StickerSearchManager.getInstance().setAlarm();
		}
		
		Logger.d(TAG, "rebalancing completed with result : " + result);
	}

}
