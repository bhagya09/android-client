package com.hike.cognito.datapoints;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.utils.PhoneSpecUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskAllApps extends DataPointTask {
    private static final String PACKAGE_NAME = "pn";
    private static final String APPLICATION_NAME = "an";
    private static final String INSTALL_TIME = "it";
    private static final String RUNNING_APPS = "ra";

    private final static byte RUNNING_PROCESS_BIT = 0;
    private final static byte FOREGROUND_TASK_BIT = 1;

    public DataPointTaskAllApps(String url, Boolean isPii, Integer transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
        List<PackageInfo> packInfoList = ctx.getPackageManager().getInstalledPackages(0);
        Set<String> runningPackageNames = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_ALL_RUNNING_PROCESSES);
        Set<String> currentRunningtasks = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_FOREGROUND_PROCESSES);
        JSONArray appLogsJsonArray = new JSONArray();

        for (PackageInfo pi : packInfoList) {
            int appStatus = 0;
            if (pi.versionName == null)
                continue;
            if (runningPackageNames.contains(pi.packageName)) {
                appStatus = PhoneSpecUtils.getNumberAfterSettingBit(appStatus, RUNNING_PROCESS_BIT, true);
                runningPackageNames.remove(pi.packageName);
            }
            if (currentRunningtasks.contains(pi.packageName)) {
                appStatus = PhoneSpecUtils.getNumberAfterSettingBit(appStatus, FOREGROUND_TASK_BIT, true);
                currentRunningtasks.remove(pi.packageName);
            }

            try {
                appLogsJsonArray.put(toJSON(pi.packageName,
                        pi.applicationInfo.loadLabel(ctx.getPackageManager()).toString(),
                        new File(pi.applicationInfo.sourceDir).lastModified(), appStatus));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (appLogsJsonArray == null || appLogsJsonArray.length() == 0) {
            return null;
        }
        return appLogsJsonArray;
    }

    public JSONObject toJSON(String packageName, String applicationName,
                             long installTime, int running) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.putOpt(PACKAGE_NAME, packageName);
        jsonObj.putOpt(APPLICATION_NAME, applicationName);
        jsonObj.putOpt(INSTALL_TIME, installTime);
        jsonObj.putOpt(RUNNING_APPS, running);
        return jsonObj;
    }
}
