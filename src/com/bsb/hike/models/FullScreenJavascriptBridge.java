package com.bsb.hike.models;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.bridge.JavascriptBridge;

public class FullScreenJavascriptBridge extends JavascriptBridge
{

	public FullScreenJavascriptBridge(CustomWebView mWebView, Activity activity)
	{
		super(activity, mWebView);

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

}
