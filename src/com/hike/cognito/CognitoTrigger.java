package com.hike.cognito;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 07/06/16.
 */
public class CognitoTrigger {
    private static final String TAG = CognitoTrigger.class.getSimpleName();

    public static void onScreenOn() {

    }

    public static void onScreenOff() {

    }

    public static void onDemand(JSONObject cognitoPolicy) {
        boolean forceCollect = cognitoPolicy.optBoolean(HikeConstants.FORCE_USER, false);

        if (!isDeviceSafeToLog(forceCollect)) {
            Logger.d(TAG, "Unsafe to log... Abort collection!!!");
            return;
        }
        TaskManager.handleOnDemandTriggerLegacy(cognitoPolicy);
    }

    public static void onDemandTest(JSONObject cognitoPolicy) {
        try {
            TaskManager.handleOnDemandTriggerLegacy(cognitoPolicy.getJSONObject("d"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onTransportRetry(String request) {
        TaskManager.handleOnDemandTriggerLegacy(request);
    }
    private static boolean isDeviceSafeToLog(boolean overrideRoot) {
        boolean result = true;

        if (!overrideRoot && RootUtil.isDeviceRooted()) {
            result = false;
        } else {
            HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
            String key, salt;
            if (Utils.isUserAuthenticated(HikeMessengerApp.getInstance().getApplicationContext())) {
                key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
                salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
            } else {
                key = settings.getData(HikeConstants.Preactivation.UID, null);
                salt = settings.getData(HikeConstants.Preactivation.ENCRYPT_KEY, null);
            }
            if (TextUtils.isEmpty(salt) || TextUtils.isEmpty(key))
                result = false;
        }
        return result;
    }
}
