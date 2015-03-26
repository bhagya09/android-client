package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;


class RequestQueue
{
	BlockingQueue<Runnable> queue;
	
	private int DEFAULT_CAPACITY = 128;
	
	private ConcurrentHashMap<String, Request> stickerTaskMap;
	
	private TaskDispatcher dispatcher = null;
	
	public RequestQueue()
	{
		// TODO Auto-generated constructor stub
		queue = new PriorityBlockingQueue<Runnable>();
		stickerTaskMap = new ConcurrentHashMap<String, Request>();
		dispatcher = new TaskDispatcher(this);
		dispatcher.start();
	}
	
	public void addTask(String taskId, Request request)
	{
		stickerTaskMap.put(taskId, request);
		dispatcher.execute(request);
		
	}
	
	boolean isTaskAlreadyExist(String taskId)
	{
		if(stickerTaskMap.get(taskId) != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	void removeTask(String taskId)
	{
		stickerTaskMap.remove(taskId);
	}
	
	void shutdown()
	{
		stickerTaskMap.clear();
		dispatcher.shutdown();
	}
	
	public BlockingQueue<Runnable> getQueue()
	{
		return queue;
	}
}
