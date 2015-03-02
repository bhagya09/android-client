package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * class to listen to network changes
 */

public class ConnectionChangeReceiver extends BroadcastReceiver
{

	private static final String TAG = "NETWORK CHANGED";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Logger.d(TAG, "+1");
		// TODO Auto-generated method stub
		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();

		// Disabling the network listener if the user is already signed up.(if the user updates from the play store.we dont want to listen to network changes)

		if (Utils.isUserAuthenticated(context))
		{
			Utils.disableNetworkListner(context);
			return;
		}

		// GCM_ID_SENT_PRELOAD=true,UserAuth=false,UserOnline=true;GooglePlayServices Installed---->Best Case Scenario

		if (Utils.isUserOnline(context) && (!Utils.isUserAuthenticated(context)) && !mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false))
		{
			Intent in = new Intent(HikeService.REGISTER_TO_GCM_ACTION);
			mprefs.saveData(HikeConstants.REGISTER_GCM_SIGNUP, HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP);
			LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(in);
		}
	}

}