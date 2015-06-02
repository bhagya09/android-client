/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bsb.hike.smartImageLoader;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images fetched from a URL.
 */
public class TimelineImageLoader extends ImageWorker
{
	private static final String TAG = "TimelineImageLoader";

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
	public TimelineImageLoader(Context ctx, int imageWidth, int imageHeight)
	{
		super();
		this.context = ctx;
		this.mImageWidth = imageWidth;
		this.mImageHeight = imageHeight;
		mResources = this.context.getResources();
	}

	/**
	 * Initialize providing a single target image size (used for both width and height);
	 * 
	 * @param context
	 * @param imageSize
	 */
	public TimelineImageLoader(Context ctx, int imageSize)
	{
		this(ctx, imageSize, imageSize);
	}

	public void setImageSize(int width, int height)
	{
		mImageWidth = width;
		mImageHeight = height;
	}

	/**
	 * Set the target image size (width and height will be the same).
	 * 
	 * @param size
	 */
	public void setImageSize(int size)
	{
		setImageSize(size, size);
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the AsyncTask background thread.
	 * 
	 * @param data
	 *            The data to load the bitmap
	 * @return The downloaded and resized bitmap
	 */
	protected Bitmap processBitmap(String id)
	{
		Bitmap bitmap = null;
		String fileName = Utils.getProfileImageFileName(id);
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
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
