package com.bsb.hike.voip.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.view.CallFailedFragment.CallFailedFragListener;
import com.bsb.hike.voip.view.VoipCallFragment.CallFragmentListener;

/**
 * This activity does not extend <b>HikeAppStateBaseFragmentActivity</b>.<br/>
 * <p>
 * We do not want to send fg/bg packets from here because it leads to the problem that
 * even a missed call will mark the recipient as online.
 * </p>  
 * 
 * @author anuj
 *
 */
public class VoIPActivity extends AppCompatActivity implements CallFragmentListener, CallFailedFragListener
{
	private VoipCallFragment mainFragment;
	@SuppressWarnings("unused")
	private String tag = VoIPConstants.TAG + " Activity";

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voip_activity);

		setupMainFragment();
		StatusBarColorChanger.setStatusBarColor(getWindow(), HikeConstants.STATUS_BAR_BLUE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
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
		
		if (intent.hasExtra(VoIPConstants.Extras.REMOVE_FAILED_FRAGMENT) && isShowingCallFailedFragment())
			removeCallFailedFragment();

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
			clearActivityFlags();
			
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

	/**
	 * Duplicate method from HikeAppStateBaseFragmentActivity
	 * @param tag
	 * @return
	 */
	public boolean isFragmentAdded(String tag)
	{
		return getSupportFragmentManager().findFragmentByTag(tag) != null;
	}
	
	/**
	 * Duplicate method from HikeAppStateBaseFragmentActivity
	 * @param containerView
	 * @param fragment
	 * @param tag
	 */
	public void addFragment(int containerView, Fragment fragment, String tag)
	{
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(containerView, fragment, tag);
		fragmentTransaction.commitAllowingStateLoss();
	}

	/**
	 * Duplicate method from HikeAppStateBaseFragmentActivity
	 * @param tag
	 * @return
	 */
	public boolean removeFragment(String tag)
	{
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);

		if (fragment != null)
		{	
			fragmentTransaction.remove(fragment);
			fragmentTransaction.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void clearActivityFlags() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
