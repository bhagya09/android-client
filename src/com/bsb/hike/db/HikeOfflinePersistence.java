package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.OfflineHikePacket;
import com.bsb.hike.utils.Logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HikeOfflinePersistence extends SQLiteOpenHelper 
{
	public static final String MQTT_DATABASE_NAME = "mqttpersistence";

	public static final int OFFLINE_DATABASE_VERSION = 3;

	public static final String OFFLINE_DATABASE_TABLE = "offlineMessages";

	public static final String OFFLINE_MESSAGE_ID = "msgId";
	
	public static final String OFFLINE_MESSAGE = "data";
	
	public static final String OFFLINE_TIME_STAMP = "offlineTimeStamp";

	public static final String OFFLINE_MSISDN = "offlineMsisdn";
	
	public static final String OFFLINE_PACKET_ID = "offlineId";

	//Added for Instrumentation
	public static final String OFFLINE_MSG_TRACK_ID = "offlineMsgTrackId";
	
	public static final String OFFLINE_MSG_ID_INDEX = "offlineMsgIdIndex";
	
	public static final String OFFLINE_TIME_STAMP_INDEX = "offlineTimeStampIndex";
	
	private SQLiteDatabase mDb;
	
	private static final HikeOfflinePersistence hikeOfflinePersistence = new HikeOfflinePersistence();
	
	public static HikeOfflinePersistence getInstance()
	{
		return hikeOfflinePersistence;
	}
	
	private HikeOfflinePersistence()
	{
		super(HikeMessengerApp.getInstance().getApplicationContext(), MQTT_DATABASE_NAME, null, OFFLINE_DATABASE_VERSION);
		mDb = HikeMqttPersistence.getInstance().getDb();
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		if (db == null)
		{
			db = mDb;
		}

		String sql = "CREATE TABLE IF NOT EXISTS " + OFFLINE_DATABASE_TABLE + " ( " + OFFLINE_PACKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + OFFLINE_MESSAGE_ID + " INTEGER,"
				+ OFFLINE_MESSAGE + " BLOB," + OFFLINE_TIME_STAMP + " INTEGER," +  OFFLINE_MSISDN + " TEXT," + 
				OFFLINE_MSG_TRACK_ID + " TEXT) ";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + OFFLINE_MSG_ID_INDEX + " ON " + OFFLINE_DATABASE_TABLE + "(" + OFFLINE_MESSAGE_ID + ")";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// TODO Auto-generated method stub
		
	}
	
	public void addSentMessage(OfflineHikePacket packet) throws MqttPersistenceException
	{
		ContentValues cv = new ContentValues();
		
		cv.put(OFFLINE_MESSAGE_ID, packet.getMsgId());
		cv.put(OFFLINE_MESSAGE, packet.getMessage());
		cv.put(OFFLINE_TIME_STAMP, packet.getTimeStamp());
		cv.put(OFFLINE_MSISDN, packet.getMsisdn());
		cv.put(OFFLINE_MSG_TRACK_ID, packet.getTrackId());
		
		long rowId = mDb.insertWithOnConflict(OFFLINE_DATABASE_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		if (rowId < 0)
		{
			throw new MqttPersistenceException("Unable to persist message");
		}
		packet.setPacketId(rowId);
	}
	
	public List<OfflineHikePacket> getAllSentMessages(String msisdn)
	{
		Cursor c = mDb.rawQuery("SELECT * FROM " + OFFLINE_DATABASE_TABLE + " WHERE offlineMsisdn='" + msisdn + "'", null);
		try
		{
			List<OfflineHikePacket> vals = new ArrayList<OfflineHikePacket>(c.getCount());
			int dataIdx = c.getColumnIndex(OFFLINE_MESSAGE);
			int idIdx = c.getColumnIndex(OFFLINE_MESSAGE_ID);
			int tsIdx = c.getColumnIndex(OFFLINE_TIME_STAMP);
			int packetMsisdnIdx = c.getColumnIndex(OFFLINE_MSISDN);
			int packetIdIdx = c.getColumnIndex(OFFLINE_PACKET_ID);
			int msgTrackIDIdx = c.getColumnIndex(OFFLINE_MSG_TRACK_ID);
			
			while (c.moveToNext())
			{
				OfflineHikePacket hikePacket = new OfflineHikePacket(c.getBlob(dataIdx), c.getLong(idIdx),
						c.getLong(tsIdx), c.getString(packetMsisdnIdx), c.getLong(packetIdIdx), c.getString(msgTrackIDIdx));
				vals.add(hikePacket);
			}

			return vals;
		}
		finally
		{
			c.close();
		}
	}

	public void removeMessage(long msgId)
	{
		String[] bindArgs = new String[] { Long.toString(msgId) };
		int numRows = mDb.delete(OFFLINE_DATABASE_TABLE, OFFLINE_MESSAGE_ID + "=?", bindArgs);
		Logger.d("HikeOFFLINEPersistence", "Removed " + numRows + " Rows from " + OFFLINE_DATABASE_TABLE + " with Msg ID: " + msgId);
	}
	
	public void removeMessageForPacketId(long packetId)
	{
		String[] bindArgs = new String[] { Long.toString(packetId) };
		int numRows = mDb.delete(OFFLINE_DATABASE_TABLE, OFFLINE_PACKET_ID + "=?", bindArgs);
		Logger.d("HikeOFFLINEPersistence", "Removed " + numRows + " Rows from " + OFFLINE_DATABASE_TABLE + " with Packet ID: " + packetId);
	}
	
	public void removeMessages(ArrayList<Long> msgIds)
	{
		if(msgIds.isEmpty())
		{
			return;
		}
		StringBuilder inSelection = new StringBuilder("("+msgIds.get(0));
		for (int i=0; i<msgIds.size(); i++)
		{
			inSelection.append("," + Long.toString(msgIds.get(i)));
		}
		inSelection.append(")");
		
		mDb.execSQL("DELETE FROM " + OFFLINE_DATABASE_TABLE + " WHERE " + OFFLINE_MESSAGE_ID + " IN "+ inSelection.toString());
		Logger.d("HikeOFFLINEPersistence", "Removed "+" Rows from " + OFFLINE_DATABASE_TABLE + " with Msgs ID: " + inSelection.toString());
	}

}
