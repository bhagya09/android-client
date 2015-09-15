package com.hike.transporter;

import com.hike.transporter.interfaces.IRetryPolicy;

public class DefaultRetryPolicy implements IRetryPolicy
{
	private static final int DEFAULT_RETRY_COUNT = 2;

	private static final int DEFAULT_RETRY_DELAY = 200;// ms

	public static final float DEFAULT_BACKOFF_MULTIPLIER = 1f;

	private int retryCount;

	private int retryDelay;

	private float backOffMultiplier;

	public DefaultRetryPolicy()
	{
		this.retryCount = DEFAULT_RETRY_COUNT;
		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.backOffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
	}

	public DefaultRetryPolicy(int retryCount, int retryDelay, float backOffMultiplier)
	{
		this.retryCount = retryCount;
		this.retryDelay = retryDelay;
		this.backOffMultiplier = backOffMultiplier;
	}

	public DefaultRetryPolicy(DefaultRetryPolicy defaultRetryPolicy)
	{
		this.retryCount = defaultRetryPolicy.retryCount;
		this.retryDelay = defaultRetryPolicy.retryDelay;
		this.backOffMultiplier = defaultRetryPolicy.backOffMultiplier;
	}

	@Override
	public int getRetryCount()
	{
		return retryCount;
	}

	@Override
	public int getRetryDelay()
	{
		return retryDelay;
	}

	@Override
	public void consumeRetry()
	{
		retryCount--;
		retryDelay = (int) (retryDelay * backOffMultiplier);
	}

	public void setRetryDone()
	{
		retryCount = -1;
		retryDelay = -1;
	}
}
