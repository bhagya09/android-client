package com.bsb.hike.modules.httpmgr.request.requestbody;

import java.io.IOException;
import java.io.OutputStream;

import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.RequestBody;

public class MultipartRequestBody implements IRequestBody
{
	private RequestBody mRequestBody;

	public MultipartRequestBody(RequestBody requestBody)
	{
		mRequestBody = requestBody;
	}

	@Override
	public String fileName()
	{
		return null;
	}

	@Override
	public String mimeType()
	{
		return mRequestBody.contentType().toString();
	}

	@Override
	public long length()
	{
		try
		{
			return mRequestBody.contentLength();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return -1L;
		}
	}

	@Override
	public void writeTo(Request<?> request, OutputStream out) throws IOException
	{
		Sink sink = Okio.sink(out);
		BufferedSink bufferedRequestBody = Okio.buffer(sink);
		mRequestBody.writeTo(bufferedRequestBody);
		bufferedRequestBody.flush();
		sink.flush();
		out.flush();
	}

	@Override
	public RequestBody getRequestBody()
	{
		return mRequestBody;
	}

}
