package com.bsb.hike.modules.stickersearch;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;

public class StickerSearchConstants
{
	// Generic constants for UI================================================================[[
	public static final int SCROLL_DELAY = 1000; // 1 seconds period in milliseconds

	public static final int SCROLL_SPEED_PER_DIP = 500;

	public static final int SHOW_SCROLL_FTUE_COUNT = 2;

	// ================================================================Generic constants for UI]]

	// Generic constants for data setup========================================================[[
	public static final int STICKER_DATA_FIRST_SETUP = 0;

	public static final int STICKER_DATA_UPDATE_TRIAL = 1;

	// ========================================================Generic constants for data setup]]

	// Generic constants for periodic tasks====================================================[[
	public static final int REBALACING_DEFAULT_TIME = 4; // 4 a.m.

	public static final long DEFAULT_STICKER_TAG_REFRESH_TIME = 7 * 24 * 60 * 60 * 1000l; // 7 days period in milliseconds

	// ====================================================Generic constants for periodic tasks]]

	// Generic constants for searching text====================================================[[
	public static final int SEARCH_MAX_TEXT_LIMIT = 70;

	public static final int SEARCH_MAX_BROKER_LIMIT = 75;

	// ====================================================Generic constants for searching text]]

	// Constants used in calculation===========================================================[[
	public static final long LOCAL_SUMMERY_TIME_WINDOW = 14 * 24 * 60 * 60 * 1000l; // 14 days period in milliseconds

	public static final long CURRENT_SUMMERY_TIME_WINDOW = 3 * 24 * 60 * 60 * 1000L; // 3 days period in milliseconds

	public static final float MAXIMUM_FREQUENCY = 100.0f; // relative count

	// ===========================================================Constants used in calculation]]

	// Constants used for selecting search result==============================================[[
	public static final int MAXIMUM_SEARCH_COUNT = HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER;

	public static final float MAXIMUM_SELECTION_COUNT_RATIO = 0.50f;

	public static final int MAXIMUM_TAG_SELECTION_PER_STICKER_COUNT = 2;

	// ==============================================Constants used for selecting search result]]

	// Constants used for calculating score====================================================[[
	public static final float WEITAGE_MATCH_SCORE = 0.30f;

	public static final float WEITAGE_EXACT_MATCH = 0.34f;

	public static final float WEITAGE_FREQUENCY = 0.30f;

	public static final float WEITAGE_CONTEXT_MOMENT = 0.06f;

	public static final float TRENDING_FREQUENCY_RATIO = 0.50f;

	public static final float LOCAL_FREQUENCY_RATIO = 0.37f;

	public static final float GLOBAL_FREQUENCY_RATIO = 0.13f;

	public static final float MINIMUM_MATCH_SCORE_SINGLE_WORD = 0.33f;

	public static final float MINIMUM_MATCH_SCORE_SINGLE_CHARACTER = 0.30f;

	public static final float MINIMUM_MATCH_SCORE_PHRASE = 0.20f;

	public static final int MAXIMUM_ODD_MATCH_COUNT = 4;

	// ====================================================Constants used for calculating score]]

	// Regular expressions=====================================================================[[
	public static final String REGEX_PREDICATE = "\\*";

	public static final String REGEX_SINGLE_OR_PREDICATE = "\'|\\*";

	public static final String REGEX_ASSOCIATOR = " \\+ ";

	public static final String REGEX_SPACE = " ";

	// =====================================================================Regular expressions]]

	// Generic constants=======================================================================[[
	public static final String STRING_EMPTY = "";

	public static final String STRING_SPACE = " ";

	public static final String STRING_PREDICATE = "*";

	public static final String STRING_PREDICATE_NEXT = "* ";

	public static final String STRING_INNER_SET_OPEN = "(";

	public static final String STRING_INNER_SET_CLOSE = ")";

	public static final String STRING_OUTER_SET_OPEN = "[";

	public static final String STRING_OUTER_SET_CLOSE = "]";

	public static final String STRING_ASSOCIATOR = " + ";

	public static final String STRING_DISSOCIATOR = ", ";
	// =======================================================================Generic constants]]
}
