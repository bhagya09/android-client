package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;

public class FTUECardImageLoader extends ImageWorker
{
	public static String FTUE_CARD_IMAGE_PREFIX = "ftue:";

	@Override
	protected Bitmap processBitmap(String data)
	{
		if (data.startsWith(FTUE_CARD_IMAGE_PREFIX))
		{
			int id = Integer.parseInt(data.substring(data.indexOf(":") + 1));
			return HikeBitmapFactory.decodeResource(getResources(), id);
		}
		return null;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
