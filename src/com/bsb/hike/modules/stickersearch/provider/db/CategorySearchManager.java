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
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.SearchEngine;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.datamodel.CategorySearchData;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchAnalyticsTask;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchTask;
import com.bsb.hike.modules.stickersearch.tasks.CategoryTagInsertTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 *
 * This class mainly handles interaction of the CategorySearchwatcher with the HikeStickerSearchDatabase
 *
 * Created by akhiltripathi on 13/04/16.
 */
public class CategorySearchManager
{

	private static volatile CategorySearchManager instance;

	public static final String SHOP_SEARCH_WEIGHTS = "s_srcW"; // Server Controlled Feature weights String {Syntax : fw1:fw2:fw3:...fwN} where fwN is the weight for the Nth feature

	public static final String SEARCH_QUERY_LENGTH_THRESHOLD = "s_q_l_t"; // Server Controlled Query Min Length threshold. The CategorySearchTask gives no results if query length is <= this threshold

	public static final String SEARCH_RESULTS_LIMIT = "s_s_limit"; // Server Controlled Limit on the number of results shown in Shop Search

	public static final String SEARCH_RESULTS_LOG_LIMIT = "s_s_l_limit"; //  Server Controlled Limit on the number of top results full search data to send to analytics

	public static final String CATEGORIES_SEARCHED_DAILY_REPORT = "cat_srch_report"; // Pref key that stores categories wise search report in a JSON string

	public static final String DEFAULT_WEIGHTS_INPUT = "0:1:0:2"; //Default Weight Strings [ genderMatchScoreWeight : packStateScoreWeight : stickerCountScoreWeight : nameMatchScoreWeight ]

	public static final int DEFAULT_SEARCH_RESULTS_LIMIT = -1;

	public static final int DEFAULT_SEARCH_RESULTS_LOG_LIMIT = 5;

	public static final int DEFAULT_SEARCH_QUERY_LENGTH_THRESHOLD = 0;

	public static final String TAG = CategorySearchManager.class.getSimpleName();

	private static Map<String, SortedSet<CategorySearchData>> mCacheForShopSearchKeys; // cache to store shop search results per query

	private static Map<String, Float> mCacheForLocalAnalogousScore; // cache to score TextDifference score for two strings. Key Syntax : String1*String2

	private static Map<Integer, StickerCategory> mCacheForSearchedCategories; // cache to store all pack metadata from stickerCategoryTable in HikeConverstaionDatabase

	private static Map<Integer, List<Float>> mCacheForCategoryScore; // cache to store all category scores that are independent of the search query [ genderMatchScore : packStateScore : stickerCountScore ]

	private SearchEngine categorySearchEngine; // SerachEngine Model instance to manage various search threads executors

	private float[] weights;

    //initialises caches with defaults
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

    /**
     *
     * @param query : Pre-Processed query string to search packs
     * @param onTextSubmit : boolean to decide if the search was automatically initiated or user initiated, send result analytics as soon as searched in case of user initiated search and look for an exact match
     *
     * @return StickerCategory model List of the categories searched on the basis of given user query
     */
	public List<StickerCategory> searchForPacks(String query, boolean onTextSubmit)
	{
		Set<CategorySearchData> resultCategories = getCategorySearchDataForKey(query, onTextSubmit);

		HikeHandlerUtil.getInstance().postRunnable(new CategorySearchAnalyticsTask(query, resultCategories, onTextSubmit));

		return getOrderedCategoryList(resultCategories);
	}

    /**
     *
     * This method looks for search results in the mCacheForShopSearchKeys Cache first
     * If results not found in cache then searches the ShopSearchVirtualTable in HikeStickerSearchDatabase and stores them in the cache
     *
     * @param key : Pre-Processed query string to search packs
     * @param exactMatch : boolean to decide if to do a prefix search or exact match
     *
     * @return   SortedSet of categories Searched for the given match key
     *           The searched categories are sorted based on comparator of CategorySearchedData model
     *           Considering pack attributes [Targeted_Gender ; Pack_Downloaded_State ; Pack_stickersAvailable_Count] including the text match score of the match key with the pack name
     *
     *           The pack data are sorted using a TreeSet implementation which ensure uniqueness along with order
     */

