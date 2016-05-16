package com.bsb.hike.modules.quickstickersuggestions.tasks;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

import java.util.Set;

/**
 * Created by anubhavgupta on 13/05/16.
 */
public class FetchForAllStickerQuickSuggestionTask implements Runnable
{

	@Override
	public void run()
	{
        Set<Sticker> stickerSet = HikeConversationsDatabase.getInstance().getAllStickers();
        StickerManager.getInstance().initiateMultiStickerQuickSuggestionDownloadTask(stickerSet);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FETCHED_QUICK_SUGGESTION_FIRST_TIME, true);
	}
}
