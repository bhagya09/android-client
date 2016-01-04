package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

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
		BitmapDrawable bd = this.getImageCache().get(data);
		if (bd != null)
			return bd.getBitmap();

		Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromFile(data, (HikeConstants.HikePhotos.MAX_IMAGE_DIMEN + HikeCropFragment.SIZE_MODIFIER),
				(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN + HikeCropFragment.SIZE_MODIFIER), Bitmap.Config.RGB_565, options, true);
		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
