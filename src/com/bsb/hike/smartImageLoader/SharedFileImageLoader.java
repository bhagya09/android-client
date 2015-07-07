package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TextDrawable;

public class SharedFileImageLoader extends ImageWorker
{
	private int size_image;

	private Context context;

	public SharedFileImageLoader(Context context, int size_image)
	{
		this.context = context;
		this.size_image = size_image;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		BitmapDrawable bd = this.getImageCache().get(data);
		if (bd != null)
			return bd.getBitmap();
		
		String[] dataArray = data.split("::");
		String filePath = dataArray[0];
		HikeFileType hikeFileType = HikeFileType.values()[Integer.valueOf(dataArray[1])];
		
		Bitmap b = getSharedMediaThumbnailFromCache(filePath, size_image, (hikeFileType == HikeFileType.IMAGE));

		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}
	/**
	 * if isImage is True:- No Cache is used, always loads
	 * else 			 :- Cache is used 
	 * @param destFilePath
	 * @param imageSize
	 * @param isImage
	 * @return
	 */
	public Bitmap getSharedMediaThumbnailFromCache(String destFilePath, int imageSize, boolean isImage)
	{
		Bitmap thumbnail = null;
		if (isImage)
		{
			Log.d("image_config", "========================== \n Inside API  getSharedMediaThumbnailFromCache");
			if(Utils.isHoneycombOrHigher())
			{
				thumbnail = HikeBitmapFactory.getImageThumbnailAsPerAlgo(context, destFilePath, imageSize, HikeBitmapFactory.AlgoState.STATE_3);
			}
			else
			{
				thumbnail = HikeBitmapFactory.getImageThumbnailAsPerAlgo(context, destFilePath, imageSize, HikeBitmapFactory.AlgoState.STATE_3);
			}
			thumbnail = Utils.getRotatedBitmap(destFilePath, thumbnail);
		}
		else
		// Video
		{
			thumbnail = ThumbnailUtils.createVideoThumbnail(destFilePath, MediaStore.Images.Thumbnails.MICRO_KIND);
		}

		if (thumbnail == null)
		{
			return null;
		}
		return thumbnail;
	}

}
