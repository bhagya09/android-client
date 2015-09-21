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
 * @file    KPTPackageHandlerService.java
 *
 * @brief   The Service class used by broadcast receiver to handle package
 *  		installation and uninstallation activities.
 *
 * @details
 *
 *****************************************************************************/


package com.kpt.adaptxt.beta.packageinstaller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;
import com.kpt.adaptxt.beta.settings.KPTAddonManager;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.core.coreapi.KPTPackage;
import com.kpt.adaptxt.core.coreapi.KPTPackageComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTParamKeyMapLayout;
import com.kpt.adaptxt.core.coreapi.KPTParamKeymapId;

/**
 * The Service class used by broadcast receiver to handle package
 * installation and uninstallation activities.
 * @author 
 *
 */
public class KPTPackageHandlerService extends Service implements KPTCoreServiceListener {
	public int SIMPLE_NOTFICATION_ID;
	/**
	 * Static string used for filling wxtra details in the intent being passed to service.
	 */
	public static final String PACKAGE_NAME = "com.kpt.adaptxt.beta.PACKAGE_NAME";

	/**
	 * Intent action string used for installing package.
	 */
	public static final String KPT_ACTION_PACKAGE_INSTALL = "com.kpt.adaptxt.beta.PACKAGE_INSTALL";

	/**
	 * Intent action string used for uninstalling package.
	 */
	public static final String KPT_ACTION_PACKAGE_UNINSTALL = "com.kpt.adaptxt.beta.PACKAGE_UNINSTALL";

	public static final String KPT_ACTION_PACKAGE_UPGRADE = "com.kpt.adaptxt.beta.PACKAGE_UPGRADE";

	public static final String KPT_ACTION_PACKAGE_BASE_UPGRADE = "base_upgraded";
	
	public static final String KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION = "base_upgrade_package_install";
	
	/**
	 * Intent action for broadscasts before/after installation/uninstallation
	 */
	public static final String KPT_ACTION_BROADCAST_ADDON_INSTALLATION_STARTED = "addon_installation_started";
	
	public static final String KPT_ACTION_BROADCAST_ADDON_INSTALLATION_FINISHED = "addon_installation_finished";
	
	public static final String KPT_ACTION_BROADCAST_ADDON_UNINSTALLATION_STARTED = "addon_uninstallation_started";
	
	public static final String KPT_ACTION_BROADCAST_ADDON_UNINSTALLATION_FINISHED = "addon_uninstallation_finished";
	
	public static final String KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED = "addon_theme_installed";
	public static final String KPT_ACTION_BROADCAST_THEME_ADDON_REMOVED = "addon_theme_removed";
	
	//TODO Remove these actions after generalization is complete
//	public static final String KPT_ACTION_BROADCAST_BLUE_THEME_ADDON_INSTALLED = "addon_blue_theme_installed";
//	public static final String KPT_ACTION_BROADCAST_BLUE_THEME_ADDON_REMOVED= "addon_blue_theme_removed";

	/**
	 * Folder name under which the core packages are stored in add on's assets.
	 */
	private static final String sCoreAddOnPath = "core_packages";

	/**
	 * Folder name under which license files are stored in the assets of AddOn.
	 */
	//private static final String sLicenseAddOnPath = "license";

	/**
	 * Target path into which the core files and license has to be copied.
	 */
	private static final String sCorePackagePath = "/Profile/Profile/Packages/";

	/**
	 * Pattern to be used for identifying license text files
	 */
	private static final String TXT_PATTERN = ".txt";
	
	private static final String ADDON_XML_FILE_NAME = "androidaddoncontent.xml";

	/**
	 * Debug string to be used in LogCat
	 */
	private static final String sAdaptxtDebug = "KPTPackageHandlerService";

	/**
	 * Number of bytes to read during file copy from assets.
	 * Large 10 Kb is used because the core files are more than 500 kb.
	 */
	private static final int mNoofBytesToRead = 10240;


	/**
	 * Handle to the Core service, required for installing add on packages.
	 */
	private KPTCoreServiceHandler mCoreServiceHandle;

	/**
	 * Handle to actual core engine reference
	 */
	KPTCoreEngine mCoreEngine;

	/**
	 * A list of Packages to be installed in core.
	 * Packages are put in queue if already a package is being installed.
	 */
	private List<String> mInstallPackageList;

	/**
	 * A list of Packages to be uninstalled in core.
	 * Packages are put in queue if already a package is being installed/uninstalled.
	 */
	private List<String> mUninstallPackageList;

	/**
	 * A list of Packages to be upgraded in core.
	 * Packages are put in queue if already a package is being upgraded.
	 */
	private List<String> mUpgradePackageList;


	/**
	 * An instance of asynchronous background task
	 */
	BackgroundTask mBackgroundTask;


	/**
	 * To maintain references of number of clients bound to this service.
	 * So that it can be stopped afely once all requests are processed
	 */
	private int mStartId;


	/**
	 * Used in shared prefs to trigger the search for pre-installed Adaptxt Add-Ons
	 */
	public static final String SEARCH_PREINSTALL_ADDONS = "search_for_pre_installed_addons";

	/**
	 * Adapter interface used to edit the values of compatibility matrix database
	 */
	private KPTDbAdapter mDbAdapter;
	
	private boolean mBaseUpdateIncompatibleOccured = false;

	/**
	 * Structure to hold the information of current core files being installed.
	 * 
	 * @author 
	 *
	 */
	private class KPTCoreFileInfo {
		/**
		 * Core atp File path
		 */
		String mCoreFilePath;

		/**
		 * Core ATP file name
		 */
		String mCoreFileName;
	}

	public static final String sPackageRegex = "\\(";
	public static final String COMPATIBLE = "Compatible";
	public static final String VERSION_NAME = "version_name";
	public String actionString;
	
	public static final String basePackageName = "com.kpt.adaptxt.beta";
	private KPTAdaptxtDBHandler kptDB;
	private String TAG = KPTPackageHandlerService.class.getSimpleName();



