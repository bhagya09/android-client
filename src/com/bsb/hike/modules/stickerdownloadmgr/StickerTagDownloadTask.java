package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.ArrayList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchSetupManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.Logger;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.tagsForCategoriesRequest;

public class StickerTagDownloadTask implements IHikeHTTPTask
{

	private static String TAG = "StickerTagDownloadTask";

	private static int requestStep = 0;

	private ArrayList<String> stickerCategoryList;

	public StickerTagDownloadTask(Set<String> stickerSet)
	{
		this.stickerCategoryList = new ArrayList<String>(stickerSet);
	}

	@Override
	public void execute()
	{

		if (this.stickerCategoryList == null)
		{
			return;
		}

		Logger.d(TAG, "sticker list : " + stickerCategoryList.toString());
		
		int i = 1;
		int downloadSize = getDownloadSize();
		JSONArray array = null;

		while (i <= stickerCategoryList.size())
		{
			if (array == null)
			{
				array = new JSONArray();
			}

			array.put(stickerCategoryList.get(i - 1));

			if ((i % downloadSize) == 0)
			{
				download(array);
				array = null;
				requestStep++;
			}

			i++;
		}
		if (array != null)
		{
			download(array);
		}
	}

	private void download(JSONArray array)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(HikeConstants.CATEGORY_ID_LIST, array);

			RequestToken requestToken = tagsForCategoriesRequest(getRequestId(), json, getResponseListener());

			if (requestToken.isRequestRunning())
			{
				return;
			}

			requestToken.execute();
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "json exception ", e);
		}
	}

	private IRequestListener getResponseListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject json = (JSONObject) result.getBody().getContent();
				Logger.d("anubhav", "response : " + json.toString());
				
				StickerSearchManager.getInstance().insertStickerTags(json);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				int x = 5;
			}
		};
	}

	private int getDownloadSize()
	{
		return 100;
	}

	@Override
	public void cancel()
	{

	}

	private String getRequestId()
	{
		return StickerRequestType.TAGS.getLabel() + "\\" + requestStep;
	}

}
