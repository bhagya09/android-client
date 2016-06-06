package com.bsb.hike.modules.quickstickersuggestions;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadTips;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
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

    private static volatile QuickStickerSuggestionController _instance;

    private SparseArray<Boolean> ftueTipSeenArray;

    private long ttl;

    private int receiveFtueSessionCount;

    private int sentFtueSessionCount;

    private boolean ftueSessionRunning;

    private int sessionType;

    public static final int FTUE_RECEIVE_SESSION = 0;

    public static final int FTUE_SENT_SESSION = 1;

    private static final int DEFAULT_FTUE_SESSION_COUNT = 2;

    public static final long DEFAULT_QUICK_SUGGESTED_STICKERS_TTL = 2 * HikeConstants.ONE_DAY_MILLS;

    public static final long QUICK_SUGGESTION_TIP_VISIBLE_TIME = 2 * 1000; // 2 secs

    private static final int DEFAULT_MAX_FETCH_COUNT = 100;

    private static final int DEFAULT_MIN_SEEN_COUNT = 5;

    public static final int QUICK_SUGGESTION_FTUE_PAGE = 15;

    public static final int QUICK_SUGGESTION_STICKER_ANIMATION = 16;

    private boolean qsLoaded;

    private QuickStickerSuggestionController()
    {
        showQuickStickerSuggestionOnStickerReceive = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE, false);
        showQuickStickerSuggestionOnStickerSent = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT, false);
        ttl = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL, DEFAULT_QUICK_SUGGESTED_STICKERS_TTL);
        initFtueConditions();
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

    public void initFtueConditions()
    {
        receiveFtueSessionCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QS_RECEIVE_FTUE_SESSION_COUNT, DEFAULT_FTUE_SESSION_COUNT);
        sentFtueSessionCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QS_SENT_FTUE_SESSION_COUNT, DEFAULT_FTUE_SESSION_COUNT);
        ftueTipSeenArray = new SparseArray<>();
        ftueSessionRunning = false;
        sessionType = 0;
    }

    public void toggleQuickSuggestionOnReceive(boolean showQuickStickerSuggestionOnStickerReceive) {
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
        if(qsLoaded) {
            return;
        }

        FetchQuickStickerSuggestionTask fetchQuickStickerSuggestionTask = new FetchQuickStickerSuggestionTask(quickSuggestionCategory);
        HikeHandlerUtil.getInstance().postRunnable(fetchQuickStickerSuggestionTask);
        qsLoaded = true;
    }

    public void clearLoadedState()
    {
        qsLoaded = false;
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

    public void retryFailedQuickSuggestions()
    {
        Set<String> retrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.QUICK_SUGGESTION_RETRY_SET, new HashSet<String>());
        StickerManager.getInstance().initiateMultiStickerQuickSuggestionDownloadTask(StickerManager.getInstance().getStickerSetFromStickerStringSet(retrySet));
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

    public boolean shouldAnimateSticker(ConvMessage convMessage) {
        Sticker sticker = convMessage.getMetadata().getSticker();
        boolean isAllowed = isStickerClickAllowed(convMessage.isSent());
        return isAllowed && isFtueSessionRunning(convMessage.isSent()) && !isTipSeen(QUICK_SUGGESTION_STICKER_ANIMATION) && sticker.isStickerFileAvailable();
    }

    public void animateForQsFtue(ConvMessage convMessage, View view) {
        if(shouldAnimateSticker(convMessage))
        {
            view.startAnimation(HikeAnimationFactory.getQuickSuggestionStickerAnimation(view.getContext()));
        }
        else
        {
            if(view.getAnimation() != null)
            {
                view.clearAnimation();
            }
        }
    }

    public void sendFetchSuccessSignalToUi(List<StickerCategory> quickStickerCategoryList) {
        for (StickerCategory category : quickStickerCategoryList) {
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

    public boolean canStartFtue(boolean isSentSession)
    {
        if(!isStickerClickAllowed(isSentSession) || isFtueSessionRunning())
        {
            return false;
        }

        return isSentSession ? sentFtueSessionCount > 0 : receiveFtueSessionCount > 0;
    }

    public void startFtueSession(boolean isSentSession)
    {
        ftueSessionRunning = true;
        sessionType = isSentSession ? FTUE_SENT_SESSION : FTUE_RECEIVE_SESSION;
        ftueTipSeenArray.clear();
    }

    public int getFtueSessionType()
    {
        return sessionType;
    }

    public boolean isFtueSessionRunning()
    {
        return ftueSessionRunning;
    }

    public boolean isFtueSessionRunning(boolean isSent)
    {
        return isFtueSessionRunning() && (isSent ? sessionType == FTUE_SENT_SESSION : sessionType == FTUE_RECEIVE_SESSION);
    }

    public void completeFtueSession()
    {
        if(isFtueSessionCompleted())
        {
            if(sessionType == FTUE_RECEIVE_SESSION)
            {
                receiveFtueSessionCount --;
            }
            else
            {
                sentFtueSessionCount --;
            }
        }
        ftueSessionRunning = false;
        ftueTipSeenArray.clear();
    }

    public boolean isFtueSessionCompleted() {
        return isFtueSessionRunning() && isAllTipsSeen();
    }

    private boolean isAllTipsSeen() {
        if (sessionType == FTUE_RECEIVE_SESSION) {
            return isTipSeen(ChatThreadTips.QUICK_SUGGESTION_RECEIVED_FIRST_TIP) && isTipSeen(ChatThreadTips.QUICK_SUGGESTION_RECEIVED_SECOND_TIP) && isTipSeen(ChatThreadTips.QUICK_SUGGESTION_RECEIVED_THIRD_TIP);
        } else {
            return isTipSeen(ChatThreadTips.QUICK_SUGGESTION_SENT_FIRST_TIP) && isTipSeen(ChatThreadTips.QUICK_SUGGESTION_SENT_SECOND_TIP) && isTipSeen(ChatThreadTips.QUICK_SUGGESTION_SENT_THIRD_TIP);
        }
    }

    public String getTiptext(int whichTip)
    {
        Resources res = HikeMessengerApp.getInstance().getResources();
        switch (whichTip)
        {
            case ChatThreadTips.QUICK_SUGGESTION_RECEIVED_FIRST_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_RECEIVED_FIRST_TIP_TEXT, res.getString(R.string.qs_received_first_tip_text));
            case ChatThreadTips.QUICK_SUGGESTION_RECEIVED_SECOND_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_RECEIVED_SECOND_TIP_TEXT, res.getString(R.string.qs_received_second_tip_text));
            case ChatThreadTips.QUICK_SUGGESTION_RECEIVED_THIRD_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_RECEIVED_THIRD_TIP_TEXT, res.getString(R.string.qs_received_third_tip_text));
            case ChatThreadTips.QUICK_SUGGESTION_SENT_FIRST_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_SENT_FIRST_TIP_TEXT, res.getString(R.string.qs_sent_first_tip_text));
            case ChatThreadTips.QUICK_SUGGESTION_SENT_SECOND_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_SENT_SECOND_TIP_TEXT, res.getString(R.string.qs_sent_second_tip_text));
            case ChatThreadTips.QUICK_SUGGESTION_SENT_THIRD_TIP:
                return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.QUICK_SUGGESTION_SENT_THIRD_TIP_TEXT, res.getString(R.string.qs_sent_third_tip_text));
        }
        return "";
    }

    public void setFtueTipSeen(int whichTip)
    {
        ftueTipSeenArray.put(whichTip, true);
    }

    public boolean isTipSeen(int whichTip)
    {
        return ftueTipSeenArray.get(whichTip) == null ? false : ftueTipSeenArray.get(whichTip);
    }

    public boolean shouldFetchQuickSuggestions()
    {
        int maxFetchCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MAX_FETCH_COUNT, DEFAULT_MAX_FETCH_COUNT);
        maxFetchCount = maxFetchCount - 1;
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MAX_FETCH_COUNT, maxFetchCount);
        return maxFetchCount >= 0;
    }

    public void seenQuickSuggestions()
    {
        int seenCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MIN_SEEN_COUNT, 0);
        seenCount = seenCount + 1;
        if(seenCount >= DEFAULT_MIN_SEEN_COUNT)
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MIN_SEEN_COUNT, 0);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MAX_FETCH_COUNT, DEFAULT_MAX_FETCH_COUNT);
        }
        else
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MIN_SEEN_COUNT, seenCount);
        }
    }
}
