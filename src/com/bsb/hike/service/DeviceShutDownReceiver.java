package com.bsb.hike.service;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceShutDownReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// Send MQTT bg packet only when App is not in Background
		if(!(HikeMessengerApp.currentState == CurrentState.CLOSED
				|| HikeMessengerApp.currentState == CurrentState.BACKGROUNDED))
		{
			//Send "bg" packet to MQTT with QOS '1'
			JSONObject sessionDataObject = HAManager.getInstance().recordAndReturnSessionEnd();
			Utils.sendSessionMQTTPacket(context, HikeConstants.BACKGROUND, sessionDataObject);
		}
	}

}
