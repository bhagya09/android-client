package com.bsb.hike.platform;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

/**
 * Created by shobhitmandloi on 27/01/15.
 */
public class CustomWebView extends WebView
{
	public boolean isLoaded = true;
	
	//Custom WebView to stop background calls when moves out of view.
	public CustomWebView(Context context)
	{
		super(context);
	}

	public CustomWebView(Context context, AttributeSet attrs)
	{
		this(context, attrs, android.R.attr.webViewStyle);
	}

	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
	}


	// if webView is not visible, call onPause of WebView, else call onResume.
	@Override
	public void onWindowVisibilityChanged(int visibility)
	{
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.GONE)
		{
			onWebViewGone();
		}
		else if (visibility == View.VISIBLE)
		{
			onWebViewVisible();
		}
	}
	
	@SuppressLint("NewApi")
	public void onWebViewGone()
	{
		Logger.i("customWebView", "on webview gone "+this.hashCode());
		try
		{
			// we giving callback to javascript to stop heavy processing
			if(isLoaded)
			{
				this.loadUrl("javascript:onPause()");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressLint("NewApi")
	public void onWebViewVisible()
	{
		Logger.i("customWebView", "on webview visible "+this.hashCode());
		try
		{
			if(isLoaded)
			{
				this.loadUrl("javascript:onResume()");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
