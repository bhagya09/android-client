package com.hike.cognito.datapoints;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.hike.cognito.UserSessionRecorder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskSessionLog extends DataPointTask {
    private static final String TAG = DataPointTaskSessionLog.class.getSimpleName();
    private static final String PACKAGE_NAME = "pn";
    private static final String APPLICATION_NAME = "an";

    private static final String SESSION_COUNT = "sn";
    private static final String DURATION = "dr";

    public DataPointTaskSessionLog(String url, Boolean isPii, Integer transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        HikeSharedPreferenceUtil userPrefs = HikeSharedPreferenceUtil.getInstance(UserSessionRecorder.USER_LOG_SHARED_PREFS);
        PackageManager pm = HikeMessengerApp.getInstance().getPackageManager();
        JSONArray sessionJsonArray = new JSONArray();

        for (Map.Entry<String, ?> entry : userPrefs.getPref().getAll().entrySet()) {
            try {
                String[] sessionInfo = entry.getValue().toString().split(":");
                ApplicationInfo ai = pm.getApplicationInfo(entry.getKey(), PackageManager.GET_UNINSTALLED_PACKAGES);
                String applicationName = ai.loadLabel(HikeMessengerApp.getInstance().getPackageManager()).toString();
                sessionJsonArray.put(toJSON(entry.getKey(), applicationName, Long.parseLong(sessionInfo[0]), Integer.parseInt(sessionInfo[1])));
            } catch (PackageManager.NameNotFoundException e) {
                Logger.d(TAG, "Application uninstalled or not found : " + entry.getKey());
            } catch (JSONException jse) {
                jse.printStackTrace();
            } catch (Exception e) {
                Logger.d(TAG, "Exception : " + e);
            }
        }

        if (sessionJsonArray == null || sessionJsonArray.length() == 0) {
            Logger.d(TAG, "No sessions recorded!!");
            return null;
        }

        Logger.d(TAG, sessionJsonArray.toString());
        //cleanup user data
        userPrefs.deleteAllData();
        return sessionJsonArray;
    }

    public JSONObject toJSON(String packageName, String applicationName, long duration, int sessions) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.putOpt(PACKAGE_NAME, packageName);
        jsonObj.putOpt(APPLICATION_NAME, applicationName);
        jsonObj.putOpt(SESSION_COUNT, sessions);
        jsonObj.putOpt(DURATION, duration);
        return jsonObj;
    }
}
