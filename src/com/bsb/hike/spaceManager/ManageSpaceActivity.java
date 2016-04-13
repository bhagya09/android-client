package com.bsb.hike.spaceManager;

import android.os.Bundle;

import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;


public class ManageSpaceActivity extends HikeAppStateBaseFragmentActivity
{

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

    }

    private void setupActionBar()
    {

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
    }
}