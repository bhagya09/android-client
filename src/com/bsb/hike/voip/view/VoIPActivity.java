package com.bsb.hike.voip.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.view.CallFailedFragment.CallFailedFragListener;
import com.bsb.hike.voip.view.VoipCallFragment.CallFragmentListener;

public class VoIPActivity extends HikeAppStateBaseFragmentActivity implements CallFragmentListener, CallFailedFragListener
{
	private VoipCallFragment mainFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voip_activity);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setupMainFragment();
		Intent intent = getIntent();
		if (intent != null) 
		{
			mainFragment.handleIntent(intent);
		}
	}

	private void setupMainFragment()
	{
		if(mainFragment!=null)
		{
			return;
		}
		mainFragment = new VoipCallFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.parent_layout, mainFragment).commit();
	}

	@Override
	protected void onNewIntent(Intent intent) 
	{
		super.onNewIntent(intent);
		Logger.d(VoIPConstants.TAG, "VoIPActivity onNewIntent().");
		if(mainFragment instanceof VoipCallFragment)
		{
			mainFragment.handleIntent(intent);
		}

		if(intent.getBooleanExtra(VoIPConstants.Extras.INCOMING_CALL, false))
		{
			removeCallFailedFragment();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(mainFragment!=null && mainFragment instanceof VoipCallFragment)
		{
			if(mainFragment.onKeyDown(keyCode, event))
			{
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void showCallFailedFragment(Bundle bundle)
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_FAILED_FRAGMENT_TAG))
		{
			CallFailedFragment callFailedFragment = new CallFailedFragment();
			callFailedFragment.setArguments(bundle);

			addFragment(R.id.parent_layout, callFailedFragment, HikeConstants.VOIP_CALL_FAILED_FRAGMENT_TAG);

			// Using this method to ensure fragment commit happens immediately
			getSupportFragmentManager().executePendingTransactions();
		}
	}

	@Override
	public void removeCallFailedFragment()
	{
		removeFragment(HikeConstants.VOIP_CALL_FAILED_FRAGMENT_TAG);
	}

	@Override
	public boolean isShowingCallFailedFragment() 
	{
		return isFragmentAdded(HikeConstants.VOIP_CALL_FAILED_FRAGMENT_TAG);
	}

	public void showDeclineWithMessageFragment(Bundle bundle)
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_DECLINE_MESSAGE_FRAGMENT_TAG))
		{
			CallDeclineWithMessageFragment fragment = new CallDeclineWithMessageFragment();
			fragment.setArguments(bundle);
			fragment.show(getSupportFragmentManager(), HikeConstants.VOIP_CALL_DECLINE_MESSAGE_FRAGMENT_TAG);
		}
	}

	@Override
	protected void onStop() 
	{
		super.onStop();
		removeCallFailedFragment();
	}
}
