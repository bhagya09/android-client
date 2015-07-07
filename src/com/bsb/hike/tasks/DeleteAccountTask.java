package com.bsb.hike.tasks;

import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.AccountBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSystemSettingsDBUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;

public class DeleteAccountTask implements ActivityCallableTask
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

	public void execute()
	{
		IRequestListener requestListener = new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject json = (JSONObject) result.getBody().getContent();
				if (!Utils.isResponseValid(json))
				{
					doOnFailure();
					return;
				}
				doOnSuccess();
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure();
			}
		};

		RequestToken requestToken = this.delete ? HttpRequests.deleteAccountRequest(requestListener) : HttpRequests.unlinkAccountRequest(requestListener);
		requestToken.execute();
	}

	/**
	 * This method cleans up the residual app data during signing out process
	 */
	private void clearAppData()
	{
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
		app.hikeBotInfoMap.clear();

		/**
		 * Unregister from GCM service
		 */
		GCMRegistrar.unregister(ctx.getApplicationContext());

		StealthModeManager.getInstance().clearStealthMsisdn();

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
		
		HikeStickerSearchDatabase.getInstance().deleteDataInTables(!delete);
		if(delete)
		{
			HikeSystemSettingsDBUtil.getInstance(true).deleteAllHikeSpecificData();
		}
		else
		{
			HikeSystemSettingsDBUtil.getInstance(true).deleteAllHikeSpecificDataExcept(HikeConstants.STICKER_SEARCH);
		}
		

		ContactManager.getInstance().deleteAll();
		HikeContentDatabase.getInstance().deleteAll();

		/**
		 * Clearing cache
		 */
		HikeMessengerApp.getLruCache().clearIconCache();
		ContactManager.getInstance().clearCache();
		// IconCacheManager.getInstance().clearIconCache();

		/**
		 * Clearing facebook session tokens
		 */
		/*
		 * Session session = Session.getActiveSession(); if (session != null) { session.closeAndClearTokenInformation(); Session.setActiveSession(null); }
		 */

		/**
		 * Deleting residual sticker data
		 */
		StickerManager.getInstance().deleteStickers();
		
		/**
		 * Setting token and uid in memory to null
		 */
		AccountUtils.mToken = null;
		AccountUtils.mUid = null;
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

	private void doOnSuccess()
	{
		if (delete)
		{
			AccountBackupRestore.getInstance(ctx).deleteAllFiles();
		}
		
		clearAppData();
		Logger.d("DeleteAccountTask", "account deleted");

		/*
		 * We need to do this where on reset/delete account. We need to we need to run initial setup for stickers. for normal cases it runs from onCreate method of HikeMessangerApp
		 * but in this case onCreate won't be called and user can complete signup.
		 */
		HikeMessengerApp.getInstance().startUpdgradeIntent();

		finished = true;

		/* clear any toast notifications */
		NotificationManager mgr = (NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
		mgr.cancelAll();

		// redirect user to the welcome screen
		if (listener != null)
		{
			listener.accountDeleted(true);
		}
	}

	private void doOnFailure()
	{
		finished = true;
		if (listener != null)
		{
			listener.accountDeleted(false);
		}
	}

}
