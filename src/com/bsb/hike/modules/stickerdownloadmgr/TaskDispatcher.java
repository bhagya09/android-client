package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

class TaskDispatcher
{
	ThreadPoolExecutor pool;
	
	RequestQueue queue;
	
	private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

	private final int CORE_POOL_SIZE = 2;

	private final int MAXIMUM_POOL_SIZE = 3;

	private final short KEEP_ALIVE_TIME = 5 * 60; // in seconds
	
	private class MyThreadFactory implements ThreadFactory
	{
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r)
		{
			int threadCount = threadNumber.getAndIncrement();
			Thread t = new Thread(r);
			// This approach reduces resource competition between the Runnable object's thread and the UI thread.
			t.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
			t.setName("Sticker Thread-" + threadCount);
			Logger.d(getClass().getSimpleName(), "Running SDM thread : " + t.getName());
			return t;
		}
	}
	
	private static RejectedExecutionHandler executionHandler = new RejectedExecutionHandler()
	{
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
		{
		}
	};
	
	TaskDispatcher(RequestQueue queue)
	{
		// TODO Auto-generated constructor stub
		this.queue = queue;
	}
	
	void start()
	{
		pool = new ThreadPoolExecutor(2, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, queue.getQueue(), new MyThreadFactory(), executionHandler);
		
		if(Utils.isGingerbreadOrHigher())
		{
			pool.allowCoreThreadTimeOut(true);
		}
			
	}
	
	void execute(Request request)
	{
		pool.execute(request);
	}
	
	void shutdown()
	{
		pool.shutdown();
	}
}
