package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 19/05/16.
 */
public class CognitoUploadGcmTask implements IGcmTask {
    public static final String DATA_TO_UPLOAD = "data_to_upload";

    public static final String URL = "upload_url";

    public static final String REQUEST_ID = "request_id";

    @Override
    public Void execute(TaskParams taskParams) {
        Bundle extra = taskParams.getExtras();
        final String uploadUrl = extra.getString(URL);
        final String requestId = extra.getString(REQUEST_ID);
        String dataToUpload = extra.getString(DATA_TO_UPLOAD);

        try {
            IRequestListener requestListener = new IRequestListener() {

                @Override
                public void onRequestSuccess(Response result) {
                    JSONObject response = (JSONObject) result.getBody().getContent();
                    Logger.d("Cognito", response.toString());
                }

                @Override
                public void onRequestProgressUpdate(float progress) {
                }

                @Override
                public void onRequestFailure(final HttpException httpException) {
                    Logger.d("Cognito", "failure : " + requestId);
                }
            };
            HttpRequests.cognitoUploadRequest(uploadUrl, requestId, new JSONObject(dataToUpload), requestListener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
