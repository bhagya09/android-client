package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;
import android.os.Bundle;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.quickSuggestionsForSingleStickerRequest;

/**
 * Created by anubhavgupta on 07/01/16.
 */
public class SingleStickerQuickSuggestionDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private final String TAG = "SingleStickerQuickSuggestionDownloadTask";

	private RequestToken requestToken;

	private Sticker sticker;

	public SingleStickerQuickSuggestionDownloadTask(Sticker sticker)
	{
		this.sticker = sticker;
	}

	@Override
	public void execute()
	{
		requestToken = quickSuggestionsForSingleStickerRequest(
				getRequestId(),
				sticker,
				StickerLanguagesManager.getInstance().listToString(
						StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
								StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)),
				0, // TODO have setid logic here
				getResponseListener());
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
					Logger.e(TAG, "Single sticker quick suggestions download failed null or invalid response");
					doOnFailure(null);
					return;
				}
				Logger.d(TAG, "Got response for single sticker quick suggestions download task " + response.toString());

				doOnSuccess(response);
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

	@Override
	public void cancel()
	{
		if (requestToken != null)
		{
			requestToken.cancel();
		}
	}

    @Override
    public String getRequestId()
	{
		return StickerConstants.StickerRequestType.SINGLE_QUICK_SUGGESTION.getLabel() + "\\" + sticker.getCategoryId() + "\\" + sticker.getStickerId();
	}

	@Override
	public void doOnSuccess(Object result)
	{
		JSONObject response = (JSONObject) result;
		QuickStickerSuggestionController.getInstance().insertQuickSuggestion(response);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.e(TAG, "response failed for single sticker quick suggestions", exception);
	}

    @Override
    public Bundle getRequestBundle()
    {
        return null;
    }
}
