/**
 * File   : HikeStickerSearchDatabase.java
 * Content: It contains all operations regarding creating/upgrading/inserting in/reading/removing to/from Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DatabaseErrorHandlers.CustomDatabaseErrorHandler;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.StickerAppositeDataContainer;
import com.bsb.hike.modules.stickersearch.datamodel.StickerTagDataContainer;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExecutionDurationLogger;

import java.io.File;
import java.util.*;

public class HikeStickerSearchDatabase extends SQLiteOpenHelper
{
	public static final String TAG = HikeStickerSearchDatabase.class.getSimpleName();

	public static final String TAG_UPGRADE = "HSSDB$UpgradeOperation";

	public static final String TAG_INSERTION = "HSSDB$InsertOperation";

	public static final String TAG_REBALANCING = "HSSDB$RebalancingOperation";

	private volatile int MAXIMUM_SELECTION_COUNT_PER_SEARCH;

	private volatile int MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER;

	private volatile Random mRandom;

	private volatile Context mContext;

	private volatile SQLiteDatabase mDb;

	private volatile String mExistingVirtualTablesList;

	private volatile HashMap<Character, Boolean> mExistingVirtualTableMap;

	private static volatile HikeStickerSearchDatabase sHikeStickerSearchDatabase;

	private static final Object sDatabaseLock = new Object();

	private static long sInsertionTimePerSession = 0;

	private static long sPTInsertionTimePerSession = 0;

	private HikeStickerSearchDatabase(Context context)
	{
		super(context, HikeStickerSearchBaseConstants.DATABASE_HIKE_STICKER_SEARCH, null, HikeStickerSearchBaseConstants.STICKERS_SEARCH_DATABASE_VERSION, new CustomDatabaseErrorHandler());

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

			HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);

			sHikeStickerSearchDatabase.MAXIMUM_SELECTION_COUNT_PER_SEARCH = (int) (StickerSearchConstants.MAXIMUM_SEARCH_COUNT * stickerDataSharedPref.getData(
					HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_RATIO_PER_SEARCH, StickerSearchConstants.RATIO_MAXIMUM_SELECTION_COUNT));

			sHikeStickerSearchDatabase.MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_PER_STICKER,
					StickerSearchConstants.MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER);

			sHikeStickerSearchDatabase.mExistingVirtualTablesList = stickerDataSharedPref.getData(HikeStickerSearchBaseConstants.KEY_PREF_STICKER_SEARCH_VT_TABLES_LIST,
					HikeStickerSearchBaseConstants.DEFAULT_VT_TABLE_LIST);

			sHikeStickerSearchDatabase.loadTableMap();
		}
	}

	/* Get instance of database from outside of this class */
	public static HikeStickerSearchDatabase getInstance()
	{
		if ((sHikeStickerSearchDatabase == null) || (sHikeStickerSearchDatabase.mDb == null) || (sHikeStickerSearchDatabase.mContext == null)
				|| (sHikeStickerSearchDatabase.mRandom == null) || (sHikeStickerSearchDatabase.mExistingVirtualTablesList == null))
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

		long operationStartTime = System.currentTimeMillis();

		if (oldVersion < HikeStickerSearchBaseConstants.VERSION_STICKER_REGIONAL_TAG_MAPPING_ADDED)
		{
			String sql = "ALTER TABLE " + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + " ADD COLUMN " + HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE
					+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST;
			db.execSQL(sql);

			sql = "ALTER TABLE " + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + " ADD COLUMN " + HikeStickerSearchBaseConstants.STICKER_TAG_KEYBOARD_ISO
					+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + " DEFAULT " + HikeStickerSearchBaseConstants.DEFAULT_STICKER_TAG_SCRIPT_ISO_CODE;
			db.execSQL(sql);

			if (oldVersion >= HikeStickerSearchBaseConstants.VERSION_STICKER_TAG_MAPPING_INDEX_ADDED)
			{
				// Drop older index on table: TABLE_STICKER_TAG_MAPPING for 2 columns 'Tag Word/ Phrase' and 'Sticker Information' together (as described in onCreate())
				sql = "DROP INDEX " + HikeStickerSearchBaseConstants.STICKER_TAG_MAPPING_INDEX;
				db.execSQL(sql);
			}

			// Create index on table: TABLE_STICKER_TAG_MAPPING for 3 columns 'Tag Word/ Phrase', 'Sticker Information' and Tag language' together (as described in onCreate())
			sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_INDEX + HikeStickerSearchBaseConstants.STICKER_TAG_MAPPING_INDEX + HikeStickerSearchBaseConstants.SYNTAX_ON
					+ HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN
					+ HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE + HikeStickerSearchBaseConstants.SYNTAX_NEXT + HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE
					+ HikeStickerSearchBaseConstants.SYNTAX_NEXT + HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;
			db.execSQL(sql);
		}

		Logger.i(TAG_UPGRADE,
				"Time taken in db upgrade = " + Utils.getExecutionTimeLog(operationStartTime, System.currentTimeMillis(), ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));
	}

	/* Add tables which are either fixed or default (virtual table) */
	private void createFixedTables(SQLiteDatabase db)
	{
		Logger.i(TAG, "createFixedTables(" + db + ")");

		// Create fixed table: TABLE_STICKER_TAG_ENTITY
		// Primary key : Integer [Compulsory]
		// Name of Entity : String [Compulsory], eg. InitMarker, ContactNumber, GrouId, ChatStory, Region/Language, State etc.
		// Type of Entity : Integer [Compulsory], Recognize to know what kind of entity is in above examples
		// Qualified Data : String [Optional], Data of entity, which can be directly 'imposed over'/'defined for' client user
		// Unqualified data : String [Optional], Data of entity, which can be used relatively to determine order of probability distribution
		String sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_ENTITY
				+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_PRIMARY_KEY
				+ HikeStickerSearchBaseConstants.ENTITY_NAME + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.ENTITY_TYPE
				+ HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT + HikeStickerSearchBaseConstants.ENTITY_QUALIFIED_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.ENTITY_UNQUALIFIED_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;
		db.execSQL(sql);

		// Create fixed table: TABLE_STICKER_PACK_CATEGORY_HISTORY
		// Category Id : String [Compulsory]
		// Chat Story : String [Compulsory], IDs of associated stories put from TABLE_STICKER_TAG_ENTITY
		// Sticker History : String [Compulsory], History of each sticker falling under given category
		// Overall History : String [Compulsory], History of given category w.r.t. its own
		sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_PACK_CATEGORY_HISTORY
				+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.CATEGORY_ID + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.CATEGORY_CHAT_STORIES + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.CATEGORY_STICKERS_HISTORY
				+ HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT + HikeStickerSearchBaseConstants.CATEGORY_OVERALL_HISTORY + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST
				+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;
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
		// Language: String [Compulsory], Actual language of the tag irrespective of its script in which tag is written
		// Script: String [Compulsory], Script of the tag, in which it is written
		sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_TABLE + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN
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
				+ HikeStickerSearchBaseConstants.SYNTAX_INTEGER_NEXT + HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE + HikeStickerSearchBaseConstants.SYNTAX_TEXT_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_TAG_KEYBOARD_ISO + HikeStickerSearchBaseConstants.SYNTAX_TEXT_LAST + " DEFAULT "
				+ HikeStickerSearchBaseConstants.DEFAULT_STICKER_TAG_SCRIPT_ISO_CODE + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;
		db.execSQL(sql);

		// Create index on fixed table: TABLE_STICKER_TAG_MAPPING for 3 columns 'Tag Word/ Phrase', 'Sticker Information' and Tag language' together (as described above)
		sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_INDEX + HikeStickerSearchBaseConstants.STICKER_TAG_MAPPING_INDEX + HikeStickerSearchBaseConstants.SYNTAX_ON
				+ HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE
				+ HikeStickerSearchBaseConstants.SYNTAX_NEXT + HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + HikeStickerSearchBaseConstants.SYNTAX_NEXT
				+ HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;
		db.execSQL(sql);
	}

	/* Prepare search engine database */
	public void prepare()
	{
		Logger.i(TAG, "prepare()");

		Logger.d(TAG, "Starting population first time...");
		try
		{
			mDb.beginTransaction();

			int initialTableCount = (mExistingVirtualTablesList == null) ? 0 : mExistingVirtualTablesList.length();

			for (int i = 0; i < initialTableCount; i++)
			{
				createVirtualTable(getVirtualTableNameForChar(mExistingVirtualTablesList.charAt(i)));
			}

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
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
	private void createVirtualTable(String tableName)
	{
		String sql;

		if (!Utils.isTableExists(mDb, tableName))
		{
			Logger.v(TAG, "Creating virtual table with name: " + tableName);

			sql = HikeStickerSearchBaseConstants.SYNTAX_CREATE_VTABLE + tableName + HikeStickerSearchBaseConstants.SYNTAX_FTS_VERSION_4
					+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.TAG_REAL_PHRASE + HikeStickerSearchBaseConstants.SYNTAX_NEXT
					+ HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_NEXT + HikeStickerSearchBaseConstants.SYNTAX_FOREIGN_KEY
					+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE
					+ HikeStickerSearchBaseConstants.SYNTAX_FOREIGN_REF + HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING
					+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE
					+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE;

			mDb.execSQL(sql);
		}

	}

	/* Setup virtual table for given prefix, if does not exist */
	private String setupVirtualTableForFirstChar(Character prefix)
	{
		String tableName = getVirtualTableNameForChar(prefix);

		createVirtualTable(tableName);

		if (!tableForCharExists(prefix))
		{
			mExistingVirtualTableMap.put(prefix, true);
			mExistingVirtualTablesList = mExistingVirtualTablesList + prefix;
			HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA).saveData(
					HikeStickerSearchBaseConstants.KEY_PREF_STICKER_SEARCH_VT_TABLES_LIST, mExistingVirtualTablesList);
		}

		return tableName;
	}

	private void loadTableMap()
	{
		mExistingVirtualTableMap = new HashMap<Character, Boolean>();

		for (int i = 0; i < mExistingVirtualTablesList.length(); i++)
		{
			mExistingVirtualTableMap.put(mExistingVirtualTablesList.charAt(i), true);
		}
	}

	private boolean tableForCharExists(Character c)
	{
		if (mExistingVirtualTableMap.get(c) == null)
		{
			return false;
		}

		return mExistingVirtualTableMap.get(c).booleanValue();
	}

	private String getVirtualTableNameForChar(Character suffix)
	{
		if (suffix == StickerSearchConstants.CHAR_EMPTY)
		{
			return HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
		}
		else
		{
			return HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + suffix;
		}
	}

	/* Do not change the order of deletion as per dependency of foreign keys */
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

	/* Delete fts data in virtual tables */
	private void deleteSearchData()
	{
		Logger.i(TAG, "deleteSearchData()");

		// Delete dynamically added tables
		String table;

		int remainingCount = mExistingVirtualTablesList.length();
		for (int i = 0; i < remainingCount; i++)
		{
			table = getVirtualTableNameForChar(mExistingVirtualTablesList.charAt(i));

			if (Utils.isTableExists(mDb, table))
			{
				Logger.v(TAG, "Deleting virtual table with name: " + table);
				mDb.delete(table, null, null);

				SQLiteDatabase.releaseMemory();
			}
		}

		HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA).removeData(
				HikeStickerSearchBaseConstants.KEY_PREF_STICKER_SEARCH_VT_TABLES_LIST);
	}

	public void insertStickerTagData(Map<String, ArrayList<String>> packStoryData, ArrayList<StickerTagDataContainer> stickersTagData)
	{
		Logger.i(TAG_INSERTION, "insertStickerTagData()");

		if (Utils.isEmpty(stickersTagData))
		{
			Logger.wtf(TAG_INSERTION, "insertStickerTagData(), Invalid tag data insertion request.");
			return;
		}

		long requestStartTime = System.currentTimeMillis();
		long operationStartTime = requestStartTime;

		// Tag data conformity check operation
		ArrayList<Integer> validStickerTagDataIndices = new ArrayList<Integer>();
		ArrayList<String> stickerCodeList = new ArrayList<String>();
		ArrayList<String> allTagList = new ArrayList<String>();
		ArrayList<String> allTagLanguageList = new ArrayList<String>();

		int stickerCount = stickersTagData.size();
		StickerTagDataContainer stickerTagData;
		String stickerCode;
		ArrayList<String> tagListPerSticker = null;
		ArrayList<String> languageListPerSticker = null;
		int tagCountPerSticker;

		for (int i = 0; i < stickerCount; i++)
		{
			stickerTagData = stickersTagData.get(i);

			if (isValidTagData(stickerTagData))
			{
				validStickerTagDataIndices.add(i);

				stickerCode = stickerTagData.getStickerCode();
				tagListPerSticker = stickerTagData.getTagList();
				tagCountPerSticker = tagListPerSticker.size();
				languageListPerSticker = stickerTagData.getLanguageList();

				for (int j = 0; j < tagCountPerSticker; j++)
				{
					stickerCodeList.add(stickerCode);
					allTagList.add(tagListPerSticker.get(j));
					allTagLanguageList.add(languageListPerSticker.get(j));
				}
			}
			else
			{
				Logger.wtf(TAG_INSERTION, "insertStickerTagData(), Wrong data for " + stickerTagData);
			}
		}

		int totalTagsCount = allTagList.size();
		long operationOverTime = System.currentTimeMillis();
		Logger.i(TAG_INSERTION, "insertStickerTagData(), Total tags count (to update/ insert) = " + totalTagsCount);
		Logger.i(
				TAG_INSERTION,
				"Time taken in checking tag data conformity = "
						+ Utils.getExecutionTimeLog(operationStartTime, operationOverTime, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));

		// Tag data building (querying) operation
		Cursor c = null;
		HashMap<String, ContentValues> existingTagData = new HashMap<String, ContentValues>();

		String[] columnsInvolvedInQuery = new String[] { HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE,
				HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE };
		int[] nullCheckIndicatorForColumnsInvolvedInQuery = new int[] { HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK, HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK,
				HikeStickerSearchBaseConstants.SQLITE_NULL_OR_NON_NULL_CHECK };

		String whereConditionToQueryAndUpdate = StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithANDSyntax(columnsInvolvedInQuery,
				nullCheckIndicatorForColumnsInvolvedInQuery);

		int maxRowCountPerQuery = HikeStickerSearchBaseConstants.SQLITE_MAX_LIMIT_VARIABLE_NUMBER / columnsInvolvedInQuery.length;
		long queryOperationStartTime = System.currentTimeMillis();
		int currentCount;

		for (int i = 0, remainingCount = totalTagsCount; remainingCount > 0; remainingCount = (remainingCount - currentCount))
		{
			operationStartTime = System.currentTimeMillis();
			currentCount = ((remainingCount / maxRowCountPerQuery) > 0) ? maxRowCountPerQuery : remainingCount;

			// Build arguments to query
			String[] argsInCondition = new String[currentCount * columnsInvolvedInQuery.length];
			int argIndex;

			for (int j = 0; j < currentCount; j++, i++)
			{
				argIndex = j * columnsInvolvedInQuery.length;

				argsInCondition[argIndex] = allTagList.get(i);
				argsInCondition[argIndex + 1] = stickerCodeList.get(i);
				argsInCondition[argIndex + 2] = allTagLanguageList.get(i);
			}

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null,
						StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithORSyntax(currentCount, whereConditionToQueryAndUpdate), argsInCondition, null, null, null);

				String uniqueKey;

				if ((c != null) && (c.getCount() > 0))
				{
					int tagPhraseIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE);
					int exactnessOrderIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY);
					int momentCodeIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME);
					int stickerCodeIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE);
					int tagLanguageIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE);
					int popularityIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY);
					int availabilityIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_AVAILABILITY);

					while (c.moveToNext())
					{
						ContentValues existingCv = new ContentValues();
						existingCv.put(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY, c.getInt(exactnessOrderIndex));
						existingCv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME, c.getInt(momentCodeIndex));
						existingCv.put(HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY, c.getInt(popularityIndex));
						existingCv.put(HikeStickerSearchBaseConstants.STICKER_AVAILABILITY, c.getInt(availabilityIndex));

						uniqueKey = c.getString(stickerCodeIndex) + StickerSearchConstants.STRING_DELIMITER + c.getString(tagPhraseIndex) + StickerSearchConstants.STRING_DELIMITER
								+ c.getString(tagLanguageIndex);
						existingTagData.put(uniqueKey, existingCv);
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

			operationOverTime = System.currentTimeMillis();
			Logger.i(
					TAG_INSERTION,
					"Time taken in individual query (on group of tags) = "
							+ Utils.getExecutionTimeLog(operationStartTime, operationOverTime, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));
		}

		Logger.d(
				TAG_INSERTION,
				"Time taken in overall tag data query = "
						+ Utils.getExecutionTimeLog(queryOperationStartTime, operationOverTime, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));

		// Tag data setup (update/ insert) operation
		operationStartTime = System.currentTimeMillis();
		int existingTagsCount = existingTagData.size();
		int newTagsCount = totalTagsCount - existingTagsCount;
		ArrayList<String> insertedTags = new ArrayList<String>(newTagsCount);
		ArrayList<Long> insertedRows = new ArrayList<Long>(newTagsCount);
		int newTagsInsertionSucceeded = 0;
		int newTagsInsertionFailed = 0;

		try
		{
			mDb.beginTransaction();

			int stickerCountWithValidData = validStickerTagDataIndices.size();
			int stickerIndex;
			String tag;
			String language;
			String script;
			long rowId;
			String uniqueKey;
			boolean isLanguageUpdateNeeded;

			for (int i = 0; i < stickerCountWithValidData; i++)
			{
				isLanguageUpdateNeeded = false;
				stickerIndex = validStickerTagDataIndices.get(i);
				stickerTagData = stickersTagData.get(stickerIndex);
				stickerCode = stickerTagData.getStickerCode();
				ArrayList<String> stickerTags = stickerTagData.getTagList();
				ArrayList<String> tagLanguages = stickerTagData.getLanguageList();
				ArrayList<String> tagScripts = stickerTagData.getScriptList();
				ArrayList<Integer> tagExactnessPriorities = stickerTagData.getTagExactMatchPriorityList();
				ArrayList<Integer> tagPopularities = stickerTagData.getTagPopularityList();
				int stickerMoment = stickerTagData.getMomentCode();
				int availability = stickerTagData.getStickerAvailabilityStatus() ? HikeStickerSearchBaseConstants.DECISION_STATE_YES
						: HikeStickerSearchBaseConstants.DECISION_STATE_NO;
				int size = stickerTags.size();

				for (int j = 0; j < size; j++)
				{
					tag = stickerTags.get(j);
					language = tagLanguages.get(j);
					script = tagScripts.get(j);

					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.STICKER_EXACTNESS_WITH_TAG_PRIORITY, tagExactnessPriorities.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_TIME, stickerMoment);
					cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_POPULARITY, tagPopularities.get(j));
					cv.put(HikeStickerSearchBaseConstants.STICKER_AVAILABILITY, availability);

					uniqueKey = stickerCode + StickerSearchConstants.STRING_DELIMITER + tag + StickerSearchConstants.STRING_DELIMITER + language;
					ContentValues existingCv = existingTagData.get(uniqueKey);
					// Check for existing row again with language is null, if row with proper language is not available
					if (existingCv == null)
					{
						uniqueKey = stickerCode + StickerSearchConstants.STRING_DELIMITER + tag + StickerSearchConstants.STRING_DELIMITER + null;
						existingCv = existingTagData.get(uniqueKey);
						isLanguageUpdateNeeded = true;
					}

					// Case 1. No row for given sticker and tag was found in database, insert new row
					if (existingCv == null)
					{
						cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE, tag);
						cv.put(HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE, stickerCode);
						cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, 0);
						cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE, language);
						cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_KEYBOARD_ISO, script);

						rowId = mDb.insert(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, cv);

						if (rowId < HikeStickerSearchBaseConstants.SQLITE_FIRST_INTEGER_ROW_ID)
						{
							Logger.e(TAG, "insertStickerTagData(), Error while inserting tag '" + tag + "' into database !!!");
							newTagsInsertionFailed++;
						}
						else
						{
							newTagsInsertionSucceeded++;
							insertedTags.add(tag);
							insertedRows.add(rowId);
						}
					}
					else
					{
						// Case 2. At least one row for given sticker and tag was found in database, update the language data
						if (cv.equals(existingCv) && isLanguageUpdateNeeded)
						{
							cv.clear();
							cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE, language);
							mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, whereConditionToQueryAndUpdate, new String[] { tag, stickerCode, language });
						}
						// Case 3. At least one row for given sticker and tag was found in database with different attributes, update the language data and other attributes too
						else if (!cv.equals(existingCv))
						{
							if (isLanguageUpdateNeeded)
							{
								cv.put(HikeStickerSearchBaseConstants.STICKER_TAG_LANGUAGE, language);
							}
							mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, whereConditionToQueryAndUpdate, new String[] { tag, stickerCode, language });
						}

						// Clear data, which is no longer needed
						existingTagData.remove(stickerCode + StickerSearchConstants.STRING_DELIMITER + tag);
						existingCv.clear();
						existingCv = null;
					}
				}
			}

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
			SQLiteDatabase.releaseMemory();

			// Clear data, which is no longer needed
			existingTagData.clear();
			existingTagData = null;
			validStickerTagDataIndices.clear();
			validStickerTagDataIndices = null;
			stickerCodeList.clear();
			stickerCodeList = null;
			allTagList.clear();
			allTagList = null;
			allTagLanguageList.clear();
			allTagLanguageList = null;
			if (tagListPerSticker != null)
			{
				tagListPerSticker.clear();
				tagListPerSticker = null;
			}
			stickersTagData.clear();
			stickersTagData = null;
		}

		operationOverTime = System.currentTimeMillis();
		Logger.i(
				TAG_INSERTION,
				"Time taken in insertion (into primary table) = "
						+ Utils.getExecutionTimeLog(operationStartTime, operationOverTime, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));

		Logger.v(TAG, "insertStickerTagData(), Existing tags count = " + existingTagsCount + ", New tags count = " + newTagsCount);
		Logger.v(TAG, "insertStickerTagData(), Newly inserted tags count = " + newTagsInsertionSucceeded + ", Newly abandoned tags count = " + newTagsInsertionFailed);

		updatePTWriteTime(operationOverTime - requestStartTime);
		Logger.d(
				TAG_INSERTION,
				"Time taken in insertion for current session (into primary table) = "
						+ Utils.getExecutionTimeLog(0, sPTInsertionTimePerSession, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));

		if (newTagsInsertionSucceeded > 0)
		{
			Logger.v(TAG, "insertStickerTagData(), Newly added tags: " + insertedTags);

			long operationVTStartTime = System.nanoTime();

			insertIntoVirtualTable(insertedTags, insertedRows);

			long operationVTOverTime = System.nanoTime();
			operationOverTime = System.currentTimeMillis();
			Logger.i(
					TAG_INSERTION,
					"Time taken in insertion (into virtual table) = "
							+ Utils.getExecutionTimeLog(operationVTStartTime, operationVTOverTime, ExecutionDurationLogger.PRECISION_UNIT_NANO_SECOND));
		}

		Logger.i(
				TAG_INSERTION,
				"Time taken in overall insertion for current request = "
						+ Utils.getExecutionTimeLog(requestStartTime, operationOverTime, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));

		updateOverallWriteTime(operationOverTime - requestStartTime);
		Logger.d(
				TAG_INSERTION,
				"Time taken in overall insertion for current session = "
						+ Utils.getExecutionTimeLog(0, sInsertionTimePerSession, ExecutionDurationLogger.PRECISION_UNIT_MILLI_SECOND));
	}

	private void updatePTWriteTime(long durationInNanoSeconds)
	{
		if (sPTInsertionTimePerSession == 0)
		{
			Logger.d(TAG_INSERTION, "Initiating current session for insertion...");
		}

		sPTInsertionTimePerSession += durationInNanoSeconds;
	}

	private void updateOverallWriteTime(long durationInNanoSeconds)
	{
		sInsertionTimePerSession += durationInNanoSeconds;
	}

	private boolean isValidTagData(StickerTagDataContainer stickersTagData)
	{
		return (stickersTagData == null) ? false : stickersTagData.isValidData();
	}

	private void insertIntoVirtualTable(ArrayList<String> tags, ArrayList<Long> referenceIds)
	{
		Logger.i(TAG, "insertIntoVirtualTable()");

		int totalCount = tags.size();
		int remainingCount = totalCount;
		int currentCount;
		try
		{
			mDb.beginTransaction();

			for (int i = 0; i < totalCount;)
			{
				currentCount = ((remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0) ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
						: remainingCount;

				for (int j = 0; j < currentCount; j++, i++)
				{
					String tag = tags.get(i);
					Character firstChar = tag.charAt(0);
					if (StickerSearchUtility.isSpecialCharacterForLatin(firstChar) || !Character.isDefined(firstChar))
					{
						firstChar = StickerSearchConstants.CHAR_EMPTY;
					}

					String table;
					if (tableForCharExists(firstChar))
					{
						table = getVirtualTableNameForChar(firstChar);
					}
					else
					{
						table = setupVirtualTableForFirstChar(firstChar);
					}

					ContentValues cv = new ContentValues();
					cv.put(HikeStickerSearchBaseConstants.TAG_REAL_PHRASE, tag);
					cv.put(HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID, referenceIds.get(i));

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

	private ArrayList<StickerAppositeDataContainer> searchIntoPrimaryTable(String matchKey, String[] referenceArgs, boolean isExactMatchNeeded)
	{
		ArrayList<StickerAppositeDataContainer> list = null;

		if ((referenceArgs != null) && (referenceArgs.length > 0))
		{
			Cursor c = null;

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null, HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_IN
						+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParametersSyntax(referenceArgs.length)
						+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE, referenceArgs, null, null, null);

				int count = (c == null) ? 0 : c.getCount();

				if (count > 0)
				{
					if (count <= MAXIMUM_SELECTION_COUNT_PER_SEARCH)
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

	private ArrayList<StickerAppositeDataContainer> selectTagsForStickers(String matchKey, boolean isExactMatchNeeded, Cursor c)
	{
		ArrayList<StickerAppositeDataContainer> list = new ArrayList<StickerAppositeDataContainer>(c.getCount());
		int[] columnIndices = computeColumnIndices(c);

		while (c.moveToNext())
		{
			list.add(buildStickerData(c, columnIndices));
		}

		return list;
	}

	private ArrayList<StickerAppositeDataContainer> selectTagsForStickersWithLimit(String matchKey, boolean isExactMatchNeeded, Cursor c)
	{
		ArrayList<StickerAppositeDataContainer> list = new ArrayList<StickerAppositeDataContainer>(c.getCount());
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
					if (tagCountPerSticker >= MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER)
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
					if (tagCountPerSticker >= MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER)
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

	private StickerAppositeDataContainer buildStickerData(Cursor c, int[] columnIndices)
	{
		StickerAppositeDataContainer stickerAppositeDataContainer;

		if (columnIndices.length == HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_COUNT)
		{
			stickerAppositeDataContainer = new StickerAppositeDataContainer(c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_STICKER_CODE]),
					c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_TAG_PHRASE]),
					c.getString(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_OVERALL_FREQUENCY]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_EXACTNESS_ORDER]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_DATA_MOMENT_CODE]),
					c.getInt(columnIndices[HikeStickerSearchBaseConstants.INDEX_STICKER_AVAILABILITY_STATUS]));
		}
		else
		{
			stickerAppositeDataContainer = null;
		}

		return stickerAppositeDataContainer;
	}

	public ArrayList<StickerAppositeDataContainer> searchIntoFTSAndFindStickerList(String matchKey, boolean isExactMatchNeeded)
	{
		ArrayList<StickerAppositeDataContainer> result = null;
		ArrayList<String> tempReferences = null;
		String[] rows = null;
		Cursor c = null;
		int count = 0;

		try
		{
			Character firstChar = matchKey.charAt(0);
			if (StickerSearchUtility.isSpecialCharacterForLatin(firstChar) || !Character.isDefined(firstChar))
			{
				firstChar = StickerSearchConstants.CHAR_EMPTY;
			}

			// If no tag data is inserted for given character, then return null
			if (!tableForCharExists(firstChar))
			{
				return null;
			}

			String table = getVirtualTableNameForChar(firstChar);
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

		HashSet<String> removingStickerSetInDatabase = new HashSet<String>();
		Cursor c = null;
		try
		{
			String whereConditionToGetAvailableStickers = StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithANDSyntax(
					new String[] { HikeStickerSearchBaseConstants.STICKER_AVAILABILITY }, new int[] { HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK });

			c = mDb.query(true, HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE },
					whereConditionToGetAvailableStickers, new String[] { String.valueOf(HikeStickerSearchBaseConstants.DECISION_STATE_YES) }, null, null, null, null);
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
			int count = ((remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0) ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
					: remainingCount;
			int indexLimit = j + count;

			try
			{
				c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, null,
						HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE + HikeStickerSearchBaseConstants.SYNTAX_IN + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN
								+ StickerSearchUtility.getSQLiteDatabaseMultipleParametersSyntax(count) + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE,
						Arrays.copyOfRange(args, j, indexLimit), null, null, null);
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

		if (!Utils.isEmpty(primaryKeys))
		{
			String[] tables = new String[mExistingVirtualTablesList.length()];
			for (int i = 0; i < mExistingVirtualTablesList.length(); i++)
			{
				tables[i] = getVirtualTableNameForChar(mExistingVirtualTablesList.charAt(i));
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

					for (int i = 0; i < mExistingVirtualTablesList.length(); i++)
					{
						mDb.delete(
								tables[i],
								HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START
										+ StickerSearchUtility.getSQLiteDatabaseMultipleMatchesSyntax(ids) + HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null);
						SQLiteDatabase.releaseMemory();
					}

					mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_IN
							+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParametersSyntax(count)
							+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE, ids);

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
			String whereConditionToUpdate = StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithANDSyntax(
					new String[] { HikeStickerSearchBaseConstants.STICKER_RECOGNIZER_CODE }, new int[] { HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK });

			c = mDb.query(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID,
					HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY }, whereConditionToUpdate, new String[] { stickerCode }, null, null, null);

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
					currentCount = ((remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0) ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
							: remainingCount;
					indexLimit = i + currentCount;

					mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, HikeStickerSearchBaseConstants.UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_IN
							+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN + StickerSearchUtility.getSQLiteDatabaseMultipleParametersSyntax(currentCount)
							+ HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE, Arrays.copyOfRange(rowIds, i, indexLimit));

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
		Date date = new Date();
		long currentTime = System.currentTimeMillis();
		boolean isTestModeOn = StickerSearchUtility.isTestModeForSRModule();

		Logger.i(TAG_REBALANCING,
				"summarizeAndDoRebalancing(), " + (isTestModeOn ? "Test " : StickerSearchConstants.STRING_EMPTY) + "Operation is started today at time:: " + date.toString());

		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);

		int MAXIMUM_PRIMARY_TABLE_CAPACITY = isTestModeOn ? HikeStickerSearchBaseConstants.TEST_MAXIMUM_PRIMARY_TABLE_CAPACITY : stickerDataSharedPref.getData(
				HikeConstants.STICKER_SEARCH_BASE_MAXIMUM_PRIMARY_TABLE_CAPACITY, HikeStickerSearchBaseConstants.MAXIMUM_PRIMARY_TABLE_CAPACITY);

		float THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION = isTestModeOn ? HikeStickerSearchBaseConstants.TEST_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION : stickerDataSharedPref
				.getData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION, HikeStickerSearchBaseConstants.THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION);

		float THRESHOLD_DATABASE_EXPANSION_COEFFICIENT = isTestModeOn ? HikeStickerSearchBaseConstants.TEST_THRESHOLD_DATABASE_EXPANSION_COEFFICIENT : stickerDataSharedPref
				.getData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_EXPANSION_COEFFICIENT, HikeStickerSearchBaseConstants.THRESHOLD_DATABASE_EXPANSION_COEFFICIENT);

		float THRESHOLD_DATABASE_FORCED_SHRINK_COEFFICIENT = isTestModeOn ? HikeStickerSearchBaseConstants.TEST_THRESHOLD_DATABASE_FORCED_SHRINK_COEFFICIENT
				: stickerDataSharedPref.getData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_FORCED_SHRINK_COEFFICIENT,
						HikeStickerSearchBaseConstants.THRESHOLD_DATABASE_FORCED_SHRINK_COEFFICIENT);

		long TIME_WINDOW_TRENDING_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_TRENDING_SUMMERY : stickerDataSharedPref.getData(
				HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_TRENDING, StickerSearchConstants.TIME_WINDOW_TRENDING_SUMMERY);

		long TIME_WINDOW_LOCAL_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_LOCAL_SUMMERY : stickerDataSharedPref.getData(
				HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_LOCAL, StickerSearchConstants.TIME_WINDOW_LOCAL_SUMMERY);

		long TIME_WINDOW_GLOBAL_SUMMERY = isTestModeOn ? StickerSearchConstants.TEST_TIME_WINDOW_GLOBAL_SUMMERY : stickerDataSharedPref.getData(
				HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_GLOBAL, StickerSearchConstants.TIME_WINDOW_GLOBAL_SUMMERY);

		Cursor c = null;
		long totalPossibleTagCount = 0;

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
					totalPossibleTagCount = c.getLong(c.getColumnIndex(HikeStickerSearchBaseConstants.UNIQUE_ID));
				}

				c.close();
				c = null;
			}

			SQLiteDatabase.releaseMemory();
		}

		Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), total tags entered = " + totalPossibleTagCount + " till date:: " + date.toString());

		long previousTrendingTime = HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_TRENDING_SUMMERIZATION_TIME, 0L);
		long previousLocalTime = HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_LOCAL_SUMMERIZATION_TIME, 0L);
		long previousGlobalTime = HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_GLOBAL_SUMMERIZATION_TIME, 0L);
		
		if (totalPossibleTagCount > 0)
		{
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Current time = " + currentTime + " milliseconds.");

			long intervalFromPreviousSummeryTime = currentTime - previousTrendingTime;
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Previous trending summary time = " + previousTrendingTime + " milliseconds.");
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Time distance from previous trending summery operation = " + intervalFromPreviousSummeryTime + " milliseconds.");
			boolean isTrendingSummeryTurn = intervalFromPreviousSummeryTime >= TIME_WINDOW_TRENDING_SUMMERY;

			intervalFromPreviousSummeryTime = currentTime - previousLocalTime;
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Previous local summary time = " + previousLocalTime + " milliseconds.");
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Time distance from previous local summery operation = " + intervalFromPreviousSummeryTime + " milliseconds.");
			boolean isLocalSummeryTurn = intervalFromPreviousSummeryTime >= TIME_WINDOW_LOCAL_SUMMERY;

			intervalFromPreviousSummeryTime = currentTime - previousGlobalTime;
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Previous global summary time = " + previousGlobalTime + " milliseconds.");
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Time distance from previous global summery operation = " + intervalFromPreviousSummeryTime + " milliseconds.");
			boolean isGlobalSummeryTurn = intervalFromPreviousSummeryTime >= TIME_WINDOW_GLOBAL_SUMMERY;

			boolean isFrequencySummerizationRequired = isTrendingSummeryTurn || isLocalSummeryTurn || isGlobalSummeryTurn;

			ArrayList<String> rowsIds = new ArrayList<String>();
			ArrayList<Character> virtualTableInfo = new ArrayList<Character>();
			ArrayList<Float> trendingFrequencies = new ArrayList<Float>();
			ArrayList<Float> localFrequencies = new ArrayList<Float>();
			ArrayList<Float> globalFrequencies = new ArrayList<Float>();
			ArrayList<Integer> ages = new ArrayList<Integer>();

			ArrayList<Float> slottedFrequenciesPerSticker;
			int blockCount = 0;
			int existingTagCountPerBlock;
			long remainingCount = totalPossibleTagCount;
			int currentCount;

			// Fetch age and frequency of each sticker-tag data from primary table
			for (long i = 0; i < totalPossibleTagCount;)
			{
				currentCount = ((remainingCount / HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER) > 0) ? HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER
						: (int) remainingCount;
				i = i + currentCount;
				blockCount++;

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
						int tagIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_TAG_PHRASE);
						int ageIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE);
						int compositeFrequencyIndex = c.getColumnIndex(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY);

						while (c.moveToNext())
						{
							rowsIds.add(c.getString(rowIdIndex));
							virtualTableInfo.add(c.getString(tagIndex).charAt(0));

							slottedFrequenciesPerSticker = StickerSearchUtility.getIndividualNumericValues(c.getString(compositeFrequencyIndex),
									StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT, Float.class);

							trendingFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING));
							localFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL));
							globalFrequencies.add(slottedFrequenciesPerSticker.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL));

							slottedFrequenciesPerSticker.clear();
							slottedFrequenciesPerSticker = null;

							ages.add(c.getInt(ageIndex));
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

			Logger.v(TAG_REBALANCING, "summarizeAndDoRebalancing(), Read data in total no. of blocks = " + blockCount);

			/* Frequency shifting must be carried out in following order only */
			// Before shifting==>
			//
			// Today ending at 4 am
			// |||
			// |||
			// ^^^
			// -------------------------Global
			// ===--------------===Local
			// ===--------Trending
			//
			// ==> Order of shifting ('===' represents proportional shift, ':::' represents vacant space to be filled in next period and '---' represents pure cumulative frequency)
			//
			//
			// After shifting==>
			//
			// :::-----------------------Global
			// :::---------------Local
			// :::-------Trending
			// ^^^
			// |||
			// |||
			// Tomorrow starting after 4 am
			//

			int existingTotalTagCount = rowsIds.size();
			int initialTotalTagCount = existingTotalTagCount;
			long dbSizeInBytes = -1;
			long availableMemoryInBytes = -1;

			// Prepare trending summarization
			if (isTrendingSummeryTurn)
			{
				// Compute proportional trending frequencies first for all sticker-tags
				float maxTrendingFrequency = Collections.max(trendingFrequencies);

				float MAXIMUM_FREQUENCY_TRENDING = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_TRENDING,
						StickerSearchConstants.MAXIMUM_FREQUENCY_TRENDING);

				// Perform proportional update on trending frequency on day to day basis
				float ratioToRetainCurrentTrendingToNextTrending = 1.00f - (1.00f / TIME_WINDOW_TRENDING_SUMMERY);

				if (maxTrendingFrequency > MAXIMUM_FREQUENCY_TRENDING)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						trendingFrequencies.set(i, ((trendingFrequencies.get(i) * MAXIMUM_FREQUENCY_TRENDING / maxTrendingFrequency) * ratioToRetainCurrentTrendingToNextTrending));
					}
				}
				else
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						trendingFrequencies.set(i, (trendingFrequencies.get(i) * ratioToRetainCurrentTrendingToNextTrending));
					}
				}

				// Perform proportional shift from trending to local on day to day basis
				float ratioToCarryForwardTrendingTowardsLocal = TIME_WINDOW_TRENDING_SUMMERY / TIME_WINDOW_LOCAL_SUMMERY;

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					localFrequencies.set(i, (localFrequencies.get(i) + trendingFrequencies.get(i) * ratioToCarryForwardTrendingTowardsLocal));
				}

				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Trending summerization is done today at time:: " + date.toString());
			}

			// Prepare local summarization
			if (isLocalSummeryTurn)
			{
				// Compute proportional local frequencies first for all sticker-tags
				float maxLocalFrequency = Collections.max(localFrequencies);

				float MAXIMUM_FREQUENCY_LOCAL = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_LOCAL, StickerSearchConstants.MAXIMUM_FREQUENCY_LOCAL);

				// Perform proportional update on local frequency on day to day basis
				float ratioToRetainCurrentLocalToNextLocal = 1.00f - (1.00f / TIME_WINDOW_LOCAL_SUMMERY);

				if (maxLocalFrequency > MAXIMUM_FREQUENCY_LOCAL)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						localFrequencies.set(i, (localFrequencies.get(i) * (MAXIMUM_FREQUENCY_LOCAL / maxLocalFrequency) * ratioToRetainCurrentLocalToNextLocal));
					}
				}
				else
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						localFrequencies.set(i, (localFrequencies.get(i) * ratioToRetainCurrentLocalToNextLocal));
					}
				}

				// Perform proportional shift from local to global on day to day basis
				float ratioToCarryForwardLocalTowardsGlobal = TIME_WINDOW_LOCAL_SUMMERY / TIME_WINDOW_GLOBAL_SUMMERY;

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					globalFrequencies.set(i, (globalFrequencies.get(i) + localFrequencies.get(i) * ratioToCarryForwardLocalTowardsGlobal));
				}

				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Local summerization is done today at time:: " + date.toString());
			}

			// Prepare global summarization
			if (isGlobalSummeryTurn)
			{
				// Compute proportional global frequencies first for all sticker-tags
				float maxGlobalFrequency = Collections.max(globalFrequencies);

				float MAXIMUM_FREQUENCY_GLOBAL = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_GLOBAL, StickerSearchConstants.MAXIMUM_FREQUENCY_GLOBAL);

				// Perform proportional update on global frequency on day to day basis
				float ratioToRetainCurrentGlobalToNextGlobal = 1.00f - (1.00f / TIME_WINDOW_GLOBAL_SUMMERY);

				if (maxGlobalFrequency > MAXIMUM_FREQUENCY_GLOBAL)
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						globalFrequencies.set(i, ((globalFrequencies.get(i) * MAXIMUM_FREQUENCY_GLOBAL / maxGlobalFrequency) * ratioToRetainCurrentGlobalToNextGlobal));
					}
				}
				else
				{
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						globalFrequencies.set(i, (globalFrequencies.get(i) * ratioToRetainCurrentGlobalToNextGlobal));
					}
				}

				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global summerization is done today at time:: " + date.toString());
			}

			// Calculate current aging of sticker-tags
			for (int i = 0; i < existingTotalTagCount; i++)
			{
				ages.set(i, (ages.get(i) + 1));
			}

			// Clear data, if summary update needn't to be performed w.r.t. frequency
			if (!isFrequencySummerizationRequired)
			{
				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Rebalancing and summerization is not required today at time:: " + date.toString());

				virtualTableInfo.clear();
				virtualTableInfo = null;

				trendingFrequencies.clear();
				trendingFrequencies = null;
				localFrequencies.clear();
				localFrequencies = null;
				globalFrequencies.clear();
				globalFrequencies = null;

				existingTotalTagCount = 0;
			}

			// Do re-balancing with updated summarized data, if required to do so
			int cuttOffTagDataSize = (int) (MAXIMUM_PRIMARY_TABLE_CAPACITY * THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION);

			if ((existingTotalTagCount > 0) && (existingTotalTagCount < cuttOffTagDataSize))
			{
				// Check internal memory insufficiency
				File file = mContext.getDatabasePath(HikeStickerSearchBaseConstants.DATABASE_HIKE_STICKER_SEARCH);
				dbSizeInBytes = file.length();
				availableMemoryInBytes = file.getFreeSpace();
				long possibleDbExpansionSizeInBytes = (long) (dbSizeInBytes * THRESHOLD_DATABASE_EXPANSION_COEFFICIENT);

				if (availableMemoryInBytes < possibleDbExpansionSizeInBytes)
				{
					Logger.w(TAG_REBALANCING, "summarizeAndDoRebalancing(), Internal memory seems to get full in few days. Let's shrink sticker search database.");
					cuttOffTagDataSize = (int) (existingTotalTagCount * THRESHOLD_DATABASE_FORCED_SHRINK_COEFFICIENT);
				}
			}

			Logger.v(TAG_REBALANCING, "summarizeAndDoRebalancing(), Existing modifiable data size = " + existingTotalTagCount + ", cuttOffTagDataSize = " + cuttOffTagDataSize);
			ArrayList<Integer> eliminatedIndices = new ArrayList<Integer>();

			// Check if re-balancing is still required after several trails
			if (existingTotalTagCount >= cuttOffTagDataSize)
			{
				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global rebalancing is triggered today at time:: " + new Date().toString());

				// Calculate overall frequency till date
				ArrayList<Float> overallFrequencies = new ArrayList<Float>(existingTotalTagCount);

				for (int i = 0; i < existingTotalTagCount; i++)
				{
					overallFrequencies.add(trendingFrequencies.get(i) + localFrequencies.get(i) + globalFrequencies.get(i));
				}

				int oldestAge;
				int retainedCount = existingTotalTagCount;
				ArrayList<Integer> eliminatingIndices = new ArrayList<Integer>();
				ArrayList<Float> tempFrequencies = new ArrayList<Float>();
				ArrayList<Integer> tempAges = new ArrayList<Integer>(ages);
				int eligibleToEliminateCount;

				// Determine, which _id's are needed to delete
				while (retainedCount >= cuttOffTagDataSize)
				{
					// Compute current oldest age
					oldestAge = Collections.max(tempAges);

					// Delete based on age w.r.t. frequency distribution over sticker-tags in older to newer order
					for (int i = 0; i < existingTotalTagCount; i++)
					{
						if (tempAges.get(i) == oldestAge)
						{
							tempFrequencies.add(overallFrequencies.get(i));
							eliminatingIndices.add(i);
						}
					}

					// Compute eligibility according to one age at a time
					eligibleToEliminateCount = eliminatingIndices.size();
					if (eligibleToEliminateCount > 0)
					{
						// Find median frequency
						Collections.sort(tempFrequencies);

						int middleIndex = eligibleToEliminateCount / 2;
						float medianFrequency = ((eligibleToEliminateCount % 2) == 0) ? ((tempFrequencies.get(middleIndex - 1) + tempFrequencies.get(middleIndex)) / 2)
								: tempFrequencies.get(middleIndex);

						for (int i = 0; (i < eligibleToEliminateCount) && (retainedCount >= cuttOffTagDataSize); i++)
						{
							if (overallFrequencies.get(eliminatingIndices.get(i)) <= medianFrequency)
							{
								eliminatedIndices.add(eliminatingIndices.get(i));
								tempAges.set(eliminatingIndices.get(i), Integer.MIN_VALUE);
								retainedCount--;
							}
						}

						tempFrequencies.clear();
						eliminatingIndices.clear();
					}

				}

				overallFrequencies.clear();
				overallFrequencies = null;
				tempFrequencies = null;
				eliminatingIndices = null;
				tempAges.clear();
				tempAges = null;
			}

			int totalDeletingReferenceCount = eliminatedIndices.size();

			// Delete rows, if needed
			if (totalDeletingReferenceCount > 0)
			{
				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global elimination is triggered today at time:: " + date.toString());

				try
				{
					mDb.beginTransaction();

					int deletingIndex;
					String rowId;
					char virtualTableSuffix;
					String table;
					String whereConditionToDelete = StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithANDSyntax(
							new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID }, new int[] { HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK });

					for (int i = 0; i < totalDeletingReferenceCount; i++)
					{
						// Delete eligible row in primary table and its reference in other tables w.r.t. foreign key
						deletingIndex = eliminatedIndices.get(i);
						rowId = rowsIds.get(deletingIndex);
						virtualTableSuffix = virtualTableInfo.get(deletingIndex);

						if (StickerSearchUtility.isSpecialCharacterForLatin(virtualTableSuffix) || Character.isDefined(virtualTableSuffix))
						{
							table = getVirtualTableNameForChar(StickerSearchConstants.CHAR_EMPTY);
						}
						else
						{
							table = getVirtualTableNameForChar(virtualTableSuffix);
						}

						Logger.v(TAG_REBALANCING, "summarizeAndDoRebalancing(), Deleting primary table reference id: " + rowId + " in virtual table: " + table);

						mDb.delete(table, HikeStickerSearchBaseConstants.TAG_GROUP_UNIQUE_ID + HikeStickerSearchBaseConstants.SYNTAX_MATCH_START + rowId
								+ HikeStickerSearchBaseConstants.SYNTAX_MATCH_END, null);

						mDb.delete(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, whereConditionToDelete, new String[] { rowId });

						// Invalidate deleted data
						rowsIds.set(deletingIndex, null);

						trendingFrequencies.set(deletingIndex, null);
						localFrequencies.set(deletingIndex, null);
						globalFrequencies.set(deletingIndex, null);

						ages.set(deletingIndex, null);
					}

					mDb.setTransactionSuccessful();
				}
				finally
				{
					mDb.endTransaction();
					SQLiteDatabase.releaseMemory();

					virtualTableInfo.clear();
					virtualTableInfo = null;
					eliminatedIndices.clear();
					eliminatedIndices = null;
				}

				// Update remaining data of primary table by removing invalid data
				for (int i = 0; i < totalDeletingReferenceCount; i++)
				{
					rowsIds.remove(null);

					trendingFrequencies.remove(null);
					localFrequencies.remove(null);
					globalFrequencies.remove(null);

					ages.remove(null);
				}

				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global data elimination size = " + totalDeletingReferenceCount);
				Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global elimination is done today at time:: " + date.toString());
			}
			else
			{
				if (isFrequencySummerizationRequired)
				{
					virtualTableInfo.clear();
					virtualTableInfo = null;
				}

				eliminatedIndices = null;
			}

			// Update rows with computed summary above, if needed
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global summarized data update is triggered today at time:: " + date.toString());
			int retainedDataCount = rowsIds.size();

			if (retainedDataCount > 0)
			{
				try
				{
					mDb.beginTransaction();

					StringBuilder outputBuilder = null;
					ArrayList<Float> frequencyListPerStciker = null;
					if (isFrequencySummerizationRequired)
					{
						outputBuilder = new StringBuilder();
						frequencyListPerStciker = new ArrayList<Float>(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT);
					}

					String whereConditionToUpdate = StickerSearchUtility.getSQLiteDatabaseMultipleConditionsWithANDSyntax(
							new String[] { HikeStickerSearchBaseConstants.UNIQUE_ID }, new int[] { HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK });

					for (int i = 0; i < retainedDataCount; i++)
					{
						ContentValues cv = new ContentValues();

						if (isFrequencySummerizationRequired)
						{
							frequencyListPerStciker.add(trendingFrequencies.get(i));
							frequencyListPerStciker.add(localFrequencies.get(i));
							frequencyListPerStciker.add(globalFrequencies.get(i));

							cv.put(HikeStickerSearchBaseConstants.STICKER_OVERALL_FREQUENCY, StickerSearchUtility.getCompositeNumericValues(outputBuilder, frequencyListPerStciker));

							frequencyListPerStciker.clear();
						}

						cv.put(HikeStickerSearchBaseConstants.STICKER_ATTRIBUTE_AGE, ages.get(i));

						mDb.update(HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_MAPPING, cv, whereConditionToUpdate, new String[] { rowsIds.get(i) });
					}

					mDb.setTransactionSuccessful();
				}
				finally
				{
					mDb.endTransaction();
					SQLiteDatabase.releaseMemory();

					Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global summarized data update size = " + retainedDataCount);
					Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Global summarized data update is done today at time:: " + date.toString());
				}
			}

			// Clear data after operation completed
			rowsIds.clear();
			rowsIds = null;

			if (isFrequencySummerizationRequired)
			{
				trendingFrequencies.clear();
				trendingFrequencies = null;
				localFrequencies.clear();
				localFrequencies = null;
				globalFrequencies.clear();
				globalFrequencies = null;
			}

			ages.clear();
			ages = null;

			// Update summary time, once computed summary is updated in database
			currentTime = System.currentTimeMillis();
			if (isTrendingSummeryTurn)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_TRENDING_SUMMERIZATION_TIME, currentTime);
			}

			if (isLocalSummeryTurn)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_LOCAL_SUMMERIZATION_TIME, currentTime);
			}

			if (isGlobalSummeryTurn)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_GLOBAL_SUMMERIZATION_TIME, currentTime);
			}

			// Send summarization analytics to record current data status
			StickerManager.getInstance().sendRebalancingAnalytics(date.toString(), dbSizeInBytes, availableMemoryInBytes, initialTotalTagCount, totalDeletingReferenceCount);
		}
		else
		{
			Logger.i(TAG_REBALANCING, "summarizeAndDoRebalancing(), Primary table is empty today at time:: " + date.toString());

			HikeSharedPreferenceUtil.getInstance().removeData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_TRENDING_SUMMERIZATION_TIME);
			HikeSharedPreferenceUtil.getInstance().removeData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_LOCAL_SUMMERIZATION_TIME);
			HikeSharedPreferenceUtil.getInstance().removeData(HikeStickerSearchBaseConstants.KEY_PREF_LAST_GLOBAL_SUMMERIZATION_TIME);

			// Send summarization analytics to record that operation need not to be performed and stop further recording, once operation was halted
			if ((previousTrendingTime != 0L) || (previousLocalTime != 0L) || (previousGlobalTime != 0L))
			{
				StickerManager.getInstance().sendRebalancingAnalytics(date.toString(), -1L, -1L, -1, -1);
			}
		}

		return true;
	}
}