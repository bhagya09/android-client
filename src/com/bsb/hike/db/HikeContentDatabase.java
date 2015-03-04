package com.bsb.hike.db;

import java.net.URISyntaxException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DBConstants.HIKE_CONTENT;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.Logger;

public class HikeContentDatabase extends SQLiteOpenHelper implements DBConstants,HIKE_CONTENT{

	private static HikeContentDatabase hikeContentDatabase;
	SQLiteDatabase mDB;
	private HikeContentDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mDB = getWritableDatabase();
	}
	
	public static  HikeContentDatabase getInstance(Context context){
		if(hikeContentDatabase==null){
			hikeContentDatabase = new HikeContentDatabase(context);
		}
		return hikeContentDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		mDB = db;
		String[] createQueries = getCreateQueries();
		for(String create:createQueries){
			db.execSQL(create);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		mDB = db;
		// CREATE all tables, it is possible that few tables are created in this version
		onCreate(mDB);
		String [] updateQueries = getUpdateQueries(oldVersion, newVersion);
		for(String update : updateQueries){
			db.execSQL(update);
		}
		
		// DO any other update operation here
	}
	
	private String[] getCreateQueries(){
		String[] createAndIndexes = new String[3];
		int i=0;
		// CREATE TABLE
		//CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		String contentTable = CREATE_TABLE +CONTENT_TABLE
				+ "("
				+_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ CONTENT_ID+" INTEGER UNIQUE, "
				+ LOVE_ID+ " INTEGER, "
				+CHANNEL_ID+" INTEGER, "
				+HIKE_CONTENT.TIMESTAMP+" INTEGER, "
				+METADATA+" TEXT"
				+")";
		
		createAndIndexes[i++] = contentTable;
		//CREATE TABLE 
		
		//ALARM TABLE->id,time,willWakeCpu,time,intent
		
		String alarmTable = CREATE_TABLE + ALARM_MGR_TABLE 
				+ "("
				+ _ID + " INTEGER PRIMARY KEY, "
				+ TIME + " TEXT, "
				+ WILL_WAKE_CPU + " INTEGER, "
				+ INTENT + " TEXT," 
				+ HIKE_CONV_DB.TIMESTAMP + " INTEGER" + ")";
		createAndIndexes[i++]=alarmTable;
		
		
		// APP_ALARM TABLE - > id, data
		String contentIndex = CREATE_INDEX + CONTENT_ID_INDEX + " ON "+CONTENT_TABLE +" ("+CONTENT_ID+")";
		
		
		createAndIndexes[i++] = contentIndex;
		// INDEX ENDS HERE
		
		return createAndIndexes;
	}
	
	private String[] getUpdateQueries(int oldVersion,int newVersion){
		String[] updateAndIndexes = new String[0];
		// UPDATE  TABLE
		
		//UPDATE INDEXES
		return updateAndIndexes;
	}
	
	public void insertIntoAlarmManagerDB(long time, int requestCode, boolean WillWakeCPU, Intent intent)
	{
		ContentValues cv = new ContentValues();
		cv.put(_ID, requestCode);
		cv.put(TIME, time + "");
		cv.put(WILL_WAKE_CPU, WillWakeCPU);
		cv.put(INTENT, intent.toUri(0));

		mDB.insertWithOnConflict(ALARM_MGR_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void deleteFromAlarmManagerDB(int requestCode)
	{
		mDB.delete(ALARM_MGR_TABLE, _ID + "=" + requestCode, null);
	}

	public void rePopulateAlarmWhenClosed()
	{
		Logger.d(HikeAlarmManager.TAG, "Populating alarm started");
		String selectQuery = "SELECT  * FROM " + ALARM_MGR_TABLE;

		Cursor cursor = mDB.rawQuery(selectQuery, null);
		try
		{
			if (cursor.moveToFirst())
			{
				do
				{
					Logger.d(HikeAlarmManager.TAG, "rePopulating  Alarms");
					int requestCode = cursor.getInt(cursor.getColumnIndex(_ID));
					long time = Long.parseLong(cursor.getString(cursor.getColumnIndex(TIME)));
					int willWakeCpu = cursor.getInt(cursor.getColumnIndex(WILL_WAKE_CPU));
					String intent = cursor.getString(cursor.getColumnIndex(INTENT));
					Uri asd = Uri.parse(intent);

					Intent intentAlarm = Intent.getIntent(asd.toString());

					HikeAlarmManager.setAlarmWithIntent(HikeMessengerApp.getInstance(), time, requestCode, (willWakeCpu != 0), intentAlarm);

				}
				while (cursor.moveToNext());
			}
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
