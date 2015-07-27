/**
 * File   : HikeStickerSearchBaseConstants.java-->enum STATE_CATEGORY
 * Content: It contains all the constants used in sticker-search operation.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

public class HikeStickerSearchBaseConstants
{
	public static final int STICKERS_SEARCH_DATABASE_VERSION = 1;

	public static final String DATABASE_HIKE_STICKER_SEARCH = "hike_sticker_search_base";

	// Fixed tables used for Sticker-Tag relation and recommendations================================[[
	public static final String TABLE_STICKER_TAG_ENTITY = "stickerTagEntity";

	public static final String TABLE_STICKER_PACK_CATEGORY_HISTORY = "stickerCategoryHistory";

	public static final String TABLE_STICKER_TAG_MAPPING = "stickerTagMapping";

	// ================================Fixed tables used for Sticker-Tag relation and recommendations]]

	// Dynamic tables used for Sticker-Tag relation and recommendations==============================[[
	public static final String TABLE_STICKER_TAG_SEARCH = "stickerTagSearchData_";

	public static final int INITIAL_FTS_TABLE_COUNT = 27; // 26 for alphabets and one for special characters

	public static final int THRESHOLD_DYNAMIC_TABLE_COUNT = 50; // Changeable in future based on memory usage

	public static final int MAXIMUM_DYNAMIC_TABLE_CAPACITY = 10000; // Changeable in future based on memory usage

	public static final float THRESHOLD_DYNAMIC_TABLE_CAPACITY = 0.90f; // 90 percent // Changeable in future based on memory usage

	// ==============================Dynamic tables used for Sticker-Tag relation and recommendations]]

	// Constants used for Sticker-Tag relation and recommendations===================================[[
	public static final String UNIQUE_ID = "_id";

	// Table: TABLE_STICKER_TAG_ENTITY
	public static final String ENTITY_NAME = "primaryEntityName";

	public static final String ENTITY_TYPE = "entityType";

	public static final String ENTITY_QUALIFIED_HISTORY = "qualifiedData";

	public static final String ENTITY_UNQUALIFIED_HISTORY = "unqualifiedData";

	// Table: TABLE_STICKER_PACK_CATEGORY_HISTORY
	public static final String CATEGORY_ID = "categoryId";

	public static final String CATEGORY_CHAT_STORIES = "chatStoryIds"; // foreign key from TABLE_STICKER_TAG_ENTITY

	public static final String CATEGORY_STICKERS_HISTORY = "stickersHistory";

	public static final String CATEGORY_OVERALL_HISTORY = "overallHistory";

	// Table: TABLE_STICKER_TAG_MAPPING
	public static final String STICKER_TAG_PHRASE = "tagName";

	public static final String STICKER_STATE_FUNCTION_OF_FREQUENCY = "frequencyAsStateFunction";

	public static final String STICKER_REGION_FUNCTION_OF_FREQUENCY = "frequencyAsRegionFunction";

	public static final String STICKER_OVERALL_FREQUENCY_FOR_TAG = "totalFrequencyForTag";

	public static final String STICKER_OVERALL_FREQUENCY = "totalStickerFrequency";

	public static final String STICKER_STORY_THEME_ENTITIES = "stickerStoryThemeIds";

	public static final String STICKER_EXACTNESS_WITH_TAG_PRIORITY = "stickerTagClosenessOrder";

	public static final String STICKER_ATTRIBUTE_TIME = "stickerUsageMoment";

	public static final String STICKER_ATTRIBUTE_FESTIVALS = "stickerUsageForEvents";

	public static final String STICKER_ATTRIBUTE_AGE = "stickerAge";

	public static final String STICKER_RECOGNIZER_CODE = "stickerInformation";

	public static final String STICKER_STRING_USED_WITH_TAG = "frequentPrefixStringsUsed";

	public static final String STICKER_WORDS_NOT_USED_WITH_TAG = "frequentWordsForRejectingSticker";

	public static final String STICKER_TAG_POPULARITY = "stickerTagSuitabilityOrder";

	public static final String STICKER_IS_AVAILABLE = "isStickerAvailable";

	// Table: TABLE_TAG_SEARCH_*X, where *X is dynamically changeable variable
	public static final String TAG_REAL_PHRASE = "realTagName";

	public static final String TAG_GROUP_UNIQUE_ID = "tagUniqueId"; // foreign key from TABLE_STICKER_TAG_MAPPING

	// Syntax constants
	public static final int SQLITE_LIMIT_VARIABLE_NUMBER = 500;

	public static final String SYNTAX_PRIMARY_KEY = " INTEGER PRIMARY KEY AUTOINCREMENT, ";

	public static final String SYNTAX_FOREIGN_KEY = "FOREIGN KEY";

	public static final String SYNTAX_FOREIGN_REF = " REFERENCES ";

	public static final String SYNTAX_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";

	public static final String SYNTAX_CREATE_VTABLE = "CREATE VIRTUAL TABLE IF NOT EXISTS ";

	public static final String SYNTAX_FTS_VERSION_4 = " USING fts4";

	public static final String SYNTAX_INTEGER_NEXT = " INTEGER, ";

	public static final String SYNTAX_INTEGER_LAST = " INTEGER";

	public static final String SYNTAX_TEXT_NEXT = " TEXT, ";

	public static final String SYNTAX_TEXT_LAST = " TEXT";

	public static final String SYNTAX_NEXT = ", ";

	public static final String SYNTAX_START = "(";

	public static final String SYNTAX_END = ")";

	public static final String SYNTAX_MATCH_START = " MATCH '";

	public static final String SYNTAX_MATCH_END = "'";

	public static final String SYNTAX_PREDICATE_MATCH_END = "*'";

	public static final String SYNTAX_SINGLE_PARAMETER = "=?";

	public static final String SYNTAX_SINGLE_PARAMETER_NEXT = "=? AND ";

	// Entity type constants
	public static final int ENTITY_INIT_MARKER = 0; // Reserved

	public static final int ENTITY_STATE = 1; // Tag classification

	public static final int ENTITY_LANGUAGE = 2; // Tag region/ language

	public static final int ENTITY_CHAT_STORY_TOPIC = 3; // Conversation story

	public static final int ENTITY_INDIVIDUAL_USER = 4; // Individual person to chat with

	public static final int ENTITY_GROUP_USER = 5; // Group to chat in

	// Entity-qualified constants
	public static final String IS_INITIALISED = "isInitialising";

	public static final String TOTAL_CALASSIFICATION = "stateCount";

	// =============================Constants used for Sticker-Tag relation and recommendations]]

	// Constants used in calculation===========================================================[[
	public static final long CURRENT_SUMMERY_TIME_WINDOW = 3 * 24 * 60 * 60 * 1000L; // 3 days time period in milliseconds

	public static final float MAXIMUM_FREQUENCY = 100.0f; // relative count

	// ===========================================================Constants used in calculation]]

	// Generic constants=======================================================================[[
	public static final String STRING_EMPTY = "";

	public static final String STRING_SPACE = " ";

	public static final String STRING_INNER_SET_OPEN = "(";

	public static final String STRING_INNER_SET_CLOSE = ")";

	public static final String STRING_OUTER_SET_OPEN = "[";

	public static final String STRING_OUTER_SET_CLOSE = "]";

	public static final int DECISION_STATE_YES = 0;

	public static final int DECISION_STATE_NO = 1;

	// =======================================================================Generic constants]]

	// Constants used in shared_pref or system_db==============================================[[
	public static final String SHARED_PREF_STICKER_DATA = "hike_sticker_tag_data";

	public static final String KEY_PREF_USER_ID = "hike_user_id";

	public static final String KEY_PREF_PRE_STICKERS_TABLE_INFO = "previous_stickers_table_info";

	public static final String KEY_PREF_CUR_STICKERS_TABLE_INFO = "current_stickers_table_info";

	public static final String KEY_PREF_PREVIOUS_STICKERS_TABLE_VERSION = "previous_stickers_table_version";

	public static final String KEY_PREF_CURRENT_STICKERS_TABLE_VERSION = "current_stickers_table_version";

	public static final String KEY_PREF_PARAMETERS_HISTORY = "sticker_tag_relation_parameter_history";

	public static final String KEY_PREF_IS_POPULATED = "is_populated";

	public static final String KEY_PREF_LAST_SUMMERIZATION_TIME = "last_summerization_time";

	// ==============================================Constants used in shared_pref or system_db]]

	// Constants used for coding of time moments===============================================[[
	// Values of following indicators must be changed with sync to server side too; if required to do so
	public static final int MOMENT_CODE_UNIVERSAL = -1;

	public static final int MOMENT_CODE_UNIVERSAL_INITIATOR = 0;

	public static final int MOMENT_CODE_UNIVERSAL_TERMINATOR = 1;

	public static final int MOMENT_CODE_MORNING_TERMINAL = 2;

	public static final int MOMENT_CODE_NOON_TERMINAL = 3;

	public static final int MOMENT_CODE_AFTER_NOON_TERMINAL = 4;

	public static final int MOMENT_CODE_EVENING_TERMINAL = 5;

	public static final int MOMENT_CODE_NIGHT_TERMINAL = 6;

	// ------------------------------Add more in future; if required-----------------------------

	public static final int MOMENT_CODE_MORNING_NON_TERMINAL = 11;

	public static final int MOMENT_CODE_NOON_NON_TERMINAL = 12;

	public static final int MOMENT_CODE_AFTER_NOON_NON_TERMINAL = 13;

	public static final int MOMENT_CODE_EVENING_NON_TERMINAL = 14;

	public static final int MOMENT_CODE_NIGHT_NON_TERMINAL = 15;

	// ===============================================Constants used for coding of time moments]]

	// Constants used for indexing of sticker data=============================================[[
	// Order of following indices must be maintained iteratively; whenever removal/ addition of new index is taken place
	public static final int INDEX_STICKER_DATA_STICKER_CODE = 0;

	public static final int INDEX_STICKER_DATA_TAG_PHRASE = 1;

	public static final int INDEX_STICKER_DATA_PHRASE_LANGUAGE = 2;

	public static final int INDEX_STICKER_DATA_TAG_STATE_CATEGORY = 3;

	public static final int INDEX_STICKER_DATA_OVERALL_FREQUENCY_FOR_TAG = 4;

	public static final int INDEX_STICKER_DATA_OVERALL_FREQUENCY = 5;

	public static final int INDEX_STICKER_DATA_STORY_THEMES = 6;

	public static final int INDEX_STICKER_DATA_EXACTNESS_ORDER = 7;

	public static final int INDEX_STICKER_DATA_MOMENT_CODE = 8;

	public static final int INDEX_STICKER_DATA_FESTIVALS = 9;

	public static final int INDEX_STICKER_DATA_AGE = 10;

	public static final int INDEX_STICKER_DATA_USED_WITH_STRINGS = 11;

	public static final int INDEX_STICKER_DATA_REJECTED_WITH_WORDS = 12;

	public static final int INDEX_STICKER_DATA_COUNT = 13;

	// =============================================Constants used for indexing of sticker data]]

	// Default story
	public static final String DEFAULT_STORY = "generic";

	// Default theme
	public static final String DEFAULT_THEME_TAG = "GENERIC";

	// Constants used for calculating score====================================================[[

	public static final float WEITAGE_PRE_SCORE = 0.08f;

	public static final float WEITAGE_POST_SCORE = 0.29f;

	public static final float WEITAGE_EXACT_MATCH = 0.34f;

	public static final float WEITAGE_FREQUENCY = 0.23f;

	public static final float WEITAGE_CONTEXT_MOMENT = 0.06f;

	// ====================================================Constants used for calculating score]]

	// States used for Sticker-Tag relation and recommendations================================[[
	public static enum STATE_CATEGORY
	{
		OTHER(0), // Undefined
		GENERAL(1), // Usual expression
		THEME(2), // Centered story
		EMOTION(3), // Object of feeling
		FEELING(4), // State of mind/ heart
		REACTION(5), // Way of expressing
		RESPONSE(6), // Reply or, Reaction
		BEHAVIOUR(7), // External expression
		SMILEY(8), // Character or, Mood
		TITLE(100); // Exact phrase for sticker

		private final int mId;

		private STATE_CATEGORY(int identifier)
		{
			mId = identifier;
		}

		public int getId()
		{
			return mId;
		}

		public static int getIdFromCategory(String tagCategory)
		{

			int id;

			switch (tagCategory)
			{
			case "title":
				id = TITLE.getId();
				break;

			case "smiley":
				id = SMILEY.getId();
				break;

			case "behaviour":
				id = BEHAVIOUR.getId();
				break;

			case "response":
				id = RESPONSE.getId();
				break;

			case "reaction":
				id = REACTION.getId();
				break;

			case "feeling":
				id = FEELING.getId();
				break;

			case "emotion":
				id = EMOTION.getId();
				break;

			case "theme":
				id = THEME.getId();
				break;

			case "general":
				id = GENERAL.getId();
				break;

			default:
				id = OTHER.getId();
			}

			return id;
		}
	}

	// ================================States used for Sticker-Tag relation and recommendations]]

	// States used for day time division=======================================================[[
	public static enum TIME_CODE
	{
		INVALID(-1000), UNKNOWN(-1), MORNING(0), NOON(1), AFTER_NOON(2), EVENING(3), NIGHT(4);

		private final int mId;

		private TIME_CODE(int identifier)
		{
			mId = identifier;
		}

		public int getId()
		{
			return mId;
		}
	}
	// =======================================================States used for day time division]]
}