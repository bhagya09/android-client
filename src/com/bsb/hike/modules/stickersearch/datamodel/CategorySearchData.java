package com.bsb.hike.modules.stickersearch.datamodel;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * Created by akhiltripathi on 12/04/16.
 */
public class CategorySearchData extends CategoryTagData implements Comparable<CategorySearchData>
{
	private static final String TAG = CategorySearchData.class.getSimpleName();

	private String matchKeyword;

	private StickerCategory category;

	private final int FOR_USER_GENDER_SCORE = 1;

	private final int FOR_GENERIC_GENDER_SCORE = 0;

	private final int NOT_FOR_USER_GENDER_SCORE = -1;

	private final int PACK_DOWNLOADED_UPDATE_NOT_AVAILABLE = -1;

	private final int PACK_NOT_DOWNLOADED = 0;

	private final int PACK_DOWNLOADED_UPDATE_AVAILABLE = 1;

	private final float DEFAULT_STICKER_COUNT_SCORE = 0f;

	private CategorySearchData(Builder builder)
	{
		super(builder);
		this.matchKeyword = builder.matchKeyword;
		this.category = builder.category;
	}

	public StickerCategory getCategory()
	{
		if (category == null)
		{
			category = CategorySearchManager.getInstance().getSearchedCategoriesFromCache(ucid);
		}

		return category;
	}

	@Override
	public int compareTo(CategorySearchData another)
	{

		if (this.equals(another))
		{
			return 0;
		}

		if (this.getCategory() == null || another.getCategory() == null)
		{
			return this.name.compareToIgnoreCase(another.name);
		}

		int scoreCompare = Float.compare(another.getSearchMatchScore(), this.getSearchMatchScore());

		if (scoreCompare != 0)
		{
			return scoreCompare;
		}

		return Integer.compare(this.getCategory().getShopRank(), another.getCategory().getShopRank());

	}

	private float getSearchMatchScore()
	{
		int userGender = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.GENDER, 1);
		int genderMatchScore = (userGender == forGender) ? FOR_USER_GENDER_SCORE : (forGender == 0) ? FOR_GENERIC_GENDER_SCORE : NOT_FOR_USER_GENDER_SCORE;

		StickerCategory category = getCategory();

		int packStateScore = category.isDownloaded() ? (category.isUpdateAvailable() ? PACK_DOWNLOADED_UPDATE_AVAILABLE : PACK_DOWNLOADED_UPDATE_NOT_AVAILABLE)
				: PACK_NOT_DOWNLOADED;

		float stickerCountScore = category.isDownloaded() ? DEFAULT_STICKER_COUNT_SCORE : category.getDownloadedStickersCount() / category.getTotalStickers();

		float nameMatchScore = CategorySearchManager.getInstance().computeStringMatchScore(matchKeyword, name);

		float[] featureWeights = CategorySearchManager.getInstance().getFeatureWeights();

		float searchScore = (featureWeights[0] * genderMatchScore) + (featureWeights[1] * packStateScore) + (featureWeights[2] * stickerCountScore)
				+ (featureWeights[3] * nameMatchScore);

		Logger.i(TAG, "Scores for " + ucid + " ( " + name + " ) : genderMatchScore = " + genderMatchScore + " packStateScore = " + packStateScore + " stickerCountScore = "
				+ stickerCountScore + " nameMatchScore = " + nameMatchScore);

		return searchScore;
	}

	public static class Builder extends CategoryTagData.Builder<Builder>
	{

		private String matchKeyword;

		private StickerCategory category;

		public Builder(int ucid)
		{
			super(ucid);
		}

		public Builder setMatchKeyword(String matchKeyword)
		{
			this.matchKeyword = matchKeyword;
			return this;
		}

		public Builder setCategory(StickerCategory category)
		{
			this.category = category;
			return this;
		}

		public CategorySearchData build()
		{
			return new CategorySearchData(this);
		}
	}

}
