package com.bsb.hike.analytics;

import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.DEFAULT_HTTP_ANALYTICS;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * @author himanshu
 *         <p>
 *         This class is responsible for recording the activity opening time and dump into Analytics
 */
public class RecordActivityOpenTime {
    private static final String TAG = "RecordActivityOpenTime";

	private final int MAX_ACTIVITY_OPEN = 1000;

    private final String LATENT_AREA="latarea";
    
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
        HAManager.getInstance().logDevEvent(HikeConstants.ACTIVITY_LATENT_AREA, LATENT_AREA, infoJson);
    }

    public boolean shouldStart() {
        // Now using a Random number to generate probability

        int randomInt = new Random().nextInt(MAX_ACTIVITY_OPEN);
        int maxAllowed = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PROB_ACTIVITY_OPEN, HikeConstants.DEFAULT_ACTIVITY_OPEN);
        Logger.d(TAG,"Random Int generated is"+randomInt+"Max Allowed is "+maxAllowed);

        if (randomInt > maxAllowed)
        {
            return false;
        }
        return true;

    }

    public void onDestroy() {
        startTime = -1;
        endTime = -1;
        activity = null;
    }
}
