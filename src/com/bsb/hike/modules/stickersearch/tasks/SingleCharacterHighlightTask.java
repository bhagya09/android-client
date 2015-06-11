package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class SingleCharacterHighlightTask implements Runnable
{
	private String returnedString;

	private int[][] highlightArray;

	public SingleCharacterHighlightTask(String returnedString, int[][] highlightArray)
	{
		synchronized (StickerSearchManager.class)
		{
			this.returnedString = returnedString;
			this.highlightArray = highlightArray;
		}
	}

	@Override
	public void run()
	{
		StickerSearchManager.getInstance().highlightSingleCharacterAndShowStickerPopup(this.returnedString, this.highlightArray);
	}

}
