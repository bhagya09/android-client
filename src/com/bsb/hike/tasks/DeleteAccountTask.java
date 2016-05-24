package com.bsb.hike.tasks;

import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.ces.disk.CesDiskManager;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineException;
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
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
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

		HttpRequestStateDB.getInstance().deleteAll();

		if (delete)
		{
			// DBBackupRestore.getInstance(ctx).deleteAllFiles();
		}
		
		StickerSearchDataController.getInstance().clear(true);
		if(delete)
		{
			HikeSystemSettingsDBUtil.getInstance(true).deleteAllHikeSpecificData();
		}
		else
		{
			HikeSystemSettingsDBUtil.getInstance(true).deleteAllHikeSpecificDataExcept(HikeConstants.MODULE_STICKER_SEARCH);
		}
		StickerSearchManager.getInstance().shutdown();

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
		 * Deleting residual sticker data on account deletion only and not in case of unlink relink account
		 */
		if (delete)
		{
			StickerManager.getInstance().deleteStickers();
		}
		Utils.deleteDiskCache();
		CesDiskManager.deleteAllCesData();
		
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
		HAManager.getInstance().record(AnalyticsConstants.EVENT_DELETE_ACCOUNT, AnalyticsConstants.NON_UI_EVENT,
                delete ? AnalyticsConstants.DELETE_ACCOUNT : AnalyticsConstants.RESET_ACCOUNT,
                HAManager.EventPriority.HIGH);
		if (delete)
		{
			AccountBackupRestore.getInstance(ctx).deleteAllFiles();
		}

		if (!delete)
		{
			AccountBackupRestore.getInstance(ctx).backup(); // Keep a backup if it's reset account
		}
		
		/*
		 *Closing connection is connected or connecting  via hike direct    
		 */
		if(OfflineController.getInstance().isConnected() || OfflineController.getInstance().isConnecting())
		{
			OfflineController.getInstance().shutdownProcess(new OfflineException(OfflineException.USER_DISCONNECTED));
		}
		//Resetting to use Phone Language as default
		LocalLanguageUtils.setApplicationLocalLanguage(new LocalLanguage("Phone Language",LocalLanguage.PhoneLangauge.getLocale()), HikeConstants.APP_LANG_CHANGED_DEL_ACC);
		clearAppData();
		Logger.d("DeleteAccountTask", "account deleted");

		/*
		 * We need to do this where on reset/delete account. We need to we need to run initial setup for stickers. for normal cases it runs from onCreate method of HikeMessangerApp
		 * but in this case onCreate won't be called and user can complete signup.
		 */
		HikeMessengerApp.getInstance().startUpdgradeIntent();


		finished = true;

		/* clear any toast notifications */
		try
		{
			NotificationManager mgr = (NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			mgr.cancelAll();
		}
		catch (SecurityException e)
		{
			/**
			 * some of the users on HTC HTC Desire 626GPLUS dual sim were getting permission denial while try to cancel notifications.
			 */
			Logger.e("DeleteAccountTask", "Exception while canceling notification from DeleteAccountTask", e);
		}

		// reset badge counters
		HikeMessengerApp.getPubSub().publish(HikePubSub.ACCOUNT_RESET_OR_DELETE, null);
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
