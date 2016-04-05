package com.bsb.hike.backup.iface;

import com.bsb.hike.backup.iface.Backupable;
import com.bsb.hike.backup.iface.Restorable;

/**
 * Created by gauravmittal on 10/03/16.
 */
public interface BackupableRestorable extends Backupable, Restorable
{
	void selfDestruct();
}
