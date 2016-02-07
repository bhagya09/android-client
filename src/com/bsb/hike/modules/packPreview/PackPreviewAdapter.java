package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.MiniStickerLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by anubhavgupta on 04/01/16.
 */
public class PackPreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{

	private static final String TAG = PackPreviewAdapter.class.getSimpleName();

	private Context mContext;

	private View header;

	private List<Sticker> stickerList;

	private MiniStickerLoader miniStickerLoader;

	private Drawable defaultDrawable;

	private int sizeEachImage;

	private final int VIEW_TYPE_TAP_TEXT_HEADER = 0;

	private final int VIEW_TYPE_STICKER = 1;

	private final int VIEW_TYPE_COPYRIGHT_FOOTER = 2;

	private final int VIEW_TYPE_RECOMMENDED_PACKS_FOOTER = 3;

	public PackPreviewAdapter(Context context, View header)
	{
		mContext = context;
		this.header = header;
		miniStickerLoader = new MiniStickerLoader(true);
		defaultDrawable = mContext.getResources().getDrawable(R.drawable.shop_placeholder, null);
		sizeEachImage = StickerSearchUtils.getStickerSize();

	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
	{
		switch (viewType)
		{
		case VIEW_TYPE_TAP_TEXT_HEADER:
			viewGroup.addView(header);
			return new TapTextHeaderViewHolder(header);
		case VIEW_TYPE_STICKER:
			ImageView stickerIv = new ImageView(mContext);
			RecyclerView.LayoutParams ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
			stickerIv.setLayoutParams(ll);
			return new StickerViewHolder(stickerIv);
		case VIEW_TYPE_COPYRIGHT_FOOTER:
			View copyrightFooter = LayoutInflater.from(mContext).inflate(R.layout.copyright_footer, viewGroup, false);
			return new CopyrightFooterViewHolder(copyrightFooter);
		case VIEW_TYPE_RECOMMENDED_PACKS_FOOTER:
			View recommendedPacksFooter = LayoutInflater.from(mContext).inflate(R.layout.recommended_packs_footer, viewGroup, false);
			RecyclerView rvRecommendedPacksGrid = (RecyclerView) recommendedPacksFooter.findViewById(R.id.rvRecommendedPacksGrid);
			int height = StickerSearchUtils.getStickerSize();
			rvRecommendedPacksGrid.getLayoutParams().height = height;
			LinearLayoutManager recommendedPacksLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
			RecommendedPacksAdapter mRecommendedPacksAdapter = new RecommendedPacksAdapter(mContext);
			rvRecommendedPacksGrid.setLayoutManager(recommendedPacksLayoutManager);
			rvRecommendedPacksGrid.setAdapter(mRecommendedPacksAdapter);
			mRecommendedPacksAdapter.setStickerList(stickerList);
			mRecommendedPacksAdapter.notifyDataSetChanged();
			return new RecommendedPacksFooterViewHolder(recommendedPacksFooter);
		default:
			stickerIv = new ImageView(mContext);
			ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
			stickerIv.setLayoutParams(ll);
			return new StickerViewHolder(stickerIv);
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
	{
		int viewType = getItemViewType(position);
		switch (viewType)
		{
		case VIEW_TYPE_STICKER:
			Sticker sticker = stickerList.get(position - 1);
			StickerViewHolder stickerViewHolder = (StickerViewHolder) viewHolder;
			int padding = Utils.dpToPx(5);
			ImageView stickerIv = stickerViewHolder.stickerIv;
			stickerIv.setScaleType(ImageView.ScaleType.FIT_XY);
			stickerIv.setPadding(padding, padding, padding, padding);
			stickerIv.setImageDrawable(defaultDrawable);
			miniStickerLoader.loadImage(StickerManager.getInstance().getStickerSetString(sticker.getStickerId(), sticker.getCategoryId()), stickerIv);
		}
	}

	@Override
	public int getItemViewType(int position)
	{
		if (position == 0)
		{
			return VIEW_TYPE_TAP_TEXT_HEADER;
		}
		else if (position == stickerList.size() + 1)
		{
			return VIEW_TYPE_COPYRIGHT_FOOTER;
		}
		else if (position == stickerList.size() + 2)
		{
			return VIEW_TYPE_RECOMMENDED_PACKS_FOOTER;
		}
		else
		{
			return VIEW_TYPE_STICKER;
		}
	}

	@Override
	public int getItemCount()
	{
		if (Utils.isEmpty(stickerList))
			return 3;
		return stickerList.size() + 3;
	}

	public void setStickerList(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;
	}

	private class TapTextHeaderViewHolder extends RecyclerView.ViewHolder
	{
		public TapTextHeaderViewHolder(View row)
		{
			super(row);
		}
	}

	private class StickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		private ImageView stickerIv;

		public StickerViewHolder(View row)
		{
			super(row);
			stickerIv = (ImageView) row;
			row.setOnClickListener(this);
		}

		@Override
		public void onClick(View v)
		{

		}
	}

	private class CopyrightFooterViewHolder extends RecyclerView.ViewHolder
	{
		public CopyrightFooterViewHolder(View row)
		{
			super(row);
		}
	}

	private class RecommendedPacksFooterViewHolder extends RecyclerView.ViewHolder
	{
		public RecommendedPacksFooterViewHolder(View row)
		{
			super(row);
		}
	}
}
