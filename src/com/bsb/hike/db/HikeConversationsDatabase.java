package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.DBConstants.HIKE_CONV_DB;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ConversationMsisdns;
import com.bsb.hike.modules.contactmgr.GroupDetails;
import com.bsb.hike.platform.ContentLove;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper implements DBConstants, HIKE_CONV_DB
{

	private SQLiteDatabase mDb;

	private static volatile HikeConversationsDatabase hikeConversationsDatabase;

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
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.MESSAGES_TABLE
				+ " ( "
				+ DBConstants.MESSAGE + " STRING, " // The message text
				+ DBConstants.MSG_STATUS + " INTEGER, " // Whether the message is sent or not. Plus also tells us the current state of the message.
				+ DBConstants.TIMESTAMP + " INTEGER, " // Message time stamp
				+ DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // The message id (Unique)
				+ DBConstants.MAPPED_MSG_ID + " INTEGER, " // The message id of the message on the sender's side (Only applicable for received messages)
				+ DBConstants.CONV_ID + " INTEGER," // Deprecated
				+ DBConstants.MESSAGE_METADATA + " TEXT, " // Extra info of the message. Stored in JSON format
				+ DBConstants.GROUP_PARTICIPANT + " TEXT, " // The MSISDN of the participant that sent the message (Only for groups)
				+ DBConstants.IS_HIKE_MESSAGE + " INTEGER DEFAULT -1, " // Whether the message is a hike or SMS message.
				+ DBConstants.READ_BY + " TEXT, " // Deprecated
				+ DBConstants.MSISDN + " TEXT, " // The conversation's msisdn. This will be the msisdn for one-to-one and the group id for groups
				+ DBConstants.MESSAGE_HASH + " TEXT DEFAULT NULL, " // Used for duplication checks.
				+ DBConstants.MESSAGE_TYPE + " INTEGER" + " INTEGER DEFAULT -1, " // The type of the message.
				+ DBConstants.HIKE_CONV_DB.LOVE_ID_REL + " INTEGER DEFAULT -1, " // love id applicable to few messages like content
				+ DBConstants.HIKE_CONTENT.CONTENT_ID + " INTEGER DEFAULT -1, " // content id applicable to few messages like content
				+ DBConstants.HIKE_CONTENT.NAMESPACE + " TEXT DEFAULT 'message',"  //namespace for uniqueness of content
				+ DBConstants.SERVER_ID + " INTEGER, "
				+ DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0" //normal/broadcast/multi-forward
				+ " ) ";

		db.execSQL(sql);

		sql = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_CONTENT_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.CONTENT_ID + " ) ";
		db.execSQL(sql);

		sql = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_NAMESPACE_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.NAMESPACE + " ) ";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CONVERSATION_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.CONV_ID + " , " + DBConstants.TIMESTAMP
				+ " DESC" + " )";
		db.execSQL(sql);

		createIndexOverServerIdField(db);

		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.MESSAGE_HASH_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_HASH + " DESC" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CONVERSATIONS_TABLE
				+ " ( "
				+ DBConstants.CONV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Deprecated
				+ DBConstants.ONHIKE + " INTEGER, " // Whether the conversation is on hike or not
				+ DBConstants.CONTACT_ID + " STRING, " // Deprecated
				+ DBConstants.MSISDN + " UNIQUE, " // The conversation's msisdn. This will be the msisdn for one-to-one and the group id for groups
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " // Flag. Whether to show the SMS Credits overlay or not.

				// Messages table columns begin (We keep the last entry of messages for a conversation)
				+ DBConstants.MESSAGE + " STRING, "
				+ DBConstants.MSG_STATUS + " INTEGER, "
				+ DBConstants.LAST_MESSAGE_TIMESTAMP + " INTEGER, "
				+ DBConstants.MESSAGE_ID + " INTEGER, "
				+ DBConstants.MAPPED_MSG_ID + " INTEGER, "
				+ DBConstants.MESSAGE_METADATA + " TEXT, "
				+ DBConstants.GROUP_PARTICIPANT + " TEXT, "
				+ DBConstants.SERVER_ID + " INTEGER, "
				+ DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0, " // last message origin type
				// Messages table columns end

				+ DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0, " // Whether the message is a status message.
				+ DBConstants.UNREAD_COUNT + " INTEGER DEFAULT 0, " // The unread count for the conversation
				+ DBConstants.IS_STEALTH + " INTEGER DEFAULT 0, " // Whether the conversation is a hidden conversation.
				+ DBConstants.SORTING_TIMESTAMP + " LONG, " //This timestamp will be used for sorting conversation objects
				+ DBConstants.CONVERSATION_METADATA + " TEXT" // Extra info. JSON format
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID + " STRING, " // The group id.
				+ DBConstants.MSISDN + " TEXT, " // Msisdn of the group participant
				+ DBConstants.NAME + " TEXT, " // Name of the participant
				+ DBConstants.ONHIKE + " INTEGER, " // Whether the participant is on hike or not.
				+ DBConstants.HAS_LEFT + " INTEGER, " // Whether the participant has left the group or not.
				+ DBConstants.ON_DND + " INTEGER, " // Whether the participant is on DND or not
				+ DBConstants.SHOWN_STATUS + " INTEGER " // Whether we have shown a DND status for this participant or not. 
				+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN
				+ " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_INFO_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID + " STRING PRIMARY KEY, " // The group id
				+ DBConstants.GROUP_NAME + " TEXT, " // Name of the group
				+ DBConstants.GROUP_OWNER + " TEXT, " // Group owner's msisdn
				+ DBConstants.GROUP_ALIVE + " INTEGER, " // Whether the group is alive or not
				+ DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0, " // Whether the group is muted or not
				+ DBConstants.READ_BY + " TEXT, " // An array of the msisdns that have read the message.
				+ DBConstants.MESSAGE_ID + " INTEGER" // The message id of the message we are showing the read by for.
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE
				+ " ( "
				+ DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, " // The index of the emoticon
				+ DBConstants.LAST_USED + " INTEGER" // Timestamp of when it was used
				+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX + " ON " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " ) ";
		db.execSQL(sql);

		sql = getStatusTableCreationStatement();
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.STATUS_INDEX + " ON " + DBConstants.STATUS_TABLE + " ( " + DBConstants.MSISDN + " ) ";
		db.execSQL(sql);

		sql = getStickerCategoryTableCreateQuery();
		db.execSQL(sql);

		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.PROTIP_TABLE
				+ " ("
				+ DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Protip id
				+ DBConstants.PROTIP_MAPPED_ID + " TEXT UNIQUE, " // Protip id sent by the server
				+ DBConstants.HEADER + " TEXT, "  // Protip header text
				+ DBConstants.PROTIP_TEXT + " TEXT, " // Protip subtext
				+ DBConstants.TIMESTAMP + " INTEGER, " // Protip time stamp
				+ DBConstants.IMAGE_URL + " TEXT, " // Protip image URL
				+ DBConstants.WAIT_TIME + " INTEGER, " // When to show the Tip
				+ DBConstants.PROTIP_GAMING_DOWNLOAD_URL + " TEXT"
				+ " )";
		db.execSQL(sql);
		sql = getSharedMediaTableCreateQuery();
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_TABLE
				+ " ("
				+ DBConstants.FILE_KEY + " TEXT PRIMARY KEY, " // File key
				+ DBConstants.IMAGE + " BLOB," // File thumbnail
                + REF_COUNT + " INTEGER"      // number of messages using the thumbnail.
				+ " )";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_INDEX + " ON " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CHAT_BG_TABLE
				+ " ("
				+ DBConstants.MSISDN + " TEXT UNIQUE, " // Msisdn or group id
				+ DBConstants.BG_ID + " TEXT, " // Chat theme id
				+ DBConstants.TIMESTAMP + " INTEGER" // Timestamp when this them was changed.
				+ ")";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CHAT_BG_INDEX + " ON " + DBConstants.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(sql);
		// CONTENT LOVE TABLE
		sql = CREATE_TABLE + LOVE_TABLE + "(" + _ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + LOVE_ID
				+ " INTEGER, " + COUNT + " INTEGER, " + USER_STATUS
				+ " INTEGER, " + REF_COUNT + " INTEGER, "
				+ HIKE_CONV_DB.TIMESTAMP + " INTEGER" + ")";
		sql = getStickerShopTableCreateQuery();
		db.execSQL(sql);

		sql = CREATE_TABLE + DBConstants.BOT_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.NAME + " TEXT, " + DBConstants.CONVERSATION_METADATA + " TEXT, "+ DBConstants.IS_MUTE + " INTEGER DEFAULT 0)";
		db.execSQL(sql);

	}
	private void createIndexOverServerIdField(SQLiteDatabase db)
	{
		//creating index over server Id field
		String sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.SERVER_ID_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.SERVER_ID + " DESC" + " )";
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
		mDb.delete(DBConstants.STICKER_CATEGORIES_TABLE, null, null);
		mDb.delete(DBConstants.STICKER_SHOP_TABLE, null, null);
		mDb.delete(DBConstants.PROTIP_TABLE, null, null);
		mDb.delete(DBConstants.SHARED_MEDIA_TABLE, null, null);
		mDb.delete(DBConstants.FILE_THUMBNAIL_TABLE, null, null);
		mDb.delete(DBConstants.CHAT_BG_TABLE, null, null);
		mDb.delete(DBConstants.BOT_TABLE, null, null);
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
			// Drop table is required for user upgrading from build numbers: 8, 9, 10 or 11
			String drop = "DROP TABLE IF EXISTS " + DBConstants.STATUS_TABLE;
			String create = getStatusTableCreationStatement();

			String alter1 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MSG_STATUS + " INTEGER";
			String alter3 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.TIMESTAMP + " INTEGER";
			String alter4 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ID + " INTEGER";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MAPPED_MSG_ID + " INTEGER";
			String alter6 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_METADATA + " TEXT";
			String alter7 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.GROUP_PARTICIPANT + " TEXT";
			String alter8 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0";

			db.execSQL(drop);
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

		// No need to make sticker cateogory table here. We are making a new one in version 30
		//if (oldVersion < 15)
		//{
		//	String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKERS_TABLE + " (" + DBConstants.CATEGORY_ID + " TEXT PRIMARY KEY, " + DBConstants.TOTAL_NUMBER
		//			+ " INTEGER, " + DBConstants.REACHED_END + " INTEGER," + DBConstants.UPDATE_AVAILABLE + " INTEGER" + " )";
		//	db.execSQL(create);
		//}
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

		/*
		 * Version 29 removes the empty one-to-one conversations
		 */
		if (oldVersion < 29)
		{
			deleteEmptyConversations(db);
		}
		
		/*
		 * Version 30 fixes a Play Store crash where some DBs did not have a moodId column.
		 * We will first query the table and check if this column exists, if not we will drop
		 * the table and create a new one
		 */
		if (oldVersion < 30)
		{
			if (!checkIfStatusTableIsValid(db))
			{
				dropAndRecreateStatusTable(db);
			}
		}

		if (oldVersion < 31)
		{
			db.execSQL(getStickerCategoryTableCreateQuery());
			db.execSQL(getStickerShopTableCreateQuery());
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 31
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1);
			editor.putInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1);
			editor.commit();
		}

		/**
		 * We introduced content here, so love concept came in message table for content shared by people. Ref count is introduced to keep a track on the number of messages where
		 * this thumbnail is attached.
		 */
		if (oldVersion < 32)
		{
			String alter2 = "ALTER TABLE " + DBConstants.FILE_THUMBNAIL_TABLE + " ADD COLUMN " + REF_COUNT + " INTEGER";
			db.execSQL(alter2);

			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.HIKE_CONV_DB.LOVE_ID_REL + " INTEGER";
			db.execSQL(alter);
		}

		if (oldVersion < 33)
		{
			String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.BOT_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.NAME + " TEXT, "
					+ DBConstants.CONVERSATION_METADATA + " TEXT)";
			db.execSQL(sql);

		}


		if (oldVersion < 35)
		{
			String alter = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.IS_MUTE + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		if(oldVersion < 36)
		{
			String alterContentId = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.HIKE_CONTENT.CONTENT_ID + " INTEGER DEFAULT -1"; // content id applicable to few messages
			db.execSQL(alterContentId);

			String alterNamespace = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + HIKE_CONTENT.NAMESPACE + " TEXT DEFAULT 'message'"; // namespace for uniqueness applicable to few messages
			db.execSQL(alterNamespace);
			String alter1 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			String alter2 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0";
			String alter3 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			//We are adding a new field sorting timestamp to use this for ordering purpose.
			String alter4 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.SORTING_TIMESTAMP + " LONG";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0";
			String alter6 = "ALTER TABLE " + DBConstants.SHARED_MEDIA_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			String createIndex1 = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_CONTENT_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.CONTENT_ID + " ) ";
			String createIndex2 = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_NAMESPACE_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.NAMESPACE + " ) ";
			
			db.execSQL(alter1);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			db.execSQL(alter6);
			db.execSQL(createIndex1);
			db.execSQL(createIndex2);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 1);
		}
	}

	public void reinitializeDB()
	{
		close();
		hikeConversationsDatabase = new HikeConversationsDatabase(HikeMessengerApp.getInstance());
		/*
		 * We can remove this line, if we can guarantee, NoOne keeps a local copy of HikeConversationsDatabase. 
		 * right now we store convDb reference in some classes and use that refenence to query db. ex. DbConversationListener. 
		 * i.e. on restore we have two objects of HikeConversationsDatabase in memory.
		 */
		mDb = hikeConversationsDatabase.getMdb(); 
	}
	
	private SQLiteDatabase getMdb()
	{
		return mDb;
	}

	public void clearTable(String table)
	{
		mDb.delete(table, null, null);
	}

	private String getStatusTableCreationStatement()
	{
		return "CREATE TABLE IF NOT EXISTS " + DBConstants.STATUS_TABLE
				+ " ("
				+ DBConstants.STATUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Status id
				+ DBConstants.STATUS_MAPPED_ID + " TEXT UNIQUE, " // Status id sent by the server
				+ DBConstants.MSISDN + " TEXT, " // Msisdn of the person who generated the status
				+ DBConstants.STATUS_TEXT + " TEXT, " // Text of the status
				+ DBConstants.STATUS_TYPE + " INTEGER, " // Type of status
				+ DBConstants.TIMESTAMP + " INTEGER, " // Time stamp of status
				+ DBConstants.MESSAGE_ID + " INTEGER DEFAULT 0, " // Message id of the message this status generated in the messages table. Only valid if status is received when a one to one conversation exists.
				+ DBConstants.SHOW_IN_TIMELINE + " INTEGER, " // Whether this status should be shown in the timeline or not.
				+ DBConstants.MOOD_ID + " INTEGER, " // The mood id of the status
				+ DBConstants.TIME_OF_DAY + " INTEGER" // Deprecated.
				+ " )";
	}

	/**
	 * This method checks if the status table has a mood id column. We had to add this to fix a bug
	 * where this column did not exist for a few users.
	 * @param db
	 * @return
	 */
	private boolean checkIfStatusTableIsValid(SQLiteDatabase db)
	{
		Cursor c = null;
		try
		{
			c = db.query(DBConstants.STATUS_TABLE, new String[] { DBConstants.MOOD_ID }, null, null, null, null, null);
			return true;
		}
		catch (SQLiteException e)
		{
			Logger.w(getClass().getSimpleName(), "Mood id column does not exist");
			return false;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private void dropAndRecreateStatusTable(SQLiteDatabase db)
	{
		String drop = "DROP TABLE IF EXISTS " + DBConstants.STATUS_TABLE;
		String create = getStatusTableCreationStatement();

		db.execSQL(drop);
		db.execSQL(create);
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
	 *
	 * @param message
	 *            - message to be added to database
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

	public int updateMsgStatusBetween(long startMsgId, long endMsgId, int val, String msisdn)
	{
		String initialWhereClause = DBConstants.MESSAGE_ID + " BETWEEN " + startMsgId + " AND " + endMsgId;

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, val, msisdn);
	}

	public ArrayList<Long> getCurrentUnreadMessageIdsForMsisdn(String msisdn, long maxMsgId)
	{
		ArrayList<Long> ids = new ArrayList<Long>();

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID,  DBConstants.MSG_STATUS, DBConstants.MESSAGE_ORIGIN_TYPE }, DBConstants.MSISDN + "=? AND "+ DBConstants.MESSAGE_ID + "<=? AND " + DBConstants.MSG_STATUS + "<"
					+ State.SENT_DELIVERED_READ.ordinal(), new String[] { msisdn, String.valueOf(maxMsgId) }, null, null, null);

			while (c.moveToNext())
			{
				long id = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				int msgStatus = c.getInt(c.getColumnIndex(DBConstants.MSG_STATUS));
				int msgOriginType = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ORIGIN_TYPE));
				/*
				 * 1. if message is not a broadcast one than its fine this is message is a right candidate for
				 * force R.
				 * 2. if message is broadcast then only if its status is Delivered we chhose this as a candidate for
				 * force R.
				 */
				if (ConvMessage.originTypeValue(msgOriginType) != OriginType.BROADCAST
						|| ConvMessage.stateValue(msgStatus) == State.SENT_DELIVERED)
				{
					ids.add(id);
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
		return ids;
	}

	public ArrayList<Long>  setAllDeliveredMessagesReadForMsisdn(String msisdn, ArrayList<Long> msgIds)
	{
		long maxMsgId = Utils.getMaxLongValue(msgIds);
		ArrayList<Long> messageIdsToBeUpdated = getCurrentUnreadMessageIdsForMsisdn(msisdn, maxMsgId);
		
		if(messageIdsToBeUpdated == null || messageIdsToBeUpdated.isEmpty())
		{
			return null;
		}
		
		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + Utils.valuesToCommaSepratedString(messageIdsToBeUpdated);

		int status = State.SENT_DELIVERED_READ.ordinal();

		String query = initialWhereClause;

		executeUpdateMessageStatusStatement(query, status, msisdn);

		return messageIdsToBeUpdated;
	}

	/**
	 *
	 * @param groupId
	 *            -- groupId of group for which mr packet came
	 * @param ids
	 *            -- list of ids present in mr packet
	 * @param msisdn
	 *            -- partipant msisdn from which mr packet came
	 * @return id -- maxMsgId from list of ids that are sent by user. If ids doesn't contains any id sent by user return -1
	 */
	public long setReadByForGroup(String groupId, ArrayList<Long> ids, String msisdn)
	{
		Cursor c = null;
		Cursor conversationCursor = null;
		try
		{
			conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null,
					null, null);

			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
					null, null, null);

			if (!conversationCursor.moveToFirst())
			{
				return -1;

			}

			if (c.moveToFirst())
			{
				mDb.beginTransaction();
				String readByString = null;
				long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

				ContentValues contentValues = new ContentValues();

				long maxMsgId = getMrIdForGroup(groupId, ids);			// get max sent message id from list of ids

				if(maxMsgId >= msgId)			// we are updating readBy string if max message id is greater than or equal to that present in group info table
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
				if (maxMsgId > 0)
				{

					long conversationMsgId = conversationCursor.getLong(conversationCursor.getColumnIndex(DBConstants.MESSAGE_ID));
					int minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
					int maxStatusOrdinal = State.SENT_DELIVERED_READ.ordinal();

					/*
					 * Making sure we only set the status of sent messages.
					 */
					contentValues.clear();
					String whereClause = DBConstants.MESSAGE_ID + " <= " + maxMsgId + " AND " + DBConstants.MSG_STATUS + " > " + minStatusOrdinal + " AND "
							+ DBConstants.MSG_STATUS + " < " + maxStatusOrdinal + " AND " + DBConstants.MSISDN + "=?";
					contentValues.put(DBConstants.MSG_STATUS, State.SENT_DELIVERED_READ.ordinal());
					mDb.update(DBConstants.MESSAGES_TABLE, contentValues, whereClause, new String[] { groupId });

					if (conversationMsgId == maxMsgId)
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
		for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet()) // Iterate through status map
		{

			String groupId = entry.getKey();
			PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
			maxMsgId = pair.getFirst().getFirst();

			if (maxMsgId == -1)
			{
				continue;
			}
			try
			{
				c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
						null, null, null);

				conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null,
						null, null);

				if (!conversationCursor.moveToFirst())
				{
					continue;
				}
				if (c.moveToFirst())
				{
					String readByString = null;
					long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

					if (maxMsgId >= msgId)
					{
						readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
					}
					else
					{
						continue;
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
					for (String msisdn : pair.getFirst().getSecond())
					{
						boolean alreadyAdded = false;
						int length = readByArray.length();

						for (int i = 0; i < length; i++)
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
					if (conversationMsgId == maxMsgId)
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
		if (msgId == -1)
		{
			return;
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
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?  AND " + DBConstants.MSG_STATUS + ">=" + minStatusOrdinal
					+ " AND " + DBConstants.MSG_STATUS + "<" + maxStatusOrdinal + " AND " + DBConstants.MESSAGE_ID + "<=" + msgId, new String[] { msisdn }, null, null, null);

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

		updateStatement = updateStatement + " AND " + DBConstants.MSG_STATUS + " >= " + minStatusOrdinal + " AND " + DBConstants.MSG_STATUS + " <= " + maxStatusOrdinal
				+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.MSISDN + " =" + DatabaseUtils.sqlEscapeString(msisdn)) : "");

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

	public void updateMessageMetadata(long serverID, MessageMetadata metadata)
	{
		String thumbnailString = extractThumbnailFromMetadata(metadata);

		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MESSAGE_METADATA, metadata.serialize());
		try
		{
			mDb.beginTransaction();
			mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[] { String.valueOf(serverID) });
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.SERVER_ID + "=? AND " + DBConstants.IS_STATUS_MSG + " = 0", new String[] { String.valueOf(serverID) });
			updateSharedMediaTableMetadata(serverID, metadata);
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception in updateMessageMetadata: ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
		addThumbnailStringToMetadata(metadata, thumbnailString);
	}

	private void updateSharedMediaTableMetadata(long serverId, MessageMetadata metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		putMetadataAccordingToFileType(contentValues, metadata, false);
		if(!contentValues.containsKey(DBConstants.MESSAGE_METADATA))
		{
			return;
		}
		mDb.update(DBConstants.SHARED_MEDIA_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[] { String.valueOf(serverId) });
	}

	public void updateConversationMetadata(String msisdn, Conversation.MetaData metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.CONVERSATION_METADATA, metadata.toString());
		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_META_DATA_UPDATED, metadata);
	}

	private void bindConversationInsert(SQLiteStatement insertStatement, ConvMessage conv,boolean bindForConvId)
	{

		final int msgOriginTypeColumn = 1;
		final int messageColumn = 2;
		final int msgStatusColumn = 3;
		final int timestampColumn = 4;
		final int mappedMsgIdColumn = 5;
		final int messageMetadataColumn = 6;
		final int groupParticipant = 7;
		final int isHikeMessageColumn = 8;
		final int messageHash = 9;
		final int typeColumn = 10;
		final int msgMsisdnColumn = 11;
		final int contentIdColumn = 12;
		final int nameSpaceColumn = 13;
		final int msisdnColumn = 14;

		insertStatement.clearBindings();
		insertStatement.bindString(messageColumn, conv.getMessage());
		// 0 -> SENT_UNCONFIRMED ; 1 -> SENT_CONFIRMED ; 2 -> RECEIVED_UNREAD ;
		// 3 -> RECEIVED_READ
		insertStatement.bindLong(msgStatusColumn, conv.getState().ordinal());
		insertStatement.bindLong(timestampColumn, conv.getTimestamp());
		insertStatement.bindLong(mappedMsgIdColumn, conv.getMappedMsgID());
		if(bindForConvId){
		insertStatement.bindString(msisdnColumn, conv.getMsisdn());
		}
        if(conv.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT) {
            insertStatement.bindString(messageMetadataColumn, conv.platformMessageMetadata != null ? conv.platformMessageMetadata.JSONtoString() : "");

        } else if(conv.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || conv.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT) {
			insertStatement.bindString(messageMetadataColumn, conv.webMetadata != null ? conv.webMetadata.JSONtoString() : "");

		}else
        {
            insertStatement.bindString(messageMetadataColumn, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
        }

		insertStatement.bindLong(isHikeMessageColumn, conv.isSMS() ? 0 : 1);
		insertStatement.bindString(groupParticipant, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");
		String msgHash = createMessageHash(conv);
		if (msgHash != null)
		{
			insertStatement.bindString(messageHash, msgHash);
		}
		insertStatement.bindLong(typeColumn, conv.getMessageType());
		insertStatement.bindString(msgMsisdnColumn, conv.getMsisdn());
		insertStatement.bindLong(contentIdColumn, conv.getContentId());
		insertStatement.bindString(nameSpaceColumn, conv.getNameSpace() != null ? conv.getNameSpace() : "");
		insertStatement.bindLong(msgOriginTypeColumn, conv.getMessageOriginType().ordinal());
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
	 *
	 * @param msg
	 *            The message for which hash is to be created.
	 * @return The message hash string .
	 */
	private String createMessageHash(ConvMessage msg)
	{
		String msgHash = null;
		if (!msg.isSent() && (msg.getParticipantInfoState() == ParticipantInfoState.NO_INFO))
		{
			msgHash = msg.getMsisdn() + msg.getMappedMsgID();

			String message = msg.getMessage();
			if(message !=null && message.length() > 0)
			{
				msgHash = msgHash + message.charAt(0) + message.charAt(message.length() - 1);
			}
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

	public boolean addConversations(List<ConvMessage> convMessages)
	{
		if(convMessages.isEmpty())
		{
			return false;
		}
		ArrayList<ContactInfo> contacts= new ArrayList<ContactInfo>(1);
		String msisdn = convMessages.get(0).getMsisdn();
		contacts.add(new ContactInfo(msisdn, msisdn, null, null,!convMessages.get(0).isSMS()));

		return addConversations(convMessages, contacts,true);
	}

	/**
	 *
	 * @param convMessages
	 *            -- list of messages to be added to database
	 * @return <li><b>true</b> if messages successfully added to database</li> <li><b>false</b> if messages are not inserted to database possibly due to duplicate</li>
	 */
	public boolean addConversations(List<ConvMessage> convMessages, ArrayList<ContactInfo> contacts,boolean createConvIfNotExist)
	{
        SQLiteStatement insertStatement = getSqLiteStatementToInsertIntoMessagesTable(createConvIfNotExist);

		try
		{
			mDb.beginTransaction();

			long msgId = -1;

			int unreadMessageCount = 0;

            Map<String, Pair<List<String>, Long>> map = new HashMap<String, Pair<List<String>, Long>>();
			int totalMessage = convMessages.size()-1;
			long baseId = -1;
			for (ContactInfo contact : contacts)
			{
				for (int i=0;i<=totalMessage;i++)
				{
					ConvMessage conv  = convMessages.get(i);
					conv.setSMS(!contact.isOnhike());
					conv.setMsisdn(contact.getMsisdn());
					String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());
					
					long sortingTimeStamp = conv.getTimestamp();
					if(conv.getTimestamp() <= 0)
					{
						sortingTimeStamp = System.currentTimeMillis()/1000;
					}
					
					long lastMessageTimeStamp = sortingTimeStamp;

					bindConversationInsert(insertStatement, conv,createConvIfNotExist);

					/*
					 * In case message is duplicate insert statement will throw exception . It will catch that exception and will return false denoting duplicate message case
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
					 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the
					 * conversation does not exist we do not add a conversation.
					 */
					if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn()))
					{

						addConversation(conv.getMsisdn(), !conv.isSMS(), null, null, conv);
						bindConversationInsert(insertStatement, conv,createConvIfNotExist);
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

						assert (msgId >= 0);
					}
					if(baseId==-1){
						baseId = msgId;
					}

					conv.setMsgID(msgId);
					insertServerId(conv);
					ChatThread.addtoMessageMap(conv);

					/*
					 * msdId > 0 means that the conversation exists.
					 */
					if (conv.isFileTransferMessage() && msgId > 0)
					{
						addSharedMedia(conv);
					}
					if (Utils.shouldIncrementCounter(conv))
					{
						unreadMessageCount++;
					}
					/*
					 * Updating the conversations table only for last message for each contact
					 */
					if(i==totalMessage){
					// incrementing timestamp to save different timestamp for each conversation so that we can fetch in order
					ContentValues contentValues = getContentValueForConversationMessage(conv,lastMessageTimeStamp, sortingTimeStamp++);

					//TODO proper check for broadcast message
					if(conv.isBroadcastMessage() && !conv.isBroadcastConversation())
					{
						//We donot update sorting timestamp value if this is broadcast message in normal 1-1 chat
						contentValues.remove(DBConstants.SORTING_TIMESTAMP);
					}
					mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() });
					}
				// upgrade groupInfoTable
				updateReadBy(conv);
				if (Utils.isGroupConversation(conv.getMsisdn()))
				{
					long timestamp = 0;
					Pair<List<String>, Long> pair = map.get(conv.getMsisdn());
					if (pair != null)
						timestamp = pair.second;
					if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
					{
						timestamp = conv.getTimestamp();
					}
					List<String> lastMsisdns = new ArrayList<String>();
					if (conv.getMetadata() != null)
						lastMsisdns = getGroupLastMsgMsisdn(conv.getMetadata().getJSON());
					if (lastMsisdns.size() == 0 && null != conv.getGroupParticipantMsisdn())
					{
						lastMsisdns.add(conv.getGroupParticipantMsisdn());
					}
					map.put(conv.getMsisdn(), new Pair<List<String>, Long>(lastMsisdns, timestamp));
				}
				}

				incrementUnreadCounter(contact.getMsisdn(), unreadMessageCount);
			}
			// setting base DB id as whole packet logic for multi forward depends on base ID, i.e : first contact -> first message 
			convMessages.get(0).setMsgID(baseId);
			mDb.setTransactionSuccessful();
			ContactManager.getInstance().removeOlderLastGroupMsisdns(map);
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
	 *            -- list of messages came in bulk packet
	 * @return <li><b>list</b> of non duplicate messages successfully added to database</li>
	 *
	 */
	public LinkedList<ConvMessage> addConversationsBulk(List<ConvMessage> convMessages)
	{
		Logger.d("bulkPacket", "adding conversation started");
		LinkedList<ConvMessage> resultList = new LinkedList<ConvMessage>();
        SQLiteStatement insertStatement = getSqLiteStatementToInsertIntoMessagesTable(true);
		try
		{
			long msgId = -1;

			for (ConvMessage conv : convMessages)
			{

				String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());
				bindConversationInsert(insertStatement, conv,true);

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
				 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the conversation
				 * does not exist we do not add a conversation.
				 */
				if (msgId <= 0 && !Utils.isGroupConversation(conv.getMsisdn()))
				{
					addConversation(conv.getMsisdn(), !conv.isSMS(), null, null, conv);

					bindConversationInsert(insertStatement, conv,true);
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

					assert (msgId >= 0);
				}
				conv.setMsgID(msgId);
				insertServerId(conv);
				ChatThread.addtoMessageMap(conv);

				/*
				 * msdId > 0 means that the conversation exists.
				 */
				if (conv.isFileTransferMessage() && msgId > 0)
				{
					addSharedMedia(conv);
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

	public ArrayList<ContactInfo> addBroadcastRecipientConversations(ConvMessage convMessage)
	{
		ContactManager contactManager = ContactManager.getInstance();
		ArrayList<ConvMessage> convMessages= new ArrayList<ConvMessage>(1);
		ConvMessage broadcastConvMessage = new ConvMessage(convMessage);
		//Here we set origin type of 
		broadcastConvMessage.setMessageOriginType(ConvMessage.OriginType.BROADCAST);
		broadcastConvMessage.setServerId(convMessage.getMsgID());
		convMessages.add(broadcastConvMessage);
		ArrayList<String> contactMsisdns = convMessage.getSentToMsisdnsList();
		ArrayList<ContactInfo> contacts = new ArrayList<ContactInfo>();
		for (String msisdn : contactMsisdns)
		{
			ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, contactManager.getName(msisdn), msisdn, true);
			contacts.add(contactInfo);
		}

		addConversations(convMessages, contacts, false);

		return contacts;
	}

    private void insertServerId(ConvMessage convMessage) {
    	if (convMessage.getMsgID() > 0)
    	{
    		ContentValues contentValues = new ContentValues();
    		contentValues.put(DBConstants.SERVER_ID, convMessage.getServerId());
    		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(convMessage.getMsgID()) });
    	}
	}

	private SQLiteStatement getSqLiteStatementToInsertIntoMessagesTable(boolean createConvIfNotExist) {
        SQLiteStatement insertStatement = null;
        //TODO we need to insert messageOrginType field in both of these queries.
        if(createConvIfNotExist){
            insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_ORIGIN_TYPE + "," + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
                    + DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.CONV_ID
                    + ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE   + "," + DBConstants.MSISDN +  "," + HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + " ) "
                    + " SELECT ?, ?, ?, ?, ?, ?, ?,  " + DBConstants.CONV_ID + ", ?, ?, ?, ?, ?, ? FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONVERSATIONS_TABLE + "."
                    + DBConstants.MSISDN + "=?");
        }else{
            insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_ORIGIN_TYPE + "," + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
                    + DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT
                    + ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE  + "," + DBConstants.MSISDN +  "," + HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + " ) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");

        }
        return insertStatement;
    }

	/**
	 * This function updates unread count for each msisdn/groupId
	 *
	 * @param messageListMap
	 *            -- map of msisdn/groupid to list of conversation messages
	 */
	public void incrementUnreadCountBulk(Map<String, LinkedList<ConvMessage>> messageListMap)
	{
		for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
		{
			LinkedList<ConvMessage> list = entry.getValue();

			if(list != null && !list.isEmpty())		// check for empty or null lists
			{
				int unreadCount = 0;
				for(ConvMessage conv : list)
				{
					/*
					 * We don't increment unreadcount if message is status message
					 */
					if(Utils.shouldIncrementCounter(conv))
					{
						unreadCount ++;
					}
				}

				// update DB
				if(unreadCount != 0)
				{
					incrementUnreadCounter(entry.getKey(), unreadCount);
				}
			}
		}
	}

	/**
	 *
	 * @param convMessages
	 *            -- list of messages to be added to conversation table
	 * @param lastPinMap
	 *            -- list of pin messages to be added to conversation table
	 */
	public void addLastConversations(List<ConvMessage> convMessages, HashMap<String, PairModified<ConvMessage, Integer>> lastPinMap)
	{
		Map<String, Pair<List<String>, Long>> map = new HashMap<String, Pair<List<String>, Long>>();

		for (ConvMessage conv : convMessages)
		{
			String msisdn = conv.getMsisdn();
			ContentValues contentValues = getContentValueForConversationMessage(conv,conv.getTimestamp());

			//TODO proper check for broadcast message
			if(conv.isBroadcastMessage() && !conv.isBroadcastConversation())
			{
				//We donot update sorting timestamp value if this is broadcast message in normal 1-1 chat
				contentValues.remove(DBConstants.SORTING_TIMESTAMP);
			}
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });

			if (lastPinMap.get(conv.getMsisdn()) != null)
			{
				lastPinMap.get(msisdn).setSecond(lastPinMap.get(msisdn).getSecond() - 1);
			}

			if (Utils.isGroupConversation(conv.getMsisdn()))
			{
				long timestamp = 0;
				Pair<List<String>, Long> pair = map.get(conv.getMsisdn());
				if (pair != null)
					timestamp = pair.second;
				if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
				{
					timestamp = conv.getTimestamp();
				}
				List<String> lastMsisdns = new ArrayList<String>();
				if (conv.getMetadata() != null)
					lastMsisdns = getGroupLastMsgMsisdn(conv.getMetadata().getJSON());
				if (lastMsisdns.size() == 0 && null != conv.getGroupParticipantMsisdn())
				{
					lastMsisdns.add(conv.getGroupParticipantMsisdn());
				}
				map.put(conv.getMsisdn(), new Pair<List<String>, Long>(lastMsisdns, timestamp));
			}
		}
		ContactManager.getInstance().removeOlderLastGroupMsisdns(map);

		for (Entry<String, PairModified<ConvMessage, Integer>> entry : lastPinMap.entrySet())
		{
			PairModified<ConvMessage, Integer> pair = entry.getValue();
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

	private ContentValues getContentValueForConversationMessage(ConvMessage conv,long timeStampForMessage)
	{
		return getContentValueForConversationMessage(conv, timeStampForMessage, timeStampForMessage);
	}
	private ContentValues getContentValueForConversationMessage(ConvMessage conv,long lastMessageTimeStamp, long sortingTimeStamp)
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
			contentValues.put(DBConstants.SERVER_ID, conv.getServerId());
			contentValues.put(DBConstants.MAPPED_MSG_ID, conv.getMappedMsgID());
			contentValues.put(DBConstants.MSG_STATUS, conv.getState().ordinal());
			contentValues.put(DBConstants.SORTING_TIMESTAMP, sortingTimeStamp);
			contentValues.put(DBConstants.LAST_MESSAGE_TIMESTAMP, lastMessageTimeStamp);
			contentValues.put(DBConstants.MESSAGE_ORIGIN_TYPE, conv.getMessageOriginType().ordinal());
		}
		if (conv.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			contentValues = getContentValueForPinConversationMessage(conv, contentValues);
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
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() }, null,
					null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);
				try
				{
					Conversation.MetaData convMetaData = null;
					if (metadata != null)
					{
						convMetaData = new Conversation.MetaData(metadata, conv.getMsisdn());
					}
					else
					{
						convMetaData = new Conversation.MetaData(null, null);
						convMetaData.setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, conv.getMsgID());
					}
					long preTimeStamp = convMetaData.getLastPinTimeStamp(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					long currentTimeStamp = conv.getTimestamp();
					if (preTimeStamp < currentTimeStamp)
					{
						convMetaData = updatePinMetadata(conv, convMetaData, 0);

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
			if (c != null)
			{
				c.close();
			}
		}
	}

	/*
	 * add pin related content values to the content values that comes in argument and return modified content values This function is specific for bulk. We also give unread count
	 * in this function.
	 */
	private ContentValues getContentValueForPinConversationMessage(ConvMessage conv, ContentValues contentValues, int unreadCount)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() }, null,
					null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);

				try
				{
					Conversation.MetaData convMetaData = null;
					convMetaData = new Conversation.MetaData(metadata, conv.getMsisdn());

					convMetaData = updatePinMetadata(conv, convMetaData, unreadCount);
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
			if (c != null)
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
					if (unreadCount != 0)
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

	public void deleteConversation(List<String> msisdns)
	{
		try
		{
			mDb.beginTransaction();
			for (String msisdn : msisdns)
			{
				mDb.delete(DBConstants.CONVERSATIONS_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
				mDb.delete(DBConstants.MESSAGES_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
				mDb.delete(DBConstants.SHARED_MEDIA_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
				if (Utils.isGroupConversation(msisdn))
				{
					mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
					mDb.delete(DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
					removeChatThemeForMsisdn(msisdn);
				}
				setExtraConvUnreadCount(msisdn, 0);
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void deleteBot(String msisdn){
		mDb.beginTransaction();
		try
		{
			mDb.delete(DBConstants.BOT_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
			removeChatThemeForMsisdn(msisdn);
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner)
	{
		return addConversation(msisdn, onhike, groupName, groupOwner, null);
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
	 * @param initialConvMessage
	 *            the initial ConvMessage object to be added to the conversation before publishing the new conversation event
	 * @return Conversation object representing the conversation
	 */
	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner, ConvMessage initialConvMessage)
	{
		ContactInfo contactInfo = Utils.isGroupConversation(msisdn) ? new ContactInfo(msisdn, msisdn, groupName, msisdn) : HikeMessengerApp.getContactManager().getContact(msisdn,
				false, true);
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
				e.printStackTrace();
				return null;
			}

			if (id >= 0)
			{
				Conversation conv;
				if (Utils.isGroupConversation(msisdn))
				{
					if (!(Utils.isBroadcastConversation(msisdn)))
					{
						conv = new GroupConversation(msisdn, (contactInfo != null) ? contactInfo.getName() : null, groupOwner, true);
					}
					else
					{
						conv = new BroadcastConversation(msisdn, (contactInfo != null) ? contactInfo.getName() : null, groupOwner, true);
					}
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

					((GroupConversation) conv).setGroupParticipantList(ContactManager.getInstance().getGroupParticipants(msisdn, false, false));
				}
				else
				{
					conv = new Conversation(msisdn, (contactInfo != null) ? contactInfo.getName() : null, onhike);
				}
				if (initialConvMessage != null)
				{
					conv.addMessage(initialConvMessage);
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

	public void insertBot(String msisdn, String name, String metadata, int isMute)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		values.put(DBConstants.NAME, name);
		values.put(DBConstants.CONVERSATION_METADATA, metadata);
		values.put(DBConstants.IS_MUTE, isMute);
		mDb.insertWithOnConflict(DBConstants.BOT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void updateBot(String msisdn, String name, String metadata, int isMute)
	{
		ContentValues values = new ContentValues();
		if (!TextUtils.isEmpty(name))
		{
			values.put(DBConstants.NAME, name);
		}
		if (!TextUtils.isEmpty(metadata))
		{
			values.put(DBConstants.CONVERSATION_METADATA, metadata);
		}
		if (isMute != -1)
		{
			values.put(DBConstants.IS_MUTE, isMute);
		}
		mDb.updateWithOnConflict(DBConstants.BOT_TABLE, values, DBConstants.MSISDN + "=?", new String[] { msisdn }, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public boolean isBotMuted(String msisdn)
	{
		Cursor c = null;
		int muteInt = 0;
		try
		{
			String selection = DBConstants.MSISDN + " = ?";

			c = mDb.query(DBConstants.BOT_TABLE, new String[] { DBConstants.IS_MUTE }, selection, new String[] { msisdn }, null, null, null, null);

			if (c.moveToFirst())
			{
				muteInt = c.getInt(c.getColumnIndex(DBConstants.IS_MUTE));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return muteInt == 0 ? false : true;
	}

	public List<ConvMessage> getConversationThread(String msisdn, int limit, Conversation conversation, long maxMsgId)
	{
		String limitStr = (limit == -1) ? null : new Integer(limit).toString();
		String selection = DBConstants.MSISDN + " = ?" + (maxMsgId == -1 ? "" : " AND " + DBConstants.MESSAGE_ID + "<" + maxMsgId);
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE,DBConstants.MESSAGE_ORIGIN_TYPE}, selection, new String[] { msisdn }, null, null, DBConstants.MESSAGE_ID + " DESC", limitStr);


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
		Conversation conv = null;
		try
		{
			if (getMetadata)
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH,
						DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			else
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH },
						DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			int unreadCount = c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
			boolean isStealth = c.getInt(c.getColumnIndex(DBConstants.IS_STEALTH)) != 0;
			String metadata = null;

			if (Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn);
				conv.setIsStealth(isStealth);
			}
			else
			{
				String name;
				if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
				{
					name = HikeMessengerApp.hikeBotNamesMap.get(msisdn);
					onhike = true;
				}
				else
				{
					ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, false, true, false);
					name = contactInfo.getName();
					onhike |= contactInfo.isOnhike();
				}
				conv = new Conversation(msisdn, name, onhike, isStealth);

			}
			if (getMetadata)
			{
				metadata = c.getString(c.getColumnIndex(DBConstants.CONVERSATION_METADATA));
				try
				{
					conv.setMetaData(new Conversation.MetaData(metadata, msisdn));
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
					messages = getConversationThread(msisdn, unreadCount, conv, -1);
				}
				else
				{
					messages = getConversationThread(msisdn, limit, conv, -1);
				}
				conv.setMessages(messages);
			}
			unreadCount += getExtraConvUnreadCount(msisdn);
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

	/*
	 * This function returns unread count for any given msisdn , In addition to unread messages there could be many events which we might want to add in unread for any given
	 * conversation , for example missed call, PIN, Micro App State chagned etc
	 */
	public int getExtraConvUnreadCount(String msisdn)
	{
		return HikeSharedPreferenceUtil.getInstance(HikeSharedPreferenceUtil.CONV_UNREAD_COUNT).getData(msisdn, 0);
	}

	/*
	 * This function set unread count for any given msisdn , In addition to unread messages there could be many events which we might want to add in unread for any given
	 * conversation , for example missed call, PIN, Micro App State chagned etc
	 */
	public void setExtraConvUnreadCount(String msisdn, int count)
	{
		HikeSharedPreferenceUtil.getInstance(HikeSharedPreferenceUtil.CONV_UNREAD_COUNT).saveData(msisdn, count);
	}

	public Conversation getConversation(String msisdn, int limit)
	{
		return getConversation(msisdn, limit, false);
	}

	public Conversation getConversationBulk(String msisdn, int limit)
	{
		Cursor c = null;
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH },
					DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			int unreadCount = c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
			boolean isStealth = c.getInt(c.getColumnIndex(DBConstants.IS_STEALTH)) != 0;

			if (Utils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn);
				conv.setIsStealth(isStealth);
			}
			else
			{
				String name;
				if (HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn))
				{
					name = HikeMessengerApp.hikeBotNamesMap.get(msisdn);
					onhike = true;
				}
				else
				{
					ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, false, true);
					name = contactInfo.getName();
					onhike |= contactInfo.isOnhike();
				}
				conv = new Conversation(msisdn, name, onhike, isStealth);

			}

			conv.setUnreadCount(unreadCount +getExtraConvUnreadCount(msisdn));

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
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.ONHIKE, DBConstants.SORTING_TIMESTAMP },
					DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int lastMessageTsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
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
				conv = getGroupConversation(msisdn);
			}
			else
			{
				ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, false, true);

				onhike |= contactInfo.isOnhike();
				conv = new Conversation(msisdn, contactInfo.getName(), onhike);
			}

			ConvMessage message = new ConvMessage(messageString, msisdn, c.getInt(lastMessageTsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
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

	public ConversationMsisdns getConversationMsisdns()
	{
		Cursor c = null;
		List<String> oneToOneMsisdns = new ArrayList<String>();
		Map<String, Pair<List<String>, Long>> grpLastMsisdns = new HashMap<String, Pair<List<String>, Long>>();

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.GROUP_PARTICIPANT, DBConstants.MESSAGE_METADATA ,DBConstants.SORTING_TIMESTAMP}, null, null, null,
					null, null);

			final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int timestampColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnColumn);
				String groupParticipant = c.getString(groupParticipantColumn);
				String metadata = c.getString(metadataColumn);
				long timestamp = c.getLong(timestampColumn);

				if (Utils.isGroupConversation(msisdn))
				{
					List<String> grpMsisdns = null;
					try
					{
						if (!TextUtils.isEmpty(metadata))
						{
							grpMsisdns = getGroupLastMsgMsisdn(new JSONObject(metadata));
							if (null != grpMsisdns)
							{
								if (grpMsisdns.isEmpty())
								{
									if (null != groupParticipant && !groupParticipant.equals(""))
									{
										grpMsisdns.add(groupParticipant);
									}
								}
							}
						}
						grpLastMsisdns.put(msisdn, new Pair<List<String>, Long>(grpMsisdns,timestamp));
					}
					catch (JSONException e)
					{
						Logger.e("HikeConversationsDatabase", "Exception while getting last group message msisdns : " + e);
					}
				}
				else
				{
					oneToOneMsisdns.add(msisdn);
				}
			}
			ConversationMsisdns convsMsisdns = new ConversationMsisdns(oneToOneMsisdns, grpLastMsisdns);
			return convsMsisdns;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	/*
	 * This method returns a list of ContactInfo for all 1-1 conversations with timestamp set.
	 */
	public List<ContactInfo> getOneToOneContacts(boolean removeNewOrReturningUsers)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.SORTING_TIMESTAMP, DBConstants.MESSAGE_METADATA}, null, null, null,
					null, null);

			final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int sortingTimestampColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);

			List<ContactInfo> oneToOneContacts = new ArrayList<ContactInfo>();
			while(c.moveToNext())
			{
				String msisdn = c.getString(msisdnColumn);
				String metadata = c.getString(metadataColumn);
				long timestamp = c.getLong(sortingTimestampColumn);

				if(!Utils.isGroupConversation(msisdn))
				{
					if (!TextUtils.isEmpty(metadata) && removeNewOrReturningUsers)
					{
						try
						{
							JSONObject md = new JSONObject(metadata);
							if(md.optString(HikeConstants.TYPE).equals(HikeConstants.MqttMessageTypes.USER_JOINED))
							{
								continue;
							}
						}
						catch (JSONException e)
						{
							Logger.e("HikeConversationsDatabase", "Exception while parsing metadata for fetching new users msisdn : " + e);
						}
					}
					/*
					 * It is assumed that the contact would be in persistence cache since we have a conversation going on for this msisdn.
					 * If not, it makes a db query and loads into persistence cache.
					 */
					ContactInfo contact = ContactManager.getInstance().getContact(msisdn, false, true);
					contact.setLastMessaged(timestamp);
					oneToOneContacts.add(contact);
				}
			}
			return oneToOneContacts;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<String> getGroupLastMsgMsisdn(JSONObject metadata)
	{
		List<String> grpLastMsisdns = new ArrayList<String>();
		try
		{
			ParticipantInfoState participantInfoState = metadata.has(HikeConstants.DND_USERS) || metadata.has(HikeConstants.DND_NUMBERS) ? ParticipantInfoState.DND_USER
					: ParticipantInfoState.fromJSON(metadata);
			switch (participantInfoState)
			{
			case CHANGED_GROUP_NAME:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
				break;

			case DND_USER:
				JSONArray dndNumbers = metadata.has(HikeConstants.DND_USERS) ? metadata.getJSONArray(HikeConstants.DND_USERS) : metadata.getJSONArray(HikeConstants.DND_NUMBERS);
				if (dndNumbers != null && dndNumbers.length() > 0)
				{
					for (int i = 0; i < dndNumbers.length(); i++)
					{
						String dndName = dndNumbers.optString(i);
						grpLastMsisdns.add(dndName);
					}
				}
				break;

			case PARTICIPANT_JOINED:
				JSONArray participantInfoArray = metadata.getJSONArray(HikeConstants.DATA);

				JSONObject participant = (JSONObject) participantInfoArray.opt(0);
				grpLastMsisdns.add(participant.optString(HikeConstants.MSISDN));

				if (participantInfoArray.length() == 2)
				{
					JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
					grpLastMsisdns.add(participant2.optString(HikeConstants.MSISDN));
				}
				break;

			case PARTICIPANT_LEFT:
				grpLastMsisdns.add(metadata.getString(HikeConstants.DATA));
				break;

			case USER_JOIN:
			case USER_OPT_IN:
				grpLastMsisdns.add(metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN));
				break;

			case CHANGED_GROUP_IMAGE:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
				break;
			case STATUS_MESSAGE:
			case CHAT_BACKGROUND:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
			}
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception while getting last message in group msisdn from metadata " + e);
		}
		return grpLastMsisdns;
	}

	public Map<String, GroupDetails> getIdGroupDetailsMap(List<String> grpIds)
	{
		StringBuilder groupIds = new StringBuilder("(");
		for (String grpId : grpIds)
		{
			groupIds.append(DatabaseUtils.sqlEscapeString(grpId) + ",");
		}
		int idx = groupIds.lastIndexOf(",");
		if (idx >= 0)
		{
			groupIds.replace(idx, groupIds.length(), ")");
		}

		Cursor groupInfoCursor = null;
		try
		{
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
					new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP }, DBConstants.GROUP_ID + " IN " + groupIds, null,
					null, null, null);
			Map<String, GroupDetails> map = new HashMap<String, GroupDetails>();
			final int groupIdIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupAliveIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ALIVE);
			final int groupMuteIdx = groupInfoCursor.getColumnIndex(DBConstants.MUTE_GROUP);
			while (groupInfoCursor.moveToNext())
			{
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				boolean groupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;
				boolean groupMute = groupInfoCursor.getInt(groupMuteIdx) != 0;
				map.put(groupId, new GroupDetails(groupId, groupName, groupAlive, groupMute));
			}
			return map;
		}
		finally
		{
			if (null != groupInfoCursor)
			{
				groupInfoCursor.close();
			}

		}
	}

	/**
	 * Returns a hash map between group id and group name
	 *
	 * @return
	 */
	public Map<String, GroupDetails> getIdGroupDetailsMap()
	{
		Cursor groupInfoCursor = null;
		try
		{
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP }, null, null, null,
					null, null);

			Map<String, GroupDetails> map = new HashMap<String, GroupDetails>();

			final int groupIdIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupAliveIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ALIVE);
			final int groupMuteIdx = groupInfoCursor.getColumnIndex(DBConstants.MUTE_GROUP);
			while (groupInfoCursor.moveToNext())
			{
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				boolean groupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;
				boolean groupMute = groupInfoCursor.getInt(groupMuteIdx) != 0;
				map.put(groupId, new GroupDetails(groupId, groupName, groupAlive, groupMute));
			}
			return map;
		}
		finally
		{
			if (null != groupInfoCursor)
			{
				groupInfoCursor.close();
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
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.UNREAD_COUNT,
					DBConstants.IS_STEALTH, DBConstants.CONVERSATION_METADATA, DBConstants.SORTING_TIMESTAMP }, null, null, null, null, null);
		}
		else
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.UNREAD_COUNT,
					DBConstants.IS_STEALTH, DBConstants.SORTING_TIMESTAMP }, null, null, null, null, null);
		}

		Map<String, Conversation> conversations = new HashMap<String, Conversation>();

		final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int lastMessageTsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
		final int sortingTsColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		final int unreadCountColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);
		final int isStealthColumn = c.getColumnIndex(DBConstants.IS_STEALTH);

		try
		{
			List<String> groupIds = new ArrayList<String>();
			List<String> msisdns = new ArrayList<String>();
			while (c.moveToNext())
			{
				Conversation conv;
				// TODO this can be expressed in a single sql query
				String msisdn = c.getString(msisdnIdx);
				String messageString = c.getString(msgColumn);
				String metadata = c.getString(metadataColumn);
				long lastMessageTimestamp = c.getLong(lastMessageTsColumn);
				long sortingTimestamp = c.getLong(sortingTsColumn);

				if (Utils.isGroupConversation(msisdn))
				{
					GroupDetails details = ContactManager.getInstance().getGroupDetails(msisdn);
					if (null == details)
					{
						groupIds.add(msisdn);
						conv = new GroupConversation(msisdn, null, null, true, false);
					}
					else
					{
						String name = details.getGroupName();
						boolean groupAlive = details.isGroupAlive();
						boolean isMuteGroup = details.isGroupMute();
						if(!Utils.isBroadcastConversation(msisdn))
							conv = new GroupConversation(msisdn, name, null, groupAlive, isMuteGroup);
						else
						{
							conv = new BroadcastConversation(msisdn, name, null, groupAlive, isMuteGroup);
						}
					}
				}
				else
				{
					conv = new Conversation(msisdn, sortingTimestamp);
					ContactInfo contact = ContactManager.getInstance().getContact(conv.getMsisdn());
					ContactManager.getInstance().updateContactRecency(msisdn, sortingTimestamp, false);
					if (null == contact)
					{
						msisdns.add(msisdn);
					}
					else
					{
						conv.setContactName(contact.getName());
						conv.setOnhike(contact.isOnhike());
					}
				}

				conv.setUnreadCount(c.getInt(unreadCountColumn)+getExtraConvUnreadCount(msisdn));
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

				ConvMessage message = new ConvMessage(messageString, msisdn, lastMessageTimestamp, ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
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
				conv.setTimestamp(sortingTimestamp);
				Logger.d("HikeConversationDatabase", "conversation msisdn : " + msisdn);
				conversations.put(msisdn, conv);
			}

			if (msisdns.size() > 0)
			{
				List<ContactInfo> contacts = ContactManager.getInstance().getContact(msisdns, false, true);
				for (ContactInfo contact : contacts)
				{
					Conversation conv = conversations.get(contact.getMsisdn());
					conv.setContactName(contact.getName());
					conv.setOnhike(contact.isOnhike());
				}
			}

			if (groupIds.size() > 0)
			{
				Logger.d("HikeConversationDatabase", " group ids list that returned null for group details : " + groupIds);
				Map<String, GroupDetails> groupDetailsMap = ContactManager.getInstance().getGroupDetails(groupIds);
				for (Entry<String, GroupDetails> mapEntry : groupDetailsMap.entrySet())
				{
					Conversation conv = conversations.get(mapEntry.getKey());
					GroupDetails details = mapEntry.getValue();
					if (null != details)
					{
						String name = details.getGroupName();
						boolean groupAlive = details.isGroupAlive();
						boolean isMuteGroup = details.isGroupMute();
						conv.setContactName(name);
						((GroupConversation) conv).setGroupAlive(groupAlive);
						((GroupConversation) conv).setIsMuted(isMuteGroup);
					}
				}
			}
		}
		finally
		{
			c.close();
		}
		List<Conversation> conversationsList = new ArrayList<Conversation>(conversations.values());
		Collections.sort(conversationsList, Collections.reverseOrder());
		Logger.d("ConversationsTimeTest", "Query time: " + (System.currentTimeMillis() - startTime));
		return conversationsList;
	}

	private ConvMessage getLastMessageForConversation(String msisdn)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP, DBConstants.MAPPED_MSG_ID,
					DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
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

			long msgId;
			try
			{
				msgId = conversation.getMetaData().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE, DBConstants.MESSAGE_ORIGIN_TYPE}, DBConstants.MESSAGE_ID + " =?", new String[] { Long.toString(msgId) }, null, null, null, null);
			List<ConvMessage> elements = getMessagesFromDB(c, conversation);
			return elements.size() > 0 ? elements.get(elements.size() - 1) : null;
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
			return null;

	}

	private Conversation getGroupConversation(String msisdn)
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

			GroupConversation conv;
			if (Utils.isBroadcastConversation(msisdn))
			{
				conv = new BroadcastConversation(msisdn, groupName, groupOwner, isGroupAlive);
			}
			else
			{
				conv = new GroupConversation(msisdn, groupName, groupOwner, isGroupAlive);
			}
			conv.setGroupParticipantList(ContactManager.getInstance().getGroupParticipants(msisdn, false, false));
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

	public JSONArray updateStatusAndSendDeliveryReport(String msisdn)
	{

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID }, DBConstants.MSISDN + "=? and " + DBConstants.MSG_STATUS
					+ "=?", new String[] { msisdn, Integer.toString(ConvMessage.State.RECEIVED_UNREAD.ordinal()) }, null, null, null);
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

	/**
	 * deletes multiple messages corresponding to give msgId
	 *
	 * @param msgIds
	 * @param msisdn
	 * @param containsLastMessage
	 *            null if its value is not known. In this case we need to fetch this value from db. value true implies that given msgIds set contains a messageId of a message which
	 *            is currently last message of that conversation. false implies it does not contains last message's id.
	 */
	public void deleteMessages(ArrayList<Long> msgIds, String msisdn, Boolean containsLastMessage)
	{
		StringBuilder inSelection = new StringBuilder("(" + msgIds.get(0));
		for (int i = 0; i < msgIds.size(); i++)
		{
			inSelection.append("," + Long.toString(msgIds.get(i)));
		}
		inSelection.append(")");
		try
		{
			mDb.beginTransaction();
			if (containsLastMessage == null)
			{
				ConvMessage convMessage = getLastMessageForConversation(msisdn);
				if (msgIds.contains(convMessage.getMsgID()))
				{
					containsLastMessage = true;
				}
				else
				{
					containsLastMessage = false;
				}
			}

			mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MESSAGE_ID + " IN " + inSelection.toString());

			mDb.execSQL("DELETE FROM " + DBConstants.SHARED_MEDIA_TABLE + " WHERE " + DBConstants.MESSAGE_ID + " IN " + inSelection.toString());

			if (containsLastMessage)
			{
				deleteMessageFromConversation(msisdn);
			}
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

	public void clearConversation(String msisdn)
	{
		String[] args = new String[] { msisdn };
		/*
		 * Clearing the messages table.
		 */
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MSISDN + "= ?", args);

		mDb.execSQL("DELETE FROM " + DBConstants.SHARED_MEDIA_TABLE + " WHERE " + DBConstants.MSISDN + "= ?", args);

		setExtraConvUnreadCount(msisdn, 0);
		/*
		 * Next we have to clear the conversation table.
		 */
		clearLastConversationMessage(msisdn);
	}

	private void clearLastConversationMessage(String msisdn)
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

		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	private void deleteMessageFromConversation(String msisdn)
	{
		/*
		 * We get the latest message from the messages table
		 */
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, null, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, DBConstants.MESSAGE_ID + " DESC LIMIT " + 1);
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
				ContentValues contentValues = getContentValueForConversationMessage(message,message.getTimestamp());
				if (Utils.isGroupConversation(msisdn))
				{
					updateGroupRecency(message);
					HikeMessengerApp.getContactManager().removeContact(c.getString(groupParticipantColumn), false);
				}
				mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
			}
			else
			{
				if (Utils.isGroupConversation(msisdn))
				{
					/*
					 * If we have removed the last message of a group, we should do the same operations we do when clearing a conversation.
					 */
					clearLastConversationMessage(msisdn);
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

	private void updateGroupRecency(ConvMessage conv)
	{
		if (Utils.isGroupConversation(conv.getMsisdn()))
		{
			if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
			{
				ContactManager.getInstance().updateGroupRecency(conv.getMsisdn(), conv.getTimestamp());
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
	 * Add or remove participants from a group members table based on participants map that is passed as parameter
	 *
	 * @param groupId
	 *            The id of the group to which the participants are to be added
	 * @param participantList
	 *            A list of the participants to be added
	 */
	public int addRemoveGroupParticipants(String groupId, Map<String, PairModified<GroupParticipant, String>> participantList, boolean groupRevived)
	{
		boolean participantsAlreadyAdded = true;
		boolean infoChangeOnly = false;

		List<PairModified<GroupParticipant, String>> currentParticipantsList = ContactManager.getInstance().getGroupParticipants(groupId, true, false);
		Map<String, PairModified<GroupParticipant, String>> currentParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
		for (PairModified<GroupParticipant, String> grpParticipant : currentParticipantsList)
		{
			String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
			currentParticipants.put(msisdn, grpParticipant);
		}

		if (currentParticipants.isEmpty())
		{
			participantsAlreadyAdded = false;
		}
		for (Entry<String, PairModified<GroupParticipant, String>> newParticipantEntry : participantList.entrySet())
		{
			if (!currentParticipants.containsKey(newParticipantEntry.getKey()))
			{
				participantsAlreadyAdded = false;
				infoChangeOnly = false;
			}
			else
			{
				GroupParticipant currentParticipant = currentParticipants.get(newParticipantEntry.getKey()).getFirst();
				if (currentParticipant.onDnd() != newParticipantEntry.getValue().getFirst().onDnd())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				if (currentParticipant.getContactInfo().isOnhike() != newParticipantEntry.getValue().getFirst().getContactInfo().isOnhike())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				currentParticipants.remove(newParticipantEntry.getKey());
			}
		}

		if (groupRevived)
		{
			/*
			 * if group is revived then we have to check whether some members are added or removed from the group and update that in the db
			 */
			if (currentParticipants.isEmpty() && participantsAlreadyAdded)
			{
				return HikeConstants.NO_CHANGE;
			}
		}
		else
		{
			/*
			 * if group is not revived that means we have added some members into the group or gcj was already received when adding members the parameter participantList contains
			 * only members which are added and not all the group participants
			 */
			if (participantsAlreadyAdded)
				return HikeConstants.NO_CHANGE;
		}

		Map<String, PairModified<GroupParticipant, String>> newParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			mDb.beginTransaction();

			if (groupRevived && !currentParticipants.isEmpty())
			{
				String removeMsisdns = Utils.getMsisdnStatement(currentParticipants.keySet());
				Logger.d(getClass().getSimpleName(), " remove these from group members table GroupId : " + groupId + " removed msisdns : " + removeMsisdns);
				mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " = ? " + " AND " + DBConstants.MSISDN + " IN " + removeMsisdns, new String[] { groupId });
				ContactManager.getInstance().removeGroupParticipant(groupId, currentParticipants.keySet());
			}

			if(!participantsAlreadyAdded)
			{
				ih = new InsertHelper(mDb, DBConstants.GROUP_MEMBERS_TABLE);
				insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN
						+ ", " + DBConstants.NAME + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_LEFT + ", " + DBConstants.ON_DND + ", " + DBConstants.SHOWN_STATUS + " ) "
						+ " VALUES (?, ?, ?, ?, ?, ?, ?)");
				for (Entry<String, PairModified<GroupParticipant, String>> participant : participantList.entrySet())
				{
					GroupParticipant groupParticipant = participant.getValue().getFirst();
					insertStatement.bindString(ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
					insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), participant.getKey());
					insertStatement.bindString(ih.getColumnIndex(DBConstants.NAME), participant.getValue().getSecond());
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.ONHIKE), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.HAS_LEFT), 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.ON_DND), groupParticipant.onDnd() ? 1 : 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.SHOWN_STATUS), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
					insertStatement.executeInsert();

					newParticipants.put(participant.getKey(), new PairModified<GroupParticipant, String>(groupParticipant, participant.getValue().getSecond()));
				}
				ContactManager.getInstance().addGroupParticipants(groupId, newParticipants);
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

	public Map<String, Map<String, String>> getGroupMembersName(String msisdns)
	{
		Cursor c = null;
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.NAME, DBConstants.GROUP_ID }, DBConstants.MSISDN + " IN " + msisdns,
					null, null, null, null);
			final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int nameIdx = c.getColumnIndex(DBConstants.NAME);
			final int groupidIdx = c.getColumnIndex(DBConstants.GROUP_ID);
			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);
				String groupId = c.getString(groupidIdx);
				Map<String, String> groupParticipant = map.get(groupId);
				if (null == groupParticipant)
				{
					groupParticipant = new HashMap<String, String>();
					map.put(groupId, groupParticipant);
				}
				groupParticipant.put(msisdn, name);
			}
			return map;
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
	public Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly)
	{
		String selection = DBConstants.GROUP_ID + " =? " + (activeOnly ? " AND " + DBConstants.HAS_LEFT + "=0" : "")
				+ (notShownStatusMsgOnly ? " AND " + DBConstants.SHOWN_STATUS + "=0" : "");
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND },
					selection, new String[] { groupId }, null, null, null);

			Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();
			List<String> allMsisdns = new ArrayList<String>();
			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				allMsisdns.add(msisdn);
				String name = c.getString(c.getColumnIndex(DBConstants.NAME));
				GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, name, msisdn, c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0),
						c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT)) != 0, c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0);
				participantList.put(msisdn, new PairModified<GroupParticipant, String>(groupParticipant, name));
			}

			return new Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>>(participantList, allMsisdns);
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

	public Map<String, Integer> getAllGroupsActiveParticipantCount()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.GROUP_ID, "count(*) as count" },
					(DBConstants.HAS_LEFT + "=0") , null, DBConstants.GROUP_ID, null, null);
			Map<String, Integer> groupCountMap = new HashMap<String, Integer>();
			while(c.moveToNext())
			{
				String groupId = c.getString(c.getColumnIndex(DBConstants.GROUP_ID));
				int count = c.getInt(c.getColumnIndex("count"));
				groupCountMap.put(groupId, count);
			}
			return groupCountMap;
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
		if (!ContactManager.getInstance().isGroupExist(groupId))
		{
			return ;
		}
		ContactManager.getInstance().setGroupMute(groupId, isMuted);
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

	public Map<String, String> getGroupParticipantNameMap(String groupId, List<String> msisdns)
	{
		Map<String, String> map = new HashMap<String, String>();

		StringBuilder msisdnsDB = new StringBuilder("(");
		for (String msisdn : msisdns)
		{
			msisdnsDB.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
		}
		int idx = msisdnsDB.lastIndexOf(",");
		if (idx >= 0)
		{
			msisdnsDB.replace(idx, msisdnsDB.length(), ")");
		}

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.NAME }, DBConstants.GROUP_ID + " =? AND " + DBConstants.MSISDN + " IN "
					+ msisdnsDB.toString(), new String[] { groupId }, null, null, null);
			String name = "";

			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				name = c.getString(c.getColumnIndex(DBConstants.NAME));
				map.put(msisdn, name);
			}
			return map;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public GroupParticipant getGroupParticipant(String groupId, String participantMsisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND },
					DBConstants.GROUP_ID + "=? AND " + DBConstants.MSISDN + "=?", new String[] { groupId, participantMsisdn }, null, null, null);

			GroupParticipant groupParticipant = null;

			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				String name = c.getString(c.getColumnIndex(DBConstants.NAME));
				groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, name, msisdn, c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0), c.getInt(c
						.getColumnIndex(DBConstants.HAS_LEFT)) != 0, c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0);
			}

			if (groupParticipant != null)
			{
				ContactInfo contactInfo = ContactManager.getInstance().getContact(participantMsisdn, true, false, true);
				if (contactInfo != null)
				{
					groupParticipant.setContactInfo(contactInfo);
				}
			}
			return groupParticipant;
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
		if (!ContactManager.getInstance().isGroupExist(groupId))
		{
			return 0;
		}
		ContactManager.getInstance().setGroupAlive(groupId, alive);
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_ALIVE, alive);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
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

				List<PairModified<GroupParticipant, String>> groupParticipantMap = ContactManager.getInstance().getGroupParticipants(groupId, true, false, false);
				groupName = TextUtils.isEmpty(groupName) ? Utils.defaultGroupName(groupParticipantMap) : groupName;
				int numMembers = groupParticipantMap.size();

				// Here we make this string the msisdn so that it can be
				// displayed in the list view when forwarding the message
				String numberMembers = context.getString(R.string.num_people, (numMembers + 1));

				ContactInfo group = new ContactInfo(groupId, groupId, groupName, numberMembers, true);
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

			List<String> msisdns = new ArrayList<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx));
				statusMessages.add(statusMessage);

				List<StatusMessage> msisdnMessages = statusMessagesMap.get(msisdn);
				if (msisdnMessages == null)
				{
					msisdns.add(msisdn);
					msisdnMessages = new ArrayList<StatusMessage>();
					statusMessagesMap.put(msisdn, msisdnMessages);
				}
				msisdnMessages.add(statusMessage);
			}
			if (msisdns.size() > 0)
			{
				List<ContactInfo> contactList = HikeMessengerApp.getContactManager().getContact(msisdns, true, true);

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

	public StatusMessage getLastStatusMessage(StatusMessage.StatusMessageType[] smTypes, ContactInfo contactInfo)
	{
		ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>(1);
		contactList.add(contactInfo);
		Map<String, StatusMessage> lastSMMap = getLastStatusMessages(false, smTypes, contactList);
		return lastSMMap.get(contactInfo.getMsisdn());
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
			c1 = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_METADATA }, DBConstants.IS_STATUS_MSG + "=1 AND " + DBConstants.MSISDN + "=?",
					new String[] { msisdn }, null, null, null);
			if (c1.moveToFirst())
			{
				String metadataString = c1.getString(c1.getColumnIndex(DBConstants.MESSAGE_METADATA));
				try
				{
					MessageMetadata messageMetadata = new MessageMetadata(new JSONObject(metadataString), false);

					if (statusId.equals(messageMetadata.getStatusMessage().getMappedId()))
					{
						deleteMessageFromConversation(msisdn);
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

	public void updateStickerCountForStickerCategory(String categoryId, int totalNum)
	{
		updateStickerCategoryData(categoryId, null, totalNum, -1);
	}

	public void removeStickerCategory(String categoryId)
	{
		mDb.delete(DBConstants.STICKER_CATEGORIES_TABLE, DBConstants._ID + "=?", new String[] { categoryId });
		mDb.delete(DBConstants.STICKER_SHOP_TABLE, DBConstants._ID + "=?", new String[] { categoryId });
	}

	public void updateStickerCategoryData(String categoryId, Boolean updateAvailable, int totalStickerCount, int categorySize)
	{
		ContentValues contentValues = new ContentValues();
		if(updateAvailable != null)
		{
			contentValues.put(DBConstants.UPDATE_AVAILABLE, updateAvailable);
		}
		if(totalStickerCount != -1)
		{
			contentValues.put(DBConstants.TOTAL_NUMBER, totalStickerCount);
		}
		if(categorySize != -1)
		{
			contentValues.put(DBConstants.CATEGORY_SIZE, categorySize);
		}

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { categoryId });
		/*
		 * There is no field as UPDATE_AVAILABLE in sticker shop
		 */
		contentValues.remove(DBConstants.UPDATE_AVAILABLE);
		if(contentValues.size() > 0)
		{
			/*
			 * if this category is also there in shop we need to update values there as well
			 */
			mDb.update(DBConstants.STICKER_SHOP_TABLE, contentValues, DBConstants._ID + "=?", new String[] { categoryId });
		}
	}

	public LinkedHashMap<String, StickerCategory> getAllStickerCategoriesWithVisibility(boolean isVisible)
	{
		Cursor c = null;
		LinkedHashMap<String, StickerCategory> stickerDataMap;

		try
		{
			String selection = DBConstants.IS_VISIBLE + "=?";
			String[] selectionArgs = { isVisible ? Integer.toString(1) : Integer.toString(0) };

			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null , selection, selectionArgs, null, null, null);
			stickerDataMap = parseStickerCategoriesCursor(c);
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

	public LinkedHashMap<String, StickerCategory> getAllStickerCategories()
	{
		Cursor c = null;
		LinkedHashMap<String, StickerCategory> stickerDataMap;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null , null, null, null, null, null);
			stickerDataMap = parseStickerCategoriesCursor(c);
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

	public LinkedHashMap<String, StickerCategory> parseStickerCategoriesCursor(Cursor c)
	{
		LinkedHashMap<String, StickerCategory> stickerDataMap = new LinkedHashMap<String, StickerCategory>(c.getCount());

		while (c.moveToNext())
		{
			try
			{
				String categoryId = c.getString(c.getColumnIndex(DBConstants._ID));
				String categoryName = c.getString(c.getColumnIndex(DBConstants.CATEGORY_NAME));
				boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
				boolean isVisible = c.getInt(c.getColumnIndex(DBConstants.IS_VISIBLE)) == 1;
				boolean isCustom = c.getInt(c.getColumnIndex(DBConstants.IS_CUSTOM)) == 1;
				int catIndex = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
				int categorySize = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_SIZE));
				int totalStickers = c.getInt(c.getColumnIndex(DBConstants.TOTAL_NUMBER));

				StickerCategory s;
				/**
				 * Making sure that Recents category is added as CustomStickerCategory only. 
				 * This is being done to avoid ClassCast exception on the PlayStore. 
				 */
				if(isCustom || categoryId.equals(StickerManager.RECENT))
				{
					s = new CustomStickerCategory(categoryId, categoryName, updateAvailable, isVisible, isCustom, true, catIndex, totalStickers,
						categorySize);
				}
				else
				{
					s = new StickerCategory(categoryId, categoryName, updateAvailable, isVisible, isCustom, true, catIndex, totalStickers,
							categorySize);
				}
				stickerDataMap.put(categoryId, s);
			}
			catch (Exception e)
			{
				Logger.e(getClass().getSimpleName(), e.getMessage());
				e.printStackTrace();
			}
		}
		return stickerDataMap;
	}

	public boolean isStickerUpdateAvailable(String categoryId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, new String[] { DBConstants.UPDATE_AVAILABLE }, DBConstants._ID + "=?", new String[] { categoryId }, null,
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
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA }, null, null, null, null, null);

			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metatdataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

			mDb.beginTransaction();

			while (c.moveToNext())
			{
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

					/*
					 * we don't need to initialize sharedMediaTable here.
					 */

					JSONObject fileJson = fileJsonArray.getJSONObject(0);

					HikeFile hikeFile = new HikeFile(fileJson, false);

					if (hikeFile.getThumbnail() == null)
					{
						continue;
					}

					byte[] imageBytes = Base64.decode(hikeFile.getThumbnailString(), Base64.DEFAULT);

                    addFileThumbnail(hikeFile.getFileKey(), imageBytes);

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

	public void addSharedMedia(ConvMessage convMessage)
	{
		ContentValues sharedMediaValues = getSharedMediaContentValues(convMessage);

		mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, sharedMediaValues);
	}

	private ContentValues getSharedMediaContentValues(ConvMessage convMessage )
	{
		ContentValues contentValues = new ContentValues();
		boolean isSent = ConvMessage.isMessageSent(convMessage.getState());
		contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
		contentValues.put(DBConstants.SERVER_ID, convMessage.getServerId());
		contentValues.put(DBConstants.MSISDN, convMessage.getMsisdn());
		contentValues.put(DBConstants.GROUP_PARTICIPANT, convMessage.getGroupParticipantMsisdn() != null ? convMessage.getGroupParticipantMsisdn() : "");
		contentValues.put(DBConstants.TIMESTAMP, convMessage.getTimestamp());
		contentValues.put(DBConstants.IS_SENT, isSent);
		contentValues.put(DBConstants.HIKE_FILE_TYPE, convMessage.getMetadata().getHikeFiles().get(0).getHikeFileType().ordinal());

		putMetadataAccordingToFileType(contentValues, convMessage.getMetadata(), true);

		return contentValues;
	}

	private void putMetadataAccordingToFileType(ContentValues contentValues, MessageMetadata metadata, boolean removeThumbnail)
	{
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadata.getJSON().optString(HikeConstants.CONTENT_TYPE)))
		{
			contentValues.put(DBConstants.MESSAGE_METADATA, metadata.getJSON().toString());
		}
		else if (metadata.getJSON().has(HikeConstants.FILES))
		{
			String thumbnailString = null;
			if(removeThumbnail)
			{
				/*
				 * We need to remove thumbnail from json object before saving in sharedMediaTable
				 */
				thumbnailString = metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0).optString(HikeConstants.THUMBNAIL);
				if (!TextUtils.isEmpty(thumbnailString))
				{
					metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0).remove(HikeConstants.THUMBNAIL);
				}
			}
			contentValues.put(DBConstants.MESSAGE_METADATA, metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0).toString());

			if (thumbnailString != null && !TextUtils.isEmpty(thumbnailString))
			{
				addThumbnailStringToMetadata(metadata, thumbnailString);
			}
		}
	}

	public void addFileThumbnail(String fileKey, byte[] imageBytes)
	{
        ContentValues fileThumbnailValues = new ContentValues();

        Cursor c = null;
        try {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[]{REF_COUNT}, DBConstants.FILE_KEY + "=?", new String[]{fileKey}, null, null, null);


            if (!c.moveToFirst()) {
                fileThumbnailValues.put(DBConstants.FILE_KEY, fileKey);
                fileThumbnailValues.put(DBConstants.IMAGE, imageBytes);
                fileThumbnailValues.put(REF_COUNT, 1);
                mDb.insert(DBConstants.FILE_THUMBNAIL_TABLE, null, fileThumbnailValues);
            } else {
                int refCount = c.getInt(c.getColumnIndex(REF_COUNT));
                fileThumbnailValues.put(REF_COUNT, refCount + 1);
                mDb.update(DBConstants.FILE_THUMBNAIL_TABLE, fileThumbnailValues, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            }
        }
        finally {
            if (c != null)
            {
                c.close();
            }
        }
    }

    public void reduceRefCount(String fileKey){

        ContentValues fileThumbnailValues = new ContentValues();

        Cursor c = null;
        try {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[]{REF_COUNT}, DBConstants.FILE_KEY + "=?", new String[]{fileKey}, null, null, null);
            if (!c.moveToFirst())
                return;

            int refCount = c.getInt(c.getColumnIndex(REF_COUNT));

            if (refCount > 1) {
                fileThumbnailValues.put(REF_COUNT, refCount - 1);
                mDb.update(DBConstants.FILE_THUMBNAIL_TABLE, fileThumbnailValues, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            } else {
                mDb.delete(DBConstants.FILE_THUMBNAIL_TABLE, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            }

        }
        finally {
            if (c != null)
            {
                c.close();
            }
        }

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

    public byte [] getThumbnail(String fileKey){

        Cursor c = null;
        try
        {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.FILE_KEY + "=?", new String[] { fileKey }, null, null, null);

            if (!c.moveToFirst())
            {
                return null;
            }

            return c.getBlob(c.getColumnIndex(DBConstants.IMAGE));

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

	public Pair<String, Long> getReadByValueForGroup(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null,
					null, null);
			if (!c.moveToFirst())
			{
				return null;
			}
			String readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
			Long msgId = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
			return new Pair<String, Long>(readByString, msgId);
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

	public HashMap addBotToHashMap(HashMap<String, String> hikeBotNamesMap)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, new String[] { DBConstants.MSISDN, DBConstants.NAME }, null, null, null, null, null);

			while (c.moveToNext())
			{
				int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
				int nameIdx = c.getColumnIndex(DBConstants.NAME);
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);
				if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(msisdn))
				{
					hikeBotNamesMap.put(msisdn, name);
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
		return hikeBotNamesMap;
	}

	public ConvMessage showParticipantStatusMessage(String groupId)
	{

		List<PairModified<GroupParticipant, String>> smsParticipants = ContactManager.getInstance().getGroupParticipants(groupId, true, true);

		if (smsParticipants.isEmpty())
		{
			return null;
		}

		JSONObject dndJSON = new JSONObject();
		JSONArray dndParticipants = new JSONArray();

		for (PairModified<GroupParticipant, String> smsParticipantEntry : smsParticipants)
		{
			GroupParticipant smsParticipant = smsParticipantEntry.getFirst();
			String msisdn = smsParticipant.getContactInfo().getMsisdn();
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
		String selection = DBConstants.MSISDN + " = ?" + " AND " + DBConstants.MESSAGE_TYPE + "==" + HikeConstants.MESSAGE_TYPE.TEXT_PIN;
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			String query = "SELECT " + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + "," + DBConstants.TIMESTAMP + "," + DBConstants.MESSAGE_ID + ","
					+ DBConstants.MAPPED_MSG_ID + "," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.IS_HIKE_MESSAGE + ","
					+ DBConstants.READ_BY + "," + DBConstants.MESSAGE_TYPE + ","+DBConstants.HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + "," + DBConstants.MESSAGE_ORIGIN_TYPE + " FROM " + DBConstants.MESSAGES_TABLE + " where " + selection + " order by " + DBConstants.MESSAGE_ID

					+ " DESC LIMIT " + limitStr + " OFFSET " + startFrom;
			c = mDb.rawQuery(query, new String[] { msisdn });

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
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MIN (" + DBConstants.TIMESTAMP + ") AS TIME", DBConstants.MSISDN }, DBConstants.MSISDN + " IN "
					+ msisdnStatement + " AND " + DBConstants.MSG_STATUS + "=" + State.SENT_CONFIRMED.ordinal() + " AND  " + DBConstants.IS_HIKE_MESSAGE + "=" + "1", null,
					DBConstants.MSISDN, null, null);

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
			updateToNewSharedMediaTable();
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
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(), msisdn));
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
			Logger.e(getClass().getSimpleName(), "Exception in adding msisdn ", e);
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
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(), msisdn));
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
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
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
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID,
					DBConstants.MSISDN }, null, null, null, null, null);

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
				String messageHash = createMessageHash(msisdn, (long) mappedId, message, (long) ts);
				Integer msgId = c.getInt(msgIdIndex);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_HASH, messageHash);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { msgId.toString() });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
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
			String convIdStatement = Utils.getMsisdnStatement(groupIdMap.keySet());
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
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
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

	/**
	 * Generates a list of messages based on the query passed to it.
	 *
	 * @param c
	 *            The query on the message table.
	 * @param conversation
	 *            Conversation for which the messages are to be fetched.
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
		final int messageOriginTypeColumn = c.getColumnIndex(DBConstants.MESSAGE_ORIGIN_TYPE);
		final int loveIdColumn = c.getColumnIndex(DBConstants.HIKE_CONV_DB.LOVE_ID_REL);
		final int contentIdColumn = c.getColumnIndex(DBConstants.HIKE_CONTENT.CONTENT_ID);
		final int nameSpaceColumn = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());
		SparseArray<ConvMessage> contentMessages = new SparseArray<ConvMessage>();
		StringBuilder loveIds = new StringBuilder("(");
		while (c.moveToNext())
		{
			int hikeMessage = c.getInt(isHikeMessageColumn);
			boolean isHikeMessage = hikeMessage == -1 ? conversation.isOnhike() : (hikeMessage == 0 ? false : true);

			ConvMessage message = new ConvMessage(c.getString(msgColumn), conversation.getMsisdn(), c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
					c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), !isHikeMessage, c.getInt(typeColumn),c.getInt(contentIdColumn), c.getString(nameSpaceColumn));
			//if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT){
//				int loveId = c.getInt(loveIdColumn);
//				// DEFAULT value of love id is -1
//				if(loveId !=- 1){
//				loveIds.append(loveId);
//				loveIds.append(",");
//				contentMessages.put(loveId, message);
//				}
			//}
			String metadata = c.getString(metadataColumn);
			try
			{
                if(message.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT){
                    message.platformMessageMetadata = new PlatformMessageMetadata(metadata, mContext);
                }else if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
					message.webMetadata = new WebMetadata(metadata);
				}else{
                    message.setMetadata(metadata);
                }
			}
			catch (JSONException e)
			{
				Logger.w(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			message.setReadByArray(c.getString(readByColumn));
			message.setMessageOriginType(ConvMessage.originTypeValue(c.getInt(messageOriginTypeColumn)));
			elements.add(elements.size(), message);
		}
		if(contentMessages.size()>0){
			String loveIdsCommaSeparated = loveIds.substring(0, loveIds.length()-1)+")"; // -1 coz of last comma
			// fetch love data from love table
			List<ContentLove> list = getContentLoveData(loveIdsCommaSeparated);
			for(ContentLove love : list){
				ConvMessage message = (ConvMessage) contentMessages.get(love.loveId);
				if(message!=null){
					message.contentLove = love;
				}
			}
		}
		return elements;
	}

	private List<ContentLove> getContentLoveData(String loveIdsCommaSeparated){
		String query = "SELECT " + DBConstants.HIKE_CONV_DB.LOVE_ID + ","
				+ HIKE_CONV_DB.COUNT + "," + HIKE_CONV_DB.USER_STATUS + ","
				+ HIKE_CONV_DB.TIMESTAMP + " FROM " + HIKE_CONV_DB.LOVE_TABLE
				+ " WHERE " + HIKE_CONV_DB.LOVE_ID + " in "
				+ loveIdsCommaSeparated;
		Cursor c = mDb.rawQuery(query, null);
		List<ContentLove> listToReturn = new ArrayList<ContentLove>();
		if(c.getCount()>0){
			int loveId = c.getColumnIndex(HIKE_CONV_DB.LOVE_ID);
			int count = c.getColumnIndex(HIKE_CONV_DB.COUNT);
			int userStatus = c.getColumnIndex(HIKE_CONV_DB.USER_STATUS);
			int timeStamp = c.getColumnIndex(HIKE_CONV_DB.TIMESTAMP);
			while(c.moveToNext()){
				ContentLove love = new ContentLove();
				love.loveId = c.getInt(loveId);
				love.loveCount = c.getInt(count);
				love.userStatus = c.getInt(userStatus) ==1;
				love.updatedTimeStamp = new Date(c.getLong(timeStamp));
				listToReturn.add(love);
			}
		}
		return listToReturn;
	}

	/**
	 * Updates group info table with last sent message id for a group. It also clears readby column for that group
	 *
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
	 *
	 * @param groupId
	 *            -- group id for which mr packet came
	 * @param ids
	 *            -- list of message ids that came in "mr" packet
	 * @return maxMrId from list of ids that are sent by me else -1
	 */
	public long getMrIdForGroup(String groupId, ArrayList<Long> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return -1;
		}
		String inQureyString = Utils.valuesToCommaSepratedString(ids);

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MAX (" + DBConstants.MESSAGE_ID + ") AS msgid" }, DBConstants.MSISDN + "=?  AND " + DBConstants.MESSAGE_ID + " IN " + inQureyString,
					new String[] { groupId }, null, null, null);
			if (c.moveToFirst())
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

	public int getPinCount(String msisdn)
	{
		String selection = DBConstants.MSISDN + " = ?" + " AND " + DBConstants.MESSAGE_TYPE + "==" + HikeConstants.MESSAGE_TYPE.TEXT_PIN;
		Cursor c = null;
		try
		{
				c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MSISDN }, selection, new String[]{msisdn}, null, null, null);
				return c.getCount();
		}
		finally{
			if(c!=null)
				c.close();
		}
	}

	public void updateToNewSharedMediaTable()
	{
		final String TEMP_SHARED_MEDIA_TABLE = "tempSharedMediaTable";

		/*
		 * Renaming current sharedMediaTable to temp_sharedMediaTable
		 */
		String sql1 = "ALTER TABLE " + DBConstants.SHARED_MEDIA_TABLE + " RENAME TO " + TEMP_SHARED_MEDIA_TABLE;

		/*
		 * creating new sharedMediaTable with updated schema
		 */
		String sql2 = getSharedMediaTableCreateQuery();

		/*
		 * selecting rows from messages table for all msgIds from old shared Media table
		 * SELECT msgid, msisdn, fromMsisdn, timestamp, msgStatus, metadata FROM messages where messages.msgId IN (SELECT msgid FROM tempSharedMediaTable);
		 */
		String sql3 = "SELECT " + DBConstants.MESSAGE_ID + ", " + DBConstants.MSISDN + ", " + DBConstants.GROUP_PARTICIPANT + ", " +  DBConstants.TIMESTAMP + ", " + DBConstants.MSG_STATUS + ", "
				+ DBConstants.MESSAGE_METADATA + " FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID
				+ " IN ( SELECT " + DBConstants.MESSAGE_ID + " FROM " + TEMP_SHARED_MEDIA_TABLE + " )";

		String sql4 = "DROP TABLE " + TEMP_SHARED_MEDIA_TABLE;

		mDb.execSQL(sql1);
		mDb.execSQL(sql2);

		Cursor c = null;
		try
		{
			c = mDb.rawQuery(sql3, null);

			final int msgIdIndex = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
			final int tsIndex = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int msgStatusIndex = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int metadataIndex = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

			while (c.moveToNext())
			{
				long msgId = c.getLong(msgIdIndex);
				String msisdn = c.getString(msisdnIndex);
				long ts = c.getLong(tsIndex);
				int messageStatus = c.getInt(msgStatusIndex);
				String messageMetadataString = c.getString(metadataIndex);
				String groupParticipant = c.getString(groupParticipantColumn);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_ID, msgId);
				contentValues.put(DBConstants.MSISDN, msisdn);
				contentValues.put(DBConstants.GROUP_PARTICIPANT, groupParticipant);
				contentValues.put(DBConstants.TIMESTAMP, ts);
				contentValues.put(DBConstants.IS_SENT, ConvMessage.isMessageSent(State.values()[messageStatus]));

				/*
				 * Extracting the hikeFileType from message metadata 
				 */
				JSONObject metadataFileArrayJson = new JSONObject(messageMetadataString);
				if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadataFileArrayJson.optString(HikeConstants.CONTENT_TYPE)))
				{
					contentValues.put(DBConstants.MESSAGE_METADATA, metadataFileArrayJson.toString());
				}
				else if (metadataFileArrayJson.has(HikeConstants.FILES))
				{
					JSONObject messageMetadata = metadataFileArrayJson.optJSONArray(HikeConstants.FILES).optJSONObject(0);
					String contentTypeString = messageMetadata.optString(HikeConstants.CONTENT_TYPE);
					boolean isRecording = messageMetadata.optLong(HikeConstants.PLAYTIME, -1) != -1;
					HikeFileType hikeFileType = HikeFileType.fromString(contentTypeString, isRecording);

					contentValues.put(DBConstants.HIKE_FILE_TYPE, hikeFileType.ordinal());
					contentValues.put(DBConstants.MESSAGE_METADATA, messageMetadata.toString());
				}

				mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, contentValues);
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
		
		/*
		 * Now we can drop the temprory shared media table.
		 */
		mDb.execSQL(sql4);
	}

	public String getSharedMediaTableCreateQuery()
	{
		/*
		 * creating new sharedMediaTable with updated schema
		 */
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.SHARED_MEDIA_TABLE + " (" + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY, "
				+ DBConstants.MSISDN + " TEXT, " + DBConstants.GROUP_PARTICIPANT + " TEXT, " + DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.IS_SENT + " INT, " + DBConstants.HIKE_FILE_TYPE + " INTEGER, "
				+ DBConstants.MESSAGE_METADATA + " TEXT, "+ DBConstants.SERVER_ID + " INTEGER "+" )";

		return sql;
	}

	public List<?> getSharedMedia(String msisdn, int limit, long maxMsgId, boolean onlyMedia)
	{
		return getSharedMedia(msisdn, limit, maxMsgId, onlyMedia, false);
	}


	public String getSharedMediaSelection(boolean onlyMedia)
	{
		StringBuilder hfTypeSelection = null;

		HikeFileType[] mediaFileTypes;
		if (onlyMedia)
		{
			mediaFileTypes = new HikeFileType[] { HikeFileType.IMAGE, HikeFileType.VIDEO };
		}
		else
		{
			mediaFileTypes = new HikeFileType[] { HikeFileType.OTHER, HikeFileType.AUDIO };
		}

		hfTypeSelection = new StringBuilder("(");
		for (HikeFileType hfType : mediaFileTypes)
		{
			hfTypeSelection.append(hfType.ordinal() + ",");
		}
		hfTypeSelection.replace(hfTypeSelection.lastIndexOf(","), hfTypeSelection.length(), ")");
		return hfTypeSelection.toString();

	}

	/*
	 * itemsToLeft : true implies all items which has msgId less than given msgId : false implies all items which has msgId greater than given msgId
	 * 
	 * returns list in order of msgId max to min.
	 */
	public List<?> getSharedMedia(String msisdn, int limit, long givenMsgId, boolean onlyMedia, boolean itemsToRight)
	{
		String msgIdSelection = DBConstants.MESSAGE_ID + (itemsToRight ? ">" : "<") + givenMsgId;
		String hfTypeSelection = getSharedMediaSelection(onlyMedia);

		String selection =  DBConstants.MSISDN + " = ?" + (givenMsgId == -1 ? "" : " AND " + msgIdSelection) + " AND "
				+ (DBConstants.HIKE_FILE_TYPE + " IN " + hfTypeSelection);

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.SHARED_MEDIA_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.GROUP_PARTICIPANT, DBConstants.TIMESTAMP, DBConstants.IS_SENT,
					DBConstants.MESSAGE_METADATA }, selection, new String[] { msisdn }, null, null, DBConstants.MESSAGE_ID + (itemsToRight ? " ASC" : " DESC"), null);

			List<?> sharedFilesList;
			if(onlyMedia)
			{
				sharedFilesList = new ArrayList<HikeSharedFile>(limit);
			}
			else
			{
				sharedFilesList = new ArrayList<FileListItem>(limit);
			}

			SharedMediaCursorIterator cursorIterator = new SharedMediaCursorIterator(c, msisdn);
			while (cursorIterator.hasNext() && limit>0)
			{
				HikeSharedFile hikeSharedFile = cursorIterator.next();
				if(hikeSharedFile.exactFilePathFileExists())
				{
					limit--;

					if (onlyMedia)
					{
						((List<HikeSharedFile>) sharedFilesList).add(hikeSharedFile);
					}
					else
					{
						((List<FileListItem>) sharedFilesList).add(new FileListItem(hikeSharedFile));
					}
				}
			}

			return sharedFilesList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private class SharedMediaCursorIterator implements Iterator<HikeSharedFile>
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
			msgIdIndex = cursor.getColumnIndex(DBConstants.MESSAGE_ID);
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
			return null;
		}

		@Override
		public void remove()
		{

		}
	};

	public int getSharedMediaCount(String msisdn, boolean onlyMedia)
	{
		int count = 0;
		String hfTypeSelection = getSharedMediaSelection(onlyMedia);

		Cursor c = null;

		String selection =  DBConstants.MSISDN + " = ?"  + " AND "
				+ (DBConstants.HIKE_FILE_TYPE + " IN " + hfTypeSelection);

		try
		{
			c = mDb.query(DBConstants.SHARED_MEDIA_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.GROUP_PARTICIPANT, DBConstants.TIMESTAMP, DBConstants.IS_SENT,
					DBConstants.MESSAGE_METADATA }, selection, new String[] { msisdn }, null, null, null, null);

			SharedMediaCursorIterator cursorIterator = new SharedMediaCursorIterator(c, msisdn);
			while (cursorIterator.hasNext())
			{
				HikeSharedFile hikeSharedFile = cursorIterator.next();
				if(hikeSharedFile.exactFilePathFileExists())
				{
					count++;
				}
			}
			return count;
		}
		finally
		{
			if(c!=null)
				c.close();
		}
	}

	public void deleteEmptyConversations(SQLiteDatabase mDb)
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSISDN, DBConstants.MESSAGE_METADATA }, null, null, null, null, null);

		int msgIdx = c.getColumnIndex(DBConstants.MESSAGE);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int metadataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

		StringBuilder deleteSelection = new StringBuilder("(");

		while (c.moveToNext())
		{
			String message = c.getString(msgIdx);
			String metadata = c.getString(metadataIdx);
			String msisdn = c.getString(msisdnIdx);

			if (message == null && metadata == null && !Utils.isGroupConversation(msisdn))
			{
				deleteSelection.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
		}

		String deleteSelectionString = deleteSelection.toString();
		if (!"(".equals(deleteSelectionString))
		{
			deleteSelectionString = new String(deleteSelectionString.substring(0, deleteSelectionString.length() - 1) + ")");
			mDb.delete(DBConstants.CONVERSATIONS_TABLE, DBConstants.MSISDN + " IN " + deleteSelectionString, null);
		}
	}

	/**
	 * Returns the sum of unread messages for all conversations.
	 *
	 * @return
	 */
	public int getTotalUnreadMessages()
	{
		int unreadMessages = 0;
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.UNREAD_COUNT }, null, null, null, null, null);

			final int unreadMessageColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);

			if (c.moveToFirst())
			{
				do
				{
					unreadMessages += c.getInt(unreadMessageColumn);
				}
				while (c.moveToNext());
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		unreadMessages += Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false);

		return unreadMessages;
	}

	public HashMap<String, ContentValues> getCurrentStickerDataMapping(String tableName)
	{
		HashMap<String, ContentValues> result = new HashMap<String, ContentValues>();
		Cursor c = null;
		try
		{
			c = mDb.query(tableName, new String[] { DBConstants.CATEGORY_ID, DBConstants.TOTAL_NUMBER, DBConstants.UPDATE_AVAILABLE }, null, null,
					null, null, null);

			final int catIdColumnIndex = c.getColumnIndex(DBConstants.CATEGORY_ID);
			final int totalNumColumnIndex = c.getColumnIndex(DBConstants.TOTAL_NUMBER);
			final int updateAvailColumnIndex = c.getColumnIndex(DBConstants.UPDATE_AVAILABLE);

			while (c.moveToNext())
			{
				String catId = c.getString(catIdColumnIndex);
				int totalNumber = c.getInt(totalNumColumnIndex);
				int updateAvailable = c.getInt(updateAvailColumnIndex);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.TOTAL_NUMBER, totalNumber);
				contentValues.put(DBConstants.UPDATE_AVAILABLE, updateAvailable);

				result.put(catId, contentValues);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateToNewStickerCategoryTable",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return result;
	}

	public String getStickerCategoryTableCreateQuery()
	{
		/**
		 * creating new Sticker category table with updated schema
		 * Category Id : Id of this category
		 * Category Name : Category name to show for this category
		 * Total Number : total number of stickers available in this category
		 * Reached End : weather we have downloaded all the stickers in this category or Not
		 * Update available : weather there is an update available for this category 
		 * IS_VISIBLE : weather this category is visible
		 * IS_CUSTOM : weather this is a custom category 
		 * category index : order of this category among all the categories
		 * metadata : other metadata associated with this category 
		 */
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKER_CATEGORIES_TABLE + " (" + DBConstants._ID + " TEXT PRIMARY KEY, " + DBConstants.CATEGORY_NAME
				+ " TEXT, " + DBConstants.TOTAL_NUMBER + " INTEGER, " + DBConstants.UPDATE_AVAILABLE + " INTEGER DEFAULT 0," + DBConstants.IS_VISIBLE + " INTEGER DEFAULT 0,"
				+ DBConstants.IS_CUSTOM + " INTEGER DEFAULT 0," + DBConstants.CATEGORY_INDEX + " INTEGER," + DBConstants.CATEGORY_SIZE + " INTEGER DEFAULT 0 " + " )";
		return sql;
	}


	public String getStickerShopTableCreateQuery()
	{
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKER_SHOP_TABLE + " (" + DBConstants._ID + " TEXT PRIMARY KEY, " + DBConstants.CATEGORY_NAME
				+ " TEXT, " + DBConstants.TOTAL_NUMBER + " INTEGER, " + DBConstants.CATEGORY_SIZE + " INTEGER DEFAULT 0 " + " )";

		return sql;
	}

	public void upgradeForStickerShopVersion1()
	{
		try
		{
			mDb.beginTransaction();
			insertAllCategoriesToStickersTable();
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

	public void insertAllCategoriesToStickersTable()
	{
		final String STICKERS_TABLE = "stickersTable";

		HashMap<String, ContentValues> currentCategoryData = getCurrentStickerDataMapping(STICKERS_TABLE);
		JSONObject jsonObj;
		try
		{
			jsonObj = new JSONObject(Utils.loadJSONFromAsset(mContext, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);
				String categoryName = obj.optString(StickerManager.CATEGORY_NAME);;
				String downloadPreference = obj.optString(StickerManager.DOWNLOAD_PREF);
				/*
				 * All categories which we have set to be visible OR user has already initiated a download for them once will be visible in the pallate
				 */
				int isVisible = obj.optBoolean(StickerManager.IS_VISIBLE)
						|| mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(downloadPreference, false) ? 1 : 0;
				int isCustom = obj.optBoolean(StickerManager.IS_CUSTOM) ? 1 : 0;
				int catIndex = i;

				ContentValues contentValues = currentCategoryData.get(categoryId);
				if (contentValues == null)
				{
					contentValues = new ContentValues();
					if(obj.has(StickerManager.TOTAL_STICKERS))
					{
						contentValues.put(DBConstants.TOTAL_NUMBER, obj.optString(StickerManager.TOTAL_STICKERS));
					}
				}
				contentValues.put(DBConstants._ID, categoryId);
				contentValues.put(DBConstants.CATEGORY_NAME, categoryName);
				contentValues.put(DBConstants.IS_VISIBLE, isVisible);
				contentValues.put(DBConstants.IS_CUSTOM, isCustom);
				contentValues.put(DBConstants.CATEGORY_INDEX, catIndex);

				if(isVisible == 1)
				{
					mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
				}
			}
			/*
			 * Now we can drop the old stickers_table.
			 */
			if(!currentCategoryData.isEmpty())
			{
				mDb.execSQL("DROP TABLE " + STICKERS_TABLE);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ContentValues getContentValuesForStickerShopTable(String categoryId, String categoryName, int categorySize)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, categoryId);
		contentValues.put(DBConstants.CATEGORY_NAME, categoryName);
		contentValues.put(DBConstants.CATEGORY_SIZE, categorySize);
		return contentValues;
	}

	public void updateVisibilityAndIndex(Set<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory stickerCategory : stickerCategories)
			{
				updateVisibilityAndIndex(stickerCategory);
			}
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

	public void updateVisibilityAndIndex(StickerCategory stickerCategory)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_VISIBLE, stickerCategory.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, stickerCategory.getCategoryIndex());

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { stickerCategory.getCategoryId() });
	}

	public void insertInToStickerCategoriesTable(StickerCategory stickerCategory)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, stickerCategory.getCategoryId());
		contentValues.put(DBConstants.CATEGORY_NAME, stickerCategory.getCategoryName());
		contentValues.put(DBConstants.TOTAL_NUMBER, stickerCategory.getTotalStickers());
		contentValues.put(DBConstants.UPDATE_AVAILABLE, stickerCategory.isUpdateAvailable());
		contentValues.put(DBConstants.IS_CUSTOM, stickerCategory.isCustom());
		contentValues.put(DBConstants.CATEGORY_SIZE, stickerCategory.getCategorySize());
		contentValues.put(DBConstants.IS_VISIBLE, stickerCategory.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, stickerCategory.getCategoryIndex());

		mDb.insertWithOnConflict(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
	}

	/*
	 * This method is called from sticker shop call responce.
	 */
	public void updateStickerCategoriesInDb(JSONArray jsonArray)
	{
		try
		{
			mDb.beginTransaction();
			for (int i=0; i<jsonArray.length(); i++)
			{
				JSONObject jsonObj  = jsonArray.getJSONObject(i);
				String catId = jsonObj.optString(StickerManager.CATEGORY_ID);

				if(TextUtils.isEmpty(catId))
				{
					continue;
				}
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants._ID, catId);
				if(jsonObj.has(HikeConstants.CAT_NAME))
				{
					contentValues.put(DBConstants.CATEGORY_NAME, jsonObj.getString(HikeConstants.CAT_NAME));
				}
				if(jsonObj.has(HikeConstants.NUMBER_OF_STICKERS))
				{
					contentValues.put(DBConstants.TOTAL_NUMBER, jsonObj.getInt(HikeConstants.NUMBER_OF_STICKERS));
				}
				if(jsonObj.has(HikeConstants.SIZE))
				{
					contentValues.put(DBConstants.CATEGORY_SIZE, jsonObj.getInt(HikeConstants.SIZE));
				}

				mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { catId });
				mDb.insertWithOnConflict(DBConstants.STICKER_SHOP_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
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

	public void updateStickerCategoriesInDb(Collection<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory category : stickerCategories)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants._ID, category.getCategoryId());
				contentValues.put(DBConstants.CATEGORY_NAME, category.getCategoryName());
				contentValues.put(DBConstants.TOTAL_NUMBER, category.getTotalStickers());
				contentValues.put(DBConstants.CATEGORY_SIZE, category.getCategorySize());
				
				/*
				 * Adding extra fields as per stickerCategoriesTable
				 */
				contentValues.put(DBConstants.IS_VISIBLE, category.isVisible());
				contentValues.put(DBConstants.CATEGORY_INDEX, category.getCategoryIndex());

				int rowsAffected = mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { category.getCategoryId() });
				/*
				 * if row is not there in stickerCategoriesTable and server specifically tells us to switch on the visibility 
				 * then we need to insert this item in stickerCategoriesTable
				 */
				if(category.isVisible() && rowsAffected == 0)
				{
					mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
				}

			}
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

	public Cursor getCursorForStickerShop()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STICKER_SHOP_TABLE, new String[] { DBConstants._ID, DBConstants.TOTAL_NUMBER, DBConstants.CATEGORY_NAME, DBConstants.CATEGORY_SIZE }, null, null,
					null, null, null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateToNewStickerCategoryTable",e);
		}
		return c;
	}

	public void clearStickerShop()
	{
		mDb.delete(DBConstants.STICKER_SHOP_TABLE, null, null);
	}

	/**
	 * Returns the maximum category index from StickerCategories Table
	 * @return
	 */
	public int getMaxStickerCategoryIndex()
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, new String[] { "MAX(" + DBConstants.CATEGORY_INDEX + ")" + "AS " + DBConstants.CATEGORY_INDEX }, null, null, null, null, null, null);

			if (c.moveToFirst())
			{
				return c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
			}
			else
				return -1;
		}

		catch (Exception e)
		{
			return -1;
		}

		finally
		{
			if (c != null)
				c.close();
		}
	}

	/**
	 * Returns true if category is inserted.
	 * @param category
	 * @return
	 */
	public boolean insertNewCategoryInPallete(StickerCategory category)
	{
		int rowId = -1;
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, category.getCategoryId());
		contentValues.put(DBConstants.CATEGORY_NAME, category.getCategoryName());
		contentValues.put(DBConstants.TOTAL_NUMBER, category.getTotalStickers());
		contentValues.put(DBConstants.CATEGORY_SIZE, category.getCategorySize());
		contentValues.put(DBConstants.IS_VISIBLE, category.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, category.getCategoryIndex());

		rowId = (int) mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);

		return rowId < 0 ? false : true;
	}



	/**
	 * Used to persist the changes made to the {@link StickerCategory#isUpdateAvailable()} flag
	 *
	 * @param category
	 */
	public void saveUpdateFlagOfStickerCategory(StickerCategory category)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.UPDATE_AVAILABLE, category.isUpdateAvailable());

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { category.getCategoryId() });

	}

	public void saveUpdateFlagOfStickerCategory(List<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory stickerCategory : stickerCategories)
			{
				saveUpdateFlagOfStickerCategory(stickerCategory);
			}
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

	public StickerCategory getStickerCategoryforId(String categoryId)
	{
		Cursor c = null;
		StickerCategory stickerCategory = null;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null, DBConstants._ID + "=?", new String[] { categoryId }, null, null, null);
			stickerCategory = parseStickerCategoryCursor(c);
		}

		catch (Exception e)
		{
			Logger.wtf("StickerManager", "Exception in getStickerCategoryforId  : " + e.toString());
		}

		finally
		{
			if(c != null)
			{
				c.close();
			}
		}

		return stickerCategory;
	}

	private StickerCategory parseStickerCategoryCursor(Cursor c)
	{
		StickerCategory stickerCategory = null;
		if(c.moveToFirst())
		{
			String categoryId = c.getString(c.getColumnIndex(DBConstants._ID));
			String categoryName = c.getString(c.getColumnIndex(DBConstants.CATEGORY_NAME));
			boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
			boolean isVisible = c.getInt(c.getColumnIndex(DBConstants.IS_VISIBLE)) == 1;
			boolean isCustom = c.getInt(c.getColumnIndex(DBConstants.IS_CUSTOM)) == 1;
			int catIndex = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
			int categorySize = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_SIZE));
			int totalStickers = c.getInt(c.getColumnIndex(DBConstants.TOTAL_NUMBER));

			stickerCategory = new StickerCategory(categoryId, categoryName, updateAvailable, isVisible, isCustom, true, catIndex, totalStickers, categorySize);
		}

		return stickerCategory;
	}


	public Cursor getMessage(String messageId)
	{
		String selection = DBConstants.MESSAGE_ID + "=?";
		return mDb.query(MESSAGES_TABLE, null, selection, new String[]{messageId}, null, null, null);
	}

	public void updateMetadataOfMessage(long messageId, String metadata)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(MESSAGE_ID, messageId);
		contentValues.put(MESSAGE_METADATA, metadata);
		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { String.valueOf(messageId) });
	}

	public String getMetadataOfMessage(long messageId){
		String selection = DBConstants.MESSAGE_ID + "=?";
		Cursor c = mDb.query(MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_METADATA }, selection, new String[] { String.valueOf(messageId) }, null, null, null);
		if(c.moveToFirst()){
			return c.getString(c.getColumnIndex(MESSAGE_METADATA));
		}
		return null;
	}

	/**
	 *
	 * @param msisdn
	 * @return unread count, -1 if conversation does not exist
	 */
	public int getConvUnreadCount(String msisdn){
		String selection = DBConstants.MSISDN + "=?";
		Cursor c = mDb.query(CONVERSATIONS_TABLE, new String[]{UNREAD_COUNT}, selection, new String[]{msisdn}, null, null, null);
		if(c.moveToFirst()){
			return c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
		}
		return -1;
	}

	/*
	 * 
	 * metadata:{'layout_id':'','file_id':'','card_data':{},'helper_data':{}}
	 * 
	 * This function reads helper json given in parameter and update it in metadata of message , it inserts new keys in metadata present in helper and updates old
	 */
	public String updateHelperData(long messageId, String helper)
	{

		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			try
			{
				JSONObject metadataJSON = new JSONObject(json);
				JSONObject helperData = new JSONObject(helper);
				JSONObject cardObj = metadataJSON.optJSONObject(HikePlatformConstants.CARD_OBJECT);
				JSONObject oldHelper = cardObj.optJSONObject(HikePlatformConstants.HELPER_DATA);
				if (oldHelper == null)
				{
					oldHelper = new JSONObject();
				}
				Iterator<String> i = helperData.keys();
				while (i.hasNext())
				{
					String key = i.next();
					oldHelper.put(key, helperData.get(key));
				}
				cardObj.put(HikePlatformConstants.HELPER_DATA, oldHelper);
				metadataJSON.put(HikePlatformConstants.CARD_OBJECT, cardObj);
				json = metadataJSON.toString();
				updateMetadataOfMessage(messageId, json);
				return json;
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Logger.e("HikeconversationDB", "Meta data of message is null, id= " + messageId);
		}
		return null;
	}

	public String updateJSONMetadata(int messageId, String newMeta)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			try
			{
				JSONObject metadataJSON = new JSONObject(json);
				JSONObject newMetaData = new JSONObject(newMeta);
				Iterator<String> i = newMetaData.keys();
				while (i.hasNext())
				{
					String key = i.next();
					if (newMetaData.get(key) != null)
					{
						//Algo is that there might be JSONObject as the value and we might want to update some keys inside that JSONObject, so we are using the try
						//catch block for that. If JSONException is thrown, we will directly update the JSON, else will iterate again and then update.
						try
						{

							JSONObject newMetaJSON = new JSONObject(String.valueOf(newMetaData.get(key)));
							JSONObject metaJSON = new JSONObject(String.valueOf(metadataJSON.get(key)));
							Iterator<String> index = newMetaJSON.keys();
							while (index.hasNext())
							{
								String indexKey = index.next();
								metaJSON.put(indexKey, newMetaJSON.get(indexKey));
							}
							metadataJSON.put(key, metaJSON);
							Logger.d(mContext.getClass().getSimpleName(), "values are JSONObject, so updating the iterated JSON.");
						}
						catch (JSONException e)
						{
							Logger.d(mContext.getClass().getSimpleName(), "JSON exception thrown, so updating the original JSON directly.");
							metadataJSON.put(key, newMetaData.get(key));
						}

					}
				}
				json = metadataJSON.toString();
				updateMetadataOfMessage(messageId, json);
				return json;
			}
			catch (JSONException e)
			{
				Logger.e("HikeconversationDB", "JSOn Exception = " + e.getMessage());
			}
		}
		else
		{
			Logger.e("HikeconversationDB", "Meta data of message is null, id= " + messageId);
		}
		return null;
	}

	public void deleteAppAlarm(int messageId)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			{
				JSONObject metadataJSON = null;
				try
				{
					metadataJSON = new JSONObject(json);
					metadataJSON.remove(HikePlatformConstants.ALARM_DATA);
					updateMetadataOfMessage(messageId, metadataJSON.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		}
	}

	public void insertMicroAppALarm(int messageId,String alarmData)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			{

				try
				{
					JSONObject metadataJSON = new JSONObject(json);
					metadataJSON.put(HikePlatformConstants.ALARM_DATA, alarmData);
					updateMetadataOfMessage(messageId, metadataJSON.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		}
	}

	public boolean upgradeForServerIdField()
	{
		boolean result = false;
		try
		{
			mDb.beginTransaction();

			long startTime = System.currentTimeMillis();

			String update1 = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			String update2 = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			String update3 = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.SORTING_TIMESTAMP + " = "  + DBConstants.LAST_MESSAGE_TIMESTAMP;
			String update4 = "UPDATE " + DBConstants.SHARED_MEDIA_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			mDb.execSQL(update1);
			mDb.execSQL(update2);
			mDb.execSQL(update3);
			mDb.execSQL(update4);
			createIndexOverServerIdField(mDb);

			long endTime = System.currentTimeMillis();

			Logger.d("HikeConversationDatadase", " ServerId db upgrade time : "+(startTime-endTime));

			mDb.setTransactionSuccessful();
			result = true;
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
			result = false;
		}
		finally
		{
			mDb.endTransaction();
		}

		return result;
	}

	public boolean updateSortingTimestamp(String msisdn, long timestamp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.SORTING_TIMESTAMP, timestamp);
		int rowsUpdated = mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		boolean updated = (rowsUpdated != 0);
		return (updated);
	}

	public boolean isContentMessageExist(String msisdn, String contentId, String nameSpace)

	{
		String where = DBConstants.MSISDN + "=? and " + DBConstants.HIKE_CONTENT.CONTENT_ID + "=? and " + HIKE_CONTENT.NAMESPACE + "=?";
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID }, where, new String[] { msisdn, contentId, nameSpace }, null, null, null);
		try
		{
			boolean toReturn = c.moveToFirst();
			return toReturn;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}


	public Map<String, ArrayList<Long>> getMsisdnMapForServerId(Long serverId, String excludingMsisdn)
	{
		ArrayList<Long> al = new ArrayList<Long>();
		al.add(serverId);
		Map<String, ArrayList<Long>> map = getMsisdnMapForServerIds(al, excludingMsisdn);

		return map;
	}
	
	/*
	 * return a map from msisdn to list of server ids. for all given msgIds entries in messages table.
	 */
	public Map<String, ArrayList<Long>> getMsisdnMapForServerIds(ArrayList<Long> serverIds, String userMsisdn)
	{
		Cursor c = null;
		Map<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

		if(serverIds == null || serverIds.isEmpty())
		{
			return map;
		}
		try
		{
			/*
			 * quering for all rows where serverIds are in set of given serverIds
			 * 1. msgid = serverid (normal msg case OR msg of the broadcast conversation in case of broadcast)
			 * 2. OR msisdn = givenMsisdn
			 */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE_ID, DBConstants.SERVER_ID },
					DBConstants.SERVER_ID + " IN " + Utils.valuesToCommaSepratedString(serverIds), null, null, null, null);
			final int msisdnColumnIdIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int messageIdColumnIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int serverIdColumnIdIdx = c.getColumnIndex(DBConstants.SERVER_ID);
			while (c.moveToNext())
			{
				String msisdnIdColumnValue = c.getString(msisdnColumnIdIdx);
				long messageIdColumnValue = c.getLong(messageIdColumnIdIdx);
				long serverIdColumnValue = c.getLong(serverIdColumnIdIdx);
				
				/*
				 * selecting only items from result set in which either
				 * 1. msgid = serverid (normal msg case OR msg of the broadcast conversation in case of broadcast)
				 * 2. OR msisdn = givenMsisdn
				 */
				if(messageIdColumnValue == serverIdColumnValue || msisdnIdColumnValue.equals(userMsisdn) )
				{
					ArrayList<Long> msgIdArray = map.get(msisdnIdColumnValue);
					if (msgIdArray == null)
					{
						msgIdArray = new ArrayList<Long>();
						map.put(msisdnIdColumnValue, msgIdArray);
					}
					msgIdArray.add(messageIdColumnValue);
				}
			}
		}
		finally
		{
			if (null != c)
			{
				c.close();
			}

		}
		return map;
	}
}
