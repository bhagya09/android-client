/**
 * 
 */
package com.bsb.hike.modules.kpt;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author anubansal
 *
 */
public class KptUtils
{
	
	private static ArrayList<String> unsupportedLanguage;

	private static String[] supportedLanguages;
	
	private static int currentLanguageIndex;
	
	public static void onGlobeKeyPressed(Activity activity, CustomKeyboard mCustomKeyboard)
	{
		IntentFactory.openKeyboardLanguageSetting(activity);
	}
	
	private static void createBuilder(Activity activity, final CustomKeyboard mCustomKeyboard, int currentLanguageIndex2)
	{
		ContextThemeWrapper mContextWrapper = new ContextThemeWrapper(activity, R.style.AdaptxtTheme);
		AlertDialog.Builder builderNew = new AlertDialog.Builder(mContextWrapper);
		
		builderNew.setCancelable(true);
		// builder.setIcon(R.drawable.adaptxt_launcher_icon);
		builderNew.setNegativeButton(android.R.string.cancel, null);
		builderNew.setTitle("Keyboard Language");
		builderNew.setSingleChoiceItems(supportedLanguages, currentLanguageIndex2, new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				currentLanguageIndex = which;
				mCustomKeyboard.processChangeLanguageForDialog(which);
				dialog.dismiss();
			}
		});
		final int color;
		color = activity.getResources().getColor(R.color.kpt_white_color_text);

		AlertDialog mOptionsDialog = builderNew.create();

		Window window = mOptionsDialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = activity.getWindow().getDecorView().getRootView().getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		mOptionsDialog.show();
	}
	
	public static void generateKeyboardAnalytics(String currentLanguage)
	{
////		tracking keyboard language change event
//		try
//		{
//			JSONObject metadata = new JSONObject();
//			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.KEYBOARD_LANGUAGE_CHANGED_EVENT);
//			metadata.put(HikeConstants.KEYBOARD_LANGUAGE, currentLanguage);
//			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
//		}
//		catch(JSONException e)
//		{
//			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + currentLanguage + "\n" + e);
//		}
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
