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
public class FetchAllCategoriesDownloadTask implements IHikeHTTPTask
{

	private final String FETCH_ALL_CATEGORIES_TAG = "FetchAllCategoryDownloadTask";

	private int PAGE_SIZE_FETCH_STICKER_CATEGORY_CALL = 200;

	private RequestToken token;

	public String getCategoryFetchRequestId(int offset)
	{
		return StickerConstants.StickerRequestType.FETCH_CATEGORY.getLabel() + offset;
	}

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
					Logger.d(FETCH_ALL_CATEGORIES_TAG, result.getBody().getContent().toString());
					JSONObject response = (JSONObject) result.getBody().getContent();

					if (!Utils.isResponseValid(response))
					{
						Logger.e(FETCH_ALL_CATEGORIES_TAG, "Sticker Category fetch download failed null response");
						return;
					}

					JSONObject resultData = response.getJSONObject(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(FETCH_ALL_CATEGORIES_TAG, "Sticker Category fetch download failed null data");
						return;
					}
					Logger.d(FETCH_ALL_CATEGORIES_TAG, "Sticker Category fetch download result : " + resultData);

					JSONArray jsonArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(jsonArray, false);
					if (jsonArray.length() == 0)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FETCH_CATEGORIES_TIMESTAMP, System.currentTimeMillis());
					}
					else
					{
						download();
					}
				}
				catch (Exception e)
				{
					Logger.d(FETCH_ALL_CATEGORIES_TAG, e.toString());
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

		if ((System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FETCH_CATEGORIES_TIMESTAMP, 0L)) > HikeConstants.ONE_DAY_MILLS)
		{
			download();
		}

	}

	private void download()
	{
		int offset = HikeConversationsDatabase.getInstance().getRowsCountStickerCategoryTable();
		token = HttpRequests.fetchAllCategoriesData(getCategoryFetchRequestId(offset), offset, PAGE_SIZE_FETCH_STICKER_CATEGORY_CALL, getRequestListener());
		token.execute();
	}

	@Override
	public void cancel()
	{

	}

}
