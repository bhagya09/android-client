package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.SearchEngine;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategorySearchData;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchTask;
import com.bsb.hike.modules.stickersearch.tasks.CategoryTagInsertTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;

/**
 * Created by akhiltripathi on 13/04/16.
 */
public class CategorySearchManager
{

	private static volatile CategorySearchManager instance;

	public static final String SHOP_SEARCH_WEIGHTS = "s_srcW";

    public static final String SEARCH_QUERY_LENGTH_THRESHOLD = "s_q_l_t";

    public static final String SEARCH_RESULTS_LIMIT = "s_s_limit";

    public static final String AUTO_SEARCH_TIME = "a_s_tm";

    public static final long DEFAULT_AUTO_SEARCH_TIME = 1250L;

	public static final String DEFAULT_WEIGHTS_INPUT = "0:1:0:2";

    public static final int DEFAULT_SEARCH_RESULTS_LIMIT = -1;

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

        mCacheForCategoryScore = new HashMap<Integer,List<Float>>();

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

	public List<StickerCategory> searchForPacks(String query)
	{
		Set<CategorySearchData> resultCategories = getCategorySearchDataForKey(query);

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
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener);
		categorySearchEngine.runOnSearchThread(categorySearchTask, 0);
		return true;
	}

	public boolean onQueryTextChange(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener);
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
			if (category != null )
			{
				result.add(category);
			}
            else
            {
                Logger.e(TAG, "getOrderedCategoryList ignoring : "+categorySearchData.getName() );
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
			result = StickerSearchUtility.computeWordMatchScore(searchKey.replaceAll(StickerSearchConstants.REGEX_SHOP_SEARCH_SEPARATORS_LATIN, StickerSearchConstants.STRING_EMPTY), tag);
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
}
