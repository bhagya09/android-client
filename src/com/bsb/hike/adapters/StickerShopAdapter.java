package com.bsb.hike.adapters;

import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.adapters.StickerShopBaseAdapter.ViewHolder;

public class StickerShopAdapter extends CursorAdapter
{
	private LayoutInflater layoutInflater;

	private boolean isListFlinging;

	private int idColoumn;

	private int categoryNameColoumn;

	private int totalStickersCountColoumn;

	private int categorySizeColoumn;

	private Animation packPreviewFtueAnimation;

	private boolean shownPackPreviewFtue;

	private Map<String, StickerCategory> stickerCategoriesMap;

    private StickerOtherIconLoader stickerOtherIconLoader;

    private StickerShopBaseAdapter stickerShopBaseAdapter;
	
	public StickerShopAdapter(Context context, Cursor cursor, Map<String, StickerCategory> stickerCategoriesMap)
	{
		super(context, cursor, false);
		this.layoutInflater = LayoutInflater.from(context);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
		this.stickerCategoriesMap = stickerCategoriesMap;
        this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
		this.shownPackPreviewFtue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, false);
		this.packPreviewFtueAnimation = shownPackPreviewFtue ? null : HikeAnimationFactory.getZoomInZoomOutAnimation(mContext);
        this.stickerShopBaseAdapter = new StickerShopBaseAdapter(context);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
        ViewHolder viewholder = stickerShopBaseAdapter.loadShopViewHolder(v);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		ViewHolder viewholder = (ViewHolder) view.getTag();
		String categoryId = cursor.getString(idColoumn);

        stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PREVIEW_IMAGE_SHOP_TYPE), viewholder.categoryPreviewIcon);
        stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);

        StickerCategory category;
		if (stickerCategoriesMap.containsKey(categoryId))
		{
			category = stickerCategoriesMap.get(categoryId);
		}
		else
		{
            String displayCategoryName = context.getResources().getString(R.string.pack_rank, cursor.getPosition() + 1);
            String categoryName = cursor.getString(categoryNameColoumn);
            int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
            int categorySizeInBytes = cursor.getInt(categorySizeColoumn);

            category = new StickerCategory.Builder()
					.setCategoryId(categoryId)
					.setCategoryName(categoryName)
					.setCategorySize(categorySizeInBytes)
					.setTotalStickers(totalStickerCount)
					.build();
			stickerCategoriesMap.put(categoryId, category);
		}

		stickerShopBaseAdapter.loadViewFromCategory(category, viewholder);
        showPackPreviewFtue(cursor.getPosition(), viewholder);
	}
	
	@Override
	public String getItem(int position)
	{
		Cursor cursor =  (Cursor) super.getItem(position);
		if(cursor != null)
		{
			return cursor.getString(idColoumn);
		}
		return null;
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
		if(!shownPackPreviewFtue)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, true);
			shownPackPreviewFtue = true;
		}
	}

    private void showPackPreviewFtue(int position, ViewHolder viewholder)
    {
		Animation animation = viewholder.downloadState.getAnimation();
		if (shownPackPreviewFtue)
		{
			if (animation != null)
			{
				viewholder.downloadState.clearAnimation();
			}
		}
		else
		{
			if (position == 0)
			{
				if (animation == null)
				{
					viewholder.downloadState.startAnimation(packPreviewFtueAnimation);
				}
			}
			else
			{
				viewholder.downloadState.setAnimation(null);
			}
		}
    }
}
