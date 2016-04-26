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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by ashishagarwal on 15/04/16.
 */
public class FetchCategoryMetadataTask implements IHikeHTTPTask
{

	private final String FETCH_ALL_CATEGORIES_TAG = "FetchCategoryMetadataTask";

	private RequestToken token;

	private JSONObject requestJsonBody;

	private List<StickerCategory> list;

	private String ucids = "";

	public FetchCategoryMetadataTask(List<StickerCategory> list)
	{
		this.list = list;
		requestJsonBody = new JSONObject();
		JSONObject jsonObject;
		JSONArray array = new JSONArray();
		try
		{
			for (StickerCategory category : list)
			{
				ucids += category.getUcid();
				jsonObject = new JSONObject();
				jsonObject.put(Integer.toString(category.getUcid()), category.getPackUpdationTime());
				array.put(jsonObject);
			}
			requestJsonBody.put(HikeConstants.UCIDS, array);
			Logger.d(FETCH_ALL_CATEGORIES_TAG,  requestJsonBody.toString());
		}
		catch (Exception e)
		{
            Logger.d(FETCH_ALL_CATEGORIES_TAG, e.toString());
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
				Logger.d(FETCH_ALL_CATEGORIES_TAG, httpException.toString());
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
					JSONArray jsonArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(jsonArray, false);
					HikeConversationsDatabase.getInstance().updateIsPackMetadataUpdated(list);
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
		if (requestJsonBody != null)
		{
			token = HttpRequests.fetchCategoryData(getCategoryFetchRequestId(), requestJsonBody, getRequestListener());
			token.execute();
		}
	}

	@Override
	public void cancel()
	{

	}

}
