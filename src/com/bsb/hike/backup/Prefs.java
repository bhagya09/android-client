package com.bsb.hike.backup;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StealthModeManager;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class Prefs
{
	public static final String PREFS = "pref";

	private JSONObject prefJson;

	public Prefs()
	{
	}

	public Prefs takeBackup() throws JSONException
	{
		prefJson = new JSONObject();
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

		prefJson.put(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, prefUtil.getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, ""))
				.put(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, prefUtil.getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
				.put(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, prefUtil.getData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, false))
				.put(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, prefUtil.getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
				.put(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, prefUtil.getData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, false))
				.put(HikeMessengerApp.CONV_DB_VERSION_PREF, prefUtil.getData(HikeMessengerApp.CONV_DB_VERSION_PREF, DBConstants.CONVERSATIONS_DATABASE_VERSION))
				.put(HikePlatformConstants.CUSTOM_TABS, prefUtil.getData(HikePlatformConstants.CUSTOM_TABS, true));

		SharedPreferences settingUtils = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance());

		prefJson.put(HikeConstants.STEALTH_NOTIFICATION_ENABLED, settingUtils.getBoolean(HikeConstants.STEALTH_NOTIFICATION_ENABLED, true))
				.put(HikeConstants.STEALTH_INDICATOR_ENABLED, settingUtils.getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED, false))
				.put(HikeConstants.CHANGE_STEALTH_TIMEOUT, settingUtils.getString(HikeConstants.CHANGE_STEALTH_TIMEOUT, StealthModeManager.DEFAULT_RESET_TOGGLE_TIME));
		return this;
	}

	public String serialize()
	{
		return prefJson.toString();
	}

    public void deserialize(String prefJsonString) throws JSONException
    {
        prefJson = new JSONObject(prefJsonString);
    }

	public void restore() throws JSONException
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

		if (prefJson.has(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN))
		{
			String key = HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN;
			prefUtil.saveData(key, prefJson.getString(key));
		}

		if (prefJson.has(HikeMessengerApp.STEALTH_MODE_SETUP_DONE))
		{
			String key = HikeMessengerApp.STEALTH_MODE_SETUP_DONE;
			prefUtil.saveData(key, prefJson.getBoolean(key));
		}
		if (prefJson.has(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST))
		{
			String key = HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST;
			prefUtil.saveData(key, prefJson.getBoolean(key));
		}
		if (prefJson.has(HikeMessengerApp.SHOW_STEALTH_INFO_TIP))
		{
			String key = HikeMessengerApp.SHOW_STEALTH_INFO_TIP;
			prefUtil.saveData(key, prefJson.getBoolean(key));
		}
		if (prefJson.has(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD))
		{
			String key = HikeMessengerApp.STEALTH_PIN_AS_PASSWORD;
			prefUtil.saveData(key, prefJson.getBoolean(key));
		}
		if (prefJson.has(HikeMessengerApp.CONV_DB_VERSION_PREF))
		{
			String key = HikeMessengerApp.CONV_DB_VERSION_PREF;
			prefUtil.saveData(key, prefJson.getInt(key));
		}
		if (prefJson.has(HikePlatformConstants.CUSTOM_TABS))
		{
			String key = HikePlatformConstants.CUSTOM_TABS;
			prefUtil.saveData(key, prefJson.getBoolean(key));
		}

		SharedPreferences settingUtils = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance());

		if (prefJson.has(HikeConstants.STEALTH_INDICATOR_ENABLED))
		{
			String key = HikeConstants.STEALTH_INDICATOR_ENABLED;
			settingUtils.edit().putBoolean(key, prefJson.getBoolean(key)).commit();
		}
		if (prefJson.has(HikeConstants.CHANGE_STEALTH_TIMEOUT))
		{
			String key = HikeConstants.CHANGE_STEALTH_TIMEOUT;
			settingUtils.edit().putString(key, prefJson.getString(key)).commit();
		}
		if (prefJson.has(HikeConstants.STEALTH_NOTIFICATION_ENABLED))
		{
			String key = HikeConstants.STEALTH_NOTIFICATION_ENABLED;
			settingUtils.edit().putBoolean(key, prefJson.getBoolean(key)).commit();
		}
	}

	public static File getPrefFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, PREFS);
	}
}
