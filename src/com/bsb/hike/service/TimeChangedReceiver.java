package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TimeChangedReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (!Utils.isUserAuthenticated(context))
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "Time has been changed");
		JSONObject jsonObject = new JSONObject();
		try
		
		{
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.REQUEST_SERVER_TIMESTAMP);
			HikeMqttManagerNew.getInstance().sendMessage(jsonObject, HikeMqttManagerNew.MQTT_QOS_ZERO);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), e);
		}
	}

}
