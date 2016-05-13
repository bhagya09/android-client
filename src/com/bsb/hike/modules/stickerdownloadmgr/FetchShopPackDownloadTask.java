package com.bsb.hike.modules.stickerdownloadmgr;

import android.database.Cursor;
import android.os.AsyncTask;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.PriorityConstants;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import java.util.List;

public class FetchShopPackDownloadTask extends AsyncTask<Void, Void, Void>
{
	private int limit;

	private final String TAG = "FetchShopPackDownloadTask";

	public FetchShopPackDownloadTask(int limit)
	{
		this.limit = limit;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			Exception e = new HttpException(REASON_CODE_OUT_OF_SPACE);
			Logger.e(TAG, "on failure, exception ", e);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, e);
			return null;
		}
		Cursor cursor = null;
		try
		{
			cursor = HikeConversationsDatabase.getInstance().getCursorForShopMetaDataUpdate(limit);
			List<StickerCategory> list = HikeConversationsDatabase.getInstance().getCategoriesForShopMetadataUpdate(cursor);
			if (list == null)
			{
				StickerManager.getInstance().initiateFetchCategoryRanksAndDataTask(0, true, true);
			}
			else if (list.size() != 0)
			{
				StickerManager.getInstance().fetchCategoryMetadataTask(list, Request.REQUEST_TYPE_SHORT, PriorityConstants.PRIORITY_HIGH, true);
			}
			else
			{
				if ((cursor.getCount() + StickerManager.SHOP_FETCH_PACK_COUNT - limit) > 0)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, null);
				}
				if (!HikeSharedPreferenceUtil.getInstance().getData(StickerManager.STICKER_SHOP_RANK_FULLY_FETCHED, false) && ((limit - cursor.getCount()) > 0))
				{
					StickerManager.getInstance().initiateFetchCategoryRanksAndDataTask(cursor.getCount(), true, true);
				}
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
		return null;
	}
}