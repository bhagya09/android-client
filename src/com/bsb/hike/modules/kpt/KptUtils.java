/**
 * 
 */
package com.bsb.hike.modules.kpt;

import java.util.ArrayList;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.CustomFontEditText;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.GlobeKeyData;
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
		GlobeKeyData globeKeyData = mCustomKeyboard.getGlobeKeydata();

		if (null != globeKeyData)
		{

			final int status = globeKeyData.getStatus();
			if (status == GlobeKeyData.STATUS_OK)
			{
				currentLanguageIndex = globeKeyData.getCurrentIndex();
				Logger.e("KPT", " current index " + currentLanguageIndex);
				supportedLanguages = globeKeyData.getDisplayLanguages();

				if (null != supportedLanguages)
				{
					for (int i = 0; i < supportedLanguages.length; i++)
					{
						Logger.e("KPT", " Language name " + supportedLanguages[i]);
					}
					createBuilder(activity, mCustomKeyboard);
				}
				else
				{
					Logger.e("KPT", " Strange KPT failed to provide display language name ");
				}

				unsupportedLanguage = globeKeyData.getUnsupportedLangugeList();
				final int size = unsupportedLanguage.size();

				if (size > 0)
				{

					for (String string : unsupportedLanguage)
					{
						Logger.e("KPT", " device unsupported " + string);
					}
				}
				else
				{
					Logger.e("KPT", " GOOD! Device support all the languag of KPT ");
				}

				// We need to inform this to hike // 1 is the selected language index
				/*
				 * mCustomKeyboard.processChangeLanguageForDialog(1); AdaptxtEditText editText1 = (AdaptxtEditText)findViewById(R.id.edittext1);
				 * mCustomKeyboard.showCustomKeyboard(editText1, true);
				 */
			}
		}
		else
		{
			Logger.e("KPT", " KPT failed to provide globe key data ");
		}
	}
	
	private static void createBuilder(Activity activity, final CustomKeyboard mCustomKeyboard)
	{
		ContextThemeWrapper mContextWrapper = new ContextThemeWrapper(activity, R.style.AdaptxtTheme);
		AlertDialog.Builder builderNew = new AlertDialog.Builder(mContextWrapper);
		
		builderNew.setCancelable(true);
		// builder.setIcon(R.drawable.adaptxt_launcher_icon);
		builderNew.setNegativeButton(android.R.string.cancel, null);
		builderNew.setTitle("Keyboard Language");
		builderNew.setSingleChoiceItems(supportedLanguages, -1, new DialogInterface.OnClickListener()
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
//		TODO : analytics code here
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
	public static boolean isSystemKeyboard(Activity activity)
	{
		return HikeMessengerApp.isSystemKeyboard(activity);
	}
	
}
