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
		Logger.d(TAG, "onTextChange(" + s + ", " + start + ", " + before + ", " + count + ")");

		int[][] result = null;
		ArrayList<int[]> tempResult = new ArrayList<int[]>();
		// if (!HikeSharedPreferenceUtil.getInstance().getData("isPopulated", false)) return null;

		if (s.length() > 70)
		{
			int selection = 70;
			while (!" ".equals(s.charAt(selection)) && selection < s.length())
			{
				selection++;
			}
			s = s.subSequence(0, selection);
		}

		Pair<ArrayList<String>, Pair<ArrayList<Integer>, ArrayList<Integer>>> cobj = StickerSearchUtility.splitAndDoIndexing(s, " |,|\\.|&|\\?|@");
		ArrayList<String> wordList = cobj.first;
		ArrayList<Integer> startList = null;
		ArrayList<Integer> endList = null;
		int previousBoundary = 0;
		int i = 0;

		if (wordList != null && wordList.size() > 0)
		{
			StringBuilder searchText = new StringBuilder();
			String searchKey;
			int size = wordList.size();
			startList = cobj.second.first;
			endList = cobj.second.second;
			String value;
			int wordCountInPhraseExcludingPivot = 0;

			for (String word : wordList)
			{
				value = word.toUpperCase(Locale.ENGLISH);

				if (value.length() > 0)
				{
					searchText.append(word);
					String nextWord;
					int maxPermutationSize = 3;
					for (int j = 1; j <= maxPermutationSize && i + j < size; j++)
					{
						nextWord = wordList.get(i + j);
						if (nextWord.length() == 0)
						{
							maxPermutationSize++;
							continue;
						}
						searchText.append("* ");
						searchText.append(nextWord.length() > 3 ? nextWord.subSequence(0, 3) : nextWord);
						wordCountInPhraseExcludingPivot++;
					}
					searchKey = searchText.toString().toUpperCase(Locale.ENGLISH);

					if (!history.containsKey(searchKey))
					{
						ArrayList<Sticker> list = new ArrayList<Sticker>();
						Logger.d(TAG, "Phrase\"" + value + "\" was not found in local cache...");
						ArrayList<String> phraseResultList = null;
						if (!searchKey.equals(value))
						{
							phraseResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(searchKey, (i == 0 && searchKey.length() == 1));
						}
						
						// phrase stickers
						if (phraseResultList != null && phraseResultList.size() > 0)
						{
							LinkedHashSet<Sticker> stResules = new LinkedHashSet<Sticker>();
							for (String stData : phraseResultList)
							{
								stResules.add(StickerManager.getInstance().getStickerFromSetString(stData));
							}
							Logger.d(TAG, "Filtering phrase stickers before saving in local cache: " + stResules);

							list.addAll(stResules);

							if (previousBoundary < startList.get(i) || startList.get(i) == 0)
							{
								previousBoundary = endList.get(i + wordCountInPhraseExcludingPivot);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
							}
						}

						// add separator between word stickers and phrase stickers
						list.add(null);

						// word stickers
						ArrayList<Sticker> wordResult = null;
						if (value.length() > 1)
						{
							wordResult = history.get(value);
							if (wordResult == null)
							{
								ArrayList<String> wordResultList = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindStickerList(value, false);
								if (wordResultList != null && wordResultList.size() > 0)
								{
									wordResult = new ArrayList<Sticker>();
									LinkedHashSet<Sticker> stResules = new LinkedHashSet<Sticker>();
									for (String stData : wordResultList)
									{
										stResules.add(StickerManager.getInstance().getStickerFromSetString(stData));
									}
									Logger.d(TAG, "Filtering stickers before saving in local cache: " + stResules);
									wordResult.addAll(stResules);
									history.put(value, wordResult);
								}
							}
							else
							{
								Logger.d(TAG, "Filtering word stickers from local cache: " + wordResult);
							}
						}

						if (wordResult != null && wordResult.size() > 0)
						{
							list.addAll(wordResult);
							if ((phraseResultList == null || phraseResultList.size() == 0) && (previousBoundary < startList.get(i) || startList.get(i) == 0))
							{
								previousBoundary = endList.get(i);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
							}
						}
						else
						{
							list.remove(null); // remove separator if only one stickers type is present either word or phrase
						}

						history.put(searchKey, list);
					}
					else
					{
						if (history.get(searchKey).size() > 0 && (previousBoundary < startList.get(i) || startList.get(i) == 0))
						{
							int marker = history.get(searchKey).indexOf(null);
							if (marker != 0)
							{
								// word + phrase both searched successfully
								previousBoundary = endList.get(i + wordCountInPhraseExcludingPivot);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
							}
							else
							{
								// only word searched successfully
								previousBoundary = endList.get(i);
								tempResult.add(new int[] { startList.get(i), previousBoundary });
							}
						}
					}

					searchText.setLength(0);
				}

				i++;
			}
		}

		int finalSuggestionsCount = tempResult.size();
		if (finalSuggestionsCount > 0)
		{
			if (previousBoundary == endList.get(i - 1))
			{
				int[] lastSuccessBoundary = tempResult.get(finalSuggestionsCount - 1);
				lastSuccessBoundary[1] = s.length();
				tempResult.set(finalSuggestionsCount - 1, lastSuccessBoundary);
			}

			result = new int[finalSuggestionsCount][2];
			for (i = 0; i < finalSuggestionsCount; i++)
			{
				result[i][0] = tempResult.get(i)[0];
				result[i][1] = tempResult.get(i)[1];
			}
		}
		Logger.d(TAG, "Results address: " + Arrays.toString(result));
		pResult = result;
		pwords = wordList;
		pstarts = startList;
		pends = endList;

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

		if (pwords == null)
		{
			return null;
		}

		ArrayList<Sticker> selectedStickers = null;
		LinkedHashSet<Sticker> stickers = null;
		StringBuilder searchText = new StringBuilder();

		for (int i = 0; i < pwords.size(); i++)
		{
			if ((where >= (int) pstarts.get(i)) && (where <= pends.get(i)))
			{
				// phrase part
				String preWord = null;
				int maxPermutationSize = 4;
				int j = i - 1;
				int count;
				boolean eligibility = false;
				ArrayList<String> selectedTextInPhrase = new ArrayList<String>();
				if (pwords.get(i).length() == 0)
				{
					count = 0;
				}
				else
				{
					selectedTextInPhrase.add(pwords.get(i));
					count = 1;
				}
				while (j >= 0 && count < maxPermutationSize)
				{
					if (pwords.get(j).length() > 0)
					{
						selectedTextInPhrase.add(pwords.get(j));
						eligibility = true;
						count++;
					}

					j--;
				}

				if (eligibility)
				{
					Collections.reverse(selectedTextInPhrase);
					searchText.append(selectedTextInPhrase.get(0));
					for (j = 1; j < selectedTextInPhrase.size(); j++)
					{
						searchText.append("* ");
						preWord = selectedTextInPhrase.get(j);
						searchText.append(preWord.length() > 3 ? preWord.subSequence(0, 3) : preWord);
					}

					selectedStickers = history.get(searchText.toString().toUpperCase(Locale.ENGLISH));
					if (selectedStickers != null)
					{
						int marker = selectedStickers.indexOf(null);
						if (marker > 0)
						{
							stickers = new LinkedHashSet<Sticker>();
							marker = Math.min(marker, 5);
							stickers.addAll(selectedStickers.subList(0, marker));
						}
						else if (selectedStickers.size() > 0 && marker < 0)
						{
							stickers = new LinkedHashSet<Sticker>();
							marker = Math.min(selectedStickers.size(), 5);
							stickers.addAll(selectedStickers.subList(0, marker));
						}
					}
				}
				searchText.setLength(0);

				// word part
				searchText.append(pwords.get(i));
				String nextWord = null;
				maxPermutationSize = 3;
				for (j = 1; j <= maxPermutationSize && i + j < pwords.size(); j++)
				{
					nextWord = pwords.get(i + j);
					if (nextWord.length() == 0)
					{
						maxPermutationSize++;
						continue;
					}
					searchText.append("* ");
					searchText.append(nextWord.length() > 3 ? nextWord.subSequence(0, 3) : nextWord);
				}

				selectedStickers = history.get(searchText.toString().toUpperCase(Locale.ENGLISH));
				if (selectedStickers != null && (selectedStickers.contains(null) ? selectedStickers.size() > 1 : selectedStickers.size() > 0))
				{
					if (stickers == null)
					{
						stickers = new LinkedHashSet<Sticker>();
					}
					stickers.addAll(selectedStickers);
					stickers.remove(null);
				}

				Logger.d(TAG, "Fetched stickers: " + stickers);
				break;
			}
		}

		if (stickers != null)
		{
			selectedStickers = new ArrayList<Sticker>();
			selectedStickers.addAll(stickers);
		}

		return selectedStickers;
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

	private static HashMap<String, ArrayList<Sticker>> history = new HashMap<String, ArrayList<Sticker>>();

	private static int[][] pResult;
}
