package com.bsb.hike.db;

import android.os.AsyncTask;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import static com.bsb.hike.backup.AccountBackupRestore.RestoreErrorStates;

/**
 * This Async Task is used to performing restore operation when db goes kaput!
 * Created by piyush on 16/03/16.
 */
public class DBRestoreAsyncTask extends AsyncTask<Void, Void, Integer>
{
	private IRestoreCallback mCallback;

	public DBRestoreAsyncTask(IRestoreCallback mCallback)
	{
		this.mCallback = mCallback;
	}

	@Override
	protected void onPreExecute()
	{
		if (mCallback != null)
		{
			mCallback.preRestoreSetup();
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
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DB_CORRUPT, false);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false); // UnBlock any possible notifs as well

		Utils.connectToMQTT();

		if (mCallback != null)
		{
			mCallback.postRestoreFinished(result);
		}
	}

	public void setRestoreCallback(IRestoreCallback mCallback)
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
