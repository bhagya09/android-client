package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kpt.adaptxt.beta.AdaptxtApplication;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class ThemeDetailsActivity extends Activity{

	private WebView mThemeDetailsWebView;
	private String URL = "http://www.adaptxt.com/adaptxtaddons/fulldetails?";
	private LinearLayout mParent;
	private String mDeviceName;
	
	private KPTAdaptxtDBHandler mKptDB;
	SharedPreferences mSharedPref;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kpt_theme_details_layout);
		mDeviceName = android.os.Build.MODEL;
		mParent = (LinearLayout) findViewById(R.id.webview_parent);
		handler.sendEmptyMessageDelayed(0, 0);
		
		mKptDB = new KPTAdaptxtDBHandler(getApplicationContext());
				mKptDB.open();
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
	}

	final Handler handler = new Handler(){

		public void handleMessage(Message msg) {
			showWebView();
		};
	};

	/**
	 * Shows the Themes WebView
	 */
	public void showWebView(){

		mThemeDetailsWebView = new WebView(this);

		int adaptxt_themeid = getIntent().getExtras().getInt("theme_id");

		String installedthemes = "";
		ArrayList<String> themeNames = getThemeNames();
		for(int i =0 ; i < themeNames.size(); i ++){
			installedthemes += themeNames.get(i)+"|";
		}

		mThemeDetailsWebView.getSettings().setJavaScriptEnabled(true);
		mThemeDetailsWebView.getSettings().setAppCacheEnabled(false);
		mThemeDetailsWebView.clearFormData();
		mThemeDetailsWebView.clearHistory();
		mThemeDetailsWebView.clearSslPreferences();
		mThemeDetailsWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		mThemeDetailsWebView.getSettings().setSupportMultipleWindows(true);
		mThemeDetailsWebView.getSettings().setSupportZoom(true);
		mThemeDetailsWebView.setVerticalScrollBarEnabled(false);
		mThemeDetailsWebView.getSettings().setBuiltInZoomControls(true);
		mThemeDetailsWebView.setWebViewClient(new ChildWebViewClient());
		mThemeDetailsWebView.addJavascriptInterface(new MyJavaScriptInterface(getApplicationContext()), "AndroidFunction");
		String queryString ="adaptxt_themeid=" + adaptxt_themeid +"&devicename="+ mDeviceName + "&installedthemes="+ installedthemes; 
		mThemeDetailsWebView.loadUrl(URL + queryString);
	}

	/**
	 * Used to show the progress until the page loads.
	 * @author SThatikonda
	 *
	 */
	private class ChildWebViewClient extends WebViewClient{
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			mParent.removeAllViews();
			mParent.addView(mThemeDetailsWebView);
			mParent.setGravity(Gravity.NO_GRAVITY);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// TODO Auto-generated method stub
			super.onPageStarted(view, url, favicon);

			ProgressBar mSpinner = new ProgressBar(ThemeDetailsActivity.this); 
			mSpinner.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			mSpinner.setIndeterminate(true);
			mParent.addView(mSpinner);
			mParent.setGravity(Gravity.CENTER);

		}

	}

	/**
	 * Java class to communicate with the html page click events. 
	 * Methods in this class are called by html java script. 
	 * @author SThatikonda
	 *
	 */
	public class MyJavaScriptInterface {
		Context mContext;

		// To retrieve installed theme names from db and send as parameter
		private String apkFileName;

		MyJavaScriptInterface(Context c) {
			mContext = c;
		}

		/**
		 * Downloads the Theme APK from Amazon server
		 * @param themeAPKName
		 */
		public void downloadThemeAddon(String themeAPKName){

			if (!mSharedPref.getBoolean(KPTConstants.PREFERENCE_EULA_ACCEPTED, false)
					|| !mSharedPref.getBoolean(KPTConstants.PREFERENCE_SETUP_COMPLETED, false)) {
				//Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_1_200, Toast.LENGTH_SHORT).show();			
			}else{

				PackageManager pm=getPackageManager();
				Intent intent = new Intent (Intent.ACTION_VIEW);
				intent.setData (Uri.parse (KPTConstants.MARKET_SEARCH_WORD + themeAPKName ));
				try {
					pm.getPackageInfo("com.android.vending", PackageManager.GET_UNINSTALLED_PACKAGES);
					startActivity (intent);
				} catch (NameNotFoundException e) {
					Toast.makeText(ThemeDetailsActivity.this, getResources().getText(R.string.kpt_android_market_app_unav), Toast.LENGTH_SHORT).show();
				}
			}
		}


		public void showThemeList(){
			AdaptxtApplication appState = null;// AdaptxtApplication.getAdaptxtApplication(this);
			appState.setIsFromThemeDetailsPage(true);
			setResult(RESULT_OK);
			finish();
		}
	}

	private ArrayList<String> getThemeNames(){
		return mKptDB.getExternalThemeNames();

	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		AdaptxtApplication appState = null;// AdaptxtApplication.getAdaptxtApplication(this);
		appState.setIsFromThemeDetailsPage(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 1){

		}
		AdaptxtApplication appState =  AdaptxtApplication.getAdaptxtApplication(this);
		appState.setIsFromThemeDetailsPage(true);
	}
	
	@Override
		protected void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
			try{
				mKptDB.close();
			}catch (SQLException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

}
