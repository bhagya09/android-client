package com.bsb.hike.db.DatabaseErrorHandlers;

import android.database.sqlite.SQLiteDatabase;

/**
 * Extend this class to define your own Database error handler. Add your custom functionality to {@link HikeDatabaseErrorHandler.onDatabaseCorrupt(SQLiteDatabase)} Created by
 * 
 * piyush on 04/01/16.
 */
public abstract class HikeDatabaseErrorHandler extends DefaultDatabaseErrorHandler
{
	@Override
	public void onCorruption(SQLiteDatabase dbObj)
	{
		onDatabaseCorrupt(dbObj);
		super.onCorruption(dbObj);
	}

	public abstract void onDatabaseCorrupt(SQLiteDatabase dbObj);
}
