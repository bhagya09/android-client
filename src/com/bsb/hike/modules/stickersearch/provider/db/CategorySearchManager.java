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
public enum CategorySearchManager
{

	INSTANCE;

	public static final String SHOP_SEARCH_WEIGHTS = "s_srcW";

    public static final String SEARCH_RESULTS_LIMIT = "s_s_limit";

    public static final String AUTO_SEARCH_TIME = "a_s_tm";

    public static final long DEFAULT_AUTO_SEARCH_TIME = 500L;

	public static final String DEFAULT_WEIGHTS_INPUT = "0:1:1:2";

    public static final int DEFAULT_SEARCH_RESULTS_LIMIT = 100;

	public static final String TAG = CategorySearchManager.class.getSimpleName();

	private static Map<String, SortedSet<CategorySearchData>> mCacheForShopSearchKeys = new HashMap<String, SortedSet<CategorySearchData>>();

	private static Map<String, Float> mCacheForLocalAnalogousScore = new HashMap<String, Float>();

	private static Map<Integer, StickerCategory> mCacheForSearchedCategories = new HashMap<Integer, StickerCategory>();

	private SearchEngine categorySearchEngine = new SearchEngine();

	private float[] weights;

	/* Get the instance of this class from outside */
	public static CategorySearchManager getInstance()
	{
       if(mCacheForSearchedCategories.size() == 0)
        {
            INSTANCE.loadCategoriesForShopSearch();
        }
		return INSTANCE;
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
	}

	public boolean onQueryTextSubmit(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, false);
		categorySearchEngine.runOnSearchThread(categorySearchTask, 0);
		return true;
	}

	public boolean onQueryTextChange(String query, CategorySearchListener listener)
	{
		CategorySearchTask categorySearchTask = new CategorySearchTask(query, listener, true);
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
                Logger.e(TAG, "getOrderedCategoryList ignoring : "+category.getCategoryName() );
            }

            if(result.size() == HikeSharedPreferenceUtil.getInstance().getData(SHOP_SEARCH_WEIGHTS, DEFAULT_SEARCH_RESULTS_LIMIT))
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
			result = StickerSearchUtility.computeWordMatchScore(searchKey, tag);
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
}
