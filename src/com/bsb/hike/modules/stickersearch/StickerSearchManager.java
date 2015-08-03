package com.bsb.hike.modules.stickersearch;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONObject;

import android.content.Intent;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.listeners.IStickerSearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.tasks.HighlightAndShowStickerPopupTask;
import com.bsb.hike.modules.stickersearch.tasks.InitiateStickerTagDownloadTask;
import com.bsb.hike.modules.stickersearch.tasks.LoadChatProfileTask;
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
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerSearchManager
{
	private static volatile StickerSearchManager _instance;

	private IStickerSearchListener listener;

	private StickerSearchEngine searchEngine;

	private volatile String currentString;

	private volatile int currentLength = 0;

	private boolean isFirstPhraseOrWord = false;

	private int numStickersVisibleAtOneTime;

	private boolean showAutopopupSettingOn;

	private StickerSearchManager()
	{
		searchEngine = new StickerSearchEngine();
		showAutopopupSettingOn = isShowAutopopupSettingOn();
		setNumStickersVisibleAtOneTime(StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance()));
		setAlarmFirstTime();
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

	public void loadChatProfile(String msidn, boolean isGroupChat, long lastMessageTimestamp)
	{
		LoadChatProfileTask loadChatProfileTask = new LoadChatProfileTask(msidn, isGroupChat, lastMessageTimestamp);
		searchEngine.runOnSearchThread(loadChatProfileTask, 0);
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

		isFirstPhraseOrWord = false;
		Pair<CharSequence, int[][]> result = StickerSearchHostManager.getInstance().onTextChange(s, start, before, count);

		HighlightAndShowStickerPopupTask highlightAndShowtask = new HighlightAndShowStickerPopupTask(result);
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
			onClickToSendSticker(highlightArray[0][0], false);
		}
	}

	public void highlightAndShowStickerPopup(Pair<CharSequence, int[][]> result)
	{
		if (listener == null)
		{
			Logger.d(StickerTagWatcher.TAG, "highlightAndShowStickerPopup(), Resource error, can't do anything ???");

			return;
		}

		if (result == null)
		{
			Logger.e(StickerTagWatcher.TAG, "Unable to find recommendation result, currentTextLength = " + this.currentLength);

			listener.dismissStickerSearchPopup();
			listener.dismissStickerRecommendFtueTip();

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

			listener.dismissStickerSearchPopup();
			listener.dismissStickerRecommendFtueTip();

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

			listener.dismissStickerSearchPopup();
			listener.dismissStickerRecommendFtueTip();

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

			listener.dismissStickerSearchPopup();
			listener.dismissStickerRecommendFtueTip();

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
			if (firstTagHighlightLength == 1)
			{
				listener.dismissStickerSearchPopup();

				SingleCharacterHighlightTask singleCharacterHighlightTask = new SingleCharacterHighlightTask(s, highlightArray);
				searchEngine.runOnUiThread(singleCharacterHighlightTask, StickerSearchConstants.WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION);
			}
			else
			{
				listener.highlightText(highlightArray[0][0], currentTextLength);
				onClickToSendSticker(highlightArray[0][0], false);
			}

			isFirstPhraseOrWord = true;
			return;
		}
		// Update local reference to avoid exceptions, if values might change while executing following instructions
		else
		{
			currentTextString = this.currentString;
			currentTextLength = currentTextString.length();
		}

		// More than one word may be possibly found to be searched
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

	public void onClickToSendSticker(int clickPosition, boolean onTouch)
	{
		Logger.i(StickerTagWatcher.TAG, "onClickToSendSticker(" + clickPosition + ")");

		if (!onTouch && !showAutopopupSettingOn)
		{
			// if its not because of touch and auto pop-up setting is off then return
			return;
		}

		Pair<Pair<String, String>, ArrayList<Sticker>> results = StickerSearchHostManager.getInstance().onClickToSendSticker(clickPosition);

		if (listener != null)
		{
			if ((results != null) && (results.second != null))
			{
				if (onTouch)
				{
					listener.setStickerRecommendFtueSeen();
				}

				listener.dismissStickerSearchPopup();
				listener.showStickerSearchPopup(results.first.first, results.first.second, results.second);
			}
			else
			{
				listener.dismissStickerSearchPopup();
			}
		}
	}

	public void downloadStickerTags(boolean firstTime)
	{
		InitiateStickerTagDownloadTask stickerTagDownloadTask = new InitiateStickerTagDownloadTask(firstTime);
		searchEngine.runOnQueryThread(stickerTagDownloadTask);
	}

	public void insertStickerTags(JSONObject json, int trialValue)
	{
		StickerTagInsertTask stickerInsertTask = new StickerTagInsertTask(json, trialValue);
		searchEngine.runOnQueryThread(stickerInsertTask);
	}

	public void initStickerSearchProiderSetupWizard()
	{
		StickerSearchSetupTask stickerSearchSetupTask = new StickerSearchSetupTask();
		searchEngine.runOnQueryThread(stickerSearchSetupTask);
	}

	public void removeDeletedStickerTags()
	{
		RemoveDeletedStickerTagsTask removeDeletedStickerTagsTask = new RemoveDeletedStickerTagsTask();
		searchEngine.runOnQueryThread(removeDeletedStickerTagsTask);
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
		return isFirstPhraseOrWord;
	}

	public void setAlarmFirstTime()
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SET_ALARM_FIRST_TIME, false))
		{
			Logger.d("Rebalancing", "setting alarm first time");
			setAlarm();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SET_ALARM_FIRST_TIME, true);
		}
	}

	public void setAlarm()
	{
		long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(),
				HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.REBALANCING_TIME, StickerSearchConstants.REBALACING_DEFAULT_TIME), 0, 0, 0);
		if (scheduleTime < System.currentTimeMillis())
		{
			scheduleTime += 24 * 60 * 60 * 1000;
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

	public void setNumStickersVisibleAtOneTime(int numStickersVisibleAtOneTime)
	{
		this.numStickersVisibleAtOneTime = numStickersVisibleAtOneTime;
	}

	public boolean isShowAutopopupSettingOn()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, true);
	}

	public void setShowAutopopupSettingOn(boolean showAutopopupSettingOn)
	{
		this.showAutopopupSettingOn = showAutopopupSettingOn;
	}
}