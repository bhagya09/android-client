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
 * @file    KPTPkgHandlerService.java
 *
 * @brief   The Service class used by broadcast receiver to handle package
 *  		installation and uninstallation activities.
 *
 * @details
 *
 *****************************************************************************/


package com.kpt.adaptxt.beta.packageinstaller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;
import com.kpt.adaptxt.beta.settings.AddonDownloadActivity;
import com.kpt.adaptxt.beta.settings.KPTAddonManager;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;
import com.kpt.adaptxt.core.coreapi.KPTPackage;
import com.kpt.adaptxt.core.coreapi.KPTPackageComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;

/**
 * The Service class used by broadcast receiver to handle package
 * installation and uninstallation activities.
 * @author 
 *
 */
public class KPTPkgHandlerService extends Service implements KPTCoreServiceListener {

	private static final String TAG = KPTPkgHandlerService.class.getSimpleName();
	//This is to be changed  in final commit
	private   String ADAPTXT_FOLDER ;// getFilesDir().getAbsolutePath();//  Environment.getExternalStorageDirectory().getAbsolutePath()+"/AdaptxtLearn/";

	private String KPT_ACTION_PACKAGE_UPGRADE = "com.kpt.adaptxt.beta.PACKAGE_UPGRADE";
	private String KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION = "base_upgrade_package_install";
	private String KPT_ACTION_BROADCAST_THEME_ADDON_REMOVED = "addon_theme_removed";




	public static String SILENT_PACKAGE_URL = "package_url";
	public static String SILENT_PACKAGE_NAME = "package_name";
	public static String SILENT_PACKAGE_LANGUAGE = "package_language";


	/**
	 * Target path into which the core files and license has to be copied.
	 */
	public static final String sCorePackagePath = "/Profile/Profile/Packages/";
	/**
	 * Pattern to be used for identifying license text files
	 */
	private String TXT_PATTERN = ".txt";

	private String ADDON_XML_FILE_NAME = "androidaddoncontent.xml";

	/**
	 * Debug string to be used in LogCat
	 */
	private String sAdaptxtDebug = "MS";
	/**
	 * Handle to the Core service, required for installing add on packages.
	 */
	private KPTCoreServiceHandler mCoreServiceHandle;

	/**
	 * Handle to actual core engine reference
	 */
	private KPTCoreEngine mCoreEngine;

	/**
	 * A list of Packages to be installed in core.
	 * Packages are put in queue if already a package is being installed.
	 */
	private List<AddOnInfo> mInstallPackageList;

	/**
	 * An instance of asynchronous background task
	 */
	private BackgroundTask mBackgroundTask;

	private AddonUninstallTask mAddonUninstallTask;
	/**
	 * To maintain references of number of clients bound to this service.
	 * So that it can be stopped afely once all requests are processed
	 */
	private int mStartId;
	/**
	 * Adapter interface used to edit the values of compatibility matrix database
	 */
	private KPTDbAdapter mDbAdapter;
	private SharedPreferences.Editor versionInfoSharedPrefEditor;
	private String actionString;
	private KPTAdaptxtDBHandler kptDB;

	/**
	 * Number of times add-on install has been retired on a failure case.
	 * 
	 */
	private int mNoOfRetries = 0;

	class AddOnInfo {
		private String mFileName;
		private String mVersionNo;
		private String mFilePath;


		public AddOnInfo(String addOnPath, String addOnFileName,
				String addOnVersion) {
			mFileName = addOnFileName;
			mVersionNo = addOnVersion;
			mFilePath =  addOnPath;

		}
		public String getFileName() {
			return mFileName;
		}
		public String getVersionNo() {
			return mVersionNo;
		}
		public String getFilePath() {
			return mFilePath;
		}

	}
	@Override
	public void onCreate() {
		mInstallPackageList = new ArrayList<AddOnInfo>();
			ADAPTXT_FOLDER = getFilesDir().getAbsolutePath();
		//mDownloadTask = new HashMap<String, KPTPkgHandlerService.DownloadAddOnTask>();
		mDbAdapter = new KPTDbAdapter(this);
		kptDB = new KPTAdaptxtDBHandler(this);

		mDbAdapter.open();
		kptDB.open();
		versionInfoSharedPrefEditor = getSharedPreferences(AddonDownloadActivity.INSTALLED_ADDONS_VERSIONINFO, MODE_PRIVATE).edit();
	}

	/**
	 * Returns binder for clients to make synchronous calls on service.
	 * Currently returns NULL, as binding is not supported.
	 * @param intent Intent from client. 
	 */
	@Override
	public IBinder onBind(Intent intent) {
		//Currently only asynchronous operations are supported by this
		//service hence no client can directly bind to this service.
		//Hence returning NULL.
		return null;
	}

