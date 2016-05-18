package com.bsb.hike.modules.stickersearch.datamodel;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchAnalyticsTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akhiltripathi on 12/04/16.
 */
public class CategorySearchData extends CategoryTagData implements Comparable<CategorySearchData>
{
	private static final String TAG = CategorySearchData.class.getSimpleName();

	private String matchKeyword;

	private StickerCategory category;

	private float genderMatchScore = 0;

	private float packStateScore = 0;

	private float stickerCountScore = 0.0f;

	private float nameMatchScore = 0.0f;

	private float searchScore = Float.MIN_VALUE;

	private final float FOR_USER_GENDER_SCORE = 1.0f;

	private final float FOR_GENERIC_GENDER_SCORE = 0f;

	private final float NOT_FOR_USER_GENDER_SCORE = -1.0f;

	private final float PACK_DOWNLOADED_UPDATE_NOT_AVAILABLE = -1.0f;

	private final float PACK_NOT_DOWNLOADED = 0f;

	private final float PACK_DOWNLOADED_UPDATE_AVAILABLE = 1.0f;

	private final float DEFAULT_STICKER_COUNT_SCORE = 0f;

	private final int GENDER_SCORE_MAP_INDEX = 0;

	private final int PACK_STATE_SCORE_MAP_INDEX = 1;

	private final int STICKER_COUNT_SCORE_MAP_INDEX = 2;

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
		if (Float.compare(searchScore, Float.MIN_VALUE) != 0)
		{
			return searchScore;
		}
        
		if (getCategory() == null)
		{
			return 0;
		}

		List<Float> categoryScores = CategorySearchManager.getInstance().getCategoryScores(getUcid());

		if (!Utils.isEmpty(categoryScores))
		{
			genderMatchScore = categoryScores.get(GENDER_SCORE_MAP_INDEX);
			packStateScore = categoryScores.get(PACK_STATE_SCORE_MAP_INDEX);
			stickerCountScore = categoryScores.get(STICKER_COUNT_SCORE_MAP_INDEX);
		}
		else
		{
			categoryScores = new ArrayList<Float>(3);

			int userGender = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.GENDER, 0);
			genderMatchScore = (userGender == forGender) ? FOR_USER_GENDER_SCORE : (forGender == 0) ? FOR_GENERIC_GENDER_SCORE : NOT_FOR_USER_GENDER_SCORE;
			categoryScores.add(genderMatchScore);

			StickerCategory category = getCategory();

			packStateScore = category.isDownloaded() ? ((category.isUpdateAvailable() || category.isMoreStickerAvailable()) ? PACK_DOWNLOADED_UPDATE_AVAILABLE
					: PACK_DOWNLOADED_UPDATE_NOT_AVAILABLE) : PACK_NOT_DOWNLOADED;
			categoryScores.add(packStateScore);

			stickerCountScore = (category.isDownloaded() || category.getTotalStickers() == 0) ? DEFAULT_STICKER_COUNT_SCORE : category.getDownloadedStickersCount()
					/ category.getTotalStickers();
			categoryScores.add(stickerCountScore);

			CategorySearchManager.getInstance().saveCategoryScores(ucid, categoryScores);
		}

		nameMatchScore = CategorySearchManager.getInstance().computeStringMatchScore(matchKeyword, name);

		float[] featureWeights = CategorySearchManager.getInstance().getFeatureWeights();

		searchScore = (featureWeights[0] * genderMatchScore) + (featureWeights[1] * packStateScore) + (featureWeights[2] * stickerCountScore)
				+ (featureWeights[3] * nameMatchScore);

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

	public JSONObject toJSON() throws JSONException
	{
		JSONObject categorySearchDataJson = new JSONObject();
		categorySearchDataJson.put(HikeConstants.UCID, this.getUcid());

		int shopRank = (getCategory() == null) ? -1 : this.getCategory().getShopRank();
		categorySearchDataJson.put(HikeConstants.RANK, shopRank);

		categorySearchDataJson.put(CategorySearchAnalyticsTask.CATEGORY_SCORE, this.getSearchMatchScore());
		categorySearchDataJson.put(CategorySearchAnalyticsTask.CATEGORY_FEATURE_VECTOR, this.getFeatureVectorJSONs());

		return categorySearchDataJson;
	}

	public JSONArray getFeatureVectorJSONs() throws JSONException
	{
		JSONArray featureVectors = new JSONArray();

		float[] featureWeights = CategorySearchManager.getInstance().getFeatureWeights();

		JSONObject genderMatchScoreJSON = new JSONObject();
		genderMatchScoreJSON.put(HikeConstants.STICKER_SCORE_WEIGHTAGE, featureWeights[0]);
		genderMatchScoreJSON.put(CategorySearchAnalyticsTask.CATEGORY_GENDER_MATCH_SCORE, this.genderMatchScore);
		featureVectors.put(genderMatchScoreJSON);

		JSONObject stateScoreJSON = new JSONObject();
		stateScoreJSON.put(HikeConstants.STICKER_SCORE_WEIGHTAGE, featureWeights[1]);
		stateScoreJSON.put(CategorySearchAnalyticsTask.CATEGORY_STATE_SCORE, this.packStateScore);
		featureVectors.put(stateScoreJSON);

		JSONObject stickerCountScoreJSON = new JSONObject();
		stickerCountScoreJSON.put(HikeConstants.STICKER_SCORE_WEIGHTAGE, featureWeights[2]);
		stickerCountScoreJSON.put(CategorySearchAnalyticsTask.CATEGORY_STICKER_COUNT_SCORE, this.stickerCountScore);
		featureVectors.put(stickerCountScoreJSON);

		JSONObject nameMatchScoreJSON = new JSONObject();
		nameMatchScoreJSON.put(HikeConstants.STICKER_SCORE_WEIGHTAGE, featureWeights[3]);
		nameMatchScoreJSON.put(CategorySearchAnalyticsTask.CATEGORY_NAME_MATCH_SCORE, this.nameMatchScore);
		featureVectors.put(nameMatchScoreJSON);

		return featureVectors;
	}

}
