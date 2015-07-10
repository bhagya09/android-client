/**
 * File   : HikeStickerSearchDatabase.java
 * Content: It contains all operations regarding creating/upgrading/inserting in/reading/removing to/from Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

public class HikeStickerSearchDatabase extends SQLiteOpenHelper
{
	private static final String TAG = HikeStickerSearchDatabase.class.getSimpleName();

	private volatile Context mContext;

	private volatile SQLiteDatabase mDb;

	private static volatile HikeStickerSearchDatabase sHikeStickerSearchDatabase;

	private static final Object sDatabaseLock = new Object();

	private HikeStickerSearchDatabase(Context context)
	{
		super(context, HikeStickerSearchBaseConstants.DATABASE_HIKE_STICKER_SEARCH, null, HikeStickerSearchBaseConstants.STICKERS_SEARCH_DATABASE_VERSION);

		Logger.i(TAG, "HikeStickerSearchDatabase(" + context + ")");
		mContext = context;
		mDb = getWritableDatabase();
	}

	/* Call to initialize database instance */
	private static void init()
	{
		Logger.i(TAG, "init()");

		synchronized (sDatabaseLock)
		{
			if (sHikeStickerSearchDatabase == null)
			{
				sHikeStickerSearchDatabase = new HikeStickerSearchDatabase(HikeMessengerApp.getInstance());
			}
			if (sHikeStickerSearchDatabase.mDb == null)
			{
				sHikeStickerSearchDatabase.close();
				sHikeStickerSearchDatabase.mDb = sHikeStickerSearchDatabase.getWritableDatabase();
			}
			if (sHikeStickerSearchDatabase.mContext == null)
			{
				sHikeStickerSearchDatabase.mContext = HikeMessengerApp.getInstance();
			}
		}
	}

	/* Get instance of database from outside of this class */
	public static HikeStickerSearchDatabase getInstance()
	{
		if ((sHikeStickerSearchDatabase == null) || (sHikeStickerSearchDatabase.mDb == null) || (sHikeStickerSearchDatabase.mContext == null))
		{
			Logger.w(TAG, "Either database has not been initialized or, reinitializing...");
			init();
		}

		return sHikeStickerSearchDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Logger.i(TAG, "onCreate(" + db + ")");

		try
		{
			createFixedTables(db);
		}
		catch (SQLException e)
		{
			Logger.e(TAG, "Error in executing sql: ", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Logger.i(TAG, "onUpgrade(" + db + ", " + oldVersion + ", " + newVersion + ")");
	}

	/* Add tables which are either fixed or default (virtual table) */
	private void createFixedTables(SQLiteDatabase db)
	{
		Logger.i(TAG, "createFixedTables(" + db + ")");

		if (db == null)
		{
			db = mDb;
		}

		// Create fixed table: TABLE_STICKER_TAG_ENTITY
		// Primary key : Integer [Compulsory]
		// Name of Entity : String [Compulsory], eg. InitMarker, ContactNumber, GrouId, ChatStory, Region/Language, State etc.
		// Type of Entity : Integer [Compulsory], Recognize to know what kind of entity is in above examples
		// Qualified Data : String [Optional], Data of entity, which can be directly 'imposed over'/'defined for' client user
		// Unqualified data : String [Optional], Data of entity, which can be used relatively to determine order of probability distribution
		String sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY + HikeStickerSearchBaseConstants.SYNTAX_START
				+ HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_PRIMARY_KEY + HikeStickerSearchBaseConstants.ENTITY_NAME
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.ENTITY_TYPE + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT
				+ HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.ENTITY_UNQUALIFIED_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + HikeStickerSearchBaseConstants.SYNTAX_END;
		db.execSQL(sql);

		// Create fixed table: TABLE_STICKER_PACK_CATEGORY_HISTORY
		// Category Id : String [Compulsory]
		// Chat Story : String [Compulsory], IDs of associated stories put from TABLE_STICKER_TAG_ENTITY
		// Sticker History : String [Compulsory], History of each sticker falling under given category
		// Overall History : String [Compulsory], History of given category w.r.t. its own
		sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_PACK_CATEGORY_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_START
				+ HikeStickerSearchBaseConstants.CATEGORY_ID + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.CATEGORY_CHAT_STORIES
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.CATEGORY_STICKERS_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.CATEGORY_OVERALL_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + HikeStickerSearchBaseConstants.SYNTAX_END;
		db.execSQL(sql);

		// Create fixed table: TABLE_STICKER_TAG_MAPPING
		// Primary key : Integer [Compulsory]
		// Tag Word/ Phrase : String [Compulsory], Given tag either a single word or a phrase
		// State Function : String [Compulsory], Frequency density function of states
		// Region Function : String [Compulsory], Frequency density function of regions/ languages
		// Overall Tag History : String [Compulsory], History of given tag w.r.t. associated sticker
		// Overall Sticker History : String [Compulsory], History of associated sticker irrespective of given tag
		// Associated Story : String [Compulsory], ID of associated themes put from TABLE_STICKER_TAG_ENTITY
		// Moment Attribute : Integer [Optional], Optional data such as time specialization
		// Festival Attribute : String [Optional], Optional data such as celebration specialization
		// Age Attribute : Integer [Optional], Optional data to determine how old sticker is
		// Exactness : Integer [Optional], Optional data to determine how closely sticker is related to given tag
		// Sticker Information : String [Compulsory], Recognizer code in the form of "pack_id:sticker_id"
		// Prefix Strings Used With : String [Optional], List of words/ texts with which given tag is used in LRU cycle
		// Surrounding Words For Rejection : String [Optional], List of words/ texts with which given tag-sticker is rejected in LRU cycle
		sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + HikeStickerSearchBaseConstants.SYNTAX_START
				+ HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_PRIMARY_KEY + HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.STICKER_REGION_FUNCTION_OF_FREQUENCY
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.STICKER_STATE_FUNCTION_OF_FREQUENCY
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY_FOR_TAG
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_STORY_THEME_ENTITIES + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_FESTIVALS + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_STRING_USED_WITH_TAG + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_WORDS_NOT_USED_WITH_TAG + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + HikeStickerSearchBaseConstants.SYNTAX_END;
		db.execSQL(sql);
	}

	/* Prepare search engine database */
	public void prepare()
	{
		Logger.i(TAG, "prepare()");

		String[] tables = new String[HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT];
		tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
		int remainingCount = HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT - 1;
		for (int i = 0; i < remainingCount; i++)
		{
			tables[i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
		}
		Logger.d(TAG, "Starting population first time...");
		createVirtualTable(tables);
	}

	/* Mark for first time setup to know the status of setup/update/elimination/insertion/re-balancing */
	public void markDataInsertionInitiation()
	{
		Logger.i(TAG, "markDataInsertionInitiation()");

		ContentValues cv = new ContentValues();
		cv.put(HikeStickerSearchBaseConstants.ENTITY_NAME, HikeStickerSearchBaseConstants.IS_INITIALISED);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_TYPE, HikeStickerSearchBaseConstants.ENTITY_INIT_MARKER);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY, HikeStickerSearchBaseConstants.STRING_EMPTY);

		mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY, null, cv);
	}

	/* Create virtual table used for searching tags */
	public void createVirtualTable(String[] tablesName)
	{
		Logger.d(TAG, "createVirtualTable(" + Arrays.toString(tablesName) + ")");

		// Create fixed virtual table: TABLE_STICKER_TAG_TEXT_SEARCH_PrefixStart_PrefixEnd
		// Real Tag Word/Phrase : String [Compulsory], Given tag either a single word or a phrase, element of [PrefixStart*, PrefixEnd*)
		// Group Id : Integer [Compulsory], Group id of given tag put from TABLE_STICKER_TAG_MAPPING
		String sql;

		try
		{
			if (tablesName != null)
			{
				for (int i = 0; i < tablesName.length; i++)
				{
					sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_VTABLE + tablesName[i] + HikeStickerSearchBaseConstants.SYNTAX_FTS_VERSION_4
							+ HikeStickerSearchBaseConstants.SYNTAX_START + HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + HikeStickerSearchBaseConstants.SYNTAX_NEXT
							+ HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_NEXT + HikeStickerSearchBaseConstants.SYNTAX_FOREIGN_KEY
							+ HikeStickerSearchBaseConstants.SYNTAX_START + HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_END
							+ HikeStickerSearchBaseConstants.SYNTAX_FOREIGN_REF + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING
							+ HikeStickerSearchBaseConstants.SYNTAX_START + HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_END
							+ HikeStickerSearchBaseConstants.SYNTAX_END;
					mDb.execSQL(sql);
				}
			}
			else
			{
				Logger.e(TAG, "Invalid table names to create for fts");
			}
		}
		catch (SQLException e)
		{
			Logger.d(TAG, "Error in executing sql: " + e.getMessage());
		}
	}

	/* Do not change the order of deletion as per dependency of foreign keys. */
	public void deleteDataInTables(boolean isNeedToDeleteAllSearchData)
	{
		Logger.d(TAG, "deleteAll(" + isNeedToDeleteAllSearchData + ")");

		// Delete tables used for search
		deleteSearchData();

		if (isNeedToDeleteAllSearchData)
		{
			// Delete fixed table: TABLE_STICKER_CATEGORY_HISTORY
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_PACK_CATEGORY_HISTORY, null, null);

			// Delete fixed table: TABLE_STICKER_TAG_MAPPING
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, null);

			// Delete fixed table: TABLE_STICKER_TAG_MAPPING
			mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY, null, null);
		}
	}

	/* Delete fts data */
	private void deleteSearchData()
	{
		Logger.d(TAG, "deleteSearchData()");

		// Delete dynamically added tables
	}

	public long[] insertIntoPrimaryTable(ArrayList<String> tags, ArrayList<Integer> priorities,ArrayList<Integer> moments, ArrayList<String> stickerInfo)
	{

		long[] rows = new long[tags.size()];
		try
		{
			mDb.beginTransaction();
			for (int j = 0; j < tags.size();)
			{
				for (int i = 0; (i < 1000) && (j < tags.size()); i++)
				{
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, tags.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY, priorities.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME, moments.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, stickerInfo.get(j));
					rows[j++] = mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, cv);
				}
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

		return rows;
	}

	private ArrayList<ArrayList<Object>> searchInPrimaryTable(String match, int[] primaryKeys, boolean isExactMatchNeeded)
	{

		ArrayList<ArrayList<Object>> list = null;
		ArrayList<ArrayList<Object>> tempList = null;
		ArrayList<Float> matchRankList = null;
		Cursor c = null;
		if ((primaryKeys == null) || (primaryKeys.length <= 0))
			return tempList;
		try
		{
			String[] args = new String[primaryKeys.length];
			for (int i = 0; i < primaryKeys.length; i++)
			{
				args[i] = String.valueOf(primaryKeys[i]);
			}

			StringBuilder sb = new StringBuilder(primaryKeys.length * 2 - 1);
			sb.append("?");
			for (int i = 1; i < primaryKeys.length; i++)
			{
				sb.append(",?");
			}

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN (" + sb.toString() + ")", args, null,
					null, null);
			if (c != null)
			{
				int count = c.getCount();
				if (count > 0)
				{
					list = new ArrayList<ArrayList<Object>>(count);
					tempList = new ArrayList<ArrayList<Object>>(count);
					matchRankList = new ArrayList<Float>(count);
					ArrayList<String> temp = new ArrayList<String>();
					if (match.contains(" "))
					{
						int wordCountInPhrase = StickerSearchUtility.splitAndDoIndexing(match, " ").first.size();
						while (c.moveToNext())
						{
							temp.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)));
							tempList.add(buildStickerData(c));
							Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> tagPhrases = StickerSearchUtility.splitAndDoIndexing(
									c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)), " ");
							float score = ((float) (wordCountInPhrase * (tagPhrases.first.size() > 2 ? 70 : 100))) / tagPhrases.first.size();
							Logger.d(TAG, "scores: " + c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)) + ": " + score);
							matchRankList.add(score);
						}
					}
					else
					{
						if (isExactMatchNeeded)
						{
							String actualTag;
							int matchLength = match.length();
							if (matchLength > 1)
							{
								count = 0;
								while (c.moveToNext())
								{
									actualTag = c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE));
									Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> tagPhrases = StickerSearchUtility.splitAndDoIndexing(actualTag, " ");
									if (match.equalsIgnoreCase(tagPhrases.first.get(0)))
									{
										temp.add(actualTag);
										tempList.add(buildStickerData(c));
										float score = ((float) (matchLength * (tagPhrases.first.size() > 1 ? 60 : 100))) / (tagPhrases.first.get(0).length());
										Logger.d(TAG, "scores: " + c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)) + ": " + score);
										matchRankList.add(score);
									}
									else
									{
										Logger.d(TAG, "Rejected phrase: " + actualTag);
									}
								}
							}
							else
							{
								count = 0;
								while (c.moveToNext())
								{
									actualTag = c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE));
									Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> tagPhrases = StickerSearchUtility.splitAndDoIndexing(actualTag, " ");
									if (tagPhrases.first.get(0).length() == 1)
									{
										temp.add(actualTag);
										tempList.add(buildStickerData(c));
										float score = ((tagPhrases.first.size() > 1) ? 50f : 100f);
										Logger.d(TAG, "scores: " + c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)) + ": " + score);
										matchRankList.add(score);
										count++;
									}
								}
							}
						}
						else
						{
							while (c.moveToNext())
							{
								temp.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)));
								tempList.add(buildStickerData(c));
								Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> tagPhrases = StickerSearchUtility.splitAndDoIndexing(
										c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)), " ");
								float score = ((float) (match.length() * (tagPhrases.first.size() > 1 ? 70 : 100))) / (tagPhrases.first.get(0).length());
								Logger.d(TAG, "scores: " + c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)) + ": " + score);
								matchRankList.add(score);
							}
						}
					}

					// set order
					float maxMatch = Float.MIN_VALUE;
					int prefCount = 8;
					ArrayList<Object> previousStikcer = null;
					ArrayList<Object> currentStickerData;
					for (int i = 0; i < prefCount && i < count; i++)
					{
						maxMatch = Collections.max(matchRankList);
						int index = matchRankList.indexOf(maxMatch);
						currentStickerData = tempList.get(index);
						matchRankList.remove(index);
						tempList.remove(index);

						if ((previousStikcer == null) || !currentStickerData.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE).equals(
								previousStikcer.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE)))
						{
							list.add(currentStickerData);
						}
						else
						{
							if ((int) currentStickerData.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER) >
								(int) previousStikcer.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER))
							{
								list.set(list.size() - 1, currentStickerData);
							}

							prefCount++;
						}

						previousStikcer = currentStickerData;
					}

					list.addAll(tempList);
					Logger.i(TAG, "Search findings count = " + list.size());
					Logger.i(TAG, "Search findings: " + list);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
			SQLiteDatabase.releaseMemory();
		}

		return list;
	}

	private ArrayList<Object> buildStickerData(Cursor c)
	{
		ArrayList<Object> data = new ArrayList<Object>(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_COUNT);

		// Do not change the order of insertion as per indices defined as followed
		// INDEX_STICKER_DATA_STICKER_CODE = 0;
		// INDEX_STICKER_DATA_TAG_PHRASE = 1;
		// INDEX_STICKER_DATA_PHRASE_LANGUAGE = 2;
		// INDEX_STICKER_DATA_TAG_CATEGORY = 3;
		// INDEX_STICKER_DATA_OVERALL_FREQUENCY_FOR_TAG = 4;
		// INDEX_STICKER_DATA_OVERALL_FREQUENCY = 5;
		// INDEX_STICKER_DATA_STORY_THEMES = 6;
		// INDEX_STICKER_DATA_EXACTNESS_ORDER = 7;
		// INDEX_STICKER_DATA_MOMENT_CODE = 8;
		// INDEX_STICKER_DATA_FESTIVALS = 9;
		// INDEX_STICKER_DATA_AGE = 10;
		// INDEX_STICKER_DATA_USED_WITH_STRINGS = 11;
		// INDEX_STICKER_DATA_REJECTED_WITH_WORDS = 12;
		// INDEX_STICKER_DATA_COUNT = 13;

		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_REGION_FUNCTION_OF_FREQUENCY)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STATE_FUNCTION_OF_FREQUENCY)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY_FOR_TAG)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STORY_THEME_ENTITIES)));
		data.add(c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY)));
		data.add(c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_FESTIVALS)));
		data.add(c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STRING_USED_WITH_TAG)));
		data.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_WORDS_NOT_USED_WITH_TAG)));

		return data;
	}

	public void insertIntoFTSTable(ArrayList<String> tags, long[] rows)
	{

		try
		{
			mDb.beginTransaction();
			for (int j = 0; j < tags.size();)
			{
				for (int i = 0; (i < 1000) && (j < tags.size()); i++)
				{
					String s = tags.get(j).toUpperCase(Locale.ENGLISH).trim();
					char[] array = s.toCharArray();
					String table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
							: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array[0];
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE, s);
					cv.put(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID, rows[j++]);
					mDb.insert(table, null, cv);
				}
				SQLiteDatabase.releaseMemory();
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public ArrayList<ArrayList<Object>> searchIntoFTSAndFindStickerList(String phrase, boolean isExactMatchNeeded)
	{

		int[] rows = null;
		Cursor c = null;
		try
		{
			char[] array = phrase.toCharArray();
			String table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
					+ array[0];
			Logger.d(TAG, "Searching \"" + phrase + "\" in " + table + ", exact search: " + isExactMatchNeeded);

			if (isExactMatchNeeded)
			{
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + table + " MATCH '" + phrase + "'", null);
			}
			else
			{
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + table + " MATCH '" + phrase + "*'", null);
			}

			if (c != null)
			{
				int i = 0;
				rows = new int[c.getCount()];
				while (c.moveToNext())
				{
					rows[i++] = c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID));
				}
			}
		}
		catch (SQLiteException e)
		{
			Logger.e(TAG, "Exception while searching \"" + phrase + "\"", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return searchInPrimaryTable(phrase, rows, isExactMatchNeeded);
	}

	public void disableTagsForDeletedStickers(Set<String> stickerInfo)
	{

		if (stickerInfo == null || stickerInfo.isEmpty())
			return;

		Cursor c = null;
		ArrayList<Long> primaryKeys = null;
		try
		{
			Iterator<String> iterator = stickerInfo.iterator();
			String[] args = new String[stickerInfo.size()];
			int i = 0;
			while (iterator.hasNext())
			{
				args[i++] = iterator.next();
			}

			StringBuilder sb = new StringBuilder(args.length * 2 - 1);
			sb.append("?");
			for (i = 1; i < args.length; i++)
			{
				sb.append(",?");
			}

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " NOT IN (" + sb.toString()
					+ ")", args, null, null, null);
			if (c != null)
			{
				if (c.getCount() > 0)
				{
					primaryKeys = new ArrayList<Long>(c.getCount());
					while (c.moveToNext())
					{
						primaryKeys.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
					}
				}
			}

			if (primaryKeys != null && primaryKeys.size() > 0)
			{
				String[] groupIds = new String[primaryKeys.size()];
				for (i = 0; i < primaryKeys.size(); i++)
				{
					groupIds[i] = String.valueOf(primaryKeys.get(i));
				}
				sb = new StringBuilder(groupIds.length * 2 - 1);
				sb.append("?");
				for (i = 1; i < groupIds.length; i++)
				{
					sb.append(",?");
				}
				String[] tables = new String[27];
				tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
				for (i = 0; i < 26; i++)
				{
					tables[i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
				}

				try
				{
					mDb.beginTransaction();
					for (i = 0; i < 27; i++)
					{
						mDb.delete(tables[i], "WHERE " + HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + " IN (" + sb.toString() + ")", groupIds);
						SQLiteDatabase.releaseMemory();
						try
						{
							Thread.sleep(20);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				finally
				{
					mDb.setTransactionSuccessful();
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
			SQLiteDatabase.releaseMemory();
		}
	}
}