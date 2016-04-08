/**
 * File   : StickerAppositeDataContainer.java
 * Content: It is a container class to store all sticker related data for a particular tag.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerEventSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;

import java.util.ArrayList;

public class StickerAppositeDataContainer implements Comparable<StickerAppositeDataContainer>
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

	private int mAge;

	private String mStringsUsedWithSticker;

	private String mStringsNotUsedWithSticker;

	private int mStickerAvailability;

	private float mMatchingScore;

	private float mRecommendationScore;

	private ArrayList<Float> mOverallFrequencies;

	private int mNowCastEventRank;

	public StickerAppositeDataContainer(String stickerCode, String tag, String overallFrequencyFunction, int exactMatchOrder, int momentCode, String timeStampBasedEventsRanks,
			int stickerAvailability)
	{
		mStickerCode = stickerCode;
		mTag = tag;
		mOverallFrequencyFunction = overallFrequencyFunction;
		mOverallFrequencies = StickerSearchUtility.getIndividualNumericValues(mOverallFrequencyFunction, StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT,
				Float.class);
		mExactMatchOrder = exactMatchOrder;
		mMomentCode = momentCode;
		mNowCastEventRank = StickerEventSearchManager.getInstance().getNowCastTimeStampRangeRank(timeStampBasedEventsRanks);
		mStickerAvailability = stickerAvailability;
		mMatchingScore = 0.0f;
		mRecommendationScore = 0.0f;
	}

	public StickerAppositeDataContainer(String stickerCode,  String overallFrequencyFunction,  int momentCode, int stickerAvailability,int age)
	{
		mStickerCode = stickerCode;
		mOverallFrequencyFunction = overallFrequencyFunction;
		mOverallFrequencies = StickerSearchUtility.getIndividualNumericValues(mOverallFrequencyFunction, StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_COUNT, Float.class);
		mMomentCode = momentCode;
		mStickerAvailability = stickerAvailability;
		mMatchingScore = 0.0f;
		mRecommendationScore = 0.0f;
		mAge = age;
	}

	public String getStickerCode()
	{
		return mStickerCode;
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

	public int getRankOfNowCastEvent()
	{
		return mNowCastEventRank;
	}

	public boolean getStickerAging()
	{
		return (mAge > -1);
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

	public int getAge()
	{
		return mAge;
	}

	public void setLanguageFunction(String languageFunction)
	{
		this.mLanguageFunction = mLanguageFunction;
	}

	public float getTrendingFrequency()
	{
		if (mOverallFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING)
		{
			return mOverallFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_TRENDING);
		}
		else
		{
			return StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}
	}

	public float getLocalFrequency()
	{
		if (mOverallFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL)
		{
			return mOverallFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_LOCAL);
		}
		else
		{
			return StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}
	}

	public float getGlobalFrequency()
	{
		if (mOverallFrequencies.size() > StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL)
		{
			return mOverallFrequencies.get(StickerSearchConstants.FREQUENCY_DIVISION_SLOT_PER_STICKER_GLOBAL);
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
	/*
	 * LHS = RHS ==> return 0; LHS > RHS ==> return -1; LHS < RHS ==> return 1;
	 */
	public int compareTo(StickerAppositeDataContainer obj)
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;

		/* Computation must be followed in same order as used in equals() to avoid collision due to same hashCode generated for unequal object */
		result = prime * result + mAge;
		result = prime * result + mExactMatchOrder;
		result = prime * result + mMomentCode;
		result = prime * result + mStickerAvailability;
		result = prime * result + mNowCastEventRank;
		result = prime * result + ((mTag == null) ? 0 : mTag.hashCode());
		result = prime * result + ((mStickerCode == null) ? 0 : mStickerCode.hashCode());
		result = prime * result + ((mLanguageFunction == null) ? 0 : mLanguageFunction.hashCode());
		result = prime * result + ((mOverallFrequencyFunction == null) ? 0 : mOverallFrequencyFunction.hashCode());
		result = prime * result + ((mStateFunction == null) ? 0 : mStateFunction.hashCode());
		result = prime * result + ((mStoryThemeFunction == null) ? 0 : mStoryThemeFunction.hashCode());
		result = prime * result + ((mStringsNotUsedWithSticker == null) ? 0 : mStringsNotUsedWithSticker.hashCode());
		result = prime * result + ((mStringsUsedWithSticker == null) ? 0 : mStringsUsedWithSticker.hashCode());
		result = prime * result + ((mTagRelatedFrequencyFunction == null) ? 0 : mTagRelatedFrequencyFunction.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		StickerAppositeDataContainer other = (StickerAppositeDataContainer) obj;

		/* Compare in order of raw data types to derived data types i.e. comparison must be done earlier for those data types, which takes low comparison-processing time */
		/* Like order can be: Numeric types ---> Strings ---> Collections of numeric values ---> Collections of Strings or, derived classes and so on */
		if (mAge != other.mAge)
		{
			return false;
		}

		if (mExactMatchOrder != other.mExactMatchOrder)
		{
			return false;
		}

		if (mMomentCode != other.mMomentCode)
		{
			return false;
		}

		if (mStickerAvailability != other.mStickerAvailability)
		{
			return false;
		}

		if (mNowCastEventRank != other.mNowCastEventRank)
		{
			return false;
		}

		if (mTag == null)
		{
			if (other.mTag != null)
			{
				return false;
			}
		}
		else if (!mTag.equals(other.mTag))
		{
			return false;
		}

		if (mStickerCode == null)
		{
			if (other.mStickerCode != null)
			{
				return false;
			}
		}
		else if (!mStickerCode.equals(other.mStickerCode))
		{
			return false;
		}

		if (mLanguageFunction == null)
		{
			if (other.mLanguageFunction != null)
			{
				return false;
			}
		}
		else if (!mLanguageFunction.equals(other.mLanguageFunction))
		{
			return false;
		}

		if (mOverallFrequencyFunction == null)
		{
			if (other.mOverallFrequencyFunction != null)
			{
				return false;
			}
		}
		else if (!mOverallFrequencyFunction.equals(other.mOverallFrequencyFunction))
		{
			return false;
		}

		if (mStateFunction == null)
		{
			if (other.mStateFunction != null)
			{
				return false;
			}
		}
		else if (!mStateFunction.equals(other.mStateFunction))
		{
			return false;
		}

		if (mStoryThemeFunction == null)
		{
			if (other.mStoryThemeFunction != null)
			{
				return false;
			}
		}
		else if (!mStoryThemeFunction.equals(other.mStoryThemeFunction))
		{
			return false;
		}

		if (mStringsNotUsedWithSticker == null)
		{
			if (other.mStringsNotUsedWithSticker != null)
			{
				return false;
			}
		}
		else if (!mStringsNotUsedWithSticker.equals(other.mStringsNotUsedWithSticker))
		{
			return false;
		}

		if (mStringsUsedWithSticker == null)
		{
			if (other.mStringsUsedWithSticker != null)
			{
				return false;
			}
		}
		else if (!mStringsUsedWithSticker.equals(other.mStringsUsedWithSticker))
		{
			return false;
		}

		if (mTagRelatedFrequencyFunction == null)
		{
			if (other.mTagRelatedFrequencyFunction != null)
			{
				return false;
			}
		}
		else if (!mTagRelatedFrequencyFunction.equals(other.mTagRelatedFrequencyFunction))
		{
			return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		return "[sticker_info: " + mStickerCode + ", <tag=" + mTag + "><lan_fn=" + mLanguageFunction + "><st_fn=" + mStateFunction + "><tag_fr_fn=" + mTagRelatedFrequencyFunction
				+ "><tfr_fn=" + mOverallFrequencyFunction + "><thm_fn=" + mStoryThemeFunction + "><ext_match_ord=" + mExactMatchOrder + "><mnt_cd=" + mMomentCode + "><event_rk="
				+ mNowCastEventRank + "><age=" + mAge + "><+ve_usage=" + mStringsUsedWithSticker + "><-ve_usage=" + mStringsNotUsedWithSticker + "><match_scr=" + mMatchingScore
				+ "><sr_scr=" + mRecommendationScore + ">]";
	}

	public float getCumalativeNormalisedFrequency(float maxLocalFrequency,float maxTrendingFrequency,float maxGlobalFrequency)
	{
		if(maxLocalFrequency == 0.0f)
		{
			maxLocalFrequency = StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}

		if(maxTrendingFrequency == 0.0f)
		{
			maxTrendingFrequency = StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}

		if(maxGlobalFrequency == 0.0f)
		{
			maxGlobalFrequency = StickerSearchConstants.DEFAULT_FREQUENCY_VALUE;
		}

		return (this.getLocalFrequency()/maxLocalFrequency + this.getTrendingFrequency()/maxTrendingFrequency + this.getGlobalFrequency()/maxGlobalFrequency);
	}
}