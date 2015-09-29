/**
 * File   : StickerSearchUtility.java
 * Content: It is a utility class, especially designed for sticker search transitive operations.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;
import com.bsb.hike.utils.Utils;

import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Pair;

public class StickerSearchUtility
{
	/* Determine if QA testing is enabled for making longer processes to shorter processes */
	public static boolean isTestModeForSRModule()
	{
		return Utils.isTestMode(HikeConstants.MODULE_STICKER_SEARCH);
	}

	/* Save setting preference related to sticker recommendation */
	public static void saveStickerRecommendationSettingsValue(String key, boolean value)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance()).edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	/* Get setting preference related to sticker recommendation */
	public static boolean getStickerRecommendationSettingsValue(String key, boolean defaultvalue)
	{
		return PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance()).getBoolean(key, defaultvalue);
	}

	/* Determine if given character is special character */
	public static boolean isSpecialCharacter(char c)
	{
		return ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == ' ') || (c == '\t') || (c == '\n');
	}

	/* Check if given word is of smiley's type */
	public static boolean isSmiley(String string, String generalizedString)
	{
		if (generalizedString == null)
		{
			generalizedString = formGeneralizedWord(string, null);
		}

		return Utils.isBlank(string) ? false : !generalizedString.equals(string);
	}

	/* Eliminate special characters from the given word and form a new word with remaining characters */
	public static String formGeneralizedWord(String str, StringBuilder outputBuilder)
	{
		if (outputBuilder == null)
		{
			outputBuilder = new StringBuilder();
		}
		else
		{
			outputBuilder.setLength(0);
		}

		if (Utils.isBlank(str))
		{
			char[] letters = str.toCharArray();

			// First, check if word is starting from special character
			if ((letters.length > 0) && !(StickerSearchUtility.isSpecialCharacter(letters[0])))
			{
				for (char c : letters)
				{
					if (!StickerSearchUtility.isSpecialCharacter(c))
					{
						outputBuilder.append(c);
					}
				}
			}
		}

		return outputBuilder.toString();
	}

	/* Get individual values from composite string of n values */
	public static <T> String getCompositeNumericValues(ArrayList<T> values)
	{
		StringBuilder outputBuilder = new StringBuilder();
		int size = (values == null) ? 0 : values.size();

		if (size > 0)
		{
			int lengthBeforeLastElement = size - 1;
			int i = 0;

			for (; i < lengthBeforeLastElement; i++)
			{
				outputBuilder.append(values.get(i));
				outputBuilder.append(StickerSearchConstants.STRING_ASSOCIATOR);
			}

			// Add last element
			outputBuilder.append(values.get(i));
		}

		return outputBuilder.toString();
	}

	/* Get individual values from composite string of n values for large no. of same instructions */
	public static <T> String getCompositeNumericValues(StringBuilder outputBuilder, ArrayList<T> values)
	{
		if (outputBuilder == null)
		{
			outputBuilder = new StringBuilder();
		}
		else
		{
			outputBuilder.setLength(0);
		}
		int size = (values == null) ? 0 : values.size();

		if (size > 0)
		{
			int lengthBeforeLastElement = size - 1;
			int i = 0;

			for (; i < lengthBeforeLastElement; i++)
			{
				outputBuilder.append(values.get(i));
				outputBuilder.append(StickerSearchConstants.STRING_ASSOCIATOR);
			}

			// Add last element
			outputBuilder.append(values.get(i));
		}

		return outputBuilder.toString();
	}

	/* Get individual values from string composed of 2 values */
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> getIndividualNumericValues(String s, int count, Class<T> kind)
	{
		ArrayList<Object> result = new ArrayList<Object>(count);

		// Return default values, if empty set
		if (Utils.isBlank(s))
		{
			if (kind == Integer.class)
			{
				for (int i = 0; i < count; i++)
				{
					result.add(0);
				}
			}
			else if (kind == Float.class)
			{
				for (int i = 0; i < count; i++)
				{
					result.add(0.00f);
				}
			}
			else if (kind == Long.class)
			{
				for (int i = 0; i < count; i++)
				{
					result.add(0L);
				}
			}
			else if (kind == Double.class)
			{
				for (int i = 0; i < count; i++)
				{
					result.add(0.00d);
				}
			}
			else
			{
				throw new IllegalArgumentException("Unknown type of numerical values are being demanded.");
			}
		}
		// Return actual values, if non-empty set
		else
		{
			ArrayList<String> values = split(s, StickerSearchConstants.REGEX_ASSOCIATOR, 0);
			int length = (values == null) ? 0 : values.size();
			int lessCount = count - length;
			int i = 0;

			if (kind == Integer.class)
			{
				for (i = 0; i < lessCount; i++)
				{
					result.add(0);
				}

				for (int j = 0; i < count; i++, j++)
				{
					try
					{
						result.add(Integer.parseInt(values.get(j)));
					}
					catch (NumberFormatException e)
					{
						result.add(0);
					}

				}
			}
			else if (kind == Float.class)
			{
				for (i = 0; i < lessCount; i++)
				{
					result.add(0.00f);
				}

				for (int j = 0; i < count; i++, j++)
				{
					try
					{
						result.add(Float.parseFloat(values.get(j)));
					}
					catch (NumberFormatException e)
					{
						result.add(0.00f);
					}

				}
			}
			else if (kind == Long.class)
			{
				for (i = 0; i < lessCount; i++)
				{
					result.add(0L);
				}

				for (int j = 0; i < count; i++, j++)
				{
					try
					{
						result.add(Long.parseLong(values.get(j)));
					}
					catch (NumberFormatException e)
					{
						result.add(0L);
					}

				}
			}
			else if (kind == Double.class)
			{
				for (i = 0; i < lessCount; i++)
				{
					result.add(0.00d);
				}

				for (int j = 0; i < count; i++, j++)
				{
					try
					{
						result.add(Double.parseDouble(values.get(j)));
					}
					catch (NumberFormatException e)
					{
						result.add(0.00d);
					}

				}
			}
			else
			{
				throw new IllegalArgumentException("Unknown type of numerical values are being demanded.");
			}
		}

		return (ArrayList<T>) result;
	}

	/* Get syntax string (a part of SQL query) while applying 'IN' clause with combination of multiple '?'s */
	public static String getSQLiteDatabaseMultipleParametersSyntax(int count)
	{
		StringBuilder sb;

		if (count > 0)
		{
			sb = new StringBuilder(count
					* (HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_NO_CHECK.length() + HikeStickerSearchBaseConstants.SYNTAX_NEXT_WITHOUT_SPACE.length())
					- HikeStickerSearchBaseConstants.SYNTAX_NEXT_WITHOUT_SPACE.length());
			int lengthBeforeLastParameter = count - 1;

			for (int i = 0; i < lengthBeforeLastParameter; i++)
			{
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_NO_CHECK);
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_NEXT_WITHOUT_SPACE);
			}

			// Add last parameter syntax
			sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_NO_CHECK);
		}
		else
		{
			throw new IllegalArgumentException("Invalid argument (count)");
		}

		return sb.toString();
	}

	/* Get syntax string (a part of SQL query) while applying multiple conditions using 'OR' operator */
	public static String getSQLiteDatabaseMultipleConditionsWithORSyntax(int count, String condition)
	{
		String syntaxString;
		boolean isCountArgumnetValid = count > 0;
		boolean isConditionArgumentValid = !Utils.isBlank(condition);

		if (isCountArgumnetValid && isConditionArgumentValid)
		{
			if (count == 1)
			{
				syntaxString = condition;
			}
			else
			{
				StringBuilder sb = new StringBuilder(
						count
								* (HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN.length() + condition.length() + HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE.length() + HikeStickerSearchBaseConstants.SYNTAX_OR_NEXT
										.length()) - HikeStickerSearchBaseConstants.SYNTAX_OR_NEXT.length());
				int lengthBeforeLastSubCondition = count - 1;

				for (int i = 0; i < lengthBeforeLastSubCondition; i++)
				{
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN);
					sb.append(condition);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_OR_NEXT);
				}

				// Add last sub-condition
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN);
				sb.append(condition);
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE);

				syntaxString = sb.toString();
			}
		}
		else
		{
			String error;
			if (!isCountArgumnetValid && !isConditionArgumentValid)
			{
				error = "Invalid arguments (count, condition)";
			}
			else if (!isCountArgumnetValid)
			{
				error = "Invalid argument (count)";
			}
			else
			{
				error = "Invalid argument (condition)";
			}

			throw new IllegalArgumentException(error);
		}

		return syntaxString;
	}

	/* Get syntax string (a part of SQL query) while applying multiple conditions using 'AND' operator */
	public static String getSQLiteDatabaseMultipleConditionsWithANDSyntax(String[] columnsInvolvedInCondition)
	{
		StringBuilder sb;

		if ((columnsInvolvedInCondition != null) && (columnsInvolvedInCondition.length > 0))
		{
			sb = new StringBuilder();
			int lengthBeforeLastCondition = columnsInvolvedInCondition.length - 1;
			int i = 0;

			for (; i < lengthBeforeLastCondition; i++)
			{
				sb.append(columnsInvolvedInCondition[i]);
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_CHECK);
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_AND_NEXT);
			}

			// Add last element syntax in sub-condition
			sb.append(columnsInvolvedInCondition[i]);
			sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_CHECK);
		}
		else
		{
			throw new IllegalArgumentException("Invalid argument (columnsInvolvedInCondition)");
		}

		return sb.toString();
	}

	/* Get syntax string (a part of SQL query) while applying 'MATCH' clause with multiple 'OR' in virtual table */
	public static String getSQLiteDatabaseMultipleMatchesSyntax(String[] ids)
	{
		StringBuilder sb = new StringBuilder();

		if ((ids != null) && (ids.length > 0))
		{
			int lengthBeforeLastElement = ids.length - 1;
			int i = 0;

			for (; i < lengthBeforeLastElement; i++)
			{
				sb.append(ids[i]);
				sb.append(HikeStickerSearchBaseConstants.SYNTAX_OR_NEXT);
			}

			// Add last element syntax
			sb.append(ids[i]);
		}
		else
		{
			throw new IllegalArgumentException((ids == null) ? "Invalid argument (ids)" : "Empty argument (ids)");
		}

		return sb.toString();
	}

	/* Get the code w.r.t. moment of time i.e. morning, evening, night etc. */
	public static TIME_CODE getMomentCode()
	{
		TIME_CODE momentCode = HikeStickerSearchBaseConstants.TIME_CODE.UNKNOWN;

		Calendar calendar = Calendar.getInstance();
		int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		int minuteOfHour = calendar.get(Calendar.MINUTE);
		int secondOfMinute = calendar.get(Calendar.SECOND);
		int milliSecondOfMinute = calendar.get(Calendar.MILLISECOND);

		float time = hourOfDay + (((float) minuteOfHour) / 100) + (((float) secondOfMinute) / 10000) + (((float) milliSecondOfMinute) / 10000000);

		if (time >= 4.3000000f && time < 12.0000000f)
		{
			momentCode = TIME_CODE.MORNING;
		}
		else if (time >= 12.0000000f && time < 12.3000000f)
		{
			momentCode = TIME_CODE.NOON;
		}
		else if (time >= 12.3000000f && time < 16.3000000f)
		{
			momentCode = TIME_CODE.AFTER_NOON;
		}
		else if (time >= 16.3000000f && time < 20.0000000f)
		{
			momentCode = TIME_CODE.EVENING;
		}
		else if ((time >= 20.0000000f && time < 24.0000000f) || (time >= 0.0000000f && time < 4.3000000f))
		{
			momentCode = TIME_CODE.NIGHT;
		}

		return momentCode;
	}

	/* Split charSequence in regular manner */
	public static ArrayList<String> split(CharSequence input, String regExpression)
	{
		return split(input, regExpression, 0);
	}

	/* Split charSequence in regular manner with limit on splitting */
	public static ArrayList<String> split(CharSequence input, String regExpression, int limit)
	{
		ArrayList<String> matchList = null;

		if ((input != null) && (regExpression != null))
		{
			int index = 0;
			int start = 0;
			int length = input.length();
			boolean matchLimited = (limit > 0);

			// All 3 lists are coupled w.r.t. order of insertion of elements in each list
			matchList = new ArrayList<String>(); // words

			Matcher m = TextMatchManager.getPattern(regExpression).matcher(input);

			// Add segments before each match found
			while (m.find())
			{
				if (!matchLimited || (matchList.size() < (limit - 1)))
				{
					start = m.start();
					matchList.add(input.subSequence(index, start).toString());
					index = m.end();
				}
				else if (matchList.size() == (limit - 1))
				{
					// Add last one
					matchList.add(input.subSequence(index, length).toString());
					index = m.end();
				}
			}

			// If no match was found, return this
			if (index == 0)
			{
				matchList.add(input.toString());
			}
			else
			{
				// Add remaining segment
				if (!matchLimited || (matchList.size() < limit))
				{
					matchList.add(input.subSequence(index, length).toString());
				}

				// Construct result
				if (limit == 0)
				{
					int i = matchList.size() - 1;

					while ((i > -1) && matchList.get(i).equals(StickerSearchConstants.STRING_EMPTY))
					{
						matchList.remove(i--);
					}
				}
			}
		}

		return matchList;
	}

	/* Split charSequence in regular manner with boundary indexing */
	public static Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> splitAndDoIndexing(CharSequence input, String regExpression)
	{
		return splitAndDoIndexing(input, regExpression, 0);
	}

	/* Split charSequence in regular manner with boundary indexing along with limit on splitting */
	public static Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> splitAndDoIndexing(CharSequence input, String regExpression, int limit)
	{
		ArrayList<String> matchList = null;
		ArrayList<Integer> startList = null;
		ArrayList<Integer> endList = null;

		if ((input != null) && (regExpression != null))
		{
			int index = 0;
			int start = 0;
			int length = input.length();
			boolean matchLimited = (limit > 0);

			// All 3 lists are coupled w.r.t. order of insertion of elements in each list
			matchList = new ArrayList<String>(); // words
			startList = new ArrayList<Integer>(); // start indexes of words (inclusive)
			endList = new ArrayList<Integer>(); // end indexes of words (exclusive)

			Matcher m = TextMatchManager.getPattern(regExpression).matcher(input);

			// Add segments before each match found
			while (m.find())
			{
				if (!matchLimited || (matchList.size() < (limit - 1)))
				{
					start = m.start();
					matchList.add(input.subSequence(index, start).toString());
					startList.add(index);
					endList.add(start);
					index = m.end();
				}
				else if (matchList.size() == (limit - 1))
				{
					// Add last one
					matchList.add(input.subSequence(index, length).toString());
					startList.add(index);
					endList.add(length);
					index = m.end();
				}
			}

			// If no match was found, return this
			if (index == 0)
			{
				matchList.add(input.toString());
				startList.add(index);
				endList.add(length);
			}
			else
			{
				// Add remaining segment
				if (!matchLimited || (matchList.size() < limit))
				{
					matchList.add(input.subSequence(index, length).toString());
					startList.add(index);
					endList.add(length);
				}

				// Construct result
				if (limit == 0)
				{
					int i = matchList.size() - 1;

					while ((i > -1) && matchList.get(i).equals(StickerSearchConstants.STRING_EMPTY))
					{
						matchList.remove(i);
						startList.remove(i);
						endList.remove(i--);
					}
				}
			}
		}

		return new Pair<>(matchList, new Pair<>(startList, endList));
	}

	public static class TextMatchManager
	{
		private static final HashMap<String, Pattern> sPatternContainer = new HashMap<String, Pattern>();

		private static Pattern getPattern(String regex)
		{
			Pattern pattern = sPatternContainer.get(regex);
			if ((pattern == null) && (regex != null))
			{
				pattern = Pattern.compile(regex);
				sPatternContainer.put(regex, pattern);
			}

			return pattern;
		}

		public static void clearResources()
		{
			sPatternContainer.clear();
		}
	}
}