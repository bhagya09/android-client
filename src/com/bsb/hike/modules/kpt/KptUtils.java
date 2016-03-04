/**
 * 
 */
package com.bsb.hike.modules.kpt;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.KPTAddonItem;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;

import android.app.Activity;
import android.view.View;

/**
 * @author anubansal
 *
 */
public class KptUtils
{
	public static void onGlobeKeyPressed(Activity activity, CustomKeyboard mCustomKeyboard)
	{
		IntentFactory.openKeyboardLanguageSetting(activity);
		
	}
	
	public static void generateKeyboardAnalytics(KPTAddonItem item)
	{
//		tracking keyboard language change event
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.KEYBOARD_LANGUAGE_CHANGED_EVENT);
			metadata.put(HikeConstants.KEYBOARD_LANGUAGE_CHANGE, item.getlocaleName());
			metadata.put(HikeConstants.KEYBOARD_LANGUAGE_CHANGE_SOURCE, HikeConstants.KEYBOARD_LANG_CHANGE_KBD);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			Utils.sendLocaleToServer();
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + item.getDisplayName() + "\n" + e);
		}
	}

	public static void destroyKeyboardResources(CustomKeyboard mCustomKeyboard, int ... editTexts)
	{
		if (mCustomKeyboard != null)
		{
			for (int editText : editTexts)
			{
				mCustomKeyboard.unregister(editText);
			}
						
			mCustomKeyboard.closeAnyDialogIfShowing();

			mCustomKeyboard.destroyCustomKeyboard();
			
			Logger.d("kptUtils", "onDestroy");
		}
	}
	
	public static void pauseKeyboardResources(CustomKeyboard mCustomKeyboard, AdaptxtEditText ... editTexts)
	{
		if (mCustomKeyboard != null)
		{
			for (AdaptxtEditText adaptxtEditText : editTexts)
			{
				mCustomKeyboard.showCustomKeyboard(adaptxtEditText, false);
			}
			
			mCustomKeyboard.closeAnyDialogIfShowing();
			Logger.d("kptUtils", "onPause");

			mCustomKeyboard.onPause();
		}
	}

	// Adding on resume call on Dec 16, 2015
	// Kpt team just realised that this callback is important to them.
	// They tried to fix issues: AND-4160 && AND-4159
	// Which resulted in another issue: suggestions not working in first chat thread launch.
	// For fixing this issue we need to make onResume call on kpt keyboard.
	// So we are placing onResume call on every activity we have made on the onPause call.
	public static void resumeKeyboard(CustomKeyboard mCustomKeyboard)
	{
		if (mCustomKeyboard != null)
		{
			mCustomKeyboard.onResume();
		}
	}
	
	public static final void updatePadding(Activity activity, int resourceView, int bottomPadding)
	{
		View mainView = activity.findViewById(resourceView);
		if (mainView != null && mainView.getPaddingBottom() != bottomPadding)
		{
			mainView.setPadding(0, 0, 0, bottomPadding);
		}
	}
	
	public static boolean isSystemKeyboard()
	{
		return HikeMessengerApp.isSystemKeyboard();
	}
	
}