package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.PriorityConstants;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import java.util.List;

public class StickerShopDownload implements Runnable
{
	private int limit;

	private final String TAG = "StickerShopDownload";

	public StickerShopDownload(int limit)
	{
		this.limit = limit;
	}

	@Override
	public void run()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			Exception e = new HttpException(REASON_CODE_OUT_OF_SPACE);
			Logger.e(TAG, "on failure, exception ", e);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, e);
			return;
		}
		List<StickerCategory> list = HikeConversationsDatabase.getInstance().getStickerCatToBeSendForShopMetaData(limit);
		if (list.size() > 0)
		{
			StickerManager.getInstance().fetchCategoryMetadataTask(list, Request.REQUEST_TYPE_SHORT, PriorityConstants.PRIORITY_HIGH, true);
		}
		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, limit);
		}
	}
}