package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.platform.NativeCardRenderer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by varunarora on 18/05/16.
 */
public class NativeCardImageLoader extends ImageWorker {
    private int width;
    private int height;
    public NativeCardImageLoader(int width, int height){
        this.height = height;
        this.width = width;
    }
    @Override
    protected Bitmap processBitmap(String data) {
        InputStream input = null;
        try {
            input = new java.net.URL(data).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: add the recycling logic for the bitmap
        // Decode Bitmap
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
        return resizedBitmap;
    }

    @Override
    protected Bitmap processBitmapOnUiThread(String data) {
        return null;
    }
}
