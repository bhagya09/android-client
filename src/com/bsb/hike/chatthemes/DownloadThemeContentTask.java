package com.bsb.hike.chatthemes;

import com.bsb.hike.HikeConstants;
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

    private String[] mThemeIds = null;

    private String mThemeId = null;

    private boolean isCustom = false;

    private RequestToken token;

    private final String TAG = "DownloadThemeContentTask";

    public DownloadThemeContentTask(String[] themeIds) {
        this.mThemeIds = themeIds;
    }

    public DownloadThemeContentTask(String themeId, boolean isCustom) {
        this.mThemeId = themeId;
        this.isCustom = isCustom;
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

    private IRequestListener getRequestListener() {
        return new IRequestListener() {

            @Override
            public void onRequestFailure(HttpException httpException) {
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
                    doOnSuccess(parseAssetContent(response));
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
            if (mThemeId != null) {
                ids.put(0, mThemeId);
            } else {
                for (int i = 0; i < mThemeIds.length; i++) {
                    ids.put(i, mThemeIds[i]);
                }
            }
            themeIds.put(HikeChatThemeConstants.JSON_DWNLD_THEME_ID, ids);
            return themeIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] parseAssetContent(JSONObject resp) {
        try {
            JSONArray data = resp.getJSONArray(HikeConstants.DATA_2);
            if(isCustom){
                ChatThemeManager.getInstance().processCustomThemeSignal(data.getJSONObject(0), true);
            }else {
                //TODO CHATTHEME, Enable if it OTA Themes
                //ChatThemeManager.getInstance().processNewThemeSignal(data, false);
            }
        } catch (JSONException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }

        return mThemeIds;
    }
}
