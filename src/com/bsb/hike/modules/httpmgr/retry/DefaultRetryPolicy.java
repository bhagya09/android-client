package com.bsb.hike.modules.httpmgr.retry;

import com.bsb.hike.modules.httpmgr.exception.HttpException;

/**
 * This class implements {@link IRetryPolicy} and defines a default retry policy which is used in case does not set any retry policy. It defines two constructors one that usses
 * default values and other in which user can give retry count , retry delay and back off multiplier
 * 
 * @author sidharth
 * 
 */
public class DefaultRetryPolicy implements IRetryPolicy
{
	/** The default number of retry attempts. */
	private static final int DEFAULT_RETRY_COUNT = 1;

	/** The default delay before retry a request (in ms). */
	private static final int DEFAULT_RETRY_DELAY = 2000;

	/** The max retry delay value (2 min) which should not be exceeded in retry process (in ms) */
	public static final int MAX_RETRY_DELAY = 2 * 60 * 1000;

	/** The default back off multiplier. */
	public static final float DEFAULT_BACKOFF_MULTIPLIER = 1f;

	private int retryCount;

	private int retryDelay;

	private float backOffMultiplier;

	/**
	 * This constructor uses default values for retry count , retry delay and back off multiplier
	 * 
	 * @see #DEFAULT_RETRY_COUNT
	 * @see #DEFAULT_RETRY_DELAY
	 * @see #DEFAULT_BACKOFF_MULTIPLIER
	 */
	public DefaultRetryPolicy()
	{
		this.retryCount = DEFAULT_RETRY_COUNT;
		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.backOffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
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
	public DefaultRetryPolicy(int retryCount, int retryDelay, float backOffMultiplier)
	{
		this.retryCount = retryCount;
		this.retryDelay = retryDelay;
		this.backOffMultiplier = backOffMultiplier;
	}

	/**
	 * @see IRetryPolicy#getRetryCount()
	 */
	@Override
	public int getRetryCount()
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
	 * This method returns the back off multiplier after each retry
	 * 
	 * @return
	 */
	public float getBackOffMultiplier()
	{
		return backOffMultiplier;
	}

	/**
	 * Decreases the retry count and changes the delay between retries using back off multiplier parameter
	 * 
	 * @see IRetryPolicy#retry(HttpException)
	 * @see HttpException
	 */
	@Override
	public void retry(HttpException ex)
	{
		retryCount--;
		retryDelay = (int) (retryDelay * backOffMultiplier);
	}
}