package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by konarkarora on 02/11/15.
 */
public class HikeMicroAppsCodeMigrationService extends IntentService
{

	private final String microAppMigrationMappingFile = "microAppsMigrationMapping";

	public HikeMicroAppsCodeMigrationService()
	{
		super("HikeMicroAppsCodeMigrationService");
	}

	public HikeMicroAppsCodeMigrationService(String name)
	{
		super("HikeMicroAppsCodeMigrationService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{

		/**
		 * if the service is restarted then a null intent is returned
		 * 
		 * http://developer.android.com/reference/android/app/IntentService.html#onStartCommand(android.content.Intent,%20int,%20int)
		 */
		if (intent == null)
		{
			return;
		}
		else
		{
			String mappingJsonString = Utils.loadJSONFromAsset(this, microAppMigrationMappingFile);

			// stop the code execution and returns if migration mapping file is not found
			if (TextUtils.isEmpty(mappingJsonString))
				return;

			HashMap<String, HashMap<String, Integer>> migrationMap = getMapFromString(mappingJsonString);
			boolean isSuccessfullyMigrated = true;
			HashMap<String, Boolean> mapForMigratedApps = new HashMap<String, Boolean>();

			/*
			 * Iterating and doing the migration over the key set of hikeBotInfoMap currently present in BotTable
			 */
			for (Map.Entry<String, BotInfo> entry : HikeMessengerApp.hikeBotInfoMap.entrySet())
			{
				if (!migrationMap.containsKey(entry.getKey()))
					continue;

				HashMap<String, Integer> msisdnMigrationMap = migrationMap.get(entry.getKey());
				NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(entry.getValue().getMetadata());
				mapForMigratedApps.put(entry.getKey(), false);

				if (entry.getValue().isNonMessagingBot())
				{
					try
					{
						String microAppName = botMetadata.getAppName();

						if (!msisdnMigrationMap.containsKey(microAppName))
							continue;

						int version = msisdnMigrationMap.get(microAppName);
						String unzipPath = Utils.getMicroAppContentRootFolder();
						// Create directory for micro app if not exists already
						new File(unzipPath, microAppName).mkdirs();

						// Create directory for this version for specific micro app
						unzipPath += microAppName + File.separator;
						new File(unzipPath, HikeConstants.Extras.VERSIONING_DIRECTORY_NAME + version).mkdirs();
						unzipPath += HikeConstants.Extras.VERSIONING_DIRECTORY_NAME + version + File.separator;

						// File instance for destination file directory path
						File newFilePath = new File(unzipPath);

						// File instance for source file directory path
						File originalFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + microAppName);

						// Copy from source to destination
						boolean isCopied = PlatformUtils.copyDirectoryTo(originalFile, newFilePath);
						if (isCopied)
							mapForMigratedApps.put(entry.getKey(), true);

					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}

			for (Map.Entry<String, Boolean> entry : mapForMigratedApps.entrySet())
			{
				if (!entry.getValue())
				{
					isSuccessfullyMigrated = false;
					break;
				}
			}

			if (isSuccessfullyMigrated)
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
	private HashMap<String, HashMap<String, Integer>> getMapFromString(String json)
	{
		Gson gson = new Gson();
		Type migrationMap = new TypeToken<HashMap<String, HashMap<String, Integer>>>()
		{
		}.getType();
		HashMap<String, HashMap<String, Integer>> map = gson.fromJson(json, migrationMap);
		return map;
	}

}
