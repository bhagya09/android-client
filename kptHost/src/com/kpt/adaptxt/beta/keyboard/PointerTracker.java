package com.kpt.adaptxt.beta.keyboard;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.KPTKeyboardSwitcher;
import com.kpt.adaptxt.beta.Keyboard;
import com.kpt.adaptxt.beta.KeyboardView;
import com.kpt.adaptxt.beta.MainKeyboardView;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.glide.KPTGlideKBShortcuts;
import com.kpt.adaptxt.beta.keyboard.GestureStroke.GestureStrokeParams;
import com.kpt.adaptxt.beta.util.KPTConstants;



public final class PointerTracker implements PointerTrackerQueue.Element {

	public interface KeyEventHandler {
		/**
		 * Get KeyDetector object that is used for this PointerTracker.
		 * @return the KeyDetector object that is used for this PointerTracker
		 */
		public KeyDetector getKeyDetector();

		/**
		 * Get KeyboardActionListener object that is used to register key code and so on.
		 * @return the KeyboardActionListner for this PointerTracker
		 */
		public KeyboardActionListener getKeyboardActionListener();

		/**
		 * Get DrawingProxy object that is used for this PointerTracker.
		 * @return the DrawingProxy object that is used for this PointerTracker
		 */
		public DrawingProxy getDrawingProxy();

		/**
		 * Get TimerProxy object that handles key repeat and long press timer event for this
		 * PointerTracker.
		 * @return the TimerProxy object that handles key repeat and long press timer event.
		 */
		public KeyTimerController getTimerProxy();

		public boolean getKeyboardType();

	}
	public interface DrawingProxy extends MoreKeysPanel.Controller {
		public void invalidateKey(Key key);
		public void showKeyPreview(PointerTracker tracker);
		public void dismissKeyPreview(PointerTracker tracker);
		public void showGesturePreviewTrail(PointerTracker tracker, boolean isOldestTracker);
	}

	public interface KeyTimerController {
		public void startTypingStateTimer(Key typedKey);
		public boolean isTypingState();
		public void startKeyRepeatTimer(PointerTracker tracker);
		public void startLongPressTimer(PointerTracker tracker);
		public void startLongPressTimer(int code);
		public void cancelLongPressTimer();
		public void startDoubleTapTimer();
		public void cancelDoubleTapTimer();
		public boolean isInDoubleTapTimeout();
		public void cancelKeyTimers();
		public void startMultiTapKey(PointerTracker tracker);
		public void toogle12KeySymbolsLayout(PointerTracker pointerTracker);
		public void setCurrentTextEntryType(int currentEntryType);
		public void setPreviousTextEntryType(int previousEntryType);
		public void showNextSetOfPopupCharacters(int motionEventAction);
		public void showSpaceLanguageChangeIcon(PointerTracker tracker, int action);
		public void startLongPressTimerNoDelay(PointerTracker tracker, Key key);
		
		public static class Adapter implements KeyTimerController {
			@Override
			public void startTypingStateTimer(Key typedKey) {}
			@Override
			public boolean isTypingState() { return false; }
			@Override
			public void startKeyRepeatTimer(PointerTracker tracker) {}
			@Override
			public void startLongPressTimer(PointerTracker tracker) {}
			@Override
			public void startLongPressTimer(int code) {}
			@Override
			public void cancelLongPressTimer() {}
			@Override
			public void startDoubleTapTimer() {}
			@Override
			public void cancelDoubleTapTimer() {}
			@Override
			public boolean isInDoubleTapTimeout() { return false; }
			@Override
			public void cancelKeyTimers() {}
			@Override
			public void startMultiTapKey(PointerTracker tracker) { }
			@Override
			public void toogle12KeySymbolsLayout(PointerTracker pointerTracker) { }
			@Override
			public void setCurrentTextEntryType(int currentEntryType) { }
			@Override
			public void setPreviousTextEntryType(int previousEntryType) { }
			@Override
			public void showNextSetOfPopupCharacters(int motionEventAction) { }
			@Override
			public void showSpaceLanguageChangeIcon(PointerTracker tracker, int action) { }
			@Override
			public void startLongPressTimerNoDelay(PointerTracker tracker, Key key) { }
		}
	}


	static final class BogusMoveEventDetector {
		// Move these thresholds to resource.
		// These thresholds' unit is a diagonal length of a key.
		private static final float BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD = 0.53f;
		private static final float BOGUS_MOVE_RADIUS_THRESHOLD = 1.14f;

		private int mAccumulatedDistanceThreshold;
		private int mRadiusThreshold;

		// Accumulated distance from actual and artificial down keys.
		/* package */ int mAccumulatedDistanceFromDownKey;
		private int mActualDownX;
		private int mActualDownY;

		public void setKeyboardGeometry(final int keyWidth, final int keyHeight) {
			final float keyDiagonal = (float)Math.hypot(keyWidth, keyHeight);
			mAccumulatedDistanceThreshold = (int)(
					keyDiagonal * BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD);
			mRadiusThreshold = (int)(keyDiagonal * BOGUS_MOVE_RADIUS_THRESHOLD);
		}

		public void onActualDownEvent(final int x, final int y) {
			mActualDownX = x;
			mActualDownY = y;
		}

		public void onDownKey() {
			mAccumulatedDistanceFromDownKey = 0;
		}

		public void onMoveKey(final int distance) {
			mAccumulatedDistanceFromDownKey += distance;
		}

		public boolean hasTraveledLongDistance(final int x, final int y) {
			final int dx = Math.abs(x - mActualDownX);
			final int dy = Math.abs(y - mActualDownY);
			// A bogus move event should be a horizontal movement. A vertical movement might be
			// a sloppy typing and should be ignored.
			return dx >= dy && mAccumulatedDistanceFromDownKey >= mAccumulatedDistanceThreshold;
		}

		/* package */ int getDistanceFromDownEvent(final int x, final int y) {
			return getDistance(x, y, mActualDownX, mActualDownY);
		}

		public boolean isCloseToActualDownEvent(final int x, final int y) {
			return getDistanceFromDownEvent(x, y) < mRadiusThreshold;
		}
	}

	public static void init(boolean hasDistinctMultitouch,
			boolean needsPhantomSuddenMoveEventHack) {
		if (hasDistinctMultitouch) {
			sPointerTrackerQueue = new PointerTrackerQueue();
		} else {
			sPointerTrackerQueue = null;
		}
		sNeedsPhantomSuddenMoveEventHack = needsPhantomSuddenMoveEventHack;
		sParams = PointerTrackerParams.DEFAULT;
		sGestureStrokeParams = GestureStrokeParams.DEFAULT;
		sTimeRecorder = TimeRecorder.DEFAULT;
	}


	/**
	 * 
	 * @param isCurrentKeyboardType QWERTY whether the current keyboard is qwerty or phone type
	 * @param currentQWERTYkeypadType whether it is primary, secondary or tertiary in qwerty
	 * @param currentPhoneKeypadType whether it is primary, secondary or tertiary in phone keyboard
	 * @param shouldDisplayAccentedBubble whether user has enabled accented bubbles optio in settings
	 * @param installedDictionariesCount number of dictionaries installed
	 */
	public static void setCurrentKeyboardProperties(final boolean isCurrentKeyboardTypeQWERTY, 
					final int currentQWERTYkeypadType, 
					final int currentPhoneKeypadType,
					final boolean shouldDisplayAccentedBubble,
					final int installedDictionariesCount) {
		
		final int trackersSize = PointerTracker.sTrackers.size();
		for (int i = 0; i < trackersSize; ++i) {
			final PointerTracker tracker = PointerTracker.sTrackers.get(i);
			tracker.setIsQwertyKeyboard(isCurrentKeyboardTypeQWERTY);
			tracker.setCurrentQWERTYKeyboardType(currentQWERTYkeypadType);
			tracker.setCurrentPhoneKeyboardType(currentPhoneKeypadType);
			tracker.setIfAccentedBubblesShouldBeDisplayed(shouldDisplayAccentedBubble);
			tracker.setInstalledDictionariesCount(installedDictionariesCount);
		}
	}
	
	public static void setParameters(Resources res) {
		sParams = new PointerTrackerParams(res);
		sGestureStrokeParams = new GestureStrokeParams(res);
		sTimeRecorder = new TimeRecorder(res);
	}

	static final class PointerTrackerParams {
		public final boolean mSlidingKeyInputEnabled;
		public final int mTouchNoiseThresholdTime;
		public final int mTouchNoiseThresholdDistance;
		public final int mSuppressKeyPreviewAfterBatchInputDuration;

