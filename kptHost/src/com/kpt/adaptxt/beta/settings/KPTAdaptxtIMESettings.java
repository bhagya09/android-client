/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpt.adaptxt.beta.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.AdaptxtApplication;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.packageinstaller.KPTDatabaseHelper;
import com.kpt.adaptxt.beta.packageinstaller.KPTDbAdapter;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTAdaptxtIMESettings extends PreferenceActivity
implements SharedPreferences.OnSharedPreferenceChangeListener,
DialogInterface.OnDismissListener {

	public static final String SERVER_USERNAME = "xmlclient";
	public static final String SERVER_PASSWORD = "sko9yv5vlu";
	
	private static final String ADDON_MANAGER = "add_on_mgr";
	//private static final String LATEST_VERSION_KEY = "latestversion";
	private static final String PERSONAL_SUGG = "Learn";
	//private static final String THEMES_LAYOLUTS = "theme_layout";
	private static final String GLIDE_LAYOUT = "glide_layout";
	private static final String KPBCUSTOMIZATION = "KBCustomization";
	private static final String ADV_SETTINGS = "advance_settings";
	private static final String HELP = "Adaptxt_Help";
	private static final String RECOMMENDED = "Adaptxt_recommend";

	//private static final String TAG = "KPTAdaptxtIMESettings";
	public static final String XML_PATH_SERVER = "http://www.adaptxt.com/xmldocs/AndroidAdaptxtVersion.xml";
	private static final String XML_TAG_VERSION = "version";
	private static final String XML_TAG_LATEST = "latest";
	
	private static final String KPBACKUPRESTORE = "KBtest";

	private boolean mOkClicked = false;
	
	private Preference mAddonManager;
	private Preference mPersonalSugg;
	//private Preference mThemesLayout;
	private Preference mGlideLayout;
	//private Preference mKBCustomization;
	private Preference mAdvSettings;
	private Preference mHelp;
	private Preference mRecomended;
	//private Preference mLatestVersion;


	private String USER_CONTEXT_FILE_PATH = "/Profile/Profile/TestUser/Context";
	private String USER_DICT_FILE_PATH = "/Profile/Profile/TestUser/Dictionaries";


	public static final int DIALOG_BACKUP = 100;
	public static final int DIALOG_NEW_VERSION = 111;
	private boolean mIsShowingHelpDialog;

	//public static final int DISPLAY_INCOMPATIBLE_DIALOG = 1;

	private static final String VERSION_AVAIL =  "VERSION_AVAIL";
	private static final String VERSION_NOAVAIL = "VERSION_NOAVAIL";

	public static final String HELP_DIALOG = "HELP_DIALOG";

	private KPTAdaptxtDBHandler kptDB;




	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.prefs);
		kptDB = new KPTAdaptxtDBHandler(this);
		kptDB.open();

		USER_CONTEXT_FILE_PATH = getFilesDir().getAbsolutePath()+ USER_CONTEXT_FILE_PATH;
		USER_DICT_FILE_PATH = getFilesDir().getAbsolutePath()+ USER_DICT_FILE_PATH;


		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		//mLatestVersion = (Preference) findPreference(LATEST_VERSION_KEY);
		//mKBCustomization = (Preference) findPreference(KPBCUSTOMIZATION);

		//int currentThemeMode = Integer.parseInt(prefs.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
		mAddonManager = (Preference) findPreference(ADDON_MANAGER);
		mPersonalSugg = (Preference) findPreference(PERSONAL_SUGG);
		//mThemesLayout = (Preference) findPreference(THEMES_LAYOLUTS);
		mGlideLayout = (Preference) findPreference(GLIDE_LAYOUT);
		mAdvSettings = (Preference) findPreference(ADV_SETTINGS);
		mHelp = (Preference) findPreference(HELP);
		mRecomended = (Preference) findPreference(RECOMMENDED);

		
		

		
		
		
	}
	
	public boolean checkNetworkConnection() {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo)
		{
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}
	
	
	protected void launchAddonManager(int value){
		Intent intent = new Intent(KPTAdaptxtIMESettings.this, KPTAddonManager.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, value);
		AdaptxtApplication appState = AdaptxtApplication.getAdaptxtApplication(this);// ((AdaptxtApplication) getApplicationContext());
		appState.setIsFromThemeDetailsPage(true);
		startActivity(intent);
	}





	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	
	public static void showWizard(Context ctx){/*
		Intent i = new Intent();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setClass(ctx, KPTSetupWizardNew.class);
		ctx.startActivity(i);
		
	*/}
	

	@Override
	protected void onResume() {
		super.onResume();
	
		//Log.e("VMC", "IME SETTINGS --------- onResume");
		/*if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KPTConstants.PREFERENCE_EULA_ACCEPTED, false)) {
			showEULA(this, false);
		}else if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KPTConstants.PREFERENCE_SETUP_COMPLETED, false)) {
			showWizard(this);
			
		}*/
		
		/*if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HELP_DIALOG, false))  {
			showHelpDialog(KPTAdaptxtIMESettings.this);
		}*/

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if(prefs.getBoolean(KPTConstants.PREF_SHOW_INCOMPATIBLE_UPDATE_DIALOG, false)) { 
			KPTDbAdapter mDbAdapter = new KPTDbAdapter(this);
			mDbAdapter.open();
			Cursor cur = mDbAdapter.fetchAllItems();
			String message = "";
			int pkgNameCoreColumn =  cur.getColumnIndex(KPTDatabaseHelper.PKG_CORE_VERSION);
			int pkgNameDevColmun =  cur.getColumnIndex(KPTDatabaseHelper.PKG_DEVICE_VERSION);
			int coreDisplayColmun =  cur.getColumnIndex(KPTDatabaseHelper.CORE_DISPALY_NAME);
			if(cur !=null && cur.moveToFirst()) {
				do {
					if(cur.getString(pkgNameCoreColumn) != null && cur.getString(pkgNameDevColmun) != null
							&& !cur.getString(pkgNameCoreColumn).equals(cur.getString(pkgNameDevColmun))
							&& cur.getString(coreDisplayColmun) !=null) {
						message = message + "\n" + cur.getString(coreDisplayColmun);
					}
				} while (cur.moveToNext());
			}

			mDbAdapter.close();

			Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(KPTAdaptxtIMESettings.this).edit();
			prefEditor.putBoolean(KPTConstants.PREF_SHOW_INCOMPATIBLE_UPDATE_DIALOG, false);
			prefEditor.commit();

			new AlertDialog.Builder(this) 
			.setTitle(R.string.kpt_incompatible_dialog_title)
			.setMessage(getResources().getString(R.string.kpt_incompatible_dialog_message)+message)
			.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			})
			.create()
			.show();
		}

		
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		mIsShowingHelpDialog = false;
		try {
			kptDB.close();
		} catch (SQLiteException e) {

		}
	}



	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
	
		case DIALOG_NEW_VERSION:
			Dialog dialog_new_version = new AlertDialog.Builder(KPTAdaptxtIMESettings.this) 
			//.setIcon(R.drawable.alert_dialog_icon)
			.setMessage(R.string.kpt_license_new_version)
			.setTitle(R.string.kpt_license_dialog_title)
			.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					dialog.dismiss();
					Intent intent = new Intent (Intent.ACTION_VIEW);
					intent.setData (Uri.parse (KPTConstants.MARKET_SEARCH_WORD + getPackageName() ));
					startActivity (intent);
		
				}
			})
			.setNegativeButton(R.string.kpt_alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
					finish();

				}
			})
			.setOnCancelListener(new OnCancelListener(){ 
				@Override
				public void onCancel(DialogInterface dialog) {
					KPTAdaptxtIMESettings.this.finish();
				}
			})
			.create();
			dialog_new_version.show();
			return dialog_new_version;
		default:
			//KPTLog.e(TAG, "unknown dialog " + id);
			return null;
		}
	}

	public void onDismiss(DialogInterface dialog) {
		//mLogger.settingsWarningDialogDismissed();
		if (!mOkClicked) {
			// This assumes that onPreferenceClick gets called first, and this if the user
			// agreed after the warning, we set the mOkClicked value to true.
			//mVoicePreference.setValue(mVoiceModeOff);
		}
	}


	boolean retVal = false;




	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}

	
}
