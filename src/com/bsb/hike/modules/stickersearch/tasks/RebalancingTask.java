package com.bsb.hike.modules.stickersearch.tasks;

import android.content.Intent;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;

public class RebalancingTask implements Runnable
{
	private static final String TAG = RebalancingTask.class.getSimpleName();
	
	private Intent intent;
	
	private boolean result;
	
	public RebalancingTask(Intent intent)
	{
		this.intent = intent;
	}
	
	@Override
	public void run()
	{
		Logger.d(TAG, "Rebalancing started.");

		result = StickerSearchDataController.startRebalancing();
		HikeAlarmManager.deleteAlarmFromDatabase(intent);

		if (result)
		{
			StickerSearchManager.getInstance().setRebalancingAlarm();
		}

		Logger.d(TAG, "Rebalancing completed with result: " + result);
	}
}