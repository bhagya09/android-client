package com.bsb.hike.imageHttp;

import java.io.File;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
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
public class HikeImageDownloader extends HikeImageWorker 
{
	private String id;

	private boolean statusImage;
	
	private boolean forceNewRequest;

	private boolean hasCustomIcon;
	
	private String url;

	private boolean isProfilePicDownload;
	
	private String msisdn;
	
	private String fileName;
	
	private String name;
	
	private String pathOfTempFile;
	
	private static final String TAG = "dp_download";
	
	public static HikeImageDownloader newInstance(String key, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name,
			String url, boolean isProfilePicDownloaded,boolean forceNewRequest) {
		
		HikeImageDownloader mHeadLessImageDownloaderFragment = new HikeImageDownloader();
		mHeadLessImageDownloaderFragment.id = key;
		mHeadLessImageDownloaderFragment.hasCustomIcon = hasCustomIcon;
		mHeadLessImageDownloaderFragment.statusImage = statusImage;
		mHeadLessImageDownloaderFragment.url = url;
		mHeadLessImageDownloaderFragment.isProfilePicDownload = isProfilePicDownloaded;
		mHeadLessImageDownloaderFragment.msisdn = msisdn;
		mHeadLessImageDownloaderFragment.fileName = fileName;
		mHeadLessImageDownloaderFragment.name = name;
		mHeadLessImageDownloaderFragment.forceNewRequest = forceNewRequest;
        return mHeadLessImageDownloaderFragment;
    }

	public void startLoadingTask()
	{
		//The commented refactoring to be done in next release
		//if(TextUtils.isEmpty(fileName))
		//{
		String fileName= Utils.getProfileImageFileName(id);
		//}
		
		Logger.d(TAG, "executing DownloadProfileImageTask");
		pathOfTempFile = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + File.separator + Utils.getTempProfileImageFileName(id, true);
		
		RequestToken token = HttpRequests.downloadImageTaskRequest(id, fileName, pathOfTempFile, hasCustomIcon, statusImage, url,forceNewRequest, requestListener);
		if(token != null && !token.isRequestRunning())
		{
			token.execute();
		}
		else
		{
	    	Logger.d(TAG, "As mImageLoaderFragment already there, so not starting new one");
		}
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			Logger.d(TAG, "inside API onRequestSuccess inside HEADLESSIMAGEDOWNLOADFRAGMENT");
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			
			String filePath = directory + "/" +  Utils.getProfileImageFileName(id);
			//String filePath = directory + "/" +  fileName;
			
			if(!doAtomicFileRenaming(filePath, pathOfTempFile))
			{
				return;
			}
			
			if(isProfilePicDownload)
			{
				if(!doPostSuccessfulProfilePicDownload())
				{
					return;
				}
			}
			
			if(taskCallbacks != null)
			{
				taskCallbacks.onSuccess(result);
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
		}

		@Override
		public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				doAtomicMultiFileDel(Utils.getProfileImageFileName(id), pathOfTempFile);
				//doAtomicMultiFileDel(fileName, pathOfTempFile);
				
				if(taskCallbacks != null)
				{
					taskCallbacks.onCancelled();
				}
			}
			else
			{
				doAtomicMultiFileDel(Utils.getProfileImageFileName(id), pathOfTempFile);
				//doAtomicMultiFileDel(fileName, pathOfTempFile);
				
				if(taskCallbacks != null)
				{
					taskCallbacks.onFailed();
				}
			}
		}
	};
	
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

			String postedImgPath = directory + "/" +  Utils.getProfileImageFileName(id);
			Bundle bundle = new Bundle();
			bundle.putString(HikeConstants.Extras.PATH, postedImgPath);
			bundle.putString(HikeConstants.Extras.IMAGE_PATH, this.fileName);
			bundle.putString(HikeConstants.Extras.MSISDN, this.msisdn);
			bundle.putString(HikeConstants.Extras.NAME, this.name);
			bundle.putString(HikeConstants.STATUS_ID, this.id);

			HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_AVATAR_DOWNLOADED, bundle);
		}
		
		return true;
	}
}