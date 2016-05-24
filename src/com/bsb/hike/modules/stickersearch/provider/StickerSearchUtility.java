/**
 * File   : StickerSearchUtility.java
 * Content: It is a utility class, especially designed for sticker search transitive operations.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.Word;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class StickerSearchUtility
{
	/* Determine if QA testing is enabled for simulating longer processes with shorter processes */
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

	/* Save the configuration settings received from server for sticker recommendation */
	public static void saveStickerRecommendationConfiguration(JSONObject json)
	{
		final String tag = "StickerRecommendationConfiguration";

		if ((json != null) && (json.length() > 0))
		{
			int minimumVersionToAcceptConfiguartionData = json.optInt(HikeConstants.STICKER_RECOMMENDATION_CONFIGURATION_MIN_VERSION_TO_APPLY, 0);
			if (minimumVersionToAcceptConfiguartionData > Utils.getAppVersionCode())
			{
				Logger.e(tag, "Proposed sticker recommendation configuration is not applicable for current version of Hike app. It should be updated.");
				return;
			}

			HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);
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
							int HH = rebalancingData.optInt(HikeConstants.STICKER_DATA_HOUR, 0);
							int mm = rebalancingData.optInt(HikeConstants.STICKER_DATA_MINUTE, 0);
							int ss = rebalancingData.optInt(HikeConstants.STICKER_DATA_SECOND, 0);
							int SSS = rebalancingData.optInt(HikeConstants.STICKER_DATA_MILLI_SECOND, 0);

							if (Utils.isValidTimeStampOfTheDay(HH, mm, ss, SSS))
							{
								Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
								calendar.set(Calendar.HOUR_OF_DAY, HH);
								calendar.set(Calendar.MINUTE, mm);
								calendar.set(Calendar.SECOND, ss);
								calendar.set(Calendar.MILLISECOND, SSS);

								stickerDataSharedPref
										.saveData(settingName, (new SimpleDateFormat(HikeConstants.FORMAT_TIME_OF_THE_DAY, Locale.ENGLISH)).format(calendar.getTime()));
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
						JSONObject separatorsData = json.getJSONObject(settingName);

						if (separatorsData != null)
						{
							Iterator<String> languages = separatorsData.keys();

							while (languages.hasNext())
							{
								String languageISOCode = languages.next();
								String regex = separatorsData.getString(languageISOCode);
								if (isValidSeparatorsRegex(regex))
								{
									stickerDataSharedPref.saveData(getSharedPrefKeyForSeparatorsRegex(languageISOCode), regex);
								}
								else
								{
									Logger.e(tag, "Invalid separators regex for language: " + languageISOCode);
								}
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
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_TRENDING, trending);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_LOCAL, local);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_SUMMERY_INTERVAL_GLOBAL, global);
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
							float local = (float) frequencyData.getDouble(HikeConstants.STICKER_DATA_LOCAL);
							float global = (float) frequencyData.getDouble(HikeConstants.STICKER_DATA_GLOBAL);

							if ((trending > 0) && (local > trending) && (global > local))
							{
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_TRENDING, trending);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_LOCAL, local);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAX_FREQUENCY_GLOBAL, global);
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
							float wExactMatch = (float) scoreWeightageData.getDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE_EXACT_MATCH);
							float wFrequency = (float) scoreWeightageData.getDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY);
							float wContextMoment = (float) scoreWeightageData.getDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT);

							if (isValidFraction(wLateral) && isValidFraction(wExactMatch) && isValidFraction(wFrequency) && isValidFraction(wContextMoment)
									&& ((wLateral + wExactMatch + wFrequency + wContextMoment) == 1.00f))
							{
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SCORE_WEIGHTAGE_MATCH_LATERAL, wLateral);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SCORE_WEIGHTAGE_EXACT_MATCH, wExactMatch);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY, wFrequency);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT, wContextMoment);
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
							stickerDataSharedPref.saveData(settingName, exactMatchMinReq);
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
							stickerDataSharedPref.saveData(settingName, marginalFullMatchReq);
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
							stickerDataSharedPref.saveData(settingName, autoCorrectionReq);
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
							float local = (float) frequencyRatioData.getDouble(HikeConstants.STICKER_DATA_LOCAL);
							float global = (float) frequencyRatioData.getDouble(HikeConstants.STICKER_DATA_GLOBAL);

							if (isValidFraction(trending) && isValidFraction(local) && isValidFraction(global) && (trending > local) && (local > global)
									&& ((trending + local + global) == 1.00f))
							{
								stickerDataSharedPref.saveData(HikeConstants.STICKER_FREQUENCY_RATIO_TRENDING, trending);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_FREQUENCY_RATIO_LOCAL, local);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_FREQUENCY_RATIO_GLOBAL, global);
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
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT, textLimit);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER, textBrokerLimit);
								stickerDataSharedPref.saveData(HikeConstants.STIKCER_TAG_MAXIMUM_SEARCH_PHRASE_PERMUTATION_SIZE, permutationSize);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MINIMUM_SEARCH_WORD_LENGTH_FOR_AUTO_CORRECTION, minAutoCorrectionLength);
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
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_RATIO_PER_SEARCH, selectionRatio);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_TAG_MAXIMUM_SELECTION_PER_STICKER, selectionLimit);
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
						stickerDataSharedPref.saveData(settingName, retryOption);
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
							stickerDataSharedPref.saveData(settingName, recommendationDelay);
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
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SEARCH_BASE_MAXIMUM_PRIMARY_TABLE_CAPACITY, ptCapacity);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION, ptThresholdCapacity);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_EXPANSION_COEFFICIENT, dbExpansionCoefficient);
								stickerDataSharedPref.saveData(HikeConstants.STICKER_SEARCH_BASE_THRESHOLD_FORCED_SHRINK_COEFFICIENT, dbForcedShrinkCoefficient);
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
			Logger.e(tag, "Invalid json data to save sticker recommendation configuration.");
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
				// Check if each separator is represented by single character or its '|' itself
				if (!((charSeparators.get(i).length() == 1) || charSeparators.get(i).equals(StickerSearchConstants.STRING_HEX_CHAR_OR)))
				{
					isValidSeparators = false;
					break;
				}
			}
		}

		return isValidSeparators;
	}

	/* Get the key used to save separators regex string in shared preference for given language */
	private static String getSharedPrefKeyForSeparatorsRegex(String languageISOCode)
	{
		return (HikeConstants.STICKER_TAG_REGEX_SEPARATORS + StickerSearchConstants.STRING_JOINTER + languageISOCode);
	}

	/* Get the key used to save recommendation analytics data in shared preference for given language */
	public static String getSharedPrefKeyForRecommendationData(String dataKey, String languageISOCode)
	{
		return (dataKey + StickerSearchConstants.STRING_JOINTER + languageISOCode);
	}

	/* Get combined regular expression for all separators applicable to language argument */
	public static String getSeparatorsRegex(String keyboardLanguageISOCode)
	{
		String separatorsRegex;
		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);

		if (!Utils.isBlank(keyboardLanguageISOCode) && !keyboardLanguageISOCode.startsWith(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE))
		{
			separatorsRegex = stickerDataSharedPref.getData(getSharedPrefKeyForSeparatorsRegex(keyboardLanguageISOCode), null);

			if (separatorsRegex == null)
			{
				separatorsRegex = stickerDataSharedPref.getData(getSharedPrefKeyForSeparatorsRegex(HikeConstants.STICKER_TAG_REGEX_SEPARATORS_REGIONAL_REGULAR),
						StickerSearchConstants.DEFAULT_REGEX_SEPARATORS_REGIONAL);
			}
		}
		else
		{
			separatorsRegex = stickerDataSharedPref.getData(getSharedPrefKeyForSeparatorsRegex(HikeConstants.STICKER_TAG_REGEX_SEPARATORS_LATIN_REGULAR),
					StickerSearchConstants.DEFAULT_REGEX_SEPARATORS_LATIN);
		}

		return separatorsRegex;
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
				chars.add(StickerSearchConstants.CHAR_OR);
			}
			else
			{
				chars.add(charSeparators.get(i).charAt(0));
			}
		}

		return chars;
	}

	/* Save recommendation analytics data stored in sticker search shared_pref */
	public static void saveStickerRecommendationAnalyticsDataIntoPref(Map<String, PairModified<Integer, Integer>> autoPopupClicksPerLanguageMap,
			Map<String, PairModified<Integer, Integer>> tapOnHighlightWordClicksPerLanguageMap)
	{
		// Clear previous data, if any
		clearStickerRecommendationAnalyticsDataFromPref();

		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);
		PairModified<Integer, Integer> totalAndAcceptedRecommendationCountPairPerLanguage;
		HashSet<String> currentLanguages = new HashSet<String>();
		HashMap<String, Integer> accuracyData = new HashMap<String, Integer>();

		// Save auto-popup data for each language
		if ((autoPopupClicksPerLanguageMap != null) && (autoPopupClicksPerLanguageMap.size() > 0))
		{
			Set<String> languages = autoPopupClicksPerLanguageMap.keySet();
			currentLanguages.addAll(languages);

			for (String languageISOCode : languages)
			{
				totalAndAcceptedRecommendationCountPairPerLanguage = autoPopupClicksPerLanguageMap.get(languageISOCode);

				accuracyData.put(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_TOTAL_COUNT_PER_LANGUAGE, languageISOCode),
						totalAndAcceptedRecommendationCountPairPerLanguage.getFirst());
				accuracyData.put(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode),
						totalAndAcceptedRecommendationCountPairPerLanguage.getSecond());
			}
		}

		// Save highlight word tapping data for each language
		if ((tapOnHighlightWordClicksPerLanguageMap != null) && (tapOnHighlightWordClicksPerLanguageMap.size() > 0))
		{
			Set<String> languages = tapOnHighlightWordClicksPerLanguageMap.keySet();
			currentLanguages.addAll(languages);

			for (String languageISOCode : languages)
			{
				totalAndAcceptedRecommendationCountPairPerLanguage = tapOnHighlightWordClicksPerLanguageMap.get(languageISOCode);

				accuracyData.put(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_TOTAL_COUNT_PER_LANGUAGE, languageISOCode),
						totalAndAcceptedRecommendationCountPairPerLanguage.getFirst());
				accuracyData.put(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode),
						totalAndAcceptedRecommendationCountPairPerLanguage.getSecond());
			}
		}

		if (accuracyData.size() > 0)
		{
			stickerDataSharedPref.saveDataSet(StickerSearchConstants.KEY_PREF_STICKER_RECOOMENDATION_LANGUAGE_LIST, currentLanguages);
			stickerDataSharedPref.saveDataMap(accuracyData);
		}
	}

	/* Clear recommendation analytics data stored in sticker search shared_pref */
	public static void clearStickerRecommendationAnalyticsDataFromPref()
	{
		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);
		Set<String> languages = stickerDataSharedPref.getDataSet(StickerSearchConstants.KEY_PREF_STICKER_RECOOMENDATION_LANGUAGE_LIST, null);
		if (!Utils.isEmpty(languages))
		{
			HashSet<String> removingKeys = new HashSet<String>();
			for (String languageISOCode : languages)
			{
				removingKeys.add(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_TOTAL_COUNT_PER_LANGUAGE, languageISOCode));
				removingKeys.add(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_AUTO_POPUP_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode));
				removingKeys.add(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_TOTAL_COUNT_PER_LANGUAGE, languageISOCode));
				removingKeys.add(getSharedPrefKeyForRecommendationData(StickerSearchConstants.KEY_PREF_TAP_ON_HIGHLIGHT_WORD_ACCEPTED_COUNT_PER_LANGUAGE, languageISOCode));
			}

			removingKeys.add(StickerSearchConstants.KEY_PREF_STICKER_RECOOMENDATION_LANGUAGE_LIST);
			stickerDataSharedPref.removeData(removingKeys);
		}
		else
		{
			stickerDataSharedPref.removeData(StickerSearchConstants.KEY_PREF_STICKER_RECOOMENDATION_LANGUAGE_LIST);
		}
	}

	/* Get string for predictive search */
	public static String getPredictiveSubString(String source, int offset, float predictionRatio)
	{
		return ((source != null) && (source.length() > offset)) ? source.substring(0, (int) (source.length() * predictionRatio + 0.50f)) : source;
	}

	/* Get ceiling first order moment of 2 numbers */
	public static int getFirstOrderMoment(int first, int second)
	{
		return (int) ((float) (first + second) / 2 + 0.50f);
	}

	/* Determine if given character is special character */
	public static boolean isSpecialCharacterForLatin(char c)
	{
		return ((int) c < 256) && !(((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')));
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
			if ((letters.length > 0) && !(StickerSearchUtility.isSpecialCharacterForLatin(letters[0])))
			{
				for (char c : letters)
				{
					if (!StickerSearchUtility.isSpecialCharacterForLatin(c))
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
			throw new IllegalArgumentException("Invalid argument (count).");
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
	public static String getSQLiteDatabaseMultipleConditionsWithANDSyntax(String[] columnsInvolvedInCondition, int[] isCheckingForNullOrNonNULLValueOrBoth)
	{
		StringBuilder sb;

		if ((columnsInvolvedInCondition != null) && (columnsInvolvedInCondition.length > 0) && (isCheckingForNullOrNonNULLValueOrBoth != null)
				&& (isCheckingForNullOrNonNULLValueOrBoth.length == columnsInvolvedInCondition.length))
		{
			sb = new StringBuilder();
			int lengthBeforeLastElement = columnsInvolvedInCondition.length - 1;

			for (int i = 0; i < columnsInvolvedInCondition.length; i++)
			{
				if (isCheckingForNullOrNonNULLValueOrBoth[i] == HikeStickerSearchBaseConstants.SQLITE_NULL_CHECK)
				{
					sb.append(columnsInvolvedInCondition[i]);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_UNSIGNED_CHECK);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_NULL);
				}
				else if (isCheckingForNullOrNonNULLValueOrBoth[i] == HikeStickerSearchBaseConstants.SQLITE_NON_NULL_CHECK)
				{
					sb.append(columnsInvolvedInCondition[i]);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_CHECK);
				}
				else
				{
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_OPEN);
					sb.append(columnsInvolvedInCondition[i]);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_UNSIGNED_CHECK);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_NULL);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_OR_NEXT);
					sb.append(columnsInvolvedInCondition[i]);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_SINGLE_PARAMETER_CHECK);
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_BRACKET_CLOSE);
				}

				// Do not add ' AND ' separator after sub-condition syntax of last element
				if (i != lengthBeforeLastElement)
				{
					sb.append(HikeStickerSearchBaseConstants.SYNTAX_AND_NEXT);
				}
			}
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
			throw new IllegalArgumentException((ids == null) ? "Invalid argument (ids)." : "Empty argument (ids).");
		}

		return sb.toString();
	}

	/* Get the code w.r.t. moment of time i.e. morning, evening, night etc. */
	public static TIME_CODE getMomentCode()
	{
		TIME_CODE momentCode = TIME_CODE.UNKNOWN;

		Calendar calendar = Calendar.getInstance();
		int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		int minuteOfHour = calendar.get(Calendar.MINUTE);
		int secondOfMinute = calendar.get(Calendar.SECOND);
		int milliSecondOfMinute = calendar.get(Calendar.MILLISECOND);

		// Format: HH.mmssmmm
		// HH = Hour of the day
		// mm = Minute of the hour
		// ss = Second of the minute
		// SSS = milliSecond of the minute
		float time = hourOfDay + (((float) minuteOfHour) / 1E+2f) + (((float) secondOfMinute) / 1E+4f) + (((float) milliSecondOfMinute) / 1E+7f);

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
					for (int i = (matchList.size() - 1); (i > -1) && (matchList.get(i).length() == 0); i--)
					{
						matchList.remove(i);
					}
				}
			}
		}

		return matchList;
	}

	/* Split charSequence in regular manner with boundary indexing */
	public static ArrayList<Word> splitWithIndexing(CharSequence input, String regExpression)
	{
		return splitWithIndexing(input, regExpression, 0);
	}

	/* Split charSequence in regular manner with boundary indexing along with limit on splitting */
	public static ArrayList<Word> splitWithIndexing(CharSequence input, String regExpression, int limit)
	{
		ArrayList<Word> wordList = null;

		if ((input != null) && (regExpression != null))
		{
			int index = 0;
			int start = 0;
			int length = input.length();
			boolean matchLimited = (limit > 0);
			wordList = new ArrayList<Word>();

			Matcher m = TextMatchManager.getPattern(regExpression).matcher(input);

			// Add segments before each match found
			while (m.find())
			{
				if (!matchLimited || (wordList.size() < (limit - 1)))
				{
					start = m.start();
					wordList.add(new Word(input.subSequence(index, start).toString(), index, start));
					index = m.end();
				}
				else if (wordList.size() == (limit - 1))
				{
					// Add last one
					wordList.add(new Word(input.subSequence(index, length).toString(), index, length));
					index = m.end();
				}
			}

			// If no match was found, return this
			if (index == 0)
			{
				wordList.add(new Word(input.toString(), index, length));
			}
			else
			{
				// Add remaining segment
				if (!matchLimited || (wordList.size() < limit))
				{
					wordList.add(new Word(input.subSequence(index, length).toString(), index, length));
				}

				// Construct result
				if (limit == 0)
				{
					for (int i = (wordList.size() - 1); (i > -1) && (wordList.get(i).getValue().length() == 0); i--)
					{
						wordList.remove(i);
					}
				}
			}
		}

		return wordList;
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

	public static float computeTextMatchScore(String searchKey, String tag, float marginalLateralScore)
	{

        float result = 0f;

        ArrayList<String> searchWords = StickerSearchUtility.split(searchKey, StickerSearchConstants.REGEX_SPACE, 0);
        while (searchWords.contains(StickerSearchConstants.STRING_EMPTY))
        {
            searchWords.remove(StickerSearchConstants.STRING_EMPTY);
        }

        ArrayList<String> tagWords = StickerSearchUtility.split(tag, StickerSearchConstants.REGEX_SPACE, 0);
        while (tagWords.contains(StickerSearchConstants.STRING_EMPTY))
        {
            tagWords.remove(StickerSearchConstants.STRING_EMPTY);
        }

        int searchWordsCount = searchWords.size();
        int exactWordsCount = tagWords.size();
        float matchCount = 0.0f;
        float localScore;

        for (int indexInSearchKey = 0; indexInSearchKey < searchWordsCount; indexInSearchKey++)
        {
            for (int indexInTag = 0; indexInTag < exactWordsCount; indexInTag++)
            {
                if (tagWords.get(indexInTag).contains(searchWords.get(indexInSearchKey)))
                {
                    localScore = ((float) searchWords.get(indexInSearchKey).length()) / tagWords.get(indexInTag).length();

                    if (indexInSearchKey == indexInTag)
                    {
                        matchCount += localScore;
                    }
                    else if (indexInSearchKey < indexInTag)
                    {
                        matchCount += localScore * (((float) (indexInSearchKey + 1)) / (indexInTag + 1));
                    }
                    else
                    {
                        matchCount += localScore * (((float) (indexInTag + 1)) / (indexInSearchKey + 1));
                    }

                    break;
                }
            }
        }



        // Apply spectra-full match prioritization before final scoring
		int maxIndexBound = Math.max(searchWordsCount, exactWordsCount);
		if (matchCount < maxIndexBound)
		{
			matchCount = matchCount
					+ computeAnalogousSpectrelScore(tagWords, searchWords, StickerSearchUtility.getFirstOrderMoment(searchWordsCount, exactWordsCount), marginalLateralScore);
		}
		result = Math.min(1.00f, (matchCount / maxIndexBound));

		return result;
	}

    public static float computeWordMatchScore(String searchKey, String tag)
    {
        ArrayList<String> searchWords = StickerSearchUtility.split(searchKey, StickerSearchConstants.REGEX_SPACE, 0);
        while (searchWords.contains(StickerSearchConstants.STRING_EMPTY))
        {
            searchWords.remove(StickerSearchConstants.STRING_EMPTY);
        }

        ArrayList<String> tagWords = StickerSearchUtility.split(tag, StickerSearchConstants.REGEX_SPACE, 0);
        while (tagWords.contains(StickerSearchConstants.STRING_EMPTY))
        {
            tagWords.remove(StickerSearchConstants.STRING_EMPTY);
        }

        int searchWordsCount = searchWords.size();
        int exactWordsCount = tagWords.size();
        float matchScore = 0.0f;
        float localScore;

        for (int indexInSearchKey = 0; indexInSearchKey < searchWordsCount; indexInSearchKey++)
        {
            for (int indexInTag = 0; indexInTag < exactWordsCount; indexInTag++)
            {
                if (tagWords.get(indexInTag).contains(searchWords.get(indexInSearchKey)))
                {
                    localScore = ((float) searchWords.get(indexInSearchKey).length()) / tagWords.get(indexInTag).length();

                    if (indexInSearchKey == indexInTag)
                    {
                        matchScore += localScore;
                    }
                    else if (indexInSearchKey < indexInTag)
                    {
                        matchScore += localScore * (((float) (indexInSearchKey + 1)) / (indexInTag + 1));
                    }
                    else
                    {
                        matchScore += localScore * (((float) (indexInTag + 1)) / (indexInSearchKey + 1));
                    }

                    break;
                }
            }
        }

        return matchScore;
    }

	public static float computeAnalogousSpectrelScore(ArrayList<String> tagWords, ArrayList<String> searchWords, int maximumPossibleSpectrumSpreading, float marginalLateralScore)
	{
		int wordMatchIndex;
		float specificSpectrumWidth;
		float matchCount = 0.0f;
		int spectrumLimit = Math.min(StickerSearchConstants.MAXIMUM_ACCEPTED_SPECTRUM_SCORING_SIZE, searchWords.size());

		for (int i = 0; i < spectrumLimit; i++)
		{
			wordMatchIndex = tagWords.indexOf(searchWords.get(i));
			if (wordMatchIndex > -1)
			{
				specificSpectrumWidth = marginalLateralScore / (i + 1);
				matchCount = matchCount + (specificSpectrumWidth / maximumPossibleSpectrumSpreading) / (wordMatchIndex + 1);
			}
		}

		return matchCount;
	}
}