		public static final PointerTrackerParams DEFAULT = new PointerTrackerParams();

		private PointerTrackerParams() {
			mSlidingKeyInputEnabled = false;
			mTouchNoiseThresholdTime = 0;
			mTouchNoiseThresholdDistance = 0;
			mSuppressKeyPreviewAfterBatchInputDuration = 0;
		}

		private PointerTrackerParams(Resources res) {
			mSlidingKeyInputEnabled = res.getBoolean(R.bool.kpt_config_sliding_key_input_enabled);
			mTouchNoiseThresholdTime = res.getInteger(R.integer.kpt_config_touch_noise_threshold_time);
			mTouchNoiseThresholdDistance = res.getDimensionPixelSize(R.dimen.kpt_config_touch_noise_threshold_distance);
			mSuppressKeyPreviewAfterBatchInputDuration = res.getInteger(R.integer.kpt_config_suppress_key_preview_after_batch_input_duration);
		}
	}


	static final class TimeRecorder {
		private final int mSuppressKeyPreviewAfterBatchInputDuration;
		private int mStaticTimeThresholdAfterFastTyping = 0; // msec
		private long mLastTypingTime;
		private long mLastLetterTypingTime;
		private long mLastBatchInputTime;

		public static final TimeRecorder DEFAULT = new TimeRecorder();

		private TimeRecorder(){
			mSuppressKeyPreviewAfterBatchInputDuration = 1000 ;
			mStaticTimeThresholdAfterFastTyping = 0;
		}

		public TimeRecorder(final Resources res) {
			mSuppressKeyPreviewAfterBatchInputDuration = 1000;
			mStaticTimeThresholdAfterFastTyping = 0;
		}

		public boolean isInFastTyping(final long eventTime) {
			final long elapsedTimeSinceLastLetterTyping = eventTime - mLastLetterTypingTime;
			return elapsedTimeSinceLastLetterTyping < mStaticTimeThresholdAfterFastTyping;
		}

		private boolean wasLastInputTyping() {
			return mLastTypingTime >= mLastBatchInputTime;
		}

		public void onCodeInput(final int code, final long eventTime) {
			// Record the letter typing time when
			// 1. Letter keys are typed successively without any batch input in between.
			// 2. A letter key is typed within the threshold time since the last any key typing.
			// 3. A non-letter key is typed within the threshold time since the last letter key
			// typing.
			if (Character.isLetter(code)) {
				if (wasLastInputTyping()
						|| eventTime - mLastTypingTime < mStaticTimeThresholdAfterFastTyping) {
					mLastLetterTypingTime = eventTime;
				}
			} else {
				if (eventTime - mLastLetterTypingTime < mStaticTimeThresholdAfterFastTyping) {
					// This non-letter typing should be treated as a part of fast typing.
					mLastLetterTypingTime = eventTime;
				}
			}
			mLastTypingTime = eventTime;
		}

		public void onEndBatchInput(final long eventTime) {
			mLastBatchInputTime = eventTime;
		}

		public long getLastLetterTypingTime() {
			return mLastLetterTypingTime;
		}

		public boolean needsToSuppressKeyPreviewPopup(final long eventTime) {
			return !wasLastInputTyping()
					&& eventTime - mLastBatchInputTime < mSuppressKeyPreviewAfterBatchInputDuration;
		}
	}


	// Move this threshold to resource.
	// TODO: Device specific parameter would be better for device specific hack?
	private static final float PHANTOM_SUDDEN_MOVE_THRESHOLD = 0.25f; // in keyWidth
	// This hack might be device specific.
	private static final boolean sNeedsProximateBogusDownMoveUpEventHack = true;

	public static final ArrayList<PointerTracker> sTrackers = CollectionUtils.newArrayList();
	private static PointerTrackerQueue sPointerTrackerQueue;
	public final int mPointerId;


	/** True if {@link PointerTracker}s should handle gesture events. */
	private static boolean sShouldHandleGesture = true;
	private static boolean sMainDictionaryAvailable = false;
	private static boolean sGestureHandlingEnabledByInputField = false;
	private static boolean sGestureHandlingEnabledByUser = false;

	private static final KeyboardActionListener EMPTY_LISTENER =
			new KeyboardActionListener.Adapter();

	private DrawingProxy mDrawingProxy;
	private KeyTimerController mTimerProxy;
	private KeyDetector mKeyDetector;
	private KeyboardActionListener mListener = EMPTY_LISTENER;

	private Keyboard mKeyboard;
	private int mPhantonSuddenMoveThreshold;
	private final BogusMoveEventDetector mBogusMoveEventDetector = new BogusMoveEventDetector();

	// The position and time at which first down event occurred.
	private long mDownTime;
	private long mUpTime;

	// The current key where this pointer is.
	private Key mCurrentKey = null;
	// The position where the current key was recognized for the first time.
	private int mKeyX;
	private int mKeyY;

	// Last pointer position.
	private int mLastX;
	private int mLastY;
	
	public int mDownX;
	public int mDownY;

	private Key mDownKey;
	
	// true if event is already translated to a key action.
	private boolean mKeyAlreadyProcessed;

	// true if this pointer has been long-pressed and is showing a more keys panel.
	private boolean mIsShowingMoreKeysPanel = false;

	// true if this pointer is in a sliding key input.
	boolean mIsInSlidingKeyInput;
	// true if this pointer is in a sliding key input from a modifier key,
	// so that further modifier keys should be ignored.
	boolean mIsInSlidingKeyInputFromModifier;

	// true if a sliding key input is allowed.
	private boolean mIsAllowedSlidingKeyInput;
	private static boolean sNeedsPhantomSuddenMoveEventHack = false;
	private static boolean mIsDetectingGesture = false; // per PointerTracker.
	private static boolean sInGesture = false;
	private static final InputPointers sAggregratedPointers = new InputPointers(GestureStroke.DEFAULT_CAPACITY);
	private static int sLastRecognitionPointSize = 0; // synchronized using sAggregratedPointers
	private static long sLastRecognitionTime = 0; // synchronized using sAggregratedPointers

	private final GestureStrokeWithPreviewPoints mGestureStrokeWithPreviewPoints;
	private static long sGestureFirstDownTime;

	// Parameters for pointer handling.
	private static PointerTrackerParams sParams;
	private static GestureStrokeParams sGestureStrokeParams;
	private static TimeRecorder sTimeRecorder;

	public static final int KEY_CURRENT_STATE_SINGLE_TAP = 0;
	public static final int KEY_CURRENT_STATE_LONG_PRESS = 1;
	//this case is handled in KeyboardView.openMoreKeysKeyboardIfRequired
	public static final int KEY_CURRENT_STATE_MINI_KEYBOARD = 2;

	private boolean mIsSpaceSlidingLocaleStarted = false;
	public static boolean mIsConjuction=false;
	public static boolean mPhantonMiniKeyboardOnScreen = false;
	
	/**
	 * this variable indicated the current state this pointer(ie.. current pointer key).
	 * It will always be be either in one of the following state <br><br> 
	 * 
	 * 1) KEY_CURRENT_STATE_SINGLE_TAP,<br>
	 * 2) KEY_CURRENT_STATE_LONG_PRESS,<br>
	 * 3) KEY_CURRENT_STATE_MINI_KEYBOARD<br><br>
	 * 
	 *   these key states are used in sending the keycode to IME(check detectAndSendKey() for usage)<br>
	 *   
	 * @param state
	 */
	private int mKeyCurrentState = KEY_CURRENT_STATE_SINGLE_TAP;
	
	private boolean mIsInMultiTap;
	private int mMultiTapCount;
	/**
	 * is the current keyboard type QWERTY ?
	 */
	private boolean mIsQwertyKeyboard;
	
	private boolean mIsDoubleTapedSpaceKey = false;
	
	/**
	 * the current keyboard type in QWERTY mode ie.. either primary, secondary or tertiary 
	 */
	private int mCurrentQWERTYKeyboardType;
	
	/**
	 * the current keyboard type in PHONE mode ie.. either primary, secondary or tertiary 
	 */
	private int mCurrentPHONEKeyboardType;
	
	/**
	 * indicates whether the user has selected the "Show Accents" setting in the Adaptxt settings
	 */
	private boolean mShouldDisplayAccents;
	
