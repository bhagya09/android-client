package com.bsb.hike.ui.fragments;

import java.util.HashMap;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerShopAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerShopFragment extends StickerShopBaseFragment implements OnScrollListener, AdapterView.OnItemClickListener
{

	public static final String TAG = StickerShopFragment.class.getSimpleName();

	StickerShopAdapter mAdapter;

	private int FIRST_PACK_VIEW_ROW_NUMBER = 1;

	@Override
	public void doInitialSetup()
	{
		FetchCursorTask fetchCursorTask = new FetchCursorTask();
		fetchCursorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class FetchCursorTask extends AsyncTask<Void, Void, Pair<Cursor, Drawable>>
	{

		@Override
		protected Pair<Cursor, Drawable> doInBackground(Void... arg0)
		{
			if (!isAdded()) // not attached to any activity
			{
				return null;
			}

			Bitmap bmp = HikeBitmapFactory.decodeResource(HikeMessengerApp.getInstance().getResources(), R.drawable.art_banner);
			Drawable dr = HikeBitmapFactory.getBitmapDrawable(HikeMessengerApp.getInstance().getResources(), bmp);

			Cursor cursor = HikeConversationsDatabase.getInstance()
					.getCursorFromStickerCategoryTable(StickerManager.getInstance().moreDataAvailableForStickerShop() ? 0 : Integer.MAX_VALUE);
			currentCategoriesCount = cursor != null ? cursor.getCount() : 0;

			return new Pair(cursor, dr);
		}

		@Override
		protected void onPreExecute()
		{
			getView().findViewById(R.id.loading_data).setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Pair<Cursor, Drawable> pair)
		{
			if (pair == null || !isAdded())
			{
				return;
			}
			super.onPostExecute(pair);
			View parent = getView();
			if (parent != null && parent.findViewById(R.id.loading_data) != null)
			{
				parent.findViewById(R.id.loading_data).setVisibility(View.GONE);
			}
			initAdapterAndList(pair.first, pair.second);
			HikeMessengerApp.getPubSub().addListeners(StickerShopFragment.this, pubSubListeners);
			registerListener();
		}
	}

	private void initAdapterAndList(Cursor cursor, Drawable headerViewDrawable)
	{
		listview = (ListView) getView().findViewById(android.R.id.list);
		listview.setVisibility(View.VISIBLE);
		headerView = getActivity().getLayoutInflater().inflate(R.layout.sticker_shop_header, null);
		ImageView shopBanner = (ImageView) headerView.findViewById(R.id.shop_banner);
		shopBanner.setImageDrawable(headerViewDrawable);
		loadingFooterView = getActivity().getLayoutInflater().inflate(R.layout.sticker_shop_footer, null);
		downloadFailedFooterView = getActivity().getLayoutInflater().inflate(R.layout.sticker_shop_footer_loading_failed, null);

		stickerCategoriesMap = new HashMap<String, StickerCategory>();
		stickerCategoriesMap.putAll(StickerManager.getInstance().getStickerCategoryMap());
		mAdapter = new StickerShopAdapter(getActivity(), cursor, stickerCategoriesMap);

		listview.addHeaderView(headerView);
		listview.addFooterView(loadingFooterView);
		listview.addFooterView(downloadFailedFooterView);
		listview.setAdapter(mAdapter);
		listview.setOnScrollListener(this);
		listview.removeFooterView(loadingFooterView);
		listview.removeFooterView(downloadFailedFooterView);
		listview.setOnItemClickListener(this);

		downloadFailedFooterView.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				downLoadStickerData();
			}
		});

		loadingFooterView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Logger.d(TAG, "LOADING FOOTER VIEW CLICKED");
			}
		});

		if ((mAdapter.getCursor() == null) || mAdapter.getCursor().getCount() == 0)
		{
			listview.setVisibility(View.GONE);
			downLoadStickerData();
		}
		else
		{
			listview.setVisibility(View.VISIBLE);
		}
	}

	public void downLoadStickerData()
	{
		currentCategoriesCount = (mAdapter == null) || (mAdapter.getCursor() == null) ? 0 : mAdapter.getCursor().getCount();
		downloadState = DOWNLOADING;
		loadingFailedEmptyStateMainText = (TextView) loadingFailedEmptyState.findViewById(R.id.main_text);
		loadingFailedEmptyStateSubText = (TextView) loadingFailedEmptyState.findViewById(R.id.sub_text);
		if (currentCategoriesCount == 0)
		{
			loadingEmptyState.setVisibility(View.VISIBLE);
			loadingFailedEmptyState.setVisibility(View.GONE);
		}

		else
		{
			loadingEmptyState.setVisibility(View.GONE);
			loadingFailedEmptyState.setVisibility(View.GONE);
			listview.removeFooterView(downloadFailedFooterView);
			listview.removeFooterView(loadingFooterView);
			listview.addFooterView(loadingFooterView);
		}

		StickerManager.getInstance().executeFetchShopPackTask(currentCategoriesCount + StickerManager.getInstance().getShopPageSize());
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if ((firstVisibleItem + visibleItemCount) > maxIndexShown)
		{
			maxIndexShown = firstVisibleItem + visibleItemCount;
		}

		if (downloadState == NOT_DOWNLOADING && (!mAdapter.isEmpty()) && (firstVisibleItem + visibleItemCount) > (totalItemCount - (StickerManager.getInstance().getShopPageSize() / 2))
				&& StickerManager.getInstance().moreDataAvailableForStickerShop()) {
			downLoadStickerData();
		}

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

	public static StickerShopFragment newInstance()
	{
		StickerShopFragment stickerShopFragment = new StickerShopFragment();
		return stickerShopFragment;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (position < FIRST_PACK_VIEW_ROW_NUMBER || position > mAdapter.getCount())
		{
			Logger.d(TAG, "position is less than 0. wrong item clicked");
			return;
		}
		String categoryId = mAdapter.getItem(position - FIRST_PACK_VIEW_ROW_NUMBER);
		IntentFactory.openPackPreviewIntent(getActivity(), categoryId, position, StickerConstants.PackPreviewClickSource.SHOP, "");
		mAdapter.setShownPackPreviewFtue();
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
		if (mAdapter == null)
		{
			return;
		}

		Cursor reloadedCursor = HikeConversationsDatabase.getInstance().getCursorFromStickerCategoryTable(currentCategoriesCount + StickerManager.getInstance().getShopPageSize());
		if (reloadedCursor != null) {
			currentCategoriesCount = reloadedCursor.getCount();
		}
		mAdapter.changeCursor(reloadedCursor);
		mAdapter.notifyDataSetChanged();
	}

	public void showBanner(boolean visible)
	{
		if (listview == null)
		{
			return;
		}
        
		if (visible)
		{
			listview.addHeaderView(headerView);
			FIRST_PACK_VIEW_ROW_NUMBER = 1;
		}
		else
		{
			listview.removeHeaderView(headerView);
			FIRST_PACK_VIEW_ROW_NUMBER = 0;
		}
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

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}
}