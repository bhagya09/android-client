package com.bsb.hike.modules.stickerdownloadmgr.retry;

import java.util.Random;

import com.bsb.hike.utils.Logger;

/**
 * Default {@link RetryPolicy} implementation. Proposes an exponential back off algorithm. When {@link #getRetryCount()} returns 0, the request is not retried anymore and will
 * fail. Between each retry attempt, the request processor will sleep for {@link #getDelayBeforeRetry()} milliseconds.
 * 
 *
 */
public class DefaultRetryPolicy implements IRetryPolicy
{

	/** The default number of retry attempts. */
	public static final int DEFAULT_MAX_RETRY_COUNT = 1;

	/** The default delay before retry a request (in ms). */
	public static final long DEFAULT_DELAY_BEFORE_RETRY = 2500;

	public static final int DEFAULT_MAX_RECONNECT_TIME = 20;

	/** The default backoff multiplier. */
	public static final float DEFAULT_BACKOFF_MULT = 1f;

	/** The number of retry attempts. */
	private int retryCount = 0;

	private int maxReconnectTime = DEFAULT_MAX_RECONNECT_TIME;

	private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;

	/**
	 * The delay to wait before next retry attempt. Will be multiplied by {@link #backOffMultiplier} between every retry attempt.
	 */
	private long reconnectTime = 0;

	/**
	 * The backoff multiplier. Will be multiplied by {@link #delayBeforeRetry} between every retry attempt.
	 */
	private float backOffMultiplier = DEFAULT_BACKOFF_MULT;

	private boolean retry = true;

	private String taskId;

	// ----------------------------------
	// CONSTRUCTORS
	// ----------------------------------
	public DefaultRetryPolicy(String taskId, int maxRetryCount, int maxReconnectTime)
	{
		this.maxRetryCount = maxRetryCount;
		this.maxReconnectTime = maxReconnectTime;
		this.taskId = taskId;
	}

	public DefaultRetryPolicy(String taskId)
	{
		this(taskId, DEFAULT_MAX_RETRY_COUNT, DEFAULT_MAX_RECONNECT_TIME);
	}

	// ----------------------------------
	// PUBLIC API
	// ----------------------------------

	public int getRetryCount()
	{
		return retryCount;
	}

	public void retry(Exception error) throws Exception
	{

		if (retry && retryCount < maxRetryCount)
		{
			// make first attempt within first 5 seconds
			if (reconnectTime == 0)
			{
				Random random = new Random();
				reconnectTime = random.nextInt(5) + 1;
			}
			else
			{
				reconnectTime *= 2;
			}
			reconnectTime = reconnectTime > maxReconnectTime ? maxReconnectTime : reconnectTime;
			try
			{
				Thread.sleep(reconnectTime * 1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.d(getClass().getSimpleName(), "Sleep interrupted: " + Thread.currentThread().toString());
				e.printStackTrace();
			}
			retryCount++;
			Logger.d(getClass().getSimpleName(), "StD retry # : " + retryCount + " for taskId : " + taskId);

		}
		else
		{
			retryCount++;
			Logger.d(getClass().getSimpleName(), "Returning false on retry attempt No. " + retryCount);
			throw error;
		}
	}
	
	void stopOnMaxRetry(int retryCount)
	{
		if(retryCount == maxRetryCount)
		{
			retry = false;
		}
	}
	
	long getDelayBeforeRetry()
	{
		return reconnectTime;
	}

	boolean getRetry()
	{
		return this.retry;
	}

	public void setRetry(boolean retry)
	{
		this.retry = retry;
	}
	
	public void reset()
	{
		this.retry = true;
		this.reconnectTime = 0;
		this.retryCount = 0;
	}

}
