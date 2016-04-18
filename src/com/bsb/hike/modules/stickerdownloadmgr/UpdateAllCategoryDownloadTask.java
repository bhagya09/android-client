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
public class UpdateAllCategoryDownloadTask implements IHikeHTTPTask
{

	private static final String UPDATE_ALL_CATEGORIES_TAG = "UpdateAllCategoryDownloadTask";

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
					Logger.d(UPDATE_ALL_CATEGORIES_TAG, result.getBody().getContent().toString());

					JSONObject response = (JSONObject) result.getBody().getContent();

					if (!Utils.isResponseValid(response))
					{
						Logger.e(UPDATE_ALL_CATEGORIES_TAG, "Sticker Category Update download failed null response");
						return;
					}

					JSONObject resultData = response.getJSONObject(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(UPDATE_ALL_CATEGORIES_TAG, "Sticker Category Update failed null data");
						return;
					}
					Logger.d(UPDATE_ALL_CATEGORIES_TAG, "Sticker Category Update result : " + resultData);

					JSONArray jsonArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(jsonArray, false);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATE_ORDER_SERVER_TIMESTAMP, resultData.optLong(HikeConstants.TIMESTAMP));
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATE_CATEGORIES_TIMESTAMP, System.currentTimeMillis());
				}
				catch (Exception e)
				{
					Logger.d(UPDATE_ALL_CATEGORIES_TAG, e.toString());
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
		if ((System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATE_CATEGORIES_TIMESTAMP, 0L)) > HikeConstants.ONE_DAY_MILLS)
		{
			int offset = HikeConversationsDatabase.getInstance().getRowsCountStickerCategoryTable();
			token = HttpRequests.updateAllCategoriesData(StickerConstants.StickerRequestType.UPDATE_CATEGORY.getLabel(),
					HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATE_ORDER_SERVER_TIMESTAMP, 0L), offset, getRequestListener());
			token.execute();
		}
	}

	@Override
	public void cancel()
	{

	}
}
