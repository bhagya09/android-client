package com.bsb.hike.modules.httpmgr.network;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

public class NetworkChecker
{
	public static boolean isNetworkAvailable()
	{
		return Utils.isUserOnline(HikeMessengerApp.getInstance());
	}
}
