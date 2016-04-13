package com.bsb.hike.modules.httpmgr.client;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;


/**
 *
 * Represents the OkHttpClient wrapper
 *
 * @author anubhavgupta & sidharth
 *
 */
public class OkClient implements com.bsb.hike.modules.httpmgr.client.IClient
{
	private OkHttpClient client;

	/**
	 * These constants are used internally by okHttp for connection pooling
	 */
	private static final boolean keepAlive = true;

	private static final long keepAliveDuration = 5 * 60 * 1000; // 5 min

	private static final int numOfMaxConnections = 5;

	static
	{
		/**
		 * Don't change these keys as they are used internally by okHttp for connection pooling
		 */
		System.setProperty("http.keepAlive", Boolean.toString(keepAlive));
		System.setProperty("http.keepAliveDuration", Long.toString(keepAliveDuration));
		System.setProperty("http.maxConnections", Integer.toString(numOfMaxConnections));
	}

	/**
	 * Generates a new OkHttpClient with given clientOption parameters
	 *
	 * @param clientOptions
	 * @return
	 */
	static OkHttpClient generateClient(
			com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		clientOptions = clientOptions != null ? clientOptions : com.bsb.hike.modules.httpmgr.client.ClientOptions
				.getDefaultClientOptions();
		OkHttpClient client = new OkHttpClient();
		return setClientParameters(client, clientOptions);
	}

	/**
	 * Sets Client option parameters to given OkHttpClient
	 *
	 * @param client
	 * @param clientOptions
	 * @return
	 */
	static OkHttpClient setClientParameters(OkHttpClient client, com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		client.setConnectTimeout(clientOptions.getConnectTimeout(), TimeUnit.MILLISECONDS);
		client.setReadTimeout(clientOptions.getReadTimeout(), TimeUnit.MILLISECONDS);
		client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
		client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
		client.setSocketFactory(client.getSocketFactory());
		client.setSslSocketFactory(clientOptions.getSslSocketFactory());
		client.setHostnameVerifier(clientOptions.getHostnameVerifier());

		if (clientOptions.getProxy() != null)
		{
			client.setProxy(clientOptions.getProxy());
		}

		if (clientOptions.getProxySelector() != null)
		{
			client.setProxySelector(clientOptions.getProxySelector());
		}

		if (clientOptions.getProxySelector() != null)
		{
			client.setProxySelector(clientOptions.getProxySelector());
		}

		if (clientOptions.getCookieHandler() != null)
		{
			client.setCookieHandler(clientOptions.getCookieHandler());
		}

		if (clientOptions.getCache() != null)
		{
			client.setCache(clientOptions.getCache());
		}

		if (clientOptions.getCache() != null)
		{
			client.setCache(clientOptions.getCache());
		}

		if (clientOptions.getHostnameVerifier() != null)
		{
			client.setHostnameVerifier(clientOptions.getHostnameVerifier());
		}

		if (clientOptions.getCertificatePinner() != null)
		{
			client.setCertificatePinner(clientOptions.getCertificatePinner());
		}

		if (clientOptions.getAuthenticator() != null)
		{
			client.setAuthenticator(clientOptions.getAuthenticator());
		}

		if (clientOptions.getProtocols() != null)
		{
			client.setProtocols(clientOptions.getProtocols());
		}

		if (clientOptions.getConnectionSpecs() != null)
		{
			client.setConnectionSpecs(clientOptions.getConnectionSpecs());
		}

		if (clientOptions.getDispatcher() != null)
		{
			client.setDispatcher(clientOptions.getDispatcher());
		}

		if (clientOptions.getConnectionPool() != null)
		{
			client.setConnectionPool(clientOptions.getConnectionPool());
		}

		client.setFollowSslRedirects(clientOptions.getFollowSslRedirects());
		client.setFollowRedirects(clientOptions.getFollowRedirects());

		return client;
	}

	public OkClient()
	{
		client = generateClient(com.bsb.hike.modules.httpmgr.client.ClientOptions.getDefaultClientOptions());
	}

	public OkClient(com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		client = generateClient(clientOptions);
	}

	public OkClient(OkHttpClient client)
	{
		this.client = client;
	}

	@Override
	public Response execute(Request<?> request) throws Throwable
	{
		com.squareup.okhttp.Request httpRequest = makeOkRequest(request);
		Call call = client.newCall(httpRequest);
		com.squareup.okhttp.Response response = call.execute();
		return parseOkResponse(request, response);

	}

	/**
	 * Parse hike http request to OkHttpRequest
	 *
	 * @param request
	 * @return
	 */
	private com.squareup.okhttp.Request makeOkRequest(Request<?> request)
	{
		com.squareup.okhttp.Request.Builder httpRequestBuilder = new com.squareup.okhttp.Request.Builder();

		httpRequestBuilder.url(request.getUrl());
		for (Header header : request.getHeaders())
		{
			httpRequestBuilder.addHeader(header.getName(), header.getValue());
		}
		IRequestBody body = request.getBody();
		httpRequestBuilder.method(request.getMethod(), null == body ? null : body.getRequestBody());
		com.squareup.okhttp.Request httpRequest = httpRequestBuilder.build();
		return httpRequest;
	}

	/**
	 * Parse OkhttpResponse to hike http response
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Throwable
	 */
	private <T> Response parseOkResponse(Request<T> request, com.squareup.okhttp.Response response) throws Throwable
	{
		Response.Builder responseBuilder = new Response.Builder();
		responseBuilder.setUrl(response.request().urlString());
		responseBuilder.setStatusCode(response.code());
		responseBuilder.setReason(response.message());
		com.squareup.okhttp.ResponseBody responseBody = response.body();

		InputStream stream = responseBody.byteStream();
		try
		{
			int contentLength = (int) responseBody.contentLength();
			T bodyContent = request.parseResponse(stream, contentLength);
			ResponseBody<T> body = ResponseBody.create(responseBody.toString(), contentLength, bodyContent);
			responseBuilder.setBody(body);
			Response res = responseBuilder.build();
			res.getResponseInterceptors().addAll(request.getResponseInterceptors());
			return res;
		}
		finally
		{
			Utils.closeStreams(stream);
		}
	}

	/**
	 * Clones okClient with given client option parameters
	 *
	 * @param clientOptions
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public OkClient clone(com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
	{
		return new OkClient(setClientParameters(client.clone(), clientOptions));
	}

}
