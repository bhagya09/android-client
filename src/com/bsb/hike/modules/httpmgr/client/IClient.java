package com.bsb.hike.modules.httpmgr.client;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * Abstraction of an HTTP client which can execute {@link Request Requests}. This class must be thread-safe as invocation may happen from multiple threads simultaneously.
 */
public interface IClient
{
	/**
	 * Synchronously execute an HTTP represented by {@code request} and encapsulate all response data into a {@link Response} instance.
	 * <p>
	 * Note: If the request has a body, its length and mime type will have already been added to the header list as {@code Content-Length} and {@code Content-Type}, respectively.
	 * Do NOT alter these values as they might have been set as a result of an application-level configuration.
	 */
	Response execute(Request<?> request) throws Throwable;

	/**
	 * Returns a shallow copy of this IClient
	 * 
	 * @param options
	 *            client options used while cloning
	 * @return
	 */
	IClient clone(ClientOptions options);
}
