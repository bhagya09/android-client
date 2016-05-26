package com.bsb.hike.modules.quickstickersuggestions;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

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

import org.apache.http.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by anubhavgupta on 08/05/16.
 */
public class QuickStickerSuggestionController
{
    private boolean showQuickStickerSuggestionOnStickerReceive;

    private boolean showQuickStickerSuggestionOnStickerSent;

    private long ttl;

    public static final long DEFAULT_QUICK_SUGGESTED_STICKERS_TTL = 2 * HikeConstants.ONE_DAY_MILLS;

    private static volatile QuickStickerSuggestionController _instance;

    private QuickStickerSuggestionController()
    {
        showQuickStickerSuggestionOnStickerReceive = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE, false);
        showQuickStickerSuggestionOnStickerSent = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT, false);
        ttl = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL, DEFAULT_QUICK_SUGGESTED_STICKERS_TTL);
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

    public void toggleQuickSuggestionOnReceive(boolean showQuickStickerSuggestionOnStickerReceive)
    {
        this.showQuickStickerSuggestionOnStickerReceive = showQuickStickerSuggestionOnStickerReceive;
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE, showQuickStickerSuggestionOnStickerReceive);

        if(showQuickStickerSuggestionOnStickerReceive)
        {
            StickerManager.getInstance().fetchQuickSuggestionForAllStickers();
        }
    }

    public void toggleQuickSuggestionOnSent(boolean showQuickStickerSuggestionOnStickerSent)
    {
        this.showQuickStickerSuggestionOnStickerSent = showQuickStickerSuggestionOnStickerSent;
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT, showQuickStickerSuggestionOnStickerSent);

        if(showQuickStickerSuggestionOnStickerSent)
        {
            StickerManager.getInstance().fetchQuickSuggestionForAllStickers();
        }
    }

    public void refreshQuickSuggestionTtl(long ttl)
    {
        this.ttl = ttl;
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL, ttl);
    }

    public boolean isQuickSuggestionEnabled()
    {
        return showQuickStickerSuggestionOnStickerSent || showQuickStickerSuggestionOnStickerReceive;
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
        return (System.currentTimeMillis() > lastCategoryRefreshTime + ttl);
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

    public boolean needsRefresh(QuickSuggestionStickerCategory quickSuggestionStickerCategory)
    {
        if((System.currentTimeMillis() - quickSuggestionStickerCategory.getLastRefreshTime()) > ttl)
        {
            return true;
        }
        return false;
    }

    public int getSetIdForQuickSuggestions()
    {
        String uid = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.UID_SETTING, null);
        return Math.abs(TextUtils.isEmpty(uid) ? 0 : uid.hashCode() % 100) + 1;
    }


    public boolean shouldShowFtuePage()
    {
        return true;
    }

    public void sendFetchSuccessSignalToUi(List<StickerCategory> quickStickerCategoryList)
    {
        for(StickerCategory category : quickStickerCategoryList)
        {
            QuickSuggestionStickerCategory quickSuggestionCategory = (QuickSuggestionStickerCategory) category;
            LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(new Intent(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_SUCCESS).putExtra(HikeConstants.BUNDLE, quickSuggestionCategory.toBundle()));
        }
    }

    public void sendFetchFailedSignalToUi(Sticker sticker)
    {
        QuickSuggestionStickerCategory stickerCategory = new QuickSuggestionStickerCategory.Builder()
                .setCategoryId(StickerManager.QUICK_SUGGESTIONS)
                .setIsCustom(true)
                .setQuickSuggestSticker(sticker)
                .build();
        LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(new Intent(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_FAILED).putExtra(HikeConstants.BUNDLE, stickerCategory.toBundle()));
    }
}
