package com.bsb.hike.modules.stickerdownloadmgr;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StickerSizeDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private String catId;
	private Bundle bundle;
	
	protected StickerSizeDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
		this.catId = category.getCategoryId();
	}

	@Override
	public STResult call() throws Exception
	{
		
		try
		{
			String urlString = AccountUtils.base + "/stickers/size?catId=" + catId + "&resId=" + Utils.getResolutionId();;
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers/size?catId=" + catId + "&resId=" + Utils.getResolutionId();;
			}
			
			setDownloadUrl(urlString);
			HttpResponse response = (HttpResponse) download(null, HttpRequestType.HEAD);
			int resCode = response.getStatusLine().getStatusCode();
			// Make sure the response code is 200.
			if (resCode == 200)
			{
				int size = Integer.parseInt(response.getFirstHeader(HikeConstants.SIZE).getValue());
				int numberOfStickers = Integer.parseInt(response.getFirstHeader(HikeConstants.NUMBER_OF_STICKERS).getValue());
				
				bundle = new Bundle();
				bundle.putInt(HikeConstants.SIZE, size);
				bundle.putInt(HikeConstants.NUMBER_OF_STICKERS, numberOfStickers);
				return STResult.SUCCESS;
			}
			else
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
		}
		catch (StickerException e)
		{
			setException(e);
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch (Exception e)
		{
			setException(new StickerException(e));
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			return STResult.DOWNLOAD_FAILED;
		}
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		setResult(bundle);
		super.postExecute(result);
	}
}
