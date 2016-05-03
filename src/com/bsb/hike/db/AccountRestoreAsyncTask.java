package com.bsb.hike.db;

import android.os.AsyncTask;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.lang.ref.WeakReference;

import static com.bsb.hike.backup.AccountBackupRestore.RestoreErrorStates;

/**
 * This Async Task is used to performing restore operation when db goes kaput!
 * Created by piyush on 16/03/16.
 */
public class AccountRestoreAsyncTask extends AsyncTask<Void, Void, Integer>
{
	private WeakReference<IRestoreCallback> mCallback;

	public AccountRestoreAsyncTask(WeakReference<IRestoreCallback> mCallback)
	{
		this.mCallback = mCallback;
	}

	@Override
	protected void onPreExecute()
	{
		if (mCallback.get() != null)
		{
			mCallback.get().preRestoreSetup();
		}
	}

	@Override
	protected Integer doInBackground(Void... params)
	{
		return AccountBackupRestore
				.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).restore();
	}

	@Override
	protected void onPostExecute(@RestoreErrorStates Integer result)
	{
		// If we were not successful in restoring, we will again show the restore screen, hence not letting the user connect to MQ or perform other tasks.
		if (result == AccountBackupRestore.STATE_RESTORE_SUCCESS)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DB_CORRUPT, false);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS,
					false); // UnBlock any possible notifs as well

			Utils.connectToMQTT();
		}

		if (mCallback.get() != null)
		{
			mCallback.get().postRestoreFinished(result);
		}
	}

	public void setRestoreCallback(WeakReference<IRestoreCallback> mCallback)
	{
		this.mCallback = mCallback;
	}

	/**
	 * Use this interface if you require to communicate between the caller of this async task and the task itself
	 */
	public interface IRestoreCallback
	{
		public void preRestoreSetup();

		public void postRestoreFinished(@RestoreErrorStates Integer restoreResult);
	}
}
