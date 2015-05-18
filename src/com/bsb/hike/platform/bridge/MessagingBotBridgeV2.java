package com.bsb.hike.platform.bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.utils.Logger;

/**
 * This Class is made to support MicroApps based on PlatformJS version 2, In this version we introduced messageId restriction
 * 
 * We accept messageId and update database accordingly, version before than this had a bug of updating database of wrong message because of thread communication
 * 
 * All these functions are executed in JavaBridge Thread  
 * 
 */
public class MessagingBotBridgeV2 extends MessagingBotJavaScriptBridge
{
	private static final int UPDATE_METDATA = 1201; 
	
	private static final String tag = "MessagingBotBridgev2";
	

	public MessagingBotBridgeV2(Activity activity, CustomWebView mWebView)
	{
		super(activity, mWebView);
	}

	public MessagingBotBridgeV2(Activity activity, CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity, webView, convMessage, adapter);
	}

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
	 * 
	 * @param messageId for which you want to delete alarm
	 */
	@JavascriptInterface
	public void deleteAlarm(String messageId)
	{
		MessagingBotBridgeHelper.deleteAlarm(Integer.parseInt(messageId));
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
	 */
	@JavascriptInterface
	public void getFromCache(String messageId,String id, String key)
	{
		if(isCorrectMessage(messageId, "getfromcache"))
		{
			super.getFromCache(id, key);
		}
		
	}

	@Override
	@JavascriptInterface
	public void getLargeDataFromCache(String id)
	{
		notAllowedMethodCalled("getLargeDataFromCache");
	}
	
	@JavascriptInterface
	public void getLargeDataFromCache(String messageId,String id)
	{
		if(isCorrectMessage(messageId, "getlargeDataFromCache")){
			super.getLargeDataFromCache(id);
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
		}
	}

	
	@JavascriptInterface
	public void putInCache(String messageId,String key, String value)
	{
		if(isCorrectMessage(messageId, "putInCache")){
			super.putInCache(key, value);
		}
	}

	
	@JavascriptInterface
	public void putLargeDataInCache(String messageId,String value)
	{
		if(isCorrectMessage(messageId, "putLargeDataInCache")){
			super.putLargeDataInCache(value);
		}
	}

	
	@JavascriptInterface
	public void setAlarm(String mId,String json, String timeInMills){
		if(weakActivity.get()!=null){
			MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), Integer.parseInt(mId));
		}
	}


	@JavascriptInterface
	public void onResize(String messageId,String height)
	{
		if (isCorrectMessage(messageId, "onResize"))
		{
			super.onResize(height);
		}
	}
	
	
	@JavascriptInterface
	public void deleteMessage(String messageId)
	{
		MessagingBotBridgeHelper.deleteMessage(Long.parseLong(messageId), message.getMsisdn(), adapter);
	}

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


	@JavascriptInterface
	public void updateHelperData(String messageId,String json)
	{
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(Long.parseLong(messageId), json);
		if(metadata!=null)
		{
			sendMessageToUiThread(UPDATE_METDATA, Integer.parseInt(messageId), metadata);
		}
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
