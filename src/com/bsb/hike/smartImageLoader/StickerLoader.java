package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.modules.diskcache.Cache;
import com.bsb.hike.modules.stickerdownloadmgr.MiniStickerImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerDownloadTask;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;

import java.io.File;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";
	
	private Context ctx;
	
	private boolean downloadIfNotFound,lookInDiskCache;

	private int downloadStickerType;

	Cache stickerDiskCache;

	public StickerLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
		mResources = ctx.getResources();

	}

	public StickerLoader(Context ctx, boolean downloadIfNotFound,boolean lookInDiskCache,int downloadStickerType)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
		mResources = ctx.getResources();
		if(lookInDiskCache)
		{
			this.lookInDiskCache = lookInDiskCache;
			//To Do discuss parameters Akt Anubhav
			stickerDiskCache = new Cache(new File("test"),0);
		}

		this.downloadStickerType = downloadStickerType;
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

	private Bitmap checkAndDownload(Bitmap bitmap, String data)
	{

		if(bitmap != null)
		{
			return bitmap;
		}

		try
		{

			int stickerSize = StickerSearchUtils.getStickerSize();

			if(bitmap == null && lookInDiskCache && !TextUtils.isEmpty(data))
			{
				bitmap = HikeBitmapFactory.decodeSampledBitmapFromByteArray(stickerDiskCache.getCache().get(data).getData(),stickerSize,stickerSize);
			}
			else if((bitmap == null) && downloadIfNotFound && !TextUtils.isEmpty(data))
			{
				String[] args = data.split(File.separator);
				int length = args.length;

				String stickerId = args[length - 1];
				String categoryId = args[length - 3];

				if(downloadStickerType == HikeStickerSearchBaseConstants.MINI_STICKER_AVAILABLE_ONLY)
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
