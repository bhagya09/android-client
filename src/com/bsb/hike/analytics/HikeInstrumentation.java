package com.bsb.hike.analytics;

public class HikeInstrumentation
{
	private static long chatOpenStartTime = -1;

	public static long getChatOpenStartTime()
	{
		return chatOpenStartTime;
	}

	public static void setChatOpenStartTime(long time)
	{
		chatOpenStartTime = time;
	}
	
	public static void resetChatOpenStartTime()
	{
		chatOpenStartTime = -1;
	}
	
	public static void recordTimeTakenInChatOpen()
	{
		if(chatOpenStartTime == -1)
		{
			return;
		}
		
		long chatOpenTime = System.currentTimeMillis() - chatOpenStartTime;
		resetChatOpenStartTime();
	}
}
