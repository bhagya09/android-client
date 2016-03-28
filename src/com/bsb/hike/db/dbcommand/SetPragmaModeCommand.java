package com.bsb.hike.db.dbcommand;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * Created by sidharth on 22/03/16.
 */
public class SetPragmaModeCommand implements ICommand<Void>
{
	private SQLiteDatabase mDb;

	public SetPragmaModeCommand(SQLiteDatabase mDb)
	{
		this.mDb = mDb;
	}

	@Override
	public Void execute()
	{
		String journalMode = null;
		Cursor c = null;
		try
		{
			int journalModeIndex = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.JOURNAL_MODE_INDEX, -1);
			if (journalModeIndex >= 0 && journalModeIndex < DBConstants.JOURNAL_MODE_ARRAY.length)
			{
				journalMode = DBConstants.JOURNAL_MODE_ARRAY[journalModeIndex];
				Logger.e(getClass().getSimpleName(), "Changing journal mode to " + journalMode);
				/**
				 * Sqlite doesn't allow running PRAGMA calls using SqliteDatabase execSql() method it throws eexception android.database.sqlite.SQLiteException: unknown error (code
				 * 0): Queries can be performed using SQLiteDatabase query or rawQuery methods only. Therefore using rawQuery and calling c.moveToFirst() (We can call any cursor
				 * method here)
				 */
				c = mDb.rawQuery("PRAGMA journal_mode = " + journalMode + ";", null);
				c.moveToFirst();
			}
		}
		catch (Throwable th)
		{
			Logger.e(getClass().getSimpleName(), "Error in changing journal mode to " + journalMode, th);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return null;
	}
}
