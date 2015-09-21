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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.kpt.adaptxt.beta.keyboard.CollectionUtils;
import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.keyboard.PointerTracker;
import com.kpt.adaptxt.beta.keyboard.PreviewTextCustomView;
import com.kpt.adaptxt.beta.keyboard.StaticInnerHandlerWrapper;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;



/**
 * A view that renders a virtual {@link Keyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 * 
 * @attr ref android.R.styleable#KeyboardView_keyBackground
 * @attr ref android.R.styleable#KeyboardView_keyPreviewLayout
 * @attr ref android.R.styleable#KeyboardView_keyPreviewOffset
 * @attr ref android.R.styleable#KeyboardView_labelTextSize
 * @attr ref android.R.styleable#KeyboardView_keyTextSize
 * @attr ref android.R.styleable#KeyboardView_keyTextColor
 * @attr ref android.R.styleable#KeyboardView_verticalCorrection
 * @attr ref android.R.styleable#KeyboardView_popupLayout
 */
public class KeyboardView extends View implements PointerTracker.DrawingProxy {
	
    public static final int FONT_SIZE_MIN = 0;
	public static final int FONT_SIZE_DEF = 1;
	public static final int FONT_SIZE_MAX = 2;
    
    public static final int NOT_A_KEY = -1;
    protected Keyboard mKeyboard;
    private int mCurrentKeyIndex = NOT_A_KEY;
    private int mKeyTextSize;
    
    private int mKeyTextSize2;
    private int mKeyTextSize3;
    private String mAdaptxtKeyLabel;
   
    // For email and password fields ATX should be disabled.
    public boolean mDisableATX = false;
    
    private float mShadowRadius;
    private int mShadowColor;
    private float mBackgroundDimAmount;
    
    private int mPreviewOffset;
    private int mPreviewHeight;
    private int[] mOffsetInWindow;

    private int mMiniKeyboardOffsetX;
    private int mMiniKeyboardOffsetY;
    
    private Key[] mKeys;

    private Key mSpaceKey;
    
    // To draw the Functionality  keys as per the text given 
   	private final int MIN_TEXT_SIZE = 5;
   	private final int MAX_TEXT_SIZE = 25;
    
    protected int mVerticalCorrection;
    private int mProximityThreshold;

    public boolean mIsFlingEvent = false;
    private boolean mPreviewCentered = false;
    private int mPopupPreviewX;
    private int mPopupPreviewY;
    private int mWindowY;

    private boolean mProximityCorrectOn;
    
    private Paint mPaint;
    private Paint mPaintCustomFont;
    
    private Rect mPadding;
    
    protected int mMoreKeysLayout;
   
    private Key mInvalidatedKey;
    /** The dirty region for single key drawing */
    private final Rect mInvalidatedKeyRect = new Rect();

    // This map caches key label text height in pixel as value and key label text size as map key.
    private static final HashMap<Integer, Float> mTextHeightCache = new HashMap<Integer, Float>();
   
    private static final Rect mTextBounds = new Rect();
   
    private Drawable mKeyBackground;
    
    protected static Key mCurrentkeyPressed;

    protected Key mAtxKey;
   
    protected static final int REPEAT_INTERVAL = 200; // ~20 keys per second
    
   
    protected int LONGPRESS_TIMEOUT;
    
    //private static final int LONGPRESS_TIMEOUT = 5000;
    protected static final int DELETE_LONGPRESS_TIMEOUT = 600;

    private boolean mInMultiTap;
    
    /**
     * To hold whether we need to show the visual feedback in caps or not. 
     * It occurs in Phone layout multi tap Shift ON state.
    */
    private boolean mMultiTapShiftState = false;
    
    private Key mPreviousKey;
    
    protected SharedPreferences mSharedPreference;
    
	private static final String TAG = "KeyboardView";
    
    private boolean mKeyHeightChanged;
    
    private final float SCREEN_DENSITY_DPI;
    public static float SCREEN_DENSITY;
    public static float SCALED_DENSITY;
    
    public static ArrayList<String> indicLanguages = new ArrayList<String>();

    /**
     * for custom key shapes use this while invalidating keys
     */
    private int EXTRA_PADDING_CUSTOM_KEYSHAPE = 0;
    /**
     * For 12 key layout, keys have -1 dp as horizoantal gap in the xmls. So
     * for custom themes use this while invalidatin key 
     */
    private int EXTRA_HORIZONTAL_PADDING_12_KEY = 0;

    private GradientDrawable mCustomKeyShapeDrawable;
	private boolean mShouldCustomKeyShapeDrawn = false;
	
	private float mPrimary_offset;
	private float mSecondary_offset;
	private float mPrimary_popup_offset;
	
	/**
	 * IME's handler for posting keyboard draw updates.
	 */
	public AdaptxtIME mIMEHandler;
	
    static {
    	
    	indicLanguages.add("ar-EG"); 
    	indicLanguages.add("fa-IR");
    	indicLanguages.add("he-IL");
    	indicLanguages.add("hi-IN");
    	indicLanguages.add("mr-IN");
    	indicLanguages.add("ur-PK");
    	indicLanguages.add("bn-IN");
    	indicLanguages.add("kn-IN");
    	indicLanguages.add("ml-IN");
    	indicLanguages.add("ta-IN");
    	indicLanguages.add("te-IN");
    	indicLanguages.add("gu-IN");
    	indicLanguages.add("ne-NP");
    	indicLanguages.add("as-IN"); 
    	indicLanguages.add("pa-IN"); 
    	indicLanguages.add("th-TH");
    	indicLanguages.add("ck-IQ");
    	indicLanguages.add("ku-IQ");
    	
    }

    
    private StringBuilder mPreviewLabel = new StringBuilder(1);

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted. **/
    private boolean mBufferNeedsUpdate;
    /** The dirty region in the keyboard bitmap */
    private Rect mDirtyRect = new Rect();
    /** The keyboard bitmap for faster updates */
    private Bitmap mBuffer;
    /** The canvas for the above mutable keyboard bitmap */
    private Canvas mCanvas;

    protected KPTLanguageSwitcher mLanguageSwitcher;
    protected KPTKeyboardSwitcher mKeyboardSwitcher;
    
    private boolean mIsLongPressed = false;
    
	public KPTAdaptxtTheme mTheme;
    
    public static CharSequence accentedChars;
	
	/** True if the entire keyboard needs to be dimmed. */
	private boolean mNeedsToDimEntireKeyboard;
	
	/**
	 * this flag indicated whether accents should be displayed or not
	 */
	protected boolean mShouldDisplayAccents;
	/**
	 * this is the drawable that will be set to space when space is highlighted
	 */
	private Drawable mSpaceHighlightedDrawable;
	
    int mTransparency = 255;
    
	private final SparseArray<PreviewTextCustomView> mKeyPreviewTexts = CollectionUtils.newSparseArray();
	private final DrawingHandler mDrawingHandler = new DrawingHandler(KeyboardView.this);
	
	// Preview placer view
	private final PreviewPlacerView mPreviewPlacerView;
	private boolean mUpdatePreviewPlaceView = false;
	
	private int mDelayAfterPreview;
	private boolean mShowKeyPreviewPopup = true;
	private final int mKeyPreviewLayoutId;
	
	/**
	 * This is a window view it should be added only once.
	 * due to hide suggestion bar feature we need to remove the view and add once.
	 * But in this case framework doesnt update window view for the very first time
	 * so using counter
	
	 * Caution, dont add it each time coz adding and removing view to root is costly
	 */
	private int counter = 0;
	
	private boolean mIsPopupKeyboardOnScreen = false;

	private int mActionBarHeight;
	private Context mContext;
	private String label;
	private int mWidth;
	private int mHeight;

	
	 
	public static String mLeftChevronIconText;
	public static String mRightChevronIconText;
	
	protected static int Keys_per_page;
	
