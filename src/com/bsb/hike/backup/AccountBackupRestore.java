package com.bsb.hike.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntDef;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.BackupState;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * AccountBackupRestore is a singleton class that performs are the backup/restore related
 * operations
 * @author gauravmittal
 */
public class AccountBackupRestore
{

	private static final String LOGTAG = AccountBackupRestore.class.getSimpleName();

	/**
	 * Restore states : <br>
	 * 1. Restore is Successful <br>
	 * 2. Available and does not belong to current msisdn <br>
	 * 3. Available and is incompatible with the current app version <br>
	 * 4. Restore error
	 */
	public static final int STATE_RESTORE_SUCCESS = 1;

	public static final int STATE_RESTORE_FAILURE_MSISDN_MISMATCH = 2;

	public static final int STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION = 3;

	public static final int STATE_RESTORE_FAILURE_GENERIC = 4;


	@IntDef({STATE_RESTORE_SUCCESS, STATE_RESTORE_FAILURE_MSISDN_MISMATCH, STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION, STATE_RESTORE_FAILURE_GENERIC})
	@Retention(RetentionPolicy.SOURCE)
	public @interface RestoreErrorStates {}

	public static final String RESTORE_EVENT_KEY = "rstr";

	public static final String BACKUP_EVENT_KEY = "bck";

	public static final String SIZE = "sz";

	public static final String STATUS = "sts";

	public static final String TIME_TAKEN = "tt";

	public static final String BACKUP = "backup";

	public static final String DATA = "data";

	private static volatile AccountBackupRestore _instance = null;

	private final Context mContext;

	private AccountBackupRestore(Context context)
	{
		this.mContext = context;
	}

