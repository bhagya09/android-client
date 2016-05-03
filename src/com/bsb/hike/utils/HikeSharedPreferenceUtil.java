package com.bsb.hike.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.StringDef;

import com.bsb.hike.HikeMessengerApp;

public class HikeSharedPreferenceUtil
{
	@Retention(RetentionPolicy.SOURCE)
	@StringDef({ HikeMessengerApp.ACCOUNT_SETTINGS, HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF, HikeMessengerApp.DEFAULT_SETTINGS_PREF })
	public @interface SharedPrefFile
	{
	}

	private static final String DEFAULT_PREF_NAME = HikeMessengerApp.ACCOUNT_SETTINGS;

	public static final String STRING_EMPTY = "";

	public static final String STRING_SEPARATOR = ",";

	public static final String CONV_UNREAD_COUNT = "ConvUnreadCount";

	private SharedPreferences hikeSharedPreferences;

	private Editor editor;

	private static HashMap<String, HikeSharedPreferenceUtil> hikePrefsMap = new HashMap<String, HikeSharedPreferenceUtil>();

	private static HikeSharedPreferenceUtil initializeHikeSharedPref(Context context, String argSharedPrefName)
	{
		HikeSharedPreferenceUtil hikeSharedPreferenceUtil = null;
		if (context != null)
		{
			hikeSharedPreferenceUtil = new HikeSharedPreferenceUtil();
			hikeSharedPreferenceUtil.hikeSharedPreferences = context.getSharedPreferences(argSharedPrefName, Activity.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			hikeSharedPreferenceUtil.editor = hikeSharedPreferenceUtil.hikeSharedPreferences.edit();
			hikePrefsMap.put(argSharedPrefName, hikeSharedPreferenceUtil);
		}
		return hikeSharedPreferenceUtil;
	}

	public static HikeSharedPreferenceUtil getInstance()
	{
		if (hikePrefsMap.containsKey(DEFAULT_PREF_NAME))
		{
			return hikePrefsMap.get(DEFAULT_PREF_NAME);
		}
		else
		{
			if (HikeMessengerApp.getInstance() != null)
			{
				return initializeHikeSharedPref(HikeMessengerApp.getInstance().getApplicationContext(), DEFAULT_PREF_NAME);
			}
			else
			{
				return null;
			}
		}
	}

	public static HikeSharedPreferenceUtil getInstance(String argSharedPrefName)
	{
		if (hikePrefsMap.containsKey(argSharedPrefName))
		{
			return hikePrefsMap.get(argSharedPrefName);
		}
		else
		{
			return initializeHikeSharedPref(HikeMessengerApp.getInstance().getApplicationContext(), argSharedPrefName);
		}
	}

	private HikeSharedPreferenceUtil()
	{
		// TODO Auto-generated constructor stub
	}

	public synchronized boolean saveData(String key, String value)
	{
		editor.putString(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, boolean value)
	{
		editor.putBoolean(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, long value)
	{
		editor.putLong(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, float value)
	{
		editor.putFloat(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, int value)
	{
		editor.putInt(key, value);
		return editor.commit();
	}

	public synchronized boolean saveDataMap(Map<String, Integer> keyValueMap)
	{
		if ((keyValueMap != null) && (keyValueMap.size() > 0))
		{
			Set<String> keys = keyValueMap.keySet();
			for (String key : keys)
			{
				editor.putInt(key, keyValueMap.get(key));
			}
			return editor.commit();
		}

		return false;
	}

	/**
	 * It was added on API 11 onwards. Instead of this, use of getDataSet(String, Set<String>) in all cases is recommended.
	 */
	public synchronized boolean saveStringSet(String key, Set<String> stringSet)
	{
		editor.putStringSet(key, stringSet);
		return editor.commit();
	}

	public synchronized boolean saveDataSet(String key, Set<String> value)
	{
		if (Utils.isHoneycombOrHigher())
		{
			editor.putStringSet(key, value);
		}
		else
		{
			if (value != null)
			{
				StringBuilder sb = new StringBuilder(STRING_EMPTY);
				for (String s : value)
				{
					sb.append(s);
					sb.append(STRING_SEPARATOR);
				}
				// do not remove last separator, as it would not be able to determine, if last element of set was empty string
				// at same time, if set argument has no element, then equivalent string will empty without having any separator
				editor.putString(key, sb.toString());
			}
			// value is null, i.e. remove the key as same as Android standard behavior
			else
			{
				editor.remove(key);
			}
		}

		return editor.commit();
	}

	public synchronized boolean removeData(String key)
	{
		editor.remove(key);
		return editor.commit();
	}

	public synchronized boolean removeData(Set<String> keys)
	{
		if (!Utils.isEmpty(keys))
		{
			for (String key : keys)
			{
				editor.remove(key);
			}

			return editor.commit();
		}

		return false;
	}

	public synchronized Boolean getData(String key, boolean defaultValue)
	{
		return hikeSharedPreferences.getBoolean(key, defaultValue);
	}

	public synchronized String getData(String key, String defaultValue)
	{
		return hikeSharedPreferences.getString(key, defaultValue);
	}

	/**
	 * It was added on API 11 onwards. Instead of this, use of getDataSet(String, Set<String>) in all cases is recommended.
	 */
	public synchronized Set<String> getStringSet(String key, Set<String> defaultValues)
	{
		return hikeSharedPreferences.getStringSet(key, defaultValues);
	}

	public synchronized Set<String> getDataSet(String key, Set<String> defaultValues)
	{
		if (Utils.isHoneycombOrHigher())
		{
			return hikeSharedPreferences.getStringSet(key, defaultValues);
		}
		else
		{
			String transformedValue = hikeSharedPreferences.getString(key, null);
			// return default value, if no such key is present
			if (transformedValue == null)
			{
				return defaultValues;
			}
			// return empty set, if empty set (equivalent to empty string) was saved
			else if (transformedValue.length() == 0)
			{
				return new HashSet<String>(0);
			}
			// return set built by values (determined by splitting equivalent string)
			else
			{
				String[] values = transformedValue.split(STRING_SEPARATOR);
				HashSet<String> result = new HashSet<String>(values.length);

				for (String value : values)
				{
					result.add(value);
				}

				// if saved set contained empty string as its last element, then equivalent string would have been ended with 2 separators.
				if (transformedValue.endsWith(STRING_SEPARATOR + STRING_SEPARATOR))
				{
					result.add(STRING_EMPTY);
				}

				return result;
			}
		}
	}

	public synchronized float getData(String key, float defaultValue)
	{
		return hikeSharedPreferences.getFloat(key, defaultValue);
	}

	public synchronized int getData(String key, int defaultValue)
	{
		return hikeSharedPreferences.getInt(key, defaultValue);
	}

	public synchronized long getData(String key, long defaultValue)
	{
		return hikeSharedPreferences.getLong(key, defaultValue);
	}

	public synchronized Map<String, ?> getAllData()
	{
		return hikeSharedPreferences.getAll();
	}

	public synchronized void deleteAllData()
	{
		editor.clear();
		editor.commit();
	}

	public SharedPreferences getPref()
	{
		return hikeSharedPreferences;
	}

	public synchronized boolean contains(String key)
	{
		return hikeSharedPreferences.contains(key);
	}
}
