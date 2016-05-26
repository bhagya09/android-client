package com.hike.abtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class NewUserExperimentFetchTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private static final String TAG = NewUserExperimentFetchTask.class.getSimpleName();
    private static final String USER_ID = "user_id";
    private static final String OS_VERSION = "os_version";
    private static final String OS = "os";
    private static final String AGE = "age";

    private static final int NUMBER_OF_RETRY = 2;
    private static final int DELAY_BEFORE_RETRY = 100;

    @Override
    public void execute() {
        Logger.d(TAG, "execute: ");
        JSONObject requestPayload = null;

        try {
            requestPayload = getRequest();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (requestPayload == null) return;

        RequestToken requestToken = HttpRequests.getAbTestNewUserRequestToken(getRequestListener(),
                requestPayload, NUMBER_OF_RETRY, DELAY_BEFORE_RETRY);

        if (!requestToken.isRequestRunning()) requestToken.execute();
    }

    @Override
    public void cancel() {

    }

    @Override
    public void doOnSuccess(Object result) {
        Logger.d(TAG, "OnSuccess: ");
        JSONObject requestJson = (JSONObject)result;
        try {
            ABTest.onRequestReceived(requestJson.getString(HikeConstants.TYPE), requestJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ABTest.setNewExperimentsAvailable();
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(TAG, "OnFailure: ");
    }

    private IRequestListener getRequestListener() {
        IRequestListener requestListener = new IRequestListener() {
            @Override
            public void onRequestSuccess(Response result) {
                JSONObject response = (JSONObject) result.getBody().getContent();
                if (!Utils.isResponseValid(response)) {
                    doOnFailure(null);
                } else {
                    Logger.d(TAG, "Response: " + response);
                    doOnSuccess(response);
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress){
            }

            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException) {
                doOnFailure(null);
            }
        };

        return requestListener;
    }
    public JSONObject getRequest() throws JSONException {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

        JSONObject requestJson = new JSONObject();
        requestJson.put(USER_ID, settings.getString(HikeMessengerApp.UID_SETTING, null));
        requestJson.put(HikeConstants.MSISDN, settings.getString(HikeMessengerApp.MSISDN_SETTING, null));
        requestJson.put(HikeConstants.GENDER, settings.getString(HikeConstants.SERVER_GENDER_SETTING, null));
        requestJson.put(OS_VERSION, Build.VERSION.RELEASE);
        requestJson.put(OS, HikeConstants.ANDROID);
        try {
            requestJson.put(HikeConstants.APP_VERSION, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        requestJson.put(AGE, getAge(settings.getInt(HikeConstants.SERVER_BIRTHDAY_YEAR, 0)));
        Logger.d(TAG, "Request: " + requestJson);
        return requestJson;
    }

    private int getAge(int birthYear) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if (birthYear == 0 || birthYear >= currentYear) {
            return 0;
        } else {
            return (currentYear - birthYear);
        }
    }
}
