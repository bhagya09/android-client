package com.bsb.hike.modules.stickersearch.ui;

import java.util.List;

import android.support.v7.widget.SearchView;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class CategorySearchWatcher implements CategorySearchListener, SearchView.OnQueryTextListener
{
	public static final String TAG = CategorySearchWatcher.class.getSimpleName();

	private CategorySearchListener mListener;

    private String currentQueryState;

	public CategorySearchWatcher(CategorySearchListener listener)
	{
		Logger.i(TAG, "Initialising Category tag watcher...");
		this.mListener = listener;
	}

	public void releaseResources()
	{
		CategorySearchManager.getInstance().clearTransientResources();
        currentQueryState = null;
		mListener = null;
	}

	@Override
	public void onSearchCompleted(final List<StickerCategory> categories)
	{
		CategorySearchManager.getInstance().getSearchEngine().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener == null)
				{
					Logger.e(TAG, "onSearchCompleted() : No listener present");
                    return;
				}
				mListener.onSearchCompleted(categories);
			}
		}, 0);
	}

	@Override
	public void onNoCategoriesFound(final String query)
	{
		CategorySearchManager.getInstance().getSearchEngine().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener == null)
				{
					Logger.e(TAG, "onNoCategoriesFound() : No listener present");
                    return;
				}

				mListener.onNoCategoriesFound(query);
			}
		}, 0);
	}

	@Override
	public void onSearchInitiated()
	{
		CategorySearchManager.getInstance().getSearchEngine().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener == null)
				{
					Logger.e(TAG, "onSearchInitiated() : No listener present");
					return;
				}

				mListener.onSearchInitiated();
			}
		}, 0);
	}

    @Override
	public boolean onQueryTextSubmit(String query)
	{
		return CategorySearchManager.getInstance().onQueryTextSubmit(query, this);
	}

	@Override
	public boolean onQueryTextChange(final String query)
	{
		currentQueryState = new String(query);

		CategorySearchManager.getInstance().getSearchEngine().runOnSearchThread(new Runnable()
		{
			private String capturedQueryState = query;

			@Override
			public void run()
			{
				if (capturedQueryState.equals(currentQueryState))
				{
					Logger.e(TAG, "Yo going to search oq= " + capturedQueryState + " <> nq=" + currentQueryState);
					CategorySearchManager.getInstance().onQueryTextChange(currentQueryState, CategorySearchWatcher.this);
				}
				else
				{
					Logger.i(TAG, "ignoring since changed oq= " + capturedQueryState + " <> nq=" + currentQueryState);
				}
			}
		}, HikeSharedPreferenceUtil.getInstance().getData(CategorySearchManager.AUTO_SEARCH_TIME, CategorySearchManager.DEFAULT_AUTO_SEARCH_TIME));

		return true;
	}
}
