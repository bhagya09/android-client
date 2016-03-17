package com.bsb.hike.analytics;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author himanshu
 *         <p>
 *         This class is responsible for recording the activity opening time and dump into Analytics
 */
public class RecordActivityOpenTime {
    private static final String TAG = "RecordActivityOpenTime";

    private String activity = null;

    private long startTime = -1;

    private long endTime = -1;

    public RecordActivityOpenTime(String activity) {
        this.activity = activity;
    }

    public void startRecording() {
        startTime = System.currentTimeMillis();
    }

    public void stopRecording() {
        endTime = System.currentTimeMillis();
    }

    public long getRecordedTime() {
        return (endTime - startTime);
    }

    public void dumpAnalytics() {
        if (TextUtils.isEmpty(activity) || getRecordedTime() <= 0) {
            return;
        }
        JSONObject infoJson = new JSONObject();
        try {
            infoJson.put("an", activity);
            infoJson.put("t", getRecordedTime());

        } catch (JSONException jsonEx) {
            Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json:", jsonEx);
            return;
        }
        //TODO need to figure out area,event with analytics
        HAManager.getInstance().logDevEvent(MqttConstants.CONNECTION_PROD_AREA, MqttConstants.EXCEPTION_DEV_AREA + "_100", infoJson);
    }

    /**
     * @return true/false shoud start the recording or not:
     *
     * ttl:Time Stamp in millis (Future Time Stamp)
     * <p>
     * Sample JSON {
     * "t": "ac",
     * "d": {
     * "screen": {
     * "HomeActivity": {
     * "mc": 2
     * },
     * "ChatThreadActivity": {
     * "mc": 2
     * }
     * },
     * "ttl": 1458209573000
     * }
     * }
     */

    public boolean shouldStart() {
        String str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME, "");
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(str);
            long ttl = jsonObject.getLong(HikeConstants.TIME_TO_LIVE);

            // If TTL is expired remove the pref completly
            if (System.currentTimeMillis() > ttl) {
                Logger.d(TAG, "TTL expired removing shared pref");
                HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME);
                return false;
            }

            JSONObject screen = jsonObject.getJSONObject(HikeConstants.SCREEN);

            //if screen array is not present then remove the key ? probably some problem with JSON.
            if (screen == null) {
                HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME);
                return false;
            }
            //Handling empty JSON case
            if (screen.length() <= 0) {
                HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME);
                return false;
            }
            JSONObject activityName = screen.getJSONObject(activity);

            if (activityName == null) {
                return false;
            }

            int maxCount = activityName.getInt(HikeConstants.MAX_COUNT);

            // removing the key if mc<=0 from the pref
            if (maxCount <= 0) {
                jsonObject.getJSONObject(HikeConstants.SCREEN).remove(activity);
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME, jsonObject.toString());
                return false;
            }
            maxCount -= 1;
            activityName.put(HikeConstants.MAX_COUNT, maxCount);

            //upading pref with latest max count

            jsonObject.put(activity, activityName);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.RECORD_ACTIVITY_OPEN_TIME, jsonObject.toString());
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }

    public void onDestroy() {
        startTime = -1;
        endTime = -1;
        activity = null;
    }
}
