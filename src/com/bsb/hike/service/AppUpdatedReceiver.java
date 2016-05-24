package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.filetransfer.FTApkManager;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author Rishabh This receiver is used to notify that the app has been updated.
 */
public class AppUpdatedReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(final Context context, Intent intent)
	{
		Logger.d("AUTOAPK","this thing should get called when update is complete");
		if (context.getPackageName().equals(intent.getData().getSchemeSpecificPart()))
		{
			Logger.d(getClass().getSimpleName(), "App has been updated");

			final SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

			/*
			 * If the user has not signed up yet, don't do anything.
			 */
			if (!Utils.isUserAuthenticated(context))
			{
				return;
			}

			HAManager.getInstance().logUserGoogleAccounts();
			/*
			 * Checking if the current version is the latest version. If it is we reset the preference which prompts the user to update the app.
			 */


			if (!Utils.isUpdateRequired(prefs.getString(HikeConstants.Extras.LATEST_VERSION, ""), context))
			{
				Logger.d("AUTOAPK", "LATEST_VERSION code being executed");
				Editor editor = prefs.edit();
				editor.remove(HikeConstants.Extras.UPDATE_AVAILABLE);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_OVERLAY);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP);
				editor.remove(HikeConstants.Extras.UPDATE_TO_IGNORE);
				editor.remove(HikeConstants.Extras.LATEST_VERSION);
				editor.remove(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
				editor.remove(HikeConstants.Extras.URL);
				editor.commit();
				
				//Removing Upgrade tips, persistent notifications
				Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Flushing update tips and persistent notifs.");
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOULD_SHOW_PERSISTENT_NOTIF, false);
				HikeNotification.getInstance().cancelPersistNotif();
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.UPDATE_CRITICAL_TIP);

			}
			FTApkManager.checkUpdateSuccess(prefs);
			/*
			 * This will happen for builds older than 1.1.15
			 */
			if (!prefs.contains(HikeMessengerApp.COUNTRY_CODE) && prefs.getString(HikeMessengerApp.MSISDN_SETTING, "").startsWith(HikeConstants.INDIA_COUNTRY_CODE))
			{
				Editor editor = prefs.edit();
				editor.putString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE);
				editor.commit();
				HikeMessengerApp.getPubSub().publish(HikePubSub.REFRESH_RECENTS, null);
			}

			SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (!appPrefs.contains(HikeConstants.FREE_SMS_PREF))
			{
				Editor editor = appPrefs.edit();
				boolean freeSMSOn = prefs.getString(HikeMessengerApp.COUNTRY_CODE, "").equals(HikeConstants.INDIA_COUNTRY_CODE);
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, freeSMSOn);
				editor.commit();
				HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, freeSMSOn);
			}

            PlatformUtils.platformDiskConsumptionAnalytics(AnalyticsConstants.APP_UPDATE_TRIGGER);
		}
	}

}
