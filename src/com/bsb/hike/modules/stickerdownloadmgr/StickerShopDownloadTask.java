package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerShopDownloadRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
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

public class StickerShopDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private int offset;
	
	private final String TAG = "StickerShopDownloadTask";
	
	private RequestToken token;
	
	public StickerShopDownloadTask(int offset)
	{
		this.offset = offset;
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
		token = StickerShopDownloadRequest(requestId, offset, getRequestListener());
		
		if(token.isRequestRunning())
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
		return StickerRequestType.SHOP.getLabel();
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
						Logger.e(TAG, "Sticker download failed null response");
						doOnFailure(null);
						return ;
					}

					JSONArray resultData = response.optJSONArray(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(TAG, "Sticker download failed null data");
						doOnFailure(null);
						return;
					}

					Logger.d(TAG, "Sticker shop downloaod result : " + resultData);
					doOnSuccess(resultData);
				}
				catch (Exception e)
				{
					doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
					return;
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
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, result);
	}
	
	@Override
	public void doOnFailure(HttpException e)
	{
		Logger.e(TAG, "on failure, exception ", e);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, e);
	}
}