package com.bsb.hike.modules.stickersearch;

import com.bsb.hike.models.Sticker;

public interface IStickerRecommendFragmentListener
{
	public void onCloseClicked();
	
	public void onSettingsClicked();
	
	public void stickerSelected(Sticker sticker);
}
