/*
 * Copyright (C) 2010 Google Inc.
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

package com.kpt.adaptxt.beta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.util.FontsUtill;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.core.coreapi.KPTLanguage;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Keeps track of list of selected input languages and the current
 * input language that the user has selected.
 */
public class KPTLanguageSwitcher {

	/**
	 * IME reference
	 */
	public AdaptxtIME mIme;

	private Context mContext;

	/**
	 * Current selected/top priority language
	 */
	private int mCurrentIndex = -1;

	/**
	 * Default language string
	 */
	private String mDefaultInputLanguage;

	/**
	 * Default locale
	 */
	private Locale mDefaultInputLocale;

	/**
	 * System's locale
	 */
	private Locale mSystemLocale;

	/**
	 * Reference to adaptxt core service
	 */
	private KPTCoreServiceHandler mCoreServiceHandler;

	/**
	 * List of language items and their details
	 */
	private List<KPTLanguageItem> mEnabledLanguagesList;


	//Map<Integer,KPTLanguageItem> mPrioritizeAddonList = new HashMap<Integer,KPTLanguageItem>(); 


	private boolean mSpaceSwipe = false;

	private boolean mShoudDisplayEngLocale;

	private final SharedPreferences mSharedPref;

	private static final String LATIN_SCRIPT = "Latn";

	private String mCurrentDictionaryFileName = null;
	
	private static HashMap<String, String> langFontList;
	static{
		langFontList = new HashMap<String, String>();
		langFontList.put("engus", "ab");
		langFontList.put("hinin", "अआ");
		langFontList.put("benin", "অআ");
		langFontList.put("gujin", "અઆ");
		langFontList.put("marin", "अआ");
		langFontList.put("tamin", "அஆ");
		langFontList.put("telin", "అఆ");
		langFontList.put("kanin", "ಅಆ");
		langFontList.put("malin", "അആ");
	}

	/**
	 * Helper class for holding language details
	 * @author smadabusi
	 *
	 */

	private class KPTLanguageItem implements Comparable<KPTLanguageItem>{

		/**
		 * Core side component Id of the language
		 */
		int mComponentId;

		/**
		 * Display name of the language
		 */
		String mDisplayName;

		/**
		 * Locale of the language
		 */
		Locale mLocale;

		/**
		 * Locale of the language
		 */
		String mLocaleString;

		/**
		 * 2 letter language_region code
		 */
		String mLanguageCode;

		/**
		 * Language's script
		 */
		String mScript;
		
		/**
		 * language file name
		 */
		String mLanguageFileName;

		@Override
		public int compareTo(KPTLanguageItem another) {
			return this.mDisplayName.compareTo(another.mDisplayName);
		}
	}
	
	/**
	 * Default Constructor
	 * @param ime
	 */
	public KPTLanguageSwitcher(AdaptxtIME ime, Context context) {
		mIme = ime;
		//this is for activity
		mContext = context;
		 mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		mEnabledLanguagesList = new ArrayList<KPTLanguageItem>();
	}

