package com.kpt.adaptxt.beta;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.util.FontsUtill;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.util.KPTKXmlParser;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;

public class KPTAdaptxtAddonSettings implements KPTCoreServiceListener, AdaptxtSettings{
	Context mContext;

	private SharedPreferences mSharedPreference;

	public static final String XML_CONTENT_TAG = "content";
	//public static final String BASE_VERSION_FILE_NAME = "version";
	public static final String XML_CONTENTS_TAG = "contents";
	public static final String XML_ADDON_TAG = "addon";
	public static final String XML_DICT_DISPLAY_NAME = "displayname";
	public static final String XML_DICT_FILE_NAME = "filename";
	public static final String XML_ZIP_FILENAME = "zipfilename";
	public static final String XML_SEARCH = "searchstring";
	public static final String XML_TYPE = "type";
	public static final String XML_ATTR_NO = "no";
	public static final String XML_BASE_URL = "baseurl";

	private static final String S3_COMPATABILITY_XML = "addoncompatibiltiymatrix.xml";
	
	public static final String ACCESS_KEY_ID = "AKIAJWNDOADATKUM2GMQ";
	public static final String SECRET_KEY = "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";
	public static final String S3_BUCKET_NAME = "adaptxtaddons";
	public static final String USER_AGENT = "Adaptxt";
	public static final String SUB_BUCKET ="";
	
	public static final int DOWNLOAD_RETRY_DELAY = 500; 
	public static final int DOWNLOAD_RETRY_ATTEMPTS = 3;

	public static String BASE_URL;

	public boolean settingsConnected;
	private KPTCoreServiceHandler mCoreServiceHandler;
	private KPTCoreEngine mCoreEngine;
	private static KPTLanguageSwitcher mLanguageSwitcher;

	List<KPTAddonItem> mXMLLanguageList = null;
	List<KPTAddonItem> mUnsupportedList = null;
	List<KPTAddonItem> mInstalledLanguges = null;
	
	ParseTask mParseAsync;
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
	private AdaptxtSettingsRegisterListener mListner;

	public KPTAdaptxtAddonSettings(Context context, AdaptxtSettingsRegisterListener listner/*, ATXAddonDownloadObserver addonListener*/) {
		mContext = context;

		mSharedPreference = PreferenceManager.getDefaultSharedPreferences(mContext);	

		this.mListner = listner;
		//mAddonListener = addonListener;
		mXMLLanguageList = new ArrayList<KPTAddonItem>();
		mUnsupportedList = new ArrayList<KPTAddonItem>();
		mInstalledLanguges = new ArrayList<KPTAddonItem>();
		
		mLanguageSwitcher = new KPTLanguageSwitcher(mContext);

		//mParseAsync = new ParseTask();
		//mParseAsync.execute();
		
		parseXML();

		mListner.coreEngineStatus(false);

		if(mCoreServiceHandler == null) {
			mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(mContext.getApplicationContext());
			mCoreEngine = mCoreServiceHandler.getCoreInterface();
			if(mCoreEngine != null) {
				initializeCore();
				mLanguageSwitcher.setCoreServicehandler(mCoreServiceHandler);
				mLanguageSwitcher.loadLocales(mContext);
				mListner.coreEngineStatus(true);
			}
		} 

		if(mCoreEngine == null) {
			mCoreServiceHandler.registerCallback(this);
		}

	}

	private void initializeCore() {
		mCoreEngine.initializeCore(mContext);
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		mCoreEngine = coreEngine;
		settingsConnected = true;

		if(mCoreEngine != null){
			mCoreEngine.initializeCore(mContext);

			mLanguageSwitcher.setCoreServicehandler(mCoreServiceHandler);
			mLanguageSwitcher.loadLocales(mContext);
			mListner.coreEngineStatus(true);
		}

		mListner.coreEngineService();
	}


	public void destroySettings(){
		if(mCoreEngine!=null) {
			mCoreEngine.forceDestroyCore();
		}

		if(mCoreServiceHandler!=null) {
			mCoreServiceHandler.unregisterCallback(this);
			mCoreServiceHandler.destroyCoreService();
		}
		mCoreEngine = null;
		mCoreServiceHandler = null;

		mXMLLanguageList = null;
	}

