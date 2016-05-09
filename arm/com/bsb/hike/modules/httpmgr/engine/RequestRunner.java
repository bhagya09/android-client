package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkClient;

/**
 * This class clones the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
 *
 * @author sidharth & anubhav
 *
 */
public class RequestRunner extends RequestRunnerBase
{
	public RequestRunner(ClientOptions options, HttpEngine engine, com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier)
	{
		super(options, engine, requestListenerNotifier);
	}

	protected IClient getDefaultClient(ClientOptions options)
	{
		return new OkClient(options);
	}
}