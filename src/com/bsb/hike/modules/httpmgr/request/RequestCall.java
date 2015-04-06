package com.bsb.hike.modules.httpmgr.request;

import java.util.concurrent.Future;

/**
 * Abstract class that implements {@link Runnable} and consists of request object. This class object is submitted to the executors
 * 
 * @author sidharth
 * 
 */
public abstract class RequestCall implements Runnable, Comparable<RequestCall>
{
	private Request<?> request;

	private long submissionTime;

	public RequestCall(Request<?> request)
	{
		this.request = request;
		this.submissionTime = System.currentTimeMillis();
	}

	@Override
	public void run()
	{
		execute();
	}

	public abstract void execute();

	public int getPriority()
	{
		return request.getPriority();
	}

	public int getRequestType()
	{
		return request.getRequestType();
	}

	public long getSubmissionTime()
	{
		return submissionTime;
	}

	public boolean isCancelled()
	{
		return request.isCancelled();
	}

	public void setFuture(Future<?> future)
	{
		request.setFuture(future);
	}

	public void setPriority(int priority)
	{
		request.setPriority(priority);
	}

	public Request<?> getRequest()
	{
		return request;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getPriority();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		RequestCall other = (RequestCall) obj;
		if (this.getPriority() != other.getPriority())
		{
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(RequestCall another)
	{
		if (this == another)
		{
			return 0;
		}
		return this.getPriority() - another.getPriority();
	}

}
