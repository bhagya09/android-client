package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;

public class LoadChatProfileTask implements Runnable
{
	private String contactId;

	private boolean isGroupChat;

	private long lastMessageTimestamp;

	private String keyboardLanguageISOCode;

	public LoadChatProfileTask(String msidn, boolean isGroupChat, long lastMessageTimestamp, String keyboardLanguageISOCode)
	{
		this.contactId = msidn;
		this.isGroupChat = isGroupChat;
		this.lastMessageTimestamp = lastMessageTimestamp;
		this.keyboardLanguageISOCode = keyboardLanguageISOCode;
	}

	@Override
	public void run()
	{
		StickerSearchHostManager.getInstance().loadChatProfile(contactId, isGroupChat, lastMessageTimestamp, keyboardLanguageISOCode);
	}
}