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
		if (time < 0l)
		{
			throw new IllegalStateException("StopWatch was not started");
		}
		time = (System.currentTimeMillis() - time);
	}

	public void reset()
	{
		time = -1l;
	}

	public long getElapsedTime()
	{
		if (time < 0l)
		{
			throw new IllegalStateException(
					"StopWatch was not started! Cannot determine elapsed time");
		}
		return time;
	}

}
