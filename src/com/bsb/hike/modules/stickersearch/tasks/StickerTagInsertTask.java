package com.bsb.hike.modules.stickersearch.tasks;

import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;

public class StickerTagInsertTask implements Runnable
{
	private int state;
	
	private JSONObject data;

	private int trialValue;

	public StickerTagInsertTask(JSONObject data, int trialValue, int state)
	{
		this.state = state;
		this.data = data;
		this.trialValue = trialValue;
	}

	@Override
	public void run()
	{
		if (trialValue == 0) {
			StickerSearchDataController.getInstance().setupStickerSearchWizard(data, state);
		} else if (trialValue == 1) {
			StickerSearchDataController.getInstance().updateStickerSearchWizard(data, state);
		} else {
			Logger.d(StickerTagInsertTask.class.getSimpleName(), "Unknown trail of data setup.");
		}
	}
}
