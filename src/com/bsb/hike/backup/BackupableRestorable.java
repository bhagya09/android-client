package com.bsb.hike.backup;

/**
 * Created by gauravmittal on 10/03/16.
 */
public interface BackupableRestorable
{

	public boolean preBackupSetup() throws Exception;

	public void backup() throws Exception;

	public void postBackupSetup() throws Exception;

	public boolean preRestoreSetup() throws Exception;

	public void restore() throws Exception;

	public void postRestoreSetup() throws Exception;

	public void finish();

	public void selfDestruct();
}
