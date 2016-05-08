package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.SearchEngine;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategorySearchData;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchAnalyticsTask;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchTask;
import com.bsb.hike.modules.stickersearch.tasks.CategoryTagInsertTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 13/04/16.
 */
public class CategorySearchManager
{

	private static volatile CategorySearchManager instance;

	public static final String SHOP_SEARCH_WEIGHTS = "s_srcW";

	public static final String SEARCH_QUERY_LENGTH_THRESHOLD = "s_q_l_t";

	public static final String SEARCH_RESULTS_LIMIT = "s_s_limit";

	public static final String SEARCH_RESULTS_LOG_LIMIT = "s_s_l_limit";

	public static final String AUTO_SEARCH_TIME = "a_s_tm";

	public static final String CATEGORIES_SEARCHED_DAILY_REPORT = "cat_srch_report";

	public static final long DEFAULT_AUTO_SEARCH_TIME = 1250L;

	public static final String DEFAULT_WEIGHTS_INPUT = "0:1:0:2";

	public static final int DEFAULT_SEARCH_RESULTS_LIMIT = -1;

	public static final int DEFAULT_SEARCH_RESULTS_LOG_LIMIT = 5;

	public static final int DEFAULT_SEARCH_QUERY_LENGTH_THRESHOLD = 0;

	public static final String TAG = CategorySearchManager.class.getSimpleName();

	private static Map<String, SortedSet<CategorySearchData>> mCacheForShopSearchKeys;

	private static Map<String, Float> mCacheForLocalAnalogousScore;

	private static Map<Integer, StickerCategory> mCacheForSearchedCategories;

	private static Map<Integer, List<Float>> mCacheForCategoryScore;

	private SearchEngine categorySearchEngine;

	private float[] weights;

	private CategorySearchManager()
	{
		mCacheForShopSearchKeys = new HashMap<String, SortedSet<CategorySearchData>>();

		mCacheForLocalAnalogousScore = new HashMap<String, Float>();

		mCacheForSearchedCategories = new HashMap<Integer, StickerCategory>();

		mCacheForCategoryScore = new HashMap<Integer, List<Float>>();

		categorySearchEngine = new SearchEngine();

		this.loadCategoriesForShopSearch();
	}

	/* Get the instance of this class from outside */
	public static CategorySearchManager getInstance()
	{
		if (instance == null)
		{
			synchronized (CategorySearchManager.class)
			{
				if (instance == null)
				{
					instance = new CategorySearchManager();
				}
			}
		}

		return instance;
	}

	public SearchEngine getSearchEngine()
	{
		return categorySearchEngine;
	}

	public List<StickerCategory> searchForPacks(String query, boolean sendLogs)
	{
		Set<CategorySearchData> resultCategories = getCategorySearchDataForKey(query);

		HikeHandlerUtil.getInstance().postRunnable(new CategorySearchAnalyticsTask(query, resultCategories, sendLogs));

		return getOrderedCategoryList(resultCategories);
	}

	private SortedSet<CategorySearchData> getCategorySearchDataForKey(String key)
	{
		SortedSet<CategorySearchData> result = null;
		if (mCacheForShopSearchKeys.containsKey(key))
		{
			result = mCacheForShopSearchKeys.get(key);
		}
		else
		{
			result = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindCategoryDataList(key);

			if (result == null)
			{
				result = new TreeSet<CategorySearchData>();
			}

			mCacheForShopSearchKeys.put(key, result);
		}
		return result;
	}

	public void clearTransientResources()
	{
		mCacheForLocalAnalogousScore.clear();
		mCacheForShopSearchKeys.clear();
		mCacheForSearchedCategories.clear();
		mCacheForCategoryScore.clear();
		categorySearchEngine.shutDown();

		mCacheForLocalAnalogousScore = null;
		mCacheForShopSearchKeys = null;
		mCacheForSearchedCategories = null;
		mCacheForCategoryScore = null;
		categorySearchEngine = null;

		instance = null;
	}

