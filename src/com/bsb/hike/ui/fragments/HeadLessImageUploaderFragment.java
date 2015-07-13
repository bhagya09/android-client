package com.bsb.hike.ui.fragments;

import android.os.Bundle;

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
public class HeadlessImageUploaderFragment extends HeadlessImageWorkerFragment implements UploadProfileImageTaskCallbacks
{

	private byte[] bytes;
	
	private String tmpFilePath;
	
	private String msisdn;
	
	private boolean toDelTempFileOnCallFail;
	
	private boolean toDelPreviousMsisdnPic;

	private static final String TAG = "dp_upload";

	public static HeadlessImageUploaderFragment newInstance(byte[] bytes, String tmpFilePath, String msisdn, boolean toDelTempFileOnCallFail, boolean toDeletPrevMsisdnPic) {
		
		HeadlessImageUploaderFragment mHeadLessImageUploaderFragment = new HeadlessImageUploaderFragment();
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
		taskCallbacks = null;
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
		Logger.d(TAG, "inside API onRequestSuccess inside HEADLESSIMAGEDOWNLOADFRAGMENT");
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String originqlFilePath = directory + "/" +  Utils.getProfileImageFileName(msisdn);
		
		if(bytes != null)
		{
			doContactManagerIconChange(msisdn, bytes, !toDelPreviousMsisdnPic);
		}
		
		doAtomicFileRenaming(originqlFilePath, tmpFilePath);
		
		if(taskCallbacks != null)
		{
			taskCallbacks.onSuccess(result);
		}
	}

	@Override
	public void onRequestCancelled()
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

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		Logger.d(TAG, "inside API onFailure inside HEADLESSIMAGEUPLOADFRAGMENT");
		
		if(toDelTempFileOnCallFail)
		{
			if(!doAtomicFileDel(tmpFilePath))
			{
				return;
			}
		}

		if(taskCallbacks != null)
		{
			taskCallbacks.onFailed();
		}
	}

}