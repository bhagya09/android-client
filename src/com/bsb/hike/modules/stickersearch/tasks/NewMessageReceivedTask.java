package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.models.Sticker;

public class NewMessageReceivedTask implements Runnable
{
	private String prevText;

	private Sticker sticker;

	private String nextText;

	public NewMessageReceivedTask(String prevText, Sticker sticker, String nextText)
	{
		this.prevText = prevText;
		this.sticker = sticker;
		this.nextText = nextText;
	}

	@Override
	public void run()
	{
		// TODO
		// StickerSearchHostManager.getInstance().sentMessage(prevText, sticker, nextText);
	}

}
