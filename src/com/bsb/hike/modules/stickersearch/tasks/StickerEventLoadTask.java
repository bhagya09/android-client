package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;

public class StickerEventLoadTask implements Runnable
{
	public StickerEventLoadTask()
	{

	}

	@Override
	public void run()
	{
		StickerSearchDataController.getInstance().loadEvents();
	}
}