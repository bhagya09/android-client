package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.modules.stickerdownloadmgr.MiniStickerImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerDownloadTask;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.utils.Logger;

import java.io.File;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";
	
	private Context ctx;
	
	private boolean downloadIfNotFound,lookInDiskCache;

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

	@Override
	protected Bitmap processBitmap(String data)
	{
		if (data.contains("res:"))
		{
			int id = Integer.parseInt(data.substring(data.indexOf(":") + 1));
			return HikeBitmapFactory.decodeResource(mResources, id);
		}
		else if(lookInDiskCache && data.contains(StickerSearchConstants.MINI_STICKER_KEY_CODE))
		{
			return HikeBitmapFactory.getMiniStickerBitmap(data);
		}
		else
		{
			Bitmap bitmap = HikeBitmapFactory.decodeFile(data);
			bitmap = checkAndDownload(bitmap, data);
			return bitmap;
		}
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private Bitmap checkAndDownload(Bitmap bitmap, String dataKey)
	{

		if(bitmap != null)
		{
			return bitmap;
		}

		try
		{
			if((bitmap == null) && downloadIfNotFound && !TextUtils.isEmpty(dataKey))
			{
				boolean isMini =  dataKey.contains(StickerSearchConstants.MINI_STICKER_KEY_CODE);

				String data = isMini?dataKey.substring(dataKey.indexOf("_") + 1):dataKey;

				String[] args = null;
				int length =0;
				String stickerId = null;
				String categoryId = null;
				if(isMini)
				{
					args = data.split(":");
					length = args.length;
					stickerId = args[length - 1];
					categoryId = args[length - 2];
				}
				else
				{
					args = data.split(File.separator);
					length = args.length;
					stickerId = args[length - 1];
					categoryId = args[length - 3];
				}


				if(isMini)
				{
					MiniStickerImageDownloadTask miniStickerImageDownloadTask = new MiniStickerImageDownloadTask(stickerId,categoryId);
					miniStickerImageDownloadTask.execute();
				}
				else
				{
					SingleStickerDownloadTask singleStickerDownloadTask = new SingleStickerDownloadTask(stickerId, categoryId, null);
					singleStickerDownloadTask.execute();
				}

			}
		}
		catch(Exception e)
		{
			//Safety catch as don't want to hamper existing experience
			Logger.e(TAG, "exception in downloading sticker from loader : ", e);
		}

		return bitmap;
	}
}
