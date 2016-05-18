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
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images fetched from a URL.
 */
public class TimelineUpdatesImageLoader extends ImageWorker
{
	private static final String TAG = "TimelineUpdatesImageLoader";

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
	private TimelineUpdatesImageLoader(Context ctx, int imageWidth, int imageHeight)
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
	public TimelineUpdatesImageLoader(Context ctx, int imageSize)
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
	protected Bitmap processBitmap(String id, Object statusMessageObj)
	{
		if (statusMessageObj != null)
		{
			StatusMessage statusMessage = (StatusMessage) statusMessageObj;
			if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
			{
				return loadImage(id, false, statusMessage);
			}
			else
			{
				return loadImage(id, true, statusMessage);
			}
		}
		else
		{
			return loadImage(id, true, null);
		}
	}

	private Bitmap loadImage(String id, boolean loadingProfilePic, StatusMessage statusMessage)
	{
		Bitmap bitmap = null;
		String fileName = Utils.getProfileImageFileName(id);
		File orgFile =  new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT, fileName);

		if (!orgFile.exists())
		{
			Bitmap cacheBmp = getFromCache(id, loadingProfilePic, statusMessage);
			if(cacheBmp!=null)
				return cacheBmp;
		}
		else
		{
			try
			{
				bitmap = HikeBitmapFactory.scaleDownBitmap(orgFile.getPath(), mImageWidth, mImageHeight, Bitmap.Config.RGB_565, true, false);
				Logger.d(TAG, id + " Compressed Bitmap size in KB: " + BitmapUtils.getBitmapSize(bitmap) / 1024);
				
				if(bitmap == null)
				{
					return getFromCache(id, loadingProfilePic, statusMessage);
				}
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}
		return bitmap;
	}
	
	private Bitmap getFromCache(String id, boolean loadingProfilePic, StatusMessage statusMessage)
	{
		BitmapDrawable b = null;
		if (loadingProfilePic)
		{
			b = this.getLruCache().getIconFromCache(id);
		}
		else
		{
			if (!TextUtils.isEmpty(statusMessage.getFileKey()))
			{
				b = this.getLruCache().getFileIconFromCache(statusMessage.getFileKey());
			}
		}
		Logger.d(TAG, "Bitmap from icondb");
		if (b != null)
		{
			return b.getBitmap();
		}
		else
		{
			return null;
		}
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return loadImage(data, true, null);
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		return loadImage(data, true, null);
	}
}
