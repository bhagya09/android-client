package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.StickerManager;

public class StickerOtherIconLoader extends ImageWorker
{
	private Context ctx;
	private boolean downloadIfNotFound;
	private int width = -1;
	private int height = -1;
	
	/**
	 * 
	 * @param ctx
	 * @param downloadIfNotFound -- true if preview image should be downloaded if not found
	 */
	public StickerOtherIconLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
	}
	
	public void setImageSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{	
		String[] args = data.split(HikeConstants.DELIMETER);
		String categoryId = args[0];
		int type = Integer.valueOf(args[1]);
		Bitmap bmp = StickerManager.getInstance().getCategoryOtherAsset(ctx, categoryId, type, width, height, downloadIfNotFound);
		return bmp;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return processBitmap(data);
	}

}
