package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";
	
	private Context ctx;
	
	private boolean downloadIfNotFound,lookInDiskCache,loadFullSticker;

	public StickerLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
		mResources = ctx.getResources();

	}

	public StickerLoader(Context ctx, boolean downloadIfNotFound,boolean lookInDiskCache)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
		mResources = ctx.getResources();
		this.lookInDiskCache = lookInDiskCache;
	}

	public void laodLargeSticker(boolean loadFullSticker)
	{
		this.loadFullSticker = loadFullSticker;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{


		if (data.contains("res:"))
		{
			int id = Integer.parseInt(data.substring(data.indexOf(":") + 1));
			return HikeBitmapFactory.decodeResource(mResources, id);
		}

		Bitmap bitmap = null;

		Sticker sticker = new Sticker(data);

		if(sticker.isFullStickerAvailable())
		{
			bitmap = HikeBitmapFactory.decodeFile(loadFullSticker?sticker.getLargeStickerPath():sticker.getSmallStickerPath());
		}

		else if(lookInDiskCache && StickerManager.getInstance().isMiniStickersEnabled())
		{
			bitmap =  HikeBitmapFactory.getMiniStickerBitmap(sticker.getMiniStickerPath());
		}

		if(bitmap == null)
		{
			bitmap = checkAndDownload(bitmap, sticker);
			return bitmap;
		}
		else
		{
			return bitmap;
		}
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private Bitmap checkAndDownload(Bitmap bitmap, Sticker sticker)
	{

		if(bitmap != null)
		{
			return bitmap;
		}

		try
		{
			if((bitmap == null) && downloadIfNotFound && sticker != null)
			{
				if( StickerManager.getInstance().isMiniStickersEnabled())
				{
					StickerManager.getInstance().initiateMiniStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId());
				}
				else
				{
					StickerManager.getInstance().initialiseSingleStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId(), null);
				}

			}
		}
		catch(Exception e)
		{
			//Safety catch as don't want to hamper existing experience
			Logger.e(TAG, "exception in downloading sticker from loader : ", e);
		}

		return null;
	}
}
