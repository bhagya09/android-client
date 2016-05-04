package com.bsb.hike.modules.gcmnetworkmanager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 28/04/16.
 */
public class GcmNwMgrService extends GcmTaskService
{

	@Override
	public int onRunTask(TaskParams taskParams)
	{
        switch(taskParams.getTag())
        {
            default:
                break;
        }
		return GcmNetworkManager.RESULT_SUCCESS;
	}
}
