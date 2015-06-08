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

/**
 * 
 * 	@author Deepak Malik
 *
 *	Contains Persistence related functions for Offline related messaging based on HikeMqttPersistence
 */
public class HikeOfflinePersistence 
{
	private static final String TAG = "HikeOfflinePersistence";

	private SQLiteDatabase mDb;
	
	private static HikeOfflinePersistence hikeOfflinePersistence;
	
	public static HikeOfflinePersistence getInstance()
	{
		if(hikeOfflinePersistence == null)
		{
			hikeOfflinePersistence = new HikeOfflinePersistence();
		}
		return hikeOfflinePersistence;
	}
	
	private HikeOfflinePersistence()
	{
		mDb = HikeMqttPersistence.getInstance().getDb();
	}

	public static void onCreate(SQLiteDatabase db) 
	{
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " ( " + DBConstants.HIKE_PERSISTENCE.OFFLINE_PACKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + " INTEGER,"
				+ DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE + " BLOB," + DBConstants.HIKE_PERSISTENCE.OFFLINE_TIME_STAMP + " INTEGER," +  DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + " TEXT," + 
				DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_TRACK_ID + " TEXT) ";
		db.execSQL(sql);
		Logger.d(TAG, "Creating Offline Persistence table in MQTT Persistence Table");
		
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_ID_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + ")";
		Logger.d(TAG, "Creating Offline Persistence table index on msgId");
		db.execSQL(sql);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		if (oldVersion < 4)
		{
			String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " ( " + DBConstants.HIKE_PERSISTENCE.OFFLINE_PACKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + " INTEGER,"
					+ DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE + " BLOB," + DBConstants.HIKE_PERSISTENCE.OFFLINE_TIME_STAMP + " INTEGER," +  DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + " TEXT," + 
					DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_TRACK_ID + " TEXT) ";
			db.execSQL(sql);
			Logger.d(TAG, "Creating Offline Persistence table in MQTT Persistence Table in OnUpgrade");
			
			sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_ID_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + ")";
			db.execSQL(sql);
			Logger.d(TAG, "Creating Offline Persistence table index on msgId in OnUpgrade");
		}
	}
	
	public void addSentMessage(OfflineHikePacket packet) throws MqttPersistenceException
	{
		ContentValues cv = new ContentValues();
		
		cv.put(DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID, packet.getMsgId());
		cv.put(DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE, packet.getMessage());
		cv.put(DBConstants.HIKE_PERSISTENCE.OFFLINE_TIME_STAMP, packet.getTimeStamp());
		cv.put(DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN, packet.getMsisdn());
		cv.put(DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_TRACK_ID, packet.getTrackId());
		
		long rowId = mDb.insertWithOnConflict(DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		if (rowId < 0)
		{
			throw new MqttPersistenceException("Unable to persist message");
		}
		packet.setPacketId(rowId);
	}
	
	public List<OfflineHikePacket> getAllSentMessages(String msisdn)
	{
		Cursor c = mDb.rawQuery("SELECT * FROM " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " WHERE " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + "='" + msisdn + "'", null);
		List<OfflineHikePacket> vals = null;
		try
		{
			vals = new ArrayList<OfflineHikePacket>(c.getCount());
			int dataIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE);
			int idIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID);
			int tsIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_TIME_STAMP);
			int packetMsisdnIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN);
			int packetIdIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_PACKET_ID);
			int msgTrackIDIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_TRACK_ID);
		
			while (c.moveToNext())
			{
				OfflineHikePacket hikePacket = new OfflineHikePacket(c.getBlob(dataIdx), c.getLong(idIdx),
						c.getLong(tsIdx), c.getString(packetMsisdnIdx), c.getLong(packetIdIdx), c.getString(msgTrackIDIdx));
				vals.add(hikePacket);
			}
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		finally
		{
			c.close();
		}
		return vals;
	}

	public void removeMessage(long msgId)
	{
		String[] bindArgs = new String[] { Long.toString(msgId) };
		int numRows = mDb.delete(DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE, DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + "=?", bindArgs);
		Logger.d("HikeOFFLINEPersistence", "Removed " + numRows + " Rows from " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " with Msg ID: " + msgId);
	}
	
	public void removeMessageForPacketId(long packetId)
	{
		String[] bindArgs = new String[] { Long.toString(packetId) };
		int numRows = mDb.delete(DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE, DBConstants.HIKE_PERSISTENCE.OFFLINE_PACKET_ID + "=?", bindArgs);
		Logger.d("HikeOFFLINEPersistence", "Removed " + numRows + " Rows from " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " with Packet ID: " + packetId);
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
		
		mDb.execSQL("DELETE FROM " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " WHERE " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE_ID + " IN "+ inSelection.toString());
		Logger.d("HikeOFFLINEPersistence", "Removed "+" Rows from " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " with Msgs ID: " + inSelection.toString());
	}
}
