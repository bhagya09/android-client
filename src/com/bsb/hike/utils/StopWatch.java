package com.bsb.hike.utils;

/**
 * Utility class to measure time taken in methods.
 * Removes the headache of manually keeping time variables
 * Created by piyush on 13/12/15.
 */
public class StopWatch
{
	private long time = -1l;

	public void start()
	{
		time = System.currentTimeMillis();
	}

	public void stop()
	{
		time = (System.currentTimeMillis() - time);
	}

	public void reset()
	{
		time = -1l;
	}

	public long getElapsedTime()
	{
		return time;
	}

}
