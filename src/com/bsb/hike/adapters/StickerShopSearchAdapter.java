package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.ui.fragments.StickerShopBaseFragment.StickerShopViewHolder;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

public class StickerShopSearchAdapter extends BaseAdapter
{
	private LayoutInflater layoutInflater;

	private StickerOtherIconLoader stickerOtherIconLoader;

	private boolean isListFlinging;

	private Animation packPreviewFtueAnimation;

	private boolean shownPackPreviewFtue;

	private List<StickerCategory> searchedCategories;

	private Context mContext;

	public StickerShopSearchAdapter(Context context, List<StickerCategory> searchedCategories,StickerOtherIconLoader stickerOtherIconLoader)
	{
		this.mContext = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerOtherIconLoader = stickerOtherIconLoader;
		this.searchedCategories = searchedCategories;
		this.packPreviewFtueAnimation = shownPackPreviewFtue ? null : HikeAnimationFactory.getStickerPreviewFtueAnimation(context);
	}

	@Override
	public String getItem(int position)
	{
		if (position < searchedCategories.size())
		{
			return searchedCategories.get(position).getCategoryId();
		}
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		if (position < searchedCategories.size())
		{
			searchedCategories.get(position).getUniqueId();
		}

		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		StickerShopViewHolder viewHolder = null;

		if (convertView == null)
		{
            convertView= layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
			viewHolder = new StickerShopViewHolder(convertView);
		}
        else
        {
            viewHolder = (StickerShopViewHolder) convertView.getTag();
        }

		StickerCategory category = searchedCategories.get(position);

		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(category.getCategoryId(), StickerManager.PREVIEW_IMAGE_SHOP_TYPE),
                viewHolder.categoryPreviewIcon);
		stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);

		viewHolder.loadViewFromCategory(mContext, category);
        viewHolder.showPackPreviewFtue(packPreviewFtueAnimation,position);

        return convertView;
	}

	@Override
	public int getCount()
	{
		return searchedCategories.size();
	}

	public StickerOtherIconLoader getStickerPreviewLoader()
	{
		return stickerOtherIconLoader;
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;
		isListFlinging = b;
		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

	public void setShownPackPreviewFtue()
	{
		if (!shownPackPreviewFtue)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, true);
			shownPackPreviewFtue = true;
		}
	}

    public void updateSearchresult(List<StickerCategory> categories)
    {
        this.searchedCategories = categories;
        notifyDataSetChanged();
    }

}
