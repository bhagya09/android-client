package com.bsb.hike.media;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout;
import com.bsb.hike.media.OverFlowMenuLayout.OverflowViewListener;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class HikeActionBar
{

	private HikeAppStateBaseFragmentActivity mActivity;

	public OverFlowMenuLayout overFlowMenuLayout;
	
	private Menu mMenu;
	
	private boolean overflowMenIndicatorInUse = false;

	/**
	 * @generated
	 * @ordered
	 */

	public HikeActionBar(HikeAppStateBaseFragmentActivity activity)
	{
		this.mActivity = activity;

	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout)
	{
		MenuInflater menuInflater = mActivity.getMenuInflater();
		menuInflater.inflate(menuLayout, menu);
		this.mMenu = menu;
	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout, List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener)
	{
		onCreateOptionsMenu(menu, menuLayout);
		onCreateOverflowMenu(overflowItems, listener, onDismissListener);
	}

	private void onCreateOverflowMenu(List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener)
	{
		overFlowMenuLayout = new OverFlowMenuLayout(overflowItems, listener, onDismissListener, mActivity);
	}

	public boolean isOverflowMenuPopulated()
	{
		return overFlowMenuLayout != null;
	}

	public void onPreareOptionsMenu(Menu menu)
	{

	}

	public void setOverflowViewListener(OverflowViewListener viewListener)
	{
		overFlowMenuLayout.setOverflowViewListener(viewListener);
	}

	/**
	 * If something is your activity specific then handle that click on your side pass click if it is some sort of utility OR common case which this object can handle for example
	 * copy
	 * 
	 * @param menuItem
	 */
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{

		return false;
	}

	/**
	 * This function is used to inflate a custom layout for action bar. 
	 * It returns the view inflated. The calling classes have to set the View in the ActionBar themselves.
	 * @param layoutResId
	 */
	public View inflateCustomActionBarView(int layoutResId)
	{
		ActionBar sherlockActionBar = mActivity.getSupportActionBar();
		sherlockActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		
		View actionBarView = LayoutInflater.from(mActivity.getApplicationContext()).inflate(layoutResId, null);
		
		sherlockActionBar.setCustomView(actionBarView);
		sherlockActionBar.setDisplayHomeAsUpEnabled(true);
		//http://stackoverflow.com/questions/27354812/android-remove-left-margin-from-actionbars-custom-layout
		//removing space on the left of action bar
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
		
		
		return actionBarView;
	}
	
	/**
	 * Called when we use the default color for action bar's background
	 * @param layoutResId
	 * @return
	 */
	public View setCustomActionBarView(int layoutResId)
	{
		return inflateCustomActionBarView(layoutResId);
	}

	/**
	 * Returns the list of overflow menu items held by this ActionBar
	 * @return
	 */
	public List<OverFlowMenuItem> getOverFlowMenuItems()
	{
		if(overFlowMenuLayout != null)
		{
			return overFlowMenuLayout.getOverFlowMenuItems(); 
		}
		
		else
		{
			return null;
		}
	}

	public void showOverflowMenu(int width, int height, int xOffset, int yOffset, View anchor)
	{
		/**
		 * Getting an NPE at times here. This can be null only in case where onCreateOptionsMenu is yet to be called though it shouldn't happen. It's a defensive check
		 */
		if (overFlowMenuLayout != null)
		{	
			overFlowMenuLayout.show(width, height, xOffset, yOffset, anchor, PopupWindow.INPUT_METHOD_NOT_NEEDED);
		}
	}
	
	/**
	 * Returns a menuItem for a given resId
	 * 
	 * @param resId
	 * @return
	 */
	public MenuItem getMenuItem(int resId)
	{
		MenuItem menuItem = null;
		if (mMenu != null)
		{
			menuItem = mMenu.findItem(resId);
		}

		return menuItem;
	}

	
	/**
	 * Can be used to update the unread count of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param newCount
	 */
	public void updateOverflowMenuItemCount(int itemId, int newCount)
	{
		if(overFlowMenuLayout!=null)
		{
			overFlowMenuLayout.updateOverflowMenuItemCount(itemId, newCount);
		}
	}
	
	/**
	 * Can be used to update the title of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param newTitle
	 */
	public void updateOverflowMenuItemString(int itemId, String newTitle)
	{
		if(overFlowMenuLayout!=null)
		{
			overFlowMenuLayout.updateOverflowMenuItemString(itemId, newTitle);
		}
	}
	
	/**
	 * Can be used to update the active state of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param enabled
	 */
	public void updateOverflowMenuItemActiveState(int itemId, boolean enabled)
	{
		if(overFlowMenuLayout!=null)
		{
			overFlowMenuLayout.updateOverflowMenuItemActiveState(itemId, enabled, true);
		}
	}

	/**
	 * Can be used to update the title of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param newTitle
	 */
	public void updateOverflowMenuItemIcon(int itemId, int drawableId)
	{
		if(overFlowMenuLayout!=null)
		{
			overFlowMenuLayout.updateOverflowMenuItemIcon(itemId, drawableId);
		}
	}

	/**
	 * This is used to update/show indicator image on the overflow menu icon. This will be called from the UI Thread
	 * 
	 * Can also be used by any other futuristic feature
	 * 
	 * Count indicator takes priority over image indicator.
	 * 
	 * @return
	 * 	true - if the indicator image is successfully displayed.
	 * 	false - if the image can not be displayed. This will also happen if the counter is currently visible.
	 */
	public boolean updateOverflowMenuIndicatorImage(int imadeResId)
	{
		MenuItem menuItem = getMenuItem(R.id.overflow_menu);
		
		if (menuItem != null && MenuItemCompat.getActionView(menuItem) != null)
		{
			ImageView topBarIndiImage = (ImageView) MenuItemCompat.getActionView(menuItem).findViewById(R.id.top_bar_indicator_img);
			TextView topBarCounter = (TextView) MenuItemCompat.getActionView(menuItem).findViewById(R.id.top_bar_indicator_text);

			if (imadeResId != 0 && topBarCounter.getVisibility() != View.VISIBLE)
			{
				topBarIndiImage.setImageResource(imadeResId);
				topBarIndiImage.setVisibility(View.VISIBLE);
				topBarIndiImage.startAnimation(Utils.getNotificationIndicatorAnim());
				overflowMenIndicatorInUse = true;
				return true;
			}
			else
			{
				topBarIndiImage.setVisibility(View.GONE);
				if (topBarCounter.getVisibility() != View.VISIBLE)
				{
					overflowMenIndicatorInUse = false;
				}
				if (imadeResId == 0)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This is used to update/show counter on the overflow menu icon. This will be called from the UI Thread
	 * 
	 * Can be used for pin count or in future say missed calls count for VoIP or any other futuristic feature
	 * 
	 * Count indicator takes priority over image indicator.
	 */
	public void updateOverflowMenuIndicatorCount(int newCount)
	{
		MenuItem menuItem = getMenuItem(R.id.overflow_menu);
		
		if (menuItem != null && MenuItemCompat.getActionView(menuItem) != null)
		{
			TextView topBarCounter = (TextView) MenuItemCompat.getActionView(menuItem).findViewById(R.id.top_bar_indicator_text);

			if (newCount < 1)
			{
				topBarCounter.setVisibility(View.GONE);
				overflowMenIndicatorInUse = false;
			}
			else
			{
				topBarCounter.setVisibility(View.VISIBLE);
				topBarCounter.setText(getUnreadCounterText(newCount));
				topBarCounter.startAnimation(Utils.getNotificationIndicatorAnim());
				overflowMenIndicatorInUse = true;
				updateOverflowMenuIndicatorImage(0);
			}
		}
	}
	
	private String getUnreadCounterText(int counter)
	{
		if (counter >= HikeConstants.MAX_PIN_CONTENT_LINES_IN_HISTORY)
		{
			return mActivity.getString(R.string.max_pin_unread_counter);
		}
		else
		{
			return Integer.toString(counter);
		}
	}
	
	public boolean isOverflowMenuIndicatorInUse()
	{
		return overflowMenIndicatorInUse;
	}

	public void removeItemIfExists(int id)
	{
		if (overFlowMenuLayout != null)
		{
			overFlowMenuLayout.removeItem(id);
		}
	}

	public void releseResources()
	{
		if (overFlowMenuLayout != null)
		{
			overFlowMenuLayout.releaseResources();
			overFlowMenuLayout = null;
		}
	}
	
	public boolean isOverflowMenuShowing()
	{
		if (overFlowMenuLayout != null)
		{
			return overFlowMenuLayout.isShowing();
		}
		
		return false;
	}

	public void dismissOverflowMenu()
	{
		if (overFlowMenuLayout != null)
		{
			overFlowMenuLayout.dismiss();
		}
	}

	public void resetView()
	{
		mActivity.getSupportActionBar().setCustomView(null);
	}
	
	/**
	 * @param shouldAvoidDismissOnClick the shouldAvoidDismissOnClick to set
	 */
	public void setShouldAvoidDismissOnClick(boolean shouldAvoidDismissOnClick)
	{
		if (overFlowMenuLayout != null)
		{
			overFlowMenuLayout.setShouldAvoidDismissOnClick(shouldAvoidDismissOnClick);
		}
	}
	
	/**
	 * Utility method to refresh the overflow menu item
	 * 
	 * @param item
	 */
	public void refreshOverflowMenuItem(OverFlowMenuItem item)
	{
		if (overFlowMenuLayout != null)
		{
			overFlowMenuLayout.refreshOverflowMenuItm(item);
		}
	}
}
