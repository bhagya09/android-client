package com.hike.cognito.transport;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.hike.cognito.security.Security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public class RealTimeTransport extends Transport {

    private String mDataType = null;

    RealTimeTransport() {
        super(null, false);
    }

    RealTimeTransport(String url, boolean isPii) {
        super(url, isPii);
        mDataType = url; //TODO Its a hack, fix interface on server!!!
    }

    @Override
    public void sendJsonData(JSONObject logData) {
        logData = secureData(logData);
        if(logData == null) return;

        HttpRequests.cognitoUploadRequest(getUrl(), mDataType, logData, getRequestListener(mDataType)).execute();
    }

    @Override
    public void sendJsonArrayData(JSONArray logData) {
        if(logData == null) return;
        JSONObject logJsonData = secureData(logData.toString());
        if(logJsonData == null) return;

        HttpRequests.cognitoUploadRequest(getUrl(), mDataType, logJsonData, getRequestListener(mDataType)).execute();
    }

    private JSONObject secureData(JSONObject data) {
        if (isPII()) {
            try {
                JSONObject jsonLogObj = new JSONObject();
                jsonLogObj.putOpt(mDataType, Security.getSecurity(getUrl()).encryptData(data.toString()));
                return jsonLogObj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private JSONObject secureData(String data) {
        try {
            if (isPII()) {
                JSONObject jsonLogObj = new JSONObject();
                jsonLogObj.putOpt(mDataType, Security.getSecurity(getUrl()).encryptData(data));
                return jsonLogObj;
            } else {
                return new JSONObject(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public IRequestListener getRequestListener(final String dataToUpload) {
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
            public void onRequestFailure(@Nullable Response errorResponse, final HttpException httpException) {
                Logger.d("Cognito", "Failed to upload: " + dataToUpload);
                httpException.printStackTrace();
            }
        };
        return requestListener;
    }
}
