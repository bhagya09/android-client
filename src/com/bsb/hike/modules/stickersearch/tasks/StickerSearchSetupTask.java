package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;

public class StickerSearchSetupTask implements Runnable
{
	
	public StickerSearchSetupTask()
	{

	}

	@Override
	public void run()
	{
		StickerSearchDataController.getInstance().init();
	}
}