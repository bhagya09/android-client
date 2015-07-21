package com.bsb.hike.modules.stickersearch.listeners;

import com.bsb.hike.models.Sticker;

public interface IStickerRecommendFragmentListener
{
	public void onCloseClicked(String word, String phrase);
	
	public void onSettingsClicked();
	
	public void stickerSelected(String word, String phrase, Sticker sticker, int selectIndex, int size);
}
