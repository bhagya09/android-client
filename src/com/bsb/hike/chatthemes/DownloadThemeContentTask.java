package com.bsb.hike.chatthemes;

import android.support.annotation.Nullable;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.chatthemes.model.ChatThemeToken;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssetId;


public class DownloadThemeContentTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private RequestToken token;

    private ChatThemeToken mToken = null;

    private final String TAG = "DownloadThemeContentTask";

    public DownloadThemeContentTask(ChatThemeToken token) {
        this.mToken = token;
    }

    @Override
    public void execute() {
        JSONObject body = prepareBodyObject();
        if (body != null) {
            token = downloadChatThemeAssetId(body, getRequestListener());
            if (token.isRequestRunning()) {
                return;
            }
            token.execute();
        }
    }

    @Override
    public void cancel() {
        if (token != null) {
            token.cancel();
        }
    }

    @Override
    public void doOnSuccess(Object result) {
        Logger.d(TAG, "chat theme asset id download complete");
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(TAG, "chat theme asset id download failed");
    }

    @Override
    public Bundle getRequestBundle() {
        return null;
    }

    @Override
    public String getRequestId() {
        return null;
    }

    private IRequestListener getRequestListener() {
        return new IRequestListener() {

            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException) {
                doOnFailure(httpException);
            }

            @Override
            public void onRequestSuccess(Response result) {
                try {
                    JSONObject response = (JSONObject) result.getBody().getContent();
                    if (!Utils.isResponseValid(response)) {
                        doOnFailure(null);
                        return;
                    }
                    parseAssetContent(response);
                    doOnSuccess(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress) {

            }

        };
    }

    private JSONObject prepareBodyObject() {
        try {
            JSONObject themeIds = new JSONObject();
            JSONArray ids = new JSONArray();
            ids.put(0, mToken.getThemeId());
            themeIds.put(HikeChatThemeConstants.JSON_DWNLD_THEME_ID, ids);
            return themeIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void parseAssetContent(JSONObject resp) {
        try {
            JSONArray data = resp.getJSONArray(HikeConstants.DATA_2);
            if(mToken.isCustom()){
                ChatThemeManager.getInstance().processCustomThemeSignal(data.getJSONObject(0), mToken, true);
            }else {
                //TODO CHATTHEME, Enable if it OTA Themes
                //ChatThemeManager.getInstance().processNewThemeSignal(data, false);
            }
        } catch (JSONException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }
    }
}
