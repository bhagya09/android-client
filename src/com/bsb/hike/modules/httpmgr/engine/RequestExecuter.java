package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.request.RequestConstants.POST;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_AUTH_FAILURE;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_CONNECTION_TIMEOUT;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_MALFORMED_URL;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_NO_NETWORK;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_RESPONSE_PARSING_ERROR;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_SERVER_ERROR;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_SOCKET_TIMEOUT;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_UNEXPECTED_ERROR;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_CONTENT_LENGTH_REQUIRED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_LENGTH_REQUIRED;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.UUID;

import org.apache.http.conn.ConnectTimeoutException;

import com.bsb.hike.modules.httpmgr.DefaultHeaders;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants;
import com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsLogger;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.network.NetworkChecker;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.utils.Utils;

/**
 * This class is responsible for submitting the {@link Request} to the {@link HttpEngine} for engine and decides whether to execute the request asynchronously or synchronously
 * based on request parameters. Also handle exceptions and retries based on {@link IRetryPolicy} set in the request object
 * 
 * @author sidharth
 * 
 */
public class RequestExecuter
{
	private IClient client;

	private Request<?> request;

	private HttpEngine engine;

	private IResponseListener listener;

	private Response response;
	
	private boolean allInterceptorsExecuted;

	private String trackId;
	
	public RequestExecuter(IClient client, HttpEngine engine, Request<?> request, IResponseListener listener)
	{
		this.client = client;
		this.engine = engine;
		this.request = request;
		this.listener = listener;
		checkAndInitializeAnalyticsFields();
	}

	private void checkAndInitializeAnalyticsFields()
	{
		if (HttpAnalyticsLogger.shouldSendLog(request.getUrl()))
		{
			this.trackId = UUID.randomUUID().toString();
			this.request.addHeader(HttpAnalyticsConstants.TRACK_ID_HEADER_KEY, trackId);
		}
	}

	/**
	 * Starts the request interceptor chain for pre processing of request before executing
	 */
	private void preProcess()
	{
		LogFull.d("Pre-processing started for " + request.toString());
		RequestFacade requestFacade = new RequestFacade(request);
		Iterator<IRequestInterceptor> iterator = requestFacade.getRequestInterceptors().iterator();
		RequestInterceptorChain chain = new RequestInterceptorChain(iterator, requestFacade);
		chain.proceed();
		/**
		 * This is to handle the case in which one of interceptor in the pipeline do not chain.proceed() then we have to clear request and response objects and also remove this
		 * request from map
		 */
		if (!allInterceptorsExecuted)
		{
			HttpUtils.finish(request, response);
		}
	}

	/**
	 * Processes request synchronously or asynchronously based on request parameters
	 * 
	 * @see #execute(boolean, long)
	 */
	public void execute()
	{
		execute(true, 0);
	}

	/**
	 * Processes request synchronously or asynchronously based on request parameters
	 * 
	 * @param firstTry
	 *            true if it's an first attempt to execute a request otherwise false
	 * @param delay
	 *            delay between the retries
	 */
	private void execute(final boolean firstTry, long delay)
	{
		if (request.isAsynchronous())
		{
			processAsync(firstTry, delay);
		}
		else
		{
			processSync(firstTry, delay);
		}
	}

