package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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

/**
 * Created by akhiltripathi on 13/04/16.
 */
public enum CategorySearchManager
{

    INSTANCE;

    public final String SEARCH_WEIGHTS = "srcW";

    public final String DEFAULT_WEIGHTS_INPUT = "0:1:1:2";

    public static final String TAG = CategorySearchManager.class.getSimpleName();

    private static HashMap<String, TreeSet<CategorySearchData>> mCacheForShopSearchKeys = new HashMap<String, TreeSet<CategorySearchData>>();

    private static HashMap<String, Float> mCacheForLocalAnalogousScore = new HashMap<String, Float>();

    private static HashMap<String, StickerCategory> mCacheForSearchedCategories = new HashMap<String, StickerCategory>();

    private SearchEngine categorySearchEngine = new SearchEngine();

    private float[] weights;

    /* Get the instance of this class from outside */
    public static CategorySearchManager getInstance()
    {
        return INSTANCE;
    }

	public SearchEngine getSearchEngine()
	{
		return categorySearchEngine;
	}

    public List<StickerCategory> searchForPacks(String query)
    {
        TreeSet<CategorySearchData> resultCategories = getCategoriesForKey(query.toLowerCase());

        return getOrderedCategories(resultCategories);
    }

    private TreeSet<CategorySearchData> getCategoriesForKey(String key)
    {
        TreeSet<CategorySearchData> result = null;
        if(mCacheForShopSearchKeys.containsKey(key))
        {
            result = mCacheForShopSearchKeys.get(key);
        }
        else
        {
            result = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindCategoryList(key);

            if(result == null)
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
    }

    public boolean onQueryTextSubmit(String query,CategorySearchListener listener)
    {
        CategorySearchTask categorySearchTask = new CategorySearchTask(query,listener);
        categorySearchEngine.runOnSearchThread(categorySearchTask, 0);
        return true;
    }

    public boolean onQueryTextChange(String s,CategorySearchListener listener)
    {
        return false;
    }

    private List<StickerCategory> getOrderedCategories(Set<CategorySearchData> querySet)
    {
        if(Utils.isEmpty(querySet))
        {
            return null;
        }

        List<StickerCategory> result = new ArrayList<>(querySet.size());

        for(CategorySearchData categorySearchData : querySet)
        {
            StickerCategory category = categorySearchData.getCategory();
            if(category != null)
            {
                result.add(category);
                Logger.e("aktt","name "+category.getCategoryId());
            }
        }

        return result;
    }

    public void insertCategoryTags(List<CategoryTagData> categoryTagData)
    {
        CategoryTagInsertTask categoryTagInsertTask = new CategoryTagInsertTask(categoryTagData);
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

    public void loadSearchedCategories(String[] categories)
    {
        mCacheForSearchedCategories.putAll(HikeConversationsDatabase.getInstance().getCategoriesForShopSearch(categories));
    }

    public StickerCategory getSearchedCategoriesFromCache(String catId)
    {
        return mCacheForSearchedCategories.get(catId);
    }

	public float[] getFeatureWeights()
	{
		if(weights == null)
        {
            String[] inputs = HikeSharedPreferenceUtil.getInstance().getData(SEARCH_WEIGHTS, DEFAULT_WEIGHTS_INPUT).split(HikeConstants.DELIMETER);
            weights = new float[inputs.length];
            for(int i =0; i<inputs.length; i++)
            {
                weights[i] = Float.parseFloat(inputs[i]);
            }
        }

        return weights;
	}
}
