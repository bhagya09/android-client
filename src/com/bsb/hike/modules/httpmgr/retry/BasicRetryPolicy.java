package com.bsb.hike.modules.httpmgr.retry;

import static com.bsb.hike.modules.httpmgr.request.RequestConstants.POST;
import static java.net.HttpURLConnection.HTTP_LENGTH_REQUIRED;

import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;

/**
 * This class implements {@link IRetryPolicy} and defines a default retry policy which is used in case does not set any retry policy. It defines two constructors one that usses
 * default values and other in which user can give retry count , retry delay and back off multiplier
 * 
 * @author sidharth
 * 
 */
public class BasicRetryPolicy
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
	public BasicRetryPolicy()
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
	public BasicRetryPolicy(int retryCount, int retryDelay, float backOffMultiplier)
	{
		this.retryCount = retryCount;
		this.retryDelay = retryDelay;
		this.backOffMultiplier = backOffMultiplier;
	}

	/**
	 * This method returns the number of retries of a request
	 * 
	 * @return
	 */
	public int getRetryCount()
	{
		return retryCount;
	}

	/**
	 * This method returns the delay between retries of a request
	 * 
	 * @return
	 */
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
	 * This is the main method of the retry policy and contains all the logic of retrying request which handles some errors in which request parameters need to be changed and plays
	 * with the parameters retry count , retry delay and back off multiplier depending on the exception {@link HttpException} that occurred during previous execution
	 * 
	 * @param requestFacade
	 * @param ex
	 */
	public void retry(RequestFacade requestFacade, HttpException ex)
	{
		handleError(requestFacade, ex);
		changeRetryParameters(requestFacade, ex);
	}

	/**
	 * Decreases the retry count and changes the delay between retries using back off multiplier parameter
	 * 
	 * @param requestFacade
	 * @param ex
	 */
	protected void changeRetryParameters(RequestFacade requestFacade, HttpException ex)
	{
		retryCount--;
		retryDelay = (int) (retryDelay * backOffMultiplier);
	}

	/**
	 * Makes changes to request parameters based on error code
	 * 
	 * @param requestFacade
	 * @param ex
	 */
	protected void handleError(RequestFacade requestFacade, HttpException ex)
	{
		switch (ex.getErrorCode())
		{
		case HTTP_LENGTH_REQUIRED:
			handle411Error(requestFacade);
			break;
		default:
			break;
		}
	}

	/**
	 * Handles HTTP_LENGTH_REQUIRED response code. In this case we retry request with original body and without "content-encoding":"gzip" header
	 * 
	 * @param ex
	 */
	protected void handle411Error(RequestFacade requestFacade)
	{
		if (requestFacade.getMethod() == POST && requestFacade.getBody() != null)
		{
			IRequestBody requestBody = requestFacade.getBody();
			if (requestBody instanceof GzipRequestBody)
			{
				requestFacade.setBody(((GzipRequestBody) requestBody).getOriginalBody());
				HttpUtils.removeHeader(requestFacade.getHeaders(), "Content-Encoding", "gzip");
				requestFacade.getRequestInterceptors().remove("gzip");
			}
		}
	}
}