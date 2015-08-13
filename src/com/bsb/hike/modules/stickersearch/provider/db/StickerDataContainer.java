/**
 * File   : StickerDataContainer.java
 * Content: It is a container class to store all sticker related data for a particular tag.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;

public class StickerDataContainer implements Comparable<StickerDataContainer>
{
	private String mStickerCode;

	private String mTag;

	private String mLanguageFunction;

	private String mStateFunction;

	private String mTagRelatedFrequencyFunction;

	private String mOverallFrequencyFunction;

	private String mStoryThemeFunction;

	private int mExactMatchOrder;

	private int mMomentCode;

	private String mFestivals;

	private int mAge;

	private String mStringsUsedWithSticker;

	private String mStringsNotUsedWithSticker;

	private int mStickerAvailability;

	private float mMatchingScore;

	private float mRecommendationScore;

	private ArrayList<Float> mFrequencies;

	public StickerDataContainer(String stickerCode, String tag, String overallFrequencyFunction, int exactMatchOrder, int momentCode, int stickerAvailability)
	{
		mStickerCode = stickerCode;
		mTag = tag;
		mOverallFrequencyFunction = overallFrequencyFunction;
		mFrequencies = StickerSearchUtility.getIndividualNumericValues(mOverallFrequencyFunction, StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT, Float.class);
		mExactMatchOrder = exactMatchOrder;
		mMomentCode = momentCode;
		mStickerAvailability = stickerAvailability;
		mMatchingScore = 0.0f;
		mRecommendationScore = 0.0f;
	}

	public String getStickerCode()
	{
		return mStickerCode;
	}

	public boolean getStickerAvailabilityStatus()
	{
		return (mStickerAvailability == HikeStickerSearchBaseConstants.DECISION_STATE_YES);
	}

	public String getStickerTag()
	{
		return mTag;
	}

	public String getLanguageFunction()
	{
		return mLanguageFunction;
	}

	public String getStateFunction()
	{
		return mStateFunction;
	}

	public String getTagRelatedFrequencyFunction()
	{
		return mTagRelatedFrequencyFunction;
	}

	public String getOverallFrequencyFunction()
	{
		return mOverallFrequencyFunction;
	}

	public String getStoryThemeFunction()
	{
		return mStoryThemeFunction;
	}

	public int getExactMatchOrder()
	{
		return mExactMatchOrder;
	}

	public int getMomentCode()
	{
		return mMomentCode;
	}

	public String getFestivalList()
	{
		return mFestivals;
	}

	public boolean getStickerAging()
	{
		return (mMomentCode == 0);
	}

	public String getStringsUsedWithSticker()
	{
		return mStringsUsedWithSticker;
	}

	public String getStringsNotUsedWithSticker()
	{
		return mStringsNotUsedWithSticker;
	}

	public void setScore(float matchingScore, float overallScore)
	{
		mMatchingScore = matchingScore;
		mRecommendationScore = overallScore;
	}

	public float getTrendingFrequency()
	{
		if (mFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING)
		{
			return mFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING);
		}
		else
		{
			return StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}
	}

	public float getLocalFrequency()
	{
		if (mFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL)
		{
			return mFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL);
		}
		else
		{
			return StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}
	}

	public float getGlobalFrequency()
	{
		if (mFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL)
		{
			return mFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL);
		}
		else
		{
			return StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}
	}

	public float getMatchingScore()
	{
		return mMatchingScore;
	}

	public float getRecommendationScore()
	{
		return mRecommendationScore;
	}

	@Override
	public boolean equals(Object obj)
	{
		boolean result = (obj != null) && (obj instanceof StickerDataContainer);

		if (result)
		{
			StickerDataContainer comparableObject = (StickerDataContainer) obj;

			result = ((mStickerCode == null) ? (comparableObject.getStickerCode() == null) : mStickerCode.equals(comparableObject.getStickerCode()))
					&& ((mTag == null) ? (comparableObject.getStickerTag() == null) : mTag.equals(comparableObject.getStickerTag()))
					&& ((mLanguageFunction == null) ? (comparableObject.getLanguageFunction() == null) : mLanguageFunction.equals(comparableObject.getLanguageFunction()))
					&& ((mStateFunction == null) ? (comparableObject.getStateFunction() == null) : mStateFunction.equals(comparableObject.getStateFunction()))
					&& ((mTagRelatedFrequencyFunction == null) ? (comparableObject.getTagRelatedFrequencyFunction() == null) : mTagRelatedFrequencyFunction.equals(comparableObject
							.getTagRelatedFrequencyFunction()))
					&& ((mOverallFrequencyFunction == null) ? (comparableObject.getOverallFrequencyFunction() == null) : mOverallFrequencyFunction.equals(comparableObject
							.getOverallFrequencyFunction()))
					&& (mExactMatchOrder == comparableObject.getExactMatchOrder())
					&& (mMomentCode == comparableObject.getMomentCode())
					&& ((mFestivals == null) ? (comparableObject.getFestivalList() == null) : (mFestivals.equals(comparableObject.getFestivalList())))
					&& (mAge == comparableObject.mAge)
					&& ((mStringsUsedWithSticker == null) ? (comparableObject.getStringsUsedWithSticker() == null) : mStringsUsedWithSticker.equals(comparableObject
							.getStringsUsedWithSticker()))
					&& ((mStringsNotUsedWithSticker == null) ? (comparableObject.getStringsNotUsedWithSticker() == null) : mStringsNotUsedWithSticker.equals(comparableObject
							.getStringsNotUsedWithSticker())) && (mStickerAvailability == comparableObject.mStickerAvailability);
		}

		return result;
	}

	@Override
	public String toString()
	{
		return "[sticker_info: " + mStickerCode + ", <tag=" + mTag + "><language_fn=" + mLanguageFunction + "><state_fn=" + mStateFunction + "><tag_fr_fn="
				+ mTagRelatedFrequencyFunction + "><tfr_fn=" + mOverallFrequencyFunction + "><theme_fn=" + mStoryThemeFunction + "><exact_match_order=" + mExactMatchOrder
				+ "><moment_code=" + mMomentCode + "><festival=" + mFestivals + "><age=" + mAge + "><+ve_usage=" + mStringsUsedWithSticker + "><-ve_usage="
				+ mStringsNotUsedWithSticker + ">]";
	}

	@Override
	/*
	 * LHS = RHS ==> return 0; LHS > RHS ==> return -1; LHS < RHS ==> return 1;
	 */
	public int compareTo(StickerDataContainer obj)
	{
		if ((obj == null) || (mRecommendationScore > obj.mRecommendationScore))
		{
			return -1;
		}
		else if (mRecommendationScore == obj.mRecommendationScore)
		{
			if (mMatchingScore == obj.mMatchingScore)
			{
				return 0;
			}
			else if (mMatchingScore > obj.mMatchingScore)
			{
				return -1;
			}
		}

		return 1;
	}
}