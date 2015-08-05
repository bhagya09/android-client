/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	static final String TAG = "AlarmPingSender";

	// TODO: Add log.
	private ClientComms comms;
	private HikeMessengerApp app;
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;
	private volatile boolean hasStarted = false;

	public AlarmPingSender(HikeMessengerApp app) {
		if (app == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.app = app;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		String action = MqttConstants.PING_SENDER
				+ comms.getClient().getClientId();
		Logger.d(TAG, "Register alarmreceiver to MqttService"+ action);
		app.registerReceiver(alarmReceiver, new IntentFilter(action));

		pendingIntent = PendingIntent.getBroadcast(app, 0, new Intent(
				action), PendingIntent.FLAG_UPDATE_CURRENT);
		
		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {
		// Cancel Alarm.
		AlarmManager alarmManager = (AlarmManager) app
				.getSystemService(Service.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);

		Logger.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
		if(hasStarted){
			hasStarted = false;
			try{
				app.unregisterReceiver(alarmReceiver);
			}catch(IllegalArgumentException e){
				//Ignore unregister errors.			
			}
		}
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		Logger.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		AlarmManager alarmManager = (AlarmManager) app
				.getSystemService(Service.ALARM_SERVICE);
		if (Utils.isKitkatOrHigher())
		{
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		}
		else
		{
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		}
	}

	/*
	 * This class sends PingReq packet to MQTT broker
	 */
	class AlarmReceiver extends BroadcastReceiver {
		private WakeLock wakelock;
		private String wakeLockTag = MqttConstants.PING_WAKELOCK
				+ that.comms.getClient().getClientId();

		@Override
		public void onReceive(Context context, Intent intent) {
			// According to the docs, "Alarm Manager holds a CPU wake lock as
			// long as the alarm receiver's onReceive() method is executing.
			// This guarantees that the phone will not sleep until you have
			// finished handling the broadcast.", but this class still get
			// a wake lock to wait for ping finished.
			Logger.d(TAG, "Check time :" + System.currentTimeMillis());
			IMqttToken token = comms.checkForActivity();

			// No ping has been sent.
			if (token == null) {
				return;
			}

			// Assign new callback to token to execute code after PingResq
			// arrives. Get another wakelock even receiver already has one,
			// release it until ping response returns.
			int pingWackLockTimeout = HikeSharedPreferenceUtil.getInstance().getData(MqttConstants.ALARM_PING_WAKELOCK_TIMEOUT, (MqttConstants.DEFAULT_PING_WAKELOCK_TIMEOUT_IN_SECONDS * 1000));
			if(pingWackLockTimeout > 0)
			{
				if (wakelock == null)
				{
					PowerManager pm = (PowerManager) app.getSystemService(Service.POWER_SERVICE);
					wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
					wakelock.setReferenceCounted(false);
				}
				wakelock.acquire(pingWackLockTimeout);
			}
			token.setActionCallback(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Logger.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					if(wakelock != null && wakelock.isHeld()){
						wakelock.release();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken,
						Throwable exception) {
					Logger.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					if(wakelock != null && wakelock.isHeld()){
						wakelock.release();
					}
				}
			});
			
			app.connectToService();
		}
	}
}
