package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bsb.hike.db.dbcommand.SetPragmaModeCommand;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.utils.Logger;
import com.hike.transporter.interfaces.IPersistanceInterface;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.TConstants.TDBConstants;

public class HikeMqttPersistence extends SQLiteOpenHelper
{
	private SQLiteDatabase mDb;

	private static HikeMqttPersistence hikeMqttPersistence;

	public static void init(Context context)
	{
		if (hikeMqttPersistence == null)
		{
			hikeMqttPersistence = new HikeMqttPersistence(context);
		}
	}

	public static HikeMqttPersistence getInstance()
	{
		return hikeMqttPersistence;
	}

	private HikeMqttPersistence(Context context)
	{
		super(context, DBConstants.HIKE_PERSISTENCE.DATABASE_NAME, null, DBConstants.HIKE_PERSISTENCE.DATABASE_VERSION);
		mDb = getWritableDatabase();
		SetPragmaModeCommand setPragmaModeCommand = new SetPragmaModeCommand(mDb);
		setPragmaModeCommand.execute();
	}

	public SQLiteDatabase getDb()
	{
		return mDb;
	}

	public void addSentMessage(HikePacket packet) throws MqttPersistenceException
	{
		InsertHelper ih = null;
		try
		{
			Logger.d("HikeMqttPersistence", "Persisting message data: " + new String(packet.getMessage()));
			ih = new InsertHelper(mDb, DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE);
			ih.prepareForReplace();
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE), packet.getMessage());
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID), packet.getMsgId());
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP), packet.getTimeStamp());
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE), packet.getPacketType());
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MSG_TRACK_ID), packet.getTrackId());
			ih.bind(ih.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MSG_MSG_TYPE), packet.getMsgType());
			long rowid = ih.execute();
			if (rowid < 0)
			{
				throw new MqttPersistenceException("Unable to persist message");
			}
			packet.setPacketId(rowid);
		}
		finally
		{
			if (ih != null)
			{
				ih.close();
			}
		}
	}

	@Override
	public void close()
	{
		mDb.close();
	}

	public List<HikePacket> getAllSentMessages()
	{
		Cursor c = mDb.query(DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE, new String[] { DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE, DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID, DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP, DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_ID, 
				DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE, DBConstants.HIKE_PERSISTENCE.MQTT_MSG_TRACK_ID, DBConstants.HIKE_PERSISTENCE.MQTT_MSG_MSG_TYPE }, null, null, null, null, DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP);
		try
		{
			List<HikePacket> vals = new ArrayList<HikePacket>(c.getCount());
			int dataIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE);
			int idIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID);
			int tsIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP);
			int packetIdIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_ID);
			int packetTypeIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE);
			int msgTrackIDIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MSG_TRACK_ID);
			int msgTypeIdx = c.getColumnIndex(DBConstants.HIKE_PERSISTENCE.MQTT_MSG_MSG_TYPE);
			
			while (c.moveToNext())
			{
				HikePacket hikePacket = new HikePacket(c.getBlob(dataIdx), c.getLong(idIdx),
						c.getLong(tsIdx), c.getLong(packetIdIdx), c.getInt(packetTypeIdx), c.getString(msgTrackIDIdx), c.getString(msgTypeIdx));
				vals.add(hikePacket);
			}

			return vals;
		}
		finally
		{
			c.close();
		}
	}
		
	public boolean isMessageSent(long mqttMsgId)
	{
		Cursor c = mDb.query(DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE, new String[] { DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID }, DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID + "=?", new String[] { Long.toString(mqttMsgId) }, null, null, null);
		try
		{
			int count = c.getCount();
			return (count == 0);
		}
		finally
		{
			c.close();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}

		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " ( " + DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID + " INTEGER,"
				+ DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE + " BLOB," + DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP + " INTEGER," +  DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE + " INTEGER," + 
				DBConstants.HIKE_PERSISTENCE.MQTT_MSG_TRACK_ID + " TEXT," + DBConstants.HIKE_PERSISTENCE.MQTT_MSG_MSG_TYPE + " TEXT) ";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.MQTT_MSG_ID_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID + ")";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP_INDEX + " ON " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + "(" + DBConstants.HIKE_PERSISTENCE.MQTT_TIME_STAMP + ")";
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if(oldVersion < 2)
		{
			String alter = "ALTER TABLE " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " ADD COLUMN " + DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE + " INTEGER";
			db.execSQL(alter);
		}
		
		//Both column are added for Instrumentation 
		if(oldVersion < 3)
		{
			String alter1 = "ALTER TABLE " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " ADD COLUMN " + DBConstants.HIKE_PERSISTENCE.MQTT_MSG_TRACK_ID + " TEXT";
			db.execSQL(alter1);
			
			String alter2 = "ALTER TABLE " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " ADD COLUMN " + DBConstants.HIKE_PERSISTENCE.MQTT_MSG_MSG_TYPE + " TEXT";
			db.execSQL(alter2);
		}
	}

	public void removeMessage(long msgId)
	{
		String[] bindArgs = new String[] { Long.toString(msgId) };
		int numRows = mDb.delete(DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE, DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID + "=?", bindArgs);
		Logger.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " with Msg ID: " + msgId);
	}
	
	public void removeMessage(int type)
	{
		String[] bindArgs = new String[] { Integer.toString(type) };
		int numRows = mDb.delete(DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE, DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_TYPE + "=?", bindArgs);
		Logger.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " with type: " + type);
	}
	
	public void removeMessages(List<Long> msgIds)
	{
		if(msgIds == null || msgIds.isEmpty())
		{
			Logger.e(HikeMqttPersistence.class.getSimpleName(), "removeMessages :: msgIds not present");
			return;
		}
		
		StringBuilder inSelection = new StringBuilder("("+msgIds.get(0));
		for (int i=0; i<msgIds.size(); i++)
		{
			inSelection.append("," + Long.toString(msgIds.get(i)));
		}
		inSelection.append(")");
		
		mDb.execSQL("DELETE FROM " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " WHERE " + DBConstants.HIKE_PERSISTENCE.MQTT_MESSAGE_ID + " IN "+ inSelection.toString());
		Logger.d("HikeMqttPersistence", "Removed "+" Rows from " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " with Msgs ID: " + inSelection.toString());
	}

	public void removeMessageForPacketId(long packetId)
	{
		String[] bindArgs = new String[] { Long.toString(packetId) };
		int numRows = mDb.delete(DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE, DBConstants.HIKE_PERSISTENCE.MQTT_PACKET_ID + "=?", bindArgs);
		Logger.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + DBConstants.HIKE_PERSISTENCE.MQTT_DATABASE_TABLE + " with Packet ID: " + packetId);
	}
}
