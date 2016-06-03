package com.bsb.hike.media;


import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

/**
 * This class is used when we have to share KeyBoardPopup layout with other views. It's an aggregator class, and has the KeyBoard popup layout object, as well as a list of
 * {@link ShareablePopup} interfaces. The class uses intelligence in the {@link #showPopup(ShareablePopup)} and {@link #togglePopup(ShareablePopup)} to display the views.
 * 
 * @author piyush
 */
public class ShareablePopupLayout
{
	private KeyboardPopupLayout mKeyboardPopupLayout;

	private View mViewToDisplay;

	private View prevVisibleView;

	public static String TAG = "ShareablePopupLayout";

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */

	public ShareablePopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds, PopupListener listener,OnSoftKeyboardListener onSoftKeyboardListener)
	{
		initViewToDisplay(context);
		initPopupLayout(context, mainView, firstTimeHeight, eatOuterTouchIds, listener,onSoftKeyboardListener);
	}

	private void initViewToDisplay(Context context)
	{
		this.mViewToDisplay = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.shared_keyboard_layout, null);
	}

	/**
	 * Initializes the shared keyboard popup layout for Sticker Picker and Emoticon Picker
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 * @param editText 
	 * @param onSoftKeyboardListener 
	 */

	private void initPopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds, PopupListener listener, OnSoftKeyboardListener onSoftKeyboardListener)
	{
		if (mKeyboardPopupLayout == null)
		{
			if (HikeMessengerApp.keyboardApproach(context))
			{
				mKeyboardPopupLayout = (eatOuterTouchIds == null) ? new KeyboardPopupLayout21(mainView, firstTimeHeight, context, listener, onSoftKeyboardListener)
						: new KeyboardPopupLayout21(mainView, firstTimeHeight, context, eatOuterTouchIds, listener, onSoftKeyboardListener);
			}
			else
			{
				mKeyboardPopupLayout = (eatOuterTouchIds == null) ? new KeyboardPopupLayout(mainView, firstTimeHeight, context, listener) : new KeyboardPopupLayout(mainView,
						firstTimeHeight, context, eatOuterTouchIds, listener);
			}
		}
	}

    /**
     * This method dismisses a popup if already showing, else it displays it
     *
     * @param popup
     */

    public boolean togglePopup(ShareablePopup popup, int screenOrientation, boolean refreshKeyboardHeight)
    {
        View popupView = popup.getView(screenOrientation);

        /** Exit condition
         *  We simply return here.
         */
        if (popupView == null)
        {
            return false;
        }

        /**
         * If we're already showing a view, let's say stickers and sticker icon was tapped again, then we should dismiss the view.
         */
        if (prevVisibleView != popupView || !mKeyboardPopupLayout.isShowing())
        {
            return showPopup(popupView,refreshKeyboardHeight);
        }

        else
        {
            if (mKeyboardPopupLayout.isShowing())
            {
                if (mKeyboardPopupLayout instanceof KeyboardPopupLayout21)
                {
                    ((KeyboardPopupLayout21) mKeyboardPopupLayout).showKeyboardAfterPopupDismiss();
                }
                dismiss();
                return true;
            }
        }

        return false;
    }


	/**
	 * Utility method used for displaying the Popups using the Keyboard Popup layout. Appropriate comments have been added in the code flow for easily readability
	 * 
	 * @param popup
	 */
	
	public boolean showPopup(ShareablePopup popup, int screenOrientation, int customKeyboardHeight)
	{
		View popupView = popup.getView(screenOrientation);
		
		/**
		 * Exit condition
		 * We simply return here.
		 */
		if (popupView == null)
		{
			return false;
		}
		
		addPopupView(popupView);

		swapViews(popupView);

        // Since this is for Keyboard popup layout, we need to refreshKeyboardHeight while setting popup
        boolean refreshKeyboardHeight = true;

		return showKeyboardPopup(refreshKeyboardHeight,customKeyboardHeight);
	}
	
	private boolean showPopup(View popupView,boolean refreshKeyboardHeight)
	{
		addPopupView(popupView);

		swapViews(popupView);

		return showKeyboardPopup(refreshKeyboardHeight,0);
	}

	/**
	 * Check whether we already had this view in the layout or not, else add it
	 * 
	 */
	private void addPopupView(View popupView)
	{
		FrameLayout frameLayout = (FrameLayout) mViewToDisplay.findViewById(R.id.shared_keyboard_parent);
		
		/**
		 * Removing the residual views, if any
		 */
		frameLayout.removeAllViews();

		/**
		 * New view. Add it to the layout
		 */
		Logger.i(TAG, "Adding popupView in frameLayout");
		frameLayout.addView(popupView);
	}

	private boolean showKeyboardPopup(boolean refreshKeyboardHeight,int customKeyboardHeight)
	{
        if(refreshKeyboardHeight)
        {
            mKeyboardPopupLayout.dismiss();
            return mKeyboardPopupLayout.showCustomKeyboardPopup(mViewToDisplay,refreshKeyboardHeight,customKeyboardHeight);
        }
        else if (!mKeyboardPopupLayout.isShowing())
		{
            return mKeyboardPopupLayout.showKeyboardPopup(mViewToDisplay);
		}
		
		return true;
	}

	/**
	 * Used for swapping the prevVisible view with the new view which is to be made visible
	 * 
	 * @param popupView
	 */
	private void swapViews(View popupView)
	{
		if (prevVisibleView == null)
		{
			prevVisibleView = popupView;
			popupView.setVisibility(View.VISIBLE);
		}

		else if (prevVisibleView != popupView)
		{
			prevVisibleView.setVisibility(View.GONE);
			popupView.setVisibility(View.VISIBLE);
			prevVisibleView = popupView;
		}
	}

	/**
	 * Used for dismissing the popup window
	 */
	public void dismiss()
	{
		mKeyboardPopupLayout.dismiss();
	}

	public boolean isShowing()
	{
		return mKeyboardPopupLayout.isShowing();
	}

	public void updateListenerAndView(PopupListener listener, View view)
	{
		this.mKeyboardPopupLayout.updateListenerAndView(listener, view);
	}

	public boolean isKeyboardOpen()
	{
		return mKeyboardPopupLayout.isKeyboardOpen();
	}

	public void releaseResources()
	{
		mKeyboardPopupLayout.releaseResources();
	}

	public boolean onEditTextTouch(View v, MotionEvent event){
		return mKeyboardPopupLayout.onEditTextTouch(v, event);
	}

	public boolean isBusyInOperations(){
		return mKeyboardPopupLayout.isBusyInOperations();
	}

	public void onBackPressed()
	{
		mKeyboardPopupLayout.onBackPressed();
	}

	public void setWindowSystemBarBgFlag(boolean windowFlagEnabled)
	{
		if (mKeyboardPopupLayout != null)
		{
			mKeyboardPopupLayout.setDrawSystemBarBgFlagEnabled(windowFlagEnabled);
		}
	}

	public void onConfigurationChanged()
	{
		if (mKeyboardPopupLayout != null)
		{
			mKeyboardPopupLayout.onConfigChanged();
		}
	}

	public boolean isPopupShowing(ShareablePopup popup, int screenOrientation)
	{
		if(!isShowing())
		{
			return false;
		}

		View popupView = popup.getView(screenOrientation);

		/** Exit condition
		 *  We simply return here.
		 */
		if (popupView == null)
		{
			return false;
		}

		/**
		 * If we're already showing a view, let's say stickers and sticker was tapped again, we return true
		 */
		if (prevVisibleView == popupView)
		{
			return true;
		}

		return false;
	}

}