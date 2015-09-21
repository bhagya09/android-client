package com.kpt.adaptxt.beta.settings;



import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.KPTLanguageSwitcher;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTIMELanguageSelectionDialog {

	/**
	 * Dialog to be launched in maintenance mode.
	 */
	public static int MAINTANENCE_MODE_DIALOG = 1;

	/**
	 * Dialog to be launched in normal mode, displaying list of languages.
	 */
	public static int NORMAL_MODE_DIALOG = 2;

	/**
	 * Current selected language item index
	 */
	private int mCurrentLanguageIndex;

	/**
	 * Current dialog mode
	 */
	private int mCurrentDialogMode;

	private LinearLayout parent;

	private AlertDialog alertDialog;

	public KPTIMELanguageSelectionDialog(Context context,final AdaptxtIME mAdaptxtIME, IBinder windowToken, KPTLanguageSwitcher languageSwitcher,
			Handler uiHandler, int mode, ContextThemeWrapper contextThemeWrapper) {

		Context mcontext = context;
		//AdaptxtIME mAdaptxtIME = mAdaptxtIMEs;
		final KPTLanguageSwitcher mLanguageSwitcher = languageSwitcher;
		final Handler mUIHandler = uiHandler;
		mCurrentDialogMode = mode;
		final SharedPreferences.Editor sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mcontext).edit();
		parent = (LinearLayout) View.inflate(mcontext, R.layout.kpt_ime_language_selection_dialog, null);

		ListView languageListView = (ListView) parent.findViewById(R.id.language_selection_list_view);

		Drawable dividerImage = mcontext.getResources().getDrawable(android.R.drawable.divider_horizontal_dim_dark);
		languageListView.setDivider(dividerImage);

		if(mCurrentDialogMode == MAINTANENCE_MODE_DIALOG) {
			String[] maintModeText  = {mcontext.getResources().getString(R.string.kpt_ime_language_selection_dialog_maint_mode)};
			ArrayAdapter<String> adp = new ArrayAdapter<String>(mcontext,R.layout.kpt_language_selection_maint_mode_text_view_item, maintModeText);
			languageListView.setAdapter(adp);		
			languageListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			languageListView.setVerticalScrollBarEnabled(false);
		} else if(mCurrentDialogMode == NORMAL_MODE_DIALOG){
			String[] displayLanguages =mLanguageSwitcher.getDisplayLanguages();

			if(displayLanguages != null) {
				ArrayAdapter<String> adp = new ArrayAdapter<String>(mcontext,R.layout.kpt_language_selection_list_radio_item, displayLanguages);
				languageListView.setAdapter(adp);		
				languageListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				mCurrentLanguageIndex = mLanguageSwitcher.getCurrentIndex();
				languageListView.setItemChecked(mCurrentLanguageIndex, true);
			}

			languageListView.setOnItemClickListener(new  OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View view, int position,
						long id) {
					if(mCurrentLanguageIndex != position) {
						mCurrentLanguageIndex  = position;
						boolean isRTL = mAdaptxtIME.checkIfRTL(mLanguageSwitcher.getInputLocale());
						mLanguageSwitcher.setCurrentIndex(position);
						
						mUIHandler.removeMessages(AdaptxtIME.MSG_RELOAD_KEYBOARDS);
						Message msg = mUIHandler.obtainMessage(AdaptxtIME.MSG_RELOAD_KEYBOARDS);
						if(isRTL != mAdaptxtIME.checkIfRTL(mLanguageSwitcher.getInputLocale())){
							msg.arg1 =1;	
						}else{
							msg.arg1 = 0;
						}
						mUIHandler.sendMessageDelayed(msg, 100);
						
						alertDialog.dismiss();
						
						String prevLocale = mLanguageSwitcher.getPrevInputLocale().toString();
						String NextLocale = mLanguageSwitcher.getNextInputLocale().toString();
						if(prevLocale.equalsIgnoreCase("th_TH") || NextLocale.equalsIgnoreCase("th_TH")){
							mAdaptxtIME.onKey(32, null, false);
						}
					} else {
						alertDialog.dismiss();
					}
				}
			});	
		}
	}	

	public View getListView() {
		return parent;
	}

	public void setAlertDialog(AlertDialog mOptionsDialog) {
		alertDialog = mOptionsDialog;
		
	}
}
