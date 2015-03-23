package com.bsb.hike.modules.httpmgr.response;

/**
 * Abstract class that implements {@link Runnable}. This class object is submitted to the response executor.
 * 
 * @author sidharth
 * 
 */
public abstract class ResponseCall implements Runnable
{
	@Override
	public void run()
	{
		execute();
	}

	public abstract void execute();
}
