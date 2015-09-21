package com.kpt.adaptxt.beta;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kpt.adaptxt.beta.settings.AddonDownloadActivity;
import com.kpt.adaptxt.beta.settings.AddonDownloadActivity.AddonData;
import com.kpt.adaptxt.beta.settings.AddonDownloadActivity.DownloadAddOnTask;
import com.kpt.adaptxt.beta.util.KPTConstants;




public class AdaptxtApplication extends Application {

	private Map<String, DownloadAddOnTask> mDownloadTasksMap;
	
	private boolean mIsFromThemeDetailsPage;
	//private AnalyticsServerTask mAnalyticServerTask = null;
	//private ArrayList<ServerTaskInfo> mServerTaskInfoList;
	private final String licenseFilePath = "/TestUser/Dictionaries/licence.dat";
	private String tapJoyAppId ="1e62f33c-63ac-4773-ad06-7410359f8659"; 
	private String tapSecretKey ="vaq66bkRIuUUUqkiSuoY"; 
	
	
	 /**
	 * List of addons that are currently being installed.
	 * This list is shared acrross all components (Activites, Service etc) of ADAPTXT,
	 * with the help of this Adaptxt Application Object.
	 * The changes in the list will aid in refreshing the UI AddonManager screens.
	 * An element is added into the list when installation into core has started (KPTPkgService is started).
	 * To be specific this is used for 
	 */
	private ArrayList<String> mInstallingList;
	/**
	 * List of addons that are currently being upgraded.
	 * This list is shared acrross all components (Activites, Service etc) of ADAPTXT,
	 * with the help of this Adaptxt Application Object.
	 * The changes in the list will aid in refreshing the UI AddonManager screens.
	 * An element is added into the list after the update available addon is downloaded 
	 * and installation into core has started (KPTPkgService is started).
	 * To be specific this is used for showing the correct kind of ProgressBar
	 */
	private ArrayList<String> mUpgradingList;
	
	/**
	 *  Map which gives all info like display name compatibility etc
	 *  (Key:DisplayName Value:AddonData Object)
	 */
	private Map<String, AddonData> mAddonDataMap;
	
	
	/**
	 * flag is used to know whether the customization dialog is being displayed,
	 * when screen is locked
	 */
	private boolean mCustDialogDisplayed;
	
	
	private Context mContext;

	private static AdaptxtApplication sAdaptxtApplication = null;
	
	public static AdaptxtApplication getAdaptxtApplication(Context context) {
		if(sAdaptxtApplication == null) {
			sAdaptxtApplication = new AdaptxtApplication(context);
		}
		return sAdaptxtApplication;
	}
	
	
	public AdaptxtApplication(Context context){
		onCreate(context);
	}
	
	
	public void onCreate(Context context) {
		//super.onCreate();
		mContext = context;
		resetSharedPreferences(mContext);
		//TapjoyConnect.requestTapjoyConnect(mContext, tapJoyAppId, tapSecretKey);
		
		mDownloadTasksMap = new HashMap<String, AddonDownloadActivity.DownloadAddOnTask>();
		
		//mServerTaskInfoList = new ArrayList<ServerTaskInfo>();
		
		mAddonDataMap = new HashMap<String, AddonData>();
		
		mInstallingList = new ArrayList<String>();
		mUpgradingList = new ArrayList<String>();
		
		SharedPreferences.Editor versionInfoSharedPrefEditor = mContext.getSharedPreferences(AddonDownloadActivity.INSTALLED_ADDONS_VERSIONINFO, Context.MODE_PRIVATE).edit();
		versionInfoSharedPrefEditor.putString("engus", "1.0");
		versionInfoSharedPrefEditor.commit();
		
	}

	public Map<String, DownloadAddOnTask> getDownloadTask() {
		return mDownloadTasksMap;
	}
	
	

	

	
	public Map<String, AddonData> getAddOnDataMap() {
		return mAddonDataMap; 
	}
	
	
	
	
	
	/**
	 * Returns a List which will be filled with addons for which 
	 * installation into core has started (KPTPkgService is started)
	 */
	public ArrayList<String> getInstallingList() {
		return mInstallingList;
	}
	/**
	 * Returns a List which will be filled with addons for which 
	 * installation of upgradable addons into core has started (KPTPkgService is started)
	 */
	
	public ArrayList<String> getUpgradingList() {
		return mUpgradingList;
	}

	/*public void setDownloadTasksMap(Map<String, DownloadAddOnTask> tasksMap) {
		mDownloadTasksMap = tasksMap;
	}
	*/
	
	public boolean isCustDialogDisplayed() {
		return mCustDialogDisplayed;
	}

	public void setCustDialogDisplayed(boolean custDialogDisplayed) {
		mCustDialogDisplayed = custDialogDisplayed;
	}
	
	/*public AnalyticsServerTask getAnalyticServerTask(){
		return mAnalyticServerTask;
	}
	
	public void setAnalyticServerTask(AnalyticsServerTask analyticsServerTask){
		mAnalyticServerTask = analyticsServerTask;
	}
	
	public ArrayList<ServerTaskInfo> getServerTaskInfoList(){
		return mServerTaskInfoList;
	}
	
	public void setServerTaskInfoList(ArrayList<ServerTaskInfo> list){
		mServerTaskInfoList = list;
	}*/
	
	public boolean getIsFromThemeDetailsPage(){
		return mIsFromThemeDetailsPage;
	}
	
	public void setIsFromThemeDetailsPage(boolean fromThemeDetailsPage){
		mIsFromThemeDetailsPage = fromThemeDetailsPage;
	}
	
	/**
	 * Resetting preferences for some use cases where installation and un-installation prefs have to be reset
	 * For Ex: while Addon installation, user pulls the battery out and then again launches Adaptxt, progress
	 * dialog is continously shown when user goes into add-on manager screen
	 * 
	 * @param clientContext
	 */
	private void resetSharedPreferences(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		Editor sharedEdit = sharedPref.edit();
		sharedEdit.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
		sharedEdit.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
		sharedEdit.putBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
		sharedEdit.commit();
	
		File file = new File(mContext.getFilesDir().getAbsolutePath()+"/" + AddonDownloadActivity.S3_COMPATABILITY_XML);

		if(file.exists()){
			file.delete();
			Log.e("KPTADAPTXT APPLICATION", "file exits " + file.delete());
		}


	}

}
