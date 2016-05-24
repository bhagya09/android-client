package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;

public class StickerEventsLoadTask implements Runnable
{
	public StickerEventsLoadTask()
	{

	}

	@Override
	public void run()
	{
		// Load events in main memory, which may occur in near future.
		// Loading is being done once in 24 hours by default (this condition is there inside called method)
		StickerSearchDataController.getInstance().loadStickerEvents();
	}
}