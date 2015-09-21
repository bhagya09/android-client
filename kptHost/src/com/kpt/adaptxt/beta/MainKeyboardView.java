package com.kpt.adaptxt.beta;

import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.keyboard.KeyDetector;
import com.kpt.adaptxt.beta.keyboard.KeyboardActionListener;
import com.kpt.adaptxt.beta.keyboard.MoreKeysKeyboardView;
import com.kpt.adaptxt.beta.keyboard.MoreKeysPanel;
import com.kpt.adaptxt.beta.keyboard.PointerTracker;
import com.kpt.adaptxt.beta.keyboard.PointerTracker.DrawingProxy;
import com.kpt.adaptxt.beta.keyboard.PointerTracker.KeyEventHandler;
import com.kpt.adaptxt.beta.keyboard.PointerTracker.KeyTimerController;
import com.kpt.adaptxt.beta.keyboard.PreviewTextCustomView;
import com.kpt.adaptxt.beta.keyboard.StaticInnerHandlerWrapper;
import com.kpt.adaptxt.beta.keyboard.SuddenJumpingTouchEventHandler;
import com.kpt.adaptxt.beta.settings.KPTAddonManager;
import com.kpt.adaptxt.beta.settings.KPTIMELanguageSelectionDialog;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;

public class MainKeyboardView extends KeyboardView implements  SuddenJumpingTouchEventHandler.ProcessMotionEvent,
PointerTracker.KeyEventHandler {
	
	private static final String TAG = MainKeyboardView.class.getSimpleName();
	private KeyboardActionListener mKeyboardActionListener;
	private boolean mHasDistinctMultitouch;
	private final SuddenJumpingTouchEventHandler mTouchScreenRegulator;
	private int mOldPointerCount = 1;
	private Key mOldKey;
	private KeyTimerHandler mKeyTimerHandler;

	private KeyDetector mKeyDetector;

	/**
	 *  Object for Language Selection Dialog  
	 */
	private KPTIMELanguageSelectionDialog mLangSelectionDialog; 
	
	static final int KEYCODE_OPTIONS = -100;
	static final int KEYCODE_SHIFT_LONGPRESS = -101;
	public static final int KEYCODE_VOICE = -102;
	static final int KEYCODE_F1 = -103;
	static final int KEYCODE_DELETE_LONGPRESS = -106;
	public static final int KEYCODE_SPACE_DOUBLE_TAP = -107;
	static final int KEYCODE_AT = 64;

	private PopupWindow mMoreKeysWindow;
	private MoreKeysPanel mMoreKeysPanel;
	private int mMoreKeysPanelPointerTrackerId;
	private final WeakHashMap<Key, MoreKeysPanel> mMoreKeysPanelCache =
			new WeakHashMap<Key, MoreKeysPanel>();
	

	
	private boolean isQWERTYKeyboard = false; 
	
	private int mMiniKeyboardStartX;
	private int mMiniKeyboardStartY;
	private int mOriginX;
	private int mOriginY;
	
	private boolean ignoreOrigin = false;
	private MoreKeysKeyboardView mMoreKeysKeyboardView;
	
	private static int mDeleteLongPressCount = 0;
	
	/**
	 * Current text entry type by the user either can be glide entry or normal text input
	 */
	private int mCurrentTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_NORMAL;
	/**
	 * Previous text entry type done by the user can be glide entry or normal text input
	 */
	private int mPreviousTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_NORMAL;
	
	public static boolean mShowfirstbublechar = false;
	public static boolean isRightHalf = false;
	
	boolean mIsCurrentSDKBelowGB;
	
	private Resources mResources;
	private AdaptxtEditText mEditText;
	
	public MainKeyboardView(Context context, AttributeSet attrs) {
		this(context, attrs, R.styleable.Kpt_KeyboardView_kpt_keyboardViewStyle);
	}

	public MainKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mResources = context.getResources();
		//mKeyboard = new Keyboard(getContext(), R.xml.kbd_qwerty_fk, KPTKeyboardSwitcher.QWERTY_KEYBOARDMODE_NORMAL, false, 0, true);
		
		//mAdaptxtIME = new AdaptxtIME(context);
		
		mHasDistinctMultitouch = context.getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
		//TODO presently we are making fasle
		final boolean needsPhantomSuddenMoveEventHack = false;
		/*Boolean.parseBoolean(ResourceUtils.getDeviceOverrideValue(res,
	                        R.array.phantom_sudden_move_event_device_list, "false"));*/
		

		mKeyDetector = new KeyDetector( context.getResources().getDimension(R.dimen.kpt_config_key_hysteresis_distance),
				context.getResources().getDimension(R.dimen.kpt_config_key_hysteresis_distance_for_sliding_modifier));
		mKeyTimerHandler = new KeyTimerHandler(this);

		mLeftChevronIconText = getResources().getString(R.string.kpt_icontext_space_chevron_left);
	    mRightChevronIconText = getResources().getString(R.string.kpt_icontext_space_chevron_right);
	    	
	    if(Build.VERSION.SDK_INT < 8) {
	    	mIsCurrentSDKBelowGB = true;
	    } else {
	    	mIsCurrentSDKBelowGB = false;
	    }
	    
		mTouchScreenRegulator = new SuddenJumpingTouchEventHandler(getContext(), this);
		PointerTracker.init(mHasDistinctMultitouch, needsPhantomSuddenMoveEventHack);
		PointerTracker.setParameters(context.getResources());
		
	}
	
	/*@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		mAdaptxtIME.setIMEView(this);
		mKeyboard = new Keyboard(getContext(), R.xml.kbd_qwerty_fk, KPTKeyboardSwitcher.QWERTY_KEYBOARDMODE_NORMAL, false, 0, true);
	}*/
	
	/*public void print(){
		Log.e("VMC", "mInputView ---------------- ");
	}*/
	
	public void setEditText(AdaptxtEditText view) {
		//mAdaptxtIME.onStartInput(view, false);
		this.mEditText = view;
	}
	
	public AdaptxtEditText getFocusedEditText() {
		
		return mEditText;
	}
	
	@Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }
	
	@Override
	public boolean onTouchEvent(final MotionEvent me) {
		if (getKeyboard() == null) {
			return false;
		}
		return mTouchScreenRegulator.onTouchEvent(me, true);
	}

	public void setOnKeyboardActionListener(KeyboardActionListener listener) {
		mKeyboardActionListener = listener;
	}

	protected KeyboardActionListener getOnKeyboardActionListener() {
		return mKeyboardActionListener;
	}

	public static final class KeyTimerHandler extends StaticInnerHandlerWrapper<MainKeyboardView>
	implements KeyTimerController{

		private static final int MSG_TYPING_STATE_EXPIRED = 0;
		private static final int MSG_REPEAT_KEY = 1;
		private static final int MSG_LONGPRESS_KEY = 2;
		private static final int MSG_DOUBLE_TAP = 3;
		private static final int MSG_MULTI_TAP = 4;
		private static final int MSG_TOOGLE_12KEY_SYMBOLS_LAYOUT = 5;
		private static final int MSG_START_LONGPRESS_NO_DELAY = 6;

		private final int mKeyRepeatStartTimeout;
		private final int mKeyRepeatInterval;
		private final int mIgnoreAltCodeKeyTimeout;

		// For multi-tap
		private Key mLastSentKey;
		private int mTapCount;
		private long mLastTapTime;

		private Key mLongPressNoDelayKey;
		
		MainKeyboardView outerInstance;
		private static final int MULTITAP_INTERVAL = 800; // milliseconds
		
		public KeyTimerHandler(MainKeyboardView outerInstance) {
			super(outerInstance);
			this.outerInstance = outerInstance;

			Resources resources = outerInstance.getContext().getResources();
			mKeyRepeatStartTimeout = resources.getInteger(R.integer.kpt_config_key_repeat_start_timeout);
			mKeyRepeatInterval = resources.getInteger(R.integer.kpt_config_key_repeat_interval);
			mIgnoreAltCodeKeyTimeout = resources.getInteger(R.integer.kpt_config_ignore_alt_code_key_timeout);

		}

		@Override 
		public void handleMessage(final Message msg) {
			final MainKeyboardView keyboardView = getOuterInstance();
			final PointerTracker tracker = (PointerTracker) msg.obj;
			switch (msg.what) {
			case MSG_TYPING_STATE_EXPIRED:
				// startWhileTypingFadeinAnimation(keyboardView);
				break;
			case MSG_REPEAT_KEY:
				final Key currentKey = tracker.getKey();
				if (currentKey != null && currentKey.codes[0] == msg.arg1) {
					tracker.onRegisterKey(currentKey, true);
					switch (msg.arg1) {
					case KPTConstants.KEYCODE_DELETE:

						tracker.setCurrentKeyState(PointerTracker.KEY_CURRENT_STATE_LONG_PRESS);
						//tracker.insertSecondaryCharOnLongPress();
						
						mDeleteLongPressCount++;
                        Message repeat = obtainMessage(MSG_REPEAT_KEY, currentKey.codes[0], 0, tracker);
                        if(mDeleteLongPressCount <= 1){
                        	sendMessageDelayed(repeat, REPEAT_INTERVAL);
                        }/*else if(mDeleteLongPressCount < 7){
                        	sendMessageDelayed(repeat, REPEAT_INTERVAL/4);
                        }*/else{
                        	sendMessageDelayed(repeat, 0);
                        }
						break;
					default:
						startKeyRepeatTimer(tracker, mKeyRepeatInterval);
						break;
					}
				}
				break;
			case MSG_LONGPRESS_KEY:
				final Key longPressedKey = tracker.getKey();
				//if there is no popup keyboard for this key commit secondary char into editor or else
				//display the mini keyboard. 
				if(longPressedKey != null) {
					//check if accented bubble has to be displayed
					final boolean shouldDisplayAccents = outerInstance.shouldDisplayAccentedBubbles(longPressedKey); 
					if(!shouldDisplayAccents 
							|| (longPressedKey.popupResId == 0 && !tracker.isMiniKeyboardDisplayed())) {
						
						//display either icon text or secondary char as key preview
						final PreviewTextCustomView previewText = outerInstance.getKeyPreviewText(tracker.mPointerId);
						
						if(longPressedKey.icon != null || longPressedKey.useExternalFont) {
                    		outerInstance.setIconAsPreviewOnKeyLongPress(longPressedKey, previewText);
                    	} else {
                    		if(longPressedKey.codes.length > 1) {
                    			outerInstance.setSecondaryLabelasPreviewOnKeyLongPress(longPressedKey, previewText);
                    		}
                    	}
						
						tracker.setCurrentKeyState(PointerTracker.KEY_CURRENT_STATE_LONG_PRESS);
						//commit secondary char into editor
						if(longPressedKey.label2 != null
								|| longPressedKey.codes[0] == KPTConstants.KEYCODE_SPACE){
							outerInstance.setCurrentTextEntryType(KPTConstants.TEXT_ENTRY_TYPE_NORMAL);
							tracker.insertSecondaryCharOnLongPress();
						}
					} else {
						tracker.setCurrentKeyState(PointerTracker.KEY_CURRENT_STATE_MINI_KEYBOARD);
						outerInstance.setCurrentTextEntryType(KPTConstants.TEXT_ENTRY_TYPE_NORMAL);
						keyboardView.openMoreKeysKeyboardIfRequired(tracker.getKey(), tracker);
					}
				}
				
				break;
			/*case MSG_MULTI_TAP:
				final Key current12Key = tracker.getKey();
				if(checkForATxState()) {
					checkMultiTap(tracker.getDownTime(), current12Key, tracker);
					if (mTapCount == -1) {
	                    	mTapCount = 0;
	                    } 
				} else {
					resetMultiTap();
					tracker.setMultiTap(false);
				}
				tracker.setMultiTapCount(mTapCount);
				if (mTapCount == -1) {
					mTapCount = 0;
				}
				break;*/
			case MSG_TOOGLE_12KEY_SYMBOLS_LAYOUT:
				outerInstance.toogle12KeySymbolslayout(true);
				break;
			case MSG_START_LONGPRESS_NO_DELAY:
				if(mLongPressNoDelayKey != null) {
					tracker.setCurrentKeyState(PointerTracker.KEY_CURRENT_STATE_MINI_KEYBOARD);
					outerInstance.setCurrentTextEntryType(KPTConstants.TEXT_ENTRY_TYPE_NORMAL);
					keyboardView.openMoreKeysKeyboardIfRequired(mLongPressNoDelayKey, tracker);
				}
				break;
			}
		}

		private void startKeyRepeatTimer(final PointerTracker tracker, final long delay) {
			final Key key = tracker.getKey();
			if (key == null) {
				return;
			}
			
			final int keyCode = key.codes[0];
			final long delayTime;
			switch (keyCode) {
			case KPTConstants.KEYCODE_DELETE:
				delayTime = DELETE_LONGPRESS_TIMEOUT;
				break;
			default:
				delayTime = delay;
				break;
			}
			sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, key.codes[0], 0, tracker), delayTime);
		}

		public void cancelKeyRepeatTimer() {
			removeMessages(MSG_REPEAT_KEY);
		}

		// TODO: Suppress layout changes in key repeat mode
		public boolean isInKeyRepeat() {
			return hasMessages(MSG_REPEAT_KEY);
		}

		@Override
		public void startTypingStateTimer(Key typedKey) {
			KeyboardView.mCurrentkeyPressed = typedKey;
			if (typedKey.isModifierKey()) {
				return;
			}

			final boolean isTyping = isTypingState();
			removeMessages(MSG_TYPING_STATE_EXPIRED);
			sendMessageDelayed(obtainMessage(MSG_TYPING_STATE_EXPIRED), mIgnoreAltCodeKeyTimeout);
			if (isTyping) {
				return;
			}
		}

		@Override
		public boolean isTypingState() {
			return hasMessages(MSG_TYPING_STATE_EXPIRED);
		}

		@Override
		public void startKeyRepeatTimer(PointerTracker tracker) {
			startKeyRepeatTimer(tracker, mKeyRepeatStartTimeout);
		}

		@Override
		public void startLongPressTimer(PointerTracker tracker) {
			cancelLongPressTimer();
			if (tracker == null) {
				return;
			}	
			
			tracker.setCurrentKeyState(PointerTracker.KEY_CURRENT_STATE_SINGLE_TAP);
			sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, tracker), outerInstance.LONGPRESS_TIMEOUT);
		}

		@Override
		public void startLongPressTimer(int code) {
			// this is for the shift Key according to LIME
		}

		@Override
		public void cancelLongPressTimer() {
			removeMessages(MSG_LONGPRESS_KEY);
		}

		@Override
		public void startDoubleTapTimer() {
			sendMessageDelayed(obtainMessage(MSG_DOUBLE_TAP),ViewConfiguration.getDoubleTapTimeout());
		}

		@Override
		public void cancelDoubleTapTimer() {
			removeMessages(MSG_DOUBLE_TAP);
		}

		@Override
		public boolean isInDoubleTapTimeout() {
			return hasMessages(MSG_DOUBLE_TAP);
		}

		@Override
		public void cancelKeyTimers() {
			cancelKeyRepeatTimer();
			cancelLongPressTimer();
			mDeleteLongPressCount = 0;  //reset the value in action up
		}

		public void cancelAllMessages() {
			cancelKeyTimers();
		}

		@Override
		public void startMultiTapKey(final PointerTracker tracker) {
			if (outerInstance.isQWERTYKeyboard) {
				return;
			}
			//sendMessageDelayed(obtainMessage(MSG_MULTI_TAP, 0, 0, tracker), 0);
			
			final Key current12Key = tracker.getKey();
			if(checkForATxState()) {
				checkMultiTap(tracker.getDownTime(), current12Key, tracker);
				/*if (mTapCount == -1) {
                    	mTapCount = 0;
                    } */
			} else {
				resetMultiTap();
				tracker.setMultiTap(false);
			}
			tracker.setMultiTapCount(mTapCount);
			if (mTapCount == -1) {
				mTapCount = 0;
			}
		}
		
		@Override
		public void toogle12KeySymbolsLayout(PointerTracker pointerTracker) {
			sendMessageDelayed(obtainMessage(MSG_TOOGLE_12KEY_SYMBOLS_LAYOUT, 0, 0, pointerTracker), 0);
			
		}

		private void resetMultiTap() {
			mLastSentKey = null;
			mTapCount = 0;
			mLastTapTime = -1;

		}

		private void checkMultiTap(long eventTime, Key current12Key, PointerTracker tracker) {
			if (current12Key == null) {
				mTapCount =-1;
				return;
			}
			if (current12Key.codes.length > 1) { // Changed from 1 to 2 to support long press of secondary char
				tracker.setMultiTap(true);
				if (eventTime < mLastTapTime + MULTITAP_INTERVAL
						&&  current12Key == mLastSentKey) {
					mTapCount = (mTapCount + 1) % current12Key.codes.length;
					//mLastSentKey = current12Key;
					//return;
				} else {
					mTapCount = -1;
					//mLastSentKey = current12Key;
					//return;
				}
				mLastTapTime = eventTime;
				mLastSentKey = current12Key;
				return;
			}
			if (eventTime > mLastTapTime + MULTITAP_INTERVAL || current12Key != mLastSentKey) {
				tracker.setMultiTap(false);
				resetMultiTap();
			}
		}

		private boolean checkForATxState() {
			if (outerInstance.mDisableATX && outerInstance.mAtxKey != null) {
				outerInstance.mAtxKey.on = false;
			}
			return outerInstance.mAtxKey != null ? !outerInstance.mAtxKey.on : true;
		}

		@Override
		public void showNextSetOfPopupCharacters(int motionEventAction) {
			if(motionEventAction == MotionEvent.ACTION_DOWN) {
				//MainKeyboardView.mShowfirstbublechar = true;
			} else if(motionEventAction == MotionEvent.ACTION_MOVE) {
				outerInstance.changeBubblePage();
			} else {
				//
			}
		}

		@Override
		public void setCurrentTextEntryType(int currentEntryType) {
			outerInstance.setCurrentTextEntryType(currentEntryType);
		}

		@Override
		public void setPreviousTextEntryType(int previousEntryType) {
			outerInstance.setPreviousTextEntryType(previousEntryType);
		}

		@Override
		public void showSpaceLanguageChangeIcon(PointerTracker tracker, int action) {
			//if trial expired don't show the language show icon
			/*if(KPTAdaptxtIME.isTrialExpired && !KPTAdaptxtIME.mIsAtleastOneOpenAdaptxtDictionaryInstalled) {
				return;
			}*/
			
			if(MotionEvent.ACTION_MOVE == action) {
				outerInstance.displaySpacePopup(tracker);
				outerInstance.isInside(tracker, tracker.getLastX(), tracker.getLastY(), tracker.getStartX());
			} else {
				outerInstance.changeLanguage(tracker);
			}
		}

		@Override
		public void startLongPressTimerNoDelay(final PointerTracker tracker, final Key key) {
			//if this is the indic language alt key show a mini keyboard from single tap
			//set long press delay as zero
			if(KeyboardView.indicLanguages.contains(outerInstance.mLanguageSwitcher.getInputLanguageLocale())
            				/*|| KPTAdaptxtIME.mIsThaiLanguage */
            				|| outerInstance.mLanguageSwitcher.getInputLanguageLocale().equalsIgnoreCase("ka-GE")) {
				
				mLongPressNoDelayKey = key;
				PointerTracker.mPhantonMiniKeyboardOnScreen = true;
				MainKeyboardView.mShowfirstbublechar = false;
				cancelLongPressTimer();
				sendMessageDelayed(obtainMessage(MSG_START_LONGPRESS_NO_DELAY, tracker), 0);
			}
		}
	}

	@Override
	public boolean processMotionEvent(MotionEvent me) {
		final boolean nonDistinctMultitouch = !mHasDistinctMultitouch;
		int action;
		if(mIsCurrentSDKBelowGB) {
			action = me.getAction() & MotionEvent.ACTION_MASK;
		} else {
			action = me.getActionMasked();
		}
		final int pointerCount = me.getPointerCount();
		final int oldPointerCount = mOldPointerCount;
		mOldPointerCount = pointerCount;

		// TODO: cleanup this code into a multi-touch to single-touch event converter class?
		// If the device does not have distinct multi-touch support panel, ignore all multi-touch
		// events except a transition from/to single-touch.
		if (nonDistinctMultitouch && pointerCount > 1 && oldPointerCount > 1) {
			return true;
		}

		KeyEventHandler keyEventHandler = this;
		if(mMoreKeysPanel != null) {
			keyEventHandler = mMoreKeysPanel;
		}
		
		final long eventTime = me.getEventTime();
		//final int index = me.getActionIndex();
		int index;
		if(mIsCurrentSDKBelowGB) {
			index = me.getPointerId((action >> MotionEvent.ACTION_POINTER_ID_SHIFT));
		} else {
			index = me.getActionIndex();
		}
		
		final int id = me.getPointerId(index);
		final float x, y;

		if (mMoreKeysPanel != null && id == mMoreKeysPanelPointerTrackerId) {
			x = mMoreKeysPanel.translateX(me.getX(index), !mTouchScreenRegulator.isTouchFromMainKeyboard());
			y = mMoreKeysPanel.translateY(me.getY(index), !mTouchScreenRegulator.isTouchFromMainKeyboard());
		} else {
			x = me.getX(index);
			y = me.getY(index);
		}

		if (mKeyTimerHandler.isInKeyRepeat()) {

			final PointerTracker tracker = PointerTracker.getPointerTracker(id, keyEventHandler);
			// Key repeating timer will be canceled if 2 or more keys are in action, and current
			// event (UP or DOWN) is non-modifier key.
			if (pointerCount > 1 && !tracker.isModifier()) {
				mKeyTimerHandler.cancelKeyRepeatTimer();
			}
			// Up event will pass through.
		}

		// TODO: cleanup this code into a multi-touch to single-touch event converter class?
		// Translate mutli-touch event to single-touch events on the device that has no distinct
		// multi-touch panel.
		if (nonDistinctMultitouch) {
			// Use only main (id=0) pointer tracker.
			final PointerTracker tracker = PointerTracker.getPointerTracker(0, keyEventHandler);
			if (pointerCount == 1 && oldPointerCount == 2) {
				// Multi-touch to single touch transition.
				// Send a down event for the latest pointer if the key is different from the
				// previous key.
				final Key newKey = tracker.getKeyOn((int)x, (int)y);
				if (mOldKey != newKey) {
					tracker.onDownEvent((int)x, (int)y, eventTime, this);
					if (action == MotionEvent.ACTION_UP)
						tracker.onUpEvent((int)x, (int)y, eventTime);
				}
			} else if (pointerCount == 2 && oldPointerCount == 1) {
				// Single-touch to multi-touch transition.
				// Send an up event for the last pointer.
				final int lastX = tracker.getLastX();
				final int lastY = tracker.getLastY();
				mOldKey = tracker.getKeyOn(lastX, lastY);
				tracker.onUpEvent(lastX, lastY, eventTime);
			} else if (pointerCount == 1 && oldPointerCount == 1) {
				tracker.processMotionEvent(action, (int)x, (int)y, eventTime, this,me);
			} else {
				Log.w(TAG, "Unknown touch panel behavior: pointer count is " + pointerCount
						+ " (old " + oldPointerCount + ")");
			}
			return true;
		}
		
		
		if (!isQWERTYKeyboard) {
			// Use only main (id=0) pointer tracker.
			final PointerTracker tracker = PointerTracker.getPointerTracker(0, keyEventHandler);
			if (pointerCount == 1 && oldPointerCount == 2) {
				// Multi-touch to single touch transition.
				// Send a down event for the latest pointer if the key is different from the
				// previous key.
				final Key newKey = tracker.getKeyOn((int)x, (int)y);
				if (mOldKey != newKey) {
					tracker.onDownEvent((int)x, (int)y, eventTime, this);
					if (action == MotionEvent.ACTION_UP)
						tracker.onUpEvent((int)x, (int)y, eventTime);
				}
			} else if (pointerCount == 2 && oldPointerCount == 1) {
				// Single-touch to multi-touch transition.
				// Send an up event for the last pointer.
				final int lastX = tracker.getLastX();
				final int lastY = tracker.getLastY();
				mOldKey = tracker.getKeyOn(lastX, lastY);
				tracker.onUpEvent(lastX, lastY, eventTime);
			} else if (pointerCount == 1 && oldPointerCount == 1) {
				tracker.processMotionEvent(action, (int)x, (int)y, eventTime, this,me);
			} else {
				Log.w(TAG, "Unknown touch panel behavior: pointer count is " + pointerCount
						+ " (old " + oldPointerCount + ")");
			}
			return true;

		}
		
		if (action == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < pointerCount; i++) {
				
				//TP 15640 Multiples of text inserted when trying to insert text through Ext.Character bubble
				if(mMoreKeysPanel != null && id == mMoreKeysPanelPointerTrackerId) {
					keyEventHandler = mMoreKeysPanel;
				} else {
					keyEventHandler = this;
				}
				
				final int pointerId = me.getPointerId(i);
				final PointerTracker tracker = PointerTracker.getPointerTracker(
						pointerId, keyEventHandler);
				final float px, py;
				final MotionEvent motionEvent;
				if (mMoreKeysPanel != null
						&& tracker.mPointerId == mMoreKeysPanelPointerTrackerId) {
					px = mMoreKeysPanel.translateX(me.getX(i), !mTouchScreenRegulator.isTouchFromMainKeyboard());
					py = mMoreKeysPanel.translateY(me.getY(i), !mTouchScreenRegulator.isTouchFromMainKeyboard());
					motionEvent = null;
				} else {
					px = (int)me.getX(i);
					py = (int)me.getY(i);
					motionEvent = me;
				}
                tracker.onMoveEvent(px, py, eventTime, motionEvent);
			}
		} else {
			if(mMoreKeysPanel != null && id == mMoreKeysPanelPointerTrackerId) {
				keyEventHandler = mMoreKeysPanel;
			} else {
				keyEventHandler = this;
			}
			
			final PointerTracker tracker = PointerTracker.getPointerTracker(id, keyEventHandler);
			tracker.processMotionEvent(action, x, y, eventTime, keyEventHandler,me);
		}
		return true;
	}

	@Override
	public void cancelAllMessages() {
		mKeyTimerHandler.cancelAllMessages();
		super.cancelAllMessages();
	}

	@Override
	public void setKeyboard(Keyboard newKeyboard) {
		// Copy space key status from old keyboard to new keyboard
		if(getKeyboard() != null && newKeyboard != null) {
			KPTAdpatxtKeyboard oldKeyboard = (KPTAdpatxtKeyboard) getKeyboard();
			KPTAdpatxtKeyboard currKeyboard = (KPTAdpatxtKeyboard) newKeyboard;
			if(oldKeyboard.isSpaceHighlighted() != currKeyboard.isSpaceHighlighted()) {
				currKeyboard.setSpaceHighlighted(oldKeyboard.isSpaceHighlighted());
				//currKeyboard.updateSpaceBarForLocale();
			}
		}
		mKeyTimerHandler.cancelLongPressTimer();
		super.setKeyboard(newKeyboard);

		mKeyDetector.setKeyboard(
				newKeyboard, -getPaddingLeft(), -getPaddingTop() + super.mVerticalCorrection);
		mTouchScreenRegulator.setKeyboard(getKeyboard());
		isQWERTYKeyboard = mKeyboardSwitcher.isCurrentKeyboardModeQwerty();
		int installedDictionariesCount = 0; 
		if(mLanguageSwitcher != null) {
			installedDictionariesCount = mLanguageSwitcher.getLocaleCount();
		}
		mShouldDisplayAccents = mSharedPreference.getBoolean(KPTConstants.PREF_DISPLAY_ACCENTS, true);
		PointerTracker.setKeyDetector(mKeyDetector);
		PointerTracker.setCurrentKeyboardProperties(isQWERTYKeyboard, 
						mKeyboardSwitcher.getCurrentQwertyKeypadState(), 
						mKeyboardSwitcher.getPhoneKeypadState(), 
						mShouldDisplayAccents, 
						installedDictionariesCount);
		
		
	}
	
	@Override
	public void closing() {
		super.closing();
		dismissMoreKeysPanel();
		dismissLanguageSelectionDialog(); //TP #15541. SIP and Select keyboard language dialogue remains same in home screen on pressing home button.
	}

	@Override
	public KeyDetector getKeyDetector() {
		return mKeyDetector;
	}

	@Override
	public KeyboardActionListener getKeyboardActionListener() {
		return mKeyboardActionListener;
	}

	@Override
	public DrawingProxy getDrawingProxy() {
		return this;
	}

	@Override
	public KeyTimerController getTimerProxy() {
		return mKeyTimerHandler;
	}

	public void setPhoneKeyboard(KPTAdpatxtKeyboard mKeyboard) {
	}

	public void setPhoneSymbolsKeyboard(KPTAdpatxtKeyboard phonesymbols) {
	}

	private boolean openMoreKeysKeyboardIfRequired(final Key parentKey,
			final PointerTracker tracker) {
		// Check if we have a popup layout specified first.
		if (mMoreKeysLayout == 0) {
			return false;
		}

		// Check if we are already displaying popup panel.
		if (mMoreKeysPanel != null)
			return false;
		if (parentKey == null)
			return false;
		return onLongPress(parentKey, tracker);
	}

	// This default implementation returns a more keys panel.
	protected MoreKeysPanel onCreateMoreKeysPanel(final Key popupKey, int mStartX, int mStartY) {
		final View container = LayoutInflater.from(getContext()).inflate(mMoreKeysLayout, null);
		if (container == null)
			throw new NullPointerException();

		mMoreKeysKeyboardView = (MoreKeysKeyboardView)container.findViewById(R.id.more_keys_keyboard_view);
		mMoreKeysKeyboardView.setLanguageSwitcher(getLanguageSwitcher());
		mMoreKeysKeyboardView.setKeyboardSwitcher(getKeyboardSwitcher());
		mMoreKeysKeyboardView.setTheme(getTheme());
		mMoreKeysKeyboardView.setKeyTimerHandler(mKeyTimerHandler);
		mMoreKeysKeyboardView.setTouchScreenRegulator(mTouchScreenRegulator);
		
		setCurrentKey(popupKey);
		mPopupKeyboardId = popupKey.popupResId;
		
		//resetting default value, by default always display first page
		IsFirstPage = true;
		
		int numberOfKeysInRow = 0;
		if(mKeyboard != null && mKeyboard.getRowDetails(popupKey.rowNumber) != null ){
			numberOfKeysInRow = mKeyboard.getRowDetails(popupKey.rowNumber).numberOfKeysInRow;
		}
		if((numberOfKeysInRow + 1)/2 > popupKey.keyIndexInRow){
			isRightHalf = false;
		}else{
			isRightHalf = true;
		}
		
		Locale saveLocale = null;
		if(popupKey.popupResId != 0) {
			//this has a popup xml to display so change to the required language 
			//temporarily
			Configuration conf = mResources.getConfiguration();
			saveLocale = conf.locale;
			conf.locale =  mKeyboardSwitcher.getCurrentInputLocale();
			mResources.updateConfiguration(conf, null);
		}
		
		accentedChars = popupKey.popupCharacters;
		Keyboard keyboard = new Keyboard(getContext(), mPopupKeyboardId, true, 0, false);
		if (accentedChars != null) {
			keyboard = new Keyboard(getContext(), mPopupKeyboardId, 
					popupCharsToBeDisplayed(), -1, getPaddingLeft()  + getPaddingRight(), true, 0, false);
		} else {
			List<Key> keys = keyboard.getKeys();
			if(keys != null && keys.size() > 0) {
				int key_gap = keys.get(0).gap;
				int key_width = keys.get(0).width;
				Keys_per_page  =  (getWidth() /( key_width + key_gap))-1;
				total_keys = keys.size();
				//if(keyboard.getNumberOfRows()>1) {
					keyboard.loadKeyboard(getContext(), getContext().getResources().getXml(mPopupKeyboardId), true,Keys_per_page, total_keys);
				//}
			}
		}
		setPopupKeyboardStatus(true);
		mMoreKeysKeyboardView.setKeyboard(keyboard);
		container.measure(
                MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST), 
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
		
		if(popupKey.codes[0] != KPTConstants.KEYCODE_ALT) {
			mShowfirstbublechar = true;
		}
		
		//reset to default language
		if(popupKey.popupResId != 0) {
			final Configuration conf = mResources.getConfiguration();
			conf.locale = saveLocale;
			mResources.updateConfiguration(conf, null);
		}
		
		int[] mWindowOffset = new int[2];
        getLocationInWindow(mWindowOffset);
        
        if(mLanguageSwitcher != null){
			//Stopping the vibration with smiley key on secondary SIP as we are forcibly calling longpress on it for signle tap BUG#196
			if(popupKey.codes[0]!=58){
				//mLanguageSwitcher.mIme.vibrate(KPTAdaptxtIME.VIBRATE_GENERIC);
			}
		}
        
		int popupX = popupKey.x + getPaddingLeft();
		int popupY = popupKey.y + getPaddingTop();
		/*removing the following line from the code 
		 * to display the accented bubble on top of the
		 * base character bug # 9328 -- Bhavani             * 
		 */

		//mPopupX = mPopupX + popupKey.width;
		mMiniKeyboardStartX = popupX + container.getPaddingRight() + mWindowOffset[0];
		// to change the bubble window position if it crosses the screen right side
		if( mMiniKeyboardStartX + container.getMeasuredWidth() > getWidth() || isRightHalf) {
			popupX = popupX - container.getMeasuredWidth() + popupKey.width;
			mMiniKeyboardStartX = popupX + container.getPaddingRight() + mWindowOffset[0];
		} 
		
		mMoreKeysKeyboardView.setStartX(mMiniKeyboardStartX);
		
		popupY = popupY - container.getMeasuredHeight();
		mMiniKeyboardStartY = popupY + container.getPaddingBottom() + mWindowOffset[1];

		mMiniKeyboardStartY = mMiniKeyboardStartY - popupKey.height/4;
		
		//keyboard.setPopupOffset(x < 0 ? 0 : x, y);
		if(popupKey.codes[0] == KPTConstants.KEYCODE_INFINITY){
			keyboard.setShifted(false);
		}else{
			keyboard.setShifted(isShifted());
		}
		
		return mMoreKeysKeyboardView;
	}

	// This default implementation returns a more keys panel.
	protected MoreKeysPanel onCreateMoreKeysPanelHKB(CharSequence popupCharacters, View parent) {
		final View container = LayoutInflater.from(getContext()).inflate(mMoreKeysLayout, null);
		if (container == null)
			throw new NullPointerException();

		mMoreKeysKeyboardView = (MoreKeysKeyboardView)container.findViewById(R.id.more_keys_keyboard_view);
		mMoreKeysKeyboardView.setLanguageSwitcher(getLanguageSwitcher());
		mMoreKeysKeyboardView.setKeyboardSwitcher(getKeyboardSwitcher());
		mMoreKeysKeyboardView.setTheme(getTheme());
		mMoreKeysKeyboardView.setKeyTimerHandler(mKeyTimerHandler);
		mMoreKeysKeyboardView.setTouchScreenRegulator(mTouchScreenRegulator);

		int popupKeyboardId = R.xml.kbd_popup_template;
        Keyboard keyboard = new Keyboard(getContext(), popupKeyboardId, 
        		popupCharacters, -1, getPaddingLeft() + getPaddingRight(), true, 0, false);

		mMoreKeysKeyboardView.setKeyboard(keyboard);
		
        container.measure(
                MeasureSpec.makeMeasureSpec(parent.getWidth(), MeasureSpec.AT_MOST), 
                MeasureSpec.makeMeasureSpec(parent.getHeight(), MeasureSpec.UNSPECIFIED));
        
		final int measuredWidth = container.getMeasuredWidth();
		final int measuredHeight = container.getMeasuredHeight();
		
		int[] mWindowOffset = new int[2];
		getLocationInWindow(mWindowOffset);

        final DisplayMetrics dispMetrics = getResources().getDisplayMetrics();
        int screenWidth = dispMetrics.widthPixels;
        int screenHeight = dispMetrics.heightPixels;
        
		// 'x' position of accentuated bubble should always be center aligned to the screen.
        int mPopupX = (screenWidth - measuredWidth)/2;
        int mPopupY;
        
        int bubbleOffset = (int)getResources().getDimension(R.dimen.kpt_accentuated_bubble_offset);
        int candStripHeight = (int)getResources().getDimension(R.dimen.kpt_candidate_strip_height);
        int[] xy = new int[2]; // for xy co-ordinates of the view
        
        // getting the mid point of SIP/screen
        int midOfSIP = 0;
    	if(getHeight() != 0){        		
    		midOfSIP = getHeight()/2;
    	} else{
    		midOfSIP = screenHeight/2;
    	}
        	
        if(isShown()){ // when the entire SIP is in display
        	getLocationInWindow(xy);
        	mPopupY = (xy[1] + midOfSIP) - candStripHeight;
        } else if(mLanguageSwitcher.mIme.isCandidateViewShown()){  // when candidate bar alone is in display           	
        	mPopupY = - (measuredHeight + container.getPaddingBottom() + bubbleOffset);
        } else{ // when both SIP and candidate bar are not shown on screen
        	getLocationInWindow(xy);
        	midOfSIP = screenHeight/2;
        	// this check is being done because of the different behaviour of XY co-ordinates
        	// on different devices like MotoCharm & Samsung Galaxy Pro
        	if(midOfSIP > xy[1]){
        		mPopupY = -(midOfSIP - xy[1]);
        	} else{
        		mPopupY = -(xy[1] - midOfSIP);
        	}          
        }
     
        mMiniKeyboardStartX = mPopupX;
        mMiniKeyboardStartY = mPopupY;
		return mMoreKeysKeyboardView;
	}
		
	/**Fix for TP item #8104
	 * This variable is used to maintain preview of the character
	 * in upper case until the first character inserted in the empty editor.
	 */
	private static int total_keys = 0;
	private int mPopupKeyboardId;
	private AlertDialog mLanguageDialog;
	
	private String popupCharsToBeDisplayed(){

    	String reqPopUpChars = null;
    	if (accentedChars == null) {
			return reqPopUpChars;
		}
    	reqPopUpChars = (String) accentedChars;
    	String accentedChars_rtl = "";
    	if(accentedChars.length() > KPTConstants.NUMBER_OF_POPUPCHARS_INROW){
    		if(IsFirstPage){
    			reqPopUpChars =(String) accentedChars.subSequence(0, KPTConstants.NUMBER_OF_POPUPCHARS_INROW) ;
    		} else {
    			reqPopUpChars =(String) accentedChars.subSequence(accentedChars.length()-KPTConstants.NUMBER_OF_POPUPCHARS_INROW, accentedChars.length()) ;
    		}
    	}
		for (int i = reqPopUpChars.length()-1; i >= 0; i--) {
			accentedChars_rtl = accentedChars_rtl + reqPopUpChars.charAt(i);
		}
    	if(accentedChars.length() > KPTConstants.NUMBER_OF_POPUPCHARS_INROW){
    		if(isRightHalf){
    			if(IsFirstPage ){
    				reqPopUpChars = mLeftChevronIconText  + accentedChars_rtl ;
    			} else {
    				reqPopUpChars = accentedChars_rtl + mRightChevronIconText ;
    			}
    		}else {
    			if(IsFirstPage)	{
    				reqPopUpChars = reqPopUpChars + mRightChevronIconText;
    			} else {
    				reqPopUpChars = mLeftChevronIconText+ reqPopUpChars;
    			}
    		}
    	}else{
    		if(isRightHalf){
    			reqPopUpChars = accentedChars_rtl;
    		}
    	}
    	return reqPopUpChars;
    }
	
	private boolean changeBubblePage() {
		Keyboard keyboard = null;
		Resources orig = getContext().getResources();
		Configuration conf = orig.getConfiguration();
		Locale saveLocale = conf.locale;
		conf.locale =  mKeyboardSwitcher.getCurrentInputLocale();
		orig.updateConfiguration(conf, null);
		IsFirstPage = !IsFirstPage;
		/*if(action == MotionEvent.ACTION_DOWN) {
			IsChevron = true;
			mLongPressTime = System.currentTimeMillis();
		}*/

		try {
			if(accentedChars == null){
				keyboard = new Keyboard(getContext(),mPopupKeyboardId, true, 0, false);
				keyboard.loadKeyboard(getContext(), getContext().getResources().getXml(mPopupKeyboardId), IsFirstPage,Keys_per_page, total_keys);
			}else {
				keyboard = new Keyboard(getContext(), mPopupKeyboardId, popupCharsToBeDisplayed(),
						-1, getPaddingLeft() + getPaddingRight(), true, 0, false);
			}
			
			mMoreKeysKeyboardView.setKeyboard(keyboard);
		} catch(Exception e) {
			e.printStackTrace();
		}
		conf.locale = saveLocale;
		orig.updateConfiguration(conf, null);
		return true;
	}
	
	/**
	 * Called when a key is long pressed. By default this will open more keys keyboard associated
	 * with this key.
	 * @param parentKey the key that was long pressed
	 * @param tracker the pointer tracker which pressed the parent key
	 * @return true if the long press is handled, false otherwise. Subclasses should call the
	 * method on the base class if the subclass doesn't wish to handle the call.
	 */
	protected boolean onLongPress(final Key parentKey, final PointerTracker tracker) {
		/*if (parentKey.popupResId == 0) {
            return true;
        }*/
		return openMoreKeysPanel(parentKey, tracker);
	}
	
	protected boolean onLongPress(int keyCode, CharSequence popupCharacters, boolean capsOn, View parent) {
		return openMoreKeysPanelHKB(popupCharacters, parent);
	}

	private boolean openMoreKeysPanel(final Key parentKey, final PointerTracker tracker) {
		MoreKeysPanel moreKeysPanel = mMoreKeysPanelCache.get(parentKey);
		if (moreKeysPanel == null) {
			moreKeysPanel = onCreateMoreKeysPanel(parentKey, tracker.mDownX, tracker.mDownY);
			if (moreKeysPanel == null)
				return false;
			//mMoreKeysPanelCache.put(parentKey, moreKeysPanel);
		}

		if (mMoreKeysWindow == null) {
			mMoreKeysWindow = new PopupWindow(getContext());
			mMoreKeysWindow.setBackgroundDrawable(null);
			//mMoreKeysWindow.setAnimationStyle(R.style.MoreKeysKeyboardAnimation);
		}

		mMoreKeysPanel = moreKeysPanel;
		mMoreKeysPanelPointerTrackerId = tracker.mPointerId;

		moreKeysPanel.showMoreKeysPanel(
				this, this, mMiniKeyboardStartX, mMiniKeyboardStartY, mMoreKeysWindow, mKeyboardActionListener, ignoreOrigin,
				mOriginX, mOriginY);

		tracker.onShowMoreKeysPanel(mMiniKeyboardStartX, mMiniKeyboardStartY, moreKeysPanel, parentKey);
		setPopupKeyboardStatus(true);
		dimEntireKeyboard(true);
		return true;
	}
	
	private boolean openMoreKeysPanelHKB(final CharSequence popupCharacters, View parent) {
		MoreKeysPanel moreKeysPanel = onCreateMoreKeysPanelHKB(popupCharacters, parent);
			
		if (mMoreKeysWindow == null) {
			mMoreKeysWindow = new PopupWindow(getContext());
			mMoreKeysWindow.setBackgroundDrawable(null);
			//mMoreKeysWindow.setAnimationStyle(R.style.MoreKeysKeyboardAnimation);
		}
		
		mMoreKeysPanel = moreKeysPanel;
		moreKeysPanel.showMoreKeysPanel(
				parent, this, mMiniKeyboardStartX, mMiniKeyboardStartY, mMoreKeysWindow, mKeyboardActionListener, ignoreOrigin,
				mOriginX, mOriginY);
		
		final PointerTracker tracker = PointerTracker.getPointerTracker(0, this);
		mMoreKeysPanelPointerTrackerId = 0;
		mShowfirstbublechar = false;
		tracker.onShowMoreKeysPanel(mMiniKeyboardStartX, mMiniKeyboardStartY, moreKeysPanel, null);
		
		setPopupKeyboardStatus(true);
		dimEntireKeyboard(true);
		return true;
	}

	@Override
	public boolean dismissMoreKeysPanel() {
		if (mMoreKeysWindow != null && mMoreKeysWindow.isShowing()) {
			mMoreKeysWindow.dismiss();
			mMoreKeysPanel = null;
			setPopupKeyboardStatus(false);
			mMoreKeysPanelPointerTrackerId = -1;
			dimEntireKeyboard(false);
			return true;
		}
		return false;
	}
	
	@Override
	public void resetGlideInputType() {
		
	}
	
   /**
     * Receives hover events from the input framework.
     *
     * @param event The motion event to be dispatched.
     * @return {@code true} if the event was handled by the view, {@code false}
     *         otherwise
     */
    @Override
    public boolean dispatchHoverEvent(final MotionEvent event) {
        // Reflection doesn't support calling superclass methods.
        return false;
    }
	
    public void setCurrentTextEntryType(int type) {
		mCurrentTextEntryType = type;
	}
	
	public int getCurrentTextEntryType() {
		return mCurrentTextEntryType;
	}
	
	public void setPreviousTextEntryType(int type) {
		mPreviousTextEntryType = type;
	}

	public int getPreviousTextEntryType() {
		return mPreviousTextEntryType;
	}
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	@Override
	public boolean getKeyboardType() {
		return isQWERTYKeyboard;
	}
	
	/**
	 * Called by IME to initialize the handler object.
	 * @param imeHandler Handler serviced by IME
	 */
	public void setIMEHandler(AdaptxtIME imeHandler) {
		mIMEHandler = imeHandler;
	}
	
	public void languageSelection() {
		KPTAdpatxtKeyboard adaptxtKB = (KPTAdpatxtKeyboard)getKeyboard();
		if (adaptxtKB == null) {
			return;
		}
		
		KPTLanguageSwitcher kptLangSwitcher = adaptxtKB.getLanguageSwitcher();
		if(kptLangSwitcher == null) {
			return;
		}
		
		SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(getContext());
		if (kptLangSwitcher.getLocaleCount() == 0
				&& !(sharedPreference.getBoolean(KPTConstants.PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS, false))) { //Bug 6920
			// launch Addon manager settings view on long press of space key
			// if there are no addons installed.
			getOnKeyboardActionListener().launchSettings(KPTAddonManager.class);
		}else if(sharedPreference.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false)) {
			// If core is in maintenance mode then show maintenance mode status in language selection dialog
			mLangSelectionDialog = new KPTIMELanguageSelectionDialog(getContext(),mIMEHandler,
					getWindowToken(), kptLangSwitcher, mIMEHandler.mHandler,
					KPTIMELanguageSelectionDialog.MAINTANENCE_MODE_DIALOG, new ContextThemeWrapper(getContext(), R.style.AdaptxtTheme));
			//mLangSelectionDialog.show();
//			displayLanguagesSelectionDialog();
		}else {
			kptLangSwitcher.loadLocales(getContext());
			// Show language selection dialog with list of languages
			mLangSelectionDialog = new KPTIMELanguageSelectionDialog(getContext(),mIMEHandler,
					getWindowToken(), kptLangSwitcher, mIMEHandler.mHandler,
					KPTIMELanguageSelectionDialog.NORMAL_MODE_DIALOG, new ContextThemeWrapper(getContext(), R.style.AdaptxtTheme));
			
			
			//mLangSelectionDialog.show();
			displayLanguagesSelectionDialog(mLangSelectionDialog.getListView());
		}
	}
	
	public void displayLanguagesSelectionDialog(View listview){
		ContextThemeWrapper mContextWrapper = new ContextThemeWrapper(getContext(), R.style.AdaptxtTheme); 
		AlertDialog.Builder  builder = new AlertDialog.Builder(mContextWrapper);
		builder.setCancelable(true);
//		builder.setIcon(R.drawable.adaptxt_launcher_icon);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setTitle(getContext().getResources().getString(R.string.kpt_ime_language_selection_dialog_title));
		
		builder.setView(listview);
		
		final int color;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			color = getResources().getColor(R.color.kpt_balck_color_text);
		}else{
			color = getResources().getColor(R.color.kpt_white_color_text);
		}
		
		mLanguageDialog = builder.create();
		mLangSelectionDialog.setAlertDialog(mLanguageDialog);
		
		Window window = mLanguageDialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		mLanguageDialog.show();

	}
	
	public void dismissLanguageSelectionDialog() {
		if(mLanguageDialog != null && mLanguageDialog.isShowing()) {
			mLanguageDialog.dismiss();
		}
	}
	
	public KeyboardView getMiniKeyboardView() {
		if(mMoreKeysPanel != null) {
			return mMoreKeysKeyboardView;
		} else {
			return null;
		}
	}
	
	@Override
	public boolean isShown() {
		if(View.VISIBLE == getVisibility()){
			return true;
		}else{
			return false;
		}
	}
}
