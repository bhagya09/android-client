//NudgeManager
package com.bsb.hike.chatthread;

import android.content.Context;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class NudgeManager {
    private static final int NUDGE_TOAST_OCCURRENCE = 2;
    private final Context context;
    private final HikeSharedPreferenceUtil hikePreferences;
    private final HAManager haManager;
    private long lastNudgeTime = -1;
    private int currentNudgeCount;
    private boolean doubleTapPref;

    public NudgeManager(final Context context, HikeSharedPreferenceUtil hikePreferences, HAManager haManager) {
        this.context = context;
        this.hikePreferences = hikePreferences;
        this.haManager = haManager;
    }

    public boolean shouldSendNudge() {
        long timeSinceLastNudge = System.currentTimeMillis() - lastNudgeTime;
        int cool_off_time = hikePreferences.getData(HikeConstants.NUDGE_SEND_COOLOFF_TIME, 300);
        if (timeSinceLastNudge < cool_off_time)
            return false;

        if (doubleTapPref) {
            lastNudgeTime = System.currentTimeMillis();
            return true;
        }

        logDoubleClickEvent();
        if (++currentNudgeCount > NUDGE_TOAST_OCCURRENCE) {
            currentNudgeCount = 0;
            showToast(R.string.nudge_toast);
        }
        return false;
    }

    protected void showToast(int msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void logDoubleClickEvent() {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.UNCHECKED_NUDGE);
            haManager.record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
        } catch (JSONException ex) {
            Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json", ex);
        }
    }

    public void updateLatestNudgeSetting() {
        doubleTapPref = hikePreferences.getSharedPreferenceAsBoolean(HikeConstants.DOUBLE_TAP_PREF, true);
    }
}