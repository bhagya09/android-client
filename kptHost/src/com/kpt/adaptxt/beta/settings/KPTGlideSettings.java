package com.kpt.adaptxt.beta.settings;

import java.util.Map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTGlideSettings extends PreferenceActivity {

	//private static final String GLIDE_FEEDBACK_KEY = "glide_feedback";
	
	private CheckBoxPreference mGlideSetting;
	//private Preference mGlideFeedback;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.prefs_glide_settings);
		mGlideSetting = (CheckBoxPreference)findPreference(KPTConstants.PREF_GLIDE);
		
		//mGlideFeedback = (Preference) findPreference(GLIDE_FEEDBACK_KEY);
		/*mGlideFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				// Removed network check as per the TP item 8328
				// if(CheckInternetConnection()){
				final Intent emailIntent = new Intent(
						android.content.Intent.ACTION_SEND);

				 Fill it with Data 
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
						new String[] { "support@adaptxt.com" });
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						getResources().getString(
								R.string.kpt_UI_STRING_EMAILSUBJECT_1_220));

				String adaptxt_VersionName = null;
				try {
					Context mContext = KPTGlideSettings.this;
					adaptxt_VersionName = mContext.getPackageManager()
							.getPackageInfo(mContext.getPackageName(), 0).versionName;

				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}

				String str = "";
				String[] displayLanguages = KPTAdaptxtIME.getInstalledAddons();
				if (displayLanguages != null) {
					for(int i=0;i<displayLanguages.length;i++)
						str += displayLanguages[i] + "-" + adaptxt_VersionName
						+ "\n";
				}

				String body = getResources().getString(R.string.system_info)
						+ "\n"
						+ "\n"
						+ getResources().getString(R.string.email_model)
						+ Build.MANUFACTURER
						+ "\t"
						+ Build.MODEL
						+ "\n"
						+ getResources().getString(R.string.email_android)
						+ Build.VERSION.RELEASE
						+ "\n"
						+ getResources().getString(R.string.email_adaptxt)
						+ adaptxt_VersionName
						+ "\n"
						+ getResources().getString(R.string.email_addon)
						+ "\n"
						+ str
						+ "\n"
						+ "<--------"
						+ getResources().getString(R.string.email_add_comment)
						+ "------->" + "\n";

				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);

				 Send it off to the Activity-Chooser 
				KPTGlideSettings.this.startActivity(Intent.createChooser(
						emailIntent,
						getResources().getString(R.string.send_mail)));
				
				 * }else { Toast.makeText(KPTAdaptxtIMESettings.this,
				 * getResources().getString(R.string.kpt_toast_internet),
				 * Toast.LENGTH_SHORT).show(); }
				 
				return true;
			}
		});*/
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean isCoreMaintMode = prefs.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
		boolean isCoreInitializedAtleastOnce = prefs.getBoolean(KPTConstants.PREF_CORE_INITIALIZED_FIRST_TIME, false);
		boolean isAddonBeingInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
		boolean isAddonBeingUnInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
		//boolean isAddonInstalled = prefs.getBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, false);
		
		int addonCount = getInstalledAddonsCount();
		
		if(isCoreInitializedAtleastOnce && !(isCoreMaintMode) && !(isAddonBeingInstalled || isAddonBeingUnInstalled) && (addonCount > 0) ) {
			mGlideSetting.setEnabled(true);
		} else {
			mGlideSetting.setEnabled(false);
		}
	}
	
	private int getInstalledAddonsCount() {
		int count  = 0;
		SharedPreferences sPrefs = getSharedPreferences(AddonDownloadActivity.INSTALLED_ADDONS_VERSIONINFO, MODE_PRIVATE);
		Map<String, ?> installedAddonMap = sPrefs.getAll();
		count = installedAddonMap.size();
		return count;
	}
	
}
