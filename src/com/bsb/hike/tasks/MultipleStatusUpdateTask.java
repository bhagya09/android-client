package com.bsb.hike.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This class posts multiple status updates
 *
 * Created by Atul M on 27/12/15.
 */
public class MultipleStatusUpdateTask implements IHikeHTTPTask, HikePubSub.Listener
{

	private WeakReference<Activity> mWeakActivity;

	private Runnable timeoutRunnable;

	private final boolean mShowProgressDialog;

	private List<StatusUpdateTask> mTasks;

	private MultiSUTaskListener mListener;

	private long TIME_FOR_TASK = 3000;

	private long timeout = 0;

	private ProgressDialog progressDialog;

	private final String TAG = MultiSUTaskListener.class.getSimpleName();

	private boolean isInit;

	public MultipleStatusUpdateTask(List<StatusUpdateTask> argTasks, MultiSUTaskListener argListener, boolean showProgressDialog, Activity argActivity)
	{
		mTasks = argTasks;
		mListener = argListener;
		mShowProgressDialog = showProgressDialog;
		mWeakActivity = new WeakReference<>(argActivity);
	}

	@Override
	public synchronized void execute()
	{
		init();
		for (StatusUpdateTask task : mTasks)
		{
			if (task.getTaskStatus() == StatusUpdateTask.TASK_IDLE)
			{
				task.execute();
				break;
			}
		}
	}

	private void init()
	{
		if (!isInit)
		{
			isInit = true;

			checkNetworkType();

			timeout = TIME_FOR_TASK * mTasks.size();

			Logger.d(TAG, "Timeout set to " + TIME_FOR_TASK + " * " + mTasks.size());

			registerPubsub();

			if (mShowProgressDialog && timeout > 0)
			{
				showProgressDialog();
			}

			timeoutRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					if (mListener != null)
					{
						if(mWeakActivity!=null && mWeakActivity.get()!=null)
						{
							mWeakActivity.get().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mListener.onTimeout();
									detachCallbacks();
								}
							});
						}
						else
						{
							progressDialog = null;
							mListener.onTimeout();
							detachCallbacks();
						}
					}
				}
			};

			HikeHandlerUtil.getInstance().postRunnableWithDelay(timeoutRunnable, timeout);
		}
	}

	@Override
	public synchronized void cancel()
	{
		for (StatusUpdateTask task : mTasks)
		{
			task.cancel();
		}
		endTask(false);
	}

	@Override
	public Bundle getRequestBundle()
	{
		return null;
	}

	@Override
	public String getRequestId()
	{
		return null;
	}

    private void endTask(boolean successful)
	{
		if (timeoutRunnable != null)
		{
			HikeHandlerUtil.getInstance().removeRunnable(timeoutRunnable);
		}
		unregisterPubsub();
		dismissProgressDialog();
		if (successful)
		{
			if (mListener != null)
			{
				mListener.onSuccess();
			}
		}
		else
		{
			if (mListener != null)
			{
				mListener.onFailed();
			}
		}
	}

	private synchronized void checkTasks()
	{
		// Iterate through all tasks, remove all successful tasks.
		// If the remaining tasks have failed. Prompt user/schedule for later.
		for (StatusUpdateTask task : mTasks)
		{
			if (task.getTaskStatus() == StatusUpdateTask.TASK_SUCCESS)
			{
				mTasks.remove(task);
				Logger.d(TAG, "Upload successful");
				break;
			}
		}

		if (mTasks.isEmpty())
		{
			endTask(true);
			return;
		}

		boolean allFailed = true;
		for (StatusUpdateTask task : mTasks)
		{
			if (task.getTaskStatus() != StatusUpdateTask.TASK_FAILED)
			{
				allFailed = false;
				break;
			}
		}

		if (allFailed)
		{
			endTask(false);
		}

		execute();
	}

	private void registerPubsub()
	{
		HikeMessengerApp.getPubSub().addListener(HikePubSub.STATUS_POST_REQUEST_DONE, this);
	}

	private void unregisterPubsub()
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.STATUS_POST_REQUEST_DONE, this);
	}

	@Override
	public void onEventReceived(final String type, Object object)
	{
		if (HikePubSub.STATUS_POST_REQUEST_DONE.equals(type))
		{
			checkTasks();
		}
	}

	public void showProgressDialog()
	{
		if (mWeakActivity != null & mWeakActivity.get() != null)
		{
			progressDialog = ProgressDialog.show(mWeakActivity.get(), "", mWeakActivity.get().getString(R.string.posting_photos_timeline), true, false);
		}
	}

	public void dismissProgressDialog()
	{
		if (progressDialog != null && progressDialog.isShowing())
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	public static interface MultiSUTaskListener
	{
		void onSuccess();

		void onFailed();

		void onTimeout();
	}

	public NetworkType checkNetworkType()
	{
		int networkType = Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext());

		switch (networkType)
		{
		case -1:
			Toast.makeText(HikeMessengerApp.getInstance(), R.string.no_internet_msg, Toast.LENGTH_SHORT).show();
			endTask(false);
			return NetworkType.NO_NETWORK;
		case 0:
			TIME_FOR_TASK = 0;
			return NetworkType.TWO_G;
		case 1:
			TIME_FOR_TASK = 3500;
			return NetworkType.WIFI;
		case 2:
			TIME_FOR_TASK = 0;
			return NetworkType.TWO_G;
		case 3:
			TIME_FOR_TASK = 3500;
			return NetworkType.THREE_G;
		case 4:
			TIME_FOR_TASK = 3000;
			return NetworkType.FOUR_G;
		default:
			TIME_FOR_TASK = 0;
			return NetworkType.TWO_G;
		}
	}

	public void detachCallbacks()
	{
		dismissProgressDialog();
		mListener = null;
	}

}
