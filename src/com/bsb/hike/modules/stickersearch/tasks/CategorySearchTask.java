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

	private boolean performPartialSearch;

	public CategorySearchTask(String query, CategorySearchListener listener, boolean partialSearch)
	{
		this.query = preProcessQuery(query);
		this.mListener = listener;
		this.performPartialSearch = partialSearch;
	}

	@Override
	public void run()
	{
		List<StickerCategory> results = CategorySearchManager.getInstance().searchForPacks(query);

		if (!performPartialSearch)
		{
			sendResponse(results);
			return;
		}

		if (Utils.isEmpty(results) && query.indexOf(' ') > 0)
		{
			sendResponse(CategorySearchManager.getInstance().searchForPacks(query.substring(0, query.lastIndexOf(' '))));
		}
		else
		{
			sendResponse(results);
		}

	}

	private void sendResponse(List<StickerCategory> results)
	{
		if (!Utils.isEmpty(results))
		{
			mListener.onSearchCompleted(results);
		}
		else if (!performPartialSearch)
		{
			mListener.onNoCategoriesFound(query);
		}
	}

	private String preProcessQuery(String query)
	{
		return query.trim().toLowerCase();
	}
}