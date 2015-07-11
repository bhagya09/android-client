package com.bsb.hike.ui.fragments;

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
public class HeadLessImageWorkerFragment extends SherlockFragment
{

	static class SingletonClass
	{
		private static SingletonClass instance = null;

		private SingletonClass()
		{
			// Exists only to defeat instantiation.
		}

		public static SingletonClass getInstance()
		{
			if (instance == null)
			{
				synchronized (SingletonClass.class)
				{
					if (instance == null)
					{
						instance = new SingletonClass();
					}
				}
			}
			return instance;
		}
	}

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
	protected boolean doAtomicFileDel(String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestFailAtomicFileIO");
		synchronized (SingletonClass.getInstance())
		{
			return Utils.removeFile(tmpFilePath);
		}
	}

	/**
	 * Deletes temp File and original File
	 * 
	 * @param orignialFilePath
	 * @param tmpFilePath
	 */
	protected boolean doAtomicMultiFileDel(String orignialFilePath, String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestFailAtomicFileIO");
		synchronized (SingletonClass.getInstance())
		{
			return (Utils.removeFile(tmpFilePath) && Utils.removeFile(orignialFilePath));
		}
	}

	/**
	 * Renames tmp file to fileName
	 * 
	 * @param originalFilePath
	 * @param tmpFilePath
	 * @return
	 */
	protected boolean doAtomicFileRenaming(String originalFilePath, String tmpFilePath)
	{
		Logger.d("dp_download", "inside API doRequestSuccAtomicFileIO");
		synchronized (SingletonClass.getInstance())
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
		Logger.d("dp_download", "inside API doContactManagerIconChange");
		synchronized (SingletonClass.getInstance())
		{
			ContactManager.getInstance().setIcon(msisdn, bytes, isProfilePc);
		}
	}
}