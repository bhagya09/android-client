package com.bsb.hike.media;

import static com.bsb.hike.HikeConstants.IntentAction.ACTION_KEYBOARD_CLOSED;
import static com.bsb.hike.HikeConstants.IntentAction.ACTION_KEYBOARD_OPEN;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.Logger;

public class KeyboardPopupLayout extends PopUpLayout implements OnDismissListener
{
	protected View mainView;

	protected int possibleKeyboardHeightLand, possibleKeyboardHeight, originalBottomPadding;

	protected boolean isKeyboardOpen;

	protected int firstTimeHeight;

	protected int[] mEatTouchEventViewIds;
	
	protected PopupListener mListener;

	/**
	 * 
	 * @param mainView
	 *            - This should be top most view of your activity which get resized when soft keyboard is toggled
	 * @param firstTimeHeight
	 *            - This is the height which will be used before keyboard opens
	 * @param context
	 */
	public KeyboardPopupLayout(View mainView, int firstTimeHeight, Context context, PopupListener listener)
	{
		super(context);
		this.mainView = mainView;
		this.firstTimeHeight = firstTimeHeight;
		originalBottomPadding = mainView.getPaddingBottom();
		this.mListener = listener;
		registerOnGlobalLayoutListener();
	}

	public KeyboardPopupLayout(View mainView, int firstTimeHeight, Context context, int[] eatTouchEventViewIds, PopupListener listener)
	{
		this(mainView, firstTimeHeight, context, listener);
		this.mEatTouchEventViewIds = eatTouchEventViewIds;
	}

	protected void registerOnGlobalLayoutListener()
	{
		mainView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
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

			/**
			 * Conditionally setting the touch interceptor
			 */
			if (null != mEatTouchEventViewIds && mEatTouchEventViewIds.length > 0)
			{
				popup.setTouchInterceptor(this);
			}
		}

		view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
		popup.setHeight(height);
		setOnDismissListener(this);
		if (isKeyboardOpen)
		{
			updatePadding(0);
		}
		else
		{
			updatePadding(popup.getHeight());
		}
		popup.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
		return true;
	}

	@Override
	public void onDismiss()
	{
		/**
		 * Whenever this pop up is dismissed, we want bottom padding of mainview to be reset
		 */
		updatePadding(originalBottomPadding);
		if (mListener != null)
		{
			mListener.onPopupDismiss();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
		{
			int eventX = (int) event.getX();
			int eventY = (int) event.getRawY();
			/**
			 * For vertical, we need accurate heuristics as event.getY() was not returning accurate data
			 * http://stackoverflow.com/questions/6237200/motionevent-gety-and-getx-return-incorrect-values
			 */
			if (shouldEatOuterTouchEvent(eventX, eventY))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Given the view ids to eat touch from it checks whether the touched point falls within the ambit of the view or not. If it falls within the view, it returns true, else
	 * returns false
	 * 
	 * @param eventX
	 * @return {@link Boolean}
	 */

	private boolean shouldEatOuterTouchEvent(int eventX, int eventY)
	{
		if (null == mEatTouchEventViewIds)
		{
			return false;
		}

		for (int id : mEatTouchEventViewIds)
		{
			if (shouldEatOuterTouch(eventX, eventY, id))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether a touch event point lies within the ambit of a given view. The view is identified by its viewId.
	 * 
	 * We're checking the touch event along the Y-axis first. If it lies in the middle region containing the view, we further check for their X-position, whether they lie over the
	 * view or elsewhere.
	 * 
	 * @param eventX
	 * @param viewId
	 * @return {@link Boolean}
	 */
	private boolean shouldEatOuterTouch(int eventX, int eventY, int viewId)
	{
		View st = mainView.findViewById(viewId);
		int[] xy = new int[2];
		st.getLocationInWindow(xy);
		if (eventY < xy[1])
		{
			return false;
		}
		else if (eventY > (xy[1] + st.getHeight()))
		{
			return true;
		}
		else if (eventX > xy[0] && eventX < (xy[0] + st.getWidth()))
		{
			return true;
		}
		return false;
	}

	public void updateListenerAndView(PopupListener listener, View view)
	{
		this.mListener = listener;
		this.mainView = view;
		registerOnGlobalLayoutListener();
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
			this.mainView.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
			this.mainView = null;
		}
	}

	private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener()
	{

		@Override
		public void onGlobalLayout()
		{
			if (mainView == null)
			{
				Logger.wtf("chatthread", "Getting null view inside global layout listener");
				return;
			}

			Log.i("chatthread", "global layout listener");

			Log.i("chatthread", "global layout listener rootHeight " + mainView.getRootView().getHeight() + " new height " + mainView.getHeight());
			Rect r = new Rect();
			mainView.getWindowVisibleDisplayFrame(r);
			// this is height of view which is visible on screen
			int rootViewHeight = context.getResources().getDisplayMetrics().heightPixels;
			int temp = rootViewHeight - r.bottom;
			Logger.i("chatthread", "keyboard  height " + temp);
			boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
			if (temp > 0)
			{
				if (islandScape)
				{
					possibleKeyboardHeightLand = temp;
				}
				else
				{
					possibleKeyboardHeight = temp;
				}
				if(isKeyboardOpen == false)
				{
					onKeyboardOpen(temp);
				}
				
				if (isShowing())
				{
					updatePadding(0);
				}
				updateDimension(LayoutParams.MATCH_PARENT, temp);
			}
			else
			{
				// when we change orientation , from portrait to landscape and keyboard is open , it is possible that screen does adjust its size more than once until it
				// stabilize
				if (islandScape)
					possibleKeyboardHeightLand = 0;
				if(isKeyboardOpen == true)
				{
					onKeyboardClose();
				}
				
			}
		}
	};

	public void onCloseKeyBoard()
	{
		if (isKeyboardOpen() && isShowing())
		{
			dismiss();
		}
	}
	
	public void setPopupDismissListener(PopupListener listener)
	{
		this.mListener = listener;
	}

	public boolean onEditTextTouch(View v, MotionEvent event)
	{
		return false;
	}

	/**
	 * 
	 * @return true if previous task is running
	 */
	public boolean isBusyInOperations()
	{
		return false;
	}

	public void onBackPressed()
	{

	}
	
	protected void onKeyboardOpen(int keyBoardHeight)
	{
		isKeyboardOpen = true;
		Intent intent = new Intent(ACTION_KEYBOARD_OPEN);
		intent.putExtra(HikeConstants.KEYBOARD_HEIGHT, keyBoardHeight);
		LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
	}
	
	protected void onKeyboardClose()
	{
		isKeyboardOpen = false;
		Intent intent = new Intent(ACTION_KEYBOARD_CLOSED);
		LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
	}

}
