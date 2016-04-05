package com.bsb.hike.backup.tasks;

import android.os.AsyncTask;

import com.bsb.hike.backup.iface.Backupable;
import com.bsb.hike.backup.iface.Restorable;
import com.bsb.hike.utils.Logger;

/**
 * This task is responsible for executing lifecycle for {@link Backupable Backupables} and {@link Restorable Restorables}. Can additionally pass in
 * {@link com.bsb.hike.backup.tasks.BackupRestoreExecutorTask.Listener a listener} to get notifications Created by atul on 05/04/16.
 */
public class BackupRestoreExecutorTask<T> extends AsyncTask<T, Boolean, Boolean>
{
	private final String TAG = BackupRestoreExecutorTask.class.getSimpleName();

	@SafeVarargs
	@Override
	protected final Boolean doInBackground(T... jobs)
	{
		if (jobs == null || jobs.length < 1)
		{
			Logger.d(TAG, "No jobs available! Abort");
			return false;
		}

		// Currently supporting 1 job for a task
		T job = jobs[0];

		if (job instanceof Backupable)
		{
			Logger.d(TAG, "Backup execution");
			Backupable backupable = (Backupable) job;
			try
			{
				boolean preBackupStatus = backupable.preBackupSetup();
				if (preBackupStatus)
				{
					backupable.backup();
				}
				else
				{
					Logger.d(TAG, "Backup pre-setup failed. Abort.");
					return false;
				}
				backupable.postBackupSetup();
				backupable.finish();
				Logger.d(TAG, "Backup successful");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		else if (job instanceof Restorable)
		{
			Restorable restorable = (Restorable) job;
			try
			{
				boolean preBackupStatus = restorable.preRestoreSetup();
				if (preBackupStatus)
				{
					restorable.restore();
				}
				else
				{
					Logger.d(TAG, "Restore pre-setup failed. Abort.");
					return false;
				}
				restorable.postRestoreSetup();
				restorable.finish();
				Logger.d(TAG, "Restore successful");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean isSuccess)
	{
		// Not in use at the moment. The respective backupable/restorable implementations notify the app via PubSub events.
	}
}
