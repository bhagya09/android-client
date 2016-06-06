package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.utils.StickerManager;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class MultiStickerDownloadGcmTask implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		Bundle extra = taskParams.getExtras();
		try
		{
			String categoryId = extra.getString(HikeConstants.CATEGORY_ID);

			StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
			if (category == null)
			{
				category = HikeConversationsDatabase.getInstance().getStickerCategoryforId(categoryId);
			}

			StickerConstants.DownloadType downloadType = (StickerConstants.DownloadType) extra.getSerializable(HikeConstants.DOWNLOAD_TYPE);
			JSONObject bodyJson = new JSONObject(extra.getString(HikeConstants.BODY));

			StickerManager.getInstance().initialiseDownloadStickerPackTask(category, downloadType, bodyJson);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return null;
	}
}