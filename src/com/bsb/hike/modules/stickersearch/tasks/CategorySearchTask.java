package com.bsb.hike.modules.stickersearch.tasks;

import java.util.List;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class CategorySearchTask implements Runnable
{
	private String query;

	private CategorySearchListener mListener;

	public CategorySearchTask(String query, CategorySearchListener listener)
	{
		this.query = preProcessQuery(query);
		this.mListener = listener;
	}

	@Override
	public void run()
	{
		if (mListener == null)
		{
			return;
		}

		mListener.onSearchInitiated();

		List<StickerCategory> results = CategorySearchManager.getInstance().searchForPacks(query);

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

	private String preProcessQuery(String query)
	{
		return query.trim().toLowerCase();
	}
}