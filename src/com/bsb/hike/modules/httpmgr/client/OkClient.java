package com.bsb.hike.modules.httpmgr.client;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * Represents the OkHttpClient wrapper
 *
 * @author anubhavgupta & sidharth
 *
 */
public class OkClient implements IClient
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

	protected OkHttpClient generateClient(ClientOptions options)
	{
		return new OkHttpClientFactory().generateClient(options);
	}

	public OkClient()
	{
		client = generateClient(ClientOptions.getDefaultClientOptions());
	}

	public OkClient(ClientOptions clientOptions)
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
		Headers responseHeaders = response.headers();
		if (responseHeaders != null)
		{
			int size = responseHeaders.size();
			List<Header> headersList = new ArrayList<>(size);
			for (int i = 0; i < size; ++i)
			{
				Header header = new Header(responseHeaders.name(i), responseHeaders.value(i));
				headersList.add(header);
			}
			responseBuilder.setHeaders(headersList);
		}

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
	public OkClient clone(ClientOptions clientOptions)
	{
		return new OkClient(new OkHttpClientFactory().setClientParameters(client.clone(), clientOptions));
	}

}
