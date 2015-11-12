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
import com.kpt.adaptxt.beta.CustomKeyboard;
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
	
	public static void generateKeyboardAnalytics(String currentLanguage)
	{
//		tracking keyboard language change event
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.KEYBOARD_LANGUAGE_CHANGED_EVENT);
			metadata.put(HikeConstants.KEYBOARD_LANGUAGE, currentLanguage);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + currentLanguage + "\n" + e);
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
