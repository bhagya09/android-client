package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;



/**
 * The activity handles multiple activity intents.
 * Starts the Activities provided in the intents array sequentially  and if started for result returns the result of the final activity to the calling activity.
 * Use this when you want to start activity A and then use its result to start B and use its result to start C and so on...
 * 
 * If started for for result it will the result of the last activity < C in the explanation > to its calling activity otherwise will finish itself on starting the last activity <C>
 * 
 * Array of destination intents is must for this activity otherwise it will exit with Result code RESULT_CANCELED
 *  
 */
public class DelegateActivity extends HikeAppStateBaseFragmentActivity
{
	public static final String DESTINATION_INTENT = "di";

	private ArrayList<Intent> destinationIntents;

	private boolean startedForResult;

	private final String TAG = DelegateActivity.class.getSimpleName();

	private int requestCode = 11; // We can have a single request code since DelegateActivity has standard launch mode

	private int currentIntentCounter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		startedForResult = (getCallingActivity() != null);

		currentIntentCounter = 0;

		if (intent == null || !intent.hasExtra(DelegateActivity.DESTINATION_INTENT))
		{
			Logger.d(TAG, "No Intent to handle.");
			onError();
			return;
		}

		ArrayList<Parcelable> parcels = intent.getParcelableArrayListExtra(DESTINATION_INTENT);
		if (parcels == null)
		{
			Logger.d(TAG, "Destination intents not present. Nothing to do!");
			onError();
			return;
		}
		destinationIntents = new ArrayList<Intent>(parcels.size());
		for (Parcelable destParcel : parcels)
		{
			if (!(destParcel instanceof Intent))
			{
				Logger.d(TAG, "Invalid Destination Intent!");
				onError();
				break;
			}

			destinationIntents.add((Intent) destParcel);
		}

		if (startedForResult)
		{
			Logger.d(TAG, "Starting activity for result");
			DelegateActivity.this.startActivityForResult(destinationIntents.get(currentIntentCounter++), requestCode);

		}
		else
		{
			Logger.d(TAG, "Starting activity");
			DelegateActivity.this.startActivity(destinationIntents.get(currentIntentCounter++));
			DelegateActivity.this.finish();
		}
	}

	private void onError()
	{
		setResult(RESULT_CANCELED);
		DelegateActivity.this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK)
		{
			//Delegate activity should not handle error states.The activity starting it should handle it.
			Logger.d(TAG, "Invalid RESULT CODE = " + resultCode);
			onError();
		}

		if (requestCode != this.requestCode)
		{
			// Adding Check since delegate activity should receive only its request code
			Logger.d(TAG, "Invalid REQUEST CODE = " + requestCode);
			onError();
		}

		Intent currentIntent = null;

		boolean receivedFromCamera = destinationIntents.get(currentIntentCounter - 1).getAction() == MediaStore.ACTION_IMAGE_CAPTURE;// Special case since camera returns null data
		boolean isLastIntent = false;
		
		/**
		 * Will determine the next intent to be picked from the intents array provided to be launched
		 * If the next Intent is not the last intent of the destinations array it starts the next intent for result
		 * If DelegateActivity was not started for result and its the last intent from the array, it opens the last intent and exits.
		 * If DelegateActivity was started for result and its its the last intent from the array, it launches the last activity for result and returns its result to the activity that started DelegateActivity
		 * 
		 */
		
		if (startedForResult)
		{
			if (currentIntentCounter < destinationIntents.size())
			{
				currentIntent = destinationIntents.get(currentIntentCounter++);
			}
			else
			{
				currentIntent = new Intent();
				isLastIntent = true;
			}
		}
		else
		{
			currentIntent = destinationIntents.get(currentIntentCounter++);
			if (currentIntentCounter == destinationIntents.size())
			{
				isLastIntent = true;
			}
		}

		
		if (receivedFromCamera)
		{
			// Special case since camera returns null data
			// Retrieve saved file path
			// Currently on accessing the first file since recursive call not yet involved.
			String newFilePath = currentIntent.getStringExtra(HikeMessengerApp.FILE_PATHS);
			MediaScannerConnection.scanFile(getApplicationContext(), new String[] { newFilePath }, null, null);
			currentIntent.putExtra(HikeMessengerApp.FILE_PATH, newFilePath);
		}
		else if (data != null)
		{
			//transfers data received to the next intent
			currentIntent.setAction(data.getAction());
			currentIntent.setData(data.getData());
			currentIntent.putExtras(data.getExtras());
		}

		if (!isLastIntent)
		{
			DelegateActivity.this.startActivityForResult(currentIntent, this.requestCode);
		}
		else if (!startedForResult)
		{
			DelegateActivity.this.startActivity(currentIntent);
			DelegateActivity.this.finish();
		}
		else
		{
			DelegateActivity.this.setResult(RESULT_OK, currentIntent);
			DelegateActivity.this.finish();
		}

	}

}