	/**
	 * Processes the request synchronously i.e. same thread on which it is submitted
	 * 
	 * @param firstTry
	 * @param delay
	 */
	private void processSync(boolean firstTry, long delay)
	{
		if (firstTry)
		{
			preProcess();
		}
		else
		{
			try
			{
				LogFull.d("retrying " + request.toString() + "sleeping for delay : " + delay);
				Thread.sleep(delay);
				processRequest();
			}
			catch (InterruptedException e)
			{
				LogFull.e(e, "excetion : ");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Processes the request asynchronously , a {@link RequestCall} is created which is passed to engine for async execution
	 * 
	 * @param firstTry
	 * @param delay
	 */
	private void processAsync(final boolean firstTry, long delay)
	{
		RequestCall call = new RequestCall(request)
		{
			@Override
			public void execute()
			{
				try
				{
					if (firstTry)
					{
						preProcess();
					}
					else
					{
						processRequest();
					}
				}
				finally
				{
					finish(this);
					request.setRequestCancellationListener(null);
				}
			}
		};

		engine.submit(call, delay);
	}

	private void finish(RequestCall requestCall)
	{
		engine.solveStarvation(requestCall);
	}

	/**
	 * Checks if network is available or not and request is cancelled or not. If network is available and request is not cancelled yet then executes the request using
	 * {@link IClient}
	 * 
	 */
	public void processRequest()
	{
		LogFull.d(request.toString() + " processing started");

		if (!NetworkChecker.isNetworkAvailable())
		{
			if (!request.isCancelled())
			{
				LogFull.e("no network");
				listener.onResponse(null, new HttpException(REASON_CODE_NO_NETWORK));
				return;
			}
		}

		try
		{
			if (request.isCancelled())
			{
				LogFull.i(request.toString() + "is already cancelled");
				return;
			}

			/**
			 * add default headers to the request
			 */
			DefaultHeaders.applyDefaultHeaders(request);

			/** Logging request for analytics */
			HttpAnalyticsLogger.logHttpRequest(trackId, request.getUrl(), request.getMethod(), request.getAnalyticsParam());

			response = client.execute(request);
			if (response.getStatusCode() < 200 || response.getStatusCode() > 299)
			{
				throw new IOException();
			}

			LogFull.d(request.toString() + " completed");
			
			notifyResponseToRequestRunner();
		}
		catch (SocketTimeoutException ex)
		{
			HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_SOCKET_TIMEOUT, request.getMethod(), request.getAnalyticsParam());
			handleRetry(ex, REASON_CODE_SOCKET_TIMEOUT);
		}
		catch (ConnectTimeoutException ex)
		{
			HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_CONNECTION_TIMEOUT, request.getMethod(), request.getAnalyticsParam());
			handleRetry(ex, REASON_CODE_CONNECTION_TIMEOUT);
		}
		catch (MalformedURLException ex)
		{
			HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_MALFORMED_URL, request.getMethod(), request.getAnalyticsParam());
			handleException(ex, REASON_CODE_MALFORMED_URL);
		}
		catch (IOException ex)
		{
			int statusCode = 0;
			if (response != null)
			{
				HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), response.getStatusCode(), request.getMethod(), request.getAnalyticsParam());
				statusCode = response.getStatusCode();
			}
			else
			{
				HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_NO_NETWORK, request.getMethod(), request.getAnalyticsParam(), Utils.getStackTrace(ex));
				handleRetry(ex, REASON_CODE_NO_NETWORK);
				return;
			}
			if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN)
			{
				handleException(ex, REASON_CODE_AUTH_FAILURE);
			}
			/**
			 * in case of response code == 411 we make a retry without gzip
			 */
			else if(statusCode == HTTP_LENGTH_REQUIRED)
			{
				handle411Error(ex);
			}
			else
			{
				handleException(ex, REASON_CODE_SERVER_ERROR);
				return;
			}
		}
		catch (Throwable ex)
		{
			HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_UNEXPECTED_ERROR, request.getMethod(), request.getAnalyticsParam(), Utils.getStackTrace(ex));
			handleException(ex, REASON_CODE_UNEXPECTED_ERROR);
		}
	}

	private void notifyResponseToRequestRunner()
	{
		ResponseBody<?> body = response.getBody();
		if (null == body || null == body.getContent())
		{
			LogFull.d("null response for  " + request.getUrl());
			HttpAnalyticsLogger.logResponseReceived(trackId, request.getUrl(), REASON_CODE_RESPONSE_PARSING_ERROR, request.getMethod(), request.getAnalyticsParam());
			listener.onResponse(null, new HttpException(REASON_CODE_RESPONSE_PARSING_ERROR, "response parsing error"));
		}
		else
		{
			LogFull.d("positive response for : " + request.getUrl());
			// positive response
			HttpAnalyticsLogger.logSuccessfullResponseReceived(trackId, request.getUrl(), response.getStatusCode(), request.getMethod(), request.getAnalyticsParam());
			listener.onResponse(response, null);
		}	
	}
	
	/**
	 * Handles HTTP_LENGTH_REQUIRED response code. In this case we retry request with original body and without "content-encoding":"gzip" header
	 * @param ex
	 */
	private void handle411Error(Exception ex)
	{
		if(request.getMethod() == POST && request.getBody() != null)
		{
			IRequestBody requestBody = request.getBody();
			if(requestBody instanceof GzipRequestBody)
			{
				request.setBody(((GzipRequestBody) requestBody).getOriginalBody());
			}
			HttpUtils.removeHeader(request.getHeaders(), "Content-Encoding", "gzip");
			request.getRequestInterceptors().remove("gzip");
			handleRetry(ex, REASON_CODE_CONTENT_LENGTH_REQUIRED);
		}
	}

	/**
	 * Handles the exception that occurs while executing the request, and in case of {@link IOException} handle retries based on {@link IRetryPolicy}
	 * 
	 * @param ex
	 */
	private void handleException(Throwable ex, int reasonCode)
	{
		LogFull.e(ex, "exception occured for " + request.toString());
		listener.onResponse(null, new HttpException(reasonCode, ex));
	}

	/**
	 * Handles the retries of the request based on {@link IRetryPolicy}
	 * 
	 * @param ex
	 */
	private void handleRetry(Exception ex, int responseCode)
	{
		HttpException httpException = new HttpException(responseCode, ex);
		if (null != request.getRetryPolicy())
		{
			IRetryPolicy retryPolicy = request.getRetryPolicy();
			retryPolicy.retry(httpException);
			if (retryPolicy.getRetryCount() >= 0)
			{
				LogFull.i("retring " + request.toString());
				execute(false, retryPolicy.getRetryDelay());
			}
			else
			{
				LogFull.i("max retry count reached for " + request.toString());
				listener.onResponse(null, httpException);
			}
		}
		else
		{
			LogFull.i("no retry policy retuning for " + request.toString());
			listener.onResponse(null, httpException);
		}
	}

	/**
	 * This class implements {@link IRequestInterceptor.Chain} and executes {@link IRequestInterceptor#intercept(IRequestInterceptor.Chain) for each node present in the interceptor chain
	 * @author sidharth
	 *
	 */
	public class RequestInterceptorChain implements IRequestInterceptor.Chain
	{
		private Iterator<IRequestInterceptor> iterator;

		private RequestFacade requestFacade;

		public RequestInterceptorChain(Iterator<IRequestInterceptor> iterator, RequestFacade requestFacade)
		{
			this.iterator = iterator;
			this.requestFacade = requestFacade;
		}

		@Override
		public RequestFacade getRequestFacade()
		{
			return requestFacade;
		}

		@Override
		public void proceed()
		{
			if (iterator.hasNext())
			{
				RequestInterceptorChain chain = new RequestInterceptorChain(iterator, requestFacade);
				iterator.next().intercept(chain);
			}
			else
			{
				LogFull.d("Pre-processing completed for " + request.toString());
				if (RequestProcessor.isRequestDuplicateAfterInterceptorsProcessing(request))
				{
					return;
				}
				allInterceptorsExecuted = true;
				processRequest();
			}
		}
	}
}
