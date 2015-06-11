package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchSetupManager;

public class StickerSearchSetupTask implements Runnable
{
	
	public StickerSearchSetupTask()
	{

	}

	@Override
	public void run()
	{
		StickerSearchSetupManager.getInstance().init();
	}
}