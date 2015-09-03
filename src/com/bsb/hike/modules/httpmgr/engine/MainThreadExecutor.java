package com.bsb.hike.modules.httpmgr.engine;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/** Executor that runs tasks on Android's main thread. */
public final class MainThreadExecutor implements Executor
{
	private final Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public void execute(Runnable r)
	{
		handler.post(r);
	}
	
	public void executeDelayed(Runnable r, long delayMillis)
	{
		handler.postDelayed(r, delayMillis);
	}
}
