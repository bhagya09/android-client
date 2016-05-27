package com.bsb.hike.modules.gcmnetworkmanager;

import com.bsb.hike.modules.gcmnetworkmanager.tasks.CognitoUploadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.GcmTaskConstants;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.StickerGcmTasks;
import com.bsb.hike.utils.Logger;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 28/04/16.
 */
public class GcmNwMgrService extends GcmTaskService
{
	private final String TAG = getClass().getSimpleName();
	@Override
	public int onRunTask(TaskParams taskParams)
	{
        String tag = taskParams.getTag();

		Logger.d(TAG, "Got OnRunTask for tag : " + tag);

        if(tag.startsWith(GcmTaskConstants.STICKER_GCM_TASKS_PREFIX_KEY))
        {
            StickerGcmTasks stickerGcmTasks = new StickerGcmTasks();
            stickerGcmTasks.execute(taskParams);
        }
		else if (tag.startsWith(GcmTaskConstants.COGNITO_UPLOAD_GCM_TASK))
		{
			CognitoUploadGcmTask cognitoUploadGcmTask = new CognitoUploadGcmTask();
			cognitoUploadGcmTask.execute(taskParams);
		}
		return GcmNetworkManager.RESULT_SUCCESS;
	}
}
