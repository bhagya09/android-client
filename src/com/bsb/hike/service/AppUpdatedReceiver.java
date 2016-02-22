package com.bsb.hike.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.kpt.adaptxt.beta.util.KPTConstants;

/**
 * @author Rishabh This receiver is used to notify that the app has been updated.
 */
public class AppUpdatedReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(final Context context, Intent intent)
	{
		if (context.getPackageName().equals(intent.getData().getSchemeSpecificPart()))
		{
			Logger.d(getClass().getSimpleName(), "App has been updated");

			final HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();


			Intent intentKpt = new Intent();
			intentKpt.setAction(KPTConstants.ACTION_BASE_PACKAGE_REPLACED);
			context.sendBroadcast(intentKpt);
			
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
			if (!Utils.isUpdateRequired(prefs.getData(HikeConstants.Extras.LATEST_VERSION, ""), context))
			{
				prefs.removeData(HikeConstants.Extras.UPDATE_AVAILABLE);
				prefs.removeData(HikeConstants.Extras.SHOW_UPDATE_OVERLAY);
				prefs.removeData(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP);
				prefs.removeData(HikeConstants.Extras.UPDATE_TO_IGNORE);
				prefs.removeData(HikeConstants.Extras.LATEST_VERSION);
				prefs.removeData(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
				prefs.removeData(HikeConstants.Extras.URL);

				//Removing Upgrade tips, persistent notifications
				Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Flushing update tips and persistent notifs.");
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOULD_SHOW_PERSISTENT_NOTIF, false);
				HikeNotification.getInstance().cancelPersistNotif();
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_NORMAL_UPDATE_TIP, false);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.UPDATE_CRITICAL_TIP);
			}
			/*
			 * This will happen for builds older than 1.1.15
			 */
			if (!prefs.contains(HikeMessengerApp.COUNTRY_CODE) && prefs.getData(HikeMessengerApp.MSISDN_SETTING, "").startsWith(HikeConstants.INDIA_COUNTRY_CODE))
			{
				prefs.saveData(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE);
				HikeMessengerApp.getPubSub().publish(HikePubSub.REFRESH_RECENTS, null);
			}

			SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (!appPrefs.contains(HikeConstants.FREE_SMS_PREF))
			{
				Editor editor = appPrefs.edit();
				boolean freeSMSOn = prefs.getData(HikeMessengerApp.COUNTRY_CODE, "").equals(HikeConstants.INDIA_COUNTRY_CODE);
				prefs.saveData(HikeConstants.FREE_SMS_PREF, freeSMSOn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, freeSMSOn);
			}

		}
	}
}