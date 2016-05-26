package com.bsb.hike.adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.StickerPageObject;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
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
import com.bsb.hike.view.StickerIconPageIndicator.StickerIconPagerAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StickerAdapter extends PagerAdapter implements StickerIconPagerAdapter, HikePubSub.Listener
{
    private static final int REFRESH_ADAPTER = 1;

    private final String TAG = StickerAdapter.class.getSimpleName();

    private List<StickerCategory> stickerCategoryList;

	private LayoutInflater inflater;

	private Context mContext;

	private Map<String, StickerPageObject> stickerObjMap;
	
	private StickerLoader worker;
	
	private StickerOtherIconLoader stickerOtherIconLoader;

	private StickerLoader miniStickerLoader;
	
	private StickerPickerListener mStickerPickerListener;

	private String[] pubSubListeners = { HikePubSub.STICKER_DOWNLOADED };

	private int NUM_COLUMNS;

	private boolean shown ;

	public StickerAdapter(Context context, StickerPickerListener listener)
	{
		this.inflater = LayoutInflater.from(context);
		this.mContext = context;
		this.mStickerPickerListener = listener;
		instantiateStickerList();
		stickerObjMap = Collections.synchronizedMap(new HashMap<String, StickerPageObject>());
		NUM_COLUMNS = StickerManager.getInstance().getNumColumnsForStickerGrid(mContext);

		//only loading full stickers or downloading the full version if not yet downloaded
		worker = new StickerLoader.Builder()
                .downloadLargeStickerIfNotFound(true)
                .setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.shop_placeholder))
                .build();

		miniStickerLoader = new StickerLoader.Builder()
				.downloadMiniStickerIfNotFound(true)
				.loadMiniStickerIfNotFound(true)
				.setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.shop_placeholder))
				.build();

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
		container.removeView((View) object);
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
		View stickerPage = inflater.inflate(R.layout.sticker_page, null);
		StickerCategory category = stickerCategoryList.get(position);
		Logger.d(getClass().getSimpleName(), "Instantiate View for category : " + category.getCategoryId());

		setupStickerPage(stickerPage, category);

		container.addView(stickerPage);
		stickerPage.setTag(category.getCategoryId());

		return stickerPage;
	}

	private StickerPageObject createStickerPageObject(View parent, StickerCategory stickerCategory)
	{
		ViewGroup containerView = (ViewGroup) parent.findViewById(R.id.container);
		final GridView stickerGridView = (GridView) parent.findViewById(R.id.emoticon_grid);
		stickerGridView.setNumColumns(NUM_COLUMNS);

		StickerPageObject stickerPageObject = new StickerPageObject.Builder()
				.setParentView(parent)
				.setStickerGridView(stickerGridView)
				.setContainerView(containerView)
				.setStickerCategory(stickerCategory)
				.build();
		stickerObjMap.put(stickerCategory.getCategoryId(), stickerPageObject);

		return stickerPageObject;
	}

	public void setupStickerPage(final View parent, final StickerCategory category)
	{
		StickerPageObject stickerPageObject = createStickerPageObject(parent, category);

		if(StickerManager.getInstance().isQuickSuggestionCategory(category.getCategoryId()))
		{
			if(!shown && QuickStickerSuggestionController.getInstance().shouldShowFtuePage())
			{
				if(Utils.isEmpty(category.getStickerList()))
				{
					setUpEmptyPage(stickerPageObject);
				}
				else
				{
					setQsFtuePage(stickerPageObject);
				}
			}
			else
			{
				setUpNormalPage(stickerPageObject);
			}
		}
		else
		{
			setUpNormalPage(stickerPageObject);
		}
	}

	public void setUpNormalPage(StickerPageObject stickerPageObject)
	{
		stickerPageObject.getStickerGridView().setVisibility(View.VISIBLE);

		StickerCategory category = stickerPageObject.getStickerCategory();
		initStickers(stickerPageObject);

		/**
		 * Conditionally setting up the emptyView
		 */
		if(category.getDownloadedStickersCount() < 1)
		{
			stickerPageObject.getStickerGridView().setEmptyView(inflateEmptyView(stickerPageObject));
		}
	}

	public void setUpEmptyPage(StickerPageObject stickerPageObject)
	{
		inflateEmptyView(stickerPageObject);
		stickerPageObject.getStickerGridView().setVisibility(View.GONE);
		stickerPageObject.getContainerView().setVisibility(View.VISIBLE);
	}

	public void setQsErrorPage(StickerPageObject stickerPageObject)
	{
		inflateQuickSuggestionErrorView(stickerPageObject);
		stickerPageObject.getStickerGridView().setVisibility(View.GONE);
		stickerPageObject.getContainerView().setVisibility(View.VISIBLE);
	}

	public void setQsFtuePage(StickerPageObject stickerPageObject)
	{
		inflateQuickSuggestionFtueView(stickerPageObject);
		stickerPageObject.getContainerView().setVisibility(View.VISIBLE);
	}

	private View inflateEmptyView(StickerPageObject stickerPageObject)
	{
		ViewGroup container = stickerPageObject.getContainerView();
		StickerCategory category = stickerPageObject.getStickerCategory();

		if(StickerManager.getInstance().isRecentCategory(category.getCategoryId()))
		{
			return inflateRecentEmptyView(container);
		}
		else if(StickerManager.getInstance().isQuickSuggestionCategory(category.getCategoryId()))
		{
			return inflateQuickSuggestionEmptyViewView(container);
		}
		else
		{
			return inflateNormalEmptyView(container, category);
		}
	}

	private View inflateRecentEmptyView(ViewGroup container)
	{
		return LayoutInflater.from(mContext).inflate(R.layout.recent_empty_view, container);
	}

	private View inflateQuickSuggestionEmptyViewView(ViewGroup container)
	{
		return LayoutInflater.from(mContext).inflate(R.layout.quick_suggestions_empty_view, container);
	}

	public View inflateNormalEmptyView(ViewGroup container, final StickerCategory category)
	{
		View empty = LayoutInflater.from(mContext).inflate(R.layout.sticker_pack_empty_view, container);
		CustomFontButton downloadBtn = (CustomFontButton) empty.findViewById(R.id.download_btn);
		TextView categoryName = (TextView) empty.findViewById(R.id.category_name);
		TextView category_details = (TextView) empty.findViewById(R.id.category_details);
		ImageView previewImage = (ImageView) empty.findViewById(R.id.preview_image);
		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(category.getCategoryId(), StickerManager.PREVIEW_IMAGE_PACK_PREVIEW_PALETTE_TYPE), previewImage);
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
				StickerManager.getInstance().initialiseDownloadStickerPackTask(category, DownloadType.NEW_CATEGORY, StickerManager.getInstance().getPackDownloadBodyJson(DownloadSource.FIRST_TIME));
				setupStickerPage(stickerObjMap.get(category.getCategoryId()).getParentView(), category);
			}
		});

		return empty;
	}

	public View inflateQuickSuggestionErrorView(final StickerPageObject stickerPageObject)
	{
		stickerPageObject.getContainerView().removeAllViews();
		View errorView = LayoutInflater.from(mContext).inflate(R.layout.quick_suggestion_error_view, stickerPageObject.getContainerView());
		errorView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				setupStickerPage(stickerPageObject.getParentView(), stickerPageObject.getStickerCategory());
				QuickStickerSuggestionController.getInstance().loadQuickStickerSuggestions((QuickSuggestionStickerCategory) stickerPageObject.getStickerCategory());
			}
		});

		return errorView;
	}

	public View inflateQuickSuggestionFtueView(final StickerPageObject stickerPageObject)
	{
		stickerPageObject.getContainerView().removeAllViews();

		View ftueView = LayoutInflater.from(mContext).inflate(R.layout.quick_suggestion_ftue_page, stickerPageObject.getContainerView());

		StickerCategory category = stickerPageObject.getStickerCategory();
		final GridView stickerGridView = (GridView) ftueView.findViewById(R.id.sticker_grid);
		stickerGridView.setNumColumns(NUM_COLUMNS);
		stickerPageObject.setStickerGridView(stickerGridView);

		List<Sticker> stickersList = category.getStickerList();
		int numStickers = stickersList.size() >= NUM_COLUMNS ? NUM_COLUMNS : stickersList.size();
		stickersList = stickersList.subList(0, numStickers);

		final List<StickerPageAdapterItem> stickerPageList = StickerManager.getInstance().generateStickerPageAdapterItemList(stickersList);
		setStickerPageAdapter(stickerPageObject, stickerPageList);
		stickerPageObject.getStickerPageAdapter().setQsFtueCatgeory(true);

		return ftueView;
	}

	private void setStickerPageAdapter(StickerPageObject stickerPageObject, List<StickerPageAdapterItem> stickerPageList)
	{
		StickerCategory stickerCategory = stickerPageObject.getStickerCategory();

		/**
		 * If StickerPageAdapter is already initialised, we clear the prev list and add new items
		 */
		if(stickerPageObject.getStickerPageAdapter() != null)
		{
			StickerPageAdapter stickerPageAdapter = stickerPageObject.getStickerPageAdapter();
			stickerPageAdapter.getStickerPageAdapterItemList().clear();
			stickerPageAdapter.getStickerPageAdapterItemList().addAll(stickerPageList);
			stickerPageAdapter.notifyDataSetChanged();
		}
		else
		{
			StickerLoader stickerLoader = StickerManager.getInstance().isQuickSuggestionCategory(stickerCategory.getCategoryId()) ? miniStickerLoader : worker;
			final StickerPageAdapter stickerPageAdapter = new StickerPageAdapter(mContext, stickerPageList, stickerCategory, stickerLoader, stickerPageObject.getStickerGridView(), mStickerPickerListener);
			stickerPageObject.setStickerPageAdapter(stickerPageAdapter);
			stickerPageObject.getStickerGridView().setAdapter(stickerPageAdapter);
		}
	}

	private void addViewBasedOnState(StickerPageObject stickerPageObjects, StickerCategory category)
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
		StickerPageObject spo = stickerObjMap.get(category.getCategoryId());
		if(spo == null)
		{
			return;
		}

		initStickers(spo);
	}

	private void initStickers(StickerPageObject spo)
	{

		StickerCategory category = spo.getStickerCategory();
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
			int totalPlaceHolders = 2 * NUM_COLUMNS - 1;
			while(totalPlaceHolders > 0)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.PLACE_HOLDER));
				totalPlaceHolders --;
			}
		}

		setStickerPageAdapter(spo, stickerPageList);
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

	public void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.RECENTS_UPDATED);
		filter.addAction(StickerManager.STICKERS_PROGRESS);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_SUCCESS);
		filter.addAction(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_FAILED);
		filter.addAction(StickerManager.QUICK_STICKER_SUGGESTION_FTUE_STICKER_CLICKED);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, filter);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.RECENTS_UPDATED))
			{
				Sticker st = (Sticker) intent.getSerializableExtra(StickerManager.RECENT_STICKER_SENT);
				refreshRecents(st);
			}
			else if(intent.getAction().equals(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_FAILED) || intent.getAction().equals(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_SUCCESS))
			{
				Logger.d(TAG, "fetched quick suggestion intent received ");

				String action = intent.getAction();

				Bundle bundle = intent.getBundleExtra(HikeConstants.BUNDLE);
				StickerCategory category = QuickSuggestionStickerCategory.fromBundle(bundle);

				if(category == null)
				{
					Logger.wtf(TAG, "null category received");
					return;
				}

				StickerPageObject stickerPageObject = stickerObjMap.get(category.getCategoryId());

				if(stickerPageObject == null)
				{
					Logger.wtf(TAG, "no qs category exist");
					return;
				}

				Logger.d(TAG, "fetch failed for quick suggestion category ");

				if(category.equals(stickerCategoryList.get(0)))
				{
					if(action.equals(StickerManager.QUICK_STICKER_SUGGESTION_FETCH_SUCCESS))
					{
						updateQuickSuggestionCategoryInList((QuickSuggestionStickerCategory) category);
						setupStickerPage(stickerPageObject.getParentView(), stickerPageObject.getStickerCategory());
					}
					else
					{
						if (Utils.isEmpty(((QuickSuggestionStickerCategory) stickerCategoryList.get(0)).getStickerSet()))
						{
							setQsErrorPage(stickerPageObject);
						}
					}
				}
			}
			else if(intent.getAction().equals(StickerManager.QUICK_STICKER_SUGGESTION_FTUE_STICKER_CLICKED))
			{
				StickerPageObject stickerPageObject = stickerObjMap.get(StickerManager.QUICK_SUGGESTIONS);
				if (stickerPageObject == null)
				{
					return;
				}
				shown = true;
				setupStickerPage(stickerPageObject.getParentView(), stickerPageObject.getStickerCategory());
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
				final StickerPageObject spo = stickerObjMap.get(cat.getCategoryId());
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
						initStickers(spo);
					}
				}
			}
		}
	};

	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        HikeMessengerApp.getPubSub().removeListeners(this,pubSubListeners );
	}

	public StickerLoader getStickerLoader()
	{
		return worker;
	}
	
	public StickerOtherIconLoader getStickerOtherIconLoader()
	{
		return stickerOtherIconLoader;
	}

	public StickerLoader getMiniStickerLoader()
	{
		return miniStickerLoader;
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

	@Override
	public void onEventReceived(String type, Object object)
	{
        switch (type)
        {
            case HikePubSub.STICKER_DOWNLOADED:
                sendUIMessage(REFRESH_ADAPTER, object);
                break;
        }
	}

    protected Handler uiHandler = new Handler(Looper.getMainLooper())
    {
        public void handleMessage(android.os.Message msg)
        {
            /**
             * Defensive check
             */
            if (msg == null)
            {
                Logger.e(TAG, "Getting a null message in Sticker Adapter");
                return;
            }
            handleUIMessage(msg);
        }

    };

    protected void handleUIMessage(android.os.Message msg)
    {
        switch (msg.what)
        {
            case REFRESH_ADAPTER:
                Sticker sticker = (Sticker) msg.obj;
                if (sticker.getCategory() != null)
                {
                    initStickers(sticker.getCategory());
                }

                refreshRecents(null);
				refreshQuickSuggestionCategory();

                break;
            default:
                Logger.d(TAG, "Did not find any matching event for msg.what : " + msg.what);
                break;
        }
    }

    protected void sendUIMessage(int what, Object data)
    {
        Message message = Message.obtain();
        message.what = what;
        message.obj = data;
        uiHandler.sendMessage(message);
    }


    private void refreshRecents(Sticker sticker)
    {
		StickerPageObject spo = stickerObjMap.get(StickerManager.RECENT);
        if (spo != null)
        {
            final StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
            if (stickerPageAdapter != null)
            {
                if (sticker != null)
                {
                    stickerPageAdapter.updateRecentsList(sticker);
                }
                stickerPageAdapter.notifyDataSetChanged();
            }
        }
    }

	private void refreshQuickSuggestionCategory()
	{
		StickerPageObject spo = stickerObjMap.get(StickerManager.QUICK_SUGGESTIONS);
		if (spo != null)
		{
			final StickerPageAdapter stickerPageAdapter = spo.getStickerPageAdapter();
			if (stickerPageAdapter != null)
			{
				stickerPageAdapter.notifyDataSetChanged();
			}
		}
	}

	private void updateQuickSuggestionCategoryInList(QuickSuggestionStickerCategory newCategory)
	{
		QuickSuggestionStickerCategory presentCategory = (QuickSuggestionStickerCategory) stickerCategoryList.get(0);
		presentCategory.setSentStickers(newCategory.getSentStickers());
		presentCategory.setReplyStickers(newCategory.getReplyStickers());
		presentCategory.setLastRefreshTime(newCategory.getLastRefreshTime());
	}

	public void addQuickSuggestionCategory(StickerCategory quickSuggestionCategory)
	{
		removeQuickSuggestionCategory();
		stickerCategoryList.add(0, quickSuggestionCategory);
	}

	public boolean removeQuickSuggestionCategory()
	{
		if(getCount() > 0 && StickerManager.getInstance().isQuickSuggestionCategory(stickerCategoryList.get(0).getCategoryId()))
		{
			stickerCategoryList.remove(0);
			return true;
		}
		else
		{
			return false;
		}
	}
}
