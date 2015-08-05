package com.bsb.hike.modules.stickersearch.tasks;

import android.util.Pair;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class HighlightAndShowStickerPopupTask implements Runnable
{
	private Pair<CharSequence, int[][]> result;

	public HighlightAndShowStickerPopupTask(Pair<CharSequence, int[][]> result)
	{
		this.result = result;
	}

	@Override
	public void run()
	{
		StickerSearchManager.getInstance().highlightAndShowStickerPopup(result);
	}
}
