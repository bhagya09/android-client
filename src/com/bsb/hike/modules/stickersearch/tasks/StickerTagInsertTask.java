package com.bsb.hike.modules.stickersearch.tasks;

import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;

public class StickerTagInsertTask implements Runnable
{
	private JSONObject data;

	private int trialValue;

	public StickerTagInsertTask(JSONObject data, int trialValue)
	{
		this.data = data;
		this.trialValue = trialValue;
	}

	@Override
	public void run()
	{
		if ((trialValue == StickerSearchConstants.TRIAL_STICKER_DATA_FIRST_SETUP) || (trialValue == StickerSearchConstants.TRIAL_STICKER_DATA_UPDATE_REFRESH))
		{
			StickerSearchDataController.getInstance().setupStickerSearchWizard(data, trialValue);
		}
		else
		{
			Logger.d(StickerTagInsertTask.class.getSimpleName(), "Unknown trail of data setup.");
		}
	}
}
