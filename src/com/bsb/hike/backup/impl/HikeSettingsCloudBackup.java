package com.bsb.hike.backup.impl;

import com.bsb.hike.BuildConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.backup.iface.BackupRestoreTaskLifecycle;
import com.bsb.hike.backup.model.CloudBackupPrefInfo;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for creating and uploading settings backup JSON. Created by atul on 31/03/16.
 */
public class HikeSettingsCloudBackup implements BackupRestoreTaskLifecycle, IRequestListener
{
	private List<CloudBackupPrefInfo> prefInfoList;

	private final String TAG = HikeSettingsCloudBackup.class.getSimpleName();

	@Override
	public boolean doPreTask()
	{
		prefInfoList = new ArrayList<CloudBackupPrefInfo>();

		// HikeMessengerApp.ACCOUNT_SETTINGS
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_STRING, ""));
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.CONV_DB_VERSION_PREF, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_INT,
				DBConstants.CONVERSATIONS_DATABASE_VERSION));
		prefInfoList.add(new CloudBackupPrefInfo(HikePlatformConstants.CUSTOM_TABS, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList
				.add(new CloudBackupPrefInfo(HikeConstants.INTERCEPTS.ENABLE_SCREENSHOT_INTERCEPT, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.INTERCEPTS.ENABLE_VIDEO_INTERCEPT, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.INTERCEPTS.ENABLE_IMAGE_INTERCEPT, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));

		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.Extras.ENABLE_CLOUD_SETTING_BACKUP, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));

		// HikeMessengerApp.DEFAULT_SETTINGS_PREF
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STEALTH_NOTIFICATION_ENABLED, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STEALTH_INDICATOR_ENABLED, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.CHANGE_STEALTH_TIMEOUT, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_STRING,
				StealthModeManager.DEFAULT_RESET_TOGGLE_TIME));
		/*
		 * Notification Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.VIBRATE_PREF_LIST, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_STRING, HikeMessengerApp
				.getInstance().getString(R.string.vib_default)));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.TICK_SOUND_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));

		//Cannot take a backup since restore might happen on some device which does not have this ringtone
//		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.NOTIF_SOUND_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_STRING, "HikeJingle"));

		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STATUS_BOOLEAN_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STATUS_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_INT, 0));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.H2O_NOTIF_BOOLEAN_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));

//		Cannot take a backup of led color since restore might happen on some device which does not support current led color
//		prefInfoList.add(new CloudBackupPrefInfo(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_INT,
//				HikeConstants.LED_DEFAULT_WHITE_COLOR));

		/*
		 * Media Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.COMPRESS_VIDEO, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.MD_AUTO_DOWNLOAD_VIDEO_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.MD_AUTO_DOWNLOAD_AUDIO_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.WF_AUTO_DOWNLOAD_VIDEO_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.WF_AUTO_DOWNLOAD_AUDIO_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));

		/*
		 * Chat Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.DOUBLE_TAP_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.SEND_ENTER_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));

		/*
		 * Privacy Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.PROFILE_PIC_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.LAST_SEEN_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL,true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.LAST_SEEN_PREF_LIST, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_STRING, HikeMessengerApp
				.getInstance().getString(R.string.privacy_my_contacts)));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.SSL_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));

		/*
		 *SMS Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.FREE_SMS_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));

		/*
		 * Language Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.LOCAL_LANGUAGE_PREF, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_STRING, LocalLanguage.PhoneLangauge.getLocale()));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.LOCALIZATION_FTUE_COMPLETE, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, false));

		/*
		 * Stickers Settings
		 */
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STICKER_RECOMMEND_PREF, HikeMessengerApp.ACCOUNT_SETTINGS, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STICKER_RECOMMEND_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));

		return true;
	}

	@Override
	public void doTask()
	{
		JSONObject backupJSON = null;
		try
		{
			Logger.d(TAG, "Begin backup data serialization");
			// Get backup data in JSON
			backupJSON = serialize();
		}
		catch (JSONException jsonex)
		{
			jsonex.printStackTrace();
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, jsonex);
			return;
		}

		if (backupJSON == null)
		{
			Logger.wtf(TAG, "Backup data null?");
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, null);
			return;
		}

		Logger.d(TAG, "Begin server upload");
		// Upload to server
		HttpRequests.uploadUserSettings(this, 2, 1000, backupJSON).execute();
	}

	public JSONObject serialize() throws JSONException
	{
		JSONObject prefsBackupJson = new JSONObject();

		// Putting in backup origin/reference details
		prefsBackupJson.put(HikeConstants.BackupRestore.OS, HikeConstants.ANDROID);
		prefsBackupJson.put(HikeConstants.BackupRestore.FROM, ContactManager.getInstance().getSelfMsisdn());
		prefsBackupJson.put(HikeConstants.BackupRestore.TIMESTAMP, System.currentTimeMillis());
		prefsBackupJson.put(HikeConstants.BackupRestore.VERSION, AccountUtils.appVersion);

		JSONObject dataObject = new JSONObject();

		for (CloudBackupPrefInfo prefInfo : prefInfoList)
		{
			String prefName = prefInfo.getPrefName();
			String settingName = prefInfo.getKeyName();

			// Check if JSON corresponding to preference file is available
			JSONObject prefObject = dataObject.optJSONObject(prefName);

			// If not present, add one
			if (prefObject == null)
			{
				prefObject = new JSONObject();
				dataObject.put(prefName, prefObject);
			}

			// Lets add setting key/values
			prefObject.put(settingName, prefInfo.serialize());

			if(BuildConfig.DEBUG)
			Logger.d(TAG, "Backup key - " + settingName + " val- " + prefInfo.serialize().toString());
		}

		prefsBackupJson.put(HikeConstants.BackupRestore.DATA, dataObject);

		return prefsBackupJson;
	}

	@Override
	public void doPostTask()
	{
		// Since this Backupable instance makes a HTTP POST.  We notify app using Pubsub.
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		Logger.d(TAG, "Upload failed");

		// PubSub
		HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, httpException);

		// Abort
		prefInfoList.clear();
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		if (Utils.isResponseValid((JSONObject) result.getBody().getContent()))
		{
			Logger.d(TAG, "Upload successful");
			// Save backup timestamp
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BackupRestore.RUX_BACKUP_TS_PREF, System.currentTimeMillis());
			prefInfoList.clear();

			// PubSub
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_BACKUP_SUCESS, result);
		}
		else
		{
			onRequestFailure(new HttpException(HttpException.REASON_CODE_SERVER_STATUS_FAILED));
		}
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		// Do nothing
	}
}
