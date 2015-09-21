package com.kpt.adaptxt.beta.settings;

import java.util.Map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.packageinstaller.KPTDbAdapter;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTPrefsAdvSettings extends PreferenceActivity 
	implements SharedPreferences.OnSharedPreferenceChangeListener{
	
	private static final String AUTO_CORRECTION_KEY = "autocorrection";
	private static final String AUTO_CORRECTION_MODE_KEY = "autocorrection_mode";
	private static final String DISPLAY_ACCENTS_KEY = "Display_Accents";
	//private static final String POPUP_PRESS_KEY = "popup_on";
	private static final String AUTO_CAPITALIZATION_KEY = "auto_capitalization";

	private static final String KEYBOARD_TYPE = "layout_portrait"; 
//	private static final String KEYBOARD_TYPE_LAND = "layout_landscape";	
	
	//private CheckBoxPreference mAutoCorrect;
	//private ListPreference mAutoCorrectMode;
	private CheckBoxPreference mDisplayAccents;
	private CheckBoxPreference mSoundOnPress;
	private CheckBoxPreference mVibrateOnPress;
	private CheckBoxPreference mPopupOnPress;
	private CheckBoxPreference mAutoCapitalization; 

	private ListPreference mKeyboardType;
//	private ListPreference mKeyboardTypeLand;
	private SharedPreferences prefs;

	//private CheckBoxPreference mPunctutionPrediction;
	private CheckBoxPreference mAutoSpacing;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs_addvancesettings);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		
		//mAutoCorrect = (CheckBoxPreference)findPreference(AUTO_CORRECTION_KEY);
		//mAutoCorrectMode = (ListPreference)findPreference(AUTO_CORRECTION_MODE_KEY);
		mDisplayAccents = (CheckBoxPreference)findPreference(DISPLAY_ACCENTS_KEY);
		mSoundOnPress = (CheckBoxPreference) findPreference(KPTConstants.PREF_SOUND_ON);
		mVibrateOnPress = (CheckBoxPreference) findPreference(KPTConstants.PREF_VIBRATE_ON);
		mPopupOnPress = (CheckBoxPreference) findPreference(KPTConstants.PREF_POPUP_ON);
		mAutoCapitalization = (CheckBoxPreference) findPreference(AUTO_CAPITALIZATION_KEY);
		mAutoSpacing = (CheckBoxPreference)findPreference(KPTConstants.PREF_AUTO_SPACE);
		mKeyboardType = (ListPreference)findPreference(KEYBOARD_TYPE);
//		mKeyboardTypeLand = (ListPreference)findPreference(KEYBOARD_TYPE_LAND);
	//	mPunctutionPrediction = (CheckBoxPreference) findPreference(KPTConstants.PREF_PUNCTUTION_PREDICTION);

		
		mDisplayAccents.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				// TODO Auto-generated method stub
				if(!mDisplayAccents.isChecked())
				{
					mDisplayAccents.setSummary(R.string.kpt_UI_STRING_SETTINGS_SITEM_8);
				}
				else
				{
					mDisplayAccents.setSummary(R.string.kpt_UI_STRING_SETTINGS_SITEM_8A);
				}
				return true;
			}

		});
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		boolean isCoreMaintMode = prefs.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
		boolean isCoreInitializedAtleastOnce = prefs.getBoolean(KPTConstants.PREF_CORE_INITIALIZED_FIRST_TIME, false);
		boolean isAddonBeingInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
		boolean isAddonBeingUnInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
		//boolean isAddonInstalled = prefs.getBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, false);
		
		int addonCount = getInstalledAddonsCount();
		
		if(isCoreInitializedAtleastOnce && !(isCoreMaintMode) && !(isAddonBeingInstalled || isAddonBeingUnInstalled) && (addonCount > 0) ) {
			
			//mAutoCorrect.setEnabled(true);
			//mPunctutionPrediction.setEnabled(true);
			//mAutoCorrectMode.setEnabled(true);
			mDisplayAccents.setEnabled(true);
			mAutoCapitalization.setEnabled(true);
			mAutoSpacing.setEnabled(true);
			mDisplayAccents.setSummary(R.string.kpt_UI_STRING_SETTINGS_SITEM_8);
		} else {
			
			//mAutoCorrect.setEnabled(false);
			//mPunctutionPrediction.setEnabled(false);
			//mAutoCorrectMode.setEnabled(false);
			mDisplayAccents.setEnabled(false);
			mAutoCapitalization.setEnabled(false);
			mAutoSpacing.setEnabled(false);
			mDisplayAccents.setSummary(R.string.kpt_UI_STRING_SETTINGS_SITEM_8A);
		}
		
		String[] keyboardTypePort= getResources().getStringArray(R.array.kpt_keyboard_types);
		mKeyboardType.setSummary(keyboardTypePort[Integer.parseInt(prefs.getString(
				KPTConstants.PREF_PORT_KEYBOARD_TYPE, KPTConstants.KEYBOARD_TYPE_QWERTY))]);

		String[] keyboardTypeLand= getResources().getStringArray(R.array.kpt_keyboard_types);
		/*mKeyboardTypeLand.setSummary(keyboardTypeLand[Integer.parseInt(prefs.getString(
				KPTConstants.PREF_LAND_KEYBOARD_TYPE, KPTConstants.KEYBOARD_TYPE_QWERTY))]);*/
	}
	
	private int getInstalledAddonsCount() {
		int count  = 0;
		SharedPreferences sPrefs = getSharedPreferences(AddonDownloadActivity.INSTALLED_ADDONS_VERSIONINFO, MODE_PRIVATE);
		Map<String, ?> installedAddonMap = sPrefs.getAll();
		count = installedAddonMap.size();
		if(count == 0) {
			/*
			 * FOR THE NEXT RELEASE PLEASE REMOVE THE BELOW CODE AND GET DIRECTLY FROM SHARED PREFERNECE 
			 */
			KPTDbAdapter mDbAdapter = new KPTDbAdapter(this);
			mDbAdapter.open();
			count = mDbAdapter.getCount();
			mDbAdapter.close();
		}
		
		return count;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if(key.equals(KEYBOARD_TYPE)){

			String[] keyboardTypePort= getResources().getStringArray(R.array.kpt_keyboard_types);
			mKeyboardType.setSummary(keyboardTypePort[Integer.parseInt(prefs.getString(
					KPTConstants.PREF_PORT_KEYBOARD_TYPE, KPTConstants.KEYBOARD_TYPE_QWERTY))]);
		}

		/*if(key.equals(KEYBOARD_TYPE_LAND)){

			String[] keyboardTypeLand= getResources().getStringArray(R.array.kpt_keyboard_types);
			mKeyboardTypeLand.setSummary(keyboardTypeLand[Integer.parseInt(prefs.getString(
					KPTConstants.PREF_LAND_KEYBOARD_TYPE, KPTConstants.KEYBOARD_TYPE_QWERTY))]);
		}*/
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	

}
