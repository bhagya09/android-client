package com.bsb.hike.backup;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.channels.FileChannel;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.BackupState;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.CBCEncryption;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
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

	public static final int STATE_MSISDN_MISMATCH = 2;

	public static final int STATE_INCOMPATIBLE_APP_VERSION = 3;

	public static final int STATE_RESTORE_ERROR = 4;


	@IntDef({STATE_RESTORE_SUCCESS, STATE_MSISDN_MISMATCH,STATE_INCOMPATIBLE_APP_VERSION,STATE_RESTORE_ERROR})
	@Retention(RetentionPolicy.SOURCE)
	public @interface RestoreErrorStates {}


	// TODO - Move this to a stand alone file & simplify.
	private class PreferenceBackup
	{
		public static final String PREFS = "pref";

		private JSONObject prefJson;

		public PreferenceBackup()
		{
		}

		public PreferenceBackup takeBackup() throws JSONException
		{
			prefJson = new JSONObject();
			HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

			prefJson.put(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, prefUtil.getData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN, ""))
					.put(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, prefUtil.getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
					.put(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, prefUtil.getData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST, false))
					.put(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, prefUtil.getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
					.put(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, prefUtil.getData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, false))
					.put(HikeMessengerApp.CONV_DB_VERSION_PREF, prefUtil.getData(HikeMessengerApp.CONV_DB_VERSION_PREF, DBConstants.CONVERSATIONS_DATABASE_VERSION))
					.put(HikePlatformConstants.CUSTOM_TABS, prefUtil.getData(HikePlatformConstants.CUSTOM_TABS, false));

			SharedPreferences settingUtils =  PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance());

			prefJson.put(HikeConstants.STEALTH_NOTIFICATION_ENABLED, settingUtils.getBoolean(HikeConstants.STEALTH_NOTIFICATION_ENABLED, true))
					.put(HikeConstants.STEALTH_INDICATOR_ENABLED, settingUtils.getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED, false))
					.put(HikeConstants.CHANGE_STEALTH_TIMEOUT, settingUtils.getString(HikeConstants.CHANGE_STEALTH_TIMEOUT, StealthModeManager.DEFAULT_RESET_TOGGLE_TIME));
			return this;
		}

		public String serialize()
		{
			return prefJson.toString();
		}

		public void restore(String prefJsonString) throws JSONException
		{
			prefJson = new JSONObject(prefJsonString);
			HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

			if (prefJson.has(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN))
			{
				String key = HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN;
				prefUtil.saveData(key, prefJson.getString(key));
			}

			if (prefJson.has(HikeMessengerApp.STEALTH_MODE_SETUP_DONE))
			{
				String key = HikeMessengerApp.STEALTH_MODE_SETUP_DONE;
				prefUtil.saveData(key, prefJson.getBoolean(key));
			}
			if (prefJson.has(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST))
			{
				String key = HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST;
				prefUtil.saveData(key, prefJson.getBoolean(key));
			}
			if (prefJson.has(HikeMessengerApp.SHOW_STEALTH_INFO_TIP))
			{
				String key = HikeMessengerApp.SHOW_STEALTH_INFO_TIP;
				prefUtil.saveData(key, prefJson.getBoolean(key));
			}
			if (prefJson.has(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD))
			{
				String key = HikeMessengerApp.STEALTH_PIN_AS_PASSWORD;
				prefUtil.saveData(key, prefJson.getBoolean(key));
			}
			if (prefJson.has(HikeMessengerApp.CONV_DB_VERSION_PREF))
			{
				String key = HikeMessengerApp.CONV_DB_VERSION_PREF;
				prefUtil.saveData(key, prefJson.getInt(key));
			}
			if (prefJson.has(HikePlatformConstants.CUSTOM_TABS))
			{
				String key = HikePlatformConstants.CUSTOM_TABS;
				prefUtil.saveData(key, prefJson.getBoolean(key));
			}

			SharedPreferences settingUtils =  PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance());

			if (prefJson.has(HikeConstants.STEALTH_INDICATOR_ENABLED))
			{
				String key = HikeConstants.STEALTH_INDICATOR_ENABLED;
				settingUtils.edit().putBoolean(key, prefJson.getBoolean(key)).commit();
			}
			if (prefJson.has(HikeConstants.CHANGE_STEALTH_TIMEOUT))
			{
				String key = HikeConstants.CHANGE_STEALTH_TIMEOUT;
				settingUtils.edit().putString(key, prefJson.getString(key)).commit();
			}
			if (prefJson.has(HikeConstants.STEALTH_NOTIFICATION_ENABLED))
			{
				String key = HikeConstants.STEALTH_NOTIFICATION_ENABLED;
				settingUtils.edit().putBoolean(key, prefJson.getBoolean(key)).commit();
			}
		}

		public File getPrefFile()
		{
			new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
			return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, PREFS);
		}
	}



	public static final String RESTORE_EVENT_KEY = "rstr";

	public static final String BACKUP_EVENT_KEY = "bck";

	public static final String SIZE = "sz";

	public static final String STATUS = "sts";

	public static final String TIME_TAKEN = "tt";

	private static volatile AccountBackupRestore _instance = null;

	private static final String HIKE_PACKAGE_NAME = "com.bsb.hike";

	public static final String DATABASE_EXT = ".db";

	public static final String BACKUP = "backup";

	public static final String DATA = "data";

	private static final String[] dbNames = { DBConstants.CONVERSATIONS_DATABASE_NAME };

	private static final String[] resetTableNames = { DBConstants.STICKER_SHOP_TABLE, DBConstants.STICKER_CATEGORIES_TABLE };

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
		try
		{
			backupDB(backupToken);
			backupPrefs(backupToken);
			backupUserData();
		}
		catch (Exception e)
		{
			deleteTempDBFiles();
			deleteTempPrefFile();
			e.printStackTrace();
			result = false;
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

	private void backupDB(String backupToken) throws Exception {
		Logger.d(LOGTAG, "encrypting with key: " + backupToken);
		if (TextUtils.isEmpty(backupToken)) {
			throw new Exception("Backup Token is empty");
		}

		if (isAnyDBCorrupt()) {
			Logger.e(LOGTAG, "Found corrupt DBs. Skipping backup!");
			return;
		}

		for (String fileName : dbNames) {

			File dbCopy = exportDatabse(fileName);
			if (dbCopy == null || !dbCopy.exists())
			{
				throw new Exception("Backup file " + dbCopy + " is missing");
			}
			File backup = getBackupFile(dbCopy.getName());
			CBCEncryption.encryptFile(dbCopy, backup, backupToken);
			dbCopy.delete();
		}
	}

	private boolean isAnyDBCorrupt() {
		boolean isDbCorrupt = false;
		for (String fileName : dbNames) {
			SQLiteDatabase db = null;
			try {
				String currentDBPath = mContext.getDatabasePath(fileName).getPath();
				db = SQLiteDatabase.openDatabase(currentDBPath, null, SQLiteDatabase.OPEN_READONLY);
				if (!db.isDatabaseIntegrityOk()) {
					isDbCorrupt = true;
					Logger.e(LOGTAG, "Found corrupt database - " + fileName);
					break;
				}
			} catch (SQLiteDatabaseCorruptException e) {
				Logger.e(LOGTAG, "Found corrupt database - " + fileName);
				isDbCorrupt = true;
				break;
			} finally {
				if (db != null) {
					db.close();
				}
			}
		}
		return isDbCorrupt;
	}

	private void backupPrefs(String backupToken) throws Exception
	{
		PreferenceBackup prefBackup = new PreferenceBackup();
		String prefBackupString = prefBackup.takeBackup().serialize();
		File prefFile = prefBackup.getPrefFile();
		writeToFile(prefBackupString, prefFile);
		File prefFileBackup = getBackupFile(prefFile.getName());
		CBCEncryption.encryptFile(prefFile, prefFileBackup, backupToken);
		prefFile.delete();
	}

	private void backupUserData() throws Exception
	{
		BackupMetadata backupMetadata = new BackupMetadata(mContext);
		String dataString = backupMetadata.toString();
		File userDataFile = getMetadataFile();
		writeToFile(dataString, userDataFile);
	}

	private BackupMetadata getBackupMetadata() {
		BackupMetadata userData;
		try
		{
			File userDataFile = getMetadataFile();
			String userDataString = readStringFromFile(userDataFile);
			userData = new BackupMetadata(mContext, userDataString);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return userData;
	}

	private void writeToFile(String text, File file) throws IOException
	{
		FileWriter fileWriter = new FileWriter(file);
		try
		{
			fileWriter.write(text);
			fileWriter.close();
		}
		finally
		{
			closeChannelsAndStreams(fileWriter);
		}
	}

	private String readStringFromFile(File file) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(file));
		try
		{
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null)
			{
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		}
		finally
		{
			closeChannelsAndStreams(br);
		}
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
		long time = System.currentTimeMillis();
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
		Logger.d(LOGTAG, "DB Export complete!! in " + time / 1000 + "." + time % 1000 + "s");
		return dbCopy;
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
		boolean result = true;

		@RestoreErrorStates
		int successState = STATE_RESTORE_SUCCESS;

		String backupToken = getBackupToken();
		BackupState state = getBackupState();
		BackupMetadata backupMetadata = getBackupMetadata();

		if (state == null && backupMetadata == null)
		{
			successState = STATE_RESTORE_ERROR;
			result = false;
		}

		else if (!ContactManager.getInstance().isMyMsisdn(backupMetadata.getMsisdn()))
		{
			successState = STATE_MSISDN_MISMATCH;
			result = false;
		}
		else if (backupMetadata != null && !isBackupAppVersionCompatible(backupMetadata.getAppVersion()))
		{
			successState = STATE_INCOMPATIBLE_APP_VERSION;
			result = false;
		}
		else if (state != null && !isBackupDbVersionCompatible(state.getDBVersion()))
		{
			successState = STATE_INCOMPATIBLE_APP_VERSION;
			result = false;
		}

		if (result)
		{
			try
			{
				restoreDB(backupToken);
				if (state == null && backupMetadata != null)
				{
					restorePrefs(backupToken);
				}
			}
			catch (Exception e)
			{
				deleteTempDBFiles();
				e.printStackTrace();
				result = false;
				successState = STATE_RESTORE_ERROR;
			}
		}
		if (result)
		{
			if (state != null)
			{
				state.restorePrefs(mContext);
			}
			postRestoreSetup(state, backupMetadata);
		}

		else
		{
			BotUtils.initBots();
		}

		time = System.currentTimeMillis() - time;
		Logger.d(LOGTAG, "Restore " + result + " in " + time / 1000 + "." + time % 1000 + "s");
		recordLog(RESTORE_EVENT_KEY,result,time);
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
			Logger.d(LOGTAG,"Current App Deatils: " + (new BackupMetadata(mContext)).toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void restoreDB(String backupToken) throws Exception
	{
		Logger.d(LOGTAG, "decrypting with key: " + backupToken);
		if (TextUtils.isEmpty(backupToken))
		{
			throw new Exception("Backup Token is empty");
		}
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			File backup = getBackupFile(dbCopy.getName());
			CBCEncryption.decryptFile(backup, dbCopy, backupToken);
			importDatabase(dbCopy);
			dbCopy.delete();
		}
	}

	private void restorePrefs(String backupToken) throws Exception
	{
		PreferenceBackup prefBackup = new PreferenceBackup();
		File prefFile = prefBackup.getPrefFile();
		File prefFileBackup = getBackupFile(prefFile.getName());
		CBCEncryption.decryptFile(prefFileBackup, prefFile, backupToken);
		String prefBackupString = readStringFromFile(prefFile);
		prefBackup.restore(prefBackupString);
		prefFile.delete();
	}

	/**
	 * Replaces the current Application database file with the provided database file
	 * @param dbCopy
	 * 		The file to placed as the new database.
	 * @throws Exception
	 */
	private void importDatabase(File dbCopy) throws Exception
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
			Logger.d(LOGTAG, "copy fail");
			throw e;
		}
		finally
		{
			closeChannelsAndStreams(src, dst, in, out);
		}
		time = System.currentTimeMillis() - time;
		Logger.d(LOGTAG, "DB import complete!! in " + time / 1000 + "." + time % 1000 + "s");
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
				File backup = getBackupFile(dbCopy.getName());
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

	private void postRestoreSetup(BackupState state, BackupMetadata backupMetadata)
	{
		if (state != null && state.getDBVersion() < DBConstants.CONVERSATIONS_DATABASE_VERSION)
		{
			HikeConversationsDatabase.getInstance().reinitializeDB();
		}
		else if (backupMetadata != null)
		{
			HikeConversationsDatabase.getInstance().reinitializeDB();
		}
		for (String table : resetTableNames)
		{
			HikeConversationsDatabase.getInstance().clearTable(table);
		}
		HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1();
		BotUtils.postAccountRestoreSetup();
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
			File backup = getBackupFile(DBCopy.getName());
			if (!backup.exists())
				return false;
		}
		File prefBackupFile = getPrefBackupFile();
		File backupStateFile = getBackupStateFile();

		if(!prefBackupFile.exists() && !backupStateFile.exists())
		{
			return false;
		}
		if (getLastBackupTime() > 0)
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
	 * Deletes the temporary files.
	 */
	private void deleteTempDBFiles()
	{
		for (String fileName : dbNames)
		{
			File currentDB = getCurrentDBFile(fileName);
			File dbCopy = getDBCopyFile(currentDB.getName());
			dbCopy.delete();
		}
	}

	private void deleteTempPrefFile()
	{
		getTempPrefFile().delete();
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
			File backup = getBackupFile(dbCopy.getName());
			dbCopy.delete();
			backup.delete();
		}
		getBackupStateFile().delete();
		getPrefBackupFile().delete();
		getMetadataFile().delete();
		deleteTempDBFiles();
		deleteTempPrefFile();
	}

	private File getTempPrefFile()
	{
		PreferenceBackup prefBackup = new PreferenceBackup();
		return prefBackup.getPrefFile();
	}

	private File getPrefBackupFile()
	{
		return getBackupFile(getTempPrefFile().getName());
	}

	private File getCurrentDBFile(String dbName)
	{
		File data = Environment.getDataDirectory();
		String currentDBPath = "//data//" + HIKE_PACKAGE_NAME + "//databases//" + dbName;
		File currentDB = new File(data, currentDBPath);
		return currentDB;
	}

	private File getBackupFile(String name)
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
		boolean prefUpdated = true;
		try
		{
			backupPrefs(getBackupToken());
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
