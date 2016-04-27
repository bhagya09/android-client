package com.bsb.hike.adapters;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bsb.hike.R;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.adapters.StickerShopBaseAdapter.ViewHolder;

public class StickerShopSearchAdapter extends BaseAdapter
{
	private LayoutInflater layoutInflater;

	private StickerOtherIconLoader stickerOtherIconLoader;

	private boolean isListFlinging;

	private List<StickerCategory> searchedCategories;

    private Map<String, StickerCategory> stickerCategoriesMap;

	private Context mContext;

    private StickerShopBaseAdapter stickerShopBaseAdapter;

	public StickerShopSearchAdapter(Context context, Map<String, StickerCategory> stickerCategoriesMap)
	{
		this.mContext = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
        this.stickerCategoriesMap = stickerCategoriesMap;
        this.stickerShopBaseAdapter = new StickerShopBaseAdapter(context);
	}

	@Override
	public String getItem(int position)
	{
		if(!Utils.isEmpty(searchedCategories) && position < searchedCategories.size())
        {
			return searchedCategories.get(position).getCategoryId();
		}

		return null;
	}

	@Override
	public long getItemId(int position)
	{
		if (!Utils.isEmpty(searchedCategories) && position < searchedCategories.size())
		{
			searchedCategories.get(position).getCategoryId().hashCode();
		}

		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder viewHolder = null;

        if (convertView == null)
		{
            convertView= layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
			viewHolder = stickerShopBaseAdapter.loadShopViewHolder(convertView);
		}
        else
        {
            viewHolder = (ViewHolder) convertView.getTag();
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

        stickerShopBaseAdapter.loadViewFromCategory(category, viewHolder);

        return convertView;
	}

	@Override
	public int getCount()
	{
		if (!Utils.isEmpty(searchedCategories))
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
