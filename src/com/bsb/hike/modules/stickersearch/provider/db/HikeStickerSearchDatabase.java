/**
 * File   : HikeStickerSearchDatabase.java
 * Content: It contains all operations regarding creating/upgrading/inserting in/reading/removing to/from Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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

	private static final String TAG_REBALANCING = "HSSDB$Rebalancing";

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

			sHikeStickerSearchDatabase.sMaxSelectionCount = (int) (StickerSearchConstants.MAXIMUM_SEARCH_COUNT * StickerSearchConstants.RATIO_MAXIMUM_SELECTION_COUNT);
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

	private ArrayList<StickerDataContainer> searchIntoPrimaryTable(String matchKey, String[] referenceArgs, boolean isExactMatchNeeded)
	{
		ArrayList<StickerDataContainer> list = null;

		if ((referenceArgs != null) && (referenceArgs.length > 0))
		{
			Cursor c = null;

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID
						+ HikeStickerSearchBaseConstants.SYNTAX_IN_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParameterSyntax(referenceArgs.length)
						+ HikeStickerSearchBaseConstants.SYNTAX_END, referenceArgs, null, null, null);

				int count = (c == null) ? 0 : c.getCount();

				if (count > 0)
				{
					if (count <= sMaxSelectionCount)
					{
						list = selectTagsForStickers(matchKey, isExactMatchNeeded, c);
					}
					else
					{
						list = selectTagsForStickersWithLimit(matchKey, isExactMatchNeeded, c);
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
			}
		}

		return list;
	}

	private ArrayList<StickerDataContainer> selectTagsForStickers(String matchKey, boolean isExactMatchNeeded, Cursor c)
	{
		ArrayList<StickerDataContainer> list = new ArrayList<StickerDataContainer>(c.getCount());
		int[] columnIndices = computeColumnIndices(c);

		while (c.moveToNext())
		{
			list.add(buildStickerData(c, columnIndices));
		}

		return list;
	}

	private ArrayList<StickerDataContainer> selectTagsForStickersWithLimit(String matchKey, boolean isExactMatchNeeded, Cursor c)
	{
		ArrayList<StickerDataContainer> list = new ArrayList<StickerDataContainer>(c.getCount());
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
						Logger.i(TAG, "Skipped repeated sticker for tag: " + c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]));
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
				actualTag = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]);
				currentStickerCode = c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE]);

				if (currentStickerCode.equals(previousStickerCode))
				{
					if (tagCountPerSticker >= StickerSearchConstants.MAXIMUM_TAG_SELECTION_PER_STICKER_COUNT)
					{
						Logger.i(TAG, "Skipped repeated sticker for tag: " + actualTag);
					}
					else
					{
						firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

						if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
						{
							if (actualTag.substring(0, firstWordLimitIndexInTag).equals(matchKey))
							{
								list.add(buildStickerData(c, columnIndices));
								tagCountPerSticker++;
							}
							else
							{
								Logger.i(TAG, "Rejected sticker due to different first word in tag: " + actualTag);
							}
						}
						else if (firstWordLimitIndexInTag == -1)
						{
							if (actualTag.equals(matchKey))
							{
								list.add(buildStickerData(c, columnIndices));
								tagCountPerSticker++;
							}
							else
							{
								Logger.i(TAG, "Rejected sticker due to different first word in tag: " + actualTag);
							}
						}
						else
						{
							Logger.i(TAG, "Rejected sticker due to error in fetching first word in tag: " + actualTag);
						}
					}
				}
				else
				{
					firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

					if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
					{
						if (actualTag.substring(0, firstWordLimitIndexInTag).equals(matchKey))
						{
							list.add(buildStickerData(c, columnIndices));
							tagCountPerSticker = 1;
							previousStickerCode = currentStickerCode;
						}
						else
						{
							Logger.i(TAG, "Rejected sticker due to different first word in tag: " + actualTag);
						}
					}
					else if (firstWordLimitIndexInTag == -1)
					{
						if (actualTag.equals(matchKey))
						{
							list.add(buildStickerData(c, columnIndices));
							tagCountPerSticker = 1;
							previousStickerCode = currentStickerCode;
						}
						else
						{
							Logger.i(TAG, "Rejected sticker due to different first word in tag: " + actualTag);
						}
					}
					else
					{
						Logger.i(TAG, "Rejected sticker due to error in fetching first word in tag: " + actualTag);
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

	public ArrayList<StickerDataContainer> searchIntoFTSAndFindStickerList(String matchKey, boolean isExactMatchNeeded)
	{
		ArrayList<StickerDataContainer> result = null;
		ArrayList<String> tempReferences = null;
		String[] rows = null;
		Cursor c = null;
		int count = 0;

		try
		{
			char[] array = matchKey.toCharArray();
			String table = (array[0] > 'Z' || array[0] < 'A') ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH : HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
					+ array[0];
			Logger.i(TAG, "Searching \"" + matchKey + "\" in " + table + ", exact search: " + isExactMatchNeeded);

			if (isExactMatchNeeded)
			{
				c = mDb.query(table, null, HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + matchKey
						+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null, null, null, null);
			}
			else
			{
				c = mDb.query(table, null, HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + matchKey
						+ HikeStickerSearchBaseConstants.SYNTAX_PREDICATE_MATCH_END, null, null, null, null);
			}

			count = ((c == null) ? 0 : c.getCount());
			if (count > 0)
			{
				tempReferences = selectReferencesForTags(matchKey, isExactMatchNeeded, c);
			}
		}
		catch (SQLiteException e)
		{
			Logger.e(TAG, "Exception while searching \"" + matchKey + "\"", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		if (count > 0)
		{
			if (count > StickerSearchConstants.MAXIMUM_SEARCH_COUNT)
			{
				Collections.shuffle(tempReferences, mRandom);
				count = StickerSearchConstants.MAXIMUM_SEARCH_COUNT;
			}

			rows = new String[count];

			for (int i = 0; i < count; i++)
			{
				rows[i] = tempReferences.get(i);
			}

			result = searchIntoPrimaryTable(matchKey, rows, isExactMatchNeeded);
			SQLiteDatabase.releaseMemory();
		}

		return result;
	}

	private ArrayList<String> selectReferencesForTags(String matchKey, boolean isExactMatchNeeded, Cursor c)
	{
		int count = (c == null) ? 0 : c.getCount();
		ArrayList<String> tempReferences = null;

		if (count > 0)
		{
			tempReferences = new ArrayList<String>(count);

			int tagIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE);
			int referenceIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID);

			if (matchKey.contains(StickerSearchConstants.STRING_SPACE) || !isExactMatchNeeded || (count < StickerSearchConstants.MAXIMUM_SEARCH_COUNT))
			{
				while (c.moveToNext())
				{
					tempReferences.add(c.getString(referenceIndex));
				}
			}
			else
			{
				String actualTag;
				String firstWordInTag;
				int firstWordLimitIndexInTag;

				while (c.moveToNext())
				{
					actualTag = c.getString(tagIndex);
					firstWordLimitIndexInTag = actualTag.indexOf(StickerSearchConstants.STRING_SPACE);

					if ((firstWordLimitIndexInTag > 0) && (firstWordLimitIndexInTag < actualTag.length()))
					{
						firstWordInTag = actualTag.substring(0, firstWordLimitIndexInTag);

						if (firstWordInTag.startsWith(matchKey))
						{
							tempReferences.add(c.getString(referenceIndex));
						}
						else
						{
							Logger.i(TAG, "Rejected search result due to different first word in tag: " + actualTag);
						}
					}
					else if (firstWordLimitIndexInTag == -1)
					{
						tempReferences.add(c.getString(referenceIndex));
					}
					else
					{
						Logger.i(TAG, "Rejected search result due to error in fetching first word in tag: " + actualTag);
					}
				}
			}
		}

		return tempReferences;
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
		Logger.i(TAG, "analyseMessageSent(" + prevText + ", " + sticker + ", " + nextText + ")");

		if (sticker == null)
		{
			return;
		}

		String stickerCode = StickerManager.getInstance().getStickerSetString(sticker);
		Cursor c = null;
		int totalCount = 0;
		String[] rowIds = null;
		String compositeFrequency = null;

		try
		{
			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID,
					HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY }, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
					+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { stickerCode }, null, null, null);

			totalCount = (c == null) ? 0 : c.getCount();
			if (totalCount > 0)
			{
				rowIds = new String[totalCount];

				int rowIdIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID);
				int i = 0;

				if (c.moveToNext())
				{
					rowIds[i++] = c.getString(rowIdIndex);

					ArrayList<Float> slottedFrequencies = StickerSearchUtility.getIndividualNumericValues(
							c.getString(c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY)),
							StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT, Float.class);

					slottedFrequencies.set(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING,
							(slottedFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING) + 1.00f));

					compositeFrequency = StickerSearchUtility.getCompositeNumericValues(slottedFrequencies);
				}

				while (c.moveToNext())
				{
					rowIds[i++] = c.getString(rowIdIndex);
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

		if (totalCount > 0)
		{
			try
			{
				mDb.beginTransaction();

				ContentValues cv = new ContentValues();
				cv.put(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY, compositeFrequency);
				cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, 0);

				int remainingCount = totalCount;
				int currentCount;
				int indexLimit;

				for (int i = 0; i < totalCount; i++)
				{
					currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
							: remainingCount;
					indexLimit = i + currentCount;

					mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID
							+ HikeStickerSearchBaseConstants.SYNTAX_IN_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParameterSyntax(currentCount)
							+ HikeStickerSearchBaseConstants.SYNTAX_END, Arrays.copyOfRange(rowIds, i, indexLimit));

					remainingCount -= currentCount;
					i = indexLimit;
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

	public boolean summarizeAndDoRebalancing()
	{
		boolean isTestModeOn = StickerSearchUtility.isTestModeForSRModule();

		int MAXIMUM_PRIMARY_TABLE_CAPACITY = isTestModeOn ? HikeStickerSearchBaseConstants.TEST_MAXIMUM_PRIMARY_TABLE_CAPACITY
				: HikeStickerSearchBaseConstants.MAXIMUM_PRIMARY_TABLE_CAPACITY;
		long TIME_WINDOW_TRENDING_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_TRENDING_SUMMERY : StickerSearchConstants.TIME_WINDOW_TRENDING_SUMMERY;
		long TIME_WINDOW_LOCAL_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_LOCAL_SUMMERY : StickerSearchConstants.TIME_WINDOW_LOCAL_SUMMERY;
		long TIME_WINDOW_GLOBAL_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_GLOBAL_SUMMERY : StickerSearchConstants.TIME_WINDOW_GLOBAL_SUMMERY;

		long currentTime = System.currentTimeMillis();
		Date date = new Date();

		Cursor c = null;
		long totalTagCount = 0;

		try
		{
			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID }, null, null, null, null,
					HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_DESCENDING, String.valueOf(1));
		}
		finally
		{
			if (c != null)
			{
				if (c.moveToFirst())
				{
					totalTagCount = c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID));
				}

				c.close();
				c = null;
			}
			SQLiteDatabase.releaseMemory();
		}

		Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), total tags entered = " + totalTagCount + " till date:: " + date.toString());

		if (totalTagCount > 0)
		{
			long previousTime = HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_SUMMERIZATION_TIME, currentTime);
			long intervalFromPreviousSummeryTime = currentTime - previousTime;
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Current time = " + currentTime + " milliseconds.");
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Previous time = " + previousTime + " milliseconds.");
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Time difference from previous operation = " + intervalFromPreviousSummeryTime + " milliseconds.");

			boolean isGlobalSummeryTime = intervalFromPreviousSummeryTime >= TIME_WINDOW_GLOBAL_SUMMERY;
			boolean isLocalSummeryTime = !isGlobalSummeryTime && (intervalFromPreviousSummeryTime >= TIME_WINDOW_LOCAL_SUMMERY);
			boolean isTrendingSummeryTime = !isLocalSummeryTime && (intervalFromPreviousSummeryTime >= TIME_WINDOW_TRENDING_SUMMERY);

			ArrayList<String> rowsIds = new ArrayList<String>();
			ArrayList<Character> virtualTableInfo = new ArrayList<Character>();
			ArrayList<Float> trendingFrequencies = new ArrayList<Float>();
			ArrayList<Float> localFrequencies = new ArrayList<Float>();
			ArrayList<Float> globalFrequencies = new ArrayList<Float>();
			ArrayList<Integer> ages = new ArrayList<Integer>();

			ArrayList<Float> slottedFrequenciesPerSticker;
			int existingTagCountPerBlock;
			long remainingCount = totalTagCount;
			int currentCount;

			// Fetch age and frequency of each sticker-tag data from primary table
			for (long i = 0; i < totalTagCount;)
			{
				currentCount = (remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0 ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
						: (int) remainingCount;
				i = i + currentCount;

				try
				{
					c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID,
							HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE,
							HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY }, HikeStickerSearchBaseConstants.UNIQUE_ID
							+ HikeStickerSearchBaseConstants.SYNTAX_LESS_THAN_OR_EQUALS + i, null, null, null, HikeStickerSearchBaseConstants.UNIQUE_ID
							+ HikeStickerSearchBaseConstants.SYNTAX_DESCENDING, String.valueOf(currentCount));

					existingTagCountPerBlock = (c == null) ? 0 : c.getCount();

					if (existingTagCountPerBlock > 0)
					{
						int rowIdIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID);
						int ageIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE);
						int compositeFrequencyIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY);
						int tagIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE);

						while (c.moveToNext())
						{
							rowsIds.add(c.getString(rowIdIndex));
							virtualTableInfo.add(c.getString(tagIndex).charAt(0));
							ages.add(c.getInt(ageIndex));

							slottedFrequenciesPerSticker = StickerSearchUtility.getIndividualNumericValues(c.getString(compositeFrequencyIndex),
									StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT, Float.class);

							trendingFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING));
							trendingFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL));
							trendingFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL));

							slottedFrequenciesPerSticker.clear();
							slottedFrequenciesPerSticker = null;
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

			int existingTotalTagCount = rowsIds.size();

			// Do summarization first
			if (isTrendingSummeryTime)
			{
				float maxTrendingFrequency = Collections.max(trendingFrequencies);

				if (maxTrendingFrequency > StickerSearchConstants.MAXIMUM_FREQUENCY_TRENDING)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						trendingFrequencies.set(i, (trendingFrequencies.get(i) * StickerSearchConstants.MAXIMUM_FREQUENCY_TRENDING / maxTrendingFrequency));
					}
				}

				float ratioToCarryForwardTrendingTowardsLocal = StickerSearchConstants.TIME_WINDOW_TRENDING_CARRY_ON / StickerSearchConstants.TIME_WINDOW_LOCAL_CARRY_ON;

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					localFrequencies.set(i, (localFrequencies.get(i) + trendingFrequencies.get(i) * ratioToCarryForwardTrendingTowardsLocal));
					ages.set(i, (ages.get(i) + 1));
				}

				Logger.i(TAG_REBALANCING, "Trending summerization is done today at time:: " + date.toString());
			}
			else if (isLocalSummeryTime)
			{
				float maxLocalFrequency = Collections.max(localFrequencies);

				if (maxLocalFrequency > StickerSearchConstants.MAXIMUM_FREQUENCY_LOCAL)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						localFrequencies.set(i, (localFrequencies.get(i) * StickerSearchConstants.MAXIMUM_FREQUENCY_LOCAL / maxLocalFrequency));
					}
				}

				float ratioToCarryForwardLocalTowardsGlobal = StickerSearchConstants.TIME_WINDOW_LOCAL_CARRY_ON / StickerSearchConstants.TIME_WINDOW_GLOBAL_CARRY_ON;

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					globalFrequencies.set(i, (globalFrequencies.get(i) + localFrequencies.get(i) * ratioToCarryForwardLocalTowardsGlobal));
					ages.set(i, (ages.get(i) + 1));
				}

				Logger.i(TAG_REBALANCING, "Local summerization is done today at time:: " + date.toString());
			}
			else if (isGlobalSummeryTime)
			{
				float maxGlobalFrequency = Collections.max(globalFrequencies);

				if (maxGlobalFrequency > StickerSearchConstants.MAXIMUM_FREQUENCY_GLOBAL)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						globalFrequencies.set(i, (globalFrequencies.get(i) * StickerSearchConstants.MAXIMUM_FREQUENCY_GLOBAL / maxGlobalFrequency));
					}
				}

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					ages.set(i, (ages.get(i) + 1));
				}

				Logger.i(TAG_REBALANCING, "Global summerization is done today at time:: " + date.toString());
			}
			else
			{
				Logger.i(TAG_REBALANCING, "Rebalancing and summerization is not required today at time:: " + date.toString());

				rowsIds.clear();
				virtualTableInfo.clear();
				ages.clear();
				trendingFrequencies.clear();
				localFrequencies.clear();
				globalFrequencies.clear();
				existingTotalTagCount = 0;
			}

			// Do re-balancing with updated summarized data, if required to do so
			int cuttOffTagDataSize = (int) (MAXIMUM_PRIMARY_TABLE_CAPACITY * HikeStickerSearchBaseConstants.THRESHOLD_PRIMARY_TABLE_CAPACITY);

			if (existingTotalTagCount < cuttOffTagDataSize)
			{
				// Check internal memory issues
				File file = mContext.getDatabasePath(HikeStickerSearchBaseConstants.DATABASE_HIKE_STICKER_SEARCH);
				long dbSize = file.length();
				long availableSizeInBytes = file.getFreeSpace();
				long possibleDbExpansionSize = (long) (dbSize * HikeStickerSearchBaseConstants.THRESHOLD_DATABASE_EXPANSION_RATIO);

				if (availableSizeInBytes < possibleDbExpansionSize)
				{
					Logger.w(TAG_REBALANCING, "Internal memory seems to be getting full in few days. Let's shrink sticker search database.");
					cuttOffTagDataSize = (int) (existingTotalTagCount * HikeStickerSearchBaseConstants.THRESHOLD_DATABASE_FORCED_SHRINK_RATIO);
				}
			}

			ArrayList<Integer> eliminatedIndices = new ArrayList<Integer>();

			// Check if re-balancing is still required after several trails
			if (existingTotalTagCount >= cuttOffTagDataSize)
			{
				Logger.i(TAG_REBALANCING, "Global rebalancing is triggered today at time:: " + new Date().toString());

				// Calculate overall frequency
				ArrayList<Float> overallFrequencies = new ArrayList<Float>(existingTotalTagCount);

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					overallFrequencies.add(trendingFrequencies.get(i) + localFrequencies.get(i) + globalFrequencies.get(i));
				}

				int oldestAge;
				int retainedCount = existingTotalTagCount;
				ArrayList<Integer> eliminatingIndices = new ArrayList<Integer>();
				ArrayList<Float> eliminatingFrequencies = new ArrayList<Float>();
				ArrayList<Float> tempFrequencies = new ArrayList<Float>();
				int eliminatingCount;

				// Determine, which id's are needed to delete
				while (retainedCount >= cuttOffTagDataSize)
				{
					oldestAge = Collections.max(ages);

					// Delete based on age w.r.t. frequency in older to newer order
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						if (ages.get(i) == oldestAge)
						{
							tempFrequencies.add(overallFrequencies.get(i));
							eliminatingFrequencies.add(overallFrequencies.get(i));
							eliminatingIndices.add(i);
						}
					}

					eliminatingCount = tempFrequencies.size();
					if (eliminatingCount > 0)
					{
						Collections.sort(tempFrequencies);
						int middleIndex = eliminatingCount / 2;
						float medianFrequency = ((eliminatingCount % 2) == 0) ? (tempFrequencies.get(middleIndex - 1) + tempFrequencies.get(middleIndex)) / 2 : tempFrequencies
								.get(middleIndex);

						for (int j = 0; (j < eliminatingCount) && (retainedCount >= cuttOffTagDataSize); j++)
						{
							if (eliminatingFrequencies.get(j) <= medianFrequency)
							{
								eliminatedIndices.add(eliminatingIndices.get(j));
								retainedCount--;
							}
						}

						tempFrequencies.clear();
						eliminatingFrequencies.clear();
						eliminatingIndices.clear();
					}

				}
			}

			ages.clear();

			int totalDeletingReferenceCount = eliminatedIndices.size();

			// Delete rows, if needed
			if (totalDeletingReferenceCount > 0)
			{
				Logger.i(TAG_REBALANCING, "Global elimination is triggered today at time:: " + new Date().toString());
				String table;

				try
				{
					mDb.beginTransaction();

					int deletingIndex;
					String rowId;
					char virtualTableSuffix;

					for (int i = 0; i < totalDeletingReferenceCount; i++)
					{
						deletingIndex = eliminatedIndices.get(i);
						rowId = rowsIds.get(deletingIndex);
						virtualTableSuffix = virtualTableInfo.get(deletingIndex);

						Logger.v(TAG_REBALANCING, "Deleting row id: " + rowId + " w.r.t. virtual table: " + virtualTableSuffix);

						table = (virtualTableSuffix > 'Z' || virtualTableSuffix < 'A') ? HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH
								: HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + virtualTableSuffix;

						mDb.delete(table, HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + rowId
								+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null);

						mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID
								+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { rowId });

						rowsIds.set(deletingIndex, null);
						trendingFrequencies.set(deletingIndex, null);
						localFrequencies.set(deletingIndex, null);
						globalFrequencies.set(deletingIndex, null);
					}

					mDb.setTransactionSuccessful();
				}
				finally
				{
					mDb.endTransaction();
					SQLiteDatabase.releaseMemory();

					virtualTableInfo.clear();
					eliminatedIndices.clear();
				}

				// Update remaining data in primary table
				for (int i = 0; i < totalDeletingReferenceCount; i++)
				{
					rowsIds.remove(null);
					trendingFrequencies.remove(null);
					localFrequencies.remove(null);
					globalFrequencies.remove(null);
				}

				Logger.i(TAG_REBALANCING, "Global elimination data removal size = " + totalDeletingReferenceCount);
				Logger.i(TAG_REBALANCING, "Global elimination is done today at time:: " + date.toString());
			}

			// Update rows, if needed
			Logger.i(TAG_REBALANCING, "Global summarized data update is triggered today at time:: " + date.toString());
			int retainedDataCount = rowsIds.size();

			try
			{
				mDb.beginTransaction();

				ArrayList<Float> frequencyListPerStciker = new ArrayList<Float>(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT);
				StringBuilder outputBuilder = new StringBuilder();

				for (int i = 0; i < retainedDataCount; i++)
				{
					frequencyListPerStciker.add(trendingFrequencies.get(i));
					frequencyListPerStciker.add(localFrequencies.get(i));
					frequencyListPerStciker.add(globalFrequencies.get(i));

					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY, StickerSearchUtility.getCompositeNumericValues(outputBuilder, frequencyListPerStciker));
					frequencyListPerStciker.clear();

					mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID
							+ HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER, new String[] { rowsIds.get(i) });
				}

				mDb.setTransactionSuccessful();
			}
			finally
			{
				mDb.endTransaction();
				SQLiteDatabase.releaseMemory();

				Logger.i(TAG_REBALANCING, "Global summarized data update size = " + retainedDataCount);
				Logger.i(TAG_REBALANCING, "Global summarized data update is done today at time:: " + new Date().toString());
			}

			rowsIds.clear();
			trendingFrequencies.clear();
			localFrequencies.clear();
			globalFrequencies.clear();
		}
		else
		{
			Logger.i(TAG_REBALANCING, "Primary table is empty at time:: " + new Date().toString());
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_SUMMERIZATION_TIME, currentTime);

		return true;
	}
}