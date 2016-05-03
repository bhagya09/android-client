package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;

/**
 * This interceptor compresses the HTTP request body *
 * 
 * @author anubhav
 * 
 */
public final class GzipRequestInterceptor implements IRequestInterceptor
{
	/** Threshold value of the length of data below which it can't be compressed as it will lead to greater length than original (some gzip headers are added durign compression) */
	private static final int THRESHOLD_LENGTH_FOR_GZIP = 250;

	@Override
	public void intercept(Chain chain) throws Exception
	{
		RequestFacade requestFacade = chain.getRequestFacade();

		if (requestFacade.getBody() == null || HttpUtils.containsHeader(requestFacade.getHeaders(), "Content-Encoding"))
		{
			chain.proceed();
		}

		if (requestFacade.getBody().length() >= THRESHOLD_LENGTH_FOR_GZIP)
		{
			requestFacade.getHeaders().add(new Header("Content-Encoding", "gzip"));
			requestFacade.setBody(new GzipRequestBody(requestFacade.getBody()));
		}
		chain.proceed();
	}
}