package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;

public class InputMethodChangedTask implements Runnable
{
	private String language;

	public InputMethodChangedTask(String language)
	{
		this.language = language;
	}

	@Override
	public void run()
	{
		StickerSearchHostManager.getInstance().onInputMethodChanged(language);
	}
}