package com.bsb.hike.media;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class CaptureImageParser
{
	public interface CaptureImageListener
	{
		public void imageCaptured(Uri uri);

		public void imageCaptured(String imagePath);

		public void imageCaptureFailed();
	}

	private static final String TAG = "parseimage";

	/**
	 * 
	 * @param resultCode
	 *            - returned after camera intent
	 * @param data
	 *            - returned after camera intent
	 * @param listener
	 *            - listener to which give callback
	 */
	public static void parseCameraResult(Context context, int resultCode, Intent data, CaptureImageListener listener)
	{
		Logger.d(TAG, "onactivity result");

		HikeSharedPreferenceUtil sharedPreference = HikeSharedPreferenceUtil.getInstance();
		if (resultCode == Activity.RESULT_OK)
		{
			Logger.d(TAG, "onactivity result ok");
			// this key was saved when we started camera activity
			String capturedFilepath = sharedPreference.getData(HikeMessengerApp.FILE_PATH, null);
			sharedPreference.removeData(HikeMessengerApp.FILE_PATH);

			if (capturedFilepath != null)
			{
				File imageFile = new File(capturedFilepath);

				if (imageFile != null && imageFile.exists())
				{
					/**
					 * Sending broadcast to notify the System Gallery app to refresh itself since a new file has been added to DCIM folder
					 */
					context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + imageFile)));
					showSMODialog(context, imageFile, listener);
				}

			}
			else
			{
				Logger.e(TAG, "captured image path is null");
				listener.imageCaptureFailed();
			}
		}
		else
		{
			// result cancelled
			listener.imageCaptureFailed();
		}
	}
	
	public static void showSMODialog(Context context, final File file, final CaptureImageListener listener)
	{
		HikeDialogFactory.showDialog(context, HikeDialogFactory.SHARE_IMAGE_QUALITY_DIALOG, new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				listener.imageCaptured(file.getAbsolutePath());
				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}
		}, new Long[] { (long) 1, file.length() }).setCanceledOnTouchOutside(false); // 1 since count of images is 1.
	}
}
