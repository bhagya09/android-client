package com.bsb.hike.utils;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.bsb.hike.R;

public abstract class HikeAppStateBasePreferenceActivity extends SherlockPreferenceActivity
{

	private static final String TAG = "HikeAppState";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		HikeAppStateUtils.onCreate(this);
		overridePendingTransition(R.anim.appear, R.anim.pop_top);
		super.onCreate(savedInstanceState);
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
		overridePendingTransition(R.anim.pump_top, R.anim.disappear);
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
}
