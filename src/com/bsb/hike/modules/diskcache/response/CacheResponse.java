package com.bsb.hike.modules.diskcache.response;

import com.bsb.hike.modules.diskcache.request.CacheRequest;

public class CacheResponse
{
	private CacheRequest request;

	private byte[] data;
	
	private boolean isExpired;

	private long lastModified;
	
	private CacheResponse(Builder builder)
	{
		this.request = builder.request;
		this.data = builder.data;
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
		return data;
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

		private byte[] data;
		
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

		public Builder setData(byte[] data)
		{
			this.data = data;
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
