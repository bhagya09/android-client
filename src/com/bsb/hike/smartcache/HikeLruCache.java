package com.bsb.hike.smartcache;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.BitmapModule.RecyclingBitmapDrawable;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.customClasses.MySoftReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HikeLruCache extends LruCache<String, BitmapDrawable>
{

	// Default memory cache size in kilobytes
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 5; // 5MB

	// Compression settings when writing images to disk cache
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;

	private static final int DEFAULT_COMPRESS_QUALITY = 70;

	// Constants to easily toggle various caches
	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;

	private final static int MAX_CACHE_SIZE = 1024 * 15;

	private static volatile HikeLruCache instance;

	protected Resources mResources;

	private Context context;

	/**
	 * A holder class that contains cache parameters.
	 */
	public static class ImageCacheParams
	{
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;

		public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;

		public int compressQuality = DEFAULT_COMPRESS_QUALITY;

		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;

		/**
		 * Create a set of image cache parameters that can be provided to {@link ImageCache#getInstance(ImageCacheParams)}
		 */
		public ImageCacheParams(int memCacheSize)
		{
			this.memCacheSize = memCacheSize;
		}

		public ImageCacheParams()
		{

		}

		/**
		 * Sets the memory cache size based on a percentage of the max available VM memory. Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
		 * memory. Throws {@link IllegalArgumentException} if percent is < 0.01 or > .8. memCacheSize is stored in kilobytes instead of bytes as this will eventually be passed to
		 * construct a LruCache which takes an int in its constructor.
		 * 
		 * This value should be chosen carefully based on a number of factors Refer to the corresponding Android Training class for more discussion:
		 * http://developer.android.com/training/displaying-bitmaps/
		 * 
		 * @param percent
		 *            Percent of available app memory to use to size memory cache
		 */
		public void setMemCacheSizePercent(float percent)
		{
			if (percent < 0.01f || percent > 0.8f)
			{
				throw new IllegalArgumentException("setMemCacheSizePercent - percent must be " + "between 0.01 and 0.8 (inclusive)");
			}
			memCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
			if (memCacheSize > MAX_CACHE_SIZE)
				memCacheSize = MAX_CACHE_SIZE;
		}
	}

	private final Set<MySoftReference<Bitmap>> reusableBitmaps;

	private HikeLruCache(ImageCacheParams cacheParams, Context context)
	{
		super(cacheParams.memCacheSize);
		reusableBitmaps = Utils.canInBitmap() ? Collections.synchronizedSet(new HashSet<MySoftReference<Bitmap>>()) : null;
		this.context = context;
		this.mResources = context.getResources();
	}

	public static HikeLruCache getInstance(ImageCacheParams cacheParams, Context context)
	{
		if (instance == null)
		{
			synchronized (HikeLruCache.class)
			{
				if (instance == null)
					instance = new HikeLruCache(cacheParams, context);
			}
		}
		return instance;
	}

	/**
	 * Measure item size in kilobytes rather than units which is more practical for a bitmap cache
	 */
	protected int sizeOf(String key, BitmapDrawable value)
	{
		final int bitmapSize = BitmapUtils.getBitmapSize(value) / 1024;
		return bitmapSize == 0 ? 1 : bitmapSize;
	}

	protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue)
	{
		if (RecyclingBitmapDrawable.class.isInstance(oldValue))
		{
			// The removed entry is a recycling drawable, so notify it
			// that it has been removed from the memory cache
			((RecyclingBitmapDrawable) oldValue).setIsCached(false);
		}
	}

	public BitmapDrawable putInCache(String data, BitmapDrawable value)
	{
		if (null != value)
		{
			if (RecyclingBitmapDrawable.class.isInstance(value))
				((RecyclingBitmapDrawable) value).setIsCached(true);
			return put(data, value);
		}
		return null;
	}

	/**
	 * @param options
	 *            - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options)
	{
		Bitmap bitmap = null;

		if (reusableBitmaps != null && !reusableBitmaps.isEmpty())
		{
			synchronized (reusableBitmaps)
			{
				final Iterator<MySoftReference<Bitmap>> iterator = reusableBitmaps.iterator();
				Bitmap item;

				while (iterator.hasNext())
				{
					item = iterator.next().get();

					if (null != item && item.isMutable())
					{
						// Check to see it the item can be used for inBitmap
						if (canUseForInBitmap(item, options))
						{
							bitmap = item;

							// Remove from reusable set so it can't be used again
							iterator.remove();
							break;
						}
					}
					else
					{
						// Remove from the set if the reference has been cleared.
						iterator.remove();
					}
				}
			}
		}

		return bitmap;
	}

    public BitmapDrawable getBitmapDrawable(String key){

        BitmapDrawable value;

            value = get(key);

            // if bitmap is found in cache and is recyclyed, remove this from cache and make thread get new Bitmap
            if (null != value && value.getBitmap().isRecycled())
            {
                remove(key);
                value = null;
            }
            if (value == null){
                value = (BitmapDrawable) HikeConversationsDatabase.getInstance().getFileThumbnail(key);
                putInCache(key, value);
            }

        return value;

    }

	/**
	 * @param candidate
	 *            - Bitmap to check
	 * @param targetOptions
	 *            - Options that have the out* value populated
	 * @return true if <code>candidate</code> can be used for inBitmap re-use with <code>targetOptions</code>
	 */
	@TargetApi(VERSION_CODES.KITKAT)
	private static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions)
	{

		if (!Utils.hasKitKat())
		{
			// On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
			return candidate.getWidth() == targetOptions.outWidth && candidate.getHeight() == targetOptions.outHeight && targetOptions.inSampleSize == 1;
		}

		// From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
		// is smaller than the reusable bitmap candidate allocation byte count.
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
		return byteCount <= candidate.getAllocationByteCount();
	}

	/**
	 * Return the byte usage per pixel of a bitmap based on its configuration.
	 * 
	 * @param config
	 *            The bitmap configuration.
	 * @return The byte usage per pixel.
	 */
	private static int getBytesPerPixel(Config config)
	{
		if (config == Config.ARGB_8888)
		{
			return 4;
		}
		else if (config == Config.RGB_565)
		{
			return 2;
		}
		else if (config == Config.ARGB_4444)
		{
			return 2;
		}
		else if (config == Config.ALPHA_8)
		{
			return 1;
		}
		return 1;
	}

	/**
	 * 
	 * @param key
	 * @return This method is synchronous method and can call db if image is not found in cache
	 */
	public BitmapDrawable getIconFromCache(String key)
	{
		BitmapDrawable b = get(key);
		if (b == null)
		{
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
			if(idx == -1)
				idx = key.lastIndexOf(TimelineUpdatesImageLoader.SMALL_ICON_ID_SUFFIX);
			if (idx > 0)
				key = new String(key.substring(0, idx));
			BitmapDrawable bd = (BitmapDrawable) ContactManager.getInstance().getIcon(key);
			if (bd != null)
			{
				putInCache(key, bd);
			}
			return bd;
		}
		else
			return b;
	}
	
	public BitmapDrawable getFileIconFromCache(String key)
	{
		BitmapDrawable b = get(key);
		if (b == null)
		{
			BitmapDrawable bd = (BitmapDrawable) HikeConversationsDatabase.getInstance().getFileThumbnail(key);
			if (bd == null)
			{
				return null;
			}
			putInCache(key, bd);
			return bd;
		}
		else
			return b;
	}
	
	public boolean deleteIconForMSISDN(String msisdn)
	{
        boolean rowsDeleted = ContactManager.getInstance().removeIcon(msisdn);
        clearIconForMSISDN(msisdn);
		return rowsDeleted;
	}

	public void clearIconForMSISDN(String msisdn)
	{
		remove(msisdn);
		remove(msisdn + ProfileActivity.PROFILE_PIC_SUFFIX);
	}

	public void clearIconCache()
	{
		evictAll();
	}

	public void removeItemForKey(String key)
	{
		remove(key);
	}

	public void removeChatTheme(String chatThemeId)
	{
		remove(chatThemeId+HikeConstants.ORIENTATION_LANDSCAPE);
		remove(chatThemeId+HikeConstants.ORIENTATION_PORTRAIT);
	}

	public void saveChatTheme(String chatThemeId,BitmapDrawable bitmapDrawable)
	{
		if(TextUtils.isEmpty(chatThemeId)||bitmapDrawable==null)
		{
			return;
		}

		put(chatThemeId, bitmapDrawable);
	}

	public BitmapDrawable getChatTheme(String chatThemeId)
	{
		BitmapDrawable bd = null;
		if (TextUtils.isEmpty(chatThemeId))
		{
			return bd;
		}
		bd = get(chatThemeId);

		return bd;
	}
}
