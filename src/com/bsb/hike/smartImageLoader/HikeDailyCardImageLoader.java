package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
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


    private static final String TAG = HikeDailyCardImageLoader.class.getSimpleName();

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

    protected void setImageDrawable(ImageView imageView, Drawable drawable)
    {
        try
        {
            if(TextUtils.isEmpty(foreGroundColor)){
                imageView.setImageDrawable(drawable);
            }else{
                LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{drawable, new ColorDrawable(Color.parseColor(foreGroundColor))});
                imageView.setImageDrawable(layerDrawable);
            }


        }
        catch (Exception e)
        {
            Logger.d(TAG, "Bitmap is already recycled when setImageDrawable is called in ImageWorker post processing.");
        }
    }
}
