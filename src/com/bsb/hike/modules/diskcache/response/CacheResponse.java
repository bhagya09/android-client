package com.bsb.hike.modules.diskcache.response;

import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSource;

public class CacheResponse
{
	private final String TAG = CacheResponse.class.getSimpleName();

	private CacheRequest request;

	private BufferedSource bufferedSource;

	private InputStream inputStream;
	
	private boolean isExpired;

	private long lastModified;
	
	private CacheResponse(Builder builder)
	{
		this.request = builder.request;
		this.bufferedSource = builder.bufferedSource;
		this.inputStream = builder.inputStream;
		this.lastModified = builder.lastModified;
		this.isExpired = builder.isExpired;
		ensureSaneDefaults();
	}
	
	private void ensureSaneDefaults()
	{
		
	}
	
	public CacheRequest getRequest()
	{
		return request;
	}

	public byte[] getData()
	{
		try {
			return bufferedSource.readByteArray();
		} catch (IOException ex) {
			Logger.e(TAG, "ioexception in get data call : ", ex);
			return  null;
		} finally {
			Utils.closeStreams(bufferedSource);
		}
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public boolean isExpired()
	{
		return isExpired;
	}

	public long getLastModifiedTime()
	{
		return lastModified;
	}
	
	public static class Builder
	{
		
		private CacheRequest request;

		private BufferedSource bufferedSource;

		private InputStream inputStream;
		
		private boolean isExpired;

		private long lastModified;

		public CacheResponse build()
		{
			return new CacheResponse(this);
		}
		
		public Builder setRequest(CacheRequest request)
		{
			this.request = request;
			return this;
		}

		public Builder setBufferedSource(BufferedSource bufferedSource)
		{
			this.bufferedSource = bufferedSource;
			return this;
		}

		public Builder setInputStream(InputStream inputStream)
		{
			this.inputStream = inputStream;
			return this;
		}

		public Builder setLastModifiedTime(long lastModified)
		{
			this.lastModified = lastModified;
			return this;
		}

		public Builder setExpired(boolean isExpired)
		{
			this.isExpired = isExpired;
			return this;
		}
	}
}
