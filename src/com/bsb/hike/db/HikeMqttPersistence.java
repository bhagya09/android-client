package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bsb.hike.models.HikePacket;
import com.bsb.hike.utils.Logger;

public class HikeMqttPersistence extends SQLiteOpenHelper
{

	public static final String MQTT_DATABASE_NAME = "mqttpersistence";

	public static final int MQTT_DATABASE_VERSION = 3;

	public static final String MQTT_DATABASE_TABLE = "messages";

	public static final String MQTT_MESSAGE_ID = "msgId";

	public static final String MQTT_PACKET_ID = "mqttId";
	
	public static final String MQTT_PACKET_TYPE = "mqttType";

	public static final String MQTT_MESSAGE = "data";

	public static final String MQTT_MSG_ID_INDEX = "mqttMsgIdIndex";

	public static final String MQTT_TIME_STAMP = "mqttTimeStamp";

	//Added for Instrumentation
	public static final String MQTT_MSG_TRACK_ID = "mqttMsgTrackId";
	
	//Added for Instrumentation
	public static final String MQTT_MSG_MSG_TYPE = "mqttMsgMsgType";
	
	public static final String MQTT_TIME_STAMP_INDEX = "mqttTimeStampIndex";

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
		super(context, MQTT_DATABASE_NAME, null, MQTT_DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	public void addSentMessage(HikePacket packet) throws MqttPersistenceException
	{
		InsertHelper ih = null;
		try
		{
			Logger.d("HikeMqttPersistence", "Persisting message data: " + new String(packet.getMessage()));
			ih = new InsertHelper(mDb, MQTT_DATABASE_TABLE);
			ih.prepareForReplace();
			ih.bind(ih.getColumnIndex(MQTT_MESSAGE), packet.getMessage());
			ih.bind(ih.getColumnIndex(MQTT_MESSAGE_ID), packet.getMsgId());
			ih.bind(ih.getColumnIndex(MQTT_TIME_STAMP), packet.getTimeStamp());
			ih.bind(ih.getColumnIndex(MQTT_PACKET_TYPE), packet.getPacketType());
			ih.bind(ih.getColumnIndex(MQTT_MSG_TRACK_ID), packet.getTrackId());
			ih.bind(ih.getColumnIndex(MQTT_MSG_MSG_TYPE), packet.getMsgType());
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
		Cursor c = mDb.query(MQTT_DATABASE_TABLE, new String[] { MQTT_MESSAGE, MQTT_MESSAGE_ID, MQTT_TIME_STAMP, MQTT_PACKET_ID, MQTT_PACKET_TYPE, MQTT_MSG_TRACK_ID, MQTT_MSG_MSG_TYPE }, null, null, null, null, MQTT_TIME_STAMP);
		try
		{
			List<HikePacket> vals = new ArrayList<HikePacket>(c.getCount());
			int dataIdx = c.getColumnIndex(MQTT_MESSAGE);
			int idIdx = c.getColumnIndex(MQTT_MESSAGE_ID);
			int tsIdx = c.getColumnIndex(MQTT_TIME_STAMP);
			int packetIdIdx = c.getColumnIndex(MQTT_PACKET_ID);
			int packetTypeIdx = c.getColumnIndex(MQTT_PACKET_TYPE);
			int msgTrackIDIdx = c.getColumnIndex(MQTT_MSG_TRACK_ID);
			int msgTypeIdx = c.getColumnIndex(MQTT_MSG_MSG_TYPE);
			
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
		Cursor c = mDb.query(MQTT_DATABASE_TABLE, new String[] { MQTT_MESSAGE_ID }, MQTT_MESSAGE_ID + "=?", new String[] { Long.toString(mqttMsgId) }, null, null, null);
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

		String sql = "CREATE TABLE IF NOT EXISTS " + MQTT_DATABASE_TABLE + " ( " + MQTT_PACKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + MQTT_MESSAGE_ID + " INTEGER,"
				+ MQTT_MESSAGE + " BLOB," + MQTT_TIME_STAMP + " INTEGER," +  MQTT_PACKET_TYPE + " INTEGER," + 
				MQTT_MSG_TRACK_ID + " TEXT," + MQTT_MSG_MSG_TYPE + " TEXT) ";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + MQTT_MSG_ID_INDEX + " ON " + MQTT_DATABASE_TABLE + "(" + MQTT_MESSAGE_ID + ")";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + MQTT_TIME_STAMP_INDEX + " ON " + MQTT_DATABASE_TABLE + "(" + MQTT_TIME_STAMP + ")";
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if(oldVersion < 2)
		{
			String alter = "ALTER TABLE " + MQTT_DATABASE_TABLE + " ADD COLUMN " + MQTT_PACKET_TYPE + " INTEGER";
			db.execSQL(alter);
		}
		
		//Both column are added for Instrumentation 
		if(oldVersion < 3)
		{
			String alter1 = "ALTER TABLE " + MQTT_DATABASE_TABLE + " ADD COLUMN " + MQTT_MSG_TRACK_ID + " TEXT";
			db.execSQL(alter1);
			
			String alter2 = "ALTER TABLE " + MQTT_DATABASE_TABLE + " ADD COLUMN " + MQTT_MSG_MSG_TYPE + " TEXT";
			db.execSQL(alter2);
		}
	}

	public void removeMessage(long msgId)
	{
		String[] bindArgs = new String[] { Long.toString(msgId) };
		int numRows = mDb.delete(MQTT_DATABASE_TABLE, MQTT_MESSAGE_ID + "=?", bindArgs);
		Logger.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + MQTT_DATABASE_TABLE + " with Msg ID: " + msgId);
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
		
		mDb.execSQL("DELETE FROM " + MQTT_DATABASE_TABLE + " WHERE " + MQTT_MESSAGE_ID + " IN "+ inSelection.toString());
		Logger.d("HikeMqttPersistence", "Removed "+" Rows from " + MQTT_DATABASE_TABLE + " with Msgs ID: " + inSelection.toString());
	}

	public void removeMessageForPacketId(long packetId)
	{
		String[] bindArgs = new String[] { Long.toString(packetId) };
		int numRows = mDb.delete(MQTT_DATABASE_TABLE, MQTT_PACKET_ID + "=?", bindArgs);
		Logger.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + MQTT_DATABASE_TABLE + " with Packet ID: " + packetId);
	}
}
