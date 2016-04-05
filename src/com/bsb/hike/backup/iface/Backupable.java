package com.bsb.hike.backup.iface;

/**
 * Created by atul on 05/04/16.
 */
public interface Backupable
{
    boolean preBackupSetup() throws Exception;

    void backup() throws Exception;

    void postBackupSetup() throws Exception;

    void finish();
}
