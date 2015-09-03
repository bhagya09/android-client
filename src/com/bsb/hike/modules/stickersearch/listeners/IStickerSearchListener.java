package com.bsb.hike.modules.stickersearch.listeners;

import java.util.List;

import com.bsb.hike.models.Sticker;

public interface IStickerSearchListener
{
	public void highlightText(int start, int end);
	
	public void unHighlightText(int start, int end);
	
	public void showStickerSearchPopup(String word, String phrase, List<Sticker> stickerList);
	
	public void dismissStickerSearchPopup();
	
	public void showStickerRecommendFtueTip();
	
	public void showStickerRecommendAutoPopupOffTip();
	
	public void dismissTip(int whichTip);
	
	public void setTipSeen(int whichTip, boolean dismissIfVisible);
	
	public void clearSearchText();
	
	public void selectSearchText();

	public boolean isStickerRecommendationPopupShowing();
}
