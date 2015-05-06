package com.bsb.hike.platform.bridge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class FullScreenJavascriptBridge extends JavascriptBridge
{
	
	private String msisdn;

	public FullScreenJavascriptBridge(WebView mWebView, Activity activity)
	{
		super(activity, mWebView);
	}
	
	public FullScreenJavascriptBridge(WebView mWebView, Activity activity, String msisdn)
	{
		this(mWebView, activity);
		this.msisdn = msisdn;
	}

	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{

	}

	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{

	}

	@JavascriptInterface
	public void openHikeActivity(final String metaData)
	{
		openActivity(metaData);
	}

	/**
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is not present in the helper data.
	 * 
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{

	}

	/**
	 * Calling this function will initiate forward of the message to a friend or group.
	 * 
	 * @param json
	 *            : if the data has changed , then send the updated fields and it will update the metadata. If the key is already present, it will be replaced else it will be added
	 *            to the existent metadata. If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already
	 *            present and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String json)
	{

	}
	
	/**
	 * calling this method will forcefully mute the chat thread. The user won't receive any more
	 * notifications after calling this.
	 */
	@JavascriptInterface
	public void muteChatThread()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT, msisdn);
	}
	

}
