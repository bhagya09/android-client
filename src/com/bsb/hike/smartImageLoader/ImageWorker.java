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

import org.apache.http.util.TextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TextDrawable;
import com.bsb.hike.view.TextDrawable.Builder;
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

    protected Bitmap mLoadingBitmap;

    private boolean dontSetBackground = false;

    private boolean mFadeInBitmap = true;

    private AtomicBoolean mExitTasksEarly = new AtomicBoolean(false);

    protected Resources mResources;

    private boolean setDefaultAvatarIfNoCustomIcon = false;

    private boolean setHiResDefaultAvatar = false;

    protected boolean setDefaultDrawableNull = true;

    protected boolean cachingEnabled = true;

    protected ImageLoaderListener imageLoaderListener;
    protected String foreGroundColor;

    public void setForeGroundColor(String foreGroundColor) {
        this.foreGroundColor = foreGroundColor;
    }

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

    /**
     * Load Image.
     *
     * @param key
     *            Could be any primary key msisdn, groupid, statusid, etc
     * @param imageView
     *            ImageView on which the Bitmap is to be set
     * @param isFlinging
     *            Used in-case if the caller is an AdapterView and is flinging.
     * @param runOnUiThread
     *            Load image on UI thread (not recommended)
     * @param setDefaultAvatarInitially
     *            If true, default avatar will be set on provided ImageView until key Bitmap is loaded
     * @param refObj
     *            Contextual object, passed back to loadImage(key,object) implementation of overriding class.
     */
    public void loadImage(String key, ImageView imageView, boolean isFlinging, boolean runOnUiThread, boolean setDefaultAvatarInitially, Object refObj)
    {
        if (key == null)
        {
            return;
        }

        BitmapDrawable value = null;

        if (setDefaultAvatarInitially)
        {
            setDefaultAvatar(imageView, key, refObj);
        }
        else
        {
            if(setDefaultDrawableNull)
            {
                imageView.setImageDrawable(null);
                imageView.setBackgroundDrawable(null);
            }
        }

        if (mImageCache != null)
        {
            value = mImageCache.get(key);
            // if bitmap is found in cache and is recyclyed, remove this from cache and make thread get new Bitmap
            if (value != null && value.getBitmap().isRecycled())
            {
                mImageCache.remove(key);
                value = null;
            }
        }
        if (value != null)
        {
            Logger.d(TAG, key + " Bitmap found in cache and is not recycled.");
            // Bitmap found in memory cache
            setImageDrawable(imageView, value);
            sendImageCallback(imageView , true);
        }
        else if (runOnUiThread)
        {
            Bitmap b = processBitmapOnUiThread(key);
            if (b != null && mImageCache != null)
            {
                BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(mResources, b);
                if (bd != null && cachingEnabled)
                {
                    mImageCache.putInCache(key, bd);
                }
                imageView.setImageDrawable(bd);
                sendImageCallback(imageView,true);
            }
            else if (b == null && setDefaultAvatarIfNoCustomIcon)
            {
                String data = key;
                int idx = data.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
                if (idx > 0)
                    key = new String(data.substring(0, idx));

                setDefaultAvatar(imageView, key,refObj);
                sendImageCallback(imageView,true);
            }
            else
            {
                sendImageCallback(imageView,false);
            }

        }
        else if (cancelPotentialWork(key, imageView) && !isFlinging)
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (refObj != null)
            {
                task.setContextObject(refObj);
            }

            Bitmap loadingBitmap = mLoadingBitmap;
            Builder textDrawableBuilder = null;

            Drawable drawable = imageView.getDrawable();

            if (drawable != null)
            {
                if (drawable instanceof BitmapDrawable)
                {
                    loadingBitmap = ((BitmapDrawable) drawable).getBitmap();
                }
                else if (drawable instanceof TextDrawable)
                {
                    textDrawableBuilder = ((TextDrawable) drawable).getBuilder();
                }
            }

            if (textDrawableBuilder != null)
            {
                final AsyncShapeDrawable asyncDrawable = new AsyncShapeDrawable(task, textDrawableBuilder);
                imageView.setImageDrawable(asyncDrawable);
            }
            else
            {
                final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, loadingBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
            }

            // NOTE: This uses a custom version of AsyncTask that has been pulled from the
            // framework and slightly modified. Refer to the docs at the top of the class
            // for more info on what was changed.
            task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, key);
        }
    }

    public void loadImage(String data, ImageView imageView, boolean isFlinging, boolean runOnUiThread, boolean setDefaultAvatarInitially)
    {
        loadImage(data, imageView, isFlinging, runOnUiThread, setDefaultAvatarInitially, null);
    }

    TypedArray bgColorArray = Utils.getDefaultAvatarBG();
    protected void setDefaultAvatar(ImageView imageView, String data, Object refObj)
    {
        if (refObj instanceof ContactInfo)
        {
            ContactInfo cInfo = (ContactInfo) refObj;
            if(!TextUtils.isEmpty(cInfo.getFirstName()))
            {
                int index = BitmapUtils.iconHash(cInfo.getMsisdn()) % (bgColorArray.length());
                int bgColor = bgColorArray.getColor(index, 0);
                imageView.setImageDrawable(HikeBitmapFactory.getDefaultTextAvatar(cInfo.getFirstName(),-1,bgColor,true));
            }
            else
            {
                imageView.setImageDrawable(HikeBitmapFactory.getDefaultTextAvatar(data));
            }
        }
        else
        {
            imageView.setImageDrawable(HikeBitmapFactory.getDefaultTextAvatar(data));
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

    public boolean getIsExitTasksEarly()
    {
        return mExitTasksEarly.get();
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

    protected Bitmap processBitmap(String data,Object refObj)
    {
        //Overridden
        return null;
    }

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
    public static boolean cancelPotentialWork(Object data, View imageView)
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
            else if (drawable instanceof AsyncShapeDrawable)
            {
                final AsyncShapeDrawable asyncDrawable = (AsyncShapeDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
    private static BitmapWorkerTask getBitmapWorkerTask(View imageView)
    {
        if (imageView != null)
        {
            final Drawable drawable = imageView.getBackground();
            if (drawable instanceof AsyncDrawable)
            {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
            else if (drawable instanceof AsyncShapeDrawable)
            {
                final AsyncShapeDrawable asyncDrawable = (AsyncShapeDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }else if (drawable instanceof AsyncLayerDrawable)
            {
                final AsyncLayerDrawable asyncDrawable = (AsyncLayerDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    protected class BitmapWorkerTask extends MyAsyncTask<String, Void, BitmapDrawable>
    {
        private String data;

        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<View> viewReference;
        private WeakReference<Object> objectReference;

        public void setContextObject(Object refObject)
        {
            objectReference = new WeakReference<Object>(refObject);
        }

        public BitmapWorkerTask(ImageView imageView)
        {
            imageViewReference = new WeakReference<ImageView>(imageView);
            viewReference = null;
        }
        public BitmapWorkerTask(View imageView)
        {
            viewReference = new WeakReference<View>(imageView);
            imageViewReference = null;
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
            if (mImageCache != null && !isCancelled() && (getAttachedImageView() != null || getAttachedView() != null) && !mExitTasksEarly.get())
            {
                if (objectReference != null)
                {
                    bitmap = processBitmap(dataString,objectReference.get());
                }
                else
                {
                    bitmap = processBitmap(dataString);
                }
            }

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the cache for future use. Note we don't check if the task was cancelled
            // here, if it was, and the thread is still running, we may as well add the processed
            // bitmap to our cache as it might be used again in the future
            if (bitmap != null)
            {

                drawable = HikeBitmapFactory.getBitmapDrawable(mResources, bitmap);

                if (mImageCache != null && cachingEnabled)
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
            final View view = getAttachedView();
            if(imageView != null)
            {
                if (value != null)
                {
                    setImageDrawable(imageView, value);
                    sendImageCallback(imageView, true);
                }
                else if (setDefaultAvatarIfNoCustomIcon)
                {
                    String key = data;
                    int idx = data.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
                    if (idx > 0)
                        key = new String(data.substring(0, idx));

                    setDefaultAvatar(imageView, key,null);
                    sendImageCallback(imageView, true);
                }
                else if (defaultDrawable != null)
                {
					/*
					 * This case is currently being used in very specific scenerio of
					 * media viewer files for which we could not create thumbnails(ex. tif images)
					 */
                    setImageDrawable(imageView, defaultDrawable);
                    sendImageCallback(imageView,true);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
                else
                {
                    sendImageCallback(imageView, false);
                }

            }else if(view != null){
                if (value != null)
                {
                    setImageDrawable(view, value);
                    sendImageCallback(view, true);
                }
                else if (defaultDrawable != null)
                {
					/*
					 * This case is currently being used in very specific scenerio of
					 * media viewer files for which we could not create thumbnails(ex. tif images)
					 */
                    setImageDrawable(view, defaultDrawable);
                    sendImageCallback(view, true);
                }
                else
                {
                    sendImageCallback(view, false);
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
            if(imageViewReference == null){
                return null;
            }
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask)
            {
                return imageView;
            }

            return null;
        }
        private View getAttachedView()
        {
            if(viewReference == null){
                return null;
            }
            final View imageView = viewReference.get();
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
    protected static class AsyncDrawable extends BitmapDrawable
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

    protected static class AsyncShapeDrawable extends TextDrawable
    {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncShapeDrawable(BitmapWorkerTask bitmapWorkerTask, Builder builder)
        {
            super(builder);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask()
        {
            return bitmapWorkerTaskReference.get();
        }
    }
    protected static class AsyncLayerDrawable extends LayerDrawable{
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
        /**
         * Create a new layer drawable with the list of specified layers.
         *
         * @param layers A list of drawables to use as layers in this new drawable.
         */
        public AsyncLayerDrawable(Drawable[] layers, BitmapWorkerTask bitmapWorkerTask) {
            super(layers);
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
    protected void setImageDrawable(ImageView imageView, Drawable drawable)
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
                final TransitionDrawable td = new TransitionDrawable(new Drawable[] { drawable, new ColorDrawable(mResources.getColor(android.R.color.transparent))  });

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
    protected void setImageDrawable(View imageView, Drawable drawable)
    {
        if (drawable != null && drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap().isRecycled())
        {
            Logger.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
            return;
        }
        try
        {
            if (mFadeInBitmap)
            {
                // Transition drawable with a transparent drawable and the final drawable
                final TransitionDrawable td = new TransitionDrawable(new Drawable[] { drawable, new ColorDrawable(mResources.getColor(android.R.color.transparent))  });

                if (!dontSetBackground)
                {
                    // Set background to loading bitmap
                    imageView.setBackgroundDrawable(HikeBitmapFactory.getBitmapDrawable(mResources, mLoadingBitmap));
                }

                imageView.setBackground(td);
                td.startTransition(FADE_IN_TIME);
            }
            else
            {
                if(foreGroundColor != null){
                    LayerDrawable backgroundDrawable = new LayerDrawable(new Drawable[] { drawable,
                            new ColorDrawable(Color.parseColor(foreGroundColor)) });
                    imageView.setBackground(backgroundDrawable);
                }else{
                    imageView.setBackground(drawable);
                }

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

    public void setCachingEnabled(boolean enableCache)
    {
        this.cachingEnabled = enableCache;
    }

    public boolean isCachingEnabled()
    {
        return cachingEnabled;
    }

    public interface ImageLoaderListener {

        public void onImageWorkSuccess(ImageView imageView);

        public void onImageWorkFailed(ImageView imageView);
    }

    public void setImageLoaderListener(ImageLoaderListener imageLoaderListener)
    {
        this.imageLoaderListener = imageLoaderListener;
    }

    /**
     * This is the call back to listener after image is loaded into ImageView
     * @param imageView
     */
    private void sendImageCallback(ImageView imageView, boolean success)
    {
        if(imageLoaderListener != null && success)
        {
            imageLoaderListener.onImageWorkSuccess(imageView);
        }
        else if(imageLoaderListener != null && !success)
        {
            imageLoaderListener.onImageWorkFailed(imageView);
        }
    }

    private ImageLoaderViewListener imageLoaderViewListener;
    public void setImageLoaderListener(ImageLoaderViewListener imageLoaderListener)
    {
        this.imageLoaderViewListener = imageLoaderListener;
    }

    /**
     * This is the call back to listener after image is loaded into ImageView
     * @param imageView
     */
    protected void sendImageCallback(View imageView, boolean success)
    {
        if(imageLoaderViewListener != null && success)
        {
            imageLoaderViewListener.onImageWorkSuccess(imageView);
        }
        else if(imageLoaderViewListener != null && !success)
        {
            imageLoaderViewListener.onImageWorkFailed(imageView);
        }
    }
    public interface ImageLoaderViewListener {

        public void onImageWorkSuccess(View imageView);

        public void onImageWorkFailed(View imageView);

    }
}