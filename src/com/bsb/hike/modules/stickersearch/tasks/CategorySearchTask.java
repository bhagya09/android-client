package com.bsb.hike.modules.stickersearch.tasks;

import java.util.List;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
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
		List<StickerCategory> results = CategorySearchManager.getInstance().searchForPacks(query);

		sendResponse(results);
	}

	private void sendResponse(List<StickerCategory> results)
	{
		if (Utils.isEmpty(results))
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