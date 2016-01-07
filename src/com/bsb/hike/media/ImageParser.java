package com.bsb.hike.media;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.ui.GalleryActivity;
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
	public static void parseResult(Context context, int resultCode, Intent data, ImageParserListener listener, boolean showSMO)
	{
		Logger.d(TAG, "onactivity result");

		if (resultCode == Activity.RESULT_OK)
		{
			Logger.d(TAG, "onactivity result ok");
			// this key was saved when we started camera activity
			
			String capturedFilepath = null;

			//TODO Make a single constant for all these
			if (data != null && data.getAction() == HikeConstants.HikePhotos.PHOTOS_ACTION_CODE)
			{
				capturedFilepath = data.getStringExtra(HikeConstants.Extras.IMAGE_PATH);
			}
			else if (data != null && data.getAction() == GalleryActivity.GALLERY_RESULT_ACTION)
			{
				capturedFilepath = data.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);
				if(data != null && data.hasExtra(HikeCropActivity.CROPPED_IMAGE_PATH))
				{
					capturedFilepath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);
				}
			}
			else if(data != null && data.hasExtra(HikeConstants.HikePhotos.ORIG_FILE))
			{
				capturedFilepath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
			}
			else if(data != null && data.hasExtra(HikeConstants.IMAGE_PATHS))
			{
				ArrayList<Uri> filePathArray = data.getParcelableArrayListExtra(HikeConstants.IMAGE_PATHS);
				if(filePathArray == null || filePathArray.isEmpty())
				{
					listener.imageParseFailed();
					return;
				}

				capturedFilepath = filePathArray.get(0).getPath();
			}
			else if(data != null && data.hasExtra(HikeCropActivity.CROPPED_IMAGE_PATH))
			{
				capturedFilepath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);
			}
			else
			{
				// After checking for custom action codes, we try to fetch result from camera
				capturedFilepath = Utils.getCameraResultFile();
			}
			
			if (capturedFilepath != null)
			{
				final File imageFile = new File(capturedFilepath);

				if (imageFile != null && imageFile.exists())
				{
					if (showSMO)
					{
						showSMODialog(context, imageFile, listener);
					}
					else
					{
						listener.imageParsed(imageFile.getAbsolutePath());
					}
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
