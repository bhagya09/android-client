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

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.view.inputmethod.EditorInfo;

import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;

public class KPTAdpatxtKeyboard extends Keyboard {

    private static final String TAG = "Adaptxt Keyboard";
    // Minimum width of space key preview (proportional to keyboard width)
    static final float SPACEBAR_POPUP_MIN_RATIO = 0.4f;
    // defining special key for Xi key
    private Key mXiKey;
    private Key mShiftKey;
    private Key mEnterKey;
    private Key mF1Key;
    private Key m123Key;
    //private Key mDelKeyIcon;
    private int mSpaceKeyIndex = -1;
    private int mXiKeyIndex = 1;
    private Locale mLocale;
    private KPTLanguageSwitcher mLanguageSwitcher;
    private Context mContext;
    // Whether this keyboard has voice icon on it
    private boolean mHasVoiceButton;
    // Whether voice icon is enabled at all
    private boolean mVoiceEnabled;
    private boolean mIsAlphaKeyboard;
    private CharSequence m123Label;
    private int mExtensionResId; 
    public static boolean mSpaceSwypeStart1=false;
    public static final int SHIFT_OFF = 0;
    public static final int SHIFT_ON = 1;
    public static final int SHIFT_LOCKED = 2;
    
    private int mShiftState = SHIFT_OFF;

    public static final float SPACEBAR_DRAG_THRESHOLD = 0.2f;
    
    static int sSpacebarVerticalCorrection;
    

    public KPTAdpatxtKeyboard(Context context, int xmlLayoutResId, boolean isPopupKeyboard, boolean fromActivity, KPTAdaptxtTheme theme) {
        this(context, xmlLayoutResId, 0, isPopupKeyboard, fromActivity, theme);

        //mButtonArrowLeftIcon = mTheme.getResources().getDrawable(mTheme.mKeyboardLanguageArrowsLeft);
        //mButtonArrowRightIcon = mTheme.getResources().getDrawable(mTheme.mkeyboardLanguageArrowsRight);
       // mSpace_line_Icon = res.getDrawable(mTheme.mKeyboardSpace);
    }
    
    public KPTAdpatxtKeyboard(Context context, int xmlLayoutResId, int mode, boolean isPopupKeyboard, boolean fromActivity, KPTAdaptxtTheme theme) {
        super(context, xmlLayoutResId, mode, isPopupKeyboard, 0, fromActivity);
        final Resources res = context.getResources();
        mContext = context;
   // 	if(!KPTAdaptxtKeyboardView.mIsExtension)
    //	{
    		sSpacebarVerticalCorrection = res.getDimensionPixelOffset(
    				R.dimen.kpt_spacebar_vertical_correction_dk_fk);
    		mIsAlphaKeyboard = xmlLayoutResId == R.xml.kbd_qwerty_fk;	
    		mSpaceKeyIndex = indexOf((int) ' ');
    		mXiKeyIndex = indexOf(KPTConstants.KEYCODE_XI);
    	//}   
    }

