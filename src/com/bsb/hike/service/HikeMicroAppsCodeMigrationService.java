package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformContentCache;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by konarkarora on 02/11/15.
 */
public class HikeMicroAppsCodeMigrationService extends IntentService
{
	public HikeMicroAppsCodeMigrationService()
	{
		super("HikeMicroAppsCodeMigrationService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
        // keeping this variables as true bcoz there might be case in which user does not have any data that needs to be migrated
		boolean isMicroAppsSuccessfullyMigrated = true;
        boolean isDPDirectoryMigrated = true;
        boolean isGameEngineMigrated = true;
		HashMap<String, Boolean> mapForMigratedApps = new HashMap<String, Boolean>();
        String unzipPath = PlatformUtils.getMicroAppContentRootFolder();

        // Migrating static files (DP directory) here
        try
        {
            if (new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + PlatformContentConstants.MICROAPPS_DP_DIR).exists())
            {
                isDPDirectoryMigrated = PlatformUtils.copyDirectoryTo(new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + PlatformContentConstants.MICROAPPS_DP_DIR),
                        new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.MICROAPPS_DP_DIR));
            }
        }
        catch (IOException e)
        {
            PlatformUtils.microappsMigrationFailedAnalytics(e.toString());
            e.printStackTrace();
        }

		/*
		 * Iterating and doing the migration over the key set of hikeBotInfoMap currently presenxt in BotTable
		 */
		for (Map.Entry<String, BotInfo> entry : HikeMessengerApp.hikeBotInfoMap.entrySet())
		{
			NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(entry.getValue().getMetadata());

			if (entry.getValue().isNonMessagingBot())
			{
				try
				{
                    mapForMigratedApps.put(entry.getKey(), false);
					// Generate file instance for destination file directory path that would be used after versioning release
					String botName = entry.getValue().getMsisdn();
                    botName =  botName.substring(1, botName.length()-1);

                    // Keeping default bot type as web micro apps
                    byte botType = HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS;

                    // For Native micro apps, if game engine is not already migrated , migrate it as well
                    if (botMetadata.isNativeMode())
                    {
                        botType = HikePlatformConstants.PlatformBotType.NATIVE_APPS;

                        JSONArray mapps = botMetadata.getAsocmapp();
                        if (mapps != null)
                        {
                            for (int i = 0; i < mapps.length(); i++)
                            {
                                JSONObject json = new JSONObject();
                                try
                                {
                                    json = mapps.getJSONObject(i);
                                }
                                catch (JSONException e)
                                {
                                    e.printStackTrace();
                                }
                                String appName = json.optString(HikeConstants.NAME);
                                if (new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + appName).exists())
                                {
                                    // If there's already a directory present in new path, there's a chance it might be partial one so deleting it before copying it again
                                    if(new File(PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS, unzipPath, appName)).exists())
                                    {
                                        PlatformUtils.deleteDirectory(PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS, unzipPath, appName));
                                    }

                                    isGameEngineMigrated = PlatformUtils.copyDirectoryTo(new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + appName),
                                            new File(PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS, unzipPath, appName)));

                                    // delete game engine from the old content code if it successfully got migrated
                                    if(isGameEngineMigrated && !TextUtils.isEmpty(appName))
                                        PlatformUtils.deleteDirectory(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + appName);
                                }
                            }
                        }
                    }

                    String newVersioningCodePath = PlatformUtils.generateMappUnZipPathForBotType(botType, unzipPath, botName);

					File newFilePath = new File(newVersioningCodePath);

					// File instance for source file directory path that was in use before versioning release
					String microAppName = botMetadata.getAppName();

                    if(TextUtils.isEmpty(microAppName))
                        continue;

					File originalFile = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + microAppName);

					// Copy this code from source to destination
                    boolean isCopied = false;
                    if(originalFile.exists())
                    {
                        isCopied = PlatformUtils.copyDirectoryTo(originalFile, newFilePath);
                    }
                    else
                    {
                        // Since the file name have not been found on old content directory , here we are assuming that this file has been migrated in the last time and setting its value as true in hashmap
                        mapForMigratedApps.put(entry.getKey(), true);
                    }

                    if (isCopied)
                    {
                        botMetadata.setAppName(botName);

                        // Update appPackage Url on migration
                        String appPackage = botMetadata.getAppPackage();
                        if(!TextUtils.isEmpty(appPackage))
                        {
                            int endIndex = appPackage.lastIndexOf("/");
                            if (endIndex != -1)
                            {
                                appPackage = appPackage.substring(0, endIndex + 1) + botName + ".zip";
                            }
                        }
                        botMetadata.setAppPackage(appPackage);

                        JSONObject json = botMetadata.getJson();
                        if (json.has(HikePlatformConstants.CARD_OBJECT))
                        {
                            JSONObject cardObj = json.optJSONObject(HikePlatformConstants.CARD_OBJECT);

                            if (cardObj.has(HikePlatformConstants.APP_NAME))
                            {
                                cardObj.put(HikePlatformConstants.APP_NAME, botName);
                                cardObj.put(HikePlatformConstants.APP_PACKAGE, appPackage);
                            }
                        }

						// Update metadata for this bot in the database and bots Hash Map
						String botMetadataJson = json.toString();
						HikeConversationsDatabase.getInstance().updateBotMetaData(entry.getKey(), botMetadataJson);
						entry.getValue().setMetadata(botMetadataJson);
						mapForMigratedApps.put(entry.getKey(), true);

                        // Delete micro app file if from the old content code if it successfully got migrated
                        if(!TextUtils.isEmpty(microAppName))
                            PlatformUtils.deleteDirectory(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + microAppName);
					}
				}
				catch (FileNotFoundException fnfe)
				{
                    Logger.e("Hike Micro apps code Migration Service File Not Found Exception" ,fnfe.toString());
                    PlatformUtils.microappsMigrationFailedAnalytics(fnfe.toString());
                    fnfe.printStackTrace();
				}
                catch (JSONException e)
                {
                    Logger.e("Hike Micro apps code Migration Service JSONException",e.toString());
                    PlatformUtils.microappsMigrationFailedAnalytics(e.toString());
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    Logger.e("Hike Micro apps code Migration Service IOException",e.toString());
                    PlatformUtils.microappsMigrationFailedAnalytics(e.toString());
                    e.printStackTrace();
                }
                catch (Exception e)
                {
                    PlatformUtils.microappsMigrationFailedAnalytics(e.toString());
                    e.printStackTrace();
                }
			}
		}
		for (Map.Entry<String, Boolean> entry : mapForMigratedApps.entrySet())
		{
            if (!entry.getValue())
			{
				isMicroAppsSuccessfullyMigrated = false;
				break;
			}
		}

		/*
		 * Check if migration is successful , save the flag as true in Shared Preferences and delete old directory content code  , else set alarm for making it happen in future
		 */
		if (isMicroAppsSuccessfullyMigrated && isDPDirectoryMigrated && isGameEngineMigrated)
		{
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, true);
            PlatformUtils.deleteDirectory(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR);
            PlatformContentCache.clearFormedContentCache();
            PlatformUtils.platformDiskConsumptionAnalytics(AnalyticsConstants.MICROAPPS_MIGRATION_SUCCESS_TRIGGER);
            HikeMessengerApp.getInstance().hikeBotInfoMap.clear();
            HikeConversationsDatabase.getInstance().getBotHashmap();
		}
		else
		{
			long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(), 4, 45, 30, 0);
			// If the scheduled time is in the past
			// Scheduled time is increased by 24 hours i.e. same time next day.
			if (scheduleTime < System.currentTimeMillis())
			{
				scheduleTime += 24 * 60 * 60 * 1000;
			}

			HikeAlarmManager.setAlarm(this, scheduleTime, HikeAlarmManager.REQUEST_CODE_MICROAPPS_MIGRATION, true);
            PlatformUtils.microappsMigrationFailedAnalytics("Migration failed because of logical issue");
        }
	}

}
