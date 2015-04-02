package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.response.ResponseFacade;

/**
 * Interface for response interceptors for doing work on the response obtained from server
 * 
 * @author sidharth
 * 
 */
public interface IResponseInterceptor
{
	/**
	 * Main work of interceptor is done in this method and when completed {@link Chain#proceed()} should be called for moving to next interceptor or getting response in request
	 * listener. If {@link Chain#proceed()} is not called then response will not notified and will be terminated from this interceptor only
	 * 
	 * @param chain
	 */
	void intercept(Chain chain);

	interface Chain
	{
		/**
		 * Returns the {@link ResponseFacade} object for making changes to the response
		 * 
		 * @return
		 */
		ResponseFacade getResponseFacade();

		/**
		 * Tells that work of this interceptor is completed and chain can proceed to next interceptor
		 */
		void proceed();
	}
}