	public int listInstalledAddons(){
		return mCoreEngine.getInstalledDictionaries().length;
	}

	public List<KPTAddonItem> checkInstalledLanguages(){
		List<KPTAddonItem> enabledLanguagesList = new ArrayList<KPTAddonItem>();
		
		if(mCoreEngine != null){
			KPTParamDictionary[] dictionaryList = mCoreEngine.getInstalledDictionaries();

			for(KPTParamDictionary dictionary : dictionaryList) {
				KPTAddonItem languageItem = new KPTAddonItem();
				languageItem.setComponentId(dictionary.getComponentId());
				languageItem.setDisplayName(dictionary.getDictDisplayName());
				languageItem.setFileName(dictionary.getDictFileName());

				if (dictionary.isDictLoaded()) {
					enabledLanguagesList.add(languageItem);
				}/*else{
					languageItem.setDisplayName(getLanguageEngName(languageItem.getFileName()));
					mUnsupportedList.add(languageItem);
				}*/
			}
		}
		
		return enabledLanguagesList;
	}
	
	public List<KPTAddonItem> checkUnSupportLanguages(){
		List<KPTAddonItem> unupportLanguagesList = new ArrayList<KPTAddonItem>();
		
		if(mCoreEngine != null){
			KPTParamDictionary[] dictionaryList = mCoreEngine.getInstalledDictionaries();

			for(KPTParamDictionary dictionary : dictionaryList) {
				KPTAddonItem languageItem = new KPTAddonItem();
				languageItem.setComponentId(dictionary.getComponentId());
				languageItem.setDisplayName(dictionary.getDictDisplayName());
				languageItem.setFileName(dictionary.getDictFileName());

				if (!dictionary.isDictLoaded()) {
					languageItem.setDisplayName(getLanguageEngName(languageItem.getFileName()));
					unupportLanguagesList.add(languageItem);
				}
			}
		}
		return unupportLanguagesList;
	}
	
	private String getLanguageEngName(String fileName){
		String langEngName = null;
		
		if(fileName.equals("hinin") ){
			langEngName = "Hindi";
		}else if(fileName.equals("benin") ){
			langEngName = "Bengali";
		}else if(fileName.equals("gujin") ){
			langEngName = "Gujarati";
		}else if(fileName.equals("marin") ){
			langEngName = "Marathi";
		}else if(fileName.equals("tamin") ){
			langEngName = "Tamil";
		}else if(fileName.equals("telin") ){
			langEngName = "Telugu";
		}else if(fileName.equals("kanin") ){
			langEngName = "Kannada";
		}else if(fileName.equals("malin") ){
			langEngName = "Malayalam";
		}
		
		return langEngName;
	}

	/**
	 * 
	 * @return List of installed supported languages
	 */
	public List<KPTAddonItem> getInstalledLanguages(){
		return checkInstalledLanguages();
	}
	
	/**
	 * 
	 * @return List of unsupported languages
	 */
	public List<KPTAddonItem> getUnsupportedLanguagesList(){
		return checkUnSupportLanguages();
	}

	public String getCurrentLanguage(){
		return mLanguageSwitcher.getCurrentLanguageDisplayName();
	}

	public List<KPTAddonItem> getLanguagesList(){
		return mXMLLanguageList;
	}
	
	//API to change language.
	public void changeLanguage(KPTAddonItem addonItem){
		mLanguageSwitcher.updatePriorityInCore(addonItem.getComponentId());
	}
	
	
	
	/**
	 * Get the AddonItem to download ad start download.
	 * @param downlaodItem
	 */
	/*public void setAddonDownlaod(KPTAddonItem downlaodItem){
		//Log.e("VMC", "IN ADDON DOWNLAOD ------####-------> "+downlaodItem.getDisplayName());
		new DownloadAddOnTask(downlaodItem).execute();
	}*/


