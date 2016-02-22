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
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.modules.kpt.KptUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class UpgradeIntentService extends IntentService
{

	private static final String TAG = "UpgradeIntentService";

	private HikeSharedPreferenceUtil prefs;

	Context context;

	@Override
	protected void onHandleIntent(Intent dbIntent)
	{
		context = this;
		prefs = HikeSharedPreferenceUtil.getInstance();
		if (prefs.getData(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1)
		{
			initialiseSharedMediaAndFileThumbnailTable();

			// setting the preferences to 2 to indicate we're done with the
			// migration !
			prefs.saveData(HikeConstants.UPGRADE_AVATAR_CONV_DB, 2);
			prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);

			// fire the pubsub event to let the HomeActivity class know that the
			// avatar
			// upgrade is done and it can stop the spinner
		}

		if (prefs.getData(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1) == 1)
		{
			addMessageHashNMsisdnNReadByForGroup();
			// setting the preferences to 2 to indicate we're done with the
			// migration !
			prefs.saveData(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, 2);
			prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
		}
		
		if (prefs.getData(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1) == 1)
		{
			upgradeForDatabaseVersion28();
			// setting the preferences to 2 to indicate we're done with the
			// migration !
			prefs.saveData(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, 2);
			prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
		}
		
		if (prefs.getData(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1) == 1)
		{
			if(StickerManager.moveHardcodedStickersToSdcard(getApplicationContext()))
			{
				prefs.saveData(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 2);
				prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
			}
		}
		
		if (prefs.getData(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 1)
		{
			upgradeForStickerShopVersion1();
			prefs.saveData(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 2);
			prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
			StickerManager.getInstance().doInitialSetup();
		}
		
		if (prefs.getData(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 1) == 1)
		{
			if(upgradeForServerIdField())
			{
				prefs.saveData(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 2);
				prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
			}
		}
		
		// This value is set as 1 in onUpgrade of HikeConversationsDatabase.
		if (prefs.getData(HikeMessengerApp.UPGRADE_SORTING_ID_FIELD, 0) == 1)
		{
			if (upgradeForSortingIdField())
			{
				Logger.v(TAG, "Upgrade Sorting Id Field was successful");
				prefs.saveData(HikeMessengerApp.UPGRADE_SORTING_ID_FIELD, 2);
				prefs.saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false);
			}
		}
		if (prefs.getData(HikeMessengerApp.UPGRADE_LANG_ORDER, 0) == 0)
		{
			{
				LocalLanguageUtils.requestLanguageOrderListFromServer();
			}
		}

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
}