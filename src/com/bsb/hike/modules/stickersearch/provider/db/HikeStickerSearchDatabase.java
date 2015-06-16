/**
 * File   : HikeStickerSearchDatabase.java
 * Content: It contains all operations while creating/upgrading/inserting in/reading/removing Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.utils.Logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HikeStickerSearchDatabase extends SQLiteOpenHelper {

	private static final String TAG = HikeStickerSearchDatabase.class.getSimpleName();

	private volatile Context mContext;
	private volatile SQLiteDatabase mDb;

	private static final Object sDatabaseLock = new Object();

	private static volatile HikeStickerSearchDatabase sHikeStickerSearchDatabase;

	private HikeStickerSearchDatabase(Context context) {
		super(context, HikeStickerSearchBaseConstants.DATABASE_HIKE_STICKER_SEARCH, null, HikeStickerSearchBaseConstants.STICKERS_SEARCH_DATABASE_VERSION);

		Logger.d(TAG, "HikeStickerSearchDatabase(" + context + ")");
		mContext = context;
		mDb = getWritableDatabase();
	}

	/* Call to initialize database for first time setup */
	public static void init(Context context) {
		Logger.d(TAG, "init(" + context + ")");

		synchronized (sDatabaseLock) {
			if (sHikeStickerSearchDatabase == null) {
				sHikeStickerSearchDatabase = new HikeStickerSearchDatabase(context);
			}
			if (sHikeStickerSearchDatabase.mDb == null) {
				sHikeStickerSearchDatabase.mDb = sHikeStickerSearchDatabase.getWritableDatabase();
			}
			if (sHikeStickerSearchDatabase.mContext == null) {
				sHikeStickerSearchDatabase.mContext = HikeMessengerApp.getInstance();
			}
		}
	}

	/* Get instance of this class from outside */
	public static HikeStickerSearchDatabase getInstance() {

		if (sHikeStickerSearchDatabase == null) {
			Logger.d(TAG, "Reinitializing...");
			init(HikeMessengerApp.getInstance());
		}
		return sHikeStickerSearchDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Logger.d(TAG, "onCreate(" + db + ")");

		try {
			addFixedTables(db);
		} catch (SQLException e) {
			Logger.d(TAG, "Error in executing sql: " + e.getMessage());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Logger.d(TAG, "onUpgrade(" + db + ", " + oldVersion + ", " + newVersion + ")");
	}

	/* Add tables which are either fixed or default (virtual table) */
	private void addFixedTables(SQLiteDatabase db) {
		Logger.v(TAG, "addFixedTables(" + db + ")");

		if (db == null) {
			db = mDb;
		}

		// Create fixed table: TABLE_STICKER_TAG_ENTITY
		// Primary key       : Integer [Compulsory]
		// Name of Entity    : String [Compulsory], eg. InitMarker, ContactNumber, GrouId, ChatStory, Region/Language, State etc.
		// Type of Entity    : Integer [Compulsory], Recognize to know what kind of entity is in above examples
		// Qualified Data    : String [Optional], Data of entity, which can be directly 'imposed over'/'defined for' client user
		// Unqualified data  : String [Optional], Data of entity, which can be used relatively to determine order of probability distribution
		String sql = HikeStickerSearchBaseConstants.CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY + " ( "
				+ HikeStickerSearchBaseConstants.UNIQUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ HikeStickerSearchBaseConstants.ENTITY_NAME + " TEXT, "
				+ HikeStickerSearchBaseConstants.ENTITY_TYPE + " INTEGER, "
				+ HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY + " TEXT, "
				+ HikeStickerSearchBaseConstants.ENTITY_UNQUALIFIED_HISTORY + " TEXT)";
		db.execSQL(sql);

		// Create fixed table: TABLE_STICKER_CATEGORY_HISTORY
		// Category Id       : String [Compulsory]
		// Chat Story        : String [Compulsory], ID of associated story put from TABLE_STICKER_TAG_ENTITY
		// Sticker History   : String [Compulsory], History of each sticker falling under given category
		// Overall History   : String [Compulsory], History of given category w.r.t. its own
		sql = HikeStickerSearchBaseConstants.CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_CATEGORY_HISTORY + " ( "
				+ HikeStickerSearchBaseConstants.CATEGORY_ID + " TEXT, "
				+ HikeStickerSearchBaseConstants.CATEGORY_CHAT_STORY_ID + " INTEGER, "
				+ HikeStickerSearchBaseConstants.CATEGORY_STICKERS_HISTORY + " TEXT, "
				+ HikeStickerSearchBaseConstants.CATEGORY_OVERALL_HISTORY + " TEXT, "
				+ HikeStickerSearchBaseConstants.FOREIGN_KEY + "(" + HikeStickerSearchBaseConstants.CATEGORY_CHAT_STORY_ID + ")"
				+ HikeStickerSearchBaseConstants.FOREIGN_REF + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY + "(" + HikeStickerSearchBaseConstants.UNIQUE_ID + "))";
		db.execSQL(sql);

		// Create fixed table: TABLE_STICKER_TAG_MAPPING
		// Primary key       : Integer [Compulsory]
		// Tag Word/Phrase   : String [Compulsory], Given tag either a single word or a phrase
		// State Function    : String [Compulsory], Frequency density function of states
		// Region Function   : String [Compulsory], Frequency density function of regions/languages
		// Tag History       : String [Compulsory], History of given tag w.r.t. associated sticker
		// Overall History   : String [Compulsory], History of associated sticker irrespective of given tag
		// Associated Story  : String [Compulsory], ID of associated story put from TABLE_STICKER_TAG_ENTITY
		// Extra Attributes  : String [Optional], Optional data such as festivals, specialization etc.
		// Prefixes used     : String [Optional], List of words/texts with which given tag is used in LRU cycle
		// Suffixes used     : String [Optional], List of words/texts with which given tag is used in LRU cycle
		// Prefixes rejected : String [Optional], List of words/texts with which given tag-sticker is rejected in LRU cycle
		// Suffixes rejected : String [Optional], List of words/texts with which given tag-sticker is rejected in LRU cycle
		sql = HikeStickerSearchBaseConstants.CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + " ( "
				+ HikeStickerSearchBaseConstants.UNIQUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_STATE_FUNCTION_OF_FREQUENCY + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_REGION_FUNCTION_OF_FREQUENCY + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY_FOR_TAG + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_STORY_TOPIC_ENTITY_ID + " INTEGER, "
				+ HikeStickerSearchBaseConstants.STICKER_EXTRA_ATTRIBUTES + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_PREFIX_STRING_USED_WITH_TAG + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_SUFFIX_STRING_USED_WITH_TAG + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_PREFIX_WORDS_NOT_USED_WITH_TAG + " TEXT, "
		//		+ HikeStickerSearchBaseConstants.STICKER_SUFFIX_WORDS_NOT_USED_WITH_TAG + " TEXT, "
				+ HikeStickerSearchBaseConstants.STICKER_SUFFIX_WORDS_NOT_USED_WITH_TAG + " TEXT)";
		//		+ HikeStickerSearchBaseConstants.FOREIGN_KEY + "(" + HikeStickerSearchBaseConstants.STICKER_STORY_TOPIC_ENTITY_ID + ")"
		//		+ HikeStickerSearchBaseConstants.FOREIGN_REF + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY + "(" + HikeStickerSearchBaseConstants.UNIQUE_ID + "))";
		db.execSQL(sql);
	}

	/* Mark for first time setup to know the status of setup/update/elimination/insertion/re-balancing */
	public void markDataInsertionInitiation() {
		Logger.d(TAG, "markDataInsertionInitiation()");

		ContentValues cv = new ContentValues();
		cv.put(HikeStickerSearchBaseConstants.ENTITY_NAME, HikeStickerSearchBaseConstants.isInitialized);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_TYPE, HikeStickerSearchBaseConstants.ENTITY_INIT_MARKER);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY, HikeStickerSearchBaseConstants.EMPTY);

		mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY, null, cv);
	}

	/* Create virtual table used for searching tags */
	public void createVirtualTable(String [] tablesName) {
		Logger.d(TAG, "createVirtualTable(" + Arrays.toString(tablesName) + ")");

		// Create fixed virtual table: TABLE_STICKER_TAG_TEXT_SEARCH_PrefixStart_PrefixEnd
		// Real Tag Word/Phrase      : String [Compulsory], Given tag either a single word or a phrase, element of [PrefixStart*, PrefixEnd*)
		// Group Id                  : Integer [Compulsory], Group id of given tag put from TABLE_STICKER_TAG_MAPPING
		String sql;

		try {
			if (tablesName != null) {
				for (int i = 0; i < tablesName.length; i++) {
					sql = HikeStickerSearchBaseConstants.CREATE_VTABLE + tablesName [i] + HikeStickerSearchBaseConstants.FTS_VERSION_4 + "("
							+ HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + ", "
							+ HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + ", "
							+ HikeStickerSearchBaseConstants.FOREIGN_KEY + "(" + HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + ")"
							+ HikeStickerSearchBaseConstants.FOREIGN_REF + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + "(" + HikeStickerSearchBaseConstants.UNIQUE_ID + "))";
					mDb.execSQL(sql);
				}
			} else {
				Logger.e(TAG, "Invalid table names to create for fts");
			}
		} catch (SQLException e) {
			Logger.d(TAG, "Error in executing sql: " + e.getMessage());
		}
	}

	/* Do not change the order of deletion as per dependency of foreign keys. */
	public void deleteDataInTables(boolean isNeedToDeleteSearchDataOnly) {
		Logger.d(TAG, "deleteAll(" + isNeedToDeleteSearchDataOnly + ")");

		if (isNeedToDeleteSearchDataOnly) {
			// Delete tables used for search
			deleteSearchData();
		} else {
			// Delete fixed table: TABLE_STICKER_CATEGORY_HISTORY
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_CATEGORY_HISTORY, null, null);

			// Delete tables used for search
			deleteSearchData();

			// Delete fixed table: TABLE_STICKER_TAG_MAPPING
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, null);

			// Delete fixed table: TABLE_STICKER_TAG_MAPPING
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY, null, null);
		}
	}

	/* Delete fts data */
	private void deleteSearchData() {
		Logger.d(TAG, "deleteSearchData()");

		// Delete dynamically added tables
	}

	public long [] insertIntoPrimaryTable(ArrayList<String> tags, ArrayList<String> stickerInfo) {

		long [] rows = new long [tags.size()];
		try {
			mDb.beginTransaction();
			for (int j = 0; j < tags.size(); ) {
				for (int i = 0; (i < 1000) && (j < tags.size()); i++) {
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, tags.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, stickerInfo.get(j));
					rows [j++] = mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, cv);
				}
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}

		return rows;
	}

	private ArrayList<String> searchInPrimaryTable(String match, int [] primaryKeys) {

		ArrayList<String> list = null;
		ArrayList<String> tempList = null;
		ArrayList<Float> matchRankList = null;
		Cursor c = null;
		if ((primaryKeys == null) || (primaryKeys.length <= 0)) return tempList;
		try {
			String [] args = new String [primaryKeys.length];
			for (int i = 0; i < primaryKeys.length; i++) {
				args [i] = String.valueOf(primaryKeys [i]);
			}

			StringBuilder sb = new StringBuilder(primaryKeys.length * 2 - 1);
	        sb.append("?");
	        for (int i = 1; i < primaryKeys.length; i++) {
	            sb.append(",?");
	        }

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN (" + sb.toString() + ")", args, null, null, null);
			if (c != null) {
				int count = c.getCount();
				if (count > 0) {
					list = new ArrayList<String>(count);
					tempList = new ArrayList<String>(count);
					matchRankList = new ArrayList<Float>(count);
					while(c.moveToNext()) {
						tempList.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)));
						Object [] [] tagPhrases = StickerSearchUtility.splitAndDoIndexing(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)), " ");
						float score = ((float) (match.length() * (tagPhrases.length > 1 ? 70 : 100))) / ((String) tagPhrases [0] [0]).length();
						Logger.d(TAG, "scores: " + c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)) + ": " + score);
						matchRankList.add(score);
					}

					Float maxMatch;
					for (int i = 0; i < 10 && i < count; i++) {
						maxMatch = Collections.max(matchRankList);
						int index = matchRankList.indexOf(maxMatch);
						matchRankList.remove(index);
						list.add(tempList.get(index));
						tempList.remove(index);
					}
					list.addAll(tempList);
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
			mDb.releaseMemory();
		}

		Logger.d(TAG, "list: " + list);
		return list;
	}

	public void insertIntoFTSTable(ArrayList<String> tags, long [] rows) {

		try {
			mDb.beginTransaction();
			for (int j = 0; j < tags.size(); ) {
				for (int i = 0; (i < 1000) && (j < tags.size()); i++) {
					char [] array = tags.get(j).toUpperCase(Locale.ENGLISH).toCharArray();
					String table = array [0] > 'Z' || array [0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array [0];
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE, tags.get(j));
					cv.put(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID, rows [j++]);
					mDb.insert(table, null, cv);
				}
				mDb.releaseMemory();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	public ArrayList<String> searchIntoFTSAndFindStickerList(String word, boolean t) {

		int [] rows = null;
		Cursor c = null;
		try {
			char [] array = word.toUpperCase(Locale.ENGLISH).toCharArray();
			String table = array [0] > 'Z' || array [0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array [0];
			if (t) {
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + table + " MATCH '" + word + "'", null);
			} else {
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + table + " MATCH '" + word + "*'", null);
			}

			if (c != null) {
				int i = 0;
				rows = new int [c.getCount()];
				while(c.moveToNext()){
					rows [i++] = c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID));
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return searchInPrimaryTable(word, rows);
	}

	public void disableTagsForDeletedStickers(Set<String> stickerInfo) {

		if (stickerInfo == null || stickerInfo.isEmpty()) return;

		Cursor c = null;
		ArrayList<Long> primaryKeys = null;
		try {
			Iterator<String> iterator = stickerInfo.iterator();
			String [] args = new String [stickerInfo.size()];
			int i = 0;
			while (iterator.hasNext()) {
				args [i++] = iterator.next();
			}

			StringBuilder sb = new StringBuilder(args.length * 2 - 1);
	        sb.append("?");
	        for (i = 1; i < args.length; i++) {
	            sb.append(",?");
	        }

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " IN (" + sb.toString() + ")", args, null, null, null);
			if (c != null) {
				if (c.getCount() > 0) {
					primaryKeys = new ArrayList<Long>(c.getCount());
					while(c.moveToNext()){
						primaryKeys.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
					}
				}
			}


			if (primaryKeys != null && primaryKeys.size() > 0) {
				String [] groupIds = new String [primaryKeys.size()];
				for (i = 0; i < primaryKeys.size(); i++) {
					groupIds [i] = String.valueOf(primaryKeys.get(i));
				}
				sb = new StringBuilder(groupIds.length * 2 - 1);
				sb.append("?");
				for (i = 1; i < groupIds.length; i++) {
					sb.append(",?");
				}
				String [] tables = new String [27];
				tables [0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
				for (i = 0; i < 26; i++) {
					tables [i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
				}

				try {
					mDb.beginTransaction();
					for (i = 0; i < 27; i++) {
						mDb.delete(tables [i], "WHERE " + HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + " IN (" + sb.toString() + ")", groupIds);
						mDb.releaseMemory();
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} finally {
					mDb.setTransactionSuccessful();
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
			mDb.releaseMemory();
		}
	}
}