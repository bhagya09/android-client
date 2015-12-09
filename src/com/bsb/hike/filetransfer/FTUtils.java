package com.bsb.hike.filetransfer;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.utils.Utils;

public class FTUtils {

	private static final int IMAGE_THUMB_COMPRESS_FACTOR = 25;
	private static final int OTHER_THUMB_COMPRESS_FACTOR = 75;

	/**
	 * Get thumbnail for the given media
	 * 
	 * @param hikeFileType
	 * @param destinationFile
	 * @return fileKey
	 */
	public static Bitmap getMediaThumbnail(HikeFileType hikeFileType, File destinationFile, String fileKey)
	{
		Bitmap thumbnail = null;
		if (hikeFileType == HikeFileType.IMAGE)
		{
			thumbnail = FTUtils.createImageThumb(destinationFile, fileKey);
		}
		else if (hikeFileType == HikeFileType.VIDEO)
		{
			thumbnail = FTUtils.createVideoThumb(destinationFile, fileKey);
		}
		return thumbnail;
	}
	/**
	 * Creates thumb for image
	 * 
	 * @param destFile
	 * @param fileKey
	 * @return Bitmap
	 */
	public static Bitmap createImageThumb(File destFile, String fileKey)
	{
		Bitmap thumbnail = null;
		if (!TextUtils.isEmpty(fileKey))
		{
			BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
			if (bd != null)
				thumbnail = ImageWorker.drawableToBitmap(bd);
		}

		if(thumbnail == null)
		{
			Bitmap.Config config = Bitmap.Config.RGB_565;
			if(Utils.hasJellyBeanMR1()){
				config = Bitmap.Config.ARGB_8888;
			}
			thumbnail = HikeBitmapFactory.scaleDownBitmap(destFile.getPath(), HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
					config, false, false);
			thumbnail = Utils.getRotatedBitmap(destFile.getPath(), thumbnail);
		}
		return thumbnail;
	}

	/**
	 * Creates video thumb
	 * 
	 * @param destFile
	 * @param fileKey
	 * @return Bitmap
	 */
	public static Bitmap createVideoThumb(File destFile, String fileKey)
	{
		Bitmap thumbnail = null;
		if (!TextUtils.isEmpty(fileKey))
		{
			BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
			if (bd != null)
				thumbnail = ImageWorker.drawableToBitmap(bd);
		}
		if(thumbnail == null)
		{
			thumbnail = ThumbnailUtils.createVideoThumbnail(destFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
		}
		return thumbnail;
	}

	/**
	 * Compresses thumb
	 * 
	 * @param thumbnail
	 * @param hikeFileType
	 * @return byte[]
	 */
	public static byte [] compressThumb(Bitmap thumbnail, HikeFileType hikeFileType)
	{
		int compressQuality;
		if (hikeFileType == HikeFileType.IMAGE)
		{
			compressQuality = IMAGE_THUMB_COMPRESS_FACTOR;
		}else{
			compressQuality = OTHER_THUMB_COMPRESS_FACTOR;
		}
		return BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
	}

	/**
	 * Returns image quality for image compression
	 * 
	 * @return String
	 */
	public static String getImageQuality(){
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance().getApplicationContext());
		int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
		String imageQuality = ImageQuality.IMAGE_QUALITY_DEFAULT;
		switch (quality)
		{
		case ImageQuality.QUALITY_ORIGINAL:
			imageQuality = ImageQuality.IMAGE_QUALITY_ORIGINAL;
			break;
		case ImageQuality.QUALITY_MEDIUM:
			imageQuality = ImageQuality.IMAGE_QUALITY_MEDIUM;
			break;
		case ImageQuality.QUALITY_SMALL:
			imageQuality = ImageQuality.IMAGE_QUALITY_SMALL;
			break;
		}
		return imageQuality;
	}
	/**
	 * Fetches the type of internet connection the device is using
	 *
	 * @param context
	 * @return
	 */
	public static FileTransferManager.NetworkType getNetworkType(Context context)
	{
		int networkType = Utils.getNetworkType(context);
		switch (networkType)
		{
		case -1:
			return FileTransferManager.NetworkType.NO_NETWORK;
		case 0:
			return FileTransferManager.NetworkType.TWO_G;
		case 1:
			return FileTransferManager.NetworkType.WIFI;
		case 2:
			return FileTransferManager.NetworkType.TWO_G;
		case 3:
			return FileTransferManager.NetworkType.THREE_G;
		case 4:
			return FileTransferManager.NetworkType.FOUR_G;
		default:
			return FileTransferManager.NetworkType.TWO_G;
		}
	}
}
