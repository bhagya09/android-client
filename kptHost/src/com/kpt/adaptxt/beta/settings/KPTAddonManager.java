package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.SQLException;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;

public class KPTAddonManager  extends TabActivity implements OnSharedPreferenceChangeListener{
	
	private TabHost mTabHost;
	private Resources mResources;
	
	public static final int DOWNLOAD_TAB = 0;
	public static final int THEMES_TAB = 1;
	public static final String SHOW_TAB_KEY = "show_tab_key";
	public static final String TAB1 = "tab1";
	public static final String TAB2 = "tab2";
	private KPTAdaptxtDBHandler kptDB;
	private final int MENU_SEARCH = 0;
	private SharedPreferences mSharedPref;
	
	
	/**
	 * serach field edit text
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		 mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		 mSharedPref.registerOnSharedPreferenceChangeListener(this);
		super.onCreate(savedInstanceState);
		kptDB = new KPTAdaptxtDBHandler(getApplicationContext());
		kptDB.open();
		mTabHost = getTabHost();
		mResources = getResources();
		
		
		
		
		mTabHost.addTab(mTabHost.newTabSpec(TAB1).setIndicator(getString(R.string.kpt_UI_STRING_AMGR_1_300))
				.setContent(new Intent(this, AddonDownloadActivity.class)));
		mTabHost.addTab(mTabHost.newTabSpec(TAB2).setIndicator(getString(R.string.kpt_UI_STRING_HEADER_2_7002))
				.setContent(new Intent(this, ThemeDownloadActivity.class)));
		
		
		 
		
		setTitleUIForTabs();
		mTabHost.setCurrentTabByTag(TAB1);
		changeTabBackgrounds();
		 
		mTabHost.setOnTabChangedListener(new OnTabChangeListener(){
			@Override
			public void onTabChanged(String tabId) {

				/**
				 * make sure that no keyboard is shown when tab is changed 
				 * Bugtracker 287
				 */
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mTabHost.getApplicationWindowToken(), 0);
				changeTabBackgrounds();
			}
		});
	}
	
	
	/**
	 * UI for each tab header like title color, gravity, title bg etc
	 * 
	 */
	private void setTitleUIForTabs() {
	
		TextView leftChild = ((TextView) ((ViewGroup) KPTAddonManager.this.getTabHost()
				.getTabWidget().getChildTabViewAt(0)).getChildAt(1));
		TextView rightChild = ((TextView) ((ViewGroup) KPTAddonManager.this.getTabHost()
				.getTabWidget().getChildTabViewAt(1)).getChildAt(1));
		
		//set all default values like color, text size etc..
		leftChild.setTextColor(mResources.getColor(R.color.kpt_functional_keys_white_color));
		rightChild.setTextColor(mResources.getColor(R.color.kpt_functional_keys_white_color));
		
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2) {
			leftChild.setAllCaps(false);
			rightChild.setAllCaps(false);
		}
		
		leftChild.setTextSize(15);
		rightChild.setTextSize(15);
		
		leftChild.setGravity(Gravity.CENTER_VERTICAL);
		rightChild.setGravity(Gravity.CENTER_VERTICAL);
		
		DisplayMetrics dm = this.getResources().getDisplayMetrics();
		float densityDpi = dm.density;
		if(densityDpi >1 ){
			densityDpi = 1;
		}
		leftChild.setPadding(0, 0, 0, (int)(20*densityDpi));
		rightChild.setPadding(0, 0, 0, (int)(20*densityDpi));
		
	}

	/**
	 * Dynamic change of selected tab background
	 */
	private void changeTabBackgrounds() {
		int totalChilds = mTabHost.getTabWidget().getChildCount();
		for (int i = 0; i < totalChilds; i++) {
			View tabChild = mTabHost.getTabWidget().getChildAt(i);
			TextView tabView = ((TextView) ((ViewGroup) KPTAddonManager.this.getTabHost()
					.getTabWidget().getChildTabViewAt(i)).getChildAt(1));
			
			if(i == mTabHost.getCurrentTab()) {
				tabChild.setBackgroundDrawable(mResources.getDrawable(R.drawable.kpt_tab_selected));
				tabView.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				tabChild.setBackgroundDrawable(mResources.getDrawable(R.drawable.kpt_tab_unselected));
				tabView.setTypeface(Typeface.DEFAULT);
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		////KPTLog.e(" --ADDONMANAGER--onDestroy", "onDestroy");
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		try{
			kptDB.close();
		}catch (SQLException e) {
			// TODO: handle exception
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
			menu.add(0, 0, 0, getString(R.string.kpt_search_menu));
			return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(getCurrentActivity() != null && mTabHost != null && mTabHost.getCurrentTab() == THEMES_TAB){
			menu.setGroupEnabled(MENU_SEARCH, false);
		}else if(getCurrentActivity() != null && mTabHost != null && mTabHost.getCurrentTab() == DOWNLOAD_TAB){
			menu.setGroupEnabled(MENU_SEARCH, true);
		}
		if(getCurrentActivity() != null && mTabHost != null && mTabHost.getCurrentTab() == DOWNLOAD_TAB &&
		((AddonDownloadActivity)getCurrentActivity()).mDisableMenuItem){
			return false;
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		if(getCurrentActivity() != null && mTabHost!= null && mTabHost.getCurrentTab()== DOWNLOAD_TAB){
			((AddonDownloadActivity)getCurrentActivity()).performSearch();
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		
		
		// Fix for TP 7209
		/*if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KPTSetupWizard.PREFERENCE_EULA_ACCEPTED, false)) {
			KPTAdaptxtIMESettings.showEULA(this, false);		
		}*/
		//Fix for 7153
		/*int count  = 0;
		SharedPreferences sPrefs = getSharedPreferences(AddonInstalledActivity.INSTALLED_ADDONS_VERSIONINFO, MODE_PRIVATE);
		Map<String, ?> installedAddonMap = sPrefs.getAll();
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTAddonManager.this);
		if(sharedPref.getBoolean(KPTConstants.PREF_BASE_UPGRADE_PREV_ADDON_VERSION, false)) {
			KPTDbAdapter mDbAdapter = new KPTDbAdapter(this);
			mDbAdapter.open();
			count = mDbAdapter.getCount();
			mDbAdapter.close();
			
		} else {
			count = installedAddonMap.size();
		}*/

		/*
		 * Display installed tab if atleast one addon is installed, else display download tab
		 */
		//ServerUtilities.isGCMIdAvailableAndRegisteredOnServer(this);
	
		if(getIntent() != null && getIntent().getExtras() != null){

			int key = getIntent().getExtras().getInt(SHOW_TAB_KEY);
			switch (key) {
			
			case DOWNLOAD_TAB:
				getTabHost().setCurrentTabByTag(TAB1);
				break;
			case THEMES_TAB:
				getTabHost().setCurrentTabByTag(TAB2);
				changeTabBackgrounds();
				break;
			default:
				getTabHost().setCurrentTabByTag(TAB1);
				break;
			}
		} else{
			getTabHost().setCurrentTabByTag(TAB1);
		}
				
		super.onResume();
		
	
	}
	
	private ArrayList<KPTThemeItem> getAllExternalThemes(){
		
		return kptDB.getAllExternalThemes();
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		/*Message msg = Message.obtain();
		msg.obj = key;
		//handler.sendMessage(msg);
*/		
	}
	
/*	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			
			String key = (String)msg.obj;
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTAddonManager.this);
			boolean isBaseUpdateIncompatibleDialog = sharedPref.getBoolean(KPTConstants.PREF_SHOW_BASE_UPDATE_INCOMPATIBLE_DIALOG, false);
			if(isBaseUpdateIncompatibleDialog){
					Editor prefEditor = sharedPref.edit();
					prefEditor.putBoolean(KPTConstants.PREF_SHOW_BASE_UPDATE_INCOMPATIBLE_DIALOG, false);
					prefEditor.commit();
					
					new AlertDialog.Builder(KPTAddonManager.this) 
					.setTitle(R.string.kpt_incompatible_dialog_title)
					.setMessage(getResources().getString(R.string.kpt_base_incompatible_dialog_message))
					.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
						}
					})
					.create()
					.show();
					return;
			}
		
			super.handleMessage(msg);
		}
		
	};*/
	
	
}
