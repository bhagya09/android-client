package com.bsb.hike.media;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

/**
 * @author Gauraw Negi
 *
 *         this class is alternate approach of keyboard , sticker and emojji handling
 *
 *         Our main focus in keyboard issues is to hold EditText in is position while keyboard and emojji are change their appearance while other is present We are using
 *         OnPreDrawListener for fulfill this task We are also using state to select which task should perform.
 * 
 *         <pre>
 * 			STATE 1:
 * 			Click on Sticker icon or emojji icon =>
 *          * updatePadding 
 *          * set state=STICKER
 *          
 *          STATE 2:
 *          Click on EditText First Time =>  
 *          * handle by system
 *          
 *          STATE 3:
 *          Click on Sticker icon or emojji icon when keyboard is present =>
 *          * state=KEYBOARD_STICKER
 *          * hideKeyboard 
 *          * set preDrawTaskFlag =true
 *          * updatePadding when keyboard disappear completely
 *          
 *          STATE 4:
 *          Click on Edittext when either Sticker icon or emojji is present// Or Click on Sticker icon or emojji icon atfer state=KEYBOARD_STICKER =>
 *          * openKeyboard if icon is clicked
 *          * set preDrawTaskFlag =true
 *          * when its keyboard opened hide popupWindow and update padding to originalBottomPadding
 * </pre>
 * 
 */
public class KeyboardPopupLayout21 extends KeyboardPopupLayout
{
	private static final int KEYBOARD_CONFIGURATION_OLD = 1;

	private static final int KEYBOARD_CONFIGURATION_NEW = 2;

	private static final int STATE_NONE = 0;// not keyboard and sticker present

	private static final int STATE_STICKER = 2;// only sticker or emoji pad

	private static final int STATE_KEYBOARD_STICKER = 3;// Sticker after keyboard

	private static final int STATE_STICKER_KEYBOARD = 4;// keyboard after keyboard

	private static final int ON_PRE_DRAW_MAX_WAIT_COUNT = 200;// wait count util task will be finished

	private OnSoftKeyboardListener onSoftKeyboardListener;

	private boolean preDrawTaskFlag;

	private boolean showKeyboardAfterPopupDismiss;

	private int state = 0; // current input-methods states

	private int countUtilKeyboardIsNotOpen = 0;// reset on every state change

	private View lastClickedEditText;// hold EditText through which keyboard opened last time

	private int lastScreenOrientation;

	private boolean disableCalculateKeyboardHeightTask;// we will not store keyboard height if this flag is ture

	/**
	 * 
	 * @param mainView
	 *            - This should be top most view of your activity which get resized when soft keyboard is toggled
	 * @param firstTimeHeight
	 *            - This is the height which will be used before keyboard opens
	 * @param context
	 * @param editText
	 *            For which keyboard working
	 */
	public KeyboardPopupLayout21(View mainView, int firstTimeHeight, Context context, PopupListener listener, OnSoftKeyboardListener onSoftKeyboardListener)
	{
		super(mainView, firstTimeHeight, context, listener);
		registerOnPreDrawListener();
		registerOnGlobalLayoutListener();
		setOnSoftKeyboardListener(onSoftKeyboardListener);
		lastScreenOrientation = context.getResources().getConfiguration().orientation;
	}

	public KeyboardPopupLayout21(View mainView, int firstTimeHeight, Context context, int[] eatTouchEventViewIds, PopupListener listener,
			OnSoftKeyboardListener onSoftKeyboardListener)
	{
		this(mainView, firstTimeHeight, context, listener, onSoftKeyboardListener);
		this.mEatTouchEventViewIds = eatTouchEventViewIds;
	}

	private void registerOnPreDrawListener()
	{
		mainView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
	}

