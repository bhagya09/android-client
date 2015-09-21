/******************************************************************************
 * COPYRIGHT (c) 2010 KeyPoint Technologies (UK) Ltd. All rights reserved.
 *
 * The copyright to the computer program(s) herein is the property of KeyPoint
 * Technologies (UK) Ltd. The program(s) may be used and/or copied only with
 * the written permission from KeyPoint Technologies (UK) Ltd or in accordance
 * with the terms and conditions stipulated in the agreement/contract under
 * which the program(s) have been supplied.
 */
/**
 * @file    KPTPackageListener.java
 *
 * @brief   The broadcast receiver which listens for package installation and 
 * 			uninstalltion events from framework and informs Package handler.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.packageinstaller;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.settings.AddonDownloadActivity;
import com.kpt.adaptxt.beta.settings.KPTAddonManager;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;

/**
 * The broadcast receiver which listens for package installation and 
 * uninstalltion events from framework and informs Package handler.
 * @author 
 *
 */
public class KPTPackageListener extends BroadcastReceiver {
	public static final String TAG = "KPTPackageListener"; 
	/**
	 * All Adaptxt addons must start with same package name
	 */
	public static final String sAddonPackageName = "com.kpt.adaptxt.reliance.addon.";
	public static final String sProPackageName = "com.kpt.adaptxt.beta";
	
	/**
	 * 
	 */
	//public static final String sThemeAddonPackageName = "com.kpt.adaptxt.addon.theme.external.";
	
