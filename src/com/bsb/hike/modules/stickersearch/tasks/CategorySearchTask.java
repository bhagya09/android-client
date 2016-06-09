package com.bsb.hike.modules.stickersearch.tasks;

import java.util.List;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 12/04/16.
 */

public class CategorySearchTask implements Runnable
{
	private String query;

	private CategorySearchListener mListener;

    private boolean onTextSubmit;

    public CategorySearchTask(String query, CategorySearchListener listener, boolean onTextSubmit) {
        this.query = preProcessQuery(query);
        this.mListener = listener;
        this.onTextSubmit = onTextSubmit;
    }

	@Override
	public void run()
	{
		if (mListener == null)
		{
			return;
		}

		mListener.onSearchInitiated();

		List<StickerCategory> results = CategorySearchManager.getInstance().searchForPacks(query, onTextSubmit);

		sendResponse(results);

	}

	private void sendResponse(List<StickerCategory> results)
	{
		if (Utils.isEmpty(results) || (query.length() <= HikeSharedPreferenceUtil.getInstance().getData(CategorySearchManager.SEARCH_QUERY_LENGTH_THRESHOLD, CategorySearchManager.DEFAULT_SEARCH_QUERY_LENGTH_THRESHOLD)))
		{
            mListener.onNoCategoriesFound(query);
		}
		else
		{
            mListener.onSearchCompleted(results);
		}
	}

    private String preProcessQuery(String query) {
        query = query.replaceAll("^\\s+", "");//trimming all leading spaces
        return query.toLowerCase();
    }
}