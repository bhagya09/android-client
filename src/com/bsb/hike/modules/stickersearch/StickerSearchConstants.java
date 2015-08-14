package com.bsb.hike.modules.stickersearch;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;

public class StickerSearchConstants
{
	// Generic constants for UI================================================================[[
	public static final int WAIT_TIME_IN_FTUE_SCROLL = 1000; // 1 second period in milliseconds

	public static final int WAIT_TIME_SINGLE_CHARACTER_RECOMMENDATION = 250; // 0.25 second period in milliseconds

	public static final int SCROLL_SPEED_PER_DIP = 500;

	public static final int SHOW_SCROLL_FTUE_COUNT = 2;

	public static final int SCROLL_TIME = 500;

	// ================================================================Generic constants for UI]]

	// Generic constants for data setup========================================================[[
	public static final int TRIAL_STICKER_DATA_FIRST_SETUP = 0;

	public static final int TRIAL_STICKER_DATA_UPDATE_REFRESH = 1;

	public static final int DECISION_STATE_NO = 0;

	public static final int DECISION_STATE_YES = 1;

	// ========================================================Generic constants for data setup]]

	// Generic constants for periodic tasks====================================================[[
	public static final int REBALACING_DEFAULT_TIME_HOUR = 4; // 4 a.m. time-stamp

	public static final long DEFAULT_STICKER_TAG_REFRESH_TIME = 14 * 24 * 60 * 60 * 1000L; // 14 days period in milliseconds

	// ====================================================Generic constants for periodic tasks]]

	// Generic constants for searching text====================================================[[
	public static final int MAXIMUM_SEARCH_TEXT_LIMIT = 70;

	public static final int MAXIMUM_SEARCH_TEXT_BROKER_LIMIT = 75;

	public static final int MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION = 3;

	public static final float LIMIT_AUTO_CORRECTION = 0.70f; // 70 percent spelling must be correct

	public static final int MAXIMUM_PHRASE_PERMUTATION_SIZE = 4;

	// ====================================================Generic constants for searching text]]

	// Constants used in summary calculation===================================================[[
	public static final long TIME_WINDOW_TRENDING_SUMMERY = 3 * 24 * 60 * 60 * 1000L; // 3 days period in milliseconds

	public static final long TIME_WINDOW_LOCAL_SUMMERY = 14 * 24 * 60 * 60 * 1000L; // 14 days period in milliseconds

	public static final long TIME_WINDOW_GLOBAL_SUMMERY = 21 * 24 * 60 * 60 * 1000L; // 21 days period in milliseconds

	public static final long TEST_TIME_WINDOW_TRENDING_SUMMERY = 15 * 60 * 1000L; // 15 minute period in milliseconds

	public static final long TEST_TIME_WINDOW_LOCAL_SUMMERY = 30 * 60 * 1000L; // 30 minute period in milliseconds

	public static final long TEST_TIME_WINDOW_GLOBAL_SUMMERY = 45 * 60 * 1000L; // 45 minute period in milliseconds

	public static final float MAXIMUM_FREQUENCY_TRENDING = 2E+2f; // relative count

	public static final float MAXIMUM_FREQUENCY_LOCAL = 1E+3f; // relative count

	public static final float MAXIMUM_FREQUENCY_GLOBAL = 1.5E+3f; // relative count

	// ====================================================Constants used in summary calculation]]

	// Constants used for selecting search result==============================================[[
	public static final int MAXIMUM_SEARCH_COUNT = HikeStickerSearchBaseConstants.SQLITE_LIMIT_VARIABLE_NUMBER;

	public static final float RATIO_MAXIMUM_SELECTION_COUNT = 0.40f;

	public static final int MAXIMUM_TAG_SELECTION_COUNT_PER_STICKER = 2;

	// ==============================================Constants used for selecting search result]]

	// Constants used for calculating score====================================================[[
	public static final float LIMIT_EXACT_MATCH = 0.70f; // 70 percent

	public static final float WEIGHTAGE_MATCH_LATERAL = 0.30f;

	public static final float WEIGHTAGE_EXACT_MATCH = 0.32f;

	public static final float WEIGHTAGE_FREQUENCY = 0.28f;

	public static final float WEIGHTAGE_CONTEXT_MOMENT = 0.10f;

	public static final float RATIO_TRENDING_FREQUENCY = 0.59f;

	public static final float RATIO_LOCAL_FREQUENCY = 0.28f;

	public static final float RATIO_GLOBAL_FREQUENCY = 0.13f;

	public static final float MINIMUM_MATCH_SCORE_SINGLE_WORD_PREDICTIVE = 0.24f;

	public static final float MINIMUM_MATCH_SCORE_SINGLE_WORD_EXACT = 0.33f;

	public static final float MINIMUM_MATCH_SCORE_SINGLE_CHARACTER = 0.25f;

	public static final float MINIMUM_MATCH_SCORE_PHRASE_PREDICTIVE = 0.20f;

	public static final float MINIMUM_MATCH_SCORE_PHRASE_LIMITED = 0.10f;

	public static final float MARGINAL_FULL_SCORE_LATERAL = 0.80f; // marginal full score = 80.00% but not 100% (nothing is perfect till its perfect)

	public static final float DEFAULT_FREQUENCY_VALUE = 0.00f;

	public static final int FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING = 0;

	public static final int FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL = 1;

	public static final int FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL = 2;

	public static final int FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT = 3;

	// ====================================================Constants used for calculating score]]

	// Regular expressions=====================================================================[[
	public static final String REGEX_OR = "\\|\\\\|\\|"; // Regular expression for '|' or '\\|'

	public static final String REGEX_SEPARATORS = " |\n|\t|,|\\.|\\?";

	public static final String REGEX_PREDICATE = "\\*";

	public static final String REGEX_SINGLE_OR_PREDICATE = "\'|\\*";

	public static final String REGEX_ASSOCIATOR = " \\+ ";

	public static final String REGEX_SPACE = " ";

	// =====================================================================Regular expressions]]

	// Generic constants=======================================================================[[
	public static final String STRING_HEX_CHAR_OR = "x7C"; // Hexadecimal code of '|'

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

	public static final Character CHAR_SPACE = ' ';

	public static final Character CHAR_OR = '|';
	// =======================================================================Generic constants]]
}