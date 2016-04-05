package com.bsb.hike.db.DatabaseErrorHandlers;

import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikeAlarmManager;
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

		//Set an alarm to play a notif, since we"ll be disconnecting the user from MQ the next time he opens the app or if he loses connectivity on his/her own
		Long alarmTime = System.currentTimeMillis() + (1000 * 60 * 10); // (Current time + 10 minutes)
		HikeAlarmManager.setAlarm(HikeMessengerApp.getInstance().getApplicationContext(), alarmTime, HikeAlarmManager.REQUESTCODE_SHOW_CORRUPT_DB_NOTIF, false);
		// TODO Should this alarm be persistent in case phone gets switched off ?
		//Fire PubSub
		HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.DB_CORRUPT, null);
		//Call super which will handle logging and deletion of the database
		super.onDatabaseCorrupt(dbObj);
	}
}
