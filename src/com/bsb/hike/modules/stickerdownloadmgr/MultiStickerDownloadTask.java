package com.bsb.hike.modules.stickerdownloadmgr;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.multiStickerDownloadRequest;

public class MultiStickerDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private String TAG = MultiStickerDownloadTask.class.getSimpleName();

	private StickerCategory category;

	private StickerConstants.DownloadType downloadType;

	private JSONObject bodyJson;

	private int existingStickerNumber = 0;

	File largeStickerDir, smallStickerDir;

	private String requestId;

	private RequestToken requestToken;

	public MultiStickerDownloadTask(StickerCategory category, StickerConstants.DownloadType downloadType, JSONObject bodyJson)
	{
		this.category = category;
		this.downloadType = downloadType;
		this.bodyJson = bodyJson;
	}

	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		download();
	}

	private void download()
	{
		requestToken = multiStickerDownloadRequest(getRequestId(), getRequestInterceptor(), getRequestListener());

		if (requestToken.isRequestRunning()) // duplicate check
		{
			return;
		}
		requestToken.execute();
	}

	private IRequestInterceptor getRequestInterceptor()
	{
		return new IRequestInterceptor()
		{

			@Override
			public void intercept(Chain chain) throws Exception
			{
				Logger.d(TAG, "intercept(), CategoryId: " + category.getCategoryId());
				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(category.getCategoryId());
				if (directoryPath == null)
				{
					Logger.e(TAG, "intercept(), Sticker download failed directory does not exist");
					doOnFailure(null);
					return;
				}

				JSONArray existingStickerIds = new JSONArray();

				largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
				smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);

				if (smallStickerDir.exists())
				{
					String[] stickerIds = smallStickerDir.list(StickerManager.getInstance().stickerFileFilter);
					if (stickerIds != null)
					{
						for (String stickerId : stickerIds)
						{
							existingStickerIds.put(stickerId);
							existingStickerNumber++;
							Logger.d(TAG, "intercept(), Existing id: " + stickerId);
						}
					}
				}
				else
				{
					smallStickerDir.mkdirs();
					Logger.d(TAG, "intercept(), No existing sticker.");
				}
				if (!largeStickerDir.exists())
					largeStickerDir.mkdirs();

				Utils.makeNoMediaFile(largeStickerDir);
				Utils.makeNoMediaFile(smallStickerDir);

				try
				{
					bodyJson.put(StickerManager.CATEGORY_ID, category.getCategoryId());
					bodyJson.put(HikeConstants.STICKER_IDS, existingStickerIds);
					bodyJson.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
					bodyJson.put(HikeConstants.NUMBER_OF_STICKERS, getStickerDownloadSize());
					bodyJson.put(HikeConstants.KEYBOARD_LIST, new JSONArray(StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)));

					Logger.d(TAG, "intercept(), Sticker Download Task Request: " + bodyJson.toString());

					IRequestBody body = new JsonBody(bodyJson);
					chain.getRequestFacade().setBody(body);
					chain.proceed();
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "intercept(), Json exception during creation of request body", e);
					doOnFailure(new HttpException("json exception", e));
					return;
				}
			}
		};
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
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
						doOnFailure(null);
						return;
					}
					Logger.d(TAG, "Got response for download task " + response.toString());
					JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

					if (null == data)
					{
						Logger.e(TAG, "Sticker download failed null data");
						doOnFailure(null);
						return;
					}

					if (!data.has(HikeConstants.PACKS))
					{
						Logger.e(TAG, "Sticker download failed null pack data");
						doOnFailure(null);
						return;
					}

					JSONObject packs = data.getJSONObject(HikeConstants.PACKS);
					String categoryId = packs.keys().next();

					if (!packs.has(categoryId))
					{
						Logger.e(TAG, "Sticker download failed null category data");
						doOnFailure(null);
						return;
					}

					JSONObject categoryData = packs.getJSONObject(categoryId);

					totalNumber = categoryData.optInt(HikeConstants.TOTAL_STICKERS, -1);
					reachedEnd = categoryData.optBoolean(HikeConstants.REACHED_STICKER_END);
					Logger.d(TAG, "Reached end? " + reachedEnd);
					Logger.d(TAG, "Sticker count: " + totalNumber);



					if (categoryData.has(HikeConstants.STICKERS))
					{
						JSONObject stickers = categoryData.getJSONObject(HikeConstants.STICKERS);
						Set<Sticker> stickerSet = new HashSet<>();

						for (Iterator<String> keys = stickers.keys(); keys.hasNext();)
						{
							String stickerId = keys.next();
                            Sticker sticker = new Sticker(categoryId,stickerId);
							JSONObject stickerData = stickers.getJSONObject(stickerId);
							String stickerImage = stickerData.getString(HikeConstants.IMAGE);

							existingStickerNumber++;

							try
							{
								byte[] byteArray = StickerManager.getInstance().saveLargeStickers(largeStickerDir.getAbsolutePath(), stickerId, stickerImage);
								StickerManager.getInstance().saveSmallStickers(smallStickerDir.getAbsolutePath(), stickerId, byteArray);
								StickerManager.getInstance().saveInStickerTagSet(sticker);
                                StickerManager.getInstance().saveInTableStickerSet(sticker);
								stickerSet.add(sticker);
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

                        StickerManager.getInstance().saveStickerSetFromJSON(stickers, categoryId);

                        StickerManager.getInstance().sendResponseTimeAnalytics(result, HikeConstants.STICKER_PACK, categoryId, null);

						StickerManager.getInstance().initiateMultiStickerQuickSuggestionDownloadTask(stickerSet);
					}

					StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(data);
					StickerSearchManager.getInstance().insertStickerTags(data, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT);

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
							doOnSuccess(null);
						}
						else
						{
							doOnFailure(null);
						}
					}
				}
				catch (Exception e)
				{
					doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
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
				doOnFailure(httpException);
			}
		};
	}

	private String getRequestId()
	{
		return (StickerRequestType.MULTIPLE.getLabel() + "\\" + category.getCategoryId() + "\\" + existingStickerNumber);
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
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, DownloadSource.fromValue(bodyJson.optInt(HikeConstants.DOWNLOAD_SOURCE)));
		b.putSerializable(StickerManager.PERCENTAGE, percentage);
		StickerManager.getInstance().onStickersDownloadProgress(b);
	}

	@Override
	public void doOnSuccess(Object result)
	{
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, DownloadSource.fromValue(bodyJson.optInt(HikeConstants.DOWNLOAD_SOURCE)));
		b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
		StickerManager.getInstance().sucessFullyDownloadedStickers(b);
	}

	@Override
	public void doOnFailure(HttpException e)
	{
		Logger.e(TAG, "on failure, exception ", e);
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.CATEGORY_ID, category.getCategoryId());
		b.putSerializable(HikeConstants.DOWNLOAD_SOURCE, DownloadSource.fromValue(bodyJson.optInt(HikeConstants.DOWNLOAD_SOURCE)));
		b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
		if (e != null && e instanceof HttpException && ((HttpException) e).getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
		{
			b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE, true);
		}
		StickerManager.getInstance().stickersDownloadFailed(b);
		StickerManager.getInstance().logStickerDownloadError(HikeConstants.STICKER_PACK);
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