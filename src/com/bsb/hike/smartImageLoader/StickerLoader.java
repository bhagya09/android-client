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
import com.bsb.hike.models.utils.Size;

public class StickerLoader extends ImageWorker
{
	private static final String TAG = "StickerLoader";

	private boolean lookForOfflineSticker;

	private boolean loadMiniStickerIfNotFound;

	private boolean downloadMiniStickerIfNotFound;

	private boolean downloadLargeStickerIfNotFound;

	private boolean stretchMini;
    
	private Bitmap defaultBitmap;

    private Size stickerSize;

	private StickerLoader(Builder builder)
    {
        super();
        
        this.downloadLargeStickerIfNotFound = builder.downloadLargeStickerIfNotFound;
        this.lookForOfflineSticker = builder.lookForOfflineSticker;
        this.loadMiniStickerIfNotFound = builder.loadMiniStickerIfNotFound;
        this.stretchMini = builder.stretchMini;
        this.downloadMiniStickerIfNotFound = builder.downloadMiniStickerIfNotFound;
        this.defaultBitmap = builder.defaultBitmap;
        this.stickerSize = builder.stickerSize;
    }

	@Override
	protected Bitmap processBitmap(String data)
	{
		String[] args = data.split(HikeConstants.DELIMETER);
		Sticker sticker = new Sticker(args[0], args[1]);
		String path = args[2];
		Bitmap bitmap;

		Size loadSize = (stickerSize == null) ? new Size(sticker.getWidth(), sticker.getHeight()) : stickerSize;

		Bitmap large = loadStickerBitmap(path);
		bitmap = checkAndLoadOfflineSticker(large, sticker);
		bitmap = checkAndLoadMiniSticker(bitmap, sticker, loadSize);
		checkAndDownloadLargeSticker(large, sticker);

        bitmap = checkAndLoadDefaultBitmap(bitmap);

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

	public Bitmap checkAndLoadDefaultBitmap(Bitmap bitmap)
	{
		if (bitmap == null && defaultBitmap != null)
		{
			return defaultBitmap;
		}

		return bitmap;
	}

    private Bitmap loadMiniStickerBitmap(String path,Size size)
	{
		CacheResponse cacheResponse = HikeMessengerApp.getDiskCache().get(path);

		Bitmap bitmap = null;

		if (cacheResponse != null)
		{

			bitmap = HikeBitmapFactory.decodeStream(cacheResponse.getInputStream());

		}

        bitmap = loadStretchedMiniStickerBitmap(bitmap,size);

		return bitmap;
	}

    private Bitmap loadStretchedMiniStickerBitmap(Bitmap bitmap, Size size)
    {
        if (stretchMini)
        {
            bitmap = stretchStickerBitmap(bitmap, size);
        }

        return bitmap;
    }

	private Bitmap stretchStickerBitmap(Bitmap bitmap, Size size)
	{
		if (bitmap != null)
		{
			bitmap = HikePhotosUtils.compressBitamp(bitmap, size.getWidth(),size.getHeight(), true, Bitmap.Config.ARGB_8888);
		}

		return bitmap;
	}

	private Bitmap checkAndLoadOfflineSticker(Bitmap bitmap, Sticker sticker)
	{
		if (lookForOfflineSticker && bitmap == null)
		{
			bitmap = loadStickerBitmap(OfflineUtils.getOfflineStkPath(sticker.getCategoryId(), sticker.getStickerId()));
			bitmap = stretchStickerBitmap(bitmap, new Size(sticker.getWidth(), sticker.getHeight()));
		}
		return bitmap;
	}

	private Bitmap checkAndLoadMiniSticker(Bitmap bitmap, Sticker sticker, Size loadSize)
	{
		if (loadMiniStickerIfNotFound && bitmap == null)
		{
			bitmap = loadMiniStickerBitmap(sticker.getMiniStickerPath(), loadSize);
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

		private Bitmap defaultBitmap = null;

        private Size stickerSize = null;

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

        public Builder setDefaultBitmap(Bitmap defaultBitmap)
        {
            this.defaultBitmap = defaultBitmap;
            return this;
        }

        public Builder stretchMini(boolean state)
        {
            this.stretchMini = state;
            return this;
        }

        public Builder setStickerDimension(Size stickerSize)
        {
            this.stickerSize = stickerSize;
            return this;
        }

        public StickerLoader build()
        {
            return new StickerLoader(this);
        }

    }


}