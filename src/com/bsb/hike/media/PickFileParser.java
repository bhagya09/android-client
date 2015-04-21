package com.bsb.hike.media;

import java.io.File;
import java.net.URI;

import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class PickFileParser
{

	public static interface PickFileListener
	{
		public void pickFileSuccess(int requestCode, String filePath);

		public void pickFileFailed(int requestCode);
	}

	public static void onAudioOrVideoResult(int requestCode, int resultCode, Intent data, PickFileListener listener, Activity activity)
	{
		if (resultCode == Activity.RESULT_OK)
		{
			if (data == null || data.getData() == null)
			{
				listener.pickFileFailed(requestCode);
			}
			else
			{
				String filePath = parseUri(data, activity);
				listener.pickFileSuccess(requestCode, filePath);
			}
		}
		else
		{
			listener.pickFileFailed(requestCode);
		}
	}

	public static String parseUri(Intent data, Activity activity)
	{
		Uri fileURI = data.getData();
		String fileUriStart = "file://";
		String fileUriString = fileURI.toString();
		String filePath = null;
		if (fileUriString.startsWith(fileUriStart))
		{
			/*
			 * Done to fix the issue in a few Sony devices.
			 */
			filePath = new File(URI.create(Utils.replaceUrlSpaces(fileUriString))).getAbsolutePath();
		}
		else
		{
			// content path/uri returned
			filePath = Utils.getRealPathFromUri(fileURI, activity);
		}
		return filePath;
	}
}
