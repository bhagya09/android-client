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
import com.hike.cognito.UserLogInfo;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 19/05/16.
 */
public class CognitoUploadGcmTask implements IGcmTask {
    public static final String URL = "upload_url";

    public static final String REQUEST_ID = "request_id";

    @Override
    public Void execute(TaskParams taskParams) {
        Bundle extra = taskParams.getExtras();
        final String requestId = extra.getString(REQUEST_ID);
        UserLogInfo.requestUserLogs(requestId);
        return null;
    }
}
