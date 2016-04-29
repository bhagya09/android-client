package com.bsb.hike.modules.stickerdownloadmgr;

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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by ashishagarwal on 15/04/16.
 */
public class FetchCategoryMetadataTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private final String TAG = "FetchCategoryMetadataTask";

	private RequestToken token;

	private JSONObject requestJsonBody;

	private List<StickerCategory> list;

	private String ucids = "";

    public FetchCategoryMetadataTask(List<StickerCategory> list)
    {
        this.list = list;
        createRequestJsonBody();
    }


    private void createRequestJsonBody()
    {
		requestJsonBody = new JSONObject();
		JSONObject jsonObject;
		JSONArray array = new JSONArray();
		try
        {
			for (StickerCategory category : list)
            {
				ucids += category.getUcid() + HikeConstants.DELIMETER;
				jsonObject = new JSONObject();
				jsonObject.put(Integer.toString(category.getUcid()), category.getPackUpdationTime());
				array.put(jsonObject);
			}
			requestJsonBody.put(HikeConstants.UCIDS, array);
            Logger.d(TAG, requestJsonBody.toString());
        }
        catch (Exception e)
        {
			Logger.e(TAG, "Exception in createjJsonBody" ,e);
		}
	}

	public String getCategoryFetchRequestId()
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
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					Logger.d(TAG, response.toString());

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
					JSONArray jsonArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(jsonArray, false);
					HikeConversationsDatabase.getInstance().updateIsPackMetadataUpdated(list);
				}
				catch (Exception e)
				{
					Logger.e(TAG, "Exception", e);
					doOnFailure(new HttpException(e));
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
		if (requestJsonBody == null)
		{
			return;
		}
		token = HttpRequests.fetchCategoryData(getCategoryFetchRequestId(), requestJsonBody, getRequestListener());
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
