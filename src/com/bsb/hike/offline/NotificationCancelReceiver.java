package com.bsb.hike.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

/**
 * 
 * @author himanshu
 *	A broadcast receiver use to cancel a notification
 */
public class NotificationCancelReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
		
	}
}
