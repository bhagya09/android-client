package com.bsb.hike.modules.stickerdownloadmgr;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

class StickerShopDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private Bundle bundle;
	private int offset;
	private Object resultObject;
	
	protected StickerShopDownloadTask(Handler handler, Context ctx, String taskId, int offset, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
		this.offset = offset;
	}

	@Override
	public STResult call() throws Exception
	{
		
		try
		{
			String urlString = AccountUtils.base + "/stickers/shop?offset=" + offset;
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers/shop?offset=" + offset ;
			}
			
			urlString += "&resId=" + Utils.getResolutionId();
			
			setDownloadUrl(urlString);
			
			JSONObject response = (JSONObject) download(null, HttpRequestType.GET);
			
			Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
			Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
			JSONArray data = response.optJSONArray(HikeConstants.DATA_2);
			if(null == data)
			{
				setException(new StickerException(StickerException.NULL_DATA));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null data for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
			resultObject = data;
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
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		setResult(resultObject);
		super.postExecute(result);
	}
}

