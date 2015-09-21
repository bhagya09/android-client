/******************************************************************************
 * COPYRIGHT (c) 2010 KeyPoint Technologies (UK) Ltd. All rights reserved.
 *
 * The copyright to the computer program(s) herein is the property of KeyPoint
 * Technologies (UK) Ltd. The program(s) may be used and/or copied only with
 * the written permission from KeyPoint Technologies (UK) Ltd or in accordance
 * with the terms and conditions stipulated in the agreement/contract under
 * which the program(s) have been supplied.
 */
/**
 * @file    KPTCoreEngine.java
 *
 * @brief   Platform side interface to the core engine.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;

import com.kpt.adaptxt.beta.Keyboard;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.CurrentWord;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.KPTCorrectionInfo;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.KPTInputInfo;
import com.kpt.adaptxt.core.coreapi.KPTPackage;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTParamKeyMapLayout;
import com.kpt.adaptxt.core.coreapi.KPTParamKeymapId;
import com.kpt.adaptxt.core.coreapi.KPTTypes.KPTStatusCode;

/**
 * KPTCoreEngine provides the interface to the core engine functions
 * 
 * @author rktadikonda
 *
 */

public interface KPTCoreEngine {
	/*Correction mode values w.r.t core*/
	public static final int KPTCORRECTION_LOW = 2;
	public static final int KPTCORRECTION_MEDIUM = 1;
	public static final int KPTCORRECTION_AGGRESIVE = 0;
	
	/**
	 * Error codes in platform side for package installation operation
	 */
	public static final int PACKAGE_ALREADY_EXISTS = -1;
	
	public static final int PACKAGE_INSTALLATION_FAILED = -2;
	
	/**
	 * Initializes handle to the core.
	 * 
	 */
	public boolean initializeCore(Context clientContext);
	
	/**
	 * Activates current top priority dictionary keymap
	 * @return Setting keymap is successful.
	 */
	public boolean activateTopPriorityDictionaryKeymap();
	
	
	/**
	 * Gets the loaded top priority component id from installed dictionary list.
	 * @param installedPkgs Installed packages array
	 * @return top priority dictionary
	 */
	public KPTParamDictionary getTopPriorityDictionary() ;
	
	/**
	 * TO be called from package installer once any package is installed to update user loaded status.
	 */
	public void updateCoreUserLoadStatus(Context clientContext);
	
	/**
	 * Adds character entered to core by pre-inserting it to give auto/space corrections
	 * @param c - character entered
	 * @param isPrevSpace - is previous character space
	 * @param justAddedAutoSpace - previous added char is auto space or not.
	 * @return KPTCorrectionInfo if auto correction available else null 
	 */
	public KPTCorrectionInfo addChar(char c, boolean isPrevSpace, boolean justAddedAutoSpace, boolean isAccVisible,
								boolean atxOn);
	
	/**
	 * speacially for Thai -- Adds character entered to core by pre-inserting it to give auto/space corrections
	 * @param c - character entered
	 * @param isPrevSpace - is previous character space
	 * @param justAddedAutoSpace - previous added char is auto space or not.
	 * @return KPTCorrectionInfo if auto correction available else null 
	 */
	public KPTCorrectionInfo addCharThai(char c, boolean isPrevSpace, boolean justAddedAutoSpace, boolean isAccVisible,
								boolean atxOn);
	
	public boolean removeChar(boolean beforeCursor);
	
	/**
	 * Inserts string into the core.
	 * @param text - text to be entered into core
	 * @return true if insertion to core is success
	 */

	public boolean insertText(String text, boolean misAtxOn, boolean shouldlearn);
	
	/**
	 * Inserts string into the core.
	 * @param text - text to be entered into core
	 * @param setAddedSpace used for glide suggestion input
	 * @return true if insertion to core is success
	 */
	public boolean insertText(String text, boolean misAtxOn, boolean shouldlearn, boolean setAddedSpace);
	
	/**
	 * Gets suggestions from core.
	 * @return list of suggestions containing info about each type 
	 * of suggestion(error/space/normal etc)
	 */
	public List<KPTSuggestion> getSuggestions();
	
	/* Adding shortcut and expansion to core*/
	/**
	 * Reset core i.e., clean entered characters in Core.
	 * 
	 * @return
	 */
	public void addATRShortcutAndExpansion(String shortcut,String expansion);
	
