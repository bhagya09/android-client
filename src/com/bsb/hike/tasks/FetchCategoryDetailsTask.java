package com.bsb.hike.tasks;

import android.os.AsyncTask;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.stickerdownloadmgr.StickerCategoryDownloadTask;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class FetchCategoryDetailsTask extends AsyncTask<Void, Void, StickerCategory> implements IHikeHttpTaskResult
{

	private String catId;
	
	public FetchCategoryDetailsTask(String catId)
	{
		this.catId = catId;
	}
	
	@Override
	protected StickerCategory doInBackground(Void... params)
	{
		return HikeConversationsDatabase.getInstance().getStickerCategoryforId(catId);
	}
	
	@Override
	protected void onPostExecute(StickerCategory stickerCategory)
	{
		super.onPostExecute(stickerCategory);
		
		if (stickerCategory == null || Utils.isEmpty(stickerCategory.getAllStickers())
				|| (stickerCategory.getPreviewUpdationTime() < (System.currentTimeMillis() - HikeConstants.ONE_DAY_MILLS)))
		{
			StickerManager.getInstance().initialiseCategoryDetailsTask(catId);
		}
		else
		{
			doOnSuccess(stickerCategory);
		}
	}
	
	@Override
	public void doOnSuccess(Object result)
	{
		StickerCategory stickerCategory = (StickerCategory) result;
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_SUCCESS, stickerCategory);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_FAILURE, catId);
	}
	
}
