package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 15/04/16.
 */
public class FetchCategoryTagDataTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private final String TAG = FetchCategoryTagDataTask.class.getSimpleName();

	private RequestToken token;

	private JSONObject requestJsonBody;

	private Map<Integer, CategoryTagData> fetchMap;

	private List<CategoryTagData> fetchList;

	private String ucids = "";

	public FetchCategoryTagDataTask(List<CategoryTagData> list)
	{
		fetchList = list;
		fetchMap = new HashMap<Integer, CategoryTagData>();
		createRequestJsonBody();
	}

	private void createRequestJsonBody()
	{
		requestJsonBody = new JSONObject();
		JSONObject jsonObject;
		JSONArray array = new JSONArray();
		try
		{
			for (CategoryTagData categoryTagData : fetchList)
			{
				ucids += categoryTagData.getUcid() + HikeConstants.DELIMETER;
				jsonObject = new JSONObject();
				jsonObject.put(Integer.toString(categoryTagData.getUcid()), categoryTagData.getCategoryLastUpdatedTime());
				array.put(jsonObject);
				fetchMap.put(categoryTagData.getUcid(), categoryTagData);
			}
			requestJsonBody.put(HikeConstants.UCIDS, array);
		}
		catch (Exception e)
		{
			Logger.d(TAG, e.toString());
		}
	}

	public String getCategoryFetchTagsRequestId()
	{
		return StickerConstants.StickerRequestType.FETCH_CATEGORY.getLabel() + ucids;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure(httpException);
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (!Utils.isResponseValid(response))
				{
					Logger.e(TAG, "Sticker Category fetch download failed null response");
					doOnFailure(null);
					return;
				}

				JSONObject resultData = response.optJSONObject(HikeConstants.DATA_2);
				if (null == resultData)
				{
					Logger.e(TAG, "Sticker Category fetch download failed null data");
					doOnFailure(null);
					return;
				}

				JSONArray packs = resultData.optJSONArray(HikeConstants.PACKS);

				CategorySearchManager.getInstance().insertCategoryTags(packs, fetchMap);

				HikeConversationsDatabase.getInstance().updateIsPackTagdataUpdated(fetchList);
				doOnSuccess(null);

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
		if (requestJsonBody == null)
		{
			return;
		}

		token = HttpRequests.fetchCategoryTagData(getCategoryFetchTagsRequestId(), requestJsonBody, getRequestListener());

		if (!token.isRequestRunning())
		{
			token.execute();
		}
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
	public void doOnSuccess(Object result)
	{

	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.e(TAG, "Exception", exception);
	}
}
