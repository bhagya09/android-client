package com.bsb.hike.ui.fragments;

import java.io.File;

import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.tasks.UploadProfileImageTask;
import com.bsb.hike.tasks.UploadProfileImageTask.UploadProfileImageTaskCallbacks;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;


/**
 * 
 * This class takes 'tmpFilePath' as arguments
 * Uploads 'tmpFilePath'
 * On Success  :-  1) Delete Any other File Present with name 'msisdn' in directory
 * 				   2) Rename tmpFile to msisdn.jpg
 *				   3) listener callback
 */
public class HeadLessImageUploaderFragment extends HeadLessImageWorkerFragment implements UploadProfileImageTaskCallbacks
{

	private byte[] bytes;
	
	private String tmpFilePath;
	
	private String msisdn;
	
	private boolean toDelTempFileOnCallFail;
	
	private boolean toDelPreviousMsisdnPic;

	private static final String TAG = HeadLessImageWorkerFragment.class.getName();

	public static HeadLessImageUploaderFragment newInstance(byte[] bytes, String tmpFilePath, String msisdn, boolean toDelTempFileOnCallFail, boolean toDeletPrevMsisdnPic) {
		
		HeadLessImageUploaderFragment mHeadLessImageUploaderFragment = new HeadLessImageUploaderFragment();
		Bundle arguments = new Bundle();
    	arguments.putByteArray(HikeConstants.Extras.BYTES, bytes);
    	arguments.putString(HikeConstants.Extras.FILE_PATH, tmpFilePath);
    	arguments.putString(HikeConstants.Extras.MSISDN, msisdn);
    	arguments.putBoolean(HikeConstants.Extras.DEL_SCR_FILE_ON_CALL_FAIL, toDelTempFileOnCallFail);
    	arguments.putBoolean(HikeConstants.Extras.DEL_PREV_MSISDN_PIC, toDeletPrevMsisdnPic);
    	mHeadLessImageUploaderFragment.setArguments(arguments);
        return mHeadLessImageUploaderFragment;
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

		startUpLoadingTask();
	}

	public void startUpLoadingTask()
	{
		readArguments();
		
		String filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		UploadProfileImageTask uploadProfileImageTask = new UploadProfileImageTask(filePath, tmpFilePath);
		uploadProfileImageTask.setUploadProfileImageTaskCallbacks(this);
		uploadProfileImageTask.execute();
	}

	/**
	 * Set the callback to null so we don't accidentally leak the Activity instance.
	 */
	@Override
	public void onDetach()
	{
		super.onDetach();
		mTaskCallbacks = null;
	}

	private void readArguments()
	{
		bytes = getArguments().getByteArray(HikeConstants.Extras.BYTES);
		tmpFilePath = getArguments().getString(HikeConstants.Extras.FILE_PATH);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		toDelTempFileOnCallFail = getArguments().getBoolean(HikeConstants.Extras.DEL_SCR_FILE_ON_CALL_FAIL);
		toDelPreviousMsisdnPic = getArguments().getBoolean(HikeConstants.Extras.DEL_PREV_MSISDN_PIC);
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		Logger.d("dp_download", "inside API onRequestSuccess inside HEADLESSIMAGEDOWNLOADFRAGMENT");
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String originqlFilePath = directory + "/" +  Utils.getProfileImageFileName(msisdn);
		
		if(bytes != null)
		{
			doContactManagerIconChange(msisdn, bytes, !toDelPreviousMsisdnPic);
		}
		
		if(!doAtomicFileRenaming(originqlFilePath, tmpFilePath))
		{
			return;
		}
		
		if(mTaskCallbacks != null)
		{
			mTaskCallbacks.onSuccess(result);
		}
	}

	@Override
	public void onRequestCancelled()
	{
		Logger.d("dp_upload", "inside API onRequestCancelled inside HEADLESSIMAGEUPLOAD FRAGMENT");
		
		if(toDelTempFileOnCallFail)
		{
			if(!doAtomicFileDel(tmpFilePath))
			{
				return;
			}
		}
		
		if(mTaskCallbacks != null)
		{
			mTaskCallbacks.onCancelled();
		}
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		Logger.d("dp_upload", "inside API onFailure inside HEADLESSIMAGEUPLOADFRAGMENT");
		
		if(toDelTempFileOnCallFail)
		{
			if(!doAtomicFileDel(tmpFilePath))
			{
				return;
			}
		}

		if(mTaskCallbacks != null)
		{
			mTaskCallbacks.onFailed();
		}
	}

}