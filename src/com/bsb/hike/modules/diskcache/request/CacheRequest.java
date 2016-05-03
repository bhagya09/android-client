package com.bsb.hike.modules.diskcache.request;

import com.bsb.hike.modules.httpmgr.Header;

import java.util.LinkedList;
import java.util.List;

public abstract class CacheRequest
{
	public static final long DEFAULT_TTL = -1;  // max ttl

	private String key;

	private long ttl;

	private long actualTtl;

	private List<Header> headers;
	
	/**
	 * Return cache key for the request
	 */
	public String getKey()
	{
		return this.key;
	}

	/**
	 * Return time to live of the request
	 */
	public long getTtl()
	{
		return this.ttl;
	}

	/**
	 * Return the exact time at which this request is invalidated
	 * @return
	 */
	public long getActualTtl() {
		return actualTtl;
	}

	/**
	 * Return list of Headers for request
	 */
	public List<Header> getHeaders()
	{
		return this.headers;
	}
	
	/**
	 * Subclasses must implement their version of getData
	 */
	public abstract byte[] getData();
	
	protected CacheRequest(Init<?> builder)
	{
		this.key = builder.key;
		this.ttl = builder.ttl;
		this.actualTtl = System.currentTimeMillis() + ttl;
		this.headers = builder.headers;
		ensureSaneDefaults();
	}
	
	private void ensureSaneDefaults()
	{
		if ( key == null)
		{
			throw new IllegalArgumentException("key cannot be null");
		}
		
		if(headers == null)
		{
			headers = new LinkedList<>();
		}

		if( ttl <= 0)
		{
			ttl = DEFAULT_TTL;
		}
	}
	
	protected static abstract class Init<S extends Init<S>>
	{
		private String key;
		
		private long ttl;
		
		private List<Header> headers;
		
		protected abstract S self();
		
		/**
		 * Sets Cache key
		 * @param key
		 */
		public S setKey(String key)
		{
			this.key = key;
			return self();
		}
		
		/**
		 * Sets time to live for the request
		 * @param ttl in milliseconds
		 *            <pre>for ex if u need to give ttl as 1 day please give ttl = 1 * 24 * 60 * 60 * 1000</pre>
		 *            <pre>-1 if don't want any tll for entry</pre>
		 */
		public S setTtl(long ttl)
		{
			this.ttl = ttl;
			return self();
		}
		
		/**
		 * Sets list of headers for request {@link Header}
		 * @param headers
		 */
		public S setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return self();
		}
		
		public abstract CacheRequest build();
	}
}