	public ArrayList<String> getATRShortcuts();
	
	public ArrayList<String> getATRExpansions();
	
	public void setATRStatus(boolean status);
	
	public void getATRDictInfo();
	
	public String getAtrExpansion(String shortcut);
	
	public boolean removeATRShortcutAndExpansion(String shortcut);
	
	public boolean removeAllATRShortcutAndExpansion(ArrayList<String> shortcuts); 
	
	public String trimAccents(String data);

	public void forceDestroyCore();
	
	/**
	 * Destroys the core handle and releases all resources.
	 */
	public void destroyCore();
	
	/**
	 * Informing whether Editor is blackListed or not
	 * @param blisted - true/false
	 */
	public void setBlackListed(boolean blisted);
	
	/**
	 * Sets core in maintenance mode.
	 * This api used by package installer service.
	 * @param maintenance mode
	 */
	public void setCoreMaintenanceMode(boolean maintenanceMode);

	/**
	 * Reset core i.e., clean entered characters in Core.
	 * @return
	 */
	public boolean resetCoreString();

	/**
	 * Sets absolute position to core
	 * @param cursorPos - Position w.r.t startIndex in buffer
	 * @return true if success else false
	 */
	public boolean setAbsoluteCaretPosition(int cursorPos);
	
	/**
	 * Gets caret position in core
	 * @return caret Position
	 */
	public int getAbsoluteCaretPosition();
	
	/**
	 * Remove String from core
	 * @param beforeCursor - String to be deleted before cursor or after cursor.
	 * true if before cursor.
	 * @param positions - The number of characters to be deleted before/after
	 *  the current cursor position.
	 * @return true if success else false
	 */
	public boolean removeString(boolean beforeCursor, int positions);
	
	/**
	 * Replace String for specified number of positions
	 * @param startIndex - Start Index
	 * @param endIndex - End Index
	 * @param replaceString -  to be replaced String
	 * @return true if success else false
	 */
	public boolean replaceString(int startIndex, int endIndex, String replaceString);
	
	/**
	 * Logout request to core.
	 *//*
	public void logout();*/
	
	/**
	 * getCurrent word in Core
	 * @return previous String and after String w.r.t cursor
	 * Previous String contains only characters that the be set in composing mode.
	 */
	public CurrentWord getCurrentWord();
	
	/**
	 * getCurrent word in Core
	 * @return previous String and after String w.r.t cursor
	 * Previous String contains only characters that the be set in composing mode.
	 */
	public CurrentWord getCurrentWordforDelete();
	
	/**
	 * getPrefixLength from core
	 * @return get the length of prefix text before cursor
	 */
	public int getPrefixLength();
	
	/**
	 * getSuffixLength from core
	 * @return get the length of suffix text after cursor
	 */
	public int getSuffixLength();
	
	/**
	 * Set Caps Mode w.r.t states as input.
	 * @param state - KPT_SUGG_STATES enum gives information about states.
	 */
	public void setCapsStates(KPT_SUGG_STATES state);
	
	/**
	 * Set maximum number of Suggestions to be retrieved from Core
	 * @param maxSugg - Max.Suggestions
	 */
	public void setMaxSuggestions(int maxSugg);
	/**
	 * Set maximum number of non dictionary words that can be stored
	 * @param maxLimit - Max.non dictionary words
	 */
	public boolean setUserDictionaryWordLimit(int maxLimit) ;
	/**
	 * Error Correction on/off
	 * @param errorCorrect - true/false
	 * @param autoCorrect - autoCorrection true/false
	 * @param correctionMode - KPTCORRECTION_LOW/KPTCORRECTION_MEDIUM/KPTCORRECTION_AGGRESSIVE
	 * @param spaceCorrectionMode - on/off
	 */
	public void setErrorCorrection(boolean errorCorrect, boolean autoCorrect, int correctionMode, boolean spaceCorrectionMode);


	/**
	 * Returns the reverted word
	 * @return reverted String
	 */
	public String getRevertedWord();
	
