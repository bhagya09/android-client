package com.bsb.hike.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.productpopup.AtomicTipManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.triggers.InterceptUtils;
import com.bsb.hike.utils.BirthdayUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;

/**
 * This receiver is responsible for capturing notification dismissed/deleted events and consequently clear notification message stack. This is done so that messages already shown
 * once in a notification are not repeated in subsequent notifications.
 * 
 * @author Atul M
 * 
 */
public class NotificationDismissedReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent != null)
		{
			int notificationId = intent.getIntExtra(HikeNotification.HIKE_NOTIFICATION_ID_KEY, 0);

			if (notificationId == HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID)
			{
				//Get current count of retry.
				 int retryCount  = intent.getExtras().getInt(HikeConstants.RETRY_COUNT, 0);
				 //Right now we retry only once.
				 
				 int maxRetryCount = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MAX_REPLY_RETRY_NOTIF_COUNT, HikeConstants.DEFAULT_MAX_REPLY_RETRY_NOTIF_COUNT);
				if (retryCount < maxRetryCount && !HikeNotificationMsgStack.getInstance().isEmpty())
				{
					long retryTime = HikeNotification.getInstance().getNextRetryNotificationTime(retryCount);
					Logger.i("NotificationDismissedReceiver", "NotificationDismissedReceiver called alarm time = "
							+retryTime  + "retryCount = "+retryCount);
					
					Intent retryNotificationIntent = new Intent();
					retryNotificationIntent.putExtra(HikeConstants.RETRY_COUNT, retryCount+1);
					HikeAlarmManager.setAlarmWithIntent(context, retryTime,
							HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION, false, retryNotificationIntent);
				}
			}

			else if(notificationId == HikeNotification.NOTIFICATION_PRODUCT_POPUP)
			{
				if (intent.hasExtra(HikeConstants.TIP_ID))
				{
					String tipId = intent.getStringExtra(HikeConstants.TIP_ID);
					boolean isCancellable = intent.getBooleanExtra(ProductPopupsConstants.IS_CANCELLABLE, true);
					String analyticsTag = intent.getStringExtra(AnalyticsConstants.EXP_ANALYTICS_TAG);
					AtomicTipManager.getInstance().tipFromNotifAnalytics(AnalyticsConstants.AtomicTipsAnalyticsConstants.TIP_NOTIF_SWIPED, tipId, isCancellable, analyticsTag);
				}
			}

			else if (notificationId == HikeNotification.NOTIF_INTERCEPT_NON_DOWNLOAD)
			{
				String type = intent.getStringExtra(HikeConstants.TYPE);
				if(!TextUtils.isEmpty(type))
				{
					InterceptUtils.recordInterceptEventV2(type, AnalyticsConstants.InterceptEvents.INTERCEPT_NOTIF_SWIPED, AnalyticsConstants.InterceptEvents.SWIPED);
				}
			}

			else if (notificationId == HikeNotification.BIRTHDAY_NOTIF)
			{
				String packetId = intent.getStringExtra(HikeConstants.ID);
				ArrayList<String> list = intent.getStringArrayListExtra(HikeConstants.Extras.LIST);
				BirthdayUtils.recordBirthdayAnalytics(
						AnalyticsConstants.BirthdayEvents.BIRTHDAY_NOTIF_SWIPE_OFF,
						AnalyticsConstants.BirthdayEvents.BIRTHDAY_PUSH_NOTIF,
						AnalyticsConstants.BirthdayEvents.BIRTHDAY_NOTIF_SWIPE_OFF,
						String.valueOf(packetId), null, null, null, null, list.toString());
			}
			else
			{
				if(intent.getBooleanExtra(HikeConstants.MqttMessageTypes.USER_JOINED, false))
				{
					Logger.d(HikeConstants.UserJoinMsg.TAG, "uj notif dismissed");
					String msisdn = intent.getStringExtra(HikeConstants.MSISDN);
					String tag = intent.getStringExtra(AnalyticsConstants.EXP_ANALYTICS_TAG);
					HikeNotificationUtils.recordRichUJNotifSwipe(String.valueOf(notificationId), tag, msisdn);
				}
			}
		}

	}

}
