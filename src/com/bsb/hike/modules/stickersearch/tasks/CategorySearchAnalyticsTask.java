package com.bsb.hike.modules.stickersearch.tasks;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategorySearchData;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class CategorySearchAnalyticsTask implements Runnable
{
	private final String TAG = CategorySearchAnalyticsTask.class.getSimpleName();

	public static final String SHOP_SEARCH_RESULTS_ANALYTICS_LOG = "ssr_log";

	public static final String QUERY_KEY = "query";

	public static final String RESULTS_COUNT = "rCnt";

	public static final String RESULT_SET = "results";

	public static final String CATEGORY_FEATURE_VECTOR = "c_f_vector";

	public static final String CATEGORY_SCORE = "c_score";

	public static final String CATEGORY_GENDER_MATCH_SCORE = "c_gm_s";

	public static final String CATEGORY_STATE_SCORE = "c_st_s";

	public static final String CATEGORY_STICKER_COUNT_SCORE = "c_sc_s";

	public static final String CATEGORY_NAME_MATCH_SCORE = "c_nm_s";

	public static final String CATEGORY_RANK_REPORT = "rnkAvg";

	public static final String CATEGORY_NORMALIZED_RANK_REPORT = "n_rnkAvg";

	public static final String[] TOP_BUCKETS = { "top5", "top10", "top20" };

	public static final String SHOP_SEARCH_SEARCH_BUTTON_TRIGGER = "sBtn";

	public static final String SHOP_SEARCH_BACK_BUTTON_TRIGGER = "bckBtn";

	public static final String SHOP_SEARCH_CROSS_BUTTON_TRIGGER = "crsBtn";

	public static final String SHOP_SEARCH_PACK_PREVIEWED_BUTTON_TRIGGER = "pPrev";

    private String searchedQuery;

    private Set<CategorySearchData> searchedCategories;

    private boolean sendLogsImmediately;

	public CategorySearchAnalyticsTask(String query, Set<CategorySearchData> categoriesSearchData, boolean sendLogsImmediately)
	{
		this.searchedQuery = preProcessQuery(query);
		this.searchedCategories = categoriesSearchData;
        this.sendLogsImmediately = sendLogsImmediately;
	}

	@Override
	public void run()
	{
		if (!StickerManager.getInstance().isShopSearchEnabled())
		{
			return;
		}

		if (TextUtils.isEmpty(searchedQuery))
		{
			Logger.i(TAG, "Invalid searched analytics data. Skipping analytics recording");
			return;
		}

		try
		{

			int categoryToLogVectorsLimit = HikeSharedPreferenceUtil.getInstance().getData(CategorySearchManager.SEARCH_RESULTS_LOG_LIMIT,
					CategorySearchManager.DEFAULT_SEARCH_RESULTS_LOG_LIMIT);

			int categoryLoggedCount = 0;

			JSONObject categorySearchMetadata = new JSONObject();

			categorySearchMetadata.put(QUERY_KEY, searchedQuery);

			categorySearchMetadata.put(RESULTS_COUNT, Utils.isEmpty(searchedCategories) ? 0 : searchedCategories.size());

			if (!Utils.isEmpty(searchedCategories))
			{
				JSONArray resultsMetadata = new JSONArray();

				for (CategorySearchData searchedCategory : searchedCategories)
				{
					try
					{
						if (categoryLoggedCount < categoryToLogVectorsLimit)
						{
							JSONObject searchedCategoryJSON = searchedCategory.toJSON();

							searchedCategoryJSON.put(HikeConstants.INDEX, ++categoryLoggedCount);

							resultsMetadata.put(searchedCategoryJSON);
						}

						CategorySearchManager.logSearchedCategoryToDailyReport(searchedCategory, categoryLoggedCount, searchedCategories.size());
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "Exception While logging analytics JSON : " + e.getMessage());
					}

				}

				categorySearchMetadata.put(RESULT_SET, resultsMetadata);
			}

			HikeSharedPreferenceUtil.getInstance().saveData(SHOP_SEARCH_RESULTS_ANALYTICS_LOG, categorySearchMetadata.toString());

			if (sendLogsImmediately)
			{
				CategorySearchManager.sendCategorySearchResultResponseAnalytics(CategorySearchAnalyticsTask.SHOP_SEARCH_SEARCH_BUTTON_TRIGGER);
			}

		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception While logging analytics JSON : " + e.getMessage());
		}

	}

	private String preProcessQuery(String query)
	{
		if (TextUtils.isEmpty(query))
		{
			return null;
		}

		return query.trim();
	}

}