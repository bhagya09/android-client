package com.bsb.hike.platform.bridge;

import android.app.Activity;
import android.content.Intent;
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
		Logger.e(tag, "Native Error Not Allowed Methid called : "+methodName);
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
		Logger.e(tag, function+" called but conv message has been updated, message id did not match");
		return false;
	}

	@Override
	@JavascriptInterface
	public void deleteAlarm()
	{
		notAllowedMethodCalled("deleteAlarm");
	}

	@JavascriptInterface
	public void deleteAlarm(String messageId)
	{
		MessagingBotBridgeHelper.deleteAlarm(Integer.parseInt(messageId));
	}

	@Override
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		notAllowedMethodCalled("forwardToChat");
	}
	
	@JavascriptInterface
	public void forwardToChat(String messageId,String json)
	{
		if(isCorrectMessage(messageId,"forwardChat"))
		{
			super.forwardToChat(json);
		}
	}

	@Override
	@JavascriptInterface
	public void getFromCache(String id, String key)
	{
		notAllowedMethodCalled("getFromCache");
	}

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

	@Override
	@JavascriptInterface
	public void putInCache(String key, String value)
	{
		notAllowedMethodCalled("putInCache");
	}
	
	@JavascriptInterface
	public void putInCache(String messageId,String key, String value)
	{
		if(isCorrectMessage(messageId, "putInCache")){
			super.putInCache(key, value);
		}
	}

	@Override
	@JavascriptInterface
	public void putLargeDataInCache(String value)
	{
		notAllowedMethodCalled("putLargeDataInCache");
	}
	
	@JavascriptInterface
	public void putLargeDataInCache(String messageId,String value)
	{
		if(isCorrectMessage(messageId, "putLargeDataInCache")){
			super.putLargeDataInCache(value);
		}
	}

	@Override
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


	@Override
	@JavascriptInterface
	public void onResize(String height)
	{
		notAllowedMethodCalled("onResize");
	}
	
	@JavascriptInterface
	public void onResize(String messageId,String height)
	{
		if(isCorrectMessage(messageId, "onResize")){
			super.onResize(height);
		}	
	}

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
			message.webMetadata = metadata;
		}
	}
	
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
		if(metadata!=null && isCorrectMessage(messageId, "updatemetadata"))
		{
			updateMetadata(metadata, notifyScreen);
		}
	}
}
