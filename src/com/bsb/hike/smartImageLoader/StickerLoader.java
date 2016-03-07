package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.diskcache.response.CacheResponse;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.utils.StickerManager;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";

	private boolean lookForOfflineSticker;

	private boolean loadMiniStickerIfNotFound;

	private boolean downloadMiniStickerIfNotFound;

	private boolean downloadLargeStickerIfNotFound;

	private boolean stretchMini;

	public StickerLoader(boolean downloadLargeStickerIfNotFound)
	{
		super();
		this.downloadLargeStickerIfNotFound = downloadLargeStickerIfNotFound;
		mResources = HikeMessengerApp.getInstance().getResources();
	}

	public StickerLoader(boolean loadMiniStickerIfNotFound, boolean downloadMiniStickerIfNotFound, boolean downloadLargeStickerIfNotFound)
	{
		super();
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

		if (path.startsWith(HikeConstants.MINI_KEY_PREFIX))
		{
			bitmap = loadMiniStickerBitmap(sticker.getMiniStickerPath(), sticker.getWidth(), sticker.getHeight());
			checkAndDownloadMiniSticker(bitmap, sticker);
		}
		else
		{
			bitmap = loadStickerBitmap(path);
            checkAndDownloadLargeSticker(bitmap, sticker);
			bitmap = checkAndLoadOfflineSticker(bitmap, sticker);
			bitmap = checkAndLoadMiniSticker(bitmap, sticker);
		}
		return bitmap;
	}

	public void loadSticker(Sticker sticker, StickerConstants.StickerType stickerType, ImageView imageView)
	{
		loadSticker(sticker, stickerType, imageView, false);
	}

	public void loadSticker(Sticker sticker, StickerConstants.StickerType stickerType, ImageView imageView, boolean isFlinging)
	{
		loadSticker(sticker, stickerType, imageView, isFlinging, false);
	}

	public void loadSticker(Sticker sticker, StickerConstants.StickerType stickerType, ImageView imageView, boolean isFlinging, boolean runOnUiThread)
	{
		String path = StickerManager.getInstance().getStickerCacheKey(sticker, stickerType);

		loadImage(path, imageView, isFlinging, runOnUiThread);
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

	private Bitmap loadStickerBitmap(String path)
	{
		return HikeBitmapFactory.decodeFile(path);
	}

	private Bitmap loadMiniStickerBitmap(String path, int width, int height)
	{
		CacheResponse cacheResponse = HikeMessengerApp.getDiskCache().get(path);

		Bitmap bitmap = null;

		if (cacheResponse != null)
		{

			bitmap = HikeBitmapFactory.decodeStream(cacheResponse.getInputStream());

			if (bitmap != null && stretchMini)
			{
				bitmap = stretchMiniBitmap(bitmap, width, height);
			}
		}

		return bitmap;
	}

    private Bitmap stretchMiniBitmap(Bitmap bitmap,int width,int height)
    {
        return HikePhotosUtils.compressBitamp(bitmap, width, height, true, Bitmap.Config.ARGB_8888);
    }

	private Bitmap checkAndLoadOfflineSticker(Bitmap bitmap, Sticker sticker)
	{
		if (lookForOfflineSticker && bitmap == null)
		{
			return loadStickerBitmap(OfflineUtils.getOfflineStkPath(sticker.getStickerId(), sticker.getCategoryId()));
		}
		return bitmap;
	}

	private Bitmap checkAndLoadMiniSticker(Bitmap bitmap, Sticker sticker)
	{
		if (loadMiniStickerIfNotFound && bitmap == null)
		{
			bitmap = loadMiniStickerBitmap(sticker.getMiniStickerPath(), sticker.getWidth(), sticker.getHeight());
			checkAndDownloadMiniSticker(bitmap, sticker);
		}
		return bitmap;
	}

	private void checkAndDownloadMiniSticker(Bitmap bitmap, Sticker sticker)
	{
		if (downloadMiniStickerIfNotFound && bitmap == null)
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
	public void setStretchMini(boolean stretchMini)
	{
		this.stretchMini = stretchMini;
	}

	public void setLookForOfflineSticker(boolean lookForOfflineSticker)
	{
		this.lookForOfflineSticker = lookForOfflineSticker;
	}

}