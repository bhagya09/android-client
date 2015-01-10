package com.bsb.hike.media;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow.OnDismissListener;

import com.bsb.hike.utils.Logger;

public class KeyboardPopupLayout extends PopUpLayout implements OnDismissListener
{
	private View mainView;

	private int possibleKeyboardHeightLand, possibleKeyboardHeight, originalBottomPadding;

	private boolean isKeyboardOpen;

	private int firstTimeHeight;

	private int[] mEatTouchEventViewIds;

	/**
	 * 
	 * @param mainView
	 *            - This should be top most view of your activity which get resized when soft keyboard is toggled
	 * @param firstTimeHeight
	 *            - This is the height which will be used before keyboard opens
	 * @param context
	 */
	public KeyboardPopupLayout(View mainView, int firstTimeHeight, Context context)
	{
		super(context);
		this.mainView = mainView;
		this.firstTimeHeight = firstTimeHeight;
		originalBottomPadding = mainView.getPaddingBottom();
		registerOnGlobalLayoutListener();
	}
	
	public KeyboardPopupLayout(View mainView, int firstTimeHeight, Context context, int[] eatTouchEventViewIds)
	{
		this(mainView, firstTimeHeight, context);
		this.mEatTouchEventViewIds = eatTouchEventViewIds;
	}

	private void registerOnGlobalLayoutListener()
	{
		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{

			@Override
			public void onGlobalLayout()
			{
				Log.i("chatthread", "global layout listener");

				Log.i("chatthread", "global layout listener rootHeight " + mainView.getRootView().getHeight() + " new height " + mainView.getHeight());
				Rect r = new Rect();
				mainView.getWindowVisibleDisplayFrame(r);
				// this is height of view which is visible on screen
				int rootViewHeight = mainView.getRootView().getHeight();
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
					isKeyboardOpen = true;
					updateDimension(LayoutParams.MATCH_PARENT, temp);
				}
				else
				{
					// when we change orientation , from portrait to landscape and keyboard is open , it is possible that screen does adjust its size more than once until it
					// stabilize
					if (islandScape)
						possibleKeyboardHeightLand = 0;
					isKeyboardOpen = false;
				}
			}
		});
	}

	private void updatePadding(int bottomPadding)
	{
		if (mainView.getPaddingBottom() != bottomPadding)
		{
			Logger.i("chatthread", "resize main height with bottom padding " + bottomPadding);
			mainView.setPadding(0, 0, 0, bottomPadding);
		}
	}

	public void showKeyboardPopup(View view)
	{
		boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		int height = islandScape ? possibleKeyboardHeightLand : possibleKeyboardHeight;
		if (height == 0)
		{
			height = firstTimeHeight;
		}
		if (popup == null)
		{
			initPopUpWindow(LayoutParams.MATCH_PARENT, height, view, context);
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
	}

	@Override
	public void onDismiss()
	{
		/**
		 * Whenever this pop up is dismissed, we want bottom padding of mainview to be reset
		 */
		updatePadding(originalBottomPadding);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
		{
			int eventX = (int) event.getX();
			return shouldEatOuterTouch(eventX);
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

	private boolean shouldEatOuterTouch(int eventX)
	{
		if (null == mEatTouchEventViewIds)
		{
			return false;
		}

		for (int id : mEatTouchEventViewIds)
		{
			if (shouldEatOuterTouch(eventX, id))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether a touch event point lies within the ambit of a given view. The view is identified by its viewId.
	 * 
	 * @param eventX
	 * @param viewId
	 * @return {@link Boolean}
	 */
	private boolean shouldEatOuterTouch(int eventX, int viewId)
	{
		View st = mainView.findViewById(viewId);
		int[] xy = new int[2];
		st.getLocationInWindow(xy);
		return ((eventX >= xy[0] && eventX <= (xy[0] + st.getWidth())));
	}

	public void updateMainView(View view)
	{
		this.mainView = view;
		registerOnGlobalLayoutListener();
	}

	public boolean isKeyboardOpen()
	{
		return isKeyboardOpen;
	}
}
