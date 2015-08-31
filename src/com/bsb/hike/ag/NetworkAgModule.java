package com.bsb.hike.ag;


import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import jp.co.agoop.networkconnectivity.lib.NetworkConnectivity;
import android.content.Context;

public class NetworkAgModule
{

	private static final String TAG = NetworkAgModule.class.getSimpleName();

	public static void stopLogging()
	{
		NetworkConnectivity mConnectivity = NetworkConnectivity.getInstance(HikeMessengerApp.getInstance());
		if(mConnectivity != null)
		{
			mConnectivity.stopLogging();
		}
	}

	public static void startLogging()
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AG_ENABLED, true))
		{
			Logger.d(TAG, "AG network logging is not enabled.. returning");
			return;
		}
		else
		{
			Logger.d(TAG, "starting AG network logging");
		}
		
		long startTime = System.currentTimeMillis();
		
		NetworkConnectivity mConnectivity = NetworkConnectivity.getInstance(HikeMessengerApp.getInstance());
		mConnectivity.setAuthenticationKey("120a7b20-05e3-11e5-9971-023d7307ee72");// Your AuthenticationKey
		mConnectivity.setServerUrl("https://ag.hike.in/mobile");
		mConnectivity.setDownloadUrl("http://ag.hike.in/latency");
		mConnectivity.setLogIntervalSec(28800);
		mConnectivity.setHighLocationPrecise(false);
		mConnectivity.setIsDebugMode(true);
		mConnectivity.startLogging();
		
		Logger.d(TAG, "time taken in ag module starting : "+ (System.currentTimeMillis() - startTime));
	}

}