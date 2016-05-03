package com.bsb.hike.platform;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by shobhitmandloi on 27/01/15.
 */
public class CustomWebView extends WebView
{
	public boolean isLoaded = true;

	private boolean isShowing = false;

	private boolean isDestroyed = false;

	private static boolean applyWhiteScreenFix = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WHITE_SCREEN_FIX, true);

	private String javaScriptInterface;

	private static final Method ON_PAUSE_METHOD = findOnPauseMethod();

	private static final Method ON_RESUME_METHOD = findOnResumeMethod();
	private Handler mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
	private Runnable postJSRunnable;
	// Custom WebView to stop background calls when moves out of view.
	public CustomWebView(Context context)
	{
		this((applyWhiteScreenFix ? context.getApplicationContext() : context), null);
	}

	public CustomWebView(Context context, AttributeSet attrs)
	{
		this((applyWhiteScreenFix ? context.getApplicationContext() : context), attrs, android.R.attr.webViewStyle);
	}

	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super((applyWhiteScreenFix ? context.getApplicationContext() : context), attrs, defStyleAttr);
		allowUniversalAccess();
		webViewProperties();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super((applyWhiteScreenFix ? context.getApplicationContext() : context), attrs, defStyleAttr, defStyleRes);
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
		if (applyWhiteScreenFix)  //If applyWhite screen fix pref is set, we use that, else we use the default in market behaviour
		{
			if (!isDestroyed)
			{
				getSettings().setJavaScriptEnabled(false);

				if (Utils.isKitkatOrHigher())
				{
					setWebViewClient(null);
				}
				else
				{
					// Giving a dummy object to avoid
					// android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag
					setWebViewClient(new WebViewClient());
				}
				setWebChromeClient(null);
				isDestroyed = true;
			}
		}

		if (!isDestroyed)
		{
			if (Utils.isHoneycombOrHigher())
			{
				removeJavascriptInterface(javaScriptInterface);
			}
			isDestroyed = true;
		}
		if (mHandler != null && postJSRunnable != null)
		{
			mHandler.removeCallbacks(postJSRunnable);
		}
		mHandler = null;
		postJSRunnable = null;
		stopLoading();
		removeAllViews();
		clearHistory();
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

	public void loadMicroAppData(String data) {
		this.loadDataWithBaseURL("", data, "text/html", "UTF-8", "");
	}

	@Override
	public void addJavascriptInterface(Object obj, String interfaceName)
	{
		this.javaScriptInterface = interfaceName;
		super.addJavascriptInterface(obj, interfaceName);
	}


	@Override
	public void loadUrl(final String url)
	{
		if (mHandler == null)
		{
			return;
		}

		postJSRunnable = new Runnable() {
			@Override
			public void run()
			{
				if (Utils.isKitkatOrHigher() && url.startsWith("javascript"))
				{
					try
					{
						evaluateJavascript(Utils.appendTokenInURL(url), null);
					}
					catch (IllegalStateException e)
					{

						CustomWebView.super.loadUrl(Utils.appendTokenInURL(url)); // On some Custom ROMs and Nokia phones depite being on API greater than 19 this function is not available.This API not supported on Android 4.3 and earlier\n\tat android.webkit.WebViewClassic.evaluateJavaScript(WebViewClassic.java:2674)\n\tat
					}
				}
				else
				{
					CustomWebView.super.loadUrl(Utils.appendTokenInURL(url));
				}
			}
		};
		mHandler.post(postJSRunnable);
	}

	public boolean isWebViewShowing()
	{
		return this.isShowing;
	}

	public boolean isWebViewDestroyed()
	{
		return this.isDestroyed;
	}

	public void onPaused()
	{
		if (!applyWhiteScreenFix) //Default behaviour if pref not set
		{
			this.onPause();
			return;
		}

//		setConfigCallback(null);
		if (ON_PAUSE_METHOD != null)
		{
			try
			{
				ON_PAUSE_METHOD.invoke(this);
			}

			catch (Exception e)
			{
				// Do Nothing
			}
		}

		else
		{
			this.onPause();
		}

	}

	public void onResumed()
	{
		if (!applyWhiteScreenFix)
		{
			this.onResume();  //Default behaviour if pref not set
			return;
		}

		setConfigCallback((WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
		if (ON_RESUME_METHOD != null)
		{
			try
			{
				ON_RESUME_METHOD.invoke(this);
			}

			catch (Exception e)
			{
				// Do Nothing
			}
		}

		else
		{
			this.onResume();
		}

	}

	public void setConfigCallback(WindowManager windowManager)
	{
		if (!applyWhiteScreenFix)
		{
			return;
		}

		try
		{
			Field field = WebView.class.getDeclaredField("mWebViewCore");
			field = field.getType().getDeclaredField("mBrowserFrame");
			field = field.getType().getDeclaredField("sConfigCallback");
			field.setAccessible(true);
			Object configCallback = field.get(null);

			if (null == configCallback)
			{
				return;
			}

			field = field.getType().getDeclaredField("mWindowManager");
			field.setAccessible(true);
			field.set(configCallback, windowManager);
		}
		catch (Exception e)
		{
			// DO NOTHING
		}
	}

	/**
	 * Static method to return the WebView's onPause method, if available
	 *
	 * @return Method for onPause or null
	 */
	@Nullable
	private static Method findOnPauseMethod()
	{
		final Class<WebView> cls = WebView.class;
		try
		{
			return cls.getMethod("onPause");
		}
		catch (Exception e)
		{
			// Nothing
		}
		return null;
	}

	/**
	 * Static method to return the WebView's onResume method, if available
	 *
	 * @return Method for onResume or null
	 */
	@Nullable
	private static Method findOnResumeMethod()
	{
		final Class<WebView> cls = WebView.class;
		try
		{
			return cls.getMethod("onResume");
		}
		catch (Exception e)
		{
			// Nothing
		}
		return null;
	}

	public static void setApplyWhiteScreenFix(boolean enable)
	{
		applyWhiteScreenFix = enable;
	}

	public void clearWebViewCache(boolean includeDiskFiles)
	{
		if (applyWhiteScreenFix)
		{
			this.clearCache(includeDiskFiles);
		}
	}

	public void removeWebViewReferencesFromWebKit() {
		if (Utils.isKitkatOrHigher() || !applyWhiteScreenFix) {
			return;
		}
		try {
			detachAllViewsFromParent();
			Class classWV = Class.forName("android.webkit.WebView");
			Field mProviderField = classWV.getDeclaredField("mProvider");
			Object webViewClassic = getFieldValueSafely(mProviderField, this);
			if (webViewClassic == null) {
				return;
			}
			clearHtml5VideoProxyView(webViewClassic);
			Class classWVCl = Class.forName("android.webkit.WebViewClassic");
			Field webviewCore = classWVCl.getDeclaredField("mWebViewCore");
			Object webViewCoreObj = getFieldValueSafely(webviewCore, webViewClassic);
			if (webViewCoreObj != null) {
				clearDeviceMotionAndOrientationManager(webViewCoreObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearDeviceMotionAndOrientationManager(Object webViewCoreObj) {
		try {
			Class classwvCore = Class.forName("android.webkit.WebViewCore");
			Field deviceMotionField = classwvCore.getDeclaredField("mDeviceMotionAndOrientationManager");
			Object deviceMotionObj = getFieldValueSafely(deviceMotionField, webViewCoreObj);
			if (deviceMotionObj != null) {
				Class classDeviceMotion = Class.forName("android.webkit.DeviceMotionAndOrientationManager");
				Field webviewCore = classDeviceMotion.getDeclaredField("mWebViewCore");
				setFieldValueSafely(webviewCore, deviceMotionObj, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearHtml5VideoProxyView(Object webViewClassic) {
		try {
			Class classWVCl = Class.forName("android.webkit.WebViewClassic");
			Field html5VideoProxyField = classWVCl.getDeclaredField("mHTML5VideoViewProxy");
			Object html5videoproxy = getFieldValueSafely(html5VideoProxyField, webViewClassic);
			if (html5videoproxy != null) {
				//video proxy object is only populated in case of video view  present in the webview full story page.
				Class html5class = Class.forName("android.webkit.HTML5VideoViewProxy");
				Field wvcField = html5class.getDeclaredField("mWebView");
				setFieldValueSafely(wvcField, html5videoproxy, null);
				setFieldValueSafely(html5VideoProxyField, webViewClassic, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Object getFieldValueSafely( Field field, Object classInstance ) throws IllegalArgumentException, IllegalAccessException {
		boolean oldAccessibleValue = field.isAccessible();
		field.setAccessible( true );
		Object result = field.get( classInstance );
		field.setAccessible( oldAccessibleValue );
		return result;
	}

	private void setFieldValueSafely( Field field, Object classInstance, Object value ) throws IllegalArgumentException, IllegalAccessException {
		boolean oldAccessibleValue = field.isAccessible();
		field.setAccessible( true );
		field.set(classInstance, value);
		field.setAccessible( oldAccessibleValue );
	}
}


