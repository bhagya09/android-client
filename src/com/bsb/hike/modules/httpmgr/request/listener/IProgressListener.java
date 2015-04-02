package com.bsb.hike.modules.httpmgr.request.listener;

/**
 * This listener is used by internal classes to communicate the requets progress to the outside world
 * 
 * @author sidharth
 * 
 */
public interface IProgressListener
{
	/**
	 * Request progress in percentage is sent
	 * 
	 * @param progress
	 */
	void onProgressUpdate(float progress);
}