	/**
	 * Destroys the binder resources
	 */
	@Override
	public void onDestroy() {
		//Log.e(sAdaptxtDebug, "KPTPkgHandlerService onDestroy()");
		if(mCoreServiceHandle != null) {
			if(mCoreEngine != null ) {
				mCoreEngine.destroyCore();
			}
			mCoreServiceHandle.destroyCoreService();
			mCoreServiceHandle.unregisterCallback(this);
			mCoreEngine = null;
			mCoreServiceHandle = null;
		}
		

		try {
			mDbAdapter.close();
			kptDB.close();
		} catch (SQLiteException e) {

		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.e(sAdaptxtDebug, "SERVICE OnStartCommand");
		if(intent == null) {
			// Intent is null. this could be because service was killed in between.
			// And framework is restarting the service with an invalid intent.
			return START_REDELIVER_INTENT;
		}
		File tempFile = new File(ADAPTXT_FOLDER);
		if(!tempFile.exists()){
			try {
				tempFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		actionString = intent.getAction();
		if(actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_DOWNLOAD_INSTALL)){
			downloadAndInstallAddOn(intent);
		} else if(actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_UNINSTALL)) {
			uninstallAddon(intent);
		}else if(actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_BASE_UPGRADE)) {

			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, true);
		//	sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_IS_UPGRADING, true);
			sharedPrefEditor.putBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, false);
			//sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_LICENSE_EXPIRED, false);
			//sharedPrefEditor.putInt(KPTSetupWizard.PREF_SETUP_STEP, 0);
			//sharedPrefEditor.putString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+"");
			sharedPrefEditor.commit();


