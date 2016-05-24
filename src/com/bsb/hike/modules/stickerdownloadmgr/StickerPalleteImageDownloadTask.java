package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerPalleteImageDownloadRequest;

import java.io.File;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPalleteImageDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private String TAG = "StickerPalleteImageDownloadTask";

	private String categoryId;

	private String enableImagePath;

	private String disableImagePath;
	
	private RequestToken token;

	public StickerPalleteImageDownloadTask(String categoryId)
	{
		this.categoryId = categoryId;
	}
	
	@Override
	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		
		String requestId = getRequestId();
		
		token = StickerPalleteImageDownloadRequest(requestId, categoryId, getRequestInterceptor(), getRequestListener());
		if(token.isRequestRunning()) // return request already running
		{
			return ;
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
	
	private String getRequestId()
	{
		return (StickerRequestType.ENABLE_DISABLE.getLabel() + "\\" + categoryId);
	}

	private IRequestInterceptor getRequestInterceptor()
	{
		return new IRequestInterceptor()
		{
			
			@Override
			public void intercept(Chain chain) throws Exception
			{
				String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (dirPath == null)
				{
					Logger.e(TAG, "Sticker download failed directory does not exist");
					doOnFailure(null);
					return;
				}

				enableImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PALLATE_ICON_SELECTED + StickerManager.OTHER_ICON_TYPE;
				disableImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PALLATE_ICON + StickerManager.OTHER_ICON_TYPE;

				File otherDir = new File(dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT);
				if (!otherDir.exists())
				{
					if (!otherDir.mkdirs())
					{
						Logger.e(TAG, "Sticker download failed directory not created");
						doOnFailure(null);
						return;
					}
				}
				Utils.makeNoMediaFile(otherDir);
				
				chain.proceed();
			}
		};
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
					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null or invalid response");
						doOnFailure(null);
						return;
					}
					Logger.d(TAG, "Got response for download task " + response.toString());
					JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

					if (null == data)
					{
						Logger.e(TAG, "Sticker download failed null data");
						doOnFailure(null);
						return;
					}

					String enableImg = data.getString(HikeConstants.ENABLE_IMAGE);
					String disableImg = data.getString(HikeConstants.DISABLE_IMAGE);
					Utils.saveBase64StringToFile(new File(enableImagePath), enableImg);
					Utils.saveBase64StringToFile(new File(disableImagePath), disableImg);
					doOnSuccess(null);
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
		finish();
	}

	@Override
	public void doOnFailure(HttpException e)
	{
		finish();
		Logger.e(TAG, "exception :", e);
	}

	private void finish()
	{
		HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PALLATE_ICON_SELECTED_TYPE));
		HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PALLATE_ICON_TYPE));
	}
}
