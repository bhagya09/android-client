package com.bsb.hike.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.bsb.hike.db.DBBackupRestore;

public class BackupAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{
	private Context ctx;
	
	private boolean isFinished = false;

	public static interface BackupAccountListener
	{
		public void accountBacked(boolean isSuccess);
	}

	private BackupAccountListener listener;
	
	public BackupAccountTask(Context context, BackupAccountListener activity)
	{
		this.ctx = context.getApplicationContext();
		this.listener = activity;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		return DBBackupRestore.getInstance(ctx).backupDB();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		listener.accountBacked(result);
		isFinished = true;
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.listener = (BackupAccountListener) activity;
		
	}

	@Override
	public boolean isFinished()
	{
		return isFinished;
	}
}
