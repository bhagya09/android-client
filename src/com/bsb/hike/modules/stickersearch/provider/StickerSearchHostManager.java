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

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import android.content.Context;
import android.util.Pair;

public class StickerSearchHostManager
{

	private static final String TAG = StickerSearchHostManager.class.getSimpleName();

	private static final int MAXIMUM_STICKER_CAPACITY = 10;

	private Context mContext;

	private String mCurrentText;

	private int mCurrentTextSignificantLength;

	private int mStart;

	private int mEnd;

	private int mIndexLimit;

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

	private StickerSearchHostManager(Context context)
	{
		mContext = context;
		sWords = new LinkedList<Word>();
		sStickers = new ArrayList<Sticker>(MAXIMUM_STICKER_CAPACITY);
		mCurrentTextSignificantLength = 0;
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

			mCurrentText = null;
			mCurrentTextSignificantLength = 0;
		}
		history.clear();
	}

	public int[] beforeTextChange(CharSequence s, int start, int count, int after)
	{
		return null;
	}

	public Pair<CharSequence, int[][]> onTextChange(CharSequence s, int start, int before, int count)
	{
		Logger.i(TAG, "onTextChanged(), searching start: " + System.currentTimeMillis());

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
			mStart = start;
			mEnd = end;
			mIndexLimit = s.length();

			int brokerLimit = 60;
			boolean isNeedToRemoveLastWord = false;
			if (mIndexLimit > 55)
			{
				mIndexLimit = 55;
				while ((mIndexLimit < s.length()) && !" ".equals(s.charAt(mIndexLimit)))
				{
					mIndexLimit++;
					if (mIndexLimit >= brokerLimit)
					{
						isNeedToRemoveLastWord = true;
						break;
					}
				}

				if (!isNeedToRemoveLastWord)
				{
					mIndexLimit = s.length();
				}
			}

			result = searchAndGetStickerResult(s, start, end, before, count, isNeedToRemoveLastWord);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in searching...", e);
			result = null;
		}

		Logger.i(TAG, "onTextChanged(), searching over time: " + System.currentTimeMillis());

		return result;
	}

	private Pair<CharSequence, int[][]> searchAndGetStickerResult(CharSequence s, int start, int end, int before, int count, boolean isNeedToRemoveLastWord)
	{
		Logger.i(TAG, "searchAndGetStickerResult(" + s + ", [" + start + " - " + end + "], " + before + ", " + count + ", " + isNeedToRemoveLastWord + ")");

		int[][] result = null;
		ArrayList<int[]> tempResult = new ArrayList<int[]>();

		CharSequence wholeString = s;
		if ((mIndexLimit != wholeString.length()) && (mIndexLimit < wholeString.length()))
		{
			s = wholeString.subSequence(0, mIndexLimit);
		}

		Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> cobj = StickerSearchUtility.splitAndDoIndexing(s, " |\n|\t|,|\\.|@");
		ArrayList<String> wordList = cobj.first;
		ArrayList<Integer> startList = null;
		ArrayList<Integer> endList = null;
		int size = (wordList == null ? 0 : wordList.size());

		if (size > 0)
		{
			startList = cobj.second.first;
			endList = cobj.second.second;
			pwords = wordList;
			pstarts = startList;
			pends = endList;

			// remove last word, if needed
			if (isNeedToRemoveLastWord)
			{
				int lastPossibleConsiderableIndex = pends.get(size - 1);
				if (lastPossibleConsiderableIndex < wholeString.length())
				{
					char c = wholeString.charAt(lastPossibleConsiderableIndex);
					if ((c == ' ') || (c == '\n') || (c == '\t') || (c == ',') || (c == '.') || (c == '@'))
					{
						mIndexLimit = lastPossibleConsiderableIndex;
					}
					else
					{
						pwords.remove(size - 1);
						pstarts.remove(size - 1);
						pends.remove(size - 1);
						size = pwords.size();
						if (size > 0)
						{
							mIndexLimit = pends.get(size - 1);
						}
						else
						{
							mIndexLimit = 0;
						}
					}
				}
			}

			String value;
			String nextWord;
			int lastIndexInPhraseStartedWithPivot;
			int maxPermutationSize;
			StringBuilder searchText = new StringBuilder();
			boolean suggestionFoundOnLastValidPhrase = false;
			int previousBoundary = 0;

			for (int i = 0, j = 0; i < size; i++)
			{
				value = wordList.get(i).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);

				if (value.length() > 0)
				{
					suggestionFoundOnLastValidPhrase = false;
					searchText.setLength(0);
					maxPermutationSize = 4;
					nextWord = null;

					// build phrase from a group of 4 words
					searchText.append(value);
					for (lastIndexInPhraseStartedWithPivot = i + 1; maxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
					{
						nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
						if (nextWord.length() == 0)
						{
							continue;
						}
						searchText.append("* ");
						searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * 0.7 + 0.5)) : nextWord));
						maxPermutationSize--;
					}
					lastIndexInPhraseStartedWithPivot--;
					String searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

					if (!history.containsKey(searchKey))
					{
						LinkedHashSet<Sticker> list = new LinkedHashSet<Sticker>();
						Logger.d(TAG, "Phrase \"" + searchKey + "\" was not found in local cache...");

						// phrase stickers
						ArrayList<String> phraseResultList = null;
						if (lastIndexInPhraseStartedWithPivot > i)
						{
							Logger.d(TAG, "Phrase \"" + searchKey + "\" is going to be serached in database...");
							phraseResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(searchKey, false);
							if (phraseResultList != null && phraseResultList.size() > 0)
							{
								for (String stData : phraseResultList)
								{
									list.add(StickerManager.getInstance().getStickerFromSetString(stData));
								}
								Logger.d(TAG, "Filtering phrase stickers before saving in local cache: " + list);

								if (previousBoundary < startList.get(i) || startList.get(i) == 0)
								{
									previousBoundary = endList.get(lastIndexInPhraseStartedWithPivot);
									tempResult.add(new int[] { startList.get(i), previousBoundary });
									Logger.d(TAG, "Making blue due to phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}

								suggestionFoundOnLastValidPhrase = true;
							}
						}

						// add separator between word stickers and phrase stickers
						list.add(null);
						Logger.d(TAG, "Phrase searching is done, now word searching is being started...");

						// word stickers
						LinkedHashSet<Sticker> wordResult = null;
						int actualStartOfWord = startList.get(i);
						int actualEndOfWord = endList.get(i) - 1;
						// determine if exact match is needed
						boolean exactSearch = !((actualStartOfWord > start) ? ((actualStartOfWord <= end) && (actualEndOfWord == end)) : (actualEndOfWord >= end));

						String partialSearchKey = value.toUpperCase(Locale.ENGLISH);
						String wordSearchKey = partialSearchKey + (exactSearch ? "" : "*");

						if (partialSearchKey.length() > 1)
						{
							wordResult = history.get(wordSearchKey);
							if (wordResult == null)
							{
								Logger.d(TAG, "Word \"" + wordSearchKey + "\" was not found in local cache...");

								ArrayList<String> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey, exactSearch);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = new LinkedHashSet<Sticker>();
									for (String stData : wordResultList)
									{
										wordResult.add(StickerManager.getInstance().getStickerFromSetString(stData));
									}
									Logger.d(TAG, "Filtering word stickers before saving in local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);

									history.put(wordSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Word searchKey::" + searchKey + " ==> []");
									history.put(partialSearchKey, new LinkedHashSet<Sticker>());
								}
							}
							else
							{
								Logger.d(TAG, "Filtering word stickers from local cache, searchKey::" + wordSearchKey + " ==> " + wordResult);
							}
						}
						else if (partialSearchKey.length() == 1 && j == 0)
						{
							wordResult = history.get(partialSearchKey);
							if (wordResult == null)
							{
								Logger.d(TAG, "Single word \"" + partialSearchKey + "\" was not found in local cache...");

								ArrayList<String> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(partialSearchKey, true);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = new LinkedHashSet<Sticker>();
									for (String stData : wordResultList)
									{
										wordResult.add(StickerManager.getInstance().getStickerFromSetString(stData));
									}
									Logger.d(TAG, "Filtering single character word stickers before saving in local cache, searchKey::" + partialSearchKey + " ==> " + wordResult);

									history.put(partialSearchKey, wordResult);
								}
								else if (wordResultList == null)
								{
									Logger.i(TAG, "Saving to cache, Single character word searchKey::" + searchKey + " ==> []");
									history.put(partialSearchKey, new LinkedHashSet<Sticker>());
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

							if (!suggestionFoundOnLastValidPhrase && (previousBoundary < startList.get(i) || startList.get(i) == 0))
							{
								previousBoundary = endList.get(i);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
								Logger.d(TAG, "Making blue due to word \"" + partialSearchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
							}
						}
						else
						{
							// remove separator if only one stickers type is present either word or phrase
							list.remove(null);
						}

						if (suggestionFoundOnLastValidPhrase)
						{
							Logger.i(TAG, "Saving to cache, Phrase searchKey::" + searchKey + " ==> " + list);
							history.put(searchKey, list);
						}
						else
						{
							if (lastIndexInPhraseStartedWithPivot > i)
							{
								Logger.i(TAG, "Saving to cache, Phrase searchKey::" + searchKey + " ==> []");
								history.put(searchKey, new LinkedHashSet<Sticker>());

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
									LinkedHashSet<Sticker> savedStickers;

									for (lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
									{
										nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
										if (nextWord.length() == 0)
										{
											continue;
										}
										searchText.append("* ");
										searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * 0.7 + 0.5)) : nextWord));
										currentMaxPermutationSize--;
									}
									lastIndexInPhraseStartedWithPivot--;
									searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);


									currentPhrase = searchKey;
									if (currentPhrase.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = history.get(searchKey);
									if ((savedStickers != null) && (savedStickers.size() > 0))
									{
										if ((previousBoundary < startList.get(i)) || (startList.get(i) == 0))
										{
											int marker = -1;
											int k = 0;
											for (Sticker sticker : savedStickers)
											{
												if (sticker == null)
												{
													marker = k;
													break;
												}
												k++;
											}

											if (marker != 0)
											{
												// word + phrase both searched successfully
												previousBoundary = endList.get(lastIndexInPhraseStartedWithPivot);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to partial phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
											}
											else
											{
												// only word searched successfully
												previousBoundary = endList.get(i);
												tempResult.add(new int[] { startList.get(i), previousBoundary });
												Logger.d(TAG, "Making blue due to individual word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
											}

											break;
										}
									}

									previousPhrase = currentPhrase;
								}
							}
						}
					}
					else
					{
						LinkedHashSet<Sticker> savedStickers = history.get(searchKey);
						if ((savedStickers != null) && (savedStickers.size() > 0))
						{
							if ((previousBoundary < startList.get(i)) || (startList.get(i) == 0))
							{
								int marker = -1;
								int k = 0;
								for (Sticker sticker : savedStickers)
								{
									if (sticker == null)
									{
										marker = k;
										break;
									}
									k++;
								}

								if (marker != 0 && lastIndexInPhraseStartedWithPivot > i)
								{
									// word + phrase both searched successfully
									previousBoundary = endList.get(lastIndexInPhraseStartedWithPivot);
									tempResult.add(new int[] { startList.get(i), previousBoundary });
									Logger.d(TAG, "Making blue due to phrase \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
								}
								else if ((searchKey.length() > 1) || (j == 0))
								{
									// only word searched successfully
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

								for (lastIndexInPhraseStartedWithPivot = i + 1; currentMaxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < size; lastIndexInPhraseStartedWithPivot++)
								{
									nextWord = wordList.get(lastIndexInPhraseStartedWithPivot).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
									if (nextWord.length() == 0)
									{
										continue;
									}
									searchText.append("* ");
									searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * 0.7 + 0.5)) : nextWord));
									currentMaxPermutationSize--;
								}
								lastIndexInPhraseStartedWithPivot--;
								searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

								if (lastIndexInPhraseStartedWithPivot > i)
								{
									currentPhrase = searchKey;
									if (currentPhrase.equals(previousPhrase))
									{
										continue;
									}

									savedStickers = history.get(searchKey);
									if ((savedStickers != null) && (savedStickers.size() > 0))
									{
										if ((previousBoundary < startList.get(i)) || (startList.get(i) == 0))
										{
											int marker = -1;
											int k = 0;
											for (Sticker sticker : savedStickers)
											{
												if (sticker == null)
												{
													marker = k;
													break;
												}
												k++;
											}

											if (marker != 0 && lastIndexInPhraseStartedWithPivot > i)
											{
												// word + phrase both searched successfully
												previousBoundary = endList.get(lastIndexInPhraseStartedWithPivot);
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
									}
								}
								else
								{
									break;
								}

								previousPhrase = currentPhrase;
							}

							// handle individual word
							if (((previousBoundary < startList.get(i)) || (startList.get(i) == 0)) && ((value.length() > 1) || (j == 0)))
							{
								searchKey = value.toUpperCase(Locale.ENGLISH);
								int actualStartOfWord = startList.get(i);
								int actualEndOfWord = endList.get(i) - 1;
								// determine if exact match is needed
								boolean exactSearch = !((actualStartOfWord > start) ? ((actualStartOfWord <= end) && (actualEndOfWord == end)) : (actualEndOfWord >= end));
								if (exactSearch)
								{
									savedStickers = history.get(searchKey);
								}
								else
								{
									savedStickers = history.get(searchKey + "*");
								}

								if (savedStickers != null && (savedStickers.contains(null) ? savedStickers.size() > 1 : savedStickers.size() > 0))
								{
									{
										// only word searched successfully, may be part of earlier phrase search successfully
										previousBoundary = endList.get(i);
										tempResult.add(new int[] { startList.get(i), previousBoundary });
										Logger.d(TAG, "Making blue due to individual  word \"" + searchKey + "\" in [" + startList.get(i) + " - " + previousBoundary + "]");
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
			pwords = wordList;
			pstarts = null;
			pends = null;
			mIndexLimit = 0;
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

		return new Pair<CharSequence, int[][]>(s, result);
	}

	public void onMessageSent(String prevText, Sticker sticker, String nextText)
	{
		Logger.i(TAG, "onMessageSent()");

		if (pwords != null)
		{
			pwords.clear();
			pstarts.clear();
			pends.clear();
		}

		pResult = null;
		history.clear();
	}

	public ArrayList<Sticker> onClickToSendSticker(int where)
	{
		Logger.d(TAG, "onClickToSendSticker(" + where + ")");

		if (pwords == null || pwords.size() == 0)
		{
			return null;
		}

		ArrayList<Sticker> selectedStickers = null;
		LinkedHashSet<Sticker> stickers = null;

		for (int i = 0, j = 0; i < pwords.size(); i++)
		{
			String word = pwords.get(i).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
			if ((where >= (int) pstarts.get(i)) && (where <= pends.get(i)))
			{
				Logger.d(TAG, "Clicked word index = " + i);
				Logger.d(TAG, "Clicked word = " + word);

				if (word.length() > 0)
				{
					stickers = computeProbableStickers(word, i, (j == 0));
				}
				else
				{
					// check pre-words, if valid
					int preIndex = i;
					int preInvalidCount = 0;
					while (--preIndex >= 0)
					{
						if (pwords.get(preIndex).length() > 0)
						{
							preInvalidCount = i - preIndex;
							break;
						}
					}

					// check post-words, if valid
					int postIndex = i;
					int postInvalidCount = 0;
					while (++postIndex < pwords.size())
					{
						if (pwords.get(postIndex).length() > 0)
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
							stickers = computeProbableStickers(word, preIndex, (j == 0));
						}
						else
						{
							stickers = computeProbableStickers(word, postIndex, (j == 0));
						}
					}
				}
				Logger.d(TAG, "Fetched stickers: " + stickers);
				break;
			}
			else if (word.length() > 0)
			{
				j++;
			}
		}

		if (stickers != null && stickers.size() > 0)
		{
			selectedStickers = new ArrayList<Sticker>();
			selectedStickers.addAll(stickers);
		}

		return selectedStickers;
	}

	private LinkedHashSet<Sticker> computeProbableStickers(String word, int wordIndexInText, boolean isFirstValidWord)
	{
		LinkedHashSet<Sticker> stickers = new LinkedHashSet<Sticker>();
		LinkedHashSet<Sticker> tempSelectedStickers = null;

		int maxPermutationSize = 4;
		int currentMaxPermutationSize;
		StringBuilder searchText = new StringBuilder();

		int actualStartOfWord = pstarts.get(wordIndexInText);
		int actualEndOfWord = pends.get(wordIndexInText) - 1;
		// determine if exact match is needed
		boolean exactSearch = !((actualStartOfWord > mStart) ? ((actualStartOfWord <= mEnd) && (actualEndOfWord == mEnd)) : (actualEndOfWord >= mEnd))
				|| (isFirstValidWord && (word.length() == 1));

		// phrase part
		int j;
		int count;
		ArrayList<Sticker> retainList = new ArrayList<Sticker>(8);
		int prePhraseStickersCount = 0;
		ArrayList<String> selectedTextInPhrase = new ArrayList<String>();
		String preWord;
		String currentPhrase;

		for (String previousPhrase = null; maxPermutationSize > 1;)
		{
			j = wordIndexInText - 1;
			count = 1;
			selectedTextInPhrase.add(word);
			currentMaxPermutationSize = maxPermutationSize;

			while (j >= 0 && count < currentMaxPermutationSize)
			{
				preWord = pwords.get(j).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
				if (preWord.length() > 0)
				{
					selectedTextInPhrase.add(preWord);
					count++;
				}
				else
				{
					currentMaxPermutationSize++;
				}

				j--;
			}
			Logger.d(TAG, "Clicked pre-phrase word list in reverse order = " + selectedTextInPhrase);

			if (count > 1)
			{
				String nextWord;
				Collections.reverse(selectedTextInPhrase);
				searchText.append(selectedTextInPhrase.get(0));

				for (j = 1; j < count; j++)
				{
					searchText.append("* ");
					nextWord = selectedTextInPhrase.get(j);
					searchText.append((nextWord.length() > 3) ? nextWord.subSequence(0, (int) (nextWord.length() * 0.7 + 0.5)) : nextWord).toString();
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

				Logger.d(TAG, "Finding stickers for searched pre-phrase \"" + currentPhrase + "\"");
				tempSelectedStickers = history.get(searchText.toString().toUpperCase(Locale.ENGLISH));

				if (tempSelectedStickers != null && (tempSelectedStickers.contains(null) ? tempSelectedStickers.size() > 1 : tempSelectedStickers.size() > 0))
				{
					int marker = 0;
					int markedLimit = 6 - (10 / maxPermutationSize);

					for (Sticker sticker : tempSelectedStickers)
					{
						if (marker >= 6)
						{
							break;
						}
						else if (sticker == null)
						{
							marker--;
							continue;
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

		// word part
		maxPermutationSize = 4;
		String nextWord;
		int lastIndexInPhraseStartedWithPivot;

		for (String previousPhrase = null; maxPermutationSize > 0;)
		{
			// build phrase from a group of some words
			searchText.append(word);
			currentMaxPermutationSize = maxPermutationSize;
			nextWord = null;

			for (lastIndexInPhraseStartedWithPivot = wordIndexInText + 1; currentMaxPermutationSize > 1 && lastIndexInPhraseStartedWithPivot < pwords.size(); lastIndexInPhraseStartedWithPivot++)
			{
				nextWord = pwords.get(lastIndexInPhraseStartedWithPivot).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
				if (nextWord.length() == 0)
				{
					continue;
				}
				searchText.append("* ");
				searchText.append((nextWord.length() > 3 ? nextWord.subSequence(0, (int) (nextWord.length() * 0.7 + 0.5)) : nextWord));
				currentMaxPermutationSize--;
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

			Logger.d(TAG, "Finding stickers for searched phrase \"" + currentPhrase + "\"");
			if (lastIndexInPhraseStartedWithPivot == wordIndexInText && !exactSearch)
			{
				tempSelectedStickers = history.get(currentPhrase + "*");
			}
			else
			{
				tempSelectedStickers = history.get(currentPhrase);
			}

			if (tempSelectedStickers != null && (tempSelectedStickers.contains(null) ? tempSelectedStickers.size() > 1 : tempSelectedStickers.size() > 0))
			{
				stickers.addAll(tempSelectedStickers);
				stickers.remove(null);
			}

			previousPhrase = currentPhrase;
			searchText.setLength(0);
			maxPermutationSize--;
		}

		return stickers;
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

			mContext = null;
			sStickerSearchHostManager = null;
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
					if (mCurrentTextSignificantLength > 0)
					{
						mPreviousWords.add(word.getValue());
						mCurrentTextSignificantLength--;
					}
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

			return ((mValue == null) ? HikeStickerSearchBaseConstants.EMPTY : mValue);
		}

		private ArrayList<String[]> getStickers()
		{

			return mStickerInfo;
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

	// temp code to test
	private static ArrayList<String> pwords = null;

	private static ArrayList<Integer> pstarts = null;

	private static ArrayList<Integer> pends = null;

	private static HashMap<String, LinkedHashSet<Sticker>> history = new HashMap<String, LinkedHashSet<Sticker>>();

	private static int[][] pResult;
}
