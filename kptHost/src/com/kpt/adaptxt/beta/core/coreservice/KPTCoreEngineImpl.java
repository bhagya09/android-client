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
 * @file    KPTCoreEngineImpl.java
 *
 * @brief   Platform side handler of the core engine.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kpt.adaptxt.beta.KPTLog;
import com.kpt.adaptxt.beta.Keyboard;
import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.core.coreapi.KPTCommands.KPTCmd;
import com.kpt.adaptxt.core.coreapi.KPTCore;
import com.kpt.adaptxt.core.coreapi.KPTFrameWork;
import com.kpt.adaptxt.core.coreapi.KPTGlideSuggRequest;
import com.kpt.adaptxt.core.coreapi.KPTInpMgrInsertChar;
import com.kpt.adaptxt.core.coreapi.KPTKeymapKeyInfo;
import com.kpt.adaptxt.core.coreapi.KPTKeymapRow;
import com.kpt.adaptxt.core.coreapi.KPTLanguage;
import com.kpt.adaptxt.core.coreapi.KPTPackage;
import com.kpt.adaptxt.core.coreapi.KPTParamATRDictInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamATRInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamAppContextInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamBase;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo.KPTLicenseStateT;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentsOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamDictOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTParamInputBuffer;
import com.kpt.adaptxt.core.coreapi.KPTParamInputComposition;
import com.kpt.adaptxt.core.coreapi.KPTParamInputConfig;
import com.kpt.adaptxt.core.coreapi.KPTParamInputCoreSync;
import com.kpt.adaptxt.core.coreapi.KPTParamInputCursor;
import com.kpt.adaptxt.core.coreapi.KPTParamInputInsertcharWithAC;
import com.kpt.adaptxt.core.coreapi.KPTParamInputInsertion;
import com.kpt.adaptxt.core.coreapi.KPTParamInputPreInsert;
import com.kpt.adaptxt.core.coreapi.KPTParamInputResetRemoveReplace;
import com.kpt.adaptxt.core.coreapi.KPTParamKeyMapLayout;
import com.kpt.adaptxt.core.coreapi.KPTParamKeyMapOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamKeymapId;
import com.kpt.adaptxt.core.coreapi.KPTParamLayoutDetails;
import com.kpt.adaptxt.core.coreapi.KPTParamLearnFromFile;
import com.kpt.adaptxt.core.coreapi.KPTParamLearning;
import com.kpt.adaptxt.core.coreapi.KPTParamLicense;
import com.kpt.adaptxt.core.coreapi.KPTParamPackageInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamPersonalDict;
import com.kpt.adaptxt.core.coreapi.KPTParamPersonalDictWordIdOperations;
import com.kpt.adaptxt.core.coreapi.KPTParamSuggestion;
import com.kpt.adaptxt.core.coreapi.KPTParamSuggestionConfig;
import com.kpt.adaptxt.core.coreapi.KPTParamUserInfo;
import com.kpt.adaptxt.core.coreapi.KPTParseFile;
import com.kpt.adaptxt.core.coreapi.KPTSuggEntry;
import com.kpt.adaptxt.core.coreapi.KPTTypes.KPTStatusCode;

/**
 * KPTCoreEngineImpl handles the Core engine object from JNI layer. Also provides
 * singleton reference to core engine handle.
 * 
 * @author 
 *
 */

public class KPTCoreEngineImpl implements KPTCoreEngine {

	/**
	 * The JNI core engine handle using which all Core engine function calls are made
	 */
	private KPTCore  mAdaptxtCore;

	/**
	 * Static reference to this class to provide single instance across binders.
	 */
	private static KPTCoreEngineImpl sCoreEngine = null;

	/**
	 * Static counter to keep track of instances and releasing core engine framework on
	 * destruction of last instance.
	 */
	public static int mReferenceCount = 0;

	private String apkDirPath = null;

	private boolean mATRStatus = false;
	/**
	 * Blacklisting flag is set by IME based on editor
	 */
	private volatile boolean mblisted = false;

	/**
	 * Set by package installer to put core in maintenance mode
	 */
	private volatile boolean mIsCoreMaintanenceMode = false;

	/**
	 * This flag is set to true only after a user is properly loaded.
	 */
	private volatile boolean mIsCoreUserLoaded = false;

	private char Thai_word_separater = 't';

	private static final int LEARNCONTEXT = 0;


	public int mPrefixLength =0 ;
	public int mSuffixLength = 0;
	public int mCompStrLength = 0;	
	public String mCompString = null;

	private static int MAX_NO_WORDS_MYDICTIONARY = 1500;
	private KPTParamUserInfo user;

	//KPTCMD_LICENSE_FRAMEWORK
	private byte [] UserRef ={'A','P','I','C','u','s','t','o','m','e','r'};	
	/*private byte[] licenseKey = {'U','6','I','A','J','6','H','B','3','6','R','E','C','7','H','L','A','M','H','B',
			'F','8','7','K','Q','B','U','7','V','B','Y','5','\n'};*/
	byte [] licenseKey = {'7','B','V','C','4','8','H','B','3','6','R','E','C','7','H','L','A','M','H','B','I','G','7','K','Q','B','U','7','C','L','Q','A','\0'};

	private static final String licenseFilePath = "/TestUser/Dictionaries/licence.dat";

	private KPTParamBase mLicenseInfo;

	/* Reverted String*/
	private String mRevertedWord = "";

	/* Flag for maintaining auto Correction is on/off */
	private boolean mAutoCorrection = false;

	/* Maintained globally as it is used for inserting suggestions.*/
	private KPTParamSuggestion mGetSuggs = null;

	/**
	 * Suggestion offset of the current word
	 */
	private int mSuggestionOffset = 0;

	private int mSuggOffset = 0;

	/**
	 * Application context for making context calls
	 */
	private static Context mApplicationContext;

	/**
	 * Global suggestion status code value
	 */
	private KPTStatusCode mSuggStatusCode;

	private static String KPTDebugString = "KPTCoreEngineImpl";

	/**
	 * integer to indicate whether core should learn the context or not.
	 */
	private int mInsertOptions;

	private int mInsertSuggestionOptions;

	private int mNoOfRetries = 0;


	private KPTSuggEntry[] mGlideSuggestionEntries;


	/**
	 * Default constructor
	 */
	private KPTCoreEngineImpl() {
		mLicenseInfo = new KPTParamLicense(KPTCmd.KPTCMD_LICENSE.getBitNumber(), UserRef, licenseKey, null, licenseKey.length,1);

	}

	/**
	 * Static getter function to provide single instance to all binders.
	 */
	public static KPTCoreEngineImpl getCoreEngineImpl() {
		if (sCoreEngine == null) {
			sCoreEngine = new KPTCoreEngineImpl();
		}
		return sCoreEngine;
	}

	/**
	 * Copies required core files from assets to the profile folder.
	 * @param filePath
	 * @param assetMgr
	 */
	public void prepareCoreFiles(String filePath, AssetManager assetMgr) {
		apkDirPath = filePath +"/Profile" ;
		atxAssestCopy(filePath, assetMgr);
		apkDirPath = apkDirPath + "/Profile";
		mLicenseInfo = new KPTParamLicense(KPTCmd.KPTCMD_LICENSE.getBitNumber(), UserRef,licenseKey, null, licenseKey.length,1);
	}


	/**
	 * Copies required core files from assets to the profile folder.
	 * @param filePath
	 * @param assetMgr
	 */
	public void prepareCoreFilesForRetry(String filePath, AssetManager assetMgr) {
		prepareCoreFiles(filePath, assetMgr);
	}


	private KPTParamPersonalDict pDict;

	private String TAG = KPTCoreEngineImpl.class.getSimpleName();
	/**
	 * Initializes handle to the core. 
	 * 
	 */
	public boolean initializeCore(Context clientContext) {

		boolean retVal = true;
		synchronized (this) {
			mNoOfRetries  = 0;
			retVal = initCore(clientContext);
			if(retVal){
				mReferenceCount++;
			}
			if(retVal && !mIsCoreUserLoaded) {
				updateCoreUserLoadStatus(clientContext);
			}
		}
		//sharedPrefEditor.edit().putBoolean(KPTConstants.PREF_TEMP, false).commit();
		return retVal;
	}

	private boolean initCore(Context clientContext) {
		boolean retVal = true;
		mNoOfRetries++;
		//KPTLog.e(KPTDebugString, "initializeCore mReferenceCount--before " + mReferenceCount);
		/* We are synchronizing the current instance in this function to make that the Core initialization
		 * processing of one thread doesn't interfere with a possible Core destruction processing of
		 * another thread. Currently this issue was identified during system locale change which initiates
		 * the above conflict between KPTLocaleHandlerService and KPTPackageInstallerService.
		 */
		SharedPreferences.Editor sharedPrefEditor = PreferenceManager
				.getDefaultSharedPreferences(clientContext).edit();
		sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_INITIALIZED_FIRST_TIME, true);
		sharedPrefEditor.commit();
        Log.e("KPT", "mReferenceCount "+mReferenceCount);
		if (mReferenceCount == 0) {
			mAdaptxtCore = new KPTCore();

			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkCreate(0, 1,apkDirPath, true);
			Log.e("KPT", "statuscode "+statuscode);
			if (statuscode == KPTStatusCode.KPT_SC_SUCCESS || statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
				KPTFrameWork fwk = new KPTFrameWork();
				mAdaptxtCore.KPTFwkGetVersion(fwk);
				if (licenseFramework()) {
					/*if (loginUser(mLicenseInfo, clientContext)) {
					} else {// LOGIN_USER FAILED
						retVal = false;
					}*/
				} else {
					Log.e("KPT", "license failed");
					retVal = false;
				}
			} else { //KPTFwkCreate FAILED
				retVal = false;
			}
		}
		//boolean retVal = isCoreUserLoaded();
		//Log.e(KPTDebugString, "retVal-->"+retVal);
		//retVal = false;

		if(!retVal && mNoOfRetries < KPTConstants. NO_OF_RETRY_INSTALLATION) {
			Log.e(KPTDebugString, "initCore Failed, retrying " +mNoOfRetries);
			/**
			 *  bug fix for #14781. Empty Suggestion bar is displayed and installed Add-ons are disappeared on overwriting SP V2.2.1 with trail
			 *  There is a delay in setting upgrade flag in broadcast reciver,
			 *   so replacing the new profile directory in upgrade when the initialization failed for initial attempt.
			 */
			if(mNoOfRetries == 1){
				/**
				 * If core initialization failed, 
				 * for first retry unziping the latest profile.zip from assets. 
				 * This only override existing files and merge the file directory
				 */
				sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, true);
				sharedPrefEditor.commit();
				prepareCoreFilesForRetry(clientContext.getFilesDir().getAbsolutePath(),clientContext.getAssets() );
			}
			try {
				Thread.sleep(KPTConstants.INSTALLATION_RETRY_DELAY);
				retVal = initCore(clientContext);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return retVal;

	}




	/**
	 * Sets the application context reference. Called only by CoreService.
	 */
	public void setApplicationContext(Context appContext) {
		mApplicationContext = appContext;
	}

	/**
	 * license framework with key
	 * @return true if success else false
	 */
	private boolean licenseFramework() {				
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LICENSE_FRAMEWORK, mLicenseInfo);

		return (statuscode == KPTStatusCode.KPT_SC_SUCCESS) ? true: false;	    
	}

