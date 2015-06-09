package com.bsb.hike.db;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.OfflineHikePacket;
import com.bsb.hike.utils.Logger;

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
		
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_ID_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + ")";
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
			
			sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSG_ID_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + ")";
			db.execSQL(sql);
			Logger.d(TAG, "Creating Offline Persistence table index on msgId in OnUpgrade");
		}
	}
	
	private void addSentMessage(OfflineHikePacket packet) throws MqttPersistenceException
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
	
	public List<JSONObject> getAllSentMessages(String msisdn)
	{
		Cursor c = mDb.rawQuery("SELECT "+ DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE +" FROM " + DBConstants.HIKE_PERSISTENCE.OFFLINE_DATABASE_TABLE + " WHERE " + DBConstants.HIKE_PERSISTENCE.OFFLINE_MSISDN + "='" + msisdn + "'", null);
		List<JSONObject> vals = null;
		try
		{
			vals = new ArrayList<JSONObject>(c.getCount());
			int dataIdx = c.getColumnIndexOrThrow(DBConstants.HIKE_PERSISTENCE.OFFLINE_MESSAGE);

			while (c.moveToNext())
			{
				try 
				{
					JSONObject jsonObject = new JSONObject(new String(c.getBlob(dataIdx), "UTF-8"));
					vals.add(jsonObject);
				}
				catch (UnsupportedEncodingException e) 
				{
					e.printStackTrace();
				} 
				catch (JSONException e) 
				{
					e.printStackTrace();
				}
				
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
	
	public void addMessage(JSONObject jsonObject)
	{
		long msgId = 0;
		String msisdn = null;
		try
		{
			msgId = jsonObject.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
			msisdn = jsonObject.getString(HikeConstants.TO);
			OfflineHikePacket hikePacket = new OfflineHikePacket(jsonObject.toString().getBytes("UTF-8"), msgId, System.currentTimeMillis() / 1000, msisdn);
			addSentMessage(hikePacket);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (MqttPersistenceException e)
		{
			e.printStackTrace();
		}

	}
}
