package com.bsb.hike.ui.fragments;

import java.util.Map;

import org.json.JSONArray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

/**
 * Abstract Fragment which provides the basic functionalities of SHOP including download and pack preview and DB reads
 */

public abstract class StickerShopBaseFragment extends Fragment implements Listener
{
	protected String[] pubSubListeners = { HikePubSub.STICKER_CATEGORY_MAP_UPDATED, HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE };

	protected StickerOtherIconLoader stickerOtherIconLoader;

	protected ListView listview;

	protected int previousFirstVisibleItem;

	protected int velocity;

	protected long previousEventTime;

	protected Map<String, StickerCategory> stickerCategoriesMap;

	protected final int NOT_DOWNLOADING = 0;

	protected final int DOWNLOADING = 1;

	protected final int DOWNLOAD_FAILED = 2;

	protected int downloadState = NOT_DOWNLOADING;

	View loadingFooterView, downloadFailedFooterView, loadingEmptyState, loadingFailedEmptyState, searchFailedState;

	TextView loadingFailedEmptyStateMainText, loadingFailedEmptyStateSubText;

	protected int currentCategoriesCount;

	private static final String TAG = StickerShopBaseFragment.class.getSimpleName();

	protected View headerView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticker_shop, null);
		loadingEmptyState = parent.findViewById(R.id.loading_data);
		loadingFailedEmptyState = parent.findViewById(R.id.loading_failed);
		searchFailedState = parent.findViewById(R.id.search_failed);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		if (StickerManager.getInstance().stickerShopUpdateNeeded())
		{
			HikeConversationsDatabase.getInstance().clearStickerShop();
		}

		doInitialSetup();
	}

    /**
     * Method called onActivityCreated.
     * Implement adapter initialisation and Data read here
     */
	protected abstract void doInitialSetup();

    /**
     * Wrapper for notifyDatasetChanged of the Fragment adapter
     */
	protected abstract void notifyAdapter();

    /**
     * Method called when data needs to be reloaded due to shop data update/download
     */
	protected abstract void reloadAdapter();

    /**
     *
     * @return StickerOtherIconLoader : Thumbnail Loader object of the fragment which extends ImageWorker
     */
	protected abstract StickerOtherIconLoader getStickerPreviewLoader();

	@Override
	public void onDestroy()
	{
		unregisterListeners();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (getStickerPreviewLoader() != null)
		{
			getStickerPreviewLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (getStickerPreviewLoader() != null)
		{
			getStickerPreviewLoader().setExitTasksEarly(false);
		}
		notifyAdapter();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.STICKER_CATEGORY_MAP_UPDATED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateStickerCategoriesMap(StickerManager.getInstance().getStickerCategoryMap());
					notifyAdapter();
				}
			});
		}
		else if (HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS.equals(type))
		{
			JSONArray resultData = (JSONArray) object;
			if (resultData.length() == 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKER_SHOP_DATA_FULLY_FETCHED, true);
			}
			else
			{
				// TODO we should also update stickerCategoriesMap in StickerManager from here as well
				HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(resultData, true);
			}
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (currentCategoriesCount == 0)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.LAST_STICKER_SHOP_UPDATE_TIME, System.currentTimeMillis());
					}
					listview.setVisibility(View.VISIBLE);
					listview.removeFooterView(loadingFooterView);
					loadingEmptyState.setVisibility(View.GONE);
					loadingFailedEmptyState.setVisibility(View.GONE);
					searchFailedState.setVisibility(View.GONE);
					reloadAdapter();
					downloadState = NOT_DOWNLOADING;
				}
			});
		}
		else if (HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE.equals(type))
		{
			final HttpException exception = (HttpException) object;

			// footerView.setVisibility(View.GONE);
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
					if (currentCategoriesCount == 0)
					{
						loadingEmptyState.setVisibility(View.GONE);
						searchFailedState.setVisibility(View.GONE);
						loadingFailedEmptyState.setVisibility(View.VISIBLE);

						if (exception != null && exception.getErrorCode() == HttpException.REASON_CODE_OUT_OF_SPACE)
						{
							loadingFailedEmptyStateMainText.setText(R.string.shop_download_failed_out_of_space);
							loadingFailedEmptyStateSubText.setVisibility(View.GONE);
						}
						else if (exception != null && exception.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
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
						else if (exception != null && exception.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
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
			if (!isAdded())
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
						notifyAdapter();
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
			else if (intent.getAction().equals(StickerManager.STICKER_PREVIEW_DOWNLOADED))
			{
				notifyAdapter();
			}
			else
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				final String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final DownloadType type = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
				final StickerCategory category = stickerCategoriesMap.get(categoryId);
				final boolean failedDueToLargeFile = b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
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
							if (failedDueToLargeFile)
							{
								Toast.makeText(getActivity(), R.string.out_of_space, Toast.LENGTH_SHORT).show();
							}
							category.setState(StickerCategory.RETRY);
							notifyAdapter();
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
							notifyAdapter();
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

	public void registerListener()
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

}