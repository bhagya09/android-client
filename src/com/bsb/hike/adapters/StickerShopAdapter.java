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
import com.bsb.hike.ui.fragments.StickerShopBaseFragment.StickerShopViewHolder;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

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

    private StickerShopAdapter mAdapter;

    private StickerOtherIconLoader stickerOtherIconLoader;
	
	public StickerShopAdapter(Context context, Cursor cursor, Map<String, StickerCategory> stickerCategoriesMap, StickerOtherIconLoader stickerOtherIconLoader)
	{
		super(context, cursor, false);
		this.layoutInflater = LayoutInflater.from(context);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
		this.stickerCategoriesMap = stickerCategoriesMap;
        this.stickerOtherIconLoader = stickerOtherIconLoader;
		shownPackPreviewFtue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, false);
		this.packPreviewFtueAnimation = shownPackPreviewFtue ? null : HikeAnimationFactory.getStickerPreviewFtueAnimation(mContext);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
        StickerShopViewHolder viewholder = new StickerShopViewHolder(v);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		StickerShopViewHolder viewholder = (StickerShopViewHolder) view.getTag();
		String categoryId = cursor.getString(idColoumn);
		String displayCategoryName = context.getResources().getString(R.string.pack_rank, cursor.getPosition() + 1);
		String categoryName = cursor.getString(categoryNameColoumn);
		int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
		int categorySizeInBytes = cursor.getInt(categorySizeColoumn);

        stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PREVIEW_IMAGE_SHOP_TYPE), viewholder.categoryPreviewIcon);
        stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);

        StickerCategory category;
		if (stickerCategoriesMap.containsKey(categoryId))
		{
			category = stickerCategoriesMap.get(categoryId);
		}
		else
		{
			category = new StickerCategory.Builder()
					.setCategoryId(categoryId)
					.setCategoryName(categoryName)
					.setCategorySize(categorySizeInBytes)
					.setTotalStickers(totalStickerCount)
					.build();
			stickerCategoriesMap.put(categoryId, category);
		}

        viewholder.loadViewFromCategory(context,category);

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

	@Override
	public int getCount()
	{
		return super.getCount();
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
}
