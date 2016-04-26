package com.bsb.hike.modules.httpmgr.client;

import com.bsb.hike.StethoInterceptor;
import com.squareup.okhttp.OkHttpClient;

public class OkClientCustomDev extends OkClient
{
	public OkClientCustomDev()
	{
		super();
	}

	public OkClientCustomDev(com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		super(clientOptions);
	}

	public OkClientCustomDev(OkHttpClient client)
	{
		super(client);
	}

	/**
	 * Generates a new OkHttpClient with given clientOption parameters
	 *
	 * @param clientOptions
	 * @return
	 */
	protected OkHttpClient generateClient(com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		OkHttpClient client = super.generateClient(clientOptions);
		client.networkInterceptors().add(new StethoInterceptor());
		return client;
	}
}