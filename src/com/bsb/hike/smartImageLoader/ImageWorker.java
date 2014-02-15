/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.bsb.hike.smartImageLoader;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.ui.utils.RecyclingBitmapDrawable;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an ImageView. It handles things like using a memory and disk cache, running the work in
 * a background thread and setting a placeholder image.
 */
public abstract class ImageWorker
{
	private static final String TAG = "ImageWorker";

	public static final String ROUND_SUFFIX = "round";

	private static final int FADE_IN_TIME = 100;

	protected HikeLruCache mImageCache;

	private Bitmap mLoadingBitmap;

	private boolean mFadeInBitmap = true;

	private AtomicBoolean mExitTasksEarly = new AtomicBoolean(false);

	protected boolean mPauseWork = false;

	private final Object mPauseWorkLock = new Object();

	protected Resources mResources;

	protected ImageWorker()
	{
		this.mImageCache = HikeMessengerApp.getLruCache();
	}

	public void setResource(Context ctx)
	{
		mResources = ctx.getResources();
	}

	public void loadImage(String data, boolean rounded, ImageView imageView, boolean runOnUiThread)
	{
		String key = data + (rounded ? ROUND_SUFFIX : "");
		loadImage(key, imageView, false, runOnUiThread);
	}

	/**
	 * Load an image specified by the data parameter into an ImageView (override {@link ImageWorker#processBitmap(Object)} to define the processing logic). A memory and disk cache
	 * will be used if an {@link ImageCache} has been added using {@link ImageWorker#addImageCache(FragmentManager, ImageCache.ImageCacheParams)}. If the image is found in the
	 * memory cache, it is set immediately, otherwise an {@link AsyncTask} will be created to asynchronously load the bitmap.
	 * 
	 * @param data
	 *            The URL of the image to download.
	 * @param imageView
	 *            The ImageView to bind the downloaded image to.
	 */
	public void loadImage(String data, ImageView imageView)
	{
		loadImage(data, imageView, false);
	}

	public void loadImage(String data, ImageView imageView, boolean isFlinging)
	{
		loadImage(data, imageView, isFlinging, false);
	}

