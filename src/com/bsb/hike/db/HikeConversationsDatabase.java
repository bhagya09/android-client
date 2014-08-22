package com.bsb.hike.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper
{

	private SQLiteDatabase mDb;

	private static HikeConversationsDatabase hikeConversationsDatabase;

	private static Context mContext;

	public static void init(Context context)
	{
		if (hikeConversationsDatabase == null)
		{
			mContext = context;
			hikeConversationsDatabase = new HikeConversationsDatabase(context);
		}
	}

	public static HikeConversationsDatabase getInstance()
	{
		return hikeConversationsDatabase;
	}

	private HikeConversationsDatabase(Context context)
	{
		super(context, DBConstants.CONVERSATIONS_DATABASE_NAME, null, DBConstants.CONVERSATIONS_DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE + " STRING, " + DBConstants.MSG_STATUS + " INTEGER, " /*
																																									 * this is to
																																									 * check if msg
																																									 * sent or
																																									 * recieved of
																																									 * the msg sent.
																																									 */
				+ DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DBConstants.MAPPED_MSG_ID + " INTEGER, "
				+ DBConstants.CONV_ID + " INTEGER," + DBConstants.MESSAGE_METADATA + " TEXT, " + DBConstants.GROUP_PARTICIPANT + " TEXT, " + DBConstants.IS_HIKE_MESSAGE
				+ " INTEGER DEFAULT -1, " + DBConstants.READ_BY + " TEXT, " + DBConstants.MSISDN + " TEXT, " + DBConstants.MESSAGE_HASH + " TEXT DEFAULT NULL, " + DBConstants.MESSAGE_TYPE + " INTEGER" + " INTEGER DEFAULT -1" + " ) ";

		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CONVERSATION_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.CONV_ID + " , " + DBConstants.TIMESTAMP
				+ " DESC" + " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.MESSAGE_HASH_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_HASH + " DESC" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CONVERSATIONS_TABLE + " ( " + DBConstants.CONV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DBConstants.ONHIKE
				+ " INTEGER, " + DBConstants.CONTACT_ID + " STRING, " + DBConstants.MSISDN + " UNIQUE, " + DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MESSAGE
				+ " STRING, " + DBConstants.MSG_STATUS + " INTEGER, " + DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.MESSAGE_ID + " INTEGER, " + DBConstants.MAPPED_MSG_ID
				+ " INTEGER, " + DBConstants.MESSAGE_METADATA + " TEXT, " + DBConstants.GROUP_PARTICIPANT + " TEXT, " + DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0, "
				+ DBConstants.UNREAD_COUNT + " INTEGER DEFAULT 0, " + DBConstants.IS_STEALTH + " INTEGER DEFAULT 0, " + DBConstants.CONVERSATION_METADATA + " TEXT" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + " STRING, " + DBConstants.MSISDN + " TEXT, " + DBConstants.NAME
				+ " TEXT, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.HAS_LEFT + " INTEGER, " + DBConstants.ON_DND + " INTEGER, " + DBConstants.SHOWN_STATUS + " INTEGER "
				+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN
				+ " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_INFO_TABLE + " ( " + DBConstants.GROUP_ID + " STRING PRIMARY KEY, " + DBConstants.GROUP_NAME + " TEXT, "
				+ DBConstants.GROUP_OWNER + " TEXT, " + DBConstants.GROUP_ALIVE + " INTEGER, " + DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0, " + DBConstants.READ_BY + " TEXT, "
				+ DBConstants.MESSAGE_ID + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, " + DBConstants.LAST_USED + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX + " ON " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.STATUS_TABLE + " (" + DBConstants.STATUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DBConstants.STATUS_MAPPED_ID
				+ " TEXT UNIQUE, " + DBConstants.MSISDN + " TEXT, " + DBConstants.STATUS_TEXT + " TEXT, " + DBConstants.STATUS_TYPE + " INTEGER, " + DBConstants.TIMESTAMP
				+ " INTEGER, " + DBConstants.MESSAGE_ID + " INTEGER DEFAULT 0, " + DBConstants.SHOW_IN_TIMELINE + " INTEGER, " + DBConstants.MOOD_ID + " INTEGER, "
				+ DBConstants.TIME_OF_DAY + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.STATUS_INDEX + " ON " + DBConstants.STATUS_TABLE + " ( " + DBConstants.MSISDN + " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKERS_TABLE + " (" + DBConstants.CATEGORY_ID + " TEXT PRIMARY KEY, " + DBConstants.TOTAL_NUMBER + " INTEGER, "
				+ DBConstants.REACHED_END + " INTEGER," + DBConstants.UPDATE_AVAILABLE + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.PROTIP_TABLE + " (" + DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DBConstants.PROTIP_MAPPED_ID
				+ " TEXT UNIQUE, " + DBConstants.HEADER + " TEXT, " + DBConstants.PROTIP_TEXT + " TEXT, " + DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.IMAGE_URL
				+ " TEXT, " + DBConstants.WAIT_TIME + " INTEGER, " + DBConstants.PROTIP_GAMING_DOWNLOAD_URL + " TEXT" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.SHARED_MEDIA_TABLE + " (" + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY, " + DBConstants.CONV_ID + " INTEGER" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " )";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_INDEX + " ON " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.BG_ID + " TEXT, " + DBConstants.TIMESTAMP
				+ " INTEGER" + ")";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CHAT_BG_INDEX + " ON " + DBConstants.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(sql);
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.CONVERSATIONS_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGES_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_INFO_TABLE, null, null);
		mDb.delete(DBConstants.EMOTICON_TABLE, null, null);
		mDb.delete(DBConstants.STATUS_TABLE, null, null);
		mDb.delete(DBConstants.STICKERS_TABLE, null, null);
		mDb.delete(DBConstants.PROTIP_TABLE, null, null);
		mDb.delete(DBConstants.SHARED_MEDIA_TABLE, null, null);
		mDb.delete(DBConstants.FILE_THUMBNAIL_TABLE, null, null);
		mDb.delete(DBConstants.CHAT_BG_TABLE, null, null);
	}

	@Override
	public void close()
	{
		super.close();
		mDb.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (db == null)
		{
			db = mDb;
		}

		if (oldVersion < 2)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.ONHIKE + " INTEGER";
			db.execSQL(alter);
		}

		if (oldVersion < 3)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.ON_DND + " INTEGER";
			db.execSQL(alter);
			alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.SHOWN_STATUS + " INTEGER";
			db.execSQL(alter);
		}

		// This is being done to change the column type of column "name" in the
		// group members table
		if (oldVersion < 4)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " RENAME TO " + "temp_table";

			String dropIndex = "DROP INDEX " + DBConstants.GROUP_INDEX;

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + " STRING, " + DBConstants.MSISDN + " TEXT, "
					+ DBConstants.NAME + " TEXT, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.HAS_LEFT + " INTEGER, " + DBConstants.ON_DND + " INTEGER, "
					+ DBConstants.SHOWN_STATUS + " INTEGER " + " )";

			String createIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", "
					+ DBConstants.MSISDN + " ) ";

			String insert = "INSERT INTO " + DBConstants.GROUP_MEMBERS_TABLE + " SELECT * FROM temp_table";

			String drop = "DROP TABLE temp_table";

			db.execSQL(alter);
			db.execSQL(dropIndex);
			db.execSQL(create);
			db.execSQL(createIndex);
			db.execSQL(insert);
			db.execSQL(drop);
		}

		// Creating Emotions Table and adding index
		if (oldVersion < 5)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, " + DBConstants.LAST_USED
					+ " INTEGER" + " )";
			db.execSQL(create);
			create = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX + " ON " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " ) ";
			db.execSQL(create);
		}

		// Add muteGroup column
		if (oldVersion < 6)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		/*
		 * We won't use the DB to manage the file name and key anymore. Instead we write this data to a file. So we write all the current data in the db to the file.
		 */
		if (oldVersion < 7)
		{
			Cursor table = null;
			Cursor c = null;
			try
			{
				/*
				 * Check if the table exists first.
				 */
				table = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[] { DBConstants.FILE_TABLE });
				/*
				 * Only do something if the table exists.
				 */
				if (table.moveToFirst())
				{

					c = db.query(DBConstants.FILE_TABLE, null, null, null, null, null, null);

					int fileNameIdx = c.getColumnIndex(DBConstants.FILE_NAME);
					int fileKeyIdx = c.getColumnIndex(DBConstants.FILE_KEY);

					JSONObject data = new JSONObject();

					while (c.moveToNext())
					{
						String fileName = c.getString(fileNameIdx);
						String fileKey = c.getString(fileKeyIdx);

						try
						{
							data.put(fileName, fileKey);
						}
						catch (JSONException e)
						{
							Logger.e(getClass().getSimpleName(), "Invalid values");
						}
					}
					Logger.d(getClass().getSimpleName(), "DB data: " + data.toString());
					Utils.makeNewFileWithExistingData(data);

					String drop = "DROP TABLE " + DBConstants.FILE_TABLE;
					db.execSQL(drop);
				}
			}
			finally
			{
				if (table != null)
				{
					table.close();
				}
				if (c != null)
				{
					c.close();
				}
			}
		}

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 8)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 9)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 10)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 11)
		// {
		// }

		/*
		 * Dropping the earlier status table to ensure the previous statuses (if any) are deleted.
		 */
		if (oldVersion < 12)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.STATUS_TABLE + " (" + DBConstants.STATUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ DBConstants.STATUS_MAPPED_ID + " TEXT UNIQUE, " + DBConstants.MSISDN + " TEXT, " + DBConstants.STATUS_TEXT + " TEXT, " + DBConstants.STATUS_TYPE
					+ " INTEGER, " + DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.MESSAGE_ID + " INTEGER DEFAULT 0, " + DBConstants.SHOW_IN_TIMELINE + " INTEGER, "
					+ DBConstants.MOOD_ID + " INTEGER, " + DBConstants.TIME_OF_DAY + " INTEGER" + " )";
			db.execSQL(create);

			String alter1 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MSG_STATUS + " INTEGER";
			String alter3 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.TIMESTAMP + " INTEGER";
			String alter4 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ID + " INTEGER";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MAPPED_MSG_ID + " INTEGER";
			String alter6 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_METADATA + " TEXT";
			String alter7 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.GROUP_PARTICIPANT + " TEXT";
			String alter8 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0";

			db.execSQL(create);
			db.execSQL(alter1);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			db.execSQL(alter6);
			db.execSQL(alter7);
			db.execSQL(alter8);

			denormaliseConversations(db);
		}

		/*
		 * Adding index on MSISDN in status table.
		 */
		if (oldVersion < 13)
		{
			String createIndex = "CREATE INDEX IF NOT EXISTS " + DBConstants.STATUS_INDEX + " ON " + DBConstants.STATUS_TABLE + " ( " + DBConstants.MSISDN + " ) ";
			db.execSQL(createIndex);
		}

		/*
		 * Adding a column to keep a track of the message type i.e hike or sms.
		 */
		if (oldVersion < 14)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.IS_HIKE_MESSAGE + " INTEGER DEFAULT -1";
			db.execSQL(alter);
		}

		/*
		 * Version 15 adds the sticker table.
		 */
		if (oldVersion < 15)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKERS_TABLE + " (" + DBConstants.CATEGORY_ID + " TEXT PRIMARY KEY, " + DBConstants.TOTAL_NUMBER
					+ " INTEGER, " + DBConstants.REACHED_END + " INTEGER," + DBConstants.UPDATE_AVAILABLE + " INTEGER" + " )";
			db.execSQL(create);
		}

		/*
		 * Version 16 adds the protips table.
		 */
		if (oldVersion < 16)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.PROTIP_TABLE + " (" + DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ DBConstants.PROTIP_MAPPED_ID + " TEXT UNIQUE, " + DBConstants.HEADER + " TEXT, " + DBConstants.PROTIP_TEXT + " TEXT, " + DBConstants.TIMESTAMP + " INTEGER, "
					+ DBConstants.IMAGE_URL + " TEXT, " + DBConstants.WAIT_TIME + " INTEGER" + " )";
			db.execSQL(create);
		}

		/*
		 * Version 17 add the unread column.
		 */
		if (oldVersion < 17)
		{
			String alter = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.UNREAD_COUNT + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		/*
		 * Version 18 adds the shared media and file thumbnail table. We also parse through all the messages to populate these tables.
		 */
		if (oldVersion < 18)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.SHARED_MEDIA_TABLE + " (" + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY, " + DBConstants.CONV_ID
					+ " INTEGER" + " )";
			db.execSQL(create);
			create = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " )";
			db.execSQL(create);
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 18
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, 1);
			editor.commit();
		}

		/*
		 * Version 19 adds the 'read by' column in the messages table.
		 */
		if (oldVersion < 19)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.READ_BY + " TEXT";
			db.execSQL(alter);
		}

		/*
		 * Version 20 adds an index for the file thumbnails table.
		 */
		if (oldVersion < 20)
		{
			String createIndex = "CREATE INDEX IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_INDEX + " ON " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " )";
			db.execSQL(createIndex);
		}

		/*
		 * Version 21 adds PROTIP_GAMING_DOWNLOAD_URL to the protip table
		 */
		if (oldVersion < 21)
		{
			String alter = "ALTER TABLE " + DBConstants.PROTIP_TABLE + " ADD COLUMN " + DBConstants.PROTIP_GAMING_DOWNLOAD_URL + " TEXT";
			db.execSQL(alter);
		}

		/*
		 * Version 22 adds the Chat BG table.
		 */
		if (oldVersion < 22)
		{
			String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.BG_ID + " TEXT)";
			db.execSQL(sql);
			sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CHAT_BG_INDEX + " ON " + DBConstants.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + ")";
			db.execSQL(sql);
		}

		/*
		 * Version 23 adds the timestamp column to the chat bg table
		 */
		if (oldVersion < 23)
		{
			String alter = "ALTER TABLE " + DBConstants.CHAT_BG_TABLE + " ADD COLUMN " + DBConstants.TIMESTAMP + " INTEGER";
			db.execSQL(alter);
		}

		/*
		 * Version 24 adds the stealth column to the converations table
		 */
		if (oldVersion < 24)
		{
			String alter = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.IS_STEALTH + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		// to delete duplicate stickers
		if (oldVersion < 26)
		{
			try
			{
				StickerManager st = StickerManager.getInstance();
				st.deleteDuplicateStickers();
			}
			catch (Exception e)
			{
			}
		}

		/*
		 * Version 27 adds the message hash column to the messages table Version 27 adds READ_BY and MESSAGE_ID column in groupInfo table
		 */
		if (oldVersion < 27)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MSISDN + " TEXT";
			String alter1 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_HASH + " TEXT DEFAULT NULL";
			String createIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.MESSAGE_HASH_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_HASH
					+ " DESC" + " )";
			String alter2 = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.READ_BY + " TEXT";
			String alter3 = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ID + " INTEGER";
			String alter4 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_TYPE + " INTEGER DEFAULT -1";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.CONVERSATION_METADATA + " TEXT";
			db.execSQL(alter);
			db.execSQL(alter1);
			db.execSQL(createIndex);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 27
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, 1);
			editor.commit();
		}
		
		/*
		 * Version 28 migrates msisdn entries to message table
		 */
		if (oldVersion < 28)
		{
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 28
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, 1);
			editor.commit();
		}
	}

	public int updateOnHikeStatus(String msisdn, boolean onHike)
	{
		Cursor conversationCursor = null;
		Cursor groupMemberCursor = null;
		try
		{
			String selection = DBConstants.MSISDN + "=?";
			String[] args = { msisdn };

			conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			groupMemberCursor = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			ContentValues values = new ContentValues();
			values.put(DBConstants.ONHIKE, onHike);

			int rowsUpdated = 0;

			if (conversationCursor.moveToFirst())
			{

				boolean prevOnHikeVal = conversationCursor.getInt(conversationCursor.getColumnIndex(DBConstants.ONHIKE)) == 1;

				if (prevOnHikeVal != onHike)
				{
					rowsUpdated += mDb.update(DBConstants.CONVERSATIONS_TABLE, values, selection, args);
				}
			}

			if (groupMemberCursor.moveToFirst())
			{

				boolean prevOnHikeVal = groupMemberCursor.getInt(groupMemberCursor.getColumnIndex(DBConstants.ONHIKE)) == 1;

				if (prevOnHikeVal != onHike)
				{
					rowsUpdated += mDb.update(DBConstants.GROUP_MEMBERS_TABLE, values, selection, args);
				}
			}

			return rowsUpdated;

		}
		finally
		{
			if (conversationCursor != null)
			{
				conversationCursor.close();
			}
			if (groupMemberCursor != null)
			{
				groupMemberCursor.close();
			}
		}
	}

	/**
	 * Adds single message to database
	 * @param message
	 * 			- message to be added to database
	 * @return result of {@link #addConversations(List)} function
	 */
	public boolean addConversationMessages(ConvMessage message)
	{
		List<ConvMessage> l = new ArrayList<ConvMessage>(1);
		l.add(message);
		return addConversations(l);
	}

	public int updateMsgStatus(long msgID, int val, String msisdn)
	{
		String initialWhereClause = DBConstants.MESSAGE_ID + " =" + String.valueOf(msgID);

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, val, msisdn);
	}

	public long[] setAllDeliveredMessagesReadForMsisdn(String msisdn, JSONArray msgIds)
	{
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.CONV_ID + " = (SELECT " + DBConstants.CONV_ID + " FROM "
				+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.MSISDN + "=? ) AND " + DBConstants.MSG_STATUS + "<" + State.SENT_DELIVERED_READ.ordinal(),
				new String[] { msisdn }, null, null, null);
		long[] ids = new long[c.getCount() + msgIds.length()];

		if (ids.length == 0 && msgIds.length() == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder("(");
		int i = 0;
		try
		{
			while (c.moveToNext())
			{
				long id = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				sb.append(id + ",");
				ids[i++] = id;
			}
			for (i = 0; i < msgIds.length(); i++)
			{
				long id = msgIds.optLong(i);
				sb.append(id + ",");
				ids[c.getCount() + i++] = id;
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		sb.replace(sb.lastIndexOf(","), sb.length(), ")");

		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + sb.toString();

		int status = State.SENT_DELIVERED_READ.ordinal();

		String query = initialWhereClause;

		executeUpdateMessageStatusStatement(query, status, msisdn);

		return ids;
	}

	/**
	 * 
	 * @param groupId
	 * 			-- groupId of group for which mr packet came
	 * @param ids
	 * 			-- list of ids present in mr packet
	 * @param msisdn
	 * 			-- partipant msisdn from which mr packet came
	 * @return id
	 * 			-- maxMsgId from list of ids that are sent by user. If ids doesn't contains any id sent by user return -1
	 */
	public long setReadByForGroup(String groupId, long[] ids, String msisdn)
	{
		Cursor c = null;
		Cursor conversationCursor = null;
		try
		{
			conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.CONV_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null, null,
					null);
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
					null, null, null);


			if (!conversationCursor.moveToFirst())
			{
				return -1;

			}
			long convId = conversationCursor.getInt(conversationCursor.getColumnIndex(DBConstants.CONV_ID));


			if (c.moveToFirst())
			{
				mDb.beginTransaction();
				String readByString = null;
				long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

				boolean idPresent = false;				// boolean to check whether the list of ids contains the latest sent message id

				for (int i = 0; i < ids.length; i++)
				{
					if (ids[i] == msgId)
					{
						idPresent = true;
						break;
					}
				}


				ContentValues contentValues = new ContentValues();

				
					if(idPresent)				// We have to update readbyString 
					{
						readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));

						JSONArray readByArray;
						try
						{
							if (TextUtils.isEmpty(readByString))
							{
								readByArray = new JSONArray();
							}
							else
							{
								readByArray = new JSONArray(readByString);
							}
						}
						catch (JSONException e)
						{
							Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
							readByArray = new JSONArray();
						}
						/*
						 * Checking if this number has already been added.
						 */
						boolean alreadyAdded = false;
						for (int i = 0; i < readByArray.length(); i++)
						{
							if (readByArray.optString(i).equals(msisdn))
							{
								alreadyAdded = true;
								break;
							}
						}
						if (!alreadyAdded)
						{
							readByArray.put(msisdn);
							contentValues.put(DBConstants.READ_BY, readByArray.toString());
							mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
						}	
					}
				

				long maxMsgId = getMrIdForGroup(groupId, ids);			// get max sent message id from list of ids

				if(maxMsgId > 0)
				{

					long conversationMsgId = conversationCursor.getLong(conversationCursor.getColumnIndex(DBConstants.MESSAGE_ID));
					int minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
					int maxStatusOrdinal = State.SENT_DELIVERED_READ.ordinal();


					/*
					 * Making sure we only set the status of sent messages.
					 */
					contentValues.clear();
					String whereClause = DBConstants.MESSAGE_ID + " <= " + maxMsgId + " AND " + DBConstants.MSG_STATUS + " > " + minStatusOrdinal + " AND "
							+ DBConstants.MSG_STATUS + " < " + maxStatusOrdinal + " AND " + DBConstants.CONV_ID + "=?";
					contentValues.put(DBConstants.MSG_STATUS, State.SENT_DELIVERED_READ.ordinal());
					mDb.update(DBConstants.MESSAGES_TABLE, contentValues, whereClause, new String[] { Long.toString(convId) });
					
					if(conversationMsgId == maxMsgId)
					{
						mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { groupId });
					}
				}
				
				mDb.setTransactionSuccessful();
				return maxMsgId;
			}
			
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}

			if (conversationCursor != null)
			{
				conversationCursor.close();
			}
			
			mDb.endTransaction();
		}
		return -1;
	}
	
	public void setReadByForGroupBulk(Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap)
	{

		long maxMsgId = -1;
		Cursor c = null;
		Cursor conversationCursor = null;
		for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet())		// Iterate through status map
		{

			String groupId = entry.getKey();
			PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
			maxMsgId = pair.getFirst().getFirst();
			
			if(maxMsgId == -1)
			{
				continue;
			}
			try
			{
				c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
						null, null, null);
				
				conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null, null,
						null);
				
				if (!conversationCursor.moveToFirst())
				{
					continue;
				}
				if (c.moveToFirst())
				{
					String readByString = null;
					long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

					if (msgId == maxMsgId)
					{
						readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));	
					}
					else
					{
						return;
					}
					
					JSONArray readByArray;
					try
					{
						if (TextUtils.isEmpty(readByString))
						{
							readByArray = new JSONArray();
						}
						else
						{
							readByArray = new JSONArray(readByString);
						}

					}
					catch (JSONException e)
					{
						Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
						readByArray = new JSONArray();
					}
					/*
					 * Checking if this number has already been added.
					 */
					boolean alreadyAdded = false;
					for(String msisdn : pair.getFirst().getSecond())
					{
						for (int i = 0; i < readByArray.length(); i++)
						{
							if (readByArray.optString(i).equals(msisdn))
							{
								alreadyAdded = true;
								break;
							}
						}
						if (!alreadyAdded)
						{
							readByArray.put(msisdn);
						}
					}

					long conversationMsgId = conversationCursor.getLong(conversationCursor.getColumnIndex(DBConstants.MESSAGE_ID));

					ContentValues contentValues = new ContentValues();
					if(conversationMsgId == maxMsgId)
					{
						contentValues.put(DBConstants.MSG_STATUS, State.SENT_DELIVERED_READ.ordinal());
						mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { groupId });
					}
					contentValues.clear();
					contentValues.put(DBConstants.READ_BY, readByArray.toString());
					mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });


				}
			}
			finally
			{
				if (c != null)
				{
					c.close();
				}
				if (conversationCursor != null)
				{
					conversationCursor.close();
				}
			}
		}
	}

	public void setMessageState(String msisdn, long msgId, int status)
	{
		if(msgId == -1)
		{
			return ;
		}
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (status <= State.SENT_DELIVERED_READ.ordinal())
		{
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = status;
		}
		else
		{
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = status;
		}
		Cursor c = null;
		long[] ids = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.CONV_ID + " = (SELECT " + DBConstants.CONV_ID + " FROM "
					+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.MSISDN + "=? ) AND " + DBConstants.MSG_STATUS + ">=" + minStatusOrdinal + " AND "
					+ DBConstants.MSG_STATUS + "<" + maxStatusOrdinal + " AND " + DBConstants.MESSAGE_ID + "<=" + msgId, new String[] { msisdn }, null, null, null);

			ids = new long[c.getCount()];
			int i = 0;
			while (c.moveToNext())
			{
				long id = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				ids[i++] = id;
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		if (ids != null)
		{
			updateBatch(ids, status, msisdn);
		}
	}

	public int updateBatch(long[] ids, int status, String msisdn)
	{
		StringBuilder sb = new StringBuilder("(");
		/* TODO make utils.join work for arrays */
		for (int i = 0; i < ids.length; i++)
		{
			sb.append(ids[i]);
			if (i != ids.length - 1)
			{
				sb.append(",");
			}
		}
		sb.append(")");

		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + sb.toString();

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, status, msisdn);
	}

	public void updateStatusBulk(Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap)
	{

		String msisdn;
		for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet())
		{

			msisdn = (String) entry.getKey();
			PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
			setMessageState(msisdn, pair.getFirst().getFirst(), State.SENT_DELIVERED_READ.ordinal());
			setMessageState(msisdn, pair.getSecond(), State.SENT_DELIVERED.ordinal());
		}
	}

	public int executeUpdateMessageStatusStatement(String updateStatement, int status, String msisdn)
	{
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (status <= State.SENT_DELIVERED_READ.ordinal())
		{
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = status;
		}
		else
		{
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = status;
		}

		updateStatement = updateStatement
				+ " AND "
				+ DBConstants.MSG_STATUS
				+ " >= "
				+ minStatusOrdinal
				+ " AND "
				+ DBConstants.MSG_STATUS
				+ " <= "
				+ maxStatusOrdinal
				+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.CONV_ID + "=(SELECT " + DBConstants.CONV_ID + " FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE "
						+ DBConstants.MSISDN + " =" + DatabaseUtils.sqlEscapeString(msisdn) + ")") : "");

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSG_STATUS, status);
		int numRows = mDb.update(DBConstants.MESSAGES_TABLE, contentValues, updateStatement, null);

		if (status == State.RECEIVED_READ.ordinal())
		{
			contentValues.put(DBConstants.UNREAD_COUNT, 0);
		}
		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, updateStatement, null);

		return numRows;
	}

	/**
	 * Extracts the thumbnail string from the metadata to save it in a different table. Returns this extracted string so that it can be set back in the metadata once the insertion
	 * has been done.
	 * 
	 * @param metadata
	 * @return
	 */
	private String extractThumbnailFromMetadata(MessageMetadata metadata)
	{
		if (metadata == null || metadata.getHikeFiles() == null)
		{
			return null;
		}
		try
		{
			HikeFile hikeFile = metadata.getHikeFiles().get(0);
			if (TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				return null;
			}

			JSONObject metadataJson = metadata.getJSON();
			JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
			JSONObject fileJson;

			fileJson = fileArray.getJSONObject(0);
			fileJson.remove(HikeConstants.THUMBNAIL);

			String thumbnailString = hikeFile.getThumbnailString();

			if (TextUtils.isEmpty(thumbnailString))
			{
				return null;
			}
			addFileThumbnail(hikeFile.getFileKey(), Base64.decode(thumbnailString, Base64.DEFAULT));

			return thumbnailString;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
			return null;
		}
	}

	private void addThumbnailStringToMetadata(MessageMetadata metadata, String thumbnailString)
	{
		if (TextUtils.isEmpty(thumbnailString))
		{
			return;
		}

		try
		{
			JSONObject metadataJson = metadata.getJSON();
			JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
			JSONObject fileJson;

			fileJson = fileArray.getJSONObject(0);
			fileJson.put(HikeConstants.THUMBNAIL, thumbnailString);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
		}
	}

	public void updateMessageMetadata(long msgID, MessageMetadata metadata)
	{
		String thumbnailString = extractThumbnailFromMetadata(metadata);

		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MESSAGE_METADATA, metadata.serialize());
		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { String.valueOf(msgID) });

		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MESSAGE_ID + "=? AND " + DBConstants.IS_STATUS_MSG + " = 0", new String[] { String.valueOf(msgID) });

		addThumbnailStringToMetadata(metadata, thumbnailString);
	}

	public void updateConversationMetadata(long convId, Conversation.MetaData metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.CONVERSATION_METADATA, metadata.toString());
		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { String.valueOf(convId) });
	}

	private void bindConversationInsert(SQLiteStatement insertStatement, ConvMessage conv)
	{
		final int messageColumn = 1;
		final int msgStatusColumn = 2;
		final int timestampColumn = 3;
		final int mappedMsgIdColumn = 4;
		final int messageMetadataColumn = 5;
		final int groupParticipant = 6;
		final int isHikeMessageColumn = 7;
		final int messageHash = 8;
		final int typeColumn = 9;
		final int msgMsisdnColumn = 10;
		final int msisdnColumn = 11;

		insertStatement.clearBindings();
		insertStatement.bindString(messageColumn, conv.getMessage());
		// 0 -> SENT_UNCONFIRMED ; 1 -> SENT_CONFIRMED ; 2 -> RECEIVED_UNREAD ;
		// 3 -> RECEIVED_READ
		insertStatement.bindLong(msgStatusColumn, conv.getState().ordinal());
		insertStatement.bindLong(timestampColumn, conv.getTimestamp());
		insertStatement.bindLong(mappedMsgIdColumn, conv.getMappedMsgID());
		insertStatement.bindString(msisdnColumn, conv.getMsisdn());
		insertStatement.bindString(messageMetadataColumn, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
		insertStatement.bindLong(isHikeMessageColumn, conv.isSMS() ? 0 : 1);
		insertStatement.bindString(groupParticipant, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");
		String msgHash = createMessageHash(conv);
		if (msgHash != null)
		{
			insertStatement.bindString(messageHash, msgHash);
		}
		insertStatement.bindLong(typeColumn, conv.getMessageType());
		insertStatement.bindString(msgMsisdnColumn, conv.getMsisdn());
	}

	public boolean wasMessageReceived(ConvMessage conv)
	{
		String msgHash = createMessageHash(conv);
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_HASH }, DBConstants.MESSAGES_TABLE + "."
					+ DBConstants.MESSAGE_HASH + "=?", new String[] { msgHash }, null, null, null);
			int count = c.getCount();
			return (count != 0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Creates the messages hash for the message object.
	 * @param msg
	 * 			The message for which hash is to be created.
	 * @return The message hash string .
	 */
	private String createMessageHash(ConvMessage msg)
	{
		String msgHash = null;
		if (!msg.isSent() && (msg.getParticipantInfoState() == ParticipantInfoState.NO_INFO))
		{
			msgHash = msg.getMsisdn() + msg.getMappedMsgID() + msg.getMessage().charAt(0) + msg.getMessage().charAt(msg.getMessage().length() - 1);
			Logger.d(getClass().getSimpleName(), "Message hash: " + msgHash);
		}
		return msgHash;
	}
	
	private String createMessageHash(String msisdn, long mappedMsgId, String message, long ts)
	{
		String msgHash = null;
		if (TextUtils.isEmpty(message))
		{
			msgHash = msisdn + mappedMsgId + ts;
		}
		else
		{
			msgHash = msisdn + mappedMsgId + message.charAt(0) + message.charAt(message.length() - 1) + ts;
		}
		Logger.d(getClass().getSimpleName(), "Message hash: " + msgHash);
		return msgHash;
	}
	/**
	 * 
	 * @param convMessages
	 * 			-- list of messages to be added to database
	 * @return
	 * 		   <li><b>true</b> if messages successfully added to database</li>
	 * 		   <li><b>false</b> if messages are not inserted to database possibly due to duplicate</li>
	 */
	public boolean addConversations(List<ConvMessage> convMessages)
	{
		SQLiteStatement insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
				+ DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.CONV_ID
				+ ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE + "," + DBConstants.MSISDN + " ) " + " SELECT ?, ?, ?, ?, ?, ?, " + DBConstants.CONV_ID + ", ?, ?, ?, ? FROM "
				+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN + "=?");
		try
		{
			mDb.beginTransaction();

			long msgId = -1;

			int unreadMessageCount = 0;
			int unreadPinMessageCount = 0;

			for (ConvMessage conv : convMessages)
			{

				String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());

				bindConversationInsert(insertStatement, conv);

				/*
				 * In case message is duplicate insert statement will throw exception . 
				 * It will catch that exception and will return false denoting duplicate message case
				 */
				try
				{
					msgId = insertStatement.executeInsert();
				}
				catch (Exception e)
				{
					// duplicate message return false
					Logger.e(getClass().getSimpleName(), "Duplicate value ", e);
					return false;
				}


				addThumbnailStringToMetadata(conv.getMetadata(), thumbnailString);
				/*
				 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the conversation does
				 * not exist we do not add a conversation.
				 */
				if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn()))
				{
					Conversation conversation = addConversation(conv.getMsisdn(), !conv.isSMS(), null, null);
					if (conversation != null)
					{
						conversation.addMessage(conv);
					}

					bindConversationInsert(insertStatement, conv);
					try
					{
						msgId = insertStatement.executeInsert();
					}
					catch (Exception e)
					{
						// duplicate message return false
						Logger.e(getClass().getSimpleName(), "Duplicate value ", e);
						return false;
					}

					conv.setConversation(conversation);
					assert (msgId >= 0);
				}
				else if (conv.getConversation() == null)
				{
					// conversation not set, retrieve it from db
					Conversation conversation = this.getConversation(conv.getMsisdn(), 0);
					conv.setConversation(conversation);
				}
				conv.setMsgID(msgId);
				ChatThread.addtoMessageMap(conv);

				if (conv.isFileTransferMessage() && conv.getConversation() != null)
				{
					addSharedMedia(msgId, conv.getConversation().getConvId());
				}
				if (Utils.shouldIncrementCounter(conv))
				{
					unreadMessageCount++;
				}
				/*
				 * Updating the conversations table
				 */
				ContentValues contentValues = getContentValueForConversationMessage(conv);
				mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() });
				
				// upgrade groupInfoTable
				updateReadBy(conv);
			}

			incrementUnreadCounter(convMessages.get(0).getMsisdn(), unreadMessageCount);
			mDb.setTransactionSuccessful();
			return true;
		}
		finally
		{
			insertStatement.close();
			mDb.endTransaction();
		}
	}
	
	/**
	 * 
	 * @param convMessages
	 * 			-- list of messages came in bulk packet
	 * @return
	 * 		   <li><b>list</b> of non duplicate messages successfully added to database</li>
	 * 		  
	 */
	public LinkedList<ConvMessage> addConversationsBulk(List<ConvMessage> convMessages)
	{
		HashMap<String, Conversation> convesationMap = new HashMap<String, Conversation>();
		Logger.d("bulkPacket", "adding conversation started");
		LinkedList<ConvMessage> resultList = new LinkedList<ConvMessage>();
		SQLiteStatement insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
				+ DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.CONV_ID
				+ ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE + "," + DBConstants.MSISDN + " ) " + " SELECT ?, ?, ?, ?, ?, ?, " + DBConstants.CONV_ID + ", ?, ?, ?, ? FROM "
				+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN + "=?");
		try
		{
			long msgId = -1;

			for (ConvMessage conv : convMessages)
			{

				String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());
				bindConversationInsert(insertStatement, conv);

				try
				{
					msgId = insertStatement.executeInsert();
				}
				catch (Exception e)
				{
					// duplicate message . Skip further processing
					Logger.e(getClass().getSimpleName(), "Duplicate value ", e);
					continue;
				}
				addThumbnailStringToMetadata(conv.getMetadata(), thumbnailString);
				/*
				 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the conversation does
				 * not exist we do not add a conversation.
				 */
				if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn()))
				{
					Conversation conversation = addConversation(conv.getMsisdn(), !conv.isSMS(), null, null);
					if (conversation != null)
					{
						conversation.addMessage(conv);
					}

					bindConversationInsert(insertStatement, conv);
					try
					{
						msgId = insertStatement.executeInsert();
					}
					catch (Exception e)
					{
						// duplicate message . Skip further processing
						Logger.e(getClass().getSimpleName(), "Duplicate value ", e);
						continue;
					}

					conv.setConversation(conversation);
					assert (msgId >= 0);
				}
				else if (conv.getConversation() == null)
				{
					// conversation not set, retrieve it from db
					Conversation conversation = null;
					String msisdn = conv.getMsisdn();
					if (convesationMap.get(msisdn) == null)
					{
						conversation = this.getConversation(msisdn, 0);
						convesationMap.put(msisdn, conversation);
					}
					else
					{
						conversation = convesationMap.get(msisdn);
					}
					conv.setConversation(conversation);
				}
				conv.setMsgID(msgId);
				ChatThread.addtoMessageMap(conv);

				if (conv.isFileTransferMessage() && conv.getConversation() != null)
				{
					addSharedMedia(msgId, conv.getConversation().getConvId());
				}
				resultList.add(conv);
			}
			Logger.d("BulkProcess", "adding conversation returning");
			return resultList;
		}
		finally
		{
			insertStatement.close();
		}
	}
	
	/**
	 * This function updates unread count for each msisdn/groupId
	 *  
	 * @param messageListMap
	 * 			-- map of msisdn/groupid to list of conversation messages
	 */
	public void incrementUnreadCountBulk(Map<String, LinkedList<ConvMessage>> messageListMap)
	{
		for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
		{
			LinkedList<ConvMessage> list= entry.getValue();
			if(!list.isEmpty())
			{
				incrementUnreadCounter(entry.getKey(), list.size());
			}
		}
	}
	
	/**
	 * 
	 * @param convMessages
	 * 			-- list of messages to be added to conversation table
	 * @param lastPinMap
	 * 			-- list of pin messages to be added to conversation table
	 */
	public void addLastConversations(List<ConvMessage> convMessages, HashMap<String, PairModified<ConvMessage, Integer>> lastPinMap)
	{
		for (ConvMessage conv : convMessages)
		{
			String msisdn = conv.getMsisdn();
			ContentValues contentValues = getContentValueForConversationMessage(conv);
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
			
			if(lastPinMap.get(conv.getMsisdn()) != null)
			{
				lastPinMap.get(msisdn).setSecond(lastPinMap.get(msisdn).getSecond() - 1);
			}
		}
		
		for (Entry<String, PairModified<ConvMessage, Integer>> entry : lastPinMap.entrySet())
		{
			PairModified<ConvMessage, Integer> pair= entry.getValue();
			String msisdn = entry.getKey();
			ContentValues contentValues = getContentValueForPinConversationMessage(pair.getFirst(), new ContentValues(), pair.getSecond());
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
	}
	
	public void addLastPinConversations(List<ConvMessage> convMessages)
	{
		for (ConvMessage conv : convMessages)
		{
			ContentValues contentValues = getContentValueForPinConversationMessage(conv, new ContentValues());
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() });
		}
	}

	public void updateIsHikeMessageState(long id, boolean isHikeMessage)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_HIKE_MESSAGE, isHikeMessage);

		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(id) });
	}

	private ContentValues getContentValueForConversationMessage(ConvMessage conv)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE, conv.getMessage());
		contentValues.put(DBConstants.MESSAGE_METADATA, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
		contentValues.put(DBConstants.GROUP_PARTICIPANT, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");

		boolean statusMessage = conv.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
		contentValues.put(DBConstants.IS_STATUS_MSG, statusMessage);

		if (!statusMessage)
		{
			contentValues.put(DBConstants.MESSAGE_ID, conv.getMsgID());
			contentValues.put(DBConstants.MAPPED_MSG_ID, conv.getMappedMsgID());
			contentValues.put(DBConstants.MSG_STATUS, conv.getState().ordinal());
			contentValues.put(DBConstants.TIMESTAMP, conv.getTimestamp());
		}
		if (conv.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			contentValues =  getContentValueForPinConversationMessage(conv, contentValues);
		}
		return contentValues;
	}
	
	/*
	 * add pin related content values to the content values that comes in argument and return modified content values
	 */
	private ContentValues getContentValueForPinConversationMessage(ConvMessage conv, ContentValues contentValues)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() },
					null, null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);
				try
				{
					Conversation.MetaData convMetaData = null;
					if (metadata != null)
					{
						convMetaData = new Conversation.MetaData(metadata);
					}
					else
					{
						convMetaData = new Conversation.MetaData(null);
						convMetaData.setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, conv.getMsgID());
					}
					long preTimeStamp = convMetaData.getLastPinTimeStamp(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					long currentTimeStamp = conv.getTimestamp();
					if (preTimeStamp < currentTimeStamp)
					{
						convMetaData = updatePinMetadata(conv, convMetaData,0);

					}
					contentValues.put(DBConstants.CONVERSATION_METADATA, convMetaData.toString());
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_META_DATA_UPDATED, convMetaData);
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return contentValues;
		}
		finally 
		{
			if(c != null)
			{
				c.close();
			}
		}
	}
	
	/*
	 * add pin related content values to the content values that comes in argument and return modified content values
	 * This function is specific for bulk. We also give unread count in this function.
	 */
	private ContentValues getContentValueForPinConversationMessage(ConvMessage conv, ContentValues contentValues, int unreadCount)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() },
					null, null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);

				try
				{
					Conversation.MetaData convMetaData = null;
					convMetaData = new Conversation.MetaData(metadata);

					convMetaData = updatePinMetadata(conv, convMetaData, unreadCount);
					contentValues.put(DBConstants.CONVERSATION_METADATA, convMetaData.toString());
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return contentValues;
		}
		finally 
		{
			if(c != null)
			{
				c.close();
			}
		}
	}
	
	public Conversation.MetaData updatePinMetadata(ConvMessage msg, Conversation.MetaData metadata, int unreadCount)
	{
		try
		{
			if (metadata != null)
			{

				// update only for received
				if (!msg.isSent())
				{
					if(unreadCount != 0)
					{
						metadata.setUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, (metadata.getUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN) + unreadCount));
					}
					else
					{
						metadata.incrementUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					}
				
					metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, false);
				}
				else
				{
					metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);					
				}
				
				metadata.setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
    			metadata.setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, msg.getMsgID());
				metadata.setLastPinTimeStamp(HikeConstants.MESSAGE_TYPE.TEXT_PIN, msg.getTimestamp());
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metadata;
	}

	public void deleteConversation(Long[] ids, List<String> msisdns)
	{
		try
		{
			mDb.beginTransaction();
			for (int i = 0; i < ids.length; i++)
			{
				Long[] bindArgs = new Long[] { ids[i] };
				String msisdn = msisdns.get(i);
				mDb.execSQL("DELETE FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONV_ID + "= ?", bindArgs);
				mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.CONV_ID + "= ?", bindArgs);
				if (Utils.isGroupConversation(msisdn))
				{
					mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
					mDb.delete(DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
					removeChatThemeForMsisdn(msisdn);
				}
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	/**
	 * Add a conversation to the db
	 * 
	 * @param msisdn
	 *            the msisdn of the contact
	 * @param onhike
	 *            true iff the contact is onhike. If this is false, we consult the local db as well
	 * @param groupName
	 *            the name of the group. Sent as <code>null</code> if the conversation is not a group conversation
	 * @return Conversation object representing the conversation
	 */
	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner)
	{
		HikeUserDatabase huDb = HikeUserDatabase.getInstance();
		ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ? new ContactInfo(msisdn, msisdn, groupName, msisdn) : huDb.getContactInfoFromMSISDN(msisdn, false);
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.CONVERSATIONS_TABLE);
			ih.prepareForInsert();
			ih.bind(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
			if (contactInfo != null)
			{
				ih.bind(ih.getColumnIndex(DBConstants.CONTACT_ID), contactInfo.getId());
				onhike |= contactInfo.isOnhike();
			}

			ih.bind(ih.getColumnIndex(DBConstants.ONHIKE), onhike);

			long id = 0l;
			try
			{
				id = ih.execute();
			}
			catch (Exception e)
			{
				System.out.println("message" + e.getMessage());
				e.printStackTrace();
				return null;
			}

			if (id >= 0)
			{
				Conversation conv;
				if (Utils.isGroupConversation(msisdn))
				{
					conv = new GroupConversation(msisdn, id, (contactInfo != null) ? contactInfo.getName() : null, groupOwner, true);
					InsertHelper groupInfoIH = null;
					try
					{
						groupInfoIH = new InsertHelper(mDb, DBConstants.GROUP_INFO_TABLE);
						groupInfoIH.prepareForInsert();
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ID), msisdn);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_NAME), groupName);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_OWNER), groupOwner);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ALIVE), 1);
						groupInfoIH.execute();
					}
					finally
					{
						if (groupInfoIH != null)
						{
							groupInfoIH.close();
						}
					}

					((GroupConversation) conv).setGroupParticipantList(getGroupParticipants(msisdn, false, false));
				}
				else
				{
					conv = new Conversation(msisdn, id, (contactInfo != null) ? contactInfo.getName() : null, onhike);
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv);
				return conv;

			}
			/* TODO does this happen? If so, what should we do? */
			Logger.wtf("Conversationadding", "Couldn't add conversation --- race condition?");
			return null;

		}
		finally
		{
			if (ih != null)
			{
				ih.close();
			}
		}
	}

	public List<ConvMessage> getConversationThread(String msisdn, long convid, int limit, Conversation conversation, long maxMsgId)
	{
		String limitStr = (limit == -1) ? null : new Integer(limit).toString();
		String selection = DBConstants.CONV_ID + " = ?" + (maxMsgId == -1 ? "" : " AND " + DBConstants.MESSAGE_ID + "<" + maxMsgId);
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE }, selection, new String[] { Long.toString(convid) }, null, null, DBConstants.MESSAGE_ID + " DESC", limitStr);

			List<ConvMessage> elements = getMessagesFromDB(c, conversation);
			Collections.reverse(elements);

			return elements;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Conversation getConversation(String msisdn, int limit, boolean getMetadata)
	{

		Cursor c = null;
		HikeUserDatabase huDb = null;
		Conversation conv = null;
		try
		{
			if (getMetadata)
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT,
						DBConstants.IS_STEALTH, DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			else
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT,
						DBConstants.IS_STEALTH }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			long convid = c.getInt(c.getColumnIndex(DBConstants.CONV_ID));
			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			int unreadCount = c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
			boolean isStealth = c.getInt(c.getColumnIndex(DBConstants.IS_STEALTH)) != 0;
			String metadata = null;

			if (Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn, convid);
				conv.setIsStealth(isStealth);
			}
			else
			{
				huDb = HikeUserDatabase.getInstance();

				String name;
				if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
				{
					name = HikeMessengerApp.hikeBotNamesMap.get(msisdn);
					onhike = true;
				}
				else
				{
					ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn, false);
					name = contactInfo.getName();
					onhike |= contactInfo.isOnhike();
				}
				conv = new Conversation(msisdn, convid, name, onhike, isStealth);

			}
			if (getMetadata)
			{
				metadata = c.getString(c.getColumnIndex(DBConstants.CONVERSATION_METADATA));
				try
				{
					conv.setMetaData(new Conversation.MetaData(metadata));
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			List<ConvMessage> messages;
			if (limit != 0)
			{
				if (limit != -1 && unreadCount > limit)
				{
					messages = getConversationThread(msisdn, convid, unreadCount, conv, -1);
				}
				else
				{
					messages = getConversationThread(msisdn, convid, limit, conv, -1);
				}
				conv.setMessages(messages);
			}
			conv.setUnreadCount(unreadCount);

			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	public Conversation getConversation(String msisdn, int limit)
	{
		return getConversation(msisdn, limit, false);
	}

	public Conversation getConversationBulk(String msisdn, int limit)
	{
		Cursor c = null;
		HikeUserDatabase huDb = null;
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT,
					DBConstants.IS_STEALTH }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			long convid = c.getInt(c.getColumnIndex(DBConstants.CONV_ID));
			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			int unreadCount = c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
			boolean isStealth = c.getInt(c.getColumnIndex(DBConstants.IS_STEALTH)) != 0;

			if (Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn, convid);
				conv.setIsStealth(isStealth);
			}
			else
			{
				huDb = HikeUserDatabase.getInstance();

				String name;
				if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
				{
					name = HikeMessengerApp.hikeBotNamesMap.get(msisdn);
					onhike = true;
				}
				else
				{
					ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn, false);
					name = contactInfo.getName();
					onhike |= contactInfo.isOnhike();
				}
				conv = new Conversation(msisdn, convid, name, onhike, isStealth);

			}

			conv.setUnreadCount(unreadCount);

			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Using this method to get the conversation with the last message. If there is no last message, we return null if the conversation is not a GC.
	 * 
	 * @param msisdn
	 * @return
	 */
	public Conversation getConversationWithLastMessage(String msisdn)
	{
		Cursor c = null;
		HikeUserDatabase huDb = null;
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.CONV_ID, DBConstants.MESSAGE, DBConstants.MSG_STATUS,
					DBConstants.TIMESTAMP, DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.ONHIKE },
					DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			long convid = c.getInt(c.getColumnIndex(DBConstants.CONV_ID));
			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

			String messageString = c.getString(msgColumn);
			String metadata = c.getString(metadataColumn);

			/*
			 * If the message does not contain any text or metadata, its an empty message and the conversation is blank.
			 */
			if (!Utils.isGroupConversation(msisdn) && TextUtils.isEmpty(messageString) && TextUtils.isEmpty(metadata))
			{
				return null;
			}

			if (Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn, convid);
			}
			else
			{
				huDb = HikeUserDatabase.getInstance();
				ContactInfo contactInfo = huDb.getContactInfoFromMSISDN(msisdn, false);

				onhike |= contactInfo.isOnhike();
				conv = new Conversation(msisdn, convid, contactInfo.getName(), onhike);
			}

			ConvMessage message = new ConvMessage(messageString, msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
					c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
			try
			{
				message.setMetadata(metadata);
			}
			catch (JSONException e)
			{
				Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			conv.addMessage(message);

			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<Conversation> getConversations()
	{
		return getConversations(false);
	}

	public List<Conversation> getConversations(boolean getMetadata)
	{
		long startTime = System.currentTimeMillis();

		Cursor groupInfoCursor = null;
		Cursor c = null;

		if (getMetadata)
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.CONV_ID, DBConstants.MESSAGE, DBConstants.MSG_STATUS,
					DBConstants.TIMESTAMP, DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT,
					DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH, DBConstants.CONVERSATION_METADATA }, null, null, null, null, null);
		}
		else
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.CONV_ID, DBConstants.MESSAGE, DBConstants.MSG_STATUS,
					DBConstants.TIMESTAMP, DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT,
					DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH }, null, null, null, null, null);
		}

		Map<String, Conversation> conversationMap = new HashMap<String, Conversation>(c.getCount());

		List<Conversation> conversations = new ArrayList<Conversation>(c.getCount());

		final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		final int convIdx = c.getColumnIndex(DBConstants.CONV_ID);

		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		final int unreadCountColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);
		final int isStealthColumn = c.getColumnIndex(DBConstants.IS_STEALTH);

		HikeUserDatabase huDb = null;

		StringBuilder msisdns = null;
		try
		{
			huDb = HikeUserDatabase.getInstance();
			while (c.moveToNext())
			{
				Conversation conv;
				// TODO this can be expressed in a single sql query
				String msisdn = c.getString(msisdnIdx);
				long convid = c.getInt(convIdx);
				String messageString = c.getString(msgColumn);
				String metadata = c.getString(metadataColumn);

				if (!Utils.isGroupConversation(msisdn))
				{
					if (msisdns == null)
					{
						msisdns = new StringBuilder("(");
					}
					msisdns.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
				}
				conv = new Conversation(msisdn, convid);
				conv.setUnreadCount(c.getInt(unreadCountColumn));
				conv.setIsStealth(c.getInt(isStealthColumn) == 1);

				if (getMetadata)
				{
					int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
					JSONObject convMetadata = null;
					try
					{
						convMetadata = new JSONObject(c.getString(metadataIndex));
					}
					catch (JSONException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// conv.setMetadata(convMetadata);
				}

				if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
				{
					conv.setContactName(HikeMessengerApp.hikeBotNamesMap.get(msisdn));
				}

				/*
				 * If the message does not contain any text or metadata, its an empty message and the conversation is blank.
				 */
				if (!TextUtils.isEmpty(messageString) || !TextUtils.isEmpty(metadata) || Utils.isGroupConversation(msisdn))
				{
					ConvMessage message = new ConvMessage(messageString, msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
							c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
					try
					{
						message.setMetadata(metadata);
					}
					catch (JSONException e)
					{
						Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
					}
					message.setConversation(conv);

					conv.addMessage(message);
				}

				conversationMap.put(msisdn, conv);
			}
			if (msisdns != null)
			{
				msisdns.replace(msisdns.lastIndexOf(","), msisdns.length(), ")");
			}
			else
			{
				msisdns = new StringBuilder("()");
			}

			List<ContactInfo> contactList = huDb.getContactNamesFromMsisdnList(msisdns.toString());

			for (ContactInfo contactInfo : contactList)
			{
				Conversation conversation = conversationMap.get(contactInfo.getMsisdn());
				conversation.setContactName(contactInfo.getName());
				conversation.setOnhike(contactInfo.isOnhike());
			}

			/*
			 * Getting the info for group conversations
			 */
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
					new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER, DBConstants.GROUP_ALIVE }, null, null, null, null, null);

			final int groupIdIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupOwnerIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_OWNER);
			final int groupAliveIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ALIVE);

			Map<String, Map<String, GroupParticipant>> groupIdParticipantsMap = getAllGroupParticipants();

			while (groupInfoCursor.moveToNext())
			{
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				String groupOwner = groupInfoCursor.getString(groupOwnerIdx);
				boolean isGroupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;

				Conversation conversation = conversationMap.get(groupId);
				if (conversation == null)
				{
					continue;
				}
				Map<String, GroupParticipant> groupParticipants = groupIdParticipantsMap.get(groupId);

				GroupConversation groupConversation = new GroupConversation(groupId, conversation.getConvId(), groupName, groupOwner, isGroupAlive);
				groupConversation.setGroupParticipantList(groupParticipants);
				groupConversation.setMessages(conversation.getMessages());
				groupConversation.setUnreadCount(conversation.getUnreadCount());
				groupConversation.setIsStealth(conversation.isStealth());

				/*
				 * Setting the conversation for the message.
				 */
				if (!conversation.getMessages().isEmpty())
				{
					ConvMessage message = conversation.getMessages().get(0);
					message.setConversation(groupConversation);
				}

				conversationMap.remove(groupId);
				conversationMap.put(groupId, groupConversation);
			}

		}
		finally
		{
			c.close();
			if (groupInfoCursor != null)
			{
				groupInfoCursor.close();
			}
		}
		conversations.addAll(conversationMap.values());
		Logger.d(getClass().getSimpleName(), "Query time: " + (System.currentTimeMillis() - startTime));
		Collections.sort(conversations, Collections.reverseOrder());
		return conversations;
	}

	private ConvMessage getLastMessageForConversation(String msisdn)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MAPPED_MSG_ID,
					DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

			if (c.moveToFirst())
			{
				ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
				String metadata = c.getString(metadataColumn);
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}
				return message;
			}
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ConvMessage getLastPinForConversation(Conversation conversation)
	{
		Cursor c = null;

		try
		{

			long msgId = conversation.getMetaData().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE }, DBConstants.MESSAGE_ID + " =?", new String[] { Long.toString(msgId) }, null, null, null, null);
			List<ConvMessage> elements = getMessagesFromDB(c, conversation);
			return elements.get(elements.size() - 1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	private Map<String, Map<String, GroupParticipant>> getAllGroupParticipants()
	{
		Cursor c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME,
				DBConstants.ON_DND }, null, null, null, null, null);

		try
		{
			int groupIdIdx = c.getColumnIndex(DBConstants.GROUP_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int hasLeftIdx = c.getColumnIndex(DBConstants.HAS_LEFT);
			int onHikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int onDndIdx = c.getColumnIndex(DBConstants.ON_DND);

			Map<String, Map<String, GroupParticipant>> groupIdParticipantsMap = new HashMap<String, Map<String, GroupParticipant>>();
			HikeUserDatabase huDB = HikeUserDatabase.getInstance();
			Map<String, List<GroupParticipant>> msisdnToGP = new HashMap<String, List<GroupParticipant>>();
			StringBuilder msisdnSB = new StringBuilder("(");

			while (c.moveToNext())
			{
				String groupId = c.getString(groupIdIdx);
				String msisdn = c.getString(msisdnIdx);
				msisdnSB.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
				GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, c.getString(nameIdx), msisdn, c.getInt(onHikeIdx) != 0),
						c.getInt(hasLeftIdx) != 0, c.getInt(onDndIdx) != 0);

				Map<String, GroupParticipant> participantList = groupIdParticipantsMap.get(groupId);
				if (participantList == null)
				{
					participantList = new HashMap<String, GroupParticipant>();
					groupIdParticipantsMap.put(groupId, participantList);
				}
				participantList.put(msisdn, groupParticipant);

				List<GroupParticipant> groupParticipants = msisdnToGP.get(msisdn);
				if (groupParticipants == null)
				{
					groupParticipants = new ArrayList<GroupParticipant>();
					msisdnToGP.put(msisdn, groupParticipants);
				}
				groupParticipants.add(groupParticipant);
			}
			// atleast one msisdn entered
			if (!"(".equals(msisdnSB.toString()))
			{
				String msisdnMulti = msisdnSB.substring(0, msisdnSB.length() - 1) + ")";
				List<ContactInfo> contactInfos = huDB.getContactNamesFromMsisdnList(msisdnMulti);
				for (ContactInfo contactInfo : contactInfos)
				{
					List<GroupParticipant> groupParticipants = msisdnToGP.get(contactInfo.getMsisdn());
					if (groupParticipants == null)
					{
						continue;
					}
					for (GroupParticipant groupParticipant : groupParticipants)
					{
						groupParticipant.setContactInfo(contactInfo);
					}
				}
			}
			return groupIdParticipantsMap;
		}
		finally
		{
			c.close();
		}
	}

	private Conversation getGroupConversation(String msisdn, long convid)
	{
		Cursor groupCursor = null;
		try
		{
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
					new String[] { DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP }, DBConstants.GROUP_ID + " = ? ",
					new String[] { msisdn }, null, null, null);
			if (!groupCursor.moveToFirst())
			{
				Logger.w(getClass().getSimpleName(), "Could not find db entry: " + msisdn);
				return null;
			}

			String groupName = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_NAME));
			String groupOwner = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_OWNER));
			boolean isGroupAlive = groupCursor.getInt(groupCursor.getColumnIndex(DBConstants.GROUP_ALIVE)) != 0;
			boolean isMuted = groupCursor.getInt(groupCursor.getColumnIndex(DBConstants.MUTE_GROUP)) != 0;

			GroupConversation conv = new GroupConversation(msisdn, convid, groupName, groupOwner, isGroupAlive);
			conv.setGroupParticipantList(getGroupParticipants(msisdn, false, false));
			conv.setGroupMemberAliveCount(getActiveParticipantCount(msisdn));
			conv.setIsMuted(isMuted);

			return conv;
		}
		finally
		{
			if (groupCursor != null)
			{
				groupCursor.close();
			}
		}
	}

	public JSONArray updateStatusAndSendDeliveryReport(long convID)
	{

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID }, DBConstants.CONV_ID + "=? and " + DBConstants.MSG_STATUS
					+ "=?", new String[] { Long.toString(convID), Integer.toString(ConvMessage.State.RECEIVED_UNREAD.ordinal()) }, null, null, null);
			/* If there are no rows in the cursor then simply return null */
			if (c.getCount() <= 0)
			{
				return null;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("(");

			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int mappedMsgIdIdx = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);

			JSONArray ids = new JSONArray();
			while (c.moveToNext())
			{
				long msgId = c.getLong(msgIdIdx);
				long mappedMsgId = c.getLong(mappedMsgIdIdx);
				if (mappedMsgId > 0)
				{
					ids.put(String.valueOf(mappedMsgId));
				}
				sb.append(msgId);
				if (!c.isLast())
				{
					sb.append(",");
				}
			}
			sb.append(")");
			ContentValues values = new ContentValues();
			values.put(DBConstants.MSG_STATUS, ConvMessage.State.RECEIVED_READ.ordinal());
			int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID + " in " + sb.toString(), null);

			// Resetting the unread count as well
			values.put(DBConstants.UNREAD_COUNT, 0);
			mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MESSAGE_ID + " in " + sb.toString(), null);

			Logger.d("HIKE CONVERSATION DB ", "Rows Updated : " + rowsAffected);
			if (ids.length() == 0)
			{
				return null;
			}
			return ids;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/* deletes a single message */
	public void deleteMessage(ConvMessage convMessage, boolean isLastMessage)
	{
		Long[] bindArgs = new Long[] { convMessage.getMsgID() };
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MESSAGE_ID + "= ?", bindArgs);

		if (isLastMessage)
		{
			deleteMessageFromConversation(convMessage.getMsisdn(), convMessage.getConversation().getConvId());
		}
	}

	public void clearConversation(long convId)
	{
		Long[] args = new Long[] { convId };
		/*
		 * Clearing the messages table.
		 */
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.CONV_ID + "= ?", args);

		/*
		 * Next we have to clear the conversation table.
		 */
		clearLastConversationMessage(convId);
	}

	private void clearLastConversationMessage(long convId)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE, "");
		contentValues.put(DBConstants.MESSAGE_METADATA, "");
		contentValues.put(DBConstants.GROUP_PARTICIPANT, "");
		contentValues.put(DBConstants.IS_STATUS_MSG, false);
		contentValues.put(DBConstants.MESSAGE_ID, 0);
		contentValues.put(DBConstants.MAPPED_MSG_ID, 0);
		contentValues.put(DBConstants.UNREAD_COUNT, 0);
		contentValues.put(DBConstants.MSG_STATUS, State.RECEIVED_READ.ordinal());

		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { Long.toString(convId) });
	}

	private void deleteMessageFromConversation(String msisdn, long convId)
	{
		/*
		 * We get the latest message from the messages table
		 */
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, null, DBConstants.CONV_ID + "=?", new String[] { Long.toString(convId) }, null, null, DBConstants.MESSAGE_ID + " DESC LIMIT "
					+ 1);
			boolean conversationEmpty = false;
			if (c.moveToFirst())
			{
				final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
				final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
				final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
				final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
				final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
				final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
				final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

				ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
				String metadata = c.getString(metadataColumn);
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}
				ContentValues contentValues = getContentValueForConversationMessage(message);
				mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
			}
			else
			{
				if (Utils.isGroupConversation(msisdn))
				{
					/*
					 * If we have removed the last message of a group, we should do the same operations we do when clearing a conversation.
					 */
					clearLastConversationMessage(convId);
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, msisdn);
					return;
				}
				else
				{
					/*
					 * This conversation is empty.
					 */
					mDb.delete(DBConstants.CONVERSATIONS_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
					conversationEmpty = true;
				}
			}
			ConvMessage newLastMessage = conversationEmpty ? null : getLastMessageForConversation(msisdn);

			HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_MESSAGE_DELETED, new Pair<ConvMessage, String>(newLastMessage, msisdn));
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean wasOverlayDismissed(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.OVERLAY_DISMISSED }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			int s = 0;
			if (c.moveToFirst())
			{
				s = c.getInt(0);
			}
			return (s == 0) ? false : true;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void setOverlay(boolean dismiss, String msisdn)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.OVERLAY_DISMISSED, dismiss);
		if (msisdn != null)
		{
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
		else
		{
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, null, null);
		}
	}

	/**
	 * Add a new participants to a group
	 * 
	 * @param groupId
	 *            The id of the group to which the participants are to be added
	 * @param participantList
	 *            A list of the participants to be added
	 */
	public int addGroupParticipants(String groupId, Map<String, GroupParticipant> participantList)
	{
		boolean participantsAlreadyAdded = true;
		boolean infoChangeOnly = false;

		Map<String, GroupParticipant> currentParticipants = getGroupParticipants(groupId, true, false);
		if (currentParticipants.isEmpty())
		{
			participantsAlreadyAdded = false;
		}
		for (Entry<String, GroupParticipant> newParticipantEntry : participantList.entrySet())
		{
			if (!currentParticipants.containsKey(newParticipantEntry.getKey()))
			{
				participantsAlreadyAdded = false;
				infoChangeOnly = false;
			}
			else
			{
				GroupParticipant currentParticipant = currentParticipants.get(newParticipantEntry.getKey());
				if (currentParticipant.onDnd() != newParticipantEntry.getValue().onDnd())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				if (currentParticipant.getContactInfo().isOnhike() != newParticipantEntry.getValue().getContactInfo().isOnhike())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
			}
		}
		if (participantsAlreadyAdded)
		{
			return HikeConstants.NO_CHANGE;
		}

		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.GROUP_MEMBERS_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN + ", "
					+ DBConstants.NAME + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_LEFT + ", " + DBConstants.ON_DND + ", " + DBConstants.SHOWN_STATUS + " ) "
					+ " VALUES (?, ?, ?, ?, ?, ?, ?)");
			mDb.beginTransaction();
			for (Entry<String, GroupParticipant> participant : participantList.entrySet())
			{
				GroupParticipant groupParticipant = participant.getValue();
				insertStatement.bindString(ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
				insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), participant.getKey());
				insertStatement.bindString(ih.getColumnIndex(DBConstants.NAME), groupParticipant.getContactInfo().getName());
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.ONHIKE), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.HAS_LEFT), 0);
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.ON_DND), groupParticipant.onDnd() ? 1 : 0);
				insertStatement.bindLong(ih.getColumnIndex(DBConstants.SHOWN_STATUS), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);

				insertStatement.executeInsert();
			}
			mDb.setTransactionSuccessful();
			return infoChangeOnly ? HikeConstants.PARTICIPANT_STATUS_CHANGE : HikeConstants.NEW_PARTICIPANT;
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
			if (ih != null)
			{
				ih.close();
			}
			
			mDb.endTransaction();
		}
	}

	public int updateDndStatus(String msisdn)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.ON_DND, 0);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public int updateShownStatus(String groupId)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.SHOWN_STATUS, 1);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	/**
	 * Should be called when a participant leaves the group
	 * 
	 * @param groupId
	 *            : The group ID of the group containing the participant
	 * @param msisdn
	 *            : The msisdn of the participant
	 */
	public int setParticipantLeft(String groupId, String msisdn)
	{
		Cursor c = null;
		try
		{
			String selection = DBConstants.GROUP_ID + "=? AND " + DBConstants.MSISDN + "=?";
			String[] selectionArgs = new String[] { groupId, msisdn };

			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.HAS_LEFT }, selection, selectionArgs, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			int hasLeft = c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT));
			/*
			 * If member has already left don't do anything.
			 */
			if (hasLeft == 1)
			{
				return 0;
			}

			ContentValues contentValues = new ContentValues(1);
			contentValues.put(DBConstants.HAS_LEFT, 1);

			return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, selection, selectionArgs);

		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Returns a list of participants to a group
	 * 
	 * @param groupId
	 * @return
	 */
	public Map<String, GroupParticipant> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly)
	{
		return getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly, true);
	}

	/**
	 * Returns a list of participants to a group
	 * 
	 * @param groupId
	 * @return
	 */
	public Map<String, GroupParticipant> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly, boolean fetchParticipants)
	{
		String selection = DBConstants.GROUP_ID + " =? " + (activeOnly ? " AND " + DBConstants.HAS_LEFT + "=0" : "")
				+ (notShownStatusMsgOnly ? " AND " + DBConstants.SHOWN_STATUS + "=0" : "");
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND },
					selection, new String[] { groupId }, null, null, null);

			Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

			HikeUserDatabase huDB = HikeUserDatabase.getInstance();
			StringBuilder allMsisdns = new StringBuilder("(");
			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				allMsisdns.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
				GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, c.getString(c.getColumnIndex(DBConstants.NAME)), msisdn, c.getInt(c
						.getColumnIndex(DBConstants.ONHIKE)) != 0), c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT)) != 0, c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0);
				participantList.put(msisdn, groupParticipant);
			}
			String msisdns = allMsisdns.toString();
			// at least one msisdn is required to run this in query
			if (fetchParticipants && !"(".equals(msisdns))
			{
				msisdns = msisdns.substring(0, msisdns.length() - 1) + ")";
				List<ContactInfo> list = huDB.getContactNamesFromMsisdnList(msisdns);
				for (ContactInfo contactInfo : list)
				{
					participantList.get(contactInfo.getMsisdn()).setContactInfo(contactInfo);
				}
			}

			return participantList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int getActiveParticipantCount(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.HAS_LEFT + "=0 AND " + DBConstants.GROUP_ID + "=?",
					new String[] { groupId }, null, null, null);
			return c.getCount();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Reutrn the group name corresponding to a group ID.
	 * 
	 * @param groupId
	 * @return
	 */
	public String getGroupName(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_NAME }, DBConstants.GROUP_ID + " = ? ", new String[] { groupId }, null, null, null);
			String groupName = "";
			if (c.moveToFirst())
			{
				groupName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));
			}
			return groupName;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean doesConversationExist(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.MSISDN + " = ? ", new String[] { msisdn }, null, null, null);

			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean isGroupAlive(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ALIVE }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null, null, null);

			if (!c.moveToFirst())
			{
				return false;
			}
			return c.getInt(c.getColumnIndex(DBConstants.GROUP_ALIVE)) == 1;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean isGroupMuted(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID }, DBConstants.GROUP_ID + " = ? AND " + DBConstants.MUTE_GROUP + " = 1",
					new String[] { groupId }, null, null, null);
			return c.moveToFirst();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void toggleGroupMute(String groupId, boolean isMuted)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MUTE_GROUP, isMuted);

		mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	public int setGroupName(String groupId, String groupname)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_NAME }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			String existingName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));

			if (groupname.equals(existingName))
			{
				return 0;
			}

			ContentValues values = new ContentValues(1);
			values.put(DBConstants.GROUP_NAME, groupname);
			return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public String getParticipantName(String groupId, String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.NAME }, DBConstants.GROUP_ID + " = ? AND " + DBConstants.MSISDN + " = ? ", new String[] {
					groupId, msisdn }, null, null, null);
			String name = "";
			if (c.moveToFirst())
			{
				name = c.getString(c.getColumnIndex(DBConstants.NAME));
			}
			return name;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int toggleGroupDeadOrAlive(String groupId, boolean alive)
	{
		if (!doesConversationExist(groupId))
		{
			return 0;
		}
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_ALIVE, alive);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
	}

	public long[] getUnreadMessageIds(long convId)
	{
		Cursor cursor = null;
		try
		{
			cursor = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID },
					DBConstants.MSG_STATUS + " IN " + "(" + ConvMessage.State.SENT_CONFIRMED.ordinal() + ", " + ConvMessage.State.SENT_DELIVERED.ordinal() + ")" + " AND "
							+ DBConstants.CONV_ID + " =?", new String[] { convId + "" }, null, null, null);

			if (!cursor.moveToFirst())
			{
				return null;
			}
			long[] ids = new long[cursor.getCount()];
			int i = 0;
			int idIdx = cursor.getColumnIndex(DBConstants.MESSAGE_ID);
			do
			{
				ids[i++] = cursor.getLong(idIdx);
			}
			while (cursor.moveToNext());
			return ids;
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	public List<String> listOfGroupConversationsWithMsisdn(String msisdn)
	{
		Cursor cursor = null;
		try
		{
			String selection = DBConstants.MSISDN + "=? AND " + DBConstants.HAS_LEFT + "=0 AND " + DBConstants.GROUP_ID + " NOT IN (SELECT " + DBConstants.GROUP_ID + " FROM "
					+ DBConstants.GROUP_INFO_TABLE + " WHERE " + DBConstants.GROUP_ALIVE + " =0)";
			List<String> groupConversations = new ArrayList<String>();
			cursor = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.GROUP_ID }, selection, new String[] { msisdn }, null, null, null);
			int groupIdIdx = cursor.getColumnIndex(DBConstants.GROUP_ID);
			while (cursor.moveToNext())
			{
				groupConversations.add(cursor.getString(groupIdIdx));
			}
			return groupConversations;
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	/**
	 * Called when forwarding a message so that the groups can also be displayed in the contact list.
	 * 
	 * @return
	 */
	public List<ContactInfo> getGroupNameAndParticipantsAsContacts(Context context)
	{
		Cursor groupCursor = null;
		try
		{
			List<ContactInfo> groups = new ArrayList<ContactInfo>();
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME }, DBConstants.GROUP_ALIVE + "=1", null, null, null,
					null);
			int groupNameIdx = groupCursor.getColumnIndex(DBConstants.GROUP_NAME);
			int groupIdIdx = groupCursor.getColumnIndex(DBConstants.GROUP_ID);
			while (groupCursor.moveToNext())
			{
				String groupId = groupCursor.getString(groupIdIdx);
				String groupName = groupCursor.getString(groupNameIdx);

				Map<String, GroupParticipant> groupParticipantMap = getGroupParticipants(groupId, true, false, false);
				groupName = TextUtils.isEmpty(groupName) ? Utils.defaultGroupName(groupParticipantMap) : groupName;
				int numMembers = groupParticipantMap.size();

				// Here we make this string the msisdn so that it can be
				// displayed in the list view when forwarding the message
				String numberMembers = context.getString(R.string.num_people, (numMembers + 1));

				ContactInfo group = new ContactInfo(groupId, numberMembers, groupName, groupId, true);
				groups.add(group);
			}

			return groups;
		}
		finally
		{
			if (groupCursor != null)
			{
				groupCursor.close();
			}
		}
	}

	public void updateRecencyOfEmoticon(int emoticonIndex, long lastUsed)
	{
		SQLiteStatement insertStatement = null;
		try
		{
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + ", " + DBConstants.LAST_USED + " ) "
					+ " VALUES (?, ?)");

			insertStatement.bindLong(1, emoticonIndex);
			insertStatement.bindLong(2, lastUsed);

			long id = insertStatement.executeInsert();
			Logger.d(getClass().getSimpleName(), "iNserted row: " + id);
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
		}
	}

	public int[] fetchEmoticonsOfType(int startOffset, int endOffset, int limit)
	{
		Cursor c = null;
		try
		{
			String[] columns = new String[] { DBConstants.EMOTICON_NUM };
			String selection = DBConstants.EMOTICON_NUM + ">=" + startOffset + (endOffset != 0 ? " AND " + DBConstants.EMOTICON_NUM + "<" + (endOffset) : "");
			String orderBy = DBConstants.LAST_USED + " DESC " + (limit != -1 ? (" LIMIT " + limit) : "");

			c = mDb.query(DBConstants.EMOTICON_TABLE, columns, selection, null, null, null, orderBy);
			int[] emoticonIndices = new int[c.getCount()];
			int emoticonIndexIdx = c.getColumnIndex(DBConstants.EMOTICON_NUM);
			int i = 0;
			while (c.moveToNext())
			{
				emoticonIndices[i++] = c.getInt(emoticonIndexIdx);
			}
			return emoticonIndices;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public long addStatusMessage(StatusMessage statusMessage, boolean showInCentralTimeline)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.STATUS_MAPPED_ID, statusMessage.getMappedId());
		values.put(DBConstants.STATUS_TEXT, statusMessage.getText());
		values.put(DBConstants.MSISDN, statusMessage.getMsisdn());
		values.put(DBConstants.STATUS_TYPE, statusMessage.getStatusMessageType().ordinal());
		values.put(DBConstants.TIMESTAMP, statusMessage.getTimeStamp());
		values.put(DBConstants.SHOW_IN_TIMELINE, showInCentralTimeline);
		values.put(DBConstants.MOOD_ID, statusMessage.getMoodId());
		values.put(DBConstants.TIME_OF_DAY, statusMessage.getTimeOfDay());
		/*
		 * Inserting -1 to denote that this status is not a part of any conversation yet.
		 */
		values.put(DBConstants.MESSAGE_ID, -1);

		long id = mDb.insert(DBConstants.STATUS_TABLE, null, values);
		statusMessage.setId(id);

		return id;
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, String... msisdnList)
	{
		return getStatusMessages(timelineUpdatesOnly, -1, -1, msisdnList);
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, int limit, int lastStatusId, String... msisdnList)
	{
		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY };

		StringBuilder selection = new StringBuilder();

		StringBuilder msisdnSelection = null;
		if (msisdnList != null)
		{
			msisdnSelection = new StringBuilder("(");
			for (String msisdn : msisdnList)
			{
				msisdnSelection.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
			msisdnSelection.replace(msisdnSelection.lastIndexOf(","), msisdnSelection.length(), ")");
		}

		if (!TextUtils.isEmpty(msisdnSelection))
		{
			selection.append(DBConstants.MSISDN + " IN " + msisdnSelection.toString() + (timelineUpdatesOnly ? " AND " : ""));
		}
		if (timelineUpdatesOnly)
		{
			selection.append(DBConstants.SHOW_IN_TIMELINE + " =1 ");
		}
		if (lastStatusId != -1)
		{
			selection.append(" AND " + DBConstants.STATUS_ID + " < " + lastStatusId);
		}

		String orderBy = DBConstants.STATUS_ID + " DESC ";

		if (limit != -1)
		{
			orderBy += "LIMIT " + limit;
		}

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection.toString(), null, null, null, orderBy);

			List<StatusMessage> statusMessages = new ArrayList<StatusMessage>(c.getCount());
			Map<String, List<StatusMessage>> statusMessagesMap = new HashMap<String, List<StatusMessage>>();

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);

			StringBuilder msisdns = null;
			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx));
				statusMessages.add(statusMessage);

				List<StatusMessage> msisdnMessages = statusMessagesMap.get(msisdn);
				if (msisdnMessages == null)
				{
					if (msisdns == null)
					{
						msisdns = new StringBuilder("(");
					}
					msisdns.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
					msisdnMessages = new ArrayList<StatusMessage>();
					statusMessagesMap.put(msisdn, msisdnMessages);
				}
				msisdnMessages.add(statusMessage);
			}
			if (msisdns != null)
			{
				msisdns.replace(msisdns.lastIndexOf(","), msisdns.length(), ")");

				List<ContactInfo> contactList = HikeUserDatabase.getInstance().getContactNamesFromMsisdnList(msisdns.toString());
				for (ContactInfo contactInfo : contactList)
				{
					List<StatusMessage> msisdnMessages = statusMessagesMap.get(contactInfo.getMsisdn());
					if (msisdnMessages != null)
					{
						for (StatusMessage statusMessage : msisdnMessages)
						{
							statusMessage.setName(contactInfo.getName());
						}
					}
				}
			}

			return statusMessages;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, StatusMessage> getLastStatusMessages(boolean timelineUpdatesOnly, StatusMessage.StatusMessageType[] smTypes, List<ContactInfo> contactList)
	{
		Map<String, StatusMessage> statusMessagesMap = new HashMap<String, StatusMessage>();

		if (contactList == null || contactList.isEmpty())
		{
			return statusMessagesMap;
		}
		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY };

		StringBuilder selection = new StringBuilder();

		StringBuilder msisdnSelection = null;
		msisdnSelection = new StringBuilder("(");
		for (ContactInfo contactInfo : contactList)
		{
			msisdnSelection.append(DatabaseUtils.sqlEscapeString(contactInfo.getMsisdn()) + ",");
		}
		msisdnSelection.replace(msisdnSelection.lastIndexOf(","), msisdnSelection.length(), ")");

		if (!TextUtils.isEmpty(msisdnSelection))
		{
			selection.append(DBConstants.MSISDN + " IN " + msisdnSelection.toString() + (timelineUpdatesOnly ? " AND " : ""));
		}
		if (timelineUpdatesOnly)
		{
			selection.append(DBConstants.SHOW_IN_TIMELINE + " =1 ");
		}

		StringBuilder smTypeSelection = null;
		if (smTypes != null)
		{
			smTypeSelection = new StringBuilder("(");
			for (StatusMessage.StatusMessageType smType : smTypes)
			{
				smTypeSelection.append(smType.ordinal() + ",");
			}
			smTypeSelection.replace(smTypeSelection.lastIndexOf(","), smTypeSelection.length(), ")");
		}

		if (!TextUtils.isEmpty(smTypeSelection))
		{
			selection.append(" AND " + DBConstants.STATUS_TYPE + " IN " + smTypeSelection.toString());
		}
		String orderBy = DBConstants.STATUS_ID + " DESC ";

		String havingSelection = "MAX(" + DBConstants.STATUS_ID + ")=" + DBConstants.STATUS_ID;
		;

		String groupby = DBConstants.MSISDN;
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection.toString(), null, groupby, havingSelection, orderBy);

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx));
				statusMessagesMap.put(msisdn, statusMessage);

			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return statusMessagesMap;
	}

	public void setMessageIdForStatus(String statusId, long messageId)
	{
		String whereClause = DBConstants.STATUS_MAPPED_ID + "=?";
		String[] whereArgs = new String[] { statusId };

		ContentValues values = new ContentValues(1);
		values.put(DBConstants.MESSAGE_ID, messageId);

		mDb.update(DBConstants.STATUS_TABLE, values, whereClause, whereArgs);
	}

	public void deleteStatus(String statusId)
	{
		String selection = DBConstants.STATUS_MAPPED_ID + "=?";
		String[] whereArgs = new String[] { statusId };
		/*
		 * First we want the message id corresponding to this status.
		 */
		Cursor c = null;
		long messageId = 0;
		String msisdn = "";
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MSISDN }, selection, whereArgs, null, null, null);

			if (c.moveToFirst())
			{
				messageId = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		/*
		 * Now we delete the status
		 */
		mDb.delete(DBConstants.STATUS_TABLE, selection, whereArgs);

		/*
		 * This would be true if the status message was not a part of any conversation.
		 */
		if (messageId == -1)
		{
			return;
		}
		/*
		 * And we delete the message
		 */
		mDb.delete(DBConstants.MESSAGES_TABLE, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(messageId) });

		/*
		 * Checking if the status message deleted was the last message
		 */
		Cursor c1 = null;
		try
		{
			c1 = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MESSAGE_METADATA }, DBConstants.IS_STATUS_MSG + "=1 AND "
					+ DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (c1.moveToFirst())
			{
				long convId = c1.getLong(c1.getColumnIndex(DBConstants.CONV_ID));
				String metadataString = c1.getString(c1.getColumnIndex(DBConstants.MESSAGE_METADATA));
				try
				{
					MessageMetadata messageMetadata = new MessageMetadata(new JSONObject(metadataString), false);

					if (statusId.equals(messageMetadata.getStatusMessage().getMappedId()))
					{
						deleteMessageFromConversation(msisdn, convId);
					}

				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
		}
		finally
		{
			if (c1 != null)
			{
				c1.close();
			}
		}
	}

	public void deleteStatusMessagesForMsisdn(String msisdn)
	{
		String selection = DBConstants.MSISDN + "=?";
		String[] selectionArgs = { msisdn };

		mDb.delete(DBConstants.STATUS_TABLE, selection, selectionArgs);
	}

	public int getTimelineStatusMessageCount()
	{
		return (int) DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM " + DBConstants.STATUS_TABLE + " WHERE " + DBConstants.SHOW_IN_TIMELINE + " =1", null);
	}

	private void denormaliseConversations(SQLiteDatabase mDb)
	{
		Logger.d(getClass().getSimpleName(), "Denormalisingggg");
		String query = "SELECT " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MSG_STATUS + ", "
				+ DBConstants.MESSAGES_TABLE + "." + DBConstants.TIMESTAMP + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID + ", " + DBConstants.MESSAGES_TABLE
				+ "." + DBConstants.MAPPED_MSG_ID + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_METADATA + ", " + DBConstants.MESSAGES_TABLE + "."
				+ DBConstants.GROUP_PARTICIPANT + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID + " FROM messages LEFT OUTER JOIN messages "
				+ "AS max ON messages.convid = max.convid AND max.msgid > messages.msgid" + " WHERE max.msgid IS NULL";

		Cursor c = null;

		try
		{
			c = mDb.rawQuery(query, null);
			mDb.beginTransaction();

			final int convIdx = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID);
			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.GROUP_PARTICIPANT);

			while (c.moveToNext())
			{
				String message = c.getString(msgColumn);
				int msgState = c.getInt(msgStatusColumn);
				int timeStamp = c.getInt(tsColumn);
				long messageId = c.getLong(msgIdColumn);
				long mappedMessageId = c.getLong(mappedMsgIdColumn);
				String metadata = c.getString(metadataColumn);
				String groupParticipant = c.getString(groupParticipantColumn);
				int convid = c.getInt(convIdx);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE, message);
				contentValues.put(DBConstants.MSG_STATUS, msgState);
				contentValues.put(DBConstants.TIMESTAMP, timeStamp);
				contentValues.put(DBConstants.MESSAGE_ID, messageId);
				contentValues.put(DBConstants.MAPPED_MSG_ID, mappedMessageId);
				contentValues.put(DBConstants.MESSAGE_METADATA, metadata);
				contentValues.put(DBConstants.GROUP_PARTICIPANT, groupParticipant);
				try
				{
					MessageMetadata messageMetadata = new MessageMetadata(new JSONObject(metadata), false);
					contentValues.put(DBConstants.IS_STATUS_MSG, messageMetadata.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE);
				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
				}

				mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.CONV_ID + "=" + convid, null);
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void updateReachedEndForCategory(String categoryId, boolean reachedEnd)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.REACHED_END, reachedEnd);

		mDb.update(DBConstants.STICKERS_TABLE, values, DBConstants.CATEGORY_ID + "=?", new String[] { categoryId });
	}

	public void addOrUpdateStickerCategory(String categoryId, int totalNum, boolean reachedEnd)
	{
		SQLiteStatement insertStatement = null;
		try
		{
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.STICKERS_TABLE + " ( " + DBConstants.CATEGORY_ID + ", " + DBConstants.TOTAL_NUMBER
					+ ", " + DBConstants.REACHED_END + ", " + DBConstants.UPDATE_AVAILABLE + " ) " + " VALUES (?, ?, ?, ?)");

			insertStatement.bindString(1, categoryId);
			insertStatement.bindLong(2, totalNum);
			insertStatement.bindLong(3, reachedEnd ? 1 : 0);
			insertStatement.bindLong(4, 0);

			insertStatement.execute();
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
		}
	}

	public void removeStickerCategory(String categoryId)
	{
		mDb.delete(DBConstants.STICKERS_TABLE, DBConstants.CATEGORY_ID + "=?", new String[] { categoryId });
	}

	public void stickerUpdateAvailable(String categoryId)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.UPDATE_AVAILABLE, true);

		mDb.update(DBConstants.STICKERS_TABLE, contentValues, DBConstants.CATEGORY_ID + "=?", new String[] { categoryId });
	}

	public EnumMap<StickerCategoryId, StickerCategory> stickerDataForCategories()
	{
		Cursor c = null;
		EnumMap<StickerCategoryId, StickerCategory> stickerDataMap = new EnumMap<StickerManager.StickerCategoryId, StickerCategory>(StickerCategoryId.class);
		try
		{
			c = mDb.query(DBConstants.STICKERS_TABLE, new String[] { DBConstants.CATEGORY_ID, DBConstants.UPDATE_AVAILABLE, DBConstants.REACHED_END }, null, null, null, null, null);
			while (c.moveToNext())
			{
				try
				{
					String category = c.getString(c.getColumnIndex(DBConstants.CATEGORY_ID));
					boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
					boolean reachedEnd = c.getInt(c.getColumnIndex(DBConstants.REACHED_END)) == 1;
					StickerCategoryId catId = StickerManager.StickerCategoryId.getCategoryIdFromName(category);
					StickerCategory s = new StickerCategory(catId, updateAvailable, reachedEnd);
					stickerDataMap.put(catId, s);
				}
				catch (Exception e)
				{

				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return stickerDataMap;
	}

	public boolean isStickerUpdateAvailable(StickerCategoryId categoryId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STICKERS_TABLE, new String[] { DBConstants.UPDATE_AVAILABLE }, DBConstants.CATEGORY_ID + "=?", new String[] { categoryId.name() }, null,
					null, null);
			if (!c.moveToFirst())
			{
				return false;
			}
			return c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public boolean hasReachedStickerEnd(String categoryId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STICKERS_TABLE, new String[] { DBConstants.REACHED_END }, DBConstants.CATEGORY_ID + "=?", new String[] { categoryId }, null, null, null);
			if (!c.moveToFirst())
			{
				return false;
			}
			return c.getInt(c.getColumnIndex(DBConstants.REACHED_END)) == 1;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void insertExpressionsStickerCategory()
	{
		addOrUpdateStickerCategory(StickerCategoryId.expressions.name(), StickerManager.getInstance().LOCAL_STICKER_RES_IDS_EXPRESSIONS.length, false);
	}

	public void insertHumanoidStickerCategory()
	{
		addOrUpdateStickerCategory(StickerCategoryId.humanoid.name(), StickerManager.getInstance().LOCAL_STICKER_RES_IDS_HUMANOID.length, false);
	}

	public long addProtip(Protip protip)
	{
		ContentValues contentValues = new ContentValues();

		contentValues.put(DBConstants.PROTIP_MAPPED_ID, protip.getMappedId());
		contentValues.put(DBConstants.HEADER, protip.getHeader());
		contentValues.put(DBConstants.PROTIP_TEXT, protip.getText());
		contentValues.put(DBConstants.IMAGE_URL, protip.getImageURL());
		contentValues.put(DBConstants.WAIT_TIME, protip.getWaitTime());
		contentValues.put(DBConstants.TIMESTAMP, protip.getTimeStamp());
		contentValues.put(DBConstants.PROTIP_GAMING_DOWNLOAD_URL, protip.getGameDownlodURL());
		return mDb.insert(DBConstants.PROTIP_TABLE, null, contentValues);
	}

	public Protip getLastProtip()
	{
		String[] columns = { "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.PROTIP_MAPPED_ID, DBConstants.HEADER, DBConstants.PROTIP_TEXT, DBConstants.IMAGE_URL,
				DBConstants.WAIT_TIME, DBConstants.TIMESTAMP, DBConstants.PROTIP_GAMING_DOWNLOAD_URL };

		return getProtip(columns, null, null);
	}

	public Protip getProtipForId(long id)
	{
		String[] columns = { DBConstants.ID, DBConstants.PROTIP_MAPPED_ID, DBConstants.HEADER, DBConstants.PROTIP_TEXT, DBConstants.IMAGE_URL, DBConstants.WAIT_TIME,
				DBConstants.TIMESTAMP, DBConstants.PROTIP_GAMING_DOWNLOAD_URL };
		String selection = DBConstants.ID + "=?";
		String[] selectionArgs = { Long.toString(id) };

		return getProtip(columns, selection, selectionArgs);
	}

	private Protip getProtip(String[] columns, String selection, String[] selectionArgs)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.PROTIP_TABLE, columns, selection, selectionArgs, null, null, null);
			if (!c.moveToFirst())
			{
				return null;
			}

			long id = c.getLong(c.getColumnIndex(DBConstants.ID));
			String mappedId = c.getString(c.getColumnIndex(DBConstants.PROTIP_MAPPED_ID));
			String header = c.getString(c.getColumnIndex(DBConstants.HEADER));
			String text = c.getString(c.getColumnIndex(DBConstants.PROTIP_TEXT));
			String url = c.getString(c.getColumnIndex(DBConstants.IMAGE_URL));
			long waitTime = c.getLong(c.getColumnIndex(DBConstants.WAIT_TIME));
			long timeStamp = c.getLong(c.getColumnIndex(DBConstants.TIMESTAMP));
			String gamingDownloadURL = c.getString(c.getColumnIndex(DBConstants.PROTIP_GAMING_DOWNLOAD_URL));
			if (mappedId == null)
			{
				return null;
			}

			return new Protip(id, mappedId, header, text, url, waitTime, timeStamp, gamingDownloadURL);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteProtip(String mappedId)
	{
		mDb.delete(DBConstants.PROTIP_TABLE, DBConstants.PROTIP_MAPPED_ID + "=?", new String[] { mappedId });
	}

	public void incrementUnreadCounter(String msisdn)
	{
		incrementUnreadCounter(msisdn, 1);
	}

	public void incrementUnreadCounter(String msisdn, int incrementValue)
	{
		String sqlString = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.UNREAD_COUNT + "=" + DBConstants.UNREAD_COUNT + " + " + incrementValue + " WHERE "
				+ DBConstants.MSISDN + "=" + DatabaseUtils.sqlEscapeString(msisdn);
		mDb.execSQL(sqlString);
	}

	public void initialiseSharedMediaAndFileThumbnailTable(SQLiteDatabase mDb)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA }, null, null, null, null, null);

			final int convIdIdx = c.getColumnIndex(DBConstants.CONV_ID);
			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metatdataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

			mDb.beginTransaction();

			while (c.moveToNext())
			{

				long convId = c.getLong(convIdIdx);
				long messageId = c.getLong(msgIdIdx);
				String metadata = c.getString(metatdataIdx);

				try
				{
					JSONObject metadataJson = new JSONObject(metadata);

					JSONArray fileJsonArray = metadataJson.optJSONArray(HikeConstants.FILES);

					if (fileJsonArray == null)
					{
						continue;
					}

					ContentValues sharedMediaValues = getSharedMediaContentValues(messageId, convId);
					mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, sharedMediaValues);

					JSONObject fileJson = fileJsonArray.getJSONObject(0);

					HikeFile hikeFile = new HikeFile(fileJson, false);

					if (hikeFile.getThumbnail() == null)
					{
						continue;
					}

					byte[] imageBytes = Base64.decode(hikeFile.getThumbnailString(), Base64.DEFAULT);

					ContentValues fileThumbnailValues = getFileThumbnailContentValues(hikeFile.getFileKey(), imageBytes);
					mDb.insert(DBConstants.FILE_THUMBNAIL_TABLE, null, fileThumbnailValues);

					fileJson.remove(HikeConstants.THUMBNAIL);

					ContentValues fileJsonUpdateValues = new ContentValues();
					fileJsonUpdateValues.put(DBConstants.MESSAGE_METADATA, metadataJson.toString());
					mDb.update(DBConstants.MESSAGES_TABLE, fileJsonUpdateValues, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(messageId) });

				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON");
				}
			}

			mDb.setTransactionSuccessful();

		}
		finally
		{
			mDb.endTransaction();
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void initialiseSharedMediaAndFileThumbnailTable()
	{
		initialiseSharedMediaAndFileThumbnailTable(mDb);
	}

	public void addSharedMedia(long messageId, long convId)
	{
		ContentValues sharedMediaValues = getSharedMediaContentValues(messageId, convId);

		mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, sharedMediaValues);
	}

	private ContentValues getSharedMediaContentValues(long messageId, long convId)
	{
		ContentValues sharedMediaValues = new ContentValues();
		sharedMediaValues.put(DBConstants.MESSAGE_ID, messageId);
		sharedMediaValues.put(DBConstants.CONV_ID, convId);

		return sharedMediaValues;
	}

	public void addFileThumbnail(String fileKey, byte[] imageBytes)
	{
		ContentValues fileThumbnailValues = getFileThumbnailContentValues(fileKey, imageBytes);

		mDb.insert(DBConstants.FILE_THUMBNAIL_TABLE, null, fileThumbnailValues);
	}

	private ContentValues getFileThumbnailContentValues(String fileKey, byte[] imageBytes)
	{
		ContentValues fileThumbnailValues = new ContentValues();
		fileThumbnailValues.put(DBConstants.FILE_KEY, fileKey);
		fileThumbnailValues.put(DBConstants.IMAGE, imageBytes);

		return fileThumbnailValues;
	}

	public Drawable getFileThumbnail(String fileKey)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.FILE_KEY + "=?", new String[] { fileKey }, null, null, null);

			if (!c.moveToFirst())
			{
				return null;
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return HikeBitmapFactory.getBitmapDrawable(mContext.getResources(), HikeBitmapFactory.decodeBitmapFromByteArray(icondata, Bitmap.Config.RGB_565));
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public String getReadByValueForMessageID(long msgId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.READ_BY }, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(msgId) }, null, null, null);
			if (!c.moveToFirst())
			{
				return null;
			}
			String readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
			return readByString;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Pair<String,Long> getReadByValueForGroup(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY,DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null, null, null);
			if (!c.moveToFirst())
			{
				return null;
			}
			String readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
			Long msgId = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
			return new Pair<String,Long>(readByString,msgId);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteAllProtipsBeforeThisId(long id)
	{
		mDb.delete(DBConstants.PROTIP_TABLE, DBConstants.ID + "< ?", new String[] { Long.toString(id) });
	}

	public void setChatBackground(String msisdn, String bgId, long timeStamp)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		values.put(DBConstants.BG_ID, bgId);
		values.put(DBConstants.TIMESTAMP, timeStamp);

		mDb.insertWithOnConflict(DBConstants.CHAT_BG_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public Pair<ChatTheme, Long> getChatThemeAndTimestamp(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CHAT_BG_TABLE, new String[] { DBConstants.TIMESTAMP, DBConstants.BG_ID }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null,
					null);
			if (c.moveToFirst())
			{
				ChatTheme chatTheme = ChatTheme.getThemeFromId(c.getString(c.getColumnIndex(DBConstants.BG_ID)));
				Long timeStamp = c.getLong(c.getColumnIndex(DBConstants.TIMESTAMP));

				return new Pair<ChatTheme, Long>(chatTheme, timeStamp);
			}
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ChatTheme getChatThemeForMsisdn(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CHAT_BG_TABLE, new String[] { DBConstants.BG_ID }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (c.moveToFirst())
			{
				try
				{
					return ChatTheme.getThemeFromId(c.getString(c.getColumnIndex(DBConstants.BG_ID)));
				}
				catch (IllegalArgumentException e)
				{
					/*
					 * For invalid theme id, we return the default id.
					 */
					return ChatTheme.DEFAULT;
				}
			}
			return ChatTheme.DEFAULT;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void removeChatThemeForMsisdn(String msisdn)
	{
		mDb.delete(DBConstants.CHAT_BG_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public void setChatThemesFromArray(JSONArray chatBackgroundArray)
	{
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.CHAT_BG_TABLE);
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.CHAT_BG_TABLE + " ( " + DBConstants.MSISDN + ", " + DBConstants.BG_ID + " ) "
					+ " VALUES (?, ?)");
			mDb.beginTransaction();

			if (chatBackgroundArray == null || chatBackgroundArray.length() == 0)
			{
				return;
			}
			for (int i = 0; i < chatBackgroundArray.length(); i++)
			{
				JSONObject chatBgJson = chatBackgroundArray.optJSONObject(i);

				if (chatBgJson == null)
				{
					continue;
				}

				String msisdn = chatBgJson.optString(HikeConstants.MSISDN);
				String bgId = chatBgJson.optString(HikeConstants.BG_ID);

				if (TextUtils.isEmpty(msisdn))
				{
					continue;
				}

				ChatTheme chatTheme = null;

				try
				{
					/*
					 * We don't support custom themes yet.
					 */
					if (chatBgJson.optBoolean(HikeConstants.CUSTOM))
					{
						throw new IllegalArgumentException();
					}

					chatTheme = ChatTheme.getThemeFromId(bgId);
				}
				catch (IllegalArgumentException e)
				{
					continue;
				}

				insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
				insertStatement.bindString(ih.getColumnIndex(DBConstants.BG_ID), bgId);

				insertStatement.executeInsert();

				HikeMessengerApp.getPubSub().publish(HikePubSub.CHAT_BACKGROUND_CHANGED, new Pair<String, ChatTheme>(msisdn, chatTheme));
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
			if (ih != null)
			{
				ih.close();
			}
			mDb.endTransaction();
		}
	}

	public void changeGroupOwner(String groupId, String msisdn)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.GROUP_OWNER, msisdn);

		mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	public void toggleStealth(String msisdn, boolean isStealth)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.IS_STEALTH, isStealth ? 1 : 0);

		mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public void addStealthMsisdnToMap()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.IS_STEALTH + "=1", null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				HikeMessengerApp.addStealthMsisdnToMap(c.getString(msisdnIdx));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ConvMessage showParticipantStatusMessage(String groupId)
	{

		Map<String, GroupParticipant> smsParticipants = getGroupParticipants(groupId, true, true);

		if (smsParticipants.isEmpty())
		{
			return null;
		}

		JSONObject dndJSON = new JSONObject();
		JSONArray dndParticipants = new JSONArray();

		for (Entry<String, GroupParticipant> smsParticipantEntry : smsParticipants.entrySet())
		{
			GroupParticipant smsParticipant = smsParticipantEntry.getValue();
			String msisdn = smsParticipantEntry.getKey();
			if (smsParticipant.onDnd())
			{
				dndParticipants.put(msisdn);
			}
		}

		if (dndParticipants.length() == 0)
		{
			// No DND participants. Just return
			return null;
		}
		try
		{
			dndJSON.put(HikeConstants.FROM, groupId);
			dndJSON.put(HikeConstants.TYPE, HikeConstants.DND);
			dndJSON.put(HikeConstants.DND_USERS, dndParticipants);

			ConvMessage convMessage = new ConvMessage(dndJSON, null, mContext, false);
			updateShownStatus(groupId);

			return convMessage;
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return null;
	}
	
	public int getUnreadPinCounter()
	{
		return 0;
	}

	public List<ConvMessage> getAllPinMessage(int startFrom, int limit, String msisdn, Conversation conv)
	{
		String limitStr = (limit == -1) ? null : new Integer(limit).toString();
		String startFromStr = (startFrom < 0) ? "0" : String.valueOf(startFrom);
		String selection = DBConstants.CONV_ID + " = ?" + " AND " + DBConstants.MESSAGE_TYPE + "==" + HikeConstants.MESSAGE_TYPE.TEXT_PIN;
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			String query = "SELECT " + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + "," + DBConstants.TIMESTAMP + "," + DBConstants.MESSAGE_ID + ","
					+ DBConstants.MAPPED_MSG_ID + "," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.IS_HIKE_MESSAGE + ","
					+ DBConstants.READ_BY + "," + DBConstants.MESSAGE_TYPE + " FROM " + DBConstants.MESSAGES_TABLE + " where " + selection + " order by " + DBConstants.MESSAGE_ID + " DESC LIMIT " + limitStr + " OFFSET "
					+ startFrom;
			c = mDb.rawQuery(query, new String[] { Long.toString(conv.getConvId()) });

			List<ConvMessage> elements = getMessagesFromDB(c, conv);
						
			return elements;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void markPinMessagesRead(List<ConvMessage> msgs)
	{

	}

	public void removeLastPinMessageForConv(String convId)
	{

	}

	public ArrayList<String> getOfflineMsisdnsList(String msisdnStatement)
	{
		Cursor c = null;
		ArrayList<String> msisdnResult = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE + "," + DBConstants.CONVERSATIONS_TABLE, new String[] {
					" MIN (" + DBConstants.MESSAGES_TABLE + "." + DBConstants.TIMESTAMP + ") AS TIME", DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN },
					DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID + " IN (SELECT " + DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.CONV_ID + " FROM "
							+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN + " IN " + msisdnStatement + " ) " + " AND "
							+ " ( " + DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID + "=" + DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.CONV_ID + " ) " + " AND  "
							+ DBConstants.MESSAGES_TABLE + "." + DBConstants.MSG_STATUS + "=" + State.SENT_CONFIRMED.ordinal() + " AND  " + DBConstants.MESSAGES_TABLE + "."
							+ DBConstants.IS_HIKE_MESSAGE + "=" + "1", null, DBConstants.CONVERSATIONS_TABLE + "." + DBConstants.MSISDN, null, null);

			if (c != null)
			{
				msisdnResult = new ArrayList<String>(c.getCount());
				while (c.moveToNext())
				{
					long msgTime = c.getLong(c.getColumnIndex("TIME"));
					if ((System.currentTimeMillis() / 1000 - msgTime) > HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME)
					{
						msisdnResult.add(c.getString(c.getColumnIndex(DBConstants.MSISDN)));
					}

					Logger.d("HikeToOffline", "TimeStamp : " + c.getLong(c.getColumnIndex("TIME")));
					Logger.d("HikeToOffline", "Msisdn : " + c.getString(c.getColumnIndex(DBConstants.MSISDN)));
				}
			}
		}
		catch (Exception e)
		{
			Logger.e("HikeToOffline", "Exception ", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return msisdnResult;
	}

	public void addMessageHashNMsisdnNReadByForGroup()
	{
		try
		{
			mDb.beginTransaction();
			addMessageHashAndMsisdn();
			updateReadByArrayForGroups();
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}
	
	public void upgradeForDatabaseVersion28()
	{
		try
		{
			mDb.beginTransaction();
			addMessageMsisdn();
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}
	
	private void addMessageMsisdn()
	{
		Cursor c = null;
		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);
			
			final int convIdIndex = c.getColumnIndex(DBConstants.CONV_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);
			
			while (c.moveToNext())
			{
				Integer convId = c.getInt(convIdIndex);
				String msisdn = c.getString(msisdnIndex);
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(),msisdn));
			}
			
			for (Pair<String, String> pair : convIdtoMsisdn)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MSISDN, pair.second);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { pair.first });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in adding msisdn ",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	private void addMessageHashAndMsisdn()
	{
		Cursor c = null;
		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);
			
			final int convIdIndex = c.getColumnIndex(DBConstants.CONV_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);
			
			while (c.moveToNext())
			{
				Integer convId = c.getInt(convIdIndex);
				String msisdn = c.getString(msisdnIndex);
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(),msisdn));
			}
			
			for (Pair<String, String> pair : convIdtoMsisdn)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MSISDN, pair.second);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { pair.first });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		
		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID, DBConstants.MSISDN }, null, null, null, null, null);
			
			final int messageIndex = c.getColumnIndex(DBConstants.MESSAGE);
			final int tsIndex = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int msgIdIndex = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int mappedMsgIdIndex = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);
			
			while (c.moveToNext())
			{
				String message = c.getString(messageIndex);
				int ts = c.getInt(tsIndex);
				int mappedId = c.getInt(mappedMsgIdIndex);
				String msisdn = c.getString(msisdnIndex);
				String messageHash = createMessageHash(msisdn, (long)mappedId, message, (long)ts);
				Integer msgId = c.getInt(msgIdIndex);
				
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_HASH, messageHash);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { msgId.toString() });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private void updateReadByArrayForGroups()
	{
		Cursor c = null;
		try
		{
			HashMap<String, String> groupIdMap = getAllGroupConversations();
			String convIdStatement = getConvIdStatement(groupIdMap.keySet());
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MAX (" + DBConstants.MESSAGE_ID + ") AS msgid", DBConstants.READ_BY, DBConstants.CONV_ID },
					DBConstants.CONV_ID + " IN " + convIdStatement + " AND " + DBConstants.MSG_STATUS + " = " + State.SENT_DELIVERED_READ.ordinal(), null, DBConstants.CONV_ID,
					null, null);

			final int convIdIdx = c.getColumnIndex(DBConstants.CONV_ID);
			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int readByIdx = c.getColumnIndex(DBConstants.READ_BY);

			while (c.moveToNext())
			{
				long messageId = c.getLong(msgIdIdx);
				long convId = c.getLong(convIdIdx);
				String readByString = c.getString(readByIdx);
				String groupId = groupIdMap.get(Long.toString(convId));

				Logger.d("readByValues", "conVid : " + convId + " messageId : " + messageId + " groupId : " + groupId + " readby : " + readByString);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_ID, messageId);
				contentValues.put(DBConstants.READ_BY, readByString);
				mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public HashMap<String, String> getAllGroupConversations()
	{
		Cursor c = null;
		String msisdn;
		HashMap<String, String> groupIdMap = new HashMap<String, String>();

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);
			while (c.moveToNext())
			{
				msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				if (Utils.isGroupConversation(msisdn))
				{
					groupIdMap.put(Integer.toString(c.getInt(c.getColumnIndex(DBConstants.CONV_ID))), msisdn);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return groupIdMap;
	}

	private String getConvIdStatement(Collection<String> collection)
	{

		StringBuilder sb = new StringBuilder("(");
		;
		for (String convId : collection)
		{

			sb.append(DatabaseUtils.sqlEscapeString(convId));

			sb.append(",");

		}
		sb.replace(sb.lastIndexOf(","), sb.length(), ")");

		return sb.toString();

	}
	
	/**
	 * Generates a list of messages based on the query passed to it.
	 * @param c
	 * 			The query on the message table.
	 * @param conversation
	 * 			Conversation for which the messages are to be fetched.
	 * @return The list on ConvMessage objects.
	 */
	private List<ConvMessage> getMessagesFromDB(Cursor c, Conversation conversation)
	{
		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		final int isHikeMessageColumn = c.getColumnIndex(DBConstants.IS_HIKE_MESSAGE);
		final int readByColumn = c.getColumnIndex(DBConstants.READ_BY);
		final int typeColumn = c.getColumnIndex(DBConstants.MESSAGE_TYPE);

		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());

		while (c.moveToNext())
		{
			int hikeMessage = c.getInt(isHikeMessageColumn);
			boolean isHikeMessage = hikeMessage == -1 ? conversation.isOnhike() : (hikeMessage == 0 ? false : true);

			ConvMessage message = new ConvMessage(c.getString(msgColumn), conversation.getMsisdn(), c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
					c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), !isHikeMessage, c.getInt(typeColumn));
			String metadata = c.getString(metadataColumn);
			try
			{
				message.setMetadata(metadata);
			}
			catch (JSONException e)
			{
				Logger.w(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			message.setReadByArray(c.getString(readByColumn));
			elements.add(elements.size(), message);
			message.setConversation(conversation);
		}
		return elements;
	}
	
	/**
	 * Updates group info table with last sent message id for a group. It also clears readby column for that group
	 * @param convMessage
	 */
	private void updateReadBy(ConvMessage convMessage)
	{
		if (Utils.isGroupConversation(convMessage.getMsisdn()) && convMessage.isSent())
		{
			String readByString = null;
			ContentValues contentValues = new ContentValues();
			contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
			contentValues.put(DBConstants.READ_BY, readByString);
			mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { convMessage.getMsisdn() });
		}
	}
	
	/**
	 * This function queries message table for max mr id from list of ids that are sent by me with msisdn as groupid 
	 * @param groupId
	 * 			-- group id for which mr packet came
	 * @param ids
	 * 			-- list of message ids that came in "mr" packet
	 * @return maxMrId from list of ids that are sent by me else -1
	 */
	public long getMrIdForGroup(String groupId, long ids[])
	{
		if(ids == null || ids.length == 0)
		{
			return -1;
		}
		StringBuilder sb = new StringBuilder("(");
		
		for (int i = 0; i < ids.length; i++)
		{		
			sb.append(ids[i]);
			if (i != ids.length - 1)
			{
				sb.append(",");
			}
		}
		sb.append(")");
		
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MAX (" + DBConstants.MESSAGE_ID + ") AS msgid" }, DBConstants.CONV_ID + " = (SELECT " + DBConstants.CONV_ID + " FROM "
					+ DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.MSISDN + "=? ) AND "
					+ DBConstants.MESSAGE_ID + " IN " + sb.toString(), new String[] { groupId }, null, null, null);
			if(c.moveToFirst())
			{
				return c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
			}
			return -1;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

}
