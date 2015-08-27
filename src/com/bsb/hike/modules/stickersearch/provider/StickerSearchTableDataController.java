/**
 * File   : StickerSearchTableManager.java
 * Content: It contains dynamically table creation mechanism.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.text.TextUtils;

public class StickerSearchTableDataController
{

	private static final String TAG = StickerSearchTableDataController.class.getSimpleName();

	private static final int INDEX_TABLE_NAME = 0;

	private static final int INDEX_TABLE_PREF_LB = 1;

	private static final int INDEX_TABLE_PREF_UB = 2;

	private static final int INDEX_PRIORITY_REMOVED = 3;

	private static final int INDEX_TAGS_REMOVED_COUNT = 4;

	private static final int SIZE_RESULT_ARRAY = 5;

	private static final int NUMBER_OF_DIVERSE_CHAR = 26;

	private ArrayList<String> mTags; // Input list

	private ArrayList<String> mTables; // Output list, at max 50 elements

	private ArrayList<String> mPrefixLBOfTableContent; // Output list with limited elements

	private ArrayList<String> mPrefixUBOfTableContent; // Output list with limited elements

	private ArrayList<Integer> mPriorityRemovedList; // Output list with limited elements

	private ArrayList<Integer> mTagsRemovedPerPriority; // Output list with limited elements

	private ArrayList<Integer> mPriority; // Input list, lower priority means more important

	private ArrayList<Integer> mTableRemainingCapacity; // Intermediate list

	private String mDefaultTableName;

	private Context mContext;

	private int mTotalCapacityOfAnyTable;

	private int mTotalCapacityRemained;

	private int mCurrentWordsOrder;

	public StickerSearchTableDataController(ArrayList<String> tagArray, Context context)
	{
		mTags = tagArray;
		mTables = null;
		mContext = context;
	}

	/* Find the names and boundaries of tables required to put fts data */
	public void determineTablesRequired(ArrayList<?>[] tagsResultData)
	{
		Logger.d(TAG, "determineTablesRequired()");

		mCurrentWordsOrder = ((mTags == null) ? 0 : mTags.size());
		if ((mCurrentWordsOrder <= 0) || (mContext == null) || (mPriority == null) || (mPriority.size() != mCurrentWordsOrder))
		{
			Logger.e(TAG, "Invalid data for preprocessing. Context = " + mContext + ", currentWordsOrder = " + mCurrentWordsOrder);
		}
		else
		{
			int specialCount = convertIntoConcreteDataAndGetSpecialCount();

			if (mCurrentWordsOrder <= 0)
			{
				Logger.e(TAG, "Invalid data order for preprocessing.");
			}
			else
			{
				// Table list will contain table names and each table will have strings in set [prefix*, prefix*)
				// Any table can contain multiple set of such sets.
				// Each of following lists will contain at max 50 elements or very limited no. of elements.
				// All of following 4 lists will be having ordering dependency.
				mTables = new ArrayList<String>();
				mPrefixLBOfTableContent = new ArrayList<String>();
				mPrefixUBOfTableContent = new ArrayList<String>();
				mTableRemainingCapacity = new ArrayList<Integer>();

				// PriorityRemovedList will contain the priorities, for which tags are eliminated.
				// Each of following lists will contain very limited no. of elements.
				// All of following 2 lists will be having ordering dependency.
				mPriorityRemovedList = new ArrayList<Integer>();
				mTagsRemovedPerPriority = new ArrayList<Integer>();

				mTotalCapacityOfAnyTable = (int) (HikeStickerSearchBaseConstants.MAXIMUM_DYNAMIC_TABLE_CAPACITY * HikeStickerSearchBaseConstants.THRESHOLD_DYNAMIC_TABLE_CAPACITY);
				mTotalCapacityRemained = mTotalCapacityOfAnyTable * HikeStickerSearchBaseConstants.THRESHOLD_DYNAMIC_TABLE_COUNT;

				// Handle all special tags irrespective of word's order
				computeTableForTagsStartingFromSpecialChar(specialCount);

				// Eliminate tags of non-importance, if order is exceeding threshold
				eliminateNonImportantTags();

				// Sort the tags to apply searching operation
				Collections.sort(mTags, String.CASE_INSENSITIVE_ORDER);
				// Initial call to tabulate tags
				computeTableContent(StickerSearchConstants.STRING_EMPTY);

				if (tagsResultData.length < SIZE_RESULT_ARRAY)
				{
					Logger.e(TAG, "Unable to save result of preprocessing.");
				}
				else
				{
					mDefaultTableName = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;

					// Put table names and other attributes in result array
					int totalNumberOfTablesRequired = mPrefixLBOfTableContent.size();
					for (int i = 0; i < totalNumberOfTablesRequired; i++)
					{
						mTables.add(mDefaultTableName + "_" + mPrefixLBOfTableContent.get(i) + "_" + mPrefixUBOfTableContent.get(i));
					}

					tagsResultData[INDEX_TABLE_NAME] = mTables;
					tagsResultData[INDEX_TABLE_PREF_LB] = mPrefixLBOfTableContent;
					tagsResultData[INDEX_TABLE_PREF_UB] = mPrefixUBOfTableContent;
					tagsResultData[INDEX_PRIORITY_REMOVED] = mPriorityRemovedList;
					tagsResultData[INDEX_TAGS_REMOVED_COUNT] = mTagsRemovedPerPriority;
				}
			}
		}
	}

	/* Remove invalid entries as well as words starting from special characters along with counting of such words */
	private int convertIntoConcreteDataAndGetSpecialCount()
	{
		Logger.v(TAG, "convertIntoConcreteDataAndGetSpecialCount()");

		int specialCount = 0;
		int numberOfRemovableWords = 0;
		mCurrentWordsOrder = 0;
		StringBuilder sb = new StringBuilder();

		for (String str : mTags)
		{
			str = ((str == null) ? str : str.trim().toUpperCase(Locale.ENGLISH));
			if (Utils.isBlank(str))
			{
				// Handle invalid tag
				mTags.set(mCurrentWordsOrder, null);
				mPriority.set(mCurrentWordsOrder, null);
				numberOfRemovableWords++;
			}
			else
			{
				// Handle special character within the tag
				str = StickerSearchUtility.formGeneralizedWord(str, sb);
				if (TextUtils.isEmpty(str))
				{
					specialCount++;
					mTags.set(mCurrentWordsOrder, null);
					mPriority.set(mCurrentWordsOrder, null);
					numberOfRemovableWords++;
				}
				else
				{
					mTags.set(mCurrentWordsOrder, str);
				}
			}

			mCurrentWordsOrder++;
		}

		// Remove invalid or special tags
		for (int i = 0; i < numberOfRemovableWords; i++)
		{
			mTags.remove(null);
			mPriority.remove(null);
		}

		mCurrentWordsOrder = mTags.size();

		return specialCount;
	}

	/* Add reserved (default) table for all smiley's and words starting from special characters */
	private void computeTableForTagsStartingFromSpecialChar(int wordsOrder)
	{
		Logger.v(TAG, "computeTagsStartingFromSpecialChar(" + wordsOrder + ")");

		if (wordsOrder > 0)
		{
			mPrefixLBOfTableContent.add(StickerSearchConstants.STRING_EMPTY);
			mPrefixUBOfTableContent.add(StickerSearchConstants.STRING_EMPTY);
			mTableRemainingCapacity.add(mTotalCapacityOfAnyTable - wordsOrder);
			mTotalCapacityRemained -= mTotalCapacityOfAnyTable;
		}
	}

	/*
	 * Recursive call to eliminate non-important words based on priority derived from popularity, if total strength exceeds threshold data size
	 */
	private void eliminateNonImportantTags()
	{
		Logger.v(TAG, "eliminateNonImportantTags()");

		if (mCurrentWordsOrder > mTotalCapacityRemained)
		{
			int nonImportantPriority = getMaxPriority();
			int currentStrength = mCurrentWordsOrder;

			int priority;
			for (int i = 0; (i < mCurrentWordsOrder) && (currentStrength > mTotalCapacityRemained); i++)
			{
				priority = mPriority.get(i);
				if (priority == nonImportantPriority)
				{
					mTags.set(i, null);
					currentStrength--;
				}
			}

			// Remove non-important tags
			int numberOfRemovableWords = mCurrentWordsOrder - currentStrength;
			for (int i = 0; i < numberOfRemovableWords; i++)
			{
				mTags.remove(null);
				mPriority.remove(null);
			}

			mPriorityRemovedList.add(nonImportantPriority);
			mTagsRemovedPerPriority.add(numberOfRemovableWords);
			mCurrentWordsOrder = mTags.size();

			eliminateNonImportantTags();
		}
	}

	/* Get maximum priority w.r.t. its value */
	private int getMaxPriority()
	{
		Logger.v(TAG, "getMaxPriority()");

		return Collections.max(mPriority);
	}

	/* Recursive call to additive determining of tables required so far till we reach to last word */
	private void computeTableContent(String prefixSearchedSoFar)
	{
		Logger.d(TAG, "computeTableContent(" + prefixSearchedSoFar + ")");

		if ((mTags == null) || (mTags.isEmpty()) || (prefixSearchedSoFar == null)
				|| (mTableRemainingCapacity.size() >= HikeStickerSearchBaseConstants.THRESHOLD_DYNAMIC_TABLE_COUNT))
		{
			Logger.e(TAG, "Preprocessing completes on given trial.");
		}
		else
		{
			char charLowerBound;
			char charUpperBound;

			String prefixLowerBound;
			String prefixUpperBound;

			int indexForLastBaseCharacter;
			int indexStartInclusive;
			int indexEndExclusive;
			int count;

			int actualIndexOfLastExistingTable;
			int virtualIndexOfLastExistingTable;
			int remainingCapacityOfExistingTable;

			for (int i = 0; i < NUMBER_OF_DIVERSE_CHAR; i++)
			{
				charLowerBound = (char) (((int) 'A') + i);
				prefixLowerBound = prefixSearchedSoFar + charLowerBound;
				if (charLowerBound == 'Z')
				{
					indexForLastBaseCharacter = prefixSearchedSoFar.length() - 1;
					if (indexForLastBaseCharacter >= 0)
					{
						prefixUpperBound = prefixSearchedSoFar.substring(0, indexForLastBaseCharacter)
								+ (char) (((int) (prefixSearchedSoFar.charAt(indexForLastBaseCharacter))) + 1);
					}
					else
					{
						Logger.v(TAG, "Preprocessing reached to last word, stopping...");
						return;
					}
				}
				else
				{
					charUpperBound = (char) (((int) 'A') + i + 1);
					prefixUpperBound = prefixSearchedSoFar + charUpperBound;
				}

				// Find number of tags found in set [prefixLowerBound*, prefixUpperBound*)
				indexStartInclusive = Collections.binarySearch(mTags, prefixLowerBound);
				indexEndExclusive = Collections.binarySearch(mTags, prefixUpperBound);
				count = Math.min(Math.abs(indexStartInclusive - indexEndExclusive), Math.abs(indexStartInclusive + indexEndExclusive + 1));

				actualIndexOfLastExistingTable = virtualIndexOfLastExistingTable = mPrefixLBOfTableContent.size() - 1;
				// Do not use reserved table, named previously, if any
				if (mPrefixLBOfTableContent.contains(StickerSearchConstants.STRING_EMPTY))
				{
					virtualIndexOfLastExistingTable = actualIndexOfLastExistingTable - 1;
				}

				remainingCapacityOfExistingTable = ((virtualIndexOfLastExistingTable == -1) ? mTotalCapacityOfAnyTable : mTableRemainingCapacity
						.get(actualIndexOfLastExistingTable));
				if ((count > 0) && (count <= remainingCapacityOfExistingTable))
				{
					// Check if new table needs to be named
					if (virtualIndexOfLastExistingTable == -1)
					{
						mPrefixLBOfTableContent.add(prefixLowerBound);
						mPrefixUBOfTableContent.add(prefixUpperBound);
						mTableRemainingCapacity.add(remainingCapacityOfExistingTable - count);
					}
					else
					{
						mPrefixUBOfTableContent.set(actualIndexOfLastExistingTable, prefixUpperBound);
						mTableRemainingCapacity.set(actualIndexOfLastExistingTable, (remainingCapacityOfExistingTable - count));
					}
					mTotalCapacityRemained -= count;

					// Remove tags, which have been computed so far
					int startIndex = (mTags.contains(prefixLowerBound) ? indexStartInclusive : (Math.abs(indexStartInclusive) - 1));
					for (int j = 0; j < count; j++)
					{
						mTags.remove(startIndex++);
					}
				}
				else if (count > 0)
				{
					computeTableContent(prefixLowerBound);
				}
				else
				{
					Logger.v(TAG, "Preprocessing reached to last word for given prefix, stopping...");
				}
			}
		}
	}
}