	/**
	 * the Current text entry type ie.. either normal type or glide input. used in IME class
	 */
	private int mCurrentTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_NORMAL;
	/**
	 * <p>when a key is long pressed if there are no popup chars directly commit secondary char into editor.<p>
	 * 
	 * This flag indicates whether the char is commited during long press, so that
	 * it wont get inserted again on key release
	 *  
	 */
	private boolean isSecondaryCharAlreadyCommitted = false;
	
	/**
	 *  this flag indicates if the space key is long pressed (ie... continuous space insertion has been started)
	 */
	private boolean mSpaceLongPressStarted = false;
	
	/**
	 * this indicates if the glide has started on special keys like mode change 
	 * to handle gesture shortcuts like select all, paste etc..
	 */
	private boolean mDownStartedOnSpecialKey = false;
	public static int mCurrentSyllableIndex = -1;
	public static int mPreviousSyllableIndex = -1;
	
	/**
	 * number of dictionaries that are installed
	 */
	private static int mInstalledDictionariesCount = 0;
	
	private PointerTracker(final int id, final KeyEventHandler handler) {
		if (handler == null) {
			throw new NullPointerException();
		}

		mPointerId = id;
		mListener = handler.getKeyboardActionListener();
		mDrawingProxy = handler.getDrawingProxy();
		mTimerProxy = handler.getTimerProxy();
		mIsQwertyKeyboard = handler.getKeyboardType(); 
		mGestureStrokeWithPreviewPoints = new GestureStrokeWithPreviewPoints(id, sGestureStrokeParams);
		setKeyDetectorInner(handler.getKeyDetector());

	}

	private void setKeyDetectorInner(final KeyDetector keyDetector) {
		final Keyboard keyboard = keyDetector.getKeyboard();
		if (keyDetector == mKeyDetector && keyboard == mKeyboard) {
			return;
		}
		mKeyDetector = keyDetector;
		mKeyboard = keyDetector.getKeyboard();
		final int keyWidth = mKeyboard.getSingleKeyWidth();
		final int keyHeight = mKeyboard.getSingleKeyHeight();
		mGestureStrokeWithPreviewPoints.setKeyboardGeometry(keyWidth);
		final Key newKey = mKeyDetector.detectHitKey(mKeyX, mKeyY);
		if (newKey != mCurrentKey) {
			if (mDrawingProxy != null) {
				setReleasedKeyGraphics(mCurrentKey);
			}
			// Ke ep {@link #mCurrentKey} that comes from previous keyboard.
		}
		mPhantonSuddenMoveThreshold = (int)(keyWidth * PHANTOM_SUDDEN_MOVE_THRESHOLD);
		mBogusMoveEventDetector.setKeyboardGeometry(keyWidth, keyHeight);
	}
	
	static int getDistance(final int x1, final int y1, final int x2, final int y2) {
		return (int)Math.hypot(x1 - x2, y1 - y2);
	}

	public static PointerTracker getPointerTracker(final int id, final KeyEventHandler handler) {
		final ArrayList<PointerTracker> trackers = sTrackers;

		// Create pointer trackers until we can get 'id+1'-th tracker, if needed.
		for (int i = trackers.size(); i <= id; i++) {
			final PointerTracker tracker = new PointerTracker(i, handler);
			trackers.add(tracker);
		}

		return trackers.get(id);
	}

	public static boolean isAnyInSlidingKeyInput() {
		return sPointerTrackerQueue != null ? sPointerTrackerQueue.isAnyInSlidingKeyInput() : false;
	}

	public static void setKeyboardActionListener(final KeyboardActionListener listener) {
		final int trackersSize = sTrackers.size();
		for (int i = 0; i < trackersSize; ++i) {
			final PointerTracker tracker = sTrackers.get(i);
			tracker.mListener = listener;
		}
	}

	public static void resetMinikeyboardForAllPointers() {
		mPhantonMiniKeyboardOnScreen = false;
		final int trackersSize = sTrackers.size();
		for (int i = 0; i < trackersSize; ++i) {
			final PointerTracker tracker = sTrackers.get(i);
			tracker.mIsShowingMoreKeysPanel = false;
		}
	}
	
	public void resetSingleTapMinikeyboardDismissState() {
		mPhantonMiniKeyboardOnScreen = false;
		mIsShowingMoreKeysPanel = false;
		mDrawingProxy.dismissMoreKeysPanel();
		mDrawingProxy.resetGlideInputType();
		mCurrentKey = null;
	}
	
	public static void setKeyDetector(final KeyDetector keyDetector) {
		final int trackersSize = sTrackers.size();
		for (int i = 0; i < trackersSize; ++i) {
			final PointerTracker tracker = sTrackers.get(i);
			tracker.setKeyDetectorInner(keyDetector);
		}
	}

	public static void setReleasedKeyGraphicsToAllKeys() {
		final int trackersSize = sTrackers.size();
		for (int i = 0; i < trackersSize; ++i) {
			final PointerTracker tracker = sTrackers.get(i);
			tracker.setReleasedKeyGraphics(tracker.mCurrentKey);
		}
	}
	
	// Returns true if keyboard has been changed by this callback.
	private boolean callListenerOnPressAndCheckKeyboardLayoutChange(final Key key) {
		return false;
	}

	private void updateReleaseKeyGraphics(final Key key) {
		key.onReleased();
		mDrawingProxy.invalidateKey(key);
	}

	private void updatePressKeyGraphics(final Key key) {
		key.onPressed();
		mDrawingProxy.invalidateKey(key);
	}

	public GestureStrokeWithPreviewPoints getGestureStrokeWithPreviewPoints() {
		return mGestureStrokeWithPreviewPoints;
	}

	public int getStartX() {
		return mDownX;
	}
	
	public int getLastX() {
		return mLastX;
	}

	public int getLastY() {
		return mLastY;
	}

	public long getDownTime() {
		return mDownTime;
	}

	public Key getKey() {
		return mCurrentKey;
	}

	public Key getKeyOn(final int x, final int y) {
		return mKeyDetector.detectHitKey(x, y);
	}

	private static void updateGestureHandlingMode() {

		sShouldHandleGesture = sMainDictionaryAvailable
				&& sGestureHandlingEnabledByInputField
				&& sGestureHandlingEnabledByUser; 
				//&& !AccessibilityUtils.getInstance().isTouchExplorationEnabled();
	}

	// Note that this method is called from a non-UI thread.
	public static void setMainDictionaryAvailability(final boolean mainDictionaryAvailable) {
		sMainDictionaryAvailable = mainDictionaryAvailable;
		updateGestureHandlingMode();
	}

	public static void setGestureHandlingEnabledByUser(final boolean gestureHandlingEnabledByUser) {
		sGestureHandlingEnabledByUser = gestureHandlingEnabledByUser;
		updateGestureHandlingMode();
	}

	public static void setGestureHandlingEnabledByInputField(final boolean gestureHandlingEnabledByInputField) {
		sGestureHandlingEnabledByInputField = gestureHandlingEnabledByInputField;
		updateGestureHandlingMode();
	}

	private void setReleasedKeyGraphics(final Key key) {
		mDrawingProxy.dismissKeyPreview(this);
		if (key == null) {
			return;
		}

		// Even if the key is disabled, update the key release graphics just in case.
		if(key.mPressed){
			updateReleaseKeyGraphics(key);
		}
		
/*
		if (key.isShift()) {
			updateReleaseKeyGraphics(key);
		}*/
	}

	private void setPressedKeyGraphics(final Key key, final long eventTime) {
		if (key == null) {
			return;
		}

		if (!sInGesture) {
			mDrawingProxy.showKeyPreview(this);
		}
		
		updatePressKeyGraphics(key);	
	}

	public void processMotionEvent(final int action, final float x, final float  y, final long eventTime,
			final KeyEventHandler handler,MotionEvent me) {

		switch (action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			//if(!mPhantonMiniKeyboardOnScreen) {
				onDownEvent(x, y, eventTime, handler);
			/*} else {
				mCurrentKey = null;
			}*/
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			onUpEvent(x, y, eventTime);
			break;
		case MotionEvent.ACTION_MOVE:
			onMoveEvent(x, y, eventTime, me);
			break;
		case MotionEvent.ACTION_CANCEL:
			onCancelEvent(x, y, eventTime);
			break;
		}
	}
	
