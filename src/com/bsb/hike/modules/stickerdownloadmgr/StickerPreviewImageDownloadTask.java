package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPreviewImageDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private String catId;

	protected StickerPreviewImageDownloadTask(Handler handler, Context ctx, String taskId, String categoryId, IStickerResultListener callback)
	{
		super(handler, ctx, taskId , callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
		this.catId = categoryId;
	}

	@Override
	public STResult call() throws Exception
	{
		String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(catId);
		if (dirPath == null)
		{
			setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory does not exist for task : " + taskId);
			return STResult.DOWNLOAD_FAILED;
		}
		
		String previewImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT  + "/" + StickerManager.PREVIEW_IMAGE + StickerManager.OTHER_ICON_TYPE;
		
		try
		{
			File otherDir = new File(dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT);
			if (!otherDir.exists())
			{
				if (!otherDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory not created for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
			}
			
			String urlString = AccountUtils.base + "/stickers/preview?catId=" + catId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers/preview?catId=" + catId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
			JSONObject response = (JSONObject) download(null, HttpRequestType.GET);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)) || !catId.equals(response.getString(StickerManager.CATEGORY_ID)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
			Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String stickerData = data.getString(HikeConstants.PREVIEW_IMAGE);
			
			HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(catId, StickerManager.PREVIEW_IMAGE_TYPE));
			Utils.saveBase64StringToFile(new File(previewImagePath), stickerData);
			
		}
		catch(StickerException e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch(Exception e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(new StickerException(e));
			return STResult.DOWNLOAD_FAILED;
		}
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		super.postExecute(result);
	}

}
