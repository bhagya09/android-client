/**
 * File   : StickerTagDataContainer.java
 * Content: It is a container class to store all tag related data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;

public class StickerTagDataContainer
{
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

	@Override
	public boolean equals(Object obj)
	{
		boolean result = (obj != null) && (obj instanceof StickerTagDataContainer);

		if (result)
		{
			StickerTagDataContainer comparableObject = (StickerTagDataContainer) obj;

			result = ((mStickerCode == null) ? (comparableObject.getStickerCode() == null) : mStickerCode.equals(comparableObject.getStickerCode()))
					&& ((mTags == null) ? (comparableObject.getTagList() == null) : mTags.equals(comparableObject.getTagList()))
					&& ((mLanguages == null) ? (comparableObject.getLanguageList() == null) : mLanguages.equals(comparableObject.getLanguageList()))
					&& ((mTagCategories == null) ? (comparableObject.getTagCategoryList() == null) : mTagCategories.equals(comparableObject.getTagCategoryList()))
					&& ((mThemes == null) ? (comparableObject.getThemeList() == null) : mThemes.equals(comparableObject.getThemeList()))
					&& ((mTagExactMatchPriorities == null) ? (comparableObject.getTagExactMatchPriorityList() == null) : mTagExactMatchPriorities.equals(comparableObject
							.getTagExactMatchPriorityList()))
					&& ((mTagPriorities == null) ? (comparableObject.getTagPopularityList() == null) : mTagPriorities.equals(comparableObject.getTagPopularityList()))
					&& (mMomentCode == comparableObject.getMomentCode())
					&& ((mFestivals == null) ? (comparableObject.getFestivalList() == null) : (mFestivals.equals(comparableObject.getFestivalList())));
		}

		return result;
	}

	@Override
	public String toString()
	{
		return "[sticker_info: " + mStickerCode + ", tag_data: {<tag=" + mTags + "><language=" + mLanguages + "><category=" + mTagCategories + "><theme=" + mThemes
				+ "><exact_match_order=" + mTagExactMatchPriorities + "><tag_popularity_order=" + mTagPriorities + ">}, attributes: {<moment_code=" + mMomentCode + "><festival="
				+ mFestivals + "}]";
	}
}