	public class ParseTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			parseXML();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

		}

	} 

	private void parseXML() {
		XmlPullParser parser = null;
		InputStream inputStream = null;
		try {
			AssetManager am = mContext.getAssets();
			inputStream = am.open(KPTConstants.ATX_ASSETS_FOLDER + S3_COMPATABILITY_XML);

			//File file = new File(mContext.getAssets().+"/" + KPTConstants.ATX_ASSETS_FOLDER +AddonDownloadActivity.S3_COMPATABILITY_XML);
			//inputStream = new FileInputStream(file);

			parser = new KPTKXmlParser();
			parser.setInput(inputStream, null);
			String displayname = null;
			String filename = null;
			String zipFilename = null;
			String addonId = null;
			KPTAddonItem addonItem = null;
			boolean done = false;

			try {
				int eventType = parser.getEventType();
				do {
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if(parser.getName().equals(XML_BASE_URL)) {
							BASE_URL = parser.nextText();
						}else if (parser.getName().equals(XML_ADDON_TAG)) {
							addonItem = new KPTAddonItem();
							addonId = parser.getAttributeValue(null, "id");
							addonItem.setAddonID(addonId);
						} else if (parser.getName().equals(XML_DICT_DISPLAY_NAME)) {
							displayname = parser.nextText();
							//Log.e("TAG-MS", "************************************************");
							//Log.e("TAG-MS", "DisplayName "+displayname);
							addonItem.setDisplayName(displayname);
							//mTapToDownloadAddonList.add(displayname);
						} else if (parser.getName().equals(XML_DICT_FILE_NAME)) {
							filename = parser.nextText();
							//Log.e("TAG-MS", "FileName "+ filename);
							addonItem.setFileName(filename.toLowerCase());
							//mNameMap.put(filename.toLowerCase(), displayname);
						} else if(parser.getName().equals(XML_ZIP_FILENAME)){
							zipFilename = parser.nextText();
							//Log.e("TAG-MS", "zipFilename "+ zipFilename);
							addonItem.setZipFileName(zipFilename);
						} /*else if (parser.getName().equals(XML_SEARCH)) {
							String search_word = parser.nextText();
							mAddonSearchTagsMap.put(displayname,search_word.split(":"));
						}else if (parser.getName().equals(XML_TYPE)) {
							type = parser.nextText();
							addonItem.setType(type);
							mLanguageAddonList.add(displayname);

						} else if(parser.getName().equals(XML_PRICE)){
							mPriceList.put(displayname, parser.nextText());
						}*//*	else if(parser.getName().equals(XML_CURRENT_VERSION)){
							addonCurrentVersion = parser.getAttributeValue(null, XML_ATTR_NO);
							addonData.setToBeDownloadedAddonLatestVersion(addonCurrentVersion);
							//Log.e("TAG-MS", "addonCurrentVersion "+addonCurrentVersion);
						}else if(parser.getName().equals(XML_COMP_BASE_VERSION)){
							compatibleVersion = parser.nextText();
							//Log.e("TAG-MS", "compatibleVersion "+compatibleVersion);
							compatibiltiyInfoMap.put(addonCurrentVersion, compatibleVersion.split(REGEX_PIPE));
						} else if(parser.getName().equals(XML_VERSION)){
							addonCurrentVersion = parser.getAttributeValue(null, XML_ATTR_NO);
							//Log.e("TAG-MS", "addonCurrentVersion "+addonCurrentVersion);
						} */

						break;
					case XmlPullParser.END_TAG:
						if (parser.getName().equals(XML_ADDON_TAG)) {
							mXMLLanguageList.add(addonItem);
							addonItem = null;
							displayname = null;
							filename = null;
							zipFilename = null;
							addonId = null;
						} else if (parser.getName().equals(XML_CONTENTS_TAG)) {
							done = true;
						} 
						break;
					}
					eventType = parser.next();

				} while (!done && eventType != XmlPullParser.END_DOCUMENT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 
	 * @return Map of ATR shortcut and respective expansion
	 */
	public HashMap<String, String> getATRList(){
		ArrayList<String> mShortcutList = mCoreEngine.getATRShortcuts();
		Collections.sort(mShortcutList);
		ArrayList<String> mExpansionList = mCoreEngine.getATRExpansions();

		HashMap<String, String> atrMap = new HashMap<String, String>();

		int i = 0;
		for (String string : mShortcutList) {
			atrMap.put(string, mExpansionList.get(i));
			i++;
		}
		
		if(atrMap.size() > 0 /*&& mUnsupportedList.size() > 0*/){
			atrMap = checkATRSuport(atrMap);
		}

		return atrMap;
	}
	
	public HashMap<String, String> checkATRSuport(HashMap<String, String> atrsList){
		HashMap<String, String> tempList = new HashMap<String, String>();
		tempList.putAll(atrsList);
		
		for (HashMap.Entry<String, String> entry : atrsList.entrySet()){
			//Log.e("KPT", entry.getKey() + "/" + entry.getValue());
			boolean support = FontsUtill.isSupported(mContext, entry.getKey());
			
			if(!support){
				//Log.e("VMC", "NOT SUPPORT ---> "+entry.getKey());
				tempList.remove(entry.getKey());
			}/*else{
				Log.e("VMC", "SUPPORT ---> "+entry.getKey());
			}*/
		}
		
		return tempList;
	}

	public int addATRShortcut(String shortCut, String expansion){
		if(shortCut != null && expansion != null){
			if(shortCut.length() < 3){
				return ATR_ERROR_SHORTCUT_SHORT;
			}else if(shortCut.length() > 3){
				return ATR_ERROR_SHORTCUT_LONG;
			}else if(expansion.length() < 3){
				return ATR_ERROR_EXPANSION_SHORT;
			}else{
				mCoreEngine.addATRShortcutAndExpansion(shortCut, expansion);
				mCoreEngine.saveUserContext();
				return KPT_SUCCESS;
			}
		}else{
			return ATR_ERROR_NULL;
		}
	}
	
	public int deleteATR(final String shortCut){
		if (null != mCoreEngine) { 
			if(shortCut.length() < 3){
				return ATR_ERROR_SHORTCUT_SHORT;
			}else if(shortCut.length() > 3){
				return ATR_ERROR_SHORTCUT_LONG;
			}else{
				boolean status = mCoreEngine.removeATRShortcutAndExpansion(shortCut);
				return status == true ? KPT_SUCCESS :  ATR_INTERNAL_ERROR;
			}
		}else{
			return ATR_INTERNAL_ERROR;
		}

	}
	
	public int deleteAllATR(){
		if (null != mCoreEngine) { 

			boolean status = mCoreEngine.removeAllATRShortcutAndExpansion(null);
			return status == true ? KPT_SUCCESS :  ATR_INTERNAL_ERROR;

		}else{
			return ATR_INTERNAL_ERROR;
		}

	}
	
	
	public void saveUserContext(){
		if (null != mCoreEngine) { 
			mCoreEngine.saveUserContext();
		}
	}
	
	// Getters
		/**
		 *  Returns the state of the Private mode is ON or OFF
		 *  
		 *  returns 0 if ON and 1 if OFF
		 */
		@Override
		public int getPrivateModeState() {
			if(mSharedPreference == null)
			{
				return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
			}
			boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false); 
			return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
		}

		@Override
		public int getGlideState() {
			if(mSharedPreference == null)
			{
				return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
			}
			boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true); 
			return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
		}

		@Override
		public int getAutoCapitalizationState() {
			if(mSharedPreference == null)
			{
				return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
			}
			boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_CAPITALIZATION, true); 
			return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
		}

		@Override
		public int getAutoSpacingState() {
			if(mSharedPreference == null)
			{
				return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
			}
			boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_SPACE, true); 
			return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
		}

		@Override
		public int getDisplaySuggestionsState() {
			if(mSharedPreference == null)
			{
				return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
			}
			boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE, true); 
			return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
		}

	@Override
	public int getDisplayAccentsState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_DISPLAY_ACCENTS, true); 
		return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
	}

	@Override
	public int getPopupOnKeyPressState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}

		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_POPUP_ON, true); 
		return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
	}

	@Override
	public int getSoundOnKeyPressState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_SOUND_ON, false); 
		return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
	}

	@Override
	public int getVibrateOnKeyPressState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_VIBRATE_ON, false); 
		return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
	}

	@Override
	public int getLongPressDuration() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		return	mSharedPreference.getInt(KPTConstants.PREF_KEY_POPUP,KPTConstants.DEFAULT_POPUP);
	}

	@Override
	public float getKeyPressSoundVolume() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		return mSharedPreference.getFloat(KPTConstants.PREF_KEY_SOUND,KPTConstants.DEFAULT_VOLUME);
	}

	@Override
	public int getKeyPressVibrationDuration() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		return mSharedPreference.getInt(KPTConstants.PREF_KEY_VIBRATION,KPTConstants.DEFAULT_VIBRATION);
	}

	@Override
	public String getPortraitKeyboardType() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED_STRING;
		}
		return mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);	
	}

	/*@Override
	public String getLandscapeKeyboardType() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED_STRING;
		}
		return mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);	
	}*/

	/*@Override
		public int getCurrentThemeId() {
			return  Integer.parseInt(mSharedPreference.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
		}*/

	// Setters
	// consider checking if it matches with the existing value, then do nothing
	// define constant for return values : success, failure :- due to so n so reasons

	@Override
	public int setPrivateModeState(int isPrivateModeOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		//setPrivateMode(isPrivateModeOn); set to core : refer AdaptxtIME.java : #9091		
		editor.putBoolean(KPTConstants.PREF_PRIVATE_MODE,(isPrivateModeOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setGlideState(int isGlideOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_glide_settings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_GLIDE,(isGlideOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setAutoCapitalizationState(int isAutoCapitalizationOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_AUTO_CAPITALIZATION,(isAutoCapitalizationOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setAutoSpacingState(int isAutoSpacingOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_AUTO_SPACE,(isAutoSpacingOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setDisplaySuggestionsState(int isDisplaySuggestionsOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_SUGGESTION_ENABLE,(isDisplaySuggestionsOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setDisplayAccentsState(int isDisplayAccentsOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_DISPLAY_ACCENTS,(isDisplayAccentsOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setPopupOnKeyPressState(int isPopupOnKeyPressOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_POPUP_ON,(isPopupOnKeyPressOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);	
	}

	@Override
	public int setSoundOnKeyPressState(int isSoundOnKeyPressOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_SOUND_ON,(isSoundOnKeyPressOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);	
	}

	@Override
	public int setVibrateOnKeyPressState(int isVibrateOnKeyPressOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_VIBRATE_ON,(isVibrateOnKeyPressOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setLongPressDuration(int duration) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// Refer : PopupDurationDialogPreferance.java : #97		
		editor.putInt(KPTConstants.PREF_KEY_POPUP,duration);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setKeyPressSoundVolume(float volume) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// Refer : SoundVolumeDialogPreferance.java : #97		
		editor.putFloat(KPTConstants.PREF_KEY_SOUND,volume);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setKeyPressVibrationDuration(int duration) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// Refer : VibrationDurationDialogPreferance.java : #96		
		editor.putInt(KPTConstants.PREF_KEY_VIBRATION,duration);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int setPortraitKeyboardType(String keyboardType) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting	
		editor.putString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,keyboardType);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}
	
	@Override
	public int setAutoCorrectionState(int isAutoCorrectitonOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_AUTOCORRECTION,(isAutoCorrectitonOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	/*@Override
	public int setLandscapeKeyboardType(String keyboardType) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,keyboardType);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}*/

	/**
	 * This is direct functionality Need to analysis
	 */
	@Override
	public int removeAccents() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		// TODO Auto-generated method stub
		return AdaptxtSettings.KPT_SUCCESS;
	}

	@Override
	public int setHideSuggestionBarState(int isHideSuggBarOn) {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		final SharedPreferences.Editor editor=mSharedPreference.edit(); 
		// found only on prefs_addvancesettings.xml, no where else they are setting		
		editor.putBoolean(KPTConstants.PREF_SUGGESTION_ENABLE, (isHideSuggBarOn == AdaptxtSettings.KPT_TRUE)?true:false);
		boolean isCommitSuccess = editor.commit();
		return (isCommitSuccess ? AdaptxtSettings.KPT_SUCCESS : AdaptxtSettings.KPT_ERROR_CAN_NOT_COMMIT);
	}

	@Override
	public int getHideSuggestionBarState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE, true); 
		return (retVal ? AdaptxtSettings.KPT_FALSE : AdaptxtSettings.KPT_TRUE);
		
	}

	@Override
	public int getAutoCorrectionState() {
		if(mSharedPreference == null)
		{
			return AdaptxtSettings.KPT_ERROR_NOT_INITIALIZED;
		}
		boolean retVal = mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, true); 
		return (retVal ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
	}
	
}
