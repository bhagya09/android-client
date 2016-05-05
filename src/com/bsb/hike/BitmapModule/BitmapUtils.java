package com.bsb.hike.BitmapModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class BitmapUtils {

    public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format) {
        return bitmapToBytes(bitmap, format, 50);
    }

    public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        if (bitmap == null) {
            byte[] b = new byte[]{0};
            return b;
        }
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(format, quality, bao);
        return bao.toByteArray();
    }

    public static int iconHash(String s) {
        /*
		 * ignore everything after :: so that your large icon by default matches your msisdn
		 */
        s = s.split("::")[0];
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            count += s.charAt(i);
        }

        return count;
    }

    public static int getDefaultAvatarResourceId(String msisdn, boolean rounded) {
        int count = 5;
        int id;
        switch (iconHash(msisdn) % count) {
            case 0:
                id = rounded ? R.drawable.avatar_bubblegum_rounded : R.drawable.avatar_bubblegum;
                break;
            case 1:
                id = rounded ? R.drawable.avatar_apricot_rounded : R.drawable.avatar_apricot;
                break;
            case 2:
                id = rounded ? R.drawable.avatar_carnation_rounded : R.drawable.avatar_carnation;
                break;
            case 3:
                id = rounded ? R.drawable.avatar_light_gold_rounded : R.drawable.avatar_light_gold;
                break;
            case 4:
                id = rounded ? R.drawable.avatar_sky_blue_rounded : R.drawable.avatar_sky_blue;
                break;
            default:
                id = rounded ? R.drawable.avatar_bubblegum_rounded : R.drawable.avatar_bubblegum;
                break;
        }

        return id;
    }

    public static void saveBitmapToFileAndRecycle(String fileDir, String fileName, Bitmap bitmap) throws IOException {
        File file = new File(fileDir, fileName);
        saveBitmapToFile(file, bitmap, CompressFormat.PNG, 70);
        bitmap.recycle();
    }

    public static void saveBitmapToFile(File file, Bitmap bitmap) throws IOException {
        saveBitmapToFile(file, bitmap, CompressFormat.PNG, 70);
    }

    public static void saveBitmapToFile(File file, Bitmap bitmap, CompressFormat compressFormat, int quality) throws IOException {
        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(file);

            byte[] b = BitmapUtils.bitmapToBytes(bitmap, compressFormat, quality);
            if (b == null) {
                throw new IOException();
            }

            fos.write(b);
            fos.flush();
            fos.getFD().sync();
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    /**
     * Get the size in bytes of a bitmap in a BitmapDrawable. Note that from Android 4.4 (KitKat) onward this returns the allocated memory size of the bitmap which can be larger
     * than the actual bitmap data byte count (in the case it was re-used).
     *
     * @param value
     * @return size in bytes
     */
    public static int getBitmapSize(BitmapDrawable bd) {
        if (bd == null)
            return 0;

        return getBitmapSize(bd.getBitmap());
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (bitmap == null)
            return 0;
        // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
        // larger than bitmap byte count.
        if (Utils.hasKitKat()) {
            return bitmap.getAllocationByteCount();
        }
        return bitmap.getByteCount();
    }
}
