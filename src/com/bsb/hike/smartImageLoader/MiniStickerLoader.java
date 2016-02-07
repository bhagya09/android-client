package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.diskcache.response.CacheResponse;
import com.bsb.hike.modules.stickerdownloadmgr.MiniStickerImageDownloadTask;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

/**
 * Created by anubhavgupta on 03/01/16.
 */
public class MiniStickerLoader extends ImageWorker {

    private final String TAG = MiniStickerLoader.class.getSimpleName();

    private boolean downloadIfNotFound = false;

    private String categoryId;

    private String stickerId;

    public MiniStickerLoader(boolean downloadIfNotFound)
    {
        super();
        this.downloadIfNotFound = downloadIfNotFound;
    }

    @Override
    protected Bitmap processBitmap(String data) {
        Bitmap bitmap = null;
        try {
            String[] array = data.split(HikeConstants.DELIMETER);
            categoryId = array[0];
            stickerId = array[1];
            CacheResponse cacheResponse = HikeMessengerApp.getDiskCache().get(StickerManager.getInstance().getMiniStickerKey(stickerId, categoryId));
            if(cacheResponse != null) {
                bitmap = HikeBitmapFactory.decodeStream(cacheResponse.getInputStream());
            }
            downloadIfNotFound(bitmap);
        } catch (Exception e) {
            Logger.e(TAG, "exception in processBitmap : ", e);
        }
        return bitmap;
    }

    @Override
    protected Bitmap processBitmapOnUiThread(String data) {
        return null;
    }

    private void downloadIfNotFound(Bitmap bitmap)
    {
        if(bitmap == null && downloadIfNotFound)
        {
            MiniStickerImageDownloadTask miniStickerDownloadTask = new MiniStickerImageDownloadTask(categoryId, stickerId);
            miniStickerDownloadTask.execute();
        }
    }
}
