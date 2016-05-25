package com.bsb.hike.modules.quickstickersuggestions.tasks;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
import com.bsb.hike.utils.StickerManager;

/**
 * Created by anubhavgupta on 09/05/16.
 */
public class FetchQuickStickerSuggestionTask implements Runnable
{
    private QuickSuggestionStickerCategory quickSuggestionCategory;

    public FetchQuickStickerSuggestionTask(QuickSuggestionStickerCategory quickSuggestionCategory)
    {
        this.quickSuggestionCategory = quickSuggestionCategory;
    }

    @Override
    public void run()
    {
        quickSuggestionCategory = HikeConversationsDatabase.getInstance().getQuickStickerSuggestionsForSticker(quickSuggestionCategory);
        QuickStickerSuggestionController.getInstance().checkIfNeedsRefresh(quickSuggestionCategory);
        LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(new Intent(StickerManager.QUICK_STICKER_SUGGESTION_FETCHED).putExtra(HikeConstants.BUNDLE, quickSuggestionCategory.toBundle()));
    }
}
