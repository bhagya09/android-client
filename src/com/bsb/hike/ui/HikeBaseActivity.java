package com.bsb.hike.ui;

import java.util.ArrayList;

import android.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.Logger;

/**
 * The activity handles multiple activity intents. Starts the Activities provided in the intents array sequentially and if started for result returns the result of the final
 * activity to the calling activity. Use this when you want to start activity A and then use its result to start B and use its result to start C and so on...
 * 
 * If started for for result it will the result of the last activity < C in the explanation > to its calling activity otherwise will finish itself on starting the last activity <C>
 * 
 * Array of destination intents is must for this activity otherwise it will exit with Result code RESULT_CANCELED
 */
public abstract class HikeBaseActivity extends AppCompatActivity
{
	public static final String DESTINATION_INTENT = "di";

	public static final int DELEGATION_REQUEST_CODE = 2305;

	private ArrayList<Intent> destinationIntents;

	private final String TAG = HikeBaseActivity.class.getSimpleName();
	public int statusBarColorValue;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		statusBarColorValue=getResources().getColor(com.bsb.hike.R.color.blue_hike_status_bar_m);
		setStatusBarColor(getWindow(), HikeConstants.STATUS_BAR_BLUE);
		if (!intent.hasExtra(HikeBaseActivity.DESTINATION_INTENT))
		{
			Logger.d(TAG, "Destination intents not present. Nothing to do!");
			return;
		}

		Logger.d(TAG, "Found Destination intents!");

		ArrayList<Parcelable> parcels = intent.getParcelableArrayListExtra(DESTINATION_INTENT);
		if (parcels == null)
		{
			Logger.d(TAG, "Destination intents not present. Nothing to do!");
			return;
		}

		Logger.d(TAG, "Destination intents are parced!");

		destinationIntents = new ArrayList<Intent>(parcels.size());
		for (Parcelable destParcel : parcels)
		{
			if (!(destParcel instanceof Intent))
			{
				Logger.d(TAG, "Invalid Destination Intent!");
				destinationIntents = null;
				return;
			}

			destinationIntents.add((Intent) destParcel);
		}

		Logger.d(TAG, "Destination intents counts = " + destinationIntents.size());

	}

	/**
	 * 
	 * @return wether this activity was started for result or not
	 */
	protected boolean isStartedForResult()
	{
		return getCallingActivity() != null;
	}

	protected ArrayList<Intent> getDestinationIntents()
	{
		return destinationIntents;
	}

	protected boolean hasDelegateActivities()
	{
		return (destinationIntents != null && destinationIntents.size() > 0);
	}

	/**
	 * A new array list of intents is created removing the intent of launching the current delegate activity(TOP = 0)
	 * 
	 * @param extras
	 *            to be inserted into the extras of the intent of the next Delegate activities to be launched
	 * @return Intent which can be used to fire the next delegate activity
	 */
	private Intent getDelegateIntent(Bundle extras)
	{
		if (!hasDelegateActivities())
		{
			return null;
		}

		// transfers data received to the next intent
		Intent delegateIntent = destinationIntents.get(0);

		if (extras != null)
		{
			delegateIntent.putExtras(extras);
		}

		ArrayList<Intent> nextIntents = new ArrayList<Intent>(destinationIntents);
		nextIntents.remove(0);

		delegateIntent.putParcelableArrayListExtra(HikeBaseActivity.DESTINATION_INTENT, nextIntents);

		return delegateIntent;

	}

	/**
	 * Fires the next delegate activity intent
	 */
	protected void launchNextDelegateActivity()
	{
		if (!hasDelegateActivities())
		{
			return;
		}

		if (isStartedForResult())
		{
			startActivityForResult(getDelegateIntent(null), DELEGATION_REQUEST_CODE);
		}
		else
		{
			startActivity(getDelegateIntent(null));
		}
	}

	/**
	 * Fires the next delegate activity intent adding provided extras into the intent
	 * 
	 * @param putExtras
	 */
	protected void launchNextDelegateActivity(Bundle putExtras)
	{
		if (!hasDelegateActivities())
		{
			return;
		}

		if (isStartedForResult())
		{
			startActivityForResult(getDelegateIntent(putExtras), DELEGATION_REQUEST_CODE);
		}
		else
		{
			startActivity(getDelegateIntent(putExtras));
		}
	}

	/**
	 * onActivityResult if the result is from a delegate activity its handled over here. If started for result it returns the result to the calling activity Children can classes
	 * can overide this method if they wish to process the delegate result differently
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		Logger.d(TAG, "ON activity result Destination intents!");

		if (resultCode == RESULT_OK && requestCode == DELEGATION_REQUEST_CODE && isStartedForResult())
		{
			setResult(RESULT_OK, data);
			finish();
		}
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		ActionBar actionBar=getSupportActionBar();
		if(actionBar!=null)
			actionBar.setDisplayHomeAsUpEnabled(true);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Providing a genetic back pressed framework. 
	 * OnBackPressed will be called on pressing the back arrow key in actionbar.
	 * If any activity wants a different implementation, intercept the click by overriding this method
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(item.getItemId()==R.id.home)
		{
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	protected void setStatusBarColor(Window window,String color){
		StatusBarColorChanger.setStatusBarColor(window, color);
	}

	public void showPopupDialog(ProductContentModel mmModel)
	{
		//Do Nothing
	}
}
