package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.utils.Logger;

class Request extends FutureTask<STResult> implements Comparable<Request>
{
	public static final int PRIORITY_HIGHEST = 0;
	public static final int PRIORITY_HIGH = 25;
    public static final int PRIORITY_NORMAL = 50;
    public static final int PRIORITY_LOW = 100;
    
	private BaseStickerDownloadTask task;
	private int priority = PRIORITY_NORMAL;

	public Request(BaseStickerDownloadTask callable)
	{
		super(callable);
		this.task = callable;
	}

	private BaseStickerDownloadTask getTask()
	{
		return task;
	}
	
	void setPrioity(int priority)
	{
		if (priority < 0) {
            throw new IllegalArgumentException("Priority must be positive.");
        }
		this.priority = priority;
	}
	
	int getPriority()
	{
		return this.priority;
	}

	@Override
	public void run()
	{
		Logger.d(getClass().getSimpleName(), "TimeCheck: Starting time : " + System.currentTimeMillis());
		super.run();
	}

	@Override
	protected void done()
	{
		super.done();
		STResult result = STResult.DOWNLOAD_FAILED;
		try
		{
			result = this.get();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (ExecutionException e)
		{
			e.printStackTrace();
		}

		task.postExecute(result);

		Logger.d(getClass().getSimpleName(), "TimeCheck: Exiting  time : " + System.currentTimeMillis());
	}

	@Override
	public int compareTo(Request another)
	{
		if(this == another)
		{
			return 0;
		}
		return this.getPriority() - another.getPriority();
	}
}
