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
import android.view.animation.Animation;
import android.widget.ImageView;
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
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public abstract class StickerShopBaseFragment extends Fragment implements Listener
{
    protected String[] pubSubListeners = {HikePubSub.STICKER_CATEGORY_MAP_UPDATED, HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE};

    protected StickerOtherIconLoader stickerOtherIconLoader;

    protected ListView listview;

    protected int previousFirstVisibleItem;

    protected int velocity;

    protected long previousEventTime;
	
	Map<String, StickerCategory> stickerCategoriesMap;

    protected final int NOT_DOWNLOADING = 0;

    protected final int DOWNLOADING = 1;

    protected final int DOWNLOAD_FAILED = 2;

    protected int downloadState = NOT_DOWNLOADING;
	
	View loadingFooterView, downloadFailedFooterView, loadingEmptyState, loadingFailedEmptyState;
	
	TextView loadingFailedEmptyStateMainText, loadingFailedEmptyStateSubText;

    protected int currentCategoriesCount;

	private static final String TAG = StickerShopBaseFragment.class.getSimpleName();

    protected View headerView;

    public static class StickerShopViewHolder
    {
        public TextView categoryName;

        public TextView totalStickers;

        public TextView stickersPackDetails;

        public TextView categoryPrice;

        public ImageView downloadState;

        public ImageView categoryPreviewIcon;

        private boolean shownPackPreviewFtue;

        public StickerShopViewHolder(View v)
        {
            this.categoryName = (TextView) v.findViewById(R.id.category_name);
            this.stickersPackDetails = (TextView) v.findViewById(R.id.pack_details);
            this.downloadState = (ImageView) v.findViewById(R.id.category_download_btn);
            this.categoryPreviewIcon = (ImageView) v.findViewById(R.id.category_icon);
            this.categoryPrice = (TextView) v.findViewById(R.id.category_price);
            this.shownPackPreviewFtue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, false);
            v.setTag(this);
        }

        public void loadViewFromCategory(Context context,StickerCategory category)
        {

            this.categoryName.setText(category.getCategoryName());
            int totalStickerCount = category.getTotalStickers();
            int categorySizeInBytes = category.getCategorySize();
            if (totalStickerCount > 0)
            {
                String detailsStirng = totalStickerCount == 1 ? context.getResources().getString(R.string.singular_stickers, totalStickerCount)  : context.getResources().getString(R.string.n_stickers, totalStickerCount);
                if (categorySizeInBytes > 0)
                {
                    detailsStirng += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
                }
                this.stickersPackDetails.setVisibility(View.VISIBLE);
                this.stickersPackDetails.setText(detailsStirng);
            }
            else
            {
                this.stickersPackDetails.setVisibility(View.GONE);
            }

            if(category.isVisible())
            {
                switch (category.getState())
                {
                    case StickerCategory.NONE:
                    case StickerCategory.DONE_SHOP_SETTINGS:
                    case StickerCategory.DONE:
                        if (category.getDownloadedStickersCount() == 0)
                        {
                            this.categoryPrice.setVisibility(View.VISIBLE);
                            this.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
                            this.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
                        }
                        else
                        {
                            this.categoryPrice.setText(context.getResources().getString(R.string.downloaded).toUpperCase());
                            this.categoryPrice.setTextColor(context.getResources().getColor(R.color.blue_hike));
                        }
                        break;
                    case StickerCategory.UPDATE:
                        this.categoryPrice.setVisibility(View.VISIBLE);
                        this.categoryPrice.setText(context.getResources().getString(R.string.update_sticker));
                        this.categoryPrice.setTextColor(context.getResources().getColor(R.color.sticker_settings_update_color));
                        break;
                    case StickerCategory.RETRY:
                        this.categoryPrice.setVisibility(View.VISIBLE);
                        this.categoryPrice.setText(context.getResources().getString(R.string.RETRY));
                        this.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
                        break;
                    case StickerCategory.DOWNLOADING:
                        this.categoryPrice.setVisibility(View.VISIBLE);
                        this.categoryPrice.setText(context.getResources().getString(R.string.downloading_stk));
                        this.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));

                        break;
                }
            }
            else
            {
                this.categoryPrice.setVisibility(View.VISIBLE);
                this.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
                this.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
            }
            this.downloadState.setTag(category);
            this.downloadState.setVisibility(View.VISIBLE);
        }

        public void showPackPreviewFtue(Animation packPreviewFtueAnimation,int position)
        {
            if(!shownPackPreviewFtue)
            {
                Animation animation = this.downloadState.getAnimation();
                if(animation != null)
                {
                    animation.cancel();
                }

                if(position == 0)
                {
                    this.downloadState.startAnimation(packPreviewFtueAnimation);
                }
                else
                {
                    this.downloadState.setAnimation(null);
                }
            }
        }

    }

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
        this.stickerOtherIconLoader = new StickerOtherIconLoader(getActivity(), true);
		doInitialSetup();
	}

	protected abstract void doInitialSetup();

    protected abstract void notifyAdapter();

    protected abstract void reloadAdapter();

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
		if (stickerOtherIconLoader != null)
		{
            stickerOtherIconLoader.setExitTasksEarly(true);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (stickerOtherIconLoader != null)
		{
            stickerOtherIconLoader.setExitTasksEarly(false);
		}
        notifyAdapter();
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
					updateStickerCategoriesMap(StickerManager.getInstance().getStickerCategoryMap());
					notifyAdapter();
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
					listview.setVisibility(View.VISIBLE);
					listview.removeFooterView(loadingFooterView);
					loadingEmptyState.setVisibility(View.GONE);
					loadingFailedEmptyState.setVisibility(View.GONE);
                    reloadAdapter();
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
			else if(intent.getAction().equals(StickerManager.STICKER_PREVIEW_DOWNLOADED))
			{
				notifyAdapter();
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
							if(failedDueToLargeFile)
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