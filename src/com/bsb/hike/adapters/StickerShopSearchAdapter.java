package com.bsb.hike.adapters;

import java.util.List;
import java.util.Map;

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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopSearchAdapter extends BaseAdapter
{
	private LayoutInflater layoutInflater;

	private StickerOtherIconLoader stickerOtherIconLoader;

	private boolean isListFlinging;

	private List<StickerCategory> searchedCategories;

    private Map<String, StickerCategory> stickerCategoriesMap;

	private Context mContext;

	public StickerShopSearchAdapter(Context context, List<StickerCategory> searchedCategories, Map<String, StickerCategory> stickerCategoriesMap, StickerOtherIconLoader stickerOtherIconLoader)
	{
		this.mContext = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerOtherIconLoader = stickerOtherIconLoader;
		this.searchedCategories = searchedCategories;
        this.stickerCategoriesMap = stickerCategoriesMap;
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
			searchedCategories.get(position).getCategoryId().hashCode();
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

        String categoryId = searchedCategories.get(position).getCategoryId();

        StickerCategory category = null;
        if (stickerCategoriesMap.containsKey(categoryId))
        {
            category = stickerCategoriesMap.get(categoryId);
        }
		else
        {
            category = searchedCategories.get(position);
        }

		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(category.getCategoryId(), StickerManager.PREVIEW_IMAGE_SHOP_TYPE),
                viewHolder.categoryPreviewIcon);
		stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);

		viewHolder.loadViewFromCategory(mContext, category);

        return convertView;
	}

	@Override
	public int getCount()
	{
		if (Utils.isEmpty(searchedCategories))
		{
			return searchedCategories.size();
		}
		return 0;
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

    public void updateSearchresult(List<StickerCategory> categories)
    {
        this.searchedCategories = categories;
        notifyDataSetChanged();
    }

}
