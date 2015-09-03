package com.bsb.hike.modules.stickersearch.listeners;

import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;

public interface IStickerPickerRecommendationListener extends StickerPickerListener
{
	public void stickerSelectedRecommedationPopup(Sticker sticker, String source, boolean clearText);
}
