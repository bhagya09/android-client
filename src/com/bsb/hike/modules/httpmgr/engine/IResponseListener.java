package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * This interface is used by {@link RequestRunner} to communicate with {@link RequestExecuter}
 * 
 * @author sidharth & anubhav
 * 
 */
public interface IResponseListener
{
	/**
	 * Gives Response and httpException if successful exception will be null and on failure response will be null
	 * 
	 * @param response
	 * @param ex
	 */
	public void onResponse(Response response, HttpException ex);
}
