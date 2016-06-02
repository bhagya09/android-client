package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.quickSuggestionsForMultiStickerRequest;

public class MultiStickerQuickSuggestionDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private static String TAG = "MultiStickerQuickSuggestionDownloadTask";

	private static int requestStep = 0;

	private ArrayList<Sticker> stickerList;

	public MultiStickerQuickSuggestionDownloadTask(Set<Sticker> stickerSet)
	{
		this.stickerList = Utils.isEmpty(stickerSet) ? new ArrayList<Sticker>(0) : new ArrayList<>(stickerSet);
	}

	@Override
	public void execute()
	{
		if (Utils.isEmpty(stickerList))
		{
			return;
		}

		int i = 1;
		int downloadSize = getDownloadSize();
		JSONArray array = null;

		while (i <= stickerList.size())
		{
			if (array == null)
			{
				array = new JSONArray();
			}

			array.put(stickerList.get(i - 1).getStickerCode());

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
			json.put("stickers", array);
			json.put(HikeConstants.LANG, new JSONArray(StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)));
			json.put(HikeConstants.GENDER, HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.GENDER, 0));
			json.put(HikeConstants.SET_ID, QuickStickerSuggestionController.getInstance().getSetIdForQuickSuggestions());

			json = Utils.getParameterPostBodyForHttpApi(HttpRequestConstants.BASE_QUICK_SUGGESTIONS, json);
			RequestToken requestToken = quickSuggestionsForMultiStickerRequest(getRequestId(), json, getResponseListener());

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
					Logger.e(TAG, "invalid response for multi sticker quick suggestion");
					doOnFailure(null);
					return;
				}
				Logger.d(TAG, "Got response for download task " + response.toString());

				JSONArray quickResponse = response.optJSONArray(HikeConstants.QUICK_RESPONSE);

				if (null == quickResponse)
				{
					Logger.e(TAG, "invalid quick response json array for quick suggestion");
					doOnFailure(null);
					return;
				}

				doOnSuccess(quickResponse);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				doOnFailure(httpException);
			}
		};
	}

	private int getDownloadSize()
	{
		return 200;
	}

	@Override
	public void cancel()
	{

	}

	private String getRequestId()
	{
		return StickerRequestType.MULTI_QUICK_SUGGESTION.getLabel() + "\\" + requestStep;
	}

	@Override
	public void doOnSuccess(Object result)
	{
		JSONArray response = (JSONArray) result;
		QuickStickerSuggestionController.getInstance().insertQuickSuggestion(response);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.e(TAG, "response failed for quick suggestions", exception);
	}
}