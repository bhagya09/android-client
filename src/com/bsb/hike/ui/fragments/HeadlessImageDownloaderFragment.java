package com.bsb.hike.ui.fragments;

import java.io.File;

import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.tasks.DownloadProfileImageTask;
import com.bsb.hike.tasks.DownloadProfileImageTask.DownloadProfileImageTaskCallbacks;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This is UILess Fragment allows ImageDownloading using DownloadProfilePicImageTask
 * It provides callback interface to get the result on "http-response" thread
 * 
 * if isProfilePicDownload = true, means used for DP(User/Group/SU) Download
 *
 */
public class HeadlessImageDownloaderFragment extends HeadlessImageWorkerFragment implements DownloadProfileImageTaskCallbacks
{
	private String id;

	private boolean statusImage;

	private boolean hasCustomIcon;
	
	private String url;

	private DownloadProfileImageTask downloadProfileImageTask;
	
	private boolean isProfilePicDownload;
	
	private String msisdn;
	
	private String fileName;
	
	private String name;
	
	String tFilePath;
	
	private static final String TAG = "dp_download";

	public static HeadlessImageDownloaderFragment newInstance(String key, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name,
			String url, boolean isProfilePicDownloaded) {
		
		HeadlessImageDownloaderFragment mHeadLessImageDownloaderFragment = new HeadlessImageDownloaderFragment();
		Bundle arguments = new Bundle();
    	arguments.putString(HikeConstants.Extras.MAPPED_ID, key);
    	arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, statusImage);
    	arguments.putBoolean(HikeConstants.Extras.HAS_CUSTOM_ICON, hasCustomIcon); 
    	arguments.putBoolean(HikeConstants.Extras.IS_PROFILE_PIC_DOWNLOAD, isProfilePicDownloaded);
    	arguments.putString(HikeConstants.Extras.FILE_NAME, fileName);
    	arguments.putString(HikeConstants.Extras.MSISDN, msisdn);
    	arguments.putString(HikeConstants.Extras.NAME, name);
		mHeadLessImageDownloaderFragment.setArguments(arguments);
        return mHeadLessImageDownloaderFragment;
    }
	
	/**
	 * This method will only be called once when the retained Fragment is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Retain this fragment across configuration changes.
		setRetainInstance(true);

		startLoadingTask();
	}

	public void startLoadingTask()
	{
		readArguments();
		
		String fileName = Utils.getProfileImageFileName(id);
		
		// Create and execute the background task.
		String filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		Logger.d(TAG, "executing DownloadProfileImageTask");
		filePath = filePath + File.separator + Utils.getUniqueFilename(HikeFileType.IMAGE);
		
		tFilePath = filePath;
		
		RequestToken token = HttpRequests.downloadImageTaskRequest(id, fileName, filePath, hasCustomIcon, statusImage, url, requestListener);
		token.execute();
	}

	/**
	 * Set the callback to null so we don't accidentally leak the Activity instance.
	 */
	@Override
	public void onDetach()
	{
		super.onDetach();
		taskCallbacks = null;
	}

	private void readArguments()
	{
		id = getArguments().getString(HikeConstants.Extras.MAPPED_ID);
		hasCustomIcon = getArguments().getBoolean(HikeConstants.Extras.HAS_CUSTOM_ICON, false);
		statusImage = getArguments().getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, true);
		url = getArguments().getString(HikeConstants.Extras.URL_TO_LOAD, null);
		isProfilePicDownload = getArguments().getBoolean(HikeConstants.Extras.IS_PROFILE_PIC_DOWNLOAD, false);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		fileName = getArguments().getString(HikeConstants.Extras.FILE_NAME);
		name = getArguments().getString(HikeConstants.Extras.NAME);
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			Logger.d(TAG, "inside API onRequestSuccess inside HEADLESSIMAGEDOWNLOADFRAGMENT");
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			String filePath = directory + "/" +  Utils.getProfileImageFileName(id);
			
			if(!doAtomicFileRenaming(filePath, tFilePath))
			{
				return;
			}
			
			if(isProfilePicDownload)
			{
				if(!doPostSuccessfulProfilePicDownload())
				{
					return ;
				}
			}
			
			if(taskCallbacks.get() != null)
			{
				taskCallbacks.get().onSuccess(result);
			}
			
			removeHeadlessFragement();
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				doAtomicMultiFileDel(Utils.getProfileImageFileName(id), tFilePath);
				
				if(taskCallbacks.get() != null)
				{
					taskCallbacks.get().onCancelled();
				}
				
				removeHeadlessFragement();
			}
			else
			{
				doAtomicMultiFileDel(Utils.getProfileImageFileName(id), tFilePath);
				
				if(taskCallbacks.get() != null)
				{
					taskCallbacks.get().onFailed();
				}
				
				removeHeadlessFragement();
			}
		}
	};
	
	@Override
	public void onRequestProgressUpdate(float progress)
	{
		
	}
	
	@Override
	public void onRequestSuccess(Response result)
	{
		Logger.d(TAG, "inside API onRequestSuccess inside HEADLESSIMAGEDOWNLOADFRAGMENT");
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String filePath = directory + "/" +  Utils.getProfileImageFileName(id);
		
		if(!doAtomicFileRenaming(filePath, downloadProfileImageTask.getFilePath()))
		{
			return;
		}
		
		if(isProfilePicDownload)
		{
			if(!doPostSuccessfulProfilePicDownload())
			{
				return ;
			}
		}
		
		if(taskCallbacks.get() != null)
		{
			taskCallbacks.get().onSuccess(result);
		}
		
		removeHeadlessFragement();
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		doAtomicMultiFileDel(Utils.getProfileImageFileName(id), downloadProfileImageTask.getFilePath());
		
		if(taskCallbacks.get() != null)
		{
			taskCallbacks.get().onFailed();
		}
		
		removeHeadlessFragement();
	}

	@Override
	public void onRequestCancelled()
	{
		doAtomicMultiFileDel(Utils.getProfileImageFileName(id), downloadProfileImageTask.getFilePath());
		
		if(taskCallbacks.get() != null)
		{
			taskCallbacks.get().onCancelled();
		}
		
		removeHeadlessFragement();
	}
	
	private boolean doPostSuccessfulProfilePicDownload()
	{
		Logger.d(TAG, "inside API doPostSuccessfulProfilePicDownload");
		String tempId = id;

		if (!statusImage)
		{
			tempId = id + ProfileActivity.PROFILE_PIC_SUFFIX;
		}

		HikeMessengerApp.getLruCache().remove(tempId);

		if (statusImage)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
		}
		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_DOWNLOADED, id);
		}

		if (this.name == null)
		{
			this.name = this.msisdn; // show the msisdn if its an unsaved contact
		}
		
		if (statusImage && !TextUtils.isEmpty(this.fileName) && !TextUtils.isEmpty(this.msisdn))
		{
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			this.fileName = directory + File.separator + Utils.getProfileImageFileName(msisdn);

			Bundle bundle = new Bundle();
			bundle.putString(HikeConstants.Extras.IMAGE_PATH, this.fileName);
			bundle.putString(HikeConstants.Extras.MSISDN, this.msisdn);
			bundle.putString(HikeConstants.Extras.NAME, this.name);
			HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_AVATAR_DOWNLOADED, bundle);
		}
		
		return true;
	}
}