	private SortedSet<CategorySearchData> getCategorySearchDataForKey(String key, boolean exactMatch)
	{
		SortedSet<CategorySearchData> result = null;
		if (mCacheForShopSearchKeys.containsKey(key))
		{
			result = mCacheForShopSearchKeys.get(key);
		}
		else
		{
			result = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindCategoryDataList(key, exactMatch);

			if (result == null)
			{
				result = new TreeSet<CategorySearchData>();
			}

			mCacheForShopSearchKeys.put(StickerSearchUtils.generateCacheKey(key, exactMatch), result);
		}
		return result;
	}

    /**
     *
     * Clears all Caches
     * Shuts down the Search Engine [Closes the executors and releases them]
     *
     * Deletes the CastegorySearchManager instance as well
     */
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

    /**
     * This is called in case of user triggered search {keyboard search button press}
     * Analytics are send as soon as search is completed in a different thread
     *
     * CategorySearchTask carries out the search in the search thread of the Category SearchEngine
     *
     * @param query : Un-Processed query string to search packs
     * @param listener : CategorySearchListener to handle the search results
     */
	public boolean onQueryTextSubmit(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, true);
		categorySearchEngine.runOnSearchThread(categorySearchTask, 0);
		return true;
	}

    /**
     * This is called in case of auto triggered search
     *
     * CategorySearchTask carries out the search in the search thread of the Category SearchEngine
     *
     * @param query : Un-Processed query string to search packs
     * @param listener : CategorySearchListener to handle the search results
     */
	public boolean onQueryTextChange(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, false);
		categorySearchTask.run();
		return true;
	}

    /**
     *
     * @param querySet :  Sorted Set of categories represented in CategorySearchData model
     * @return : List of valid categories represented in StickerCategory model
     *           If the Search Results count limit is set from the server to N [>0] then this adds only top N valid categories to return List else tall the valid categories
     *
     */
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

    /**
     *
     * @param searchKey
     * @param tag
     * @return float score of the text distance difference btw searchKey and tag
     */
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

    /**
     * Loads all pack metadata from stickerCategoryTable in HikeConverstaionDatabase into mCacheForSearchedCategories
     */
	public void loadCategoriesForShopSearch()
	{
		Map<Integer, StickerCategory> loadMap = HikeConversationsDatabase.getInstance().getCategoriesForShopSearch();
		if (loadMap != null)
		{
			mCacheForSearchedCategories.putAll(loadMap);
		}
	}

    /**
     *
     * @param ucid
     * @return StickerCategory model of the given category ucid from mCacheForSearchedCategories
     */
	public StickerCategory getSearchedCategoriesFromCache(int ucid)
	{
		return mCacheForSearchedCategories.get(ucid);
	}

	public float[] getFeatureWeights()
	{
		if (weights == null)
		{
			String weightString = HikeSharedPreferenceUtil.getInstance().getData(SHOP_SEARCH_WEIGHTS, DEFAULT_WEIGHTS_INPUT);

			if (TextUtils.isEmpty(weightString) || !weightString.contains(HikeConstants.DELIMETER))
			{
				weightString = DEFAULT_WEIGHTS_INPUT;
			}

			String[] inputs = weightString.split(HikeConstants.DELIMETER);
			weights = new float[inputs.length];
			for (int i = 0; i < inputs.length; i++)
			{
				weights[i] = Float.parseFloat(inputs[i]);
			}
		}

		return weights;
	}

    /**
     *
     * @param categoryUcid
     * @return List of all category scores that are independent of the search query [ genderMatchScore : packStateScore : stickerCountScore ] of the given category ucid from mCacheForCategoryScore cache
     */
	public List<Float> getCategoryScores(int categoryUcid)
	{
		return mCacheForCategoryScore.get(categoryUcid);
	}

    /**
     *
     * @param categoryUcid
     * @param categoryScores
     *
     * @saves List of all category scores that are independent of the search query [ genderMatchScore : packStateScore : stickerCountScore ] of the given category ucid to mCacheForCategoryScore cache
     */
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

    /**
     *
     * This method saves the searched info of all categories searched within a time period to shared preference in a Json String
     *
     * @param categorySearchData  : Search Data of the category represented in the CategorySearchData model
     * @param index : the position where the category was shown in search result in an instance
     * @param totalResults : the total number of search result in an search instance
     * @throws JSONException
     *
     * JSON Example :
     *
        {
            "ek":"c_s_rep",
            "csrd":[
                {
                    "ucid":"142",
                    "rCnt":3,
                    "rnkAvg":3,
                    "n_rnkAvg":0.25338164251207734,
                    "top5":2,
                    "top10":1,
                    "top20":0
                },
                {
                    "ucid":"117",
                    "rCnt":1,
                    "rnkAvg":2,
                    "n_rnkAvg":0.08695652173913043,
                    "top5":1,
                    "top10":0,
                    "top20":0
                },
                ...
            ]
        }
     *
     */
	public static void logSearchedCategoryToDailyReport(CategorySearchData categorySearchData, int index, int totalResults) throws JSONException
	{
		if (categorySearchData == null || totalResults == 0)
		{
			return;
		}
        
		String searchReport = HikeSharedPreferenceUtil.getInstance().getData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		JSONObject searchReportMetadata = TextUtils.isEmpty(searchReport) ? new JSONObject() : new JSONObject(searchReport);

		String categoryKey = Integer.toString(categorySearchData.getUcid());

		JSONObject categoryReport = searchReportMetadata.has(categoryKey) ? searchReportMetadata.getJSONObject(categoryKey) : new JSONObject();

		categoryReport.put(CategorySearchAnalyticsTask.RESULTS_COUNT, categoryReport.optInt(CategorySearchAnalyticsTask.RESULTS_COUNT) + 1);
		categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT, categoryReport.optInt(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT) + index);
		categoryReport.put(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT, categoryReport.optDouble(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT, 0.0)
				+ (index * 1.0) / totalResults);

		int topBucketIdx = index / 5;
		if (topBucketIdx >= 0 && topBucketIdx < CategorySearchAnalyticsTask.TOP_BUCKETS.length)
		{
			categoryReport.put(CategorySearchAnalyticsTask.TOP_BUCKETS[topBucketIdx], categoryReport.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[topBucketIdx]) + 1);
		}

		searchReportMetadata.put(categoryKey, categoryReport);

		HikeSharedPreferenceUtil.getInstance().saveData(CATEGORIES_SEARCHED_DAILY_REPORT, searchReportMetadata.toString());
	}

    /**
     * This method sends the searched info of all categories searched within a time period stored in a shared preference as a Json String
     *
     * This method sends one analytic packet per category
     *
     * JSON example (For a single category):
     *
         {
             "b":0,
             "ver":"v2",
             "fu":"VIhUB-V-iw4mSom0",
             "g":"264",
             "cts":1463041332530,
             "msisdn":"+912233223322",
             "vi":10,
             "k":"act_stck",
             "di":"and:6c2b5d9cb39413790e1c6edb700d797288c33aba",
             "ts":1463041347,
             "o":"shpSrchAggRept",
             "p":"shpSrch",
             "s":0.07734500805152977,
             "r":1464719341936,
             "av":"android-4.2.6.81.25.Custom_Dev_Flavor",
             "us":2.6,
             "uk":"shpSrchAggRept",
             "v":6,
             "ov":"5.1",
             "ra":4
         }
     */
	public static void sendSearchedCategoryDailyReport()
	{
		String searchReport = HikeSharedPreferenceUtil.getInstance().getData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		if (!StickerManager.getInstance().isShopSearchEnabled() || TextUtils.isEmpty(searchReport))
		{
			return;
		}

		try
		{
			JSONObject searchReportJSON = new JSONObject(searchReport);
			Iterator<String> iterator = searchReportJSON.keys();

			while (iterator.hasNext())
			{
				JSONObject metadata = new JSONObject();
				metadata.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
				metadata.put(AnalyticsConstants.V2.PHYLUM, HikeConstants.LogEvent.SHOP_SEARCH);
				metadata.put(AnalyticsConstants.V2.ORDER, HikeConstants.LogEvent.CATEGORY_SEARCHED_REPORT);
				metadata.put(AnalyticsConstants.V2.UNIQUE_KEY, HikeConstants.LogEvent.CATEGORY_SEARCHED_REPORT);
				metadata.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());

				String catUcid = iterator.next();
				JSONObject categoryReportJson = searchReportJSON.getJSONObject(catUcid);

				int categorySearchedCount = categoryReportJson.optInt(CategorySearchAnalyticsTask.RESULTS_COUNT);

				if (categorySearchedCount <= 0)
				{
					continue;
				}

				metadata.put(AnalyticsConstants.V2.GENUS, catUcid);
				metadata.put(AnalyticsConstants.V2.VAL_INT, categorySearchedCount);
				metadata.put(AnalyticsConstants.V2.USER_STATE, categoryReportJson.optInt(CategorySearchAnalyticsTask.CATEGORY_RANK_REPORT) * 1.0 / categorySearchedCount);
				metadata.put(AnalyticsConstants.V2.SPECIES, categoryReportJson.optDouble(CategorySearchAnalyticsTask.CATEGORY_NORMALIZED_RANK_REPORT, 0.0) / categorySearchedCount);
				metadata.put(AnalyticsConstants.V2.VARIETY, categoryReportJson.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[0]));
				metadata.put(AnalyticsConstants.V2.RACE, categoryReportJson.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[1]));
				metadata.put(AnalyticsConstants.V2.BREED, categoryReportJson.optInt(CategorySearchAnalyticsTask.TOP_BUCKETS[2]));

				HAManager.getInstance().recordV2(metadata);
			}

			HikeSharedPreferenceUtil.getInstance().saveData(CATEGORIES_SEARCHED_DAILY_REPORT, "");

		}
		catch (JSONException e)
		{
			Logger.e(TAG, "sendSearchedCategoryDailyReport() : Exception While send report analytics JSON : " + e.getMessage());
		}
	}

    /**
     *
     * Method sends the complete feature JSON for the Top N searched category.
     * Where N is decided from the Server Controlled value stored in SEARCH_RESULTS_LOG_LIMIT pref
     *
     * This method sends one analytic packet per category
     *
     * @param source : action source of the search result {Search button press; Cross Button Press; Back Button press; Searched Category Clicked}
     *
     * JSON example :

            {
                "src":"crsBtn",
                "ver":"v2",
                "fu":"VIhUB-V-iw4mSom0",
                "cts":1463041055297,
                "msisdn":"+912233223322",
                "vi":"0",
                "k":"act_stck",
                "di":"and:6c2b5d9cb39413790e1c6edb700d797288c33aba",
                "ts":1463041073,
                "o":"shpSrchRept",
                "p":"shpSrch",
                "vs":"byw",
                "uk":"shpSrchRept",
                "av":"android-4.2.6.81.25.Custom_Dev_Flavor",
                "ov":"5.1",
                "r":1464711731733
            }

     */
	public static void sendCategorySearchResultResponseAnalytics(final String source)
	{
		final String recordedReportString = HikeSharedPreferenceUtil.getInstance().getData(CategorySearchAnalyticsTask.SHOP_SEARCH_RESULTS_ANALYTICS_LOG, "");

		if (!StickerManager.getInstance().isShopSearchEnabled() || TextUtils.isEmpty(recordedReportString) || TextUtils.isEmpty(source))
		{
			return;
		}

		HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchAnalyticsTask.SHOP_SEARCH_RESULTS_ANALYTICS_LOG, "");

		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					JSONObject recordedReport = new JSONObject(recordedReportString);

					JSONArray categoriesReport = recordedReport.optJSONArray(CategorySearchAnalyticsTask.RESULT_SET);

					if (Utils.isEmpty(categoriesReport))
					{

						JSONObject metadata = new JSONObject();
						metadata.put(AnalyticsConstants.V2.SOURCE, source);
						metadata.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
						metadata.put(AnalyticsConstants.V2.PHYLUM, HikeConstants.LogEvent.SHOP_SEARCH);
						metadata.put(AnalyticsConstants.V2.ORDER, HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE);
						metadata.put(AnalyticsConstants.V2.UNIQUE_KEY, HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE);
						metadata.put(AnalyticsConstants.V2.VAL_STR, recordedReport.optString(CategorySearchAnalyticsTask.QUERY_KEY));
						metadata.put(AnalyticsConstants.V2.VAL_INT, recordedReport.optString(CategorySearchAnalyticsTask.RESULTS_COUNT));
						metadata.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());

						HAManager.getInstance().recordV2(metadata);
						return;
					}

					for (int i = 0; i < categoriesReport.length(); i++)
					{

						JSONObject metadata = new JSONObject();
						metadata.put(AnalyticsConstants.V2.SOURCE, source);
						metadata.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
						metadata.put(AnalyticsConstants.V2.PHYLUM, HikeConstants.LogEvent.SHOP_SEARCH);
						metadata.put(AnalyticsConstants.V2.ORDER, HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE);
						metadata.put(AnalyticsConstants.V2.UNIQUE_KEY, HikeConstants.LogEvent.SEARCHED_CATEGORY_RESPONSE);
						metadata.put(AnalyticsConstants.V2.VAL_STR, recordedReport.optString(CategorySearchAnalyticsTask.QUERY_KEY));
						metadata.put(AnalyticsConstants.V2.VAL_INT, recordedReport.optString(CategorySearchAnalyticsTask.RESULTS_COUNT));
						metadata.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());

						JSONObject categoryReport = categoriesReport.getJSONObject(i);
						metadata.put(AnalyticsConstants.V2.GENUS, categoryReport.optString(HikeConstants.UCID));
						metadata.put(AnalyticsConstants.V2.USER_STATE, categoryReport.optInt(HikeConstants.RANK));
						metadata.put(AnalyticsConstants.V2.FORM, categoryReport.optDouble(CategorySearchAnalyticsTask.CATEGORY_SCORE, 0));
						metadata.put(AnalyticsConstants.V2.CENSUS, categoryReport.optInt(HikeConstants.INDEX, 0));

						JSONArray featureVector = categoryReport.optJSONArray(CategorySearchAnalyticsTask.CATEGORY_FEATURE_VECTOR);

						if (!Utils.isEmpty(featureVector))
						{
							metadata.put(AnalyticsConstants.V2.SPECIES, featureVector.getJSONObject(0).optDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE, 0));
							metadata.put(AnalyticsConstants.V2.VARIETY, featureVector.getJSONObject(0).optDouble(CategorySearchAnalyticsTask.CATEGORY_GENDER_MATCH_SCORE, 0));
							metadata.put(AnalyticsConstants.V2.RACE, featureVector.getJSONObject(1).optDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE, 0));
							metadata.put(AnalyticsConstants.V2.BREED, featureVector.getJSONObject(1).optDouble(CategorySearchAnalyticsTask.CATEGORY_STATE_SCORE, 0));
							metadata.put(AnalyticsConstants.DATA, featureVector.getJSONObject(2).optDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE, 0));
							metadata.put(AnalyticsConstants.V2.SECTION, featureVector.getJSONObject(2).optDouble(CategorySearchAnalyticsTask.CATEGORY_STICKER_COUNT_SCORE, 0));
							metadata.put(AnalyticsConstants.TYPE, featureVector.getJSONObject(3).optDouble(HikeConstants.STICKER_SCORE_WEIGHTAGE, 0));
							metadata.put(AnalyticsConstants.V2.SERIES, featureVector.getJSONObject(3).optDouble(CategorySearchAnalyticsTask.CATEGORY_NAME_MATCH_SCORE, 0));
						}

						HAManager.getInstance().recordV2(metadata);
					}

				}
				catch (JSONException e)
				{
					Logger.e(TAG, "sendCategorySearchResultResponseAnalytics() : Exception While send report analytics JSON : " + e.getMessage());
				}
			}
		});

	}
}
