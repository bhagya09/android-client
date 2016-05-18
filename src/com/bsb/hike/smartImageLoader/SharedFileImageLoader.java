package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.BitmapModule.HikeBitmapFactory.BitmapResolutionState;
import com.bsb.hike.BitmapModule.HikeBitmapFactory.Dimension;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class SharedFileImageLoader extends ImageWorker
{
	private int size_image;

	private Context context;
	
	private boolean isThumbnail;
	
	public SharedFileImageLoader(Context context, int size_image)
	{
		this(context,size_image,true);
	}

	public SharedFileImageLoader(Context context,int size_image, boolean isThumbnail)
	{
		this.context = context;
		this.size_image = size_image;
		this.isThumbnail = isThumbnail;
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
		
		Bitmap b = null;
		
		if(hikeFileType == HikeFileType.IMAGE && !isThumbnail())
		{
			b = getSharedMediaImageFromCache(filePath,new Dimension(size_image,size_image));
		}
		else
		{
			b = getSharedMediaThumbnailFromCache(filePath, (hikeFileType == HikeFileType.IMAGE));
		}

		return b;
	}
	
	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}
	
	public boolean isThumbnail() 
	{
		return isThumbnail;
	}

	public void setThumbnail(boolean isThumbnail) 
	{
		this.isThumbnail = isThumbnail;
	}

	/**
	 * if isImage is True:- No Cache is used, always loads
	 * else 			 :- Cache is used 
	 * @param destFilePath
	 * @param imageSize
	 * @param isImage
	 * @return
	 */
	private Bitmap getSharedMediaThumbnailFromCache(String destFilePath, boolean isImage)
	{
		Bitmap thumbnail = null;
		if (isImage)
		{
			thumbnail = HikeBitmapFactory.scaleDownBitmap(destFilePath, size_image, size_image, Bitmap.Config.RGB_565, true, false);
			thumbnail = Utils.getRotatedBitmap(destFilePath, thumbnail);
		}
		else
		// Video
		{
			thumbnail = ThumbnailUtils.createVideoThumbnail(destFilePath, MediaStore.Images.Thumbnails.MICRO_KIND);
		}

		return thumbnail;
	}
	
	private Bitmap getSharedMediaImageFromCache(String destFilePath,Dimension defaultSize)
	{
		Bitmap thumbnail = null;
		Log.d("image_config", "========================== \n Inside API  getSharedMediaThumbnailFromCache");
		
		BitmapResolutionState initState = BitmapResolutionState.STATE_3;

		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_HIGH_RES_IMAGE, true))
		{
			initState = BitmapResolutionState.INIT_STATE;
		}
		
		thumbnail = HikeBitmapFactory.getBestResolutionBitmap(context, destFilePath, defaultSize,initState);
		thumbnail = Utils.getRotatedBitmap(destFilePath, thumbnail);
		
		return thumbnail;
	}

}
