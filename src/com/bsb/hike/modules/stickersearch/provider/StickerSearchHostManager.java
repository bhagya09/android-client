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
	}

	public int[] beforeTextChange(CharSequence s, int start, int count, int after)
	{
		return null;
	}

	public Pair<CharSequence, int[][]> onTextChange(CharSequence s, int start, int before, int count)
	{
		Logger.i(TAG, "onTextChanged searching start: " + System.currentTimeMillis());

		try
		{
			int end = ((count > 0) ? (start + count - 1) : start);
			mStart = start;
			mEnd = end;
			return searchAndGetStickerResult(s, start, end, before, count);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in searching...", e);
		}
		Logger.i(TAG, "onTextChanged searching over: " + System.currentTimeMillis());

		return null;
	}

	private Pair<CharSequence, int[][]> searchAndGetStickerResult(CharSequence s, int start, int end, int before, int count)
	{
		Logger.v(TAG, "searchAndGetStickerResult(" + s + ", [" + start + " - " + end + "], " + before + ", " + count + ")");

		int[][] result = null;
		ArrayList<int[]> tempResult = new ArrayList<int[]>();
		if (s.length() > 70)
		{
			int selection = 70;
			while (!" ".equals(s.charAt(selection)) && selection < s.length())
			{
				selection++;
			}
			s = s.subSequence(0, selection);
		}

		Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> cobj = StickerSearchUtility.splitAndDoIndexing(s, " |\n|\t|,|\\.|@");
		ArrayList<String> wordList = cobj.first;
		ArrayList<Integer> startList = null;
		ArrayList<Integer> endList = null;
		int size = (wordList == null ? 0 : wordList.size());

		if (wordList != null && wordList.size() > 0)
		{
			startList = cobj.second.first;
			endList = cobj.second.second;
			pwords = wordList;
			pstarts = startList;
			pends = endList;

			int previousBoundary = 0;
			String value;
			String nextWord;
			StringBuilder searchText = new StringBuilder();
			int lastIndexInPhraseStartedWithPivot;
			int maxPermutationSize;
			boolean suggestionFoundOnLastValidPhrase = false;

			for (int i = 0; i < size; i++)
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
									Logger.d(TAG, "Filtering word stickers before saving in local cache: searchKey::" + wordSearchKey + " ==> " + wordResult);

									history.put(wordSearchKey, wordResult);
								}
							}
							else
							{
								Logger.d(TAG, "Filtering word stickers from local cache: " + wordResult);
							}
						}
						else if (partialSearchKey.length() == 1 && i == 0)
						{
							wordResult = history.get(partialSearchKey);
							if (wordResult == null)
							{
								Logger.d(TAG, "Single word \"" + partialSearchKey + "\" was not found in local cache...");

								ArrayList<String> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(searchKey, true);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = new LinkedHashSet<Sticker>();
									for (String stData : wordResultList)
									{
										wordResult.add(StickerManager.getInstance().getStickerFromSetString(stData));
									}
									Logger.d(TAG, "Filtering single character word stickers before saving in local cache: searchKey::" + partialSearchKey + " ==> " + wordResult);

									history.put(partialSearchKey, wordResult);
								}
							}
							else
							{
								Logger.d(TAG, "Filtering single character word stickers from local cache: " + wordResult);
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
							history.put(searchKey, list);
							Logger.i(TAG, "Phrase searchKey::" + searchKey + " ==> " + list);
						}
						else if (lastIndexInPhraseStartedWithPivot > i)
						{
							history.put(searchKey, new LinkedHashSet<Sticker>());
							Logger.i(TAG, "Phrase searchKey::" + searchKey + " ==> []");
						}
					}
					else if (searchKey.length() > 0)
					{
						LinkedHashSet<Sticker> savedStickers = history.get(searchKey);
						if (savedStickers.size() > 0)
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
								else
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
							// handle individual word
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
								if ((previousBoundary < startList.get(i)) || (startList.get(i) == 0))
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
			}
		}
		else
		{
			pwords = wordList;
			pstarts = null;
			pends = null;
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

		Logger.d(TAG, "Results address: " + Arrays.toString(result));

		return new Pair<CharSequence, int[][]>(s, result);
	}

	public void onSend(CharSequence s)
	{

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

		for (int i = 0; i < pwords.size(); i++)
		{
			if ((where >= (int) pstarts.get(i)) && (where <= pends.get(i)))
			{
				Logger.d(TAG, "Clicked word index = " + i);
				String word = pwords.get(i).replaceAll("\'|\\*", HikeStickerSearchBaseConstants.EMPTY);
				Logger.d(TAG, "Clicked word = " + word);

				if (word.length() > 0)
				{
					stickers = computeProbableStickers(word, i);
				}
				else
				{
					// TO DO
				}
				Logger.d(TAG, "Fetched stickers: " + stickers);
				break;
			}
		}

		if (stickers != null && stickers.size() > 0)
		{
			selectedStickers = new ArrayList<Sticker>();
			selectedStickers.addAll(stickers);
		}

		return selectedStickers;
	}

	private LinkedHashSet<Sticker> computeProbableStickers(String word, int wordIndexInText)
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
				|| ((wordIndexInText == 0) && (word.length() == 1));

		// phrase part
		int j = wordIndexInText - 1;
		ArrayList<Sticker> retainList = new ArrayList<Sticker>(8);
		int prePhraseStickersCount = 0;
		ArrayList<String> selectedTextInPhrase = new ArrayList<String>();
		String preWord;
		String currentPhrase;

		for (String previousPhrase = null; maxPermutationSize > 1;)
		{
			selectedTextInPhrase.add(word);
			int count = 1;
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
			j = wordIndexInText - 1;
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
