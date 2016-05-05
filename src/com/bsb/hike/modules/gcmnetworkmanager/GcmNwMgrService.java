package com.bsb.hike.modules.gcmnetworkmanager;

import com.bsb.hike.modules.gcmnetworkmanager.tasks.GcmTaskConstants;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.SingleStickerDownloadGcmTask;
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
        String tag = taskParams.getTag();

        if(tag.startsWith(GcmTaskConstants.SINGLE_STICKER_GCM_TASK))
        {
            SingleStickerDownloadGcmTask singleStickerDownloadGcmTask = new SingleStickerDownloadGcmTask();
            singleStickerDownloadGcmTask.execute(taskParams);
        }
		return GcmNetworkManager.RESULT_SUCCESS;
	}
}
