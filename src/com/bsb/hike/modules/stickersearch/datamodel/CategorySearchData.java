package com.bsb.hike.modules.stickersearch.datamodel;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

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

	public CategorySearchData(String categoryID)
	{
		super(categoryID);
	}

	public StickerCategory getCategory()
	{
		if (category == null)
		{
			category = CategorySearchManager.getInstance().getSearchedCategoriesFromCache(categoryID);
		}

		return category;
	}

	@Override
	public String toString()
	{
		return categoryID + " : name = " + name + "; language = " + language + "; theme = " + theme + "; gender = " + forGender + "; keys = " + getKeywordsSet();
	}

	public void setMatchKeyword(String matchKeyword)
	{
		this.matchKeyword = matchKeyword;
	}

	@Override
	public int compareTo(CategorySearchData another)
	{
		Logger.e(TAG, "CTD compareTo() " + this.name + " <> " + another.name);

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

		Logger.i(TAG, "Scores for " + categoryID + " ( " + name + " ) : genderMatchScore = " + genderMatchScore + " packStateScore = " + packStateScore + " stickerCountScore = "
				+ stickerCountScore + " nameMatchScore = " + nameMatchScore);

		return searchScore;
	}

}
