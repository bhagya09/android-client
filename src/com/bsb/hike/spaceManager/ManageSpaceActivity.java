package com.bsb.hike.spaceManager;

import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.spaceManager.models.SpaceManagerItem;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;

import java.util.List;


public class ManageSpaceActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.UiListener
{
    private static final String TAG = "ManageSpaceActivity";
    private List<SpaceManagerItem> spaceManagerItems;

    private String[] uiPubSubTypes = {HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS, HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.space_manager);
        setupActionBar();
        init();
    }

    private void init()
    {
        HikeMessengerApp.getPubSub().addUiListener(this, uiPubSubTypes);
        fetchItems();
    }

    private void fetchItems()
    {
        Intent fetchItemsIntent = new Intent(this, SpaceManagerIntentService.class);
        fetchItemsIntent.setAction(SpaceManagerIntentService.ACTION_FETCH_SPACE_MANAGER_ITEMS);
        this.startService(fetchItemsIntent);
    }

    private void setupActionBar()
    {

    }

    @Override
    public void onUiEventReceived(String type, Object object)
    {
        Logger.d(TAG, "received pubsub - " + type);
        if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS))
        {
            if(object != null && object instanceof List)
            {
                Logger.d(TAG, "successfully fetched space manager items");
                spaceManagerItems = (List<SpaceManagerItem>) object;
                Logger.d(TAG, "items list: " + spaceManagerItems.toString());
            }
            else
            {
                Logger.d(TAG, "received invalid list or items");
            }
        }
        else if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL))
        {
            Logger.d(TAG, "failure in fetchin space manager items");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        HikeMessengerApp.getPubSub().removeUiListener(this, uiPubSubTypes);
    }
}