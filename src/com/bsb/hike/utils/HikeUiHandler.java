package com.bsb.hike.utils;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author himanshu
 * 
 *         A non leaky handler that keeps a weak Ref of activty and removes all callbacks on destroy.
 * 
 */
public class HikeUiHandler extends Handler
{
	WeakReference<IHandlerCallback> iHandlerCallback;

	public HikeUiHandler(IHandlerCallback iHandlerCallback)
	{
		this.iHandlerCallback = new WeakReference<HikeUiHandler.IHandlerCallback>(iHandlerCallback);
	}

	protected void onDestroy()
	{
		removeCallbacksAndMessages(null);
		
	}

	@Override
	public void handleMessage(Message msg)
	{
		if (iHandlerCallback.get() != null)
		{
			iHandlerCallback.get().handleUIMessage(msg);
		}
	}
	

	/**
	 * 
	 * @author himanshu
	 * 
	 *         An Interface that will be called only if the activity is not destroyed.All the messages will be delivered  
	 *         in this method on the UI thread.
	 */
	public static interface IHandlerCallback
	{
		public void handleUIMessage(Message msg);
	}
	
}
