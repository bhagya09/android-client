package com.kpt.adaptxt.beta.glide;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.R;

public class KPTCustomGesturePreferences {
	
	private Context mContext;
	
	public static final String DELIMITER = "---";
	
	public static final String PREF_PREDEFINED_GESTURES_ALREADY_SAVED = "predefined_gestures_already_saved";
	public static final String PREF_APPLICATIONS_FILE_NAME = "applications_shortcuts_pref";
	public static final String PREF_WEBSITES_FILE_NAME = "websites_shortcuts_pref";
	public static final String PREF_CLIPBOARD_FILE_NAME = "clipboard_shortcuts_pref";
	
	public static final String GESTURE_SHORT_TYPE_APPLICATIONS = "shortcut_applications";
	private static KPTCustomGesturePreferences mCustomGesturePref;
	
	private SharedPreferences mSharedPrefApplications;
	private SharedPreferences mSharedPrefWebsites;
	//private SharedPreferences mSharedPrefClipboard;
	
	private Map<String, ?> mAppPrefs;
	private Map<String, ?> mWebsitePrefs;
	//private Map<String, ?> mClipboardPrefs;
	
	public static KPTCustomGesturePreferences getCustomGesturePreferences() {
		if(mCustomGesturePref == null) {
			mCustomGesturePref = new KPTCustomGesturePreferences();
		}
		return mCustomGesturePref;
	}
	
	public void initializePreferences(Context con) {
		mContext = con;
		mSharedPrefApplications = con.getSharedPreferences(PREF_APPLICATIONS_FILE_NAME, Activity.MODE_PRIVATE);
		mSharedPrefWebsites = con.getSharedPreferences(PREF_WEBSITES_FILE_NAME, Activity.MODE_PRIVATE);
		//mSharedPrefClipboard = con.getSharedPreferences(PREF_CLIPBOARD_FILE_NAME, Activity.MODE_PRIVATE);
		
		getOnlyApplicationsPreferences();
		getOnlyWebsitesPreferences();
		//getOnlyClipboardPreferences();
	}
	
	public boolean saveApplicationsPreference(String keycode, String applicationName, String packageName,
							boolean editingShortcut) {
		//if this is a edit shortcut then don't check the second conditions ie.. if it already exists
		if(editingShortcut || !checkIfThisUnicodeIsAlreadyUsed(keycode)) {
			String value = applicationName + DELIMITER + packageName;
			mSharedPrefApplications.edit().putString(keycode, value).commit();
			getOnlyApplicationsPreferences();
			return true;
		} else {
			return false;
		}
	}
	
	public boolean saveWebsitePreference(String keycode, String url, boolean editingShortcut) {
		//if this is a edit shortcut then don't check the second conditions ie.. if it already exists
		if(editingShortcut || !checkIfThisUnicodeIsAlreadyUsed(keycode)) {
			mSharedPrefWebsites.edit().putString(keycode, url).commit();
			getOnlyWebsitesPreferences();
			return true;
		} else {
			return false;
		}
	}
	
	/*public boolean saveClipboardPreference(String keycode, int clipboardAction) {
		if(!checkIfThisUnicodeIsAlreadyUsed(keycode)) {
			mSharedPrefClipboard.edit().putString(keycode, String.valueOf(clipboardAction)).commit();
			getOnlyWebsitesPreferences();
			return true;
		} else {
			return false;
		}
	}*/
	
	public Map<String, ?> getOnlyApplicationsPreferences() {
		mAppPrefs = mSharedPrefApplications.getAll();
		return mAppPrefs;
	}
	
	public Map<String, ?> getOnlyWebsitesPreferences() {
		mWebsitePrefs = mSharedPrefWebsites.getAll();
		return mWebsitePrefs;
	}
	
	/*public Map<String, ?> getOnlyClipboardPreferences() {
		mClipboardPrefs = mSharedPrefClipboard.getAll();
		return mClipboardPrefs;
	}*/
	
