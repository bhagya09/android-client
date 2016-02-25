package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.diskcache.response.CacheResponse;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.StickerManager;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";

	private boolean lookForOfflineSticker;

	private boolean loadMiniStickerIfNotFound;

	private boolean downloadMiniStickerIfNotFound;

	private boolean downloadLargeStickerIfNotFound;

	public StickerLoader(boolean loadMiniStickerIfNotFound, boolean downloadMiniStickerIfNotFound, boolean downloadLargeStickerIfNotFound)
	{
		super();
		this.loadMiniStickerIfNotFound = loadMiniStickerIfNotFound;
		this.downloadMiniStickerIfNotFound = downloadMiniStickerIfNotFound;
		this.downloadLargeStickerIfNotFound = downloadLargeStickerIfNotFound;
		mResources = HikeMessengerApp.getInstance().getResources();
	}

	public StickerLoader(boolean lookForOfflineSticker, boolean loadMiniStickerIfNotFound, boolean downloadMiniStickerIfNotFound, boolean downloadLargeStickerIfNotFound)
	{
		super();
		this.lookForOfflineSticker = lookForOfflineSticker;
		this.loadMiniStickerIfNotFound = loadMiniStickerIfNotFound;
		this.downloadMiniStickerIfNotFound = downloadMiniStickerIfNotFound;
		this.downloadLargeStickerIfNotFound = downloadLargeStickerIfNotFound;
		mResources = HikeMessengerApp.getInstance().getResources();
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		String[] args = data.split(HikeConstants.DELIMETER);
		Sticker sticker = new Sticker(args[0], args[1]);
		String path = args[2];
		Bitmap bitmap;

		if(path.contains("mini"))
		{
			bitmap = loadMiniSticker(path);
			checkAndDownloadMiniSticker(bitmap, sticker);
		}
		else
		{
			bitmap = loadSticker(path);
			checkAndDownloadLargeSticker(bitmap, sticker);
			bitmap = checkAndLoadOfflineSticker(bitmap, sticker);
			bitmap = checkAndLoadMiniSticker(bitmap, sticker);
		}
		return bitmap;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

	private Bitmap loadSticker(String path)
	{
		return HikeBitmapFactory.decodeFile(path);
	}

	private Bitmap loadMiniSticker(String key)
	{
		CacheResponse cacheResponse = HikeMessengerApp.getDiskCache().get(key);
		if(cacheResponse != null)
		{
			return HikeBitmapFactory.decodeStream(cacheResponse.getInputStream());
		}
		return null;
	}


	private Bitmap checkAndLoadOfflineSticker(Bitmap bitmap, Sticker sticker)
	{
		if(bitmap == null && lookForOfflineSticker)
		{
			return loadSticker(OfflineUtils.getOfflineStkPath(sticker.getStickerId(), sticker.getCategoryId()));
		}
		return null;
	}

	private Bitmap checkAndLoadMiniSticker(Bitmap bitmap, Sticker sticker)
	{
		if(bitmap == null && loadMiniStickerIfNotFound)
		{
			bitmap = loadMiniSticker(sticker.getMiniStickerPath());
			checkAndDownloadMiniSticker(bitmap, sticker);
		}
		return  bitmap;
	}

	private void checkAndDownloadMiniSticker(Bitmap bitmap, Sticker sticker)
	{
		if(bitmap == null && downloadMiniStickerIfNotFound)
		{
			StickerManager.getInstance().initiateMiniStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId());
		}
	}

	private void checkAndDownloadLargeSticker(Bitmap bitmap, Sticker sticker)
	{
		if(bitmap == null && downloadLargeStickerIfNotFound)
		{
			StickerManager.getInstance().initiateSingleStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId(), null);
		}
	}

}