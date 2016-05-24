package com.bsb.hike.modules.stickerdownloadmgr;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.multiStickerImageDownloadRequest;

public class MultiStickerImageDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private String TAG = MultiStickerImageDownloadTask.class.getSimpleName();

	private StickerCategory category;

	private StickerConstants.DownloadType downloadType;

	private JSONObject bodyJson;

	private File largeStickerDir, smallStickerDir;

	private List<RequestToken> requestTokenList;

	private boolean isCancelled;

	private boolean isFailed;

	private int existingStickerNumber;

	private int totalStickers;

	private Set<String> stickerSet;

	public MultiStickerImageDownloadTask(StickerCategory category, StickerConstants.DownloadType downloadType, JSONObject bodyJson)
	{
		this.category = category;
		this.downloadType = downloadType;
		this.existingStickerNumber = 0;
		this.totalStickers = category.getTotalStickers();
		this.bodyJson = bodyJson;
		stickerSet = new HashSet<>();
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
		Logger.d(TAG, "pack request for category : " + category.getCategoryId() + " started at time : " + System.currentTimeMillis());
		int numCalls = totalStickers/getStickerDownloadSize();

		numCalls += ((totalStickers % getStickerDownloadSize() == 0) ? 0 : 1);

		int call = 0, offset = -1;
		requestTokenList = new ArrayList<>(numCalls);

		while(!isCancelled && (call < numCalls))
		{
			call ++;
			offset ++;
			RequestListener requestListener = new RequestListener();
			RequestToken requestToken = multiStickerImageDownloadRequest(getRequestId(offset), new RequestInterceptor(offset, requestListener), requestListener);
			requestListener.setRequestToken(requestToken);
			requestTokenList.add(requestToken);
			if (requestToken.isRequestRunning()) // duplicate check
			{
				continue;
			}
			requestToken.execute();
		}
	}

	private class RequestInterceptor implements IRequestInterceptor
	{
		private int offset;

		private RequestListener requestListener;

		public RequestInterceptor(int offset, RequestListener requestListener)
		{
			this.offset = offset;
			this.requestListener = requestListener;
		}

		@Override
		public void intercept(Chain chain) throws Exception
		{
			Logger.d(TAG, "intercept(), CategoryId: " + category.getCategoryId());
			String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(category.getCategoryId());
			if (directoryPath == null)
			{
				Logger.e(TAG, "intercept(), Sticker download failed directory does not exist");
				requestListener.onRequestFailure(null);
				return;
			}

			largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
			smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);

			if (!smallStickerDir.exists())
				smallStickerDir.mkdirs();
			if (!largeStickerDir.exists())
				largeStickerDir.mkdirs();

			Utils.makeNoMediaFile(largeStickerDir);
			Utils.makeNoMediaFile(smallStickerDir);

			try
			{
				bodyJson.put(StickerManager.CATEGORY_ID, category.getCategoryId());
				bodyJson.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
				bodyJson.put(HikeConstants.NUMBER_OF_STICKERS, getStickerDownloadSize());
				bodyJson.put(HikeConstants.OFFSET, offset);

				Logger.d(TAG, "intercept(), Sticker Download Task Request: " + bodyJson.toString());

				IRequestBody body = new JsonBody(bodyJson);
				chain.getRequestFacade().setBody(body);
				chain.proceed();
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "intercept(), Json exception during creation of request body", e);
				requestListener.onRequestFailure(new HttpException("json exception", e));
				return;
			}
		}
	}

	private class RequestListener implements IRequestListener
	{
		private RequestToken requestToken;

		public RequestListener()
		{
		}

		public void setRequestToken(RequestToken requestToken) {
			this.requestToken = requestToken;
		}

		@Override
		public void onRequestSuccess(Response result)
		{
			try
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				if (!Utils.isResponseValid(response))
				{
					Logger.e(TAG, "Sticker download failed null or invalid response");
					onRequestFailure(null);
					return;
				}
				Logger.d(TAG, "Got response for download task " + response.toString());
				JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

				if (null == data)
				{
					Logger.e(TAG, "Sticker download failed null data");
					onRequestFailure(null);
					return;
				}

				if (!data.has(HikeConstants.PACKS))
				{
					Logger.e(TAG, "Sticker download failed null pack data");
					onRequestFailure(null);
					return;
				}

				JSONObject packs = data.getJSONObject(HikeConstants.PACKS);
				String categoryId = packs.keys().next();

				if (!packs.has(categoryId))
				{
					Logger.e(TAG, "Sticker download failed null category data");
					onRequestFailure(null);
					return;
				}

				JSONObject categoryData = packs.getJSONObject(categoryId);

				if (categoryData.has(HikeConstants.STICKERS))
				{
					JSONObject stickers = categoryData.getJSONObject(HikeConstants.STICKERS);

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
							stickerSet.add(sticker.getStickerCode());
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

					StickerManager.getInstance().sendResponseTimeAnalytics(result, HikeConstants.STICKER_PACK_CDN, categoryId, null);
				}

				requestCompleted(requestToken, false);
			}
			catch (Exception e)
			{
				onRequestFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
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
			Logger.e(TAG, "on failure, exception ", httpException);
			requestCompleted(requestToken, true);
		}
	}

	private String getRequestId(int offset)
	{
		return (StickerRequestType.MULTIPLE.getLabel() + "\\" + category.getCategoryId() + "\\" + offset);
	}

	private void requestCompleted(RequestToken requestToken, boolean failed)
	{
		if(!Utils.isEmpty(requestTokenList))
		{
			requestTokenList.remove(requestToken);
		}
		if(failed)
		{
			isFailed = true; 							// in the end we have to report failure even if one request fails
		}

		check();										// check to continue or not
	}

	private void check()
	{
		if(Utils.isEmpty(requestTokenList))
		{
			Logger.d(TAG, "sticker set for get tags : " + stickerSet);
			StickerSearchManager.getInstance().downloadStickerTags(false, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT, stickerSet, StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE));
			StickerManager.getInstance().initiateMultiStickerQuickSuggestionDownloadTask(StickerManager.getInstance().getStickerSetFromStickerStringSet(stickerSet));

			if(isFailed)
			{
				doOnFailure(null);
			}
			else
			{
				doOnSuccess(null);
			}
			Logger.d(TAG, "pack request for category : " + category.getCategoryId() + " completed at time : " + System.currentTimeMillis() + " is failed : " + isFailed);
		}
		else
		{
			if (totalStickers != 0)
			{
				onProgress(existingStickerNumber / totalStickers);
			}
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
		if (e != null && e.getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
		{
			b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE, true);
		}
		StickerManager.getInstance().stickersDownloadFailed(b);
		StickerManager.getInstance().logStickerDownloadError(HikeConstants.STICKER_PACK);
	}

	@Override
	public void cancel()
	{
		isCancelled = true;
		if (!Utils.isEmpty(requestTokenList))
		{
			for(RequestToken requestToken : requestTokenList)
			{
				requestToken.cancel();
			}
		}
	}
}