	public KPTLanguageSwitcher(Context context) {
		mContext = context;
		mEnabledLanguagesList = new ArrayList<KPTLanguageItem>();
		 mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	/**
	 * Total loaded language count.
	 * @return
	 */
	public int getLocaleCount() {
		return mEnabledLanguagesList.size();
	}
	
	public void checkLanguageChange(String langChange){
		
		if(langChange != null && !(langChange.equals(getCurrentLanguageDisplayName()))){
			
			for (KPTLanguageItem kptLanguageItem : mEnabledLanguagesList) {
				if(kptLanguageItem.mDisplayName.equals(langChange)){
					updatePriorityInCore(kptLanguageItem.mComponentId);
				}
			}
		}
	}

	/**
	 * Loads the currently selected input languages from shared preferences.
	 * Resets the stored language list.
	 * @param sp
	 * @return whether there was any change
	 */
	public boolean loadLocales(Context appContext) {
		boolean updateResult = updateLanguages(appContext);
		if(!updateResult) {
			loadDefaults();
			if (mEnabledLanguagesList.size() == 0) {
				return false;
			}
		} 
		return true;
	}
	
	public ArrayList<KPTAddonItem> getUnsupportedLanguageList() {
		KPTCoreEngine coreEngine = mCoreServiceHandler.getCoreInterface();
		KPTParamDictionary[] dictionaryList = coreEngine.getInstalledDictionaries();
		
		ArrayList<KPTAddonItem> unsupportedLanguageList = new ArrayList<KPTAddonItem>();
		
		for(KPTParamDictionary dictionary : dictionaryList) {
			if (!(dictionary.isDictLoaded())) {
				KPTAddonItem languageItem = new KPTAddonItem();
				languageItem.setComponentId(dictionary.getComponentId());
				languageItem.setDisplayName(dictionary.getDictDisplayName());
				languageItem.setFileName(dictionary.getDictFileName());
				
				unsupportedLanguageList.add(languageItem);
			}
		}
		return unsupportedLanguageList;
	}
	
	public List<KPTAddonItem> getInstalledDicts(Context appContext){
		KPTCoreEngine coreEngine = mCoreServiceHandler.getCoreInterface();
		KPTParamDictionary[] dictionaryList = coreEngine.getInstalledDictionaries();
		
		List<KPTAddonItem> enabledLanguagesList = new ArrayList<KPTAddonItem>();
		ArrayList<KPTAddonItem> tempList = new ArrayList<KPTAddonItem>();
		
		for(KPTParamDictionary dictionary : dictionaryList) {
			if (dictionary.isDictLoaded()) {
				// Add this dictionary to our lists
				KPTAddonItem languageItem = new KPTAddonItem();
				languageItem.setComponentId(dictionary.getComponentId());
				languageItem.setDisplayName(dictionary.getDictDisplayName());
				languageItem.setFileName(dictionary.getDictFileName());

				String languageText = (dictionary.getDictFileName() != null && langFontList.get(dictionary.getDictFileName()) != null) ? 
						langFontList.get(dictionary.getDictFileName()) : dictionary.getDictDisplayName();
				boolean isFontSupported = FontsUtill.isSupported(mContext, languageText);
				if(isFontSupported){
					enabledLanguagesList.add(languageItem);
				}else{

					tempList.add(languageItem);
					coreEngine.unloadDictionary(dictionary.getComponentId());
					
					dictionary.setDictState(dictionary.getFieldMaskState(),
							dictionary.getComponentId(), false, 
							dictionary.getDictPriority(), false,
							dictionary.getLicenseState().getLicenseStateInt());
				}
			}
			
			//coreEngine.activateTopPriorityDictionaryKeymap();
		}
		
		coreEngine.saveUserContext();
		PreferenceManager.getDefaultSharedPreferences(appContext).edit()
				.putBoolean(KPTConstants.PREFS_UNSUPPORTED_LANG_STATUS, true).commit();
		return enabledLanguagesList;
	}
	
	/*public ArrayList<KPTAddonItem> getUnSupportedLanguages(){
		return mUnsupportedList;
	}*/

	/**
	 * Updates the language list from core.
	 * Resets and initializes the enabled language list in platform side.
	 * @return If any language is loaded in core.
	 */
	private boolean updateLanguages(Context appContext) {
		if(appContext == null || PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
				KPTConstants.PREF_CORE_MAINTENANCE_MODE, false) ) {
			// Core in maintenance mode
			return false;
		}

		// To avoid loading of language list multiple times in IME
		boolean isDirty = PreferenceManager.getDefaultSharedPreferences(appContext).getBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true);
		if (!isDirty && mEnabledLanguagesList.size() != 0 && !mSpaceSwipe) {
			// Already language list updated, no need to fetch from core again.
			return true;
		}

		// Bug Fix: 6215.
		// Clear the language list only when core is not in maintenance mode.
		// Else till core comes out of maintenance mode let the existing language list be available.
		mEnabledLanguagesList.clear();
		//mPrioritizeAddonList.clear();

		if(mCoreServiceHandler  == null) {
			return false;
		}

		KPTCoreEngine coreEngine = mCoreServiceHandler.getCoreInterface();
		if(coreEngine == null) {
			return false;
		}

		// Get the list of languages from core
		KPTParamDictionary[] dictionaryList = coreEngine.getInstalledDictionaries();

		if(dictionaryList == null || dictionaryList.length == 0) {
			// Error in getting list from core
			return false;
		}

		mEnabledLanguagesList.clear();

