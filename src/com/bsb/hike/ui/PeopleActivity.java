package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class PeopleActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	FriendsFragment mainFragment;
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
		parent.setContentInsetsAbsolute(0,0);
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
		MenuItem searchMenuItem = menu.findItem(R.id.search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		searchView.clearFocus();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		searchView.setOnQueryTextListener(onQueryTextListener);
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

}
