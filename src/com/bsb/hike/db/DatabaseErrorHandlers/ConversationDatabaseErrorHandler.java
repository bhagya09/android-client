package com.bsb.hike.db.DatabaseErrorHandlers;

import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * This error handler is passed to {@link com.bsb.hike.db.HikeConversationsDatabase}.
 * The purpose of this handler is to record the db corrupt even in shared prefs.
 * Created by piyush on 15/03/16.
 */
public class ConversationDatabaseErrorHandler extends CustomDatabaseErrorHandler
{
	@Override
	public void onDatabaseCorrupt(SQLiteDatabase dbObj)
	{
		// Save the corrupt state into prefs
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DB_CORRUPT, true);
		//Call super which will handle logging and deletion of the database
		super.onDatabaseCorrupt(dbObj);
	}
}
