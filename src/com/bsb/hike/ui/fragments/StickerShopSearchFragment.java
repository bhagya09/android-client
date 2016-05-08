package com.bsb.hike.ui.fragments;

import java.util.HashMap;
import java.util.List;

import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerShopSearchAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchAnalyticsTask;
import com.bsb.hike.modules.stickersearch.ui.CategorySearchWatcher;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

public class StickerShopSearchFragment extends StickerShopBaseFragment implements OnScrollListener, SearchView.OnQueryTextListener, CategorySearchListener,
		AdapterView.OnItemClickListener
{

	public static final String TAG = StickerShopSearchFragment.class.getSimpleName();

	private CategorySearchWatcher searchWatcher;

	private List<StickerCategory> searchedCategories;

	private StickerShopSearchAdapter mAdapter;

	private CustomFontTextView searchFailedMessageView;

    private String currentQuery;

	public StickerShopSearchFragment()
	{
		super();
		searchWatcher = new CategorySearchWatcher(this);
	}

	@Override
	public void doInitialSetup()
	{
		searchFailedMessageView = (CustomFontTextView) searchFailedState.findViewById(R.id.empty_search_txt);
		initAdapterAndList();
		HikeMessengerApp.getPubSub().addListeners(StickerShopSearchFragment.this, pubSubListeners);
		registerListener();
	}

	private void initAdapterAndList()
	{
		listview = (ListView) getView().findViewById(android.R.id.list);
		listview.setVisibility(View.VISIBLE);

		stickerCategoriesMap = new HashMap<String, StickerCategory>();
		stickerCategoriesMap.putAll(StickerManager.getInstance().getStickerCategoryMap());

		// to fix
		mAdapter = new StickerShopSearchAdapter(getActivity(), stickerCategoriesMap);

		listview.setAdapter(mAdapter);
		listview.setOnScrollListener(this);
		listview.setOnItemClickListener(this);
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
		currentQuery = query;
		return searchWatcher.onQueryTextSubmit(query);
	}

	@Override
	public boolean onQueryTextChange(String query)
	{
		currentQuery = query;
		return searchWatcher.onQueryTextChange(query);
	}

	@Override
	public void onSearchCompleted(List<StickerCategory> categories)
	{
		if (!isAdded())
		{
			return;
		}

        if (Utils.isUserOnline(HikeMessengerApp.getInstance().getApplicationContext()))
		{
			loadingEmptyState.setVisibility(View.GONE);
			searchFailedState.setVisibility(View.GONE);
            loadingFailedEmptyState.setVisibility(View.GONE);
            listview.setVisibility(View.VISIBLE);
			mAdapter.updateSearchresult(categories);
		}
		else
		{
			showNoInternetConnectionState();
		}
	}

	@Override
	public void onNoCategoriesFound(String query)
	{
		if (!isAdded())
		{
			return;
		}

		if (Utils.isUserOnline(HikeMessengerApp.getInstance().getApplicationContext()))
		{
            loadingEmptyState.setVisibility(View.GONE);
            loadingFailedEmptyState.setVisibility(View.GONE);
            listview.setVisibility(View.GONE);
			setSearchEmptyState(query);
			searchFailedState.setVisibility(View.VISIBLE);
		}
		else
		{
			showNoInternetConnectionState();
		}

	}

	@Override
	public void onSearchInitiated()
	{
		if (!isAdded())
		{
			return;
		}

		searchFailedState.setVisibility(View.GONE);
		loadingFailedEmptyState.setVisibility(View.GONE);
		listview.setVisibility(View.GONE);
		loadingEmptyState.setVisibility(View.VISIBLE);
	}

    private void showNoInternetConnectionState()
	{
		loadingEmptyState.setVisibility(View.GONE);
		searchFailedState.setVisibility(View.GONE);
		listview.setVisibility(View.GONE);
		loadingFailedEmptyState.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (position < 0 || position > mAdapter.getCount())
		{
			Logger.d(TAG, "position is less than 0. wrong item clicked");
			return;
		}
		String categoryId = mAdapter.getItem(position);
		IntentFactory.openPackPreviewIntent(getActivity(), categoryId, position, StickerConstants.PackPreviewClickSource.SHOP_SEARCH.getValue() + HikeConstants.DELIMETER
				+ currentQuery);
		CategorySearchManager.sendCategorySearchResultResponseAnalytics(CategorySearchAnalyticsTask.SHOP_SEARCH_PACK_PREVIEWED_BUTTON_TRIGGER);
	}

	@Override
	protected void notifyAdapter()
	{
		if (mAdapter == null)
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

	@Override
	protected StickerOtherIconLoader getStickerPreviewLoader()
	{
		if (mAdapter != null)
		{
			return mAdapter.getStickerPreviewLoader();
		}

		return null;
	}

	public void releaseSearchResources()
	{
		searchWatcher.releaseResources();
	}

	private void setSearchEmptyState(String query)
	{
		String emptyText = String.format(HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.no_sticker_pack_match_found), query);
		if (!TextUtils.isEmpty(query))
		{
			SpannableString spanEmptyText = new SpannableString(emptyText);
			String darkText = "'" + query + "'";
			int start = spanEmptyText.toString().indexOf(darkText);
			int end = start + darkText.length();
			spanEmptyText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.standard_light_grey2)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			searchFailedMessageView.setText(spanEmptyText, TextView.BufferType.SPANNABLE);
		}
		else
		{
			searchFailedMessageView.setText(emptyText);
		}
	}

}