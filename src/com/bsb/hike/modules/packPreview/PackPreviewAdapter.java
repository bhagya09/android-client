package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.models.utils.Size;

import java.util.List;

/**
 * Created by anubhavgupta on 04/01/16.
 */
public class PackPreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{

	private static final String TAG = PackPreviewAdapter.class.getSimpleName();

	private Context mContext;

	private List<Sticker> stickerList;

	private List<Pair<Integer, BasePackPreviewAdapterItem>> headerList;

	private List<Pair<Integer, BasePackPreviewAdapterItem>> footerList;

	private int stickerListSize, headerListSize, footerListSize;

	private StickerLoader stickerLoader;

	private int sizeEachImage;

	private View.OnClickListener onClickListener;

	public static final int VIEW_TYPE_GRID_TOP_MARGIN = 0;

	public static final int VIEW_TYPE_TAP_TEXT_HEADER = 1;

	public static final int VIEW_TYPE_STICKER = 2;

	public static final int VIEW_TYPE_VIEW_ALL_FOOTER = 3;

	public static final int VIEW_TYPE_AUTHOR_FOOTER = 4;

	public static final int VIEW_TYPE_RECOMMENDED_PACKS_FOOTER = 5;

	private int rowSize;

	public PackPreviewAdapter(Context context, View.OnClickListener onClickListener)
	{
		this.mContext = context;
		this.onClickListener = onClickListener;
		init();

	}

