package com.bsb.hike.modules.stickersearch.tasks;

import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;

public class StickerTagInsertTask implements Runnable
{
	private int state;
	
	private JSONObject data;
	
	public StickerTagInsertTask(JSONObject data, int state)
	{
		this.state = state;
		this.data = data;
	}

	@Override
	public void run()
	{
		StickerSearchDataController.getInstance().setupStickerSearchWizard(data, state);
	}
}
