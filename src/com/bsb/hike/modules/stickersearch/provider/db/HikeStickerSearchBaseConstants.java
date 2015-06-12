/**
 * File   : HikeStickerSearchBaseConstants.java-->enum STATE_CATEGORY
 * Content: It contains all the constants used in sticker-search operation.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

public class HikeStickerSearchBaseConstants {

	public static final int STICKERS_SEARCH_DATABASE_VERSION            = 1;
	public static final String DATABASE_HIKE_STICKER_SEARCH             = "hike_sticker_tags";

	// Fixed tables used for Sticker-Tag relation and recommendations================================[[
	public static final String TABLE_STICKER_TAG_ENTITY                 = "stickerTagEntity";
	public static final String TABLE_STICKER_CATEGORY_HISTORY           = "stickerCategoryHistory";
	public static final String TABLE_STICKER_TAG_MAPPING                = "stickerTagMapping";
	// ================================Fixed tables used for Sticker-Tag relation and recommendations]]

	// Dynamic tables used for Sticker-Tag relation and recommendations==============================[[
	public static final String TABLE_STICKER_TAG_SEARCH                 = "stickerTagSearchData_";
	public static final int THRESHOLD_DYNAMIC_TABLE_COUNT               = 50; // Changeable in future based on memory usage
	public static final int MAXIMUM_DYNAMIC_TABLE_CAPACITY              = 10000; // Changeable in future based on memory usage
	public static final float THRESHOLD_DYNAMIC_TABLE_CAPACITY          = 0.90f; // 90 percent // Changeable in future based on memory usage
	// ==============================Dynamic tables used for Sticker-Tag relation and recommendations]]

	// Constants used for Sticker-Tag relation and recommendations===================================[[
	public static final String UNIQUE_ID                                = "id";

	// Table: TABLE_STICKER_TAG_ENTITY
	public static final String ENTITY_NAME                              = "primaryEntityName";
	public static final String ENTITY_TYPE                              = "entityType";
	public static final String ENTITY_QUALIFIED_HISTORY                 = "qualifiedData";
	public static final String ENTITY_UNQUALIFIED_HISTORY               = "unqualifiedData";

	// Table: TABLE_STICKER_CATEGORY_HISTORY
	public static final String CATEGORY_ID                              = "categoryId";
	public static final String CATEGORY_CHAT_STORY_ID                   = "chatStoryId"; // foreign key from TABLE_STICKER_TAG_ENTITY
	public static final String CATEGORY_STICKERS_HISTORY                = "stickersHistory";
	public static final String CATEGORY_OVERALL_HISTORY                 = "overallHistory";

	// Table: TABLE_STICKER_TAG_MAPPING
	public static final String STICKER_TAG_PHRASE                       = "tagName";
	public static final String STICKER_STATE_FUNCTION_OF_FREQUENCY      = "frequencyAsStateFunction";
	public static final String STICKER_REGION_FUNCTION_OF_FREQUENCY     = "frequencyAsRegionFunction";
	public static final String STICKER_OVERALL_FREQUENCY_FOR_TAG        = "totalFrequencyForTag";
	public static final String STICKER_OVERALL_FREQUENCY                = "totalFrequency";
	public static final String STICKER_STORY_TOPIC_ENTITY_ID            = "stickerStoryTopicId";
	public static final String STICKER_EXTRA_ATTRIBUTES                 = "optionalAttributes";
	public static final String STICKER_RECOGNIZER_CODE                  = "stickerInformation";
	public static final String STICKER_PREFIX_STRING_USED_WITH_TAG      = "frequentPrefixStrings";
	public static final String STICKER_SUFFIX_STRING_USED_WITH_TAG      = "frequentSuffixStrings";
	public static final String STICKER_PREFIX_WORDS_NOT_USED_WITH_TAG   = "prefixStringForRejectedSticker";
	public static final String STICKER_SUFFIX_WORDS_NOT_USED_WITH_TAG   = "suffixStringForRejectedSticker";

	// Table: TABLE_TAG_SEARCH_*X // *X is dynamically changeable variable
	public static final String TAG_REAL_PHRASE                          = "realTagName";
	public static final String TAG_GROUP_UNIQUE_ID                      = "fixedTagUniqueId"; // foreign key from TABLE_STICKER_TAG_MAPPING

	// Syntax constants
	public static final String FOREIGN_KEY                              = "FOREIGN KEY";
	public static final String FOREIGN_REF                              = " REFERENCES ";
	public static final String CREATE_TABLE                             = "CREATE TABLE IF NOT EXISTS ";
	public static final String CREATE_VTABLE                            = "CREATE VIRTUAL TABLE IF NOT EXISTS ";
	public static final String FTS_VERSION_4                            = " USING fts4";

	// Entity type constants
	public static final int ENTITY_INIT_MARKER                          = 0; // Reserved
	public static final int ENTITY_STATE                                = 1;
	public static final int ENTITY_REGION                               = 2;
	public static final int ENTITY_CHAT_STORY_TOPIC                     = 3;
	public static final int ENTITY_INDIVIDUAL_USER                      = 4;
	public static final int ENTITY_GROUP_USER                           = 5;

	// Entity-qualified constants
	public static final String isInitialized                            = "isInitialising";
	public static final String stateCount                               = "stateCount";
	// =============================Constants used for Sticker-Tag relation and recommendations]]

	// Constants used in calculation===========================================================[[
	public static final int MAXIMUM_PROBABILITY                         = 100; // percent
	public static final int CURRENT_TIME_WINDOW                         = 3; // days
	public static final int MAXIMUM_FREQUENCY                           = 100; // relative count
	// ===========================================================Constants used in calculation]]

	// Generic constants=======================================================================[[
	public static final String HIKE_PACKAGE_NAME                        = "com.bsb.hike";
	public static final String EMPTY                                    = "";
	public static final String TRUE                                     = String.valueOf(true);
	public static final String FALSE                                    = String.valueOf(false);
	public static final String INNER_SET_OPEN                           = "(";
	public static final String INNER_SET_CLOSE                          = ")";
	public static final String OUTER_SET_OPEN                           = "[";
	public static final String OUTER_SET_CLOSE                          = "]";
	public static final String ASSOCIATOR                               = " + ";
	public static final String DISSOCIATOR                              = ", ";
	// =======================================================================Generic constants]]

	// Constants used in shared_pref or system_db==============================================[[
	public static final String SHARED_PREF_STICKER_DATA                 = "hike_sticker_tags";
	public static final String KEY_PREF_PRE_STICKERS_TABLE_INFO         = "pre_stickers_table_info";
	public static final String KEY_PREF_CUR_STICKERS_TABLE_INFO         = "cur_stickers_table_info";
	public static final String KEY_PREF_PRE_STICKERS_TABLE_VERSION      = "pre_stickers_table_version";
	public static final String KEY_PREF_CUR_STICKERS_TABLE_VERSION      = "cur_stickers_table_version";
	public static final String KEY_PRE_STICKERS_TABLE_INFO              = HIKE_PACKAGE_NAME + "_" + KEY_PREF_PRE_STICKERS_TABLE_INFO;
	public static final String KEY_CUR_STICKERS_TABLE_INFO              = HIKE_PACKAGE_NAME + "_" + KEY_PREF_CUR_STICKERS_TABLE_INFO;
	public static final String KEY_PRE_STICKERS_TABLE_VERSION           = HIKE_PACKAGE_NAME + "_" + KEY_PREF_PRE_STICKERS_TABLE_VERSION;
	public static final String KEY_CUR_STICKERS_TABLE_VERSION           = HIKE_PACKAGE_NAME + "_" + KEY_PREF_CUR_STICKERS_TABLE_VERSION;
	// ==============================================Constants used in shared_pref or system_db]]

	// States used for Sticker-Tag relation and recommendations================================[[
	public static enum STATE_CATEGORY {

		OTHER(0), // Undefined
		THEME(1), // Centered story
		EMOTION(2), // Object of feeling
		FEELING(3), // State of mind/heart
		BEHAVIOUR(4), // External expression
		REACTION(5), // Way of expressing
		RESPONSE(6), // Reply or, Reaction
		GENERAL(7), // Usual expression
		SMILEY(8); // Character or, Mood

		private final int mId;

		private STATE_CATEGORY(int identifier) {
			mId = identifier;
		}

		public int getId() {
			return mId;
		}

		public static int getIdFromCategory(String tagCategory) {

			int id;

			switch (tagCategory) {
			case "Theme":
				id = THEME.getId();
				break;

			case "Emotion":
				id = EMOTION.getId();
				break;

			case "Feeling":
				id = FEELING.getId();
				break;

			case "Behaviour":
				id = BEHAVIOUR.getId();
				break;

			case "Reaction":
				id = REACTION.getId();
				break;

			case "Response":
				id = RESPONSE.getId();
				break;

			case "General":
				id = GENERAL.getId();
				break;

			case "Smiley":
				id = SMILEY.getId();
				break;

			default:
				id = OTHER.getId();
			}

			return id;
		}
	}
	// ================================States used for Sticker-Tag relation and recommendations]]
}