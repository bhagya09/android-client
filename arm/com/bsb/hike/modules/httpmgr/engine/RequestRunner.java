package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkClient;
import com.bsb.hike.modules.httpmgr.client.TwinPrimeOkClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.hike.transporter.utils.Logger;

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
		defaultClient = getClientBasedOnServerValue(options);
		this.engine = engine;
		this.requestListenerNotifier = requestListenerNotifier;
	}

	/**
	 *
	 * @param options
	 * @return ICLIENT //1-->TWIN PRIME CLIENT,0-->OKCLIENT
	 */
	private IClient getClientBasedOnServerValue(ClientOptions options)
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TP_ENABLE,0)==1)
		{
			return new TwinPrimeOkClient(options);
		}
		else
		{
			return new OkClient(options);
		}
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
