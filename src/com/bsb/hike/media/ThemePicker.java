package com.bsb.hike.media;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.BackPressListener;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;

public class ThemePicker implements BackPressListener, OnDismissListener, OnClickListener
{

	private static final String TAG = "themepicker";

	public static interface ThemePickerListener
	{
		public void themeClicked(ChatTheme theme);

		public void themeSelected(ChatTheme theme);

		public void themeCancelled();
	}

	private SherlockFragmentActivity sherlockFragmentActivity;

	private View viewToDisplay;

	private ActionMode actionMode;

	private ChatTheme userSelection;

	private ThemePickerListener listener;

	private boolean listenerInvoked = false, reInflation;
	
	private PopUpLayout popUpLayout;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

	public ThemePicker(SherlockFragmentActivity sherlockFragmentActivity, ThemePickerListener listener, ChatTheme currentTheme)
	{
		this.userSelection = currentTheme;
		this.sherlockFragmentActivity = sherlockFragmentActivity;
		this.listener = listener;
		this.popUpLayout = new PopUpLayout(sherlockFragmentActivity.getApplicationContext());
	}

	/**
	 * This method calls {@link #showThemePicker(int, int, View, ChatTheme)} with offset as 0
	 */
	public void showThemePicker(View anchor, ChatTheme currentTheme, int footerTextResId, int orientation)
	{
		showThemePicker(0, 0, anchor, currentTheme, footerTextResId, orientation);
	}

	/**
	 * This method shows theme picker and changes action bar as per theme picker requirement , internally it uses {@link #showPopUpWindowNoDismiss(int, int, View)}
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 * @param currentTheme
	 */
	public void showThemePicker(int xoffset, int yoffset, View anchor, ChatTheme currentTheme, int footerTextResId, int orientation)
	{
		Logger.i(TAG, "show theme picker");
		this.userSelection = currentTheme;
		sherlockFragmentActivity.startActionMode(actionmodeCallback);
		initView(footerTextResId, orientation);
		popUpLayout.showPopUpWindowNoDismiss(xoffset, yoffset, anchor, getView());
		popUpLayout.setOnDismissListener(this);
	}

	public View getView()
	{
		return viewToDisplay;
	}

	/**
	 * This method inflates view needed to show theme picker, if view is inflated already (not null) We simply return
	 */
	public void initView(int footerTextResId, int orientation)
	{
		if (viewToDisplay != null)
		{
			/**
			 * If orientation was changed, we need to refresh views
			 */
			if (orientationChanged(orientation))
			{
				refreshViews(false);
				currentConfig = orientation;
			}
			
			return;
		}
		
		View parentView = viewToDisplay = sherlockFragmentActivity.getLayoutInflater().inflate(R.layout.chat_backgrounds, null);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView.findViewById(R.id.chat_theme_tip);

		chatThemeTip.setText(footerTextResId);

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		final ArrayAdapter<ChatTheme> gridAdapter = new ArrayAdapter<ChatTheme>(sherlockFragmentActivity.getApplicationContext(), -1, ChatTheme.values())
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(sherlockFragmentActivity).inflate(R.layout.chat_bg_item, parent, false);
				}
				ChatTheme chatTheme = getItem(position);

				ImageView theme = (ImageView) convertView.findViewById(R.id.theme);
				ImageView animatedThemeIndicator = (ImageView) convertView.findViewById(R.id.animated_theme_indicator);

				animatedThemeIndicator.setVisibility(chatTheme.isAnimated() ? View.VISIBLE : View.GONE);
				theme.setBackgroundResource(chatTheme.previewResId());
				theme.setEnabled(userSelection == chatTheme);

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);
		if (userSelection != null)
		{
			int selection = userSelection.ordinal();
			attachmentsGridView.setSelection(selection);
		}
		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				ChatTheme selected = ChatTheme.values()[position];
				gridAdapter.notifyDataSetChanged();
				if (selected != userSelection)
				{
					listener.themeClicked(selected);
				}
				userSelection = selected;
			}
		});

	}

	private boolean orientationChanged(int orientation)
	{
		return currentConfig != orientation;
	}

	private ActionMode.Callback actionmodeCallback = new ActionMode.Callback()
	{
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			Logger.i(TAG, "on prepare actionmode");
			
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			Logger.i(TAG, "on destroy actionmode");
			/**
			 * Proceeding only if there was no reinflation
			 */
			if (!reInflation)
			{
				actionMode = null;
				popUpLayout.dismiss();
				// we are not getting click event of close button in action bar, so
				// if action bar is closed because of click there, we fallback
				// onlistenerInvoked listenerInvoked becomes true if we click on
				// done button in action bar
				if (!listenerInvoked)
				{
					listener.themeCancelled();
					listenerInvoked = false;
				}
			}
			
			reInflation = false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			Logger.i(TAG, "on create action mode");
			actionMode = mode;
			mode.setCustomView(LayoutInflater.from(sherlockFragmentActivity).inflate(R.layout.hike_action_mode, null));
			
			View saveThemeBtn = mode.getCustomView().findViewById(R.id.done_container);

			saveThemeBtn.startAnimation(AnimationUtils.loadAnimation(sherlockFragmentActivity, R.anim.scale_in));

			saveThemeBtn.setOnClickListener(ThemePicker.this);
			
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			Logger.i(TAG, "onActionItemClicked");
			return false;
		}
	};

	@Override
	public boolean onBackPressed()
	{
		if (popUpLayout.isShowing())
		{
			actionMode.finish();
			return true;
		}
		return false;
	}

	private int getNumColumnsChatThemes()
	{
		Resources resources = sherlockFragmentActivity.getResources();
		int width = resources.getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * resources.getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemeGridMargin = 2 * resources.getDimensionPixelSize(R.dimen.chat_theme_grid_margin);

		int chatThemeGridWidth = width - chatThemeGridMargin - chatThemePaletteMargin;

		int chatThemeItemWidth = resources.getDimensionPixelSize(R.dimen.chat_bg_item_width);

		return (int) (chatThemeGridWidth / chatThemeItemWidth);
	}

	@Override
	public void onDismiss()
	{
		if (actionMode != null)
			actionMode.finish();
	}

	@Override
	public void onClick(View arg0)
	{
		if (arg0.getId() == R.id.done_container)
		{
			listener.themeSelected(userSelection);
			listenerInvoked = true;
			popUpLayout.dismiss();
		}
	}

	public boolean isShowing()
	{
		return popUpLayout.isShowing();
	}
	
	/**
	 * This method changes the number of columns field of the grid view and then calls notifyDataSetChanged
	 */
	public void refreshViews(boolean reInflateActionBar)
	{
		GridView grid = (GridView) viewToDisplay.findViewById(R.id.attachment_grid);
		grid.setNumColumns(getNumColumnsChatThemes());
		((ArrayAdapter<ChatTheme>) grid.getAdapter()).notifyDataSetChanged();
		
		if (reInflateActionBar)
		{
			reInflation = true;
			sherlockFragmentActivity.startActionMode(actionmodeCallback);
		}
	}
}