		KPTLanguageItem topPriorityLanguageItem = null;
		int topPriorityRank = dictionaryList.length;
		for(KPTParamDictionary dictionary : dictionaryList) {
			if (dictionary.isDictLoaded()) {
				// Add this dictionary to our lists
				KPTLanguage dictionaryLanguage = dictionary.getDictLanguage();

				String totalLanguageCode = new String();
				String languageCode = null;
				if(dictionaryLanguage.getLanguage() != null && dictionaryLanguage.getLanguage().length() > 1) {
					languageCode = dictionaryLanguage.getLanguage().substring(0, 2);
					totalLanguageCode = totalLanguageCode.concat(languageCode); 
				}
				String regionCode = null;
				if(dictionaryLanguage.getLanguage() != null && dictionaryLanguage.getLanguage().length() > 4) {
					regionCode = dictionaryLanguage.getLanguage().substring(3,5);
					totalLanguageCode = totalLanguageCode.concat(regionCode);
				}

				KPTLanguageItem languageItem = new KPTLanguageItem();
				languageItem.mComponentId = dictionary.getComponentId();
				languageItem.mDisplayName = dictionary.getDictDisplayName();
				languageItem.mLocale = new Locale(languageCode, regionCode);
				languageItem.mLanguageCode = totalLanguageCode;
				languageItem.mScript = dictionary.getDictLanguage().getScript();
				languageItem.mLocaleString = languageCode + "-" + regionCode; 
				languageItem.mLanguageFileName = dictionary.getDictFileName();
				mEnabledLanguagesList.add(languageItem);
				//	mPrioritizeAddonList.put(dictionary.getDictPriority(), languageItem);
				if(dictionary.getDictPriority() < topPriorityRank) {
					topPriorityLanguageItem = languageItem;
					topPriorityRank = dictionary.getDictPriority();
				}
			}
		}
		// Sort the list in alphabetical order for display
		Collections.sort(mEnabledLanguagesList);

		// Update the current index
		mCurrentIndex = Collections.binarySearch(mEnabledLanguagesList, topPriorityLanguageItem);

