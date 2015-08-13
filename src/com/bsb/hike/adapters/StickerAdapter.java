package com.bsb.hike.adapters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontButton;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator.StickerEmoticonIconPagerAdapter;

public class StickerAdapter extends PagerAdapter implements StickerEmoticonIconPagerAdapter
{
	private List<StickerCategory> stickerCategoryList;

	private LayoutInflater inflater;

	private Context mContext;

	private Map<String, StickerPageObjects> stickerObjMap;
	
	private StickerLoader worker;
	
	private StickerOtherIconLoader stickerOtherIconLoader;
	
	private StickerPickerListener mStickerPickerListener;

	private class StickerPageObjects
	{
		private GridView stickerGridView;

		private StickerPageAdapter spa;

		public StickerPageObjects(GridView sgv)
		{
			stickerGridView = sgv;
		}

		public GridView getStickerGridView()
		{
			return stickerGridView;
		}

		public void setStickerPageAdapter(StickerPageAdapter sp)
		{
			this.spa = sp;
		}

		public StickerPageAdapter getStickerPageAdapter()
		{
			return spa;
		}
	}

	public StickerAdapter(Context context, StickerPickerListener listener)
	{
		this.inflater = LayoutInflater.from(context);
		this.mContext = context;
		this.mStickerPickerListener = listener;
		instantiateStickerList();
		stickerObjMap = Collections.synchronizedMap(new HashMap<String, StickerAdapter.StickerPageObjects>());
		worker = new StickerLoader(mContext, true);
		stickerOtherIconLoader = new StickerOtherIconLoader(mContext, true);
		registerListener();
		Logger.d(getClass().getSimpleName(), "Sticker Adapter instantiated ....");
	}

	/**
	 * Utility method for updating the sticker list
	 */
	public void instantiateStickerList()
	{
		this.stickerCategoryList = StickerManager.getInstance().getStickerCategoryList();
	}

	@Override
	public int getCount()
	{
		return stickerCategoryList.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		Logger.d(getClass().getSimpleName(), "Item removed from position : " + position);
		((ViewPager) container).removeView((View) object);
		if(stickerCategoryList.size() <= position)  //We were getting an ArrayIndexOutOfBounds Exception here
		{
			return;
		}
		
		StickerCategory cat = stickerCategoryList.get(position);
		stickerObjMap.remove(cat.getCategoryId());
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View emoticonPage;
		emoticonPage = inflater.inflate(R.layout.sticker_page, null);
		StickerCategory category = stickerCategoryList.get(position);
		Logger.d(getClass().getSimpleName(), "Instantiate View for categpory : " + category.getCategoryId());
		setupStickerPage(emoticonPage, category);

		((ViewPager) container).addView(emoticonPage);
		emoticonPage.setTag(category.getCategoryId());

		return emoticonPage;
	}