	public void onDownEvent(final float x1, final float y1, final long eventTime,
			final KeyEventHandler handler) {

		final int x = (int)x1;
		final int y = (int)y1;

		mDownStartedOnSpecialKey = false;
		mDrawingProxy = handler.getDrawingProxy();
		mTimerProxy = handler.getTimerProxy();
		setKeyboardActionListener(handler.getKeyboardActionListener());
		setKeyDetectorInner(handler.getKeyDetector());
		// Naive up-to-down noise filter.
		final long deltaT = eventTime - mUpTime;
		if (deltaT < sParams.mTouchNoiseThresholdTime) {
			final int distance = getDistance(x, y, mLastX, mLastY);
			if (distance < sParams.mTouchNoiseThresholdDistance) {
				mKeyAlreadyProcessed = true;
				return;
			}
		}

		final Key key = getKeyOn(x, y);
		mBogusMoveEventDetector.onActualDownEvent(x, y);
		final PointerTrackerQueue queue = sPointerTrackerQueue;
		if (queue != null) {
			if (key != null && key.isModifierKey()) {
				// Before processing a down event of modifier key, all pointers already being
				// tracked should be released.
				queue.releaseAllPointers(eventTime);
			}
			queue.add(this);
		}
		onDownEventInternal(x, y, eventTime);
		if (!sShouldHandleGesture) {
			return;
		}
		//TODO if this is special key call detect special key logic  

		// A gesture should start only from the letter key.

		mIsDetectingGesture = (mKeyboard != null) && sGestureHandlingEnabledByInputField
				&& !mIsShowingMoreKeysPanel /*&& key != null *//*&& Keyboard.isLetterCode(key.codes[0])*/;
		if (mIsDetectingGesture) {
			if (getActivePointerTrackerCount() == 1) {
				sGestureFirstDownTime = eventTime;
			}
			mGestureStrokeWithPreviewPoints.onDownEvent(x, y, eventTime, sGestureFirstDownTime,
					sTimeRecorder.getLastLetterTypingTime());
			mGestureStrokeWithPreviewPoints.addPoint(x, y);
		}
	}


	private void onDownEventInternal(final int x, final int y, final long eventTime) {
		Key key = onDownKey(x, y, eventTime);
		// Sliding key is allowed when 1) enabled by configuration, 2) this pointer starts sliding
		// from modifier key, or 3) this pointer's KeyDetector always allows sliding input.
		mIsAllowedSlidingKeyInput = sParams.mSlidingKeyInputEnabled
				|| (key != null && key.isModifierKey())
				|| mKeyDetector.alwaysAllowsSlidingInput();
		mKeyAlreadyProcessed = false;
		resetSlidingKeyInput();
		if (key != null) {
			// This onPress call may have changed keyboard layout. Those cases are detected at
			// {@link #setKeyboard}. In those cases, we should update key according to the new
			// keyboard layout.
			if (callListenerOnPressAndCheckKeyboardLayoutChange(key)) {
				key = onDownKey(x, y, eventTime);
			}

			setSlidingLocaleStart(false);
			KeyboardView.mSpaceSwypeStart = false;
			mDownStartedOnSpecialKey = checkIfSpecialKey(key);
			mKeyCurrentState = KEY_CURRENT_STATE_SINGLE_TAP;
			startMultiTap();
			mListener.onPress(key.codes[0]);
			checkKeyToStartDoubleTapTimer(key);
			//calling this only to reset the value
			mTimerProxy.showNextSetOfPopupCharacters(MotionEvent.ACTION_DOWN);
			startRepeatKey(key);
			startLongPressTimer(key);
			setPressedKeyGraphics(key, eventTime);
		}
	}

	private void onGestureMoveEvent(final int x, final int y, final long eventTime,
			final boolean isMajorEvent, final Key key) {
		final int gestureTime = (int)(eventTime - sGestureFirstDownTime);
		if (mIsDetectingGesture) {
			mGestureStrokeWithPreviewPoints.addPoint(x, y, gestureTime, isMajorEvent);
			mayStartBatchInput(key);
			if (sInGesture) {
				mayUpdateBatchInput(eventTime, key);
			}
		}
	}

