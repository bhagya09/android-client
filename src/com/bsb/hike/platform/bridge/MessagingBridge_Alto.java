package com.bsb.hike.platform.bridge;


import android.app.Activity;
import android.content.Intent;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This class was introduced to cater platform bridge Platform Bridge Version 1 onwards. We have introduced message id and platform Platform Bridge Version concept here.
 * 
 *  Now all communication between micro app and js bridge is made after message id Platform Bridge Version check
 *  
 *  We share message id with micro app during moustache templating 
 *
 *
 *  Platform Bridge Version Start = 0
 *  Platform Bridge Version End = ~
 */
public class MessagingBridge_Alto extends MessagingBridge_Nano
{

	public MessagingBridge_Alto(Activity activity, CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity, webView, convMessage, adapter);
	}

	public MessagingBridge_Alto(Activity activity, CustomWebView mWebView)
	{
		super(activity, mWebView);
	}
	
	private static final int UPDATE_METDATA = 1201; 
	
	private static final String tag = "MessagingBotBridgev2";
	


	private void notAllowedMethodCalled(String methodName)
	{
		//Logger.e(tag, "Native Error Not Allowed Methid called : "+methodName);
		mWebView.loadUrl("javascript:nativeError('" + methodName + " is not allowed to call in this Platform Bridge Version')");
	}
	
	private boolean isCorrectMessage(String messageId,String function)
	{
		try{
		if(Long.parseLong(messageId) == message.getMsgID())
		{
			return true;
		}
		}catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		Logger.e(tag, function+" called but conv message has been updated, message id did not match, got from card : "+messageId +" and current is "+message.getMsgID());
		return false;
	}


	/**
	 * @deprecated
	 *
	 * Platform Bridge Version 0
	 */
	@Override
	@JavascriptInterface
	public void deleteAlarm()
	{
		notAllowedMethodCalled("deleteAlarm");
	}
	
	/**
	 * Platform Bridge Version 1
	 * calling this function will delete the alarm associated with this javascript.
	 * @param messageId for which you want to delete alarm
	 */
	@JavascriptInterface
	public void deleteAlarm(String messageId)
	{
		MessagingBotBridgeHelper.deleteAlarm(Integer.parseInt(messageId));
	}

	
	/**
	 * @deprecated
	 * Platform Bridge Version 0
	 */
	@Override
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		notAllowedMethodCalled("forwardTochat");
	}
	
	/**
	 * Platform Bridge Version 1
	 *	Calling this function will initiate forward of the message to a friend or group.
	 * @param messageId : to validate whether you are forwarding the proper message
	 * @param json : if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 *             If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 *             and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String messageId,String json)
	{
		if(isCorrectMessage(messageId,"forwardChat"))
		{
			super.forwardToChat(json);
		}
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to get the data from the native memory
	 * @param messageId: to validate whether you are getting cache data for proper message
	 * @param id: the id of the function that native will call to call the js .
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 */
	@JavascriptInterface
	public void getFromCache(String messageId,String id, String key)
	{
		if(isCorrectMessage(messageId, "getfromcache"))
		{
			String value = HikeContentDatabase.getInstance().getFromContentCache(key, message.getNameSpace());
			callbackToJS(id, value);
		}
		
	}

	
	/**
	 * Platform Bridge Version 1
	 * Call this function to get the bulk large data from the native memory
	 * @param messageId: : : to validate whether you are getting bulk cache data for proper message
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getLargeDataFromCache(String messageId,String id)
	{
		if(isCorrectMessage(messageId, "getlargeDataFromCache")){
			String value = HikeContentDatabase.getInstance().getFromContentCache(message.getNameSpace(), message.getNameSpace());
			callbackToJS(id, value);
		}
	}

	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 * @param height : The height of the loaded content
	 */
	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		notAllowedMethodCalled("onLoadFinished");
	}

	/**
	 * Platform Bridge Version 1
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * @param messageId : : : : to validate whether you are calling the function for proper message
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String messageId,String height)
	{
		if(isCorrectMessage(messageId, "onloadfinished")){
			super.onLoadFinished(height);
			if(message.webMetadata.getPlatformJSCompatibleVersion() >= HikePlatformConstants.VERSION_1)
			{
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Logger.i(tag, "inside run onloadfinished "+listener);
						init();
						sendAlarmData();
					}
				});
			}
		}
	}
	
	private void sendAlarmData()
	{
		String alarmData = message.webMetadata.getAlarmData();
		Logger.d(tag, "alarm data to html is " + alarmData);
		if (!TextUtils.isEmpty(alarmData))
		{
			alarmPlayed(alarmData);
		}
			
	}

	
	/**
	 * Platform Bridge Version 1
	 * Call this method to put data in cache. This will be a key-value pair. A microapp can have different key-value pairs
	 * in the native's cache.
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 * @param value: : the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putInCache(String key, String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, message.getNameSpace(), value);
	}

	/**
	 * Platform Bridge Version 1
	 * Call this method to put bulk large data in cache. Earlier large data will be replaced by this new data and there will
	 * be only one entry per microapp.
	 * @param value: the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putLargeDataInCache(String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(message.getNameSpace(), message.getNameSpace(), value);

	}

	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		notAllowedMethodCalled("setAlarm");
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.inc_unread: if inc_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'inc_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 * @param messageId:to validate whether you are setting alarm for proper message
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String messageId,String json, String timeInMills){
		if(weakActivity.get()!=null){
			MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), Integer.parseInt(messageId));
		}
	}


	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void onResize(String height)
	{
		notAllowedMethodCalled("onResize");
	}

	/**
	 * Platform Bridge Version 1
	 * Whenever the content's height is changed, the html will call this function to resize the height of the Android Webview.
	 * Calling this function is MUST, whenever the height of the content changes.
	 * @param messageId : : to validate whether you are calling the function for the proper message
	 * @param height : the new height when the content is reloaded.
	 */
	@JavascriptInterface
	public void onResize(String messageId,String height)
	{
		if (isCorrectMessage(messageId, "onResize"))
		{
			super.onResize(height);
		}
	}
	
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void deleteMessage()
	{
		notAllowedMethodCalled("deleteMessage");
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to delete the message. The message will get deleted instantaneously
	 * @param messageId
	 */
	@JavascriptInterface
	public void deleteMessage(String messageId)
	{
		MessagingBotBridgeHelper.deleteMessage(Long.parseLong(messageId), message.getMsisdn(), adapter);
	}

	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share()
	{
		notAllowedMethodCalled("share");
	}

	/**
	 * Platform Bridge Version 1
	 * @param messageId
	 */
	@JavascriptInterface
	public void share(String messageId)
	{
		if(isCorrectMessage(messageId, "share"))
		{
			super.share();
		}
	}
	
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share(String text, String caption)
	{
		notAllowedMethodCalled("share with params");
	}

	/**
	 * Platform Bridge Version 1
	 * calling this function will share the screenshot of the webView along with the text at the top and a caption text
	 * to all social network platforms by calling the system's intent.
	 * @param messageId : to validate whether sharing for proper message
	 * @param text : heading of the image with the webView's screenshot.
	 * @param caption : intent caption
	 */
	@JavascriptInterface
	public void share(String messageId,String text, String caption)
	{
		if(isCorrectMessage(messageId, "shareWithparams"))
		{
			super.share(text, caption);
		}
	}


	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 * @param json :
	 */
	@Override
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		notAllowedMethodCalled("updateHelperData");
	}

	/**
	 * Platform Bridge Version 1
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 * @param messageId : the message id to validate whether updating helper data for proper message.
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String messageId,String json)
	{
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(Long.parseLong(messageId), json);
		if(metadata!=null)
		{
			sendMessageToUiThread(UPDATE_METDATA, Integer.parseInt(messageId), metadata);
		}
	}
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		notAllowedMethodCalled("updateMetadata");
	}

	/**
	 * Platform Bridge Version 1
	 * @param messageId
	 * @param json
	 * @param notifyScreen
	 */
	@JavascriptInterface
	public void updateMetadata(String messageId,String json, String notifyScreen)
	{
		WebMetadata metadata = MessagingBotBridgeHelper.updateMetadata(Integer.parseInt(messageId), json);
		if(metadata!=null)
		{
			sendMessageToUiThread(UPDATE_METDATA, Integer.parseInt(messageId), metadata);
		}
	}
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		notAllowedMethodCalled("loganalytics");
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to log analytics events.
	 * @param messageId: : to validate whether you are logging for proper message
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */

	@JavascriptInterface
	public void logAnalytics(String messageId,String isUI, String subType, String json)
	{
		if(isCorrectMessage(messageId, "loganalytics"))
		{
			super.logAnalytics(isUI, subType, json);
		}
	}
	
	@Override
	protected void handleUiMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_METDATA:
			if(msg.arg1 == message.getMsgID())
			{
				this.message.webMetadata = (WebMetadata) msg.obj;
			}else{
				metadataMap.put(msg.arg1, (WebMetadata) msg.obj);
				Logger.e(tag, "update metadata called but message id is different, called with "+msg.arg1 + " and current is "+message.getMsgID());
			}
			break;
		default:
			super.handleUiMessage(msg);
		}
	}
	
	
	/**
	 * Platform Version 2 called by the special packet sent in the bot to delete the conversation of the particular bot
	 */
	@JavascriptInterface
	public void deleteBotConversation()
	{
		Logger.i(tag, "delete bot conversation and removing from conversation fragment");
		final Activity context = weakActivity.get();
		ConversationsAdapter.removeBotMsisdn = message.getMsisdn();
		final Intent intent = Utils.getHomeActivityIntent(context);
		mHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				context.startActivity(intent);
			}
		});
	}

}
