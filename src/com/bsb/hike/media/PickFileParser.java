package com.bsb.hike.media;

import android.app.Activity;
import android.content.Intent;

import com.bsb.hike.utils.Utils;

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
				String filePath = Utils.getAbsolutePathFromUri(data.getData(), activity, true);
				listener.pickFileSuccess(requestCode, filePath);
			}
		}
		else
		{
			listener.pickFileFailed(requestCode);
		}
	}

}
