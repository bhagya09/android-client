package com.bsb.hike.platform.bridge;

import android.app.Activity;

import com.bsb.hike.platform.CustomWebView;

public class URLJavaScriptBridge extends JavascriptBridge
{

	public URLJavaScriptBridge(Activity activity, CustomWebView mWebView)
	{
		super(activity, mWebView);
	}

	@Override
	protected void logAnalytics(String isUI, String subType, String json)
	{
		
	}

	@Override
	public void onLoadFinished(String height)
	{
		
	}


}
