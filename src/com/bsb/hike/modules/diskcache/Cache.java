package com.bsb.hike.modules.diskcache;

import com.bsb.hike.modules.diskcache.request.ByteArrayRequest;
import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.modules.diskcache.response.CacheResponse;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.utils.Logger;
import com.squareup.okhttp.internal.DiskLruCache;
import com.squareup.okhttp.internal.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public final class Cache
{

	private static final String TAG = "Cache";

	private static final int VERSION = 1;

	private static final int ENTRY_METADATA = 0;
	
	private static final int ENTRY_BODY = 1;

	private static final int ENTRY_COUNT = 2;
	
	final InternalCache internalCache = new InternalCache()
	{
		@Override
		public CacheResponse get(String key)
		{
			return Cache.this.get(key);
		}

		@Override
		public boolean put(CacheRequest request)
		{
			return Cache.this.put(request);
		}

		@Override
		public boolean remove(String key)
		{
			return Cache.this.remove(key);

		}

		@Override
		public boolean update(CacheRequest request)
		{
			return Cache.this.update(request);
		}
	};

	private final DiskLruCache cache;

	public Cache(File directory, long maxSize)
	{
		synchronized (Cache.this)
		{
			cache = DiskLruCache.create(directory, VERSION, ENTRY_COUNT, maxSize);
		}
	}

	public InternalCache getCache()
	{
		return internalCache;
	}

	/**
	 *  Get cached data for particular key
		
	 * @param key
	 * @return {@link CacheResponse}
	 */
	private CacheResponse get(String key)
	{
		assert cache != null;

		DiskLruCache.Snapshot snapshot;
		Entry entry;

		try
		{
			snapshot = cache.get(key);
			if (snapshot == null)
			{
				return null;
			}
		}
		catch (IOException e)
		{
			Logger.e(TAG, "IO Exception in get ", e);
			return null;
		}

		try
		{
			entry = new Entry(snapshot.getSource(ENTRY_METADATA));
		}
		catch (IOException e)
		{
			Logger.e(TAG, "IO Exception in get ", e);
			Util.closeQuietly(snapshot);
			return null;
		}

		CacheResponse response = entry.response(snapshot);

		if (!entry.matches(key, response.getRequest().getKey()))
		{
			return null;
		}

		CacheStrategy cacheStrategy = new CacheStrategy.Factory(response.getRequest(), response, internalCache).get();

		return cacheStrategy.cacheResponse;
	}

	/**
	 * Puts {@link CacheRequest} contents in cache.
	 * <li>Note : It stores entries metadata in file and data in other file</li>
	 * <li>Note : metadata file is named as key.0</li>
	 * <li>Note : data file is named as key.1</li>
	 * <li> metadata information consists of key, full headerlist and cache control info </li>
	 * 
	 * @param request
	 * @return true if entry is successfully inserted in cache, false otherwise
	 */
	private boolean put(CacheRequest request)
	{
		assert cache != null;

		Entry entry = new Entry(request);
		DiskLruCache.Editor editor = null;
		try
		{
			editor = cache.edit(request.getKey());
			if (editor == null)
			{
				return false;
			}
			entry.writeTo(editor);
			editor.commit();
			return true;
		}
		catch (IOException e)
		{
			abortQuietly(editor);
			return false;
		}
	}

	/**
	 * removes entry from the cache
	 * @param key
	 * @return true if entry is successfully removed, false otherwise
	 */
	private boolean remove(String key)
	{
		assert cache != null;

		try
		{
			return cache.remove(key);
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/**
	 * For now it is same as put call
	 * 
	 * @param request
	 * @return
	 */
	private boolean update(CacheRequest request)
	{
		return put(request);
	}

	/**
	 * Closes the cache and deletes all of its stored values. This will delete all files in the cache directory including files that weren't created by the cache.
	 */
	public void delete() throws IOException
	{
		cache.delete();
	}

	/**
	 * Deletes all values stored in the cache. In-flight writes to the cache will complete normally, but the corresponding responses will not be stored.
	 */
	public void evictAll() throws IOException
	{
		cache.evictAll();
	}
	
	/**
	 * Returns the number of bytes currently being used to store the values in this cache. This may be greater than the max size if a background deletion is pending.
	 * @throws IOException
	 */
	public long getSize() throws IOException
	{
		return cache.size();
	}

	/**
	 * Returns the maximum number of bytes that this cache should use to store its data.
	 */
	public long getMaxSize()
	{
		return cache.getMaxSize();
	}

	/**
	 * Force buffered operations to the filesystem.
	 * @throws IOException
	 */
	public void flush() throws IOException
	{
		cache.flush();
	}

	/**
	 * Closes this cache. Stored values will remain on the filesystem.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		cache.close();
	}

	/**
	 * Returns the directory where this cache stores its data.
	 */
	public File getDirectory()
	{
		return cache.getDirectory();
	}

	/**
	 * Returns true if this cache has been closed.
	 */
	public boolean isClosed()
	{
		return cache.isClosed();
	}

	private static void abortQuietly(DiskLruCache.Editor editor)
	{
		// Give up because the cache cannot be written.
		try
		{
			if (editor != null)
			{
				editor.abort();
			}
		}
		catch (IOException ignored)
		{
		}
	}

	private static class Entry
	{
		private byte[] data;

		private String key;

		private long lastModified;

		private long ttl;

		private List<Header> headerList;

		public boolean isExpired()
		{
			return this.ttl < System.currentTimeMillis();
		}

		/**
	     * Reads an entry from an input stream. A typical entry looks like this:
	     * <pre>{@code 
	     *   key
	     *   last modified time
	     *   ttl
	     *   headers each seperated by new line
	     *   data
	     * }</pre>
	     *
	     */

		public Entry(Source in) throws IOException
		{
			try
			{
				BufferedSource source = Okio.buffer(in);
				key = source.readUtf8LineStrict();
				lastModified = readLong(source);
				ttl = readLong(source);
				int headerListSize = readInt(source);
				headerList = new ArrayList<>(headerListSize);

				for (int i = 0; i < headerListSize; i++)
				{
					Header header = readHeader(source);
					headerList.add(header);
				}
				data = source.readByteArray();
			}
			finally
			{
				in.close();
			}
		}

		public Entry(CacheRequest request)
		{
			this.key = request.getKey();
			this.data = request.getData();
			this.lastModified = System.currentTimeMillis();
			this.headerList = request.getHeaders();
			this.ttl = request.getTtl();
		}

		public void writeTo(DiskLruCache.Editor editor) throws IOException
		{
			BufferedSink sink = Okio.buffer(editor.newSink(ENTRY_METADATA));
			sink.writeUtf8(this.key);
			sink.writeByte('\n');
			sink.writeUtf8(Long.toString(lastModified));
			sink.writeByte('\n');
			sink.writeUtf8(Long.toString(ttl));
			sink.writeByte('\n');
			sink.writeUtf8(Integer.toString(headerList.size()));
			sink.writeByte('\n');
			for (Header header : headerList)
			{
				sink.writeUtf8(header.getName());
				sink.writeUtf8(": ");
				sink.writeUtf8(header.getValue());
				sink.writeByte('\n');
			}
			sink = Okio.buffer(editor.newSink(ENTRY_BODY));
			sink.write(data);
		}

		public CacheResponse response(DiskLruCache.Snapshot snapshot)
		{
			ByteArrayRequest request = new ByteArrayRequest.Builder()
					.setKey(key)
					.setHeaders(headerList)
					.setTtl(ttl)
					.setData(data)
					.build();

			return new CacheResponse.Builder()
					.setRequest(request)
					.setData(data)
					.setExpired(isExpired())
					.setLastModifiedTime(lastModified)
					.build();
		}

		public boolean matches(String requestKey, String responseKey)
		{
			return requestKey.equalsIgnoreCase(responseKey);
		}

		private static int readInt(BufferedSource source) throws IOException
		{
			String line = source.readUtf8LineStrict();
			try
			{
				return Integer.parseInt(line);
			}
			catch (NumberFormatException e)
			{
				throw new IOException("Expected an integer but was \"" + line + "\"");
			}
		}

		private static long readLong(BufferedSource source) throws IOException
		{
			String line = source.readUtf8LineStrict();
			try
			{
				return Long.parseLong(line);
			}
			catch (NumberFormatException e)
			{
				throw new IOException("Expected an long but was \"" + line + "\"");
			}
		}

		private static Header readHeader(BufferedSource source) throws IOException
		{
			String line = source.readUtf8LineStrict();
			try
			{
				int index = line.indexOf(":");
				return new Header(line.substring(0, index), line.substring(index + 1));
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				throw new IOException("Null key value pair for \"" + line + "\"");
			}
		}
	}

}
