package com.bsb.hike.backup.tasks;

import android.os.AsyncTask;

import com.bsb.hike.backup.iface.BackupRestoreTaskLifecycle;
import com.bsb.hike.utils.Logger;

/**
 * This task is responsible for executing lifecycle for {@link BackupRestoreTaskLifecycle BackupRestoreTaskLifecycle}. Created by atul on 05/04/16.
 */
public class BackupRestoreExecutorTask<T extends BackupRestoreTaskLifecycle> extends AsyncTask<T, Void, Void>
{
	private final String TAG = BackupRestoreExecutorTask.class.getSimpleName();

	@SafeVarargs
	@Override
	protected final Void doInBackground(T... jobs)
	{
		if (jobs == null || jobs.length < 1)
		{
			Logger.d(TAG, "No jobs available! Abort");
		}

		// Currently supporting 1 job for a task
		T job = jobs[0];

		Logger.d(TAG, "Lifecycle execution");

		boolean preBackupStatus = job.doPreTask();
		if (preBackupStatus)
		{
			job.doTask();
		}
		else
		{
			Logger.d(TAG, "Lifecycle pre-setup failed. Abort.");
		}
		job.doPostTask();
		Logger.d(TAG, "Lifecycle complete");
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid)
	{
		// Not in use at the moment. The respective BackupRestoreTaskLifecycle implementations notify the app via PubSub events.
	}
}
