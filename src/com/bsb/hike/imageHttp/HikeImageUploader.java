package com.bsb.hike.imageHttp;

import android.support.annotation.Nullable;

import java.io.File;

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
	
	private static RequestToken profileToken;
	
	private RequestToken groupToken;
	
	public static HikeImageUploader newInstance(byte[] bytes, String tmpFilePath, String msisdn, boolean toDelTempFileOnCallFail, boolean toDeletPrevMsisdnPic) {
		
		HikeImageUploader mHeadLessImageUploaderFragment = new HikeImageUploader();
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

		Logger.d(TAG, "Attempting upload");
		
		if(OneToNConversationUtils.isGroupConversation(msisdn))
		{
			groupToken = HttpRequests.editGroupProfileAvatarRequest(tmpFilePath, requestListener, msisdn);
			
			executeToken(groupToken);
			
		}
		else
		{
			if(profileToken == null)
			{
				Logger.d(TAG, "Begining upload");
				
				profileToken = HttpRequests.editProfileAvatarRequest(tmpFilePath, requestListener);
				
				executeToken(profileToken);
				
			}
			else
			{
				Logger.d(TAG, "Aborting upload");
				if(taskCallbacks != null)
				{
					taskCallbacks.onTaskAlreadyRunning();
				}
			}
			
		}
		
	}
	
	public static RequestToken getProfileRequestToken()
	{
		return profileToken;
	}
	
	private void executeToken(RequestToken token)
	{
		if(token !=null)
		{
			Logger.d(TAG, "Begining upload");
			
			token.execute();
			
		}
		else
		{
			Logger.d(TAG, "Aborting upload, upload Failed");
			if(taskCallbacks != null)
			{
				taskCallbacks.onFailed();
			}
		}
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			Logger.d(TAG, "inside API onRequestSuccess inside HEADLESS IMAGE UPLOAD FRAGMENT");
			
			clearTokens();
			
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			String originqlFilePath = directory + File.separator +  Utils.getProfileImageFileName(msisdn);
			
			if(bytes != null)
			{
				doContactManagerIconChange(msisdn, bytes, !toDelPreviousMsisdnPic);
			}
			
			doAtomicFileRenaming(originqlFilePath, tmpFilePath);
			
			if(taskCallbacks != null)
			{
				Logger.d(TAG, "calling onSuccess of listener");
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
			clearTokens();
			
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				Logger.d(TAG, "inside API onRequestCancelled inside HEADLESSIMAGEUPLOAD FRAGMENT");
				
				if(toDelTempFileOnCallFail)
				{
					doAtomicFileDel(tmpFilePath);
				}
				
				if(taskCallbacks != null)
				{
					taskCallbacks.onCancelled();
				}
			}
			else
			{
				Logger.d(TAG, "inside API onFailure inside HEADLESSIMAGEUPLOADFRAGMENT");
				
				if(toDelTempFileOnCallFail)
				{
					doAtomicFileDel(tmpFilePath);
				}

				if(taskCallbacks != null)
				{
					taskCallbacks.onFailed();
				}
			}
		}
	};
	
	public boolean isTaskRunning()
	{
		RequestToken currentToken = null;
		
		if(OneToNConversationUtils.isGroupConversation(msisdn))
		{
			currentToken = groupToken ;
		}
		else
		{
			currentToken = profileToken ;
		}
		
		if(currentToken == null)
		{
			return false;
		}
		
		return currentToken.isRequestRunning();
	}
	
	private void clearTokens()
	{
		if(OneToNConversationUtils.isGroupConversation(msisdn))
		{
			groupToken = null;
		}
		else
		{
			profileToken = null;
		}
	}
}