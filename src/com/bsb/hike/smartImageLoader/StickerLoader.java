package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import java.io.File;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";
	
	private Context ctx;
	
	private boolean downloadIfNotFound;

	public StickerLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
		mResources = ctx.getResources();
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		if (data.contains("res:"))
		{
			int id = Integer.parseInt(data.substring(data.indexOf(":") + 1));
			return HikeBitmapFactory.decodeResource(mResources, id);
		}
		else
		{
			Bitmap bitmap = HikeBitmapFactory.decodeFile(data);
			checkAndDownload(bitmap, data);
			return bitmap;
		}
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private void checkAndDownload(Bitmap bitmap, String data)
	{
		try
		{
			if((bitmap == null) && downloadIfNotFound && !TextUtils.isEmpty(data))
			{
				String[] args = data.split(File.separator);
				int length = args.length;

				String stickerId = args[length - 1];
				String categoryId = args[length - 3];

				StickerManager.getInstance().initialiseSingleStickerDownloadTask(stickerId, categoryId, null);
			}
		}
		catch(Exception e)
		{
			//Safety catch as don't want to hamper existing experience
			Logger.e(TAG, "exception in downloading sticker from loader : ", e);
		}
	}
}
