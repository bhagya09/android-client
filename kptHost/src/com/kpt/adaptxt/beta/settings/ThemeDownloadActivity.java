package com.kpt.adaptxt.beta.settings;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kpt.adaptxt.beta.AdaptxtApplication;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class ThemeDownloadActivity extends Activity implements Runnable, 
OnSharedPreferenceChangeListener, KPTCoreServiceListener {

	private KPTAdaptxtDBHandler mKptDB;
	public static final String THEME_WEBPAGE_URL = "http://adaptxt.com/adaptxtaddons/list";
	public static final String EXTERNALTHEME_FOLDER = "ExternalThemes";
	public static final String TEMPORARY_FOLDER = "temp";
	public static final String EXTTHEME_IMAGES_FOLDER = "thumbnail";
	private HorizontalScrollView mParentHorizontalScroll;
	private AdaptxtApplication appState;
	private  final int FIRST_SCREEN = 1;
	private final int SECOND_SCREEN = 2;
	private final int THEMES_SCREEN = 4;
	private int mCurrentScreen = 1;
	private TextView mLeftHeader, mRightHeader;
	private float mDownX = -1;
	private boolean isRightToLeft;
	private int mScreenWidth;
	private Resources mResources;
	private ListView mInstalledThemesListView;
	private SharedPreferences mSharedPref;
	private KPTCoreEngine mCoreEngine;
	public ProgressDialog mProgressDialog;
	private DownloadedThemesAdapter mDownloadedThemes;
	private ArrayList<String> mInstalledThemesList;
	private GridView mThemesToDownloadView;
	private DownloadThemesAdapter mDownloadThemes;
	private ArrayList<String> mInstalledThemeNames;
	public static final String THEME_COMPATABILITY_XML = "adaptxt_external_themes.xml";
	public static final String THEME_XML_END_TAG = "table";
	
	public static final String XML_THEME_RESOURSES_TAG = "theme_resourses";
	public static final String XML_THEME_NAME = "theme_name";
	public static final String XML_THEME_DESCRIPTION = "theme_singledesc";
	public static final String XML_THEME_PRICE= "theme_price";
	public static final String XML_THEME_PACKAGENAME= "theme_pkgname";
	public static final String XML_THEME_THUMBIMAGE= "theme_thumbimage";
	public static final String XML_PARENT_TAG = "themes";
	
	private Map<String, ThemeData> mThemeDataMap;
	private ArrayList<String> mExternalThemeNames;
	XmlPullParserFactory xmlfactory;
	private LinearLayout mLagProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mKptDB = new KPTAdaptxtDBHandler(getApplicationContext());
		mKptDB.open();
		appState =  AdaptxtApplication.getAdaptxtApplication(this);
		mResources = getResources();
		mScreenWidth = mResources.getDisplayMetrics().widthPixels;
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		mThemeDataMap =  new LinkedHashMap<String, ThemeData>();
		
	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mKptDB.open();
		
		setContentView(R.layout.kpt_theme_download);
		mSharedPref.registerOnSharedPreferenceChangeListener(this);
		
		mInstalledThemesList = mKptDB.getAllThemeNames();
		Thread thread = new Thread(this);
		thread.start();
		setActivityUI();
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

		mInstalledThemesListView = (ListView) findViewById(R.id.theme_downloaded_left_list);
		mInstalledThemesListView.setOnItemClickListener(mListItemClickListener);		
		LayoutParams params = mInstalledThemesListView.getLayoutParams();
		params.width = getResources().getDisplayMetrics().widthPixels;
		mInstalledThemesListView.setLayoutParams(params);

		mThemesToDownloadView = (GridView)findViewById(R.id.mygrid);
		LayoutParams params2 = mThemesToDownloadView.getLayoutParams();
		params2.width = getResources().getDisplayMetrics().widthPixels;
		mThemesToDownloadView.setLayoutParams(params2);

		mLagProgress = (LinearLayout) findViewById(R.id.theme_download_lag_progress_parent);
		LayoutParams params3 = mLagProgress.getLayoutParams();
		params3.width = getResources().getDisplayMetrics().widthPixels;
		mLagProgress.setLayoutParams(params3);
		
		mLeftHeader = (TextView) findViewById(R.id.theme_download_title_left);
		//mRightHeader = (TextView) findViewById(R.id.theme_download_title_right);
		mLeftHeader.setText(getString(R.string.kpt_UI_STRING_AMGR_1));
		//mRightHeader.setText(getString(R.string.kpt_UI_STRING_MENUITEM_2_200));

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
		
		//Removing as per Reliance requirement
		/*mRightHeader.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mParentHorizontalScroll.post(new Runnable() {
					public void run() {
						mCurrentScreen = SECOND_SCREEN;
						mTitleHandler.sendEmptyMessageDelayed(mCurrentScreen, 0);
					}
				});
			}
		});*/

		mDownloadedThemes = new DownloadedThemesAdapter(this);
		mDownloadThemes = new DownloadThemesAdapter(this);

		mInstalledThemesListView.setAdapter(mDownloadedThemes);
		mThemesToDownloadView.setAdapter(mDownloadThemes);
		mThemesToDownloadView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {

				ThemeData data = mThemeDataMap.get(mExternalThemeNames.get(position));
				downloadThemeAddon(data.getmPackageName());
				updateAdapter();
			}
		});
	}
	
	private OnItemClickListener  mListItemClickListener = new OnItemClickListener() {
		private int currentThemeId;
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long arg3) {
			currentThemeId = Integer.parseInt(mSharedPref.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
			ThemesDownloaded mSelectedAddon = (ThemesDownloaded)view.getTag();
			if(position > 4){
				String selectedThemeName = mSelectedAddon.mDisplayTxtView.getText().toString();
				int rowId = mKptDB.getThemeId(selectedThemeName);
				if(currentThemeId != rowId){
					saveTheme(rowId);
				}
			}else{
				if(position != currentThemeId){
					saveTheme(position);
				}
			}

			final int childCount = arg0.getChildCount();
			for (int i = 0; i < childCount; i++) {
				final View v = arg0.getChildAt(i);
				final ThemesDownloaded mSelectedAddonHolder = (ThemesDownloaded)v.getTag();
				mSelectedAddonHolder.mDisplayTxtView.setChecked(false);
			}
			final ThemesDownloaded mSelectedAddonHolder = (ThemesDownloaded)view.getTag();
			mSelectedAddonHolder.mDisplayTxtView.setChecked(true);
			if(mDownloadedThemes != null){
				mDownloadedThemes.updateThemes();
				mDownloadedThemes.notifyDataSetChanged();
			}

		}
		
	};
	
	private void saveTheme(int pos) {
		SharedPreferences.Editor spEdit = mSharedPref.edit();
		spEdit.putString(KPTConstants.PREF_THEME_MODE, pos+"");
		spEdit.commit();
	}
	

	class DownloadThemesAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public DownloadThemesAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);
			updateAdapter();
		}

		public int getCount() {
			return mExternalThemeNames.size();
		}

		public Object getItem(int position) {
			return position;
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid unneccessary calls
			// to findViewById() on each row.
			ThemesDownload myHolder = new ThemesDownload();
			// When convertView is not null, we can reuse it directly, there is no need
			// to reinflate it. We only inflate a new View when the convertView supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_themes_grid_layout, null);
				// Creates a ViewHolder and store references to the two children views 
				// we want to bind data to.

				myHolder.mGridThemelogo = (ImageView)convertView.findViewById(R.id.grid_themes_logo);
				myHolder.mGridThemeName = (TextView)convertView.findViewById(R.id.grid_themes_name);
				myHolder.mGridDownloadIcon =(ImageView)convertView.findViewById(R.id.grid_themes_download);
				convertView.setTag(myHolder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				myHolder = (ThemesDownload) convertView.getTag();
			}

			ThemeData data = mThemeDataMap.get(mExternalThemeNames.get(position));
			myHolder.mGridThemeName.setText(data.getmExternalThemeName());
			myHolder.mGridThemelogo.setImageDrawable(data.getDrawable());
			myHolder.mGridDownloadIcon.setImageDrawable(getResources().getDrawable(R.drawable.kpt_theme_download));

			myHolder.mGridDownloadIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ThemeData data = mThemeDataMap.get(mExternalThemeNames.get(position));
					downloadThemeAddon(data.getmPackageName());
					updateAdapter();
				}
			});
			return convertView;
		}
	}
	
	class ThemesDownload {
		ImageView mGridThemelogo;
		TextView mGridThemeName;
		ImageView mGridDownloadIcon;
	}
	

	 /**
	   * Downloads the Theme APK from Amazon server
	   * @param themeAPKName
	   */
	  public void downloadThemeAddon(String pkgName){
		  AdaptxtApplication appState =  AdaptxtApplication.getAdaptxtApplication(this);;
		  
		  if (!mSharedPref.getBoolean(KPTConstants.PREFERENCE_EULA_ACCEPTED, false)
					|| !mSharedPref.getBoolean(KPTConstants.PREFERENCE_SETUP_COMPLETED, false)) {
				//Toast.makeText(getApplicationContext(), R.string.kpt_UI_STRING_TOAST_1_200, Toast.LENGTH_SHORT).show();			
		  }else{
			  PackageManager pm=getPackageManager();
			  Intent intent = new Intent (Intent.ACTION_VIEW);
			  intent.setData (Uri.parse (KPTConstants.MARKET_SEARCH_WORD + pkgName ));
			  try {
				  pm.getPackageInfo("com.android.vending", PackageManager.GET_UNINSTALLED_PACKAGES);
				  startActivity (intent);
				  appState.setIsFromThemeDetailsPage(true);
			  } catch (NameNotFoundException e) {
				  Toast.makeText(getApplicationContext(), getResources().getText(R.string.kpt_android_market_app_unav), Toast.LENGTH_SHORT).show();
			  }
		  }
	  }
	
	private ArrayList<String> getExternalThemesFromDB(){
		return mKptDB.getExternalThemeNames();
	}
	
	
	class DownloadedThemesAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private Drawable[] mThemeImages;

		private int defaultThemesCount;
		private int selectedDefaultTheme;
		private String themeName;
		
		public DownloadedThemesAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);
			updateThemes();
		}

		private void updateThemes(){
			  mInstalledThemeNames = mKptDB.getAllThemeNames();//getAllInstalledThemeNames(); //
			  for(int i=0;i<3;i++){
				  mInstalledThemeNames.remove(0);
			  }
			  String[] internalThemes = getResources().getStringArray(R.array.kpt_theme_options_entries);
			  defaultThemesCount = internalThemes.length;
				for (int i = 0; i < internalThemes.length; i++) {
					mInstalledThemeNames.add(i, internalThemes[i]);
				}
			  mThemeImages = mKptDB.getAllThemeDrawable();
			  selectedDefaultTheme = Integer.parseInt(
					  mSharedPref.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
			  themeName = getSelectedThemeName(selectedDefaultTheme);
		}
		  
		/**
		 * get all themes installed in Default, external and custom themes order
		 * 
		 * Fix to TP 15494 case 1
		 */
		public ArrayList<String> getAllInstalledThemeNames() {
			
			ArrayList<String> allInstalledThemes = new ArrayList<String>();
			
			//first add all default themes based on current language
			String[] internalThemes = getResources().getStringArray(R.array.kpt_theme_options_entries);
			defaultThemesCount = internalThemes.length;
			for (int i = 0; i < internalThemes.length; i++) {
				allInstalledThemes.add(internalThemes[i]);
			}
			
			//add all external themes
			ArrayList<KPTThemeItem> externalThemelist = mKptDB.getAllExternalThemes();
			for (int i = 0; i < externalThemelist.size(); i++) {
				allInstalledThemes.add(externalThemelist.get(i).themeName);
			}

			//add all custom themes
			ArrayList<String> customtheme = mKptDB.getAllCustomThemeNames();
			allInstalledThemes.addAll(customtheme);
			
			/*for (int i = 0; i < allInstalledThemes.size(); i++) {
				Log.e("kpt","all theme -->"+allInstalledThemes.get(i));
			}*/
			return allInstalledThemes;
		}
		
		public int getCount() {
			return mInstalledThemeNames.size();
		}

		public Object getItem(int position) {
			return position;
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid unneccessary calls
			// to findViewById() on each row.
			ThemesDownloaded holder = new ThemesDownloaded();
			// When convertView is not null, we can reuse it directly, there is no need
			// to reinflate it. We only inflate a new View when the convertView supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_downloaded_theme_list_item, null);
				// Creates a ViewHolder and store references to the two children views
				// we want to bind data to.
				holder.mPreviewImage = (ImageView)convertView.findViewById(R.id.theme_select_image);
				holder.mDisplayTxtView = (CheckedTextView) convertView.findViewById(R.id.radioButton);   
				holder.mDleteBinIcon = (ImageView) convertView.findViewById(R.id.theme_select_delete);  
				//holder.mSubtext = (TextView) convertView.findViewById(R.id.subtext);
				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ThemesDownloaded) convertView.getTag();
			}
			convertView.setBackgroundColor(Color.TRANSPARENT);
			holder.mDisplayTxtView.setText(mInstalledThemeNames.get(position));
			holder.mPreviewImage.setImageDrawable(mThemeImages[position]);
			if(position > (defaultThemesCount-1)){
				holder.mDleteBinIcon.setImageDrawable(getResources().getDrawable(R.drawable.kpt_delete_enable));
			}else{
				holder.mDleteBinIcon.setImageDrawable(getResources().getDrawable(R.drawable.kpt_delete_disable));
			}
			//holder.mSubtext.setText(getResources().getString(R.string.kpt_tap_enable));

			holder.mDisplayTxtView.setChecked(false);
			if(position > (defaultThemesCount-1)) {
				if(mInstalledThemeNames.get(position).equalsIgnoreCase(themeName)){
					holder.mDisplayTxtView.setChecked(true);
				}else{
					holder.mDisplayTxtView.setChecked(false);
				}
			} else {
				if (selectedDefaultTheme == position) {
					holder.mDisplayTxtView.setChecked(true);
				}
			}

			holder.mDleteBinIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					/*if (position > 4) {
						deleteThemeAlertDialog(position);

					}else{
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.kpt_UI_STRING_DIALOG_MESSAGE_4_300), Toast.LENGTH_SHORT).show();
					}*/
				}
			});
			return convertView;
		}

	}
	class ThemesDownloaded {
		ImageView mPreviewImage;
		CheckedTextView mDisplayTxtView;
		ImageView mDleteBinIcon;
		//TextView mSubtext;
	}
	
	private String getSelectedThemeName(int currentThemeId) {
		String themeName = null;
		try{
			themeName = mKptDB.getCurThemeName(currentThemeId);
			
		}catch (SQLiteException e) {
			e.printStackTrace();
		}
		return themeName;
	}
	public void removeExternalTheme(String themeName){
		
		Uri packageUri = Uri.parse("package:"+getThemePackageName(themeName));
        Intent uninstallIntent =
              new Intent(Intent.ACTION_DELETE, packageUri);
        startActivity(uninstallIntent);
	}
	/*public void deleteThemeAlertDialog(final int position){

		final int themeType = mKptDB.getThemeType(mInstalledThemeNames.get(position));
		AlertDialog.Builder myAlertDialogBuilder = new AlertDialog.Builder(this);
		myAlertDialogBuilder.setIcon(R.drawable.kpt_adaptxt_launcher_icon);
		if(themeType == -1){
			myAlertDialogBuilder.setTitle(getResources().getString(R.string.kpt_UI_STRING_DIALOG_TITLE_1_300));
			myAlertDialogBuilder.setMessage(getResources().getString(R.string.kpt_UI_STRING_DIALOG_MESSAGE_1_300));
		}else{
			myAlertDialogBuilder.setTitle(getResources().getString(R.string.kpt_UI_STRING_DIALOG_TITLE_2_300));
			myAlertDialogBuilder.setMessage(getResources().getString(R.string.kpt_UI_STRING_DIALOG_MESSAGE_2_300));
		}


		myAlertDialogBuilder.setPositiveButton(getResources().getString(R.string.kpt_alert_dialog_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				int themeTobeDeletedId = mKptDB.getThemeId(mInstalledThemeNames.get(position));
				if(themeType == KPTConstants.THEME_CUSTOM){
					int rowsEffected = mKptDB.deleteCustomThemes(mInstalledThemeNames.get(position));

					if(rowsEffected > 0){
						if(themeTobeDeletedId == Integer.parseInt(mSharedPref.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""))){
							//After some theme is removed; reset the preferences to default theme. 
							saveTheme(KPTConstants.DEFAULT_THEME);
						}
						mDownloadedThemes.updateThemes();
						mDownloadedThemes.notifyDataSetChanged();
						
						
					}
				}else{
					int currentTheme = Integer.parseInt(mSharedPref.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
					boolean currentThemeDeleting = mKptDB.isCurrentThemeDeleted(themeTobeDeletedId , currentTheme);
					
					removeExternalTheme(mInstalledThemeNames.get(position));
					
					if(themeTobeDeletedId == currentTheme || currentThemeDeleting){
						//After some theme is removed; reset the preferences to default theme. 
						saveTheme(KPTConstants.DEFAULT_THEME);
					}
					
					
					updateAdapter();
				}


			}});

		myAlertDialogBuilder.setNegativeButton(mResources.getString(R.string.kpt_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {

			}});

		AlertDialog myAlertDialog =  myAlertDialogBuilder.create();
		myAlertDialog.show();

	}*/
	

	
	 private String getThemePackageName(String themeName){
			return mKptDB.getThemePackageName(themeName);
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
	
	  
	  @Override
	  protected void onDestroy() {
		  super.onDestroy();
		  try{
			  mKptDB.close();
		  }catch (SQLException e) {
			  
		  }

	  }
	  
		// Handler used to change the language and technical titles accordingly
	  final Handler mTitleHandler = new Handler(){

		  public void handleMessage(Message msg) {

			  switch (msg.what) {
			  case FIRST_SCREEN:
				  //					mDisableMenuItem = false;
				  mParentHorizontalScroll.post(new Runnable() {
					  public void run() {
						  mParentHorizontalScroll.smoothScrollTo(0, 0);
					  }
				  });
				  mLeftHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.kpt_tab_selected));
				  //mRightHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_unselected));

				  break;
			  case SECOND_SCREEN:
			  case THEMES_SCREEN:
				  //					mDisableMenuItem = false;
				  mParentHorizontalScroll.post(new Runnable() {
					  public void run() {
						  mParentHorizontalScroll.smoothScrollTo(mScreenWidth, 0);
					  }
				  });
				  mLeftHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.kpt_tab_unselected));
				 // mRightHeader.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_selected));
				  //					showThemesWebViewUI();

				  break;

			  }
		  };
	  };

		public void onScreenChanged(boolean hasFocus) {
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
		}

		@Override
		public void serviceConnected(KPTCoreEngine coreEngine) {
			mCoreEngine = coreEngine;
			String txt = null;
			if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false)){
				txt =  getString(R.string.kpt_pd_add_on_install_progress);
			} else if(mSharedPref.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false)) {
				txt =  getString(R.string.kpt_pd_add_on_uninstall_progress);
			} else {
				txt =  getString(R.string.kpt_notofication_addon_downloading);
			}
			mProgressDialog = ProgressDialog.show(this, getResources().getText(R.string.kpt_title_wait),txt, true, false);
			Thread thread = new Thread(this);
			thread.start();
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences arg0,
				String arg1) {
		}

		@Override
		public void run() {
			updateAdapter();
		}
		
		public void copyRequireFiles(){
			float currentCompatibleThemesMasterVersion = getVersionNoOfCurrentCompatibleXML();
			if(checkNetworkConnection()){
				float currentVersionThemesMasterFromServer = 0;
				try {
					//currentVersionThemesMasterFromServer = getLatestCompatibilityVerNoInServer();
					/**
					 * IF the version of compatibility matrix xml in the internal storage of this app 
					 * is less than the version of compatibility matrix xml on the server,
					 * we download the latest compatibilty xml and update the respective shared preference
					 * 
					 */
					//if(currentCompatibleThemesMasterVersion != currentVersionThemesMasterFromServer) {
						MyAsynchTask myAsynchTask = new MyAsynchTask();
						myAsynchTask.execute(currentCompatibleThemesMasterVersion);
					//}
				} catch (Exception e) {
					e.printStackTrace();
					atxAssetsCopy();
					update();
				}
			} else {
				atxAssetsCopy();
				update();
			}
		}
		
		
	
		
		class MyAsynchTask extends AsyncTask<Float, Void, Boolean>{

			float currentCompatibleThemesMasterVersion;
			@Override
			protected Boolean doInBackground(Float... params) {
				// TODO Auto-generated method stub
				currentCompatibleThemesMasterVersion = params[0];


				boolean result = false;

				try {
				
						atxAssetsCopy();
						//update();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					atxAssetsCopy();
					//update();
				}
				return result;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				
				
				//boolean isFileDownloadedSuccessfully = result;//getLatestCompatibiltyXMLFromServer(ThemeDownloadActivity.this);
				if(result){
					SharedPreferences.Editor editor = mSharedPref.edit();
					editor.putFloat(KPTConstants.PREF_CURRENT_COMPATIBILITY_THEMES_XML_VERSION, currentCompatibleThemesMasterVersion);
					editor.commit();
					String zipPath = getFilesDir().getAbsolutePath()+"/"+TEMPORARY_FOLDER+"/" + KPTConstants.EXTERNAL_THEMMES_ZIPNAME;
					//						String zipPath = filePath + "/ExternalThemes.zip";
					File atxZipFile = new File(zipPath);
					if(atxZipFile.exists()) {
						File whereTo = new File(getFilesDir().getAbsolutePath());
						myUnzip(zipPath,whereTo);
					}
					File atxZipFile1 = new File(getFilesDir().getAbsolutePath()+"/"+TEMPORARY_FOLDER );
					boolean yes = atxZipFile1.delete();
					
				} else {
					atxAssetsCopy();
				}
			
				update();
			}
			
		}
		
		public void updateAdapter(){
			mExternalThemeNames = new ArrayList<String>();
			copyRequireFiles();
			
		}
	
		public void update() {
			//do parsing
			doParsing();
			//updating the map
			ArrayList<String> externalThemesfromDB = getExternalThemesFromDB();
			/**
			 * Get the External themes from DB, if they installed in device, update the map.
			 */
			if(externalThemesfromDB != null && externalThemesfromDB.size()>0){
				for (int i = 0; i < externalThemesfromDB.size(); i++) {
					String extThemeName = externalThemesfromDB.get(i);
					ThemeData data = mThemeDataMap.get(extThemeName);
					if(data != null){
						String themeName = data.getmExternalThemeName();
						if(themeName != null && themeName.equalsIgnoreCase(extThemeName)){
							mThemeDataMap.remove(extThemeName);
						}
						
					}else{
						// To be removed in next version as external themes names are updated in theme apk's
						//#15495. UI localisation issues_All languages 2nd case
						
						if(extThemeName !=null && extThemeName.indexOf(" ") != -1){							
							extThemeName =	extThemeName.substring(0, extThemeName.indexOf(" "));							
						}
						ThemeData data1 = mThemeDataMap.get(extThemeName);
						if(data1 !=null){
							String themeName = data1.getmExternalThemeName();
							if(themeName.equalsIgnoreCase(extThemeName)){
								mThemeDataMap.remove(extThemeName);
							}
						}
					}
				}
			}
			Iterator<String> keysIterator = mThemeDataMap.keySet().iterator();
			mExternalThemeNames.clear();
			//Preparing Map
			while (keysIterator.hasNext()) {
				String key = keysIterator.next();
				mExternalThemeNames.add(key);
			}
			
			if(mDownloadThemes != null) {
				mDownloadThemes.notifyDataSetChanged();
			}
			
			mLagProgress.setVisibility(View.GONE);
			mThemesToDownloadView.setVisibility(View.VISIBLE);
		}
		
		public class ThemeData{
			private String mExternalThemeName;
			private String mExternalThemeNameDesc;
			private String mExternalThemeNameThumbId;
			private String mPrice;
			private Drawable mDrawable;
			private String mPackageName;
			public  ThemeData(){
			}
			
			public String getmExternalThemeName() {
				return mExternalThemeName;
			}

			public void setmExternalThemeName(String mExternalThemeName) {
				this.mExternalThemeName = mExternalThemeName;
			}

			public String getmExternalThemeNameDesc() {
				return mExternalThemeNameDesc;
			}

			public void setmExternalThemeNameDesc(String mExternalThemeNameDesc) {
				this.mExternalThemeNameDesc = mExternalThemeNameDesc;
			}

			public String getmExternalThemeNameThumbId() {
				return mExternalThemeNameThumbId;
			}

			public void setmExternalThemeNameThumbId(String mExternalThemeNameThumbId) {
				this.mExternalThemeNameThumbId = mExternalThemeNameThumbId;
			}

			public Drawable getDrawable() {
				return mDrawable;
			}

			public void setDrawable(Drawable drawable) {
				this.mDrawable = drawable;
			}

			public String getmPrice() {
				return mPrice;
			}

			public void setmPrice(String mPrice) {
				this.mPrice = mPrice;
			}

			public String getmPackageName() {
				return mPackageName;
			}

			public void setmPackageName(String mPackageName) {
				this.mPackageName = mPackageName;
			}
		}

		
	
	
	
	public void doParsing(){
		XmlPullParser parser = null;
		try {
			xmlfactory = XmlPullParserFactory.newInstance();
			File myFile = new File(getFilesDir().getAbsolutePath() + "/"+EXTERNALTHEME_FOLDER+"/"+ THEME_COMPATABILITY_XML);
			
			InputStream is = null;
			parser = xmlfactory.newPullParser();
			is = new FileInputStream(myFile);
			parser.setInput(is, null);

			ThemeData themeData = null;
			String themeName = null;

			int eventType = parser.getEventType();
			do {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					if (parser.getName().equals(XML_THEME_RESOURSES_TAG)) {
						themeData = new ThemeData();
					} else if(parser.getName().equals(XML_THEME_NAME)) {
						themeName = parser.nextText();
						themeData.setmExternalThemeName(themeName);
					} else if(parser.getName().equals(XML_THEME_DESCRIPTION)) {
						themeData.setmExternalThemeNameDesc(parser.nextText());
					} else if(parser.getName().equals(XML_THEME_PRICE)) {
						themeData.setmPrice(parser.nextText());
					}  else if(parser.getName().equals(XML_THEME_PACKAGENAME)) {
						themeData.setmPackageName(parser.nextText());
					}else if(parser.getName().equals(XML_THEME_THUMBIMAGE)) {
						 String imageName = parser.nextText(); 
						 String filePath = getFilesDir().getAbsolutePath() +"/"+EXTERNALTHEME_FOLDER+"/"+EXTTHEME_IMAGES_FOLDER+"/"+imageName;
						 Drawable drwble = Drawable.createFromPath(filePath);
						 themeData.setDrawable(drwble);
					}
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().equals(XML_THEME_RESOURSES_TAG)) {
						mThemeDataMap.put(themeName, themeData);
					}
					break;
				}
				eventType = parser.next();

			} while (eventType != XmlPullParser.END_DOCUMENT);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 	Gets the version number of the themesmaster xml where
	 * for the first time it is taken from assets, if we download the latest xml from server
	 * then we change the shared preference accordingly and get that value
	 */
	private float getVersionNoOfCurrentCompatibleXML() {
		Float currentXmlVersion = mSharedPref.getFloat(KPTConstants.PREF_CURRENT_COMPATIBILITY_THEMES_XML_VERSION, 0);
		if(currentXmlVersion == 0) {
			SharedPreferences.Editor editor = mSharedPref.edit();
				currentXmlVersion = Float.parseFloat(getResources().getString(R.string.kpt_themes_master_xml_version));
				editor.putFloat(KPTConstants.PREF_CURRENT_COMPATIBILITY_THEMES_XML_VERSION, currentXmlVersion);
				editor.commit();
			
		} 
		return currentXmlVersion;
	}

	private void atxAssetsCopy() {
		String filePath = getFilesDir().getAbsolutePath();
		AssetManager assetMgr = getAssets();

		String path = filePath+"/"+EXTERNALTHEME_FOLDER;
		File atxFile = new File(path);
		if(atxFile.exists()) {
			return;
		} 

		try {
			InputStream isd = assetMgr.open(KPTConstants.ATX_ASSETS_FOLDER+KPTConstants.EXTERNAL_THEMMES_ZIPNAME);
			OutputStream os = new FileOutputStream(filePath+"/"+KPTConstants.EXTERNAL_THEMMES_ZIPNAME); 
			byte[] b = new byte[1024]; 
			int length;
			while ((length = isd.read(b))>0) { os.write(b,0,length);}
			isd.close();
			os.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		String zipPath = filePath + "/"+KPTConstants.EXTERNAL_THEMMES_ZIPNAME;
		File atxZipFile = new File(zipPath);
		if(atxZipFile.exists()) {
			unzip(zipPath);
		}
		atxZipFile.delete();

	}
	
	private static void unzip(String zipFileName) {
		try {
			File file = new File(zipFileName);
			ZipFile zipFile = new ZipFile(file);

			// create a directory named the same as the zip file in the 
			// same directory as the zip file.
			File zipDir = new File(file.getParentFile(), EXTERNALTHEME_FOLDER);
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
				entryDestination.getParentFile().mkdirs();

				// Directories are included as seperate entries 
				// in the zip file.
				if(!entry.isDirectory()) {
					generateFile(entryDestination, entry, zipFile);
				} else {
					entryDestination.mkdirs();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void myUnzip(String zipFileName, File path){
		try {
			
			File file = new File(zipFileName);
			ZipFile zipFile = new ZipFile(file);
			// create a directory named the same as the zip file in the 
			// same directory as the zip file.
			File zipDir = new File(path, EXTERNALTHEME_FOLDER);
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
				entryDestination.getParentFile().mkdirs();

				// Directories are included as seperate entries 
				// in the zip file.
				if(!entry.isDirectory()) {
					generateFile(entryDestination, entry, zipFile);
				} else {
					entryDestination.mkdirs();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
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
		

}
