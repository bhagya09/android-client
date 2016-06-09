package com.bsb.hike.modules.stickerdownloadmgr;

import android.support.annotation.Nullable;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.tagsForSingleStickerRequest;

/**
 * Created by anubhavgupta on 07/01/16.
 */
public class SingleStickerTagDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private final String TAG = SingleStickerTagDownloadTask.class.getSimpleName();

    private RequestToken requestToken;

    private String stickerId;

    private String categoryId;

    public SingleStickerTagDownloadTask(String stickerId, String categoryId)
    {
        this.stickerId = stickerId;
        this.categoryId = categoryId;
    }

    @Override
    public void execute() {
        requestToken = tagsForSingleStickerRequest(
                getRequestId(),
                stickerId,
                categoryId,
                Utils.listToString(
                        StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
                                StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)),
                getResponseListener(),
                getRequestBundle());

        if (requestToken.isRequestRunning())
        {
            return;
        }
		StickerManager.getInstance().saveInStickerTagSet(new Sticker(categoryId, stickerId)); // add to set so that tag retry can occur for this sticker if this request fails
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
                    Logger.e(TAG, "Single sticker tag download failed null or invalid response");
                    doOnFailure(null);
                    return;
                }
                Logger.d(TAG, "Got response for single sticker tag download task " + response.toString());

                JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

                if (null == data)
                {
                    Logger.e(TAG, "single sticker tag download failed null data");
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
                Logger.d(TAG, "response failed.");
            }
        };
    }

    @Override
    public void cancel() {
        if(requestToken != null)
        {
            requestToken.cancel();
        }
    }

    @Override
    public String getRequestId()
    {
        return StickerConstants.StickerRequestType.SINGLE_TAG.getLabel() +"\\" + categoryId + "\\" + stickerId;
    }

    @Override
    public void doOnSuccess(Object result) {
        JSONObject response = (JSONObject) result;
        StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(response);
        StickerSearchManager.getInstance().insertStickerTags(response, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT);
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(TAG, "response failed.");
    }

    @Override
	public Bundle getRequestBundle()
	{
		Bundle extras = new Bundle();
		extras.putString(HikeConstants.STICKER_ID, stickerId);
		extras.putString(HikeConstants.CATEGORY_ID, categoryId);
		return extras;
	}
}
