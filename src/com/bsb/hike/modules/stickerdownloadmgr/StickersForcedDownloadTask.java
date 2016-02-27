package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by akhiltripathi on 11/01/16.
 */
public class StickersForcedDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private static final String TAG = StickersForcedDownloadTask.class.getSimpleName();

	private Set<String> languagesSet;

	private Set<String> forcedRecentsStickers;

	private Set<String> stickerToDownloadTagsSet;

	public StickersForcedDownloadTask(Set<String> languagesSet)
	{
		this.languagesSet = languagesSet;
		stickerToDownloadTagsSet = new HashSet<>();
	}

	private IRequestListener getResponseListener()
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
						Logger.e(TAG, "Forced Sticker download failed null or invalid response");
						doOnFailure(null);
						return;
					}
					Logger.d(TAG, "Got response for Forced download task " + response.toString());

					JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

					if (null == data)
					{
						Logger.e(TAG, "Sticker download failed null data");
						doOnFailure(null);
						return;
					}

					Iterator<String> categories = data.keys();

					while (categories.hasNext())
					{
						String category = categories.next();
						if (Utils.isBlank(category))
						{
							Logger.e(TAG, "onRequestSuccess(),Invalid category id.");
							continue;
						}

						JSONObject categoryData = data.optJSONObject(category);
						if ((categoryData == null) || (categoryData.length() <= 0))
						{
							Logger.e(TAG, "onRequestSuccess(), Empty json data for pack: " + category);
							continue;
						}

						Iterator<String> stickers = categoryData.keys();

						while (stickers.hasNext())
						{
							String stickerID = stickers.next();

							JSONObject stickersData = categoryData.optJSONObject(stickerID);

							if ((stickersData == null) || (stickersData.length() <= 0))
							{
								Logger.e(TAG, "onRequestSuccess(), Empty json sticker data for sticker: " + stickerID);
								continue;
							}

							JSONObject stickersMetaData = stickersData.optJSONObject("md");
							if ((stickersMetaData == null) || (stickersMetaData.length() <= 0))
							{
								Logger.e(TAG, "onRequestSuccess(), Empty json sticker metadata for pack: " + stickerID);
								continue;
							}

							Sticker sticker = new Sticker(category,stickerID);

							if (isValidForcedSticker(sticker))
							{
								if (stickersMetaData.has("image"))
								{
									switch (stickersMetaData.getInt("image"))
									{
										case 1:
											StickerManager.getInstance().initiateSingleStickerDownloadTask(stickerID, category, null);
											break;
									}
								}

								if (stickersMetaData.has("mini_image"))
								{
									switch (stickersMetaData.getInt("mini_image"))
									{
										case 1:
											StickerManager.getInstance().initiateMiniStickerDownloadTask(stickerID, category);
											break;
									}
								}

								if (stickersMetaData.has("tags"))
								{
									switch (stickersMetaData.getInt("tags"))
									{
										case 1:
											stickerToDownloadTagsSet.add(sticker.getStickerCode());
											break;
									}
								}
							}
							else
							{
								Logger.e(TAG, "Invalid(Already Present) forced sticker JSON" + sticker.getStickerCode());
							}


							if (stickersMetaData.has("recents"))
							{
								if (forcedRecentsStickers == null)
								{
									forcedRecentsStickers = new HashSet<String>();
								}

								JSONObject recentsSticker = stickersMetaData.getJSONObject("recents");
								recentsSticker.put("catId", category);
								recentsSticker.put("sId", stickerID);

								forcedRecentsStickers.add(recentsSticker.toString());
							}
						}
					}
				}
				catch (JSONException e)
				{
					Logger.w(TAG, "Exception in JSON" + e.getMessage());
					e.printStackTrace();
				}

				doOnSuccess(null);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e(StickerTagWatcher.TAG, "Request failed.");
				doOnFailure(httpException);
			}
		};
	}

	private String getRequestId()
	{
		return StickerConstants.StickerRequestType.FORCED.getLabel();
	}

	@Override
	public void execute()
	{
		try
		{
			JSONObject json = new JSONObject();

			if (Utils.isEmpty(languagesSet))
			{
				languagesSet.add(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE);
			}
			Logger.d(TAG, "language list for download : " + languagesSet);
			json.put(HikeConstants.KEYBOARD_LIST, new JSONArray(languagesSet));

			RequestToken requestToken = HttpRequests.getForcedDownloadListRequest(getRequestId(), getResponseListener(), json);

			if (requestToken.isRequestRunning())
			{
				return;
			}

			requestToken.execute();
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "json exception ", e);
		}

	}

	@Override
	public void cancel()
	{

	}

	@Override
	public void doOnSuccess(Object result)
	{

		StickerSearchManager.getInstance().downloadStickerTags(true, StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD, languagesSet, stickerToDownloadTagsSet);
		if (forcedRecentsStickers != null)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FORCED_RECENTS_PRESENT, true);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FORCED_RECENTS_LIST, forcedRecentsStickers);

			//ToDo Add correct PubSub for recent updated
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
		}
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.e(TAG, "Forced Download Failed ", exception);
	}

	private boolean isValidForcedSticker(Sticker sticker)
	{
		return !sticker.isStickerAvailable();
	}

}