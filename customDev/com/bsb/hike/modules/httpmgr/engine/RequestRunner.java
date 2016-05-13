package com.bsb.hike.modules.httpmgr.engine;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkClientCustomDev;
import com.bsb.hike.modules.httpmgr.client.TwinPrimeOkClientCustomDev;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * This class clones the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
 *
 * @author sidharth & anubhav
 *
 */
public class RequestRunner extends com.bsb.hike.modules.httpmgr.engine.RequestRunnerBase
{
	public RequestRunner(ClientOptions options, HttpEngine engine, com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier requestListenerNotifier)
	{
		super(options, engine, requestListenerNotifier);
	}

	protected IClient getDefaultClient(ClientOptions options)
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TP_ENABLE,0)==1)
		{
			return new TwinPrimeOkClientCustomDev(options);
		}
		else
		{
			return new OkClientCustomDev(options);
		}
	}
}