package com.bsb.hike.modules.diskcache;

import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.modules.diskcache.response.CacheResponse;

public interface InternalCache
{
	CacheResponse get(String key);

	boolean put(CacheRequest request);

	boolean remove(String key);

	boolean update(CacheRequest request);

	boolean isKeyExists(String key);

	boolean isClosed();
}