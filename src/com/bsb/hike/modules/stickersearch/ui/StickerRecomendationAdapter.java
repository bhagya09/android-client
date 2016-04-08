package com.bsb.hike.modules.stickersearch.ui;

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
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.StickerManager;

import java.util.List;

public class StickerRecomendationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ImageWorker.ImageLoaderListener
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

		//the sticker loader will attempt to download mini sticker if sticker not present provided the server switch is enabled other wise will download full sticker
		boolean loadMini = StickerManager.getInstance().isMiniStickersEnabled();
		this.stickerLoader = new StickerLoader.Builder()
                            .downloadLargeStickerIfNotFound(!loadMini)
                            .loadMiniStickerIfNotFound(loadMini)
                            .downloadMiniStickerIfNotFound(loadMini)
                            .build();

        this.stickerLoader.setImageLoaderListener(this);

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
        imageView.setEnabled(false);
		imageView.setPadding(padding, padding, padding, padding);
		stickerLoader.loadSticker(sticker, StickerConstants.StickerType.SMALL, imageView);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		RecyclerView.LayoutParams ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
		ImageView convertView = new ImageView(mContext);
		convertView.setLayoutParams(ll);
		return new StickerViewHolder(convertView);
	}

	@Override
	public void onImageWorkSuccess(ImageView imageView)
	{
		if (listener.isAdded())
		{
			imageView.setEnabled(true);
		}
	}

	@Override
	public void onImageWorkFailed(ImageView imageView)
	{
		if (listener.isAdded())
		{
			imageView.setEnabled(false);
		}
	}

    public class StickerViewHolder extends RecyclerView.ViewHolder implements OnClickListener
	{
		private ImageView imageView;

		public StickerViewHolder(View row)
		{
			super(row);
			this.imageView = (ImageView) row;
            this.imageView.setOnClickListener(this);
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
