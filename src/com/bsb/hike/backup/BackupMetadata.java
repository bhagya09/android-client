package com.bsb.hike.backup;

import android.content.Context;
import android.content.pm.PackageManager;

import com.bsb.hike.BuildConfig;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representing metadata about the backup.
 * <p>
 * There are three pieces of meta-data:
 * 1. Version of app that took the back-up
 * 2. Timestamp of when the backup was taken
 * 3. MSISDN of user when the backup was taken
 */
public class BackupMetadata {

    private static final String LOGTAG = BackupMetadata.class.getSimpleName();

    public static final String TIMESTAMP = "ts";
    public static final String VERSION = "version";
    public static final String MSISDN = "msisdn";

    private Context mContext;

    private int mAppVersion;
    private String mMsisdn;
    private long mBackupTimeStamp;

    /**
     * Construct object with default values
     * @param c
     */
    public BackupMetadata(Context c) {
        mContext = c;
        try {
            mAppVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            mMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "");
            mBackupTimeStamp = System.currentTimeMillis();
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(LOGTAG, "Failed to read app version", e);
            mAppVersion = BuildConfig.VERSION_CODE;
        }
    }

    /**
     * Construct {@code BackupMetadata} object from it's {@code json} representation.
     * @param c
     * @param userDataJsonString
     */
    public BackupMetadata(Context c, String userDataJsonString) {

        mContext = c;

        try {
            JSONObject dataJson = new JSONObject(userDataJsonString);

            if (dataJson.has(VERSION)) {
                mAppVersion = dataJson.getInt(VERSION);
            }

            if (dataJson.has(MSISDN)) {
                mMsisdn = dataJson.getString(MSISDN);
            }

            if (dataJson.has(TIMESTAMP)) {
                mBackupTimeStamp = dataJson.getLong(TIMESTAMP);
            }
        } catch (JSONException e) {
            Logger.e(LOGTAG, "Failed to construct BackupMetadata Object", e);
        }
    }

    /**
     * Returns a JSON String representation of this object
     * @return json representation of this object
     */
    @Override
    public String toString() {
        JSONObject dataJson = new JSONObject();
        try {
            dataJson.put(VERSION, mAppVersion);
            dataJson.put(MSISDN, mMsisdn);
            dataJson.put(TIMESTAMP, mBackupTimeStamp);
        }  catch (JSONException e) {
            Logger.e(LOGTAG, "Failed to construct dataJson", e);
        } finally {
            return dataJson.toString();
        }
    }

    public int getAppVersion() {
        return mAppVersion;
    }

    public long getBackupTime() {
        return mBackupTimeStamp;
    }

    public String getMsisdn()
    {
        return mMsisdn;
    }
}
