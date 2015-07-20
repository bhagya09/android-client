package com.bsb.hike.modules.httpmgr.request.requestbody;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

/**
 * This class uses {@link GZIPOutputStream} for gzipping the body
 * 
 * @author sidharth
 * 
 */
public class GzipRequestBody implements IRequestBody
{
	private IRequestBody originalBody;

	public GzipRequestBody(IRequestBody body)
	{
		originalBody = body;
	}

	@Override
	public String fileName()
	{
		return originalBody.fileName();
	}

	@Override
	public String mimeType()
	{
		return originalBody.mimeType();
	}

	@Override
	public long length()
	{
		return -1; // We don't know the compressed length in advance!
	}

	@Override
	public void writeTo(Request<?> request, OutputStream out) throws IOException
	{
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
		originalBody.writeTo(request, gzipOutputStream);
		gzipOutputStream.close();
	}

	@Override
	public RequestBody getRequestBody()
	{
		final RequestBody body = originalBody.getRequestBody();
		return new RequestBody()
		{
			@Override
			public MediaType contentType()
			{
				return body.contentType();
			}

			@Override
			public long contentLength()
			{
				return -1; // We don't know the compressed length in advance!
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException
			{
				BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
				body.writeTo(gzipSink);
				gzipSink.close();
			}
		};
	}
	
	public IRequestBody getOriginalBody()
	{
		return this.originalBody;
	}
}