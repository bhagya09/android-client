package com.bsb.hike.ui.utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

public class LockPattern
{
	public static int mBarMinWiredDots = 4;

	private static int mBarMaxTries = 5;

	char[] encryptedPattern;

	public static void onLockActivityResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
		case HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT:
		case HikeConstants.ResultCodes.CREATE_LOCK_PATTERN:
			/*
			 * Check for case where intent is null
			 */
			if(null == data)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, true);
				break;
			}
			
			boolean isReset = data.getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false);
			if (resultCode == activity.RESULT_OK)
			{
				String encryptedPattern = String.valueOf(data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN));
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, encryptedPattern);
				StealthModeManager.getInstance().setUp(true);
				DBBackupRestore.getInstance(activity).updatePrefs();
				//only firing this event if this is not the password reset flow
				if (!isReset)
				{
					if(requestCode == HikeConstants.ResultCodes.CREATE_LOCK_PATTERN)
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, null);
					else if (requestCode == HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_REVEAL_TIP, null);
						HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, false);
					
					}
					//StealthResetTimer.getInstance().activate(false);
//					HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_ENTER_PASS_TIP, null);
					
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STEALTH_FTUE_DONE);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}
			}
			else
			{
				//making this check so that we can find out if this is password reset flow or otherwise
				if(!isReset)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, true);
				}
			}
			break;// _ReqCreateLockPattern

		case HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT:
		case HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN:
		case HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_CHANGE_PREF:
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				if(requestCode ==  HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN)
				{
					StealthModeManager.getInstance().activate(true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
					HikeAnalyticsEvent.sendStealthEnabled(true);
				}
				else if(requestCode ==  HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, false);
				}
				break;
			case Activity.RESULT_CANCELED:
				if(!(requestCode == HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_CHANGE_PREF))
				{
					if(requestCode == HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN)
					{
						StealthModeManager.getInstance().activate(false);
					}
					else if(requestCode ==  HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, true);
					}	
				}
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ENTER_WRONG_STEALTH_MODE);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				break;
			case LockPatternActivity.RESULT_FAILED:
				if(requestCode ==  HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, true);
				}	
				break;
			default:
				return;
			}

			break;
		case HikeConstants.ResultCodes.CONFIRM_AND_ENTER_NEW_PASSWORD:
			//adding this to handle the case where the user is selecting a new password
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				LockPattern.createNewPattern(activity, true, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
				break;
			case Activity.RESULT_CANCELED:
				break;
			case LockPatternActivity.RESULT_FAILED:
				break;
			default:
				return;
			}
			break;
		}
	}

	/**
	 * Gets the theme that the user chose to apply to {@link LockPatternActivity}.
	 * 
	 * @return the theme for {@link LockPatternActivity}.
	 */
	public static int getThemeForLockPatternActivity()
	{

		return R.style.Alp_42447968_Theme_Dialog_Dark;
	}// getThemeForLockPatternActivity()

	/**
	 * This method creates a new pattern.
	 * @param activity
	 * @param isResetPassword 
	 */
	public static void createNewPattern(Activity activity, boolean isResetPassword, int requestCode)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, activity, LockPatternActivity.class);
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMinWiredDots);
		i.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, isResetPassword);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivityForResult(i, requestCode);
	}// onClick()

	/**
	 * This method validates an existing password
	 * @param activity
	 * @param isResetPassword  
	 */
	private static Intent confirmPatternIntent(Activity activity, boolean isResetPassword, int requestCode)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null, activity, LockPatternActivity.class);
		String encryptedPattern = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, "");
		i.putExtra(LockPatternActivity.EXTRA_PATTERN, encryptedPattern.toCharArray());
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, isResetPassword);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMaxTries);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
		}// onClick()
	
	public static void confirmPattern(Activity activity, boolean isResetPassword, int requestCode, Bundle bundle)
	{
		Intent i = confirmPatternIntent(activity, isResetPassword, requestCode);
		i.putExtra(HikeConstants.STEALTH, bundle);
		activity.startActivityForResult(i, requestCode);
	}
	
	public static void confirmPattern(Activity activity, boolean isResetPassword, int requestCode)
	{
		Intent i = confirmPatternIntent(activity, isResetPassword, requestCode);
		activity.startActivityForResult(i, requestCode);
	}
	

}
