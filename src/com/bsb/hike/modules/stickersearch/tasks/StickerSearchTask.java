package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.Logger;

public class StickerSearchTask implements Runnable
{
	private static final String TAG = StickerSearchTask.class.getSimpleName();

	private CharSequence s;
	
	private int start, before, count;
	
	public StickerSearchTask(CharSequence s, int start , int before, int count)
	{
		this.s = s;
		this.start = start;
		this.before = before;
		this.count = count;
	}
	
	@Override
	public void run()
	{
		try {
			StickerSearchManager.getInstance().textChanged(s, start, before, count);
		}
		catch (Exception e) {
			Logger.d(TAG, "Exception in searching..." + (e == null ? e : e.getMessage()));
		}
	}

}
