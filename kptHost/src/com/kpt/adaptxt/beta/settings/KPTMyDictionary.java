package com.kpt.adaptxt.beta.settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTMyDictionary extends ListActivity implements
KPTCoreServiceListener, Runnable, OnSharedPreferenceChangeListener {

	public ProgressDialog mProgressDialog;
	public ProgressDialog mPdDeleteAll;
	public ProgressDialog pleaseWait;

	public BackupDictBackgroundTask mBackupDictBackgroundTask;
	public RestoreUserDictTask mRestoreUserDictTask;
	//public static ImportWordsBackupDictBackgroundTask mImportUserDictTask;
	//public static final String PREFERENCE_IMPORT_DICT_CLICKED_ONCE = "import.clicked";
	private final static int DIALOG_TASKING = 100;
	// Menu item Ids
	public static final int ADD_WORD = Menu.FIRST;
	public static final int DELETEALL_ID = ADD_WORD + 1;
	public static final int SEARCH_WORD = DELETEALL_ID + 1;
	private Menu menu;

	protected static final int DISMISS_DIALOG = 0;
	protected static final int SHOW_DIALOG = 1;
	public final static long SIZE_KB = 1024L;

	private int SIMPLE_NOTFICATION_ID;

	public static final String TEST_FILE_NAME = "/testMyDict";
	//public static final String NO_WORDS_LEARNED = "Adaptxt hasn't learned any words";
	//public static final String TOAST_LEARNING_DONE = "Learning Words Done";
	//public static final String TOAST_NO_USERDICT_WORDS = "No Words in Android Dictionary to Learn";
	public static final String NOTOFICATION_ADAPTXT_BACKUP_TICKER = "Adaptxt BackUp Done";
	public static final String NOTOFICATION_ADAPTXT_BACKUP_TEXT = "Adaptxt Dictionary BackUp Completed";
	public static final String NOTOFICATION_ADAPTXT_BACKUP_TITLE = "Adaptxt";
	public static final String EXTERNAL_FILE_PATH_CONTEXT = "/Adaptxt/Context/";
	public static final String EXTERNAL_FILE_PATH_USER = "/Adaptxt/User";
	public static final String EXTERNAL_FILE_PATH_DICTIONARIES = "/Adaptxt/Dictionaries/";

	/*private static final String USERDICT_RESPONSE_SUCCESS = "user dict success";
	private static final String USERDICT_RESPONSE_FAILURE = "user dict failure";
	private static final String USERDICT_RESPONSE_NODICT = "no user dict";*/
	private static final String BACKUP_DONE = "done";
	public static ProgressDialog mpd;
	// public boolean mFocusable = true; //False when we start a progress dialog
	// & when addon is being installed

	// private static final int DIALOG_EDITWORD = 2;

	/**
	 * Word list displays list of user dictionary words
	 */
	// private ArrayAdapter<String> mWordList;

	/**
	 * Service handler to initiate and obtain core interface handle
	 */
	private KPTCoreServiceHandler mCoreServiceHandler = null;

	/**
	 * Core interface handle
	 */
	private KPTCoreEngine mCoreEngine = null;

	public ArrayList<String> mWordList = null;
	// public ArrayList<String> mWordImportBackupList = null;

	public static final String USER_CONTEXT_FILE_PATH = "/Profile/Profile/TestUser/Context";
	public static final String USER_DICT_FILE_PATH = "/Profile/Profile/TestUser/Dictionaries";
	public static final String USER_FILE_NAME = "/myuserwords";

	private boolean isCalled = false;



	public MyDictionaryAdapter mDictAdapter;

	Task mBackgroundTask;
	private AlertDialog.Builder mAlertDialog;
	private AlertDialog mDialog;
	private int mTotalWords;
	private ContextThemeWrapper mContextWrapper;
	private int mStartOffset = 0;
	private String mWordDataArray[] = null;
	int mWordsCount = 0;
	private int WORDS_OFFSET = 500;
	private boolean isFinalOffset = false;
	private boolean mOpenView = false;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public class Task extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			if (mCoreServiceHandler == null) {
				mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(getApplicationContext());
				mCoreEngine = mCoreServiceHandler.getCoreInterface();

				if (mCoreEngine != null) {
					//KPTLog.e("Core Engine NOT NULL ON RESUME","Core Engine NOT NULL ON RESUME");
					mCoreEngine.initializeCore(KPTMyDictionary.this);
				}
			}

			if (mCoreEngine == null) {
				mCoreServiceHandler.registerCallback(KPTMyDictionary.this);
			} else {
				mStartOffset = 0;
				populateUI();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			if (mDictAdapter != null) {
				mDictAdapter.notifyDataSetChanged();
				setListAdapter(mDictAdapter);
			}

			if (pleaseWait != null && pleaseWait.isShowing()){
				
				runOnUiThread(new Runnable(){
					public void run()
					{
						//Do you task here 
						pleaseWait.dismiss();
						pleaseWait = null;
					}
				});
			}

			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			//if(pleaseWait == null)
			pleaseWait = ProgressDialog.show(KPTMyDictionary.this,
					getResources().getString(R.string.kpt_title_wait),
					getResources().getString(R.string.kpt_loading_data));

			super.onPreExecute();
		}
	}
	/** To get the available space in SD card of the device*/
	public static long getAvailableSDcardSpace(){
		long SIZE_MB = SIZE_KB * SIZE_KB;
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long availableSpace = ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize())/SIZE_MB;
		return availableSpace;
	}
	@Override
	public void onResume() {

		//KPTLog.e("MYDICT", "OnResume");
		super.onResume();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		/*boolean isLicenseExpired = sharedPrefs.getBoolean(
				KPTConstants.PREF_CORE_LICENSE_EXPIRED, false);
		if (isLicenseExpired) {
			setListAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1,
					new String[] { getResources().getString(
							R.string.license_exp) }));
		} else {*/
			if(mWordList == null){
				mDictAdapter = new MyDictionaryAdapter(this);
				mWordList = new ArrayList<String>();
			}

			if(mBackgroundTask == null ||mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
				mBackgroundTask = new Task();
				mBackgroundTask.execute();
			}else{
				if(mWordList != null){
					populateUI();
				}
			}

//			mStartOffset = 0;
			isFinalOffset = false;
			isCalled = false;
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			if (sharedPref.getBoolean(
					KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS,
					false)
					&& (mpd == null || !mpd.isShowing())) {
				// mFocusable = false;
				//Log.e("MYDICT", "mPDR dialog started");
				mpd = ProgressDialog.show(KPTMyDictionary.this, getResources()
						.getString(R.string.kpt_title_wait), getResources()
						.getString(R.string.kpt_pd_add_on_install_progress), true,
						false);
			} else if (sharedPref
					.getBoolean(
							KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS,
							false)
							&& (mpd == null || !mpd.isShowing())) {
				//Log.e("MYDICT", "mPDR - II dialog started");
				mpd = ProgressDialog.show(KPTMyDictionary.this, getResources()
						.getString(R.string.kpt_title_wait), getResources()
						.getString(R.string.kpt_pd_add_on_uninstall_progress),
						true, false);
			} else {
				if (mpd != null && mpd.isShowing()) {
					mpd.dismiss();
				}
			}

			// if (mCoreServiceHandler == null) {
			// mCoreServiceHandler = KPTCoreServiceHandler
			// .getCoreServiceInstance(getApplicationContext());
			// mCoreEngine = mCoreServiceHandler.getCoreInterface();
			//
			// if (mCoreEngine != null) {
			// //KPTLog.e("Core Engine NOT NULL ON RESUME",
			// "Core Engine NOT NULL ON RESUME");
			// mCoreEngine.initializeCore(this);
			// }
			// }

			/*
			 * if (mCoreEngine == null) {
			 * mCoreServiceHandler.registerCallback(this); } else {
			 * populateUI();
			 * 
			 * 
			 * // if(pleaseWait != null || pleaseWait.isShowing()) //
			 * pleaseWait.dismiss(); SharedPreferences sharedPref =
			 * PreferenceManager .getDefaultSharedPreferences(this); if
			 * (sharedPref .getBoolean(
			 * KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS,
			 * false) && (mpd == null || !mpd.isShowing())) { // mFocusable =
			 * false; //KPTLog.e("MYDICT", "mPDR dialog started"); mpd =
			 * ProgressDialog.show( KPTMyDictionary.this,
			 * getResources().getString(R.string.kpt_title_wait),
			 * getResources().getString( R.string.kpt_pd_add_on_install_progress),
			 * true, false); } else if (sharedPref .getBoolean(
			 * KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS,
			 * false) && (mpd == null || !mpd.isShowing())) { //KPTLog.e("MYDICT",
			 * "mPDR - II dialog started"); mpd = ProgressDialog.show(
			 * KPTMyDictionary.this,
			 * getResources().getString(R.string.kpt_title_wait),
			 * getResources().getString( R.string.kpt_pd_add_on_uninstall_progress),
			 * true, false); } else { // mpd = ProgressDialog.show( //
			 * KPTMyDictionary.this, //
			 * getResources().getString(R.string.kpt_title_wait), //
			 * "Refreshing..",true, false); // populateUI();
			 * 
			 * } }
			 */
			//KPTLog.e("MYDICT", "SIZE " + mWordList.size());
			// getListView().setBackgroundColor(Color.parseColor("#333333"));
			getListView().setOnItemClickListener(new OnItemClickListener() {

				// @Override
				public void onItemClick(AdapterView<?> adapterView, View view,
						int position, long id) {
					if (position == 0) {
						if (android.os.Environment.getExternalStorageState()
								.equals(android.os.Environment.MEDIA_MOUNTED)) {
							long availableSDcardSpace = getAvailableSDcardSpace();
							if(availableSDcardSpace > 5){
								if (mBackupDictBackgroundTask == null
										|| mBackupDictBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
									mBackupDictBackgroundTask = new BackupDictBackgroundTask();
									mBackupDictBackgroundTask.execute("COPY",
									"COPY");
								}
							}
							else{
								new AlertDialog.Builder(KPTMyDictionary.this)
								.setTitle(
										getResources()
										.getText(
												R.string.kpt_dialog_title_warning))
												.setMessage(
														getResources()
														.getText(
																R.string.kpt_UI_STRING_TOAST_MESSAGE_8_300))
																.setPositiveButton(
																		R.string.kpt_alert_dialog_ok,
																		new DialogInterface.OnClickListener() {
																			public void onClick(
																					DialogInterface dialog,
																					int whichButton) {
																				dialog.dismiss();

																			}
																		}).create().show();
							}
							
						} else {
							new AlertDialog.Builder(KPTMyDictionary.this)
							.setTitle(
									getResources()
									.getText(
											R.string.kpt_dialog_title_warning))
											.setMessage(
													getResources()
													.getText(
															R.string.kpt_dialog_message_sdcard_absent))
															.setPositiveButton(
																	R.string.kpt_alert_dialog_ok,
																	new DialogInterface.OnClickListener() {
																		public void onClick(
																				DialogInterface dialog,
																				int whichButton) {
																			dialog.dismiss();

																		}
																	}).create().show();
						}
					}
					if(position == 1){
						File userFile =  new File(Environment
								.getExternalStorageDirectory().getAbsolutePath()+EXTERNAL_FILE_PATH_USER );
						if(userFile.exists()){
							if (mRestoreUserDictTask == null
									|| mRestoreUserDictTask.getStatus() == AsyncTask.Status.FINISHED) {
								mRestoreUserDictTask = new RestoreUserDictTask();
								mRestoreUserDictTask.execute("COPY",
								"COPY");
							}
						} else {
							Toast.makeText(KPTMyDictionary.this, R.string.kpt_UI_STRING_MYDICT_ITEM3, Toast.LENGTH_LONG).show();
						}

					}
					/*if (position == 1) {
						if (!PreferenceManager.getDefaultSharedPreferences(
								KPTMyDictionary.this).getBoolean(
								PREFERENCE_IMPORT_DICT_CLICKED_ONCE, false)) {
							SharedPreferences.Editor editor = PreferenceManager
									.getDefaultSharedPreferences(
											KPTMyDictionary.this).edit();
							editor.putBoolean(
									PREFERENCE_IMPORT_DICT_CLICKED_ONCE, true);
							editor.commit();
						}


						if (mImportUserDictTask == null
								|| mImportUserDictTask.getStatus() == AsyncTask.Status.FINISHED) {
							mImportUserDictTask = new ImportWordsBackupDictBackgroundTask();
							mImportUserDictTask.execute("IMPORT", "IMPORT");
						}
					}*/
				}
			});
		//}
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
				mContextWrapper = this; 
			}else{
				mContextWrapper = new ContextThemeWrapper(KPTMyDictionary.this, android.R.style.Theme_Holo); 
			}

		getListView().setOnScrollListener(new OnScrollListener() {

			private GetWordsTask wordsTask;
			private int nextCountOffset;
		

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == SCROLL_STATE_IDLE && mWordsCount < mTotalWords){
					if(wordsTask == null || wordsTask.getStatus() == AsyncTask.Status.FINISHED) {
						wordsTask = new GetWordsTask(nextCountOffset);
						wordsTask.execute();
						isCalled = true;
						nextCountOffset = 0;
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

				if(mWordsCount > 0){
					nextCountOffset = (mStartOffset + 250);
					
					
					//Next offset ranges from 2 items before and 3 items after, to handle fast scroll skip items
					if((firstVisibleItem >= (nextCountOffset - 9) && firstVisibleItem <= (nextCountOffset + 10)) && !isFinalOffset && !isCalled){
						if(wordsTask == null || wordsTask.getStatus() == AsyncTask.Status.FINISHED) {
							wordsTask = new GetWordsTask(nextCountOffset);
							wordsTask.execute();
							isCalled = true;
							nextCountOffset = 0;
						}
					}
				}
			}
		});
	}
	
	public class GetWordsTask extends AsyncTask<Void, Void, Void> {

		private int nextEndOffset;

		public GetWordsTask(int nextCountOffset) {
			// TODO Auto-generated constructor stub
			nextEndOffset = nextCountOffset;
		}

		@Override
		protected Void doInBackground(Void... params) {
//			int nextEndOffset = mWordsCount+ WORDS_OFFSET;
			if(nextEndOffset < mTotalWords){
				isFinalOffset = false;
//				nextEndOffset = WORDS_OFFSET;
			}else{
			
				
				isFinalOffset = true;
				nextEndOffset = (mTotalWords - mWordsCount);
			}
			
			mStartOffset = mWordsCount;
			mWordDataArray = mCoreEngine.getUserDictionary(mStartOffset, nextEndOffset, isFinalOffset);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (mWordDataArray != null) {
						mWordList.addAll(mWordsCount, new ArrayList<String>(Arrays.asList(mWordDataArray)));
						mDictAdapter.notifyDataSetChanged();
						//Collections.addAll(mWordList, mWordDataArray);
						mWordsCount = mWordsCount + mWordDataArray.length;

						mWordDataArray = null;
						isCalled = false;
					}

				}
			});
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			
			super.onPreExecute();
		}
	}

	private synchronized void populateUI() {
		if(mWordList != null){
			mWordList.clear();
			mWordList.add(0,getResources().getString(R.string.kpt_mydict_list_item_backup));
			mWordList.add(1,getResources().getString(R.string.kpt_UI_STRING_MYDICT_ITEM1));
		}
		if(mCoreEngine != null){
			mTotalWords = mCoreEngine.getUserDictionaryTotalCount();

			if(mWordDataArray != null || mWordList != null && mWordList.size() > 2){
				mStartOffset = 0;
				mWordsCount = 0;
				mWordDataArray = null;
				isFinalOffset = false;
			}


			/*mWordList.add(1,
				getResources().getString(R.string.kpt_mydict_list_item_import));*/


			if(mTotalWords > 0){
				if(mTotalWords < WORDS_OFFSET){
					mWordDataArray = mCoreEngine.getUserDictionary(mStartOffset, mTotalWords, isFinalOffset);
				}else{
					mWordDataArray = mCoreEngine.getUserDictionary(mStartOffset, WORDS_OFFSET, isFinalOffset);
				}
				mOpenView = true;
			}
		}


		if (mWordDataArray != null) {
			mWordList.add(2,getResources().getString(R.string.kpt_T101_UI_STRING_MENUITEMS_9)+" "+mTotalWords+"/"+KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
			Collections.addAll(mWordList, mWordDataArray);
			mWordsCount = mWordDataArray.length;
			mWordDataArray = null;

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if(menu != null){
						menu.getItem(1).setVisible(true);

						if((mWordList.size() - 3) >= KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT){
							menu.getItem(0).setVisible(false);
						}

					}

				}
			});
		}
		else{
			mWordList.add(2,getResources().getString(R.string.kpt_T101_UI_STRING_MENUITEMS_9)+" "+"/"+KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
		}
	}

	@Override
	protected void onPause() {
		//KPTLog.e("MYDICT", "OnPause");
		if (mpd != null && mpd.isShowing()) {
			mpd.dismiss();
			mpd = null;
		}
		mpd = null;

		PreferenceManager.getDefaultSharedPreferences(this)
		.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		// TODO Auto-generated method stub
		mCoreEngine = coreEngine;
		mDictAdapter = new MyDictionaryAdapter(this);
		if (mCoreEngine != null) {
			//KPTLog.e("MYDICT", "ServiceConnected");
			mCoreEngine.initializeCore(this);

			if(mBackgroundTask == null ||mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
				mBackgroundTask = new Task();
				mBackgroundTask.execute();
			}else{
				populateUI();
			}

			SharedPreferences sharedPref = PreferenceManager
			.getDefaultSharedPreferences(this);
			if (sharedPref.getBoolean(
					KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS,
					false)
					&& (mpd == null || !mpd.isShowing())) {
				// mFocusable = false;
				//KPTLog.e("MYDICT", "mPDR dialg started");
				mpd = ProgressDialog.show(KPTMyDictionary.this, getResources()
						.getString(R.string.kpt_title_wait), getResources()
						.getString(R.string.kpt_pd_add_on_install_progress), true,
						false);
			} else if (sharedPref
					.getBoolean(
							KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS,
							false)
							&& (mpd == null || !mpd.isShowing())) {
				//KPTLog.e("MYDICT", "mPDR dialg started");
				mpd = ProgressDialog.show(KPTMyDictionary.this, getResources()
						.getString(R.string.kpt_title_wait), getResources()
						.getString(R.string.kpt_pd_add_on_uninstall_progress),
						true, false);
			} else {
				// mpd = ProgressDialog.show(KPTMyDictionary.this,
				// getResources()
				// .getString(R.string.kpt_title_wait), "refreshing ui",
				// true, false);
				// populateUI();
				if (mpd != null && mpd.isShowing()) {
					mpd.dismiss();
				}
			}
		}
		//KPTLog.e("MYDICT", "SIZE SERVICE " + mWordList.size());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(1, ADD_WORD, 0,
				getResources().getString(R.string.kpt_mydict_menu_item_add));
		menu.add(0, DELETEALL_ID, 1,
				getResources().getString(R.string.kpt_mydict_dialog_del_title));
		menu.add(2, SEARCH_WORD, 2, getResources().getString(R.string.kpt_search_menu));
		
		this.menu = menu;
		
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Zero Menu: Import from Android dictionary
		// One Menu: Delete All

		/*if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				PREFERENCE_IMPORT_DICT_CLICKED_ONCE, false)) {
			menu.getItem(0).setVisible(true);
		} else
			menu.getItem(0).setVisible(false);*/
		if(mWordList == null){
			return false;
		}
		if (mWordList.size() > 2) 
			menu.getItem(1).setVisible(true);
		else 
			menu.getItem(1).setVisible(false);
		
		
		if((mWordList.size()-3) > KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT){
			menu.getItem(0).setEnabled(false);
		}
		else{
			menu.getItem(0).setEnabled(true);
		}
		

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mCoreEngine != null) {
			mCoreEngine = mCoreServiceHandler.getCoreInterface();
		}
		switch (item.getItemId()) {

		case DELETEALL_ID: {
			if (mWordList.size() == 3) {

				Toast.makeText(
						KPTMyDictionary.this,
						getResources().getString(
								R.string.kpt_mydict_toast_no_userdict),
								Toast.LENGTH_LONG).show();

			} else {
				final int totalWords = mCoreEngine.getUserDictionaryTotalCount();
				
				new AlertDialog.Builder(this).setTitle(getResources().getText(R.string.kpt_mydict_dialog_del_title))
				.setMessage(getResources().getText(R.string.kpt_mydict_delete_all))
				.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						if (mWordList.size() > 0) {
							dialog.dismiss();
							mPdDeleteAll = ProgressDialog.show(KPTMyDictionary.this, getResources().getText(R.string.kpt_title_wait),
									String.format(getResources().getString(R.string.kpt_dictionary_msg), (totalWords)));

							Thread thread = new Thread(KPTMyDictionary.this);
							thread.start();
						}
					}
				}).setNegativeButton(R.string.kpt_alert_dialog_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
						/* User clicked Cancel so do some stuff */
					}
				}).create().show();

			}

		}
		break;
		case ADD_WORD: 
			createAddWordDialog();
			break;
		case SEARCH_WORD: {

			if (mWordList.size() > 3) {
				Intent intent = new Intent(KPTMyDictionary.this,
						KPTMyDictionarySearch.class);
				intent.putExtra("words",
						mWordList.toArray(new String[mWordList.size()]));
				startActivity(intent);
			} else {
				Toast.makeText(KPTMyDictionary.this,
						R.string.kpt_mydict_toast_no_userdict, 2000).show();
			}
		}
		break;
		}
		return true;
	}
	/**
	 * creates a dialog when tapped on add menu item in my 
	 * dictionary and performs operations mentioned in dialog.
	 */

	public void createAddWordDialog(){

		mAlertDialog = new AlertDialog.Builder(mContextWrapper);
		LinearLayout v = (LinearLayout) getLayoutInflater().inflate(R.layout.kpt_add_word_to_mydictionary, null);
		mAlertDialog.setTitle(getResources().getString(R.string.kpt_UI_STRING_DIALOG_TITLE_6));
		mAlertDialog.setView(v);
		//filter to restrict the space.
		InputFilter filter = new InputFilter() { 
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) { 
					if (Character.isSpace(source.charAt(i))) {
						KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB = true;
						return ""; 
					}

				}
				KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB = false;
				return null;
			} 
		}; 

		final EditText etAddword = (EditText) v.findViewById(R.id.etAddWord);
		etAddword.setInputType(etAddword.getInputType()
			    | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
			    | EditorInfo.TYPE_TEXT_VARIATION_FILTER);
		etAddword.setFilters(new InputFilter[]{filter});
		final TextView tvErrorMsg = (TextView) v.findViewById(R.id.tvErrorMsg);
		
		
		
		mAlertDialog.setPositiveButton(getResources().getString(R.string.kpt_alert_dialog_ok), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				 //Do nothing here because we override this button later to change the close behaviour. 
                //However, we still need this because on older versions of Android unless we 
                //pass a handler the button doesn't get instantiated
			}
		});
		mAlertDialog.setNegativeButton(getResources().getString(R.string.kpt_alert_dialog_cancel), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				mDialog.dismiss();
			}
		});
		
		mDialog = mAlertDialog.create();
		WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
		lp.width = getResources().getDisplayMetrics().widthPixels;
		mDialog.getWindow().setAttributes(lp);
		mDialog.show();
		mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){            
			@Override
			public void onClick(View v)
			{
				Boolean wantToCloseDialog = false;
				//Do stuff, possibly set wantToCloseDialog to true then...
				String[] strAddword = new String[]{etAddword.getText().toString()};
				if(strAddword != null && !strAddword[0].equals("") && mCoreEngine != null ){
					//String strTemp = strAddword[0].toLowerCase();
					////strAddword[0] = strTemp;
					boolean isDictWord = mCoreEngine.addUserDictionaryWords(strAddword);
					if(!isDictWord){
						tvErrorMsg.setTextColor(Color.RED);
						tvErrorMsg.setVisibility(View.VISIBLE);
						tvErrorMsg.setText(getResources().getString(R.string.kpt_UI_STRING_ERROR_MESSAGE_1_110));
						etAddword.setText("");
					}
					else{
						wantToCloseDialog = true;
						mDialog.dismiss();
						mBackgroundTask = new Task();
						mBackgroundTask.execute();
						mCoreEngine.saveUserContext();
					}
				}else{
					tvErrorMsg.setTextColor(Color.RED);
					tvErrorMsg.setVisibility(View.VISIBLE);
					tvErrorMsg.setText(getResources().getString(R.string.kpt_T110_UI_STRING_ERROR_MESSAGE_2));
				}

				if(wantToCloseDialog)
					mDialog.dismiss();
			}
		});
		
		/*WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
		lp.width = getResources().getDisplayMetrics().widthPixels;
		mDialog.getWindow().setAttributes(lp);*/
		
	}

	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		if(mDialog != null && mDialog.isShowing()){
			WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
			lp.width = getResources().getDisplayMetrics().widthPixels;
			mDialog.getWindow().setAttributes(lp);
		}
		if(mPdDeleteAll != null & mPdDeleteAll.isShowing()){
			mPdDeleteAll.dismiss();
			mPdDeleteAll = null;
		}

	}

	@Override
	public void onDestroy() {
		//KPTLog.e("MYDICT", "OnDestroy");
		super.onDestroy();
		mStartOffset = 0;
		if(mOpenView){
			mCoreEngine.closeDictPages();
			mOpenView =false;
		}
		
		if (mCoreEngine != null) {
			mCoreEngine.saveUserContext();
			mCoreEngine.destroyCore();
		}

		if (mCoreServiceHandler != null) {
			mCoreServiceHandler.unregisterCallback(this);
			mCoreServiceHandler.destroyCoreService();
		}
		mCoreEngine = null;
		mCoreServiceHandler = null;
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}



	public class MyDictionaryAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		//private Bitmap mImportIcon;
		private Drawable mDelIcon;
		private Bitmap mBackupIcon;

		public MyDictionaryAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

			// Icons bound to the rows.
			/*mImportIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ic_menu_upload);*/
			// mDelIcon = BitmapFactory.decodeResource(context.getResources(),
			// android.R.drawable.ic_menu_delete);

			mDelIcon = context.getResources().getDrawable(
					android.R.drawable.ic_menu_delete);

			mBackupIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.kpt_dictionary_backup_icon);
		}

		public int getCount() {
			return mWordList.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean isEnabled(int position) {
			// TODO Auto-generated method stub
			/*
			 * if(!mFocusable) { return false; } else {
			 */
			return super.isEnabled(position);
			// }

		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unneccessary calls
			// to findViewById() on each row.
			final ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_my_dictionary_list,
						null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();

				holder.mText = (TextView) convertView.findViewById(R.id.text);
				holder.mText1 = (TextView) convertView.findViewById(R.id.text0);
				holder.mIcon = (ImageView) convertView.findViewById(R.id.icon);
				holder.mText2 = (TextView) convertView.findViewById(R.id.text1);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			convertView.setPadding(0, 5, 0, 5);
			SpannableString txt;
			switch (position) {
			case 0:
				holder.mText.setTextColor(Color.parseColor("#ffcc00"));
				holder.mText.setText(mWordList.get(position));
				holder.mIcon.setImageBitmap(mBackupIcon);
				holder.mIcon.setPadding(0, 0, 10, 0);
				holder.mText2.setVisibility(View.GONE);
				if (mBackupDictBackgroundTask != null
						&& mBackupDictBackgroundTask.getStatus() == Status.RUNNING) {
					//KPTLog.e("Backup dict running", "backup dict running");
					holder.mText1.setText(getResources().getText(
							R.string.kpt_backup_progress));
					holder.mText1.setTextColor(Color.parseColor("#ffcc00"));
					holder.mText1.setVisibility(View.VISIBLE);
				} else {
					holder.mText1.setText(getResources().getText(
							R.string.kpt_sdcard_required));
					holder.mText1.setTextColor(Color.parseColor("#999999"));
					holder.mText1.setVisibility(View.VISIBLE);
				}
				break;

			case 1:
				holder.mText.setTextColor(Color.parseColor("#ffcc00"));
				holder.mText.setText(mWordList.get(position));
				holder.mText1.setText(getResources().getText(
						R.string.kpt_UI_STRING_MYDICT_ITEM2));
				holder.mText1.setVisibility(View.VISIBLE);
				holder.mIcon.setVisibility(View.GONE);
				holder.mText2.setVisibility(View.GONE);
				break;
			case 2:
				holder.mText.setTextColor(Color.WHITE);
				holder.mText1.setVisibility(View.GONE);
				holder.mText2.setVisibility(View.GONE);
				// holder.mIcon.setImageBitmap(mDelIcon);
				holder.mIcon.setPadding(0, 0, 0, 0);
				String number_of_usd_words = "";
				if(mCoreEngine !=null )
				{
					number_of_usd_words = getResources().getString(R.string.kpt_T101_UI_STRING_MENUITEMS_9)+" "+mCoreEngine.getUserDictionaryTotalCount()+"/"+KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT;
				}
				else
				{
					number_of_usd_words = getResources().getString(R.string.kpt_T101_UI_STRING_MENUITEMS_9)+" 0"+"/"+KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT;
				}
				//holder.mText.setTextColor(Color.parseColor("#ffcc00"));
				txt = new SpannableString(number_of_usd_words);
				txt.setSpan(new ForegroundColorSpan(Color.parseColor("#ffffff")),0, txt.length(), 0);
				holder.mText.setText(txt, BufferType.SPANNABLE);
				holder.mIcon.setVisibility(View.GONE);
				break;
			default:
				holder.mText.setTextColor(Color.WHITE);
				holder.mText
						.setText(mWordList.get(position), BufferType.NORMAL);

				holder.mText1.setVisibility(View.GONE);
				holder.mText2.setVisibility(View.VISIBLE);
				// holder.mIcon.setImageBitmap(mDelIcon);
				holder.mIcon.setPadding(0, 0, 0, 0);

				//holder.mIcon.setImageDrawable(mDelIcon);
				
				
					/*String basicString = (position-1)+". ";
										String AlignString = "";*/
					holder.mIcon.setVisibility(View.VISIBLE);
					String num_spaces = " ";
					if(position<1000) num_spaces="   ";
					if(position<100) num_spaces="    ";
					if(position<10) num_spaces="     ";
				holder.mIcon.setVisibility(View.VISIBLE);
				txt = new SpannableString((position - 2) + ". ");
				txt.setSpan(
						new ForegroundColorSpan(Color.parseColor("#ffcc00")),
						0, txt.length(), 0);
				holder.mText2.setText(txt, BufferType.SPANNABLE);

				holder.mIcon.setImageDrawable(mDelIcon);


				break;
			}

			holder.mIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (position > 2) {
						boolean delCompl = mCoreEngine.removeUserDictionaryWords(new String[] { mWordList.get(position) });
						if (delCompl) {
							mWordList.remove(position);
							notifyDataSetChanged();
						}
					}
				}
			});
			return convertView;
		}

		class ViewHolder {
			TextView mText;
			TextView mText1;
			ImageView mIcon;
			TextView mText2;
		}
	}

	/*class ImportWordsBackupDictBackgroundTask extends
			AsyncTask<String, Void, String> {

		boolean addUserWords;

		@Override
		protected void onPreExecute() {

			getListView().setClickable(false);
			KPTMyDictionary.this.showDialog(DIALOG_TASKING);
		}

		@Override
		protected String doInBackground(String... params) {
			// Locale locale = Locale.getDefault();
			// String localeString = locale.getLanguage() + "-" +
			// locale.getCountry();

			ContentResolver cres = KPTMyDictionary.this.getContentResolver();
			Cursor cursor = cres
					.query(UserDictionary.Words.CONTENT_URI,
							PROJECTION,
							"(locale IS NULL) or (locale=?)",
							new String[] { KPTMyDictionary.this.getResources()
									.getConfiguration().locale.getDisplayName() },
							null);
			if (cursor != null && cursor.getCount() > 0) {
				//KPTLog.e("CURSOR NOT NULL", "COUNT IS " + cursor.getCount());
				// String[] userDictArray = new String[cursor.getCount()];
				String stringBuffer = "";
				if (cursor.moveToFirst()) {
					// int i=0;
					while (!cursor.isAfterLast()) {
						stringBuffer = stringBuffer
								+ cursor.getString(INDEX_WORD) + "\n";

	 * userDictArray[i]= cursor.getString(INDEX_WORD);
	 * if(mCoreEngine!=null) {
	 * mCoreEngine.addUserDictionaryWords(new
	 * String[]{cursor.getString(INDEX_WORD)}); } i++;

						cursor.moveToNext();
					}
				}
				cursor.close();
				if (mCoreEngine != null) {
					addUserWords = mCoreEngine
							.addUserDictionaryWords(stringBuffer);
					mWordList.clear();
					mWordList.add(
							0,
							getResources().getString(
									R.string.kpt_mydict_list_item_backup));
					mWordList.add(
							1,
							getResources().getString(
									R.string.kpt_mydict_list_item_import));
					if (mCoreEngine.getUserDictionary() != null) {
						//KPTLog.e("Core Engine USER DICT NOT NULL ", "***");
						Collections.addAll(mWordList,
								mCoreEngine.getUserDictionary());
						Set<String> h = new LinkedHashSet<String>(mWordList);
						mWordList.clear();
						mWordList.addAll(h);
					}
				}

				if (addUserWords)
					return USERDICT_RESPONSE_SUCCESS;
				else
					return USERDICT_RESPONSE_FAILURE;
			} else {
				if (cursor != null) {
					cursor.close();
					// cres.unregisterContentObserver(new
					// ContentObserver(null));
				}

				return USERDICT_RESPONSE_NODICT;
			}
		}

		@Override
		protected void onPostExecute(String str) {
			getListView().setClickable(true);
			KPTMyDictionary.this.dismissDialog(DIALOG_TASKING);
			if (str != null && str.equalsIgnoreCase(USERDICT_RESPONSE_SUCCESS)) {
				if (mDictAdapter != null) {
					mDictAdapter.notifyDataSetChanged();
				}
			} else if (str != null
					&& str.equalsIgnoreCase(USERDICT_RESPONSE_NODICT)) {
				Toast.makeText(
						KPTMyDictionary.this,
						getResources().getText(
								R.string.kpt_mydict_toast_no_userdict),
						Toast.LENGTH_LONG).show();
			}
		}
	}*/

	public class RestoreUserDictTask extends
	AsyncTask<String, Void, String> {
		ProgressDialog mProgressDialog;
		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(KPTMyDictionary.this, getResources().getString(R.string.kpt_title_wait), getResources().getString(R.string.kpt_UI_STRING_MYDICT_ITEM4));
			mDictAdapter.notifyDataSetChanged();
			super.onPreExecute();
		}
		@Override
		protected String doInBackground(String... arg0) {
			if(mCoreEngine != null) {
				String[] newUserWords = deSerialize();
				String userWordsString = "";
				for(int i = 0 ; i < newUserWords.length ; i++){
					userWordsString += newUserWords[i]+"\n";
				}
				if(userWordsString != null) {
					mCoreEngine.learnBuffer(userWordsString);
					mCoreEngine.saveUserContext();
				}
			} else {
				Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::Core Engine NULL");
			}
			return null;
		}

		private String[] deSerialize() {
			String[] userWords = null;
			try{
				File userFile =  new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()+EXTERNAL_FILE_PATH_USER  );
				if(!userFile.exists()) {
					return userWords;
				}
				InputStream file = new FileInputStream(userFile);
				InputStream buffer = new BufferedInputStream( file );
				ObjectInput input = new ObjectInputStream ( buffer );
				try{
					userWords = (String[])input.readObject();
				}
				finally{
					input.close();
				}
			}
			catch(ClassNotFoundException ex){
				Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::DeSerilzation Failed");
			}
			catch(IOException ex){
				Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::DeSerilzation Failed IO Exception");
			}

			return userWords;
		}


		@Override
		protected void onPostExecute(String str) {
			populateUI();
			Context context = getApplicationContext();
			CharSequence contentTitle = getResources().getText(
					R.string.kpt_ticker_text);
			CharSequence contentText = getResources().getString(R.string.kpt_UI_STRING_MYDICT_ITEM5);/* getResources().getText(
					R.string.kpt_notification_backup_done);*/
			NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			final Notification notifyDetails = new Notification(
					R.drawable.kpt_notification_icon, contentText,
					System.currentTimeMillis());
			if (context == null) {
				//KPTLog.e("CONEXT NULL", "");
			}
			Intent notifyIntent = new Intent(KPTMyDictionary.this,
					KPTMyDictionary.class);
			PendingIntent pIntent = PendingIntent.getActivity(context, 0,
					notifyIntent,
					notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
			if (pIntent == null) {
				//KPTLog.e("PINTNT NULL", "");
			}
			notifyDetails.setLatestEventInfo(context, contentTitle,
					contentText, pIntent);
			nManager.notify(SIMPLE_NOTFICATION_ID, notifyDetails); 
			mDictAdapter.notifyDataSetChanged();
			 try {
			        mProgressDialog.dismiss();
			        mProgressDialog = null;
			    } catch (Exception e) {
			        // nothing
			    }

		}

	}

	public class BackupDictBackgroundTask extends
	AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			mDictAdapter.notifyDataSetChanged();
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			File ExternalFile = new File(Environment
					.getExternalStorageDirectory().getAbsolutePath()
					+ EXTERNAL_FILE_PATH_CONTEXT);
			File InternalFile = new File(getFilesDir().getAbsolutePath()
					+ USER_CONTEXT_FILE_PATH);
			Date date = new Date();
			try {
				copyToSDCard(InternalFile, ExternalFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			File ExternalDictFile = new File(Environment
					.getExternalStorageDirectory().getAbsolutePath()
					+ EXTERNAL_FILE_PATH_DICTIONARIES);
			File InternalDictFile = new File(getFilesDir().getAbsolutePath()
					+ USER_DICT_FILE_PATH);
			try {
				copyToSDCard(InternalDictFile, ExternalDictFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			backUpUserDict();
			//KPTLog.e("TIME 2 COPY", new Date().getTime() - date.getTime() + "");

			return BACKUP_DONE;
		}

		private void backUpUserDict() {
			if(mCoreEngine != null) {
				String[] userWords = mCoreEngine.getUserDictionary();
				if(userWords!= null && userWords.length > 0){
					serialize(userWords);
					mCoreEngine.saveUserContext();
					File myFile = new File(getFilesDir().getAbsolutePath()+USER_FILE_NAME);
					File extFile = new File(Environment
							.getExternalStorageDirectory().getAbsolutePath()+EXTERNAL_FILE_PATH_USER );
					Log.e("BackUpDictTask", "myFile-->"+myFile.exists()+"  extFile-->"+extFile.exists());
					try {
						copyToSDCard(myFile, extFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
					myFile.delete();
				}
			} else {
				Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::Core Engine NULL");
			}

		}


		private void serialize(String[] strArr) {
			try{
				//use buffering
				OutputStream file = new FileOutputStream(getFilesDir().getAbsolutePath()+USER_FILE_NAME);
				OutputStream buffer = new BufferedOutputStream( file );
				ObjectOutput output = new ObjectOutputStream( buffer );

				try{
					output.writeObject(strArr);
					Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::Serilzation Success");
				}
				finally{
					output.close();
				}
			}  
			catch(IOException ex){
				Log.e("BackUpDictTask", "BackUpDictTask"+ "  ::Serilzation Failed");
			}

		}

		private void copyToSDCard(File src, File dest) throws IOException {
			// Check to ensure that the source is valid...
			if (!src.exists()) {
				throw new IOException("copyFiles: Can not find source: "
						+ src.getAbsolutePath() + ".");
			} else if (!src.canRead()) { // check to ensure we have rights to
				// the source...
				throw new IOException("copyFiles: No right to source: "
						+ src.getAbsolutePath() + ".");
			}
			// is this a directory copy?
			if (src.isDirectory()) {
				if (!dest.exists()) { // does the destination already exist?
					// if not we need to make it exist if possible (note this is
					// mkdirs not mkdir)
					if (!dest.mkdirs()) {
						throw new IOException(
								"copyFiles: Could not create direcotry: "
								+ dest.getAbsolutePath() + ".");
					}
				}
				// get a listing of files...
				String list[] = src.list();
				// copy all the files in the list.
				for (int i = 0; i < list.length; i++) {
					File dest1 = new File(dest, list[i]);
					File src1 = new File(src, list[i]);
					copyToSDCard(src1, dest1);
				}
			} else {
				// This was not a directory, so lets just copy the file
				FileInputStream fin = null;
				FileOutputStream fout = null;
				byte[] buffer = new byte[4096]; // Buffer 4K at a time (you can
				// change this).
				int bytesRead;
				try {
					// open the files for input and output
					fin = new FileInputStream(src);
					fout = new FileOutputStream(dest);
					// while bytesRead indicates a successful read, lets
					// write...
					while ((bytesRead = fin.read(buffer)) >= 0) {
						fout.write(buffer, 0, bytesRead);
					}
				} catch (IOException e) { // Error copying file...
					IOException wrapper = new IOException(
							"copyFiles: Unable to copy file: "
							+ src.getAbsolutePath() + "to"
							+ dest.getAbsolutePath() + ".");
					wrapper.initCause(e);
					wrapper.setStackTrace(e.getStackTrace());
					throw wrapper;
				} finally { // Ensure that the files are closed (if they were
					// open).
					if (fin != null) {
						fin.close();
					}
					if (fout != null) {
						fout.close();
					}
				}
			}

		}

		@Override
		protected void onPostExecute(String str) {
			if (str != null) {
				Context context = getApplicationContext();
				CharSequence contentTitle = getResources().getText(
						R.string.kpt_ticker_text);
				CharSequence contentText = getResources().getText(
						R.string.kpt_notification_backup_done);
				NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				final Notification notifyDetails = new Notification(
						R.drawable.kpt_notification_icon, contentText,
						System.currentTimeMillis());
				if (context == null) {
					//KPTLog.e("CONEXT NULL", "");
				}
				Intent notifyIntent = new Intent(KPTMyDictionary.this,
						KPTMyDictionary.class);
				PendingIntent pIntent = PendingIntent.getActivity(context, 0,
						notifyIntent,
						notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL);
				if (pIntent == null) {
					//KPTLog.e("PINTNT NULL", "");
				}
				notifyDetails.setLatestEventInfo(context, contentTitle,
						contentText, pIntent);
				nManager.notify(SIMPLE_NOTFICATION_ID, notifyDetails);
				mDictAdapter.notifyDataSetChanged();
			}
		}
	}

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mPdDeleteAll != null) {
				if (mDictAdapter != null) {
					mDictAdapter.notifyDataSetChanged();
				}
				if(mPdDeleteAll.isShowing()){
					mPdDeleteAll.dismiss();
					mPdDeleteAll=null;
				}
			}
		}
	};

	@Override
	public void run() {
		if (mWordList.size() > 0) {
			if (mCoreEngine.removeAllUserDictionaryWords()) {
				mWordList.clear();
				mWordList.add(0,getResources().getString(
						R.string.kpt_mydict_list_item_backup));
				mWordList.add(1,getResources().getString(R.string.kpt_UI_STRING_MYDICT_ITEM1));
				mWordList.add(2,getResources().getString(R.string.kpt_T101_UI_STRING_MENUITEMS_9)+" "+mCoreEngine.getUserDictionaryTotalCount()+"/"+KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
				mCoreEngine.saveUserContext();
				/*mWordList.add(
						1,
						getResources().getString(
								R.string.kpt_mydict_list_item_import));*/
			}
		}
		mHandler.sendEmptyMessage(0);
	}

	/*
	 * final Handler mPDHandler = new Handler();
	 * 
	 * final Runnable mPDRunnable = new Runnable() {
	 * 
	 * public void run() { //KPTLog.e("MYDICT", "RUN - START Dialog"); mpd =
	 * ProgressDialog.show(KPTMyDictionary.this,
	 * getResources().getString(R.string.kpt_title_wait),
	 * "Add-On is being installed", true, false); } };
	 * 
	 * final Runnable mPDdismissRunnable = new Runnable() {
	 * 
	 * public void run() { //KPTLog.e("MYDICT", "RUN - Dismiss Dialog");
	 * mpd.dismiss(); } };
	 */
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what == DISMISS_DIALOG) {
				if (mpd != null) {
					mpd.dismiss();
				}
				//KPTLog.e("MYDICT", "Dismiss Dialog");
				mpd = null;
				if (mCoreEngine != null) {
					//if(!mCalled){
						populateUI();
					//}
				}
			} else if (msg.what == SHOW_DIALOG) {
				SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(KPTMyDictionary.this);
				String pdMessage;
				if (prefs
						.getBoolean(
								KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS,
								false)) {
					pdMessage = getResources().getString(
							R.string.kpt_pd_add_on_install_progress);
				} else {
					pdMessage = getResources().getString(
							R.string.kpt_pd_add_on_uninstall_progress);
				}
				mpd = ProgressDialog
				.show(KPTMyDictionary.this,
						getResources().getString(R.string.kpt_title_wait),
						pdMessage, true, false);
				//KPTLog.e("MYDICT", "START Dialog");
				removeMessages(SHOW_DIALOG);
			}
			/*
			 * removeMessages(0); removeMessages(1);
			 */
			super.handleMessage(msg);
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// Fix for TP 7147
		if (prefs.getBoolean(
				KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS,
				false)
				|| prefs.getBoolean(
						KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS,
						false)) {
			if (mpd == null) {
				handler.removeMessages(SHOW_DIALOG);
				handler.removeMessages(DISMISS_DIALOG);
				handler.sendEmptyMessage(SHOW_DIALOG);
			}
		} else {
			handler.removeMessages(SHOW_DIALOG);
			handler.removeMessages(DISMISS_DIALOG);
			handler.sendEmptyMessage(DISMISS_DIALOG);
		}
		/*
		 * else { //KPTLog.e("MYDICT",
		 * "SharedPrefChange - Package installation Done"); if(mpd!=null &&
		 * mpd.isShowing()) { mPDHandler.post(mPDdismissRunnable); } }
		 */

	}


	@Override
	public void onContentChanged() {
		// TODO Auto-generated method stub
		super.onContentChanged();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_SEARCH) {

			if (mWordList.size() > 3) {
				Intent intent = new Intent(KPTMyDictionary.this,
						KPTMyDictionarySearch.class);
				intent.putExtra("words",
						mWordList.toArray(new String[mWordList.size()]));

				startActivity(intent);
				return true;
			} else {
				Toast.makeText(KPTMyDictionary.this,
						R.string.kpt_mydict_toast_no_userdict, 2000).show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}