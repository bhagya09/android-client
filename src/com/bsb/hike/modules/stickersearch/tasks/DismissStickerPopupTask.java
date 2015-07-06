package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class DismissStickerPopupTask implements Runnable
{

	public DismissStickerPopupTask()
	{
	}
	
	@Override
	public void run()
	{
		StickerSearchManager.getInstance().dismissStickerSearchPopup();
	}

}
