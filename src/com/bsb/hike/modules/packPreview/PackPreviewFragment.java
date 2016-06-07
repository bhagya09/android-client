package com.bsb.hike.modules.packPreview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.media.StickerPreviewContainer;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.tasks.FetchCategoryDetailsTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhavgupta on 04/01/16.
 */
public class PackPreviewFragment extends Fragment implements HikePubSub.Listener, PackPreviewFragmentScrollListener.OnVerticalScrollListener,
		PackPreviewRecyclerView.TouchListener, View.OnClickListener, OnLongClickListener, HikePubSub.UiListener
{

	private static final String TAG = PackPreviewFragment.class.getSimpleName();

	private String[] pubSubListeners = { HikePubSub.STICKER_DOWNLOADED };

	private String[] pubSubUiListeners = { HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_SUCCESS, HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_FAILURE };

	private StickerOtherIconLoader stickerOtherIconLoader;

	private String catId;

	private StickerCategory stickerCategory;

	private View loadingView, loadingFailed, headerDivider, categoryDetailsContainer;

	private RecyclerView rvGrid;

	private GridLayoutManager layoutManager;

	private PackPreviewAdapter mAdapter;

	private ImageView categoryIcon;

	private CustomFontButton downloadBtn;

	private TextView categoryName, categoryDetails, categoryDescription;

	private StickerPreviewContainer stickerPreviewContainer;

	private View headerContainer, viewAllButton;

	public static int NUM_COLUMNS;

	public static int NUM_INITIALLY_VISIBLE_STICKERS;

	private int categoryDetailsContainerMaxHeight, categoryIconMaxWidth, categoryIconMaxHeight, categoryDescriptionMaxHeight, topMarginForCenterVertical;

	private int downloadButtonMaxWidth = Integer.MAX_VALUE;

	private int categoryPosition;

	public PackPreviewFragment()
	{
		stickerOtherIconLoader = new StickerOtherIconLoader(HikeMessengerApp.getInstance(), true);
		NUM_COLUMNS = StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance());
		NUM_INITIALLY_VISIBLE_STICKERS = NUM_COLUMNS * HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NUM_ROWS_INITIALLY_VISIBLE, StickerConstants.DEFAULT_PACK_PREVIEW_VIEW_ALL_VISIBLE_ROWS);

	}

	public static PackPreviewFragment newInstance(String catId, int position)
	{
		PackPreviewFragment fragment = new PackPreviewFragment();
		Bundle args = new Bundle();
		args.putString(HikeConstants.STICKER_CATEGORY_ID, catId);
		args.putInt(HikeConstants.POSITION, position);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		catId = args.getString(HikeConstants.STICKER_CATEGORY_ID);
		categoryPosition = args.getInt(HikeConstants.POSITION);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.pack_preview, container, false);
		initView(parent);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		executeFetchCategoryDetailsTask(new FetchCategoryDetailsTask(catId));
		registerListener();
	}

	@Override
	public void onDestroy()
	{
		releaseResources();
		super.onDestroy();
	}

	public void releaseResources()
	{
		deRegisterListeners();

		if(mAdapter != null)
		{
			mAdapter.releaseResources();
		}
		if(stickerPreviewContainer != null)
		{
			stickerPreviewContainer.releaseResources();
		}

		mAdapter = null;
		stickerPreviewContainer = null;
	}

	private void executeFetchCategoryDetailsTask(FetchCategoryDetailsTask fetchCategoryDetailsTask)
	{
		loadingView.setVisibility(View.VISIBLE);
		fetchCategoryDetailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void initView(View parent)
	{
		loadingView = parent.findViewById(R.id.loading);
		loadingFailed = parent.findViewById(R.id.loading_failed);
		loadingFailed.setOnClickListener(this);
		categoryIcon = (ImageView) parent.findViewById(R.id.category_icon);
		downloadBtn = (CustomFontButton) parent.findViewById(R.id.download_btn);
		categoryName = (TextView) parent.findViewById(R.id.category_name);
		categoryDetails = (TextView) parent.findViewById(R.id.category_details);
		categoryDescription = (TextView) parent.findViewById(R.id.description);
		rvGrid = (RecyclerView) parent.findViewById(R.id.rvGrid);
		((PackPreviewRecyclerView) rvGrid).setTouchListener(this);
		headerDivider = parent.findViewById(R.id.header_divider);
		categoryDetailsContainer = parent.findViewById(R.id.category_detail_container);
		stickerPreviewContainer = (StickerPreviewContainer) parent.findViewById(R.id.sticker_preview_container);
		stickerPreviewContainer.initialise(rvGrid, this);
		viewAllButton = parent.findViewById(R.id.viewAllButton);
		viewAllButton.setOnClickListener(this);
	}

	private void registerListener()
	{
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		HikeMessengerApp.getPubSub().addUiListener(this, pubSubUiListeners);

		IntentFilter filter = new IntentFilter(StickerManager.STICKER_PREVIEW_DOWNLOADED);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).registerReceiver(mMessageReceiver, filter);
	}

	private void deRegisterListeners()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		HikeMessengerApp.getPubSub().removeUiListener(this, pubSubUiListeners);
		LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).unregisterReceiver(mMessageReceiver);
	}

	private void setDetails()
	{
		if(Utils.isEmpty(stickerCategory.getAllStickers()) || stickerCategory.getAllStickers().size() < NUM_INITIALLY_VISIBLE_STICKERS)
		{
			viewAllButton.setVisibility(View.GONE);
		}

		stickerOtherIconLoader.loadImage(
				StickerManager.getInstance().getCategoryOtherAssetLoaderKey(stickerCategory.getCategoryId(), StickerManager.PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE), categoryIcon);
		if (stickerCategory.getTotalStickers() > 0)
		{
			categoryDetails.setVisibility(View.VISIBLE);
			String detailsString = getResources().getString(R.string.n_stickers, stickerCategory.getTotalStickers());
			if (stickerCategory.getCategorySize() > 0)
			{
				detailsString += ", " + Utils.getSizeForDisplay(stickerCategory.getCategorySize());
			}
			categoryDetails.setText(detailsString);
		}
		else
		{
			categoryDetails.setVisibility(View.GONE);
		}

		if (TextUtils.isEmpty(stickerCategory.getDescription()))
		{
			categoryDescription.setVisibility(View.GONE);
		}
		else
		{
			categoryDescription.setText(stickerCategory.getDescription());
		}
		categoryName.setText(stickerCategory.getCategoryName());
		downloadBtn.setOnClickListener(this);

		layoutManager = new GridLayoutManager(getActivity(), NUM_COLUMNS, LinearLayoutManager.VERTICAL, false);
		List<Sticker> stickerList = stickerCategory.getAllStickers();
		stickerList = Utils.isEmpty(stickerList) || stickerList.size() < NUM_INITIALLY_VISIBLE_STICKERS ? stickerList : stickerList.subList(0, NUM_INITIALLY_VISIBLE_STICKERS);
		mAdapter = new PackPreviewAdapter(getActivity(), this, this);
		mAdapter.setLists(stickerList, getHeaderList(), getFooterList());
		rvGrid.setLayoutManager(layoutManager);
		rvGrid.setAdapter(mAdapter);

		layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
		{
			@Override
			public int getSpanSize(int position)
			{
				if(mAdapter != null)
				{
					return mAdapter.getSpanSize(position);
				}
				else
				{
					Logger.wtf(TAG, "adapater is null and get span size is called . should not happen ");
					return 1;
				}
			}
		});

		rvGrid.addOnScrollListener(new PackPreviewFragmentScrollListener(NUM_COLUMNS, this));

		setRvGridPadding();
		layoutCategoryIcon(0);
		calculateTopMarginForCenterVertical(0);
		layoutCategoryDetails();
		layoutCategoryDescription(0, PackPreviewFragmentScrollListener.SCROLL_DOWN);
		layoutDownloadButton(0);
		layoutHeaderDivider(0);

		categoryDetailsContainerMaxHeight = Math.max(categoryDetailsContainerMaxHeight, categoryDetailsContainer.getHeight());
		categoryDescriptionMaxHeight = Math.max(categoryDescriptionMaxHeight, categoryDescription.getHeight());
	}

	public List<Pair<Integer, BasePackPreviewAdapterItem>> getHeaderList()
	{
		List<Pair<Integer, BasePackPreviewAdapterItem>> headerList = new ArrayList<>(2);

		BasePackPreviewAdapterItem gridTopMarginHeaderItem = new GridTopMarginItem(getActivity());
		headerContainer = ((GridTopMarginItem) gridTopMarginHeaderItem).getHeaderContainer();

		headerList.add(new Pair<>(PackPreviewAdapter.VIEW_TYPE_GRID_TOP_MARGIN, gridTopMarginHeaderItem));

		BasePackPreviewAdapterItem tapTextHeaderItem = new TapTextHeaderItem(getActivity());
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_STICKER_PREVIEW, false))
		{
			headerList.add(new Pair<>(PackPreviewAdapter.VIEW_TYPE_TAP_TEXT_HEADER, tapTextHeaderItem));
		}

		return headerList;
	}

	public List<Pair<Integer, BasePackPreviewAdapterItem>> getFooterList()
	{
		List<Pair<Integer, BasePackPreviewAdapterItem>> footerList = new ArrayList<>(3);

		if(isViewAllClicked())
		{
			BasePackPreviewAdapterItem packAuthorFooterItem = new PackAuthorFooterItem(getActivity(), stickerCategory.getAuthor(), stickerCategory.getCopyRightString());
			footerList.add(new Pair<>(PackPreviewAdapter.VIEW_TYPE_AUTHOR_FOOTER, packAuthorFooterItem));

			if(!Utils.isEmpty(stickerCategory.getSimilarPacks()) && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_RECOMMENDED_PACKS, false))
			{
				BasePackPreviewAdapterItem recommendedPacksFooterItem = new RecommendedPacksFooterItem(getActivity(), getActivity(), stickerCategory);
				footerList.add(new Pair<>(PackPreviewAdapter.VIEW_TYPE_RECOMMENDED_PACKS_FOOTER, recommendedPacksFooterItem));
			}
		}

		return footerList;
	}

	@Override
	public void onTouch(MotionEvent event)
	{
		if(stickerPreviewContainer != null)
		{
			stickerPreviewContainer.dismiss();
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId()) {
		case R.id.viewAllButton:
			viewAllClicked();
			break;
		case R.id.loading_failed:
			loadingFailedClicked();
			break;
		case R.id.download_btn:
			downloadButtonClicked();
			break;
		default:
			stickerClicked(v, HikeConstants.SINGLE_TAP);
			break;
		}
	}

	@Override
	public void onVerticalScrolled(int dy, int scrollDirection)
	{
		if (dy < 0)
		{
			return;
		}

		layoutCategoryIcon(dy);
		calculateTopMarginForCenterVertical(dy);
		layoutCategoryDetails();
		layoutCategoryDescription(dy, scrollDirection);
		layoutDownloadButton(dy);
		layoutHeaderDivider(dy);

	}

	private void setRvGridPadding()
	{
		int paddingTop = Utils.dpToPx(12) + Utils.dpToPx(128) + Utils.dpToPx(21) + Utils.dpToPx(2);
		headerContainer.setPadding(0, paddingTop, 0, 0);
	}

	private void calculateTopMarginForCenterVertical(int scrolly)
	{

		RelativeLayout.LayoutParams categoryIconParams = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();

		int containerHeight = categoryIconParams.height;

		int itemsTotalHeightMax = categoryName.getHeight() + categoryDetails.getHeight() + Utils.dpToPx(0.4f) + categoryDescription.getHeight() + Utils.dpToPx(9)
				+ Utils.dpToPx(30) + Utils.dpToPx(10);

		int itemsTotalHeightMin = categoryName.getHeight() + categoryDetails.getHeight() + Utils.dpToPx(0.4f);

		topMarginForCenterVertical = Math.max(topMarginForCenterVertical, (containerHeight - itemsTotalHeightMax) / 2);

		int minTopMarginForCenterVertical = (Utils.dpToPx(48) - itemsTotalHeightMin) / 2;

		Logger.d(TAG, "max top margin for center vertical : " + topMarginForCenterVertical);

		Logger.d(TAG, "min top margin for center vertical : " + minTopMarginForCenterVertical);

		float minTranslation = -96 * Utils.densityMultiplier;
		float scaleY = (Math.max(1 - (-scrolly / minTranslation), (float) minTopMarginForCenterVertical / topMarginForCenterVertical));

		Logger.d(TAG, "scale y : " + scaleY);
		RelativeLayout.LayoutParams categoryNameParams = (RelativeLayout.LayoutParams) categoryName.getLayoutParams();
		categoryNameParams.topMargin = (int) (topMarginForCenterVertical * scaleY);
		categoryName.setLayoutParams(categoryNameParams);
	}

	private void layoutCategoryIcon(int scrolly)
	{
		categoryIconMaxHeight = Utils.dpToPx(128);
		categoryIconMaxWidth = Utils.dpToPx(128);

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();
		float minTranslation = -96 * Utils.densityMultiplier;
		float scaleX = (Math.max(1 - (-scrolly / minTranslation), 48 / 128f));
		float scaleY = (Math.max(1 - (-scrolly / minTranslation), 48 / 128f));
		params.height = (int) (categoryIconMaxHeight * scaleY);
		params.width = (int) (categoryIconMaxWidth * scaleX);
		params.leftMargin = Utils.dpToPx(15);

		categoryIcon.setLayoutParams(params);
	}

	private void layoutCategoryDetails()
	{
		RelativeLayout.LayoutParams categoryIconParams = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();

		RelativeLayout.LayoutParams categoryNameParams = (RelativeLayout.LayoutParams) categoryName.getLayoutParams();
		categoryNameParams.rightMargin = Utils.dpToPx(16);
		categoryNameParams.leftMargin = Utils.dpToPx(15) + categoryIconParams.width + Utils.dpToPx(20);
		categoryName.setLayoutParams(categoryNameParams);

		RelativeLayout.LayoutParams categoryDetailsParams = (RelativeLayout.LayoutParams) categoryDetails.getLayoutParams();
		categoryDetailsParams.topMargin = categoryNameParams.topMargin + categoryName.getHeight() + Utils.dpToPx(0.4f);
		categoryDetailsParams.leftMargin = Utils.dpToPx(15) + categoryIconParams.width + Utils.dpToPx(20);
		categoryNameParams.rightMargin = Utils.dpToPx(16);
		categoryDetails.setLayoutParams(categoryDetailsParams);
	}

	private void layoutCategoryDescription(int scrolly, int scrollDirection)
	{
		RelativeLayout.LayoutParams categoryIconParams = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();

		categoryDescriptionMaxHeight = Math.max(categoryDescriptionMaxHeight, categoryDescription.getHeight());

		RelativeLayout.LayoutParams categoryDetailsParams = (RelativeLayout.LayoutParams) categoryDetails.getLayoutParams();

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) categoryDescription.getLayoutParams();
		params.leftMargin = Utils.dpToPx(15) + categoryIconParams.width + Utils.dpToPx(20);
		params.topMargin = categoryDetailsParams.topMargin + categoryDetails.getHeight() + Math.max(Utils.dpToPx(9) - scrolly, 0);
		params.rightMargin = Utils.dpToPx(16);

		float minTranslation = -96 * Utils.densityMultiplier;
		float scaleY = Math.max(0, 1 - (-scrolly / minTranslation));
		params.height = (int) (categoryDescriptionMaxHeight * scaleY);
		categoryDescription.setLayoutParams(params);

		if (scrollDirection == PackPreviewFragmentScrollListener.SCROLL_UP)
		{
			categoryDescription.setAlpha(Math.max(0, 0.2f - (-scrolly / minTranslation)));
		}
		else
		{
			categoryDescription.setAlpha(Math.min(1, 1 - (-scrolly / minTranslation)));
		}

	}

	private void layoutDownloadButton(int scrolly)
	{
		float minTranslation = -96 * Utils.densityMultiplier;

		RelativeLayout.LayoutParams categoryIconParams = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();
		RelativeLayout.LayoutParams categoryDescriptionLayoutParams = (RelativeLayout.LayoutParams) categoryDescription.getLayoutParams();
		int maxTopMargin = categoryDescriptionLayoutParams.topMargin + categoryDescriptionLayoutParams.height +  Utils.dpToPx(10) - scrolly;
		int minTopMargin = (categoryIconParams.height - Utils.dpToPx(30))/2;

		downloadButtonMaxWidth = Math.min(downloadButtonMaxWidth, Utils.getDeviceWidth() - categoryIconParams.leftMargin - categoryIconParams.width - Utils.dpToPx(20) - Utils.dpToPx(16));

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) downloadBtn.getLayoutParams();
		float scaleX = Math.max(1 - (-scrolly / minTranslation), 100 / downloadButtonMaxWidth);
		params.width = Math.max(Utils.dpToPx(100), (int) (downloadButtonMaxWidth * scaleX));
		params.height = Utils.dpToPx(30);
		params.rightMargin = Utils.dpToPx(16);
		params.topMargin = Math.max(maxTopMargin, minTopMargin);

		downloadBtn.setLayoutParams(params);

		RelativeLayout.LayoutParams categoryNameParams = (RelativeLayout.LayoutParams) categoryName.getLayoutParams();
		categoryNameParams.rightMargin = Math.min(Utils.dpToPx(16) +  (int) ((-scrolly / (minTranslation/1.4)) * Utils.dpToPx(100)) , Utils.dpToPx(16) + Utils.dpToPx(100));
		categoryName.setLayoutParams(categoryNameParams);
	}

	private void layoutHeaderDivider(int scrolly)
	{
		RelativeLayout.LayoutParams categoryIconParams = (RelativeLayout.LayoutParams) categoryIcon.getLayoutParams();

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) headerDivider.getLayoutParams();
		params.topMargin = categoryIconParams.height + Math.max(Utils.dpToPx(21) - scrolly, Utils.dpToPx(10));
		headerDivider.setLayoutParams(params);

	}

	private void updateButtonState()
	{
		StickerCategory category = StickerManager.getInstance().getStickerCategoryMap().get(catId);
		if (category != null)
		{
			stickerCategory = category;
		}

		downloadBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_blue));

		switch (stickerCategory.getState())
		{
		case StickerCategory.NONE:
		case StickerCategory.DONE_SHOP_SETTINGS:
		case StickerCategory.DONE:
			if (stickerCategory.getDownloadedStickersCount() == 0 || !stickerCategory.isDownloaded())
			{
				downloadBtn.setText(getResources().getString(R.string.download));
			}
			else
			{
				downloadBtn.setText(getResources().getString(R.string.downloaded));
				downloadBtn.setAlpha(0.4f);
			}
			break;
		case StickerCategory.UPDATE:
			downloadBtn.setText(getResources().getString(R.string.update));
			downloadBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_green));
			break;
		case StickerCategory.RETRY:
			downloadBtn.setText(getResources().getString(R.string.retry));
			break;
		case StickerCategory.DOWNLOADING:
			downloadBtn.setText(getResources().getString(R.string.downloading_sticker));
			break;
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

			switch (intent.getAction())
			{
			case StickerManager.MORE_STICKERS_DOWNLOADED:
			case StickerManager.STICKERS_UPDATED:
			case StickerManager.STICKERS_DOWNLOADED:
				String categoryId = intent.getStringExtra(StickerManager.CATEGORY_ID);
				if (categoryId.equalsIgnoreCase(catId))
				{
					updateButtonState();
				}
				break;
			case StickerManager.STICKERS_FAILED:
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				if (categoryId.equalsIgnoreCase(catId))
				{
					updateButtonState();
				}
				break;
			case StickerManager.STICKER_PREVIEW_DOWNLOADED:
				stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(catId, StickerManager.PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE),
						categoryIcon);
				break;
			}
		}
	};

	@Override
	public void onUiEventReceived(String type, Object object) {
		switch (type) {
			case HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_SUCCESS:
				if (!isAdded()) {
					return;
				}

				StickerCategory category = (StickerCategory) object;
				if (category.getCategoryId().equalsIgnoreCase(catId)) {
					stickerCategory = category;
					setDetails();
					updateButtonState();
					loadingView.setVisibility(View.GONE);
					loadingFailed.setVisibility(View.GONE);
				}
				break;

			case HikePubSub.STICKER_CATEGORY_DETAILS_DOWNLOAD_FAILURE:
				if (!isAdded()) {
					return;
				}
				String categoryId = (String) object;
				if (categoryId.equalsIgnoreCase(catId)) {
					loadingView.setVisibility(View.GONE);
					loadingFailed.setVisibility(View.VISIBLE);
				}
				break;
		}
	}

	@Override
	public void onEventReceived(String type, final Object object)
	{
		switch (type)
		{
		case HikePubSub.STICKER_DOWNLOADED:
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (mAdapter != null)
					{
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	}


	private void stickerClicked(View v, int clickType)
	{
		int position = rvGrid.getChildAdapterPosition(v) - mAdapter.getHeaderListSize();
		if (position < 0 || position >= stickerCategory.getAllStickers().size())
		{
			return;
		}

		Sticker sticker = stickerCategory.getAllStickers().get(position);

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_STICKER_PREVIEW, false))
		{
			stickerPreviewContainer.show(v, sticker);
		}

		sendStickerClickedAnalytics(sticker, clickType);

	}

	private void viewAllClicked()
	{
		if(mAdapter == null)
		{
			Logger.wtf(TAG, "view all clicked with no adapter");
			return;
		}
		viewAllButton.setVisibility(View.GONE);
		mAdapter.setLists(stickerCategory.getAllStickers(), getHeaderList(), getFooterList());
		mAdapter.notifyDataSetChanged();
		StickerManager.getInstance().sendViewAllClickAnalytics(stickerCategory.getCategoryId());
	}

	private void loadingFailedClicked()
	{
		executeFetchCategoryDetailsTask(new FetchCategoryDetailsTask(catId));
	}

	private void downloadButtonClicked()
	{
		switch (stickerCategory.getState())
		{
			case StickerCategory.NONE:
			case StickerCategory.DONE_SHOP_SETTINGS:
			case StickerCategory.DONE:
				if (stickerCategory.getDownloadedStickersCount() == 0 || !stickerCategory.isDownloaded())
				{
					StickerManager.getInstance().setShowLastCategory(true);
					StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(stickerCategory.getCategoryId());
					stickerPalleteImageDownloadTask.execute();
					StickerManager.getInstance().initialiseDownloadStickerPackTask(stickerCategory,
							StickerConstants.DownloadType.NEW_CATEGORY, StickerManager.getInstance().getPackDownloadBodyJson(StickerConstants.DownloadSource.PREVIEW, categoryPosition, isViewAllClicked()));
				}
				break;
			case StickerCategory.UPDATE:
			case StickerCategory.RETRY:
				StickerManager.getInstance().initialiseDownloadStickerPackTask(stickerCategory, StickerManager.getInstance().getPackDownloadBodyJson(StickerConstants.DownloadSource.PREVIEW, categoryPosition, isViewAllClicked()));
				break;
			default:
				break;
		}

		updateButtonState();
		/**
		 * This is done to remove the green dot for update available state. For a new category added on the fly from the server, the update available is set to true to show
		 * a green indicator. To remove that, we are doing this.
		 */
		if (stickerCategory.isUpdateAvailable())
		{
			stickerCategory.setUpdateAvailable(false);
		}
	}

	private boolean isViewAllClicked()
	{
		return viewAllButton.getVisibility() == View.GONE;
	}

	private void sendStickerClickedAnalytics(final Sticker sticker, final int clickType)
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(AnalyticsConstants.V2.GENUS, sticker.getCategoryId());
					metadata.put(AnalyticsConstants.V2.SPECIES, sticker.getStickerId());
					metadata.put(AnalyticsConstants.V2.SOURCE, AnalyticsConstants.PACK_PREVIEW);
					metadata.put(AnalyticsConstants.TYPE, clickType);
					metadata.put(AnalyticsConstants.V2.FORM, false);
					StickerManager.getInstance().sendStickerClickedLogs(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "sendStickerClickedAnalytics() : Exception while parsing JSON");
				}
			}
		});
	}

	@Override
	public boolean onLongClick(View v)
	{
		stickerClicked(v, HikeConstants.LONG_TAP);
		return false;
	}
}
