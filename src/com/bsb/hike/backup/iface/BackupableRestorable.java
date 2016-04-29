package com.bsb.hike.backup.iface;

/**
 * Created by gauravmittal on 10/03/16.
 */
public interface BackupableRestorable
{
	boolean preBackupSetup() throws Exception;

	void backup() throws Exception;

	void postBackupSetup() throws Exception;

	boolean preRestoreSetup() throws Exception;

	void restore() throws Exception;

	void postRestoreSetup() throws Exception;

	void finish();

	void selfDestruct();
}
