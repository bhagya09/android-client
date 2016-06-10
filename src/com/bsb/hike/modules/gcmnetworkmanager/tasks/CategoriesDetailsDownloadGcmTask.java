package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class CategoriesDetailsDownloadGcmTask implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		Bundle extra = taskParams.getExtras();
		boolean isNewUser = extra.getBoolean(HikeConstants.IS_NEW_USER, false);

		if (isNewUser)
		{
			StickerManager.getInstance().checkAndDownLoadStickerData();
		}
		else
		{
			StickerManager.getInstance().refreshDownloadPacksMetadata(true);
		}

		return null;
	}
}