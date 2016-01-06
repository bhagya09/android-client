package com.bsb.hike.db.DatabaseErrorHandlers;

import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by piyush on 04/01/16.
 */
public class ConversationDatabaseErrorHandler extends HikeDatabaseErrorHandler
{
	private static final String TAG = "ConversationDatabaseErrorHandler";

	@Override
	public void onDatabaseCorrupt(SQLiteDatabase dbObj)
	{
		Logger.e(TAG, "Conv Db is corrupt");

		Utils.recordDatabaseCorrupt(dbObj);
		// TODO Do a backup restore here
	}
}
