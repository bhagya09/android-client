package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import com.google.android.gms.gcm.TaskParams;
import com.hike.cognito.CognitoTrigger;

import android.os.Bundle;


/**
 * Created by abhijithkrishnappa on 19/05/16.
 */
public class CognitoUploadGcmTask implements IGcmTask {
    public static final String REQUEST_ID = "request_id";

    @Override
    public Void execute(TaskParams taskParams) {
        Bundle extra = taskParams.getExtras();
        final String requestId = extra.getString(REQUEST_ID);
        CognitoTrigger.onTransportRetry(requestId);
        return null;
    }
}
