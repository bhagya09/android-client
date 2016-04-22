package com.bsb.hike.modules.stickersearch.provider.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.SearchEngine;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchTask;
import com.bsb.hike.modules.stickersearch.tasks.CategoryTagInsertTask;

/**
 * Created by akhiltripathi on 13/04/16.
 */
public enum CategorySearchManager
{

    INSTANCE;

    public static final String TAG = CategorySearchManager.class.getSimpleName();

    private static HashMap<String, TreeSet<CategoryTagData>> mCacheForShopSearchKeys = new HashMap<String, TreeSet<CategoryTagData>>();

    private SearchEngine categorySearchEngine = new SearchEngine();

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

        Set<CategoryTagData> categoriesFromFullText = getCategoriesForKey(query);

        String keys[] = query.split(StickerSearchConstants.DEFAULT_REGEX_SEPARATORS_LATIN, StickerSearchConstants.DEFAULT_SHOP_SEARCH_KEY_LIMIT);

        Set<CategoryTagData> categoriesFromKeys = null;

        if(keys.length > 1)
        {
            categoriesFromKeys = getCategoriesForKey(keys);
        }

        return getOrderedCategories(categoriesFromFullText,categoriesFromKeys);
    }

    public Set<CategoryTagData> getCategoriesForKey(String keys[])
    {
        List<Set<CategoryTagData>> resultSets = new ArrayList<Set<CategoryTagData>>(keys.length);

        for(String key : keys)
        {
            resultSets.add(getCategoriesForKey(key));
        }

        Set<CategoryTagData> resultSet = resultSets.get(0);

        for(int i=1;i<resultSets.size();i++)
        {
            resultSet.retainAll(resultSets.get(i));
        }

        return resultSet;

    }

    private TreeSet<CategoryTagData> getCategoriesForKey(String key)
    {
        TreeSet<CategoryTagData> result = null;

        if(mCacheForShopSearchKeys.containsKey(key))
        {
            result = mCacheForShopSearchKeys.get(key);

        }
        else
        {
            result = HikeStickerSearchDatabase.getInstance().searchIntoFTSAndFindCategoryList(key);

            if(result == null)
            {
                result = new TreeSet<CategoryTagData>();
            }

            mCacheForShopSearchKeys.put(key, result);
        }

        return result;
    }

    public void clearTransientResources()
    {

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

    private List<StickerCategory> getOrderedCategories(Set<CategoryTagData> querySet, Set<CategoryTagData> keySet)
    {
        List<StickerCategory> result = new ArrayList<>(querySet.size());

        for(CategoryTagData categoryTagData : querySet)
        {
            StickerCategory category = categoryTagData.getCategory();
            if(category != null)
            {
                result.add(category);
            }
        }

        return result;
    }

    public void insertCategoryTags(List<CategoryTagData> categoryTagData)
    {
        CategoryTagInsertTask categoryTagInsertTask = new CategoryTagInsertTask(categoryTagData);
        categorySearchEngine.runOnQueryThread(categoryTagInsertTask);
    }

}
