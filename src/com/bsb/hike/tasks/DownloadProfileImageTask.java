package com.bsb.hike.tasks;

import java.io.File;
import java.lang.ref.WeakReference;

import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Utils;

/**
 * This class Downloads Image from url into file with 
 * FileName :- {@value fileName} 
 * Directory:- {@value filePath}
 * 
 * Note: if url is null --> creates URL on basis of hasCustomIcon, statusImage
 *
 */
public class DownloadProfileImageTask
{
	private String id;

	private String fileName;

	private String filePath;

	private boolean statusImage;
	
	private boolean hasCustomIcon;

	private String urlString;
	
	private WeakReference<DownloadProfileImageTaskCallbacks> downloadProfileImageTaskCallbacks;

	public DownloadProfileImageTask(String id, String filePath, String fileName, boolean hasCustomIcon, boolean statusImage)
	{
		this(id, filePath, fileName, hasCustomIcon, statusImage, null);
	}

	public DownloadProfileImageTask(String id, String filePath, String fileName, boolean hasCustomIcon, boolean statusImage, String url)
	{
		this.id = id;
		this.statusImage = statusImage;
		this.filePath = filePath;
		this.fileName = fileName;
		this.hasCustomIcon = hasCustomIcon;
		this.urlString = url;
	}

	public void execute()
	{
		File dir = new File(filePath);
		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				//doOnFailure();
				
				if (downloadProfileImageTaskCallbacks != null)
				{
					downloadProfileImageTaskCallbacks.get().onRequestCancelled();
				}
				return;
			}
		}

		filePath = filePath + "/" + Utils.getUniqueFilename(HikeFileType.IMAGE);
		
		RequestToken token = HttpRequests.downloadImageTaskRequest(id, fileName, filePath, hasCustomIcon, statusImage, urlString, requestListener);
		token.execute();
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			//doOnSuccess();
			
			if(downloadProfileImageTaskCallbacks != null)
			{
				downloadProfileImageTaskCallbacks.get().onRequestSuccess(result);
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			if(downloadProfileImageTaskCallbacks != null)
			{
				downloadProfileImageTaskCallbacks.get().onRequestProgressUpdate(progress);
			}
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				//doOnCancelled();
				
				if(downloadProfileImageTaskCallbacks != null)
				{
					downloadProfileImageTaskCallbacks.get().onRequestCancelled();
				}
			}
			else
			{
				//doOnFailure();
				
				if(downloadProfileImageTaskCallbacks != null)
				{
					downloadProfileImageTaskCallbacks.get().onRequestFailure(httpException);
				}
			}
		}
	};

	public String getFilePath()
	{
		return filePath;
	}
	
	public void setDownloadProfileImageTaskCallbacks(DownloadProfileImageTaskCallbacks downloadProfileImageTaskCallbacks)
	{
		this.downloadProfileImageTaskCallbacks = new WeakReference<DownloadProfileImageTaskCallbacks>(downloadProfileImageTaskCallbacks);
	}
	
	/**
	 * 
	 * Callback interface through which this task will report 
	 * it's progress and results back to the caller.
	 * These will run on "http-response" thread
	 *
	 */
	public interface DownloadProfileImageTaskCallbacks
	{
		public void onRequestSuccess(Response result);
		
		public void onRequestCancelled();

		public void onRequestProgressUpdate(float progress);

		public void onRequestFailure(HttpException httpException);
	}
}
