package com.bsb.hike.backup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.backup.iface.Restorable;
import com.bsb.hike.backup.model.CloudBackupPrefInfo;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * Created by atul on 05/04/16.
 */
public class CloudSettingsRestorable implements Restorable, IRequestListener
{

	private JSONObject mSettingsJSON;

	private ArrayList<CloudBackupPrefInfo> prefInfoList;

	public CloudSettingsRestorable(String settingsJSON) throws JSONException
	{
		mSettingsJSON = new JSONObject(settingsJSON);
	}

	@Override
	public boolean preRestoreSetup() throws Exception
	{
		prefInfoList = new ArrayList<CloudBackupPrefInfo>();
		return true;
	}

	@Override
	public void restore() throws Exception
	{
		// Retrieve "d"
		JSONObject settingsDataJSON = mSettingsJSON.optJSONObject(HikeConstants.BackupRestore.DATA);
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
			}
			else
			{
				// Handle manual cases
                restoreCustomData(prefInfo);
			}
		}

	}

	public void restoreSharedPreference(CloudBackupPrefInfo prefInfo)
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(prefInfo.getPrefName());
		int dataType = prefInfo.getDataType();
		switch (dataType)
		{
		case CloudBackupPrefInfo.TYPE_BOOL:
            prefs.saveData(prefInfo.getKeyName(),Boolean.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_STRING:
            prefs.saveData(prefInfo.getKeyName(),String.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_INT:
            prefs.saveData(prefInfo.getKeyName(),Integer.valueOf(prefInfo.getRestoreValue()));
			break;

		case CloudBackupPrefInfo.TYPE_STR_ARR:
            String[] arr = prefInfo.getRestoreValue().split(",");
            prefs.saveStringSet(prefInfo.getKeyName(),  new HashSet<String>(Arrays.asList(arr)));
			break;
		}
	}

	public void restoreCustomData(CloudBackupPrefInfo prefInfo)
	{
        String prefName = prefInfo.getPrefName();
        if(prefName.equals("mutesettings"))
        {
            //TODO
        }
	}

	@Override
	public void postRestoreSetup() throws Exception
	{

	}

	@Override
	public void finish()
	{
		mSettingsJSON = null;
		prefInfoList = null;
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{

	}

	@Override
	public void onRequestSuccess(Response result)
	{

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}
}
