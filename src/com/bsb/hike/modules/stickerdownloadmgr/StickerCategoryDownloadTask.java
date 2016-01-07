package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.stickerCategoryDetailsDownloadRequest;

public class StickerCategoryDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static final String TAG = "StickerCatgeoryDownloadTask";
	private String categoryId;
	
	private RequestToken token;
	
	public StickerCategoryDownloadTask(String categoryId)
	{
		this.categoryId = categoryId;
	}

	@Override
	public void execute()
	{
		String requestId = getRequestId(); // for duplicate check

		token = stickerCategoryDetailsDownloadRequest(requestId, categoryId, getRequestListener());

		if (token.isRequestRunning()) // return if request is running
		{
			return;
		}
		token.execute();
	}

	@Override
	public void cancel()
	{
		if (null != token)
		{
			token.cancel();
		}
	}
	
	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			
			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{	
					JSONObject response = (JSONObject) result.getBody().getContent();
					
					Logger.d(TAG, "response :" + response.toString());
					
					if(!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						doOnFailure(null);
						return ;
					}
					
					Logger.d(TAG,  "Got response for download : " + response.toString());
					
					JSONObject resultData = response.optJSONObject(HikeConstants.DATA_2);
					if(null == resultData)
					{
						Logger.e(TAG, "Sticker download failed null data");
						doOnFailure(null);
						return;
					}
					
					doOnSuccess(resultData);
				}
				catch (Exception e)
				{
					doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
				}
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
				
			}
			
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				doOnFailure(httpException);
			}
		};
	}
	
	@Override
	public void doOnSuccess(Object result)
	{
		JSONObject jsonObj = (JSONObject) result;
		StickerCategory stickerCategory = StickerManager.getInstance().parseStickerCategoryMetadata(jsonObj);
		if(stickerCategory == null)
		{
			return;
		}
		boolean isDownloaded = StickerManager.getInstance().getStickerCategoryMap().containsKey(stickerCategory.getCategoryId());
		stickerCategory.setIsDownloaded(isDownloaded);
		HikeConversationsDatabase.getInstance().insertInToStickerCategoriesTable(stickerCategory);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_SUCCESS, stickerCategory);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_FAILURE, exception);
	}
	
	private String getRequestId()
	{
		return (StickerRequestType.CATEGORY_DETAIL.getLabel() + "\\" + categoryId);
	}
}
