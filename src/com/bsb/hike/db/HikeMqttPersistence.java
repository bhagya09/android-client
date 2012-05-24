package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bsb.hike.models.HikePacket;

public class HikeMqttPersistence extends SQLiteOpenHelper
{

	public static final String MQTT_DATABASE_NAME = "mqttpersistence";

	public static final int MQTT_DATABASE_VERSION = 1;

	public static final String MQTT_DATABASE_TABLE = "messages";

	public static final String MQTT_MESSAGE_ID = "msgId";

	public static final String MQTT_PACKET_ID = "mqttId";

	public static final String MQTT_MESSAGE = "data";

	public static final String MQTT_INDEX = "mqttIndex";

	private SQLiteDatabase mDb;

	public HikeMqttPersistence(Context context)
	{
		super(context, MQTT_DATABASE_NAME, null, MQTT_DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	public void addSentMessage(HikePacket packet) throws MqttPersistenceException
	{
		Log.d("HikeMqttPersistence", "Persisting message data: " + new String(packet.getMessage()));
		InsertHelper ih = new InsertHelper(mDb, MQTT_DATABASE_TABLE);
		ih.prepareForReplace();
		ih.bind(ih.getColumnIndex(MQTT_MESSAGE), packet.getMessage());
		ih.bind(ih.getColumnIndex(MQTT_MESSAGE_ID), packet.getMsgId());
		long rowid = ih.execute();
		if (rowid < 0)
		{
			throw new MqttPersistenceException("Unable to persist message");
		}
	}

	@Override
	public void close()
	{
		mDb.close();
	}

	public List<HikePacket> getAllSentMessages()
	{
		Cursor c = mDb.query(MQTT_DATABASE_TABLE, new String[]{MQTT_MESSAGE, MQTT_MESSAGE_ID}, null, null, null, null, null);
		try
		{
			List<HikePacket> vals = new ArrayList<HikePacket>(c.getCount());
			int dataIdx = c.getColumnIndex(MQTT_MESSAGE);
			int idIdx = c.getColumnIndex(MQTT_MESSAGE_ID);
			while (c.moveToNext())
			{
				vals.add(new HikePacket(c.getBlob(dataIdx), c.getLong(idIdx)));
			}

			mDb.delete(MQTT_DATABASE_TABLE, null, null);

			return vals;
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

		String sql = "CREATE TABLE IF NOT EXISTS " + MQTT_DATABASE_TABLE 
				+ " ( "
						+ MQTT_PACKET_ID +" INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ MQTT_MESSAGE_ID +" INTEGER,"
						+ MQTT_MESSAGE +" BLOB"
				+ " ) ";
		db.execSQL(sql);
		
		sql = "CREATE INDEX IF NOT EXISTS " + MQTT_INDEX
				+ " ON "
				+ MQTT_DATABASE_TABLE 
				+ "(" + MQTT_MESSAGE_ID + ")";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//do nothing
	}

	public HikePacket popMessage(int mqttId)
	{
		Cursor c = mDb.query(MQTT_DATABASE_TABLE, new String[]{MQTT_MESSAGE, MQTT_MESSAGE_ID}, 
				MQTT_PACKET_ID + "=?", new String[]{Integer.toString(mqttId)}, null, null, null);
		int dataIdx = c.getColumnIndex(MQTT_MESSAGE);
		int idIdx = c.getColumnIndex(MQTT_MESSAGE_ID);
		try
		{
			if (!c.moveToFirst())
			{
				return null;
			}
			return new HikePacket(c.getBlob(dataIdx), c.getLong(idIdx));
		}
		finally
		{
			c.close();
		}
	}

	public void removeMessage(long msgId)
	{
		String[] bindArgs = new String[] { Long.toString(msgId) };
		int numRows = mDb.delete(MQTT_DATABASE_TABLE, MQTT_MESSAGE_ID + "=?", bindArgs);
		Log.d("HikeMqttPersistence", "Removed " + numRows + " Rows from " + MQTT_DATABASE_TABLE + " with Msg ID: " + msgId);
	}
}
