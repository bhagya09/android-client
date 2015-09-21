/*
 * Copyright (C) 2008 Google Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;


public class KPTKeyboardSwitcher {

	// <------ Qwerty keyboard type values ------>
    public static final int QWERTY_MODE_TEXT = 1;
    public static final int QWERTY_MODE_SYMBOLS = 2;
    public static final int QWERTY_MODE_PHONE = 3;
    public static final int QWERTY_MODE_URL = 4;
    public static final int QWERTY_MODE_EMAIL = 5;
    public static final int QWERTY_MODE_IM = 6;
    public static final int QWERTY_MODE_WEB = 7;
    public static final int QWERTY_MODE_LOOKUP = 9;
	public static final int QWERTY_MODE_SHIFT = 8;
	public static final int QWERTY_MODE_PASSWORD = 10;
    
    public static final int MODE_TEXT_QWERTY = 0;
    public static final int MODE_TEXT_ALPHA = 1;
    public static final int MODE_TEXT_COUNT = 2;
    
    public static final int QWERTY_KEYBOARDMODE_NORMAL = R.id.mode_normal;
    public static final int QWERTY_KEYBOARDMODE_URL = R.id.mode_url;
    public static final int QWERTY_KEYBOARDMODE_EMAIL = R.id.mode_email;
    public static final int QWERTY_KEYBOARDMODE_IM = R.id.mode_im;
    public static final int QWERTY_KEYBOARDMODE_WEB = R.id.mode_webentry;
    public static final int QWERTY_KEYBOARDMODE_PASSWORD = R.id.mode_password;
    
    public static final int QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI = R.id.mode_normal_hide_xi;
    public static final int QWERTY_KEYBOARDMODE_URL_HIDE_XI = R.id.mode_url_hide_xi;
    public static final int QWERTY_KEYBOARDMODE_EMAIL_HIDE_XI = R.id.mode_email_hide_xi;
    public static final int QWERTY_KEYBOARDMODE_IM_HIDE_XI = R.id.mode_im_hide_xi;
    public static final int QWERTY_KEYBOARDMODE_WEB_HIDE_XI = R.id.mode_webentry_hide_xi;
    //public static final int QWERTY_KEYBOARDMODE_PASSWORD_HIDE_XI = R.id.mode_password_hide_xi;
    //public static final int QWERTY_KEYBOARDMODE_LOOKUP = R.id.mode_lookup;
    
    private static final int SYMBOLS_MODE_STATE_NONE = 0;
    private static final int SYMBOLS_MODE_STATE_BEGIN = 1;
    private static final int SYMBOLS_MODE_STATE_SYMBOL = 2;

    // <--------- phone keyboard type values ------->
    public static final int PHONE_MODE_TEXT = 21;
    public static final int PHONE_MODE_SYMBOLS = 22;
    public static final int PHONE_MODE_URL = 23;
    public static final int PHONE_MODE_EMAIL = 24;
    public static final int PHONE_MODE_CHUNJIIN = 25;
    public static final int PHONE_MODE_PASSWORD = 26;
   // public static final int PHONE_MODE_PANTECH = 26;
  //  public static final int PHONE_MODE_NARATGUL = 27;
    
    public static final int PHONE_KEYBOARDMODE_NORMAL = R.id.phone_first_row_normal;
    public static final int PHONE_KEYBOARDMODE_URL = R.id.phone_first_row_email;
    public static final int PHONE_KEYBOARDMODE_EMAIL = R.id.phone_first_row_url;
    public static final int PHONE_KEYBOARDMODE_PASSWORD = R.id.phone_last_row_password;
    
    public static final int MODE_PHONE_ALPHABET = 0;
    public static final int MODE_PHONE_SYMBOLS = 1;
    public static final int MODE_PHONE_SYMBOLS_SHIFTED = 2;
    
    public static final int MODE_QWERTY_ALPHABET = 3;
    public static final int MODE_QWERTY_SYMBOLS = 4;
    public static final int MODE_QWERTY_SYMBOLS_SHIFTED = 5;
    public static final int MODE_QWERTY_NUMERIC = 6;
    public static final int MODE_QWERTY_NUMERIC_SYMBOLS = 7;
    public static final int MODE_QWERTY_KOREAN_SHIFT = 8;
    
    private int mPhoneKeypadState;
    /**
     * Maintaines the current keypad that is being displayed
     */
    private int mCurrentKeypadStateShown;
    
    private boolean mIsCurrentModeQwerty;
    private boolean isPhoneKeypad;
    
    /**
     * This variable is used to switch to English locale when the current 
     * keyboard language is in non latin language and in password field. 
     */
    public boolean mShouldLoadEngLocale;
    
    
    MainKeyboardView mInputView;
	
   /* private static final int[] ALPHABET_MODES = {
    	QWERTY_KEYBOARDMODE_NORMAL,
    	QWERTY_KEYBOARDMODE_URL,
    	QWERTY_KEYBOARDMODE_EMAIL,
    	QWERTY_KEYBOARDMODE_IM,
    	QWERTY_KEYBOARDMODE_WEB,
    	//QWERTY_KEYBOARDMODE_LOOKUP,
    	PHONE_MODE_TEXT,
    	PHONE_MODE_CHUNJIIN,
    	PHONE_MODE_URL,
    	PHONE_MODE_EMAIL};*/

	//LatinIME mContext;
	Context mContext;
	InputMethodService mInputMethodService;

	private KeyboardId mSymbolsId;
	private KeyboardId mSymbolsShiftedId;

	private KeyboardId mCurrentId;
	private Map<KeyboardId, KPTAdpatxtKeyboard> mKeyboards;

	int mMode; /** One of the MODE_XXX values */
	private int mImeOptions;
	private int mTextMode = MODE_TEXT_QWERTY;
	public  boolean mIsSymbols;
	private boolean mHasVoice;
	private boolean mVoiceOnPrimary;
	private boolean mPreferSymbols;
	private int mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;

	private int mLastDisplayWidth;
	private KPTLanguageSwitcher mLanguageSwitcher;
	private Locale mInputLocale;
	public String mABCText;
	public String m123Text;
	public String m123TextForQwerty;
	public String mKoreanText;
	//To know the current theme
	private SharedPreferences pref;

	//public int mThemeMode;
	public static ArrayList<Integer> phoneKeypadModes = new ArrayList<Integer>();
	public static boolean mIsSymbolschanging = false;
	public static String[] mPageSymbols;
	//It holds the current set of symbols layout count from total symbols layouts.
	private int mSymbolsLayoutcount = 0;
	private int mAltState=0;
	private boolean mCapsLock;

	private KPTAdaptxtTheme mTheme;
	private int mCurrentEditorMode;
	
	private boolean hideXiKey = false;

	public KPTKeyboardSwitcher(Context context, InputMethodService ims) {
		mContext = context;
		mKeyboards = new HashMap<KeyboardId, KPTAdpatxtKeyboard>();

		pref=PreferenceManager.getDefaultSharedPreferences(mContext);

		mSymbolsId = new KeyboardId(R.xml.kbd_symbols_fk, false);
		mSymbolsShiftedId = new KeyboardId(R.xml.kbd_symbols_shift_fk, false);

		mInputMethodService = ims;
	}
	//constructor for customization
	public KPTKeyboardSwitcher(Context context) {
		//rCubeActivity = context;
		mKeyboards = new HashMap<KeyboardId, KPTAdpatxtKeyboard>();
		mContext = context;
		pref=PreferenceManager.getDefaultSharedPreferences(mContext);

		mSymbolsId = new KeyboardId(R.xml.kbd_symbols_fk, false);
		mSymbolsShiftedId = new KeyboardId(R.xml.kbd_symbols_shift_fk, false);

		//mInputMethodService = ims;
	}

	/**
	 * Sets the input locale, when there are multiple locales for input.
	 * If no locale switching is required, then the locale should be set to null.
	 * @param locale the current input locale, or null for default locale with no locale 
	 * button.
	 */
	public void setLanguageSwitcher(KPTLanguageSwitcher languageSwitcher) {
		mLanguageSwitcher = languageSwitcher;
		mInputLocale = mLanguageSwitcher.getInputLocale();
		setModeChangeText();
	}

	void setInputView(MainKeyboardView inputView) {
		mInputView = inputView;
	}

	public void makeKeyboards() {
		mKeyboards.clear();

		// Configuration change is coming after the keyboard gets recreated. So don't rely on that.
		// If keyboards have already been made, check if we have a screen width change and 
		// create the keyboard layouts again at the correct orientation
		int displayWidth = 0;
		if(mInputMethodService == null) {
			displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
		} else {
			displayWidth = mInputMethodService.getMaxWidth();
		}

		if (displayWidth == mLastDisplayWidth){
			return;
		}
		mLastDisplayWidth = displayWidth;

		mSymbolsId = new KeyboardId(R.xml.kbd_symbols_fk, mHasVoice && !mVoiceOnPrimary);
		mSymbolsShiftedId = new KeyboardId(R.xml.kbd_symbols_shift_fk,
				mHasVoice && !mVoiceOnPrimary);
	}

	/**
	 * Represents the parameters necessary to construct a new LatinKeyboard,
	 * which also serve as a unique identifier for each keyboard type.
	 */
	private static class KeyboardId {
		public int mXml;
		public int mKeyboardMode; /** A KEYBOARDMODE_XXX value */
		public boolean mEnableShiftLock;
		public boolean mHasVoice;

		public KeyboardId(int xml, int mode, boolean enableShiftLock, boolean hasVoice) {
			this.mXml = xml;
			this.mKeyboardMode = mode;
			this.mEnableShiftLock = enableShiftLock;
			this.mHasVoice = hasVoice;
		}

		public KeyboardId(int xml, boolean hasVoice) {
			this(xml, 0, false, hasVoice);
		}

		public boolean equals(Object other) {
			return other instanceof KeyboardId && equals((KeyboardId) other);
		}

		public boolean equals(KeyboardId other) {
			return other.mXml == this.mXml
					&& other.mKeyboardMode == this.mKeyboardMode
					&& other.mEnableShiftLock == this.mEnableShiftLock;
		}

		public int hashCode() {
			return (mXml + 1) * (mKeyboardMode + 1) * (mEnableShiftLock ? 2 : 1)
					* (mHasVoice ? 4 : 8);
		}
	}

	void setVoiceMode(boolean enableVoice, boolean voiceOnPrimary) {
		if (enableVoice != mHasVoice || voiceOnPrimary != mVoiceOnPrimary) {
			mKeyboards.clear();
		}
		mHasVoice = enableVoice;
		mVoiceOnPrimary = voiceOnPrimary;
		if(mInputView != null){
			setKeyboardMode(mMode, mImeOptions, mHasVoice,
					mIsSymbols, mInputView.isShifted());
		}/*else if(mRcubeInputView != null){
			setKeyboardMode(mMode, mImeOptions, mHasVoice,
					mIsSymbols, mRcubeInputView.isShifted());
		}*/
	}

	boolean hasVoiceButton(boolean isSymbols) {
		return mHasVoice && (isSymbols != mVoiceOnPrimary);
	}

	public void setKeyboardMode(int mode, int imeOptions, boolean enableVoice) {
		mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;
		mPreferSymbols = mode == QWERTY_MODE_SYMBOLS;
		setKeyboardMode(mode == QWERTY_MODE_SYMBOLS ? QWERTY_MODE_TEXT : mode, imeOptions, enableVoice,
				mPreferSymbols, false);
	}

	void setKeyboardMode(int mode, int imeOptions, boolean enableVoice, boolean isSymbols, boolean retainShiftState) {
		if (mInputView != null) {
			mMode = mode;
			mImeOptions = imeOptions;
			if (enableVoice != mHasVoice) {
				setVoiceMode(mHasVoice, mVoiceOnPrimary);
			}
			mIsSymbols = isSymbols;

			KeyboardId id = getKeyboardId(mode, imeOptions, isSymbols);

			KPTAdpatxtKeyboard keyboard = getKeyboard(id, false);
			if (mode == QWERTY_MODE_PHONE) {
				mInputView.setPhoneKeyboard(keyboard);
			} else if(mode == PHONE_MODE_SYMBOLS) {
				mInputView.setPhoneSymbolsKeyboard(keyboard);
			} 
			

			mCurrentId = id;
			mInputView.setKeyboard(keyboard);
			keyboard.setShiftLocked(mCapsLock || keyboard.isShiftLocked());
			keyboard.setShifted(retainShiftState);
			keyboard.setImeOptions(mContext.getResources(), mMode, imeOptions);
		}else{
			return;
		}

	}

	private KPTAdpatxtKeyboard getKeyboard(KeyboardId id, boolean fromActivity) {
		changeBackground();

		// With the introduction of Olympic Theme & its look up key,
		// the following check is required to recreate the keyboard appropriately
		// while switching between themes in 12 key phone layout
		/*if(!mIsCurrentModeQwerty && mKeyboards.containsKey(id)){
			mKeyboards.remove(id);
		}*/

		//if (!mKeyboards.containsKey(id) || mIsSymbolschanging) {
			Resources orig = mContext.getResources();
			Configuration conf = orig.getConfiguration();
			Locale saveLocale = conf.locale;
			
			
			/**
			 * When the current keyboard is in Non Latin language 
			 * and editor is password then load English locale as per the requirement.
			 */
			if(mShouldLoadEngLocale){
				conf.locale = new Locale("en", "US");
			}else{
				conf.locale =  mInputLocale;
			}
			
			orig.updateConfiguration(conf, null);
			KPTAdpatxtKeyboard keyboard = new KPTAdpatxtKeyboard(
					mContext, id.mXml, id.mKeyboardMode, false, fromActivity, mTheme);
			keyboard.setVoiceMode(
					hasVoiceButton(id.mXml == R.xml.kbd_symbols_fk), mHasVoice);


			keyboard.setLanguageSwitcher(mLanguageSwitcher);
			/*if (id.mKeyboardMode == QWERTY_KEYBOARDMODE_NORMAL
					|| id.mKeyboardMode == QWERTY_KEYBOARDMODE_URL
					|| id.mKeyboardMode == QWERTY_KEYBOARDMODE_IM
					|| id.mKeyboardMode == QWERTY_KEYBOARDMODE_EMAIL
					|| id.mKeyboardMode == QWERTY_KEYBOARDMODE_WEB
					) {
				keyboard.setExtension(R.xml.kbd_extension);
			}*/

			if (id.mEnableShiftLock) {
				keyboard.enableShiftLock();
			}
			mKeyboards.put(id, keyboard);

			conf.locale = saveLocale;
			orig.updateConfiguration(conf, null);
		//}
		return mKeyboards.get(id);
	}


	private void changeBackground() {
		if(mInputView != null){
			if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				mInputView.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
			}else{
				mInputView.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
			}
		}
	}

	private KeyboardId getKeyboardIdActivity() {
		return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL, true, true);
	}

	private KeyboardId getKeyboardId(int mode, int imeOptions, boolean isSymbols) {
		boolean hasVoice = hasVoiceButton(isSymbols);

		if (isSymbols) {
			if(mode == PHONE_MODE_SYMBOLS) {
				mPhoneKeypadState = MODE_PHONE_SYMBOLS_SHIFTED;
				mCurrentKeypadStateShown = MODE_PHONE_SYMBOLS_SHIFTED;
				return new KeyboardId(R.xml.kbd_phone_symbols_shifted , hasVoice);
		
    		} else {
    			if(mode == QWERTY_MODE_PHONE) {
    				mCurrentKeypadStateShown = MODE_QWERTY_NUMERIC_SYMBOLS;
    				return new KeyboardId(R.xml.kbd_normal_qwerty_phone_symbols_fk, hasVoice);
    			} else {
    				mCurrentKeypadStateShown = MODE_QWERTY_SYMBOLS;
    				hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
    						|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
    				
    				if(mode == QWERTY_MODE_URL) {
    					if(hideXiKey){
    						return new KeyboardId(R.xml.kbd_symbols_fk, QWERTY_KEYBOARDMODE_URL_HIDE_XI, true, hasVoice);
    					}else{
    						return new KeyboardId(R.xml.kbd_symbols_fk, QWERTY_KEYBOARDMODE_URL, true, hasVoice);
    					}
    					
    				} else {
    					if(hideXiKey){
    						return new KeyboardId(R.xml.kbd_symbols_fk, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
    					}else{
    						return new KeyboardId(R.xml.kbd_symbols_fk, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
    					}
    				}
    			}
    		}
		}

		String keyboardLayout = pref.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);
		pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		/*if(mContext.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
			keyboardLayout = pref.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}else{
			keyboardLayout = pref.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}*/
		mIsCurrentModeQwerty = keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY);

		phoneKeypadModes.clear();
		mIsSymbolschanging = false;

		if(mIsCurrentModeQwerty) {
			return getIDForQwerty(mode, hasVoice);
		} else {
			return getIDForPhone(mode, hasVoice);
		}
	}
	/*private KeyboardId getKoreanIdForQwerty(int mode, boolean hasVoice){
		if(isPhoneMode(mode)) {
			mode = QWERTY_MODE_TEXT;
		}
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, true));

		isPhoneKeypad=false;
		//assign default value as qwerty alphabet
		mCurrentKeypadStateShown = MODE_QWERTY_ALPHABET;
		////Log.e("VMC", "KEY SWITCHER MODE ---------------> "+mode);
		switch (mode) {
		case QWERTY_MODE_SHIFT :
			//return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_MODE_SHIFT, false, false);
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
			}
		case QWERTY_MODE_TEXT:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if (mTextMode == MODE_TEXT_QWERTY) {
					if(hideXiKey){
						return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
					}else{
						return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
					}
				}
			}else{
				if (mTextMode == MODE_TEXT_QWERTY) {
					if(hideXiKey){
						return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
					}else{
						return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
					}
				}
			}
			
			break;
		case QWERTY_MODE_SYMBOLS:
			mCurrentKeypadStateShown = MODE_QWERTY_SYMBOLS;
			return new KeyboardId(R.xml.kbd_symbols_fk,hasVoice);
		case QWERTY_MODE_PHONE:
			isPhoneKeypad = true;
			mCurrentKeypadStateShown = MODE_QWERTY_NUMERIC;
			return new KeyboardId(R.xml.kbd_normal_qwerty_phone_fk, hasVoice);
		case QWERTY_MODE_URL:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_URL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_URL, true, hasVoice);
				}
			}else{
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_URL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_URL, true, hasVoice);
				}
			}
			

		case QWERTY_MODE_EMAIL: 
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_EMAIL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_EMAIL, true, hasVoice);
				}
			}else{
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_EMAIL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_EMAIL, true, hasVoice);
				}
			}
			
		case QWERTY_MODE_IM:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_IM_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_IM, true, hasVoice);
				}
			}else{
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_IM_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_IM, true, hasVoice);
				}
			}
			
		case QWERTY_MODE_WEB:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_WEB_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_WEB, true, hasVoice);
				}
			}else{
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_WEB_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_WEB, true, hasVoice);
				}
			}
			
		case QWERTY_MODE_PASSWORD:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_PASSWORD, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_PASSWORD, true, hasVoice);
			}
			
		default:
			if(KPTAdaptxtIME.mIsKoreanPrimaryLayoutTwo){
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
				}
			}else{
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
				}
			}
			
		}
		return null;
	}*/
	private KeyboardId getIDForQwerty(int mode, boolean hasVoice) {
		if(isPhoneMode(mode)) {
			mode = QWERTY_MODE_TEXT;
		}
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));

		isPhoneKeypad=false;
		//assign default value as qwerty alphabet
		mCurrentKeypadStateShown = MODE_QWERTY_ALPHABET;

		switch (mode) {
		case QWERTY_MODE_SHIFT :
			//return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_MODE_SHIFT, false, false);
			/*if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_shift, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
			}*/
		case QWERTY_MODE_TEXT:
			if (mTextMode == MODE_TEXT_QWERTY) {
				if(hideXiKey){
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
				}else{
					return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
				}
			}
			break;
		case QWERTY_MODE_SYMBOLS:
			mCurrentKeypadStateShown = MODE_QWERTY_SYMBOLS;
			return new KeyboardId(R.xml.kbd_symbols_fk,hasVoice);
		case QWERTY_MODE_PHONE:
			isPhoneKeypad = true;
			mCurrentKeypadStateShown = MODE_QWERTY_NUMERIC;
			return new KeyboardId(R.xml.kbd_normal_qwerty_phone_fk, hasVoice);
		case QWERTY_MODE_URL:
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_URL_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_URL, true, hasVoice);
			}

		case QWERTY_MODE_EMAIL: 
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_EMAIL_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_EMAIL, true, hasVoice);
			}
		case QWERTY_MODE_IM:
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_IM_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_IM, true, hasVoice);
			}
		case QWERTY_MODE_WEB:
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_WEB_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_WEB, true, hasVoice);
			}
		case QWERTY_MODE_PASSWORD:
			//When the current language is non latin then show the last row with switch locale key else normal row
			if(mLanguageSwitcher != null && !mLanguageSwitcher.isLatinScript()){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_PASSWORD, true, hasVoice);
			} else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, hideXiKey ? QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI : QWERTY_KEYBOARDMODE_NORMAL,
						true, hasVoice);
			}
		default:
			if(hideXiKey){
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI, true, hasVoice);
			}else{
				return new KeyboardId(R.xml.kbd_qwerty_fk, QWERTY_KEYBOARDMODE_NORMAL, true, hasVoice);
			}
		}
		return null;
	}

	private KeyboardId getIDForPhone(int mode, boolean hasVoice) {

		if(!(isPhoneMode(mode) ||  mode == QWERTY_MODE_PHONE)) {
			mode = PHONE_MODE_TEXT;
		}
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
		
		//int firstRowWithXi_Korean = R.id.phone_first_row_normal_xikey;
		//int firstRowWithOutXi_Korean = R.id.phone_first_row_normal_no_xikey;

		int lastRowWithVoiceId;
		int lastRowWithOutVoiceId;
		lastRowWithVoiceId = R.id.phone_last_row_with_voice;
		lastRowWithOutVoiceId = R.id.phone_last_row_no_voice;
		
		int lastRowWithVoiceIdHideXi = R.id.phone_last_row_with_voice_hide_xi;
		int lastRowWithOutVoiceIdHideXi = R.id.phone_last_row_no_voice_hide_xi;
		

		isPhoneKeypad = false;
		mPhoneKeypadState = MODE_PHONE_ALPHABET;
		mCurrentKeypadStateShown = MODE_PHONE_ALPHABET;

		switch (mode) {
		/*case PHONE_MODE_CHUNJIIN :
			if(hasVoice) {
				phoneKeypadModes.add(hideXiKey ? lastRowWithVoiceIdHideXi : lastRowWithVoiceId);
			} else {
				phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
			}
			return new KeyboardId(R.xml.kbd_phone_chunjiin, PHONE_MODE_CHUNJIIN, false, false);*/
		case PHONE_MODE_TEXT:
			if (mTextMode == MODE_TEXT_QWERTY) {
				/*if(AdaptxtIME.bIsKorean){
					//phoneKeypadModes.add(hideXiKey ? firstRowWithOutXi_Korean : firstRowWithXi_Korean);
				}else{*/
					phoneKeypadModes.add(R.id.phone_first_row_normal);
				//}
				if(hasVoice) {
					phoneKeypadModes.add(hideXiKey ? lastRowWithVoiceIdHideXi : lastRowWithVoiceId);
				} else {
					phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
				}
				return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_TEXT, true, hasVoice);
			}
			break;
		case QWERTY_MODE_PHONE:
			isPhoneKeypad = true;
			mCurrentKeypadStateShown = MODE_QWERTY_NUMERIC;
			return new KeyboardId(R.xml.kbd_normal_qwerty_phone_fk, hasVoice);
		case PHONE_MODE_SYMBOLS:
			mPhoneKeypadState = MODE_PHONE_SYMBOLS;
			mCurrentKeypadStateShown = MODE_PHONE_SYMBOLS;
			return new KeyboardId(R.xml.kbd_phone_symbols,hasVoice);
		case PHONE_MODE_URL:
			/*if(KPTAdaptxtIME.bIsKorean){
				//phoneKeypadModes.add(hideXiKey ? firstRowWithOutXi_Korean : firstRowWithXi_Korean);
			}else{*/
				phoneKeypadModes.add(R.id.phone_first_row_url);
			//}
			phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
			return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_URL, true, hasVoice);
		case PHONE_MODE_EMAIL:
			/*if(KPTAdaptxtIME.bIsKorean){
				//phoneKeypadModes.add(hideXiKey ? firstRowWithOutXi_Korean : firstRowWithXi_Korean);
			}else{*/
				phoneKeypadModes.add(R.id.phone_first_row_email);
			//}
			phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
			return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_EMAIL, true, hasVoice);
		case PHONE_MODE_PASSWORD:
			//When the current language is non latin then show the last row with switch locale key else normal row
			if(mLanguageSwitcher != null && !mLanguageSwitcher.isLatinScript()){
				/*if(KPTAdaptxtIME.bIsKorean){
					//phoneKeypadModes.add(hideXiKey ? firstRowWithOutXi_Korean : firstRowWithXi_Korean);
				}else{*/
					phoneKeypadModes.add(R.id.phone_first_row_normal);
				//}
				phoneKeypadModes.add(PHONE_KEYBOARDMODE_PASSWORD);
				return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_PASSWORD , true, hasVoice);
			}else{
				phoneKeypadModes.add(R.id.phone_first_row_normal);
				if(hasVoice) {
					phoneKeypadModes.add(hideXiKey ? lastRowWithVoiceIdHideXi : lastRowWithVoiceId);
				} else {
					phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
				}
				return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_TEXT, true, hasVoice);
			}
		default:
			if (mTextMode == MODE_TEXT_QWERTY) {
				phoneKeypadModes.add(R.id.phone_first_row_normal);
				if(hasVoice) {
					phoneKeypadModes.add(hideXiKey ? lastRowWithVoiceIdHideXi : lastRowWithVoiceId);
				} else {
					phoneKeypadModes.add(hideXiKey ? lastRowWithOutVoiceIdHideXi : lastRowWithOutVoiceId);
				}
				return new KeyboardId(R.xml.kbd_phone, PHONE_MODE_TEXT, true, hasVoice);
			}
			break;
		}
		return null;
	}

	private boolean isPhoneMode(int mode) {
		//QWERTY_MODE_PHONE is same for both phone and qwerty
		return (mode == PHONE_MODE_TEXT || mode == PHONE_MODE_SYMBOLS ||
				mode == PHONE_MODE_EMAIL || mode == PHONE_MODE_CHUNJIIN 
				|| mode == PHONE_MODE_URL || mode == PHONE_MODE_PASSWORD);
	}

	/** 
	 * indicates whether the current keyboard is qwerty numeric(displayed in phone number field) 
	 * @return
	 */
	public boolean isCurrentKeyboardQWERTYPhone() {
		return isPhoneKeypad;
	}

	public int getPhoneKeypadState() {
		return mPhoneKeypadState;
	}

	int getKeyboardMode() {
		return mMode;
	}

	int getImeOptions() {
		return mImeOptions;
	}

	boolean isTextMode() {
		return mMode == QWERTY_MODE_TEXT;
	}

	int getTextMode() {
		return mTextMode;
	}

	void setTextMode(int position) {
		if (position < MODE_TEXT_COUNT && position >= 0) {
			mTextMode = position;
		}
		if (isTextMode()) {
			setKeyboardMode(QWERTY_MODE_TEXT, mImeOptions, mHasVoice);
		}
	}

	int getTextModeCount() {
		return MODE_TEXT_COUNT;
	}

	/**
	 * toggles the keyboard layout between phone and phone_symbols.
	 */
	public void togglePhoneModeChange(int phoneMode) {
		int mode;
		if (mInputView == null) {
			return;
		}
		if (isAlphabetMode()) {
			mode = PHONE_MODE_SYMBOLS;
			setKeyboardMode(mode, mImeOptions, false, false, mInputView.isShifted());
			mSymbolsModeState = SYMBOLS_MODE_STATE_BEGIN;
		}
		else {
			if(phoneMode == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
				mode = PHONE_MODE_EMAIL;
			} else if(phoneMode == EditorInfo.TYPE_TEXT_VARIATION_URI) {
				mode = PHONE_MODE_URL;
			} else {
				mode = PHONE_MODE_TEXT;
			}
			mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;
			setKeyboardMode(mode, mImeOptions, false, false, mInputView.isShifted());
		}
	}

	/**
	 * toggles the keyboard layout between phone_symbols and phone_symbols_shifted.
	 */
	public void handlePhoneSymbolsModeChange() {
		if (mInputView == null) {
			return;
		}
		mSymbolsModeState = SYMBOLS_MODE_STATE_BEGIN;
		mSymbolsLayoutcount = 0;
		if (mIsSymbols) {
			setKeyboardMode(PHONE_MODE_SYMBOLS, mImeOptions, false, false, mInputView.isShifted());
		}
		else {
			mIsSymbolschanging = true;
			applySymbolsLayout();

			setKeyboardMode(PHONE_MODE_SYMBOLS, mImeOptions, false, true, mInputView.isShifted());
			mSymbolsModeState = SYMBOLS_MODE_STATE_BEGIN;
		}
	}

	public  boolean isAlphabetMode() {

		return isCurrentKeypadAlphabet();

		/**
		 * murali - commenting the below code because earlier the last row fields specified in ALPHABET_MODES array 
		 * are applicable to only primary state of qwerty and phone keyboards. symbols and symbols shifted always has 
		 * same last row for all languages. 
		 * 
		 *   But for v2.1 requirement now we have 2 types of last rows (normal and url) for symbols and symbols shifted,
		 *   so the below logic doesnt work. instead now we are maintaing a seperate flag
		 * 
		 */

		/*int currentMode = mCurrentId.mKeyboardMode;
		for (Integer mode : ALPHABET_MODES) {
			if (currentMode == mode) {
				return true;
			}
		}
		return false;*/
	}

	void toggleShift(EditText editorInfo) {
		if (editorInfo == null) {
			return;
		}
		//Log.e("NIR>>","toggleShift(EditText editorInfo)");
		int variation = editorInfo.getInputType() & EditorInfo.TYPE_MASK_VARIATION;
		final boolean flagNoSuggestions = 0 != ( editorInfo.getInputType() 
								& EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		if(flagNoSuggestions && variation == 0 &&  editorInfo.getClass().getPackage().equals(KPTConstants.SEARCH_EDITOR) ){
			variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
		}
		boolean isSearchEditor = editorInfo.getClass().getPackage() != null
				&& KPTConstants.SEARCH_EDITOR.equalsIgnoreCase(editorInfo.getClass().getPackage().toString());
		if(variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL && isSearchEditor) {
			variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
		} 
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));

		int keyboardMode;
		if(variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
			keyboardMode = hideXiKey? QWERTY_KEYBOARDMODE_URL_HIDE_XI : QWERTY_KEYBOARDMODE_URL;
		} else {
			keyboardMode = hideXiKey ? QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI : QWERTY_KEYBOARDMODE_NORMAL;
		}

		mSymbolsId=new KeyboardId(R.xml.kbd_symbols_fk, keyboardMode, true, false);	
		mSymbolsShiftedId = new KeyboardId(R.xml.kbd_symbols_shift_fk, keyboardMode, true, false);

		if (mCurrentId.equals(mSymbolsId)) {
			mCurrentKeypadStateShown = MODE_QWERTY_SYMBOLS_SHIFTED;
			mAltState=2;
			//KPTAdpatxtKeyboard symbolsKeyboard = getKeyboard(mSymbolsId);
			KPTAdpatxtKeyboard symbolsShiftedKeyboard = getKeyboard(mSymbolsShiftedId, false);
			if (symbolsShiftedKeyboard == null) {
				return;
			}
			// symbolsKeyboard.setShifted(true);
			mCurrentId = mSymbolsShiftedId;
			mInputView.setKeyboard(symbolsShiftedKeyboard);
			// symbolsShiftedKeyboard.setShifted(true);
			symbolsShiftedKeyboard.setImeOptions(mContext.getResources(), mMode, mImeOptions);
		} else{
			if (mLanguageSwitcher == null) {
				return;
			}
			String inputLangLocale = mLanguageSwitcher.getInputLanguageLocale();
			if (mCurrentId.equals(mSymbolsShiftedId) 
					&& !KeyboardView.indicLanguages.contains(inputLangLocale)) {
				mCurrentKeypadStateShown = MODE_QWERTY_SYMBOLS;
				mAltState=1;
				KPTAdpatxtKeyboard symbolsKeyboard = getKeyboard(mSymbolsId, false);
				if (symbolsKeyboard == null) {
					return;
				}
				//KPTAdpatxtKeyboard symbolsShiftedKeyboard = getKeyboard(mSymbolsShiftedId);
				// symbolsShiftedKeyboard.setShifted(false);
				mCurrentId = mSymbolsId;
				mInputView.setKeyboard(getKeyboard(mSymbolsId, false));
				// symbolsKeyboard.setShifted(false);
				symbolsKeyboard.setImeOptions(mContext.getResources(), mMode, mImeOptions);
			}
		}
	}

	void jumptoTertiaryKeyboard(){
		int keyboardMode;
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));

		keyboardMode = hideXiKey ? QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI : QWERTY_KEYBOARDMODE_NORMAL;

		mSymbolsId=new KeyboardId(R.xml.kbd_symbols_fk, keyboardMode, true, false);	
		mSymbolsShiftedId = new KeyboardId(R.xml.kbd_symbols_shift_fk, keyboardMode, true, false);

		// set the keyboard to tertiary
		KPTAdpatxtKeyboard symbolsShiftedKeyboard = getKeyboard(mSymbolsShiftedId, false);
		if (symbolsShiftedKeyboard == null) {
			return;
		}
		setKeyboardMode(mMode, mImeOptions, mHasVoice, !mIsSymbols, mInputView.isShifted());
		mAltState=2;
		mSymbolsModeState = SYMBOLS_MODE_STATE_SYMBOL;

		mInputView.setKeyboard(symbolsShiftedKeyboard);	
		symbolsShiftedKeyboard.setImeOptions(mContext.getResources(), mMode, mImeOptions);
		mIsSymbols = true;
		mCurrentId = mSymbolsShiftedId;
	}


	public int getAltState(){
		return mAltState;
	}

	void toggleSymbols(EditText edittext) {
		/*if (editorInfo == null) {
			return;
		}*/
		
		hideXiKey = pref.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)
				|| !(pref.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false));
		
		int variation = edittext.getInputType() & EditorInfo.TYPE_MASK_VARIATION;
		final boolean flagNoSuggestions = 0 != (edittext.getInputType() 
								& EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		if(flagNoSuggestions && variation == 0 &&  edittext.getClass().getPackage().toString().equals(KPTConstants.SEARCH_EDITOR) ){
			variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
		}
		boolean isSearchEditor = edittext.getClass().getPackage() != null
				&& KPTConstants.SEARCH_EDITOR.equalsIgnoreCase(edittext.getClass().getPackage().toString());
		if(variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL && isSearchEditor) {
			variation = EditorInfo.TYPE_TEXT_VARIATION_URI;
		} 

		int mode;
		if(variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
			mode = hideXiKey ? QWERTY_KEYBOARDMODE_URL_HIDE_XI : QWERTY_KEYBOARDMODE_URL;
		} else {
			mode = hideXiKey ? QWERTY_KEYBOARDMODE_NORMAL_HIDE_XI : QWERTY_KEYBOARDMODE_NORMAL;
		}

		mCurrentEditorMode = mode;

		setKeyboardMode(mMode, mImeOptions, mHasVoice, !mIsSymbols, mInputView.isShifted());
		if (mIsSymbols && !mPreferSymbols) {
			mAltState=1;
			mSymbolsModeState = SYMBOLS_MODE_STATE_BEGIN;
		} else {
			mAltState=2;
			mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;
		}
	}

	/**
	 * Updates state machine to figure out when to automatically switch back to alpha mode.
	 * Returns true if the keyboard needs to switch back 
	 */
	boolean onKey(int key) {
		// Switch back to alpha mode if user types one or more non-space/enter characters
		// followed by a space/enter

		switch (mSymbolsModeState) {
		case SYMBOLS_MODE_STATE_BEGIN:
			if (key != KPTConstants.KEYCODE_SPACE && key != KPTConstants.KEYCODE_ENTER && key > 0) {
				mSymbolsModeState = SYMBOLS_MODE_STATE_SYMBOL;
			}
			break;
		case SYMBOLS_MODE_STATE_SYMBOL:
			if (key == KPTConstants.KEYCODE_ENTER || key == KPTConstants.KEYCODE_SPACE) {
				return true;
			}
			break;
		}
		return false;
	}

	/**
	 * Returns current input locale of the keyboard.
	 * @return Current Locale
	 */
	public Locale getCurrentInputLocale() {
		return mInputLocale;
	}

	/**
	 * Used to updating the current count value of symbols
	 * @param isLeft  User clicked on either leftside(true) or rightside(false)
	 *  from center of the key.
	 */
	public  void changeSymbols(boolean isLeft) {

		/*	if (isLeft) {
			// this is not the first set of symbols.
			if (mSymbolsLayoutcount > 0) {
				mSymbolsLayoutcount--;
			} else {
				mSymbolsLayoutcount = 5;
			}
		}
		else{*/
		// this is not the last set of symbols.
		if (mSymbolsLayoutcount < 5) {
			mSymbolsLayoutcount++;
		} else {
			mSymbolsLayoutcount = 0;
		}
		//}
		applySymbolsLayout();
	}

	/**
	 * Based on current set of symbols, applying appropriate symbols in the SIP.
	 */
	private void applySymbolsLayout() {
		mIsSymbolschanging = true;
		
		//TP 11398
		Resources res = mContext.getResources();
		Configuration conf = res.getConfiguration();
		Locale saveLocale = conf.locale;
		conf.locale =  mInputLocale;
		res.updateConfiguration(conf, null);
		
		switch (mSymbolsLayoutcount) {
		case 0:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_1);
			break;
		case 1:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_2);
			break;
		case 2:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_3);
			break;
		case 3:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_4);
			break;
		case 4:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_5);
			break;
		case 5:
			mPageSymbols = res.getStringArray(R.array.kpt_symbols_page_6);
			break;
		}
		
		conf.locale = saveLocale;
		res.updateConfiguration(conf, null);
		
		setKeyboardMode(PHONE_MODE_SYMBOLS, mImeOptions, false, true, mInputView.isShifted());
		mInputView.changeKeyboardBG();
	}

	public boolean isCurrentKeyboardModeQwerty() {
		return mIsCurrentModeQwerty;
	}

	public int getCurrentQwertyKeypadState() {
		return mCurrentKeypadStateShown;
	}

	/**
	 * this will return true ONLY if the current keypad is EITHER QWERTY 
	 * PRIMARY or PHONE PRIMARY
	 * 
	 * @return
	 */
	public boolean isCurrentKeypadAlphabet() {

		/*//Log.e("kpt","Current qwerty state -->"+mCurrentKeypadStateShown);
		//Log.e("kpt","is alphabet -->"+(mCurrentKeypadStateShown == MODE_QWERTY_ALPHABET
				|| mCurrentKeypadStateShown == MODE_PHONE_ALPHABET));*/

		return (mCurrentKeypadStateShown == MODE_QWERTY_ALPHABET
				|| mCurrentKeypadStateShown == MODE_PHONE_ALPHABET);
	}
	/**
	 * Method used to update the Text for localized 12? and ABC of 12 Key Layout 
	 */
	public void setModeChangeText(){
		mSymbolsLayoutcount = 0;
		if(mContext!=null){
			Resources orig = mContext.getResources();
			Configuration conf = orig.getConfiguration();
			Locale saveLocale = conf.locale;
			conf.locale = mInputLocale;
			orig.updateConfiguration(conf, null);
			m123Text = mContext.getResources().getString(R.string.kpt_phone_mode_change_to_symbols);
			m123TextForQwerty  = mContext.getResources().getString(R.string.kpt_label_symbol_key);
			mABCText = mContext.getResources().getString(R.string.kpt_label_alpha_key);
			conf.locale = saveLocale;
			orig.updateConfiguration(conf, null);
		}
	}

	public void setCapsLockState(boolean mCapsLock) {
		this.mCapsLock = mCapsLock;
	}

	public void setTheme(KPTAdaptxtTheme theme) {
		mTheme = theme;
	}

	//this will update all the already created keyboards height
	public void updateAllKeyboardHeights(int customizedKeyHeight) {
		try {
			if(mKeyboards != null) {
				Set<KeyboardId> keyboardKeys = mKeyboards.keySet();
				Iterator<KeyboardId> iterator = keyboardKeys.iterator();
				while (iterator.hasNext()) {
					KeyboardId id = iterator.next();
					KPTAdpatxtKeyboard keyboard = mKeyboards.get(id);
					if(keyboard != null) {
						List<Key> keys = keyboard.getKeys();
						for (Key k : keys) {
							k.height = customizedKeyHeight;
							k.y = customizedKeyHeight * k.rowNumber; 
						}
						keyboard.setHeight(customizedKeyHeight * keyboard.getNumberOfRows());
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	
}