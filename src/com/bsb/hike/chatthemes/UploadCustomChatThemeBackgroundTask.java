package com.bsb.hike.chatthemes;


import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatthemes.model.ChatThemeToken;
import com.bsb.hike.models.Conversation.Conversation;
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
    private ChatThemeToken ctToken;
    private String sessionId;
    private Conversation mConversation;

    private final String TAG = "UploadCustomChatThemeBackgroundTask";

    public UploadCustomChatThemeBackgroundTask(ChatThemeToken token, Conversation conversation, String sessionId) {
        this.ctToken = token;
        this.sessionId = sessionId;
        this.mConversation = conversation;
    }

    @Override
    public void execute() {
        token = postCustomChatThemeBgImgUpload(ctToken.getImagePath(), sessionId, getRequestListener());
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
                HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CUSTOM_IMAGE_UPLOAD_FAILED, ctToken);
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
                    String themeId = ChatThemeManager.getInstance().processCustomThemeSignal(meta, ctToken, false);
                    if(themeId != null && mConversation != null) {
                        Pair<Conversation, String> pair = new Pair<>(mConversation, themeId);
                        HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CUSTOM_IMAGE_UPLOAD_SUCCESS, pair);
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
