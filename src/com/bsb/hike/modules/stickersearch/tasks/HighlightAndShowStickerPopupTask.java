package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class HighlightAndShowStickerPopupTask implements Runnable
{
	private CharSequence s;
	private int [] [] startEndPair;
	 
	public HighlightAndShowStickerPopupTask(CharSequence s, int [] [] whereToWhere)
	{
		this.s = s;
		this.startEndPair = whereToWhere;
	}
	
	@Override
	public void run()
	{
		StickerSearchManager.getInstance().highlightAndShowStickerPopup(s.toString(), startEndPair);
	}

}
