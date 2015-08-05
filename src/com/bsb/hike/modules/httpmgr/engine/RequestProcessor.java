package com.bsb.hike.modules.httpmgr.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IProgressListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;

/**
 * This class handles the duplicate check of the request and then submits the request to {@link RequestRunner}
 * 
 * @author sidharth & anubhav
 * 
 */
public class RequestProcessor
{
	private static Map<String, Request<?>> requestMap;

	private RequestRunner requestRunner;

	private RequestListenerNotifier requestListenerNotifier;

	public RequestProcessor(ClientOptions options, HttpEngine engine, RequestListenerNotifier notifier)
	{
		requestMap = new ConcurrentHashMap<String, Request<?>>();
		requestListenerNotifier = notifier;
		requestRunner = new RequestRunner(options, engine, requestListenerNotifier);
	}

	/**
	 * Checks request is duplicate or not if yes then updates the list of request listeners with listeners in request object.Then submits the request to {@link RequestRunner} for
	 * further processing
	 * 
	 * @param request
	 * @param options
	 *            {@link ClientOptions} options to be used for executing this request
	 */
	public void addRequest(final Request<?> request, ClientOptions options)
	{
		String requestId = request.getId();
		
		Request<?> req = requestMap.get(requestId);
		if (null != req)
		{
			LogFull.i(request.toString() + " already exists");
			req.addRequestListeners(request.getRequestListeners());
		}
		else
		{
			LogFull.d("adding " + request.toString() + " to request map");
			requestMap.put(requestId, request);
			IRequestCancellationListener listener = new IRequestCancellationListener()
			{
				@Override
				public void onCancel()
				{
					LogFull.i("on cancel called for " + request.toString() + "  removing from request map");
					requestListenerNotifier.notifyListenersOfRequestCancellation(request);
					removeRequest(request);
				}
			};
			request.setRequestCancellationListener(listener);

			IProgressListener progressListener = new IProgressListener()
			{
				@Override
				public void onProgressUpdate(float progress)
				{
					requestListenerNotifier.notifyListenersOfRequestProgress(request, progress);
				}
			};
			request.setProgressListener(progressListener);

			requestRunner.submit(request, options);
		}
	}

	/**
	 * Returns true if the request passed as parameter is running currently otherwise false
	 * 
	 * @param request
	 * @return
	 */
	public boolean isRequestRunning(Request<?> request)
	{
		String requestId = request.getId();
		if (requestId !=null && requestMap.containsKey(requestId))
		{
			LogFull.d(request.toString() + " is already running ");
			return true;
		}
		LogFull.d(request.toString() + " is not already running ");
		return false;
	}

	/**
	 * This method calculates the id of the request and compares it with previous calculated request id (this is needed because headers can be added inside the interceptors and
	 * original id calculated during request build up don't know about these headers added in interceptors, So we calculate again to see if this type of request is already in the
	 * http manager system or not). If request is already in system then we add the listeners of request to previous request object and also update the
	 * {@link RequestProcessor#requestMap} accordingly
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isRequestDuplicateAfterInterceptorsProcessing(Request<?> request)
	{
		String reqId = request.getId();
		String newRequestId = request.generateId();
		if (reqId.equals(newRequestId))
		{
			return false;
		}

		request.setId(newRequestId);
		if (requestMap.containsKey(newRequestId))
		{
			LogFull.i(request.toString() + " already exists");
			Request<?> req = requestMap.get(newRequestId);
			req.addRequestListeners(request.getRequestListeners());
			requestMap.remove(reqId);
			return true;
		}
		else
		{
			requestMap.put(newRequestId, request);
		}
		requestMap.remove(reqId);
		return false;
	}

	public static void removeRequest(Request<?> request)
	{
		if (request.getId() != null)
		{
			requestMap.remove(request.getId());
		}
	}

	/**
	 * Shutdown method to close everything (setting all variables to null for easy garbage collection)
	 */
	public void shutdown()
	{
		requestMap.clear();
		requestMap = null;
		requestRunner.shutdown();
		requestRunner = null;
		requestListenerNotifier.shutdown();
		requestListenerNotifier = null;
	}
}
