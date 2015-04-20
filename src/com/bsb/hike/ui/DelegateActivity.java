package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

public class DelegateActivity extends Activity
{
	public static final String SOURCE_INTENT = "si";

	public static final String DESTINATION_INTENT = "di";
	
	private String fileDestinations[];

	private Intent sourceIntent;

	private Intent destinationIntent;

	private final String TAG = DelegateActivity.class.getSimpleName();

	private int requestCode = 11; // We can have a single request code since DelegateActivity has standard launch mode

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null)
		{
			Parcelable parcel = intent.getParcelableExtra(SOURCE_INTENT);
			if (parcel != null && parcel instanceof Intent)
			{
				sourceIntent = (Intent) parcel;
			}
			else
			{
				Logger.d(TAG, "Source intent not present. Nothing to do");
				onError();
			}

			parcel = intent.getParcelableExtra(DESTINATION_INTENT);
			if (parcel != null && parcel instanceof Intent)
			{
				destinationIntent = (Intent) parcel;
			}
			
			if(intent.hasExtra(HikeMessengerApp.FILE_PATHS))
			{
				fileDestinations = intent.getStringArrayExtra(HikeMessengerApp.FILE_PATHS);
			}
		}
		else
		{
			Logger.d(TAG, "Empty Intent. Nothing to do");
			onError();
		}

		

		if (destinationIntent != null)
		{
			if(!IntentFactory.isIntentAvailable(getApplicationContext(), sourceIntent))
			{
				onError();
				return;
			}
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
				if (sourceIntent.getAction() == MediaStore.ACTION_IMAGE_CAPTURE)
				{

					// Retrieve saved file path
					// Currently on accessing the first file since recursive call not yet involved.
					String newFilePath = fileDestinations[0];

					destinationIntent.putExtra(HikeMessengerApp.FILE_PATH, newFilePath);
				}

				if (data != null)
				{
					destinationIntent.putExtras(data);
				}
				DelegateActivity.this.startActivity(destinationIntent);
				DelegateActivity.this.finish();
			}
		}
		else
		{
			onError();
		}
	}

}
