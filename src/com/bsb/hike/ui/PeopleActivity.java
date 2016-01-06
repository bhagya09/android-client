package com.bsb.hike.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.modules.kpt.HikeCustomKeyboard;
import com.bsb.hike.modules.kpt.KptUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.ui.v7.SearchView;
import com.bsb.hike.ui.v7.SearchView.OnQueryTextListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.kpt.adaptxt.beta.KPTAddonItem;
import com.kpt.adaptxt.beta.RemoveDialogData;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

public class PeopleActivity extends HikeAppStateBaseFragmentActivity implements Listener, AdaptxtKeyboordVisibilityStatusListner
{
	FriendsFragment mainFragment;
	private HikeCustomKeyboard mCustomKeyboard;
	private AdaptxtEditText searchET;
	private MenuItem searchMenuItem;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		initialisePeopleScreen(savedInstanceState);
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.FAVOURITES.ordinal());
		
	}

	private void initialisePeopleScreen(Bundle savedInstanceState)
	{

		setContentView(R.layout.home);
		LinearLayout viewHolder = (LinearLayout) findViewById(R.id.keyboardView_holder);
		if (!HikeMessengerApp.isSystemKeyboard()) {
			mCustomKeyboard = new HikeCustomKeyboard(PeopleActivity.this, viewHolder,
					KPTConstants.MULTILINE_LINE_EDITOR, null,
					PeopleActivity.this);
		}
		setupMainFragment(savedInstanceState);
		setupActionBar();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.ic_top_bar_search);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.favorites);

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}


	private void setupMainFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null) {
            return;
        }
		
        mainFragment = new FriendsFragment();
        
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_screen, mainFragment).commit();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.country_select_menu, menu);
		 searchMenuItem = menu.findItem(R.id.search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		searchView.clearFocus();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		searchView.setOnQueryTextListener(onQueryTextListener);
		searchET = (AdaptxtEditText) searchView
				.findViewById(R.id.search_src_text);
		if (!KptUtils.isSystemKeyboard()) {
			mCustomKeyboard.registerEditText(searchET);
			mCustomKeyboard.init(searchET);
		}
		searchET.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!KptUtils.isSystemKeyboard()&& mCustomKeyboard!=null)
				{
					if (hasFocus)
					{
						mCustomKeyboard.showCustomKeyboard(v, true);
					}
					else
					{
						mCustomKeyboard.showCustomKeyboard(v, false);
						mCustomKeyboard.updateCore();
					}
				}
				else
				{
					if (hasFocus)
					{
						Utils.toggleSoftKeyboard(PeopleActivity.this.getApplicationContext());
					}
				}
			}
		});

		MenuItemCompat.setShowAsAction(MenuItemCompat.setActionView(searchMenuItem, searchView), MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener()
		{

			@Override
			public boolean onMenuItemActionExpand(MenuItem item)
			{
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item)
			{
				searchView.setQuery("", true);
				if (mCustomKeyboard != null && mCustomKeyboard.isCustomKeyboardVisible()) {
					mCustomKeyboard.showCustomKeyboard(searchET, false);
				}

				return true;
			}
		});

		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return super.onPrepareOptionsMenu(menu);
	}

	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener()
	{

		@Override
		public boolean onQueryTextSubmit(String query)
		{
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.FRIENDS_TAB_QUERY, newText);
			return true;
		}
	};
	
	@Override
	public void onBackPressed()
	{
		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_NOTIFICATION, false))
		{
			Intent intent = new Intent(PeopleActivity.this, HomeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
		// if custom keyboard is showing close it
		if (mCustomKeyboard != null && mCustomKeyboard.isCustomKeyboardVisible()) {
			mCustomKeyboard.showCustomKeyboard(searchET, false);
			return;
		}
		//close action mode
		if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
			searchMenuItem.collapseActionView();
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onStop()
	{
		/*
		 * Ensuring we reset when leaving the activity as well, since we might receive a request when we were in this activity.
		 */
		Utils.resetUnseenFriendRequestCount(this);
		super.onStop();
	}

	@Override
	protected void onResume()
	{
		Utils.resetUnseenFriendRequestCount(this);
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		super.onPause();
	}

	@Override
	public void analyticalData(KPTAddonItem kptAddonItem)
	{
		KptUtils.generateKeyboardAnalytics(kptAddonItem);
	}

	@Override
	public void onInputViewCreated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputviewVisbility(boolean arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showGlobeKeyView() {
		KptUtils.onGlobeKeyPressed(PeopleActivity.this, mCustomKeyboard);
		
	}

	@Override
	public void showQuickSettingView() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dismissRemoveDialog() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showRemoveDialog(RemoveDialogData arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		if (mCustomKeyboard != null)
		{
			mCustomKeyboard.onConfigurationChanged(newConfig);			
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {

		KptUtils.destroyKeyboardResources(mCustomKeyboard, R.id.search_src_text);
			super.onDestroy();
	}
}
