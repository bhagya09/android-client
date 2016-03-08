package com.bsb.hike.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CustomTabActivityHelper
{
	private CustomTabsSession mCustomTabsSession;

	private CustomTabsClient mClient;

	private CustomTabsServiceConnection mConnection;

	private ConnectionCallback mConnectionCallback;

	private static CustomTabActivityHelper activityHelper;

	private String TAG = CustomTabActivityHelper.class.getCanonicalName();

	/**
	 * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView
	 *
	 * @param activity
	 *            The host activity
	 * @param customTabsIntent
	 *            a CustomTabsIntent to be used if Custom Tabs is available
	 * @param uri
	 *            the Uri to be opened
	 * @param fallback
	 *            a CustomTabFallback to be used if Custom Tabs is not available
	 */
	public static void openCustomTab(Activity activity, CustomTabsIntent customTabsIntent, String uri, CustomTabFallback fallback, String title)
	{
		String packageName = CustomTabsHelper.getPackageNameToUse(activity);

		// If we cant find a package name, it means there's no browser that supports
		// Chrome Custom Tabs installed. So, we fallback to the webview
		if (packageName == null)
		{
			if (fallback != null)
			{
				fallback.openUri(uri, title);
			}
		}
		else
		{
			//{"t":"le_android","d":{"et":"nonUiEvent","st":"repl","ep":"HIGH","cts":1456826270480,"tag":"plf","md":{"ek":"micro_app","event":"chromeCustomTabs","fld4":"justOpened","fld1":"http:\/\/timesofindia.indiatimes.com\/business\/india-business\/Day-after-Budget-Sensex-surges-over-450-points\/articleshow\/51204579.cms","sid":1456826226544}}}
			JSONObject json = new JSONObject();
			try
			{
				json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
				json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.CHROME_CUSTOM_TABS);
				json.putOpt(AnalyticsConstants.LOG_FIELD_4,AnalyticsConstants.JUST_OPENED);
				json.putOpt(AnalyticsConstants.LOG_FIELD_1,uri);
			} catch (JSONException e)
			{
				e.printStackTrace();
			}

			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);

			customTabsIntent.intent.setPackage(packageName);
			customTabsIntent.launchUrl(activity, Uri.parse(uri));
		}
	}

	public static CustomTabActivityHelper getInstance()
	{
		synchronized (CustomTabActivityHelper.class)
		{
			if (activityHelper == null)
			{
				synchronized (CustomTabActivityHelper.class)
				{
					activityHelper = new CustomTabActivityHelper();
				}
			}
		}
		return activityHelper;
	}

	/**
	 * Unbinds the Activity from the Custom Tabs Service
	 * 
	 * @param activity
	 *            the activity that is connected to the service
	 */
	public void unbindCustomTabsService(Activity activity)
	{
		mClient = null;
		mCustomTabsSession = null;
		if (mConnection == null)
			return;
		//Fix for AND-4863
		try{
			activity.unbindService(mConnection);
		} catch (RuntimeException ex){
            // exception occurs when service is not registered to activity.
			Logger.e(TAG,ex.getMessage());
		}
	}

	/**
	 * Creates or retrieves an exiting CustomTabsSession
	 *
	 * @return a CustomTabsSession
	 */
	public CustomTabsSession getSession()
	{
		if (mClient == null)
		{
			mCustomTabsSession = null;
		}
		else if (mCustomTabsSession == null)
		{
			mCustomTabsSession = mClient.newSession(new CustomTabsCallback()
			{
				public void onNavigationEvent(int navigationEvent, Bundle extras)
				{
					Logger.d(TAG, "loading other page");
				}
			});
		}
		return mCustomTabsSession;
	}

	/**
	 * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
	 * 
	 * @param connectionCallback
	 */
	public void setConnectionCallback(ConnectionCallback connectionCallback)
	{
		this.mConnectionCallback = connectionCallback;
	}

	/**
	 * Binds the Activity to the Custom Tabs Service
	 * 
	 * @param activity
	 *            the activity to be binded to the service
	 */
	public void bindCustomTabsService(Activity activity)
	{
		if (mClient != null)
			return;

		String packageName = CustomTabsHelper.getPackageNameToUse(activity);
		if (packageName == null)
			return;
		mConnection = new CustomTabsServiceConnection()
		{
			@Override
			public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client)
			{
				mClient = client;
				mClient.warmup(0L);
				if (mConnectionCallback != null)
					mConnectionCallback.onCustomTabsConnected();
				// Initialize a session as soon as possible.
				getSession();
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				mClient = null;
				if (mConnectionCallback != null)
					mConnectionCallback.onCustomTabsDisconnected();
			}
		};
		CustomTabsClient.bindCustomTabsService(activity, packageName, mConnection);

	}

	/**
	 * A Callback for when the service is connected or disconnected. Use those callbacks to handle UI changes when the service is connected or disconnected
	 */
	public interface ConnectionCallback
	{
		/**
		 * Called when the service is connected
		 */
		void onCustomTabsConnected();

		/**
		 * Called when the service is disconnected
		 */
		void onCustomTabsDisconnected();
	}

	/**
	 * To be used as a fallback to open the Uri when Custom Tabs is not available
	 */
	public interface CustomTabFallback
	{
		/**
		 *
		 * @param uri
		 *            The uri to be opened by the fallback
		 */
		void openUri(String url, String title);
	}

}
