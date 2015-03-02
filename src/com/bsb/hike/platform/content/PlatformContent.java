package com.bsb.hike.platform.content;


import java.io.File;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class PlatformContent
{

	private static boolean isInitialized;

	private PlatformContent()
	{
		// Classic singleton
	}

	public static enum EventCode
	{
		INVALID_DATA
				{
					@Override
					public String toString()
					{
						return "inv_data";
					}
				},

		LOW_CONNECTIVITY
				{
					@Override
					public String toString()
					{
						return "low_con";
					}
				},

		STORAGE_FULL
				{
					@Override
					public String toString()
					{
						return "st_full";
					}
				},
		UNKNOWN
				{
					@Override
					public String toString()
					{
						return "unknown";
					}
				},
		DOWNLOADING
				{
					@Override
					public String toString()
					{
						return "downloading";
					}
		},
		LOADED
		{
			@Override
			public String toString()
			{
				return "loaded";
			}
		}
	}

	/**
	 * Gets well formed HTML content.
	 * 
	 * @param contentData
	 *            the content data
	 * @param listener
	 *            the listener
	 * @return new request made, use this for cancelling requests
	 * 
	 * @return the content
	 */
	public static PlatformContentRequest getContent(String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		if (!isInitialized)
		{
			PlatformContentConstants.PLATFORM_CONTENT_DIR = HikeMessengerApp.getInstance().getApplicationContext().getFilesDir() + File.separator
						+ PlatformContentConstants.CONTENT_DIR_NAME + File.separator;
			isInitialized = true;
		}

		Logger.d("PlatformContent", "Content Dir : " + PlatformContentConstants.PLATFORM_CONTENT_DIR);

		PlatformContentRequest request = PlatformContentRequest.make(PlatformContentModel.make(contentData), listener);

		if (request != null)
		{
			PlatformContentLoader.getLoader().handleRequest(request);
			return request;
		}
		else
		{
			Logger.e("PlatformContent", "Incorrect content data");
			listener.onEventOccured(EventCode.INVALID_DATA);
			return null;
		}

	}

	public static String getForwardCardData(String contentData)
	{
		return PlatformContentModel.getForwardData(contentData);
	}

	public static void cancelRequest(PlatformContentRequest argRequest)
	{
		PlatformRequestManager.remove(argRequest);
	}

	public static void cancelAllRequests()
	{
		PlatformRequestManager.removeAll();
	}

}
