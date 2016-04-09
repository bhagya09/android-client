package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerTagCache;

import org.json.JSONObject;

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
		StickerTagCache.getInstance().insertTags(data,state);
	}

}
