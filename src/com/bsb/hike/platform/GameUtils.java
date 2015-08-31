package com.bsb.hike.platform;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.models.HikeHandlerUtil;

import android.app.Activity;

public class GameUtils {
	public static final int PUT_IN_CACHE = 2;
	public static final int GET_FROM_CACHE = 3;
	public static final int LOG_ANALYTICS = 4;
	public static final int FORWARD_TO_CHAT = 5;
	public BotInfo mBotInfo;
	Activity activity;
	HikeHandlerUtil mThread;
	PlatformHelper helper;
	public GameUtils(BotInfo mBotInfo,Activity activty)
	{
		this.mBotInfo=mBotInfo;
		this.activity=activity;
		helper=new PlatformHelper(this.mBotInfo,this.activity);
		
	}
	public void callNative(final String id,final int functionId,final Object ... data)
	{
		mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable()
		{
			
			@Override
			public void run()
			{
				switch(functionId)
				{
				case PUT_IN_CACHE:
					helper.putInCache((String)data[0],(String)data[1]);
					
				case GET_FROM_CACHE:
					String cache=helper.getFromCache((String)data[0],(String)data[1]);
//					runOnGLthread(new Runnable()
//							{
//								@Override
//								public void run()
//								{
//									gameCallback(id,cache);
//								}
//							});
					
				case LOG_ANALYTICS:
					helper.logAnalytics((String)data[0],(String)data[1],(String)data[2]);
					
				case FORWARD_TO_CHAT:
					helper.forwardToChat((String)data[0],(String)data[1]);
					
				}
			}
		});
	}

}
