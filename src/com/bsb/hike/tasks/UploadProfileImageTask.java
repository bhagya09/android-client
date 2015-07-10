package com.bsb.hike.tasks;

import java.io.File;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;

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

	private UploadProfileImageTaskCallbacks uploadProfileImageTaskCallbacks;

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
			if (!dir.mkdirs())
			{
				doOnFailure();
				
				if (uploadProfileImageTaskCallbacks != null)
				{
					uploadProfileImageTaskCallbacks.onRequestCancelled();
				}
				
				return;
			}
		}

		RequestToken token = HttpRequests.editProfileAvatarRequest(fileName, requestListener);
		token.execute();
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			doOnSuccess();

			if (uploadProfileImageTaskCallbacks != null)
			{
				uploadProfileImageTaskCallbacks.onRequestSuccess(result);
			}

		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			if (uploadProfileImageTaskCallbacks != null)
			{
				uploadProfileImageTaskCallbacks.onRequestProgressUpdate(progress);
			}
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				doOnCancelled();

				if (uploadProfileImageTaskCallbacks != null)
				{
					uploadProfileImageTaskCallbacks.onRequestCancelled();
				}
			}
			else
			{
				doOnFailure();

				if (uploadProfileImageTaskCallbacks != null)
				{
					uploadProfileImageTaskCallbacks.onRequestFailure(httpException);
				}
			}
		}
	};

	public void cancel()
	{
		doOnCancelled();
	}

	private void doOnCancelled()
	{

	}

	private void doOnFailure()
	{

	}

	private void doOnSuccess()
	{

	}

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
		this.uploadProfileImageTaskCallbacks = uploadProfileImageTaskCallbacks;
	}

}