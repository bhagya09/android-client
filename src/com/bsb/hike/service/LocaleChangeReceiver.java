package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

public class LocaleChangeReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		/*
		 * If the user has not signed up yet, don't do anything.
		 */
		if (TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING, null)))
		{
			return;
		}

		Utils.sendLocaleToServer();
	}

}
