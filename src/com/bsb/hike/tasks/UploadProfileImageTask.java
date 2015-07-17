package com.bsb.hike.tasks;

import java.io.File;
import java.lang.ref.WeakReference;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

/**
 * This class Uploads image with 
 * FileName :- {@value fileName} 
 * Directory:- {@value filePath}
 * 
 */
public class UploadProfileImageTask
{
	private String fileName;

	private String filePath;

	private WeakReference<UploadProfileImageTaskCallbacks> uploadProfileImageTaskCallbacks;
	
	private final String TAG = "dp_upload_task";

	public UploadProfileImageTask(String filePath, String fileName)
	{
		this.filePath = filePath;
		this.fileName = fileName;
	}

	public void execute()
	{
		File dir = new File(filePath);
		if (!dir.exists())
		{
			//doOnFailure();
			
			if (uploadProfileImageTaskCallbacks != null)
			{
				uploadProfileImageTaskCallbacks.get().onRequestCancelled();
			}
			
			return;
		}

		RequestToken token = HttpRequests.editProfileAvatarRequest(fileName, requestListener);
		token.execute();
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			//doOnSuccess();

			if (uploadProfileImageTaskCallbacks != null)
			{
				Logger.d(TAG, "calling onRequestSuccess of listener from UploadImageProfileTask");
				uploadProfileImageTaskCallbacks.get().onRequestSuccess(result);
			}

		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			if (uploadProfileImageTaskCallbacks != null)
			{
				uploadProfileImageTaskCallbacks.get().onRequestProgressUpdate(progress);
			}
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				//doOnCancelled();

				if (uploadProfileImageTaskCallbacks != null)
				{
					uploadProfileImageTaskCallbacks.get().onRequestCancelled();
				}
			}
			else
			{
				//doOnFailure();

				if (uploadProfileImageTaskCallbacks != null)
				{
					uploadProfileImageTaskCallbacks.get().onRequestFailure(httpException);
				}
			}
		}
	};


	public String getFilePath()
	{
		return filePath;
	}

	/**
	 * 
	 * Callback interface through which this task will report it's progress and results back to the caller. These will run on "http-response" thread
	 *
	 */
	public interface UploadProfileImageTaskCallbacks
	{
		public void onRequestSuccess(Response result);

		public void onRequestCancelled();

		public void onRequestProgressUpdate(float progress);

		public void onRequestFailure(HttpException httpException);
	}

	public void setUploadProfileImageTaskCallbacks(UploadProfileImageTaskCallbacks uploadProfileImageTaskCallbacks)
	{
		this.uploadProfileImageTaskCallbacks = new WeakReference<UploadProfileImageTaskCallbacks>(uploadProfileImageTaskCallbacks);
	}

}