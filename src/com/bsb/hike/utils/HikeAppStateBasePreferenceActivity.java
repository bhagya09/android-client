package com.bsb.hike.utils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.ui.utils.StatusBarColorChanger;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;

public abstract class HikeAppStateBasePreferenceActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		HikeAppStateUtils.onCreate(this);
		super.onCreate(savedInstanceState);
		setStatusBarColor(getWindow(), HikeConstants.STATUS_BAR_BLUE);
	}

	@Override
	protected void onResume()
	{
		HikeAppStateUtils.onResume(this);
		super.onResume();
	}

	@Override
	protected void onStart()
	{
		HikeAppStateUtils.onStart(this);
		super.onStart();
	}

	@Override
	protected void onRestart()
	{
		HikeAppStateUtils.onRestart(this);
		super.onRestart();
	}

	@Override
	public void onBackPressed()
	{
		HikeAppStateUtils.onBackPressed();
		super.onBackPressed();
	}

	@Override
	protected void onPause()
	{
		HikeAppStateUtils.onPause(this);
		super.onPause();
	}

	@Override
	protected void onStop()
	{
		HikeAppStateUtils.onStop(this);
		super.onStop();
	}

	@Override
	public void finish()
	{
		HikeAppStateUtils.finish();
		super.finish();
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode)
	{
		HikeAppStateUtils.startActivityForResult(this);
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		HikeAppStateUtils.onActivityResult(this);
		super.onActivityResult(requestCode, resultCode, data);
	}
	protected void setStatusBarColor(Window window,String color){
		StatusBarColorChanger.setStatusBarColor(window, color);
	}
}	
