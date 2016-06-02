package com.bsb.hike.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 */
public class AnalyticsSender {
    private Context context;

    private static AnalyticsSender _instance;

    /**
     * parameterized constructor of the class
     *
     * @param context application context
     */
    private AnalyticsSender(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * static constructor of AnalyticsSender class
     *
     * @param context application context
     * @return singleton instance of AnalyticsSender
     */
    public static AnalyticsSender getInstance(Context context) {
        if (_instance == null) {
            synchronized (AnalyticsSender.class) {
                if (_instance == null) {
                    _instance = new AnalyticsSender(context.getApplicationContext());
                }
            }
        }
        return _instance;
    }

    /**
     * Used to check if there is analytics data logged on client
     *
     * @return true if there is analytics data logged, false otherwise
     */
    private boolean isAnalyticsUploadReady() {
        boolean isPossible = true;

        // get files absolute paths
        String[] fileNames = HAManager.getFileNames(context);

        if (fileNames == null || fileNames.length == 0) {
            isPossible = false;
        }
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "DO FILES EXIT :" + isPossible);
        return isPossible;
    }

    /**
     * starts the analytics data upload to server as per the set alarm and schedules the next alarm
     */
    public void startUploadAndScheduleNextAlarm() {
        HAManager instance = HAManager.getInstance();

        // if user is offline, save true to prefs so that logs could be sent when Internet is available
        if (!Utils.isUserOnline(context)) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is offline, set true in prefs and return");
            HAManager.getInstance().setIsSendAnalyticsDataWhenConnected(true);
        }
        // user is connected
        else {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is online.....");

            // if there are no logs on disk, set next alarm and return
            if (!isAnalyticsUploadReady()) {
                scheduleNextAlarm();
                return;
            }
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOADING FROM ALARM ROUTE---");
            instance.sendAnalyticsData(true, false);
        }
    }

    public void scheduleNextAlarm() {
        HAManager haManager = HAManager.getInstance();

        long nextSchedule = haManager.getWhenToSend();
        nextSchedule = System.currentTimeMillis() + haManager.getAnalyticsSendFrequency() * AnalyticsConstants.ONE_MINUTE;
        haManager.setNextSendTimeToPrefs(nextSchedule);
        HikeAlarmManager.setAlarm(context, nextSchedule, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);

        // don't remove! added for testing purpose.
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm set at :" + nextSchedule);
    }
}

class NetworkListener extends BroadcastReceiver {
    Context context;

    public NetworkListener(Context context) {
        this.context = context;

        this.context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (Utils.isUserOnline(context)) {
                HAManager instance = HAManager.getInstance();

                if (instance.isSendAnalyticsDataWhenConnected()) {
                    instance.sendAnalyticsData(true, false);
                }
            }
        }
    }
}