	private void init()
	{
		sizeEachImage = StickerSearchUtils.getStickerSize();

		stickerLoader = new StickerLoader.Builder()
				.loadMiniStickerIfNotFound(true)
				.setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.shop_placeholder))
				.setStickerDimension(new Size(sizeEachImage, sizeEachImage))
				.downloadMiniStickerIfNotFound(true)
				.build();

		rowSize = StickerManager.getInstance().getNumColumnsForStickerGrid(HikeMessengerApp.getInstance());
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
	{
		if (viewType == VIEW_TYPE_STICKER)
		{
			ImageView stickerIv = new ImageView(mContext);
			return new StickerViewHolder(stickerIv);
		}
		else
		{
			BasePackPreviewAdapterItem item = getItem(viewType);
			return item.getViewHolder();
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
	{
		int viewType = getItemViewType(position);
		switch (viewType)
		{
		case VIEW_TYPE_STICKER:
			position = position - headerListSize;
			Sticker sticker = stickerList.get(position);
			StickerViewHolder stickerViewHolder = (StickerViewHolder) viewHolder;
			ImageView stickerIv = stickerViewHolder.stickerIv;
			stickerIv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			applyPadding(stickerIv, position);
			stickerLoader.loadSticker(sticker, StickerConstants.StickerType.SMALL, stickerIv);
			break;
		}
	}

	private void applyPadding(View view, int position)
	{
		int totalSize = getItemCount();
		int rows = (int) Math.ceil(totalSize/(double) rowSize);
		int bottomRowStart = (rows - 1) * rowSize;
		int bottomRowEnd = totalSize - 1;

		int verticalSpacing = Utils.dpToPx(16);
		int horizontalSpacing = Utils.dpToPx(8);

		int leftSpacing = Utils.dpToPx(16);
		int rightSpacing = Utils.dpToPx(16);

		int padding = Utils.dpToPx(0);

		int paddingLeft = padding + horizontalSpacing/2;
		int paddingTop = padding + verticalSpacing/2;
		int paddingRight = padding + horizontalSpacing/2;
		int paddingBottom = padding + verticalSpacing/2;

		if (position < 0 || position >= totalSize)							//header or footer
		{
			paddingLeft = 0;
			paddingTop = 0;
			paddingRight = 0;
			paddingBottom = 0;
		}
		if(position == 0)													// top left
		{
			paddingLeft -= horizontalSpacing/2;
			paddingTop -= verticalSpacing/2;
		}
		if(position > 0 && position < rowSize - 1)							// middle element of top row
		{
			paddingTop -= verticalSpacing/2;
		}
		if(position == rowSize -1)											// top right
		{
			paddingTop -= verticalSpacing/2;
			paddingRight -= horizontalSpacing/2;
		}
		if(position == bottomRowStart)										// bottom left
		{
			paddingLeft -= horizontalSpacing/2;
			paddingBottom -= verticalSpacing/2;
		}
		if(position == bottomRowEnd)										// bottom right
		{
			paddingRight -= horizontalSpacing/2;
			paddingBottom -= verticalSpacing/2;
		}
		if(position > bottomRowStart && position < bottomRowEnd)			// middle element of bottom row
		{
			paddingBottom -= verticalSpacing/2;
		}
		if(position % rowSize == 0)											// leftmost	column
		{
			paddingLeft -= horizontalSpacing/2;
			paddingLeft += leftSpacing;
		}
		if(position % rowSize == (rowSize - 1))								// rightmost column
		{
			paddingRight -= horizontalSpacing/2;
			paddingRight += rightSpacing;
		}

		view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
	}

	@Override
	public int getItemViewType(int position)
	{
		if (position < headerListSize)
		{
			return headerList.get(position).first;
		}
		else if (position >= headerListSize && position < (headerListSize + stickerListSize))
		{
			return VIEW_TYPE_STICKER;
		}
		else
		{
			position = position - (headerListSize + stickerListSize);
			return footerList.get(position).first;
		}
	}

	@Override
	public int getItemCount()
	{
		return (headerListSize + stickerListSize + footerListSize);
	}


	public BasePackPreviewAdapterItem getItem(int viewType)
	{
		for(Pair<Integer, BasePackPreviewAdapterItem> item : headerList)
		{
			if(item.first == viewType)
			{
				return item.second;
			}
		}

		for(Pair<Integer, BasePackPreviewAdapterItem> item : footerList)
		{
			if(item.first == viewType)
			{
				return item.second;
			}
		}

		return null;
	}

	private class StickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		private ImageView stickerIv;

		public StickerViewHolder(View row)
		{
			super(row);
			stickerIv = (ImageView) row;
			RecyclerView.LayoutParams ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
			stickerIv.setLayoutParams(ll);
			row.setOnClickListener(this);
		}

		@Override
		public void onClick(View v)
		{
			if(onClickListener != null)
			{
				onClickListener.onClick(v);
			}
		}
	}

	public void releaseResources()
	{
		if(!Utils.isEmpty(headerList))
		{
			for (Pair<Integer, BasePackPreviewAdapterItem> pair : headerList) {
				BasePackPreviewAdapterItem item = pair.second;
				if (item != null) {
					item.releaseResources();
				}
			}
		}

		if(!Utils.isEmpty(footerList))
		{
			for (Pair<Integer, BasePackPreviewAdapterItem> pair : footerList) {
				BasePackPreviewAdapterItem item = pair.second;
				if (item != null) {
					item.releaseResources();
				}
			}
		}
	}

	public int getSpanSize(int position)
	{
		int viewType = getItemViewType(position);

		switch(viewType)
		{
			case VIEW_TYPE_GRID_TOP_MARGIN:
			case VIEW_TYPE_TAP_TEXT_HEADER:
			case VIEW_TYPE_VIEW_ALL_FOOTER:
			case VIEW_TYPE_AUTHOR_FOOTER:
			case VIEW_TYPE_RECOMMENDED_PACKS_FOOTER:
				return PackPreviewFragment.NUM_COLUMNS;
			case VIEW_TYPE_STICKER:
				return 1;
			default:
				return 1;
		}
	}

	public void setLists(List<Sticker> stickerList, List<Pair<Integer, BasePackPreviewAdapterItem>> headerList,
							 List<Pair<Integer, BasePackPreviewAdapterItem>> footerList)
	{
		this.stickerList = stickerList;
		this.headerList = headerList;
		this.footerList = footerList;

		stickerListSize = Utils.isEmpty(stickerList) ? 0 : stickerList.size();
		headerListSize = Utils.isEmpty(headerList) ? 0 : headerList.size();
		footerListSize = Utils.isEmpty(footerList) ? 0 : footerList.size();
	}

	public int getHeaderListSize()
	{
		return headerListSize;
	}
}
