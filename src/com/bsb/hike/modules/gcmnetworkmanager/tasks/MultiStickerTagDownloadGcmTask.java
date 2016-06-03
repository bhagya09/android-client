package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class MultiStickerTagDownloadGcmTask implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		Bundle extra = taskParams.getExtras();
		Set<String> stickerList = new HashSet<String>(extra.getStringArrayList(HikeConstants.STICKERS));
		Set<String> languages = new HashSet<String>(extra.getStringArrayList(HikeConstants.LANGUAGES));
		int state = extra.getInt(HikeConstants.STATE);

		StickerSearchManager.getInstance().downloadStickerTags(false, state, stickerList, languages);
		return null;
	}
}