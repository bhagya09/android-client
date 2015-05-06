package com.bsb.hike.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;

public class DelegateActivity extends HikeAppStateBaseFragmentActivity
{
	public static final String SOURCE_INTENT = "si";

	public static final String DESTINATION_INTENT = "di";

	private Intent sourceIntent;

	private ArrayList<Intent> destinationIntents;

	private Intent resultIntent;

	private boolean startedForResult;

	private final String TAG = DelegateActivity.class.getSimpleName();

	private int requestCode = 11, currentIntentCounter; // We can have a single request code since DelegateActivity has standard launch mode

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		startedForResult = (getCallingActivity() != null);

		currentIntentCounter = 0;

		if (intent != null)
		{
			Parcelable parcel = intent.getParcelableExtra(SOURCE_INTENT);
			if (parcel != null && parcel instanceof Intent)
			{
				sourceIntent = (Intent) parcel;
			}
			else
			{
				Logger.d(TAG, "Source intent not present. Nothing to do!");
				onError();
			}

			ArrayList<Parcelable> parcels = intent.getParcelableArrayListExtra(DESTINATION_INTENT);
			if (parcels != null)
			{
				destinationIntents = new ArrayList<Intent>(parcels.size());
				for (Parcelable destParcel : parcels)
				{
					if (destParcel instanceof Intent)
					{
						destinationIntents.add((Intent) destParcel);
					}
					else
					{
						Logger.d(TAG, "Invalid Destination Intent!");
						onError();
						break;
					}
				}

			}
			else
			{
				Logger.d(TAG, "Destination intents not present. Nothing to do!");
				onError();
			}

		}
		else
		{
			Logger.d(TAG, "Empty Intent. Nothing to do");
			onError();
		}

		if (destinationIntents != null)
		{
			Logger.d(TAG, "Starting activity for result");
			DelegateActivity.this.startActivityForResult(sourceIntent, requestCode);
		}
		else
		{
			Logger.d(TAG, "Starting activity");
			DelegateActivity.this.startActivity(sourceIntent);
			DelegateActivity.this.finish();
		}
	}

	private void onError()
	{
		DelegateActivity.this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK)
		{
			if (requestCode == this.requestCode)
			{
				Intent currentIntent = null, previousIntent = null;
				previousIntent = currentIntentCounter == 0 ? sourceIntent : destinationIntents.get(currentIntentCounter - 1);
				if (startedForResult)
				{
					if (currentIntentCounter < destinationIntents.size())
					{
						currentIntent = destinationIntents.get(currentIntentCounter);
						currentIntentCounter++;
					}
					else
					{
						resultIntent = new Intent();
						currentIntent = resultIntent;
					}
				}
				else 
				{
					if(currentIntentCounter < destinationIntents.size() - 1)
					{
						currentIntent = destinationIntents.get(currentIntentCounter);
						currentIntentCounter++;
					}
					else
					{
						resultIntent = destinationIntents.get(currentIntentCounter);
						currentIntent = resultIntent;
					}
				}
				

				if (data != null)
				{
					currentIntent.setAction(data.getAction());
					currentIntent.setData(data.getData());
					currentIntent.putExtras(data.getExtras());
				}
				else if (previousIntent.getAction() == MediaStore.ACTION_IMAGE_CAPTURE)
				{
					// Special case since camera returns null data
					// Retrieve saved file path
					// Currently on accessing the first file since recursive call not yet involved.
					String newFilePath = currentIntent.getStringExtra(HikeMessengerApp.FILE_PATHS);
					MediaScannerConnection.scanFile(getApplicationContext(), new String[] { newFilePath }, null, null);
					currentIntent.putExtra(HikeMessengerApp.FILE_PATH, newFilePath);
				}

				if (resultIntent == null )
				{
					DelegateActivity.this.startActivityForResult(currentIntent, this.requestCode);
				}
				else if (!startedForResult)
				{
					DelegateActivity.this.startActivity(resultIntent);
					DelegateActivity.this.finish();
				}
				else
				{
					DelegateActivity.this.setResult(RESULT_OK, resultIntent);
					DelegateActivity.this.finish();
				}

			}
		}
		else
		{
			onError();
		}
	}

}
