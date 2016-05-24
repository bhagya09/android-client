package com.bsb.hike.modules.httpmgr.retry;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.HTTP_UNZIP_FAILED;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_UNKNOWN_HOST_EXCEPTION;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.FT_PRODUCTION_API;
import static com.bsb.hike.modules.httpmgr.request.RequestConstants.POST;
import static java.net.HttpURLConnection.HTTP_LENGTH_REQUIRED;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.PRODUCTION_API;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.PLATFORM_PRODUCTION_API;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.STICKERS_PRODUCTION_API;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.log.LogFull;
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
	public static final int DEFAULT_RETRY_COUNT = 1;

	/** The default delay before retry a request (in ms). */
	public static final int DEFAULT_RETRY_DELAY = 2000;

	/** The max retry delay value (2 min) which should not be exceeded in retry process (in ms) */
	public static final int MAX_RETRY_DELAY = 2 * 60 * 1000;

	/** The default back off multiplier. */
	public static final float DEFAULT_BACKOFF_MULTIPLIER = 1f;

	private int numOfRetries;

	private int retryIndex;

	private int retryDelay;

	private float backOffMultiplier;

	private List<String> hostUris;

	/**
	 * This constructor uses default values for retry count , retry delay and back off multiplier
	 * 
	 * @see #DEFAULT_RETRY_COUNT
	 * @see #DEFAULT_RETRY_DELAY
	 * @see #DEFAULT_BACKOFF_MULTIPLIER
	 */
	public BasicRetryPolicy()
	{
		this.numOfRetries = DEFAULT_RETRY_COUNT;
		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.backOffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
	}

	/**
	 * This constructor accepts three parameters which are used by the default retry policy
	 * 
	 * @param numOfRetries
	 *            number of retries
	 * @param retryDelay
	 *            delay between each retry
	 * @param backOffMultiplier
	 *            back off multiplier used to change delay between each retry
	 */
	public BasicRetryPolicy(int numOfRetries, int retryDelay, float backOffMultiplier)
	{
		this.numOfRetries = numOfRetries;
		this.retryDelay = retryDelay;
		this.backOffMultiplier = backOffMultiplier;
	}

	public void setHostUris(URL url)
	{
		switch (url.getHost())
		{
		case STICKERS_PRODUCTION_API:
		case PRODUCTION_API:
			this.hostUris = HttpManager.getProductionHostUris();
			break;
		case PLATFORM_PRODUCTION_API:
			this.hostUris = HttpManager.getPlatformProductionHostUris();
			break;
		case FT_PRODUCTION_API:
			this.hostUris = HttpManager.getFtHostUris();
			break;
		default:
			break;
		}
	}

	public int getRetryIndex()
	{
		return retryIndex;
	}

	/**
	 * This method returns the number of retries of a request
	 * 
	 * @return
	 */
	public int getNumOfRetries()
	{
		return numOfRetries;
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
		retryIndex++;
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
		case HTTP_UNZIP_FAILED:
		case HTTP_LENGTH_REQUIRED:
			handle411Error(requestFacade);
			break;
		case REASON_CODE_UNKNOWN_HOST_EXCEPTION:
			handleUnknownHostException(requestFacade);
			break;
		default:
			handleDefaultErrorCase(requestFacade, ex);
			break;
		}
	}

	protected void handleDefaultErrorCase(RequestFacade requestFacade, HttpException ex)
	{

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

	/**
	 * Handles {@link UnknownHostException}, retry with hardcoded IP as url host
	 * 
	 * @param ex
	 */
	protected void handleUnknownHostException(RequestFacade requestFacade)
	{
		URL url = requestFacade.getUrl();
		try
		{
			String fallBackHostUri = getFallbackHost();
			if (fallBackHostUri == null)
			{
				// retry with original host in case of null received from getFallbackHost() method
				return;
			}
			requestFacade.setUrl(new URL(url.getProtocol(), fallBackHostUri, url.getPort(), url.getFile()));
		}
		catch (MalformedURLException e)
		{
			LogFull.e("exception while setting url in case of unknown host exception", e);
		}
	}

	private String getFallbackHost()
	{
		if (null == hostUris || hostUris.size() == 0)
		{
			return null;
		}

		Random random = new Random();
		int index = random.nextInt(hostUris.size());
		return hostUris.get(index);
	}
}