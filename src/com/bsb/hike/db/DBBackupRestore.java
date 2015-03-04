package com.bsb.hike.db;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.CBCEncryption;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DBBackupRestore
{
	/**
	 * @author gauravmittal
	 * 	DBBackupRestore is a singleton class that performs are the backup/restore related
	 * 	operations
	 */
	
	public static final String RESTORE_EVENT_KEY = "rstr";

	public static final String BACKUP_EVENT_KEY = "bck";

	public static final String SIZE = "sz";

	public static final String STATUS = "sts";

	public static final String TIME_TAKEN = "tt";

	private static volatile DBBackupRestore _instance = null;

	private static final String HIKE_PACKAGE_NAME = "com.bsb.hike";

	public static final String DATABASE_EXT = ".db";

	public static final String BACKUP = "backup";

	private static final String[] dbNames = { DBConstants.CONVERSATIONS_DATABASE_NAME };

	private static final String[] resetTableNames = { DBConstants.STICKER_SHOP_TABLE, DBConstants.STICKER_CATEGORIES_TABLE };

	private final Context mContext;

	private DBBackupRestore(Context context)
	{
		this.mContext = context;
	}

	/**
	 * Gets the BDBackupRestore instance. Creates one it not already created.
	 * @param context
	 * @return
	 * 		The BDBackupRestore instance
	 */
	public static DBBackupRestore getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (DBBackupRestore.class)
			{
				if (_instance == null)
					_instance = new DBBackupRestore(context.getApplicationContext());
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
		Logger.d(getClass().getSimpleName(), "Scheduled next Auto-Backup for: " + Utils.getFormattedDateTimeFromTimestamp(scheduleTime/1000, mContext.getResources().getConfiguration().locale));
	}

	/**
	 * Creates a complete backup of chats and the specified preferences.
	 * @return
	 * 	true for success, and false for for failure. 
	 */
	public boolean backupDB()
	{
		Long time = System.currentTimeMillis();
		boolean result = true;
		try
		{
			for (String fileName : dbNames)
			{
				File dbCopy = exportDatabse(fileName);
				if (dbCopy == null || !dbCopy.exists())
				{
					result = false;
					break;
				}

				File backup = getDBBackupFile(dbCopy.getName());
				String backupToken = getBackupToken();
				Logger.d(getClass().getSimpleName(), "encrypting with key: " + backupToken);
				if (TextUtils.isEmpty(backupToken))
				{
					throw new Exception("Backup Token is empty");
				}
				CBCEncryption.encryptFile(dbCopy, backup, backupToken);
				dbCopy.delete();
			}
		}
		catch (Exception e)
		{
			deleteTempFiles();
			e.printStackTrace();
			result = false;
		}
		if (result)
		{
			if (!updateBackupState(null))
			{
				result = false;
			}
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Backup " + result + " in " + time / 1000 + "." + time % 1000 + "s");
		recordLog(BACKUP_EVENT_KEY,result,time);
		return result;
	}

	/**
	 * Creates a copy of the specified database of the application.
	 * @param databaseName
	 * 		The name of the database.
	 * @return
	 * 		The copy the database.
	 */
	public File exportDatabse(String databaseName)
	{
		Long time = System.currentTimeMillis();
		File dbCopy;

		FileChannel src = null;
		FileChannel dst = null;
		FileInputStream in = null;
		FileOutputStream out = null;

		try
		{
			File currentDB = getCurrentDBFile(databaseName);
			dbCopy = getDBCopyFile(currentDB.getName());
			in = new FileInputStream(currentDB);
			src = in.getChannel();
			out = new FileOutputStream(dbCopy);
			dst = out.getChannel();

			dst.transferFrom(src, 0, src.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			closeChannelsAndStreams(src, dst, in, out);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "DB Export complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return dbCopy;
	}

	/**
	 * Restores the complete backup of chats and the specified preferences.
	 * @return
	 * 	true for success, and false for for failure. 
	 */
	public boolean restoreDB()
	{
		Long time = System.currentTimeMillis();
		boolean result = true;
		BackupState state = getBackupState();
		if (state == null)
		{
			result = false;
		}
		if (state.getDBVersion() > DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			result = false;
		}
		if (result)
		{
			try
			{
				for (String fileName : dbNames)
				{
					File currentDB = getCurrentDBFile(fileName);
					File dbCopy = getDBCopyFile(currentDB.getName());
					File backup = getDBBackupFile(dbCopy.getName());
					String backupToken = getBackupToken();
					Logger.d(getClass().getSimpleName(), "decrypting with key: " + backupToken);
					if (TextUtils.isEmpty(backupToken))
					{
						throw new Exception("Backup Token is empty");
					}
					CBCEncryption.decryptFile(backup, dbCopy, backupToken);
					importDatabase(dbCopy);
					dbCopy.delete();
				}
			}
			catch (Exception e)
			{
				deleteTempFiles();
				e.printStackTrace();
				result = false;
			}
		}
		if (result)
		{
			state.restorePrefs(mContext);
			postRestoreSetup(state);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "Restore " + result + " in " + time / 1000 + "." + time % 1000 + "s");
		recordLog(RESTORE_EVENT_KEY,result,time);
		return result;
	}

	/**
	 * Replaces the current Application database file with the provided database file
	 * @param dbCopy
	 * 		The file to placed as the new database.
	 */
	private void importDatabase(File dbCopy)
	{
		Long time = System.currentTimeMillis();

		FileChannel src = null;
		FileChannel dst = null;
		FileInputStream in = null;
		FileOutputStream out = null;

		try
		{
			File currentDB = getCurrentDBFile(dbCopy.getName());
			in = new FileInputStream(dbCopy);
			src = in.getChannel();
			out = new FileOutputStream(currentDB);
			dst = out.getChannel();

			dst.transferFrom(src, 0, src.size());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.d(getClass().getSimpleName(), "copy fail");
		}
		finally
		{
			closeChannelsAndStreams(src, dst, in, out);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(getClass().getSimpleName(), "DB import complete!! in " + time / 1000 + "." + time % 1000 + "s");
	}
	
	private void recordLog(String eventKey, boolean result, long timeTaken)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			JSONArray sizes = new JSONArray();
			for (String fileName : dbNames)
			{
				File currentDB = getCurrentDBFile(fileName);
				File dbCopy = getDBCopyFile(currentDB.getName());
				File backup = getDBBackupFile(dbCopy.getName());
				sizes.put(backup.length());
			}
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

	private void postRestoreSetup(BackupState state)
	{
		if (state.getDBVersion() < DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			HikeConversationsDatabase.getInstance().upgrade(state.getDBVersion() , DBConstants.CONVERSATIONS_DATABASE_VERSION);
		}
		for (String table : resetTableNames)
		{
			HikeConversationsDatabase.getInstance().clearTable(table);
		}
		HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1();
	}

	/**
	 * Closes the closeables.
	 * @param closeables
	 * 		Set of the closeables to be closed.
	 */
	private void closeChannelsAndStreams(Closeable... closeables)
	{
		for (Closeable closeable : closeables)
		{
			try
			{
				if (closeable != null)
					closeable.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks if a backup is there or not.
	 * @return
	 * 		true is the backup is available, false otherwise.
	 */
	public boolean isBackupAvailable()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File DBCopy = getDBCopyFile(currentDB.getName());
			File backup = getDBBackupFile(DBCopy.getName());
			if (!backup.exists())
				return false;
		}
		BackupState state = getBackupState();
		if (state != null)
		{
			if (state.getBackupTime() > 0)
			{
				return true;
			}
		}
		return false;
	}

	public long getLastBackupTime()
	{
		BackupState state = getBackupState();
		if (state != null)
		{
			return state.getBackupTime();
		}
		return -1;
	}

	/**
	 * Deletes the temporary files.
	 */
	private void deleteTempFiles()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			dbCopy.delete();
		}
	}

	/**
	 * Deletes all the backup files.
	 */
	public void deleteAllFiles()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			File backup = getDBBackupFile(dbCopy.getName());
			dbCopy.delete();
			backup.delete();
		}
		getBackupStateFile().delete();
		deleteTempFiles();
	}

	private File getCurrentDBFile(String dbName)
	{
		File data = Environment.getDataDirectory();
		String currentDBPath = "//data//" + HIKE_PACKAGE_NAME + "//databases//" + dbName + "";
		File currentDB = new File(data, currentDBPath);
		return currentDB;
	}

	private File getDBBackupFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name + "." + BACKUP);
	}

	private File getDBCopyFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name);
	}
	
	private File getBackupStateFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, BACKUP);
	}

	/**
	 * Update the backup state file
	 * @param state
	 * 
	 * @return
	 * 		Success or failure
	 */
	private boolean updateBackupState(BackupState state)
	{
		if (state == null)
		{
			state = new BackupState(dbNames[0], DBConstants.CONVERSATIONS_DATABASE_VERSION);
			state.backupPrefs(mContext);
		}
		File backupStateFile = getBackupStateFile();
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try
		{
			fileOut = new FileOutputStream(backupStateFile);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(state);
			out.close();
			fileOut.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			closeChannelsAndStreams(fileOut,out);
		}
		return true;
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
			closeChannelsAndStreams(fileIn,in);
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
		BackupState state = getBackupState();
		if (state == null)
		{
			state = new BackupState(dbNames[0], DBConstants.CONVERSATIONS_DATABASE_VERSION);
		}
		state.backupPrefs(mContext);
		return updateBackupState(state);
	}

}
