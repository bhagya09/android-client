/**
 * File   : HikeSystemSettingsDBUtil.java
 * Content: It is a utility class, especially to provide centralized handling for read/ write/ delete data in Android_System_Settings_DataBase.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.System;

import com.bsb.hike.HikeMessengerApp;

/**
 * Warning: One should use this utility to read/ modify but not to create/ delete system settings data (key-value pairs) and read/ create/ modify/ delete application specific data
 * (key-value pairs). Deletion of all application specific data is facilitated in such a way that one can exclude particular module data from deletion while clearing all data.
 * Hence, one can choose the key-names along with '<module-name>' by passing additional parameter in respective method; if respective data is eligible to a specific module. These
 * data operations are internally synchronized by Android_Settings_DataBase, hence application doesn't have any control over order of occurrence of these operations.
 * 
 * Use of these data operations is recommended only for 3 purposes:
 * 1. To read system settings/ control values commonly used by all applications. Application can modify these values, only if such application is meant for providing system settings control.
 * 2. To share/ notify data with other application through ContentObserver implemented by friend application.
 * 3. To save/ delete/ restore data, which needs to be recovered after reinstalling application or, after clearing application data or, in case of dependency on user history or, while handling multiuser scenario.
 */

public class HikeSystemSettingsDBUtil
{
	private static final String TAG = HikeSystemSettingsDBUtil.class.getSimpleName();

	public static final String STRING_EMPTY = "";

	public static final String STRING_SEPARATOR = ",";

	public static final String STRING_RELATER = ":";

	public static final String STRING_CLOSURE = "$";

	private static final String HIKE_PACKAGE_NAME_PREF = HikeMessengerApp.getInstance().getPackageName() + STRING_RELATER;

	private static final String HIKE_SETTINGS_KEY_LIST = HIKE_PACKAGE_NAME_PREF + "keys";

	private static final String SYSTEM_DATABASE_WHERE = Settings.NameValueTable.NAME + "=?";

	private boolean mIsHikeSpecificData;

	private static ContentResolver sContentResolver;

	private volatile String mHikeKeysListString;

	private static final Object sDatabaseOperateLock = new Object();

	private static final Object sHikeKeyInfoEditLock = new Object();

	private static final SystemSettingsUpdateException sSystemSettingsUpdateException = new SystemSettingsUpdateException();

	private static final NullPointerException sNullPointerException = new NullPointerException(SystemSettingsUpdateException.ERROR_NPE);

	private static final HashMap<Boolean, HikeSystemSettingsDBUtil> sInstanceContainer = new HashMap<Boolean, HikeSystemSettingsDBUtil>();

	/* Constructor to this class (singleton) */
	private HikeSystemSettingsDBUtil(boolean isHikeSpecificData)
	{
		Logger.i(TAG, "HikeSystemSettingsDBUtil(" + isHikeSpecificData + ")");

		mIsHikeSpecificData = isHikeSpecificData;
		if (sContentResolver == null)
		{
			sContentResolver = HikeMessengerApp.getInstance().getContentResolver();
		}
		if (mIsHikeSpecificData)
		{
			synchronized (sDatabaseOperateLock)
			{
				mHikeKeysListString = System.getString(sContentResolver, HIKE_SETTINGS_KEY_LIST);
			}
		}
	}

	/* Generate and get instance of utility class from outside according to type i.e. whether using for Hike specific data or, common system-wide control data */
	public static HikeSystemSettingsDBUtil getInstance(boolean isHikeSpecificData)
	{
		HikeSystemSettingsDBUtil hikeSystemSettingsDbUtil = sInstanceContainer.get(isHikeSpecificData);

		if (hikeSystemSettingsDbUtil == null)
		{
			synchronized (HikeSharedPreferenceUtil.class)
			{
				hikeSystemSettingsDbUtil = sInstanceContainer.get(isHikeSpecificData);

				if (hikeSystemSettingsDbUtil == null)
				{
					hikeSystemSettingsDbUtil = new HikeSystemSettingsDBUtil(isHikeSpecificData);
					sInstanceContainer.put(isHikeSpecificData, hikeSystemSettingsDbUtil);
				}
			}
		}

		return hikeSystemSettingsDbUtil;
	}

