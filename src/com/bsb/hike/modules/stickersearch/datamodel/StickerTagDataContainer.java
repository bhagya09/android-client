/**
 * File   : StickerTagDataContainer.java
 * Content: It is a container class to store all tag related data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class StickerTagDataContainer
{
	private static final String TAG = StickerTagDataContainer.class.getSimpleName();

	private String mStickerCode;

	private ArrayList<String> mTags;

	private ArrayList<String> mLanguages;

	private ArrayList<String> mScripts;

	private ArrayList<String> mTagCategories;

	private ArrayList<String> mThemes;

	private ArrayList<Integer> mTagExactMatchPriorities;

	private ArrayList<Integer> mTagPriorities;

	private int mMomentCode;

	private List<StickerEventDataContainer> mEvents;

	private StickerTagDataContainer(StickerTagDataBuilder builder)
	{
		mStickerCode = builder.mStickerCode;
		mTags = builder.mTags;
		mLanguages = builder.mLanguages;
		mScripts = builder.mScripts;
		mTagCategories = builder.mTagCategories;
		mThemes = builder.mThemes;
		mTagExactMatchPriorities = builder.mTagExactMatchPriorities;
		mTagPriorities = builder.mTagPriorities;
		mMomentCode = builder.mMomentCode;
		mEvents = builder.mEvents;
	}

	public String getStickerCode()
	{
		return mStickerCode;
	}

	public boolean getStickerAvailabilityStatus()
	{
		return (mStickerCode == null) ? false : StickerManager.getInstance().getStickerFromSetString(mStickerCode).isStickerAvailable();
	}

	public ArrayList<String> getTagList()
	{
		return mTags;
	}

	public ArrayList<String> getLanguageList()
	{
		return mLanguages;
	}

	public ArrayList<String> getScriptList()
	{
		return mScripts;
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

	public List<StickerEventDataContainer> getFestiveData()
	{
		return mEvents;
	}

	public boolean isValidData()
	{
		boolean result = false;
		int size = (mTags == null) ? 0 : mTags.size();

		if (size > 0)
		{
			result = (mStickerCode != null) && (mLanguages != null) && (mScripts != null) && (mTagCategories != null) && (mThemes != null) && (mTagExactMatchPriorities != null)
					&& (mTagPriorities != null);

			if (result)
			{
				// Language, Script, Category and various Priorities are per tag and themes are per sticker
//				result = (mLanguages.size() == size) && (mScripts.size() == size) && (mTagCategories.size() == size) && (mTagExactMatchPriorities.size() == size)
//						&& (mTagPriorities.size() == size) && (mThemes.size() > 0);
				//temp hack since theme list is not being used currently and also server does not send theme list for tags in regional scripts
				result = (mLanguages.size() == size) && (mScripts.size() == size) && (mTagCategories.size() == size) && (mTagExactMatchPriorities.size() == size)
						&& (mTagPriorities.size() == size) && (mThemes.size() >= 0);
			}
		}
		/* Explicitly, attribute only data was sent by server.
		 * Currently, festival list is the attribute, which can be sent without any tag attached.
		 * Add conditions for other attributes in future as the case may be.
		 */
		else
		{
			result = (mStickerCode != null) && (mEvents != null);
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
				|| ((mMomentCode >= HikeStickerSearchBaseConstants.MOMENT_CODE_MORNING_NON_TERMINAL)
						&& (mMomentCode <= HikeStickerSearchBaseConstants.MOMENT_CODE_NIGHT_NON_TERMINAL));
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
		result = prime * result + ((mLanguages == null) ? 0 : mLanguages.hashCode());
		result = prime * result + ((mScripts == null) ? 0 : mScripts.hashCode());
		result = prime * result + ((mTagCategories == null) ? 0 : mTagCategories.hashCode());
		result = prime * result + ((mTags == null) ? 0 : mTags.hashCode());
		result = prime * result + ((mThemes == null) ? 0 : mThemes.hashCode());
		result = prime * result + ((mEvents == null) ? 0 : mEvents.hashCode());

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
		/* Like order can be: Numeric types ---> Strings ---> Collections of numeric values ---> Collections of Strings or, derived classes and so on */
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

		if (mScripts == null)
		{
			if (other.mScripts != null)
			{
				return false;
			}
		}
		else if (!mScripts.equals(other.mScripts))
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

		if (mEvents == null)
		{
			if (other.mEvents != null)
			{
				return false;
			}
		}
		else if (!mEvents.equals(other.mEvents))
		{
			return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		return "[stkr_info: " + mStickerCode + ", tag_data: {<tag=" + mTags + "><lan=" + mLanguages + "><scr=" + mScripts + "><cat=" + mTagCategories + "><thm=" + mThemes
				+ "><ext_match_ord=" + mTagExactMatchPriorities + "><tag_popularity_ord=" + mTagPriorities + ">}, attrs: {<mnt_cd=" + mMomentCode + "><evt=" + mEvents + "}]";
	}

	public static class StickerTagDataBuilder
	{
		private String mStickerCode;

		private ArrayList<String> mTags;

		private ArrayList<String> mLanguages;

		private ArrayList<String> mScripts;

		private ArrayList<String> mTagCategories;

		private ArrayList<String> mThemes;

		private ArrayList<Integer> mTagExactMatchPriorities;

		private ArrayList<Integer> mTagPriorities;

		private int mMomentCode;

		private List<StickerEventDataContainer> mEvents;

		/* Initial constructor should have all 3 parameters, based on which any sticker can be defined uniquely */
		public StickerTagDataBuilder(String stickerCode, ArrayList<String> tags, ArrayList<String> themes, ArrayList<String> languages)
		{
			mStickerCode = stickerCode;
			mTags = tags;
			mThemes = themes;
			mLanguages = languages;
		}

		public StickerTagDataBuilder tagCategories(ArrayList<String> tagCategories)
		{
			mTagCategories = tagCategories;
			return this;
		}

		public StickerTagDataBuilder scripts(ArrayList<String> scripts)
		{
			mScripts = scripts;
			return this;
		}

		public StickerTagDataBuilder priorities(ArrayList<Integer> tagExactMatchPriorities, ArrayList<Integer> tagPriorities)
		{
			mTagExactMatchPriorities = tagExactMatchPriorities;
			mTagPriorities = tagPriorities;
			return this;
		}

		public StickerTagDataBuilder events(int moment, List<StickerEventDataContainer> events)
		{
			mMomentCode = moment;
			mEvents = events;
			return this;
		}

		public StickerTagDataContainer build()
		{
			return new StickerTagDataContainer(this);
		}
	}
}