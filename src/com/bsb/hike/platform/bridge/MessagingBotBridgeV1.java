package com.bsb.hike.platform.bridge;

import android.app.Activity;
import android.widget.BaseAdapter;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;

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

}
