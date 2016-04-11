package com.bsb.hike.backup;

import org.json.JSONException;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.backup.iface.BackupRestoreTaskLifecycle;
import com.bsb.hike.backup.impl.CloudSettingsBackupable;
import com.bsb.hike.backup.impl.CloudSettingsRestorable;
import com.bsb.hike.backup.tasks.BackupRestoreExecutorTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

/**
 * Created by atul on 06/04/16.
 */
public class CloudSettingsBackupManager implements HikePubSub.Listener
{

	private volatile static CloudSettingsBackupManager mInstance;

	public static CloudSettingsBackupManager getInstance()
	{
		if (mInstance == null)
		{
			synchronized (CloudSettingsBackupManager.class)
			{
				if (mInstance == null)
				{
					mInstance = new CloudSettingsBackupManager();
				}
			}
		}
		return mInstance;
	}

	String[] pubsubListeners = { HikePubSub.CLOUD_SETTINGS_BACKUP_SUCESS, HikePubSub.CLOUD_SETTINGS_BACKUP_FAILED, HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED,
			HikePubSub.CLOUD_SETTINGS_RESTORE_SUCCESS };

	private CloudSettingsBackupManager()
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
		CloudSettingsBackupable settingsBackupable = new CloudSettingsBackupable();
		new BackupRestoreExecutorTask<BackupRestoreTaskLifecycle>().execute(settingsBackupable);
	}

	public void doRestore(String backupData)
	{
		if(!Utils.isSettingsBackupEnabled())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED, null);
			return;
		}

		CloudSettingsRestorable settingsRestorable = new CloudSettingsRestorable();

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
