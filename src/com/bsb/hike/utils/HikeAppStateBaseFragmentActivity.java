package com.bsb.hike.utils;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import com.bsb.hike.analytics.RecordActivityOpenTime;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;

public class HikeAppStateBaseFragmentActivity extends HikeBaseActivity implements Listener,IHandlerCallback
{

	private static final String TAG = "HikeAppState";
	
	protected static final int PRODUCT_POPUP_HANDLER_WHAT = -99;
	
	protected static final int PRODUCT_POPUP_SHOW_DIALOG=-100;
	
	protected HikeUiHandler uiHandler = new HikeUiHandler (this);
	
	private boolean isActivityVisible = false;

	private RecordActivityOpenTime recordActivityOpenTime =null;
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
			// clearing the notification once the popup is been seen

			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager != null)
				notificationManager.cancel(HikeNotification.NOTIFICATION_PRODUCT_POPUP);
			DialogPojo mmDialogPojo = ProductInfoManager.getInstance().getDialogPojo(mmModel);
			HikeDialogFragment mmFragment = HikeDialogFragment.getInstance(mmDialogPojo);

			// If activity is finishing don't commit.

			if (!isFinishing())
				mmFragment.showDialog(getSupportFragmentManager());
		}
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		startRecordTime();
		HikeAppStateUtils.onCreate(this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.DB_CORRUPT, this);
		super.onCreate(savedInstanceState);
	}

	private void startRecordTime()
	{
		recordActivityOpenTime =new RecordActivityOpenTime(this.getClass().getSimpleName());
		if(recordActivityOpenTime.shouldStart()) {
			recordActivityOpenTime.startRecording();
		}
		else
		{
			recordActivityOpenTime=null;
		}
	}


	@Override
	protected void onResume()
	{
		isActivityVisible = true;
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
		isActivityVisible = false;
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
		HikeAppStateUtils.startActivityForResult(this);
		try
		{
			super.startActivityForResult(intent, requestCode);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Unable to find activity", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UNABLE_TO_START_ACTIVITY, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Unable to find activity :" , e);
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
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UNABLE_TO_START_ACTIVITY, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Unable to find activity :", e);
			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}		
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
					openImageViewer(object);
				}
			});
		}

		else if (HikePubSub.DB_CORRUPT.equals(type))
		{
			if (amIHomeActivity())
			{
				return;
			}

			else
			{
				Intent intent = IntentFactory.getHomeActivityIntent(HikeAppStateBaseFragmentActivity.this);
				startActivity(intent);
				this.finish();
			}
		}
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
		HikeMessengerApp.getInstance().getPubSub().removeListener(HikePubSub.DB_CORRUPT, this);
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
	 * Post Message to mHandler to call this method
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
	
	protected boolean isActivityVisible()
	{
		return isActivityVisible;
	}

	protected void recordActivityEndTime()
	{
		if(recordActivityOpenTime ==null)
		{
			return;
		}
		recordActivityOpenTime.stopRecording();
		recordActivityOpenTime.dumpAnalytics();
		recordActivityOpenTime.onDestroy();
		recordActivityOpenTime = null;
	}
	private boolean amIHomeActivity()
	{
		return (HikeAppStateBaseFragmentActivity.this instanceof HomeActivity);
	}

}
