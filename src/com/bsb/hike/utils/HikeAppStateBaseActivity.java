package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author Rishabh Using this to notify the server when the app comes to the foreground or background.
 */
public abstract class HikeAppStateBaseActivity extends Activity
{

	private static final String TAG = "HikeAppState";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		HikeAppStateUtils.onCreate(this);
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
		super.onActivityResult(requestCode, resultCode, data);
		HikeAppStateUtils.onActivityResult(this);
	}

}