	public boolean chechIfInApplicationPreference(String keycode) {
		Iterator<String> keys = mAppPrefs.keySet().iterator();
		String actualChar = Character.toString((char) Integer.parseInt(keycode));
		while (keys.hasNext()) {
			int keyCode = Integer.parseInt(keys.next());
			String str = Character.toString((char) keyCode);
			if(actualChar.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;//mAppPrefs.containsKey(keycode);
	}
	
	public boolean chechIfInWebsitePreference(String keycode) {
		Iterator<String> keys = mWebsitePrefs.keySet().iterator();
		String actualChar = Character.toString((char) Integer.parseInt(keycode));
		while (keys.hasNext()) {
			int unicode = Integer.parseInt(keys.next());
			String str = Character.toString((char) unicode);
			if(actualChar.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;//mWebsitePrefs.containsKey(keycode);
	}
	
	/*public boolean chechIfInClipboardPreference(String keycode) {
		Iterator<String> keys = mClipboardPrefs.keySet().iterator();
		String actualChar = Character.toString((char) Integer.parseInt(keycode));
		while (keys.hasNext()) {
			int keyCode = Integer.parseInt(keys.next());
			String str = Character.toString((char) keyCode);
			if(actualChar.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;//mClipboardPrefs.containsKey(keycode);
	}*/
	
	public void deleteApplicationPreference(String keycode) {
		mSharedPrefApplications.edit().remove(keycode).commit();
		getOnlyApplicationsPreferences();
	}
	
	public void deleteWebsitePreference(String keycode) {
		mSharedPrefWebsites.edit().remove(keycode).commit();
		getOnlyWebsitesPreferences();
	}
	
	/*public void deleteClipboardPreference(String keycode) {
		mSharedPrefClipboard.edit().remove(keycode).commit();
		getOnlyClipboardPreferences();
	}*/
	
	public int getTotalCreatedCustomGestures() {
		if(mAppPrefs != null && mWebsitePrefs != null) {
			return (mAppPrefs.size() + mWebsitePrefs.size());
		}
		return 0;
	}
	
	public void loadDefaultPreferences() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext); 
		if(pref.getBoolean(PREF_PREDEFINED_GESTURES_ALREADY_SAVED, false)) {
			//Log.e("kpt","predefined are already saved");
		} else {
			Resources res = mContext.getResources();

			mSharedPrefApplications.edit().putString(res.getString(R.string.kpt_applications_predefined_key_1), 
					res.getString(R.string.kpt_applications_predefined_value_1)).commit();
			mSharedPrefApplications.edit().putString(res.getString(R.string.kpt_applications_predefined_key_2), 
					res.getString(R.string.kpt_applications_predefined_value_2)).commit();

			mSharedPrefWebsites.edit().putString(res.getString(R.string.kpt_website_predefined_key_1), 
					res.getString(R.string.kpt_website_predefined_value_1)).commit();
			mSharedPrefWebsites.edit().putString(res.getString(R.string.kpt_website_predefined_key_2), 
					res.getString(R.string.kpt_website_predefined_value_2)).commit();

			/*mSharedPrefClipboard.edit().putString(res.getString(R.string.clipboard_predefined_key_1), 
					String.valueOf(KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_CUT)).commit();
			mSharedPrefClipboard.edit().putString(res.getString(R.string.clipboard_predefined_key_2), 
					String.valueOf(KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_COPY)).commit();
			mSharedPrefClipboard.edit().putString(res.getString(R.string.clipboard_predefined_key_3), 
					String.valueOf(KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_PASTE)).commit();
			mSharedPrefClipboard.edit().putString(res.getString(R.string.clipboard_predefined_key_4), 
					String.valueOf(KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_SELECTALL)).commit();*/
			/*mSharedPrefClipboard.edit().putString(res.getString(R.string.clipboard_predefined_key_5), 
					String.valueOf(KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_SELECT)).commit();*/

			pref.edit().putBoolean(PREF_PREDEFINED_GESTURES_ALREADY_SAVED, true).commit();
			
			getOnlyApplicationsPreferences();
			getOnlyWebsitesPreferences();
			//getOnlyClipboardPreferences();
		}
	}
	
	public boolean checkIfThisUnicodeIsAlreadyUsed(String unicodeStr) {
		if(chechIfInApplicationPreference(unicodeStr)) {
			return true;
		} else if(chechIfInWebsitePreference(unicodeStr)) {
			return true;
		}/* else if(chechIfInClipboardPreference(unicodeStr)) {
			return true;
		}*/ else {
			return false;
		}
		/*Map<String, String> shortCuts = getAllCustomShortcuts();
		Iterator<String> keys = shortCuts.keySet().iterator();
		String actualChar = Character.toString((char) Integer.parseInt(unicodeStr));
		while (keys.hasNext()) {
			int keyCode = Integer.parseInt(keys.next());
			String str = Character.toString((char) keyCode);
			if(actualChar.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;//shortCuts.containsKey(unicode);*/
	}
	
	public void deletePreferences(int gestureType) {
		switch (gestureType) {
		case KPTGlideCustomGesturesActivity.SHORTCUT_TYPE_APPLICATION:
			mSharedPrefApplications.edit().clear().commit();
			getOnlyApplicationsPreferences();
			break;
		case KPTGlideCustomGesturesActivity.SHORTCUT_TYPE_WEBSITE:
			mSharedPrefWebsites.edit().clear().commit();
			getOnlyWebsitesPreferences();
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getAllCustomShortcuts() {
		Map<String, String> customShortcuts = new HashMap<String, String>();
		customShortcuts.putAll((Map<String, String>) mSharedPrefApplications.getAll());
		customShortcuts.putAll((Map<String, String>) mSharedPrefWebsites.getAll());
		//customShortcuts.putAll((Map<String, String>) mSharedPrefClipboard.getAll());
		return customShortcuts;
	}
}
