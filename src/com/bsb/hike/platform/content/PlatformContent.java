package com.bsb.hike.platform.content;


import android.os.Environment;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.platform.ContentModules.*;
import com.bsb.hike.platform.PlatformContentListener;
import com.bsb.hike.platform.PlatformContentLoader;
import com.bsb.hike.platform.PlatformContentRequest;
import com.bsb.hike.utils.Logger;

import java.io.File;

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
						return "low_con"+errorCode;
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
				},
		INCOMPLETE_ZIP_DOWNLOAD
		        {
                    @Override
                    public String toString()
                    {
                        return "incomplete_zip_download";
                    }
		        },
		EMPTY_URL
		        {
                    @Override
                    public String toString()
                    {
                        return "empty_url";
                    }
		        },
		ZERO_BYTE_ZIP_DOWNLOAD
		        {
                    @Override
                    public String toString()
                    {
                        return "zero_byte_zip_download";
                    }
		        };
		int errorCode;
		public void setErrorCode(int errorCode)
		{
			this.errorCode = errorCode;
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
	public static PlatformContentRequest getContent(int uniqueId, String contentData, PlatformContentListener<PlatformContentModel> listener, boolean clearRequestInQueue)
	{
		Logger.d("PlatformContent", "Content Dir : " + PlatformContentConstants.PLATFORM_CONTENT_DIR);
		PlatformContentModel model = PlatformContentModel.make(uniqueId,contentData);
		if(model != null) {
			model.setUniqueId(uniqueId); // GSON issue
		}
		PlatformContentRequest request = PlatformContentRequest.make(model, listener);


		if (request != null)
		{
			if(clearRequestInQueue){
				PlatformZipDownloader.removeDownloadingRequest(request.getContentData().getId());
			}
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
	public static PlatformContentRequest getContent(int uniqueId, String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		return getContent(uniqueId, contentData,listener, false);
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
