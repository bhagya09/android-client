package com.bsb.hike.modules.stickersearch;

import java.util.List;

import com.bsb.hike.models.Sticker;

public interface IStickerSearchListener
{
	public void highlightText(int start, int end);
	
	public void unHighlightText(int start, int end);
	
	public void showStickerSearchPopup(List<Sticker> stickerList);
	
	public void dismissStickerSearchPopup();
}
