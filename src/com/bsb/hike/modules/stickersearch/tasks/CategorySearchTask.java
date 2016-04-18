package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;

import java.util.List;

public class CategorySearchTask implements Runnable
{
	private String query;

    private CategorySearchListener mListener;

	public CategorySearchTask(String query,CategorySearchListener listener)
	{
        this.query = query;
        this.mListener = listener;
	}

	@Override
	public void run()
	{
		List<StickerCategory> results = CategorySearchManager.getInstance().searchForPacks(query);

        if(results == null)
        {
            mListener.onNoCategoriesFound(query);
            return;
        }

        mListener.onSearchCompleted(results);

	}
}