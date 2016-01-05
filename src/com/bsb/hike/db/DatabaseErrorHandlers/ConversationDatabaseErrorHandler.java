package com.bsb.hike.db.DatabaseErrorHandlers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.db.DBConstants;
import com.bsb.hike.utils.Logger;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;

import java.io.File;

/**
 * Created by piyush on 04/01/16.
 */
public class ConversationDatabaseErrorHandler extends HikeDatabaseErrorHandler
{
	private Context context;

	public ConversationDatabaseErrorHandler(Context context)
	{
		this.context = context;
	}

	private static final String TAG = "ConversationDatabaseErrorHandler";

	@Override
	public void onDatabaseCorrupt(SQLiteDatabase dbObj)
	{
		Bugsnag.init(context.getApplicationContext());
		Logger.e(TAG, "Conv Db is corrupt");
		MetaData metaData = new MetaData();
		if (dbObj.isOpen())
		{
			metaData.addToTab("User", "MaxSize", String.valueOf(dbObj.getMaximumSize()));
			metaData.addToTab("User", "DBVersion",String.valueOf( dbObj.getVersion()));
		}
		metaData.addToTab("User", "DBSize", (new File(dbObj.getPath())).length());
		Bugsnag.notify(new Exception("Conversation Database Corrupt"), metaData);
	}

}
