package com.bsb.hike.modules.httpmgr.retry;

import com.bsb.hike.modules.httpmgr.exception.HttpException;

/**
 * This interface represents the retry policy used by the callers for their particular request. Retry policy contains the information of number of retry count, delay between
 * retries and back off multiplier after each retry
 * 
 * @author sidharth
 * 
 */
public interface IRetryPolicy
{
	/**
	 * This method returns the number of retries of a request
	 * 
	 * @return
	 */
	public int getRetryCount();

	/**
	 * This method returns the delay between retries of a request
	 * 
	 * @return
	 */
	public int getRetryDelay();

	/**
	 * This is the main method of the retry policy and contains all the logic of retrying request which plays with the parameters retry count , retry delay and back off multiplier
	 * depending on the exception {@link HttpException} that occurred during previous execution
	 * 
	 * @param ex
	 */
	public void retry(HttpException ex);
}