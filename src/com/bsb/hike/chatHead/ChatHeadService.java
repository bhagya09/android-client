package com.bsb.hike.chatHead;

import java.util.Set;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;

public class ChatHeadService extends Service
{

	private String TAG = "ChatHeadService";
	
	
	// boolean to show whether the chat head must be shown or not for a particular session
	
	private final Handler chatHeadHandler = new Handler();

	private Runnable chatHeadRunnable = new Runnable()
	{

		@Override
		public void run()
		{   
			Set<String> foregroundPackages = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS);
			if(!ChatHeadUtils.useOfAccessibilittyPermitted())
			{
				ChatHeadViewManager.getInstance(ChatHeadService.this).actionWindowChange(foregroundPackages);
			}
			chatHeadHandler.postDelayed(this, 1000L);
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		chatHeadHandler.removeCallbacks(chatHeadRunnable);
		chatHeadHandler.postDelayed(chatHeadRunnable, 1000L);
		return Service.START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		ChatHeadViewManager.getInstance(this).onCreate();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
        ChatHeadViewManager.getInstance(this).onConfigChanged();
	}

	@Override
	public void onDestroy()
	{
		chatHeadHandler.removeCallbacks(chatHeadRunnable);
		ChatHeadViewManager.getInstance(this).onDestroy();
		super.onDestroy();
	}

}
