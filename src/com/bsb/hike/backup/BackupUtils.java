package com.bsb.hike.backup;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.model.BackupMetadata;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class BackupUtils
{

	public static final String LOGTAG = BackupUtils.class.getSimpleName();

	public static final String HIKE_PACKAGE_NAME = "com.bsb.hike";

	public static final String TEMP_SUFFIX = "_temp";

	public static final String BACKUP = "backup";

	public static final String BACKUP_SUFFIX = "." + BACKUP;

	/**
	 * Creates a copy of the specified database of the application.
	 * @param databaseName
	 * 		The name of the database.
	 * @return
	 * 		The copy the database.
	 */
	public static File exportDatabse(String databaseName)
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
	 * Replaces the current Application database file with the provided database file
	 * @param dbCopy
	 * 		The file to placed as the new database.
	 * @throws Exception
	 */
	public static void importDatabase(File dbCopy) throws Exception
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

	/**
	 * Closes the closeables.
	 * @param closeables
	 * 		Set of the closeables to be closed.
	 */
	public static void closeChannelsAndStreams(Closeable... closeables)
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

    public static boolean isDBCorrupt(String dbName) {
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openDatabase(getCurrentDBFile(dbName).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
			if (!db.isDatabaseIntegrityOk()) {
				Logger.e(LOGTAG, "Found corrupt database - " + dbName + ". Exiting!");
				return true;
			}
		} catch (SQLiteDatabaseCorruptException e) {
			Logger.e(LOGTAG, "Found corrupt database - " + dbName + ". Exiting!");
			return true;
		} finally {
			if (db != null) {
				db.close();
			}
		}
		return false;
	}


	public static File getTempFile(File file)
	{
		return new File(file.getAbsolutePath() + TEMP_SUFFIX);
	}

	public static File getCurrentDBFile(String dbName)
	{
		File currentDB = HikeMessengerApp.getInstance().getDatabasePath(dbName);
		return currentDB;
	}

    public static File getBackupFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name + BACKUP_SUFFIX);
	}

    public static File getDBCopyFile(String name)
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, name);
	}

	public static File getBackupStateFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, BACKUP);
	}

	public static void writeToFile(String text, File file) throws IOException
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

	public static String readStringFromFile(File file) throws IOException
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

	public static boolean isSharedPrefFile(String prefName)
	{
		if (HikeMessengerApp.ACCOUNT_SETTINGS.equals(prefName)
				|| HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF.equals(prefName)
				|| HikeMessengerApp.DEFAULT_SETTINGS_PREF.equals(prefName))
		{
			return true;
		}
		return false;
	}

	public static void backupUserData() throws Exception
	{
		BackupMetadata backupMetadata = new BackupMetadata(HikeMessengerApp.getInstance().getApplicationContext());
		String dataString = backupMetadata.toString();
		File userDataFile = getMetadataFile();
		BackupUtils.writeToFile(dataString, userDataFile);
	}

	public static BackupMetadata getBackupMetadata()
	{
		BackupMetadata userData;
		try
		{
			File userDataFile = getMetadataFile();
			String userDataString = BackupUtils.readStringFromFile(userDataFile);
			userData = new BackupMetadata(HikeMessengerApp.getInstance().getApplicationContext(), userDataString);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return userData;
	}

	public static File getMetadataFile()
	{
		new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT).mkdirs();
		return new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT, AccountBackupRestore.DATA);
	}

	public static boolean isDeviceDpiDifferent()
	{
		BackupMetadata metadata = BackupUtils.getBackupMetadata();

		if (metadata != null)
		{
			if (metadata.getDensityDPI() == BackupMetadata.NO_DPI)
			{
				return false; // No DPI presently recorded so return. Perhaps checking an old backup file
			}

			else if (metadata.getDensityDPI() != Utils.getDeviceDensityDPI())
			{
				return true;
			}
		}

		return false;
	}
}
