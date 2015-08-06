package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;

public class StickerSearchTask implements Runnable
{
	private CharSequence s;

	private int start, before, count;

	public StickerSearchTask(CharSequence s, int start, int before, int count)
	{
		this.s = s;
		this.start = start;
		this.before = before;
		this.count = count;
	}

	@Override
	public void run()
	{
		StickerSearchManager.getInstance().textChanged(s, start, before, count);
	}
}