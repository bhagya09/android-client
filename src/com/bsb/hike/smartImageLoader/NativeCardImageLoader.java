package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.platform.NativeCardRenderer;
import com.bsb.hike.utils.HikeAnalyticsEvent;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by varunarora on 18/05/16.
 */
public class NativeCardImageLoader extends ImageWorker {
    private int width;
    private int height;
    private int layoutId;
    private String contentId;
    String msisdn;
    public NativeCardImageLoader(int width, int height, int layoutId, String contentId, String msisdn){
        this.height = height;
        this.width = width;
        this.layoutId = layoutId;
        this.contentId = contentId;
        this.msisdn = msisdn;
    }
    @Override
    protected Bitmap processBitmap(String data) {
        InputStream input = null;
        try {
            input = new java.net.URL(data).openStream();
            HikeAnalyticsEvent.nativeCardImageLoaded(layoutId, contentId, msisdn);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: add the recycling logic for the bitmap
        // Decode Bitmap
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        Bitmap resizedBitmap = null;
        if(bitmap != null){
            resizedBitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
        }

        return resizedBitmap;
    }

    @Override
    protected Bitmap processBitmapOnUiThread(String data) {
        return null;
    }
}
