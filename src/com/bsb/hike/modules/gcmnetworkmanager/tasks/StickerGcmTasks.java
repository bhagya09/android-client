package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPreviewImageDownloadTask;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.StickerManager;
import com.edmodo.cropper.cropwindow.handle.Handle;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public class StickerGcmTasks implements IGcmTask
{
	@Override
	public Void execute(TaskParams taskParams)
	{
		String tag = taskParams.getTag();

		String requestType = (tag.split(":"))[0];

		Bundle extra = taskParams.getExtras();

		switch (requestType)
		{
		case GcmTaskConstants.SINGLE_STICKER_GCM_TASK:
			handleSingleStickerDownload(extra);
			break;
		case GcmTaskConstants.DEFAULT_TAGS_GCM_TASK:
			handleDefaultTagsDownload(extra);
			break;
		case GcmTaskConstants.CATEGORY_DETAILS_GCM_TASK:
			handleCategoryDetailsDownload(extra);
			break;
		case GcmTaskConstants.MULTI_STICKER_GCM_TASK:
		case GcmTaskConstants.MULTI_STICKER_IMAGE_GCM_TASK:
			handleMultiStickerDownload(extra);
			break;
		case GcmTaskConstants.MULTI_STICKER_TAG_GCM_TASK:
			handleMultiStickerTagDownload(extra);
			break;
		case GcmTaskConstants.SINGLE_STICKER_TAG_GCM_TASK:
			handleSingleStickerTagDownload(extra);
			break;
		case GcmTaskConstants.STICKER_PREVIEW_IMAGE_GCM_TASK:
			handleStickerPreviewImageDownload(extra);
			break;
		case GcmTaskConstants.CATEGORY_PALLETE_IMAGE_GCM_TASK:
			handleCategoryPalleteImageDownload(extra);
			break;

		}

		return null;
	}

	private void handleSingleStickerDownload(Bundle extra)
	{
		String stickerId = extra.getString(HikeConstants.STICKER_ID);
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);
		long msgId = extra.getLong(HikeConstants.MESSAGE_ID);
		boolean downloadMini = extra.getBoolean(HikeConstants.MINI_STICKER_IMAGE);

		ConvMessage convMessage = HikeConversationsDatabase.getInstance().getConvMessageForMsgId(msgId);

		StickerManager.getInstance().initiateSingleStickerDownloadTask(stickerId, categoryId, convMessage, downloadMini);
	}

	private void handleDefaultTagsDownload(Bundle extra)
	{
		Set<String> languages = new HashSet<String>(extra.getStringArrayList(HikeConstants.LANGUAGES));
		boolean isSignUp = extra.getBoolean(HikeConstants.IS_NEW_USER);

		StickerManager.getInstance().downloadDefaultTags(isSignUp, languages);
	}

	private void handleCategoryDetailsDownload(Bundle extra)
	{
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);
		StickerManager.getInstance().initialiseCategoryDetailsTask(categoryId);
	}

	private void handleMultiStickerDownload(Bundle extra)
	{
		try
		{
			StickerCategory category = (StickerCategory) extra.getSerializable(HikeConstants.CATEGORY_ID);
			StickerConstants.DownloadType downloadType = (StickerConstants.DownloadType) extra.getSerializable(HikeConstants.DOWNLOAD_TYPE);
			JSONObject bodyJson = new JSONObject(extra.getString(HikeConstants.BODY));

			StickerManager.getInstance().initialiseDownloadStickerPackTask(category, downloadType, bodyJson);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void handleMultiStickerTagDownload(Bundle extra)
	{
		Set<String> stickerList = new HashSet<String>(extra.getStringArrayList(HikeConstants.STICKERS));
		Set<String> languages = new HashSet<String>(extra.getStringArrayList(HikeConstants.LANGUAGES));
		int state = extra.getInt(HikeConstants.STATE);

		StickerSearchManager.getInstance().downloadStickerTags(false, state, stickerList, languages);
	}

	private void handleSingleStickerTagDownload(Bundle extra)
	{
		String stickerId = extra.getString(HikeConstants.STICKER_ID);
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);

		StickerManager.getInstance().initiateSingleStickerTagDownloadTask(stickerId, categoryId);
	}

	private void handleStickerPreviewImageDownload(Bundle extra)
	{
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);
		int type = extra.getInt(HikeConstants.TYPE);
		StickerPreviewImageDownloadTask stickerPreviewImageDownloadTask = new StickerPreviewImageDownloadTask(categoryId, type);
		stickerPreviewImageDownloadTask.execute();
	}

	private void handleCategoryPalleteImageDownload(Bundle extra)
	{
		String categoryId = extra.getString(HikeConstants.CATEGORY_ID);
		StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(categoryId);
		stickerPalleteImageDownloadTask.execute();
	}

}
