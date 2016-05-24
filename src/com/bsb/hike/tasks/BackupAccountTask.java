package com.bsb.hike.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.backup.AccountBackupRestore;

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
		boolean status = AccountBackupRestore.getInstance(ctx).backup();
		try
		{
			Thread.sleep(HikeConstants.BACKUP_RESTORE_UI_DELAY);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		listener.accountBacked(result);
		final int msgStringId = result ? R.string.backup_successful : R.string.backup_failed;
		Toast.makeText(ctx, msgStringId, Toast.LENGTH_SHORT).show();
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
