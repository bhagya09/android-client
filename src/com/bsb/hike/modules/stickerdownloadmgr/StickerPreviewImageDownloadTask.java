package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerPreviewImageDownloadRequest;

import java.io.File;

import org.json.JSONObject;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

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

public class StickerPreviewImageDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private String TAG = "StickerPreviewImageDownloadTask";

	private String categoryId;

	private int previewType;

	String previewImagePath;
	
	private RequestToken token;

	public StickerPreviewImageDownloadTask(String categoryId, int previewType)
	{
		this.categoryId = categoryId;
		this.previewType = previewType;
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
		token = StickerPreviewImageDownloadRequest(requestId, categoryId, getRequestInterceptor(), getRequestListener());

		if (token.isRequestRunning()) // duplicate check
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

	private String getRequestId()
	{
		return (StickerRequestType.PREVIEW.getLabel() + "\\" + categoryId);
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

				previewImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PREVIEW_IMAGE + StickerManager.OTHER_ICON_TYPE;

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

					String stickerData = data.getString(HikeConstants.PREVIEW_IMAGE);
					HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, previewType));
					Utils.saveBase64StringToFile(new File(previewImagePath), stickerData);
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
		Intent i = new Intent(StickerManager.STICKER_PREVIEW_DOWNLOADED);
		LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(i);
	}

	@Override
	public void doOnFailure(HttpException e)
	{
		Logger.e(TAG, "on failure, exception ", e);
	}
}