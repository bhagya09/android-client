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
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.TagToStcikerDataContainer;
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

public class HikeStickerSearchDatabase extends SQLiteOpenHelper
{
	private static final String TAG = HikeStickerSearchDatabase.class.getSimpleName();

	private static final String TAG_REBALANCING = TAG + "$Rebalancing";

	private volatile int sMaxSelectionCount;

	private volatile Random mRandom;

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
		mRandom = new Random();
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

			if (sHikeStickerSearchDatabase.mContext == null)
			{
				sHikeStickerSearchDatabase.mContext = HikeMessengerApp.getInstance();
			}

			if (sHikeStickerSearchDatabase.mDb == null)
			{
				sHikeStickerSearchDatabase.close();
				sHikeStickerSearchDatabase.mDb = sHikeStickerSearchDatabase.getWritableDatabase();
			}

			if (sHikeStickerSearchDatabase.mRandom == null)
			{
				sHikeStickerSearchDatabase.mRandom = new Random();
			}

			sHikeStickerSearchDatabase.sMaxSelectionCount = (int) (StickerSearchConstants.MAXIMUM_SEARCH_COUNT * StickerSearchConstants.MAXIMUM_SELECTION_COUNT_RATIO);
		}
	}

	/* Get instance of database from outside of this class */
	public static HikeStickerSearchDatabase getInstance()
	{
		if ((sHikeStickerSearchDatabase == null) || (sHikeStickerSearchDatabase.mDb == null) || (sHikeStickerSearchDatabase.mContext == null)
				|| (sHikeStickerSearchDatabase.mRandom == null))
		{
			Logger.w(TAG, "Either database has not been initialized, initializing...");

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
		// Popularity : Integer [Compulsory], Order of tag for given sticker i.e. overall ranking of tag among all tags used with that sticker in terms of usage count
		// Availability : Integer [Compulsory], Download state of sticker, whether it is available for direct use or need to download
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
				+ HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY + HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT + HikeStickerSearchBaseConstants.STICKER_AVAILABILITY
				+ HikeStickerSearchBaseConstants.SYNTAX_INTEGER_LAST + HikeStickerSearchBaseConstants.SYNTAX_END;
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

	/* Mark for first time setup to know the status of setup/ update/ elimination/ insertion/ re-balancing */
	public void markDataInsertionInitiation()
	{
		Logger.i(TAG, "markDataInsertionInitiation()");

		ContentValues cv = new ContentValues();
		cv.put(HikeStickerSearchBaseConstants.ENTITY_NAME, HikeStickerSearchBaseConstants.IS_INITIALISED);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_TYPE, HikeStickerSearchBaseConstants.ENTITY_INIT_MARKER);
		cv.put(HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY, StickerSearchConstants.STRING_EMPTY);

		mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY, null, cv);
	}

	/* Create virtual table used for searching tags */
	public void createVirtualTable(String[] tablesName)
	{
		Logger.i(TAG, "createVirtualTable(" + Arrays.toString(tablesName) + ")");

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
				Logger.e(TAG, "Invalid table names to create for fts.");
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
		Logger.i(TAG, "deleteDataInTables(" + isNeedToDeleteAllSearchData + ")");

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
		Logger.i(TAG, "deleteSearchData()");

		// Delete dynamically added tables
		String[] tables = new String[HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT];

		tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
		mDb.delete(tables[0], null, null);
		SQLiteDatabase.releaseMemory();

		int remainingCount = HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT - 1;
		for (int i = 0; i < remainingCount; i++)
		{
			tables[i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
			mDb.delete(tables[i + 1], null, null);

			SQLiteDatabase.releaseMemory();
		}
	}

	public void insertStickerTagData(Map<String, ArrayList<String>> packStoryData, ArrayList<TagToStcikerDataContainer> stickersTagData)
	{
		Logger.i(TAG, "insertStickerTagData()");

		ArrayList<String> newTags = new ArrayList<String>();
		ArrayList<Long> rows = new ArrayList<Long>();

		Cursor c = null;
		long rowId;
		long existingTagsCount = 0;
		long newTagsCount = 0;
		boolean isExistingTag;
		String tag;

		try
		{
			mDb.beginTransaction();
			for (TagToStcikerDataContainer stickerTagData : stickersTagData)
			{
				if (!isValidTagData(stickerTagData))
				{
					Logger.wtf(TAG, "insertStickerTagData(), Wrong data for " + stickerTagData);
					continue;
				}

				String stickerCode = stickerTagData.getStickerCode();
				ArrayList<String> stickerTags = stickerTagData.getTagList();
				ArrayList<Integer> tagExactnessPriorities = stickerTagData.getTagExactMatchPriorityList();
				ArrayList<Integer> tagPopularities = stickerTagData.getTagPopularityList();
				int stickerMoment = stickerTagData.getMomentCode();
				int availability = stickerTagData.getStickerAvailabilityStatus() ? HikeStickerSearchBaseConstants.DECISION_STATE_YES
						: HikeStickerSearchBaseConstants.DECISION_STATE_NO;
				int size = stickerTags.size();

				for (int i = 0; i < size; i++)
				{
					tag = stickerTags.get(i);

					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY, tagExactnessPriorities.get(i));
					cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME, stickerMoment);
					cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY, tagPopularities.get(i));
					cv.put(HikeStickerSearchBaseConstants.STICKER_AVAILABILITY, availability);

					try
					{
						c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE
								+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_NEXT + HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
								+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { tag, stickerCode }, null, null, null);
					}
					finally
					{
						if (c != null)
						{
							isExistingTag = (c.getCount() > 0);
							c.close();
							c = null;

							if (isExistingTag)
							{
								existingTagsCount++;

								mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE
										+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_NEXT + HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
										+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { tag, stickerCode });
							}
							else
							{
								cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, tag);
								cv.put(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, stickerCode);
								cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, 0);

								rowId = mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, cv);

								if (rowId < HikeStickerSearchBaseConstants.SQLITE_FIRST_INTEGER_ROW_ID)
								{
									Logger.e(TAG, "Error while inserting tag '" + tag + "' into database !!!");
								}
								else
								{
									newTagsCount++;
									newTags.add(tag);
									rows.add(rowId);
								}
							}
						}
					}
				}
			}

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

		Logger.v(TAG, "Existing tags count = " + existingTagsCount);
		Logger.v(TAG, "Newly added tags count = " + newTagsCount);

		if (newTagsCount > 0)
		{
			Logger.v(TAG, "Newly added tags: " + newTags);

			insertIntoVirtualTable(newTags, rows);
		}
	}

	private boolean isValidTagData(TagToStcikerDataContainer stickersTagData)
	{
		return (stickersTagData == null) ? false : stickersTagData.isValidData();
	}

	private void insertIntoVirtualTable(ArrayList<String> tags, ArrayList<Long> referenceIds)
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
					String tag = tags.get(i);
					char firstChar = tag.charAt(0);
					String table = (firstChar > 'Z' || firstChar < 'A') ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
							: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + firstChar;

					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE, tag);
					cv.put(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID, referenceIds.get(i++));

					if (mDb.insert(table, null, cv) < HikeStickerSearchBaseConstants.SQLITE_FIRST_INTEGER_ROW_ID)
					{
						Logger.e(TAG, "Error while inserting tag '" + tag + "' in virtual table: " + table);
					}
				}

				SQLiteDatabase.releaseMemory();

				remainingCount -= currentCount;
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	private ArrayList<StickerDataContainer> searchInPrimaryTable(String matchKey, String[] referenceArgs, boolean isExactMatchNeeded)
	{
		ArrayList<StickerDataContainer> list = null;

		if ((referenceArgs != null) && (referenceArgs.length > 0))
		{
			Cursor c = null;

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID
						+ HikeStickerSearchBaseConstants.SYNTAX_IN_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParameterSyntax(referenceArgs.length)
						+ HikeStickerSearchBaseConstants.SYNTAX_END, referenceArgs, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, null, null);

				int count = (c == null) ? 0 : c.getCount();

				if (count > 0)
				{
					if (count <= sMaxSelectionCount)
					{
						list = selectTagsForStickers(matchKey, isExactMatchNeeded, count, c);
					}
					else
					{
						list = selectTagsForStickersWithLimit(matchKey, isExactMatchNeeded, count, c);
					}

					Logger.i(TAG, "Search findings count = " + list.size());
					Logger.i(TAG, "Search findings: " + list);
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

		return list;
	}

	private ArrayList<StickerDataContainer> selectTagsForStickers(String matchKey, boolean isExactMatchNeeded, int count, Cursor c)
	{
		ArrayList<StickerDataContainer> list = new ArrayList<StickerDataContainer>(count);
		int[] columnIndices = computeColumnIndices(c);

		if (matchKey.contains(StickerSearchConstants.STRING_SPACE) || !isExactMatchNeeded)
		{
			while (c.moveToNext())
			{
				list.add(buildStickerData(c, columnIndices));
			}
		}
		else
		{
			String actualTag;
			int firstWordLimitIndexInTag;

			while (c.moveToNext())
			{
				actualTag = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]);
				firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

				if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
				{
					if (matchKey.equalsIgnoreCase(actualTag.substring(0, firstWordLimitIndexInTag)))
					{
						list.add(buildStickerData(c, columnIndices));
					}
					else
					{
						Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
					}
				}
				else if (firstWordLimitIndexInTag == -1)
				{
					if (matchKey.equalsIgnoreCase(actualTag))
					{
						list.add(buildStickerData(c, columnIndices));
					}
					else
					{
						Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
					}
				}
				else
				{
					Logger.i(TAG, "Rejected search result due to invalid first word: " + actualTag);
				}
			}
		}

		return list;
	}

	private ArrayList<StickerDataContainer> selectTagsForStickersWithLimit(String matchKey, boolean isExactMatchNeeded, int count, Cursor c)
	{

		ArrayList<StickerDataContainer> list = new ArrayList<StickerDataContainer>(count);
		int[] columnIndices = computeColumnIndices(c);

		String previousStickerCode = null;
		String currentStickerCode;
		int tagCountPerSticker = 0;

		if (matchKey.contains(StickerSearchConstants.STRING_SPACE) || !isExactMatchNeeded)
		{
			while (c.moveToNext())
			{
				currentStickerCode = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE]);

				if (currentStickerCode.equals(previousStickerCode))
				{
					if (tagCountPerSticker >= StickerSearchConstants.MAXIMUM_TAG_SELECTION_PER_STICKER_COUNT)
					{
						Logger.i(TAG, "Skipped search result due to repeated tag: " + c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]));
					}
					else
					{
						list.add(buildStickerData(c, columnIndices));
						tagCountPerSticker++;
					}
				}
				else
				{
					list.add(buildStickerData(c, columnIndices));
					tagCountPerSticker = 1;
					previousStickerCode = currentStickerCode;
				}
			}
		}
		else
		{
			String actualTag;
			int firstWordLimitIndexInTag;

			while (c.moveToNext())
			{
				currentStickerCode = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE]);

				if (currentStickerCode.equals(previousStickerCode))
				{
					actualTag = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]);

					if (tagCountPerSticker >= StickerSearchConstants.MAXIMUM_TAG_SELECTION_PER_STICKER_COUNT)
					{
						Logger.i(TAG, "Skipped search result due to repeated tag: " + actualTag);
					}
					else
					{
						firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

						if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
						{
							if (matchKey.equalsIgnoreCase(actualTag.substring(0, firstWordLimitIndexInTag)))
							{
								list.add(buildStickerData(c, columnIndices));
								tagCountPerSticker++;
							}
							else
							{
								Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
							}
						}
						else if (firstWordLimitIndexInTag == -1)
						{
							if (matchKey.equalsIgnoreCase(actualTag))
							{
								list.add(buildStickerData(c, columnIndices));
								tagCountPerSticker++;
							}
							else
							{
								Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
							}
						}
						else
						{
							Logger.i(TAG, "Rejected search result due to invalid first word: " + actualTag);
						}
					}
				}
				else
				{
					actualTag = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]);
					firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

					if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
					{
						if (matchKey.equalsIgnoreCase(actualTag.substring(0, firstWordLimitIndexInTag)))
						{
							list.add(buildStickerData(c, columnIndices));
							tagCountPerSticker = 1;
							previousStickerCode = currentStickerCode;
						}
						else
						{
							Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
						}
					}
					else if (firstWordLimitIndexInTag == -1)
					{
						if (matchKey.equalsIgnoreCase(actualTag))
						{
							list.add(buildStickerData(c, columnIndices));
							tagCountPerSticker = 1;
							previousStickerCode = currentStickerCode;
						}
						else
						{
							Logger.i(TAG, "Rejected search result due to different first word: " + actualTag);
						}
					}
					else
					{
						Logger.i(TAG, "Rejected search result due to invalid first word: " + actualTag);
					}
				}
			}
		}

		return list;
	}

	private int[] computeColumnIndices(Cursor c)
	{
		int[] columnIndices;

		// Do not change the order of insertion as per indices defined as followed
		// INDEX_STICKER_DATA_STICKER_CODE = 0
		// INDEX_STICKER_DATA_TAG_PHRASE = 1
		// INDEX_STICKER_DATA_PHRASE_LANGUAGE = 2
		// INDEX_STICKER_DATA_TAG_STATE_CATEGORY = 3
		// INDEX_STICKER_DATA_OVERALL_FREQUENCY_FOR_TAG = 4
		// INDEX_STICKER_DATA_OVERALL_FREQUENCY = 5
		// INDEX_STICKER_DATA_STORY_THEMES = 6
		// INDEX_STICKER_DATA_EXACTNESS_ORDER = 7
		// INDEX_STICKER_DATA_MOMENT_CODE = 8
		// INDEX_STICKER_DATA_FESTIVALS = 9
		// INDEX_STICKER_DATA_AGE = 10
		// INDEX_STICKER_DATA_USED_WITH_STRINGS = 11
		// INDEX_STICKER_DATA_REJECTED_WITH_WORDS = 12
		// INDEX_STICKER_AVAILABILITY_STATUS = 13
		// INDEX_STICKER_DATA_COUNT = 14

		if (c != null)
		{
			columnIndices = new int[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_COUNT];

			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_PHRASE_LANGUAGE] = c
					.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_REGION_FUNCTION_OF_FREQUENCY);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_STATE_CATEGORY] = c
					.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STATE_FUNCTION_OF_FREQUENCY);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_OVERALL_FREQUENCY_FOR_TAG] = c
					.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY_FOR_TAG);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_OVERALL_FREQUENCY] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STORY_THEMES] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STORY_THEME_ENTITIES);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_MOMENT_CODE] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_FESTIVALS] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_FESTIVALS);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_AGE] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_USED_WITH_STRINGS] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_STRING_USED_WITH_TAG);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_REJECTED_WITH_WORDS] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_WORDS_NOT_USED_WITH_TAG);
			columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_AVAILABILITY_STATUS] = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_AVAILABILITY);
		}
		else
		{
			columnIndices = null;
		}

		return columnIndices;
	}

	private StickerDataContainer buildStickerData(Cursor c, int[] columnIndices)
	{
		StickerDataContainer sticker;

		if (columnIndices.length == HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_COUNT)
		{
			sticker = new StickerDataContainer(c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE]),
					c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]),
					c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_OVERALL_FREQUENCY]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_MOMENT_CODE]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_AVAILABILITY_STATUS]));
		}
		else
		{
			sticker = null;
		}

		return sticker;
	}

	public ArrayList<StickerDataContainer> searchIntoFTSAndFindStickerList(String phrase, boolean isExactMatchNeeded)
	{
		ArrayList<StickerDataContainer> result = null;
		ArrayList<Long> tempRows = null;
		String[] rows = null;
		Cursor c = null;

		try
		{
			char[] array = phrase.toCharArray();
			String table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
					+ array[0];
			Logger.i(TAG, "Searching \"" + phrase + "\" in " + table + ", exact search: " + isExactMatchNeeded);

			if (isExactMatchNeeded)
			{
				c = mDb.query(table, new String[] { HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID }, HikeStickerSearchBaseConstants.TAG_REAL_PHRASE
						+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + phrase + HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null, null, null, null);
			}
			else
			{
				c = mDb.query(table, new String[] { HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID }, HikeStickerSearchBaseConstants.TAG_REAL_PHRASE
						+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + phrase + HikeStickerSearchBaseConstants.SYNTAX_PREDICATE_MATCH_END, null, null, null, null);
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

		int rowsCount = (tempRows == null) ? 0 : tempRows.size();
		if (rowsCount > 0)
		{
			if (rowsCount > HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER)
			{
				Collections.shuffle(tempRows, mRandom);
				rowsCount = HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER;
			}

			rows = new String[rowsCount];

			for (int i = 0; i < rowsCount; i++)
			{
				rows[i] = String.valueOf(tempRows.get(i));
			}

			result = searchInPrimaryTable(phrase, rows, isExactMatchNeeded);
		}

		return result;
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
			catch (SQLException e)
			{
				Logger.e(TAG, "Error in executing sql delete queries: ", e);
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
			c = mDb.query(true, HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE },
					HikeStickerSearchBaseConstants.STICKER_AVAILABILITY + HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER,
					new String[] { String.valueOf(HikeStickerSearchBaseConstants.DECISION_STATE_YES) }, null, null, null, null);
		}
		finally
		{
			if (c != null)
			{
				while (c.moveToNext())
				{
					removingStickerSetInDatabase.add(c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE)));
				}

				c.close();
				c = null;
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
			int indexLimit = j + count;

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
						+ HikeStickerSearchBaseConstants.SYNTAX_IN_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParameterSyntax(count)
						+ HikeStickerSearchBaseConstants.SYNTAX_END, Arrays.copyOfRange(args, j, indexLimit), null, null, null);
			}
			finally
			{
				if (c != null)
				{
					while (c.moveToNext())
					{
						primaryKeys.add(c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID)));
					}

					c.close();
					c = null;
				}
				SQLiteDatabase.releaseMemory();
			}

			j = indexLimit;
		}

		if (primaryKeys != null && primaryKeys.size() > 0)
		{
			String[] tables = new String[HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT];
			tables[0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
			int remainingTableCount = HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT - 1;

			for (int i = 0; i < remainingTableCount; i++)
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
					int indexLimit = j + count;

					String[] ids = Arrays.copyOfRange(groupIds, j, indexLimit);

					for (int i = 0; i < HikeStickerSearchBaseConstants.INITIAL_FTS_TABLE_COUNT; i++)
					{
						mDb.delete(
								tables[i],
								HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START
										+ StickerSearchUtility.getSQLiteDatabaseMultipleMatchSyntax(ids) + HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null);
						SQLiteDatabase.releaseMemory();
					}

					mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_IN_OPEN
							+ StickerSearchUtility.getSQLiteDatabaseMultipleParameterSyntax(count) + HikeStickerSearchBaseConstants.SYNTAX_END, ids);

					j = indexLimit;
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
			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
					+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { stickerCode }, null, null, null);
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
					mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID
							+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { String.valueOf(rowIdList.get(i)) });
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
		Logger.i(TAG, "startRebalancing()");

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
				c = null;
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
						c = null;
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
							c = null;
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
								Logger.v(TAG_REBALANCING, "Deleting tag: " + tagList.get(i) + " w.r.t. sticker: " + stickerList.get(i));

								char[] array = tagList.get(i).toCharArray();
								table = array[0] > 'Z' || array[0] < 'A' ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
										: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + array[0];

								mDb.delete(table,
										HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + String.valueOf(rowIdList.get(i))
												+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null);
								mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID
										+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { String.valueOf(rowIdList.get(i++)) });
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

								Logger.v(TAG_REBALANCING, "Aging tag: " + tagList.get(i) + " with age = " + updatedAge + " w.r.t. sticker: " + stickerList.get(i));

								ContentValues cv = new ContentValues();
								cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, updatedAge);
								mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID
										+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { String.valueOf(rowIdList.get(i++)) });
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