		// Update teh shared pref that language list is updated.
		SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
		sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, false).commit();
		mSpaceSwipe = false;
		return true;
	}

	/*  public String[] getPriorityDictList(){

		String[] langArray = new String[mPrioritizeAddonList.size()];
		for(int j=0; j<mPrioritizeAddonList.size();j++){
			langArray[j] = mPrioritizeAddonList.get(j).mDisplayName;
		}
		return langArray;
    }*/
	/**
	 * Sets the priority in the core for selected top language
	 */
	private void updatePriorityInCore() {
		if(mEnabledLanguagesList.size() > 0 && mCurrentIndex != -1 && mEnabledLanguagesList.size() > mCurrentIndex) {

			int componentId = mEnabledLanguagesList.get(mCurrentIndex).mComponentId;
			mCurrentDictionaryFileName = mEnabledLanguagesList.get(mCurrentIndex).mLanguageFileName;
			
			KPTCoreEngine coreEngine = mCoreServiceHandler.getCoreInterface();

			if(coreEngine != null) {
				boolean res = coreEngine.setDictionaryPriority(componentId, 0);
				// Set this language keymap in core
				res = coreEngine.activateLanguageKeymap(componentId, null);
				if(!res) {
					//KPTLog.e("KPTLanguageSwitcher", "Set keymap failed");
				}
				coreEngine.saveUserContext();
				
				mSharedPref.edit().putInt(KPTConstants.KPT_CHANGE_LANG_ID, componentId).commit();
			}
			
		}
	}
	
	public void updatePriorityInCore(int componentId) {
		KPTCoreEngine coreEngine = mCoreServiceHandler.getCoreInterface();

		if(coreEngine != null) {
			boolean res = coreEngine.setDictionaryPriority(componentId, 0);
			// Set this language keymap in core
			res = coreEngine.activateLanguageKeymap(componentId, null);
			if(!res) {
				//KPTLog.e("KPTLanguageSwitcher", "Set keymap failed");
			}
			coreEngine.saveUserContext();
			mSharedPref.edit().putInt(KPTConstants.KPT_CHANGE_LANG_ID, componentId).commit();
		}
	}

	public String getCurrentDictionaryFileName() {
		return mCurrentDictionaryFileName;
	}
	
	/**
	 * Sets the adaptxt core service reference
	 * @param coreServiceHandler Adaptxt core service reference
	 */
	public void setCoreServicehandler(KPTCoreServiceHandler coreServiceHandler) {
		mCoreServiceHandler = coreServiceHandler;
	}

	/**
	 * Loaded default system's locale.
	 */
	private void loadDefaults() {
		mDefaultInputLocale = mContext.getResources().getConfiguration().locale;
		String country = mDefaultInputLocale.getCountry();
		mDefaultInputLanguage = mDefaultInputLocale.getLanguage() +
				(TextUtils.isEmpty(country) ? "" : "_" + country);
		
	}

	/**
	 * Returns the currently selected input language code, or the display language code if
	 * no specific locale was selected for input.
	 * @return Current selected language
	 */
	public String getInputLanguage() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1) 
			return mDefaultInputLanguage;
		updateLanguages(mContext);
		if (mEnabledLanguagesList.size() > mCurrentIndex) {
			return mEnabledLanguagesList.get(mCurrentIndex).mLanguageCode;
		}
		return mDefaultInputLanguage;
	}

	/**
	 * Returns the currently selected language's locale
	 * @return Current selected language's script
	 */
	public String getInputLanguageLocale() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1) 
			return "en-US";
		if (mEnabledLanguagesList.size() > mCurrentIndex) {
			if(mShoudDisplayEngLocale){
				return "en-US";
			}else{
				return mEnabledLanguagesList.get(mCurrentIndex).mLocaleString;
			}
		}
		return "en-US";
	}

	/**
	 * Returns the list of enabled language codes list.
	 * @return Language code string list
	 */
	public String[] getEnabledLanguages() {
		if(mEnabledLanguagesList.size() > 0) {
			String[] langArray = new String[mEnabledLanguagesList.size()];

			for(int ind = 0 ; ind  < mEnabledLanguagesList.size() ; ind++) {
				langArray[ind] = mEnabledLanguagesList.get(ind).mLanguageCode;
			}

			return langArray;

		}
		else {
			return new String[0];
		}
	}

	public int getCurrentComponentId(){
		if(mEnabledLanguagesList.size() > 0 && mCurrentIndex != -1 && mEnabledLanguagesList.size() > mCurrentIndex)     		
			return mEnabledLanguagesList.get(mCurrentIndex).mComponentId;
		else 
			return -1;
	}

	/**
	 * Returns the list of display language list.
	 * @return All enabled language's display name list
	 */
	public String[] getDisplayLanguages() {
		if(mEnabledLanguagesList.size() > 0) {
			String[] langArray = new String[mEnabledLanguagesList.size()];
			for(int ind = 0 ; ind  < mEnabledLanguagesList.size() ; ind++) {
				langArray[ind] = mEnabledLanguagesList.get(ind).mDisplayName;
			}
			return langArray;
		}
		else {
			return new String[0];
		}
	}

	/**
	 * Get current language's display name
	 * @return Current language's display name.
	 */
	public String getCurrentLanguageDisplayName() {
		if(mEnabledLanguagesList.size() > 0 && mCurrentIndex != -1) {
			mSpaceSwipe = true;
			updateLanguages(mContext);
			if(mEnabledLanguagesList.size()> mCurrentIndex){
				return mEnabledLanguagesList.get(mCurrentIndex).mDisplayName;
			}
		}
		return null;

	}

	
	
	/**
	 * Returns the currently selected input locale, or the display locale if no specific
	 * locale was selected for input.
	 * @return Current selected locale
	 */
	public Locale getInputLocale() {
		
		
		Locale locale;
		
			if (getLocaleCount() == 0 || mCurrentIndex == -1
					|| mEnabledLanguagesList.size() <= mCurrentIndex) {
				return mDefaultInputLocale;
			}
			if(mShoudDisplayEngLocale){
				locale = new Locale("en","US");
			}else{
				mCurrentDictionaryFileName = mEnabledLanguagesList.get(mCurrentIndex).mLanguageFileName;
				locale = mEnabledLanguagesList.get(mCurrentIndex).mLocale;
			}
		
		
		return locale;
	}

	public String getPrevLangLocaleString() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1
				|| mEnabledLanguagesList.size() <= mCurrentIndex) {
			return "en";
		}
		
		return  mEnabledLanguagesList.get(mCurrentIndex).mLocaleString;
	}
	
	/**
	 * Returns the next input locale in the list. Wraps around to the beginning of the
	 * list if we're at the end of the list.
	 * @return Next locale in language list
	 */
	public Locale getNextInputLocale() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1) {
			return mDefaultInputLocale;
		}

		int nextIndex = (mCurrentIndex + 1) % mEnabledLanguagesList.size();
		if (mEnabledLanguagesList.size() > nextIndex) {
			return mEnabledLanguagesList.get(nextIndex).mLocale;
		}
		return mDefaultInputLocale;
	}

	public String getNextLanguageDisplayName() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1) {
			return mDefaultInputLocale.getDisplayName();
		}
		mSpaceSwipe = true;
		updateLanguages(mContext);
		int nextIndex = (mCurrentIndex + 1) % mEnabledLanguagesList.size();
		if (mEnabledLanguagesList.size() > nextIndex) {
			return mEnabledLanguagesList.get(nextIndex).mDisplayName;
		}
		return mDefaultInputLocale.getDisplayName();
	}
	/**
	 * Sets the system locale (display UI) used for comparing with the input language.
	 * @param locale the locale of the system
	 */
	public void setSystemLocale(Locale locale) {
		mSystemLocale = locale;
	}

	/**
	 * Returns the system locale.
	 * @return the system locale
	 */
	public Locale getSystemLocale() {
		return mSystemLocale;
	}

	/**
	 * Returns the previous input locale in the list. Wraps around to the end of the
	 * list if we're at the beginning of the list.
	 * @return Previous locale in language list
	 */
	public Locale getPrevInputLocale() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1) {
			return mDefaultInputLocale;
		}

		int prevIndex = (mCurrentIndex - 1 + mEnabledLanguagesList.size()) % mEnabledLanguagesList.size() ;
		if (mEnabledLanguagesList.size() > prevIndex) {
			return mEnabledLanguagesList.get(prevIndex).mLocale;
		}
		return mDefaultInputLocale;
	}

	public String getPrevLanguageDisplayName() {
		if (getLocaleCount() == 0 || mCurrentIndex == -1){
			return mDefaultInputLocale.getDisplayName();
		}
		mSpaceSwipe = true;
		updateLanguages(mContext);
		int prevIndex = (mCurrentIndex - 1 + mEnabledLanguagesList.size())
				% mEnabledLanguagesList.size();

		if (mEnabledLanguagesList.size() > prevIndex) {
			return mEnabledLanguagesList.get(prevIndex).mDisplayName;
		}
		return mDefaultInputLocale.getDisplayName();
	}

	public void reset() {
		// Currently no implementation. Just stub.
	}

	/* public boolean reset(boolean lastAddon) {
        if(lastAddon) {
     	   mEnabledLanguagesList.clear();
     	   mCurrentIndex = 0;
     	   loadDefaults();
     	   SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
 			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
 			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_LAST_ADDON_REMOVED, false);
 			sharedPrefEditor.commit();
 			return true;
        }
        return false;
     }*/

	/**
	 * Set the next language in list as selected
	 * Currently this is never called.
	 */
	public void next() {
		if (getLocaleCount() == 0) {
			return; //return when mEnableLangList is empty
		}
		mCurrentIndex++;
		if (mCurrentIndex >= mEnabledLanguagesList.size()){
			mCurrentIndex = 0; // Wrap around
		}
		updatePriorityInCore();
	}

	/**
	 * Set the previous language in list as selected
	 * Currently this is never called.
	 */
	public void prev() {
		if (getLocaleCount() == 0) {
			return; //return when mEnableLangList is empty
		}
		mCurrentIndex--;
		if (mCurrentIndex < 0){
			mCurrentIndex = mEnabledLanguagesList.size() - 1; // Wrap around
		}
		updatePriorityInCore();
	}

	/**
	 * Set the supplied index as current selected index.
	 * @param currIndex current selected index.
	 */
	public void setCurrentIndex(int currIndex) {
		//	mCurrentIndex= mEnabledLanguagesList.indexOf(mPrioritizeAddonList.get(currIndex));
		mCurrentIndex = currIndex;
		updatePriorityInCore();
	}

	/**
	 * Returns current index
	 * @return current index
	 */
	public int getCurrentIndex() {
		return mCurrentIndex;
	}

	/**
	 * Commits in Shared preference
	 */
	/*public void persist() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor editor = sp.edit();
        editor.putString(KPTConstants.PREF_INPUT_LANGUAGE, getInputLanguage());
        editor.commit();
    }*/

	/**
	 * Converts the supplied string to upper case
	 * @param s string to be converted to upper case
	 * @return case converted string
	 */
	static String toTitleCase(String str) {
		if (str.length() == 0) {
			return str;
		}

		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	public boolean isLatinScript() {
		if (getLocaleCount() == 0) {
			return false; //return false when mEnableLangList is empty
		}
		if (mEnabledLanguagesList.size() <= mCurrentIndex) {
			return false;
		}

		if(LATIN_SCRIPT.equals(mEnabledLanguagesList.get(mCurrentIndex).mScript)) {
			return true;
		} else {
			return false;
		}
	}

	public String getScript() {
		//if (getLocaleCount() == 0) return mEnabledLanguagesList.get(mCurrentIndex).mScript ; //return latin script when mEnableLangList is empty
		if(mCurrentIndex != -1 && mEnabledLanguagesList != null && mEnabledLanguagesList.size() > mCurrentIndex ){
			return mEnabledLanguagesList.get(mCurrentIndex).mScript;
		}
		return null;
	}

	public void setShoudLoadEngLocale(boolean mShouldLoadEngLocale) {
			
		mShoudDisplayEngLocale = mShouldLoadEngLocale;
	}
	
	public boolean getShouldLoadEngLocale(){
		return mShoudDisplayEngLocale;
	}
}