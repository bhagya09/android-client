package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class SingleStickerTagDownloadGcmTask implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		Bundle extra = taskParams.getExtras();
		String stickerId = extra.getString(HikeConstants.STICKER_ID);
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);

		StickerManager.getInstance().initiateSingleStickerTagDownloadTask(stickerId, categoryId);
		return null;
	}
}