    public KPTAdpatxtKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding, boolean isPopupKeyboard, boolean fromActivity) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding, isPopupKeyboard, 0, fromActivity);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser, int rowNumber, int keyIndexInRow) {
        Key key = new AdaptxtKey(res, parent, x, y, parser, rowNumber, keyIndexInRow);
        switch (key.codes[0]) {
        case KPTConstants.KEYCODE_XI:
        	mXiKey = key;
        	break;
        case KPTConstants.KEYCODE_ENTER:
            mEnterKey = key;
            break;
        case KPTConstants.KEYCODE_F1:
            mF1Key = key;
            break;
        }
        return key;
    }
    

    void setImeOptions(Resources res, int mode, int options) {
    	if (mEnterKey != null) {
    		Resources orig = mContext.getResources();
    		Configuration conf = orig.getConfiguration();
    		Locale saveLocale = conf.locale;
    		conf.locale = mLanguageSwitcher.getInputLocale();
    		orig.updateConfiguration(conf, null);

    		// Reset some of the rarely used attributes.
    		mEnterKey.popupCharacters = null;
    		mEnterKey.popupResId = 0;
    		mEnterKey.text = null;
    		
    		//Log.e(TAG, "IME ACTION" + (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)));
    		switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
    		case EditorInfo.IME_ACTION_GO:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_go);
    			mEnterKey.label = null;
    			break;
    		case EditorInfo.IME_ACTION_NEXT:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_next);
    			mEnterKey.label = null;
    			break;
    		case EditorInfo.IME_ACTION_DONE:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_done);
    			mEnterKey.label = null;
    			break;
    		case EditorInfo.IME_ACTION_SEARCH:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_search);
    			mEnterKey.label = null;
    			break;
    		case EditorInfo.IME_ACTION_SEND:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_send);
    			mEnterKey.label = null;
    			break;
    		default:
    			mEnterKey.iconText = mContext.getResources().getString(R.string.kpt_icontext_enter_default);
    			mEnterKey.label = null;
    			break;
    		}

    		conf.locale = saveLocale;
    		orig.updateConfiguration(conf, null);
    		// Set the initial size of the preview icon
    		if (mEnterKey.iconPreview != null) {
    			mEnterKey.iconPreview.setBounds(0, 0, 
    					mEnterKey.iconPreview.getIntrinsicWidth(),
    					mEnterKey.iconPreview.getIntrinsicHeight());
    		}
    	}
    }
      
    void enableShiftLock() {
        int index = getShiftKeyIndex();
        if (index >= 0 && getKeys().size() > index) {
            mShiftKey = getKeys().get(index);
            if (mShiftKey != null && mShiftKey instanceof AdaptxtKey) {
                ((AdaptxtKey)mShiftKey).enableShiftLock();
            }
        }
    }

    void setShiftLocked(boolean shiftLocked) {
    	if (mShiftKey != null) {
    		if (shiftLocked) {
    			mShiftKey.on = true;
    			mShiftState = SHIFT_LOCKED;
    		} else {
    			mShiftKey.on = false;
    			mShiftState = SHIFT_ON;
    		}
    	}
    }

    public int getShiftState() {
    	return mShiftState;
    }
    
    boolean isShiftLocked() {
        return mShiftState == SHIFT_LOCKED;
    }
    
    @Override
    public boolean setShifted(boolean shiftState) {
    	boolean shiftChanged = false;
    	String inputLanguageLocale = mLanguageSwitcher.getInputLanguageLocale();
    	// condition to avoid shift key state for the following languages as they dont have uppercase and lower case letters.
    	if(mShiftKey != null &&  
    			(inputLanguageLocale.equals("ar-EG") || 
    			inputLanguageLocale.equals("fa-IR") ||
    			inputLanguageLocale.equals("he-IL") ||
    			inputLanguageLocale.equals("hi-IN") ||
    			inputLanguageLocale.equals("mr-IN") ||
    			inputLanguageLocale.equals("ur-PK") ||
    			inputLanguageLocale.equals("bn-IN") ||
    			inputLanguageLocale.equals("kn-IN") ||
    			inputLanguageLocale.equals("ml-IN") ||
    			inputLanguageLocale.equals("ta-IN") ||
    			inputLanguageLocale.equals("te-IN"))){
    		shiftChanged = mShiftState == SHIFT_OFF;
    		mShiftKey.on = false;
    		mShiftState = SHIFT_OFF;
    		return true;
    	}

    	if (mShiftKey != null) {
    		if (shiftState == false) {
    			shiftChanged = mShiftState != SHIFT_OFF;
    			mShiftState = SHIFT_OFF;
    			mShiftKey.on = false;
    		} else {
    			if (mShiftState == SHIFT_OFF) {
    				shiftChanged = mShiftState == SHIFT_OFF;
    				mShiftState = SHIFT_ON;
    			} else if(mShiftState == SHIFT_LOCKED) {
    				mShiftKey.on = true;
    				mShiftState = SHIFT_LOCKED;
    				shiftChanged = true;
    			}
    		}
    	} else {
    		return super.setShifted(shiftState);
    	}
    	return shiftChanged;
    }

    @Override
    public boolean isShifted() {
        if (mShiftKey != null) {
            return mShiftState != SHIFT_OFF;
        } else {
            return super.isShifted();
        }
    }

    public void setExtension(int resId) {
        mExtensionResId = resId;
    }

    public int getExtension() {
        return mExtensionResId;
    }

    public void setVoiceMode(boolean hasVoiceButton, boolean hasVoice) {
        mHasVoiceButton = hasVoiceButton;
        mVoiceEnabled = hasVoice;
        updateF1Key();
    }

    private void updateF1Key() {
    	if (mF1Key == null) {
    		return;
    	}
    	if (m123Key != null && mIsAlphaKeyboard) {
    		m123Key.icon = null; 
    		m123Key.iconPreview = null;
    		m123Key.label = m123Label;
    	}

    	if (mHasVoiceButton && mVoiceEnabled) {
    		mF1Key.codes = new int[] { KPTConstants.KEYCODE_VOICE };
    		mF1Key.label = null;
    	} else {
    		mF1Key.label = ",";
    		mF1Key.codes = new int[] { ',' };
    		mF1Key.icon = null;
    		mF1Key.iconPreview = null;
    	}
    }
    
    /**
     * Gets if currently space key is highlighted
     * @return Is space highlighted.
     */
    public boolean isSpaceHighlighted() {
    	return mIsSpaceHighlighted;
    }
    
    /**
     * Sets the highlighting flag for space
     * @param isSpaceHighlighted
     */
    public void setSpaceHighlighted(boolean isSpaceHighlighted) {
    	mIsSpaceHighlighted = isSpaceHighlighted;
    }
    
    public void setXiHighlighted(boolean isHighlighted){
    	if(mXiKey != null){
    		mXiKey.on = isHighlighted;
    	}
    }
    
    /**
     * Returns the language switcher reference held by this class.
     * @return Language Switcher reference.
     */
    public KPTLanguageSwitcher getLanguageSwitcher() {
    	return mLanguageSwitcher;
    }

    public void setLanguageSwitcher(KPTLanguageSwitcher switcher) {
        mLanguageSwitcher = switcher;
        Locale locale = mLanguageSwitcher.getLocaleCount() > 0
                ? mLanguageSwitcher.getInputLocale()
                : null;
        if (mLocale != null && mLocale.equals(locale)){
        	return;
        }
        mLocale = locale;
        //updateSpaceBarForLocale();new_spacekey__testing
    }

    /**
     * Gets space key index
     * @return space key index value.
     */
    public int getSpaceKeyIndex() {
    	return mSpaceKeyIndex;
    }
    
    public Key getXiKey() {
    	return mXiKey;
    }
    
    public int getXiKeyIndex() {
    	return mXiKeyIndex;
    }
    
    private int indexOf(int code) {
        List<Key> keys = getKeys();
        int count = keys.size();
        for (int i = 0; i < count; i++) {
            if (keys.get(i).codes[0] == code) {
            	return i;
            }
        }
        return -1;
    }

    class AdaptxtKey extends Key {
        public AdaptxtKey(Resources res, Keyboard.Row parent, int x, int y, 
                XmlResourceParser parser, int rowNumber, int keyIndexInRow) {
            super(res, parent, x, y, parser, rowNumber, keyIndexInRow);
            if (popupCharacters != null && popupCharacters.length() == 0) {
                // If there is a keyboard with no keys specified in popupCharacters
                popupResId = 0;
            }
        }
    }
}
