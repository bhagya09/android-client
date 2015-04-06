package com.bsb.hike.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.tasks.FetchBulkLastSeenTask;
import com.bsb.hike.tasks.FetchBulkLastSeenTask.FetchBulkLastSeenCallback;
import com.bsb.hike.tasks.FetchLastSeenTask;
import com.bsb.hike.tasks.FetchLastSeenTask.FetchLastSeenCallback;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;

public class LastSeenScheduler
{
	public static interface LastSeenFetchedCallback
	{
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime);
	}

	private class Retry
	{
		int retryCount;
		int retryWaitTime;
	}

	private static LastSeenScheduler lastSeenScheduler;

	private boolean shouldFetchBulkLastSeen;

	private Context context;

	private String msisdn;

	private LastSeenFetchedCallback lastSeenFetchedCallback;

	private FetchLastSeenTask fetchLastSeenTask;

	private FetchBulkLastSeenTask fetchBulkLastSeenTask;

	private Handler mHandler;

	private Retry retryBulk;

	private Retry retrySingle;

	private boolean fetchBulkLastSeenCancelled = false;

	private LastSeenScheduler(Context context)
	{
		this.context = context;
		this.mHandler = new Handler(Looper.getMainLooper());
		this.retryBulk = new Retry();
		this.retrySingle = new Retry();
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
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.BULK_LAST_SEEN_PREF, false))
		{
			return;
		}

		resetLastSeenRetryParams(retryBulk);
		/*
		 * We reset this flag so that the retry logic works now.
		 */
		fetchBulkLastSeenCancelled = false;
		if(fetchBulkLastSeenTask == null)
		{
			fetchBulkLastSeenTask = new FetchBulkLastSeenTask(context, bulkLastSeenCallback);
			fetchBulkLastSeenTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public void start(String msisdn, LastSeenFetchedCallback lastSeenFetchedCallback)
	{
		resetLastSeenRetryParams(retrySingle);

		this.msisdn = msisdn;
		this.lastSeenFetchedCallback = lastSeenFetchedCallback;
		fetchLastSeenRunnable.run();
	}

	public void stop(boolean stopBulkFetch)
	{
		if (!stopBulkFetch)
		{
			lastSeenFetchedCallback = null;

			resetLastSeenRetryParams(retrySingle);
			fetchLastSeenTask.cancel(true);

			mHandler.removeCallbacks(fetchLastSeenRunnable);
		}
		else
		{
			if (fetchBulkLastSeenTask != null)
			{
				resetLastSeenRetryParams(retryBulk);

				/*
				 * We set this task to ensure another bulk last seen task is not scheduled by the retry logic again.
				 */
				fetchBulkLastSeenCancelled = true;
			}
			mHandler.removeCallbacks(fetchBulkLastSeenRunnable);
		}

	}

	private FetchLastSeenCallback lastSeenCallback = new FetchLastSeenCallback()
	{

		@Override
		public void lastSeenNotFetched()
		{
			scheduleRetry(retrySingle);
		}

		@Override
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime)
		{
			resetLastSeenRetryParams(retrySingle);
			if (lastSeenFetchedCallback != null)
			{
				lastSeenFetchedCallback.lastSeenFetched(msisdn, offline, lastSeenTime);
			}
		}
	};

	private FetchBulkLastSeenCallback bulkLastSeenCallback = new FetchBulkLastSeenCallback()
	{
		@Override
		public void bulkLastSeenNotFetched()
		{
			fetchBulkLastSeenTask = null;

			if (fetchBulkLastSeenCancelled)
			{
				resetLastSeenRetryParams(retryBulk);
			}
			else
			{
				scheduleRetry(retryBulk);
			}
		}

		@Override
		public void bulkLastSeenFetched()
		{
			fetchBulkLastSeenTask = null;

			resetLastSeenRetryParams(retryBulk);
		}
	};

	private void scheduleRetry(Retry retry)
	{
		if (retry.retryCount >= HikeConstants.MAX_LAST_SEEN_RETRY_COUNT - 1)
		{
			return;
		}

		retry.retryCount++;
		retry.retryWaitTime += HikeConstants.RETRY_WAIT_ADDITION;

		mHandler.postDelayed(shouldFetchBulkLastSeen ? fetchBulkLastSeenRunnable : fetchLastSeenRunnable, retry.retryWaitTime * 1000);
	}

	private void resetLastSeenRetryParams(Retry retry)
	{
		retry.retryCount = 0;
		retry.retryWaitTime = 0;
	}

	private Runnable fetchLastSeenRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			/*
			 * Adding this check to ensure we don't make a request for empty/null msisdn.
			 * TODO figure out why this is happening
			 */
			if (TextUtils.isEmpty(msisdn))
			{
				Logger.w("LastSeen", "msisdn is null!");
				return;
			}
			fetchLastSeenTask = new FetchLastSeenTask(context, msisdn, lastSeenCallback);
			fetchLastSeenTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	};

	private Runnable fetchBulkLastSeenRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			fetchBulkLastSeenTask = new FetchBulkLastSeenTask(context, bulkLastSeenCallback);
			fetchBulkLastSeenTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
		}
	};
}
