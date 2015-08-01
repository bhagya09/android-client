package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;

public class LoadChatProfileTask implements Runnable
{
	private String contactId;

	private boolean isGroupChat;

	private long lastMessageTimestamp;

	public LoadChatProfileTask(String msidn, boolean isGroupChat, long lastMessageTimestamp)
	{
		this.contactId = msidn;
		this.isGroupChat = isGroupChat;
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	@Override
	public void run()
	{
		StickerSearchHostManager.getInstance().loadChatProfile(contactId, isGroupChat, lastMessageTimestamp);
	}
}