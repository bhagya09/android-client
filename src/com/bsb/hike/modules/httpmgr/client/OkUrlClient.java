package com.bsb.hike.modules.httpmgr.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

/**
 * Represents UrlClient wrapper with underlying pooling and caching done by OkHttp
 * 
 * @author anubhavgupta & sidharth
 * 
 */
public class OkUrlClient extends UrlConnectionClient
{
	private final OkUrlFactory okUrlFactory;

	public OkUrlClient()
	{
		this(ClientOptions.getDefaultClientOptions());
	}

	public OkUrlClient(ClientOptions clientOptions)
	{
		OkHttpClient okHttpCLient = OkClient.generateClient(clientOptions);
		this.okUrlFactory = new OkUrlFactory(okHttpCLient);
	}

	public OkUrlClient(OkHttpClient client)
	{
		this.okUrlFactory = new OkUrlFactory(client);
	}

	@Override
	protected HttpURLConnection openConnection(Request<?> request) throws IOException
	{
		return okUrlFactory.open(request.getUrl());
	}

	/**
	 * Clones the OkUrlClient with given client option parameters
	 */
	@Override
	public OkUrlClient clone(ClientOptions clientOptions)
	{
		return new OkUrlClient(OkClient.setClientParameters(okUrlFactory.client().clone(), clientOptions));
	}
}
