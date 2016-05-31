package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.stickerCategoriesDetailsDownloadRequest;

import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerCategoriesDetailsDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static final String TAG = "StickerCatgeoryDownloadTask";

	private Collection<StickerCategory> stickerCategoryList;

	private RequestToken token;

	private JSONObject requestJsonBody;

	private String ucids = "";

	public StickerCategoriesDetailsDownloadTask(Collection<StickerCategory> stickerCategoryList)
	{
		this.stickerCategoryList = stickerCategoryList;
		createRequestJsonBody();
	}

	private void createRequestJsonBody()
	{
		requestJsonBody = new JSONObject();

		JSONObject jsonObject;

		JSONArray catIdArray = new JSONArray();
		JSONArray tsArray = new JSONArray();

		try
		{
			for (StickerCategory category : stickerCategoryList)
			{
				ucids += category.getUcid() + HikeConstants.DELIMETER;
				catIdArray.put(category.getCategoryId());
				tsArray.put(category.getPackUpdationTime());
			}

			requestJsonBody.put(StickerManager.CATEGORY_IDS, catIdArray);
			requestJsonBody.put(HikeConstants.TIMESTAMP, tsArray);
			requestJsonBody.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
			requestJsonBody.put(HikeConstants.LANG, StickerSearchUtils.getCurrentLanguageISOCode());

			List<String> unsupportedLanguages = StickerLanguagesManager.getInstance().getUnsupportedLanguagesCollection();
			if (Utils.isEmpty(unsupportedLanguages))
			{
				requestJsonBody.put(HikeConstants.UNKNOWN_KEYBOARDS, StickerLanguagesManager.getInstance().listToString(unsupportedLanguages));
			}

		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in createjJsonBody", e);
		}
	}

	@Override
	public void execute()
	{
		String requestId = getRequestId(); // for duplicate check

		token = stickerCategoriesDetailsDownloadRequest(requestId, requestJsonBody, getRequestListener(), getRequestBundle());

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
					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						doOnFailure(null);
						return;
					}

					Logger.d(TAG, "Got response for download : " + response.toString());

					JSONArray resultData = response.optJSONArray(HikeConstants.DATA_2);
					if (null == resultData)
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
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure(httpException);
			}
		};
	}

	@Override
	public void doOnSuccess(Object result)
	{
		JSONArray resultData = (JSONArray) result;
		StickerManager.getInstance().parseStickerCategoryMetadata(resultData);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_FAILURE, exception);
	}

	@Override
	public String getRequestId()
	{
		return StickerRequestType.CATEGORY_DETAIL.getLabel() + Utils.StringToMD5(ucids);
	}

	@Override
	public Bundle getRequestBundle()
	{
		return null;
	}
}
