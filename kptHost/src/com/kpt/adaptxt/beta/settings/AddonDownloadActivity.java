package com.kpt.adaptxt.beta.settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kpt.adaptxt.beta.AdaptxtApplication;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.packageinstaller.KPTPkgHandlerService;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.util.KPTKXmlParser;
import com.kpt.adaptxt.core.coreapi.KPTParamComponentInfo;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;

public class AddonDownloadActivity extends Activity implements Runnable, 
OnSharedPreferenceChangeListener, KPTCoreServiceListener, TextWatcher {

	
	private HorizontalScrollView mParentHorizontalScroll;
	private ListView mLanguageListView;
	private ListView mSearchListView;
	private SharedPreferences mSharedPref;
	
	private float mDownX = -1;
	
	private  final int FIRST_SCREEN = 1;
	private boolean isRightToLeft;
	private int mCurrentScreen = 1;
	public  boolean mDisableMenuItem = false;


	private KPTCoreServiceHandler mCoreServiceHandler;
	private KPTCoreEngine mCoreEngine;
	private boolean isCoreEngineInitialized;
	private KPTParamDictionary[] mInstalledDict;

	public ProgressDialog mProgressDialog;
	private ProgressDialog mAddonUpdateFromIMEDialog;

	private KPTMergeAdapter mLanguagesAdapter;
	private ArrayList<String> mLanguageAddonList = new ArrayList<String>();
	private ArrayList<String> mInstalledLanguageAddonList = new ArrayList<String>();


	private TextView mLeftHeader;/*, mRightHeader;*/

	public static String BASE_URL;
	/**
	 *  Map (Key:DisplayName Value:AddonData Object) which give all info like display name compatibility etc
	 */
	private Map<String, AddonData> mAddonDataMap;

	private Map<String, DownloadAddOnTask> mDownloadAddOnTasksMap ;
	private ArrayList<String> mInstallingList ;

	private int mScreenWidth;
	private Resources mResources;

	public static final String S3_COMPATABILITY_XML = "addoncompatibiltiymatrix2.xml";
	
	public static final String PREF_UNLOADED_ADDONS = "unloaded_addons";
	private boolean mFocusable = true;
	
	public static int CALLED_FROM_DOWNLOAD_ADDON_TASK = -2;

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
	//public static final String XML_PRICE = "price";
	
	public static final String REGEX_PIPE = "\\|"; 
	public static final String PIPE = "|";

	public static final String KEYMAP_TASK_FALILED = "failed";
	public static final String KEYMAP_TASK_SUCCESS = "success";

	public static final String URI_PATH = "http://adaptxt.com/adaptxtlive/faq/android#t2n85";
	
	/**
	 * Name of shared preference file name (versionInfo.xml) that maintains all the info related to 
	 * the versions of addons installed in Core Engine, which is used for compatibility
	 * 
	 */
	public static final String INSTALLED_ADDONS_VERSIONINFO = "versionInfo";

	private static final int MAX_NO_DICTS_LOADED = 4;


	
	/**
	 * Constants to know where the user has stated the downloads from,
	 * So that disable other two tabs. User can download addons from only one
	 * tab at a time.
	 */
	public static final int DOWNLOAD_STARTED_FROM_INSTALLED_ACTIVTY = 1;
	public static final int DOWNLOAD_STARTED_FROM_DOWNLOAD_ACTIVTY = 2;
	public static final int DOWNLOAD_STARTED_FROM_LATEST_ACTIVTY = 3;
	public static final int DOWNLOAD_FINISHED_CANCELLED = 4;
	
	/*
	 * map used for searh feature
	 */
	public static HashMap<String, String []> mAddonSearchTagsMap = new HashMap<String, String []>();
	
	private final int MENU_SEARCH = 0;
	/**
	 * serach field edit text
	 */
	private EditText mSearchEdit;
	/**
	 * This list holds the info of the current searched addons by user.
	 * For Ex: If user enters A, all the addons starting with letter A will be in this list
	 * This list will be filled based on mTempList
	 */
	private ArrayList<String> mCurrentSearchList = new ArrayList<String>();
	/**
	 * This list contains all the info (Language+Technincal) and is not used for drawing. 
	 */
	private ArrayList<String> mTempList = new ArrayList<String>();
	private KPTMergeAdapter mSearchAdapter;
	private boolean mSearchSelected = false;
	
	public static final int DOWNLOAD_RETRY_DELAY = 500; 
	public static final int DOWNLOAD_RETRY_ATTEMPTS = 3;
	
	/**
	 * Indicates whether to use http or https for addon download
	 */
	public static final boolean USE_HTTP_FOR_ADDON_DOWNLOAD = true;
	public static final String USER_AGENT = "Adaptxt";
	/**
	 * time interval to check for any update. This is in milli seconds.
	 * Days * hours * mins * secs * milliseconds
	 */
	public static final int XML_DOWNLOAD_MIN_DAYS = 1;
	public static final long XML_DOWNLOAD_MIN_INTERVAL = XML_DOWNLOAD_MIN_DAYS * 24 * 60 * 60 * 1000;

	private static final String TAG = "AddonDownloadActivity";

	private AdaptxtApplication appState;

	
	private DownloadedAddonAdapter mDownloadedLanguageAddonAdapter;
	private String mDownloadHeaderText;
	private String mInstalledHeaderText;
	private TapToDownloadAddonAdapter languagesAdapter;
	private TapToDownloadAddonAdapter searchAdapter;
	
	public ContextWrapper mContextWrapper;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		mDisableMenuItem = false;
		appState =  AdaptxtApplication.getAdaptxtApplication(this);
		
		mContextWrapper = new ContextThemeWrapper(this, R.style.AdaptxtTheme); 

		mDownloadAddOnTasksMap = appState.getDownloadTask();
		mAddonDataMap = appState.getAddOnDataMap();
		mInstallingList = appState.getInstallingList();
		mResources = getResources();
		
		mScreenWidth = mResources.getDisplayMetrics().widthPixels;
	
		mInstalledHeaderText = getResources().getString(R.string.kpt_UI_STRING_AMGR_1);
		mDownloadHeaderText = getResources().getString(R.string.kpt_UI_STRING_AMGR_2);
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		mCoreEngine = coreEngine;
		String txt = null;
	
			txt = null;
			if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false)){
				txt =  getString(R.string.kpt_pd_add_on_install_progress);
			} else if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false)) {
				txt =  getString(R.string.kpt_pd_add_on_uninstall_progress);
			} else {
				txt =  getString(R.string.kpt_notofication_addon_downloading);
			}
			mProgressDialog = ProgressDialog.show(this, getResources().getText(R.string.kpt_title_wait), txt, true, false);
		Thread thread = new Thread(this);
		thread.start();
	}


	@Override
	protected void onResume() {
		super.onResume();
		mSearchSelected = false;
		setContentView(R.layout.kpt_addon_download_layout);
		setActivityUI();
		mSharedPref.registerOnSharedPreferenceChangeListener(this);


		if(mCoreServiceHandler == null) {
			mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
			mCoreEngine = mCoreServiceHandler.getCoreInterface();
			if(mCoreEngine != null) {
				initializeCore();
			}
		} 

		if(mCoreEngine == null) {
			mCoreServiceHandler.registerCallback(this);
		} 
		else {

			String txt = null;
			if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false)){
				txt =  getString(R.string.kpt_pd_add_on_install_progress);
			} else if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false)) {
				txt =  getString(R.string.kpt_pd_add_on_uninstall_progress);
			} else {
				txt =  getString(R.string.kpt_notofication_addon_downloading);
			}
			mProgressDialog = ProgressDialog.show(this, getResources().getText(R.string.kpt_title_wait), txt, true, false);
			Thread thread = new Thread(this);
			thread.start();
		}

	}

	/**
	 * Initializes core engine framework. If it fails for the first time, it destroys the core
	 * object and then retries again. IF it fails even after destroying core, then the internal files 
	 * are deleted and retried again.
	 * 
	 */
	private void initializeCore() {
		isCoreEngineInitialized = mCoreEngine.initializeCore(this);
		if(!isCoreEngineInitialized) {
			// Core Engine not initialized destroy the core object and retry again
			mCoreEngine.forceDestroyCore();
			isCoreEngineInitialized =  mCoreEngine.initializeCore(this);
			//Even after retry initialization failed, then clear the internal files and retry again
			if(!isCoreEngineInitialized) {
				
				try {
					String filePath = getFilesDir().getAbsolutePath()+"/Profile";
					File file = new File(filePath);
					recusriveDelete(file);
					file.delete();

					mCoreEngine.prepareCoreFilesForRetry(getFilesDir().getAbsolutePath(), getAssets());
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}catch (Exception e) {
					//e.printStackTrace();
				}
				
				isCoreEngineInitialized =  mCoreEngine.initializeCore(this);
				//After all possiblie retries core engine still not initialized, which is a rare use case
				if(!isCoreEngineInitialized) {
					Log.e("MS","ADDON DOWNLOAD ACTIVITY ***** CORE INITIALIZATION FAILED EVEN AFTER 3 ATTEMPTS, FORCE DESTROY CORE AND" +
							"AFTER CLEARING INTERNAL FILES ****** ");
				}
			}
		}
	}

	public void recusriveDelete(File file) {
		File[] files = file.listFiles();
		for (File f : files) {
			if(f.isDirectory()) {
				if(f.listFiles().length != 0) {
					recusriveDelete(f);
					f.delete();
				} else {
					boolean status = f.delete();
					//Log.e("kpt","deleting folder "+f.getName()+" is "+status);
				}
			} else {
				boolean status = f.delete();
				//Log.e("kpt","deleting file "+f.getName()+" is "+status);
			}
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if(mSearchSelected) {
				mSearchSelected = false;
				
				mSearchEdit.setVisibility(View.GONE);
				mSearchListView.setVisibility(View.GONE);
				//mSearchListView.setAdapter(mSearchAdapter);
				
				findViewById(R.id.addon_downlaod_title_bar).setVisibility(View.VISIBLE);
				mLanguageListView.setVisibility(View.VISIBLE);
				mTitleHandler.sendEmptyMessageDelayed(FIRST_SCREEN, 0);
				
				return true;
			} /*else {
				 return super.onKeyDown(keyCode, event);
			}*/
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onPause() {
		if(mProgressDialog !=null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		super.onPause();
	}

	/**
	 *   Set the UI for both sub views (Language and Technical Addons which are to be downloaded from server and installed) 
	 * 	
	 */
	private void setActivityUI() {
		mParentHorizontalScroll = (HorizontalScrollView) findViewById(R.id.hsv);
		mParentHorizontalScroll.setSmoothScrollingEnabled(true);
		mParentHorizontalScroll.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch(event.getAction()){
				case MotionEvent.ACTION_MOVE:
					if(mDownX == -1){
						mDownX = event.getX();
					}
					break;
				case MotionEvent.ACTION_UP:
					if(mDownX > event.getX()) {
						isRightToLeft = true; 
					}else {
						isRightToLeft = false;
					}
					if(Math.abs((event.getX() - mDownX)) > (mScreenWidth * 0.3)){
						onScreenChanged(true);
					}else{
						mTitleHandler.sendEmptyMessageDelayed(mCurrentScreen, 0);
					}
					mDownX = -1;
					break;
				}
				return false;
			}
		});

		mSearchListView = (ListView) findViewById(R.id.SearchListView);
		mSearchListView.setOnItemClickListener(mSearchItemClickListener);
		LayoutParams params3 = mSearchListView.getLayoutParams();
		params3.width = getResources().getDisplayMetrics().widthPixels;
		mSearchListView.setLayoutParams(params3);
		mSearchListView.setVisibility(View.GONE);
		
		mLanguageListView = (ListView) findViewById(R.id.download_left_list);
		
		
		
		mLanguageListView.setOnItemClickListener(mListItemClickListener);		
		LayoutParams params = mLanguageListView.getLayoutParams();
		params.width = getResources().getDisplayMetrics().widthPixels;
		mLanguageListView.setLayoutParams(params);

		
		mSearchEdit = (EditText)  findViewById(R.id.addons_search);
		mSearchEdit.requestFocus();
		mSearchEdit.setVisibility(View.GONE);
		mSearchEdit.setInputType(InputType.TYPE_CLASS_TEXT  | InputType.TYPE_TEXT_VARIATION_FILTER );
		mSearchEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mSearchEdit.addTextChangedListener(this);
		
		mLeftHeader = (TextView) findViewById(R.id.download_title_left);
		//mRightHeader = (TextView) findViewById(R.id.download_title_center);
		
		mLeftHeader.setText(getString(R.string.kpt_UI_STRING_AMGR_7));
		//mRightHeader.setText(getString(R.string.kpt_UI_STRING_AMGR_8));
		
		mLeftHeader.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mParentHorizontalScroll.post(new Runnable() {
					public void run() {
						mCurrentScreen = FIRST_SCREEN;
						mTitleHandler.sendEmptyMessageDelayed(mCurrentScreen, 100);
					}
				});
			}
		});

	

		
		 languagesAdapter = new TapToDownloadAddonAdapter(AddonDownloadActivity.this, true);
		 searchAdapter = new TapToDownloadAddonAdapter(AddonDownloadActivity.this, false);
		
		
		mDownloadedLanguageAddonAdapter = new DownloadedAddonAdapter(this);
		
		mLanguagesAdapter = new KPTMergeAdapter();
		mSearchAdapter = new KPTMergeAdapter();
		
		
		mLanguagesAdapter.addAdapter(mDownloadedLanguageAddonAdapter);
		mLanguagesAdapter.addAdapter(languagesAdapter);
		
		mSearchAdapter.addAdapter(searchAdapter);
		
		//fillAdapter(-1);
		
		/*mLanguageListView.setAdapter(mLanguagesAdapter);
		mTechnincalListView.setAdapter(mTechnicalsAdapter);*/
		
		
	}
	

	public void onScreenChanged(boolean hasFocus) {/*
		mParentHorizontalScroll.post(new Runnable() {
			public void run() {
				int currentScrollPosition = mParentHorizontalScroll.getScrollX();
			
				if(isRightToLeft){
					if(currentScrollPosition > 0 && currentScrollPosition <= mScreenWidth){
						mCurrentScreen = SECOND_SCREEN;
					}else if(currentScrollPosition > mScreenWidth && currentScrollPosition < (2 * mScreenWidth)){
						mParentHorizontalScroll.smoothScrollTo(2 * mScreenWidth, 0);
						mCurrentScreen = SECOND_SCREEN;
					}
				}else{
					if(currentScrollPosition >= mScreenWidth && currentScrollPosition <= (2 * mScreenWidth)){
						mCurrentScreen = SECOND_SCREEN;
					}else if(currentScrollPosition > (2 * mScreenWidth) && currentScrollPosition < (3 * mScreenWidth)){
						mCurrentScreen = FIRST_SCREEN;
					}else if(currentScrollPosition  >= 0  && currentScrollPosition < mScreenWidth){ 
						mCurrentScreen = FIRST_SCREEN;
					}
				}
				mTitleHandler.sendEmptyMessageDelayed(mCurrentScreen, 0);
			}
		});
	*/}
	
	
	public void performSearch(){
		mSearchSelected = true;
		mCurrentSearchList.clear();


		mCurrentSearchList.addAll(mLanguageAddonList);
		//mCurrentSearchList.addAll(mTechnicalAddonList);
		int size = mCurrentSearchList.size();
		for(int i = 0; i<size;i++){

			if(mCurrentSearchList.contains(mDownloadHeaderText)){
				mCurrentSearchList.remove(mDownloadHeaderText);
			}
			if(mCurrentSearchList.contains(mInstalledHeaderText)){
				mCurrentSearchList.remove(mInstalledHeaderText);
			}
		}


		mTempList.clear();
		mTempList.addAll(mLanguageAddonList);

		int length = mTempList.size();

		for(int i = 0; i<length;i++){
			if(mTempList.contains(mDownloadHeaderText)){
				mTempList.remove(mDownloadHeaderText);
			}
			if(mTempList.contains(mInstalledHeaderText)){
				mTempList.remove(mInstalledHeaderText);
			}
		}
		mSearchEdit.setVisibility(View.VISIBLE);
		mSearchEdit.requestFocus();
		mSearchEdit.setText("");
		mSearchListView.setVisibility(View.VISIBLE);
		mSearchListView.setAdapter(mSearchAdapter);

		findViewById(R.id.addon_downlaod_title_bar).setVisibility(View.GONE);
		mLanguageListView.setVisibility(View.GONE);
	}
	
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Message msg = new Message();
		msg.obj = key;
		mHandler.sendMessage(msg);
	}
	
	
	// Handler used to change the language and technical titles accordingly
	final Handler mTitleHandler = new Handler(){
		
		

		public void handleMessage(Message msg) {
	
			switch (msg.what) {
			
			case FIRST_SCREEN:
				mDisableMenuItem = false;
				mParentHorizontalScroll.post(new Runnable() {
					public void run() {
						mParentHorizontalScroll.smoothScrollTo(0, 0);
					}
				});
				mLeftHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.kpt_tab_selected));
			//	mRightHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_unselected));
				break;
			/*case SECOND_SCREEN:
				mDisableMenuItem = false;
				mParentHorizontalScroll.post(new Runnable() {
					public void run() {
						mParentHorizontalScroll.smoothScrollTo(mScreenWidth, 0);
					}
				});
				mLeftHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_unselected));
			//	mRightHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_selected));
				break;*/
			
			}
		};
	};

	final Handler mHandler = new Handler(){


		@Override
		public void handleMessage(Message msg) {
			String key = (String)msg.obj;

			//SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(AddonDownloadActivity.this);
			//Log.e("MS", "OnSharedPrefChange Key-->"+key);
			if(key.equals(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS)){
				boolean installStatus = mSharedPref.getBoolean(key, false) ;
				//Log.e("MS", "In Install-->"+installStatus);
				if(installStatus) { // Installation of addon into core must have started
					mFocusable = false;
				} else {//  Installation of addon into core is done
					mDisableMenuItem = true;
					if(mCoreEngine!=null) {
						try {
							mInstalledDict = mCoreEngine.getInstalledDictionaries();
							if(mInstalledDict != null) {
								for(KPTParamDictionary installedDict:mInstalledDict) {
									mInstallingList.remove(installedDict.getDictFileName());
									for(String displayName:mLanguageAddonList) {
										if(displayName.equalsIgnoreCase(installedDict.getDictDisplayName())) {
											mLanguageAddonList.remove(displayName);
											break;
										}
									}
								
								}
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			if(key.equals(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE)) {
				if(!mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false)
						&& !mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false)) {
					mDisableMenuItem = false;
					parseXML();
					mFocusable = true;
					Log.e("MS","No Package In Queue");
					fillAdapter(-1);
					
					if(mProgressDialog != null && mProgressDialog.isShowing()) {
						mProgressDialog.dismiss();
					}
					
					if(mAddonUpdateFromIMEDialog != null && mAddonUpdateFromIMEDialog.isShowing()) {
						mAddonUpdateFromIMEDialog.dismiss();
					}
					
					mLanguageListView.setAdapter(mLanguagesAdapter);
					//mTechnincalListView.setAdapter(mTechnicalsAdapter);
					
					mDownloadedLanguageAddonAdapter.notifyDataSetChanged();
					//mDownloadedTechnicalAddonAdapter.notifyDataSetChanged();
					
					//mSharedPref.edit().putBoolean(KPTConstants.PREF_OLD_ADDONS_UPGRADE, false).commit();
				}
			} 
			
			
		}};
		
		@Override
		public void run() {

			//Log.e("MS", "MAIN RUN");
			if(mCoreEngine != null) {
				initializeCore();				
			}	
			copyXmlFromAsset(false);

			
			//Log.e("MS", "commited installed dict from databse");
			boolean installInProgress = mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
			boolean installInQueue = mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false);
			if(!installInProgress && !installInQueue && mDownloadAddOnTasksMap.size() == 0){
			
				parseXML();
				UIHandler.sendEmptyMessage(0);
			}
		}

		 Handler UIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				fillAdapter(-1);
				
				//Bugtracker 254
				mLanguageListView.setAdapter(mLanguagesAdapter);
				//mTechnincalListView.setAdapter(mTechnicalsAdapter);
				
				if(mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				
				if( mAddonUpdateFromIMEDialog != null && mAddonUpdateFromIMEDialog.isShowing()) {
					mAddonUpdateFromIMEDialog.dismiss();
				}
			}
		};

	
		

		private synchronized void fillAdapter(final int currentFocusedListItem) {

			if(mCoreEngine!=null) {
				mInstalledDict = mCoreEngine.getInstalledDictionaries();
			}
			mInstalledLanguageAddonList.clear();
			//mInstalledTechnicalAddonList.clear();
			if(mInstalledDict!=null) {
				for(int i=0;i<mInstalledDict.length;i++) {
					//Log.e("MS", "FillAdapter() AddonDownloadActivity InstalledDict-->"+installedDict.getDictDisplayName());
					//mTapToDownloadAddonList.remove(installedDict.getDictDisplayName());
					mLanguageAddonList.remove(mInstalledDict[i].getDictDisplayName());
				//	mTechnicalAddonList.remove(mInstalledDict[i].getDictDisplayName());
					/**
					 * this is a safety check to make sure that already installed addons are
					 * removed from installing list
					 */
					mInstallingList.remove(mInstalledDict[i].getDictFileName());
					String displayname = mInstalledDict[i].getDictDisplayName();
				
						mInstalledLanguageAddonList.add(mInstalledDict[i].getDictDisplayName());
					//Log.e("MS","FillAdapter DisplayName-->"+mInstalledDict[i].getDictDisplayName());
					if(i>MAX_NO_DICTS_LOADED && mInstalledDict[i].isDictLoaded()) {
						mCoreEngine.unloadDictionary(mInstalledDict[i].getComponentId());
						mInstalledDict[i].setDictState(mInstalledDict[i].getFieldMaskState(),
								mInstalledDict[i].getComponentId(), false, 
								mInstalledDict[i].getDictPriority(), false,
								mInstalledDict[i].getLicenseState().getLicenseStateInt());
					}
				}
				if(mInstalledDict.length >0){
					if(mInstalledLanguageAddonList!= null && mInstalledLanguageAddonList.size() >0){
						mInstalledLanguageAddonList.add(0,mInstalledHeaderText);
					}
				
				}
				
			}

			
			Set<String> ht = new LinkedHashSet<String>(mLanguageAddonList);
			mLanguageAddonList.clear();
			mLanguageAddonList.addAll(ht);
			if (mLanguageAddonList.contains(mDownloadHeaderText)) {
				mLanguageAddonList.remove(mDownloadHeaderText);
			}
		/*	if (mTechnicalAddonList.contains(mDownloadHeaderText)) {
				mTechnicalAddonList.remove(mDownloadHeaderText);
			}*/
			Collections.sort(mLanguageAddonList);

			/*Set<String> h = new LinkedHashSet<String>(mTechnicalAddonList);
			mTechnicalAddonList.clear();
			mTechnicalAddonList.addAll(h);
			Collections.sort(mTechnicalAddonList);*/
			
			mLanguageAddonList.add(0,mDownloadHeaderText);
			//mTechnicalAddonList.add(0,mDownloadHeaderText);

			if(currentFocusedListItem != CALLED_FROM_DOWNLOAD_ADDON_TASK){
				//if(mParentHorizontalScroll.getScrollX() <= mScreenWidth) {
					//mLanguageListView.setSelection(currentFocusedListItem);
					mLanguageListView.post(new Runnable() {
						@Override
						public void run() {
							mLanguageListView.setSelection(0);
							mLanguageListView.clearFocus();

						}
					});
			//	} else {
					//mTechnincalListView.setSelection(currentFocusedListItem);
					/*mTechnincalListView.post(new Runnable() {
						@Override
						public void run() {
							mTechnincalListView.setSelection(0);
							mTechnincalListView.clearFocus();
						}
					});
				}*/
			}
			
			  mLanguagesAdapter.notifyDataSetChanged();
	    	//  mTechnicalsAdapter.notifyDataSetChanged();

		}


		


	
		
		public void updateOnItemClickSuccess(){
			String displayName = viewHolder.mAddonDisplayTxtView.getText().toString();

			mSharedPref.edit().putInt(KPTConstants.PREF_DOWNLOAD_STARTED, DOWNLOAD_STARTED_FROM_DOWNLOAD_ACTIVTY).commit();
			viewHolder.mTapToDownloadTxtView.setVisibility(View.GONE);
			viewHolder.mTapToDownloadIcon.setVisibility(View.GONE);
			viewHolder.mProgressBar.setVisibility(View.VISIBLE);
			viewHolder.mAddonDownloadPerc.setVisibility(View.VISIBLE);
			viewHolder.mAddonCurrentStatusView.setVisibility(View.VISIBLE);
			viewHolder.mAddonCurrentStatusView.setText(getString(R.string.kpt_UI_STRING_INAPP_STATUS_1));
			viewHolder.mCancelDownloadIcon.setVisibility(View.VISIBLE);
			viewHolder.mDownloadAddOnTask = new DownloadAddOnTask(viewHolder.mProgressBar, displayName, false, viewHolder.mAddonDownloadPerc);
			mDownloadAddOnTasksMap.put(displayName, viewHolder.mDownloadAddOnTask);
			viewHolder.mDownloadAddOnTask.execute(displayName);

		}
		
		private OnItemClickListener mSearchItemClickListener = new  OnItemClickListener() {

			//@Override
			public void onItemClick(AdapterView<?> adapterView,  View view, int position,
					long id) {
			
					downloadItemListener(view);
				
			}
		};
		

		
		
		LangListViewHolder viewHolder ;
		private OnItemClickListener mListItemClickListener = new  OnItemClickListener() {

			//@Override
			public void onItemClick(AdapterView<?> adapterView,  View view, int position,
					long id) {

				if(mLanguagesAdapter.getAdapter(position) instanceof DownloadedAddonAdapter){

					if(position == 0 ||
							mSharedPref.getInt(KPTConstants.PREF_DOWNLOAD_STARTED, 0) == DOWNLOAD_STARTED_FROM_DOWNLOAD_ACTIVTY
							||(mInstallingList != null && mInstallingList.size()>0)){
						return;
					}

					final TextListViewHolder mSelectedAddonHolder = (TextListViewHolder)view.getTag();

					if(position != 0){
						if(mSelectedAddonHolder.mProgressBar.getVisibility() == View.VISIBLE){
							return;
						}
						// For Un-installing/Disable/Cancel the selected dictionary.
						clickOnItemClickListenerForDownloadedItems(mSelectedAddonHolder,position);
					}

					return;
				}else{

					//For downloading the selected dictionary.
					downloadItemListener(view);

				}
			} 

		};
		
//		private HashMap<String,String> mPriceList = new HashMap<String,String>();
		
		private boolean isTechnical = false;
		protected void downloadItemListener(View view) {
			// TODO Auto-generated method stub
			viewHolder = (LangListViewHolder)view.getTag();
			if(viewHolder.mProgressBar.getVisibility() == View.VISIBLE){
				return;
			}
			String titleText = viewHolder.mTitle.getText().toString();
			if (((mDownloadHeaderText).equals(titleText))){
				return;
			}
			
			String displayName = viewHolder.mAddonDisplayTxtView.getText().toString();
			mAddonDataMap.get(displayName);
			
			
			if (mAddonDataMap!= null && !mAddonDataMap.containsKey(displayName)){
				return;
			}
			
			
			AddonData addonData = mAddonDataMap.get(displayName);
		

			if(!doesAddonInstallLimitExceeded()) {
				if(mDownloadAddOnTasksMap.size() <= 1 ){
					if(checkNetworkConnection()) {
						
						
							updateOnItemClickSuccess();
					
						
					} else {
						Log.e(TAG, "jjjjjjjjjjjjjjjjjjjjjjjjjjj ");
						Toast.makeText(
								AddonDownloadActivity.this,
								getResources().getString(
										R.string.kpt_no_network_connectivity),
										Toast.LENGTH_SHORT).show();
					}
				}  else {
					Toast.makeText(
							AddonDownloadActivity.this,
							getResources().getString(R.string.kpt_UI_STRING_INAPP_TOAST_1), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(
						AddonDownloadActivity.this, getResources().getString(R.string.kpt_UI_STRING_INAPP_TOAST_2), Toast.LENGTH_SHORT).show();
			}
		}

	
		
		
		
		/**
		 * Activates the top priority dictionary KEYMAP
		 * Since it takes time on lower end devices, aprogress dialog was added
		 * Called for every check and uncheck 
		 * 
		 */
		private void activateTopPriorityDictionary() {
			final ProgressDialog mpd1  = ProgressDialog.show(AddonDownloadActivity.this, getResources().getString(R.string.kpt_title_wait), getResources().getString(R.string.kpt_enable_context), true, false);
			new Thread(new Runnable() {
				@Override
				public void run() {
					UIHandler.post(new Runnable() {
						public void run() {
							boolean activate = mCoreEngine.activateTopPriorityDictionaryKeymap();
							Log.e("MS", "ActivateTopPriorityDictionaryKeymap "+activate);
							if(activate){
								if(mpd1!=null && mpd1.isShowing()) {
									mpd1.dismiss();
								}
							}else{
								if(mpd1!=null && mpd1.isShowing()) {
									mpd1.dismiss();
								}
							}
						}
					});
				}}).start();
		}
		
	  protected void clickOnItemClickListenerForDownloadedItems(final TextListViewHolder mSelectedAddonHolder,int position) {
			// TODO Auto-generated method stub


			//If progress dialog is being shown it means that donwload or install is in progress so return
			if(mSelectedAddonHolder.mProgressBar.getVisibility()==View.VISIBLE){
				return;
			}
			
			final String displayName;
		
				displayName = mInstalledLanguageAddonList.get((int) mLanguagesAdapter.getItemId(position));
			
			if(mInstalledDict!=null) {

				mSelectedAddonHolder.mDisplayTxtView.toggle();
				boolean dictToBeLoaded = mSelectedAddonHolder.mDisplayTxtView.isChecked();
				for(int i=0;i<mInstalledDict.length;i++) {

					if(mInstalledDict[i].getDictDisplayName().equalsIgnoreCase(displayName)) {
						// Load this dictionary in Core based on certain paramaters
						if(dictToBeLoaded) {
							// IF this is the sixth addon that needs to be loaded then show a toast as only five can be loaded
							if(i > MAX_NO_DICTS_LOADED) {
								AlertDialog.Builder builder = new AlertDialog.Builder(AddonDownloadActivity.this);
								builder.setTitle(getString(R.string.kpt_UI_STRING_ATRIM_ITEM1));
								builder.setMessage(getString(R.string.kpt_UI_STRING_INAPP_DIALOG_MESSAGE_1));
								builder.create().show();
							} else {
								
									//holder.mText2.setText(getResources().getText(R.string.kpt_tap_disable));
									mCoreEngine.loadDictionary(mInstalledDict[i].getComponentId());
									mInstalledDict[i].setDictState(mInstalledDict[i].getFieldMaskState(),
											mInstalledDict[i].getComponentId(), true, 
											mInstalledDict[i].getDictPriority(), true,
											mInstalledDict[i].getLicenseState().getLicenseStateInt());
									if(mCoreEngine!=null && mCoreEngine.getLoadedComponents()!=null
											&& mCoreEngine.getLoadedComponents().length>2 ) {
										// fix for TP 7148
										//Log.e("MS",	"IS The TopPriorityDict so show PD");
										activateTopPriorityDictionary();
									}

									if(mLanguagesAdapter != null) {
										mLanguagesAdapter.notifyDataSetChanged();
									}
									
									
								
							}	
						} else {
							// Un- Load this dictionary in Core based on certain paramaters
							final int index = i;
							KPTParamComponentInfo[] loadedComponents = mCoreEngine
									.getLoadedComponents();
							if (loadedComponents != null
									&& loadedComponents.length == 2) {
							
									mSelectedAddonHolder.mDisplayTxtView.setChecked(true);
									Toast.makeText(AddonDownloadActivity.this, getString(R.string.kpt_addon_atleast_one),
											Toast.LENGTH_SHORT).show();
								
							} else {
								
									AlertDialog.Builder builder = new AlertDialog.Builder(AddonDownloadActivity.this);
									builder.setMessage(getString(R.string.kpt_UI_STRING_AMGR_10));

									builder.setPositiveButton(getString(R.string.kpt_UI_STRING_AMGR_11),
											new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											mProgressDialog = ProgressDialog.show(AddonDownloadActivity.this, getResources().getText(R.string.kpt_title_wait), 
													getString(R.string.kpt_pd_add_on_uninstall_progress), true, false);

											//String dispName = mSelectedAddonHolder.mDisplayTxtView.getText().toString();

											//Log.e("MS", "Dict file name = " + displayName);
											AddonData data = mAddonDataMap.get(displayName);
											String fileName = data.getFileName();
											AddonData addonData = null;
											if(data != null) {
												addonData = new AddonData();
												addonData.setFileName(data.getFileName());
												addonData.setAddonId(data.getAddonId());
												addonData.setDisplayName(data.getDisplayName());
											} 
											Intent intent = new Intent(AddonDownloadActivity.this, KPTPkgHandlerService.class);
											intent.setAction(KPTConstants.KPT_ACTION_PACKAGE_UNINSTALL);
											intent.putExtra(KPTConstants.INTENT_EXTRA_FILE_NAME, fileName);
											intent.setFlags(Intent.FLAG_FROM_BACKGROUND);
											startService(intent);
										}
									});

									builder.setNeutralButton(getString(R.string.kpt_UI_STRING_AMGR_4_DISABLE),
											new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											mCoreEngine.unloadDictionary(mInstalledDict[index].getComponentId());
											mInstalledDict[index].setDictState(mInstalledDict[index].getFieldMaskState(),
													mInstalledDict[index].getComponentId(), false, 
													mInstalledDict[index].getDictPriority(), false,
													mInstalledDict[index].getLicenseState().getLicenseStateInt());

											/*if(mCoreEngine!=null && mCoreEngine.getLoadedComponents()!=null
														&& mCoreEngine.getLoadedComponents().length>2 ) {*/
											// fix for TP 7148
											//Log.e("MS",	"Dialog IS The TopPriorityDict so show PD");
											activateTopPriorityDictionary();
											//}


											mLanguagesAdapter.notifyDataSetChanged();
											//mTechnicalsAdapter.notifyDataSetChanged();
											//populatePrioritizeAddonView();
										}
									});

									builder.setNegativeButton(getString(R.string.kpt_alert_dialog_cancel),
											new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface arg0,
												int arg1) {
											mSelectedAddonHolder.mDisplayTxtView.toggle(); //Bug tracker 299
											arg0.dismiss();
										}
									});

									builder.create().show(); 
								

							}
						}
						break;
					}
				}
				//}

				// If this is the last dicitionary that was unchecked then toggle or check and 
				KPTParamComponentInfo[] loadedComponents = mCoreEngine.getLoadedComponents();
				if(loadedComponents != null &&
						loadedComponents.length==1) {
					mSelectedAddonHolder.mDisplayTxtView.toggle();
					Toast.makeText(AddonDownloadActivity.this, getResources().getText(R.string.kpt_addon_atleast_one), Toast.LENGTH_SHORT).show();
					for(int i=0;i<mInstalledDict.length;i++) {
						if(mInstalledDict[i].getDictDisplayName().equalsIgnoreCase(displayName)) {
							mCoreEngine.loadDictionary(mInstalledDict[i].getComponentId());
							mInstalledDict[i].setDictState(mInstalledDict[i].getFieldMaskState(),
									mInstalledDict[i].getComponentId(), true, 
									mInstalledDict[i].getDictPriority(), true,
									mInstalledDict[i].getLicenseState().getLicenseStateInt());
							break;
						}
					}
				}
			}

		
		}

	protected void launchMarket() {
		  PackageManager pm=getPackageManager();
			Intent intent = new Intent (Intent.ACTION_VIEW);
			intent.setData (Uri.parse (KPTConstants.MARKET_SEARCH_WORD));
			try {
				pm.getPackageInfo("com.android.vending", PackageManager.GET_UNINSTALLED_PACKAGES);
				startActivity (intent);
			} catch (NameNotFoundException e) {
				Toast.makeText(AddonDownloadActivity.this, getResources().getText(R.string.kpt_android_market_app_unav), Toast.LENGTH_SHORT).show();
			}
			
		}

		private boolean doesAddonInstallLimitExceeded() {
			KPTParamDictionary[] installed = mCoreEngine.getInstalledDictionaries();
			int installedInCore = 0;
			if(installed != null) {
				installedInCore = installed.length;
			}
			
			int count = installedInCore + mDownloadAddOnTasksMap.size() +
			mInstallingList.size();
			if(count < 5) {
				return false;
			}

			return true;
		}

		
		/**
		 * Converts strings like 0.7.3, 1.0.2 to 0.73 & 1.02 that can be used as float
		 */
		private String getFloatValue(String version) {
			int startIndex = version.indexOf(".");
			int lastIndex = version.lastIndexOf(".");
			if(startIndex != lastIndex) {
				version = version.substring(0, lastIndex) + version.substring(lastIndex + 1);
			}
			return version;
		}
		

		

		private void copyXmlFromAsset(boolean forceCopy) {
			File myFile = new File(getFilesDir().getAbsolutePath()+"/" + AddonDownloadActivity.S3_COMPATABILITY_XML);
			if(myFile.exists() && ! forceCopy) {
				//////KPTLog.e("Addon XML Parser", "--------->> Addon XML in Internal Storage " + myFile.exists());    
				return;
			}
			try {
				if(forceCopy){
					myFile.delete();
				}
				//////KPTLog.e("Addon XML Parser", "COPY XML ASSET");
				InputStream isd = getApplicationContext().getAssets().open(KPTConstants.ATX_ASSETS_FOLDER+AddonDownloadActivity.S3_COMPATABILITY_XML);
				//myFile = new File(getFilesDir().getAbsolutePath()+"/" + "addon.xml");
				OutputStream os = new FileOutputStream(myFile); 
				byte[] b = new byte[1024]; 
				int length;
				while ((length = isd.read(b))>0) { os.write(b,0,length);}
				os.flush();
				isd.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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

		private void parseXML() {
			InputStream is = null;
			XmlPullParser parser = null;
			File myFile = new File(getFilesDir().getAbsolutePath() + "/" + AddonDownloadActivity.S3_COMPATABILITY_XML);
			try {

				is = new FileInputStream(myFile);
				parser = new KPTKXmlParser();
				parser.setInput(is, null);
				String displayname = null;
				String filename = null;
				String zipFilename = null;
				String addonId = null;
				AddonData addonData = null;
				boolean done = false;
				
				String type = null;

				LinkedHashMap <String, String[]> compatibiltiyInfoMap = null;


				try {
					int eventType = parser.getEventType();
					do {
						switch (eventType) {
						case XmlPullParser.START_TAG:
							if(parser.getName().equals(XML_BASE_URL)) {
								BASE_URL = parser.nextText();
							}else if (parser.getName().equals(XML_ADDON_TAG)) {
								addonData = new AddonData();
								addonId = parser.getAttributeValue(null, "id");
								addonData.setAddonId(addonId);
								compatibiltiyInfoMap = new LinkedHashMap<String, String[]>();
							} else if (parser.getName().equals(XML_DICT_DISPLAY_NAME)) {
								displayname = parser.nextText();
								//Log.e("TAG-MS", "************************************************");
								//Log.e("TAG-MS", "DisplayName "+displayname);
								addonData.setDisplayName(displayname);
								//mTapToDownloadAddonList.add(displayname);
							} else if (parser.getName().equals(XML_DICT_FILE_NAME)) {
								filename = parser.nextText();
								//Log.e("TAG-MS", "FileName "+ filename);
								addonData.setFileName(filename.toLowerCase());
								//mNameMap.put(filename.toLowerCase(), displayname);
							} else if(parser.getName().equals(XML_ZIP_FILENAME)){
								zipFilename = parser.nextText();
								//Log.e("TAG-MS", "zipFilename "+ zipFilename);
								addonData.setZipFileName(zipFilename);
							} else if (parser.getName().equals(XML_SEARCH)) {
								String search_word = parser.nextText();
								mAddonSearchTagsMap.put(displayname,search_word.split(":"));
							}else if (parser.getName().equals(XML_TYPE)) {
								type = parser.nextText();
								addonData.setType(type);
								mLanguageAddonList.add(displayname);
								
							}/* else if(parser.getName().equals(XML_PRICE)){
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
								/*
								 * //KPTLog.e("TAG-MS", "Disp Name-->" + displayname
								 * +"--fileName-->"+filename
								 * +"--compact-->"+cmpt+"--flagname-->"
								 * +flagName+"--shortname-->"+shortname);
								 */
								mAddonDataMap.put(displayname, addonData);
								addonData = null;
								displayname = null;
								filename = null;
								type = null;
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
			} finally {
				if (is != null) {
					try {
						is.close();
						// //KPTLog.e("MTAPTODOWNLOADLIST SIZE",
						// mTapToDownloadAddonList.size()+"");
					} catch (IOException e) {
					}
				}
			}
		}
		

		
		
		class DownloadedAddonAdapter extends BaseAdapter {
			private LayoutInflater mInflater;
			TypedArray typedArray = null;
//			private ArrayList<String> installedAddonList;

			public DownloadedAddonAdapter(Context context) {
				// Cache the LayoutInflate to avoid asking for a new one each time.
				mInflater = LayoutInflater.from(context);
			}

			public int getCount() {
				if(mInstalledLanguageAddonList != null) {
					return mInstalledLanguageAddonList.size();
				} else {
					return 0;
				}
			}

			public Object getItem(int position) {
				return position;
			}

			@Override
			public boolean isEnabled(int position) {
				if(mFocusable) {
					return true;
				} else {
					return false;
				}
			}

			public long getItemId(int position) {
				return position;
			}

			/**
			 * Make a view to hold each row.
			 *
			 * @see android.widget.ListAdapter#getView(int, android.view.View,
			 *      android.view.ViewGroup)
			 */
			public View getView(int position, View convertView, ViewGroup parent) {
				// A ViewHolder keeps references to children views to avoid unneccessary calls
				// to findViewById() on each row.
				TextListViewHolder holder = new TextListViewHolder();
//				Log.e("KPT","DownloadedAddonAdapter--->>>"+ position);
				// When convertView is not null, we can reuse it directly, there is no need
				// to reinflate it. We only inflate a new View when the convertView supplied
				// by ListView is null.
				if (convertView == null) {
					
					convertView = mInflater.inflate(R.layout.kpt_list_item_downloaded_icon, null);

					// Creates a ViewHolder and store references to the two children views
					// we want to bind data to.
					holder.mDisplayTxtView = (CheckedTextView) convertView.findViewById(R.id.chkTxt);    
					holder.mAddonStatusTxtView = (TextView)convertView.findViewById(R.id.subtext_downloaded);
					holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.addon_progressbar);
					holder.mHeader = (TextView)convertView.findViewById(R.id.list_header_title);
					convertView.setTag(holder);
				} else {
					// Get the ViewHolder back to get fast access to the TextView
					// and the ImageView.
						holder = (TextListViewHolder) convertView.getTag();
				}
				
				String displayName = mInstalledLanguageAddonList.get(position);
				convertView.setBackgroundColor(Color.TRANSPARENT);
				holder.mHeader.setVisibility(View.GONE);
				holder.mDisplayTxtView.setVisibility(View.VISIBLE);
				 // Cache the check box drawable.
		        typedArray = getApplicationContext().getTheme().obtainStyledAttributes(new int[] {android.R.attr.listChoiceIndicatorMultiple});

		        if ((typedArray != null) && (typedArray.length() > 0)) {
		        	holder.mDisplayTxtView.setCheckMarkDrawable(typedArray.getDrawable(0));
		        }
				// Bind the data efficiently with the holder.
				if(mInstalledDict!=null &&  mAddonDataMap.containsKey(displayName)) {
				//	boolean istechnical= mAddonDataMap.get(displayName).mType.equals("Technical");
					
						holder.mDisplayTxtView.setText(displayName);
						TreeMap<Integer, Integer> priorMap = new TreeMap<Integer, Integer>();
						for(int i=0;i<mInstalledDict.length;i++) {
							if(mInstalledDict[i].isDictLoaded()) {
								priorMap.put(mInstalledDict[i].getDictPriority(), i);
							}
						}
						for(int i=0;i<mInstalledDict.length;i++) {
							if(mInstalledDict[i].getDictDisplayName().equalsIgnoreCase(mInstalledLanguageAddonList.get(position))) {
								holder.mDisplayTxtView.setChecked(mInstalledDict[i].isDictLoaded());
								if(mInstalledDict[i].isDictLoaded()) {
									if(priorMap.get(priorMap.firstKey())==i) {
										holder.mAddonStatusTxtView.setText(getResources().getText(R.string.kpt_default_keyboard_txt));
										holder.mAddonStatusTxtView.setTextColor(Color.YELLOW);
									} else  {
										holder.mAddonStatusTxtView.setText(getResources().getText(R.string.kpt_tap_disable));
										holder.mAddonStatusTxtView.setTextColor(Color.WHITE);
									}
								} else {
									holder.mAddonStatusTxtView.setText(getResources().getText(R.string.kpt_tap_enable));
									holder.mAddonStatusTxtView.setTextColor(Color.WHITE);
								}
								break;
							}
						}
				}
			
				if(mAddonDataMap.containsKey(displayName)){/*

					//	Log.e("MS","Installed Activity : displayName = " + displayName +"Addon map size " + mAddonDataMap.size());
					AddonData addonInfo = mAddonDataMap.get(displayName);
					String fileName = mAddonDataMap.get(displayName).getFileName();
					//	Log.e("MS", "fileName-->"+fileName);
					String latestDictVersion = addonInfo.getLatestCompatibleAddonVersion(getString(R.string.version_number));
					String installedDictVersion = sPrefs.getString(fileName, null);
					if(installedDictVersion == null) {
						installedDictVersion = latestDictVersion;
					}
					// check if the installed version is compatible with the current base
					holder.isUpdateAvailable = false;
					holder.isCompatible = true;
					//Log.e("MS", "Checking compatibility between base and addOn-->"+fileName+" displayName-->"+displayName+ "  whose versionNo is-->"+installedDictVersion);
					if(addonInfo.isCompatible(getString(R.string.version_number), installedDictVersion)) {
						latestDictVersion = getFloatValue(latestDictVersion);
						installedDictVersion = getFloatValue(installedDictVersion);
						//If the installed addon version is less than addon version on server, update is available
						if(Float.parseFloat(installedDictVersion)<Float.parseFloat(latestDictVersion)) {
							holder.isUpdateAvailable = true;
							holder.mAddonStatusTxtView.setText(getString(R.string.kpt_UI_STRING_INAPP_SUBTEXT_1));
							if(!updateAvailableHolders.containsKey(displayName)){
								updateAvailableHolders.put(displayName,holder);
							}
						} 
					} else {// The addon installed in core is not compatible with the current base
						// show addon incompatibel and disable this view
						holder.isCompatible = false;
						holder.mAddonStatusTxtView.setText(getString(R.string.kpt_UI_STRING_INAPP_SUBTEXT_2));
						//String latestDictVersion = addonInfo.getLatestCompatibleAddonVersion(getString(R.string.version_number));
						latestDictVersion = getFloatValue(latestDictVersion);
						installedDictVersion = getFloatValue(installedDictVersion);
						if(Float.parseFloat(installedDictVersion)<Float.parseFloat(latestDictVersion)&&
								addonInfo.isCompatible(getString(R.string.version_number), latestDictVersion)) {
							holder.isCompatible = true;
							holder.isUpdateAvailable = true;
							holder.mAddonStatusTxtView.setText(getString(R.string.kpt_UI_STRING_INAPP_SUBTEXT_3));
						} 

					}
					//If its already installed and its not an update thats being downloaded pls remove from list.(SAFETY CHECK)
					//Log.e("MS", "Remove from list downloaded adapter");

					//Log.e("MS","Installed activity file name-->" + fileName);
					

					

					String unloadedAddons = mSharedPref.getString(AddonDownloadActivity.PREF_UNLOADED_ADDONS, "");
					if(unloadedAddons.contains(fileName)){
						holder.isExceededAddon = true;
					} else {
						holder.isExceededAddon = false;
					}
					
				
					
				*/} else {
					holder.mDisplayTxtView.setCheckMarkDrawable(null);
					holder.mAddonStatusTxtView.setVisibility(View.GONE);
					holder.mProgressBar.setVisibility(View.GONE);
					holder.mHeader.setVisibility(View.VISIBLE);
					holder.mHeader.setText(mInstalledHeaderText);
					holder.mDisplayTxtView.setVisibility(View.GONE);
				}

				return convertView;
			}

		}
		
		class TextListViewHolder {
			TextView mHeader;
			TextView mAddonCurrentStatusView;
			CheckedTextView mDisplayTxtView;
			TextView mAddonStatusTxtView;
			//boolean isUpdateAvailable = false;
			//boolean isCompatible = true;
			boolean isExceededAddon = false;
			ProgressBar mProgressBar;
		}
		
		
	
		
		
		

		private class TapToDownloadAddonAdapter extends BaseAdapter {
			private LayoutInflater mInflater;
			
			private Bitmap mDownloadIcon;
			
			LangListViewHolder holder = new LangListViewHolder();
			private boolean mIsLanguagesList;

			public TapToDownloadAddonAdapter(Context context, boolean isLanguagesList) {
				// Cache the LayoutInflate to avoid asking for a new one each time.
				mInflater = LayoutInflater.from(context);
				mIsLanguagesList = isLanguagesList;
				
				
				// Icons bound to the rows.
				mDownloadIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.kpt_taptodownload);
			}

			public int getCount() {
				
				if(mSearchSelected) {
					return mCurrentSearchList.size();
				} else {
					return mLanguageAddonList.size();
				}
				
				//return mTapToDownloadAddonList.size();
			}

			public Object getItem(int position) {
				return position;
			}
			public long getItemId(int position) {
				return position;
			}
			@Override
			public boolean isEnabled(int position) {
				if(mFocusable) {
					return true;
				} else {
					return false;
				}
			}

			
			/**
			 * Make a view to hold each row.
			 *
			 * @see android.widget.ListAdapter#getView(int, android.view.View,
			 *      android.view.ViewGroup)
			 */
			public View getView(int position, View convertView, ViewGroup parent) {
				// A ViewHolder keeps references to children views to avoid unneccessary calls
				// to findViewById() on each row.

				// When convertView is not null, we can reuse it directly, there is no need
				// to reinflate it. We only inflate a new View when the convertView supplied
				// by ListView is null.
				if (convertView == null) {
						convertView = (RelativeLayout) mInflater.inflate(R.layout.kpt_list_item_icon_text, null);
						// Creates a ViewHolder and store references to the two children views
						// we want to bind data to.
						holder = new LangListViewHolder();
						
						holder.mAddonDisplayTxtView = (TextView) convertView.findViewById(R.id.text);     
						holder.mTapToDownloadTxtView = (TextView) convertView.findViewById(R.id.text0);    
						holder.mAddonCurrentStatusView = (TextView) convertView.findViewById(R.id.text2);
						holder.mAddonDownloadPerc = (TextView) convertView.findViewById(R.id.downlaod_percentage);
						holder.mTapToDownloadIcon = (ImageView) convertView.findViewById(R.id.icon);
						holder.parentRelative = (RelativeLayout) convertView.findViewById(R.id.list_item_icon_relativelayout); 
						holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.addon_progressbar);
						holder.mCancelDownloadIcon = (ImageView) convertView.findViewById(R.id.icon_download_cancel);
						holder.mTitle = (TextView)convertView.findViewById(R.id.list_header_title1);
						convertView.setTag(holder);
					//}
				} else {
					// Get the ViewHolder back to get fast access to the TextView
					// and the ImageView.
					holder = (LangListViewHolder) convertView.getTag();
				}
					holder.mTitle.setVisibility(View.GONE);
					holder.mAddonDisplayTxtView.setVisibility(View.VISIBLE);
					holder.mProgressBar.setVisibility(View.GONE);
					holder.mTapToDownloadTxtView.setVisibility(View.VISIBLE);
					holder.mCancelDownloadIcon.setVisibility(View.VISIBLE);
					holder.mTapToDownloadIcon.setVisibility(View.VISIBLE);
					holder.parentRelative.setVisibility(View.VISIBLE);
					convertView.setBackgroundColor(Color.TRANSPARENT);
				
				// Bind the data efficiently with the holder.
				final String displayname;
			
				if(mSearchSelected) {
					displayname = mCurrentSearchList.get(position);
				} else {
						displayname = mLanguageAddonList.get(position);
					
				}
					
				if(mAddonDataMap.containsKey(displayname)){
					holder.mAddonDisplayTxtView.setText(displayname);
					final String fileName = mAddonDataMap.get(displayname).getFileName();
					
					String message = String.format(getResources().getString(R.string.kpt_tap_download), mAddonSearchTagsMap.get(displayname));

					// TP 15497 case 2 (New) text next to new langugaes has to be localized
					String newMessageStr = "(" + getString(R.string.kpt_UI_STRING_AMGR_2_300) + ")";
					if(message.contains(KPTConstants.NEW_STRING_ADDONS)) {
						int indexOfNew = message.indexOf(KPTConstants.NEW_STRING_ADDONS);
						
						String before = message.substring(0, indexOfNew);
						String after = message.substring(indexOfNew + KPTConstants.NEW_STRING_ADDONS.length(), message.length());
						String finalStr = before + newMessageStr + after;
						message = finalStr;
					}
					
					Spannable wordtoSpan = new SpannableString(message);   
					int spanStart = message.lastIndexOf(newMessageStr/*KPTConstants.NEW_STRING_ADDONS*/) ;
					int spanColor = Color.YELLOW;
					
					
					if(spanStart != -1){
						wordtoSpan.setSpan(new ForegroundColorSpan(spanColor), spanStart, spanStart + newMessageStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						wordtoSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD) , spanStart, spanStart + newMessageStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					holder.mTapToDownloadTxtView.setText(wordtoSpan);

					holder.mTapToDownloadIcon.setImageBitmap(mDownloadIcon);
					holder.mTitle.setText("");
					holder.mTitle.setVisibility(View.GONE);
					
					String key = KPTConstants.PREF_ADDON_DOWNLOAD_INPROGRESS + fileName;
					/*String downloadInfo = */mSharedPref.getString(key, null) ;
					/*Log.e("MS", "TapToDownloadAdapter KEY-->"+key +"--------->fileName-->"+fileName);
				 Log.e("MS", "downloadInfo-->"+downloadInfo+"**");*/

					/*Log.e("MS","Downloading list size :: " + mDownloadingAddonList.size());
				for (int i = 0; i < mDownloadingAddonList.size(); i++) {
					Log.e("MS",displayname + "<---- Downloading list -------->" + mDownloadingAddonList.get(i));
				}*/

					DownloadAddOnTask currentDonwloadTask = mDownloadAddOnTasksMap.get(displayname);
					if(currentDonwloadTask != null && currentDonwloadTask.getStatus() == AsyncTask.Status.RUNNING){
						holder.mProgressBar.setPadding(6, 0, 0, 10);
						holder.mProgressBar.setVisibility(View.VISIBLE);
						holder.mAddonCurrentStatusView.setVisibility(View.VISIBLE);
						holder.mAddonCurrentStatusView.setText(getString(R.string.kpt_UI_STRING_INAPP_STATUS_1));
						holder.mAddonDownloadPerc.setVisibility(View.VISIBLE);
						holder.mTapToDownloadTxtView.setVisibility(View.GONE);
						holder.mCancelDownloadIcon.setVisibility(View.VISIBLE);
						holder.mTapToDownloadIcon.setVisibility(View.GONE);
					} else {
						if(mInstallingList.contains(fileName)){
							holder.mProgressBar.setPadding(6, 0, 50, 10);
							holder.mProgressBar.setVisibility(View.VISIBLE); // change the kind of progress bar
							holder.mProgressBar.setIndeterminate(true);

							holder.mAddonCurrentStatusView.setVisibility(View.VISIBLE);
							holder.mAddonDownloadPerc.setVisibility(View.GONE);

							holder.mAddonCurrentStatusView.setText(getString(R.string.kpt_UI_STRING_INAPP_STATUS_2));
							holder.mTapToDownloadTxtView.setVisibility(View.GONE);

							holder.mCancelDownloadIcon.setVisibility(View.GONE);
							holder.mTapToDownloadIcon.setVisibility(View.GONE);
						} else {
							holder.mAddonCurrentStatusView.setVisibility(View.GONE);
							holder.mAddonDownloadPerc.setVisibility(View.GONE);
							holder.mProgressBar.setVisibility(View.GONE);
							holder.mTapToDownloadTxtView.setVisibility(View.VISIBLE);

							holder.mCancelDownloadIcon.setVisibility(View.GONE);
							holder.mTapToDownloadIcon.setVisibility(View.VISIBLE);
						}
					}

					OnClickListener cancelClickListener =  new OnClickListener() {
						@Override
						public void onClick(View v) {
							//Log.e("MS", "Download Buttton Clicked");
							DownloadAddOnTask cancelDownloadTask = mDownloadAddOnTasksMap.get(displayname);
							if(cancelDownloadTask != null && cancelDownloadTask.getStatus() == AsyncTask.Status.RUNNING){
								//Log.e("MS", "Download Buttton Clicked 11");

								mDisableMenuItem = false;

								mSharedPref.edit().putInt(KPTConstants.PREF_DOWNLOAD_STARTED, DOWNLOAD_FINISHED_CANCELLED).commit();

								holder.mProgressBar.setIndeterminate(true);
								holder.mProgressBar.setMax(0);
								holder.mProgressBar.setProgress(0);

								mInstallingList.remove(fileName);
								mDownloadAddOnTasksMap.remove(displayname);



								if(mLanguagesAdapter != null ) {
									mLanguagesAdapter.notifyDataSetChanged();
									if(mSearchSelected) {
										mSearchAdapter.notifyDataSetChanged();
									}
								}


								cancelDownloadTask.cancel(true);
								mDownloadAddOnTasksMap.remove(cancelDownloadTask);
							}
							
						}
					};

					holder.mCancelDownloadIcon.setOnClickListener(cancelClickListener);
					
					
					
				} else {
						holder.mTapToDownloadTxtView.setVisibility(View.GONE);
						holder.mCancelDownloadIcon.setVisibility(View.GONE);
						holder.parentRelative.setVisibility(View.GONE);
						holder.mTapToDownloadIcon.setVisibility(View.GONE);
						holder.mTitle.setVisibility(View.VISIBLE);
						holder.mTitle.setText(displayname);
						holder.mAddonDisplayTxtView.setVisibility(View.GONE);
						
//					}
					
				}
				return convertView;
			}
		} 

		static class LangListViewHolder {
			TextView mTitle;
			TextView mAddonDisplayTxtView;
			TextView mTapToDownloadTxtView;
			TextView mAddonCurrentStatusView;
			TextView mAddonDownloadPerc;
			ImageView mTapToDownloadIcon;
			ImageView mCancelDownloadIcon;
			ProgressBar mProgressBar;
			DownloadAddOnTask mDownloadAddOnTask;
			RelativeLayout parentRelative;

		}
		
		
		
		public static class AddonData {
			private String mDisplayName;
			String mFileName;
			private String mZipFileName;
			//private String mAddonCurrentVersion;
			public String mType;
			public String addonId;

			//private LinkedHashMap<String, String[]> mCompatibilityInfoMap;

			public String getAddonId() {
				return addonId;
			}

			public void setAddonId(String id) {
				addonId = id;
			}

			public String getZipFileName() {
				return mZipFileName;
			}

			public void setZipFileName(String zipFileName) {
				mZipFileName = zipFileName;
			}

			/*public String getToBeDownloadedAddonLatestVersion(){
				return mAddonCurrentVersion;
			}
			public void setToBeDownloadedAddonLatestVersion(String addonversion){
				mAddonCurrentVersion = addonversion;
			}

			public AddonData(){
				mCompatibilityInfoMap = new LinkedHashMap <String, String[]>();
			}

			public LinkedHashMap<String, String[]> getCompatibilityInfoMap(){
				return mCompatibilityInfoMap;
			}

			public void setCompatibilityInfoMap(LinkedHashMap<String, String[]> compatibilityInfoMap){
				mCompatibilityInfoMap = compatibilityInfoMap;
			}*/

			public void setDisplayName(String name) {
				mDisplayName = name;
			}

			public void setType(String name) {
				mType = name;
			}

			public void setFileName(String name) {
				mFileName = name;

			}

			public String getDisplayName() {
				return mDisplayName;
			}

			public String getFileName() {
				return mFileName;
			}

			/*public String getTypeName() {
				return mType;
			}*/

		/*	public boolean isCompatible(String baseVersion, String addonVersion) {
				//Log.e("MS", "Calling isCompatible Method");
				//Log.e("MS","Base version arg-->"+baseVersion);
				//Log.e("MS","Addon version arg-->" + addonVersion);

				if(addonVersion == null || baseVersion == null || mCompatibilityInfoMap == null) {
					return false;
				}

				String[] compatibleVersions = mCompatibilityInfoMap.get(addonVersion);
				if(compatibleVersions!=null){
					return Arrays.asList(compatibleVersions).contains(baseVersion);
				}else{
					return false;
				}
				for(String version : compatibleVersions) {
				if(version.equals(baseVersion)) {
					result = true;
					break;
				}
			}

			Log.e("MS","is compatible = "+ result);
			return result;
			}*/

		/*	public String getLatestCompatibleAddonVersion(String baseVersion) {
				//Log.e("MS", "Calling getLatestCompatibleAddonVersion Method");
				List<String[]> values = new ArrayList<String[]>(mCompatibilityInfoMap.values());
				for (int i = 0; i < values.size(); i++) {
					if(Arrays.asList(values.get(i)).contains(baseVersion)) {
						Set<String> keys = mCompatibilityInfoMap.keySet();
						String[] result = new String[keys.size()];
						result = keys.toArray(result);
						//Log.e("MS","Compatible Addon version for this base is-->" + result[i]);
						return result[i];
					}
				}

				return null;

			}*/
		}

		public class DownloadAddOnTask extends AsyncTask<String, Integer, String> {

			private ProgressBar mProgressbar;
			private AddonData mAddonInfo;
			private TextView mPercentageView;
			Date startDate;
			int mDownloadAttempts = 0;
			private String errorString = "Status Code: 403";
			boolean isTimeDiff = false;

			public DownloadAddOnTask(ProgressBar progressBar, String displayName, boolean isAddOnBeingUpgraded
					, TextView percentageView) {
				mPercentageView = percentageView;
				mProgressbar = progressBar;
				mAddonInfo = mAddonDataMap.get(displayName);
			}

			@Override
			protected void onPreExecute() {
				startDate = new Date();
				if(mSearchEdit != null && mSearchEdit.getVisibility() == View.VISIBLE) {
					mSearchSelected = false;

					mSearchEdit.setVisibility(View.GONE);
					mSearchListView.setVisibility(View.GONE);
					//mSearchListView.setAdapter(mSearchAdapter);

					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearchEdit.getWindowToken(), 0);

					findViewById(R.id.addon_downlaod_title_bar).setVisibility(View.VISIBLE);
					mLanguageListView.setVisibility(View.VISIBLE);

						mCurrentScreen = FIRST_SCREEN;
						mTitleHandler.sendEmptyMessageDelayed(mCurrentScreen, 0);
						mLanguageListView.post(new Runnable() {
							@Override
							public void run() {
								mLanguageListView.setSelection(mLanguageAddonList.indexOf(mAddonInfo.getDisplayName()));
							}
						});
					


					//fillAdapter(-1);

				}

				mDisableMenuItem = true;
				super.onPreExecute();
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				//Log.e("MS", "Download Task Has Been cancelled by user for the dictionary-->"+mAddonInfo.getFileName());

			}


			@Override
			protected String doInBackground(String... params) {
				//				Looper.prepare();
				String retVal = KPTConstants.DOWNLOAD_TASK_FAILED;
				//				Log.e("MS","isCancelled-->"+isCancelled());
				String mZipFileName = mAddonInfo.getZipFileName(); //ENGUS_Android_KPT_smartphone.zip
				String mAddonFileName = mAddonInfo.getFileName(); //engus
				//String mURL = BASE_URL +  "/" + mZipFileName;;
				String mAddOnVersion = "1.0";

				// Put the code to download addon from server
				String corePackagePath = getFilesDir() + KPTPkgHandlerService.sCorePackagePath;

				//check net only for first addo click
				if(checkNetworkConnection() && !isCancelled()) {
					try {

						//Download the addon zip file and copy into core packages folder
						//Date downloadTime = new Date(); 
						downloadAddOnZipFile(corePackagePath + mZipFileName, mZipFileName, mAddOnVersion);

						//Log.e("Time", "Total Download Time-->"+(new Date().getTime() - downloadTime.getTime()));
						String zipPath = corePackagePath  + mZipFileName;
						File zipFile = new File(corePackagePath +  mZipFileName);

						//ServerUtilities.sendAddonInfoToAnalyticServer(AddonDownloadActivity.this, ServerConstants.METHOD_TYPE_ADDON_INSTALLATION, mAddonInfo);

						// Unzip the downloaded file
						if(!isCancelled()) {
							//Date unzipTime = new Date(); 
							unzip(zipPath);
							//Log.e("Time", "Unzip Time-->"+(new Date().getTime()-unzipTime.getTime()));
							zipFile.delete();
						}

						retVal = KPTConstants.DOWNLOAD_TASK_SUCCESS;

						// Copying of ATP is done and since its valid we can remove the hash file and meta data (i.e. XML) files
						/*new File(corePackagePath + mAddonFileName.toUpperCase() + KPTPkgHandlerService.ADDON_EXTENSION).delete();
						new File(corePackagePath  + mAddonFileName.toUpperCase() + KPTPkgHandlerService.ADDON_EXTENSION + KPTPkgHandlerService.ADDON_XML_EXTENSION).delete();
						if(retVal.equals(KPTPkgHandlerService.DOWNLOAD_TASK_FAILED)){
							new File(corePackagePath + mAddonFileName.toUpperCase() + KPTPkgHandlerService.ADDON_EXTENSION + KPTPkgHandlerService.ADDON_ATP_EXTENSION).delete();
						}*/

					} catch (Exception e) {
						retVal = KPTConstants.DOWNLOAD_TASK_FAILED;
						if(retVal.equals(KPTConstants.DOWNLOAD_TASK_FAILED)){
							new File(corePackagePath + mAddonFileName.toUpperCase() + KPTConstants.ADDON_EXTENSION + KPTConstants.ADDON_ATP_EXTENSION).delete();
						}

						//Log.e("MS", "Exception");
						if((e.toString()).contains(errorString)){
							isTimeDiff = true;
						}
						e.printStackTrace();
					}
				}
				return retVal;
			}

			@Override
			protected void onPostExecute(String result) {

				mDownloadAddOnTasksMap.remove(mAddonInfo.getDisplayName());
				if(result.equals(KPTConstants.DOWNLOAD_TASK_SUCCESS)) {
					//					Log.e("MS", "DownloadTime For-->"+mAddonInfo.getFileName()+"  is-->"+(new Date().getTime() - startDate.getTime()));
					String addonZipFileName = mAddonInfo.getZipFileName();
					//Log.e("MS","LatestCompatibleAddonVersion " + addOnVersion);
					String url = BASE_URL  + "/" + addonZipFileName;
					//Log.e("MS","URL = " + url);
					//do a compatibility check first
					Intent intent = new Intent(AddonDownloadActivity.this, KPTPkgHandlerService.class);
					intent.setAction(KPTConstants.KPT_ACTION_PACKAGE_DOWNLOAD_INSTALL);
					intent.putExtra(KPTConstants.INTENT_EXTRA_URL, url);
					intent.putExtra(KPTConstants.INTENT_EXTRA_ZIP_FILE_NAME, addonZipFileName);
					intent.putExtra(KPTConstants.INTENT_EXTRA_FILE_NAME, mAddonInfo.getFileName());
					intent.setFlags(Intent.FLAG_FROM_BACKGROUND);
					/*if(isAddOnUpgraded){
					mUpgradingList.add(mAddonInfo.getFileName());
				} else {*/
					mInstallingList.add(mAddonInfo.getFileName());
					//}

					installAddOn(intent);
				} else if(isTimeDiff){
					//Log.e("MS", "Download Failed");
					Toast.makeText(AddonDownloadActivity.this, getString(R.string.kpt_UI_STRING_TOAST_4_110), Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(AddonDownloadActivity.this, getString(R.string.kpt_UI_STRING_INAPP_TOAST_4), Toast.LENGTH_LONG).show();
				}

				if(mDownloadAddOnTasksMap.size() == 0){
					mSharedPref.edit().putInt(KPTConstants.PREF_DOWNLOAD_STARTED, DOWNLOAD_FINISHED_CANCELLED).commit();
				}

				parseXML();
				int currentFocusedListItem = -1;
				/*if(mParentHorizontalScroll.getScrollX() <= mScreenWidth/2) {
					currentFocusedListItem = mLanguageListView.getFirstVisiblePosition();
				} else {
					currentFocusedListItem = mTechnincalListView.getFirstVisiblePosition();
				}*/
				fillAdapter(CALLED_FROM_DOWNLOAD_ADDON_TASK);

			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				mProgressbar.setProgress(values[0]);
				mPercentageView.setText(values[0] + " %");
				/*if(!mProgressHandler.hasMessages(MSG_UPDATE_PROGRESS)) {
					mProgressHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 300);
				}*/
			}
			
		/*	public void updateProgress(int downloadedSize, int totalSize) {
			    int percentage = downloadedSize / totalSize * 100;
			    publishProgress(percentage);
			}*/
			

			/**
			 * Download the addon zip file (Ex:ENGUS_Android_KPT_smartphone.zip) from server
			 * The ZIP file contains the following:
			 *   1. ATP (ENGUS_Android_KPT_smartphone.atp)  -- Used by core engine for installation of the AddOn
			 * @throws IOException 
			 * 
			 */

			private void downloadAddOnZipFile(String corePackagePath, String zipFileName, String addonVersion) throws Exception {


				InputStream is = null;
				FileOutputStream fos = null;
				URL  url = new URL(BASE_URL+"/"+zipFileName);

				try {
					// Create a trust manager that does not validate certificate chains
					TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}
						public void checkClientTrusted(X509Certificate[] certs, String authType) {
						}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {
						}
					}
					};

					// Install the all-trusting trust manager
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

					// Create all-trusting host name verifier
					HostnameVerifier allHostsValid = new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					};

					// Install the all-trusting host verifier
					HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
					URLConnection urlConn = url.openConnection();//connect

					is = urlConn.getInputStream();               //get connection inputstream
					fos = new FileOutputStream(corePackagePath);   //open outputstream to local file
					 // int totalSize = urlConn.getContentLength();
					 // int downloadedSize = 0;
					byte[] buffer = new byte[1024];              //declare 4KB buffer
					int len;

					//while we have availble data, continue downloading and storing to local file
					while ((len = is.read(buffer)) > 0) {  
						fos.write(buffer, 0, len);
						//downloadedSize += len;
						// updateProgress(downloadedSize, totalSize);
						

					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeyManagementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if (is != null) {
							is.close();
						}
					} finally {
						if (fos != null) {
							fos.close();
						}
					}
				}


				/*

				try {
					File myFile = new File(corePackagePath);
					Download download = mTransferManager.download(S3_BUCKET_NAME, AddonDownloadActivity.SUB_BUCKET +addOnVersion+"/"+zipFileName, myFile);
					mProgressbar.setIndeterminate(false);
					mProgressbar.setMax(100);
					while (download.isDone() == false) {
						int progress = (int)(download.getProgress().getPercentTransfered());
						//Log.e("MS", "Progress for "+zipFileName+" -->"+progress);
						publishProgress(progress);
						// Do work while we wait for our upload to complete...
					}
				} catch(Exception e) { 

				 * In very rare scenarios the Server connection will be lost because of
				 * Broken pipe. So, when a SSLException is caught try for another 3 attemps, 
				 * even then if connection cannot be established then raise a normal Exception

					if(e instanceof SSLException || e instanceof SocketException) {
						mDownloadAttempts ++;
						if(mDownloadAttempts <= DOWNLOAD_RETRY_ATTEMPTS) {
							Thread.sleep(DOWNLOAD_RETRY_DELAY);
							downloadAddOnZipFile(corePackagePath, zipFileName, addOnVersion);
						} else {
							throw e new Exception("e");
						}						
					} else {
						throw e;
					}
				}
				 */
				}

			private String unzip(String zipFileName) throws ZipException, IOException {
				String coreFile = null;
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
							//KPTLog.e(sAdaptxtDebug, "Unzip Copy failed");
							return null;
						}
					}
					else {
						entryDestination.mkdirs();
					}
				}
				zipFile.close();

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
		
		

		@Override
		protected void onDestroy() {
			////KPTLog.e(" -- KPTADDONENABLERACTIVITY --onDestroy", "onDestroy");
			super.onDestroy();
			
			if(mCoreEngine!=null) {
				mCoreEngine.destroyCore();
			}

			if(mCoreServiceHandler!=null) {
				mCoreServiceHandler.unregisterCallback(this);
				mCoreServiceHandler.destroyCoreService();
			}
			mCoreEngine = null;
			mCoreServiceHandler = null;
			
			PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
			
			
		}
		
		private synchronized void installAddOn(Intent intent) {
			//Log.e("MS", "StartService");
			startService(intent);
		}

		@Override
		public void afterTextChanged(Editable arg0) {
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
		}

		int textlength;
		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

			Editable text = mSearchEdit.getText();
			if(text != null && text.toString() != null) {
				textlength = text.toString().trim().length();
				mCurrentSearchList.clear();
				String[] array = mTempList.toArray(new String[mTempList.size()]);
				for (int i = 0; i < array.length; i++) {
					//if (textlength <= array[i].length()) {

					if ( textlength <= array[i].length() && array[i].substring(0, textlength).equalsIgnoreCase(mSearchEdit.getText().toString().trim())) {
						mCurrentSearchList.add(array[i]);
					}
					else if(mAddonSearchTagsMap.containsKey(array[i]))
					{

						String [] alt_words = mAddonSearchTagsMap.get(array[i]);
						for (int j = 0; j < alt_words.length; j++) 
						{
							if(textlength <= alt_words[j].length())
							{
								if(alt_words[j].substring(0, textlength).equalsIgnoreCase(mSearchEdit.getText().toString().trim()))
								{
									mCurrentSearchList.add(array[i]);
									break;
								}
							}
						}
					}
					//}
				}
				
				mSearchAdapter.notifyDataSetChanged();
				
			}
		}
		
		
		
		
}
