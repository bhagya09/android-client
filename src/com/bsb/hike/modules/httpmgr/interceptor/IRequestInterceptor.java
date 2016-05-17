package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;

/**
 * Interface for request interceptors for doing pre processing work on the request
 * 
 * @author sidharth
 * 
 */
public interface IRequestInterceptor
{
	/**
	 * Main work of interceptor is done in this method and when completed {@link Chain#proceed()} should be called for moving to next interceptor or request execution. If
	 * {@link Chain#proceed()} is not called then request will not executed and will be terminated from this interceptor only
	 * 
	 * @param chain
	 */
	void intercept(Chain chain) throws Exception;

	interface Chain
	{
		/**
		 * Returns the {@link RequestFacade} object for making changes to the request
		 * 
		 * @return
		 */
		RequestFacade getRequestFacade();

		/**
		 * Tells that work of this interceptor is completed and chain can proceed to next interceptor
		 */
		void proceed() throws Exception;
	}
}
