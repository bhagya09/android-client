package com.bsb.hike.smartImageLoader;

import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;

public class SocialIconLoader extends ImageWorker
{

	private static final String TAG = "SocialIconLoader";

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
	public SocialIconLoader(Context ctx, int imageWidth, int imageHeight)
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
	public SocialIconLoader(Context ctx, int imageSize)
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
	protected Bitmap processBitmap(String url)
	{
		try
		{
			Bitmap bitmap = HikeBitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
			return bitmap;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