	/**
	 * Called by the framework whenever a package (.apk) is installed in the phone.  
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		
		
		if (null == intent) {
			Log.e("KPT", " Strange KPTpackageListner recieved null intent from HIKE ");
		}
		
		String actionStr = intent.getAction();
		
		//Log.e("KPT", " KPTpackageListner recieved action from Hike action is  ? "+actionStr);
		
		/*
		String actionStr = intent.getAction();
		if (Intent.ACTION_PACKAGE_ADDED.equalsIgnoreCase(actionStr)) {
			String packageName = intent.getData().getEncodedSchemeSpecificPart();
			if(intent.getDataString().contains(sAddonPackageName) && 
					!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && !intent.getDataString().contains(KPTAdaptxtTheme.THEME_PACKAGE_NAME)) {
				// Send an intent to the package handler service specifying
				// the package name to be installed. 
				displayToast(context);
				packageName = intent.getData().getEncodedSchemeSpecificPart();
				Intent installAddOn = new Intent(context,KPTPackageHandlerService.class);
				installAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_INSTALL);
				installAddOn.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				installAddOn.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(installAddOn);
				
			}
			if (packageName.contains(KPTAdaptxtTheme.THEME_PACKAGE_NAME)){
				Intent themeInstalled = new Intent(context,KPTPkgHandlerService.class);
				themeInstalled.setAction(KPTConstants.KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED);
				themeInstalled.putExtra(KPTConstants.PACKAGE_NAME, packageName);
				themeInstalled.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(themeInstalled);
			}
			
			
		}
		else if(Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(actionStr)) {
			String packageName = intent.getData().getEncodedSchemeSpecificPart();
			//KPTLog.e("KPTPackageListener PACKAGE REMOVED ", intent.getDataString());
			if(intent.getDataString().contains(sAddonPackageName) && 
					!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				// Send an intent to the package handler service specifying 
				// the package name to be uninstalled. 
				
				String packageName = intent.getData().getEncodedSchemeSpecificPart();
				Intent unInstallAddOn = new Intent(context,KPTPackageHandlerService.class);
				unInstallAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_UNINSTALL);
				unInstallAddOn.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				unInstallAddOn.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(unInstallAddOn);
			}
			if(packageName.contains(KPTAdaptxtTheme.THEME_PACKAGE_NAME)){
				Intent themeAddonUninstalled = new Intent (context, KPTPackageHandlerService.class); 
				themeAddonUninstalled.setAction(KPTPackageHandlerService.KPT_ACTION_BROADCAST_THEME_ADDON_REMOVED); 
				themeAddonUninstalled.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				themeAddonUninstalled.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(themeAddonUninstalled);
			}
		} else if(Intent.ACTION_PACKAGE_REPLACED.equalsIgnoreCase(actionStr)) {
			if(intent.getDataString().contains(sAddonPackageName)) {
				String packageName = intent.getData().getEncodedSchemeSpecificPart();
				Intent upgradeAddOn = new Intent(context,KPTPackageHandlerService.class);
				upgradeAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_UPGRADE);
				upgradeAddOn.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				upgradeAddOn.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(upgradeAddOn);
			} else if(intent.getDataString().contains(context.getPackageName())) {
				//PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KPTConstants.PREF_BASE_UPGRADE_PREV_ADDON_VERSION, true).commit();
				//KPTLog.e("KPTTEST", "Base Upgraded");
				
				File compatibilityXML = new File(context.getFilesDir().getAbsolutePath()+"/" + AddonDownloadActivity.S3_COMPATABILITY_XML);
				
				if(compatibilityXML.exists()) {
					compatibilityXML.delete();
				}
				
				
				PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(KPTConstants.PREF_CURRENT_COMPATIBILITY_XML_VERSION, 0).commit();
				PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(KPTConstants.PREF_COMPATIBILITY_XML_VER_LAST_CHECK_TIME, -1).commit();
				
				Intent baseUpgraded = new Intent(context,KPTPkgHandlerService.class);
				baseUpgraded.setAction(KPTConstants.KPT_ACTION_PACKAGE_BASE_UPGRADE);
				baseUpgraded.setFlags(Intent.FLAG_FROM_BACKGROUND);
				context.startService(baseUpgraded);
				
				//UnComment from next official market release
				//Also send the info to Analytics Server
				ServerTaskInfo servertaskInfo = new ServerTaskInfo(ServerConstants.METHOD_TYPE_APP_REGISTRATION_UPDATE, null, null, null, null);
				AdaptxtApplication appState =((AdaptxtApplication)context.getApplicationContext());
				appState.getServerTaskInfoList().add(servertaskInfo);
				AnalyticsServerTask analyticServertask = appState.getAnalyticServerTask();
				if(analyticServertask == null ) {
					analyticServertask = new AnalyticsServerTask(context);
					appState.setAnalyticServerTask(analyticServertask);
					analyticServertask.execute(null,null);
				} 
				MobileAppTracker mobileAppTracker = new MobileAppTracker(context, ServerConstants.MAT_ADVERTISER_ID,  ServerConstants.MAT_KEY);
				if(getResources().getBoolean(R.bool.kpt_enable_mat)) {
					mobileAppTracker.trackUpdate();
				}
			}
		}
		else {
			//KPTLog.e("KPTPackageListener Misc Action Received", intent.getAction());
		}
	*/}
	
	/*private void displayToast(Context context) {

		//int duration = Toast.LENGTH_LONG;
		//String message = String.format(installStatus, pkgName);
		CharSequence contentTitle = context.getResources().getText(R.string.kpt_UI_STRING_ATRIM_ITEM1);
		CharSequence contentText = context.getResources().getText(R.string.kpt_UI_STRING_INAPP_NOTIFICATION_MESSAGE_1);
		NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification notifyDetails = new Notification(R.drawable.notification_icon,
				contentText,System.currentTimeMillis());
		
		Intent notifyIntent = new Intent(context,KPTAddonManager.class);
		PendingIntent pIntent = PendingIntent.getActivity(context, 
				0, notifyIntent, notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
		if(pIntent==null) {
			//KPTLog.e("PINTNT NULL", "");
		}
		notifyDetails.setLatestEventInfo(context, contentTitle, contentText, pIntent);
		//notifyDetails.defaults = Notification.FLAG_AUTO_CANCEL;
		nManager.notify(KPTConstants.SIMPLE_NOTFICATION_ID, notifyDetails);

		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	
	}*/
}