	protected static boolean IsFirstPage = true;
	
    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.styleable.Kpt_KeyboardView_kpt_keyboardViewStyle);
          
    }
    
    
    public KeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);      
        mContext = context;
        SCREEN_DENSITY = context.getResources().getDisplayMetrics().density;
        SCREEN_DENSITY_DPI = context.getResources().getDisplayMetrics().densityDpi;
        SCALED_DENSITY = getResources().getDisplayMetrics().scaledDensity;
        
        TypedArray a =
            context.obtainStyledAttributes(
                attrs, R.styleable.Kpt_KeyboardView, defStyle, 0);

        /*LayoutInflater inflate =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);*/

        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
        mDelayAfterPreview = context.getResources().getInteger(R.integer.kpt_config_key_preview_linger_timeout);
        mKeyPreviewLayoutId = a.getResourceId(R.styleable.Kpt_KeyboardView_kpt_keyPreviewLayout, 0);
		if (mKeyPreviewLayoutId == 0) {
			mShowKeyPreviewPopup = false;
		}
		
        int keyTextSize = 0;

        int n = a.getIndexCount();
        
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            if (attr == R.styleable.Kpt_KeyboardView_kpt_verticalCorrection) {
				mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_keyPreviewLayout) {
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_keyPreviewOffset) {
				mPreviewOffset = a.getDimensionPixelOffset(attr, 0);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_keyPreviewHeight) {
				mPreviewHeight = a.getDimensionPixelSize(attr, 80);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_keyTextSize) {
				mKeyTextSize = a.getDimensionPixelSize(attr, 18);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_popupLayout) {
				mMoreKeysLayout = a.getResourceId(attr, 0);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_shadowColor) {
				mShadowColor = a.getColor(attr, 0);
			} else if (attr == R.styleable.Kpt_KeyboardView_kpt_shadowRadius) {
				mShadowRadius = a.getFloat(attr, 0f);
			}
        }
        
        TypedArray a2 =
            context.obtainStyledAttributes(
                attrs, R.styleable.Kpt_Atx_KeyboardView, defStyle, 0);

        int n2 = a2.getIndexCount();
        for (int i = 0; i < n2; i++) {
            int attr = a2.getIndex(i);

            if (attr == R.styleable.Kpt_Atx_KeyboardView_kpt_keyTextSize2) {
				mKeyTextSize2 = a2.getDimensionPixelSize(attr, 12);
			} else if (attr == R.styleable.Kpt_Atx_KeyboardView_kpt_keyTextSize3) {
				mKeyTextSize3 = a2.getDimensionPixelSize(attr, 12);
			}
        }
        
        a = getContext().obtainStyledAttributes(
                R.styleable.Kpt_Theme);
        mBackgroundDimAmount = a.getFloat(R.styleable.Kpt_Theme_kpt_backgroundDimAmount, 0.5f);

        mLeftChevronIconText = getResources().getString(R.string.kpt_icontext_space_chevron_left);
    	mRightChevronIconText = getResources().getString(R.string.kpt_icontext_space_chevron_right);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(keyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        mPaintCustomFont = new Paint();
        mPaintCustomFont.setAntiAlias(true);
        mPaintCustomFont.setTextSize(keyTextSize);
        mPaintCustomFont.setTextAlign(Align.CENTER);
        mPaintCustomFont.setAlpha(255);
        
        mPadding = new Rect(0, 0, 0, 0);
        if(mKeyBackground!=null)
        	mKeyBackground.getPadding(mPadding);

        //initGestureDetector();
        
        mCustomKeyShapeDrawable = new GradientDrawable();	
        
        mPreviewPlacerView = new PreviewPlacerView(context, attrs);
    }
    
    public static class DrawingHandler extends StaticInnerHandlerWrapper<KeyboardView> {
		private static final int MSG_DISMISS_KEY_PREVIEW = 0;

		public DrawingHandler(final KeyboardView outerInstance) {
			super(outerInstance);
		}

		@Override
		public void handleMessage(final Message msg) {
			final KeyboardView keyboardView = getOuterInstance();
			if (keyboardView == null) return;
			final PointerTracker tracker = (PointerTracker) msg.obj;
			switch (msg.what) {
			case MSG_DISMISS_KEY_PREVIEW:
				final TextView previewText = keyboardView.mKeyPreviewTexts.get(tracker.mPointerId);
				if (previewText != null) {
					previewText.setVisibility(INVISIBLE);
				}
				break;
			}
		}

		public void dismissKeyPreview(final long delay, final PointerTracker tracker) {
			sendMessageDelayed(obtainMessage(MSG_DISMISS_KEY_PREVIEW, tracker), delay);
		}

		public void cancelDismissKeyPreview(final PointerTracker tracker) {
			removeMessages(MSG_DISMISS_KEY_PREVIEW, tracker);
		}

		private void cancelAllDismissKeyPreviews() {
			removeMessages(MSG_DISMISS_KEY_PREVIEW);
		}

		public void cancelAllMessages() {
			cancelAllDismissKeyPreviews();
		}
	}
    
	public void setTheme(KPTAdaptxtTheme theme) {
		if (theme == null) {
			return;
		}
		mTheme = theme;
		if(mPaintCustomFont != null){
			mPaintCustomFont.setTypeface(mTheme.getCustomFontTypeface());
		}
		changePopupBG();
	}
    
	protected KPTAdaptxtTheme getTheme() {
		return mTheme;
	}
	
	public void changeHindhiKeyBoard(){
	
		
		for (int i = 0; i < mKeys.length; i++) {
			
			Key key = mKeys[i];
			//Log.e("KPT", "key.rowNumber "+key.rowNumber);
			if(key.rowNumber == 0 || (key.rowNumber == 1 &&  key.vowelUnicode !=-1) ) {
				
				if(key.vowelBase!=null && key.vowelBase.length !=0 && key.vowelUnicode!=-1){
					
                	key.label = key.vowelBase[PointerTracker.mCurrentSyllableIndex+1];
                	if(PointerTracker.mCurrentSyllableIndex!=-1){
                		key.codes[0] = key.vowelUnicode;
                	}else{
                		key.codes[0] =(int)key.label.charAt(0);
                	}
                	
                	invalidateKey(key);
                }
				
			}else{
				break;
			}
		}
		
	}
	
    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard the keyboard to display in this view
     */
    public void setKeyboard(Keyboard keyboard) {
    	if (keyboard == null) {
			return;
		}
    	
    	//getting Popup Duration
        LONGPRESS_TIMEOUT=mSharedPreference.getInt(KPTConstants.PREF_KEY_POPUP,KPTConstants.DEFAULT_POPUP);
    	
    	//showPreview(NOT_A_KEY);
        // Remove any pending messages
        removeMessages();
        mKeyboard = keyboard;
        List<Key> keys = mKeyboard.getKeys();
        mKeys = keys.toArray(new Key[keys.size()]);
        
        mPrimary_offset = getResources().getFraction(R.fraction.kpt_primary_char_offset, 100, 100);
        mSecondary_offset = getResources().getFraction(R.fraction.kpt_secondary_char_offset, 100, 100);
        mPrimary_popup_offset = getResources().getFraction(R.fraction.kpt_primary_char_popup_key_offset, 100, 100);
        
        mSpaceKey = mKeyboard.getSpaceKey();
        		
      //  applyAnyKeyboardCustomizationChanges(keyboard, keys);
        
        requestLayout();
        mDirtyRect.set(0, 0, getWidth(), getHeight());
        mBufferNeedsUpdate = true;
        invalidateAllKeys();
        
        //draw atx key
        if(mKeyboardSwitcher != null 
        		&& !mKeyboardSwitcher.isCurrentKeyboardModeQwerty() 
        		&& mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) {
        	//update atx from shared preference
        	boolean state = mSharedPreference.getBoolean(KPTConstants.PREF_PHONE_ATX_STATE, true);
        	if(mDisableATX) {
        		state = false;
        	}
        	handleAtx(state);
        }
        
        mShouldDisplayAccents = mSharedPreference.getBoolean(KPTConstants.PREF_DISPLAY_ACCENTS, true);
        
        final boolean isKeyPopupEnabledInSettings = mSharedPreference.getBoolean(KPTConstants.PREF_POPUP_ON, true); 
        setKeyPreviewPopupEnabled(isKeyPopupEnabledInSettings, mDelayAfterPreview);
        
        if(mKeyboardSwitcher != null 
        		&& !mKeyboardSwitcher.isCurrentKeyboardModeQwerty()) {
        	EXTRA_HORIZONTAL_PADDING_12_KEY = Math.max(1,(int) getResources().getDimension(R.dimen.kpt_custom_theme_horiz_padding_12_key));
        } else {
        	EXTRA_HORIZONTAL_PADDING_12_KEY = 0;
        }
        
        computeProximityThreshold(keyboard);
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        //mAbortKey = true; // Until the next ACTION_DOWN
    }

    /**
     * Returns the current keyboard being displayed by this view.
     * @return the currently attached keyboard
     * @see #setKeyboard(Keyboard)
     */
    public Keyboard getKeyboard() {
        return mKeyboard;
    }
    public void getActionBarPlusStatusBarHeight(int height) {
       this.mActionBarHeight = height;
    }
    
  // Function to draw the functionality keys as per the text given 
    
    public int getExactTextSize(Key key, String label) {
    	if (mKeyboard == null) {
    		return -1;
    	}
    	Paint paint = new Paint();
    	String textToDisplay;
    	textToDisplay = label;

    	float scaledDensity = Math.max(1, getResources().getDisplayMetrics().scaledDensity);
    	int widthRoundOff;
    	/*if(SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_LOW || 
    			SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_MEDIUM) {
    		widthRoundOff = 10 * (int)Math.max(1, SCREEN_DENSITY);
    	} else {*/
    		widthRoundOff = 15 * (int)Math.max(1, SCREEN_DENSITY);
    	//}
    	for (int i = MIN_TEXT_SIZE; i <= MAX_TEXT_SIZE; i++) {
    		if(!mKeyboard.isPopupKeyboard) {
    			paint.setTextSize(i * scaledDensity);
    		}
    		if(paint.measureText(textToDisplay, 0, textToDisplay.length()) >= key.width - widthRoundOff) { //7578
    			return (int) (i * scaledDensity);
    		}
    	}
    	return -1;
    }
    
 
    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView#isShifted()
     */
    public boolean setShifted(boolean shifted) {
        if (mKeyboard != null) {
            if (mKeyboard.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise. If there is
     * no shift key on the keyboard or there is no keyboard attached, it returns false.
     * @see KeyboardView#setShifted(boolean)
     */
    public boolean isShifted() {
        if (mKeyboard != null) {
            return mKeyboard.isShifted();
        }
        return false;
    }

    private void addKeyPreview(final TextView keyPreview) {
		locatePreviewPlacerView();
		mPreviewPlacerView.addView(
				keyPreview, ViewLayoutUtils.newLayoutParam(mPreviewPlacerView, 0, 0));
	}

	private void locatePreviewPlacerView() {
		if (mPreviewPlacerView.getParent() != null) {
			if(mUpdatePreviewPlaceView) {
				ViewParent parent = mPreviewPlacerView.getParent();
				if(parent instanceof ViewParent) {
					((ViewGroup)parent).removeView(mPreviewPlacerView);
				}
				counter ++;
				if(counter == 5) {
					counter = 0;
					mUpdatePreviewPlaceView = false;
				}
			} else {
				return;
			}
		}
		
		final int width = getWidth();
		int height = getHeight();
		
		if (width == 0 || height == 0) {
			// In transient state.
			return;
		}
		final int[] viewOrigin = new int[2];
		getLocationInWindow(viewOrigin);
		
		final View rootView = getRootView();
		
		if (rootView == null) {
			Log.w(TAG, "Cannot find root view");
			return;
		}
		
		final ViewGroup windowContentView = (ViewGroup)rootView.findViewById(android.R.id.content);
				
		// Note: It'd be very weird if we get null by android.R.id.content.
		if (windowContentView == null) {
			Log.w(TAG, "Cannot find android.R.id.content view to add PreviewPlacerView");
		} else {
			windowContentView.addView(mPreviewPlacerView);
			mPreviewPlacerView.setKeyboardViewGeometry(viewOrigin[0], viewOrigin[1] - mActionBarHeight, width, height);
		}
	}
	
	/**
	 * remove preview placer view from parent ie.. rootview and add it again.
	 * fix for 13863 Trace path is not displayed according to the gesture when sbar is hidden 
	 */
	public void updatePreviewPlacerView() {
		mUpdatePreviewPlaceView  = true;
		locatePreviewPlacerView();
	}
	
    public void setPopupOffset(int x, int y) {
        mMiniKeyboardOffsetX = x;
        mMiniKeyboardOffsetY = y;
    }

    /**
     * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include key
     * codes for adjacent keys.  When disabled, only the primary key code will be
     * reported.
     * @param enabled whether or not the proximity correction is enabled
     */
    public void setProximityCorrectionEnabled(boolean enabled) {
        mProximityCorrectOn = enabled;
    }

    /**
     * Returns true if proximity correction is enabled.
     */
    public boolean isProximityCorrectionEnabled() {
        return mProximityCorrectOn;
    }

    private CharSequence adjustCase(CharSequence label) {
    	if (mKeyboard == null) {
    		return label;
    	}
    	/*if (mKeyboard.isShifted() && label != null && label.length() < 3
                && Character.isLowerCase(label.charAt(0))) {
        	Locale currentLocale = mLanguageSwitcher.getInputLocale();

        	String charString = label.toString();
			charString = charString.toUpperCase( mLanguageSwitcher.getInputLocale());

			if(label.length() == 1 && charString.length() > 1) {
				// Just use basic character case conversion since some characters don't have corresponding
				// upper case chars. They return 2 chars instead.Bug 6294.
				label = Character.toString(Character.toUpperCase(label.charAt(0)));
			} else {
				label = label.toString().toUpperCase(currentLocale);
			}
        }
        return label;*/
    	if (mLanguageSwitcher == null) {
    		return label;
    	}
    	
    	boolean isShifted;
    	// To show the Visual feedback in Caps when shift key is "ON" in multi tap mode
    	if (mInMultiTap && !isPopupKeyboardShown()) {
    		if (mKeyboard.isShifted() && !mMultiTapShiftState) {
    			mMultiTapShiftState = true;
    		}

    		if (mPreviousKey!= null && getCurrentKey() != null
    				&& !mKeyboard.isShifted() 
    				&& getCurrentKey().keyIndexInRow != mPreviousKey.keyIndexInRow) {
    			mMultiTapShiftState = false;
    		}
    		isShifted = mMultiTapShiftState;
    	}else{
    		mMultiTapShiftState = false;
    		isShifted = mKeyboard.isShifted();
    	}
    	if (!indicLanguages.contains(mLanguageSwitcher.getInputLanguageLocale())
				&& isShifted 
				&& label != null
    			&& Character.isLowerCase(label.charAt(0))) {
    		
    		Locale currentLocale = mLanguageSwitcher.getInputLocale();

    		String charString = label.toString();
    		if(Character.isLetter(label.charAt(0))){
    			if (mKeyboard.mShiftKey != null && !mKeyboard.mShiftKey.on 
    					&& mKeyboardSwitcher.getPhoneKeypadState() == 
    					KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) {
    				String camelModeString = String.valueOf(label.charAt(0)).
    						toUpperCase(currentLocale);
    				label = camelModeString.concat(charString.substring(1,charString.length()));
    			}
    			else {
    				charString = charString.toUpperCase( mLanguageSwitcher.getInputLocale());
    				if(label.length() == 1 && charString.length() > 1) {
    					// Just use basic character case conversion since some characters don't have corresponding
    					// upper case chars. They return 2 chars instead.Bug 6294.
    					label = Character.toString(Character.toUpperCase(label.charAt(0)));
    				} else {
    					label = label.toString().toUpperCase(currentLocale);
    				}
    			}
    		}
    	}
    	mPreviousKey = getCurrentKey();
    	return label;
    }
    
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
        	//TP #15610. Unnecessary white space is displaying on the right hand side 
        	//of the SIP for few Indic and Arabic languages in landscape orientation.
            int width = getResources().getDisplayMetrics().widthPixels; //mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingLeft();
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec);
            }
            
            int height = mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom();
            //For custm key shape increase the keyboard height by 4 px and move the keys down by 3 px(done in inbufferdraw) 
            if(mShouldCustomKeyShapeDrawn) {
            	height = height + 4; 
            	//this.setPadding(0, 3, 0, 0);
            }
            
            setMeasuredDimension(width, height);
        }
    }

    /**
	 * Enables or disables the key feedback popup. This is a popup that shows a magnified
	 * version of the depressed key. By default the preview is enabled.
	 * @param previewEnabled whether or not to enable the key feedback preview
	 * @param delay the delay after which the preview is dismissed
	 * @see #isKeyPreviewPopupEnabled()
	 */
	public void setKeyPreviewPopupEnabled(final boolean previewEnabled, final int delay) {
		mShowKeyPreviewPopup = previewEnabled;
		mDelayAfterPreview = delay;
	}

	/**
	 * Returns the enabled state of the key feedback preview
	 * @return whether or not the key feedback preview is enabled
	 * @see #setKeyPreviewPopupEnabled(boolean, int)
	 */
	public boolean isKeyPreviewPopupEnabled() {
		return mShowKeyPreviewPopup;
	}
	
    public void setGesturePreviewMode(final boolean drawsGesturePreviewTrail,
			final boolean drawsGestureFloatingPreviewText) {
		mPreviewPlacerView.setGesturePreviewMode(
				drawsGesturePreviewTrail, drawsGestureFloatingPreviewText);
	}
    
    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private void computeProximityThreshold(Keyboard keyboard) {
        if (keyboard == null) {
        	return;
        }
        final Key[] keys = mKeys;
        if (keys == null){
        	return;
        }
        int length = keys.length;
        int dimensionSum = 0;
        for (int i = 0; i < length; i++) {
            Key key = keys[i];
            dimensionSum += Math.min(key.width, key.height) + key.gap;
        }
        if (dimensionSum < 0 || length == 0) {
        	return;
        }
        mProximityThreshold = (int) (dimensionSum * 1.4f / length);
        mProximityThreshold *= mProximityThreshold; // Square it
    }

    @Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// Release the buffer, if any and it will be reallocated on the next
		// draw
		mBuffer = null;
		mWidth = w;
		mHeight = h;
	}

    @Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        //mKeyboard = new Keyboard(getContext(), R.xml.kbd_qwerty_fk, KPTKeyboardSwitcher.QWERTY_KEYBOARDMODE_NORMAL, false, 0, true);
		
		if (mBufferNeedsUpdate || mBuffer == null || mKeyHeightChanged) {
			changeKeyboardBG();
			Locale locale =  mLanguageSwitcher.getInputLocale();
			//Bug Fix : Settings locale is changing when language change. 
			// new Configuration(); is giving null value.
			Resources orig = mContext.getResources();
			Configuration config = orig.getConfiguration();
    		//Configuration config = new Configuration();
    		Locale tempLocale = config.locale;
    		config.locale = locale;
    		mContext.getResources().updateConfiguration(config, null);
    		onBufferDraw();
			config.locale = tempLocale;
			mContext.getResources().updateConfiguration(config, null);
			mBufferNeedsUpdate = false;
		}

		if (canvas != null && mBuffer != null) {
			canvas.drawBitmap(mBuffer, 0, 0, null);
		}
	}
    
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	super.onTouchEvent(event);

    	switch (event.getAction()) {
    	case MotionEvent.ACTION_DOWN:

    		return true;

    	case MotionEvent.ACTION_UP:
    		performClick();
    		return true;

    	}
    	return false;

    }

    @Override
    public boolean performClick() {
    	super.performClick();
    	
    	return true;
    }

	private void onBufferDraw() {
    	
    	//long startTime = System.currentTimeMillis();
    	if (mKeyboard == null || mTheme == null) {
    		return;
    	}
    	/*int width = getWidth();
        int height = getHeight();*/
        
    	int width = mWidth;
    	int height = mHeight;
    	
        if (width == 0 || height == 0) {
            width = 300;
            height = 300;
        }
        
        if (mBuffer == null || mKeyHeightChanged ||  
        		mBuffer.getWidth() != width || mBuffer.getHeight() != height) {
            if (mBuffer != null)
                mBuffer.recycle();
            mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mDirtyRect.union(0, 0, width, height);
            if (mCanvas != null) {
                mCanvas.setBitmap(mBuffer);
            } else {
                mCanvas = new Canvas(mBuffer);
            }
            mKeyHeightChanged = false;
        }

    	final Canvas canvas = mCanvas;
    	canvas.clipRect(mDirtyRect, Op.REPLACE);
    	canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
    	
    	final Paint paint = mPaint;
    	final int kbdPaddingLeft = getPaddingLeft();
    	final int kbdPaddingTop = getPaddingTop();
    	
    	//final int themeType = mTheme.getCurrentThemeType();
    	
    	final int primaryColor;
    	final int secondaryColor;
    	//for custom themes leave few extra pixels on the right side and top of the key
    	int extraPixelX;
        int extraPixelY;
        
    	boolean isCurrentModeQwerty = false;
    	if(mKeyboardSwitcher != null) {
    		isCurrentModeQwerty = mKeyboardSwitcher.isCurrentKeyboardModeQwerty();
    	}
    	
    	
    		extraPixelX = 0;
    		extraPixelY = 0;
    		primaryColor = mTheme.getResources().getColor(mTheme.mPrimaryTextColor);
    		secondaryColor = mTheme.getResources().getColor(mTheme.mSecondaryTextColor);
    		
    		//primaryColor = getResources().getColor(R.color.primary_key_color_val);
    		//secondaryColor = getResources().getColor(R.color.secondary_key_color_val);
    	
    	
    	Drawable keyBGDrawable;
    	if (mKeyboard.isPopupKeyboard) {
			keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mPopupKeyBackground);
		} else {
			keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mKeyBackground);
		}
    	
    	/*
    	 *if the keyshape is changed then leave few pixels on top of the keyboard.
    	 * or else the key backgrounds will overlap the suggestion bar.
    	 * Also reverse translate once the drawing is done 
    	 */
    	if(mShouldCustomKeyShapeDrawn) {
        	canvas.translate(0, EXTRA_PADDING_CUSTOM_KEYSHAPE);
        }
    	
    	paint.setTypeface(mTheme.getUserSelectedFontTypeface());
    	if(mInvalidatedKey != null && mInvalidatedKeyRect.contains(mDirtyRect)
    			/*&& !mIsPopOnScreen*/) {

    		int keyWidthBound = mInvalidatedKey.width;
    		if(mInvalidatedKey.x + kbdPaddingLeft + mInvalidatedKey.width > width) {
    			keyWidthBound = width - (mInvalidatedKey.x + kbdPaddingLeft);
    		}
    		
    		canvas.translate(mInvalidatedKey.x + kbdPaddingLeft + extraPixelX, mInvalidatedKey.y + kbdPaddingTop + extraPixelY);
    		/* 
    		 * only if key background or keyshape is changed in custom theme draw 
    		 * custom key backgrouds, or just follow the normal key background draws.
    		 * just change the primary and secondary colors
    		 */
    		
    			if(mInvalidatedKey.codes[0] == KPTConstants.KEYCODE_SPACE){
					keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mSpaceKey);
					drawKeyBackground(canvas, mInvalidatedKey, keyBGDrawable, keyWidthBound, isCurrentModeQwerty);
				}else{
					keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mKeyBackground);
					drawKeyBackground(canvas, mInvalidatedKey, keyBGDrawable, keyWidthBound, isCurrentModeQwerty);
				}
    		
    		drawKey(canvas, mInvalidatedKey, keyWidthBound, paint, primaryColor, secondaryColor);
    		canvas.translate(-(mInvalidatedKey.x + kbdPaddingLeft + extraPixelX), -(mInvalidatedKey.y + kbdPaddingTop + extraPixelY));
    		
    	} else {
    		
    		if(mKeyboard != null && mKeyboard.mKeys != null){
    			for(Key key : mKeyboard.mKeys) {
    				
    				int keyWidthBound = key.width;
    				if(key.x + kbdPaddingLeft + key.width > width) {
    					keyWidthBound = width - (key.x + kbdPaddingLeft);
    				}
    				
    				canvas.translate(key.x + kbdPaddingLeft + extraPixelX, key.y + kbdPaddingTop + extraPixelY);
    				/* 
    				 * only if key background or keyshape is changed in custom theme draw 
    				 * custom key backgrouds, or just follow the normal key background draws.
    				 * just change the primary and secondary colors
    				 */
    				
    				if(key.codes[0] == KPTConstants.KEYCODE_SPACE){
    					keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mSpaceKey);
    					drawKeyBackground(canvas, key, keyBGDrawable, keyWidthBound, isCurrentModeQwerty);
    				}else{
    					keyBGDrawable = mTheme.getResources().getDrawable(mTheme.mKeyBackground);
    					drawKeyBackground(canvas, key, keyBGDrawable, keyWidthBound, isCurrentModeQwerty);
    				}
    				
    				
    				drawKey(canvas, key, keyWidthBound, paint, primaryColor, secondaryColor);
    				canvas.translate(-(key.x + kbdPaddingLeft + extraPixelX), -(key.y + kbdPaddingTop + extraPixelY));
    			}
    		}
    		
    	}
    	
    	if(mShouldCustomKeyShapeDrawn) {
        	canvas.translate(0, -EXTRA_PADDING_CUSTOM_KEYSHAPE);
        }
    	
    	mInvalidatedKey = null;
    	mDirtyRect.setEmpty();
    	
    	// Overlay a dark rectangle to dim the keyboard
        if (mNeedsToDimEntireKeyboard) {
            paint.setColor((int) (mBackgroundDimAmount * 0xFF) << 24);
            canvas.drawRect(0, 0, width, height, paint);
        }
        
    	////Log.e("KPT","Time taken to draw = " + (System.currentTimeMillis() - startTime));
    }
    
    public void dimEntireKeyboard(final boolean dimmed) {
		final boolean needsRedrawing = mNeedsToDimEntireKeyboard != dimmed;
		mNeedsToDimEntireKeyboard = dimmed;
		if (needsRedrawing) {
			invalidateAllKeys();
		}
	}
    
    /**
     * drawing of key bakgrounds are handled here
     * 
     * @param canvas
     * @param key
     * @param keyBGDrawable
     * @param keyWidthBound
     * @param isCurrentModeQwerty
     */
    private void drawKeyBackground(final Canvas canvas, final Key key, final Drawable keyBGDrawable, 
    		final int keyWidthBound, final boolean isCurrentModeQwerty) {
    	if(key == null) {
    		return;
    	}
    	if (keyBGDrawable == null) {
			return;
		}
    	//check the key selecetion state ie.. if the key is in pressed state or normal state
    	final int[] drawableState = key.getCurrentDrawableState(isCurrentModeQwerty);
    	final Rect bounds = keyBGDrawable.getBounds();
    	if (keyWidthBound != bounds.right || key.height != bounds.bottom) {
    		keyBGDrawable.setBounds(0, 0, keyWidthBound, key.height);
    	}
    	
    	//set key transparencey used on the user preference iee. through customization
    	keyBGDrawable.setAlpha(mTransparency);
    	keyBGDrawable.setState(drawableState);
    	keyBGDrawable.draw(canvas);
    }
    
    /**
     * draw  key background when custom theme is selected
     * @param canvas
     * @param key
     * @param keyBG
     * @param paint
     * @param isCurrentModeQwerty
     * @param keyWidthBound
     * @param extraPixelX
     * @param extraPixelY
     */
   /* private void drawCustomThemeKeyBackground(final Canvas canvas, Key key,  
    		final Drawable keyBG, final Paint paint, 
    		final boolean isCurrentModeQwerty, final int keyWidthBound, 
    		int extraPixelX, int extraPixelY) {
    	if(key == null) {
    		return;
    	}
    	if (canvas == null) {
			return;
		}
    	Drawable keyBGDrawable = null;
    	int borderWidth = 0;
    	boolean isKeyBGAlreadyDrawn = false;
    	
    	final int[] drawableState = key.getCurrentDrawableState(isCurrentModeQwerty);
    	final boolean isInPressedState;
    	if(drawableState != null && drawableState.length > 0 && 
				drawableState[0] == android.R.attr.state_pressed) {
    		isInPressedState = true;
    	} else {
    		isInPressedState = false;
    	}
    	
    	//if this is space key and qwerty, change key state if space should should be
    	//highlighted
    	if(key.codes[0] == KPTConstants.KEYCODE_SPACE) {
    		if(mKeyboard.isSpaceHighlighted()) {
    			key.on = true;
    		} else {
    			key.on = false;
    		}
    	}
    	
    	if(key.on) {
    		if(mShouldCustomKeyShapeDrawn) {
    			borderWidth = (int) getResources().getDimension(R.dimen.custom_shape_border_width);
    			mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    			mCustomKeyShapeDrawable.setBounds(0, 0, getPaddingLeft() + key.width - extraPixelX, 
    										getPaddingTop() + key.height - 2 * extraPixelY);
    			mCustomKeyShapeDrawable.setAlpha(mTransparency);
    			mCustomKeyShapeDrawable.draw(canvas);
    		} else {
    			if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1 ) {
    				borderWidth = (int) getResources().getDimension(R.dimen.custom_shape_border_width);
    				paint.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    				canvas.drawRect(0, 0, getPaddingLeft() + key.width - extraPixelX, 
    									  getPaddingTop() + key.height - extraPixelY, paint);
    			}  else {
    				keyBGDrawable = keyBG;
    				extraPixelX = 0;
    				extraPixelY = 0;
    			}
    		}
    	}

    	Rect bounds = keyBG.getBounds();
		bounds.left = borderWidth;
		bounds.top = borderWidth;
		bounds.right = key.width - extraPixelX - borderWidth;
		bounds.bottom = key.height - extraPixelY - borderWidth;
		
    	if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1 ) { 
    		if(isInPressedState) {
    			keyBGDrawable = new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);				
    		} else{
    			keyBGDrawable = new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]);
    		}

    		if(isBelowICS) {
    			if(mShouldCustomKeyShapeDrawn) {
    				if(drawableState != null && drawableState.length > 0 && 
    						drawableState[0] == android.R.attr.state_pressed) {
    					mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);				
    				} else{
    					mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]);
    				}
    				
    				canvas.save();
    				canvas.clipRect(bounds);
    				mCustomKeyShapeDrawable.setBounds(bounds);
    				mCustomKeyShapeDrawable.setAlpha(mTransparency);
    				mCustomKeyShapeDrawable.draw(canvas);
    				canvas.restore();
    				isKeyBGAlreadyDrawn = true;
    			} else {
    				canvas.save();
    				canvas.clipRect(bounds);
    				keyBGDrawable.setBounds(bounds);
    				keyBGDrawable.draw(canvas);
    				canvas.restore();
    				isKeyBGAlreadyDrawn = true;
    			}
    		} else {
    			if(mShouldCustomKeyShapeDrawn) {
    				
    				int extraGap = (int) getResources().getDimension(R.dimen.custom_shape_border_width);
            		Rect mRect = new Rect(borderWidth, borderWidth, keyWidthBound - extraGap - borderWidth, 
            											key.height - extraGap - borderWidth);
        			mCustomKeyShapeDrawable.setBounds(mRect);
            		if(isInPressedState) {
            			mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);				
            		} else{
            			if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1) {
            				mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]);
            			} else {
            				mCustomKeyShapeDrawable.setColor(mTheme.getResources().getColor(mTheme.mCustomKeyShapeColor));
            			}
            		}
            		mCustomKeyShapeDrawable.setAlpha(mTransparency);
            		mCustomKeyShapeDrawable.draw(canvas);
            		isKeyBGAlreadyDrawn = true;
            	} 
    		}
    	} else {
    		if(isBelowICS) {
    			if(mShouldCustomKeyShapeDrawn) {
    				if(isInPressedState) {
    					mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);				
    				} else{
    					mCustomKeyShapeDrawable.setColor(mTheme.getResources().getColor(mTheme.mCustomKeyShapeColor));
    				}
    				
    				canvas.save();
    				canvas.clipRect(bounds);
    				mCustomKeyShapeDrawable.setBounds(bounds);
    				mCustomKeyShapeDrawable.setAlpha(mTransparency);
    				mCustomKeyShapeDrawable.draw(canvas);
    				canvas.restore();
    				isKeyBGAlreadyDrawn = true;
    			} else {
    				keyBGDrawable = keyBG;
    				extraPixelX = 0;
                	extraPixelY = 0;
    			}
    		} else {
    			if(mShouldCustomKeyShapeDrawn) {
    				int extraGap = (int) getResources().getDimension(R.dimen.custom_shape_border_width);
    				Rect mRect = new Rect(borderWidth, borderWidth, keyWidthBound - extraGap - borderWidth, 
    						key.height - extraGap - borderWidth);
    				mCustomKeyShapeDrawable.setBounds(mRect);
    				if(isInPressedState) {
    					mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);				
    				} else{
    					if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1) {
    						mCustomKeyShapeDrawable.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]);
    					} else {
    						mCustomKeyShapeDrawable.setColor(mTheme.getResources().getColor(mTheme.mCustomKeyShapeColor));
    					}
    				}
    				mCustomKeyShapeDrawable.setAlpha(mTransparency);
    				mCustomKeyShapeDrawable.draw(canvas);
    				isKeyBGAlreadyDrawn = true;
    			} else {
    				keyBGDrawable = keyBG;
    				extraPixelX = 0;
    				extraPixelY = 0;
    			}
    		}
    	}
    	
    	if(keyBGDrawable != null){
    		if (keyBGDrawable instanceof Drawable) {
    			keyBGDrawable.setAlpha(mTransparency);
    			keyBGDrawable.setState(drawableState);
    		}
    		if (!isKeyBGAlreadyDrawn) {
    			keyBGDrawable.setBounds(borderWidth, borderWidth, 
    					keyWidthBound - extraPixelX - borderWidth, key.height - extraPixelY - borderWidth);
    			keyBGDrawable.draw(canvas);
    		}
    	}
    }*/
    
    /**
     * draw the key labels, external font icons like delete, shift etc.. and icons.
     * 
     * @param canvas 
     * @param key  
     * @param keyWidthBound
     * @param paint
     * @param primaryColor
     * @param secondaryColor
     */
    private void drawKey(final Canvas canvas, final Key key, final int keyWidthBound, 
    		final Paint paint, final int primaryColor, final int secondaryColor) {
    	if(canvas == null || key == null) {
    		return;
    	}

    	String label = key.label == null? null : adjustCase(key.label).toString();
    	final String label2 = key.label2 == null? null : adjustCase(key.label2).toString(); 
    	
    	paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
    	if (label != null) {
		
			paint.setTypeface(mTheme.getUserSelectedFontTypeface());
    		
    		//if key have only one label like in popup characters and 12 keys
    		if(label != null && label2 == null) {
    			/**
    			 * The following if condition is used to draw the locale name on key
    			 * to switch between languages when in pwd field.
    			 */
    			if(key.codes[0] == KPTConstants.KEYCODE_LOCALE_SWITCH){
    				if(!mLanguageSwitcher.isLatinScript()){
    					label = getResources().getString(R.string.kpt_english_locale);
    				} else {
    					label = mLanguageSwitcher.getPrevLangLocaleString().substring(0,2).toUpperCase();
    				}
				}
    			
    			/*
    			 * tp 11858. this is a temp fix. when mini-kb is drawn(upon longpressing a key),
    			 * even the main-kb is getting drawn again, so atx key is drawn as popup key instead of normal key
    			 * Find out who is calling main-kb.
    			 */
    			if(/*mIsPopOnScreen*/ key.keyLabelYPos == 0) {
    				drawPopupKeys(canvas, key, paint, keyWidthBound, primaryColor);
    			} else {
    				paint.setColor(primaryColor);
    				//for atx key alone the text has to be underlines and text size has to be different
    				if(key.codes[0] == KPTConstants.KEYCODE_ATX) {
    					paint.setTextSize(key.keyLabel2TextSize);
    					if(mAtxKey!=null && mAtxKey.on) {
    						paint.setUnderlineText(true);
    					}
    					if(mDisableATX) { //tp #8182
    						paint.setColor(Color.GRAY);
    					}
    				} else {
    					
    					//Fix for TP Item - 12285
    					if (label.length() > 1 && key.codes[0] != KPTConstants.KEYCODE_LOCALE_SWITCH) {
    						int modifiedTextSize = getExactTextSize(key, label);
    					
    						if(modifiedTextSize != -1 && (key.vowelBase == null || key.vowelBase.length == 0) ){
    							paint.setTextSize(modifiedTextSize); 
    						} else{
    							paint.setTextSize(mKeyTextSize/*key.keyLabelTextSize*/);
    						}
    					} else {
    		    			paint.setTextSize(mKeyTextSize/*key.keyLabelTextSize*/);
    		    		}
    				}
    				
    				canvas.drawText(label, 
    						(keyWidthBound * key.keyLabelXPos),
    						(key.height * key.keyLabelYPos)
    						- (paint.ascent() *  mPrimary_offset),
    						paint);

    				if(paint.isUnderlineText()) {
    					paint.setUnderlineText(false);
    				}
    			}
    		} else if(label != null && label2 != null) {
    			paint.setTextSize(key.keyLabelTextSize);
    			paint.setColor(primaryColor);
				canvas.drawText(label, 
						(keyWidthBound * key.keyLabelXPos),
						(key.height * key.keyLabelYPos)
						- (paint.ascent() *  mPrimary_offset),
						paint);
				
    			paint.setTextSize(key.keyLabel2TextSize);
        		paint.setColor(secondaryColor);
        		canvas.drawText(label2,
        				(keyWidthBound * key.keyLabel2XPos),
        				(key.height * key.keyLabel2YPos)
        				- (paint.ascent() *  mSecondary_offset),
        				paint);
        		
    		}
    	}

    	/*
    	 * handling space seperately because in qwerty keypad we need to draw language name 
    	 * chevrons which cannot be specified in the xml. This same method will be called
    	 *  from ime to handle space highlight scenarios. 
    	 * in phone keypad we draw based on the icon text specified in the xml
    	 */
    	if(key.codes[0] == KPTConstants.KEYCODE_SPACE) {
    		updateSpaceKeyHighlight(canvas, key, keyWidthBound, paint, primaryColor);
    		return;
    	}
    	
    	//handle external font icons  
    	if(key.useExternalFont) {
    		drawKeyExternalFontLabels(canvas, key, paint, 
    				keyWidthBound, primaryColor, secondaryColor);
    	}

    	//draw key icons
    	if(key.icon != null) {
    		drawKeyIcons(canvas, key, keyWidthBound, paint, primaryColor);
    	}
    }
    
    /**
     * this will handle the drawing of popup keyboard keys
     * 
     * @param canvas
     * @param key
     * @param paint
     * @param keyWidthBound
     */
    private void drawPopupKeys(final Canvas canvas, Key key,
    		final Paint paint, final int keyWidthBound, final int primaryColor) {

    	final String label = key.label == null? null : adjustCase(key.label).toString();
    	if(label != null) {
    		/*
    		 * To get the custom font type for the left and right chevron icons of the bubble window 
    		 * as we are taking them from ttf file..
    		 */
    		
    		if(label.equalsIgnoreCase(mRightChevronIconText) 
    				|| label.equalsIgnoreCase(mLeftChevronIconText)){
    			paint.setTypeface(mTheme.getCustomFontTypeface());
    		} /*else {
    			paint.setTypeface(mTheme.getUserSelectedFontTypeface());
    		}*/

    		paint.setColor(primaryColor);	

    		/*
    		 * if the popup keys has larger texts like http, .com, handle the
    		 * text sizes in run time based on the text length. else draw the primary
    		 * lebel text size
    		 */
    		
    		
    		if (label.length() > 1) {
    			int modifiedTextSize = getExactTextSize(key, label);
    			if(modifiedTextSize != -1) {
    				paint.setTextSize(modifiedTextSize); 
    			} else{
    				paint.setTextSize(getResources().getDimension(R.dimen.kpt_keyLabelTextSize));
    			}
    		} else {
    			paint.setTextSize(getResources().getDimension(R.dimen.kpt_keyLabelTextSize));
    		}
    		/*
    		 * for popup keyboards alone we are hardcoding x, y positions as 0.5 and
    		 * 0.5 respectively(nothing but center of key) since most of the popup keyboards are not xmls,
    		 * just strings and key objects are constructed after the key is pressed
    		 */ 
    		canvas.drawText(label,
    				(float)(keyWidthBound  * 0.5), 
    				(float) ((key.height * 0.5)
    						- (paint.ascent() *  mPrimary_popup_offset)),
    						paint);
    		
    	}
    }
    
    /**
     * this handles the drawing of icons as external fonts. for this we maintain a special flag 
     * useExternalFont for keys which indicate that key has to drawn the icon as text. 
     * the text is set as iconText field for the same key.
     * 
     * the fonts are drawn using the adp.ttf file present in the assests.
     * 
     * @param canvas
     * @param key
     * @param keyWidthBound
     * @param primaryColor
     * @param secondaryColor
     */
    private void drawKeyExternalFontLabels(final Canvas canvas, final Key key, final Paint paint, 
    		final int keyWidthBound, int primaryColor, final int secondaryColor) {

    	//get the icons as labels for the key
    	String[] iconLables = getStringsForIcons(key);

    	if(iconLables != null) {
    		String label = null;
    		String label2 = null;

    		if(iconLables.length == 1) {
    			if(key.codes[0] == KPTConstants.KEYCODE_SHIFT) {
    				int shiftState = ((KPTAdpatxtKeyboard)mKeyboard).getShiftState();
    				label = getResources().getString(R.string.kpt_icontext_shift_off);
    				if(shiftState == KPTAdpatxtKeyboard.SHIFT_LOCKED) {
    					label = getResources().getString(R.string.kpt_icontext_shift_lock);
    				}else if(mMultiTapShiftState || shiftState == KPTAdpatxtKeyboard.SHIFT_ON){
    					label = getResources().getString(R.string.kpt_icontext_shift_on);
    				}else if (shiftState == KPTAdpatxtKeyboard.SHIFT_OFF){
    					label = getResources().getString(R.string.kpt_icontext_shift_off);
    				}
    			}else if(key.codes[0] == KPTConstants.KEYCODE_XI) {
    				label = getResources().getString(R.string.kpt_icontext_xi_key);
    				/*if(!KPTConstants.mXi_enable){
    					if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM){
    						primaryColor = Color.GRAY;
    					}else{
    						primaryColor = mTheme.getResources().getColor(mTheme.mXiDisable);
    					}

    					primaryColor = mTheme.getResources().getColor(mTheme.mXiDisable);
    				}*/
    			} else if(key.codes[0] == KPTConstants.KEYCODE_LOCALE_SWITCH) {
    				label = mLanguageSwitcher.getPrevLangLocaleString().substring(0, 2).toUpperCase();
    			} else if(key.codes[0] == KPTConstants.KEYCODE_ENTER ){
    				final int imeOption = getIMEOptionForEnterkey();
        			
        			switch (imeOption) {
    				case EditorInfo.IME_ACTION_DONE:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_done);
    					break;
    				case EditorInfo.IME_ACTION_GO:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_go);
    					break;
    				case EditorInfo.IME_ACTION_NEXT:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_next);
    					break;
    				case EditorInfo.IME_ACTION_SEND:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_send);
    					break;
    				case EditorInfo.IME_ACTION_SEARCH:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_search);
    					break;
    				default:
    					label = this.getResources().getString(R.string.kpt_icontext_enter_default);
    					break;
    				}
    			}else if(key.codes[0] == KPTConstants.KEYCODE_GLOBE ){
    				label = this.getResources().getString(R.string.kpt_icontext_globe);
    			}
    			else{
    				label = iconLables[0];
    			}
    		} else if(key.codes[0] == KPTConstants.KEYCODE_ENTER ){
    			final int imeOption = getIMEOptionForEnterkey();
    			switch (imeOption) {
				case EditorInfo.IME_ACTION_DONE:
					label = this.getResources().getString(R.string.kpt_icontext_enter_done);
					break;
				case EditorInfo.IME_ACTION_GO:
					label = this.getResources().getString(R.string.kpt_icontext_enter_go);
					break;
				case EditorInfo.IME_ACTION_NEXT:
					label = this.getResources().getString(R.string.kpt_icontext_enter_next);
					break;
				case EditorInfo.IME_ACTION_SEND:
					label = this.getResources().getString(R.string.kpt_icontext_enter_send);
					break;
				case EditorInfo.IME_ACTION_SEARCH:
					label = this.getResources().getString(R.string.kpt_icontext_enter_search);
					break;
				default:
					label = this.getResources().getString(R.string.kpt_icontext_enter_default);
					break;
				}
    		}else if(key.codes[0] == KPTConstants.KEYCODE_GLOBE ){
				label = this.getResources().getString(R.string.kpt_icontext_globe);
			}else{
    			label = iconLables[0];
    			label2 = iconLables[1];
    		}

    		float labelXPos;
    		float labelYPos;
    		float labelHeight;
    		
    		//use this paint object for drawing external fonts, as this paint object is already
    		//loaded with adp.ttf font. Also external fonts for all functional keys will
    		//have same text across all languages
    		mPaintCustomFont.setTextSize(mKeyTextSize/*getResources().getDimension(R.dimen.external_font_text_size)*/); //TP 11761, 11786
    		mPaintCustomFont.setColor(primaryColor);
    		if(label != null) {
    			
    			labelXPos = keyWidthBound * key.keyLabelXPos;
    			if(label2 != null) {
    				labelYPos = (float) ((key.height * key.keyLabelYPos)
    						- (mPaintCustomFont.ascent() *  mPrimary_offset));
    			} else {
    				labelHeight = getCharHeight(new char[] { label.charAt(0) } , mPaintCustomFont);
    				labelYPos = key.height * key.keyLabelYPos + labelHeight/2;
    			}
        		
    			canvas.drawText(label, labelXPos,
    					labelYPos,
    					mPaintCustomFont);
    		}

    		mPaintCustomFont.setColor(secondaryColor);
    		mPaintCustomFont.setTextSize(key.keyLabel2TextSize);
    		if(label2 != null) {
    			if(key.codes[0] != KPTConstants.KEYCODE_ENTER){
    				//	16642 	Thai layout (font issue) 
    				labelXPos = keyWidthBound * key.keyLabel2XPos;
    				labelYPos = key.height * key.keyLabel2YPos;
    				labelHeight = getCharHeight(new char[] { label2.charAt(0) } , mPaintCustomFont);

    				canvas.drawText(label2,labelXPos,
    						labelYPos + labelHeight/2,
    						mPaintCustomFont);

    				if(mPaintCustomFont.getTypeface() == Typeface.DEFAULT_BOLD) {
    					mPaintCustomFont.setTypeface(mTheme.getCustomFontTypeface());
    				}
    			}
    		}
    	}
    }
    
    /**
     * draw icons on key. mainly used to draw keys which has adaptxt logo 
     * since that will not be put in external font
     * 
     * @param canvas
     * @param key
     * @param keyWidthBound
     * @param paint
     * @param primaryColor
     */
    private void drawKeyIcons(final Canvas canvas, final Key key, 
    		final int keyWidthBound, final Paint paint, final int primaryColor) {
    	if (mKeyboardSwitcher == null || mLanguageSwitcher == null){
    		return;
    	}
    	paint.setTextSize(key.keyLabel2TextSize);
    	if(AdaptxtIME.mIsHindhi){
    		drawKeyIconsHindhi(canvas, key, keyWidthBound, paint, primaryColor);
    		return;
    	}
    	
    	boolean indic = false;
    	if(indicLanguages.contains(mLanguageSwitcher.getInputLanguageLocale()) 
    			&&(mKeyboardSwitcher.getAltState() != 2)){
    		indic = true;
    	}else{
    		indic = !mKeyboardSwitcher.mIsSymbols;
    	}

    	if((!mKeyboardSwitcher.isCurrentKeyboardModeQwerty()  
    			&& mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) 
    			|| (mKeyboardSwitcher.isCurrentKeyboardModeQwerty() 
    					&&  indic)){
    		//In XML file there is no label for this key so we are getting the string from resources to support localization
    		if(mKeyboardSwitcher.isCurrentKeyboardModeQwerty()) {
    			mAdaptxtKeyLabel = mKeyboardSwitcher.m123TextForQwerty;
    		} else {
    			mAdaptxtKeyLabel =  mKeyboardSwitcher.m123Text;
    		}
    	} else {
    		//In XML file there is no label for this key so we are getting the string from resources to support localization
    		mAdaptxtKeyLabel = mKeyboardSwitcher.mABCText;
    	}

    	/*if(mLanguageSwitcher.getInputLanguageLocale().equals("th-TH")) {
    		
    		if(mKeyboardSwitcher.getCurrentQwertyKeypadState() == KPTKeyboardSwitcher.MODE_QWERTY_ALPHABET || 
        			mKeyboardSwitcher.getCurrentQwertyKeypadState() == KPTKeyboardSwitcher.MODE_QWERTY_SYMBOLS){
    			mAdaptxtKeyLabel = mKeyboardSwitcher.m123TextForQwerty;
    		} else {
    			mAdaptxtKeyLabel = mKeyboardSwitcher.mABCText;
    		}
    		
    	}*/ 

    	paint.setColor(primaryColor);
    	canvas.drawText(mAdaptxtKeyLabel, 
    			(float)(keyWidthBound  * key.keyLabelXPos),
    			(float) ((key.height * key.keyLabelYPos)
    					- (paint.ascent() *  mPrimary_offset)),
    					paint);
    	//Stopping the bg drawable for 123# key in 12-Key layout
    	if(!(key.codes[0] == KPTConstants.KEYCODE_PHONE_MODE_SYM)){
    		key.icon.setBounds(0, 0, key.width, key.height);
    		key.icon.draw(canvas);
    	}
    }
    
    
    
    private void drawKeyIconsHindhi(final Canvas canvas, final Key key, 
    		final int keyWidthBound, final Paint paint, final int primaryColor) {
    	
    	
    	
    	
    	if(!mKeyboardSwitcher.isCurrentKeyboardModeQwerty()  
    			&& mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET){
    		mAdaptxtKeyLabel =  mContext.getResources().getString(R.string.kpt_label_alpha_key);
    	}else if(mKeyboardSwitcher.getAltState() != 1){
    		mAdaptxtKeyLabel = mKeyboardSwitcher.m123TextForQwerty;
    	}else{
    		mAdaptxtKeyLabel =  mContext.getResources().getString(R.string.kpt_label_alpha_key);
    	}
    	

    	paint.setColor(primaryColor);
    	canvas.drawText(mAdaptxtKeyLabel, 
    			(float)(keyWidthBound  * key.keyLabelXPos),
    			(float) ((key.height * key.keyLabelYPos)
    					- (paint.ascent() *  mPrimary_offset)),
    					paint);

    	key.icon.setBounds(0, 0, key.width, key.height);
    	key.icon.draw(canvas);
    }
    
    
    private static float getCharHeight(char[] character, Paint paint) {
    	final Integer key = getCharGeometryCacheKey(character[0], paint);
        final Float cachedValue = mTextHeightCache.get(key);
        if (cachedValue != null)
            return cachedValue;

        paint.getTextBounds(character, 0, 1, mTextBounds);
        final float height = mTextBounds.height();
        mTextHeightCache.put(key, height);
        return height;
    }
    
    private static int getCharGeometryCacheKey(char reference, Paint paint) {
        final int labelSize = (int)paint.getTextSize();
        final Typeface face = paint.getTypeface();
        final int codePointOffset = reference << 15;
        if (face == Typeface.DEFAULT) {
            return codePointOffset + labelSize;
        } else {
            return codePointOffset + labelSize;
        }
    }
    
    /**
     * this is space related drawings. Language name is displayed on the space bar.
     * Also if space bar is highlighted changes the key background drawable
     * 
     * @param canvas
     * @param key
     * @param keyWidthBound
     * @param paint
     * @param primaryColor 
     */
    public void updateSpaceKeyHighlight(final Canvas canvas, final Key key, 
    		final int keyWidthBound, final Paint paint, final int primaryColor) {
    	//boolean trialFalg = Boolean.parseBoolean(Settings.System.getString(getContext().getContentResolver(), KPTConstants.TRIAL_EXPIRED));
    	////Log.e("kpt","is atleast one installed-->"+!KPTAdaptxtIME.mIsAtleastOneOpenAdaptxtDictionaryInstalled);
    	int keyLabel2TextSizeForSpace = (int) getResources().getDimension(R.dimen.kpt_keyLabel2TextSizeForSpace);
    	if(mLanguageSwitcher == null || mKeyboardSwitcher == null) {
    		return;
    	}
    	if (mTheme == null) {
			return;
		}
    	boolean isCustomTheme = mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM
    								? true : false;
    	if(mKeyboard.isSpaceHighlighted()) {
    		//check for custom theme and key_bg, if key background is not changed
    		//call the normal key flow
    		if(isCustomTheme && (mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1
    				|| mShouldCustomKeyShapeDrawn)) {
    			//this will be handled in drawCustomKeyBackground. check for space condition
    		} else {
    				//mSpaceHighlightedDrawable = mTheme.getResources().getDrawable(mTheme.mSpaceHighlight);
    				mSpaceHighlightedDrawable = getResources().getDrawable(mTheme.mSpaceKey);
    				mSpaceHighlightedDrawable.setBounds(0, 0, key.width, key.height);
    				//mSpaceHighlightedDrawable.setAlpha(mTheme.getCurrentCustomTransparency());
    				mSpaceHighlightedDrawable.draw(canvas);
    		}
    	}
    	
    	/*mSpaceHighlightedDrawable = getResources().getDrawable(mTheme.mSpaceKey);
		mSpaceHighlightedDrawable.setBounds(0, 0, key.width, key.height);
		mSpaceHighlightedDrawable.setAlpha(mTheme.getCurrentCustomTransparency());
		mSpaceHighlightedDrawable.draw(canvas);*/
    	

    	/*if(isCustomTheme) {
    		paint.setColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    	} else {
    		paint.setColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));
    	}*/
    	
    	paint.setColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));
    	
    	if(SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_LOW
    			|| SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_MEDIUM ) {
    		paint.setTextSize(keyLabel2TextSizeForSpace - 2);
    	} else {
    		paint.setTextSize(keyLabel2TextSizeForSpace);
    	}
    	/*
    	 * if the current keyboard is qwerty display the language name on it
    	 * if it is phone keypad, primary will always be space. but secondary char will be
    	 * language dependent. like for few indic language instead of zero we use
    	 * different character. so first check label2 if not then use externaltext  
    	 */
    	if(mKeyboardSwitcher.isCurrentKeyboardModeQwerty()
    			&& !mKeyboardSwitcher.isCurrentKeyboardQWERTYPhone()){
    		
    			/*
    			 *	this is ONLY for qwerty primary, qwerty secondary and qwerty tertiary
    			 * NOT for qwerty numeric phone and qwerty numeric phone symbols(phone number fields)
    			 */
    			String language = mLanguageSwitcher.getCurrentLanguageDisplayName();
    			if(language != null){
    				canvas.drawText(language,
    						(float)(keyWidthBound * 0.50),
    						(float) ((key.height * 0.5) - (paint.ascent() *  mSecondary_offset)),
    						paint);
    			}
    	} else if(mKeyboardSwitcher.isCurrentKeyboardQWERTYPhone() || mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_SYMBOLS_SHIFTED) {
    		/*
    		 * language name will NOT be displayed for qwerty numeric phone and qwerty numeric phone symbols
    		 * instead draw normal space
    		 */
    		String[] iconLables = getStringsForIcons(key);
			if(iconLables != null && iconLables[0] != null && iconLables[0].length() > 0) {
				float labelXPos = keyWidthBound * key.keyLabelXPos;
				float labelYPos = key.height * key.keyLabelYPos;
				float labelHeight = getCharHeight(new char[] { iconLables[0].charAt(0) } , mPaintCustomFont);

				mPaintCustomFont.setColor(primaryColor);
				canvas.drawText(iconLables[0], labelXPos,
						labelYPos + labelHeight/2,
						mPaintCustomFont);
			}
    	} else {
    		if((mKeyboardSwitcher.getPhoneKeypadState() != KPTKeyboardSwitcher.MODE_PHONE_SYMBOLS_SHIFTED)){
    			/*
    			 *	this is ONLY for qwerty primary, qwerty secondary and qwerty tertiary
    			 * NOT for qwerty numeric phone and qwerty numeric phone symbols(phone number fields)
    			 */
    			String language = mLanguageSwitcher.getCurrentLanguageDisplayName();
    			if(language != null){
    				canvas.drawText(language,
    						(float)(keyWidthBound * 0.50),
    						(float) ((key.height * 0.55) - (paint.ascent() *  mSecondary_offset)),
    						paint);
    			}
    		}

    		if(mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET){
    			//if the label 2 is specified like for marathi draw it or else draw zero
    			String label2 = key.label2 == null? "0" : key.label2.toString();
    			canvas.drawText(label2,
    					(float)(keyWidthBound * 0.50),
    					(float) ((key.height * 0.25)
    							- (paint.ascent() *  mSecondary_offset)),
    							paint);
    			if(paint.getTypeface() == Typeface.DEFAULT_BOLD) {
    				paint.setTypeface(mTheme.getCustomFontTypeface());
    			}
    		}
    	}
    	
     	//draw chevrons on space key if more than 1 lang is installed
    	if( mLanguageSwitcher.getLocaleCount() > 1 
    			&& (mKeyboardSwitcher.isCurrentKeyboardModeQwerty() || (mKeyboardSwitcher.getPhoneKeypadState() != KPTKeyboardSwitcher.MODE_PHONE_SYMBOLS_SHIFTED))) {
    		paint.setTextSize(getResources().getDimension(R.dimen.kpt_keytextsize));
    		paint.setTypeface(mTheme.getCustomFontTypeface());
    		paint.setColor(primaryColor);
    		
    		canvas.drawText(getResources().getString(R.string.kpt_icontext_space_chevron_left), 
    				getResources().getDimension(R.dimen.kpt_draw_space_key_arrows)/2,  
    				(float) ((key.height * 0.50) - (paint.ascent() *  mSecondary_offset)), 
    				paint);

    		canvas.drawText(getResources().getString(R.string.kpt_icontext_space_chevron_right), 
    				key.width - getResources().getDimension(R.dimen.kpt_draw_space_key_arrows)/2,
    				(float) ((key.height * 0.50) - (paint.ascent() *  mSecondary_offset)), 
    				paint);
    		
    	}
    }
    
    /**
     * get text that has to be drawn as icons
     * @param k
     * @return
     */
    public String[] getStringsForIcons(Key key) {
    	CharSequence iconText = key.iconText;
    	if(iconText != null) {
    		return iconText.toString().split(":");
    	}
    	return null;
    }

    /**
	 * Handle multi-tap keys by producing the key label for the current
	 * multi-tap state.
	 */
	private CharSequence getPreviewText(final PointerTracker tracker) {
		final Key key = tracker.getKey();
		if (tracker.isInMultiTap()) {
			 final int tapCount = tracker.getMultiTapCount(); 
			// Multi-tap
			mPreviewLabel.setLength(0);
			mPreviewLabel
					.append((char) key.codes[tapCount < 0 ? 0 : tapCount]);
			return adjustCase(mPreviewLabel);
		} else {
			return adjustCase(key.label);
		}
	}
	 
	public boolean isCurrentKeyLongPressed() {
		return mIsLongPressed;
	}
	
	/**
	 * Check the key is symbols change mode key(called 1/6 key) or not.
	 * 
	 * @param keyIndex
	 *            the key index.
	 * @return true if the pressed key is symbols change mode key.
	 */
	public boolean isSymbolsPageChangeKey(Key key) {
		return key != null
				&& key.codes[0] == KPTConstants.KEYCODE_PHONE_SYM_PAGE_CHANGE;
	}
    
	public void displaySpacePopup(PointerTracker tracker) {
		final Key key = mKeyboard.getSpaceKey();
		if(key == null) {
			return;
		}
		
		//Used to prevent SPACE key preview and bubble char preview.
		if ((key.codes[0] == KPTConstants.KEYCODE_SPACE && 
				(((mLanguageSwitcher.getLocaleCount()==1)) || !mSpaceSwypeStart))){
			return;
		}


		final PreviewTextCustomView previewText = getKeyPreviewText(tracker.mPointerId);
		if (mTheme != null) {
			Drawable drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
			previewText.setBackgroundDrawable(drawable);
		}
		previewText.setImageDrawable(key.iconPreview != null ? key.iconPreview : key.icon);
		previewText.setShouldDisplayEllipse(false);
		
		/*
		 * for custom theme we use seperate ellipse.
		 */
		/*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
			
			previewText.setThemePrimaryColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_PRIMARY_CHAR]);
			previewText.setThemeSecondaryColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);

			previewText.setDrawCustomThemeKeyPreview(true);
			previewText.setBackgroundDrawable(null);
			//if key bg is not changed, then use the normal key drawable
			if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
				previewText.setBackgroundDrawable(new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]));
			}else{
				Drawable drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
				previewText.setBackgroundDrawable(drawable);
			}
		} else {
		}*/
		
		previewText.setDrawCustomThemeKeyPreview(false);
		previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));
		
		if (previewText.getParent() == null) {
			addKeyPreview(previewText);
		}

		int popupHeight = mPreviewHeight;
		int popupWidth = key.width;
		int mPopupPreviewX;
		int mPopupPreviewY;
		
		if (!mPreviewCentered) {
			mPopupPreviewX = key.x - previewText.getPaddingLeft() + getPaddingLeft();
			mPopupPreviewY = key.y - (popupHeight+mActionBarHeight) + mPreviewOffset;
		} else {
			mPopupPreviewX = 160 - previewText.getMeasuredWidth() / 2;
			mPopupPreviewY = - previewText.getMeasuredHeight();
		}
		
		if (mOffsetInWindow == null) {
			mOffsetInWindow = new int[2];
			getLocationInWindow(mOffsetInWindow);
			mOffsetInWindow[0] += mMiniKeyboardOffsetX; // Offset may be zero
			mOffsetInWindow[1] += mMiniKeyboardOffsetY; // Offset may be zero
			int[] mWindowLocation = new int[2];
			getLocationOnScreen(mWindowLocation);
			mWindowY = mWindowLocation[1];
		}
		
		mPopupPreviewX += mOffsetInWindow[0];
		mPopupPreviewY += mOffsetInWindow[1];
		
		// If the popup cannot be shown above the key, put it on the side
		if (mPopupPreviewY + mWindowY < 0) {
			// If the key you're pressing is on the left side of the keyboard, show the popup on
			// the right, offset by enough to see at least one key to the left/right.
			if (key.x + key.width <= getWidth() / 2) {
				mPopupPreviewX += (int) (key.width * 2.5);
			} else {
				mPopupPreviewX -= (int) (key.width * 2.5);
			}
			mPopupPreviewY += popupHeight;
		}
				
		previewText.setLayoutParams(popupWidth, popupHeight);
		//previewText.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ViewLayoutUtils.placeViewAt(previewText, mPopupPreviewX, mPopupPreviewY, popupWidth, popupHeight);
		previewText.requestLayout();
		previewText.invalidate();
		previewText.setVisibility(VISIBLE);
	}
		 
    public void resetWindowOffsets() {
    	mOffsetInWindow = null;
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only 
     * draws the cached buffer.
     * @see #invalidateKey(int)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        mBufferNeedsUpdate = true;
        invalidate();
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     * @param keyIndex the index of the key in the attached {@link Keyboard}.
     * @see #invalidateAllKeys
     */
    public void invalidateKey(int keyIndex) {
        if (mKeys == null) {
        	return;
        }
        if (keyIndex < 0 || keyIndex >= mKeys.length) {
            return;
        }
        final Key key = mKeys[keyIndex];
        final int x = key.x + getPaddingLeft();
        final int y = key.y + getPaddingTop();
        mInvalidatedKey = key;
        mInvalidatedKeyRect.set(x + EXTRA_HORIZONTAL_PADDING_12_KEY,  y + EXTRA_PADDING_CUSTOM_KEYSHAPE, 
                x + key.width , y + key.height + EXTRA_PADDING_CUSTOM_KEYSHAPE);
        mDirtyRect.union(mInvalidatedKeyRect);
        mBufferNeedsUpdate = true;
        invalidate(mInvalidatedKeyRect);
    }
    
    /**
     * invalidate space key alone. since space highlight should be checked from ime,
     * just call this if it is to be invalidated
     */
    public void invalidateSpaceKey() {
    	if (mKeyboard != null) {
    		invalidateKey(mKeyboard.getSpacekeyIndex());
		}
    }

    @Override
	public boolean dismissMoreKeysPanel() {
		return false;
	}

	@Override
	public void resetGlideInputType() {
		
	}

	@Override
	public void invalidateKey(Key key) {
		final int x = key.x + getPaddingLeft();
		final int y = key.y + getPaddingTop();
		mCurrentKeyIndex = key.keyIndexInRow;
		mInvalidatedKey = key;
		mInvalidatedKeyRect.set(x + EXTRA_HORIZONTAL_PADDING_12_KEY,  y + EXTRA_PADDING_CUSTOM_KEYSHAPE, 
				x + key.width , y + key.height + EXTRA_PADDING_CUSTOM_KEYSHAPE);
		mDirtyRect.union(mInvalidatedKeyRect);
		mBufferNeedsUpdate = true;
		invalidate(mInvalidatedKeyRect);
	}

	@Override
	public void showKeyPreview(PointerTracker tracker) {
		if(!isKeyPreviewPopupEnabled()) {
			//display popup option is not enabled in settings
			return;
		}
		final Key key = tracker.getKey();
		if (key == null) {
			////Log.e(TAG, "For pointyer id --> "+tracker.mPointerId +" key is null");
			return;
		}
		
		if(key != null) {
			/*
			 * dont display key preview for any of these following keys
			 */
			if((key.codes[0] == KPTConstants.KEYCODE_XI && !KPTConstants.mXi_enable)
					|| (key.codes[0] == KPTConstants.KEYCODE_ATX && mAtxKey != null && !mAtxKey.on)
					|| key.codes[0] == KPTConstants.KEYCODE_SPACE) {
				return;
			}
		}
		
		int popupWidth;
		
		final PreviewTextCustomView previewText = getKeyPreviewText(tracker.mPointerId);
		previewText.setTextColor(getResources().getColor(R.color.kpt_primary_key_color));
		previewText.setGravity(Gravity.CENTER);

		Drawable drawable = null;
		if (key.icon != null || key.useExternalFont) {
			drawable = setIconForTextView(key, previewText);
			popupWidth = (key.width + previewText.getPaddingLeft() + previewText.getPaddingRight());
		} else {
			drawable = setTextForTextView(tracker, previewText);
			/*
			 * for qwerty, numeric keypad the key preview will be one and half time more than the key width 
			 */
			if(mKeyboardSwitcher.isCurrentKeyboardModeQwerty()){
				if(mKeyboardSwitcher.getCurrentQwertyKeypadState() == KPTKeyboardSwitcher.MODE_QWERTY_NUMERIC/*
						|| mKeyboardSwitcher.getCurrentQwertyKeypadState() == KPTKeyboardSwitcher.MODE_QWERTY_NUMERIC_SYMBOLS*/){
					popupWidth = (int)((key.width + previewText.getPaddingLeft() + previewText.getPaddingRight())) ;
				}else{
					popupWidth = (int)((key.width + previewText.getPaddingLeft() + previewText.getPaddingRight())  * 1.5) ;
				}
			} else {
				popupWidth = (key.width + previewText.getPaddingLeft() + previewText.getPaddingRight());
			}
		}

		int popupHeight = mPreviewHeight;
	
		if (!mPreviewCentered) {
			mPopupPreviewX = key.x - previewText.getPaddingLeft() + getPaddingLeft();
			mPopupPreviewY = key.y - (popupHeight+mActionBarHeight) + mPreviewOffset;
		} else {
			mPopupPreviewX = 160 - previewText.getMeasuredWidth() / 2;
			mPopupPreviewY = - previewText.getMeasuredHeight();
		}
		
			previewText.setShouldDisplayEllipse(false);
			previewText.setDrawCustomThemeKeyPreview(false);
			previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));
			previewText.setPadding(0, 0, 0, 0);
		
		/*
		 * make sure we clear mOffsetInWindow when moving from one editor to another 
		 */
		if (mOffsetInWindow == null) {
			mOffsetInWindow = new int[2];
			getLocationInWindow(mOffsetInWindow);
			mOffsetInWindow[0] += mMiniKeyboardOffsetX; // Offset may be zero
			mOffsetInWindow[1] += mMiniKeyboardOffsetY; // Offset may be zero
			int[] mWindowLocation = new int[2];
			getLocationOnScreen(mWindowLocation);
			mWindowY = mWindowLocation[1];
		}
		
		mPopupPreviewX += mOffsetInWindow[0];
		mPopupPreviewY += mOffsetInWindow[1];
		
		// If the popup cannot be shown above the key, put it on the side
		if (mPopupPreviewY + mWindowY < 0) {
			// If the key you're pressing is on the left side of the keyboard, show the popup on
			// the right, offset by enough to see at least one key to the left/right.
			if (key.x + key.width <= getWidth() / 2) {
				mPopupPreviewX += (int) (key.width * 2.5);
			} else {
				mPopupPreviewX -= (int) (key.width * 2.5);
			}
			mPopupPreviewY += popupHeight;
		}
			
		if(mPopupPreviewX + popupWidth > getWidth()) {
			int extraWidth = (mPopupPreviewX + popupWidth) - getWidth();
			mPopupPreviewX = mPopupPreviewX - extraWidth - 3; //instead of leaving on the border leaving 3px
		}
		
		previewText.invalidate();
		// If the key preview has no parent view yet, add it to the ViewGroup which can place
		// key preview absolutely in SoftInputWindow.
		if (previewText.getParent() == null) {
			addKeyPreview(previewText);
		}

		mDrawingHandler.cancelDismissKeyPreview(tracker);

		previewText.setLayoutParams(popupWidth, popupHeight);
		//previewText.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ViewLayoutUtils.placeViewAt(previewText, mPopupPreviewX, mPopupPreviewY, popupWidth, popupHeight);
		previewText.requestLayout();
		previewText.invalidate();
		previewText.setVisibility(VISIBLE);
	}

	private Drawable setIconForTextView(final Key key, final PreviewTextCustomView previewText) {
		Drawable drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
		float previewTextSize;
		if (key.codes[0] == KPTConstants.KEYCODE_PHONE_MODE_SYM
				|| key.codes[0] == KPTConstants.KEYCODE_MODE_CHANGE
				|| key.codes[0] == KPTConstants.KEYCODE_JUMP_TO_TERTIARY) {
			
			previewText.setBackgroundDrawable(null);
			previewText.drawText(mAdaptxtKeyLabel);

			if (mKeyboardSwitcher != null  
					&& mKeyboardSwitcher.isCurrentKeyboardModeQwerty()) {
				 previewTextSize = fitTextInPreview(mAdaptxtKeyLabel,
						key.width + previewText.getPaddingLeft()
						+ previewText.getPaddingRight());
				 previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP,previewTextSize);
			} else {
				previewText.setTextSize(mKeyTextSize3);
			}
		} else {
			if(key.codes[0] == KPTConstants.KEYCODE_SPACE ) {
				previewText.setBackgroundDrawable(key.iconPreview != null ? key.iconPreview : key.icon);
				previewText.drawText("");
				mLanguageSwitcher.mIme.mHandler.removeMessages(AdaptxtIME.MSG_SPACE_LONGPRESS);
			}
			else{
				/* Drawing the previews for single tap keys. 
				 * Commenting the last two lines as no icon preview is used.
				 * */
				String[] iconTextArr = getStringsForIcons(key);
				if(iconTextArr != null && iconTextArr.length > 0) {
					String iconText = iconTextArr[0];
					if(iconText != null) {
						previewText.setTypeface(mTheme.getCustomFontTypeface());
						previewText.setTextSize(mKeyTextSize3);
						//Bug fix #11989. Adding HDPI condition too
						if(SCREEN_DENSITY_DPI != DisplayMetrics.DENSITY_LOW &&
								SCREEN_DENSITY_DPI != DisplayMetrics.DENSITY_MEDIUM &&
								SCREEN_DENSITY_DPI != DisplayMetrics.DENSITY_HIGH) {
							previewText.setPadding(0, 0, 0, 20);
						}
						/*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM){
							previewText.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_PRIMARY_CHAR]);
						}else{
							previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPrimaryTextColor));
						}*/
						previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPrimaryTextColor));
						previewText.drawText(iconText);
					}
				}
			}
			/*if (key.codes[0] == KPTConstants.KEYCODE_SPACE
					&& !(mKeyboardSwitcher.isCurrentKeyboardModeQwerty())
					&& key.popupResId != 0) {

				drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
				previewText.setBackgroundDrawable(drawable);
			}*/

			if(key.codes[0]== KPTConstants.KEYCODE_ADAPTXT){  
				final Drawable settingsDrawable = mTheme.getResources().getDrawable(mTheme.mAdaptxtSettingsPreView);
				previewText.setImageDrawable(settingsDrawable);
				previewText.drawText("");
			}

			if(key.codes[0] == KPTConstants.KEYCODE_ENTER ){
				final int imeOption = getIMEOptionForEnterkey();
    			
    			switch (imeOption) {
				case EditorInfo.IME_ACTION_DONE:
					label = this.getResources().getString(R.string.kpt_icontext_enter_done);
					break;
				case EditorInfo.IME_ACTION_GO:
					label = this.getResources().getString(R.string.kpt_icontext_enter_go);
					break;
				case EditorInfo.IME_ACTION_NEXT:
					label = this.getResources().getString(R.string.kpt_icontext_enter_next);
					break;
				case EditorInfo.IME_ACTION_SEND:
					label = this.getResources().getString(R.string.kpt_icontext_enter_send);
					break;
				case EditorInfo.IME_ACTION_SEARCH:
					label = this.getResources().getString(R.string.kpt_icontext_enter_search);
					break;
				default:
					label = this.getResources().getString(R.string.kpt_icontext_enter_default);
					break;
				}
    			}
			// For "mic" and "post-man" keys  has icons but still should display printable chars(Like "."  and ",") on single tap
			if(key.codes[0]== KPTConstants.KEYCODE_MIC 
					|| key.codes[0]== KPTConstants.KEYCODE_PERIOD 
					|| key.codes[0]==KPTConstants.KEYCODE_DANDA 
					|| key.codes[0] == KPTConstants.KEYCODE_ATX) {

				previewText.setBackgroundDrawable(null);
				if (key.codes[0] == KPTConstants.KEYCODE_MIC) {
					previewText.drawText(",");
				} else if(key.codes[0] == KPTConstants.KEYCODE_DANDA) {
					previewText.drawText("|");
				} else if (key.codes[0] == KPTConstants.KEYCODE_ATX) {
					previewText.drawText(key.label);
					previewText.setTextSize((int)fitTextInPreview((String)key.label, key.width));
				} else if (key.codes[0] == KPTConstants.KEYCODE_THAI_COMMIT){
					previewText.drawText("\u2713");
				} else {
					previewText.drawText(".");
				}
			}
			
			//locale swicth should not have useExtrenalFont tag, remove in next xml changes
			if(key.codes[0] == KPTConstants.KEYCODE_LOCALE_SWITCH) {
				if(key.label != null && mLanguageSwitcher != null) {
					String label = mLanguageSwitcher.getPrevLangLocaleString().substring(0, 2).toUpperCase();
					previewText.drawText(label);
				}
			} 
		}
		
		previewText.setBackgroundDrawable(drawable);
		return drawable;
	}
	
	
	private int getIMEOptionForEnterkey(){
		// setting to Default action
		int imeOption = EditorInfo.IME_ACTION_SEND;

		if (null != mIMEHandler) {
			if (null != mIMEHandler.mInputView) {
				final AdaptxtEditText adaptxtEditText = mIMEHandler.mInputView.getFocusedEditText();
				if (null != adaptxtEditText) {
					imeOption = adaptxtEditText.getImeOptions() & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);;
				}else{
					//Log.e("KPT", "Strange KeyboardView drawKeyExternalFontLabel's mIMEHandler.mInputView-->adaptxtEditText is NULL");	
				}
			}else{
				//Log.e("KPT", "Strange KeyboardView drawKeyExternalFontLabel's mIMEHandler.mInputView is NULL");
			}
		}else{
			//Log.e("KPT", "Strange KeyboardView drawKeyExternalFontLabel's mIMEHandler is NULL");
		}
		return imeOption;
	}

	private Drawable setTextForTextView(final PointerTracker tracker, final PreviewTextCustomView previewText) {
		final Key key = tracker.getKey();
		Drawable drawable = null;
		float previewTextSize = mKeyTextSize2;
		
		previewText.setTypeface(mTheme.getUserSelectedFontTypeface());
		previewText.setBackgroundDrawable(null);
		
		String inputLanguageLocale = mLanguageSwitcher.getInputLanguageLocale();
		if( indicLanguages.contains(inputLanguageLocale)) {
			previewText.setTextSize(getResources().getDimension(R.dimen.kpt_xhdpi_preview_text_size));
		} else{
			//Bug Fix #11977. This check id for resizing the ATX key in Phone keyboard
			if(key.codes[0] == KPTConstants.KEYCODE_ATX){
				previewTextSize = fitTextInPreview(mAdaptxtKeyLabel,
						key.width + previewText.getPaddingLeft()
						+ previewText.getPaddingRight());
				 previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP,previewTextSize);
			}else{
				previewText.setTextSize(mKeyTextSize3);
			}
		}
		
		if (!(key.codes.length == 1 && key.codes[0] == KPTConstants.KEYCODE_PERIOD)
						|| key.codes[0] == KPTConstants.KEYCODE_ADAPTXT
						|| key.codes[0] == KPTConstants.KEYCODE_MIC
						|| key.codes[0] == KPTConstants.KEYCODE_DANDA
						|| key.codes[0] == KPTConstants.KEYCODE_PHONE_MODE_SYM
						|| key.codes[0] == KPTConstants.KEYCODE_MODE_CHANGE
						|| key.codes[0] == KPTConstants.KEYCODE_JUMP_TO_TERTIARY
						|| key.codes[0] == KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE 
						|| key.codes[0] == KPTConstants.KEYCODE_DELETE) {
			mShouldDisplayAccents = mSharedPreference.getBoolean(KPTConstants.PREF_DISPLAY_ACCENTS, true);
			if (key.popupResId != 0 && mShouldDisplayAccents) {
				drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBgMore);
				previewText.setBackgroundDrawable(drawable);
			} else {
				drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
				previewText.setBackgroundDrawable(drawable);
			}
		}
		
		//Fix for TP# 9300 & TP#11974 -- vamsee
		if(key.codes.length == 1 && key.codes[0] == KPTConstants.KEYCODE_PERIOD) {
			drawable = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
			previewText.setBackgroundDrawable(drawable);
		}
		
		// for TP item 6337
		String previewString = "";
		if(mIsLongPressed && key.label2 != null) {
			previewString = key.label2.toString();
		} else {
			CharSequence adjustedText = getPreviewText(tracker);
			if(adjustedText != null) {
				previewString = adjustedText.toString();
			}
		}
		previewText.drawText(previewString);
		return drawable;
	}

	
	/**
	 * this method returns whether the accented bubble should be displayed or not for the key.
	 * 
	 * even if the "Extended Bubble char" option is off in the settings still show the 
	 * bubble in secondary and tertiary keyboard and for few special keys
	 * 
	 * @param key
	 * @return
	 */
	public boolean shouldDisplayAccentedBubbles(final Key key) {
		boolean shouldDisplayAccents = false;
		mShouldDisplayAccents = mSharedPreference.getBoolean(KPTConstants.PREF_DISPLAY_ACCENTS, true);
		if(mKeyboardSwitcher!= null){
    		if(key.codes[0] == KPTConstants.KEYCODE_PERIOD 
    				|| key.codes[0] == KPTConstants.KEYCODE_AT
    				|| (key.codes.length > 1 && key.codes[1] == KPTConstants.KEYCODE_SMILEY)
    				|| key.codes[0] == KPTConstants.KEYCODE_DANDA){
    			shouldDisplayAccents = true;        			
    		}else{
    			shouldDisplayAccents = mShouldDisplayAccents ? mShouldDisplayAccents :!mKeyboardSwitcher.isAlphabetMode();
    		}
    	}
		return shouldDisplayAccents;
	}
	
	/**
     *  Function to handle the visual feedback images (for displaying separate icons as feedback 
     *  previews(Adaptxt key, postman key,etc..)) for Longpress.
     * @param keyIndex
     */
	protected void setIconAsPreviewOnKeyLongPress(final Key key, final PreviewTextCustomView previewText) {
		Drawable drawable = null;

		switch (key.codes[0]) {
		/*case KPTConstants.KEYCODE_PHONE_MODE_SYM:*/
		case KPTConstants.KEYCODE_MODE_CHANGE:
		case KPTConstants.KEYCODE_JUMP_TO_TERTIARY:
		case KPTConstants.KEYCODE_ADAPTXT:
			if (mTheme != null) {
				drawable = mTheme.getResources().getDrawable(mTheme.mAdaptxtSettingsPreView);
			}
			if (drawable != null) {
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
			}

			/*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
				
				previewText.setThemePrimaryColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_PRIMARY_CHAR]);
				previewText.setThemeSecondaryColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
				previewText.setDrawCustomThemeKeyPreview(true);
				previewText.setBackgroundDrawable(null);
				//if key bg is not changed, then use the normal key drawable
				if(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
					previewText.setBackgroundDrawable(new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]));
					previewText.setShouldDisplayEllipse(false);
				}else{
					Drawable d = getResources().getDrawable(R.drawable.kpt_keyboard_key_feedback_background_val); //mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
					previewText.setBackgroundDrawable(d);
					
					previewText.setShouldDisplayEllipse(false);
				}
			} else {*/
				Drawable d = mTheme.getResources().getDrawable(mTheme.mKeyFeedbackBg);
				previewText.setBackgroundDrawable(d);
			//}
			
			final Drawable settingsDrawable = mTheme.getResources().getDrawable(mTheme.mAdaptxtSettingsPreView);
			previewText.setImageDrawable(settingsDrawable);
			
			break;
		//case KPTConstants.KEYCODE_ENTER:
		case KPTConstants.KEYCODE_PERIOD:
		case KPTConstants.KEYCODE_DANDA:
		case KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE:
		case KPTConstants.KEYCODE_MIC:
		case KPTConstants.KEYCODE_THAI_COMMIT:
			String[] iconText = getStringsForIcons(key);
			if(iconText != null && iconText.length > 1) {
				if(key.codes[0] != KPTConstants.KEYCODE_MIC && 
						SCREEN_DENSITY_DPI != DisplayMetrics.DENSITY_LOW &&
						SCREEN_DENSITY_DPI != DisplayMetrics.DENSITY_MEDIUM) {
					previewText.setPadding(0, 0, 0, 20);
				}
				if (mTheme != null) {
					previewText.setTypeface(mTheme.getCustomFontTypeface());
					/*if (mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
						previewText.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
					}else{
						previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));
					}*/
				}
				previewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));
				previewText.setTextSize(mKeyTextSize3);
				previewText.drawText(iconText[1]);
			}
			break;
		default:
			break;
		}
	}
    
    protected void setSecondaryLabelasPreviewOnKeyLongPress(final Key key, final PreviewTextCustomView previewText) {
    	if(previewText.getVisibility()==View.VISIBLE) {
    		if(key.codes[0]== KPTConstants.KEYCODE_PERIOD 
    					&& (mKeyboardSwitcher != null 
    							&& mKeyboardSwitcher.getPhoneKeypadState() != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET )) {
    			previewText.drawText(",");
    		} else if((key.codes[0]== KPTConstants.KEYCODE_ZERO) || 
    				(key.codes[0] == KPTConstants.KEYCODE_NEPALI_MARATHI_ZERO) ||
    				(key.codes[0] == KPTConstants.KEYCODE_BENGALI_ZERO)&& 
    				(mKeyboardSwitcher != null && mKeyboardSwitcher.getPhoneKeypadState() != KPTKeyboardSwitcher.MODE_PHONE_ALPHABET ))	{
    			previewText.drawText("+");
    		} else if(key.codes[0]== KPTConstants.KEYCODE_SPACE && key.label2 == null) {
    			//showKey(NOT_A_KEY);
    		} else{
    			previewText.drawText(adjustCase(key.label2));
    		}
    	}
    }
    
	@Override
	public void dismissKeyPreview(PointerTracker tracker) {
		mDrawingHandler.dismissKeyPreview(mDelayAfterPreview, tracker);
	}

	@Override
	public void showGesturePreviewTrail(PointerTracker tracker, boolean isOldestTracker) {
		locatePreviewPlacerView();
		mPreviewPlacerView.invalidatePointer(tracker, isOldestTracker);
	}
	
	public void showGestureFloatingPreviewText(final String gestureFloatingPreviewText) {
		locatePreviewPlacerView();
		mPreviewPlacerView.setGestureFloatingPreviewText(gestureFloatingPreviewText);
	}

	public void dismissGestureFloatingPreviewText() {
		locatePreviewPlacerView();
		mPreviewPlacerView.dismissGestureFloatingPreviewText();
	}
	
    public void handleAtx(boolean flag) {
    	if (mKeyboard == null) {
			return;
		}
    	
    	if(mLanguageSwitcher != null && mLanguageSwitcher.mIme != null && 
    			mLanguageSwitcher.mIme.mUnknownEditorType == KPTConstants.UNKNOWN_EDITOR_TYPE_HTC_MAIL) {
    		flag = false;
    	}
    	
    	mAtxKey = mKeyboard.getAtxKey();
    	if (mAtxKey != null) {
    		/*if(mDisableATX) {
    			//mAtxKey.on = false;
    		} else {*/
    			if (flag/* && !mDisableATX*/) {
    				mAtxKey.on = true;
    			} else   {
    				if (mDisableATX) {
    					//no need to set the drawable again
    					//mAtxKey.icon = getResources().getDrawable(R.drawable.kpt_btn_keyboard_key_normal_val);
    				}
    				mAtxKey.on = false;
    			}
    		//}
    		invalidateKey(mKeyboard.getAtxKeyIndex());
    	}
    }

    protected PreviewTextCustomView getKeyPreviewText(final int pointerId) {
		PreviewTextCustomView previewText = mKeyPreviewTexts.get(pointerId);
		if (previewText != null) {
			return previewText;
		}
		final Context context = getContext();
		/*if (mKeyPreviewLayoutId != 0) {
			previewText = (TextView)LayoutInflater.from(context).inflate(mKeyPreviewLayoutId, null);
		} else {
			previewText = new TextView(context);
		}*/
		
		PreviewTextCustomView customPreview = new PreviewTextCustomView(context);
		//customPreview.setBackground(context.getResources().getDrawable(R.drawable.kpt_keyboard_key_feedback_background_kt));
		//previewText = customPreview;
		
		mKeyPreviewTexts.put(pointerId, customPreview);
		return customPreview;
	}
    
	public void cancelAllMessages() {
		mDrawingHandler.cancelAllMessages();
	}
	
    
    public void closing() {
    	removeMessages();
    	mBuffer = null;
    	mCanvas = null;
    }

    private void removeMessages() {
    	mIsLongPressed = false;
    	if(mLanguageSwitcher != null)
    		mLanguageSwitcher.mIme.mHandler.removeMessages(AdaptxtIME.MSG_SPACE_LONGPRESS);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
        mPreviewPlacerView.removeAllViews();
    }

    public void setPopupKeyboardStatus(final boolean popupStatus) {
    	mIsPopupKeyboardOnScreen = popupStatus;
    }
    
    public boolean isPopupKeyboardShown() {
    	return mIsPopupKeyboardOnScreen;
    }

    public 	void setLanguageSwitcher(KPTLanguageSwitcher languageSwitcher) {
		mLanguageSwitcher = languageSwitcher;
	}
	
    protected KPTLanguageSwitcher getLanguageSwitcher() {
		return mLanguageSwitcher;
	}
    
    public void setKeyboardSwitcher(KPTKeyboardSwitcher keyboardSwitcher) {
		mKeyboardSwitcher = keyboardSwitcher;
	}
	
    protected KPTKeyboardSwitcher getKeyboardSwitcher() {
    	return mKeyboardSwitcher;
    }
    
    public void handleKeysFromHardkeyArrows(int keyIndex) {
    	int oldKeyIndex = mCurrentKeyIndex;
    	mCurrentKeyIndex = keyIndex;

    	// Release the old key and press the new key
    	final Key[] keys = mKeys;
    	if (oldKeyIndex != mCurrentKeyIndex) {
    		if (oldKeyIndex != NOT_A_KEY && keys.length > oldKeyIndex) {
    			keys[oldKeyIndex].onReleased();
    			invalidateKey(oldKeyIndex);
    		}
    		if (mCurrentKeyIndex != NOT_A_KEY && keys.length > mCurrentKeyIndex) {
    			keys[mCurrentKeyIndex].onPressed();
    			invalidateKey(mCurrentKeyIndex);
    		}
    	}
    }
    
	/**
	 * Returns the currently pressed key's index
	 * @return Index of pressed key. NOT_A_KEY if no valid key is pressed.
	 */
	public int getCurrentKeyIndex() {
		return mCurrentKeyIndex;
	}
	
	public void setCurrentKeyIndex(int currentIndex) {
		mCurrentKeyIndex = currentIndex;
	}
	
	/**
	 * Returns the currently pressed key's index
	 * @return Index of pressed key. NOT_A_KEY if no valid key is pressed.
	 */
	public Key getCurrentKey(){
		return mCurrentkeyPressed;
	}
	
	protected void setCurrentKey(final Key key) {
		mCurrentkeyPressed = key;
	}

	//Used to calculate the text size based on the width of the preview. Ensures to fit the text in popup window(only for 123# & ABC)
    public float fitTextInPreview(String label, int width) {
    	if (TextUtils.isEmpty(label)) {
			return -1;
		}
    	Paint paint = new Paint();
    	//min and max text size is taken to support ldpi and xhdpi devices.
    	int min = (int) this.getResources().getDimension(R.dimen.kpt_min_textpreview_size);
    	int max = (int) this.getResources().getDimension(R.dimen.kpt_max_textpreview_size);
    	
    	int treshold = (int) Math.max(1, SCREEN_DENSITY);
    	for (int i = min; i <= max; i++) {
    		paint.setTextSize(i);
    		if(paint.measureText(label, 0, label.length()) >= (width - 6 * treshold)) { 
    			if(SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_LOW) {
    				return i/SCALED_DENSITY ;
    			}else{
    				return i/SCALED_DENSITY - 5;
    			}
    		}
    	}
    	return -1;
    }
    
	public void themeChanged() {
		invalidateAllKeys();
		changeKeyboardBG();
	}
	
	protected void toogle12KeySymbolslayout(boolean state){
		if (mKeyboardSwitcher != null) {
			mKeyboardSwitcher.changeSymbols(state);	
		}
	}
	
	//	<-------BELOW ALL ARE CUSTOMIZATION RELATED CHANGES ------------------------->
	
	/**
	 * When the keyboard is set check for keyboard customization related changes. Since few 
	 * customizations are specific to languages and orientations, performing this in setkeyboard
	 * 
	 * @param keyboard
	 * @param keys
	 *//*
	private void applyAnyKeyboardCustomizationChanges(Keyboard keyboard, List<Key> keys) {
		if(mLanguageSwitcher == null) {
			return;
		}
		
		*//**
		 * check for swap related customizations
		 *//*
		int orientation = this.getResources().getConfiguration().orientation;
		if(!mIsPopupKeyboardOnScreen) {
        	List<KeysInfo> deserializeList = deserializeSwapKeysList(mLanguageSwitcher.getCurrentLanguageDisplayName());
        	int keysSize;
        	if( mSharedPreference.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false)){
        		keysSize = keys.size()-1;
        	}else{
        		keysSize = keys.size();
        	}
        	
        	if(deserializeList != null && deserializeList.size() == keysSize) {

        		for (int i = 0; i < deserializeList.size(); i++) {
					KeysInfo kinfo = deserializeList.get(i);
					int actualKeyPosition = kinfo.actualListPosition;
					int locS = i;
					int locE = 0;
					
					*//**
					 * based on the swaped key, swap the x, y and widths. instead of 
					 * swapping the whole widths. 
					 * 
					 * TP 10710 : Also swapping widths because for few languages like arabic 
					 * few keys have different widths 
					 *//*
					for (int j = 0; j < keys.size(); j++) {
						Key k = keys.get(j);
						int pos = k.actualKeyPosition;
						if(pos == actualKeyPosition) {
							
							Key k1 = keys.get(i);
							Key k2 = keys.get(j);
							
							int x1 = k1.x;
							int y1 = k1.y;
							int edgeFlag1 = k1.edgeFlags;
							int rownNum1 = k1.rowNumber;
							int width1 = k1.width;
							int height1 = k1.height;
							int keyIndexInRow1 = k1.keyIndexInRow;
							
							int x2 = k2.x;
							int y2 = k2.y;
							int edgeFlag2 = k2.edgeFlags;
							int rownNum2 = k2.rowNumber;
							int width2 = k2.width;
							int height2 = k2.height;
							int keyIndexInRow2 = k2.keyIndexInRow;
							
							k1.x = x2;
							k1.y = y2;
							k1.edgeFlags = edgeFlag2;
							k1.rowNumber = rownNum2;
							k1.width = width2;
							k1.height = height2;
							k1.keyIndexInRow = keyIndexInRow2;
							
							k2.x = x1;
							k2.y = y1;
							k2.edgeFlags = edgeFlag1;
							k2.rowNumber = rownNum1;
							k2.width = width1;
							k2.height = height1;
							k2.keyIndexInRow = keyIndexInRow1;
							
							locE = j;
							break;
						}
					}
					Collections.swap(keys,locS,locE);
				}

        		//Collections.sort(keys);
        		mKeys = keys.toArray(new Key[keys.size()]);
        		keyboard.updateNearaestKeys(mKeys);
        		
        		if(orientation == Configuration.ORIENTATION_PORTRAIT && !AdaptxtIME.mIsThaiLanguage) {
        			int height = mSharedPreference.
        					getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, (int) this.getResources().getDimension(R.dimen.key_height_dk_fk));
        			final int keyCount = mKeys.length;
        			for (int i = 0; i < keyCount; i++) {
        				mKeys[i].height = height;
        				mKeys[i].y = height * mKeys[i].rowNumber; 
        			}
        		} 
        	}
        }
		
		
		mKeyTextSize = mKeyboard.keyLabelTextSize;
        
     
        
        *//**
         * update the font typeface based on user preference. 
         * remove font style for vietnamese language coz few characters are not rendered properly
         *//*
        if (mTheme != null) {
        	if(!mLanguageSwitcher.isLatinScript() || "vi-VN".equalsIgnoreCase(mLanguageSwitcher.getInputLanguageLocale())) {
        		mTheme.setUserSelectedFontTypeface(null);
        	}	else {
        		mTheme.setUserSelectedFontTypeface(mSharedPreference.getString(KPTConstants.PREF_CUST_FONT_STYLE, KPTAdaptxtTheme.FONT_DEFAULT));
        	}
        	updateFontStyle();

        	updateCustomKeyShape();
        	

        	if( mIsPopupKeyboardOnScreen && mTheme.getCurrentThemeType() != KPTConstants.THEME_CUSTOM){ 
        		changePopupBG();
        	}else{
        		changeKeyboardBG();
        	}
        }
        
        if(!KPTAdaptxtIME.mIsThaiLanguage) {
        	int customHeight;
            if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            	customHeight  = mSharedPreference.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, 
            			(int) getContext().getResources().getDimension(R.dimen.key_height_dk_fk));
            }else {
            	customHeight  = mSharedPreference.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL_LANDSCAPE, 
            			(int) getContext().getResources().getDimension(R.dimen.key_height_land));
            }
            if(customHeight != mPreviewHeight) {
        		mPreviewHeight = customHeight;
        	}
        } else {
        	//50 is the default value set in input.xml layout file
        	mPreviewHeight = (int) (50 * Math.max(this.getResources().getDisplayMetrics().density, 1));
        }
        
        invalidateAllKeys();
	}*/
	
	/**
	 * this is called when user has changes the keyheight values and selected save 
	 * option
	 */
	public void keyBoardHeightChanged(int savedKeyHeight) {
		if (mKeyboard == null) {
			return;
		}
		mOffsetInWindow = null;
		//apply the increased key height to key preview also
		if(savedKeyHeight != mPreviewHeight) {
			mPreviewHeight = savedKeyHeight;
		}
		List<Key> keys = mKeyboard.getKeys();
		mKeys = keys.toArray(new Key[keys.size()]);
		computeProximityThreshold(mKeyboard);
	}
	
	/**
	 * Apply the keyshape based on the user preference
	 */
	/*public void updateCustomKeyShape() {
		if(mTheme != null && mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
			//check if user has set any keyshapre, if true create the shape accordingly
			int keyshape = mTheme.getCurrentCustomThemeKeyShape();
			if(keyshape != -1) {
				EXTRA_PADDING_CUSTOM_KEYSHAPE = 3;
				mShouldCustomKeyShapeDrawn = true;
				createCustomKeyShape(keyshape);
			} else {
				EXTRA_PADDING_CUSTOM_KEYSHAPE = 0;
				mShouldCustomKeyShapeDrawn = false;
				mTheme.setSelectedCustomKeyShape(null);
			}
		}
	}*/
	
	/**
	 * when the current theme is moved from custom theme to default theme reset the values that
	 * are set by custom theme
	 */
	public void resetValuesToDefaultTheme() {
		EXTRA_PADDING_CUSTOM_KEYSHAPE = 0;
		mShouldCustomKeyShapeDrawn = false;
	}
	
	/**
	 * create the custom key shapre that the user has selected
	 * @param keyShape
	 */
	/*private void createCustomKeyShape(final int keyShape) {
		if (mTheme == null) {
			return;
		}
		mShouldCustomKeyShapeDrawn = true;
		mCustomKeyShapeDrawable.setColor(mTheme.getResources().getColor(mTheme.mCustomKeyShapeColor));
		final int shapeRadius;
		//TP 10646
		if(SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_LOW) {
			if(keyShape == KPTAdaptxtTheme.KEY_SHAPE_5) {
				shapeRadius = 4;
			} else {
				shapeRadius = 12;
			}
		} else if(SCREEN_DENSITY_DPI == DisplayMetrics.DENSITY_MEDIUM) {
			if(keyShape == KPTAdaptxtTheme.KEY_SHAPE_5) {
				shapeRadius = 8;
			} else {
				shapeRadius = 16;
			}
		} else {
			if(keyShape == KPTAdaptxtTheme.KEY_SHAPE_5) {
				shapeRadius = 12;
			} else {
				shapeRadius = 24;
			}
		}

		switch (keyShape) {
		case KPTAdaptxtTheme.KEY_SHAPE_1:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.RECTANGLE);
			setCornerRadii(mCustomKeyShapeDrawable, shapeRadius, shapeRadius,
					shapeRadius, shapeRadius);
			break;
		case KPTAdaptxtTheme.KEY_SHAPE_2:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.RECTANGLE);
			setCornerRadii(mCustomKeyShapeDrawable, shapeRadius, 0, shapeRadius, 0);
			break;
		case KPTAdaptxtTheme.KEY_SHAPE_3:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.RECTANGLE);
			setCornerRadii(mCustomKeyShapeDrawable, 0, shapeRadius, 0, shapeRadius);
			break;
		case KPTAdaptxtTheme.KEY_SHAPE_4:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.RECTANGLE);
			setCornerRadii(mCustomKeyShapeDrawable, 0, 0, 0, 0);
			break;
		case KPTAdaptxtTheme.KEY_SHAPE_5:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.RECTANGLE);
			setCornerRadii(mCustomKeyShapeDrawable, shapeRadius, shapeRadius, shapeRadius, shapeRadius);
			break;
		case KPTAdaptxtTheme.KEY_SHAPE_6:
			mCustomKeyShapeDrawable.setShape(GradientDrawable.OVAL);
			break;
		}
		mTheme.setSelectedCustomKeyShape(mCustomKeyShapeDrawable);
	}
	
	private void setCornerRadii(GradientDrawable drawable, float r0,
			float r1, float r2, float r3) {
		drawable.setCornerRadii(new float[] { r0, r0, r1, r1,
			r2, r2, r3, r3 });
	}*/

	/**
	 * apply typeface to paint objects. these are the objects that are used while
	 * drawaing
	 */
	public void updateFontStyle() {
		mPaint.setTypeface(mTheme.getUserSelectedFontTypeface());
	}

	
	
	/**
	*  set the key transpareny selcted by the user through keyboard customization
	*/
	public void setKeyTransparency(int transparency) {
		mTransparency = transparency; 
	}
	
	@SuppressWarnings("deprecation")
	public void changePopupBG(){
		if(mTheme.mPopupKeyBoardBG!=0){
			setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mPopupKeyBoardBG));
		}else{
			setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
		}
	}
	
	/**
     * Changes/sets the selected picture to the Keyboard board as a background.  
     */
    @SuppressWarnings("deprecation")
	public void changeKeyboardBG(){
    	if (mTheme == null) {
			return;
		}
    	/*int bgPreference = mTheme.getmCustomBGPreffs();
    	String pictureBitmapBGPath;
    	// Get the updated crop picture background bitimap path from database/adaptxt theme class.
    	if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    		pictureBitmapBGPath = mTheme.getCustomBGPath(0);
    		if(pictureBitmapBGPath == null || pictureBitmapBGPath.equals("null")){
    			setKeyTransparency(255);
    		}
    	}else{
    		pictureBitmapBGPath = mTheme.getCustomBGPath(1);
    		if(pictureBitmapBGPath == null || pictureBitmapBGPath.equals("null")){
    			setKeyTransparency(255);
    		}
    	}*/
    	/*if(bgPreference == 0){
    		if(pictureBitmapBGPath != null){
    			File f = new File(pictureBitmapBGPath);
    			//Check if that file exists in SD card or not
    			if(f.exists()){
    				setBackgroundDrawable(Drawable.createFromPath(pictureBitmapBGPath));
    			} else{
    				if((mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) && 
    						mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
    					setBackgroundResource(0);
    					this.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]);
    				}else{
    					this.setBackgroundColor(0);
    					if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    						setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
    					}else{
    						setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
    					//}
    				}
    			}
    		}else if((mTheme.isCustomizationInProgress || mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM)
    				&& mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
    			//setBackgroundResource(0);
    			this.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]);
    		}else{
    			this.setBackgroundColor(0);
    			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    				setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
    			}else{
    				setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
    			}
    		}
    		//        	mMiniKeyboardCache.clear();

    	}else if(bgPreference == 1){
    		if((mTheme.isCustomizationInProgress || mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM)
    				&& mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
    			//setBackgroundResource(0);
    			this.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]);
    		}else if(pictureBitmapBGPath != null){
    			File f = new File(pictureBitmapBGPath);
    			//Check if that file exists in SD card or not
    			if(f.exists()){
    				setBackgroundDrawable(Drawable.createFromPath(pictureBitmapBGPath));
    			} else{
    				if((mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) && 
    						mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG] != -1){
    					setBackgroundResource(0);
    					this.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]);
    				}else{
    					this.setBackgroundColor(0);
    					if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    						setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
    					}else{
    						setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
    					}
    				}
    			}
    		}else{
    			this.setBackgroundColor(0);
    			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    				setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
    			}else{
    				setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
    			}
    		}
    		//    		mMiniKeyboardCache.clear();

    	}else{*/
    		this.setBackgroundColor(0);
    		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
    			setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGLand));
    		}else{
    			setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyboardBGPort));
    		}
    	//}
    }
    
	/**
	 * deserialize the swapped keys data.
	 * 
	 * @param lang
	 * @return
	 *//*
	private List<KeysInfo> deserializeSwapKeysList(String lang) {
		try {
			String serPath = getContext().getFilesDir() + "/" + lang + KPTAdaptxtActivityKBCustomization.SER_FILE_NAME;
			File file = new File(serPath);
			if(!file.exists()) {
				return null;
			}
			
			////Log.e("KPT","de ser path = " + serPath);
			FileInputStream fis = new FileInputStream(serPath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			List<KeysInfo> deserializedList = (List<KeysInfo>) ois.readObject();
			ois.close();

			Set<String> languages = deserializeMap.keySet();
			Iterator<String> langIterator = languages.iterator();
			if(mLanguageSwitcher != null) {
				//Log.e("KPT","Current input locale = " + mLanguageSwitcher.getInputLanguageLocale());
				while (langIterator.hasNext()) {
					String langugeList = langIterator.next();
					//Log.e("KPT","input view deserialize Locale = " + langugeList);
					Map<Integer, KeysInfo> mapData = deserializeMap.get(langugeList);
					Iterator<KeysInfo> values = mapData.values().iterator();
					//Log.e("KPT"," serialized keys count = " + mapData.size());
					while (values.hasNext()) {
					KeysInfo info = values.next();
					//Log.e("KPT","X = " + info.x +" Y = " + info.y + " label = " + info.actualListPosition + " code = ");
				}
				}
			}
			
			return deserializedList;
		} catch (FileNotFoundException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();
		} catch (ClassNotFoundException e) {e.printStackTrace();
		}
		
		return null;
	}*/
	
	public void updateTracePathColor() {
		mPreviewPlacerView.updateTracePathColor(mTheme);
	}
	
	public static boolean mSpaceSwypeStart = false;
	private int mSpaceDragLastDiff;
	public static SlidingLocaleDrawable mSlidingLocaleIcon;
	
	public void isInside(PointerTracker tracker, int x, int y, int mSpaceDragStartX) {
		
		y += KPTAdpatxtKeyboard.sSpacebarVerticalCorrection;
		int diff = x - mSpaceDragStartX;
		if(Math.abs(diff) > mSpaceKey.width * KPTAdpatxtKeyboard.SPACEBAR_DRAG_THRESHOLD) {
			mSpaceSwypeStart = true;
		}
		
		if (Math.abs(diff - mSpaceDragLastDiff) > 0) {
			updateLocaleDrag(tracker, diff);
			displaySpacePopup(tracker);
		}
		mSpaceDragLastDiff = diff;
	}
	
	private void updateLocaleDrag(PointerTracker tracker, int diff) {
		Drawable mSpacePreviewIcon = getContext().getResources().getDrawable(R.drawable.kpt_sym_keyboard_feedback_space);
		if(mSpaceKey == null) {
			return;
		}
		
    	if (mSlidingLocaleIcon == null) {
    		mSlidingLocaleIcon = new SlidingLocaleDrawable(mSpacePreviewIcon, mSpaceKey.width, mSpaceKey.height);
    		mSlidingLocaleIcon.setBounds(0, 0, mSpaceKey.width, mSpaceKey.height);
    		mSpaceKey.iconPreview = mSlidingLocaleIcon;
    	}
    	
    	mSlidingLocaleIcon.mWidth = mSpaceKey.width;
    	mSlidingLocaleIcon.mBackground = mSpacePreviewIcon;
    	mSlidingLocaleIcon.setBounds(0, 0, mSpaceKey.width, mSpaceKey.height);
    	mSlidingLocaleIcon.setTheme(mTheme);
    	
    	 if(mLanguageSwitcher != null && mKeyboardSwitcher != null 
         		&& (mKeyboardSwitcher.isCurrentKeyboardModeQwerty() || mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET)) {
    		
    		mSlidingLocaleIcon.setDiff(diff, tracker);
    		if (Math.abs(diff) == Integer.MAX_VALUE) {
    			mSpaceKey.iconPreview = mSpacePreviewIcon;
    		} else {
    			mSpaceKey.iconPreview = mSlidingLocaleIcon;
    		}
    		mSpaceKey.iconPreview.invalidateSelf();
    	}
    }
	
	public void changeLanguage(final PointerTracker tracker) {
		int languageDirection = getLanguageChangeDirection();
		//mSpaceSwypeStart = false;
		mSpaceDragLastDiff = 0;
		if (languageDirection != 0) {
			tracker.changeLanguageFromSpaceKey(languageDirection);
		}
	}
	
	private int getLanguageChangeDirection() {
    	if (mSpaceKey == null 
    			|| mLanguageSwitcher.getLocaleCount() < 2 
    			|| !mSpaceSwypeStart 
    			|| Math.abs(mSpaceDragLastDiff) < mSpaceKey.width * KPTAdpatxtKeyboard.SPACEBAR_DRAG_THRESHOLD ) {
            return 0; // No change
        }
        //to support drag on space to change language.
    	if(mLanguageSwitcher != null && mKeyboardSwitcher != null 
         		&& (mKeyboardSwitcher.isCurrentKeyboardModeQwerty() || mKeyboardSwitcher.getPhoneKeypadState() == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET)) {
    		return mSpaceDragLastDiff > 0 ? 1 : -1;
    	}
    	else {
    		return 0;
    	}
    }
	
	/**
     * Animation to be displayed on the spacebar preview popup when switching 
     * languages by swiping the spacebar. It draws the current, previous and
     * next languages and moves them by the delta of touch movement on the spacebar.
     */
    class SlidingLocaleDrawable extends Drawable {

        private int mWidth;
        private int mHeight;
        private Drawable mBackground;
        private int mDiff;
        private TextPaint mTextPaint;
        private int mMiddleX;
        private boolean mHitThreshold;
        private int     mThreshold;
        private String mCurrentLanguage;
        private String mNextLanguage;
        private String mPrevLanguage;
        private float mSpaceArrows;
        private Resources mRes;
        private PointerTracker mTracker;
		private KPTAdaptxtTheme mKPTTheme;
        
        public SlidingLocaleDrawable(Drawable background, int width, int height) {
            mBackground = background;
            Context mContext = getContext();
            mRes = mContext.getResources();
            
            int customHeight = PreferenceManager.getDefaultSharedPreferences(mContext).
            getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, (int) mContext.getResources().getDimension(R.dimen.kpt_key_height_dk_fk));
            
            if(height != customHeight){
            	height = customHeight;	
            }
            mBackground.setBounds(0, height/2,
                    mBackground.getIntrinsicWidth(), mBackground.getIntrinsicHeight()+ height/2);
            mWidth = width;
            mHeight = height;
            mTextPaint = new TextPaint();
            mTextPaint.setTextSize(mContext.getResources().getDimensionPixelSize(
            		R.dimen.kpt_spacebar_text_size));
            mTextPaint.setColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));
            mTextPaint.setTextAlign(Align.CENTER);
            mTextPaint.setAntiAlias(true);
            mThreshold = ViewConfiguration.get(mContext).getScaledTouchSlop();
            
            mSpaceArrows = mContext.getResources().getDimension(R.dimen.kpt_draw_space_key_arrows);
            setWillNotDraw(false);
        }

        public void setTheme(KPTAdaptxtTheme theme) {
        	mKPTTheme = theme;
        }
        
        void setDiff(int diff, PointerTracker tracker) {
            if (diff == Integer.MAX_VALUE) {
                mHitThreshold = false;
                mCurrentLanguage = null;
                return;
            }
            
            mTracker = tracker;
            mDiff = diff;
            mBackground.setBounds(0, mHeight/2,
                    mBackground.getIntrinsicWidth(), mBackground.getIntrinsicHeight()+ mHeight/2);
            mMiddleX = (mWidth - mBackground.getIntrinsicWidth()) / 2;
            if (mDiff > mWidth) mDiff = mWidth;
            if (mDiff < -mWidth) mDiff = -mWidth;
            if (Math.abs(mDiff) > mThreshold) mHitThreshold = true;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
        	if (mTheme == null) {
        		return;
        	}
        	canvas.save();

        	if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
        		mTextPaint.setColor(mKPTTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
        	} else {
        		mTextPaint.setColor(mKPTTheme.getResources().getColor(mKPTTheme.mPreviewTextColor));
        	}

        	mTextPaint.setTextSize(mRes.getDimensionPixelSize(R.dimen.kpt_spacebar_text_size));
        	
        	if (mHitThreshold) {
        		mTracker.setSlidingLocaleStart(true);
        		canvas.clipRect(0, 0, mWidth, mHeight);

        		if (mLanguageSwitcher.getLocaleCount() > 1) {
        			mTextPaint.setTypeface(Typeface.DEFAULT);
        			mCurrentLanguage = getInputLanguage(mWidth, mTextPaint);
        			mNextLanguage = getNextInputLanguage(mWidth, mTextPaint);
        			mPrevLanguage = getPrevInputLanguage(mWidth, mTextPaint);
        			canvas.drawText(mCurrentLanguage,
        					mWidth / 2 + mDiff , mHeight/2, mTextPaint); 
        			canvas.drawText(mPrevLanguage,
        					mDiff - 36, mHeight/2, mTextPaint);
        			canvas.drawText(mNextLanguage,
        					mDiff + mWidth+36,mHeight/2 ,  mTextPaint);

        			mTextPaint.setTextSize(mRes.getDimension(R.dimen.kpt_keytextsize));
        			mTextPaint.setTypeface(mKPTTheme.getCustomFontTypeface());
        			if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM){
        				mTextPaint.setColor(mKPTTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_PRIMARY_CHAR]);
        			}else{
        				mTextPaint.setColor(mKPTTheme.getResources().getColor(mKPTTheme.mPreviewTextColor));
        			}
        			canvas.drawText(mRes.getString(R.string.kpt_icontext_space_chevron_left), 
        					(mSpaceArrows/2) /*mButtonArrowRightIcon.getIntrinsicWidth() / 2*/,  
        					(float) ((mHeight * 0.50)), mTextPaint);

        			canvas.drawText(mRes.getString(R.string.kpt_icontext_space_chevron_right), 
        					mWidth - mSpaceArrows/2 /*mWidth - mButtonArrowRightIcon.getIntrinsicWidth() / 2*/,
        					(float) ((mHeight * 0.50)), mTextPaint);
        		}
        	}
        	if (mBackground != null) {
        		canvas.translate(mMiddleX, 0);
        	}
        	canvas.restore();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
        
        @Override
        public void setAlpha(int alpha) { }
        
        @Override
        public void setColorFilter(ColorFilter cf) { }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }
    }
    
    private String getInputLanguage(int widthAvail, Paint paint) {
    	return mLanguageSwitcher.getCurrentLanguageDisplayName();
    }

    private String getNextInputLanguage(int widthAvail, Paint paint) {
    	return mLanguageSwitcher.getNextLanguageDisplayName();
    }

    private String getPrevInputLanguage(int widthAvail, Paint paint) {
    	return mLanguageSwitcher.getPrevLanguageDisplayName();
    }
    
   
}
