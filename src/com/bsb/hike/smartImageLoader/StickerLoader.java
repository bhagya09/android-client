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

	public StickerLoader(Builder builder)
    {
        super();
        
        this.downloadLargeStickerIfNotFound = builder.downloadLargeStickerIfNotFound;
        this.lookForOfflineSticker = builder.lookForOfflineSticker;
        this.loadMiniStickerIfNotFound = builder.loadMiniStickerIfNotFound;
        this.stretchMini = builder.stretchMini;
        this.downloadMiniStickerIfNotFound = builder.downloadMiniStickerIfNotFound;

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

		}

        bitmap = loadStretchMiniBitmap(bitmap, width, height);

		return bitmap;
	}

	private Bitmap loadStretchMiniBitmap(Bitmap bitmap, int width, int height)
	{
		if (stretchMini && bitmap != null)
		{
			bitmap = HikePhotosUtils.compressBitamp(bitmap, width, height, true, Bitmap.Config.ARGB_8888);
		}

		return bitmap;
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

    public static class Builder
    {
        private boolean downloadLargeStickerIfNotFound = false;

        private boolean lookForOfflineSticker = false;

        private boolean loadMiniStickerIfNotFound = false;

        private boolean downloadMiniStickerIfNotFound = false;

        private boolean stretchMini = false;

        public Builder downloadLargeStickerIfNotFound(boolean state)
        {
            this.downloadLargeStickerIfNotFound = state;
            return this;
        }

        public Builder lookForOfflineSticker(boolean state)
        {
            this.lookForOfflineSticker = state;
            return this;
        }

        public Builder loadMiniStickerIfNotFound(boolean state)
        {
            this.loadMiniStickerIfNotFound = state;
            return this;
        }

        public Builder downloadMiniStickerIfNotFound(boolean state)
        {
            this.downloadMiniStickerIfNotFound = state;
            return this;
        }

        public Builder stretchMini(boolean state)
        {
            this.stretchMini = state;
            return this;
        }

        public StickerLoader build()
        {
            return new StickerLoader(this);
        }

    }


}