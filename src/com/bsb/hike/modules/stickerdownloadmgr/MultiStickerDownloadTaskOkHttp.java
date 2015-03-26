package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.multiStickerDownloadRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class MultiStickerDownloadTaskOkHttp implements IHikeHTTPTask
{
	private String TAG = "MultiStickerDownloadTask";

	private StickerCategory category;

	private StickerConstants.DownloadType downloadType;

	private DownloadSource source;

	private int existingStickerNumber = 0;

	File largeStickerDir, smallStickerDir;

	private String requestId;

	private RequestToken requestToken;
	
	public MultiStickerDownloadTaskOkHttp(StickerCategory category, StickerConstants.DownloadType downloadType, DownloadSource source)
	{
		this.category = category;
		this.downloadType = downloadType;
		this.source = source;
	}

	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			onFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		requestId = getRequestId();
		download();
	}

	private void download()
	{
		requestToken = multiStickerDownloadRequest(requestId, new IRequestInterceptor()
		{

			@Override
			public void intercept(Chain chain)
			{
				Logger.d(TAG, "CategoryId: " + category.getCategoryId());
				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(category.getCategoryId());
				if (directoryPath == null)
				{
					Logger.e(TAG, "Sticker download failed directory does not exist");
					onFailure(null);
					return;
				}

				JSONArray existingStickerIds = new JSONArray();

				largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
				smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);

				if (smallStickerDir.exists())
				{
					String[] stickerIds = smallStickerDir.list(StickerManager.getInstance().stickerFileFilter);
					for (String stickerId : stickerIds)
					{
						existingStickerIds.put(stickerId);
						existingStickerNumber++;
						Logger.d(TAG, "Existing id: " + stickerId);
					}
				}
				else
				{
					smallStickerDir.mkdirs();
					Logger.d(TAG, "No existing sticker");
				}
				if (!largeStickerDir.exists())
					largeStickerDir.mkdirs();

				Utils.makeNoMediaFile(largeStickerDir);
				Utils.makeNoMediaFile(smallStickerDir);

				try
				{
					JSONObject request = new JSONObject();
					request.put(StickerManager.CATEGORY_ID, category.getCategoryId());
					request.put(HikeConstants.STICKER_IDS, existingStickerIds);
					request.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
					request.put(HikeConstants.NUMBER_OF_STICKERS, getStickerDownloadSize());
					if (source != null)
					{
						request.put(HikeConstants.DOWNLOAD_SOURCE, source.ordinal());
					}
					Logger.d(TAG, "Sticker Download Task Request : " + request.toString());

					IRequestBody body = new JsonBody(request);
					chain.getRequestFacade().setBody(body);
					chain.proceed();
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "Json exception during creation of request body", e);
					onFailure(e);
					return;
				}
			}
		},
				new IRequestListener()
				{

					@Override
					public void onRequestSuccess(Response result)
					{
						int totalNumber = 0;
						boolean reachedEnd = false;

						try
						{
							JSONObject response = (JSONObject) result.getBody().getContent();
							if (!Utils.isResponseValid(response))
							{
								Logger.e(TAG, "Sticker download failed null or invalid response");
								onFailure(null);
								return;
							}
							Logger.d(TAG, "Got response for download task " + response.toString());
							JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

							if (null == data)
							{
								Logger.e(TAG, "Sticker download failed null data");
								onFailure(null);
								return;
							}

							totalNumber = response.optInt(HikeConstants.TOTAL_STICKERS, -1);
							reachedEnd = response.optBoolean(HikeConstants.REACHED_STICKER_END);
							Logger.d(TAG, "Reached end? " + reachedEnd);
							Logger.d(TAG, "Sticker count: " + totalNumber);

							for (Iterator<String> keys = data.keys(); keys.hasNext();)
							{
								String stickerId = keys.next();
								String stickerData = data.getString(stickerId);
								existingStickerNumber++;

								try
								{
									byte[] byteArray = StickerManager.getInstance().saveLargeStickers(largeStickerDir.getAbsolutePath(), stickerId, stickerData);
									StickerManager.getInstance().saveSmallStickers(smallStickerDir.getAbsolutePath(), stickerId, byteArray);
								}
								catch (FileNotFoundException e)
								{
									Logger.w(TAG, e);
								}
								catch (IOException e)
								{
									Logger.w(TAG, e);
								}
							}

							if (totalNumber != 0)
							{
								onProgress(existingStickerNumber / totalNumber);
							}

							if (category.getTotalStickers() != totalNumber)
							{
								category.setTotalStickers(totalNumber);
								HikeConversationsDatabase.getInstance().updateStickerCountForStickerCategory(category.getCategoryId(), totalNumber);
							}

							if (shouldContinue(reachedEnd, totalNumber, existingStickerNumber))
							{
								download(); // recursive
								return;
							}
							else
							{
								if (isSucessfull(reachedEnd, totalNumber, existingStickerNumber))
								{
									onSuccess(null);
								}
								else
								{
									onFailure(null);
								}
							}
						}
						catch (Exception e)
						{
							onFailure(e);
							return;
						}

					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{

					}

					@Override
					public void onRequestFailure(HttpException httpException)
					{
						onFailure(httpException);
					}
				});

		if (requestToken.isRequestRunning()) // duplicate check
		{
			return;
		}
		requestToken.execute();
	}

	private String getRequestId()
	{
		return (StickerRequestType.MULTIPLE.getLabel() + "\\" + category.getCategoryId());
	}

	/**
	 * This function checks whether we should continue downloading stickers.
	 * 
	 * @param reachedEnd
	 *            -- true if we have downloaded all the stickers false if we have not
	 * @param totalNumber
	 *            -- total number of stickers in category
	 * @param existingStickerNumber
	 *            -- existing number of stickers in category
	 * @return false if reached end = true Or totalNumber count is less than zero (in case of server error) Or existing sticker count is greater than or equal to total number
	 */
	private boolean shouldContinue(boolean reachedEnd, int totalNumber, int existingStickerNumber)
	{
		if (reachedEnd || totalNumber < 0 || (totalNumber > 0 && existingStickerNumber == totalNumber))
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
	 *            -- true if we have downloaded all the stickers false if we have not
	 * @param totalNumber
	 *            -- total number of stickers in category
	 * @param existingStickerNumber
	 *            -- existing number of stickers in category
	 * @return true if reachedend = true Or totalNumber count is greater than zero and existing sticker count is greater than or equal to total number
	 */
	private boolean isSucessfull(boolean reachedEnd, int totalNumber, int existingStickerNumber)
	{

		if (reachedEnd || (totalNumber > 0 && existingStickerNumber >= totalNumber))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getStickerDownloadSize()
	{
		return 10;
	}

	private void onProgress(double percentage)
	{
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, source);
		b.putSerializable(StickerManager.PERCENTAGE, percentage);
		StickerManager.getInstance().onStickersDownloadProgress(b);
	}

	private void onSuccess(Object result)
	{
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, source);
		b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
		StickerManager.getInstance().sucessFullyDownloadedStickers(b);
	}

	private void onFailure(Exception e)
	{
		Logger.e(TAG, "on failure, exception ", e);
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, source);
		b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
		if (e != null && e instanceof HttpException && ((HttpException) e).getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
		{
			b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE, true);
		}
		StickerManager.getInstance().stickersDownloadFailed(b);
	}

	@Override
	public void cancel()
	{
		if (null != requestToken)
		{
			requestToken.cancel();
		}
	}
}