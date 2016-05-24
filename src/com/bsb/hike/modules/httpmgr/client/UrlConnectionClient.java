package com.bsb.hike.modules.httpmgr.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.bsb.hike.utils.Utils;

/**
 * Represents UrlConnection client wrapper
 * 
 * @author anubhavgupta @ sidharth
 * 
 */
public class UrlConnectionClient implements IClient
{
	private static final int CHUNK_SIZE = 4096;

	public UrlConnectionClient()
	{
	}

	@Override
	public Response execute(Request<?> request) throws Throwable
	{
		HttpURLConnection connection = openConnection(request);
		prepareRequest(connection, request);
		return readResponse(request, connection);
	}

	protected HttpURLConnection openConnection(Request<?> request) throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) request.getUrl().openConnection();
		connection.setConnectTimeout(Defaults.CONNECT_TIMEOUT_MILLIS);
		connection.setReadTimeout(Defaults.READ_TIMEOUT_MILLIS);
		return connection;
	}

	/**
	 * Creates a HttpUrlConnection request from hike http request
	 * 
	 * @param connection
	 * @param request
	 * @throws IOException
	 */
	void prepareRequest(HttpURLConnection connection, Request<?> request) throws IOException
	{
		connection.setRequestMethod(request.getMethod());
		connection.setDoInput(true);

		for (Header header : request.getHeaders())
		{
			connection.addRequestProperty(header.getName(), header.getValue());
		}

		IRequestBody body = request.getBody();
		if (body != null)
		{
			connection.setDoOutput(true);
			connection.addRequestProperty("Content-Type", body.mimeType());
			long length = body.length();
			if (length != -1)
			{
				connection.setFixedLengthStreamingMode((int) length);
				connection.addRequestProperty("Content-Length", String.valueOf(length));
			}
			else
			{
				connection.setChunkedStreamingMode(CHUNK_SIZE);
			}

			body.writeTo(request, connection.getOutputStream());
		}
	}

	/**
	 * Parses the HttpUrlConnection response to hike htto response
	 * 
	 * @param request
	 * @param connection
	 * @return
	 * @throws Throwable
	 */
	<T> Response readResponse(Request<T> request, HttpURLConnection connection) throws IOException
	{
		int status = connection.getResponseCode();
		String reason = connection.getResponseMessage();
		if (reason == null)
		{
			reason = ""; // HttpURLConnection treats empty reason as null.
		}

		List<Header> headers = new ArrayList<Header>();
		for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet())
		{
			String name = field.getKey();
			for (String value : field.getValue())
			{
				headers.add(new Header(name, value));
			}
		}

		String mimeType = connection.getContentType();
		int length = connection.getContentLength();
		InputStream stream = null;
		try
		{
			T bodyContent = null;
			ResponseBody<T> responseBody = null;
			if (status >= 200 && status <= 299)
			{
				stream = connection.getInputStream();
				bodyContent = request.parseResponse(stream, length);
				responseBody = ResponseBody.create(mimeType, length, bodyContent);
			}
			else
			{
				stream = connection.getErrorStream();
				byte[] bytes = HttpUtils.streamToBytes(stream);
				String errorString = new String(bytes);
				responseBody = ResponseBody.createErrorResponse(mimeType, length, errorString);
			}

			Response response = new Response.Builder().setUrl(connection.getURL().toString()).setStatusCode(status).setReason(reason).setHeaders(headers).setBody(responseBody)
					.build();
			response.getResponseInterceptors().addAll(request.getResponseInterceptors());
			return response;
		}
		finally
		{
			Utils.closeStreams(stream);
		}
	}

	@Override
	public IClient clone(ClientOptions options)
	{
		throw new IllegalStateException("can't clone UrlCOnnectionClient");
	}
}
