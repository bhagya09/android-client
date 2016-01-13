package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
		boolean isSuccessfullyMigrated = true;
		HashMap<String, Boolean> mapForMigratedApps = new HashMap<String, Boolean>();

		/*
		 * Iterating and doing the migration over the key set of hikeBotInfoMap currently present in BotTable
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
					String unzipPath = PlatformUtils.getMicroAppContentRootFolder();

					String botName = entry.getValue().getMsisdn();
                    botName =  botName.substring(1, botName.length()-1);
					unzipPath += botName;

					File newFilePath = new File(unzipPath);

					// File instance for source file directory path that was in use before versioning release
					String microAppName = botMetadata.getAppName();
					File originalFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + microAppName);

					// Copy from source to destination
					boolean isCopied = PlatformUtils.copyDirectoryTo(originalFile, newFilePath);
					if (isCopied)
					{
						botMetadata.setAppName(botName);
						JSONObject json = botMetadata.getJson();

						if (json.has(HikePlatformConstants.CARD_OBJECT))
						{
							JSONObject cardObj = json.optJSONObject(HikePlatformConstants.CARD_OBJECT);

							if (cardObj.has(HikePlatformConstants.APP_NAME))
							{
								cardObj.put(HikePlatformConstants.APP_NAME, botName);
							}
						}

						// Update metadata for this bot in the database and bots Hash Map
						String botMetadataJson = json.toString();
						HikeConversationsDatabase.getInstance().updateBotMetaData(entry.getKey(), botMetadataJson);
						entry.getValue().setMetadata(botMetadataJson);
						mapForMigratedApps.put(entry.getKey(), true);
					}
				}
				catch (FileNotFoundException fnfe)
				{
                    mapForMigratedApps.put(entry.getKey(), true);
                    Logger.e("Hike Micro apps code Migration Service File Not Found Exception" ,fnfe.toString());
                    fnfe.printStackTrace();
				}
                catch (JSONException e)
                {
                    Logger.e("Hike Micro apps code Migration Service JSONException",e.toString());
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    Logger.e("Hike Micro apps code Migration Service IOException",e.toString());
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

		/*
		 * Check if migration is successful , save the flag is true in Shared Preferences
		 */
		if (isSuccessfullyMigrated)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, true);
		}
	}

}
