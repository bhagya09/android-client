package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkUrlClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * This class clones the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
 *
 * @author sidharth & anubhav
 *
 */
public class RequestRunner
{
	private IClient defaultClient;

	private HttpEngine engine;

	private com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier;

	public RequestRunner(ClientOptions options, HttpEngine engine, com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier)
	{
		defaultClient = new OkUrlClient(options);
		this.engine = engine;
		this.requestListenerNotifier = requestListenerNotifier;
	}

	/**
	 * Clones the {@link IClient} object if parameter <code>options</code> is not null and then passes this client to the {@link RequestExecuter} for final execution of the request
	 *
	 * @param request
	 * @param options
	 */
	public void submit(final Request<?> request, ClientOptions options)
	{
		IClient client = (null != options) ? defaultClient.clone(options) : defaultClient;

		com.bsb.hike.modules.httpmgr.engine.RequestExecuter requestExecuter = new com.bsb.hike.modules.httpmgr.engine.RequestExecuter(client, engine, request, new com.bsb.hike.modules.httpmgr.engine.IResponseListener()
		{
			@Override
			public void onResponse(Response response, HttpException ex)
			{
				if (null == response)
				{
					requestListenerNotifier.notifyListenersOfRequestFailure(request, ex);
				}
				else
				{
					requestListenerNotifier.notifyListenersOfRequestSuccess(request, response);
				}
			}
		});
		requestExecuter.execute();
	}

	/**
	 * Shutdown method to close everything (setting all variables to null for easy garbage collection)
	 */
	public void shutdown()
	{
		engine.shutDown();
		engine = null;
		defaultClient = null;
		requestListenerNotifier.shutdown();
	}
}