	public void onMoveEvent(final float x1, final float y1, final long eventTime, final MotionEvent me) {
		if (mKeyAlreadyProcessed) {
			return;
		}

		final int x = (int)x1;
		final int y = (int)y1;
	
		if((mIsQwertyKeyboard || mCurrentPHONEKeyboardType == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET)
				&& mDownKey != null 
				&& mDownKey.codes[0] == KPTConstants.KEYCODE_SPACE
				&& mInstalledDictionariesCount > 1) {
			
			//if the continuous space has been started on key down start it on action up
			if(mSpaceLongPressStarted && isSlidingSpaceLocaleStarted()) {
				mSpaceLongPressStarted = false;
				mListener.onKey(KPTConstants.KEYCODE_SPACE_LONGPRESS_END, null, false);
			}
			
			onMoveKey(x, y);
			mTimerProxy.showSpaceLanguageChangeIcon(this, MotionEvent.ACTION_MOVE);
			return;
		}
		
		if (sShouldHandleGesture && me != null ) {
			// Add historical points to gesture path.
			final int pointerIndex = me.findPointerIndex(mPointerId);
			final int historicalSize = me.getHistorySize();
			try{
				//#16141. User feedback: Glide doesn't work -- for some devices we get historical points as zero so we add the event coodinates
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					if(historicalSize > 0){
						for (int h = 0; h < historicalSize; h++) {
							final float historicalX = me.getHistoricalX(pointerIndex, h);
							final float historicalY = me.getHistoricalY(pointerIndex, h);
							final long historicalTime = me.getHistoricalEventTime(h);
							mGestureStrokeWithPreviewPoints.addPoint(historicalX, historicalY);
							onGestureMoveEvent((int)historicalX, (int)historicalY, historicalTime, false  /*isMajorEvent*/ , null);
						}
					}else{
						int pointerCount =  me.getPointerCount();
						if(pointerCount >0){
							mGestureStrokeWithPreviewPoints.addPoint(me.getX(mPointerId), me.getY(mPointerId));
							onGestureMoveEvent((int)me.getX(mPointerId), (int)me.getY(mPointerId), me.getEventTime(), false  /*isMajorEvent*/ , null);
						}

					}
				} else {
					for (int h = 0; h <= historicalSize; h++) {
						final float historicalX = me.getHistoricalX(pointerIndex, h);
						final float historicalY = me.getHistoricalY(pointerIndex, h);
						final long historicalTime = me.getHistoricalEventTime(h);
						mGestureStrokeWithPreviewPoints.addPoint(historicalX, historicalY);
						onGestureMoveEvent((int)historicalX, (int)historicalY, historicalTime, false  /*isMajorEvent*/ , null);
					}
				}
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();

			}


		}
		onMoveEventInternal(x, y, eventTime);
	}

	private void onMoveEventInternal(final int x, final int y, final long eventTime) {		
		final int lastX = mLastX;
		final int lastY = mLastY;
		final Key oldKey = mCurrentKey;
		Key key = onMoveKey(x, y);
		if (key != null && sShouldHandleGesture && key.codes[0] != KPTConstants.KEYCODE_SPACE) {
			// Register move event on gesture tracker.
			onGestureMoveEvent(x, y, eventTime, true /* isMajorEvent */, key);
			if (sInGesture) {
				mTimerProxy.cancelLongPressTimer();
				mCurrentKey = null;
				setReleasedKeyGraphics(oldKey);
				return;
			}
		}
	    int xDiff = Math.abs(x - mDownX);
	    int yDiff = Math.abs(y - mDownY);
		// we show first char in bubble window till he moves 30 density independent pixels horizontally
	    if(xDiff > (30 *  KeyboardView.SCREEN_DENSITY ) || yDiff > (30 *  KeyboardView.SCREEN_DENSITY ) )  {
	    	if(!(mDownX == 0 && mDownY == 0)) {
	    		MainKeyboardView.mShowfirstbublechar = false;
	    	}
	    }
		
		//in mini keyboard when the current key is either right chevron or left chevron
		//change the mini keyboard page
		if(mIsShowingMoreKeysPanel && key != null && key.label != null &&    
				(key.label.equals(KeyboardView.mRightChevronIconText) 
				|| key.label.equals(KeyboardView.mLeftChevronIconText))) {
			mTimerProxy.showNextSetOfPopupCharacters(MotionEvent.ACTION_MOVE);
			return;
		}
		
		if (key != null) {
			if (oldKey == null) {

				// The pointer has been slid in to the new key, but the finger was not on any keys.
				// In this case, we must call onPress() to notify that the new key is being pressed.
				// This onPress call may have changed keyboard layout. Those cases are detected at
				// {@link #setKeyboard}. In those cases, we should update key according to the
				// new keyboard layout.
				if (callListenerOnPressAndCheckKeyboardLayoutChange(key)) {
					key = onMoveKey(x, y);
				}
				onMoveToNewKey(key, x, y);
				checkKeyToStartDoubleTapTimer(key);
				startLongPressTimer(key);
				setPressedKeyGraphics(key, eventTime);
			} else if (isMajorEnoughMoveToBeOnNewKey(x, y, eventTime, key)) {
				// The pointer has been slid in to the new key from the previous key, we must call
				// onRelease() first to notify that the previous key has been released, then call
				// onPress() to notify that the new key is being pressed.
				setReleasedKeyGraphics(oldKey);
				callListenerOnRelease(oldKey, oldKey.codes[0], true);
				startSlidingKeyInput(oldKey);
				mTimerProxy.cancelKeyTimers();
				startRepeatKey(key);
				if (mIsAllowedSlidingKeyInput) {
					// This onPress call may have changed keyboard layout. Those cases are detected
					// at {@link #setKeyboard}. In those cases, we should update key according
					// to the new keyboard layout.
					if (callListenerOnPressAndCheckKeyboardLayoutChange(key)) {
						key = onMoveKey(x, y);
					}
					onMoveToNewKey(key, x, y);
					checkKeyToStartDoubleTapTimer(key);
					startLongPressTimer(key);
					setPressedKeyGraphics(key, eventTime);
				} else {
					// HACK: On some devices, quick successive touches may be reported as a sudden
					// move by touch panel firmware. This hack detects such cases and translates the
					// move event to successive up and down events.
					// TODO: Should find a way to balance gesture detection and this hack.
					if (sNeedsPhantomSuddenMoveEventHack
							&& getDistance(x, y, lastX, lastY) >= mPhantonSuddenMoveThreshold) {

						onUpEventInternal(eventTime);
						onDownEventInternal(x, y, eventTime);
					}
					// HACK: On some devices, quick successive proximate touches may be reported as
					// a bogus down-move-up event by touch panel firmware. This hack detects such
					// cases and breaks these events into separate up and down events.
					else if (sNeedsProximateBogusDownMoveUpEventHack
							/*&& sTimeRecorder.isInFastTyping(eventTime)*/
							&& mBogusMoveEventDetector.isCloseToActualDownEvent(x, y)) {
						onUpEventInternal(eventTime);
						onDownEventInternal(x, y, eventTime);
					} else {
						// HACK: If there are currently multiple touches, register the key even if
						// the finger slides off the key. This defends against noise from some
						// touch panels when there are close multiple touches.
						// Caveat: When in chording input mode with a modifier key, we don't use
						// this hack.
						if (getActivePointerTrackerCount() > 1 && sPointerTrackerQueue != null
								&& !sPointerTrackerQueue.hasModifierKeyOlderThan(this)) {
							onUpEvent(x, y, eventTime);
							mKeyAlreadyProcessed = true;
						}
						if (!mIsDetectingGesture) {
							mKeyAlreadyProcessed = true;
						}
						setReleasedKeyGraphics(oldKey);
					}
				}
			}
		} else {
			if (oldKey != null && isMajorEnoughMoveToBeOnNewKey(x, y, eventTime, key)) {
				// The pointer has been slid out from the previous key, we must call onRelease() to
				// notify that the previous key has been released.
				setReleasedKeyGraphics(oldKey);
				callListenerOnRelease(oldKey, oldKey.codes[0], true);
				startSlidingKeyInput(oldKey);
				mTimerProxy.cancelLongPressTimer();
				if (mIsAllowedSlidingKeyInput) {
					onMoveToNewKey(key, x, y);
				} else {
					if (!mIsDetectingGesture) {
						mKeyAlreadyProcessed = true;
					}
				}
			}
		}
	}

	public void onUpEvent(final float x, final float y, final long eventTime) {
		final PointerTrackerQueue queue = sPointerTrackerQueue;
		if (queue != null) {
			if (!sInGesture) {
				if (mCurrentKey != null && mCurrentKey.isModifierKey()) {
					// Before processing an up event of modifier key, all pointers already being
					// tracked should be released.
					queue.releaseAllPointersExcept(this, eventTime);
				} else {
					queue.releaseAllPointersOlderThan(this, eventTime);
				}
			}
		}
		onUpEventInternal(eventTime);
		if (queue != null) {
			queue.remove(this);
		}
	}

	private void onUpEventInternal(final long eventTime) {
		mTimerProxy.cancelKeyTimers();
		resetSlidingKeyInput();
		mIsDetectingGesture = false;
		final Key currentKey = mCurrentKey;
		mCurrentKey = null;
		mDownKey = null;
		// Release the last pressed key.
		setReleasedKeyGraphics(currentKey);

		//if the continuous space has been started on key down start it on action up
		if(mSpaceLongPressStarted) {
			mSpaceLongPressStarted = false;
			mListener.onKey(KPTConstants.KEYCODE_SPACE_LONGPRESS_END, null, false);
		}
		
		if(KeyboardView.mSpaceSwypeStart) {
			mTimerProxy.showSpaceLanguageChangeIcon(this, MotionEvent.ACTION_UP);
		}
		
		boolean mIsRightOrLeftChevronKey = false; 
		if (mIsShowingMoreKeysPanel) {
			if(currentKey != null && currentKey.label != null &&    
					(currentKey.label.equals(KeyboardView.mRightChevronIconText) 
							|| currentKey.label.equals(KeyboardView.mLeftChevronIconText))) {
				mIsRightOrLeftChevronKey = true;
			}
			
			if(!mIsRightOrLeftChevronKey) {
				mDrawingProxy.dismissMoreKeysPanel();
			}
		}

		if (sInGesture) {
			boolean anyShortCutDetected = false;
			//check if any special operation has been started
			if(mDownStartedOnSpecialKey) {
				anyShortCutDetected = KPTGlideKBShortcuts.getInstance().
									detectShortcut(getKeyOn(mDownX, mDownY), getKeyOn((int)mLastX, (int)mLastY));
			}
			if (currentKey != null) {
				callListenerOnRelease(currentKey, currentKey.codes[0], true);
			}
			mayEndBatchInput(eventTime);

			if(anyShortCutDetected) {
				return;
			}
			
			mCurrentTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_GLIDE;
			mTimerProxy.setCurrentTextEntryType(mCurrentTextEntryType);
			mListener.onGlideCompleted(mGestureStrokeWithPreviewPoints.getmFloatXCoords()
					, mGestureStrokeWithPreviewPoints.getmFloatYCoords());
			mTimerProxy.setPreviousTextEntryType(mCurrentTextEntryType);

			return;
		}

		//TODO in Product place this check below sInGesture condition as in LIME. 
		if (mKeyAlreadyProcessed) {
			return;
		}
		
		if (mIsDoubleTapedSpaceKey) {
			mIsDoubleTapedSpaceKey = false;
			setCurrentKeyState(KEY_CURRENT_STATE_SINGLE_TAP);
			if(currentKey != null) {
				mListener.onKey(KPTConstants.KEYCODE_SPACE_DOUBLE_TAP, currentKey.codes, false);
			}
			return;
		}
		
		//commenting the !currentKey.isRepeatable() because delete will also be handled as normal tap in up
		//see onRegisterKey() comments  
		if (currentKey != null /*&& !currentKey.isRepeatable()*/) {
			mCurrentTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_NORMAL;
			mTimerProxy.setCurrentTextEntryType(mCurrentTextEntryType);
			detectAndSendKey(currentKey);	
			mTimerProxy.setPreviousTextEntryType(mCurrentTextEntryType);
		}
		
		//reset the flag. 
		if(isSecondaryCharAlreadyCommitted) {
			isSecondaryCharAlreadyCommitted = false;
			return;
		}
				
		if(mIsShowingMoreKeysPanel && !mIsRightOrLeftChevronKey) {
			mDrawingProxy.resetGlideInputType();
			mIsShowingMoreKeysPanel = false;
		}
	}

	public void onShowMoreKeysPanel(final float x, final float y, final KeyEventHandler handler, final Key parentKey) {
		onLongPressed();
		mIsShowingMoreKeysPanel = true;
		onDownEvent(x, y, SystemClock.uptimeMillis(), handler);
	}
	
	public void onLongPressed() {
		mKeyAlreadyProcessed = true;
		setReleasedKeyGraphics(mCurrentKey);
		final PointerTrackerQueue queue = sPointerTrackerQueue;
		if (queue != null) {
			queue.remove(this);
		}
	}

	public void onCancelEvent(final float x, final float y, final long eventTime) {

		final PointerTrackerQueue queue = sPointerTrackerQueue;
		if (queue != null) {
			queue.releaseAllPointersExcept(this, eventTime);
			queue.remove(this);
		}
		onCancelEventInternal();
	}

	private void onCancelEventInternal() {
		mTimerProxy.cancelKeyTimers();
		setReleasedKeyGraphics(mCurrentKey);
		resetSlidingKeyInput();
	}

	private Key onDownKey(final int x, final int y, final long eventTime) {
		mDownTime = eventTime;
		//TP 15438 When miniKeyboard is being displayed on the keyboard it should not take the downX and downY of the minikeyboard
		//still it should consider the mainkeyboard downX,downY . 
		if(!mIsShowingMoreKeysPanel) {
			mDownX = x;
			mDownY = y;
		}
		mBogusMoveEventDetector.onDownKey();
		mDownKey = onMoveToNewKey(onMoveKeyInternal(x, y), x, y); 
		return mDownKey;
	}

	private Key onMoveKeyInternal(final int x, final int y) {
		mBogusMoveEventDetector.onMoveKey(getDistance(x, y, mLastX, mLastY));
		mLastX = x;
		mLastY = y;
		return mKeyDetector.detectHitKey(x, y);
	}

	private Key onMoveKey(final int x, final int y) {
		return onMoveKeyInternal(x, y);
	}

	private Key onMoveToNewKey(final Key newKey, final int x, final int y) {
		mCurrentKey = newKey;
		mKeyX = x;
		mKeyY = y;
		return newKey;
	}

	private void startRepeatKey(final Key key) {
		if (key != null && key.isRepeatable() && !sInGesture) {
			setMultiTap(false);
			mCurrentTextEntryType = KPTConstants.TEXT_ENTRY_TYPE_NORMAL;
			mTimerProxy.setCurrentTextEntryType(mCurrentTextEntryType);
			onRegisterKey(key, false);
			mTimerProxy.startKeyRepeatTimer(this);
		}
	}
	
	private void startMultiTap() {
		mTimerProxy.startMultiTapKey(this);	
	}
	
	private static int getActivePointerTrackerCount() {
		return (sPointerTrackerQueue == null) ? 1 : sPointerTrackerQueue.size();
	}

	private void mayStartBatchInput(final Key key) {
		if (sInGesture || !mGestureStrokeWithPreviewPoints.isStartOfAGesture()) {
			return;
		}
		if (key == null /*|| !Character.isLetter(key.codes[0])*/) {
			return;
		}

		sInGesture = true;
		synchronized (sAggregratedPointers) {
			sAggregratedPointers.reset();
			sLastRecognitionPointSize = 0;
			sLastRecognitionTime = 0;
			//mListener.onStartBatchInput();
		}
		//if(sPointerTrackerQueue != null) {
			final boolean isOldestTracker = sPointerTrackerQueue != null && sPointerTrackerQueue.getOldestElement() == this;
			mDrawingProxy.showGesturePreviewTrail(this, isOldestTracker);
		//}
	}

	private void mayUpdateBatchInput(final long eventTime, final Key key) {
		if (key != null) {
			synchronized (sAggregratedPointers) {
				final GestureStroke stroke = mGestureStrokeWithPreviewPoints;
				stroke.appendIncrementalBatchPoints(sAggregratedPointers);
				final int size = sAggregratedPointers.getPointerSize();
				if (size > sLastRecognitionPointSize
						&& stroke.hasRecognitionTimePast(eventTime, sLastRecognitionTime)) {
					sLastRecognitionPointSize = size;
					sLastRecognitionTime = eventTime;

					// mListener.onUpdateBatchInput(sAggregratedPointers);
				}
			}
		}
		final boolean isOldestTracker = sPointerTrackerQueue != null && sPointerTrackerQueue.getOldestElement() == this;
		mDrawingProxy.showGesturePreviewTrail(this, isOldestTracker);
	}

	private void mayEndBatchInput(final long eventTime) {
		synchronized (sAggregratedPointers) {
			mGestureStrokeWithPreviewPoints.appendAllBatchPoints(sAggregratedPointers);
			if (getActivePointerTrackerCount() == 1) {
				sInGesture = false;
				sTimeRecorder.onEndBatchInput(eventTime);
				//mListener.onEndBatchInput(sAggregratedPointers);
			}
		}
		final boolean isOldestTracker = sPointerTrackerQueue != null && sPointerTrackerQueue.getOldestElement() == this;
		mDrawingProxy.showGesturePreviewTrail(this, isOldestTracker);
	}

	private void startSlidingKeyInput(final Key key) {
		if (!mIsInSlidingKeyInput) {
			mIsInSlidingKeyInputFromModifier = key.isModifierKey();
		}
		mIsInSlidingKeyInput = true;
	}

	// Note that we need primaryCode argument because the keyboard may be in shifted state and the
	// primaryCode is different from {@link Key#mCode}.
	private void callListenerOnRelease(final Key key, final int primaryCode,
			final boolean withSliding) {
		if (sInGesture) {
			return;
		}
		final boolean ignoreModifierKey = mIsInSlidingKeyInputFromModifier && key.isModifierKey();


		if (ignoreModifierKey) {
			return;
		}
		
		//TODO: Place key.isEnabled() method	
		mListener.onRelease(primaryCode);
	}

	private boolean isMajorEnoughMoveToBeOnNewKey(final int x, final int y, final long eventTime,
			final Key newKey) {
		if (mKeyDetector == null) {
			throw new NullPointerException("keyboard and/or key detector not set");
		}
		final Key curKey = mCurrentKey;
		if (newKey == curKey) {
			return false;
		} else if (curKey != null) {
			final int keyHysteresisDistanceSquared = mKeyDetector.getKeyHysteresisDistanceSquared(
					mIsInSlidingKeyInputFromModifier);
			final int distanceFromKeyEdgeSquared = curKey.squaredDistanceToEdge(x, y);
			if (distanceFromKeyEdgeSquared >= keyHysteresisDistanceSquared) {

				return true;
			}
			if (sNeedsProximateBogusDownMoveUpEventHack && !mIsAllowedSlidingKeyInput
					/* && sTimeRecorder.isInFastTyping(eventTime)*/
					&& mBogusMoveEventDetector.hasTraveledLongDistance(x, y)) {

				return true;
			}
			return false;
		} else { // curKey == null && newKey != null
			return true;
		}
	}

	/**
	 * start the long press timer
	 * @param key
	 */
	private void startLongPressTimer(final Key key) {
		if (key != null &&  /*key.isLongPressEnabled()*/ true && !sInGesture) {
			mTimerProxy.startLongPressTimer(this);
		}
	}
	
	/**
	 * <p>call detectAndSendKey only when the delete key long press is detected.
	 * Note that this method is called from downevent and longpress. So during long press it will be called twice.
	 * (since detecting long press starts from down event)</p>
	 * 
	 * <p>According to our feature, when delete is long pressed(KEYCODE_DELETE_LONGPRESS) the complete word has to be deleted.
	 * But during long press this will be called from down also (KEYCODE_DELETE), so first a char will be 
	 * deleted and then complete word will be deleted, which should not be the case. </p>
	 * 
	 * So in down just start the handler
	 *  
	 * @param key
	 * @param from
	 */
	public void onRegisterKey(final Key key, boolean isDetectedFromLongPress) {
		if (key != null) {
			if(isDetectedFromLongPress) {
				detectAndSendKey(key);
				mTimerProxy.setPreviousTextEntryType(mCurrentTextEntryType);
			}
			mTimerProxy.startTypingStateTimer(key);
		}
	}
	
	private CharSequence getConjuctionText(Key key){
		
		if(!isConjuctionKey(key)){
			return null;
		}
		
		if(mKeyCurrentState == KEY_CURRENT_STATE_SINGLE_TAP && key.label!=null && key.codes[0] == KPTConstants.KEYCODE_CONJUNCTION) {
			return key.label;
		} else if(key.codes.length == 2 && mKeyCurrentState == KEY_CURRENT_STATE_LONG_PRESS && key.label2!=null && key.codes[1] == KPTConstants.KEYCODE_CONJUNCTION) {
			return key.label2;
		}else if(key.codes.length == 1 && mKeyCurrentState == KEY_CURRENT_STATE_MINI_KEYBOARD && key.codes[0] == KPTConstants.KEYCODE_CONJUNCTION){
			return key.label;
		}
		else{
			
			return null;
		}
		
	}
	
	private boolean isConjuctionKey(Key key){
		
		if(key ==null || key.codes==null){
			return false;
		}
		
		if(key.codes.length == 2 && (key.codes[0] == KPTConstants.KEYCODE_CONJUNCTION || key.codes[1] == KPTConstants.KEYCODE_CONJUNCTION ) ){
			return true;
		}
		
		if(key.codes.length == 1 && key.codes[0] == KPTConstants.KEYCODE_CONJUNCTION ){
			return true;
		}
		
		return false;
	}

	/**
	 * this methods sends the key code to the IME.
	 * 
	 * @param key 
	 */
	private void detectAndSendKey(final Key key) {

		if (key == null) {
			return;
		}
		
		//if the secondary char is already committed in down event during long press,
		//then return so that the char is not inserted again in action_up
		if(isSecondaryCharAlreadyCommitted) {
			isSecondaryCharAlreadyCommitted = false;
			return;
		}
		
		if(KeyboardView.mSpaceSwypeStart) {
			KeyboardView.mSpaceSwypeStart = false;
			return;
		}
		
		//in mini keyboard when the current key is either right chevron or left chevron
		//change the mini keyboard page
		if(mIsShowingMoreKeysPanel && key != null && key.label != null &&    
				(key.label.equals(KeyboardView.mRightChevronIconText) 
						|| key.label.equals(KeyboardView.mLeftChevronIconText))) {
			mTimerProxy.showNextSetOfPopupCharacters(MotionEvent.ACTION_MOVE);
			return;
		}
				
		int code = key.codes[0];
		if (mIsQwertyKeyboard) {
			if(key.text != null && !isConjuctionKey(key)) {
				mListener.onText(key.text);
			}else if(key.codes[0] == KPTConstants.KEYCODE_CONJUNCTION && AdaptxtIME.mIsHindhi){
				PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
				mCurrentSyllableIndex  = key.syllableIndex;
				mListener.onText(key.label);
			}else if(getConjuctionText(key)!=null ){
				mIsConjuction = true;
				mListener.onText(getConjuctionText(key));
				mIsConjuction = false;
			}
			else if(key.codes[0] == KPTConstants.KEYCODE_ALT && key.popupResId != 0) { 
				//for alt key in indic languages, single tap should bring the mini keyboard
				//so calling longpress timer. In the implementation method set time delay
				//with zero sec, so a mini keyboard will be displayed.
				//This is nothing but calling long press method in single tap
				mTimerProxy.startLongPressTimerNoDelay(this, key);
			} else {
				//based on the current key state set the keycode
				if(mKeyCurrentState == KEY_CURRENT_STATE_SINGLE_TAP) {
					code = key.codes[0];
				} else if(mKeyCurrentState == KEY_CURRENT_STATE_LONG_PRESS) {
					code = getKeySecondaryCharCodeOnLongPress(key); 
				} else {
					//this case is mostly for mini keyboard which is handle in the
					//mini keyboard view
				}

				//reset current state 
				setCurrentKeyState(KEY_CURRENT_STATE_SINGLE_TAP);
				PointerTracker.mPreviousSyllableIndex = PointerTracker.mCurrentSyllableIndex;
				mCurrentSyllableIndex  = key.syllableIndex;
				
				mListener.onKey(code, key.codes, false);
			}
		}else{
			final boolean isToogleLayoutKey = isToogleLayoutKey(key);
			if (isToogleLayoutKey) {
				mTimerProxy.toogle12KeySymbolsLayout(this);
				return;
			}
			
			if (isInMultiTap()) {
				int[] codes = new int[KPTConstants.MAX_NEARBY_KEYS];
				Arrays.fill(codes, KeyboardView.NOT_A_KEY);

				int tapCount = getMultiTapCount();  
				boolean flag = false;

				if (tapCount != -1) {
					flag  = true;
				} else {
					tapCount = 0;
				}

				if (key.codes.length > tapCount) {
					code = key.codes[tapCount];
				}

				if(flag) {
					codes[0] = code;
					mListener.onKey(Keyboard.KEYCODE_REPLACE, codes, false);
				} else {
					if(mKeyCurrentState == KEY_CURRENT_STATE_LONG_PRESS) {
						code = getKeySecondaryCharCodeOnLongPress(key); 
					}
					mListener.onKey(code, codes, false);
				}
			}else{
				if(mKeyCurrentState == KEY_CURRENT_STATE_LONG_PRESS) {
					code = getKeySecondaryCharCodeOnLongPress(key); 
				}
				
				//reset current key state
				setCurrentKeyState(KEY_CURRENT_STATE_SINGLE_TAP);
				mListener.onKey(code, key.codes, false);
			}
		}
	}

	/**
	 * change the language when the user glides the finger on space key
	 * @param direction the direction in which the space key is swyped,
	 * 		based on the direction set the previous or next language.
	 */
	public void changeLanguageFromSpaceKey(final int direction) {
		mListener.onKey(direction == 1 ? KPTConstants.KEYCODE_NEXT_LANGUAGE 
				: KPTConstants.KEYCODE_PREV_LANGUAGE, null, false);
	}
	
	/**
	 * when a key is long pressed if there are no popup chars directly commit secondary char into editor
	 *  or else display the mini keyboard.<br><br>
	 *  
	 *  This method commits the secondary char into the editor
	 */
	public void insertSecondaryCharOnLongPress() {
		final Key key = mCurrentKey;
		detectAndSendKey(key);
		isSecondaryCharAlreadyCommitted = true;
	}
	
	/**
	 * this method returns what action needs to be done when a key is long pressed<br><br>
	 * 
	 *  <p>For Example, when enter key is long pressed clipboard needs to be shown. When space is long pressed in few
	 *  layouts continuous space needs to be inserted. <br>
	 *  this method checks such conditions<p>
	 *  
	 * @param key key that is long pressed
	 * @return what action needs to be done for this key
	 */
	private int getKeySecondaryCharCodeOnLongPress(final Key key) {
		int keycode = key.codes.length == 1 ? key.codes[0] : key.codes[1] ;
		switch(key.codes[0]){
		case KPTConstants.KEYCODE_MIC:
			keycode = KPTConstants.KEYCODE_MIC;
			break;
		case KPTConstants.KEYCODE_ENTER:
			keycode = KPTConstants.KEYCODE_ENTER;
			break;
		case KPTConstants.KEYCODE_DELETE:
			keycode = KPTConstants.KEYCODE_DELETE_LONGPRESS;
			break;
		case KPTConstants.KEYCODE_ADAPTXT:
			keycode = KPTConstants.KEYCODE_LAUNCH_SETTINGS;
		case KPTConstants.KEYCODE_PHONE_MODE_SYM : 
		case KPTConstants.KEYCODE_JUMP_TO_TERTIARY:
		case KPTConstants.KEYCODE_MODE_CHANGE:
			keycode = KPTConstants.KEYCODE_LAUNCH_QUICK_SETTINGS_DIALOG;
			break;
		case KPTConstants.KEYCODE_SHIFT:
			keycode = KPTConstants.KEYCODE_SHIFT_LONGPRESS;
			break;
		case KPTConstants.KEYCODE_SHIFTINDIC:
			keycode = KPTConstants.KEYCODE_SHIFTINDIC;
			break;
		case KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE: 
			//keycode = KPTConstants.KEYCODE_LAUNCH_SHARE_DIALOG;
			break;
		case KPTConstants.KEYCODE_PERIOD :
		case KPTConstants.KEYCODE_THAI_COMMIT:
			if(key.codes.length == 1 
					&& (mIsQwertyKeyboard 
							&& mCurrentQWERTYKeyboardType != KPTKeyboardSwitcher.MODE_QWERTY_NUMERIC
							&& mCurrentQWERTYKeyboardType != KPTKeyboardSwitcher.MODE_QWERTY_NUMERIC_SYMBOLS)){
				keycode = KPTConstants.KEYCODE_LAUNCH_SHARE_DIALOG;
			}
			break;
		case KPTConstants.KEYCODE_DANDA :
			keycode = KPTConstants.KEYCODE_DANDA;
			break;
		case KPTConstants.KEYCODE_SPACE:
			if(!mIsQwertyKeyboard && mCurrentPHONEKeyboardType == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET) {
				boolean shouldDisplayAccents = mShouldDisplayAccents;
				// Extended bubbles should be displayed in secondary and teritiary layouts even if extended bubble is disabled in settings screen.
				if(!mIsShowingMoreKeysPanel){
					shouldDisplayAccents = shouldDisplayAccents ? shouldDisplayAccents : !isAlphabetMode();
				}

				if (shouldDisplayAccents) {
					if (key.popupResId != 0){
						if (key.codes.length > 1) {
							keycode = key.codes[1];
						}
					}
				} else {
					if (key.codes.length > 1) {
						keycode = key.codes[1];
					}
				}
			} else {
				//when the space swype is started don't insert continous spaces
				if(!KeyboardView.mSpaceSwypeStart) {
					keycode = KPTConstants.KEYCODE_SPACE_LONGPRESS_START;
					mSpaceLongPressStarted = true;
				} 
			}
			break;
		default:
			if (key.codes[0] == '0' 
			&& (mCurrentQWERTYKeyboardType == KPTKeyboardSwitcher.QWERTY_MODE_PHONE 
			|| mCurrentPHONEKeyboardType == KPTKeyboardSwitcher.PHONE_MODE_SYMBOLS)) {
				// Long pressing on 0 in phone number keypad gives you a '+'.
				keycode = '+';
			} else {
				// TP 8108, handling 2ndary key chars in 12 key/phone keypad layout when accentuated bubble is disabled
				//If codes length > 1 and if need not display accented characters
				//then simulate the secondary character. (Only in alphabet mode)
				boolean shouldDisplayAccents = mShouldDisplayAccents;

				// Extended bubbles should be displayed in secondary and teritiary layouts even if extended bubble is disabled in settings screen.
				if(!mIsShowingMoreKeysPanel){
					if(key.codes[0] == KPTConstants.KEYCODE_PERIOD 
							|| key.codes[0] == KPTConstants.KEYCODE_AT 
							|| key.codes[0]== KPTConstants.KEYCODE_DANDA){
						shouldDisplayAccents = true;
					}else{
						shouldDisplayAccents = shouldDisplayAccents ? shouldDisplayAccents : !isAlphabetMode();
					}
				}

				if(key.codes.length > 1 
						&& (key.popupResId == 0 || !shouldDisplayAccents)){
					// TP 8108
					if(mIsQwertyKeyboard) {
						if(key.codes[1] == -7){
							mListener.onText(":-)");
						}else{
							keycode = key.codes[1];
						}
					}else{
						// if only 2 keycodes are there, pick up the 2nd one
						if(key.codes.length == 2){
							keycode = key.codes[1];
						} else {
							keycode = (int)key.label2.charAt(0);
						}
					}
				}
			}
			break;
		}
		return keycode;
	}
	
	/**
	 * this will return true ONLY if the current keypad is EITHER QWERTY 
	 * PRIMARY or PHONE PRIMARY
	 * @return
	 */
	public boolean isAlphabetMode() {
		return (mCurrentQWERTYKeyboardType == KPTKeyboardSwitcher.MODE_QWERTY_ALPHABET
				|| mCurrentPHONEKeyboardType == KPTKeyboardSwitcher.MODE_PHONE_ALPHABET);
	}
	
	public boolean isMiniKeyboardDisplayed() {
		return mIsShowingMoreKeysPanel;
	}
	
	/**
	 * check if the current key is a special key
	 * @param key
	 * @return
	 */
	private boolean checkIfSpecialKey(final Key key) {
		if(key == null) {
			return false;
		}
		/*Log.e("kpt","is special key--->"+(key.type == Key.KEY_TYPE_FUNCTIONAL_SEND_CORE
				|| key.type == Key.KEY_TYPE_FUNCTIONAL_DONT_SEND_CORE));*/
		return (key.type == Key.KEY_TYPE_FUNCTIONAL_SEND_CORE
				|| key.type == Key.KEY_TYPE_FUNCTIONAL_DONT_SEND_CORE);
	}
	
	private void resetSlidingKeyInput() {
		mIsInSlidingKeyInput = false;
		mIsInSlidingKeyInputFromModifier = false;
	}

	@Override
	public boolean isModifier() {
		return mCurrentKey != null && mCurrentKey.isModifierKey();
	}

	@Override
	public boolean isInSlidingKeyInput() {
		return mIsAllowedSlidingKeyInput;
	}

	@Override
	public void onPhantomUpEvent(long eventTime) {
		 onUpEventInternal(eventTime);
	     mKeyAlreadyProcessed = true;
	}

	/**
	 * set the state of the current key. It will always be be either in one of the following state <br><br> 
	 * 
	 * 1) KEY_CURRENT_STATE_SINGLE_TAP,<br>
	 * 2) KEY_CURRENT_STATE_LONG_PRESS,<br>
	 * 3) KEY_CURRENT_STATE_MINI_KEYBOARD<br><br>
	 * 
	 *   these key states are used in sending the keycode to IME(check detectAndSendKey() for usage)<br>
	 *   
	 * @param state
	 */
	public void setCurrentKeyState(final int state) {
		mKeyCurrentState = state;
	}

	public void setMultiTap(final boolean isInMultiTap){
		mIsInMultiTap = isInMultiTap;
	}
	
	/**
	 * return true if the multi tap is enabled
	 * @return
	 */
	public boolean isInMultiTap(){
		return mIsInMultiTap;
	}
	
	public void setMultiTapCount(int count){
		mMultiTapCount = count;
	}
	
	/**
	 * get the current multi tap count
	 * @return
	 */
	public int getMultiTapCount(){
		return mMultiTapCount;
	}
	
	/**
	 * set if the current keyboard is QWERTY or not
	 * @param mIsQwertyKeyboard
	 */
	private void setIsQwertyKeyboard(final boolean mIsQwertyKeyboard) {
		this.mIsQwertyKeyboard = mIsQwertyKeyboard;
	}
	
	private void setCurrentQWERTYKeyboardType(final int currentQWERTYKeyboardType) {
		this.mCurrentQWERTYKeyboardType = currentQWERTYKeyboardType;
	}
	
	private void setCurrentPhoneKeyboardType(final int mCurrentPHONEKeyboardType) {
		this.mCurrentPHONEKeyboardType = mCurrentPHONEKeyboardType;
	}
	
	/**
	 * set if the user has enabled "Ext. Character Bubble" option in settings
	 * @param shouldDisplayAccents
	 */
	private void setIfAccentedBubblesShouldBeDisplayed(final boolean shouldDisplayAccents) {
		this.mShouldDisplayAccents = shouldDisplayAccents;
	}
	
	public static void setInstalledDictionariesCount(final int installedDictionariesCount) {
		mInstalledDictionariesCount = installedDictionariesCount;
	}

	
	/**
	 * Check the key is symbols change mode key(called 1/6 key) or not.
	 * @param keyIndex the key index.            
	 * @return true if the pressed key is symbols change mode key.
	 */
	protected boolean isToogleLayoutKey(final Key key) {
		return key.codes[0] == KPTConstants.KEYCODE_PHONE_SYM_PAGE_CHANGE;
	}
	
	private void checkKeyToStartDoubleTapTimer(final Key key){
		if (key.codes[0] == KPTConstants.KEYCODE_SPACE) {
			stateOfDoubleTapTimer();
		}else{
			mIsDoubleTapedSpaceKey = false;
			cancelDoubleTapTimer();
		}
	}
	
	private void stateOfDoubleTapTimer(){
		final boolean  isInDoubleTapTimeout = isInDoubleTapTimeout();
		if (!isInDoubleTapTimeout) {
			// This is first tap.
			mIsDoubleTapedSpaceKey = false;
			startDoubleTapTimer();
		}
		if (isInDoubleTapTimeout) {
			// call listner interface.
			mIsDoubleTapedSpaceKey = true;
		}
	}
	private void startDoubleTapTimer(){
		mTimerProxy.startDoubleTapTimer();
	}
	
	private void cancelDoubleTapTimer(){
		mTimerProxy.cancelDoubleTapTimer();
	}
	
	private boolean isInDoubleTapTimeout(){
		return mTimerProxy.isInDoubleTapTimeout();
	}

	public void setSlidingLocaleStart(boolean isStarted) {
		mIsSpaceSlidingLocaleStarted = isStarted;
	}
	
	private boolean isSlidingSpaceLocaleStarted() {
		return mIsSpaceSlidingLocaleStarted;
	}
}
