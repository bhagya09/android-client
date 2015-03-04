package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;
import java.io.FileOutputStream;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class SingleStickerDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String stkId;
	private String catId;
	private String taskId;
	private String largeStickerPath;
	

	protected SingleStickerDownloadTask(Handler handler, Context ctx, String taskId, String stkId, String catId, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.stkId = stkId;
		this.catId = catId;
		this.handler = handler;
		this.context = ctx;
	}

	@Override
	public STResult call() throws Exception
	{
		FileOutputStream fos = null;
		
		try
		{
			String urlString = AccountUtils.base + "/stickers?catId=" + catId + "&stId=" + stkId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers?catId=" + catId + "&stId=" + stkId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
			JSONObject response = (JSONObject) download(null, HttpRequestType.GET);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
			Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
			
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
			
			this.catId = response.getString(StickerManager.CATEGORY_ID);  //Fetching the category field from the response
			
			String stickerData = data.getString(stkId);
			
			String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(catId);

			if (dirPath == null)
			{
				setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory does not exist for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}

			largeStickerPath = dirPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stkId;
			String smallStickerPath = dirPath + HikeConstants.SMALL_STICKER_ROOT + "/" + stkId;

			File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
			if (!largeDir.exists())
			{
				if (!largeDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory not created for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
			}
			File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
			if (!smallDir.exists())
			{
				if (!smallDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory not created for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
			}

			Utils.saveBase64StringToFile(new File(largeStickerPath), stickerData);

			boolean isDisabled = data.optBoolean(HikeConstants.DISABLED_ST);
			if (!isDisabled)
			{
				Bitmap thumbnail = HikeBitmapFactory.scaleDownBitmap(largeStickerPath, StickerManager.SIZE_IMAGE, StickerManager.SIZE_IMAGE, true,false);

				if (thumbnail != null)
				{
					File smallImage = new File(smallStickerPath);
					BitmapUtils.saveBitmapToFile(smallImage, thumbnail);
					thumbnail.recycle();
				}
			}
		}
		catch (StickerException e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch (Exception e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(new StickerException(e));
			return STResult.DOWNLOAD_FAILED;
		}
		StickerManager.getInstance().checkAndRemoveUpdateFlag(catId);
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// Sending category back to the callee
		setResult(catId);
		super.postExecute(result);
		
	}
}
