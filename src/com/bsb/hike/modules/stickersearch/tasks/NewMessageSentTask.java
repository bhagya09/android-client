package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;

public class NewMessageSentTask implements Runnable
{
	private String prevText;

	private Sticker sticker;

	private String nextText;

	public NewMessageSentTask(String prevText, Sticker sticker, String nextText)
	{
		this.prevText = prevText;
		this.sticker = sticker;
		this.nextText = nextText;
	}

	@Override
	public void run()
	{
		StickerSearchHostManager.getInstance().onMessageSent(prevText, sticker, nextText);
	}
}
