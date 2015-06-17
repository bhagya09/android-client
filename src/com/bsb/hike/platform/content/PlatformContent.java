package com.bsb.hike.platform.content;


import java.io.File;

import android.os.Environment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class PlatformContent
{

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
				},
		ALREADY_DOWNLOADED
				{
					@Override
					public String toString()
					{
						return "already_dwnld";
					}
				},
		UNZIP_FAILED
				{
					@Override
					public String toString()
					{
						return "unzip_fail";
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
		return getContent(0, contentData, listener);
	}
	
	/**
	 * 
	 * @param uniqueId - the id which you will get back once templating is finished :  {@link PlatformContentModel#getUniqueId()}
	 * @param contentData
	 * @param listener
	 * @return
	 */
	public static PlatformContentRequest getContent(int uniqueId, String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		Logger.d("PlatformContent", "Content Dir : " + PlatformContentConstants.PLATFORM_CONTENT_DIR);
		PlatformContentModel model = PlatformContentModel.make(uniqueId,contentData);
		if(model != null) {
			model.setUniqueId(uniqueId); // GSON issue
		}
		PlatformContentRequest request = PlatformContentRequest.make(model, listener);

		if (request != null)
		{
			PlatformContentLoader.getLoader().handleRequest(request);
			return request;
		}
		else
		{
			Logger.e("PlatformContent", "Incorrect content data");
			listener.onEventOccured(0,EventCode.INVALID_DATA);
			return null;
		}
	}

	public static void init(boolean isProduction)
	{
		PlatformContentConstants.PLATFORM_CONTENT_DIR = isProduction ? HikeMessengerApp.getInstance().getApplicationContext().getFilesDir() + File.separator + PlatformContentConstants.CONTENT_DIR_NAME + File.separator:
				Environment.getExternalStorageDirectory() + File.separator + PlatformContentConstants.HIKE_DIR_NAME + File.separator + PlatformContentConstants.CONTENT_DIR_NAME + File.separator ;
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