	public boolean onQueryTextSubmit(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, true);
		categorySearchEngine.runOnSearchThread(categorySearchTask, 0);
		return true;
	}

	public boolean onQueryTextChange(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, false);
		categorySearchTask.run();
		return true;
	}

	private List<StickerCategory> getOrderedCategoryList(Set<CategorySearchData> querySet)
	{
		if (Utils.isEmpty(querySet))
		{
			return null;
		}

		List<StickerCategory> result = new ArrayList<>(querySet.size());

		for (CategorySearchData categorySearchData : querySet)
		{
			StickerCategory category = categorySearchData.getCategory();
			if (category != null)
			{
				result.add(category);
			}
			else
			{
				Logger.e(TAG, "getOrderedCategoryList ignoring : " + categorySearchData.getName());
			}

			int searchResultsLimit = HikeSharedPreferenceUtil.getInstance().getData(SEARCH_RESULTS_LIMIT, DEFAULT_SEARCH_RESULTS_LIMIT);

			if ((searchResultsLimit > 0) && (result.size() == searchResultsLimit))
			{
				Logger.e(TAG, "getOrderedCategoryList limit reached");
				break;
			}
		}

		return result;
	}

	public void insertCategoryTags(JSONArray categoriesData, Map<Integer, CategoryTagData> sourceData)
	{
		CategoryTagInsertTask categoryTagInsertTask = new CategoryTagInsertTask(categoriesData, sourceData);
		categorySearchEngine.runOnQueryThread(categoryTagInsertTask);
	}

	public float computeStringMatchScore(String searchKey, String tag)
	{
		String cacheKey = searchKey + StickerSearchConstants.STRING_PREDICATE + tag;
		Float result = mCacheForLocalAnalogousScore.get(cacheKey);

		if (result == null)
		{
			result = StickerSearchUtility.computeWordMatchScore(
					searchKey.replaceAll(StickerSearchConstants.REGEX_SHOP_SEARCH_SEPARATORS_LATIN, StickerSearchConstants.STRING_EMPTY), tag);
			mCacheForLocalAnalogousScore.put(cacheKey, result);
		}

		return result;
	}

	public void loadCategoriesForShopSearch()
	{
		Map<Integer, StickerCategory> loadMap = HikeConversationsDatabase.getInstance().getCategoriesForShopSearch();
		if (loadMap != null)
		{
			mCacheForSearchedCategories.putAll(loadMap);
		}
	}

	public StickerCategory getSearchedCategoriesFromCache(int ucid)
	{
		return mCacheForSearchedCategories.get(ucid);
	}

	public float[] getFeatureWeights()
	{
		if (weights == null)
		{
			String[] inputs = HikeSharedPreferenceUtil.getInstance().getData(SHOP_SEARCH_WEIGHTS, DEFAULT_WEIGHTS_INPUT).split(HikeConstants.DELIMETER);
			weights = new float[inputs.length];
			for (int i = 0; i < inputs.length; i++)
			{
				weights[i] = Float.parseFloat(inputs[i]);
			}
		}

		return weights;
	}

	public List<Float> getCategoryScores(int categoryUcid)
	{
		return mCacheForCategoryScore.get(categoryUcid);
	}

	public void saveCategoryScores(int categoryUcid, List<Float> categoryScores)
	{
		mCacheForCategoryScore.put(categoryUcid, categoryScores);
	}

	public static void removeShopSearchTagsForCategory(final Set<Integer> ucids)
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				HikeStickerSearchDatabase.getInstance().deleteCategoryTagFromCategorySearchTable(ucids);
			}
		});
	}

	public static void logSearchedCategoryToDailyReport(CategorySearchData categorySearchData, int index, int totalResults) throws JSONException
	{
		String searchReport = HikeSharedPreferenceUtil.getInstance().getData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		JSONObject searchReportMetadata = TextUtils.isEmpty(searchReport) ? new JSONObject() : new JSONObject(searchReport);

		String categoryKey = Integer.toString(categorySearchData.getUcid());

		JSONObject categoryReport = searchReportMetadata.has(categoryKey) ? searchReportMetadata.getJSONObject(categoryKey) : new JSONObject();

		categoryReport.put(CategorySearchAnalyticsTask.RESULTS_COUNT, categoryReport.optInt(CategorySearchAnalyticsTask.RESULTS_COUNT) + 1);
		categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT, categoryReport.optInt(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT) + index);
		categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT, categoryReport.optDouble(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT)
				+ (index * 1.0) / totalResults);

		int topBucketIdx = index / 5;
		if (topBucketIdx >= 0 && topBucketIdx < CategorySearchAnalyticsTask.TOP_BUCKETS.length)
		{
			categoryReport.put(CategorySearchAnalyticsTask.TOP_BUCKETS[topBucketIdx], categoryReport.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[topBucketIdx]) + 1);
		}

		searchReportMetadata.put(categoryKey, categoryReport);

		HikeSharedPreferenceUtil.getInstance().saveData(CATEGORIES_SEARCHED_DAILY_REPORT, searchReportMetadata.toString());
	}

	public static void sendSearchedCategoryDailyReport()
	{
		String searchReport = HikeSharedPreferenceUtil.getInstance().getData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		if (TextUtils.isEmpty(searchReport))
		{
			return;
		}

		try
		{
			JSONObject searchReportJSON = new JSONObject(searchReport);
			Iterator<String> iterator = searchReportJSON.keys();

			JSONObject metadata = new JSONObject();
			JSONArray categoriesReport = new JSONArray();

			while (iterator.hasNext())
			{
				String catUcid = iterator.next();
				JSONObject categoryReportJson = searchReportJSON.getJSONObject(catUcid);

				int categorySearchedCount = categoryReportJson.optInt(CategorySearchAnalyticsTask.RESULTS_COUNT);

				if (categorySearchedCount <= 0)
				{
					continue;
				}

				JSONObject categoryReport = new JSONObject();
				categoryReport.put(HikeConstants.UCID, catUcid);
				categoryReport.put(CategorySearchAnalyticsTask.RESULTS_COUNT, categorySearchedCount);
				categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT, categoryReportJson.optInt(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT) * 1.0
						/ categorySearchedCount);
				categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT,
						categoryReportJson.optDouble(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT) / categorySearchedCount);
				for (int i = 0; i < CategorySearchAnalyticsTask.TOP_BUCKETS.length; i++)
				{
					categoryReport.put(CategorySearchAnalyticsTask.TOP_BUCKETS[i], categoryReportJson.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[i]));
				}

				categoriesReport.put(categoryReport);
			}

			if (categoriesReport.length() > 0)
			{
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CATEGORY_SEARCHED_REPORT);
				metadata.put(HikeConstants.LogEvent.CATEGORY_SEARCHED_REPORT_DATA, categoriesReport);

				HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, HAManager.EventPriority.HIGH, metadata);
			}

			HikeSharedPreferenceUtil.getInstance().saveData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		}
		catch (JSONException e)
		{
			Logger.e(TAG, "sendSearchedCategoryDailyReport() : Exception While send report analytics JSON : " + e.getMessage());
		}
	}

	public static void sendCategorySearchResultResponseAnalytics(final String source)
	{
		final String recordedReportString = HikeSharedPreferenceUtil.getInstance().getData(CategorySearchAnalyticsTask.SHOP_SEARCH_RESULTS_ANALYTICS_LOG, "");

		if (TextUtils.isEmpty(recordedReportString))
		{
			return;
		}

		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					JSONObject metadata = new JSONObject();

					JSONObject recordedReport = new JSONObject(recordedReportString);

					recordedReport.put(HikeConstants.SOURCE, source);

					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE);
					metadata.put(HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE_DATA, recordedReport);

					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadata);

				}
				catch (JSONException e)
				{
					Logger.e(TAG, "sendCategorySearchResultResponseAnalytics() : Exception While send report analytics JSON : " + e.getMessage());
				}
			}
		});

	}
}
