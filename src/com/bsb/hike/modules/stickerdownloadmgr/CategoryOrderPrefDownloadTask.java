package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by ashishagarwal on 15/04/16.
 */
public class CategoryOrderPrefDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static final String FETCH_CAT_PREF_ORDER_TAG = "FetchcatPrefOrderDownloadTask";

	private RequestToken token;

	private IRequestListener getRequestListener()
	{

		return new IRequestListener()
		{
			@Override
			public void onRequestFailure(HttpException httpException)
			{
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					Logger.d(FETCH_CAT_PREF_ORDER_TAG, result.getBody().getContent().toString());

					JSONObject response = (JSONObject) result.getBody().getContent();

					if (!Utils.isResponseValid(response))
					{
						Logger.e(FETCH_CAT_PREF_ORDER_TAG, "Sticker Order download failed null response");
						return;
					}

					JSONObject resultData = response.getJSONObject(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(FETCH_CAT_PREF_ORDER_TAG, "Sticker Order download failed null data");
						return;
					}
					Logger.d(FETCH_CAT_PREF_ORDER_TAG, "Sticker Order downloaod result : " + resultData);

					JSONArray orderArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoryPrefOrder(orderArray);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATE_ORDER_TIMESTAMP, System.currentTimeMillis());
				}
				catch (Exception e)
				{
					Logger.d(FETCH_CAT_PREF_ORDER_TAG, e.toString());
					return;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}
		};

	}

	@Override
	public void execute()
	{
		if ((System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATE_ORDER_TIMESTAMP, 0L)) > HikeConstants.ONE_DAY_MILLS)
		{
			token = HttpRequests.getPrefOrderForCategories(StickerConstants.StickerRequestType.UPDATE_ORDER.getLabel(), getRequestListener());
			token.execute();
		}
	}

	@Override
	public void cancel()
	{

	}

	@Override
	public void doOnSuccess(Object result)
	{

	}

	@Override
	public void doOnFailure(HttpException exception)
	{

	}
}