	/* Save key-value pair first time or, modify integer value for an existing key */
	/* Return true; if operation was successful */
	public boolean saveData(String key, int value)
	{
		Logger.i(TAG, "saveData(" + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result = false;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.putInt(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, value);

				if (result)
				{
					result = updateHikeKeysInfo(key, false);
				}
			}
			else
			{
				result = System.putInt(sContentResolver, key, value);
			}
		}

		return result;
	}

	/* Save key-value pair first time or, modify integer value for an existing key related to any specific module */
	/* One must retrieve value for such key specific to any module by calling 'getData()' method with 'moduleName' parameter */
	/* Return true; if operation was successful */
	public boolean saveData(String moduleName, String key, int value)
	{
		Logger.i(TAG, "saveData(" + moduleName + ", " + key + ", " + value + "f, " + mIsHikeSpecificData + ")");

		boolean result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = saveData((moduleName + STRING_CLOSURE + key), value);
		}
		else
		{
			result = false;
		}

		return result;
	}

	/* Save key-value pair first time or, modify float value for an existing key */
	/* Always, specify 'f' as suffix in 'value' argument to avoid auto-boxing to integer */
	/* Return true; if operation was successful */
	public boolean saveData(String key, float value)
	{
		Logger.i(TAG, "saveData(" + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result = false;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.putFloat(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, value);

				if (result)
				{
					result = updateHikeKeysInfo(key, false);
				}
			}
			else
			{
				result = System.putFloat(sContentResolver, key, value);
			}
		}

		return result;
	}

	/* Save key-value pair first time or, modify float value for an existing key related to any specific module */
	/* Always, specify 'f' as suffix in 'value' argument to avoid auto-boxing to integer */
	/* One must retrieve value for such key specific to any module by calling 'getData()' method with 'moduleName' parameter */
	/* Return true; if operation was successful */
	public boolean saveData(String moduleName, String key, float value)
	{
		Logger.i(TAG, "saveData(" + moduleName + ", " + key + ", " + value + "f, " + mIsHikeSpecificData + ")");

		boolean result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = saveData((moduleName + STRING_CLOSURE + key), value);
		}
		else
		{
			result = false;
		}

		return result;
	}

	/* Save key-value pair first time or, modify long value for an existing key */
	/* Always, specify 'L' as suffix in 'value' argument to avoid auto-boxing to integer */
	/* Return true; if operation was successful */
	public boolean saveData(String key, long value)
	{
		Logger.i(TAG, "saveData(" + key + ", " + value + "L, " + mIsHikeSpecificData + ")");

		boolean result = false;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.putLong(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, value);

				if (result)
				{
					result = updateHikeKeysInfo(key, false);
				}
			}
			else
			{
				result = System.putLong(sContentResolver, key, value);
			}
		}

		return result;
	}

	/* Save key-value pair first time or, modify long value for an existing key related to any specific module */
	/* Always, specify 'L' as suffix in 'value' argument to avoid auto-boxing to integer */
	/* One must retrieve value for such key specific to any module by calling 'getData()' method with 'moduleName' parameter */
	/* Return true; if operation was successful */
	public boolean saveData(String moduleName, String key, long value)
	{
		Logger.i(TAG, "saveData(" + moduleName + ", " + key + ", " + value + "L, " + mIsHikeSpecificData + ")");

		boolean result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = saveData((moduleName + STRING_CLOSURE + key), value);
		}
		else
		{
			result = false;
		}

		return result;
	}

	/* Save key-value pair first time or, modify String value for an existing key */
	/* Return true; if operation was successful */
	/* Throw NullPointerException (Caught internally); if trying to save a null value for any key */
	public boolean saveData(String key, String value)
	{
		Logger.i(TAG, "saveData(" + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result = false;

		if (!Utils.isBlank(key))
		{
			if (value != null)
			{
				if (mIsHikeSpecificData)
				{
					result = System.putString(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, value);

					if (result)
					{
						result = updateHikeKeysInfo(key, false);
					}
				}
				else
				{
					result = System.putString(sContentResolver, key, value);
				}
			}
			else
			{
				Logger.wtf(TAG, "It failed to save value for key: " + key, sNullPointerException);
			}
		}

		return result;
	}

	/* Save key-value pair first time or, modify String value for an existing key related to any specific module */
	/* One must retrieve value for such key specific to any module by calling 'getData()' method with 'moduleName' parameter */
	/* Return true; if operation was successful */
	/* Throw NullPointerException (Caught internally); if trying to save a null value for any key */
	public boolean saveData(String moduleName, String key, String value)
	{
		Logger.i(TAG, "saveData(" + moduleName + ", " + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = saveData((moduleName + STRING_CLOSURE + key), value);
		}
		else
		{
			result = false;
		}

		return result;
	}

	/* Save key-value pair first time or, modify String-set value for an existing key */
	/* It will override existing set instead of appending to existing set */
	/* Return true; if operation was successful */
	/* Throw NullPointerException (Caught internally); if trying to save a null set for any key */
	public boolean saveDataSet(String key, Set<String> value)
	{
		Logger.i(TAG, "saveDataSet(" + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result = false;

		if (!Utils.isBlank(key))
		{
			if (value != null)
			{
				StringBuilder sb = new StringBuilder(STRING_EMPTY);
				for (String s : value)
				{
					sb.append(s);
					sb.append(STRING_SEPARATOR);
				}

				String transformedValue = sb.toString();

				if (mIsHikeSpecificData)
				{
					result = System.putString(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, transformedValue);

					if (result)
					{
						result = updateHikeKeysInfo(key, false);
					}
				}
				else
				{
					result = System.putString(sContentResolver, key, transformedValue);
				}
			}
			else
			{
				Logger.wtf(TAG, "It failed to save value for key: " + key, sNullPointerException);
			}
		}

		return result;
	}

	/* Save key-value pair first time or, modify String-set value for an existing key related to any specific module */
	/* It will override existing set instead of appending to existing set */
	/* One must retrieve value for such key specific to any module by calling 'getData()' method with 'moduleName' parameter */
	/* Return true; if operation was successful */
	/* Throw NullPointerException (Caught internally); if trying to save a null set for any key */
	public boolean saveDataSet(String moduleName, String key, Set<String> value)
	{
		Logger.i(TAG, "saveDataSet(" + moduleName + ", " + key + ", " + value + ", " + mIsHikeSpecificData + ")");

		boolean result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = saveDataSet((moduleName + STRING_CLOSURE + key), value);
		}
		else
		{
			result = false;
		}

		return result;
	}

	/* Get integer value for a key */
	/* Return default value; if key is not stored or, data type of existing value is undefined by integer */
	public int getData(String key, int defaultValue)
	{
		Logger.i(TAG, "getData(" + key + ", " + defaultValue + ", " + mIsHikeSpecificData + ")");

		int result;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.getInt(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, defaultValue);
			}
			else
			{
				result = System.getInt(sContentResolver, key, defaultValue);
			}
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get integer value for a key related to any specific module */
	/* Return default value; if key is not stored or, data type of existing value is undefined by integer */
	public int getData(String moduleName, String key, int defaultValue)
	{
		Logger.i(TAG, "getData(" + moduleName + ", " + key + ", " + defaultValue + ", " + mIsHikeSpecificData + ")");

		int result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = getData((moduleName + STRING_CLOSURE + key), defaultValue);
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get float value for a key */
	/* Return default value; if key is not stored or, data type of existing value is undefined by float */
	public float getData(String key, float defaultValue)
	{
		Logger.i(TAG, "getData(" + key + ", " + defaultValue + "f, " + mIsHikeSpecificData + ")");

		float result;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.getFloat(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, defaultValue);
			}
			else
			{
				result = System.getFloat(sContentResolver, key, defaultValue);
			}
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get float value for a key related to any specific module */
	/* Return default value; if key is not stored or, data type of existing value is undefined by integer */
	public float getData(String moduleName, String key, float defaultValue)
	{
		Logger.i(TAG, "getData(" + moduleName + ", " + key + ", " + defaultValue + "f, " + mIsHikeSpecificData + ")");

		float result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = getData((moduleName + STRING_CLOSURE + key), defaultValue);
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get long value for a key */
	/* Return default value; if key is not stored or, data type of existing value is undefined by long */
	public long getData(String key, long defaultValue)
	{
		Logger.i(TAG, "getData(" + key + ", " + defaultValue + "L, " + mIsHikeSpecificData + ")");

		long result;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.getLong(sContentResolver, HIKE_PACKAGE_NAME_PREF + key, defaultValue);
			}
			else
			{
				result = System.getLong(sContentResolver, key, defaultValue);
			}
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get long value for a key related to any specific module */
	/* Return default value; if key is not stored or, data type of existing value is undefined by integer */
	public long getData(String moduleName, String key, long defaultValue)
	{
		Logger.i(TAG, "getData(" + moduleName + ", " + key + ", " + defaultValue + "L, " + mIsHikeSpecificData + ")");

		long result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = getData((moduleName + STRING_CLOSURE + key), defaultValue);
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get String value for a key */
	/* Return null; if key is not stored */
	public String getData(String key)
	{
		Logger.i(TAG, "getData(" + key + ", " + mIsHikeSpecificData + ")");

		String result;

		if (!Utils.isBlank(key))
		{
			if (mIsHikeSpecificData)
			{
				result = System.getString(sContentResolver, HIKE_PACKAGE_NAME_PREF + key);
			}
			else
			{
				result = System.getString(sContentResolver, key);
			}
		}
		else
		{
			result = null;
		}

		return result;
	}

	/* Get String value for a key related to any specific module */
	/* Return null; if key is not stored */
	public String getData(String moduleName, String key)
	{
		Logger.i(TAG, "getData(" + moduleName + ", " + key + ", " + mIsHikeSpecificData + ")");

		String result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = getData(moduleName + STRING_CLOSURE + key);
		}
		else
		{
			result = null;
		}

		return result;
	}

	/* Get String-set value for a key */
	/* Return default value; if key is not stored or, data type of existing value is undefined by String-set */
	public Set<String> getData(String key, Set<String> defaultValue)
	{
		Logger.i(TAG, "getData(" + key + ", " + defaultValue + ", " + mIsHikeSpecificData + ")");

		Set<String> result;

		if (!Utils.isBlank(key))
		{
			String transformedValue;
			if (mIsHikeSpecificData)
			{
				transformedValue = System.getString(sContentResolver, HIKE_PACKAGE_NAME_PREF + key);
			}
			else
			{
				transformedValue = System.getString(sContentResolver, key);
			}

			if (transformedValue == null)
			{
				Logger.w(TAG, "getData(), The key with name: " + key + " is either undefined or, not stored yet.");
				result = defaultValue;
			}
			else if (transformedValue.length() == 0)
			{
				result = new HashSet<String>(0);
			}
			else
			{
				String[] values = transformedValue.split(STRING_SEPARATOR);
				result = new HashSet<String>(values.length);

				for (String value : values)
				{
					result.add(value);
				}

				if (transformedValue.endsWith(STRING_SEPARATOR + STRING_SEPARATOR))
				{
					result.add(STRING_EMPTY);
				}
			}
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get String-set value for a key related to any specific module */
	/* Return default value; if key is not stored or, data type of existing value is undefined by String-set */
	public Set<String> getData(String moduleName, String key, Set<String> defaultValue)
	{
		Logger.i(TAG, "getData(" + moduleName + ", " + key + ", " + defaultValue + ", " + mIsHikeSpecificData + ")");

		Set<String> result;

		if (!Utils.isBlank(moduleName) && !Utils.isBlank(key))
		{
			result = getData((moduleName + STRING_CLOSURE + key), defaultValue);
		}
		else
		{
			result = defaultValue;
		}

		return result;
	}

	/* Get Uri for a stored key */
	public Uri getUriFor(String keyName)
	{
		Logger.i(TAG, "getUriFor(" + keyName + ")");

		Uri uri;

		if (!Utils.isBlank(keyName))
		{
			if (mIsHikeSpecificData)
			{
				uri = System.getUriFor(HIKE_PACKAGE_NAME_PREF + keyName);
			}
			else
			{
				uri = System.getUriFor(keyName);
			}
		}
		else
		{
			uri = null;
		}

		return uri;
	}

	/* Get Uri for a stored key related to any specific module */
	public Uri getUriFor(String moduleName, String keyName)
	{
		Logger.i(TAG, "getUriFor(" + moduleName + ", " + keyName + ")");

		Uri uri;

		if (!Utils.isBlank(keyName))
		{
			uri = getUriFor(moduleName + STRING_CLOSURE + keyName);
		}
		else
		{
			uri = null;
		}

		return uri;
	}

	/* Delete stored key-value pair specifically stored for application-only use */
	/* Return true; if operation was successful */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteHikeSpecificData(String key)
	{
		Logger.i(TAG, "deleteHikeSpecificData(" + key + ")");

		boolean result = false;

		if (key != null)
		{
			if (mIsHikeSpecificData)
			{
				Uri uri = System.getUriFor(HIKE_PACKAGE_NAME_PREF + key);
				if (uri != null)
				{
					result = ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_PACKAGE_NAME_PREF + key, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
							new String[] { (HIKE_PACKAGE_NAME_PREF + key) })) > 0);

					if (!updateHikeKeysInfo(key, true))
					{
						Logger.e(TAG, "deleteHikeSpecificData(), Key with name '" + key + "' was deleted but same could not be updated to key info.");
					}
				}
				else
				{
					Logger.w(TAG, "deleteHikeSpecificData(), No such key with name '" + key + "' was stored.");
					result = true;
				}
			}
			else
			{
				throw sSystemSettingsUpdateException;
			}
		}
		else
		{
			Logger.e(TAG, "deleteHikeSpecificData(), Invalid key argument to delete.");
		}

		return result;
	}

	/* Delete stored key-value pairs specifically stored for application-only use */
	/* Return the number of keys removed */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public int deleteHikeSpecificData(List<String> keys)
	{
		Logger.i(TAG, "deleteHikeSpecificData(" + keys + ")");

		int result = 0;
		int size = ((keys == null) ? 0 : keys.size());

		if (size > 0)
		{
			if (mIsHikeSpecificData)
			{
				ArrayList<String> removedKeyList = new ArrayList<String>(size);
				Uri uri;

				for (String key : keys)
				{
					uri = System.getUriFor(HIKE_PACKAGE_NAME_PREF + key);
					if (uri != null)
					{
						if ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_PACKAGE_NAME_PREF + key, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
								new String[] { (HIKE_PACKAGE_NAME_PREF + key) })) > 0)
						{
							removedKeyList.add(key);
							result++;
						}
					}
					else
					{
						Logger.w(TAG, "deleteHikeSpecificData(), No such key with name '" + key + "' was stored.");
						result++;
					}
				}

				if (removedKeyList.size() > 0)
				{
					if (!updateHikeKeysInfo(removedKeyList, true))
					{
						Logger.e(TAG, "deleteHikeSpecificData(), Keys with name in " + removedKeyList + " were deleted but same could not be updated to key info.");
					}
				}
			}
			else
			{
				throw sSystemSettingsUpdateException;
			}
		}
		else
		{
			Logger.e(TAG, "deleteHikeSpecificData(), Invalid keys argument to delete.");
		}

		return result;
	}

	/* Delete stored key-value pairs related to any specific module and specifically stored for application-only use */
	/* Return true; if operation was successful */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteHikeSpecificDataForModule(String moduleName)
	{
		Logger.i(TAG, "deleteHikeSpecificDataForModule(" + moduleName + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = false;

		if (!Utils.isBlank(moduleName))
		{
			result = true;

			if (!Utils.isBlank(keysListString))
			{
				String[] keys = keysListString.split(STRING_SEPARATOR);
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					if (key.startsWith(moduleName + STRING_CLOSURE))
					{
						removableKeyList.add(key);
						count++;
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}
		else
		{
			Logger.e(TAG, "deleteHikeSpecificData(), Invalid module argument to delete.");
		}

		return result;
	}

	/* Delete stored key-value pairs related to any of supplied modules and specifically stored for application-only use */
	/* Return true; if operation was successful */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteHikeSpecificDataForModules(List<String> moduleNames)
	{
		Logger.i(TAG, "deleteHikeSpecificDataForModules(" + moduleNames + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = false;
		int size = ((moduleNames == null) ? 0 : moduleNames.size());
		for (int i = (size - 1); i >= 0; i--)
		{
			if (Utils.isBlank(moduleNames.get(i)))
			{
				moduleNames.remove(i);
				size--;
			}
		}

		if (size > 0)
		{
			result = true;

			if (!Utils.isBlank(keysListString))
			{
				String[] keys = keysListString.split(STRING_SEPARATOR);
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					for (String module : moduleNames)
					{
						if (key.startsWith(module + STRING_CLOSURE))
						{
							removableKeyList.add(key);
							count++;
							break;
						}
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}
		else
		{
			Logger.e(TAG, "deleteHikeSpecificData(), Invalid modules argument to delete.");
		}

		return result;
	}

	/* Delete all stored key-value pairs specifically stored for application-only use */
	/* Return true; if all keys were removed successfully */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteAllHikeSpecificData()
	{
		Logger.i(TAG, "deleteAllHikeSpecificData()");

		final String keysListString = mHikeKeysListString;
		boolean result = true;

		if (!Utils.isBlank(keysListString))
		{
			String[] keys = keysListString.split(STRING_SEPARATOR);

			result = (deleteHikeSpecificData(Arrays.asList(keys)) == keys.length);
		}

		return result;
	}

	/* Delete all stored key-value pairs specifically stored for application-only use except supplied key */
	/* It facilitates preferential clearing of data */
	/* Return true; if all applicable keys were removed successfully */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteAllHikeSpecificDataExcept(String excludedKey)
	{
		Logger.i(TAG, "deleteAllHikeSpecificDataExcept(" + excludedKey + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = true;

		if (!Utils.isBlank(keysListString))
		{
			String[] keys = keysListString.split(STRING_SEPARATOR);

			if (Utils.isBlank(excludedKey))
			{
				result = (deleteHikeSpecificData(Arrays.asList(keys)) == keys.length);
			}
			else
			{
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					if (!key.equals(excludedKey))
					{
						removableKeyList.add(key);
						count++;
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}

		return result;
	}

	/* Delete all stored key-value pairs specifically stored for application-only use except supplied keys */
	/* It facilitates preferential clearing of data */
	/* Return true; if all applicable keys were removed successfully */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteAllHikeSpecificDataExcept(List<String> keyNames)
	{
		Logger.i(TAG, "deleteAllHikeSpecificDataExcept(" + keyNames + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = true;
		int size = ((keyNames == null) ? 0 : keyNames.size());
		for (int i = (size - 1); i >= 0; i--)
		{
			if (Utils.isBlank(keyNames.get(i)))
			{
				keyNames.remove(i);
				size--;
			}
		}

		if (!Utils.isBlank(keysListString))
		{
			String[] keys = keysListString.split(STRING_SEPARATOR);

			if (size <= 0)
			{
				result = (deleteHikeSpecificData(Arrays.asList(keys)) == keys.length);
			}
			else
			{
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					if (!keyNames.contains(key))
					{
						removableKeyList.add(key);
						count++;
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}

		return result;
	}

	/* Delete all stored key-value pairs specifically stored for application-only use except key-value pairs recognized by supplied module-identifier */
	/* It facilitates preferential clearing of data */
	/* Return true; if all applicable keys were removed successfully */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteAllHikeSpecificDataExceptModule(String excludedModuleName)
	{
		Logger.i(TAG, "deleteAllHikeSpecificDataExceptModules(" + excludedModuleName + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = true;

		if (!Utils.isBlank(keysListString))
		{
			String[] keys = keysListString.split(STRING_SEPARATOR);

			if (Utils.isBlank(excludedModuleName))
			{
				result = (deleteHikeSpecificData(Arrays.asList(keys)) == keys.length);
			}
			else
			{
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					if (!key.startsWith(excludedModuleName + STRING_CLOSURE))
					{
						removableKeyList.add(key);
						count++;
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}

		return result;
	}

	/* Delete all stored key-value pairs specifically stored for application-only use except key-value pairs recognized by supplied module-identifiers */
	/* It facilitates preferential clearing of data */
	/* Return true; if all applicable keys were removed successfully */
	/* Throw RunTimeException; if trying to delete commonly used system-wide key-value data */
	public boolean deleteAllHikeSpecificDataExceptModules(List<String> excludedModuleNames)
	{
		Logger.i(TAG, "deleteAllHikeSpecificDataExceptModules(" + excludedModuleNames + ")");

		final String keysListString = mHikeKeysListString;
		boolean result = true;
		int size = ((excludedModuleNames == null) ? 0 : excludedModuleNames.size());
		for (int i = (size - 1); i >= 0; i--)
		{
			if (Utils.isBlank(excludedModuleNames.get(i)))
			{
				excludedModuleNames.remove(i);
				size--;
			}
		}

		if (!Utils.isBlank(keysListString))
		{
			String[] keys = keysListString.split(STRING_SEPARATOR);

			if (size <= 0)
			{
				result = (deleteHikeSpecificData(Arrays.asList(keys)) == keys.length);
			}
			else
			{
				ArrayList<String> removableKeyList = new ArrayList<String>(keys.length);
				int count = 0;

				for (String key : keys)
				{
					boolean isNeedToRemove = true;

					for (String excludedModule : excludedModuleNames)
					{
						if (key.startsWith(excludedModule + STRING_CLOSURE))
						{
							isNeedToRemove = false;
							break;
						}
					}

					if (isNeedToRemove)
					{
						removableKeyList.add(key);
						count++;
					}
				}

				if (count > 0)
				{
					result = (deleteHikeSpecificData(removableKeyList) == count);
				}
			}
		}

		return result;
	}

	/* Update and maintain list of all keys to key-history specifically used for application-only data */
	/* Return true; if operation was successful */
	private boolean updateHikeKeysInfo(Object keyInfo, boolean isRemoving)
	{
		Logger.d(TAG, "updateHikeKeysInfo(" + keyInfo + ", " + isRemoving + ")");

		boolean result;

		if (isRemoving)
		{
			synchronized (sHikeKeyInfoEditLock)
			{
				result = removeAndUpdateHikeKeysInfo(keyInfo);
			}
		}
		else
		{
			synchronized (sHikeKeyInfoEditLock)
			{
				result = addAndUpdateHikeKeysInfo((String) keyInfo);
			}
		}

		return result;
	}

	/* Add any new key to key-history specifically used for application-only data */
	/* Return true; if operation was successful */
	private boolean addAndUpdateHikeKeysInfo(String key)
	{

		boolean result = true;
		final String currentKeysListString = mHikeKeysListString;

		if ((mHikeKeysListString != null) && !mHikeKeysListString.contains(key))
		{
			mHikeKeysListString = mHikeKeysListString + STRING_SEPARATOR + key;
		}
		else if (mHikeKeysListString == null)
		{
			mHikeKeysListString = key;
		}

		if (mHikeKeysListString.equals(currentKeysListString))
		{
			Logger.v(TAG, "addAndUpdateHikeKeysInfo(), Key is already present.");
		}
		else
		{
			synchronized (sDatabaseOperateLock)
			{
				result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
				if (!result)
				{
					result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
				}
			}
		}

		return result;
	}

	/* Remove any deleted key from key-history specifically used for application-only data */
	/* Return true; if operation was successful */
	private boolean removeAndUpdateHikeKeysInfo(Object keyInfo)
	{

		boolean result = true;
		final String currentKeysListString = mHikeKeysListString;

		if (mHikeKeysListString != null)
		{
			if (keyInfo instanceof ArrayList<?>)
			{
				ArrayList<?> keys = (ArrayList<?>) keyInfo;
				String key;

				for (Object keyObject : keys)
				{
					key = (String) keyObject;
					if (mHikeKeysListString.contains(STRING_SEPARATOR + key))
					{
						mHikeKeysListString = mHikeKeysListString.replace((STRING_SEPARATOR + key), STRING_EMPTY);
					}
					else if (mHikeKeysListString.contains(key + STRING_SEPARATOR))
					{
						mHikeKeysListString = mHikeKeysListString.replace((key + STRING_SEPARATOR), STRING_EMPTY);
					}
					else if (mHikeKeysListString.equals(key))
					{
						mHikeKeysListString = null;
						break;
					}
				}
				keys.clear();

				if (mHikeKeysListString == null)
				{
					Uri uri = System.getUriFor(HIKE_SETTINGS_KEY_LIST);
					if (uri != null)
					{
						synchronized (sDatabaseOperateLock)
						{
							result = ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_SETTINGS_KEY_LIST, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
									new String[] { (HIKE_SETTINGS_KEY_LIST) })) > 0);
							if (!result)
							{
								result = ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_SETTINGS_KEY_LIST, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
										new String[] { (HIKE_SETTINGS_KEY_LIST) })) > 0);
							}
						}
					}
				}
				else if (mHikeKeysListString.equals(currentKeysListString))
				{
					Logger.wtf(TAG, "removeAndUpdateHikeKeysInfo(), None of the keys is present.");
				}
				else
				{
					synchronized (sDatabaseOperateLock)
					{
						result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
						if (!result)
						{
							result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
						}
					}
				}
			}
			else if (keyInfo instanceof String)
			{
				String key = (String) keyInfo;

				if (mHikeKeysListString.contains(STRING_SEPARATOR + key))
				{
					mHikeKeysListString = mHikeKeysListString.replace((STRING_SEPARATOR + key), STRING_EMPTY);
				}
				else if (mHikeKeysListString.equals(key))
				{
					mHikeKeysListString = null;
				}
				else if (mHikeKeysListString.contains(key + STRING_SEPARATOR))
				{
					mHikeKeysListString = mHikeKeysListString.replace((key + STRING_SEPARATOR), STRING_EMPTY);
				}

				if (mHikeKeysListString == null)
				{
					Uri uri = System.getUriFor(HIKE_SETTINGS_KEY_LIST);
					if (uri != null)
					{
						synchronized (sDatabaseOperateLock)
						{
							result = ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_SETTINGS_KEY_LIST, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
									new String[] { (HIKE_SETTINGS_KEY_LIST) })) > 0);
							if (!result)
							{
								result = ((sContentResolver.delete(Uri.parse(uri.toString().replace(HIKE_SETTINGS_KEY_LIST, STRING_EMPTY)), SYSTEM_DATABASE_WHERE,
										new String[] { (HIKE_SETTINGS_KEY_LIST) })) > 0);
							}
						}
					}
				}
				else if (mHikeKeysListString.equals(currentKeysListString))
				{
					Logger.wtf(TAG, "removeAndUpdateHikeKeysInfo(), No such key is present.");
				}
				else
				{
					synchronized (sDatabaseOperateLock)
					{
						result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
						if (!result)
						{
							result = System.putString(sContentResolver, HIKE_SETTINGS_KEY_LIST, mHikeKeysListString);
						}
					}
				}
			}
			else
			{
				Logger.e(TAG, "removeAndUpdateHikeKeysInfo(), keyInfo argument is invalid to remove and update.");
				result = false;
			}
		}
		else
		{
			Logger.w(TAG, "removeAndUpdateHikeKeysInfo(), No key history was found.");
		}

		return result;
	}

	/**
	 * File : SystemSettingsUpdateException.java Content: It is a subclass of exception, designed to handle run-time exception; if trying to delete any commonly used system-wide
	 * key-value pair.
	 */
	public static class SystemSettingsUpdateException extends RuntimeException
	{

		/**
		 * serialVersionUID: It should be changed, if needs to handle more cases in future similarly like version-control.
		 */
		private static final long serialVersionUID = 1L;

		private static final String EXCEPTION_INFO = "\nException Information: ";

		private static final String VERSION_INFO = "_v_";

		private static final String ERROR_DELETE = "You are trying to delete system settings data. You must not delete settings key which is not specific to your application, rather you can update it by calling saveData(); if it is required to do so.";

		private static final String ERROR_NPE = "You are trying to save a null value in System Database.";

		/* Constructor to this class, which can be accessed only by it's outer class */
		private SystemSettingsUpdateException()
		{
			super(ERROR_DELETE + EXCEPTION_INFO + SystemSettingsUpdateException.class.getSimpleName() + VERSION_INFO + serialVersionUID);
		}
	}
}