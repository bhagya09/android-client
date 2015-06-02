package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils;

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
	
	public Bitmap getSharedMediaThumbnailFromCache(String destFilePath, int imageSize, boolean isImage)
	{
		Bitmap thumbnail = null;
		if (isImage)
		{
			Log.d("image_config", "========================== \n Inside API  getSharedMediaThumbnailFromCache");
			thumbnail = HikeBitmapFactory.getImageThumbnailAsPerAlgo(context, destFilePath, imageSize, HikeBitmapFactory.AlgoState.INIT_STATE.getValue());
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
