package com.bsb.hike.ces;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.ces.CesConstants.CESModule;
import com.bsb.hike.ces.ft.CesFtTask;
import com.bsb.hike.ces.ft.FTScoreComputation;
import com.bsb.hike.models.HikeHandlerUtil;
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

	private CustomerExperienceScore(Context ctx)
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		pool = new ThreadPoolExecutor(1, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, Utils.threadFactory("CES Thread", false), rejectedExecutionHandler());
	}

	public static CustomerExperienceScore getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (CustomerExperienceScore.class)
			{
				if (_instance == null)
					_instance = new CustomerExperienceScore(context.getApplicationContext());
			}
		}
		return _instance;
	}

	public static RejectedExecutionHandler rejectedExecutionHandler()
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

	public void processCesData()
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
					}
					cesScore_data.put(CesUtils.getDayBeforeUTCDate(), sData);
					
					CesTransport transport = new CesTransport();
					JSONObject response = transport.sendCesScore(cesScore_data);
					if(response != null && response.has(CesConstants.L1_DATA_REQUIRED))
					{
						JSONObject respData = response.getJSONObject(CesConstants.L1_DATA_REQUIRED);
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
							boolean isUploaded = transport.sendCesLevelOneInfo(cesl1Data);
						}
					}
				} catch (JSONException e)
				{
					Logger.e(TAG, "Parsing error : ", e);
					e.printStackTrace();
				}
			}
		});
	}
}
