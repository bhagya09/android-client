package com.bsb.hike.media;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.chatthemes.HikeChatThemeConstants;
import com.bsb.hike.chatthread.BackPressListener;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;

public class ThemePicker implements BackPressListener, OnDismissListener, OnClickListener
{

	private static final String TAG = "themepicker";

	public static interface ThemePickerListener
	{
		public void themeClicked(String themeId);

		public void themeSelected(String themeId);

		public void themeCancelled();
	}

	private AppCompatActivity appCompatActivity;

	private View viewToDisplay;

	private ActionMode actionMode;

	// It contains the currently selected ChatThemeId in the current chat thread
	private String userSelection;
	
	private ThemePickerListener listener;

	private boolean listenerInvoked = false, reInflation;
	
	private PopUpLayout popUpLayout;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

	private ArrayList<String> availableThemes;

	private ArrayAdapter<String> gridAdapter = null;
	
	public ThemePicker(AppCompatActivity appCompatActivity, ThemePickerListener listener, String currentThemeId)
	{
		this.userSelection = currentThemeId;
		this.appCompatActivity = appCompatActivity;
		this.listener = listener;
		this.popUpLayout = new PopUpLayout(appCompatActivity.getApplicationContext());
		this.currentConfig = appCompatActivity.getResources().getConfiguration().orientation;
	}

	/**
	 * This method calls {@link #showThemePicker(int, int, View, ChatTheme)} with offset as 0
	 */
	public void showThemePicker(View anchor, String currentThemeId, int footerTextResId, int orientation)
	{
		showThemePicker(0, 0, anchor, currentThemeId, footerTextResId, orientation);
	}

	/**
	 * This method shows theme picker and changes action bar as per theme picker requirement , internally it uses {@link #showPopUpWindowNoDismiss(int, int, View)}
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param anchor
	 * @param currentTheme
	 */
	public void showThemePicker(int xoffset, int yoffset, View anchor, String currentThemeId, int footerTextResId, int orientation)
	{
		Logger.i(TAG, "show theme picker");
		this.userSelection = currentThemeId;
		appCompatActivity.startSupportActionMode(actionmodeCallback);
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
			setOrientation(orientation);
			return;
		}
		
		View parentView = viewToDisplay = appCompatActivity.getLayoutInflater().inflate(R.layout.chat_backgrounds, null);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView.findViewById(R.id.chat_theme_tip);

		chatThemeTip.setText(footerTextResId);

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		availableThemes = ChatThemeManager.getInstance().getAvailableThemeIds();
		if(ChatThreadUtils.isCustomChatThemeEnabled()) {
			availableThemes.add(0, HikeChatThemeConstants.THEME_PALETTE_CAMERA_ICON);
		}

