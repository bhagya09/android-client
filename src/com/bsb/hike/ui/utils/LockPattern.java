package com.bsb.hike.ui.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.haibison.android.lockpattern.LockPatternActivity;
import com.haibison.android.lockpattern.util.Settings;

import org.json.JSONException;
import org.json.JSONObject;

public class LockPattern
{
	public static int mBarMinWiredDots = 4;

	private static int mBarMaxTries = 5;

	char[] encryptedPattern;

	public static void onLockActivityResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		boolean isReset = (data != null && data.getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false));
		
		switch (requestCode)
		{
		case HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT:
		case HikeConstants.ResultCodes.CREATE_LOCK_PATTERN:
			/*
			 * Check for case where intent is null
			 */
			if(null == data)
			{
				break;
			}
			
			if (resultCode == Activity.RESULT_OK)
			{
				String encryptedPattern = String.valueOf(data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN));
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, encryptedPattern);
				StealthModeManager.getInstance().setUp(true);
				AccountBackupRestore.getInstance(activity).updatePrefs();
				//only firing this event if this is not the password reset flow
				if (!isReset)
				{
					if(requestCode == HikeConstants.ResultCodes.CREATE_LOCK_PATTERN)
						StealthModeManager.getInstance().setTipVisibility(true,  ConversationTip.STEALTH_FTUE_TIP);
					else if (requestCode == HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT)
					{
						StealthModeManager.getInstance().setTipVisibility(true,  ConversationTip.STEALTH_REVEAL_TIP);
						markStealthMsisdn(data.getExtras());
					}
					
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

					//sending server mqtt packet on enabling stealth mode
					HikeAnalyticsEvent.sendStealthEnabled(true);

					JSONObject metadata = new JSONObject();
					try
					{
						metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
						metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StealthEvents.STEALTH_ACTIVATE);
						metadata.put(AnalyticsConstants.StealthEvents.STEALTH_ACTIVATE, true);
					} catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
					}
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);

				}
				else if(requestCode ==  HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT)
				{
					markStealthMsisdn(data.getExtras());
				}
				break;
			case Activity.RESULT_CANCELED:
			case LockPatternActivity.RESULT_FAILED:
			default:
				break;
			}

			break;
		case HikeConstants.ResultCodes.CONFIRM_AND_ENTER_NEW_PASSWORD:
			//adding this to handle the case where the user is selecting a new password
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				//Record Successful attempt
				recordPasswordEnterAttempt(true);
				LockPattern.createNewPattern(activity, true, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
				break;
			case Activity.RESULT_CANCELED:
			case LockPatternActivity.RESULT_FAILED:
			default:
				//Record Failed Attempt
				recordPasswordEnterAttempt(false);
				break;
			}
			break;
		}
		
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
			metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StealthEvents.STEALTH_PASSWORD_ENTRY);
			metadata.put(AnalyticsConstants.StealthEvents.STEALTH_REQUEST, requestCode);
			metadata.put(AnalyticsConstants.StealthEvents.STEALTH_RESULT, resultCode);
			if(resultCode <= Activity.RESULT_CANCELED)
			{
				metadata.put(AnalyticsConstants.StealthEvents.STEALTH_PASSWORD_CORRECT, resultCode == Activity.RESULT_OK);
			}
			if(isReset)
			{
				metadata.put(AnalyticsConstants.StealthEvents.STEALTH_PASSWORD_CHANGE, isReset);
			}
		} catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
		HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);

	}

	private static void markStealthMsisdn(Bundle stealthBundle)
	{
		if(stealthBundle != null && stealthBundle.containsKey(HikeConstants.STEALTH_MSISDN))
		{
			String msisdn = stealthBundle.getString(HikeConstants.STEALTH_MSISDN);
			StealthModeManager.getInstance().markStealthMsisdn(msisdn, true, true);
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
	public static void createNewPattern(Activity activity, boolean isResetPassword, int requestCode, Bundle bundle)
	{
		Intent i = createNewPatternIntent(activity, isResetPassword, requestCode);
		i.putExtra(HikeConstants.STEALTH, bundle);
		activity.startActivityForResult(i, requestCode);
	}// onClick()

	public static void createNewPattern(Activity activity, boolean isResetPassword, int requestCode)
	{
		Intent i = createNewPatternIntent(activity, isResetPassword, requestCode);
		activity.startActivityForResult(i, requestCode);
	}

	private static Intent createNewPatternIntent(Activity activity, boolean isResetPassword, int requestCode)
	{
		Intent i = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, activity, LockPatternActivity.class);
		i.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity());
		i.putExtra(Settings.Security.METADATA_AUTO_SAVE_PATTERN, true);
		i.putExtra(Settings.Display.METADATA_MIN_WIRED_DOTS, mBarMinWiredDots);
		i.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, isResetPassword);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

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

	private static void recordPasswordEnterAttempt(boolean successful)
	{
		recordHiddenModeResetClicks("hdn_cng_pwd", successful ? "correct" : "incorrect");
	}

	public static void recordCancelClickForResetPassword(String family)
	{
		recordHiddenModeResetClicks(family, "cancel");
	}

	public static void recordConfirmOnResetPassword(String family)
	{
		recordHiddenModeResetClicks(family, "confirm");
	}

	public static void recordRetryClickForResetPassword(String family)
	{
		recordHiddenModeResetClicks(family, "retry");
	}

	private static void recordHiddenModeResetClicks(String family, String species)
	{
		try
		{
			JSONObject json = HikeAnalyticsEvent.getSettingsAnalyticsJSON();

			if (json != null)
			{
				json.put(AnalyticsConstants.V2.FAMILY, family);
				json.put(AnalyticsConstants.V2.GENUS, StealthModeManager.getInstance().isPinAsPassword() ? "pin" : "pattern");
				json.put(AnalyticsConstants.V2.SPECIES, species);
				HAManager.getInstance().recordV2(json);
			}
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}
}
