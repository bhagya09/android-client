package com.bsb.hike.chatthemes;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatthemes.model.ChatThemeToken;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssets;

/**
 * Created by sriram on 12/06/16.
 */
public class DownloadMultipleAssetsTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private HashMap<String, ChatThemeToken> mTokenMap;

    private HashSet<String> mAssetIds = new HashSet<>();

    private RequestToken token;

    public DownloadMultipleAssetsTask(HashMap<String, ChatThemeToken> tokenMap) {
        this.mTokenMap = tokenMap;
    }

    @Override
    public void execute() {
        if (!StickerManager.getInstance().isMinimumMemoryAvailable()) {
            doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
            return;
        }

        JSONObject body = prepareBodyObject();
        if (body != null) {
            token = downloadChatThemeAssets(body, getRequestListener());
            if (token.isRequestRunning()) {
                return;
            }
            updateAssetDownloadStatus(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADING);
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
        updateAssetDownloadStatus(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
        Set<String> themeIds = mTokenMap.keySet();
        for(String themeId : themeIds) {
            HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS, mTokenMap.get(themeId));
        }
    }

    @Override
    public void doOnFailure(HttpException exception) {
        updateAssetDownloadStatus(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);
        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_FAILURE, exception);
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
            JSONObject assetIds = new JSONObject();
            JSONArray ids = new JSONArray();
            Set<String> themeIds = mTokenMap.keySet();
            for(String themeId : themeIds){
                ChatThemeToken token = mTokenMap.get(themeId);
                //to avoid duplicates if any in the request
                mAssetIds.addAll(Arrays.asList(token.getAssets()));
            }

            //adding to the JSON array
            int i = 0;
            for(String assetId : mAssetIds) {
                ids.put(i, assetId);
                i++;
            }
            assetIds.put(HikeChatThemeConstants.JSON_DWNLD_ASSET_ID, ids);
            return assetIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void parseAssetContent(JSONObject resp) {
        try {
            JSONObject data = resp.getJSONObject(HikeConstants.DATA_2);
            String directoryPath = ChatThemeManager.getInstance().getDrawableHelper().getThemeAssetStoragePath();
            if(!TextUtils.isEmpty(directoryPath)) {
                for (String assetId : mAssetIds) {
                    String path = directoryPath + File.separator + assetId;
                    Utils.saveBase64StringToFile(new File(path), data.getString(assetId));
                }
            }
        } catch (JSONException | IOException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }
    }

    private void updateAssetDownloadStatus(byte status) {
        for (String assetId : mAssetIds) {
            HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(assetId);
            if (asset != null) {
                asset.setIsDownloaded(status);
            }
        }
    }
}
