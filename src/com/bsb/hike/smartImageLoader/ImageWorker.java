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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an ImageView. It handles things like using a memory and disk cache, running the work in
 * a background thread and setting a placeholder image.
 */
public abstract class ImageWorker
{
	private static final String TAG = "ImageWorker";

	private static final int FADE_IN_TIME = 100;

	protected HikeLruCache mImageCache;

	private Bitmap mLoadingBitmap;

	private boolean dontSetBackground = false;

	private boolean mFadeInBitmap = true;

	private AtomicBoolean mExitTasksEarly = new AtomicBoolean(false);

	protected Resources mResources;

	private boolean setDefaultAvatarIfNoCustomIcon = false;

	private boolean setHiResDefaultAvatar = false;
	
	private boolean setDefaultDrawableNull = true;
	
	/*
	 * This case is currently being used in very specific scenerio of
	 * media viewer files for which we could not create thumbnails(ex. tif images)
	 */
	private Drawable defaultDrawable = null;

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
		String key = data + (rounded ? ProfileActivity.PROFILE_ROUND_SUFFIX : "");
		loadImage(key, imageView, false, runOnUiThread);
	}

	public void loadImage(String data, boolean rounded, ImageView imageView, boolean runOnUiThread, boolean isFlinging, boolean setDefaultAvatarInitially)
	{
		String key = data + (rounded ? ProfileActivity.PROFILE_ROUND_SUFFIX : "");
		loadImage(key, imageView, isFlinging, runOnUiThread, setDefaultAvatarInitially);
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
		loadImage(data, imageView, isFlinging, runOnUiThread, false);
	}

	public void loadImage(String data, ImageView imageView, boolean isFlinging, boolean runOnUiThread, boolean setDefaultAvatarInitially)
	{
		if (data == null)
		{
			return;
		}

		BitmapDrawable value = null;

		if (setDefaultAvatarInitially)
		{
			setDefaultAvatar(imageView, data);
		}
		else
		{
			if(setDefaultDrawableNull){
				imageView.setImageDrawable(null);
				imageView.setBackgroundDrawable(null);
			}
		}
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
			Logger.d(TAG, data + " Bitmap found in cache and is not recycled.");
			// Bitmap found in memory cache
			imageView.setImageDrawable(value);
		}
		else if (runOnUiThread)
		{
			Bitmap b = processBitmapOnUiThread(data);
			if (b != null && mImageCache != null)
			{
				BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(mResources, b);
				if (bd != null)
				{
					mImageCache.putInCache(data, bd);
				}
				imageView.setImageDrawable(bd);
			}
			else if (b == null && setDefaultAvatarIfNoCustomIcon)
			{
				setDefaultAvatar(imageView, data);
			}
		}
		else if (cancelPotentialWork(data, imageView) && !isFlinging)
		{
			Bitmap loadingBitmap = mLoadingBitmap;

			/*
			 * Setting this loading bitmap to prevent the imageView from showing a blank drawable while we try to fetch the actual drawable for the imageView.
			 */
			if (setDefaultAvatarInitially)
			{
				Drawable drawable = imageView.getDrawable();
				if (drawable instanceof BitmapDrawable)
				{
					loadingBitmap = ((BitmapDrawable) drawable).getBitmap();
				}
			}

			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, loadingBitmap, task);
			imageView.setImageDrawable(asyncDrawable);

			// NOTE: This uses a custom version of AsyncTask that has been pulled from the
			// framework and slightly modified. Refer to the docs at the top of the class
			// for more info on what was changed.
			task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, data);
		}
		// else
		// {
		// imageView.setImageDrawable(null);
		// }
	}

	protected void setDefaultAvatar(ImageView imageView, String data)
	{
		int idx = data.lastIndexOf(ProfileActivity.PROFILE_ROUND_SUFFIX);
		boolean rounded = false;
		if (idx > 0)
		{
			data = new String(data.substring(0, idx));
			rounded = true;
		}
		else
		{
			int idx1 = data.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
			if (idx1 > 0)
				data = new String(data.substring(0, idx1));
		}
		boolean isGroupConversation = Utils.isGroupConversation(data);

		imageView.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(data, rounded));
		if (setHiResDefaultAvatar)
		{
			imageView.setImageResource(isGroupConversation ? R.drawable.ic_default_avatar_group_hires : R.drawable.ic_default_avatar_hires);
		}
		else
		{
			imageView.setImageResource(isGroupConversation ? R.drawable.ic_default_avatar_group : R.drawable.ic_default_avatar);
		}
	}

	/**
	 * Flag which denotes whether the background was already set and should not be set by this worker.
	 * 
	 * @param b
	 */
	public void setDontSetBackground(boolean b)
	{
		this.dontSetBackground = b;
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
	 * @param bitmap
	 */
	public void setLoadingImage(Drawable bitmap)
	{
		if(bitmap != null)
			mLoadingBitmap = drawableToBitmap(bitmap);
	}

	/**
	 * Set placeholder bitmap that shows when the the background thread is running.
	 * 
	 * @param resId
	 */
	public void setLoadingImage(int resId)
	{
		mLoadingBitmap = HikeBitmapFactory.decodeBitmapFromResource(mResources, resId, Bitmap.Config.RGB_565);
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
	}

	public void setDefaultAvatarIfNoCustomIcon(boolean b)
	{
		this.setDefaultAvatarIfNoCustomIcon = b;
	}

	public void setHiResDefaultAvatar(boolean b)
	{
		this.setHiResDefaultAvatar = b;
	}

	public void setDefaultDrawableNull(boolean b)
	{
		this.setDefaultDrawableNull = b;
	}
	
	public void setDefaultDrawable(Drawable d)
	{
		this.defaultDrawable = d;
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
			Logger.d(TAG, "cancelWork - cancelled work for " + bitmapData);
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
				Logger.d(TAG, "cancelPotentialWork - cancelled work for " + data);
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
			Logger.d(TAG, "doInBackground - starting work");
			data = params[0];
			final String dataString = data;

			Bitmap bitmap = null;
			BitmapDrawable drawable = null;

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

				drawable = HikeBitmapFactory.getBitmapDrawable(mResources, bitmap);

				if (mImageCache != null)
				{
					Logger.d(TAG, "Putting data in cache : " + dataString);
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
			if(imageView != null)
			{
				if (value != null)
				{
					setImageDrawable(imageView, value);
				}
				else if (setDefaultAvatarIfNoCustomIcon)
				{
					setDefaultAvatar(imageView, data);
				}
				else if (defaultDrawable != null)
				{
					/*
					 * This case is currently being used in very specific scenerio of
					 * media viewer files for which we could not create thumbnails(ex. tif images)
					 */
					setImageDrawable(imageView, defaultDrawable);
					imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				}

			}
		}

		@Override
		protected void onCancelled(BitmapDrawable value)
		{
			super.onCancelled(value);
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
			Logger.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
			return;
		}
		try
		{
			if (mFadeInBitmap)
			{
				// Transition drawable with a transparent drawable and the final drawable
				final TransitionDrawable td = new TransitionDrawable(new Drawable[] { new ColorDrawable(android.R.color.transparent), drawable });
				if (!dontSetBackground)
				{
					// Set background to loading bitmap
					imageView.setBackgroundDrawable(HikeBitmapFactory.getBitmapDrawable(mResources, mLoadingBitmap));
				}

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
			Logger.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
		}
	}

	public HikeLruCache getLruCache()
	{
		return this.mImageCache;
	}
	
	public static Bitmap drawableToBitmap (Drawable drawable) {
	    if (drawable instanceof BitmapDrawable) {
	        return ((BitmapDrawable)drawable).getBitmap();
	    }

	    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap); 
	    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
	    drawable.draw(canvas);

	    return bitmap;
	}
}
