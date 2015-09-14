package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.service.MqttMessagesManager;

/**
 * 
 * @author himanshu This class has all the utility methods to handle packets types involved in Offline Messaging.
 */
public class OfflineMessagesManager
{
	private Context context;
	private static final String TAG = "OfflineThreadManager";

	public OfflineMessagesManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();

	}
	
	public void handleChatThemeMessage(JSONObject messageJSON) throws JSONException
	{
		messageJSON.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
		MqttMessagesManager.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).saveChatBackground(messageJSON);
	}

}
