package com.bsb.hike.platform;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;

/**
 * Created by shobhitmandloi on 27/01/15.
 */
public class CustomWebView extends WebView
{
	public boolean isLoaded = true;

	private String javaScriptInterface;

	private boolean isShowing = false;

	private boolean isDestroyed = false;

	// Custom WebView to stop background calls when moves out of view.
	public CustomWebView(Context context)
	{
		this(context, null);
	}

	public CustomWebView(Context context, AttributeSet attrs)
	{
		this(context, attrs, android.R.attr.webViewStyle);
	}

	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		allowUniversalAccess();
		webViewProperties();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		allowUniversalAccess();
	}

	// if webView is not visible, call onPause of WebView, else call onResume.
	@Override
	public void onWindowVisibilityChanged(int visibility)
	{
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.INVISIBLE)
		{
			this.isShowing = false;
		}

		else if (visibility == View.GONE)
		{
			this.isShowing = false;
			onWebViewGone();
		}
		else if (visibility == View.VISIBLE)
		{
			onWebViewVisible();
			this.isShowing = true;
		}
	}

	@SuppressLint("NewApi")
	public void onWebViewGone()
	{
		Logger.i("customWebView", "on webview gone " + this.hashCode());
		try
		{
			// we giving callback to javascript to stop heavy processing
			if (isLoaded)
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
		Logger.i("customWebView", "on webview visible " + this.hashCode());
		try
		{
			if (isLoaded)
			{
				this.loadUrl("javascript:onResume()");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@SuppressLint("NewApi")
	public void allowUniversalAccess()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			getSettings().setAllowUniversalAccessFromFileURLs(true);
			getSettings().setAllowFileAccessFromFileURLs(true);

		}
	}

	public void onActivityDestroyed()
	{
		if (!isDestroyed)
		{
			stopLoading();
			removeAllViews();
			if (Utils.isHoneycombOrHigher())
			{
				removeJavascriptInterface(javaScriptInterface);
			}
			isDestroyed = true;
		}
	}

	@Override
	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String failUrl)
	{
		if (!isDestroyed)
		{
			if (Utils.isLollipopOrHigher() && !Utils.appInstalledOrNot(HikeMessengerApp.getInstance().getApplicationContext(), "com.google.android.webview"))
			{
				PlatformUtils.sendPlatformCrashAnalytics("PackageManager.NameNotFoundException");
			}
			
			super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl);
		}
	}

	@Override
	public void loadData(String data, String mimeType, String encoding)
	{
		if(!isDestroyed)
		{
			if (Utils.isLollipopOrHigher() && !Utils.appInstalledOrNot(HikeMessengerApp.getInstance().getApplicationContext(), "com.google.android.webview"))
			{
				PlatformUtils.sendPlatformCrashAnalytics("PackageManager.NameNotFoundException");
			}
			
			super.loadData(data, mimeType, encoding);
		}
	}

	public void webViewProperties()
	{
		setVerticalScrollBarEnabled(false);
		setHorizontalScrollBarEnabled(false);
		getSettings().setJavaScriptEnabled(true);
		enableAppCache();
	}

	private void enableAppCache()
	{
		File cacheDirectoryPath = getContext().getCacheDir();
		if (cacheDirectoryPath != null && cacheDirectoryPath.exists())
		{
			getSettings().setAppCachePath(cacheDirectoryPath.getAbsolutePath());
			getSettings().setAppCacheEnabled(true);
			getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		}
	}

	public void loadMicroAppData(String data)
	{
		this.loadDataWithBaseURL("", data, "text/html", "UTF-8", "");
	}

	@Override
	public void addJavascriptInterface(Object obj, String interfaceName)
	{
		this.javaScriptInterface = interfaceName;
		super.addJavascriptInterface(obj, interfaceName);
	}

	@Override
	public void loadUrl(String url)
	{
		if (android.os.Build.VERSION.SDK_INT > 18 && url.startsWith("javascript"))
		{
			evaluateJavascript(Utils.appendTokenInURL(url), new ValueCallback<String>()
			{
				@Override
				public void onReceiveValue(String value)
				{
					Logger.d("CustomWebView", value);
				}
			});
		}
		else
		{
			super.loadUrl(Utils.appendTokenInURL(url));
		}
	}

	public boolean isWebViewShowing()
	{
		return this.isShowing;
	}
	
	public boolean isWebViewDestroyed()
	{
		return this.isDestroyed;
	}
}


