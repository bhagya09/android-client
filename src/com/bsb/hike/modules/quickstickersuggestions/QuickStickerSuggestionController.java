package com.bsb.hike.modules.quickstickersuggestions;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.quickstickersuggestions.tasks.FetchQuickStickerSuggestionTask;
import com.bsb.hike.modules.quickstickersuggestions.tasks.InsertQuickSuggestionTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by anubhavgupta on 08/05/16.
 */
public class QuickStickerSuggestionController
{
    private boolean showQuickStickerSuggestionOnStickerReceive;

    private boolean showQuickStickerSuggestionOnStickerSent;

    private long suggestedStickerTtl;

    public static final long DEFAULT_QUICK_SUGGESTED_STICKERS_TTL = 2 * HikeConstants.ONE_DAY_MILLS;

    private static volatile QuickStickerSuggestionController _instance;

    private QuickStickerSuggestionController()
    {
        showQuickStickerSuggestionOnStickerReceive = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE, true);
        showQuickStickerSuggestionOnStickerSent = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT, false);
        suggestedStickerTtl = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL, DEFAULT_QUICK_SUGGESTED_STICKERS_TTL);
    }

    public static QuickStickerSuggestionController getInstance()
    {
        if(_instance == null)
        {
            synchronized (QuickStickerSuggestionController.class)
            {
                if(_instance == null)
                {
                    _instance = new QuickStickerSuggestionController();
                }
            }
        }

        return _instance;
    }

    public boolean isStickerClickAllowed(boolean isSent)
    {
        return isSent ? showQuickStickerSuggestionOnStickerSent : showQuickStickerSuggestionOnStickerReceive;
    }

    public void releaseResources()
    {
    }

    public void loadQuickStickerSuggestions(QuickSuggestionStickerCategory quickSuggestionCategory)
    {
        FetchQuickStickerSuggestionTask fetchQuickStickerSuggestionTask = new FetchQuickStickerSuggestionTask(quickSuggestionCategory);
        HikeHandlerUtil.getInstance().postRunnable(fetchQuickStickerSuggestionTask);
    }

    public StickerCategory getQuickSuggestionCategory(ConvMessage convMessage)
    {
        Sticker quickSuggestionSticker = convMessage.getMetadata().getSticker();
        boolean isSent = convMessage.isSent();

        StickerCategory quickSuggestionCategory = new QuickSuggestionStickerCategory.Builder()
                .setCategoryId(StickerManager.QUICK_SUGGESTIONS)
                .setQuickSuggestSticker(quickSuggestionSticker)
                .showReplyStickers(!isSent)
                .build();

        return quickSuggestionCategory;
    }

    public void insertQuickSuggestion(JSONObject quickSuggestionJson)
    {
        JSONArray quickSuggestionJsonArray = new JSONArray();
        quickSuggestionJsonArray.put(quickSuggestionJson);
        insertQuickSuggestion(quickSuggestionJsonArray);
    }

    public void insertQuickSuggestion(JSONArray quickSuggestionJsonArray)
    {
        InsertQuickSuggestionTask insertQuickSuggestionTask = new InsertQuickSuggestionTask(quickSuggestionJsonArray);
        HikeHandlerUtil.getInstance().postRunnable(insertQuickSuggestionTask);
    }

    public boolean isCategoryRefreshRequired(long lastCategoryRefreshTime)
    {
        return (System.currentTimeMillis() > lastCategoryRefreshTime + suggestedStickerTtl);
    }

    public void saveInRetrySet(Sticker sticker)
    {
        Set<String> retrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, new HashSet<String>());
        retrySet.add(sticker.getStickerCode());
        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, retrySet);

    }

    public void saveInRetrySet(Set<Sticker> stickerSet)
    {
        Set<String> retrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, new HashSet<String>());
        for(Sticker sticker : stickerSet)
        {
            retrySet.add(sticker.getStickerCode());
        }
        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, retrySet);
    }

    public void removeFromRetrySet(Sticker sticker)
    {
        Set<String> retrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, new HashSet<String>());
        retrySet.remove(sticker.getStickerCode());
        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, retrySet);
    }

    public void removeFromRetrySet(Set<Sticker> stickerSet)
    {
        Set<String> retrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, new HashSet<String>());
        for(Sticker sticker : stickerSet)
        {
            retrySet.remove(sticker.getStickerCode());
        }
        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, retrySet);
    }
}
