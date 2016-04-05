package com.bsb.hike.backup.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * Created by atul on 31/03/16.
 */
public class CloudBackupPrefInfo
{

	@IntDef({ TYPE_BOOL, TYPE_INT, TYPE_STRING, TYPE_STR_ARR })
	@Retention(RetentionPolicy.SOURCE)
	public @interface PrefDataType
	{
	}

	public static final int TYPE_BOOL = 1;

	public static final int TYPE_INT = 2;

	public static final int TYPE_STRING = 3;

	public static final int TYPE_STR_ARR = 4;

	private String keyName;

	private @HikeSharedPreferenceUtil.SharedPrefFile String prefName;

	private @CloudBackupPrefInfo.PrefDataType int dataType;

	private Object defaultValue;

	private String restoreValue;

	public CloudBackupPrefInfo(@NonNull String argKeyName, @HikeSharedPreferenceUtil.SharedPrefFile String argPrefFile, @PrefDataType int argDataType, @NonNull Object argDefault)
	{
		keyName = argKeyName;
		prefName = argPrefFile;
		dataType = argDataType;
		defaultValue = argDefault;
	}

	public CloudBackupPrefInfo(@NonNull String argSettingsName, @NonNull String argPrefName, @NonNull JSONObject argValueJSON) throws JSONException
	{
		keyName = argSettingsName;
		prefName = argPrefName;
		restoreValue = argValueJSON.getString(HikeConstants.BackupRestore.VALUE);

		int lDataType = argValueJSON.optInt(HikeConstants.BackupRestore.DATA_TYPE);
		switch (lDataType)
		{
		case TYPE_INT:
			dataType = TYPE_INT;
			break;

		case TYPE_STRING:
			dataType = TYPE_STRING;
			break;

		case TYPE_BOOL:
			dataType = TYPE_BOOL;
			break;

		case TYPE_STR_ARR:
			dataType = TYPE_STR_ARR;
			break;

		default:
			throw new JSONException("Received data-type not defined");
		}
	}

	public String getKeyName()
	{
		return keyName;
	}

	public String getPrefName()
	{
		return prefName;
	}

	public Object getDefaultValue()
	{
		return defaultValue;
	}

	public String getRestoreValue()
	{
		return restoreValue;
	}

	public int getDataType()
	{
		return dataType;
	}

	@Override
	public String toString()
	{
		String objInfo = null;
		try
		{
			objInfo = serialize().toString();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return objInfo;
	}

	public JSONObject serialize() throws JSONException
	{
		HikeSharedPreferenceUtil sharedPref = HikeSharedPreferenceUtil.getInstance(prefName);
		JSONObject settingJSON = new JSONObject();
		settingJSON.put(HikeConstants.BackupRestore.VALUE, sharedPref.getData(keyName, defaultValue.toString()));
		settingJSON.put(HikeConstants.BackupRestore.DATA_TYPE, dataType);
		return settingJSON;
	}

}
