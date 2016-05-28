package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPreviewImageDownloadTask;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class StickerPreviewImageDownloadGcmTask implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		Bundle extra = taskParams.getExtras();

		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);
		int type = extra.getInt(HikeConstants.TYPE);
		StickerPreviewImageDownloadTask stickerPreviewImageDownloadTask = new StickerPreviewImageDownloadTask(categoryId, type);
		stickerPreviewImageDownloadTask.execute();

		return null;
	}
}