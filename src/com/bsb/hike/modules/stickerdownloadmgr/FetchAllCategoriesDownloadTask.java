package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by ashishagarwal on 15/04/16.
 */
public class FetchAllCategoriesDownloadTask implements IHikeHTTPTask
{

	private final String FETCH_ALL_CATEGORIES_TAG = "FetchAllCategoryDownloadTask";

	private RequestToken token;

	private JSONObject reqJson;

	private List<StickerCategory> list;

	private String ucids;

	public FetchAllCategoriesDownloadTask(List<StickerCategory> list)
	{
		this.list = list;
		reqJson = new JSONObject();
		JSONObject jsonObject;
		JSONArray array = new JSONArray();
		try
		{
			for (StickerCategory category : list)
			{
				ucids += category.getUcid();
				jsonObject = new JSONObject();
				jsonObject.put(category.getUcid() + "", category.getPackUpdationTime());
				array.put(jsonObject);
			}

			reqJson.put(HikeConstants.UCIDS, array);
		}
		catch (Exception e)
		{

		}
	}

	public String getCategoryFetchRequestId()
	{
		return StickerConstants.StickerRequestType.FETCH_CATEGORY.getLabel()+ ucids;
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
					HikeConversationsDatabase.getInstance().categoryMetadataStatusUpdate(list);
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
			download();
	}

	private void download()
	{
		if (reqJson != null)
		{
			token = HttpRequests.fetchAllCategoriesData(getCategoryFetchRequestId(), reqJson, getRequestListener());
			token.execute();
		}
	}

	@Override
	public void cancel()
	{

	}

}
