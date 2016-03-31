package com.bsb.hike.adapters;

import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopAdapter extends CursorAdapter
{
	private LayoutInflater layoutInflater;

	private StickerOtherIconLoader stickerOtherIconLoader;
	
	private boolean isListFlinging;

	private int idColoumn;

	private int categoryNameColoumn;

	private int totalStickersCountColoumn;

	private int categorySizeColoumn;

	private Animation packPreviewFtueAnimation;

	private boolean shownPackPreviewFtue;

	private Map<String, StickerCategory> stickerCategoriesMap;
	
	private final int FULLY_DOWNLOADED = 0;
	
	private final int NOT_DOWNLOADED = 1;
	
	private final int UPDATE_AVAILABLE = 2;
	
	private final int RETRY = 3;

	class ViewHolder
	{
		TextView categoryName;

		TextView totalStickers;

		TextView stickersPackDetails;
		
		TextView categoryPrice;

		ImageView downloadState;
		
		ImageView categoryPreviewIcon;
	}

	public StickerShopAdapter(Context context, Cursor cursor, Map<String, StickerCategory> stickerCategoriesMap)
	{
		super(context, cursor, false);
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
		this.stickerCategoriesMap = stickerCategoriesMap;
		shownPackPreviewFtue = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_PACK_PREVIEW_FTUE, false);
		this.packPreviewFtueAnimation = shownPackPreviewFtue ? null : HikeAnimationFactory.getStickerPreviewFtueAnimation(mContext);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
		ViewHolder viewholder = new ViewHolder();
		viewholder.categoryName = (TextView) v.findViewById(R.id.category_name);
		viewholder.stickersPackDetails = (TextView) v.findViewById(R.id.pack_details);
		viewholder.downloadState = (ImageView) v.findViewById(R.id.category_download_btn);
		viewholder.categoryPreviewIcon = (ImageView) v.findViewById(R.id.category_icon);
		viewholder.categoryPrice = (TextView) v.findViewById(R.id.category_price);
		v.setTag(viewholder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		ViewHolder viewholder = (ViewHolder) view.getTag();
		String categoryId = cursor.getString(idColoumn);
		String displayCategoryName = context.getResources().getString(R.string.pack_rank, cursor.getPosition() + 1);
		String categoryName = cursor.getString(categoryNameColoumn);
		displayCategoryName += " " + categoryName;
		int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
		int categorySizeInBytes = cursor.getInt(categorySizeColoumn);
		viewholder.categoryName.setText(displayCategoryName);
		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PREVIEW_IMAGE_SHOP_TYPE), viewholder.categoryPreviewIcon);
		stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);
		if (totalStickerCount > 0)
		{
			String detailsStirng = totalStickerCount == 1 ? context.getResources().getString(R.string.singular_stickers, totalStickerCount)  : context.getResources().getString(R.string.n_stickers, totalStickerCount);
			if (categorySizeInBytes > 0)
			{
				detailsStirng += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
			}
			viewholder.stickersPackDetails.setVisibility(View.VISIBLE);
			viewholder.stickersPackDetails.setText(detailsStirng);
		}
		else
		{
			viewholder.stickersPackDetails.setVisibility(View.GONE);
		}

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
		viewholder.downloadState.setVisibility(View.VISIBLE);
		showPackPreviewFtue(cursor.getPosition(), viewholder);
		
		if(category.isVisible())
		{
			switch (category.getState())
			{
			case StickerCategory.NONE:
			case StickerCategory.DONE_SHOP_SETTINGS:
			case StickerCategory.DONE:
				if (category.getDownloadedStickersCount() == 0)
				{
					viewholder.categoryPrice.setVisibility(View.VISIBLE);
					viewholder.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
					viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				}
				else
				{
					viewholder.categoryPrice.setText(context.getResources().getString(R.string.downloaded).toUpperCase());
					viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.blue_hike));
				}
				break;
			case StickerCategory.UPDATE:
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.update_sticker));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.sticker_settings_update_color));
				break;
			case StickerCategory.RETRY:
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.RETRY));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				break;
			case StickerCategory.DOWNLOADING:
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.downloading_stk));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				
				break;
			}
		}
		else
		{
			viewholder.categoryPrice.setVisibility(View.VISIBLE);
			viewholder.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
			viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
		}
		viewholder.downloadState.setTag(category);
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

	private void showPackPreviewFtue(int position, ViewHolder viewholder)
	{
		if(!shownPackPreviewFtue)
		{
			Animation animation = viewholder.downloadState.getAnimation();
			if(animation != null)
			{
				animation.cancel();
			}

			if(position == 0)
			{
				viewholder.downloadState.startAnimation(packPreviewFtueAnimation);
			}
			else
			{
				viewholder.downloadState.setAnimation(null);
			}
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
