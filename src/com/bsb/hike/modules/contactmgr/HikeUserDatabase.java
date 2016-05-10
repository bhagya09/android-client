package com.bsb.hike.modules.contactmgr;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatHead.CallerContentModel;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.DatabaseErrorHandlers.CustomDatabaseErrorHandler;
import com.bsb.hike.db.DbException;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.FetchUIDTaskPojo;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class HikeUserDatabase extends SQLiteOpenHelper implements HikePubSub.Listener
{
	private static final String TAG = "HikeUserDatabase";
	private static volatile SQLiteDatabase mDb;

	private static volatile HikeUserDatabase hikeUserDatabase;
	
	private SQLiteDatabase mReadDb;

	private Context mContext;

	private String[] pubSubEvents = new String[]{HikePubSub.NEW_CONVERSATION, HikePubSub.CONVERSATION_DELETED};
	
	private HikeUserDatabase(Context context)
	{
		super(context, DBConstants.USERS_DATABASE_NAME, null, DBConstants.USERS_DATABASE_VERSION, new CustomDatabaseErrorHandler());
		this.mContext = context;
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
		initPubSubListeners();
	}

	private void initPubSubListeners() {
		HikeMessengerApp.getPubSub().addListeners(this,pubSubEvents);
	}

	public static HikeUserDatabase getInstance()
	{
		if (hikeUserDatabase == null)
		{
			synchronized (HikeUserDatabase.class)
			{
				if (hikeUserDatabase == null)
				{
					hikeUserDatabase = new HikeUserDatabase(HikeMessengerApp.getInstance().getApplicationContext());
				}
			}
		}
		return hikeUserDatabase;
	}

	public void reinitializeDB()
	{
		Logger.d(getClass().getSimpleName(), "Reinitialising user DB");
		close();
		Logger.d(getClass().getSimpleName(), "User DB is closed now");

		hikeUserDatabase = new HikeUserDatabase(HikeMessengerApp.getInstance().getApplicationContext());
		/*
		 * We can remove this line, if we can guarantee, NoOne keeps a local copy of HikeUserDatabase.
		 * right now we store convDb reference in some classes and use that refenence to query db. ex. DbConversationListener.
		 * i.e. on restore we have two objects of HikeConversationsDatabase in memory.
		 */
		mDb = hikeUserDatabase.getWriteDatabase();
		mReadDb = hikeUserDatabase.getReadableDatabase();
		Logger.d(getClass().getSimpleName(), "User DB initialization is complete");
	}

	public SQLiteDatabase getWriteDatabase()
	{
		if(mDb == null || !mDb.isOpen())
		{
			mDb = super.getWritableDatabase();
		}
		return mDb;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE
				+ " ( "
				+ DBConstants.ID + " STRING , " // Contact ID. Not used.
				+ DBConstants.NAME + " TEXT , " // Contact name
				+ DBConstants.MSISDN + " TEXT COLLATE nocase, " // Contact normalised msisdn
				+ DBConstants.ONHIKE + " INTEGER DEFAULT 0, " // Contact's on hike status
				+ DBConstants.PHONE + " TEXT, " // Contact's phone number in the user's contacts DB
				+ DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, " // Whether the contact has a custom avatar or not. Not used
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
				+ DBConstants.MSISDN_TYPE + " STRING, " // The msisdn type
				+ DBConstants.LAST_MESSAGED + " INTEGER, " // When this user was last messaged
				+ DBConstants.HIKE_JOIN_TIME + " INTEGER DEFAULT 0, " // When this user joined hike
				+ DBConstants.LAST_SEEN + " INTEGER DEFAULT -1, " // When this user was last seen on hike
				+ DBConstants.IS_OFFLINE + " INTEGER DEFAULT 1, " // Whether this user is online or not
				+ DBConstants.INVITE_TIMESTAMP + " INTEGER DEFAULT 0, " // When this user was last invited.
				+ DBConstants.PLATFORM_USER_ID + " TEXT DEFAULT '' ,"    // Platform user id
				+ DBConstants.HIKE_UID + " TEXT DEFAULT NULL , "
				+ DBConstants.BLOCK_STATUS + " TEXT DEFAULT 0 ,"
				+ DBConstants.FAVORITE_TYPE + " TEXT DEFAULT 0 "
				+ " )";

		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.THUMBNAILS_TABLE + " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " ) ";
		db.execSQL(create);

		//create = "CREATE TABLE IF NOT EXISTS " + DBConstants.FAVORITES_TABLE + " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, " + DBConstants.FAVORITE_TYPE + " INTEGER" + " ) ";
		//db.execSQL(create);

		create = getHikeCallerTable();
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.USER_INDEX + " ON " + DBConstants.USERS_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);

		create = "CREATE INDEX IF NOT EXISTS " + DBConstants.THUMBNAIL_INDEX + " ON " + DBConstants.THUMBNAILS_TABLE + " (" + DBConstants.MSISDN + ")";
		db.execSQL(create);

		//create = "CREATE INDEX IF NOT EXISTS " + DBConstants.FAVORITE_INDEX + " ON " + DBConstants.FAVORITES_TABLE + " (" + DBConstants.MSISDN + ")";
		//db.execSQL(create);

		for(String s: getIndexQueriesForV19())
		{
			db.execSQL(s);
		}
	}

	private List<String> getIndexQueriesForV19() {
		int i = 0;
		String indexQuery = null;
		List<String> index=new ArrayList<>();

		indexQuery = "CREATE INDEX IF NOT EXISTS " + DBConstants.BLOCK_STATUS_INDEX + " ON " + DBConstants.USERS_TABLE + " (" + DBConstants.BLOCK_STATUS + ")";
		index.add(indexQuery);

		indexQuery = "CREATE INDEX IF NOT EXISTS " + DBConstants.FAVORITE_INDEX + " ON " + DBConstants.USERS_TABLE + " (" + DBConstants.FAVORITE_TYPE + ")";
		index.add(indexQuery);
		return index;
	}

	@Override
	public void close()
	{
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Logger.d(getClass().getSimpleName(), "Upgrading users table from " + oldVersion + " to " + newVersion);
		if (oldVersion < 3)
		{
			String alter1 = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.MSISDN_TYPE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.LAST_MESSAGED + " INTEGER";
			db.execSQL(alter1);
			db.execSQL(alter2);
		}
		// Changing the datatype of the name column
		if (oldVersion < 4)
		{
			Logger.d(getClass().getSimpleName(), "Updating table");
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " RENAME TO " + "temp_table";

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE + " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME + " TEXT, " + DBConstants.MSISDN
					+ " TEXT COLLATE nocase, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE + " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MSISDN_TYPE + " STRING, " + DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE + " SELECT * FROM temp_table";

			String drop = "DROP TABLE IF EXISTS temp_table";

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		/*
		 * Keeping this as comments to show why we need the next if clause.
		 */
		// Add favorite column
		// if (oldVersion < 5) {
		// String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
		// + " ADD COLUMN " + DBConstants.FAVORITE
		// + " INTEGER DEFAULT " + FavoriteType.NOT_FAVORITE.ordinal();
		// db.execSQL(alter);
		// }
		if (oldVersion < 7)
		{
			// Create the favorites table.
			onCreate(db);

			String tempTable = "tempTable";
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " RENAME TO " + tempTable;

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE + " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME + " TEXT, " + DBConstants.MSISDN
					+ " TEXT COLLATE nocase, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE + " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " + DBConstants.MSISDN_TYPE + " STRING, " + DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE + " SELECT " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", "
					+ DBConstants.ONHIKE + ", " + DBConstants.PHONE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", " + DBConstants.OVERLAY_DISMISSED + ", " + DBConstants.MSISDN_TYPE
					+ ", " + DBConstants.LAST_MESSAGED + " FROM " + tempTable;

			String drop = "DROP TABLE IF EXISTS " + tempTable;

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		if (oldVersion < 8)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.HIKE_JOIN_TIME + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}
		if (oldVersion < 10)
		{
			/*
			 * Removing all auto recommended favorites.
			 */
			db.delete(DBConstants.FAVORITES_TABLE, DBConstants.FAVORITE_TYPE + "=" + FavoriteType.AUTO_RECOMMENDED_FAVORITE.ordinal(), null);
		}
		/*
		 * Version 11 for adding indexes.
		 */
		if (oldVersion < 11)
		{
			onCreate(db);
		}
		/*
		 * Version 12 for added last seen column
		 */
		if (oldVersion < 12)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.LAST_SEEN + " INTEGER DEFAULT -1";
			db.execSQL(alter);
		}
		/*
		 * Version 13 for is online column
		 */
		if (oldVersion < 13)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.IS_OFFLINE + " INTEGER DEFAULT 1";
			db.execSQL(alter);
		}

		/*
		 * Version 14 is for the rounded thumbnails
		 * Now we have removed rounded thumbnails table. So no need to create it anymore
		 */
		//if (oldVersion < 14)
		//{
		//}
		/*
		 * Version 15 adds the invited timestamp column
		 */
		if (oldVersion < 15)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.INVITE_TIMESTAMP + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}
		
		// Now we have removing rounded thumbnails table. Using RoundedImageView to create rounded thumbnails instead
		if (oldVersion < 16)
		{
			String drop = "DROP TABLE IF EXISTS " + DBConstants.ROUNDED_THUMBNAIL_TABLE;
			db.execSQL(drop);
		}

		if (oldVersion < 17)
		{
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.PLATFORM_USER_ID + " TEXT";
			db.execSQL(alter);

			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikePlatformConstants.PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH, HikePlatformConstants.MAKE_HTTP_CALL);
			editor.commit();
		}
		if (oldVersion < 18)
		{
			db.execSQL(getHikeCallerTable());
		}
		if(oldVersion < 19) {
			//TODO : Handle migration

			if (!Utils.isColumnExistsInTable(mDb, DBConstants.USERS_TABLE, DBConstants.HIKE_UID)) {
				String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.HIKE_UID + " TEXT";
				db.execSQL(alter);
			}

			if (!Utils.isColumnExistsInTable(mDb, DBConstants.USERS_TABLE, DBConstants.BLOCK_STATUS)) {
				String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.BLOCK_STATUS + " TEXT DEFAULT 0";
				db.execSQL(alter);
			}

			if (!Utils.isColumnExistsInTable(mDb, DBConstants.USERS_TABLE, DBConstants.FAVORITE_TYPE)) {
				String alter = "ALTER TABLE " + DBConstants.USERS_TABLE + " ADD COLUMN " + DBConstants.FAVORITE_TYPE + " TEXT DEFAULT 0";
				db.execSQL(alter);
			}

			for (String s : getIndexQueriesForV19()) {
				db.execSQL(s);
			}
			// Need to migrate the DBs in upgradeIntentService
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.MIGRATE_TABLE_TO_USER, 1);
		}

	}

	public void clearTable(String tableName)
	{
		mDb.delete(tableName, null, null);
	}

	private String getHikeCallerTable()
	{

		String hikeCallerTable = DBConstants.CREATE_TABLE + DBConstants.HIKE_USER.HIKE_CALLER_TABLE + " (" + BaseColumns._ID + " INTEGER , " + DBConstants.MSISDN + " TEXT PRIMARY KEY , " + DBConstants.NAME
				+ " TEXT NOT NULL, " + DBConstants.HIKE_USER.LOCATION + " TEXT, " + DBConstants.HIKE_USER.IS_ON_HIKE + " INTEGER DEFAULT 0, " + DBConstants.HIKE_USER.IS_SPAM + " INTEGER DEFAULT 0, "
				+ DBConstants.HIKE_USER.IS_BLOCK + " INTEGER DEFAULT 0," + DBConstants.HIKE_USER.SPAM_COUNT + " INTEGER DEFAULT 0," + DBConstants.HIKE_USER.CREATION_TIME + " INTEGER, " + DBConstants.HIKE_USER.ON_HIKE_TIME + " INTEGER, " + DBConstants.HIKE_USER.IS_SYNCED + " INTEGER DEFAULT 1 "+ ")";

		return hikeCallerTable;
	}

	public CallerContentModel getCallerContentModelFromMsisdn(String msisdn)
	{
		try
		{
			Cursor cursor = mDb.query(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, null, DBConstants.MSISDN + "=? ", new String[] { msisdn }, null, null, null, "1");
			cursor.moveToFirst();
			CallerContentModel callerContentModel = new CallerContentModel();
			callerContentModel.setBlock(!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_BLOCK))
					&& (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_BLOCK)) == ChatHeadUtils.VALUE_TRUE));
			callerContentModel.setFullName(cursor.getString(cursor.getColumnIndex(DBConstants.NAME)));
			callerContentModel.setIsOnHike(!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_ON_HIKE))
					&& (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_ON_HIKE)) == ChatHeadUtils.VALUE_TRUE));
			callerContentModel.setIsSpam(!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_SPAM))
					&& (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_SPAM)) == ChatHeadUtils.VALUE_TRUE));
			callerContentModel.setIsSynced(!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_SYNCED))
					&& (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_SYNCED)) == ChatHeadUtils.VALUE_TRUE));
			callerContentModel.setLocation(cursor.getString(cursor.getColumnIndex(DBConstants.HIKE_USER.LOCATION)));
			callerContentModel.setSpamCount(
					!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.SPAM_COUNT)) ? cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.SPAM_COUNT)) : 0);
			callerContentModel.setMsisdn(cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN)));
			callerContentModel.setCreationTime(
					!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.CREATION_TIME)) ? cursor.getLong(cursor.getColumnIndex(DBConstants.HIKE_USER.CREATION_TIME)) : 0);
			callerContentModel.setUpdationTime(
					!cursor.isNull(cursor.getColumnIndex(DBConstants.HIKE_USER.ON_HIKE_TIME)) ? cursor.getLong(cursor.getColumnIndex(DBConstants.HIKE_USER.ON_HIKE_TIME)) : 0);

			return callerContentModel;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Cursor getCallerBlockContactCursor()
	{
		Cursor cursor =  mDb.query(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, new String[]{DBConstants.MSISDN, DBConstants.NAME, DBConstants.HIKE_USER.IS_BLOCK, BaseColumns._ID}, DBConstants.HIKE_USER.IS_BLOCK + "=? ", new String[] {"1"}, null, null, DBConstants.NAME + " ASC", null);
		cursor.moveToFirst();
		return cursor;
	}

	public void updateCallerSyncStatus(JSONObject callerSyncJSON)
	{
		JSONArray blockedArray = null;
		JSONArray unBlockedArray = null;
		try
		{
			if (callerSyncJSON.has(StickyCaller.BLOCKED_LIST))
			{
				blockedArray = callerSyncJSON.getJSONArray(StickyCaller.BLOCKED_LIST);
			}
			if (callerSyncJSON.has(StickyCaller.UNBLOCKED_LIST))
			{
				unBlockedArray = callerSyncJSON.getJSONArray(StickyCaller.UNBLOCKED_LIST);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		if (blockedArray != null && blockedArray.length() >0)
		{
			String blockedString = blockedArray.toString();
			blockedString = blockedString.replace("[", "").replace("]", "");
			ContentValues blocked = new ContentValues();
			blocked.put(DBConstants.HIKE_USER.IS_SYNCED, ChatHeadUtils.VALUE_TRUE);
			mDb.update(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, blocked, DBConstants.HIKE_USER.IS_BLOCK + "=" + ChatHeadUtils.VALUE_TRUE + " AND " + DBConstants.MSISDN + " IN (" + blockedString + ")", null);
		}
		if (unBlockedArray != null && unBlockedArray.length() >0)
		{
			String unBlockedString = unBlockedArray.toString();
			unBlockedString = unBlockedString.replace("[", "").replace("]", "");
			ContentValues unBlocked = new ContentValues();
			unBlocked.put(DBConstants.HIKE_USER.IS_SYNCED, ChatHeadUtils.VALUE_TRUE);
			mDb.update(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, unBlocked, DBConstants.HIKE_USER.IS_BLOCK + "=" + ChatHeadUtils.VALUE_FALSE + " AND " + DBConstants.MSISDN + " IN (" + unBlockedString + ")", null);
		}
	}


	public Cursor getAllUnsyncedContactCursor()
	{
		Cursor cursor =  mDb.query(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, new String[]{DBConstants.MSISDN, DBConstants.HIKE_USER.IS_BLOCK}, DBConstants.HIKE_USER.IS_SYNCED + "=? ", new String[] {String.valueOf(ChatHeadUtils.VALUE_FALSE)}, null, null, null, null);
		cursor.moveToFirst();
		return cursor;
	}

	void addContacts(List<ContactInfo> contacts, boolean isFirstSync) throws DbException
	{
		SQLiteDatabase db = mDb;
		db.beginTransaction();

		Map<String, String> msisdnTypeMap = new HashMap<String, String>();
		/*
		 * Since this is the first sync, we just run one query and pickup all the extra info required. For all subsequent syncs we run the query for each contact separately.
		 */
		if (isFirstSync)
		{
			// Adding the last contacted and phone type info
			Cursor extraInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null, null, null);

			int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
			int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
			int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

			while (extraInfo.moveToNext())
			{
				String msisdnType = Phone.getTypeLabel(this.mContext.getResources(), extraInfo.getInt(typeIdx), extraInfo.getString(labelIdx)).toString();

				msisdnTypeMap.put(extraInfo.getString(msisdnIdx), msisdnType);
			}
			extraInfo.close();
		}

		ContentValues cv=new ContentValues();
		try
		{
			for (ContactInfo contact : contacts)
			{
				long updatedRows = 0l;
				cv.put(DBConstants.NAME, contact.getName());
				cv.put(DBConstants.MSISDN, contact.getMsisdn());
				cv.put(DBConstants.ID, contact.getId());

				cv.put(DBConstants.ONHIKE, contact.isOnhike());
				cv.put(DBConstants.PHONE, contact.getPhoneNum());
				cv.put(DBConstants.PLATFORM_USER_ID, contact.getPlatformId());
				cv.put(DBConstants.FAVORITE_TYPE, contact.getFavoriteType() != null ? contact.getFavoriteType().ordinal() : FavoriteType.NOT_FRIEND.ordinal());
				cv.put(DBConstants.BLOCK_STATUS,contact.getBlockedStatus());
				cv.put(DBConstants.HIKE_UID,contact.getUid());
				if (!isFirstSync)
				{
					String selection = Phone.CONTACT_ID + " =? " + " AND " + Phone.NUMBER + " =? ";
					// Adding the last contacted and phone type info
					Cursor additionalInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.TYPE, Phone.LABEL }, selection,
							new String[] { contact.getId(), contact.getPhoneNum() }, null);

					int typeIdx = additionalInfo.getColumnIndex(Phone.TYPE);
					int labelIdx = additionalInfo.getColumnIndex(Phone.LABEL);
					if (additionalInfo.moveToFirst())
					{
						contact.setMsisdnType(Phone.getTypeLabel(this.mContext.getResources(), additionalInfo.getInt(typeIdx), additionalInfo.getString(labelIdx)).toString());
					}
					additionalInfo.close();

					cv.put(DBConstants.MSISDN_TYPE, contact.getMsisdnType());

					/*
					 * We add to favorites this reference. So should set the favorite type here.
					 */
					Cursor favoriteCursor = mDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.FAVORITE_TYPE ,DBConstants.ID,DBConstants.PHONE,DBConstants.BLOCK_STATUS}, DBConstants.MSISDN + "=?",
							new String[] { contact.getMsisdn() }, null, null, null);
					try
					{
							while(favoriteCursor.moveToNext()) {
								int favoriteTypeOrdinal = favoriteCursor.getInt(favoriteCursor.getColumnIndex(DBConstants.FAVORITE_TYPE));
								contact.setFavoriteType(FavoriteType.values()[favoriteTypeOrdinal]);
								cv.put(DBConstants.FAVORITE_TYPE, favoriteTypeOrdinal);

								String contactId = favoriteCursor.getString(favoriteCursor.getColumnIndex(DBConstants.ID));
								String phoneNumner = favoriteCursor.getString(favoriteCursor.getColumnIndex(DBConstants.PHONE));
								String blockStatus = favoriteCursor.getString(favoriteCursor.getColumnIndex(DBConstants.BLOCK_STATUS));
								cv.put(DBConstants.BLOCK_STATUS, blockStatus);
								if (TextUtils.isEmpty(contactId) && TextUtils.isEmpty(phoneNumner)) {
									Logger.d(TAG, "Going to update on User Db as we found a null id contact");


									selection = DBConstants.MSISDN + " =? AND " +DBConstants.ID + " is null AND " + DBConstants.PHONE + " is null";
									updatedRows = mDb.update(DBConstants.USERS_TABLE, cv, selection, new String[]{contact.getMsisdn()});
									Logger.d(TAG, "MSISDN ADDED" + contact.getMsisdn() + "result -->" + updatedRows + "UPDATE EXECUTED");
									break;
								} else {
									Logger.d(TAG, "not exceuting update as the contact id is not null");
								}
							}
						}
					finally
					{
						favoriteCursor.close();
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_ADDED, contact);
				}
				else
				{
					cv.put(DBConstants.MSISDN_TYPE, msisdnTypeMap.get(contact.getPhoneNum()));
					/*
					 * We're saving this parameter to notify that the extra info that we are now fetching (Msisdn type) has been synced. So for apps that update from an older
					 * version, we can just check this value to verify whether the contacts have their extra info synced.
					 */
					Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
					editor.commit();
				}
				if (updatedRows == 0) {
					long value = mDb.insertWithOnConflict(DBConstants.USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
					Logger.d(TAG, "MSISDN ADDED" + contact.getMsisdn() + "result -->" + value + "INSERT EXECUTED");
				}
				cv.clear();
			}
			db.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		}
		finally
		{
			db.endTransaction();
		}
	}



	public void insertIntoCallerTable(CallerContentModel callerContentModel, boolean isCompleteData, boolean setIsBlock)
	{
		if (callerContentModel != null && callerContentModel.getMsisdn() != null && callerContentModel.getFullName() != null)
		{
			ContentValues contentValues = getBasicCallerContentValues(callerContentModel, isCompleteData, setIsBlock);
			contentValues.put(DBConstants.NAME, callerContentModel.getFullName());
			contentValues.put(DBConstants.MSISDN, callerContentModel.getMsisdn());
			contentValues.put(DBConstants.HIKE_USER.CREATION_TIME, System.currentTimeMillis());
			mDb.insertWithOnConflict(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
		}
	}

	public void insertAllBlockedContactsIntoCallerTable(ArrayList<CallerContentModel> callerContent)
	{
		try
		{
			mDb.beginTransaction();
			if (callerContent != null)
			{
				for (CallerContentModel callerContentModel : callerContent)
				{
					if (callerContentModel != null && callerContentModel.getMsisdn() != null && callerContentModel.getFullName() != null)
					{
						ContentValues contentValues = getBasicCallerContentValues(callerContentModel, true, true);
						contentValues.put(DBConstants.NAME, callerContentModel.getFullName());
						contentValues.put(DBConstants.MSISDN, callerContentModel.getMsisdn());
						contentValues.put(DBConstants.HIKE_USER.CREATION_TIME, System.currentTimeMillis());
						mDb.insertWithOnConflict(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
					}
				}
			}
			mDb.setTransactionSuccessful();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CALLER_BLOKED_LIST_SYNCHED_SIGNUP, true);
		}
		finally
		{
			mDb.endTransaction();
		}

	}

	private ContentValues getBasicCallerContentValues(CallerContentModel callerContentModel, boolean setIsOnHikeTime, boolean setIsBlock)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.HIKE_USER.IS_ON_HIKE, callerContentModel.getIsOnHike() ? ChatHeadUtils.VALUE_TRUE : ChatHeadUtils.VALUE_FALSE);
		contentValues.put(DBConstants.HIKE_USER.LOCATION, callerContentModel.getLocation());
		contentValues.put(DBConstants.HIKE_USER.IS_SPAM, callerContentModel.isSpam() ? ChatHeadUtils.VALUE_TRUE : ChatHeadUtils.VALUE_FALSE);
		contentValues.put(DBConstants.HIKE_USER.SPAM_COUNT, callerContentModel.getSpamCount());
		contentValues.put(DBConstants.HIKE_USER.ON_HIKE_TIME, setIsOnHikeTime ? System.currentTimeMillis() : 0);
		if (setIsBlock)
		{
			contentValues.put(DBConstants.HIKE_USER.IS_BLOCK, callerContentModel.isBlock() ? ChatHeadUtils.VALUE_TRUE : ChatHeadUtils.VALUE_FALSE);
		}
		return  contentValues;
	}

	public void updateCallerTable(CallerContentModel callerContentModel, boolean setIsBlock)
	{
		if (callerContentModel != null && callerContentModel.getMsisdn() != null)
		{
			ContentValues contentValues = getBasicCallerContentValues(callerContentModel, true, setIsBlock);
			if (callerContentModel.getFullName() != null)
			{
				contentValues.put(DBConstants.HIKE_USER.CREATION_TIME, System.currentTimeMillis());
				if (ChatHeadUtils.getNameFromNumber(mContext, callerContentModel.getMsisdn())  == null)
				{
					contentValues.put(DBConstants.NAME, callerContentModel.getFullName());
				}
			}
			int noOfRow = mDb.update(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, contentValues, DBConstants.MSISDN + "=? ", new String[] { callerContentModel.getMsisdn() });
			if (noOfRow == 0 && callerContentModel.getFullName() != null)
			{
				insertIntoCallerTable(callerContentModel, true, setIsBlock);
			}
		}
	}

	public int updateBlockStatusIntoCallerTable(String msisdn, ContentValues contentValues)
	{
		if (msisdn != null)
		{
			return mDb.update(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, contentValues, DBConstants.MSISDN + "=? ", new String[] { msisdn });
		}
		return 0;
	}

	public void addBlockList(List<PairModified<String,String>> msisdns)
	{
		Logger.d(TAG, "Going to insert into block into User Table");
		if (Utils.isEmpty(msisdns)) {
			return;
		}

		ContentValues cv = new ContentValues();
		mDb.beginTransaction();
		try {
			for (PairModified<String,String> msisdn : msisdns) {
				Logger.d(TAG, "Adding msisdb to block" + msisdn);
				cv.put(DBConstants.MSISDN, msisdn.getFirst());
				if (!TextUtils.isEmpty(msisdn.getSecond())) {
					cv.put(DBConstants.HIKE_UID, msisdn.getSecond());
				}
				cv.put(DBConstants.BLOCK_STATUS, DBConstants.STATUS_BLOCKED);
				long value = mDb.update(DBConstants.USERS_TABLE, cv, DBConstants.MSISDN + "=?", new String[]{msisdn.getFirst()});
				if (value == -1 || value == 0) {

					value = mDb.insertWithOnConflict(DBConstants.USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
					Logger.d(TAG, "MSISDN BLOCKED" + msisdn + "result -->" + value + "INSERT EXECUTED");
				} else {
					Logger.d(TAG, "MSISDN BLOCKED" + msisdn + "result -->" + value + "UPDATE EXECUTED");
				}

			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}


	/**
	 * Sets the address book from the list of contacts Deletes any existing contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 */
	void setAddressBookAndBlockList(List<ContactInfo> contacts, List<PairModified<String,String>> blockedMsisdns) throws DbException
	{
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		//mDb.delete(DBConstants.BLOCK_TABLE, null, null);

		addContacts(contacts, true);
		addBlockList(blockedMsisdns);
	}


	private ContactInfo processContact(Cursor c)
	{
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);
		int hikeJoinTimeIdx = c.getColumnIndex(DBConstants.HIKE_JOIN_TIME);
		int isOfflineIdx = c.getColumnIndex(DBConstants.IS_OFFLINE);
		int lastSeenTimeIdx = c.getColumnIndex(DBConstants.LAST_SEEN);
		int platformIdIndex = c.getColumnIndex(DBConstants.PLATFORM_USER_ID);
		int blockStatusIndex = c.getColumnIndex(DBConstants.BLOCK_STATUS);


		long hikeJoinTime = 0;
		if (hikeJoinTimeIdx != -1)
		{
			hikeJoinTime = c.getLong(hikeJoinTimeIdx);
		}
		String id = TextUtils.isEmpty(c.getString(idx)) ? c.getString(msisdnIdx) : c.getString(idx);

		ContactInfo contactInfo = new ContactInfo(id, c.getString(msisdnIdx), c.getString(nameIdx), c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
				c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx), c.getInt(hasCustomPhotoIdx) == 1, hikeJoinTime);
		if (favoriteIdx != -1)
		{

			int favoriteTypeOrd = c.getInt(favoriteIdx);
			contactInfo.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
		}
		else
		{
			contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
		}

		if (isOfflineIdx != -1)
		{
			contactInfo.setOffline(c.getInt(isOfflineIdx));
		}

		if (lastSeenTimeIdx != -1)
		{
			contactInfo.setLastSeenTime(c.getLong(lastSeenTimeIdx));
		}
		if (platformIdIndex != -1)
		{
			contactInfo.setPlatformId(c.getString(platformIdIndex));
		}
		if(blockStatusIndex != -1)
		{
			contactInfo.setBlockStatus(c.getString(blockStatusIndex).equals(DBConstants.STATUS_BLOCKED));
		}
		return contactInfo;
	}

	private List<ContactInfo> extractContactInfo(Cursor c)
	{
		return extractContactInfo(c, false);
	}

	private List<ContactInfo> extractContactInfo(Cursor c, boolean distinct)
	{
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(c.getCount());
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		Set<String> msisdnSet = null;
		if (distinct)
		{
			msisdnSet = new HashSet<String>();
		}

		while (c.moveToNext())
		{
			String msisdn = c.getString(msisdnIdx);

			if (TextUtils.isEmpty(msisdn))
			{
				continue;
			}

			if (distinct && msisdnSet.contains(msisdn))
			{
				continue;
			}
			else if (distinct)
			{
				msisdnSet.add(msisdn);
			}
			ContactInfo contactInfo = processContact(c);
			contactInfos.add(contactInfo);
		}
		return contactInfos;
	}

	private Map<String, ContactInfo> extractContactInfoMap(Cursor c)
	{
		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		while (c.moveToNext())
		{
			String msisdn = c.getString(msisdnIdx);
			if (TextUtils.isEmpty(msisdn))
			{
				continue;
			}
			ContactInfo contactInfo = processContact(c);
			contactMap.put(contactInfo.getMsisdn(), contactInfo);
		}
		return contactMap;
	}

	ContactInfo getContactInfoFromMSISDN(String msisdn, boolean ifNotFoundReturnNull)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,
					DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
					DBConstants.HIKE_JOIN_TIME, DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN, DBConstants.PLATFORM_USER_ID ,DBConstants.FAVORITE_TYPE,DBConstants.HIKE_UID,DBConstants.BLOCK_STATUS}, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			contactInfos = extractContactInfo(c);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		if (contactInfos != null && contactInfos.isEmpty())
		{
			Logger.d(getClass().getSimpleName(), "No contact found");
			if (ifNotFoundReturnNull)
			{
				return null;
			}
			else
			{
					String name = null;
					/*
					 * Setting the hike bot name if the msisdn is a hikebot msisdn.
					 */
					if (HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn))
					{
						name = HikeMessengerApp.hikeBotInfoMap.get(msisdn).getConversationName();
					}

					ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, name, msisdn, false);
					contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
					return contactInfo;
				}
			}

		return contactInfos.get(0);
	}

	private LinkedHashMap<String, ContactInfo> getSortedContactMap()
	{

		Cursor c = null;

		LinkedHashMap<String, ContactInfo> contactMap = new LinkedHashMap<String, ContactInfo>();

		try
		{
			c = mReadDb.rawQuery("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.PHONE + ", "
					+ DBConstants.LAST_MESSAGED + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", "
					+ DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP +  ", " + DBConstants.PLATFORM_USER_ID +  ", "+ DBConstants.FAVORITE_TYPE +" , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS +  " from "
					+ DBConstants.USERS_TABLE + " GROUP BY " + DBConstants.MSISDN + " ORDER BY " + DBConstants.NAME + " COLLATE NOCASE ", null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				if (TextUtils.isEmpty(msisdn))
				{
					continue;
				}
				ContactInfo contactInfo = processContact(c);
				if (!contactMap.containsKey(contactInfo.getMsisdn()))
					contactMap.put(contactInfo.getMsisdn(), contactInfo);
			}

			return contactMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, ContactInfo> getContactMap(String msisdns)
	{
		Cursor c = null;

		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();

		try
		{
			c = mReadDb.rawQuery("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.PHONE + ", "
					+ DBConstants.LAST_MESSAGED + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", "
					+ DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP + ", " + DBConstants.PLATFORM_USER_ID + ", "+ DBConstants.FAVORITE_TYPE  + " , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS + " from "
					+ DBConstants.USERS_TABLE + " WHERE " + DBConstants.MSISDN + " IN " + msisdns + " GROUP BY " + DBConstants.MSISDN, null);

			contactMap = extractContactInfoMap(c);

			return contactMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, ContactInfo> getContactMap(int onHike)
	{
		Cursor c = null;

		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();

		try
		{
			c = mReadDb.rawQuery("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.PHONE + ", "
					+ DBConstants.LAST_MESSAGED + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", "
					+ DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP + ", " + DBConstants.PLATFORM_USER_ID+ ", "+ DBConstants.FAVORITE_TYPE + " , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS +  " from "
					+ DBConstants.USERS_TABLE + " WHERE " + DBConstants.ONHIKE + " = " + onHike + " GROUP BY " + DBConstants.MSISDN, null);

			contactMap = extractContactInfoMap(c);

			return contactMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, ContactInfo> getContactMap(String phoneNumbers, int onHike, int limit)
	{
		Cursor c = null;

		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();

		if(TextUtils.isEmpty(phoneNumbers))
		{
			return contactMap;
		}

		try
		{
			c = mReadDb.rawQuery("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.PHONE + ", "
					+ DBConstants.LAST_MESSAGED + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", "
					+ DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP + ", " + DBConstants.PLATFORM_USER_ID + " , " + DBConstants.FAVORITE_TYPE + " , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS +  " from "
					+ DBConstants.USERS_TABLE + " WHERE " + DBConstants.PHONE + " IN " + phoneNumbers + " AND " + DBConstants.ONHIKE + "=" + onHike + " GROUP BY "
					+ DBConstants.MSISDN + " LIMIT " + limit, null);

			contactMap = extractContactInfoMap(c);

			return contactMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, ContactInfo> getContactMap(String msisdns, int onHike, boolean nativeSMSOn, boolean inMsisdns)
	{
		Cursor c = null;

		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();

		try
		{
			StringBuilder queryBuilder = new StringBuilder("SELECT max(" + DBConstants.ID + ") AS " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", "
					+ DBConstants.PHONE + ", " + DBConstants.LAST_MESSAGED + ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO +", " +DBConstants.FAVORITE_TYPE + " , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS +  ", " + DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP + ", " + DBConstants.PLATFORM_USER_ID + " from "
					+ DBConstants.USERS_TABLE + " WHERE ");

			if (onHike != HikeConstants.BOTH_VALUE)
			{
				queryBuilder.append(DBConstants.ONHIKE + " = " + onHike + " AND ");
				if (onHike == HikeConstants.NOT_ON_HIKE_VALUE)
				{
					queryBuilder.append(" (" + DBConstants.MSISDN + " LIKE '+91%') AND ");
				}
			}
			else if (!nativeSMSOn)
			{
				queryBuilder.append(" ((" + DBConstants.ONHIKE + " =1) OR  (" + DBConstants.MSISDN + " LIKE '+91%')) AND ");
			}

			if (inMsisdns)
			{
				queryBuilder.append(DBConstants.MSISDN + " IN " + msisdns + " GROUP BY " + DBConstants.MSISDN);
			}
			else
			{
				queryBuilder.append(DBConstants.MSISDN + " NOT IN " + msisdns + " GROUP BY " + DBConstants.MSISDN);
			}

			c = mReadDb.rawQuery(queryBuilder.toString(), null);

			contactMap = extractContactInfoMap(c);

			return contactMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, FavoriteType> getFavoriteMap()
	{
		Cursor c = null;

		Map<String, FavoriteType> favMap = new HashMap<>();

		try
		{
			String selection = DBConstants.FAVORITE_TYPE + " > ?";
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.FAVORITE_TYPE }, selection, new String[]{String.valueOf(FavoriteType.NOT_FRIEND.ordinal())}, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favoriteTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				FavoriteType favoriteType = FavoriteType.values()[c.getInt(favoriteTypeIdx)];
				favMap.put(msisdn, favoriteType);
			}

			return favMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	public Map<String, FavoriteType> getFavoriteMap(String msisdns)
	{
		Cursor c = null;

		Map<String, FavoriteType> favMap = new HashMap<>();

		try
		{

			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.FAVORITE_TYPE }, DBConstants.MSISDN + " IN " + msisdns+ " AND " + DBConstants.FAVORITE_TYPE + " > 0 ", null, null, null,
					null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favoriteTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				FavoriteType favoriteType = FavoriteType.values()[c.getInt(favoriteTypeIdx)];
				favMap.put(msisdn, favoriteType);
			}

			return favMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, FavoriteType> getFavoriteMap(FavoriteType[] favoriteTypes)
	{
		StringBuilder favTypes = new StringBuilder("(");
		int total = favoriteTypes.length;
		if (total == 0)
		{
			return null;
		}
		for (int i = 0; i < total; i++)
		{
			favTypes.append(favoriteTypes[i].ordinal());
			if (i < total - 1)
			{
				favTypes.append(",");
			}
		}
		String favTypeIn = favTypes.append(")").toString();

		Cursor c = null;
		Map<String, FavoriteType> favMap = new HashMap<String, FavoriteType>();
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.FAVORITE_TYPE }, DBConstants.FAVORITE_TYPE + " IN " + favTypeIn, null,
					null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favoriteTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				FavoriteType favoriteType = FavoriteType.values()[c.getInt(favoriteTypeIdx)];
				favMap.put(msisdn, favoriteType);
			}
			return favMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private Map<String, ContactInfo> getContactInfoMap(List<String> msisdns, Map<String, ContactInfo> contactMap, Map<String, FavoriteType> favoriteMap,
			boolean ignoreUnknownContacts)
	{
		Map<String, ContactInfo> contactInfos = new HashMap<String, ContactInfo>();
		for (String msisdn : msisdns)
		{
			ContactInfo contact = contactMap.get(msisdn);
			if (null != contact)
			{
				contactInfos.put(msisdn, contact);
				continue;
			}
			else
			{
				if (!ignoreUnknownContacts)
				{
					ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, null, msisdn, false);

					if (HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn))
					{
						String name = HikeMessengerApp.hikeBotInfoMap.get(msisdn).getConversationName();
						contactInfo.setName(name);
						contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
					}
					else
					{
						FavoriteType fav = favoriteMap.get(msisdn);
						if (null != fav)
						{
							contactInfo.setFavoriteType(fav);
						}
						else
						{
							contactInfo.setFavoriteType(FavoriteType.NOT_FRIEND);
						}
					}
					contactInfos.put(msisdn, contactInfo);
				}
			}
		}
		return contactInfos;
	}

	public Map<String, ContactInfo> getContactInfoFromMsisdns(List<String> msisdns, boolean favoriteTypeNeeded)
	{
		return getContactInfoFromMsisdns(msisdns, favoriteTypeNeeded, false);
	}

	public Map<String, ContactInfo> getContactInfoFromMsisdns(List<String> msisdns, boolean favoriteTypeNeeded, boolean ignoreUnknownContacts)
	{
		Cursor c = null;

		Map<String, ContactInfo> contactMap = new HashMap<String, ContactInfo>();
		Map<String, FavoriteType> favoriteMap = new HashMap<String, FavoriteType>();

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

		try
		{
			contactMap = getContactMap(msisdnsDB.toString());

			if (favoriteTypeNeeded)
			{
				favoriteMap = getFavoriteMap(msisdnsDB.toString());
			}

			return getContactInfoMap(msisdns, contactMap, favoriteMap, ignoreUnknownContacts);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	Map<String, ContactInfo> getAllContactInfo()
	{
		return getSortedContactMap();
	}

	public List<ContactInfo> getAllContactsForSyncing()
	{
		Cursor c = null;
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		try
		{
			c = mReadDb.rawQuery("SELECT " + DBConstants.ID + ", " + DBConstants.NAME + ", " + DBConstants.MSISDN + ", " + DBConstants.PHONE + ", " + DBConstants.LAST_MESSAGED
					+ ", " + DBConstants.MSISDN_TYPE + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_CUSTOM_PHOTO + ", " + DBConstants.HIKE_JOIN_TIME + ", " + DBConstants.FAVORITE_TYPE + " , " + DBConstants.HIKE_UID + " , "+ DBConstants.BLOCK_STATUS + ","+  DBConstants.LAST_SEEN + ", " + DBConstants.IS_OFFLINE + ", " + DBConstants.INVITE_TIMESTAMP + ", " + DBConstants.PLATFORM_USER_ID + " from " + DBConstants.USERS_TABLE, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				if (TextUtils.isEmpty(msisdn)|| TextUtils.isEmpty(c.getString(c.getColumnIndex(DBConstants.ID))))
				{
					Logger.d(TAG,"Skipping Contact");
					continue;
				}
				ContactInfo contact = processContact(c);
				contacts.add(contact);
			}
			return contacts;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	List<ContactInfo> getHikeContacts(int limit, String msisdnsIn, String msisdnsNotIn, String myMsisdn)
	{
		Cursor c = null;
		List<ContactInfo> contactInfos = null;
		try
		{
			StringBuilder selectionBuilder = new StringBuilder();
			if (!TextUtils.isEmpty(msisdnsIn))
			{
				selectionBuilder.append(DBConstants.MSISDN + " IN " + msisdnsIn + " AND ");
			}
			if (!TextUtils.isEmpty(msisdnsNotIn))
			{
				selectionBuilder.append(DBConstants.MSISDN + " NOT IN " + msisdnsNotIn + " AND ");
			}
			if (limit > 0)
			{
				c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME,
						DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
						DBConstants.FAVORITE_TYPE,DBConstants.HIKE_UID,DBConstants.BLOCK_STATUS, DBConstants.HIKE_JOIN_TIME, DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selectionBuilder.toString()
						+ DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn) + " AND " + DBConstants.ONHIKE + "=1", null, DBConstants.MSISDN, null, null,
						Integer.toString(limit));
			}
			else
			{
				c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME,
						DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
						DBConstants.FAVORITE_TYPE,DBConstants.FAVORITE_TYPE,DBConstants.BLOCK_STATUS, DBConstants.HIKE_JOIN_TIME, DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selectionBuilder.toString()
						+ DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn) + " AND " + DBConstants.ONHIKE + "=1", null, DBConstants.MSISDN, null, null);
			}
			contactInfos = extractContactInfo(c, true);
			return contactInfos;
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
	 * Adds a single contact to the hike user db
	 * 
	 * @param hikeContactInfo
	 *            contact to add
	 * @return true if the insert was successful
	 */
	void addContact(ContactInfo hikeContactInfo) throws DbException
	{
		List<ContactInfo> l = new LinkedList<ContactInfo>();
		l.add(hikeContactInfo);
		addContacts(l, false);
	}

	List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts(Set<String> blockMsisdns)
	{
		List<Pair<AtomicBoolean, ContactInfo>> contactInfos = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

		Map<String, ContactInfo> contactMap = getContactMap(HikeConstants.NOT_ON_HIKE_VALUE);

		for (String msisdn : blockMsisdns)
		{
			contactMap.remove(msisdn);
		}

		for (Entry<String, ContactInfo> mapEntry : contactMap.entrySet())
		{
			String msisdn = mapEntry.getKey();
			if (ContactManager.getInstance().isIndianMobileNumber(msisdn))
			{
				ContactInfo contactInfo = mapEntry.getValue();
				contactInfos.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contactInfo));
			}
		}

		return contactInfos;
	}


	Map<String, ContactInfo> getNOTFRIENDScontactsFromDB(Set<String> blockSet,int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		Map<String, FavoriteType> favoriteMap = getFavoriteMap();

		List<String> msisdns = new ArrayList<String>(favoriteMap.keySet());

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

		Map<String, ContactInfo> contactMap = getContactMap(msisdnsDB.toString(), onHike, nativeSMSOn, false);

		Map<String, ContactInfo> contactInfomap = getContactInfoMap(msisdns, contactMap, favoriteMap, ignoreUnknownContacts);

		// remove self msisdn from favoriteMap
		contactInfomap.remove(myMsisdn);

		// remove block msisdns from favoriteMap
		for (String msisdn : blockSet)
		{
			contactInfomap.remove(msisdn);
		}

		return contactInfomap;
	}

	Map<String, ContactInfo> getContactsOfFavoriteTypeDB(FavoriteType[] favoriteType, Set<String> blockSet, int onHike, String myMsisdn, boolean nativeSMSOn,
			boolean ignoreUnknownContacts)
	{
		Map<String, FavoriteType> favoriteMap = getFavoriteMap(favoriteType);
		if (null == favoriteMap)
		{
			return null;
		}

		// remove block msisdns from favoriteMap
		for (String msisdn : blockSet)
		{
			favoriteMap.remove(msisdn);
		}

		// remove self msisdn from favoriteMap
		favoriteMap.remove(myMsisdn);

		List<String> msisdns = new ArrayList<String>(favoriteMap.keySet());

		if (msisdns.size() > 0)
		{
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

			Map<String, ContactInfo> contactMap = getContactMap(msisdnsDB.toString(), onHike, nativeSMSOn, true);

			return getContactInfoMap(msisdns, contactMap, favoriteMap, ignoreUnknownContacts);
		}
		return null;
	}



	void deleteMultipleRows(Map<String, List<ContactInfo>> hike_Contacts)
	{
		Set<String> ids = hike_Contacts.keySet();
		Set<String> conversationExists = new HashSet<>(ids.size());

		for (Entry<String, List<ContactInfo>> e : hike_Contacts.entrySet()) {
			List<ContactInfo> list = e.getValue();
			for (ContactInfo ci : list) {
				if (ContactManager.getInstance().isConvExists(ci.getMsisdn())) {
					conversationExists.add(e.getKey());
					break;
				}
			}
		}

		ids.removeAll(conversationExists);

		String ids_joined = "(" + Utils.join(ids, ",", "\"", "\"") + ")";
		String selection= DBConstants.ID + " in " + ids_joined+ " AND ( "+DBConstants.FAVORITE_TYPE + " > 0 OR " + DBConstants.BLOCK_STATUS + " = "+ DBConstants.STATUS_BLOCKED + " ) ";
		Logger.d(TAG,"The Selection query is "+selection);

		ContentValues cv = new ContentValues();
		cv.putNull(DBConstants.NAME);
		cv.putNull(DBConstants.ID);
		cv.putNull(DBConstants.PHONE);


		long rows=mDb.update(DBConstants.USERS_TABLE,cv,selection,null);
		String idsJoinedConversationEx = "(" + Utils.join(conversationExists, ",", "\"", "\"") + ")";
		rows = mDb.update(DBConstants.USERS_TABLE,cv,DBConstants.ID + " in "+ idsJoinedConversationEx,null);
		Logger.d(TAG,"Update in COnversation Exists + "+rows);

		Logger.d(TAG,"Update in deleteMultipleRows + "+rows);

		selection=DBConstants.ID + " in " + ids_joined+ " AND "+DBConstants.FAVORITE_TYPE + " = 0 AND " + DBConstants.BLOCK_STATUS + " = "+ DBConstants.STATUS_UNBLOCKED;
		Logger.d(TAG,"The Selection query is "+selection);



		rows=mDb.delete(DBConstants.USERS_TABLE,selection,null);

		Logger.d(TAG,"Delete in deleteMultipleRows + "+rows);
	}

	Map<String,ContactInfo> deleteMultipleRows(Collection<String> ids, Collection<String> pNums) {
		String ids_joined = "(" + Utils.join(ids, ",", "\"", "\"") + ")";
		String phoneNumbers = "(" + Utils.join(pNums, ",", "\"", "\"") + ")";
		String selection = DBConstants.ID + " in " + ids_joined + " AND " + DBConstants.PHONE + " in " + phoneNumbers;
		Cursor c = null;
		Map<String, ContactInfo> map = null;
		try {
			c = mDb.query(DBConstants.USERS_TABLE, null, selection, null, null, null, null);
			String key = null;
			map = new HashMap<>(c.getCount());
			if (c != null && c.moveToFirst()) {
				ContactInfo ci = processContact(c);
				key = ci.getId() + "_" + ci.getMsisdn() + "_" + ci.getPhoneNum();
				map.put(key, ci);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		long rows = mDb.delete(DBConstants.USERS_TABLE, selection, null);
		Logger.d(TAG, "Number of rows deleted in deleteMultipleRows" + rows);
		return map;
	}

	void updateContacts(List<ContactInfo> updatedContacts)
	{
		if (updatedContacts == null)
		{
			return;
		}

		ArrayList<String> ids = new ArrayList<String>(updatedContacts.size());
		ArrayList<String> pNums = new ArrayList<String>(updatedContacts.size());
		for (ContactInfo c : updatedContacts)
		{
			ids.add(c.getId());
			pNums.add(c.getPhoneNum());
		}
		final Map<String, ContactInfo> deletedContactInfoMap = deleteMultipleRows(ids, pNums);
		try
		{
			if (deletedContactInfoMap != null && deletedContactInfoMap.size() > 0) {

				for(ContactInfo ci:updatedContacts)
				{
					String key = ci.getId() + "_" + ci.getMsisdn() + "_" + ci.getPhoneNum();
					if (deletedContactInfoMap.get(key) != null) {

						Logger.d(TAG, "Going to set FAV TYPE TO CONTACT" + ci.toString() + "and block status is " + ci.getBlockedStatus());
						ci.setFavoriteType(deletedContactInfoMap.get(key).getFavoriteType());
						ci.setBlockStatus(deletedContactInfoMap.get(key).getBlockedStatus());
						ci.setHikeJoinTime(deletedContactInfoMap.get(key).getHikeJoinTime());
						ci.setHasCustomPhoto(deletedContactInfoMap.get(key).hasCustomPhoto());
						ci.setLastSeenTime(deletedContactInfoMap.get(key).getLastSeenTime());
					}
				}

			}
			addContacts(updatedContacts, false);
		}
		catch (DbException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	int updateHikeContact(String msisdn, boolean onhike)
	{
		Cursor c = null;
		try
		{
			String selection = DBConstants.MSISDN + "=?";
			String[] args = { msisdn };

			c = mDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			boolean onHikeDB = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) == 1;

			/*
			 * DB is already updated with this value.
			 */
			if (onHikeDB == onhike)
			{
				return 0;
			}

			ContentValues vals = new ContentValues(1);
			vals.put(DBConstants.ONHIKE, onhike);
			return mDb.update(DBConstants.USERS_TABLE, vals, "msisdn=?", new String[] { msisdn });
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	void deleteAll()
	{
		mDb.delete(DBConstants.USERS_TABLE, null, null);
		mDb.delete(DBConstants.THUMBNAILS_TABLE, null, null);
		mDb.delete(DBConstants.HIKE_USER.HIKE_CALLER_TABLE, null, null);
	}

	ContactInfo getContactInfoFromPhoneNo(String number)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,
					DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.PLATFORM_USER_ID }, DBConstants.PHONE + "=?", new String[] { number }, null,
					null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			if (contactInfos.isEmpty())
			{
				return null;
			}

			return contactInfos.get(0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	FavoriteType getFriendshipStatus(String number)
	{
		Cursor favoriteCursor = null;
		try
		{
			favoriteCursor = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.FAVORITE_TYPE }, DBConstants.MSISDN + " =? ", new String[] { number }, null,
					null, null);

			FavoriteType favoriteType = FavoriteType.NOT_FRIEND;
			if (favoriteCursor.moveToFirst())
			{
				favoriteType = FavoriteType.values()[favoriteCursor.getInt(favoriteCursor.getColumnIndex(DBConstants.FAVORITE_TYPE))];
			}
			return favoriteType;
		}
		finally
		{
			if (favoriteCursor != null)
			{
				favoriteCursor.close();
			}
		}
	}

	ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,
					DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.PLATFORM_USER_ID }, "(" + DBConstants.PHONE + "=? OR " + DBConstants.MSISDN
					+ "=?) AND " + DBConstants.MSISDN + "!='null'", new String[] { number, number }, null, null, null);
			List<ContactInfo> contactInfos = extractContactInfo(c);
			if (contactInfos.isEmpty())
			{
				return null;
			}

			return contactInfos.get(0);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

    List<ContactInfo> getContactInfoListFromPhoneNoOrMsisdn(String msisdnsIn)
    {
        Cursor c = null;
        List<ContactInfo> contactInfos = null;

        if(TextUtils.isEmpty(msisdnsIn))
            return contactInfos;

        try
        {
            StringBuilder selectionBuilder = new StringBuilder();

            selectionBuilder.append(DBConstants.MSISDN + " IN ('" + msisdnsIn + "') AND ");

            c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME,
                    DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
                    DBConstants.FAVORITE_TYPE_SELECTION, DBConstants.HIKE_JOIN_TIME, DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN }, selectionBuilder.toString()
                    + DBConstants.MSISDN + "!='null'" , null, null, null, null);

            contactInfos = extractContactInfo(c, true);

            return contactInfos;
        }
        finally
        {
            if (c != null)
            {
                c.close();
            }
        }

    }




	public void unblock(String msisdn)
	{
		ContentValues cv=new ContentValues();
		cv.put(DBConstants.BLOCK_STATUS,DBConstants.STATUS_UNBLOCKED);
		long value=mDb.update(DBConstants.USERS_TABLE,cv,DBConstants.MSISDN + "=?", new String[] { msisdn});
		Logger.d(TAG,"Unblocked MSISDN= "+msisdn+"And the Value in DB is "+value);
		//mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	void block(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
		{
			return;
		}
		List<PairModified<String,String>> blockedContact=new ArrayList(1);
		blockedContact.add(new PairModified<String, String>(msisdn,null));
		addBlockList(blockedContact);
	}

	void unblockUID(String hikeUID)
	{
		if(TextUtils.isEmpty(hikeUID))
		{
			return;
		}
		ContentValues cv=new ContentValues();
		cv.put(DBConstants.BLOCK_STATUS,DBConstants.STATUS_UNBLOCKED);
		long value=mDb.update(DBConstants.USERS_TABLE,cv,DBConstants.HIKE_UID + "=?", new String[] { hikeUID});
		Logger.d(TAG,"Unblocked UID= "+hikeUID+"And the Value in DB is "+value);
		//mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?", new String[] { hikeUID });
	}

	public boolean isMsisdnBlocked(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
		{
			return false;
		}
		Cursor c = null;
		String isBlocked = DBConstants.STATUS_UNBLOCKED;
		try
		{
			c = mDb.query(DBConstants.USERS_TABLE, new String[]{DBConstants.BLOCK_STATUS}, DBConstants.MSISDN + "=?", new String[]{msisdn}, null, null, null);

			if (c.moveToFirst()) {
				isBlocked = c.getString(c.getColumnIndex(DBConstants.BLOCK_STATUS));
			}

			return isBlocked.equals(DBConstants.STATUS_BLOCKED);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	boolean isHUIDBlocked(String hikeUID)
	{
		if (TextUtils.isEmpty(hikeUID)) {
			return false;
		}
		Cursor c = null;
		String isBlocked = DBConstants.STATUS_UNBLOCKED;
		try {
			c = mDb.query(DBConstants.USERS_TABLE, new String[]{DBConstants.BLOCK_STATUS}, DBConstants.HIKE_UID + "=?", new String[]{hikeUID}, null, null, null);

			if (c.moveToFirst()) {
				isBlocked = c.getString(c.getColumnIndex(DBConstants.BLOCK_STATUS));
			}

			return isBlocked.equals(DBConstants.STATUS_BLOCKED);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}


	void setIcon(String msisdn, byte[] data, boolean isProfileImage)
	{
		if (!isProfileImage)
		{
			/*
			 * We delete the older file that contained the larger avatar image for this msisdn.
			 */
			Utils.removeLargerProfileImageForMsisdn(msisdn);

		}
		// IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		HikeMessengerApp.getLruCache().remove(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] { msisdn });
		
		File cacheDir = mContext.getCacheDir();
		if(cacheDir != null && cacheDir.exists())
		{
			writeThumbnailToFile(new File(cacheDir, msisdn + ".jpg"), data);
		}
	}
	
	void setThumbnail(String msisdn, byte[] data)
	{
		//TODO Consider merging with setIcon(String msisdn, byte[] data, boolean isProfileImage)
		HikeMessengerApp.getLruCache().remove(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);
		
		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] { msisdn });
	}

	Drawable getIcon(String msisdn)
	{
		// Fetch Bot DP from file
		if (BotUtils.isBot(msisdn))
		{
			Bitmap botDp = BotUtils.getBotDp(msisdn);
			
			if (botDp != null)
			{
				return HikeBitmapFactory.getBitmapDrawable(botDp);
			}
		}
		
		byte[] icondata = getIconByteArray(msisdn);
		
		if(icondata != null)
		{
			return HikeBitmapFactory.getBitmapDrawable(mContext.getResources(), HikeBitmapFactory.decodeByteArray(icondata, 0, icondata.length));
		}
		
		else //Discovery Bots 
		{
			Bitmap botDp = BotUtils.getBotDp(msisdn);
			
			if (botDp != null)
			{
				return HikeBitmapFactory.getBitmapDrawable(botDp);
			}
		}
		
		return null;
	}

	byte[] getIconByteArray(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return null;
			}
			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return icondata;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	String getIconIdentifierString(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.IMAGE }, "msisdn=?", new String[] { msisdn }, null, null, null);

			if (!c.moveToFirst())
			{
				/* lookup based on this msisdn */
				return null;
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			String iconString = Base64.encodeToString(icondata, Base64.DEFAULT);

			if (iconString.length() < 6)
			{
				return iconString;
			}
			else
			{
				return iconString.substring(0, 5) + iconString.substring(iconString.length() - 6);
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

	public boolean removeIcon(String msisdn)
	{
		/*
		 * We delete the older file that contained the larger avatar image for this msisdn.
		 */
		Utils.removeLargerProfileImageForMsisdn(msisdn);

		int deletedRows = mDb.delete(DBConstants.THUMBNAILS_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 0);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause, new String[] { msisdn });
		if (deletedRows > 0)
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	public void updateContactRecency(String msisdn, long timeStamp)
	{
		ContentValues updatedTime = new ContentValues(1);
		updatedTime.put(DBConstants.LAST_MESSAGED, timeStamp);

		String whereClause = DBConstants.MSISDN + "=?";
		int rows = mDb.update(DBConstants.USERS_TABLE, updatedTime, whereClause, new String[] { msisdn });
		Logger.d(getClass().getSimpleName(), "Row has been updated: " + rows);
	}

	public void syncContactExtraInfo()
	{
		Cursor extraInfo = null;

		try
		{
			extraInfo = this.mContext.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null, null, null);

			int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
			int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
			int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

			mDb.beginTransaction();

			while (extraInfo.moveToNext())
			{
				String msisdnType = Phone.getTypeLabel(this.mContext.getResources(), extraInfo.getInt(typeIdx), extraInfo.getString(labelIdx)).toString();
				ContentValues contentValues = new ContentValues(1);
				contentValues.put(DBConstants.MSISDN_TYPE, msisdnType);
				String whereClause = DBConstants.PHONE + " =? ";
				mDb.update(DBConstants.USERS_TABLE, contentValues, whereClause, new String[] { extraInfo.getString(msisdnIdx) });
			}
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
			editor.commit();
			mDb.setTransactionSuccessful();
		}
		catch (SecurityException e)
		{
			Logger.e("ContactUtils", "Exception while syncContactExtraInfo", e);
		}
		finally
		{
			if (extraInfo != null)
			{
				extraInfo.close();
			}
			mDb.endTransaction();
		}
	}

	void toggleContactFavorite(String msisdn, FavoriteType favoriteType) {
		/*
		 * If we are setting the type as not favorite, we'll remove the row itself.
		 */
		ContentValues cv = new ContentValues();
		Logger.d(TAG, "Adding msisdb to favourite" + msisdn);
		cv.put(DBConstants.MSISDN, msisdn);
		cv.put(DBConstants.FAVORITE_TYPE, favoriteType.ordinal());
		long value = mDb.update(DBConstants.USERS_TABLE, cv, DBConstants.MSISDN + "=?", new String[]{msisdn});
		if (value == -1 || value == 0) {
			value = mDb.insertWithOnConflict(DBConstants.USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
			Logger.d(TAG, "MSISDN FAVOUTITE" + msisdn + "result -->" + value + "INSERT EXECUTED");
		} else {
			Logger.d(TAG, "MSISDN FAVOURITE" + msisdn + "result -->" + value + "UPDATE EXECUTED");
		}
	}

	public List<ContactInfo> getNonHikeMostContactedContactsFromListOfNumbers(String selectionNumbers, final Map<String, Integer> mostContactedValues, int limit)
	{
		Map<String, FavoriteType> favoriteMap = getFavoriteMap();

		Map<String, ContactInfo> contactMap = getContactMap(selectionNumbers, HikeConstants.NOT_ON_HIKE_VALUE, limit);

		List<String> msisdns = new ArrayList<String>(contactMap.keySet());

		Map<String, ContactInfo> contactInfoMap = getContactInfoMap(msisdns, contactMap, favoriteMap, true);

		List<ContactInfo> contactList = new ArrayList<ContactInfo>(contactInfoMap.values());

		Collections.sort(contactList, new Comparator<ContactInfo>()
		{
			@Override
			public int compare(ContactInfo lhs, ContactInfo rhs)
			{
				int lhsContactNum = mostContactedValues.get(lhs.getPhoneNum());
				int rhsContactNum = mostContactedValues.get(rhs.getPhoneNum());

				if (lhsContactNum != rhsContactNum)
				{
					return -((Integer) lhsContactNum).compareTo(rhsContactNum);
				}
				return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
			}
		});

		return contactList;
	}

	void setMultipleContactsToFavorites(JSONObject favorites) {

		JSONArray msisdns = favorites.names();
		if (msisdns == null) {
			return;
		}
		mDb.beginTransaction();
		try {
			for (int i = 0; i < msisdns.length(); i++) {
				String msisdn = msisdns.optString(i);
				JSONObject msisdnInfo = favorites.optJSONObject(msisdn);

				FavoriteType favoriteType;
				if (msisdnInfo.has(HikeConstants.PENDING)) {
					boolean pending = msisdnInfo.optBoolean(HikeConstants.PENDING);
					favoriteType = pending ? FavoriteType.REQUEST_RECEIVED : FavoriteType.REQUEST_RECEIVED_REJECTED;
				} else if (msisdnInfo.has(HikeConstants.REQUEST_PENDING)) {
					boolean requestPending = msisdnInfo.optBoolean(HikeConstants.REQUEST_PENDING);
					favoriteType = requestPending ? FavoriteType.REQUEST_SENT : FavoriteType.REQUEST_SENT_REJECTED;
				} else {
					favoriteType = FavoriteType.FRIEND;
				}

				ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn);
				if (null != contactInfo) {
					ContactInfo updatedContact = new ContactInfo(contactInfo);
					updatedContact.setFavoriteType(favoriteType);
					ContactManager.getInstance().updateContacts(updatedContact);
				}
				ContentValues cv=new ContentValues();
				cv.put(DBConstants.MSISDN,msisdn);
				cv.put(DBConstants.FAVORITE_TYPE,favoriteType.ordinal());
				long value = mDb.update(DBConstants.USERS_TABLE, cv, DBConstants.MSISDN + "=?", new String[]{msisdn});
				if (value == -1 || value == 0) {

					value = mDb.insertWithOnConflict(DBConstants.USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
					Logger.d(TAG, "FAVOURITE TOGGLED" + msisdn + "result -->" + value + "INSERT EXECUTED");
				} else {
					Logger.d(TAG, "FAVOURITE TOGGLED" + msisdn + "result -->" + value + "UPDATE EXECUTED");
				}

			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();

			if (favorites.length() > 0) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.REFRESH_FAVORITES, null);
			}
		}

	}


	boolean hasIcon(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

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

	boolean doesContactExist(String msisdn)
	{
		String[] columns = new String[] { DBConstants.NAME };
		String selection = DBConstants.MSISDN + "=? ";
		Cursor c = null;

		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection, new String[] { msisdn }, null, null, null);
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

	int getHikeContactCount(String myMsisdn)
	{
		String selection = DBConstants.ONHIKE + " = 1 AND " + DBConstants.MSISDN + "!=" + DatabaseUtils.sqlEscapeString(myMsisdn);
		Cursor c = null;
		try
		{
			c = mReadDb.query(true, DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN }, selection, null, null, null, null, null);

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

	int getNonHikeContactsCount()
	{
		String selection = DBConstants.ONHIKE + " = 0";
		Cursor c = null;
		try
		{
			c = mReadDb.query(true, DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN }, selection, null, null, null, null, null);

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

	void setHikeJoinTime(String msisdn, long hikeJoinTime)
	{
		String whereClause = DBConstants.MSISDN + "=?";
		String[] whereArgs = new String[] { msisdn };

		ContentValues values = new ContentValues(1);
		values.put(DBConstants.HIKE_JOIN_TIME, hikeJoinTime);

		mDb.update(DBConstants.USERS_TABLE, values, whereClause, whereArgs);
	}

	void updateLastSeenTime(String msisdn, long lastSeenTime)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.LAST_SEEN, lastSeenTime);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	void updateIsOffline(String msisdn, int offline)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_OFFLINE, offline);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	private Set<String> getQueryableNumbersString(List<ContactInfo> contactInfos)
	{
		Set<String> msisdns = new HashSet<String>();
		if (contactInfos.isEmpty())
		{
			return null;
		}

		for (ContactInfo contactInfo : contactInfos)
		{
			msisdns.add(contactInfo.getMsisdn());
		}

		return msisdns;
	}

	/*
	 * This is done because we set msisdntype as ftuecontact and if they are in contact manager then it's get changed for other places as well and we dont want that so creating a
	 * duplicate copy for ftue contacts
	 */
	private List<ContactInfo> getDuplicateContactsForFtue(List<ContactInfo> contacts)
	{
		List<ContactInfo> duplicateContacts = new ArrayList<ContactInfo>();
		for (ContactInfo contact : contacts)
		{
			ContactInfo duplicate = new ContactInfo(contact);
			duplicateContacts.add(duplicate);
		}
		return duplicateContacts;
	}

	FtueContactsData getFTUEContacts(SharedPreferences preferences)
	{
		FtueContactsData ftueContactsData = new FtueContactsData();

		int limit = HikeConstants.FTUE_LIMIT;

		String myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

		ftueContactsData.setTotalHikeContactsCount(getHikeContactCount(myMsisdn));

		/*
		 * adding server recommended contacts to ftue contacts list;
		 */
		Set<String> recommendedContactsSelection = Utils.getServerRecommendedContactsSelection(preferences.getString(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), myMsisdn);
		Logger.d("getFTUEContacts", "recommendedContactsSelection = " + recommendedContactsSelection);
		if (null != recommendedContactsSelection && !recommendedContactsSelection.isEmpty())
		{
			List<ContactInfo> recommendedContacts = getDuplicateContactsForFtue(ContactManager.getInstance().getHikeContacts(limit * 2, recommendedContactsSelection, null,
					myMsisdn));
			if (recommendedContacts.size() >= limit)
			{
				ftueContactsData.getHikeContacts().addAll(recommendedContacts.subList(0, limit));
				return ftueContactsData;
			}
			else
			{
				ftueContactsData.getHikeContacts().addAll(recommendedContacts);
			}
		}

		limit = HikeConstants.FTUE_LIMIT - ftueContactsData.getHikeContacts().size();
		// added server recommended contacts

		/*
		 * adding favorites if required;
		 */
		if (limit > 0)
		{
			List<ContactInfo> friendList = getDuplicateContactsForFtue(ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.FRIEND,
					HikeConstants.ON_HIKE_VALUE, myMsisdn, false, true));
			for (ContactInfo contactInfo : friendList)
			{
				if (!Utils.isListContainsMsisdn(ftueContactsData.getHikeContacts(), contactInfo.getMsisdn()))
				{
					ftueContactsData.getHikeContacts().add(contactInfo);
					limit--;

					if (limit < 1)
					{
						return ftueContactsData;
					}
				}
			}

		}
		else
		{
			return ftueContactsData;
		}

		// added favorites contacts

		/*
		 * adding random hike contacts if required;
		 */
		if (limit > 0)
		{
			Set<String> currentSelection = getQueryableNumbersString(ftueContactsData.getHikeContacts());
			List<ContactInfo> hikeContacts = getDuplicateContactsForFtue(ContactManager.getInstance().getHikeContacts(limit * 2, null, currentSelection, myMsisdn));
			if (hikeContacts.size() >= limit)
			{
				ftueContactsData.getHikeContacts().addAll(hikeContacts.subList(0, limit));
				return ftueContactsData;
			}
			else
			{
				ftueContactsData.getHikeContacts().addAll(hikeContacts);
			}

		}
		else
		{
			return ftueContactsData;
		}
		limit = HikeConstants.FTUE_LIMIT - ftueContactsData.getHikeContacts().size();

		// added random hike contacts

		/*
		 * adding most contacted sms contacts if required;
		 */
		if (limit > 0)
		{
			List<ContactInfo> nonHikeContacts = getDuplicateContactsForFtue(ContactManager.getInstance().getNonHikeMostContactedContacts(limit * 4));
			ftueContactsData.setTotalSmsContactsCount(getNonHikeContactsCount());

			if (nonHikeContacts.size() >= limit)
			{
				ftueContactsData.getSmsContacts().addAll(nonHikeContacts.subList(0, limit));
			}
			else
			{
				ftueContactsData.getSmsContacts().addAll(nonHikeContacts);
			}
		}

		return ftueContactsData;
	}

	void updateInvitedTimestamp(String msisdn, long timestamp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.INVITE_TIMESTAMP, timestamp);

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public ContactInfo getMostRecentContact(int hikeState)
	{
		String selection;
		switch (hikeState)
		{
		case HikeConstants.ON_HIKE_VALUE:
			selection = DBConstants.ONHIKE + "=1";
			break;
		case HikeConstants.NOT_ON_HIKE_VALUE:
			selection = DBConstants.ONHIKE + "=0 AND " + DBConstants.MSISDN + " LIKE '+91%'";
			break;
		default:
			selection = null;
			break;
		}
		selection += (selection == null ? "" : " AND ") + DBConstants.BLOCK_STATUS + " = " + DBConstants.STATUS_UNBLOCKED;

		Cursor c = null;
		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN, "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.NAME, DBConstants.ONHIKE,
					DBConstants.PHONE, DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE_TYPE_SELECTION,
					DBConstants.HIKE_JOIN_TIME, DBConstants.IS_OFFLINE, DBConstants.LAST_SEEN, DBConstants.PLATFORM_USER_ID }, selection, null, null, null, DBConstants.LAST_MESSAGED + " DESC LIMIT 1");
			if (c.getCount() != 0)
			{
				ContactInfo ci = extractContactInfo(c).get(0);
				return ci;
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


	
	public Set<String> getBlockedMsisdnSet()
	{
		Cursor c = null;
		Set<String> blockedSet = new HashSet<String>();

		try
		{
			c = mReadDb.query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.BLOCK_STATUS + "=?", new String[] { DBConstants.STATUS_BLOCKED}, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				blockedSet.add(c.getString(msisdnIdx));
			}
			Logger.d(TAG, "The Blocked Msisdns are " + blockedSet);
			return blockedSet;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	void platformUserIdDbEntry(JSONArray data)
	{
		try
		{
			int length = data.length();
			mDb.beginTransaction();
			for (int i=0; i<length; i++)
			{
				JSONObject obj = data.getJSONObject(i);
				String msisdn = obj.optString(HikeConstants.MSISDN);

				String platformUID = "";
				if(!obj.isNull(HikePlatformConstants.PLATFORM_USER_ID))
				{
					platformUID = obj.optString(HikePlatformConstants.PLATFORM_USER_ID);
				}
				if (TextUtils.isEmpty(msisdn) || TextUtils.isEmpty(platformUID))
				{
					continue;
				}
				ContentValues contentValues = new ContentValues(1);
				contentValues.put(DBConstants.PLATFORM_USER_ID, platformUID);
				mDb.update(DBConstants.USERS_TABLE, contentValues, "msisdn=?", new String[] { msisdn });
			}
			mDb.setTransactionSuccessful();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}


	}

	ArrayList<String> getMsisdnsForMissingPlatformUID()
	{
		Cursor c = null;
		ArrayList<String> msisdns = new ArrayList<String>();
		try
		{

			c = mReadDb.rawQuery("select " + DBConstants.MSISDN + " from "+DBConstants.USERS_TABLE +" where "+ DBConstants.ONHIKE + "=1 AND "  + DBConstants.PLATFORM_USER_ID + "=''" ,null);


			while (c.moveToNext())
			{
				msisdns.add(c.getString(c.getColumnIndex(DBConstants.MSISDN)));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return msisdns;
	}
	
	/**
	 * Fetches the byte array of the image from the Thumbnails table and writes to the file specified.
	 * @param imageFile
	 * @param msisdn
	 */
	public void writeThumbnailToFileForMsisdn(File imageFile, String msisdn)
	{
		Cursor c = null;

		try
		{
			c = mReadDb.rawQuery("select " + DBConstants.IMAGE + " from " + DBConstants.THUMBNAILS_TABLE + " where " + DBConstants.MSISDN + "=?", new String[] { msisdn });
			if (c.moveToFirst())
			{
				Utils.saveByteArrayToFile(imageFile, getIconByteArray(msisdn));
			}
			else
			{
				Logger.e("HikeUserDatabase", "The " + msisdn + " does not exist in the thumbnails table");
			}
		}
		catch (IOException e)
		{
			Logger.e("HikeUserDatabase", "IOException in getImagePathForThumbnail : ", e);
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
	 * Writes the byte array of the thumbnail to the file specified.
	 * @param imageFile
	 * @param data
	 */
	public void writeThumbnailToFile(File imageFile, byte[] data)
	{
		try
		{
			Utils.saveByteArrayToFile(imageFile, data);
		}
		catch (IOException e)
		{
			Logger.e("HikeUserDatabase", "writeThumbnailToFile IOException : " + e);
		}
	}
	
	/**
	 * Will return the thumbnail path of the user DP. eg - file:///data/data/com.bsb.hike/cache/+91112233456.jpg
	 *
	 * @param msisdn
	 * @return
	 */
	public String getImagePathForThumbnail(String msisdn)
	{
		File cacheDir = mContext.getCacheDir();
		if (cacheDir == null || !cacheDir.exists())
		{
			Logger.e("HikeUserDatabase", "writeThumbnailToFile : cache directory is null/doesn't exist.");
			return null;
		}

		File imageFile = new File(cacheDir, msisdn + ".jpg");

		if (!imageFile.exists())
		{
			writeThumbnailToFileForMsisdn(imageFile, msisdn);
		}

		return HikePlatformConstants.FILE_DESCRIPTOR + imageFile.getAbsolutePath();
	}

	public List<String> getBlockedMsisdnFromBlockTable() {
		Cursor c = null;
		List<String> blockedSet = new ArrayList<>();

		try {
			c = mReadDb.query(DBConstants.BLOCK_TABLE, new String[]{DBConstants.MSISDN}, null, null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext()) {
				blockedSet.add(c.getString(msisdnIdx));
			}

			return blockedSet;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}


	public void dropBlockTable() {
		String dropTable=DBConstants.DROP_TABLE + DBConstants.BLOCK_TABLE;
		mDb.execSQL(dropTable);
	}


	public Map<String, FavoriteType> getFavoriteMsisdnFromFavTable()
	{
		Cursor c = null;

		Map<String, FavoriteType> favMap = new HashMap<>();

		try
		{
			c = mReadDb.query(DBConstants.FAVORITES_TABLE, new String[] { DBConstants.MSISDN, DBConstants.FAVORITE_TYPE }, null, null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int favoriteTypeIdx = c.getColumnIndex(DBConstants.FAVORITE_TYPE);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				FavoriteType favoriteType = FavoriteType.values()[c.getInt(favoriteTypeIdx)];
				favMap.put(msisdn, favoriteType);
			}

			return favMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	public void insertFavIntoUserTable(Map<String, FavoriteType> favoriteMsisdnFromFavTable) {

		if (favoriteMsisdnFromFavTable != null && favoriteMsisdnFromFavTable.isEmpty()) {
			return;
		}

		for (Entry<String, FavoriteType> entry : favoriteMsisdnFromFavTable.entrySet()) {
			ContactManager.getInstance().toggleContactFavorite(entry.getKey(), entry.getValue());
		}
	}

	public void dropFavTable() {
		String dropTable=DBConstants.DROP_TABLE + DBConstants.FAVORITES_TABLE;
		mDb.execSQL(dropTable);
	}

	/**
	 * Function to get the hike Id of missing Msisdn from DB
	 * @return
     */
	public Set<String> getMsisdnsForMissingHikeUID() {
		Cursor c = null;
		Set<String> msisdns = new HashSet<>();
		try {

			c = mReadDb.rawQuery("select " + DBConstants.MSISDN + " from " + DBConstants.USERS_TABLE + " where (" + DBConstants.ONHIKE + "=1 OR " + DBConstants.FAVORITE_TYPE + " >0 OR "+ DBConstants.BLOCK_STATUS + " >0 )"+" AND " + DBConstants.HIKE_UID + " is null", null);


			while (c.moveToNext()) {
				msisdns.add(c.getString(c.getColumnIndex(DBConstants.MSISDN)));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return msisdns;
	}

    public void updateContactUid(Set<FetchUIDTaskPojo> data) {
		if (Utils.isEmpty(data)) {
			return;
		}
		mDb.beginTransaction();
		try {
			ContentValues cv = new ContentValues();
			for (FetchUIDTaskPojo d : data) {
				if (TextUtils.isEmpty(d.getUid())) {    // onHike is false
					cv.put(DBConstants.ONHIKE, 0);
				} else {
					cv.put(DBConstants.HIKE_UID, d.getUid());
					cv.put(DBConstants.ONHIKE, 1);
				}
				long rows = mDb.update(DBConstants.USERS_TABLE, cv, DBConstants.MSISDN + "=?", new String[]{d.getMsisdn()});
				Logger.d(TAG, "Update Exceuted " + rows);
				cv.clear();
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {

		switch (type) {
			case HikePubSub.NEW_CONVERSATION:
				updateTableWhenNewConversationCreated((ConvInfo) object);
				break;
			case HikePubSub.CONVERSATION_DELETED:
				updateTableWhenNewConversationDeleted((ConvInfo) object);
				break;
		}
	}

	public void updateTableWhenNewConversationCreated(ConvInfo convInfo) {

		//TODO : Correct implementation will be to update this table on pubsub that will be thrown on sucess of profile call for unknown caontact.
		String msisdn = convInfo.getMsisdn();
		if (TextUtils.isEmpty(msisdn) || OneToNConversationUtils.isOneToNConversation(msisdn) || BotUtils.isBot(msisdn)) {
			return;
		}
		boolean doesConversationExist = doesContactExist(msisdn);
		if (doesConversationExist) {
			return;
		}
		ContentValues cv = new ContentValues();
		cv.put(DBConstants.MSISDN, msisdn);
		cv.put(DBConstants.ONHIKE, convInfo.isOnHike() ? 1 : 0);

		mDb.insertWithOnConflict(DBConstants.USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}

	private void updateTableWhenNewConversationDeleted(ConvInfo convInfo) {
		String msisdn = convInfo.getMsisdn();
		if (TextUtils.isEmpty(msisdn) || OneToNConversationUtils.isOneToNConversation(msisdn) || BotUtils.isBot(msisdn)) {
			return;
		}
		// Deletion Logic:If saved in addressBk return
		//if fav or BS = 1 return


		boolean isInAddressBook = isAddressBookContact(msisdn);

		if(isInAddressBook)
		{
			return;
		}

		String selection = DBConstants.MSISDN + " =? AND " +DBConstants.FAVORITE_TYPE  + " = 0    AND " + DBConstants.BLOCK_STATUS + " = 0";
		Logger.d(TAG,"updateTableWhenNewConversationDeleted --> delete success" + selection);
		long value = mDb.delete(DBConstants.USERS_TABLE, selection, new String[]{msisdn});
		if (value == 0) {
			ContentValues cv = new ContentValues();
			cv.putNull(DBConstants.ID);
			cv.putNull(DBConstants.NAME);
			cv.putNull(DBConstants.PHONE);
			value = mDb.update(DBConstants.USERS_TABLE, cv, DBConstants.MSISDN + " = ? and id is null", new String[]{msisdn});
			Logger.d(TAG,"updateTableWhenNewConversationDeleted --> update success" + selection);
		}
	}

	boolean isAddressBookContact(String msisdn) {
		if (TextUtils.isEmpty(msisdn)) {
			return false;
		}
		boolean isAddressBookContact = false;
		Cursor c = null;
		try {


			c = mDb.rawQuery("Select name  from users where msisdn = ?  and id is not null and name is not null", new String[]{msisdn});
			isAddressBookContact = c.getCount() > 0 ? true : false;
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return isAddressBookContact;
	}
}
