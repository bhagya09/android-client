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
 *
 * Model class to store Category Search Information
 *
 */
public class CategorySearchData extends CategoryTagData implements Comparable<CategorySearchData>
{
	private static final String TAG = CategorySearchData.class.getSimpleName();

	private String matchKeyword; //Keyword for which the category was searched

	private StickerCategory category;

	private float genderMatchScore = 0; // Score representing that if the targeted gender of the category is same as the user's gender

	private float packStateScore = 0; // Score representing the category state : if its downloaded, is update available for a downloaded category,etc.

	private float stickerCountScore = 0.0f; // Score representing how popular the stickers in the category for the user before he downloads the pack

	private float nameMatchScore = 0.0f; // Score representing String Diff score of the matchKeyword and the Category Display Name

	private float searchScore = Float.MIN_VALUE; // composite score of all the scores used in sorting categories

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

    /**
     *
     * If Category Metadata for the given models is not available it compares the pack name of the two categories alphabetically
     *
     * If Category Metadata for the given models is available and the SearchMatchScore is not same then compares the SearchMatchScore of the given categories
     *
     * If Category Metadata for the given models is available and the SearchMatchScore is same then compares the shop rank for the user of the given categories
     *
     * @param another : CategorySearchData model instance with which the current model instance is to be compared
     * @return -1 if this is lower, 0 if equal , 1 if this is higher
     */
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

    /**
     *
     * @return composite score of all the scores used in sorting categories
     *
     *          Methods looks for cached scores in the CategorySearchManager and if not not found computes the scores and stores them in respective cache
     */
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

    /**
     *
     * @return JSON object containing all search info of the given category
     * @throws JSONException
     */

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

    /**
     *
     * @return JSONArray containing all feature vectors info of the given category
     * @throws JSONException
     */
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
