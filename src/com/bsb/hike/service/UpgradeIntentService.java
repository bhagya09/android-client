package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.tasks.MigrateBlockTableToUserTable;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import java.io.File;

public class UpgradeIntentService extends IntentService
{

	private static final String TAG = "UpgradeIntentService";

	private SharedPreferences prefs;

	Context context;

	@Override
	protected void onHandleIntent(Intent dbIntent)
	{
		context = this;
		prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (prefs.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1)
		{
			initialiseSharedMediaAndFileThumbnailTable();

			// setting the preferences to 2 to indicate we're done with the
			// migration !
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, 2);
			editor.commit();

			// fire the pubsub event to let the HomeActivity class know that the
			// avatar
			// upgrade is done and it can stop the spinner
		}

		if (prefs.getInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1) == 1)
		{
			addMessageHashNMsisdnNReadByForGroup();
			// setting the preferences to 2 to indicate we're done with the
			// migration !
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, 2);
			editor.commit();
		}
		
		if (prefs.getInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1) == 1)
		{
			upgradeForDatabaseVersion28();
			// setting the preferences to 2 to indicate we're done with the
			// migration !
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, 2);
			editor.commit();
		}
		
		if (prefs.getInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1) == 1)
		{
			if(StickerManager.moveHardcodedStickersToSdcard(getApplicationContext()))
			{
				Editor editor = prefs.edit();
				editor.putInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 2);
				editor.commit();
			}
		}
		
		if (prefs.getInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 1)
		{
			upgradeForStickerShopVersion1();
			Editor editor = prefs.edit();
			editor.putInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 2);
			editor.commit();
		}
		
		if (prefs.getInt(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 1) == 1)
		{
			if(upgradeForServerIdField())
			{
				Editor editor = prefs.edit();
				editor.putInt(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 2);
				editor.commit();
			}
		}
		
		// This value is set as 1 in onUpgrade of HikeConversationsDatabase.
		if (prefs.getInt(HikeMessengerApp.UPGRADE_SORTING_ID_FIELD, 0) == 1)
		{
			if (upgradeForSortingIdField())
			{
				Logger.v(TAG, "Upgrade Sorting Id Field was successful");
				Editor editor = prefs.edit();
				editor.putInt(HikeMessengerApp.UPGRADE_SORTING_ID_FIELD, 2);
				editor.commit();
			}
		}
		if (prefs.getInt(HikeMessengerApp.UPGRADE_LANG_ORDER, 0) == 0)
		{
			{
				LocalLanguageUtils.requestLanguageOrderListFromServer();
			}
		}

        // Schedule versioning migration if its not done already
        if(prefs.getBoolean(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, false) == false)
        {
            scheduleHikeMicroAppsMigrationAlarm(getBaseContext());
        }

		if(prefs.getInt(HikeMessengerApp.UPGRADE_FOR_STICKER_TABLE, 1) == 1)
		{
			if(upgradeForStickerTable())
			{
				Logger.v(TAG, "Upgrade for sticker table was successful");
				Editor editor = prefs.edit();
				editor.putInt(HikeMessengerApp.UPGRADE_FOR_STICKER_TABLE, 2);
				editor.commit();
                StickerManager.getInstance().doInitialSetup();
			}
		}

		if (prefs.getInt(HikeMessengerApp.MIGRATE_TABLE_TO_USER, 1) == 1) {
			MigrateBlockTableToUserTable migrateBlockTableToUserTable = new MigrateBlockTableToUserTable();
			try {
				boolean result = migrateBlockTableToUserTable.call();
				if (result) {
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.MIGRATE_TABLE_TO_USER, 2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Set block notifications as false in shared preference i.e allow notifications to occur once Upgrade intent completes
        Editor editor = prefs.edit();
        editor.putBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
        editor.apply();

		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPGRADING, false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FINISHED_UPGRADE_INTENT_SERVICE, null);
	}

	public UpgradeIntentService()
	{

		super(TAG);

	}

	private void initialiseSharedMediaAndFileThumbnailTable()
	{
		HikeConversationsDatabase.getInstance().initialiseSharedMediaAndFileThumbnailTable();
	}

	private void addMessageHashNMsisdnNReadByForGroup()
	{
		HikeConversationsDatabase.getInstance().addMessageHashNMsisdnNReadByForGroup();
	}
	
	private void upgradeForDatabaseVersion28()
	{
		HikeConversationsDatabase.getInstance().upgradeForDatabaseVersion28();
	}

	private void upgradeForStickerShopVersion1()
	{
		HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1();
		StickerManager.getInstance().moveStickerPreviewAssetsToSdcard();
	}
	
	private boolean upgradeForServerIdField()
	{
		return HikeConversationsDatabase.getInstance().upgradeForServerIdField();
	}
	
	private boolean upgradeForSortingIdField()
	{
		return HikeConversationsDatabase.getInstance().upgradeForSortingIdField();
	}


    /**
     * Used to schedule the alarm for migration of old running micro apps in the content directory
     */
    private void scheduleHikeMicroAppsMigrationAlarm(Context context)
    {
        // Do the migration tasks only if migration has not been done already and old directory content code exists on device
        if(new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR).exists())
        {
            Intent migrationIntent = new Intent(context, HikeMicroAppsCodeMigrationService.class);
            context.startService(migrationIntent);
        }
        else
        {
            // As there is no older content directory there to migrate, setting the migration as successful so that it doesn't get triggered again
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, true);
        }
    }

	private boolean upgradeForStickerTable()
	{
		return HikeConversationsDatabase.getInstance().upgradeForStickerTable();
	}
}