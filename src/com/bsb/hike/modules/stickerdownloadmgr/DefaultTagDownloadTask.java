package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.defaultTagsRequest;

public class DefaultTagDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private static String TAG = "DefaultTagDownloadTask";

	private boolean isSignUp;

	private Collection<String> languages;

	private RequestToken requestToken;
	
	public DefaultTagDownloadTask(boolean isSignUp, Collection<String> languages)
	{
		this.isSignUp = isSignUp;
        this.languages = languages;

        Logger.d(TAG, "languages list : " + languages);

	}

	@Override
	public void execute()
	{
		if (Utils.isEmpty(languages))
		{
			return;
		}

		long lastSuccessfulTagDownloadTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_SUCESSFULL_TAGS_DOWNLOAD_TIME, 0L);
		requestToken = defaultTagsRequest(getRequestId(), isSignUp, lastSuccessfulTagDownloadTime, getResponseListener(),
				StickerLanguagesManager.getInstance().listToString(languages), getRequestBundle());

		if (requestToken.isRequestRunning())
		{
			return;
		}

		requestToken.execute();
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
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.d(StickerTagWatcher.TAG, "response failed.");
			}
		};
	}

	@Override
	public void cancel()
	{
		if(requestToken != null)
		{
			requestToken.cancel();
		}
	}

    @Override
    public String getRequestId()
	{
		return StickerRequestType.TAGS.getLabel();
	}

	@Override
	public void doOnSuccess(Object result)
	{
		JSONObject response = (JSONObject) result;
		StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(response);
		StickerSearchManager.getInstance().insertStickerTags(response, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DEFAULT_TAGS_DOWNLOADED, true);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_SUCESSFULL_TAGS_DOWNLOAD_TIME, System.currentTimeMillis());

		for(String lang : languages)
		{
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(lang, true);
		}
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.d(TAG, "response failed.");
	}

    @Override
	public Bundle getRequestBundle()
	{
		Bundle extras = new Bundle();
		extras.putBoolean(HikeConstants.IS_NEW_USER, isSignUp);
		extras.putStringArrayList(HikeConstants.LANGUAGES, new ArrayList<String>(languages));
		return extras;
	}
}