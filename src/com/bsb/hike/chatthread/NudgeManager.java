package com.bsb.hike.chatthread;

import android.content.Context;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TODO
 */
public class NudgeManager {

    private static final int NUDGE_COOL_OFF_TIME = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NUDGE_SEND_COOLOFF_TIME, 300);
    private static final int NUDGE_TOAST_OCCURRENCE = 2;
    private final Context context;
    private long lastNudgeTime = -1;
    private int currentNudgeCount;
    private boolean doubleTapPref;

    public NudgeManager(final Context c) {
        this.context = c;
    }

    public boolean shouldSendNudge() {

        if (System.currentTimeMillis() - lastNudgeTime < NUDGE_COOL_OFF_TIME && lastNudgeTime > 0) {
            return false;
        }

        if (!doubleTapPref) {
            try {
                JSONObject metadata = new JSONObject();
                metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.UNCHECKED_NUDGE);
                HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
            } catch (JSONException ex) {
                Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json", ex);
            }
            currentNudgeCount++;
            if (currentNudgeCount > NUDGE_TOAST_OCCURRENCE) {
                Toast.makeText(context.getApplicationContext(), R.string.nudge_toast, Toast.LENGTH_SHORT).show();
                currentNudgeCount = 0;
            }
            return false;
        }
        lastNudgeTime = System.currentTimeMillis();

        return true;
    }

    public void updateLatestNudgeSetting() {
        doubleTapPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getBoolean(HikeConstants.DOUBLE_TAP_PREF, true);
    }
}