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

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;

import android.text.TextUtils;
import android.util.Pair;

public class StickerSearchUtility
{
	/* Determine if given character is special character */
	public static boolean isSpecialCharacter(char c)
	{

		return ((c < 'A') || (c > 'Z'));
	}

	/* Check if given word is of smiley's type */
	public static boolean isSmiley(String str)
	{

		boolean result = false;
		if (!TextUtils.isEmpty(str))
		{
			result = !str.equals(formGeneralizedWord(null, str));
		}

		return result;
	}

	/* Eliminate special characters from the given word and form a new word without them */
	public static String formGeneralizedWord(StringBuilder sb, String str)
	{
		if (sb == null)
		{
			sb = new StringBuilder();
		}
		sb.setLength(0);
		char[] letters = str.toCharArray();
		// First, check if word is starting from special character
		if ((letters.length > 0) && !(StickerSearchUtility.isSpecialCharacter(letters[0])))
		{
			for (char c : letters)
			{
				if (!StickerSearchUtility.isSpecialCharacter(c))
				{
					sb.append(c);
				}
			}
		}

		return sb.toString();
	}

	/* Get the code w.r.t. momement of time i.e. morning, evening, night etc. */
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
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.MORNING;
		}
		else if (time >= 12.0000000f && time < 12.3000000f)
		{
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.NOON;
		}
		else if (time >= 12.3000000f && time < 16.3000000f)
		{
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.AFTER_NOON;
		}
		else if (time >= 16.3000000f && time < 20.0000000f)
		{
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.EVENING;
		}
		else if (time >= 20.0000000f || time < 4.3000000f)
		{
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.NIGHT;
		}

		return momentCode;
	}

	/* Split charSequence in regular manner with boundary indexing */
	public static Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> splitAndDoIndexing(CharSequence input, String regExpression)
	{
		return splitAndDoIndexing(input, regExpression, 0);
	}

	/* Split charSequence in regular manner with boundary indexing along with limit on splitting */
	private static Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> splitAndDoIndexing(CharSequence input, String regExpression, int limit)
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

					while ((i > -1) && matchList.get(i).equals(HikeStickerSearchBaseConstants.STRING_EMPTY))
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

	private static class TextMatchManager
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
	}
}