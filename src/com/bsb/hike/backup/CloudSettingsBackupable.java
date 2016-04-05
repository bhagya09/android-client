package com.bsb.hike.backup;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.iface.Backupable;
import com.bsb.hike.backup.model.CloudBackupPrefInfo;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

/**
 * Responsible for creating and uploading settings backup JSON. Created by atul on 31/03/16.
 */
public class CloudSettingsBackupable implements Backupable, IRequestListener
{
	private ArrayList<CloudBackupPrefInfo> prefInfoList;

	@Override
	public boolean preBackupSetup() throws Exception
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

		// HikeMessengerApp.DEFAULT_SETTINGS_PREF
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STEALTH_NOTIFICATION_ENABLED, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, true));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.STEALTH_INDICATOR_ENABLED, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_BOOL, false));
		prefInfoList.add(new CloudBackupPrefInfo(HikeConstants.CHANGE_STEALTH_TIMEOUT, HikeMessengerApp.DEFAULT_SETTINGS_PREF, CloudBackupPrefInfo.TYPE_STRING,
				StealthModeManager.DEFAULT_RESET_TOGGLE_TIME));

		return true;
	}

	@Override
	public void backup() throws Exception
	{
		// Get backup data in JSON
		JSONObject backupJson = serialize();

		// Upload to server
		HttpRequests.uploadUserSettings(this, 2, 1000, backupJson).execute();
	}

	public JSONObject serialize() throws JSONException
	{
		JSONObject prefsBackupJson = new JSONObject();

		// Putting in backup origin/reference details
		prefsBackupJson.put(HikeConstants.BackupRestore.OS, HikeConstants.ANDROID);
		prefsBackupJson.put(HikeConstants.BackupRestore.FROM, ContactManager.getInstance().getSelfMsisdn());
		prefsBackupJson.put(HikeConstants.BackupRestore.TIMESTAMP, System.currentTimeMillis());
		prefsBackupJson.put(HikeConstants.BackupRestore.VERSION, Utils.getHikePackageInfo().versionName);

		for (CloudBackupPrefInfo prefInfo : prefInfoList)
		{
			String prefName = prefInfo.getPrefName();
			String settingName = prefInfo.getKeyName();

			// Check if JSON corresponding to preference file is available
			JSONObject prefObject = prefsBackupJson.optJSONObject(prefName);

			// If not present, add one
			if (prefObject == null)
			{
				prefObject = new JSONObject();
				prefsBackupJson.put(prefName, prefObject);
			}

			// Lets add setting key/values
			prefObject.put(settingName, prefInfo.serialize());
		}

		return prefsBackupJson;
	}

	@Override
	public void postBackupSetup() throws Exception
	{
		// Since this Backupable instance makes a HTTP POST. We do destructor activities in listener registered with the call.
	}

	@Override
	public void finish()
	{
		// Since this Backupable instance makes a HTTP POST. We do destructor activities in listener registered with the call.
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		// Do analytics logging

		// Abort
		prefInfoList.clear();
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		if (Utils.isResponseValid((JSONObject) result.getBody().getContent()))
		{
			// Save backup timestamp
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BackupRestore.RUX_BACKUP_TS_PREF, System.currentTimeMillis());
			prefInfoList.clear();
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
