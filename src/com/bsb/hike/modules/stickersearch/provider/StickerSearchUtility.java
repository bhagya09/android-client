/**
 * File   : StickerSearchUtility.java
 * Content: It is a utility class, especially designed for sticker search transitive operations.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.TimeStamp;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.SharedPreferences.Editor;
import android.util.Pair;

public class StickerSearchUtility
{
	/* Determine if QA testing is enabled for simulating longer processes with shorter processes */
	public static boolean isTestModeForSRModule()
	{
		return Utils.isTestMode(HikeConstants.MODULE_STICKER_SEARCH);
	}

	/* Save the configuration settings received from server for sticker recommendation */
	public static void saveStickerRecommendationConfiguration(JSONObject json, Editor editor)
	{
		final String tag = "StickerRecommendationConfiguration";

		if ((json != null) && (json.length() > 0) && (editor != null))
		{
			Iterator<String> configSettings = json.keys();

			while (configSettings.hasNext())
			{
				String settingName = configSettings.next();

				if (HikeConstants.STICKER_TAG_REBALANCING_TRIGGER_TIME_STAMP.equals(settingName))
				{
					try
					{
						JSONObject rebalancingData = json.getJSONObject(settingName);

						if (rebalancingData != null)
						{
							TimeStamp timeStamp = new TimeStamp(rebalancingData.optInt(HikeConstants.STICKER_DATA_HOUR, 0), rebalancingData.optInt(
									HikeConstants.STICKER_DATA_MINUTE, 0), rebalancingData.optInt(HikeConstants.STICKER_DATA_SECOND, 0), rebalancingData.optInt(
									HikeConstants.STICKER_DATA_MILLI_SECOND, 0));

							if (timeStamp.isValidTimeStampOfTheDay())
							{
								editor.putString(settingName, timeStamp.toString());
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_REGEX_SEPARATORS.equals(settingName))
				{
					try
					{
						String regex = json.getString(settingName);
						if (isValidSeparatorsRegex(regex))
						{
							editor.putString(settingName, regex);
						}
						else
						{
							Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_SUMMERY_INTERVAL.equals(settingName))
				{
					try
					{
						JSONObject summeryData = json.getJSONObject(settingName);

						if (summeryData != null)
						{
							long trending = summeryData.getLong(HikeConstants.STICKER_DATA_TRENDING);
							long local = summeryData.getLong(HikeConstants.STICKER_DATA_LOCAL);
							long global = summeryData.getLong(HikeConstants.STICKER_DATA_GLOBAL);

							if ((trending > 0) && (local > trending) && (global > local))
							{
								editor.putLong(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_TRENDING, trending);
								editor.putLong(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_LOCAL, local);
								editor.putLong(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_GLOBAL, global);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_MAX_FREQUENCY.equals(settingName))
				{
					try
					{
						JSONObject frequencyData = json.getJSONObject(settingName);

						if (frequencyData != null)
						{
							float trending = (float) frequencyData.getDouble(HikeConstants.STICKER_DATA_TRENDING);
							float local = (float) frequencyData.getLong(HikeConstants.STICKER_DATA_LOCAL);
							float global = (float) frequencyData.getLong(HikeConstants.STICKER_DATA_GLOBAL);

							if ((trending > 0) && (local > trending) && (global > local))
							{
								editor.putFloat(HikeConstants.STICKER_TAG_MAX_FREQUENCY_TRENDING, trending);
								editor.putFloat(HikeConstants.STICKER_TAG_MAX_FREQUENCY_LOCAL, local);
								editor.putFloat(HikeConstants.STICKER_TAG_MAX_FREQUENCY_GLOBAL, global);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_SCORE_WEIGHTAGE.equals(settingName))
				{
					try
					{
						JSONObject scoreWeightageData = json.getJSONObject(settingName);

						if (scoreWeightageData != null)
						{
							float wLateral = (float) scoreWeightageData.getDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE_MATCH_LATERAL);
							float wExactMatch = (float) scoreWeightageData.getLong(HikeConstants.STICKER_SCORE_WEIGHTAGE_EXACT_MATCH);
							float wFrequency = (float) scoreWeightageData.getLong(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY);
							float wContextMoment = (float) scoreWeightageData.getLong(HikeConstants.STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT);

							if (isValidFraction(wLateral) && isValidFraction(wExactMatch) && isValidFraction(wFrequency) && isValidFraction(wContextMoment)
									&& ((wLateral + wExactMatch + wFrequency + wContextMoment) == 1.00f))
							{
								editor.putFloat(HikeConstants.STICKER_SCORE_WEIGHTAGE_MATCH_LATERAL, wLateral);
								editor.putFloat(HikeConstants.STICKER_SCORE_WEIGHTAGE_EXACT_MATCH, wExactMatch);
								editor.putFloat(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY, wFrequency);
								editor.putFloat(HikeConstants.STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT, wContextMoment);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_LIMIT_EXACT_MATCH.equals(settingName))
				{
					try
					{
						float exactMatchMinReq = (float) json.getDouble(settingName);

						if (isValidReq(exactMatchMinReq))
						{
							editor.putFloat(settingName, exactMatchMinReq);
						}
						else
						{
							Logger.e(tag, "Incorrect data for '" + settingName + "' key...");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_SCORE_MARGINAL_FULL_MATCH_LATERAL.equals(settingName))
				{
					try
					{
						float marginalFullMatchReq = (float) json.getDouble(settingName);

						if (isValidReq(marginalFullMatchReq))
						{
							editor.putFloat(settingName, marginalFullMatchReq);
						}
						else
						{
							Logger.e(tag, "Incorrect data for '" + settingName + "' key...");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_LIMIT_AUTO_CORRECTION.equals(settingName))
				{
					try
					{
						float autoCorrectionReq = (float) json.getDouble(settingName);

						if (isValidReq(autoCorrectionReq))
						{
							editor.putFloat(settingName, autoCorrectionReq);
						}
						else
						{
							Logger.e(tag, "Incorrect data for '" + settingName + "' key...");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_FREQUENCY_RATIO.equals(settingName))
				{
					try
					{
						JSONObject frequencyRatioData = json.getJSONObject(settingName);

						if (frequencyRatioData != null)
						{
							float trending = (float) frequencyRatioData.getDouble(HikeConstants.STICKER_DATA_TRENDING);
							float local = (float) frequencyRatioData.getLong(HikeConstants.STICKER_DATA_LOCAL);
							float global = (float) frequencyRatioData.getLong(HikeConstants.STICKER_DATA_GLOBAL);

							if (isValidFraction(trending) && isValidFraction(local) && isValidFraction(global) && (local > trending) && (global > local)
									&& ((trending + local + global) == 1.00f))
							{
								editor.putFloat(HikeConstants.STICKER_FREQUENCY_RATIO_TRENDING, trending);
								editor.putFloat(HikeConstants.STICKER_FREQUENCY_RATIO_LOCAL, local);
								editor.putFloat(HikeConstants.STICKER_FREQUENCY_RATIO_GLOBAL, global);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_MAXIMUM_SEARCH.equals(settingName))
				{
					try
					{
						JSONObject searchLimitData = json.getJSONObject(settingName);

						if (searchLimitData != null)
						{
							int textLimit = searchLimitData.getInt(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT);
							int textBrokerLimit = searchLimitData.getInt(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER);
							int permutationSize = searchLimitData.getInt(HikeConstants.STIKCER_TAG_MAXIMUM_SEARCH_PHRASE_PERMUTATION_SIZE);
							int minAutoCorrectionLength = searchLimitData.getInt(HikeConstants.STICKER_TAG_MINIMUM_SEARCH_WORD_LENGTH_FOR_AUTO_CORRECTION);

							if ((textLimit > 0) && (textBrokerLimit >= textLimit) && (permutationSize >= 1) && (minAutoCorrectionLength >= 1))
							{
								editor.putInt(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT, textLimit);
								editor.putInt(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER, textBrokerLimit);
								editor.putInt(HikeConstants.STIKCER_TAG_MAXIMUM_SEARCH_PHRASE_PERMUTATION_SIZE, permutationSize);
								editor.putInt(HikeConstants.STICKER_TAG_MINIMUM_SEARCH_WORD_LENGTH_FOR_AUTO_CORRECTION, minAutoCorrectionLength);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_MAXIMUM_SELECTION.equals(settingName))
				{
					try
					{
						JSONObject selectionLimitData = json.getJSONObject(settingName);

						if (selectionLimitData != null)
						{
							float selectionRatio = (float) selectionLimitData.getDouble(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_RATIO_PER_SEARCH);
							int selectionLimit = selectionLimitData.getInt(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_PER_STICKER);

							if (isValidReq(selectionRatio) && (selectionLimit >= 1))
							{
								editor.putFloat(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_RATIO_PER_SEARCH, selectionRatio);
								editor.putInt(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_PER_STICKER, selectionLimit);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_TAG_RETRY_ON_FAILED_LOCALLY.equals(settingName))
				{
					try
					{
						int retryOption = json.getInt(settingName);
						editor.putInt(settingName, retryOption);
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_WAIT_TIME_SINGLE_CHAR_RECOMMENDATION.equals(settingName))
				{
					try
					{
						int recommendationDelay = json.getInt(settingName);

						if (recommendationDelay >= 0)
						{
							editor.putInt(settingName, recommendationDelay);
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else if (HikeConstants.STICKER_SEARCH_BASE.equals(settingName))
				{
					try
					{
						JSONObject searchBaseData = json.getJSONObject(settingName);

						if (searchBaseData != null)
						{
							int ptCapacity = searchBaseData.getInt(HikeConstants.STICKER_SEARCH_BASE_MAXIMUM_PRIMARY_TABLE_CAPACITY);
							float ptThresholdCapacity = (float) searchBaseData.getDouble(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION);
							float dbExpansionCoefficient = (float) searchBaseData.getDouble(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_EXPANSION_COEFFICIENT);
							float dbForcedShrinkCoefficient = (float) searchBaseData.getDouble(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_FORCED_SHRINK_COEFFICIENT);

							if ((ptCapacity > 0) && isValidReq(ptThresholdCapacity) && isValidReq(dbExpansionCoefficient) && isValidReq(dbForcedShrinkCoefficient))
							{
								editor.putInt(HikeConstants.STICKER_SEARCH_BASE_MAXIMUM_PRIMARY_TABLE_CAPACITY, ptCapacity);
								editor.putFloat(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION, ptThresholdCapacity);
								editor.putFloat(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_EXPANSION_COEFFICIENT, dbExpansionCoefficient);
								editor.putFloat(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_FORCED_SHRINK_COEFFICIENT, dbForcedShrinkCoefficient);
							}
							else
							{
								Logger.e(tag, "Invalid combination of data for '" + settingName + "' key...");
							}
						}
						else
						{
							Logger.e(tag, "Empty data for '" + settingName + "' key.");
						}
					}
					catch (JSONException e)
					{
						Logger.e(tag, "Invalid data for '" + settingName + "' key...", e);
					}
				}
				else
				{
					Logger.w(tag, "Unknown setting data to save sticker recommendation configuration. Key '" + settingName + "' can't be handled.");
				}
			}
		}
		else
		{
			Logger.e(tag, "Invalid json data to save sticker recommendation configuration through editor: " + editor);
		}
	}

	/* Check if given value is real and proper fraction */
	private static boolean isValidFraction(float weight)
	{
		return (weight >= 0.00f) && (weight <= 1.00f);
	}

	/* Check if given value is real and proper fraction along with checking minimum possible positive value */
	private static boolean isValidReq(float req)
	{
		return (req >= 0.01f) && (req <= 1.00f);
	}

	/* Check if given regular expression contains a valid list of separators */
	private static boolean isValidSeparatorsRegex(String regex)
	{
		ArrayList<String> charSeparators = split(regex, StickerSearchConstants.REGEX_OR, 0);
		boolean isValidSeparators = false;

		if (!Utils.isEmpty(charSeparators))
		{
			isValidSeparators = true;
			int separatorsCount = charSeparators.size();

			for (int i = 0; i < separatorsCount; i++)
			{
				if (!((charSeparators.get(i).length() == 1) || charSeparators.get(i).equals(StickerSearchConstants.STRING_HEX_CHAR_OR)))
				{
					isValidSeparators = false;
					break;
				}
			}
		}

		return isValidSeparators;
	}

	/* Get list of unique separator characters in the given regular expression */
	public static Set<Character> getSeparatorChars(String regex)
	{
		ArrayList<String> charSeparators = split(regex, StickerSearchConstants.REGEX_OR, 0);
		int separatorsCount = Utils.isEmpty(charSeparators) ? 0 : charSeparators.size();
		HashSet<Character> chars = new HashSet<Character>();

		for (int i = 0; i < separatorsCount; i++)
		{
			if (charSeparators.get(i).equals(StickerSearchConstants.STRING_HEX_CHAR_OR))
			{
				chars.add('|');
			}
			else
			{
				chars.add(charSeparators.get(i).charAt(0));
			}
		}

		return chars;
	}

	/* Get string for predictive search */
	public static String getPredictiveSubString(String source, int offset, float predictionRatio)
	{
		return ((source != null) && (source.length() > offset)) ? source.substring(0, (int) (source.length() * predictionRatio + 0.50f)) : source;
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

	/* Get individual values from string composed of 'count' values */
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

	/* Get syntax string (a part of SQL query) while applying 'IN' clause with multiple '?' */
	public static String getSQLiteDatabaseMultipleParameterSyntax(int count)
	{
		StringBuilder sb;

		if (count > 0)
		{
			sb = new StringBuilder(count * 2 - 1);

			sb.append("?");

			for (int i = 1; i < count; i++)
			{
				sb.append(",?");
			}
		}
		else
		{
			throw new IllegalArgumentException("Invalid argument (count)");
		}

		return sb.toString();
	}

	/* Get syntax string (a part of SQL query) while applying 'MATCH' clause with multiple 'OR' */
	public static String getSQLiteDatabaseMultipleMatchSyntax(String[] ids)
	{
		StringBuilder sb = new StringBuilder();

		if ((ids != null) && (ids.length > 0))
		{
			sb.append(ids[0]);

			for (int i = 1; i < ids.length; i++)
			{
				sb.append(" OR " + ids[i]);
			}
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

		// Format: HH.mmssmmm
		// HH = Hour of the day
		// mm = Minute of the hour
		// ss = Second of the minute
		// mmm = milliSecond of the minute
		float time = hourOfDay + (((float) minuteOfHour) / 1E+2f) + (((float) secondOfMinute) / 1E+4f) + (((float) milliSecondOfMinute) / 1E+7f);

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
		else if ((time >= 20.0000000f && time < 24.0000000f) || (time >= 0.0000000f && time < 4.3000000f))
		{
			momentCode = HikeStickerSearchBaseConstants.TIME_CODE.NIGHT;
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