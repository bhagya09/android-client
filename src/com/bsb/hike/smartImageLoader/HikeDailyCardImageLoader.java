package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;
import com.bsb.hike.view.TextDrawable;

import java.io.IOException;
import java.io.InputStream;

import io.fabric.sdk.android.services.network.HttpRequest;

/**
 * Created by varunarora on 11/05/16.
 */
public class HikeDailyCardImageLoader extends ImageWorker {


    public void setSource(String source) {
        this.source = source;
    }

    private String source;

    @Override
    protected Bitmap processBitmap(String data) {
        InputStream input = null;
        try {
            input = new java.net.URL(data).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Decode Bitmap
        Bitmap bitmap = BitmapFactory.decodeStream(input);

        return bitmap;
    }

    @Override
    protected Bitmap processBitmapOnUiThread(String data) {
        return processBitmap(data);
    }




    public void loadImage(String key, View imageView, boolean isFlinging, boolean runOnUiThread, Object refObj)
    {
        if (key == null)
        {
            return;
        }

		BitmapDrawable value = null;

		if (setDefaultDrawableNull)
		{
			imageView.setBackground(null);
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
            Logger.d(getClass().getSimpleName(), key + " Bitmap found in cache and is not recycled.");
            // Bitmap found in memory cache
            setImageDrawable(imageView, value);
            sendImageCallback(imageView, true);
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
                setImageDrawable(imageView,bd);
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
            TextDrawable.Builder textDrawableBuilder = null;
            Drawable[] layers = null;
            Drawable drawable = imageView.getBackground();

            if (drawable != null)
            {
                if (drawable instanceof BitmapDrawable)
                {
                    loadingBitmap = ((BitmapDrawable) drawable).getBitmap();
                }
                else if (drawable instanceof TextDrawable)
                {
                    textDrawableBuilder = ((TextDrawable) drawable).getBuilder();
                }else if(drawable instanceof LayerDrawable){
                    LayerDrawable layerDrawable = (LayerDrawable)drawable;
                    layers = new Drawable[layerDrawable.getNumberOfLayers()];
                    for(int i=0;i<layerDrawable.getNumberOfLayers();i++){
                        layers[i] = layerDrawable.getDrawable(i);
                    }
                }
            }

            if (textDrawableBuilder != null)
            {
                final AsyncShapeDrawable asyncDrawable = new AsyncShapeDrawable(task, textDrawableBuilder);
                imageView.setBackground(asyncDrawable);
            }
            else if(layers != null && layers.length > 0){
                final AsyncLayerDrawable layerDrawable = new AsyncLayerDrawable(layers,task);
                imageView.setBackground(layerDrawable);
            }
            else
            {
                final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, loadingBitmap, task);
                imageView.setBackground(asyncDrawable);
            }

            // NOTE: This uses a custom version of AsyncTask that has been pulled from the
            // framework and slightly modified. Refer to the docs at the top of the class
            // for more info on what was changed.
            task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR, key);
        }
    }


}
