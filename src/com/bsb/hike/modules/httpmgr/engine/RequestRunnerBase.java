package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.gcmnetworkmanager.Config;
import com.bsb.hike.modules.gcmnetworkmanager.HikeGcmNetworkMgr;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * This class clones the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
 *
 * @author sidharth & anubhav
 *
 */
public abstract class RequestRunnerBase
{
	private IClient defaultClient;

	private HttpEngine engine;

	private com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier;

	public RequestRunnerBase(ClientOptions options, HttpEngine engine, com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier)
	{
		defaultClient = getDefaultClient(options);
		this.engine = engine;
		this.requestListenerNotifier = requestListenerNotifier;
	}

	protected abstract IClient getDefaultClient(ClientOptions options);

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
						final Config gcmTaskConfig = request.getGcmTaskConfig();
						if (null == response)
						{
							requestListenerNotifier.notifyListenersOfRequestFailure(request, ex);
							if (gcmTaskConfig != null && gcmTaskConfig.getNumRetries() > 0)
							{
								gcmTaskConfig.decrementRetries();
								updateGcmTaskConfigInDB(gcmTaskConfig);
								HikeGcmNetworkMgr.getInstance().schedule(gcmTaskConfig);
							}
							else
							{
								removeGcmTaskConfigFromDB(gcmTaskConfig);
							}
						}
						else
						{
							requestListenerNotifier.notifyListenersOfRequestSuccess(request, response);
							removeGcmTaskConfigFromDB(gcmTaskConfig);
						}
					}
		});
		requestExecuter.execute();
	}

	private void removeGcmTaskConfigFromDB(final Config gcmTaskConfig)
	{
		HikeHandlerUtil.getInstance().postAtFront(new Runnable()
		{
			@Override
			public void run()
			{
				if (gcmTaskConfig != null)
				{
					HttpRequestStateDB.getInstance().deleteBundleForTag(gcmTaskConfig.getTag());
				}
			}
		});
	}

	private void updateGcmTaskConfigInDB(final Config gcmTaskConfig)
	{
		HikeHandlerUtil.getInstance().postAtFront(new Runnable()
		{
			@Override
			public void run()
			{
				if (gcmTaskConfig != null)
				{
					HttpRequestStateDB.getInstance().update(gcmTaskConfig.getTag(), gcmTaskConfig.toBundle());
				}
			}
		});
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
