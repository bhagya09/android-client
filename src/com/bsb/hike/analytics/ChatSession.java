package com.bsb.hike.analytics;

import com.bsb.hike.utils.Logger;


public class ChatSession 
{
	private String msisdn;
	
	private long chatSessionTotalTime;
	
	private long sessionStartingTimeStamp;
	
	private boolean sessionEnded = false;

	public ChatSession(String msisdn)
	{
		this.msisdn = msisdn;
		
		startChatSession();

		chatSessionTotalTime = -1;
	}

	public void startChatSession()
	{
		sessionStartingTimeStamp = System.currentTimeMillis();
		sessionEnded = false;
	}
	
	public long getChatSessionTotalTime()
	{
		return chatSessionTotalTime;
	}
	
	private void updateChatSessionTotalTime()
	{
		long timeSpent = (System.currentTimeMillis() - sessionStartingTimeStamp);
		
		this.chatSessionTotalTime += timeSpent;
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Chat Session Time Spent -- " + timeSpent);
	}

	public void endChatSession()
	{
		if(sessionEnded)
		{
			return;
		}
		else
		{
			updateChatSessionTotalTime();
			sessionEnded = true;
		}
	}
	
	public String getMsisdn()
	{
		return msisdn;
	}
}
