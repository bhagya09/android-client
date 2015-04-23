package com.bsb.hike.chatthread;

/**
 * This class is a barebones skeleton for Bot chat thread. This is still Work in progress.
 * 
 * @author piyush
 */
public class BotChatThread extends OneToOneChatThread
{

	/**
	 * @param activity
	 * @param msisdn
	 */
	public BotChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	@Override
	protected String[] getPubSubListeners()
	{
		return super.getPubSubListeners();
	}

}