	public void loadImage(String data, ImageView imageView, boolean isFlinging, boolean runOnUiThread)
	{
		if (data == null)
		{
			return;
		}

		BitmapDrawable value = null;

		if (mImageCache != null)
		{
			value = mImageCache.get(data);
			// if bitmap is found in cache and is recyclyed, remove this from cache and make thread get new Bitmap
			if (value != null && value.getBitmap().isRecycled())
			{
				mImageCache.remove(data);
				value = null;
			}
		}

		if (value != null)
		{
			Log.d(TAG, data + " Bitmap found in cache and is not recycled.");
			// Bitmap found in memory cache
			imageView.setImageDrawable(value);
		}
		else if (runOnUiThread)
		{
			Bitmap b = processBitmapOnUiThread(data);
			if (b != null && mImageCache != null)
			{
				BitmapDrawable bd = Utils.getBitmapDrawable(mResources, b);
				mImageCache.putInCache(data, bd);
				imageView.setImageDrawable(bd);
			}
		}
		else if (cancelPotentialWork(data, imageView) && !isFlinging)
		{
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, mLoadingBitmap, task);
			imageView.setImageDrawable(asyncDrawable);

			// NOTE: This uses a custom version of AsyncTask that has been pulled from the
			// framework and slightly modified. Refer to the docs at the top of the class
			// for more info on what was changed.
			task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, data);
		}
		else
		{
			imageView.setImageDrawable(null);
		}
	}

	/**
	 * Set placeholder bitmap that shows when the the background thread is running.
	 * 
	 * @param bitmap
	 */
	public void setLoadingImage(Bitmap bitmap)
	{
		mLoadingBitmap = bitmap;
	}

	/**
	 * Set placeholder bitmap that shows when the the background thread is running.
	 * 
	 * @param resId
	 */
	public void setLoadingImage(int resId)
	{
		mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
	}

	/**
	 * Adds an {@link ImageCache} to this {@link ImageWorker} to handle disk and memory bitmap caching.
	 * 
	 * @param fragmentManager
	 * @param cacheParams
	 *            The cache parameters to use for the image cache.
	 */
	public void addImageCache(HikeLruCache mImageCache)
	{
		this.mImageCache = mImageCache;
	}

	/**
	 * If set to true, the image will fade-in once it has been loaded by the background thread.
	 */
	public void setImageFadeIn(boolean fadeIn)
	{
		mFadeInBitmap = fadeIn;
	}

	public void setExitTasksEarly(boolean exitTasksEarly)
	{
		mExitTasksEarly.set(exitTasksEarly);
		setPauseWork(false);
	}

	/**
	 * Subclasses should override this to define any processing or work that must happen to produce the final bitmap. This will be executed in a background thread and be long
	 * running. For example, you could resize a large bitmap here, or pull down an image from the network.
	 * 
	 * @param data
	 *            The data to identify which image to process, as provided by {@link ImageWorker#loadImage(Object, ImageView)}
	 * @return The processed bitmap
	 */
	protected abstract Bitmap processBitmap(String data);

	/**
	 * Subclasses should override this to define any processing or work that must happen to produce the final bitmap. This will be executed in UI thread.
	 * 
	 * @param data
	 *            The data to identify which image to process, as provided by {@link ImageWorker#loadImage(Object, ImageView)}
	 * @return The processed bitmap
	 */
	protected abstract Bitmap processBitmapOnUiThread(String data);

	/**
	 * @return The {@link ImageCache} object currently being used by this ImageWorker.
	 */
	protected HikeLruCache getImageCache()
	{
		return mImageCache;
	}

	/**
	 * Cancels any pending work attached to the provided ImageView.
	 * 
	 * @param imageView
	 */
	public static void cancelWork(ImageView imageView)
	{
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null)
		{
			bitmapWorkerTask.cancel(true);
			final Object bitmapData = bitmapWorkerTask.data;
			Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
		}
	}

	/**
	 * Returns true if the current work has been canceled or if there was no work in progress on this image view. Returns false if the work in progress deals with the same data.
	 * The work is not stopped in that case.
	 */
	public static boolean cancelPotentialWork(Object data, ImageView imageView)
	{
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null)
		{
			final Object bitmapData = bitmapWorkerTask.data;
			if (bitmapData == null || !bitmapData.equals(data))
			{
				bitmapWorkerTask.cancel(true);
				Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
			}
			else
			{
				// The same work is already in progress.
				return false;
			}
		}
		return true;
	}

	/**
	 * @param imageView
	 *            Any imageView
	 * @return Retrieve the currently active work task (if any) associated with this imageView. null if there is no such task.
	 */
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView)
	{
		if (imageView != null)
		{
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable)
			{
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	/**
	 * The actual AsyncTask that will asynchronously process the image.
	 */
	private class BitmapWorkerTask extends MyAsyncTask<String, Void, BitmapDrawable>
	{
		private String data;

		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(ImageView imageView)
		{
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * Background processing.
		 */
		@Override
		protected BitmapDrawable doInBackground(String... params)
		{
			Log.d(TAG, "doInBackground - starting work");
			data = params[0];
			final String dataString = data;
			Bitmap bitmap = null;
			BitmapDrawable drawable = null;

			// Wait here if work is paused and the task is not cancelled
			synchronized (mPauseWorkLock)
			{
				while (mPauseWork && !isCancelled())
				{
					try
					{
						mPauseWorkLock.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}

			// If the bitmap was not found in the cache and this task has not been cancelled by
			// another thread and the ImageView that was originally bound to this task is still
			// bound back to this task and our "exit early" flag is not set, then call the main
			// process method (as implemented by a subclass)
			if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly.get())
			{
				bitmap = processBitmap(dataString);
			}

			// If the bitmap was processed and the image cache is available, then add the processed
			// bitmap to the cache for future use. Note we don't check if the task was cancelled
			// here, if it was, and the thread is still running, we may as well add the processed
			// bitmap to our cache as it might be used again in the future
			if (bitmap != null)
			{

				drawable = Utils.getBitmapDrawable(mResources, bitmap);

				if (mImageCache != null)
				{
					Log.d(TAG, "Putting data in cache : " + dataString);
					mImageCache.putInCache(dataString, drawable);
				}
			}

			return drawable;
		}

		/**
		 * Once the image is processed, associates it to the imageView
		 */
		@Override
		protected void onPostExecute(BitmapDrawable value)
		{
			// if cancel was called on this task or the "exit early" flag is set then we're done
			if (isCancelled() || mExitTasksEarly.get())
			{
				value = null;
			}

			final ImageView imageView = getAttachedImageView();
			if (value != null && imageView != null)
			{
				setImageDrawable(imageView, value);
			}
		}

		@Override
		protected void onCancelled(BitmapDrawable value)
		{
			super.onCancelled(value);
			synchronized (mPauseWorkLock)
			{
				mPauseWorkLock.notifyAll();
			}
		}

		/**
		 * Returns the ImageView associated with this task as long as the ImageView's task still points to this task as well. Returns null otherwise.
		 */
		private ImageView getAttachedImageView()
		{
			final ImageView imageView = imageViewReference.get();
			final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

			if (this == bitmapWorkerTask)
			{
				return imageView;
			}

			return null;
		}
	}

	/**
	 * A custom Drawable that will be attached to the imageView while the work is in progress. Contains a reference to the actual worker task, so that it can be stopped if a new
	 * binding is required, and makes sure that only the last started worker process can bind its result, independently of the finish order.
	 */
	private static class AsyncDrawable extends BitmapDrawable
	{
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask)
		{
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask()
		{
			return bitmapWorkerTaskReference.get();
		}
	}

	/**
	 * Called when the processing is complete and the final drawable should be set on the ImageView.
	 * 
	 * @param imageView
	 * @param drawable
	 */
	private void setImageDrawable(ImageView imageView, Drawable drawable)
	{
		if (drawable != null && ((BitmapDrawable) drawable).getBitmap().isRecycled())
		{
			Log.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
			return;
		}
		try
		{
			if (mFadeInBitmap)
			{
				// Transition drawable with a transparent drawable and the final drawable
				final TransitionDrawable td = new TransitionDrawable(new Drawable[] { new ColorDrawable(android.R.color.transparent), drawable });
				// Set background to loading bitmap
				imageView.setBackgroundDrawable(new BitmapDrawable(mResources, mLoadingBitmap));

				imageView.setImageDrawable(td);
				td.startTransition(FADE_IN_TIME);
			}
			else
			{
				imageView.setImageDrawable(drawable);
			}
		}
		catch (Exception e)
		{
			Log.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
		}
	}

	/**
	 * Pause any ongoing background work. This can be used as a temporary measure to improve performance. For example background work could be paused when a ListView or GridView is
	 * being scrolled using a {@link android.widget.AbsListView.OnScrollListener} to keep scrolling smooth.
	 * <p>
	 * If work is paused, be sure setPauseWork(false) is called again before your fragment or activity is destroyed (for example during {@link android.app.Activity#onPause()}), or
	 * there is a risk the background thread will never finish.
	 */
	public void setPauseWork(boolean pauseWork)
	{
		synchronized (mPauseWorkLock)
		{
			mPauseWork = pauseWork;
			if (!mPauseWork)
			{
				mPauseWorkLock.notifyAll();
			}
		}
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested width and height.
	 * 
	 * @param res
	 *            The resources object containing the image data
	 * @param resId
	 *            The resource id of the image data
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight, HikeLruCache cache)
	{

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		if (Utils.hasHoneycomb())
		{
			addInBitmapOptions(options, cache);
		}

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception in decoding Bitmap from resources: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, HikeLruCache cache)
	{

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		if (Utils.hasHoneycomb())
		{
			addInBitmapOptions(options, cache);
		}

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeFile(filename, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeFile(filename, options);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception in decoding Bitmap from file: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromByeArray(String msisdn, boolean rounded, int reqWidth, int reqHeight, HikeLruCache cache)
	{
		byte[] icondata = HikeUserDatabase.getInstance().getIconByteArray(msisdn, rounded);
		if (icondata == null)
			return null;
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeByteArray(icondata, 0, icondata.length, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		if (Utils.hasHoneycomb())
		{
			addInBitmapOptions(options, cache);
		}

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeByteArray(icondata, 0, icondata.length, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeByteArray(icondata, 0, icondata.length, options);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception in decoding Bitmap from ByteArray: ", e);
		}
		return result;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected static void addInBitmapOptions(BitmapFactory.Options options, HikeLruCache cache)
	{
		// inBitmap only works with mutable bitmaps so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;

		if (cache != null)
		{
			// Try and find a bitmap to use for inBitmap
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

			if (inBitmap != null)
			{
				Log.d(TAG, "Found a bitmap in reusable set.");
				options.inBitmap = inBitmap;
			}
		}
	}

	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding bitmaps using the decode* methods from {@link BitmapFactory}. This implementation
	 * calculates the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap having a width and height equal to or larger than the requested width
	 * and height.
	 * 
	 * @param options
	 *            An options object with out* params already populated (run through a decode* method with inJustDecodeBounds==true
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth)
			{
				inSampleSize *= 2;
			}

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger inSampleSize).

			long totalPixels = width * height / inSampleSize;

			// Anything more than 2x the requested pixels we'll sample down further
			final long totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels > totalReqPixelsCap)
			{
				inSampleSize *= 2;
				totalPixels /= 2;
			}
		}
		return inSampleSize;
	}

	public HikeLruCache getLruCache()
	{
		return this.mImageCache;
	}
}
