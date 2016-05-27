package com.hike.cognito;

import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
import com.hike.cognito.datapoints.DataPointTaskSessionLog;
import com.hike.cognito.datapoints.DataPointTaskAccInfo;
import com.hike.cognito.datapoints.DataPointTaskAdvId;
import com.hike.cognito.datapoints.DataPointTaskAllApps;
import com.hike.cognito.datapoints.DataPointTaskCallLogs;
import com.hike.cognito.datapoints.DataPointTaskDevDetails;
import com.hike.cognito.datapoints.DataPointTaskPhoneSpec;
import com.hike.cognito.datapoints.DataPointTaskStaticLoc;
import com.hike.cognito.transport.Transport;

public class UserLogInfo {
    public static final int START = 0;
    public static final int STOP = 2;
    public static final int OPERATE = 1;

    private static final String TAG = "UserLogInfo";
    public static final String USER_LOG_SHARED_PREFS = "user_log_info";
    public static final int CALL_ANALYTICS_FLAG = 1;
    public static final int APP_ANALYTICS_FLAG = 2;
    public static final int LOCATION_ANALYTICS_FLAG = 4;
    public static final int ADVERTISIND_ID_FLAG = 8;
    public static final int FETCH_LOG_FLAG = 16;
    public static final int PHONE_SPEC = 32;
    public static final int DEVICE_DETAILS = 64;
    public static final int ACCOUNT_ANALYTICS_FLAG = 128;
    public static final int ALL_LOGS = 255;

    private static int mRequestFlags;

    //TODO: Fix it for next release
    public static void requestUserLogs(final String request) {
        switch(request) {
            case HikeConstants.APP_LOG_ANALYTICS:
                requestUserLogs(APP_ANALYTICS_FLAG);
                return;
            case HikeConstants.CALL_LOG_ANALYTICS:
                requestUserLogs(CALL_ANALYTICS_FLAG);
                return;
            case HikeConstants.LOCATION_LOG_ANALYTICS:
                requestUserLogs(LOCATION_ANALYTICS_FLAG);
                return;
            case HikeConstants.ADVERTSING_ID_ANALYTICS:
                requestUserLogs(ADVERTISIND_ID_FLAG);
                return;
            case HikeConstants.SESSION_LOG_TRACKING:
                requestUserLogs(FETCH_LOG_FLAG);
                return;
            case HikeConstants.PHONE_SPEC:
                requestUserLogs(PHONE_SPEC);
                return;
            case HikeConstants.DEVICE_DETAILS:
                requestUserLogs(DEVICE_DETAILS);
                return;
            case HikeConstants.ACCOUNT_LOG_ANALYTICS:
                requestUserLogs(ACCOUNT_ANALYTICS_FLAG);
                return;
        }
    }

    public static void requestUserLogs(final int flags) {
        for (int counter = 0; counter < Integer.SIZE; counter++) {
            try {
                sendLogs((1 << counter) & flags);
            } catch (JSONException e) {
                Logger.d(TAG, "JSON exception in making Logs" + e);
            }
        }
    }

    public static void requestUserLogs(JSONObject data) throws JSONException {

        mRequestFlags = 0;

        if (data.optBoolean(HikeConstants.CALL_LOG_ANALYTICS)) {
            mRequestFlags |= CALL_ANALYTICS_FLAG;
        }
        if (data.optBoolean(HikeConstants.LOCATION_LOG_ANALYTICS)) {
            mRequestFlags |= LOCATION_ANALYTICS_FLAG;
        }
        if (data.optBoolean(HikeConstants.APP_LOG_ANALYTICS)) {
            mRequestFlags |= UserLogInfo.APP_ANALYTICS_FLAG;
        }
        if (data.optBoolean(HikeConstants.ADVERTSING_ID_ANALYTICS)) {
            mRequestFlags |= UserLogInfo.ADVERTISIND_ID_FLAG;
        }
        if (data.optBoolean(HikeConstants.FETCH_LOG_ANALYTICS)) {
            //TODO possibly turn this into "gl":true to "gl":"stl"
            mRequestFlags |= UserLogInfo.FETCH_LOG_FLAG;
        }
        if (data.optBoolean(HikeConstants.PHONE_SPEC)) {
            mRequestFlags |= UserLogInfo.PHONE_SPEC;
        }
        if (data.optBoolean(HikeConstants.DEVICE_DETAILS)) {
            mRequestFlags |= UserLogInfo.DEVICE_DETAILS;
        }
        if (data.optBoolean(HikeConstants.ACCOUNT_LOG_ANALYTICS)) {
            mRequestFlags |= UserLogInfo.ACCOUNT_ANALYTICS_FLAG;
        }

        if (mRequestFlags == 0) {
            return;
        }

        requestUserLogs(mRequestFlags);
    }

    public static void sendLogs(final int flags) throws JSONException {
        switch (flags) {
            case APP_ANALYTICS_FLAG:
                TaskProcessor.processTask(new DataPointTaskAllApps(HikeConstants.APP_LOG_ANALYTICS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case CALL_ANALYTICS_FLAG:
                TaskProcessor.processTask(new DataPointTaskCallLogs(HikeConstants.CALL_LOG_ANALYTICS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case LOCATION_ANALYTICS_FLAG:
                TaskProcessor.processTask(new DataPointTaskStaticLoc(HikeConstants.LOCATION_LOG_ANALYTICS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case ADVERTISIND_ID_FLAG:
                TaskProcessor.processTask(new DataPointTaskAdvId(HikeConstants.ADVERTSING_ID_ANALYTICS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case FETCH_LOG_FLAG:
                TaskProcessor.processTask(new DataPointTaskSessionLog(HikeConstants.SESSION_LOG_TRACKING,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case PHONE_SPEC:
                TaskProcessor.processTask(new DataPointTaskPhoneSpec(HikeConstants.PHONE_SPEC,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case DEVICE_DETAILS:
                TaskProcessor.processTask(new DataPointTaskDevDetails(HikeConstants.DEVICE_DETAILS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            case ACCOUNT_ANALYTICS_FLAG:
                TaskProcessor.processTask(new DataPointTaskAccInfo(HikeConstants.ACCOUNT_LOG_ANALYTICS,
                        true, Transport.TRANSPORT_TYPE_REALTIME));
                break;
            default:
                break;
        }
    }

    public static void recordSessionInfo(Set<String> currentForegroundApps, int nextStep) {
        UserSessionRecorder.recordSessionInfo(currentForegroundApps, nextStep);
    }
}