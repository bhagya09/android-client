package com.bsb.hike.modules.stickersearch;

import android.content.Intent;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatthread.ChatThreadTips;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.listeners.IStickerSearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.tasks.CurrentLanguageTagsDownloadTask;
import com.bsb.hike.modules.stickersearch.tasks.HighlightAndShowStickerPopupTask;
import com.bsb.hike.modules.stickersearch.tasks.InitiateStickerTagDownloadTask;
import com.bsb.hike.modules.stickersearch.tasks.InputMethodChangedTask;
import com.bsb.hike.modules.stickersearch.tasks.NewMessageReceivedTask;
import com.bsb.hike.modules.stickersearch.tasks.NewMessageSentTask;
import com.bsb.hike.modules.stickersearch.tasks.RebalancingTask;
import com.bsb.hike.modules.stickersearch.tasks.RemoveDeletedStickerTagsTask;
import com.bsb.hike.modules.stickersearch.tasks.SingleCharacterHighlightTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchSetupTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerTagInsertTask;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class StickerSearchManager
{
	private static int WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION;

	private static volatile StickerSearchManager _instance;

	private IStickerSearchListener listener;

	private StickerSearchEngine searchEngine;

	private volatile String currentString;

	private volatile int currentLength;

	private boolean isFirstPhraseOrWord;

	private boolean isTappedOnHighLightedWord;

	private int numStickersVisibleAtOneTime;

	private boolean showAutoPopupSettingOn;

	private boolean autoPopupTurningOffTrailRunning;

	private int rejectionCount;

	private int rejectionPatternCount;

	private int rejectionCountPerTrial;

	private int trialCountForAutoPopupTurnOff;

	private String keyboardLanguageISOCode;

	private HashMap<String, PairModified<Integer, Integer>> autoPopupClicksPerLanguageMap;

	private HashMap<String, PairModified<Integer, Integer>> tapOnHighlightWordClicksPerLanguageMap;

	private StickerSearchManager()
	{
		WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA).getData(
				HikeConstants.STICKER_WAIT_TIME_SINGLE_CHAR_RECOMMENDATION, StickerSearchConstants.WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION);

		searchEngine = new StickerSearchEngine();
		isFirstPhraseOrWord = false;
		isTappedOnHighLightedWord = false;
		currentString = null;
		currentLength = 0;

		// Order of calling following method must not be changed
		setShowAutoPopupConfiguration();
		setAndResumeRecommendationStateForAnalytics();
		setNumStickersVisibleAtOneTime(StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance()));
		setRebalancingAlarmFirstTime();
	}

	public static StickerSearchManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (StickerSearchManager.class)
			{
				if (_instance == null)
				{
					_instance = new StickerSearchManager();
				}
			}
		}

		return _instance;
	}

	public void addStickerSearchListener(IStickerSearchListener listner)
	{
		this.listener = listner;
	}

	public void removeStickerSearchListener(IStickerSearchListener listener)
	{
		if (this.listener != listener)
		{
			Logger.wtf(StickerTagWatcher.TAG, "listener mismatch !!");
		}

		this.listener = null;
	}

	public void loadChatProfile(String msisdn, boolean isGroupChat, long lastMessageTimestamp, String currentKeyboardLanguageISOCode)
	{
		keyboardLanguageISOCode = currentKeyboardLanguageISOCode;
		StickerSearchHostManager.getInstance().loadChatProfile(msisdn, isGroupChat, lastMessageTimestamp, keyboardLanguageISOCode);

		setRecommendationStateForLanguage(currentKeyboardLanguageISOCode);

		/*
		 * LoadChatProfileTask loadChatProfileTask = new LoadChatProfileTask(msisdn, isGroupChat, lastMessageTimestamp); searchEngine.runOnSearchThread(loadChatProfileTask, 0);
		 */
	}

	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		currentString = s.toString();
		currentLength = s.length();

		StickerSearchTask textChangedTask = new StickerSearchTask(s, start, before, count);
		searchEngine.runOnSearchThread(textChangedTask, 0);
	}

	public void textChanged(CharSequence s, int start, int before, int count)
	{
		Logger.i(StickerTagWatcher.TAG, "calling to search and get stickers for string: " + s);

		boolean isLastShownPopupWasAutoSuggestion = isFromAutoRecommendation();

		isFirstPhraseOrWord = false;
		isTappedOnHighLightedWord = false;
		Pair<CharSequence, int[][]> result = StickerSearchHostManager.getInstance().onTextChange(s, start, before, count);

		HighlightAndShowStickerPopupTask highlightAndShowtask = new HighlightAndShowStickerPopupTask(result, isLastShownPopupWasAutoSuggestion);
		searchEngine.runOnUiThread(highlightAndShowtask, 0);
	}

	public void highlightSingleCharacterAndShowStickerPopup(String returnedString, int[][] highlightArray)
	{
		if (listener == null)
		{
			Logger.d(StickerTagWatcher.TAG, "highlightSingleCharacterAndShowStickerPopup(), Resource error, can't do anything ???");

			return;
		}

		if (returnedString.equals(this.currentString))
		{
			listener.highlightText(highlightArray[0][0], this.currentLength);
			onClickToShowRecommendedStickers(highlightArray[0][0], false);
		}
	}

	public void highlightAndShowStickerPopup(Pair<CharSequence, int[][]> result, boolean isLastShownPopupWasAutoSuggestion)
	{
		if (listener == null)
		{
			Logger.d(StickerTagWatcher.TAG, "highlightAndShowStickerPopup(), Resource error, can't do anything ???");

			return;
		}

		if (result == null)
		{
			Logger.e(StickerTagWatcher.TAG, "Unable to find recommendation result, currentTextLength = " + this.currentLength);

			/* No need to call checkToTakeActionOnAutoPopupTurnOff(), as this case is just an error handling */

			listener.dismissStickerSearchPopup();
			listener.dismissTip(ChatThreadTips.STICKER_RECOMMEND_TIP);

			if (this.currentLength > 0)
			{
				listener.unHighlightText(0, this.currentLength);
			}

			return;
		}

		CharSequence returnedString = result.first;
		int highlightArray[][] = result.second;

		if (Utils.isBlank(returnedString) || (highlightArray == null) || (highlightArray.length <= 0))
		{
			Logger.i(StickerTagWatcher.TAG, "No recommendation result, current text length = " + this.currentLength);

			if (StickerSearchManager.getInstance().isAutoPoupTrialRunning() && listener.isStickerRecommendationPopupShowing() && isLastShownPopupWasAutoSuggestion)
			{
				checkToTakeActionOnAutoPopupTurnOff();
			}
			listener.dismissStickerSearchPopup();
			listener.dismissTip(ChatThreadTips.STICKER_RECOMMEND_TIP);

			if (this.currentLength > 0)
			{
				listener.unHighlightText(0, this.currentLength);
			}

			return;
		}

		String s = returnedString.toString();

		if (!s.equals(this.currentString))
		{
			Logger.w(StickerTagWatcher.TAG, "highlightAndShowStickerPopup(), Rapid change in text.");

			if (StickerSearchManager.getInstance().isAutoPoupTrialRunning() && listener.isStickerRecommendationPopupShowing() && isLastShownPopupWasAutoSuggestion)
			{
				checkToTakeActionOnAutoPopupTurnOff();
			}
			listener.dismissStickerSearchPopup();
			listener.dismissTip(ChatThreadTips.STICKER_RECOMMEND_TIP);

			if (this.currentLength > 0)
			{
				listener.unHighlightText(0, this.currentLength);
			}

			return;
		}

		// Use local reference to avoid exceptions, if values might change while executing following instructions
		String currentTextString = this.currentString;
		int currentTextLength = currentTextString.length();

		if (highlightArray[0][0] >= currentTextLength)
		{
			Logger.w(StickerTagWatcher.TAG, "highlightAndShowStickerPopup(), Exceptional rapid change in text.");

			if (StickerSearchManager.getInstance().isAutoPoupTrialRunning() && listener.isStickerRecommendationPopupShowing() && isLastShownPopupWasAutoSuggestion)
			{
				checkToTakeActionOnAutoPopupTurnOff();
			}
			listener.dismissStickerSearchPopup();
			listener.dismissTip(ChatThreadTips.STICKER_RECOMMEND_TIP);

			if (currentTextLength > 0)
			{
				listener.unHighlightText(0, currentTextLength);
			}

			return;
		}

		// Only first word/ phrase is typed and searched successfully
		Logger.i(StickerTagWatcher.TAG, "First highlight pair: [" + highlightArray[0][0] + " - " + highlightArray[0][1] + "]");

		int firstTagHighlightLength = highlightArray[0][1] - highlightArray[0][0];
		String preString = currentTextString.substring(0, highlightArray[0][0]);
		String postString = ((highlightArray[0][1] + 1) > currentTextLength) ? StickerSearchConstants.STRING_EMPTY : currentTextString.substring(highlightArray[0][1] + 1);

		if ((firstTagHighlightLength > 0) && Utils.isBlank(preString) && Utils.isBlank(postString))
		{
			isFirstPhraseOrWord = true;

			if (firstTagHighlightLength == 1)
			{
				listener.dismissStickerSearchPopup();

				/* No need to call checkToTakeActionOnAutoPopupTurnOff(), as auto pop-up will be re-shown here */

				SingleCharacterHighlightTask singleCharacterHighlightTask = new SingleCharacterHighlightTask(s, highlightArray);
				searchEngine.runOnUiThread(singleCharacterHighlightTask, WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION);
			}
			else
			{
				listener.highlightText(highlightArray[0][0], currentTextLength);
				onClickToShowRecommendedStickers(highlightArray[0][0], false);
			}

			return;
		}
		// Update local reference to avoid exceptions, if values might change while executing following instructions
		else
		{
			currentTextString = this.currentString;
			currentTextLength = currentTextString.length();
		}

		// More than one word may be possibly found to be searched successfully
		if (StickerSearchManager.getInstance().isAutoPoupTrialRunning() && listener.isStickerRecommendationPopupShowing() && isLastShownPopupWasAutoSuggestion)
		{
			checkToTakeActionOnAutoPopupTurnOff();
		}
		listener.dismissStickerSearchPopup();

		if (highlightArray[0][0] > 0)
		{
			listener.unHighlightText(0, highlightArray[0][0]);
		}

		for (int i = 0, start, end; (i < highlightArray.length) && (highlightArray[i] != null); i++)
		{
			start = highlightArray[i][0];
			end = highlightArray[i][1];

			if ((end > start) && (end <= currentTextLength))
			{
				listener.highlightText(start, end);

				if (((i + 1) < highlightArray.length) && (highlightArray[i + 1] != null) && (highlightArray[i + 1][0] < currentTextLength) && (highlightArray[i + 1][0] > end))
				{
					listener.unHighlightText(end, highlightArray[i + 1][0]);
				}
				// Handle last possible phrase/ word low-lighting
				else if (end < currentTextLength)
				{
					listener.unHighlightText(end, currentTextLength);
				}
			}
			// Handle last possible partial phrase/ word highlighting
			else
			{
				if (start < currentTextLength)
				{
					listener.highlightText(start, currentTextLength);
				}

				break;
			}
		}

		showStickerRecommendFtue(preString, highlightArray);
	}

	private void showStickerRecommendFtue(String preString, int[][] highlightArray)
	{
		if ((highlightArray.length > 1) || ((highlightArray.length > 0) && !Utils.isBlank(preString)))
		{
			listener.showStickerRecommendFtueTip();
		}
	}

	public void onClickToShowRecommendedStickers(int clickPosition, boolean onTapOnHighlightWord)
	{
		Logger.i(StickerTagWatcher.TAG, "onClickToShowRecommendedStickers(" + clickPosition + "," + onTapOnHighlightWord + ")");

		// Do nothing, if it is not because of touch on highlighted word and auto pop-up setting is turned-off
		if (!onTapOnHighlightWord && !showAutoPopupSettingOn)
		{
			if (listener != null)
			{
				listener.dismissStickerSearchPopup();
			}
			return;
		}

		isTappedOnHighLightedWord = onTapOnHighlightWord;
		Pair<Pair<String, String>, ArrayList<Sticker>> results = StickerSearchHostManager.getInstance().onClickToShowRecommendedStickers(clickPosition);

		if (listener != null)
		{
			if ((results != null) && (results.second != null))
			{
				if (onTapOnHighlightWord)
				{
					listener.setTipSeen(ChatThreadTips.STICKER_RECOMMEND_TIP, true);
					listener.setTipSeen(ChatThreadTips.STICKER_RECOMMEND_AUTO_OFF_TIP, true);
				}

				listener.showStickerSearchPopup(results.first.first, results.first.second, results.second);
				increaseRecommendationStateForCurrentLanguage(onTapOnHighlightWord);
			}
			else
			{
				listener.dismissStickerSearchPopup();
			}
		}
	}

	public void downloadStickerTags(boolean firstTime, int state, Set<String> languagesSet)
	{
		InitiateStickerTagDownloadTask stickerTagDownloadTask = new InitiateStickerTagDownloadTask(firstTime, state, languagesSet);
		searchEngine.runOnQueryThread(stickerTagDownloadTask);
	}

	public void insertStickerTags(JSONObject json, int state)
	{
		StickerTagInsertTask stickerInsertTask = new StickerTagInsertTask(json, state);
		searchEngine.runOnQueryThread(stickerInsertTask);
	}

	public void initStickerSearchProviderSetupWizard()
	{
		StickerSearchSetupTask stickerSearchSetupTask = new StickerSearchSetupTask();
		searchEngine.runOnQueryThread(stickerSearchSetupTask);
	}

	public void removeDeletedStickerTags()
	{
		RemoveDeletedStickerTagsTask removeDeletedStickerTagsTask = new RemoveDeletedStickerTagsTask();
		searchEngine.runOnQueryThread(removeDeletedStickerTagsTask);
	}

	public void inputMethodChanged(String languageISOCode)
	{
		InputMethodChangedTask inputMethodChangedTask = new InputMethodChangedTask(languageISOCode);
		searchEngine.runOnQueryThread(inputMethodChangedTask);

		setRecommendationStateForLanguage(languageISOCode);
	}

	public void downloadTagsForCurrentLanguage()
	{
		CurrentLanguageTagsDownloadTask currentLanguageTagsDownloadTask = new CurrentLanguageTagsDownloadTask();
		searchEngine.runOnQueryThread(currentLanguageTagsDownloadTask);
	}

	public void sentMessage(String prevText, Sticker sticker, String nextText, String currentText)
	{
		NewMessageSentTask newMessageSentTask = new NewMessageSentTask(prevText, sticker, nextText, currentText);
		searchEngine.runOnQueryThread(newMessageSentTask);
	}

	public void receivedMessage(String prevText, Sticker sticker, String nextText)
	{
		NewMessageReceivedTask newMessageReceivedTask = new NewMessageReceivedTask(prevText, sticker, nextText);
		searchEngine.runOnQueryThread(newMessageReceivedTask);
	}

	public boolean getFirstContinuousMatchFound()
	{
		return this.isFirstPhraseOrWord;
	}

	public boolean isFromAutoRecommendation()
	{
		return (this.isFirstPhraseOrWord && !this.isTappedOnHighLightedWord);
	}

	public boolean isAutoPoupTrialRunning()
	{
		return this.autoPopupTurningOffTrailRunning;
	}

	public void setRebalancingAlarmFirstTime()
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SET_ALARM_FIRST_TIME, false))
		{
			Logger.d(HikeStickerSearchDatabase.TAG_REBALANCING, "Setting rebalancing alarm first time...");

			setRebalancingAlarm();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SET_ALARM_FIRST_TIME, true);
		}
	}

	public void setRebalancingAlarm()
	{
		long scheduleTime = Utils.getTimeInMillis(
				Calendar.getInstance(Locale.ENGLISH),
				HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA).getData(HikeConstants.STICKER_TAG_REBALANCING_TRIGGER_TIME_STAMP,
						null), StickerSearchConstants.DEFAULT_REBALANCING_TIME_HOUR, 0, 0, 0);

		if (scheduleTime < System.currentTimeMillis())
		{
			scheduleTime += 24 * 60 * 60 * 1000; // Next day at given time
		}

		HikeAlarmManager.setAlarmwithIntentPersistance(HikeMessengerApp.getInstance(), scheduleTime, HikeAlarmManager.REQUEST_CODE_STICKER_RECOMMENDATION_BALANCING, true,
				getRebalancingAlarmIntent(), true);
	}

	private Intent getRebalancingAlarmIntent()
	{
		Intent intent = new Intent();
		intent.putExtra(HikeAlarmManager.INTENT_EXTRA_DELETE_FROM_DATABASE, false);
		return intent;
	}

	public void startRebalancing(Intent intent)
	{
		RebalancingTask rebalancingTask = new RebalancingTask(intent);
		searchEngine.runOnQueryThread(rebalancingTask);
	}

	public int getNumStickersVisibleAtOneTime()
	{
		return numStickersVisibleAtOneTime;
	}

	private void setNumStickersVisibleAtOneTime(int numStickersVisibleAtOneTime)
	{
		this.numStickersVisibleAtOneTime = numStickersVisibleAtOneTime;
	}

	private void setShowAutoPopupConfiguration()
	{
		this.showAutoPopupSettingOn = StickerSearchUtility.getStickerRecommendationSettingsValue(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, true);

		if (this.showAutoPopupSettingOn)
		{
			if ((HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF))
					&& (HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF)))
			{
				this.autoPopupTurningOffTrailRunning = true;
				this.rejectionCountPerTrial = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF,
						Integer.MAX_VALUE);
				this.trialCountForAutoPopupTurnOff = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF,
						Integer.MAX_VALUE);

				this.rejectionCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TILL_NOW, 0);
				this.rejectionPatternCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TILL_NOW, 0);
			}
			else
			{
				this.autoPopupTurningOffTrailRunning = false;
			}
		}
		else
		{
			this.autoPopupTurningOffTrailRunning = false;
		}
	}

	public void setShowAutoPopupSettingOn(boolean showAutopopupSettingOn)
	{
		this.showAutoPopupSettingOn = showAutopopupSettingOn;
	}

	public void setShowAutoPopupTurnOffPattern(int rejectCountPerTrial, int trailCount)
	{
		if (this.showAutoPopupSettingOn)
		{
			this.autoPopupTurningOffTrailRunning = true;
			this.rejectionCountPerTrial = rejectCountPerTrial;
			this.trialCountForAutoPopupTurnOff = trailCount;

			resetOrStartFreshTrialForAutoPopupTurnOff(true);
		}
		else
		{
			// User had already turned off auto pop-up setting, no need to start auto pop-up trial
			saveOrDeleteAutoPopupTrialState(true);
		}
	}

	public void resetOrStartFreshTrialForAutoPopupTurnOff(boolean isFirstTrialStarting)
	{
		this.rejectionCount = 0;
		if (isFirstTrialStarting)
		{
			this.rejectionPatternCount = 0;
		}
	}

	public void checkToTakeActionOnAutoPopupTurnOff()
	{
		this.rejectionCount++;

		if (this.rejectionCount >= this.rejectionCountPerTrial)
		{
			this.rejectionPatternCount++;

			// Turn off auto pop-up
			if (this.rejectionPatternCount >= this.trialCountForAutoPopupTurnOff)
			{
				this.autoPopupTurningOffTrailRunning = false;
				setShowAutoPopupSettingOn(false);

				StickerSearchUtility.saveStickerRecommendationSettingsValue(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, false);
				saveOrDeleteAutoPopupTrialState(true);
				
				if(listener != null)
				{
					listener.showStickerRecommendAutoPopupOffTip();
				}

				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_AUTO_RECOMMEND_SETTING_OFF_TIP, true);
			}
			// Reset count and start next trial
			else
			{
				resetOrStartFreshTrialForAutoPopupTurnOff(false);
			}
		}

		Logger.d(StickerTagWatcher.TAG, "checkToTakeActionOnAutoPopupTurnOff(), r = " + this.rejectionCount + ", p = " + this.rejectionPatternCount);
	}

	public void saveOrDeleteAutoPopupTrialState(boolean isClearing)
	{
		if (isClearing)
		{
			// If auto-suggestion setting is turned on/ off by user or server packet, stop running rejection trial
			this.autoPopupTurningOffTrailRunning = false;

			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TILL_NOW);
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TILL_NOW);

			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF);
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF);
		}
		else
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TILL_NOW, this.rejectionCount);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TILL_NOW, this.rejectionPatternCount);
		}
	}

	private void setAndResumeRecommendationStateForAnalytics()
	{
		this.autoPopupClicksPerLanguageMap = new HashMap<String, PairModified<Integer, Integer>>();
		this.tapOnHighlightWordClicksPerLanguageMap = new HashMap<String, PairModified<Integer, Integer>>();

		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);
		Set<String> languages = stickerDataSharedPref.getDataSet(StickerSearchConstants.KEY_PREF_STICKER_RECOOMENDATION_LANGUAGE_LIST, null);
		if (!Utils.isEmpty(languages))
		{
			PairModified<Integer, Integer> totalAndAcceptedRecommendationCountPairPerLanguage;
			int totalRecommendationCount;
			int acceptedClicks;

			for (String languageISOCode : languages)
			{
				// Fetch previous auto-popup data for each language
				totalRecommendationCount = stickerDataSharedPref.getData(
						StickerSearchUtility.getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_TOTAL_COUNT_PER_LANGUAGE, languageISOCode), 0);
				if (totalRecommendationCount > 0)
				{
					acceptedClicks = stickerDataSharedPref.getData(
							StickerSearchUtility.getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode), 0);
					totalAndAcceptedRecommendationCountPairPerLanguage = new PairModified<Integer, Integer>(totalRecommendationCount, acceptedClicks);

					this.autoPopupClicksPerLanguageMap.put(languageISOCode, totalAndAcceptedRecommendationCountPairPerLanguage);
				}

				// Fetch previous highlight word tapping data for each language
				totalRecommendationCount = stickerDataSharedPref
						.getData(StickerSearchUtility.getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_TOTAL_COUNT_PER_LANGUAGE,
								languageISOCode), 0);
				if (totalRecommendationCount > 0)
				{
					acceptedClicks = stickerDataSharedPref.getData(StickerSearchUtility.getSharedPrefKeyForRecommendationData(
							StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode), 0);
					totalAndAcceptedRecommendationCountPairPerLanguage = new PairModified<Integer, Integer>(totalRecommendationCount, acceptedClicks);

					this.tapOnHighlightWordClicksPerLanguageMap.put(languageISOCode, totalAndAcceptedRecommendationCountPairPerLanguage);
				}
			}
		}
	}

	private void resetRecommendationStateForAnalytics()
	{
		this.autoPopupClicksPerLanguageMap.clear();
		this.tapOnHighlightWordClicksPerLanguageMap.clear();
	}

	private void setRecommendationStateForLanguage(String languageISOCode)
	{
		// Initialize auto-popup accuracy collection for current language when auto-pop setting is on
		if (this.showAutoPopupSettingOn && !this.autoPopupClicksPerLanguageMap.containsKey(languageISOCode))
		{
			this.autoPopupClicksPerLanguageMap.put(languageISOCode, new PairModified<Integer, Integer>(0, 0));
		}

		// Initialize tap on highlight word accuracy collection for current language irrespective of auto-pop setting state
		if (!this.tapOnHighlightWordClicksPerLanguageMap.containsKey(languageISOCode))
		{
			this.tapOnHighlightWordClicksPerLanguageMap.put(languageISOCode, new PairModified<Integer, Integer>(0, 0));
		}
	}

	private void increaseRecommendationStateForCurrentLanguage(boolean onTapOnHighlightWord)
	{
		PairModified<Integer, Integer> accuracyMetrices;
		if (onTapOnHighlightWord)
		{
			if (this.tapOnHighlightWordClicksPerLanguageMap.containsKey(this.keyboardLanguageISOCode))
			{
				accuracyMetrices = this.tapOnHighlightWordClicksPerLanguageMap.get(this.keyboardLanguageISOCode);
				accuracyMetrices.setFirst(accuracyMetrices.getFirst() + 1);
			}
		}
		else
		{
			if (this.autoPopupClicksPerLanguageMap.containsKey(this.keyboardLanguageISOCode))
			{
				accuracyMetrices = this.autoPopupClicksPerLanguageMap.get(this.keyboardLanguageISOCode);
				accuracyMetrices.setFirst(accuracyMetrices.getFirst() + 1);
			}
		}
	}

	public void sendStickerRecommendationAnalytics()
	{
		if (StickerManager.getInstance()
				.sendRecommendationAccuracyAnalytics(new Date().toString(), this.autoPopupClicksPerLanguageMap, this.tapOnHighlightWordClicksPerLanguageMap))
		{
			// Reset all values after sending analytics data logged till now
			StickerSearchUtility.clearStickerRecommendationAnalyticsDataFromPref();
			resetRecommendationStateForAnalytics();
		}
	}

	public void saveCurrentRecommendationStateForAnalyticsIntoPref()
	{
		StickerSearchUtility.saveStickerRecommendationAnalyticsDataIntoPref(this.autoPopupClicksPerLanguageMap, this.tapOnHighlightWordClicksPerLanguageMap);

		// Dereference data, which is no longer needed
		this.autoPopupClicksPerLanguageMap.clear();
		this.autoPopupClicksPerLanguageMap = null;
		this.tapOnHighlightWordClicksPerLanguageMap.clear();
		this.tapOnHighlightWordClicksPerLanguageMap = null;
	}

	public void shutdown()
	{
		searchEngine.shutDown();

		searchEngine = null;
		_instance = null;
	}
}