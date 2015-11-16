package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;

public class InputMethodChangedTask implements Runnable
{
	private String languageISOCode;

	public InputMethodChangedTask(String languageISOCode)
	{
		this.languageISOCode = languageISOCode;
	}

	@Override
	public void run()
	{
		StickerSearchHostManager.getInstance().onInputMethodChanged(languageISOCode);
	}
}