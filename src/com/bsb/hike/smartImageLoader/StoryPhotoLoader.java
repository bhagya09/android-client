package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;

public class StoryPhotoLoader extends ImageWorker {

    private static final String TAG = StoryPhotoLoader.class.getSimpleName();

    public StoryPhotoLoader() {
        setCachingEnabled(false);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background thread.
     *
     * @param id The data to load the bitmap
     * @return The downloaded and resized bitmap
     */
    protected Bitmap processBitmap(String id, Object statusMessageObj) {
        if (statusMessageObj != null) {
            StatusMessage statusMessage = (StatusMessage) statusMessageObj;
            if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE) {
                return loadImage(id, false, statusMessage);
            } else {
                return loadImage(id, true, statusMessage);
            }
        } else {
            return loadImage(id, true, null);
        }
    }

    private Bitmap loadImage(String id, boolean loadingProfilePic, StatusMessage statusMessage) {
        Bitmap bitmap = null;

        String fileName = Utils.getProfileImageFileName(id);
        File orgFile = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT, fileName);

        if (!orgFile.exists()) {
            Bitmap cacheBmp = getFromCache(id, loadingProfilePic, statusMessage);
            if (cacheBmp != null)
                return cacheBmp;
        } else {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = HikeBitmapFactory.decodeFile(orgFile.getAbsolutePath(), options);
                Logger.d(TAG, id + " Compressed Bitmap size in KB: " + BitmapUtils.getBitmapSize(bitmap) / 1024);

                if (bitmap == null) {
                    return getFromCache(id, loadingProfilePic, statusMessage);
                }
            } catch (Exception e1) {
                Logger.e(TAG, "exception occured while loading photo", e1);
                e1.printStackTrace();
            }
        }
        return bitmap;
    }

    private Bitmap getFromCache(String id, boolean loadingProfilePic, StatusMessage statusMessage) {
        BitmapDrawable b = null;
        if (loadingProfilePic) {
            b = this.getLruCache().getIconFromCache(id);
        } else {
            if (!TextUtils.isEmpty(statusMessage.getFileKey())) {
                b = this.getLruCache().getFileIconFromCache(statusMessage.getFileKey());
            }
        }
        Logger.d(TAG, "Bitmap from icondb");
        if (b != null) {
            return b.getBitmap();
        } else {
            return null;
        }
    }

    @Override
    protected Bitmap processBitmapOnUiThread(String data) {
        return loadImage(data, true, null);
    }

    @Override
    protected Bitmap processBitmap(String data) {
        return loadImage(data, true, null);
    }
}
