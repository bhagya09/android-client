package com.bsb.hike.tasks;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.StickerManager;
import com.facebook.Session;
import com.google.android.gcm.GCMRegistrar;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{

	public static interface DeleteAccountListener
	{
		public void accountDeleted(boolean isSuccess);
	}

	private DeleteAccountListener listener;

	private boolean finished;

	private boolean delete;

	private Context ctx;

	public DeleteAccountTask(DeleteAccountListener activity, boolean delete, Context context)
	{
		this.listener = activity;
		this.delete = delete;
		this.ctx = context;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		try
		{
			AccountUtils.deleteOrUnlinkAccount(this.delete);

			if (delete)
			{
				DBBackupRestore.getInstance(ctx).deleteAllFiles();
			}

			clearAppData();
			Logger.d("DeleteAccountTask", "account deleted");

			/*
			 * We need to do this where on reset/delete account. We need to we need to run initial setup for stickers. for normal cases it runs from onCreate method of
			 * HikeMessangerApp but in this case onCreate won't be called and user can complete signup.
			 */
			HikeMessengerApp.getInstance().startUpdgradeIntent();
			return true;
		}
		catch (Exception e)
		{
			Logger.e("DeleteAccountTask", "error deleting account", e);
			return false;
		}
	}

	/**
	 * This method cleans up the residual app data during signing out process
	 */
	private void clearAppData()
	{
		StickerDownloadManager.getInstance().shutDownAll();

		/**
		 * Clearing the shared preferences
		 */
		Editor editor = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		Editor appPrefEditor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();

		editor.clear();
		appPrefEditor.clear();
		editor.commit();
		appPrefEditor.commit();
		
		NUXManager.getInstance().shutDownNUX();
		/**
		 * Stopping hike service which will call destroy mqtt
		 */
		HikeMessengerApp app = HikeMessengerApp.getInstance();
		app.setServiceAsDisconnected();
		app.stopService(new Intent(ctx, HikeService.class));

		/**
		 * Unregister from GCM service
		 */
		GCMRegistrar.unregister(ctx.getApplicationContext());

		HikeMessengerApp.clearStealthMsisdn();

		FileTransferManager.getInstance(ctx).shutDownAll();

		/**
		 * Clearing db
		 */
		HikeConversationsDatabase convDb = HikeConversationsDatabase.getInstance();
		convDb.deleteAll();

		if (delete)
		{
			// DBBackupRestore.getInstance(ctx).deleteAllFiles();
		}

		ContactManager.getInstance().deleteAll();

		/**
		 * Clearing cache
		 */
		HikeMessengerApp.getLruCache().clearIconCache();
		HikeMessengerApp.getContactManager().clearCache();
		// IconCacheManager.getInstance().clearIconCache();

		/**
		 * Clearing facebook session tokens
		 */
		Session session = Session.getActiveSession();
		if (session != null)
		{
			session.closeAndClearTokenInformation();
			Session.setActiveSession(null);
		}

		/**
		 * Deleting residual sticker data
		 */
		StickerManager.getInstance().deleteStickers();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		finished = true;
		if (result.booleanValue())
		{
			/* clear any toast notifications */
			NotificationManager mgr = (NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			mgr.cancelAll();

			// redirect user to the welcome screen
			if (listener != null)
			{
				listener.accountDeleted(true);
			}
		}
		else
		{
			if (listener != null)
			{
				listener.accountDeleted(false);
			}
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(ctx,
					this.delete ? ctx.getResources().getString(R.string.delete_account_failed) : ctx.getResources().getString(R.string.unlink_account_failed), duration);
			toast.show();
		}
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.listener = (DeleteAccountListener) activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

}
