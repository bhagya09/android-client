/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpt.adaptxt.beta;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.CurrentWord;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.KPTCorrectionInfo;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngineImpl.KPTInputInfo;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;
import com.kpt.adaptxt.beta.core.coreservice.KPTSuggestion;
import com.kpt.adaptxt.beta.core.coreservice.KPT_SUGG_STATES;
import com.kpt.adaptxt.beta.glide.KPTCustomGesturePreferences;
import com.kpt.adaptxt.beta.glide.KPTGlideKBShortcuts;
import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.keyboard.KeyboardActionListener;
import com.kpt.adaptxt.beta.keyboard.PointerTracker;
import com.kpt.adaptxt.beta.packageinstaller.KPTPkgHandlerService;
import com.kpt.adaptxt.beta.settings.KPTATRListview;
import com.kpt.adaptxt.beta.settings.KPTAdaptxtIMESettings;
import com.kpt.adaptxt.beta.settings.KPTAddATRShortcut;
import com.kpt.adaptxt.beta.settings.KPTAddonManager;
import com.kpt.adaptxt.beta.settings.KPTQuickSettingsViewManager;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.util.SystemInfo;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;
import com.kpt.adaptxt.beta.view.KPTCandidateView;
import com.kpt.adaptxt.beta.view.KPTCandidateViewContainer;
import com.kpt.adaptxt.beta.view.KPTCandidateViewPopup;
import com.kpt.adaptxt.beta.view.KPTLinkEnabledEditTextView;
import com.kpt.adaptxt.core.coreapi.KPTParamDictionary;
import com.kpt.adaptxt.core.coreapi.KPTSuggEntry;




/**
 * Input method implementation for Qwerty'ish keyboard.
 */

@SuppressWarnings("deprecation")
public class AdaptxtIME implements 
KeyboardActionListener, SharedPreferences.OnSharedPreferenceChangeListener,
android.view.View.OnClickListener,KPTCoreServiceListener,KPTLinkEnabledEditTextView.TextLinkClickListener {
	
	public static final String hindhiLocale ="hi";
	public static boolean mIsHindhi = false;
	
	private static AdaptxtIME sATXKeyboardController = null;

	private Context mContext;
	private AdaptxtEditText mEditText = null;
	private boolean mUserSelectedCompleteText = false;

	private StringBuffer mTextInBuffer = new StringBuffer();
	private int mCurrCursorPos = 0;
	
	//Fix for the Bugs 6222 and 6616
	public boolean onShowInputRequested(int flags, boolean configChange) {
		// TODO Auto-generated method stub
		return true;
	}


	/**
	 *  this is used to sync between cusror positions changes and onupdateselection calls.
	 *  
	 *  this will be incremented when we are NOT EXPECTING onupdateselection like for char input throught SIP.
	 *  else there can be a jump or cut or paste
	 */
	private int mExpectingUpdateSelectionCounter;

	/**
	 *  Package names for which we follow special behavior
	 */


	/**
	 *  Defines what type of unknown editor it is.
	 *  For Ex: 
	 *  0 - email editor
	 *  1 -  Polaris Office from Samsung Store        
	 * 
	 */
	public int mUnknownEditorType = -1;


	/**
	 *  Constants related to Core
	 */
	private static final int MAX_NO_OF_SUGG = 20;

	/**
	 *  Constants related to Contextual menu positions
	 */
	private static final int POS_SETTINGS = 0;
	private static final int POS_CHOOSE_KEYBOARD_TYPE = 1;
	private static final int POS_METHOD = 2;

	/**
	 *  The private IME option used to indicate that no microphone should be
	 *  shown for a given text field. For instance this is specified by the search dialog
	 *   when the dialog is already showing a voice search button.
	 */
	private static final String IME_OPTION_NO_MICROPHONE = "nm";
	private static final int NOT_AN_EDITOR = 0;
	private int htc_to_field_editor = 2448;



	public static final String ELAPSED_TIME_ACTION = "elapsed_time_action";
	/**
	 *  Constants related to time intervals
	 */
	private final int MULTITAP_SUGGESTION_INTERVAL = 800;
	private static final int DELETE_ACCELERATE_AT = 20;//How many continuous deletes at which to start deleting at a higher speed.
	private static final int QUICK_PRESS = 200;// Key events faster than this are long-presses.
	private static final int COMPOSING_TIME_LIMIT = 500;

	private boolean mIsXiTouched = false; //to know whether xi key already touched or not 
	/**
	 *  Constants related to Vibration
	 */
	public static final int VIBRATE_GENERIC = 0;
	public static final int VIBRATE_NO_SUGG = 1;
	public static final int VIBRATE_ACC_SUGG = 2;
	private boolean mPrevCharUpper = false;
	/**
	 * Constants related to Handler Messages
	 * 
	 */
	private static final int MSG_UPDATE_SUGGESTIONS = 0;
	private static final int MSG_UPDATE_SHIFT_STATE = 2;
	public static final int MSG_RELOAD_KEYBOARDS = 5;
	public static final int MSG_SPACE_LONGPRESS = 9;
	//private static final int MSG_SHOW_UPDATE_DIALOG = 10;
	private static final int MSG_HKB_DELETE_LONGPRESS = 11;
	//private static final int MSG_SHOW_UPGRADE_PAID_USER = 12;
	//private static final int MSG_SHOW_UNPAID_USER_TRIAL_EXPIRY = 13;
	
	private final int MSG_GET_VIEW_HEIGHT = 15;
	/**
	 * Constants related to HKB 
	 */
	private static final int HKB_DEFAULT_STATE = 0;
	private static final int HKB_LONGPRESS_STATE = 1;
	private static final int HKB_IGNORE_STATE = 2;
	//These flags indicates the states (off, on, lock) of Alt & Shift keys of the HKB
	private static final int HKB_STATE_OFF = 0;
	private static final int HKB_STATE_ON = 1;
	private static final int HKB_STATE_LOCK = 2;
	private static final int UNICODE_FUN_KEY = 0;

	private static final int ALL_DPADS_CONSUME = 12345;
	private static final int EXTENDED_TOUCHABLE_REGION_HEIGHT = 100;
	private static final String TAG = "KPTAdaptxtIME";
	//public static final String PREF_IS_THAI_LANGUAGE = "is_thai_language";

	private int cursorJumpPosition = -1;
	boolean isCursorJump = false;
	
	//private static boolean sSpeechRecognizerAvail;

	//Bug 6927: This flag is used to indicate if onKeyDown() condition fails,
	//then return without handling in onKeyUp() to maintain consistency 
	private static boolean sHardKeyHandled = false;
	private static Method sComposingMethod;


	public int mThemeMode;
	private SharedPreferences mSharedPreference;
	private Resources mResources;
	//private KPTAdaptxtDBHandler mKptDB;
	private Dialog mSuggDelDialog;

	private int mExtractedToken;
	//private boolean mChunjiinKeyboard = true;
	//private boolean mNaratgulKeyboard= false;
	private KPTQuickSettingsViewManager mKPTQuickSettingManager;

	private boolean mIsDoubleTapOnPeriodKey = false;// Is double tap on period key or not.
	private boolean mIsUnknownEditor = false;// For unknown editors(like Samsung's email editor)

	private boolean mShowApplicationCandidateBar = false;

	private CompletionInfo[] mApplicationCompletionList = null;

	/**
	 *  To know if composing is enabled/disabled
	 */
	public boolean mComposingMode = false; 
	private InputConnection mInputConnection = null;
	public MainKeyboardView mInputView;
	//public MainKeyboardView mInputViewTemp;
	public KPTCandidateViewContainer mCandidateViewContainer;
	private KPTCandidateView mCandidateView;
	public KPTKeyboardSwitcher mKeyboardSwitcher;
	private KPTSuggestion mSelectedSuggestion;
	private boolean mSuggestionBar;
	private View mExtractArea;// This variable is used to get the extract area managed by InputMethodService 
	private LinearLayout mParentInputView;


	private StringBuilder mComposing = new StringBuilder();
	private List<KPTSuggestion> mSuggList = null;
	private KPTAdaptxtTheme mTheme;
	private int mExtractedLength = 0;
	private SpannableStringBuilder mExtractedText = new SpannableStringBuilder();

	//private KPTClipboard mKPTClipboard;
	private LinearLayout mClipboardView;
	private ClipboardManager mClipManager;

	private KPTCoreEngine mCoreEngine;
	private KPTCoreServiceHandler mCoreServiceHandler;
	private boolean isCoreEngineInitialized;

	private AlertDialog mOptionsDialog;

	private String mInputLocale;
	private String mSystemLocale;
	private static KPTLanguageSwitcher mLanguageSwitcher;


	/**
	 * this flag indicated weather clipboard should be disabled or not.
	 * 
	 * According to TP #10140, clipboard should be disabled in username and password fields 
	 */
	private boolean mShouldDisableClipboard;
	private boolean mShowNumberKey;

	private boolean mShouldDisableSpaceDoubleTap = false;

	
	private boolean mPredictionOn;
	private boolean mCoreMaintenanceMode;
	private boolean mCompletionOn;
	private boolean mAutoSpace;
	private boolean mJustAddedAutoSpace;//This flag indicates whether space is added by user. 
	private boolean mAutoCorrectOn;
	private boolean mCapsLock;
	private boolean mPasswordText;
	private boolean mVibrateOn;
	private boolean mSoundOn;
	private boolean mAutoCap;
	private boolean mErrorCorrectionOn;
	KPTKeyboardSwitcher mKbdSwitcher;
	//private boolean mIsShowingHint;
	private int mCorrectionMode;
	private boolean mIsInputFinished; //bug fix  for 5772 --karthik
	//private boolean mEnableVoice = true;
	//private boolean mVoiceOnPrimary;
	private int mOrientation;
	private int mDeleteCount; //LIME
	private long mLastKeyTime; //LIME
	private boolean mInputTypeNoAutoCorrect;

	private Vibrator mVibrator;
	private AudioManager mAudioManager;
	private final float FX_VOLUME = -1.0f;
	private boolean mSilentMode;

	private String mWordSeparators;
	private String mSentenceSeparators;

	private boolean mIsKeyboardTypeQwerty;
	private  float soundVariation;
	private float mChangeVolume;
	private  int vibrationVariation;
	/**
	 * Holds the input info for setpreexistingtexttocomposingregion() method
	 */
	private KPTInputInfo mInputInfo = null;

	/**
	 * In korean language there are two primary layouts so the variable used to identify
	 * the second primary layout.when the user types any character in second primary layout
	 * we need to load first primary layout.
	 */
	public boolean isComputeInsetsDoneOnce = false;
	private CharSequence mEnteredText;//Keeps track of recently inserted text (multi-character key) for  reverting
	public int mCurrentEditorType;
	private boolean mAtxStateOn = true;

	/** 
	 * These flag indicates whether it is user input or not (to identify whether paste operation)
	 * mCharAdded - It indicates whether user entered input thru SIP 
	 * isHkb - user entered inputs thru hard keyboard
	 * mCandiateSelected - indicates whether suggestion item is selected by user 
	 */
	private boolean mCharAdded = false;
	private boolean isHKb = false;
	private boolean mCandidateSelected;
	private boolean isStartCV;//Flag to determine when Candidate View is to be shown (Only CV to be shown when HKB is pressed)
	private boolean isHideCV;	
	private boolean isBrowser;
	/* DPAD strokes counter */
	private int mCounter; /* Kashyap - 5857 - Bug Fix */

	/**
	 * These variables acts like counter for Alt & Shift keys of HKB to figure out
	 * whether state (of Alt & Shift) is Off, On or lock
	 */
	private int mhardAlt, mhardShift;
	//private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
	//This flag indicates when start of sentence (caps or not)to be considered based on editor or not.
	private boolean mRespectEditor;
	private int mLongPressState = HKB_DEFAULT_STATE; // 0 - default, 1 - longpress, 2 - Ignore

	private KPTHardKeyboardMap mHardKeyboardKeymap;
	private boolean mIsPopupChar;
	private boolean mIsSIPInput = true; //bug 6613
	private int mCurrentTheme = KPTConstants.DEFAULT_THEME;
	private boolean mIsSuggFromCandidateView;
	private boolean menableACCAfterTap;
	private boolean mAutoSpacePref;

	private boolean mShowUpdateDilaog = false; 
	private static  boolean DEBUG;
	public static Method sTextSelectionMethod;

	/**
	 * To implement s-bar inside the input view instead of default candidate view.
	 * Default candidate view is used for hard keyboard
	 * 
	 */

	private LinearLayout mSuggestuicontainer;
	public KPTCandidateViewContainer mHardKBCandidateContainer;
	private KPTCandidateView mHardKBCandidateView;

	private boolean mOrientationChanged = false;

	//-----------START glide variables
	private boolean mIsGlideEnabled = false;
	/**
	 * top from suggestion after user has lift the finger
	 */
	private KPTSuggestion  mGlideSuggestion = null;

	//END GLIDE----------------

	public ContextWrapper mContextWrapper;

	static {
	
		try {
			sComposingMethod = InputConnection.class.getMethod("setComposingRegion", int.class, int.class);	
			sTextSelectionMethod = TextView.class.getMethod("setTextIsSelectable", boolean.class);
		} catch(NoSuchMethodException nsme) {

		}		
	}

	public Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_SUGGESTIONS:

				if (!mShowApplicationCandidateBar) {
					/*if(mCandidateViewContainer != null){
						mCandidateViewContainer.updateCandidateBar();
					}*/
					//#13832. Phone keypad: Xi key isn't activated when AC is triggered with some symbols from key 1

					if(msg.arg1 != 0){
					handleMultitapInputToCore(msg.arg1);	
					}
					updateSuggestions(true);

				}
				break;
			case MSG_UPDATE_SHIFT_STATE: {
				if(mIsSIPInput) // Bug fix 6613
					updateShiftKeyState(mEditText);
				else
					updateHKBShiftKeyState(mEditText);
			}	
			break;
			case MSG_RELOAD_KEYBOARDS: {
				toggleLanguage(true, false);
				if(mCandidateViewContainer!=null){
					mCandidateViewContainer.updateCandidateBar();
					postUpdateSuggestions(50);
				}
				
			}
			break;
			case MSG_SPACE_LONGPRESS:
				if(mEditText.getmMaxLength() > 0 
						&& ((mEditText.getText().length() + 1) > mEditText.getmMaxLength())){
					return;
				}
				
				handleUserInput(KPTConstants.KEYCODE_SPACE, true);
				mHandler.sendEmptyMessage(MSG_SPACE_LONGPRESS);
				break;


			case ALL_DPADS_CONSUME:
				synchronized (this) {
					if(mInputConnection != null) { //Kashyap: Select text and Navigate Issue
						ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 
								0);
						if(extracted!= null && extracted.selectionStart != extracted.selectionEnd){
							mCounter = 0;
							return;
						}
					} else {
						mCounter = 0;
						return;
					}
					if(mHandler != null) {
						if(mHandler.hasMessages(ALL_DPADS_CONSUME)){
							mHandler.removeMessages(ALL_DPADS_CONSUME);
							mHandler.sendEmptyMessageDelayed(ALL_DPADS_CONSUME, COMPOSING_TIME_LIMIT);
							return;
						}
					}
					mCounter = 0;
					if(mCoreEngine != null) {
						if(mInputConnection != null){
							mInputConnection.beginBatchEdit();
							setPreExistingTextToComposingMode(mInputConnection);
							mInputConnection.endBatchEdit();
						}
					}
				}
				break;
			case MSG_HKB_DELETE_LONGPRESS:
				
				
				ExtractedText extracted = null;
				if(mInputConnection != null) {
					extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 
							0);
					if(extracted!= null && extracted.selectionStart != extracted.selectionEnd){
						mCounter = 0;
						return;
					}
				} else {
					mCounter = 0;
					return;
				}
				if (extracted != null && extracted.text != null && extracted.text.length() > 0){//bug No:8793 
					KPTConstants.mIsNavigationBackwar = true;
					handleDeleteOnLongPress(mIsSIPInput);
				}
				break;
			case MSG_GET_VIEW_HEIGHT:
				if (null != mInputView) {
					AdaptxtEditText editor = mInputView.getFocusedEditText();
					if (null != editor) {
						AdaptxtKeyboordVisibilityStatusListner editListner = editor.getInputViewListner();
						if(null != editListner){
							editListner.onInputviewVisbility((mInputView.getVisibility() == View.VISIBLE), getReaLHeight());

							if (null != mLanguageSwitcher) {
								editListner.analyticalData(mLanguageSwitcher.getCurrentLanguageDisplayName());
							}else{
								Log.e("KPT", "KPT failed to send anlyatical data to hike mLanguageSwitcher was null ");
							}
						}
					}else{
						//Log.e("KPT", " Strange Hanlder mInputView.getFocusedEditText() is NULL ");
					}
				}else{
					//Log.e("KPT", " Strange Hanlder mInputView is NULL ");
				}
				
				break;
			/*case MSG_SHOW_UPDATE_DIALOG:
				if (mInputView != null && mInputView.getWindowVisibility() == View.VISIBLE ) {
					// First time it will be null, incase Frame work call mutiple time onstartinputview
					// then check the visibility of dialog and display.
					if (mOptionsDialog == null || (!mOptionsDialog.isShowing())) {
						//Disabling whats new dialog as per PMG comment for #13478.
						//updateDialogInfo();
					}
				}else{
					mHandler.removeMessages(MSG_SHOW_UPDATE_DIALOG);
					mHandler.sendEmptyMessageDelayed(MSG_SHOW_UPDATE_DIALOG, 50);
				}
				break;*/

			
			}
		}


	};
	//#13832. Phone keypad: Xi key isn't activated when AC is triggered with some symbols from key 1

	private void handleMultitapInputToCore(int primaryCode) {

		if(mCoreEngine == null || mInputView == null || (mInputView.getFocusedEditText().getInputType() == NOT_AN_EDITOR && !mInputView.isShown())){
			return;
		}

		mCharAdded = true;
		KPTCorrectionInfo info = null;
		mCounter = 0;
		boolean isTextSelected = false;

		if(mHandler != null){
			mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}

		if(mInputView.getFocusedEditText() != null){
			EditText editText = mInputView.getFocusedEditText();
			editText.beginBatchEdit();

			String extracted = editText.getText().toString();//mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);

			if(extracted!= null && editText.getSelectionStart() != editText.getSelectionEnd()){
				mCharAdded = false;
				isTextSelected = true;
			}

			int phoneKeyboardState = mKeyboardSwitcher.getPhoneKeypadState();

			boolean atxOn = mAtxStateOn && !mIsPopupChar
					&& mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL
					&& !mInputView.isCurrentKeyLongPressed()  //TP 8108. checking if long pressed or not
					&& mKeyboardSwitcher != null ? phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET:false;



			//Disabling the space correction for multi tap mode
			if(!mAtxStateOn && !mJustAddedAutoSpace){
				mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
						mAutoCorrectOn, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
			}

			int beforeLen = 0;
			int afterLen = 0;
			if(!isTextSelected ) { 
				beforeLen = getExtractedTextLength();				
				info = mCoreEngine.addCharTwo((char)primaryCode,atxOn);		
				if(info == null){
					//setCurrentCharToComposingforPhone(primaryCode,phoneKeyboardState);
					if(!isHideCV){
					updateInlining();
					}
				} else {
//					deleteSurroundingText(1, 0);
//					deleteCurrentChar();
					
					if (mCurrCursorPos > 0) {
						mCurrCursorPos--;
						mTextInBuffer.deleteCharAt(mCurrCursorPos);
//						updateEditorText();//we shouldn't call update suggs before applyACOnKeyPress()
					}
					applyAutoCorrectOnKeyPress(info);
				}
				afterLen = getExtractedTextLength();
			} else {
				beforeLen = getExtractedTextLength();
//				deleteSurroundingText(1, 0);
				deleteCurrentChar(false);
				insertOrAppendBuffer(String.valueOf((char)primaryCode), true);
//				commitText(String.valueOf((char)primaryCode), 1);
				mCoreEngine.resetCoreString(); // TP 8097
				resetTextBuffer();
				mJustAddedAutoSpace = false;
				afterLen = getExtractedTextLength();
			}
			editText.endBatchEdit();
			browserTest();
//			removeExtraCharsFromCoreIfRequired(beforeLen, afterLen, extracted);
		}
		resetAccDismiss();

		updateShiftKeyState(mInputView.getFocusedEditText());
		
	}

	//Reciever to know the idle state of the phone
	

	private static String mSpecialCharList;

	AdaptxtApplication mApplication;
	private IMECutCopyPasteImplementation mCutCopyPasteInterface;
	
	public AdaptxtIME(Context context){
		////Log.e("KPt", "AdaptxtIME");
		mContext = context;
		
			
		onCreate();
		////Log.e("KPt", "AdaptxtIME mCoreEngine "+mCoreEngine);
		if (mCoreEngine == null) {
			//mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(mContext);
			if(mCoreServiceHandler != null){
				mCoreEngine = mCoreServiceHandler.getCoreInterface();
			}
			
			////Log.e("KPt", "AdaptxtIME mCoreEngine "+mCoreEngine);
			if (mCoreEngine != null) {
				initializeCore();
				mCoreEngine.setUserDictionaryWordLimit(KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
				if(mLanguageSwitcher != null){
					mLanguageSwitcher.loadLocales(mContext);
					String inputLanguage = mLanguageSwitcher.getInputLanguage();
					if(mResources == null){
						if (inputLanguage == null) {
							inputLanguage = mResources.getConfiguration().locale.toString();
						}
					}
					
					initSuggest(inputLanguage);
				}
				
			}
		}
		
		if(mCoreEngine == null){
			if(mCoreServiceHandler != null){
			mCoreServiceHandler.registerCallback(this);
			}
		}
	}
	

	public void onCreate() {
		mResources = mContext.getResources();
		mContextWrapper = new ContextThemeWrapper(mContext, R.style.AdaptxtTheme); 

		DEBUG = mResources.getBoolean(R.bool.kpt_debug);
		final Configuration conf = mResources.getConfiguration();

		mSharedPreference = PreferenceManager.getDefaultSharedPreferences(mContext);


		SharedPreferences.Editor sharedPrefEditor = mSharedPreference.edit();
		sharedPrefEditor.putBoolean(KPTConstants.PREF_KPT_IME_RUNNING, true);
		sharedPrefEditor.commit();

		mLanguageSwitcher = new KPTLanguageSwitcher(this,mContext);
		mLanguageSwitcher.loadLocales(mContext);
		mKeyboardSwitcher = new KPTKeyboardSwitcher(mContext);
		mKeyboardSwitcher.setLanguageSwitcher(mLanguageSwitcher);
		mSystemLocale = conf.locale.toString();
		mLanguageSwitcher.setSystemLocale(conf.locale);
		String inputLanguage = mLanguageSwitcher.getInputLanguage();
		
		
		if (inputLanguage == null) {
			inputLanguage = conf.locale.toString();
		}
		//1
		initSuggest(inputLanguage);
		mOrientation = conf.orientation;
		mIsHindhi = isHindhiLanugae();
		mKPTQuickSettingManager = new KPTQuickSettingsViewManager(mContextWrapper);
		mKPTQuickSettingManager.setKPTAdaptxtIME(this);

		IntentFilter inf = new IntentFilter();
		inf.addAction(Intent.ACTION_SCREEN_OFF);
		inf.addAction(Intent.ACTION_SCREEN_ON);


		mHardKeyboardKeymap = new KPTHardKeyboardMap(mContext);
		mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(mContext.getApplicationContext());
		mLanguageSwitcher.setCoreServicehandler(mCoreServiceHandler);

		mIsGlideEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true);

		updateRingerMode();

		mAppCtxtSuggEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_APP_CTXT_SUGGS, true);

		//mSharedPreference.edit().putFloat(KPTConstants.PREF_KEY_SOUND,3.0f); 
		mAppCtxtSuggEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_APP_CTXT_SUGGS, true);
		mSuggestionBar = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE,true);	
		mAutoSpacePref = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_SPACE, true);

   
		mSpecialCharList = mContext.getResources().getString(R.string.kpt_punctuations_list);
		
		 mCutCopyPasteInterface = new IMECutCopyPasteImplementation();

	}
	/*
	* #13482. Hyphen and apostrophe behaviour affected by gliding
	*   Igonore space when glide after a punctuation 
	*/
	
	private boolean isSepecialChar(CharSequence c){
		return mSpecialCharList.contains(c);
	}



	private boolean mSpaceCorrectionSuggestionsOn = true;

	private boolean mAppCtxtSuggEnabled;

	public static boolean isTrialExpired;

	/**
	 * If one of the Manx, irish, scottish gaelic and Sorani Kurdish is installed
	 *  even if trial is expired, dont consider it as trial expired
	 */
	public static boolean mIsAtleastOneOpenAdaptxtDictionaryInstalled = false;

	private int mfieldId = -1;

	private String packageName;

	private KPTCandidateViewPopup mExpandSuggView;

	private int mEditorMode;

	public static boolean mShowNumbers;

	private View mPreviewBackupContainer;

	private boolean mIsWebEditText;
	private LinearLayout keyboardViewHolder;

	private void initSuggest(String locale) {
		if (locale == null) {
			return;
		}
		mInputLocale = locale;
		if(mResources!=null){
			Configuration conf = mResources.getConfiguration();
			Locale saveLocale = conf.locale;
			conf.locale = new Locale(locale);
			mResources.updateConfiguration(conf, mResources.getDisplayMetrics());
			updateCorrectionMode();
			mWordSeparators = mResources.getString(R.string.kpt_word_separators);
			mSentenceSeparators = mResources.getString(R.string.kpt_sentence_separators);

			conf.locale = saveLocale;
			mResources.updateConfiguration(conf, mResources.getDisplayMetrics());
		}
		
		
	}

	public void onFinishInputView(boolean finishingInput) {
		if (mInputView != null) {
			mInputView.closing();
		}

		if(mCoreEngine != null) {
			mCoreEngine.resetCoreString();
		}
		//Log.e("ES", "onFinishInputView " +( mEditText != null ? mEditText.getId() : 0));
		
		removeInlinig();
		if(mOptionsDialog!=null && mOptionsDialog.isShowing())
		{
			mOptionsDialog.dismiss();
			mOptionsDialog=null;
		}
		
		if(mKPTQuickSettingManager != null){
			mKPTQuickSettingManager.dissMissQuickSettingDialog();
		}
		
		/*if(!mKPTClipboard.isHidden()){
			mKPTClipboard.hideClipboard();
		}*/

		if (mSuggDelDialog != null && mSuggDelDialog.isShowing()){
			mSuggDelDialog.dismiss();
		}
		mIsInputFinished = true;

		if(mCandidateView!=null){
			mCandidateView.dismissExpandedSuggWindow();
		}

	}

	

	public void onDestroy() {
		try{
		if (mCoreServiceHandler != null) {
			mCoreEngine = mCoreServiceHandler.getCoreInterface();
			if (mCoreEngine != null) {
				mCoreEngine.forceDestroyCore();
			}
			
			
			mCoreServiceHandler.destroyCoreService();
		}
		}catch (Exception e) {
			// TODO: handle exception
		}
		mCoreEngine = null;
		mCoreServiceHandler = null;

		SharedPreferences.Editor sharedPrefEditor = mSharedPreference.edit();
		sharedPrefEditor.putBoolean(KPTConstants.PREF_KPT_IME_RUNNING, false);


		sharedPrefEditor.commit();

	}

	public void onConfigurationChanged(Configuration conf, EditText editText) {
		// If the system locale changes and is different from the saved
		// locale (mSystemLocale), then reload the input locale list from the
		// latin ime settings (shared prefs) and reset the input locale
		// to the first one.

		setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
		if (mKPTQuickSettingManager != null) {
			mKPTQuickSettingManager.dissMissQuickSettingDialog();
		}
		/*if (mKPTClipboard != null) {
			mKPTClipboard.hideClipboard();
		}*/

		final String systemLocale = conf.locale.toString();
		if (!TextUtils.equals(systemLocale, mSystemLocale)) {
			mSystemLocale = systemLocale;
			if (mLanguageSwitcher != null) {
				mLanguageSwitcher.loadLocales(mContext);
				mLanguageSwitcher.setSystemLocale(conf.locale);
				toggleLanguage(true, true);
			} else {
				reloadKeyboards();
			}
		}
		if(mCandidateView!=null){
			mCandidateView.dismissExpandedSuggWindow();
		}

		if(mHardKBCandidateView != null){
			mHardKBCandidateView.dismissExpandedSuggWindow();
		}

		
		/*if (mInputConnection != null){
			finishComposingText(); // For voice input
		}*/

		if (mCoreEngine != null && !KPTConstants.mIsNavigationBackwar)// Bug Fix : 5803
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn, false, mCorrectionMode , mSpaceCorrectionSuggestionsOn);

		// If orientation changed while predicting, commit the change
		//SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

		if (conf.orientation != mOrientation) {
			mOrientationChanged = true;

			if( conf.orientation == Configuration.ORIENTATION_LANDSCAPE){
				mShowNumbers = false;
			}
			//TP:#20310
//			if (mInputConnection != null){
//				mInputConnection.beginBatchEdit();

				checkIfGlideShouldBeEnabled();
				
				mInputView.updatePreviewPlacerView();
				mInputView.getActionBarPlusStatusBarHeight(getActionBarPlusStatusBarHeight());

				//tp 8147 update keymaps when ever the orientation is changed
				if(mCoreEngine != null && mLanguageSwitcher != null)
					mCoreEngine.activateLanguageKeymap(mLanguageSwitcher.getCurrentComponentId(), null);
				
				if(mInputConnection!=null){
					setPreExistingTextToComposingMode(mInputConnection);
				}
					
				//setPreExistingTextToComposingMode(mInputConnection);//karthik -- fix for bug 5660
//				mInputConnection.endBatchEdit();
				updateShiftKeyState(editText);
//				updateHKBShiftKeyState(editText);
				updateSuggestions(true);
//			}
			mOrientation = conf.orientation;
			toggleLayout();
			if(mInputView != null) {
				mInputView.resetWindowOffsets();
			}

			/*if (mInputView != null ) {
				mInputView.dismissLangSelectionDialog();
			}*/
			if(mInputView.getVisibility() == View.VISIBLE){
				mHandler.sendEmptyMessageDelayed(MSG_GET_VIEW_HEIGHT, 300);
			}
		}
	/*	mKeyboardSwitcher.setKeyboardMode(KPTKeyboardSwitcher.QWERTY_MODE_TEXT, 0,
				false);*/ //#20327


		if(mSuggDelDialog!=null && mSuggDelDialog.isShowing()){
			mSuggDelDialog.dismiss();
		}
		
		
		//Bug Fix : 19970
		int savedSuggestionHeight = (int) mContext.getResources().getDimension(R.dimen.kpt_candidate_strip_height);
		if(mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			if(mSuggestuicontainer != null){
			android.view.ViewGroup.LayoutParams params1 = mSuggestuicontainer.getLayoutParams();
			params1.height = savedSuggestionHeight;
			mSuggestuicontainer.setLayoutParams(params1);
			}

			if(mCandidateViewContainer != null){
			android.view.ViewGroup.LayoutParams params2 = mCandidateViewContainer.getLayoutParams();
			if(params2 != null) {
				params2.height = savedSuggestionHeight;
				mCandidateViewContainer.setLayoutParams(params2);
			}
			}

		}else{
			if(mSuggestuicontainer != null){
			android.view.ViewGroup.LayoutParams params1 = mSuggestuicontainer.getLayoutParams();
			params1.height = savedSuggestionHeight;
			mSuggestuicontainer.setLayoutParams(params1);
			}

			if(mCandidateViewContainer != null){
			android.view.ViewGroup.LayoutParams params2 = mCandidateViewContainer.getLayoutParams();
			if(params2 != null) {
				params2.height = savedSuggestionHeight;
				mCandidateViewContainer.setLayoutParams(params2);
			}
			}
		}
		if(mCandidateView != null && mCandidateViewContainer != null){
			mCandidateViewContainer.updateCandidateBar();
			mCandidateView.invalidate();
		}
		
		
		//Bug Fix : 19967
		if(mInputView != null && mInputView.isPopupKeyboardShown()){
			mInputView.dismissMoreKeysPanel();
			//mInputView.cancelAllMessages();
			
		}
	}
	
	public void updateActionBarHeight(){
		if (mInputView != null) {
			mInputView.getActionBarPlusStatusBarHeight(getActionBarPlusStatusBarHeight());
		}
	}

	public View onCreateInputView(View holderView) {
		if(keyboardViewHolder == null) 
			return createInputView(holderView);

		return keyboardViewHolder;

	}

	public View onCreateCandidatesView() {
		// sending the RTL flag to candidate view for picking up the proper layout XML
		boolean isRTL = checkIfRTL(getInputLocale());
		if (mCandidateViewContainer == null){
			createCandidatesView(true,isRTL);
		}
	

		return mCandidateViewContainer;

	}

	public boolean isUnkownEditor() {
		return mIsUnknownEditor;
	}



	public int getUknownEditorType() {
		return mUnknownEditorType;
	}



	private void UpdateEditorInfo(EditText attribute, boolean restarting) {
		if(attribute == null){
			return;
		}
		mExpectingUpdateSelectionCounter = 0;
		mShowNumbers = false;
		if(mKeyboardSwitcher != null){
			
			mKeyboardSwitcher.mShouldLoadEngLocale = false;//make the boolean false to revert the locale when editor is changed from pwd to other editor.
			if(mLanguageSwitcher != null){
				
				mLanguageSwitcher.setShoudLoadEngLocale(mKeyboardSwitcher.mShouldLoadEngLocale);
			}
		}

		if(mCandidateViewContainer!=null && mInputView!=null ){
			// The EditorInfo might have a flag that affects fullscreen mode.
			// Note: This call should be done by InputMethodService?
			//((InputMethodService) mContext).updateFullscreenMode();
			//Based on hardkeyboard or softkeyboard initialize candidateview related UI
			/*if(mHardKBCandidateContainer != null && !(mInputView.isShown())){
				mHardKBCandidateContainer.initViews();
				mHardKBCandidateContainer.updateCandidateBar();
			}*/
			// Always initViews() should call before updateCandidateBar().
			//Because the initializations and all are doing in initViews()
			mCandidateViewContainer.initViews();
			mCandidateViewContainer.updateCandidateBar();

			//Wether to show or not the Delete icon in candicateview.
			CharSequence afterText = getTextAfterCursor(1, 0);
			CharSequence beforeText = getTextBeforeCursor(1, 0);
			if(!(afterText != null && afterText.length() != 0 && beforeText != null && beforeText.length() != 0)){
				KPTConstants.mIsNavigationBackwar = true;
			}

			// Fix for 8282
			//Call updateSpaceBarForLocale() to update the Space bar related resources while changing the theme 
			mInputView.invalidateSpaceKey();
		}

		//TP #15553. SIP truncates in landscape orientation 
		//when messaging editor is opened through Recent apps for V4.4 devices
		if(mPreviewBackupContainer != null){
			if(mInputView!=null && !mInputView.isShown()) {

				mPreviewBackupContainer.setVisibility(View.GONE);
			} else {
				mPreviewBackupContainer.setVisibility(View.VISIBLE);
			}
		}
		

		//this method calls updatelanguages which is used to check languages loaded into core.
		loadSettings();
		//updateInputConnection();
		// Fix TP 7151, including NULL check for editor info attribute passed by framework
		if(attribute != null){

			setCurrentInputTypeToNormal();
			setPreviousInputTypeToNormal();
			setGlideSuggestionToNull();
			mCoreMaintenanceMode = mSharedPreference.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
			//mHardKeyboardKeymap.setCurrentLocale(mLanguageSwitcher.getInputLanguageLocale());
			// If language is changed in background update the keyboards.
			
			if(mKeyboardSwitcher!=null && mLanguageSwitcher!=null && !mKeyboardSwitcher.getCurrentInputLocale().equals(mLanguageSwitcher.getInputLocale())) {
				toggleLanguage(true, true);
			}


			/**
			 * Set the variables to default values then modify with respect to editor type
			 */
			int variation = attribute.getInputType() & EditorInfo.TYPE_MASK_VARIATION;
			mCurrentEditorType = attribute.getInputType() & EditorInfo.TYPE_MASK_VARIATION;
			mAutoCap = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_CAPITALIZATION, true);
			mInputTypeNoAutoCorrect = true;
			mPredictionOn = false;
			mCompletionOn = false;
			mAutoSpace = true;
			mIsUnknownEditor = false;
			mPasswordText = false;
			mIsWebEditText = false;
			if(mSuggestionBar){
				isHideCV = false;
			}else{
				isHideCV = true;
			}
			mRespectEditor =true;
			mCapsLock = false;
			mEnteredText = null;

			mUnknownEditorType = -1;
			mShowApplicationCandidateBar = false;
			mShouldDisableClipboard = false;
			mShouldDisableSpaceDoubleTap = false;
			KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB = false;

			switch (attribute.getInputType() & EditorInfo.TYPE_MASK_CLASS) {

			case EditorInfo.TYPE_CLASS_NUMBER:
			case EditorInfo.TYPE_CLASS_DATETIME:

				isHideCV = true;
				mAutoSpace = false;
				mAutoCap = false;
				mRespectEditor = true;

				if(variation == EditorInfo.TYPE_NUMBER_FLAG_DECIMAL || variation == EditorInfo.TYPE_NUMBER_FLAG_SIGNED){

					mPredictionOn = false;
					isHideCV = true;
					mAutoSpace = false;
					mAutoCap = false;
					mRespectEditor =true;
				}

				break;
			case EditorInfo.TYPE_CLASS_PHONE:

				isHideCV = true;
				mAutoSpace = false;
				mAutoCap = false;
				mRespectEditor =true;
				break;

			case EditorInfo.TYPE_CLASS_TEXT:

				setTypeTextVariationAttributes(attribute);
				break;
				// TP 8036: for handling Samsung Galaxy S2's email editor
			case EditorInfo.TYPE_MASK_CLASS:

				mIsUnknownEditor = true;
				mInputTypeNoAutoCorrect = false;
				mRespectEditor =true;
				break;

			default:

				isHideCV = true; //Bug Fix: 5525 - Karthik
				mPredictionOn = false;
				if(attribute.getId() < 0 && attribute.getImeOptions() < 0){
					isBrowser = true;
				}

			}
			
			mAutoSpace =/* mAutoSpace &&*/ mAutoSpacePref;
			//	//Log.e(TAG,mAutoSpacePref+ " update " + mAutoSpace);

			if(mInputView != null) {
				mInputView.resetWindowOffsets();
			}

			String packageName = null;
			//			if(attribute != null){
			packageName =  mContext.getPackageName();
			//			}

			if(packageName != null && packageName.compareTo(KPTConstants.PACKAGE_NAME_QUICK_OFFICE) == 0){
								mShouldDisableClipboard = true; //TP 13524
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_QUICK_OFFICE;
								mIsUnknownEditor = true;
			}
			
			if(packageName != null && packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EMAIL_DEFAULT_LG)){ 
				mShouldDisableClipboard = true;
				mIsUnknownEditor = true;
				mInputTypeNoAutoCorrect = false;
				mRespectEditor =true;
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_LG_EMAIL;

			}

			if(packageName != null && packageName.compareTo(KPTConstants.PACKAGE_NAME_KINGSOFT) == 0){
				mShouldDisableClipboard = true; //TP 12042
				isHideCV = true;
				mPredictionOn = false;
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_KINGSOFT;
			}
			if(packageName != null && packageName.compareTo(KPTConstants.PACKAGE_NAME_HTC_MAIL) == 0){
				mIsUnknownEditor = true;
				mInputTypeNoAutoCorrect = false;
				mRespectEditor = true;
				mPredictionOn = true;
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL;
			}
			if( mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				mShowNumbers = false;
			}
			defaultActionsforUpdateEditorInfo(attribute, restarting);
			updateLayoutToCore();
			checkIfGlideShouldBeEnabled();

			/*
			 * for htc mail alone disable atx key 
			 */
			if(packageName != null && packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_MAIL)) {
				mAtxStateOn = false;
				if(mInputView != null){
					mInputView.mDisableATX = true;
				}
				
			}
		}
	}


	private static String SMS_APP_STRING = "sms";
	private static String MMS_APP_STRING = "mms";
	private static String MSG_APP_STRING = "message";
	private static String EMAIL_APP_STRING = "email";
	private static String MAIL_APP_STRING = "mail";
	private static String GMAIL_APP_STRING = "android.gm";


	private static String SOCIAL_APP_TYPE = "social";	
	private static String GMAIL_APP_TYPE = "gmail";
	private static String EMAIL_APP_TYPE = "email";
	private static String SMS_APP_TYPE = "sms";
	private static String DEFAULT_APP_TYPE = "default";



	private static final int FORMAL_SUGG = 1;

	private static final int INFORMAL_SUGG = 2;



	private void setTypeTextVariationAttributes(EditText attribute) {
		
		if(attribute == null){
			return;
		}
		
		mKbdSwitcher = new KPTKeyboardSwitcher(mContext, null);
		int variation = attribute.getInputType() & EditorInfo.TYPE_MASK_VARIATION;

		if(variation == htc_to_field_editor){

			variation=EditorInfo.TYPE_TEXT_VARIATION_FILTER;
		}
		mInputTypeNoAutoCorrect = false;
		mPredictionOn = true;
		mRespectEditor = true;
		mSpaceCorrectionSuggestionsOn = mAutoSpacePref;
		mShowUpdateDilaog = true;
		mShowNumberKey = false;

		switch(variation){

		case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
		case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
		case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:			 

			isHideCV = true;


			mShowUpdateDilaog = false;
			mPasswordText = true;
			mAutoSpace = false;
			mAutoCap = false;
			mRespectEditor =true;
			mPredictionOn = false;
			mShouldDisableClipboard = true;
			mShouldDisableSpaceDoubleTap = true;

			mShowNumberKey = true;
			mShowNumbers = true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME:


			mInputTypeNoAutoCorrect = true;
			mAutoSpace = false;
			mRespectEditor =true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
		case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
			mShowUpdateDilaog = false;
			mShouldDisableClipboard = true;
			mSpaceCorrectionSuggestionsOn = false;
			mPredictionOn = false;
			isHideCV = true;
			mAutoSpace = false;
			mAutoCap = false;
			mRespectEditor =false;
			mShowNumbers = true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:

			mInputTypeNoAutoCorrect = false;
			mRespectEditor =true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
			//isHideCV = false;

			mRespectEditor =true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_URI:
			mSpaceCorrectionSuggestionsOn = false;
			mPredictionOn = false;
			mAutoSpace = false;
			mAutoCap = false;
			mRespectEditor =true;
			isHideCV = true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
		case EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE:

			mInputTypeNoAutoCorrect = false;
			mRespectEditor =true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_FILTER:

			isHideCV = true;
			mPredictionOn = false;
			mAutoSpace = false;
			mAutoCap = true;
			mRespectEditor =true;
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:

			mIsWebEditText = true;
			mPredictionOn = false;
			mAutoSpace = false;
			//isHideCV = false;
			mRespectEditor =true;

			if ((attribute.getInputType() & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0
					&& (attribute.getInputType() & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
				// No multi-line allowed and auto-correct is also not allowed
				// hence disable auto-caps too. bug id: 6683
				mAutoCap = false;
			}

			String packagename = mContext.getPackageName();
			if(packagename != null
					&&(packagename.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_NOTE)||
							packagename.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_CHROME_WEB_EDITORS))){

				mIsUnknownEditor = true;
				if(packagename.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_NOTE)){
					isHideCV = true; // TP 11533
					mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_NOTE;
				}else if(packagename.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_CHROME_WEB_EDITORS)){

					mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_CHROME_WEB_EDITORS;
				}

				//isHideCV = true;
			}
			break;

		case EditorInfo.TYPE_TEXT_VARIATION_NORMAL:
			// TP 8036: If the editor type is normal and the package name is of email application, then
			// we are setting the unknwon editor flag (Samsung Galaxy Ace, YS5360, S2, Note, etc..)
			setApplicationSpecficAttributes(attribute);
			break;
		}

		// If NO_SUGGESTIONS is set, don't do prediction.
		if ((attribute.getInputType() & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
			mPredictionOn = false;
			//Log.e("VMC", "setTypeTextVariationAttributes --->12 mPrediction = false");
			mAutoSpace = false;
			isHideCV = true;
			//mSuggestionBar  = false;
		}
		//Fix for TP #7012 Hiding the suggestionbar for PERSON_NAME Fields
		else if(variation == EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME){
			isHideCV = true;
			mPredictionOn = false;
		}
		if ((attribute.getInputType() & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0 
				&& !mContext.getPackageName().equals(KPTConstants.TEAM_VIEWR_EDITOR_PACKAGE_NAME)) {
			mPredictionOn = false;
			mCompletionOn = true /*&& ((InputMethodService) mContext).isFullscreenMode()*/;
			mInputTypeNoAutoCorrect = true;

		}
		final boolean flagAutoComplete = 0 != (attribute.getInputType() & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		if (flagAutoComplete) {
			// IF the orientation is Landscape  and it should be in full screen mode, then only showing
			// frame work suggetions like contacts in message "TO" field,..
			if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE /*&& ((InputMethodService) mContext).isExtractViewShown()*/ ) {

				//isHideCV = false;
				mPredictionOn = false;
				mCompletionOn = false;
				mShowApplicationCandidateBar = true;
			}
		}
		////Log.e(TAG, "Editor PKG name ---> "+ mContext.getPackageName());
		if(!isHideCV &&  mAppCtxtSuggEnabled){			

			String appType = DEFAULT_APP_TYPE ;
			int suggType = FORMAL_SUGG;

			if(KPTConstants.SMS_APPLICATON_LIST.contains(mContext.getPackageName()) ||
					mContext.getPackageName().contains(SMS_APP_STRING)||mContext.getPackageName().contains(MMS_APP_STRING)||
					mContext.getPackageName().contains(MSG_APP_STRING)){
				appType = SMS_APP_STRING;
				suggType = INFORMAL_SUGG;

			}else if (	KPTConstants.GMAIL_APPLICATON_LIST.contains(mContext.getPackageName())||
					mContext.getPackageName().contains(GMAIL_APP_STRING)){
				appType = GMAIL_APP_TYPE;
				suggType = FORMAL_SUGG;

			}else if(KPTConstants.EMAIL_APPLICATON_LIST.contains(mContext.getPackageName()) ||mContext.getPackageName().contains(EMAIL_APP_STRING)||
					mContext.getPackageName().contains(MAIL_APP_STRING)  ){
				appType = EMAIL_APP_TYPE;
				suggType = FORMAL_SUGG;

			}else if(KPTConstants.SOCIAL_APPLICATON_LIST.contains(mContext.getPackageName())){
				appType = SOCIAL_APP_TYPE;
				suggType = FORMAL_SUGG;
			}
			if(mCoreEngine != null){
				mCoreEngine.setAppContextAppName(appType,suggType);
			}

		}


	} 

	private void setApplicationSpecficAttributes(EditText attribute) {
		if(attribute == null){
			return;
		}
		packageName = mContext.getPackageName();
		if(packageName == null){
			return;
		}		

		////Log.e("kpt","package name-->"+packageName);
		//removing suggestion bar in ploaris office editors
		if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE_SAMSUNG_STORE) ||
				packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE) || 
				packageName.contains(KPTConstants.PACKAGE_NAME_POLARIS) || 
				packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_NOTE)) {

			isHideCV = true;
			mIsUnknownEditor = true;
			mInputTypeNoAutoCorrect = false;
			mRespectEditor =true;

		} else if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EMIAL)||
				packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EMAIL_DEFAULT_LG)){ 

			mIsUnknownEditor = true;
			mInputTypeNoAutoCorrect = false;
			mRespectEditor =true;

		} else if (packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE_HTC_INBUILT)) {

			isHideCV = true;
			mInputTypeNoAutoCorrect = false;
			mRespectEditor = true;

		}else if(packageName.equalsIgnoreCase(KPTConstants.SEARCH_EDITOR)) {
			mPredictionOn = false;
			isHideCV = true;
		} 

		//Fix for TP Item - 13415
		else if(packageName.equalsIgnoreCase(KPTConstants.ONE_NOTE_APPLICATION)){
			mIsUnknownEditor = true;
		}		
		//This is Disabled as InputConnection is getting charecter as Null for this application
		else if(packageName.equalsIgnoreCase(KPTConstants.S_NOTE_APPLICATION_SAMSUNG)){
			mIsUnknownEditor = true;
			isHideCV = true;
		}

		if(mIsUnknownEditor) {
			if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EMIAL)) {
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_EMAIL;
			} else if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_NOTE)) {
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_NOTE;
			} else if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE_SAMSUNG_STORE)){
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_POLARIS_OFFICE_SAMSUNG_STORE;
			} else if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_HTC_MAIL)) {
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL;
			} else if(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EMAIL_DEFAULT_LG)) {
				mUnknownEditorType =  KPTConstants.UNKNOWN_EDITOR_TYPE_LG_EMAIL;
			}
			//This is Disabled as InputConnection is getting charecter as Null for this application
			else if(packageName.equalsIgnoreCase(KPTConstants.S_NOTE_APPLICATION_SAMSUNG)){
				mUnknownEditorType = KPTConstants.UNKNOWN_EDITOR_TYPE_S_NOTE;
			}
		} else {
			mUnknownEditorType = -1;
		}
	}

	public void onDisplayCompletions(CompletionInfo[] completions) {

		if (mShowApplicationCandidateBar && completions != null ) {
			List<KPTSuggestion> suggestions = new  ArrayList<KPTSuggestion>();
			suggestions.clear();
			int mode = 0;
			if (completions.length == 0) {
				mode = KPTCandidateView.SUGGESTIONS_BLANK_SUGGESTIONS;
			}else{
				mode = KPTCandidateView.SUGGESTIONS_DEFAULT;
				for (CompletionInfo completionInfo : completions) {
					CharSequence completionText = completionInfo.getText();
					if (completionText != null){
						String completionInfoText = completionText.toString();
						KPTSuggestion kptSuggestion = new KPTSuggestion();
						kptSuggestion.setsuggestionType(KPTSuggestion.KPT_SUGGS_TYPE_WORD);
						kptSuggestion.setsuggestionString(completionInfoText);
						kptSuggestion.setsuggestionLength(completionInfoText.length());
						suggestions.add(kptSuggestion);						
					}
				}
				mApplicationCompletionList = completions;
			}

			if (mCandidateViewContainer != null && mCandidateView != null) {
				mCandidateView.setFrameWorkSuggestionState(true);
				mCandidateView.setSuggestions(suggestions, false, false, mode);

			}
		}
	}

	/**
	 * Method used to handle all the actions need to be performed after identifying the editor type 
	 * in updateeditorinfo method
	 */
	private void defaultActionsforUpdateEditorInfo(EditText attribute, boolean restarting){
		if(attribute == null){
			return;
		}
		packageName = null;//attribute.packageName;
		//As of now it is calling only from updateEditorInfo() with this null check.But later if we trigger this 
		//from other places, it checks.
		boolean currentPrivateMode = mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false);
		mIsHindhi = isHindhiLanugae();

		updateCorrectionMode();

		/*if(mCoreEngine !=null){
			mCoreEngine.setPunctuationPrediction(mSharedPreference.getBoolean(KPTConstants.PREF_PUNCTUTION_PREDICTION, true));
		}*/

		boolean dispSuggBar = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE, true);

		if(mPredictionOn && dispSuggBar) {
			mAtxStateOn = mSharedPreference.getBoolean(KPTConstants.PREF_PHONE_ATX_STATE, true);
			if(mInputView != null){
				
				mInputView.mDisableATX = false;
			}
		} else {
			mAtxStateOn = false;
			if(mInputView != null){
				
				mInputView.mDisableATX = true;
			}

		}

		// AtxState false for standard keyboard
		if (mIsKeyboardTypeQwerty) {
			mAtxStateOn = false;
		} 

		//call corresponding keyboard based on the mode
		mEditorMode = getKeyboardMode(attribute);
		mKeyboardSwitcher.setKeyboardMode(mEditorMode,
				attribute.getImeOptions(), true);

		if(mCoreEngine != null){
			
			
			if(isHideCV || !mSuggestionBar || mPasswordText == true || mShowApplicationCandidateBar ){
					mCoreEngine.setBlackListed(true);

			}else {
				mCoreEngine.setBlackListed(false);
			}
		}

		if(mInputView != null){
			mInputView.closing();
			//Changed the state to false to fix Bug# 5895 
			mInputView.setProximityCorrectionEnabled(false);
		}

		mComposing.setLength(0);
		mDeleteCount = 0;
		mJustAddedAutoSpace = false;
		//Bug fix #12294
		//if(!mIsThaiLanguage){
		setPrivateMode(true);
		addPreExistingTextToCore(restarting);
		setPrivateMode(currentPrivateMode);
		//}

		if (mInputConnection != null) { 
			mInputConnection.beginBatchEdit();
			setPreExistingTextToComposingMode(mInputConnection);
			mInputConnection.endBatchEdit();
		}

		if(attribute.getId() != mfieldId ){
			mfieldId = attribute.getId();
		}

		//change
		/*if (((!isHideCV && mSuggestionBar) && (attribute.getInputType() != EditorInfo.TYPE_NULL))
				&& (mCandidateView!= null && !mCandidateView.isShown()) ){

			setSuggestionStripShown(true);
		} else {
			setSuggestionStripShown(false);
		}*/


		if(mIsSIPInput || (mInputView !=null && mInputView.isShown())){
			updateShiftKeyState(attribute);
		}
		else{
			updateHKBShiftKeyState(attribute);				
		}

		updateSuggestions(true);
	}

	/**
	 * get the keyboard mode depending on the editor type.
	 * 
	 * @param editorInfo
	 * @return
	 */
	int getKeyboardMode(EditText attribute) {
		//default value
		int mode = KPTKeyboardSwitcher.QWERTY_MODE_TEXT;

		if (attribute == null) {
			return mode;
		}
		if (mInputView == null) {
			return mode;
		}
		////Log.e(TAG, "VARAITION -----------------> "+(attribute.inputType & EditorInfo.TYPE_MASK_CLASS));
		switch (attribute.getInputType() & EditorInfo.TYPE_MASK_CLASS) {
		case EditorInfo.TYPE_CLASS_NUMBER:
		case EditorInfo.TYPE_CLASS_DATETIME: {
			if (mIsKeyboardTypeQwerty){
				mode = KPTKeyboardSwitcher.QWERTY_MODE_SYMBOLS;
			} else {
				mode = KPTKeyboardSwitcher.PHONE_MODE_SYMBOLS;
			}
			break;
		}
		case EditorInfo.TYPE_CLASS_PHONE: {
			mode = KPTKeyboardSwitcher.QWERTY_MODE_PHONE;
			break;
		}
		case EditorInfo.TYPE_CLASS_TEXT: {
			if (mIsKeyboardTypeQwerty) {
				mode = KPTKeyboardSwitcher.QWERTY_MODE_TEXT;
			} else { 
				mode = KPTKeyboardSwitcher.PHONE_MODE_TEXT;
			}

			int variation = attribute.getInputType() & EditorInfo.TYPE_MASK_VARIATION;
			final boolean flagNoSuggestions = 0 != (attribute.getInputType() & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			if(flagNoSuggestions && variation == 0 &&  mContext.getPackageName().equals(KPTConstants.SEARCH_EDITOR) ){
				variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
			}
			boolean isSearchEditor =mContext.getPackageName() != null
					&& mContext.getPackageName()
					.equalsIgnoreCase(KPTConstants.SEARCH_EDITOR);

			if(variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL && isSearchEditor) {
				variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
			} 
			switch (variation) {
			case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS: 
			case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS: {
				if (mIsKeyboardTypeQwerty) {
					mode = KPTKeyboardSwitcher.QWERTY_MODE_EMAIL;
				} else{
					mode = KPTKeyboardSwitcher.PHONE_MODE_EMAIL;
				}
				break;
			}
			case EditorInfo.TYPE_TEXT_VARIATION_URI: {
				if (mIsKeyboardTypeQwerty) {
					mode = KPTKeyboardSwitcher.QWERTY_MODE_URL;
					
				} else {
					mode = KPTKeyboardSwitcher.PHONE_MODE_URL;
				}
				mCurrentEditorType = EditorInfo.TYPE_TEXT_VARIATION_URI;
				break;
			}
			case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
			case EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE: {
				if (mIsKeyboardTypeQwerty) {
					mode = KPTKeyboardSwitcher.QWERTY_MODE_IM;
				} else {
					mode = KPTKeyboardSwitcher.PHONE_MODE_TEXT;
				}
				break;
			}
			case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
			case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
				if (mIsKeyboardTypeQwerty) {
					mode = KPTKeyboardSwitcher.QWERTY_MODE_PASSWORD;
				} else {
					mode = KPTKeyboardSwitcher.PHONE_MODE_PASSWORD;
				}
				break;
			case EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT: {
				if (mIsKeyboardTypeQwerty) {
					mode = KPTKeyboardSwitcher.QWERTY_MODE_WEB;
				} else {
					mode = KPTKeyboardSwitcher.PHONE_MODE_TEXT;
				}
				break;
			}
			}
			break;
		}// ends EditorInfo.TYPE_CLASS_TEXT
		default: {
			if (mIsKeyboardTypeQwerty) {
				mode = KPTKeyboardSwitcher.QWERTY_MODE_TEXT;
			} else { 
				mode = KPTKeyboardSwitcher.PHONE_MODE_TEXT;
			}
		}
		}
		return mode;

	}

	/*private void setPhonekeyboardType(boolean bNaratguel, boolean bChunjiin, boolean bSky) {		
		//mNaratgulKeyboard = bNaratguel;
		//mChunjiinKeyboard = bChunjiin;
	}*/


	private void resetAccDismiss(){
		/*if (mCandidateViewContainer != null && mCandidateViewContainer.isShown()) {
			//mCandidateViewContainer.resetAccDismiss();
		}
		if (mHardKBCandidateContainer != null && mHardKBCandidateContainer.isShown()) {
			//mHardKBCandidateContainer.resetAccDismiss();
		}*/
	}

	/**
	 * This function is used to in-line the text. It is called on every user input like
	 * Key press on SIP or HKB, candidate selection, cut/copy/paste
	 * @param ic
	 */
	//Need to remove this argument. Because we are having mInputConnection as global variable.
	private synchronized void setPreExistingTextToComposingMode(InputConnection ic) {

		if (mCoreEngine == null || mCoreMaintenanceMode /*|| mInputConnection == null*/) { 
			return;
		}

		//Kashyap: Bug fix - 5998, 6007. Selected Text is getting Deleted Upon Changing the Input Method
		AdaptxtEditText extText = mInputView.getFocusedEditText();//mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if(extText == null || extText.getSelectionStart() != extText.getSelectionEnd()){
			return;
		}
		mComposing.setLength(0);

		/*if(mCounter == 0){*/
		boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn && !mIsPopupChar;
		if(!atxOn){
			finishComposingText();//Kashyap: 5751 bug fix
		}
		if ((isPredictionOn() || atxOn)/* && mCheckPrediction*/) { //fix for 7627
			int curPos = 0;
			String[] curWord = null;
			//CurrentWord cuurent = mCoreEngine.getCurrentWord();
			if(mInputInfo != null) {
				curWord = mInputInfo.currWordArray;
			} else {
				CurrentWord cuurent = mCoreEngine.getCurrentWord();
				if(cuurent!= null){
					curWord = cuurent.currWord;
				}
			}

			String currentWord = null;
			//SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			//If 12K predictive mode, then get length of string to compose
			//from composing string length
			String compString = mCoreEngine.getComposingString();
			if(atxOn && compString!= null 
					&& !compString.equalsIgnoreCase("")
					&& !mInputView.isPopupKeyboardShown()
					&& mKeyboardSwitcher.getPhoneKeypadState() == 
					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) { //bug 7446 //Bug 7371, 7374 

				currentWord = mCoreEngine.getPredictiveWord();
			} else {
				if(curWord != null && curWord[0] != null)
					currentWord = curWord[0];
			}


			/*boolean isLicenseExpired= mSharedPreference.getBoolean(KPTConstants.PREF_CORE_LICENSE_EXPIRED, false);
				if(isLicenseExpired) { // fix for TP item 6931
					CharSequence text = extText.text; //Bug 7456
					if(text != null) {
						CharSequence textBeforeCursor = getTextBeforeCursor(text.length(), 0);
						if(textBeforeCursor != null){
							curPos = textBeforeCursor.length();
						}
					}
				}
				else {*/
			curPos = mCoreEngine.getAbsoluteCaretPosition();
			//}

			/**
			 * For each user input (add or remove), following steps are performed
			 * 1) First update the char to editor (it is in handleruserinput,handlebackspace etc)
			 * 2) Requesting core for current word
			 * 3) Remove the text before cursor
			 * 4) Get the text before cursor from core and update this text to editor with in-lining
			 */
			if (currentWord!= null && currentWord.length() > 0) {
				mComposing.append(currentWord);
				if(sComposingMethod != null) {
					//								mInputConnection.beginBatchEdit();
					atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn;
					if(!atxOn || (atxOn && mCoreEngine.getComposingStringLength() != 0)){
						finishComposingText();
						//If any char is added when ATX is on then set cursor position - 1 since
						//in atx on, first composing region is set before entering text, otherwise
						//text is entered first then composing is set

						/*if(atxOn && isCharAdded()) 
										mInputConnection.setComposingRegion(curPos - currentWord.length(), curPos-1);
									else {*/
						//if(!mIsUnknownEditor){
							//mInputConnection.setComposingRegion(curPos - currentWord.length(), curPos);
							//setComposingRegion(curPos - currentWord.length(), curPos);
						//}else{
							//deleteSurroundingText(currentWord.length(), 0);
							//setComposingText(currentWord,1);
						//}
						//}
					}
					if(isCharAdded() && atxOn){
						if(mIsUnknownEditor) {
							deleteSurroundingText(currentWord.length(), 0);
						}
						setComposingText(currentWord,1);
					}
				} else {
					finishComposingText();
					deleteSurroundingText(currentWord.length(), 0);
					setComposingText(currentWord, 1);
				}
			}

			//clear inlining when atx state is false
			if(!mIsKeyboardTypeQwerty && !mAtxStateOn) {
				finishComposingText();
			}

			// 	#10962. Text editor slider deactivated & out of sync --- Bhavani

			/*if(curPos > 0){
						mInputConnection.setSelection(curPos, curPos);
					}*/
		}
		//}
	}

	private boolean setComposingRegion(int i, int curPos) {
		mComposingMode = true;
		if (null != mEditText) {
			String textInEditor = mEditText.getText().toString();
			int textLenghtInEditor = textInEditor.length();
			if(i > 0 && i <= textLenghtInEditor && curPos <= textLenghtInEditor){
				SpannableString spannableString = processUnderline(i, curPos, textInEditor);
				 
				mEditText.setText(spannableString,true);
				mEditText.setSelection(curPos,true);
			}else{
				mCoreEngine.syncCoreBuffer(textInEditor, textLenghtInEditor);
				int editorCursorPos = mEditText.getSelectionStart();
				mCoreEngine.setAbsoluteCaretPosition(editorCursorPos);
				SpannableString spannableString = new SpannableString(textInEditor);
				 
				mEditText.setText(spannableString,true);
				mEditText.setSelection(editorCursorPos,true);
			}
			
			
		}
		return /*mInputConnection.setComposingRegion(i, curPos)*/ true;
	}
	//#13832. Phone keypad: Xi key isn't activated when AC is triggered with some symbols from key 1

	private void handleReplaceInMultitap(char primaryCode, boolean isSIP) {
		if(mCoreEngine == null){
			return;
		}

		EditText editorInfo = mInputView.getFocusedEditText();//((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (editorInfo == null) {
			return;
		}

		if (mInputView == null) {
			return;
		}

		if(editorInfo.getInputType() == NOT_AN_EDITOR && !mInputView.isShown()){
			return;
		}
		//Disabling space correction in multitap mode
		if(!mJustAddedAutoSpace){
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
					mAutoCorrectOn, mCorrectionMode,false);
		}
		mCounter = 0;
		if(!isStartOfSentence() && !mCapsLock && !isHideCV) {
			primaryCode = Character.toLowerCase(primaryCode);
		}
		if(mHandler != null){
			mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}
		boolean isTextSelected = false;
		if(editorInfo != null){
			//editorInfo.beginBatchEdit();
			EditText extracted = mInputView.getFocusedEditText();/*mInputConnection.getExtractedText(new ExtractedTextRequest(), 
					0);*/
			if(extracted != null && extracted.getSelectionStart() != extracted.getSelectionEnd()){
				mCharAdded = false;
				isTextSelected = true; //Bug Fix: 5784 - Kashyap
			}

			if(!isTextSelected) { //Bug Fix: 5784 - Kashyap
				boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
						&& mKeyboardSwitcher.getPhoneKeypadState() == 
						KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;

				if(!atxOn || primaryCode == KPTConstants.KEYCODE_SPACE)
					finishComposingText();


				if((mJustAddedAutoSpace) && (!atxOn || mIsDoubleTapOnPeriodKey)) { // fix for 7690
					handlePunctuationKey(extracted,primaryCode); //bug 7596
				} else {
					
//					deleteCurrentChar();
					if (mCurrCursorPos > 0) {
						mCurrCursorPos--;
						mTextInBuffer.deleteCharAt(mCurrCursorPos);
						if(isHideCV){
						
						
							if(mEditText.getSelectionEnd() < mEditText.getText().length()){
							mEditText.getText().delete(mCurrCursorPos-1, mCurrCursorPos);
							}else{
								mEditText.getText().delete(mCurrCursorPos, mCurrCursorPos+1);
							}
						}
//						updateEditorText();
					}
					
					//deleteSurroundingText(1, 0);
					if(!atxOn || primaryCode == KPTConstants.KEYCODE_SPACE){
						//commitText(String.valueOf((char)primaryCode), 1);*/
						if(mKeyboardSwitcher != null){
							
							int phoneKeyboardState = mKeyboardSwitcher.getPhoneKeypadState();
							setCurrentCharToComposingforPhone(primaryCode,phoneKeyboardState);
						}
					}
				}
			} else {
				deleteCurrentChar(false);
				String inputStr = String.valueOf((char)primaryCode);
				insertOrAppendBuffer(inputStr, true);
				
				/*deleteSurroundingText(1, 0);
				commitText(String.valueOf((char)primaryCode), 1);*/
				mJustAddedAutoSpace = false;
			}
			//editorInfo.endBatchEdit();
			browserTest();
		}

		resetAccDismiss();

		if(isSIP)
			updateShiftKeyState(mEditText);
		else 
			updateHKBShiftKeyState(mEditText);

		mMultitapCharInMessage = primaryCode; 
		//updateSuggestions(false);
		postUpdateSuggestions(MULTITAP_SUGGESTION_INTERVAL,primaryCode);

		//Reverting back space correction
		if(!mJustAddedAutoSpace){
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
					mAutoCorrectOn, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
		}
	}


	private void handleReplace(int primaryCode, boolean isSIP){
		if(mCoreEngine == null){
			return;
		}

		AdaptxtEditText editorInfo = mInputView.getFocusedEditText();//((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (editorInfo == null) {
			return;
		}

		if (mInputView == null) {
			return;
		}

		if(editorInfo.getInputType() == NOT_AN_EDITOR && !mInputView.isShown()){
			return;
		}
		//Disabling space correction in multitap mode
		if(!mJustAddedAutoSpace){
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
					mAutoCorrectOn, mCorrectionMode,false);
		}
		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}
		boolean isTextSelected = false;
		if(editorInfo != null){
			editorInfo.beginBatchEdit();
			EditText extracted = mInputView.getFocusedEditText();/*mInputConnection.getExtractedText(new ExtractedTextRequest(), 
					0);*/
			if(extracted != null && extracted.getSelectionStart() != extracted.getSelectionEnd()){
				mCharAdded = false;
				isTextSelected = true; //Bug Fix: 5784 - Kashyap
			}

			if(!isTextSelected) { //Bug Fix: 5784 - Kashyap
				boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
						&& mKeyboardSwitcher.getPhoneKeypadState() == 
						KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;

				if(!atxOn || primaryCode == KPTConstants.KEYCODE_SPACE)
					finishComposingText();

				/*mCharAdded =*/ mCoreEngine.removeChar(true); //Bug 11735 - vamsee
				if(getTextBeforeCursor(1, 0) != null){
					mCoreEngine.addChar((char)primaryCode,
							getTextBeforeCursor(1, 0).toString().equalsIgnoreCase(
									"" + (char)KPTConstants.KEYCODE_SPACE)? true : false , 
											mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState(),
											atxOn);
				} else {
					mCoreEngine.addChar((char)primaryCode,
							false , mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState(),
							atxOn);
				}
				if((mJustAddedAutoSpace) && (!atxOn || mIsDoubleTapOnPeriodKey)) { // fix for 7690
					handlePunctuationKey(editorInfo,primaryCode); //bug 7596
				} else {
//					deleteSurroundingText(1, 0);
					deleteCurrentChar(true);
					if(!atxOn || primaryCode == KPTConstants.KEYCODE_SPACE)
//						commitText(String.valueOf((char)primaryCode), 1);
						insertOrAppendBuffer(String.valueOf((char)primaryCode), true);
				}
			} else {
				//15854	12-key mode, chrome url/serach field: problematic letter entry
				//if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_CHROME_WEB_EDITORS){
//				deleteSurroundingText(1, 0);
				deleteCurrentChar(false);
				//}
				insertOrAppendBuffer(String.valueOf((char)primaryCode), true);
//				commitText(String.valueOf((char)primaryCode), 1);
				mJustAddedAutoSpace = false;
			}
			editorInfo.endBatchEdit();
			browserTest();
		}

		resetAccDismiss();

		if(isSIP)
			updateShiftKeyState(mInputView.getFocusedEditText());
		else 
			updateHKBShiftKeyState(mInputView.getFocusedEditText());

		postUpdateSuggestions(MULTITAP_SUGGESTION_INTERVAL);

		//Reverting back space correction
		if(!mJustAddedAutoSpace){
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
					mAutoCorrectOn, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
		}
	}

	public void createCandidatesView(boolean fromIMF,boolean isRTL) {
		// pick up the layout XML according to Adaptxt keyboard locale
		Configuration conf = mResources.getConfiguration();
		Locale saveLocale = conf.locale;
		conf.locale = getInputLocale();
		mResources.updateConfiguration(conf, null);

		mCandidateViewContainer=(KPTCandidateViewContainer) View.inflate(mContext, R.layout.kpt_candidates, null);

		mHardKBCandidateContainer = (KPTCandidateViewContainer) View.inflate(mContext, R.layout.kpt_candidates, null);	

		mCandidateView = (KPTCandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
		mCandidateViewContainer.setRTL(isRTL);
		mCandidateView.setRTL(isRTL);
		mCandidateView.setService(this);
		mCandidateViewContainer.setTheme(mTheme);
		mCandidateView.setTheme(mTheme);
		//Make sure the setTheme() should be called before initViews()
		mCandidateViewContainer.initViews();
		//((InputMethodService) mContext).setCandidatesViewShown(false);


		mHardKBCandidateView = (KPTCandidateView) mHardKBCandidateContainer
				.findViewById(R.id.candidates);
		mHardKBCandidateView.setRTL(isRTL);
		//mHardKBCandidateView.setService(this);
		mHardKBCandidateContainer.setTheme(mTheme);
		mHardKBCandidateView.setTheme(mTheme);
		mHardKBCandidateContainer.initViews();

		mExpandSuggView = (KPTCandidateViewPopup) mParentInputView.findViewById(R.id.expandSuggView);
		mCandidateView.setExpandView(mExpandSuggView);

		if (mSuggestuicontainer != null) {
			mSuggestuicontainer.removeAllViews();
			mSuggestuicontainer.addView(mCandidateViewContainer);
		}
		//setSuggestionStripShown(false);
		//setCandidatesView(mCandidateViewContainer);
		conf.locale = saveLocale;
		mResources.updateConfiguration(conf, null);
		mSharedPreference.registerOnSharedPreferenceChangeListener(this);
	}


	public void setSuggestionStripShown(boolean shown) {

		/*
		 * Set visibility for default S-bar if it is a hard keyboard else for soft input 
		 * set visibility for suggestion bar in input view.
		 * 
		 */

		if(!mInputView.isShown()){
			//((InputMethodService) mContext).setCandidatesViewShown(shown);
		}else{                                                    
			if(mSuggestuicontainer!=null){
				mSuggestuicontainer.setVisibility(shown ? View.VISIBLE : View.GONE);
				/*if (((InputMethodService) mContext).isFullscreenMode()) {
					mSuggestuicontainer.setVisibility(
							shown ? View.VISIBLE : View.GONE);
				} else {
					mSuggestuicontainer.setVisibility(
							shown ? View.VISIBLE : View.INVISIBLE);
				}*/
			}
			//((InputMethodService) mContext).setCandidatesViewShown(false);
		}

		if(mInputView != null && mInputView.getFocusedEditText() != null 
				&& mInputView.getFocusedEditText().isFocusable()) {
			mInputView.updatePreviewPlacerView();
		}
	}
	
	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		//mCoreEngine = coreEngine;
		
		onStartInputView(mEditText, true);
		/*mInputView.invalidate();
		mCandidateView.invalidate();*/
		checkLanguages();
	}
	
	public void checkLanguages(){
		if(mCoreEngine != null && mLanguageSwitcher != null){
			boolean alreadyChecked = mSharedPreference.getBoolean(KPTConstants.PREFS_UNSUPPORTED_LANG_STATUS, false);
			//Log.e("VMC", "ALREADY CHECKED ----> "+alreadyChecked);
			if(!(alreadyChecked)){
				mLanguageSwitcher.getInstalledDicts(mContext);
			}else{
				int componentID = mSharedPreference.getInt(KPTConstants.KPT_CHANGE_LANG_ID, -1);
				if(componentID > 0){
					mLanguageSwitcher.updatePriorityInCore(componentID);
					//if(!mKeyboardSwitcher.getCurrentInputLocale().equals(mLanguageSwitcher.getInputLocale())) {
						toggleLanguage(true, false);
					//}

					
				}
			}
		}
	}


	public LinearLayout createInputView(View holderView) {

		mTheme = new KPTAdaptxtTheme(mContext, 0);
		if (mCoreEngine == null) {
			//mCoreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(mContext);
			if(mCoreServiceHandler != null){
				mCoreEngine = mCoreServiceHandler.getCoreInterface();
			}
			
			//Log.e("KPt", "createInputView mCoreEngine "+mCoreEngine);
			if (mCoreEngine != null) {
				initializeCore();
				mCoreEngine.setUserDictionaryWordLimit(KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
				if(mLanguageSwitcher != null){
					mLanguageSwitcher.loadLocales(mContext);
					String inputLanguage = mLanguageSwitcher.getInputLanguage();
					if (inputLanguage == null) {
						inputLanguage = mResources.getConfiguration().locale.toString();
					}
					initSuggest(inputLanguage);
				}
				
			}
		}
		
		if(mCoreEngine == null && mCoreServiceHandler != null){
			mCoreServiceHandler.registerCallback(this);
		}

		keyboardViewHolder = (LinearLayout) holderView; 

		mParentInputView =  (android.widget.LinearLayout) ((Activity) mContext).getLayoutInflater().inflate(R.layout.kpt_input, null);

		mPreviewBackupContainer = (View)mParentInputView.findViewById(R.id.key_preview_backing);
		//mPreviewBackupContainer.setBackgroundColor(Color.argb(100, 155, 00, 155));

		mInputView = (MainKeyboardView) mParentInputView.findViewById(R.id.keyboardView);
		mInputView.setVisibility(View.VISIBLE);
		mSuggestuicontainer =(LinearLayout)mParentInputView.findViewById(R.id.suggestion_container);

		mClipboardView = (LinearLayout) mParentInputView
				.findViewById(R.id.childView);
		mClipboardView.setVisibility(View.GONE);
		//mKPTClipboard.setView(mClipboardView);
		//mKPTClipboard.setKPTAdaptxtIME(this);

		mInputView.setIMEHandler(this);
		mKeyboardSwitcher.setInputView(mInputView);
		mKeyboardSwitcher.setTheme(mTheme);
		mKeyboardSwitcher.makeKeyboards();
		mInputView.setOnKeyboardActionListener(this);
		mInputView.setTheme(mTheme);
		//mKPTClipboard.setTheme(mTheme);
		mInputView.setLanguageSwitcher(mLanguageSwitcher);
		mInputView.setKeyboardSwitcher(mKeyboardSwitcher);	
		mKeyboardSwitcher.setKeyboardMode(KPTKeyboardSwitcher.QWERTY_MODE_TEXT, 0,
				false);

		//createGlideView();
//		mInputView.getActionBarPlusStatusBarHeight(getActionBarPlusStatusBarHeight());
		//adding the complete keyboard view to client's view
		keyboardViewHolder.addView(mParentInputView);

		return keyboardViewHolder;

	}
	//Returning the Sum of ActionBar and StatusBar
	public int getActionBarPlusStatusBarHeight(){


		int statusBarHeight = (int) (25 * mContext.getResources().getDisplayMetrics().density);

		Rect r = new Rect();
		Window w = ((Activity) mContext).getWindow();
		w.getDecorView().getWindowVisibleDisplayFrame(r);
		int viewTop = ((Activity) mContext).getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();

		//Remove action bar height for normal Activity or if ActionbarActivity is not extended 
		try {
			Method method  = mContext.getClass().getMethod("getSupportActionBar", null);
			if(method == null){
				statusBarHeight = 0;
			}
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
			statusBarHeight = 0;
		}


		return viewTop + statusBarHeight;
	}
	
	private void createGlideView() {

		KPTGlideKBShortcuts.getInstance().setService(this,mContext);

		KPTCustomGesturePreferences customGesturePreference = KPTCustomGesturePreferences.getCustomGesturePreferences();
		customGesturePreference.initializePreferences(mContext);
		customGesturePreference.loadDefaultPreferences();
	}

	/**
	 * Initializes core engine framework. If it fails for the first time, it destroys the core
	 * object and then tries again. IF it fails even after destroying core, then the internal files 
	 * are deleted and retried again.
	 * 
	 */
	private void initializeCore(){
		if (mCoreEngine == null) {
			return;
		}
		isCoreEngineInitialized = mCoreEngine.initializeCore(mContext);
		//Log.e("KPT", "isCoreEngineInitialized "+isCoreEngineInitialized);
		if(!isCoreEngineInitialized) {
			// Core Engine not initialized destroy the core object and retry again
			mCoreEngine.forceDestroyCore();
			isCoreEngineInitialized =  mCoreEngine.initializeCore(mContext);
			//Even after retry, initialization failed then clear the internal files and retry again
			if(!isCoreEngineInitialized) {

				try {
					String filePath = mContext.getFilesDir().getAbsolutePath()+"/Profile";
					File file = new File(filePath);
					recusriveDelete(file);
					file.delete();

					mCoreEngine.prepareCoreFilesForRetry(mContext.getFilesDir().getAbsolutePath(), mContext.getAssets());
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}catch(Exception e) {
					//e.printStackTrace();
				}

				isCoreEngineInitialized =  mCoreEngine.initializeCore(mContext);
				//After all possiblie retries core engine still not initialized, which is a rare use case
				if(!isCoreEngineInitialized) {
					//Log.e("MS","ADAPTXT IME ***** CORE INITIALIZATION FAILED EVEN AFTER 3 ATTEMPTS, FORCE DESTROY CORE AND" +
						//	"AFTER CLEARING INTERNAL FILES ****** ");
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
					f.delete();
				}
			} else {
				f.delete();
			}
		}
	}

	/**
	 * This method will be called from QuickSettingView Manager to update suggestions after closing the QAView.
	 */
	public void updateQuickAccessSuggestions(){
		updateSuggestions(true);
	}

	public boolean isClipBoardHasText() {
		if (mClipManager == null) {
			mClipManager = (ClipboardManager) mContext.getSystemService(Service.CLIPBOARD_SERVICE);
		}
		return mClipManager.hasText();
	}

	public boolean isAnySelectedText() {
		if (mInputConnection == null) {
			return false;
		}
		ExtractedText extracted = mInputConnection.getExtractedText(
				new ExtractedTextRequest(), 0);
		if (extracted != null
				&& extracted.selectionStart != extracted.selectionEnd)
			return true;
		else
			return false;
	}

	// For Cut Operation
	public void clipboardCut() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if (mInputView.getFocusedEditText() == null) {
			return;
		}
		//		ExtractedText extracted = null;
		EditText extracted = mInputView.getFocusedEditText();/*mInputConnection.getExtractedText(
				new ExtractedTextRequest(), 0);*/
		if (extracted == null) {
			return;
		}
		if (extracted.getSelectionStart() != extracted.getSelectionEnd()) {
			String selectedText = null;
			String toBeReplaceText = "";
			extracted.beginBatchEdit();
			CharSequence extractedText = extracted.getText();
			if (mCoreEngine != null && extractedText != null) {
				if (extracted.getSelectionStart() > extracted.getSelectionEnd()) {
					selectedText = extracted.getText().toString().substring(
							extracted.getSelectionStart(), extracted.getSelectionEnd());
					mCoreEngine.replaceString(extracted.getSelectionEnd(),
							extracted.getSelectionStart() - 1, toBeReplaceText);
				} else {
					selectedText = extracted.getText().toString().substring(
							extracted.getSelectionStart(), extracted.getSelectionEnd());
					mCoreEngine.replaceString(extracted.getSelectionStart(),
							extracted.getSelectionEnd() - 1, toBeReplaceText);
				}
			}

			if (selectedText != null) {
				mClipManager.setText(selectedText);
			}

			setCurrentInputTypeToNormal();

			commitText("", 1);
			//setPreExistingTextToComposingMode(mInputConnection);
			extracted.endBatchEdit();
			updateShiftKeyState(extracted);
			updateSuggestions(false);
		}
	}

	public void clipboardSelectWord() {
		if(mInputConnection != null) {
			ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			if(extracted!= null && extracted.selectionStart == extracted.selectionEnd) {
				int startIndex = -1;
				int endIndex = -1;

				CurrentWord currentWord = mCoreEngine.getCurrentWord();
				if(currentWord != null && currentWord.currWord[0] != null && currentWord.currWord[1] != null){
					startIndex = mCoreEngine.getAbsoluteCaretPosition() - currentWord.currWord[0].length();
					endIndex = mCoreEngine.getAbsoluteCaretPosition() + currentWord.currWord[1].length();

					mInputConnection.setSelection(startIndex, endIndex);
				}
			}
		}
	}

	/*public void clipboardUp() {


		// Fix for TP Item - 13639 , 9776
		InputConnection inputConn = ((InputMethodService) mContext).getCurrentInputConnection();
		long l = SystemClock.uptimeMillis();
		if (mKPTClipboard != null) {
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 0, 59, 0, 0, 0, 0, 6));
			}
			((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 1, 59, 0, 0, 0, 0, 6));
			}	
		}

	}*/

	public void clipboardBeginningOfMessage() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if(mInputConnection != null) {
			mInputConnection.setSelection(0, 0);
		}
	}

	public void clipboardEndOfMessage() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if(mInputConnection != null) {
			ExtractedText extracted = null;
			extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			if(extracted != null) {
				CharSequence text = extracted.text;
				if(text != null) {
					int len = text.toString().length();
					mInputConnection.setSelection(len, len);
				}
			}
		}
	}

	public void hideKeyboard() {
		hideWindow();
	}

	public void clipboardDelete(boolean longPress) {
		//mExpectingUpdateSelectionCounter ++;
		if (longPress) {
			handleDeleteOnLongPress(true);
		} else
			handleBackspace(true);
	}

	/*public void showClipboard() {
		if(!mIsUnknownEditor && !mShouldDisableClipboard)  {
			boolean isEnableSelection = true;
			if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_QUICK_OFFICE){
				isEnableSelection = false;
			}
			mKPTClipboard.showClipboard(isEnableSelection);
			isComputeInsetsDoneOnce = false;
		} else {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
		}
	}

	// For Left Navigation Operation
	public void clipboardLeft(boolean longPress) {
		//InputConnection inputConn = ((InputMethodService) mContext).getCurrentInputConnection();
		EditText inputConn = mInputView.getFocusedEditText();
		long l = SystemClock.uptimeMillis();
		if (mKPTClipboard != null) {
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.dispatchKeyEvent(new KeyEvent(l, l, 0, 59, 0, 0, 0, 0, 6));
			}
			//((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.dispatchKeyEvent(new KeyEvent(l, l, 1, 59, 0, 0, 0, 0, 6));
			}	
		}
	}*/

	/*public void clipboardSelectOperation() {
		mKPTClipboard.mSelectionON = true;

		EditText extracted = null;
		if (mInputView.getFocusedEditText() != null) {
			extracted = mInputView.getFocusedEditText();mInputConnection.getExtractedText(
					new ExtractedTextRequest(), 0);
		}

		CharSequence textBeforeCursor = null;
		if(extracted != null && extracted.getText() != null) {
			textBeforeCursor = getTextBeforeCursor(extracted.getText().toString().length(), 0);

			if (textBeforeCursor != null && textBeforeCursor.length() > 0) {
				int coreCurPos = textBeforeCursor.length();
				//mCaretPosition = coreCurPos;
				if (extracted.getText().length() == textBeforeCursor.length()) {
					extracted.setSelection(coreCurPos, coreCurPos - 1);
				} else if (extracted.getSelectionEnd() == extracted.getSelectionStart()) {
					extracted.setSelection(coreCurPos, coreCurPos + 1);
				}

			}
		}
	}*/

	/*public void clipboardSelectAll() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if(mInputView.getFocusedEditText() == null){
			return;
		}

		if(!isUnkownEditor()) {
			EditText eText = mInputView.getFocusedEditText();//mInputConnection.getExtractedText(new ExtractedTextRequest(),0);

			try {
				if(eText != null && eText.getSelectionStart() != eText.getSelectionEnd()) {
					CharSequence selectedStr = eText.getText().toString();
					if(!TextUtils.isEmpty(selectedStr)) {

						String textInEditor = selectedStr.toString();
						String selectedText = null;
						if(selectedStr.length() > eText.getSelectionStart()) {
							selectedText = selectedStr.toString().substring(eText.getSelectionStart(), eText.getSelectionEnd());
						}

						if(textInEditor.equals(selectedText)) {
							//total text in editor is already selected no need to select again
							return;
						} 
					}
				}
			}catch(Exception e) {
				//e.printStackTrace();
			}

			if(eText != null && eText.getText() != null && !TextUtils.isEmpty(eText.getText())) {
				eText.beginBatchEdit();
				finishComposingText();
				eText.setSelection(0, 0);
				eText.setSelection(0, eText.getText().length());
				eText.endBatchEdit();

				//TP 15370. dont disable suggestion bar 
				//setSuggestionStripShown(false);
				isComputeInsetsDoneOnce = false;
			}
		} else {
			
			//mInputConnection.performContextMenuAction(android.R.id.selectAll);niraj
		}
	}

	// For Right Navigation
	public void clipboardRight(boolean longpress) {
		InputConnection inputConn = ((InputMethodService) mContext).getCurrentInputConnection();
		long l = SystemClock.uptimeMillis();
		if (mKPTClipboard != null) {
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 0, 59, 0, 0, 0, 0, 6));
			}
			((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 1, 59, 0, 0, 0, 0, 6));
			}	
		}
	}

	// For Copy Operation
	public void clipboardCopy() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if (mInputConnection == null) {
			return;
		}
		ExtractedText extracted = mInputConnection.getExtractedText(
				new ExtractedTextRequest(), 0);
		String selectedText = null;
		if (extracted != null
				&& extracted.selectionStart != extracted.selectionEnd) {
			CharSequence extractedText = extracted.text;
			if (extractedText != null){
				if (extracted.selectionStart > extracted.selectionEnd) {
					selectedText = extractedText.toString().substring(
							extracted.selectionEnd, extracted.selectionStart);
				} else {
					selectedText = extractedText.toString().substring(
							extracted.selectionStart, extracted.selectionEnd);
				}
			}

			if (selectedText != null) {
				mClipManager.setText(selectedText);
			}
		}
	}

	// For Down Operation
	public void clipboardDown() {
		//sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);

		//Fix for TP Item-9776 ,13639
		InputConnection inputConn = ((InputMethodService) mContext).getCurrentInputConnection();
		long l = SystemClock.uptimeMillis();
		if (mKPTClipboard != null) {
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 0, 59, 0, 0, 0, 0, 6));
			}
			((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
			if(mKPTClipboard.mSelectionON){
				if(inputConn != null)
					inputConn.sendKeyEvent(new KeyEvent(l, l, 1, 59, 0, 0, 0, 0, 6));
			}	
		}
	}*/

	// Suppose its a password field then disable copy and paste options
	public boolean isPasswordField() {
		return mPasswordText;
	}

	// For Paste Operation
	public void clipboardPaste() {

		if(mIsUnknownEditor) {
			Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
			return;
		}

		if (mInputConnection == null) {
			return;
		}
		if (mCoreEngine == null) {
			return;
		}
		mInputConnection.beginBatchEdit();
		finishComposingText();

		CharSequence textToPaste = mClipManager.getText();

		ExtractedText extracted;
		if (textToPaste != null) {
			extracted = mInputConnection.getExtractedText(
					new ExtractedTextRequest(), 0);
			if (extracted == null) {
				int variation = ((InputMethodService) mContext).getCurrentInputEditorInfo().inputType
						& EditorInfo.TYPE_MASK_VARIATION;
				if (variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
						|| variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
						|| variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
					commitText(textToPaste.toString(), 1);
					mCoreEngine.insertText(textToPaste.toString(), false,false);

					mCoreEngine.setAbsoluteCaretPosition(mCoreEngine
							.getAbsoluteCaretPosition()
							+ textToPaste.length());
					finishComposingText();
					mInputConnection.endBatchEdit();
					return;
				}
			}

			CharSequence textBefCur = mInputConnection.getTextBeforeCursor(
					1, 0);
			boolean shouldAddSpace = true;
			if (extracted != null
					&& (extracted.text != null && extracted.text.length() == 0)
					|| (textBefCur != null && textBefCur.equals(" "))) {
				shouldAddSpace = false;
			}
			try {
				if(extracted != null && extracted.selectionStart != extracted.selectionEnd) {
					CharSequence selectedStr = extracted.text;
					if(!TextUtils.isEmpty(selectedStr)) {

						String textInEditor = selectedStr.toString();
						String selectedText = null;
						if(extracted.selectionStart > extracted.selectionEnd) {
							if(selectedStr.length() >= extracted.selectionStart) {
								selectedText = selectedStr.toString().substring(extracted.selectionEnd, extracted.selectionStart);
							}	
						} else {
							if(selectedStr.length() >= extracted.selectionEnd) {
								selectedText = selectedStr.toString().substring(extracted.selectionStart, extracted.selectionEnd);
							}
						}
						if(textInEditor.equals(selectedText)) {
							shouldAddSpace = false;
						} 
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			if (shouldAddSpace) {
				textToPaste = " " + textToPaste + " ";
			}

			setCurrentInputTypeToNormal();

			commitText(textToPaste.toString(), 1);

			extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			if(extracted != null && extracted.text != null) {
				mCoreEngine.syncCoreBuffer(extracted.text.toString(), extracted.selectionEnd);
			}
			/*mCoreEngine.insertText(textToPaste.toString(), false,false);
			mCoreEngine.setAbsoluteCaretPosition(mCoreEngine
					.getAbsoluteCaretPosition() + textToPaste.length());*/
			finishComposingText();
			mInputConnection.endBatchEdit();

			updateSuggestions(false);
		}
	}


	// For Navigation by Word by Word
	public void moveLeft() {
		if (mInputConnection == null) {
			return;
		}
		ExtractedText extracted = mInputConnection.getExtractedText(
				new ExtractedTextRequest(), 0);
		CharSequence textBeforeCursor = null;
		if(extracted != null &&
				extracted.text != null) {
			textBeforeCursor = getTextBeforeCursor(extracted.text.toString().length(), 0);
		}
		mInputConnection.beginBatchEdit();
		if (textBeforeCursor != null) {
			StringBuilder str = new StringBuilder(textBeforeCursor.toString());

			if (textBeforeCursor.length() > 0) {
				boolean spaceCheck = getTextBeforeCursor(1, 0).toString().equals(" ");
				if (spaceCheck) {
					((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
				} else {
					String revStr = (str.reverse()).toString();
					StringTokenizer st = new StringTokenizer(revStr, " ");
					try {
						if (st.hasMoreTokens() && st.countTokens() > 1) {
							int caretPos = textBeforeCursor.length()
									- st.nextToken().length() - 1;
							mInputConnection.setSelection(caretPos, caretPos);

						} else {
							((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
						}
					} catch (Exception e) {
					}
				}
			}
			mInputConnection.endBatchEdit();
		}
	}

	// For Navigation by Word by Word
	public void moveRight() {
		if(mInputConnection == null) {
			return;
		}
		ExtractedText extracted = mInputConnection.getExtractedText(
				new ExtractedTextRequest(), 0);
		if (extracted == null) {
			return;
		}
		CharSequence textBeforeCursor = null;
		CharSequence textAfterCursor = null;
		if(extracted != null && extracted.text != null) {
			textBeforeCursor = getTextBeforeCursor(extracted.text.toString().length(), 0);
			textAfterCursor = getTextAfterCursor(extracted.text.toString().length(), 0);
		}

		mInputConnection.beginBatchEdit();
		if ((textAfterCursor != null)) {
			String str = new String(textAfterCursor.toString());
			if (textAfterCursor.length() > 0) {
				boolean spaceCheck = getTextAfterCursor(1, 0).toString().equals(" ");
				if (spaceCheck) {
					((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
				} else {
					StringTokenizer st = new StringTokenizer(str, " ");
					if (st.hasMoreTokens() && st.countTokens() > 1) {
						if (textBeforeCursor != null){						
							int caretPos = textBeforeCursor.length()
									+ st.nextToken().length() + 2;
							mInputConnection.setSelection(caretPos, caretPos);
						}
					}else {
						((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
					}
				}
			}
			mInputConnection.endBatchEdit();
		}
	}

	public void onStartInput(EditText attribute, boolean restarting) {
		//super.onStartInput(attribute, restarting);
		mShowHKB = false;
		isComputeInsetsDoneOnce = false;

		//TP 8709. if accented characters are displayed and pressed home. since we can't get any 
		//callback for home press, we are checking here if any popups are remaining
		if(mInputView != null && !mInputView.isShown()) {
			mInputView.dismissMoreKeysPanel();
		}
		if(mKPTQuickSettingManager != null){
			
			mKPTQuickSettingManager.dissMissQuickSettingDialog();
		}
		//if(DEBUG)//Log.e("KPT", "onStartInput");
		//mKPTClipboard.hideClipboard();

		updateAdaptxtSoundandVibrate();

		setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));

		/*if(mLanguageSwitcher != null) {
			mIsThaiLanguage = KPTConstants.THAI_LANGUAGE.equalsIgnoreCase(mLanguageSwitcher.getScript());
		}*/

		// Fix for TP item 7064
		if(mOptionsDialog!=null && mOptionsDialog.isShowing())
		{
			mOptionsDialog.dismiss();
			mOptionsDialog=null;
		}
		if (mInputView == null) {
			return;
		} else if(!mInputView.isShown()) { //bug 626
			mInputView.setVisibility(View.VISIBLE);
		}
		/*KPTAdpatxtKeyboard adaptxtKeyboard = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
		if(adaptxtKeyboard != null){
			//Log.e("VMC", "ADAPTXT KEYBOARD IS NOT NULL ***** ");
			Key xiKey = adaptxtKeyboard.getXiKey();
			if(xiKey != null){
				xiKey.on=false;
			}
		}else{
			//Log.e("VMC", "ADAPTXT KEYBOARD IS NULL");
		}*/
		isHKb = false; //Kashyap: Bug Fix - 6104
		isStartCV = false;
		isBrowser = false;
		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}
		if (mIsInputFinished != false) { //bug fix  for 5772 --karthik
			mCharAdded = false;
			mCandidateSelected = false;
			mExtractedLength = 0;		
			mExtractedText = new SpannableStringBuilder();
		}
		mIsInputFinished = false;
		/*if((attribute != null && attribute.getInputType() == EditorInfo.TYPE_NULL) 
				|| mInputConnection == null) {
			setSuggestionStripShown(false);
		}*/
		//Performance Change
		//undoing performance change to fix this bug 6622
		//removed condition !mInputView.isShown because while switching IME between Adaptxt & other
		//candidate view is getting hidden
		if(mCapsLock) { //TP 8709
			mhardShift = HKB_STATE_LOCK; //Bug 6908
		} /*else if(mInputView != null && mInputView.isShifted()) {
			mhardShift = HKB_STATE_ON;
		}*/ else {
			mhardShift = HKB_STATE_OFF;
		}
		mhardAlt = HKB_STATE_OFF; //Bug 6901

		//SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		mAtxStateOn = mSharedPreference.getBoolean(KPTConstants.PREF_PHONE_ATX_STATE, true);
		//JET: Later need to change it as if the editor has some text?
//		resetTextBuffer();
		
		if(mInputView.isShown()/* || ((InputMethodService) mContext).isExtractViewShown() || !((InputMethodService) mContext).isInputViewShown()*/){
			isStartCV = true; // fix for TP 7201
			UpdateEditorInfo(attribute, restarting);
		}
		// ***Below code is commented to stop the old flow (first lauch or addon-manger)
		// Since setup wizard is added it will handle the new flow***

		//Search whether any add-on exist, if yes then install add-ons
		if(!mSharedPreference.getBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, false)) {
			KPTPkgHandlerService.searchForPreInstallAddons(mContext);
		} 
		
	}


	//bug #6933 case 1 - return when popupkeyboard is shown    
	public void showWindow(boolean showInput) {
		//	//Log.e(TAG, "Show Window");
		((InputMethodService) mContext).setCandidatesViewShown(false);
		if (mInputView != null && mInputView.isPopupKeyboardShown()) {
			return;
		} 
	}

	public MainKeyboardView onStartInputView(AdaptxtEditText attribute, boolean restarting) {
		mEditText = attribute;
		/*boolean isPaidCust = mSharedPreference.getBoolean(KPTConstants.PREMIUM_USER	, false);
		boolean showPaidAlarm = mSharedPreference.getBoolean("user_paid_already", true);
		boolean noDialog = false;*/
		//Log.e(TAG, "mEditText in checkdittext in editor " + mEditText.getId());
		/*
		 * Core is initilaized in create input view , but in some instances it is not called so initializing the core in this method.
		 * The issue is related to blanck CV on updagrade of Adaptxt.
		 */
		//Log.e("KPT", "onStartInputView mCoreEngine "+mCoreEngine);
		if (mCoreEngine == null) {
			if(mCoreServiceHandler != null){
				
				mCoreEngine = mCoreServiceHandler.getCoreInterface();
			}
			//Log.e("KPT", "onStartInputView mCoreEngine "+mCoreEngine);
			if (mCoreEngine != null) {
				initializeCore();
				mCoreEngine.setUserDictionaryWordLimit(KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
				if(mLanguageSwitcher != null ){
					
					mLanguageSwitcher.loadLocales(mContext);
					String inputLanguage = mLanguageSwitcher.getInputLanguage();
					if (inputLanguage == null) {
						inputLanguage = mResources.getConfiguration().locale.toString();
					}
					initSuggest(inputLanguage);
				}
			}
		}
		
		//checkLanguageChange();
		if(mInputView != null && !mInputView.isShown()) {
			mInputView.dismissMoreKeysPanel();
		}
		//if(DEBUG)//Log.e("KPT", "onStartInputView");

		// Calling this method will update the Keyboard views to new theme selected.
		updateTheme();

		mCurrentTheme =Integer.parseInt(mSharedPreference.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.BRIGHT_THEME+""));

		/*if(mSharedPreference.getBoolean(KPTConstants.PREF_BASE_IS_UPGRADING, false) 
				&& mShowUpdateDilaog && mHandler != null){
			mHandler.sendEmptyMessageDelayed(MSG_SHOW_UPDATE_DIALOG, 50);
			mShowUpdateDilaog = false;
		}*/

		setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));

		if (mInputView == null) {
			return null;
		}else if(!mInputView.isShown()) { //bug 6626
			mInputView.setVisibility(View.VISIBLE);
		}
		//15838 	The last letter adds in the next input in a messenger. 
		/*if(mSyllableComp != null) {
			mSyllableComp.dissolveJamo();
		}
		if(mPhoneSyllableComp != null) {
			mPhoneSyllableComp.dissolveJamo();
		}*/
		// disabling Xi key if enabled
		KPTAdpatxtKeyboard adaptxtKeyboard = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
		if(adaptxtKeyboard != null){
			Key xiKey = adaptxtKeyboard.getXiKey();
			if(xiKey != null){
				xiKey.on=false;
			}
		}		
		KPTConstants.mXi_enable=false;
		onNewEditor();
		
		//JET: Later need to change it as if the editor has some text?
		//resetTextBuffer();

		if(mInputView.isShown()){
			isStartCV = true;
			UpdateEditorInfo(attribute, restarting);
		}
		if(mCoreEngine != null){
			mAppCtxtSuggEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_APP_CTXT_SUGGS, true);
			mAutoSpacePref = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_SPACE, true);
			mCoreEngine.setAppContextEnabled(mAppCtxtSuggEnabled);  
			mCoreEngine.setAutoSpaceEnable(mAutoSpacePref);
			mCoreEngine.setATRStatus(mSharedPreference.getBoolean(KPTConstants.PREF_ATR_FEATURE, true));
		}
		
		//Suggestion bar height update w.r.t orientation changes.
		applyCustomizedKeyboardAndCandidateHeightChanges();

		mSharedPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(!mSharedPreference.getBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, false)) {
			KPTPkgHandlerService.searchForPreInstallAddons(mContext);
		}
		//TP:#20310
		if(mCoreEngine != null && mLanguageSwitcher != null){
			mCoreEngine.activateLanguageKeymap(mLanguageSwitcher.getCurrentComponentId(), null);
		}
		
		//Glide settings update when user navigationg to Settings screen & return
		mInputView.updatePreviewPlacerView();
		mInputView.getActionBarPlusStatusBarHeight(getActionBarPlusStatusBarHeight());
		mIsGlideEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true);
		
		//int dictsUpdate = mSharedPreference.getInt(KPTConstants.PREFS_ADDONS_UPDATE_COUNT, 0);
		/*	
		if(mCoreEngine != null){
			//Log.e(TAG, "check passed");
			checkAddonsUpdate();
		}*/
		if(mHandler.hasMessages(MSG_GET_VIEW_HEIGHT)){
			mHandler.removeMessages(MSG_GET_VIEW_HEIGHT);
		}
		mHandler.sendEmptyMessageDelayed(MSG_GET_VIEW_HEIGHT, 300);
		
		return mInputView;
	}
	
	
	private void onNewEditor() {
		
		
		if (null == mEditText || mTextInBuffer == null) {
			//Log.e("KPT", " Strange edit text copy edittext is null");
			return;
		}
		mTextInBuffer.setLength(0);
		mCurrCursorPos = mEditText.getSelectionEnd();
		mTextInBuffer.setLength(0);
		mTextInBuffer = mTextInBuffer.append(mEditText.getText().toString());
		if(mCoreEngine != null ){
			mCoreEngine.resetCoreString();
			mCoreEngine.syncCoreBuffer(mTextInBuffer.toString(), mCurrCursorPos);
		}
		updateSuggestions(true);
		
	}


	public int getApproxHeight() {
		int h = mContext.getResources().getDisplayMetrics().heightPixels;
		int height = 0 ;
		int res = (int)mContext.getResources().getDimension(R.dimen.kpt_key_height_dk_fk);
		res = (int) (res * 4 + 3 * mContext.getResources().getDimension(R.dimen.kpt_vertical_gap_dk_fk));
		int cvHeight = (int)mContext.getResources().getDimension(R.dimen.kpt_candidate_strip_height);
		height = res +cvHeight;
		////Log.e("ES", " calculated height ? "+height+ "key ? "+res + " cv ? "+cvHeight);		
		return height;
	}
	
	
	public int getReaLHeight(){
		
		int keybaordHeight = 0;
		int candidateView = 0;
		
		if (mInputView != null && mInputView.getVisibility() == View.VISIBLE) {
			keybaordHeight = mInputView.getHeight();
		}
		
		if (mCandidateViewContainer != null && mCandidateViewContainer.getVisibility() == View.VISIBLE) {
			candidateView = mCandidateViewContainer.getHeight();
		}
		
		return keybaordHeight+candidateView;
	}
	
	
	public void onKeyboardStartInputView(EditText attribute, boolean restarting) {
		////Log.e("VMC","onKeyboardStartInputView");
		

		/*
		 * Core is initilaized in create input view , but in some instances it is not called so initializing the core in this method.
		 * The issue is related to blanck CV on updagrade of Adaptxt.
		 */
		//Log.e("KPT", "onKeyboardStartInputView mCoreEngine "+mCoreEngine);
		if (mCoreEngine == null) {
			mCoreEngine = mCoreServiceHandler.getCoreInterface();
			//Log.e("KPT", "onKeyboardStartInputView mCoreEngine "+mCoreEngine);
			if (mCoreEngine != null) {
				initializeCore();
				mCoreEngine.setUserDictionaryWordLimit(KPTConstants.MAX_USER_DICTIONARY_WORD_LIMIT);
				if(mLanguageSwitcher != null){
					mLanguageSwitcher.loadLocales(mContext);
					String inputLanguage = mLanguageSwitcher.getInputLanguage();
					if (inputLanguage == null) {
						inputLanguage = mResources.getConfiguration().locale.toString();
					}
					initSuggest(inputLanguage);
				}
				
			}
		}

		

		
		////Log.e(TAG, " on start input view ");
		// / if(!noDialog){
		
	
		// }

		// In landscape mode, this method gets called without the input view
		// being created.

	/*	if (mVoiceRecognitionTrigger != null) {
			mVoiceRecognitionTrigger.onStartInputView();
		}*/

		if(mInputView != null && !mInputView.isShown()) {
			mInputView.dismissMoreKeysPanel();
		}
		//if(DEBUG)//Log.e("KPT", "onStartInputView");

		// Calling this method will update the Keyboard views to new theme selected.
		updateTheme();

		mCurrentTheme =Integer.parseInt(mSharedPreference.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.BRIGHT_THEME+""));

	/*	if(mSharedPreference.getBoolean(KPTConstants.PREF_BASE_IS_UPGRADING, false) 
				&& mShowUpdateDilaog && mHandler != null){
			mHandler.sendEmptyMessageDelayed(MSG_SHOW_UPDATE_DIALOG, 50);
			mShowUpdateDilaog = false;
		}*/

		setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));

		if (mInputView == null) {
			return;
		}else if(!mInputView.isShown()) { //bug 6626
			mInputView.setVisibility(View.VISIBLE);
		}
		//15838 	The last letter adds in the next input in a messenger. 
		/*if(mSyllableComp != null) {
			mSyllableComp.dissolveJamo();
		}
		if(mPhoneSyllableComp != null) {
			mPhoneSyllableComp.dissolveJamo();
		}*/
		// disabling Xi key if enabled
		KPTAdpatxtKeyboard adaptxtKeyboard = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
		if(adaptxtKeyboard != null){
			Key xiKey = adaptxtKeyboard.getXiKey();
			if(xiKey != null){
				xiKey.on=false;
			}
		}		
		KPTConstants.mXi_enable=false;

		if(mInputView.isShown()){
			isStartCV = true;
			UpdateEditorInfo(attribute, restarting);
		}

		if(mCoreEngine != null){
			mAppCtxtSuggEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_APP_CTXT_SUGGS, true);
			mAutoSpacePref = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_SPACE, true);
			mCoreEngine.setAppContextEnabled(mAppCtxtSuggEnabled);  
			mCoreEngine.setAutoSpaceEnable(mAutoSpacePref);
			mCoreEngine.setATRStatus(mSharedPreference.getBoolean(KPTConstants.PREF_ATR_FEATURE, true));
		}

		applyCustomizedKeyboardAndCandidateHeightChanges();

		//		mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
		if(!mSharedPreference.getBoolean(KPTConstants.SEARCH_PREINSTALL_ADDONS, false)) {
			KPTPkgHandlerService.searchForPreInstallAddons(mContext);
		}

		mInputView.updatePreviewPlacerView();
//		mInputView.getActionBarPlusStatusBarHeight(getActionBarPlusStatusBarHeight());

		//int dictsUpdate = mSharedPreference.getInt(KPTConstants.PREFS_ADDONS_UPDATE_COUNT, 0);
		/*	
		if(mCoreEngine != null){
			//Log.e(TAG, "check passed");
			checkAddonsUpdate();
		}*/
	}

	public void onInitializeInterface() {

		//super.onInitializeInterface();
		// Fix for TP item 7163
		if(mInputView != null && mInputView.isPopupKeyboardShown()){
			mInputView.dismissMoreKeysPanel();
		}
		// Fix for 7016 & 7018. Creating the inputview and candidate view here
		//createInputView();
		boolean isRTL = checkIfRTL(getInputLocale());
		createCandidatesView(true,isRTL);
	}

	/**
	Method to check whether the current keyboard locale is a RTL(RightToLeft) language (or) not
	 */

	public boolean checkIfRTL(Locale aCurrentLocale) {
		// construct system locale string and check it exists in RTL XML list
		/*String sysLocaleString = aCurrentLocale.getLanguage() + "-" + aCurrentLocale.getCountry();
		List<String> msiRTLMap = KPTParseRTLLocaleMapXml.parseXmlMsi(mContext);
		if( -1 != msiRTLMap.indexOf(sysLocaleString)){	//JeT need to check index related	
			return true;
		}*/
		return false;
	}

	public Locale getInputLocale(){
		return mLanguageSwitcher.getInputLocale();
	}


	public void onStartCandidatesView(EditText info, boolean restarting) { 

		if (mInputView == null) {
			return;
		}

		// Fix for TP 7737
		if(mInputView.isShown())
		{
			isStartCV = true;
		}
		if (mCandidateView == null) {
			return;
		}
		if (mHardKBCandidateView == null) {
			return;
		}
		//Performance Change. Uncommented the two lines below
		if(!mCandidateView.isShown() || !mHardKBCandidateView.isShown()){
			//TP #15499. Blank screen is displaying on selecting a contact from the list in Whatsapp application.
			//For whatsapp(in 4.4 device) in New message > search > enter a char > select one result, in this scenario
			//after onFinishInputView instead of onstartInput and onstartInputView, this method is
			//called directly. So the following check
			String pkgName = null;
			if(info != null) { 
				pkgName = mContext.getApplicationContext().getPackageName();
			} else {
				EditorInfo i = ((InputMethodService) mContext).getCurrentInputEditorInfo();
				if(i != null) {
					pkgName = i.packageName;
				}
			}
			//fix for 16087 User feedback: issues with Tapatalk app 
	
				//UpdateEditorInfo(info, restarting);
			
		}
		if(mCandidateViewContainer != null){
			/*if(((InputMethodService) mContext).isInputViewShown() && mHardKBCandidateContainer != null){
				mHardKBCandidateContainer.initViews();
				mHardKBCandidateContainer.updateCandidateBar();
			}*/

			mCandidateViewContainer.initViews();
			mCandidateViewContainer.updateCandidateBar();
		}

	}


	/**
	 * Adds existing text present in Editor to core including Cursor Position
	 */
	private void addPreExistingTextToCore(boolean restarting) {
		if (mCoreEngine == null || mCoreMaintenanceMode) {
			return;
		}
		if (mInputConnection == null) {
			return;
		}
		

		boolean isPvtModeOn = (mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
		//Kashyap: Bug Fix - 5978 - depending on Extracted text instead of text after and before cursor
		ExtractedText extText = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		updateExtractedText(extText);
		if (extText == null) {
			if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL || 
					mIsWebEditText) {
				if(restarting || mOrientationChanged) {
					if(mOrientationChanged) {
						mOrientationChanged = false;
					}
					addPreExistTxtToCoreForSpecificApps(restarting);
					setPreExistingTextToComposingMode(mInputConnection);
					updateSuggestions(true);
				} else {
					mCoreEngine.resetCoreString();
				}
			}
			return;
		}
		boolean atxOn = false;
			atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
					&& mKeyboardSwitcher.getPhoneKeypadState() == 
					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
		//undoing previous code to fix Bug 6622
		EditorInfo editorInfo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		String packageName = null;
		if(editorInfo != null && editorInfo.packageName != null) {
			packageName = editorInfo.packageName;
		}
		if(extText.text != null){
			mExtractedText = new SpannableStringBuilder(extText.text.toString());
			mExtractedLength = mExtractedText.length();
			String coreBuffer = mCoreEngine.getCoreBuffer();
			int isNewTextSameasPrevText =-1;
			if(coreBuffer!=null)
				isNewTextSameasPrevText =  coreBuffer.compareTo(mExtractedText.toString());
			/*
			 * Text in edit text is sync'ed with core buffer every time this method is called.
			 * 
			 * This method is called for every updateEditorInfo()
			 * This is called in two scenarios 
			 * 
			 * 1) When editor is changed : we reset core, add new text and learn the text in edit text.
			 * 2) Orientation change with same edit text : We sync the text with core to retain the ACC and revert information.
			 * 
			 * As in case two for some editors it is called more than one time then ignore the later calls using mOrentationChanged boolean
			 * 
			 * 
			 */

			if(isNewTextSameasPrevText!=0){
				////Log.e(TAG, "core reset and learning with pruning");				
				mCoreEngine.resetCoreString();
				//When the candidate view is not shown text present in the editor should not be learnt so the following check is imposed.
				if((!isHideCV && mSuggestionBar) /*&& !isPvtModeOn*/){
					//removed for TP 8666 mCoreEngine.learnBufferWithPrunning(mExtractedText, mExtractedLength);
					// added for TP 8666
					mCoreEngine.insertText(mExtractedText.toString(), atxOn,false);
				}
				// //removed for TP 8666 mCoreEngine.insertText(mExtractedText, atxOn,false);
			}else if((isNewTextSameasPrevText==0)&& mOrientationChanged){
				////Log.e(TAG, "sync core buffer in orientation change");
				mOrientationChanged  = false;
				mCoreEngine.syncCoreBuffer(mExtractedText.toString(), extText.selectionEnd);
			} else if(packageName != null && packageName.compareTo(KPTConstants.PACKAGE_NAME_EVERNOTE) == 0) {
				mCoreEngine.syncCoreBuffer(mExtractedText.toString(), extText.selectionEnd);
				setXiKeyState(isRevertedWord() && mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
			}
			mCoreEngine.setAbsoluteCaretPosition(extText.selectionEnd);
			// bug fix for #14969. Word that is being edited is suggested again
			postUpdateSuggestions();
		}
	}

	/**
	 * 
	 *  For certain editors like HTC MAil App (launched on selecting reply from HTC Calendar)
	 *  and Samsung S4 email editor. OnstartInput and OnstartInputview are called on a cursor jump.
	 *  OnupdatezSel might or might not be called depending on the app. Even if onUpdateSel is called
	 *  txtBeforeCursor, txtAfterCursor and extractedTxt in editor may or may not be correct.
	 *  
	 *  But OnstartInput and OnStartInputView are called with restarting flag as false. Depending on this
	 *  we reset the core string buffer and show suggestion on jump.
	 * 
	 * @param restarting
	 */
	private boolean addPreExistTxtToCoreForSpecificApps(boolean restarting){
		if(mCoreEngine == null) {
			return false;
		}
		boolean atxOn = false;
		String txtBeforeCursor = null;
		String txtAfterCursor = null;
		String coreEngineBufferStr = null;
		int totalTxtInCoreLength = -1;

		coreEngineBufferStr = mCoreEngine.getCoreBuffer();

		/**Use totalExtractedInEditor, if its not null and totalExtractedInEditorLength>0. 
			If null then depend on core string buffer
		 */
		/*String totalExtractedInEditor = null;
		ExtractedText extTxt = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if(extTxt != null && extTxt.text.toString() != null) {
			totalExtractedInEditor = extTxt.text.toString();
		}*/
		if(coreEngineBufferStr != null && coreEngineBufferStr.length()>0 && mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL) {
			totalTxtInCoreLength = coreEngineBufferStr.length();
		} else {
			totalTxtInCoreLength = 15000;
		}
		CharSequence charSeqBeforeCursor = getTextBeforeCursor(totalTxtInCoreLength, 0);
		CharSequence charSeqAfterCursor = getTextAfterCursor(totalTxtInCoreLength, 0);
		int currentCursorPos = -1;
		if(charSeqBeforeCursor != null) {
			txtBeforeCursor = charSeqBeforeCursor.toString();
		}
		if(txtBeforeCursor != null) {
			currentCursorPos = txtBeforeCursor.length();
		}
		if(charSeqAfterCursor != null) {
			txtAfterCursor = charSeqAfterCursor.toString();
		}

		String txtinEditor = txtBeforeCursor+txtAfterCursor;

			atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
					&& mKeyboardSwitcher.getPhoneKeypadState() == 
					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
		boolean txtInEditorAndCoreAreSame = false;
		if(txtinEditor != null && coreEngineBufferStr != null && txtinEditor.compareTo(coreEngineBufferStr) == 0) {
			txtInEditorAndCoreAreSame = true;
		}
		if( txtinEditor!= null && currentCursorPos > -1) {
			if(!txtInEditorAndCoreAreSame) {
				mCoreEngine.resetCoreString();
				mCoreEngine.insertText(txtinEditor,atxOn,false);
			}
			mCoreEngine.setAbsoluteCaretPosition(currentCursorPos);
		}
		return txtInEditorAndCoreAreSame;
	}


	public void onFinishInput() {
		//super.onFinishInput();
		mIsInputFinished = true;
		if(mKPTQuickSettingManager != null){
			
			mKPTQuickSettingManager.dissMissQuickSettingDialog();
		}
		//mKPTQuickSettingManager.dismissCustomizationDialog();
		//mKPTClipboard.hideClipboard();

		if(mCoreEngine != null) {
			mCoreEngine.resetCoreString();
		}

		if (mInputView != null) {
			mInputView.closing();
		}
		if(mComposing!=null){
			mComposing.setLength(0);
		}

		if(mCoreEngine != null) {
			mCoreEngine.setBlackListed(false);
		}
		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}

		if(mSuggDelDialog!=null && mSuggDelDialog.isShowing()){
			mSuggDelDialog.dismiss();
		}

		if(mCandidateView!=null ){
			mCandidateView.dismissExpandedSuggWindow();
		}
		if(mHardKBCandidateView!=null ){
			mHardKBCandidateView.dismissExpandedSuggWindow();
		}
		//setSuggestionStripShown(false);
	}

	public void onUpdateExtractedText(int token, ExtractedText text) {
		//super.onUpdateExtractedText(token, text);
		if(token != mExtractedToken){
			return;
		}
	}

	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		/*super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);*/

		try {
			/*if(DEBUG){
				//Log.e("KPT","-------------- onUpdateSelection -----------------");
				//Log.e("KPT","old sel start -> " + oldSelStart + " old sel end -> " + oldSelEnd);
				//Log.e("KPT","new sel start -> " + newSelStart + " new sel end -> " + newSelEnd);
			}*/

			/*
			 * if in case the the old and new cusrsor positions are same, just return. This method will be called only once
			 * when a char is added through sip. But will be called more than once in few scenarions like
			 * delete key press, pick suggestion etc..
			 * 
			 * For Example, consider user has pressed delete. When it is called for the 
			 * first time old and new postions will be different so check for selection counter variable.
			 * When called second time old and new positions will be same, so no need to check anything
			 */
			if ((newSelStart == newSelEnd) && (oldSelStart == oldSelEnd)
					&& (newSelStart == oldSelStart) && (newSelEnd == oldSelEnd)
					&& (newSelStart == oldSelEnd) && (newSelEnd == oldSelStart)) {
				return;
			}


			if(mCoreEngine == null) {
				if(DEBUG) //Log.e("KPT","Returning core engine null");
				return;
			}

			if(isHideCV){
				boolean isEditorActionSearch = false;
				EditorInfo attr = ((InputMethodService) mContext).getCurrentInputEditorInfo();
				if (attr != null) {
					int imeOptionKey = attr.imeOptions & (EditorInfo.IME_MASK_ACTION |EditorInfo.IME_FLAG_NO_ENTER_ACTION);
					if(imeOptionKey == EditorInfo.IME_ACTION_SEARCH) {
						isEditorActionSearch = true; 
					}
				}
				if(isEditorActionSearch){
					setGlideSuggestionToNull();
					setCurrentInputTypeToNormal();
				}
				mCharAdded = false;
				mCandidateSelected = false;
				isHKb = false;

				if(DEBUG) //Log.e("KPT","returning isHideCV");
				return;
			}
			/*
			 *  For samsung in-built calculator apps onUpdateSelection is getting called even though 
			 *  the inputView is not shown, due to which the handleJumpCutPaste gets called
			 *  which calls stePreexistingtext. Ideally these events should not be handled,
			 *  hence returning
			 * 
			 */

			EditorInfo editorInfo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
			String packageName = null;
			if(editorInfo != null) {
				packageName = editorInfo.packageName;
			}

			if (packageName != null && packageName.equals(KPTConstants.SAMSUNG_CALCULATOR)) {
				return;
			}

			ExtractedText extracted = null;
			if(mInputConnection != null){
				extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
				if(extracted == null ) {
					if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL && newSelStart == newSelEnd) {
						/*setGlideSuggestionToNull();
						setCurrentInputTypeToNormal();
						setPreviousInputTypeToNormal();*/
						
						addPreExistTxtToCoreForSpecificApps(false);
						mCoreEngine.setAbsoluteCaretPosition(newSelStart);
						setGlideSuggestionToNull();
						setCurrentInputTypeToNormal();
						setPreviousInputTypeToNormal();
						updateSuggestions(false);
						setPreExistingTextToComposingMode(mInputConnection);
					}
					//if(DEBUG) //Log.e("KPT","Returning extracted text null");
					return;

				}
			}

			/*
			 * if the extended suggestion bar is displayed, dismiss it
			 */
			if(mCandidateView!=null ){
				mCandidateView.dismissExpandedSuggWindow();
			}
			if(mHardKBCandidateView!=null ){
				mHardKBCandidateView.dismissExpandedSuggWindow();
			}


			/*
			 * this method will be called when ever there is a change in cursor position.
			 * So for ONE char input there should be ONE corresponding OnUpdateSelection.
			 * 
			 * But for fast typing there will be NO SYNC for this flow.
			 * For example if user type "hello" really fast, the flow will be
			 * 1) commit "h"
			 * 2) onUpdateSelection for "h"
			 * 3) commit "e"
			 * 4) commit "l"
			 * 5) commit "l"
			 * 6) commit "o"
			 * 7) onUpdateSelection for "e"
			 * 8) onUpdateSelection for "l"
			 * 9) onUpdateSelection for "l"
			 * 10) onUpdateSelection for "o" 
			 * 
			 *  So handleJumpCutPaste will be called 3 times(for "l","l","o") since the core and
			 *  platform cusror positions will be different, and each time we will sync text with core. 
			 *  
			 *  So we use mExpectingUpdateSelectionCounter(default value is zero) which will be incremented when ever
			 *  we want the onupdateselection to be returned WITHOUT SYNCING anything with core like normal key typing.
			 *  and decremented when there is no need to sync
			 * 
			 */
			boolean isExpectingOnUpdateSelection = mExpectingUpdateSelectionCounter >= 1;

			if(!isExpectingOnUpdateSelection){
				if(extracted == null ) { //even though it is already checked, in some strange scenarions we are
					//still getting null. so checking again :)
					return;
				}
				int coreCaretPos = mCoreEngine.getAbsoluteCaretPosition();

				if(SystemInfo.getManufacturer().toLowerCase().contains("samsung")
						&& SystemInfo.getModelInfo().toLowerCase().contains("i9500") && mIsUnknownEditor) {
					String coreEngineBufferStr = null;
					int totalTxtInCoreLength = -1;
					coreEngineBufferStr = mCoreEngine.getCoreBuffer();
					if(coreEngineBufferStr != null && coreEngineBufferStr.length()>0 && mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL) {
						totalTxtInCoreLength = coreEngineBufferStr.length();
					} else {
						totalTxtInCoreLength = 15000;
					}
					CharSequence charSeqBeforeCursor = getTextBeforeCursor(totalTxtInCoreLength, 0);
					int samSungEmailExtractedTextLenght = charSeqBeforeCursor.length();
					isExpectingOnUpdateSelection = (coreCaretPos == samSungEmailExtractedTextLenght);
				} else{
					isExpectingOnUpdateSelection = (coreCaretPos == extracted.selectionEnd);
				}
			}

			////Log.e(TAG, "mExpectingUpdateSelectionCounter -->" + mExpectingUpdateSelectionCounter );
			//	//Log.e(TAG, "isExpectingOnUpdateSelection -->" + isExpectingOnUpdateSelection );

			if(isHKb == false && (extracted != null && extracted.selectionStart == extracted.selectionEnd 
					&& isExpectingOnUpdateSelection)){
				if(!(mExpectingUpdateSelectionCounter <= 0))
					mExpectingUpdateSelectionCounter --;
				return;
			} else if(mIsUnknownEditor 
					&& packageName != null 
					&& packageName.compareTo(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE_SAMSUNG_STORE) != 0
					&& packageName.compareTo(KPTConstants.PACKAGE_NAME_POLARIS_OFFICE) != 0) {
				//addPreExistingTextToCore(false);
				if(SystemInfo.getManufacturer().toLowerCase().contains("samsung")
						&& SystemInfo.getModelInfo().toLowerCase().contains("i9500")) {
					addPreExistTxtToCoreForSpecificApps(false);
				} else {
					addPreExistingTextToCore(false);
				}
				setPreExistingTextToComposingMode(mInputConnection);
				updateSuggestions(false);
				setGlideSuggestionToNull();
				setCurrentInputTypeToNormal();
				setPreviousInputTypeToNormal();
				return;
			}

			// Fix for #10 bug in Karbonn project
			// If the above check is surpassed, then this callback most probably handles cursor jump & in-line scenarios
			// So finish any possible on-going text inlining before proceeding further
			if(mInputConnection != null){
				finishComposingText();
			}

			// BugTracker #12, As per 0.7.3 requirement A07330-1, AC suggestion shud not be displayed while cursor navigation
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn, false,//Bug Fix : 5803
					mCorrectionMode,mSpaceCorrectionSuggestionsOn);


			if(isHKB() == true) { //This check to handle - onstartinput is called enormously, when HKB is operated
				if((isCharAdded() == false && isSuggestionSelected() == false)){   		
					isHKb = false;
					try{
						//Check whether a char is really entered 
						if(extracted != null && extracted.text!= null && 
								oldSelEnd >= newSelEnd ? (oldSelEnd - newSelEnd == 1) : (newSelEnd - oldSelEnd == 1)){
							handleUserInput(extracted.text.subSequence(oldSelEnd, newSelEnd).charAt(0), mIsSIPInput);
							return;
						} else {
							addPreExistingTextToCore(false);
							//if(mIsKeyboardTypeQwerty) 
							setPreExistingTextToComposingMode(mInputConnection);
							UpdateEditorInfo(mEditText, false);
							updateSuggestions(false);
						}
					}catch(Exception e){
						addPreExistingTextToCore(false);
						//if(mIsKeyboardTypeQwerty) 
						setPreExistingTextToComposingMode(mInputConnection);
						UpdateEditorInfo(mEditText, false);
						updateSuggestions(false);
					}
				} else {
					addPreExistingTextToCore(false);
					//if(mIsKeyboardTypeQwerty) 
					setPreExistingTextToComposingMode(mInputConnection);
					UpdateEditorInfo(mEditText, false);
					updateSuggestions(false);
				}
			}

			if (extracted!= null && extracted.selectionStart != extracted.selectionEnd) {

				menableACCAfterTap = true;

				if(mComposing.length() > 0){
					mComposing.setLength(0);
					if (mInputConnection != null) {
						finishComposingText();
					}
				}

				//Commenting the below lines because candidate view should be visible even when text is selected
				//according to TP 12013. HandlejumpCutPaste should be called for this

				/*mCoreEngine.setAbsoluteCaretPosition(newSelEnd);
				postUpdateSuggestions();
				return;*/
			}

			synchronized (this) {
				if (mInputConnection != null) {
					mInputConnection.beginBatchEdit();
					//mSuggestionShouldReplaceCurrentWord = false;
					// If the current selection in the text view changes, we should
					// clear whatever candidate text we have.
					if ((((mComposing.length() > 0)) && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd))
							&& isCharAdded() != true && isSuggestionSelected() != true) {
						mComposing.setLength(0);
						//updateSuggestions();
						finishComposingText();
					} 
						postUpdateShiftKeyState();

					// if its user operation ignore(SIP Operation)
					// else reset core(jump, cut, copy, paste)

					try {
						//	//Log.e(TAG, "handle cut pase called");
						handleJumpCutPaste(extracted, oldSelStart, oldSelEnd,
								newSelStart, newSelEnd, mInputConnection);
					} catch (Exception e) {
						addPreExistingTextToCore(false);
						//if(mIsKeyboardTypeQwerty) 
							setPreExistingTextToComposingMode(mInputConnection);
						if(mIsSIPInput)
							updateShiftKeyState(mEditText);
						else
							updateHKBShiftKeyState(mEditText);
						updateSuggestions(false);
					}
					//CharSequence txtAfterCur = getTextAfterCursor(Integer.MAX_VALUE / 2, 0);
					updateSuggestions(false);
					mCharAdded = false;
					mCandidateSelected = false;
					mInputConnection.endBatchEdit();
				}
			}

			CharSequence charAfter = getTextAfterCursor(1, 0);
			CharSequence charBefore = getTextBeforeCursor(1, 0);

			if (!mCharAdded && (charAfter != null && !charAfter.equals(" ") && !charAfter.equals("\n")&& !charAfter.equals(""))
					&& (charBefore != null && !charBefore.equals(" ") && !charBefore.equals("\n") && !charBefore.equals(""))) {
				KPTConstants.mIsNavigationBackwar = false;
			} else{
				KPTConstants.mIsNavigationBackwar = true;
			}

			mCandidateViewContainer.updateView();
			mHardKBCandidateContainer.updateView();

			if (!KPTConstants.mIsNavigationBackwar) {
				mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
						false, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
			} else {
				// BugTracker #12, resetting the AC correction status
				mCoreEngine.setErrorCorrection(mErrorCorrectionOn, mAutoCorrectOn
						&& (!isHideCV && mSuggestionBar),// Bug Fix : 5803
						mCorrectionMode,mSpaceCorrectionSuggestionsOn);
			}

			/*if (!mKPTClipboard.isHidden()) {

				setSuggestionStripShown(false);

			}*/
		}catch(Exception e) {

		}
	}



	public void deleteWord(){
		if (mInputConnection == null) {
			return;
		}
		if (mCoreEngine == null) {
			return;
		}
		ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(),0);
		if (extracted == null) {
			return;
		}
		int startIndex = 0, endIndex = 0;
		mInputConnection.beginBatchEdit(); 
		finishComposingText();
		CharSequence textBeforeCursor = null;
		if(extracted.text != null) {
			textBeforeCursor = getTextBeforeCursor(extracted.text.toString().length(), 0);
		}
		CurrentWord cuurent = mCoreEngine.getCurrentWordforDelete();
		String[] curWord =null;
		if(cuurent !=null){
			curWord = cuurent.currWord;
		}
		if(curWord != null && curWord.length > 1 && curWord[0] != null && curWord[1] != null){
			startIndex = mCoreEngine.getAbsoluteCaretPosition()- curWord[0].length();
			endIndex = mCoreEngine.getAbsoluteCaretPosition()+ curWord[1].length();
			mInputConnection.setSelection(textBeforeCursor.length(),textBeforeCursor.length());

			deleteSurroundingText(curWord[0].length(), curWord[1].length()+1);
		}
		updateShiftKeyState(mEditText);
		// Resetting auto correction state after deleting the suggestion using bin icon as we are disabling auto correction while showing bin icon in cv
		mCoreEngine.setErrorCorrection(mErrorCorrectionOn, mAutoCorrectOn
				&& (!isHideCV && mSuggestionBar),mCorrectionMode,mSpaceCorrectionSuggestionsOn);
		if( extracted != null){
			mCoreEngine.replaceString(startIndex,endIndex, "");
			KPTConstants.mIsNavigationBackwar = true;
			updateSuggestions(false);
		}
		mInputConnection.endBatchEdit();
		setXiKeyState(isRevertedWord() && mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));

		//this vibration and sound is getting called in onTouch also , so removing here.
		//		playKeyClick(0);
		//		vibrate(0);

	}



	private synchronized void handleJumpCutPaste(ExtractedText extracted, int oldSelStart,
			int oldSelEnd, int newSelStart, int newSelEnd, InputConnection ic)
					throws StringIndexOutOfBoundsException {


		setGlideSuggestionToNull();
		setCurrentInputTypeToNormal();
		setPreviousInputTypeToNormal();

		//This is called only from onUpdateSelection with Core engine and extracted null check
		int coreCursorPos = mCoreEngine.getAbsoluteCaretPosition();
		String coreBuffer = mCoreEngine.getCoreBuffer();
		if(/*extracted == null || */extracted.text == null) {
			mExtractedLength = 0;
			mExtractedText = new SpannableStringBuilder();
		}else {
			mExtractedLength = extracted.text.length();
			mExtractedText = new SpannableStringBuilder( extracted.text);
		}

		//boolean isNewTextSameasPrevText = coreBuffer.equals(mExtractedText); 
		int isNewTextSameasPrevText =-1;
		if(coreBuffer!=null)
			isNewTextSameasPrevText =  coreBuffer.compareTo(mExtractedText.toString());

		if (isNewTextSameasPrevText==0) {
			/*if(DEBUG) {
				//Log.e("KPT","JUMP DETECTED");
			}*/
			
			//passing newSelStart instead of newSelEnd because as per Tp 12013 when text is selected update suggestion
			//based on the left cheveron. In normal jump scenarios newSelStart and newSelEnd will always be same

			//if(coreCursorPos != newSelEnd) {
			mCoreEngine.setAbsoluteCaretPosition(newSelStart);
			mCurrCursorPos = newSelStart;
			if(!isHideCV){
				updateEditorText(true, true);
			}
			//}

			/*if(bIsKorean && mSyllableComp != null && mPhoneSyllableComp != null && !mReplacedChar) {
				mPhoneSyllableComp.dissolveJamo();
				mSyllableComp.dissolveJamo();
				mNaratgeulPreviousCode = 0;
			}*/


		} else {
			//Toast.makeText(this, "SyncCore Buffer HandleJump", Toast.LENGTH_SHORT).show();
			mCharAdded = false;
			if(extracted.text == null) {
				mCoreEngine.syncCoreBuffer("", 0);
				resetTextBuffer();
			}else {

				/*if(mExtractedText.length() < coreBuffer.length() ) {
					if(DEBUG) //Log.e("KPT","CUT DETECTED");

					mCoreEngine.replaceString(oldSelStart, oldSelEnd, "");
				} else {
					if(DEBUG) //Log.e("KPT","PASTE DETECTED");
					String pasteString =mExtractedText.substring(oldSelStart, newSelEnd);
					mCoreEngine.replaceString(oldSelStart, oldSelEnd, pasteString);
				}*/
				mCoreEngine.syncCoreBuffer(extracted.text.toString(), (newSelStart>newSelEnd)? newSelStart : newSelEnd);
				//JET sync the buffer
				syncBufferText(extracted.text.toString(), ((newSelStart > newSelEnd) ? newSelStart : newSelEnd));
			}
			finishComposingText();
		}

		mInputConnection.beginBatchEdit();
			setPreExistingTextToComposingMode(mInputConnection);
		mInputConnection.endBatchEdit();

		if(mIsSIPInput)
			updateShiftKeyState(mEditText);
		else
			updateHKBShiftKeyState(mEditText);

			updateSuggestions(false);

			setXiKeyState(isRevertedWord() && mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
	}

	public void hideWindow() {
		////Log.e(TAG, "hide window");
		if (mOptionsDialog != null && mOptionsDialog.isShowing()) {
			mOptionsDialog.dismiss();
			mOptionsDialog = null;
		}
		if(mKPTQuickSettingManager != null) {
			mKPTQuickSettingManager.dissMissQuickSettingDialog();
		}

		System.gc();
		//super.hideWindow();
	}


	public void setInputView(final View view) {
		//super.setInputView(view);

		//mExtractArea = getWindow().getWindow().getDecorView().findViewById(android.R.id.extractArea);
	}

	public void onComputeInsets(InputMethodService.Insets outInsets) {
		//super.onComputeInsets(outInsets);

		final MainKeyboardView mainKeyboardView = mInputView;
		if (mainKeyboardView == null ||mSuggestuicontainer == null || mCandidateViewContainer == null) {
			return;
		}
		////Log.e("kpt","On compute insets-->"+!isInputViewShown());
		if(!mInputView.isShown() || mainKeyboardView.getHeight() == 0) {

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				//fix for  #16088. User feedback: locked Chrome browser
				outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
				outInsets.touchableRegion.set(0, 0, 0, 0);
			} else {
				outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_CONTENT;
			}

			return;
		}

		final int adjustedBackingHeight = getAdjustedBackingViewHeight();
		final boolean backingGone = (mPreviewBackupContainer.getVisibility() == View.GONE);
		final int backingHeight = backingGone ? 0 : adjustedBackingHeight;
		// In fullscreen mode, the height of the extract area managed by InputMethodService should
		// be considered.-+
		// See {@link android.inputmethodservice.InputMethodService#onComputeInsets}.
		final int extractHeight = ((InputMethodService) mContext).isFullscreenMode() ? mExtractArea.getHeight() : 0;
		final int suggestionsHeight = (mSuggestuicontainer.getVisibility() == View.GONE)
				? 0	: mCandidateViewContainer.getHeight();
		final int extraHeight = extractHeight + backingHeight + suggestionsHeight;
		int touchY = extraHeight;
		// Need to set touchable region only if input view is being shown
		if (mainKeyboardView.isShown()) {
			if (mSuggestuicontainer.getVisibility() == View.VISIBLE) {
				touchY -= suggestionsHeight;
			}
			final int touchWidth = mainKeyboardView.getWidth();
			final int touchHeight = mainKeyboardView.getHeight() + extraHeight
					// Extend touchable region below the keyboard.
					+ EXTENDED_TOUCHABLE_REGION_HEIGHT;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
				outInsets.touchableRegion.set(0, touchY, touchWidth, touchHeight);
			} else {
				outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_CONTENT;
			}
		}
		outInsets.contentTopInsets = touchY;
		outInsets.visibleTopInsets = touchY;

		isComputeInsetsDoneOnce = true;
	}

	private int getAdjustedBackingViewHeight() {
		final int currentHeight = mPreviewBackupContainer.getHeight();
		if(isComputeInsetsDoneOnce) {
			return currentHeight;
		}

		final android.view.ViewGroup.LayoutParams params = mPreviewBackupContainer.getLayoutParams();
		if(params != null) {

			int backingpreviewHeight;
			int savedSuggestionHeight = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(KPTConstants.PREF_CUST_SUGGESTION_HEIGHT, 
					(int) mResources.getDimension(R.dimen.kpt_candidate_strip_height));
					//Bug fix for 16659 	Adaptxt SIP is getting flickered when navigated from SIP to Clipboard and vice versa 
			//if(mKPTClipboard.isHidden()) {
				final int displayHeight = mResources.getDisplayMetrics().heightPixels;
				//final Rect rect = new Rect();
				//mPreviewBackupContainer.getWindowVisibleDisplayFrame(rect);
				final int statusBarHeight = getStatusBarHeight(); //rect.top;

				/*	// Calculate the width and height for the backing view(view used to extend touchable region above the sip).
				if(!isCandidateViewShown()) {
				     savedSuggestionHeight = 0;
				}
				 */

				if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					backingpreviewHeight = (int)(displayHeight -(mInputView.getKeyboard().getHeight() + savedSuggestionHeight))
							- 2 * statusBarHeight;
				}else{
					backingpreviewHeight = (int)(displayHeight -(mInputView.getKeyboard().getHeight() + savedSuggestionHeight))
							- statusBarHeight ;
				}
			/*} else {

				final int displayHeight = mResources.getDisplayMetrics().heightPixels;
				final Rect rect = new Rect();
				mPreviewBackupContainer.getWindowVisibleDisplayFrame(rect);
				final int statusBarHeight = rect.top;
				if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					backingpreviewHeight = (int)(displayHeight) - (mClipboardView.getHeight() + 2 * statusBarHeight + savedSuggestionHeight);
				}else{
					backingpreviewHeight = (int)(displayHeight) - (mClipboardView.getHeight() + statusBarHeight + savedSuggestionHeight);
				}
			}*/
			params.height = backingpreviewHeight;
			mPreviewBackupContainer.setLayoutParams(params);
			return params.height;
		} else {
			return 0;
		}
	}

	//TP 15601 Last row of SIP gets truncated in Landscape mode for Default e-mail editor while navigating through post man
	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = mContext.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	protected static void setTouchableRegionCompat(InputMethodService.Insets outInsets,
			int x, int y, int width, int height) {
		outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
		outInsets.touchableRegion.set(x, y, width, height);
	}

	public boolean isPrintableChar( int keyCode ) {
		char c = (char) keyCode;
		Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
		boolean isControlChar = Character.isISOControl(keyCode);//Bug 6645
		return ((block != Character.UnicodeBlock.PRIVATE_USE_AREA) && !isControlChar);
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {

		/*if (mInputConnection == null) {
			return true;
		}

		EditorInfo editorInfo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (editorInfo == null) {
			return true;
		}
		if (editorInfo.inputType == NOT_AN_EDITOR) {
			return true;
		}

		if (mInputView == null) {
			return true;
		}
		if (mInputView.isPopupKeyboardShown()){
			return true;
		}*/

		/*if ((mInputConnection == null || getCurrentInputEditorInfo().inputType == NOT_AN_EDITOR)
				|| mInputView == null || mInputView.isPopupKeyboardShown()){
			return true;
		}*/
		int uniCode = event.getUnicodeChar();
		if (uniCode == UNICODE_FUN_KEY) {
			if(keyCode == KeyEvent.KEYCODE_DEL) {
				mHandler.sendEmptyMessage(MSG_HKB_DELETE_LONGPRESS);
				return true;
			}
			return false;
		} else {


			// Check if display accent is true
			boolean shouldDisplayAccents = mSharedPreference.getBoolean(
					KPTConstants.PREF_DISPLAY_ACCENTS, true);

			// Extended bubbles should be displayed in secondary and teritiary layouts even if extended bubble is disabled in settings screen.

			if(mKeyboardSwitcher!= null ){
				shouldDisplayAccents = shouldDisplayAccents ? shouldDisplayAccents :!mKeyboardSwitcher.isAlphabetMode();

			}

			String accentedCharList = null;
			// Currently accented is supported only for letter, so checking for
			// letter
			if (Character.isLetter(event.getUnicodeChar())
					&& shouldDisplayAccents) {
				// Get accented info
				accentedCharList = mHardKeyboardKeymap
						.getAccentedCharString(uniCode); // passing unicode of key
			}

			// Check if secondary key is there
			int keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON);
			boolean valid = isPrintableChar(keyChar);
			if (keyChar != 0 && valid && accentedCharList != null
					&& accentedCharList.length() > 0) { // Case where both secondary key & accented info available
				char sKey = (char) keyChar;
				accentedCharList = Character.toString(sKey) + accentedCharList;
			} else if ((keyChar != 0 && valid) 			// Case where secondary key exist, no accented info
					&& (accentedCharList == null ||  accentedCharList.length() == 0)) { 
				KeyEvent eUp = new KeyEvent(event.getDownTime(), event
						.getEventTime(), KeyEvent.ACTION_UP, keyCode, event
						.getRepeatCount(), MetaKeyKeyListener.META_ALT_ON);
				mLongPressState = HKB_LONGPRESS_STATE;
				onKeyUp(eUp.getKeyCode(), eUp);
				mLongPressState = HKB_IGNORE_STATE;
				return true;
			} else if ((keyChar == 0 || valid)
					&& (accentedCharList == null || accentedCharList.length() == 0)) { // Ignore
				return true;
			}

			if (accentedCharList != null && accentedCharList.length() > 0) {
				// mLongPressState = HKB_IGNORE_STATE;
				if (mInputView.getWindowToken() != null) {

					mInputView.onLongPress(keyCode, accentedCharList,
							((mhardShift == HKB_STATE_OFF) ? false : true),
							mInputView);
				} else if (mCandidateViewContainer.getWindowToken() != null) {
					mInputView.onLongPress(keyCode, accentedCharList,
							((mhardShift == HKB_STATE_OFF) ? false : true),
							mCandidateViewContainer);
				} else if (mHardKBCandidateContainer.getWindowToken() != null) {
					mInputView.onLongPress(keyCode, accentedCharList,
							((mhardShift == HKB_STATE_OFF) ? false : true),
							mHardKBCandidateContainer);
				} 
				else if (keyChar != 0 && valid) { // Bug fix 6636
					// Since both input view and candidate view window token is null
					// we are displaying secondary character
					KeyEvent eUp = new KeyEvent(event.getDownTime(), event
							.getEventTime(), KeyEvent.ACTION_UP, keyCode, event
							.getRepeatCount(), MetaKeyKeyListener.META_ALT_ON);
					mLongPressState = HKB_LONGPRESS_STATE;
					onKeyUp(eUp.getKeyCode(), eUp);
					mLongPressState = HKB_IGNORE_STATE;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * this flag indicated whether the candidate bar should be shown 
	 * for hard keyboard device.  
	 */
	boolean mShowHKB = false;

	private boolean mSpaceDoubleTAP;
	private char mMultitapCharInMessage;
	public static Hashtable<String,Integer> mHindhiSyllableTable = new Hashtable<String,Integer>();

	//private boolean mSpacebackspaced;

	/**
	 * This is the callback for Hard keyboard events.
	 * After HKB event is handled, this method is returning false to framework so that 
	 * framework handles the cursor change for Alt & Shift state. 
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	//bug fix 16495. User feedback: predictive mode is disabled after Samsung smiley is inserted
		if( keyCode != KeyEvent.KEYCODE_MENU){
			mAtxStateOn = false;
		}
		EditorInfo editorInfo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (mCoreEngine == null 
				|| mInputView == null
				|| (editorInfo != null && editorInfo.inputType == NOT_AN_EDITOR /*&& !mInputView.isShown()*/)) {
			sHardKeyHandled = true;
			return /*super.onKeyDown(keyCode, event)*/false;
		}

		if (keyCode != KeyEvent.KEYCODE_DPAD_UP
				&& keyCode != KeyEvent.KEYCODE_DPAD_DOWN
				&& keyCode != KeyEvent.KEYCODE_DPAD_LEFT
				&& keyCode != KeyEvent.KEYCODE_DPAD_RIGHT
				&& keyCode != KeyEvent.KEYCODE_DPAD_CENTER
				&& keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_VOLUME_UP
				&& keyCode != KeyEvent.KEYCODE_VOLUME_DOWN){
			mIsSIPInput = false;
		}

		int uniCode = event.getUnicodeChar();
		if (uniCode != UNICODE_FUN_KEY && isPrintableChar(uniCode) 
				&& keyCode != KeyEvent.KEYCODE_ENTER 
				&& keyCode != KeyEvent.KEYCODE_SPACE) { // A-Z, 0-9, period, comma, slash
			if (event.getRepeatCount() == 0) {
				event.startTracking();
				mLongPressState = HKB_DEFAULT_STATE;
			}

			/*if(!((InputMethodService) mContext).isInputViewShown() && !isCandidateViewShown()) {
				mShowHKB = true;
				((InputMethodService) mContext).setCandidatesView(mHardKBCandidateContainer);
				((InputMethodService) mContext).setCandidatesViewShown(true);
			}*/

			return true;
		} else {
			switch (keyCode) {
			// Fix for TP 7219
			case KeyEvent.KEYCODE_HOME:
				if(mInputView.isShown()){
					mInputView.closing();
				}
				break;
				//Bug Fix 6705. 
			case KeyEvent.KEYCODE_MENU: {
				if(mInputView.isPopupKeyboardShown()){
					mInputView.dismissMoreKeysPanel();
					return false;
				} else {
					return /*super.onKeyDown(keyCode, event)*/true;
				}
			}
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT: {
				if(!mInputView.isPopupKeyboardShown())
					return /*super.onKeyDown(keyCode, event)*/false;
				else
					return true;
			}
			case KeyEvent.KEYCODE_ENTER: { //bug 6617
				if(!mInputView.isPopupKeyboardShown()) {
					sendKeyChar((char)event.getUnicodeChar());									
				} else {
					// Fix for TP #11604
					handleArrowKeyPressOnPopupKB(keyCode);
				}
				updateHKBShiftKeyState(mEditText);
				return true;
			}
			case KeyEvent.KEYCODE_DEL: {
				isStartCV = true;
				//Performance Change
				/*synchronized (this) {
					mCharAdded = true;
				}*/

				if(!mInputView.isPopupKeyboardShown()) {
					handleBackspace(mIsSIPInput);
					mDeleteCount++;
				}
				if (event.getRepeatCount() == 0) {
					event.startTracking();
					mLongPressState = HKB_DEFAULT_STATE;
				}
				return true;
			}
			case KeyEvent.KEYCODE_SPACE: {
				// Fix for TP 7176. Removing the HKB Alt key check to override the device's default functionality
				if(!mInputView.isPopupKeyboardShown()) {
					handleUserInput(KPTConstants.KEYCODE_SPACE, mIsSIPInput);
					return true;
				}
			}

			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if(mInputView.isPopupKeyboardShown() /*&& mKeyPressedCode != KeyboardView.NOT_A_KEY*/) {
					handleArrowKeyPressOnPopupKB(keyCode);
					return true;
				} else {
					// Fix for TP item 6060, where HKB right navigation key at the end of cursor
					// was dismissing the SIP
					if (mhardAlt == HKB_STATE_LOCK) // fix for TP item 6977
					{
						mhardAlt = HKB_STATE_OFF;
					}
					break;
				}

			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(mInputView.isPopupKeyboardShown() /*&& mKeyPressedCode != KeyboardView.NOT_A_KEY*/) {
					return true;
				} else {
					return false;
				}

			case KeyEvent.KEYCODE_BACK: {
				// Fix for TP item 7204
				if(mInputView.isPopupKeyboardShown())
				{
					mInputView.dismissMoreKeysPanel();
					return true;
				}
				else
				{
					handleClose(true);
					if (event.getRepeatCount() == 0) {
						if(mCandidateView != null && mCandidateView.isShowingSuggPopUp()){
							mCandidateView.dismissExpandedSuggWindow();
							if (mCandidateViewContainer != null) {
								mCandidateViewContainer.changeExpandIcon(mTheme.getCurrentThemeType());
							}
							return true;
						}
						if (mHardKBCandidateView != null && mHardKBCandidateView.isShowingSuggPopUp()){
							mHardKBCandidateView.dismissExpandedSuggWindow();
							if (mHardKBCandidateContainer != null) {
								mHardKBCandidateContainer.changeExpandIcon(mTheme.getCurrentThemeType());
							}
							return true;
						}
						else{
							mInputView.dismissMoreKeysPanel();
							handleClose(true);
						}
						//bug fix 6626, Closing the SIP if it is QWERTY device with hard keyboard hidden
						if (mResources.getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY
								&& mResources.getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
							if (mInputView.isShown()) {
								mInputView.setVisibility(View.GONE);
								return true;
							}
						}
					}
				}				
			}				
			}
		}
		return /*super.onKeyDown(keyCode, event)*/true;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// keyHandled flag for the bug --6927
		if (mInputView == null) {
			sHardKeyHandled = false;
			return /*super.onKeyUp(keyCode, event)*/false;
		}

		EditorInfo editorInfo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (mCoreEngine == null || sHardKeyHandled 
				|| (editorInfo != null && editorInfo.inputType == NOT_AN_EDITOR && !mInputView.isShown())) {
			sHardKeyHandled = false;
			return /*super.onKeyUp(keyCode, event)*/false;
		}
		if(mInputView.isPopupKeyboardShown()) {
			//if(mKeyPressedCode != KeyboardView.NOT_A_KEY) {
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT: {
				// Consume this event, return true.
				// Don't reset the mKeyPressedCode flag.
			}
			break;
			case KeyEvent.KEYCODE_DPAD_CENTER: {
				//if(keyCode == mKeyPressedCode ) {
				KeyboardView miniKeyboardView = mInputView.getMiniKeyboardView();
				if (miniKeyboardView == null) {
					return true;
				}
				if(miniKeyboardView.getCurrentKeyIndex() != KeyboardView.NOT_A_KEY) {
					Keyboard miniKeyboard = miniKeyboardView.getKeyboard();
					if (miniKeyboard != null){
						List<Key> mKeys = miniKeyboard.getKeys();
						if (mKeys != null && mKeys.size() > miniKeyboardView.getCurrentKeyIndex()) {
							Key currentKey = mKeys.get(miniKeyboardView.getCurrentKeyIndex());
							//bug fix 6655
							if(currentKey.text != null){
								onText(currentKey.text); 
							}else{
								onKey(currentKey.codes[0], new int[1], false);
							}
						}
					}
					//miniKeyboardView.showPreview(KeyboardView.NOT_A_KEY);
					mInputView.dismissMoreKeysPanel();
					//}
				}
			}
			}
			return true;
		}
		if (mLongPressState == HKB_IGNORE_STATE) {
			mLongPressState = HKB_DEFAULT_STATE;
			return true;
		}

		int keyChar = 0;
		if (keyCode != KeyEvent.KEYCODE_DEL) {
			mDeleteCount = 0;
		}else{
			updateSuggestions(true);
		}

		// For Browser Paste Issue
		if(isBrowser) {
			if(mInputConnection != null){
				ExtractedText extractedText = mInputConnection.getExtractedText(new ExtractedTextRequest(), 
						0);
				if(extractedText != null && extractedText.text != null){
					mExtractedText = new SpannableStringBuilder(extractedText.text.toString());
					mExtractedLength = mExtractedText.length();
					CharSequence textBeforeCursor = getTextBeforeCursor(Integer.MAX_VALUE/2, 0);
					if (textBeforeCursor != null) {
						mCoreEngine.syncCoreBuffer(mExtractedText.toString(), textBeforeCursor.length());
					}
				}
			}
		}

		int uniCode = event.getUnicodeChar();
		if (uniCode == UNICODE_FUN_KEY) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT: {
				// Clear Alt state and its cursor
				/* mhardAlt = HKB_STATE_OFF;
				int states = MetaKeyKeyListener.META_ALT_ON 
				| MetaKeyKeyListener.META_ALT_LOCKED;*/

				if (mhardShift == HKB_STATE_OFF) {
					mhardShift = HKB_STATE_ON;
				} else if (mhardShift == HKB_STATE_ON) {
					mhardShift = HKB_STATE_LOCK;
				} else if (mhardShift == HKB_STATE_LOCK) {
					mhardShift = HKB_STATE_OFF;
				}

				//getCurrentInputConnection().clearMetaKeyStates(states);
				updateHKBShiftStateToCore();				
				updateSuggestions(true);						

				//tp 7032. commented
				//if start of sentence is true and if user explicitly hits caps state then clear shift states
				/*if(isStartOfSentence()) {
					getCurrentInputConnection().clearMetaKeyStates(MetaKeyKeyListener.META_SHIFT_ON 
							| MetaKeyKeyListener.META_CAP_LOCKED);
					return true;
				}*/

				return /*super.onKeyUp(keyCode, event)*/true;
			}

			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT: {
				// Clear Shift state and its cursor change
				/* mhardShift = HKB_STATE_OFF;
				int states = MetaKeyKeyListener.META_SHIFT_ON 
						| MetaKeyKeyListener.META_CAP_LOCKED;*/

				if (mhardAlt == HKB_STATE_OFF) {
					mhardAlt = HKB_STATE_ON;
				} else if (mhardAlt == HKB_STATE_ON) {
					mhardAlt = HKB_STATE_LOCK;
				} else if (mhardAlt == HKB_STATE_LOCK) {
					mhardAlt = HKB_STATE_OFF;
				}
				//getCurrentInputConnection().clearMetaKeyStates(states);
				return /*super.onKeyUp(keyCode, event)*/true;
			}

			case KeyEvent.KEYCODE_DEL: {
				//Move this code to KeyDown event inorder 
				//support the same behaviour of SIP
				/*isStartCV = true;

				synchronized (this) {
					mCharAdded = true;
				}
				handleBackspace();
				mDeleteCount++;*/
				if(mHandler != null && mHandler.hasMessages(MSG_HKB_DELETE_LONGPRESS)){
					mHandler.removeMessages(MSG_HKB_DELETE_LONGPRESS);
				}
				return true;
			}

			case KeyEvent.KEYCODE_BACK:
				return /*super.onKeyUp(keyCode, event)*/true;

			case KeyEvent.KEYCODE_DPAD_CENTER:
				return false;


			default:
				return /*super.onKeyUp(keyCode, event)*/true;
			}
		} else if(keyCode == KeyEvent.KEYCODE_SPACE) {			
			return /*super.onKeyUp(keyCode, event)*/true;
		} else if(keyCode == KeyEvent.KEYCODE_ENTER) {			
			return true;
		} else {
			isStartCV = true;
			if (mLongPressState == HKB_LONGPRESS_STATE) { 
				keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON);				
				//bug fix 6618: Clear the state of alt or shift once long press on key occurs 
				if(mInputConnection != null){
					if (mhardAlt == HKB_STATE_ON) {					
						mhardAlt = HKB_STATE_OFF;
						mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_ALT_ON);
					} else if (mhardShift == HKB_STATE_ON) {
						mhardShift = HKB_STATE_OFF;
						mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_SHIFT_ON);
					}
				}
			}


			/**
			 * tp 7032. get the key char based on the states of the Alt and caps/shift
			 * key. Also clear the states of the keys once they are out of META_ALT_ON
			 * state. 
			 */
			if(mhardAlt == HKB_STATE_ON) {
				switch (mhardShift) {
				case HKB_STATE_ON:
					//tertiary char followed by all small chars
					//BUG ID 7206
					if(isStartOfSentence())
						//secondary char followed by all small chars
						keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON);
					else
						//tertiary char followed by all small chars
						keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON 
								| MetaKeyKeyListener.META_SHIFT_ON);
					mhardShift = HKB_STATE_OFF;
					if(mInputConnection != null)
						mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_SHIFT_ON);
					break;
				case HKB_STATE_LOCK:
					//tertiary char followed by all capital chars
					keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON 
							| MetaKeyKeyListener.META_CAP_LOCKED);
					if(mInputConnection != null)
						mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_CAP_LOCKED);
					break;
				case HKB_STATE_OFF:
					//secondary char followed by all small chars
					keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON);
					break;
				}
				mhardAlt = HKB_STATE_OFF;
				if(mInputConnection != null)
					mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_ALT_ON);
			} else if(mhardAlt == HKB_STATE_LOCK) {
				switch (mhardShift) {
				case HKB_STATE_ON:
					//Bug ID: 7177 
					//next char is tertiary, than all secondary characters
					keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_LOCKED);
					mhardShift = HKB_STATE_OFF;
					break;
				case HKB_STATE_LOCK:
					//all tertiary characters
					keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_LOCKED);
					break;
				case HKB_STATE_OFF:
					//all secondary characters
					keyChar = event.getUnicodeChar(MetaKeyKeyListener.META_ALT_ON);
					break;
				}
			} else if(mhardAlt == HKB_STATE_OFF) {
				switch (mhardShift) {
				case HKB_STATE_ON:
					//next char caps followed by all small chars
					keyChar = Character.toUpperCase(event.getUnicodeChar());
					mhardShift = HKB_STATE_OFF;
					break;
				case HKB_STATE_LOCK:
					//all chars caps
					keyChar = Character.toUpperCase(event.getUnicodeChar());
					break;
				case HKB_STATE_OFF:
					//all small
					// Fix for TP 7157
					if(isStartOfSentence()) {
						keyChar = Character.toUpperCase(event.getUnicodeChar());
					}else {
						keyChar = event.getUnicodeChar();	
					}
					break;
				}
			}

			//If it is not functional key, then send to core otherwise return to framework
			if (isPrintableChar(keyChar) && keyChar != UNICODE_FUN_KEY) {
				//Performance Change
				/*synchronized (this) {
					mCharAdded = true;
				}*/
				handleUserInput(keyChar, mIsSIPInput);				
				return true;
			} 

		}

		return /*super.onKeyUp(keyCode, event)*/true;
	}	

	/**
	 * Handles Arrow key press on the accented characters popup keyboard
	 * @param KeyCode The keycode of one of the right/left/up/down arrow keys
	 */
	private void handleArrowKeyPressOnPopupKB(int keyCode) {
		//This is called from onKeyDown() with mInputView null check. If we are calling from otherthan this
		//need to check the null check for mInputView.
		KeyboardView miniKeyboardView = mInputView.getMiniKeyboardView();
		if(mInputView.isPopupKeyboardShown() && miniKeyboardView != null ) {
			Keyboard miniKeyboard = miniKeyboardView.getKeyboard();
			if (miniKeyboard == null) {
				return;
			}
			List<Key> keysList = miniKeyboard.getKeys();

			if(miniKeyboardView.getCurrentKeyIndex() == KeyboardView.NOT_A_KEY) {
				int indexOfKeyToShow = 0;
				switch(keyCode) {
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					// Start from the first key
					indexOfKeyToShow = 0;
					break;

				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_UP:
					// start from the last key
					indexOfKeyToShow = keysList != null ? (keysList.size() - 1) : 0;
					break;

				default:
					break;

				}

				miniKeyboardView.handleKeysFromHardkeyArrows(indexOfKeyToShow);

			} else {
				if (keysList == null ||  keysList.size() <= miniKeyboardView.getCurrentKeyIndex()) {
					return;
				}
				Key currentKey = keysList.get(miniKeyboardView.getCurrentKeyIndex());
				int noOfRows = miniKeyboard.mNumberofRows;
				int rowIndex = currentKey.rowNumber;
				int keyIndexwithinRow = currentKey.keyIndexInRow;
				int keyIndex = miniKeyboardView.getCurrentKeyIndex() ;

				if(noOfRows == 1) {
					if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						keyCode = KeyEvent.KEYCODE_DPAD_LEFT;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
						keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
					}
				}

				switch(keyCode) {
				case KeyEvent.KEYCODE_DPAD_DOWN: {
					rowIndex++;

					if(rowIndex == noOfRows) {
						rowIndex = noOfRows - 1;
					}
				}
				break;

				case KeyEvent.KEYCODE_DPAD_UP: {
					rowIndex--;

					if(rowIndex < 0) {
						rowIndex = 0;
					}
				}
				break;

				case KeyEvent.KEYCODE_DPAD_LEFT: {
					keyIndexwithinRow--;
					if (keyIndexwithinRow < 0) {
						keyIndexwithinRow = 0;
					}
				}
				break;

				case KeyEvent.KEYCODE_DPAD_RIGHT: {
					keyIndexwithinRow++;
					// Will check for out of bounds later
				}
				break;
				//Fix for TP Item - 11604
				case KeyEvent.KEYCODE_ENTER: {
					int primaryCode = 0;
					if (keyIndex != KeyboardView.NOT_A_KEY ) {
						primaryCode = currentKey.codes[0];
					}
					onKey(primaryCode, null, true);
					dismissPopupKeyboard();
				}
				break;
				default:
					break;
				}

				// If either one has changed then show preview
				if(currentKey.keyIndexInRow != keyIndexwithinRow ||
						currentKey.rowNumber != rowIndex) {
					int lastKeyIndexinRow = 0;
					int indexofKeytoShow = 0;
					Key keyToShow = null;
					for(int ind = 0; ind < keysList.size(); ind++) {
						Key nextKey = keysList.get(ind);
						if(nextKey.rowNumber == rowIndex) {
							if(nextKey.keyIndexInRow == keyIndexwithinRow) {
								// This is the required key. Show And Break
								keyToShow = nextKey;
								indexofKeytoShow = ind;
								break;
							} else {
								if(nextKey.keyIndexInRow > lastKeyIndexinRow) {
									keyToShow = nextKey;
									indexofKeytoShow = ind;
									lastKeyIndexinRow = nextKey.keyIndexInRow;
								}
							}
						}
					}

					if(keyToShow != null && keyToShow != currentKey) {
						miniKeyboardView.handleKeysFromHardkeyArrows(indexofKeytoShow);

					}
				}
			}
		}
	}

	/*private void revertVoiceInput() {
		if (mInputConnection != null) {
			mCoreEngine.getCurrentWord();
			// Add a space if the field already has text.
			CharSequence charBeforeCursor = getTextBeforeCursor(1, 0);
			if (charBeforeCursor != null
					&& !charBeforeCursor.equals(" ")
					&& (charBeforeCursor.length() > 0)) {
				//delete the composing text from editor and also from core
				if (!mIsKeyboardTypeQwerty && !mAtxStateOn) {//Bug id:8154 
					deleteSurroundingText(
							mCoreEngine.getPrefixLength(), 0);
				} else {
					commitText("", 1);
				}

				mCoreEngine.removeString(true, mCoreEngine.getPrefixLength());
			}			
		}
		updateSuggestions();
		mVoiceInputHighlighted = false;
	}*/

	public void reloadKeyboards() {
		if (mKeyboardSwitcher == null) {
			mKeyboardSwitcher = new KPTKeyboardSwitcher(mContext, null);
		}
		if(mLanguageSwitcher != null){
			
			mKeyboardSwitcher.setLanguageSwitcher(mLanguageSwitcher);
		}
		if (mInputView != null) {
			mKeyboardSwitcher.setVoiceMode(true,true);
		}
		mKeyboardSwitcher.makeKeyboards();
	}

	private void commitTyped(InputConnection inputConnection) {
		if (inputConnection != null) {
			inputConnection.finishComposingText();
			//inputConnection.commitText("", 1);			
		}
	}

	private void postUpdateShiftKeyState() {
		mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
		mHandler.sendMessageDelayed(mHandler
				.obtainMessage(MSG_UPDATE_SHIFT_STATE), 100);
	}

	/**
	 * Posts a asynchronous request to IME's handler to reload the keyboards.
	 */
	private void postReloadKeyboards() {
		mHandler.removeMessages(MSG_RELOAD_KEYBOARDS);
		Message msg = mHandler
				.obtainMessage(AdaptxtIME.MSG_RELOAD_KEYBOARDS);
		msg.arg1 = 1;
		mHandler.sendMessageDelayed(msg, 100);
	}


	/**
	 * This method is called when editor gets focus and for each user input (char thru SIP, candidate selection, delete char)
	 * There are some editor and scenarios where Adaptxt explicitly need set caps as true irrespecitive of whether core suggest caps mode
	 * Ex: Contacts Name field if space is entered, If editor begins with space. 
	 * @param attr
	 */
	public void updateShiftKeyState(EditText attr) {
		if (attr == null) {
			return;
		}
		/*if (mInputConnection == null) {
			return;
		}*/
		if (mCoreEngine == null) {
			return;
		}
		if (mInputView == null){
			return;
		}
		/*	//Log.e(TAG,"attr>>>>>"+attr);
		//Log.e(TAG,"mInputView>>>>>"+mInputView);
		//Log.e(TAG,"mInputConnection>>>>>"+mInputConnection);
		//Log.e(TAG,"mCoreEngine>>>>>"+mCoreEngine);*/


		/*mInputView
			.setShifted(mCapsLock || getCursorCapsMode(mInputConnection, attr) != 0);*/
		boolean isStartOfSentence = isStartOfSentence();
		if(!isStartOfSentence && mRespectEditor) // karthik fix for 5990
			isStartOfSentence = getCursorCapsMode(mInputConnection,attr)!=0;
		updateShiftKeyState(attr, isStartOfSentence);
	}


	/**
	 * This method is called when editor gets focus and for each user input (char thru SIP, candidate selection, delete char)
	 * There are some editor and scenarios where Adaptxt explicitly need set caps as true irrespecitive of whether core suggest caps mode
	 * Ex: Contacts Name field if space is entered, If editor begins with space. 
	 * @param attr
	 * @param retainShiftState should shift state be retained.Overrides startOFSentence state too.
	 */
	public void updateShiftKeyState(EditText attr, boolean retainShiftState) {
		if (attr != null && mInputView != null
				/*&& mKeyboardSwitcher.isAlphabetMode() && mInputConnection != null */&& mCoreEngine!=null) {

			if(!mCapsLock && mInputView.isShifted()&& !retainShiftState) // Caps Bug fixes -- karthik
			{
				//Set the caps state to true once it is honour case
				mCoreEngine.resetNextCaps(); 
			}

			mInputView.setShifted(mCapsLock || retainShiftState);

		}
		updateShiftStateToCore();
	}

	/**
	 *  Method used to change to another SIP in Korean lang
	 *  Korean has two primary layouts associated with it, this function switches 
	 *  between those two primary layouts
	 * 
	 */
	private void changeKeyboardOnShift() {
		if (mKeyboardSwitcher == null) {
			return;
		}
		int options = mKeyboardSwitcher.getImeOptions(); 
		updateCorrectionMode();        
		mKeyboardSwitcher.setKeyboardMode(mEditorMode,
				options, false);

		reloadKeyboards();              
		System.gc();
	}
	/**
	 * This method is called to update the cursor of HKB. 
	 */
	public void updateHKBShiftKeyState(EditText attr) {
		if (attr != null && mInputConnection != null && mCoreEngine != null) {
			boolean isStartOfSentence = isStartOfSentence();
			if (mhardShift != HKB_STATE_LOCK) { // Bug 6908
				if (!isStartOfSentence && mRespectEditor) // karthik fix for 5990
					isStartOfSentence = getCursorCapsMode(mInputConnection, attr) != 0;

				if (isStartOfSentence) {
					mhardShift = HKB_STATE_ON;
					mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_SHIFT_ON
							| MetaKeyKeyListener.META_CAP_LOCKED);
				} else if (!isStartOfSentence && mhardShift != HKB_STATE_LOCK) {
					mhardShift = HKB_STATE_OFF;
					mInputConnection.clearMetaKeyStates(MetaKeyKeyListener.META_SHIFT_ON
							| MetaKeyKeyListener.META_CAP_LOCKED);
				}
			}
		}
		updateHKBShiftStateToCore();
	}

	//For every character, asking CORE whether start of sentence to be true 
	private boolean isStartOfSentence() {
		if (!mAutoCap){
			if(mCoreEngine != null){
				mCoreEngine.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_AUTO_CAPS_OFF);
			}
			return false;
		}
		else if(mInputInfo != null) { 
			return mInputInfo.isStartOfSentence;
		} else {
			if(mCoreEngine != null){
				return mCoreEngine.isStartOfSentence();
			}else{
				return false;
			}
			
		}
	}

	//This is used to get the caps mode from editor
	private int getCursorCapsMode(InputConnection ic, EditText attr) {
		int caps = 0;
		//EditorInfo ei = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (mAutoCap && mEditText != null/*&& ei != null && ei.inputType != EditorInfo.TYPE_NULL*/) {
			//caps = mInputConnection.getCursorCapsMode(attr.getInputType());
			
			int a = mEditText.getSelectionStart();
	        int b = mEditText.getSelectionEnd();

	        if (a > b) {
	            int tmp = a;
	            a = b;
	            b = tmp;
	        }
			TextUtils.getCapsMode(mEditText.getText().toString(), a, attr.getInputType());
		}
		
		return caps;
	}


	private void maybeRemovePreviousPeriod(CharSequence text) {
		/*if (mInputConnection == null){
			return;
		}*/
		if (text == null) {
			return;
		}

		// When the text's first character is '.', remove the previous period
		// if there is one.
		CharSequence lastOne = getTextBeforeCursor(1, 0);
		if (lastOne != null && lastOne.length() == 1
				&& lastOne.charAt(0) == KPTConstants.KEYCODE_PERIOD
				&& text.charAt(0) == KPTConstants.KEYCODE_PERIOD) {
//			deleteSurroundingText(1, 0);
			
			deleteCurrentChar(false);
		}
	}

	@SuppressWarnings("unused")
	private void removeTrailingSpace() {
		/*if (mInputConnection == null){
			return;
		}*/
		if (mCoreEngine == null) {
			return;
		}
		CharSequence lastOne = getTextBeforeCursor(1, 0);
		if (lastOne != null && lastOne.length() == 1
				&& lastOne.charAt(0) == KPTConstants.KEYCODE_SPACE) {
//			deleteSurroundingText(1, 0);
			mCoreEngine.removeString(true, 1);
			deleteCurrentChar(true);
		}
	}

	private boolean isAlphabet(int code) {
		if (Character.isLetter(code)) {
			return true;
		} else {
			return false;
		}
	}
	
	private void changeKeyboard(){
		
		if(mIsHindhi != true){
			return;
		}
		
		if(PointerTracker.mCurrentSyllableIndex == -1 && PointerTracker.mPreviousSyllableIndex == -1){
			return;
		}
		
	    if(PointerTracker.mCurrentSyllableIndex == 9 || PointerTracker.mCurrentSyllableIndex == 30 || PointerTracker.mCurrentSyllableIndex == 33 || PointerTracker.mCurrentSyllableIndex == 34 || PointerTracker.mCurrentSyllableIndex == 35  ){
	    	if(mKeyboardSwitcher != null){
	    		
	    		int options = mKeyboardSwitcher.getImeOptions(); 
	    		updateCorrectionMode();
	    		
	    		mKeyboardSwitcher.setKeyboardMode(mEditorMode, options, false);
	    	}

			reloadKeyboards();              
			System.gc();
	    }else{
	    	if(mInputView != null){
	    		mInputView.changeHindhiKeyBoard();
	    	}
	    	
	    }
		
		
	}
	
	
	// Implementation of KeyboardViewListener
	public void onKey(int primaryCode, int[] keyCodes, boolean ispopupChar) {
		
		if((primaryCode != KPTConstants.KEYCODE_DELETE
				&& primaryCode != KPTConstants.KEYCODE_DELETE_LONGPRESS
				&& primaryCode != KPTConstants.KEYCODE_GLOBE
				&& primaryCode != KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE
				&& primaryCode != KPTConstants.KEYCODE_MODE_CHANGE
				&& primaryCode != KPTConstants.KEYCODE_MODE_CHANGE_SHIFT
				&& primaryCode != KPTConstants.KEYCODE_PHONE_MODE_SYM
				&& primaryCode != KPTConstants.KEYCODE_ALT
				&& primaryCode != KPTConstants.KEYCODE_PHONE_MODE_SHIFT_SYM
				&& primaryCode != KPTConstants.KEYCODE_LOCALE_SWITCH
				&& primaryCode != KPTConstants.KEYCODE_NEXT_LANGUAGE
				&& primaryCode != KPTConstants.KEYCODE_PREV_LANGUAGE
				&& primaryCode != KPTConstants.KEYCODE_SHIFT
				&& primaryCode != KPTConstants.KEYCODE_JUMP_TO_TERTIARY)
				&& mEditText.getmMaxLength() > 0 
				&& (mEditText.getText().length() + 1) > mEditText.getmMaxLength()){
			return;
		}

		mShowHKB = false; 

		mIsSIPInput = true;
		mIsPopupChar = ispopupChar;

		long when = SystemClock.uptimeMillis();
		//boolean deleteOnNoText = false; // to identify whether deleteKey is pressed when no text is on editor

		boolean isXIEnabledBeforeSpaceDoubleTap = false;
		if(primaryCode == KPTConstants.KEYCODE_SPACE_DOUBLE_TAP) {
			isXIEnabledBeforeSpaceDoubleTap = KPTConstants.mXi_enable;
		}

		// TP 7735 Disable "Xi" key on any key press other than Xi key
		if((primaryCode !=  KPTConstants.KEYCODE_DELETE) && KPTConstants.mXi_enable) {
			setXiKeyState(false);
		}
		// For Browser Paste Issue
		if(isBrowser) {
			if(mInputConnection != null && mCoreEngine != null){
				ExtractedText extractedText = mInputConnection.getExtractedText(new ExtractedTextRequest(), 
						0);

				if(extractedText != null && extractedText.text != null){
					mExtractedText = new SpannableStringBuilder(extractedText.text.toString());
					mExtractedLength = mExtractedText.length();
					CharSequence textBeforeCursor = getTextBeforeCursor(Integer.MAX_VALUE/2, 0);
					if (textBeforeCursor != null) {
						mCoreEngine.syncCoreBuffer(mExtractedText.toString(), textBeforeCursor.length());
					}
				}
			}
		}


		if (primaryCode != KPTConstants.KEYCODE_DELETE
				|| when > mLastKeyTime + QUICK_PRESS) {
			mDeleteCount = 0;
		}
		mLastKeyTime = when;

		switch (primaryCode) {
		case KPTConstants.KEYCODE_GLOBE:
			showLanguageChangeDialog();
			break;
		case KPTConstants.KEYCODE_SHIFT_CHANGE: // used to switch between the primary layouts in korean
			int options = mKeyboardSwitcher.getImeOptions(); 
			updateCorrectionMode();

			mKeyboardSwitcher.setKeyboardMode(mEditorMode, options, false);

			reloadKeyboards();              
			System.gc();

			break;
			// handling the case when we press Xi key only if xi key is enabled
			// and updating space and xi keys..
		case KPTConstants.KEYCODE_XI:
			if(KPTConstants.mXi_enable && (mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL &&
			mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_EMAIL))
			{
				executeXiRevert();
			}
			break;
		case KPTConstants.KEYCODE_DELETE:
			//TP 12001
			KPTConstants.mIsNavigationBackwar = true;

			handleBackspace(mIsSIPInput);
			mDeleteCount++;
			if (menableACCAfterTap) {
				// BugTracker #12, As per 0.7.3 requirement A07330-1, AC suggestion shud not be displayed while cursor navigation
				if(mCoreEngine != null){
					if (!KPTConstants.mIsNavigationBackwar) {
						mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
								false, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
					} else {
						mCoreEngine.setErrorCorrection(mErrorCorrectionOn, mAutoCorrectOn && (!isHideCV && mSuggestionBar),//Bug Fix : 5803
								mCorrectionMode,mSpaceCorrectionSuggestionsOn);
					}
				}
				
				menableACCAfterTap = false;
			}
			break;


		case KPTConstants.KEYCODE_SHIFT:
			handleShift();
			break;
		case KPTConstants.KEYCODE_LOOKUP:
			/*if(getOlympicKeyState()){
				webURLlookup();
			}*/
			break;
		case KPTConstants.KEYCODE_PHONE_MODE_SYM:
			if(mKeyboardSwitcher != null){
				
				mKeyboardSwitcher.togglePhoneModeChange(mCurrentEditorType);
			}
			if(mInputView != null){
				
				updateShiftKeyState(mEditText, mInputView.isShifted());
				mInputView.changeKeyboardBG();
			}

			break;
		case KPTConstants.KEYCODE_PHONE_MODE_SHIFT_SYM:
			if(mKeyboardSwitcher != null){
				
				mKeyboardSwitcher.handlePhoneSymbolsModeChange();
			}
			if(mInputView != null){
				
				updateShiftKeyState(mEditText, mInputView.isShifted());
				mInputView.changeKeyboardBG();
			}
			updateSuggestions(true);
			break;
		case KPTConstants.KEYCODE_ALT:
				handleAlt();
				if(mInputView != null){
					
					mInputView.changeKeyboardBG();
				}
			checkIfGlideShouldBeEnabled();
			break;
		case KPTConstants.KEYCODE_LOCALE_SWITCH:
			switchLocale();
			break;
		case KPTConstants.KEYCODE_JUMP_TO_TERTIARY:
			jumptoTertiaryKeyboard();
			checkIfGlideShouldBeEnabled();
			break;
		case KPTConstants.KEYCODE_THAI_COMMIT:
			//THAI
			handleThaiCommit();
			break; 
		case KPTConstants.KEYCODE_THAI_SHIFT: 
			handleThaiShift();
			break;
		case KPTConstants.KEYCODE_CANCEL:
			if (mOptionsDialog == null || !mOptionsDialog.isShowing()) {
				handleClose(false);
			}
			break;
		case KPTConstants.KEYCODE_OPTIONS:
		case KPTConstants.KEYCODE_ADAPTXT:
			showOptionsMenu();
			break;
		case KPTConstants.KEYCODE_NEXT_LANGUAGE:
			toggleLanguage(false, false);
			if(mInputView != null){
				
				mInputView.changeKeyboardBG();
			}
			break;
		case KPTConstants.KEYCODE_PREV_LANGUAGE:
			toggleLanguage(false, true);
			if(mInputView != null){
				
				mInputView.changeKeyboardBG();
			}
			break;
		case KPTConstants.KEYCODE_ATX :
			setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
			handleAtx();
			if(mIsSIPInput)//Bug ID:8093
				updateShiftKeyState(mEditText);
			else
				updateHKBShiftKeyState(mEditText);

			updateSuggestions(true);
			break;
		case KPTConstants.KEYCODE_DELETE_LONGPRESS:
			KPTConstants.mIsNavigationBackwar = true;
			handleDeleteOnLongPress(mIsSIPInput);
			break;
		case KPTConstants.KEYCODE_SPACE_DOUBLE_TAP:
			//TP 15520 Auto-spacing disabled and full-stop entered by a double-tap of space bar
			//if(mAutoSpacePref) {
			if (!KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB){
				boolean currentPrivateMode = mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false);
				mSpaceDoubleTAP = true;
				setPrivateMode(true);
				applyAutoPunctuation(isXIEnabledBeforeSpaceDoubleTap);
				setPrivateMode(currentPrivateMode);
			}
			/*}else {
				onKey(KPTConstants.KEYCODE_SPACE, null, false);
			}*/

			break;
		case KPTConstants.KEYCODE_SHIFT_LONGPRESS:
			if (mKeyboardSwitcher == null || mKeyboardSwitcher.isAlphabetMode()) {//karthik -- fix 5993
				if (mCapsLock) {
					handleShift();
				} else {
					toggleCapsLock();
					updateShiftStateToCore();
					updateSuggestions(true);
				}
			}else {
				mKeyboardSwitcher.toggleShift(mInputView.getFocusedEditText());
			}
			break;
		case KPTConstants.KEYCODE_MODE_CHANGE_SHIFT:
		case KPTConstants.KEYCODE_MODE_CHANGE:
		case KPTConstants.KEYCODE_SHIFTINDIC:
			changeKeyboardMode();
			if(mInputView != null){
				
				mInputView.changeKeyboardBG();
				//Bug 6304
				//Bug 6304
				updateShiftKeyState(mEditText, mInputView.isShifted());
				updateSuggestions(true);
				mInputView.invalidateSpaceKey();
			}

			break;
		case KPTConstants.KEYCODE_DOUBLE_CONSONANT :
			/*int keyCode = updatePreviousCharcterToDoubleConsonant(mNaratgeulPreviousCode);

			if(keyCode != 0) {
				mNaratgeulPreviousCode = keyCode;
				int retcode = mPhoneSyllableComp.deleteReplacePhoneKoreanSyllable();
				handleReplaceKoreanChar(keyCode, retcode);
				mReplacedChar = true; // fix for 8768
				underlineCurrentWord();
				updateSuggestions(true);  
			}*/
			break;	
		case KPTConstants.KEYCODE_RIGHT_ARROW :

			finishComposingText();	

			//Now get the text after the cursor, if the text exists move the position of the cursor else enter a space
			String coreBuffer = mCoreEngine.getCoreBuffer();
			if(coreBuffer != null /*&& coreBuffer.length() > 0*/) {
				int coreCursorPos = mCoreEngine.getAbsoluteCaretPosition();
				if(coreBuffer.toString().length() > coreCursorPos) {
					int nextPos = coreCursorPos+1;
					mCoreEngine.setAbsoluteCaretPosition(nextPos);
					mInputConnection.setSelection(nextPos, nextPos);
				} 
			}	
			break;
		case KPTConstants.KEYCODE_ADD_LINE :

		/*case KPTConstants.KEYCODE_VOICE:
			if(!sSpeechRecognizerAvail) {
				Toast.makeText(mContext, 
						mContext.getResources().getString(R.string.kpt_toast_novoice), 
						Toast.LENGTH_SHORT).show();
				return;
			}

			//Fix for #12471. Voice input should not be enabled for password fields.
			if(!mPasswordText){
				if (sSpeechRecognizerAvail) {
					hideWindow();

					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						((InputMethodService) mContext).requestHideSelf(0);
						mInputView.closing();
					}

					mVoiceRecognitionTrigger.startVoiceRecognition();
				}
			}else{
				Toast.makeText(mContext, 
						mContext.getString(R.string.kpt_Toast_hide_voice_dialog), 
						Toast.LENGTH_SHORT).show(); 
			}
			break;*/
		case KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE : 
			//mInputView.languageSelection();
			showLanguageChangeDialog();
			break;
		case KPTConstants.KEYCODE_DOT_COMMA:
			if (!KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB)
				mIsDoubleTapOnPeriodKey = true;
		case 0:
			break;
		case 9 /* Tab */:
			try { //Leaving the tab key behavior to framework if it > 2.1, else handling in the code
				if (Float.valueOf(Build.VERSION.RELEASE.substring(0, 3)) > 2.1) { 
					((InputMethodService) mContext).sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB);
				} else {
					handleUserInput(primaryCode, mIsSIPInput);
				}
			} catch (Exception e) {
				handleUserInput(primaryCode, mIsSIPInput);
			}
			break;
		case Keyboard.KEYCODE_REPLACE:{
			//fix for 7461
			char keyChar = (char) keyCodes[0];
			if (mPrevCharUpper) {
				if (mKeyboardSwitcher !=null && mKeyboardSwitcher.isAlphabetMode() && Character.isLetter(keyChar)) {
					String charString = Character.toString(keyChar);
					if(mLanguageSwitcher!=null){
						
						charString = charString.toUpperCase(mLanguageSwitcher
								.getInputLocale());
					}
					keyChar = getKeyChar(charString, keyChar);
				}
			}
			if (!KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB || (KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB && isAlphabet(keyChar)) ) {
					boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn && mKeyboardSwitcher!=null
							&& mKeyboardSwitcher.getPhoneKeypadState() == 
							KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;

					if(mIsKeyboardTypeQwerty || atxOn){
						handleReplace(keyChar, mIsSIPInput);
					}else{
						handleReplaceInMultitap(keyChar, mIsSIPInput);
					}
			}

			break;
		}
		// to handle NEXT, ENTER, DONE //Kashyap: bug fix 5754 niraj
		case KPTConstants.KEYCODE_ENTER:
			/*if(bIsKorean) { 
				if(mSyllableComp != null) {
					mSyllableComp.dissolveJamo();
				}
				if(mPhoneSyllableComp != null) {
					mPhoneSyllableComp.dissolveJamo();
				}
			}*/
			//Muntiline Enter support KPTConstants.MULTI_LINES_ADAPTXT
			/*if((mInputView.getFocusedEditText().getInputType() == (InputType.TYPE_TEXT_FLAG_MULTI_LINE) 
			|| (mInputView.getFocusedEditText().getInputType() == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS)
			|| (mInputView.getFocusedEditText().getInputType() == KPTConstants.MULTI_LINES)
			|| (mInputView.getFocusedEditText().getInputType() == KPTConstants.MULTI_LINES_ADAPTXT))){*/

				AdaptxtEditText adaptxtEditor = null;
				if(mInputView != null){
					
					adaptxtEditor = (AdaptxtEditText)mInputView.getFocusedEditText();
				}
				if(adaptxtEditor != null){
					AdaptxtEditTextEventListner eventListner = adaptxtEditor.getEventListner();
					//Check the ime options to check if Enter is Send or New Line
					if(adaptxtEditor.getImeOptions() == EditorInfo.IME_ACTION_UNSPECIFIED 
							|| adaptxtEditor.getImeOptions() == EditorInfo.IME_ACTION_NONE){
						
						int eType = adaptxtEditor.getmEditorType();
						
						if(eType == KPTConstants.MULTILINE_LINE_EDITOR){

							if(mUserSelectedCompleteText){
								mCharAdded = false;
								if(mCoreEngine != null){

									mCoreEngine.resetCoreString();
								}
								mUserSelectedCompleteText = false;
								if(mEditText != null){

									mEditText.setUserSelectedPSomeText(false);
								}
								resetTextBuffer();
							}else if (mEditText != null && mEditText.getUserSelectedPSomeText()) {
								mEditText.setUserSelectedPSomeText(false);
								final int selectionStart = mEditText.getSelectionStart();
								final int selectionEnd = mEditText.getSelectionEnd();
								if(mCoreEngine != null){

									mCoreEngine.replaceString(selectionStart,selectionEnd, "");
								}
								deleteBuffChars(selectionStart, selectionEnd, false);
							}

							// Feed core engine with new line 
							// insert new line at current cursor and update the selection position in 
							// editor 
							/*if(eventListner != null){
								eventListner.onReturnAction(eType);	
							}*/				
							adaptxtEditor.beginBatchEdit();
							finishComposingText();
							String newLine = "\n";
							if(mCoreEngine != null){

								mCoreEngine.insertText(newLine, false, true);
							}

							insertOrAppendBuffer(newLine, false);
							adaptxtEditor.endBatchEdit();
							//sendKeyChar((char)primaryCode);


						}else if(eType == KPTConstants.SINGLE_LINE_EDITOR){
							if(eventListner != null){
								eventListner.onReturnAction(eType);	
							}
						}
						updateShiftKeyState(mEditText);
					}else{
						

						int eType = adaptxtEditor.getImeOptions();

						if(eventListner != null){
							eventListner.onReturnAction(eType);	
						}
						clearCore();
					}
				}
			
			
			/*}else{
				mCustomKeyboard.hideCustomKeyboard();
			}*/
			break;
		/*case KPTConstants.KEYCODE_DISPLAY_CLIPBOARD:
			showClipboard();
			break;*/
		case KPTConstants.KEYCODE_LAUNCH_SETTINGS:
			launchSettings();
			break;
		case KPTConstants.KEYCODE_LAUNCH_QUICK_SETTINGS_DIALOG:
			AdaptxtEditText editor = null;
			if(mInputView != null){
				
				editor = mInputView.getFocusedEditText();
			}
			if (null != editor) {
				AdaptxtKeyboordVisibilityStatusListner editListner = editor.getInputViewListner();
				if(null != editListner){
					// hike will be placing the view
					editListner.showQuickSettingView();
				}
			}
			//mKPTQuickSettingManager.displayQuickSettingsDialog();
			//mKPTQuickSettingManager.launchAdaptxtSettings();
			break;
		case KPTConstants.KEYCODE_LAUNCH_SHARE_DIALOG:
			primaryCode = KPTConstants.KEYCODE_DOT;
			char keyChars = (char) primaryCode;

			mPrevCharUpper = false;
			if (mCapsLock || (mInputView != null && mInputView.isShifted())) {
				mPrevCharUpper = true;
				String charString = Character.toString(keyChars);
				if(mLanguageSwitcher != null){
					
					charString = charString.toUpperCase( mLanguageSwitcher.getInputLocale());
				}
				if(charString.length() > 1) {
					// Some characters don't have proper upper case conversions. It gives 2 upper case characters.
					// So ignore string based case conversion and just use chacarcter based conversion
					// Bug 6294
					keyChars  = Character.toUpperCase(keyChars);
				} else {
					keyChars = charString.charAt(0);
				}
			}
			handleUserInput(keyChars, mIsSIPInput);
			//shareOnFacebook();
			break;
		case KPTConstants.KEYCODE_SPACE_LONGPRESS_START:
			mHandler.sendEmptyMessage(AdaptxtIME.MSG_SPACE_LONGPRESS);
			break;
		case KPTConstants.KEYCODE_SPACE_LONGPRESS_END:
			mHandler.removeMessages(AdaptxtIME.MSG_SPACE_LONGPRESS);
			break;
		default:
			//mExpectingUpdateSelectionCounter ++;
			if (primaryCode != KPTConstants.KEYCODE_ENTER) {
				//mJustAddedAutoSpace = false;
			}
			char keyChar = (char) primaryCode;
			
			
			
			mPrevCharUpper = false;
			if (mCapsLock || mInputView.isShifted()) {
				mPrevCharUpper = true;
				String charString = Character.toString(keyChar);
				if(mLanguageSwitcher != null){
					
					charString = charString.toUpperCase( mLanguageSwitcher.getInputLocale());
				}
				if(charString.length() > 1) {
					// Some characters don't have proper upper case conversions. It gives 2 upper case characters.
					// So ignore string based case conversion and just use chacarcter based conversion
					// Bug 6294
					keyChar  = Character.toUpperCase(keyChar);
				} else {
					keyChar = charString.charAt(0);
				}
			}

			handleUserInput(keyChar, mIsSIPInput);
			break;
		}

		if (mKeyboardSwitcher != null && mKeyboardSwitcher.onKey(primaryCode) ) {
			changeKeyboardMode();
			checkIfGlideShouldBeEnabled();
			mInputView.changeKeyboardBG();
			//Bug 6304
			updateShiftKeyState(mEditText);
			updateSuggestions(true);

		}
		// Reset after any single keystroke
		mEnteredText = null;
		
		if(mIsHindhi && primaryCode!=2 && primaryCode!=-2 ){
			changeKeyboard();
		}
		
	}
	
	
	
	private int getSyllableIndex(){
		
		//Log.e("get Syllable index", "getSyllableIndex");
		
		int SyllableIndex = -1;
		
		
		if(mCoreEngine == null || (mInputView!=null && mInputView.getFocusedEditText().getInputType() == NOT_AN_EDITOR && !mInputView.isShown())){
			return SyllableIndex;
		}else if(mEditText != null){
			
			EditText extracted = mEditText;
			updateExtractedText(extracted);
			if(extracted == null){
				
				return SyllableIndex;
				
			}else if(extracted.getSelectionStart() != extracted.getSelectionEnd()){
				
				return SyllableIndex;
				
			}if(extracted.getSelectionStart()<1){
				
				return SyllableIndex;
				
			}else{
				
				String textInEditor = mEditText.getText().toString();
				String lastSyllable = textInEditor.substring(extracted.getSelectionStart()-1,extracted.getSelectionStart());
				//Log.e("KPT", "lastSyllable "+lastSyllable);
				if(lastSyllable!=null && !lastSyllable.isEmpty()){
					
					if(mHindhiSyllableTable!=null && mHindhiSyllableTable.get(lastSyllable) != null ){
						return mHindhiSyllableTable.get(lastSyllable);
					}
				}
				
			}
			
		}else{
			return SyllableIndex;
		}
		
		
		return SyllableIndex;
	}
	
  private boolean isHindhiLanugae(){
	  
	  if(mLanguageSwitcher!=null){
		  
		  Locale currentLocale = mLanguageSwitcher.getInputLocale();
		  
		  if(currentLocale!=null && currentLocale.getLanguage().equalsIgnoreCase(AdaptxtIME.hindhiLocale)){
				return true;
			}else{
				return false;
			}
		  
	  }else{
		  return false;
	  }
	  
  }
	
	@SuppressWarnings("unused")
	public void showLanguageChangeDialog(){
		
		AlertDialog.Builder builder = new Builder(mContext);
		AdaptxtEditText editor = null;
		if(mInputView != null){
			
			editor = mInputView.getFocusedEditText();
		}
		if (null != editor) {
			AdaptxtKeyboordVisibilityStatusListner editListner = editor.getInputViewListner();
			if(null != editListner){
				// hike will be placing the view
				editListner.showGlobeKeyView();
			}
		}
		
		
		
		/*
		if (null == mContext) {
			//Log.e("KPT", "cant show Language change dialog mcontext is null");
			return;
		}

		if((mOptionsDialog != null && mOptionsDialog.isShowing()) 
				|| (mInputView != null && mInputView.getWindowToken() == null)){
			//Log.e("KPT", "cant show Language change dialog might be already showing orelse input is null");
			return; 
		}

		KPTAdpatxtKeyboard adaptxtKB = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
		if (adaptxtKB == null) {
			//Log.e("KPT", "cant show Language change dialog KPTAdpatxtKeyboard is null");
			return;
		}

		KPTLanguageSwitcher kptLangSwitcher = adaptxtKB.getLanguageSwitcher();
		if(kptLangSwitcher == null) {
			//Log.e("KPT", "cant show Language change dialog kptLangSwitcher is null");
			return;
		}

		SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (kptLangSwitcher.getLocaleCount() == 0
				&& !(sharedPreference.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false))) { //Bug 6920
			// launch Addon manager settings view on long press of space key
			// if there are no addons installed.
			// getOnKeyboardActionListener().launchSettings(KPTAddonManager.class);
		}else if(sharedPreference.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false)) {
			// If core is in maintenance mode then show maintenance mode status in language selection dialog
			mLangSelectionDialog = new KPTIMELanguageSelectionDialog(this, mContext,
					mInputView.getWindowToken(), mLanguageSwitcher, mHandler,
					KPTIMELanguageSelectionDialog.MAINTANENCE_MODE_DIALOG, new ContextThemeWrapper(mContext, R.style.AdaptxtTheme));

			//mLangSelectionDialog.show();
			//	             displayLanguagesSelectionDialog();
		}else {
			kptLangSwitcher.loadLocales(mContext);
			// Show language selection dialog with list of languages
			mLangSelectionDialog = new KPTIMELanguageSelectionDialog(this, mContext,
					mInputView.getWindowToken(), mLanguageSwitcher, mHandler,
					KPTIMELanguageSelectionDialog.NORMAL_MODE_DIALOG, new ContextThemeWrapper(mContext, R.style.AdaptxtTheme));
		}

		ContextThemeWrapper mContextWrapper = new ContextThemeWrapper(mContext, R.style.AdaptxtTheme); 
		AlertDialog.Builder  builder = new AlertDialog.Builder(mContextWrapper);
		builder.setCancelable(true);
		//      builder.setIcon(R.drawable.adaptxt_launcher_icon);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setTitle(mContext.getResources().getString(R.string.kpt_ime_language_selection_dialog_title));

		builder.setView(mLangSelectionDialog.getListView());

		final int color;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			color = mContext.getResources().getColor(R.color.kpt_balck_color_text);
		}else{
			color = mContext.getResources().getColor(R.color.kpt_white_color_text);
		}

		mOptionsDialog = builder.create();
		mLangSelectionDialog.setAlertDialog(mOptionsDialog);

		Window window = mOptionsDialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = mInputView.getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		mOptionsDialog.show();

	*/}
	
	


	public boolean showGlobeKeyView( AlertDialog.Builder builder){
		
		if((mOptionsDialog != null && mOptionsDialog.isShowing()) 
				|| (mInputView != null && mInputView.getWindowToken() == null)){
			//Log.e("KPT", "cant show Language change dialog might be already showing orelse input is null");
			return false; 
		}
		
		mOptionsDialog = builder.create();

		Window window = mOptionsDialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = mInputView.getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		mOptionsDialog.show();
		
		return true;
	}
	
	
	
	public void processChangeLanguageForDialog(final int position){

		if (mLanguageSwitcher == null) {
			return;
		}

		int currentIndex = mLanguageSwitcher.getCurrentIndex();
		if(currentIndex != position) {
			currentIndex  = position;
			boolean isRTL = checkIfRTL(mLanguageSwitcher.getInputLocale());
			mLanguageSwitcher.setCurrentIndex(position);
			mHandler.removeMessages(AdaptxtIME.MSG_RELOAD_KEYBOARDS);
			Message msg = mHandler.obtainMessage(AdaptxtIME.MSG_RELOAD_KEYBOARDS);
			if(isRTL != checkIfRTL(mLanguageSwitcher.getInputLocale())){
				msg.arg1 =1;	
			}else{
				msg.arg1 = 0;
			}
			mHandler.sendMessageDelayed(msg, 100);

			//mOptionsDialog.dismiss();

			String prevLocale = mLanguageSwitcher.getPrevInputLocale().toString();
			String NextLocale = mLanguageSwitcher.getNextInputLocale().toString();
			if(prevLocale.equalsIgnoreCase("th_TH") || NextLocale.equalsIgnoreCase("th_TH")){
				onKey(32, null, false);
			}
		} else {
			//mOptionsDialog.dismiss();
		}
	}
	public GlobeKeyData getGlobeKeyData(){
		GlobeKeyData data = new GlobeKeyData();
		
		if (null == mContext) {
			//Log.e("KPT", "cant show Language change dialog mcontext is null");
			data.setStatus(GlobeKeyData.STATUS_INTERNAL_ERROR);
			return data;
		}
		
		
		if((mOptionsDialog != null && mOptionsDialog.isShowing()) 
				|| (mInputView != null && mInputView.getWindowToken() == null)){
			//Log.e("KPT", "cant show Language change dialog might be already showing orelse input is null");
			data.setStatus(GlobeKeyData.STATUS_INTERNAL_ERROR);
			return data;
		}
		
		KPTAdpatxtKeyboard adaptxtKB = null;
		if(mInputView != null){
			
			adaptxtKB = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
		}
		if (adaptxtKB == null) {
			//Log.e("KPT", "cant show Language change dialog KPTAdpatxtKeyboard is null");
			data.setStatus(GlobeKeyData.STATUS_INTERNAL_ERROR);
			return data;
		}
		
		
		KPTLanguageSwitcher kptLangSwitcher = adaptxtKB.getLanguageSwitcher();
		if(kptLangSwitcher == null) {
			//Log.e("KPT", "cant show Language change dialog kptLangSwitcher is null");
			data.setStatus(GlobeKeyData.STATUS_INTERNAL_ERROR);
			return data;
		}
		
		
		SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		if (kptLangSwitcher.getLocaleCount() == 0
				&& !(sharedPreference.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false))) { //Bug 6920
			data.setStatus(GlobeKeyData.STATUS_MAINTANCE_MODE);
		}else if(sharedPreference.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false)) {
			data.setStatus(GlobeKeyData.STATUS_MAINTANCE_MODE);
		}else{
			
			kptLangSwitcher.loadLocales(mContext);
			data.setStatus(GlobeKeyData.STATUS_OK);
			data.setDisplayLanguages(mLanguageSwitcher.getDisplayLanguages());
			data.setCurrentIndex(mLanguageSwitcher.getCurrentIndex());
			ArrayList<KPTAddonItem> arrayList = mLanguageSwitcher.getUnsupportedLanguageList();
			ArrayList<String> arrayListNew = new ArrayList<String>();
			if (arrayList != null && arrayList.size() > 0) {
				for (KPTAddonItem kptAddonItem : arrayList) {
					arrayListNew.add(kptAddonItem.getDisplayName());
				}
			}
			data.setUnsupportedLangugeList(arrayListNew);
		}
		
		return data;
	}
	
	
	public void closeAnyDialogIfShowing(){
		
		if (null == mOptionsDialog) {
			return;
		}
		
		if (mOptionsDialog.isShowing()) {
			mOptionsDialog.dismiss();
		}
	}



	/**
	 * Used to switch the locale to english when the editor is pwd and user language is other than english
	 */
	private void switchLocale() {
		if(mKeyboardSwitcher == null){
			return;
		}
		mIsHindhi = isHindhiLanugae();
		mKeyboardSwitcher.mShouldLoadEngLocale = !mKeyboardSwitcher.mShouldLoadEngLocale;
		if(mLanguageSwitcher != null){
			
			mLanguageSwitcher.setShoudLoadEngLocale(mKeyboardSwitcher.mShouldLoadEngLocale);
		}
		int currentKeyboardMode = getKeyboardMode(mEditText);
		int imeOptions = mKeyboardSwitcher.getImeOptions();
		mCapsLock = false;
		mKeyboardSwitcher.mMode = currentKeyboardMode;
		reloadKeyboards();
		mKeyboardSwitcher.setKeyboardMode(currentKeyboardMode, imeOptions,true);

	}

	/**
	 * This function is to manuall commit the Thai Word to core 
	 */
	private void handleThaiCommit() {
		if (mCoreEngine != null){ 
			mCharAdded = false; 
			mCoreEngine.executeThaiCommit(); 
		}
		if(mComposing.length() > 0){
			mComposing.setLength(0);
		}
		if (mInputConnection != null) {
			finishComposingText();
		}
		updateSuggestions(true);
	}

	private void handleThaiShift(){ 
		changeKeyboardMode();
		updateSuggestions(true);
	}

	public void sendKeyChar(char primaryCode) {
		if(mInputView == null){
			return;
		}
		EditText attr = mInputView.getFocusedEditText();
		
		//EditorInfo attr = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (attr == null) {
			return;
		}
		//this constant is for evernote(3rd application) editor which gives different action for enter key
		//final int EverNoteEditorConstant = 1073741830;
		int imeOptionKey = attr.getImeOptions()
				& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);

		if( imeOptionKey == EditorInfo.IME_ACTION_DONE || imeOptionKey == EditorInfo.IME_ACTION_GO ||
				imeOptionKey == EditorInfo.IME_ACTION_NEXT || imeOptionKey == EditorInfo.IME_ACTION_SEARCH ||
				imeOptionKey == EditorInfo.IME_ACTION_SEND || imeOptionKey == KPTConstants.TEAM_VIEWR_MAIL_ENTER_ACTION) {
			//super.sendKeyChar(primaryCode);
		} else {
			boolean buildGreaterThanHC = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ;
			//					InputConnection inputConnection = getCurrentInputConnection();
			if(attr != null && mCoreEngine != null){
				attr.beginBatchEdit();
				finishComposingText();
				if(buildGreaterThanHC && mIsUnknownEditor) {
					//handleUserInput(KeyEvent.KEYCODE_ENTER, true);

					//mCoreEngine.insertText("\n", false, true);
					if(isCandidateViewShown()) {
						finishComposingText();
						mCoreEngine.insertText("\n", false, true);
					}
					final long eventTime = SystemClock.uptimeMillis();
					attr.dispatchKeyEvent(new KeyEvent(eventTime, eventTime,
							KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
							KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
					attr.dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
							KeyEvent.ACTION_UP,  KeyEvent.KEYCODE_ENTER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
							KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
				}else {
					commitText("\n", 1);
						if(isCandidateViewShown()) {
							finishComposingText();
							mCoreEngine.insertText("\n", false, true);
						}
				}
				attr.endBatchEdit();
			}
		}
	}

	/**
	 * It checks the string and converts it to uppercase if it has length greater than 1
	 * else returns normal character.
	 * 
	 * @param charString the character string
	 * @param keyChar the character which user pressed
	 * @return returns the character either uppercase/lowercase based on evaluation
	 */
	private char getKeyChar(String charString, char keyChar) {
		if(charString.length() > 1) {
			// Some characters don't have proper upper case conversions. It gives 2 upper case characters.
			// So ignore string based case conversion and just use chacarcter based conversion
			// Bug 6294
			return Character.toUpperCase(keyChar);
		} else {
			return charString.charAt(0);
		}
	}

	
	public void handleDeleteOnLongPress(boolean isSIP) {
		
		if(mEditText == null){
			return;
		}
		
		if (mCoreEngine == null) {
			return;
		}
		
		if(mCurrCursorPos <= 0){
			return;
		}
		
		if(mUserSelectedCompleteText){
			mCoreEngine.resetCoreString();
			mUserSelectedCompleteText = false;
			mEditText.setUserSelectedPSomeText(false);
			resetTextBuffer();
			if(mInputView != null){
				
				if(isSIP)
					updateShiftKeyState(mInputView.getFocusedEditText());
				else
					updateHKBShiftKeyState(mInputView.getFocusedEditText());
			}
			updateSuggestions(true);
			return;
		}else if (mEditText.getUserSelectedPSomeText()) {
			mEditText.setUserSelectedPSomeText(false);
		
			final int selectionStart = mEditText.getSelectionStart();
			final int selectionEnd = mEditText.getSelectionEnd();
			mCoreEngine.replaceString(selectionStart,selectionEnd-1, "");
			deleteBuffChars(selectionStart, selectionEnd, false);
			
			if(mInputView != null){
				
				if(isSIP)
					updateShiftKeyState(mInputView.getFocusedEditText());
				else
					updateHKBShiftKeyState(mInputView.getFocusedEditText());
			}
			updateSuggestions(true);
			return;
		}
		
		handleDeleteLongPress(isSIP);
		
		if(mInputView != null){
			
			if(isSIP)
				updateShiftKeyState(mInputView.getFocusedEditText());
			else
				updateHKBShiftKeyState(mInputView.getFocusedEditText());
		}
		if(!isHideCV){
			updateEditorText(true, true);
		}
	}
	
	private void handleDeleteLongPress(boolean isSIP) {
		if(mGlideSuggestion != null && mGlideSuggestion.getsuggestionString() != null && mInputView != null 
				&& mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
			handleBackSpaceOnGlide(mGlideSuggestion.getsuggestionString(),isSIP);
			return;
		}
		if(mEditText == null){
			return;
		}
		String textInEditor = mEditText.getText().toString();
		if(TextUtils.isEmpty(textInEditor)){
			return;
		}

		setCurrentInputTypeToNormal();

		if (KPTConstants.mXi_enable) {
			executeXiRevert();
			return;
		}

		if (mEditText.getSelectionStart() != mEditText.getSelectionEnd()) {
			String toBeReplaceText = "";
			if(mCoreEngine != null){
				
				if (mEditText.getSelectionStart() > mEditText.getSelectionEnd()) {
					mCoreEngine.replaceString(mEditText.getSelectionEnd(), mEditText.getSelectionStart()-1, toBeReplaceText);
				} else {
					mCoreEngine.replaceString(mEditText.getSelectionStart(), mEditText.getSelectionEnd()-1, toBeReplaceText);
				}
			}
			resetTextBuffer();

			browserTest();
			if(isSIP){
				updateShiftKeyState(mEditText);
			}else{
				updateHKBShiftKeyState(mEditText);
			}
			updateSuggestions(false);
			return;
		}

		if(mEditText.getText() != null || mCoreEngine.getCoreBuffer() != null){
			finishComposingText();


			/* Entered text will not be null only if Smiley is inserted
			 * previously
			 */
			// Use case handling - when last entered text or key is smiley from
			// smiley window and on clicking del, the entire smiley text is delete
			if (mEnteredText != null
					&& sameAsTextBeforeCursor(mEnteredText)) {
				mCoreEngine.removeString(true, mEnteredText.length());
				deleteBuffChars((mCurrCursorPos - mEnteredText.length()), mCurrCursorPos, false);
				browserTest();
			}else if(mTextInBuffer.length()>1 && mCurrCursorPos >= 2 &&
					Character.isSurrogatePair(mTextInBuffer.charAt(mCurrCursorPos-2), mTextInBuffer.charAt(mCurrCursorPos-1))){//for smiley
				mCharAdded = mCoreEngine.removeChar(true);
			
				deleteCurrentChar(false);
				deleteCurrentChar(false);
			}else{
				// mCharAdded is updated from Core in the cases where char is unknown.
				// Ex: User press del when editor is empty, then asking the core whether really delete happened or not
				mCharAdded = mCoreEngine.removeChar(true); // Kashyap: Bug Fix
				deleteCurrentChar(false);				

				if (isPredictionOn()) { // Handle delete when text is
					CurrentWord currentWord = mCoreEngine.getCurrentWord();
					String[] curWord = null;
					if (currentWord != null) {
						curWord = currentWord.currWord;
					}
					if (curWord != null && curWord[0] != null
							&& curWord[0].length() > 0) {
						// remove the word completely
						mCharAdded = mCoreEngine.removeString(true,
								curWord[0].length());
						deleteBuffChars((mCurrCursorPos - curWord[0].length()), mCurrCursorPos, false);
					} else {
						// When Sbar is disabled and there is no current
						// word and hadling the space which need to delete char by char

						StringBuffer buffer = new StringBuffer(mTextInBuffer);
						String revStr = (buffer.reverse()).toString();
						StringTokenizer st = new StringTokenizer(revStr, " ");
						if (st.hasMoreTokens()) {
							int noOfCharsToDelete = st.nextToken().length();
							mCoreEngine.removeString(true,
									noOfCharsToDelete);
							deleteBuffChars((mCurrCursorPos - noOfCharsToDelete+1), mCurrCursorPos, false);
						}
					}
					browserTest();
				} else {

					CharSequence textBeforeCursor = getTextBeforeCursor(1, 0);
					boolean spaceCheck = false;
					if (textBeforeCursor != null) {
						spaceCheck = textBeforeCursor.toString().equals(" ");
					}

					/*
					 * If its a space, just delete it. If not, then get
					 * the length of the first word token and delete it
					 */
					if (spaceCheck) {
						mCoreEngine.removeChar(true);
						deleteCurrentChar(false);
					} else {
						StringBuffer buffer = new StringBuffer(mTextInBuffer);
						String revStr = (buffer.reverse()).toString();
						StringTokenizer st = new StringTokenizer(revStr, " ");
						if (st.hasMoreTokens()) {
							int noOfCharsToDelete = st.nextToken().length();
							mCoreEngine.removeString(true,
									noOfCharsToDelete);
							deleteBuffChars((mCurrCursorPos - noOfCharsToDelete), mCurrCursorPos, false);
						}
					}
				}
			}

			if (isSIP)
				updateShiftKeyState(mEditText);
			else
				updateHKBShiftKeyState(mEditText);

			updateSuggestions(true);
		}
		mJustAddedAutoSpace = false;

		// If the call from HKB, after completion of deletion again we are
		// calling recursively with delay of 100ms
		if (!isSIP && mHandler != null) {
			mHandler.sendEmptyMessageDelayed(MSG_HKB_DELETE_LONGPRESS, 100);
		}
	}

	/**
	 *  It performs the auto punctuation based on the previous character of the current 
	 *  cursor position.
	 *  
	 *  If the cursor previous character is either letter or digit then 
	 *  it applies auto punctuation or else it adds two spaces to the editor and updates the core.
	 *  
	 *  If the editor's text is null, then also we are adding two spaces to the editor 
	 *  and updates the core.
	 */
	private void applyAutoPunctuation(boolean isXIEnabledBeforeSpaceDoubleTap) {

		if (mInputConnection == null || mCoreEngine == null)
			return;
       //handling doubletap of period in multitap
		if(!mAtxStateOn && !mIsKeyboardTypeQwerty){
			if(mHandler.hasMessages(MSG_UPDATE_SUGGESTIONS) ) {

				mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
				handleMultitapInputToCore(mMultitapCharInMessage);
			}
		}

		

		mExpectingUpdateSelectionCounter ++;

		//For Bug No.7281
		CharSequence lastOne;
		if (mJustAddedAutoSpace && mAutoSpacePref) {
			mJustAddedAutoSpace = false;
			lastOne = getTextBeforeCursor(2, 0);
			// remove extra space
			//If the lastOne returns not null then only we can proceed.
			if (lastOne != null) {
				String ch = lastOne.toString();
				if(ch.equalsIgnoreCase(" " + (char)KPTConstants.KEYCODE_SPACE)) {
//					deleteSurroundingText(1, 0);
					mCoreEngine.removeChar(true);
					deleteCurrentChar(false);
				}
			}
		}

		//TP 12366 Undo AutoCorrection not available when AC entered by space key double-tap (=full stop)
		if(isXIEnabledBeforeSpaceDoubleTap) {
			setXiKeyState(isXIEnabledBeforeSpaceDoubleTap);
		}

		lastOne = getTextBeforeCursor(2, 0);
		if (lastOne != null && lastOne.length() == 2){
			char ch = lastOne.charAt(0);
			if (Character.isDigit(ch) || Character.isLetter(ch)) {
//				deleteSurroundingText(1, 0);
				mCoreEngine.removeChar(true);
				deleteCurrentChar(false);
				finishComposingText();
				if(mAutoSpacePref) { //TP 15520. Auto-spacing disabled and full-stop entered by a double-tap of space bar
					onText(". ");
				} else {
					onText(".");
				}
				return;
			}
		}
		onText(" ");		
		mSpaceDoubleTAP = false;
	}

	/** This method called in 2 scenarios
	 * 1) Smiley text selection in the popup smiley window
	 * 2) Key selection from the extension keyboard
	 */
	public void onText(CharSequence text) {
		if(mEditText == null){
			return;
		}
		/*if (mInputConnection == null || TextUtils.isEmpty(text))
			return;*/
		if((mEditText.getmMaxLength() > 0 
				&& (mEditText.getText().length() + text.length()) > mEditText.getmMaxLength())){
			return;
		}
		//Redidrecting to handleUserInput  for conjoint keys
		if (/*mInputView != null && */(mInputView.getCurrentKey() != null &&
				mInputView.getCurrentKey().codes[0] == KPTConstants.KEYCODE_CONJUNCTION )  || PointerTracker.mIsConjuction) {
			onConjunctionKey(text);
			changeKeyboard();
			return;
		}
		//ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		//EditText extracted = mEditText;
		mEditText.beginBatchEdit();
		//finishComposingText();
		//commitTyped(mInputConnection);
		mComposing.setLength(0);

		mCharAdded = true;
		String toBeReplaceText = "";
		if (mCoreEngine == null) {
			return;
		}
		if (mEditText!= null && mEditText.getSelectionStart() != mEditText.getSelectionEnd()) {
			if (mEditText.getSelectionStart() > mEditText.getSelectionEnd()) {
				mCoreEngine.replaceString(mEditText.getSelectionEnd(),
						mEditText.getSelectionStart()-1, toBeReplaceText);
				replaceBufferCharacters(mEditText.getSelectionEnd(), mEditText.getSelectionStart()-1, toBeReplaceText);
			} else {
				mCoreEngine.replaceString(mEditText.getSelectionStart(),
						mEditText.getSelectionEnd()-1, toBeReplaceText);
				replaceBufferCharacters(mEditText.getSelectionStart(), mEditText.getSelectionEnd()-1, toBeReplaceText);
			}
		}

		boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn 
				&& mKeyboardSwitcher.getPhoneKeypadState() == 
				KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;

	
		maybeRemovePreviousPeriod(text);
//		commitText(text.toString(), 1);

		//for onText() method onUpdateSelection will be called only once.
		mExpectingUpdateSelectionCounter ++;
		// Word is learned is boosted 2 times on double tap of period key
		boolean shouldLearn = true;
		if(mSpaceDoubleTAP){
			shouldLearn = false;
		}
		
		mCoreEngine.insertText(text.toString(), atxOn,shouldLearn);
		mEditText.endBatchEdit();

		if (!TextUtils.isEmpty(text)) {
			insertOrAppendBuffer(text.toString(), true);
		}
		//JET later need to check below one  and updateSuggs() call
		if(!isHideCV){
			updateEditorText(false, true);
		}
		
		updateShiftKeyState(mEditText);
		updateSuggestions(true);
		mJustAddedAutoSpace = false;
		mEnteredText = text;
		changeKeyboard();
	}

	private void onConjunctionKey(CharSequence text) {
		String s = text.toString();
		for(int i=0; i<s.length();i++){
			int code =  Character.codePointAt(s, i);
			handleUserInput(code, true);
		}
	}

	/**
	 * Handles key events w.r.t Back Space
	 */
	private void handleBackspace(boolean isSIP) {
		if(mEditText == null){
			return;
		}
		if(mCurrCursorPos <= 0){
			return;
		}
		if(mEditText.getSelectionEnd() == 0){
			return;
		}
		if(mUserSelectedCompleteText){
			mCoreEngine.resetCoreString();
			mUserSelectedCompleteText = false;
			mEditText.setUserSelectedPSomeText(false);
			resetTextBuffer();
			if(mInputView != null){
				
				if(isSIP)
					updateShiftKeyState(mInputView.getFocusedEditText());
				else
					updateHKBShiftKeyState(mInputView.getFocusedEditText());
			}
			updateSuggestions(true);
			return;
		}else if (mEditText.getUserSelectedPSomeText()) {
			mEditText.setUserSelectedPSomeText(false);
		
			final int selectionStart = mEditText.getSelectionStart();
			final int selectionEnd = mEditText.getSelectionEnd();
			mCoreEngine.replaceString(selectionStart,selectionEnd, "");
			deleteBuffChars(selectionStart, selectionEnd, false);
			
			if(mInputView != null){
				
				if(isSIP)
					updateShiftKeyState(mInputView.getFocusedEditText());
				else
					updateHKBShiftKeyState(mInputView.getFocusedEditText());
			}
			updateSuggestions(true);
			return;
		}
		
		handleBackspaceNew(isSIP);
		
		if(mInputView != null){
			
			if(isSIP)
				updateShiftKeyState(mInputView.getFocusedEditText());
			else
				updateHKBShiftKeyState(mInputView.getFocusedEditText());
		}
		if(!isHideCV){
			updateEditorText(true, true);
		}
		
	}

	private void handleBackspaceNew(boolean isSIP){
		
		if(mEditText == null || mCoreEngine == null){
			return;
		}
		
		if(mGlideSuggestion != null && mGlideSuggestion.getsuggestionString() != null && mInputView != null 
				
				&& mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
			handleBackSpaceOnGlide(mGlideSuggestion.getsuggestionString(),isSIP);
			return;
		}
		
		//TODO we can optimize the code later with the slection start and selection end
		// now let all the feature work first santhu.
		
		String textInEditor = mEditText.getText().toString();
		if(textInEditor != null && textInEditor.isEmpty()){
			return;
		}
		StringBuffer tempBuffer = new StringBuffer(textInEditor);
		final int selectionStart = mEditText.getSelectionStart();
		final int selectionEnd = mEditText.getSelectionEnd();
		if (KPTConstants.mXi_enable) {
			executeXiRevert();
			return;
		}
		if (selectionStart != selectionEnd) {
			// user might have selected text 
			
			tempBuffer.replace(selectionStart,selectionEnd, "");
			mCoreEngine.replaceString(selectionStart,selectionEnd, "");
			
			replaceBufferCharacters(selectionStart, selectionEnd, "");
			
//			mEditText.setText(tempBuffer);
//			mEditText.setSelection(selectionStart);
//			updateSuggestions(true);
			return;
		}
		
		boolean lastCharacterOfCurrentWord = false;
		boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn;
		
		boolean deleteTwoChars = false;
		
		if(mGlideSuggestion != null && mGlideSuggestion.getsuggestionString() != null && mInputView != null 
				&& mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
			int len = mGlideSuggestion.getsuggestionString().length();
			deleteBuffChars((mCurrCursorPos - len), mCurrCursorPos, true);
		}else{
			
			if(!isHideCV){
			/**
			 * For smiley delete 
			 */
			if(mTextInBuffer.length()>1 && mCurrCursorPos >= 2 &&
					Character.isSurrogatePair(mTextInBuffer.charAt(mCurrCursorPos-2), mTextInBuffer.charAt(mCurrCursorPos-1))){
				Log.e("KPT","this is a smiley");
				deleteTwoChars = true;
			}
			if(deleteTwoChars){
				//deleteBuffChars((mCurrCursorPos - 3), mCurrCursorPos-1);
				//mCharAdded = mCoreEngine.removeString(true, 2);
				mCharAdded = mCoreEngine.removeChar(true);
				//mCharAdded = mCoreEngine.removeChar(true);
				
				deleteCurrentChar(false);
				deleteCurrentChar(false);
				//Log.e("BHAV","core buffer "+ mCoreEngine.getCoreBuffer());
			}else{
				mCharAdded = mCoreEngine.removeChar(true);
				deleteCurrentChar(false);
			}
			}else{
				mEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			}
		}
		
		
		String currWord = null;
		boolean suggOffsetChanged = false;
		
		if(!atxOn){
			CurrentWord currentWord = mCoreEngine.getCurrentWord();
			if (currentWord != null && currentWord.currWord != null && currentWord.currWord[0] != null ) {
				currWord = currentWord.currWord[0];
				suggOffsetChanged = currentWord.suggestionOffsetChanged;
			}
		}else{
			currWord = mCoreEngine.getPredictiveWord();
		}
		
		
		/*
		 * As per the composing state of the editor, we can get the modified word
		 * from core engine(after performing removeChar()) and directly set it as
		 * composing text (or) finish the composing text in editor and then delete one
		 * character from editor and perform a fresh in-lining(composing state) operation.
		 */
		if(mComposingMode && ((currWord != null && currWord.length() > 0) && !lastCharacterOfCurrentWord && !mIsUnknownEditor)){
			if(suggOffsetChanged){
				// in the use case where "12,ab" is in editor & "ab" alone is in composing state, 
				// when we backspace both "ab", the core engine straight away starts returning a new composing word("12," in this case).
				// so we need to rely on "suggestionOffset" to commit & delete the previous character and 
				// start the composing state on new one.
				finishComposingText();
//				deleteSurroundingText(1, 0);
				if(mIsKeyboardTypeQwerty || mAtxStateOn){
					setPreExistingTextToComposingMode(mInputConnection);
				}
			}else{
//				setComposingText(currWord, 1);
				updateInlining();
			}
		}else{
		
			/*finishComposingText();
			long l = SystemClock.uptimeMillis();
			//Fix for TP #15614. Unable to delete bullets in Evernote
			if(packageName != null && 
					(packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_EVERNOTE)
							|| packageName.equalsIgnoreCase(KPTConstants.PACKAGE_NAME_CHATON)
							|| KPTConstants.PACKAGE_NAME_VIBER.equalsIgnoreCase(packageName))) {
				
				mEditText.onKeyDown(KeyEvent.KEYCODE_DEL, new KeyEvent(l, l, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, 6));
				mEditText.onKeyUp(KeyEvent.KEYCODE_DEL, new KeyEvent(l, l, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, 6));
				//sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
				mCharAdded = false;
			} else {
				
				mEditText.onKeyDown(KeyEvent.KEYCODE_DEL, new KeyEvent(l, l, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, 6));
				mEditText.onKeyUp(KeyEvent.KEYCODE_DEL, new KeyEvent(l, l, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, 6));
				//sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
				
				
				//deleteSurroundingText(1, 0);
			}*/

			/* if(mIsKeyboardTypeQwerty || mAtxStateOn)
				setPreExistingTextToComposingMode(mInputConnection);*/
		}
		//updateSuggestions(true);
		
		if(mIsHindhi){
			//PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
			PointerTracker.mCurrentSyllableIndex = getSyllableIndex();
			changeKeyboard();
		}
	}

	
	private void handleBackSpaceOnGlide(String glideSuggString, boolean isSIP) {
		//mEditText.beginBatchEdit();
		finishComposingText();
		int len = glideSuggString.length();
		//deleteSurroundingText(len, 0);
		mCoreEngine.removeString(true, len);
		
		deleteBuffChars(mCurrCursorPos -len, mCurrCursorPos, false);
		
		/*String corebuffer = mCoreEngine.getCoreBuffer();
		final int acp = mCoreEngine.getAbsoluteCaretPosition();
		
		mEditText.setText("");
		mEditText.setText(corebuffer);
		mEditText.setSelection(acp);*/
		
		mExpectingUpdateSelectionCounter++;
		//mEditText.endBatchEdit();
		if(mInputView != null){
			
			if(isSIP)
				updateShiftKeyState(mInputView.getFocusedEditText());
			else
				updateHKBShiftKeyState(mInputView.getFocusedEditText());
		}

		updateSuggestions(true);
		//No need of Xi key state since we are not support xi any more for this build
		//setXiKeyState(isRevertedWord() && mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
	}
	/**
	 *  Fixed #16111	Case selection is not refreshed upon moving on to Google search widget
	 *
	 **/
	public boolean isSearchEditorAndCanGlide() {

		mIsGlideEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true);
		boolean isEditorActionSearch = false;
		/*EditorInfo attr = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (attr != null) {
			int imeOptionKey = attr.imeOptions & (EditorInfo.IME_MASK_ACTION |EditorInfo.IME_FLAG_NO_ENTER_ACTION);
			if(imeOptionKey == EditorInfo.IME_ACTION_SEARCH) {
				isEditorActionSearch = true; 
			}
		}*/


		boolean canGlideInThisEditor = mInputView.getKeyboard().isGlideSupportedForCurrentKeyboard() 
				&& !mPasswordText;

		return canGlideInThisEditor && isEditorActionSearch && mIsGlideEnabled ;
	}
	/**
	 * Handle keyEvent w.r.t Shift Key
	 */
	private void handleShift() {
		boolean b = isSearchEditorAndCanGlide();
		mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
		checkToggleCapsLock();
		if(mCoreEngine != null){
			if(b) {
				mCoreEngine.setBlackListed(false);
			}
			updateShiftStateToCore();
			if(b) {
				mCoreEngine.setBlackListed(true);
			}
		}
		
		//Bug No 13264 (Issue 2)
		if(mIsGlideEnabled && mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE
				&& mComposingMode) {

		} else {
			updateSuggestions(true);
		}
	}

	/**
	 * Handle keyEvent w.r.t Alt Key
	 */
	private void handleAlt() {
		// mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
		if(mInputView == null || mKeyboardSwitcher == null){
			return;
		}
		boolean currentShiftState = mInputView.isShifted();
		mKeyboardSwitcher.toggleShift(mInputView.getFocusedEditText());
		//Bug 6304
		updateShiftKeyState(mEditText, currentShiftState);
		updateSuggestions(true);
		mInputView.invalidateSpaceKey();
	}

	// For languages like Hindi, Urdu, etc.. "shift" key has no effect and hence it
	// will be used for directly jumping to tertiary layout
	private void jumptoTertiaryKeyboard(){
		if(mKeyboardSwitcher == null){
			return;
		}
		//boolean currentShiftState = mInputView.isShifted();
		mKeyboardSwitcher.jumptoTertiaryKeyboard();
		//Bug 6304
		//updateShiftKeyState(getCurrentInputEditorInfo(), currentShiftState);
		updateSuggestions(true);
	}

	private void updateShiftStateToCore() { // Caps Bug fixes -- karthik
		//Kashyap: Please do not change the calls in this
		//case as all the combinations are tried and then finalized
		if (mCoreEngine != null && !mCoreMaintenanceMode) {
			if (mKeyboardSwitcher != null && mKeyboardSwitcher.isAlphabetMode()) {
				if (mCapsLock == true ) {
					mCoreEngine
					.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_UPPER); //Uppercase
				} else if (!mCapsLock && mInputView.getKeyboard().isShifted()
						&& !isStartOfSentence()) { //Camelcase, but in middle of sentence
					mCoreEngine
					.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_HONOUR_USER_CAPS);
				} else if (!mCapsLock && mInputView.getKeyboard().isShifted()&& mAutoCap) { //Camel case
					mCoreEngine
					.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_SENTENCE_CASE);
				} else {													//Lowercase
					// mCoreEngine.setCapsStates(KPT_SUGG_STATES.
					// KPT_SUGG_HONOUR_USER_CAPS);
					if(mAutoCap){
						mCoreEngine
						.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_LOWER);
					}else{
						mCoreEngine
						.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_AUTO_CAPS_OFF);
					}
				}
			} else {
				if (isStartOfSentence() && !mCapsLock && mAutoCap) {
					// Fix for TP 7144
					if(mInputView != null && !mInputView.getKeyboard().isShifted())
						mCoreEngine
						.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_LOWER);
					else
						mCoreEngine
						.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_SENTENCE_CASE);
				}
			}
		}
	}

	/**
	 * This method updates the shift state of HKB to core.
	 * 
	 */
	private void updateHKBShiftStateToCore() { 
		if (mCoreEngine != null && !mCoreMaintenanceMode) {
			if (mhardShift == HKB_STATE_LOCK) {
				mCoreEngine.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_UPPER); // Uppercase
			} else if (mhardShift == HKB_STATE_ON && !isStartOfSentence()) { // Camelcase, but in middle of sentence
				mCoreEngine
				.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_HONOUR_USER_CAPS);
			} else if (mhardShift == HKB_STATE_ON) { // Camel case
				mCoreEngine
				.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_SENTENCE_CASE);
			} else { // Lowercase
				if(mAutoCap){
					mCoreEngine.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_FORCE_LOWER);
				}else{
					mCoreEngine.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_AUTO_CAPS_OFF);
				}
			}
		} /*else {
			if (isStartOfSentence() && mhardShift != HKB_STATE_LOCK) {
					mCoreEngine
							.setCapsStates(KPT_SUGG_STATES.KPT_SUGG_SENTENCE_CASE);
				}
		}*/
	}

	/**
	 * This function is used to test whether the editor type is browser or not. 
	 * If browser update the extracted text from editor to in-order to maintain sync of text 
	 * For browser, onupdateselection() will called only on jump operation
	 */
	private void browserTest() {
		if(isBrowser && mInputConnection != null){
			ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(),	0);
			updateExtractedText(extracted); 
		}
	}

	/**
	 * (mBrowserTestOnce == true? !isBrowser:true) this logic is for handling 
	 * first user input before browsertest has been done (or timeout happened).
	 * This logic always returns true except in this case - browser editor has some text
	 * and user switches to this editor and does jump operation.
	 * @return
	 */
	private synchronized boolean isCharAdded() {
		return mCharAdded && !isBrowser;
	}

	private synchronized boolean isSuggestionSelected() {
		return mCandidateSelected && !isBrowser;
	}

	private synchronized boolean isHKB() {
		return isHKb && !isBrowser;
	}

	private synchronized void updateExtractedText(ExtractedText extracted){
		if(extracted != null && extracted.text != null) {
			mExtractedLength = extracted.text.length();
			mExtractedText = new SpannableStringBuilder(extracted.text);			
		} else {
			mExtractedText = null;
			mExtractedLength = -1;
		}
	}
	
	private synchronized void updateExtractedText(EditText extracted){
		if(extracted != null && extracted.getText() != null) {
			mExtractedLength = extracted.getText().length();
			mExtractedText = new SpannableStringBuilder( extracted.getText());			
		} else {
			mExtractedText = null;
			mExtractedLength = -1;
		}
	}

	public void dismissCandidate(int primaryCode) {
		/*if (mInputConnection == null) {
			return;
		}*/
		if (mCoreEngine == null) {
			return;
		}
			//When dismiss is tapped we just need to add space to the core so 
			//instead of calling addchar it is enough to call inserttext() and commit the same to editor.
			finishComposingText();
			commitText(" ", 1);
			mCoreEngine.insertText(" ", mAtxStateOn, !mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
			/* To make sure that the new word composition is started properly*/
			updateSuggestions(true);

	}

	public void handleUserInput(int primaryCode, boolean isSIP){
		if(!KPTConstants.mIsNavigationBackwar){
			KPTConstants.mIsNavigationBackwar = true;
		}
		
		if(mIsKeyboardTypeQwerty){
			//handleQWERTYLayoutInput(primaryCode, isSIP);				
			handleQWERTYLayoutInput(primaryCode, isSIP);				
		}
		else{
			handlePhoneLayoutInput(primaryCode, isSIP);
		}

	}

	private KPTCorrectionInfo addChar(char composingChar) {
		if(mCoreEngine == null) {
			return null;
		}
		KPTCorrectionInfo info;
		//KPTCorrectionInfo info = mCoreEngine.addCharTwo(composingChar, false);
		CharSequence txtBeforeCursor = getTextBeforeCursor(1, 0);
		if(txtBeforeCursor != null){
			info = mCoreEngine.addChar(composingChar,
					txtBeforeCursor.toString().equalsIgnoreCase(
							"" + (char)KPTConstants.KEYCODE_SPACE)? true : false , 
									mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState()
									, false);
		} else {
			info = mCoreEngine.addChar(composingChar,
					false , mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState()
					, false);
		}

		return info;
	}

	/**
	 * Handle key event w.r.t SIP and Hard key 
	 * 
	 * The difference between this method and handleUserInput(int primaryCode) is only in editor updation.
	 * This method doesn't update the editor, it leaves the editor updating to the framework 
	 * for HKB since the cursor change is handled by framework for Alt & Shift.
	 * 
	 * @param primaryCode - Entered keyCode
	 * @param isSIP - Indicates whether the input is from SIP or not (HKB)
	 */
	public void handleQWERTYLayoutInput(int primaryCode, boolean isSIP){
		
		/*EditText editText = mInputView.getFocusedEditText();
		
		StringBuffer tempBuffer = new StringBuffer(editText.getText().toString());
		tempBuffer.append(String.valueOf((char)primaryCode));
		editText.setText(tempBuffer);
		editText.setSelection(tempBuffer.toString().length());

		updateSuggestions();*/
		
		
		if(mCoreEngine == null || mInputView == null || mInputView.getFocusedEditText() == null
				|| (mInputView.getFocusedEditText().getInputType() == NOT_AN_EDITOR
				&& !mInputView.isShown())){
			return;
		}

		mCharAdded = true;
		mCounter = 0;
		KPTCorrectionInfo info = null;
		boolean isTextSelected = false;

		//if(mInputConnection != null){
		if(mEditText != null){
			//ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			EditText extracted = mEditText;
			if(shouldAddExtraSpace(primaryCode)) {
				handleUserInput(KPTConstants.KEYCODE_SPACE, true);
				//mCoreEngine.insertText(" ", false, true);
				//commitText(" ", 1);
			}

			updateExtractedText(extracted);
			if(mUserSelectedCompleteText){
				mCharAdded = false;
				isTextSelected = false; 
				//	mEditText.setText("");
				mCoreEngine.resetCoreString();
				mUserSelectedCompleteText = false;
				mEditText.setUserSelectedPSomeText(false);
				resetTextBuffer();
				
			}else if (mEditText.getUserSelectedPSomeText()) {
				mEditText.setUserSelectedPSomeText(false);
				final int selectionStart = mEditText.getSelectionStart();
				final int selectionEnd = mEditText.getSelectionEnd();
				//Bug Fix: 20641. Adding -1 for selectionEnd.
				mCoreEngine.replaceString(selectionStart,selectionEnd - 1, "");

				deleteBuffChars(selectionStart, selectionEnd, false);
			}
			
			mEditText.beginBatchEdit();
			
			int beforeLen = 0;
			int afterLen = 0;
			if(!isTextSelected && !mPasswordText ) {
				info = mCoreEngine.addCharTwo((char)primaryCode,false);
				mInputInfo = mCoreEngine.getInputInfo();
				//beforeLen = getExtractedTextLength();
				beforeLen = mEditText.getText().length();

				if(info == null){
					String inputStr = String.valueOf((char) primaryCode);
					insertOrAppendBuffer(inputStr, true);
					//setCurrentCharToComposing(primaryCode);
				} else {
					applyAutoCorrectOnKeyPress(info);
				}
				afterLen = mEditText.getText().length();
			} else { // textSelected == true (i.e. Text has been selected and user now presses a key)
				/*//beforeLen = mEditText.getText().length();
				mEditText.setText("");
				commitText(String.valueOf((char)primaryCode), 1);
				//afterLen = mEditText.getText().length();
				mCoreEngine.resetCoreString(); // TP 8097
				
				mUserSelectedCompleteText = false;*/

			}

//			removeExtraCharsFromCoreIfRequired(beforeLen, afterLen, null);
			//mInputConnection.endBatchEdit();
			resetAccDismiss();
			if(isSIP)
				updateShiftKeyState(mEditText);
			else 
				updateHKBShiftKeyState(mEditText);
			updateSuggestions(true);
			mInputInfo = null;
		}
		//CurrentWord currentWordObj = null;
		//currentWordObj = mCoreEngine.getCurrentWord();
		//currentWordObj = null;
	}

	/**
	 * for editor which has maxLength property ie.. editor text size limit, when the
	 * character is not inserted in the editor remove it from the core also.
	 *  
	 *  TP 13847 Fields where character limit applies (i.e. Gallery) alters suggestions inserted
	 *  
	 * @param beforeLen
	 * @param afterLen
	 */

	private void removeExtraCharsFromCoreIfRequired(int beforeLen, int afterLen, String extracted) {
		if(mIsUnknownEditor /*|| extracted == null*/ || mCoreEngine == null) {
			return;
		}

		if(beforeLen == afterLen) {
			////Log.e("kpt","shouldRemoveExtraCharsFromCore --> Both are same");
			String coreBuffer = mCoreEngine.getCoreBuffer();
			int coreSize = 0;
			if(coreBuffer != null) {
				coreSize = mCoreEngine.getCoreBuffer().length();
			}
			int editorLength = afterLen ;
			int charsToRemove = coreSize - editorLength;
			if(charsToRemove == 1) {
				mCoreEngine.removeChar(true);
			//	deleteCurrentChar();
			} else if(charsToRemove > 1) {
				mCoreEngine.removeString(true, charsToRemove);
				if(mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
					setCurrentInputTypeToNormal();
				}
			//	deleteBuffChars(mCurrCursorPos -charsToRemove, mCurrCursorPos);
//				updateSuggestions(true);
			} else {
				//may be -ve or zero value ignore
			}
		} else {
			////Log.e("kpt","shouldRemoveExtraCharsFromCore --> Both are NOT same");
		}
	}

	private void setCurrentCharToComposing(int code){
		////Log.e("VMC", "setCurrentCharToComposing ---> "+String.valueOf((char) code));
		/**
		 * If the char entered is space finish whayever is in composing anf commit space.
		 * For Ex: user has already entered "srikanth" and it is in composing and now user enters space.
		 */
		if (code == KPTConstants.KEYCODE_SPACE || code == KeyEvent.KEYCODE_SPACE) {
			finishComposingText();
			commitText(String.valueOf((char) code), 1);
			mExpectingUpdateSelectionCounter ++;
			return;
		}/*else{ //TP: #20271
			mComposingMode = true;
		}*/

		/**
		 * Consider the following use-case:
		 * Text "varun" has already been enterd.
		 * 1. User jumps and places cursor in between the word "varun"(say in between 'a' and 'r') 
		 * 2. User now enters a punctuation(comma, dot, exclamation etc) or a NORMAL CHAR, then following rules are followed by core:
		 * 
		 * For some punctuations like dot and @ word will still be in compostion 
		 * i.e 'va.' or 'va@' will underlined or is in compostion.
		 * In such case isSuggestionOffsetChanged wil be false
		 * 
		 * For some punctuations like comma and exclamation, word will not be in compostion and that punctuation will be commited
		 * i.e.  va,run or va!run will be shown without any underlining or composition.  
		 * In such case isSuggestionOffsetChanged wil be true
		 * 
		 * For a normal character entry word will be in composition
		 * i.e. Say 'h' is entered in between 'a' and 'r', then vah will be in composing or underlined
		 * In such case isSuggestionOffsetChaned will be false
		 * 
		 * So we depend on isSuggestionOffsetChanged to determine if the text before cursor needs to be in composing or not
		 */
		CurrentWord currentWordObj = null;
		currentWordObj = mCoreEngine.getCurrentWord();
		boolean isSugOffChanged = false;
		if(currentWordObj != null ) {
			isSugOffChanged = currentWordObj.suggestionOffsetChanged;
			if(isSugOffChanged) {
				finishComposingText();
				
				mEditText.setText(mCoreEngine.getCoreBuffer(),true);
				mEditText.setSelection(mCoreEngine.getAbsoluteCaretPosition(),true);
				
				//commitText(String.valueOf((char) code), 1);
				mExpectingUpdateSelectionCounter ++;
			} else {
				/**
				 * Select sugg THE then enter punctuation ' now THE' will not be in composition 
				 * Now insert Y, the entire txt THE'Y should be in composing.
				 * For whcih we check if txtBeforeCur is present (which is not space) and not in compositon, upon which we call
				 * setPreExistingTextToComposingMode
				 */
				if(isTxtBeforeCursorPresentAndNotSpace(1) && !mComposingMode){
					commitText(String.valueOf((char)code), 1);
					mExpectingUpdateSelectionCounter ++;
					setPreExistingTextToComposingMode(mInputConnection);
				} else {
					String[] currWordArray = currentWordObj.currWord;
					if(currWordArray != null) {
						String currentWord = currWordArray[0];
						if(currentWord != null && currentWord.length()>0){
							setComposingText(currentWord, 1);
						}
						else
						{
							// Fix for TP Item - 12733
							finishComposingText();
							commitText(String.valueOf((char) code), 1);
							//mExpectingUpdateSelectionCounter ++;	
						}
					}
				}

			}
		} else { // currentWordObj is null. Rare use-case, probably when no dict installed or core init failed
			finishComposingText();
			commitText(String.valueOf((char) code), 1);
			mExpectingUpdateSelectionCounter ++;
		}

		if(code == KPTConstants.KEYCODE_SPACE && mJustAddedAutoSpace) {
			//dont make it false. made this change for space multi tap
		} else {
			mJustAddedAutoSpace = false;
		}
	}
	/**
	 * Returns true if txtBeforeCursor exists and is NOT SPACE, false otherwise
	 * 
	 * @param noOfPrevChars: The expected length of the text.
	 * @return true or false
	 */
	private boolean isTxtBeforeCursorPresentAndNotSpace(int noOfPrevChars) {
		boolean isTxtBeforeCursorPresent = false;
		CharSequence txtBeforeCursor = null;
		txtBeforeCursor = getTextBeforeCursor(noOfPrevChars, 0);
		if(txtBeforeCursor != null) {
			String strBeforeCur = txtBeforeCursor.toString();
			if(strBeforeCur != null &&
					!strBeforeCur.equalsIgnoreCase("") &&
					!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE) && 
					!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_ENTER)) {

				isTxtBeforeCursorPresent = true;
			}
		}
		return isTxtBeforeCursorPresent;
	}

	/** 
	 * Update Editor w.r.t core as auto suggestion has been done
	 * probably on pressing some puncutations like space, comma etc 
	 * 
	 */
	private void applyAutoCorrectOnKeyPress(KPTCorrectionInfo info){

		// Handling all the auto backspacing & AC selection here
		/*finishComposingText();
		deleteSurroundingText(info.getCharsToRemoveBeforeCursor(), info.getCharsToRemoveAfterCursor());*/
		//for lg email editor after deleting and commiting, deletion is not
		//working. so finishing the composing text
		/*if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_LG_EMAIL) {
			finishComposingText();
		}
		commitText(info.getModString(), 1);*/
		//Commenting the following line to fix the bug#12037
		//Because onUpdateSelection is not getting called after committing the text in the above line.
		//mExpectingUpdateSelectionCounter ++;
		
		/*String corebuffer = mCoreEngine.getCoreBuffer();
		final int acp = mCoreEngine.getAbsoluteCaretPosition();
		mEditText.setText("");
		mEditText.setText(corebuffer);
		mEditText.setSelection(acp);*/
		
		mComposingMode = false;
		
		// Fix for TP 9120
		if(mSuggList != null && mSuggList.size() > 0
				&& mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION){
			setXiKeyState(true);
		}
		
		int startIndex = mCurrCursorPos - info.getCharsToRemoveBeforeCursor();
		int endIndex = mCurrCursorPos + info.getCharsToRemoveAfterCursor();
		replaceBufferCharacters(startIndex, endIndex, info.getModString());
		
		mJustAddedAutoSpace = true;
	}
	
	private void syncBufferText(String strInEditor, int strInEditorLength) {
		mTextInBuffer.replace(0, mTextInBuffer.length(), strInEditor);
		mCurrCursorPos = strInEditorLength;
		if (TextUtils.isEmpty(mTextInBuffer) && mEditText != null) {
			updateShiftKeyState(mEditText);
		}
		updateEditorText(true, true);
	}
	

	private void deleteBuffChars(int selectionStart, int selectionEnd, boolean isUpdateSuggs) {

		if (selectionStart < 0 || selectionStart > selectionEnd || selectionEnd > mTextInBuffer.length()) {
			//do nothing
//			Log.e("JET", "deleteBuffChars exception case ");
		}else{
			mTextInBuffer.delete(selectionStart, selectionEnd);
			if(!isHideCV){
				updateEditorText(isUpdateSuggs, true);
			}else{
		
				mEditText.getText().delete(selectionStart, selectionEnd);
			}

			mCurrCursorPos = selectionStart; //check whether forward & backward selection
		}
	}
	
	private void resetTextBuffer() {
		mTextInBuffer.setLength(0);
		mCurrCursorPos = 0;
		
		updateEditorText(true, false);		
	}
	
	private void insertOrAppendBuffer(String inputStr, boolean isUpdateInlining) {
//		mComposingMode = false;
		//we are initializing globally, so we dont get nullpointer
		if(!isHideCV){
		if (mCurrCursorPos == mTextInBuffer.toString().length()) {
			mTextInBuffer.append(inputStr);
			mCurrCursorPos = mTextInBuffer.toString().length();
			updateEditorText(true, isUpdateInlining);
		}else{
			 if (mCurrCursorPos < 0 || mCurrCursorPos > mTextInBuffer.toString().length()){
				 //do nothing
//				 Log.e("JET", "insertOrAppendBuffer exception case ...");
			 }else{
				 mTextInBuffer.insert(mCurrCursorPos, inputStr);
				 mCurrCursorPos = mCurrCursorPos + inputStr.length();
				 updateEditorText(true, isUpdateInlining);
			 }
		}
		}else{
			
			
			if (mCurrCursorPos == mTextInBuffer.toString().length()) {
				mTextInBuffer.append(inputStr);
				mCurrCursorPos = mTextInBuffer.toString().length();
				//updateEditorText(true, isUpdateInlining);
		
				mEditText.getText().append(inputStr);
				
			}else{
				 if (mCurrCursorPos < 0 || mCurrCursorPos > mTextInBuffer.toString().length()){
					 //do nothing
//					 Log.e("JET", "insertOrAppendBuffer exception case ...");
				 }else{
					 mTextInBuffer.insert(mCurrCursorPos, inputStr);
	
						mEditText.getText().insert(mCurrCursorPos, inputStr);
				 }
			}
		}
	}
	
	private void deleteNextChar() {
		if (mCurrCursorPos > 0 && mTextInBuffer.length() > mCurrCursorPos) {
			mTextInBuffer.deleteCharAt(mCurrCursorPos);
//			mCurrCursorPos++;
			if(!isHideCV){
				updateEditorText(true, true);
			}
		}	
	}
	private void deleteCurrentChar(boolean isUpdateSuggs) {
		if (mCurrCursorPos > 0) {
			mCurrCursorPos--;
			mTextInBuffer.deleteCharAt(mCurrCursorPos);
			//updateEditorText(isUpdateSuggs, true);
			if(!isHideCV){
				updateEditorText(isUpdateSuggs, true);
			}else{
				int tempcur = mCurrCursorPos;
			
				mEditText.getText().delete(mCurrCursorPos, mCurrCursorPos + 1);
			}
		}		
	}

	private void replaceBufferCharacters(int startIndex, int endIndex, String replaceString) {
		if (startIndex < 0 || endIndex < 0 || startIndex > endIndex
				|| endIndex > mTextInBuffer.length()) {
			//do nothing
//			Log.e("JET", "replaceBufferCharacters exception case ...");
		}else{
			StringBuffer sbf = new StringBuffer(mTextInBuffer);
			sbf.replace(startIndex, endIndex, replaceString);
			
			if(mEditText.getmMaxLength() > 0 
					&& sbf.length() > mEditText.getmMaxLength()){
				postUpdateSuggestions(10);
				return;
			}
			
			mTextInBuffer.replace(startIndex, endIndex, replaceString);
			//start index + added mod string length
			mCurrCursorPos = startIndex + replaceString.length();
			if(!isHideCV){
				updateEditorText(true, true);
			}else{
			
				mEditText.getText().replace(startIndex, endIndex, replaceString);
			}
		}
		
	}


	public void handlePhoneLayoutInput(int primaryCode, boolean isSIP){

		if(mCoreEngine == null || mInputView == null || (mInputView.getFocusedEditText().getInputType() == NOT_AN_EDITOR && !mInputView.isShown())){
			return;
		}
        if(mEditText == null){
        	return;
        }
		mCharAdded = true;
		KPTCorrectionInfo info = null;
		mCounter = 0;
		boolean isTextSelected = false;

		if(mHandler != null){
			if(mAtxStateOn){
				mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			}else if(mHandler.hasMessages(MSG_UPDATE_SUGGESTIONS) && mMultitapCharInMessage != primaryCode) {
				mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
				handleMultitapInputToCore(mMultitapCharInMessage);
				if(!isStartOfSentence() && !mCapsLock  ) {
					primaryCode = Character.toLowerCase(primaryCode);
				}
			}
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}

		if(mInputView.getFocusedEditText() != null){
			//EditText editText = mInputView.getFocusedEditText();
			//editText.beginBatchEdit();
			//String extracted = editText.getText().toString();//mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);

			if(mUserSelectedCompleteText){
				mCharAdded = false;
				isTextSelected = false;
			//	mEditText.setText("");
				mCoreEngine.resetCoreString();
				mUserSelectedCompleteText = false;
				mEditText.setUserSelectedPSomeText(false);
				resetTextBuffer();
			}else if (mEditText.getUserSelectedPSomeText()) {
				mEditText.setUserSelectedPSomeText(false);
				final int selectionStart = mEditText.getSelectionStart();
				final int selectionEnd = mEditText.getSelectionEnd();
				mCoreEngine.replaceString(selectionStart,selectionEnd, "");
				
				deleteBuffChars(selectionStart, selectionEnd, false);
			}
				

			int phoneKeyboardState = mKeyboardSwitcher.getPhoneKeypadState();

		/*	boolean atxOn = mAtxStateOn && !mIsPopupChar
					&& mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL
					&& !mInputView.isCurrentKeyLongPressed()  //TP 8108. checking if long pressed or not
					&& mKeyboardSwitcher != null ? phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET:false;*/

			//Bug 15859
			boolean atxOn = mAtxStateOn &&
			mUnknownEditorType != KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL
					&& !mInputView.isCurrentKeyLongPressed();  //TP 8108. checking if long pressed or not			

			
			//Disabling the space correction for multi tap mode
			if(!mAtxStateOn && !mJustAddedAutoSpace){
				mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
						mAutoCorrectOn, mCorrectionMode, mSpaceCorrectionSuggestionsOn);
			}
			int beforeLen = 0;
			int afterLen = 0;
			//handling popchar input as normal input in both multitap and predictive mode
			if(atxOn || mIsPopupChar || phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET){


				if(!isTextSelected ) {
					if(phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET){
						atxOn = false;
					}else{
						atxOn = !mIsPopupChar;
					}
					
					info = mCoreEngine.addCharTwo((char)primaryCode,atxOn);
					beforeLen = getExtractedTextLength();
					if(info == null){
						if (atxOn || mIsPopupChar) {
							setCurrentCharToComposingforPhone(primaryCode,phoneKeyboardState);
						}else{
							insertOrAppendBuffer(String.valueOf((char) primaryCode), false);
						}
					} else {
						applyAutoCorrectOnKeyPress(info);
					}
					afterLen = getExtractedTextLength();
				} else {
					/*mEditText.setText("");
					//beforeLen = getExtractedTextLength();
					commitText(String.valueOf((char)primaryCode), 1);
					mCoreEngine.resetCoreString(); // TP 8097
					mJustAddedAutoSpace = false;
					//afterLen = getExtractedTextLength();
					mUserSelectedCompleteText = false;*/
				}

			}else {

				if(!isTextSelected ) { 
					setCurrentCharToComposingforPhone(primaryCode,phoneKeyboardState);
					mExpectingUpdateSelectionCounter++;

				} else {

					/*commitText(String.valueOf((char)primaryCode), 1);
					mCoreEngine.resetCoreString(); 
					mJustAddedAutoSpace = false;
					mUserSelectedCompleteText = false;
*/
				}
			}
			//editText.endBatchEdit();
			browserTest();
			if(atxOn){

//				removeExtraCharsFromCoreIfRequired(beforeLen, afterLen, extracted);
			}
		}
		resetAccDismiss();

		updateShiftKeyState(mInputView.getFocusedEditText());
//16608 	Double punctuation marks for the revert candidate 
		if(mAtxStateOn || mIsPopupChar || (mKeyboardSwitcher != null && mKeyboardSwitcher.getPhoneKeypadState() != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET)) {
			updateSuggestions(true);
		} else {
			mMultitapCharInMessage = (char)primaryCode;
			updateSuggestions(true);
			postUpdateSuggestions(MULTITAP_SUGGESTION_INTERVAL,(char)primaryCode);
		}
		//Reverting the space correction for multi tap mode
		if(!mAtxStateOn && !mJustAddedAutoSpace){
			mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
					mAutoCorrectOn, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
		}
	}

	private void setCurrentCharToComposingforPhone(int primaryCode,int phoneKeyboardState) {
		// Handling 12 key predictive and multitap separately as multitapping doesn't require
		// inlining functionality
		if(!(primaryCode == KPTConstants.KEYCODE_SPACE) 
				&& !mIsPopupChar 
				&& phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET && mAtxStateOn){

			// TP 8276,8277 making this check to make sure that text is not appended when there is partial in-lining
			if(isTxtBeforeCursorPresentAndNotSpace(1) && !mComposingMode){
				
				String inputStr = String.valueOf((char) primaryCode);
				insertOrAppendBuffer(inputStr, true);
//				setPreExistingTextToComposingMode(mInputConnection);
			}else{
				String currWord = mCoreEngine.getPredictiveWord();
				if(currWord != null && currWord.length() > 0) {
					
					
					final int coreACP = mCoreEngine.getAbsoluteCaretPosition();
					final int compositionTextLength = currWord.length();
//					String stringInEditor = mEditText.getText().toString();
//					StringBuffer completeString = new StringBuffer(stringInEditor);
					
					int cursorPositionInEditor = mEditText.getSelectionStart();
					int start = coreACP - compositionTextLength ;
					deleteBuffChars(start, cursorPositionInEditor, false);
					insertOrAppendBuffer(currWord, true);
					//setComposingText(currWord, 1);
				}
			}
		}else{
			String currentWord = null;
			if(mIsUnknownEditor && phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) {
				CurrentWord currWordObj =  mCoreEngine.getCurrentWord();
				String[] currentWordArray = null;
				if(currWordObj != null) {
					currentWordArray = currWordObj.currWord;
				}
				if(currentWordArray != null){
					currentWord = currentWordArray[1];
				}
			}
			if(mIsUnknownEditor
					&& phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET 
					&& currentWord != null 
					&& currentWord.length() > 0) {

				if(isTxtBeforeCursorPresentAndNotSpace(1)) {
					deleteBuffChars(mCurrCursorPos -currentWord.length(), mCurrCursorPos, true);
					//deleteSurroundingText(currentWord.length(), 0);
				}
				mExpectingUpdateSelectionCounter ++;
				//setComposingText(currentWord+String.valueOf((char)primaryCode), 1);
			} else {
				finishComposingText();
				
				String inputStr = String.valueOf((char)primaryCode);
//				insertOrAppendBuffer(inputStr);
				
				if (mCurrCursorPos == mTextInBuffer.toString().length()) {

					mTextInBuffer.append(inputStr);
					mCurrCursorPos = mTextInBuffer.toString().length();
					if(isHideCV){
						mEditText.getText().append(inputStr);
					}

					//					updateEditorText(false);
				}else{
					 if (mCurrCursorPos < 0 || mCurrCursorPos > mTextInBuffer.toString().length()){
						 //do nothing
//						 Log.e("JET", "insertOrAppendBuffer exception case ...");
					 }else{
						 mTextInBuffer.insert(mCurrCursorPos, inputStr);
						 if(isHideCV){
								 mEditText.getText().insert(mCurrCursorPos, inputStr);
							
							}
						 mCurrCursorPos = mCurrCursorPos + inputStr.length();

					 }
				}
				
				if (primaryCode == KPTConstants.KEYCODE_SPACE) {
					if(!isHideCV){
						mEditText.setText(mTextInBuffer.toString(),true);
						mEditText.setSelection(mCurrCursorPos,true);
					}
				}else if (!mIsPopupChar) {
					checkMultitpInitialInlining();
				}else{
					updateEditorText(true, true);
				}
				
				//commitText(String.valueOf((char)primaryCode), 1);
				if(mAtxStateOn){
					mExpectingUpdateSelectionCounter ++;
				}
			}

		}

		if(primaryCode == KPTConstants.KEYCODE_SPACE && mJustAddedAutoSpace) {
			//dont make it false. made this change for space multi tap
		} else {
			mJustAddedAutoSpace = false;
		}
	}

	
	private void checkMultitpInitialInlining() {
		boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn && mKeyboardSwitcher != null 
				&& mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
		
		if (mIsKeyboardTypeQwerty || atxOn || mCoreEngine == null || mEditText == null) {
			return;
		}

		String currWord = null;
		CurrentWord currentWord = mCoreEngine.getCurrentWord();
		if (currentWord != null && currentWord.currWord != null && currentWord.currWord[0] != null ) {
			currWord = currentWord.currWord[0];
		}

		if (currWord == null) {
			return;
		}


		int curWordLen = currWord.length() + 1;
		//			int curWordLen = currWord.length();
		if (mCurrCursorPos >= curWordLen && mTextInBuffer.length() >= mCurrCursorPos) {
			SpannableString spannableString = processUnderline(mCurrCursorPos - curWordLen, mCurrCursorPos, mTextInBuffer.toString());
			mComposingMode = true;
			mEditText.setText(spannableString,true);
			mEditText.setSelection(mCurrCursorPos,true);
		}else{
			mEditText.setText(mTextInBuffer.toString(),true);
			mEditText.setSelection(mCurrCursorPos,true);
		}
	}
	/*public void handlePhoneLayoutInput(int primaryCode, boolean isSIP){
		if(mCoreEngine == null || (getCurrentInputEditorInfo().inputType == NOT_AN_EDITOR && !mInputView.isShown())){
			return;
		}
		synchronized (this) {
			mCharAdded = true;
		}
		mCharAdded = true;
		boolean accState;

		if(!isInputViewShown()){
			accState = mHardKBCandidateContainer.getAccDsmissState();

		}else{
			accState = mCandidateViewContainer.getAccDsmissState();
		}

		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}

		boolean isSpace = false;
		boolean isTextSelected = false;
		if(mInputConnection != null){
			mInputConnection.beginBatchEdit();
			ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			if(extracted!= null && extracted.selectionStart != extracted.selectionEnd){
				mCharAdded = false;
				isTextSelected = true; //Bug Fix: 5784 - Kashyap
			}

			//Space pressed when revert word is there replace with revert word.
			if (primaryCode == KPTConstants.KEYCODE_SPACE || primaryCode == KeyEvent.KEYCODE_SPACE) {
				isSpace = true;
			}

			if(!isTextSelected) { //Bug Fix: 5784 - Kashyap
				//				finishComposingText();
				KPTCorrectionInfo info = null;
				boolean textBeforeCursor = false;
				CharSequence txtBeforeCursor = getTextBeforeCursor(1, 0);
				int phoneKeyboardState = mKeyboardSwitcher.getPhoneKeypadState();
				String currentWord = null;
				if(mIsUnknownEditor && phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) {
					CurrentWord currWordObj =  mCoreEngine.getCurrentWord();
					String[] currentWordArray = null;
					if(currWordObj != null) {
						currentWordArray = currWordObj.currWord;
					}
					if(currentWordArray != null){
						currentWord = currentWordArray[1];
					}

				}
				// Fix for TP 8163
				boolean atxOn = mAtxStateOn && !mIsPopupChar 
						&& mKeyboardSwitcher != null ? phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET:false;
				if(txtBeforeCursor != null){
					String strBeforeCur = txtBeforeCursor.toString();
					info = mCoreEngine.addChar((char)primaryCode,
							strBeforeCur.equalsIgnoreCase(
									"" + (char)KPTConstants.KEYCODE_SPACE)? true : false , 
											mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState()
											mCandidateViewContainer.getAccDsmissState(), atxOn);
					// This check is required to identify whether any printable
					// chars are there just before the cursor
					if(strBeforeCur != null &&
							!strBeforeCur.equalsIgnoreCase("") &&
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE) && 
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_ENTER)){
						textBeforeCursor = true;
					}
				} else {
					info = mCoreEngine.addChar((char)primaryCode,
							false , mJustAddedAutoSpace, mCandidateViewContainer.getAccDsmissState()
							accState, atxOn);
				}
				if(info == null){//Bug Fix: 6172, 6166 - Check not needed
					// Handling 12 key predictive and multitap separately as multitapping doesn't require
					// inlining functionality
					if(!isSpace && !mIsPopupChar && phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET && mAtxStateOn){
						// TP 8276,8277 making this check to make sure that text is not appended when there is partial in-lining
						if(textBeforeCursor && !mComposingMode){
							setPreExistingTextToComposingMode(mInputConnection);
						}else{
							String currWord = mCoreEngine.getPredictiveWord();
							if(currWord != null && currWord.length() > 0) {
								setComposingText(currWord, 1);
							}
						}
					}else{

						if(mIsUnknownEditor
								&& phoneKeyboardState != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET 
								&& currentWord != null 
								&& currentWord.length() > 0) {
							if(textBeforeCursor) {
								deleteSurroundingText(currentWord.length(), 0);
							}
							setComposingText(currentWord+String.valueOf((char)primaryCode), 1);
						} else {
							finishComposingText();
							commitText(String.valueOf((char)primaryCode), 1);
						}

					}

					if(primaryCode == KPTConstants.KEYCODE_SPACE && mJustAddedAutoSpace) {
						//dont make it false. made this change for space multi tap
					} else {
						mJustAddedAutoSpace = false;
					}

					//mJustAddedAutoSpace = false;
					//updateSuggestions();
				} else {  Update Editor w.r.t core as auto suggestion has been done
					// Handling 12 key multitapping's AC selection, all the auto backspacing here
					finishComposingText();
					deleteSurroundingText(info.getCharsToRemoveBeforeCursor(), info.getCharsToRemoveAfterCursor());
					commitText(info.getModString(), 1);
					// remove extra space
					if(getTextAfterCursor(1, 0) != null){
						String ch = getTextAfterCursor(1, 0).toString();
						if(ch != null && ch.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE)){
							deleteSurroundingText(0, 1);
							mCoreEngine.removeChar(false);
						}
					}
					// Fix for TP 9120	
					if(mSuggList!=null && mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
					{
						setXiKeyState(true);
					}
					mJustAddedAutoSpace = true;
				}
				//setPreExistingTextToComposingMode(mInputConnection);
			} else {
				commitText(String.valueOf((char)primaryCode), 1);
				mCoreEngine.resetCoreString(); // TP 8097
				mJustAddedAutoSpace = false;
			}
			mInputConnection.endBatchEdit();
			browserTest();
		}
		mCandidateViewContainer.resetAccDismiss();
		mHardKBCandidateContainer.resetAccDismiss();

		updateShiftKeyState(getCurrentInputEditorInfo());

		if(mAtxStateOn) {
			updateSuggestions();
		} else {
			postUpdateSuggestions(MULTITAP_SUGGESTION_INTERVAL);
		}

		CharSequence txtAfterCur = getTextAfterCursor(Integer.MAX_VALUE/2, 0);
		if (txtAfterCur != null && txtAfterCur.length() > 0) {
			mForwardTyping = true;
		} else {

			mForwardTyping = false;
		}
	}*/

	private void handlePunctuationKey(EditText ic, int primaryCode) {
		if(mCoreEngine == null){
			return;
		}

		CharSequence cursorText =getTextBeforeCursor(1, 0);
		if (cursorText != null) {						
			if(cursorText.length() > 0 && cursorText.charAt(0) == KPTConstants.KEYCODE_SPACE){
			/*	deleteSurroundingText(2, 0);	
				commitText(String.valueOf((char)primaryCode)+" ", 1);*/
				int caretPosition = mCoreEngine.getAbsoluteCaretPosition();
				mCoreEngine.replaceString(caretPosition - 2, caretPosition /*- 1*/, String.valueOf((char)primaryCode)+" ");
				
				int startIndex = mCurrCursorPos - 2;
				int endIndex = mCurrCursorPos;
				String inputStr =  String.valueOf((char)primaryCode)+" ";
				
				replaceBufferCharacters(startIndex, endIndex, inputStr);
			} else {
			/*	deleteSurroundingText(1, 0);
				commitText(String.valueOf((char)primaryCode), 1);*/
				int caretPosition = mCoreEngine.getAbsoluteCaretPosition();
				mCoreEngine.replaceString(caretPosition - 1, caretPosition , String.valueOf((char)primaryCode));
				
				int startIndex = mCurrCursorPos - 1;
				int endIndex = mCurrCursorPos;
				String inputStr =  String.valueOf((char)primaryCode);
				replaceBufferCharacters(startIndex, endIndex, inputStr);
			}
		}
	}

	public void handleClose(boolean isBackKey) {
		commitTyped(mInputConnection);
		mComposing.setLength(0);

		if(!isBackKey) {
			//((InputMethodService) mContext).requestHideSelf(0);niraj
			if(mInputView != null){
				
				mInputView.closing();
			}
		}
	}

	private void checkToggleCapsLock() {
		if(mInputView == null){
			return;
		}
		//Toggle caps occurs in 2 cases. Shift clicked in camel or uppercase keyboard
		if (mInputView.getKeyboard().isShifted()) { //true if camel or uppercase 
			toggleCapsLock();
		}
		mInputView.setShifted(mCapsLock || !mInputView.isShifted());
	}

	private void toggleCapsLock() {
		if(mKeyboardSwitcher == null || mInputView == null){
			return;
		}
		mCapsLock = !mCapsLock;
		mKeyboardSwitcher.setCapsLockState(mCapsLock);
		if (mKeyboardSwitcher.isAlphabetMode()) {
			((KPTAdpatxtKeyboard) mInputView.getKeyboard())
			.setShiftLocked(mCapsLock);
			mInputView.invalidateAllKeys();
		}
	}

	private void postUpdateSuggestions() {
		mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
		mHandler.sendMessageDelayed(mHandler
				.obtainMessage(MSG_UPDATE_SUGGESTIONS), 200);
	}

	public void postUpdateSuggestions(int interval) {
		mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
		mHandler.sendMessageDelayed(mHandler
				.obtainMessage(MSG_UPDATE_SUGGESTIONS), interval);

	}
	public void postUpdateSuggestions(int interval,char primaryCode) {
		mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
		Message msg = mHandler
				.obtainMessage(MSG_UPDATE_SUGGESTIONS);
		msg.arg1 = primaryCode;
		mHandler.sendMessageDelayed(msg, interval);

	}

	private boolean isPredictionOn() {
		// Return true only if core is not in maintenance mode and
		// prediction is set to true by editor.

		boolean predictionOn = mPredictionOn & !mCoreMaintenanceMode;
		return predictionOn;
	}

	private boolean isCandidateStripVisible() {
		//return isPredictionOn();
		return (!isHideCV && mSuggestionBar) && isStartCV;
	}

	public boolean isCandidateViewShown() {
		if (mCandidateViewContainer != null) {
			isHideCV = false;
			return mCandidateViewContainer.isShown();
		}else{
			isHideCV = true;
		}
		if (mHardKBCandidateView != null) {
			return mHardKBCandidateView.isShown();
		}
		/*if(mCandidateView != null || mHardKBCandidateView != null){
			return mCandidateView.isShown() || mHardKBCandidateView.isShown();
		}*/
		return false;
	}


	private void handleAtx() {

		if(mInputView!= null && !mInputView.mDisableATX) {

			mAtxStateOn = !mAtxStateOn;

			Editor sharedEditor = mSharedPreference.edit();
			sharedEditor.putBoolean(KPTConstants.PREF_PHONE_ATX_STATE, mAtxStateOn);
			sharedEditor.commit();

			mInputView.handleAtx(mAtxStateOn);
			updateCorrectionMode();
		}
		if (mCandidateView != null ) { 
			mCandidateView.setAutoCorrection(!mAtxStateOn && mAutoCorrectOn);

		}	
		if (mHardKBCandidateView != null ) { 
			mHardKBCandidateView.setAutoCorrection(!mAtxStateOn && mAutoCorrectOn);

		}
		if(mCoreEngine != null) { 
			mCoreEngine.updateCurrentWord();
			if(mAtxStateOn){
				mCoreEngine.setATRStatus(!mAtxStateOn);
			}else{
				mCoreEngine.setATRStatus(mSharedPreference.getBoolean(KPTConstants.PREF_ATR_FEATURE, true));
			}
		}
	}

	public boolean getIsHideCV(){

		return isHideCV;
	}

	public void setSuggestions(List<KPTSuggestion> suggestions,
			boolean completions,
			boolean typedWordValid, boolean haveMinimalSuggestion) {
		////Log.e("VMC", "IME : SETSUGGESTIONS--------------------> mCondidateView null? "+((mCandidateView == null)));
		if (mCandidateView == null || mCoreEngine == null) {
			return;
		}
		// condition to restrict ATR display in candidateview in forward and backward navigation
		List<KPTSuggestion> candidateSuggestions = new ArrayList<KPTSuggestion>();
		boolean isAtrOn = mSharedPreference.getBoolean(KPTConstants.PREF_ATR_FEATURE,
				true);
		String curWord[] = null;
		if(mInputInfo != null) {
			curWord = mInputInfo.currWordArray;
		} else {
			CurrentWord currentWord = mCoreEngine.getCurrentWord();
			if(currentWord!=null){
				curWord = currentWord.currWord;
			}
		}

		boolean disableATR;
		//Fix for the crash in 4.4 for Dictionary.com application
		CharSequence textBeforeCursor = getTextAfterCursor(1,0);
		if (curWord != null
				&& curWord[0] != null
				&& curWord[1] != null
				&& !curWord[0].equals("")
				&& (!curWord[1].equals("") || (textBeforeCursor != null && textBeforeCursor.equals(" "))))  {
			disableATR = true;
		}
		else{
			disableATR = false;
		}
		if(suggestions != null){
			for(int i=0 ;i<suggestions.size();i++){

				if(isAtrOn && disableATR && (suggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR) ){
					continue;
				}
				else{
					candidateSuggestions.add(suggestions.get(i));
				}
			}
		}

		//Passing the text to render on CV
		mCandidateView.setIsAcc(false);
		mCandidateView.setAccString("");
		if (mHardKBCandidateView != null) {
			mHardKBCandidateView.setIsAcc(false);
			mHardKBCandidateView.setAccString("");
		}

		//Bug 6919
		boolean isPackageInstallProgresss = mSharedPreference.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);

		if (mShowApplicationCandidateBar) {
			if (mInputView.isShown()) {
				mCandidateView.setFrameWorkSuggestionState(true);
				mCandidateView.setSuggestions(suggestions, false, false, KPTCandidateView.SUGGESTIONS_BLANK_SUGGESTIONS);
			}else if(mHardKBCandidateView != null){
				mHardKBCandidateView.setFrameWorkSuggestionState(true);
				mHardKBCandidateView.setSuggestions(suggestions, false, false, KPTCandidateView.SUGGESTIONS_BLANK_SUGGESTIONS);
			}
		}else{

			/*
			 * when glide is enabled first suggestion is displayed ONLY in the editor not in the suggestion bar.
			 * So remove the first suggestion 
			 */
			boolean noGlideSuggestionsFromCore = false;
			if(mIsGlideEnabled && mInputView != null && mInputView.getCurrentTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
				if(suggestions != null
						&& suggestions.size() > 0) {
					mGlideSuggestion = suggestions.get(0);
					/**
					*   As Space is not added after any puncutaion for glide we get a revert suggestion as top suggestion.
					*   We need to ignore the suggestion if it isa revert word. Need to be implemented from core.
					**/
					//if(!mGlideView.isGlidePaused()) {
					if(mGlideSuggestion.getsuggestionType() == KPTSuggEntry.KPT_SUGGS_TYPE_REVERT_CORRECTION 
							&& mSuggList.size()>1){
						mGlideSuggestion = mSuggList.get(1);
						candidateSuggestions.remove(1);
					}
					candidateSuggestions.remove(0);
					//}
				} else {
					noGlideSuggestionsFromCore = true;
					setGlideSuggestionToNull();
				}
			}
			mCandidateView.setFrameWorkSuggestionState(false);


				////Log.e(TAG, "update suggestions 7 "+ (mInputView.isShown()) );
				if(suggestions != null){
					
					if (mInputView.isShown()) {
						mCandidateView.setSuggestions(candidateSuggestions, completions,
								typedWordValid, mCoreEngine != null && mCoreEngine.isCoreUserLoaded() == false && isPackageInstallProgresss == false ? KPTCandidateView.SUGGESTIONS_NO_DICTIONARY_INSTALLED:
									((isPackageInstallProgresss == true)|| (mCoreMaintenanceMode == true))? KPTCandidateView.SUGGESTIONS_MAINTENANCE:
										mPasswordText == true? KPTCandidateView.SUGGESTIONS_PASSWORD :
											noGlideSuggestionsFromCore == true? KPTCandidateView.SUGGESTIONS_NO_GLIDE_SUGGESTIONS :  
												KPTCandidateView.SUGGESTIONS_DEFAULT);
						
					}else if(mHardKBCandidateView != null){
						mHardKBCandidateView.setSuggestions(candidateSuggestions, completions,
								typedWordValid, mCoreEngine != null && mCoreEngine.isCoreUserLoaded() == false && isPackageInstallProgresss == false ? KPTCandidateView.SUGGESTIONS_NO_DICTIONARY_INSTALLED:
									((isPackageInstallProgresss == true)|| (mCoreMaintenanceMode == true))? KPTCandidateView.SUGGESTIONS_MAINTENANCE:
										mPasswordText == true? KPTCandidateView.SUGGESTIONS_PASSWORD :
											KPTCandidateView.SUGGESTIONS_DEFAULT);
					}
				}else{
					mCandidateView.setSuggestions(candidateSuggestions, completions, typedWordValid, KPTCandidateView.SUGGESTIONS_BLANK_SUGGESTIONS);

				}
		
		}
	}

	public List<KPTSuggestion> getsuggestions() {
		return mSuggList;
	}

	private void updateSuggestions(boolean showSuggestions) {
		//mSuggestionShouldReplaceCurrentWord = false;
		////Log.e(TAG, "update suggestions 1");
		if( mCandidateViewContainer == null){
			////Log.e(TAG, "update suggestions mCandidateViewContainer null");
			return;
		}
		
		if(mInputView == null){
			////Log.e(TAG, "update suggestions mInputView null");
			return;
		}
		
		if(showSuggestions){
			
			if (mCoreEngine == null) {
				////Log.e(TAG, "update suggestions mCoreEngine null");
				return;
			}
			
			Keyboard keyboard = mInputView.getKeyboard();
			if (keyboard == null) {
				////Log.e(TAG, "update suggestions keyboard null");
				return;
			}

			// Update Suggestions
			////Log.e("VMC", "UPDATE SUGGS : IF ---> !mCoreMaintenanceMode : "+(!mCoreMaintenanceMode));
			if(!mCoreMaintenanceMode) {
				if(mSuggList != null){
					mSuggList.clear();
					mSuggList = null;
				}
				if(mIsGlideEnabled && mInputView.getCurrentTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
					//to display default suggestion, check text length in editor
					mSuggList = mCoreEngine.getGlideSuggestions();
					if(mSuggList != null && mSuggList.size() > 0) {
						mGlideSuggestion = mSuggList.get(0);
						if(mGlideSuggestion.getsuggestionType() == KPTSuggEntry.KPT_SUGGS_TYPE_REVERT_CORRECTION 
								&& mSuggList.size()>1){
							mGlideSuggestion = mSuggList.get(1);
						}
					}
				}else {
					
					mSuggList = mCoreEngine.getSuggestions();
				}
			} 
			
			if(mInputView.isShown()){
				setSuggestions(mSuggList, false, false, false);
				// candidate view should be shown after configuration changes
				setSuggestionStripShown(isCandidateStripVisible() || mCompletionOn);
			} else {
				setSuggestionStripShown(false);
			}
			
			//updateSpaceKeyHighlight();
			/*if (!mKPTClipboard.isHidden()) {
				setSuggestionStripShown(false);
			}*/
			if(isHideCV){
				if(!mCoreMaintenanceMode) {
					if(mSuggList != null){
						mSuggList.clear();
						mSuggList = null;
					}
					
					mSuggList = mCoreEngine.getSuggestions();
					
				} 
				setSuggestions(mSuggList, false, false, false);
			}
			return;
		}else{
			mSuggList = null;
			setSuggestions(mSuggList, false, false, false);
		}
		
	}

	/**
	 * Update the highlight on space key.
	 */
	public void updateSpaceKeyHighlight() {
		if (mInputView == null) {
			return;
		}
		if (mCandidateViewContainer == null) {
			return;
		}
		KPTAdpatxtKeyboard adaptxtKeyboard = (KPTAdpatxtKeyboard)mInputView.getKeyboard(); 
		if (adaptxtKeyboard == null) {
			return;
		}

		boolean isACSuggShown = !isRevertedWord() 
				&& mCandidateViewContainer.isAccApplicableOnSpace() 
				&& (!isHideCV && mSuggestionBar);
		boolean isSpaceHighlighted = adaptxtKeyboard.isSpaceHighlighted();
		// Update Space key only if required
		if((isSpaceHighlighted && !isACSuggShown) || 
				(!isSpaceHighlighted && isACSuggShown)/* || 
				mKPTClipboard.isHidden()*/){
			// if xi key is enabled, space highlight should be disabled
			if(KPTConstants.mXi_enable) {
				adaptxtKeyboard.setSpaceHighlighted(false);
			}else{
				// for Ac suggestion space should highlight
				adaptxtKeyboard.setSpaceHighlighted(isACSuggShown);
			}

			//if(mIsKeyboardTypeQwerty || (!mIsKeyboardTypeQwerty && !mAtxStateOn && !getRevertWordStatus()))
			mInputView.invalidateSpaceKey();

			// If input view is shown, invalidate the space key alone
			if(mInputView.isShown()) {
				mInputView.invalidateKey(adaptxtKeyboard.getSpaceKeyIndex());
			}
		}
	}

	public void pickFrameWorkSuggestion(int index, KPTSuggestion suggestion){
		if (mInputConnection != null) {
			if (mApplicationCompletionList != null && index != -1 ) {
				mInputConnection.beginBatchEdit();
				final CompletionInfo completionInfo = mApplicationCompletionList[index];
				//Send the correct completionInfo obj to editor.
				if (completionInfo != null ) {
					mInputConnection.commitCompletion(completionInfo);
				}
				mInputConnection.endBatchEdit();
			}
		}
	}

	public void pickSuggestionManually(int index, KPTSuggestion suggestion) {
		
		if (suggestion == null) {
			////Log.e("VMC", "Suggestion is null");
			return;
		}
		if (mCoreEngine == null) {
			////Log.e("VMC", "mCoreEngine is null");
			return;
		}
		
		if(mEditText == null){
			return;
		}
		
		
		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}
		if(KPTConstants.mXi_enable){
			setXiKeyState(false);
		}

		if(!mIsXiTouched){
			playKeyClick(0);
			//vibrate(VIBRATE_GENERIC);
		}
		String suggestionString = suggestion.getsuggestionString();
		int sugLength = suggestionString.length();
		
		if(mAutoSpace){
			sugLength = sugLength + 1;
		}
		
		if(suggestionString == null 
				|| (mEditText.getmMaxLength() > 0 
						&& (mEditText.getText().length() + sugLength) > mEditText.getmMaxLength())){
			postUpdateSuggestions(10);
			return;
		}
		//ExtractedText extracted = null;
		// For Browser Paste Issue
		if(isBrowser) {
			//extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);

			CharSequence textBeforeCursor = null;
			if(mEditText != null &&
					mEditText.getText() != null) {
				textBeforeCursor = getTextBeforeCursor(mEditText.getText().toString().length(), 0);
				if(textBeforeCursor != null){
					mExtractedText = new SpannableStringBuilder (mEditText.getText().toString());
					mExtractedLength = mExtractedText.length();
					String coreBuffer = mCoreEngine.getCoreBuffer();
					if(textBeforeCursor.length() != mCoreEngine.getAbsoluteCaretPosition() 
							|| (coreBuffer != null && coreBuffer.compareTo(mExtractedText.toString()) != 0)){
						// Strings are not equal.
						// There is a mismatch in suggestions return.
						mCoreEngine.syncCoreBuffer(mExtractedText.toString(), textBeforeCursor.length());
						syncBufferText(mExtractedText.toString(), textBeforeCursor.length());
						browserTest();
						postUpdateSuggestions();
						return;
					} else{
						// Proceed with suggestion selection below
					}
				}
			}
		}


		mEditText.beginBatchEdit();


		// If this is a punctuation, apply it through the normal key press
		if (suggestionString != null 
				&& suggestionString.length() == 1 
				&& isWordSeparator(suggestionString.charAt(0))) {

			//TP 15588  	Selected suggestion is not inserted with phone keypad- Predictive mode
			if(mComposingMode) {
//				mEditText.setText("");
				//mInputConnection.commitText("", 1);
				mCoreEngine.removeChar(true);
				deleteCurrentChar(true);
			}

			boolean atxState = mAtxStateOn;
			mAtxStateOn = false;
			onKey(suggestionString.charAt(0), null, false);
			mAtxStateOn = atxState;

			mEditText.endBatchEdit();
			
			postUpdateSuggestions();
			return;

		}

		//mJustAccepted = true;
		mCandidateSelected = true;
		//	#14622. Suggestion are not getting refreshed in the Phone keypad when Auto spacing is in off state.
		//		mExpectingUpdateSelectionCounter ++;

		/*if(mEditText == null) {
			extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 
					0);
		}*/
		KPTConstants.mIsNavigationBackwar = true;

		finishComposingText();
		// Fix for TP item 6984
		/*mInputConnection.endBatchEdit();
		mInputConnection.beginBatchEdit();*/
		mComposing.setLength(0);
		if(mEditText!= null && mEditText.getSelectionStart() != mEditText.getSelectionEnd()){
			
			
			if (mUserSelectedCompleteText) {
			//	mEditText.setText("");
				mCoreEngine.resetCoreString();
				mUserSelectedCompleteText = false;
				mEditText.setUserSelectedPSomeText(false);
				//JET resetting the buffer
				resetTextBuffer();
			}else{
				final int selectionStart = mEditText.getSelectionStart();
				final int selectionEnd = mEditText.getSelectionEnd();
				mCoreEngine.replaceString(selectionStart,selectionEnd, "");
				
				replaceBufferCharacters(selectionStart, selectionEnd, "");
			}
			
			boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn && mKeyboardSwitcher != null 
					&& mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;

			
			mCoreEngine.insertText(suggestionString + " ", atxOn ,true);
		/*	final String corebuffer = mCoreEngine.getCoreBuffer();
			mEditText.setText(corebuffer);
			mEditText.setSelection(mCoreEngine.getAbsoluteCaretPosition());*/
			
			String inputStr = suggestionString + " ";
			insertOrAppendBuffer(inputStr, true);
			
			if(mIsSIPInput){
				updateShiftKeyState(mEditText);
			}else{
				updateHKBShiftKeyState(mEditText);
			}
			updateSuggestions(true);

		} else {

			mAutoSpace = mAutoSpacePref;
			KPTCorrectionInfo info = mCoreEngine.insertSuggestion(suggestionString, 
					suggestion.getsuggestionId(), mAutoSpace);

			setCurrentInputTypeToNormal();
			setPreviousInputTypeToNormal();
			int beforeLength = getExtractedTextLength();
			if(info == null) {

				int length = -1;
				CurrentWord currentWord = mCoreEngine.getCurrentWord();
				if(currentWord!= null){
					length = currentWord.currWord[0].length();
				}
				if(length != -1) {
					CharSequence beforeCur = mEditText.getText().subSequence(0, mEditText.getText().length());//mInputConnection.getTextBeforeCursor(1, 0);
					if(beforeCur != null && !beforeCur.equals(" ")) {
					//	deleteSurroundingText(length, 0);
						mCoreEngine.removeString(true, length);
						
						deleteBuffChars((mCurrCursorPos -length), mCurrCursorPos, false);
					}
					//	#14611. Gliding and selecting suggestion inserts space when ï¿½Auto Spacingï¿½ option is disabled
					String space = " ";
					if(!mAutoSpace){
						space = "";
					}
					
					mCoreEngine.insertText((suggestion.getsuggestionString() + space), false, false,true);
					String inputStr = (suggestionString + space);
					insertOrAppendBuffer(inputStr, true);
				//	commitText(suggestionString+space , 1);
					setGlideSuggestionToNull();
				}
			}
			else if (!info.equals("") && info.getModString() != ""){
				// Bug Id: 6263
				// Splitting the delete in to two seperate commands.
				// Else f/w commits first and deletes next in landscape mode, which causes bug.

				//deleteSurroundingText(info.getCharsToRemoveBeforeCursor(), info.getCharsToRemoveAfterCursor());
				/*deleteSurroundingText(info.getCharsToRemoveBeforeCursor(),0);
				commitText(info.getModString(), 1);
				deleteSurroundingText(0, info.getCharsToRemoveAfterCursor());*/
				
				int startIndex = mCurrCursorPos - info.getCharsToRemoveBeforeCursor();
				int endIndex = mCurrCursorPos + info.getCharsToRemoveAfterCursor();
				
				replaceBufferCharacters(startIndex, endIndex, info.getModString());
//				processPickSuggestionManually(info);

				//cursorJumpPosition = -1;
			}
			else{
				//JET need to check why this is ?
				insertOrAppendBuffer(suggestionString, true);
//				commitText(suggestionString, 1);
			}

			int afterLength = getExtractedTextLength();
//			removeExtraCharsFromCoreIfRequired(beforeLength, afterLength, null);

			if(mIsSIPInput || (mInputView != null && mInputView.isShown())){//Bug Fix 7063
				updateShiftKeyState(mEditText); //Bug fix 5486
			}else{
				updateHKBShiftKeyState(mEditText);
			}
		}
		//pickSuggestion(suggestion, true, index, ic);

		// Follow it with a space
		if (mAutoSpace) {
			//commitText("" + KEYCODE_SPACE, 1);
			//Bug 6346, Text after cursor is returning null in some scenarios

			String ch;
			try {
				CharSequence textAfterCursor = getTextAfterCursor(1, 0);
				if((textAfterCursor != null) && (textAfterCursor.length() != 0)){
					ch = textAfterCursor.toString();
				} else{
					ch = new String("");
				}
			}catch(NullPointerException e) {
				ch = new String("");
			}

			//String ch = (getTextAfterCursor(1, 0)!=null)? (getTextAfterCursor(1, 0).toString()):"";
			if(("" + (char)KPTConstants.KEYCODE_SPACE).equalsIgnoreCase(ch)){
//				deleteSurroundingText(0, 1);
				mCoreEngine.removeChar(false);
				deleteNextChar();
			}
			mJustAddedAutoSpace = true;
		} else {
			//if(mIsKeyboardTypeQwerty)
				setPreExistingTextToComposingMode(mInputConnection);
		}
		//mInputConnection.endBatchEdit();
		// Resetting auto correction state as it is manually disabled while backward navigation.
		mCoreEngine.setErrorCorrection(mErrorCorrectionOn, mAutoCorrectOn
				&& (!isHideCV && mSuggestionBar),mCorrectionMode,mSpaceCorrectionSuggestionsOn);

		if(suggestion!=null && suggestion.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
		{
			setXiKeyState(true);
		}


		browserTest();
		postUpdateSuggestions(50); // Making an optimal value 50 so that the suggestion selection from SBar is quick

	}
    
	private void processPickSuggestionManually(KPTCorrectionInfo info) {
		
		String corebuffer = mCoreEngine.getCoreBuffer();
		final int acp = mCoreEngine.getAbsoluteCaretPosition();
		
		mEditText.setText("",true);
		mEditText.setText(corebuffer,true);
		mEditText.setSelection(acp,true);
		
	}
	
	
	private boolean sameAsTextBeforeCursor(CharSequence text) {
		CharSequence beforeText = getTextBeforeCursor(text.length(), 0);
		if(!TextUtils.isEmpty(beforeText)){
			return TextUtils.equals(text, beforeText);
		}
		return false;
	}

	protected String getWordSeparators() {
		return mWordSeparators;
	}

	public boolean isWordSeparator(int code) {
		String separators = getWordSeparators();
		return separators.contains(String.valueOf((char) code));
	}

	public boolean isSentenceSeparator(int code) {
		return mSentenceSeparators.contains(String.valueOf((char) code));
	}



	public void swipeRight() {

	}
	
	private void toggleLayout(){
		if(mKeyboardSwitcher == null){
			return;
		}
		int currentKeyboardMode = getKeyboardMode(mEditText);
		int imeOptions = mKeyboardSwitcher.getImeOptions(); //Bug no.6053 
		mCapsLock = false;//Bug 6041, 6385
		mKeyboardSwitcher.mMode = currentKeyboardMode;
		reloadKeyboards();
		mKeyboardSwitcher.setKeyboardMode(currentKeyboardMode, imeOptions,true);
		
		//TP #13194. Blank space is displayed below the SIP after changing key height & Application got crashed upon tapping on blank space
		if(mInputView != null) {
			mInputView.requestLayout();
		}

		if(mInputView != null) {
			mInputView.resetWindowOffsets();
		}
		updateLayoutToCore();
		if(mInputView != null) {
			checkIfGlideShouldBeEnabled();
		}
		
			updateShiftKeyState(mEditText);
	}

	private void toggleLanguage(boolean reset, boolean next) {
		if (mLanguageSwitcher == null) {
			return;
		}
		if (mKeyboardSwitcher == null) {
			return;
		}
		mKeyboardSwitcher.mShouldLoadEngLocale = false;//make the boolean false to revert the locale when the keyboard language is switched.
		mLanguageSwitcher.setShoudLoadEngLocale(mKeyboardSwitcher.mShouldLoadEngLocale);

		if (reset) {
			mLanguageSwitcher.reset();
		} else {
			if (next) {
				mLanguageSwitcher.next();
			} else {
				mLanguageSwitcher.prev();
			}
		}
		
		String curentLanguage = mLanguageSwitcher.getInputLanguage();
		mIsHindhi = isHindhiLanugae();
		if(!mIsHindhi){
			mHindhiSyllableTable.clear();
		}

		//mHardKeyboardKeymap.setCurrentLocale(mLanguageSwitcher.getInputLanguageLocale());
		int currentKeyboardMode = getKeyboardMode(mEditText);
		int imeOptions = mKeyboardSwitcher.getImeOptions(); //Bug no.6053 
		mCapsLock = false;//Bug 6041, 6385
		mKeyboardSwitcher.mMode = currentKeyboardMode;
		reloadKeyboards();
		mKeyboardSwitcher.setKeyboardMode(currentKeyboardMode, imeOptions,true);

		//TP #13194. Blank space is displayed below the SIP after changing key height & Application got crashed upon tapping on blank space
		if(mInputView != null) {
			mInputView.requestLayout();
		}

		if(mInputView != null) {
			mInputView.resetWindowOffsets();
		}
		updateLayoutToCore();
		if(mInputView != null) {
			checkIfGlideShouldBeEnabled();
		}
		/**
		 * calling oncomputeInsets again because when keyboard height is chaneged like
		 * switching to thai lang which has 5 rows instead of 4, the keyboard goes black 
		 * when back key is pressed, because the update height is not returned to framework
		 */
		if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			isComputeInsetsDoneOnce = false;
			//final Insets mTmpInsets = new Insets();
			//onComputeInsets(mTmpInsets);
			if (mInputView != null) {
				mInputView.invalidateAllKeys();
			}
		}

		// while switching the keyboard language
		boolean isRTL = checkIfRTL(getInputLocale());
		if (mCandidateViewContainer != null && mCandidateView != null) {
			mCandidateViewContainer.setRTL(isRTL);
			mCandidateView.setRTL(isRTL);
		}
		if (mHardKBCandidateContainer != null && mHardKBCandidateView != null) {
			mHardKBCandidateContainer.setRTL(isRTL);
			mHardKBCandidateView.setRTL(isRTL);
		}

		//TP 15609. Not typed when the language changed into English or other language from Korean
		//checking the atx state again after langage switch
		if(mPredictionOn && mSuggestionBar) {
			mAtxStateOn = mSharedPreference.getBoolean(KPTConstants.PREF_PHONE_ATX_STATE, true);
			if (null != mInputView) {
				mInputView.mDisableATX = false;
			}
			
		} else {

			mAtxStateOn = false;
			
			if (null != mInputView) {
				mInputView.mDisableATX = true;
				mInputView.handleAtx(mAtxStateOn);
			}
		
		}

		//TP 15475. Last row of the SIP in Thai language is partially visible in landscape mode after language switch.
		isComputeInsetsDoneOnce = false;
		initSuggest(curentLanguage);
		//mAtxStateOn = false;
		//handleAtx();
		//mLanguageSwitcher.persist();
		if(mEditText != null){
			
			if(mIsSIPInput)
				updateShiftKeyState(mEditText);
			else		
				updateHKBShiftKeyState(mEditText);
		}
		postUpdateSuggestions(50);

		if(mCoreEngine != null){
		    if(isHideCV || !mSuggestionBar || mPasswordText == true || mShowApplicationCandidateBar ){
					mCoreEngine.setBlackListed(true);
			}else {
				mCoreEngine.setBlackListed(false);
			}
		}
		
		AdaptxtEditText editor = null;
		if (null != mInputView) {
			 editor = mInputView.getFocusedEditText();
		}
		
		if (null != editor) {
			AdaptxtKeyboordVisibilityStatusListner editListner = editor.getInputViewListner();
			if(null != editListner){
				if (null != mLanguageSwitcher) {
					editListner.analyticalData(mLanguageSwitcher.getCurrentLanguageDisplayName());
				}else{
					Log.e("KPT", "KPT failed to send anlyatical data to hike mLanguageSwitcher was null ");
				}
			}else{
				Log.e("KPT", "KPT failed to send anlyatical data to hike editListner was null ");
			}
		}else{
			Log.e("KPT", "KPT failed to send anlyatical data to hike edit text editor was null ");
		}
		
		
		
		
	}
	

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		mSuggestionBar = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE,true);		


		if(KPTConstants.PREF_VIBRATE_ON.equals(key) || KPTConstants.PREF_SOUND_ON.equals(key)){
			updateAdaptxtSoundandVibrate();
		}
		
		if(KPTConstants.PREF_POPUP_ON.equals(key)){
			final boolean isKeyPopupEnabledInSettings = mSharedPreference.getBoolean(KPTConstants.PREF_POPUP_ON, true); 
			if(mInputView != null){
				
				mInputView.setKeyPreviewPopupEnabled(isKeyPopupEnabledInSettings, 0);
			}
		}
		if(KPTConstants.PREF_AUTO_CAPITALIZATION.equals(key)){
			mAutoCap = mSharedPreference.getBoolean(KPTConstants.PREF_AUTO_CAPITALIZATION, true);
		}

		if(KPTConstants.PREF_CORE_MAINTENANCE_MODE.equals(key)) {

			boolean coreMaintmode = sharedPreferences.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);

			// Update the keyboard if first language is being installed.
			if(coreMaintmode == false) {
				if(mLanguageSwitcher != null && mLanguageSwitcher.getLocaleCount() == 0) {
					// Load locales and refresh keyboards
					/*if(mInputView!=null && mInputView.isShown()) {
						mHandler.sendEmptyMessage(MSG_TOAST_KB_CHANGE);
					}*/
					mLanguageSwitcher.loadLocales(mContext);
					postReloadKeyboards();
				}
			}

			// Update context, core and editor whenever we are coming out of maintenance mode
			mCoreMaintenanceMode = coreMaintmode;
			mComposing.setLength(0);
			updateCorrectionMode(); //bug fix for 5844 -- karthik

			if(mInputConnection != null && ((InputMethodService) mContext).getCurrentInputEditorInfo()!=null){// Bug ID 7015
				int variation = ((InputMethodService) mContext).getCurrentInputEditorInfo().inputType & EditorInfo.TYPE_MASK_VARIATION;
				switch(((InputMethodService) mContext).getCurrentInputEditorInfo().inputType  & EditorInfo.TYPE_MASK_CLASS) 
				{
				case EditorInfo.TYPE_CLASS_TEXT:
					switch(variation){
					case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
					case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
						//mKbdSwitcher.setNumberkey(mShowNumberKey);
					case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
						if(mCoreEngine != null){
							mCoreEngine.setBlackListed(true);
						}
						break;
					default:
						break;
					}
					break;
				default:
					break;
				}
			}

			addPreExistingTextToCore(false);

			if (mInputConnection != null){
				mInputConnection.beginBatchEdit();
				//if(mIsKeyboardTypeQwerty) 
				setPreExistingTextToComposingMode(mInputConnection);
				mInputConnection.endBatchEdit();
				postUpdateShiftKeyState();
				postUpdateSuggestions();
			}
		}else if(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS.equals(key)) { //Bug 6919 on 2.3.3 SDK
			postUpdateSuggestions();
			//if ime is opened with addon installation in progress, update the locale count
			//and notify pointer tracker
			boolean installResult = sharedPreferences.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false);
			if(!installResult) {
				if(mLanguageSwitcher != null) {
					mLanguageSwitcher.loadLocales(mContext);
					PointerTracker.setInstalledDictionariesCount(mLanguageSwitcher.getLocaleCount());
				}
			}
		} else if (KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS.equals(key)) { //Bug 6932
			KPTParamDictionary coreTopDict = mCoreEngine.getTopPriorityDictionary();
			if (mInputView != null
					&& mInputView.isShown()
					&& mCoreEngine != null
					&& mLanguageSwitcher != null
					&& mLanguageSwitcher.getLocaleCount() > 1
					&& coreTopDict != null
					&& coreTopDict.getDictFileName()
					.equals(sharedPreferences.getString(
							KPTConstants.PREF_CORE_INSTALL_PACKAGE_NAME, null))) {
				//mHandler.sendEmptyMessage(MSG_TOAST_KB_CHANGE);
			}
			boolean coreMaintmode = sharedPreferences.getBoolean(
					KPTConstants.PREF_CORE_MAINTENANCE_MODE, false);
			boolean uninstall = sharedPreferences.getBoolean(
					KPTConstants.PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS, false);
			if (uninstall == false && coreMaintmode == false && mLanguageSwitcher != null) {
				mLanguageSwitcher.loadLocales(mContext);
				postReloadKeyboards();
			}
		} /*else if(KPTConstants.PREF_CORE_LAST_ADDON_REMOVED.equalsIgnoreCase(key)) {
			if(sharedPreferences.getBoolean(KPTConstants.PREF_CORE_LAST_ADDON_REMOVED, false)){
				mLanguageSwitcher.reset(true);
				postReloadKeyboards();
			}
		} else if(KPTConstants.PREF_CURRENT_KEYBOARD_TYPE.equalsIgnoreCase(key)) {
			if(mCoreEngine != null && mLanguageSwitcher != null)
				mCoreEngine.activateLanguageKeymap(mLanguageSwitcher.getCurrentComponentId(), null);
			reloadKeyboards(); //TP 12639. reseting the keyboards when keyboard type is changed
		}*/
		else if ( KPTConstants.PREF_PORT_KEYBOARD_TYPE.equalsIgnoreCase(key) 
				/*|| KPTConstants.PREF_LAND_KEYBOARD_TYPE.equalsIgnoreCase(key)*/){	
			String keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);
			mIsKeyboardTypeQwerty = keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY);

			
			if(mCoreEngine != null && mLanguageSwitcher != null)
				mCoreEngine.activateLanguageKeymap(mLanguageSwitcher.getCurrentComponentId(), null);
			toggleLayout();
		}else if (KPTConstants.PREF_THEME_MODE.equalsIgnoreCase(key)){
			updateTheme();

			if (mInputView == null || getKeyboardView().isShown()) {
				return;
			}
			if(mCandidateViewContainer != null){

				if(mInputView.isShown()){
					//updateTheme();
					mCandidateViewContainer.initViews();			
					mCandidateViewContainer.updateCandidateBar();
				}
				if (mCandidateView != null) {
					mCandidateView.invalidate();
				}
			}

			if(mHardKBCandidateContainer!=null){
				if(mInputView.isShown()){
					//updateTheme();
					mHardKBCandidateContainer.initViews();
					mHardKBCandidateContainer.updateCandidateBar();
				}
				if (mHardKBCandidateView != null) {
					mHardKBCandidateView.invalidate();
				}
			}

			mInputView.themeChanged();
			// Fix for 8282
			//Call updateSpaceBarForLocale() to update the Space bar related resources while changing the theme 
			mInputView.invalidateSpaceKey();
		}else if(KPTConstants.PREF_PRIVATE_MODE.equals(key)){
			setPrivateMode(mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
			if(mCandidateViewContainer != null){
				mCandidateViewContainer.updateCandidateBar();
			}
		} else if(KPTConstants.PREF_GLIDE.equals(key)){
			checkIfGlideShouldBeEnabled();
		}else if(KPTConstants.PREF_HIDE_XI_KEY.equals(key) ||
				KPTConstants.PREF_AUTOCORRECTION.equals(key)){
			reloadKeyboards();

		}else if(key.equals(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE)) {

			if(!sharedPreferences.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false)
					&& !sharedPreferences.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_IN_QUEUE, false)) {

				/*		SharedPreferences.Editor sharedPrefEditor = mSharedPreference.edit();
				sharedPrefEditor.putBoolean(KPTConstants.PREF_OLD_ADDONS_UPGRADE, false);
				sharedPrefEditor.commit();*/

			}



		}else if(KPTConstants.PREF_AUTO_SPACE.equals(key)){
			mAutoSpacePref = sharedPreferences.getBoolean(KPTConstants.PREF_AUTO_SPACE, true);
			if(mCoreEngine !=null){
				mCoreEngine.setAutoSpaceEnable(mAutoSpacePref);
			}

		}

		else if(KPTConstants.PREF_SUGGESTION_ENABLE.equals(key) ){
			mSuggestionBar = mSharedPreference.getBoolean(KPTConstants.PREF_SUGGESTION_ENABLE,true);
			//	//Log.e("SUGGESTION_VALUE", String.valueOf(mSuggestionBar));
		}
	}

	public void swipeLeft() {
	}

	public void swipeDown() {
	}

	public void swipeUp() {
		// launchSettings();
	}

	public void onPress(int primaryCode) {
		//mDeleteKeyLongPress = false;
		if(!((primaryCode== KPTConstants.KEYCODE_XI && !KPTConstants.mXi_enable)|| (primaryCode== KPTConstants.KEYCODE_ATX && mInputView.mDisableATX) ))
		{
			playKeyClick(primaryCode);
			vibrate(VIBRATE_GENERIC);
		}

	}

	public void onRelease(int primaryCode) {
		// Reset any drag flags in the keyboard
		//((KPTAdpatxtKeyboard) mInputView.getKeyboard()).keyReleased();
		//vibrate();
	}

	/*private boolean shouldShowVoiceButton(EditorInfo attribute) {
		return !mPasswordText
				&& !(attribute != null && attribute.privateImeOptions != null 
				&& IME_OPTION_NO_MICROPHONE.equals(attribute.privateImeOptions));

	}*/
	// receive ringer mode changes to detect silent mode
	
	private CustomKeyboard mCustomKeyboard;


	
	//Update adaptxt sound and vibrate 
	private void updateAdaptxtSoundandVibrate(){		

		soundVariation = mSharedPreference.getFloat(KPTConstants.PREF_KEY_SOUND,KPTConstants.DEFAULT_VOLUME);
		mChangeVolume = (float) (soundVariation-(0.9*soundVariation));
		vibrationVariation =mSharedPreference.getInt(KPTConstants.PREF_KEY_VIBRATION,KPTConstants.DEFAULT_VIBRATION);			
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);	

		}

		boolean vibrate = mSharedPreference.getBoolean(KPTConstants.PREF_VIBRATE_ON, false);
		boolean sound = mSharedPreference.getBoolean(KPTConstants.PREF_SOUND_ON, false);

		switch (mAudioManager.getRingerMode()) {
		case AudioManager.RINGER_MODE_SILENT  :
			mSoundOn = false;
			if(vibrate){
				mVibrateOn = true;
			}else{
				mVibrateOn = false;
			}
			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			if(vibrate){
				mVibrateOn = true;
			}else{
				mVibrateOn = false;
			}
			mSoundOn = false;
			break;
		case AudioManager.RINGER_MODE_NORMAL:
			if(sound){
				mSoundOn = true;
			}else{
				mSoundOn = false;
			}
			if(vibrate){
				mVibrateOn = true;
			}else{
				mVibrateOn = false;
			}

			break;

		}
	}


	// update flags for silent mode
	private void updateRingerMode() {
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		}
		if (mAudioManager != null) {
			mSilentMode = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
		}
	}

	public void playKeyClick(int primaryCode) {
		// if mAudioManager is null, we don't have the ringer state yet
		// mAudioManager will be set by updateRingerMode
		if (mAudioManager == null) {
			if (mInputView != null) {
				updateRingerMode();
			}
		}
		if (mSoundOn && !mSilentMode) {
			// FIXME: Volume and enable should come from UI settings
			// FIXME: These should be triggered after auto-repeat logic
			int sound = AudioManager.FX_KEYPRESS_STANDARD;
			switch (primaryCode) {
			case KPTConstants.KEYCODE_DELETE:
				sound = AudioManager.FX_KEYPRESS_DELETE;
				break;
			case KPTConstants.KEYCODE_ENTER:
				sound = AudioManager.FX_KEYPRESS_RETURN;
				break;
			case KPTConstants.KEYCODE_SPACE:
				sound = AudioManager.FX_KEYPRESS_SPACEBAR;
				break;
			}

			mAudioManager.playSoundEffect(sound,mChangeVolume);



		}
	}

	public void vibrate(int vibMode) {
		if(mVibrateOn ) {
			//Bug fix# 5905
			if (mVibrator == null) {
				mVibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE); 
			}
			// Changes part of TP 6209
			//int vibPattern = 0;
			//vibPattern = mResources.getInteger(R.integer.kpt_vibrate_generic);

			long[] vibratePattern = new long[] {0,vibrationVariation};
			mVibrator.vibrate(vibratePattern,-1);
		}
	}

	/**
	 * Updates the theme of the Keyboard. Refereshes the view and 
	 * redraw them according to the selscted theme.
	 */
	public void updateTheme() {
		if (mTheme == null) {
			return;
		}
		if (mInputView == null) {
			return;
		}
		if (mCandidateView == null) {
			return;
		}
		if (mCandidateViewContainer == null) {
			return;
		}
		mThemeMode = KPTConstants.DEFAULT_THEME;

		if (mCurrentTheme != mThemeMode) {
			//mInputView.setTheme(mTheme);
			mInputView.resetValuesToDefaultTheme();
			mCandidateView.setTheme(mTheme);
			mTheme.loadTheme(mContext, mThemeMode);
			mCurrentTheme = mThemeMode;
			mInputView.setTheme(mTheme);
			mInputView.setKeyTransparency(255);
			mCandidateView.setTheme(mTheme);

			if(!getKeyboardView().isShown() && mHardKBCandidateContainer != null && mHardKBCandidateView != null){
				mHardKBCandidateView.setTheme(mTheme);
				mHardKBCandidateContainer.initViews();
				mHardKBCandidateView.loadView(mContext, mThemeMode);
				mHardKBCandidateContainer.updateCandidateBar();
			}else{
				mCandidateView.loadView(mContext, mThemeMode);
				mCandidateViewContainer.setTheme(mTheme);
				mCandidateViewContainer.initViews();
				mCandidateViewContainer.updateCandidateBar();
				mCandidateView.invalidate();
			}

			mInputView.updateTracePathColor();
		}
	}

		public int getCurrentThemesBaseId() {
			if (mTheme == null) {
				return KPTConstants.BRIGHT_THEME;
			}
			return mTheme.getCurrentThemeBaseID();
		}

		public void updateCorrectionMode() {
			//SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

			String keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);
			/*if(mResources.getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
				keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
						KPTConstants.KEYBOARD_TYPE_QWERTY);	
			}else{
				keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
						KPTConstants.KEYBOARD_TYPE_QWERTY);	
			}*/
			mIsKeyboardTypeQwerty = keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY);
			mErrorCorrectionOn = true; // should always be true as per 0.7.3 RRD

			if(mIsKeyboardTypeQwerty){
				mAutoCorrectOn = (mErrorCorrectionOn) && !mInputTypeNoAutoCorrect
						&& mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, true);
			}else {
				mAutoCorrectOn = (mErrorCorrectionOn) && !mInputTypeNoAutoCorrect
						&& mSharedPreference.getBoolean(KPTConstants.PREF_AUTOCORRECTION, true);

				if(mAtxStateOn) {
					//mAutoCorrectOn = false;  
					//mErrorCorrectionOn = false;
				}
			}

			if (mAutoCorrectOn) {
				mCorrectionMode = Integer.parseInt(mSharedPreference.getString(
						KPTConstants.PREF_AUTOCORRECTION_MODE, "1"));//Kashyap - Bug Fix: 5907
			} else {
				mCorrectionMode = KPTCoreEngine.KPTCORRECTION_MEDIUM;
			}
			if (mCoreEngine != null) {
				if (!KPTConstants.mIsNavigationBackwar) {
					mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
							false, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
				} else {
					mCoreEngine.setErrorCorrection(mErrorCorrectionOn, mAutoCorrectOn && (!isHideCV && mSuggestionBar),//Bug Fix : 5803
							mCorrectionMode,mSpaceCorrectionSuggestionsOn);
				}
				mCoreEngine.setMaxSuggestions(MAX_NO_OF_SUGG); //Bug 5946
			}
			if(mCandidateView != null){ //Kashyap: Bug Fix - 5917
				mCandidateView.setAutoCorrection(mAutoCorrectOn);
			}
			if (mHardKBCandidateView != null) {
				mHardKBCandidateView.setAutoCorrection(mAutoCorrectOn);
			}
		}

		public void launchSettings() {
			launchSettings(KPTAdaptxtIMESettings.class);
		}

		/*public void launchAddonManager(){
		Intent intent = new Intent(KPTAdaptxtIME.this, KPTAddonManager.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, KPTAddonManager.PREMIUM_TAB);
		startActivity(intent);
	}*/

		public void lanuchInstalledTabInAddonManager(){
			handleClose(false);
			Intent intent = new Intent(mContext, KPTAddonManager.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
			/*if(getInstalledAddons() != null && getInstalledAddons().length > 0){
			intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, KPTAddonManager.INSTALLED_TAB);
		}else{*/
			intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, KPTAddonManager.DOWNLOAD_TAB);
			//		}
			mContext.startActivity(intent);
		}


		public void launchSettings(@SuppressWarnings("rawtypes") Class settingsClass) {
			handleClose(false);
			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP); // should use this flag for
			// starting an activity from
			// Service
			intent.setClass(mContext, settingsClass);
			mContext.startActivity(intent);
		}

		public void launchATRListView() {
			launchATRListView(KPTATRListview.class);
		}

		public void launchATRListView(@SuppressWarnings("rawtypes") Class atrlistview) {
			handleClose(false);
			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // should use this flag
			// for starting an activity from Service
			intent.setClass(mContext, atrlistview);
			mContext.startActivity(intent);
		}

		public static String[] getInstalledAddons() {
			if (mLanguageSwitcher != null) {
				String[] displayLanguages = mLanguageSwitcher.getDisplayLanguages();
				return displayLanguages;
			}
			return null;
		}

		private void loadSettings() {
			// Get the settings preferences
			//SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			mVibrateOn = mSharedPreference.getBoolean(KPTConstants.PREF_VIBRATE_ON, false);
			mSoundOn = mSharedPreference.getBoolean(KPTConstants.PREF_SOUND_ON, false);

			if (mLanguageSwitcher != null) {
				mLanguageSwitcher.loadLocales(mContext);
			}

			//fix for #12026
			if (mKeyboardSwitcher != null) {
				mKeyboardSwitcher.setVoiceMode(true, true);
			}
		}

		public class Item{
			public final String text;
			public final int icon;
			public Item(String text, Integer icon) {
				this.text = text;
				this.icon = icon;
			}
			@Override
			public String toString() {
				return text;
			}
		}

		public void showOptionsMenu() {
			if((mOptionsDialog != null && mOptionsDialog.isShowing()) 
					|| (mInputView != null && mInputView.getWindowToken() == null)){
				return; 
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setCancelable(true);
			builder.setIcon(R.drawable.kpt_adaptxt_launcher_icon);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setTitle(mResources.getString(R.string.kpt_UI_STRING_APP_TITLE_2_300));

			CharSequence itemSettings = mContext.getResources().getString(R.string.kpt_english_ime_settings);

			builder.setItems(new CharSequence[] { itemSettings, mContext.getResources().getString(R.string.kpt_change_keyboard_layout) },
					new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface di, int position) {
					di.dismiss();
					switch (position) {
					case POS_SETTINGS:
						launchSettings();
						break;
					case POS_CHOOSE_KEYBOARD_TYPE:
						if (mKPTQuickSettingManager != null) {
							mKPTQuickSettingManager.showThemeChangeMenu();
						}
						break;
					case POS_METHOD:
						/*((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
					.showInputMethodPicker();*/
						break;
					}
				}
			});

			mOptionsDialog = builder.create();
			Window window = mOptionsDialog.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = mInputView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			mOptionsDialog.show();
		}



		public class LayoutAdapter extends BaseAdapter{

			String[] items;
			public LayoutAdapter(String[] listitems){
				items = listitems;
			}

			@Override
			public int getCount() {
				return items != null ? items.length : 0;
			}

			@Override
			public Object getItem(int position) {
				return position;
			}

			@Override
			public long getItemId(int position) {

				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return null;
			}
		}

		private void changeKeyboardMode() {
			//	loadTheme();
			boolean isContactField = false;
			/*EditorInfo editorIndo = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		int mode = editorIndo.inputType & EditorInfo.TYPE_MASK_CLASS;*/
			int modeType = mInputView.getFocusedEditText().getInputType();

			switch (modeType) {
			case EditorInfo.TYPE_CLASS_PHONE: {
				isContactField = true;
				break;
			}
			}
			if (mKeyboardSwitcher != null) {
				if (mKeyboardSwitcher.isCurrentKeyboardModeQwerty() || isContactField) {
					mKeyboardSwitcher.toggleSymbols(mInputView.getFocusedEditText());
				}else {
					mKeyboardSwitcher.togglePhoneModeChange(mCurrentEditorType);
				}
				if (mCapsLock && mKeyboardSwitcher.isAlphabetMode()) {
					((KPTAdpatxtKeyboard) mInputView.getKeyboard())
					.setShiftLocked(mCapsLock);
				}
			}
			checkIfGlideShouldBeEnabled();
		}

		public static <E> ArrayList<E> newArrayList(E... elements) {
			int capacity = (elements.length * 110) / 100 + 5;
			ArrayList<E> list = new ArrayList<E>(capacity);
			Collections.addAll(list, elements);
			return list;
		}

		public boolean isRevertedWord() {
			if(mCoreEngine != null){
				String revertedWord = mCoreEngine.getRevertedWord();
				return (revertedWord != null && revertedWord != "") ;
			}
			return false;
		}

		// this function will be used by CandidateView, during orientation change
		public void forceRequestLayout(){
			if(mCandidateViewContainer != null ){
				mCandidateViewContainer.requestLayout();
			}
			if (mHardKBCandidateContainer != null) {
				mHardKBCandidateContainer.requestLayout();
			}
		}

		@Override
		public void onHardKey(int primaryCode, int[] keyCodes) {
			mIsSIPInput = false;
			int keyChar = primaryCode;

			if (mhardShift == HKB_STATE_ON) {
				keyChar = Character.toUpperCase(primaryCode);

				// Clear Shift state and its cursor change
				mhardShift = HKB_STATE_OFF;
				int states = MetaKeyKeyListener.META_SHIFT_ON 
						| MetaKeyKeyListener.META_CAP_LOCKED;
				if(mInputConnection != null)
					mInputConnection.clearMetaKeyStates(states);
			}
			else if(mhardShift == HKB_STATE_LOCK ) 
				keyChar = Character.toUpperCase(primaryCode);
			else if(mhardAlt == HKB_STATE_ON) {
				//Clear the Alt state if alt state is on

				mhardAlt = HKB_STATE_OFF;
				int states = MetaKeyKeyListener.META_ALT_ON 
						| MetaKeyKeyListener.META_ALT_LOCKED;
				if(mInputConnection != null)
					mInputConnection.clearMetaKeyStates(states);
			}		 
			handleUserInput(keyChar, mIsSIPInput);
		}

		public KPTKeyboardSwitcher getKeyBoardSwitcher()
		{
			return mKeyboardSwitcher;
		}

		/*public void updateInputConnection(){
		mInputConnection = ((InputMethodService) mContext).getCurrentInputConnection();
	}*/

	public void deleteSurroundingText(int aLeftLen,int aRightLen){
		if(mEditText == null){
			return;
		}
		if(isCursorJump){
			
			SpannableStringBuilder textContainer = new SpannableStringBuilder(mEditText.getText().toString());
			cursorJumpPosition = mEditText.getSelectionEnd();
			if(cursorJumpPosition > 0){
				if(aLeftLen > 0 && cursorJumpPosition >= aLeftLen){
					textContainer.delete((cursorJumpPosition - aLeftLen), cursorJumpPosition);
				//	mEditText.setText("");
				//	mEditText.setText(textContainer);
					cursorJumpPosition = cursorJumpPosition - aLeftLen ;
					//mEditText.setSelection((cursorJumpPosition));
				}
				if(aRightLen > 0){
					int extraspace = 0;
					int lastindex = cursorJumpPosition + (aRightLen);
					if(lastindex < textContainer.length() && Character.isWhitespace(textContainer.charAt(lastindex))){
						extraspace = 1;
					}
					if((cursorJumpPosition + (aRightLen+ extraspace))< textContainer.length()){
						textContainer.delete(cursorJumpPosition, (cursorJumpPosition + (aRightLen+ extraspace)));
					}
				}
				mEditText.setText("",true);
				mEditText.setText(textContainer,true);
				mEditText.setSelection((cursorJumpPosition),true);
				checkTextinEditor();
			}
			
		}else if(!mIsUnknownEditor) {
			//fix for 13635. for few web editors even if chars to be deleted is 0, one char is getting
			//deleted
			if(aLeftLen > 0 || aRightLen > 0) {
				//mInputConnection.deleteSurroundingText(aLeftLen, aRightLen);
				
				SpannableStringBuilder textContainer = new SpannableStringBuilder(mEditText.getText().toString());
				int cursorPosition = mEditText.getSelectionStart();
				
				//Below implementation is for deleting text left of Cursor
				if(aLeftLen > 0 && cursorPosition > 0 && cursorPosition >= aLeftLen){
					if(textContainer != null && textContainer.length() > 0){
						
						textContainer.delete((cursorPosition - aLeftLen), cursorPosition);
						//int startIndex = (cursorPosition - aLeftLen);
						//textContainer.delete(startIndex >= 0 ? startIndex : 0, cursorPosition);
						cursorPosition = cursorPosition-aLeftLen;
						
						
					}
				}

				if(aRightLen > 0 && cursorPosition >= 0) {
					if((cursorPosition + aRightLen)< textContainer.length()){
						textContainer.delete(cursorPosition, (cursorPosition + aRightLen));
					}
				//	mEditText.setText("");
				//	mEditText.setText(textContainer);
				}
				mEditText.setText("",true);
				mEditText.setText(textContainer,true);
				mEditText.setSelection(cursorPosition, cursorPosition,true);
			//	mEditText.setSelection(textContainer.length());


			}
		}
	}

		public CharSequence getTextBeforeCursor(int n, int flags){
			CharSequence tmpCharSequece = null;

			/*if(tmpCharSequece == null || (tmpCharSequece != null && tmpCharSequece.length()== 0) )
				tmpCharSequece = mInputConnection != null ? mInputConnection.getTextBeforeCursor(n, flags):null;
				return tmpCharSequece;*/
			
			if(mEditText != null && mEditText.getText().length() > 0){
				
				//getting the cursor postion from Editor
				int cursorPosition = mEditText.getSelectionStart();
				SpannableStringBuilder enteredText = new SpannableStringBuilder( mEditText.getText().toString());
				
				
				if(n == Integer.MAX_VALUE || n == Integer.MAX_VALUE/2){// we will send these values when we need total text before cursor
					
					tmpCharSequece = enteredText.subSequence(0, cursorPosition);
					
				}else if(cursorPosition > 0 && n >0 && n <= cursorPosition){ 
					
					//here we need text before cursor only if we have the cursor position is more than zero
					//number of charactars before cursor also should be positive 
					//number charactars requested before cursor should be less than cursor position
					
					tmpCharSequece = enteredText.subSequence(cursorPosition-n, cursorPosition);
					
				}else{
					
					//by default we will send the empty string for the scenarios like Begining of the Editor etc.
					tmpCharSequece = "";
				}
				
			}
			
			return tmpCharSequece;
		}

		public CharSequence getTextAfterCursor(int n, int flags){
			CharSequence tmpCharSequece = null;

			
			
			if(mEditText != null && mEditText.getText().length() > 0){
				
				//getting the cursor postion from Editor
				int cursorPosition = mEditText.getSelectionStart();
				int endOfEditor = mEditText.getText().length();
				int totalCharsAferCursor = endOfEditor - cursorPosition;
				SpannableStringBuilder enteredText = new SpannableStringBuilder( mEditText.getText().toString());
				
				
				if(n == Integer.MAX_VALUE || n == Integer.MAX_VALUE/2){// we will send these values when we need total text before cursor
					
					tmpCharSequece = enteredText.subSequence(cursorPosition, endOfEditor);
					
				}else if(n >0 && n <= totalCharsAferCursor && cursorPosition < endOfEditor){ 
					
					//number of charactars after cursor also should be positive 
					//number charactars requested before cursor should be less than or equal to total number of charactars after cursor
					//here we need text after cursor only if we have the cursor position is not at the end of the editor
					
					tmpCharSequece = enteredText.subSequence(cursorPosition, cursorPosition+n);
					
				}else{
					
					//by default we will send the empty string for the scenarios like Begining of the Editor etc.
					tmpCharSequece = "";
				}
				
			}
			
			return tmpCharSequece;
		}

		// Wrapper methods for setting the composing text and finishing the composing mode.
		public void setComposingText(CharSequence text,int newCursorPosition){
			setComposingTextNew(text, newCursorPosition);
		}
		
		
		
		// Wrapper methods for setting the composing text and finishing the composing mode.
		public void setComposingTextNew(CharSequence text,int newCursorPosition){
			
			if (mCoreEngine == null) {
				Log.e("KPT", "Strange setcompositionText mCoreEngine is null");
				return;
			}
 
			if (mEditText == null) {
				Log.e("KPT", "Strange setcompositionText mEditText is null");
				return;
			}
			
			final int coreACP = mCoreEngine.getAbsoluteCaretPosition();
			final int compositionTextLength = text.length();
			String stringInEditor = mEditText.getText().toString();

			StringBuffer completeString = new StringBuffer(stringInEditor);
			
			int cursorPositionInEditor = mEditText.getSelectionEnd();
			int start = coreACP - compositionTextLength ;
			int textLengthInEditor = completeString.length();
			SpannableString spannableString;
			if(start >= 0 && textLengthInEditor >= cursorPositionInEditor){
				completeString.replace(start, cursorPositionInEditor, text.toString());
				processUnderlineEditext(start, coreACP);
				if(mEditText.getSelectionEnd() != coreACP){
					mEditText.setSelection(coreACP,true);
				}
			}else{
				mCoreEngine.syncCoreBuffer(stringInEditor, textLengthInEditor);
				mCoreEngine.setAbsoluteCaretPosition(cursorPositionInEditor);
				if(mEditText.getSelectionEnd() != cursorPositionInEditor){
					mEditText.setSelection(cursorPositionInEditor,true);
				}
			}
			
			
			
			
			mComposingMode = true;
			
	}	
	
		
	 public SpannableString processUnderline(final int start, final int end, final String completeText){
			SpannableString span = new SpannableString(completeText);
			
			if (end <= completeText.length()) {
				//SPAN_COMPOSING
				for (int i = start; i <= end; i++) {
					span.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					//span.setSpan(linkSpec.span, linkSpec.start, linkSpec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			
			return span;
		}
	 public boolean processUnderlineEditext(final int start, final int end){
			if (end <= mEditText.getText().length()) {
				//SPAN_COMPOSING
				for (int i = start; i <= end; i++) {
					mEditText.getText().setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				return true;
			}			
			return false ;
		}
	 
	public void finishComposingText(){
		/*if(mInputConnection != null){
				mInputConnection.finishComposingText();
				mComposingMode = false;
			}*/
			mComposingMode = false;
		}

		public void commitText(CharSequence txt,int newCurPos){
			commitTextNew(txt, newCurPos);
		}
		
		public void commitTextNew (CharSequence txt,int newCurPos){
			if(mEditText == null){
				return;
			}
			mComposingMode = false;
			
			String textInEditor = mEditText.getText().toString();
			StringBuffer tempBuffer = new StringBuffer(textInEditor);
			final int acp = mCoreEngine.getAbsoluteCaretPosition();
			//Log.e(TAG,acp+ " tempBuffer "+ tempBuffer);
			
			/*if (txt.length() == 1 && txt.equals(" ")) {
				Log.e(TAG," in gg");
				tempBuffer.insert(acp-1, txt);
				mEditText.setText(tempBuffer);
				mEditText.setSelection(acp);
				return;
			}*/
			
			tempBuffer.append(txt);
			mEditText.setText(tempBuffer,true);
			mEditText.setSelection(tempBuffer.length(),true);
			
		}
		
		// for enabling and disbaling Xi Key
		public void setXiKeyState(boolean aState){
			//	//Log.e(TAG, "state " + aState);
			if(mAutoCorrectOn){
				if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL ||
						mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_EMAIL ||
						mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_LG_EMAIL) {
					return;
				}
				if(mInputView!=null && mInputView.isShown()){
					KPTAdpatxtKeyboard adaptxtKeyboard = (KPTAdpatxtKeyboard)mInputView.getKeyboard();
					//if (adaptxtKeyboard != null) {
					//	Key xiKey = adaptxtKeyboard.getXiKey();
						//if(xiKey!= null){
							KPTConstants.mXi_enable = aState;
						//	xiKey.on = aState;
						//	mInputView.invalidateKey(adaptxtKeyboard.getXiKeyIndex());
						//}

						/*Used to update the ACC state when xi key is enabled.
						 * As per the requirement ACC dissmiss should not be displayed when xi key is on.
						 */
						if(mCandidateViewContainer != null){
							mCandidateViewContainer.updateView();
						}
					//}
				}
			}
		}



		// for executing the Xi key revert behaviour
		public void executeXiRevert(){
			if (mInputView.getFocusedEditText() == null) {
				return;
			}
			if (mCoreEngine == null) {
				return;
			}
			// Handling revert on cursor jump scenario
			if(isRevertedWord()) {
				// Handling the normal revert functionality( space key will no more be used for revert )
				mIsXiTouched =true;
				if(mSuggList != null){
					// Reverting the AC corrected word
					pickSuggestionManually(0, mSuggList.get(0));
				}
				mIsXiTouched = false;
			}
			// Handling revert on "soon after AC sugg selection" scenario
			else {
				// Just delete one char and apply the revert suggestion
				// to Core engine and editor
				int coreCursorPos = mCoreEngine.getAbsoluteCaretPosition();
				mCoreEngine.setAbsoluteCaretPosition(coreCursorPos - 1);
				mCurrCursorPos = coreCursorPos - 1;

				List<KPTSuggestion> revertSugg = mCoreEngine.getSuggestions();
//				int coreCurPos = mCoreEngine.getAbsoluteCaretPosition();

				if (isRevertedWord()) {
					// mEditText.setSelection(coreCurPos,coreCurPos);
					mIsXiTouched = true;
					// Reverting the AC corrected word
					pickSuggestionManually(0, revertSugg.get(0));
					mIsXiTouched = false;
				} else {
					coreCursorPos = mCoreEngine.getAbsoluteCaretPosition();
					mCoreEngine.setAbsoluteCaretPosition(coreCursorPos + 1);
					mCurrCursorPos = coreCursorPos + 1;
					if(!isHideCV){
						updateEditorText(true, true);
					}
				}
			}
			// Reset the Xi key state
			setXiKeyState(false);
		}

		public void dismissPopupKeyboard(){
			if(mInputView != null && mInputView.isPopupKeyboardShown()){
				mInputView.dismissMoreKeysPanel();
			}
		}

		/**
		 * Method to Enable or Disable Private Mode
		 */
		public void showDialogForPrivateMode() {
			if (mInputView == null || mInputView.getWindowToken() == null) {
				return;
			}
			AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(mContextWrapper);
			myAlertDialog.setIcon(R.drawable.kpt_adaptxt_launcher_icon);

			final SharedPreferences.Editor editor=mSharedPreference.edit(); 


			if(!mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)){
				myAlertDialog.setTitle(mResources.getString(R.string.kpt_UI_STRING_DIALOG_TITLE_1_7003));
				myAlertDialog.setMessage(mResources.getString(R.string.kpt_UI_STRING_DIALOG_MSG_2_7003));
			}
			else{
				myAlertDialog.setTitle(mResources.getString(R.string.kpt_UI_STRING_DIALOG_TITLE_2_7003));
				myAlertDialog.setMessage(mResources.getString(R.string.kpt_UI_STRING_DIALOG_MSG_3_7003));
			}

			myAlertDialog.setPositiveButton(mResources.getString(R.string.kpt_alert_dialog_ok), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface arg0, int arg1) {
					// do something when the OK button is clicked
					boolean isPvtModeOn = (mSharedPreference.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false));
					isPvtModeOn = !isPvtModeOn;
					setPrivateMode(isPvtModeOn);
					editor.putBoolean(KPTConstants.PREF_PRIVATE_MODE,isPvtModeOn);
					editor.commit();

				}});
			myAlertDialog.setNegativeButton(mResources.getString(R.string.kpt_cancel), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface arg0, int arg1) {
					// do something when the Cancel button is clicked
					if(mOptionsDialog!=null)
						mOptionsDialog.dismiss();
				}});
			mOptionsDialog = myAlertDialog.create();
			Window window = mOptionsDialog.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = mInputView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			mOptionsDialog.show();


		}

		public void setPrivateMode(Boolean isPvtModeON){
			if(mCoreEngine != null)
				mCoreEngine.setPrivateMode(isPvtModeON);
		}


	public int getExtractedTextLength() {
		if (mEditText != null) {
			if (/*extracted != null && */mEditText.getText() != null) {
				return mEditText.getText().length();

			}
		}
		return 0;
	}

		public void setATRstatus(){
			Editor edit = mSharedPreference.edit();
			boolean mAtrEnable = false;
			if (mSharedPreference.getBoolean(KPTConstants.PREF_ATR_FEATURE,
					true)){
				edit.putBoolean(KPTConstants.PREF_ATR_FEATURE, false);
			}
			else{
				edit.putBoolean(KPTConstants.PREF_ATR_FEATURE, true);
				mAtrEnable = true;
			}
			edit.commit();
			if(mCoreEngine != null){
				mCoreEngine.setATRStatus(mAtrEnable);
			}
		}

		public void trimAccentDialog() {
			if (mInputView == null || mInputView.getWindowToken() == null) {
				return;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setIcon(R.drawable.kpt_adaptxt_launcher_icon);
			builder.setTitle(mResources.getString(R.string.kpt_UI_STRING_ATRIM_ITEM1));
			builder.setMessage(mResources.getString(R.string.kpt_UI_STRING_ATRIM_ITEM2));
			builder.setCancelable(true);
			builder.setPositiveButton(mResources.getString(R.string.kpt_alert_dialog_ok),
					new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

					String str = mResources.getString(
							R.string.kpt_UI_STRING_TOAST_2_7003);
					Toast.makeText(mContext, str,
							Toast.LENGTH_LONG).show();
					trimAccents();
				}
			});

			builder.setNegativeButton(android.R.string.cancel,
					new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			AlertDialog trimDialog = builder.create();
			Window window = trimDialog.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = mInputView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			trimDialog.show();
		}

		public void trimAccents() {
			if (mEditText == null) {
				return;
			}
			if (mCoreEngine == null) {
				return;
			}

			finishComposingText();
			EditText extracted = mInputView.getFocusedEditText();/*mInputConnection.getExtractedText(
					new ExtractedTextRequest(), 0);*/
			if (extracted == null) {
				return;
			}
			extracted.beginBatchEdit();

		if(extracted != null) {
			CharSequence extractedText = extracted.getText();
			if (extractedText != null) {
				//int extractedTextLength = extractedText.toString().length();
				//CharSequence textBeforeCursor = getTextBeforeCursor(extractedTextLength, 0);
				/*if(textBeforeCursor!= null && textBeforeCursor.length()>0 || textBeforeCursor.equals("")){
					deleteSurroundingText(textBeforeCursor.length(),
							extractedTextLength - textBeforeCursor.length());
				}*/
				String str = mCoreEngine.trimAccents(extractedText.toString());
				extracted.setText("");
				commitText(str, 1);
			}
		}

			extracted.endBatchEdit();

		addPreExistingTextToCore(false);
		if(!KPTConstants.mIsNavigationBackwar){
			KPTConstants.mIsNavigationBackwar = true;
		}
		updateSuggestions(true);

	}

	/**
	 * dismiss the quick settings dialog
	 */
	public void dismissRemoveDialog() {
		if(mSuggDelDialog != null) {
			mSuggDelDialog.dismiss();
		}
	}
	public void removeWordDialog(KPTSuggestion selectedSuggestion,
			int selectedIndex, boolean isfromCandidateView) {
		if (selectedSuggestion == null)
			return;

			mIsSuggFromCandidateView = isfromCandidateView;
			mSelectedSuggestion = selectedSuggestion;

			mSuggDelDialog = new Dialog(mContextWrapper);
			mSuggDelDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		WindowManager.LayoutParams lp = mSuggDelDialog.getWindow().getAttributes();
		/*if (!((InputMethodService) mContext).isInputViewShown() && mHardKBCandidateContainer.getWindowToken() != null) {
			lp.token = this.mHardKBCandidateContainer.getWindowToken();
		}else if (mCandidateViewContainer.getWindowToken() != null){*/
			lp.token = this.mCandidateViewContainer.getWindowToken();
			//}

			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			lp.dimAmount = 0.5f;
			lp.y = lp.y;

			mSuggDelDialog.getWindow().setAttributes(lp);
			mSuggDelDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			mSuggDelDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			mSuggDelDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

			mSuggDelDialog.setContentView(R.layout.kpt_deleteword);
			mSuggDelDialog.setCanceledOnTouchOutside(true);
			mSuggDelDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {

				if( mCandidateView!=null ){					
					mCandidateView.registerOnTouchEvent();	
					if(mCandidateView.mCandidatePopup!=null ){				
						mCandidateView.mCandidatePopup.registerOnTouchEvent();
					}
				}

				if(  mHardKBCandidateView!=null){				
					mHardKBCandidateView.registerOnTouchEvent();
					if(mHardKBCandidateView.mCandidatePopup !=null){
						mHardKBCandidateView.mCandidatePopup.registerOnTouchEvent();	
					}
				}



			}});

		TextView tvInsert = (TextView) mSuggDelDialog.findViewById(R.id.del_word_insert);
		TextView tvDelete = (TextView) mSuggDelDialog
				.findViewById(R.id.del_word_deleteword);
		TextView tvTitle = (TextView) mSuggDelDialog.findViewById(R.id.del_word_title);
		TextView tvWordMeaning = (TextView) mSuggDelDialog.findViewById(R.id.word_meaning);
		Button btnCancel = (Button) mSuggDelDialog.findViewById(R.id.del_cancel);
		LinearLayout llParent = (LinearLayout) mSuggDelDialog
				.findViewById(R.id.deleteparent);

		tvInsert.setBackgroundColor(Color.WHITE);
		tvDelete.setBackgroundColor(Color.WHITE);
		tvWordMeaning.setBackgroundColor(Color.WHITE);

		tvWordMeaning.setText(mContext.getResources().getString(R.string.kpt_UI_STRING_POPUP_ITEMS_1_1000));

		int totalWidth = mResources.getDisplayMetrics().widthPixels;
		int currWidth = (totalWidth * 75) / 100;
		llParent.setLayoutParams(new FrameLayout.LayoutParams(currWidth,
				LayoutParams.FILL_PARENT));
		tvTitle.setText('"'+mSelectedSuggestion.getsuggestionString()+'"');

		if (selectedSuggestion.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR) {
			if (mCoreEngine != null) {
				String str = mSelectedSuggestion.getsuggestionString().substring(1);
				String atrExpansion = mCoreEngine.getAtrExpansion(str);
				if (atrExpansion == null) {
					return;
				}

				if (atrExpansion.length() > 23)
					tvTitle.setText('"' + atrExpansion.substring(0, 23) + '"');
				else
					tvTitle.setText('"' + atrExpansion + '"');

				tvDelete.setText(mResources.getString(
						R.string.kpt_UI_STRING_POPUP_ITEMS_6_7003));
			}
		}

		if (selectedSuggestion.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR) {
			tvDelete.setEnabled(true);
			tvDelete.setClickable(true);
			tvDelete.setFocusable(true);
			tvDelete.setTextColor(Color.BLACK);
			tvWordMeaning.setEnabled(false);
			tvWordMeaning.setClickable(false);
			tvWordMeaning.setFocusable(false);
			tvWordMeaning.setTextColor(Color.GRAY);
		} else if (!selectedSuggestion.getsuggestionIsUserDicWord()) {
			tvDelete.setEnabled(false);
			tvDelete.setClickable(false);
			tvDelete.setFocusable(false);
			tvDelete.setTextColor(Color.GRAY);
			tvWordMeaning.setEnabled(true);
			tvWordMeaning.setClickable(true);
			tvWordMeaning.setFocusable(true);
			tvWordMeaning.setTextColor(Color.BLACK);
		} else {
			tvDelete.setEnabled(true);
			tvDelete.setClickable(true);
			tvDelete.setFocusable(true);
			tvDelete.setTextColor(Color.BLACK);
			tvWordMeaning.setEnabled(false);
			tvWordMeaning.setClickable(false);
			tvWordMeaning.setFocusable(false);
			tvWordMeaning.setTextColor(Color.GRAY);
		}

		tvInsert.setOnClickListener(this);
		tvDelete.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		tvWordMeaning.setOnClickListener(this);

		// in this user case User number not needed to search in wikipedia.
		if (mShowApplicationCandidateBar) {
			tvWordMeaning.setEnabled(false);	
			tvWordMeaning.setTextColor(Color.GRAY);
		}
		if (mSuggDelDialog != null) {
			mSuggDelDialog.show();
		}
	}


	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.del_word_insert) {
			v.setBackgroundColor(mResources.getColor(R.color.kpt_orange_selector_dialog));
			if (mIsSuggFromCandidateView){
				if (mCandidateView != null) {
					mCandidateView.insertSuggestion();
				}
				if (mHardKBCandidateView != null) {
					mHardKBCandidateView.insertSuggestion();
				}
			}else{
				if (mCandidateView != null) {
					mCandidateView.insertSuggestionforCandidatePopup();
				}
				if (mHardKBCandidateView != null) {
					mHardKBCandidateView.insertSuggestionforCandidatePopup();
				}
			}
			if (mSuggDelDialog != null && mSuggDelDialog.isShowing()) {
				mSuggDelDialog.dismiss();
			}
		} else if (id == R.id.del_word_deleteword) {
			v.setBackgroundColor(mResources.getColor(R.color.kpt_orange_selector_dialog));
			if (mSelectedSuggestion != null 
					&& mSelectedSuggestion.getsuggestionIsUserDicWord() 
					&& mCoreEngine != null) {
				String[] strArrUserWord = new String[1];
				String selectedSuggestionString = mSelectedSuggestion.getsuggestionString();
				if (selectedSuggestionString != null) {
					strArrUserWord[0] = selectedSuggestionString.toLowerCase();
					mCoreEngine.removeUserDictionaryWords(strArrUserWord);
				}
			}
			if (mCoreEngine != null
					&& mSelectedSuggestion != null
					&& mSelectedSuggestion.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR) {
				String selectedSuggestionString = mSelectedSuggestion.getsuggestionString();
				if (selectedSuggestionString != null && selectedSuggestionString.length() > 1) {
					String str = selectedSuggestionString.substring(1);
					mCoreEngine.removeATRShortcutAndExpansion(str);
				}
			}
			updateSuggestions(true);
			if (mSuggDelDialog != null && mSuggDelDialog.isShowing()) {
				mSuggDelDialog.dismiss();
			}
		} else if (id == R.id.del_cancel) {
			if (mSuggDelDialog != null && mSuggDelDialog.isShowing()){
				mSuggDelDialog.dismiss();
			}
		} else if (id == R.id.word_meaning) {
			if (mLanguageSwitcher == null) {
				return;
			}
			Configuration conf = mResources.getConfiguration();
			Locale saveLocale = conf.locale;
			conf.locale = mLanguageSwitcher.getInputLocale();
			mResources.updateConfiguration(conf, null);
			String url = mResources.getString(R.string.kpt_MEANING_LOOKUP_URL)+( mSelectedSuggestion != null ? mSelectedSuggestion.getsuggestionString() :"");
			if (!url.startsWith("https://") && !url.startsWith("http://")){
				url = "http://" + url;
			}
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(url));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			conf.locale = saveLocale;
			mResources.updateConfiguration(conf, null);
			if (mSuggDelDialog != null && mSuggDelDialog.isShowing()){
				mSuggDelDialog.dismiss();
			}
		}
	}

	/**
	 * if customization dialog is displayed when a phone call is recived
	 * dismiss the dialog 
	 * @author 
	 */
	private class StateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged (int state, String incomingNumber) {
			if(state == TelephonyManager.CALL_STATE_RINGING) {
				if(mKPTQuickSettingManager != null) {
					mKPTQuickSettingManager.dissMissQuickSettingDialog();
					dismissRemoveDialog();
				}
			} 
		}
	}



	/**
	 * Handle key event w.r.t SIP and Hard key 
	 * 
	 * The difference between this method and handleUserInput(int primaryCode) is only in editor updation.
	 * This method doesn't update the editor, it leaves the editor updating to the framework 
	 * for HKB since the cursor change is handled by framework for Alt & Shift.
	 * 
	 * @param primaryCode - Entered keyCode
	 * @param isSIP - Indicates whether the input is from SIP or not (HKB)
	 */
	public void handleQWERTYThaiLayoutInput(int primaryCode, boolean isSIP){
		if(mCoreEngine == null /*(((InputMethodService) mContext).getCurrentInputEditorInfo().inputType == NOT_AN_EDITOR && !mInputView.isShown())*/){
			return;
		}
		mCharAdded = true;

		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}
		if(mInputConnection != null){
			boolean isTextSelected = false;
			mInputConnection.beginBatchEdit();
			ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);

			if(shouldAddExtraSpace(primaryCode)) {
				handleThaiCommit();
			}

			if(extracted!= null && extracted.selectionStart != extracted.selectionEnd){
				mCharAdded = false;
				isTextSelected = true; //Bug Fix: 5784 - Kashyap
			}

			if(!isTextSelected) { //Bug Fix: 5784 - Kashyap
				//				finishComposingText();
				KPTCorrectionInfo info = null;
				boolean isTextBeforeCursor = false;

				CharSequence txtBeforeCursor = getTextBeforeCursor(1, 0);
				if(txtBeforeCursor != null){
					String strBeforeCur = txtBeforeCursor.toString();
					//Thai: Adding the punctuation character to core.. check and add space if needed.

					info = mCoreEngine.addCharThai((char)primaryCode,
							((char)KPTConstants.KEYCODE_SPACE+"").equalsIgnoreCase(strBeforeCur)? true : false, 
									/*mJustAddedAutoSpace*/false, mCandidateViewContainer.getAccDsmissState()
									, false);
					// This check is required to identify whether any printable
					// chars are there just before the cursor
					if(strBeforeCur != null &&
							!strBeforeCur.equalsIgnoreCase("") &&
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE) && 
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_ENTER)){
						isTextBeforeCursor = true;
					}
				} else {
					info = mCoreEngine.addCharThai((char)primaryCode,
							false , false/*mJustAddedAutoSpace*/, mCandidateViewContainer.getAccDsmissState()
							, false);
				}
				if(info == null){//Bug Fix: 6172, 6166 - Check not needed
					// Handling QWERTY here as the inlining concept has been changed to be
					// handled here itself rather than in SetpreExistingTextToComposingMode()
					// while typing text

					CurrentWord currentWord = mCoreEngine.getCurrentWord();
					String currWord[] = null;
					if(currentWord!=null)
						currWord = currentWord.currWord;

					if(!(primaryCode == KPTConstants.KEYCODE_SPACE || primaryCode == KeyEvent.KEYCODE_SPACE)
							&& currWord != null && currWord[0] != null && currWord[0].length()> 0){
						// TP 8276,8277 making this check to make sure that text is not appended when there is partial in-lining
						if(isTextBeforeCursor && !mComposingMode){
							commitText(String.valueOf((char)primaryCode), 1);
							mExpectingUpdateSelectionCounter++;
							setPreExistingTextToComposingMode(mInputConnection);
						}else{
							/*  Bug Fix for : "The input English characters cover digits after input comma symbol following the digits"
							 * If the suggestion offset is true, finish the composing mode and start a new composing text.
							 */
							if(currentWord.suggestionOffsetChanged){
								finishComposingText();
							}
							if(currWord[1] != null) {
								setComposingText(currWord[0]+ currWord[1], 1);
							}else {
								setComposingText(currWord[0], 1);
							}

							int coreCaretPos = mCoreEngine.getAbsoluteCaretPosition();
							mInputConnection.setSelection(coreCaretPos, coreCaretPos);

						}
					}else{
						// probably punctuation so commit previous inline text
						finishComposingText();
						//Thai: Commiting punctuation.. commit here.. 

						commitText(
								String.valueOf((char) primaryCode), 1);
					}
					if(primaryCode == KPTConstants.KEYCODE_SPACE && mJustAddedAutoSpace) {
						//dont make it false. made this change for space multi tap
					} else {
						mJustAddedAutoSpace = false;
					}
				} else { /* Update Editor w.r.t core as auto suggestion has been done*/
					// Handling all the auto backspacing & AC selection here
					finishComposingText();
					deleteSurroundingText(info.getCharsToRemoveBeforeCursor(), info.getCharsToRemoveAfterCursor());
					commitText(info.getModString(), 1);
					// remove extra space
					CharSequence textAfterCursor = getTextAfterCursor(1, 0);
					if(textAfterCursor != null){
						String ch = textAfterCursor.toString();
						if(ch != null && ch.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE)){
							deleteSurroundingText(0, 1);
							mCoreEngine.removeChar(false);
						}
					}
					//	if(mSuggList!=null && mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
					// Fix for TP 9120	
					if(mSuggList != null && mSuggList.size() >0 && mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION){
						setXiKeyState(true);
					}

					mJustAddedAutoSpace = true;
				}
			} else {
				commitText(String.valueOf((char)primaryCode), 1);
				mCoreEngine.resetCoreString(); // TP 8097
				mJustAddedAutoSpace = false;
			}
			mInputConnection.endBatchEdit();
			browserTest();
		}
		resetAccDismiss();

		/*if(isSIP){
			updateShiftKeyState(((InputMethodService) mContext).getCurrentInputEditorInfo());
		}else{ 
			updateHKBShiftKeyState(((InputMethodService) mContext).getCurrentInputEditorInfo());
		}*/
		postUpdateSuggestions(10);
	}

	public void handlePhoneThaiLayoutInput(int primaryCode, boolean isSIP){
		if(mCoreEngine == null /*|| (((InputMethodService) mContext).getCurrentInputEditorInfo().inputType == NOT_AN_EDITOR && !mInputView.isShown())*/){
			return;
		}
		if (mCandidateViewContainer == null) {
			return;
		}
		mCharAdded = true;

		mCounter = 0;
		if(mHandler != null){
			mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
			mHandler.removeMessages(ALL_DPADS_CONSUME);
		}

		boolean isSpace = false;
		boolean isTextSelected = false;
		if(mInputConnection != null){
			mInputConnection.beginBatchEdit();
			ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
			if(extracted!= null && extracted.selectionStart != extracted.selectionEnd){
				mCharAdded = false;
				isTextSelected = true; //Bug Fix: 5784 - Kashyap
			}

			//Space pressed when revert word is there replace with revert word.
			if (primaryCode == KPTConstants.KEYCODE_SPACE || primaryCode == KeyEvent.KEYCODE_SPACE) {
				isSpace = true;
			}

			if(!isTextSelected) { //Bug Fix: 5784 - Kashyap
				//				finishComposingText();
				KPTCorrectionInfo info = null;
				boolean isTextBeforeCursor = false;
				CharSequence txtBeforeCursor = getTextBeforeCursor(1, 0);
				// Fix for TP 8163
				boolean atxOn = mAtxStateOn && !mIsPopupChar 
						&& mKeyboardSwitcher != null ? mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET : false;
				if(txtBeforeCursor != null){
					String strBeforeCur = txtBeforeCursor.toString();
					info = mCoreEngine.addChar((char)primaryCode,
							((char)KPTConstants.KEYCODE_SPACE+"").equalsIgnoreCase(strBeforeCur)? true : false, 
									false, mCandidateViewContainer.getAccDsmissState()
									, atxOn);
					// This check is required to identify whether any printable
					// chars are there just before the cursor
					if(strBeforeCur != null &&
							!strBeforeCur.equalsIgnoreCase("") &&
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE) && 
							!strBeforeCur.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_ENTER)){
						isTextBeforeCursor = true;
					}
				} else {
					info = mCoreEngine.addChar((char)primaryCode,
							false , false, mCandidateViewContainer.getAccDsmissState()
							, atxOn);
				}
				if(info == null){//Bug Fix: 6172, 6166 - Check not needed
					// Handling 12 key predictive and multitap separately as multitapping doesn't require
					// inlining functionality
					int phoneKeyboardState = KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
					if (mKeyboardSwitcher != null) {
						phoneKeyboardState = mKeyboardSwitcher.getPhoneKeypadState();
					}
					if(!isSpace && !mIsPopupChar && phoneKeyboardState == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET && mAtxStateOn){
						// TP 8276,8277 making this check to make sure that text is not appended when there is partial in-lining
						if(isTextBeforeCursor && !mComposingMode){
							setPreExistingTextToComposingMode(mInputConnection);
						}else{
							String currWord = mCoreEngine.getPredictiveWord();
							if(currWord != null && currWord.length() > 0) {
								setComposingText(currWord, 1);
							}
						}
					}else{
						finishComposingText();
						commitText(String.valueOf((char)primaryCode), 1);

					}
					mExpectingUpdateSelectionCounter++;
					if(primaryCode == KPTConstants.KEYCODE_SPACE && mJustAddedAutoSpace) {
						//dont make it false. made this change for space multi tap
					} else {
						mJustAddedAutoSpace = false;
					}

					//mJustAddedAutoSpace = false;
					//updateSuggestions();
				} else { /* Update Editor w.r.t core as auto suggestion has been done*/
					// Handling 12 key multitapping's AC selection, all the auto backspacing here
					finishComposingText();
					deleteSurroundingText(info.getCharsToRemoveBeforeCursor(), info.getCharsToRemoveAfterCursor());
					commitText(info.getModString(), 1);
					mExpectingUpdateSelectionCounter++;
					// remove extra space
					CharSequence textAfterCursor = getTextAfterCursor(1, 0);
					if(textAfterCursor != null){
						String ch = textAfterCursor.toString();
						if(ch != null && ch.equalsIgnoreCase("" + (char)KPTConstants.KEYCODE_SPACE)){
							deleteSurroundingText(0, 1);
							mCoreEngine.removeChar(false);
						}
					}
					//	if(mSuggList!=null && mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
					// Fix for TP 9120	
					if(mSuggList != null && mSuggList.size() > 0 && mSuggList.get(0).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION)
					{
						setXiKeyState(true);
					}

					mJustAddedAutoSpace = true;
				}
			} else {
				commitText(String.valueOf((char)primaryCode), 1);
				mExpectingUpdateSelectionCounter++;
				mCoreEngine.resetCoreString(); // TP 8097
				mJustAddedAutoSpace = false;
			}
			mInputConnection.endBatchEdit();
			browserTest();
		}
		resetAccDismiss();

		updateShiftKeyState(mEditText);

		if(mAtxStateOn) {
			updateSuggestions(true);
		} else {
			postUpdateSuggestions(MULTITAP_SUGGESTION_INTERVAL);
		}

	}

	public boolean isSuggestionDelDialogShown() {
		if(mSuggDelDialog != null && mSuggDelDialog.isShowing()){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Update customized candidate and keyboard height changes.
	 * Remaining all customization changes will be handled in setKeyboard of keyboardview
	 * 
	 */
	private void applyCustomizedKeyboardAndCandidateHeightChanges() {
		if (mInputView == null) {
			return;
		}
		if (mCandidateViewContainer == null) {
			return;
		}
		if (mCandidateView == null) {
			return;
		}
		if (mSuggestuicontainer == null) {
			return;
		}
		if(mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			/**
			 * check candidate height
			 */
			int savedSuggestionHeight = mSharedPreference.getInt(KPTConstants.PREF_CUST_SUGGESTION_HEIGHT, 
					(int) mResources.getDimension(R.dimen.kpt_candidate_strip_height));

			android.view.ViewGroup.LayoutParams params1 = mSuggestuicontainer.getLayoutParams();
			params1.height = savedSuggestionHeight;
			mSuggestuicontainer.setLayoutParams(params1);

			//TP 12035
			android.view.ViewGroup.LayoutParams params2 = mCandidateViewContainer.getLayoutParams();
			if(params2 != null) {
				params2.height = savedSuggestionHeight;
				mCandidateViewContainer.setLayoutParams(params2);
			}

		}else{
			int savedSuggestionHeight = mSharedPreference.getInt(KPTConstants.PREF_CUST_SUGGESTION_LANDSCAPE_HEIGHT, 
					(int) mResources.getDimension(R.dimen.kpt_candidate_strip_height));

			//if(savedSuggestionHeight != -1) {
			android.view.ViewGroup.LayoutParams params1 = mSuggestuicontainer.getLayoutParams();
			params1.height = savedSuggestionHeight;
			mSuggestuicontainer.setLayoutParams(params1);

			//TP 12035
			android.view.ViewGroup.LayoutParams params2 = mCandidateViewContainer.getLayoutParams();
			if(params2 != null) {
				params2.height = savedSuggestionHeight;
				mCandidateViewContainer.setLayoutParams(params2);
			}
			//}
		}
		isComputeInsetsDoneOnce = false;
		mCandidateViewContainer.updateCandidateBar(); //TP 11927
		mCandidateView.invalidate();

		/**
		 * check if user has changes any key height
		 */
		int saveKeyHeight = mSharedPreference.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, -1);
		if(saveKeyHeight == -1) {
			saveKeyHeight = mSharedPreference.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL_LANDSCAPE, -1);
		}
		boolean isCustomizationJustClosed = mSharedPreference.getBoolean(KPTConstants.PREF_CUSTOMIZATION_CLOSED, false);
		if(isCustomizationJustClosed && mKeyboardSwitcher != null){
			mKeyboardSwitcher.makeKeyboards();
		}			
		if(saveKeyHeight != -1 && mInputView.getKeyboard() != null) {
			//TP 11931
			List<Key> keys = mInputView.getKeyboard().getKeys();
			if(keys != null && keys.size() > 0) {
				//TP 12267
				isCustomizationJustClosed = mSharedPreference.getBoolean(KPTConstants.PREF_CUSTOMIZATION_CLOSED, true);
				int alreadyDrawnKeyHeight = keys.get(0).height;
				//compare any keyheight with saved key height
				if(alreadyDrawnKeyHeight != saveKeyHeight || isCustomizationJustClosed) {
					//this indicates that the user has just updated the keyheight from
					//customization and returned back. so call toggle language
					//to reset all keyboards

					////Log.e("kpt","heights are not same so draw again");
					toggleLanguage(true, false);
					if(mInputView != null) {
						mInputView.requestLayout();
					}

				} else {
					////Log.e("kpt","heights are same");

					//Already keyboard with updated height is drawn, so no need 
					//to create again
				}
			}
			isComputeInsetsDoneOnce = false;
			/*mKeyboardSwitcher.updateAllKeyboardHeights(saveKeyHeight);	
				mInputView.keyBoardHeightChanged(saveKeyHeight);
				((KPTAdpatxtKeyboard)mInputView.getKeyboard()).themeUpdated();*/
		}
		mSharedPreference.edit().putBoolean(KPTConstants.PREF_CUSTOMIZATION_CLOSED, false).commit();
	}

	/*public void setCandidatesViewShown(boolean shown) {
		super.setCandidatesViewShown(shown);
	}*/

	@Override
	public void onGlideCompleted(float[] xCoordinates, float[] yCoordinates) {
		try{

			if(mCoreEngine == null) {
				return;
			}
			
			if(mEditText == null){
				return;
			}
			if(mEditText.getText().length()  == mEditText.getmMaxLength()){
				return;
			}
			if(mUserSelectedCompleteText){
				//			mEditText.setText("");
				mCoreEngine.resetCoreString();
				mUserSelectedCompleteText = false;
				mEditText.setUserSelectedPSomeText(false);
				resetTextBuffer();
			}else if (mEditText.getUserSelectedPSomeText()) {
				mEditText.setUserSelectedPSomeText(false);
				final int selectionStart = mEditText.getSelectionStart();
				final int selectionEnd = mEditText.getSelectionEnd();
				mCoreEngine.replaceString(selectionStart,selectionEnd, "");

				deleteBuffChars(selectionStart, selectionEnd, false);
			}
			
			
			int beforeLength = getExtractedTextLength();
			if(mSuggList != null && mSuggList.size() > 0) {
				KPTSuggestion sugg = mSuggList.get(0);
				if(sugg.getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION) {
					handleUserInput(KPTConstants.KEYCODE_SPACE, true);
				}
			}

			//vibrate(VIBRATE_GENERIC);

			finishComposingText();
			//first commit space then insert suggestion
			if(shouldAddExtraSpace(-1)) {
				//Bug fix for 13264 Use Case 1
				if(mEditText.getmMaxLength() == -1 || mEditText.getText().length() + 1 <= mEditText.getmMaxLength()){
					
					mCoreEngine.insertText(" ", false, true);
	//				commitText(" ", 1);
					String inputStr = " ";
					insertOrAppendBuffer(inputStr, true);
				}
				//handleUserInput(KPTConstants.KEYCODE_SPACE, true);

				//except for shift ON state notify core about shift

			}
			if(!mCapsLock && !mInputView.getKeyboard().isShifted()) {
				updateShiftKeyState(mEditText);
			} 

			//send glide coords to core and update suggestions
			mCoreEngine.setGlideCoords(xCoordinates, yCoordinates, KPTConstants.GLIDE_SUGG_COMPLETION);
			updateSuggestions(true);
		
			finishComposingText();
			if(mGlideSuggestion != null) {
				String glideSugg = mGlideSuggestion.getsuggestionString();
				int sugLength =  glideSugg.length();
				mCoreEngine.insertText(glideSugg, false, true,true);
				if(mEditText.getmMaxLength() == -1 || mEditText.getText().length() + sugLength <= mEditText.getmMaxLength()){
					
					insertOrAppendBuffer(glideSugg, true);
				}else{
					mCoreEngine.removeString(true, sugLength);
					setCurrentInputTypeToNormal();
					setPreviousInputTypeToNormal();
					setGlideSuggestionToNull();
				}
				
			}
			updateSuggestions(true);
//			handleGlideInput();
			updateShiftKeyState(mInputView.getFocusedEditText());
			setXiKeyState(false);

			int afterLength = getExtractedTextLength();
			String extracted = mInputView.getFocusedEditText().getText().toString();// mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
//			removeExtraCharsFromCoreIfRequired(beforeLength, afterLength, extracted);
			PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
			PointerTracker.mCurrentSyllableIndex = getSyllableIndex();
			changeKeyboard();
		}catch(Exception e){

		}
	}

	public void handleGlideInput(){
		if(mCoreEngine == null || mEditText == null){
			return;
		}
		if(mGlideSuggestion != null) {
			mCoreEngine.insertText(mGlideSuggestion.getsuggestionString(), false, true,true);
			if(mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL && mExtractedText == null){
				commitText(mGlideSuggestion.getsuggestionString(), 1);
			} else {
				
				final int cursorPosition = mEditText.getSelectionStart();
				
				String corebuffer = mCoreEngine.getCoreBuffer();
				final int acp = mCoreEngine.getAbsoluteCaretPosition();
				final int start = acp - mGlideSuggestion.getsuggestionString().length();
				
				SpannableString spannableString = processUnderline(start, acp, corebuffer.toString());
				mEditText.setText(spannableString,true);
				mEditText.setSelection(acp,true);
				//setComposingText(mGlideSuggestion.getsuggestionString(), 1);
				//mInputConnection.setComposingText(mGlideSuggestion.getsuggestionString(), 1);
			}
			mComposingMode = true;
			//setComposingText(mGlideSuggestion.getsuggestionString(), 1);
		}
	}

	private void setGlideSuggestionToNull() {
		mGlideSuggestion = null;
	}

	private void setCurrentInputTypeToNormal() {
		mInputView.setCurrentTextEntryType(KPTConstants.TEXT_ENTRY_TYPE_NORMAL);
	}

	private void setPreviousInputTypeToNormal() {
		mInputView.setPreviousTextEntryType(KPTConstants.TEXT_ENTRY_TYPE_NORMAL);

	}

	/**
	 * Previous  +  Current
	 * ------------------------
	 *   glide 	 + 	  glide 	= space
	 *   glide 	 + 	  tap 		= space
	 *   tap 	 + 	  tap 		= NO space
	 *   tap 	 +    glide 	= space
	 * 
	 * @return
	 */
	public boolean shouldAddExtraSpace(int primaryCode) {
		boolean shouldAddSpace = false;
		boolean isEditorEmpty = false;
		boolean isPreviousTextSpaceOrEnter = false;
		boolean isTextSelected = false;
		boolean isPreviousCharSymbol = false;

		//ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		CharSequence textBefCur = getTextBeforeCursor(1, 0);

		if(textBefCur != null) {
			isPreviousTextSpaceOrEnter = textBefCur.equals(" ") || textBefCur.equals("\n");
			isPreviousCharSymbol = isSepecialChar(textBefCur);
		}

		//Checking special char for only glide input 
		if(isPreviousCharSymbol && primaryCode == -1){
			return false;
		}

		if(mEditText != null && mEditText.getText() != null) {
			// If editor is empty or text is selected then we donot add space
			isTextSelected = (mEditText.getText().length() == 0) || (mEditText.getSelectionStart() != mEditText.getSelectionEnd());
			isEditorEmpty = TextUtils.isEmpty(mEditText.getText());
		}

		if(mEditText == null && mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL) {
			textBefCur = getTextBeforeCursor(5, 0);
			if(textBefCur != null && textBefCur.length()>0) {
				isEditorEmpty = false;
			}
		}
		if(mInputView == null){
			return shouldAddSpace;
		}
		if(mInputView.isCurrentKeyLongPressed() || primaryCode == KPTConstants.KEYCODE_SPACE ) {
			//Fix for tp 13208. add space when a digit is entered after gliding a word
			//Fix for tp 13386. Space is entered along with the number upon long pressing any key without accent bubble 
			if(!isEditorEmpty && !isPreviousTextSpaceOrEnter && !isBeginningOfEditor()
					&& !isTextSelected && Character.isDigit(primaryCode) && !isSepecialChar(Character.toString((char)primaryCode))
					&& mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {

				return true;
			}
			return shouldAddSpace;
		}

		if(mInputView.getCurrentTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE) {
			if(isPreviousTextSpaceOrEnter 
					|| isTextSelected
					|| isBeginningOfEditor()){
				shouldAddSpace = false;
			}  else {
				shouldAddSpace = true;
			}

		} else if(mInputView.getPreviousTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_GLIDE
				&& mInputView.getCurrentTextEntryType() == KPTConstants.TEXT_ENTRY_TYPE_NORMAL) {
			finishComposingText();
			if( isPreviousTextSpaceOrEnter || isTextSelected || isEditorEmpty
					|| isBeginningOfEditor()
					|| primaryCode == KPTConstants.KEYCODE_PERIOD
					|| primaryCode == KPTConstants.KEYCODE_MIC || isSepecialChar(Character.toString((char)primaryCode))){
				shouldAddSpace = false;
			}  else {
				shouldAddSpace = true;
			}
		}
		return shouldAddSpace;
	}

	private boolean isBeginningOfEditor() {
		boolean retVal = false;
		if (null == mInputConnection)  {
			return retVal;
		}
		final ExtractedText extracted = mInputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if (extracted == null) {
			if(mIsUnknownEditor) { 
				CharSequence textBefCur = mInputConnection.getTextBeforeCursor(1, 0);
				if(textBefCur != null) {
					retVal = textBefCur.equals("");
				}
			}
		} else {
			int cusrorPos = extracted.startOffset + extracted.selectionStart;
			if(cusrorPos == 0) {
				retVal = true;
			}
		}

		return retVal;
	}

	/**
	 * Glide is enabled only for alphabet mode. Currently only supported for
	 * qwerty layout
	 */
	public boolean checkIfGlideShouldBeEnabled() {
		//boolean isAlpabetMode = mKeyboardSwitcher.isAlphabetMode();

		boolean canGlide = false;
		String keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);
		/*if(mContext.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
			keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}else{
			keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}*/

		mIsGlideEnabled = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true);
		boolean enabledInSettings = mIsGlideEnabled;
		boolean isEditorActionSearch = false;
		/*EditorInfo attr = ((InputMethodService) mContext).getCurrentInputEditorInfo();
		if (attr != null) {
			int imeOptionKey = attr.imeOptions & (EditorInfo.IME_MASK_ACTION |EditorInfo.IME_FLAG_NO_ENTER_ACTION);
			if(imeOptionKey == EditorInfo.IME_ACTION_SEARCH) {
				isEditorActionSearch = true; 
			}
		}*/


		boolean enableGlide = (!isHideCV  && mSuggestionBar)  || isEditorActionSearch;
		boolean isQwertyLayout = keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY);
		boolean canGlideInThisEditor = mInputView.getKeyboard().isGlideSupportedForCurrentKeyboard() 
				&& enableGlide
				&& !mPasswordText;
		

		if(mCoreEngine != null && isEditorActionSearch){
			

			updateShiftStateToCore();

			if(isHideCV || !mSuggestionBar || mPasswordText == true || mShowApplicationCandidateBar ){
				mCoreEngine.setBlackListed(true);
			}else{
				mCoreEngine.setBlackListed(false);
			}

		}

		/*//Log.e("KPT","enableGlide-->" + enableGlide);
		//Log.e("KPT","checkIfGlideShouldBeEnabled : is alphabet mode " + isAlpabetMode);
		//Log.e("KPT","checkIfGlideShouldBeEnabled : is qwerty " + keyboardLayout.equals(KEYBOARD_TYPE_QWERTY));
		//Log.e("KPT","checkIfGlideShouldBeEnabled : is isHideCV on " + isHideCV);
		//Log.e("KPT","checkIfGlideShouldBeEnabled : is enabled in settings " + enabledInSettings);
		//Log.e("KPT","checkIfGlideShouldBeEnabled : is password field " + mPasswordText);*/

		if (enabledInSettings && isQwertyLayout  && canGlideInThisEditor) {
			if(mInputView != null){
				
				mInputView.setGesturePreviewMode(true, false);
			}

			canGlide = true;
		}else{
			if(mInputView != null){
				
				mInputView.setGesturePreviewMode(false, false);
			}
			canGlide = false;
		}

		PointerTracker.setGestureHandlingEnabledByUser(enabledInSettings);
		PointerTracker.setGestureHandlingEnabledByInputField(isQwertyLayout && canGlideInThisEditor);

		PointerTracker.setMainDictionaryAvailability(false);
		if(mLanguageSwitcher != null) {
			int installedDictCount = mLanguageSwitcher.getLocaleCount();
			PointerTracker.setInstalledDictionariesCount(installedDictCount);
			if(installedDictCount > 0) {
				PointerTracker.setMainDictionaryAvailability(true);
			}
		}

		/*if(enabledInSettings 
				&& mInputView.getKeyboard().isGlideSupportedForCurrentKeyboard() 
				&& keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY)
				&& enableGlide
				&& !mPasswordText) {
			setGlideVisibility(true);
			return true;
		} else {
			setGlideVisibility(false);
			return false;
		}*/

		return canGlide;	
	}

	/**
	 * update core the layout information
	 */
	private void updateLayoutToCore() {
		String keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);
		/*if(mContext.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
			keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}else{
			keyboardLayout = mSharedPreference.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}*/

		boolean enabledInSettings = mSharedPreference.getBoolean(KPTConstants.PREF_GLIDE, true);
		//send layout to core ONLY if keypad is qwerty type 
		if(mCoreEngine != null && mInputView != null 
				&& enabledInSettings
				&& keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY)
				&& mInputView.getKeyboard().isGlideSupportedForCurrentKeyboard()){
			try {
				mCoreEngine.setLayoutToCore(mInputView.getKeyboard());
			} catch(Exception e) {
				e.printStackTrace();				
			}
		}
	}


	public MainKeyboardView getKeyboardView() {
		return mInputView;
	}

	/**
	 * Check if any text is already present in the Edit text
	 * If present, commit the text to core and update suggestions
	 */
	public void checkTextinEditor(){
		if(mEditText != null && !mEditText.hasFocus()){
			return ;
		}
		checkTextinEditorNew();
		/*
		int textLength = mEditText.getText().length();

		if(textLength > 0){
			////Log.e("VMC", "IME EDIT TEXT CURSOR POSITION START ----> "+mEditText.getSelectionStart());
			////Log.e("VMC", "IME EDIT TEXT CURSOR POSITION END   ----> "+mEditText.getSelectionStart());
			if(mCoreEngine != null){
				mCoreEngine.syncCoreBuffer(mEditText.getText().toString(), mEditText.getText().length());

				if(mEditText.getSelectionStart() == mEditText.getText().length()){
					isCursorJump = false;
					cursorJumpPosition = mEditText.getSelectionStart();
					mCoreEngine.setAbsoluteCaretPosition(cursorJumpPosition);
				}else{
					isCursorJump = true;
					cursorJumpPosition = mEditText.getSelectionStart();
					mCoreEngine.setAbsoluteCaretPosition(cursorJumpPosition);
					if(mIsHindhi){
						PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
						PointerTracker.mCurrentSyllableIndex = getSyllableIndex();
						changeKeyboard();
					}

				}

				updateSuggestions(true);
				//Log.e("KPT", "mIsHindhi "+mIsHindhi);
				
			}
		}else{
			if(mCoreEngine != null //TP:#20267
					 	&& mCoreEngine.getCoreBuffer() != null && mCoreEngine.getCoreBuffer().length() == 1){
				mCoreEngine.resetCoreString();
			}
			isCursorJump = false;
			cursorJumpPosition = -1;
			updateShiftKeyState(mEditText);
			updateSuggestions(true);
		}
	*/}

	public void checkTextinEditorNew(){
		
		if (mCoreEngine == null) {
			Log.e("KPT", "Strange mCoreEngine checkTextinEditorNew is null");
			return;
		}

		if (mEditText == null) {
			Log.e("KPT", "Strange mEditText checkTextinEditorNew is null");
			return;
		}
		
	//	Log.e(TAG, "mEditText in checkdittext in editor " + mEditText.getId());
		setGlideSuggestionToNull();
		setCurrentInputTypeToNormal();
		setPreviousInputTypeToNormal();

		int textLength = mEditText.getText().length();

		if(textLength > 0){
			//Log.e("VMC", "IME EDIT TEXT CURSOR POSITION START ----> "+mEditText.getSelectionStart());
			//Log.e("VMC", "IME EDIT TEXT CURSOR POSITION END   ----> "+mEditText.getSelectionStart());
			if(mCoreEngine != null){
				mCoreEngine.syncCoreBuffer(mEditText.getText().toString(), mEditText.getText().length());
				mTextInBuffer.setLength(0);
				mTextInBuffer.append(mEditText.getText().toString());
				if(mEditText.getSelectionEnd() == mEditText.getText().length()){
					mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
							mAutoCorrectOn, mCorrectionMode,mSpaceCorrectionSuggestionsOn);
					isCursorJump = false;
					cursorJumpPosition = mEditText.getSelectionEnd();
					mCoreEngine.setAbsoluteCaretPosition(cursorJumpPosition);
					mCurrCursorPos = cursorJumpPosition;
				}else{
					isCursorJump = true;

					mCoreEngine.setErrorCorrection(mErrorCorrectionOn,
							false, mCorrectionMode,mSpaceCorrectionSuggestionsOn);

					cursorJumpPosition = mEditText.getSelectionEnd();
					mCoreEngine.setAbsoluteCaretPosition(cursorJumpPosition);
					mCurrCursorPos = cursorJumpPosition;
					if(mIsHindhi){
						PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
						PointerTracker.mCurrentSyllableIndex = getSyllableIndex();
						changeKeyboard();
					}
				}
				
				boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn;
				
				String currWord = null;
				boolean suggOffsetChanged = false;
				
				if(!atxOn){
					CurrentWord currentWord = mCoreEngine.getCurrentWord();
					if (currentWord != null && currentWord.currWord != null && currentWord.currWord[0] != null ) {
						currWord = currentWord.currWord[0];
						suggOffsetChanged = currentWord.suggestionOffsetChanged;
					}
				}else{
					currWord = mCoreEngine.getPredictiveWord();
				}
				
			
				if(currWord != null && currWord.length() > 0){
					if(suggOffsetChanged){
						// in the use case where "12,ab" is in editor & "ab" alone is in composing state, 
						// when we backspace both "ab", the core engine straight away starts returning a new composing word("12," in this case).
						// so we need to rely on "suggestionOffset" to commit & delete the previous character and 
						// start the composing state on new one.
						finishComposingText();
						deleteSurroundingText(1, 0);
						if(mIsKeyboardTypeQwerty || mAtxStateOn){
							setPreExistingTextToComposingMode(mInputConnection);
						}
					}else{
						//Log.e("KPT", "checkTextinEditor ");
						setComposingText(currWord, 1);
					}
				}
//				updateSuggestions(true);
				updateShiftKeyState(mEditText);
				
				if(!isHideCV){
					updateEditorText(true, true);
				}
			}
		}else{
			if(mCoreEngine != null && mCoreEngine.getCoreBuffer() != null && mCoreEngine.getCoreBuffer().length() == 1){
				mCoreEngine.resetCoreString();
				updateSuggestions(true);
			}
			isCursorJump = false;
			cursorJumpPosition = -1;
			updateShiftKeyState(mEditText);
			updateSuggestions(true);
		}
	
	}
	public EditText getEditor(){
		return mEditText;
	}

	public void setCursorPosition(int position){
		////Log.e("VMC", "IME --- SET CURSOR POSITION ----> "+position);

		if(mEditText.getSelectionStart() == mEditText.getText().length()){
			isCursorJump = false;
		}else{
			isCursorJump = true;
		}

		cursorJumpPosition  = position;

		mCoreEngine.learnBuffer(mEditText.getText().toString());
		mCoreEngine.setAbsoluteCaretPosition(cursorJumpPosition);
		updateSuggestions(true);
		//Log.e("KPT", "mIsHindhi "+mIsHindhi);
		if(mIsHindhi){
			PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
			PointerTracker.mCurrentSyllableIndex = getSyllableIndex();
			changeKeyboard();
		}
	}

	@Override
	public void onTextLinkClick(View textView, String clickedString) {
		// TODO Auto-generated method stub

	}



	public void setCustomHandler(CustomKeyboard customKeyboard) {
		// TODO Auto-generated method stub
		this.mCustomKeyboard = customKeyboard;

	}

	public void clearCore(){

		if(mCoreEngine != null && mInputView != null && mInputView.getFocusedEditText() != null ){
			mInputView.getFocusedEditText().setText("");
			mCoreEngine.resetCoreString();
			resetTextBuffer();
			setCurrentInputTypeToNormal();
			
			if((mIsKeyboardTypeQwerty && mKeyboardSwitcher.mIsSymbols)
				|| (!mIsKeyboardTypeQwerty 
						&& (mKeyboardSwitcher.getPhoneKeypadState()!= KPTKeyboardSwitcher.MODE_PHONE_ALPHABET))){
				changeKeyboardMode();
			}
			
			//updateShiftStateToCore();
			updateShiftKeyState(mInputView.getFocusedEditText());
			updateSuggestions(true);
		}
	}
	
	
	public ImeCutCopyPasteInterface getCutCopyPasteInterface(){
		return mCutCopyPasteInterface ;
	}
	
	public class IMECutCopyPasteImplementation implements ImeCutCopyPasteInterface{

		

		@Override
		public void processImeCopy() {
			
			if (null == mCoreEngine) {
				Log.e("KPT", " Strange mCoreEngine is null at processImeCopy");
				return;
			}
			
			if (null == mEditText) {
				Log.e("KPT", " Strange edit text copy edittext is null");
				return;
			}
			
			if (null == mKeyboardSwitcher) {
				Log.e("KPT", " Strange edit text copy mKeyboardSwitcher is null");
				return;
			}
			//JET update the cursor position
			mCurrCursorPos = mEditText.getSelectionEnd();
			mCoreEngine.setAbsoluteCaretPosition(mCurrCursorPos);
			mUserSelectedCompleteText = false;
			if(!isHideCV){
				updateEditorText(true, true);
			}
		}

		@Override
		public void processImePaste() {

			if (null == mCoreEngine) {
				Log.e("KPT", " Strange mCoreEngine is null at processImePaste");
				return;
			}
			
			if (null == mEditText) {
				Log.e("KPT", " Strange edit text paste edittext is null");
				return;
			}
			if (null == mKeyboardSwitcher) {
				Log.e("KPT", " Strange edit text paste mKeyboardSwitcher is null");
				return;
			}
			
			boolean atxOn = false;
			
			atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
					&& mKeyboardSwitcher.getPhoneKeypadState() == 
					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
			
			String stringInEditor = mEditText.getText().toString();
			int cursorPositionInEditor = mEditText.getSelectionEnd();
			
			mCoreEngine.resetCoreString();
			mCoreEngine.insertText(stringInEditor,atxOn,false);
			mCoreEngine.setAbsoluteCaretPosition(cursorPositionInEditor);
			
			syncBufferText(stringInEditor, cursorPositionInEditor);
//			updateSuggestions(true);
//			Log.e("KPT", " paste cursorPositionInEditor "+cursorPositionInEditor);
			mUserSelectedCompleteText = false;
		}

		@Override
		public void processImeCut() {
			
			if (null == mCoreEngine) {
				Log.e("KPT", " Strange mCoreEngine is null at processImeCut");
				return;
			}
			
			if (null == mEditText) {
				Log.e("KPT", " Strange edit text cut edittext is null");
				return;
			}
			
			if (null == mKeyboardSwitcher) {
				Log.e("KPT", " Strange edit text cut mKeyboardSwitcher is null");
				return;
			}
			
			boolean atxOn = false;
			
			atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn
					&& mKeyboardSwitcher.getPhoneKeypadState() == 
					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET;
			
			String stringInEditor = mEditText.getText().toString();
			int cursorPositionInEditor = mEditText.getSelectionEnd();
			
			
			mCoreEngine.resetCoreString();
			mCoreEngine.insertText(stringInEditor,atxOn,false);
			mCoreEngine.setAbsoluteCaretPosition(cursorPositionInEditor);
			
			syncBufferText(stringInEditor, cursorPositionInEditor);
//			updateSuggestions(true);
			mUserSelectedCompleteText = false;
		}

		@Override
		public void processImeSelectAll() {

			mUserSelectedCompleteText = true;
		}

		@Override
		public void processImeTextChange() {
			//Log.e("KPT", "prcoessImeTextChange mAdaptxtTextChange ");
			//this case was when the activity orientation changed 
			// client was getting oncreate call and creating object of the keyboardViewHolder
			if (null == keyboardViewHolder) {
				return;
			}
			
			if(keyboardViewHolder.getVisibility() != View.VISIBLE){
				return;
			}

			if (null == mCoreEngine) {
				Log.e("KPT", " Strange mCoreEngine is null at processImeCopy");
				return;
			}
			
			if (null == mEditText) {
				Log.e("KPT", " Strange edit text copy edittext is null");
				return;
			}
			
			if (null == mKeyboardSwitcher) {
				Log.e("KPT", " Strange edit text copy mKeyboardSwitcher is null");
				return;
			}
			String stringInEditor = mEditText.getText().toString();
		if(stringInEditor.compareTo(mTextInBuffer.toString()) != 0) {
				//Log.e(TAG, mEditText.getSelectionEnd() + " syncing hereeeeeee---> "+ stringInEditor );
				mTextInBuffer.setLength(0);
				mTextInBuffer.replace(0, mTextInBuffer.length(), stringInEditor);
				mCurrCursorPos = mEditText.getSelectionEnd();
				mCoreEngine.syncCoreBuffer(stringInEditor,mCurrCursorPos);
				mCoreEngine.setAbsoluteCaretPosition(mCurrCursorPos);
				if (TextUtils.isEmpty(mTextInBuffer)) {
					updateShiftKeyState(mEditText);
				}
				if(!isHideCV){
					updateSuggestions(true);
				}
			
			}else if(mCurrCursorPos != mEditText.getSelectionEnd()){
				mCurrCursorPos = mEditText.getSelectionEnd();
				mCoreEngine.setAbsoluteCaretPosition(mCurrCursorPos);
			}
			
			mUserSelectedCompleteText = false;
		}
 
		@Override
		public void onSelectionChanged(int selStart, int selEnd) {
		
			//this case was when the activity orientation changed 
			// client was getting oncreate call and creating object of the keyboardViewHolder
			if (null == keyboardViewHolder) {
				return;
			}
			
			if(keyboardViewHolder.getVisibility() != View.VISIBLE){
				return;
			}
			
			if (null == mCoreEngine) {
				//Log.e("KPT", " Strange mCoreEngine is null at processImeCopy");
				return;
			}
			
			if (null == mEditText) {
				//Log.e("KPT", " Strange edit text copy edittext is null");
				return;
			}
			
			if(selStart == selEnd){
				if(mCurrCursorPos != selEnd){
					//Log.e(TAG, selEnd+" selection changed "+ mCurrCursorPos);
					mCurrCursorPos = selEnd;
					mCoreEngine.setAbsoluteCaretPosition(mCurrCursorPos);
					/*if(mTextInBuffer.toString().compareTo(mEditText.getText().toString()) != 0){
						updateEditorText(true,true);
					}else if(!isHideCV){
						updateInlining();
					}*/
				}
			}

		}
	}

	public void updateCurPosition() {
		if (mEditText != null) {
			mCurrCursorPos = mEditText.getSelectionEnd();
			mEditText.setUserSelectedPSomeText(false);
			mUserSelectedCompleteText = false;
		}
	}

	private void updateEditorText(boolean isUpdateSuggs, boolean isUpdateInlining) {
		if (mEditText == null){
			return;
		}
		////mEditText.setText(null,true);
		
		if (mCoreEngine == null){
			return;
		}
		if (isUpdateInlining) {
			updateInlining();
		}else{
			mEditText.setText(mTextInBuffer.toString(),true);
			if(mTextInBuffer.length() < mCurrCursorPos){
				mCurrCursorPos = mTextInBuffer.length() ;
			}
			mEditText.setSelection(mCurrCursorPos,true);
		
		}
		
		if (isUpdateSuggs) {
			updateSuggestions(true);
		}
	}
	
	private void updateInlining() {
		
		if (mEditText == null){
			Log.e("KPT", " updateInlining mEditText null return");
			return;
		}
		
		if(mCoreEngine == null){
			Log.e("KPT", " updateInlining core engine null return");
			return;
		}
		
		boolean atxOn = !mIsKeyboardTypeQwerty && mAtxStateOn;
		String currWord = null;
		boolean suggOffsetChanged = false;
		if(mEditText.getUserSelectedPSomeText()){
			Log.e("KPT", " getUserSelectedPSomeText core engine null return");
		}
				
		if(!atxOn){
			CurrentWord currentWord = mCoreEngine.getCurrentWord();
			if (currentWord != null && currentWord.currWord != null && currentWord.currWord[0] != null ) {
				currWord = currentWord.currWord[0];
				suggOffsetChanged = currentWord.suggestionOffsetChanged;
			}
		}else{
			currWord = mCoreEngine.getPredictiveWord();
		}
		
		if(currWord != null && currWord.length() > 0){
			int curWordLen = currWord.length();
		//	Log.e(TAG,"editText Focused "+ mEditText.isFocused());
			if (mCurrCursorPos >= curWordLen && mTextInBuffer.length() >= mCurrCursorPos) {
				mComposingMode = true;
				if(mEditText.getText().toString().compareTo(mTextInBuffer.toString()) == 0 ){
					processUnderlineEditext(mCurrCursorPos - curWordLen, mCurrCursorPos);
				}else{
					SpannableString spannableString = processUnderline(mCurrCursorPos - curWordLen, mCurrCursorPos, mTextInBuffer.toString());
					mEditText.setText(spannableString,true);
				}
				
				if(mEditText.getSelectionEnd() != mCurrCursorPos){
					mEditText.setSelection(mCurrCursorPos,true);
				}
			}else{
				if(mEditText.getText().toString().compareTo(mTextInBuffer.toString()) == 0 ){
					//processUnderlineEditext(mCurrCursorPos - curWordLen, mCurrCursorPos);
				}else{
					mEditText.setText(mTextInBuffer.toString(),true);
				}
				
				if(mTextInBuffer.length() < mCurrCursorPos){
					mCurrCursorPos = mTextInBuffer.length() ;
				}
				if(mEditText.getSelectionEnd() != mCurrCursorPos){
					mEditText.setSelection(mCurrCursorPos,true);
				}
			}
		}else{
			 
			if(mEditText.getText().toString().compareTo(mTextInBuffer.toString()) == 0 ){
				//processUnderlineEditext(mCurrCursorPos - curWordLen, mCurrCursorPos);
			}else{
				mEditText.setText(mTextInBuffer.toString(),true);
			}
			if(mTextInBuffer.length() < mCurrCursorPos){
				mCurrCursorPos = mTextInBuffer.length() ;
			}
			if(mEditText.getSelectionEnd() != mCurrCursorPos){
				mEditText.setSelection(mCurrCursorPos,true);
			}
		}
	}

	private void removeInlinig(){
		if(mEditText == null){
			return;
		}
		
		int currCursorPos = mEditText.getSelectionEnd();
		Log.e(TAG, " currCursorPos " +currCursorPos);
		if(currCursorPos == 0){
			return;
		}
		UnderlineSpan[] toRemoveSpans = mEditText.getText().getSpans(0, mEditText.getText().length(), UnderlineSpan.class);
		if(toRemoveSpans != null){

			for (int i = 0; i < toRemoveSpans.length; i++) {
				mEditText.getText().removeSpan(toRemoveSpans[i]);
			}
		}
		
		
	}


	public void onPause() {
		if(mCoreEngine != null){
			mCoreEngine.saveUserContext();
			
		}
		if(mOptionsDialog !=null && mOptionsDialog.isShowing()){
					mOptionsDialog.dismiss();
				}
		
	}

	
}
