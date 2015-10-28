package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.tagsForCategoriesRequest;

public class StickerTagDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private static String TAG = StickerTagDownloadTask.class.getSimpleName();

	private static int requestStep = 0;

	private ArrayList<String> stickerCategoryList;
	
	private long lastTagRefreshTime;
	
	private int state;
	

	public StickerTagDownloadTask(Set<String> stickerSet, int state)
	{
		this.stickerCategoryList = new ArrayList<String>(stickerSet);
		this.state = state;
		
		if(state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH)
		{
			this.lastTagRefreshTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_SUCCESSFUL_STICKER_TAG_REFRESH_TIME, System.currentTimeMillis());
		}
		else
		{
			this.lastTagRefreshTime = 0L;
		}
		
	}

	@Override
	public void execute()
	{
		Logger.d(TAG, "sticker list : " + stickerCategoryList);
		if (this.stickerCategoryList == null)
		{
			return;
		}

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
			json.put("timestamp", (lastTagRefreshTime/1000));
			//temp changes for regional testing begin here
			String mStringArray[] = { "eng","hin", "mar", "guj", "tam", "tel", "mal", "ben", "bho", "kan", "dcc" };

			JSONArray temp = new JSONArray(Arrays.asList(mStringArray));
			json.put("kbd",temp);
			//temp changes for regional testing end here


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
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (!Utils.isResponseValid(response))
				{
					Logger.e(TAG, "Sticker download failed null or invalid response");
					doOnFailure(null);
					return;
				}
				Logger.d(TAG, "Got response for download task " + response.toString());

				JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

				if (null == data)
				{
					Logger.e(TAG, "Sticker download failed null data");
					doOnFailure(null);
					return;
				}

				doOnSuccess(data);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.d(StickerTagWatcher.TAG, "response failed.");
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

	@Override
	public void doOnSuccess(Object result)
	{
		JSONObject response = (JSONObject) result;
		StickerSearchManager.getInstance().insertStickerTags(response, state);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.TAG_FIRST_TIME_DOWNLOAD, false);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.d(TAG, "response failed.");
	}
}