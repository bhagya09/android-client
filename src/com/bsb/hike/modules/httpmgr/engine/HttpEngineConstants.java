package com.bsb.hike.modules.httpmgr.engine;

public class HttpEngineConstants
{
	/**
	 * From : {@link http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ThreadPoolExecutor.html}
	 * 
	 * <p>
	 * When a new task is submitted in method execute(java.lang.Runnable), and fewer than corePoolSize threads are running, a new thread is created to handle the request, even if
	 * other worker threads are idle. If there are more than corePoolSize but less than maximumPoolSize threads running, a new thread will be created only if the queue is full. By
	 * setting corePoolSize and maximumPoolSize the same, you create a fixed-size thread pool
	 * </p>
	 */
	static final short CORE_POOL_SIZE = 2;

	/** Each thread would wait for leep alive time after it had finished executing a task to see if there was more work to do */
	static final int KEEP_ALIVE_TIME = 2 * 60 * 1000; // 2 mins

	/**
	 * Sets the policy governing whether core threads may time out and terminate if no tasks arrive within the keep-alive time, being replaced if needed when new tasks arrive. When
	 * false, core threads are never terminated due to lack of incoming tasks. When true, the same keep-alive policy applying to non-core threads applies also to core threads.
	 */
	static final boolean ALLOW_CORE_THREAD_TIMEOUT = true;
}
