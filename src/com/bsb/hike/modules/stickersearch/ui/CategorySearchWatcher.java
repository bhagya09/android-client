package com.bsb.hike.modules.stickersearch.ui;

import java.util.List;

import android.support.v7.widget.SearchView;
import android.text.TextUtils;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * @author akhiltripathi
 *
 * This is a watcher model class which captures text changes on searchView ,
 * Communicates the search result to CategorySearchListener provided
 * As per urrent design implementation there is one CategorySearchListener per watcher object
 */

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
		currentQueryState = null;
		return CategorySearchManager.getInstance().onQueryTextSubmit(query, this);
	}

    /**
     * Starts a timer to see if there is a possibility to trigger auto search
     * The timer is of time period defined in the AUTO_SEARCH_TIME pref [Server Controlled] [in milliseconds]
     * After the time period it checks if the user query has changed in the given time period. If not then the auto search for packs is triggered
     *
     * @param query
     */
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
				if (TextUtils.isEmpty(capturedQueryState) || TextUtils.isEmpty(currentQueryState))
				{
					Logger.e(TAG, "onQueryTextChange() : ignoring : empty query");
					return;
				}
                
				if (capturedQueryState.equals(currentQueryState))
				{
					Logger.i(TAG, "onQueryTextChange(): going to search oq= " + capturedQueryState + " <> nq=" + currentQueryState);
					CategorySearchManager.getInstance().onQueryTextChange(currentQueryState, CategorySearchWatcher.this);
				}
				else
				{
					Logger.i(TAG, "onQueryTextChange(): ignoring since changed oq= " + capturedQueryState + " <> nq=" + currentQueryState);
				}
			}
		}, HikeSharedPreferenceUtil.getInstance().getData(CategorySearchManager.AUTO_SEARCH_TIME, CategorySearchManager.DEFAULT_AUTO_SEARCH_TIME));

		return true;
	}
}
