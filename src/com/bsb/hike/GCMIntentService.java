package com.bsb.hike;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.service.PreloadNotificationSchedular;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;
import com.google.android.gcm.GCMBaseIntentService;

//import com.bsb.hike.service.HikeMqttManager;

public class GCMIntentService extends GCMBaseIntentService
{
	public static final String DEV_TYPE = "dev_type";

	public static final String DEV_TOKEN = "dev_token";

	public static final String OLD_DEV_TOKEN = "old_dev_token";
	
	public static final String NOTIFICATION="notification";
	
	public static final String RECONNECT_VALUE="1";

	private HikeSharedPreferenceUtil prefs;

	public GCMIntentService()
	{
		super(HikeConstants.APP_PUSH_ID);
	}

	@Override
	protected void onError(Context context, String errorId)
	{
		Logger.e(getClass().getSimpleName(), "ERROR OCCURRED " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent)
	{
		Logger.d(VoIPConstants.TAG, "GCM message received.");
		Logger.d(getClass().getSimpleName(), "Message received: " + intent.getExtras());

		prefs = HikeSharedPreferenceUtil.getInstance();

		String reconnectVal = "0";
		if (!Utils.isUserAuthenticated(context))
		{

			String notification = intent.getStringExtra(NOTIFICATION);

			if (notification != null && prefs != null)
			{
				HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD);
				prefs.saveData(PreloadNotificationSchedular.CURRENT_ALARM_ID, 0);
				prefs.saveData(PreloadNotificationSchedular.NOTIFICATION_TIMELINE, notification);
				PreloadNotificationSchedular.scheduleNextAlarm(context);
			}
			sendAnalyticsEvent(intent, intent.getStringExtra("msg"), reconnectVal, false);
			return;
		}


		HikeMessengerApp app = (HikeMessengerApp) context.getApplicationContext();
		app.connectToService();
		String message = intent.getStringExtra("msg");
		if (!TextUtils.isEmpty(message))
		{
			HikeHandlerUtil.getInstance().postRunnableWithDelay(new MessageArrivedRunnable(message), 0);
		}
		else
		{
			reconnectVal = intent.getStringExtra("pushReconnect");
			boolean reconnect = false;
			Logger.d(getClass().getSimpleName(), "Server sent packet pushReconnect : " + reconnectVal);
			if (RECONNECT_VALUE.equals(reconnectVal))
				reconnect = true;
			String jsonString = intent.getStringExtra(HikeConstants.Extras.OFFLINE_PUSH_KEY);
			if (null != jsonString && jsonString.length() > 0)
			{
				Logger.d("HikeToOffline", "Gcm push received : json :" + jsonString);
				/*
				 * if user has turned off hike offline notification setting then dont show hike offline push
				 */
				if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.HIKE_OFFLINE_NOTIFICATION_PREF, true))
				{
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.OFFLINE_PUSH_KEY, jsonString);
					HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_TO_OFFLINE_PUSH, bundle);
				}
			
			}
		context.sendBroadcast(new Intent(MqttConstants.MQTT_CONNECTION_CHECK_ACTION).putExtra("reconnect", reconnect));
		}
		
		sendAnalyticsEvent(intent, message, reconnectVal, true);

	}
	

	private void sendAnalyticsEvent( Intent intent, String message, String reconnectVal, boolean userAuthenticated)
	{
		//if server switch is off
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.GCM_PROD_AREA_LOGGING, true))
		{
			return;
		}
		
		JSONObject infoJson = new JSONObject();
		try 
		{
			infoJson.put("intent", intent.toString());
			infoJson.put("extras", intent.getExtras());
			infoJson.put("reconnectVal", reconnectVal);
			infoJson.put("userAuthenticated", userAuthenticated);
		
			String devArea = "gcm";	
			HAManager.getInstance().logDevEvent(MqttConstants.CONNECTION_PROD_AREA, devArea, infoJson);
		} 
		catch (JSONException jsonEx) 
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json:",jsonEx);
		}
	}

	@Override
	protected void onRegistered(final Context context, String regId)
	{
		prefs = HikeSharedPreferenceUtil.getInstance();

		if (!prefs.getData(HikeConstants.GCM_ID, "").equals(regId))
		{

			prefs.saveData(HikeConstants.GCM_ID, regId);
			switch (prefs.getData(HikeConstants.REGISTER_GCM_SIGNUP, 0))
			{
			case HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP:
				prefs.saveData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false);
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));

				break;
			case HikeConstants.REGISTEM_GCM_AFTER_SIGNUP:
				prefs.saveData(HikeMessengerApp.GCM_ID_SENT, false);
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));

				break;
			}
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId)
	{
		Logger.d(getClass().getSimpleName(), "UNREGISTERED ID: " + regId);
		LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(HikeService.REGISTER_TO_GCM_ACTION));
	}
	
	/**
	 * This runnable to used to get message from GCM route and add to mqtt thread queue, we use mqttHandler to do same
	 * 
	 * Make sure you call {@link #setMessage(Message)} before submitting this runnable
	 * @author gaurav
	 *
	 */
	private class MessageArrivedRunnable implements Runnable
	{
		private String msg;

		public MessageArrivedRunnable(String msg)
		{
			this.msg = msg;
		}

		@Override
		public void run()
		{
			if (msg != null)
			{
				JSONObject json;
				try
				{
					json = new JSONObject(msg);
					MqttMessagesManager.getInstance(getApplicationContext()).saveGCMMessage(json);
					msg = null; // prevents submitting same runnable again and again
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	};
}
