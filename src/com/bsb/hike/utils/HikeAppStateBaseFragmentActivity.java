package com.bsb.hike.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.voip.view.CallIssuesPopup;
import com.bsb.hike.voip.view.CallRatePopup;
import com.bsb.hike.voip.view.IVoipCallListener;

public class HikeAppStateBaseFragmentActivity extends SherlockFragmentActivity implements Listener, IVoipCallListener
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
		HikeAlarmManager.cancelAlarm(HikeAppStateBaseFragmentActivity.this, HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION);
		super.onResume();
	}

	@Override
	protected void onStart()
	{
		HikeAppStateUtils.onStart(this);
		super.onStart();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SHOW_IMAGE, this);
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
		if (!removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			HikeAppStateUtils.onBackPressed();
			super.onBackPressed();
		}
	}

	protected void onSaveInstanceState(Bundle outState)
	{
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
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
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	public void finish()
	{
		HikeAppStateUtils.finish();
		super.finish();
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode)
	{
		HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY;
		super.startActivityFromFragment(fragment, intent, requestCode);
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

	@Override
	public void onEventReceived(String type, final Object object)
	{
		if (HikePubSub.SHOW_IMAGE.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					/*
					 * Making sure we don't add the fragment if the activity is finishing.
					 */
					if (isFinishing())
					{
						return;
					}

					Bundle arguments = (Bundle) object;

					ImageViewerFragment imageViewerFragment = new ImageViewerFragment();
					imageViewerFragment.setArguments(arguments);

					FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
					fragmentTransaction.add(R.id.parent_layout, imageViewerFragment, HikeConstants.IMAGE_FRAGMENT_TAG);
					fragmentTransaction.commitAllowingStateLoss();
				}
			});
		}
	}
	
	public boolean removeFragment(String tag)
	{
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);

		if (fragment != null)
		{	
			fragmentTransaction.remove(fragment);
			fragmentTransaction.commitAllowingStateLoss();
			getSupportActionBar().show();
			return true;
		}
		return false;
	}
	
	public boolean isFragmentAdded(String tag)
	{
		return getSupportFragmentManager().findFragmentByTag(tag) != null;
	}

	@Override
	public void onVoipCallEnd(final Bundle bundle, final String tag) 
	{
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				boolean isCallRateFragShowing = isFragmentAdded(HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG);
				boolean isCallIssuesFragShowing = isFragmentAdded(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG);
				if(tag.equals(HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG) && !isCallRateFragShowing && !isCallIssuesFragShowing)
				{
					CallRatePopup callRatePopup = new CallRatePopup();
					callRatePopup.setArguments(bundle);

					FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
					fragmentTransaction.add(callRatePopup, HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG);
					fragmentTransaction.commitAllowingStateLoss();
				}
				else if(tag.equals(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG) && !isCallIssuesFragShowing)
				{
					CallIssuesPopup callIssuesPopup = new CallIssuesPopup();
					callIssuesPopup.setArguments(bundle);

					FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
					fragmentTransaction.add(callIssuesPopup, HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG);
					fragmentTransaction.commitAllowingStateLoss();
				}
			}
		});
	}
}
