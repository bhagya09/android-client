package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_AUTH_FAILURE;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_CONNECTION_TIMEOUT;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_MALFORMED_URL;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_NO_NETWORK;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_SERVER_ERROR;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_SOCKET_TIMEOUT;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_UNEXPECTED_ERROR;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Iterator;

import org.apache.http.conn.ConnectTimeoutException;

import com.bsb.hike.modules.httpmgr.DefaultHeaders;
import com.bsb.hike.modules.httpmgr.Utils;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.network.NetworkChecker;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;

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

	public RequestExecuter(IClient client, HttpEngine engine, Request<?> request, IResponseListener listener)
	{
		this.client = client;
		this.engine = engine;
		this.request = request;
		this.listener = listener;
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
			Utils.finish(request, response);
		}
		else
		{
			if (RequestProcessor.isRequestDuplicateAfterInterceptorsProcessing(request))
			{
				return;
			}
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

			response = client.execute(request);

			if (response.getStatusCode() < 200 || response.getStatusCode() > 299)
			{
				throw new IOException();
			}

			LogFull.d(request.toString() + " completed");
			// positive response
			listener.onResponse(response, null);
		}
		catch (SocketTimeoutException ex)
		{
			handleRetry(ex, REASON_CODE_SOCKET_TIMEOUT);
		}
		catch (ConnectTimeoutException ex)
		{
			handleRetry(ex, REASON_CODE_CONNECTION_TIMEOUT);
		}
		catch (MalformedURLException ex)
		{
			handleException(ex, REASON_CODE_MALFORMED_URL);
		}
		catch (IOException ex)
		{
			int statusCode = 0;
			if (response != null)
			{
				statusCode = response.getStatusCode();
			}
			else
			{
				handleRetry(ex, REASON_CODE_NO_NETWORK);
				return;
			}

			if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN)
			{
				handleException(ex, REASON_CODE_AUTH_FAILURE);
			}
			else
			{
				handleException(ex, REASON_CODE_SERVER_ERROR);
				return;
			}
		}
		catch (Throwable ex)
		{
			handleException(ex, REASON_CODE_UNEXPECTED_ERROR);
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
				allInterceptorsExecuted = true;
				processRequest();
			}
		}
	}
}
