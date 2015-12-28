package com.bsb.hike.media;

import java.lang.reflect.Field;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class PopUpLayout implements OnTouchListener
{

	protected PopupWindow popup;

	protected Context context;

	public PopUpLayout(Context context)
	{
		this.context = context;
	}

	/**
	 * this method calls {@link #showPopUpWindow(int, int, int, int, View)} internally with x and y offset 0
	 * @param inputMethodMode 
	 * 			  : <p>Since the popup is focusable, if displayed along with a keyboard, it can take focus from the keyboard. Thus, if there is a possibility that keyboard could be
	 *            visible along with the popup, you should use it with {@link PopupWindow#INPUT_METHOD_NOT_NEEDED} flag.</p>
	 */
	public void showPopUpWindow(int width, int height, View anchor, View view, int inputMethodMode)
	{
		showPopUpWindow(width, height, 0, 0, anchor, view, inputMethodMode);
	}
	
	/**
	 * this method calls {@link #showPopUpWindow(int, int, int, int, View)} internally with x and y offset 0
	 */
	public void showPopUpWindow(int width, int height, View anchor, View view)
	{
		showPopUpWindow(width, height, 0, 0, anchor, view);
	}
	

	/**
	 * Shows a pop up window with default view, if you do not want to show view as pop up window, you should use {@link #getView()}
	 * 
	 * Popup window is given width and height as {@link LayoutParams#MATCH_PARENT} and background color is transparent to eat clicks and prevent it from being dismissed
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 * @param context
	 */
	public void showPopUpWindowNoDismiss(int xoffset, int yoffset, View anchor, View view)
	{
		showPopUpWindow(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, xoffset, yoffset, anchor, view);

		// This is a workaround for not to dismiss popup window if anywhere out
		// side is touched, for layout below we have made our popup with
		// MATCH_PARENT but for action bar, we have to do following
		// it will give us call back in on touch with
		// action_outside event, so we can return true to eat that event
		// BUT Point to note here is : even though the pop up is not dismissed,
		// view behind it will still get onclick event
		FrameLayout viewParent = (FrameLayout) view.getRootView();
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams) viewParent.getLayoutParams();
		lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		WindowManager windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
		windowManager.updateViewLayout(viewParent, lp);

		popup.setTouchInterceptor(this);
	}
	
	/**
	 * Shows a pop up window with default view, if you do not want to show view as pop up window, you should use {@link #getView()}
	 * 
	 * Popup window is given width and height as {@link LayoutParams#MATCH_PARENT} and background color is transparent to eat clicks and prevent it from being dismissed
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 * @param context
	 * @param inputMethodMode
	 * 	 		  : <p>Since the popup is focusable, if displayed along with a keyboard, it can take focus from the keyboard. Thus, if there is a possibility that keyboard could be
	 *            visible along with the popup, you should use it with {@link PopupWindow#INPUT_METHOD_NOT_NEEDED} flag.</p>
	 */
	public void showPopUpWindowNoDismiss(int xoffset, int yoffset, View anchor, View view, int inputMethodMode)
	{
		showPopUpWindow(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, xoffset, yoffset, anchor, view, inputMethodMode);

		// This is a workaround for not to dismiss popup window if anywhere out
		// side is touched, for layout below we have made our popup with
		// MATCH_PARENT but for action bar, we have to do following
		// it will give us call back in on touch with
		// action_outside event, so we can return true to eat that event
		// BUT Point to note here is : even though the pop up is not dismissed,
		// view behind it will still get onclick event
		FrameLayout viewParent = (FrameLayout) view.getParent();
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams) viewParent.getLayoutParams();
		lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		WindowManager windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
		windowManager.updateViewLayout(viewParent, lp);

		popup.setTouchInterceptor(this);
	}

	/**
	 * Shows a pop up window with default view, if you do not want to show view as pop up window, you should use {@link #getView()} It uses {@link #initView()} to initialize view
	 * and {@link #getView()} to get view to show
	 * 
	 * @param width
	 * @param height
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 */
	public void showPopUpWindow(int width, int height, int xoffset, int yoffset, View anchor, View view, int inputMethodMode)
	{
		if (popup == null)
		{
			initPopUpWindow(width, height, view, context, inputMethodMode);
		}

		popup.showAsDropDown(anchor, xoffset, yoffset);
	}
	
	public void showPopUpWindow(int width, int height, int xoffset, int yoffset, View anchor, View view)
	{
		if (popup == null)
		{
			initPopUpWindow(width, height, view, context);
		}

		popup.showAsDropDown(anchor, xoffset, yoffset);
	}

	/**
	 * This method is responsible for initializing popup window with given attributes, by default we set {@link android.R.color#transparent} as background color - by default popup
	 * is dismissed if outside is touched
	 * 
	 * @param inputMethodMode
	 *            : <p>Since the popup is focusable, if displayed along with a keyboard, it can take focus from the keyboard. Thus, if there is a possibility that keyboard could be
	 *            visible along with the popup, you should use it with {@link PopupWindow#INPUT_METHOD_NOT_NEEDED} flag.</p>
	 */
	protected PopupWindow initPopUpWindow(int width, int height, View viewToShow, Context context, int inputMethodMode)
	{
		popup = initPopUpWindow(width, height, viewToShow, context);
		popup.setInputMethodMode(inputMethodMode);
		return popup;
	}
	
	/**
	 * This method is responsible for initializing popup window with given attributes, by default we set {@link android.R.color#transparent} as background color - by default popup
	 * is dismissed if outside is touched
	 */
	
	protected PopupWindow initPopUpWindow(int width, int height, View viewToShow, Context context)
	{
		popup = new PopupWindow(context);
		popup.setBackgroundDrawable(context.getResources().getDrawable(android.R.color.transparent));
		popup.setWidth(width);
		popup.setHeight(height);
		popup.setContentView(viewToShow);
		// do not hide pop-up if outside is touched
		popup.setOutsideTouchable(false);
		// to gain focus
		popup.setFocusable(true);
		
		return popup;
	}

	/**
	 * This will update dimensions of pop up window if visible else no effect
	 * 
	 * @generated
	 * @ordered
	 */

	public void updateDimension(int width, int height)
	{
		if (isShowing())
		{
			popup.update(width, height);
		}
	}

	/**
	 * if popup has been initialized and it is showing, it returns true then only
	 * 
	 * @return boolean
	 */
	public boolean isShowing()
	{
		return popup != null && popup.isShowing();
	}

	/**
	 * Dismiss popup window if showing else avoid call
	 */
	public void dismiss()
	{
		if (isShowing())
		{
			popup.dismiss();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return event.getAction() == MotionEvent.ACTION_OUTSIDE;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener)
	{
		popup.setOnDismissListener(onDismissListener);
	}

	/**
	 * In Android Lollipop, a new feature called Heads-Up notifications was introduced. Due to this, whenever there was a headsup notification, it would take focus from any other view element that had focus. 
	 * If there was a popupWindow being displayed with keyboard open, it would behave erratically and it's size calculation would go for a toss. eg: the emoticon popup/sticker popup would come over the kyboard instead of taking the place of keyboard. 
	 * This is a hacky fix to prevent this issue. 
	 * 
	 * Source : Telegram on github ->  https://github.com/DrKLO/Telegram/commit/e5e31e20e46e437dc347e1eea74ec65fb71ddf5a
	 *  
	 * @param popup
	 */
	public void fixLollipopHeadsUpNotifPopup(PopupWindow popup)
	{
		if (Build.VERSION.SDK_INT >= 21)
		{
			try
			{
				Field field = PopupWindow.class.getDeclaredField("mWindowLayoutType");
				field.setAccessible(true);
				field.set(popup, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
			}
			catch (Exception e)
			{
				/* ignored */
			}
		}
	}
}
