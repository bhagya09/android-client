package com.bsb.hike.modules.signupmgr;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by sidharth on 11/03/16.
 */
class SignUpHttpRetryPolicy extends BasicRetryPolicy
{
	public static final int MAX_RETRY_COUNT = 3;

	public SignUpHttpRetryPolicy(int retryCount, int retryDelay, float backOffMultiplier)
	{
		super(retryCount, retryDelay, backOffMultiplier);
	}

	@Override
	protected void handleDefaultErrorCase(RequestFacade requestFacade, HttpException ex) {
		super.handleDefaultErrorCase(requestFacade, ex);
		protocolFallback(requestFacade);
	}

	private void protocolFallback(RequestFacade requestFacade)
	{
		URL url = requestFacade.getUrl();
		try
		{
			String protocol = url.getProtocol();
			if (getRetryIndex() % 2 != 0)
			{
                String newProtocol = "http".equals(protocol) ? "https" : "http";
                requestFacade.setUrl(new URL(newProtocol, url.getHost(), url.getPort(), url.getFile()));
			}
		}
		catch (MalformedURLException e)
		{
			LogFull.e("exception while setting url in case of unknown host exception", e);
		}
	}
}
