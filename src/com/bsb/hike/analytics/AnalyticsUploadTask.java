package com.bsb.hike.analytics;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by abhijithkrishnappa on 13/02/16.
 */
public class AnalyticsUploadTask implements IHikeHTTPTask, IHikeHttpTaskResult {
    private final Context mContext;
    /**
     * maximum retry counts
     */
    private static final int MAX_RETRY_COUNT = 3;
    /**
     * delay before making first retry request(in ms)
     */
    private static final int DELAY_BEFORE_RETRY = 5000;
    private String mFileToUpload = null;
    private boolean mIsSessionComplete = false;

    AnalyticsUploadTask(String fileToUpload, boolean isSessionComplete) {
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
        mFileToUpload = fileToUpload;
        mIsSessionComplete = isSessionComplete;
    }

    @Override
    public void execute() {
        RequestToken requestToken = HttpRequests.getAnalyticsUploadRequestToken(getRequestListener(),
                getRequestInterceptor(), getRequestId(), MAX_RETRY_COUNT, DELAY_BEFORE_RETRY);
        //Double checking.. Execute request if not already running.
        if (!requestToken.isRequestRunning()) requestToken.execute();
    }

    private String getRequestId() {
        String requestId = mFileToUpload.substring(mFileToUpload.lastIndexOf("/") + 1);
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "requestId: " + requestId);
        return requestId;
    }

    @Override
    public void cancel() {
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Request cancelled.., do nothing");
    }

    @Override
    public void doOnSuccess(Object result) {
        if (mIsSessionComplete) {
            AnalyticsSender.getInstance(mContext).scheduleNextAlarm();
            HAManager.getInstance().setIsSendAnalyticsDataWhenConnected(false);
        }
        HAManager.getInstance().resetAnalyticsEventsUploadCount();
    }

    @Override
    public void doOnFailure(HttpException exception) {
        if (!HAManager.getInstance().isSendAnalyticsDataWhenConnected()) {
            HAManager.getInstance().setIsSendAnalyticsDataWhenConnected(true);
        }
    }

    private IRequestInterceptor getRequestInterceptor() {
        return new IRequestInterceptor() {
            @Override
            public void intercept(Chain chain) throws Exception {
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Intercepting HTTP request");
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Uploading: " + mFileToUpload);
                IRequestBody body = new FileBody("text/plain", new File(mFileToUpload));
                chain.getRequestFacade().setBody(body);
                chain.getRequestFacade().getHeaders().add(new Header("Content-Encoding", "gzip"));
                chain.proceed();
            }
        };
    }

    private IRequestListener getRequestListener() {
        IRequestListener requestListener = new IRequestListener() {
            @Override
            public void onRequestSuccess(Response result) {
                JSONObject response = (JSONObject) result.getBody().getContent();
                if (!Utils.isResponseValid(response)) {
                    Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File :" + mFileToUpload +
                            " upload failed!!");
                    doOnFailure(null);
                } else {
                    Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File :" + mFileToUpload +
                            " uploaded successfully!!");
                    new File(mFileToUpload).delete();
                    doOnSuccess(null);
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress) {
            }

            @Override
            public void onRequestFailure(HttpException httpException) {
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File :" + mFileToUpload + " upload failed!!");
                doOnFailure(null);
            }
        };

        return requestListener;
    }
}
