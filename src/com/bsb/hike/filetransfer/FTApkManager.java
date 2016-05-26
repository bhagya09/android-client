package com.bsb.hike.filetransfer;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.PhoneSpecUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;

public class FTApkManager
{

    private  static Context context = HikeMessengerApp.getInstance().getApplicationContext();

    public static void makeRequest(final double apkSizeMultiplier)
    {
        RequestToken requestToken = HttpRequests.getLatestApkInfo(new IRequestListener() {
            @Override
            public void onRequestSuccess(Response result) {

                try {
                    JSONObject metadata = new JSONObject();
                    metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
                    metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.SERVER_RESPONSE_HTTP);

                    HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);
                } catch (JSONException e) {
                    Logger.w("AUTOAPK", "Invalid json");
                }

                JSONObject apkJson = null;
                String apkDownloadUrl = "";
                long apkSize;
                try {
                    apkJson = new JSONObject(result.getBody().getContent().toString());
                    Logger.d("AUTOAPK", apkJson.toString());
                    apkDownloadUrl = apkJson.getString(HikeConstants.AutoApkDownload.DOWNLOAD_APK_URL);
                    String apkVersion = apkJson.optString(HikeConstants.AutoApkDownload.DOWNLOAD_APK_VERSION, Utils.getAppVersionName());
                    apkSize = apkJson.getLong(HikeConstants.AutoApkDownload.DOWNLOAD_APK_SIZE);
                    String[] parts = apkDownloadUrl.split("/");
                    String apkName = parts[3];
                    apkName += ("_" + System.currentTimeMillis()) + ".apk";

                    long freeSdCardSpace = PhoneSpecUtils.getSDCardMem().get("free");
                    boolean spaceAvailable = freeSdCardSpace > (apkSizeMultiplier * apkSize);

                    boolean shouldStartNewDownload = false;

                    if (Utils.isUpdateRequired(apkVersion, context) && spaceAvailable) {

                        Logger.d("AUTOAPK", "version checked,  space checked,  will check old downloads now ");
                        String hfPref = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}");
                        JSONObject jo = new JSONObject(hfPref);
                        HikeFile hfOld = new HikeFile(jo, false);

                        if (FileTransferManager.getInstance(context).getDownloadFileState(-100, hfOld).getTotalSize() > 0) {
                            Logger.d("AUTOAPK", "File already exists");
                            String oldDownloadingApkVersion = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, "");
                            try {
                                shouldStartNewDownload = Utils.isVersionNameHigher(oldDownloadingApkVersion, apkVersion, context);
                            } catch (NumberFormatException nfe) {
                                Logger.e("Utils", "version name contains strings possibly", nfe);
                                shouldStartNewDownload = false;
                            }
                            Logger.d("AUTOAPK", "for the new apk version " + apkVersion + ", old apk verison name is " + oldDownloadingApkVersion + ", should we start new download : " + shouldStartNewDownload);
                        } else {
                            shouldStartNewDownload = true;
                            Logger.d("AUTOAPK", "First time download");
                        }
                        if (shouldStartNewDownload) {
                            Logger.d("AUTOAPK", "on receive of new apk, the version name was found to be higher, hence cancelling and downloading");
                            FileTransferManager.getInstance(context).cancelTask(-100, hfOld, false, hfOld.getFileSize());
                            Logger.d("AUTOAPK", "also deleting the already downloaded file if it exists");
                            if (hfOld.getFile() != null && hfOld.getFile().exists()) {
                                hfOld.getFile().delete();
                            }

                            Logger.d("AUTOAPK", " download will start after network check, saving vargs");
                            HikeFile hf = new HikeFile(apkName, HikeFile.HikeFileType.toString(HikeFile.HikeFileType.APK), apkVersion, apkSize, apkDownloadUrl);

                            Logger.d("AUTOAPK", FileTransferManager.getInstance(context).getDownloadFileState(-100, hf).toString());

                            JSONObject hfApk = hf.serialize();
                            HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
                            mprefs.saveData(HikeConstants.AutoApkDownload.NEW_APK_JSON, hfApk.toString());
                            mprefs.saveData(HikeConstants.AutoApkDownload.NEW_APK_SIZE, apkSize);
                            mprefs.saveData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, apkVersion);

                            int networkType = Utils.getNetworkShort(mprefs.getData(HikeConstants.CONNECTION_TYPE, "off"));
                            int networkTypeAvailable = Utils.getNetworkShort(Utils.getNetworkTypeAsString(context));

                            Logger.d("AUTOAPK", mprefs.getData(HikeConstants.CONNECTION_TYPE, "default"));
                            if (networkType >= networkTypeAvailable && networkTypeAvailable > 0) {
                                Logger.d("AUTOAPK", "Starting download now, correct network detected");
                                FileTransferManager.getInstance(context).downloadApk(hf.getFile(), hf.getFileKey(), hf.getHikeFileType());

                                JSONObject metadata = new JSONObject();
                                metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
                                metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.INITIATING_DOWNLOAD);
                                metadata.put(AnalyticsConstants.AutoApkEvents.NETWORK_VALIDITY, networkTypeAvailable);

                                HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);
                            }
                        } else {
                            Logger.d("AUTOAPK", "not downloading the new APK, the version check failed, might continue with older apk");
                        }

                    } else {
                        if (!Utils.isUpdateRequired(apkVersion, context)) {
                            Logger.d("AUTOAPK", "version downgrade, not downloading");
                        } else if (!spaceAvailable) {
                            Logger.d("AUTOAPK", "not enough space, not downloading");
                        }
                    }
                } catch (JSONException je) {
                    Logger.d("AUTOAPK", "Faulty JSON");
                }


            }

            @Override
            public void onRequestProgressUpdate(float progress) {
                Logger.d("AUTOAPK", "progress " + progress);
            }

            @Override
            public void onRequestFailure(HttpException httpException) {
                Logger.d("AUTOAPK", "The error code received is " + httpException.getErrorCode());

            }
        });
        requestToken.execute();
        try
        {
            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
            metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.MAKING_SERVER_HTTP_REQUEST);

            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);
        }
        catch (JSONException je)
        {
            Logger.w("AUTOAPK", "Invalid json");
        }
    }

    public static void removeApkIfNeeded()
    {

        HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();

        if(pref.getData(HikeConstants.INSTALL_PROMPT_FREQUENCY, 0) < 1 && pref.getData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK, false))
        {
            Logger.d("AUTOAPK","delete apk and vars when freq is 0 on app launch");
            try {
                JSONObject jo = new JSONObject(pref.getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
                if (!jo.toString().equals("{}")) {
                    Logger.d("AUTOAPK", "hike file : " + jo.toString());
                    HikeFile hikefile = new HikeFile(jo, false);
                    if(hikefile.getFile() != null && hikefile.getFile().exists())
                    {
                        hikefile.getFile().delete();
                    }
                }
            } catch (JSONException je)
            {
                Logger.d("AUTOAPK","json exception on app open while deleting hfFile");
            }
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_JSON);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_VERSION);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_SIZE);
            pref.removeData(HikeConstants.CONNECTION_TYPE);
            pref.removeData(HikeConstants.APK_SIZE_MULTIPLIER);
            pref.removeData(HikeConstants.DOWNLOAD_TIME);
            pref.removeData(HikeConstants.INSTALL_PROMPT_FREQUENCY);
            pref.removeData(HikeConstants.INSTALL_PROMPT_METHOD);
            pref.removeData(HikeConstants.INSTALL_PROMPT_INTERVAL);
            pref.removeData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK);
            //TODO remove the tip now after install prompt freq is gone,
            //TODO effectively remove the alarm.
            //TODO delete the update_from fown

        }
    }

    public static void startInstall(boolean isNonPlayAppAllowed)
    {
        context = HikeMessengerApp.getInstance().getApplicationContext();

        try {
            HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
            //TODO special prompt for unknown source

            JSONObject jo = new JSONObject(pref.getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
            if(!jo.toString().equals("{}")) {
                Logger.d("AUTOAPK", "hike file : " + jo.toString());
                HikeFile hikefile = new HikeFile(jo, false);
                File apkExact = hikefile.getFileFromExactFilePath();
                File apk = hikefile.getFile();
                if (apk != null) {
                    Logger.d("AUTOAPK", "hike file paths  : " + apk.getAbsolutePath());
                }
                if (apkExact != null) {
                    Logger.d("AUTOAPK", "hike exact file path : " + apkExact.getAbsolutePath());
                }

                long apkSizeReceived = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_SIZE, 0l);
                boolean validSize =  (apk!=null && apk.exists() ) ? apk.length() == apkSizeReceived && apk.length() > 0 : false;
                Logger.d("AUTOAPK", "Is the apk size valid ? : "+validSize  + ", saved size is " + apkSizeReceived);
                boolean updateNeeded = Utils.isUpdateRequired(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, ""), context)
                        && !TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, ""));
                Logger.d("AUTOAPK", HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, "") + " is the apk version new, old is is " + Utils.getAppVersionName());

                if (apk != null && apk.exists() && validSize && updateNeeded) {
                    Logger.d("AUTOAPK", "hike APK downloaded exists");
                    if (HikeFile.HikeFileType.APK == hikefile.getHikeFileType()) {
                        Logger.d("AUTOAPK", "hike showing install prompt");
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        if(isNonPlayAppAllowed)
                        pref.saveData(HikeConstants.INSTALL_PROMPT_FREQUENCY, pref.getData(HikeConstants.INSTALL_PROMPT_FREQUENCY, 0) - 1);

                        Logger.d("AUTOAPK", "reducing the install prompt freq by 1, new freq : " + pref.getData(HikeConstants.INSTALL_PROMPT_FREQUENCY,0));
                    }
                }

            }


        }
        catch (JSONException je )
        {

            Logger.d("AUTOAPK", "json exceptn fond");
        }
    }

    public static void checkAndActOnDownloadedApk(File mFile) {
        Logger.d("AUTOAPK","downlioad success for an apk");
        //TODO, remove the ConnectionChangeReceive to prevent mutople file downloads, but thats not the only solution
        // also the retrigger file download logic on finding unequal md5 is wrong somewhere , need to investigate
        try
        {
            JSONObject jo = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
            HikeFile hikeApkFile = new HikeFile(jo, false);
            long apkSizeReceived = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_SIZE, 0l);
            File hFile = hikeApkFile.getFile();
            boolean validSize = (hFile != null && hFile.exists()) ?  hFile.length() == apkSizeReceived && hFile.length() > 0 : false;
            boolean updateNeeded = Utils.isUpdateRequired(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, ""), context)
                    && !TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, ""));
            boolean fileExists = hFile!=null && hFile.exists() && hFile.equals(mFile);
            if(fileExists)
            {
                if(validSize && updateNeeded)
                {
                    Logger.d("AUTOAPK","downloaded file is hike apk update, flushing old notif, tips");
                    MqttMessagesManager mmm =MqttMessagesManager.getInstance(context);
                    mmm.flushNotifOrTip(HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE);
                    mmm.flushNotifOrTip(HikeConstants.PERSISTENT_NOTIFICATION);
                    JSONObject apkTip = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON,"{}"));
                    mmm.performUpdateTask(apkTip, true);
                }
                else
                {
                    Logger.d("AUTOAPK", "deleting the file now, since file is not valid for installation");
                    hFile.delete();
                    HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
                    pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_JSON);
                    pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON);
                    pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_VERSION);
                    pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_SIZE);
                    pref.removeData(HikeConstants.CONNECTION_TYPE);
                    pref.removeData(HikeConstants.APK_SIZE_MULTIPLIER);
                    pref.removeData(HikeConstants.DOWNLOAD_TIME);
                    pref.removeData(HikeConstants.INSTALL_PROMPT_FREQUENCY);
                    pref.removeData(HikeConstants.INSTALL_PROMPT_METHOD);
                    pref.removeData(HikeConstants.INSTALL_PROMPT_INTERVAL);
                    pref.removeData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK);
                }
            }
            else
            {
                Logger.d("AUTOAPK", "size validity : " + validSize + ", update Needed : " + updateNeeded);
            }


            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
            metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.DOWNLOAD_COMPLETION);
            metadata.put(AnalyticsConstants.AutoApkEvents.SIZE_VALIDITY, validSize);
            metadata.put(AnalyticsConstants.AutoApkEvents.UPDATE_VALIDITY, updateNeeded);
            metadata.put(AnalyticsConstants.AutoApkEvents.FILE_VALIDITY, fileExists);
            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);

        }
        catch (JSONException je)
        {
            Logger.d("AUTOAPK","jsonExcetiopn on complete of download");
        }
    }

    public static void alarmForUpdate() {
        Logger.d("AUTOAPK", "alarm ringing for auto apk reshow tip");
        try
        {
            JSONObject jo = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
            HikeFile hikeApkFile = new HikeFile(jo, false);
            if(hikeApkFile.getFile().exists())
            {
                Logger.d("AUTOAPK","downloaded apk is exists apk, flushing old notif, tips after alarm");
                MqttMessagesManager mmm = MqttMessagesManager.getInstance(context);
                mmm.flushNotifOrTip(HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE);
                mmm.flushNotifOrTip(HikeConstants.PERSISTENT_NOTIFICATION);
                JSONObject apkTip = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON,"{}"));
                mmm.performUpdateTask(apkTip, true);
            }
        }
        catch (JSONException je)
        {
            Logger.d("AUTOAPK","jsonExcetiopn on complete of download");
        }
    }

    public static void checkUpdateSuccess(final SharedPreferences prefs) {
        //TODO remove all the old apks if downloaded
        String autoApkDownloadversion = prefs.getString(HikeConstants.AutoApkDownload.NEW_APK_VERSION, "");

        if(!TextUtils.isEmpty(autoApkDownloadversion) && !Utils.isUpdateRequired(autoApkDownloadversion, context))
        {

            HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
            try {
                JSONObject jo = new JSONObject(pref.getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
                if (!jo.toString().equals("{}")) {
                    Logger.d("AUTOAPK", "hike file : " + jo.toString());
                    HikeFile hikefile = new HikeFile(jo, false);


                    Logger.d("AUTOAPK", "delete apk and vars after updation has been perfromed");
                    if(hikefile.getFile() != null && hikefile.getFile().exists()) {
                        hikefile.getFile().delete();
                    }

                }
            } catch (JSONException je)
            {
                Logger.d("AUTOAPK", "json exception after upgrade");
            }

            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_JSON);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_VERSION);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON);
            pref.removeData(HikeConstants.AutoApkDownload.NEW_APK_SIZE);
            pref.removeData(HikeConstants.CONNECTION_TYPE);
            pref.removeData(HikeConstants.APK_SIZE_MULTIPLIER);
            pref.removeData(HikeConstants.DOWNLOAD_TIME);
            pref.removeData(HikeConstants.INSTALL_PROMPT_FREQUENCY);
            pref.removeData(HikeConstants.INSTALL_PROMPT_METHOD);
            pref.removeData(HikeConstants.INSTALL_PROMPT_INTERVAL);
            pref.removeData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK);

            pref.saveData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false);
            pref.saveData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
            HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.UPDATE_CRITICAL_TIP);
            CharSequence cr = "Congratulation! Now you are on the latest version of Hike";
            Toast t  = Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), cr, Toast.LENGTH_LONG);
            t.show();
        }
    }

    public static void handleRetryOnConnectionChange(HikeSharedPreferenceUtil mprefs) {
        Logger.d("AUTOAPK", Utils.getNetworkTypeAsString(context));
        try {
            JSONObject jo = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));

            if(!jo.toString().equals("{}")) {
                HikeFile hf = new HikeFile(jo, false);

                String stamp = hf.getFileName().substring(hf.getFileName().length() - 17, hf.getFileName().length() - 4);
                Logger.d("AUTOAPK", stamp);
                Long timeStamp = Long.parseLong(stamp);
                Logger.d("AUTOAPK", timeStamp + "");
                //TODO need to add a space constant too.
                float apkSizeMultiplier = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.APK_SIZE_MULTIPLIER, 3.0f );
                long apkSize = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_SIZE, 0l);
                long freeSdCardSpace = PhoneSpecUtils.getSDCardMem().get("free");
                long downloadedApkSize = FileTransferManager.getInstance(context).getDownloadFileState(-100, hf).getTotalSize();
                boolean spaceAvailable = freeSdCardSpace > (apkSizeMultiplier * apkSize) - downloadedApkSize;
                boolean timeExceeded =  System.currentTimeMillis() - timeStamp >= 1000 * 60 *60 * mprefs.getData(HikeConstants.DOWNLOAD_TIME, 72);
                if(timeExceeded || !spaceAvailable)
                {
                    FileTransferManager.getInstance(context).cancelTask(-100, hf, false, hf.getFileSize());

                    Logger.d("AUTOAPK", "delete apk and vars on connection retry");
                    if(hf.getFile()!=null && hf.getFile().exists()) {
                        hf.getFile().delete();
                    }
                    mprefs.removeData(HikeConstants.AutoApkDownload.NEW_APK_JSON);
                    mprefs.removeData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK);
                    mprefs.removeData(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON);
                    mprefs.removeData(HikeConstants.AutoApkDownload.NEW_APK_VERSION);
                    mprefs.removeData(HikeConstants.AutoApkDownload.NEW_APK_SIZE);
                    mprefs.removeData(HikeConstants.CONNECTION_TYPE);
                    mprefs.removeData(HikeConstants.APK_SIZE_MULTIPLIER);
                    mprefs.removeData(HikeConstants.DOWNLOAD_TIME);
                    mprefs.removeData(HikeConstants.INSTALL_PROMPT_INTERVAL);
                    mprefs.removeData(HikeConstants.INSTALL_PROMPT_METHOD);
                    mprefs.removeData(HikeConstants.INSTALL_PROMPT_FREQUENCY);
                }
                else {
                    int networkType = Utils.getNetworkShort(mprefs.getData(HikeConstants.CONNECTION_TYPE, "off"));
                    int networkTypeAvailable = Utils.getNetworkShort(Utils.getNetworkTypeAsString(context));

                    Logger.d("AUTOAPK", mprefs.getData(HikeConstants.CONNECTION_TYPE, "default"));


                    long apkSizeReceived = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_SIZE, 0l);
                    boolean downloadPending;
                    if(hf.getFile()!=null && hf.getFile().exists())
                    {
                        downloadPending =  hf.getFile().length() < apkSizeReceived;
                    }
                    else
                    {
                        downloadPending = true;
                    }
                    // TODO a better logic still pending for checking if download is required again, other wise it might cause multiple downloads
                    //TODO possible solution is : on download complete one should switch off the network change receiver
                    if (networkType >= networkTypeAvailable && networkTypeAvailable > 0 && downloadPending) {
                        Logger.d("AUTOAPK", "Starting download now, correct network detected");

                        JSONObject metadata = new JSONObject();
                        metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
                        metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.RESUMING_DOWNLOAD);
                        metadata.put(AnalyticsConstants.AutoApkEvents.NETWORK_VALIDITY, networkTypeAvailable);
                        HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);

                        FileTransferManager.getInstance(context).downloadApk(hf.getFile(), hf.getFileKey(), hf.getHikeFileType());
                    }
                }}

        }
        catch (JSONException je)
        {
            Logger.d("AUTOAPK","json exception" + je.getMessage());
        }
        catch (NumberFormatException nfe)
        {
            Logger.d("AUTOAPK","number format exception on parsing long");
        }
    }

    public static void onUpdateTipClick(Context context)
    {

        boolean isNonPlayAppAllowed = false;
        try
        {
            isNonPlayAppAllowed = Settings.Secure.getInt(HikeMessengerApp.getInstance().getApplicationContext().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
        }
        catch (Settings.SettingNotFoundException e)
        {
            Logger.d("AUTOAPK", "setting not fond");
        }
        Logger.d("AUTOAPK", "unknown sources  allowed : " + isNonPlayAppAllowed);
        if(isNonPlayAppAllowed)
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
            HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.UPDATE_CRITICAL_TIP);
        }
        FTApkManager.startInstall(isNonPlayAppAllowed);
        //start install will reduce the install prompt freq by 1

        if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INSTALL_PROMPT_FREQUENCY, 0) > 0 && isNonPlayAppAllowed)
        {
            long interval = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INSTALL_PROMPT_INTERVAL, 0l);
            HikeAlarmManager.setAlarmPersistance(context, Calendar.getInstance().getTimeInMillis() + interval * 1000, HikeAlarmManager.REQUESTCODE_UPDATE_AUTO_APK_TIP, false, true);

        }
    }
}
