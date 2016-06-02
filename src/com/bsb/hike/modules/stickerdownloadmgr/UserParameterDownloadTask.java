package com.bsb.hike.modules.stickerdownloadmgr;

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.userParameterRequest;

public class UserParameterDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private static String TAG = "UserParameterDownloadTask";

    private RequestToken requestToken;

    public UserParameterDownloadTask() {

    }

    @Override
    public void execute() {

        requestToken = userParameterRequest(getRequestId(), getResponseListener());

        if (requestToken.isRequestRunning()) {
            return;
        }

        requestToken.execute();
    }

    private IRequestListener getResponseListener() {
        return new IRequestListener() {

            @Override
            public void onRequestSuccess(Response result) {
                JSONObject response = (JSONObject) result.getBody().getContent();

                if (!Utils.isResponseValid(response)) {
                    Logger.e(TAG, "user parameter request failed null or invalid response");
                    doOnFailure(null);
                    return;
                }
                Logger.d(TAG, "Got response for user parameter request " + response.toString());

                JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

                if (null == data) {
                    Logger.e(TAG, "user parameter request failed null data");
                    doOnFailure(null);
                    return;
                }

                doOnSuccess(data);
            }

            @Override
            public void onRequestProgressUpdate(float progress) {

            }

            @Override
            public void onRequestFailure(HttpException httpException) {
                Logger.d(TAG, "response failed.");
            }
        };
    }

    @Override
    public void cancel() {
        if (requestToken != null) {
            requestToken.cancel();
        }
    }

    private String getRequestId() {
        return StickerRequestType.USER_PARAMETERS.getLabel();
    }

    @Override
    public void doOnSuccess(Object result) {

        JSONObject paramJSON = (JSONObject) result;

        Iterator<String> keys = paramJSON.keys();
        List<Pair<String, String>> parameterList = new ArrayList<>(paramJSON.length());

        while (keys.hasNext()) {
            try {
                String parameterKey = keys.next();
                String parameterValue = paramJSON.getString(parameterKey);
                parameterList.add(new Pair<>(parameterKey, parameterValue));
            } catch (JSONException e) {
                Logger.e(TAG, "exception in parsing response ", e);
            }
        }
        HikeConversationsDatabase.getInstance().insertParameterListInDb(parameterList);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_USER_PARAMETER_FETCH_TIME, System.currentTimeMillis());
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(TAG, "response failed.");
    }
}