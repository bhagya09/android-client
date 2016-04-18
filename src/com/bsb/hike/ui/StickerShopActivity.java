package com.bsb.hike.ui;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.StickerShopFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

public class StickerShopActivity extends HikeAppStateBaseFragmentActivity
{
    private StickerShopFragment stickerShopFragment;

    private StickerShopSearchFragment stickerShopSearchFragment;

    private MenuItem shopSearchMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sticker_shop_parent);
        setupShopFragment(savedInstanceState);
        setupActionBar();
        showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STICKER_SHOP.ordinal());

    }

    private void setupShopFragment(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {
            return;
        }
        stickerShopFragment = StickerShopFragment.newInstance();

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
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(shopSearchMenuItem);
        searchView.setOnQueryTextListener(onQueryTextListener);
        searchView.setQueryHint(getString(R.string.shop_search));
        searchView.clearFocus();
        MenuItemCompat.setShowAsAction(MenuItemCompat.setActionView(shopSearchMenuItem, searchView), MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        
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
                    Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
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
            stickerShopSearchFragment.onQueryTextSubmit(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText)
        {
            return false;
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
        }

        super.onBackPressed();

    }
}
