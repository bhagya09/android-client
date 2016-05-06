/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bsb.hike.BitmapModule;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * A BitmapDrawable that keeps track of whether it is being displayed or cached. When the drawable is no longer being displayed or cached, {@link Bitmap#recycle() recycle()} will
 * be called on this drawable's bitmap.
 */
public class RecyclingBitmapDrawable extends BitmapDrawable
{

	static final String LOG_TAG = "CountingBitmapDrawable";

	private int mCacheRefCount = 0;

	private int mDisplayRefCount = 0;

	private boolean mHasBeenDisplayed;

	public RecyclingBitmapDrawable(Resources res, Bitmap bitmap)
	{
		super(res, bitmap);
	}

	@SuppressWarnings("deprecation")
	public RecyclingBitmapDrawable(Bitmap bitmap)
	{
		super(bitmap);
	}

	/**
	 * Notify the drawable that the displayed state has changed. Internally a count is kept so that the drawable knows when it is no longer being displayed.
	 * 
	 * @param isDisplayed
	 *            - Whether the drawable is being displayed or not
	 */
	public void setIsDisplayed(boolean isDisplayed)
	{
		synchronized (this)
		{
			Logger.d(LOG_TAG, "Is Displayed : " + isDisplayed);
			if (isDisplayed)
			{
				mDisplayRefCount++;
				mHasBeenDisplayed = true;
			}
			else
			{
				if (mDisplayRefCount > 0)
				{
					mDisplayRefCount--;
				}
			}
			Logger.d(LOG_TAG, "IsDisplayed Count : " + mDisplayRefCount);
		}

		// Check to see if recycle() can be called
		checkState();
	}

	/**
	 * Notify the drawable that the cache state has changed. Internally a count is kept so that the drawable knows when it is no longer being cached.
	 * 
	 * @param isCached
	 *            - Whether the drawable is being cached or not
	 */
	public void setIsCached(boolean isCached)
	{
		synchronized (this)
		{
			Logger.d(LOG_TAG, "Is Cached : " + isCached);
			if (isCached)
			{
				mCacheRefCount++;
			}
			else
			{
				if (mCacheRefCount > 0)
				{
					mCacheRefCount--;
				}
			}
			Logger.d(LOG_TAG, "Is Cached Count : " + mCacheRefCount);
		}

		// Check to see if recycle() can be called
		checkState();
	}

	private synchronized void checkState()
	{
		// If the drawable cache and display ref counts = 0, and this drawable
		// has been displayed, then recycle
		if (mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed && isBitmapValid())
		{
			Logger.d(LOG_TAG, "No longer being used or cached so recycling. " + toString());
			getBitmap().recycle();
		}
	}

	public synchronized boolean isBitmapValid()
	{
		Bitmap bitmap = getBitmap();
		return bitmap != null && !bitmap.isRecycled();
	}

	public synchronized boolean isBitmapMutable()
	{
		Bitmap bitmap = getBitmap();
		return null != bitmap && bitmap.isMutable();
	}

	public int size()
	{
		Bitmap bitmap = this.getBitmap();

		// From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
		// larger than bitmap byte count.
		if (Utils.hasKitKat())
		{
			return bitmap.getAllocationByteCount();
		}
		return bitmap.getByteCount();
	}

	/**
	 * This is a method added for as a workaround fix for issues we were facing when we don't put the drawable in the cache (the bitmap was getting recycled). Here we increment
	 * the cache ref count to ensure the bitmap is not recycled no matter what the display ref count is.
	 */
	public void incrementCacheReference()
	{
		setIsCached(true);
	}
}
