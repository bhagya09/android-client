package com.bsb.hike.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsService;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Custom Tabs.
 */
public class CustomTabsHelper
{
	private static final String TAG = "CustomTabsHelper";

	static final String STABLE_PACKAGE = "com.android.chrome";

	static final String BETA_PACKAGE = "com.chrome.beta";

	static final String DEV_PACKAGE = "com.chrome.dev";

	static final String LOCAL_PACKAGE = "com.google.android.apps.chrome";

	private static final String EXTRA_CUSTOM_TABS_KEEP_ALIVE = "android.support.customtabs.extra.KEEP_ALIVE";

	private static String sPackageNameToUse;

	private CustomTabsHelper()
	{
	}

	public static void addKeepAliveExtra(Context context, Intent intent)
	{
		Intent keepAliveIntent = new Intent().setClassName(context.getPackageName(), KeepAliveService.class.getCanonicalName());
		intent.putExtra(EXTRA_CUSTOM_TABS_KEEP_ALIVE, keepAliveIntent);
	}

	/**
	 * Goes through all apps that handle VIEW intents and have a warmup service. Picks the one chosen by the user if there is one, otherwise makes a best effort to return a valid
	 * package name.
	 *
	 * This is <strong>not</strong> threadsafe.
	 *
	 * @param context
	 *            {@link Context} to use for accessing {@link PackageManager}.
	 * @return The package name recommended to use for connecting to custom tabs related components.
	 */
	public static String getPackageNameToUse(Context context)
	{
		if (sPackageNameToUse != null)
			return sPackageNameToUse;

		PackageManager pm = context.getPackageManager();
		// Get default VIEW intent handler.
		Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://get.hike.in"));
		ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);
		String defaultViewHandlerPackageName = null;
		if (defaultViewHandlerInfo != null)
		{
			defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
		}

		// Get all apps that can handle VIEW intents.
		List<ResolveInfo> resolvedActivityList;
		//AND-4994
		//In marshmellow, news full story opens in webview instead of chrome custom tabs when default browser set as firefox.
		if(Utils.isMarshmallowOrHigher()){
			resolvedActivityList = pm.queryIntentActivities(activityIntent, HikeConstants.PACKAGE_MANAGER_INTENT_FLAG_MATCH_ALL);
		}else{
			resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
		}

		List<String> packagesSupportingCustomTabs = new ArrayList<>();
		for (ResolveInfo info : resolvedActivityList)
		{
			Intent serviceIntent = new Intent();
			serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
			serviceIntent.setPackage(info.activityInfo.packageName);
			if (pm.resolveService(serviceIntent, 0) != null)
			{
				packagesSupportingCustomTabs.add(info.activityInfo.packageName);
				sendAnalyticsForChromeSupport(true);
			}
		}

		// Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
		// and service calls.
		if (packagesSupportingCustomTabs.isEmpty())
		{
			sPackageNameToUse = null;
			sendAnalyticsForChromeSupport(false);
		}
		else if (packagesSupportingCustomTabs.size() == 1)
		{
			sPackageNameToUse = packagesSupportingCustomTabs.get(0);
		}
		else if (!TextUtils.isEmpty(defaultViewHandlerPackageName) && !hasSpecializedHandlerIntents(context, activityIntent)
				&& packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName))
		{
			sPackageNameToUse = defaultViewHandlerPackageName;
		}
		else if (packagesSupportingCustomTabs.contains(STABLE_PACKAGE))
		{
			sPackageNameToUse = STABLE_PACKAGE;
		}
		else if (packagesSupportingCustomTabs.contains(BETA_PACKAGE))
		{
			sPackageNameToUse = BETA_PACKAGE;
		}
		else if (packagesSupportingCustomTabs.contains(DEV_PACKAGE))
		{
			sPackageNameToUse = DEV_PACKAGE;
		}
		else if (packagesSupportingCustomTabs.contains(LOCAL_PACKAGE))
		{
			sPackageNameToUse = LOCAL_PACKAGE;
		}
		return sPackageNameToUse;
	}


	private static void sendAnalyticsForChromeSupport(boolean supported){
		//{"t":"le_android","d":{"et":"nonUiEvent","st":"repl","ep":"HIGH","cts":1456826270480,"tag":"plf","md":{"ek":"micro_app","event":"chromeCustomTabs","fld4":"chromeTabsUnSupported/chromeTabsSupported","sid":1456826226544}}}
		JSONObject json = new JSONObject();
		try
		{
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.CHROME_CUSTOM_TABS);
			json.putOpt(AnalyticsConstants.LOG_FIELD_4,supported?AnalyticsConstants.CHROME_TABS_SUPPORTED:AnalyticsConstants.CHROME_TABS_UNSUPPORTED);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
	}
	/**
	 * Used to check whether there is a specialized handler for a given intent.
	 * 
	 * @param intent
	 *            The intent to check with.
	 * @return Whether there is a specialized handler for the given intent.
	 */
	private static boolean hasSpecializedHandlerIntents(Context context, Intent intent)
	{
		try
		{
			PackageManager pm = context.getPackageManager();
			List<ResolveInfo> handlers = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
			if (handlers == null || handlers.size() == 0)
			{
				return false;
			}
			for (ResolveInfo resolveInfo : handlers)
			{
				IntentFilter filter = resolveInfo.filter;
				if (filter == null)
					continue;
				if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0)
					continue;
				if (resolveInfo.activityInfo == null)
					continue;
				return true;
			}
		}
		catch (RuntimeException e)
		{
			Log.e(TAG, "Runtime exception while getting specialized handlers");
		}
		return false;
	}

	/**
	 * @return All possible chrome package names that provide custom tabs feature.
	 */
	public static String[] getPackages()
	{
		return new String[] { "", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE };
	}
}
