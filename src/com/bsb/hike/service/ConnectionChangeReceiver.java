package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.filetransfer.FTApkManager;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.twinprime.TwinPrimeSDK.TwinPrimeSDK;

/**
 * class to listen to network changes
 */

public class ConnectionChangeReceiver extends BroadcastReceiver
{

	private static final String TAG = "NETWORK-CONNECTED";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
		int networktype = Utils.getNetworkShortinOrder(Utils.getNetworkTypeAsString(context));
		if (networktype >= 0)
		{
			PlatformUtils.retryPendingDownloadsIfAny(networktype); // Retrying only if internet is connected
		}

		FTApkManager.handleRetryOnConnectionChange(mprefs);

		// Disabling the network listener if the user is already signed up.(if the user updates from the play store.we dont want to listen to network changes)

//		if (Utils.isUserAuthenticated(context))
//		{
//			Utils.disableNetworkListner(context);
//			return;
//		}

		// GCM_ID_SENT_PRELOAD=true,UserAuth=false,UserOnline=true;GooglePlayServices Installed---->Best Case Scenario

		if (Utils.isUserOnline(context) && (!Utils.isUserAuthenticated(context)) && !mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false))
		{
			Intent in = new Intent(HikeService.REGISTER_TO_GCM_ACTION);
			mprefs.saveData(HikeConstants.REGISTER_GCM_SIGNUP, HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP);
			LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(in);
		}

		//Update TwinPrime Location

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TP_ENABLE, 0) == 1) {
			Location loc = Utils.getPassiveLocation();
			Logger.d("TwinPrime", "PassiveLocation is " + loc);
			if (loc != null)
				TwinPrimeSDK.setLocation(loc);
		}
	}
}