			// delete the existing androidaddoncontent.xml so that the user gets
			/*// the latest addon list after a base update
			File myFile = new File(getFilesDir().getAbsolutePath()+"/" + AddonDownloadActivity.S3_COMPATABILITY_XML);
			if(myFile.exists()){
				myFile.delete();
			}*/

			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, true).commit();
			mDbAdapter.close();
			stopSelfResult(startId);

		}/*else if(actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED)){
			installThemeAddon(intent);
			stopSelfResult(startId);
			return START_NOT_STICKY;
		}else if (actionString.equalsIgnoreCase(KPT_ACTION_BROADCAST_THEME_ADDON_REMOVED)){
			removeThemeAddon(intent);
			stopSelfResult(startId);
			return START_NOT_STICKY;
		}else if (actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_LEARN_TREND)){
			Log.e(TAG, "SERVICE CALLED FOR LEARN TREND "+ KPTConstants.KPT_ACTION_PACKAGE_LEARN_TREND);
			String packageURL = intent.getStringExtra(SILENT_PACKAGE_URL);
			String pakageName = intent.getStringExtra(SILENT_PACKAGE_NAME);
			String pakageLanguage =  intent.getStringExtra(SILENT_PACKAGE_LANGUAGE);

			silentAddonProcess(LEARN_TREND, pakageName+pakageLanguage,packageURL,pakageLanguage);
		

		}else if (actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_UNLEARN_TREND)){
			Log.e(TAG, "SERVICE CALLED FOR UNLEARN TREND  "+ KPTConstants.KPT_ACTION_PACKAGE_UNLEARN_TREND);
			String packageURL = intent.getStringExtra(SILENT_PACKAGE_URL);
			String pakageName = intent.getStringExtra(SILENT_PACKAGE_NAME);
			String pakageLanguage =  intent.getStringExtra(SILENT_PACKAGE_LANGUAGE);
			silentAddonProcess(UNLEARN_TREND,pakageName+pakageLanguage,packageURL,pakageLanguage);
	

		}else if (actionString.equalsIgnoreCase(KPTConstants.KPT_ACTION_PACKAGE_UPDATE)){
			Log.e(TAG, "SERVICE CALLED FOR SILENT ADDON UPDATE"+ KPTConstants.KPT_ACTION_PACKAGE_UPDATE);
			String packageURL = intent.getStringExtra(SILENT_PACKAGE_URL);
			String pakageName = intent.getStringExtra(SILENT_PACKAGE_NAME);
			String pakageLanguage =  intent.getStringExtra(SILENT_PACKAGE_LANGUAGE);
			silentAddonProcess(PACKAGE_UPDATE,pakageName+pakageLanguage,packageURL,pakageLanguage);
		

		}*/
		mStartId = startId;
		return START_REDELIVER_INTENT;
	}

	


	private void uninstallAddon(Intent intent) {
		//KPTLog.e(sAdaptxtDebug, "SERVICE uninstallAdOn");
		Bundle extrasBundle = intent.getExtras();
		if(extrasBundle != null) {
			String packageName = extrasBundle.getString(KPTConstants.INTENT_EXTRA_FILE_NAME);
			if(packageName != null) {
				if(mCoreServiceHandle == null) {
					mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
					mCoreServiceHandle.registerCallback(this);
				}
				//  Check if already some package is being installed or uninstalled.
				if(mAddonUninstallTask == null ||
						mAddonUninstallTask.getStatus() == AsyncTask.Status.FINISHED) {
					mAddonUninstallTask = new AddonUninstallTask();
					mAddonUninstallTask.execute(packageName);
				}/*
				else {
					mUninstallPackageList.add(packageName);
				}*/
			}
		}
	}

	private void installThemeAddon(Intent themeAddonIntent){ 

		Bundle extraBundle = themeAddonIntent.getExtras();
		Context addonContext = null;
		String themeName = this.getResources().getString(this.getApplicationInfo().labelRes);
		String packageName = extraBundle.getString(KPTConstants.PACKAGE_NAME);

		try {
			addonContext = createPackageContext(packageName, 0);
			Resources addonResources = getPackageManager().getResourcesForApplication(addonContext.getApplicationInfo());
			themeName = addonContext.getResources().getString(addonResources.getIdentifier("app_name", "string",packageName));


			//Checking whether theme is already present. 
			int tempID = kptDB.getThemeId(themeName);

			//Bug fix #12300. On Restart trying to install external theme again, so stopping it already exists 
			if(tempID < 0){ 
				KPTThemeItem newTheme = new KPTThemeItem(); 
				newTheme.setThemeName(themeName);
				newTheme.setThemeType(KPTConstants.THEME_EXTERNAL);
				newTheme.setPackageName(extraBundle.getString(KPTConstants.PACKAGE_NAME)); 
				newTheme.setThemeEnabled(KPTConstants.THEME_ENABLE);
				newTheme.setBaseId(-1);
				newTheme.setCustomThemeKeyShape(-1);
				newTheme.setCustomTransparency(-1);
				newTheme.setCustomBGPrefs(-1);


				long rowID = kptDB.insertTheme(newTheme);

				//After downloading the theme making it as default theme. 
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				Editor prefEdit = pref.edit(); 
				prefEdit.putString(KPTConstants.PREF_THEME_MODE, ""+rowID);
				prefEdit.commit();
			}


		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void removeThemeAddon(Intent themeUninstallIntent){ 
		Bundle extraBundle = themeUninstallIntent.getExtras();
		String packageName = extraBundle.getString(KPTConstants.PACKAGE_NAME); 
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
		int currentTheme = Integer.parseInt(sharedPref.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
		int themeTobeDeletedId = kptDB.getIDFromPackage(packageName);
		
		boolean currentThemeDeleting = kptDB.isCurrentThemeDeleted(themeTobeDeletedId , currentTheme);
		
		kptDB.deleteTheme(packageName);
		if(themeTobeDeletedId == currentTheme || currentThemeDeleting){
			//After some theme is removed; reset the preferences to default theme. 
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			Editor prefEdit = pref.edit(); 
			prefEdit.putString(KPTConstants.PREF_THEME_MODE, ""+KPTConstants.DEFAULT_THEME);
			prefEdit.commit();
		}
		
		
	}
	
	
	public static boolean searchForPreInstallAddons(Context ctx) {
		boolean retVal = false;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor editor = sharedPref.edit();
		editor.putBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, true);
		editor.commit();
		PackageManager pm = ctx.getPackageManager();
		ArrayList<PackageInfo> pInfoList = (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
		for(int i=0;i<pInfoList.size();i++) {
			if(pInfoList.get(i).packageName.contains(KPTAdaptxtTheme.THEME_PACKAGE_NAME)) {
				Intent installAddOn = new Intent(ctx,KPTPkgHandlerService.class);
				installAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED);
				installAddOn.putExtra(KPTConstants.PACKAGE_NAME, pInfoList.get(i).packageName);
				installAddOn.setFlags(Intent.FLAG_FROM_BACKGROUND);
				ctx.startService(installAddOn);
				retVal = true;
			}
		}

		//TODO: IF retval is false and base has been upgraded flag is true get package names from db and 
		//remove the dangling incompatible addons (i.e.installAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_UNINSTALL);)
		return retVal;
	}


	private void downloadAndInstallAddOn(Intent intent){

		String addOnFileName = intent.getStringExtra(KPTConstants.INTENT_EXTRA_FILE_NAME); // engus
		//Log.e("MS", "KPTPkgSevice Abt 2 Install-->"+ addOnFileName);
		String corePackagePath = getFilesDir() + sCorePackagePath  +addOnFileName.toUpperCase() + KPTConstants.ADDON_EXTENSION + KPTConstants.ADDON_ATP_EXTENSION;
		installAddOn(corePackagePath , addOnFileName, "1.0");
	}

	private void installAddOn(String addOnPath, String addOnFileName, String addOnVersion ) {
		//Log.e(sAdaptxtDebug, "SERVICE installAddOn");
		if(addOnPath != null) {
			if(mCoreServiceHandle == null) {
				mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
				mCoreServiceHandle.registerCallback(this);
			}
			//  Check if already some package is being installed or uninstalled.
			if(mBackgroundTask == null ||
					mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
				mBackgroundTask = new BackgroundTask();
				mBackgroundTask.execute(addOnPath, addOnFileName, addOnVersion);
			}
			else {
				AddOnInfo addOnInfo = new AddOnInfo(addOnPath,addOnFileName,addOnVersion);
				mInstallPackageList.add(addOnInfo);
			}
		}
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		//KPTLog.e(sAdaptxtDebug, "SERVICE serviceConnected");

	}

	class AddonUninstallTask extends  AsyncTask<String, Void, Void> {

		SharedPreferences.Editor sharedPrefEditor;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
			sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, true);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, true);
			sharedPrefEditor.commit();

		}

		@Override
		protected Void doInBackground(String... params) {

			String pkgName = params[0];

			sharedPrefEditor.putString(KPTConstants.PREF_CORE_UNINSTALL_PACKAGE_NAME, pkgName);
			sharedPrefEditor.commit();

			String dictDisplayName = null;
			if(pkgName != null) {
				dictDisplayName = uninstallPackageinCore(pkgName);
			}

			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
			if(mInstallPackageList.isEmpty()) {
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
			}
			sharedPrefEditor.commit();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			stopSelfResult(mStartId);
		}

		/**
		 * Uninstall the dictionary from core
		 * @param dictionaryFileName Dictionary file name in core
		 * @return Uninstalled dictionary display name
		 */
		private String uninstallPackageinCore(String dictionaryFileName) {
			if(dictionaryFileName == null) {
				return null;
			}

			if(mCoreEngine == null) {
				if(mCoreServiceHandle == null)
			    	mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
				mCoreEngine = mCoreServiceHandle.getCoreInterface();
				if(mCoreEngine != null) {
					// Core engine must be initialized only once.
					/*boolean initSuccess = */mCoreEngine.initializeCore(KPTPkgHandlerService.this);
					//KPTLog.e(sAdaptxtDebug, "Core init status " + initSuccess);
				}
				else {
					// Core not yet initialized.
					//KPTLog.e(sAdaptxtDebug, "Core not yet initilaized ");
					return null;
				}
			}

			String dictDisplayname = null;
			int noOfDictsLoaded = 0;

			KPTParamDictionary[] installedDicts = mCoreEngine.getInstalledDictionaries();
			// If the un-installation is happening because of basepackage upgrade then
			// do not respect the "Last Add-on standing" scenario.
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

			// Update the "last Add-on standing" preference on all un-installation.
			// Fix for 7113, this check should be done before the Core un-installation happens.
			/*installedDicts = mCoreEngine.getInstalledDictionaries();
			if(installedDicts != null && installedDicts.length == 1) {
				sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, installedDicts[0].getDictFileName());
			} else {
				sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, null);
			}*/
			sharedPrefEditor.commit();

			// uninstall dictionary only if there is more than one
			//installedDicts.length > 1 - stops to uninstall last add-on
			if(installedDicts != null && (installedDicts.length > 1 ||
					actionString.equalsIgnoreCase(KPT_ACTION_PACKAGE_UPGRADE))) {// || sharedPref.getBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, false))) {

				// Set the core state to blacklisted so that no text insertion/
				// get suggestion operation take place.
				// maintenance mode will be reset by core in updateCoreUserLoadStatus()
				mCoreEngine.setCoreMaintenanceMode(true);

				// TODO: Bug in core.Extra feilds bug. hence looping as follows, else a single call should suffice.
				for(KPTParamDictionary installedDictionary : installedDicts) {

					// Find the corresponding dictionary to be uninstalled
					if(installedDictionary.getDictFileName().equalsIgnoreCase(dictionaryFileName)) {

						// Find the package to which this dictionary corresponds to
						KPTPackage[] installedPkgs =  mCoreEngine.getInstalledPackages();

						if(installedPkgs != null) {
							for(KPTPackage installedPackage : installedPkgs) {
								KPTPackageComponentInfo[] componentList = installedPackage.getComponents();

								for(KPTPackageComponentInfo pkgComponent : componentList) {
									if(pkgComponent.getComponentId() == installedDictionary.getComponentId()) {
										// Found the matching component for installed dictionary.
										// Uninstall this package. Assuming only one component exists in a package. :)

										dictDisplayname = installedDictionary.getDictDisplayName();
										int pkgIdTobeUninstalled = installedPackage.getPackageId();
										boolean pkgUninstallRes = mCoreEngine.uninstallAddonPackage(pkgIdTobeUninstalled);
										if(!pkgUninstallRes) {
											//KPTLog.e(sAdaptxtDebug, "Package Uninstallation Failed");
										} else {
											//KPTLog.e(sAdaptxtDebug, "Package Uninstallation Successfully Completed");
										}
										break;
									}
								}
								if(dictDisplayname != null) {
									break;
								}
							}
						}
					} else if(installedDictionary.isDictLoaded()){
						// incrementing the no. of installed dictionaries which are loaded
						noOfDictsLoaded++;
					}
				}
			}


			mCoreEngine.updateCoreUserLoadStatus(KPTPkgHandlerService.this);

			// If top priority dictionary had been uninstalled then update the keymaps in core.
			mCoreEngine.activateTopPriorityDictionaryKeymap();

			return dictDisplayname;
		}
	}




	/**
	 * Background Task for runnning installation of addon
	 * packages asynchronously.
	 * @author 
	 *
	 */
	class BackgroundTask extends AsyncTask<String, Void, String> {


		@Override
		protected String doInBackground(String... params) {


			Thread thisThread = Thread.currentThread();
			thisThread.setPriority(Thread.MIN_PRIORITY);

			String filePath = params[0];
			String fileName = params[1];
			String fileVer = params[2];
			//Log.e(sAdaptxtDebug, "Installation into Core for-->" + fileName );
			//Log.e(sAdaptxtDebug, "ASYNC function doInBackground filePath-->" + filePath);
			//Log.e(sAdaptxtDebug, "ASYNC function doInBackground fileName-->" + fileName);
			//Log.e(sAdaptxtDebug, "ASYNC function doInBackground fileVer-->" + fileVer);
			String dictName = null;
			dictName = handlePackageInstallation(filePath, fileName, fileVer);

			return dictName;
		}


		@Override
		protected void onPostExecute(String resultDictName) {

			// Completed addon installation. Raise a 'Toast'!

			//Log.e(sAdaptxtDebug, "PKGService Completed addon Installat onPostExecute " + resultDictName );
			if(resultDictName != null) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
				if(!(pref.getBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, false))){
					SharedPreferences.Editor sharedPrefEditor = pref.edit();
					sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, true);
					sharedPrefEditor.commit();
				}
				// Dictionary installation is success
				//raiseToast(getResources().getString(R.string.kpt_package_install_success_toast), resultDictName);					

			} else {
				// Dictionary installation/uninstallation failed.
			}

			// Check if any more dictionaries are waiting in list to be installed and uninstalled.
			// First execute install then uninstall incase user removes as soon as he installs.
			if(!mInstallPackageList.isEmpty()) {
				// Packages are there to be installed

				mBackgroundTask = new BackgroundTask();
				AddOnInfo addonInfo = mInstallPackageList.get(0);
				mInstallPackageList.remove(0);
				mBackgroundTask.execute(addonInfo.getFilePath(), addonInfo.getFileName(), addonInfo.getVersionNo());
			} else {

				// Fix for TP 7148
				//Log.e("MS", "Pref--> NULL onPostExecute");
				Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this).edit();
				sharedPrefEditor.putString(KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, null);
				sharedPrefEditor.putString(KPTConstants.PREF_CORE_UNINSTALL_PACKAGE_NAME, null);
				// make sure the Core is not in maintenance mode
				if(mCoreEngine != null){
					mCoreEngine.setCoreMaintenanceMode(false);
				}
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
				sharedPrefEditor.putBoolean(KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION, false);
				//KPTLog.e("CORE LOG"," BY DEFAULT WE ARE MAKING CORE Setting as "+false);
				sharedPrefEditor.commit();
				boolean isServiceStopped = stopSelfResult(mStartId);
				if(isServiceStopped) {
					//KPTLog.e(sAdaptxtDebug, "KPTPackageHandler Service stopped successfully");
				}
			}
		}

		/**
		 * Starts package installation process in the background.
		 * @param filePath file path of the atp /data/data/..../ENGUS_.atp
		 *  @param fileName file name (engus)
		 *   @param fileVersion version of the addon installed 
		 * @return dictionary name of installed package
		 */
		private String handlePackageInstallation(String filePath, String fileName, String fileVersion) {
			// TP 7956: If Core is in learning mode even before installation starts, then set the "Search for Pre-install" to true
			// ex: Learn manager is doing "Core learning" and a add-on installation is triggered
			SharedPreferences checkPref = PreferenceManager.getDefaultSharedPreferences(KPTPkgHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = checkPref.edit();
			if(checkPref.getBoolean(KPTConstants.PREF_CORE_LEARNING_INPROGRESS, false)){
				// now Add-on installation will be triggered either while opening SIP (or)
				// while opening Add-on manager
				sharedPrefEditor.putBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, false); // true is the default value for failure case
				sharedPrefEditor.commit();
				return null;
			}
			String key = KPTConstants.PREF_ADDON_DOWNLOAD_INPROGRESS + fileName;
			String value = fileName + AddonDownloadActivity.PIPE;
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, true);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, true);
			//Log.e("MS", "Pref--> NULL HandlePkgInstallation ");
			sharedPrefEditor.putString(KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, null);
			//sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, null);
			sharedPrefEditor.putString(key, value + KPTConstants.DOWNLOAD_TASK_SUCCESS );
			sharedPrefEditor.commit();

			/*Log.e(sAdaptxtDebug, "filePath->"+filePath);
			Log.e(sAdaptxtDebug, "fileName->"+fileName);
			Log.e(sAdaptxtDebug, "fileVersion->"+fileVersion);*/
			// Install in core and then delete the atp file

			versionInfoSharedPrefEditor.putString(fileName, fileVersion).commit();
			//Log.e("MS", "Commiting into SharedPref Done for lang-->"+fileName);
			mNoOfRetries = 0;
			String resultDictName = installPackageinCore(filePath, fileName, fileVersion);
			deleteFile(filePath);
			if(resultDictName == null) {
				/**
				 * addon installation has been failed even after three attempts. so removing from 
				 * installing list.
				 * 
				 * better option would be showing a toast that this adon installation was failed
				 */
				//((AdaptxtApplication)getApplicationContext()).getInstallingList().remove(fileName);
				getSharedPreferences(AddonDownloadActivity.INSTALLED_ADDONS_VERSIONINFO, MODE_PRIVATE).edit().remove(fileName).commit();
			}

			//((AdaptxtApplication)getApplicationContext()).getUpgradingList().remove(fileName);
			// If Installation is done then update the required sharedPrefs
			//Log.e(sAdaptxtDebug, "mUpgradingList Size->"+((AdaptxtApplication)getApplicationContext()).getUpgradingList().size());
			//Log.e("MS", "Pref--> fileName-->"+fileName +"  InstallationDone");
			sharedPrefEditor.putString(KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, fileName);
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
			if(mInstallPackageList.isEmpty()) {
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
			}
			sharedPrefEditor.commit();
			return resultDictName;
		}



		/**
		 * Displays a toast message to the user.
		 * @param message Message to be shown
		 */
		@SuppressWarnings("deprecation")
		private void raiseToast(String installStatus, String pkgName) {

			Context context = getApplicationContext();
			CharSequence contentTitle = KPTPkgHandlerService.this.getResources().getText(R.string.kpt_ticker_text);
			CharSequence contentText = installStatus;
			NotificationManager nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			final Notification notifyDetails = new Notification(R.drawable.kpt_notification_icon,
					installStatus,System.currentTimeMillis());
			if(context==null) {
				//KPTLog.e("CONEXT NULL", "");
			}
			Intent notifyIntent = new Intent(KPTPkgHandlerService.this,KPTAddonManager.class);
			PendingIntent pIntent = PendingIntent.getActivity(context, 
					0, notifyIntent, notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
			if(pIntent==null) {
				//KPTLog.e("PINTNT NULL", "");
			}
			notifyDetails.setLatestEventInfo(context, contentTitle, contentText, pIntent);
			nManager.notify(KPTConstants.SIMPLE_NOTFICATION_ID, notifyDetails);

		}


		/**
		 * Delets supplied file
		 * @param absolutePath path of the file to be dleeted
		 * @return success or failure
		 */
		private boolean deleteFile(String absolutePath) {
			if(absolutePath == null) {
				return false;
			}

			File delFile = new File(absolutePath);
			boolean result = delFile.delete();
			if(!result) {
				// Error in disposing zip file
				//KPTLog.e(sAdaptxtDebug,  "Deleting file failed path: " + absolutePath);
			}
			return result;
		}


		/**
		 * Installs supplied Package file in core side.
		 * @param packageFileName ATP file name
		 * @return installed dictionary name if successful else null
		 */
		private String installPackageinCore(String packageFilePath, String packageFileName, String addOnVersion) {
			mNoOfRetries++;
			boolean successStatus = true;
			if(mCoreEngine == null) {
//			#14750. Application crashes on completing the set up wizard with �Sorani Kurdish� language
				if(mCoreServiceHandle == null)
					mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
				mCoreEngine = mCoreServiceHandle.getCoreInterface();
				if(mCoreEngine != null) {
					// Core engine must be initialized only once.
					mCoreEngine.initializeCore(KPTPkgHandlerService.this);
					//Log.e(sAdaptxtDebug, "Core init status " + initSuccess);
				}
				else {
					// Core not yet initialized.
					//Log.e(sAdaptxtDebug, "Core not yet initilaized ");
					return null;
				}
			}

			String installedDictName = null;
			if(mCoreEngine != null) {
				//Log.e(sAdaptxtDebug, "Start core installation installPackageinCore " + packageFilePath);

				// Set the core state to blacklisted so that no text insertion/
				// get suggestion operation take place.
				// maintenance mode will be reset by core in updateCoreUserLoadStatus()
				mCoreEngine.setCoreMaintenanceMode(true);

				// TODO: Bug in Core. getAvailablePackages needs to be removed once core fixes the bug
				/*KPTPackage[] pkgList = */mCoreEngine.getAvailablePackages();
				/*if(pkgList != null) {
					//Log.e(sAdaptxtDebug, "Available Package Count" + String.valueOf(pkgList.length));
					for(KPTPackage addOnPkg : pkgList) {
						//Log.e(sAdaptxtDebug, "Package Name" + addOnPkg.getPackageName() + " Id: " + addOnPkg.getPackageId());
					}
				}*/

				int resultPkgId = mCoreEngine.installAddOnPackage(packageFilePath);
				// Reset the black listing state to false
				//mCoreEngine.setCoreMaintenanceMode(false);

				if(resultPkgId > 0) {
					//Log.e(sAdaptxtDebug, "Start core installation SUCCESS Id: " + resultPkgId);

					/* As the license txt file is being embedded in Add-on ATP file, with the successful
					   installation Core Engine should have copied the license txt file in to the Packages path.
					   So get the path of it and send it to Core engine for licensing
					 */ 
					String licenseFilePath = null;

					File packageDir = new File(getFilesDir() + sCorePackagePath);
					File[] fileList = packageDir.listFiles();
					if (fileList != null)
					{
						for ( int i = 0;i<fileList.length;i++)
						{
							if (fileList[i].getName().endsWith(TXT_PATTERN)){
								// found the license txt file
								licenseFilePath = getFilesDir() + sCorePackagePath + fileList[i].getName();
								break;
							}

						}
					}

					// License the Add-On
					boolean result = mCoreEngine.licenseAddOn(licenseFilePath);
					// delete the license txt file
					deleteFile(licenseFilePath);
					if(!result) {
						successStatus = false;
						// Licensing Failed
						//Log.e(sAdaptxtDebug, "licensing addon FAILED");
					}
					else {
						// Licensing is successful
						//Log.e(sAdaptxtDebug, "Start core licesing SUCCESS");

						// Update core user load status once first package is installed.
						mCoreEngine.updateCoreUserLoadStatus(KPTPkgHandlerService.this);
						//Log.e(sAdaptxtDebug, "updateCoreUserLoadStatus done");

						// Log all details after licensing
						// TODO: to remove this log
						//logAllDetails();

						KPTPackage[] pkgAvailArray = mCoreEngine.getInstalledPackages();
						if(pkgAvailArray != null) {
							for(KPTPackage installedPkg : pkgAvailArray) {
								if(installedPkg.getPackageId() == resultPkgId) {
									KPTPackageComponentInfo[] compInfoListArray = installedPkg.getComponents();
									if(compInfoListArray == null || compInfoListArray.length == 0) {
										successStatus = false;
										// Component Info Array empty
										//Log.e(sAdaptxtDebug, "Installed pkg component list null");
									}
									else {
										// Get display name of the installed dictionary.
										// Getting first dictionary from component, assuming only one dictionary per package.
										KPTPackageComponentInfo dictionaryComponent = compInfoListArray[0];
										if(dictionaryComponent.getComponentType() == KPTPackageComponentInfo.COMPONENT_TYPE_DICTIONARY) {
											if(dictionaryComponent.getExtraDictionary() != null) {
												KPTParamDictionary installedDictionary = dictionaryComponent.getExtraDictionary();
												String dictDisplayName = installedDictionary.getDictDisplayName();
												if(dictDisplayName != null) {
													// FINALLY EVERYTHING IS SUCCESS
													//Log.e(sAdaptxtDebug, "Installing and getting dictionary details is success " + dictDisplayName);
													installedDictName = dictDisplayName;


													// Update the keymap in core if this the first language being installed.
													if(pkgAvailArray.length == 1) {
														boolean setKeymapResult = mCoreEngine.activateLanguageKeymap(installedDictionary.getComponentId(), installedDictionary);
														if(!setKeymapResult) {
															successStatus = false;
															//Log.e(sAdaptxtDebug, "Set keymap failed");
														}

													}
												}
												else {
													successStatus = false;
													//Log.e(sAdaptxtDebug, "Installed pkg component dictionary display name null");
												}

											}
											else {
												successStatus = false;
												// Dictionary details inside the component is null
												//Log.e(sAdaptxtDebug, "Installed pkg component dictionary details are null");
											}
										}
										else {
											successStatus = false;
											// Installed component is not a dictionary component
											//Log.e(sAdaptxtDebug, "Installed pkg component is not dictionary component");
										}
									}
									break;
								}
							}
						}
						else {
							successStatus = false;
							//Log.e(sAdaptxtDebug, "Installed packages NULL");
						}
					}
				}else if(resultPkgId == KPTCoreEngine.PACKAGE_ALREADY_EXISTS) {
					// Update core user load status once first package is installed.
					mCoreEngine.updateCoreUserLoadStatus(KPTPkgHandlerService.this);
				} else {
					//Log.e(sAdaptxtDebug, "****Core ADDON installation FAILED*****");
				}
				Log.e(sAdaptxtDebug, "mNoOfRetries-->"+mNoOfRetries);
				if((resultPkgId == KPTCoreEngine.PACKAGE_INSTALLATION_FAILED || !successStatus)&& 
						mNoOfRetries <KPTConstants. NO_OF_RETRY_INSTALLATION) {
					Log.e(sAdaptxtDebug, "Installation of addon failed so retrying installation");
					try {
						Thread.sleep(KPTConstants.INSTALLATION_RETRY_DELAY);
						installedDictName =	installPackageinCore(packageFilePath, packageFileName, addOnVersion);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

			}
			return installedDictName;
		}

		/**
		 * Utility function to log log current component details
		 */
		/*private void logAllDetails() {

			if(mCoreEngine == null) {
				return;
			}
			//KPTLog.e(sAdaptxtDebug, "******Logging component details");
			KPTParamComponentInfo[] compInfoList = mCoreEngine.getAvailableComponents();
			if(compInfoList != null) {
				//KPTLog.e(sAdaptxtDebug, "******Component Count " + compInfoList.length + "******");

				for(KPTParamComponentInfo componentInfo : compInfoList) {
					//KPTLog.e(sAdaptxtDebug, "*** New Component **** ");
					//KPTLog.e(sAdaptxtDebug,"Component Id " + componentInfo.getComponentId());
					//KPTLog.e(sAdaptxtDebug,"Component Type " + componentInfo.getType());
					//KPTLog.e(sAdaptxtDebug,"Component license state " + componentInfo.getLicenseState());
					//KPTLog.e(sAdaptxtDebug,"Component is loaded " + String.valueOf(componentInfo.getIsLoaded()));
					//KPTLog.e(sAdaptxtDebug,"Component is active " + String.valueOf(componentInfo.getIsActive()));

					if(componentInfo.getExtraDetails() != null) {
						KPTParamDictionary paramDict = componentInfo.getExtraDetails();

						//KPTLog.e(sAdaptxtDebug,"Dictionary Comp-Id " + paramDict.getComponentId());
						//KPTLog.e(sAdaptxtDebug,"Dictionary Display name " + paramDict.getDictDisplayName());
						//KPTLog.e(sAdaptxtDebug,"Dictionary File name " + paramDict.getDictFileName());
						//KPTLog.e(sAdaptxtDebug,"Dictionary Priority " + paramDict.getDictPriority());
						//KPTLog.e(sAdaptxtDebug,"Dictionary Is Active " + String.valueOf(paramDict.isDictActive()));
						//KPTLog.e(sAdaptxtDebug,"Dictionary Is Loaded " + String.valueOf(paramDict.isDictLoaded()));
						//KPTLog.e(sAdaptxtDebug,"Dictionary language subtag " + paramDict.getDictLanguage().getLanguage());
						//KPTLog.e(sAdaptxtDebug,"Dictionary language script " + paramDict.getDictLanguage().getScript());
						//KPTLog.e(sAdaptxtDebug,"Dictionary language id " + paramDict.getDictLanguage().getId());

					}
					else {
						//KPTLog.e(sAdaptxtDebug, "Component Extra details is null");
					}
				}


				// Get Keymap details

				KPTParamKeymapId[] availableKeymaps = mCoreEngine.getAvailableKeymaps();

				for(KPTParamKeymapId keymapId : availableKeymaps) {
					//KPTLog.e(sAdaptxtDebug, "*****GetAvailableKeymap Device " + keymapId.getDevice());
					//KPTLog.e(sAdaptxtDebug, "GetAvailableKeymap group " + keymapId.getGroup());
					//KPTLog.e(sAdaptxtDebug, "GetAvailableKeymap lang id " + keymapId.getLanguage().getId());
					//KPTLog.e(sAdaptxtDebug, "GetAvailableKeymap LanguageId " + keymapId.getLanguageId());
					//KPTLog.e(sAdaptxtDebug, "GetAvailableKeymap manufacturer " + keymapId.getManufacturer());
					//KPTLog.e(sAdaptxtDebug, "GetAvailableKeymap Type " + keymapId.getType());

					// Open keymap

					//mCoreEngine.openKeymap(keymapLayout)

					// Get layouts for available keymaps
					KPTParamKeyMapLayout keymapLayout = mCoreEngine.getLayoutForKeymap(keymapId);

					if(keymapLayout != null && keymapLayout.getId() != null) {
						//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap lang Id " + keymapLayout.getId().getLanguageId());
						//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap device name " + keymapLayout.getId().getDevice());
						//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap manufacturer name " + keymapLayout.getId().getManufacturer());
						//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap type " + keymapLayout.getId().getType());
						//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap count " + keymapLayout.getCount());
						if(keymapLayout.getKeys() != null) {
							//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap key count " + keymapLayout.getKeys().length);
						}
						else {
							//KPTLog.e(sAdaptxtDebug, "GetLayoutForKeymap key count null");
						}
					}
				}

				// Get open keymaps

				KPTParamKeymapId[] openKeymaps = mCoreEngine.getOpenKeymaps();

				for(KPTParamKeymapId keymapId : openKeymaps) {
					//KPTLog.e(sAdaptxtDebug, "*****GetOpenKeymap Device " + keymapId.getDevice());
					//KPTLog.e(sAdaptxtDebug, "GetOpenKeymap group " + keymapId.getGroup());
					//KPTLog.e(sAdaptxtDebug, "GetOpenKeymap lang id " + keymapId.getLanguage().getId());
					//KPTLog.e(sAdaptxtDebug, "GetOpenKeymap LanguageId " + keymapId.getLanguageId());
					//KPTLog.e(sAdaptxtDebug, "GetOpenKeymap manufacturer " + keymapId.getManufacturer());
					//KPTLog.e(sAdaptxtDebug, "GetOpenKeymap Type " + keymapId.getType());
				}

				// Get active keymaps

				KPTParamKeymapId[] activeKeymaps = mCoreEngine.getActiveKeymap();

				for(KPTParamKeymapId keymapId : activeKeymaps) {
					//KPTLog.e(sAdaptxtDebug, "*****GetActiveKeymap Device " + keymapId.getDevice());
					//KPTLog.e(sAdaptxtDebug, "GetActiveKeymap group " + keymapId.getGroup());
					//KPTLog.e(sAdaptxtDebug, "GetActiveKeymap lang id " + keymapId.getLanguage().getId());
					//KPTLog.e(sAdaptxtDebug, "GetActiveKeymap LanguageId " + keymapId.getLanguageId());
					//KPTLog.e(sAdaptxtDebug, "GetActiveKeymap manufacturer " + keymapId.getManufacturer());
					//KPTLog.e(sAdaptxtDebug, "GetActiveKeymap Type " + keymapId.getType());
				}

			}

		}*/
	}



	



}
