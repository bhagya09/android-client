package com.bsb.hike.modules.lastseenmgr;

import com.bsb.hike.modules.httpmgr.exception.HttpException;

import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;

public class LastSeenRetryPolicy extends BasicRetryPolicy
{

	/** The default number of retry attempts. */
	private static final int DEFAULT_RETRY_COUNT = 3;

	/** The max retry delay value (2 min) which should not be exceeded in retry process (in ms) */
	public static final int MAX_RETRY_DELAY = 2 * 60 * 1000;

	/** The default back off multiplier. */
	public static final float DEFAULT_RETRY_ADDITION_TIME = 2000;

	private int retryCount;

	private int retryDelay;

	private float retryDelayAddtionTime;

	/**
	 * This constructor uses default values for retry count , retry delay and retry delay addition time
	 * 
	 * @see #DEFAULT_RETRY_COUNT
	 * @see #DEFAULT_RETRY_DELAY
	 * @see #DEFAULT_RETRY_ADDITION_TIME
	 */
	public LastSeenRetryPolicy()
	{
		this.retryCount = DEFAULT_RETRY_COUNT;
		this.retryDelay = 0;
		this.retryDelayAddtionTime = DEFAULT_RETRY_ADDITION_TIME;
	}

	/**
	 * This constructor accepts three parameters which are used by the default retry policy
	 * 
	 * @param retryCount
	 *            number of retries
	 * @param retryDelay
	 *            delay between each retry
	 * @param backOffMultiplier
	 *            back off multiplier used to change delay between each retry
	 */
	public LastSeenRetryPolicy(int retryCount, int retryDelay, long retryDelayAddtionTime)
	{
		this.retryCount = retryCount;
		this.retryDelay = retryDelay;
		this.retryDelayAddtionTime = retryDelayAddtionTime;
	}

	/**
	 * @see IRetryPolicy#getRetryCount()
	 */
	@Override
	public int getNumOfRetries()
	{
		return retryCount;
	}

	/**
	 * @see IRetryPolicy#getRetryDelay()
	 */
	@Override
	public int getRetryDelay()
	{
		return retryDelay;
	}

	/**
	 * Decreases the retry count and changes the delay between retries using back off multiplier parameter
	 * 
	 * @see IRetryPolicy#retry(RequestFacade, HttpException)
	 * @see HttpException
	 */
	@Override
	protected void changeRetryParameters(RequestFacade requestFacade, HttpException ex)
	{
		retryCount--;
		retryDelay += retryDelayAddtionTime;
	}
}
