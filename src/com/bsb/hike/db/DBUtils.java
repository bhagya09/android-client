package com.bsb.hike.db;

import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * Created by sidharth on 08/03/16.
 */
public class DBUtils
{
	public static void setPragmaJournalMode(SQLiteDatabase mDb)
	{
		String journalMode = null;
		try
		{
			int journalModeIndex = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.JOURNAL_MODE_INDEX, -1);
			if (journalModeIndex >= 0 && journalModeIndex < DBConstants.JOURNAL_MODE_ARRAY.length)
			{
				journalMode = DBConstants.JOURNAL_MODE_ARRAY[journalModeIndex];
				Logger.e("DBUtils", "Changing journal mode to " + journalMode);
				mDb.rawQuery("PRAGMA journal_mode = " + journalMode + ";", null);
			}
		}
		catch (Throwable th)
		{
			Logger.e("DBUtils", "Error in changing journal mode to " + journalMode, th);
		}
	}
}
