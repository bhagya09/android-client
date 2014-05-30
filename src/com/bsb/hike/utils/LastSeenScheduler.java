package com.bsb.hike.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.tasks.FetchBulkLastSeenTask;
import com.bsb.hike.tasks.FetchLastSeenTask;
import com.bsb.hike.tasks.FetchBulkLastSeenTask.FetchBulkLastSeenCallback;
import com.bsb.hike.tasks.FetchLastSeenTask.FetchLastSeenCallback;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;

public class LastSeenScheduler
{
	public static interface LastSeenFetchedCallback
	{
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime);
	}

	private static LastSeenScheduler lastSeenScheduler;

	private boolean shouldFetchBulkLastSeen;

	private Context context;

	private String msisdn;

	private LastSeenFetchedCallback lastSeenFetchedCallback;

	private FetchLastSeenTask fetchLastSeenTask;

	private FetchBulkLastSeenTask fetchBulkLastSeenTask;

	private Handler mHandler;

	private int retryCount = 0;

	private int retryWaitTime = 0;

	private LastSeenScheduler(Context context)
	{
		this.context = context;
		this.mHandler = new Handler(Looper.getMainLooper());
	}

	public static LastSeenScheduler getInstance(Context context)
	{
		if (lastSeenScheduler == null)
		{
			lastSeenScheduler = new LastSeenScheduler(context.getApplicationContext());
		}
		return lastSeenScheduler;
	}

	public void start(boolean fetchBulkLastSeen)
	{
		fetchBulkLastSeenRunnable.run();
	}

	public void start(String msisdn, LastSeenFetchedCallback lastSeenFetchedCallback)
	{
		fetchLastSeenRunnable.run();
	}

	public void stop()
	{
		resetLastSeenRetryParams();

		if (fetchLastSeenTask != null)
		{
			fetchLastSeenTask.cancel(true);
		}
		if (fetchBulkLastSeenTask != null)
		{
			fetchBulkLastSeenTask.cancel(true);
		}
		mHandler.removeCallbacks(fetchLastSeenRunnable);
		mHandler.removeCallbacks(fetchBulkLastSeenRunnable);
	}

	private FetchLastSeenCallback lastSeenCallback = new FetchLastSeenCallback()
	{

		@Override
		public void lastSeenNotFetched()
		{
			scheduleRetry();
		}

		@Override
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime)
		{
			Log.d("TestLastSeen", "Last seen fetched!");
			resetLastSeenRetryParams();
			lastSeenFetchedCallback.lastSeenFetched(msisdn, offline, lastSeenTime);
		}
	};

	private FetchBulkLastSeenCallback bulkLastSeenCallback = new FetchBulkLastSeenCallback()
	{
		@Override
		public void bulkLastSeenNotFetched()
		{
			Log.d("TestLastSeen", "unable to fetch bulk last seen");
			scheduleRetry();
		}

		@Override
		public void bulkLastSeenFetched()
		{
			Log.d("TestLastSeen", "fetched bulk last seen");
			resetLastSeenRetryParams();
		}
	};

	private void scheduleRetry()
	{
		if (retryCount >= HikeConstants.MAX_LAST_SEEN_RETRY_COUNT - 1)
		{
			Log.d("TestLastSeen", "Last seen not fetched. Crossed max retries");
			return;
		}

		retryCount++;
		retryWaitTime += HikeConstants.RETRY_WAIT_ADDITION;

		Log.d("TestLastSeen", "Last seen not fetched. Retrying in " + retryWaitTime);

		mHandler.postDelayed(shouldFetchBulkLastSeen ? fetchBulkLastSeenRunnable : fetchLastSeenRunnable, retryWaitTime * 1000);
	}

	private void resetLastSeenRetryParams()
	{
		retryCount = 0;
		retryWaitTime = 0;
	}

	private Runnable fetchLastSeenRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			Log.d("TestLastSeen", "Retrying Last seen");
			fetchLastSeenTask = new FetchLastSeenTask(context, msisdn, lastSeenCallback);
			fetchLastSeenTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	};

	private Runnable fetchBulkLastSeenRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			Log.d("TestLastSeen", "Retrying bulk Last seen");
			fetchBulkLastSeenTask = new FetchBulkLastSeenTask(context, bulkLastSeenCallback);
			fetchBulkLastSeenTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	};
}
