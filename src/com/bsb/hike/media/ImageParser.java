package com.bsb.hike.media;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ImageParser
{
	public interface ImageParserListener
	{
		public void imageParsed(Uri uri);

		public void imageParsed(String imagePath);

		public void imageParseFailed();
	}

	private static final String TAG = "parseimage";

	/**
	 * 
	 * @param resultCode
	 *            - returned after image intent
	 * @param data
	 *            - returned after image intent
	 * @param listener
	 *            - listener to which give callback
	 */
	public static void parseResult(Context context, int resultCode, Intent data, ImageParserListener listener)
	{
		Logger.d(TAG, "onactivity result");

		if (resultCode == Activity.RESULT_OK)
		{
			Logger.d(TAG, "onactivity result ok");
			// this key was saved when we started camera activity
			
			String capturedFilepath = null;

			if (data != null && data.getAction() == HikeConstants.HikePhotos.PHOTOS_ACTION_CODE)
			{
				capturedFilepath = data.getStringExtra(HikeConstants.Extras.IMAGE_PATH);
			}
			else
			{
				// After checking for custom action codes, we try to fetch result from camera
				capturedFilepath = Utils.getCameraResultFile();
			}

			if (capturedFilepath != null)
			{
				File imageFile = new File(capturedFilepath);

				if (imageFile != null && imageFile.exists())
				{
					showSMODialog(context, imageFile, listener);
				}

			}
			else
			{
				Logger.e(TAG, "Image path is null");
				listener.imageParseFailed();
			}
		}
		else
		{
			// result cancelled
			listener.imageParseFailed();
		}
	}
	
	public static void showSMODialog(Context context, final File file, final ImageParserListener listener)
	{
		HikeDialogFactory.showDialog(context, HikeDialogFactory.SHARE_IMAGE_QUALITY_DIALOG, new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				listener.imageParsed(file.getAbsolutePath());
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