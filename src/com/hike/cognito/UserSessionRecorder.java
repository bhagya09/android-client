package com.hike.cognito;

import android.app.KeyguardManager;
import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class UserSessionRecorder {
    private static final String TAG = UserSessionRecorder.class.getSimpleName();
    private static Map<String, Long> foregroundAppsStartTimeMap;
    private static long MIN_SESSION_RECORD_TIME = 2000;

    public static void recordSessionInfo(Set<String> currentForegroundApps, int nextStep) {
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false)) {
            Logger.d(TAG, "recordSessionInfo: do nothing!");
            return;
        }

        KeyguardManager kgMgr = (KeyguardManager) HikeMessengerApp.getInstance().getApplicationContext().
                getSystemService(Context.KEYGUARD_SERVICE);

        boolean lockScreenShowing = kgMgr.inKeyguardRestrictedInputMode();
        long currentTime = System.currentTimeMillis();

        if (foregroundAppsStartTimeMap == null) {
            foregroundAppsStartTimeMap = new HashMap<String, Long>(5);
        }

        Set<String> savedForegroundApps = new HashSet<String>(foregroundAppsStartTimeMap.keySet());

        //this if logic can also be called when the activity has already started
        if (nextStep == UserLogInfo.START || foregroundAppsStartTimeMap.isEmpty()) {
            foregroundAppsStartTimeMap.clear();
            for (String packageName : currentForegroundApps) {
                if (!lockScreenShowing) {
                    foregroundAppsStartTimeMap.put(packageName, currentTime);
                }
            }
        } else if (nextStep == UserLogInfo.STOP) {
            for (String app : foregroundAppsStartTimeMap.keySet()) {
                recordASession(app, foregroundAppsStartTimeMap.get(app));
            }
            foregroundAppsStartTimeMap.clear();
        } else if (nextStep == UserLogInfo.OPERATE && !currentForegroundApps.isEmpty()) {
            savedForegroundApps.addAll(currentForegroundApps);
            for (String app : savedForegroundApps) {
                if (currentForegroundApps.contains(app) && !foregroundAppsStartTimeMap.containsKey(app) && !lockScreenShowing) {
                    // foregrounded app here
                    foregroundAppsStartTimeMap.put(app, System.currentTimeMillis());
                } else if (!currentForegroundApps.contains(app) && foregroundAppsStartTimeMap.containsKey(app)) {
                    // backgrounded apps here
                    recordASession(app, foregroundAppsStartTimeMap.get(app));
                    foregroundAppsStartTimeMap.remove(app);
                }
            }
        }
    }

    private static void recordASession(String packageName, long sesstionTime) {
        long sessionTime = System.currentTimeMillis() - sesstionTime;

        if (sessionTime > MIN_SESSION_RECORD_TIME) {
            HikeSharedPreferenceUtil userPrefs = HikeSharedPreferenceUtil.getInstance(UserLogInfo.USER_LOG_SHARED_PREFS);
            String[] loggedParams = userPrefs.getData(packageName, "0:0").split(":");
            for (String s : loggedParams) {
                //Do your stuff here
                Logger.d(TAG, s);
            }
            long duration = Long.parseLong(loggedParams[0]) + sessionTime;
            int sessions = Integer.parseInt(loggedParams[1]) + 1;
            userPrefs.saveData(packageName, duration + ":" + sessions);
            Logger.d(TAG, "time : " + sessionTime + " of " + packageName);
        }
    }
}