	/**
	 * Creates and initializes the service
	 * 
	 */
	@Override
	public void onCreate() {
		mInstallPackageList = new ArrayList<String>();
		mUninstallPackageList = new ArrayList<String>();
		mUpgradePackageList = new ArrayList<String>();
		mDbAdapter = new KPTDbAdapter(this);
		mDbAdapter.open();
		
		kptDB = new KPTAdaptxtDBHandler(this);
		kptDB.open();
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
		if(intent == null) {
			// Intent is null. this could be because service was killed in between.
			// And framework is restarting the service with an invalid intent.
			return START_REDELIVER_INTENT;
		}

		actionString = intent.getAction();
		if(actionString.equalsIgnoreCase(KPT_ACTION_PACKAGE_INSTALL)) {
			installAddOn(intent);
		} else if(actionString.equalsIgnoreCase(KPT_ACTION_PACKAGE_UNINSTALL)) {
			uninstallAddOn(intent);
		} else if(actionString.equalsIgnoreCase(KPT_ACTION_PACKAGE_UPGRADE)) {
			upgradeAddon(intent);
		} else if(actionString.equalsIgnoreCase(KPT_ACTION_PACKAGE_BASE_UPGRADE)) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
		//	Log.e(TAG , "BROADCAT RECIVED ##############################################");
			sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, true);
			//sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_HAS_BEEN_UPGRADED, true);
			//sharedPrefEditor.putBoolean(KPTConstants.PREF_BASE_IS_UPGRADING, true);
			sharedPrefEditor.putBoolean(SEARCH_PREINSTALL_ADDONS, false);
			//sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_LICENSE_EXPIRED, false);
			sharedPrefEditor.putBoolean(KPTConstants.PREFERENCE_EULA_ACCEPTED, false);	
			sharedPrefEditor.putBoolean(KPTConstants.PREFERENCE_SETUP_COMPLETED, false);
			//sharedPrefEditor.putInt(KPTSetupWizard.PREF_SETUP_STEP, 0);
			sharedPrefEditor.putString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+"");
			sharedPrefEditor.putBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, false);
			sharedPrefEditor.commit();
			
			// delete the existing androidaddoncontent.xml so that the user gets
			// the latest addon list after a base update
			File myFile = new File(getFilesDir().getAbsolutePath()+"/" + ADDON_XML_FILE_NAME);
			if(myFile.exists()){
				myFile.delete();
			}
		}else if(actionString.equalsIgnoreCase(KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED)){
			installThemeAddon(intent);
			return START_NOT_STICKY;
		}else if (actionString.equalsIgnoreCase(KPT_ACTION_BROADCAST_THEME_ADDON_REMOVED)){
			removeThemeAddon(intent); 
			return START_NOT_STICKY;
		}

		mStartId = startId;
		return START_REDELIVER_INTENT;
	}

	private void upgradeAddon(Intent addOnIntent) {

		Bundle extrasBundle = addOnIntent.getExtras();
		if(extrasBundle != null) {
			String packageName = extrasBundle.getString(PACKAGE_NAME);
			if(packageName != null) {
				if(mCoreServiceHandle == null) {
					mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
					mCoreServiceHandle.registerCallback(this);
				}

				//  Check if already some package is being upgraded.
				if(mBackgroundTask == null ||
						mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
					mBackgroundTask = new BackgroundTask();
					mBackgroundTask.execute(KPT_ACTION_PACKAGE_UPGRADE, packageName);
				} else {
					mUpgradePackageList.add(packageName);
				}
			}
		}
	}

	private void installThemeAddon(Intent themeAddonIntent){ 
		
		Bundle extraBundle = themeAddonIntent.getExtras();
		KPTThemeItem newTheme = new KPTThemeItem(); 
		Context addonContext = null;
		String themeName = this.getResources().getString(this.getApplicationInfo().labelRes);
		String packageName = extraBundle.getString(PACKAGE_NAME);
		
		try {
			addonContext = createPackageContext(packageName, 0);
			Resources addonResources = getPackageManager().getResourcesForApplication(addonContext.getApplicationInfo());
			themeName = addonContext.getResources().getString(addonResources.getIdentifier("app_name", "string",packageName));
			
			newTheme.setThemeName(themeName);
			newTheme.setThemeType(KPTConstants.THEME_EXTERNAL);
			newTheme.setPackageName(extraBundle.getString(PACKAGE_NAME)); 
			newTheme.setThemeEnabled(KPTConstants.THEME_ENABLE);
			newTheme.setBaseId(-1);
			newTheme.setCustomThemeKeyShape(-1);
			newTheme.setCustomTransparency(-1);
			newTheme.setCustomBGPrefs(-1);
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		long rowID = kptDB.insertTheme(newTheme); 
	}
	
	private void removeThemeAddon(Intent themeUninstallIntent){ 
		Bundle extraBundle = themeUninstallIntent.getExtras();
		String packageName = extraBundle.getString(KPTConstants.PACKAGE_NAME); 
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
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
	
	
	
	
	/**
	 * Starts Uninstallation of AddOn from core
	 * @param addOnIntent Intent received from clients which contains package name to be uninstalled.
	 */
	private void uninstallAddOn(Intent addOnIntent) {
		Bundle extrasBundle = addOnIntent.getExtras();
		if(extrasBundle != null) {
			String packageName = extrasBundle.getString(PACKAGE_NAME);
			if(packageName != null) {
				if(mCoreServiceHandle == null) {
					mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
					mCoreServiceHandle.registerCallback(this);
				}

				//  Check if already some package is being installed or uninstalled.
				if(mBackgroundTask == null ||
						mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
					mBackgroundTask = new BackgroundTask();
					mBackgroundTask.execute(KPT_ACTION_PACKAGE_UNINSTALL, packageName);
				}
				else {
					mUninstallPackageList.add(packageName);
				}
			}
		}


	}

	/**
	 * Installs the Addon from the supplied package name extracted from intent
	 * @param addOnIntent Intent received from broadcast receiver
	 */
	private void installAddOn(Intent addOnIntent) {
		Bundle extrasBundle = addOnIntent.getExtras();
		if(extrasBundle != null) {
			String packageName = extrasBundle.getString(PACKAGE_NAME);
			if(packageName != null) {
				if(mCoreServiceHandle == null) {
					mCoreServiceHandle = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
					mCoreServiceHandle.registerCallback(this);
				}

				//  Check if already some package is being installed or uninstalled.
				if(mBackgroundTask == null ||
						mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
					mBackgroundTask = new BackgroundTask();
					mBackgroundTask.execute(KPT_ACTION_PACKAGE_INSTALL, packageName);
				}
				else {
					mInstallPackageList.add(packageName);
				}
			}
		}
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {

	}

	public static boolean searchForPreInstallAddons(Context ctx) {
		boolean retVal = false;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor editor = sharedPref.edit();
		editor.putBoolean(SEARCH_PREINSTALL_ADDONS, true);
		editor.commit();
		PackageManager pm = ctx.getPackageManager();
		ArrayList<PackageInfo> pInfoList = (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
		for(int i=0;i<pInfoList.size();i++) {
			if(pInfoList.get(i).packageName.contains(KPTPackageListener.sAddonPackageName)) {
				Intent installAddOn = new Intent(ctx,KPTPackageHandlerService.class);
				installAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_INSTALL);
				installAddOn.putExtra(KPTPackageHandlerService.PACKAGE_NAME, pInfoList.get(i).packageName);
				if(!retVal){
					Editor shpEditor = sharedPref.edit();
					shpEditor.putBoolean(KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION, true);
					shpEditor.commit();
				}
				installAddOn.setFlags(Intent.FLAG_FROM_BACKGROUND);
				ctx.startService(installAddOn);
				retVal = true;
			}
		}

		//TODO: IF retval is false and base has been upgraded flag is true get package names from db and 
		//remove the dangling incompatible addons (i.e.installAddOn.setAction(KPTPackageHandlerService.KPT_ACTION_PACKAGE_UNINSTALL);)
		return retVal;
	}


	/**
	 * Background Task for runnning installation of addon
	 * packages asynchronously.
	 * @author 
	 *
	 */
	class BackgroundTask extends AsyncTask<String, Void, String> {

		private String mCurrentInstallationState;

		@Override
		protected String doInBackground(String... params) {

			Thread thisThread = Thread.currentThread();
			thisThread.setPriority(Thread.MIN_PRIORITY);

			mCurrentInstallationState = params[0];

			String dictName = null;

			if(KPT_ACTION_PACKAGE_INSTALL.equalsIgnoreCase(mCurrentInstallationState)) {
				dictName = handlePackageInstallation(params[1]);
			}
			else if(KPT_ACTION_PACKAGE_UNINSTALL.equalsIgnoreCase(mCurrentInstallationState)) {
				dictName = handlePackageUnInstallation(params[1]);
			} else if(KPT_ACTION_PACKAGE_UPGRADE.equalsIgnoreCase(mCurrentInstallationState)) {
				dictName = handlePackageUpgradation(params[1]);
			}

			return dictName;
		}


		@Override
		protected void onPostExecute(String resultDictName) {

			// Completed addon installation. Raise a 'Toast'!
			if(resultDictName != null) {
				if(KPT_ACTION_PACKAGE_INSTALL.equalsIgnoreCase(mCurrentInstallationState) || 
				   KPT_ACTION_PACKAGE_UPGRADE.equalsIgnoreCase(mCurrentInstallationState)) {
					// Dictionary installation is success
					raiseToast(getResources().getString(R.string.kpt_package_install_success_toast), resultDictName);
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
					Editor sharedEditor = pref.edit();
					if(!(pref.getBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, false))){
						sharedEditor.putBoolean(KPTConstants.PREF_IS_ADDON_INSTALLED, true);
						sharedEditor.commit();
					}
					
				} else {
					// Dictionary uninstallation is success
					raiseToast(getResources().getString(R.string.kpt_package_uninstall_success_message), resultDictName);					
				}

			}
			else {
				// Dictionary installation/uninstallation failed.
			}

			// Check if any more dictionaries are waiting in list to be installed and uninstalled.
			// First execute install then uninstall incase user removes as soon as he installs.
			if(!mInstallPackageList.isEmpty()) {
				// Packages are there to be installed

				mBackgroundTask = new BackgroundTask();
				String nextPackage = mInstallPackageList.get(0);
				mInstallPackageList.remove(0);
				mBackgroundTask.execute(KPT_ACTION_PACKAGE_INSTALL, nextPackage);
			} else if(!mUninstallPackageList.isEmpty()) {
				// Packages are there to be uninstalled

				mBackgroundTask = new BackgroundTask();
				String nextPackage = mUninstallPackageList.get(0);
				mUninstallPackageList.remove(0);
				mBackgroundTask.execute(KPT_ACTION_PACKAGE_UNINSTALL, nextPackage);
			} else if(!mUpgradePackageList.isEmpty()) {
				mBackgroundTask = new BackgroundTask();
				String nextPackage = mUpgradePackageList.get(0);
				mUpgradePackageList.remove(0);
				mBackgroundTask.execute(KPT_ACTION_PACKAGE_UPGRADE, nextPackage);
			} /*else if(hasBaseBeenUpgraded){
					//Base has been upgraded. Now copy the backed-up profile files
					File ExternalFileContext = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ KPTMyDictionary.EXTERNAL_FILE_PATH_CONTEXT);
					File ExternalFileDict = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ KPTMyDictionary.EXTERNAL_FILE_PATH_DICTIONARIES);
					File InternalFileContext = new File(getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_CONTEXT_FILE_PATH);
					File InternalDictFile = new File(getFilesDir().getAbsolutePath()+ KPTMyDictionary.USER_DICT_FILE_PATH);
					try {
						RestoreProfile(ExternalFileContext, InternalFileContext);
						RestoreProfile(ExternalFileDict, InternalDictFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Editor sharedPrefEditor = sharedPref.edit();
					sharedPrefEditor.putBoolean(KPTAdaptxtIME.PREF_UPGRADE_DICTIONARY_BACKUP, false);
					sharedPrefEditor.commit();
					boolean isServiceStopped = stopSelfResult(mStartId);
					if(isServiceStopped) {
					}
			}*/else {
				// The last task/request for installation/uninstallation of package
				// is finished hence stop the service.
				// Restoring the baseUpgrade preference when the Add-on service is going to stop.
				// This will make sure that the baseUpgrade scenario will never be handled more than
				// once after the upgrade operation is done.
				/*SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
				if(sharedPref.getBoolean(KPTAdaptxtIME.PREF_BASE_HAS_BEEN_UPGRADED, false)) { 
					Cursor cur = mDbAdapter.fetchAllItems();
					int fileNameColumn = cur.getColumnIndex(KPTDatabaseHelper.FILE_NAME);
					int pkgNameCoreColumn =  cur.getColumnIndex(KPTDatabaseHelper.PKG_CORE_VERSION);
					int pkgNameDevColmun =  cur.getColumnIndex(KPTDatabaseHelper.PKG_DEVICE_VERSION);
					if(cur !=null && cur.moveToFirst()) {
						do {
							if(!cur.getString(pkgNameCoreColumn).equals(cur.getString(pkgNameDevColmun))) {
								if(cur.getString(fileNameColumn) != null) {
									boolean isCompatible;
									try {
										int rId = KPTPackageHandlerService.this.getResources().getIdentifier(cur.getString(fileNameColumn), "bool", getPackageName());
										isCompatible = KPTPackageHandlerService.this.getResources().getBoolean(rId);
									} catch (Resources.NotFoundException e) {
										isCompatible = false;
									}
									if(!isCompatible) {
										uninstallPackageinCore(cur.getString(fileNameColumn));
										if(mDbAdapter.itemExists(cur.getString(fileNameColumn))) {
											mDbAdapter.deleteItem(cur.getString(fileNameColumn));
										}
									}
								}
							}
						} while (cur.moveToNext());
					}
				}

				Editor editor = sharedPref.edit();
				editor.putBoolean(KPTAdaptxtIME.PREF_BASE_HAS_BEEN_UPGRADED, false);
				editor.commit();*/
				// Fix for TP 7148
				Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this).edit();
				sharedPrefEditor.putString(KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, null);
				sharedPrefEditor.putString(KPTConstants.PREF_CORE_UNINSTALL_PACKAGE_NAME, null);
				// make sure the Core is not in maintenance mode
				if(mCoreEngine != null){
					mCoreEngine.setCoreMaintenanceMode(false);
				}
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
				
				sharedPrefEditor.putBoolean(KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION, false);
				mBaseUpdateIncompatibleOccured = false;
				//KPTLog.e("CORE LOG"," BY DEFAULT WE ARE MAKING CORE Setting as "+false);
				sharedPrefEditor.commit();
				boolean isServiceStopped = stopSelfResult(mStartId);
				if(isServiceStopped) {
					//KPTLog.e(sAdaptxtDebug, "KPTPackageHandler Service stopped successfully");
				}
			}
		}
		/**
		 * Starts package upgradation process in the background.
		 * Upgradation broadcast event is received after un-installation and Installation broadcast event
		 * @param packageName package to be installed
		 * @return dictionary name of installed package
		 */

		private String handlePackageUpgradation(String packageName) {
			String resultDictName = null;
			// Core Engine takes care of add-on upgradation and also retains the context
			resultDictName = handlePackageInstallation(packageName);
			return resultDictName;
		}

		/**
		 * Starts package installation process in the background.
		 * @param packageName package to be installed
		 * @return dictionary name of installed package
		 */
		private String handlePackageInstallation(String packageName) {
			// TP 7956: If Core is in learning mode even before installation starts, then set the "Search for Pre-install" to true
			// ex: Learn manager is doing "Core learning" and a add-on installation is triggered
			SharedPreferences checkPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
			if(checkPref.getBoolean(KPTConstants.PREF_CORE_LEARNING_INPROGRESS, false)){
				// now Add-on installation will be triggered either while opening SIP (or)
				// while opening Add-on manager
				SharedPreferences.Editor sharedPrefEditor = checkPref.edit();
				sharedPrefEditor.putBoolean(SEARCH_PREINSTALL_ADDONS, false); // true is the default value for failure case
				sharedPrefEditor.commit();
				return null;
			}
			
			
			if(isCompatible(packageName)) {
				//KPTLog.e("KPTTEST", "Install Add-On, Compatible");
				//If addon or base inform that they are compatible continue with installation
				// Update Shared preferences before dictionary is installed
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
				SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
				// Last standing add-on in core should be un-installed on encountering
				// the next first compatible Add-on installation.
				//String lastAddonFileName = sharedPref.getString(KPTConstants.PREF_LAST_ADDON_STANDING, null);
				/*if(lastAddonFileName != null) {
					StringBuffer sb = new StringBuffer(packageName);
					// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
					int dictNameIndex = sb.reverse().toString().indexOf('.');
					String dictFileName = new StringBuffer(sb.substring(0, dictNameIndex)).reverse().toString();
					if(!lastAddonFileName.equals(dictFileName)) {
						Cursor cur = mDbAdapter.fetchItem(lastAddonFileName);
						if(cur !=null) {
							String packageN = cur.getString(mDbAdapter.fetchItem(lastAddonFileName).getColumnIndex(KPTDatabaseHelper.PKG_NAME_CORE));
							mUninstallPackageList.add(packageN);
							cur.close();
						}
					}
					// fix 7113, reset the LastAddonStanding to NULL on encountering the first compatible add-on installation.
					//sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, null);
				}*/
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);

				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, true);

				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, true);
				
				// broadcasting package installation started(for possible future purpose)
				Intent startIntent = new Intent();
				startIntent.setAction(KPT_ACTION_BROADCAST_ADDON_INSTALLATION_STARTED);
				startIntent.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				getApplicationContext().sendBroadcast(startIntent);
				

				// Fetching the dictFilename required for Core Engine installation
				// from the last part of Addon pkg name.(ex: com.adaptxt.addon.engus)
				StringBuffer sb = new StringBuffer(packageName);
				// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
				int dictNameIndex = sb.reverse().toString().indexOf('.');
				String pkgName = new StringBuffer(sb.substring(0, dictNameIndex)).reverse().toString();
				sharedPrefEditor.putString(KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, pkgName);
				sharedPrefEditor.commit();

				KPTCoreFileInfo pkgFile = copyAddonFiles(packageName);
				String resultDictName = null;

				// If both license file and core files are copied then proceed with installation
				if(pkgFile != null) {
					resultDictName = installPackageinCore(pkgFile.mCoreFilePath, pkgFile.mCoreFileName);

					// delete the atp file after addon has been installed
					deleteFile(pkgFile.mCoreFilePath);
				}

				//Update Shared preferences once dictionary is installed
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
				
				// broadcasting package un-installation started(for possible future purpose)
				Intent finishedIntent = new Intent();
				finishedIntent.setAction(KPT_ACTION_BROADCAST_ADDON_INSTALLATION_FINISHED);
				finishedIntent.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
				getApplicationContext().sendBroadcast(finishedIntent);

				if(mInstallPackageList.isEmpty() && mUninstallPackageList.isEmpty()) {
					sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
				}
				sharedPrefEditor.commit();

				String dictDisplayName = null;
				KPTParamDictionary[] installedDicts = null;
				if(mCoreEngine !=null) {
					installedDicts = mCoreEngine.getInstalledDictionaries();
				}
				if(installedDicts !=null) {
					for(int i=0; i<installedDicts.length; i++) {
						if(installedDicts[i].getDictFileName().equals(pkgName)) {
							dictDisplayName = installedDicts[i].getDictDisplayName();
							break;
						}
					}
				}
				// After Add-on installation there can be 2 cases
				// (i) if the installed Add-on is an upgrade, then the existing item in compatibility DB needs to be updated
				// (ii) if its a fresh installation, create a new entry in compatibility Db.
				String  addOnPackageVersion =  getAddonPkgVersion(packageName);
				//KPTLog.e("Compatibility", "resultDictName-->"+resultDictName);
				//KPTLog.e("Compatibility", "pkgName-->"+pkgName+" packageName-->"+packageName +" addOnPackageVersion-->"+addOnPackageVersion);

				//Commented due to limitation in Core. IF the add-on already exists the resultdictname is null, but we need to update DB
				//if(resultDictName != null && addOnPackageVersion  != null) {
				if(addOnPackageVersion  != null && dictDisplayName != null) {
					if(mDbAdapter.itemExists(pkgName)) {
						boolean res = mDbAdapter.updateItem(pkgName, packageName, addOnPackageVersion, addOnPackageVersion, dictDisplayName);
						//KPTLog.e("Compatibility", "ItemExists and update res-->"+res);
					} else {
						long res = mDbAdapter.createItem(pkgName, packageName, addOnPackageVersion, addOnPackageVersion, dictDisplayName);
						//KPTLog.e("Compatibility", "Create Item ID-->"+res);
					}
				}else {
					//KPTLog.e("Compatibility", "NOT CREATING ITEM IN DB");
				}

				return resultDictName;
			}  else {
				// The installed Add-on APK is incompatible
				// (i) if it is an Add-on upgrade & the Add-on language is already in Core, update the compatibility DB with the device version information
				// (ii) if its a base upgrade & the Add-on already in Core, un-install from the Core engine.
				String  addOnPackageVersion =  getAddonPkgVersion(packageName);
				StringBuffer sb = new StringBuffer(packageName);
				// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
				int dictNameIndex = sb.reverse().toString().indexOf('.');
				String dictFileName = new StringBuffer(sb.substring(0, dictNameIndex)).reverse().toString();
				
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
				boolean baseUpdateAddonInstallation = sharedPref.getBoolean(KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION, false);

				if(actionString.equals(KPT_ACTION_PACKAGE_UPGRADE) && mDbAdapter.itemExists(dictFileName)
						&& mDbAdapter.fetchItem(dictFileName).getString(mDbAdapter.fetchItem(dictFileName).getColumnIndex(KPTDatabaseHelper.PKG_NAME_CORE)).equals(packageName)){
					//KPTLog.e("KPTTEST", "Non-Compatible Add-On installed, reverting back to older version core files and updating DB");
					mDbAdapter.updateItem(dictFileName, null, null, addOnPackageVersion, null);
					Editor sharedPrefEditor = sharedPref.edit(); 
					sharedPrefEditor.putBoolean(KPTConstants.PREF_SHOW_INCOMPATIBLE_UPDATE_DIALOG, true);
					sharedPrefEditor.commit();
				} else if(baseUpdateAddonInstallation && !mBaseUpdateIncompatibleOccured){
					//KPTLog.e("KPTTEST", "Uninstall Add-On, Not Compatible");
					/*if(mDbAdapter.itemExists(dictFileName)) {
						mUninstallPackageList.add(packageName);
					}*/
					mBaseUpdateIncompatibleOccured = true;
//					Editor sharedPrefEditor = sharedPref.edit(); 
//					sharedPrefEditor.putBoolean(KPTConstants.PREF_SHOW_BASE_UPDATE_INCOMPATIBLE_DIALOG, true);
//					sharedPrefEditor.commit();
					
				}
				return null;
			}


		}


		private boolean isCompatible(String packageName) {
			//KPTLog.e("KPTTEST","PackageName is-->"+packageName);
			boolean isCompatible = false;
			if(packageName != null) {
				try {
					Context addonContext = createPackageContext(packageName, 0);
					Resources addonResources = getPackageManager().getResourcesForApplication(addonContext.getApplicationInfo());
					String addOnPackageVersion = addonContext.getResources().getString(addonResources.getIdentifier(VERSION_NAME, "string", packageName));
					String basePackageVersion = getResources().getString(R.string.kpt_version_name);

					boolean addOnCompatible = false;//Addon says whether it it compatible with the base
					boolean baseCompatible = false;//Base says whether it is compatible with the addon
					try {
						int rId = getResources().getIdentifier(addOnPackageVersion, "bool", getPackageName());
						if(rId!=0) {
							baseCompatible = getResources().getBoolean(rId);
							//KPTLog.e("KPTTEST", "As per Base is AddOn Compatible??"+baseCompatible);
						} else {
							//KPTLog.e("KPTTEST", "Base has no Info abt addon");
						}

					} catch (Resources.NotFoundException e) {
						//KPTLog.e("KPTTEST","Res Not Found Base");
						baseCompatible = false;
					}
					try {
						int resourceID = addonResources.getIdentifier(basePackageVersion, "bool", packageName);
						if(resourceID!=0) {
							addOnCompatible = addonResources.getBoolean(resourceID);
						}else {
							//KPTLog.e("KPTTEST", "Addon has no info about base");
						}
					}catch(Resources.NotFoundException e) {
						addOnCompatible = false;
					}
					if(!baseCompatible && !addOnCompatible) {
						//Both Base and Add-On declare that they are not compatible. So donot Install.
						//Once Add-On and base XML's are updated remove installAddon and uncomment stopSelf method
						//KPTLog.e("KPTTEST", "Donot Install Add-On");
						isCompatible = false;
						
						// Throw the below notification only on normal Add-on installation incompatibility
						// and not on BaseUpdate-searchForPreInstallAdd-on scenario
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
						boolean baseUpdateAddonInstallation = sharedPref.getBoolean(KPT_ACTION_BASE_UPGRADE_PACKAGE_INSTALLATION, false);
						
						if(!baseUpdateAddonInstallation){
							// Notify that the Add-on is incompatible
							String DISPLAY_NAME = "display_name";
							Context context = getApplicationContext();
							/*CharSequence addonDisplayName = null;
							try{
								addonDisplayName = addonContext.getResources().getText(addonResources.getIdentifier(DISPLAY_NAME, "string", packageName));
							}catch(Resources.NotFoundException e){
								//The old add-ons will not have this string, so try from androidaddoncontent.xml
								HashMap<String,String> addonMap = parseAddonContentXML();
								if(addonMap != null){
									StringBuffer sb = new StringBuffer(packageName);
									// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
									int dictNameIndex = sb.reverse().toString().indexOf('.');
									String dictFileName = new StringBuffer(sb.substring(0, dictNameIndex)).reverse().toString();
									addonDisplayName = addonMap.get(dictFileName);
								}
							}
							// if the androidaddoncontent xml file is not yet downloaded, use the default generic string
							if(addonDisplayName == null){
								addonDisplayName = getResources().getText(R.string.kpt_incompatible_title_header);
							}*/
							CharSequence contentTitle = getResources().getText(R.string.kpt_incompatible_title);
							CharSequence contentText = getResources().getText(R.string.kpt_incompatible_message);
							NotificationManager nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
							final Notification notifyDetails = new Notification(R.drawable.kpt_notification_icon,
									getResources().getText(R.string.kpt_incompatible_title),System.currentTimeMillis());
							
					
							// Intent for Adaptxt base android market page
							Intent updateIntent = new Intent (Intent.ACTION_VIEW);
							updateIntent.setData (Uri.parse (KPTConstants.MARKET_SEARCH_WORD + basePackageName));
							PendingIntent pIntent = PendingIntent.getActivity(context, 
									0, updateIntent, notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
							notifyDetails.setLatestEventInfo(context,contentTitle, contentText, pIntent);
							nManager.notify(SIMPLE_NOTFICATION_ID, notifyDetails);
						}
						//stopSelf();

					} else if(baseCompatible || addOnCompatible) {
						//Base or Add-On declare that they are compatible. So install.
						//KPTLog.e("KPTTEST", "<--INSTALL ADDON-->"+"base-->"+baseCompatible+" addon-->"+addOnCompatible);
						isCompatible = true;

					}

				} catch (NameNotFoundException e) {
					e.printStackTrace();
					isCompatible = false;
				}
			}
			return isCompatible;
		}
		
		// back up mechanism of getting the dsiplay name from androidaddoncontent XML file
		// for showing the incompatible notification
		/*private HashMap<String, String> parseAddonContentXML() {
			final String XML_DICT_DISPLAY_NAME = "displayname";
			final String XML_DICT_FILE_NAME = "filename";
			final String XML_CONTENTS_TAG = "contents";
			final String XML_CONTENT_TAG = "content";
			final String XML_FILE_NAME = "androidaddoncontent.xml";
			
			InputStream is = null;
			XmlPullParser parser = null;
			File myFile = new File(getFilesDir().getAbsolutePath()+"/" + XML_FILE_NAME);
			if(myFile.exists()){
				try {
					is = new FileInputStream(myFile);
					parser = new KPTKXmlParser();
					parser.setInput(is, null);
					String displayname = null;
					String filename = null;
					boolean done = false;
					int i=-1;
					HashMap<String, String> addonContentMap = new HashMap<String, String>();
					try {
						int eventType = parser.getEventType();
						do {
							switch(eventType)
							{
							case XmlPullParser.START_TAG:
								if(parser.getName().equals(XML_DICT_DISPLAY_NAME)){
									displayname = parser.nextText();
									i++;
								} else if(parser.getName().equals(XML_DICT_FILE_NAME)) {
									filename = parser.nextText();
									addonContentMap.put(filename.toLowerCase(),displayname);
								} 
	
								break;
							case XmlPullParser.END_TAG:
								if(parser.getName().equals(XML_CONTENT_TAG)) {
									displayname = null;
									filename =  null;
								}else if(parser.getName().equals(XML_CONTENTS_TAG)){
									done = true;
								}
								break;
							}
							eventType = parser.next();
	
						} while (!done && eventType != XmlPullParser.END_DOCUMENT);
						
						return addonContentMap;
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				finally
				{
					if(is != null)
					{
						try {
							is.close();
						} catch (IOException e) {}
					}
				}
			}
			return null;
		}*/


		public String getAddonPkgVersion(String packageName) {
			Context addonContext;
			String addOnPackageVersion = null;
			try {
				addonContext = createPackageContext(packageName, 0);
				Resources addonResources = getPackageManager().getResourcesForApplication(addonContext.getApplicationInfo());
				addOnPackageVersion = addonContext.getResources().getString(addonResources.getIdentifier(VERSION_NAME, "string", packageName));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			return addOnPackageVersion;
		}

		/**
		 * Starts package uninstallation process in the background.
		 * @param packageName package to be uninstalled
		 * @return dictionary name of uninstalled package
		 */
		private String handlePackageUnInstallation(String packageName) {

			// Update Shared preferences before dictionary is installed
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, true);

			//Bug fix:7875
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);

			
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, true);

			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, true);
			
			// broadcasting package installation started(for possible future purpose)
			Intent startIntent = new Intent();
			startIntent.setAction(KPT_ACTION_BROADCAST_ADDON_UNINSTALLATION_STARTED);
			startIntent.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
			getApplicationContext().sendBroadcast(startIntent);
			
			// Fetching dictfilename from package name to be used for un-installation in Core
			StringBuffer sb = new StringBuffer(packageName);
			// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
			int dictNameIndex = sb.reverse().toString().indexOf('.');
			String pkgName = new StringBuffer(sb.substring(0, dictNameIndex)).reverse().toString();
			sharedPrefEditor.putString(KPTConstants.PREF_CORE_UNINSTALL_PACKAGE_NAME, pkgName);
			sharedPrefEditor.commit();

			// Start package uninstallation
			String pkgNameInstCore = null;
			String pkgVerInsCore = null;
			String pkgVerOnDev = null;
			Cursor cur;
			if(mDbAdapter.itemExists(pkgName)) {
				cur = mDbAdapter.fetchItem(pkgName);
				//Package name of add-on installed in core which is available in database
				pkgNameInstCore = cur.getString(cur.getColumnIndex(KPTDatabaseHelper.PKG_NAME_CORE));
				// Version of the add-on installed in core which is available in database
				pkgVerInsCore = cur.getString(cur.getColumnIndex(KPTDatabaseHelper.PKG_CORE_VERSION));
				// Version of the add-on installed on device which is available in database
				pkgVerOnDev = cur.getString(cur.getColumnIndex(KPTDatabaseHelper.PKG_DEVICE_VERSION));;
				cur.close();
			}

			// Un-install the Add-on only if the Core version and the device version of
			// Add-on in DB record is properly matched. This is done to avoid the Core files
			// being un-installed because of the un-installation of an APK which was an
			// incompatible update.
			String dictDisplayName = null;
			//KPTLog.e("Compatibility", "packageName-->"+packageName+"  pkgNameInstCore-->"+pkgNameInstCore);
			//KPTLog.e("Compatibility", "pkgVerInsCore-->"+pkgVerInsCore+"  pkgVerOnDev-->"+pkgVerOnDev);
			if(pkgNameInstCore!=null && pkgVerOnDev != null && pkgVerInsCore !=null 
					&& packageName.equals(pkgNameInstCore) 
					&& pkgVerInsCore.equals(pkgVerOnDev)) {
				dictDisplayName = uninstallPackageinCore(pkgName);
			} else {
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
				sharedPrefEditor.commit();
			}
			// Remove the record of the Add-on from DB once un-installation is successful.
			if(dictDisplayName !=null) {
				boolean res = mDbAdapter.deleteItem(pkgName);
				//KPTLog.e("Compatibility", "Removed Item From DB-->"+res);
			} else {
				//KPTLog.e("Compatibility", "Item SHOULD NOT Removed From DB");
			}
			//Update Shared preferences once dictionary is uninstalled
			sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
			if(mInstallPackageList.isEmpty() && mUninstallPackageList.isEmpty()) {
				sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
			}
			sharedPrefEditor.commit();
			
			// broadcasting package un-installation started(for possible future purpose)
			Intent finishedIntent = new Intent();
			finishedIntent.setAction(KPT_ACTION_BROADCAST_ADDON_UNINSTALLATION_FINISHED);
			finishedIntent.putExtra(KPTPackageHandlerService.PACKAGE_NAME, packageName);
			getApplicationContext().sendBroadcast(finishedIntent);

			return dictDisplayName;
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
				mCoreEngine = mCoreServiceHandle.getCoreInterface();
				if(mCoreEngine != null) {
					// Core engine must be initialized only once.
					boolean initSuccess = mCoreEngine.initializeCore(KPTPackageHandlerService.this);
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
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
			SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

			// Update the "last Add-on standing" preference on all un-installation.
			// Fix for 7113, this check should be done before the Core un-installation happens.
			installedDicts = mCoreEngine.getInstalledDictionaries();
			/*if(installedDicts != null && installedDicts.length == 1) {
				sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, installedDicts[0].getDictFileName());
			} else {
				sharedPrefEditor.putString(KPTConstants.PREF_LAST_ADDON_STANDING, null);
			}
			sharedPrefEditor.commit();*/

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


			mCoreEngine.updateCoreUserLoadStatus(KPTPackageHandlerService.this);

			if(mUninstallPackageList.isEmpty() && noOfDictsLoaded == 0){
				// TP item 6887, after un-installation of a dictionary, no existing dictionaries are in loaded state
				// so make one of them enabled and as current keyboard language
				KPTParamDictionary[] updatedInstalledDicts = mCoreEngine.getInstalledDictionaries();
				if(updatedInstalledDicts != null && updatedInstalledDicts.length >= 1){
					boolean isEnableSuccess = mCoreEngine.loadDictionary(updatedInstalledDicts[0].getComponentId());
					if(!isEnableSuccess) {
						//KPTLog.e(sAdaptxtDebug, "Enabling the last dictionary Failed");
					}
					mCoreEngine.setDictionaryPriority(updatedInstalledDicts[0].getComponentId(), 0);
				}			
			}
			// If top priority dictionary had been uninstalled then update the keymaps in core.
			mCoreEngine.activateTopPriorityDictionaryKeymap();

			return dictDisplayname;
		}

		/**
		 * Displays a toast message to the user.
		 * @param message Message to be shown
		 */
		private void raiseToast(String installStatus, String pkgName) {
			Context context = getApplicationContext();
			//int duration = Toast.LENGTH_LONG;
			//String message = String.format(installStatus, pkgName);
			CharSequence contentTitle = KPTPackageHandlerService.this.getResources().getText(R.string.kpt_ticker_text);
			CharSequence contentText = installStatus;
			NotificationManager nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			final Notification notifyDetails = new Notification(R.drawable.kpt_notification_icon,
					installStatus,System.currentTimeMillis());
			if(context==null) {
				//KPTLog.e("CONEXT NULL", "");
			}
			Intent notifyIntent = new Intent(KPTPackageHandlerService.this,KPTAddonManager.class);
			PendingIntent pIntent = PendingIntent.getActivity(context, 
					0, notifyIntent, notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
			if(pIntent==null) {
				//KPTLog.e("PINTNT NULL", "");
			}
			notifyDetails.setLatestEventInfo(context, contentTitle, contentText, pIntent);
			//notifyDetails.defaults = Notification.FLAG_AUTO_CANCEL;
			nManager.notify(SIMPLE_NOTFICATION_ID, notifyDetails);

			/*Toast toast = Toast.makeText(context, message, duration);
			toast.show();*/
		}

		/**
		 * Copies Addon .atp files to core's internal storage
		 * @param packageName
		 * @return Absolute Core File Name copied
		 */
		private KPTCoreFileInfo copyAddonFiles(String packageName) {
			KPTCoreFileInfo coreFileInfo = null;
			try {
				//KPTLog.e(sAdaptxtDebug, "Copying Files copyAddonFiles");
				Context addOnContext = createPackageContext(packageName, 0);
				AssetManager addOnAssets = addOnContext.getAssets();
				String[] pkgFiles = addOnAssets.list(sCoreAddOnPath);

				for(String fileName : pkgFiles) {
					File inputFile = new File(sCoreAddOnPath, fileName);
					InputStream ipFileStream = addOnAssets.open(KPTConstants.ATX_ASSETS_FOLDER+inputFile.getPath());

					String outputPath = getFilesDir() + sCorePackagePath  ;
					File outputFile = new File(outputPath);
					if(!outputFile.exists()) {
						outputFile.mkdirs();
					}

					OutputStream opFileStream = new FileOutputStream(outputPath + fileName);

					byte[] bytesRead = new byte[(int)mNoofBytesToRead]; 
					int noOfBytes = ipFileStream.read(bytesRead, 0, mNoofBytesToRead);
					while (noOfBytes > 0) {
						opFileStream.write(bytesRead,0,noOfBytes);
						noOfBytes = ipFileStream.read(bytesRead, 0, mNoofBytesToRead);
					}
					opFileStream.flush();
					opFileStream.close();
					ipFileStream.close();

					// unzip the copied file
					String coreAtpFile =  unzip(outputPath + fileName);

					if(coreAtpFile == null) {
						// unzipping the core file failed
						//KPTLog.e(sAdaptxtDebug, "Unzipping the zip file failed in copyAddonFiles");
						return null;
					}
					else {
						// TODO: Bug in Core. Additional Path separator should be removed.
						coreFileInfo = new KPTCoreFileInfo();
						coreFileInfo.mCoreFilePath = outputPath + coreAtpFile;
						StringBuffer sb2 = new StringBuffer(packageName);
						// Fix for TP 7128, Assuming that the package name signature will always have "DictFileName" as its end part
						int dictNameIndex = sb2.reverse().toString().indexOf('.');
						coreFileInfo.mCoreFileName = new StringBuffer(sb2.substring(0, dictNameIndex)).reverse().toString();
					}

					// delete the zip file after it has been unzipped
					boolean result = deleteFile(outputPath + fileName);
					if(!result) {
						// Error in disposing zip file
						//KPTLog.e(sAdaptxtDebug, "Deleting zip file failed in copyAddonFiles");
						return null;
					}

				}

			} catch (NameNotFoundException e) {
				// Failed to get context
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// Failed to get package files from assets
				// Or failed to write to output path
				e.printStackTrace();
				return null;
			}

			return coreFileInfo;
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
		private String installPackageinCore(String packageFilePath, String packageFileName) {

			if(mCoreEngine == null) {
				mCoreEngine = mCoreServiceHandle.getCoreInterface();;
				if(mCoreEngine != null) {
					// Core engine must be initialized only once.
					boolean initSuccess = mCoreEngine.initializeCore(KPTPackageHandlerService.this);
					//KPTLog.e(sAdaptxtDebug, "Core init status " + initSuccess);
				}
				else {
					// Core not yet initialized.
					//KPTLog.e(sAdaptxtDebug, "Core not yet initilaized ");
					return null;
				}
			}

			String installedDictName = null;
			if(mCoreEngine != null) {
				//KPTLog.e(sAdaptxtDebug, "Start core installation installPackageinCore " + packageFilePath);

				// Set the core state to blacklisted so that no text insertion/
				// get suggestion operation take place.
				// maintenance mode will be reset by core in updateCoreUserLoadStatus()
				mCoreEngine.setCoreMaintenanceMode(true);

				// TODO: Bug in Core. getAvailablePackages needs to be removed once core fixes the bug
				KPTPackage[] pkgList = mCoreEngine.getAvailablePackages();
				if(pkgList != null) {
					//KPTLog.e(sAdaptxtDebug, "Available Package Cnt" + String.valueOf(pkgList.length));
					for(KPTPackage addOnPkg : pkgList) {
						//KPTLog.e(sAdaptxtDebug, "Package Name" + addOnPkg.getPackageName() + " Id: " + addOnPkg.getPackageId());
					}
				}

				int resultPkgId = mCoreEngine.installAddOnPackage(packageFilePath);




				// Reset the black listing state to false
				//mCoreEngine.setCoreMaintenanceMode(false);


				if(resultPkgId > 0) {
					//KPTLog.e(sAdaptxtDebug, "Start core installation SUCCESS Id: " + resultPkgId);

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
						// Licensing Failed
						//KPTLog.e(sAdaptxtDebug, "licensing addon FAILED");
						/*By default add-ons are fully licensenced. In this case if add on installation fails because 
						of base licensence expiry then used to show licensence expiry msg instead of add Addon install msg on the CV.*/
						
						/*SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(KPTPackageHandlerService.this);
						SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
						sharedPrefEditor.putBoolean(KPTConstants.PREF_CORE_LICENSE_EXPIRED, true);
						sharedPrefEditor.commit();*/
						
					}
					else {
						// Licensing is successful
						//KPTLog.e(sAdaptxtDebug, "Start core licesing SUCCESS");

						// Update core user load status once first package is installed.
						mCoreEngine.updateCoreUserLoadStatus(KPTPackageHandlerService.this);
						//KPTLog.e(sAdaptxtDebug, "updateCoreUserLoadStatus done");

						// Log all details after licensing
						// TODO: to remove this log
						//logAllDetails();

						KPTPackage[] pkgAvailArray = mCoreEngine.getInstalledPackages();
						if(pkgAvailArray != null) {
							for(KPTPackage installedPkg : pkgAvailArray) {
								if(installedPkg.getPackageId() == resultPkgId) {
									KPTPackageComponentInfo[] compInfoListArray = installedPkg.getComponents();
									if(compInfoListArray == null || compInfoListArray.length == 0) {
										// Component Info Array empty
										//KPTLog.e(sAdaptxtDebug, "Installed pkg component list null");
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
													//KPTLog.e(sAdaptxtDebug, "Installing and getting dictionary details is success " + dictDisplayName);
													installedDictName = dictDisplayName;


													// Update the keymap in core if this the first language being installed.
													if(pkgAvailArray.length == 1) {
														boolean setKeymapResult = mCoreEngine.activateLanguageKeymap(installedDictionary.getComponentId(), installedDictionary);
														if(!setKeymapResult) {
															//KPTLog.e(sAdaptxtDebug, "Set keymap failed");
														}
													}
												}
												else {
													//KPTLog.e(sAdaptxtDebug, "Installed pkg component dictionary display name null");
												}

											}
											else {
												// Dictionary details inside the component is null
												//KPTLog.e(sAdaptxtDebug, "Installed pkg component dictionary details are null");
											}
										}
										else {
											// Installed component is not a dictionary component
											//KPTLog.e(sAdaptxtDebug, "Installed pkg component is not dictionary component");
										}
									}
									break;
								}
							}
						}
						else {
							//KPTLog.e(sAdaptxtDebug, "Installed packages NULL");
						}

					}
				}
				else {
					//KPTLog.e(sAdaptxtDebug, "Start core installation FAILED");
				}
				if(resultPkgId == KPTCoreEngine.PACKAGE_ALREADY_EXISTS) {
					// Update core user load status once first package is installed.
					mCoreEngine.updateCoreUserLoadStatus(KPTPackageHandlerService.this);
				}
			}

			return installedDictName;
		}

		/**
		 * Utility function to log log current component details
		 */
		private void logAllDetails() {

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

		}

		/**
		 * Unzips the supplied file at the same path
		 * @param zipFileName zip file name path
		 */
		private String unzip(String zipFileName) {
			String coreFile = null;
			try {
				File srcFile = new File(zipFileName);
				ZipFile zipFile = new ZipFile(srcFile);

				// create a directory named the same as the zip file in the 
				// same directory as the zip file.
				//File zipDir = new File(file.getParentFile(), "Profile");
				File zipDir = new File(srcFile.getParentFile().getAbsolutePath());
				zipDir.mkdir();

				Enumeration<?> entries = zipFile.entries();
				while(entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();

					String nme = entry.getName();
					coreFile = nme;
					// File for current file or directory
					File entryDestination = new File(zipDir, nme);

					// This file may be in a subfolder in the Zip bundle
					// This line ensures the parent folders are all
					// created.
					entryDestination.getParentFile().mkdirs();

					// Directories are included as separate entries 
					// in the zip file.
					if(!entry.isDirectory()) {
						boolean result = generateFile(entryDestination, entry, zipFile);
						if(!result) {
							return null;
						}
					}
					else {
						entryDestination.mkdirs();
					}
				}
				zipFile.close();
			}
			catch(IOException e) {
				e.printStackTrace();
				return null;
			}

			return coreFile;
		}

		/**
		 * Copies the content from the file in the zipped folder/file to the destination file
		 * @param entryDestination Destination file wher it is copied
		 * @param entry Src file inside ZIp file
		 * @param zipFile Entire Zip file
		 */
		private boolean generateFile(File destination, ZipEntry entry, ZipFile owner) {
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
				return false;
			}

			return true;
		}
	}

}