		gridAdapter = new ArrayAdapter<String>(appCompatActivity.getApplicationContext(), -1, availableThemes)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(appCompatActivity).inflate(R.layout.chat_bg_item, parent, false);
				}
				ImageView animatedThemeIndicator = (ImageView) convertView.findViewById(R.id.animated_theme_indicator);
				ImageView theme = (ImageView) convertView.findViewById(R.id.theme);
				View animatedBackground = convertView.findViewById(R.id.theme_animated_backgroud);

				theme.clearAnimation();
				animatedThemeIndicator.setVisibility(View.GONE);
				animatedBackground.setVisibility(View.GONE);
				theme.setImageBitmap(null);
				theme.setBackground(null);

				if(getItem(position).equalsIgnoreCase(HikeChatThemeConstants.THEME_PALETTE_CAMERA_ICON)) {
					theme.setImageResource(R.drawable.ic_ct_camera);
					theme.setScaleType(ImageView.ScaleType.CENTER);

					if (HikeSharedPreferenceUtil.getInstance().getData(HikeChatThemeConstants.SHARED_PREF_CT_SHOW_FTUE_ANIMATION, true)){
						animatedBackground.setVisibility(View.VISIBLE);
						Animation anim = AnimationUtils.loadAnimation(appCompatActivity, R.anim.scale_out_from_mid);
						animatedBackground.startAnimation(anim);
						theme.setAnimation(HikeAnimationFactory.getStickerShopIconAnimation(appCompatActivity));
					}
				} else {
					HikeChatTheme chatTheme = ChatThemeManager.getInstance().getTheme(getItem(position));
					animatedThemeIndicator.setVisibility(chatTheme.isAnimated() ? View.VISIBLE : View.GONE);
					theme.setBackground(ChatThemeManager.getInstance().getDrawableForTheme(chatTheme.getThemeId(), HikeChatThemeConstants.ASSET_INDEX_THUMBNAIL));
					theme.setEnabled(userSelection.equals(chatTheme.getThemeId()));
				}

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);
		if (userSelection != null)
		{
			attachmentsGridView.setSelection(getThemePosition(availableThemes, userSelection));
		}

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				gridAdapter.notifyDataSetChanged();
				if (availableThemes.get(position) != userSelection)
				{
					listener.themeClicked(availableThemes.get(position));
				}
				userSelection = availableThemes.get(position);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeChatThemeConstants.SHARED_PREF_CT_SHOW_FTUE_ANIMATION, false);
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
				}
				
				listenerInvoked = false;
			}
			
			reInflation = false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			Logger.i(TAG, "on create action mode");
			actionMode = mode;
			mode.setCustomView(LayoutInflater.from(appCompatActivity).inflate(R.layout.hike_action_mode, null));
			
			View saveThemeBtn = mode.getCustomView().findViewById(R.id.done_container);

			saveThemeBtn.startAnimation(AnimationUtils.loadAnimation(appCompatActivity, R.anim.scale_in));

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
		/**
		 * Can add other cases in future here as well.
		 */
		return dismiss();
	}
	
	public boolean dismiss()
	{
		if (popUpLayout.isShowing())
		{
			actionMode.finish();
			return true;
		}

		return false;
	}
	
	/**
	 * This function should be called when orientation of screen is changed, it will update its view based on orientation
	 * If picker is being shown, it will first dismiss current picker and then show it again using post on view
	 * 
	 * NOTE : It will not give dismiss callback to listener as this is not explicit dismiss
	 * @param orientation
	 */
	public void onOrientationChange(int orientation)
	{
		setOrientation(orientation);
		if(viewToDisplay!=null && isShowing())
		{
			reInflation = true;
			dismiss();
			viewToDisplay.post(new Runnable()
			{
				@Override
				public void run()
				{
					appCompatActivity.startSupportActionMode((ActionMode.Callback) actionmodeCallback);
				}
			});
		}
		
	}

	private int getNumColumnsChatThemes()
	{
		Resources resources = appCompatActivity.getResources();
		int width = resources.getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * resources.getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemePalettePadding = 2 * resources.getDimensionPixelSize(R.dimen.chat_theme_palette_padding);

		int chatThemeGridWidth = width - chatThemePalettePadding - chatThemePaletteMargin;

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
	public void refreshViews()
	{
		GridView grid = (GridView) viewToDisplay.findViewById(R.id.attachment_grid);
		grid.setNumColumns(getNumColumnsChatThemes());

		availableThemes = ChatThemeManager.getInstance().getAvailableThemeIds();
		if(ChatThreadUtils.isCustomChatThemeEnabled()) {
			availableThemes.add(0, HikeChatThemeConstants.THEME_PALETTE_CAMERA_ICON);
		}
		if(gridAdapter != null) {
			gridAdapter.clear();
			gridAdapter.addAll(availableThemes);
			grid.setAdapter(gridAdapter);
			gridAdapter.notifyDataSetChanged();
		}
	}
	
	public void setOrientation(int orientation)
	{
		if(orientation != currentConfig)
		{
			this.currentConfig = orientation;
		}
		refreshViews();

	}

	public int getThemePosition(ArrayList<String> themeIds, String searchTheme)
	{
		for(int i=0;i<themeIds.size();i++)
		{
			if(themeIds.get(i).equals(searchTheme))
			{
				return i;
			}
		}
		return 0; // error code
	}

}
