package com.bsb.hike.modules.stickersearch.ui;

import java.util.List;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.StickerLoader;

public class StickerRecomendationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{

	private List<Sticker> stickerList;

	private StickerLoader stickerLoader;

	private Context mContext;

	private int sizeEachImage;
	
	private StickerRecommendationFragment listener;

	private static final int TYPE_STICKER = 1;

	public StickerRecomendationAdapter(List<Sticker> stickerList, StickerRecommendationFragment listener)
	{
		this.stickerList = stickerList;
		this.stickerLoader = new StickerLoader(HikeMessengerApp.getInstance(), true);
		this.mContext = HikeMessengerApp.getInstance();
		this.sizeEachImage = StickerSearchUtils.getStickerSize();
		this.listener = listener;
	}

	@Override
	public int getItemCount()
	{
		return stickerList.size();
	}

	@Override
	public int getItemViewType(int position)
	{
		return TYPE_STICKER;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position)
	{
		Sticker sticker = stickerList.get(position);
		StickerViewHolder stickerViewHolder = (StickerViewHolder) viewHolder;
		ImageView imageView = stickerViewHolder.imageView;
		int padding = mContext.getResources().getDimensionPixelSize(R.dimen.sticker_recommend_sticker_image_padding);
		imageView.setScaleType(ScaleType.CENTER_INSIDE);
		imageView.setPadding(padding, padding, padding, padding);
		stickerLoader.loadImage(sticker.getSmallStickerPath(), imageView, false);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		RecyclerView.LayoutParams ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
		ImageView convertView = new ImageView(mContext);
		convertView.setLayoutParams(ll);
		return new StickerViewHolder(convertView);
	}

	public class StickerViewHolder extends RecyclerView.ViewHolder implements OnClickListener
	{
		private ImageView imageView;

		public StickerViewHolder(View row)
		{
			super(row);
			row.setOnClickListener(this);
			this.imageView = (ImageView) row;
		}

		@Override
		public void onClick(View view)
		{
			listener.click(view);
		}
	}

	public void setStickerList(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;
	}
}
