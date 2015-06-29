package com.bsb.hike.media;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.provider.BaseColumns;

import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.HikeSharedFile;


public class SharedMediaCursorIterator implements Iterator<HikeSharedFile>
{

	Cursor cursor;
	String msisdn;
	int msgIdIndex;
	int groupParticipantColumn;
	int tsIndex;
	int isSentIndex;
	int metadataIndex;

	public SharedMediaCursorIterator(Cursor c, String msisdn)
	{
		this.cursor = c;
		msgIdIndex = cursor.getColumnIndex(BaseColumns._ID);
		groupParticipantColumn = cursor.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		tsIndex = cursor.getColumnIndex(DBConstants.TIMESTAMP);
		isSentIndex = cursor.getColumnIndex(DBConstants.IS_SENT);
		metadataIndex = cursor.getColumnIndex(DBConstants.MESSAGE_METADATA);
		this.msisdn = msisdn;
	}

	@Override
	public boolean hasNext()
	{
		return cursor.getPosition() != cursor.getCount()-1;
	}

	@Override
	public HikeSharedFile next()
	{
		if (cursor.moveToNext())
		{
			return getFromCursor(cursor);
		}
		return null;
	}
	
	public HikeSharedFile getFromCursor(Cursor cursor)
	{
		long msgId = cursor.getLong(msgIdIndex);
		long ts = cursor.getLong(tsIndex);
		boolean isSent = cursor.getInt(isSentIndex) != 0;
		String messageMetadata = cursor.getString(metadataIndex);
		String groupParticipantMsisdn = cursor.getString(groupParticipantColumn);

		HikeSharedFile hikeSharedFile;
		try
		{
			hikeSharedFile = new HikeSharedFile(new JSONObject(messageMetadata), isSent, msgId, msisdn, ts, groupParticipantMsisdn);
			return hikeSharedFile;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public HikeSharedFile getFromCursor(Cursor cursor, int position)
	{
		HikeSharedFile hikeSharedFile = null;
		if (cursor.moveToPosition(position))
		{
			return getFromCursor(cursor);
		}
		return hikeSharedFile;
	}
	
	@Override
	public void remove()
	{

	}
};
