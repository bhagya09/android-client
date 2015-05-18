package com.bsb.hike.platform.bridge;

import org.json.JSONException;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class MessagingBotBridgeV1 extends MessagingBotJavaScriptBridge
{

	public MessagingBotBridgeV1(Activity activity, CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity, webView, convMessage, adapter);
	}

	public MessagingBotBridgeV1(Activity activity, CustomWebView mWebView)
	{
		super(activity, mWebView);
	}
	
	/**
	 * call this function to delete the message. The message will get deleted instantaneously
	 */
	@JavascriptInterface
	public void deleteMessage()
	{
		MessagingBotBridgeHelper.deleteMessage(message.getMsgID(), message.getMsisdn(), adapter);
	}
	
	/**
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.inc_unread: if inc_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'inc_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 *
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			if(weakActivity.get()!=null){
				MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), (int)message.getMsgID());
			}
	}

	
	/**
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 *
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID());
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(message.getMsgID(), json);
		if(metadata!=null)
		{
			message.webMetadata = metadata;
		}
	}
	
	/**
	 * calling this function will delete the alarm associated with this javascript.
	 */
	@JavascriptInterface
	public void deleteAlarm()
	{
		MessagingBotBridgeHelper.deleteAlarm((int)message.getMsgID());
	}

	
	/**
	 * Calling this function will update the metadata. If the key is already present, it will be replaced else it will be added to the existent metadata.
	 * If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 * and will add the key-value pair if the key is not present in the existent metadata.
	 *
	 * @param json
	 * @param notifyScreen : if true, the adapter will be notified of the change, else there will be only db update.
	 */
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() + " notifyScren is " + notifyScreen);
		updateMetadata(MessagingBotBridgeHelper.updateMetadata((int)message.getMsgID(), json), notifyScreen);
	}
	
	
	
	

}