	/**
	 * Inserts Suggestion clicked from Candidate View
	 * @param suggestion - Suggestion String pressed
	 * @param id - SuggId w.r.t. core
	 * @param autoSpace - true if auto spacing to be done after inserting suggestion else false
	 * @return - CorrectionInfo after suggestion has been inserted
	 */
	public KPTCorrectionInfo insertSuggestion(String suggestion, int id, boolean autoSpace);
	
	/**
	 * Brings the core in the sync with platform.
	 * @param textBuffer Buffer to set in the core
	 * @param cursorPosition The new cursor position to set in core
	 * @param languagechange true for toggle change
	 * @return If updating core buffer and cursor position is success
	 */
	boolean syncCoreBuffer(String textBuffer, int cursorPosition,boolean languageChange);
	/**
	 * Brings the core in the sync with platform.
	 * @param textBuffer Buffer to set in the core
	 * @param cursorPosition The new cursor position to set in core
	 * @return If updating core buffer and cursor position is success
	 */
	boolean syncCoreBuffer(String textBuffer, int cursorPosition);

	
	/**
	 * Retruns the text buffer stored in Core side.
	 * @return The text buffer returned from core. If error returns NULL.
	 */
	public String getCoreBuffer();
	
	/**
	 * Gets available addon atp packages in default package folder. "/profile/package".
	 * @return packages list
	 */
	public KPTPackage[] getAvailablePackages();
	
	/**
	 * Gets installed addon atp packages in core.
	 * @return packages list
	 */
	public KPTPackage[] getInstalledPackages();
	
	/**
	 * Installs supplied package from default package folder.
	 * @param packageName Package name to be installed.
	 * @return Installed packages's id
	 */
	public int installAddOnPackage(String packageName);
	
	/**
	 * Uninstalls a package from core
	 * @param packageId package Id to uninstall
	 * @return Success or Failure
	 */
	public boolean uninstallAddonPackage(int packageId);
	
	/**
	 * Licenses add-on files from the key in license file.
	 * @param licenseFilePath Absolute path of license file.
	 * @return Licensing add-on succeeded or failed. 
	 */
	public boolean licenseAddOn(String licenseFilePath);
	
	/**
	 * Gets list of available components and its details.
	 * @return List of components and their info
	 */
	public KPTParamComponentInfo[] getAvailableComponents();
	
	/**
	 * Gets list of loaded components and its details.
	 * @return List of components and their info
	 */
	public KPTParamComponentInfo[] getLoadedComponents();

	/**
	 * Gets the list of installed dicionaties in the core
	 * 
	 * @return Dictionary List
	 */
	public KPTParamDictionary[] getInstalledDictionaries();
	
	/**
	 * Changes priority of installed dictionaries
	 * @param componentId component id of dictionary to change priority.
	 * @param priority priority to be set. (Priority starts from zero index)
	 * @return If changing priority is success or not.
	 */
	public boolean setDictionaryPriority(int componentId, int priority);
	
	/**
	 * Loads and enables a Dictionary.
	 * @param componentId Component Id of the dictionary to be loaded.
	 * @return Success or Failure.
	 */
	public boolean loadDictionary(int componentId);
	
	/**
	 * Unloads and disables a dictionary.
	 * @param componentId Component Id of the dictionary to be unloaded.
	 * @return Success or Failure.
	 */
	public boolean unloadDictionary(int componentId);
	
	/**
	 * Get available keymaps in core
	 * @return Available Keymap id list
	 */
	public KPTParamKeymapId[] getAvailableKeymaps();
	
	/**
	 * Get opened keymaps list in core.
	 * @return Opened keymap list
	 */
	public KPTParamKeymapId[] getOpenKeymaps();
	
	/**
	 * Get current active keymap in core.
	 * @return Active keymap
	 */
	public KPTParamKeymapId[] getActiveKeymap();
	
	/**
	 * Get layout associated with a keymap in core.
	 * @param keymapId keymap for which layout had to be fetched
	 * @return Keymap layout associated with keymap
	 */
	public KPTParamKeyMapLayout getLayoutForKeymap(KPTParamKeymapId keymapId);
	
	/**
	 * Opens and loads a keymap layout in core.
	 * @param keymapLayout Keymap layout to be loaded in core.
	 * @return True if success
	 */
	public boolean openKeymap(KPTParamKeymapId keymapId);
	
