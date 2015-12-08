package com.bsb.hike.modules.diskcache;

import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.modules.diskcache.response.CacheResponse;

public final class CacheStrategy
{
	public final CacheRequest cacheRequest;
	
	public final CacheResponse cacheResponse;
	
	public CacheStrategy(CacheRequest request, CacheResponse response)
	{
		this.cacheRequest = request;
		this.cacheResponse = response;
	}
	
	public static class Factory
	{
		private CacheRequest request;
		
		private CacheResponse response;
		
		private InternalCache cache;
		
		private String key;
		
		private boolean isExpired;
		
		public Factory(CacheRequest request, CacheResponse response, InternalCache cache)
		{
			this.request = request;
			this.response = response;
			this.cache = cache;
			
			this.key = request.getKey();
			
			isExpired = response.isExpired();
		}

		public CacheStrategy get()
		{
			if(response == null)
			{
				return new CacheStrategy(request, null);
			}
			
			else if(isExpired)
			{
				cache.remove(key);
				return new CacheStrategy(request, null);
			}

			return new CacheStrategy(request, response);
		}
	}
	
}
