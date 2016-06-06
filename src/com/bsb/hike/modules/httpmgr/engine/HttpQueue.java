package com.bsb.hike.modules.httpmgr.engine;

import android.util.Pair;

import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bsb.hike.modules.httpmgr.engine.HttpEngineConstants.CORE_POOL_SIZE;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

/**
 * This class is used to maintain to ordering of the requests in two queues one is short queue for short running requests and other is long queue for long running requests
 * 
 * @author anubhav
 * 
 */
public class HttpQueue
{
	private PriorityBlockingQueue<RequestCall> shortQueue;

	private PriorityBlockingQueue<RequestCall> longQueue;

	private volatile AtomicInteger numShortRunningCalls = new AtomicInteger(0);

	private volatile AtomicInteger numLongRunningCalls = new AtomicInteger(0);

	private short MAX_QUEUE_SIZE = CORE_POOL_SIZE;

	HttpQueue()
	{
		// Priority Queue used for storing short requests submitted to the engine
		shortQueue = new PriorityBlockingQueue<>();

		// Priority Queue used for storing long requests submitted to engine
		longQueue = new PriorityBlockingQueue<>();
	}

	/**
	 * add the request to short or long queues
	 * 
	 * @param request
	 */
	void add(RequestCall request)
	{
		if (request.getRequestType() == REQUEST_TYPE_LONG)
		{
			LogFull.d(request.getRequest().toString() + " is added to long queue");
			longQueue.add(request);
		}
		else
		{
			LogFull.d(request.getRequest().toString() + " is added to short queue");
			shortQueue.add(request);
		}
	}

	/**
	 * increments the number of running calls
	 * 
	 * @param executer
	 *            executerType
	 */
	void incrementRunningTasksSize(short executer)
	{
		if (executer == HttpEngine.LONG_EXECUTER)
		{
			numLongRunningCalls.incrementAndGet();
			LogFull.d("incrementing number of long running calls, current number of long running calls  : " + numLongRunningCalls.get());
		}
		else
		{
			numShortRunningCalls.incrementAndGet();
			LogFull.d("incrementing number of short running calls, current number of short running calls  : " + numShortRunningCalls.get());
		}
	}

	/**
	 * decrements the number of running calls
	 * 
	 * @param executer
	 *            executerType
	 */
	void decrementRunningTasksSize(short executer)
	{
		if (executer == HttpEngine.LONG_EXECUTER)
		{
			numLongRunningCalls.decrementAndGet();
			LogFull.d("decrementing number of long running calls, current number of long running calls  : " + numLongRunningCalls.get());
		}
		else
		{
			numShortRunningCalls.decrementAndGet();
			LogFull.d("decrementing number of short running calls, current number of long running calls  : " + numShortRunningCalls.get());
		}
	}

	/**
	 * <p>
	 * Gets the next call for httpEngine
	 * </p>
	 * 
	 * <p>
	 * It works on principle that short tasks can be executed on both short and long executers if there is space Long tasks can be executed only on long executer
	 * </p>
	 * 
	 * @param executerType
	 * @return pair of next call and type of executer on which request will be executed
	 */
	synchronized Pair<RequestCall, Short> getNextCall(int executerType)
	{
		RequestCall call = null;
		RequestCall nextCall = null;
		short executer = HttpEngine.LONG_EXECUTER;

		while (null == nextCall)
		{

			if (executerType == HttpEngine.SHORT_EXECUTER)
			{
				// short queue has some tasks, fetch and return it to engine to be executed on short executer
				call = shortQueue.poll();
				if (call != null)
				{
					if (!call.isCancelled())
					{
						nextCall = call;
						executer = HttpEngine.SHORT_EXECUTER;
					}
					LogFull.d("next call for short executer from short queue : " + call.getRequest().toString());

				}
				else
				{
					// no tasks to execute
					LogFull.i("no tasks to execute");
					break;
				}
			}
			else
			{
				// long queue has some tasks, fetch and return it to engine to be executed on long executer
				call = longQueue.poll();
				if (call != null)
				{
					if (!call.isCancelled())
					{
						nextCall = call;
						executer = HttpEngine.LONG_EXECUTER;
						LogFull.d("next call for long executer from long queue : " + call.getRequest().toString());
					}
				}
				else
				{
					// long queue is empty. poll short queue
					// short queue has some tasks, fetch and return it to engine to be executed on long executer
					call = shortQueue.poll();
					if (call != null)
					{
						if (!call.isCancelled())
						{
							nextCall = call;
							executer = HttpEngine.LONG_EXECUTER;
							LogFull.d("next call from long executer from short queue : " + call.getRequest().toString());
						}
					}
					else
					{
						// no tasks to execute
						LogFull.i("no tasks to execute");
						break;
					}
				}
			}
		}
		return new Pair<RequestCall, Short>(nextCall, executer);
	}

	/**
	 * Checks if there is space available in long or short executer by checking size of long or short running queues
	 * 
	 * @param requestType
	 *            type of request (long or short)
	 * @return
	 */
	boolean spaceAvailable(int requestType)
	{
		int longRunningSize = numLongRunningCalls.get();
		int shortRunningSize = numShortRunningCalls.get();

		LogFull.d("long runng size : " + longRunningSize + " short running size : " + shortRunningSize);
		if (requestType == Request.REQUEST_TYPE_LONG)
		{
			if (longRunningSize < MAX_QUEUE_SIZE)
			{
				LogFull.d("space available");
				return true;
			}
		}
		else
		{
			if (shortRunningSize < MAX_QUEUE_SIZE)
			{
				LogFull.d("space available");
				return true;
			}
		}
		LogFull.i("space not available");
		return false;
	}

	/**
	 * clear queue and remove references
	 */
	void shutdown()
	{
		LogFull.d("shutdown started");
		longQueue.clear();
		shortQueue.clear();
		longQueue = null;
		shortQueue = null;
		numLongRunningCalls = null;
		numShortRunningCalls = null;
		LogFull.d("shutdown completed");
	}

	public void solveStarvation(RequestCall call)
	{
		if (call.getRequestType() == REQUEST_TYPE_SHORT)
		{
			changePriority(shortQueue, call);
		}
		else
		{
			changePriority(longQueue, call);
		}
	}

	private void changePriority(PriorityBlockingQueue<RequestCall> queue, RequestCall call)
	{
		List<RequestCall> requestCallsList = new ArrayList<RequestCall>();

		int size = queue.size();
		for (int index = size - 1; index >= 0; --index)
		{
			RequestCall requestCall = queue.peek();

			if (requestCall == null)
			{
				break;
			}

			if (requestCall.getSubmissionTime() < call.getSubmissionTime() && requestCall.getPriority() > call.getPriority())
			{
				queue.remove();
				requestCall.setPriority(requestCall.getPriority() - 1);
				requestCallsList.add(requestCall);
			}
		}
		queue.addAll(requestCallsList);
	}

}
