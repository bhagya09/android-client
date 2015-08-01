/**
 * File   : StickerSearchHostManager.java
 * Content: It is a provider class to host all kinds of chat search demands.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility.TextMatchManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants.TIME_CODE;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.provider.db.StickerDataContainer;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.util.Pair;

public class StickerSearchHostManager
{
	private static final String TAG = StickerSearchHostManager.class.getSimpleName();

	private String mCurrentText;

	private int mCurrentTextEditingStartIndex;

	private int mCurrentTextEditingEndIndex;

	private int mCurrentTextSignificantLength;

	private volatile ArrayList<String> sCurrentWordsInText = null;

	private volatile ArrayList<Integer> sWordStartIndicesInCurrentText = null;

	private volatile ArrayList<Integer> sWordEndIndicesInCurrentText = null;

	private static HashMap<String, ArrayList<StickerDataContainer>> sCacheForLocalSearch = new HashMap<String, ArrayList<StickerDataContainer>>();

	private static HashMap<String, Float> sCacheForLocalAnalogousScore = new HashMap<String, Float>();

	private static HashMap<String, LinkedHashSet<Sticker>> sCacheForLocalOrderedStickers = new HashMap<String, LinkedHashSet<Sticker>>();

	private TIME_CODE mMomentCode;

	private int NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL;

	private ArrayList<String> mPreviousWords;

	private IndividualChatProfile mCurrentIndividualChatProfile;

	private GroupChatProfile mCurrentGroupChatProfile;

	private static LinkedList<Word> sWords;

	private static ArrayList<Sticker> sStickers;

	private static HashMap<String, IndividualChatProfile> sIndividualChatRecord;

	private static HashMap<String, GroupChatProfile> sGroupChatRecord;

	private static final Object sHostInitLock = new Object();

	private static final Object sHostOperateLock = new Object();

	private static volatile boolean sIsHostFinishingSearchTask;

	private static volatile StickerSearchHostManager sStickerSearchHostManager;

	private static float WEITAGE_MATCH_LATERAL;

	private static float WEITAGE_EXACT_MATCH;

	private static float WEITAGE_FREQUENCY_TRENDING;

	private static float WEITAGE_FREQUENCY_LOCAL;

	private static float WEITAGE_FREQUENCY_GLOBAL;

	private static float WEITAGE_CONTEXT_MOMENT;

	private static float LIMIT_AUTO_CORRECTION;

	private static float LIMIT_EXACT_MATCH;

	private StickerSearchHostManager(Context context)
	{
		sWords = new LinkedList<Word>();
		mCurrentTextSignificantLength = 0;

		NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL = StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance().getApplicationContext()) + 1;

		WEITAGE_MATCH_LATERAL = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_MATCH_LATERAL, StickerSearchConstants.WEITAGE_MATCH_LATERAL);

		WEITAGE_EXACT_MATCH = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_EXACT_MATCH, StickerSearchConstants.WEITAGE_EXACT_MATCH);

		WEITAGE_FREQUENCY_TRENDING = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_FREQUENCY, StickerSearchConstants.WEITAGE_FREQUENCY)
				* HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_FREQUENCY_TRENDING_RATIO, StickerSearchConstants.RATIO_TRENDING_FREQUENCY);

		WEITAGE_FREQUENCY_LOCAL = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_FREQUENCY, StickerSearchConstants.WEITAGE_FREQUENCY)
				* HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_FREQUENCY_LOCAL_RATIO, StickerSearchConstants.RATIO_LOCAL_FREQUENCY);

		WEITAGE_FREQUENCY_GLOBAL = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_FREQUENCY, StickerSearchConstants.WEITAGE_FREQUENCY)
				* HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_FREQUENCY_GLOBAL_RATIO, StickerSearchConstants.RATIO_GLOBAL_FREQUENCY);

		WEITAGE_CONTEXT_MOMENT = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SCORE_WEITAGE_CONTEXT_MOMENT,
				StickerSearchConstants.WEITAGE_CONTEXT_MOMENT);

		LIMIT_AUTO_CORRECTION = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_TAG_LIMIT_AUTO_CORRECTION, StickerSearchConstants.LIMIT_AUTO_CORRECTION);

		LIMIT_EXACT_MATCH = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_TAG_LIMIT_EXACT_MATCH, StickerSearchConstants.LIMIT_EXACT_MATCH);
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
	public void loadChatProfile(String contactId, boolean isGroupChat, long lastMessageTimestamp)
	{
		Logger.v(TAG, "loadChatProfile(" + contactId + ", " + isGroupChat + ")");

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

		if (sCurrentWordsInText != null)
		{
			sCurrentWordsInText.clear();
			sCurrentWordsInText = null;
		}

		if (sWordStartIndicesInCurrentText != null)
		{
			sWordStartIndicesInCurrentText.clear();
			sWordStartIndicesInCurrentText = null;
		}

		if (sWordEndIndicesInCurrentText != null)
		{
			sWordEndIndicesInCurrentText.clear();
			sWordEndIndicesInCurrentText = null;
		}

		sCacheForLocalSearch.clear();
		sCacheForLocalAnalogousScore.clear();
		sCacheForLocalOrderedStickers.clear();

		mCurrentTextSignificantLength = 0;
		mCurrentText = null;
		mMomentCode = StickerSearchUtility.getMomentCode();
	}

	public int[] beforeTextChange(CharSequence s, int start, int count, int after)
	{
		return null;
	}

	public Pair<CharSequence, int[][]> onTextChange(CharSequence s, int start, int before, int count)
	{
		Logger.i(TAG, "onTextChanged(), Searching start: " + System.currentTimeMillis());

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
			if (mCurrentTextSignificantLength > StickerSearchConstants.MAXIMUM_SEARCH_TEXT_LIMIT)
			{
				mCurrentTextSignificantLength = StickerSearchConstants.MAXIMUM_SEARCH_TEXT_LIMIT;
				while ((mCurrentTextSignificantLength < s.length()) && (s.charAt(mCurrentTextSignificantLength) != ' '))
				{
					mCurrentTextSignificantLength++;
					if (mCurrentTextSignificantLength >= StickerSearchConstants.MAXIMUM_SEARCH_TEXT_BROKER_LIMIT)
					{
						isNeedToRemoveLastWord = true;
						break;
					}
				}
			}

			result = searchAndGetStickerResult(s, start, end, before, count, isNeedToRemoveLastWord);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "onTextChanged(), Exception in searching...", e);
			result = null;
		}

		Logger.i(TAG, "onTextChanged(), Searching over time: " + System.currentTimeMillis());

		return result;
	}

	private Pair<CharSequence, int[][]> searchAndGetStickerResult(CharSequence s, int start, int end, int before, int count, boolean isNeedToRemoveLastWord)
	{
		Logger.i(TAG, "searchAndGetStickerResult(" + s + ", [" + start + " - " + end + "], " + before + ", " + count + ", " + isNeedToRemoveLastWord + ")");

		int[][] result = null;
		ArrayList<int[]> tempResult = new ArrayList<int[]>();

		CharSequence wholeString = s;
		if ((mCurrentTextSignificantLength != wholeString.length()) && (mCurrentTextSignificantLength < wholeString.length()))
		{
			s = wholeString.subSequence(0, mCurrentTextSignificantLength);
		}

		Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> cobj = StickerSearchUtility.splitAndDoIndexing(s, " |\n|\t|,|\\.|\\?", 0);
		ArrayList<String> wordList = cobj.first;
		ArrayList<Integer> startList = null;
		ArrayList<Integer> endList = null;
		int size = ((wordList == null) ? 0 : wordList.size());

		if (size > 0)
		{
			startList = cobj.second.first;
			endList = cobj.second.second;
			sCurrentWordsInText = wordList;
			sWordStartIndicesInCurrentText = startList;
			sWordEndIndicesInCurrentText = endList;

			// remove last word, if needed
			if (isNeedToRemoveLastWord)
			{
				int lastPossibleConsiderableIndex = sWordEndIndicesInCurrentText.get(size - 1);
				if (lastPossibleConsiderableIndex < wholeString.length())
				{
					char c = wholeString.charAt(lastPossibleConsiderableIndex);
					if ((c == ' ') || (c == '\n') || (c == '\t') || (c == ',') || (c == '.') || (c == '@'))
					{
						mCurrentTextSignificantLength = lastPossibleConsiderableIndex;
					}
					else
					{
						sCurrentWordsInText.remove(size - 1);
						sWordStartIndicesInCurrentText.remove(size - 1);
						sWordEndIndicesInCurrentText.remove(size - 1);
						size = sCurrentWordsInText.size();
						if (size > 0)
						{
							mCurrentTextSignificantLength = sWordEndIndicesInCurrentText.get(size - 1);
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
			int previousBoundary = 0;

			for (int i = 0, j = 0; i < size; i++)
			{
				value = wordList.get(i).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);

				if (value.length() > 0)
				{
					suggestionFoundOnLastValidPhrase = false;
					suggestionFoundOnLastValidWord = false;
					searchText.setLength(0);
					maxPermutationSize = 4;
					nextWord = null;

					// build phrase from a group of 4 words
					searchText.append(value);

					for (lastWordIndexInPhraseStartedWithPivot = i, lastIndexInPhraseStartedWithPivot = i + 1; maxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
					{
						nextWord = wordList.get(lastIndexInPhraseStartedWithPivot)
								.replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
						if (nextWord.length() == 0)
						{
							continue;
						}
						searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
						searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * LIMIT_AUTO_CORRECTION + 0.5)) : nextWord));
						maxPermutationSize--;
						lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
					}
					lastIndexInPhraseStartedWithPivot--;

					String searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

					if (!sCacheForLocalSearch.containsKey(searchKey))
					{
						ArrayList<StickerDataContainer> list = new ArrayList<StickerDataContainer>();
						Logger.d(TAG, "Phrase \"" + searchKey + "\" was not found in local cache...");

						// phrase sticker data
						ArrayList<StickerDataContainer> phraseResultList = null;
						if (lastWordIndexInPhraseStartedWithPivot > i)
						{
							Logger.d(TAG, "Phrase \"" + searchKey + "\" is going to be serached in database...");
							phraseResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(searchKey, false);
							if (phraseResultList != null && phraseResultList.size() > 0)
							{
								list = phraseResultList;
								Logger.d(TAG, "Filtering phrase stickers before saving in local cache: " + list);

								if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
								{
									previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
									tempResult.add(new int[] { startList.get(i), previousBoundary });
									Logger.d(TAG, "Making blue due to phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}
								else if ((previousBoundary > startList.get(i)) && (previousBoundary < startList.get(lastWordIndexInPhraseStartedWithPivot))
										&& (tempResult.size() > 0))
								{
									previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
									tempResult.get(tempResult.size() - 1)[1] = previousBoundary;
									Logger.d(TAG, "Making blue due to remaining phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}

								suggestionFoundOnLastValidPhrase = true;
							}
						}

						// add separator between word stickers and phrase stickers
						list.add(null);
						Logger.d(TAG, "Phrase searching is done, now word searching is being started...");

						// word sticker data
						ArrayList<StickerDataContainer> wordResult = null;
						int actualStartOfWord = startList.get(i);
						int actualEndOfWord = endList.get(i) - 1;
						// determine if exact match is needed
						boolean exactSearch = !((actualStartOfWord > start) ? ((actualStartOfWord <= end) && (actualEndOfWord == end)) : (actualEndOfWord >= end));

						String partialSearchKey = value.toUpperCase(Locale.ENGLISH);
						String wordSearchKey = partialSearchKey + (exactSearch ? StickerSearchConstants.STRING_EMPTY : StickerSearchConstants.STRING_PREDICATE);

						if (partialSearchKey.length() > 1)
						{
							wordResult = sCacheForLocalSearch.get(wordSearchKey);
							if (wordResult == null)
							{
								Logger.d(TAG, "Word \"" + wordSearchKey + "\" was not found in local cache...");

								ArrayList<StickerDataContainer> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey,
										exactSearch);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = wordResultList;
									Logger.d(TAG, "Filtering word stickers before saving in local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);

									sCacheForLocalSearch.put(wordSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Word searchKey::" + searchKey + " ==> []");
									sCacheForLocalSearch.put(partialSearchKey, new ArrayList<StickerDataContainer>());
								}
							}
							else
							{
								Logger.d(TAG, "Filtering word stickers from local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);
							}
						}
						else if (partialSearchKey.length() == 1 && j == 0)
						{
							wordResult = sCacheForLocalSearch.get(partialSearchKey);
							if (wordResult == null)
							{
								Logger.d(TAG, "Single word \"" + partialSearchKey + "\" was not found in local cache...");

								ArrayList<StickerDataContainer> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey, true);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = wordResultList;
									Logger.d(TAG, "Filtering single character word stickers before saving in local cache, searchKey::" + partialSearchKey + " ==> " + wordResult);

									sCacheForLocalSearch.put(partialSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Single character word searchKey::" + searchKey + " ==> []");
									sCacheForLocalSearch.put(partialSearchKey, new ArrayList<StickerDataContainer>());
								}
							}
							else
							{
								Logger.d(TAG, "Filtering single character word stickers from local cache, searchKey::" + partialSearchKey + " ==> " + wordResult);
							}
						}

						if (wordResult != null && wordResult.size() > 0)
						{
							list.addAll(wordResult);
							suggestionFoundOnLastValidWord = true;
						}
						else
						{
							// remove separator if only phrase search type is present
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
								sCacheForLocalSearch.put(searchKey, new ArrayList<StickerDataContainer>());

								String currentPhrase;
								int currentMaxPermutationSize;
								maxPermutationSize = 3;
								nextWord = null;

								// handle partial phrase of remaining words
								for (String previousPhrase = null; maxPermutationSize > 1; maxPermutationSize--)
								{
									searchText.setLength(0);
									searchText.append(value);
									currentMaxPermutationSize = maxPermutationSize;
									ArrayList<StickerDataContainer> savedStickers;

									for (lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
									{
										nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE,
												StickerSearchConstants.STRING_EMPTY);
										if (nextWord.length() == 0)
										{
											continue;
										}
										searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
										searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * LIMIT_AUTO_CORRECTION + 0.5)) : nextWord));
										currentMaxPermutationSize--;
										lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
									}
									lastIndexInPhraseStartedWithPivot--;

									searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

									currentPhrase = searchKey;
									if (currentPhrase.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = sCacheForLocalSearch.get(searchKey);
									if ((savedStickers != null) && (savedStickers.size() > 0))
									{
										int marker = savedStickers.indexOf(null);
										if (marker != 0)
										{
											// word + phrase both searched successfully
											if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
											{
												previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");

												break;
											}
											else if ((previousBoundary > startList.get(i)) && (previousBoundary < startList.get(lastWordIndexInPhraseStartedWithPivot))
													&& (tempResult.size() > 0))
											{
												previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
												tempResult.get(tempResult.size() - 1)[1] = previousBoundary;
												Logger.d(TAG, "Making blue due to remaining partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - "
														+ previousBoundary + "]");

												break;
											}
										}
										else
										{
											// only word searched successfully
											if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
											{
												previousBoundary = endList.get(i);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to individual word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");

												break;
											}
										}
									}

									previousPhrase = currentPhrase;
								}
							}

							// handle current word
							if (!suggestionFoundOnLastValidPhrase && suggestionFoundOnLastValidWord
									&& ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0))))
							{
								previousBoundary = endList.get(i);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
								Logger.d(TAG, "Making blue due to word \"" + partialSearchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
							}
						}
					}
					else
					{
						ArrayList<StickerDataContainer> savedStickers = sCacheForLocalSearch.get(searchKey);
						if ((savedStickers != null) && (savedStickers.size() > 0))
						{
							int marker = savedStickers.indexOf(null);
							if (marker != 0 && lastWordIndexInPhraseStartedWithPivot > i)
							{
								// word + phrase both searched successfully
								if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
								{
									previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
									tempResult.add(new int[] { startList.get(i), previousBoundary });
									Logger.d(TAG, "Making blue due to phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}
								else if ((previousBoundary > startList.get(i)) && (previousBoundary < startList.get(lastWordIndexInPhraseStartedWithPivot))
										&& (tempResult.size() > 0))
								{
									previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
									tempResult.get(tempResult.size() - 1)[1] = previousBoundary;
									Logger.d(TAG, "Making blue due to remaining partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}
							}
							else if ((searchKey.length() > 1) || (j == 0))
							{
								// only word searched successfully
								if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
								{
									previousBoundary = endList.get(i);
									tempResult.add(new int[] { startList.get(i), previousBoundary });
									Logger.d(TAG, "Making blue due to word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}
							}
						}
						else
						{
							String currentPhrase;
							int currentMaxPermutationSize;
							maxPermutationSize = 3;
							nextWord = null;

							// handle partial phrase of remaining words
							for (String previousPhrase = null; maxPermutationSize > 1; maxPermutationSize--)
							{
								searchText.setLength(0);
								searchText.append(value);
								currentMaxPermutationSize = maxPermutationSize;

								for (lastWordIndexInPhraseStartedWithPivot = i, lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1
										&& lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
								{
									nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE,
											StickerSearchConstants.STRING_EMPTY);
									if (nextWord.length() == 0)
									{
										continue;
									}
									searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
									searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * LIMIT_AUTO_CORRECTION + 0.5)) : nextWord));
									currentMaxPermutationSize--;
									lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
								}
								lastIndexInPhraseStartedWithPivot--;

								searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

								if (lastWordIndexInPhraseStartedWithPivot > i)
								{
									currentPhrase = searchKey;
									if (currentPhrase.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = sCacheForLocalSearch.get(searchKey);
									if ((savedStickers != null) && (savedStickers.size() > 0))
									{
										if ((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0)))
										{
											int marker = savedStickers.indexOf(null);

											if (marker != 0)
											{
												// word + phrase both searched successfully
												previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
											}
											else if ((searchKey.length()) > 1 || (j == 0))
											{
												// only word searched successfully
												previousBoundary = endList.get(i);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to individual word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
											}

											break;
										}
										else if ((previousBoundary > startList.get(i)) && (previousBoundary < startList.get(lastWordIndexInPhraseStartedWithPivot))
												&& (tempResult.size() > 0))
										{
											int marker = savedStickers.indexOf(null);

											if (marker != 0)
											{
												// word + phrase both searched successfully
												previousBoundary = endList.get(lastWordIndexInPhraseStartedWithPivot);
												tempResult.get(tempResult.size() - 1)[1] = previousBoundary;
												Logger.d(TAG, "Making blue due to remaining partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - "
														+ previousBoundary + "]");
											}

											break;
										}
									}
								}
								else
								{
									break;
								}

								previousPhrase = currentPhrase;
							}

							// handle current word
							if (((previousBoundary < startList.get(i)) || ((startList.get(i) == 0) && (previousBoundary == 0))) && ((value.length() > 1) || (j == 0)))
							{
								searchKey = value.toUpperCase(Locale.ENGLISH);
								int actualStartOfWord = startList.get(i);
								int actualEndOfWord = endList.get(i) - 1;
								// determine if exact match is needed
								boolean exactSearch = !((actualStartOfWord > start) ? ((actualStartOfWord <= end) && (actualEndOfWord == end)) : (actualEndOfWord >= end));
								if (exactSearch)
								{
									savedStickers = sCacheForLocalSearch.get(searchKey);
								}
								else
								{
									savedStickers = sCacheForLocalSearch.get(searchKey + StickerSearchConstants.STRING_PREDICATE);
								}

								if (savedStickers != null && (savedStickers.contains(null) ? savedStickers.size() > 1 : savedStickers.size() > 0))
								{
									{
										// only word searched successfully, may be part of earlier phrase search successfully
										previousBoundary = endList.get(i);
										tempResult.add(new int[] { startList.get(i), previousBoundary });
										Logger.d(TAG, "Making blue due to word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
									}
								}
							}
						}
					}

					j++;
				}
			}
		}
		else
		{
			sCurrentWordsInText = wordList;
			sWordStartIndicesInCurrentText = null;
			sWordEndIndicesInCurrentText = null;
			mCurrentTextSignificantLength = 0;
		}

		int finalSuggestionsCount = tempResult.size();
		if (finalSuggestionsCount > 0)
		{
			result = new int[finalSuggestionsCount][2];
			for (int i = 0; i < finalSuggestionsCount; i++)
			{
				result[i][0] = tempResult.get(i)[0];
				result[i][1] = tempResult.get(i)[1];
			}
		}

		Logger.v(TAG, "Results address: " + Arrays.toString(result));

		return new Pair<CharSequence, int[][]>(wholeString, result);
	}

	public void onMessageSent(String prevText, Sticker sticker, String nextText, String currentText)
	{
		Logger.i(TAG, "onMessageSent()");

		if (Utils.isBlank(currentText))
		{
			Logger.i(TAG, "onMessageSent(), resetting all search results...");

			if (sCurrentWordsInText != null)
			{
				sCurrentWordsInText.clear();
				sCurrentWordsInText = null;
			}

			if (sWordStartIndicesInCurrentText != null)
			{
				sWordStartIndicesInCurrentText.clear();
				sWordStartIndicesInCurrentText = null;
			}

			if (sWordEndIndicesInCurrentText != null)
			{
				sWordEndIndicesInCurrentText.clear();
				sWordEndIndicesInCurrentText = null;
			}

			sCacheForLocalSearch.clear();
			sCacheForLocalAnalogousScore.clear();
			sCacheForLocalOrderedStickers.clear();

			mCurrentTextSignificantLength = 0;
			mCurrentText = null;
		}

		mMomentCode = StickerSearchUtility.getMomentCode();
		StickerSearchDataController.getInstance().analyseMessageSent(prevText, sticker, nextText);
	}

	public Pair<Pair<String, String>, ArrayList<Sticker>> onClickToSendSticker(int where)
	{
		Logger.d(TAG, "onClickToSendSticker(" + where + ")");

		ArrayList<String> wordList = sCurrentWordsInText;
		ArrayList<Integer> startIndexList = sWordStartIndicesInCurrentText;
		ArrayList<Integer> endIndexList = sWordEndIndicesInCurrentText;
		String currentString = mCurrentText;

		if ((wordList == null) || (startIndexList == null) || (endIndexList == null) || (wordList.size() == 0) || (wordList.size() != startIndexList.size())
				|| (wordList.size() != endIndexList.size()))
		{
			return null;
		}

		ArrayList<Sticker> selectedStickers = null;
		LinkedHashSet<Sticker> stickers = null;
		Pair<String, LinkedHashSet<Sticker>> results = null;
		String clickedWord = null;
		String clickedPhrase = null;

		for (int i = 0, j = 0; i < wordList.size(); i++)
		{
			String word = wordList.get(i).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
			if ((where >= startIndexList.get(i)) && (where <= endIndexList.get(i)))
			{
				Logger.d(TAG, "Clicked word index = " + i);
				Logger.d(TAG, "Clicked word = " + word);

				if (word.length() > 0)
				{
					results = computeProbableStickers(currentString, wordList, startIndexList, endIndexList, word, i, (j == 0));
					clickedWord = word;
				}
				else
				{
					// check pre-words, if valid
					int preIndex = i;
					int preInvalidCount = 0;
					while (--preIndex >= 0)
					{
						if (wordList.get(preIndex).length() > 0)
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
						if (wordList.get(postIndex).length() > 0)
						{
							postInvalidCount = postInvalidCount - i;
							break;
						}
					}

					if ((preInvalidCount <= 0) && (postInvalidCount <= 0))
					{
						Logger.d(TAG, "onClickToSendSticker(), No valid combination of words is present in current sentence.");
					}
					else
					{
						if (((preInvalidCount <= postInvalidCount) && (preInvalidCount > 0)) || (postInvalidCount <= 0))
						{
							results = computeProbableStickers(currentString, wordList, startIndexList, endIndexList, word, preIndex, (j == 0));
						}
						else
						{
							results = computeProbableStickers(currentString, wordList, startIndexList, endIndexList, word, postIndex, (j == 0));
						}

						clickedWord = word;
					}
				}

				if (results != null)
				{
					clickedPhrase = results.first;
					stickers = results.second;
				}
				Logger.d(TAG, "Fetched stickers: " + stickers);
				break;
			}
			else if (word.length() > 0)
			{
				j++;
			}
		}

		if ((stickers != null) && (stickers.size() > 0))
		{
			selectedStickers = new ArrayList<Sticker>(stickers);
			if (clickedPhrase == null)
			{
				clickedPhrase = clickedWord;
			}
		}

		return new Pair<Pair<String, String>, ArrayList<Sticker>>(new Pair<String, String>(clickedWord, clickedPhrase), selectedStickers);
	}

	private Pair<String, LinkedHashSet<Sticker>> computeProbableStickers(String currentString, ArrayList<String> wordList, ArrayList<Integer> startIndexList,
			ArrayList<Integer> endIndexList, String word, int wordIndexInText, boolean isFirstValidWordOfSingleCharacter)
	{
		LinkedHashSet<Sticker> stickers = new LinkedHashSet<Sticker>();
		LinkedHashSet<Sticker> tempSelectedStickers = null;
		String relatedPhrase = null;
		int relatedPhraseStartWordIndex = -1;
		int relatedPhraseEndWordIndex = -1;

		int maxPermutationSize = 4;
		int currentMaxPermutationSize;
		StringBuilder searchText = new StringBuilder();

		// determine if exact match is needed
		int actualStartOfWord = startIndexList.get(wordIndexInText);
		int actualEndOfWord = endIndexList.get(wordIndexInText) - 1;
		Logger.v(TAG, "ActualStartOfWord = " + actualStartOfWord + ", ActualEndOfWord = " + actualEndOfWord);
		Logger.v(TAG, "CurrentTextEditingStartIndex = " + mCurrentTextEditingStartIndex + ", CurrentTextEditingEndIndex = " + mCurrentTextEditingEndIndex);
		Logger.v(TAG, "isFirstValidWordOfSingleCharacter = " + (isFirstValidWordOfSingleCharacter && (word.length() == 1)));
		boolean exactSearch = !((actualStartOfWord > mCurrentTextEditingStartIndex) ? ((actualStartOfWord <= mCurrentTextEditingEndIndex) && (actualEndOfWord == mCurrentTextEditingEndIndex))
				: (actualEndOfWord >= mCurrentTextEditingEndIndex))
				|| (isFirstValidWordOfSingleCharacter && (word.length() == 1));

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
				preWord = wordList.get(j).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
				if (preWord.length() > 0)
				{
					selectedTextInPhrase.add(preWord);
					firstWordIndexInPhraseEndedWithPivot = j;
					count++;
				}
				else
				{
					currentMaxPermutationSize++;
				}

				j--;
			}
			Logger.i(TAG, "Clicked pre-phrase word list in reverse order = " + selectedTextInPhrase);

			if (count > 1)
			{
				String nextWord;
				Collections.reverse(selectedTextInPhrase);
				String firstWord = selectedTextInPhrase.get(0);
				searchText.append(firstWord);

				for (j = 1; j < count; j++)
				{
					searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
					nextWord = selectedTextInPhrase.get(j);
					searchText.append((nextWord.length() > 3) ? nextWord.subSequence(0, (int) (nextWord.length() * LIMIT_AUTO_CORRECTION + 0.5)) : nextWord).toString();
				}

				currentPhrase = searchText.toString().toUpperCase(Locale.ENGLISH);
				if (currentPhrase.equals(previousPhrase))
				{
					previousPhrase = currentPhrase;
					j = wordIndexInText - 1;
					selectedTextInPhrase.clear();
					searchText.setLength(0);
					maxPermutationSize--;
					continue;
				}

				Logger.i(TAG, "Finding stickers for searched pre-phrase \"" + currentPhrase + "\"");

				// Compute phrase search results having first word significant
				if (firstWord.length() > 1)
				{
					tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_PREDICTIVE);
				}
				// Compute phrase search results having no word significant
				else
				{
					tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_LIMITED);
				}

				if ((tempSelectedStickers != null) && (tempSelectedStickers.size() > 0))
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
			maxPermutationSize--;
		}

		// Shrink stickers for pre-phrase, if more than allowed limit
		if (prePhraseStickersCount > 8)
		{
			stickers.retainAll(retainList);
		}
		retainList.clear();

		// post-phrase and word part
		maxPermutationSize = 4;
		String nextWord;
		int lastIndexInPhraseStartedWithPivot;
		int lastWordIndexInPhraseStartedWithPivot;

		for (String previousPhrase = null; maxPermutationSize > 0;)
		{
			// build phrase from a group of some words
			searchText.append(word);
			currentMaxPermutationSize = maxPermutationSize;
			nextWord = null;

			for (lastWordIndexInPhraseStartedWithPivot = wordIndexInText, lastIndexInPhraseStartedWithPivot = wordIndexInText + 1; currentMaxPermutationSize > 1
					&& lastIndexInPhraseStartedWithPivot < wordList.size(); lastIndexInPhraseStartedWithPivot++)
			{
				nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY);
				if (nextWord.length() == 0)
				{
					continue;
				}
				searchText.append(StickerSearchConstants.STRING_PREDICATE_NEXT);
				searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * LIMIT_AUTO_CORRECTION + 0.5)) : nextWord));
				currentMaxPermutationSize--;
				lastWordIndexInPhraseStartedWithPivot = lastIndexInPhraseStartedWithPivot;
			}
			lastIndexInPhraseStartedWithPivot--;

			currentPhrase = searchText.toString().toUpperCase(Locale.ENGLISH);
			if (currentPhrase.equals(previousPhrase))
			{
				previousPhrase = currentPhrase;
				searchText.setLength(0);
				maxPermutationSize--;
				continue;
			}

			Logger.i(TAG, "Finding stickers for searched phrase \"" + currentPhrase + "\" with exactSearch: " + exactSearch);

			// Compute single word search results
			if (lastWordIndexInPhraseStartedWithPivot == wordIndexInText)
			{
				if (exactSearch)
				{
					if ((currentPhrase.length() == 1) && (isFirstValidWordOfSingleCharacter))
					{
						tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_CHARACTER);
					}
					else if (currentPhrase.length() > 1)
					{
						tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_WORD_EXACT);
					}
				}
				else if (currentPhrase.length() > 1)
				{
					tempSelectedStickers = getOrderedStickers((currentPhrase + StickerSearchConstants.STRING_PREDICATE),
							StickerSearchConstants.MINIMUM_MATCH_SCORE_SINGLE_WORD_PREDICTIVE);
				}
			}
			// Compute phrase search results having first word significant
			else if (word.length() > 1)
			{
				tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_PREDICTIVE);
			}
			// Compute phrase search results having no word significant
			else
			{
				tempSelectedStickers = getOrderedStickers(currentPhrase, StickerSearchConstants.MINIMUM_MATCH_SCORE_PHRASE_LIMITED);
			}

			if (tempSelectedStickers != null && (tempSelectedStickers.size() > 0))
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
			maxPermutationSize--;
		}

		if ((relatedPhraseStartWordIndex > -1) && (relatedPhraseEndWordIndex == -1))
		{
			relatedPhraseEndWordIndex = wordIndexInText;
		}

		if ((relatedPhraseStartWordIndex > -1) && (relatedPhraseEndWordIndex >= relatedPhraseStartWordIndex) && (currentString != null))
		{
			if ((relatedPhraseStartWordIndex < startIndexList.size()) && (relatedPhraseEndWordIndex < endIndexList.size()))
			{
				int firstCharIndex = startIndexList.get(relatedPhraseStartWordIndex);
				int lastCharIndex = endIndexList.get(relatedPhraseEndWordIndex);

				if ((firstCharIndex < lastCharIndex) && (lastCharIndex <= currentString.length()))
				{
					relatedPhrase = currentString.substring(firstCharIndex, lastCharIndex);
				}
			}
		}

		return new Pair<String, LinkedHashSet<Sticker>>(relatedPhrase, stickers);
	}

	private LinkedHashSet<Sticker> getOrderedStickers(String searchKey, float minimumMatchingScore)
	{
		Logger.i(TAG, "getOrderedStickers(" + searchKey + ", " + minimumMatchingScore + ")");

		LinkedHashSet<Sticker> stickers = null;
		Logger.v(TAG, "Existing search keys in cache: " + sCacheForLocalSearch.keySet());

		ArrayList<StickerDataContainer> cachedStickerData = sCacheForLocalSearch.get(searchKey);

		if ((cachedStickerData != null) && (cachedStickerData.size() > 0))
		{
			String plainSearchKey = searchKey.replaceAll(StickerSearchConstants.REGEX_PREDICATE, StickerSearchConstants.STRING_EMPTY);

			if (sCacheForLocalOrderedStickers.containsKey(plainSearchKey))
			{
				stickers = sCacheForLocalOrderedStickers.get(plainSearchKey);
			}
			else
			{
				stickers = computeOrderingAndGetStickers(plainSearchKey, cachedStickerData, minimumMatchingScore);
				sCacheForLocalOrderedStickers.put(plainSearchKey, stickers);
			}
		}

		return stickers;
	}

	private LinkedHashSet<Sticker> computeOrderingAndGetStickers(String searchKey, ArrayList<StickerDataContainer> stickerData, float minimumMatchingScore)
	{
		Logger.i(TAG, "computeOrderingAndGetStickers(" + searchKey + ", " + stickerData + ", " + minimumMatchingScore + ")");

		LinkedHashSet<Sticker> stickers = null;
		int count = (stickerData == null) ? 0 : stickerData.size();

		if (count > 0)
		{
			float matchScoreWeitage = WEITAGE_MATCH_LATERAL;
			float exactMatchWeitage = WEITAGE_EXACT_MATCH;
			float trendingFrequencyWeitage = WEITAGE_FREQUENCY_TRENDING;
			float localFrequencyWeitage = WEITAGE_FREQUENCY_LOCAL;
			float globalFrequencyWeitage = WEITAGE_FREQUENCY_GLOBAL;
			float contextMomentWeitage = WEITAGE_CONTEXT_MOMENT;

			int contextMomentCode = ((mMomentCode.getId() == TIME_CODE.UNKNOWN.getId()) ? TIME_CODE.INVALID.getId() : (mMomentCode.getId() + 11));
			int currentMomentTerminalCode = ((mMomentCode.getId() == TIME_CODE.UNKNOWN.getId()) ? TIME_CODE.INVALID.getId() : (mMomentCode.getId() + 2));
			Logger.v(TAG, "contextMomentCode = " + contextMomentCode + ", currentMomentTerminalCode = " + currentMomentTerminalCode);

			stickers = new LinkedHashSet<Sticker>();

			ArrayList<StickerDataContainer> timePrioritizedStickerList = new ArrayList<StickerDataContainer>();
			ArrayList<StickerDataContainer> tempStickerDataList = new ArrayList<StickerDataContainer>();
			TreeSet<StickerDataContainer> leastButSignificantStickerDataList = new TreeSet<StickerDataContainer>();
			StickerDataContainer stickerDataContainer;

			// Calculate peak frequencies
			float largestTrendingFrequency = Float.MIN_VALUE;
			float largestLocalFrequency = Float.MIN_VALUE;
			float largestGlobalFrequency = Float.MIN_VALUE;

			float stickerTrendingFrequency;
			float stickerLocalFrequency;
			float stickerGlobalFrequency;

			for (int i = 0; i < count; i++)
			{
				stickerDataContainer = stickerData.get(i);
				if (stickerDataContainer != null)
				{
					// Trending frequency
					stickerTrendingFrequency = stickerDataContainer.getTrendingFrequency();
					if (stickerTrendingFrequency > largestTrendingFrequency)
					{
						largestTrendingFrequency = stickerTrendingFrequency;
					}

					// Local frequency
					stickerLocalFrequency = stickerDataContainer.getLocalFrequency();
					if (stickerLocalFrequency > largestLocalFrequency)
					{
						largestLocalFrequency = stickerLocalFrequency;
					}

					// Global frequency
					stickerGlobalFrequency = stickerDataContainer.getGlobalFrequency();
					if (stickerGlobalFrequency > largestGlobalFrequency)
					{
						largestGlobalFrequency = stickerGlobalFrequency;
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
				stickerDataContainer = stickerData.get(i);
				if (stickerDataContainer != null)
				{
					int stickerMometCode = stickerDataContainer.getMomentCode();
					float phraseMatchScore = computeAnalogousScoreForExactMatch(searchKey,
							stickerDataContainer.getStickerTag().replaceAll(StickerSearchConstants.REGEX_SINGLE_OR_PREDICATE, StickerSearchConstants.STRING_EMPTY));

					if (stickerDataContainer.getExactMatchOrder() == -1)
					{
						stickerDataContainer
								.setScore(
										phraseMatchScore,
										((matchScoreWeitage * phraseMatchScore) + 0.00f
												+ (trendingFrequencyWeitage * stickerDataContainer.getTrendingFrequency() / largestTrendingFrequency)
												+ (localFrequencyWeitage * stickerDataContainer.getLocalFrequency() / largestLocalFrequency)
												+ (globalFrequencyWeitage * stickerDataContainer.getGlobalFrequency() / largestGlobalFrequency) + ((stickerMometCode == contextMomentCode) ? contextMomentWeitage
												: 0.00f)));
					}
					else
					{
						stickerDataContainer
								.setScore(
										phraseMatchScore,
										((matchScoreWeitage * phraseMatchScore)
												+ (exactMatchWeitage * ((phraseMatchScore > LIMIT_EXACT_MATCH) ? phraseMatchScore : 0.00f) / (stickerDataContainer
														.getExactMatchOrder() + 1))
												+ (trendingFrequencyWeitage * stickerDataContainer.getTrendingFrequency() / largestTrendingFrequency)
												+ (localFrequencyWeitage * stickerDataContainer.getLocalFrequency() / largestLocalFrequency)
												+ (globalFrequencyWeitage * stickerDataContainer.getGlobalFrequency() / largestGlobalFrequency) + ((stickerMometCode == contextMomentCode) ? contextMomentWeitage
												: 0.00f)));
					}

					if (currentMomentTerminalCode == stickerMometCode)
					{
						timePrioritizedStickerList.add(stickerDataContainer);
					}
					else if (phraseMatchScore >= minimumMatchingScore)
					{
						tempStickerDataList.add(stickerDataContainer);
					}
					else
					{
						if (leastButSignificantStickerDataList.size() >= NUMBER_OF_STICKERS_VISIBLE_IN_ONE_SCROLL)
						{
							StickerDataContainer currentLeastSignificantSticker = leastButSignificantStickerDataList.last();
							if (currentLeastSignificantSticker.compareTo(stickerDataContainer) == 1)
							{
								leastButSignificantStickerDataList.pollLast();
								leastButSignificantStickerDataList.add(stickerDataContainer);
							}
						}
						else
						{
							leastButSignificantStickerDataList.add(stickerDataContainer);
						}
					}
				}
			}

			// Sort in descending order and make a unique list of significant stickers based on ordering w.r.t. score
			count = tempStickerDataList.size();
			if (count > 0)
			{
				Collections.sort(tempStickerDataList);
				for (int i = 0; i < count; i++)
				{
					stickerDataContainer = tempStickerDataList.get(i);
					stickers.add(StickerManager.getInstance().getStickerFromSetString(stickerDataContainer.getStickerCode(), stickerDataContainer.getStickerAvailabilityStatus()));
				}

				tempStickerDataList.clear();
			}
			else if (leastButSignificantStickerDataList.size() > 0)
			{
				for (StickerDataContainer marginalSticker : leastButSignificantStickerDataList)
				{
					stickers.add(StickerManager.getInstance().getStickerFromSetString(marginalSticker.getStickerCode(), marginalSticker.getStickerAvailabilityStatus()));
				}

				leastButSignificantStickerDataList.clear();
			}

			// Apply time division, if such stickers are found after ordering
			int timelyStcikersCount = timePrioritizedStickerList.size();

			if (timelyStcikersCount > 0)
			{
				Collections.sort(timePrioritizedStickerList);
				LinkedHashSet<Sticker> timePrioritizedStickers = new LinkedHashSet<Sticker>();
				for (int i = 0; i < timelyStcikersCount; i++)
				{
					StickerDataContainer timelySticker = timePrioritizedStickerList.get(i);
					timePrioritizedStickers.add(StickerManager.getInstance().getStickerFromSetString(timelySticker.getStickerCode(), timelySticker.getStickerAvailabilityStatus()));
				}

				// Put remaining stickers after time-prioritized stickers in pop-up
				timePrioritizedStickers.addAll(stickers);
				stickers.clear();
				stickers = timePrioritizedStickers;
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
			ArrayList<String> exactWords = StickerSearchUtility.split(tag, StickerSearchConstants.REGEX_SPACE, 0);
			int searchWordsCount = searchWords.size();
			int exactWordsCount = exactWords.size();
			float count = 0;

			String unmatchedSubString;
			float localScore;

			for (int indexInSearchKey = 0; indexInSearchKey < searchWordsCount; indexInSearchKey++)
			{
				for (int indexInTag = 0; indexInTag < exactWordsCount; indexInTag++)
				{
					if (exactWords.get(indexInTag).contains(searchWords.get(indexInSearchKey)))
					{
						unmatchedSubString = exactWords.get(indexInTag).replace(searchWords.get(indexInSearchKey), StickerSearchConstants.STRING_EMPTY);
						localScore = 1.0f - (((float) unmatchedSubString.length()) / exactWords.get(indexInTag).length());

						if (indexInSearchKey == indexInTag)
						{
							count += localScore;
						}
						else if (indexInSearchKey < indexInTag)
						{
							count += localScore * (((float) (indexInSearchKey + 1)) / (indexInTag + 1));
						}
						else
						{
							count += localScore * (((float) (indexInTag + 1)) / (indexInSearchKey + 1));
						}

						break;
					}
				}
			}

			if (searchWordsCount > exactWordsCount)
			{
				result = count / searchWordsCount;
			}
			else
			{
				result = count / exactWordsCount;
			}

			sCacheForLocalAnalogousScore.put(cacheKey, result);
		}

		return result;
	}

	public void clear()
	{
		Logger.d(TAG, "clear()");

		sIsHostFinishingSearchTask = true;
		synchronized (sHostInitLock)
		{

			if (sWords != null)
			{
				for (Word word : sWords)
				{
					word.clear();
				}
				sWords.clear();
				sWords = null;
			}

			if (sStickers != null)
			{
				for (Sticker sticker : sStickers)
				{
					sticker.clear();
				}
				sStickers.clear();
				sStickers = null;
			}

			if (mPreviousWords != null)
			{
				mPreviousWords.clear();
				mPreviousWords = null;
			}

			mCurrentText = null;

			mCurrentIndividualChatProfile = null;
			mCurrentGroupChatProfile = null;

			if (sIndividualChatRecord != null)
			{
				Set<String> ids = sIndividualChatRecord.keySet();
				for (String id : ids)
				{
					sIndividualChatRecord.get(id).clear();
				}
				sIndividualChatRecord.clear();
				sIndividualChatRecord = null;
			}

			if (sGroupChatRecord != null)
			{
				Set<String> ids = sGroupChatRecord.keySet();
				for (String id : ids)
				{
					sGroupChatRecord.get(id).clear();
				}
				sGroupChatRecord.clear();
				sGroupChatRecord = null;
			}

			sStickerSearchHostManager = null;
			TextMatchManager.clearResources();
		}
	}

	private void loadIndividualChatProfile(String contactId)
	{
		Logger.d(TAG, "loadIndividualChatProfile(" + contactId + ")");

		if (sIndividualChatRecord == null)
		{
			sIndividualChatRecord = new HashMap<String, StickerSearchHostManager.IndividualChatProfile>();
		}

		mCurrentIndividualChatProfile = sIndividualChatRecord.get(contactId);
		if (mCurrentIndividualChatProfile == null)
		{
			mCurrentIndividualChatProfile = new IndividualChatProfile(contactId);
		}
	}

	private void loadGroupChatProfile(String groupId)
	{
		Logger.d(TAG, "loadGroupChatProfile(" + groupId + ")");

		if (sGroupChatRecord == null)
		{
			sGroupChatRecord = new HashMap<String, StickerSearchHostManager.GroupChatProfile>();
		}

		mCurrentGroupChatProfile = sGroupChatRecord.get(groupId);
		if (mCurrentGroupChatProfile == null)
		{
			mCurrentGroupChatProfile = new GroupChatProfile(groupId);
		}
	}

	private void setPreviouslyCommunicatedText(String text)
	{
		Logger.v(TAG, "setPreviouslyCommunicatedText()");

		synchronized (sHostOperateLock)
		{
			if (sWords != null)
			{
				if (mPreviousWords != null)
				{
					mPreviousWords.clear();
				}
				else
				{
					mPreviousWords = new ArrayList<String>();
				}
				for (Word word : sWords)
				{
					mPreviousWords.add(word.getValue());
				}
			}
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

	private static class Word
	{
		private static final String TAG = Word.class.getSimpleName();

		private String mValue;

		private int mStartCharIndexInText;

		private int mIndexInText;

		private int mLength;

		private boolean mFlag;

		// Each of following lists has to in order w.r.t. remaining lists
		private ArrayList<String[]> mStickerInfo;

		private ArrayList<Float> mFixedPriorities;

		private ArrayList<String[]> mPositiveVariableData;

		private ArrayList<String[]> mNegativeVariableData;

		private Word(String s, int startCharIndexInText, int indexInText, int length)
		{
			mValue = s;
			mStartCharIndexInText = startCharIndexInText;
			mIndexInText = indexInText;
			mLength = length;
		}

		private void fillStickerData(ArrayList<String> stickerRecognizers, ArrayList<Float> fixedPriority, ArrayList<String> positiveData, ArrayList<String> negativeData)
		{
			int count = ((stickerRecognizers == null) ? 0 : stickerRecognizers.size());
			Logger.d(TAG, "fillStickerData(No. of searched stickers = " + count);

			int i = 0;
			String[] s;
			String[] pd;
			String[] nd;
			for (String info : stickerRecognizers)
			{
				s = info.split(",");
				if (s.length == 2)
				{
					mStickerInfo.add(s);
					mFixedPriorities.add(fixedPriority.get(i));
					pd = positiveData.get(i).split(",");
					mPositiveVariableData.add(pd);
					nd = negativeData.get(i).split(",");
					mNegativeVariableData.add(nd);
				}
			}
		}

		private String getValue()
		{
			return (mValue == null) ? StickerSearchConstants.STRING_EMPTY : mValue;
		}

		private void clear()
		{
			mValue = null;
			if (mStickerInfo != null)
			{
				mStickerInfo.clear();
				mStickerInfo = null;
			}

			if (mFixedPriorities != null)
			{
				mFixedPriorities.clear();
				mFixedPriorities = null;
			}

			if (mPositiveVariableData != null)
			{
				mPositiveVariableData.clear();
				mPositiveVariableData = null;
			}

			if (mNegativeVariableData != null)
			{
				mNegativeVariableData.clear();
				mNegativeVariableData = null;
			}
		}
	}
}