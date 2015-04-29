package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.NetworkHandler.NetworkType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.modules.stickerdownloadmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class MultiStickerDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private StickerCategory category;
	private StickerConstants.DownloadType downloadType;
	private int stickerDownloadSize;
	private String taskId;
	private DownloadSource source;

	protected MultiStickerDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, StickerConstants.DownloadType downloadType, DownloadSource source, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.category  = category;
		this.downloadType = downloadType;
		this.handler = handler;
		this.context = ctx;
		this.taskId = taskId;
		this.source = source;
	}

	@Override
	public STResult call() throws Exception
	{
		Logger.d(getClass().getSimpleName(), "CategoryId: " + category.getCategoryId());

		String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(category.getCategoryId());
		if (directoryPath == null)
		{
			setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory does not exist for task : " + taskId);
			return STResult.DOWNLOAD_FAILED;
		}

		File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
		File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);
		int totalNumber = 0;
		boolean reachedEnd = false;
		boolean retry = true;
		int existingStickerNumber = 0;

		JSONArray existingStickerIds = new JSONArray();

		if (smallStickerDir.exists())
		{
			String[] stickerIds = smallStickerDir.list(StickerManager.getInstance().stickerFileFilter);
			for (String stickerId : stickerIds)
			{
				existingStickerIds.put(stickerId);
				existingStickerNumber ++;
				Logger.d(getClass().getSimpleName(), "Existing id: " + stickerId);
			}
		}
		else
		{
			smallStickerDir.mkdirs();
			Logger.d(getClass().getSimpleName(), "No existing sticker");
		}
		if (!largeStickerDir.exists())
			largeStickerDir.mkdirs();

		Utils.makeNoMediaFile(largeStickerDir);
		Utils.makeNoMediaFile(smallStickerDir);
		while (shouldContinue(reachedEnd, totalNumber, existingStickerNumber))
		{

			try
			{

				JSONObject request = new JSONObject();
				request.put(StickerManager.CATEGORY_ID, category.getCategoryId());
				request.put(HikeConstants.STICKER_IDS, existingStickerIds);
				request.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
				request.put(HikeConstants.NUMBER_OF_STICKERS, getStickerDownloadSize());
				if(source != null)
				{
					request.put(HikeConstants.DOWNLOAD_SOURCE, source.ordinal());
				}
				

				
				String urlString = AccountUtils.base + "/stickers";
				if (AccountUtils.ssl)
				{
					urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers";
				}
				setDownloadUrl(urlString);
				
				Logger.d(StickerDownloadManager.TAG,  "Sticker Download Task Request : " + request.toString());
				Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
				JSONObject response = (JSONObject) download(request, HttpRequestType.POST);

				if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
				{
					setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
				Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
				int length = response.toString().getBytes().length;

				if (length > Utils.getFreeSpace())
				{
					setException(new StickerException(StickerException.OUT_OF_SPACE));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory out of space for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}

				totalNumber = response.optInt(HikeConstants.TOTAL_STICKERS, -1);
				reachedEnd = response.optBoolean(HikeConstants.REACHED_STICKER_END);
				Logger.d(getClass().getSimpleName(), "Reached end? " + reachedEnd);
				Logger.d(getClass().getSimpleName(), "Sticker count: " + totalNumber);
				JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
				for (Iterator<String> keys = data.keys(); keys.hasNext();)
				{
					String stickerId = keys.next();
					String stickerData = data.getString(stickerId);
					existingStickerIds.put(stickerId);
					existingStickerNumber++;

					try
					{
						byte[] byteArray = StickerManager.getInstance().saveLargeStickers(largeStickerDir.getAbsolutePath(), stickerId, stickerData);
						StickerManager.getInstance().saveSmallStickers(smallStickerDir.getAbsolutePath(), stickerId, byteArray);
					}
					catch (FileNotFoundException e)
					{
						Logger.w(getClass().getSimpleName(), e);
					}
					catch (IOException e)
					{
						Logger.w(getClass().getSimpleName(), e);
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

			if (getRetryPolicy() != null)
			{
				((DefaultRetryPolicy) getRetryPolicy()).reset();
			}
			if(totalNumber != 0)
			{
				sendProgressOrResult(null, true, existingStickerNumber/totalNumber);
			}
			if(category.getTotalStickers() != totalNumber)
			{
				category.setTotalStickers(totalNumber);
				HikeConversationsDatabase.getInstance().updateStickerCountForStickerCategory(category.getCategoryId(), totalNumber);
			}
		}
		
		if(isSucessfull(reachedEnd, totalNumber, existingStickerNumber))
		{
			return STResult.SUCCESS;
		}
		else
		{
			return STResult.DOWNLOAD_FAILED;
		}
	}
	
	/**
	 * This function checks whether we should continue downloading stickers. 
	 * 
	 * @param reachedEnd
	 * 	-- true if we have downloaded all the stickers false if we have not
	 * @param totalNumber
	 * 	-- total number of stickers in category
	 * @param existingStickerNumber
	 * 	-- existing number of stickers in category 
	 * @return
	 * 	false if reached end = true Or totalNumber count is less than zero (in case of server error) Or existing sticker count is greater
	 * 	than or equal to total number
	 */
	private boolean shouldContinue(boolean reachedEnd, int totalNumber, int existingStickerNumber)
	{
		if(reachedEnd || totalNumber < 0 || (totalNumber > 0 && existingStickerNumber == totalNumber))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * This function checks whether call to download stickers is successful or not 
	 * 
	 * @param reachedEnd
	 * 	-- true if we have downloaded all the stickers false if we have not
	 * @param totalNumber
	 * 	-- total number of stickers in category
	 * @param existingStickerNumber
	 * 	-- existing number of stickers in category 
	 * @return
	 * 	true if reachedend = true Or totalNumber count is greater than zero and existing sticker count is greater
	 * 	than or equal to total number
	 */
	private boolean isSucessfull(boolean reachedEnd, int totalNumber, int existingStickerNumber)
	{
		
		if(reachedEnd || (totalNumber > 0 && existingStickerNumber >= totalNumber))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		sendProgressOrResult(result, false, 0);
		super.postExecute(result);
	}
	
	void sendProgressOrResult(STResult result, boolean isProgress, double percentage)
	{
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, source);
		if(isProgress)
		{
				b.putSerializable(StickerManager.PERCENTAGE, percentage);
				StickerManager.getInstance().onStickersDownloadProgress(b);
		}
		else
		{
			b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
			if (result != STResult.SUCCESS)
			{
				StickerException e = getException();
				if(e != null && e.getErrorCode() == StickerException.OUT_OF_SPACE)
				{
					b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE, true);
				}
				StickerManager.getInstance().stickersDownloadFailed(b);
			}
			else
			{
				StickerManager.getInstance().sucessFullyDownloadedStickers(b);
			}
		}
	}

	public int getStickerDownloadSize()
	{
		if(stickerDownloadSize !=0 )
		{
			return stickerDownloadSize;
		}
		else
		{
			NetworkType networkType = StickerDownloadManager.getInstance().getNetworkType();
			return networkType.getStickerDownloadSize();
			
		}
	}

	public void setStickerDownloadSize(int stickerDownloadSize)
	{
		this.stickerDownloadSize = stickerDownloadSize;
	}
}