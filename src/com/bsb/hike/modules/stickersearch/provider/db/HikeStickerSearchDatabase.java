/**
 * File   : HikeStickerSearchDatabase.java
 * Content: It contains all operations regarding creating/upgrading/inserting in/reading/removing to/from Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.StickerTagDataContainer;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

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

	private static final String TAG_REBALANCING = TAG + "_Rebalancing";

	private Random mRandom;

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

			if (sHikeStickerSearchDatabase.mRandom == null)
			{
				sHikeStickerSearchDatabase.mRandom = new Random();
			}
		}
	}

	/* Get instance of database from outside of this class */
	public static HikeStickerSearchDatabase getInstance()
	{
		if ((sHikeStickerSearchDatabase == null) || (sHikeStickerSearchDatabase.mDb == null) || (sHikeStickerSearchDatabase.mContext == null)
				|| (sHikeStickerSearchDatabase.mRandom == null))
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
		// Associated Story Themes : String [Compulsory], ID of associated themes put from TABLE_STICKER_TAG_ENTITY
		// Moment Attribute : Integer [Optional], Optional data such as time specialization
		// Festival Attribute : String [Optional], Optional data such as celebration specialization
		// Age Attribute : Integer [Optional], Optional data to determine how old sticker is
		// Exactness : Integer [Optional], Optional data to determine how closely sticker is related to given tag
		// Sticker Information : String [Compulsory], Recognizer code in the form of "pack_id:sticker_id"
		// Prefix Strings Used With : String [Optional], List of words/ texts with which given tag is used in LRU cycle
		// Surrounding Words For Rejection : String [Optional], List of words/ texts with which given tag-sticker is rejected in LRU cycle
		// Popularity : Integer [Compulsory], Order of suitability of tag for given sticker i.e. overall ranking of tag among all tags used with that sticker in terms of usage
		// count
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
				+ HikeStickerSearchBaseConstants.STICKER_WORDS_NOT_USED_WITH_TAG + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_LAST + HikeStickerSearchBaseConstants.SYNTAX_END;
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
			Logger.d(TAG, "Error in executing sql: ", e);
		}
	}

	/* Do not change the order of deletion as per dependency of foreign keys. */
	public void deleteDataInTables(boolean isNeedToDeleteAllSearchData)
	{
		Logger.d(TAG, "deleteAll(" + isNeedToDeleteAllSearchData + ")");

		try
		{
			mDb.beginTransaction();

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
			SQLiteDatabase.releaseMemory();

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	/* Delete fts data */
	private void deleteSearchData()
	{
		Logger.d(TAG, "deleteSearchData()");

		// Delete dynamically added tables
		String[] tables = new String[HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT];

		tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
		mDb.delete(tables[0], null, null);
		SQLiteDatabase.releaseMemory();
		try
		{
			Thread.sleep(5);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		int remainingCount = HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT - 1;
		for (int i = 0; i < remainingCount; i++)
		{
			tables[i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
			mDb.delete(tables[i + 1], null, null);

			SQLiteDatabase.releaseMemory();
			try
			{
				Thread.sleep(5);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void insertStickerTagData(Map<String, ArrayList<String>> packStoryData, ArrayList<StickerTagDataContainer> stickersTagData)
	{
		Logger.i(TAG, "insertStickerTagData()");

		ArrayList<String> tags = new ArrayList<String>();
		ArrayList<Long> rows = new ArrayList<Long>();

		try
		{
			mDb.beginTransaction();
			for (StickerTagDataContainer stickerTagData : stickersTagData)
			{
				String stickerCode = stickerTagData.getStickerCode();
				ArrayList<String> stickerTags = stickerTagData.getTagList();
				ArrayList<Integer> tagExactnessPriorities = stickerTagData.getTagExactMatchPriorityList();
				int stickerMoment = stickerTagData.getMomentCode();

				for (int i = 0; i < stickerTags.size(); i++)
				{
					tags.add(stickerTags.get(i));
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, stickerTags.get(i));
					cv.put(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY, tagExactnessPriorities.get(i));
					cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME, stickerMoment);
					cv.put(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, stickerCode);
					cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, 0);
					rows.add(mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, cv));
				}
				try
				{
					Thread.sleep(5);
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

		insertIntoVirtualTable(tags, rows);
	}

	private ArrayList<ArrayList<Object>> searchInPrimaryTable(String match, ArrayList<Long> primaryKeys, boolean isExactMatchNeeded)
	{
		ArrayList<ArrayList<Object>> list = null;
		int size = ((primaryKeys == null) ? 0 : primaryKeys.size());
		if (size <= 0)
		{
			return list;
		}

		Cursor c = null;
		try
		{
			ArrayList<ArrayList<Object>> tempList = null;
			ArrayList<Float> matchRankList = null;
			String[] args = new String[size];

			for (int i = 0; i < size; i++)
			{
				args[i] = String.valueOf(primaryKeys.get(i));
			}

			StringBuilder sb = new StringBuilder(size * 2 - 1);
			sb.append("?");
			for (int i = 1; i < size; i++)
			{
				sb.append(",?");
			}

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN(" + sb.toString() + ")", args, null,
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

						if ((previousStikcer == null)
								|| !currentStickerData.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE).equals(
										previousStikcer.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE)))
						{
							list.add(currentStickerData);
						}
						else
						{
							if ((int) currentStickerData.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER) > (int) previousStikcer
									.get(HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER))
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

	public void insertIntoVirtualTable(ArrayList<String> tags, ArrayList<Long> referenceids)
	{
		Logger.i(TAG, "insertIntoVirtualTable()");

		int count = tags.size();
		int remainingCount = count;
		int currentCount;
		try
		{
			mDb.beginTransaction();
			for (int i = 0; i < count;)
			{
				currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
						: remainingCount;
				for (int j = 0; j < currentCount; j++)
				{
					String s = tags.get(i);
					char[] array = s.toCharArray();
					String table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
							: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array[0];
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE, s);
					cv.put(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID, referenceids.get(i++));
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

				remainingCount -= currentCount;
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

		ArrayList<Long> tempRows = null;
		ArrayList<Long> rows = null;
		Cursor c = null;

		try
		{
			char[] array = phrase.toCharArray();
			String table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
					+ array[0];
			Logger.d(TAG, "Searching \"" + phrase + "\" in " + table + ", exact search: " + isExactMatchNeeded);

			if (isExactMatchNeeded)
			{
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + " MATCH '" + phrase + "'", null);
			}
			else
			{
				c = mDb.rawQuery("SELECT * FROM " + table + " WHERE " + HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + " MATCH '" + phrase + "*'", null);
			}

			int count = ((c == null) ? 0 : c.getCount());
			if (count > 0)
			{
				tempRows = new ArrayList<Long>(count);
				while (c.moveToNext())
				{
					tempRows.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID)));
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

		if ((tempRows != null) && (tempRows.size() > HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER))
		{
			Collections.shuffle(tempRows, mRandom);
			rows = new ArrayList<Long>(HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER);
			for (int i = 0; i < HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER; i++)
			{
				rows.add(tempRows.get(i));
			}
		}
		else
		{
			rows = tempRows;
		}

		return searchInPrimaryTable(phrase, rows, isExactMatchNeeded);
	}

	public void disableTagsForDeletedStickers(Set<String> stickerInfoSet)
	{
		if (stickerInfoSet == null)
		{
			return;
		}

		if (stickerInfoSet.isEmpty())
		{
			try
			{
				mDb.beginTransaction();

				mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, null);
				deleteSearchData();

				mDb.setTransactionSuccessful();
			}
			catch(SQLException e)
			{
				Logger.d(TAG, "Error in executing sql delete queries: ", e);
			}
			finally
			{
				mDb.endTransaction();
			}
			return;
		}

		HashSet<String> removingStickerSetInDatabase = new HashSet<String>();
		Cursor c = null;
		try
		{
			c = mDb.rawQuery("SELECT DISTINCT " + HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " FROM " + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING,
					null);
			if (c != null)
			{
				while (c.moveToNext())
				{
					removingStickerSetInDatabase.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)));
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
		removingStickerSetInDatabase.removeAll(stickerInfoSet);

		ArrayList<Long> primaryKeys = new ArrayList<Long>();
		Iterator<String> iterator = removingStickerSetInDatabase.iterator();
		String[] args = new String[removingStickerSetInDatabase.size()];

		for (int i = 0; iterator.hasNext(); i++)
		{
			args[i] = iterator.next();
		}

		for (int j = 0; j < args.length;)
		{
			int remainingCount = args.length - j;
			int count = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
					: remainingCount;

			StringBuilder sb = new StringBuilder(args.length * 2 - 1);
			sb.append("?");
			for (int i = 1; i < count; i++)
			{
				sb.append(",?");
			}

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null,
						HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " IN(" + sb.toString() + ")", Arrays.copyOfRange(args, j, (j + count)), null, null, null);
				if (c != null)
				{
					while (c.moveToNext())
					{
						primaryKeys.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
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

			j += count;
		}

		if (primaryKeys != null && primaryKeys.size() > 0)
		{
			String[] tables = new String[27];
			tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
			for (int i = 0; i < 26; i++)
			{
				tables[i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
			}

			String[] groupIds = new String[primaryKeys.size()];
			for (int i = 0; i < primaryKeys.size(); i++)
			{
				groupIds[i] = String.valueOf(primaryKeys.get(i));
			}

			try
			{
				mDb.beginTransaction();

				for (int j = 0; j < groupIds.length;)
				{
					int remainingCount = groupIds.length - j;
					int count = ((remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0) ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
							: remainingCount;

					StringBuilder sb = new StringBuilder(count * 2 - 1);
					sb.append(groupIds[j++]);
					for (int i = 1; i < count; i++)
					{
						sb.append(" OR " + groupIds[j++]);
					}

					for (int i = 0; i < 27; i++)
					{
						mDb.delete(tables[i], HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + " MATCH '" + sb.toString() + "'", null);
						SQLiteDatabase.releaseMemory();
					}

					sb.setLength(0);
					sb = new StringBuilder(count * 2 - 1);
					sb.append("?");
					for (int i = 1; i < count; i++)
					{
						sb.append(",?");
					}

					mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN(" + sb.toString() + ")", Arrays.copyOfRange(groupIds, (j - count), j));
					SQLiteDatabase.releaseMemory();
				}

				mDb.setTransactionSuccessful();
			}
			finally
			{
				mDb.endTransaction();
			}
		}
	}

	public void analyseMessageSent(String prevText, Sticker sticker, String nextText)
	{
		if (sticker == null)
		{
			return;
		}

		Cursor c = null;
		String stickerCode = StickerManager.getInstance().getStickerSetString(sticker);
		ArrayList<Long> rowIdList = null;
		ArrayList<Integer> previousFrequencyList = null;

		try
		{
			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + " IN(?)",
					new String[] { stickerCode }, null, null, null);
			if (c != null)
			{
				rowIdList = new ArrayList<Long>(c.getCount());
				previousFrequencyList = new ArrayList<Integer>(c.getCount());
				while (c.moveToNext())
				{
					rowIdList.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
					if (c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)) == null)
					{
						previousFrequencyList.add(0);
					}
					else
					{
						previousFrequencyList.add(Integer.parseInt(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY))));
					}
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

		if (rowIdList != null && rowIdList.size() > 0)
		{
			try
			{
				mDb.beginTransaction();

				for (int i = 0; i < rowIdList.size(); i++)
				{
					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY, String.valueOf(previousFrequencyList.get(i) + 1));
					if (!Utils.isBlank(prevText))
					{
						cv.put(HikeStickerSearchBaseConstants.STICKER_STRING_USED_WITH_TAG, prevText);
					}
					mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN(?)",
							new String[] { String.valueOf(rowIdList.get(i)) });
				}

				mDb.setTransactionSuccessful();
			}
			finally
			{
				mDb.endTransaction();
				SQLiteDatabase.releaseMemory();
			}
		}
	}

	public boolean startRebalancing()
	{
		Logger.d(TAG, "startRebalancing()");

		Cursor c = null;
		long count = 0;
		long currentTime = System.currentTimeMillis();

		try
		{
			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID }, null, null, null, null,
					HikeStickerSearchBaseConstants.UNIQUE_ID + " DESC", "1");
			if (c != null && c.moveToFirst())
			{
				count = c.getLong(0);
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

		if (count > 0)
		{
			int maxFrequency = Integer.MIN_VALUE;
			int minFrequency = Integer.MIN_VALUE;
			long remainingCount = count;
			int currentCount;

			for (long i = 0; i < count;)
			{
				currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
						: (int) remainingCount;
				i = i + currentCount;

				try
				{
					c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + "<=" + i, null, null, null,
							HikeStickerSearchBaseConstants.UNIQUE_ID + " DESC", String.valueOf(currentCount));
					if (c != null && c.getCount() > 0)
					{
						ArrayList<Integer> frequencies = new ArrayList<Integer>(c.getCount());
						while (c.moveToNext())
						{
							if (c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)) == null)
							{
								frequencies.add(0);
							}
							else
							{
								frequencies.add(Integer.parseInt(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY))));
							}
						}

						int currentMax = Collections.max(frequencies);
						int currentMin = Collections.min(frequencies);

						if (maxFrequency < currentMax)
						{
							maxFrequency = currentMax;
						}

						if (minFrequency > currentMin)
						{
							minFrequency = currentMin;
						}

						frequencies.clear();
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

				remainingCount -= currentCount;
			}

			if (maxFrequency > Integer.MIN_VALUE && minFrequency > Integer.MIN_VALUE)
			{
				ArrayList<String> tagList = new ArrayList<String>();
				ArrayList<String> stickerList = new ArrayList<String>();
				ArrayList<Long> rowIdList = new ArrayList<Long>();
				ArrayList<Integer> ageList = new ArrayList<Integer>();
				double thresholdFrequency = 0.4 * maxFrequency + 0.6 * minFrequency;
				double standardDeviation = Math.sqrt((Math.pow((maxFrequency - thresholdFrequency), 2) + Math.pow((thresholdFrequency - minFrequency), 2)) / 2);
				int cuttOffFrequency = (int) (thresholdFrequency - standardDeviation + 0.5);
				int frequency;
				remainingCount = count;

				for (long i = 0; i < count;)
				{
					currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
							: (int) remainingCount;
					i = i + currentCount;

					try
					{
						c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + "<=" + i, null, null, null,
								HikeStickerSearchBaseConstants.UNIQUE_ID + " DESC", String.valueOf(currentCount));
						if (c != null)
						{
							while (c.moveToNext())
							{
								if (c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)) == null)
								{
									frequency = 0;
								}
								else
								{
									frequency = Integer.parseInt(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)));
								}

								if (frequency < cuttOffFrequency)
								{
									tagList.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE)));
									stickerList.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)));
									rowIdList.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
									ageList.add(c.getInt(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE)));
								}
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

					remainingCount -= currentCount;
				}

				count = rowIdList.size();
				remainingCount = count;
				long thresholdTime = 7 * 24 * 3600 * 1000;
				long previousTime = HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_SUMMERIZATION_TIME, currentTime);
				boolean deleting = ((currentTime - previousTime) > thresholdTime);
				String table;

				try
				{
					mDb.beginTransaction();

					for (int i = 0; i < count;)
					{
						currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
								: (int) remainingCount;

						for (int j = 0; j < currentCount; j++)
						{
							// changed for testing only
							if (deleting && ageList.get(i) == 15)
							{
								Logger.d(TAG_REBALANCING, "Deleting tag: " + tagList.get(i) + " w.r.t. sticker: " + stickerList.get(i));

								char[] array = tagList.get(i).toCharArray();
								table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
										: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array[0];

								mDb.delete(table, HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + " MATCH '" + String.valueOf(rowIdList.get(i)) + "'", null);
								mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN(?)",
										new String[] { String.valueOf(rowIdList.get(i++)) });
							}
							else
							{
								int updatedAge;
								if (ageList.get(i) == 15)
								{
									updatedAge = 1;
								}
								else
								{
									updatedAge = ageList.get(i) + 1;
								}

								Logger.d(TAG_REBALANCING, "Aging tag: " + tagList.get(i) + " with age = " + updatedAge + " w.r.t. sticker: " + stickerList.get(i));

								ContentValues cv = new ContentValues();
								cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, updatedAge);
								mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID + " IN(?)",
										new String[] { String.valueOf(rowIdList.get(i++)) });
							}
						}

						remainingCount -= currentCount;
						try
						{
							Thread.sleep(5);
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
					SQLiteDatabase.releaseMemory();
				}
			}
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_SUMMERIZATION_TIME, currentTime);
		return true;
	}
}