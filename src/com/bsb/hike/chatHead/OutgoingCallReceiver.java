package com.bsb.hike.chatHead;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Broadcast receiver to detect the outgoing calls.
 */
public class OutgoingCallReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), number);
	}

}