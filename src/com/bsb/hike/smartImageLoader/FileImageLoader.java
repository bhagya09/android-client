package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;

public class FileImageLoader extends ImageWorker
{
	public static final String FILE_KEY_PREFIX = "fil:";

	private int reqWidth;

	private int reqHeight;

	public FileImageLoader(int reqWidth, int reqHeight)
	{
		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		String filePath = new String(data.substring(FILE_KEY_PREFIX.length()));
		Bitmap b = HikeBitmapFactory.scaleDownBitmap(filePath, reqWidth, reqHeight, Bitmap.Config.RGB_565, true, false);
		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return null;
	}

}
