package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerSignupUpgradeRequest;

public class StickerSignupUpgradeDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	
	private static final String TAG = "StickerSignupUpgradeDownloadTask";
	
	private JSONArray categoryList;
	
	private RequestToken token;
	
	public StickerSignupUpgradeDownloadTask(JSONArray categoryList)
	{
		this.categoryList = categoryList;
	}
	
	@Override
	public void execute()
	{
		if(!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		
		JSONObject postObject = getPostObject(categoryList);

		if (null == postObject)
		{
			doOnFailure(null);
			return;
		}

		String requestId = getRequestId();
		token = StickerSignupUpgradeRequest(requestId, postObject, getRequestListener());
		
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

    @Override
    public String getRequestId()
	{
		return StickerRequestType.SIGNUP_UPGRADE.getLabel();
	}
	
	private JSONObject getPostObject(JSONArray categoryList)
	{
		JSONObject postObject = new JSONObject();

		List<String> unSupportedLanguages = StickerLanguagesManager.getInstance().getUnsupportedLanguagesCollection();
		
		try
		{
			if(categoryList != null && categoryList.length() != 0)
			{
				postObject.put(StickerManager.CATEGORY_IDS, categoryList);
				postObject.put("resId", Utils.getResolutionId());
				postObject.put("lang", StickerSearchUtils.getISOCodeFromLocale(Utils.getCurrentLanguageLocale()));

				if(!Utils.isEmpty(unSupportedLanguages))
				{
					postObject.put("unknown_langs", new JSONArray(unSupportedLanguages));
				}
				postObject = Utils.getParameterPostBodyForHttpApi(HttpRequestConstants.BASE_CATEGORY_DETAIL, postObject);
				return postObject;
			}
			Logger.e(TAG, "Sticker download failed null or empty category list");
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Sticker download failed json exception", e);
			return null;
		}
		return null;
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
					if(!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						doOnFailure(null);
						return ;
					}
					
					Logger.d(TAG,  "Got response for download : " + response.toString());
					
					JSONArray resultData = response.optJSONArray(HikeConstants.DATA_2);
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
		JSONArray resultData = (JSONArray) result;
		StickerManager.getInstance().updateStickerCategoriesMetadata(resultData);
		HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, true);
	}

	@Override
	public void doOnFailure(HttpException e)
	{
		Logger.e(TAG, "on failure, exception ", e);
	}

	@Override
	public Bundle getRequestBundle()
	{
		return null;
	}
}
