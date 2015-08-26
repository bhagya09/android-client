package com.bsb.hike.modules.stickersearch.tasks;

import android.util.Pair;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class HighlightAndShowStickerPopupTask implements Runnable
{
	private Pair<CharSequence, int[][]> result;

	private boolean isLastShownPopupWasAutoSuggestion;

	public HighlightAndShowStickerPopupTask(Pair<CharSequence, int[][]> result, boolean isLastShownPopupWasAutoSuggestion)
	{
		this.result = result;

		this.isLastShownPopupWasAutoSuggestion = isLastShownPopupWasAutoSuggestion;
	}

	@Override
	public void run()
	{
		StickerSearchManager.getInstance().highlightAndShowStickerPopup(result, isLastShownPopupWasAutoSuggestion);
	}
}
