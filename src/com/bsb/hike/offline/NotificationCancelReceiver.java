package com.bsb.hike.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Logger;

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
		Logger.d("NotificationCancelReceiver", "intent is" + intent);
		String msisdn = intent.getStringExtra(HikeConstants.MSISDN);
		OfflineUtils.sendOfflineRequestCancelPacket(msisdn);
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
		OfflineAnalytics.pushNotificationClicked(0);
		OfflineController.getInstance().removeConnectionRequest();

	}
}
