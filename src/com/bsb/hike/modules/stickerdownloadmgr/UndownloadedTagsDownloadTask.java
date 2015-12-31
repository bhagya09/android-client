
package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class UndownloadedTagsDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private static String TAG = "UndownloadedTagsDownloadTask";

    private ArrayList<String> stickerCategoryList;

    private Collection<String> languages;

    private RequestToken requestToken;

    public UndownloadedTagsDownloadTask(Set<String> stickerSet, Collection<String> languages) {

        this.languages = languages;

        this.stickerCategoryList = new ArrayList<String>(stickerSet);

    }

    @Override
    public void execute() {

        try
        {
            JSONObject json = new JSONObject();
            json.put(HikeConstants.CATEGORY_ID_LIST, new JSONArray(stickerCategoryList)) ;

            if(Utils.isEmpty(languages))
            {
                languages.add(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE);
            }
            Logger.d(TAG, "language list for download : " + languages);
            json.put(HikeConstants.KEYBOARD_LIST, new JSONArray(languages));


            RequestToken requestToken = HttpRequests.getUndownloadedTagsRequest(getRequestId(), getResponseListener(), json);

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

    private IRequestListener getResponseListener() {
        return new IRequestListener() {

            @Override
            public void onRequestSuccess(Response result)
            {
                JSONObject response = (JSONObject) result.getBody().getContent();

                if (!Utils.isResponseValid(response))
                {
                    Logger.e(TAG,"Sticker download failed null or invalid response");
                    doOnFailure( null);
                    return;
                }
                Logger.d(TAG, "Got response for download task " + response.toString());

                JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

                if (null == data)
                {
                    Logger.e(TAG,"Sticker download failed null data");
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
                Logger.d(StickerTagWatcher.TAG, "Request failed.");
            }
        };
    }

    @Override
    public void cancel() {
        if (requestToken != null) {
            requestToken.cancel();
        }
    }

    @Override
    public void doOnSuccess(Object result)
    {
        JSONObject response = (JSONObject) result;
        StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(response);
        StickerSearchManager.getInstance().insertUndownloadedStickersTag(response);
    }


    @Override
    public void doOnFailure(HttpException exception) {

        if(exception != null)
        {
            Logger.e(TAG,exception.getMessage());
        }
    }

    private String getRequestId() {
        return StickerConstants.StickerRequestType.TAGS.getLabel();
    }


}