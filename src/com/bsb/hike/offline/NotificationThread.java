package com.bsb.hike.offline;

import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.google.gson.Gson;
import com.bsb.hike.chatthread.ChatThreadActivity;

/**
 * 
 * @author himanshu
 *	This class is used to show sticky notification in case of OfflineConnection
 */
public class NotificationThread implements Runnable
{

	private boolean keepRunning = true;

	private int time = 59;

	private NotificationManager notificationManager = null;

	private NotificationCompat.Builder builder = null;

	private Bitmap bitmap = null;

	private String contactFirstName;
	
	private final int sleepDuration = 1000;

	public void setCancel()
	{
		keepRunning = false;
	}

	public  NotificationThread()
	{
		OfflineParameters offlineParameters = OfflineController.getInstance().getConfigurationParamerters();
		time = offlineParameters.getConnectionTimeout() / 1000;
	}
	@Override
	public void run()
	{
		while (keepRunning && --time >= 0)
		{
			if (!showNotification(time))
			{
				keepRunning = false;
			}
			try
			{
				Thread.sleep(sleepDuration);
			}
			catch (InterruptedException e)
			{
				keepRunning = false;
				e.printStackTrace();
			}
		}
		cancelNotification();
	}

	/**
	 * 
	 * @param time
	 * @return true if notification is shown .False if the connecting msisdn is null or we connected/disconnected 
	 */
	public boolean showNotification(int time)
	{

		long startTime=System.currentTimeMillis();
		if (!(!TextUtils.isEmpty(OfflineUtils.getConnectingMsisdn()) && OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTING))
		{
			return false;
		}

		String connectingMsisdn = OfflineUtils.getConnectingMsisdn();
		Context context = HikeMessengerApp.getInstance().getApplicationContext();

		Logger.d("NotificationOffline", "Showing notification.." + connectingMsisdn);
		Intent myIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, connectingMsisdn, false,false,
				ChatThreadActivity.ChatThreadOpenSources.NOTIF);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (notificationManager == null)
			notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (builder == null)
			builder = new NotificationCompat.Builder(context);

		String title = null;

		String durationString = (time == 0) ? "" : String.format(Locale.getDefault(), " (%02d:%02d)", (time / 60), (time % 60));

		String text = context.getResources().getString(R.string.awaiting_response) + " " + durationString;

		if (TextUtils.isEmpty(contactFirstName))
		{
			if (StealthModeManager.getInstance().isStealthMsisdn(connectingMsisdn) && !StealthModeManager.getInstance().isActive())
			{
				contactFirstName = "Hike Direct Connection Request";
			}
			else
			{
				ContactInfo contactInfo = ContactManager.getInstance().getContact(connectingMsisdn);
				if (contactInfo != null && !TextUtils.isEmpty(contactInfo.getFirstName()))
				{
					contactFirstName = contactInfo.getFirstName();
				}
			}
		}
		title = contactFirstName;

		if (bitmap == null)
		{
			if (StealthModeManager.getInstance().isStealthMsisdn(connectingMsisdn) && !StealthModeManager.getInstance().isActive())
			{
				bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notify);
			}
			else
			{
				Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectingMsisdn);
				if (drawable == null)
				{
					drawable = HikeBitmapFactory.getDefaultTextAvatar(connectingMsisdn);
				}
				// bitmap = HikeBitmapFactory.drawableToBitmap(drawable);
				bitmap = HikeBitmapFactory.getCircularBitmap(HikeBitmapFactory.returnScaledBitmap((HikeBitmapFactory.drawableToBitmap(drawable, Bitmap.Config.RGB_565)), context));
			}
		}
		if (bitmap == null)
		{
			return false;
		}
		Notification myNotification = builder.setContentTitle(title).setContentText(text).setSmallIcon(HikeNotification.getInstance().returnSmallIcon())
				.setContentIntent(pendingIntent).setOngoing(true).setLargeIcon(bitmap).setColor(context.getResources().getColor(R.color.blue_hike)).build();

		notificationManager.notify(null, OfflineConstants.NOTIFICATION_IDENTIFIER, myNotification);
		Logger.d("NotificationThread",(System.currentTimeMillis()-startTime) + "");
		return true;
	}

	public void cancelNotification()
	{
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		HikeNotification.getInstance().cancelNotification(OfflineConstants.NOTIFICATION_IDENTIFIER);
	}

}
