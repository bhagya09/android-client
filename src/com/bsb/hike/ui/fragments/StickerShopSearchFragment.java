package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerShopAdapter;
import com.bsb.hike.adapters.StickerShopSearchAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.ui.CategorySearchWatcher;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerShopSearchFragment extends StickerShopBaseFragment implements OnScrollListener, SearchView.OnQueryTextListener, CategorySearchListener,
		AdapterView.OnItemClickListener
{

	private static final String TAG = StickerShopSearchFragment.class.getSimpleName();

	private CategorySearchWatcher searchWatcher;

    private List<StickerCategory> searchedCategories;

    private StickerShopSearchAdapter mAdapter;

	public StickerShopSearchFragment()
	{
		super();
		searchWatcher = new CategorySearchWatcher(this);
	}

	@Override
	public void doInitialSetup()
	{
		FetchCursorTask fetchCursorTask = new FetchCursorTask();
		fetchCursorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

    private class FetchCursorTask extends AsyncTask<Void, Void, List<StickerCategory>>
	{
		@Override
		protected List<StickerCategory> doInBackground(Void... arg0)
		{
			if (!isAdded()) // not attached to any activity
			{
				return null;
			}

            searchedCategories = HikeConversationsDatabase.getInstance().getDefaultCategoriesForShopSearch();

			return searchedCategories;
		}

		@Override
		protected void onPreExecute()
		{
			getView().findViewById(R.id.loading_data).setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(List<StickerCategory> categories)
		{
			if (categories == null || !isAdded())
			{
				return;
			}
			super.onPostExecute(categories);
			View parent = getView();
			if (parent != null && parent.findViewById(R.id.loading_data) != null)
			{
				parent.findViewById(R.id.loading_data).setVisibility(View.GONE);
			}
			initAdapterAndList(categories);
			HikeMessengerApp.getPubSub().addListeners(StickerShopSearchFragment.this, pubSubListeners);
			registerListener();
		}
	}

	private void initAdapterAndList(List<StickerCategory> categories)
	{
		listview = (ListView) getView().findViewById(android.R.id.list);
		listview.setVisibility(View.VISIBLE);

		stickerCategoriesMap = new HashMap<String, StickerCategory>();
		stickerCategoriesMap.putAll(StickerManager.getInstance().getStickerCategoryMap());
        //Tofix
		mAdapter = new StickerShopSearchAdapter(getActivity(),categories,stickerOtherIconLoader);

		listview.setAdapter(mAdapter);
		listview.setOnScrollListener(this);
		listview.setOnItemClickListener(this);

		if ((mAdapter == null) || mAdapter.getCount() == 0)
		{
			listview.setVisibility(View.GONE);
		}
		else
		{
			listview.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
        if (previousFirstVisibleItem != firstVisibleItem)
        {
            long currTime = System.currentTimeMillis();
            long timeToScrollOneElement = currTime - previousEventTime;
            velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

            previousFirstVisibleItem = firstVisibleItem;
            previousEventTime = currTime;
        }

        if (mAdapter == null)
        {
            return;
        }
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
        mAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	public static StickerShopSearchFragment newInstance()
	{
		StickerShopSearchFragment stickerShopFragment = new StickerShopSearchFragment();
		return stickerShopFragment;
	}

	@Override
	public boolean onQueryTextSubmit(String query)
	{
		searchWatcher.onQueryTextSubmit(query);
		return true;
	}

	@Override
	public boolean onQueryTextChange(String query)
	{
		searchWatcher.onQueryTextChange(query);
		return false;
	}

	@Override
	public void onSearchCompleted(List<StickerCategory> categories)
	{
        mAdapter.updateSearchresult(categories);
	}

	@Override
	public void onNoCategoriesFound(String query)
	{

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (position <= 0 || position > mAdapter.getCount())
		{
			Logger.d(TAG, "position is less than 0. wrong item clicked");
			return;
		}
		String categoryId = mAdapter.getItem(position);
		IntentFactory.openPackPreviewIntent(getActivity(), categoryId);
		mAdapter.setShownPackPreviewFtue();
	}

    @Override
    protected void notifyAdapter()
    {
        if(mAdapter == null)
        {
            return;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void reloadAdapter()
    {
        notifyAdapter();
    }

}