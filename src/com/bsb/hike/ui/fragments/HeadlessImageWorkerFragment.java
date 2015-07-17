package com.bsb.hike.ui.fragments;

import java.lang.ref.WeakReference;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This is base class for any Fragment used for any Call which does File I/O on success/Failure/Cancelled
 * 
 * It ensures that all File I/O are in in sync as concurrent Del/creating/renaming Files will cause issues.So this does those operations on keeping a lock
 *
 */
public class HeadlessImageWorkerFragment extends SherlockFragment
{

	private static final String TAG = "dp_woker";
	
	private static volatile Object singleton = new Object();
	
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

	protected WeakReference<TaskCallbacks> taskCallbacks;

	public void setTaskCallbacks(TaskCallbacks callbacks)
	{
		this.taskCallbacks = new WeakReference<TaskCallbacks>(callbacks);
	}

	/**
	 * Deletes temp File
	 * 
	 * @param tmpFilePath
	 * @return true:- if file is deleted successfully
	 * 	       false:- if file is not deleted or file is not present
	 */
	protected boolean doAtomicFileDel(String tmpFilePath)
	{
		Logger.d(TAG, "inside API doRequestFailAtomicFileIO");
		synchronized (singleton)
		{
			return Utils.removeFile(tmpFilePath);
		}
	}

	/**
	 * Deletes temp File and original File
	 * 
	 * @param orignialFilePath
	 * @param tmpFilePath
	 * @return true:- Deltes Both file Successfully
	 * 		   false:- Any of file is not deleted  or  Any of the file is not present
	 */
	protected boolean doAtomicMultiFileDel(String orignialFilePath, String tmpFilePath)
	{
		Logger.d(TAG, "inside API doRequestFailAtomicFileIO");
		synchronized (singleton)
		{
			return (Utils.removeFile(tmpFilePath) && Utils.removeFile(orignialFilePath));
		}
	}

	/**
	 * Renames tmp file to fileName
	 * 
	 * @param originalFilePath
	 * @param tmpFilePath
	 * @return true: if tmp file is renamed properly
	 *         false:- if renaming not done or tmp file is not present
	 */
	protected boolean doAtomicFileRenaming(String originalFilePath, String tmpFilePath)
	{
		Logger.d(TAG, "inside API doRequestSuccAtomicFileIO");
		synchronized (singleton)
		{
			return Utils.renameFiles(originalFilePath, tmpFilePath);
		}
	}

	/**
	 * This API removes File with name "msisdn.jpg 
	 * from directory 'Hike Media Profile'"
	 * 
	 * Note: API is made static so that can be used everywhere
	 * Which applies lock on file before deleting it
	 * 
	 * @param msisdn
	 * @param bytes
	 * @param isProfilePc
	 */
	public static void doContactManagerIconChange(String msisdn, byte[] bytes, boolean isProfilePc)
	{
		Logger.d(TAG, "inside API doContactManagerIconChange");
		synchronized (singleton)
		{
			ContactManager.getInstance().setIcon(msisdn, bytes, isProfilePc);
		}
	}
	
	/**
	 * This removes current fragment
	 */
	protected void removeHeadlessFragement()
	{
		getFragmentManager().beginTransaction().remove(this).commit();
	}
}