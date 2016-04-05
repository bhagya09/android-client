package com.bsb.hike.backup.iface;

import com.bsb.hike.backup.tasks.BackupRestoreExecutorTask;

/**
 * Use this while restoring data. Also required in {@link BackupRestoreExecutorTask BackupRestoreExecutorTask} Created by atul on 05/04/16.
 */
public interface Restorable
{
    boolean preRestoreSetup() throws Exception;

    void restore() throws Exception;

    void postRestoreSetup() throws Exception;

    void finish();
}
