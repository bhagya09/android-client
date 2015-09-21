package com.kpt.adaptxt.beta.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.AdaptxtApplication;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTPrefsThemeLayouts extends PreferenceActivity 
implements SharedPreferences.OnSharedPreferenceChangeListener{
	private static final String KEYBOARD_TYPE = "layout_portrait"; 
//	private static final String KEYBOARD_TYPE_LAND = "layout_landscape";
	private static final String THEME_MODE_KEY = "theme_mode";
	private static final String MORETHEMES = "MoreThemes";
	
	private ListPreference mKeyboardType;
//	private ListPreference mKeyboardTypeLand;
	private Preference mMoreThemes;
	private DialogPreference mThemeMode;
	
	private KPTAdaptxtDBHandler mKptDB;
	private KPTThemeItem kptThemeItem;
	private SharedPreferences prefs;
	private ImageListDialogPreferance imageList;
	private String[] defaultThemes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs_themeslayouts);
		
		imageList = (ImageListDialogPreferance) getPreferenceManager().findPreference("theme_mode");
		mKptDB = new KPTAdaptxtDBHandler(this);
		mKptDB.open();
		imageList.setDBObject(mKptDB);
		
		prefs = getPreferenceManager().getSharedPreferences();
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		mKeyboardType = (ListPreference)findPreference(KEYBOARD_TYPE);
//		mKeyboardTypeLand = (ListPreference)findPreference(KEYBOARD_TYPE_LAND);

		//Theme 
		mThemeMode = (DialogPreference)findPreference(THEME_MODE_KEY);
		mMoreThemes = (Preference) findPreference(MORETHEMES);
		
		defaultThemes = getResources().getStringArray(R.array.kpt_theme_options_entries);
		
		int currentThemeMode = Integer.parseInt(prefs.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
		
		kptThemeItem = mKptDB.getThemeValues((currentThemeMode));

		mThemeMode.setDefaultValue(kptThemeItem.getThemeID());
		if(currentThemeMode > 2){
			mThemeMode.setSummary(kptThemeItem.getThemeName());
		}else{
			mThemeMode.setSummary(defaultThemes[currentThemeMode]);
		}
		
		String[] keyboardTypePort= getResources().getStringArray(R.array.kpt_keyboard_types);
		mKeyboardType.setSummary(keyboardTypePort[Integer.parseInt(prefs.getString(
				KPTConstants.PREF_PORT_KEYBOARD_TYPE, "1"))]);



		String[] keyboardTypeLand= getResources().getStringArray(R.array.kpt_keyboard_types);
		/*mKeyboardTypeLand.setSummary(keyboardTypeLand[Integer.parseInt(prefs.getString(
				KPTConstants.PREF_LAND_KEYBOARD_TYPE, "0"))]);*/
		
		mMoreThemes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				launchAddonManager(KPTAddonManager.THEMES_TAB);
				return true;
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mKptDB.open();
		imageList.setDBObject(mKptDB);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean isCoreMaintMode = prefs.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
		boolean isCoreInitializedAtleastOnce = prefs.getBoolean(KPTConstants.PREF_CORE_INITIALIZED_FIRST_TIME, false);
		boolean isAddonBeingInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
		boolean isAddonBeingUnInstalled = prefs.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
		
		if(isCoreInitializedAtleastOnce && !(isCoreMaintMode) && !(isAddonBeingInstalled || isAddonBeingUnInstalled) ) {
			mThemeMode.setEnabled(true);
		} else {
			mThemeMode.setEnabled(false);
		}
		
	}
	
	protected void launchAddonManager(int value){
		Intent intent = new Intent(KPTPrefsThemeLayouts.this, KPTAddonManager.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, value);
		AdaptxtApplication appState =  AdaptxtApplication.getAdaptxtApplication(this);
		appState.setIsFromThemeDetailsPage(true);
		startActivity(intent);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		try{
			
			mKptDB.close();
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
	//	if(this.hasWindowFocus()){
		if(key.equals(THEME_MODE_KEY)){

			int currentThemeMode = Integer.parseInt(prefs.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
			kptThemeItem = mKptDB.getThemeValues(currentThemeMode);

			mThemeMode.setDefaultValue(currentThemeMode);
			if(currentThemeMode > 2){
				mThemeMode.setSummary(kptThemeItem.getThemeName());
			}else{
				mThemeMode.setSummary(defaultThemes[currentThemeMode]);
			}
		}

		if(key.equals(KEYBOARD_TYPE)){

			String[] keyboardTypePort= getResources().getStringArray(R.array.kpt_keyboard_types);
			mKeyboardType.setSummary(keyboardTypePort[Integer.parseInt(prefs.getString(
					KPTConstants.PREF_PORT_KEYBOARD_TYPE, "0"))]);
		}

		/*if(key.equals(KEYBOARD_TYPE_LAND)){

			String[] keyboardTypeLand= getResources().getStringArray(R.array.kpt_keyboard_types);
			mKeyboardTypeLand.setSummary(keyboardTypeLand[Integer.parseInt(prefs.getString(
					KPTConstants.PREF_LAND_KEYBOARD_TYPE, "0"))]);
		}*/
		//}
	};

}
