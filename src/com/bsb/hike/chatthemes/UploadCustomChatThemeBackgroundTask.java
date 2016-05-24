package com.bsb.hike.chatthemes;


import android.support.annotation.Nullable;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.postCustomChatThemeBgImgUpload;


/**
 * Created by sriram on 14/04/16.
 */
public class UploadCustomChatThemeBackgroundTask implements IHikeHTTPTask {
    private RequestToken token;
    private String imagePath;
    private String sessionId;

    private final String TAG = "UploadCustomChatThemeBackgroundTask";

    public UploadCustomChatThemeBackgroundTask(String imagePath, String sessionId) {
        this.imagePath = imagePath;
        this.sessionId = sessionId;
    }

    @Override
    public void execute() {
        token = postCustomChatThemeBgImgUpload(imagePath, sessionId, getRequestListener());
        if ((token == null) || token.isRequestRunning()) {
            return;
        }
        token.execute();
    }

    @Override
    public void cancel() {
        if (token != null) {
            token.cancel();
        }
    }

    private IRequestListener getRequestListener() {
        return new IRequestListener() {
            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException) {
                Log.v(TAG, "Custom Chattheme Image Upload Failed :::::::::::::::::");
                HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CUSTOM_IMAGE_UPLOAD_FAILED, null);
            }

            @Override
            public void onRequestSuccess(Response result) {
                try {
                    JSONObject response = (JSONObject) result.getBody().getContent();
                    if (!Utils.isResponseValid(response)) {
                        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CUSTOM_IMAGE_UPLOAD_FAILED, null);
                        return;
                    }

                    JSONObject meta = response.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_META);
                    Log.v(TAG, "Custom Chattheme Image Upload Successful :::::::::::::::::"+meta);
                    String themeId = ChatThemeManager.getInstance().processCustomThemeSignal(meta, false);
                    if(themeId != null) {
                        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CUSTOM_IMAGE_UPLOAD_SUCCESS, themeId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress) {

            }
        };
    }
}