	/**
	 * Gets the BDBackupRestore instance. Creates one it not already created.
	 * @param context
	 * @return
	 * 		The BDBackupRestore instance
	 */
	public static AccountBackupRestore getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (AccountBackupRestore.class)
			{
				if (_instance == null)
					_instance = new AccountBackupRestore(context.getApplicationContext());
			}
		}
		return _instance;
	}

	/**
	 * Schedules next auto backup.
	 */
	public void scheduleNextAutoBackup()
	{
		long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(), 3, 0, 0, 0);
		// If the scheduled time is in the past OR the account restore process(at the time of signup) is not yet complete.
		// Scheduled time is increased by 24 hours i.e. same time next day.
		if (scheduleTime < System.currentTimeMillis() || !mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false))
		{
			scheduleTime += 24 * 60 * 60 * 1000;
		}
		HikeAlarmManager.setAlarm(mContext, scheduleTime, HikeAlarmManager.REQUESTCODE_PERIODIC_BACKUP, true);
		Logger.d(LOGTAG, "Scheduled next Auto-Backup for: " + Utils.getFormattedDateTimeFromTimestamp(scheduleTime / 1000, mContext.getResources().getConfiguration().locale));
	}

	/**
	 * Creates a complete backup of chats and the specified preferences.
	 * @return
	 * 	true for success, and false for for failure. 
	 */
	public boolean backup()
	{
		long time = System.currentTimeMillis();
		boolean result = true;
		String backupToken = getBackupToken();
		List<BackupableRestorable> backupItems = new ArrayList<>();
		backupItems.add(new DBsBackupRestore(backupToken));
		backupItems.add(new PrefBackupRestore(backupToken));
		try
		{
			if (result) {
				for (BackupableRestorable item : backupItems) {
					if (!item.preBackupSetup()) {
						result = false;
						break;
					}
				}
			}

			if (result) {
				for (BackupableRestorable item : backupItems) {
					item.backup();
				}
			}

			if (result) {
				for (BackupableRestorable item : backupItems) {
					item.postBackupSetup();
				}
			}

			backupUserData();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		for (BackupableRestorable item : backupItems)
		{
			item.finish();
		}
		if (result)
		{
			File oldBackupStateFile = getBackupStateFile();
			if (oldBackupStateFile != null)
			{
				oldBackupStateFile.delete();
			}
		}
		time = System.currentTimeMillis() - time;
		Logger.d(LOGTAG, "Backup " + result + " in " + time / 1000 + "." + time % 1000 + "s");
		recordLog(BACKUP_EVENT_KEY, result, time);
		return result;
	}

	private void backupUserData() throws Exception
	{
		BackupMetadata backupMetadata = new BackupMetadata(mContext);
		String dataString = backupMetadata.toString();
		File userDataFile = getMetadataFile();
		BackupUtils.writeToFile(dataString, userDataFile);
	}

	private BackupMetadata getBackupMetadata() {
		BackupMetadata userData;
		try
		{
			File userDataFile = getMetadataFile();
			String userDataString = BackupUtils.readStringFromFile(userDataFile);
			userData = new BackupMetadata(mContext, userDataString);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return userData;
	}

	/**
	 * Restores the complete backup of chats and the specified preferences.
	 *
	 * @return an integer value which can be amongst the following :
	 * 1. Restore is Successful <br>
	 * 2. Available and does not belong to current msisdn <br>
	 * 3. Available and is incompatible with the current app version <br>
	 * 4. Restore error
	 */
	@RestoreErrorStates
	public int restore()
	{
		Long time = System.currentTimeMillis();

		@RestoreErrorStates
		int successState = STATE_RESTORE_SUCCESS;

		String backupToken = getBackupToken();
		BackupState state = getBackupState();
		BackupMetadata backupMetadata = getBackupMetadata();

		if (state == null && backupMetadata == null)
		{
			successState = STATE_RESTORE_FAILURE_GENERIC;
		}
		else if (!ContactManager.getInstance().isMyMsisdn(backupMetadata.getMsisdn()))
		{
			successState = STATE_RESTORE_FAILURE_MSISDN_MISMATCH;
		}
		else if (backupMetadata != null && !isBackupAppVersionCompatible(backupMetadata.getAppVersion()))
		{
			successState = STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION;
		}
		else if (state != null && !isBackupDbVersionCompatible(state.getDBVersion()))
		{
			successState = STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION;
		}

		if (successState == STATE_RESTORE_SUCCESS)
		{
			List<BackupableRestorable> backupItems = new ArrayList<>();
			backupItems.add(new DBsBackupRestore(backupToken));
			if (state == null && backupMetadata != null)
			{
				backupItems.add(new PrefBackupRestore(backupToken));
			}
			try
			{
				if (successState == STATE_RESTORE_SUCCESS) {
					for (BackupableRestorable item : backupItems) {

						if (!item.preRestoreSetup())
						{
							successState = STATE_RESTORE_FAILURE_GENERIC;
							break;
						}
					}
				}

				if (successState == STATE_RESTORE_SUCCESS) {
					for (BackupableRestorable item : backupItems) {
						item.restore();
					}
				}

				if (successState == STATE_RESTORE_SUCCESS) {
					for (BackupableRestorable item : backupItems) {
						item.postRestoreSetup();
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				successState = STATE_RESTORE_FAILURE_GENERIC;
			}

			for (BackupableRestorable item : backupItems)
			{
				item.finish();
			}
		}
		if (successState == STATE_RESTORE_SUCCESS)
		{
			if (state != null)
			{
				state.restorePrefs(mContext);
			}
		}

		time = System.currentTimeMillis() - time;
		Logger.d(LOGTAG, "Restore " + successState + " in " + time / 1000 + "." + time % 1000 + "s");
		recordLog(RESTORE_EVENT_KEY, successState == STATE_RESTORE_SUCCESS, time);
		logRestoreDetails(backupMetadata);
		return successState;
	}

	/**
	 * Logging the backup and current app details.
	 * Only for the purpose of debugging.
	 * @param backupMetadata
	 */
	private void logRestoreDetails(BackupMetadata backupMetadata)
	{
		if (backupMetadata != null)
			Logger.d(LOGTAG,"Backup Details: " + backupMetadata.toString());

		try
		{
			Logger.d(LOGTAG, "Current App Deatils: " + (new BackupMetadata(mContext)).toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void recordLog(String eventKey, boolean result, long timeTaken)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			JSONArray sizes = new JSONArray();
			metadata
					.put(HikeConstants.EVENT_KEY, eventKey)
					.put(SIZE, sizes)
					.put(STATUS, result)
					.put(TIME_TAKEN, timeTaken);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_BACKUP, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private String getBackupToken()
	{
		SharedPreferences settings = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String backupToken = settings.getString(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
		return backupToken;
	}

	/**
	 * Checks if a backup is there or not.
	 * @return
	 * 		true is the backup is available, false otherwise.
	 */
	public boolean isBackupAvailable()
	{
		String backupToken = getBackupToken();
		BackupableRestorable dbBackup = new DBsBackupRestore(backupToken);
		boolean dbBackupReady = true;
		try
		{
			if (!dbBackup.preRestoreSetup())
			{
				dbBackupReady = false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			dbBackupReady = false;
		}

		BackupableRestorable prefBackup = new PrefBackupRestore(backupToken);
		boolean prefbBackupReady = true;
		try
		{
			if (!prefBackup.preRestoreSetup())
			{
				prefbBackupReady = false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			prefbBackupReady = false;
		}

		if (getLastBackupTime() > 0
				&& prefbBackupReady
				&& dbBackupReady)
		{
			return true;
		}
		return false;
	}

	public long getLastBackupTime()
	{
		Long backupTime = (long) -1;
		BackupState state = getBackupState();
		BackupMetadata userData = getBackupMetadata();
		if (userData != null)
		{
			backupTime = userData.getBackupTime();
		}
		else if (state != null)
		{
			backupTime = state.getBackupTime();
		}
		return backupTime;
	}

	/**
	 * Deletes all the backup files.
	 */
	public void deleteAllFiles()
	{
		try
		{
			String backupToken = getBackupToken();
			List<BackupableRestorable> backupItems = new ArrayList<>();
			backupItems.add(new DBsBackupRestore(backupToken));
			backupItems.add(new PrefBackupRestore(backupToken));

			for (BackupableRestorable item : backupItems)
			{
				item.selfDestruct();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		getBackupStateFile().delete();
		getMetadataFile().delete();
	}

	private File getBackupStateFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, BACKUP);
	}

	private BackupState getBackupState()
	{
		BackupState state = null;
		File backupStateFile = getBackupStateFile();
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		try
		{
			if (!backupStateFile.exists())
			{
				return null;
			}
			fileIn = new FileInputStream(backupStateFile);
			in = new ObjectInputStream(fileIn);
			state = (BackupState) in.readObject();
			in.close();
			fileIn.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			BackupUtils.closeChannelsAndStreams(fileIn, in);
		}
		return state;
	}

	/**
	 * Takes fresh backup of the preferences.
	 * @return
	 * 		The success or failure.
	 */
	public boolean updatePrefs()
	{
		boolean prefUpdated = true;
		try
		{
			BackupableRestorable prefBackup = new PrefBackupRestore(getBackupToken());

			prefBackup.preBackupSetup();
			prefBackup.backup();
			prefBackup.postBackupSetup();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			prefUpdated = false;
		}
		return prefUpdated;
	}

	private File getMetadataFile() {
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, DATA);
	}

	/**
	 * Returns whether the current backup file's properties are compatible with the app version on which they are being restored
	 *
	 * @param backupAppVersion
	 * @return
	 */
	private boolean isBackupAppVersionCompatible(int backupAppVersion)
	{
		int appCurrentVersionCode = Utils.getAppVersionCode();

		if (appCurrentVersionCode > 0 && appCurrentVersionCode < backupAppVersion)
		{
			return false;
		}
		return true;
	}

	/**
	 * Returns whether the current backup file's properties are compatible with the app version on which they are being restored
	 *
	 * @param backupDbVersion
	 * @return
	 */
	private boolean isBackupDbVersionCompatible(int backupDbVersion)
	{
		if (backupDbVersion > DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			return false;
		}
		return true;
	}
}
