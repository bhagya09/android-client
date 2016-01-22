package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.cropimage.HikeCropFragment;
import com.bsb.hike.utils.Utils;

public class GalleryPagerImageLoader extends ImageWorker
{
	private BitmapFactory.Options options;

	void GalleryPagerImageLoader()
	{
		options = new BitmapFactory.Options();

		options.inScaled = false;

		options.inDither = true;

		options.inPreferQualityOverSpeed = true;
	}

	@Override
	public Bitmap processBitmap(String data)
	{
		Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromFile(data, (HikeConstants.HikePhotos.MAX_IMAGE_DIMEN),
				(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN), Bitmap.Config.RGB_565, options, true);
		Log.d("Atul", "COMPRESSED" + data);
		Log.d("Atul", "STARTING TO FIX ROTATION" + data);
		b = Utils.getRotatedBitmap(data,b);
		Log.d("Atul", "FIXED ROTATION" + data);
		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
