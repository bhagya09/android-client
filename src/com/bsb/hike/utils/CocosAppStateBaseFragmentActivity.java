package com.bsb.hike.utils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;

public class CocosAppStateBaseFragmentActivity extends HikeBaseActivity implements Listener,IHandlerCallback
{

	private static final String TAG = "CocosAppState";
	
	protected static final int PRODUCT_POPUP_HANDLER_WHAT = -99;
	
	protected static final int PRODUCT_POPUP_SHOW_DIALOG=-100;
	
	protected HikeUiHandler uiHandler = new HikeUiHandler (this);
	
	/**
	 * 
	 * @param msg
	 * Shows the Popup on the Activity
	 */
	@Override
	public void showPopupDialog(ProductContentModel mmModel)
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
		super.onCreate(savedInstanceState);

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
		if (removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			getSupportActionBar().show();
		}
		else
		{
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
		super.onPause();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

	@Override
	public void finish()
	{
		super.finish();
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode)
	{
		try
		{
			super.startActivityFromFragment(fragment, intent, requestCode);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Unable to find activity", e);
			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode)
	{
		try
		{
			super.startActivityForResult(intent, requestCode);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Unable to find activity", e);
//			FTAnalyticEvents.logDevException(FTAnalyticEvents.UNABLE_TO_START_ACTIVITY, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Unable to find activity :" , e);
			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void startActivity(Intent intent)
	{
		try
		{
			super.startActivity(intent);	
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Unable to find activity", e);
//			FTAnalyticEvents.logDevException(FTAnalyticEvents.UNABLE_TO_START_ACTIVITY, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Unable to find activity :", e);
			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onEventReceived(String type, final Object object)
	{

	}
	
	protected void openImageViewer(Object object)
	{
		return;
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
	
	public void updateActionBarColor(int backgroundDrawable)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(backgroundDrawable));
		// * Workaround to set actionbar background drawable multiple times. Refer SO.
		// http://stackoverflow.com/questions/17076958/change-actionbar-color-programmatically-more-then-once/17198657#17198657
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
	}
	
	public void updateActionBarColor(Drawable colorDrawable)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable((colorDrawable));
		// * Workaround to set actionbar background drawable multiple times. Refer SO.
		// http://stackoverflow.com/questions/17076958/change-actionbar-color-programmatically-more-then-once/17198657#17198657
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
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
				uiHandler.sendMessage(msg);
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
		uiHandler.sendMessage(m);
	}
	
	@Override
	protected void onDestroy()
	{
		onHandlerDestroy();
		super.onDestroy();
	}

	/**
	 * Removes all runnables and messages from the handler.Override this method to remove only specific
	 *  runnables/messages
	 */
	protected void onHandlerDestroy()
	{
		Logger.d("HikeHandler","Base Activity onDestroy");
		if (uiHandler != null)
		{
			uiHandler.onDestroy();
		}
	}
	/**
	 * This method is made to be called from handler, do not call this method directly 
	 * Post Message to mHandler to callw this method
	 * Subclasses should override this method to perform some UI functionality
	 * <b>(DO NOT FORGET TO CALL super)</b>
	 * @param msg
	 */
	@Override
	public void handleUIMessage(Message msg)
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

}
