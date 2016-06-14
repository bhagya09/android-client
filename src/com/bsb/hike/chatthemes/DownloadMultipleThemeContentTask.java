package com.bsb.hike.chatthemes;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.chatthemes.model.ChatThemeToken;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssetId;

/**
 * Created by sriram on 12/06/16.
 */
public class DownloadMultipleThemeContentTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private HashMap<String, ChatThemeToken> mTokensMap;

    private RequestToken token;

    public DownloadMultipleThemeContentTask(HashMap<String, ChatThemeToken> tokensMap){
        this.mTokensMap = tokensMap;
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
    public Bundle getRequestBundle() {
        return null;
    }

    @Override
    public String getRequestId() {
        return null;
    }

    @Override
    public void doOnSuccess(Object result) {

    }

    @Override
    public void doOnFailure(HttpException exception) {

    }

    private IRequestListener getRequestListener() {
        return new IRequestListener() {

            @Override
            public void onRequestFailure(Response errorResponse, HttpException httpException) {
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
                    //doOnSuccess();
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
            Set<String> themeIdToReq = mTokensMap.keySet();
            //adding to the JSON array
            int i = 0;
            for(String themeId : themeIdToReq) {
                ids.put(i, themeId);
                i++;
            }
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
            ChatThemeManager.getInstance().processMultipleCustomThemeSignal(data, mTokensMap);
        } catch (JSONException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }
    }
}
