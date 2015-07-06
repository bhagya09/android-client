package com.bsb.hike.modules.stickersearch;

import java.util.ArrayList;

import org.json.JSONObject;

import android.text.Editable;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.listeners.IStickerSearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.tasks.DismissStickerPopupTask;
import com.bsb.hike.modules.stickersearch.tasks.HighlightAndShowStickerPopupTask;
import com.bsb.hike.modules.stickersearch.tasks.InitiateStickerTagDownloadTask;
import com.bsb.hike.modules.stickersearch.tasks.NewMessageReceivedTask;
import com.bsb.hike.modules.stickersearch.tasks.NewMessageSentTask;
import com.bsb.hike.modules.stickersearch.tasks.RemoveDeletedStickerTagsTask;
import com.bsb.hike.modules.stickersearch.tasks.SingleCharacterHighlightTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchSetupTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerTagInsertTask;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StickerSearchManager
{
	private static StickerSearchManager _instance;

	private IStickerSearchListener listener;

	private StickerSearchEngine searchEngine;

	private volatile String currentString;

	private volatile int currentLength = 0;
	
	private boolean isFirstPhraseOrWord = false;
	
	private boolean shownFtue;

	private StickerSearchManager()
	{
		searchEngine = new StickerSearchEngine();
		shownFtue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_TIP, false);
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
			throw new IllegalStateException("some listner remains");
		}
		this.listener = null;
	}

	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		this.currentString = s.toString();
		this.currentLength = this.currentString.length();
		StickerSearchTask textChangedTask = new StickerSearchTask(s, start, before, count);
		searchEngine.runOnSearchThread(textChangedTask, 0);
	}

	public void textChanged(CharSequence s, int start, int before, int count)
	{
		Logger.d(StickerTagWatcher.TAG, "calling to search for string: " + s);

		isFirstPhraseOrWord = false;
		Pair<CharSequence, int[][]> result = StickerSearchHostManager.getInstance().onTextChange(s, start, before, count);
		if (result == null)
		{
			Logger.d(StickerTagWatcher.TAG, "unable to find recommendation result, currentTextLength = " + this.currentLength);
			if (this.currentLength > 0)
			{
				listener.unHighlightText(0, this.currentLength);
			}

			DismissStickerPopupTask dismissStickerPopupTask = new DismissStickerPopupTask();
			searchEngine.runOnUiThread(dismissStickerPopupTask, 0);
			return;
		}

		CharSequence charSequence = result.first;
		int highlightArray[][] = result.second;

		if (Utils.isBlank(charSequence) || highlightArray == null)
		{
			Logger.d(StickerTagWatcher.TAG, "no recommendation result, currentTextLength = " + this.currentLength);
			if (this.currentLength > 0)
			{
				listener.unHighlightText(0, this.currentLength);
			}

			DismissStickerPopupTask dismissStickerPopupTask = new DismissStickerPopupTask();
			searchEngine.runOnUiThread(dismissStickerPopupTask, 0);
			return;
		}

		HighlightAndShowStickerPopupTask highlightAndShowtask = new HighlightAndShowStickerPopupTask(charSequence, highlightArray);
		searchEngine.runOnUiThread(highlightAndShowtask, 0);
	}

	public void highlightSingleCharacterAndShowStickerPopup(String returnedString, int[][] highlightArray)
	{
		if (returnedString != null && highlightArray != null && returnedString.equals(currentString))
		{
			listener.highlightText(highlightArray[0][0], currentLength);
			onClickToSendSticker(highlightArray[0][0]);
		}
	}
	
	public void highlightAndShowStickerPopup(String returnedString, int[][] highlightArray)
	{
		if (listener == null)
		{
			Logger.d(StickerTagWatcher.TAG, "Resource error, can't do anything ???");
			return;
		}

		if (Utils.isBlank(returnedString) || Utils.isBlank(this.currentString) || !returnedString.equals(this.currentString))
		{
			Logger.d(StickerTagWatcher.TAG, "ontext chnanged, rapid change in text");
			listener.dismissStickerSearchPopup();
			if (currentLength > 0)
			{
				listener.unHighlightText(0, currentLength);
			}
			return;
		}

		if (highlightArray == null || highlightArray.length <= 0)
		{
			Logger.d(StickerTagWatcher.TAG, "ontext chnanged, no result for current text");
			listener.dismissStickerSearchPopup();
			if (currentLength > 0)
			{
				listener.unHighlightText(0, currentLength);
			}
			return;
		}

		// First word/ phrase is only found to be searched
		int localLength = highlightArray[0][1] - highlightArray[0][0];
		String preString = currentString.substring(0, highlightArray[0][0]);
		String postString = ((highlightArray[0][1] > currentLength) ? HikeStickerSearchBaseConstants.EMPTY : currentString.substring(highlightArray[0][1]));
		if (localLength > 0 && currentLength <= (localLength + preString.length() + postString.length()) && Utils.isBlank(preString) && Utils.isBlank(postString))
		{
			if (localLength == 1)
			{
				listener.dismissStickerSearchPopup();
				SingleCharacterHighlightTask singleCharacterHighlightTask = new SingleCharacterHighlightTask(returnedString, highlightArray);
				searchEngine.runOnUiThread(singleCharacterHighlightTask, 300);
			}
			else
			{
				listener.highlightText(highlightArray[0][0], currentLength);
				onClickToSendSticker(highlightArray[0][0]);
			}
			isFirstPhraseOrWord = true;
			return;
		}

		// More than one word may be possibly found to be searched
		listener.dismissStickerSearchPopup();
		if (highlightArray[0][0] > 0)
		{
			listener.unHighlightText(0, highlightArray[0][0]);
		}
	
		for (int i = 0, start, end; i < highlightArray.length && highlightArray[i] != null; i++)
		{
			start = highlightArray[i][0];
			end = highlightArray[i][1];

			if (end > start && end <= currentLength)
			{
				listener.highlightText(start, end);

				// Handle last phrase/ word highlighting
				if (listener != null)
				{
					if (i + 1 < highlightArray.length && highlightArray[i + 1] != null && highlightArray[i + 1][1] <= currentLength && highlightArray[i + 1][1] > end)
					{
						listener.unHighlightText(end, highlightArray[i + 1][1]);
					}
					else if (end < currentLength)
					{
						listener.unHighlightText(end, currentLength);
					}
				}
			}
			else if (start < currentLength)
			{
				listener.highlightText(start, currentLength);
				break;
			}
		}

		showStickerRecommendFtue(preString, highlightArray);
	}
	
	private void showStickerRecommendFtue(String preString, int[][] highlightArray)
	{
		if(shownFtue)
		{
			return;
		}
		
		if(highlightArray.length > 1 || !Utils.isBlank(preString)) 
		{
			listener.showStickerRecommendFtue();
		}
	}

	public void onClickToSendSticker(int clickPosition)
	{
		ArrayList<Sticker> stickerList = StickerSearchHostManager.getInstance().onClickToSendSticker(clickPosition);
		if (listener != null)
		{
			listener.dismissStickerSearchPopup();
			listener.showStickerSearchPopup(stickerList);
		}
	}
	
	public void dismissStickerSearchPopup()
	{
		if (listener != null)
		{
			listener.dismissStickerSearchPopup();
		}
		
	}

	public void downloadStickerTags(boolean firstTime)
	{
		InitiateStickerTagDownloadTask stickerTagDownloadTask = new InitiateStickerTagDownloadTask(firstTime);
		searchEngine.runOnQueryThread(stickerTagDownloadTask);
	}

	public void insertStickerTags(JSONObject json, int trialValue)
	{
		StickerTagInsertTask stickerInsertTask = new StickerTagInsertTask(json, trialValue, 1);
		searchEngine.runOnQueryThread(stickerInsertTask);
	}

	public void initSetupWizard()
	{
		StickerSearchSetupTask stickerSearchSetupTask = new StickerSearchSetupTask();
		searchEngine.runOnQueryThread(stickerSearchSetupTask);
	}
	
	public void removeDeletedStickerTags()
	{
		RemoveDeletedStickerTagsTask removeDeletedStickerTagsTask = new RemoveDeletedStickerTagsTask();
		searchEngine.runOnQueryThread(removeDeletedStickerTagsTask);
	}
	
	public void sentMessage(String prevText, Sticker sticker, String nextText)
	{
		NewMessageSentTask newMessageSentTask = new NewMessageSentTask(prevText, sticker, nextText);
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
}