/*	private boolean loginUser(KPTParamBase  licenseInformation, Context clientContext) {
		boolean retVal = true;
		KPTStatusCode statuscode = null;

		String username = apkDirPath +"/TestUser";
		user = new KPTParamUserInfo(1, username, 1234);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_CREATE, user);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS || statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_LOGIN, user);
			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {  
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LICENSE_WITHKEY, mLicenseInfo);
				if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {

				}else {// KPTCMD_LICENSE_WITHKEY FAILED
					//Log.e(KPTDebugString, "KPTCMD_LICENSE_WITHKEY FAILED-->"+statuscode);
				}
			} else {//USER_LOGIN FAILED
				//Log.e(KPTDebugString, "KPTCMD_USER_LOGIN FAILED-->"+statuscode);
				retVal = false;
			}
		} else {// USER_CREATE FAILED
			//Log.e(KPTDebugString, "KPTCMD_USER_CREATE FAILED-->"+statuscode);
			retVal = false;
		}
		return retVal;
	}*/
	/**
	 * Login User w.r.t License info
	 * @param licenseInformation - license key
	 * @return
	 */
	/*private boolean loginUser(KPTParamBase  licenseInformation, Context clientContext) {
		String username = apkDirPath +"/TestUser";
		user = new KPTParamUserInfo(1, username, 1234);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_CREATE, user);

		//Log.e(KPTDebugString, "KPTCMD_USER_CREATE status: 111111111 "+ statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS || statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_USER_CREATE status: "+ statuscode);
		}
		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_LOGIN, user);
		//Log.e(KPTDebugString, "KPTCMD_USER_LOGIN status: 22222222222 "+ statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {

			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LICENSE_WITHKEY, mLicenseInfo);

			//Log.e(KPTDebugString, "Base License KPTCMD_LICENSE_WITHFILE 333333333333333 "+ "statuscode is   " + statuscode);
			//Log.e(KPTDebugString, "4444444444444 "+ " isCoreUserLoaded   " + !isCoreUserLoaded());

			if(!isCoreUserLoaded()) {

				boolean isPkgInstalled = isAnyValidDictionaryPresentinCore();
				//Log.e(KPTDebugString, "5555555555555 "+ " isCoreUserLoaded   " + isPkgInstalled);
				if(!isPkgInstalled) {
					// No packages are installed yet. User can not be loaded.
					return false;
				}
				else {
					// Already few packages are installed.
					// Carry on with user load
					mIsCoreUserLoaded = true;
				}
			}

			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_LOAD, null);
			//Log.e(KPTDebugString, "KPTCMD_USER_LOAD 333333333333 " + " login user status code is  " + statuscode);

			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {

				// Once user load is success and user is logging for first time
				// set the keymap of highest priority dictionary as active
				activateTopPriorityDictionaryKeymap();

				setCoreMaintenanceMode(false);
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(clientContext);
				SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
				sharedPrefEditor.commit();
			} else {
				setCoreMaintenanceMode(true);
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(clientContext);
				SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);
				sharedPrefEditor.commit();
			}


		} else {
			//Log.e(KPTDebugString, "KPTCMD_USER_LOGIN failed status: "+ statuscode);
		}
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		return false;
	}*/

	/**
	 * Activates current top priority dictionary keymap
	 * @return Setting keymap is successful.
	 */
	@Override
	public boolean activateTopPriorityDictionaryKeymap() {
		KPTParamDictionary activeDictionary = getTopPriorityDictionary();
		if(activeDictionary != null) {
			return activateLanguageKeymap(activeDictionary.getComponentId(), activeDictionary);
		}
		return false;
	}

	/**
	 * Gets the loaded top priority component id from installed dictionary list.
	 * @param installedPkgs Installed packages array
	 * @return top priority dictionary
	 */
	public KPTParamDictionary getTopPriorityDictionary() {
		KPTParamDictionary  topPriorityDict = null;
		int priorityRank = Integer.MAX_VALUE; 
		KPTParamDictionary[] dictList = getInstalledDictionaries();
		if(dictList != null && dictList.length > 0) {
			for(KPTParamDictionary installedDict : dictList) {
				if(installedDict.isDictLoaded() &&
						installedDict.getDictPriority() < priorityRank) {
					priorityRank = installedDict.getDictPriority();
					topPriorityDict = installedDict;
				}
			}
		}
		return topPriorityDict;
	}

	/**
	 * TO be called from package installer once any package is installed to update user loaded status.
	 */
	@Override
	public void updateCoreUserLoadStatus(Context clientContext) {
		//KPTLog.e(KPTDebugString, "updateCoreUserLoadStatus->");
		boolean isAnyValidDictPresent = isAnyValidDictionaryPresentinCore();



		// For first time package installation update core maintenance flagsif any valid dictionary is present.
		if(!isCoreUserLoaded()) {
			if(!isAnyValidDictPresent) {
				//Log.e(KPTDebugString, "updateCoreUserLoadStatus-->No packages installed, User cannot be loaded");
				// No packages are installed yet. User can not be loaded.
				//retVal = false;
//				mNoOfLoadRetries--;
				return ;//retVal;
			}
			else {
				mIsCoreUserLoaded = true;
				// Already few packages are installed.
				// Carry on with user load
				/*KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_LOAD, null);
				Log.e(KPTDebugString, "$$$ KPTCMD_USER_LOAD " + statuscode);
				mIsUserLoadFailed = false;*/
//				if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
					// Set maintenance mode off. User load success.
					setCoreMaintenanceMode(false);
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(clientContext);
					SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
					sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
					sharedPrefEditor.commit();
					mIsCoreUserLoaded = true;
					activateTopPriorityDictionaryKeymap();
				/*}else {
					retVal = false;
					mIsUserLoadFailed = true;
					googleSendCoreEngineErrorCodes(statuscode.name()+"", "updateCoreUserLoadStatus");
				}*/
			}
		} else {
			if(isAnyValidDictPresent) {
				// User is loaded. Switch off maintenance mode which could be set during package installation.
				setCoreMaintenanceMode(false);
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(clientContext);
				SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
				sharedPrefEditor.commit();
			} else {
				// Probably due to uninstallation of all packages,
				// core needs to be set back to maintenance mode.
				// and unload user.
				mIsCoreUserLoaded = false;
				setCoreMaintenanceMode(true);
				KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_SAVE , this.user);
				//KPTLog.e(KPTDebugString, "KPTCMD_USER_SAVE- " + statuscode  );
//				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_UNLOAD , this.user);
				//KPTLog.e(KPTDebugString, "KPTCMD_USER_UNLOAD- " + statuscode  );
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(clientContext);
				SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);
				//sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_LAST_ADDON_REMOVED, true);
				sharedPrefEditor.commit();
			}
		}
	}

	/**
	 * Returns if any valid dictionary with license is present.
	 * @return If valid dict present.
	 */
	private boolean isAnyValidDictionaryPresentinCore() {
		//KPTLog.e(KPTDebugString, "isAnyValidDictionaryPresentinCore->");
		boolean isAnyValidDictPresent = false;
		KPTPackage[] installedPkg = getInstalledPackages();

		if(installedPkg != null && installedPkg.length != 0) {
			KPTParamComponentsOperations componentOps = new KPTParamComponentsOperations(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_GETAVAILABLE, componentOps);
			//Log.e(KPTDebugString, "KPTCMD_COMPONENTS_GETAVAILABLEs " + statuscode);
			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){

				KPTParamComponentInfo[] compInfoList = componentOps.getAvailComponents();
				//Log.e(KPTDebugString, "componentOps.getAvailComponents() is null " + compInfoList);
				if(compInfoList != null) {
					//Log.e(KPTDebugString, "componentOps.getAvailComponents() length is " + compInfoList.length);
					for(KPTParamComponentInfo compInfo : compInfoList) {
						int licenseState = compInfo.getLicenseState().getLicenseStateInt();

						if(licenseState != KPTLicenseStateT.eKPTLicenseUnlimited.getLicenseStateInt() ||
								licenseState != KPTLicenseStateT.eKPTLicenseLimitedValid.getLicenseStateInt()) {
							isAnyValidDictPresent = true;
							break;
						}
					}
				}
			}
		}
		return isAnyValidDictPresent;
	}
	public KPTCorrectionInfo addCharThai(char c, boolean isPrevSpace, boolean justAddedAutoSpace, boolean isAccVisible,
			boolean isAtxOn)
	{
		KPTCorrectionInfo info = null;
		KPTStatusCode statuscode = KPTStatusCode.KPT_SC_ERROR;
		if(mblisted || mIsCoreMaintanenceMode){
			return info;
		}

		int insertOptions = mInsertOptions;
		if(isAtxOn)
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_AMBIGUOUS;


		KPTInpMgrInsertChar insertCharComma = new KPTInpMgrInsertChar((int)c, insertOptions, 0);
		KPTParamInputPreInsert preInsertCharReq = new KPTParamInputPreInsert(1);
		KPTInpMgrInsertChar[] insertChars = {insertCharComma};
		preInsertCharReq.setPreInsertCharRequest(insertChars, 1);

		KPTParamInputPreInsert preInsertCharReply = new KPTParamInputPreInsert(1);
		statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_INPUTMGR_PREINSERTCHAR, 
				preInsertCharReq, preInsertCharReply);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_PREINSERTCHAR count " + preInsertCharReply.getCount());
		if(preInsertCharReply.getCount() > 0) {
			//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_PREINSERTCHAR mode " + preInsertCharReply.getSuggestions()[0].getSuggestionType());
			KPTParamInputInsertion insertSugg = new KPTParamInputInsertion(1);

			int suggSet = preInsertCharReply.getSuggestionSet();
			KPTSuggEntry[] entry = preInsertCharReply.getSuggestions();
			int suggId = entry[0].getSuggestionId();
			int suggType = entry[0].getSuggestionType();
			insertSugg.setInsertSuggestionRequest(suggSet, suggId, 
					(c == ' ' ||isPrevSpace)? false : false , mInsertSuggestionOptions);//temp fix for bug 5642
			/*if((mAutoCorrection == true || suggType == KPTSuggEntry.KPTSuggsTypeSpaceCorrection)  
					&& (c == ' ' || justAddedAutoSpace) && !(c == ' ' && isPrevSpace))
			 */
			//Kashyap Bug Fix : 5750
			// "justAddedAutoSpace" variable check is removed to fix the Auto Back spacing issue.
			if(((mAutoCorrection == true && isAccVisible == true) && (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION
					|| suggType == KPTSuggEntry.KPT_SUGGS_TYPE_ERROR_CORRECTION))
					|| (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_SPACE_CORRECTION
					&& (justAddedAutoSpace) && !(c == ' ' && isPrevSpace))){ //Bug Fix: 5773
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTSUGG, insertSugg);
				//KPTLog.e(KPTDebugString, "addChar KPTCMD_INPUTMGR_INSERTSUGG " + statuscode);
				//KPTLog.e(KPTDebugString, "AutoSuggestedWord AutoSuggestedWord: " + entry[0].getSuggestionString() + "++++");
				if((mAutoCorrection == true && (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION
						|| suggType == KPTSuggEntry.KPT_SUGGS_TYPE_ERROR_CORRECTION)) && !(c == ' ' ||isPrevSpace)){ //temp fix for bug 5642
					// Temperory fix for bug 5642 is removed as Core engine gives the space properly when Auto-correction is ON.	
					insertText(" ", isAtxOn,true,false);
					// Fix for TP 7219
					//  Fix for SQI V1.0 TP Item - 9120
					if(suggType == KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION || suggType == KPTSuggEntry.KPT_SUGGS_TYPE_ERROR_CORRECTION)
					{
						info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
								insertSugg.getCharactersRemovedBeforeCursor(),
								insertSugg.getCharactersRemovedAfterCursor(),
								insertSugg.getModificationString());
					}
					else
					{
						info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
								insertSugg.getCharactersRemovedBeforeCursor(),
								insertSugg.getCharactersRemovedAfterCursor(),
								insertSugg.getModificationString());
					}
				} else {
					info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
							insertSugg.getCharactersRemovedBeforeCursor(),
							insertSugg.getCharactersRemovedAfterCursor(),
							insertSugg.getModificationString());
				}
			} else {
				KPTParamInputInsertion insertChar = new KPTParamInputInsertion(1);
				insertChar.setInsertChar(c, insertOptions, 0);
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTCHAR, insertChar);
				//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_INSERTCHAR " + statuscode);
				info = null;
			}
		} else {
			info = null;
		}

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			return info;
		}
		return info;
	}
	@Override
	public KPTCorrectionInfo addChar(char c, boolean isPrevSpace, boolean justAddedAutoSpace, boolean isAccVisible,
			boolean isAtxOn)
	{
		KPTCorrectionInfo info = null;
		KPTStatusCode statuscode = KPTStatusCode.KPT_SC_ERROR;
		if(mblisted || mIsCoreMaintanenceMode){
			return info;
		}

		int insertOptions = mInsertOptions;
		if(isAtxOn)
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_AMBIGUOUS;


		KPTInpMgrInsertChar insertCharComma = new KPTInpMgrInsertChar((int)c, insertOptions, 0);
		KPTParamInputPreInsert preInsertCharReq = new KPTParamInputPreInsert(1);
		KPTInpMgrInsertChar[] insertChars = {insertCharComma};
		preInsertCharReq.setPreInsertCharRequest(insertChars, 1);

		KPTParamInputPreInsert preInsertCharReply = new KPTParamInputPreInsert(1);
		statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_INPUTMGR_PREINSERTCHAR, 
				preInsertCharReq, preInsertCharReply);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_PREINSERTCHAR count " + preInsertCharReply.getCount());
		if(preInsertCharReply.getCount() > 0) {
			//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_PREINSERTCHAR mode " + preInsertCharReply.getSuggestions()[0].getSuggestionType());
			KPTParamInputInsertion insertSugg = new KPTParamInputInsertion(1);

			int suggSet = preInsertCharReply.getSuggestionSet();
			KPTSuggEntry[] entry = preInsertCharReply.getSuggestions();
			int suggId = entry[0].getSuggestionId();
			int suggType = entry[0].getSuggestionType();
			insertSugg.setInsertSuggestionRequest(suggSet, suggId, 
					(c == ' ' ||isPrevSpace)? false : false , mInsertSuggestionOptions);//temp fix for bug 5642
			/*if((mAutoCorrection == true || suggType == KPTSuggEntry.KPTSuggsTypeSpaceCorrection)  
					&& (c == ' ' || justAddedAutoSpace) && !(c == ' ' && isPrevSpace))
			 */
			//Kashyap Bug Fix : 5750
			// "justAddedAutoSpace" variable check is removed to fix the Auto Back spacing issue.
			if(((mAutoCorrection == true && isAccVisible == true) && (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION
					|| suggType == KPTSuggEntry.KPT_SUGGS_TYPE_ERROR_CORRECTION))
					|| (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_SPACE_CORRECTION
					&& (justAddedAutoSpace) && !(c == ' ' && isPrevSpace))){ //Bug Fix: 5773
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTSUGG, insertSugg);
				//KPTLog.e(KPTDebugString, "addChar KPTCMD_INPUTMGR_INSERTSUGG " + statuscode);
				//KPTLog.e(KPTDebugString, "AutoSuggestedWord AutoSuggestedWord: " + entry[0].getSuggestionString() + "++++");
				if((mAutoCorrection == true && (suggType == KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION
						|| suggType == KPTSuggEntry.KPT_SUGGS_TYPE_ERROR_CORRECTION)) && !(c == ' ' ||isPrevSpace)){ //temp fix for bug 5642
					// Temperory fix for bug 5642 is removed as Core engine gives the space properly when Auto-correction is ON.	
					//insertText(" ", isAtxOn,true);
					// Fix for TP 7219
					//  Fix for SQI V1.0 TP Item - 9120
					if(suggType != KPTSuggEntry.KPT_SUGGS_TYPE_AUTO_ERROR_CORRECTION)
					{
						info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
								insertSugg.getCharactersRemovedBeforeCursor(),
								insertSugg.getCharactersRemovedAfterCursor(),
								insertSugg.getModificationString()+ " ");
					}
					else
					{
						info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
								insertSugg.getCharactersRemovedBeforeCursor(),
								insertSugg.getCharactersRemovedAfterCursor(),
								insertSugg.getModificationString());
					}
				} else {
					info = new KPTCorrectionInfo(entry[0].getSuggestionString(), 
							insertSugg.getCharactersRemovedBeforeCursor(),
							insertSugg.getCharactersRemovedAfterCursor(),
							insertSugg.getModificationString());
				}
			} else {
				KPTParamInputInsertion insertChar = new KPTParamInputInsertion(1);
				insertChar.setInsertChar(c, insertOptions, 0);
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTCHAR, insertChar);
				//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_INSERTCHAR " + statuscode);
				info = null;
			}
		} else {
			info = null;
		}

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			return info;
		}
		return info;
	}


	@Override
	public KPTCorrectionInfo addCharTwo(char c , boolean isAtxOn) {

		KPTCorrectionInfo info = null;
		KPTStatusCode statuscode = KPTStatusCode.KPT_SC_ERROR;
		if(mblisted || mIsCoreMaintanenceMode){
			return info;
		}

		//Dont override mInsertOptions just copy the value into local variable then use it 
		//because mInsertOptions is evaluated based on private on or off
		int insertOptions = mInsertOptions;
		if(isAtxOn)
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_AMBIGUOUS;

		KPTInpMgrInsertChar insertCharComma = new KPTInpMgrInsertChar((int)c, insertOptions, 0);
		KPTParamInputInsertcharWithAC preInsertCharReq = new KPTParamInputInsertcharWithAC(1);
		KPTInpMgrInsertChar[] insertChars = {insertCharComma};
		preInsertCharReq.setInsertCharRequestWithAC(insertChars , 1);

		//	long startTime = System.currentTimeMillis();
		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTCHAR_WITHAC, preInsertCharReq);
		//Log.e("kpt","time taken for " + c + " --> " + (System.currentTimeMillis() - startTime));
		//Log.e("kpt","ADD CHAR TWO : statuscode ---> "+statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS
				&& preInsertCharReq.getCount() > 0){
			info = new KPTCorrectionInfo(null, 
					preInsertCharReq.getCharactersRemovedBeforeCursor(),
					preInsertCharReq.getCharactersRemovedAfterCursor(),
					preInsertCharReq.getModificationString());
		} else {
			info = null;
		}

		return info;
	}


	@Override
	public boolean removeChar(boolean beforeCursor) {
		return removeString(beforeCursor, 1);
	}

	@Override
	public boolean removeString(boolean beforeCursor, int positions) {
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}
		KPTStatusCode statuscode;
		KPTParamInputResetRemoveReplace removeString = new KPTParamInputResetRemoveReplace(1);
		if(beforeCursor) {
			removeString.setRemoveInfo(positions, 0);
		} else {
			removeString.setRemoveInfo(0, positions);
		}
		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_REMOVE, removeString);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;	        
		return false;
	}

	@Override
	public boolean replaceString(int startIndex, int endIndex, String replacedString) {
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}
		KPTStatusCode statuscode;
		KPTParamInputResetRemoveReplace replaceString = new KPTParamInputResetRemoveReplace(1);

		replaceString.setReplaceInfo(startIndex, endIndex, replacedString, replacedString.length());

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_REPLACECONTENTS, replaceString);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;	        
		return false;
	}

	/**
	 * Sets the IME and core in blacklisted mode.
	 * This api used by both platform IME and package installer service.
	 * @param blacklisted mode
	 */
	@Override
	public void setBlackListed(boolean blisted) {
		mblisted  = blisted;
	}

	/**
	 * Sets core in maintenance mode.
	 * This api used by package installer service.
	 * @param maintenance mode
	 */
	@Override
	public void setCoreMaintenanceMode(boolean maintenanceMode) {
		mIsCoreMaintanenceMode  = maintenanceMode;
	}

	/**
	 * Used to check if core calls can be made by editor
	 * @return Whether core calls can be made.
	 */
	/*private boolean isBlackListedMode() {

		// Return true either if editor is in blacklist mode or package installer
		// sets core in maintenance mode.
		return mblisted || mIsCoreMaintanenceMode;
	}*/
	
	
	
	@Override
	public boolean insertText(String text, boolean isAtxOn,boolean shouldLearn) {
				
		return insertText(text, isAtxOn, shouldLearn, false);
	}
	
	

	@Override
	public boolean insertText(String text, boolean isAtxOn,boolean shouldLearn,boolean setAddedSpace) {
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}

		int insertOptions = mInsertOptions;
		if(/*KPTAdaptxtIME.mIsThaiLanguage && */!shouldLearn)
		{
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_TEXT_DONT_LEARN_AND_INLINE;
		} else {
			insertOptions = LEARNCONTEXT;
		}
		/*if(isAtxOn)
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_AMBIGUOUS;
		else
		{
			mInsertOptions = 0;
		}*/
		if (setAddedSpace)
		{
			insertOptions |= KPTParamInputInsertion.KPT_INSERT_TEXT_WITH_ADDED_SPACE;  // 256
		}

		KPTParamInputInsertion insertString = new KPTParamInputInsertion(1);
		insertString.setInsertString(text, text.length(), 0, 0, insertOptions, null);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTSTRING, insertString);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_INSERTSTRING " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		return false;
	}

	@Override
	public boolean resetCoreString() {
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}
		KPTParamInputResetRemoveReplace resetString = new KPTParamInputResetRemoveReplace(1);
		resetString.setResetInfo(0, "");
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_RESET, resetString);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_RESET " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;
		return false;
	}

	@Override
	public boolean setAbsoluteCaretPosition(int cursorPos) {
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}
		KPTParamInputCursor cursorPosition = new KPTParamInputCursor(1);
		cursorPosition.setOffset(cursorPos);
		cursorPosition.setMovementType(KPTParamInputCursor.KPT_SEEK_START);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_MOVECURSOR, cursorPosition);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_MOVECURSOR " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;
		return false;
	}

	@Override
	public int getAbsoluteCaretPosition() {
		if(mblisted || mIsCoreMaintanenceMode || mAdaptxtCore == null){
			return 0;
		}
		KPTParamInputCursor cursorPosition = new KPTParamInputCursor(1);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURSOR, cursorPosition);
		//KPTLog.e(KPTDebugString, "KPTCMD_INPUTMGR_GETCURSOR " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return cursorPosition.getCursorPos();
		return -1;
	}

	@Override
	public void setErrorCorrection(boolean errorCorrect, boolean autoCorrect, int correctionMode, boolean spaceCorrectionMode) {
		if(mblisted || mIsCoreMaintanenceMode){
			return;
		}
		mAutoCorrection  = autoCorrect;
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_ERROR_CORRECTION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setErrorCorrectionOn(errorCorrect);
		if(mAdaptxtCore!=null) 
			mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);

		KPTParamInputConfig inputConfig = new KPTParamInputConfig(1);
		int fieldmask = KPTParamInputConfig.KPT_INP_MGR_CONFIG_MASK_ERROR_CORRECT | 
				KPTParamInputConfig.KPT_INP_MGR_CONFIG_MASK_ERROR_CORRECTION_MODE|
				KPTParamInputConfig.KPT_INP_MGR_CONFIG_MASK_COMPOSITION_MODE |
				KPTParamInputConfig.KPT_INP_MGR_CONFIG_MASK_SPACE_CORRECT;
		inputConfig.setFieldMask(fieldmask);
		inputConfig.setErrorCorrectionSuggestionsOn(autoCorrect);
		inputConfig.setErrorCorrectionMode(correctionMode);
		inputConfig.setSpaceCorrectionSuggestionsOn(spaceCorrectionMode);//Kashyap Bug Fix : 5750 
		inputConfig.setComposeWindowMode(KPTParamInputConfig.KPT_INPUT_DEFAULT_MODE);
		if(mAdaptxtCore!=null) 
			mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_SETCONFIG, inputConfig);



	}


	@Override
	public void setMaxSuggestions(int maxSugg) {
		if(mblisted || mIsCoreMaintanenceMode || mAdaptxtCore == null){
			return;
		}

		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_MAX_SUGGESTIONS;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setMaxNumSuggestions(maxSugg);
		mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);
	}

	/**
	 * Setting Caps States to get Suggestions according to mode.
	 */
	@Override
	public void setCapsStates(KPT_SUGG_STATES state) {
		if(mblisted || mIsCoreMaintanenceMode){
			return;
		}

		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_FORCE_UPPER |
				KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_FORCE_LOWER |
				KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_USER_CAPS |
				KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_SENTENCE_CASE |
				KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_CAP_NEXT|
				KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_USE_STORED_CAPS;

		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setForceUpper(false);
		config.setForceLower(false);
		config.setHonourUserCaps(false);
		config.setUseSentenceCase(false);
		config.setUseStoredCaps(false);
		config.setCapNext(false);
		//config.setUseStoredCaps(null);
		//Kashyap: Please do not change the calls in this switch 
		//case as all the combinations are tried and then finalized
		switch(state) {
		case KPT_SUGG_SENTENCE_CASE:
			config.setUseStoredCaps(true);
			config.setUseSentenceCase(true);
			break;
		case KPT_SUGG_FORCE_UPPER:
			config.setHonourUserCaps(true);
			//config.setCapNext(false);
			config.setForceUpper(true);
			break;
		case KPT_SUGG_FORCE_LOWER:
			config.setCapNext(false);
			config.setUseStoredCaps(true);
			config.setHonourUserCaps(true);
			break;
		case KPT_SUGG_HONOUR_USER_CAPS:
			//config.setUseStoredCaps(true);
			config.setHonourUserCaps(true);
			config.setCapNext(true);
			break;
		case KPT_SUGG_AUTO_CAPS_OFF:
			config.setCapNext(false);
			config.setUseStoredCaps(false);
			config.setHonourUserCaps(true);

			break;
		}
		KPTParamSuggestionConfig retConfig = new KPTParamSuggestionConfig(1);

		mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config, retConfig);
	}

	@Override
	public void resetNextCaps() {
		if(mblisted || mIsCoreMaintanenceMode){
			return;
		}
		KPTStatusCode statuscode = KPTStatusCode.KPT_SC_INVALIDOPERATION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_CAP_NEXT);
		config.setCapNext(false);
		KPTParamSuggestionConfig retConfig = new KPTParamSuggestionConfig(1);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config, retConfig);
	}
	/**
	 * Gets suggestions from core.
	 * 
	 *//*
	public List<KPTSuggestion> getSuggestions() {
		setRevertedWord("");
		mGetSuggs  = new KPTParamSuggestion(1, 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_GETSUGGESTIONS, mGetSuggs);
		//KPTLog.e(KPTDebugString, "KPTCMD_SUGGS_GETSUGGESTIONS" + statuscode);
		List<KPTSuggestion> sugg = new ArrayList<KPTSuggestion>();
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)
		{
			int count = mGetSuggs.getSuggestionEntries().length;
			KPTSuggEntry[] entry = mGetSuggs.getSuggestionEntries();
			KPTSuggestion suggestion;
			for(int i=0; i< count ; i++)
			{
				suggestion = new KPTSuggestion();
				if(entry[i].getsuggestionType() == KPTSuggEntry.KPTSuggsTypeRevertCorrection){
					//KPTLog.e("RevertedWord", "RevertedWord " + entry[i].getsuggestionString());
					setRevertedWord(entry[i].getsuggestionString());
				}
				suggestion.setsuggestionId(entry[i].getsuggestionId());
				suggestion.setsuggestionLength(entry[i].getsuggestionString().length());
				suggestion.setsuggestionString(entry[i].getsuggestionString());
				suggestion.setsuggestionType(entry[i].getsuggestionType());
				sugg.add(suggestion);
			}
		}
		return sugg;		
	}*/

	@Override
	public List<KPTSuggestion> getSuggestions() {
		if (mblisted || mIsCoreMaintanenceMode) {
			return null;
		}
		setRevertedWord("");
		mGetSuggs = new KPTParamSuggestion(1, 0);
	//	long time = System.currentTimeMillis();
		mSuggStatusCode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_SUGGS_GETSUGGESTIONS, mGetSuggs);
	//	Log.e("kpt","Core suggestions tome-->"+(System.currentTimeMillis() - time));
		// KPTLog.e(KPTDebugString, "KPTCMD_SUGGS_GETSUGGESTIONS " +
		// mSuggStatusCode);
		List<KPTSuggestion> sugg = null;

		if (mSuggStatusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			sugg = new ArrayList<KPTSuggestion>();
			int count = mGetSuggs.getSuggestionEntries().length;
			KPTSuggEntry[] entry = mGetSuggs.getSuggestionEntries();
			KPTSuggestion suggestion;
			for (int i = 0; i < count; i++) {
				suggestion = new KPTSuggestion();
				if (entry[i].getSuggestionType() == KPTSuggEntry.KPT_SUGGS_TYPE_REVERT_CORRECTION) {
					// KPTLog.e(KPTDebugString, "RevertedWord " +
					// entry[i].getSuggestionString());
					setRevertedWord(entry[i].getSuggestionString());
				}
				suggestion.setsuggestionId(entry[i].getSuggestionId());
				suggestion.setsuggestionLength(entry[i].getSuggestionString()
						.length());
				suggestion.setsuggestionString(entry[i].getSuggestionString());
				suggestion.setsuggestionType(entry[i].getSuggestionType());

				suggestion.setsuggestionIsUserDicWord(entry[i]
						.getIsPersonalDictWord());

				if (suggestion.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION) {
					sugg.add(0, suggestion);
				} else {
					if (suggestion.getsuggestionType() != KPTSuggestion.KPT_SUGGS_TYPE_ATR) {
						sugg.add(suggestion);
					} else {
						if (mATRStatus) {
							suggestion.setsuggestionString("+"
									+ suggestion.getsuggestionString());
							if (sugg.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
								sugg.add(1, suggestion);
							else
								sugg.add(0, suggestion);
						}
					}
				}
			}
		}
		return sugg;
	}

	public void setATRStatus(boolean status)
	{
		mATRStatus = status;
	}


	public void addATRSuggestions() {
		// to add an ATR entry - START
		// KPTLog.e(KPTDebugString, " Entering addATRSuggestions ");
		String scword = "brb";
		String substword = "be right back";
		KPTParamATRInfo ATR1 = new KPTParamATRInfo(
				KPTCmd.KPTCMD_ATR.getBitNumber(), scword, substword);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_ADD_ATRENTRY, ATR1);
		String scword5 = "gm";
		String substword5 = "Good Morning";
		KPTParamATRInfo ATR5 = new KPTParamATRInfo(
				KPTCmd.KPTCMD_ATR.getBitNumber(), scword5, substword5);
		statuscode = mAdaptxtCore
				.KPTFwkRunCmd(KPTCmd.KPTCMD_ADD_ATRENTRY, ATR5);
		//KPTLog.e(KPTDebugString, "KPTCMD_ATR_ENTRY 5:" + statuscode);

	}

	public void deleteATRSuggestions() {

		KPTParamATRInfo ATR2 = new KPTParamATRInfo(
				KPTCmd.KPTCMD_ATR.getBitNumber());
		ATR2.setShortcut("brb");
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_DELETE_ATRENTRY, ATR2);
		//KPTLog.e(KPTDebugString, "KPTCMD_DELETE_ENTRY 1" + statuscode);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_DELETE_ATRENTRY,
				ATR2);
		//KPTLog.e(KPTDebugString, "KPTCMD_DELETE_ENTRY 2" + statuscode);
	}


	public String trimAccents(String data) {

		KPTParamInputBuffer inputText = new KPTParamInputBuffer(
				KPTCmd.KPTCMD_INPUTMGR.getBitNumber());
		inputText.setInputBufferText(data, data.length());
		//KPTLog.e(KPTDebugString, "getBaseForAccentText actual text 1:" + inputText.getBufferText());
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_INPUTMGR_REPLACEACCENTS, inputText);

		return inputText.getBufferText();
	}


	/**
	 * Gets current suggestion list's status code
	 * @return Sugg status code
	 */
	@Override
	public KPTStatusCode getSuggStatusCode() {
		return mSuggStatusCode;
	}

	@Override
	public KPTCorrectionInfo insertSuggestion(String suggestion, int id, boolean autoSpace){
		if(mblisted || mIsCoreMaintanenceMode){
			return null;
		}
		//getSuggestions();
		KPTParamInputInsertion insertSugg = new KPTParamInputInsertion(1);

		int suggSet = mGetSuggs.getSuggestionSet();
		//KPTSuggEntry[] entry = mGetSuggs.getSuggestionEntries();
		int suggId = id;//entry[id].getsuggestionId();
			
		insertSugg.setInsertSuggestionRequest(suggSet, suggId, autoSpace, mInsertSuggestionOptions);

		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_INSERTSUGG, insertSugg);
		KPTCorrectionInfo info = new KPTCorrectionInfo(insertSugg.getInsertString(), 
				insertSugg.getCharactersRemovedBeforeCursor(),
				insertSugg.getCharactersRemovedAfterCursor(),
				insertSugg.getModificationString()); //  Fix for SQI V1.0 TP Item - 9120
		if(statusCode != KPTStatusCode.KPT_SC_SUCCESS ){
			//KPTLog.e(KPTDebugString, "insertSuggestion statuscode= " + statusCode);
			return null;
		}
		return info;
	}

	@Override
	public String getRevertedWord(){
		return mRevertedWord;
	}

	@Override
	public String getCurrentWordComposition() {
		if(mblisted || mIsCoreMaintanenceMode){
			return null;
		}
		KPTParamInputComposition comp = new KPTParamInputComposition(1);

		comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, 
				KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURRWORD, comp);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return comp.getFixedPrefix()+comp.getFixedSuffix();
		}else{
			return null;
		}
	}

	/* This structure is used to hold current word data and boolean to hold suggestion offset is changed or not.
	 * This suggestion offset boolean is checked in IME before character input into the editor. 
	 * If the suggestion offset is true, finish the composing mode and start a new composing text.
	 * else just continue composing mode.
	 */

	public class CurrentWord{


		public String[] currWord;
		public boolean suggestionOffsetChanged;

	}


	@Override
	public CurrentWord getCurrentWordforDelete() {
		if(mblisted || mIsCoreMaintanenceMode){
			return null;
		}


		KPTParamInputComposition comp = new KPTParamInputComposition(1);

		comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, 
				KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURRWORD, comp);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			CurrentWord currWord = new CurrentWord();
			String[] currentWord = new String[2];
			if(mSuggestionOffset != comp.getSuggestionOffset() ){
				currWord.suggestionOffsetChanged = true;
			}
			mSuggestionOffset = comp.getSuggestionOffset();
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
			//int offset = comp.getCompositionOffset() - comp.getSuggestionOffset();
			/*if(KPTAdaptxtIME.mIsThaiLanguage)
			{
				if (comp.getCompStringLength() > 0) { //7652
					// Fix for TP 8150
					if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset())
					{
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
					}

				} else {

					if( !(comp.getSuggestionOffset() < 0
							|| (comp.getFixedPrefix() + comp.getCompString()).length() < comp.getSuggestionOffset())) {
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
					}
				}
				String temp = (comp.getCompString() + comp.getFixedSuffix());

				if (temp != null && temp.length() >= comp.getSuggestionOffset())
				{
					if(! (comp.getSuggestionOffset() < 0
							|| (comp.getCompString() + comp.getFixedSuffix())
							.length() < comp.getSuggestionOffset())) {
						currentWord[1] = (comp.getCompString() + comp.getFixedSuffix())
								.substring(comp.getSuggestionOffset());
					}
				}
			}
			else
			{*/
				/*if(!atxon){
					if (comp.getCompStringLength() > 0) { //7652
						// Fix for TP 8150
						if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset())
						{
							currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
						}

					} else if(comp.getSuggestionOffset() >= 0)
					{
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
					}
					String temp = (comp.getCompString() + comp.getFixedSuffix());

					if (temp != null && comp.getSuggestionOffset() >= 0 && temp.length() >= comp.getSuggestionOffset())
					{
						currentWord[1] = (comp.getCompString() + comp.getFixedSuffix()).substring(comp.getSuggestionOffset());
					}

				}else{*/

				//Fix for 12248. Complete word is not deleted on tapping delete bin icon in phone keypad layout -- need to confirm with core team

				currentWord[0] = (comp.getFixedPrefix() + comp.getCompString() + comp.getFixedSuffix()).substring(0, comp.getCompositionOffset());

				currentWord[1] = (comp.getFixedPrefix() + comp.getCompString() + comp.getFixedSuffix()).substring(comp.getCompositionOffset());
				//	}
			//}
			mCompString = comp.getCompString();
			mCompStrLength = comp.getCompositionOffset() - comp.getSuggestionOffset();
			mPrefixLength = comp.getFixedPrefixLength();
			mSuffixLength = comp.getFixedSuffixLength();

			/*KPTLog.e(KPTDebugString, "--getcurrentword prefix" + comp.getFixedPrefix() + " len " +  comp.getFixedPrefixLength()  + " comp offset " + comp.getCompositionOffset()+ " suggoff  " + comp.getSuggestionOffset());
			KPTLog.e(KPTDebugString, "--getcurrentword comp string "  + comp.getCompString() + " complen "  + comp.getCompStringLength());
			KPTLog.e(KPTDebugString, "--getcurrentword currenword[0] " + currentWord[0] + " comstrlen "  + mCompStrLength + " compstr "  + mCompString);*/
			currWord.currWord = currentWord;
			return currWord;
		} else {
			return null;
		}
	}

	@Override
	public CurrentWord getCurrentWord() {
		if(mblisted || mIsCoreMaintanenceMode){
			return null;
		}


		KPTParamInputComposition comp = new KPTParamInputComposition(1);

		comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, 
				KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURRWORD, comp);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			CurrentWord currWord = new CurrentWord();
			String[] currentWord = new String[2];
			if(mSuggestionOffset != comp.getSuggestionOffset() ){
				currWord.suggestionOffsetChanged = true;
			}
			mSuggestionOffset = comp.getSuggestionOffset();
			//int offset = comp.getCompositionOffset() - comp.getSuggestionOffset();
			/*if(KPTAdaptxtIME.mIsThaiLanguage)
			{
				if (comp.getCompStringLength() > 0) { //7652
					// Fix for TP 8150
					if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset())
					{
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
					}

				} else {

					if( !(comp.getSuggestionOffset() < 0
							|| (comp.getFixedPrefix() + comp.getCompString()).length() < comp.getSuggestionOffset())) {
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
					}
				}
				String temp = (comp.getCompString() + comp.getFixedSuffix());

				if (temp != null && temp.length() >= comp.getSuggestionOffset())
				{
					if(! (comp.getSuggestionOffset() < 0
							|| (comp.getCompString() + comp.getFixedSuffix())
							.length() < comp.getSuggestionOffset())) {
						currentWord[1] = (comp.getCompString() + comp.getFixedSuffix())
								.substring(comp.getSuggestionOffset());
					}
				}
			}
			else
			{*/
				if (comp.getCompStringLength() > 0) { //7652
					// Fix for TP 8150
					if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset())
					{
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
					}

				} else if(comp.getSuggestionOffset() >= 0)
				{
					currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
				}
				String temp = (comp.getCompString() + comp.getFixedSuffix());

				if (temp != null && comp.getSuggestionOffset() >= 0 && temp.length() >= comp.getSuggestionOffset())
				{
					currentWord[1] = (comp.getCompString() + comp.getFixedSuffix()).substring(comp.getSuggestionOffset());
				}
			//}
			mCompString = comp.getCompString();
			mCompStrLength = comp.getCompositionOffset() - comp.getSuggestionOffset();
			mPrefixLength = comp.getFixedPrefixLength();
			mSuffixLength = comp.getFixedSuffixLength();

			/*KPTLog.e(KPTDebugString, "--getcurrentword prefix" + comp.getFixedPrefix() + " len " +  comp.getFixedPrefixLength()  + " comp offset " + comp.getCompositionOffset()+ " suggoff  " + comp.getSuggestionOffset());
			KPTLog.e(KPTDebugString, "--getcurrentword comp string "  + comp.getCompString() + " complen "  + comp.getCompStringLength());
			KPTLog.e(KPTDebugString, "--getcurrentword currenword[0] " + currentWord[0] + " comstrlen "  + mCompStrLength + " compstr "  + mCompString);*/
			currWord.currWord = currentWord;
			return currWord;
		} else {
			return null;
		}
	}
	public boolean executeThaiCommit() {
		if (!mIsCoreMaintanenceMode) {
			int insertOptions = mInsertOptions;
			KPTParamInputInsertion insertChar = new KPTParamInputInsertion(1);
			insertChar.setInsertChar(Thai_word_separater, insertOptions, 0);
			KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(
					KPTCmd.KPTCMD_INPUTMGR_FREEZEWORD, insertChar);
			return ((statusCode == KPTStatusCode.KPT_SC_SUCCESS) ? true : false);
		} else {
			return false;
		}

	}
	@Override
	public int getPrefixLength() {
		return mPrefixLength;
	}

	@Override
	public int getSuffixLength() {
		return mSuffixLength;
	}

	/***
	 * Gives Information whether user is at start of sentence or not
	 * @return true if start of Sentence
	 */
	@Override
	public boolean isStartOfSentence(){
		if(mblisted || mIsCoreMaintanenceMode){
			return false;
		}
		KPTParamInputComposition comp = new KPTParamInputComposition(1);
		comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);


		KPTParamInputCursor info = new KPTParamInputCursor(KPTCmd.KPTCMD_INPUTMGR.getBitNumber());
		info.setOptions(KPTParamInputCursor.KPT_INSERTION_POINT_GET_END_OF_TEXT | KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE );
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETIPINFO, info , comp);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			int isStart = info.getState() & KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE;
			//KPTLog.e("isStartOfSentence", "" + (isStart == KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE));
			if(isStart == KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE){
				return true;
			}
		}
		return false;
	}

	/**
	 * Brings the core in the sync with platform.
	 * @param textBuffer Buffer to set in the core
	 * @param cursorPosition The new cursor position to set in core
	 * @return If updating core buffer and cursor position is success
	 */
	@Override
	public boolean syncCoreBuffer(String textBuffer, int cursorPosition) {

	  return syncCoreBuffer(textBuffer, cursorPosition,false);
	}
	
	/**
	 * Brings the core in the sync with platform.
	 * @param textBuffer Buffer to set in the core
	 * @param cursorPosition The new cursor position to set in core
	 * @return If updating core buffer and cursor position is success
	 */
	@Override
	public boolean syncCoreBuffer(String textBuffer, int cursorPosition,boolean languageChange) {

		KPTParamInputCoreSync coreSyncText = new KPTParamInputCoreSync(KPTCmd.KPTCMD_INPUTMGR.getBitNumber());
		coreSyncText.setCoreSyncText(textBuffer, cursorPosition,languageChange);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_CORESYNC, coreSyncText);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		} else {
			return false;
		}
	}
	

	/**
	 * Returns the text buffer stored in Core side.
	 * @return The text buffer returned from core. If error returns NULL.
	 */
	@Override
	public String getCoreBuffer() {
		KPTParamInputBuffer coreBufferText = new KPTParamInputBuffer(KPTCmd.KPTCMD_INPUTMGR.getBitNumber());

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETBUFFERTEXT, coreBufferText);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			if(coreBufferText.getBufferText() == null) {
				return new String();
			}else {
				return coreBufferText.getBufferText();
			}
		} else{
			return null;
		}
	}

	/**
	 * Gets available addon atp packages in default package folder. "/profile/package".
	 * @return packages list
	 */
	@Override
	public KPTPackage[] getAvailablePackages() {
		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_GETAVAILABLE, pkgInfo);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return pkgInfo.getAvailPackages();
		}
		else {
			return null;
		}
	}

	/**
	 * Gets installed addon atp packages in core.
	 * @return packages list
	 */
	@Override
	public KPTPackage[] getInstalledPackages() {
		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_GETINSTALLED, pkgInfo);
		//Log.e(KPTDebugString, "getInstalledPackages KPTCMD_PACKAGE_GETINSTALLED " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			//Log.e(KPTDebugString, "getInstalledPackages 666666666 length " + pkgInfo.getInstalledPackages());
			return pkgInfo.getInstalledPackages();
		}
		else {
			return null;
		}
	}

	/**
	 * Installs supplied package from default package folder.
	 * @param packageName Package name to be installed.
	 * @return Installed packages's id
	 */
	@Override
	public int installAddOnPackage(String packageName) {
		if(mApplicationContext != null) {
			// To avoid loading of language list multiple times in IME
			SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true).commit();
		}

		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());
		pkgInfo.setPkgNameToInstall(packageName);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_INSTALL, pkgInfo);
		//Log.e(KPTDebugString, "InstallAddOnPackage success statuscode = " + statuscode+" :"+packageName);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			int installedPkgId = pkgInfo.getPkgIdInstalled();
			return installedPkgId;
		}
		else if(statuscode == KPTStatusCode.KPT_SC_ALREADYEXISTS) {
			//KPTLog.e(KPTDebugString, "InstallAddOnPackage success statuscode= " + statuscode);
			return PACKAGE_ALREADY_EXISTS;
		}
		else {
			//KPTLog.e(KPTDebugString, "InstallAddOnPackage failed statuscode = " + statuscode);
			return PACKAGE_INSTALLATION_FAILED;
		}
	}

	/**
	 * Uninstalls a package from core
	 * @param packageId package Id to uninstall
	 * @return Success or Failure
	 */
	@Override
	public boolean uninstallAddonPackage(int packageId) {
		if(mApplicationContext != null) {
			// To avoid loading of language list multiple times in IME
			SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true).commit();
		}

		KPTParamPackageInfo pkgInfo = new KPTParamPackageInfo(KPTCmd.KPTCMD_PACKAGE.getBitNumber());
		pkgInfo.setPkgIdToUninstall(packageId);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PACKAGE_UNINSTALL, pkgInfo);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		else {
			//KPTLog.e(KPTDebugString, "UninstallAddonPackage failed statuscode = " + statuscode);
			return false;
		}
	}

	/**
	 * Licenses add-on files from the key in license file.
	 * @param licenseFilePath Absolute path of license file.
	 * @return Licensing add-on succeeded or failed. 
	 */
	@Override
	public boolean licenseAddOn(String licenseFilePath) {
		if(licenseFilePath != null){
			KPTParamBase licenseInfo = new KPTParamLicense(KPTCmd.KPTCMD_LICENSE.getBitNumber(),
					UserRef, null, licenseFilePath, 0);
			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LICENSE_WITHFILE, licenseInfo);

			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
				//KPTLog.e(KPTDebugString, "LicenseAddOn success statuscode = "+statuscode);
				return true;
			}
			else {
				//KPTLog.e(KPTDebugString, "LicenseAddOn failed statuscode = " + statuscode);
				return false;
			}
		}
		else{
			return false;
		}
	}

	/**
	 * Gets list of available components and its details.
	 * @return List of components and their info
	 */
	@Override
	public KPTParamComponentInfo[] getAvailableComponents() {
		if(mIsCoreMaintanenceMode){
			return null;
		}

		KPTParamComponentsOperations componentOps = new KPTParamComponentsOperations(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_GETAVAILABLE, componentOps);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_GETAVAILABLE Failed " + statuscode);
			return null;
		}
		else {
			// Get component details is success
			KPTParamComponentInfo[] compInfo =  componentOps.getAvailComponents();
			return compInfo;
		}

	}

	/**
	 * Gets list of loaded components and its details.
	 * @return List of components and their info
	 */
	@Override
	public KPTParamComponentInfo[] getLoadedComponents() {
		if(mIsCoreMaintanenceMode){
			return null;
		}

		KPTParamComponentsOperations componentOps = new KPTParamComponentsOperations(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_GETLOADED, componentOps);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_GETLOADED Failed " + statuscode);
			return null;
		}
		else {
			// Get component details is success
			KPTParamComponentInfo[] compInfo =  componentOps.getLoadedComponents();
			return compInfo;
		}

	}

	/**
	 * Gets the list of installed dicionaties in the core
	 * 
	 * @return Dictionary List
	 */
	@Override
	public KPTParamDictionary[] getInstalledDictionaries() {
		/*if(isBlackListedMode()){
			KPTParamDictionary[] dictparamList = new KPTParamDictionary[0];
			return dictparamList;
		}*/
		if(mAdaptxtCore == null) { // Kashyap - Bug Fix - 6239
			return null;
		}
		KPTParamDictOperations dictOps = new KPTParamDictOperations(KPTCmd.KPTCMD_DICT.getBitNumber());
		//Use null language filter, so we get all language dictionaries
		KPTLanguage langMatch = null;

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_DICT_GETLIST, langMatch, dictOps);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			//KPTLog.e(KPTDebugString, "KPTCMD_DICT_GETLIST Failed " + statuscode);
			return null;
		}
		else {
			// Get installed Dictionaries success
			return dictOps != null ? dictOps.getDictList(): null; // Kashyap - Bug Fix - 6239
		}
	}

	/**
	 * Changes priority of installed dictionaries
	 * @param componentId component id of dictionary to change priority.
	 * @param priority priority to be set. (Priority starts from zero index)
	 * @return If changing priority is success or not.
	 */
	@Override
	public boolean setDictionaryPriority(int componentId, int priority) {
		//Fix for bug number 6183.
		if(mIsCoreMaintanenceMode){
			return false;
		}

		if(mApplicationContext != null) {
			// To avoid loading of language list multiple times in IME
			SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true).commit();
		}

		int feildMaskState = KPTParamDictionary.KPT_DICT_STATE_PRIORITY;
		KPTParamDictionary dictionary = new KPTParamDictionary(KPTCmd.KPTCMD_DICT.getBitNumber());
		dictionary.setDictState(feildMaskState, componentId, false, priority, false,
				KPTLicenseStateT.eKPTLicenseUnlimited.getLicenseStateInt());
		KPTParamDictionary[] dictionaryList = new KPTParamDictionary[1];
		dictionaryList[0] = dictionary;

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_DICT_SETSTATES, dictionaryList, null);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS){
			// Changing priority failed
			//KPTLog.e(KPTDebugString, "**ERROR**" + "KPTCMD_DICT_SETSTATES status: " + statuscode);
			return false;
		}
		else {
			// Changing priority is successful
			return true;
		}
	}

	/**
	 * Loads and enables a Dictionary.
	 * @param componentId Component Id of the dictionary to be loaded.
	 * @return Success or Failure.
	 */
	@Override
	public boolean loadDictionary(int componentId) {
		if(mIsCoreMaintanenceMode){
			return false;
		}

		if(mApplicationContext != null) {
			// To avoid loading of language list multiple times in IME
			SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true).commit();
		}

		KPTParamComponentInfo componentInfo = new KPTParamComponentInfo(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		componentInfo.setComponentId(componentId);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_LOAD, componentInfo);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_LOAD failed status code: " + statuscode);
			return false;
		}
		else {
			// Enabling/ Loading a componnet is success.
			return true;
		}
	}

	/**
	 * Unloads and disables a dictionary.
	 * @param componentId Component Id of the dictionary to be unloaded.
	 * @return Success or Failure.
	 */
	@Override
	public boolean unloadDictionary(int componentId) {
		if(mIsCoreMaintanenceMode){
			return false;
		}

		if(mApplicationContext != null) {
			// To avoid loading of language list multiple times in IME
			SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_LANGUAGE_LIST_DIRTY, true).commit();
		}

		KPTParamComponentInfo componentInfo = new KPTParamComponentInfo(KPTCmd.KPTCMD_COMPONENTS.getBitNumber());
		componentInfo.setComponentId(componentId);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_COMPONENTS_UNLOAD, componentInfo);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			//KPTLog.e(KPTDebugString, "KPTCMD_COMPONENTS_UNLOAD failed status code: " + statuscode);
			return false;
		}
		else {
			// Unloading/Disabling component is success.
			return true;
		}
	}

	/**
	 * Get available keymaps in core
	 * @return Available Keymap id list
	 */
	@Override
	public KPTParamKeymapId[] getAvailableKeymaps() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETAVAILABLE, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETAVAILABLE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return available keymap id list
			return keymapOps.getAvailKeymapIds();
		} else {
			// Error, return empty keymap id list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Get opened keymaps list in core.
	 * @return Opened keymap list
	 */
	@Override
	public KPTParamKeymapId[] getOpenKeymaps() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETOPEN, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETOPEN status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return open keymap
			return keymapOps.getKeymapIdsOpen();
		}
		else {
			// error, return empty list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Get current active keymap in core.
	 * @return Active keymap
	 */
	@Override
	public KPTParamKeymapId[] getActiveKeymap() {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETACTIVE, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETACTIVE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success, return active keymap
			return keymapOps.getIdsActive();
		}
		else {
			// error, return empty list
			return new KPTParamKeymapId[0];
		}
	}

	/**
	 * Get layout associated with a keymap in core.
	 * @param keymapId keymap for which layout had to be fetched
	 * @return Keymap layout associated with keymap
	 */
	@Override
	public KPTParamKeyMapLayout getLayoutForKeymap(KPTParamKeymapId keymapId) {
		KPTParamKeyMapLayout layout = new KPTParamKeyMapLayout(KPTCmd.KPTCMD_KEYMAP.getBitNumber());
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETLAYOUT, keymapId, layout);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETLAYOUT status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS || statusCode == null) {
			//success, return corresponding layout
			return layout;
		} else {
			// error, return null
			return null;
		}
	}

	/**
	 * Opens and loads a keymap layout in core.
	 * @param keymapLayout Keymap layout to be loaded in core.
	 * @return True if success
	 */
	@Override
	public boolean openKeymap(KPTParamKeymapId keymapId) {
		KPTParamKeyMapOperations keymapOps = new KPTParamKeyMapOperations(KPTCmd.KPTCMD_KEYMAP.getBitNumber());

		// First check if any keymaps are open.
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_GETOPEN, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_GETOPEN status code: " + statusCode);
		if(statusCode != KPTStatusCode.KPT_SC_SUCCESS) {
			return false;
		}

		KPTParamKeymapId[] openKeymapIds = keymapOps.getKeymapIdsOpen();

		if(openKeymapIds != null && openKeymapIds.length > 0) {
			// Close all the open keymaps.
			for(KPTParamKeymapId openKeymapId : openKeymapIds) {
				statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_CLOSE, openKeymapId);
				//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_CLOSE status code: " + statusCode);
				if(statusCode != KPTStatusCode.KPT_SC_SUCCESS) {
					return false;
				}
			}
		}

		// Now open the new keymap
		keymapOps.setCreateIfRequiredOpen(true);
		statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_OPEN, keymapId, keymapOps);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_OPEN status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			//  success
			return true;
		} else {
			//error
			return false;
		}
	}

	/**
	 * Sets active keymap in core.
	 * @param keymapId Keymap id to be set active.
	 * @return True if success
	 */
	@Override
	public boolean setActiveKeymap(KPTParamKeymapId keymapId) {
		KPTParamKeymapId[] keymapIdList = {keymapId};
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_SETACTIVE, keymapIdList, null);
		//KPTLog.e(KPTDebugString, "KPTCMD_KEYMAP_SETACTIVE status code: " + statusCode);

		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			// success
			return true;
		}
		else {
			// error
			return false;
		}
	}

	/**
	 * Activates the keymap corresponding to the particular language dictionary component.
	 * @param componentId Component Id of the dictionary whose language keymap has to be acticvated.
	 * @param currentLanguageDict [Optional param else supply null]
	 * 		Language Dictionary whose corresponding keymap has to be activated.
	 * @return If activation is success.
	 */
	@Override
	public boolean activateLanguageKeymap(int componentId, KPTParamDictionary currentLanguageDict) {


		boolean result = false;

		if(currentLanguageDict == null) {
			KPTParamDictionary[] installedDictList = getInstalledDictionaries();

			if(installedDictList != null) {
				for(KPTParamDictionary installedDictionary : installedDictList) {
					if(installedDictionary.getComponentId() == componentId) {
						currentLanguageDict = installedDictionary;
						break;
					}
				}
			}
		}
		if(currentLanguageDict != null) {
			KPTLanguage currentLanguage = currentLanguageDict.getDictLanguage();
			KPTLog.i(KPTDebugString, "current language keymap " + currentLanguage.getLanguage().substring(0, 5));
			KPTParamKeymapId[] keymapList = getAvailableKeymaps();

			SharedPreferences sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
			//String keyboardLayout = sharedPrefEditor.getString(KPTConstants.PREF_CURRENT_KEYBOARD_TYPE, "0");

			String keyboardLayout = sharedPrefEditor.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);
			/*if(mApplicationContext.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
				keyboardLayout = sharedPrefEditor.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
						KPTConstants.KEYBOARD_TYPE_QWERTY);	
			}else{
				keyboardLayout = sharedPrefEditor.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
						KPTConstants.KEYBOARD_TYPE_QWERTY);	
			}*/

			for(KPTParamKeymapId availableKeymap : keymapList) {
				KPTLog.i(KPTDebugString, "Available language keymap " + availableKeymap.getLanguage().getLanguage().substring(0, 5));
				if(availableKeymap.getLanguage().getLanguage().substring(0, 5).equals
						(currentLanguage.getLanguage().substring(0, 5))) {
					if ((keyboardLayout.equalsIgnoreCase(KPTConstants.KEYBOARD_TYPE_QWERTY) && 
							availableKeymap.getGroup() == 3)
							|| (keyboardLayout.equalsIgnoreCase(KPTConstants.KEYBOARD_TYPE_PHONE) 
									&& availableKeymap.getGroup() == 2)) {
						result = openKeymap(availableKeymap);
						if (result) {
							result = setActiveKeymap(availableKeymap);
							/*if (result)
								//KPTLog.e(KPTDebugString,"Set Active keymap success");
							else
								//KPTLog.e(KPTDebugString,"Set Active keymap failed");*/
						} //else
						//KPTLog.e(KPTDebugString, "open keymap failed");

						break;
					}
				}
			}

		}
		return result;
	}
	/*

		boolean result = false;

		if(currentLanguageDict == null) {
			KPTParamDictionary[] installedDictList = getInstalledDictionaries();

			if(installedDictList != null) {
				for(KPTParamDictionary installedDictionary : installedDictList) {
					if(installedDictionary.getComponentId() == componentId) {
						currentLanguageDict = installedDictionary;
						break;
					}
				}
			}
		}

		if(currentLanguageDict != null) {
			KPTLanguage currentLanguage = currentLanguageDict.getDictLanguage();
			//KPTLog.i(KPTDebugString, "current language keymap " + currentLanguage.getLanguage().substring(0, 5));
			KPTParamKeymapId[] keymapList = getAvailableKeymaps();

			for(KPTParamKeymapId availableKeymap : keymapList) {
				//KPTLog.i(KPTDebugString, "Available language keymap " + availableKeymap.getLanguage().getLanguage().substring(0, 5));
				if(availableKeymap.getLanguage().getLanguage().substring(0, 5).equals
						(currentLanguage.getLanguage().substring(0, 5))) {

					KPTParamKeymapId[] activeKeymapArray = getActiveKeymap();

					boolean isKeymapAlreadyActive = false;
					for(KPTParamKeymapId activeKeymap : activeKeymapArray) {
						if(activeKeymap.getLanguage().getLanguage().substring(0,5).equals
								(availableKeymap.getLanguage().getLanguage().substring(0, 5))) {
							// Already the keymap is set as active
							// Ignore and break.
							//KPTLog.i(KPTDebugString, "Already keymap is set as active keymap.");
							result = true;
							isKeymapAlreadyActive = true;
							break;
						}
					}

					if(isKeymapAlreadyActive) {
						// Break from the outer loop
						break;
					}

					// Set this keymap id as the active keymap id
					result = openKeymap(availableKeymap);

					// If keymap is successfully opened, then activate that keymap.
					if(result) {

						result = setActiveKeymap(availableKeymap);
						if(result) {
							//KPTLog.e(KPTDebugString, "Set Active keymap success");
						}else {
							//KPTLog.e(KPTDebugString, "Set Active keymap failed");
						}
					} else {
						//KPTLog.e(KPTDebugString, "Open keymap failed");
					}

					break;
				}
			}
		}
		return result;
	}

	/**
	 * Saves User Context in Core(Saves user added words to core User Dictionary.)
	 */
	@Override
	public void saveUserContext(){
		if(!isCoreUserLoaded() || mIsCoreMaintanenceMode){
			return;
		}
		if(mAdaptxtCore != null) {
			String username = apkDirPath;
			user = new KPTParamUserInfo(1, username, 1234);
			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_SAVE, user);
			//KPTLog.i(KPTDebugString, "KPTCMD_USER_SAVE- " + statuscode);
		}
		/*if(mBackgroundTimer != null) {
			mBackgroundTimer.cancel();
			int numOfCancelledTasks = mBackgroundTimer.purge();
		}

		BackgroundTask saveUserContextTask = new BackgroundTask();
		// create deamon timer thread
		mBackgroundTimer = new Timer(true);
		mBackgroundTimer.schedule(saveUserContextTask, sTimerDelay);*/
	}

	/*@Override
	public void logout() {
		KPTStatusCode statuscode;
		//KPTLog.e(KPTDebugString, "KPTCoreEngineImpl::logout() mReferenceCount " + mReferenceCount);

		if (mAdaptxtCore != null) {
			if (isCoreUserLoaded()) {
				statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_SAVE,
						this.user);
				//KPTLog.e(KPTDebugString, "KPTCMD_USER_SAVE- " + statuscode);
				statuscode = mAdaptxtCore.KPTFwkRunCmd(
						KPTCmd.KPTCMD_USER_UNLOAD, this.user);
				//KPTLog.e(KPTDebugString, "KPTCMD_USER_UNLOAD- " + statuscode);
			}
			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_USER_LOGOUT,
					this.user);
			//KPTLog.e(KPTDebugString, "KPTCMD_USER_LOGOUT- " + statuscode);
		}
	}*/

	@Override
	public void destroyCore() {
		//KPTLog.e(KPTDebugString, "KPTCoreEngineImpl::destroyCore() mReferenceCount " + mReferenceCount);
		/* We are synchronizing the current instance in this function to make that the Core destruction
		 * processing of one thread doesn't interfere with a possible Core initialization processing of
		 * another thread. Currently this issue was identified during system locale change which initiates
		 * the above conflict between KPTLocaleHandlerService and KPTPackageInstallerService.
		 */
		synchronized (this) {
			if (mReferenceCount > 0) {
				mReferenceCount--;
				if (mReferenceCount == 0) {
					//logout();
					if(mAdaptxtCore != null) {
						KPTStatusCode statuscode = mAdaptxtCore.KPTFwkDestroy();
						if (statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
							// Error in core destruction
						}
						//KPTLog.e(KPTDebugString, "KPTCMD_FMK_DESTROY " + statuscode  );
					}
					sCoreEngine = null;
				}
			}
		}
	}

	/**
	 * Forces core destruction in case service fails abruptly
	 */
	public void forceDestroyCore() {
		//KPTLog.e(KPTDebugString, "KPTCoreEngineImpl::forceDestroyCore() mReferenceCount " + mReferenceCount);

		mReferenceCount = 1;
		destroyCore();
	}
	
	public static void atxAssestCopyFromAppInfo(Context context, String filePath, AssetManager assetMgr) {

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean hasBaseBeenUpgraded = sharedPref.getBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false);
		String path = filePath+"/Profile/Profile";
		File atxFile = new File(path);
		Log.e(KPTDebugString, "atxFileExists-->"+atxFile.exists() +"  hasBaseBeenUpgraded-->"+hasBaseBeenUpgraded);
		if(atxFile.exists() && !hasBaseBeenUpgraded) {
			Log.i("KPT", "--------->> atxprofile exists " + path + " " + atxFile.exists());   
			return;
		} 

		// make sure that any possible existing license DAT file is deleted before extracting the profile.zip
		// this is to ensure that the application update properly updates the license in all scenarios
		File licenseFile = new File(path+licenseFilePath);
		if(licenseFile.exists()){
			licenseFile.delete();
		}

		AssetManager am = assetMgr;
		try {
			/*AssetFileDescriptor af = am.openFd("Profile.zip");
			long filesize = af.getLength();*/
			InputStream isd = am.open(KPTConstants.ATX_ASSETS_FOLDER+"Profile.zip");
			OutputStream os = new FileOutputStream(filePath+"/Profile.zip"); 
			byte[] b = new byte[1024]; 
			int length;
			while ((length = isd.read(b))>0) { os.write(b,0,length);}
			isd.close();
			os.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		String zipPath = filePath + "/Profile.zip";
		File atxZipFile = new File(zipPath);
		if(atxZipFile.exists()) {
			unzip(zipPath);
		}

		atxZipFile.delete();

		// Resetting the base upgrade shared pref
		if(hasBaseBeenUpgraded){
			Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false);
			sharedPrefEditor.commit();
		}
	}


	/**
	 * Initiates copy of core files from assets
	 * @param filePath
	 * @param assetMgr
	 */
	public void atxAssestCopy(String filePath, AssetManager assetMgr) {

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
		boolean hasBaseBeenUpgraded = sharedPref.getBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false);
		String path = filePath+"/Profile/Profile";
		File atxFile = new File(path);
		//KPTLog.e(KPTDebugString, "atxFileExists-->"+atxFile.exists() +"  hasBaseBeenUpgraded-->"+hasBaseBeenUpgraded);
		
		
		if(atxFile.exists() && !hasBaseBeenUpgraded) {
			////KPTLog.i(TAG, "--------->> atxprofile exists " + path + " " + atxFile.exists());   
			return;
		} 

		/*if(hasBaseBeenUpgraded){
			//Remove the Old backed-up files
			File ExternalFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Adaptxt/");
			boolean remOldBackUp = removeDirectory(ExternalFile);
			//KPTLog.e("PackageHandlerService", "Removed Old Backed-Up files??  "+remOldBackUp);

			//Back-Up the current profile Context
			File ExternalFiles = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ KPTMyDictionary.EXTERNAL_FILE_PATH_CONTEXT);
			File InternalFile = new File(mApplicationContext.getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_CONTEXT_FILE_PATH);
			Date date = new Date();
			try {
				CopyProfile(InternalFile, ExternalFiles);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//Back-Up the current profile Dictionaries
			File ExternalDictFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath()+KPTMyDictionary.EXTERNAL_FILE_PATH_DICTIONARIES);
			File InternalDictFile = new File(mApplicationContext.getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_DICT_FILE_PATH);
			try {
				CopyProfile(InternalDictFile, ExternalDictFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//KPTLog.e("TIME 2 Backup -->", new Date().getTime() - date.getTime()+"");
			Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false);
			sharedPrefEditor.commit();
		}*/

		// make sure that any possible existing license DAT file is deleted before extracting the profile.zip
		// this is to ensure that the application update properly updates the license in all scenarios
		
		File licenseFile = new File(path+licenseFilePath);
		if(licenseFile.exists()){
			licenseFile.delete();
		}

		AssetManager am = assetMgr;
		try {
			/*AssetFileDescriptor af = am.openFd("Profile.zip");
			long filesize = af.getLength();*/
			InputStream isd = am.open(KPTConstants.ATX_ASSETS_FOLDER+"Profile.zip");
			OutputStream os = new FileOutputStream(filePath+"/Profile.zip"); 
			byte[] b = new byte[1024]; 
			int length;
			while ((length = isd.read(b))>0) { os.write(b,0,length);}
			isd.close();
			os.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		String zipPath = filePath + "/Profile.zip";
		File atxZipFile = new File(zipPath);
		if(atxZipFile.exists()) {
			//KPTLog.e(KPTDebugString, zipPath);
			//KPTLog.e(KPTDebugString, "HERE --------->> adaptxt ZIP exists and will be extracted" + atxZipFile.exists());
			unzip(zipPath);
		}

		atxZipFile.delete();
		if(atxZipFile.exists()) {
			//KPTLog.e(KPTDebugString, "--------->> adaptxt ZIP exists " + atxZipFile.exists());
		}
		else {
			//KPTLog.e(KPTDebugString, "--------->> adaptxt ZIP has been deleted from Internal Storage");
		}

		// Resetting the base upgrade shared pref
		if(hasBaseBeenUpgraded){
			Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false);
			sharedPrefEditor.commit();
		}

		/*	if(hasBaseBeenUpgraded){

			//Restore Back-Up
			//KPTLog.e(KPTDebugString,"Copying the backed up profile files");
			File ExternalFileContext = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ KPTMyDictionary.EXTERNAL_FILE_PATH_CONTEXT);
			File ExternalFileDict = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ KPTMyDictionary.EXTERNAL_FILE_PATH_DICTIONARIES);
			File InternalFileContext = new File(mApplicationContext.getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_CONTEXT_FILE_PATH);
			File InternalFileDict = new File(mApplicationContext.getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_DICT_FILE_PATH);
			try {
				CopyProfile(ExternalFileContext, InternalFileContext);
				CopyProfile(ExternalFileDict, InternalFileDict);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_UPGRADE_DICTIONARY_BACKUP, false);
			sharedPrefEditor.commit();
			//KPTLog.e(KPTDebugString,"Copying the backed up profile files Done");
		}*/
	}

	private void CopyProfile(File src, File dest) throws IOException {
		//Check to ensure that the source is valid...
		if (!src.exists()) {
			throw new IOException("copyFiles: Can not find source: " + src.getAbsolutePath()+".");
		} else if (!src.canRead()) { //check to ensure we have rights to the source...
			throw new IOException("copyFiles: No right to source: " + src.getAbsolutePath()+".");
		}
		//is this a directory copy?
		if (src.isDirectory()) 	{
			if (!dest.exists()) { //does the destination already exist?
				//if not we need to make it exist if possible (note this is mkdirs not mkdir)
				if (!dest.mkdirs()) {
					throw new IOException("copyFiles: Could not create direcotry: " + dest.getAbsolutePath() + ".");
				}
			}
			//get a listing of files...
			String list[] = src.list();
			//copy all the files in the list.
			for (int i = 0; i < list.length; i++)
			{
				File dest1 = new File(dest, list[i]);
				File src1 = new File(src, list[i]);
				CopyProfile(src1, dest1);
			}
		} else { 
			//This was not a directory, so lets just copy the file
			FileInputStream fin = null;
			FileOutputStream fout = null;
			byte[] buffer = new byte[4096]; //Buffer 4K at a time (you can change this).
			int bytesRead;
			try {
				//open the files for input and output
				fin =  new FileInputStream(src);
				fout = new FileOutputStream (dest);
				//while bytesRead indicates a successful read, lets write...
				while ((bytesRead = fin.read(buffer)) >= 0) {
					fout.write(buffer,0,bytesRead);
				}
			} catch (IOException e) { //Error copying file... 
				IOException wrapper = new IOException("copyFiles: Unable to copy file: " + 
						src.getAbsolutePath() + "to" + dest.getAbsolutePath()+".");
				wrapper.initCause(e);
				wrapper.setStackTrace(e.getStackTrace());
				throw wrapper;
			} finally { //Ensure that the files are closed (if they were open).
				if (fin != null) { fin.close(); }
				if (fout != null) { fout.close(); }
			}
		}


	}

	/**
	 * Reads and unzips a zipped file 
	 * @param zipFileName
	 */
	private static void unzip(String zipFileName) {
		//KPTLog.e(KPTDebugString, "-------->  UNZIP  <-----------");
		try {
			File file = new File(zipFileName);
			ZipFile zipFile = new ZipFile(file);

			// create a directory named the same as the zip file in the 
			// same directory as the zip file.
			File zipDir = new File(file.getParentFile(), "Profile");
			if(!zipDir.exists())
				zipDir.mkdir();

			Enumeration<?> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				String nme = entry.getName();
				// File for current file or directory
				File entryDestination = new File(zipDir, nme);

				// This file may be in a subfolder in the Zip bundle
				// This line ensures the parent folders are all
				// created.
				if(!entryDestination.getParentFile().exists())
					entryDestination.getParentFile().mkdirs();

				// Directories are included as seperate entries 
				// in the zip file.
				if(!entry.isDirectory()) {
					generateFile(entryDestination, entry, zipFile);
				} else {
					if(!entryDestination.exists())
						entryDestination.mkdirs();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param entryDestination
	 * @param entry
	 * @param zipFile
	 */
	private static void generateFile(File destination, ZipEntry entry, ZipFile owner) {
		InputStream in = null;
		OutputStream out = null;

		InputStream rawIn;
		try {
			rawIn = owner.getInputStream(entry);
			in = new BufferedInputStream(rawIn, 1024);
			FileOutputStream rawOut = new FileOutputStream(destination);
			out = new BufferedOutputStream(rawOut, 1024);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private boolean removeDirectory(File directory) {
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);
				if (entry.isDirectory()){
					if (!removeDirectory(entry))
						return false;
				}
				else {
					if (!entry.delete())
						return false;
				}
			}
		}

		return directory.delete();
	}


	private void setRevertedWord(String mRevertedWord) {
		this.mRevertedWord = mRevertedWord;
	}

	public String[] getUserDictionary(int startOffset, int endOffset, boolean finalOffset){
		if(mIsCoreMaintanenceMode){
			return null;
		}

		String[] words = null;
		KPTStatusCode statuscode;

		//Log.e(KPTDebugString, "START OFFSET ------------> "+startOffset);

		if(startOffset == 0){
			pDict = new KPTParamPersonalDict(1);
		}

		pDict.setMaxEntries(15); 
		pDict.setOffsetFromStart(startOffset);
		pDict.setMaxEntries(1000);

		pDict.setNumWordsToView(endOffset);

		int filterState = KPTParamPersonalDict.KPT_PD_VIEWFILTER_NONE;
		int viewOption = KPTParamPersonalDict.KPT_PD_VIEW_ALPHA_ASCENDING;
		pDict.setOpenViewRequest(viewOption, filterState, null);

		if(startOffset == 0){
			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_OPENVIEW, pDict);
			Log.e(KPTDebugString, "KPTCMD_PERSONAL_OPENVIEW " + statuscode);
		}

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_VIEWPAGE, pDict);
		Log.e(KPTDebugString, "KPTCMD_PERSONAL_VIEWPAGE " + statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) { 
			words = pDict.getWordsFromPage();

			if(finalOffset)
				mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_CLOSEVIEW, pDict);
		}
		return words;        
	}

	public KPTStatusCode closeDictPages(){
		return mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_CLOSEVIEW, pDict);
	}

	public String[] getUserDictionary() {
		if(mIsCoreMaintanenceMode){
			return null;
		}
		if(mAdaptxtCore == null){
			//In some cases while backing up user words this may come null
			return null;
		}

		String[] words = null;
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_GETENTRYCOUNT, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_GETENTRYCOUNT " + statuscode + " count = " + pDict.getNumWordsFound());
		if(pDict.getNumWordsFound() <= 0)
			return words;

		pDict.setMaxEntries(15); 
		pDict.setOffsetFromStart(0);
		if(pDict.getNumWordsFound()<= MAX_NO_WORDS_MYDICTIONARY) {
			pDict.setNumWordsToView(pDict.getNumWordsFound());
		} else {
			pDict.setNumWordsToView(MAX_NO_WORDS_MYDICTIONARY);
		}

		int filterState = KPTParamPersonalDict.KPT_PD_VIEWFILTER_NONE;
		int viewOption = KPTParamPersonalDict.KPT_PD_VIEW_ALPHA_ASCENDING;
		pDict.setOpenViewRequest(viewOption, filterState, null);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_OPENVIEW, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_OPENVIEW " + statuscode);

		statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_VIEWPAGE, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_VIEWPAGE " + statuscode);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) { 
			words = pDict.getWordsFromPage();
			mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_CLOSEVIEW, pDict);
		}
		return words;        
	}

	public boolean addUserDictionaryWords(String[] words) {
		if(mIsCoreMaintanenceMode){
			return false;
		} 
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);        
		pDict.setWordsTemp(words, words.length);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_ADDWORDS, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_ADDWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) 
			return true;
		return false;                
	}

	public boolean addUserDictionaryWords(String stringBuffer) {
		if(mIsCoreMaintanenceMode){
			return false;
		} 
		KPTParamLearning learn = new KPTParamLearning(KPTCmd.KPTCMD_LEARN.getBitNumber());
		learn.setLearnBuffer(stringBuffer, stringBuffer.length(), KPTParamLearning.KPT_LEARNPHASE_SINGLE, 0, 0, 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARN_BUFFER, learn);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			return false;
		}
		return true;
	}

	public boolean removeUserDictionaryWords(String[] words) {
		if(mIsCoreMaintanenceMode){
			return false;
		}
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		pDict.setRemoveRequest(words, null, 0, KPTParamPersonalDict.KPT_PD_REMOVE_BY_WORD, words.length);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_REMOVEWORDS, pDict);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_REMOVEWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)        
			return true;
		return false;
	}    

	public boolean removeAllUserDictionaryWords() {
		if(mIsCoreMaintanenceMode){
			return false;
		}
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_REMOVEALLWORDS, null);
		//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_REMOVEALLWORDS " + statuscode);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)        
			return true;
		return false;
	}

	/*//TODO: EDIT WORD not implemented
	public boolean editUserDictionaryWord(String oldWord, String newWord) {
		if(mIsCoreMaintanenceMode){
			return false;
		}
		KPTParamPersonalDictWordIdOperations wordIdOps = new KPTParamPersonalDictWordIdOperations(1);
		wordIdOps.setWord(oldWord);        
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_GETIDFORWORD, wordIdOps);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			KPTParamPersonalDictWordIdOperations wordIdOps1 = new KPTParamPersonalDictWordIdOperations(1);
			wordIdOps1.setId(wordIdOps.getId());
			wordIdOps1.setWord(newWord);
			statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_EDITWORD, wordIdOps1);
			//KPTLog.e(KPTDebugString, "KPTCMD_PERSONAL_EDITWORD " + statuscode);
			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS)        
				return true;
		}
		return false;     
	}*/

	public boolean isCoreUserLoaded() {
		return mIsCoreUserLoaded;
	}

	/*
	 * This is method sends the string data to core
	 * param String.
	 * (non-Javadoc)
	 * @see com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine#learnBuffer(java.lang.String)
	 */
	public boolean learnBuffer(String string){
		//Log.e(TAG,"LEARN " +string);
		KPTParamLearning learn = new KPTParamLearning(KPTCmd.KPTCMD_LEARN.getBitNumber());
		learn.setLearnBuffer(string, string.length(), KPTParamLearning.KPT_LEARNPHASE_SINGLE, 0, 0, 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARN_BUFFER, learn);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			return true;
		}else return false;
	}


	/*	  
	 * (non-Javadoc)
	 * @see com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine#learnBufferWithPrunning(java.lang.String, int)

	@Override
	public boolean learnBufferWithPrunning(String stringBuffer,int stringBufferLength) {
		KPTParamLearning learn = new KPTParamLearning(1);
		learn.setLearnBufferWithPrune(stringBuffer, stringBuffer.length(), apkDirPath+"/exclusionlist.txt", 0, 0, 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARN_BUFFER_WITHPRUNING, learn);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			return true;
		}else {
			return false;
		}
	}*/

	/* 
	 * (non-Javadoc)
	 * @see com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine#learnBufferWithPrunning(java.lang.String, int)
	 */
	@Override
	public boolean learnBufferWithPrunning(String stringBuffer,int stringBufferLength,int apptype) {
		KPTParamLearning learn = new KPTParamLearning(1);
		learn.setLearnBufferWithPrune(stringBuffer, stringBuffer.length(), apkDirPath+"/exclusionlist.txt", 0, 0, 0);
		KPTParamAppContextInfo appCtxtInfo = new  KPTParamAppContextInfo(1);
		//	Log.e(TAG, "apptype " + apptype);
		appCtxtInfo.setmAppType(apptype);	
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARN_BUFFER_WITHPRUNING, learn,appCtxtInfo);

		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			return true;
		}else {
			return false;
		}
	}

	@Override
	public int getComposingStringLength() {
		return mCompStrLength;
	}

	@Override
	public String getComposingString(){
		return mCompString;
	}

	public boolean updateCurrentWord() {
		KPTParamInputComposition comp = new KPTParamInputComposition(1);
		comp.setCurrentWordFieldMaskInfo(
				KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL,
				KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_INPUTMGR_GETCURRWORD, comp);

		try {
			int ca[] = comp.getAttributesComposition();
			if (ca != null && ca.length > 0) {
				for (int i = 0; i < ca.length; i++) {
					ca[i] = ca[i]
							& (~KPTParamInputComposition.KPT_CHARACTER_AMBIGUOUS);
				}

				KPTParamInputComposition updateCurWord = new KPTParamInputComposition(
						KPTCmd.KPTCMD_INPUTMGR.getBitNumber());
				updateCurWord.setCurrentWordUpdateInfo(ca, comp.getFixedPrefixLength(), ca.length);
				statuscode = mAdaptxtCore.KPTFwkRunCmd(
						KPTCmd.KPTCMD_INPUTMGR_UPDATECURRWORD, updateCurWord);
				//KPTLog.e(KPTDebugString, "--->updateCurrentWord " + statuscode);

				if (statuscode == KPTStatusCode.KPT_SC_SUCCESS)
					return true;
			}
		} catch (Exception e) {
			//KPTLog.e(KPTDebugString, "Exception in updateCurrentWord" + e);
		}


		return false;
	}

	@Override
	public boolean saveLayoutForKeyMap(KPTParamKeyMapLayout keyMapLayout) {
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_SAVE, keyMapLayout.getId());
		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int getSuggestionOffset(){
		return mSuggestionOffset;
	}

	/**
	 * return the first n chars of first suggestion. where n is the cursor position
	 * in the cuurent word
	 *  
	 */
	public String getPredictiveWord(){

		KPTParamSuggestion getSuggs  = new KPTParamSuggestion(1, 0);
		KPTStatusCode suggStatusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_GETSUGGESTIONS, getSuggs);
		if(suggStatusCode == KPTStatusCode.KPT_SC_SUCCESS)
		{
			int compStrLength;
			KPTParamInputComposition comp = new KPTParamInputComposition(1);

			comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, 
					KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);

			KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETCURRWORD, comp);
			if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {

				compStrLength = comp.getCompositionOffset() - comp.getSuggestionOffset();

				if (getSuggs != null && getSuggs.getCount() > 0) {
					KPTSuggEntry[] entry = getSuggs.getSuggestionEntries();
					if (entry != null && entry.length > 0) {
						String sugg = entry[0].getSuggestionString();
						if(compStrLength >= 0 && compStrLength <= sugg.length()) {
							return sugg.substring(0, compStrLength); 
						}
					}
				}
			}
		}
		return null;
	}
	/**
	 * Sets the core engine either to learn the context or donot learn the context.
	 */
	public void setPrivateMode(boolean privateModeOnOff){
		if(privateModeOnOff){
			mInsertOptions = KPTParamInputInsertion.KPT_INSERT_NO_LEARNING;//DONT_LEARNCONTEXT_ADDCHAR_INSERTTEXT;
			mInsertSuggestionOptions = KPTParamInputInsertion.KPT_SUGG_INSERT_DONT_LEARN;
		}
		else{
			mInsertOptions = LEARNCONTEXT;
			mInsertSuggestionOptions = LEARNCONTEXT;
		}
	}


	@Override
	public boolean setUserDictionaryWordLimit(int maxLimit) {
		// TODO Auto-generated method stub
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		pDict.setMaxEntries(maxLimit);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_SETCONFIG,pDict);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		return false;
	}	



	@Override
	public void addATRShortcutAndExpansion(String shortcut, String expansion) {

		KPTParamATRInfo ATR1 = new KPTParamATRInfo(
				KPTCmd.KPTCMD_ATR.getBitNumber(), shortcut, expansion);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_ADD_ATRENTRY, ATR1);
		// KPTLog.i(KPTDebugString, "KPTCMD_ATR_ENTRY 1:" + statuscode);

	}

	@Override
	public ArrayList<String> getATRShortcuts() {

		ArrayList<String> shortcuts = new ArrayList<String>();
		// Maintained globally as it is used for inserting suggestions.
		KPTParamATRDictInfo mGetATRDictInfo = new KPTParamATRDictInfo(
				1);
		mSuggStatusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_GET_ATRLIST,
				mGetATRDictInfo);

		// KPTLog.e("", "KPTParamATRDictInfo Object is" + mGetATRDictInfo
		// + "----- mSuggStatusCode " + mSuggStatusCode);

		int count = mGetATRDictInfo.getCount();
		// KPTLog.e("", "Shortcuts Count is " + count);

		KPTParamATRInfo[] atrentries = mGetATRDictInfo.getATREntries();

		// KPTLog.e("", "Atr ENtries object is " + atrentries);

		KPTParamATRInfo entry;
		for (int i = 0; i < count; i++) {
			entry = new KPTParamATRInfo(1);
			entry.setShortcut(atrentries[i].getShortcut());
			shortcuts.add(entry.getShortcut());

			// KPTLog.e(KPTDebugString,
			// "getATRDictInfo shortcut " + entry.getShortcut());
		}

		// KPTLog.e("", "Shortcuts length   " + shortcuts.size());

		return shortcuts;
	}

	@Override
	public ArrayList<String> getATRExpansions() {
		// TODO Auto-generated method stub

		ArrayList<String> expansions = new ArrayList<String>();
		// Maintained globally as it is used for inserting suggestions. 
		KPTParamATRDictInfo mGetATRDictInfo = new KPTParamATRDictInfo(1);
		mSuggStatusCode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_GET_ATRLIST, mGetATRDictInfo);

		int count = mGetATRDictInfo.getCount();

		// KPTLog.e("", "Expansion Count is " + count);

		KPTParamATRInfo[] atrentries = mGetATRDictInfo.getATREntries();
		KPTParamATRInfo entry;
		for (int i = 0; i < count; i++) {
			entry = new KPTParamATRInfo(1);
			entry.setSubstitution(atrentries[i].getSubstitution());

			expansions.add(entry.getSubstitution());

			// KPTLog.e(KPTDebugString,
			// "getATRDictInfo Substitution " + entry.getSubstitution());
		}
		// KPTLog.e("", "Expansions length  " + expansions);
		return expansions;
	}

	public String getAtrExpansion(String shortcut) {
		// TODO Auto-generated method stub

		KPTParamATRInfo ATR2 = new KPTParamATRInfo(1);
		ATR2.setShortcut(shortcut);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_GET_ATRENTRY, ATR2);
		String expansion = ATR2.getSubstitution();
		// KPTLog.e(KPTDebugString, "KPTCMD_EXPANSION_ENTRY " + expansion);

		return expansion;
	}

	public boolean removeATRShortcutAndExpansion(String shortcut) {

		if (mIsCoreMaintanenceMode) {
			return false;
		}

		KPTParamATRInfo ATR2 = new KPTParamATRInfo(1);
		ATR2.setShortcut(shortcut);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_DELETE_ATRENTRY, ATR2);
		// KPTLog.e(KPTDebugString, "KPTCMD_DELETE_ENTRY 1" + statuscode);

		if (statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;
		return false;

	}

	@Override
	public void getATRDictInfo() {

	}





	public boolean removeAllATRShortcutAndExpansion(ArrayList<String> shortcuts) {

		if (mIsCoreMaintanenceMode) {
			return false;
		}

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(
				KPTCmd.KPTCMD_DELETE_ALL_ATRENTRIES, null);
		// KPTLog.e(KPTDebugString, "deleteAllATREntries 1:" + statuscode);

		if (statuscode == KPTStatusCode.KPT_SC_SUCCESS)
			return true;

		return false;

	}



	@Override
	public int getUserDictionaryTotalCount() {
		int count = 0;
		KPTParamPersonalDict pDict = new KPTParamPersonalDict(1);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_PERSONAL_GETENTRYCOUNT, pDict);

		count=  pDict.getNumWordsFound();
		return count ;
	}

	@Override
	public boolean setLayoutForKeyMap(KPTParamKeyMapLayout keyMapLayout) {
		KPTStatusCode statusCode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_SETLAYOUT,keyMapLayout);
		if(statusCode == KPTStatusCode.KPT_SC_SUCCESS) {
			return true;
		}
		else {
			return false;
		}
	}

	public KPTInputInfo getInputInfo(){
		if(mblisted || mIsCoreMaintanenceMode){
			return null;
		}

		KPTInputInfo kptInfo = null;

		KPTParamInputComposition comp = new KPTParamInputComposition(1);
		comp.setCurrentWordFieldMaskInfo(KPTParamInputComposition.KPT_CURRENT_WORD_MASK_ALL, KPTParamInputComposition.KPT_COMPOSITION_MASK_ALL);


		KPTParamInputCursor info = new KPTParamInputCursor(KPTCmd.KPTCMD_INPUTMGR.getBitNumber());
		info.setOptions(KPTParamInputCursor.KPT_INSERTION_POINT_GET_END_OF_TEXT | KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE | 
				KPTParamInputCursor.KPT_INSERTION_POINT_GET_CURSOR_POSITION );


		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_GETIPINFO, info, comp);
		if(statuscode == KPTStatusCode.KPT_SC_SUCCESS){
			kptInfo = new KPTInputInfo();


			if(mSuggOffset != comp.getSuggestionOffset() ){
				kptInfo.isSuggestionOffsetChanged = true;
			}
			mSuggOffset = comp.getSuggestionOffset();

			int isStart = info.getState() & KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE;

			//KPTLog.e("isStartOfSentence", "" + (isStart == KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE));
			kptInfo.isStartOfSentence = (isStart ==KPTParamInputCursor.KPT_INSERTION_POINT_GET_START_OF_SENTENCE);

			kptInfo.absoluteCaretPos = info.getCursorPos();

			String[] currentWord = new String[2];
			//int offset = comp.getCompositionOffset() - comp.getSuggestionOffset();
			/*if(KPTAdaptxtIME.mIsThaiLanguage) {
				if (comp.getCompStringLength() > 0) { //7652
					// Fix for TP 8150
					if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset()){
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
					}
				} else {
					if( !(comp.getSuggestionOffset() < 0
							|| (comp.getFixedPrefix() + comp.getCompString()).length() < comp.getSuggestionOffset())) {
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
					}
				}
				String temp = (comp.getCompString() + comp.getFixedSuffix());
				if (temp != null && temp.length() >= comp.getSuggestionOffset()){
					if(! (comp.getSuggestionOffset() < 0
							|| (comp.getCompString() + comp.getFixedSuffix())
							.length() < comp.getSuggestionOffset())) {
						currentWord[1] = (comp.getCompString() + comp.getFixedSuffix())
								.substring(comp.getSuggestionOffset());
					}
				}
			}
			else
			{*/
				if (comp.getCompStringLength() > 0) { //7652
					// Fix for TP 8150
					if( comp.getFixedPrefix().length() + comp.getCompString().length() > comp.getSuggestionOffset()){
						currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(0, comp.getSuggestionOffset());
					}

				}else if(comp.getSuggestionOffset() >= 0)
				{
					currentWord[0] = (comp.getFixedPrefix() + comp.getCompString()).substring(comp.getSuggestionOffset());
				}
				String temp = (comp.getCompString() + comp.getFixedSuffix());

				if (temp != null && comp.getSuggestionOffset() >= 0 && temp.length() >= comp.getSuggestionOffset())
				{
					currentWord[1] = (comp.getCompString() + comp.getFixedSuffix()).substring(comp.getSuggestionOffset());
				}
			//}
			mCompString = comp.getCompString();
			mCompStrLength = comp.getCompositionOffset() - comp.getSuggestionOffset();
			mPrefixLength = comp.getFixedPrefixLength();
			mSuffixLength = comp.getFixedSuffixLength();

			/*KPTLog.e(KPTDebugString, "--getcurrentword prefix" + comp.getFixedPrefix() + " len " +  comp.getFixedPrefixLength()  + " comp offset " + comp.getCompositionOffset()+ " suggoff  " + comp.getSuggestionOffset());
			KPTLog.e(KPTDebugString, "--getcurrentword comp string "  + comp.getCompString() + " complen "  + comp.getCompStringLength());
			KPTLog.e(KPTDebugString, "--getcurrentword currenword[0] " + currentWord[0] + " comstrlen "  + mCompStrLength + " compstr "  + mCompString);*/
			kptInfo.currWordArray = currentWord;
		}
		return kptInfo;
	}


	public static class KPTInputInfo {
		public String[] currWordArray;
		public boolean isStartOfSentence;
		public int absoluteCaretPos;
		public boolean isSuggestionOffsetChanged;
	}


	/**
	 * It is efficient to use static inner class rather using it as independent class 
	 * so making it as static inner class and deleting it from core package.
	 * @author SThatikonda
	 *
	 */
	public static class KPTCorrectionInfo {
		private int charsToRemoveBeforeCursor;
		private int charsToRemoveAfterCursor;
		private int mSuggestionType;
		private String mAutoCorrection;
		private String mModString;

		public KPTCorrectionInfo(String autoCorrection, int charsToRemoveBeforeCursor, int charsToRemoveAfterCursor, String modString){
			this.charsToRemoveBeforeCursor = charsToRemoveBeforeCursor;
			this.charsToRemoveAfterCursor = charsToRemoveAfterCursor;
			mAutoCorrection = autoCorrection;
			mModString = modString;
		}

		public int getCharsToRemoveBeforeCursor() {
			return charsToRemoveBeforeCursor;
		}

		public int getCharsToRemoveAfterCursor() {
			return charsToRemoveAfterCursor;
		}

		public String getAutoCorrection() {
			return mAutoCorrection;
		}

		public String getModString() {
			return mModString;
		}

		public int getSuggestionType() {
			return mSuggestionType;
		}

		public void setSuggestionType(int mSuggestionType) {
			this.mSuggestionType = mSuggestionType;
		}
	}


	@Override
	public List<KPTSuggestion> getGlideSuggestions() {

		if(mGlideSuggestionEntries == null) {
			return null;
		}

		int count = mGlideSuggestionEntries.length;
		List<KPTSuggestion> suggestionList = new ArrayList<KPTSuggestion>();
		KPTSuggestion suggestion = null;

		//Log.e("KPT","Glide Suggestions Count = " + mGlideSuggestionEntries.length);
		for (int i = 0; i < count; i++) {

			suggestion = new KPTSuggestion();
			suggestion.setsuggestionId(mGlideSuggestionEntries[i].getSuggestionId());
			suggestion.setsuggestionLength(mGlideSuggestionEntries[i].getSuggestionString().length());
			suggestion.setsuggestionString(mGlideSuggestionEntries[i].getSuggestionString());
			suggestion.setsuggestionType(mGlideSuggestionEntries[i].getSuggestionType());
			suggestion.setsuggestionIsUserDicWord(mGlideSuggestionEntries[i].getIsPersonalDictWord());

			suggestionList.add(suggestion);
		}
		return suggestionList;
	}

	@Override
	public void setLayoutToCore(Keyboard keyboard) {

		KPTParamLayoutDetails pl = new KPTParamLayoutDetails(1);

		//excluding special keys
		int totalNormalKeys = 0;
		List<Key> listKeys = keyboard.getKeys();
		for (Key k : listKeys) {
			if(k.type == Key.KEY_TYPE_NORMAL || k.type == Key.KEY_TYPE_FUNCTIONAL_SEND_CORE) {
				totalNormalKeys ++;
			}
		}

		//dummy values, not being used currently
		KPTKeymapRow rows[] = new KPTKeymapRow[1];
		rows[0] = new KPTKeymapRow(1,20); 

		KPTKeymapKeyInfo keys[] = new KPTKeymapKeyInfo[totalNormalKeys];
		totalNormalKeys = 0;

		char[] labels;
		for (Key key : listKeys) {
			if(key.type == Key.KEY_TYPE_NORMAL || key.type == Key.KEY_TYPE_FUNCTIONAL_SEND_CORE) {
				if (key.label != null && key.label2 != null) {
					labels = new char[2];
					labels[0] = (char) key.codes[0];
					labels[1] = (char) key.codes[1];
				} else if (key.label != null) {
					labels = new char[1];
					labels[0] = (char) key.codes[0];
				} else{
					continue;
				}

				keys[totalNormalKeys] = new KPTKeymapKeyInfo(key.x, key.y, key.width, key.height, key.gap, labels, key.edgeFlags
						, key.rowNumber, key.keyIndexInRow);
				totalNormalKeys ++;
			}
		}

		pl.SetLayout(keyboard.getMinWidth(), keyboard.getHeight(), keyboard.getThreshhold(), rows, keys);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_KEYMAP_SETLAYOUTDETAILS, pl);
		KPTLog.e(KPTDebugString, "Status code for setLayout " + statuscode);
	}

	@Override
	public void setGlideCoords(float[] xCoordinates, float[] yCoordinates, int option) {

		mGetSuggs = new KPTParamSuggestion(1, 0);

		/*int size = coords.size();
		float[] x_values = new float[size/2];
		float[] y_values = new float[size/2];

		int counter = 0;
		for (int i = 0; i < size; i++) {
			if( i%2 == 0) {
				x_values[counter] = coords.get(i); 
			} else {
				y_values[counter] = coords.get(i);
				counter ++;
			}
		}*/

		//long startTime = System.currentTimeMillis();

		KPTGlideSuggRequest glideSuggReq = new KPTGlideSuggRequest();
		glideSuggReq.setRequestValues(xCoordinates, yCoordinates, option, KPTGlideSuggRequest.KPT_GLIDE_SESSION_STATE_STARTANDEND);
		mGetSuggs.setGlideSuggRequest(glideSuggReq);

		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_GETSUGGESTIONS, mGetSuggs);

		//Log.e("KPT","Time taken = " + (System.currentTimeMillis() - startTime));
		//Log.e("KPT","setGlideCoords status code :: " + statuscode);

		if (statuscode == KPTStatusCode.KPT_SC_SUCCESS) {
			mGlideSuggestionEntries = mGetSuggs.getSuggestionEntries();
		} 
	}
	@Override
	public void setAppContextEnabled(boolean config) {
		// TODO Auto-generated method stub

		if ( mIsCoreMaintanenceMode) {
			return;
		}
		KPTParamSuggestionConfig configNative = new KPTParamSuggestionConfig(1);
		if(config){

			configNative.setmEnableAppContext(1);
		}
		else{
			configNative.setmEnableAppContext(2);
		}

		if (mAdaptxtCore != null){
			KPTStatusCode kk = 	mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SETAPPCTXTSTATE, configNative);
			//	Log.e(TAG, "status " + kk.toString());
		}
	}

	@Override
	public void setAppContextAppName(String appName,int type) {

		if ( mIsCoreMaintanenceMode) {
			return;
		}
		KPTParamAppContextInfo appCtxtInfo = new  KPTParamAppContextInfo(1,appName);



		if (mAdaptxtCore != null){
			KPTStatusCode statuscode =	mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS__SETAPPCONTEXT_NAME, appCtxtInfo);
			Log.e(TAG, "status code for app name " + statuscode);
		}
	}
	@Override
	public boolean learnTrend(String str) {
		boolean retVal = true;
		KPTParamLearning learn = new KPTParamLearning(1);
		learn.setLearnBuffer(str, str.length(), KPTParamLearning.KPT_LEARNPHASE_SINGE_TD, 0, 0, 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARN_BUFFER, learn);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean UnLearnTrend(String str) {
		//Log.e(TAG, "UNLEARNNNNN "+ str);
		boolean retVal = true;
		KPTParamLearning unLearn = new KPTParamLearning(1);
		unLearn.setUnLearnBuffer(str, str.length(), 0);
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_UNLEARN_BUFFER, unLearn);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			retVal = false;
		}
		return retVal;
	}
	@Override
	public boolean loadLocation(String path,String id) {

		boolean retVal = true;
		KPTParamLearnFromFile learn = new KPTParamLearnFromFile(1);
		learn.setOptions(KPTParseFile.KPT_PARSE_STD_ALL|KPTParseFile.KPT_PARSE_INCLUSE_TEXT);
		learn.setFilePath(path);
		learn.setParserId(KPTParseFile.KPTPARSEFILE_BESTMATCH);
		learn.setIsLocationFile(true);
		learn.setLocationId(Integer.parseInt(id));
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_LEARNFILE_RUN, learn);
		if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean unLoadLocation(String path,String id) {
		boolean retVal = true;
		//	Log.e(TAG, "uninstll path "+ path);
		KPTParamLearnFromFile learn = new KPTParamLearnFromFile(1);
		learn.setOptions(KPTParseFile.KPT_PARSE_STD_ALL|KPTParseFile.KPT_PARSE_INCLUSE_TEXT);
		learn.setFilePath(path);
		learn.setParserId(KPTParseFile.KPTPARSEFILE_BESTMATCH);
		//learn.setIsLocationFile(true);
		learn.setLocationId(Integer.parseInt(id));
		KPTStatusCode statuscode = mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_UNLEARN_LOCFILE, learn);
			//Log.e(TAG, "status of uninstall " + statuscode);
		
		/**
		 * Fix for 16078 	Unable to delete the location dictionary after the SP V3.0 Application Update 
		 * This has to be removed in next version. Core returning error if the location id is not found so making
		 * uninstall success in both the cases(FAIL and SUCCESS)
		 * Change made by Bhavani
		 */
		/*if(statuscode != KPTStatusCode.KPT_SC_SUCCESS) {
			retVal = false;
		}*/
		return retVal;
	}

	@Override
	public void setPunctuationPrediction(boolean punctPrediction) {/*
		if (mblisted || mIsCoreMaintanenceMode) {
			return;
		}
		int maskAll = KPTParamSuggestionConfig.KPT_SUGGS_CONFIG_PUNCTUATIONPREDICTION;
		KPTParamSuggestionConfig config = new KPTParamSuggestionConfig(1);
		config.setFieldMasktemp(maskAll);
		config.setPunctuationPrediction(punctPrediction);
		KPTStatusCode status = null;
		if (mAdaptxtCore != null)
		status =	mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_SUGGS_SETCONFIG, config);

		Log.e(TAG,status+ "set Punctioation Prediction" +punctPrediction);

	 */}

	@Override
	public void setAutoSpaceEnable(boolean autoEnable){
		KPTParamInputConfig inputConfig = new KPTParamInputConfig(1);
		inputConfig.setSpaceCorrectionSuggestionsOn(autoEnable);
		int fieldmask = KPTParamInputConfig.KPT_INP_MGR_CONFIG_MASK_SPACE_CORRECT; 

		inputConfig.setFieldMask(fieldmask);
		if(mAdaptxtCore!=null) {
			KPTStatusCode status =     mAdaptxtCore.KPTFwkRunCmd(KPTCmd.KPTCMD_INPUTMGR_SETCONFIG, inputConfig);
			Log.e(TAG, "auto spcae status " + status);
		}


	}
}
