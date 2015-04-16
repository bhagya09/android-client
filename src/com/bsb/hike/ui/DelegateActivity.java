package com.bsb.hike.ui;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DelegateActivity extends Activity
{
	public static final String SOURCE_INTENT = "si";

	public static final String DESTINATION_INTENT = "di";

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
		}
		else
		{
			Logger.d(TAG, "Empty Intent. Nothing to do");
			onError();
		}

		

		if (destinationIntent != null)
		{
			if (!IntentManager.isIntentAvailable(getApplicationContext(), sourceIntent))
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
					/*
					 * For images, save the file path as a preferences since in some devices the reference to the file becomes null.
					 */
					HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SERVICE);

					// Retrieve saved file path
					String newFilePath = pref.getData(HikeMessengerApp.FILE_PATH, "");

					destinationIntent.putExtra(HikeMessengerApp.FILE_PATH, newFilePath);

					HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
					{
						@Override
						public void run()
						{
							// Remove saved file path from shared pref
							HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SERVICE).removeData(HikeMessengerApp.FILE_PATH);
						}
					}, 0);
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
