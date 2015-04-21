package com.bsb.hike.utils;

import android.os.Handler;
import android.os.Looper;

/**
 *
 * @author anubhavgupta
 *
 */
public class HikeUiHandler
{

	private static Handler mUihandler = new Handler(Looper.getMainLooper());
	
	/**
	 * Return handler on Main Thread
	 */
	public static Handler getHandler()
	{
		return mUihandler;
	}
	
}
