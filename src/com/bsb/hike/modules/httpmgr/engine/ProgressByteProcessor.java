package com.bsb.hike.modules.httpmgr.engine;

import java.io.IOException;
import java.io.OutputStream;

import com.bsb.hike.modules.httpmgr.request.Request;

public class ProgressByteProcessor
{
	private final OutputStream os;

	private long progress;

	private final long total;

	private Request<?> request;

	public ProgressByteProcessor(Request<?> request, final OutputStream os, final long total)
	{
		this.os = os;
		this.total = total;
		this.request = request;
	}

	public void processBytes(final byte[] buffer, final int offset, final int length) throws IOException
	{
		os.write(buffer, offset, length);
		progress += length - offset;
		request.publishProgress((float) progress / total);
	}
}