	@Override
	protected void registerOnGlobalLayoutListener()
	{
		if (mGlobalLayoutListener21 != null)
		{
			mainView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener21);
		}
	}

	private void updatePadding(int bottomPadding)
	{
		if (mainView != null && mainView.getPaddingBottom() != bottomPadding)
		{
			Logger.i("chatthread", "resize main height with bottom padding " + bottomPadding);
			mainView.setPadding(0, 0, 0, bottomPadding);
		}
	}

	public boolean showKeyboardPopup(View view)
	{

		if (mainView == null || mainView.getWindowToken() == null)
		{
			String errorMsg = "Inside method : showKeyboardPopup of KeyboardPopupLayout. Is view null" + (mainView == null);
			if (mainView != null)
			{
				errorMsg += " is WindowToken Null : " + (mainView.getWindowToken() == null);
			}

			HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
			Logger.wtf("chatthread", "window token is null or view itself is null! Cannot show sticker/emoticons. Eating this exception");
			return false;
		}

		boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		int height = islandScape ? possibleKeyboardHeightLand : possibleKeyboardHeight;
		if (height == 0 && isKeyboardOpen)
		{
			height = calculateKeyboardHeight();
		}
		if (height == 0)
		{
			if (islandScape)
			{
				int maxHeight = mainView.getRootView().getHeight();
				// giving half height of screen in landscape mode
				Logger.i("chatthread", "landscape mode is on setting half of screen " + maxHeight);
				height = (maxHeight) / 2;
			}

			else
			{
				height = firstTimeHeight;
			}
		}

		if (popup == null)
		{
			initPopUpWindow(LayoutParams.MATCH_PARENT, height, view, context, PopupWindow.INPUT_METHOD_NOT_NEEDED);
			// TODO
			// fixLollipopHeadsUpNotifPopup(popup);

			// this is a strange bug in Android, if we set focusable true, GRAVITY BOTTOM IS NOT working
			popup.setFocusable(false);
			popup.setOutsideTouchable(false);
			/**
			 * Conditionally setting the touch interceptor
			 */
			if (null != mEatTouchEventViewIds && mEatTouchEventViewIds.length > 0)
			{
				popup.setTouchInterceptor(this);
			}
		}
		else
		{
			updateInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
		}

		view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
		popup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);

		popup.setHeight(height);
		setOnDismissListener(this);
		if (isKeyboardOpen)
		{
			setState(STATE_KEYBOARD_STICKER);
		}
		else
		{
			enableCalculateKeyboardHeightTask();
			updatePadding(popup.getHeight());
			setState(STATE_STICKER);
		}
		popup.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
		if (isKeyboardOpen)
		{
			setOnPreDrawTaskFlagOn();
			hideKeyboard();
		}

		return true;
	}

	@Override
	public void dismiss()
	{
		if ((state == STATE_KEYBOARD_STICKER && showKeyboardAfterPopupDismiss) || (preDrawTaskFlag == true && state == STATE_STICKER_KEYBOARD))
		{
			updateInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
			openKeyBoard();
			setState(STATE_STICKER_KEYBOARD);
			setOnPreDrawTaskFlagOn();
			showKeyboardAfterPopupDismiss = false;
		}
		else
		{
			super.dismiss();
		}

	}

	@Override
	public void onDismiss()
	{
		if (state == STATE_STICKER)
		{
			updatePadding(originalBottomPadding);
			setState(STATE_NONE);
		}
		else if (state == STATE_KEYBOARD_STICKER)
		{
			updatePadding(originalBottomPadding);
			setState(STATE_NONE);
		}
		/**
		 * Whenever this pop up is dismissed, we want bottom padding of mainview to be reset
		 */
		if (mListener != null)
		{
			mListener.onPopupDismiss();
		}
	}

	@Override
	public void updateListenerAndView(PopupListener listener, View view)
	{
		super.updateListenerAndView(listener, view);
		registerOnPreDrawListener();
	}

	public boolean isKeyboardOpen()
	{
		return isKeyboardOpen;
	}

	public void releaseResources()
	{
		this.mListener = null;

		/**
		 * Removing the global layout listener
		 */
		if (mainView != null)
		{
			this.mainView.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener21);
			this.mainView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
			this.mainView = null;
		}
	}

	private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = new OnPreDrawListener()
	{

		@Override
		public boolean onPreDraw()
		{
			if (preDrawTaskFlag)
			{
				if (state == STATE_NONE || state == STATE_STICKER)
				{
					preDrawTaskFlag = false;
					return true;
				}
				if (state == STATE_KEYBOARD_STICKER)
				{
					if (!isKeyboardOpen || countUtilKeyboardIsNotOpen >= ON_PRE_DRAW_MAX_WAIT_COUNT)
					{
						// update height when it is confirm that keyboard is not present
						onSoftKeyboardListener.onShown();
						updatePadding((popup != null && popup.isShowing()) ? popup.getHeight() : getHeight());
						preDrawTaskFlag = false;

					}
					countUtilKeyboardIsNotOpen++;
					return false;
				}
				if (state == STATE_STICKER_KEYBOARD)
				{ // wait until keyboard will not open
					if (isKeyboardOpen || countUtilKeyboardIsNotOpen >= ON_PRE_DRAW_MAX_WAIT_COUNT)
					{

						if (popup != null && popup.isShowing())
						{ // dismissing popup because we know that now keyboard had opened
							updatePadding(originalBottomPadding);
							popup.dismiss();
							preDrawTaskFlag = false;
						}

						setState(STATE_NONE);
					}
					countUtilKeyboardIsNotOpen++;
					return false;
				}
			}

			return true;
		}
	};

	private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener21 = new ViewTreeObserver.OnGlobalLayoutListener()
	{

		@Override
		public void onGlobalLayout()
		{
			if (mainView == null)
			{
				Logger.wtf("chatthread", "Getting null view inside global layout listener");
				return;
			}
			int currentScreenOrientation = context.getResources().getConfiguration().orientation;

			if (lastScreenOrientation != currentScreenOrientation)
			{
				lastScreenOrientation = currentScreenOrientation;
				disableCalculateKeyboardHeightTask();
				// we are storing keyboard height on orientation change,wait until it will reopen in same orientation
			}
			Log.i("chatthread", "global layout listener rootHeight " + mainView.getRootView().getHeight() + " new height " + mainView.getHeight());
			Rect r = new Rect();
			mainView.getWindowVisibleDisplayFrame(r);
			// this is height of view which is visible on screen
			int rootViewHeight = mainView.getRootView().getHeight();
			// if (rootViewHeight == r.bottom)
			// { //TODO we can think on it in future
			// disableCalculateKeyboardHeightTask = false;
			// }
			int temp = rootViewHeight - r.bottom;
			Logger.i("chatthread", "keyboard  height " + temp);
			if (temp > 0)
			{
				boolean shouldNotStoreKeyboardHeight = disableCalculateKeyboardHeightTask || (rootViewHeight * 0.9 < temp || rootViewHeight * 0.1 > temp);
				// (rootViewHeight * 0.9 < temp || rootViewHeight * 0.1 > temp): we are considering that, this condition can't occur for any condition so we will not store keyboard
				// height for this condition
				if (!shouldNotStoreKeyboardHeight)
				{
					if (currentScreenOrientation == Configuration.ORIENTATION_LANDSCAPE)
					{
						possibleKeyboardHeightLand = temp;
					}
					else
					{
						possibleKeyboardHeight = temp;
					}
				}
				isKeyboardOpen = true;
				if (isShowing())
				{
					updatePadding(originalBottomPadding);
				}
				updateDimension(LayoutParams.MATCH_PARENT, temp);
			}
			else
			{
				isKeyboardOpen = false;
			}
		}
	};

	private void setOnSoftKeyboardListener(OnSoftKeyboardListener onSoftKeyboardListener)
	{
		this.onSoftKeyboardListener = onSoftKeyboardListener;
	}

	public static boolean shouldShow(Context context)
	{// server side switch
		int kc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KEYBOARD_CONFIGURATION, KEYBOARD_CONFIGURATION_OLD);
		return kc == KEYBOARD_CONFIGURATION_NEW;
	}

	public boolean onEditTextTouch(View v, MotionEvent event)
	{
		if (preDrawTaskFlag)
		{// previous task is running don't accept this event
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			if (popup != null && popup.isShowing())
			{
				updateInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
				setState(STATE_STICKER_KEYBOARD);
				setOnPreDrawTaskFlagOn();
			}
			else
			{
				preDrawTaskFlag = false;
				setState(STATE_NONE);
			}
			lastClickedEditText = v;
		}
		return false;
	}

	private int getHeight()
	{
		boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		int height = islandScape ? possibleKeyboardHeightLand : possibleKeyboardHeight;

		if (height == 0)
		{
			if (islandScape)
			{
				int maxHeight = mainView.getRootView().getHeight();
				height = (maxHeight) / 2;
			}
			else
			{
				height = firstTimeHeight;
			}
		}
		return height;
	}

	private void setState(int state)
	{
		this.state = state;
	}

	private void setOnPreDrawTaskFlagOn()
	{
		countUtilKeyboardIsNotOpen = 0;
		preDrawTaskFlag = true;
	}

	public void showKeyboardAfterPopupDismiss()
	{
		showKeyboardAfterPopupDismiss = true;
	}

	private void hideKeyboard()
	{
		enableCalculateKeyboardHeightTask();
		Utils.hideSoftKeyboard(context, mainView);
	}

	private void openKeyBoard()
	{
		View view = lastClickedEditText;
		if (view == null)
		{
			return;
		}
		Utils.showSoftKeyboard(view, InputMethodManager.SHOW_IMPLICIT);
	}

	private void updateInputMethodMode(int mode)
	{
		if (popup != null && popup.getInputMethodMode() != mode)
		{
			popup.setInputMethodMode(mode);
			popup.update();
		}
	}

	@Override
	public boolean isBusyInOperations()
	{
		return preDrawTaskFlag;
	}

	/**
	 * 
	 * This method will return keyboard height if keyboard is opened and will not store this value. you must call this method while keyboard is still open
	 * 
	 * @return keyboard height
	 */
	private int calculateKeyboardHeight()
	{
		Rect r = new Rect();
		mainView.getWindowVisibleDisplayFrame(r);
		// this is height of view which is visible on screen
		int rootViewHeight = mainView.getRootView().getHeight();
		int temp = rootViewHeight - r.bottom;
		Logger.i("chatthread", "calculateKeyboardHeight :keyboard  height " + temp);

		if (temp > 0)
		{
			if (rootViewHeight * 0.9 < temp || rootViewHeight * 0.1 > temp)
			{
				return 0;
			}
			return temp;
		}
		return 0;
	}

	@Override
	public void onBackPressed()
	{
		enableCalculateKeyboardHeightTask();
	}

	@Override
	public void onCloseKeyBoard()
	{
		super.onCloseKeyBoard();
		enableCalculateKeyboardHeightTask();
	}

	private void disableCalculateKeyboardHeightTask()
	{
		disableCalculateKeyboardHeightTask=true;
	}

	private void enableCalculateKeyboardHeightTask()
	{
		disableCalculateKeyboardHeightTask=false;
	}

}