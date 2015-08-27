package com.bsb.hike.voip.view;

import android.os.Bundle;
import android.view.WindowManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.voip.VoIPConstants;

public class CallRateActivity extends HikeAppStateBaseFragmentActivity implements IVoipCallRateListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.call_rate_activity);
		Bundle bundle = getIntent().getBundleExtra(VoIPConstants.CALL_RATE_BUNDLE);
		showCallRateFragment(bundle);
	}

	private void showCallRateFragment(final Bundle bundle) 
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG) && !isFragmentAdded(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG))
		{
			CallRateDialogFragment callRatePopup = new CallRateDialogFragment();
			callRatePopup.setArguments(bundle);

			addFragment(callRatePopup, HikeConstants.VOIP_CALL_RATE_FRAGMENT_TAG);
		}
	}

	@Override
	public void showCallIssuesFragment(final Bundle bundle)
	{
		if(!isFragmentAdded(HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG))
		{
			CallIssuesDialogFragment callIssuesPopup = new CallIssuesDialogFragment();
			callIssuesPopup.setArguments(bundle);

			addFragment(callIssuesPopup, HikeConstants.VOIP_CALL_ISSUES_FRAGMENT_TAG);
		}
	}
}
