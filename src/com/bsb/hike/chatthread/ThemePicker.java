package com.bsb.hike.chatthread;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;

public class ThemePicker extends PopUpLayout implements BackPressListener,
		OnDismissListener, OnTouchListener, OnClickListener {
	private static final String TAG = "themepicker";

	public static interface ThemePickerListener {
		public void themeClicked(ChatTheme theme);

		public void themeSelected(ChatTheme theme);

		public void themeCancelled();
	}

	private SherlockFragmentActivity sherlockFragmentActivity;
	private View viewToDisplay;
	private ActionMode actionMode;
	private ChatTheme currentSelected;
	private ThemePickerListener listener;
	private boolean listenerInvoked = false;

	public ThemePicker(SherlockFragmentActivity sherlockFragmentActivity,
			ThemePickerListener listener) {
		super(sherlockFragmentActivity.getApplicationContext());
		this.sherlockFragmentActivity = sherlockFragmentActivity;
		this.listener = listener;
	}

	/**
	 * This method calls {@link #showThemePicker(int, int, View, ChatTheme)}
	 * with offset as 0
	 */
	public void showThemePicker(View anchor, ChatTheme currentTheme) {
		showThemePicker(0, 0, anchor, currentTheme);
	}

	/**
	 * This method shows theme picker and changes action bar as per theme picker
	 * requirement , internally it uses
	 * {@link #showPopUpWindowNoDismiss(int, int, View)}
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 * @param currentTheme
	 */
	public void showThemePicker(int xoffset, int yoffset, View anchor,
			ChatTheme currentTheme) {
		Logger.i(TAG, "show theme picker");
		// do processing
		sherlockFragmentActivity.startActionMode(actionmodeCallback);
		showPopUpWindowNoDismiss(xoffset, yoffset, anchor);
		popup.setOnDismissListener(this);
	}

	@Override
	public View getView() {
		return viewToDisplay;
	}

	/**
	 * This methos inflates view needed to show theme picker, if view is
	 * inflated already (not null) We simply return
	 */
	@Override
	public void initView() {
		if (viewToDisplay != null) {
			return;
		}
		View parentView = viewToDisplay = sherlockFragmentActivity
				.getLayoutInflater().inflate(R.layout.chat_backgrounds, null);

		GridView attachmentsGridView = (GridView) parentView
				.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView
				.findViewById(R.id.chat_theme_tip);

		// chatThemeTip.setText(mConversation instanceof GroupConversation ?
		// R.string.chat_theme_tip_group : R.string.chat_theme_tip);
		// chatThemeTip.setVisibility(mConversation.isOnhike() ? View.VISIBLE :
		// View.GONE);
		chatThemeTip.setText(R.string.chat_theme_tip_group);

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		final ArrayAdapter<ChatTheme> gridAdapter = new ArrayAdapter<ChatTheme>(
				sherlockFragmentActivity.getApplicationContext(), -1,
				ChatTheme.values()) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(sherlockFragmentActivity)
							.inflate(R.layout.chat_bg_item, parent, false);
				}
				ChatTheme chatTheme = getItem(position);

				ImageView theme = (ImageView) convertView
						.findViewById(R.id.theme);
				ImageView animatedThemeIndicator = (ImageView) convertView
						.findViewById(R.id.animated_theme_indicator);

				animatedThemeIndicator
						.setVisibility(chatTheme.isAnimated() ? View.VISIBLE
								: View.GONE);
				theme.setBackgroundResource(chatTheme.previewResId());
				theme.setEnabled(currentSelected == chatTheme);

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);
		if (currentSelected != null) {
			int selection = currentSelected.ordinal();
			attachmentsGridView.setSelection(selection);
		}
		attachmentsGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				currentSelected = ChatTheme.values()[position];
				gridAdapter.notifyDataSetChanged();
				listener.themeClicked(currentSelected);
			}
		});

	}

	private ActionMode.Callback actionmodeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			Logger.i(TAG, "on prepare actionmode");
			View saveThemeBtn = mode.getCustomView().findViewById(
					R.id.done_container);

			saveThemeBtn.startAnimation(AnimationUtils.loadAnimation(
					sherlockFragmentActivity, R.anim.scale_in));

			saveThemeBtn.setOnClickListener(ThemePicker.this);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Logger.i(TAG, "on destroy actionmode");
			actionMode = null;
			dismiss();
			// we are not getting click event of close button in action bar, so
			// if action bar is closed because of click there, we fallback
			// onlistenerInvoked listenerInvoked becomes true if we click on
			// done button in action bar
			if (!listenerInvoked) {
				listener.themeCancelled();
				listenerInvoked = false;
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Logger.i(TAG, "on create action mode");
			actionMode = mode;
			mode.setCustomView(LayoutInflater.from(sherlockFragmentActivity)
					.inflate(R.layout.chat_theme_action_bar, null));
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Logger.i(TAG, "onActionItemClicked");
			return false;
		}
	};

	@Override
	public boolean onBackPressed() {
		if (isShowing()) {
			actionMode.finish();
			return true;
		}
		return false;
	}

	private int getNumColumnsChatThemes() {
		Resources resources = sherlockFragmentActivity.getResources();
		int width = resources.getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * resources
				.getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemeGridMargin = 2 * resources
				.getDimensionPixelSize(R.dimen.chat_theme_grid_margin);

		int chatThemeGridWidth = width - chatThemeGridMargin
				- chatThemePaletteMargin;

		int chatThemeItemWidth = resources
				.getDimensionPixelSize(R.dimen.chat_bg_item_width);

		return (int) (chatThemeGridWidth / chatThemeItemWidth);
	}

	@Override
	public void onDismiss() {
		if (actionMode != null)
			actionMode.finish();
	}

	@Override
	public void onClick(View arg0) {
		if (arg0.getId() == R.id.done_container) {
			listener.themeSelected(currentSelected);
			listenerInvoked = true;
			dismiss();
		}
	}

}
