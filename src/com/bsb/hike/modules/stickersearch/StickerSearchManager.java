package com.bsb.hike.modules.stickersearch;

import java.util.ArrayList;

import org.json.JSONObject;

import android.support.v4.util.Pair;
import android.text.Editable;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.tasks.HighlightAndShowStickerPopupTask;
import com.bsb.hike.modules.stickersearch.tasks.InitiateStickerTagDownloadTask;
import com.bsb.hike.modules.stickersearch.tasks.RemoveDeletedStickerTagsTask;
import com.bsb.hike.modules.stickersearch.tasks.SingleCharacterHighlightTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchSetupTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerSearchTask;
import com.bsb.hike.modules.stickersearch.tasks.StickerTagInsertTask;
import com.bsb.hike.utils.Logger;

public class StickerSearchManager
{
	private static StickerSearchManager _instance;

	private IStickerSearchListener listener;

	private StickerSearchEngine searchEngine;

	private String currentString;

	private int currentLength = 0;

	private StickerSearchManager()
	{
		searchEngine = new StickerSearchEngine();
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
		StickerSearchTask textChangedTask = new StickerSearchTask(s, start, before, count);
		searchEngine.runOnSearchThread(textChangedTask, 0);
	}

	public void afterTextChanged(Editable editable)
	{
		this.currentString = editable.toString();
		this.currentLength = currentString.length();
	}

	public void textChanged(CharSequence s, int start, int before, int count)
	{
		Logger.d(StickerTagWatcher.TAG, " making search query for string  " + s);

		Pair<CharSequence, int[][]> result = StickerSearchHostManager.getInstance().onTextChange(s, start, before, count);

		if (result == null || result.first == null || result.second == null)
		{
			Logger.d(StickerTagWatcher.TAG, " null result ");
			return;
		}

		CharSequence charSequence = result.first;
		int highlightArray[][] = result.second;

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
			return;
		}

		if (returnedString == null || !returnedString.equals(this.currentString))
		{
			Logger.d(StickerTagWatcher.TAG, "ontext chnanged rapid change in text");
			listener.dismissStickerSearchPopup();
			return;
		}

		if (highlightArray == null || highlightArray.length <= 0)
		{
			listener.dismissStickerSearchPopup();
			if (currentLength > 0)
			{
				listener.unHighlightText(0, currentLength);
			}
			return;
		}

		int localLength = highlightArray[0][1] - highlightArray[0][0];
		if (currentLength <= (localLength + currentString.substring(0, highlightArray[0][0]).length()))
		{
			if (localLength == 1)
			{
				SingleCharacterHighlightTask singleCharacterHighlightTask = new SingleCharacterHighlightTask(returnedString, highlightArray);
				searchEngine.runOnUiThread(singleCharacterHighlightTask, 300);
			}
			else
			{
				listener.highlightText(highlightArray[0][0], currentLength);
				onClickToSendSticker(highlightArray[0][0]);
			}
			return;
		}

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
				if (listener != null)
				{
					if (i + 1 < highlightArray.length && highlightArray[i + 1] != null && highlightArray[i + 1][1] <= currentLength && highlightArray[i + 1][1] > end)
					{
						listener.unHighlightText(end, highlightArray[i + 1][1]);
					}
					else
						listener.unHighlightText(end, currentLength);
				}
			}
			else if (start < currentLength)
			{
				listener.highlightText(start, currentLength);
				break;
			}
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

	public void onSend(String text)
	{
		if (listener != null)
		{
			listener.dismissStickerSearchPopup();
		}
		StickerSearchHostManager.getInstance().onSend(text);
	}

	public void downloadStickerTags(boolean firstTime)
	{
		InitiateStickerTagDownloadTask stickerTagDownloadTask = new InitiateStickerTagDownloadTask(firstTime);
		searchEngine.runOnQueryThread(stickerTagDownloadTask);
	}

	public void insertStickerTags(JSONObject json)
	{
		StickerTagInsertTask stickerInsertTask = new StickerTagInsertTask(json, 1);
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
}
