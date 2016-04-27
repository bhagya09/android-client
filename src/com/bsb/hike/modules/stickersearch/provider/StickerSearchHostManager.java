/**
 * File   : StickerSearchHostManager.java
 * Content: It is a provider class to host all kinds of chat search demands.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import android.content.Context;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.StickerAppositeDataContainer;
import com.bsb.hike.modules.stickersearch.datamodel.Word;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility.TextMatchManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class StickerSearchHostManager
{
	private static final String TAG = StickerSearchHostManager.class.getSimpleName();

	private static int NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL;

	private static int NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL_CONTINUED;

	private static int NUMBER_OF_MAX_FESTIVE_PRIORITIZED_STICKERS;

	private static String REGEX_SEPARATORS;

	private static int MAXIMUM_SEARCH_TEXT_LIMIT;

	private static int MAXIMUM_SEARCH_TEXT_BROKER_LIMIT;

	private static int MAXIMUM_PHRASE_PERMUTATION_SIZE;

	private static int MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION;

	private static float LIMIT_AUTO_CORRECTION;

	private static float LIMIT_EXACT_MATCH;

	private static float WEIGHTAGE_MATCH_LATERAL;

	private static float WEIGHTAGE_EXACT_MATCH;

	private static float WEIGHTAGE_FREQUENCY_TRENDING;

	private static float WEIGHTAGE_FREQUENCY_LOCAL;

	private static float WEIGHTAGE_FREQUENCY_GLOBAL;

	private static float WEIGHTAGE_CONTEXT_MOMENT;

	private static float MARGINAL_FULL_SCORE_LATERAL;

	private static HashSet<Character> SEPARATOR_CHARS;

	private static ConcurrentHashMap<String, ArrayList<StickerAppositeDataContainer>> sCacheForLocalSearch = new ConcurrentHashMap<String, ArrayList<StickerAppositeDataContainer>>();

	private static HashMap<String, Float> sCacheForLocalAnalogousScore = new HashMap<String, Float>();

	private static HashMap<String, LinkedHashSet<Sticker>> sCacheForLocalOrderedStickers = new HashMap<String, LinkedHashSet<Sticker>>();

	private TIME_CODE mMomentCode = StickerSearchUtility.getMomentCode();

	private String mKeyboardLanguageISOCode;

	private String mCurrentText;

	private int mCurrentTextEditingStartIndex;

	private int mCurrentTextEditingEndIndex;

	private int mCurrentTextSignificantLength;

	private volatile ArrayList<Word> mCurrentWords = null;

	private ArrayList<String> mPreviousWords;

	private ArrayList<Sticker> mStickers;

	private HashMap<String, IndividualChatProfile> mIndividualChatRecord;

	private HashMap<String, GroupChatProfile> mGroupChatRecord;

	private IndividualChatProfile mCurrentIndividualChatProfile;

	private GroupChatProfile mCurrentGroupChatProfile;

	private static final Object sHostInitLock = new Object();

	private static final Object sHostOperateLock = new Object();

	private static volatile boolean sIsHostFinishingSearchTask;

	private static volatile StickerSearchHostManager sStickerSearchHostManager;

	private StickerSearchHostManager(Context context)
	{
		mCurrentTextSignificantLength = 0;

		NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL = Math.max(StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance().getApplicationContext()), 1);

		NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL_CONTINUED = NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL + 1;

		NUMBER_OF_MAX_FESTIVE_PRIORITIZED_STICKERS = NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL - 1;

		HikeSharedPreferenceUtil stickerDataSharedPref = HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA);

		REGEX_SEPARATORS = StickerSearchUtility.getSeparatorsRegex(mKeyboardLanguageISOCode);

		SEPARATOR_CHARS = (HashSet<Character>) StickerSearchUtility.getSeparatorChars(REGEX_SEPARATORS);

		MAXIMUM_SEARCH_TEXT_LIMIT = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT, StickerSearchConstants.MAXIMUM_SEARCH_TEXT_LIMIT);

		MAXIMUM_SEARCH_TEXT_BROKER_LIMIT = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER,
				StickerSearchConstants.MAXIMUM_SEARCH_TEXT_BROKER_LIMIT);

		MAXIMUM_PHRASE_PERMUTATION_SIZE = stickerDataSharedPref.getData(HikeConstants.STIKCER_TAG_MAXIMUM_SEARCH_PHRASE_PERMUTATION_SIZE,
				StickerSearchConstants.MAXIMUM_PHRASE_PERMUTATION_SIZE);

		MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_MINIMUM_SEARCH_WORD_LENGTH_FOR_AUTO_CORRECTION,
				StickerSearchConstants.MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION);

		LIMIT_AUTO_CORRECTION = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_LIMIT_AUTO_CORRECTION, StickerSearchConstants.LIMIT_AUTO_CORRECTION);

		LIMIT_EXACT_MATCH = stickerDataSharedPref.getData(HikeConstants.STICKER_TAG_LIMIT_EXACT_MATCH, StickerSearchConstants.LIMIT_EXACT_MATCH);

		WEIGHTAGE_MATCH_LATERAL = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_MATCH_LATERAL, StickerSearchConstants.WEIGHTAGE_MATCH_LATERAL);

		WEIGHTAGE_EXACT_MATCH = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_EXACT_MATCH, StickerSearchConstants.WEIGHTAGE_EXACT_MATCH);

		WEIGHTAGE_FREQUENCY_TRENDING = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY, StickerSearchConstants.WEIGHTAGE_FREQUENCY)
				* stickerDataSharedPref.getData(HikeConstants.STICKER_FREQUENCY_RATIO_TRENDING, StickerSearchConstants.RATIO_TRENDING_FREQUENCY);

		WEIGHTAGE_FREQUENCY_LOCAL = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY, StickerSearchConstants.WEIGHTAGE_FREQUENCY)
				* stickerDataSharedPref.getData(HikeConstants.STICKER_FREQUENCY_RATIO_LOCAL, StickerSearchConstants.RATIO_LOCAL_FREQUENCY);

		WEIGHTAGE_FREQUENCY_GLOBAL = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_FREQUENCY, StickerSearchConstants.WEIGHTAGE_FREQUENCY)
				* stickerDataSharedPref.getData(HikeConstants.STICKER_FREQUENCY_RATIO_GLOBAL, StickerSearchConstants.RATIO_GLOBAL_FREQUENCY);

		WEIGHTAGE_CONTEXT_MOMENT = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT, StickerSearchConstants.WEIGHTAGE_CONTEXT_MOMENT);

		MARGINAL_FULL_SCORE_LATERAL = stickerDataSharedPref.getData(HikeConstants.STICKER_SCORE_MARGINAL_FULL_MATCH_LATERAL, StickerSearchConstants.MARGINAL_FULL_SCORE_LATERAL);
	}

	/* Get the instance of this class from outside */
	public static StickerSearchHostManager getInstance()
	{
		if ((sStickerSearchHostManager == null) || sIsHostFinishingSearchTask)
		{
			synchronized (sHostInitLock)
			{
				if (sStickerSearchHostManager == null)
				{
					sIsHostFinishingSearchTask = false;
					sStickerSearchHostManager = new StickerSearchHostManager(HikeMessengerApp.getInstance());
				}
			}
		}

		return sStickerSearchHostManager;
	}

	/*
	 * Call this method just after choosing any contact to chat (while opening chat-thread) to load the history of that contact (either a person or a group)
	 */
	public void loadChatProfile(String contactId, boolean isGroupChat, long lastMessageTimestamp, String keyboardLanguageISOCode)
	{
		Logger.i(TAG, "loadChatProfile(" + contactId + ", " + isGroupChat + ", " + lastMessageTimestamp + ", " + keyboardLanguageISOCode + ")");

		synchronized (sHostOperateLock)
		{
			if (mPreviousWords != null)
			{
				mPreviousWords.clear();
			}
			else
			{
				mPreviousWords = new ArrayList<String>();
			}
		}

		if (mCurrentWords != null)
		{
			mCurrentWords.clear();
			mCurrentWords = null;
		}

		sCacheForLocalSearch.clear();
		sCacheForLocalAnalogousScore.clear();
		sCacheForLocalOrderedStickers.clear();

		mCurrentTextSignificantLength = 0;
		mCurrentText = null;
		mMomentCode = StickerSearchUtility.getMomentCode();
		mKeyboardLanguageISOCode = keyboardLanguageISOCode;

		REGEX_SEPARATORS = StickerSearchUtility.getSeparatorsRegex(mKeyboardLanguageISOCode);
		SEPARATOR_CHARS = (HashSet<Character>) StickerSearchUtility.getSeparatorChars(REGEX_SEPARATORS);
	}

	public int[] beforeTextChange(CharSequence s, int start, int count, int after)
	{
		return null;
	}

	public Pair<CharSequence, int[][]> onTextChange(CharSequence s, int start, int before, int count)
	{
		Logger.i(TAG, "onTextChanged(), Searching start at time: " + System.currentTimeMillis());

		Pair<CharSequence, int[][]> result;

		try
		{
			int end;
			if (count > 0)
			{
				end = start + count - 1;
			}
			else
			{
				end = start - before;
			}
			if (end < 0)
			{
				end = 0;
			}

			mCurrentTextEditingStartIndex = start;
			mCurrentTextEditingEndIndex = end;
			mCurrentTextSignificantLength = s.length();
			mCurrentText = s.toString();

			boolean isNeedToRemoveLastWord = false;
			if (mCurrentTextSignificantLength > MAXIMUM_SEARCH_TEXT_LIMIT)
			{
				mCurrentTextSignificantLength = MAXIMUM_SEARCH_TEXT_LIMIT;

				while ((mCurrentTextSignificantLength < s.length()) && (s.charAt(mCurrentTextSignificantLength) != StickerSearchConstants.CHAR_SPACE))
				{
					mCurrentTextSignificantLength++;
					if (mCurrentTextSignificantLength >= MAXIMUM_SEARCH_TEXT_BROKER_LIMIT)
					{
						isNeedToRemoveLastWord = true;
						break;
					}
				}
			}

			result = searchAndGetStickerResult(s, start, end, before, count, isNeedToRemoveLastWord);
		}
		catch (Throwable e)
		{
			Logger.e(TAG, "onTextChanged(), Exception in searching...", e);
			result = null;
		}

		Logger.i(TAG, "onTextChanged(), Searching over at time: " + System.currentTimeMillis());

		return result;
	}

	private Pair<CharSequence, int[][]> searchAndGetStickerResult(CharSequence s, int start, int end, int before, int count, boolean isNeedToRemoveLastWord)
	{
		Logger.i(TAG, "searchAndGetStickerResult(" + s + ", [" + start + " - " + end + "], " + before + ", " + count + ", " + isNeedToRemoveLastWord + ")");

		int[][] result = null;
		ArrayList<int[]> tempResult = new ArrayList<int[]>();

		CharSequence sourceText = s;
		if ((mCurrentTextSignificantLength != sourceText.length()) && (mCurrentTextSignificantLength < sourceText.length()))
		{
			s = sourceText.subSequence(0, mCurrentTextSignificantLength);
		}

		mCurrentWords = StickerSearchUtility.splitWithIndexing(s, REGEX_SEPARATORS, 0);

		if (!Utils.isEmpty(mCurrentWords))
		{
			int size = mCurrentWords.size();

			// Remove last word in case of long text, if required to do so
			if (isNeedToRemoveLastWord)
			{
				int lastPossibleConsiderableIndex = mCurrentWords.get(size - 1).getEnd();

				if (lastPossibleConsiderableIndex < sourceText.length())
				{
					if (SEPARATOR_CHARS.contains(sourceText.charAt(lastPossibleConsiderableIndex)))
					{
						mCurrentTextSignificantLength = lastPossibleConsiderableIndex;
					}
					else
					{
						mCurrentWords.remove(size - 1);
						size = mCurrentWords.size();

						if (size > 0)
						{
							mCurrentTextSignificantLength = mCurrentWords.get(size - 1).getEnd();
						}
						else
						{
							mCurrentTextSignificantLength = 0;
						}
					}
				}
			}

			String value;
			String nextWord;
			int lastIndexInPhraseStartedWithPivot;
			int lastWordIndexInPhraseStartedWithPivot;
			int maxPermutationSize;
			StringBuilder searchText = new StringBuilder();
			boolean suggestionFoundOnLastValidPhrase;
			boolean suggestionFoundOnLastValidWord;
			int currentHighlightRangeStart;
			int recentHighlighRangeEnd = 0;

			for (int i = 0, significantWordIndex = 0; i < size; i++)
			{
				value = mCurrentWords.get(i).getValue().replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);

				if (value.length() > 0)
				{
					suggestionFoundOnLastValidPhrase = false;
					suggestionFoundOnLastValidWord = false;
					searchText.setLength(0);
					maxPermutationSize = MAXIMUM_PHRASE_PERMUTATION_SIZE;
					nextWord = null;

					// Build phrase from a group of MAXIMUM_PHRASE_PERMUTATION_SIZE (= 4 or value received from server) words
					searchText.append(value);

					for (lastWordIndexInPhraseStartedWithPivot = i, lastIndexInPhraseStartedWithPivot = (i + 1); (maxPermutationSize > 1)
							&& (lastIndexInPhraseStartedWithPivot < size); lastIndexInPhraseStartedWithPivot++)
					{
						nextWord = mCurrentWords.get(lastIndexInPhraseStartedWithPivot).getValue()
								.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
						if (nextWord.length() == 0)
						{
							continue;
						}

						searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
						searchText.append(StickerSearchUtility.getPredictiveSubString(nextWord, MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION, LIMIT_AUTO_CORRECTION));

						maxPermutationSize--;
						lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
					}
					lastIndexInPhraseStartedWithPivot--;

					// Determine if exact match is needed
					int actualStartOfWord = mCurrentWords.get(i).getStart();
					int actualEndOfWord = mCurrentWords.get(i).getRealEnd();
					boolean exactSearch = !((actualStartOfWord > start) ? ((actualStartOfWord <= end) && (actualEndOfWord == end)) : (actualEndOfWord >= end));

					String searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);
					if ((lastIndexInPhraseStartedWithPivot == i) && !exactSearch)
					{
						searchKey = searchKey + StickerSearchConstants.STRING_PREDICATE;
					}

					if (!sCacheForLocalSearch.containsKey(searchKey))
					{
						ArrayList<StickerAppositeDataContainer> list = null;
						Logger.v(TAG, "Phrase \'" + searchKey + "\' was not found in local cache...");

						// Phrase sticker data
						ArrayList<StickerAppositeDataContainer> phraseResultList = null;
						if (lastWordIndexInPhraseStartedWithPivot > i)
						{
							Logger.v(TAG, "Phrase \'" + searchKey + "\' is going to be serached in database...");
							phraseResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(searchKey, false);

							if (!Utils.isEmpty(phraseResultList))
							{
								list = phraseResultList;
								Logger.v(TAG, "Filtering phrase stickers before saving in local cache: " + list);
								currentHighlightRangeStart = mCurrentWords.get(i).getStart();

								if ((recentHighlighRangeEnd < currentHighlightRangeStart)
										|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
								{
									recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
									tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
									Logger.v(TAG, "Highlighting phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd + ")");
								}
								else if ((recentHighlighRangeEnd > currentHighlightRangeStart)
										&& (recentHighlighRangeEnd < mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getStart()) && (tempResult.size() > 0))
								{
									recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
									tempResult.get(tempResult.size() - 1)[Word.BOUNDARY_INDEX_END] = recentHighlighRangeEnd;
									Logger.v(TAG, "Highlighting remaining phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd
											+ ")");
								}

								suggestionFoundOnLastValidPhrase = true;
							}
						}

						// Add separator between word stickers and phrase stickers
						if (list == null)
						{
							list = new ArrayList<StickerAppositeDataContainer>();
						}
						list.add(null);
						Logger.v(TAG, "Phrase searching is done, now word searching is being started...");

						// Word sticker data
						ArrayList<StickerAppositeDataContainer> wordResult = null;

						String partialSearchKey = value.toUpperCase(Locale.ENGLISH);
						String wordSearchKey = partialSearchKey + (exactSearch ? StickerSearchConstants.STRING_EMPTY : StickerSearchConstants.STRING_PREDICATE);

						if (partialSearchKey.length() > 1)
						{
							wordResult = sCacheForLocalSearch.get(wordSearchKey);
							if (wordResult == null)
							{
								Logger.v(TAG, "Word \'" + wordSearchKey + "\' was not found in local cache...");

								ArrayList<StickerAppositeDataContainer> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey,
										exactSearch);
								if (!Utils.isEmpty(wordResultList))
								{
									wordResult = wordResultList;
									Logger.v(TAG, "Filtering word stickers before saving in local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);

									sCacheForLocalSearch.putIfAbsent(wordSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Word searchKey::" + partialSearchKey + " ==> []");
									sCacheForLocalSearch.putIfAbsent(partialSearchKey, new ArrayList<StickerAppositeDataContainer>());
								}
							}
							else
							{
								Logger.v(TAG, "Filtering word stickers from local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);
							}
						}
						else if ((partialSearchKey.length() == 1) && (significantWordIndex == 0))
						{
							wordResult = sCacheForLocalSearch.get(partialSearchKey);
							if (wordResult == null)
							{
								Logger.v(TAG, "Single word \'" + partialSearchKey + "\' was not found in local cache...");

								ArrayList<StickerAppositeDataContainer> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey,
										true);
								if (!Utils.isEmpty(wordResultList))
								{
									wordResult = wordResultList;
									Logger.v(TAG, "Filtering single character word stickers before saving in local cache, searchKey::" + partialSearchKey + " ==> " + wordResult);

									sCacheForLocalSearch.putIfAbsent(partialSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Single character word searchKey::" + partialSearchKey + " ==> []");
									sCacheForLocalSearch.putIfAbsent(partialSearchKey, new ArrayList<StickerAppositeDataContainer>());
								}
							}
							else
							{
								Logger.v(TAG, "Filtering single character word stickers from local cache, searchKey::" + partialSearchKey + " ==> " + wordResult);
							}
						}

						if (!Utils.isEmpty(wordResult))
						{
							list.addAll(wordResult);
							suggestionFoundOnLastValidWord = true;
						}
						else
						{
							// Remove separator if only one search type (phrase but not word) is present
							list.remove(null);
						}

						if (suggestionFoundOnLastValidPhrase)
						{
							Logger.i(TAG, "Saving to cache, Phrase searchKey::" + searchKey + " ==> " + list);
							sCacheForLocalSearch.put(searchKey, list);
						}
						else
						{
							if (lastWordIndexInPhraseStartedWithPivot > i)
							{
								Logger.i(TAG, "Saving to cache, Phrase searchKey::" + searchKey + " ==> []");
								sCacheForLocalSearch.put(searchKey, new ArrayList<StickerAppositeDataContainer>());

								int currentMaxPermutationSize;
								maxPermutationSize = MAXIMUM_PHRASE_PERMUTATION_SIZE - 1;
								nextWord = null;

								// Handle partial phrase of remaining words
								for (String previousPhrase = null; maxPermutationSize > 1; maxPermutationSize--)
								{
									searchText.setLength(0);
									searchText.append(value);
									currentMaxPermutationSize = maxPermutationSize;
									ArrayList<StickerAppositeDataContainer> savedStickers;

									for (lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
									{
										nextWord = mCurrentWords.get(lastIndexInPhraseStartedWithPivot).getValue()
												.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
										if (nextWord.length() == 0)
										{
											continue;
										}

										searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
										searchText.append(StickerSearchUtility.getPredictiveSubString(nextWord, MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION, LIMIT_AUTO_CORRECTION));

										currentMaxPermutationSize--;
										lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
									}
									lastIndexInPhraseStartedWithPivot--;

									searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

									if (searchKey.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = sCacheForLocalSearch.get(searchKey);

									if (!Utils.isEmpty(savedStickers))
									{
										currentHighlightRangeStart = mCurrentWords.get(i).getStart();
										int marker = savedStickers.indexOf(null);

										if (marker != 0)
										{
											// word + phrase both searched successfully
											if ((recentHighlighRangeEnd < currentHighlightRangeStart)
													|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
											{
												recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
												tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
												Logger.v(TAG, "Highlighting partial phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");

												break;
											}
											else if ((recentHighlighRangeEnd > currentHighlightRangeStart)
													&& (recentHighlighRangeEnd < mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getStart()) && (tempResult.size() > 0))
											{
												recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
												tempResult.get(tempResult.size() - 1)[Word.BOUNDARY_INDEX_END] = recentHighlighRangeEnd;
												Logger.v(TAG, "Highlighting remaining partial phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");

												break;
											}
										}
										else
										{
											// Only word could be searched successfully
											if ((recentHighlighRangeEnd < currentHighlightRangeStart)
													|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
											{
												recentHighlighRangeEnd = mCurrentWords.get(i).getEnd();
												tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
												Logger.v(TAG, "Highlighting individual word \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");

												break;
											}
										}
									}

									previousPhrase = searchKey;
								}
							}

							// Handle current word
							currentHighlightRangeStart = mCurrentWords.get(i).getStart();
							if (!suggestionFoundOnLastValidPhrase
									&& suggestionFoundOnLastValidWord
									&& ((recentHighlighRangeEnd < currentHighlightRangeStart) || isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart,
											recentHighlighRangeEnd)))
							{
								recentHighlighRangeEnd = mCurrentWords.get(i).getEnd();
								tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
								Logger.v(TAG, "Highlighting word \'" + partialSearchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd + ")");
							}
						}
					}
					else
					{
						ArrayList<StickerAppositeDataContainer> savedStickers = sCacheForLocalSearch.get(searchKey);

						if (!Utils.isEmpty(savedStickers))
						{
							currentHighlightRangeStart = mCurrentWords.get(i).getStart();
							int marker = savedStickers.indexOf(null);

							if ((marker != 0) && (lastWordIndexInPhraseStartedWithPivot > i))
							{
								// Both (word + phrase) could be searched successfully
								if ((recentHighlighRangeEnd < currentHighlightRangeStart)
										|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
								{
									recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
									tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
									Logger.v(TAG, "Highlighting phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd + ")");
								}
								else if ((recentHighlighRangeEnd > currentHighlightRangeStart)
										&& (recentHighlighRangeEnd < mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getStart()) && (tempResult.size() > 0))
								{
									recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
									tempResult.get(tempResult.size() - 1)[Word.BOUNDARY_INDEX_END] = recentHighlighRangeEnd;
									Logger.v(TAG, "Highlighting remaining partial phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
											+ recentHighlighRangeEnd + ")");
								}
							}
							else if ((searchKey.length() > 1) || (significantWordIndex == 0))
							{
								// Only word could be searched successfully
								if ((recentHighlighRangeEnd < currentHighlightRangeStart)
										|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
								{
									recentHighlighRangeEnd = mCurrentWords.get(i).getEnd();
									tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
									Logger.v(TAG, "Highlighting word \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd + ")");
								}
							}
						}
						else
						{
							int currentMaxPermutationSize;
							maxPermutationSize = MAXIMUM_PHRASE_PERMUTATION_SIZE - 1;
							nextWord = null;

							// Handle partial phrase of remaining words
							for (String previousPhrase = null; maxPermutationSize > 1; maxPermutationSize--)
							{
								searchText.setLength(0);
								searchText.append(value);
								currentMaxPermutationSize = maxPermutationSize;

								for (lastWordIndexInPhraseStartedWithPivot = i, lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1
										&& lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
								{
									nextWord = mCurrentWords.get(lastIndexInPhraseStartedWithPivot).getValue()
											.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
									if (nextWord.length() == 0)
									{
										continue;
									}

									searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
									searchText.append(StickerSearchUtility.getPredictiveSubString(nextWord, MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION, LIMIT_AUTO_CORRECTION));

									currentMaxPermutationSize--;
									lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
								}
								lastIndexInPhraseStartedWithPivot--;

								searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

								if (lastWordIndexInPhraseStartedWithPivot > i)
								{
									if (searchKey.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = sCacheForLocalSearch.get(searchKey);

									if (!Utils.isEmpty(savedStickers))
									{
										currentHighlightRangeStart = mCurrentWords.get(i).getStart();

										if ((recentHighlighRangeEnd < currentHighlightRangeStart)
												|| isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
										{
											int marker = savedStickers.indexOf(null);

											if (marker != 0)
											{
												// Both (word + phrase) could be searched successfully
												recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
												tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
												Logger.v(TAG, "Highlighting partial phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");
											}
											else if (isNonEmptyWordEligibleForSearching(searchKey, significantWordIndex))
											{
												// Only word could be searched successfully
												recentHighlighRangeEnd = mCurrentWords.get(i).getEnd();
												tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
												Logger.v(TAG, "Highlighting individual word \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");
											}

											break;
										}
										else if ((recentHighlighRangeEnd > currentHighlightRangeStart)
												&& (recentHighlighRangeEnd < mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getStart()) && (tempResult.size() > 0))
										{
											int marker = savedStickers.indexOf(null);

											if (marker != 0)
											{
												// Both (word + phrase) could be searched successfully
												recentHighlighRangeEnd = mCurrentWords.get(lastWordIndexInPhraseStartedWithPivot).getEnd();
												tempResult.get(tempResult.size() - 1)[Word.BOUNDARY_INDEX_END] = recentHighlighRangeEnd;
												Logger.v(TAG, "Highlighting remaining partial phrase \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", "
														+ recentHighlighRangeEnd + ")");
											}

											break;
										}
									}
								}
								else
								{
									break;
								}

								previousPhrase = searchKey;
							}

							// Handle current word
							currentHighlightRangeStart = mCurrentWords.get(i).getStart();
							if (((recentHighlighRangeEnd < currentHighlightRangeStart) || isMarkingToHighlightFirstWordOrPhrase(currentHighlightRangeStart, recentHighlighRangeEnd))
									&& isNonEmptyWordEligibleForSearching(value, significantWordIndex))
							{
								searchKey = value.toUpperCase(Locale.ENGLISH);

								if (exactSearch)
								{
									savedStickers = sCacheForLocalSearch.get(searchKey);
								}
								else
								{
									savedStickers = sCacheForLocalSearch.get(searchKey + StickerSearchConstants.STRING_PREDICATE);
								}

								if (!Utils.isEmpty(savedStickers))
								{
									// Only word could be searched successfully, It may be part of earlier searched phrase successfully
									recentHighlighRangeEnd = mCurrentWords.get(i).getEnd();
									tempResult.add(new int[] { currentHighlightRangeStart, recentHighlighRangeEnd });
									Logger.v(TAG, "Highlighting word \'" + searchKey + "\' in range [" + currentHighlightRangeStart + ", " + recentHighlighRangeEnd + ")");
								}
							}
						}
					}

					significantWordIndex++;
				}
			}
		}
		else
		{
			mCurrentTextSignificantLength = 0;
		}

		int finalSuggestionsCount = tempResult.size();
		StringBuilder resultArray = new StringBuilder();

		if (finalSuggestionsCount > 0)
		{
			result = new int[finalSuggestionsCount][Word.BOUNDARY_COUNT];

			for (int i = 0; i < finalSuggestionsCount; i++)
			{
				result[i] = tempResult.get(i);
				resultArray.append("[" + result[i][Word.BOUNDARY_INDEX_START] + ", " + result[i][Word.BOUNDARY_INDEX_END] + "), ");
			}

			resultArray.setLength(resultArray.length() - 2);
		}

		Logger.i(TAG, "Highlight phrase boundaries: {" + resultArray.toString() + "}");

		return new Pair<CharSequence, int[][]>(sourceText, result);
	}

	private boolean isNonEmptyWordEligibleForSearching(String value, int effectiveIndexInText)
	{
		return (value.length() > 1) || (effectiveIndexInText == 0);
	}

	private boolean isMarkingToHighlightFirstWordOrPhrase(int start, int end)
	{
		return (start == 0) && (end == 0);
	}

	public void onInputMethodChanged(String languageISOCode)
	{
		Logger.i(TAG, "onInputMethodChanged(" + languageISOCode + ")");

		mKeyboardLanguageISOCode = languageISOCode;

		REGEX_SEPARATORS = StickerSearchUtility.getSeparatorsRegex(mKeyboardLanguageISOCode);
		SEPARATOR_CHARS = (HashSet<Character>) StickerSearchUtility.getSeparatorChars(REGEX_SEPARATORS);
	}

	public void onMessageSent(String textBeforeSticker, Sticker sticker, String textAfterSticker, String currentText)
	{
		Logger.i(TAG, "onMessageSent(" + textBeforeSticker + ", " + sticker + ", " + textAfterSticker + ", " + currentText + ")");

		if (Utils.isBlank(currentText))
		{
			Logger.i(TAG, "onMessageSent(), Current text has been marked to send; resetting all previous search results...");

			if (mCurrentWords != null)
			{
				mCurrentWords.clear();
				mCurrentWords = null;
			}

			sCacheForLocalSearch.clear();
			sCacheForLocalAnalogousScore.clear();
			sCacheForLocalOrderedStickers.clear();

			mCurrentText = null;
			mCurrentTextSignificantLength = 0;
		}

		mMomentCode = StickerSearchUtility.getMomentCode();

		StickerSearchDataController.getInstance().analyseMessageSent(textBeforeSticker, sticker, textAfterSticker);
	}

	public Pair<Pair<String, String>, ArrayList<Sticker>> onClickToShowRecommendedStickers(int where)
	{
		Logger.d(TAG, "onClickToShowRecommendedStickers(" + where + ")");

		ArrayList<Word> wordList = mCurrentWords;
		String currentString = mCurrentText;

		if (Utils.isEmpty(wordList))
		{
			return null;
		}

		ArrayList<Sticker> selectedStickers = null;
		LinkedHashSet<Sticker> stickers = null;
		Pair<String, LinkedHashSet<Sticker>> results = null;
		String clickedWord = null;
		String clickedPhrase = null;
		int effectiveClickedWordIndex = -1;

		for (int i = 0, significantWordIndex = 0; i < wordList.size(); i++)
		{
			String word = wordList.get(i).getValue().replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);

			if ((where >= wordList.get(i).getStart()) && (where <= wordList.get(i).getEnd()))
			{
				Logger.d(TAG, "onClickToShowRecommendedStickers(), Clicked word index = " + i);
				Logger.d(TAG, "onClickToShowRecommendedStickers(), Clicked word = " + word);

				if (word.length() > 0)
				{
					results = computeProbableStickers(currentString, wordList, word, i, (significantWordIndex == 0));
					clickedWord = word;
					effectiveClickedWordIndex = i;
				}
				else
				{
					// check pre-words, if valid
					int preIndex = i;
					int preInvalidCount = 0;
					while (--preIndex >= 0)
					{
						if (wordList.get(preIndex).getValue().length() > 0)
						{
							preInvalidCount = i - preIndex;
							break;
						}
					}

					// check post-words, if valid
					int postIndex = i;
					int postInvalidCount = 0;
					while (++postIndex < wordList.size())
					{
						if (wordList.get(postIndex).getValue().length() > 0)
						{
							postInvalidCount = postIndex - i;
							break;
						}
					}

					if ((preInvalidCount <= 0) && (postInvalidCount <= 0))
					{
						Logger.d(TAG, "onClickToShowRecommendedStickers(), No valid combination of words is present in current text.");
					}
					else
					{
						if (((preInvalidCount <= postInvalidCount) && (preInvalidCount > 0)) || (postInvalidCount <= 0))
						{
							results = computeProbableStickers(currentString, wordList, wordList.get(preIndex).getValue(), preIndex, (significantWordIndex == 0));
							effectiveClickedWordIndex = preIndex;
						}
						else
						{
							results = computeProbableStickers(currentString, wordList, wordList.get(postIndex).getValue(), postIndex, (significantWordIndex == 0));
							effectiveClickedWordIndex = postIndex;
						}

						clickedWord = word;
					}
				}

				if (results != null)
				{
					clickedPhrase = results.first;
					stickers = results.second;
				}

				Logger.d(TAG, "onClickToShowRecommendedStickers(), Fetched stickers (effective clicked word index = " + effectiveClickedWordIndex + "): " + stickers);
				break;
			}

			if (word.length() > 0)
			{
				significantWordIndex++;
			}
		}

		if (!Utils.isEmpty(stickers))
		{
			selectedStickers = new ArrayList<Sticker>(stickers);
			if (clickedPhrase == null)
			{
				clickedPhrase = clickedWord;
			}
		}

		return new Pair<Pair<String, String>, ArrayList<Sticker>>(new Pair<String, String>(clickedWord, clickedPhrase), selectedStickers);
	}

	private Pair<String, LinkedHashSet<Sticker>> computeProbableStickers(String currentString, ArrayList<Word> wordList, String word, int wordIndexInText,
			boolean isClickedWordFirstSignificantWord)
	{
		LinkedHashSet<Sticker> stickers = new LinkedHashSet<Sticker>();
		LinkedHashSet<Sticker> tempSelectedStickers = null;
		String relatedPhrase = null;
		int relatedPhraseStartWordIndex = -1;
		int relatedPhraseEndWordIndex = -1;

		int maxPermutationSize = MAXIMUM_PHRASE_PERMUTATION_SIZE;
		int currentMaxPermutationSize;
		StringBuilder searchText = new StringBuilder();
		StringBuilder rawSearchText = new StringBuilder();

		// determine if exact match is needed
		int actualStartOfWord = wordList.get(wordIndexInText).getStart();
		int actualEndOfWord = wordList.get(wordIndexInText).getRealEnd();
		boolean isFirstValidWordWithSingleCharacter = isClickedWordFirstSignificantWord && (word.length() == 1);
		Logger.v(TAG, "ActualStartOfWord = " + actualStartOfWord + ", ActualEndOfWord = " + actualEndOfWord);
		Logger.v(TAG, "CurrentTextEditingStartIndex = " + mCurrentTextEditingStartIndex + ", CurrentTextEditingEndIndex = " + mCurrentTextEditingEndIndex);
		Logger.v(TAG, "isFirstValidWordOfSingleCharacter = " + isFirstValidWordWithSingleCharacter);
		boolean exactSearch = !((actualStartOfWord > mCurrentTextEditingStartIndex) ? ((actualStartOfWord <= mCurrentTextEditingEndIndex) && (actualEndOfWord == mCurrentTextEditingEndIndex))
				: (actualEndOfWord >= mCurrentTextEditingEndIndex))
				|| isFirstValidWordWithSingleCharacter;

		// pre-phrase part
		int j;
		int count;
		ArrayList<Sticker> retainList = new ArrayList<Sticker>(8);
		int prePhraseStickersCount = 0;
		ArrayList<String> selectedTextInPhrase = new ArrayList<String>();
		String preWord;
		String currentPhrase;
		int firstWordIndexInPhraseEndedWithPivot;

		for (String previousPhrase = null; maxPermutationSize > 1;)
		{
			j = wordIndexInText - 1;
			count = 1;
			selectedTextInPhrase.add(word);
			currentMaxPermutationSize = maxPermutationSize;
			firstWordIndexInPhraseEndedWithPivot = -1;

			while (j >= 0 && count < currentMaxPermutationSize)
			{
				preWord = wordList.get(j).getValue().replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
				if (preWord.length() > 0)
				{
					selectedTextInPhrase.add(preWord);
					firstWordIndexInPhraseEndedWithPivot = j;
					count++;
				}

				j--;
			}
			Logger.i(TAG, "computeProbableStickers(), Clicked pre-phrase word list in reverse order = " + selectedTextInPhrase);

			if (count > 1)
			{
				String nextWord;
				Collections.reverse(selectedTextInPhrase);
				String firstWord = selectedTextInPhrase.get(0);
				searchText.append(firstWord);
				rawSearchText.append(firstWord);

				// build phrase from a group of some words
				for (j = 1; j < count; j++)
				{
					nextWord = selectedTextInPhrase.get(j);

					searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
					searchText.append(StickerSearchUtility.getPredictiveSubString(nextWord, MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION, LIMIT_AUTO_CORRECTION));

					rawSearchText.append(StickerSearchConstants.STRING_SPACE);
					rawSearchText.append(nextWord);
				}

				currentPhrase = searchText.toString().toUpperCase(Locale.ENGLISH);
				if (currentPhrase.equals(previousPhrase))
				{
					selectedTextInPhrase.clear();
					searchText.setLength(0);
					rawSearchText.setLength(0);
					maxPermutationSize--;
					continue;
				}

				Logger.i(TAG, "computeProbableStickers(), Finding stickers for searched pre-phrase \'" + currentPhrase + "\'");

				// Compute phrase search results having first word significant
				if (firstWord.length() > 1)
				{
					tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
							StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_PREDICTIVE);
				}
				// Compute phrase search results having no word significant
				else
				{
					tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
							StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_LIMITED);
				}

				if (!Utils.isEmpty(tempSelectedStickers))
				{
					int marker = 0;
					int markedLimit = 6 - (10 / maxPermutationSize);

					for (Sticker sticker : tempSelectedStickers)
					{
						if (marker >= 6)
						{
							break;
						}
						else
						{
							stickers.add(sticker);
							if (marker < markedLimit)
							{
								retainList.add(sticker);
							}
							prePhraseStickersCount++;
						}

						marker++;
					}

					if (relatedPhraseStartWordIndex == -1)
					{
						relatedPhraseStartWordIndex = firstWordIndexInPhraseEndedWithPivot;
					}
				}
			}
			else
			{
				break;
			}

			previousPhrase = currentPhrase;
			selectedTextInPhrase.clear();
			searchText.setLength(0);
			rawSearchText.setLength(0);
			maxPermutationSize--;
		}

		// Shrink stickers for pre-phrase, if more than allowed limit
		if (prePhraseStickersCount > 8)
		{
			stickers.retainAll(retainList);
		}
		retainList.clear();

		// post-phrase and word part
		maxPermutationSize = MAXIMUM_PHRASE_PERMUTATION_SIZE;
		String nextWord;
		int lastIndexInPhraseStartedWithPivot;
		int lastWordIndexInPhraseStartedWithPivot;

		for (String previousPhrase = null; maxPermutationSize > 0;)
		{
			// Build phrase from a group of some words
			searchText.append(word);
			rawSearchText.append(word);
			currentMaxPermutationSize = maxPermutationSize;
			nextWord = null;

			for (lastWordIndexInPhraseStartedWithPivot = wordIndexInText, lastIndexInPhraseStartedWithPivot = wordIndexInText + 1; (currentMaxPermutationSize > 1)
					&& (lastIndexInPhraseStartedWithPivot < wordList.size()); lastIndexInPhraseStartedWithPivot++)
			{
				nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).getValue()
						.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
				if (nextWord.length() == 0)
				{
					continue;
				}

				searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
				searchText.append(StickerSearchUtility.getPredictiveSubString(nextWord, MINIMUM_WORD_LENGTH_FOR_AUTO_CORRECTION, LIMIT_AUTO_CORRECTION));

				rawSearchText.append(StickerSearchConstants.STRING_SPACE);
				rawSearchText.append(nextWord);

				currentMaxPermutationSize--;
				lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
			}
			lastIndexInPhraseStartedWithPivot--;

			currentPhrase = searchText.toString().toUpperCase(Locale.ENGLISH);
			if (currentPhrase.equals(previousPhrase))
			{
				previousPhrase = currentPhrase;
				searchText.setLength(0);
				rawSearchText.setLength(0);
				maxPermutationSize--;
				continue;
			}

			Logger.i(TAG, "computeProbableStickers(), Finding stickers for searched phrase \'" + currentPhrase + "\' with exactSearch: " + exactSearch);

			// Compute single word search results
			if (lastWordIndexInPhraseStartedWithPivot == wordIndexInText)
			{
				if (exactSearch)
				{
					if (isFirstValidWordWithSingleCharacter)
					{
						tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
								StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_CHARACTER);
					}
					else if (currentPhrase.length() > 1)
					{
						tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
								StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_WORD_EXACT);
					}
				}
				else if (currentPhrase.length() > 1)
				{
					tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), (currentPhrase + StickerSearchConstants.STRING_PREDICATE),
							StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_WORD_PREDICTIVE);
				}
			}
			// Compute phrase search results having first word significant
			else if (word.length() > 1)
			{
				tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
						StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_PREDICTIVE);
			}
			// Compute phrase search results having no word significant
			else
			{
				tempSelectedStickers = getOrderedStickers(rawSearchText.toString().toUpperCase(Locale.ENGLISH), currentPhrase,
						StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_LIMITED);
			}

			if (!Utils.isEmpty(tempSelectedStickers))
			{
				stickers.addAll(tempSelectedStickers);

				if (relatedPhraseStartWordIndex == -1)
				{
					relatedPhraseStartWordIndex = wordIndexInText;
				}
				if (relatedPhraseEndWordIndex == -1)
				{
					relatedPhraseEndWordIndex = lastWordIndexInPhraseStartedWithPivot;
				}
			}

			previousPhrase = currentPhrase;
			searchText.setLength(0);
			rawSearchText.setLength(0);
			maxPermutationSize--;
		}

		if ((relatedPhraseStartWordIndex > -1) && (relatedPhraseEndWordIndex == -1))
		{
			relatedPhraseEndWordIndex = wordIndexInText;
		}

		if ((relatedPhraseStartWordIndex > -1) && (relatedPhraseEndWordIndex >= relatedPhraseStartWordIndex) && (currentString != null))
		{
			if ((relatedPhraseStartWordIndex < wordList.size()) && (relatedPhraseEndWordIndex < wordList.size()))
			{
				int firstCharIndex = wordList.get(relatedPhraseStartWordIndex).getStart();
				int lastCharIndex = wordList.get(relatedPhraseEndWordIndex).getEnd();

				if ((firstCharIndex < lastCharIndex) && (lastCharIndex <= currentString.length()))
				{
					relatedPhrase = currentString.substring(firstCharIndex, lastCharIndex);
				}
			}
		}

		return new Pair<String, LinkedHashSet<Sticker>>(relatedPhrase, stickers);
	}

	private LinkedHashSet<Sticker> getOrderedStickers(String rawSearchKey, String searchKey, float minimumMatchingScore)
	{
		Logger.i(TAG, "getOrderedStickers(" + rawSearchKey + ", " + searchKey + ", " + minimumMatchingScore + ")");

		LinkedHashSet<Sticker> stickers = null;
		ArrayList<StickerAppositeDataContainer> cachedStickerData = sCacheForLocalSearch.get(searchKey);

		if (!Utils.isEmpty(cachedStickerData))
		{
			String plainSearchKey = rawSearchKey.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
			// Using unique key for storing ordered stickers for same phrase or word, so that predictive search and exact search could be distinguished.
			String cacheKey = plainSearchKey + StickerSearchConstants.STRING_PREDICATE + minimumMatchingScore;

			if (sCacheForLocalOrderedStickers.containsKey(cacheKey))
			{
				stickers = sCacheForLocalOrderedStickers.get(cacheKey);
			}
			else
			{
				stickers = computeOrderingAndGetStickers(plainSearchKey, cachedStickerData, minimumMatchingScore);
				sCacheForLocalOrderedStickers.put(cacheKey, stickers);
			}
		}

		return stickers;
	}

	private LinkedHashSet<Sticker> computeOrderingAndGetStickers(String matchKey, ArrayList<StickerAppositeDataContainer> stickersData, float minimumMatchingScore)
	{
		Logger.i(TAG, "computeOrderingAndGetStickers(" + matchKey + ", " + stickersData + ", " + minimumMatchingScore + ")");

		LinkedHashSet<Sticker> stickers = null;
		int count = (stickersData == null) ? 0 : stickersData.size();

		if (count > 0)
		{
			int contextMomentCode = ((mMomentCode.getId() == TIME_CODE.UNKNOWN.getId()) ? TIME_CODE.INVALID.getId() : (mMomentCode.getId() + 11));
			int currentMomentTerminalCode = ((mMomentCode.getId() == TIME_CODE.UNKNOWN.getId()) ? TIME_CODE.INVALID.getId() : (mMomentCode.getId() + 2));
			Logger.v(TAG, "computeOrderingAndGetStickers(), context Moment is '" + TIME_CODE.getContinuer(contextMomentCode).name() + "' and terminal Moment is '"
					+ TIME_CODE.getTerminal(currentMomentTerminalCode).name() + "'");

			ArrayList<StickerAppositeDataContainer> timePrioritizedStickerList = new ArrayList<StickerAppositeDataContainer>();
			ArrayList<StickerAppositeDataContainer> eventPrioritizedStickerList = new ArrayList<StickerAppositeDataContainer>();
			ArrayList<StickerAppositeDataContainer> tempStickerDataList = new ArrayList<StickerAppositeDataContainer>();
			TreeSet<StickerAppositeDataContainer> leastButSignificantStickerDataList = new TreeSet<StickerAppositeDataContainer>();
			StickerAppositeDataContainer stickerAppositeDataContainer;

			// Calculate peak frequencies and 2 maximum ranks
			float largestTrendingFrequency = Float.MIN_VALUE;
			float largestLocalFrequency = Float.MIN_VALUE;
			float largestGlobalFrequency = Float.MIN_VALUE;

			float stickerTrendingFrequency;
			float stickerLocalFrequency;
			float stickerGlobalFrequency;

			int secondLargestStickerEventRank = -1;

			for (int i = 0; i < count; i++)
			{
				stickerAppositeDataContainer = stickersData.get(i);
				if (stickerAppositeDataContainer != null)
				{
					// Trending frequency
					stickerTrendingFrequency = stickerAppositeDataContainer.getTrendingFrequency();
					if (stickerTrendingFrequency > largestTrendingFrequency)
					{
						largestTrendingFrequency = stickerTrendingFrequency;
					}

					// Local frequency
					stickerLocalFrequency = stickerAppositeDataContainer.getLocalFrequency();
					if (stickerLocalFrequency > largestLocalFrequency)
					{
						largestLocalFrequency = stickerLocalFrequency;
					}

					// Global frequency
					stickerGlobalFrequency = stickerAppositeDataContainer.getGlobalFrequency();
					if (stickerGlobalFrequency > largestGlobalFrequency)
					{
						largestGlobalFrequency = stickerGlobalFrequency;
					}

					// Second max festive rank
					if ((stickerAppositeDataContainer.getRankOfNowCastEvent() < StickerSearchConstants.MAX_RANK_DURING_EVENT) && (stickerAppositeDataContainer.getRankOfNowCastEvent() > secondLargestStickerEventRank))
					{
						secondLargestStickerEventRank = stickerAppositeDataContainer.getRankOfNowCastEvent();
					}
				}
			}

			// Set to 1.00f to avoid 'Divide by zero' case
			if (largestTrendingFrequency <= 0.00f)
			{
				largestTrendingFrequency = 1.00f;
			}
			if (largestLocalFrequency <= 0.00f)
			{
				largestLocalFrequency = 1.00f;
			}
			if (largestGlobalFrequency <= 0.00f)
			{
				largestGlobalFrequency = 1.00f;
			}

			// Calculate overall score
			for (int i = 0; i < count; i++)
			{
				stickerAppositeDataContainer = stickersData.get(i);
				if (stickerAppositeDataContainer != null)
				{
					int stickerMometCode = stickerAppositeDataContainer.getMomentCode();
					float phraseMatchScore = computeAnalogousScoreForExactMatch(matchKey,
							stickerAppositeDataContainer.getStickerTag().replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY));

					float effectiveFestiveRank = (float) stickerAppositeDataContainer.getRankOfNowCastEvent();
					if (effectiveFestiveRank >= StickerSearchConstants.MAX_RANK_DURING_EVENT)
					{
						effectiveFestiveRank = (float) (secondLargestStickerEventRank + 1) + (float) (effectiveFestiveRank - StickerSearchConstants.MAX_RANK_DURING_EVENT);
					}

					if (stickerAppositeDataContainer.getExactMatchOrder() == -1)
					{
						stickerAppositeDataContainer
								.setScore(
										phraseMatchScore,
										((WEIGHTAGE_MATCH_LATERAL * phraseMatchScore) + 0.00f
												+ (WEIGHTAGE_FREQUENCY_TRENDING * stickerAppositeDataContainer.getTrendingFrequency() / largestTrendingFrequency)
												+ (WEIGHTAGE_FREQUENCY_LOCAL * stickerAppositeDataContainer.getLocalFrequency() / largestLocalFrequency)
												+ (WEIGHTAGE_FREQUENCY_GLOBAL * stickerAppositeDataContainer.getGlobalFrequency() / largestGlobalFrequency) + ((stickerMometCode == contextMomentCode) ? WEIGHTAGE_CONTEXT_MOMENT
												: 0.00f) + ((effectiveFestiveRank < 0) ? 0.00f : StickerSearchConstants.MAXIMUM_FESTIVE_SCORE / (effectiveFestiveRank + 1))));
					}
					else
					{
						stickerAppositeDataContainer
								.setScore(
										phraseMatchScore,
										((WEIGHTAGE_MATCH_LATERAL * phraseMatchScore)
												+ (WEIGHTAGE_EXACT_MATCH * ((phraseMatchScore > LIMIT_EXACT_MATCH) ? phraseMatchScore : 0.00f) / (stickerAppositeDataContainer
														.getExactMatchOrder() + 1))
												+ (WEIGHTAGE_FREQUENCY_TRENDING * stickerAppositeDataContainer.getTrendingFrequency() / largestTrendingFrequency)
												+ (WEIGHTAGE_FREQUENCY_LOCAL * stickerAppositeDataContainer.getLocalFrequency() / largestLocalFrequency)
												+ (WEIGHTAGE_FREQUENCY_GLOBAL * stickerAppositeDataContainer.getGlobalFrequency() / largestGlobalFrequency) + ((stickerMometCode == contextMomentCode) ? WEIGHTAGE_CONTEXT_MOMENT
												: 0.00f) + ((effectiveFestiveRank < 0) ? 0.00f : StickerSearchConstants.MAXIMUM_FESTIVE_SCORE / (effectiveFestiveRank + 1))));
					}					

					if (effectiveFestiveRank > -1)
					{
						eventPrioritizedStickerList.add(stickerAppositeDataContainer);
					}

					if (currentMomentTerminalCode == stickerMometCode)
					{
						timePrioritizedStickerList.add(stickerAppositeDataContainer);
					}
					else if (phraseMatchScore >= minimumMatchingScore)
					{
						tempStickerDataList.add(stickerAppositeDataContainer);
					}
					else
					{
						if (leastButSignificantStickerDataList.size() >= NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL_CONTINUED)
						{
							StickerAppositeDataContainer currentLeastSignificantSticker = leastButSignificantStickerDataList.last();
							if (currentLeastSignificantSticker.compareTo(stickerAppositeDataContainer) == 1)
							{
								leastButSignificantStickerDataList.pollLast();
								leastButSignificantStickerDataList.add(stickerAppositeDataContainer);
							}
						}
						else
						{
							leastButSignificantStickerDataList.add(stickerAppositeDataContainer);
						}
					}
				}
			}

			// Sort festive stickers in descending order and pickup first n stickers, where n = NUMBER_OF_MAX_FESTIVE_PRIORITIZED_STICKERS
			count = eventPrioritizedStickerList.size();
			if (count > 0)
			{
				Collections.sort(eventPrioritizedStickerList);

				for (int i = (count - 1); i >= NUMBER_OF_MAX_FESTIVE_PRIORITIZED_STICKERS; i--)
				{
					eventPrioritizedStickerList.remove(i);
				}
			}

			// Sort in descending order and make a unique list of significant stickers based on ordering w.r.t. score
			count = tempStickerDataList.size();
			if (count > 0)
			{
				stickers = new LinkedHashSet<Sticker>(count);
				Collections.sort(tempStickerDataList);

				for (int i = 0; i < count; i++)
				{
					stickerAppositeDataContainer = tempStickerDataList.get(i);
					stickers.add(StickerManager.getInstance().getStickerFromSetString(stickerAppositeDataContainer.getStickerCode()));
				}

				tempStickerDataList.clear();
				tempStickerDataList = null;
			}
			else
			{
				count = leastButSignificantStickerDataList.size();
				if (count > 0)
				{
					stickers = new LinkedHashSet<Sticker>(count);
					/* Already sorted list as TreeSet */
					for (StickerAppositeDataContainer marginalSticker : leastButSignificantStickerDataList)
					{
						stickers.add(StickerManager.getInstance().getStickerFromSetString(marginalSticker.getStickerCode()));
					}

					leastButSignificantStickerDataList.clear();
					leastButSignificantStickerDataList = null;
				}
			}

			/* Apply time division and event priority, if such stickers are found after ordering */

			// Add event based stickers on first priority and time based stickers on second priority
			if (timePrioritizedStickerList.size() > 0)
			{
				Collections.sort(timePrioritizedStickerList);
				eventPrioritizedStickerList.addAll(timePrioritizedStickerList);
				timePrioritizedStickerList.clear();
			}

			int explicitlyPriortizedStickersCount = eventPrioritizedStickerList.size(); // Combined list of event based and moment based stickers
			if (explicitlyPriortizedStickersCount > 0)
			{
				LinkedHashSet<Sticker> prioritizedStickers = new LinkedHashSet<Sticker>(explicitlyPriortizedStickersCount + count);

				for (int i = 0; i < explicitlyPriortizedStickersCount; i++)
				{
					stickerAppositeDataContainer = eventPrioritizedStickerList.get(i);
					prioritizedStickers.add(StickerManager.getInstance().getStickerFromSetString(stickerAppositeDataContainer.getStickerCode()));
				}

				// Put remaining stickers after time-prioritized stickers in pop-up
				if (count > 0)
				{
					prioritizedStickers.addAll(stickers);
					stickers.clear();
				}
				stickers = prioritizedStickers;
			}
		}

		return stickers;
	}

	private float computeAnalogousScoreForExactMatch(String searchKey, String tag)
	{
		String cacheKey = searchKey + StickerSearchConstants.STRING_PREDICATE + tag;
		Float result = sCacheForLocalAnalogousScore.get(cacheKey);

		if (result == null)
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
				matchCount = matchCount + computeAnalogousSpectrelScore(tagWords, searchWords, StickerSearchUtility.getFirstOrderMoment(searchWordsCount, exactWordsCount));
			}
			result = Math.min(1.00f, (matchCount / maxIndexBound));

			sCacheForLocalAnalogousScore.put(cacheKey, result);
		}

		return result;
	}

	private float computeAnalogousSpectrelScore(ArrayList<String> tagWords, ArrayList<String> searchWords, int maximumPossibleSpectrumSpreading)
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
				specificSpectrumWidth = MARGINAL_FULL_SCORE_LATERAL / (i + 1);
				matchCount = matchCount + (specificSpectrumWidth / maximumPossibleSpectrumSpreading) / (wordMatchIndex + 1);
			}
		}

		return matchCount;
	}

	public void clearTransientResources()
	{
		Logger.i(TAG, "clearTransientResources()");

		sIsHostFinishingSearchTask = true;

		synchronized (sHostInitLock)
		{
			if (mStickers != null)
			{
				for (Sticker sticker : mStickers)
				{
					sticker.clear();
				}
				mStickers.clear();
				mStickers = null;
			}

			if (mPreviousWords != null)
			{
				mPreviousWords.clear();
				mPreviousWords = null;
			}

			mCurrentTextSignificantLength = 0;
			mCurrentText = null;

			mCurrentIndividualChatProfile = null;
			mCurrentGroupChatProfile = null;

			if (mIndividualChatRecord != null)
			{
				Set<String> ids = mIndividualChatRecord.keySet();
				for (String id : ids)
				{
					mIndividualChatRecord.get(id).clear();
				}
				mIndividualChatRecord.clear();
				mIndividualChatRecord = null;
			}

			if (mGroupChatRecord != null)
			{
				Set<String> ids = mGroupChatRecord.keySet();
				for (String id : ids)
				{
					mGroupChatRecord.get(id).clear();
				}
				mGroupChatRecord.clear();
				mGroupChatRecord = null;
			}

			if (mCurrentWords != null)
			{
				mCurrentWords.clear();
				mCurrentWords = null;
			}

			sCacheForLocalSearch.clear();
			sCacheForLocalAnalogousScore.clear();
			sCacheForLocalOrderedStickers.clear();

			mCurrentText = null;

			TextMatchManager.clearResources();

			REGEX_SEPARATORS = null;
			if (SEPARATOR_CHARS != null)
			{
				SEPARATOR_CHARS.clear();
				SEPARATOR_CHARS = null;
			}

			sStickerSearchHostManager = null;
		}

		sIsHostFinishingSearchTask = false;
	}

	private void loadIndividualChatProfile(String contactId)
	{
		Logger.i(TAG, "loadIndividualChatProfile(" + contactId + ")");

		if (mIndividualChatRecord == null)
		{
			mIndividualChatRecord = new HashMap<String, StickerSearchHostManager.IndividualChatProfile>();
		}

		mCurrentIndividualChatProfile = mIndividualChatRecord.get(contactId);
		if (mCurrentIndividualChatProfile == null)
		{
			mCurrentIndividualChatProfile = new IndividualChatProfile(contactId);
		}
	}

	private void loadGroupChatProfile(String groupId)
	{
		Logger.i(TAG, "loadGroupChatProfile(" + groupId + ")");

		if (mGroupChatRecord == null)
		{
			mGroupChatRecord = new HashMap<String, StickerSearchHostManager.GroupChatProfile>();
		}

		mCurrentGroupChatProfile = mGroupChatRecord.get(groupId);
		if (mCurrentGroupChatProfile == null)
		{
			mCurrentGroupChatProfile = new GroupChatProfile(groupId);
		}
	}

	private static class IndividualChatProfile
	{

		private IndividualChatProfile(String id)
		{
			populateChatStory();
		}

		private void populateChatStory()
		{

		}

		private void clear()
		{

		}
	}

	private static class GroupChatProfile
	{

		private GroupChatProfile(String id)
		{
			populateGroupStory();
		}

		private void populateGroupStory()
		{

		}

		private void clear()
		{

		}
	}
}