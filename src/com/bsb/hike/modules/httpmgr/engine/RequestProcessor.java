package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.gcmnetworkmanager.Config;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IProgressListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles the duplicate check of the request and then submits the request to {@link RequestRunner}
 * 
 * @author sidharth & anubhav
 * 
 */
public class RequestProcessor
{
	private static ConcurrentHashMap<String, Request<?>> requestMap;

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
		if(request.isWrongRequest())
		{
			requestListenerNotifier.notifyListenersOfRequestFailure(request, new HttpException(request.getWrongRequestErrorCode()));
			return;
		}

		String requestId = request.getId();

		Request<?> req = requestMap.putIfAbsent(requestId, request);
		if (req != null)
		{
			LogFull.i(request.toString() + " already exists");
			req.addRequestListeners(request.getRequestListeners());
		}
		else
		{
			if (request.getGcmTaskConfig() != null)
			{
				final Config config = HttpRequestStateDB.getInstance().getConfigFromDb(request.getGcmTaskConfig());
				request.setGcmTaskConfig(config);

				HikeHandlerUtil.getInstance().postAtFront(new Runnable()
				{
					@Override
					public void run()
					{
						HttpRequestStateDB.getInstance().insertBundleToDb(config.getTag(), config.toBundle());
					}
				});
			}

			LogFull.d("adding " + request.toString() + " to request map");
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

	public static void removeRequest(Request<?> request)
	{
		if (request.getId() != null)
		{
            LogFull.i(request.toString() + " removing key in map");
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
