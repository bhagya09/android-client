package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.android.gcm.GCMRegistrar;

public class RegisterToGCMTrigger extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Logger.d(getClass().getSimpleName(), "Registering for GCM");

		try
		{
			GCMRegistrar.checkDevice(context);
			GCMRegistrar.checkManifest(context);
			final String regId = GCMRegistrar.getRegistrationId(context);

			if (regId.isEmpty())
			{
				/*
				 * Since we are registering again, we should clear this preference
				 */
				HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
				mprefs.removeData(HikeMessengerApp.GCM_ID_SENT);

				GCMRegistrar.register(context, HikeConstants.APP_PUSH_ID);
			}
			else
			{
				Intent in = new Intent(HikeService.SEND_TO_SERVER_ACTION);
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(in);
			}
		}
		catch (UnsupportedOperationException e)
		{
			/*
			 * User doesnt have google services
			 */
		}
		catch (IllegalStateException e)
		{
			Logger.e("HikeService", "Exception during gcm registration : ", e);
		}
	}
}