package com.bsb.hike.db;

import java.io.Serializable;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class BackupState implements Serializable
{

	private static final long serialVersionUID = 2L;

	private String _dbName;

	private int _dbVersion;

	private long _backupTime;

	private String stealthPattern;

	private boolean stealthModeSetupDone;

	private boolean shownFirstUnmarkStealthToast;

	private boolean showStealthInfoTip;

	public BackupState(String db, int version)
	{
		_dbName = db;
		_dbVersion = version;
		_backupTime = System.currentTimeMillis();
	}

	public int getDBVersion()
	{
		return _dbVersion;
	}

	public long getBackupTime()
	{
		return _backupTime;
	}

	public boolean backupPrefs(Context context)
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();
		stealthPattern = prefUtil.getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, "");
		stealthModeSetupDone = prefUtil.getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false);
		shownFirstUnmarkStealthToast = prefUtil.getData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, false);
		showStealthInfoTip = prefUtil.getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false);
		return true;
	}

	public boolean restorePrefs(Context context)
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();
		prefUtil.saveData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, stealthPattern);
		prefUtil.saveData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, stealthModeSetupDone);
		prefUtil.saveData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, shownFirstUnmarkStealthToast);
		prefUtil.saveData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, showStealthInfoTip);
		return true;
	}

	@Override
	public String toString()
	{
		String s = super.toString() + " " + _dbName + " : " + _dbVersion + " at " + _backupTime;
		return s;
	}

}
