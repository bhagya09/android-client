package com.bsb.hike.modules.gcmnetworkmanager;

import com.bsb.hike.modules.gcmnetworkmanager.tasks.CategoryDetailsDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.CategoryPalleteImageDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.CognitoUploadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.DefaultTagsDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.GcmTaskConstants;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.MultiStickerDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.MultiStickerTagDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.SingleStickerDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.SingleStickerTagDownloadGcmTask;
import com.bsb.hike.modules.gcmnetworkmanager.tasks.StickerPreviewImageDownloadGcmTask;
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

        if(tag.startsWith(GcmTaskConstants.SINGLE_STICKER_GCM_TASK))
        {
            SingleStickerDownloadGcmTask singleStickerDownloadGcmTask = new SingleStickerDownloadGcmTask();
            singleStickerDownloadGcmTask.execute(taskParams);
        }
		else if (tag.startsWith(GcmTaskConstants.COGNITO_UPLOAD_GCM_TASK))
		{
			CognitoUploadGcmTask cognitoUploadGcmTask = new CognitoUploadGcmTask();
			cognitoUploadGcmTask.execute(taskParams);
		}
        else if (tag.startsWith(GcmTaskConstants.MULTI_STICKER_IMAGE_GCM_TASK) || tag.startsWith(GcmTaskConstants.MULTI_STICKER_GCM_TASK))
        {
            MultiStickerDownloadGcmTask multiStickerDownloadGcmTask = new MultiStickerDownloadGcmTask();
            multiStickerDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.MULTI_STICKER_TAG_GCM_TASK))
        {
            MultiStickerTagDownloadGcmTask multiStickerTagDownloadGcmTask = new MultiStickerTagDownloadGcmTask();
            multiStickerTagDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.SINGLE_STICKER_TAG_GCM_TASK))
        {
            SingleStickerTagDownloadGcmTask singleStickerTagDownloadGcmTask = new SingleStickerTagDownloadGcmTask();
            singleStickerTagDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.DEFAULT_TAGS_GCM_TASK))
        {
            DefaultTagsDownloadGcmTask defaultTagsDownloadGcmTask = new DefaultTagsDownloadGcmTask();
            defaultTagsDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.STICKER_PREVIEW_IMAGE_GCM_TASK))
        {
            StickerPreviewImageDownloadGcmTask stickerPreviewImageDownloadGcmTask = new StickerPreviewImageDownloadGcmTask();
            stickerPreviewImageDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.CATEGORY_DETAILS_GCM_TASK))
        {
            CategoryDetailsDownloadGcmTask categoryDetailsDownloadGcmTask = new CategoryDetailsDownloadGcmTask();
            categoryDetailsDownloadGcmTask.execute(taskParams);
        }
        else if (tag.startsWith(GcmTaskConstants.CATEGORY_PALLETE_IMAGE_GCM_TASK))
        {
            CategoryPalleteImageDownloadGcmTask categoryPalleteImageDownloadGcmTask = new CategoryPalleteImageDownloadGcmTask();
            categoryPalleteImageDownloadGcmTask.execute(taskParams);
        }

		return GcmNetworkManager.RESULT_SUCCESS;
	}
}
