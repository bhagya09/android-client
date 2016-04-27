package com.bsb.hike.backup;

import org.json.JSONException;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.backup.iface.BackupRestoreTaskLifecycle;
import com.bsb.hike.backup.impl.HikeSettingsCloudBackup;
import com.bsb.hike.backup.impl.HikeSettingsCloudRestore;
import com.bsb.hike.backup.tasks.BackupRestoreExecutorTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

/**
 * Created by atul on 06/04/16.
 */
public class HikeCloudSettingsManager implements HikePubSub.Listener
{

	private volatile static HikeCloudSettingsManager mInstance;

	public static HikeCloudSettingsManager getInstance()
	{
		if (mInstance == null)
		{
			synchronized (HikeCloudSettingsManager.class)
			{
				if (mInstance == null)
				{
					mInstance = new HikeCloudSettingsManager();
				}
			}
		}
		return mInstance;
	}

	String[] pubsubListeners = { HikePubSub.CLOUD_SETTINGS_BACKUP_SUCESS, HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED,
			HikePubSub.CLOUD_SETTINGS_RESTORE_SUCCESS };

	private HikeCloudSettingsManager()
	{
		HikeMessengerApp.getPubSub().addListeners(this, pubsubListeners);
	}

	public void doBackup()
	{
		if(!Utils.isSettingsBackupEnabled())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, null);
			return;
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BackupRestore.RUX_BACKUP_PENDING, true);
		HikeSettingsCloudBackup settingsCloudBackup = new HikeSettingsCloudBackup();
		new BackupRestoreExecutorTask<BackupRestoreTaskLifecycle>().execute(settingsCloudBackup);
	}

	public void doRestore(String backupData)
	{
		if(!Utils.isSettingsBackupEnabled())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED, null);
			return;
		}

		doRestoreSkipEnableCheck(backupData);
	}

	/**
	 *In sign-up flow, do a GET call no matter if the feature is enabled or not
	 */
	public void doRestoreSkipEnableCheck(String backupData)
	{
		HikeSettingsCloudRestore settingsRestorable = new HikeSettingsCloudRestore();

		if (!TextUtils.isEmpty(backupData))
		{
			try
			{
				settingsRestorable.setBackupDataJSON(backupData);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				// Its OK. Need to GET from server
			}
		}

		new BackupRestoreExecutorTask<BackupRestoreTaskLifecycle>().execute(settingsRestorable);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED))
		{
			// TODO Schedule another upload
		}
		else if (type.equals(HikePubSub.CLOUD_SETTINGS_BACKUP_SUCESS))
		{
			// Clear pending flag
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BackupRestore.RUX_BACKUP_PENDING, false);
		}
		else if (type.equals(HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED))
		{
			// TODO
		}
		else if (type.equals(HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED))
		{
			// TODO
		}
	}

	public void shutDown()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubsubListeners);
	}
}
