package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by anubhavgupta on 25/01/16.
 */
public class RecommendedPacksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{

	private StickerOtherIconLoader stickerOtherIconLoader;

	private Context mContext;

	private List<StickerCategory> recommendedPacks;

	private int sizeEachImage;

	private View.OnClickListener clickListener;

	private final int VIEW_TYPE_CATEGORY = 0;

	public RecommendedPacksAdapter(Context context, View.OnClickListener clickListener)
	{
		this.mContext = context;
		this.clickListener = clickListener;
		this.stickerOtherIconLoader = new StickerOtherIconLoader(mContext, true);
		this.sizeEachImage = StickerSearchUtils.getStickerSize();
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
	{
		View convertView;

		switch (viewType)
		{
		case VIEW_TYPE_CATEGORY:
			convertView = LayoutInflater.from(mContext).inflate(R.layout.recommended_packs_item, viewGroup, false);
			return new StickerCategoryViewHolder(convertView);
		default:
			return null;
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
	{
		StickerCategory stickerCategory = recommendedPacks.get(position);
		StickerCategoryViewHolder stickerViewHolder = (StickerCategoryViewHolder) viewHolder;
		ImageView imageView = stickerViewHolder.categoryIcon;
		TextView categoryName = stickerViewHolder.categoryName;
		categoryName.setText(stickerCategory.getCategoryName());
		stickerOtherIconLoader.loadImage(
				StickerManager.getInstance().getCategoryOtherAssetLoaderKey(stickerCategory.getCategoryId(), StickerManager.PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE), imageView);
	}

	@Override
	public int getItemViewType(int position)
	{
		return VIEW_TYPE_CATEGORY;
	}

	@Override
	public int getItemCount()
	{
		if (Utils.isEmpty(recommendedPacks))
			return 0;
		return recommendedPacks.size();
	}

	public void setRecommendedPacks(List<StickerCategory> recommendedPacks)
	{
		this.recommendedPacks = recommendedPacks;
	}

	private class StickerCategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		private ImageView categoryIcon;

		private TextView categoryName;

		public StickerCategoryViewHolder(View row)
		{
			super(row);
			categoryIcon = (ImageView) row.findViewById(R.id.ivCategoryIcon);
			categoryName = (TextView) row.findViewById(R.id.categoryName);

            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(sizeEachImage, sizeEachImage);
			int padding = Utils.dpToPx(5);
			categoryIcon.setLayoutParams(ll);
			categoryIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			categoryIcon.setPadding(padding, padding, padding, padding);
			row.setOnClickListener(this);
		}

		@Override
		public void onClick(View v)
		{
			if (clickListener != null)
			{
				clickListener.onClick(v);
			}
		}
	}
}
