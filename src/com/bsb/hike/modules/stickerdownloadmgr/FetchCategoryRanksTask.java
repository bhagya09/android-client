package com.bsb.hike.modules.stickerdownloadmgr;

import android.os.AsyncTask;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.fragments.StickerShopBaseFragment;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.ThreadPoolExecutor;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.getPrefOrderForCategories;

/**
 * Created by ashishagarwal on 15/04/16.
 */
public class FetchCategoryRanksTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static final String TAG = FetchCategoryRanksTask.class.getSimpleName();

	private RequestToken token;

	private int offset;

	private boolean toPublish;

	public FetchCategoryRanksTask(int offset, boolean toPublish)
	{
		this.offset = offset;
		this.toPublish = toPublish;
	}

	private IRequestListener getRequestListener()
	{

		return new IRequestListener()
		{
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure(httpException);
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();

					Logger.d(TAG, response.toString());

					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker Order download failed null response");
						doOnFailure(null);
						return;
					}

					JSONObject resultData = response.optJSONObject(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(TAG, "Sticker Order download failed null data");
						doOnFailure(null);
						return;
					}
					JSONArray orderArray = resultData.optJSONArray(HikeConstants.PACKS);
					HikeConversationsDatabase.getInstance().updateStickerCategoryRanks(orderArray, offset != 0);
					if (orderArray != null && orderArray.length() != 0)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKER_SHOP_DATA_FULLY_FETCHED, false);
						HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKER_SHOP_RANK_FULLY_FETCHED, false);
						if (toPublish)
						{
							StickerManager.getInstance().executeFetchShopPackTask(offset + StickerManager.SHOP_FETCH_PACK_COUNT);
						}
					}
					else
					{
						HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKER_SHOP_RANK_FULLY_FETCHED, true);
						if (toPublish)
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, null);
						}
					}
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES_METADATA, false);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES_TAGDATA, false);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ALREDAY_FETCHED_CATEGORIES_RANK_LIMIT,
							offset + HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NUMBER_OF_ROWS_FOR_ORDER, StickerConstants.DEFAULT_NUMBER_OF_ROWS_FOR_ORDER));
					if (offset == 0)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATE_SHOP_RANK_TIMESTAMP, System.currentTimeMillis());
					}
					doOnSuccess(null);
				}
				catch (Exception e)
				{
					Logger.e(TAG, "Exception", e);
					doOnFailure(new HttpException(e));
					return;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}
		};

	}

	private String getRequestId()
	{
		return StickerConstants.StickerRequestType.UPDATE_ORDER.getLabel();
	}

	@Override
	public void execute()
	{
		int alreadyFetchedCategoriesLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ALREDAY_FETCHED_CATEGORIES_RANK_LIMIT, 0); 
		int newOffset = alreadyFetchedCategoriesLimit > offset ? alreadyFetchedCategoriesLimit : offset;
		token = getPrefOrderForCategories(getRequestId(), getRequestListener(),
				HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NUMBER_OF_ROWS_FOR_ORDER, StickerConstants.DEFAULT_NUMBER_OF_ROWS_FOR_ORDER), newOffset);
		if (!token.isRequestRunning())
		{
			token.execute();
		}
	}

	@Override
	public void cancel()
	{
		if (null != token)
		{
			token.cancel();
		}
	}

	@Override
	public void doOnSuccess(Object result)
	{
		setAlarmForFetchOrder();
		StickerManager.getInstance().initiateFetchCategoryRanksAndDataTask();
	}

	private void setAlarmForFetchOrder()
	{
		Logger.d(TAG, "Cancelling old Alarm if any and Setting new Alarm");
		HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_FETCH_PACK_ORDER);
		HikeAlarmManager.setAlarmPersistance(HikeMessengerApp.getInstance().getApplicationContext(), Calendar.getInstance().getTimeInMillis() + HikeConstants.ONE_DAY_MILLS,
				HikeAlarmManager.REQUESTCODE_FETCH_PACK_ORDER, false, true);

	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		if (toPublish)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, exception);
		}
		Logger.e(TAG, "Exception", exception);
		setAlarmForFetchOrder();
	}
}
