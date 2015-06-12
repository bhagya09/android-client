package com.bsb.hike.modules.stickersearch.tasks;

import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchSetupManager;

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
		StickerSearchSetupManager.getInstance().setupStickerSearchWizard(data, state);
	}
}
