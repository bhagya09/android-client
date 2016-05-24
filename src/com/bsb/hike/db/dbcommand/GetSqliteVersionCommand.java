package com.bsb.hike.db.dbcommand;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.utils.Logger;

/**
 * Created by sidharth on 22/03/16.
 */
public class GetSqliteVersionCommand implements ICommand<String>
{
    @Override
    public String execute()
    {
        String sqliteVersion = "";
        try
        {
            Cursor cursor = SQLiteDatabase.openOrCreateDatabase(":memory:", null).rawQuery("select sqlite_version() AS sqlite_version", null);

            while (cursor.moveToNext())
            {
                sqliteVersion += cursor.getString(0);
            }
            Logger.d(getClass().getSimpleName(), " sqlite version : " + sqliteVersion);
        }
        catch (Throwable th)
        {
            Logger.e(getClass().getSimpleName(), "Error in getting sqlite version ", th);
        }
        return sqliteVersion;
    }
}
