package com.bsb.hike.media;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class OverFlowMenuLayout implements OnItemClickListener
{
	public static interface OverflowViewListener
	{
		/**
		 * This method will be called with a list of all overflow menu items. You can edit your menu items in this callback
		 * 
		 * @param overflowItems
		 */
		public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems);
	}

	protected Context context;

	protected List<OverFlowMenuItem> overflowItems;

	protected OverflowItemClickListener listener;

	protected View viewToShow;

	protected PopUpLayout popUpLayout;

	private OnDismissListener mOnDismisslistener;

	private OverflowViewListener viewListener;
	
	private ListView overFlowListView;
	
	private boolean shouldAvoidDismissOnClick = false;
	
	private LayoutAnimationController lac;

	/**
	 * This class is made to show overflow menu items, by default it populates listview of items you want o display, if some other view is required, extend this class and override
	 * initview and getview
	 * 
	 * @param overflowItems
	 * @param listener
	 * @param context
	 */
	public OverFlowMenuLayout(List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener, Context context)
	{
		this.overflowItems = overflowItems;
		this.listener = listener;
		this.context = context;
		this.mOnDismisslistener = onDismissListener;
		popUpLayout = new PopUpLayout(context);
	}

	public View getView()
	{
		return viewToShow != null ? viewToShow : initView();
	}

	public View initView()
	{
		// TODO : Copypasted code from chat thread, make separate layout file
		if (viewToShow != null)
		{
			return viewToShow;
		}
		viewToShow = LayoutInflater.from(context).inflate(R.layout.overflow_menu, null);
		overFlowListView = (ListView) viewToShow.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(context, 0, 0, overflowItems)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(context).inflate(R.layout.over_flow_menu_item, parent, false);
				}

				OverFlowMenuItem item = getItem(position);

				populateViewsForPosition(item, position, convertView);
				return convertView;
			}

			@Override
			public boolean isEnabled(int position)
			{
				return getItem(position).enabled;
			}

		});
		overFlowListView.setOnItemClickListener(this);
		return viewToShow;
	}

	protected void populateViewsForPosition(OverFlowMenuItem item, int position, View convertView)
	{
		TextView itemTextView = (TextView) convertView.findViewById(R.id.item_title);
		if (item.enabled)
		{
			itemTextView.setTextColor(context.getResources().getColor(R.color.overflow_item_text_enabled));
		}
		else
		{
			itemTextView.setTextColor(context.getResources().getColor(R.color.overflow_item_text_disabled));
		}
		itemTextView.setText(item.text);
		itemTextView.setEnabled(item.enabled);

		if (item.drawableId != 0)
		{
			ImageView itemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
			itemIcon.setVisibility(View.VISIBLE);
			itemIcon.setBackgroundResource(item.drawableId);
			itemIcon.setImageResource(0);
		}
		
		else
		{
			convertView.findViewById(R.id.item_icon).setVisibility(View.GONE);
		}

		TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
		freeSmsCount.setVisibility(View.GONE);

		TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);

		if (item.unreadCount <= 0)
		{
			newGamesIndicator.setVisibility(View.GONE);
		}
		else
		{
			newGamesIndicator.setText(setUnreadCounter(item.unreadCount));
			newGamesIndicator.setVisibility(View.VISIBLE);
		}

		convertView.setEnabled(item.enabled);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{

		// If item is disabled
		if (!((OverFlowMenuItem) arg0.getAdapter().getItem(arg2)).enabled)
		{
			return;
		}
		listener.itemClicked((OverFlowMenuItem) arg0.getAdapter().getItem(arg2));
		if (!shouldAvoidDismissOnClick)
		{
			popUpLayout.dismiss();
		}
	}

	public void show(int width, int height, View anchor)
	{
		show(width, height, 0, 0, anchor);
	}

	public void show(int width, int height, View anchor, int inputMethodMode)
	{
		show(width, height, 0, 0, anchor, inputMethodMode);
	}

	public void show(int width, int height, int xOffset, int yOffset, View anchor)
	{
		show(width, height, xOffset, yOffset, anchor, PopupWindow.INPUT_METHOD_FROM_FOCUSABLE);
	}

	public void show(int width, int height, int xOffset, int yOffset, View anchor, int inputMethodMode)
	{
		initView();
		if (viewListener != null)
		{
			viewListener.onPrepareOverflowOptionsMenu(overflowItems);
			if (!Utils.isHoneycombOrHigher())
			{
				notifyDateSetChanged();
			}
		}
		popUpLayout.showPopUpWindow(width, height, xOffset, yOffset, anchor, getView(), inputMethodMode);
		popUpLayout.setOnDismissListener(mOnDismisslistener);
	}

	public void appendItem(OverFlowMenuItem item)
	{
		this.overflowItems.add(item);
	}

	public void appendItem(OverFlowMenuItem item, int position)
	{
		this.overflowItems.add(position, item);
	}

	public void appendItems(OverFlowMenuItem... items)
	{
		for (OverFlowMenuItem item : items)
		{
			this.overflowItems.add(item);
		}
	}

	public void removeItem(int id)
	{
		Iterator<OverFlowMenuItem> iterator = overflowItems.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().id == id)
			{
				iterator.remove();
				break;
			}
		}
	}

	public List<OverFlowMenuItem> getOverFlowMenuItems()
	{
		return overflowItems;
	}

	public void notifyDateSetChanged()
	{
		if (viewToShow != null)
		{
			ListView overFlowListView = (ListView) viewToShow.findViewById(R.id.overflow_menu_list);

			((ArrayAdapter) overFlowListView.getAdapter()).notifyDataSetChanged();
		}

	}

	private String setUnreadCounter(int counter)
	{
		if (counter >= HikeConstants.MAX_PIN_CONTENT_LINES_IN_HISTORY)
		{
			return context.getString(R.string.max_pin_unread_counter);
		}

		else
		{
			return Integer.toString(counter);
		}
	}

	/**
	 * Can be used to update the unread count of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param newCount
	 */
	public void updateOverflowMenuItemCount(int itemId, int newCount)
	{
		List<OverFlowMenuItem> mItems = getOverFlowMenuItems();

		/**
		 * Defensive check
		 */
		if (mItems != null)
		{
			for (OverFlowMenuItem overFlowMenuItem : mItems)
			{
				/**
				 * Updating only if the count has changed
				 */

				if (overFlowMenuItem.id == itemId && overFlowMenuItem.unreadCount != newCount)
				{
					overFlowMenuItem.unreadCount = newCount;
					notifyDateSetChanged();
					break;
				}
			}
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
		List<OverFlowMenuItem> mItems = getOverFlowMenuItems();

		/**
		 * Defensive check
		 */
		if (mItems != null)
		{
			for (OverFlowMenuItem overFlowMenuItem : mItems)
			{
				if (overFlowMenuItem.id == itemId)
				{
					overFlowMenuItem.text = newTitle;
					notifyDateSetChanged();
					break;
				}
			}
		}

	}

	/**
	 * Can be used to update the active state of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param enabled
	 * @return whether an item was updated or not
	 */
	public boolean updateOverflowMenuItemActiveState(int itemId, boolean enabled, boolean refreshUI)
	{
		List<OverFlowMenuItem> mItems = getOverFlowMenuItems();

		/**
		 * Defensive check
		 */
		if (mItems != null)
		{
			for (OverFlowMenuItem overFlowMenuItem : mItems)
			{
				if (overFlowMenuItem.id == itemId)
				{
					overFlowMenuItem.enabled = enabled;
					if (refreshUI)
					{
						notifyDateSetChanged();
					}
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Can be used to update the icon of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param enabled
	 */
	public void updateOverflowMenuItemIcon(int itemId, int drawableId)
	{
		List<OverFlowMenuItem> mItems = getOverFlowMenuItems();

		/**
		 * Defensive check
		 */
		if (mItems != null)
		{
			for (OverFlowMenuItem overFlowMenuItem : mItems)
			{
				if (overFlowMenuItem.id == itemId)
				{
					overFlowMenuItem.drawableId = drawableId;
					notifyDateSetChanged();
					break;
				}
			}
		}
	}

	public void setOverflowViewListener(OverflowViewListener viewListener)
	{
		this.viewListener = viewListener;
	}

	public void releaseResources()
	{
		this.viewListener = null;
		this.listener = null;
		this.mOnDismisslistener = null;
		this.viewListener = null;
		this.popUpLayout = null;
	}

	public boolean isShowing()
	{
		if (popUpLayout != null)
		{
			return popUpLayout.isShowing();
		}

		return false;
	}

	public void dismiss()
	{
		if (popUpLayout != null)
		{
			popUpLayout.dismiss();
		}
	}

	/**
	 * @param shouldAvoidDismissOnClick the shouldAvoidDismissOnClick to set
	 */
	public void setShouldAvoidDismissOnClick(boolean shouldAvoidDismissOnClick)
	{
		this.shouldAvoidDismissOnClick = shouldAvoidDismissOnClick;
	}

	/**
	 * Get position for a given itemId
	 * 
	 * @param itemId
	 * @return
	 */
	private int getItemPosition(int itemId)
	{
		if (overflowItems != null)
		{
			int i = 0;
			for (OverFlowMenuItem item : overflowItems)
			{
				if (item.id == itemId)
				{
					return i;
				}

				i++;
			}
		}
		
		return -1;
	}
	
	/**
	 * Utility method to refresh a overflow menu item
	 * 
	 * @param item
	 */
	public void refreshOverflowMenuItm(OverFlowMenuItem item)
	{
		int position = getItemPosition(item.id);
		
		if (position != -1 && viewToShow != null)
		{
			ListView list = (ListView) viewToShow.findViewById(R.id.overflow_menu_list);
			if (list != null)
			{
				View convertView = list.getChildAt(position);
				
				if (convertView != null)
				{
					populateViewsForPosition(item, position, convertView);
				}
			}
		}
	}
	
	/**
	 * Commenting it out for now based on perf issues
	 */
	public void setAnimation()
	{
//		if (overFlowListView != null)
//		{
//			if (lac == null)
//			{
//				lac = new LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.translate_from_top), 0.15f);
//			}
//			
//			overFlowListView.setLayoutAnimation(lac);
//		}
	}
}