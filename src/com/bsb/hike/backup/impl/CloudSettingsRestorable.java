package com.bsb.hike.backup.impl;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.backup.BackupUtils;
import com.bsb.hike.backup.iface.BackupRestoreTaskLifecycle;
import com.bsb.hike.backup.model.CloudBackupPrefInfo;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by atul on 05/04/16.
 */
public class CloudSettingsRestorable implements BackupRestoreTaskLifecycle, IRequestListener
{

	private JSONObject mSettingsJSON;

	private ArrayList<CloudBackupPrefInfo> prefInfoList;

	private final String TAG = CloudSettingsRestorable.class.getSimpleName();

	public void setBackupDataJSON(String settingsJSON) throws JSONException
	{
		mSettingsJSON = new JSONObject(settingsJSON);
	}

	@Override
	public boolean doPreTask()
	{
		prefInfoList = new ArrayList<CloudBackupPrefInfo>();
		return true;
	}

	@Override
	public void doTask()
	{
		// Is restore JSON available? If not, GET it first then re-run.
		if (mSettingsJSON == null)
		{
			Logger.d(TAG, "Restore settings JSON null, fetching from server");
			// Do HTTP GET
			HttpRequests.downloadUserSettings(this, 2, 500).execute();
			return; // Very important
		}

		Logger.d(TAG, "Restore settings JSON found, begin restore");

		//Retrieve "setting"
		JSONObject settingsJSON = mSettingsJSON.optJSONObject(HikeConstants.BackupRestore.KEY_SETTING);

		// Retrieve "d"
		JSONObject settingsDataJSON = settingsJSON.optJSONObject(HikeConstants.BackupRestore.DATA);

		Iterator<String> iterPrefFile = settingsDataJSON.keys();

		// Traverse through preference types (account settings, mute settings, etc)
		while (iterPrefFile.hasNext())
		{
			String prefName = iterPrefFile.next();
			try
			{
				JSONObject prefGroupJSON = (JSONObject) settingsDataJSON.get(prefName); // Contains all settings for a preference type

				// Finally, traverse through settings (last_seen, media prefs, etc) for the preference type
				Iterator<String> iterSettings = prefGroupJSON.keys();
				while (iterSettings.hasNext())
				{
					String settingName = iterSettings.next(); // Setting name
					try
					{
						JSONObject settingInfoJSON = (JSONObject) prefGroupJSON.get(settingName); // JSON for corresponding setting name

						prefInfoList.add(new CloudBackupPrefInfo(settingName, prefName, settingInfoJSON));
					}
					catch (JSONException e)
					{
						e.printStackTrace();
						// Something went wrong! Its OK, skip and continue
					}
				}

			}
			catch (JSONException | ClassCastException e)
			{
				e.printStackTrace();
				// Something went wrong! Its OK, skip and continue
			}
		}

		// We have CloudBackupPrefInfo of items to be restored (from JSON) lets begin restore!
		for (CloudBackupPrefInfo prefInfo : prefInfoList)
		{
			String prefName = prefInfo.getPrefName();

			// Check if this is a shared-preference restore or manual (mute-settings, etc)
			boolean isSharedPref = BackupUtils.isSharedPrefFile(prefName);
			if (isSharedPref)
			{
				// Save into shared preferences
				restoreSharedPreference(prefInfo);
				Logger.d(TAG, "Restoring key - " + prefInfo.getKeyName() + " val:" + prefInfo.getRestoreValue());
			}
			else
			{
				// Handle manual cases
				restoreCustomData(prefInfo);
			}
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_RESTORE_SUCCESS, null);

	}

	public void restoreSharedPreference(CloudBackupPrefInfo prefInfo)
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(prefInfo.getPrefName());
		int dataType = prefInfo.getDataType();
		switch (dataType)
		{
		case CloudBackupPrefInfo.TYPE_BOOL:
			prefs.saveData(prefInfo.getKeyName(), Boolean.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_STRING:
			prefs.saveData(prefInfo.getKeyName(), String.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_INT:
			prefs.saveData(prefInfo.getKeyName(), Integer.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_FLOAT:
			prefs.saveData(prefInfo.getKeyName(), Float.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_LONG:
			prefs.saveData(prefInfo.getKeyName(), Long.valueOf(prefInfo.getRestoreValue()));
			break;

		}
	}

	public void restoreCustomData(CloudBackupPrefInfo prefInfo)
	{
		String prefName = prefInfo.getPrefName();
		if (prefName.equals("mutesettings"))
		{
			// TODO
		}
	}

	@Override
	public void doPostTask()
	{

	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED, null);
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		if(Utils.isResponseValid((JSONObject) result.getBody().getContent()))
		{
			try
			{
				mSettingsJSON = (JSONObject) result.getBody().getContent();
				doTask();
			} catch (Exception e)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED, null);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}
}
