package com.bsb.hike.platform.bridge;

import org.json.JSONException;

import android.app.Activity;
import android.os.Message;
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


/**
 * This class was introduced to cater platform bridge version 1 onwards. We have introduced message id and platform version concept here.
 * 
 *  Now all communication between micro app and js bridge is made after message id version check
 *  
 *  We share message id with micro app during moustache templating 
 *
 *
 *  @platformBridgeStart=0
 *  @platformBridgeEnd= ~
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
		mWebView.loadUrl("javascript:nativeError('" + methodName + " is not allowed to call in this version')");
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
	 */
	@Override
	@JavascriptInterface
	public void deleteAlarm()
	{
		notAllowedMethodCalled("deleteAlarm");
	}
	
	/**
	 * 
	 * @param messageId for which you want to delete alarm
	 */
	@JavascriptInterface
	public void deleteAlarm(String messageId)
	{
		MessagingBotBridgeHelper.deleteAlarm(Integer.parseInt(messageId));
	}

	
	/**
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		notAllowedMethodCalled("forwardTochat");
	}
	
	/**
	 * 
	 * @param messageId : to validate whether you are forwarding proper message
	 * @param json
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
	 * 
	 * @param messageId 
	 * @param id
	 * @param key
	 * 
	 * call this function to get the data from the native memory
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
	 * Call this function to get the bulk large data from the native memory
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
	

	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		notAllowedMethodCalled("onLoadFinished");
	}
	
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
	 * Call this method to put data in cache. This will be a key-value pair. A microapp can have different key-value pairs
	 * in the native's cache.
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 * @param value: : the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putInCache(String messageId,String key, String value)
	{
		if(isCorrectMessage(messageId, "putInCache")){
			HikeContentDatabase.getInstance().putInContentCache(key, message.getNameSpace(), value);
		}
	}

	/**
	 * Call this method to put bulk large data in cache. Earlier large data will be replaced by this new data and there will
	 * be only one entry per microapp.
	 * @param value: the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putLargeDataInCache(String messageId,String value)
	{
		if(isCorrectMessage(messageId, "putLargeDataInCache")){
			HikeContentDatabase.getInstance().putInContentCache(message.getNameSpace(), message.getNameSpace(), value);
		}
	}

	/**
	 * @deprecated
	 * @param json
	 * @param timeInMills
	 */
	@Override
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		notAllowedMethodCalled("setAlarm");
	}
	
	@JavascriptInterface
	public void setAlarm(String mId,String json, String timeInMills){
		if(weakActivity.get()!=null){
			MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), Integer.parseInt(mId));
		}
	}


	/**
	 * @deprecated
	 * @param height
	 */
	@Override
	@JavascriptInterface
	public void onResize(String height)
	{
		notAllowedMethodCalled("onResize");
	}
	
	@JavascriptInterface
	public void onResize(String messageId,String height)
	{
		if (isCorrectMessage(messageId, "onResize"))
		{
			super.onResize(height);
		}
	}
	
	
	/**
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void deleteMessage()
	{
		notAllowedMethodCalled("deleteMessage");
	}
	
	@JavascriptInterface
	public void deleteMessage(String messageId)
	{
		MessagingBotBridgeHelper.deleteMessage(Long.parseLong(messageId), message.getMsisdn(), adapter);
	}

	/**
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share()
	{
		notAllowedMethodCalled("share");
	}
	
	@JavascriptInterface
	public void share(String messageId)
	{
		if(isCorrectMessage(messageId, "share"))
		{
			super.share();
		}
	}
	
	
	/**
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share(String text, String caption)
	{
		notAllowedMethodCalled("share with params");
	}
	
	@JavascriptInterface
	public void share(String messageId,String text, String caption)
	{
		if(isCorrectMessage(messageId, "shareWithparams"))
		{
			super.share(text, caption);
		}
	}


	/**
	 * @deprecated
	 * @param json
	 */
	@Override
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		notAllowedMethodCalled("updateHelperData");
	}
	
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
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		notAllowedMethodCalled("updateMetadata");
	}
	
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
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		notAllowedMethodCalled("loganalytics");
	}
	
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

	
	
	
	

}
