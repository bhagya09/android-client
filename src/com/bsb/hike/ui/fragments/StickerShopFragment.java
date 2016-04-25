package com.bsb.hike.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerShopAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerShopDownloadTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class StickerShopFragment extends Fragment implements OnScrollListener, Listener, AdapterView.OnItemClickListener
{
	private String[] pubSubListeners = {HikePubSub.STICKER_CATEGORY_MAP_UPDATED, HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE};

	private StickerShopAdapter mAdapter;
	
	private ListView listview;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;
	
	Map<String, StickerCategory> stickerCategoriesMap;
	
	private final int NOT_DOWNLOADING = 0;
	
	private final int DOWNLOADING = 1;
	
	private final int DOWNLOAD_FAILED = 2;
	
	private int downloadState = NOT_DOWNLOADING;
	
	View loadingFooterView, downloadFailedFooterView, loadingEmptyState, loadingFailedEmptyState;
	
	TextView loadingFailedEmptyStateMainText, loadingFailedEmptyStateSubText;
	
	private int currentCategoriesCount;

	private static final String TAG = StickerShopFragment.class.getSimpleName();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		View parent = inflater.inflate(R.layout.sticker_shop, null);
		loadingEmptyState = parent.findViewById(R.id.loading_data);
		loadingFailedEmptyState = parent.findViewById(R.id.loading_failed);
		
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO Register PubSub Listeners
		super.onActivityCreated(savedInstanceState);
		if(StickerManager.getInstance().stickerShopUpdateNeeded())
		{
			HikeConversationsDatabase.getInstance().clearStickerShop();
		}
		
		executeFetchCursorTask(new FetchCursorTask());
		
		loadingFailedEmptyState.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				downLoadStickerData();
			}
		});
	}
	
	private void executeFetchCursorTask(FetchCursorTask fetchCursorTask)
	{
		if (Utils.isHoneycombOrHigher())
		{
			fetchCursorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			fetchCursorTask.execute();
		}
	}
	
	private class FetchCursorTask extends AsyncTask<Void, Void, Pair<Cursor,Drawable>>
	{
		@Override
		protected Pair<Cursor,Drawable> doInBackground(Void... arg0)
		{
			if(!isAdded()) // not attched to any activity
			{
				return null;
			}
			Bitmap bmp = HikeBitmapFactory.decodeResource(HikeMessengerApp.getInstance().getResources(), R.drawable.art_banner);
			Drawable dr = HikeBitmapFactory.getBitmapDrawable(HikeMessengerApp.getInstance().getResources(), bmp);
			Cursor cursor = HikeConversationsDatabase.getInstance().getCursorForStickerShop();
			return new Pair(cursor, dr);
		}

		@Override
		protected void onPreExecute()
		{
			getView().findViewById(R.id.loading_data).setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Pair<Cursor,Drawable> pair)
		{
			if(pair == null || !isAdded())
			{
				return ;
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

	@Override
	public void onDestroy()
	{
		// TODO Clear the adapter and stickercategory list as well
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		unregisterListeners();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	private void initAdapterAndList(Cursor cursor, Drawable headerViewDrawable)
	{
		listview = (ListView) getView().findViewById(android.R.id.list);
		listview.setVisibility(View.VISIBLE);
		View headerView = getActivity().getLayoutInflater().inflate(R.layout.sticker_shop_header, null);
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
		
		if((mAdapter.getCursor() == null) || mAdapter.getCursor().getCount() == 0)
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
		if(currentCategoriesCount == 0)
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
		

		StickerShopDownloadTask stickerShopDownloadTask = new StickerShopDownloadTask(currentCategoriesCount);
		stickerShopDownloadTask.execute();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		// TODO Auto-generated method stub
		if (downloadState == NOT_DOWNLOADING && (!mAdapter.isEmpty()) &&(firstVisibleItem + visibleItemCount)  > (totalItemCount - 5) && StickerManager.getInstance().moreDataAvailableForStickerShop())
		{
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

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.STICKER_CATEGORY_MAP_UPDATED.equals(type))
		{
			if(!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if(mAdapter == null)
					{
						return;
					}
					updateStickerCategoriesMap(StickerManager.getInstance().getStickerCategoryMap());
					mAdapter.notifyDataSetChanged();
				}
			});
		}
		else if(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS.equals(type))
		{
			// TODO Auto-generated method stub
			JSONArray resultData = (JSONArray) object;
			if(resultData.length() == 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKER_SHOP_DATA_FULLY_FETCHED, true);
			}
			else
			{
				//TODO we should also update stickerCategoriesMap in StickerManager from here as well
				HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(resultData);
			}
			final Cursor updatedCursor = HikeConversationsDatabase.getInstance().getCursorForStickerShop();
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if(currentCategoriesCount == 0)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.LAST_STICKER_SHOP_UPDATE_TIME, System.currentTimeMillis());
					}
					mAdapter.changeCursor(updatedCursor);
					listview.setVisibility(View.VISIBLE);
					listview.removeFooterView(loadingFooterView);
					loadingEmptyState.setVisibility(View.GONE);
					loadingFailedEmptyState.setVisibility(View.GONE);
					
					mAdapter.notifyDataSetChanged();
					downloadState = NOT_DOWNLOADING;
				}
			});
		}
		else if(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE.equals(type))
		{
			final HttpException exception = (HttpException) object;
			
			//footerView.setVisibility(View.GONE);
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					downloadState = DOWNLOAD_FAILED;
					if(currentCategoriesCount == 0)
					{
						loadingEmptyState.setVisibility(View.GONE);
						loadingFailedEmptyState.setVisibility(View.VISIBLE);
						
						if (exception != null && exception.getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
						{
							loadingFailedEmptyStateMainText.setText(R.string.shop_download_failed_out_of_space);
							loadingFailedEmptyStateSubText.setVisibility(View.GONE);
						}
						else if(exception != null && exception.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
						{
							loadingFailedEmptyStateMainText.setText(R.string.shop_loading_failed_no_internet);
							loadingFailedEmptyStateSubText.setVisibility(View.VISIBLE);
							loadingFailedEmptyStateSubText.setText(R.string.shop_loading_failed_switch_on);
						}
						else
						{
							loadingFailedEmptyStateMainText.setText(R.string.shop_download_failed);
							loadingFailedEmptyStateSubText.setVisibility(View.GONE);
						}
					}
					else
					{
						listview.removeFooterView(loadingFooterView);
						listview.removeFooterView(downloadFailedFooterView);
						listview.addFooterView(downloadFailedFooterView);

						TextView failedText = (TextView) downloadFailedFooterView.findViewById(R.id.footer_downloading_failed);
						if (exception != null && exception.getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
						{
							failedText.setText(R.string.shop_download_failed_out_of_space);
						}
						else if(exception != null && exception.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
						{
							failedText.setText(R.string.shop_loading_failed_no_internet);
						}
						
						else
						{
							failedText.setText(R.string.shop_download_failed);
						}
					}
				}
			});
		}

	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(!isAdded())
			{
				return;
			}
			if (intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED))
			{
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				final StickerCategory category = stickerCategoriesMap.get(categoryId);
				if (category == null)
				{
					return;
				}
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if(mAdapter == null)
						{
							return;
						}
						mAdapter.notifyDataSetChanged();
					}
				});
			}
			else if (intent.getAction().equals(StickerManager.STICKERS_PROGRESS))
			{
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				final StickerCategory category = stickerCategoriesMap.get(categoryId);
				if (category == null)
				{
					return;
				}
			}
			else if(intent.getAction().equals(StickerManager.STICKER_PREVIEW_DOWNLOADED))
			{
				if(mAdapter == null)
				{
					return ;
				}
				mAdapter.notifyDataSetChanged();
			}
			else
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				final String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final DownloadType type = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
				final StickerCategory category = stickerCategoriesMap.get(categoryId);
				final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
				if (category == null)
				{
					return;
				}

				// if this category is already loaded then only proceed else ignore
				if (intent.getAction().equals(StickerManager.STICKERS_FAILED) && (DownloadType.NEW_CATEGORY.equals(type) || DownloadType.MORE_STICKERS.equals(type)))
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if(mAdapter == null)
							{
								return;
							}
							if(failedDueToLargeFile)
							{
								Toast.makeText(getActivity(), R.string.out_of_space, Toast.LENGTH_SHORT).show();
							}
							category.setState(StickerCategory.RETRY);
							mAdapter.notifyDataSetChanged();
						}
					});
				}
				else if (intent.getAction().equals(StickerManager.STICKERS_DOWNLOADED) && DownloadType.NEW_CATEGORY.equals(type))
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if(mAdapter == null)
							{
								return;
							}
							mAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};

	public void updateStickerCategoriesMap(Map<String, StickerCategory> stickerCategoryMap)
	{
		this.stickerCategoriesMap.clear();
		this.stickerCategoriesMap.putAll(stickerCategoryMap);
	}

	public static StickerShopFragment newInstance()
	{
		StickerShopFragment stickerShopFragment = new StickerShopFragment();
		return stickerShopFragment;
	}
	
	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.STICKERS_PROGRESS);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKER_PREVIEW_DOWNLOADED);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

	}
	
	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(position <= 0 || position > mAdapter.getCount())
		{
			Logger.d(TAG, "position is less than 0. wrong item clicked");
			return ;
		}
		String categoryId = mAdapter.getItem(position - 1);
		IntentFactory.openPackPreviewIntent(getActivity(), categoryId, position, StickerConstants.PackPreviewClickSource.SHOP);
		mAdapter.setShownPackPreviewFtue();
	}
}