package com.bsb.hike.ui.fragments;

import java.io.File;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This is base class for any Fragment used for any Call 
 * which does File I/O on success/Failure/Cancelled
 * 
 * It ensures that all File I/O are in in sync as concurrent Del/creating/renaming
 * Files will cause issues.So this does those operations on keeping a lock
 *
 */
public class HeadLessImageWorkerFragment extends SherlockFragment
{

	/**
	 * Callback interface through which the fragment will report the task's progress and results back to the Caller.
	 */
	public interface TaskCallbacks
	{
		void onProgressUpdate(float percent);

		void onCancelled();

		void onFailed();
		
		void onSuccess(Response result);
		
	}

	protected TaskCallbacks mTaskCallbacks;

	public void setTaskCallbacks(TaskCallbacks callbacks)
	{
		this.mTaskCallbacks = callbacks;
	}
	
	/**
	 * Deletes temp File
	 * 
	 * @param tmpFilePath
	 */
	protected void doRequestFailAtomicFileIO(String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestFailAtomicFileIO");
		synchronized (HikeMessengerApp.getInstance())
		{
			Utils.removeUniqueTempProfileImage(tmpFilePath);
		}
	}
	
	/**
	 * Deletes temp File and original File
	 * 
	 * @param orignialFilePath
	 * @param tmpFilePath
	 */
	protected void doRequestFailAtomicFileIO(String orignialFilePath, String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestFailAtomicFileIO");
		synchronized (HikeMessengerApp.getInstance())
		{
			Utils.removeUniqueTempProfileImage(tmpFilePath);
			File file = new File(orignialFilePath);
			file.delete();
		}
	}

	/**
	 * Deletes temp File and original File
	 * 
	 * @param orignialFilePath
	 * @param tmpFilePath
	 */
	protected void doRequestCancelAtomicFileIO(String orignialFilePath, String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestCancelAtomicFileIO");
		synchronized (HikeMessengerApp.getInstance())
		{
			File file = new File(orignialFilePath);
			file.delete();
			Utils.removeUniqueTempProfileImage(tmpFilePath);
		}
	}
	
	/**
	 * Renames tmp file to fileName
	 * 
	 * @param originalFilePath
	 * @param tmpFilePath
	 */
	protected void doRequestSuccAtomicFileIO(String originalFilePath, String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestSuccAtomicFileIO");
		synchronized (HikeMessengerApp.getInstance())
		{
			Utils.renameUniqueTempProfileImage(originalFilePath, tmpFilePath);
		}
	}
	
	public void doContactManagerIconChange(String msisdn, byte[] bytes, boolean isProfilePc)
	{
		Logger.d("dp_download", "inside API doContactManagerIconChange");
		synchronized (HikeMessengerApp.getInstance())
		{
			ContactManager.getInstance().setIcon(msisdn, bytes, isProfilePc);
		}
	}
}