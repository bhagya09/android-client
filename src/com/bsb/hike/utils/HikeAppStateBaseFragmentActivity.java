package com.bsb.hike.utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.voip.view.CallIssuesDialogFragment;
import com.bsb.hike.voip.view.CallRateDialogFragment;

public class HikeAppStateBaseFragmentActivity extends SherlockFragmentActivity implements Listener
{

	private static final String TAG = "HikeAppState";
	
	protected static final int PRODUCT_POPUP_HANDLER_WHAT = -99;
	
	protected static final int PRODUCT_POPUP_SHOW_DIALOG=-100;
	
	protected Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			handleUIMessage(msg);
		};
	};

	
	/**
	 * This method is made to be called from handler, do not call this method directly 
	 * Post Message to mHandler to call this method
	 * Subclasses should override this method to perform some UI functionality
	 * <b>(DO NOT FORGET TO CALL super)</b>
	 * @param msg
	 */
	protected void handleUIMessage(Message msg)
	{
		switch(msg.what)
		{
		case PRODUCT_POPUP_HANDLER_WHAT: 
			isThereAnyPopUpForMe(msg.arg1);
			break;
		case PRODUCT_POPUP_SHOW_DIALOG:
			showPopupDialog((ProductContentModel)msg.obj);
			break;
		}
	}
	
	/**
	 * 
	 * @param msg
	 * Shows the Popup on the Activity
	 */
	protected void showPopupDialog(ProductContentModel mmModel)
	{
		if (mmModel != null)
		{
			DialogPojo mmDialogPojo = ProductInfoManager.getInstance().getDialogPojo(mmModel);
			HikeDialogFragment mmFragment = HikeDialogFragment.getInstance(mmDialogPojo);
			
		// If activity is finishing don't commit.
			
			if(!isFinishing())
			mmFragment.showDialog(getSupportFragmentManager());
		}
	}
	
	
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
		if (removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			getSupportActionBar().show();
		}
		else
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

	public void addFragment(Fragment fragment, String tag)
	{
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(fragment, tag);
		fragmentTransaction.commitAllowingStateLoss();
	}

	public void addFragment(int containerView, Fragment fragment, String tag)
	{
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(containerView, fragment, tag);
		fragmentTransaction.commitAllowingStateLoss();
	}

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
	
	public boolean isFragmentAdded(String tag)
	{
		return getSupportFragmentManager().findFragmentByTag(tag) != null;
	}

	private void isThereAnyPopUpForMe(int popUpTriggerPoint)
	{
		ProductInfoManager.getInstance().isThereAnyPopup(popUpTriggerPoint,new IActivityPopup()
		{

			@Override
			public void onSuccess(final ProductContentModel mmModel)
			{
				Message msg = Message.obtain();
				msg.what = PRODUCT_POPUP_SHOW_DIALOG;
				msg.obj = mmModel;
				mHandler.sendMessage(msg);
			}

			@Override
			public void onFailure()
			{
				// No Popup to display
			}
			
		});
	
	}
	
	protected void showProductPopup(int which)
	{
		Message m = Message.obtain();
		m.what = PRODUCT_POPUP_HANDLER_WHAT;
		m.arg1 = which;
		mHandler.sendMessage(m);
	}
}
