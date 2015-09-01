package com.bsb.hike.platform;

import java.lang.ref.WeakReference;

import com.bsb.hike.DummyGameActivity;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.models.HikeHandlerUtil;

import android.app.Activity;
import android.util.Log;

public class GameUtils
{
	public BotInfo mBotInfo;

	Activity activity;

	HikeHandlerUtil mThread;

	PlatformHelper helper;

	public static final String tag = "GameUtils";
	protected WeakReference<Activity> weakActivity;

	public GameUtils(BotInfo mBotInfo, Activity activty)
	{
		this.mBotInfo = mBotInfo;
		this.activity = activity;
		weakActivity = new WeakReference<Activity>(activity);
		
		mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();

	}

	public void validate()
	{
		if (mBotInfo == null)
		{
			Log.d(tag, "mbot info is null");
			return;
		}
	}

	public void putInCache(final String key, final String value)
	{

		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.putInCache(key, value,mBotInfo);
			}
		});

	}

	public void getFromCache(final String key)
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				String cache = helper.getFromCache(key,mBotInfo);
				DummyGameActivity.gameActivity.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						// gameCallback(id,cache);
					}
				});

			}
		});
	}

	public void logAnalytics(final String isUI, final String subType, final String json)
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.logAnalytics(isUI, subType, json,mBotInfo);
			}
		});
	}

	public void forwardToChat(final String json, final String hikeMessage)
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.forwardToChat(json, hikeMessage,mBotInfo,weakActivity);
			}
		});
	}

}
