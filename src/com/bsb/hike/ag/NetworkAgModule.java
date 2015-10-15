package com.bsb.hike.ag;

import jp.co.agoop.networkconnectivity.lib.NetworkConnectivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class NetworkAgModule extends BroadcastReceiver
{

	private static final String TAG = NetworkAgModule.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		Logger.d(TAG, "action:" + action);
		// when the application update.
		if (action != null && action.equals(Intent.ACTION_PACKAGE_REPLACED) && intent.getDataString().equals("package:" + context.getPackageName()))
		{
			Logger.d(TAG, "Module test is updated");
			startLogging();
		}
		// when the device is restart.
		else if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Logger.d(TAG, "Phone is restarted");
			startLogging();
		}

	}

	public static void stopLogging()
	{
		NetworkConnectivity mConnectivity = NetworkConnectivity.getInstance(HikeMessengerApp.getInstance());
		if(mConnectivity != null)
		{
			try
			{
				mConnectivity.stopLogging();
			}
			catch (Exception e)
			{
				Logger.e(TAG, "error while stoping AG", e);
			}
		}
	}

	public static void startLogging()
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AG_ENABLED, false))
		{
			Logger.d(TAG, "AG network logging is not enabled.. returning");
			return;
		}
		else
		{
			if(!HikeMessengerApp.isIndianUser())
			{
				Logger.d(TAG, "AG network logging is not enabled.. since this is not an indian user");
				return;
			}
			Logger.d(TAG, "starting AG network logging");
		}
		
		long startTime = System.currentTimeMillis();
		
		try
		{
			NetworkConnectivity mConnectivity = NetworkConnectivity.getInstance(HikeMessengerApp.getInstance());
			mConnectivity.setAuthenticationKey("120a7b20-05e3-11e5-9971-023d7307ee72");// Your AuthenticationKey
			mConnectivity.setServerUrl("https://ag.hike.in/mobile");
			mConnectivity.setDownloadUrl("http://ag.hike.in/latency");
			mConnectivity.setLogIntervalSec(28800);
			mConnectivity.setHighLocationPrecise(false);
			mConnectivity.setIsDebugMode(false);
			mConnectivity.startLogging();
		}
		catch (Exception e)
		{
			Logger.e(TAG, "error while starting AG", e);
		}
		
		Logger.d(TAG, "time taken in ag module starting : "+ (System.currentTimeMillis() - startTime));
	}

}