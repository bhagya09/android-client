package com.bsb.hike.chatthemes;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
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

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssets;

/**
 * Created by sriram on 24/02/16.
 */
public class DownloadAssetsTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private String[] mAssetIds;

    private RequestToken token;

    private final String TAG = "DownloadAssetsTask";

    public DownloadAssetsTask(String[] ids) {
        this.mAssetIds = ids;
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
    public void doOnSuccess(Object result) {
        Logger.d(TAG, "chat theme asset download complete");
        updateAssetDownloadStatus(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS, result);
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(TAG, "chat theme asset download failed");
        updateAssetDownloadStatus(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);
        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_FAILURE, exception);
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
            JSONObject assetIds = new JSONObject();
            JSONArray ids = new JSONArray(mAssetIds);
            assetIds.put(HikeChatThemeConstants.JSON_DWNLD_ASSET_ID, ids);
            return assetIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] parseAssetContent(JSONObject resp) {
        try {
            JSONObject data = resp.getJSONObject(HikeConstants.DATA_2);
            String directoryPath = ChatThemeManager.getInstance().getDrawableHelper().getThemeAssetStoragePath();
            for (int i = 0; i < mAssetIds.length; i++) {
                if (directoryPath == null) {
                    continue;
                }

                String path = directoryPath + File.separator + mAssetIds[i];
                Utils.saveBase64StringToFile(new File(path), data.getString(mAssetIds[i]));
            }
        } catch (JSONException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        } catch (IOException e) {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }

        return mAssetIds;
    }

    private void updateAssetDownloadStatus(byte status) {
        int len = mAssetIds.length;
        for (int i = 0; i < len; i++) {
            HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(mAssetIds[i]);
            if (asset != null) {
                asset.setIsDownloaded(status);
            }
        }
    }
}