	/**
	 * Sets active keymap in core.
	 * @param keymapId Keymap id to be set active.
	 * @return True if success
	 */
	public boolean setActiveKeymap(KPTParamKeymapId keymapId);
	
	/**
	 * Activates the keymap corresponding to the particular language dictionary component.
	 * @param componentId Component Id of the dictionary whose language keymap has to be acticvated.
	 * @param currentLanguageDict [Optional param else supply null]
	 * 		Language Dictionary whose corresponding keymap has to be activated.
	 * @return If activation is success.
	 */
	public boolean activateLanguageKeymap(int componentId, KPTParamDictionary currentLanguageDict);
	
	/**
	 * Saves User Context in Core(Saves user added words to core User Dictionary.)
	 */
	public void saveUserContext();
	
	/***
	 * Gives Information whether user is at start of sentence or not
	 * @return true if start of Sentence
	 */
	public boolean isStartOfSentence();
	
	/**
	 * Gets current suggestion list's status code
	 * @return Sugg status code
	 */
	public KPTStatusCode getSuggStatusCode();
	
	
	void resetNextCaps();
	
	public String[] getUserDictionary();
	
	public String[] getUserDictionary(int startOffset, int endOffset, boolean finalOffset);
	
	public KPTStatusCode closeDictPages();
    
    public boolean addUserDictionaryWords(String[] words);
    
    public boolean removeUserDictionaryWords(String[] words); 
    
    public boolean removeAllUserDictionaryWords();
    
   // public boolean editUserDictionaryWord(String oldWord, String newWord);
    
    public boolean isCoreUserLoaded();

	public boolean addUserDictionaryWords(String stringBuffer);
	
	public boolean learnBuffer(String string);
	
	//public boolean learnBufferWithPrunning(String stringBuffer, int stringBufferLength);
	
	public String getComposingString();
	
	public String getCurrentWordComposition();
	
	public boolean updateCurrentWord();
	
	public int getSuggestionOffset();
	
	public String getPredictiveWord();
	
	
	
	public void setPrivateMode(boolean privateModeOnOff);
	
	public boolean saveLayoutForKeyMap(KPTParamKeyMapLayout keyMapLayout);
	
	public boolean setLayoutForKeyMap(KPTParamKeyMapLayout keyMapLayout);
	
	public int getUserDictionaryTotalCount();
	
	/**
	 * This is a Thai-specific command for manually freezing the Thai text
	 * entered.
	 */
	public boolean executeThaiCommit();
	
	public int getComposingStringLength();
	
	public KPTInputInfo getInputInfo();
	
	
	/**
	 * This method is used to add the char to core. 
	 * We are using this method to move all the dependency to core engine 
	 * instead of handling on the platform side.
	 * @param char to be added to the core
	 * @return KPTCorrectionInfo 
	 */
	public KPTCorrectionInfo addCharTwo(char c,boolean isAtxOn);
	
	/**
	 * Used while  re-initialization of Core fails.
	 * On retry if everything fails we delete/clear the internal data.
	 * 
	 * @param filePath
	 * @param assetMgr
	 */
	public void prepareCoreFilesForRetry(String filePath, AssetManager assetMgr);
	
	/**
	 * get Glide suggestions from core
	 * @return
	 */
	public List<KPTSuggestion> getGlideSuggestions();
	
	/**
	 * set the current layout to core like (x, y) coordinates of each key, its label etc...
	 * @param keyboard
	 */
	public void setLayoutToCore(Keyboard keyboard);
	
	/**
	 * send the glided coordinates to core
	 * @param coords
	 * @param option
	 */
	public void setGlideCoords(float[] xCoordinates, float[] yCoordinates, int option);
	
	
	
	
	public void setAppContextEnabled(boolean config);
	
	public void setAppContextAppName(String appName,int type);

	boolean learnTrend(String str);

	boolean UnLearnTrend(String str);

	void setPunctuationPrediction(boolean punctPrediction);
	
	
	/*
	 * 
	 * Loation related API 
	 */
	
	public boolean loadLocation(String path,String id);
	
	public boolean unLoadLocation(String path,String id);

	void setAutoSpaceEnable(boolean autoEnable);
	
//To send the app type which we are LEARNING
	boolean learnBufferWithPrunning(String stringBuffer,
			int stringBufferLength, int apptype);

	
}