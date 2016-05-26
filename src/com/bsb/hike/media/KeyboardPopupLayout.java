package com.bsb.hike.media;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import static com.bsb.hike.HikeConstants.IntentAction.ACTION_KEYBOARD_CLOSED;
import static com.bsb.hike.HikeConstants.IntentAction.ACTION_KEYBOARD_OPEN;

public class KeyboardPopupLayout extends PopUpLayout implements OnDismissListener
{
    protected View mainView;

    protected int possibleKeyboardHeightLand, possibleKeyboardHeight, originalBottomPadding;

    protected boolean isKeyboardOpen;

    protected int firstTimeHeight;

    protected int[] mEatTouchEventViewIds;

    protected PopupListener mListener;

    protected boolean isDrawSystemBarBgFlagEnabled = false;

    private boolean mIsPaddingDisabled = false;

    protected boolean isCustomKeyboardPopup = false;

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

        // Note : If the config changes at runtime, make sure to invoke this method inorder to update either one of these flags
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            HikeMessengerApp.bottomNavBarWidthLandscape = Utils.getBottomNavBarWidth(context);
        }

        else
        {
            HikeMessengerApp.bottomNavBarHeightPortrait = Utils.getBottomNavBarHeight(context);
        }

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
        if (mainView != null && mainView.getPaddingBottom() != bottomPadding && !mIsPaddingDisabled)
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

        showPopup(height);

        return true;
    }

    protected void showPopup(int popupHeight)
    {
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            showPopupLandscape();
        }

        else
        {
            if (shouldApplyNavBarOffset())
            {
                showPopupForLollipop(popupHeight);
            }

            else
            {
                showPopupForPortrait();
            }
        }
    }

    private void showPopupForPortrait()
    {
        popup.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
    }

    /**
     * For devices which are Lollipop or Higher, we have to take into consideration the window flag : WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS Due to this flag
     * we were drawing the popup on the system's nav bar tray as well instead of above it. This method places the popup carefully by excluding the nav bar height
     *
     * @param popupHeight
     */
    private void showPopupForLollipop(int popupHeight)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point realPoint = new Point();
        Display display = wm.getDefaultDisplay();
        display.getRealSize(realPoint);

        popup.setWidth(realPoint.x); //In PortraitMode, so no offset needed
        popup.showAtLocation(mainView, Gravity.NO_GRAVITY, 0, realPoint.y - popupHeight - HikeMessengerApp.bottomNavBarHeightPortrait);
    }

    private void showPopupLandscape()
    {
        if(shouldApplyNavBarOffset())
        {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Point realPoint = new Point();
            Display display = wm.getDefaultDisplay();
            display.getRealSize(realPoint);

            popup.setWidth(realPoint.x - HikeMessengerApp.bottomNavBarWidthLandscape); //Need to apply offset for the nav bar which is diplayed to the right of the screen
            popup.showAtLocation(mainView, Gravity.BOTTOM | Gravity.LEFT, 0, 0);
        }

        else
        {
            popup.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
        }
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

            Logger.i("chatthread", "global layout listener");

            Logger.i("chatthread", "global layout listener rootHeight " + mainView.getRootView().getHeight() + " new height " + mainView.getHeight());
            Rect r = new Rect();
            mainView.getWindowVisibleDisplayFrame(r);
            // this is height of view which is visible on screen
            int rootViewHeight = context.getResources().getDisplayMetrics().heightPixels;
            int temp = rootViewHeight - r.bottom;
            Logger.i("chatthread", "possible keyboard  height " + temp);
            boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            interpretHeightOfKeyboard(temp, islandScape);
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

    public boolean onEditTextTouch(View v, MotionEvent event) {
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

    private void interpretHeightOfKeyboard(int temp, boolean islandScape)
    {
        if (islandScape)
        {
            interpretHeightInLandscape(temp);
        }

        else
        {
            interpretHeightInPortraitMode(temp);
        }
    }

    /**
     * For devices which are Lollipop or Higher, we have to take into consideration the window flag : WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS Due to this flag
     * we were drawing the popup on the system's nav bar tray as well instead of above it. this method excludes the navbar height from the keyboard height
     *
     * @param temp
     */
    protected void interpretHeightInPortraitMode(int temp)
    {
        int bottomNavBArThreshold = shouldApplyNavBarOffset() ? HikeMessengerApp.bottomNavBarHeightPortrait : 0;

        temp -= bottomNavBArThreshold;

        if (temp > 0)
        {
            possibleKeyboardHeight = temp;
            isKeyboardOpen = true;
            if (isShowing())
            {
                updatePadding(0);
            }

            if(!isCustomKeyboardPopup)
                updateDimension(LayoutParams.MATCH_PARENT, temp);
        }
        else
        {
            isKeyboardOpen = false;
        }
    }

    protected void interpretHeightInLandscape(int temp)
    {
        if (temp > 0)
        {
            possibleKeyboardHeightLand = temp;

            isKeyboardOpen = true;
            if (isShowing())
            {
                updatePadding(0);
            }

            if(!isCustomKeyboardPopup)
                updateDimension(LayoutParams.MATCH_PARENT, temp);
        }
        else
        {
            possibleKeyboardHeightLand = 0;
            isKeyboardOpen = false;
        }
    }

    /**
     * @return the isDrawSystemBarBgFlagEnabled
     */
    public boolean isDrawSystemBarBgFlagEnabled()
    {
        return isDrawSystemBarBgFlagEnabled;
    }

    /**
     * @param isDrawSystemBarBgFlagEnabled the isDrawSystemBarBgFlagEnabled to set
     */
    public void setDrawSystemBarBgFlagEnabled(boolean isDrawSystemBarBgFlagEnabled)
    {
        this.isDrawSystemBarBgFlagEnabled = isDrawSystemBarBgFlagEnabled;
    }

    protected boolean shouldApplyNavBarOffset()
    {
        return Utils.isLollipopOrHigher() && isDrawSystemBarBgFlagEnabled();
    }

    public void onConfigChanged()
    {
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            HikeMessengerApp.bottomNavBarWidthLandscape = Utils.getBottomNavBarWidth(context);
        }

        else
        {
            HikeMessengerApp.bottomNavBarHeightPortrait = Utils.getBottomNavBarHeight(context);
        }
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

    public void setPaddingDisabled(boolean disabled)
    {
        mIsPaddingDisabled = disabled;
    }

    /*
     * This code is being added for specific use case of displaying popups for custom keyboard with dynamic height
     * @param view
     * @param refreshKeyboard
     * @param customKeyboardHeight
     */
    public boolean showCustomKeyboardPopup(View view, boolean refreshKeyboard, int customKeyboardHeight)
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

        int height;
        if (refreshKeyboard)
        {
            height = firstTimeHeight;
        }
        else
        {
            height = islandScape ? possibleKeyboardHeightLand : possibleKeyboardHeight;
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
            initPopUpWindow(ViewGroup.LayoutParams.MATCH_PARENT, height, view, context, PopupWindow.INPUT_METHOD_NOT_NEEDED);
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

        showPopup(height);

        return true;
    }

	public boolean isCustomKeyboardPopup()
	{
		return isCustomKeyboardPopup;
	}

	public void setCustomKeyboardPopup(boolean customKeyboardPopup)
	{
		isCustomKeyboardPopup = customKeyboardPopup;
	}

}