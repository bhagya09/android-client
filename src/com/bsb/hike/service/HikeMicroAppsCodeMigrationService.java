package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by konarkarora on 02/11/15.
 */
public class HikeMicroAppsCodeMigrationService extends IntentService{

    public HikeMicroAppsCodeMigrationService()
    {
        super("HikeMicroAppsCodeMigrationService");
    }

    public HikeMicroAppsCodeMigrationService(String name)
    {
        super("HikeMicroAppsCodeMigrationService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        /**if the service is restarted then a null intent is returned

         http://developer.android.com/reference/android/app/IntentService.html#onStartCommand(android.content.Intent,%20int,%20int)
         */
        if (intent == null)
        {
            return;
        }else{

            String mappingJsonString = Utils.loadJSONFromAsset(this, "microAppsMigrationMapping");

            // stop the code execution and returns if migration mapping file is not found
            if(mappingJsonString == null || mappingJsonString.equals(""))
                return;

            HashMap<String,HashMap<String,Integer>> migrationMap= getMapFromString(mappingJsonString);
            boolean isSuccessfullyMigrated = false;
            HashMap<String,Boolean> successfullyMigrated = new HashMap<String,Boolean>();

            /*
             * Iterating and doing the migration over the key set of hikeBotInfoMap currently present in BotTable
             */
            for (Map.Entry<String, BotInfo> entry : HikeMessengerApp.hikeBotInfoMap.entrySet())
            {
                if(!migrationMap.containsKey(entry.getKey()))
                    continue;

                HashMap<String,Integer> msisdnMigrationMap = migrationMap.get(entry.getKey());
                String msisdnMetaData = entry.getValue().getMetadata();
                successfullyMigrated.put(entry.getKey(),false);

                if(entry.getValue().isNonMessagingBot()) {
                    try {
                        // To determine zip name and older file path by parsing json
                        JSONObject jsonObj = new JSONObject(msisdnMetaData);
                        JSONObject cardObj = jsonObj.optJSONObject(HikePlatformConstants.CARD_OBJECT);
                        String appPackage = cardObj.optString(HikePlatformConstants.APP_PACKAGE);
                        String microAppName = cardObj.optString(HikePlatformConstants.APP_NAME);
                        String zipFileName = appPackage.substring(appPackage.lastIndexOf("/") + 1, appPackage.length());
                        zipFileName = zipFileName.substring(0, zipFileName.lastIndexOf('.'));

                        if(!msisdnMigrationMap.containsKey(msisdnMigrationMap))
                            continue;

                        int version = msisdnMigrationMap.get(msisdnMigrationMap);
                        String unzipPath = getMicroAppContentRootFolder();
                        // Create directory for micro app if not exists already
                        new File(unzipPath, microAppName).mkdirs();

                        // Create directory for this version for specific micro app
                        unzipPath += microAppName + File.separator;
                        new File(unzipPath, "Version_" + version).mkdirs();
                        unzipPath += "Version_" + version + File.separator;

                        // File instance for destination file directory path
                        File newFilePath = new File(unzipPath);

                        // File instance for source file directory path
                        File originalFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + zipFileName);

                        // Copy from source to destination
                        boolean isCopied = PlatformUtils.copyDirectoryTo(originalFile, newFilePath);
                        if(isCopied)
                            successfullyMigrated.put(entry.getKey(),true);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            for (Map.Entry<String, Boolean> entry : successfullyMigrated.entrySet())
            {
                if(entry.getValue())
                    isSuccessfullyMigrated = true;
                else {
                    isSuccessfullyMigrated = false;
                    break;
                }
            }


            if(isSuccessfullyMigrated)
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, true);
            else
            {
                long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(), 4, 40, 30, 0);
                // If the scheduled time is in the past.
                // Scheduled time is increased by 24 hours i.e. same time next day.
                scheduleTime += 24 * 60 * 60 * 1000;

                HikeAlarmManager.setAlarm(getApplicationContext(), scheduleTime, HikeAlarmManager.REQUEST_CODE_MICROAPPS_MIGRATION, false);

            }


        }

    }

    /*
     * Code to generate mapping matrix TreeMap from json
     */
    private HashMap<String,HashMap<String,Integer>> getMapFromString(String json){
        Gson gson = new Gson();
        Type migrationMap = new TypeToken<HashMap<String,HashMap<String,Integer>>>(){}.getType();
        HashMap<String,HashMap<String,Integer>> map = gson.fromJson(json, migrationMap);
        return map;
    }

    /**
     * Returns the root folder path for Hike MicroApps <br>
     * eg : "/data/data/com.bsb.hike/files/Content/HikeMicroApps/"
     *
     * @return
     */
    private String getMicroAppContentRootFolder()
    {
        File file = new File (PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS);
        if (!file.exists())
        {
            file.mkdirs();
        }

        return PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS ;
    }

}
