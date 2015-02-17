package com.bsb.hike.chatthread;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;

public class HikeActionMode implements ActionMode.Callback, OnClickListener
{
	public static interface ActionModeListener
	{
		public void actionModeDestroyed(int actionModeId);

		public void doneClicked(int actionModeId);

		public void initActionbarActionModeView(int actionModeId, View view);
		
		public boolean onActionItemClicked(int actionModeId, MenuItem menuItem);
	}

	private static final int DEFAULT_LAYOUT_RESID = R.layout.hike_action_mode;

	protected ActionMode mActionMode;

	protected SherlockFragmentActivity mActivity;

	private int defaultLayoutId = DEFAULT_LAYOUT_RESID, actionModeId;
	
	private String actionModeTitle = "";
	
	private String doneButtonText = "";

	private ActionModeListener mListener;
	
	private boolean shouldInflateMenu;
	
	private int menuResId = -1;
	
	private Menu mMenu;

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity, ActionModeListener listener)
	{
		this(sherlockFragmentActivity, "", "", DEFAULT_LAYOUT_RESID, listener);
	}

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity, int layoutId, ActionModeListener listener)
	{
		this(sherlockFragmentActivity, "", "", layoutId, listener);
	}

	public HikeActionMode(SherlockFragmentActivity sherlockFragmentActivity, String title, String save, int layoutId, ActionModeListener listener)
	{
		this.mActivity = sherlockFragmentActivity;
		this.defaultLayoutId = layoutId;
		this.actionModeTitle = title;
		this.doneButtonText = save;
		this.mListener = listener;
	}

	protected void startActionMode()
	{
		mActivity.startActionMode(this);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		this.mActionMode = mode;
		View view = LayoutInflater.from(mActivity).inflate(defaultLayoutId, null);
		mode.setCustomView(view);
		if(shouldInflateMenu)
		{
			inflateMenu(menu);
		}
		
		if (mListener != null)
		{
			mListener.initActionbarActionModeView(actionModeId, view);
		}

		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		initView();
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		if (mListener != null)
		{
			return mListener.onActionItemClicked(actionModeId, item);
		}
		
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		if (mListener != null)
		{
			mListener.actionModeDestroyed(actionModeId);
		}
		actionBarDestroyed();
	}

	public void showActionMode(int id)
	{
		showActionMode(id, defaultLayoutId);
	}

	public void showActionMode(int id, int layoutId)
	{
		this.defaultLayoutId = layoutId;
		showActionMode(id, actionModeTitle, doneButtonText);
	}

	public void showActionMode(int id, String title, String doneButtonText)
	{
		this.actionModeId = id;
		this.actionModeTitle = title;
		this.doneButtonText = doneButtonText;
		mActivity.startActionMode(this);
	}
	
	/**
	 * Used to show an actionMode with a custom menu. The menu layout resId is passed in the params
	 * 
	 * @param id
	 * @param title
	 * @param showMenu
	 * @param menuResId
	 */
	public void showActionMode(int id, String title, boolean showMenu, int menuResId)
	{
		this.actionModeId = id;
		this.actionModeTitle = title;
		this.shouldInflateMenu = showMenu;
		this.menuResId = showMenu ? menuResId : -1;
		mActivity.startActionMode(this);
	}

	private void initDefaultView()
	{
		setText(R.id.title, actionModeTitle, -1);
		setText(R.id.save, doneButtonText, R.anim.scale_in);
		mActionMode.getCustomView().findViewById(R.id.done_container).setOnClickListener(this);
	}

	/**
	 * Override this function in case you have different custom xml
	 */
	protected void initView()
	{
		if (defaultLayoutId == DEFAULT_LAYOUT_RESID)
		{
			initDefaultView();
		}
		else
		{
			if (mListener != null)
			{
				mListener.initActionbarActionModeView(actionModeId, mActionMode.getCustomView());
			}
		}
	}

	private void setText(int viewId, String text, int animId)
	{
		if (viewId != -1)
		{
			TextView tv = (TextView) mActionMode.getCustomView().findViewById(viewId);
			tv.setText(text);
			if (animId != -1)
			{
				tv.startAnimation(AnimationUtils.loadAnimation(mActivity, animId));
			}
		}
	}
	
	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.done_container)
		{
			doneClicked();
		}
	}

	protected void actionBarDestroyed()
	{
		this.mActionMode = null;
	}

	protected void doneClicked()
	{
		if (mListener != null)
		{
			mListener.doneClicked(actionModeId);
		}
	}

	public boolean onBackPressed()
	{
		if (mActionMode != null)
		{
			finish();
			return true;
		}
		
		return false;
	}

	public void finish()
	{
		if (mActionMode != null)
		{
			mActionMode.finish();
			actionModeId = -1;
			menuResId = -1;	
			shouldInflateMenu = false;
		}
	}
	
	private void inflateMenu(Menu menu)
	{
		if (menuResId == -1)
		{
			throw new RuntimeException("Trying to inflate menu with menuId as -1");
		}
		MenuInflater mMenuInflater = mActionMode.getMenuInflater();
		mMenuInflater.inflate(menuResId, menu);
		this.mMenu = menu;
		
		/**
		 * Also hide doneButton containers
		 */
		hideView(R.id.done_container);
		hideView(R.id.done_container_divider);
	}
	
	/**
	 * -1 indicates no action mode is on currently
	 * @return
	 */
	public int whichActionModeIsOn()
	{
		if (mActionMode != null)
		{
			return actionModeId;
		}

		return -1;
	}
	
	public boolean isActionModeOn()
	{
		return (mActionMode != null);
	}
	
	public void updateTitle(String title)
	{
		setText(R.id.title, title, -1);
	}
	
	public void showHideMenuItem(int menuItemResId, boolean show)
	{
		if (mMenu != null)
		{
			MenuItem item = mMenu.findItem(menuItemResId);
			if (item != null)
			{
				item.setVisible(show);
			}
		}
	}
	
	private void hideView(int resId)
	{
		mActionMode.getCustomView().findViewById(resId).setVisibility(View.GONE);
	}
}
