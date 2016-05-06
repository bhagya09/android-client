package com.bsb.hike.backup.iface;

/**
 *
 * Provides an interface for step-by-step execution of backup/restore tasks (constructor-executor-destructor).
 *
 * Created by Atul M on 07/04/16.
 */
public interface BackupRestoreTaskLifecycle
{
	/**
	 * Do initialization/obj construction
	 */
	boolean doPreTask();

	/**
	 * Execute
	 */
	void doTask();

	/**
	 * Cleanup
	 */
	void doPostTask();
}
