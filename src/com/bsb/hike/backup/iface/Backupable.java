package com.bsb.hike.backup.iface;

import com.bsb.hike.backup.tasks.BackupRestoreExecutorTask;

/**
 *
 * Use this while backing up data. Also required in {@link BackupRestoreExecutorTask BackupRestoreExecutorTask} Created by atul on 05/04/16.
 */
public interface Backupable
{
	boolean preBackupSetup() throws Exception;

	void backup() throws Exception;

	void postBackupSetup() throws Exception;

	void finish();
}
