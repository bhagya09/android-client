package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.singleStickerDownloadRequest;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.singleStickerImageDownloadRequest;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.diskcache.request.Base64StringRequest;
import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.RequestConstants;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class SingleStickerDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static final String TAG = "SingleStickerDownloadTask";

	private String stickerId;

	private String categoryId;

	private String largeStickerPath;

	private String smallStickerPath;

	private RequestToken token;

	private ConvMessage convMessage;

	private boolean imageOnly;

	private boolean downloadMini;

	public SingleStickerDownloadTask(String stickerId, String categoryId, ConvMessage convMessage, boolean imageOnly)
	{
		this.stickerId = stickerId;
		this.categoryId = categoryId;
		this.convMessage = convMessage;
		this.imageOnly = imageOnly;
		this.downloadMini = false;
	}

	public SingleStickerDownloadTask(String stickerId, String categoryId, ConvMessage convMessage, boolean imageOnly, boolean downloadMini)
	{
		this.stickerId = stickerId;
		this.categoryId = categoryId;
		this.convMessage = convMessage;
		this.imageOnly = imageOnly;
		this.downloadMini = downloadMini;
	}

	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}

		if(!Utils.doesExternalDirExists())
		{
			Logger.e(TAG, "Sticker download failed external dir path is null");
			doOnFailure(null);
			return;
		}

		String requestId = getRequestId(); // for duplicate check

		if (imageOnly)
		{
			Bundle extras = new Bundle();
			extras.putString(HikeConstants.STICKER_ID, stickerId);
			extras.putString(HikeConstants.CATEGORY_ID, categoryId);
			extras.putLong(HikeConstants.MESSAGE_ID, convMessage != null ? convMessage.getMsgID() : -1);
			token = singleStickerImageDownloadRequest(requestId, stickerId, categoryId, downloadMini, getRequestListener(), extras);
		}
		else
		{
			token = singleStickerDownloadRequest(
					requestId,
					stickerId,
					categoryId,
					getRequestListener(),
					StickerLanguagesManager.getInstance().listToString(
							StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
									StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)));
		}


		if (token.isRequestRunning()) // return if request is running
		{
            Logger.i(TAG, categoryId+":"+stickerId+" : ignored");
			return;
		}

        Logger.i(TAG, categoryId+":"+stickerId+" : started");
		token.execute();
	}

	@Override
	public void cancel()
	{
		if (null != token)
		{
			token.cancel();
		}
	}

	private String getRequestId()
	{
		return (StickerRequestType.SINGLE.getLabel() + "\\" + categoryId + "\\" + stickerId);
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null or invalid response");
						doOnFailure(null);
						return;
					}

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

					if (!categoryData.has(HikeConstants.STICKERS))
					{
						Logger.e(TAG, "Sticker download failed null stkrs data");
						doOnFailure(null);
						return;
					}

					JSONObject stickers = categoryData.getJSONObject(HikeConstants.STICKERS);

					if (!stickers.has(stickerId))
					{
						Logger.e(TAG, "Sticker download failed null sticker data");
						doOnFailure(null);
						return;
					}

					Sticker sticker = new Sticker(categoryId, stickerId);

					JSONObject stickerData = stickers.getJSONObject(stickerId);

					String type = stickerData.optString(HikeConstants.STICKER_TYPE, StickerConstants.StickerType.LARGE.getValue());

					String stickerImage = stickerData.getString(HikeConstants.IMAGE);

					if (type.equals(StickerConstants.StickerType.MINI.getValue()))
					{

						if(saveMiniSticker(sticker, stickerImage))
						{
                            StickerManager.getInstance().saveMiniStickerSetFromJSON(stickers, categoryId);
							doOnSuccess(sticker);
						}
						else
						{
							doOnFailure(null);
						}
					}
					else
					{
						boolean failed = !saveFullSticker(stickerImage, stickerData);

                        if (!failed)
                        {
                            StickerManager.getInstance().saveStickerSetFromJSON(stickers, categoryId);

                            getStickerTags(data);

							boolean cdn = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SINGLE_STICKER_CDN, true);

                            StickerManager.getInstance().sendResponseTimeAnalytics(result, cdn ? HikeConstants.SINGLE_STICKER_CDN : HikeConstants.SINGLE_STICKER, categoryId, stickerId);

                            StickerManager.getInstance().checkAndRemoveUpdateFlag(categoryId);

                            doOnSuccess(sticker);
                        }

					}

				}
				catch (JSONException ex)
				{
					Logger.e(TAG, "Sticker download Json Exception", ex);
					doOnFailure(new HttpException("json exception", ex));
					return;
				}
				catch (IOException ex)
				{
					Logger.e(TAG, "Sticker download Io Exception", ex);
					doOnFailure(new HttpException("io exception", ex));
					return;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e(TAG, "Sticker download failed :", httpException);
				doOnFailure(httpException);
			}
		};
	}

	@Override
	public void doOnSuccess(Object result)
	{

		Sticker sticker = (Sticker)result;
		Logger.i(TAG, sticker.getStickerCode() + " : done");
        if (convMessage != null && !(TextUtils.isEmpty(categoryId)))
		{

			StickerManager.getInstance().checkAndRemoveUpdateFlag(sticker.getCategoryId());
			String oldCategoryId = convMessage.getMetadata().getSticker().getStickerId();
			if (!oldCategoryId.equals(sticker.getCategoryId()))
			{
				try
				{
					MessageMetadata newMetadata = convMessage.getMetadata();
					newMetadata.updateSticker(sticker.getCategoryId());
					HikeConversationsDatabase.getInstance().updateMessageMetadata(convMessage.getMsgID(), newMetadata);
				}
				catch (JSONException e)
				{
					Logger.wtf("MessagesAdapter", "Got new categoryId as " + result.toString() + " But failed to update the metadata for : " + convMessage.getMsgID());
				}

			}
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED, sticker);
        finish();
	}

	@Override
	public void doOnFailure(HttpException e)
	{
		StickerManager.getInstance().logStickerDownloadError(HikeConstants.SINGLE_STICKER);
        Logger.e(TAG, categoryId + ":" + stickerId + " : failed");
		if (largeStickerPath != null)
		{
            (new File(largeStickerPath)).delete();
		}

        finish();
	}

	private boolean saveMiniSticker(Sticker sticker, String stickerImage)
	{
		CacheRequest cacheRequest = new Base64StringRequest.Builder().setKey(sticker.getMiniStickerPath()).setString(stickerImage).build();
		return HikeMessengerApp.getDiskCache().put(cacheRequest);
	}

	private void getStickerTags(JSONObject data)
	{
		StickerManager.getInstance().saveInStickerTagSet(new Sticker(categoryId, stickerId));

		if (imageOnly)
		{
			SingleStickerTagDownloadTask singleStickerTagDownloadTask = new SingleStickerTagDownloadTask(stickerId, categoryId);
			singleStickerTagDownloadTask.execute();
		}
		else
		{
			StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(data);
			StickerSearchManager.getInstance().insertStickerTags(data, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT);
		}

	}

	private boolean saveFullSticker(String stickerImage, JSONObject stickerData) throws IOException
	{
		String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);

		if (dirPath == null)
		{
			Logger.e(TAG, "Sticker download failed directory does not exist");
			doOnFailure(null);
			return false;
		}

		largeStickerPath = dirPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
		smallStickerPath = dirPath + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;

		File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
		if (!largeDir.exists())
		{
			if (!largeDir.mkdirs())
			{
				Logger.e(TAG, "Sticker download failed directory not created");
				doOnFailure(null);
				return false;
			}
		}
		File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
		if (!smallDir.exists())
		{
			if (!smallDir.mkdirs())
			{
				Logger.e(TAG, "Sticker download failed directory not created");
				doOnFailure(null);
				return false;
			}
		}

		Utils.makeNoMediaFile(smallDir);
		Utils.makeNoMediaFile(largeDir);

		Utils.saveBase64StringToFile(new File(largeStickerPath), stickerImage);

		boolean isDisabled = stickerData.optBoolean(HikeConstants.DISABLED_ST);
		if (!isDisabled)
		{
			Bitmap thumbnail = HikeBitmapFactory.scaleDownBitmap(largeStickerPath, StickerManager.SIZE_IMAGE, StickerManager.SIZE_IMAGE, true, false);

			if (thumbnail != null)
			{
				File smallImage = new File(smallStickerPath);
				BitmapUtils.saveBitmapToFile(smallImage, thumbnail);

				thumbnail.recycle();
			}

		}

		return true;

	}

	private void finish()
	{
		HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getStickerCacheKey(new Sticker(categoryId, stickerId), StickerConstants.StickerType.LARGE));
		HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getStickerCacheKey(new Sticker(categoryId, stickerId), StickerConstants.StickerType.SMALL));
	}

}