package com.bsb.hike.media;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuLayout.OverflowViewListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class HikeActionBar implements OverflowItemClickListener
{

	private HikeAppStateBaseFragmentActivity mActivity;

	public OverFlowMenuLayout overFlowMenuLayoutPrimary;

	public OverFlowMenuLayout overFlowMenuLayoutSecondary;
	
	private Menu mMenu;
	
	private boolean overflowMenIndicatorInUse = false;
	
	int layoutWidth;
	int layoutHeight;
	int layoutXOffset;
	int layoutYOffset;
	View layoutAnchor;
	
	OverflowItemClickListener overflowItemClickListener;

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
		List<OverFlowMenuItem> overflowItemsSecondary = new ArrayList<OverFlowMenuItem>();
		List<OverFlowMenuItem> overflowItemsPrimary = new ArrayList<OverFlowMenuItem>();
		for (OverFlowMenuItem menuItem:overflowItems)
		{
			if (menuItem.secondary)
				overflowItemsSecondary.add(menuItem);
			else
				overflowItemsPrimary.add(menuItem);
		}
		if (overflowItemsSecondary.isEmpty())
		{
			overFlowMenuLayoutPrimary = new OverFlowMenuLayout(overflowItems, this, onDismissListener, mActivity);
		}
		else
		{
			overflowItemsPrimary.add(new OverFlowMenuItem(mActivity.getString(R.string.more), 0, 0, R.string.more));
			overFlowMenuLayoutPrimary = new OverFlowMenuLayout(overflowItemsPrimary, this, onDismissListener, mActivity);
			overFlowMenuLayoutSecondary = new OverFlowMenuLayout(overflowItemsSecondary, this, onDismissListener, mActivity);
		}
		overflowItemClickListener = listener;
	}

	public boolean isOverflowMenuPopulated()
	{
		return overFlowMenuLayoutPrimary != null;
	}

	public void onPreareOptionsMenu(Menu menu)
	{

	}

	public void setOverflowViewListener(OverflowViewListener viewListener)
	{
		overFlowMenuLayoutPrimary.setOverflowViewListener(viewListener);
		if (overFlowMenuLayoutSecondary != null)
			overFlowMenuLayoutSecondary.setOverflowViewListener(viewListener);
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
		ActionBar supportActionBar = mActivity.getSupportActionBar();
		supportActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		
		View actionBarView = LayoutInflater.from(mActivity.getApplicationContext()).inflate(layoutResId, null);
		
		supportActionBar.setCustomView(actionBarView);
		supportActionBar.setDisplayHomeAsUpEnabled(true);
		supportActionBar.show(); //The action bar could be hidden due to other fragments using it. Hence calling a show here
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
	 * Returns the list of primary overflow menu items held by this ActionBar
	 * @return
	 */
	public List<OverFlowMenuItem> getOverFlowMenuItemsPrimary()
	{
		if(overFlowMenuLayoutPrimary != null)
		{
			return overFlowMenuLayoutPrimary.getOverFlowMenuItems(); 
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Returns the list of secondary overflow menu items held by this ActionBar
	 * @return
	 */
	public List<OverFlowMenuItem> getOverFlowMenuItemsSecondary()
	{
		if(overFlowMenuLayoutSecondary != null)
		{
			return overFlowMenuLayoutSecondary.getOverFlowMenuItems(); 
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
		if (overFlowMenuLayoutPrimary != null)
		{	
			overFlowMenuLayoutPrimary.initView();
			overFlowMenuLayoutPrimary.show(width, height, xOffset, yOffset, anchor, PopupWindow.INPUT_METHOD_NOT_NEEDED);
		}
		
		layoutWidth = width;
		layoutHeight = height;
		layoutXOffset = xOffset;
		layoutYOffset = yOffset;
		layoutAnchor = anchor;
	}

	private void showSecondaryOverflowMenu()
	{
		showSecondaryOverflowMenu(layoutWidth, layoutHeight, layoutXOffset, layoutYOffset, layoutAnchor);
	}

	private void showSecondaryOverflowMenu(int width, int height, int xOffset, int yOffset, View anchor)
	{
		/**
		 * Getting an NPE at times here. This can be null only in case where onCreateOptionsMenu is yet to be called though it shouldn't happen. It's a defensive check
		 */
		if (overFlowMenuLayoutSecondary != null)
		{	
			overFlowMenuLayoutSecondary.initView();
			overFlowMenuLayoutSecondary.show(width, height, xOffset, yOffset, anchor, PopupWindow.INPUT_METHOD_NOT_NEEDED);
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
		if(overFlowMenuLayoutPrimary!=null)
		{
			overFlowMenuLayoutPrimary.updateOverflowMenuItemCount(itemId, newCount);
		}
		if(overFlowMenuLayoutSecondary!=null)
		{
			overFlowMenuLayoutSecondary.updateOverflowMenuItemCount(itemId, newCount);
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
		if(overFlowMenuLayoutPrimary!=null)
		{
			overFlowMenuLayoutPrimary.updateOverflowMenuItemString(itemId, newTitle);
		}
		if(overFlowMenuLayoutSecondary!=null)
		{
			overFlowMenuLayoutSecondary.updateOverflowMenuItemString(itemId, newTitle);
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
		if(overFlowMenuLayoutPrimary!=null)
		{
			overFlowMenuLayoutPrimary.updateOverflowMenuItemActiveState(itemId, enabled, true);
		}
		if(overFlowMenuLayoutSecondary!=null)
		{
			overFlowMenuLayoutSecondary.updateOverflowMenuItemActiveState(itemId, enabled, true);
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
		if(overFlowMenuLayoutPrimary!=null)
		{
			overFlowMenuLayoutPrimary.updateOverflowMenuItemIcon(itemId, drawableId);
		}
		if(overFlowMenuLayoutSecondary!=null)
		{
			overFlowMenuLayoutSecondary.updateOverflowMenuItemIcon(itemId, drawableId);
		}
	}

	/**
	 * This is used to update/show indicator image on the overflow menu icon. This will be called from the UI Thread
	 * 
	 * Can also be used by any other futuristic feature
	 * 
	 * Count indicator takes priority over image indicator.
	 * 
	 * By Default imageview has red background (@drawable/ic_top_bar_indicator) , setAsbackground is flag to set image as background instead
	 * 
	 * @return
	 * 	true - if the indicator image is successfully displayed.
	 * 	false - if the image can not be displayed. This will also happen if the counter is currently visible.
	 */
	public boolean updateOverflowMenuIndicatorImage(int imadeResId,boolean setAsBackground)
	{
		MenuItem menuItem = getMenuItem(R.id.overflow_menu);
		
		if (menuItem != null && MenuItemCompat.getActionView(menuItem) != null)
		{
			ImageView topBarIndiImage = (ImageView) MenuItemCompat.getActionView(menuItem).findViewById(R.id.top_bar_indicator_img);
			TextView topBarCounter = (TextView) MenuItemCompat.getActionView(menuItem).findViewById(R.id.top_bar_indicator_text);

			if (imadeResId != 0 && topBarCounter.getVisibility() != View.VISIBLE)
			{
				if(setAsBackground)
				{
					topBarIndiImage.setBackgroundResource(imadeResId);
				}
				else
				{
					topBarIndiImage.setImageResource(imadeResId);
				}
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
				updateOverflowMenuIndicatorImage(0,false);
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
		if (overFlowMenuLayoutPrimary != null)
		{
			overFlowMenuLayoutPrimary.removeItem(id);
		}
		if (overFlowMenuLayoutSecondary != null)
		{
			overFlowMenuLayoutSecondary.removeItem(id);
		}
	}

	public void releseResources()
	{
		if (overFlowMenuLayoutPrimary != null)
		{
			overFlowMenuLayoutPrimary.releaseResources();
			overFlowMenuLayoutPrimary = null;
		}
		if (overFlowMenuLayoutSecondary != null)
		{
			overFlowMenuLayoutSecondary.releaseResources();
			overFlowMenuLayoutSecondary = null;
		}
	}
	
	public boolean isOverflowMenuShowing()
	{
		if (overFlowMenuLayoutPrimary != null && overFlowMenuLayoutPrimary.isShowing())
		{
			return true;
		}
		if (overFlowMenuLayoutSecondary != null && overFlowMenuLayoutSecondary.isShowing())
		{
			return true;
		}
		
		return false;
	}

	public void dismissOverflowMenu()
	{
		if (overFlowMenuLayoutPrimary != null)
		{
			overFlowMenuLayoutPrimary.dismiss();
		}
		if (overFlowMenuLayoutSecondary != null)
		{
			overFlowMenuLayoutSecondary.dismiss();
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
		if (overFlowMenuLayoutPrimary != null)
		{
			overFlowMenuLayoutPrimary.setShouldAvoidDismissOnClick(shouldAvoidDismissOnClick);
		}
		if (overFlowMenuLayoutSecondary != null)
		{
			overFlowMenuLayoutSecondary.setShouldAvoidDismissOnClick(shouldAvoidDismissOnClick);
		}
	}
	
	/**
	 * Utility method to refresh the overflow menu item
	 * 
	 * @param item
	 */
	public void refreshOverflowMenuItem(OverFlowMenuItem item)
	{
		if (overFlowMenuLayoutPrimary != null)
		{
			overFlowMenuLayoutPrimary.refreshOverflowMenuItm(item);
		}
		if (overFlowMenuLayoutSecondary != null)
		{
			overFlowMenuLayoutSecondary.refreshOverflowMenuItm(item);
		}
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		if (item.id == R.string.more)
		{
			dismissOverflowMenu();
			showSecondaryOverflowMenu();
		}
		else
		{
			overflowItemClickListener.itemClicked(item);
		}
		
	}
}
