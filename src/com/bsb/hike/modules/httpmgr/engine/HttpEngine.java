package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.engine.HttpEngineConstants.CORE_POOL_SIZE;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Pair;

import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.modules.httpmgr.response.ResponseCall;
import com.squareup.okhttp.internal.Util;

/**
 * This class is core center of Http manager. It contains short , long and response executors for maintaining short running requests , long running requests and notifying response
 * respectively
 * 
 * @author anubhavgupta
 * 
 */
public class HttpEngine
{
	static short LONG_EXECUTER = 0x0;

	static short SHORT_EXECUTER = 0x1;

	/** Executor for executing for short running requests */
	private HttpExecuter shortRequestExecuter;

	/** Executor for executing for long running requests */
	private HttpExecuter longRequestExecuter;

	/** Executor for notifying the response */
	private ThreadPoolExecutor responseExecuter;

	/** Queue which maintains the order of the requests to be executed */
	private HttpQueue queue;

	/**
	 * Creates a new HttpEngine
	 */
	public HttpEngine()
	{
		LogFull.i("starting http engine");

		// executer used for serving short requests
		shortRequestExecuter = new HttpExecuter(this, SHORT_EXECUTER, CORE_POOL_SIZE, com.bsb.hike.utils.Utils.threadFactory("short-thread", false), com.bsb.hike.utils.Utils.rejectedExecutionHandler());

		// executer used for serving long requests
		longRequestExecuter = new HttpExecuter(this, LONG_EXECUTER, CORE_POOL_SIZE, com.bsb.hike.utils.Utils.threadFactory("long-thread", false), com.bsb.hike.utils.Utils.rejectedExecutionHandler());

		// executer used for serving response
		responseExecuter = new ThreadPoolExecutor(0, 2 * CORE_POOL_SIZE + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Util.threadFactory("http-response", false));

		// underlying queue used for storing submitted and running request
		queue = new HttpQueue();

		LogFull.i("http engine started");
	}

	/**
	 * Submits the request to respective executor with delay passed as parameter
	 * 
	 * @param request
	 * @param delay
	 */
	public void submit(RequestCall request, long delay)
	{
		if (delay <= 0)
		{
			submit(request);
		}
		else
		{
			submitWithDelay(request, delay);
		}
	}

	/**
	 * Adds the request to respective queue and also submits it to respective executer based on request type, if number of requests is less than that can be served by executer
	 * 
	 * @param request
	 */
	private synchronized void submit(RequestCall request)
	{
		queue.add(request);
		if (queue.spaceAvailable(request.getRequestType()))
		{
			submitNext(request.getRequestType());
		}
	}

	/**
	 * Submits a new request to executer which will execute after a given delay
	 * 
	 * @param request
	 * @param delay
	 *            Delay after which request should execute
	 * @return
	 */
	private void submitWithDelay(RequestCall request, long delay)
	{
		if (request.isCancelled())
		{
			LogFull.i("submitting with delay but request is cancelled");
			return;
		}

		Future<?> future;

		if (request.getRequestType() == Request.REQUEST_TYPE_LONG)
		{
			LogFull.d("scheduling on short executer " + request.getRequest().toString() + " on long executer after delay : " + delay);
			future = shortRequestExecuter.schedule(request, delay, TimeUnit.MILLISECONDS);
		}
		else
		{
			LogFull.d("scheduling on long executer  " + request.getRequest().toString() + " on long executer after delay : " + delay);
			future = longRequestExecuter.schedule(request, delay, TimeUnit.MILLISECONDS);
		}
		request.setFuture(future);
	}

	/**
	 * Submits a response call to response executer
	 * 
	 * @param response
	 * @return
	 */
	public void submit(ResponseCall response)
	{
		LogFull.d("submitting response to response executer");
		responseExecuter.submit(response);
	}

	/**
	 * Called from request executer in afterExecute method . Fetches and submits next task to the executer
	 * 
	 * @param executerType
	 */
	synchronized void fetchNextTask(short executerType)
	{
		LogFull.d("fetching next task for executer : " + executerType);
		submitNext(executerType);
	}

	/**
	 * increments short or long running tasks size Called from beforeExecute in executer
	 * 
	 * @param executer
	 */
	synchronized void incrementRunningTasksSize(short executer)
	{
		LogFull.d("Incrementing running task size for executer : " + executer);
		queue.incrementRunningTasksSize(executer);
	}

	/**
	 * decrements short or long running tasks size Called from afterExecute in executer
	 * 
	 * @param executer
	 */
	synchronized void decrementRunningTasksSize(short executer)
	{
		LogFull.d("Decrementing running task size for executer : " + executer);
		queue.decrementRunningTasksSize(executer);
	}

	/**
	 * polls the long or short queue for new tasks and submits it to the executer
	 * 
	 * @param executerType
	 */
	private void submitNext(int executerType)
	{
		Pair<RequestCall, Short> result = queue.getNextCall(executerType);

		if (result != null && result.first != null)
		{
			Future<?> future;
			RequestCall call = result.first;
			short executer = result.second;
			if (executer == LONG_EXECUTER)
			{
				LogFull.d("submitting next request to long executer " + call.getRequest().toString());
				future = longRequestExecuter.submit(call);
			}
			else
			{
				LogFull.d("submitting next request to short executer " + call.getRequest().toString());
				future = shortRequestExecuter.submit(call);
			}
			call.setFuture(future);
		}
		else
		{
			LogFull.i("nothing to submit to executers");
		}
	}

	public void solveStarvation(RequestCall call)
	{
		queue.solveStarvation(call);
	}

	/**
	 * shutdown the executer properly clearing all the references so that all the containing objects be available for garbage collection
	 */
	public void shutDown()
	{
		LogFull.d("shutdown started");
		shortRequestExecuter.shutdown();
		longRequestExecuter.shutdown();
		responseExecuter.shutdown();
		queue.shutdown();

		shortRequestExecuter = null;
		longRequestExecuter = null;
		responseExecuter = null;
		queue = null;
		LogFull.d("shutdown completed");
	}
}