	public void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.RECENTS_UPDATED);
		filter.addAction(StickerManager.STICKERS_PROGRESS);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, filter);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.RECENTS_UPDATED))
			{
				StickerPageObjects spo = stickerObjMap.get(StickerManager.RECENT);
				if (spo != null)
				{
					final StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
					if (stickerPageAdapter != null)
					{
						Sticker st = (Sticker) intent.getSerializableExtra(StickerManager.RECENT_STICKER_SENT);
						if (st != null)
						{
							stickerPageAdapter.updateRecentsList(st);
							stickerPageAdapter.notifyDataSetChanged();
						}
					}
				}
			}
			/**
			 * More stickers downloaded case
			 */
			else if(intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED) || intent.getAction().equals(StickerManager.STICKERS_UPDATED))
			{
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				if(category == null)
				{
					return;
				}
				
				initStickers(category);
			}
			else if(intent.getAction().equals(StickerManager.STICKERS_PROGRESS))
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				if(category == null)
				{
					return;
				}
				initStickers(category);
			}
			
			else
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				final String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final DownloadType type = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
				final StickerCategory cat = StickerManager.getInstance().getCategoryForId(categoryId);
				if(cat == null)
				{
					return;
				}
				final StickerPageObjects spo = stickerObjMap.get(cat.getCategoryId());
				final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
				// if this category is already loaded then only proceed else ignore
				if (spo != null)
				{
					if (intent.getAction().equals(StickerManager.STICKERS_FAILED) && (DownloadType.NEW_CATEGORY.equals(type) || DownloadType.MORE_STICKERS.equals(type)))
					{
								if(failedDueToLargeFile)
								{
									Toast.makeText(mContext, R.string.out_of_space, Toast.LENGTH_SHORT).show();
								}
								Logger.d(getClass().getSimpleName(), "Download failed for new category " + cat.getCategoryId());
								cat.setState(StickerCategory.RETRY);
								addViewBasedOnState(stickerObjMap.get(cat.getCategoryId()), cat);
							
					}
					else if (intent.getAction().equals(StickerManager.STICKERS_DOWNLOADED) && DownloadType.NEW_CATEGORY.equals(type))
					{
								initStickers(spo, cat);
					}
				}
			}
		}
	};

	public void setupStickerPage(final View parent, final StickerCategory category)
	{
		final GridView stickerGridView = (GridView) parent.findViewById(R.id.emoticon_grid);

		ViewGroup emptyView = (ViewGroup) parent.findViewById(R.id.emptyViewHolder);

		StickerPageObjects spo = new StickerPageObjects(stickerGridView);
		stickerGridView.setNumColumns(StickerManager.getInstance().getNumColumnsForStickerGrid(mContext));
		stickerObjMap.put(category.getCategoryId(), spo);
		initStickers(spo, category);
		
		/**
		 * Conditionally setting up the emptyView
		 */
		if(category.getDownloadedStickersCount() < 1)
		{
			checkAndSetEmptyView(parent, emptyView, category, stickerGridView);
		}
		
	}
	
	private void checkAndSetEmptyView(final View parent, ViewGroup emptyView, final StickerCategory category, final GridView stickerGridView)
	{
		View empty;
		if (category.isCustom())
		{
			// Set Recents EmptyView
			empty = LayoutInflater.from(mContext).inflate(R.layout.recent_empty_view, emptyView);
		}

		else
		{
			// Set Download EmptyView
			empty = LayoutInflater.from(mContext).inflate(R.layout.sticker_pack_empty_view, emptyView);
			CustomFontButton downloadBtn = (CustomFontButton) empty.findViewById(R.id.download_btn);
			TextView categoryName = (TextView) empty.findViewById(R.id.category_name);
			TextView category_details = (TextView) empty.findViewById(R.id.category_details);
			ImageView previewImage = (ImageView) empty.findViewById(R.id.preview_image);
			stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(category.getCategoryId(), StickerManager.PREVIEW_IMAGE_TYPE), previewImage);
			stickerOtherIconLoader.setImageSize(mContext.getResources().getDimensionPixelSize(R.dimen.sticker_empty_pallete_preview_image_width), mContext.getResources().getDimensionPixelSize(R.dimen.sticker_empty_pallete_preview_image_height));
			TextView separator = (TextView) empty.findViewById(R.id.separator);
			if(category.getTotalStickers() > 0)
			{
				category_details.setVisibility(View.VISIBLE);
				String detailsString = mContext.getString(R.string.n_stickers, category.getTotalStickers());
				if(category.getCategorySize() > 0)
				{
					detailsString += ", " + Utils.getSizeForDisplay(category.getCategorySize());
				}
				category_details.setText(detailsString);
				if(Utils.getDeviceOrientation(mContext) == Configuration.ORIENTATION_LANDSCAPE)
				{
					separator.setVisibility(View.VISIBLE);
				}
			}
			else
			{
				category_details.setVisibility(View.GONE);
				if(Utils.getDeviceOrientation(mContext) == Configuration.ORIENTATION_LANDSCAPE)
				{
					separator.setVisibility(View.GONE);
				}
			}
			categoryName.setText(category.getCategoryName());
			downloadBtn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					/**
					 * This is done to remove the green dot for update available state. For a new category added on the fly from the server, the update available is set to true to
					 * show a green indicator. To remove that, we are doing this.
					 */
					if(category.isUpdateAvailable())
					{
						category.setUpdateAvailable(false);
					}
					StickerManager.getInstance().initialiseDownloadStickerTask(category, DownloadSource.FIRST_TIME, DownloadType.NEW_CATEGORY, mContext);
					setupStickerPage(parent, category);
				}
			});

		}

		stickerGridView.setEmptyView(empty);
	}

	private void addViewBasedOnState(StickerPageObjects stickerPageObjects, StickerCategory category)
	{
		StickerPageAdapter spa = stickerPageObjects.getStickerPageAdapter();
		List<StickerPageAdapterItem> stickerPageList = spa.getStickerPageAdapterItemList();
		stickerPageList.remove(0);
		/* We add UI elements based on the current state of the sticker category*/
		addStickerPageAdapterItem(category, stickerPageList);
		spa.notifyDataSetChanged();
	}
	
	/**
	 * Adds StickerPageAdapter Items to the list passed based on the state of the category
	 * @param state
	 * @param stickerPageList
	 */
	private void addStickerPageAdapterItem(StickerCategory category, List<StickerPageAdapterItem> stickerPageList)
	{
		switch (category.getState()) 
		{
		case StickerCategory.UPDATE :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.UPDATE, category.getMoreStickerCount()));
			break;
			
		case StickerCategory.DOWNLOADING :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.DOWNLOADING));
			break;
			
		case StickerCategory.RETRY :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.RETRY));
			break;
			
		case StickerCategory.DONE :
			stickerPageList.add(0, new StickerPageAdapterItem(StickerPageAdapterItem.DONE));
			break;
		}
	}

	public void initStickers(StickerCategory category)
	{
		StickerPageObjects spo = stickerObjMap.get(category.getCategoryId());
		if(spo == null)
		{
			return;
		}
		
		initStickers(spo, category);
	}
	
	private void initStickers(StickerPageObjects spo, final StickerCategory category)
	{

		spo.getStickerGridView().setVisibility(View.VISIBLE);
		final List<Sticker> stickersList = category.getStickerList();
		
		final List<StickerPageAdapterItem> stickerPageList = StickerManager.getInstance().generateStickerPageAdapterItemList(stickersList);
		
		/**
		 * Added logic to add update state of category if stickers were deleted from the folder
		 */
		if((category.getState() == StickerCategory.NONE) && stickersList.size() > 0 && (stickersList.size() < category.getTotalStickers()))
		{
			category.setState(StickerCategory.UPDATE);
		}
		
		int state = category.getState();

		/* We add UI elements based on the current state of the sticker category*/
		addStickerPageAdapterItem(category, stickerPageList);
		/**
		 * Adding the placeholders in 0 sticker case in pallete. The placeholders will be added when state is either downloading or retry.
		 */
		if(stickersList.size() == 0 && (state == StickerCategory.DOWNLOADING || state == StickerCategory.RETRY))
		{
			int totalPlaceHolders = 2 * StickerManager.getInstance().getNumColumnsForStickerGrid(mContext) - 1;
			while(totalPlaceHolders > 0)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.PLACE_HOLDER));
				totalPlaceHolders --;
			}
		}
		/**
		 * If StickerPageAdapter is already initialised, we clear the prev list and add new items
		 */
		if(spo.getStickerPageAdapter() != null)
		{
			StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
			stickerPageAdapter.getStickerPageAdapterItemList().clear();
			stickerPageAdapter.getStickerPageAdapterItemList().addAll(stickerPageList);
			stickerPageAdapter.notifyDataSetChanged();
		}
		else
		{
			final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(mContext, stickerPageList, category, worker, spo.getStickerGridView(), mStickerPickerListener);
			spo.setStickerPageAdapter(stickerPageAdapter);
			spo.getStickerGridView().setAdapter(stickerPageAdapter);
		}
	}
	
	@Override
	public int getIconResId(int index)
	{
		// TODO need to remove this hardcoded drawable usage. we should actually use pallate icons
		// saved in folders of each category.
		return R.drawable.recents;//stickerCategoryList.get(index).categoryId.resId();
	}

	@Override
	public boolean isUpdateAvailable(int index)
	{
		return stickerCategoryList.get(index).isUpdateAvailable();
	}

	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
	}
	
	public StickerLoader getStickerLoader()
	{
		return worker;
	}
	
	public StickerOtherIconLoader getStickerOtherIconLoader()
	{
		return stickerOtherIconLoader;
	}

	/**
	 * Returns Sticker Category object based on index
	 * @param position
	 * @return {@link StickerCategory} Object
	 */
	@Override
	public StickerCategory getCategoryForIndex(int index)
	{
		return stickerCategoryList.get(index);
	}

	@Override
	public String getIconContentDescription(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
