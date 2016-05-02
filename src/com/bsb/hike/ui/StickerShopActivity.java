package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.StickerShopFragment;
import com.bsb.hike.ui.fragments.StickerShopSearchFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopActivity extends HikeAppStateBaseFragmentActivity
{
    private StickerShopFragment stickerShopFragment;

    private StickerShopSearchFragment stickerShopSearchFragment;

    private MenuItem shopSearchMenuItem;

    private RelativeLayout searchLayout;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sticker_shop_parent);
        setupShopFragment(savedInstanceState);
        showShopFragment();
        setupActionBar();
        showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STICKER_SHOP.ordinal());
        StickerManager.getInstance().initiateFetchCategoryRanksAndDataTask();
    }

    private void setupShopFragment(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {
            return;
        }

        stickerShopFragment = StickerShopFragment.newInstance();

    }

    private void setupShopSearchFragment()
    {
        stickerShopSearchFragment = StickerShopSearchFragment.newInstance();
    }

    private void showSearchFragment()
    {
        if(stickerShopSearchFragment == null)
        {
            setupShopSearchFragment();
        }

        if(stickerShopSearchFragment.isAdded())
        {
            return;
        }

        getSupportFragmentManager().beginTransaction().add(R.id.sticker_shop_parent, stickerShopSearchFragment).addToBackStack(null).commit();

    }

    private void showShopFragment()
    {
        if(stickerShopFragment == null )
        {
            setupShopFragment(null);
        }

        if(stickerShopFragment.isAdded())
        {
            return;
        }

        getSupportFragmentManager().beginTransaction().add(R.id.sticker_shop_parent, stickerShopFragment).commit();

    }

    public void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayHomeAsUpEnabled(true);
        View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);

        View backContainer = actionBarView.findViewById(R.id.back);
        TextView title = (TextView) actionBarView.findViewById(R.id.title);
        title.setText(R.string.sticker_shop);

        actionBar.setCustomView(actionBarView);
    }

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.sticker_shop_menu, menu);

		shopSearchMenuItem = menu.findItem(R.id.shop_search);

        shopSearchMenuItem.setVisible(StickerManager.getInstance().isShopSearchEnabled());

		if (StickerManager.getInstance().isShopSearchEnabled())
		{
			searchLayout = (RelativeLayout) MenuItemCompat.getActionView(menu.findItem(R.id.shop_search));

			setupSearchFTUE();

			MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.shop_search), new MenuItemCompat.OnActionExpandListener()
			{
				@Override
				public boolean onMenuItemActionExpand(MenuItem item)
				{
					menu.findItem(R.id.shop_settings).setVisible(false);
					stickerShopFragment.showBanner(false);
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item)
				{
					menu.findItem(R.id.shop_settings).setVisible(true);
					stickerShopFragment.showBanner(true);
					return true;
				}
			});
		}

		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.shop_settings:
                try
                {
                    JSONObject metadata = new JSONObject();
                    metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_SETTING_BTN_CLICKED);
                    HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
                }
                catch (JSONException e)
                {
                    Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
                }

                IntentFactory.openStickerSettingsActivity(StickerShopActivity.this);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener()
    {
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            Utils.hideSoftKeyboard(getApplicationContext(), shopSearchMenuItem.getActionView());
            showSearchFragment();
            return stickerShopSearchFragment.onQueryTextSubmit(query);
        }

        @Override
        public boolean onQueryTextChange(String query)
        {
			if (TextUtils.isEmpty(query))
			{
				if (stickerShopSearchFragment != null && stickerShopSearchFragment.isAdded())
				{
					StickerShopActivity.super.onBackPressed();
				}
				return true;
			}

			if (!TextUtils.isEmpty(query.trim()))
			{
				showSearchFragment();
				return stickerShopSearchFragment.onQueryTextChange(query);
			}
            return true;
        }
    };

    @Override
    public void onBackPressed()
    {

        if (isStartedForResult())
        {
            setResult(ChatThread.RESULT_CODE_STICKER_SHOP_ACTIVITY);
        }

        if (shopSearchMenuItem != null && shopSearchMenuItem.isActionViewExpanded())
        {
            shopSearchMenuItem.collapseActionView();

            if(stickerShopSearchFragment!=null && stickerShopSearchFragment.isAdded())
            {
                super.onBackPressed();
            }

            return;
        }

        super.onBackPressed();
    }

	private void setupSearchFTUE()
	{

		int searchFtueShownCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_STICKER_SHOP_SEARCH_FTUE_LIMIT, HikeConstants.DEFAULT_SEARCH_FTUE_LIMIT);
        final ImageView searchIcon = (ImageView) searchLayout.findViewById(R.id.icon);
		if (searchFtueShownCount <= 0)
		{
            searchIcon.setVisibility(View.GONE);
            searchLayout.removeView(searchIcon);
            setupSearchBar();
            return;
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_STICKER_SHOP_SEARCH_FTUE_LIMIT, --searchFtueShownCount);


		searchIcon.setImageResource(R.drawable.ic_top_bar_search);
		searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_STICKER_SHOP_SEARCH_FTUE_LIMIT, 0);
                v.clearAnimation();
                v.setAnimation(null);
                v.setVisibility(View.GONE);
                setupSearchBar();
                if (shopSearchMenuItem != null) {
                    shopSearchMenuItem.expandActionView();
                }
            }
        });

        Animation pulse = HikeAnimationFactory.getStickerShopSearchIconFtueAnimation(this,rippleListener);
        searchIcon.startAnimation(pulse);

	}

	private Animation.AnimationListener rippleListener = new Animation.AnimationListener()
	{
		int count = 1;

		@Override
		public void onAnimationStart(Animation animation)
		{

		}

		@Override
		public void onAnimationEnd(Animation animation)
		{

		}

		@Override
		public void onAnimationRepeat(Animation animation)
		{
			if (count++ % 2 != 0)
			{
				searchLayout.setPressed(true);
				searchLayout.setPressed(false);
			}
		}
	};

    private void setupSearchBar()
    {
        SearchView searchBar = (SearchView) searchLayout.findViewById(R.id.search_bar);
        searchLayout.removeView(searchBar);
        MenuItemCompat.setShowAsAction(MenuItemCompat.setActionView(shopSearchMenuItem, searchBar), MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        searchBar.setOnQueryTextListener(onQueryTextListener);
        searchBar.setQueryHint(getString(R.string.shop_search));
        shopSearchMenuItem.setIcon(R.drawable.ic_top_bar_search);
        searchBar.setVisibility(View.VISIBLE);
    }

	@Override
	protected void onDestroy()
	{
		if (stickerShopSearchFragment != null)
		{
			stickerShopSearchFragment.releaseSearchResources();
		}
		super.onDestroy();
	}
}