package com.bsb.hike.adapters;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 27/04/16.
 */
public class StickerShopBaseAdapter
{
	private Context mContext;

	public static class ViewHolder
	{
		public TextView categoryName;

		public TextView totalStickers;

		public TextView stickersPackDetails;

		public TextView categoryPrice;

		public ImageView downloadState;

		public ImageView categoryPreviewIcon;

	}

	public StickerShopBaseAdapter(Context context)
	{
		this.mContext = context;
	}

	public ViewHolder loadShopViewHolder(View view)
	{
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.categoryName = (TextView) view.findViewById(R.id.category_name);
		viewHolder.stickersPackDetails = (TextView) view.findViewById(R.id.pack_details);
		viewHolder.downloadState = (ImageView) view.findViewById(R.id.category_download_btn);
		viewHolder.categoryPreviewIcon = (ImageView) view.findViewById(R.id.category_icon);
		viewHolder.categoryPrice = (TextView) view.findViewById(R.id.category_price);
		view.setTag(viewHolder);

		return viewHolder;
	}

	public void loadViewFromCategory(StickerCategory category, ViewHolder viewHolder)
	{

		viewHolder.categoryName.setText(category.getCategoryName());
		int totalStickerCount = category.getTotalStickers();
		int categorySizeInBytes = category.getCategorySize();
		if (totalStickerCount > 0)
		{
			String detailsStirng = totalStickerCount == 1 ? mContext.getResources().getString(R.string.singular_stickers, totalStickerCount) : mContext.getResources().getString(
					R.string.n_stickers, totalStickerCount);
			if (categorySizeInBytes > 0)
			{
				detailsStirng += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
			}
			viewHolder.stickersPackDetails.setVisibility(View.VISIBLE);
			viewHolder.stickersPackDetails.setText(detailsStirng);
		}
		else
		{
			viewHolder.stickersPackDetails.setVisibility(View.GONE);
		}

		if (category.isVisible())
		{
			switch (category.getState())
			{
			case StickerCategory.NONE:
			case StickerCategory.DONE_SHOP_SETTINGS:
			case StickerCategory.DONE:
				if (category.getDownloadedStickersCount() == 0)
				{
					viewHolder.categoryPrice.setVisibility(View.VISIBLE);
					viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.sticker_pack_free));
					viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.tab_pressed));
				}
				else
				{
					viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.downloaded).toUpperCase());
					viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.blue_hike));
				}
				break;
			case StickerCategory.UPDATE:
				viewHolder.categoryPrice.setVisibility(View.VISIBLE);
				viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.update_sticker));
				viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.sticker_settings_update_color));
				break;
			case StickerCategory.RETRY:
				viewHolder.categoryPrice.setVisibility(View.VISIBLE);
				viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.RETRY));
				viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.tab_pressed));
				break;
			case StickerCategory.DOWNLOADING:
				viewHolder.categoryPrice.setVisibility(View.VISIBLE);
				viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.downloading_stk));
				viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.tab_pressed));
				break;
			}
		}
		else
		{
			viewHolder.categoryPrice.setVisibility(View.VISIBLE);
			viewHolder.categoryPrice.setText(mContext.getResources().getString(R.string.sticker_pack_free));
			viewHolder.categoryPrice.setTextColor(mContext.getResources().getColor(R.color.tab_pressed));
		}
		viewHolder.downloadState.setTag(category);
		viewHolder.downloadState.setVisibility(View.VISIBLE);
	}

}
