package com.bsb.hike.modules.stickersearch.tasks;

import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;

public class StickerTagInsertTask implements Runnable
{
	private JSONObject data;

	private int state;

	public StickerTagInsertTask(JSONObject data, int state)
	{
		this.data = data;
		this.state = state;
	}

	@Override
	public void run()
	{
		if ((state == StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT) || (state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH))
		{
			StickerSearchDataController.getInstance().setupStickerSearchWizard(data, state);
		}
		else
		{
			Logger.d(StickerTagInsertTask.class.getSimpleName(), "Unknown trail of data setup.");
		}
	}
}
