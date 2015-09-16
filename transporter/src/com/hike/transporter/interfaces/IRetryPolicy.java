package com.hike.transporter.interfaces;

/**
 * 
 * @author himanshu This is the retry policy that is set on the client for retry incase of ConnectException
 */
public interface IRetryPolicy
{
	public int getRetryCount();

	public int getRetryDelay();

	public void consumeRetry();
}