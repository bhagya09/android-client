package com.bsb.hike.modules.stickersearch.listeners;

import com.bsb.hike.models.Sticker;

import java.util.List;

public interface IStickerRecommendFragmentListener
{
	public void onCloseClicked(String word, String phrase, boolean ftue);
	
	public void onSettingsClicked();
	
	public void stickerSelected(String word, String phrase, Sticker sticker, int selectIndex, List<Sticker> stickerList, String source, boolean dismissAndClear);

	public void shownStickerRecommendFtue();
}
