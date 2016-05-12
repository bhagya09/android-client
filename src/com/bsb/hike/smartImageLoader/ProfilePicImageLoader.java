package com.bsb.hike.smartImageLoader;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ProfilePicImageLoader extends ImageWorker
{

	private static final String TAG = "ProfilePicImageLoader";

	private int mImageWidth;

	private int mImageHeight;

	private Context context;

	/**
	 * Initialize providing a target image width and height for the processing images.
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 */
	public ProfilePicImageLoader(Context ctx, int imageWidth, int imageHeight)
	{
		super();
		this.context = ctx;
		this.mImageWidth = imageWidth;
		this.mImageHeight = imageHeight;
	}

	/**
	 * Initialize providing a single target image size (used for both width and height);
	 * 
	 * @param context
	 * @param imageSize
	 */
	public ProfilePicImageLoader(Context ctx, int imageSize)
	{
		this(ctx, imageSize, imageSize);
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the AsyncTask background thread.
	 * 
	 * @param data
	 *            The data to load the bitmap
	 * @return The downloaded and resized bitmap
	 */
	@Override
	protected Bitmap processBitmap(String id)
	{
		Bitmap bitmap = null;
		String idd = id;
		int idx = id.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
		if (idx > 0)
			idd = id.substring(0, idx);

		String fileName = Utils.getProfileImageFileName(idd);

		File orgFile = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT, fileName);

		if (!orgFile.exists())
		{
			BitmapDrawable b = this.getLruCache().getIconFromCache(id);
			Logger.d(TAG, "Bitmap from icondb");
			if (b != null)
				return b.getBitmap();
		}
		else
		{
			try
			{
				bitmap = HikeBitmapFactory.scaleDownBitmap(orgFile.getPath(), mImageWidth, mImageHeight, Bitmap.Config.RGB_565, true, false);
				Logger.d(TAG, id + " Compressed Bitmap size in KB: " + BitmapUtils.getBitmapSize(bitmap) / 1024);
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}

		}
		return bitmap;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String id)
	{
		return processBitmap(id);
	}

}
