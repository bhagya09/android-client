package com.bsb.hike.modules.stickersearch;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.bsb.hike.modules.httpmgr.engine.MainThreadExecutor;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StickerSearchEngine
{
	private BlockingQueue<Runnable> searchQueue;

	private ScheduledThreadPoolExecutor backgroundSearchExecuter;

	private ThreadPoolExecutor backgroundQueryExecuter;

	private MainThreadExecutor uiExecutor;

	private final int CORE_POOL_SIZE = 1;

	private final int MAX_POOL_SIZE = 1;

	private final int KEEP_ALIVE_TIME = 30;

	public StickerSearchEngine()
	{

		uiExecutor = new MainThreadExecutor();

		backgroundSearchExecuter = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, Utils.threadFactory("sticker_search_thread", false),
				Utils.rejectedExecutionHandler());

		searchQueue = backgroundSearchExecuter.getQueue();
		backgroundSearchExecuter.setKeepAliveTime(30, TimeUnit.SECONDS);

		backgroundQueryExecuter = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				Utils.threadFactory("sticker_query_thread", false),
				Utils.rejectedExecutionHandler());


		backgroundSearchExecuter.allowCoreThreadTimeOut(true);
		backgroundQueryExecuter.allowCoreThreadTimeOut(true);
	}

	public void runOnSearchThread(Runnable task, long delay)
	{
		if (null == task)
		{
			return;
		}
		backgroundSearchExecuter.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

	public void runOnQueryThread(Runnable task)
	{
		if (null == task)
		{
			return;
		}
		backgroundQueryExecuter.execute(task);
	}

	public void runOnUiThread(Runnable task, long delayMillis)
	{
		if (null == task)
		{
			return;
		}
		if(delayMillis == 0)
		{
			uiExecutor.execute(task);
		}
		else
		{
			uiExecutor.executeDelayed(task, delayMillis);
		}
	}
	
	public void shutDown()
	{
		backgroundSearchExecuter.shutdown();
		backgroundQueryExecuter.shutdown();
		searchQueue.clear();
		
		uiExecutor = null;
		backgroundSearchExecuter = null;
		backgroundQueryExecuter = null;
		searchQueue = null;
	}
}
