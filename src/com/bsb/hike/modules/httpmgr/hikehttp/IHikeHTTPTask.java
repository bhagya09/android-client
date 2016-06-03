package com.bsb.hike.modules.httpmgr.hikehttp;

import android.os.Bundle;

/**
 * This interface is used by classes that have to execute a http task
 * 
 * @author sidharth
 * 
 */
public interface IHikeHTTPTask
{
	/**
	 * Executes the request
	 */
	public void execute();

	/**
	 * Cancels the request
	 */
	public void cancel();

    public Bundle getRequestBundle();

    public String getRequestId();
}