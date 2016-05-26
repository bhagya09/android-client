package com.bsb.hike.ces;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.ces.disk.CesDiskManager;
import com.bsb.hike.ces.ft.CesFtTask;
import com.bsb.hike.ces.ft.FTScoreComputation;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author suyash
 *
 */
public class CustomerExperienceScore {

	private final String TAG = "CustomerExperienceScore";

	private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

	private final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

	private final short KEEP_ALIVE_TIME = 60; // in seconds

	private final ExecutorService pool;

	private static volatile CustomerExperienceScore _instance = null;

	private CustomerExperienceScore()
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		pool = new ThreadPoolExecutor(1, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, Utils.threadFactory("CES Thread", false), rejectedExecutionHandler());
		if(!HikeSharedPreferenceUtil.getInstance().getData(CesConstants.CES_ALARM_PERF, false))
		{
			scheduleNextCesDataSync();
			HikeSharedPreferenceUtil.getInstance().saveData(CesConstants.CES_ALARM_PERF, true);
		}
	}

	public static CustomerExperienceScore getInstance()
	{
		if (_instance == null)
		{
			synchronized (CustomerExperienceScore.class)
			{
				if (_instance == null)
					_instance = new CustomerExperienceScore();
			}
		}
		return _instance;
	}

	private RejectedExecutionHandler rejectedExecutionHandler()
	{
		return new RejectedExecutionHandler()
		{
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
			{
				//ToDo handle this properly
			}
		};
	}

	private class CesFutureTask extends FutureTask<Void>
	{
		private CesBaseCallable task;

		public CesFutureTask(CesBaseCallable callable)
		{
			super(callable);
			this.task = callable;
		}

		private CesBaseCallable getTask()
		{
			return task;
		}

		@Override
		public void run()
		{
			Logger.d(getClass().getSimpleName(), "TimeCheck: Starting time : " + System.currentTimeMillis());
			super.run();
		}

		@Override
		protected void done()
		{
			super.done();
			Logger.d(getClass().getSimpleName(), "TimeCheck: Exiting  time : " + System.currentTimeMillis());
		}
	}

	public void recordCesData(int module, CesDataInfoFormatBuilder<?> cesData)
	{
		CesBaseCallable mTask = null;
		switch (module) {
		case CesConstants.CESModule.FT:
			mTask = new CesFtTask(cesData);
			break;
		default:
			break;
		}
		if(mTask != null)
		{
			CesFutureTask ft = new CesFutureTask(mTask);
			pool.execute(ft);
		}
	}

	public void processCesScoreAndL1Data()
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				JSONObject cesScore_data = null;
				JSONObject sData = null;
				try {
					cesScore_data = new JSONObject();
					sData = new JSONObject();

					ScoreComputationImpl ftCompute = new FTScoreComputation();
					JSONObject ft_score = ftCompute.computeScore();

					if(ft_score != null)
					{
						sData.put(CesConstants.CES_SCORE, ft_score);
						cesScore_data.put(CesUtils.getDayBeforeUTCDate(), sData);
						
						CesTransport transport = new CesTransport();
						JSONObject response = transport.sendCesScore(cesScore_data);
						if(response != null && response.has(HikeConstants.DATA_2))
						{
							JSONObject respL1Data = response.getJSONObject(HikeConstants.DATA_2);
							if(respL1Data.has(CesConstants.L1_DATA_REQUIRED))
							{
								JSONObject respData = respL1Data.getJSONObject(CesConstants.L1_DATA_REQUIRED);
								String date = CesUtils.getDayBeforeUTCDate();
								if(respData.has(date))
								{
									JSONObject requiredData = respData.getJSONObject(date);
									JSONObject cesl1Data = new JSONObject();
									JSONObject allModuleData = new JSONObject();
									if(requiredData.has(CesConstants.FT_MODULE))
									{
										JSONObject ftModuleData = ftCompute.getL1Data(requiredData.getJSONArray(CesConstants.FT_MODULE));
										if(ftModuleData != null)
										{
											allModuleData.put(CesConstants.FT_MODULE, ftModuleData);
										}
									}
									cesl1Data.put(CesUtils.getDayBeforeUTCDate(), allModuleData);
									transport.sendCesLevelOneInfo(cesl1Data);
								}
							}
						}
					}
				} catch (JSONException e)
				{
					Logger.e(TAG, "Parsing error : ", e);
				}
				finally
				{
					CesDiskManager.deleteCesDataOnAndBefore(CesUtils.getDayBeforeUTCDate());
					scheduleNextCesDataSync();
				}
			}
		});
	}

	public void processCesL2Data(final String module, final String date)
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				if(CesUtils.whichModule(module) != -1)
				{
					CesDiskManager disk = new CesDiskManager(CesUtils.whichModule(module), date, CesDiskManager.DataFlushMode.FLUSH);
					disk.dumpCesL2Data();
				}
			}
		});
	}

	/**
	 * Schedules next auto CES data sync
	 */
	public void scheduleNextCesDataSync()
	{
		Random rand = new Random();		
		int hr = rand.nextInt(CesConstants.DAY_IN_HOUR);

		Calendar calendar = Calendar.getInstance();
		Logger.d(TAG, "Previous day schedule = " + calendar.getTimeInMillis());
		calendar.set(Calendar.DATE, (calendar.get(Calendar.DATE) + 1));
		calendar.set(Calendar.HOUR_OF_DAY, hr);
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		long scheduleTime = calendar.getTimeInMillis();
		Logger.d(TAG, "ScheduleTime = " + scheduleTime);

		Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
		HikeAlarmManager.setAlarmPersistance(mContext, scheduleTime, HikeAlarmManager.REQUESTCODE_PERIODIC_CES_DATA_SYNC, true, true);
		Logger.d(TAG, "Scheduled next CES data sync for: " + Utils.getFormattedDateTimeFromTimestamp(scheduleTime / 1000, mContext.getResources().getConfiguration().locale));
	}
}
