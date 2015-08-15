/**
 * File   : StickerTagDataContainer.java
 * Content: It is a container class to store all tag related data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import java.util.ArrayList;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerTagDataContainer
{
	private static final String TAG = StickerTagDataContainer.class.getSimpleName();

	private String mStickerCode;

	private ArrayList<String> mTags;

	private ArrayList<String> mLanguages;

	private ArrayList<String> mTagCategories;

	private ArrayList<String> mThemes;

	private ArrayList<Integer> mTagExactMatchPriorities;

	private ArrayList<Integer> mTagPriorities;

	private int mMomentCode;

	private String mFestivals;

	public StickerTagDataContainer(String stickerCode, ArrayList<String> tags, ArrayList<String> languages, ArrayList<String> tagCategories, ArrayList<String> themes,
			ArrayList<Integer> tagExactMatchPriorities, ArrayList<Integer> tagPriorities, int moment, String festivals)
	{
		mStickerCode = stickerCode;
		mTags = tags;
		mLanguages = languages;
		mTagCategories = tagCategories;
		mThemes = themes;
		mTagExactMatchPriorities = tagExactMatchPriorities;
		mTagPriorities = tagPriorities;
		mMomentCode = moment;
		mFestivals = festivals;
	}

	public String getStickerCode()
	{
		return mStickerCode;
	}

	public boolean getStickerAvailabilityStatus()
	{
		return (mStickerCode == null) ? false : StickerManager.getInstance().getStickerFromSetString(mStickerCode).getStickerCurrentAvailability();
	}

	public ArrayList<String> getTagList()
	{
		return mTags;
	}

	public ArrayList<String> getLanguageList()
	{
		return mLanguages;
	}

	public ArrayList<String> getTagCategoryList()
	{
		return mTagCategories;
	}

	public ArrayList<String> getThemeList()
	{
		return mThemes;
	}

	public ArrayList<Integer> getTagExactMatchPriorityList()
	{
		return mTagExactMatchPriorities;
	}

	public ArrayList<Integer> getTagPopularityList()
	{
		return mTagPriorities;
	}

	public int getMomentCode()
	{
		return mMomentCode;
	}

	public String getFestivalList()
	{
		return mFestivals;
	}

	public boolean isValidData()
	{
		boolean result = false;
		int size = (mTags == null) ? 0 : mTags.size();

		if (size > 0)
		{
			result = (mStickerCode != null) && (mLanguages != null) && (mTagCategories != null) && (mThemes != null) && (mTagExactMatchPriorities != null)
					&& (mTagPriorities != null) && (mFestivals != null);

			if (result)
			{
				result = (mLanguages.size() == size) && (mTagCategories.size() == size) && (mTagExactMatchPriorities.size() == size) && (mTagPriorities.size() == size)
						&& (mThemes.size() > 0);
			}
		}

		if (!isValidMomentCode())
		{
			Logger.e(TAG, "Moment code is wrong for sticker: " + mStickerCode);
			result = false;
		}

		return result;
	}

	private boolean isValidMomentCode()
	{
		return ((mMomentCode >= HikeStickerSearchBaseConstants.MOMENT_CODE_UNIVERSAL) && (mMomentCode <= HikeStickerSearchBaseConstants.MOMENT_CODE_NIGHT_TERMINAL))
				|| ((mMomentCode >= HikeStickerSearchBaseConstants.MOMENT_CODE_MORNING_NON_TERMINAL) && (mMomentCode <= HikeStickerSearchBaseConstants.MOMENT_CODE_NIGHT_NON_TERMINAL));
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;

		/* Computation must be followed in same order as used in equals() to avoid collision due to same hashCode generated for unequal object */
		result = prime * result + mMomentCode;
		result = prime * result + ((mTagExactMatchPriorities == null) ? 0 : mTagExactMatchPriorities.hashCode());
		result = prime * result + ((mTagPriorities == null) ? 0 : mTagPriorities.hashCode());
		result = prime * result + ((mStickerCode == null) ? 0 : mStickerCode.hashCode());
		result = prime * result + ((mFestivals == null) ? 0 : mFestivals.hashCode());
		result = prime * result + ((mLanguages == null) ? 0 : mLanguages.hashCode());
		result = prime * result + ((mTagCategories == null) ? 0 : mTagCategories.hashCode());
		result = prime * result + ((mTags == null) ? 0 : mTags.hashCode());
		result = prime * result + ((mThemes == null) ? 0 : mThemes.hashCode());

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

		StickerTagDataContainer other = (StickerTagDataContainer) obj;

		/* Compare in order of raw data types to derived data types i.e. comparison must be done earlier for those data types, which takes low comparison-processing time */
		/* Like order cab be: Numeric types ---> Strings ---> Collections of numeric values ---> Collections of Strings or, derived classes */
		if (mMomentCode != other.mMomentCode)
		{
			return false;
		}

		if (mTagExactMatchPriorities == null)
		{
			if (other.mTagExactMatchPriorities != null)
			{
				return false;
			}
		}
		else if (!mTagExactMatchPriorities.equals(other.mTagExactMatchPriorities))
		{
			return false;
		}

		if (mTagPriorities == null)
		{
			if (other.mTagPriorities != null)
			{
				return false;
			}
		}
		else if (!mTagPriorities.equals(other.mTagPriorities))
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

		if (mFestivals == null)
		{
			if (other.mFestivals != null)
			{
				return false;
			}
		}
		else if (!mFestivals.equals(other.mFestivals))
		{
			return false;
		}

		if (mLanguages == null)
		{
			if (other.mLanguages != null)
			{
				return false;
			}
		}
		else if (!mLanguages.equals(other.mLanguages))
		{
			return false;
		}

		if (mTagCategories == null)
		{
			if (other.mTagCategories != null)
			{
				return false;
			}
		}
		else if (!mTagCategories.equals(other.mTagCategories))
		{
			return false;
		}

		if (mTags == null)
		{
			if (other.mTags != null)
			{
				return false;
			}
		}
		else if (!mTags.equals(other.mTags))
		{
			return false;
		}

		if (mThemes == null)
		{
			if (other.mThemes != null)
			{
				return false;
			}
		}
		else if (!mThemes.equals(other.mThemes))
		{
			return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		return "[stkr_info: " + mStickerCode + ", tag_data: {<tag=" + mTags + "><lan=" + mLanguages + "><cat=" + mTagCategories + "><thm=" + mThemes + "><ext_match_ord="
				+ mTagExactMatchPriorities + "><tag_popularity_ord=" + mTagPriorities + ">}, attrs: {<mnt_cd=" + mMomentCode + "><fest=" + mFestivals + "}]";
	}
}