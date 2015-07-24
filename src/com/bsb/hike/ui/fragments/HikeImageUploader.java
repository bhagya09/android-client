package com.bsb.hike.ui.fragments;

import java.io.File;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;


/**
 * 
 * This class takes 'tmpFilePath' as arguments
 * Uploads 'tmpFilePath'
 * On Success  :-  1) Delete Any other File Present with name 'msisdn' in directory
 * 				   2) Rename tmpFile to msisdn.jpg
 *				   3) listener callback
 */
public class HikeImageUploader extends HikeImageWorker
{

	private byte[] bytes;
	
	private String tmpFilePath;
	
	private String msisdn;
	
	private boolean toDelTempFileOnCallFail;
	
	private boolean toDelPreviousMsisdnPic;

	private static final String TAG = "dp_upload";
	
	private Context mContext;
	
	private RequestToken token;

	public static HikeImageUploader newInstance(Context context, byte[] bytes, String tmpFilePath, String msisdn, boolean toDelTempFileOnCallFail, boolean toDeletPrevMsisdnPic) {
		
		HikeImageUploader mHeadLessImageUploaderFragment = new HikeImageUploader();
		mHeadLessImageUploaderFragment.mContext = context;
		mHeadLessImageUploaderFragment.bytes = bytes;
		mHeadLessImageUploaderFragment.tmpFilePath = tmpFilePath;
		mHeadLessImageUploaderFragment.msisdn = msisdn;
		mHeadLessImageUploaderFragment.toDelTempFileOnCallFail = toDelTempFileOnCallFail;
		mHeadLessImageUploaderFragment.toDelPreviousMsisdnPic = toDeletPrevMsisdnPic;
        return mHeadLessImageUploaderFragment;
    }
	
	public void startUpLoadingTask()
	{
		String filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		File dir = new File(filePath);
		if (!dir.exists())
		{
			Logger.d(TAG, "Directory not present, so returning");
			return;
		}

		if(OneToNConversationUtils.isGroupConversation(msisdn))
		{
			token = HttpRequests.editGroupProfileAvatarRequest(tmpFilePath, requestListener, msisdn);
		}
		else
		{
			token = HttpRequests.editProfileAvatarRequest(tmpFilePath, requestListener);
		}
		if(!token.isRequestRunning())
		{
			token.execute();
		}
		else
		{
			//TODO on UI
			//Toast.makeText(mContext, mContext.getResources().getString(R.string.task_already_running), Toast.LENGTH_SHORT).show();
	    	Logger.d(TAG, "As mImageLoaderFragment already there, so not starting new one");
		}
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			Logger.d(TAG, "inside API onRequestSuccess inside HEADLESS IMAGE UPLOAD FRAGMENT");
			
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			String originqlFilePath = directory + File.separator +  Utils.getProfileImageFileName(msisdn);
			
			if(bytes != null)
			{
				doContactManagerIconChange(msisdn, bytes, !toDelPreviousMsisdnPic);
			}
			
			doAtomicFileRenaming(originqlFilePath, tmpFilePath);
			
			if(taskCallbacks.get() != null)
			{
				Logger.d(TAG, "calling onSuccess of listener");
				taskCallbacks.get().onSuccess(result);
			}
			
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
				Logger.d(TAG, "inside API onRequestCancelled inside HEADLESSIMAGEUPLOAD FRAGMENT");
				
				if(toDelTempFileOnCallFail)
				{
					doAtomicFileDel(tmpFilePath);
				}
				
				if(taskCallbacks.get() != null)
				{
					taskCallbacks.get().onCancelled();
				}
			}
			else
			{
				Logger.d(TAG, "inside API onFailure inside HEADLESSIMAGEUPLOADFRAGMENT");
				
				if(toDelTempFileOnCallFail)
				{
					doAtomicFileDel(tmpFilePath);
				}

				if(taskCallbacks.get() != null)
				{
					taskCallbacks.get().onFailed();
				}
			}
		}
	};
	
	public boolean isTaskRunning()
	{
		return token.isRequestRunning();
	}
}