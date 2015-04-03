package com.bsb.hike.media;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;

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

	private static String TAG = "ShareablePopupLayout";

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */

	public ShareablePopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds, PopupListener listener)
	{
		initViewToDisplay(context);
		initPopupLayout(context, mainView, firstTimeHeight, eatOuterTouchIds, listener);
	}

	private void initViewToDisplay(Context context)
	{
		this.mViewToDisplay = LayoutInflater.from(context).inflate(R.layout.shared_keyboard_layout, null);
	}

	/**
	 * Initializes the shared keyboard popup layout for Sticker Picker and Emoticon Picker
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */

	private void initPopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds, PopupListener listener)
	{
		if (mKeyboardPopupLayout == null)
		{
			mKeyboardPopupLayout = (eatOuterTouchIds == null) ? new KeyboardPopupLayout(mainView, firstTimeHeight, context, listener) : new KeyboardPopupLayout(mainView, firstTimeHeight,
					context, eatOuterTouchIds, listener);
		}
	}


	/**
	 * This method dismisses a popup if already showing, else it displays it 
	 * 
	 * @param popup
	 */

	public void togglePopup(ShareablePopup popup, int screenOrientation)
	{
		View popupView = popup.getView(screenOrientation);
		
		/** Exit condition
		 *  We simply return here.
		 */
		if (popupView == null) 
		{
			return;
		}
		
		/**
		 * If we're already showing a view, let's say stickers and sticker icon was tapped again, then we should dismiss the view.
		 */
		if (prevVisibleView != popupView || !mKeyboardPopupLayout.isShowing())
		{
			showPopup(popup, screenOrientation);
		}
		
		else
		{
			if (mKeyboardPopupLayout.isShowing())
			{
				dismiss();
			}
		}
	}
	
	/**
	 * Utility method used for displaying the Popups using the Keyboard Popup layout. Appropriate comments have been added in the code flow for easily readability
	 * 
	 * @param popup
	 */
	
	public void showPopup(ShareablePopup popup, int screenOrientation)
	{
		View popupView = popup.getView(screenOrientation);
		
		/**
		 * Exit condition
		 * We simply return here.
		 */
		if (popupView == null)
		{
			return;
		}
		
		addPopupView(popupView);

		swapViews(popupView);

		showKeyboardPopup();
	}

	/**
	 * Check whether we already had this view in the layout or not, else add it
	 * 
	 */

	private void addPopupView(View popupView)
	{
		FrameLayout frameLayout = (FrameLayout) mViewToDisplay.findViewById(R.id.shared_keyboard_parent);

		for (int i = 0; i < frameLayout.getChildCount(); i++)
		{
			if (frameLayout.getChildAt(i) == popupView)
			{
				return;
			}
		}

		/**
		 * New view. Add it to the layout
		 */
		Logger.i(TAG, "Adding popupView in frameLayout");
		frameLayout.addView(popupView);
	}

	private void showKeyboardPopup()
	{
		if (!mKeyboardPopupLayout.isShowing())
		{
			mKeyboardPopupLayout.showKeyboardPopup(mViewToDisplay);
		}
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

	public void onCloseKeyBoard()
	{
		if (isKeyboardOpen())
		{
			mKeyboardPopupLayout.onCloseKeyBoard();
		}
	}

}